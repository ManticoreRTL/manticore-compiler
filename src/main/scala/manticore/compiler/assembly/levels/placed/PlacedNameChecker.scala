package manticore.compiler.assembly.levels.placed
import manticore.compiler.assembly.levels.AssemblyChecker
import manticore.compiler.assembly.levels.AssemblyNameChecker
import manticore.compiler.AssemblyContext

object PlacedNameChecker
    extends AssemblyNameChecker
    with AssemblyChecker[PlacedIR.DefProgram] {
  val flavor = PlacedIR
  import flavor._
  override def check(
      program: DefProgram,
      context: AssemblyContext
  ): Unit = {

    program.processes.foreach { case process =>
      NameCheck.checkUniqueDefReg(process) { case (sec, fst) =>
        context.logger.error(
          s"name ${sec.variable.name} is defined multiple times, first in ${fst.serialized}",
          sec
        )
      }
      NameCheck.checkNames(process) { case (name, inst) =>
        context.logger.error(s"name ${name} is not defined!", inst)
      }
      NameCheck.checkLabels(process) { case (label, inst) =>
        context.logger.error(s"label ${label} is not defined!", inst)
      }
      NameCheck.checkSSA(process) { case NameCheck.NonSSA(rd, assigns) =>
        context.logger.error(
          s"${rd} is assigned ${assigns.length} times:\n${assigns.mkString("\n")}"
        )
      }

    }
    NameCheck.checkSends(program)(
      badDest = inst => context.logger.error("invalid destination", inst),
      selfDest = inst => context.logger.error("self SEND is not allowed", inst),
      badRegister =
        inst => context.logger.error("Bad destination register", inst)
    )

  }
}
