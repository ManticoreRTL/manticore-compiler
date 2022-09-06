package manticore.compiler.assembly.levels

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.annotations.DebugSymbol

/** Template transformation for renaming all variable. You need to override the
  * `flavor` and the `mkName` function to specialize this transformation
  * ATTENTION: This transform assumes that the instructions are ordered And it
  * also assumes that instructions are "almost" in SSA form. Almost means that a
  * register might be assigned multiple times in the same basic block but never
  * across them. The reason for even having such non SSA basic blocks is the
  * current implementation of the [[WidthConversionCore]] transform that breaks
  * SSAness inside basic block but never breaks them across basic blocks. So
  * this pass can be used to path the AST from the [[WidthConversionCore]] pass
  * or in general to completely rename every register.
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
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

      val subst           = scala.collection.mutable.Map.empty[Name, Name]
      val original_defreg = scala.collection.mutable.Map.empty[Name, DefReg]
      def appendNewDef(r: DefReg): DefReg = {
        val new_name = nextName(r)
        original_defreg += r.variable.name -> r
        subst += (r.variable.name          -> new_name)
        val new_def =
          r.copy(variable = r.variable.withName(new_name)).setPos(r.pos)
        new_def
      }

      val regs = proc.registers map appendNewDef

      val new_regs     = scala.collection.mutable.Queue.empty[DefReg]
      val dirty_regs   = scala.collection.mutable.Set.empty[Name]
      val body_builder = scala.collection.mutable.Queue.empty[Instruction]

      trait RdRenamer {
        def apply(rd: Name): Name
      }

      // The following function will try to patch non SSA code in a single
      // basic block by creating fresh names to dirty registers,
      class CreateNewRdDefIfDirty extends RdRenamer {
        val dirty_regs = scala.collection.mutable.Set.empty[Name]
        def apply(rd: Name): Name = {
          if (dirty_regs contains rd) {
            val rd_def      = original_defreg(rd)
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
      }
      val outerRenamer = new CreateNewRdDefIfDirty

      def renameInstruction(
          inst: Instruction
      ): Instruction = {
        val renamed = inst match {
          case i @ BinaryArithmetic(_, rd, rs1, rs2, _) =>
            /** we need to perform copy twice in order to enforce the order in
              * which we call append an entry to [[subst]]
              */
            i.copy(rs1 = subst(rs1), rs2 = subst(rs2))
              .copy(rd = outerRenamer(rd))
          case i @ CustomInstruction(func, rd, rsx, _) =>
            i.copy(
              rsx = rsx.map(rs => subst(rs))
            ).copy(rd = outerRenamer(rd))
          case i @ LocalLoad(rd, base, offset, order, _) =>
            i.copy(
              base = subst(base),
              address = subst(offset),
              order = order.withMemory(subst(order.memory))
            ).copy(rd = outerRenamer(rd))
          case i @ GlobalLoad(rd, base, _, _) =>
            i.copy(
              base = base.map(subst)
            ).copy(rd = outerRenamer(rd))
          case i @ LocalStore(rs, base, address, p, order, _) =>
            i.copy(
              rs = subst(rs),
              base = subst(base),
              address = subst(address),
              order = order.withMemory(subst(order.memory)),
              predicate = p.map { subst }
            )
          case i @ GlobalStore(rs, base, pred, _, _) =>
            i.copy(
              rs = subst(rs),
              base = base.map(subst),
              predicate = pred.map(subst)
            )
          case i @ SetValue(rd, _, _) =>
            i.copy(rd = outerRenamer(rd))
          case i @ Predicate(rs, _) => i.copy(rs = subst(rs))
          case i @ Mux(rd, sel, rs1, rs2, _) =>
            i.copy(
              sel = subst(sel),
              rfalse = subst(rs1),
              rtrue = subst(rs2)
            ).copy(rd = outerRenamer(rd))
          case i @ PadZero(rd, rs, _, _) =>
            i.copy(rs = subst(rs)).copy(rd = outerRenamer(rd))
          case i @ AddCarry(rd, rs1, rs2, ci, _) =>
            i.copy(
              rs1 = subst(rs1),
              rs2 = subst(rs2),
              cin = subst(ci)
            ).copy(rd = outerRenamer(rd))
          case i @ Mov(rd, rs, _) =>
            i.copy(
              rs = subst(rs)
            ).copy(rd = outerRenamer(rd))
          case i @ ClearCarry(rd, _) =>
            i.copy(carry = outerRenamer(rd))
          case i @ SetCarry(rd, _) =>
            i.copy(carry = outerRenamer(rd))
          case i @ Slice(rd, rs, _, _, _) =>
            i.copy(rs = subst(rs))
              .copy(rd = outerRenamer(rd))
          case i @ Expect(ref, got, _, _) =>
            i.copy(ref = subst(ref), got = subst(got))
          case i @ Nop => i
          case i: SynchronizationInstruction =>
            ctx.logger.error("Can not rename instruction", i)
            i
          case i @ ParMux(rd, choices, default, _) =>
            i.copy(
              default = subst(default),
              choices = choices map { case ParMuxCase(a, b) =>
                ParMuxCase(subst(a), subst(b))
              }
            ).copy(rd = outerRenamer(rd))

          case i @ Lookup(rd, index, base, _) =>
            i.copy(index = subst(index), base = subst(base))
              .copy(rd = outerRenamer(rd))

          case i @ JumpTable(rs, results, blocks, dslot, _) =>
            assert(
              results.forall { case Phi(rd, rss) =>
                rss.forall { case (_, rs) => rs != rd }
              },
              "Phis are not in SSA form!"
            )

            i.copy(
              target = subst(rs),
              blocks = blocks.map { case JumpCase(lbl, body) =>
                JumpCase(lbl, body.map(renameInstruction))
              },
              dslot = dslot.map(renameInstruction)
            ).copy(
              results = results.map { case Phi(rd, rss) =>
                Phi(
                  subst(
                    rd
                  ), // no need to call outerRenamer because we assume the
                  // phi is already in SSA form, i.e., non of the operands have the
                  // same name as rd
                  rss.map { case (lbl, rs) => (lbl, subst(rs)) }
                )
              }
            )
          case i: BreakCase => i
          case i @ PutSerial(rs, pred, _, _) =>
            i.copy(rs = subst(rs), pred = subst(pred))
          case i @ Interrupt(action, condition, _, _) =>
            action match {
              case AssertionInterrupt | FinishInterrupt | _: SerialInterrupt | StopInterrupt =>
                i.copy(condition = subst(condition))

            }
        }
        renamed.setPos(inst.pos)
      }

      proc.body.foreach { inst =>
        val new_inst: Instruction = renameInstruction(inst)

        body_builder += new_inst.setPos(inst.pos)
      }

      proc
        .copy(
          registers = regs ++ new_regs.toSeq,
          body = body_builder.toSeq,
          labels = proc.labels.map(lgrp => lgrp.copy(memory = subst(lgrp.memory)))
        )
        .setPos(proc.pos)

    }
  }

  def do_transform(source: DefProgram, ctx: AssemblyContext): DefProgram =
    Impl.rename(source)(ctx)

}
