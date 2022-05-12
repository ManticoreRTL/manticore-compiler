package manticore.compiler.assembly.levels.placed.lowering.util

import manticore.compiler.assembly.levels.placed.PlacedIR._
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.placed.lowering.util.Interval
import manticore.compiler.assembly.levels.placed.lowering.util.IntervalSet
import manticore.compiler.assembly.levels.placed.PlacedIRDependencyDependenceGraphBuilder.DependenceAnalysis
import manticore.compiler.assembly.levels.placed.PlacedIRInputOutputCollector.InputOutputPairs
import manticore.compiler.HasLoggerId
import javax.swing.text.Position


case class PositionedInstruction(instr: Instruction, position: Int)

trait LifetimeAnalysis {
  def of(name: Name): IntervalSet
  def apply(name: Name): IntervalSet = of(name)
  // def instructions: Seq[PositionedInstruction]
}


private[lowering] object LifetimeAnalysis {

  private implicit val loggerId = new HasLoggerId {
    val id = "LifetimeAnalysis"
  }

  def apply(
      process: DefProcess
  )(implicit ctx: AssemblyContext): LifetimeAnalysis = {

    import scala.collection.mutable.ArrayBuffer
    import scala.collection.mutable.{Set => MutableSet}

    sealed trait PhiInput
    case class LabeledPhiInput(label: Label, name: Name) extends PhiInput
    case class GenericPhiInput(name: Name) extends PhiInput
    sealed trait PhiProxy {
      val rd: Name
    }
    case class PhiProxySingleton(rd: Name, rs: Name) extends PhiProxy
    case class PhiProxyLabeled(rd: Name, rss: Seq[(Label, Name)])
        extends PhiProxy

    class CodeBlock(
        val header: Seq[PhiProxy],
        val body: ArrayBuffer[PositionedInstruction] = ArrayBuffer.empty,
        val successors: ArrayBuffer[CodeBlock] = ArrayBuffer.empty,
        val predecessors: ArrayBuffer[CodeBlock] = ArrayBuffer.empty,
        val liveIn: MutableSet[Name] = MutableSet.empty,
        val label: Option[Label] = None
    ) {

      def from: Int = if (body.nonEmpty) body.head.position
      else if (predecessors.nonEmpty) predecessors.map(_.to).max
      else -1
      def to: Int = if (body.nonEmpty) body.last.position else from

      def +=(instr: Instruction) = {
        body += PositionedInstruction(instr, to + 1)
        this
      }
      def ++=(instrs: Seq[Instruction]) = {
        instrs.foreach { this += _ }
        this
      }
    }

    // initial header
    val statePairs = InputOutputPairs.createInputOutputPairs(process)
    val implicitLoopPhis = statePairs.map { case (curr, next) =>
      PhiProxySingleton(curr.variable.name, next.variable.name)
    }
    val rootBlock = new CodeBlock(implicitLoopPhis)
    val orderedBlocks = ArrayBuffer.empty[CodeBlock]
    val leafBlock = process.body.foldLeft(rootBlock) {
      case (currentCodeBlock, instr) =>
        instr match {
          case jtb @ JumpTable(target, results, cases, delaySlot, _) =>
            currentCodeBlock += JumpTable(
              target,
              Seq.empty,
              Seq.empty,
              Seq.empty
            )
            currentCodeBlock ++= delaySlot

            val (nextPos: Int, newCaseCodeBlocks: Seq[CodeBlock]) =
              cases.foldLeft(
                (
                  currentCodeBlock.to + 1,
                  Seq.empty[CodeBlock]
                )
              ) { case ((pos, cblocks), JumpCase(lbl, blk)) =>
                val newCaseBlock = new CodeBlock(
                  header = Seq.empty,
                  predecessors = ArrayBuffer(currentCodeBlock),
                  label = Some(lbl)
                )
                newCaseBlock.body ++= blk.zipWithIndex.map { case (instr, ix) =>
                  PositionedInstruction(instr, pos + ix)
                }

                (newCaseBlock.to + 1, cblocks :+ newCaseBlock)
              }

            val nextHeader = results.map { case Phi(rd, rsx) =>
              PhiProxyLabeled(rd, rsx)
            }
            val nextCodeBlock = new CodeBlock(
              header = nextHeader,
              predecessors = ArrayBuffer.empty ++ newCaseCodeBlocks
            )

            for (caseCBlock <- newCaseCodeBlocks) {
              caseCBlock.successors += nextCodeBlock
            }

            currentCodeBlock.successors ++= newCaseCodeBlocks

            orderedBlocks += currentCodeBlock
            orderedBlocks ++= newCaseCodeBlocks

            nextCodeBlock

          case anyInst =>
            currentCodeBlock += anyInst
        }
    }

    orderedBlocks += leafBlock
    ctx.logger.dumpArtifact("blocks.txt") {

      val text = new StringBuilder()

      def append(codeBlock: CodeBlock): Unit = {

        codeBlock.header.foreach {
          case PhiProxyLabeled(rd, rss) =>
            text ++= s"phi(${rd}, ${rss
              .map { case (lbl, rs) => s"${lbl} ? ${rs}" }
              .mkString(", ")})\n"
          case PhiProxySingleton(rd, rs) =>
            text ++= s"phi(${rd}, ${rs})\n"

        }
        codeBlock.body.foreach { operation =>
          text ++= f"${operation.position}%4d: ${operation.instr.serialized}\n"
        }
        text ++= "\n"

      }
      orderedBlocks.foreach(append)

      text.toString()
    }
    leafBlock.successors += rootBlock
    rootBlock.predecessors += leafBlock

    val intervals = scala.collection.mutable.Map.empty[Name, IntervalSet]

    def addInterval(operand: Name, interval: Interval) = {
      if (intervals.contains(operand)) {
        intervals += (operand -> (intervals(operand) | interval))
      } else {
        intervals += (operand -> IntervalSet(interval))
      }
    }
    def setFrom(operand: Name, from: Int) = {
      if (intervals.contains(operand)) {
        intervals += (operand -> intervals(operand).trimStart(from))
      } else {
        intervals += (operand -> IntervalSet(from, Int.MaxValue))
      }
    }
    def buildInterval(codeBlock: CodeBlock): Unit = {

      for (succ <- codeBlock.successors) {
        // liveIn of this block is the union of liveIns of the successors
        // because anything that is live in the successor is live here as well

        codeBlock.liveIn ++= succ.liveIn
        for (phi <- succ.header) {
          phi match {
            case PhiProxySingleton(rd, rs) =>
              assert(codeBlock.label.isEmpty)
              // any operand used in the successors Phi nodes are also live
              // because they are used in the Phi (but their lifetime
              // interval may end at the end of this block)
              codeBlock.liveIn += rs
            case PhiProxyLabeled(rd, rss) =>
              assert(codeBlock.label.nonEmpty)
              codeBlock.liveIn ++= rss.collect {
                case (lbl, rs) if lbl == codeBlock.label.get => rs
              }
          }
        }
      }

      // for any of the operands live in the successor or the phis of the
      // successor, create lifetime interval containing the this whole block
      // note that if such an operand is defined in this block, we'll trim the
      // start of its interval to the position at which it is defined
      for (liveOperand <- codeBlock.liveIn) {
        addInterval(liveOperand, Interval(codeBlock.from, codeBlock.to + 1))
      }

      // traverse the block body in reverse order
      for (operation <- codeBlock.body.reverseIterator) {
        for (rd <- DependenceAnalysis.regDef(operation.instr)) {
          // trim the start position of the live interval
          // Note that either:
          // 1. rd is not live in any of the successors
          //    then it's lifetime interval will be contained to this block
          //    and since we are reading the instructions backwards, a
          //    definition is visited after some other uses. (see the next for loop)
          //    In this case the lifetime will be a subset of the blocks lifetime
          // 2. rd is used at some successor, then it's lifetime starts from
          //    operations.position and continues to a successor block
          // 3. rd is defined here but never used. That is cause for concern
          //    since we only set the start interval position but not the end
          //    and this signals bad code that does not have all dead codes
          //    removed.
          setFrom(rd, operation.position)
          codeBlock.liveIn -= rd
        }
        for (rs <- DependenceAnalysis.regUses(operation.instr)) {
          addInterval(rs, Interval(codeBlock.from, operation.position + 1))
          codeBlock.liveIn += rs
        }
      }

      for (phi <- codeBlock.header) {

        // remove any phi outputs from this block's live ins (they are defined
        // here so they can no be live at the block's input)
        codeBlock.liveIn -= phi.rd
      }

      if (codeBlock == rootBlock) {
        // handle the implicit loop's backward edge (for state update), anything
        // live at the beginning of this block shall remain live to the end of
        // the
        for (operand <- codeBlock.liveIn) {
          addInterval(operand, Interval(codeBlock.from, leafBlock.to + 1))
        }
      }
    }

    orderedBlocks.reverse.foreach(buildInterval)

    ctx.logger.dumpArtifact("lifetime.txt") {
      intervals
        .map { case (name, intervals) => s"${name}: ${intervals}" }
        .mkString("\n")
    }
    val asFunc = intervals.toMap.withDefault(_ => IntervalSet.empty)
    new LifetimeAnalysis {

      def of(name: Name): IntervalSet = asFunc(name)

    }
  }

}


