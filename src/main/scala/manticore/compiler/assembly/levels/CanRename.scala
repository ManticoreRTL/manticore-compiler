package manticore.compiler.assembly.levels

import manticore.compiler.AssemblyContext

trait CanRename extends Flavored {

  object Rename {
    import flavor._

    def asRenamed(
        inst: Instruction
    )(renaming: Name => Name)(implicit ctx: AssemblyContext): Instruction = {
      val renamed_inst = inst match {
        case i @ SetCarry(carry, _) => i.copy(carry = renaming(carry))
        case i @ Mux(rd, sel, rfalse, rtrue, _) =>
          i.copy(
            rd = renaming(rd),
            sel = renaming(sel),
            rfalse = renaming(rfalse),
            rtrue = renaming(rtrue)
          )
        case i @ AddCarry(rd, rs1, rs2, ci, _) =>
          i.copy(
            rd = renaming(rd),
            rs1 = renaming(rs1),
            rs2 = renaming(rs2),
            cin = renaming(ci)
          )
        case i @ SetValue(rd, value, _) =>
          i.copy(rd = renaming(rd))
        case i @ BinaryArithmetic(operator, rd, rs1, rs2, _) =>
          i.copy(
            rd = renaming(rd),
            rs1 = renaming(rs1),
            rs2 = renaming(rs2)
          )
        case i @ GlobalStore(rs, base, predicate, _, _) =>
          i.copy(
            rs = renaming(rs),
            base = base.map(renaming),
            predicate = predicate.map(renaming)
          )
        case i @ LocalStore(rs, base, offset, predicate, order, _) =>
          i.copy(
            rs = renaming(rs),
            base = renaming(base),
            address = renaming(offset),
            predicate = predicate.map(renaming),
            order = order.withMemory(renaming(order.memory))
          )
        case i @ Send(rd, rs, dest_id, _) =>
          ctx.logger.error("Can not rename instruction", i)
          i
        case i @ CustomInstruction(func, rd, rss, _) =>
          i.copy(rsx = rss.map { renaming })
        case i: ConfigCfu =>
          ctx.logger.error("Can not rename instruction", i)
          i
        case i @ LocalLoad(rd, base, offset, order, _) =>
          i.copy(
            rd = renaming(rd),
            base = renaming(base),
            address = renaming(offset),
            order = order.withMemory(renaming(order.memory))
          )
        case i @ ClearCarry(carry, _) =>
          i.copy(
            carry = renaming(carry)
          )
        case i @ Nop =>
          i
        case i @ Expect(ref, got, error_id, _) =>
          i.copy(
            ref = renaming(ref),
            got = renaming(got)
          )
        case i @ Predicate(rs, _) =>
          i.copy(
            rs = renaming(rs)
          )
        case i @ GlobalLoad(rd, base, _, _) =>
          i.copy(
            rd = renaming(rd),
            base = base.map(renaming)
          )
        case i @ Mov(rd, rs, _) =>
          i.copy(
            rd = renaming(rd),
            rs = renaming(rs)
          )
        case i @ Slice(rd, rs, _, _, _) =>
          i.copy(
            rd = renaming(rd),
            rs = renaming(rs)
          )
        case i @ Recv(rd, rs, source_id, _) =>
          ctx.logger.error("Can not rename instruction", i)
          i
        case i @ PadZero(rd, rs, width, _) =>
          i.copy(
            rd = renaming(rd),
            rs = renaming(rs)
          )
        case i @ ParMux(rd, cases, default, _) =>
          i.copy(
            rd = renaming(rd),
            choices = cases.map { case ParMuxCase(cond, choice) =>
              ParMuxCase(renaming(cond), renaming(choice))
            },
            default = renaming(default)
          )
        case i @ Lookup(rd, index, base, _) =>
          i.copy(
            rd = renaming(rd),
            index = renaming(index),
            base = renaming(base)
          )
        case jtb @ JumpTable(target, phis, blocks, dslot, _) =>
          def renameBlock(block: Seq[Instruction]): Seq[Instruction] = {
            block.map { inst =>
              asRenamed(inst)(renaming)
            }
          }
          jtb.copy(
            target = renaming(target),
            results = phis.map { case Phi(rd, rss) =>
              Phi(
                renaming(rd),
                rss.map { case (lbl, rs) => (lbl, renaming(rs)) }
              )
            },
            dslot = renameBlock(dslot),
            blocks = blocks.map { case JumpCase(lbl, blk) =>
              JumpCase(lbl, renameBlock(blk))
            }
          )
        case brk @ BreakCase(_, _) => brk
        case put @ PutSerial(rs, pred, _, _) =>
          put.copy(renaming(rs), renaming(pred))
        case intr @ Interrupt(action, rs, _, _) =>
          action match {
            // although a pattern match is not needed, I do it in case we add
            // new actions that may have a name in their action. This way
            // make the pattern match throw an error and save ourselves some
            // headache of debugging.
            case AssertionInterrupt | FinishInterrupt | StopInterrupt =>
              intr.copy(condition = renaming(rs))
            case SerialInterrupt(fmt) =>
              intr.copy(condition = renaming(rs))

          }
      }
      renamed_inst.setPos(inst.pos)
    }
  }
}
