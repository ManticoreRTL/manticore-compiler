package manticore.compiler.assembly.levels

import manticore.compiler.assembly.annotations.DebugSymbol
import manticore.compiler.AssemblyContext

trait CanRenameToDebugSymbols extends CanRename {

  import flavor._

  def debugSymToName(dbg: DebugSymbol): Name
  def constantName(v: Constant, width: Int): Name
  def makeHumanReadable(
      process: DefProcess
  )(implicit ctx: AssemblyContext): DefProcess = {

    val humanNames = process.registers.map {
      case r if (r.variable.varType == ConstType && r.value.nonEmpty) =>
        r.variable.name -> constantName(r.value.get, r.variable.width)
      case r =>
        r.annons.collectFirst { case x: DebugSymbol =>
          x
        } match {
          case None      => r.variable.name -> r.variable.name
          case Some(dbg) => r.variable.name -> debugSymToName(dbg)
        }
    }.toMap

    ctx.logger.dumpArtifact("rename_map.txt") {
      humanNames.map { kv => s"${kv._1} -> ${kv._2}" }.mkString("\n")
    }
    val renamedBody = process.body.map { Rename.asRenamed(_) { humanNames } }
    val renamedRegs = process.registers.map { r =>
      r.copy(variable = r.variable.withName(humanNames(r.variable.name)))
        .setPos(r.pos)
    }
    val renamedLabels = process.labels.map { grp =>
      grp.copy(memory = humanNames(grp.memory))
    }
    val readableProcess =
      process.copy(
        body = renamedBody,
        registers = renamedRegs,
        labels = renamedLabels
      )
    readableProcess
  }

  def makeHumanReadable(
      program: DefProgram
  )(implicit ctx: AssemblyContext): DefProgram = {
    program.copy(
      processes = program.processes.map(makeHumanReadable)
    )
  }

}
