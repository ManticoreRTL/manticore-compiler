package manticore.compiler.assembly.levels.placed

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.DependenceGraphBuilder

case class TaggedInstruction(
    instruction: PlacedIR.Instruction,
    tags: Seq[TaggedInstruction.ExecutionTag] = Seq.empty
) {
  def withTagIf(cond: Boolean, t: TaggedInstruction.ExecutionTag) =
    if (cond) copy(tags = tags :+ t) else this

  def has(t: TaggedInstruction.ExecutionTag): Boolean = tags.contains(t)

}

object TaggedInstruction {

  import PlacedIR._
  import PlacedIRDependencyDependenceGraphBuilder.DependenceAnalysis

  def apply(inst: Instruction, tag: ExecutionTag): TaggedInstruction =
    TaggedInstruction(inst, Seq(tag))

  sealed trait ExecutionTag
  case object DelayedExecution
      extends ExecutionTag // an instruction that executes with a delay, i.e., a Switch or Break
  case class PhiSource(rd: Name, rs: Name)
      extends ExecutionTag // an instruction that contributes to a Phi
  case object BorrowedExecution
      extends ExecutionTag // an instruction that executes in the delay slot of another instruction
  case class JumpTarget(label: Label)
      extends ExecutionTag // an instruction that is the target of a jump
  case object BreakTarget extends ExecutionTag

  case class InstructionUntaggablException(
      msg: String
  ) extends Exception(msg)

  /** Flatten the instructions into an indexed block of tagged instructions.
    * Tags are provide some auxiliary information that can either be used in
    * interpretation or low-level transformations close to machine code
    * generation.
    *
    * For instance, a checker can validate a schedule of jumps by ensuring the
    * correct number of [[BorrowedExecution]] tagged instructions after
    * [[DelayedExecution]].
    *
    * An interpreter can use the [[PhiSource]] tag to implicitly perform [[Mov]]
    * instruction in absence of a physical register. It could also use the
    * [[BorrowedExecution]] and [[DelayedExecution]] tags to perform the a
    * delayed jump.
    *
    * The current pass for instance uses the [[JumpTarget]] and [[BreakTarget]]
    * tags to compute label values and break offsets.
    *
    * @param block
    *   a block of instruction to tag
    * @param resBuilder
    *   a sequence of already tag instruction to prepend to the newly tagged
    *   instructions
    * @param ctx
    * @return
    */
  def indexedTaggedBlock(
      block: Seq[Instruction],
      resBuilder: IndexedSeq[TaggedInstruction] =
        IndexedSeq.empty[TaggedInstruction]
  )(implicit ctx: AssemblyContext): IndexedSeq[TaggedInstruction] = {

    require(block.nonEmpty)

    // find all the instructions that immediately follow a JumpTable
    val breakTargets = (block zip block.tail).collect {
      case (i: JumpTable, j) => j
    }.toSet

    /** Tag the instructions in a case body. This function does not handle
      * nested [[JumpTable]]s
      *
      * @param jcase
      *   jump case
      * @param phis
      *   map from phi (label, source) -> target where source is the operand of
      *   a phi appearing in a Phis assignment to target
      * @return
      */
    def caseBodyTagger(
        jcase: JumpCase,
        phis: Map[(Label, Name), Name]
    ): IndexedSeq[TaggedInstruction] = {

      def phiSources(inst: Instruction): Seq[PhiSource] =
        DependenceAnalysis.regDef(inst).collect {
          case n if phis.contains((jcase.label, n)) =>
            PhiSource(phis((jcase.label, n)), n)
        }
      val indexed = jcase.block match {
        case (jtarget: Instruction) :: rest =>
          val first = TaggedInstruction(
            jtarget,
            phiSources(jtarget) :+ JumpTarget(jcase.label)
          )
            .withTagIf(jtarget.isInstanceOf[BreakCase], DelayedExecution)
          rest.foldLeft(
            IndexedSeq(
              first
            )
          ) {
            case (ls, i: BreakCase) =>
              val prev = ls.last
              if (prev has BorrowedExecution) {
                throw InstructionUntaggablException(
                  s"Should not have multiple BREAKs in a case body! ${i}"
                )
              }
              ls :+ TaggedInstruction(i, DelayedExecution)
            case (ls, i) =>
              ls :+ TaggedInstruction(i, phiSources(i)).withTagIf(
                (ls.last has BorrowedExecution) || (ls.last has DelayedExecution),
                BorrowedExecution
              )
          }
        case Nil =>
          throw InstructionUntaggablException(
            "Empty case bodies should not be flattened, ensure that constant used sources in Phi nodes are MOVed to wires!"
          )
          IndexedSeq.empty[TaggedInstruction]
      }
      indexed
    }
    block.foldLeft(resBuilder) {
      case (
            builder,
            jtb @ JumpTable(target, results, blocks, delayedSlot, _)
          ) =>
        val delaySlotInstructions =
          delayedSlot.map(TaggedInstruction(_, BorrowedExecution))

        // this jump instruction is tagged as Delayed since it is a jump,
        // it could also be tagged as break target if it immediately follows
        // another jump table
        val taggedJtb = TaggedInstruction(jtb, DelayedExecution).withTagIf(
          breakTargets(jtb),
          BreakTarget
        )

        val withJumpAndDelaySlot =
          (builder :+ taggedJtb) ++ delaySlotInstructions

        // create a function from (label, source) in a phi to the rd so that
        // we can later "remove" the phi, note that here we assume all case
        // bodies are nonempty, i.e., every phi node should have "wire" operands
        // not constants and the wires should be
        val phisMap = results.flatMap { case Phi(rd, rsx) =>
          rsx.map { _ -> rd }
        }.toMap

        blocks.foldLeft(withJumpAndDelaySlot) { case (b, jcase) =>
          b ++ caseBodyTagger(jcase, phisMap)
        }

      case (builder, inst @ (_: BreakCase | _: ParMux)) =>
        InstructionUntaggablException(
          s"Can create indexedBlock with stranded control instruction ${inst}"
        )
        builder
      case (builder, inst) =>
        builder :+ TaggedInstruction(inst).withTagIf(
          breakTargets(inst),
          BreakTarget
        )
    }
  }

  def indexedTaggedBlock(process: DefProcess)(implicit
      ctx: AssemblyContext
  ): IndexedSeq[TaggedInstruction] = indexedTaggedBlock(process.body)

}
