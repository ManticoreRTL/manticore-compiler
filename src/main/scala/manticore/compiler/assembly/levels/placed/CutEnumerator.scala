package manticore.compiler.assembly.levels.placed

import org.jgrapht.{Graph, Graphs}
import org.jgrapht.graph.{AsSubgraph, DefaultEdge, DirectedAcyclicGraph}
import scala.collection.immutable.{BitSet => Cut}
import scala.collection.mutable.{ArrayBuffer, HashMap => MHashMap, HashSet => MHashSet}
import scala.jdk.CollectionConverters._
import org.jgrapht.traverse.TopologicalOrderIterator

object CutEnumerator {

  // The input graph must contain the vertices from which the cuts will be computed.
  // This means the primary inputs *must* be part of the graph and have in-degree 0.
  def apply[V](
    g: Graph[V, DefaultEdge],
    primaryInputs: Set[V],
    maxCutSize: Int
  ): Map[
    V, // Graph vertex
    Seq[
      Set[V] // Cut leaves
    ]
  ] = {

    // Cut enumeration. We use a BitSet to represent a cut. If a bit is set, then the
    // corresponding vertex is part of the cut. BitSets require the use of an Int as
    // the vertex identifier. We therefore first assign Int IDs to every vertex.
    val vToId = (g.vertexSet().asScala ++ primaryInputs).zipWithIndex.toMap
    val idToV = vToId.map(kv => kv.swap)

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
      vId: Int
    ): Unit = {
      // Operation to perform on each n-tuple. In the cut generation algorithm this
      // involves computing a new cut from the fan-in nodes' selected cuts, checking
      // whether an existing cut dominates it, and if not, adding it to the cuts
      // of the current node.
      def visitNtuple(
        // Node for which we are computing the cut.
        vId: Int,
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

        vCuts(vId).foreach { cPrime =>
          val sharedNodes = cPrime & c
          val cPrimeExtraNodes = sharedNodes ^ cPrime
          val cPrimeDominatesCut = cPrimeExtraNodes.isEmpty
          if (cPrimeDominatesCut) {
            return
          }
        }

        vCuts(vId) += c
      }

      // Node's fan-ins.
      val faninIds = ArrayBuffer.empty[Int]

      // Number of cuts of a given fan-in node ("radix" in TAOCP, Vol 4A, Algorithm M).
      val faninRadices = ArrayBuffer.empty[Int] // radix (m[n-1], ... , m[0]) in TAOCP

      // Index of a cut within a fan-in node's list of cuts ("value" in TAOCP, Vol 4A, Algorithm M).
      val faninRadicesCnt = ArrayBuffer.empty[Int] // value (a[n-1], ... , a[0]) in TAOCP

      // We start by initializing the radix of each fan-in node and its initial value (0) so
      // we can increment them later. We also need to know the index of every fan-in node so
      // we can query its number of cuts.

      val vPreds = Graphs.predecessorListOf(g, idToV(vId)).asScala
      vPreds.foreach { pred =>
        val predId = vToId(pred)
        faninIds += predId
        // Radix of the given fan-in is determined by the number of cuts it has.
        faninRadices += vCuts(predId).size
        // Start counting from (0, 0, ..., 0) in the M-radix number format.
        faninRadicesCnt += 0
      }

      val vInDegree = vPreds.size

      var j = 0
      while (j != vInDegree) {
        visitNtuple(vId, faninIds, faninRadicesCnt)

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

    ////////////////////////////////////////////////////////////////////////////
    // Main algorithm //////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////

    // Initialize cut set of every vertex to the empty set.
    g.vertexSet().asScala.foreach { v =>
      val vId = vToId(v)
      vCuts(vId) = ArrayBuffer.empty
    }

    // For i in primary_inputs do
    //   set CUTS(i) <- {{i}}
    // end
    primaryInputs.foreach { v =>
      val vId = vToId(v)
      vCuts(vId) += Cut(vId)
    }

    // For i in non_primary_inputs (topological order) do
    //   set CUTS(i) <- {}
    //   set new_cut = Cartesian product of node i's fan-ins' cuts.
    //   if is_valid(new_cut) then
    //     set CUTS(i) <- modified_new_cut
    //   end
    //   set CUTS(i) <- CUTS(i) U {{i}}
    // end

    // The cut enumeration algorithm assumes vertices are visited in topological order.
    new TopologicalOrderIterator(g).asScala.filter { v =>
      // Ignore primary inputs as we have already set their cut sets.
      !primaryInputs.contains(v)
    }.foreach { v =>
      val vId = vToId(v)

      // Internally uses TAOCP Vol 4A algorithm M, mixed-radix n-tuple
      // generation, to enumerate the cross-product of the node's fan-in
      // cut-sets.
      enumerateCuts(vId)

      vCuts(vId) += Cut(vId)

      // // Debug
      // val vCutsStr = vCuts(vId).map { cut =>
      //   cut.mkString("-")
      // }.mkString("\n")
      // println(s"${maxCutSize}-cuts of root ${v}, num_cuts = ${vCuts(vId).size}\n${vCutsStr}")

    }

    // Transform vertex IDs back to the original input vertices.
    val resCuts = vCuts.map { case (vId, cuts) =>
      val v = idToV(vId)
      val renamedCuts = cuts.map { cut =>
        val renamedCut = cut.unsorted.map(cutVId => idToV(cutVId))
        renamedCut
      }.toSeq

      v -> renamedCuts
    }.toMap

    resCuts
  }
}
