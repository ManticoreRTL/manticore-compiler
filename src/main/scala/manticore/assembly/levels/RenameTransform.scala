package manticore.assembly.levels

import manticore.compiler.AssemblyContext
import manticore.assembly.annotations.DebugSymbol

/** Template transformation for renaming all variable. You need to override the
  * `flavor` and the `mkName` function to specialize this transformation
  * ATTENTION: This transform assumes that the instructions are ordered
  *
  * @author Mahyar Emami <mahyar.emami@epfl.ch>
  */
trait RenameTransformation extends Flavored {

  import flavor._
  def mkName(id: Long, original: Name, tpe: VariableType): Name

  private object Impl {

    // private var unique_id: Long = 0

    private def nextName(orig: DefReg)(implicit ctx: AssemblyContext): Name = {
      val unique_id = ctx.uniqueNumber()
      val new_name: Name =
        mkName(unique_id, orig.variable.name, orig.variable.varType)
      new_name
    }

    def rename(
        prog: DefProgram
    )(implicit ctx: AssemblyContext): DefProgram = {
      prog.copy(processes = prog.processes.map(rename)).setPos(prog.pos)
    }

    private def rename(
        proc: DefProcess
    )(implicit ctx: AssemblyContext): DefProcess = {

      val subst = scala.collection.mutable.Map.empty[Name, Name]
      val original_defreg = scala.collection.mutable.Map.empty[Name, DefReg]
      def appendNewDef(r: DefReg): DefReg = {
        val new_name = nextName(r)
        original_defreg += r.variable.name -> r
        subst += (r.variable.name -> new_name)
        val new_def =
          r.copy(variable = r.variable.withName(new_name)).setPos(r.pos)
        new_def
      }

      val regs = proc.registers map appendNewDef

      val new_regs = scala.collection.mutable.Queue.empty[DefReg]
      val dirty_regs = scala.collection.mutable.Set.empty[Name]
      val body_builder = scala.collection.mutable.Queue.empty[Instruction]

      def createNewRdDef(rd: Name): Name = {
        if (dirty_regs contains rd) {
          val rd_def = original_defreg(rd)
          val rd_new_name = nextName(rd_def)
          subst += (rd_def.variable.name -> rd_new_name)
          val new_rd_def = rd_def
            .copy(rd_def.variable.withName(rd_new_name))
            .setPos(rd_def.pos)
          new_regs += new_rd_def
          rd_new_name
        } else {
          dirty_regs += rd
          subst(rd)
        }
      }
      proc.body.foreach { inst =>
        val new_inst: Instruction = inst match {
          case i @ BinaryArithmetic(_, rd, rs1, rs2, _) =>
            /** we need to perform copy twice in order to enforce the order in
              * which we call append an entry to [[subst]]
              */
            i.copy(rs1 = subst(rs1), rs2 = subst(rs2))
              .copy(rd = createNewRdDef(rd))
          case i @ CustomInstruction(func, rd, rs1, rs2, rs3, rs4, _) =>
            i.copy(
              rs1 = subst(rs1),
              rs2 = subst(rs2),
              rs3 = subst(rs3),
              rs4 = subst(rs4)
            ).copy(rd = createNewRdDef(rd))
          case i @ LocalLoad(rd, base, _, _) =>
            i.copy(base = subst(base)).copy(rd = createNewRdDef(rd))
          case i @ LocalStore(rs, base, _, p, _) =>
            i.copy(
              rs = subst(rs),
              base = subst(base),
              predicate = p.map { subst }
            )
          case i @ GlobalLoad(rd, (hh, h, l), _) =>
            i.copy(base = (subst(hh), subst(h), subst(l)))
              .copy(rd = createNewRdDef(rd))
          case i @ GlobalStore(rs, (hh, h, l), p, _) =>
            i.copy(
              rs = subst(rs),
              base = (subst(hh), subst(h), subst(l)),
              predicate = p map { subst }
            )
          case i @ SetValue(rd, _, _) =>
            i.copy(rd = createNewRdDef(rd))
          case i @ Expect(ref, got, _, _) =>
            i.copy(ref = subst(ref), got = subst(got))
          case i @ Send(rd, rs, _, _) =>
            ctx.logger.error("Can not rename Send instruction!", i)
            i.copy(rd = subst(rd), rs = subst(rs))
          case i @ Predicate(rs, _) => i.copy(rs = subst(rs))
          case i @ Mux(rd, sel, rs1, rs2, _) =>
            i.copy(
              sel = subst(sel),
              rfalse = subst(rs1),
              rtrue = subst(rs2)
            ).copy(rd = createNewRdDef(rd))
          case i @ Nop => i
          case i @ PadZero(rd, rs, _, _) =>
            i.copy(rs = subst(rs)).copy(rd = createNewRdDef(rd))
          case i @ AddC(rd, co, rs1, rs2, ci, _) =>
            i.copy(
              rs1 = subst(rs1),
              rs2 = subst(rs2),
              ci = subst(ci)
            ).copy(rd = createNewRdDef(rd), co = createNewRdDef(co))
          case i @ Mov(rd, rs, _) =>
            i.copy(
              rs = subst(rs)
            ).copy(rd = createNewRdDef(rd))
          case i @ ClearCarry(rd, _) =>
            i.copy(carry = createNewRdDef(rd))
          case i @ SetCarry(rd, _) =>
            i.copy(carry = createNewRdDef(rd))
          case _: Recv =>
            ctx.logger.error("can not rename RECV", inst)
            ctx.logger.fail("Failed renaming")
        }

        body_builder += new_inst.setPos(inst.pos)
      }

      proc
        .copy(registers = regs ++ new_regs.toSeq, body = body_builder.toSeq)
        .setPos(proc.pos)

    }
  }

  def do_transform(source: DefProgram, ctx: AssemblyContext): DefProgram =
    Impl.rename(source)(ctx)

}
