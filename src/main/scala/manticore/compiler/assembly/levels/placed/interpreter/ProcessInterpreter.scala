package manticore.compiler.assembly.levels.placed.interpreter

import manticore.compiler.assembly.levels.placed.PlacedIR._
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.assembly.levels.TransformationID
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.ConstType
import manticore.compiler.assembly.BinaryOperator
import manticore.compiler.FormatString
import manticore.compiler.FormatString.FmtBin
import manticore.compiler.FormatString.FmtDec
import manticore.compiler.FormatString.FmtHex
import manticore.compiler.FormatString.FmtConcat
import manticore.compiler.assembly.{FinishInterrupt, StopInterrupt, SerialInterrupt, AssertionInterrupt}
trait ProcessInterpreter extends InterpreterBase {

//   type Message <: HasTargetId
  // val proc: DefProcess
  implicit val ctx: AssemblyContext

  type Message <: MessageBase

  def getFunc(name: Name): CustomFunction
  // write a register
  def write(rd: Name, value: UInt16): Unit
  def writeOvf(rd: Name, v: Boolean): Unit
  // read a register
  def read(rs: Name): UInt16
  def readOvf(rs: Name): Boolean
  def updatePc(pc: Int): Unit

  // local load/store
  def lload(address: UInt16): UInt16
  def lstore(address: UInt16, value: UInt16): Unit
  // global load/store
  def gload(address: Int): UInt16
  def gstore(address: Int, value: UInt16): Unit

  def send(dest: ProcessId, rd: Name, value: UInt16): Unit
  // read from a remote process
  def recv(rs: Name, process_id: ProcessId): Option[UInt16]

  def getPred(): Boolean
  def setPred(v: Boolean): Unit

  def trap(kind: InterpretationTrap): Unit

  def dequeueOutbox(): Seq[Message]

  def enqueueInbox(msg: Message): Unit

  def dequeueTraps(): Seq[InterpretationTrap]

  def enqueueSerial(value: UInt16): Unit

  def flushSerial(): Seq[UInt16]

  def printSerial(line: String): Unit

  def step(): Unit

  def roll(): Unit

  def interpret(inst: BinaryArithmetic): Unit = {

    import manticore.compiler.assembly.BinaryOperator._

    val BinaryArithmetic(op, rd, rs1, rs2, _) = inst
    val rs1_val                               = read(rs1)
    val rs2_val                               = read(rs2)
    def shiftAmount(v: UInt16): Int = {
      if (v.toInt >= 16) {
        // note that this is only a warning because the result of shift
        // might not actually be used in computing the outputs, but we end
        // up computing the shift result anyways because there are no branches
        // in the program, only predicates and MUXes
        ctx.logger.debug(s"invalid shift amount ${v}", inst)
        v.toInt & 0xf // take the lowest 4 bits

      } else {
        v.toInt
      }
    }
    val rd_val: UInt16 = op match {
      case ADD => rs1_val + rs2_val

      case SUB => rs1_val - rs2_val
      case OR  => rs1_val | rs2_val
      case AND =>
        rs1_val & rs2_val
      case XOR => rs1_val ^ rs2_val
      case SEQ =>
        val is_eq = rs1_val == rs2_val
        UInt16(if (is_eq) 1 else 0)
      case SLL =>
        val shift_amount = shiftAmount(rs2_val)
        rs1_val << shift_amount
      case SRL =>
        val shift_amount = shiftAmount(rs2_val)
        rs1_val >> shift_amount

      case SRA =>
        val shift_amount = shiftAmount(rs2_val)
        rs1_val >>> shift_amount
      case SLT =>
        if (rs1_val < rs2_val) {
          UInt16(1)
        } else {
          UInt16(0)
        }
      case SLTS =>
        val rs1_sign = (rs1_val >> 15) == UInt16(1)
        val rs2_sign = (rs2_val >> 15) == UInt16(1)

        if (rs1_sign && !rs2_sign) {
          // rs1 is negative and rs2 is positive
          UInt16(1)
        } else if (!rs1_sign && rs2_sign) {
          // rs1 is positive and rs2 is negative
          UInt16(0)
        } else if (!rs1_sign && !rs2_sign) {
          // both are positive
          if (rs1_val < rs2_val) {
            UInt16(1)
          } else {
            UInt16(0)
          }
        } else {
          // both are negative
          val rs1_val_pos =
            (~rs1_val) + UInt16(1) // 2's complement positive number
          val rs2_val_pos =
            (~rs2_val) + UInt16(1) // 2's complement positive number
          if (rs1_val_pos > rs2_val_pos) {
            UInt16(1)
          } else {
            UInt16(0)
          }
        }
      case MUL | MULS =>
        if (op == MULS)
          ctx.logger.warn(
            s"Avoid using MULS in PlacedIR! MULS has the same behavior of MUL on lower 16 bits!",
            inst
          )
        rs1_val * rs2_val
      case MULH =>
        val rd_val = rs1_val.toInt * rs2_val.toInt
        UInt16(rd_val >> 16)

    }
    write(rd, rd_val)
  }

