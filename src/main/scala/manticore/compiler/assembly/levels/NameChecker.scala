package manticore.compiler.assembly.levels

import manticore.compiler.assembly.ManticoreAssemblyIR

import scala.annotation.tailrec

import manticore.compiler.AssemblyContext

/** A generic name checker that makes sure all the used names in the program are
  * defined. This class should be specialized as an object before being used,
  * see [[manticore.assembly.levels.unconstrained.UnconstrainedNameChecker]] for
  * an example.
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  *
  * @param irFlavor
  *   IR flavor object to specialize the checker with
  */
trait AssemblyNameChecker extends Flavored {


  import flavor._

  def do_check(prog: DefProgram, context: AssemblyContext): Unit = {

    case class DefinedName[TT](name: TT, owner: Declaration)
    @tailrec
    def checkDecls[TT](decls: Seq[DefinedName[TT]]): Unit = decls match {
      case Nil      => ()
      case r :: Nil => ()
      case r :: tail =>
        if (tail.contains(r)) {
          context.logger.error(
            s"${r.name} is declared multiple times, first time at ${r.owner.pos}"
          )

        }
        checkDecls(tail)
    }

    def checkProcess(proc: DefProcess): Unit = {

      val reg_defs: Seq[DefinedName[Name]] = proc.registers.map { r =>
        DefinedName(r.variable.name, r)
      }
      checkDecls(reg_defs)
      val func_defs = proc.functions.map { f => DefinedName(f.name, f) }
      checkDecls(func_defs)

      val reg_names = reg_defs.map { _.name }
      val func_names = func_defs.map { _.name }
      def checkRegs(regs: Seq[Name])(implicit inst: Instruction) =
        regs.foreach { r =>
          if (!reg_names.contains(r)) {
            context.logger.error(
              s"undefined register ${r} at ${inst.serialized}:${inst.pos}"
            )

          }
        }


      def checkInst(inst: Instruction): Unit = {
        implicit val implicit_inst = inst
        inst match {
          case BinaryArithmetic(_, rd, rs1, rs2, _) =>
            checkRegs(Seq(rd, rs1, rs2))
          case CustomInstruction(func, rd, rsx, _) =>
            if (!func_names.contains(func)) {
              context.logger.error(
                s"undefined function ${func} at ${inst.serialized}:${inst.pos}"
              )

            }
            checkRegs(Seq(rd) ++ rsx)
          case LocalLoad(rd, base, _, _) => checkRegs(Seq(rd, base))
          case LocalStore(rs, base, _, p, _) =>
            checkRegs(Seq(rs, base) ++ p.toSeq)
          case GlobalLoad(rd, (hh, h, l), _) =>
            checkRegs(Seq(rd, hh, h, l))
          case GlobalStore(rs, (hh, h, l), p, _) =>
            checkRegs(Seq(rs, hh, h, l) ++ p.toSeq)
          case SetValue(rd, _, _)     => checkRegs(Seq(rd))
          case Expect(ref, got, _, _) => checkRegs(Seq(ref, got))
          case Send(rd, rs, _, _)     => checkRegs(Seq(rs))
          case Predicate(rs, _)       => checkRegs(Seq(rs))
          case Mux(rd, sel, rs1, rs2, _) =>
            checkRegs(Seq(rd, sel, rs1, rs2))
          case Nop => // do nothing
          case PadZero(rd, rs, width, annons) => checkRegs(Seq(rd, rs))
          case AddC(rd, co, rs1, rs2, ci, annons) => checkRegs(Seq(rd, co, rs1, rs2, ci))
          case Mov(rd, rs, _) => checkRegs(Seq(rd, rs))
          case ClearCarry(rd, _) => checkRegs(Seq(rd))
          case SetCarry(rd, _) => checkRegs(Seq(rd))
          case _: Recv =>
            context.logger.error("can not check instruction", inst)
            context.logger.fail("Failed checkInst")

        }
      }

      proc.body.foreach(i => checkInst(i))
    }

    /** Check that every used named in each process is defined and not name is
      * defined twice in the same process scope
      */
    prog.processes.foreach(checkProcess)

    /** Check cross process names, make sure all send messages have valid
      * destinations
      */
    val proc_ids = prog.processes.map { _.id }

    val proc_map = prog.processes.map { p => p.id -> p }.toMap

    checkDecls {
      proc_ids.zip(prog.processes).map { case (n, o) => DefinedName(n, o) }
    }

    prog.processes.foreach { p =>
      p.body.filter { _.isInstanceOf[Send] }.map(_.asInstanceOf[Send]).foreach {
        case inst @ Send(rd, _, dest, _) =>
          if (dest == p.id) {
            context.logger
              .error(s"self-send instruction at ${inst.serialized}:${inst.pos}")

          }
          if (!proc_ids.contains(dest)) {
            context.logger
              .error(
                s"invalid destination id at ${inst.serialized}:${inst.pos}"
              )

          } else {

            // check if dest reg is defined
            if (
              !proc_map(dest).registers
                .map { r => r.variable.name }
                .contains(rd)
            ) {
              context.logger.error(
                s"destination register ${rd} is not defined in process ${dest} in ${inst}:${inst.pos}"
              )

            }
          }
      }
    }


  }
}
