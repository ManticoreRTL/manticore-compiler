package manticore.compiler.assembly.levels.placed

import com.google.ortools.Loader
import com.google.ortools.linearsolver.MPSolver
import com.google.ortools.linearsolver.MPVariable
import manticore.compiler.AssemblyContext
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.SimpleDirectedGraph
import org.jgrapht.graph.SimpleDirectedWeightedGraph
import org.jgrapht.graph.builder.GraphTypeBuilder

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.{HashMap => MHashMap}
import scala.jdk.CollectionConverters._

// This transform implements an optimal process-to-core placement algorithm with the ILP formulation below:
//
// known values
// ------------
//
// - PR = {0 .. |PR|-1}
//
//     - pr \in PR is a process.
//
// - GPR = (PR, SE, W) // process graph
//
//     - se \in SE indictes that se.src sends a message to se.dst.
//     - Each edge se \in SE is assigned a weight w \in W indicating the number of
//       messages sent between 2 processes.
//
// - C = {0 .. |X|*|Y|-1}
//
//     - c \in C is a core.
//
// - PA = {0 .. |C|*(|C|-1)-1}
//
//     - pa \in PA is a path in the core graph.
//     - There is exactly 1 path between any 2 cores in a core graph as we are
//       using Dimension-Order-Routing (DOR).
//
// - GC = (C, CE) // core graph
//
//     - ce \in CE is an edge linking 2 cores. These are necessarily 2 adjacent cores.
//
// variables
// ---------
//
// (1) Create binary variable x_pr_c \in {0, 1} \forall (pr, c) \in PR x C.
//
//       x_pr_c = 1 if process pr is mapped to core c.
//
// constraints
// -----------
//
// (2) A process is assigned to exactly 1 core.
//
//       \sum{c \in C} {x_pr_c} = 1   \forall pr \in PR
//
// (3) A core can be assigned at most 1 process. We use "at most" instead of "exactly"
//     as the |C| >= |PR|.
//
//       \sum{pr \in PR} {x_pr_c} <= 1   \forall c \in C
//
// (4) Create auxiliary binary variable y_se_pa \in {0, 1} \forall (se, pa) \in SE x PA.
//
//       y_se_pa = 1 if send edge se is mapped to path pa.
//
//     Due to dimension-order-routing there is only 1 path between any 2 endpoints.
//     We can therefore compute y_se_pa as follows:
//
//       y_se_pa = x_(se.src)_(pa.src) ^ x_(se.dst)_(pa.dst)
//
//     The logic AND above is not linear, but can be made linear as the following
//     two constraints.
//
//        (a) y_se_pa <= x_(se.src)_(pa.src)
//        (b) y_se_pa <= x_(se.dst)_(pa.dst)
//        (c) y_se_pa >= x_(se.src)_(pa.src) + x_(se.dst)_(pa.dst) - 1
//
//     We model these constraints as follows:
//
//        (a) -1 <= y_se_pa - x_(se.src)_(pa.src) <= 1
//        ---
//        (b) -1 <= y_se_pa - x_(se.dst)_(pa.dst) <= 1
//        ---
//        (c) x_(se.src)_(pa.src) + x_(se.dst)_(pa.dst) - 1 <= y_se_pa           <==>
//            -1 <= x_(se.src)_(pa.src) + x_(se.dst)_(pa.dst) - y_se_pa <= 1
//
// (5) Create auxiliary variables w_ce \in {0, infinity} which count the total number of
//     messages going over edge ce. The weight of an edge in the core graph is the sum of
//     the weights of all paths that go through it multiplied by whether the path is assigned
//     to an edge se.
//
//        w_ce = \sum{se \in SE} {
//                 \sum{pa \in PA : ce \in pa} {
//                   w(se) * y_se_pa
//                 }
//               }
//
//        \forall ce \in CE.
//
//     The equality constraint can be modeled as 2 inequalities.
//
//        (a) w_ce >= \sum{se \in SE} {
//                      \sum{pa \in PA : ce \in pa} {
//                        w(se) * y_se_pa
//                      }
//                    }
//           <==>
//           0 <= w_ce - \sum{...} <= +infinity
//
//        (b) w_ce <= \sum{se \in SE} {
//                      \sum{pa \in PA : ce \in pa} {
//                        w(se) * y_se_pa
//                      }
//                    }
//           <==>
//           -infinity <= w_ce - \sum{...} <= 0
//
// objective function
// ------------------
//
// Minimizes the total on-chip NoC message traffic.
//
//     minimize \sum{ce in CE} {w_ce}
//

