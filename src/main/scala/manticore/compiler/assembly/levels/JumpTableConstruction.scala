package manticore.compiler.assembly.levels

import manticore.compiler.assembly.DependenceGraphBuilder
import manticore.compiler.assembly.levels.CanCollectInputOutputPairs

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.BinaryOperator

import scalax.collection.Graph
import scalax.collection.mutable.{Graph => MutableGraph}
import scalax.collection.GraphEdge
import scalax.collection.edge.LDiEdge
import javax.xml.crypto.Data
import manticore.compiler.assembly.annotations.Memblock
import manticore.compiler.assembly.annotations.Track

/** Construct JumpTables from ParMux instructions where it is beneficial
  *
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */
trait JumpTableConstruction
    extends DependenceGraphBuilder
    with CanCollectInputOutputPairs {

  import flavor._

  val Zero: Constant
  def uniqueLabel(ctx: AssemblyContext): Label
  def indexSequence(to: Int): Seq[Constant]
  def mkMemory(width: Int)(implicit ctx: AssemblyContext): DefReg
  def mkWire(width: Int)(implicit ctx: AssemblyContext): DefReg
  def mkConstant(width: Int, value: Constant)(implicit
      ctx: AssemblyContext
  ): DefReg

  def do_transform(prog: DefProgram)(implicit
      ctx: AssemblyContext
  ): DefProgram = {

    prog.copy(processes = prog.processes.map(do_transform))
  }

  protected sealed trait JumpTableBuildRecipe
  case class JumpTableConstructible(
      instrs: Seq[Instruction],
      defs: Seq[DefReg],
      labels: DefLabelGroup,
      deadInstrs: Set[Instruction]
  ) extends JumpTableBuildRecipe
  case object JumpTableEmpty extends JumpTableBuildRecipe

  def mkJumpTable(
      pmux: ParMux,
      dataDependenceGraph: Graph[Instruction, LDiEdge],
      definingInstructions: Map[Name, Instruction],
      constants: Map[Name, Constant],
      registers: Map[Name, DefReg],
      postDomRelation: PostDominanceRelation
  )(implicit ctx: AssemblyContext): JumpTableBuildRecipe = {
    import scala.collection.mutable.ArrayBuffer
    import scala.collection.mutable.Queue
    import scala.collection.mutable.Set

    sealed trait TableLookupLogic
    case class FastLookup(
        target: Name,
        defs: Seq[DefReg],
        body: Seq[Instruction],
        labelGroup: DefLabelGroup
    ) extends TableLookupLogic
    case class SlowLookup(
        target: Name,
        defs: Seq[DefReg],
        body: Seq[Instruction],
        labelGroup: DefLabelGroup
    ) extends TableLookupLogic

    case object CanNotLookup extends TableLookupLogic

    val instructionsToRemove =
      scala.collection.mutable.Set.empty[Instruction]

    def globallyScoped(rd: Name): Boolean = {
      val rDef = registers(rd)
      val isTracked = rDef.annons.exists {
        case _: Track => true
        case _        => false
      }
      val isState = rDef.variable.varType == OutputType
      isTracked || isState
    }

    /** Performs a backward DFS to find all the instruction that can be put
      * together in one case block of a jump table. The given node should be Nth
      * choice of a ParMux.The returned sequence can be readily used to construct
      * the case body (is ordered)
      * @param node
      * @return
      */

    def traverse(
        sinkNode: dataDependenceGraph.NodeT
    )(implicit postDominated: Instruction => Boolean): Seq[Instruction] = {

      val resultStack = scala.collection.mutable.Stack.empty[Instruction]

      val visited =
        scala.collection.mutable.Set.empty[dataDependenceGraph.NodeT]

      def doDfs(currentNode: dataDependenceGraph.NodeT): Unit = {

        if (!visited(currentNode)) {
          val inst = currentNode.toOuter
          val isLocal =
            DependenceAnalysis.regDef(inst).forall(globallyScoped(_) == false)
          val isNotParMux =
            !inst.isInstanceOf[ParMux] && !inst.isInstanceOf[JumpTable]
          if (isLocal && isNotParMux && postDominated(inst)) {
            visited += currentNode
            for(predecessor <- currentNode.diPredecessors) {
              doDfs(predecessor)
            }
            resultStack push inst
          } // else {
          // this node is a dead-end, there is no point in continuing the search
          // because either
          // 1. the node computes a value that is supposed to be globally available
          // and thus can not be placed in a case body
          // 2. is a parmux or a jump table, therefore can not be inside another
          // jump table
          // 3. is not post-dominated by the instruction that directly defines
          // a value connected to one of the parmux cases, so any predecessors
          // of it are also not post-dominated by the parmux (i.e., they are
          // used to compute something else as-well)
          // }

        }
      }
      doDfs(sinkNode)
      resultStack.popAll().toSeq.reverse
    }

    def createCaseBody(rs: Name): Seq[Instruction] =
      definingInstructions.get(rs) match {
        case None => Seq.empty // case body is empty
        case Some(defInst) =>
          val node = dataDependenceGraph get defInst
          traverse(node) { predInst =>
            postDomRelation.isDefined(defInst, predInst)
          }
      }

    // collect the instructions required to compute the default case
    val defaultCaseBody = createCaseBody(pmux.default)
    // collect the instructions required to compute each case
    val conditionalCaseBodies = pmux.choices.map { case ParMuxCase(_, rs) =>
      createCaseBody(rs)
    }
    instructionsToRemove ++= defaultCaseBody
    conditionalCaseBodies.foreach { instructionsToRemove ++= _ }

    val numInstrInSlowestCase =
      defaultCaseBody.length.max(conditionalCaseBodies.map(_.length).max)
    // traversing the conditions is slightly different, we first have to check
    // if the conditions are computed with parallel SEQ wii, wi, Ci where Cis
    // are distinct constants. This enables us to create a jump table where
    // all condition evaluations are replaced by a single memory load.

    def tryCreateTableLookup(): TableLookupLogic = {

      // we assume the conditions are immediately available, i.e., there are
      // not aliased, so we we look up the instruction that defines the condition
      // we the the SEQ instruction already (and if we don't then we do not look further).
      def isComparisonWithConst(pcase: ParMuxCase): Option[(Name, Constant)] =
        definingInstructions.get(pcase.condition) match {
          case None =>
            ctx.logger.warn("ParMux instruction can be optimized out!", pmux)
            None
          case Some(cond_inst) =>
            cond_inst match {
              case BinaryArithmetic(
                    BinaryOperator.SEQ,
                    pcase.condition,
                    op1,
                    op2,
                    _
                  ) =>
                (constants.get(op1), constants.get(op2)) match {
                  case (Some(c1), Some(c2)) =>
                    ctx.logger.warn(
                      s"ParMux condition ${pcase.condition} can be optimized out!",
                      pmux
                    )
                    None
                  case (Some(c1), None) =>
                    Some((op2 -> c1))
                  case (None, Some(c2)) => Some(op1 -> c2)
                  case (None, None)     => None
                }
              case _ => None
            }
        }

      def mkSlowLookup(): TableLookupLogic = {
        // Arbitrary conditions are less optimal, since we will have to create code
        // like the following:
        // original:
        // [compute cond1]
        // [...]
        // [compute condn]
        // becomes:
        // [compute cond1]
        // MUX t1, cond1, ldefault, l1
        // [...]
        // [compute condn]
        // MUX tn, condn, ln-1, ln
        // JUMP tn
        // Note that in this case, we are removing instructionsToRemove.size instructions
        // but adding n + 1 instruction to compute the jump target and assuming the
        // longest case takes M instructions, we also need to add another instruction
        // to get out of the case. So in total we will have M + n + 2 instructions
        // and we need to make sure that this is better than instructionsToRemove.size
        val labelIndices = indexSequence(pmux.choices.length + 1)
        val memoryAddressWidth =
          BigInt(labelIndices.length - 1).bitLength // basically log2ceil
        assert(
          labelIndices.length < 1024,
          s"Did not expect such a wide ParMux ${pmux}"
        )
        val constDefs = labelIndices.map(mkConstant(memoryAddressWidth, _))
        val memDef = mkMemory(memoryAddressWidth)
        val instr = ArrayBuffer.empty[Instruction]
        val wires = ArrayBuffer.empty[DefReg]
        val lookupValue = (pmux.choices
          .zip(constDefs.tail))
          .foldLeft(constDefs.head) {
            case (prev, (ParMuxCase(cond, _), curr)) =>
              val w = mkWire(memoryAddressWidth)
              wires += w
              instr +=
                Mux(
                  w.variable.name,
                  cond,
                  prev.variable.name,
                  curr.variable.name
                ).setPos(pmux.pos)
              w
          }

        val index = mkWire(memoryAddressWidth)
        instr ++= Seq(
          Lookup(
            index.variable.name,
            lookupValue.variable.name,
            memDef.variable.name
          ).setPos(pmux.pos)
        )

        val labelGroup = DefLabelGroup(
          memory = memDef.variable.name,
          indexer = labelIndices map { _ -> uniqueLabel(ctx) },
          default = None
        )
        val addedInstructions = instr.length + numInstrInSlowestCase
        val muxTreeInstructions = pmux.choices.length
        val removedInstructions =
          instructionsToRemove.size + muxTreeInstructions
        ctx.logger.debug(
          s"Converting pmux to jump table will add ${addedInstructions} instructions and remove ${removedInstructions}",
          pmux
        )
        if (addedInstructions < removedInstructions)
          SlowLookup(
            index.variable.name,
            constDefs ++ Seq(memDef, index) ++ wires,
            instr.toSeq,
            labelGroup
          )
        else
          CanNotLookup

      }
      val constCompares = pmux.choices.map { isComparisonWithConst }
      val allCompareWithConstant = constCompares.forall(_.nonEmpty)

      if (allCompareWithConstant) {

        // all comparisons are with a constant, now let's make sure all the names
        // in the comparison are the same
        val sameNames =
          constCompares.tail.forall(p => p.get._1 == constCompares.head.get._1)
        // we also check if all the constants are different, this is more of a
        // sanity check to make sure the original ParMux was correct one
        val distinctConstants =
          constCompares.distinctBy(_.get._2).length == constCompares.length

        if (!distinctConstants && sameNames) {
          ctx.logger.error(
            "ParMux is malformed, conditions are not mutually exclusive!",
            pmux
          )
        }

        // get the register definition for the value used in the comparison
        val compareValue = registers(constCompares.head.get._1)

        if (sameNames && distinctConstants) {
          if (compareValue.variable.width > 10) {
            // if the range is too large error out for now
            ctx.logger.error("Mux lookup table is very large!")
            CanNotLookup
          } else {

            // create a memory
            val memory = mkMemory(compareValue.variable.width)

            val indexAddress = mkWire(compareValue.variable.width)

            val index = mkWire(compareValue.variable.width)

            val labelGroup = DefLabelGroup(
              memory = memory.variable.name,
              indexer = constCompares.map { x => x.get._2 -> uniqueLabel(ctx) },
              default = Some(uniqueLabel(ctx))
            ).setPos(pmux.pos)

            val jumpTargetComputation = Seq[Instruction](
              Lookup(
                index.variable.name,
                compareValue.variable.name,
                memory.variable.name
              ).setPos(pmux.pos)
            )
            // add the SEQ instruction to the set instructions to be removed
            instructionsToRemove ++= pmux.choices.map {
              case ParMuxCase(cond, _) => definingInstructions(cond)
            }
            FastLookup(
              index.variable.name,
              Seq(memory, indexAddress, index),
              jumpTargetComputation,
              labelGroup
            )
          }

        } else {
          // create the table lookup using a tree of MUXes
          mkSlowLookup()
        }
      } else {
        mkSlowLookup()
      }
    }

    // Note that the default case is always prepended to the other cases,
    // this is the reverse of the usual Verilog/C switch statement syntax
    tryCreateTableLookup() match {
      case l @ FastLookup(index, defs, body, labelGroup) =>
        JumpTableConstructible(
          body :+ JumpTable(
            target = index,
            results = Seq(
              Phi(
                pmux.rd,
                (labelGroup.default.get +: labelGroup.indexer.map(_._2))
                  .zip(pmux.default +: pmux.choices.map(_.choice))
              )
            ),
            blocks = (labelGroup.default.get +: labelGroup.indexer.map(_._2))
              .zip(
                defaultCaseBody +: conditionalCaseBodies
              )
              .map { case (l, b) => JumpCase(l, b.toSeq) }
          ).setPos(pmux.pos),
          defs,
          labelGroup,
          instructionsToRemove.toSet
        )
      case l @ SlowLookup(index, defs, body, labelGroup) =>
        JumpTableConstructible(
          body :+ JumpTable(
            target = index,
            results = Seq(
              Phi(
                pmux.rd,
                labelGroup.indexer
                  .map(_._2)
                  .zip(pmux.default +: pmux.choices.map(_.choice))
              )
            ),
            blocks = labelGroup.indexer
              .map(_._2)
              .zip(defaultCaseBody +: conditionalCaseBodies)
              .map { case (l, b) => JumpCase(l, b.toSeq) }
          ).setPos(pmux.pos),
          defs,
          labelGroup,
          instructionsToRemove.toSet
        )
      case CanNotLookup =>
        JumpTableEmpty
    }

  }

  /** Translate wide ParMux nodes into JumpTables
    *
    * @param proc
    * @param ctx
    * @return
    */

  private def do_transform(
      process: DefProcess
  )(implicit ctx: AssemblyContext): DefProcess = {

    val withJumpTable = constructJumpTable(process)

    withJumpTable

  }
  private def constructJumpTable(
      proc: DefProcess
  )(implicit ctx: AssemblyContext): DefProcess = {

    import scalax.collection.mutable.{Graph => MutableGraph}
    val dataDependenceGraph =
      DependenceAnalysis.build(process = proc, label = (_, _) => None)

    val postDominators = computePostDominators(proc)

    // We start by creating a dependence graph that only contains paths that
    // lead to ParMux instructions

    // A map of from uses to definitions
    val definingInstructions: Map[Name, Instruction] =
      DependenceAnalysis.definingInstructionMap(proc)
    val constants = proc.registers.collect {
      case DefReg(v, Some(x), _) if v.varType == ConstType => v.name -> x
    }.toMap
    val registers = proc.registers.map { r => r.variable.name -> r }.toMap
    val parmuxInsts = proc.body.collect { case i: ParMux => i }

    val jumpTabs = parmuxInsts.map { i =>
      i -> mkJumpTable(
        i,
        dataDependenceGraph,
        definingInstructions,
        constants,
        registers,
        postDominators
      )
    }

    val instrsToRemove =
      jumpTabs.foldLeft(scala.collection.mutable.Set.empty[Instruction]) {
        case (s, _ -> JumpTableConstructible(_, _, _, deads)) => s ++= deads
        case (s, _ -> JumpTableEmpty)                         => s
      }

    val builder = scala.collection.mutable.ArrayBuffer.empty[Instruction]
    proc.body.foreach {
      case pmux: ParMux =>
        jumpTabs.find(pmux == _._1) match {
          case None => ctx.logger.error("Some ParMux is unaccounted for!", pmux)
          case Some(pmux -> jmp) =>
            jmp match {
              case JumpTableConstructible(instrs, defs, labels, deadInstrs) =>
                builder ++= instrs
              case JumpTableEmpty => builder += pmux
            }
        }
      case i: Instruction =>
        if (!instrsToRemove.contains(i)) {
          builder += i
        } else {
          ctx.logger.debug("Removing instruction ", i)
        }

    }

    val newRegs = proc.registers ++ jumpTabs.flatMap {
      case _ -> JumpTableConstructible(_, defs, _, _) => defs
      case _ -> JumpTableEmpty                        => Seq.empty[DefReg]
    }
    val labels = jumpTabs.collect {
      case _ -> JumpTableConstructible(_, _, lgrp, _) => lgrp
    }

    proc.copy(
      body = builder.toSeq,
      registers = newRegs,
      labels = labels
    )
  }

  /** Compute the post dominators for all the instruction in the process (does
    * not look inside jump tables if any)
    *
    * @param process
    * @param ctx
    * @return
    */
  private def computePostDominators(
      process: DefProcess
  )(implicit ctx: AssemblyContext): PostDominanceRelation = {

    abstract sealed trait Vertex {
      def index: Int
    }
    case object EntryV extends Vertex {
      def index = 0
    }
    case object ExitV extends Vertex {
      def index = 1
    }
    case class InstrV(instr: Instruction, index: Int) extends Vertex

    val definingInstructions =
      DependenceAnalysis.definingInstruction(process.body)
    val statePairs = InputOutputPairs.createInputOutputPairs(process)
    val reverseDataflowGraph = MutableGraph.empty[Vertex, GraphEdge.DiEdge]

    val indexOf = process.body.zipWithIndex.toMap andThen { _ + 2 }

    for (instr <- process.body) {

      reverseDataflowGraph += InstrV(instr, indexOf(instr))

      for (use <- DependenceAnalysis.regUses(instr)) {
        for (defInst <- definingInstructions.get(use)) {
          reverseDataflowGraph += GraphEdge.DiEdge(
            InstrV(instr, indexOf(instr)) -> InstrV(
              defInst,
              indexOf(defInst)
            )
          )
        }
      }
    }

    val bottomInstrs = reverseDataflowGraph.nodes.collect {
      case n if n.inDegree == 0 => n.toOuter
    }

    for (iVertex <- bottomInstrs) {

      reverseDataflowGraph += GraphEdge.DiEdge(EntryV, iVertex)

    }

    val topInstrs = reverseDataflowGraph.nodes.collect {
      case n if n.outDegree == 0 => n.toOuter
    }

    for (iVertex <- topInstrs) {

      reverseDataflowGraph += GraphEdge.DiEdge(iVertex, ExitV)

    }

    import scala.collection.immutable.BitSet

    val numNodes = process.body.length + 2 // +2 for EntryV and ExitV

    val universalSet = BitSet.fromSpecific(Range(0, numNodes))
    // initialize all the dominators to the universal set
    // initialize the post dominator sets, each node i is a post dominator of itself
    val postDominators = Array.fill(process.body.length + 2) { universalSet }
    // the entry node should be initialized to itself though
    postDominators(EntryV.index) = BitSet(EntryV.index)

    val visited =
      scala.collection.mutable.Set.empty[Graph[Vertex, GraphEdge.DiEdge]#NodeT]

    def dfsLikeTraversal(node: Graph[Vertex, GraphEdge.DiEdge]#NodeT): Unit = {

      if (!visited(node)) {
        visited += node
      }

      // compute the intersection of the post dominators of the predecessors
      val predecessors = node.diPredecessors.toSeq
      val predIntersect = predecessors match {
        case first +: rest =>
          rest.foldLeft(postDominators(first.toOuter.index)) {
            case (intersect, pred) =>
              intersect & postDominators(pred.toOuter.index)
          }
        case Nil => BitSet()
      }

      // update the using
      // pdom(n) = {n} union (intersect over predecessor p of n pdom(p))
      postDominators(node.toOuter.index) =
        BitSet(node.toOuter.index) | predIntersect

      for (pred <- node.diSuccessors) {
        dfsLikeTraversal(pred)
      }
    }

    dfsLikeTraversal(reverseDataflowGraph get EntryV)

    ctx.logger.dumpArtifact(s"post_dominators.txt") {
      val text = new StringBuilder
      text ++= "Post Dominators: \n"
      text ++= s"(1) BEGIN\n"
      for (instr <- process.body) {
        val index = indexOf(instr)
        text ++= s"(${index}) ${instr}: ${postDominators(index).mkString(", ")}\n"
      }
      text ++= s"(0) END\n"
      text.toString()
    }

    // wrap the computation in a closure, better than return the individual
    // mutable bits of computation
    val postDom = new PostDominanceRelation {
      def isDefined(
          postDominator: Instruction,
          postDominated: Instruction
      ): Boolean = {
        val pDominatorIndex = indexOf(postDominator)
        val pDominatedIndex = indexOf(postDominated)
        postDominators(pDominatedIndex).contains(pDominatorIndex)
      }
    }
    postDom
  }

  private trait PostDominanceRelation {
    // checks whether postDominator post-dominates postDominated
    def isDefined(
        postDominator: Instruction,
        postDominated: Instruction
    ): Boolean
  }

}
