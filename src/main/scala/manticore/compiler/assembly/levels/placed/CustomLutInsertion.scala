package manticore.compiler.assembly.levels.placed

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.BinaryOperator.{AND, OR, XOR, BinaryOperator}
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
import manticore.compiler.assembly.levels.ConstType
import org.jgrapht.alg.isomorphism.AHURootedTreeIsomorphismInspector
import manticore.compiler.assembly.BinaryOperator
import org.jgrapht.alg.isomorphism.VF2GraphIsomorphismInspector
import org.jgrapht.GraphMapping

// Represents a rooted cone of vertices in a graph.
// The constructor is private as I want to force callers to go through the factory method in the Cone object.
class Cone[V] private (
  val root: V,
  // The primary inputs are *outside* the cone and feed the cone.
  //
  // A cone computes an expression and the primary inputs are the variables in
  // the expression. We want to support comparing two cones to determine if they
  // compute the same function.
  //
  // Two cones of the same arity that compute the same function do so subject to
  // the order of their primary inputs. The number we assign here is this order
  // and can be used to map the primary inputs of one cone to those of the other.
  val piArgIdxToName: Map[PlacedIR.CustomFunctionImpl.AtomArg, V],
  val piNameToArgIdx: Map[V, PlacedIR.CustomFunctionImpl.AtomArg],
  val leaves: Set[V],
  // Contains the root, the leaves, and all intermediate vertices between.
  val vertices: Set[V]
) {

  def size: Int = vertices.size

  def arity: Int = piArgIdxToName.size

  def contains(v: V): Boolean = {
    vertices.contains(v)
  }

  def exists(p: V => Boolean): Boolean = {
    vertices.exists(p)
  }

  override def toString(): String = {
    val nonRootVertices = vertices - root
    s"{arity = ${arity}, root = ${root}, (body = ${nonRootVertices.mkString(" --- ")}), (primary inputs = ${piNameToArgIdx.keys.mkString(" --- ")})}"
  }
}

object Cone {
  def apply[V](
    g: Graph[V, DefaultEdge],
    root: V,
    primaryInputs: Set[V],
    vertices: Set[V]
  ): Cone[V] = {
    val piSuccs = primaryInputs.flatMap { pi =>
      Graphs.successorListOf(g, pi).asScala.toSet
    }

    val leaves = piSuccs.intersect(vertices)

    val piNameToArgIdx = primaryInputs
      .zipWithIndex
      .map { case (pi, idx) =>
        pi -> PlacedIR.CustomFunctionImpl.AtomArg(idx)
      }
      .toMap

    val piArgIdxToName = piNameToArgIdx.map(kv => kv.swap)

    new Cone(root, piArgIdxToName, piNameToArgIdx, leaves, vertices)
  }
}

object GraphDump {
  // The jgrapht library does not support emitting graphviz `subgraph`s.
  // The scala-graph library does support emitting graphviz `subgraphs`s, so we use it instead.
  import scalax.collection.{Graph => SGraph}
  import scalax.collection.mutable.{Graph => MSGraph}
  import scalax.collection.GraphEdge.DiEdge
  import scalax.collection.io.dot._
  import scalax.collection.io.dot.implicits._

  // Converts a jgrapht Graph to a scala-graph graph.
  // Note that a mutable graph (MSGraph) is constructed, but we return a graph of the
  // "interface" type (SGraph) as the DOT exporter only supports this parent interface.
  private def jg2sc[V](
    jg: Graph[V, DefaultEdge]
  ): SGraph[V, DiEdge] = {
    val sg = MSGraph.empty[V, DiEdge]

    jg.vertexSet().asScala.foreach { v =>
      sg += v
    }

    jg.edgeSet().asScala.foreach { e =>
      val src = jg.getEdgeSource(e)
      val dst = jg.getEdgeTarget(e)
      sg += DiEdge(src, dst)
    }

    sg
  }

  private def quote(s: String): String = s"\"${s}\""