object OptimalPlacerTransform extends PlacedIRTransformer {
  import manticore.compiler.assembly.levels.placed.PlacedIR._

  // Note that this is NOT the same as a DefProcess' "proc_id" field!
  type ProcId = Int

  case class ProcEdge(src: ProcId, dst: ProcId) {
    override def toString(): String = s"${src}->${dst}"
  }

  case class CoreId(x: Int, y: Int) {
    override def toString(): String = s"x${x}_y${y}"
  }

  type CorePathId = Int

  class CorePath private (
      cores: Seq[CoreId]
  ) {
    import CorePath._

    def edges: Seq[PathEdge] = {
      cores.sliding(2).toSeq.map { case Seq(prev, succ) =>
        PathEdge(prev, succ)
      }
    }

    def contains(v: CoreId) = {
      cores.contains(v)
    }

    def contains(e: PathEdge) = {
      edges.contains(e)
    }

    def src: CoreId = cores.head
    def dst: CoreId = cores.last

    override def toString(): String = {
      cores.map(core => core.toString()).mkString(" -> ")
    }
  }

  object CorePath {
    case class PathEdge(src: CoreId, dst: CoreId)

    private def getXPath(
        src: CoreId,
        dst: CoreId,
        maxDimX: Int,
        acc: Seq[CoreId]
    ): Seq[CoreId] = {
      if (src.x == dst.x) {
        acc
      } else {
        val nextHop = CoreId((src.x + 1) % maxDimX, src.y)
        getXPath(nextHop, dst, maxDimX, acc :+ nextHop)
      }
    }

    private def getYPath(
        src: CoreId,
        dst: CoreId,
        maxDimY: Int,
        acc: Seq[CoreId]
    ): Seq[CoreId] = {
      if (src.y == dst.y) {
        acc
      } else {
        val nextHop = CoreId(src.x, (src.y + 1) % maxDimY)
        getYPath(nextHop, dst, maxDimY, acc :+ nextHop)
      }
    }

    def apply(
        src: CoreId,
        dst: CoreId,
        maxDimX: Int,
        maxDimY: Int
    ): CorePath = {
      // Manticore supports dimension-order-routing (DOR). Messages traverse the x-direction first,
      // then the y-direction.
      val xPath = getXPath(src, dst, maxDimX, Seq(src))
      val lastX = xPath.last
      val yPath = getYPath(lastX, dst, maxDimY, Seq(lastX))

      // The core at the intersection of the X and Y path is counted twice.
      // We filter out duplicates in the list to get rid of it.
      val fullPath = (xPath ++ yPath).distinct

      new CorePath(fullPath)
    }
  }

  type ProcessGraph = Graph[ProcId, DefaultEdge]
  type CoreGraph    = Graph[CoreId, DefaultEdge]

  def getProcessGraph(
      program: DefProgram
  ): (
      ProcessGraph,
      Map[ProcId, DefProcess],
      Map[ProcEdge, Int] // Edge weight = number of SENDs between 2 processes.
  ) = {
    val processGraph: ProcessGraph = new SimpleDirectedGraph(classOf[DefaultEdge])

    val procIdToProc = program.processes
      .map(proc => proc.id -> proc)
      .toMap

    // "GPR" = process graph. The "GprId" is the final number we will
    // associate with every process for the ILP formulation.
    val procIdToGprId = program.processes
      .map(proc => proc.id) // use proc.id so we don't have to hash a full DefProcess to look something up.
      .zipWithIndex
      .toMap

    // Add vertices to process graph.
    procIdToGprId.foreach { case (_, gprId) =>
      processGraph.addVertex(gprId)
    }

    // Initialize edge weights to 0. We will increment them iteratively.
    val edgeWeights = MHashMap.empty[ProcEdge, Int]

    // Add edges to process graph.
    program.processes.foreach { proc =>
      val srcProcId = proc.id
      val srcGprId  = procIdToGprId(srcProcId)

      proc.body.foreach { instr =>
        instr match {
          case Send(_, _, dstProcId, _) =>
            val dstGprId = procIdToGprId(dstProcId)
            processGraph.addEdge(srcGprId, dstGprId)

            val edge = ProcEdge(srcGprId, dstGprId)
            edgeWeights += edge -> (edgeWeights.getOrElse(edge, 0) + 1)

          case _ =>
        }
      }
    }

    val gprIdToProc = procIdToGprId.map { case (procId, gprId) =>
      gprId -> procIdToProc(procId)
    }

    (processGraph, gprIdToProc, edgeWeights.toMap)
  }

