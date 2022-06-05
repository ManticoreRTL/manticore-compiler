package manticore.compiler.assembly.levels.placed.lowering

import manticore.compiler.assembly.levels.placed.PlacedIRTransformer
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.assembly.levels.UInt16
private[lowering] object SetJumpTargetsTransform extends PlacedIRTransformer {
  import PlacedIR._
  override def transform(program: DefProgram)(implicit
      ctx: AssemblyContext
  ): DefProgram = program.copy(processes = program.processes.map(transform))

  private def transform(
      process: DefProcess
  )(implicit ctx: AssemblyContext): DefProcess = {

    val labelMap = scala.collection.mutable.Map.empty[Label, Int]
    val jumpTablesEnd =
      scala.collection.mutable.ArrayBuffer.empty[(Int, JumpTable)]
    def resolveJumpLabels(
        instructionBlock: Seq[Instruction],
        pc: Int = 0
    ): Int = instructionBlock match {
      case instr +: tail =>
        val nextPc = instr match {
          case jtb @ JumpTable(_, _, blocks, dslot, _) =>
            val c1 = dslot.length + pc
            val c2 = blocks.foldLeft(c1) { case (c, JumpCase(lbl, blk)) =>
              labelMap += (lbl -> c)
              resolveJumpLabels(blk, c)
            }
            c2
          case _ => pc + 1
        }
        resolveJumpLabels(tail, nextPc)
      case Nil => pc
    }

    // assign a pc to every label
    resolveJumpLabels(process.body)

    val memories = process.registers.collect {
      case DefReg(m: MemoryVariable, _, _) => m.name -> m
    }
    // go through every label group and create memory initialization contents
    // for each associated memory.
    val newContents = process.labels.map {
      case grp @ DefLabelGroup(mem, indexer, default, _) =>
        val sortedLabels = indexer.sortBy(_._1.toInt)

        val memContent = default match {
          case None =>
            val isLinearRange =
              sortedLabels.map(_._1.toInt) == Range(0, sortedLabels.length)
            if (!isLinearRange) {
              ctx.logger.error(
                "When there is no default case, labels should cover a linear range starting from 0!",
                grp
              )
            }

            val content = sortedLabels.map { case (index, lbl) =>
              UInt16(labelMap(lbl))
            }
            content
          case Some(catchall) =>
            val sz = memories.find(mem == _._1) match {
              case Some((_, memvar)) => memvar.size
              case None =>
                ctx.logger.error(
                  s"Failed looking up memory for label group!",
                  grp
                )
                0
            }
            val content = Seq
              .tabulate(sz) { index =>
                sortedLabels.find { case (ix, _) => ix.toInt == index } match {
                  case None           => labelMap(catchall)
                  case Some((_, lbl)) => labelMap(lbl)
                }
              }
              .map(UInt16(_))
            content
        }
        mem -> memContent
    }.toMap

    // now scan the registers and replace the label group memories with new
    // ones that have the updated contents

    val newRegs = process.registers.map {
      case r @ DefReg(m: MemoryVariable, _, _) =>
        newContents.get(m.name) match {
          case Some(contents) =>
            r.copy(
              variable = m.copy(initialContent = contents)
            )
          case None => r
        }
      case r => r
    }

    // the final step is assigning jump targets for BreakCase instructions
    val newBody = scala.collection.mutable.ArrayBuffer.empty[Instruction]

    def setBreakTarget(instructionBlock: Seq[Instruction], pc: Int = 0): Int =
      instructionBlock match {
        case (jtb @ JumpTable(_, _, blocks, dslot, _)) +: tail =>
          val pc1 = dslot.length + pc
          val pc2 = blocks.map(_.block.length).sum + pc1
          val newBlocks = blocks.map { case JumpCase(lbl, blk) =>
            JumpCase(
              lbl,
              blk.map {
                case brk: BreakCase => brk.copy(target = pc2 + 1)
                case i              => i
              }
            )
          }
          newBody += jtb.copy(blocks = newBlocks).setPos(jtb.pos)
          setBreakTarget(tail, pc2 + 1)
        case instr +: tail =>
          newBody += instr
          setBreakTarget(tail, pc + 1)
        case Nil =>
          pc
      }
    setBreakTarget(process.body)

    process
      .copy(
        body = newBody.toSeq,
        registers = newRegs
      )
      .setPos(process.pos)
  }

}
