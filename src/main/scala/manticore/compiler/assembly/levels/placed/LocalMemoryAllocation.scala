package manticore.compiler.assembly.levels.placed

import manticore.compiler.assembly.levels.AssemblyTransformer

import manticore.compiler.assembly.levels.placed.PlacedIR._
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.MemoryType
import manticore.compiler.assembly.levels.UInt16

/** A pass to set the pointer values in each process or fail if more memory is
  * needed than available.
  * @author
  *   Mahyar Emami <mahyar.emami@eplf.ch>
  */
object LocalMemoryAllocation extends PlacedIRTransformer {

  private def allocateMemory(
      proc: DefProcess
  )(implicit ctx: AssemblyContext): DefProcess = {

    val (mem_end: Int, new_regs: Seq[DefReg]) =
      proc.registers.foldLeft((0, Seq.empty[DefReg])) {
        case ((base: Int, prev_regs: Seq[DefReg]), r: DefReg) =>
          r.variable match {
            case MemoryVariable(_, size, _, _) =>
              val with_base = r.copy(value = Some(UInt16(base))).setPos(r.pos)
              val next_base = base + size
              (next_base, prev_regs :+ with_base)
            case _ =>
              (base, prev_regs :+ r)
          }
      }

    // maximum number of short we can fit in a local memory
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
      prog: DefProgram
  )(implicit context: AssemblyContext): DefProgram = {

    prog.copy(
      processes = prog.processes.map(allocateMemory)
    )
  }

}
