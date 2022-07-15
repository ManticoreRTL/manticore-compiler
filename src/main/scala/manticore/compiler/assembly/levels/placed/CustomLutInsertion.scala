package manticore.compiler.assembly.levels.placed

import com.google.ortools.Loader
import com.google.ortools.linearsolver.MPSolver
import com.google.ortools.linearsolver.MPVariable
import manticore.compiler.AssemblyContext
import manticore.compiler.Color
import manticore.compiler.CyclicColorGenerator
import manticore.compiler.assembly.BinaryOperator
import manticore.compiler.assembly.BinaryOperator.AND
import manticore.compiler.assembly.BinaryOperator.BinaryOperator
import manticore.compiler.assembly.BinaryOperator.OR
import manticore.compiler.assembly.BinaryOperator.XOR
import manticore.compiler.assembly.DependenceGraphBuilder
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.CanCollectProgramStatistics
import manticore.compiler.assembly.levels.ConstType
import manticore.compiler.assembly.levels.UInt16
import org.jgrapht.Graph
import org.jgrapht.GraphMapping
import org.jgrapht.Graphs
import org.jgrapht.alg.util.UnionFind
import org.jgrapht.graph.AsSubgraph
import org.jgrapht.graph.AsUnmodifiableGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DirectedAcyclicGraph
import org.jgrapht.nio.Attribute
import org.jgrapht.nio.DefaultAttribute
import org.jgrapht.traverse.TopologicalOrderIterator

import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.util.function.Function
import scala.collection.immutable.{BitSet => Cut}
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.{HashMap => MHashMap}
import scala.collection.mutable.{HashSet => MHashSet}
import scala.jdk.CollectionConverters._

object CustomLutInsertion extends DependenceGraphBuilder with PlacedIRTransformer with CanCollectProgramStatistics {

  val flavor = PlacedIR
  import flavor._

  import CustomFunctionImpl._

  // We want vertices for every element that could be used as a primary input. The highest-level names are not generated
  // by instructions, so we store `Name` directly for those. For all other vertices we store the `Instruction`.
  type DepVertex = Either[Name, Instruction]
  type DepGraph  = Graph[DepVertex, DefaultEdge]

  // Represents a rooted cone of vertices in a graph.
  // The constructor is private as I want to force callers to go through the factory method in the Cone object.
  class Cone private (
      // // Contains the vertices of the root *and* the primary inputs (i.e., g.vertexSet = vertices ++ primaryInputs)
      // val g: AsUnmodifiableGraph[DepVertex, DefaultEdge],
      val root: DepVertex,
      val vertices: Set[DepVertex],
      val primaryInputs: Set[DepVertex],
      val func: CustomFunctionImpl,
      val posArgToNamedArg: Map[PositionalArg, NamedArg]
  ) {

    def size: Int = vertices.size

    def arity: Int = primaryInputs.size

    def contains(v: DepVertex): Boolean = {
      vertices.contains(v)
    }

    def exists(p: DepVertex => Boolean): Boolean = {
      vertices.exists(p)
    }

    override def toString(): String = {
      val nonRootVertices = vertices - root
      val funcStr = CustomFunctionImpl
        .substitute(func.expr)(posArgToNamedArg.toMap[AtomArg, NamedArg])
        .toString
      s"{arity = ${arity}, root = ${root}, func = ${funcStr}"
    }
  }

  object Cone {