  def getCoreGraph(
      maxDimX: Int,
      maxDimY: Int
  ): CoreGraph = {
    val coreGraph: CoreGraph = new SimpleDirectedGraph(classOf[DefaultEdge])

    // Add core vertices.
    val coreLocToCoreId = Range(0, maxDimX).foreach { x =>
      Range(0, maxDimY).foreach { y =>
        coreGraph.addVertex(CoreId(x, y))
      }
    }

    // Add core x-dim edges.
    Range(0, maxDimY).foreach { y =>
      Range(0, maxDimX).foreach { x =>
        // The modulo is so we can create the torus link from the last column to the first column.
        val src = CoreId(x, y)
        val dst = CoreId((x + 1) % maxDimX, y)

        // In narrow configurations like a 1x2 (x-y), there will be a self-loop in the core graph
        // unless we do this check. This is normal as there is only 1 core in the x-dimension
        // in a narrow graph.
        if (src != dst) {
          coreGraph.addEdge(src, dst)
        }
      }
    }

    // Add core y-dim edges.
    Range(0, maxDimX).foreach { x =>
      Range(0, maxDimY).foreach { y =>
        // The modulo is so we can create the torus link from the last row to the first row.
        val src = CoreId(x, y)
        val dst = CoreId(x, (y + 1) % maxDimY)

        // In narrow configurations like a 2x1 (x-y), there will be a self-loop in the core graph
        // unless we do this check. This is normal as there is only 1 core in the y-dimension
        // in a narrow graph.
        if (src != dst) {
          coreGraph.addEdge(CoreId(x, y), CoreId(x, (y + 1) % maxDimY))
        }
      }
    }

    coreGraph
  }

  // Enumerates all possible paths in the core graph. The number of paths
  // is O(N^2) as we have dimension-order-routing. Otherwise there would
  // have been a combinatoric number of them!
  def enumeratePaths(
      maxDimX: Int,
      maxDimY: Int
  ): Map[
    CorePathId, // Path ID. We assign an ID to every path as we will need it in the ILP formulation.
    CorePath
  ] = {
    val paths = MHashMap.empty[Int, CorePath]

    Range(0, maxDimX).foreach { srcX =>
      Range(0, maxDimY).foreach { srcY =>
        val src = CoreId(srcX, srcY)
        Range(0, maxDimX).foreach { dstX =>
          Range(0, maxDimY).foreach { dstY =>
            val dst = CoreId(dstX, dstY)
            // Does not make sense to create a path from a core to itself.
            if (src != dst) {
              paths += paths.size -> CorePath(src, dst, maxDimX, maxDimY)
            }
          }
        }
      }
    }

    paths.toMap
  }

