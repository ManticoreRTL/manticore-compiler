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
import manticore.compiler.assembly.levels.ConstType

object CustomLutInsertion
    extends DependenceGraphBuilder
    with AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {

  val flavor = PlacedIR
  import PlacedIR._

  import CustomFunctionImpl._

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

    def isPrimaryInput(v: V): Boolean = {
      primaryInputs.contains(v)
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
    vColorMap: Map[V, Color] = Map.empty[V, Color],
    vToInstrMap: Map[V, Instruction] = Map.empty[V, Instruction]
  ): String = {
    val vertexIdProvider = new Function[V, String] {
      def apply(v: V): String = {
        val vStr = vToInstrMap.getOrElse(v, v.toString)

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
  ): (
    Graph[Name, DefaultEdge],
    Map[Name, Instruction]
   ) = {
    val g: Graph[Name, DefaultEdge] = new DirectedAcyclicGraph(classOf[DefaultEdge])

    val nameToInstrMap = MHashMap.empty[Name, Instruction]

    val constNames = proc.registers.filter { reg =>
      reg.variable.varType == ConstType
    }.map { constReg =>
      constReg.variable.name
    }.toSet

    proc.body.foreach { instr =>
      // Add a vertex for every register that is written and associate it with its instruction.
      val regDefs = DependenceAnalysis.regDef(instr)
      regDefs.foreach { rd =>
        nameToInstrMap += rd -> instr
        g.addVertex(rd)
      }

      // Add a vertex for every register that is read.
      // However, we explicitly do not create vertices for constants as they do not contribute
      // to cuts (they are not an "input" since their value is known at compile time).
      val regUses = DependenceAnalysis.regUses(instr)
      val nonConstRegUses = regUses.filter { rs =>
        !constNames.contains(rs)
      }
      nonConstRegUses.foreach { rs =>
        // The vertex will not be added again if it already exists (defined by a previous
        // instruction).
        g.addVertex(rs)
      }

      // Add edges between regDefs and regUses (excepts to constants).
      regDefs.foreach { rd =>
        nonConstRegUses.foreach { rs =>
          g.addEdge(rs, rd)
        }
      }
    }

    (g, nameToInstrMap.toMap)
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

  /**
    * Groups the instructions that make up the input clusters into a set of
    * non-overlapping cones that can be transformed into a custom instruction.
    * We solve the non-overlapping cone problem optimally with an ILP formulation.
    */
  def findOptimalConeCover(
    g: Graph[Name, DefaultEdge],
    clusters: Set[Set[Name]],
    nameToInstrMap: Map[Name, Instruction],
    maxCutSize: Int,
    maxNumCones: Int
  )(
    implicit ctx: AssemblyContext
  ): Set[Cone[Name]] = {

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
    val allClustersVertices = clusters.flatten
    assert(
      allClustersVertices.forall(v => isLogicInstr(v, nameToInstrMap)),
      s"Error: The input clusters do not all contain logic instructions!"
    )

    val (clusterSubgraph, primaryInputs) = getClustersSubgraph(g, allClustersVertices)
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
      cone.exists(v => !isLogicInstr(v, nameToInstrMap))
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

  def coneToCustomFunction(
    cone: Cone[Name],
    nameToConstMap: Map[Name, Constant],
    nameToInstrMap: Map[Name, Instruction]
  ): (
    CustomFunction, // The custom function of this cone.
    Map[AtomArg, Name] // The names of the custom function's inputs.
  ) = {

    // We already know all primary inputs of the cone. We therefore assign an
    // AtomArg with an increasing index to each so we can refer to them when
    // constructing the cone's expression tree.
    val nameToAtomArgMap = cone.primaryInputs.zipWithIndex.map { case (pi, idx) =>
      pi -> AtomArg(idx)
    }.toMap

    def makeExpr(
      name: Name
    ): CustomFunctionImpl.ExprTree = {
      if (nameToAtomArgMap.contains(name)) {
        // Base case: The name is a primary input.
        val atomArg = nameToAtomArgMap(name)
        IdExpr(atomArg)

      } else if (nameToConstMap.contains(name)) {
        // Base case: The name is a constant.
        val const = nameToConstMap(name)
        IdExpr(AtomConst(const))

      } else {
        // General case: Create expression for each argument of the instruction and
        // join them to form an expression tree.
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

    val rootName = cone.root
    val rootExpr = makeExpr(rootName)
    val customFunc = CustomFunctionImpl(rootExpr)
    val atomArgToNameMap = nameToAtomArgMap.map(kv => kv.swap)

    (customFunc, atomArgToNameMap)
  }

  def onProcess(
    proc: DefProcess
  )(
    implicit ctx: AssemblyContext
  ): DefProcess = {
    // The dependence graph does NOT contain constants as we don't want them to influence
    // the cuts we are generating (they are not "inputs" since constants are known at
    // compile-time).
    val (dependenceGraph, nameToInstrMap) = createDependenceGraph(proc)

    // Identify logic clusters in the graph.
    val logicClusters = findLogicClusters(dependenceGraph, nameToInstrMap)
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
      nameToInstrMap,
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
      s"dependence_graph_${ctx.logger.countProgress()}_${phase_id}_${proc.id}_selectedCustomInstructionCones.dot"
    ) {
      val colors = CyclicColorGenerator(bestCones.size)
      val colorMap = bestCones
        .zip(colors)
        .flatMap { case (cone, color) =>
          cone.vertices.map { v =>
            v -> color
          }
        }.toMap

      dumpGraph(dependenceGraph, colorMap, nameToInstrMap)
    }

    // Compute a custom function for every cone found.

    val nameToConstMap = proc.registers.filter { reg =>
      reg.variable.varType == ConstType
    }.map { constReg =>
      // constants are guaranteed to have a constant assigned to their variable value, so
      // it is safe to call `get`.
      constReg.variable.name -> constReg.value.get
    }.toMap

    val rootToDefFuncMap = bestCones.zipWithIndex.map { case (cone, idx) =>
      val (customFunc, argToNameMap) = coneToCustomFunction(cone, nameToConstMap, nameToInstrMap)
      // Custom functions are wrapped in a named DefFunc as per the IR spec.
      val defFunc = DefFunc(s"custom_func_${idx}", customFunc)
      // We map the root INSTRUCTION (not its regDef-ed name) to the DefFunc so we can later
      // map the instruction in the process body without extra processing.
      val rootInstr = nameToInstrMap(cone.root)
      rootInstr -> (defFunc, argToNameMap)
    }.toMap

    ctx.logger.debug {
      val defFuncsStr = rootToDefFuncMap.map { case (root, customFunc) =>
        s"${root} -> ${customFunc}"
      }.mkString("\n")
      s"Mapped custom functions:\n${defFuncsStr}"
    }

    // Replace the cone roots with their custom functions.
    // Note that this keeps all intermediate vertices of the cones in the process body.
    // A subsequent DeadCodeElimination pass will be needed to eliminate these now-unused instructions.
    val newBody = proc.body.map { instr =>
      rootToDefFuncMap.get(instr) match {
        case Some((defFunc, argToNameMap)) =>
          // Replace the instruction with a custom instruction.

          // We have the guarantee there is only 1 target register by construction.
          val rd = DependenceAnalysis.regDef(instr).head

          // The LUT hardware has "arity" inputs, but the custom function may have less
          // than this count. Nevertheless, we still need a named for every argument of
          // the custom function. We just re-use the first LUT input in all unused inputs.
          val defaultInput = argToNameMap(AtomArg(0))

          val rsx = Seq.tabulate(ctx.max_custom_instruction_inputs) { argIdx =>
            // Unused inputs use the name of constant 0 here.
            argToNameMap.get(AtomArg(argIdx)) match {
              case Some(name) => name
              case None => defaultInput
            }
          }

          CustomInstruction(defFunc.name, rd, rsx)

        case None =>
          // Leave the instruction unchanged.
          instr
        }
    }

    val newProc = proc.copy(
      body = newBody,
      functions = rootToDefFuncMap.values.map {
        case (defFunc, argToNameMap) => defFunc
      }.toSeq
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