    private def createExpr(
        root: DepVertex,
        vertices: Set[DepVertex],
        primaryInputs: Set[DepVertex],
        nameToInstrMap: Map[Name, Instruction],
        vToConstMap: Map[DepVertex, Constant]
    ): (
        ExprTree,
        Map[PositionalArg, NamedArg]
    ) = {

      // Two-way mapping is needed as we may see the same name twice in the expression
      // and we must assign the same positional argument to each. Example:
      //
      //     coneId xxx -> {arity = 2, root = Right(OR	%w1401, %w1049, %w667), func = ((%w702 & 1) | ((%w897 & 1) & (%w702 ^ 1)))
      //                                                                                 ^^^^^          ^^^^^         ^^^^^
      //                                                                      PositionalArg(0)     PositionalArg(1)   PositionalArg(0)

      val posArgToNamedArg = MHashMap.empty[PositionalArg, NamedArg]
      val namedArgToPosArg = MHashMap.empty[NamedArg, PositionalArg]

      def nextPos() = posArgToNamedArg.size

      def makeExpr(v: DepVertex): CustomFunctionImpl.ExprTree = {
        v match {
          case Left(name) =>
            vToConstMap.get(v) match {
              case Some(const) =>
                IdExpr(AtomConst(const))

              case None =>
                namedArgToPosArg.get(NamedArg(name)) match {
                  case None =>
                    // This named argument has not been seen before, so we
                    // create a new positional argument for it.
                    val pos      = nextPos()
                    val posArg   = PositionalArg(pos)
                    val namedArg = NamedArg(name)
                    posArgToNamedArg += posArg   -> namedArg
                    namedArgToPosArg += namedArg -> posArg
                    IdExpr(posArg)

                  case Some(posArg) =>
                    // This named argument has been seen before. We reuse
                    // the previously-assigned positional argument to it.
                    IdExpr(posArg)
                }
            }

          case Right(instr) =>
            instr match {
              case BinaryArithmetic(operator, rd, rs1, rs2, _) =>
                val rs1Expr = nameToInstrMap.get(rs1) match {
                  case None           => makeExpr(Left(rs1))
                  case Some(rs1Instr) => makeExpr(Right(rs1Instr))
                }
                val rs2Expr = nameToInstrMap.get(rs2) match {
                  case None           => makeExpr(Left(rs2))
                  case Some(rs2Instr) => makeExpr(Right(rs2Instr))
                }

                operator match {
                  case AND => AndExpr(rs1Expr, rs2Expr)
                  case OR  => OrExpr(rs1Expr, rs2Expr)
                  case XOR => XorExpr(rs1Expr, rs2Expr)
                  case _ =>
                    throw new IllegalArgumentException(
                      s"Unexpected non-logic operator ${operator}."
                    )
                }

              case _ =>
                throw new IllegalArgumentException(
                  s"Expected to see a logic instruction, but saw \"${instr}\" instead."
                )
            }
        }
      }

      (makeExpr(root), posArgToNamedArg.toMap)
    }

    def apply(
        g: DepGraph,
        root: DepVertex,
        primaryInputs: Set[DepVertex],
        vToConstMap: Map[DepVertex, Constant]
    ): Cone = {

      // Backtracks from the root until (and including) the primary inputs.
      // All vertices encountered along the way form a cone.
      def getConeGraphVertices(): Set[DepVertex] = {
        val coneVertices = MHashSet.empty[DepVertex]

        // Backtrack from the given vertex until a primary input is seen.
        def backtrack(v: DepVertex): Unit = {
          coneVertices += v
          if (!primaryInputs.contains(v)) {
            Graphs
              .predecessorListOf(g, v)
              .asScala
              .foreach(pred => backtrack(pred))
          }
        }

        backtrack(root)
        coneVertices.toSet
      }

      val verticesInclPrimaryInputs = getConeGraphVertices()
      val vertices                  = verticesInclPrimaryInputs -- primaryInputs
      val nameToInstrMap = vertices.flatMap {
        case Right(instr) =>
          val regDefs = NameDependence.regDef(instr)
          regDefs.map { regDef =>
            regDef -> instr
          }
        case _ =>
          None
      }.toMap
      val (expr, posArgToNamedArg) =
        createExpr(root, vertices, primaryInputs, nameToInstrMap, vToConstMap)
      val func = CustomFunctionImpl(expr)

      new Cone(root, vertices, primaryInputs, func, posArgToNamedArg)
    }
  }

  // We want vertices for every element that could be used as a primary input. The highest-level names are not generated
  // by instructions, so we store `Name` directly for those. For all other vertices we store the `Instruction`.
  def createDependenceGraph(
      proc: DefProcess
  )(implicit
      ctx: AssemblyContext
  ): DepGraph = {
    val g: DepGraph = new DirectedAcyclicGraph(classOf[DefaultEdge])

    val instrRegDefs = proc.body.flatMap { instr =>
      val regDefs = NameDependence.regDef(instr)
      regDefs.map { regDef =>
        regDef -> instr
      }
    }.toMap

    proc.body.foreach { instr =>
      // Add a vertex for every instruction.
      g.addVertex(Right(instr))

      // Add a vertex for every register that is read. Note that we may read names that
      // are not generated by any instruction (primary inputs for example). We therefore
      // need to be careful here and possibly create a `Name` for the vertex instead of
      // a `Instruction`.
      val regUses = NameDependence.regUses(instr)
      regUses.foreach { rs =>
        instrRegDefs.get(rs) match {
          case None =>
            // rs is not generated by an instruction, so we create a `Name` for it.
            g.addVertex(Left(rs))
            // Use Left(rs) here as it is a `Name`.
            g.addEdge(Left(rs), Right(instr))

          case Some(rsInstr) =>
            // rs is generated by an instruction. The program is ordered, so we must
            // have already seen rs and created a vertex for it beforehand. There is
            // nothing to do.
            // Use Right(rsInstr) here as rs is generated by an `Instruction`, not a `Name`.
            g.addEdge(Right(rsInstr), Right(instr))
        }
      }
    }

    g
  }

