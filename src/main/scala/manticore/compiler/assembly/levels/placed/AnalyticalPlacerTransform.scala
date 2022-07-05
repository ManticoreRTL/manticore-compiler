package manticore.compiler.assembly.levels.placed

import com.google.ortools.Loader
import com.google.ortools.linearsolver.MPModelExportOptions
import com.google.ortools.linearsolver.MPSolver
import com.google.ortools.linearsolver.MPVariable
import com.google.ortools.sat.BoolVar
import com.google.ortools.sat.CpModel
import com.google.ortools.sat.CpSolver
import com.google.ortools.sat.CpSolverStatus
import com.google.ortools.sat.IntVar
import com.google.ortools.sat.LinearArgument
import com.google.ortools.sat.LinearExpr
import com.google.ortools.sat.LinearExprBuilder
import com.google.ortools.sat.Literal
import manticore.compiler.AssemblyContext
import manticore.compiler.HeatmapColor
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.SimpleDirectedGraph
import org.jgrapht.graph.SimpleDirectedWeightedGraph
import org.jgrapht.graph.builder.GraphTypeBuilder

import java.io.PrintWriter
import java.nio.file.Files
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.{HashMap => MHashMap}
import scala.jdk.CollectionConverters._

// This transform implements an analytic process-to-core placement algorithm with the ILP formulation below:
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
// - privileged_process \in PR
//
//     - This process contains a privileged instruction and must be mapped to
//       the privileged core (core X0_Y0).
//
// - privileged_core \in C
//
//     - This core is the only one capable of handling privileged processes.
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
// (6) Create auxiliary variable max_w \in {0, infinity} which keeps track of the maximum
//     core edge weight.
//
//        max_w >= max{ce \in CE} {w_ce}
//
// (7) The privileged process must be mapped to the privileged core.
//
//        x_privilegedProcess_privilegedCore = 1
//
// objective function
// ------------------
//
// Minimizes the largest core edge communication cost.
//
//     minimize {max_w}
//

