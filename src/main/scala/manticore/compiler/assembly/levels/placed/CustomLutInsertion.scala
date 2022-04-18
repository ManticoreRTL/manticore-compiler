package manticore.compiler.assembly.levels.placed

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.BinaryOperator.{AND, OR, XOR}
import manticore.compiler.assembly.DependenceGraphBuilder
import manticore.compiler.assembly.levels.AssemblyTransformer
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

object CustomLutInsertion
    extends DependenceGraphBuilder
    with AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {

  val flavor = PlacedIR
  import PlacedIR._

  def dumpGraph[V](
    g: Graph[V, DefaultEdge],
    vNameMap: Option[Map[V, _]] = None
  ): String = {
    val vertexIdProvider = new Function[V, String] {
      def apply(v: V): String = {
        val vStr = if (vNameMap.isDefined) {
          vNameMap.get(v)
        } else {
          v.toString
        }

        // Note the use of quotes around the serialized name so it is a legal graphviz identifier (as
        // characters such as "." are not allowed in identifiers).
        s"\"${vStr}\""
      }
    }

    val dotExporter = new org.jgrapht.nio.dot.DOTExporter[V, DefaultEdge](vertexIdProvider)
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
  ): Seq[Set[Instruction]] = {
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
      }.values.toSeq

    logicClusters
  }

  /**
    * Groups the instructions that make up the input cluster into a set of
    * non-overlapping cones that can be transformed into a custom instruction.
    * The non-overlapping cones are the optimal ones.
    */
  def onCluster(
    instructionGraph: Graph[Instruction, DefaultEdge],
    cluster: Set[Instruction],
    maxCutSize: Int
  )(
    implicit ctx: AssemblyContext
  ): Set[Set[Instruction]] = {
    // Graph where vertices are represented as Ints to simplify debugging.
    // The graph's vertices are all logic instructions of the cluster *and* their primary inputs (which are not logic
    // instructions by definition). The primary inputs are needed as otherwise we would not know the in-degree of the
    // first vertices in the logic cluster.
    def getIdGraph(
      cluster: Set[Instruction]
    ): (
      Graph[Int, DefaultEdge],
      Map[Int, Instruction]
    ) = {
      // The primary inputs of the cluster are vertices *outside* the logic cluster that are connected
      // to vertices *inside* the cluster.
      val primaryInputs = cluster.flatMap { vCluster =>
        Graphs.predecessorListOf(instructionGraph, vCluster).asScala
      }.filter { vGeneral =>
        !cluster.contains(vGeneral)
      }

      val subgraphVertices = primaryInputs ++ cluster
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

    val (idGraph, idToInstrMap) = getIdGraph(cluster)
    Files.writeString(Paths.get("tmp.dot"), dumpGraph(idGraph))

    // Cut enumeration. We use a BitSet to represent a cut. If a bit is set, then the
    // corresponding vertex is part of the cut.
    // We use an ordered collection (ArrayBuffer) instead of an unordered collection (Set) to
    // represent a SET of cuts for every vertex as we refer to cuts by their index in this
    // array later. A Cut will only be added to a vertex's cut "SET" if it contains a vertex
    // that doesn't exist in the other cuts.
    type Cuts = ArrayBuffer[Cut]
    case class Cone(root: Int, vertices: Set[Int])
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

    def getConeVertices(
      root: Int,
      cut: Cut
    ): Set[Int] = {
      val coneVertices = MHashSet.empty[Int]

      // Backtrack from the given vertex until a vertex in the cut is seen.
      def backtrack(v: Int): Unit = {
        if (!cut.contains(v)) {
          coneVertices += v
          Graphs.predecessorListOf(idGraph, v).asScala.foreach(pred => backtrack(pred))
        }
      }

      backtrack(root)
      coneVertices.toSet
    }

    ////////////////////////////////////////////////////////////////////////////
    // Main algorithm //////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    // The primary inputs of the cluster are the vertices with an in-degree of 0.
    // These are technically vertices that are *not* logic instructions as they
    // originate from outside the cluster.
    val (primaryInputs, logicVertices) = idGraph.vertexSet().asScala.partition(v => idGraph.inDegreeOf(v) == 0)

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

      // Debug
      ctx.logger.debug {
        val vCutsStr = vCuts(v).map { cut =>
          cut.mkString("-")
        }.mkString("\n")
        s"${maxCutSize}-cuts of root ${v}, num_cuts = ${vCuts(v).size}\n${vCutsStr}"
      }
    }

    // Now that we have all cuts, we find the cones that they span.
    val allCones = logicVertices.flatMap { root =>
      val cuts = vCuts(root)
      cuts.map(cut => Cone(root, getConeVertices(root, cut)))
    }

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
    //    max sum_{i = 0}{i = num_cones} x_i * (|C_i|-1)
    //
    // constraints
    // -----------
    //
    //    x_i \in {0, 1} \forall i \in [0 .. num_cones]          (1) Each cone is either selected, or not selected.
    //
    //    sum_{C_i : v \in C_i} x_i = 1                          (2) Each vertex is covered by exactly one cone.

    Loader.loadNativeLibraries()
    val solver = MPSolver.createSolver("SCIP")
    if (solver == null) {
      ctx.logger.fail("Could not create solver SCIP.")
    }

    // (1) Each cone is either selected, or not selected (binary variable).
    //
    //    x_i \in {0, 1} \forall i \in [0 .. num_cones]
    //
    val coneVars = MHashMap.empty[Int, MPVariable]
    idToConeMap.foreach { case (coneId, cone) =>
      coneVars += coneId -> solver.makeIntVar(0, 1, s"x_${coneId}")
    }

    // (2) Each vertex is covered by exactly one cone.
    //
    //    sum_{C_i : v \in C_i} x_i = 1
    //
    logicVertices.foreach { v =>
      // The equality constraint with 1 is defined by setting the lower and upper bound
      // of the constraint to 1.
      val constraint = solver.makeConstraint(1, 1, s"coveredOnce_v${v}")

      idToConeMap.foreach { case (coneId, cone) =>
        val coefficient = if (cone.vertices.contains(v)) 1 else 0
        constraint.setCoefficient(coneVars(coneId), coefficient)
      }
    }

    // objective function
    //
    //    max sum_{i = 0}{i = num_cones} x_i * (|C_i|-1)
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
        coneVar.solutionValue > 0
      }.map { case (coneId, _) =>
        coneId -> idToConeMap(coneId)
      }

      val conesUsedStr = conesUsed.map { case (coneId, cone) =>
        s"coneId ${coneId} = {${cone.vertices.mkString("-")}}"
      }.mkString("\n")
      ctx.logger.info(s"Solved non-overlapping cone covering problem in ${solver.wallTime()}ms.\nCan reduce instruction count by ${objective.value()} using following cones:\n${conesUsedStr}")

      // Collect vertex identifiers that make up the cones, then map these identifiers to their instructions.
      // This is the final result and the caller can then decide which custom instructions to implement.
      conesUsed.map { case (coneId, cone) =>
        cone.vertices.map(v => idToInstrMap(v))
      }.toSet

    } else {
      ctx.logger.fail("Could not optimally solve cone cover problem.")
      // We return the empty set to signal that no logic instructions could be fused together.
      Set.empty
    }
  }

  def onProcess(proc: DefProcess)(implicit ctx: AssemblyContext): DefProcess = {
    val dependenceGraph = createDependenceGraph(proc)

    // Identify logic clusters in the graph.
    val logicClusters = findLogicClusters(dependenceGraph)
      .filter(cluster => cluster.size > 1) // No point in creating a LUT vector to replace a single instruction.

    // Debug
    ctx.logger.debug {
      logicClusters.map { cluster =>
        val clusterInstrs = cluster.mkString("\n")
        s"logicCluster of size ${cluster.size} = ${clusterInstrs}"
      } .mkString("\n")
    }

    // Attempt to reduce cluster sizes by using custom LUT vectors.
    val potentialCustomInstrs = logicClusters.flatMap { cluster =>
      onCluster(dependenceGraph, cluster, maxCutSize = 4)(ctx)
    }

    // Select as many clusters as we have custom instructions available in the process.
    // We select the clusters in decreasing order of their size so we get the maximum
    // reduction in instructions.

    proc
  }

  override def transform(
      program: DefProgram,
      context: AssemblyContext
  ): DefProgram = {
    val newProcesses = program.processes.map(proc => onProcess(proc)(context))
    program.copy(processes = newProcesses)
  }

}