  def interpret(instr: CustomInstruction): Unit = {
    import manticore.compiler.assembly.levels.placed.PlacedIR.CustomFunctionImpl._

    val CustomInstruction(name, rd, rsx, _) = instr
    val func                                = getFunc(name)

    // We must bind the arguments of the custom function to their concrete values
    // at the current cycle.
    val substMap = rsx.zipWithIndex
      .map { case (rs, idx) =>
        val rsVal = read(rs)
        PositionalArg(idx) -> AtomConst(rsVal)
      }
      .toMap[AtomArg, Atom]

    val exprWithArgs = substitute(func.expr)(substMap)
    val rdVal        = evaluate(exprWithArgs)

    write(rd, rdVal)
  }

  def interpret(instruction: Instruction): Unit = instruction match {
    case i: BinaryArithmetic  => interpret(i)
    case i: CustomInstruction => interpret(i)
    case LocalLoad(rd, base, index, _, _) =>
      val base_val = read(base)
      val addr_val = read(index) + base_val
      val rd_val   = lload(addr_val)
      write(rd, rd_val)
    case LocalStore(rs, base, offset, predicate, _, annons) =>
      val wen = predicate match {
        case Some(p) =>
          val pred_val = read(p)
          if (pred_val.toInt > 1) {
            ctx.logger.error(
              s"invalid predicate value ${pred_val}!",
              instruction
            )
            trap(InternalTrap)
            true
          } else {
            UInt16(1) == pred_val
          }
        case None =>
          getPred()

      }
      if (wen) {
        val base_val = read(base)
        val addr     = read(offset) + base_val
        val rs_val   = read(rs)
        lstore(addr, rs_val)
      }
    case GlobalLoad(rd, base, _, _) =>
      assert(base.length == 3, s"ill-formed base of GLD ${instruction}")
      val addr =
        read(base(0)).toLong | (read(base(1)).toLong << 16L) | (read(base(2)).toLong << 32L)
      if (!addr.isValidInt) {
        ctx.logger.error(s"address 0x${addr}048x is too large for the interpreter!")
        write(rd, UInt16(0))
        trap(InternalTrap)
      } else {
        val rdVal = gload(addr.toInt)
        write(rd, rdVal)
      }
    case GlobalStore(rs, base, predicate, _, _) =>
      assert(base.length == 3, s"ill-formed base of GST ${instruction}")
      val wen = predicate match {
        case None => getPred()
        case Some(pred) =>
          val pval = read(pred)
          if (pval.toInt > 1) {
            ctx.logger.error(s"invalid predicate value ${pval}!", instruction)
            trap(InternalTrap)
            true
          } else {
            pval == UInt16(1)
          }
      }

      if (wen) {
        val addr =
          read(base(0)).toLong | (read(base(1)).toLong << 16L) | (read(base(2)).toLong << 32L)
        if (!addr.isValidInt) {
          ctx.logger.error(s"address 0x${addr}048x is too large for the interpreter!")
          trap(InternalTrap)
        } else {
          gstore(addr.toInt, read(rs))
        }
      }

    case Send(rd, rs, dest_id, _) =>
      val rs_val = read(rs)
      send(dest_id, rd, rs_val)
    case PutSerial(rs, pred, _, _) =>
      val en = read(pred)
      if (en == UInt16(1)) {
        enqueueSerial(read(rs))
      }
    case intr @ Interrupt(description, condition, _, _) =>
      val en = read(condition) == UInt16(1)

      description.action match {
        case AssertionInterrupt if !en =>
          ctx.logger.error("Assertion failed!", intr)
          trap(FailureTrap)
        case FinishInterrupt if en =>
          ctx.logger.info("Got finish!", intr)
          trap(FinishTrap)
        case StopInterrupt if en =>
          ctx.logger.error("Got stop!", intr)
          trap(FailureTrap)
        case SerialInterrupt(fmt) if en =>
          // see whether we should use the abstract queue or the global memory
          val desc = description.asInstanceOf[SerialInterruptDescription]
          val values = if (desc.pointers.nonEmpty) {
            desc.pointers.map(adr => BigInt(gload(adr).toInt))
          } else {
            flushSerial().map(x => BigInt(x.toInt))
          }
          val resolvedFmt = fmt.consume(values)
          if (resolvedFmt.isLit == false) {
            ctx.logger.error("Did not have enough value to in the serial queue!", intr)
          }
          printSerial(resolvedFmt.toString)

        case _ => // nothing to do
      }
    case Predicate(rs, _) =>
      val rs_val = read(rs)
      if (rs_val == UInt16(1)) {
        setPred(true)
      } else if (rs_val == UInt16(0)) {
        setPred(false)
      } else {
        ctx.logger.error(s"Invalid predicate value", instruction)
        trap(InternalTrap)
        setPred(false)
      }
    case Mux(rd, sel, rfalse, rtrue, _) =>
      val sel_val    = read(sel)
      val rfalse_val = read(rfalse)
      val rtrue_val  = read(rtrue)
      val rd_val = sel_val match {
        case UInt16(0) => rfalse_val
        case UInt16(1) => rtrue_val
        case _ =>
          ctx.logger.error(s"Invalid select value ${sel_val}", instruction)
          trap(InternalTrap)
          rtrue_val
      }
      write(rd, rd_val)
    case AddCarry(rd, rs1, rs2, ci, _) =>
      val rs1_val = read(rs1).toInt
      val rs2_val = read(rs2).toInt
      val ci_val  = if (readOvf(ci)) 1 else 0

      val sum     = rs1_val + rs2_val + ci_val
      val rd_val  = UInt16.clipped(sum)
      val co_val  = sum >> 16
      assert(co_val <= 1, "invalid carry computation")
      write(rd, rd_val)
      writeOvf(rd, co_val == 1)

    case PadZero(rd, rs, width, annons) =>
      ctx.logger.warn(
        s"Did not expect instruction in Placed flavor",
        instruction
      )
      val rs_val = read(rs)
      write(rd, rs_val)
    case Mov(rd, rs, _) =>
      val rs_val = read(rs)
      write(rd, rs_val)
    case SetCarry(carry, _) =>
      writeOvf(carry, true)
    case ClearCarry(carry, _) =>
      writeOvf(carry, false)
    case Recv(rd, rs, source_id, _) =>
      val rd_val = recv(rd, source_id)
      rd_val match {
        case Some(v) => write(rd, v)
        case None    => // do nothing
      }

    case ParMux(rd, choices, default, annons) =>
      val valid_choices = choices.collect {
        case ParMuxCase(cond, rs) if read(cond) == UInt16(1) => cond -> read(rs)
      }
      if (valid_choices.length > 1) {
        ctx.logger.error(
          s"Multiple valid choices ${valid_choices.map(_._1).mkString(" and ")}",
          instruction
        )
      } else if (valid_choices.length == 1) {
        val rd_val = valid_choices.head._2
        write(rd, rd_val)
      } else {
        val rd_val = read(default)
        write(rd, rd_val)
      }

    case Slice(rd, rs, offset, length, _) =>
      val rs_val     = read(rs)
      val rs_shifted = rs_val >> offset
      val mask       = UInt16((1 << length) - 1)
      val rd_val     = rs_shifted & mask
      write(rd, rd_val)

    case Lookup(rd, index, base, _) =>
      val address = read(index) + read(base)
      val rd_val  = lload(address)
      write(rd, rd_val)
    case JumpTable(target, _, _, _, _) =>
      updatePc(read(target).toInt)
    case BreakCase(target, _) => updatePc(target)
    case Nop                  => // nothing

    case _: GlobalStore | _: GlobalLoad | _: SetValue | _: ConfigCfu =>
      ctx.logger.error(s"can not handle", instruction)
      trap(InternalTrap)
  }

}