  def assignProcessesToCores(
    processGraph: ProcessGraph,
    procEdgeWeights: Map[ProcEdge, Int],
    coreGraph: CoreGraph,
    pathIdToPath: Map[CorePathId, CorePath]
  )(
    implicit ctx: AssemblyContext
  ): Map[
    ProcId,
    CoreId
  ] = {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // ILP formulation for core process-to-core assignment.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    Loader.loadNativeLibraries()
    val solver = MPSolver.createSolver("SCIP")
    if (solver == null) {
      ctx.logger.fail("Could not create solver SCIP.")
    }

    // (1) Create binary variable x_pr_c \in {0, 1} \forall (pr, c) \in PR x C.
    //
    //       x_pr_c = 1 if process pr is mapped to core c.
    //
    val vars_xPrC = MHashMap.empty[(ProcId, CoreId), MPVariable]
    processGraph.vertexSet().asScala.foreach { procId =>
      coreGraph.vertexSet().asScala.foreach { coreId =>
        vars_xPrC += (procId, coreId) -> solver.makeIntVar(0, 1, s"x_${procId}_${coreId}")
      }
    }

    // (2) A process is assigned to exactly 1 core.
    //
    //       \sum{c \in C} {x_pr_c} = 1   \forall pr \in PR
    //
    processGraph.vertexSet().asScala.foreach { procId =>
      val constraint = solver.makeConstraint(1, 1, s"proc_${procId}_assigned_exactly_one_core")
      coreGraph.vertexSet().asScala.foreach { coreId =>
        val xPrC = vars_xPrC((procId, coreId))
        constraint.setCoefficient(xPrC, 1)
      }
    }

    // (3) A core can be assigned at most 1 process. We use "at most" instead of "exactly"
    //     as the |C| >= |PR|.
    //
    //       \sum{pr \in PR} {x_pr_c} <= 1   \forall c \in C
    //
    coreGraph.vertexSet().asScala.foreach { coreId =>
      val constraint = solver.makeConstraint(0, 1, s"core_${coreId}_assigned_at_most_one_process")
      processGraph.vertexSet().asScala.foreach { case procId =>
        val xPrC = vars_xPrC((procId, coreId))
        constraint.setCoefficient(xPrC, 1)
      }
    }

    // (4) Create auxiliary binary variable y_se_pa \in {0, 1} \forall (se, pa) \in SE x PA.
    //
    //       y_se_pa = 1 if send edge se is mapped to path pa.
    //
    //     Due to dimension-order-routing there is only 1 path between any 2 endpoints.
    //     We can therefore compute y_se_pa as follows:
    //
    //       y_se_pa = x_(se.src)_(pa.src) ^ x_(se.dst)_(pa.dst)
    //
    //     The logic AND above is not linear, but can be made linear as the following
    //     two constraints.
    //
    //        (a) y_se_pa <= x_(se.src)_(pa.src)
    //        (b) y_se_pa <= x_(se.dst)_(pa.dst)
    //        (c) y_se_pa >= x_(se.src)_(pa.src) + x_(se.dst)_(pa.dst) - 1
    //
    //     We model these constraints as follows:
    //
    //        (a) -1 <= y_se_pa - x_(se.src)_(pa.src) <= 1
    //        ---
    //        (b) -1 <= y_se_pa - x_(se.dst)_(pa.dst) <= 1
    //        ---
    //        (c) x_(se.src)_(pa.src) + x_(se.dst)_(pa.dst) - 1 <= y_se_pa           <==>
    //            -1 <= x_(se.src)_(pa.src) + x_(se.dst)_(pa.dst) - y_se_pa <= 1

    // se \in SE is defined by the process graph edge.
    val vars_ySePa = MHashMap.empty[(ProcEdge, CorePathId), MPVariable]
    procEdgeWeights.foreach { case (procEdge, weight) =>
      pathIdToPath.foreach { case (pathId, path) =>
        // Define auxiliary binary variable y_se_pa.
        val ySePaVarName = s"y_${procEdge}_${pathId}"
        val ySePa = solver.makeIntVar(0, 1, ySePaVarName)
        vars_ySePa += (procEdge, pathId) -> ySePa

        val xSeSrcPaSrc = vars_xPrC((procEdge.src, path.src))
        val xSeDstPaDst = vars_xPrC((procEdge.dst, path.dst))

        // Define constraints.

        // (a) -1 <= y_se_pa - x_(se.src)_(pa.src) <= 1
        val ySePa_lessEqualSrc_constraint = solver.makeConstraint(-1, 1, s"${ySePaVarName}_lessEqualSrc_constraint")
        ySePa_lessEqualSrc_constraint.setCoefficient(ySePa, 1)
        ySePa_lessEqualSrc_constraint.setCoefficient(xSeSrcPaSrc, -1)

        // (b) -1 <= y_se_pa - x_(se.dst)_(pa.dst) <= 1
        val ySePa_lessEqualDst_constraint = solver.makeConstraint(-1, 1, s"${ySePaVarName}_lessEqualDst_constraint")
        ySePa_lessEqualDst_constraint.setCoefficient(ySePa, 1)
        ySePa_lessEqualDst_constraint.setCoefficient(xSeDstPaDst, -1)

        // (c) -1 <= x_(se.src)_(pa.src) + x_(se.dst)_(pa.dst) - y_se_pa <= 1
        val ySePa_greaterEqualSrcDst_constraint = solver.makeConstraint(-1, 1, s"${ySePaVarName}_greaterEqualSrcDst_constraint")
        ySePa_greaterEqualSrcDst_constraint.setCoefficient(xSeSrcPaSrc, 1)
        ySePa_greaterEqualSrcDst_constraint.setCoefficient(xSeDstPaDst, 1)
        ySePa_greaterEqualSrcDst_constraint.setCoefficient(ySePa, -1)
      }
    }

    // (5) Create auxiliary variables w_ce \in {0, infinity} which count the total number of
    //     messages going over edge ce. The weight of an edge in the core graph is the sum of
    //     the weights of all paths that go through it multiplied by whether the path is assigned
    //     to an edge se.
    //
    //        w_ce = \sum{se \in SE} {
    //                 \sum{pa \in PA : ce \in pa} {
    //                   w(se) * y_se_pa
    //                 }
    //               }
    //
    //        \forall ce \in CE.
    //
    //     The equality constraint can be modeled as 2 inequalities.
    //
    //        (a) w_ce >= \sum{se \in SE} {
    //                      \sum{pa \in PA : ce \in pa} {
    //                        w(se) * y_se_pa
    //                      }
    //                    }
    //           <==>
    //           0 <= w_ce - \sum{...} <= +infinity
    //
    //        (b) w_ce <= \sum{se \in SE} {
    //                      \sum{pa \in PA : ce \in pa} {
    //                        w(se) * y_se_pa
    //                      }
    //                    }
    //           <==>
    //           -infinity <= w_ce - \sum{...} <= 0
    //

    val vars_wCe = MHashMap.empty[CorePath.PathEdge, MPVariable]
    coreGraph.edgeSet().asScala.foreach { e =>
      val src = coreGraph.getEdgeSource(e)
      val dst = coreGraph.getEdgeTarget(e)
      val coreEdge = CorePath.PathEdge(src, dst)

      // Define auxiliary binary variable w_ce. The upper limit is set to +infinity as
      // as arbitrary number of messages can transit over the edge.
      val wCeVarName = s"w_${coreEdge}"
      val wCe = solver.makeIntVar(0, MPSolver.infinity(), wCeVarName)
      vars_wCe += coreEdge -> wCe

      // Define constraints.
      // The equality constraint is modeled with a >= and <= constraint.

      // (a) 0 <= w_ce - \sum{...} <= +infinity
      val wCe_greaterEqual_constraint = solver.makeConstraint(0, MPSolver.infinity(), s"${wCeVarName}_greaterEqual_constraint")
      // (b) -infinity <= w_ce - \sum{...} <= 0
      val wCe_lessEqual_constraint = solver.makeConstraint(-MPSolver.infinity(), 0, s"${wCeVarName}_lessEqual_constraint")

      wCe_greaterEqual_constraint.setCoefficient(wCe, 1)
      wCe_lessEqual_constraint.setCoefficient(wCe, 1)

      procEdgeWeights.foreach { case (procEdge, weight) =>
        pathIdToPath.foreach { case (pathId, path) =>
          val ySePa = vars_ySePa((procEdge, pathId))
          if (path.contains(coreEdge)) {
            wCe_greaterEqual_constraint.setCoefficient(ySePa, -1 * weight)
            wCe_lessEqual_constraint.setCoefficient(ySePa, -1 * weight)
          }
        }
      }
    }

    // objective function
    // ------------------
    //
    // Minimizes the total on-chip NoC message traffic.
    //
    //     minimize \sum{ce in CE} {w_ce}
    //
    val objective = solver.objective()
    vars_wCe.foreach { case (coreEdge, wCe) =>
      objective.setCoefficient(wCe, 1)
    }
    objective.setMinimization()

    // solve optimization problem
    val resultStatus = ctx.stats.recordRunTime("solver.solve") {
      solver.solve()
    }

    // Extract results
    if (resultStatus == MPSolver.ResultStatus.OPTIMAL) {
      // process-to-core assignments.
      val assignments = vars_xPrC.filter { case ((procId, coreId), xPrC) =>
        xPrC.solutionValue() > 0
      }.keySet.toMap

      val assignmentsStr = assignments.mkString("\n")

      ctx.logger.info {
        s"Solved process-to-core placement problem in ${solver.wallTime()}ms\n" +
          s"Optimal assignment has on-chip traffic of ${objective.value().toInt} is:\n${assignmentsStr}"
      }

      assignments

    } else {
      ctx.logger.fail(
        s"Could not optimally solve process-to-core placement problem (resultStatus = ${resultStatus})."
      )
      Map.empty
    }
  }

