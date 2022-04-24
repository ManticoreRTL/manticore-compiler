package manticore.compiler.assembly.levels.placed

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.BinaryOperator.{AND, OR, XOR}
import manticore.compiler.assembly.DependenceGraphBuilder
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.{Color, CyclicColorGenerator}
import org.jgrapht.{Graph, Graphs}
import org.jgrapht.alg.util.UnionFind
import org.jgrapht.graph.{AsSubgraph, DefaultEdge, DirectedAcyclicGraph}
import org.jgrapht.traverse.TopologicalOrderIterator

import java.io.StringWriter
import java.nio.file.{Files, Paths}
import java.util.function.Function
import scala.collection.immutable.{BitSet => Cut}
import scala.collection.mutable.{ArrayBuffer, HashMap => MHashMap, HashSet => MHashSet}
import scala.jdk.CollectionConverters._
import com.google.ortools.Loader
import com.google.ortools.linearsolver.{MPSolver, MPVariable}
import org.jgrapht.nio.Attribute
import scala.collection.mutable.LinkedHashMap
import org.jgrapht.nio.DefaultAttribute

object CustomLutInsertion
    extends DependenceGraphBuilder
    with AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {

  val flavor = PlacedIR
  import PlacedIR._

  // The primary inputs are *outside* the cone and feed the cone.
  case class Cone[V](root: V, primaryInputs: Set[V], vertices: Set[V]) {
    override def toString(): String = {
      val nonRootVertices = vertices - root
      s"(root = ${root}), (body = ${nonRootVertices.mkString(" --- ")}), (primary inputs = ${primaryInputs.mkString(" --- ")})"
    }

    def size: Int = vertices.size

    def contains(v: V): Boolean = {
      vertices.contains(v)
    }

    def exists(p: V => Boolean): Boolean = {
      vertices.exists(p)
    }

    // Useful for returning the cone with different names (or different types, like
    // for replacing a Cone[Int] with a Cone[Instruction]).
    def renameVertices[T](f: V => T): Cone[T] = {
      val rootRenamed = f(root)
      val pisRenamed = primaryInputs.map(v => f(v))
      val verticesRenamed = vertices.map(v => f(v))
      Cone(rootRenamed, pisRenamed, verticesRenamed)
    }
  }

  def dumpGraph[V](
    g: Graph[V, DefaultEdge],
    vColorMap: Map[V, Color] = Map.empty[V, Color]
  ): String = {
    val vertexIdProvider = new Function[V, String] {
      def apply(v: V): String = {
        val vStr = v.toString

        // Note the use of quotes around the serialized name so it is a legal graphviz identifier (as
        // characters such as "." are not allowed in identifiers).
        s"\"${vStr}\""
      }
    }

    val vertexAttrProvider = new Function[V, java.util.Map[String, Attribute]] {
      def apply(v: V): java.util.Map[String, Attribute] = {
        val attrMap = LinkedHashMap.empty[String, Attribute]

        // We color the vertices that will be fused into a custom instruction.
        if (vColorMap.contains(v)) {
          val color = vColorMap(v)
          attrMap += "fillcolor" -> DefaultAttribute.createAttribute(color.toCssHexString())
          attrMap += "style" -> DefaultAttribute.createAttribute("filled")
        }

        attrMap.asJava
      }
    }

    val dotExporter = new org.jgrapht.nio.dot.DOTExporter[V, DefaultEdge]()
    dotExporter.setVertexIdProvider(vertexIdProvider)
    dotExporter.setVertexAttributeProvider(vertexAttrProvider)

    val writer = new StringWriter()
    dotExporter.exportGraph(g, writer)
    writer.toString()
  }

  def createDependenceGraph(
    proc: DefProcess
  )(
    implicit ctx: AssemblyContext
  ): Graph[Instruction, DefaultEdge] = {
    val dependenceGraph = DependenceAnalysis.build(proc, (_, _) => None)(ctx)

    // Convert dependence graph to jgrapht
    val convertedGraph: Graph[Instruction, DefaultEdge] = new DirectedAcyclicGraph(classOf[DefaultEdge])

    dependenceGraph.nodes.foreach { inode =>
      convertedGraph.addVertex(inode.toOuter)
    }

    dependenceGraph.edges.foreach { iedge =>
      val srcInstr = iedge.source.toOuter
      val dstInstr = iedge.target.toOuter
      convertedGraph.addEdge(srcInstr, dstInstr)
    }

    convertedGraph
  }

  def isLogicInstr(instr: Instruction): Boolean = {
    instr match {
      case BinaryArithmetic(AND | OR | XOR, _, _, _, _) => true
      case _ => false
    }
  }

  def findLogicClusters(
    g: Graph[Instruction, DefaultEdge]
  ): Set[Set[Instruction]] = {
    // We use Union-Find to cluster logic instructions together.
    val uf = new UnionFind(g.vertexSet())

    // Merge the edges that link 2 logic instructions.
    g.edgeSet().asScala.foreach { e =>
      val src = g.getEdgeSource(e)
      val dst = g.getEdgeTarget(e)

      if (isLogicInstr(src) && isLogicInstr(dst)) {
        uf.union(src, dst)
      }
    }

    val logicClusters = g.vertexSet()
      .asScala // Returns a mutable.Set
      .toSet // Convert to immutable.Set
      .filter { v =>
        isLogicInstr(v)
      }
      .groupBy { v =>
        uf.find(v)
      }.values.toSet

    logicClusters
  }

  /**
    * Groups the instructions that make up the input clusters into a set of
    * non-overlapping cones that can be transformed into a custom instruction.
    * We solve the non-overlapping cone problem optimally with an ILP formulation.
    */
  def findOptimalConeCover(
    instructionGraph: Graph[Instruction, DefaultEdge],
    clusters: Set[Set[Instruction]],
    maxCutSize: Int,
    maxNumCones: Int
  )(
    implicit ctx: AssemblyContext
  ): Set[Cone[Instruction]] = {

    ////////////////////////////////////////////////////////////////////////////
    // Helpers /////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    // Returns a subgraph of g containing the given vertices *and* their primary inputs (which are not logic
    // instructions by definition). The primary inputs are needed to perform cut enumeration as otherwise we
    // would not know the in-degree of the first vertices in each logic cluster.
    def getClustersSubgraph[V](
      g: Graph[V, DefaultEdge],
      clusterVertices: Set[V]
    ): (
      Graph[V, DefaultEdge],
      Set[V] // primary inputs
    ) = {
      // The primary inputs are vertices *outside* the given set of vertices that are connected
      // to vertices *inside* the cluster.
      val primaryInputs = clusterVertices.flatMap { vInACluster =>
        Graphs.predecessorListOf(g, vInACluster).asScala
      }.filter { vAnywhere =>
        !clusterVertices.contains(vAnywhere)
      }

      val subgraphVertices = primaryInputs ++ clusterVertices
      val subgraph = new AsSubgraph(g, subgraphVertices.asJava)

      (subgraph, primaryInputs)
    }

    // Backtracks from the root until the cut vertices. All vertices encountered along the
    // way form a cone. The cut vertices themselves are not part of the cone and are instead
    // its "primary inputs".
    def cutToCone[V](
      g: Graph[V, DefaultEdge],
      root: V,
      cut: Set[V]
    ): Cone[V] = {
      val coneVertices = MHashSet.empty[V]

      // Backtrack from the given vertex until a vertex in the cut is seen.
      def backtrack(v: V): Unit = {
        if (!cut.contains(v)) {
          coneVertices += v
          Graphs.predecessorListOf(g, v).asScala.foreach(pred => backtrack(pred))
        }
      }

      backtrack(root)
      Cone(root, cut, coneVertices.toSet)
    }

    def findBestConeCover[V](
      vertices: Set[V],
      cones: Set[Cone[V]]
    ): Set[Cone[V]] = {
      // We want to maximize the number of instructions we can remove from the cluster.
      // The cones must be non-overlapping as otherwise a vertex *outside* the cone uses an intermediate
      // result from within the cone and we therefore can't remove the vertices that make up the cone.
      // In other words, the cones must be fanout-free.
      //
      // This is a proxy for the set cover problem with non-overlapping sets. We can solve this optimally
      // using an ILP formulation.

      // We assign every cone an ID so we can easily map them to a variable in the ILP formulation.
      val idToConeMap = cones.zipWithIndex.map(kv => kv.swap).toMap

      // objective function
      // ------------------
      // The objective function maximizes the number of instructions that we can save by
      // replacing a cone by a custom instruction. We save |C_i| - 1 vertices by replacing
      // the instructions of a cone with a single custom instruction.
      //
      //    max sum_{i = 0}{i = num_cones-1} x_i * (|C_i|-1)
      //
      // constraints
      // -----------
      //
      //    x_i \in {0, 1} \forall i \in [0 .. num_cones-1]        (1) Each cone is either selected, or not selected.
      //
      //    sum_{C_i : v \in C_i} x_i <= 1                         (2) Each vertex is covered by at most one cone.
      //
      //    sum_{i = 0}{i = num_cones-1} x_i <= maxNumCones        (3) Bound the number of custom instructions to force large cones to be generated.

      Loader.loadNativeLibraries()
      val solver = MPSolver.createSolver("SCIP")
      if (solver == null) {
        ctx.logger.fail("Could not create solver SCIP.")
      }

      // (1) Each cone is either selected, or not selected (binary variable).
      //
      //    x_i \in {0, 1} \forall i \in [0 .. num_cones-1]
      //
      val coneVars = MHashMap.empty[Int, MPVariable]
      idToConeMap.foreach { case (coneId, cone) =>
        coneVars += coneId -> solver.makeIntVar(0, 1, s"x_${coneId}")
      }

      // (2) Each vertex is covered by at most one cone.
      //
      //    sum_{C_i : v \in C_i} x_i <= 1
      //
      vertices.foreach { v =>
        // The inequality constraint is defined by setting the lower/upper bound of the
        // constraint to 0/1.
        val constraint = solver.makeConstraint(0, 1, s"covered_v${v}")

        idToConeMap.foreach { case (coneId, cone) =>
          val coefficient = if (cone.contains(v)) 1 else 0
          constraint.setCoefficient(coneVars(coneId), coefficient)
        }
      }

      // (3) Bound the number of custom instructions to force large cones to be generated.
      //
      //    sum_{i = 0}{i = num_cones-1} x_i <= maxNumCones
      //
      val maxNumConesConstraint = solver.makeConstraint(0, maxNumCones, s"maxNumCones_constraint")
      idToConeMap.foreach { case (coneId, cone) =>
        val coefficient = 1
        maxNumConesConstraint.setCoefficient(coneVars(coneId), coefficient)
      }

      // objective function
      //
      //    max sum_{i = 0}{i = num_cones-1} x_i * (|C_i|-1)
      //
      val objective = solver.objective()
      idToConeMap.foreach { case (coneId, cone) =>
        val coneSize = cone.vertices.size
        val coefficient = coneSize - 1
        objective.setCoefficient(coneVars(coneId), coefficient)
      }
      objective.setMaximization()

      // solve optimization problem
      val resultStatus = solver.solve()
      if (resultStatus == MPSolver.ResultStatus.OPTIMAL) {
        val conesUsed = coneVars.filter { case (coneId, coneVar) =>
          // A cone is used if it is part of the cone cover.
          coneVar.solutionValue.toInt > 0
        }.map { case (coneId, _) =>
          coneId -> idToConeMap(coneId)
        }.values.toSet

        ctx.logger.debug {
          val conesUsedStr = conesUsed.mkString("\n")
          s"Solved non-overlapping cone covering problem in ${solver.wallTime()}ms.\nCan reduce instruction count by ${objective.value().toInt} using ${conesUsed.size} cones:\n${conesUsedStr}"
        }

        conesUsed

      } else {
        ctx.logger.fail(s"Could not optimally solve cone cover problem (resultStatus = ${resultStatus}).")
        // We return the empty set to signal that no logic instructions could be fused together.
        Set.empty
      }
    }

    ////////////////////////////////////////////////////////////////////////////
    // Main algorithm //////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    // All the vertices in the various input clusters.
    // As a sanity check we verify they are all logic instructions.
    val allClustersInstrs = clusters.flatten
    assert(
      allClustersInstrs.forall(instr => isLogicInstr(instr)),
      s"Error: The input clusters do not all contain logic instructions!"
    )

    val (clusterSubgraph, primaryInputs) = getClustersSubgraph(instructionGraph, allClustersInstrs)
    val logicVertices = clusterSubgraph.vertexSet().asScala.filter { v =>
      !primaryInputs.contains(v)
    }.toSet

    val cuts = CutEnumerator(clusterSubgraph, primaryInputs, maxCutSize)

    // Now that we have all cuts, we find the cones that they span.
    val allCones = logicVertices.flatMap { root =>
      val rootCuts = cuts(root)
      // These do NOT include the "singleton" cones that consist of a single
      // vertex as the backtracking algorithm to generate a cone from a (root, cut)
      // pair stops before the cut leaves are seen.
      // Note though that this is not a problem as we are not computing a full
      // set cover where *every* vertex of the graph must be covered, but rather
      // the largest partial cover using the cones that have at least 2 vertices.
      rootCuts.map(cut => cutToCone(clusterSubgraph, root, cut))
    }

    // Sanity check.
    val invalidCones = allCones.filter { cone =>
      cone.exists(instr => !isLogicInstr(instr))
    }
    assert(
      invalidCones.isEmpty,
      s"Found invalid cones as they contain non-logic instructions:\n${invalidCones.mkString("\n")}"
    )

    // Keep only the cones that are "valid". A cone is considered valid if it is fanout-free.
    // Fanout-free means that only the root of the cone has outgoing edges to a vertex *outside*
    // the cone. Being fanout-free is a requirement to replace the cone with a custom instruction,
    // otherwise a vertex outside the cone will need an intermediate result enclosed in the custom
    // instruction.
    val validCones = allCones.filter { cone =>
      // Remove the root as it is legal for the root to have an outgoing edge to an external vertex.
      val nonRootVertices = cone.vertices - cone.root
      val nonRootSuccessors = nonRootVertices.flatMap { v =>
        Graphs.successorListOf(clusterSubgraph, v).asScala
      }
      val hasEdgeToExternalVertex = nonRootSuccessors.exists(v => !cone.vertices.contains(v))
      !hasEdgeToExternalVertex
    }

    findBestConeCover(logicVertices, validCones)
  }

  def substituteCones(
    dependenceGraph: Graph[Instruction, DefaultEdge],
    cones: Set[Cone[Instruction]]
  ): (
    Graph[Instruction, DefaultEdge],
    Seq[CustomFunction]
  ) = {
    // def coneToExprTree(
    //   cone: Cone[Instruction],
    // ): CustomFunctionImpl.ExprTree = {

    //   def makeExpr(
    //     v: Instruction
    //   ): CustomFunctionImpl.ExprTree = {
    //     val preds = Graphs.predecessorListOf(dependenceGraph, v).asScala
    //     v match {
    //       case BinaryArithmetic(operator, rd, rs1, rs2, annons) =>
    //         operator match {
    //           case
    //         }
    //       case _ =>
    //         throw new IllegalArgumentException(s"${v} is not a logic instruction!")
    //     }
    //   }

    //   makeExpr(cone.root)
    // }

    val newBody = ArrayBuffer.empty[Instruction]


    (dependenceGraph, Seq.empty)
  }

  def onProcess(
    proc: DefProcess
  )(
    implicit ctx: AssemblyContext
  ): (
    DefProcess,
    Int, // Number of instructions saved when custom instructions are implemented
    Int, // Number of custom functions used
  ) = {
    val dependenceGraph = createDependenceGraph(proc)

    // Identify logic clusters in the graph.
    val logicClusters = findLogicClusters(dependenceGraph)
      .filter(cluster => cluster.size > 1) // No point in creating a LUT vector to replace a single instruction.

    ctx.logger.debug {
      val clusterSizesStr = logicClusters.zipWithIndex.map { case (cluster, idx) =>
        s"cluster_${idx} -> ${cluster.size} instructions"
      }.mkString("\n")

      s"Covering ${logicClusters.size} logic clusters in ${proc.id} with sizes:\n${clusterSizesStr}"
    }

    // Find the optimal cone cover globally.
    val bestCones = findOptimalConeCover(
      dependenceGraph,
      logicClusters,
      maxCutSize = ctx.max_custom_instruction_inputs,
      maxNumCones = ctx.max_custom_instructions
    )(ctx)

    // Must convert to a Seq before mapping as otherwise cones of identical size will
    // "disappear" from the count due to the semantics of Set[Int].
    val numSavedInstructions = bestCones.toSeq.map(cone => cone.size - 1).sum
    ctx.logger.info(s"We save ${numSavedInstructions} instructions in ${proc.id} using ${bestCones.size} custom instructions.")

    // To visually inspect the mapping for correctness, we dump a highlighted dot file
    // showing each cone with a different color.
    ctx.logger.dumpArtifact(
      s"dependence_graph${ctx.logger.countProgress()}_${phase_id}_${proc.id}_selectedCustomInstructionCones.dot"
    ) {
      val colors = CyclicColorGenerator(bestCones.size)
      val colorMap = bestCones
        .zip(colors)
        .flatMap { case (cone, color) =>
          cone.vertices.map { v =>
            v -> color
          }
        }.toMap

      dumpGraph(dependenceGraph, colorMap)
    }

    // We can now replace each cluster with a single custom instruction.
    val customDependenceGraph = substituteCones(dependenceGraph, bestCones)

    (proc, numSavedInstructions, bestCones.size)
  }

  override def transform(
      program: DefProgram,
      context: AssemblyContext
  ): DefProgram = {
    val origVirtualCycleLen = program.processes.map { proc =>
      proc.body.length
    }.max

    val newProcesses = program.processes.map(proc => onProcess(proc)(context))

    val progNumSavedInstructions = newProcesses.map { case (proc, numSavedInstructions, numCustomFunctionsUsed) =>
      numSavedInstructions
    }.sum

    val progNumCustomFunctions = newProcesses.map { case (proc, numSavedInstructions, numCustomFunctionsUsed) =>
      numCustomFunctionsUsed
    }.sum

    val newVirtualCycleLen = newProcesses.map { case (proc, procNumSavedInstructions, numCustomFunctionsUsed) =>
      proc.body.length - procNumSavedInstructions
    }.max

    context.logger.info(s"Virtual cycle length: ${origVirtualCycleLen} -> ${newVirtualCycleLen} (using ${progNumCustomFunctions} custom functions)")
    context.logger.info(s"Will save ${progNumSavedInstructions} instructions in the whole program")

    program
  }
}
