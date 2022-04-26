package manticore.compiler.assembly.levels.placed.interpreter

import manticore.compiler.assembly.levels.placed.PlacedIR._
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.assembly.levels.TransformationID
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.ConstType
import manticore.compiler.assembly.BinaryOperator

trait ProcessInterpreter extends InterpreterBase {

//   type Message <: HasTargetId
  val proc: DefProcess

  type Message <: MessageBase

  def getFunc(name: Name): CustomFunction
  // write a register
  def write(rd: Name, value: UInt16): Unit
  // read a register
  def read(rs: Name): UInt16
  // local load/store
  def lload(address: UInt16): UInt16
  def lstore(address: UInt16, value: UInt16): Unit

  def send(dest: ProcessId, rd: Name, value: UInt16): Unit
  // read from a remote process
  def recv(rs: Name, process_id: ProcessId): Option[UInt16]

  def getPred(): Boolean
  def setPred(v: Boolean): Unit

  def trap(kind: InterpretationTrap): Unit

  def dequeueOutbox(): Seq[Message]

  def enqueueInbox(msg: Message): Unit

  def dequeueTraps(): Seq[InterpretationTrap]

  def step(): Unit

  def roll(): Unit

  def interpret(inst: BinaryArithmetic): Unit = {

    import manticore.compiler.assembly.BinaryOperator._

    val BinaryArithmetic(op, rd, rs1, rs2, _) = inst
    val rs1_val = read(rs1)
    val rs2_val = read(rs2)
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
      case AND => rs1_val & rs2_val
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

    }
    write(rd, rd_val)
  }

  def interpret(instr: CustomInstruction): Unit = {
    import manticore.compiler.assembly.levels.placed.PlacedIR.CustomFunctionImpl._

    val CustomInstruction(name, rd, rsx, _) = instr
    val func = getFunc(name)

    // We must bind the arguments of the custom function to their concrete values
    // at the current cycle.
    val substMap = rsx.zipWithIndex.map { case (rs, idx) =>
      val rsVal = read(rs)
      AtomArg(idx) -> AtomConst(rsVal)
    }.toMap

    val exprWithArgs = substitute(func.expr)(substMap)
    val rdVal = evaluate(exprWithArgs)

    write(rd, rdVal)
  }

  def interpret(instruction: Instruction): Unit = instruction match {
    case i: BinaryArithmetic => interpret(i)
    case i: CustomInstruction => interpret(i)
    case LocalLoad(rd, base, offset, _) =>
      val base_val = read(base)
      val addr = offset + base_val
      val rd_val = lload(addr)
      write(rd, rd_val)
    case LocalStore(rs, base, offset, predicate, annons) =>
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
        val addr = offset + base_val
        val rs_val = read(rs)
        lstore(addr, rs_val)
      }
    case Send(rd, rs, dest_id, _) =>
      val rs_val = read(rs)
      send(dest_id, rd, rs_val)
    case Expect(ref, got, ExceptionIdImpl(id, msg, kind), _) =>
      val ref_val = read(ref)
      val got_val = read(got)
      if (ref_val != got_val) {
        kind match {
          case ExpectFail =>
            ctx.logger.error(s"User exception caught: ${msg}", instruction)
            ctx.logger.error(s"Expected ${ref_val} but got ${got_val}")
            trap(FailureTrap)
          case ExpectStop =>
            ctx.logger.info(s"Stop signal interpreted.", instruction)
            trap(StopTrap)
        }
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
      val sel_val = read(sel)
      val rfalse_val = read(rfalse)
      val rtrue_val = read(rtrue)
      val rd_val = sel_val match {
        case UInt16(0) => rfalse_val
        case UInt16(1) => rtrue_val
        case _ =>
          ctx.logger.error(s"Invalid select value ${sel_val}", instruction)
          rtrue_val
      }
      write(rd, rd_val)
    case AddC(rd, co, rs1, rs2, ci, _) =>
      val rs1_val = read(rs1).toInt
      val rs2_val = read(rs2).toInt
      val ci_val = read(ci).toInt
      val sum = rs1_val + rs2_val + ci_val
      val rd_val = UInt16.clipped(sum)
      val co_val = sum >> 16
      assert(co_val <= 1, "invalid carry computation")
      write(rd, rd_val)
      write(co, UInt16(co_val))
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
      write(carry, UInt16(1))
    case ClearCarry(carry, _) =>
      write(carry, UInt16(0))
    case Recv(rd, rs, source_id, _) =>
      val rd_val = recv(rd, source_id)
      rd_val match {
        case Some(v) => write(rd, v)
        case None    => // do nothing
      }

    case Nop => // nothing

    case _: GlobalStore | _: GlobalLoad | _: SetValue =>
      ctx.logger.error(s"can not handle", instruction)
      trap(InternalTrap)
  }

}