  override def transform(program: DefProgram)(implicit ctx: AssemblyContext): DefProgram = {

    val (processGraph, procIdToProc, procEdgeWeights) = getProcessGraph(program)

    val coreGraph = getCoreGraph(ctx.max_dimx, ctx.max_dimy)

    val pathIdToPath = enumeratePaths(ctx.max_dimx, ctx.max_dimy)

    ctx.logger.dumpArtifact("process_graph.dot") {
      val weights = procEdgeWeights.map { case (procEdge, weight) =>
        (procEdge.src, procEdge.dst) -> weight
      }
      dumpGraph(processGraph, weights)
    }

    ctx.logger.dumpArtifact("core_graph.dot") {
      dumpGraph(coreGraph)
    }

    val procIdToCoreId = assignProcessesToCores(processGraph, procEdgeWeights, coreGraph, pathIdToPath)

    program
  }

  def dumpGraph[V, E](
      g: Graph[V, E],
      weights: Map[(V, V), Int] = Map.empty[(V, V), Int]
  ): String = {
    def escape(s: String): String = s.replaceAll("\"", "\\\\\"")
    def quote(s: String): String  = s"\"${s}\""

    def getLabel(v: V): String = quote(escape(v.toString()))

    val s = new StringBuilder()

    // Vertices in a graph may not be legal graphviz identifiers, so we give
    // them all an Int id.
    val vToVid = g.vertexSet().asScala.zipWithIndex.toMap

    s ++= "digraph {\n"

    // Dump vertices.
    g.vertexSet().asScala.foreach { v =>
      val vId    = vToVid(v)
      val vLabel = getLabel(v)
      s ++= s"\t${vId} [label=${vLabel}]\n"
    }

    // Dump edges.
    g.edgeSet().asScala.foreach { e =>
      val src    = g.getEdgeSource(e)
      val dst    = g.getEdgeTarget(e)
      val weight = weights.get((src, dst)) match {
        case Some(w) => w.toString()
        case None => ""
      }

      val srcId  = vToVid(src)
      val dstId  = vToVid(dst)

      s ++= s"\t${srcId} -> ${dstId} [label=${weight}]\n"
    }

    s ++= "}\n" // digraph

    s.toString()
  }
}

object OptimalPlacerTest extends App {
  import OptimalPlacerTransform._

  val maxDimX = 3
  val maxDimY = 4

  val p0 = CorePath(CoreId(0, 0), CoreId(1, 2), maxDimX, maxDimY)
  val p1 = CorePath(CoreId(1, 1), CoreId(2, 0), maxDimX, maxDimY)

  println(p0)
  println(p1)

  val allPaths = enumeratePaths(maxDimX, maxDimY)
  println(s"num paths = ${allPaths.size}")
  allPaths.toSeq
    .sortBy { case (pathId, corePath) =>
      pathId
    }
    .foreach { case (pathId, corePath) =>
      println(pathId, corePath)
    }
}
