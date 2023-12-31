package manticore.compiler.assembly.levels.placed.parallel

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
import manticore.compiler.assembly.levels.placed.PlacedIRTransformer
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
// - DIST(PA) = {1 .. |X|-1 +|Y|-1}
//
//     - DIST(pa) = number of hops on path pa.
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

// The formulation above is impossible to solve for a 4x4-sized array and above.
// An alternative formulation is to replace (5-7) with the following which scales to
// 5x5 arrays in ~60s, but cannot scale further:

// (5) Create variables d_pa \in {0 .. |X|-1 + |Y|-1} which measure the distance in core hops
//     on a path.
//
//        d_pa = \sum{se \in SE} {
//          y_se_pa * DIST(pa)
//        }
//
//        \forall pa \in PA
//
// (6) Create auxiliary variable max_d \in {0 .. |X|-1 + |Y|-1} which keeps track of the maximum
//     path distance.
//
//        max_d = max{pa \in PA} {d_pa}
//
// objective function
// ------------------
//
// Minimizes the longest chosen path in the core graph.
//
//     minimize {max_D}
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

    def numHops: Int = cores.size

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

  def boolToInt(b: Boolean) = if (b) 1 else 0

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

  def distance(
      src: CoreId,
      dst: CoreId,
      maxDimX: Int,
      maxDimY: Int
  ): Int = {
    val path = CorePath(src, dst, maxDimX, maxDimY)
    path.numHops
  }

  def getCoreEdgeWeights(
      procEdgeWeights: Map[ProcEdge, Int],
      procIdToCoreId: Map[ProcId, CoreId],
      maxDimX: Int,
      maxDimY: Int
  ): Map[CorePath.PathEdge, Int] = {
    val coreEdgeWeights = MHashMap.empty[CorePath.PathEdge, Int]

    procEdgeWeights.foreach { case (procEdge, weight) =>
      val srcCoreId = procIdToCoreId(procEdge.src)
      val dstCoreId = procIdToCoreId(procEdge.dst)
      val path      = CorePath(srcCoreId, dstCoreId, maxDimX, maxDimY)

      path.edges.foreach { corePath =>
        val oldWeight = coreEdgeWeights.getOrElse(corePath, 0)
        val newWeight = oldWeight + weight
        coreEdgeWeights += corePath -> newWeight
      }
    }

    coreEdgeWeights.toMap
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
  ): Map[ProcId, CoreId] = {
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
    // coreGraph.edgeSet.asScala.foreach { e =>
    //   val src      = coreGraph.getEdgeSource(e)
    //   val dst      = coreGraph.getEdgeTarget(e)
    //   val coreEdge = CorePath.PathEdge(src, dst)
    //   val wCe      = vars_wCe(coreEdge)
    //   objective.setCoefficient(wCe, 1)
    // }
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

      ctx.logger.debug {
        vars_xPrC
          .map { case ((procId, coreId), xPrC) =>
            s"${procId} -> ${coreId} -> ${xPrC.solutionValue()}"
          }
          .mkString("\n")
      }

      ctx.logger.debug {
        vars_ySePa
          .map { case ((procEdge, coreEdgeId), ySePa) =>
            s"${procEdge} -> ${ySePa.solutionValue()}"
          }
          .mkString("\n")
      }

      ctx.logger.debug {
        val assignmentsStr = assignments.toSeq
          .sortBy { case ((procId, coreId)) =>
            procId
          }
          .map { case ((procId, coreId)) =>
            s"ProcId ${procId} -> Core ${coreId}"
          }
          .mkString("\n")

        assignmentsStr
      }

      ctx.logger.debug {
        s"Objective function value = ${objective.value()}"
      }

      ctx.logger.info {
        s"Solved process-to-core placement problem in ${solver.wallTime()}ms"
      }

      assignments

    } else {
      ctx.logger.fail(
        s"Could not solve process-to-core placement problem (resultStatus = ${resultStatus})."
      )

      Map.empty
    }
  }

  def assignProcessesToCoresCpSat(
      privilegedProcId: ProcId,
      privilegedCoreId: CoreId,
      processGraph: ProcessGraph,
      procEdgeWeights: Map[ProcEdge, Int],
      coreGraph: CoreGraph,
      pathIdToPath: Map[CorePathId, CorePath],
      initialSolutionHint: Map[ProcId, CoreId]
  )(implicit
      ctx: AssemblyContext
  ): Map[ProcId, CoreId] = {
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // ILP formulation for core process-to-core assignment.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    Loader.loadNativeLibraries()

    val model = new CpModel()

    // (1) Create binary variable x_pr_c \in {0, 1} \forall (pr, c) \in PR x C.
    //
    //       x_pr_c = 1 if process pr is mapped to core c.
    //
    val vars_xPrC  = MHashMap.empty[(ProcId, CoreId), BoolVar]
    val hints_xPrC = MHashMap.empty[(ProcId, CoreId), Boolean]
    processGraph.vertexSet().asScala.foreach { procId =>
      coreGraph.vertexSet().asScala.foreach { coreId =>
        val xPrC = model.newBoolVar(s"x_${procId}_${coreId}")
        vars_xPrC += (procId, coreId) -> xPrC

        // Giving a hint to the solver is necessary as otherwise it fails to find a solution
        // even before the timeout threshold is set.
        val hint = initialSolutionHint(procId) == coreId
        hints_xPrC += (procId, coreId) -> hint
      }
    }

    // (7) The privileged process must be mapped to the privileged core.
    //
    //        x_privilegedProcess_privilegedCore = 1
    //
    val xPrivilegedProcPrivilegedCore = vars_xPrC((privilegedProcId, privilegedCoreId))
    model.addEquality(xPrivilegedProcPrivilegedCore, 1)

    // (2) A process is assigned to exactly 1 core.
    //
    //       \sum{c \in C} {x_pr_c} = 1   \forall pr \in PR
    //
    processGraph.vertexSet().asScala.foreach { targetProcId =>
      // model.addExactlyOne expects a Literal, not a BoolVar. In scala
      // you can pass a Array[BoolVar] to a method that expects a Array[Literal],
      // but not in java as java's collections are not co-variant.
      // Therefore I enfore the conversion type to Array[Literal].
      val lits = vars_xPrC
        .collect {
          case ((procId, _), lit) if procId == targetProcId => lit
        }
        .toArray[Literal]
      model.addExactlyOne(lits)
    }

    // (3) A core can be assigned at most 1 process. We use "at most" instead of "exactly"
    //     as the |C| >= |PR|.
    //
    //       \sum{pr \in PR} {x_pr_c} <= 1   \forall c \in C
    //
    coreGraph.vertexSet().asScala.foreach { targetCoreId =>
      // model.addAtMostOne expects a Literal, not a BoolVar. In scala
      // you can pass a Array[BoolVar] to a method that expects a Array[Literal],
      // but not in java as java's collections are not co-variant.
      // Therefore I enfore the conversion type to Array[Literal].
      val lits = vars_xPrC
        .collect {
          case ((_, coreId), lit) if coreId == targetCoreId => lit
        }
        .toArray[Literal]
      model.addAtMostOne(lits)
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
    val vars_ySePa  = MHashMap.empty[(ProcEdge, CorePathId), BoolVar]
    val hints_ySePa = MHashMap.empty[(ProcEdge, CorePathId), Boolean]
    procEdgeWeights.foreach { case (procEdge, weight) =>
      pathIdToPath.foreach { case (pathId, path) =>
        val xSeSrcPaSrc = vars_xPrC((procEdge.src, path.src))
        val xSeDstPaDst = vars_xPrC((procEdge.dst, path.dst))

        // Define auxiliary binary variable y_se_pa.
        val ySePa = model.newBoolVar(s"y_${procEdge}_${pathId}")
        vars_ySePa += (procEdge, pathId) -> ySePa

        // Giving a hint to the solver is necessary as otherwise it fails to find a solution
        // even before the timeout threshold is set.
        val hint = hints_xPrC((procEdge.src, path.src)) && hints_xPrC((procEdge.dst, path.dst))
        hints_ySePa += (procEdge, pathId) -> hint

        // Option 1:
        // We create a channeling constraint by modeling the AND of the literals as their sum
        // needing to be 2. Note that it is important to model the ->  and <- directions for the
        // channeling constraints to represent <-> (if and only if).
        //
        // val linExpr = LinearExpr.sum(Array(xSeSrcPaSrc, xSeDstPaDst))
        // model.addEquality(linExpr, 2).onlyEnforceIf(ySePa)
        // model.addLessThan(linExpr, 2).onlyEnforceIf(ySePa.not())

        // Option 2:
        // We create a channeling constraint using boolean logic.
        //
        // x_(se.src)_(pa.src) ^ x_(se.dst)_(pa.dst) => y_se_pa
        // <==>
        // NOT { x_(se.src)_(pa.src) AND x_(se.dst)_(pa.dst) } OR y_se_pa
        // <==>
        // NOT { x_(se.src)_(pa.src) } or NOT { x_(se.dst)_(pa.dst) } or y_se_pa
        model.addBoolOr(Array(xSeSrcPaSrc.not(), xSeDstPaDst.not(), ySePa))
        //
        // y_se_pa => x_(se.src)_(pa.src) ^ x_(se.dst)_(pa.dst)
        // <==>
        // y_se_pa => x_(se.src)_(pa.src)
        // y_se_pa => x_(se.dst)_(pa.dst)
        model.addImplication(ySePa, xSeSrcPaSrc)
        model.addImplication(ySePa, xSeDstPaDst)
      }
    }

    // (5) Create variables d_pa \in {0 .. |X|-1 + |Y|-1} which measure the distance in core hops
    //     on a path.
    //
    //        d_pa = \sum{se \in SE} {
    //          y_se_pa * DIST(pa)
    //        }
    //
    //        \forall pa \in PA
    //
    val vars_dPa  = MHashMap.empty[CorePathId, IntVar]
    val hints_dPa = MHashMap.empty[CorePathId, Int]
    val minDist   = 0 // Using 1 yields infeasible solutions, but I can't tell why?
    val maxDist   = (ctx.hw_config.dimX - 1) + (ctx.hw_config.dimY - 1)
    pathIdToPath.foreach { case (pathId, path) =>
      val dPa = model.newIntVar(minDist, maxDist, s"d_${pathId}")
      vars_dPa += pathId -> dPa

      val exprs      = ArrayBuffer.empty[BoolVar]
      val coeffs     = ArrayBuffer.empty[Long]
      val hintExprs  = ArrayBuffer.empty[Int]
      val hintCoeffs = ArrayBuffer.empty[Int]
      procEdgeWeights.foreach { case (procEdge, _) =>
        val ySePa = vars_ySePa((procEdge, pathId))
        exprs += ySePa
        coeffs += path.numHops
        hintExprs += boolToInt(hints_ySePa((procEdge, pathId)))
        hintCoeffs += path.numHops
      }

      val linExpr = LinearExpr.weightedSum(exprs.toArray, coeffs.toArray)
      model.addEquality(dPa, linExpr)

      val hint = hintCoeffs.zip(hintExprs).map { case (coeff, expr) => coeff * expr }.sum
      hints_dPa += pathId -> hint
    }

    // (6) Create auxiliary variable max_d \in {0 .. |X|-1 + |Y|-1} which keeps track of the maximum
    //     path distance.
    //
    //        max_d = max{pa \in PA} {d_pa}
    //
    val maxD = model.newIntVar(minDist, maxDist, s"max_d")
    // Note that using toArray does not work here for some reason, but toSeq does.
    model.addMaxEquality(maxD, vars_dPa.values.toSeq.asJava)
    // Giving a hint to the solver is necessary as otherwise it fails to find a solution
    // even before the timeout threshold is set.
    val hint_maxD = hints_dPa.maxBy { case (pathId, weight) => weight }._2

    // objective function
    // ------------------
    //
    // Minimizes the longest chosen path in the core graph.
    //
    //     minimize {max_D}
    //
    model.minimize(maxD)

    // Apply all hints to the solver.
    hints_xPrC.foreach { case ((procId, coreId), hint) =>
      val xPrC = vars_xPrC((procId, coreId))
      model.addHint(xPrC, boolToInt(hint))
    }
    hints_ySePa.foreach { case ((procEdge, pathId), hint) =>
      val ySePa = vars_ySePa((procEdge, pathId))
      model.addHint(ySePa, boolToInt(hint))
    }
    hints_dPa.foreach { case (pathId, weight) =>
      val dPa = vars_dPa(pathId)
      model.addHint(dPa, weight)
    }
    model.addHint(maxD, hint_maxD)

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

      val solveCategoryStr = if (resultStatus == CpSolverStatus.OPTIMAL) "optimal" else "feasible"
      ctx.logger.info {
        // CPSolver returns wall-clock time in SECONDS, not MILLISECONDS (MPSolver returns ms though).
        s"Solved process-to-core placement problem in ${solver.wallTime()}s (${solveCategoryStr})"
      }

      assignments

    } else {
      ctx.logger.fail(
        s"Could not solve process-to-core placement problem (resultStatus = ${resultStatus})."
      )

      Map.empty
    }
  }

  def assignProcessesToCoresRoundRobin(
      program: DefProgram,
      processGraph: ProcessGraph,
      procNameToProcId: Map[String, ProcId],
      coreGraph: CoreGraph,
      procEdgeWeights: Map[ProcEdge, Int],
      pathIdToPath: Map[CorePathId, CorePath]
  )(implicit
      ctx: AssemblyContext
  ): Map[ProcId, CoreId] = {

    if (ctx.use_loc) {
      import manticore.compiler.assembly.annotations.Loc
      val allHaveLoc = program.processes.forall { p =>
        p.annons.exists {
          case _: Loc => true
          case _      => false
        }
      }
      if (!allHaveLoc) {
        ctx.logger.fail("not all processes have @LOC annotation!")
      } else {
        program.processes.map { proc =>
          procNameToProcId(proc.id.id) -> proc.annons.collectFirst { case l: Loc => CoreId(l.getX(), l.getY()) }.get
        }.toMap
      }

    } else {
      val procIdToCoreId = program.processes
        .sortBy { proc =>
          proc.body.count {
            case _ @(_: Interrupt | _: GlobalLoad | _: GlobalStore | _: PutSerial) => true
            case _                                                                 => false
          }
        } {
          Ordering[Int].reverse
        }
        .zip {
          Range(0, ctx.hw_config.dimX).flatMap { x =>
            Range(0, ctx.hw_config.dimY).map { y =>
              CoreId(x, y)
            }
          }
        }
        .map { case (proc, coreId) =>
          val procName = proc.id.id
          val procId   = procNameToProcId(procName)
          procId -> coreId
        }
        .toMap

      procIdToCoreId
    }

  }

  override def transform(program: DefProgram)(implicit ctx: AssemblyContext): DefProgram = {

    val (processGraph, procIdToProcName, procEdgeWeights) = getProcessGraph(program)

    val procNameToProcId = procIdToProcName.map(x => x.swap)

    val coreGraph = getCoreGraph(ctx.hw_config.dimX, ctx.hw_config.dimY)

    val pathIdToPath = enumeratePaths(ctx.hw_config.dimX, ctx.hw_config.dimY)

    ctx.logger.dumpArtifact("process_graph.dot", forceDump = false) {
      val weights = procEdgeWeights.map { case (procEdge, weight) =>
        (procEdge.src, procEdge.dst) -> weight
      }
      dumpGraph(processGraph, weights, procIdToProcName)
    }

    val privilegedProcs = program.processes.filter { proc =>
      proc.body.exists {
        case (_: Interrupt | _: GlobalLoad | _: GlobalStore | _: PutSerial) => true
        case _                                                              => false
      }
    }
    assert(
      privilegedProcs.size == 1,
      s"Error: Found ${privilegedProcs.size} privileged processes, but expected to only find 1."
    )
    val privilegedProcId = procNameToProcId(privilegedProcs.head.id.id)
    val privilegedCoreId = CoreId(0, 0)
    ctx.logger.info(s"Privileged process has process id ${privilegedProcId}")

    // Round-robin assignemnt solution.
    val roundRobinAssignmentHint = assignProcessesToCoresRoundRobin(
      program,
      processGraph,
      procNameToProcId,
      coreGraph,
      procEdgeWeights,
      pathIdToPath
    )
    val procIdToCoreId = roundRobinAssignmentHint
    // ctx.logger.dumpArtifact(s"procIdToCoreId.txt", forceDump = false) {
    //   procIdToCoreId.mkString("\n")
    // }

    // val procIdToCoreId: Map[ProcId, CoreId] = Map(
    //   3 -> CoreId(0, 1),
    //   1 -> CoreId(0, 2),
    //   2 -> CoreId(1, 0),
    //   0 -> CoreId(1, 1),
    //   4 -> CoreId(1, 2),
    //   5 -> CoreId(2, 0),
    //   6 -> CoreId(2, 1),
    //   7 -> CoreId(2, 2),
    //   8 -> CoreId(0, 0)
    // )

    // val procIdToCoreId: Map[ProcId, CoreId] = Map(
    //   0  -> CoreId(0, 1),
    //   1  -> CoreId(0, 2),
    //   2  -> CoreId(0, 3),
    //   3  -> CoreId(1, 0),
    //   9  -> CoreId(1, 1),
    //   5  -> CoreId(1, 2),
    //   6  -> CoreId(1, 3),
    //   7  -> CoreId(2, 0),
    //   8  -> CoreId(2, 1),
    //   4  -> CoreId(2, 2),
    //   10 -> CoreId(2, 3),
    //   11 -> CoreId(3, 0),
    //   12 -> CoreId(3, 1),
    //   13 -> CoreId(3, 2),
    //   14 -> CoreId(0, 0),
    //   15 -> CoreId(3, 3)
    // )

    // // ILP-based solution.
    // val procIdToCoreId = assignProcessesToCoresILP(
    //   privilegedProcId,
    //   privilegedCoreId,
    //   processGraph,
    //   procEdgeWeights,
    //   coreGraph,
    //   pathIdToPath
    // )

    // // CP-SAT-based solution.
    // val procIdToCoreId = assignProcessesToCoresCpSat(
    //   privilegedProcId,
    //   privilegedCoreId,
    //   processGraph,
    //   procEdgeWeights,
    //   coreGraph,
    //   pathIdToPath,
    //   roundRobinAssignmentHint
    // )

    ctx.logger.dumpArtifact("core_graph.dot", forceDump = false) {
      val coreEdgeWeights = getCoreEdgeWeights(procEdgeWeights, procIdToCoreId, ctx.hw_config.dimX, ctx.hw_config.dimY)
      dumpCoreGraph(procIdToProcName, procIdToCoreId, coreEdgeWeights, ctx.hw_config.dimX, ctx.hw_config.dimY)
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

    val weightHeatMap = weights.values
      .toSet[Int]
      .map { weight =>
        val weightNormalized = weight / weights.values.max
        val color            = HeatmapColor(weightNormalized)
        weight -> color
      }
      .toMap

    dotLines.append("digraph {")

    // Dump vertices.
    g.vertexSet().asScala.foreach { v =>
      val vId    = vToVid(v)
      val vLabel = getLabel(v)
      dotLines.append(s"\t${vId} [label=${vLabel}]")
    }

    // Dump edges.
    g.edgeSet().asScala.foreach { e =>
      val src    = g.getEdgeSource(e)
      val dst    = g.getEdgeTarget(e)
      val weight = weights((src, dst))
      val color  = weightHeatMap(weight).toCssHexString()

      val srcId = vToVid(src)
      val dstId = vToVid(dst)

      dotLines.append(s"\t${srcId} -> ${dstId} [label=${weight} fontcolor=\"${color}\" color=\"${color}\"]")
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
    val coreIdToVId = Range
      .inclusive(-1, maxDimX)
      .flatMap { x =>
        Range.inclusive(-1, maxDimY).map { y =>
          CoreId(x, y)
        }
      }
      .zipWithIndex
      .toMap

    val weightHeatMap = coreEdgeWeights.values
      .toSet[Int]
      .map { weight =>
        val weightNormalized = weight / coreEdgeWeights.values.max
        val color            = HeatmapColor(weightNormalized)
        weight -> color
      }
      .toMap

    def isInvisible(coreId: CoreId): Boolean = {
      (coreId.x == -1) || (coreId.x == maxDimX) || (coreId.y == -1) || (coreId.y == maxDimY)
    }

    def isBackwardsEdge(path: CorePath.PathEdge): Boolean = {
      val isHorizBack = (path.src.x == -1 && path.dst.x == 0) || (path.src.x == maxDimX - 1 && path.dst.x == maxDimX)
      val isVertBack  = (path.src.y == -1 && path.dst.y == 0) || (path.src.y == maxDimY - 1 && path.dst.y == maxDimY)
      isHorizBack || isVertBack
    }

    val coreIdToProcId = procIdToCoreId.map(x => x.swap)

    // Initialize all edge weights to 0 if non-existant.
    // We will overwrite the weight of the backwards edges later.
    val edges = MHashMap.empty[CorePath.PathEdge, Int]
    Range
      .inclusive(-1, maxDimY - 1)
      .foreach { y =>
        Range.inclusive(-1, maxDimX - 1).foreach { x =>
          val src     = CoreId(x, y)
          val xDst    = CoreId(x + 1, y)
          val yDst    = CoreId(x, y + 1)
          val xPath   = CorePath.PathEdge(src, xDst)
          val yPath   = CorePath.PathEdge(src, yDst)
          val xWeight = coreEdgeWeights.getOrElse(xPath, 0)
          val yWeight = coreEdgeWeights.getOrElse(yPath, 0)
          edges += xPath -> xWeight
          edges += yPath -> yWeight
        }
      }

    // Check x backwards edges from (maxDimX-1, y) -> (0, y)
    // If they exist, then overwrite the following two edges:
    //
    //   (-1       , y) -> (0      , y)
    //   (maxDimX-1, y) -> (maxDimX, y)
    //
    Range.inclusive(0, maxDimY - 1).foreach { y =>
      val rightMostRealCore = CoreId(maxDimX - 1, y)
      val leftMostRealCore  = CoreId(0, y)
      coreEdgeWeights.get(CorePath.PathEdge(rightMostRealCore, leftMostRealCore)) match {
        case None         => // Nothing to do
        case Some(weight) =>
          // Overwrite the two fake edges we add to the left and right of the core grid so
          // it renders well.
          val rightMostFakeCore = CoreId(maxDimX, y)
          val leftMostFakeCore  = CoreId(-1, y)

          val leftPath  = CorePath.PathEdge(leftMostFakeCore, leftMostRealCore)
          val rightPath = CorePath.PathEdge(rightMostRealCore, rightMostFakeCore)

          edges += leftPath  -> weight
          edges += rightPath -> weight
      }
    }

    // Check y backwards edges from (x, maxDimY-1) -> (x, 0)
    // If they exist, then overwrite the following two edges:
    //
    //   (x,        -1) -> (x,       0)
    //   (x, maxDimY-1) -> (x, maxDimY)
    //
    Range.inclusive(0, maxDimX - 1).foreach { x =>
      val bottomMostRealCore = CoreId(x, maxDimY - 1)
      val topMostRealCore    = CoreId(x, 0)
      coreEdgeWeights.get(CorePath.PathEdge(bottomMostRealCore, topMostRealCore)) match {
        case None         => // Nothing to do
        case Some(weight) =>
          // Overwrite the two fake edges we add to the top and bottom of the core grid so
          // it renders well.
          val bottomMostFakeCore = CoreId(x, maxDimY)
          val topMostFakeCore    = CoreId(x, -1)

          val topPath    = CorePath.PathEdge(topMostFakeCore, topMostRealCore)
          val bottomPath = CorePath.PathEdge(bottomMostRealCore, bottomMostFakeCore)

          edges += topPath    -> weight
          edges += bottomPath -> weight
      }
    }

    dotLines.append("digraph {")

    // Dump core vertices.
    coreIdToVId.foreach { case (coreId, vId) =>
      val style = if (isInvisible(coreId)) "style=\"invis\"" else ""
      val shape = "shape=\"circle\""
      // Some cores may have no process assigned to them, hence why we use .get() to access the process id.
      val attributes = coreIdToProcId.get(coreId) match {
        case Some(procId) =>
          val procName = procIdToProcName(procId)
          s"[label=\"${coreId}\\n${procName}\" fontcolor=\"#000000ff\" color=\"#000000ff\" ${style} ${shape}]"
        case None =>
          // This core was not assigned a process, so we make the text transparent.
          s"[label=\"${coreId}\\nN/A\" fontcolor=\"#00000030\" color=\"#00000030\" ${style} ${shape}]"
      }

      dotLines.append(s"\t${vId} ${attributes}")
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

    // Dump core edges.
    edges.foreach { case (coreEdge, weight) =>
      val (src, dst)       = (coreEdge.src, coreEdge.dst)
      val (srcVId, dstVId) = (coreIdToVId(src), coreIdToVId(dst))

      val color = if (weight == 0) {
        ""
      } else {
        val c = weightHeatMap(weight).toCssHexString()
        s"color=\"${c}\" fontcolor=\"${c}\""
      }

      val style = if (weight == 0) {
        "style=invis"
      } else if (isBackwardsEdge(coreEdge)) {
        "style=dashed"
      } else {
        ""
      }

      dotLines.append(s"\t${srcVId} -> ${dstVId} [label=${weight} ${color} ${style}]")
    }

    dotLines.append("}") // digraph
    dotLines.mkString("\n")
  }
}
