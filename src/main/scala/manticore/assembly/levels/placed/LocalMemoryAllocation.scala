package manticore.assembly.levels.placed

import manticore.assembly.levels.AssemblyTransformer

import manticore.assembly.levels.placed.PlacedIR._
import manticore.compiler.AssemblyContext
import manticore.assembly.levels.MemoryType
import manticore.assembly.levels.UInt16

object LocalMemoryAllocation
    extends AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {

  private def allocateMemory(
      proc: DefProcess
  )(implicit ctx: AssemblyContext): DefProcess = {

    val (mem_end: Int, new_regs: Seq[DefReg]) =
      proc.registers.foldLeft((0, Seq.empty[DefReg])) {
        case ((base: Int, prev_regs: Seq[DefReg]), r: DefReg) =>
          r.variable match {
            case MemoryVariable(_, _, block @ MemoryBlock(_, cap, width, _)) =>
              val with_base = r.copy(value = Some(UInt16(base))).setPos(r.pos)
              val next_base = if (width <= 16) {
                base + cap
              } else { // wide words
                val num_shorts_in_words = (width - 1) / 16 + 1
                val usage = cap * num_shorts_in_words
                base + usage
              }
              (next_base, prev_regs :+ with_base)
            case _ =>
              (base, prev_regs :+ r)
          }
      }

    val max_local_mem_size = ctx.max_local_memory / (16 / 8)
    if (mem_end > max_local_mem_size) {
      ctx.logger.error(
        s"Could not allocate local memory in process ${proc.id}! " +
          s"Needed ${mem_end} short words but only have ${max_local_mem_size}"
      )
    }

    proc
      .copy(
        registers = new_regs
      )
      .setPos(proc.pos)

  }
  override def transform(
      prog: DefProgram,
      context: AssemblyContext
  ): DefProgram = {

    prog.copy(
      processes = prog.processes.map(allocateMemory(_)(context))
    )
  }

}