  def isLogicInstr(instr: Instruction): Boolean = {
    instr match {
      case BinaryArithmetic(AND | OR | XOR, _, _, _, _) => true
      case _                                            => false
    }
  }

  def findLogicClusters(
      g: DepGraph
  ): Set[Set[DepVertex]] = {
    // We use Union-Find to cluster logic instructions together.
    val uf = new UnionFind(g.vertexSet())

    val logicVertices = g
      .vertexSet()
      .asScala
      .filter {
        case Right(instr) if isLogicInstr(instr) => true
        case _                                   => false
      }
      .toSet

    // Merge the edges that link 2 logic instructions.
    g.edgeSet().asScala.foreach { e =>
      val src = g.getEdgeSource(e)
      val dst = g.getEdgeTarget(e)

      val srcIsLogic = logicVertices.contains(src)
      val dstIsLogic = logicVertices.contains(dst)

      if (srcIsLogic && dstIsLogic) {
        uf.union(src, dst)
      }
    }

    val logicClusters = logicVertices
      .groupBy { v =>
        uf.find(v)
      }
      .values
      .toSet

    logicClusters
  }

  def getFanoutFreeCones(
      dependenceGraph: DepGraph,
      logicVertices: Set[DepVertex],
      maxCutSize: Int,
      vToConstMap: Map[DepVertex, Constant]
  )(implicit
      ctx: AssemblyContext
  ): Iterable[Cone] = {
    // Returns a subgraph of the dependence graph containing the logic vertices *and* their inputs
    // (which are not logic vertices by definition). The inputs are needed to perform cut enumeration
    // as otherwise we would not know the in-degree of the first logic vertices in the graph.
    def logicSubgraphWithInputs(): (
        DepGraph,
        Set[DepVertex] // inputs
    ) = {
      // The inputs are vertices *outside* the given set of vertices that are connected
      // to vertices *inside* the cluster. Some of these "inputs" are constants, but
      // constants are known at compile time and do not affect the runtime behavior of
      // a function. We therefore do not count them as inputs and filter them out.
      val inputs = logicVertices
        .flatMap { vLogic =>
          Graphs.predecessorListOf(dependenceGraph, vLogic).asScala
        }
        .filter { vAnywhere =>
          !logicVertices.contains(vAnywhere) && !vToConstMap.contains(vAnywhere)
        }

      val subgraphVertices = inputs ++ logicVertices
      val subgraph         = new AsSubgraph(dependenceGraph, subgraphVertices.asJava)

      (subgraph, inputs)
    }

    // Form a graph containing the vertices of all clusters AND their inputs.
    val (gLswi, primaryInputs) = logicSubgraphWithInputs()

    ctx.logger.dumpArtifact(
      s"dependence_graph_${ctx.logger.countProgress()}_${transformId}_logicSubgraphWithInputs.dot"
    ) {
      // All vertices should use the string representation of the instruction
      // that generates them, or the name itself (if a primary input).
      val nameMap = gLswi
        .vertexSet()
        .asScala
        .map { v =>
          v match {
            case Left(name)   => v -> name
            case Right(instr) => v -> instr.toString()
          }
        }
        .toMap

      dumpGraph(
        gLswi,
        vNameMap = nameMap
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
      rootCuts.map(cut => Cone(gLswi, root, cut, vToConstMap))
    }

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
      val hasEdgeToExternalVertex =
        nonRootSuccessors.exists(v => !cone.contains(v))
      !hasEdgeToExternalVertex
    }

    fanoutFreeCones
  }