  def apply[V](
    g: Graph[V, DefaultEdge],
    clusters: Map[
      Int, // Cluster ID
      Set[V] // Vertices in cluster
    ] = Map.empty[Int, Set[V]],
    clusterColorMap: Map[
      Int, // Cluster ID
      String // Color to be assigned to cluster
    ] = Map.empty[Int, String],
    vNameMap: Map[V, String] = Map.empty[V, String],
  ): String = {
    val sg = jg2sc(g)

    val dotRoot = DotRootGraph(
      directed = true,
      id = Some("program graph with custom functions")
    )

    val dotSubgraphs = clusters.map { case (clusterId, vertices) =>
      val dotSub = DotSubGraph(
        dotRoot,
        Id(s"cluster_${clusterId}")
      )
      clusterId -> dotSub
    }

    def edgeTransform(
      iedge: scalax.collection.Graph[V, DiEdge]#EdgeT
    ): Option[(
      DotGraph, // The graph/subgraph to which this edge belongs.
      DotEdgeStmt // Statements to modify the edge's representation.
    )] = {
      iedge.edge match {
        case DiEdge(source, target) =>
          Some(
            (
              dotRoot, // All edges are part of the root graph.
              DotEdgeStmt(
                NodeId(source.toString()),
                NodeId(target.toString())
              )
            )
          )
        case e @ _ =>
          assert(
            false,
            s"Edge ${e} in the dependence graph could not be serialized!"
          )
          None
      }
    }

    def nodeTransformer(
      inode: scalax.collection.Graph[V, DiEdge]#NodeT
    ): Option[(
      DotGraph, // The graph/subgraph to which this node belongs.
      DotNodeStmt // Statements to modify the node's representation.
    )] = {
      val clusterId = clusters
        .find { case (clusterId, vertices) =>
          vertices.contains(inode.toOuter)
        }.map { case (clusterId, vertices) =>
          clusterId
        }

      // If the vertex is part of a cluster, select the corresponding subgraph.
      // Otherwise add the vertex to the root graph.
      val dotGraph = clusterId.map { id =>
        dotSubgraphs(id)
      }.getOrElse(dotRoot)

      val v = inode.toOuter
      val vLabel = vNameMap.getOrElse(v, v.toString())

      // If the vertex is part of a cluster, select its corresponding color.
      // Otherwise use white as the default color.
      val vColor = clusterId.map { id =>
        clusterColorMap(id)
      }.getOrElse("white")

      Some(
        (
          dotGraph,
          DotNodeStmt(
            NodeId(v.toString()),
            Seq(
              DotAttr("label", quote(vLabel)),
              DotAttr("style", quote("filled")),
              DotAttr("fillcolor", quote(vColor))
            )
          )
        )
      )
    }

    val dotExport: String = sg.toDot(
      dotRoot = dotRoot,
      edgeTransformer = edgeTransform,
      cNodeTransformer = Some(nodeTransformer)
    )

    dotExport
  }
}

