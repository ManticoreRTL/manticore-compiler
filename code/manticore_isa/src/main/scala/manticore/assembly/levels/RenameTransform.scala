package manticore.assembly.levels

import manticore.compiler.AssemblyContext
import manticore.assembly.annotations.DebugSymbol


/**
  * Template transformation for renaming all variable.
  * You need to override the `flavor` and the `mkName` function
  * to specialize this transformation
  */
trait RenameTransformation extends Flavored {

  import flavor._
  def mkName(id: Long, original: Name): Name

  private object Impl {

    private var unique_id: Long = 0

    private def nextName(orig: DefReg): Name = {
      val new_name: Name =
        mkName(unique_id, orig.variable.name).asInstanceOf[Name]
      unique_id += 1
      new_name
    }

    def rename(
        prog: DefProgram
    )(implicit ctx: AssemblyContext): DefProgram = {
      unique_id = 0
      prog.copy(processes = prog.processes.map(rename) ).setPos(prog.pos)
    }
    private def rename(inst: Instruction, subst: scala.collection.mutable.HashMap[Name, Name])(implicit
        ctx: AssemblyContext
    ) = (inst match {
      case i @ BinaryArithmetic(_, rd, rs1, rs2, _) =>
        i.copy(rd = subst(rd), rs1 = subst(rs1), rs2 = subst(rs2))
      case i @ CustomInstruction(func, rd, rs1, rs2, rs3, rs4, _) =>
        i.copy(
          rd = subst(rd),
          rs1 = subst(rs1),
          rs2 = subst(rs2),
          rs3 = subst(rs3),
          rs4 = subst(rs4)
        )
      case i @ LocalLoad(rd, base, _, _) =>
        i.copy(rd = subst(rd), base = subst(base))
      case i @ LocalStore(rs, base, _, p, _) =>
        i.copy(
          rs = subst(rs),
          base = subst(base),
          predicate = p.map { subst }
        )
      case i @ GlobalLoad(rd, (hh, h, l), _) =>
        i.copy(rd = subst(rd), base = (subst(hh), subst(h), subst(l)))
      case i @ GlobalStore(rs, (hh, h, l), p, _) =>
        i.copy(
          rs = subst(rs),
          base = (subst(hh), subst(h), subst(l)),
          predicate = p map { subst }
        )
      case i @ SetValue(rd, _, _) =>
        i.copy(rd = subst(rd))
      case i @ Expect(ref, got, _, _) =>
        i.copy(ref = subst(ref), got = subst(got))
      case i @ Send(rd, rs, _, _) => i.copy(rd = subst(rd), rs = subst(rs))
      case i @ Predicate(rs, _)   => i.copy(rs = subst(rs))
      case i @ Mux(rd, sel, rs1, rs2, _) =>
        i.copy(
          rd = subst(rd),
          sel = subst(sel),
          rfalse = subst(rs1),
          rtrue = subst(rs2)
        )
      case i @ Nop => i
      case i @ PadZero(rd, rs, _, _) =>
        i.copy(rd = subst(rd), rs = subst(rs))
      case i @ AddC(rd, co, rs1, rs2, ci, _) =>
        i.copy(
          rd = subst(rd),
          co = subst(co),
          rs1 = subst(rs1),
          rs2 = subst(rs2),
          ci = subst(ci)
        )
    }).setPos(inst.pos)
    private def rename(
        proc: DefProcess
    )(implicit ctx: AssemblyContext): DefProcess = {

      val subst = scala.collection.mutable.HashMap.empty[Name, Name]

      val regs = proc.registers.map { r =>
        val new_name = nextName(r)
        subst += (r.variable.name -> new_name)
        r.copy(variable = r.variable.withName(new_name)).setPos(r.pos)
      }
      val renamed_body = proc.body.map { inst => rename(inst, subst)}

      proc.copy(registers = regs, body = renamed_body).setPos(proc.pos)

    }
  }

  def do_transform(source: DefProgram, ctx: AssemblyContext): DefProgram =
      Impl.rename(source)(ctx)


}
