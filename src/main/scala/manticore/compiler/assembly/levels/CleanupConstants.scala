package manticore.compiler.assembly.levels

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.ConstType

import manticore.compiler.assembly.levels.Flavored
import manticore.compiler.assembly.levels.CanRename

trait CleanupConstants extends Flavored with CanRename {

  import flavor._

  def do_transform(
      program: DefProgram
  )(implicit ctx: AssemblyContext): DefProgram = {
    def onProcess(proc: DefProcess): DefProcess = {
      val constClusters = proc.registers
        .filter(reg => reg.variable.varType == ConstType)
        .map(reg => reg.variable.name -> reg.value.get)
        .groupMap { case (name, value) => value } { case (name, value) => name }

      val aliases = constClusters.flatMap { case (value, names) =>
        val representative = names.head
        names.map(name => name -> representative)
      }

      val newBody = proc.body.map { instr =>
        // If the name doesn't exist in the map, return the name unchanged.
        val renamingFunc = (name: Name) => aliases.getOrElse(name, name)
        Rename.asRenamed(instr)(renamingFunc)
      }

      val newRegs = proc.registers.flatMap { reg =>
        if (reg.variable.varType == ConstType) {
          // A non-aliased name has itself as its renaming.
          val isNotAliased = aliases(reg.variable.name) == reg.variable.name
          if (isNotAliased) Some(reg) else None
        } else {
          // Keep original register as it isn't a constant.
          Some(reg)
        }
      }

      proc.copy(
        body = newBody,
        registers = newRegs
      )
    }

    program.copy(
      processes = program.processes.map(proc => onProcess(proc))
    )
  }
}