  // Maps every cone to a representative cone that computes the same function.
  // The representative cone can be found by querying the Union-Find graph returned.
  // The input argument mapping between a cone and its representative is returned
  // as the 2nd element of the result.
  def identifyCommonFunctions(
      cones: Map[
        Int, // Cone ID
        Cone
      ]
  ): (
      UnionFind[Int], // Clusters of cones that compute the same function.
      Map[
        Int, // Cone ID
        Map[
          PositionalArg, // Cone primary input idx
          PositionalArg // Representative primary input idx. The representative of a cluster is the result of uf.find(coneId)
        ]
      ]
  ) = {

    // We are going to look for permutations of cone inputs often. We therefore pre-compute
    // the possible permutations so we do not have to do it again each time we want to compare
    // two cones.
    def generatePermutationSubstMap(arity: Int): Vector[Map[PositionalArg, PositionalArg]] = {
      (0 until arity).permutations.map { inputPermutation =>
        val subst = inputPermutation.zipWithIndex.map { case (inputIdx, idx) =>
          PositionalArg(inputIdx) -> PositionalArg(idx)
        }
        subst.toMap
      }.toVector
    }
    val arityPermutationSubstMap = (1 to 6).map { arity =>
      arity -> generatePermutationSubstMap(arity)
    }.toMap

    // Cache of expression equations.
    val exprToEquCache = MHashMap.empty[ExprTree, Seq[BigInt]]

    def fullFuncEqualityCheck(
        f1: CustomFunction,
        f2: CustomFunction
    ): Option[ // Returns None if the functions are not equal.
      Map[
        PositionalArg,
        PositionalArg
      ] // Maps arg indices in f1 to arg indices in f2.
    ] = {
      assert(
        f1.arity == f2.arity,
        "Functions of unequal arities should not have been passed to this method!"
      )

      // Slow path:
      // To determine if f1 is equivalent to f2, we must compute f1's equation for all possible
      // permutations of its inputs. If any permutation results in a equation that matches that
      // of f2, then the functions are equivalent.

      // Note that we are varying f1's inputs. This means that, if we find a mapping, it modifies
      // f1 to be equal to f2 (and not the other way around). We are therefore considering f2 as
      // the fixed function here.

      // Cache f2's expression and its equation to save execution time in future calls.
      val equ2 = exprToEquCache.getOrElseUpdate(f2.expr, f2.equation)

      // Seq.permutations is an Iterator and only computes the next element if queried.
      // We use Iterator.find() so we can exit the expensive permutation generation as
      // soon as we find a valid input permutation (instead of computing all permutations
      // and filtering them afterwards, which is very expensive).
      val matchingPermutation = arityPermutationSubstMap(f1.arity)
        .find { subst =>
          val f1NewExpr = CustomFunctionImpl.substitute(f1.expr)(subst.toMap[AtomArg, AtomArg])
          val f1NewFunc = CustomFunctionImpl(f1NewExpr)
          // Cache the substituted expression and equation to save execution time in future calls.
          val f1NewEqu = exprToEquCache.getOrElseUpdate(f1NewExpr, f1NewFunc.equation)
          f1NewEqu == equ2
        }

      matchingPermutation
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
      (Int, Int),                       // (cone1Id, cone2Id)
      Map[PositionalArg, PositionalArg] // (cone1AtomArg, cone2AtomArg)
    ]

    val uf = new UnionFind(cones.keySet.asJava)

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

    val groupedCones = cones.groupBy { case (coneId, cone) =>
      // No point in comparing functions that have different arity or resources.
      (cone.func.arity, cone.func.resources)
    }

    groupedCones.foreach { case ((arity, resources), coneGroup) =>
      val identityArgMap = Seq
        .tabulate(arity) { idx =>
          PositionalArg(idx) -> PositionalArg(idx)
        }
        .toMap

      // A cone is equal to itself.
      coneGroup.foreach { case (coneId, cone) =>
        exprToEquCache += cone.func.expr -> cone.func.equation
        equalCones += (coneId, coneId)   -> identityArgMap
      }

      // Pairwise cone comparisons (with redundant comparisons skipped).
      val sortedConeFuncs = coneGroup.toArray
      (0 until sortedConeFuncs.size).foreach { i =>
        val (c1Id, c1) = sortedConeFuncs(i)

        // Do not compare (j, i) as we would have already compared (i, j).
        (i + 1 until sortedConeFuncs.size).foreach { j =>
          val (c2Id, c2) = sortedConeFuncs(j)

          if (isHomogeneousResource(c1.func, c2.func)) {
            // If the functions are composed of homogeneous operators, then we know immediately
            // that the functions are identical and the order of their inputs do not matter.

            // We keep both (c1Id -> c2Id) and (c2Id -> c1Id) to ensure a representative is found for both of
            // them after these pairwise comparisons.
            equalCones += (c1Id, c2Id) -> identityArgMap
            uf.union(c1Id, c2Id)

          } else {
            // Need to try a more expensive check to determine if the functions are identical.
            fullFuncEqualityCheck(c1.func, c2.func) match {
              case None =>
              // No permutation of the inputs was found such that f1 == f2, so we don't keep track of anything.
              case Some(arg1ToArg2Map) =>
                // We found some mapping from f1's inputs to f2's input such that their equations are equal.
                equalCones += (c1Id, c2Id) -> arg1ToArg2Map
                uf.union(c1Id, c2Id)
            }
          }
        }
      }
    }

    // Only keep mappings from cones to the representative.
    val coneToReprArgMap = equalCones.collect {
      case ((c1Id, c2Id), arg1ToArg2Map) if uf.find(c1Id) == c2Id =>
        // c2 is the representative and the arg map is already c1 -> c2, so we
        // keep it as-is
        c1Id -> arg1ToArg2Map

      case ((c1Id, c2Id), arg1ToArg2Map) if c1Id == uf.find(c2Id) =>
        // c1 is the representative, but the arg map is c1 -> c2. We want to
        // have the arg map for c2 -> c1, so we invert it.
        c2Id -> arg1ToArg2Map.map(kv => kv.swap)
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
        Cone // Cone
      ],
      // Groups cones that compute the same function together. We don't care about
      // what they are computing, just that they are computing the same function
      // and therefore don't need an additional custom function to model if already
      // chosen.
      coneIdToReprId: Map[
        Int, // Cone ID
        Int  // Category
      ],
      maxNumConeTypes: Int
  )(implicit
      ctx: AssemblyContext
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
    // - maxNumConeTypes
    //
    //   The maximum number of custom functions supported in hardware.
    //
    // objective function
    // ------------------
    //
    // Maximize the number of instructions that are saved through the use of custom functions,
    // while simultaneously minimizing the number of custom functions used.
    //
    //     max {
    //        (sum_{C_i \in C} x_i * (|C_i|-1)) -
    //        (sum_{CT_i \in CT} y_i)
    //     }
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
    //   (3) Each vertex is covered at most once.
    //
    //         sum_{C_i \in C : v \in C_i} x_i <= 1
    //
    //   (4) The number of custom functions does not surpass the maximum available.
    //
    //         sum_{CT_i \in CT} y_i <= maxNumConeTypes
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
    val conesPerCategory = coneIdToReprId
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
        val coneVar     = coneVars(coneId)
        val coefficient = if (cone.contains(v)) 1 else 0
        constraint.setCoefficient(coneVar, coefficient)
      }
    }

    //   (4) The number of custom functions does not surpass the maximum available.
    //
    //         sum_{CT_i \in CT} y_i <= maxNumConeTypes
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

    //   (4) The number of custom functions does not surpass the maximum available.
    //
    //         sum_{CT_i \in CT} y_i <= maxNumConeTypes
    //
    //       We model this as
    //
    //         0 <= sum_{CT_i \in CT} y_i <= maxNumConeTypes
    //
    val maxNumConesConstraint = solver.makeConstraint(0, maxNumConeTypes, s"maxNumConeTypes_constraint")
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
        val constraint  = solver.makeConstraint(0, 1, s"y_${category} >= x_${coneId}")
        val coneVar     = coneVars(coneId)
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
      val constraint = solver.makeConstraint(
        0,
        coneIds.size,
        s"y_${category} <= ${coneIds.size}"
      )
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
    // Maximize the number of instructions that are saved through the use of custom functions,
    // while simultaneously minimizing the number of custom functions used.
    //
    //     max {
    //        (sum_{C_i \in C} x_i * (|C_i|-1)) -
    //        (sum_{CT_i \in CT} y_i)
    //     }
    //
    val objective = solver.objective()
    cones.foreach { case (coneId, cone) =>
      val coneSize    = cone.size
      val coefficient = coneSize - 1
      val coneVar     = coneVars(coneId)
      objective.setCoefficient(coneVar, coefficient)
    }
    categoryVars.keys.foreach { category =>
      val categoryVar = categoryVars(category)
      objective.setCoefficient(categoryVar, -1)
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

      // Important to transform to a Seq before calling map() as otherwise cones
      // with identical size will be merged together due to the semantics of Set.
      val instructionsSaved = conesUsed.toSeq.map { coneId =>
        val cone = cones(coneId)
        cone.size - 1
      }.sum

      ctx.logger.info {
        s"Solved non-overlapping cone covering problem in ${solver.wallTime()}ms\n" +
          s"Can reduce instruction count by ${instructionsSaved} using ${conesUsed.size} cones covered by ${customFuncsUsed.size} custom functions."
      }

      conesUsed.toSet

    } else {
      ctx.logger.fail(
        s"Could not optimally solve cone cover problem (resultStatus = ${resultStatus})."
      )
      // We return the empty set to signal that no logic instructions could be fused together.
      Set.empty
    }
  }

  def onProcess(
      proc: DefProcess
  )(implicit
      ctx: AssemblyContext
  ): DefProcess = {
    val dependenceGraph = createDependenceGraph(proc)

    val vToConstMap: Map[DepVertex, Constant] = proc.registers
      .filter { reg =>
        reg.variable.varType == ConstType
      }
      .map { constReg =>
        // constants are guaranteed to have a constant assigned to their variable value, so
        // it is safe to call `get`.
        Left(constReg.variable.name) -> constReg.value.get
      }
      .toMap

    // Identify logic clusters in the graph.
    val logicClusters = findLogicClusters(dependenceGraph)
      .filter(cluster => cluster.size > 1) // No point in creating a LUT vector to replace a single instruction.

    ctx.logger.debug {
      val clusterSizesStr = logicClusters.toSeq
        .sortBy(cluster => cluster.size)(Ordering[Int].reverse)
        .zipWithIndex
        .map { case (cluster, idx) =>
          s"cluster_${idx} -> ${cluster.size} instructions"
        }
        .mkString("\n")

      s"Covering ${logicClusters.size} logic clusters in ${proc.id} with sizes:\n${clusterSizesStr}"
    }

    // We assign an Int id to every cone as we will be storing pairs of cones in a hash table later
    // and it is costly to has the Cone itself each time (hashing its id is much faster).
    val cones = ctx.stats.recordRunTime("getFanoutFreeCones") {
      getFanoutFreeCones(
        dependenceGraph,
        logicClusters.flatten,
        ctx.max_custom_instruction_inputs,
        vToConstMap
      ).filter { cone =>
        // We don't care about cones that consist of a single instruction as there
        // is no benefit in doing so (we will not reduce the instruction count).
        cone.size > 1
      }.toSeq
        .sortBy { cone =>
          (cone.arity, cone.size)
        }
        .zipWithIndex
        .map { case (cone, idx) =>
          idx -> cone
        }
        .toMap
    }

    ctx.logger.debug {
      val conesStr = cones.toSeq
        .sortBy { case (coneId, cone) =>
          coneId
        }
        .map { case (coneId, cone) =>
          s"coneId ${coneId} -> ${cone.toString()}"
        }
        .mkString("\n")

      s"There are ${cones.size} fanout-free cones.\n${conesStr}"
    }

    val (coneIdToReprId, coneToReprArgMap) =
      if (ctx.optimize_common_custom_functions) {
        // Cluster cones that compute the same function together. Cones that compute
        // the same function do so under a permutation of their inputs. This permutation
        // is computed between every cone and its representative cone (not between
        // all cones).

        val (ufCones, coneToReprArgMap) = ctx.stats.recordRunTime(
          "identifyCommonFunctions"
        )(identifyCommonFunctions(cones))
        val coneIdToReprId = cones.keys.map { coneId =>
          coneId -> ufCones.find(coneId)
        }.toMap

        ctx.logger.debug {
          s"The cones are clustered into ${ufCones.numberOfSets()} function groups."
        }

        (coneIdToReprId, coneToReprArgMap)

      } else {
        // Consider all cones to be in distinct categories of their own.

        // If all cones are distinct, then their arguments are unique and we don't
        // need to compute whether they are a permutation of another cone's inputs.
        // We use the "identity permutation" as the resulting mapping between the
        // cone's inputs and the representative's (itself) inputs.
        val coneToReprArgMap = cones.keys.map { coneId =>
          val cone = cones(coneId)
          val argMap = Seq
            .tabulate(cone.arity) { idx =>
              PositionalArg(idx) -> PositionalArg(idx)
            }
            .toMap
          coneId -> argMap
        }.toMap

        // A cone is its own representative.
        val coneIdToReprId = cones.keys.map { coneId =>
          coneId -> coneId
        }.toMap

        (coneIdToReprId, coneToReprArgMap)
      }

    ctx.logger.debug {
      coneIdToReprId.toSeq.sorted
        .map { case (coneId, reprId) =>
          s"cone ${coneId} -> representative ${reprId}"
        }
        .mkString("\n")
    }

    // We know which cones compute the same function. We can now solve an optimization
    // problem to choose the best cones to implement knowing which ones come "for free"
    // due to duplicates existing.
    val bestConeIds = ctx.stats.recordRunTime("findOptimalConeCover") {
      findOptimalConeCover(
        cones,
        coneIdToReprId,
        maxNumConeTypes = ctx.max_custom_instructions
      )(ctx)
    }

    ctx.logger.debug {
      val conesStr = bestConeIds
        .map { coneId =>
          coneId -> cones(coneId)
        }
        .toSeq
        .sortBy { case (coneId, cone) =>
          coneId
        }
        .map { case (coneId, cone) =>
          s"coneId ${coneId} -> ${cone.toString()}"
        }
        .mkString("\n")

      s"Selected cones:\n${conesStr}"
    }

    // To visually inspect the mapping for correctness, we dump a highlighted dot file
    // showing each cone category with a different color.
    ctx.logger.dumpArtifact(
      s"dependence_graph_${ctx.logger.countProgress()}_${transformId}_${proc.id}_selectedCustomInstructionCones.dot"
    ) {
      val reprIds = coneIdToReprId.values.toSet

      // Each category has a defined color.
      val reprIdToColor = reprIds
        .zip(CyclicColorGenerator(reprIds.size))
        .toMap

      val clusters = bestConeIds.map { coneId =>
        // We use the ID of the cone as the ID of the cluster.
        coneId -> cones(coneId).vertices
      }.toMap

      // Color all vertices of a cone with its color.
      val clusterColorMap = clusters.keys.map { clusterId =>
        val reprId = coneIdToReprId(clusterId)
        val color  = reprIdToColor(reprId)
        clusterId -> color.toCssHexString()
      }.toMap

      // All vertices should use the string representation of the instruction
      // that generates them, or the name itself (if a primary input).
      val nameMap = dependenceGraph
        .vertexSet()
        .asScala
        .map { v =>
          v match {
            case Left(name)   => v -> name
            case Right(instr) => v -> instr.toString()
          }
        }
        .toMap

      dumpGraph(
        dependenceGraph,
        clusters = clusters,
        clusterColorMap = clusterColorMap,
        vNameMap = nameMap
      )
    }

    // The custom functions to emit. We only emit the representative function of the best cones.
    val reprIdToFunc = bestConeIds.map { coneId =>
      val reprId = coneIdToReprId(coneId)
      val repr   = cones(reprId)
      reprId -> DefFunc(s"func_${reprId}", repr.func)
    }.toMap

    // Replace the cone roots with their custom functions.
    // Note that this keeps all intermediate vertices of the cones in the process body.
    // A subsequent DeadCodeElimination pass will be needed to eliminate these now-unused instructions.
    val newBody = proc.body.map { instr =>
      NameDependence.regDef(instr) match {
        case Seq(rd) =>
          // We have found an instruction that outputs ONE result (instructions
          // that can be replaced by custom LUTs cannot emit multiple outputs).

          bestConeIds.find { coneId =>
            // We check whether there exists a cone whose root writes to the same
            // name as the instruction.
            val cone = cones(coneId)
            val rdCone = cone.root match {
              case Left(name) =>
                // Should not happen as a root is an instruction that computes something. It cannot be name.
                throw new IllegalArgumentException(
                  s"Root ${cone.root} of cone ${cone} should have been an instruction, not a name!"
                )
              case Right(i) =>
                NameDependence.regDef(i).head
            }
            rdCone == rd
          } match {
            case None =>
              // Leave the instruction unchanged as the output of the instruction
              // does not match the root vertex of a cone chosen by the ILP solver.
              instr

            case Some(coneId) =>
              // Replace instruction with a custom instruction. The custom instruction
              // is the one defined by the cone's representative.
              val cone     = cones(coneId)
              val reprId   = coneIdToReprId(coneId)
              val reprFunc = reprIdToFunc(reprId)

              val coneToReprInputMap = coneToReprArgMap(coneId)
              val rsx = cone.posArgToNamedArg
                .map { case (conePosArg, argName) =>
                  val reprPosArg = coneToReprInputMap(conePosArg)
                  reprPosArg -> argName
                }
                .toSeq
                .sortBy { case (reprPosArg, argName) =>
                  reprPosArg.v
                }
                .map { case (reprPosArg, argName) =>
                  argName.v
                }

              CustomInstruction(reprFunc.name, rd, rsx)
          }

        case _ =>
          // Leave the instruction unchanged as the instruction is not the root of a selected cone.
          instr
      }
    }

    val newProc = proc.copy(
      body = newBody,
      functions = reprIdToFunc.values.toSeq
    )

    newProc
  }

  override def transform(
      program: DefProgram
  )(implicit ctx: AssemblyContext): DefProgram = {
    val newProcesses = program.processes.map(proc => onProcess(proc)(ctx))
    val newProgram   = program.copy(processes = newProcesses)
    ctx.stats.record(ProgramStatistics.mkProgramStats(newProgram))
    newProgram
  }

  // Helper method to dump a graph of clusters in DOT format.
  def dumpGraph[V](
      g: Graph[V, DefaultEdge],
      clusters: Map[
        Int,   // Cluster ID
        Set[V] // Vertices in cluster
      ] = Map.empty[Int, Set[V]],
      clusterColorMap: Map[
        Int,   // Cluster ID
        String // Color to be assigned to cluster
      ] = Map.empty[Int, String],
      vNameMap: Map[V, String] = Map.empty[V, String]
  ): String = {
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
    def jg2sc[V](
        jg: Graph[V, DefaultEdge]
    ): (
        SGraph[Int, DiEdge],
        Map[V, Int],
        Map[Int, V]
    ) = {
      val sg    = MSGraph.empty[Int, DiEdge]
      val vToId = MHashMap.empty[V, Int]
      val idToV = MHashMap.empty[Int, V]

      jg.vertexSet().asScala.zipWithIndex.foreach { case (v, idx) =>
        vToId += v   -> idx
        idToV += idx -> v
        sg += idx
      }

      jg.edgeSet().asScala.foreach { e =>
        val src   = jg.getEdgeSource(e)
        val dst   = jg.getEdgeTarget(e)
        val srcId = vToId(src)
        val dstId = vToId(dst)
        sg += DiEdge(srcId, dstId)
      }

      (sg, vToId.toMap, idToV.toMap)
    }

    def escape(s: String): String = s.replaceAll("\"", "\\\\\"")
    def quote(s: String): String  = s"\"${s}\""

    // Some vertices have quotes in them when represented as strings. These vertices
    // cannot be used as graphviz identifiers, so we assign an index to all vertices
    // instead.
    val (sg, vToId, idToV) = jg2sc(g)

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
        iedge: scalax.collection.Graph[Int, DiEdge]#EdgeT
    ): Option[
      (
          DotGraph,   // The graph/subgraph to which this edge belongs.
          DotEdgeStmt // Statements to modify the edge's representation.
      )
    ] = {
      iedge.edge match {
        case DiEdge(source, target) =>
          Some(
            (
              dotRoot, // All edges are part of the root graph.
              DotEdgeStmt(
                NodeId(source.toOuter),
                NodeId(target.toOuter)
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
        inode: scalax.collection.Graph[Int, DiEdge]#NodeT
    ): Option[
      (
          DotGraph,   // The graph/subgraph to which this node belongs.
          DotNodeStmt // Statements to modify the node's representation.
      )
    ] = {
      val clusterId = clusters
        .find { case (clusterId, vertices) =>
          val vId = inode.toOuter
          val v   = idToV(vId)
          vertices.contains(v)
        }
        .map { case (clusterId, vertices) =>
          clusterId
        }

      // If the vertex is part of a cluster, select the corresponding subgraph.
      // Otherwise add the vertex to the root graph.
      val dotGraph = clusterId
        .map(id => dotSubgraphs(id))
        .getOrElse(dotRoot)

      val vId    = inode.toOuter
      val v      = idToV(vId)
      val vName  = vNameMap.getOrElse(v, v.toString())
      val vLabel = escape(vName)

      // If the vertex is part of a cluster, select its corresponding color.
      // Otherwise use white as the default color.
      val vColor = clusterId
        .map(id => clusterColorMap(id))
        .getOrElse("white")

      Some(
        (
          dotGraph,
          DotNodeStmt(
            NodeId(vId),
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
