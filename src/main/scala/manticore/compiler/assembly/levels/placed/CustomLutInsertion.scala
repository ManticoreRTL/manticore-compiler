package manticore.compiler.assembly.levels.placed

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.BinaryOperator.{AND, OR, XOR}
import manticore.compiler.assembly.DependenceGraphBuilder
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.{Color, CyclicColorGenerator}
import org.jgrapht.{Graph, Graphs}
import org.jgrapht.alg.util.UnionFind
import org.jgrapht.graph.{AsSubgraph, DefaultEdge, DirectedAcyclicGraph, SimpleGraph}
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

  case class Cone[V](root: V, vertices: Set[V]) {
    override def toString(): String = {
      val nonRootVertices = vertices - root
      s"(${root}) --- ${nonRootVertices.mkString(" --- ")}"
    }

    def size: Int = vertices.size

    // Useful for returning the cone with different names (or different types, like
    // for replacing a Cone[Int] with a Cone[Instruction]).
    def renameVertices[T](f: V => T): Cone[T] = {
      val rootRenamed = f(root)
      val verticesRenamed = vertices.map(v => f(v))
      Cone(rootRenamed, verticesRenamed)
    }
  }

  val flavor = PlacedIR
  import PlacedIR._

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
    ctx.logger.debug {
      val clusterSizesStr = clusters.zipWithIndex.map { case (cluster, idx) =>
        s"cluster_${idx} -> ${cluster.size} instructions"
      }.mkString("\n")

      s"Covering ${clusters.size} logic clusters with sizes:\n${clusterSizesStr}"
    }

    // All the vertices in the various input clusters.
    val allClustersInstrs = clusters.flatten

    // As a sanity check we verify they are all logic instructions.
    assert(
      allClustersInstrs.forall(instr => isLogicInstr(instr)),
      s"Error: The input clusters do not all contain logic instructions!"
    )

    // Graph where vertices are represented as Ints to simplify debugging as
    // the identifiers in an instruction change at every compile.
    // The graph's vertices are all logic instructions of the clusters *and* their primary inputs (which are not logic
    // instructions by definition). The primary inputs are needed as otherwise we would not know the in-degree of the
    // first vertices in each logic cluster.
    def getIdGraph(
      clusters: Set[Set[Instruction]]
    ): (
      Graph[Int, DefaultEdge],
      Map[Int, Instruction]
    ) = {
      // The primary inputs of the cluster are vertices *outside* the logic cluster that are connected
      // to vertices *inside* the cluster.
      val primaryInputs = allClustersInstrs.flatMap { vInACluster =>
        Graphs.predecessorListOf(instructionGraph, vInACluster).asScala
      }.filter { vAnywhere =>
        !allClustersInstrs.contains(vAnywhere)
      }

      val subgraphVertices = primaryInputs ++ allClustersInstrs
      val instructionSubgraph = new AsSubgraph(instructionGraph, subgraphVertices.asJava)

      // The vertices are numbered in topological order for easier debugging.
      val vToIdMap = new TopologicalOrderIterator(instructionSubgraph).asScala.toSeq.zipWithIndex.toMap

      // Create a new graph where vertices are labelled as Ints. Ideally we would use some
      // sort of "named view", but JGraphT does not support one.
      val idGraph: Graph[Int, DefaultEdge] = new DirectedAcyclicGraph(classOf[DefaultEdge])
      instructionSubgraph.vertexSet().asScala.foreach { v =>
        idGraph.addVertex(vToIdMap(v))
      }
      instructionSubgraph.edgeSet().asScala.foreach { e =>
        val srcId = vToIdMap(instructionSubgraph.getEdgeSource(e))
        val dstId = vToIdMap(instructionSubgraph.getEdgeTarget(e))
        idGraph.addEdge(srcId, dstId)
      }

      val idToInstrMap = vToIdMap.map(kv => kv.swap)
      (idGraph, idToInstrMap)
    }

    val (idGraph, idToInstrMap) = getIdGraph(clusters)

    // Cut enumeration. We use a BitSet to represent a cut. If a bit is set, then the
    // corresponding vertex is part of the cut.
    // We use an ordered collection (ArrayBuffer) instead of an unordered collection (Set) to
    // represent a SET of cuts for every vertex as we refer to cuts by their index in this
    // array later. A cut will only be added to a vertex's cut "SET" if it contains a vertex
    // that doesn't exist in the other cuts.
    type Cuts = ArrayBuffer[Cut]
    val vCuts = MHashMap.empty[Int, Cuts]

    ////////////////////////////////////////////////////////////////////////////
    // Helpers /////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    def enumerateCuts(
      v: Int
    ): Unit = {
      // Operation to perform on each n-tuple. In the cut generation algorithm this
      // involves computing a new cut from the fan-in nodes' selected cuts, checking
      // whether an existing cut dominates it, and if not, adding it to the cuts
      // of the current node.
      def visitNtuple(
        // Node for which we are computing the cut.
        v: Int,
        // Fan-ins of the current node.
        faninIds: collection.Seq[Int],
        // Index of the cut to choose from the given fan-in node's cut set.
        faninRadicesCnt: collection.Seq[Int]
      ): Unit = {
        // set C <- C_1 U C_2 U ... U C_ki
        val c = faninIds.zip(faninRadicesCnt).foldLeft(Cut.empty) { case (cut, (faninId, faninRadixCnt)) =>
          val faninCuts = vCuts(faninId)
          val faninSelectedCut = faninCuts(faninRadixCnt)
          cut | faninSelectedCut
        }

        // Do not accept the cut if its size is larger than our threshold.
        if (c.size > maxCutSize) {
          return
        }

        // Don't add new cut if existing cut dominates it. A cut C' dominates
        // another cut C if C' is a subset of C. Being a subset means that C' has at
        // most the same bits set as C, but no more bits.
        //
        // So if we AND their bitsets together, we can see what they have in common,
        // then we can XOR this with C' original bits to see if C' has any bit
        // active that C does not.
        //
        //   C' = 0b 00110
        //   C  = 0b 01010 AND
        //        --------
        //        0b 00010
        //   C' = 0b 00110 XOR
        //        --------
        //        0b 00100 => C' does NOT dominate C as it contains a node that C does not.

        vCuts(v).foreach { cPrime =>
          val sharedNodes = cPrime & c
          val cPrimeExtraNodes = sharedNodes ^ cPrime
          val cPrimeDominatesCut = cPrimeExtraNodes.isEmpty
          if (cPrimeDominatesCut) {
            return
          }
        }

        vCuts(v) += c
      }

      // Index of node's fan-ins.
      val faninIds = ArrayBuffer.empty[Int]

      // Number of cuts of a given fan-in node ("radix" in TAOCP, Vol 4A, Algorithm M).
      val faninRadices = ArrayBuffer.empty[Int] // radix (m[n-1], ... , m[0]) in TAOCP

      // Index of a cut within a fan-in node's list of cuts ("value" in TAOCP, Vol 4A, Algorithm M).
      val faninRadicesCnt = ArrayBuffer.empty[Int] // value (a[n-1], ... , a[0]) in TAOCP

      // We start by initializing the radix of each fan-in node and its initial value (0) so
      // we can increment them later. We also need to know the index of every fan-in node so
      // we can query its number of cuts.

      val vPreds = Graphs.predecessorListOf(idGraph, v).asScala
      vPreds.foreach { pred =>
        faninIds += pred
        // Radix of the given fan-in is determined by the number of cuts it has.
        faninRadices += vCuts(pred).size
        // Start counting from (0, 0, ..., 0) in the M-radix number format.
        faninRadicesCnt += 0
      }

      val vInDegree = vPreds.size

      var j = 0
      while (j != vInDegree) {
        visitNtuple(v, faninIds, faninRadicesCnt)

        // Mixed-radix n-tuple generation algorithm. Adding 1 to the n-tuple.
        j = 0
        while ((j != vInDegree) && (faninRadicesCnt(j) == faninRadices(j) - 1)) {
          faninRadicesCnt(j) = 0
          j += 1
        }
        if (j != vInDegree) {
          faninRadicesCnt(j) += 1
        }
      }
    }

    def cutToCone(
      root: Int,
      cut: Cut
    ): Cone[Int] = {
      val coneVertices = MHashSet.empty[Int]

      // Backtrack from the given vertex until a vertex in the cut is seen.
      def backtrack(v: Int): Unit = {
        if (!cut.contains(v)) {
          coneVertices += v
          Graphs.predecessorListOf(idGraph, v).asScala.foreach(pred => backtrack(pred))
        }
      }

      backtrack(root)
      Cone(root, coneVertices.toSet)
    }

    ////////////////////////////////////////////////////////////////////////////
    // Main algorithm //////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    // The primary inputs of the clusters are the vertices that are not part of
    // the clusters. These are technically vertices that are *not* logic instructions
    // as otherwise they would have been included in the cluster.
    val (logicVertices, primaryInputs) = idGraph.vertexSet().asScala.partition { v =>
      val instr = idToInstrMap(v)
      allClustersInstrs.contains(instr)
    }

    ctx.logger.debug {
      val piInstrsStr = primaryInputs.map(idToInstrMap).mkString("\n")
      val logicInstrsStr = logicVertices.map(idToInstrMap).mkString("\n")
      s"primaryInputs:\n${piInstrsStr}\n\nlogicVertices:\n${logicInstrsStr}"
    }

    // Initialize cut set of every vertex to the empty set.
    idGraph.vertexSet().asScala.foreach { v =>
      vCuts(v) = ArrayBuffer.empty
    }

    // For i = 1, ..., n do
    //   set CUTS(i) <- {{i}}
    // end
    primaryInputs.foreach { v =>
      vCuts(v) += Cut(v)
    }

    // For i = n, ..., n + r do
    //   set CUTS(i) <- {}
    //   set new_cut = Cartesian product of node i's fan-ins' cuts.
    //   if is_valid(new_cut) then
    //     set CUTS(i) <- modified_new_cut
    //   end
    //   set CUTS(i) <- CUTS(i) U {{i}}
    // end

    // The cut enumeration algorithm assumes vertices are visited in topological order.
    new TopologicalOrderIterator(idGraph).asScala.filter { v =>
      // Ignore primary inputs as we have already set their cut sets.
      !primaryInputs.contains(v)
    }.foreach { v =>
      // Internally uses TAOCP Vol 4A algorithm M, mixed-radix n-tuple
      // generation, to enumerate the cross-product of the node's fan-in
      // cut-sets.
      enumerateCuts(v)

      vCuts(v) += Cut(v)

      // ctx.logger.debug {
      //   val vCutsStr = vCuts(v).map { cut =>
      //     cut.mkString("-")
      //   }.mkString("\n")
      //   s"${maxCutSize}-cuts of root ${v}, num_cuts = ${vCuts(v).size}\n${vCutsStr}"
      // }
    }

    // Now that we have all cuts, we find the cones that they span.
    val allCones = logicVertices.flatMap { root =>
      val cuts = vCuts(root)
      // These do NOT include the "singleton" cones that consist of a single
      // vertex as the backtracking algorithm to generate a cone from a (root, cut)
      // pair stops before the cut vertices are seen.
      cuts.map(cut => cutToCone(root, cut))
    } ++ logicVertices.map { root =>
      // Add the "singleton" cones. These are needed to guarantee a feasible
      // solution exists in the non-overlapping set cover problem.
      Cone(root, Set(root))
    }

    val invalidCones = allCones.map { idCone =>
      idCone.renameVertices(idToInstrMap)
    }.filter { instrCone =>
      instrCone.vertices.exists { instr =>
        !isLogicInstr(instr)
      }
    }

    // Sanity check.
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
        Graphs.successorListOf(idGraph, v).asScala
      }
      val hasEdgeToExternalVertex = nonRootSuccessors.exists(v => !cone.vertices.contains(v))
      !hasEdgeToExternalVertex
    }

    // We want to maximize the number of instructions we can remove from the cluster.
    // The cones must be non-overlapping as otherwise a vertex *outside* the cone uses an intermediate
    // result from within the cone and we therefore can't remove the vertices that make up the cone.
    // In other words, the cones must be fanout-free.
    //
    // This is a proxy for the set cover problem with non-overlapping sets. We can solve this optimally
    // using an ILP formulation.

    // We assign every cone an ID so we can easily manipulate them in a map.
    val idToConeMap = validCones.zipWithIndex.map(kv => kv.swap).toMap

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
    logicVertices.foreach { v =>
      // The inequality constraint is defined by setting the lower/upper bound of the
      // constraint to 0/1.
      val constraint = solver.makeConstraint(0, 1, s"covered_v${v}")

      idToConeMap.foreach { case (coneId, cone) =>
        val coefficient = if (cone.vertices.contains(v)) 1 else 0
        constraint.setCoefficient(coneVars(coneId), coefficient)
      }
    }

    // (3) Bound the number of custom instructions to force large cones to be generated.
    //
    //    sum_{i = 0}{i = num_cones-1} x_i <= maxNumCones
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
      val idConesUsed = coneVars.filter { case (coneId, coneVar) =>
        // A cone is used if it is part of the cone cover.
        coneVar.solutionValue.toInt > 0
      }.map { case (coneId, _) =>
        coneId -> idToConeMap(coneId)
      }

      // Collect vertex identifiers that make up the cones, then map these identifiers to their
      // instructions. This is the final result and the caller can then decide which custom instructions
      // to implement.
      val instrConesUsed = idConesUsed.map { case (coneId, cone) =>
        cone.renameVertices(idToInstrMap)
      }.toSet

      ctx.logger.debug {
        val instrConesUsedStr = instrConesUsed.mkString("\n")
        s"Solved non-overlapping cone covering problem in ${solver.wallTime()}ms.\nCan reduce instruction count by ${objective.value().toInt} using ${instrConesUsed.size} cones:\n${instrConesUsedStr}"
      }

      instrConesUsed

    } else {
      ctx.logger.fail(s"Could not optimally solve cone cover problem (resultStatus = ${resultStatus}).")
      // We return the empty set to signal that no logic instructions could be fused together.
      Set.empty
    }
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
    // TODO (skashani)

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