object AnalyticalPlacerTransform extends PlacedIRTransformer {
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
      Map[ProcId, String], // ProcId -> process name
      Map[ProcEdge, Int]   // Edge weight = number of SENDs between 2 processes.
  ) = {
    val processGraph: ProcessGraph = new SimpleDirectedGraph(classOf[DefaultEdge])

    // "GPR" = process graph. The "GprId" is the final number we will
    // associate with every process for the ILP formulation.
    val procNameToGprId = program.processes
      .map(proc => proc.id.id) // use proc.id.id so we don't have to hash a full DefProcess to look something up.
      .zipWithIndex
      .toMap

    // Add vertices to process graph.
    procNameToGprId.foreach { case (_, gprId) =>
      processGraph.addVertex(gprId)
    }

    // Initialize edge weights to 0. We will increment them iteratively.
    val edgeWeights = MHashMap.empty[ProcEdge, Int]

    // Add edges to process graph.
    program.processes.foreach { proc =>
      val srcProcName = proc.id.id
      val srcGprId    = procNameToGprId(srcProcName)

      proc.body.foreach { instr =>
        instr match {
          case Send(_, _, dstProcId, _) =>
            val dstProcName = dstProcId.id
            val dstGprId    = procNameToGprId(dstProcName)
            processGraph.addEdge(srcGprId, dstGprId)

            val edge = ProcEdge(srcGprId, dstGprId)
            edgeWeights += edge -> (edgeWeights.getOrElse(edge, 0) + 1)

          case _ =>
        }
      }
    }

    val gprIdToProcName = procNameToGprId.map(x => x.swap)
    (processGraph, gprIdToProcName, edgeWeights.toMap)
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

  def assignProcessesToCoresILP(
      privilegedProcId: ProcId,
      privilegedCoreId: CoreId,
      processGraph: ProcessGraph,
      procEdgeWeights: Map[ProcEdge, Int],
      coreGraph: CoreGraph,
      pathIdToPath: Map[CorePathId, CorePath]
  )(implicit
      ctx: AssemblyContext
  ): (
      Map[ProcId, CoreId],
      Map[CorePath.PathEdge, Int]
  ) = {
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
        // (7) The privileged process must be mapped to the privileged core.
        //
        //        x_privilegedProcess_privilegedCore = 1
        //
        val (lowerBound, upperBound) = if ((procId == privilegedProcId) && (coreId == privilegedCoreId)) {
          (1, 1)
        } else {
          (0, 1)
        }
        vars_xPrC += (procId, coreId) -> solver.makeIntVar(lowerBound, upperBound, s"x_${procId}_${coreId}")
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
        val ySePa        = solver.makeIntVar(0, 1, ySePaVarName)
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
        val ySePa_greaterEqualSrcDst_constraint =
          solver.makeConstraint(-1, 1, s"${ySePaVarName}_greaterEqualSrcDst_constraint")
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
    // The maximum admissible weight of a core edge is the sum of all edge weights.
    val maxWeight = procEdgeWeights.values.sum
    coreGraph.edgeSet().asScala.foreach { e =>
      val src      = coreGraph.getEdgeSource(e)
      val dst      = coreGraph.getEdgeTarget(e)
      val coreEdge = CorePath.PathEdge(src, dst)

      // Define auxiliary binary variable w_ce. The upper limit is set to +infinity as
      // as arbitrary number of messages can transit over the edge.
      val wCeVarName = s"w_${coreEdge}"
      val wCe        = solver.makeIntVar(0, maxWeight, wCeVarName)
      vars_wCe += coreEdge -> wCe

      // Define constraints.
      // The equality constraint is modeled with a >= and <= constraint.

      // (a) 0 <= w_ce - \sum{...} <= +infinity
      val wCe_greaterEqual_constraint =
        solver.makeConstraint(0, MPSolver.infinity(), s"${wCeVarName}_greaterEqual_constraint")
      // (b) -infinity <= w_ce - \sum{...} <= 0
      val wCe_lessEqual_constraint =
        solver.makeConstraint(-MPSolver.infinity(), 0, s"${wCeVarName}_lessEqual_constraint")

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

    // (6) Create auxiliary variable max_w \in {0, infinity} which keeps track of the maximum
    //     core edge weight.
    //
    //        max_w >= max{ce \in CE} {w_ce}
    //
    //     This can be modeled as
    //
    //        max_w >= w_ce   \forall ce \in CE
    //        <==>
    //        0 <= max_w - w_ce <= +infinity
    //
    val maxW = solver.makeIntVar(0, MPSolver.infinity(), s"max_w")
    coreGraph.edgeSet().asScala.foreach { e =>
      val src      = coreGraph.getEdgeSource(e)
      val dst      = coreGraph.getEdgeTarget(e)
      val coreEdge = CorePath.PathEdge(src, dst)
      val wCe      = vars_wCe(coreEdge)

      val constraint = solver.makeConstraint(0, MPSolver.infinity(), s"max_w_greaterThan_w_${coreEdge}")
      constraint.setCoefficient(maxW, 1)
      constraint.setCoefficient(wCe, -1)
    }

    // objective function
    // ------------------
    //
    // Minimizes the total on-chip NoC message traffic.
    //
    //     minimize {max_w}
    //
    val objective = solver.objective()
    objective.setCoefficient(maxW, 1)
    objective.setMinimization()

    // solve optimization problem
    val resultStatus = ctx.stats.recordRunTime("solver.solve") {
      solver.solve()
    }

    // Extract results
    if ((resultStatus == MPSolver.ResultStatus.OPTIMAL) || (resultStatus == MPSolver.ResultStatus.FEASIBLE)) {
      // process-to-core assignments.
      val assignments = vars_xPrC
        .filter { case ((procId, coreId), xPrC) =>
          xPrC.solutionValue() > 0
        }
        .keySet
        .toMap

      val coreEdgeWeights = vars_wCe.map { case (coreEdge, wCe) =>
        coreEdge -> wCe.solutionValue.toInt
      }.toMap

      val assignmentsStr = assignments.mkString("\n")

      ctx.logger.info {
        s"Solved process-to-core placement problem in ${solver.wallTime()}ms\n" +
          s"Optimal assignment with total on-chip traffic of ${objective.value().toInt} is:\n${assignmentsStr}"
      }

      (assignments, coreEdgeWeights)

    } else {
      ctx.logger.fail(
        s"Could not solve process-to-core placement problem (resultStatus = ${resultStatus})."
      )

      (Map.empty, Map.empty)
    }
  }

  def assignProcessesToCoresCpSat(
      privilegedProcId: ProcId,
      privilegedCoreId: CoreId,
      processGraph: ProcessGraph,
      procEdgeWeights: Map[ProcEdge, Int],
      coreGraph: CoreGraph,
      pathIdToPath: Map[CorePathId, CorePath]
  )(implicit
      ctx: AssemblyContext
  ): (
      Map[ProcId, CoreId],
      Map[CorePath.PathEdge, Int]
  ) = {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // ILP formulation for core process-to-core assignment.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    Loader.loadNativeLibraries()

    val model = new CpModel()

    // (1) Create binary variable x_pr_c \in {0, 1} \forall (pr, c) \in PR x C.
    //
    //       x_pr_c = 1 if process pr is mapped to core c.
    //
    val vars_xPrC = MHashMap.empty[(ProcId, CoreId), Literal]
    processGraph.vertexSet().asScala.foreach { procId =>
      coreGraph.vertexSet().asScala.foreach { coreId =>
        vars_xPrC += (procId, coreId) -> model.newBoolVar(s"x_${procId}_${coreId}")
      }
    }

    // (7) The privileged process must be mapped to the privileged core.
    //
    //        x_privilegedProcess_privilegedCore = 1
    val xPrivilegedProcPrivilegedCore = vars_xPrC((privilegedProcId, privilegedCoreId))
    model.addEquality(xPrivilegedProcPrivilegedCore, 1)

    // (2) A process is assigned to exactly 1 core.
    //
    //       \sum{c \in C} {x_pr_c} = 1   \forall pr \in PR
    //
    processGraph.vertexSet().asScala.foreach { targetProcId =>
      val lits = vars_xPrC.collect {
        case ((procId, _), lit) if procId == targetProcId => lit
      }
      model.addExactlyOne(lits.asJava)
    }

    // (3) A core can be assigned at most 1 process. We use "at most" instead of "exactly"
    //     as the |C| >= |PR|.
    //
    //       \sum{pr \in PR} {x_pr_c} <= 1   \forall c \in C
    //
    coreGraph.vertexSet().asScala.foreach { targetCoreId =>
      val lits = vars_xPrC.collect {
        case ((_, coreId), lit) if coreId == targetCoreId => lit
      }
      model.addAtMostOne(lits.asJava)
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

    // se \in SE is defined by the process graph edge.
    val vars_ySePa = MHashMap.empty[(ProcEdge, CorePathId), Literal]
    procEdgeWeights.foreach { case (procEdge, weight) =>
      pathIdToPath.foreach { case (pathId, path) =>
        val xSeSrcPaSrc = vars_xPrC((procEdge.src, path.src))
        val xSeDstPaDst = vars_xPrC((procEdge.dst, path.dst))

        // Define auxiliary binary variable y_se_pa.
        val ySePa = model.newBoolVar(s"y_${procEdge}_${pathId}")
        vars_ySePa += (procEdge, pathId) -> ySePa

        // We create a channeling constraint by modeling the AND of the literals as their sum
        // needing to be 2. Note that it is important to model the ->  and <- directions for the
        // channeling constraints to represent <-> (if and only if).
        val linExpr = LinearExpr.sum(Array(xSeSrcPaSrc, xSeDstPaDst))
        model.addEquality(linExpr, 2).onlyEnforceIf(ySePa)
        model.addLessThan(linExpr, 2).onlyEnforceIf(ySePa.not())
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

    val vars_wCe = MHashMap.empty[CorePath.PathEdge, LinearArgument]
    // The maximum admissible weight of a core edge is the sum of all edge weights.
    val maxWeight = procEdgeWeights.values.sum
    coreGraph.edgeSet().asScala.foreach { e =>
      val src      = coreGraph.getEdgeSource(e)
      val dst      = coreGraph.getEdgeTarget(e)
      val coreEdge = CorePath.PathEdge(src, dst)

      // Define auxiliary binary variable w_ce. The upper limit is set to +infinity as
      // as arbitrary number of messages can transit over the edge.
      val wCe = model.newIntVar(0, maxWeight, s"w_${coreEdge}")
      vars_wCe += coreEdge -> wCe

      // Linear expression for the RHS nested sum.
      val exprs  = ArrayBuffer.empty[LinearArgument]
      val coeffs = ArrayBuffer.empty[Long]
      procEdgeWeights.foreach { case (procEdge, weight) =>
        pathIdToPath.foreach { case (pathId, path) =>
          val ySePa = vars_ySePa((procEdge, pathId))
          if (path.contains(coreEdge)) {
            exprs += ySePa
            coeffs += weight
          }
        }
      }
      val linExpr = LinearExpr.weightedSum(exprs.toArray, coeffs.toArray)
      model.addEquality(wCe, linExpr)
    }

    // (6) Create auxiliary variable max_w \in {0, infinity} which keeps track of the maximum
    //     core edge weight.
    //
    //        max_w >= max{ce \in CE} {w_ce}
    //
    val maxW = model.newIntVar(0, maxWeight, s"max_w")
    model.addMaxEquality(maxW, vars_wCe.values.toArray)

    // objective function
    // ------------------
    //
    // Minimizes the largest core edge communication cost.
    //
    //     minimize {max_w}
    //
    model.minimize(maxW)

    // solve optimization problem
    val solver = new CpSolver()
    solver.getParameters().setMaxTimeInSeconds(ctx.placement_timeout_s);
    val resultStatus = ctx.stats.recordRunTime("solver.solve") {
      solver.solve(model)
    }

    // Extract results
    if ((resultStatus == CpSolverStatus.OPTIMAL) || (resultStatus == CpSolverStatus.FEASIBLE)) {
      // process-to-core assignments.
      val assignments = vars_xPrC
        .filter { case ((procId, coreId), xPrC) =>
          solver.value(xPrC) > 0
        }
        .keySet
        .toMap

      val coreEdgeWeights = vars_wCe.map { case (coreEdge, wCe) =>
        coreEdge -> solver.value(wCe).toInt
      }.toMap

      val assignmentsStr = assignments.mkString("\n")

      val solveCategoryStr = if (resultStatus == CpSolverStatus.OPTIMAL) "optimal" else "feasible"
      ctx.logger.info {
        s"Solved process-to-core placement problem in ${solver.wallTime()}ms (${solveCategoryStr})\n" +
          s"Assignment with total on-chip traffic of ${solver.objectiveValue.toInt} is:\n${assignmentsStr}"
      }

      (assignments, coreEdgeWeights)

    } else {
      ctx.logger.fail(
        s"Could not solve process-to-core placement problem (resultStatus = ${resultStatus})."
      )

      (Map.empty, Map.empty)
    }
  }

  override def transform(program: DefProgram)(implicit ctx: AssemblyContext): DefProgram = {

    val (processGraph, procIdToProcName, procEdgeWeights) = getProcessGraph(program)
    val procNameToProcId                                  = procIdToProcName.map(x => x.swap)

    val coreGraph = getCoreGraph(ctx.max_dimx, ctx.max_dimy)

    val pathIdToPath = enumeratePaths(ctx.max_dimx, ctx.max_dimy)

    ctx.logger.dumpArtifact("process_graph.dot") {
      val weights = procEdgeWeights.map { case (procEdge, weight) =>
        (procEdge.src, procEdge.dst) -> weight
      }
      dumpGraph(processGraph, weights, procIdToProcName)
    }

    val privilegedProcs = program.processes.filter { proc =>
      proc.body.exists {
        case (_: Expect | _: Interrupt | _: GlobalLoad | _: GlobalStore | _: PutSerial) => true
        case _                                                                          => false
      }
    }
    assert(
      privilegedProcs.size == 1,
      s"Error: Found ${privilegedProcs.size} privileged processes, but expected to only find 1."
    )
    val privilegedProcId = procNameToProcId(privilegedProcs.head.id.id)
    val privilegedCoreId = CoreId(0, 0)
    ctx.logger.info(s"Privileged process has process id ${privilegedProcId}")

    // val (procIdToCoreId, coreEdgeWeights) = assignProcessesToCoresILP(privilegedProcId, privilegedCoreId, processGraph, procEdgeWeights, coreGraph, pathIdToPath)
    val (procIdToCoreId, coreEdgeWeights) = assignProcessesToCoresCpSat(
      privilegedProcId,
      privilegedCoreId,
      processGraph,
      procEdgeWeights,
      coreGraph,
      pathIdToPath
    )

    ctx.logger.dumpArtifact("core_graph.dot") {
      dumpCoreGraph(procIdToProcName, procIdToCoreId, coreEdgeWeights, ctx.max_dimx, ctx.max_dimy)
    }

    val oldProcNameToNewProcId = procIdToCoreId.map { case (procId, CoreId(x, y)) =>
      val procName = procIdToProcName(procId)
      procName -> ProcessIdImpl(s"placed_X${x}_Y${y}", x, y)
    }.toMap

    val placedProcs = program.processes.map { proc =>
      val renamed = proc.body.map {
        case send: Send => send.copy(dest_id = oldProcNameToNewProcId(send.dest_id.id)).setPos(send.pos)
        case other      => other
      }
      proc.copy(id = oldProcNameToNewProcId(proc.id.id), body = renamed).setPos(proc.pos)
    }

    program.copy(processes = placedProcs).setPos(program.pos)
  }

  // This dumps an arbitrary graph. No layout modifications are performed.
  def dumpGraph[E](
      g: Graph[ProcId, E],
      weights: Map[(ProcId, ProcId), Int] = Map.empty[(ProcId, ProcId), Int],
      procIdToProcName: Map[ProcId, String] = Map.empty[ProcId, String]
  ): String = {
    def escape(s: String): String   = s.replaceAll("\"", "\\\\\"")
    def quote(s: String): String    = s"\"${s}\""
    def getLabel(v: ProcId): String = quote(escape(procIdToProcName.getOrElse(v, v.toString())))

    val dotLines = ArrayBuffer.empty[String]

    // Vertices in a graph may not be legal graphviz identifiers, so we give
    // them all an Int id.
    val vToVid = g.vertexSet().asScala.zipWithIndex.toMap

    dotLines.append("digraph {")

    // Dump vertices.
    g.vertexSet().asScala.foreach { v =>
      val vId    = vToVid(v)
      val vLabel = getLabel(v)
      dotLines.append(s"\t${vId} [label=${vLabel}]")
    }

    // Dump edges.
    g.edgeSet().asScala.foreach { e =>
      val src = g.getEdgeSource(e)
      val dst = g.getEdgeTarget(e)
      val weight = weights.get((src, dst)) match {
        case Some(w) => w.toString()
        case None    => ""
      }

      val srcId = vToVid(src)
      val dstId = vToVid(dst)

      dotLines.append(s"\t${srcId} -> ${dstId} [label=${weight}]")
    }

    dotLines.append("}") // digraph
    dotLines.mkString("\n")
  }

  // This dumps the core graph. It is specifically formatted to be a grid (using various graphviz tricks).
  def dumpCoreGraph[V, E](
      procIdToProcName: Map[ProcId, String],
      procIdToCoreId: Map[ProcId, CoreId],
      coreEdgeWeights: Map[CorePath.PathEdge, Int],
      maxDimX: Int,
      maxDimY: Int
  ): String = {
    val dotLines = ArrayBuffer.empty[String]

    // Vertices in a graph may not be legal graphviz identifiers, so we give
    // them all an Int id. Note that we create two additional rows and columns
    // of cores. These cores will be made invisible and are used to draw the
    // "backwards" edges in the grid that exist due to the torus structure.
    // We use these invisible cores as otherwise the rendered graph will be
    // illegible with the backwards edges.
    val coreIdToVId = Range(-1, maxDimX + 1)
      .flatMap { x =>
        Range(-1, maxDimY + 1).map { y =>
          CoreId(x, y)
        }
      }
      .zipWithIndex
      .toMap

    val maxWeight = coreEdgeWeights.values.max
    val weightHeatMap = coreEdgeWeights.values.map { weight =>
      val weightNormalized = weight / maxWeight.toDouble
      val color            = HeatmapColor(weightNormalized)
      weight -> color
    }.toMap

    def isInvisible(coreId: CoreId): Boolean =
      (coreId.x == -1) || (coreId.x == maxDimX) || (coreId.y == -1) || (coreId.y == maxDimY)

    val coreIdToProcId = procIdToCoreId.map(x => x.swap)

    val (xEdges, yEdges) = coreEdgeWeights.partition { case (coreEdge, weight) =>
      val (src, dst) = (coreEdge.src, coreEdge.dst)
      src.y == dst.y
    }

    val (xForwardEdges, xBackwardEdges) = xEdges.partition { case (coreEdge, weight) =>
      val (src, dst) = (coreEdge.src, coreEdge.dst)
      (src.x < dst.x)
    }

    val (yForwardEdges, yBackwardEdges) = yEdges.partition { case (coreEdge, weight) =>
      val (src, dst) = (coreEdge.src, coreEdge.dst)
      (src.y < dst.y)
    }

    dotLines.append("digraph {")

    // Dump core vertices.
    coreIdToVId.foreach { case (coreId, vId) =>
      // Some cores may have no process assigned to them, hence why we use .get() to access the process id.
      val procName = coreIdToProcId.get(coreId) match {
        case Some(procId) => procIdToProcName(procId)
        case None         => "N/A" // This core was not assigned a process.
      }
      if (isInvisible(coreId)) {
        dotLines.append(s"\t${vId} [style=invis]")
      } else {
        dotLines.append(s"\t${vId} [label=\"${coreId}\n${procName}\"]")
      }
    }

    // Place all vertices at the same y-coordinate in the same rank so the grid is enforced.
    coreIdToVId.keys
      .groupBy { coreId =>
        coreId.y
      }
      .foreach { case (y, coreIds) =>
        val sameRankVIds = coreIds.map(coreId => coreIdToVId(coreId))
        dotLines.append(s"\trank=\"same\" {${sameRankVIds.mkString(", ")}}")
      }

    // Dump edges.
    // I explicitly omit the backward edges (torus) as they make the grid layout unreadable.
    (xForwardEdges ++ yForwardEdges).foreach { case (coreEdge, weight) =>
      val (src, dst)       = (coreEdge.src, coreEdge.dst)
      val (srcVId, dstVId) = (coreIdToVId(src), coreIdToVId(dst))
      val color            = weightHeatMap(weight).toCssHexString()
      val style            = if (weight == 0) "style=invis" else ""
      dotLines.append(s"\t${srcVId} -> ${dstVId} [label=\"${weight}\" color=\"${color}\" ${style}]")
    }

    // Drawing the x-axis backward edges will result in the grid layout being unreadable.
    // We instead use the invisible vertex before/after the first/last vertex in a row
    // to model these backward edges.
    xBackwardEdges.foreach { case (coreEdge, weight) =>
      val y = coreEdge.src.y

      // We create 2 edges for this backward edge.
      // 1. Edge from invisible vertex X<-1>_Y<y> to X<0>_Y<y>
      // 2. Edge from X<maxDimX-1>_Y<y> to invisible vertex X<maxDimX>_Y<y>
      val invisHeadSrc = coreIdToVId(CoreId(-1, y))
      val invisHeadDst = coreIdToVId(CoreId(0, y))
      val invisLastSrc = coreIdToVId(CoreId(maxDimX - 1, y))
      val invisLastDst = coreIdToVId(CoreId(maxDimX, y))

      val color = weightHeatMap(weight).toCssHexString()
      val style = if (weight == 0) "style=invis" else "style=dashed"
      dotLines.append(s"\t${invisHeadSrc} -> ${invisHeadDst} [label=${weight} color=\"${color}\" ${style}]")
      dotLines.append(s"\t${invisLastSrc} -> ${invisLastDst} [label=${weight} color=\"${color}\" ${style}]")
    }

    // Drawing the y-axis backward edges will result in the grid layout being unreadable.
    // We instead use the invisible vertex before/after the first/last vertex in a column
    // to model these backward edges.
    yBackwardEdges.foreach { case (coreEdge, weight) =>
      val x = coreEdge.src.x

      // We create 2 edges for this backward edge.
      // 1. Edge from invisible vertex X<x>_Y<-1> to X<x>_Y<0>
      // 2. Edge from X<x>_Y<maxDimY-1> to invisible vertex X<x>_Y<maxDimY>
      val invisHeadSrc = coreIdToVId(CoreId(x, -1))
      val invisHeadDst = coreIdToVId(CoreId(x, 0))
      val invisLastSrc = coreIdToVId(CoreId(x, maxDimY - 1))
      val invisLastDst = coreIdToVId(CoreId(x, maxDimY))

      val color = weightHeatMap(weight).toCssHexString()
      val style = if (weight == 0) "style=invis" else "style=dashed"
      dotLines.append(s"\t${invisHeadSrc} -> ${invisHeadDst} [label=${weight} color=\"${color}\" ${style}]")
      dotLines.append(s"\t${invisLastSrc} -> ${invisLastDst} [label=${weight} color=\"${color}\" ${style}]")
    }

    dotLines.append("}") // digraph
    dotLines.mkString("\n")
  }
}

object AnalyticalPlacerTest extends App {
  import AnalyticalPlacerTransform._

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
