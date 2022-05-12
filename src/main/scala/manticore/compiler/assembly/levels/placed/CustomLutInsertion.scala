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
  val primaryInputs: Set[V],
  val leaves: Set[V],
  // Contains the root, the leaves, and all intermediate vertices between.
  val vertices: Set[V]
) {

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

  override def toString(): String = {
    val nonRootVertices = vertices - root
    s"(root = ${root}), (body = ${nonRootVertices.mkString(" --- ")}), (primary inputs = ${primaryInputs.mkString(" --- ")})"
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
    new Cone(root, primaryInputs, leaves, vertices)
  }
}

object GraphDump {
  def apply[V](
    g: Graph[V, DefaultEdge],
    vColorMap: Map[V, Color] = Map.empty[V, Color],
    vStringMap: Map[V, String] = Map.empty[V, String]
  ): String = {
    val vertexIdProvider = new Function[V, String] {
      def apply(v: V): String = {
        val vStr = vStringMap.getOrElse(v, v.toString)

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
  ): Set[Cone[Name]] = {
    // Returns a subgraph of the dependence graph containing the logic vertices *and* their inputs
    // (which are not logic vertices by definition). The inputs are needed to perform cut enumeration
    // as otherwise we would not know the in-degree of the first logic vertices in the graph.
    def logicSubgraphWithInputs(): (
      Graph[Name, DefaultEdge],
      Set[Name] // inputs
    ) = {
      // The inputs are vertices *outside* the given set of vertices that are connected
      // to vertices *inside* the cluster.
      val inputs = logicVertices.flatMap { vLogic =>
        Graphs.predecessorListOf(dependenceGraph, vLogic).asScala
      }.filter { vAnywhere =>
        !logicVertices.contains(vAnywhere)
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

    // Enumerate the cuts of every vertex in the graph. We mask out vertices
    // that correspond to constants as the cut enumeration algorithm should not
    // consider constants as potential "primary inputs" of the cuts found.
    val nonConstVertices = gLswi.vertexSet().asScala.filter(v => !nameToConstMap.contains(v))
    val gWithoutConsts = new AsSubgraph(gLswi, nonConstVertices.asJava)
    val cuts = CutEnumerator(gWithoutConsts, primaryInputs, maxCutSize)

    // Now that we have all cuts, we find the cones that they span.
    val allCones = logicVertices.flatMap { root =>
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

  // Returns a map of isomorphic cone tuples (c1, c2) and the mapping between the names of c1 and c2's vertices.
  // If (c1, c2) are not isomorphic, then no entry is entered in the resulting map.
  def identifyCommonFunctions(
    g: Graph[Name, DefaultEdge],
    cones: Set[Cone[Name]],
    nameToConstMap: Map[Name, Constant],
    nameToInstrMap: Map[Name, Instruction]
  ): Map[
    (Cone[Name], Cone[Name]),
    Map[Name,Name]
  ] = {

    def getType(v: Name): Either[BinaryOperator, Constant] = {
      nameToInstrMap.get(v) match {
        case Some(instr) =>
          Left(instr.asInstanceOf[BinaryArithmetic].operator)

        case None =>
          Right(nameToConstMap(v))
      }
    }

    def countConeResources(
      c: Cone[Name]
    ): Map[
      Either[BinaryOperator, Constant],
      Int
    ] = {
      val counts = c.vertices
        .map(v => getType(v))
        .groupBy(identity)
        .map { case (k, v) =>
          k -> v.size
        }

      counts
    }

    def areConesEqual(
      c1: Cone[Name],
      c2: Cone[Name]
    ): Option[ // Returns None if the cones are not equal.
      Map[Name, Name] // Maps names in c1 to names in c2.
    ] = {
      def isValidMapping(
        c1: Cone[Name],
        c2: Cone[Name],
        mapping: GraphMapping[Name, DefaultEdge]
      ): Boolean = {
        // The vertices in c1/c2 must be of the same type for the graphs to truly be isomorphic.
        c1.vertices.forall { v1 =>
          val v2 = mapping.getVertexCorrespondence(v1, true)
          (getType(v1), getType(v2)) match {
            case (Left(op1), Left(op2)) => op1 == op2
            case (Right(const1), Right(const2)) => const1 == const2
            case _ => false
          }
        }
      }

      // We perform an actual isomorphism to detect structural equivalence.
      val gc1 = new AsSubgraph(g, c1.vertices.asJava)
      val gc2 = new AsSubgraph(g, c2.vertices.asJava)

      // The cones may not be trees as there may be intermediate outputs in the cone that
      // get propagated to multiple other intermediate vertices. We therefore cannot use
      // a linear-time isomorphism algorithm like AHU, but must instead use the general-purpose
      // VF2 algorithm. Cones are very small though, so we expect the runtime not to be an issue.
      val isoMappings = new VF2GraphIsomorphismInspector(gc1, gc2).getMappings().asScala

      // Multiple structural mappings may exist. We must try them until we find one where all
      // vertices in gc1 and gc2 match.
      val validMapping = isoMappings.collectFirst {
        case mapping if isValidMapping(c1, c2, mapping) =>
          // Transform to a scala collection
          c1.vertices.map { v1 =>
            v1 -> mapping.getVertexCorrespondence(v1, true)
          }.toMap
      }

      validMapping
    }

    val coneSetsToCompare = cones
      .groupBy { cone =>
        // It only makes sense to compare cones that are composed of the same resources.
        countConeResources(cone)
      }

    val isoConeMappings = coneSetsToCompare
      .flatMap { case (resourceCnt, cones) =>
        // We now have a cluster of cones to compare against each other in a pairwise fashion
        // to identify duplicates.
        val mappings = new MHashMap[(Cone[Name], Cone[Name]), Map[Name, Name]]

        cones.foreach { c1 =>
          cones.foreach { c2 =>
            areConesEqual(c1, c2) match {
              case None =>
              case Some(mapping) =>
                mappings += (c1, c2) -> mapping
            }
          }
        }

        mappings
      }

    isoConeMappings
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

    val cones = getFanoutFreeCones(
      dependenceGraph,
      logicClusters.flatten,
      ctx.max_custom_instruction_inputs,
      nameToConstMap,
      nameToInstrMap
    )

    // Group cones by the function they compute.

    val coneToCustomfuncMap = identifyCommonFunctions(dependenceGraph, cones, nameToConstMap, nameToInstrMap)

    // // Find the optimal cone cover globally. Note that multiple cones may use the same custom
    // // function (if we enable the identification of common functions).
    // val coneToCustomfuncMap = findOptimalConeCover(
    //   dependenceGraph,
    //   logicClusters,
    //   nameToInstrMap,
    //   maxCutSize = ctx.max_custom_instruction_inputs,
    //   maxNumCones = ctx.max_custom_instructions,
    //   identifyCommonFunctions = true
    // )(ctx)

    // // Must convert to a Seq before mapping as otherwise cones of identical size will
    // // "disappear" from the count due to the semantics of Set[Int] (Map.keySet returns
    // // a Set and mapping a Set returns a Set as well).
    // val numSavedInstructions = coneToCustomfuncMap.keySet.toSeq.map(cone => cone.size - 1).sum
    // ctx.logger.info(s"We save ${numSavedInstructions} instructions in ${proc.id} using ${coneToCustomfuncMap.size} custom instructions.")

    // // To visually inspect the mapping for correctness, we dump a highlighted dot file
    // // showing each cone with a different color.
    // ctx.logger.dumpArtifact(
    //   s"dependence_graph_${ctx.logger.countProgress()}_${phase_id}_${proc.id}_selectedCustomInstructionCones.dot"
    // ) {
    //   val colors = CyclicColorGenerator(bestCones.size)

    //   val colorMap = bestCones
    //     .zip(colors)
    //     .flatMap { case (cone, color) =>
    //       cone.vertices.map { v =>
    //         v -> color
    //       }
    //     }.toMap

    //   val stringMap = bestCones
    //     .flatMap { cone =>
    //       cone.vertices.map { v =>
    //         v -> nameToInstrMap(v).toString()
    //       }
    //     }.toMap

    //   GraphDump(dependenceGraph, colorMap, stringMap)
    // }

    // // Compute a custom function for every cone found.

    // // val rootToDefFuncMap = bestCones.zipWithIndex.map { case (cone, idx) =>
    // //   val (customFunc, argToNameMap) = coneToCustomFunction(cone, nameToConstMap, nameToInstrMap)
    // //   // Custom functions are wrapped in a named DefFunc as per the IR spec.
    // //   val defFunc = DefFunc(s"custom_func_${idx}", customFunc)
    // //   // We map the root INSTRUCTION (not its regDef-ed name) to the DefFunc so we can later
    // //   // map the instruction in the process body without extra processing.
    // //   val rootInstr = nameToInstrMap(cone.root)
    // //   rootInstr -> (defFunc, argToNameMap)
    // // }.toMap

    // ctx.logger.debug {
    //   val defFuncsStr = rootToDefFuncMap.map { case (root, customFunc) =>
    //     s"${root} -> ${customFunc}"
    //   }.mkString("\n")
    //   s"Mapped custom functions:\n${defFuncsStr}"
    // }

    // // Replace the cone roots with their custom functions.
    // // Note that this keeps all intermediate vertices of the cones in the process body.
    // // A subsequent DeadCodeElimination pass will be needed to eliminate these now-unused instructions.
    // val newBody = proc.body.map { instr =>
    //   rootToDefFuncMap.get(instr) match {
    //     case Some((defFunc, argToNameMap)) =>
    //       // Replace the instruction with a custom instruction.

    //       // We have the guarantee there is only 1 target register by construction.
    //       val rd = DependenceAnalysis.regDef(instr).head

    //       // The LUT hardware has "arity" inputs, but the custom function may have less
    //       // than this count. Nevertheless, we still need a named for every argument of
    //       // the custom function. We just re-use the first LUT input in all unused inputs.
    //       val defaultInput = argToNameMap(AtomArg(0))

    //       val rsx = Seq.tabulate(ctx.max_custom_instruction_inputs) { argIdx =>
    //         // Unused inputs use the name of constant 0 here.
    //         argToNameMap.get(AtomArg(argIdx)) match {
    //           case Some(name) => name
    //           case None => defaultInput
    //         }
    //       }

    //       CustomInstruction(defFunc.name, rd, rsx)

    //     case None =>
    //       // Leave the instruction unchanged.
    //       instr
    //     }
    // }

    // val newProc = proc.copy(
    //   body = newBody,
    //   functions = rootToDefFuncMap.values.map {
    //     case (defFunc, argToNameMap) => defFunc
    //   }.toSeq
    // )

    // newProc

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