object CustomLutInsertion
    extends DependenceGraphBuilder
    with AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {

  val flavor = PlacedIR
  import flavor._

  import CustomFunctionImpl._

  // We want vertices for every element that could be used as a primary input. This means we must use `Name`
  // as the vertex instead of `Instruction` as no instruction exists to represent inputs of the circuit (registers).
  def createDependenceGraph(
    proc: DefProcess
  )(
    implicit ctx: AssemblyContext
  ): Graph[Name, DefaultEdge] = {
    val g: Graph[Name, DefaultEdge] = new DirectedAcyclicGraph(classOf[DefaultEdge])

    val constNames = proc.registers.filter { reg =>
      reg.variable.varType == ConstType
    }.map { constReg =>
      constReg.variable.name
    }.toSet

    proc.body.foreach { instr =>
      // Add a vertex for every register that is written and associate it with its instruction.
      val regDefs = DependenceAnalysis.regDef(instr)
      regDefs.foreach { rd =>
        g.addVertex(rd)
      }

      // Add a vertex for every register that is read.
      val regUses = DependenceAnalysis.regUses(instr)
      regUses.foreach { rs =>
        // The vertex will not be added again if it already exists (defined by a previous
        // instruction).
        g.addVertex(rs)
      }

      // Add edges between regDefs and regUses.
      regDefs.foreach { rd =>
        regUses.foreach { rs =>
          g.addEdge(rs, rd)
        }
      }
    }

    g
  }

  def isLogicInstr(
    name: Name,
    nameToInstrMap: Map[Name, Instruction]
  ): Boolean = {
    nameToInstrMap.get(name) match {
      case None =>
        // The name must be a top-level input or a constant as no instruction defines it.
        false
      case Some(instr) =>
        isLogicInstr(instr)
    }
  }

  def isLogicInstr(instr: Instruction): Boolean = {
    instr match {
      case BinaryArithmetic(AND | OR | XOR, _, _, _, _) => true
      case _ => false
    }
  }

  def findLogicClusters(
    g: Graph[Name, DefaultEdge],
    nameToInstrMap: Map[Name, Instruction]
  ): Set[Set[Name]] = {
    // We use Union-Find to cluster logic instructions together.
    val uf = new UnionFind(g.vertexSet())

    // Merge the edges that link 2 logic instructions.
    g.edgeSet().asScala.foreach { e =>
      val src = g.getEdgeSource(e)
      val dst = g.getEdgeTarget(e)

      val isSrcLogic = isLogicInstr(src, nameToInstrMap)
      val isDstLogic = isLogicInstr(dst, nameToInstrMap)

      if (isSrcLogic && isDstLogic) {
        uf.union(src, dst)
      }
    }

    val logicClusters = g.vertexSet()
      .asScala // Returns a mutable.Set
      .toSet // Convert to immutable.Set
      .filter { v =>
        isLogicInstr(v, nameToInstrMap)
      }
      .groupBy { v =>
        uf.find(v)
      }.values.toSet

    logicClusters
  }

  def getFanoutFreeCones(
    dependenceGraph: Graph[Name, DefaultEdge],
    logicVertices: Set[Name],
    maxCutSize: Int,
    nameToConstMap: Map[Name, Constant],
    nameToInstrMap: Map[Name, Instruction]
  )(
    implicit ctx: AssemblyContext
  ): Iterable[Cone[Name]] = {
    // Returns a subgraph of the dependence graph containing the logic vertices *and* their inputs
    // (which are not logic vertices by definition). The inputs are needed to perform cut enumeration
    // as otherwise we would not know the in-degree of the first logic vertices in the graph.
    def logicSubgraphWithInputs(): (
      Graph[Name, DefaultEdge],
      Set[Name] // inputs
    ) = {
      // The inputs are vertices *outside* the given set of vertices that are connected
      // to vertices *inside* the cluster. Some of these "inputs" are constants, but
      // constants are known at compile time and do not affect the runtime behavior of
      // a function. We therefore do not count them as inputs and filter them out.
      val inputs = logicVertices.flatMap { vLogic =>
        Graphs.predecessorListOf(dependenceGraph, vLogic).asScala
      }.filter { vAnywhere =>
        !logicVertices.contains(vAnywhere) && !nameToConstMap.contains(vAnywhere)
      }

      val subgraphVertices = inputs ++ logicVertices
      val subgraph = new AsSubgraph(dependenceGraph, subgraphVertices.asJava)

      (subgraph, inputs)
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
      Cone(g, root, cut, coneVertices.toSet)
    }

    // Form a graph containing the vertices of all clusters AND their inputs.
    val (gLswi, primaryInputs) = logicSubgraphWithInputs()

    ctx.logger.dumpArtifact(
      s"dependence_graph_${ctx.logger.countProgress()}_${phase_id}_logicSubgraphWithInputs.dot"
    ) {
      // All vertices should use the string representation of the instruction
      // that generates them, or the name itself (if a primary input).
      val nameMap = nameToInstrMap.map { case (regDef, instr) =>
        regDef -> instr.toString
      }

      GraphDump(
        gLswi,
        vNameMap=nameMap
      )
    }

    // Enumerate the cuts of every vertex in the graph.
    val cuts = CutEnumerator(gLswi, primaryInputs, maxCutSize)

    // Now that we have all cuts, we find the cones that they span.
    val allCones = logicVertices.toSeq.flatMap { root =>
      val rootCuts = cuts(root)
      // These do NOT include the "singleton" cones that consist of a single
      // vertex as the backtracking algorithm to generate a cone from a (root, cut)
      // pair stops before the cut leaves (the cut's inputs) are seen.
      // Note though that this is not a problem as we are not computing a full
      // set cover where *every* vertex of the graph must be covered, but rather
      // the largest partial cover using the cones that have at least 2 vertices.
      rootCuts.map(cut => cutToCone(gLswi, root, cut))
    }

    // Sanity check.
    val invalidCones = allCones.filter { cone =>
      cone.exists(v => !isLogicInstr(v, nameToInstrMap))
    }
    assert(
      invalidCones.isEmpty,
      s"Found invalid cones as they contain non-logic instructions:\n${invalidCones.mkString("\n")}"
    )

    // Keep only the cones that are fanout-free.
    // Fanout-free means that only the root of the cone has outgoing edges to a vertex *outside*
    // the cone. Being fanout-free is a requirement to replace the cone with a custom instruction,
    // otherwise a vertex outside the cone will need an intermediate result enclosed in the custom
    // instruction.
    val fanoutFreeCones = allCones.filter { cone =>
      // Remove the root as it is legal for the root to have an outgoing edge to an external vertex.
      val nonRootVertices = cone.vertices - cone.root
      val nonRootSuccessors = nonRootVertices.flatMap { v =>
        Graphs.successorListOf(gLswi, v).asScala
      }
      val hasEdgeToExternalVertex = nonRootSuccessors.exists(v => !cone.contains(v))
      !hasEdgeToExternalVertex
    }

    fanoutFreeCones
  }

  def coneToCustomFunction(
    cone: Cone[Name],
    nameToConstMap: Map[Name, Constant],
    nameToInstrMap: Map[Name, Instruction]
  ): CustomFunction = {

    def makeExpr(
      name: Name
    ): CustomFunctionImpl.ExprTree = {
      cone.piNameToArgIdx.get(name) match {
        case Some(atomArg) =>
          // Base case: The name is a primary input.
          IdExpr(atomArg)

        case None =>
          nameToConstMap.get(name) match {
            case Some(const) =>
              // Base case: The name is a constant.
              IdExpr(AtomConst(const))

            case None =>
              // General case: Create expression for each argument of the instruction
              // and join them to form an expression tree.
              val instr = nameToInstrMap(name)
              instr match {
                case BinaryArithmetic(operator, rd, rs1, rs2, annons) =>
                  val rs1Expr = makeExpr(rs1)
                  val rs2Expr = makeExpr(rs2)

                  operator match {
                    case AND => AndExpr(rs1Expr, rs2Expr)
                    case OR => OrExpr(rs1Expr, rs2Expr)
                    case XOR => XorExpr(rs1Expr, rs2Expr)
                    case _ =>
                      throw new IllegalArgumentException(s"Unexpected non-logic operator ${operator}.")
                  }

                case _ =>
                  throw new IllegalArgumentException(s"Expected to see a logic instruction, but saw \"${instr}\" instead.")
              }
          }
      }
    }

    val rootName = cone.root
    val rootExpr = makeExpr(rootName)
    val customFunc = CustomFunctionImpl(rootExpr)

    customFunc
  }

  // Maps every cone to a representative cone that computes the same function.
  // The representative cone can be found by querying the Union-Find graph returned.
  // The input argument mapping between a cone and its representative is returned
  // as the 2nd element of the result.
  def identifyCommonFunctions(
    funcs: Map[
      Int, // Cone ID
      CustomFunctionImpl // Custom function of this cone
    ],
  ): (
    UnionFind[Int], // Clusters of cones that compute the same function.
    Map[
      Int, // Cone ID
      Map[
        AtomArg, // Cone primary input idx
        AtomArg  // Representative primary input idx. The representative of a cluster is the result of uf.find(coneId)
      ]
    ]
  ) = {

    def fullFuncEqualityCheck(
      f1: CustomFunction,
      f2: CustomFunction
    ): Option[ // Returns None if the functions are not equal.
      Map[AtomArg, AtomArg] // Maps arg indices in f1 to arg indices in f2.
    ] = {
      assert(
        f1.arity == f2.arity,
        "Functions of unequal arities should not have been passed to this method!"
      )

      val equ1 = f1.equation
      val equ2 = f2.equation

      if (equ1 == equ2) {
        // Fast path:
        // Sometimes identical functions with different structures can quickly be determined
        // to be equal without needing to enumerate input permutations of f2. This always
        // happens when we are in the presence of "homegenous" functions like all-AND, all-OR,
        // or all-XOR.

        // The order of inputs in the two functions matched out of the box. We can just map
        // them to each other without modifying their order.
        val inputIdxMap = (0 until f1.arity).map { argIdx =>
          AtomArg(argIdx) -> AtomArg(argIdx)
        }.toMap
        Some(inputIdxMap)

      } else {

        // Slow path:
        // To determine if f2 is equivalent to f1, we must compute its equation for all possible
        // permutations of its inputs. If any permutation results a equation for f2 that matches
        // that of f1, then the functions are equivalent.

        val inputIdxMap = (0 until f1.arity)
          .permutations
          .map { inputPermutation =>
            val subst = inputPermutation.zipWithIndex.map { case (inputIdx, idx) =>
              AtomArg(inputIdx) -> AtomArg(idx)
            }.toMap
            subst
          }
          .find { subst =>
            val substExpr = CustomFunctionImpl.substitute(f2.expr)(subst)
            val substFunc = CustomFunctionImpl(substExpr)
            val substEqu = substFunc.equation

            equ1 == substEqu
          }.map { subst =>
            subst.toMap[AtomArg, AtomArg]
          }

        inputIdxMap
      }
    }

    def isHomogeneousResource(
      f1: CustomFunction,
      f2: CustomFunction
    ): Boolean = {
      val f1Ops = f1.resources.keySet
      val f2Ops = f2.resources.keySet

      // There should only be 1 operator.
      (f1Ops == f2Ops) && (f1Ops.size == 1)
    }

    val equalCones = MHashMap.empty[
      (Int, Int), // (cone1Id, cone2Id)
      Map[AtomArg, AtomArg] // (cone1AtomArg, cone2AtomArg)
    ]

    val uf = new UnionFind(funcs.keySet.asJava)

    // Comparing functions for equality is exponential as it involves computing their equation
    // and checking if they are equivalent under various input orderings. It is therefore important
    // to prune the space of functions for which we have no choice but to do the exponential comparison.
    //
    // Functions are determined to definitely be unequivalent if:
    //
    //   1. Their arity is different.
    //   2. They have different resources.
    //
    // Within the group of functions in each (arity, resource) category, we can quickly determine equivalence
    // between two functions if:
    //
    //   1. The functions have the same cone ID.
    //   2. The functions are composed of a homogeneous type of resource (all-AND, all-OR, all-XOR).
    //
    // In all other cases we must perform the expensive exponential check.

    val groupedFuncs = funcs.groupBy { case (coneId, func) =>
      // No point in comparing functions that have different arity or resources.
      (func.arity, func.resources)
    }

    groupedFuncs.foreach { case ((arity, resources), coneFuncs) =>
      val identityArgMap = Seq.tabulate(arity) { idx =>
        AtomArg(idx) -> AtomArg(idx)
      }.toMap

      // A cone is equal to itself.
      coneFuncs.foreach { case (coneId, f) =>
        equalCones += (coneId, coneId) -> identityArgMap
      }

      // Pairwise cone comparisons (with redundant comparisons skipped).
      val sortedConeFuncs = coneFuncs.toArray
      (0 until sortedConeFuncs.size).foreach { i =>
        val (c1Id, f1) = sortedConeFuncs(i)

        // Do not compare (j, i) as we would have already compared (i, j).
        (i+1 until sortedConeFuncs.size).foreach { j =>
          val (c2Id, f2) = sortedConeFuncs(j)

          if (isHomogeneousResource(f1, f2)) {
            // If thefunctions are composed of homogeneous operators, then we know immediately
            // that the functions are identical and the order of their inputs do not matter.

            // We keep both (c1Id -> c2Id) and (c2Id -> c1Id) to ensure a representative is found for both of
            // them after these pairwise comparisons.
            equalCones += (c1Id, c2Id) -> identityArgMap
            equalCones += (c2Id, c1Id) -> identityArgMap
            uf.union(c1Id, c2Id)

          } else {
            // Need to try a more expensive check to determine if the functions are identical.
            fullFuncEqualityCheck(f1, f2) match {
              case None =>
                // No permutation of the inputs was found such that f1 == f2, so we don't keep track of anything.
              case Some(arg1ToArg2Map) =>
                // We found some mapping from f1's inputs to f2's input such that their equations are equal.

                // We keep both (c1Id -> c2Id) and (c2Id -> c1Id) to ensure a representative is found for both of
                // them after these pairwise comparisons.
                equalCones += (c1Id, c2Id) -> arg1ToArg2Map
                equalCones += (c2Id, c1Id) -> arg1ToArg2Map.map(kv => kv.swap)
                uf.union(c1Id, c2Id)
            }
          }
        }
      }
    }

    val coneToReprArgMap = equalCones
      .filter { case ((c1Id, c2Id), argMap) =>
        val reprId = uf.find(c1Id)
        c2Id == reprId
      }
      .map { case ((cId, reprId), argMap) =>
        cId -> argMap
      }

    (uf, coneToReprArgMap.toMap)
  }

  // Finds the set of cone IDs to choose to maximize the number of instructions we
  // save through the use of custom functions.
  // Note that this function returns the cones used and not the function categories
  // used as these are already known to the caller.
  def findOptimalConeCover[V](
    cones: Map[
      Int, // Cone ID
      Cone[V] // Cone
    ],
    // Groups cones that compute the same function together. We don't care about
    // what they are computing, just that they are computing the same function
    // and therefore don't need an additional custom function to model if already
    // chosen.
    coneIdToCategory: Map[
      Int, // Cone ID
      Int  // Category
    ],
    maxNumCones: Int
  )(
    implicit ctx: AssemblyContext
  ): Set[Int] = {
    // We want to maximize the number of instructions we can remove from the program.
    // The cones must be non-overlapping as otherwise a vertex *outside* the cone uses an intermediate
    // result from within the cone and we therefore can't remove the vertices that make up the cone.
    // In other words, the cones must be fanout-free.
    //
    // This is a proxy for the set cover problem with non-overlapping sets. We can solve this optimally
    // using an ILP formulation.

    // known values
    // ------------
    //
    // - Set C = {C_1, C_2, ..., C_N}
    //
    //   The cones in the system. A cone is a collection of vertices.
    //
    // - Set CT = {CT_1, CT_2, ..., CT_Z}
    //
    //   The "types" (or categories) of cones in the system. Each CT_i is itself a set that
    //   contains the cones that implement the same function.
    //
    // - max_custom_functions
    //
    //   The maximum number of custom functions supported in hardware.
    //
    // objective function
    // ------------------
    //
    // Maximize the number of instructions that are saved through the use of custom functions.
    //
    //     max sum_{C_i \in C} x_i * (|C_i|-1)
    //
    // constraints
    // -----------
    //
    //   (1) Create binary variable x_i for every cone C_i in the system.
    //
    //         x_i \in {0, 1} for C_i \in C
    //
    //   (2) Create auxiliary binary variable y_i for every custom function (cone "type/category") CT_i in the system.
    //
    //         y_i \in {0, 1} for CT_i \in CT
    //
    //   (3) Each vertex is covered by at most once.
    //
    //         sum_{C_i \in C : v \in C_i} x_i <= 1
    //
    //   (4) The number of custom functions does not surprass the maximum available.
    //
    //         sum_{CT_i \in CT} y_i <= max_custom_functions
    //
    //       where y_i is defined as "cone TYPE i is used".
    //
    //         y_i = OR_{C_i \in C : C_i \in CT_i} x_i
    //
    //       Note that OR is not a linear constraint, but it can be made linear through
    //       the following transformation:
    //
    //         y_i >= x_i      \forall C_i \in C : C_i \in CT_i
    //
    //         y_i <= sum_{C_i \in C : C_i \in CT_i} x_i

    Loader.loadNativeLibraries()
    val solver = MPSolver.createSolver("SCIP")
    if (solver == null) {
      ctx.logger.fail("Could not create solver SCIP.")
    }

    //   (1) Create binary variable x_i for every cone C_i in the system.
    //
    //         x_i \in {0, 1} for C_i \in C
    //
    val coneVars = MHashMap.empty[Int, MPVariable]
    cones.foreach { case (coneId, cone) =>
      coneVars += coneId -> solver.makeIntVar(0, 1, s"x_${coneId}")
    }

    //   (2) Create auxiliary binary variable y_i for every custom function (cone "type/category") CT_i in the system.
    //
    //         y_i \in {0, 1} for CT_i \in CT
    //
    val conesPerCategory = coneIdToCategory
      .groupMap { case (coneId, category) =>
        category
      } { case (coneId, category) =>
        coneId
      }
    val categoryVars = MHashMap.empty[Int, MPVariable]
    conesPerCategory.keys.foreach { category =>
      categoryVars += category -> solver.makeIntVar(0, 1, s"y_${category}")
    }

    //   (3) Each vertex is covered by at most once.
    //
    //         sum_{C_i \in C : v \in C_i} x_i <= 1
    //
    //       We model this as
    //
    //         0 <= sum_{C_i \in C : v \in C_i} x_i <= 1
    //
    val allVertices = cones.flatMap { case (coneId, cone) =>
      cone.vertices
    }.toSet
    allVertices.foreach { v =>
      val constraint = solver.makeConstraint(0, 1, s"covered_v${v}")
      cones.foreach { case (coneId, cone) =>
        val coneVar = coneVars(coneId)
        val coefficient = if (cone.contains(v)) 1 else 0
        constraint.setCoefficient(coneVar, coefficient)
      }
    }

    //   (4) The number of custom functions does not surprass the maximum available.
    //
    //         sum_{CT_i \in CT} y_i <= max_custom_functions
    //
    //       where y_i is defined as "cone TYPE i is used".
    //
    //         y_i = OR_{C_i \in C : C_i \in CT_i} x_i
    //
    //       Note that OR is not a linear constraint, but it can be made linear through
    //       the following transformation:
    //
    //         y_i >= x_i      \forall C_i \in C : C_i \in CT_i
    //
    //         y_i <= sum_{C_i \in C : C_i \in CT_i} x_i
    //

    //   (4) The number of custom functions does not surprass the maximum available.
    //
    //         sum_{CT_i \in CT} y_i <= max_custom_functions
    //
    //       We model this as
    //
    //         0 <= sum_{CT_i \in CT} y_i <= max_custom_functions
    //
    val maxNumConesConstraint = solver.makeConstraint(0, maxNumCones, s"maxNumCones_constraint")
    categoryVars.keys.foreach { category =>
      val categoryVar = categoryVars(category)
      maxNumConesConstraint.setCoefficient(categoryVar, 1)
    }
    //   (4)
    //       ...
    //       Note that OR is not a linear constraint, but it can be made linear through
    //       the following transformation:
    //
    //         y_i >= x_i      \forall C_i \in C : C_i \in CT_i
    //
    //       We model this as
    //
    //        0 <= y_i - xi <= 1
    //
    conesPerCategory.foreach { case (category, coneIds) =>
      coneIds.foreach { coneId =>
        val constraint = solver.makeConstraint(0, 1, s"y_${category} >= x_${coneId}")
        val coneVar = coneVars(coneId)
        val categoryVar = categoryVars(category)
        constraint.setCoefficient(categoryVar, 1)
        constraint.setCoefficient(coneVar, -1)
      }
    }
    //   (4)
    //       ...
    //       Note that OR is not a linear constraint, but it can be made linear through
    //       the following transformation:
    //
    //         y_i <= sum_{C_i \in C : C_i \in CT_i} x_i
    //
    //       We model this as
    //
    //        0 <= (sum_{C_i \in C : C_i \in CT_i} x_i) - yi <= sum_{C_i \in C : C_i \in CT_i} 1
    //
    conesPerCategory.foreach { case (category, coneIds) =>
      val constraint = solver.makeConstraint(0, coneIds.size, s"y_${category} <= ${coneIds.size}")
      coneIds.foreach { coneId =>
        val coneVar = coneVars(coneId)
        constraint.setCoefficient(coneVar, 1)
      }
      val categoryVar = categoryVars(category)
      constraint.setCoefficient(categoryVar, -1)
    }

    // objective function
    // ------------------
    //
    // Maximize the number of instructions that are saved through the use of custom functions.
    //
    //     max sum_{C_i \in C} x_i * (|C_i|-1)
    //
    val objective = solver.objective()
    cones.foreach { case (coneId, cone) =>
      val coneSize = cone.size
      val coefficient = coneSize - 1
      val coneVar = coneVars(coneId)
      objective.setCoefficient(coneVar, coefficient)
    }
    objective.setMaximization()

    // solve optimization problem
    val resultStatus = ctx.stats.recordRunTime("solver.solve") {
      solver.solve()
    }

    // Extract results
    if (resultStatus == MPSolver.ResultStatus.OPTIMAL) {
      // The selected cones.
      val conesUsed = coneVars.filter { case (coneId, coneVar) =>
        coneVar.solutionValue.toInt > 0
      }.keySet

      // The "types" of custom functions used.
      val customFuncsUsed = categoryVars.filter { case (category, categoryVar) =>
        categoryVar.solutionValue.toInt > 0
      }.keySet

      ctx.logger.info {
        s"Solved non-overlapping cone covering problem in ${solver.wallTime()}ms.\nCan reduce instruction count by ${objective.value().toInt} using ${conesUsed.size} cones covered by ${customFuncsUsed.size} custom functions."
      }

      conesUsed.toSet

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
  ): DefProcess = {
    val dependenceGraph = createDependenceGraph(proc)

    val nameToConstMap = proc.registers.filter { reg =>
      reg.variable.varType == ConstType
    }.map { constReg =>
      // constants are guaranteed to have a constant assigned to their variable value, so
      // it is safe to call `get`.
      constReg.variable.name -> constReg.value.get
    }.toMap

    val nameToInstrMap = proc.body.foldLeft(Map.empty[Name, Instruction]) { case (currNameToInstrMap, instr) =>
      val regDefs = DependenceAnalysis.regDef(instr)
      val newEntries = regDefs.map(rd => rd -> instr).toMap
      currNameToInstrMap ++ newEntries
    }

    // Identify logic clusters in the graph.
    val logicClusters = findLogicClusters(dependenceGraph, nameToInstrMap)
      .filter(cluster => cluster.size > 1) // No point in creating a LUT vector to replace a single instruction.

    ctx.logger.debug {
      val clusterSizesStr = logicClusters
        .toSeq
        .sortBy(cluster => cluster.size)(Ordering[Int].reverse)
        .zipWithIndex.map { case (cluster, idx) =>
          s"cluster_${idx} -> ${cluster.size} instructions"
        }.mkString("\n")

      s"Covering ${logicClusters.size} logic clusters in ${proc.id} with sizes:\n${clusterSizesStr}"
    }

    // We assign an Int id to every cone as we will be storing pairs of cones in a hash table later
    // and it is costly to has the Cone itself each time (hashing its id is much faster).
    val cones = ctx.stats.recordRunTime("getFanoutFreeCones") {
      getFanoutFreeCones(
        dependenceGraph,
        logicClusters.flatten,
        ctx.max_custom_instruction_inputs,
        nameToConstMap,
        nameToInstrMap
      ).filter { cone =>
        // We don't care about cones that consist of a single instruction as there
        // is no benefit in doing so (we will not reduce the instruction count).
        cone.size > 1
      }
      .zipWithIndex.map { case (cone, idx) =>
        idx -> cone
      }.toMap
    }

    ctx.logger.debug {
      val numConesOfArity = cones
        .groupBy { case (id, cone) =>
          cone.arity
        }
        .map { case (arity, group) =>
          arity -> group.size
        }
        .toSeq
        .sortBy { case (arity, numCones) =>
          arity
        }
        .map { case (arity, numCones) =>
          s"arity ${arity} -> ${numCones} cones"
        }
        .mkString("\n")

      s"There are ${cones.size} fanout-free cones.\n${numConesOfArity}"
    }

    // The functions computed by each cone.
    val coneFuncs = ctx.stats.recordRunTime("conesToCustomFunctions") {
      cones.map { case (coneId, cone) =>
        coneId -> coneToCustomFunction(cone, nameToConstMap, nameToInstrMap)
      }.toMap
    }

    ctx.logger.debug {
      coneFuncs
        .toSeq
        .sortBy { case (coneId, func) =>
          (func.arity, coneId)
        }
        .map { case (coneId, func) =>
          val cone = cones(coneId)
          s"coneId ${coneId} | ${func} | ${cone}"
        }.mkString("\n")
    }

    val (coneIdToCategory, coneToReprArgMap) = if (ctx.optimize_common_custom_functions) {
      // Identify which functions are identical and select cones with this knowledge.

      // Cluster cones that compute the same function together. Cones that compute
      // the same function do so under a permutation of their inputs. This permutation
      // is computed between every cone and its representative cone (not between
      // all cones).

      val (ufCones, coneToReprArgMap) = ctx.stats.recordRunTime("identifyCommonFunctions")(identifyCommonFunctions(coneFuncs))
      val coneIdToCategory = cones.keys.map { coneId =>
        coneId -> ufCones.find(coneId)
      }.toMap

      ctx.logger.debug {
        s"The cones are clustered into ${ufCones.numberOfSets()} function groups."
      }

      (coneIdToCategory, coneToReprArgMap)

    } else {
      // Consider all cones to be in distinct categories of their own.

      // If all cones are distinct, then their arguments are unique and we don't
      // need to compute whether they are a permutation of another cone's inputs.
      val coneToReprArgMap = cones.keys.map { coneId =>
        val cone = cones(coneId)
        val argMap = Seq.tabulate(cone.arity) { idx =>
          AtomArg(idx) -> AtomArg(idx)
        }.toMap
        coneId -> argMap
      }.toMap

      val coneIdToCategory = cones.keys.map { coneId =>
        coneId -> coneId
      }.toMap

      (coneIdToCategory, coneToReprArgMap)
    }

    val categories = coneIdToCategory.values.toSet

    ctx.logger.debug {
      coneIdToCategory
        .toSeq
        .sorted
        .map { case (coneId, category) =>
          s"cone ${coneId} -> category ${category}"
        }.mkString("\n")
    }

    // We know which cones compute the same function. We can now solve an optimization
    // problem to choose the best cones to implement knowing which ones come "for free"
    // due to duplicates existing.
    val bestConeIds = ctx.stats.recordRunTime("findOptimalConeCover") {
      findOptimalConeCover(
        cones,
        coneIdToCategory,
        maxNumCones = ctx.max_custom_instructions
      )(ctx)
    }

    ctx.logger.debug {
      val conesStr = bestConeIds
        .toSeq
        .sorted
        .map { coneId =>
          val func = coneFuncs(coneId)
          s"cone ${coneId} -> ${func}"
        }.mkString("\n")

      s"Selected cones:\n${conesStr}"
    }

    // To visually inspect the mapping for correctness, we dump a highlighted dot file
    // showing each cone category with a different color.
    ctx.logger.dumpArtifact(
      s"dependence_graph_${ctx.logger.countProgress()}_${phase_id}_${proc.id}_selectedCustomInstructionCones.dot"
    ) {
      // Each category has a defined color.
      val categoryToColor = categories
        .zip(CyclicColorGenerator(categories.size))
        .toMap

      val clusters = bestConeIds
        .map { coneId =>
          // We use the ID of the cone as the ID of the cluster.
          coneId -> cones(coneId).vertices
        }
        .toMap

      // Color all vertices of a cone with its color.
      val clusterColorMap = cones.map { case (clusterId, vertices) =>
        val category = coneIdToCategory(clusterId)
        val color = categoryToColor(category)
        clusterId -> color.toCssHexString()
      }

      // All vertices should use the string representation of the instruction
      // that generates them, or the name itself (if a primary input).
      val nameMap = nameToInstrMap.map { case (regDef, instr) =>
        regDef -> instr.toString
      }

      GraphDump(
        dependenceGraph,
        clusters=clusters,
        clusterColorMap=clusterColorMap,
        vNameMap=nameMap
      )
    }

    // The custom functions to emit. We only emit the custom functions used by the best cones.
    val categoryToFunc = bestConeIds.map { coneId =>
      val category = coneIdToCategory(coneId)
      val func = coneFuncs(category)
      category -> DefFunc(s"func_${category}", func)
    }.toMap

    val bestConesToFunc = bestConeIds.map { coneId =>
      val category = coneIdToCategory(coneId)
      val func = categoryToFunc(category)
      val coneArgMap = coneToReprArgMap(coneId)
      coneId -> (func, coneArgMap)
    }.toMap

    // Replace the cone roots with their custom functions.
    // Note that this keeps all intermediate vertices of the cones in the process body.
    // A subsequent DeadCodeElimination pass will be needed to eliminate these now-unused instructions.
    val newBody = proc.body.map { instr =>
      DependenceAnalysis.regDef(instr) match {
        case Seq(rd) =>
          bestConeIds.find { coneId =>
            val cone = cones(coneId)
            cone.root == rd
          } match {
            case None =>
              // Leave the instruction unchanged as the output of the instruction
              // does not match the root vertex of a selected cone.
              instr

            case Some(coneId) =>
              // Replace instruction with a custom instruction.
              val (func, coneToFuncArgMap) = bestConesToFunc(coneId)
              val cone = cones(coneId)
              val rsx = cone.piArgIdxToName
                .map { case (origArgIdx, argName) =>
                  val funcArgIdx = coneToFuncArgMap(origArgIdx)
                  funcArgIdx -> argName
                }
                .toSeq
                .sortBy { case (funcArgIdx, argName) =>
                  funcArgIdx.v
                }
                .map { case (funcArgIdx, argName) =>
                  argName
                }

              CustomInstruction(func.name, rd, rsx)
          }

        case _ =>
          // Leave the instruction unchanged as the instruction is not the root of a selected cone.
          instr
      }
    }

    val newProc = proc.copy(
      body = newBody,
      functions = categoryToFunc.values.toSeq
    )

    newProc
  }

  override def transform(
    program: DefProgram,
    context: AssemblyContext
  ): DefProgram = {
    val newProcesses = program.processes.map(proc => onProcess(proc)(context))
    program.copy(processes = newProcesses)
  }
}
