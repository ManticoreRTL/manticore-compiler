package manticore.compiler.assembly.levels.placed.interpreter

import manticore.compiler.assembly.levels.placed.PlacedIR._
import manticore.compiler.assembly.levels.UInt16

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.TransformationID
import manticore.compiler.assembly.levels.ConstType
import manticore.compiler.assembly.levels.placed.PlacedIRChecker
import manticore.compiler.assembly.levels.placed.interpreter.PlacedValueChangeWriter
import manticore.compiler.assembly.levels.CarryType
import manticore.compiler.assembly.levels.InputType
import manticore.compiler.assembly.levels.MemoryType
import manticore.compiler.assembly.levels.placed.PlacedIR.CustomFunctionImpl._
import manticore.compiler.assembly.levels.placed.TaggedInstruction
import manticore.compiler.assembly.levels.placed.TaggedInstruction.PhiSource

/** Basic interpreter for placed programs. The program needs to have Send and
  * Recv instructions but does not check for NoC contention and does not require
  * allocated registers.
  *
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */
object AtomicInterpreter extends PlacedIRChecker {

  case class AtomicMessage(
      source_id: ProcessId,
      target_id: ProcessId,
      target_register: Name,
      value: UInt16
  ) extends MessageBase

  trait RegisterFile {
    def write(name: Name, value: UInt16): Unit
    def read(name: Name): UInt16
  }

  final class AtomicProcessInterpreter(
      proc: DefProcess,
      vcd: Option[PlacedValueChangeWriter],
      monitor: Option[PlacedIRInterpreterMonitor],
      serial: Option[String => Unit],
      pedanticRecv: Boolean
  )(implicit
      val ctx: AssemblyContext
  ) extends ProcessInterpreter {

    override implicit val transformId: TransformationID = TransformationID(
      s"interp.${proc.id}"
    )

    type Message = AtomicMessage

    private val register_file = {

      def badAlloc(r: DefReg): Unit =
        ctx.logger.error("Bad register allocation!", r)
      def undefinedConstant(r: DefReg): Unit =
        ctx.logger.error("Constant not defined!", r)
      def badInit(r: DefReg): Unit =
        ctx.logger.warn("Did not expect initial value!", r)
      // check whether the registers are allocated
      if (proc.registers.exists(r => r.variable.id == -1)) {
        ctx.logger.info("using an unbounded register file")
        new NamedRegisterFile(proc, vcd, monitor)(undefinedConstant)
      } else {
        ctx.logger.info("using a bounded register file")
        new PhysicalRegisterFile(
          proc,
          vcd,
          monitor,
          ctx.max_registers,
          ctx.max_carries
        )(
          undefinedConstant,
          badAlloc,
          badInit
        )
      }

    }

    override def getFunc(name: Name): CustomFunction = {
      proc.functions.find(func => func.name == name) match {
        case None =>
          ctx.logger.error(s"Custom function ${name} does not exist!")
          // So the calling loop does not continue further after this cycle.
          trap(InternalTrap)

          // We return a custom function that takes no inputs and returns 0.
          CustomFunctionImpl(IdExpr(AtomConst(UInt16(0))))

        case Some(func) =>
          func.value
      }
    }

    override def write(rd: Name, value: UInt16): Unit = {
      register_file.write(rd, value)
    }

    private val local_memory = {

      def allocateIfNeeded(regs: Seq[DefReg]): (Int, Seq[DefReg]) = {

        val memories = proc.registers.collect {
          case m @ DefReg(v: MemoryVariable, _, _) => m
        }
        val max_avail = ctx.max_local_memory / (16 / 8)
        val needs_alloc = memories.exists(_.value.isEmpty)
        if (needs_alloc) {
          ctx.logger.warn(
            "Memories are not allocated, assuming unbounded local memory"
          )

          val required = memories.map { m =>
            m.variable.asInstanceOf[MemoryVariable].size
          }.sum

          val mem_with_offset = memories.foldLeft(0, Seq.empty[DefReg]) {
            case ((base, with_offset), m) =>
              val memvar = m.variable.asInstanceOf[MemoryVariable]
              val new_base = base + memvar.size
              (new_base, with_offset :+ m.copy(value = Some(UInt16(base))))
          }
          if (required > max_avail) {
            ctx.logger.warn(
              s"Program uses more memory (${required} shorts) than available (${max_avail} shorts)"
            )

          }
          (max_avail.max(required), mem_with_offset._2)
        } else {
          (max_avail, memories)
        }
      }
      val (mem_size, memories) = allocateIfNeeded(proc.registers)
      val underlying = Array.fill(mem_size) { UInt16(0) }
      memories.foreach {
        case m @ DefReg(
              mvar @ MemoryVariable(_, _, _, initialContent),
              offset_opt,
              _
            ) => ///
          val offset = offset_opt match {
            case Some(v) =>
              // make sure the offset value is set in the register file
              register_file.write(mvar.name, v)
              v
            case None =>
              // should not happen
              ctx.logger.error(s"memory not allocated", m)
              UInt16(0)
          }
          initialContent.zipWithIndex.foreach { case (v, ix) =>
            underlying(ix + offset.toInt) = v
          }
        case _ => // do nothing, does not happen
      }
      underlying
    }

    private val outbox = scala.collection.mutable.Queue.empty[AtomicMessage]

    private val inbox = scala.collection.mutable.Queue.empty[AtomicMessage]

    private val serialQueue = scala.collection.mutable.Queue.empty[UInt16]

    private val missing_messages =
      scala.collection.mutable.Queue.empty[(Name, ProcessId)]

    private var pred: Boolean = false

    private val caught_traps =
      scala.collection.mutable.Queue.empty[InterpretationTrap]

    val instructionMemory = TaggedInstruction.indexedTaggedBlock(proc)
    private var pc: Int = 0
    private var jumpPc: Option[Int] = None

    override def updatePc(v: Int): Unit = {
      jumpPc = Some(v)
    }
    // private val instructions =
    //   proc.body.toArray // convert to array for faster indexing...

    private val maxPc = instructionMemory.length
    override def read(rs: Name): UInt16 = register_file.read(rs)

    override def lload(address: UInt16): UInt16 = local_memory(address.toInt)

    override def lstore(address: UInt16, value: UInt16): Unit = {
      local_memory(address.toInt) = value
    }

    override def send(dest: ProcessId, rd: Name, value: UInt16): Unit = {
      ctx.logger.debug(s"Sending ${rd} <- ${value} from ${proc.id} to ${dest}")
      outbox.enqueue(
        AtomicMessage(
          source_id = proc.id,
          target_id = dest,
          target_register = rd,
          value = value
        )
      )
    }

    override def recv(rd: Name, process_id: ProcessId): Option[UInt16] = {

      val msg_opt = inbox.find { msg =>
        msg.source_id == process_id && msg.target_register == rd
      }

      val res = msg_opt match {
        case Some(msg @ AtomicMessage(_, _, _, value)) =>
          inbox -= msg
          Some(value)
        case _ =>
          ctx.logger.debug(
            s"Could not find a message for value of ${rd} from ${process_id} in ${proc.id}. Will have to check for message in roll over."
          )
          missing_messages += ((rd, process_id))
          None
      }
      res
    }

    override def enqueueSerial(v: UInt16): Unit = {
      serialQueue += v
    }
    override def flushSerial(): Seq[UInt16] = serialQueue.dequeueAll(_ => true)
    override def printSerial(line: String): Unit = serial match {
      case None          => ctx.logger.info(s"[SERIAL] ${line}")
      case Some(printer) => printer(line)
    }
    override def getPred(): Boolean = pred

    override def setPred(v: Boolean): Unit = { pred = v }

    override def trap(kind: InterpretationTrap): Unit = {
      caught_traps += kind
    }

    override def dequeueOutbox(): Seq[AtomicMessage] =
      outbox.dequeueAll(_ => true)

    override def enqueueInbox(msg: AtomicMessage): Unit = inbox += msg

    override def dequeueTraps(): Seq[InterpretationTrap] =
      caught_traps.dequeueAll(_ => true)

    override def step(): Unit = {

      def handlePhi(tInst: TaggedInstruction): Unit = {
        if (register_file.isInstanceOf[NamedRegisterFile]) {
          tInst.tags.foreach {
            case PhiSource(rd, rs) =>
              write(rd, read(rs)) // propagate
            case _ => // do nothing
          }
        } else {
          // do nothing, the register allocation should implicitly handle Phis
        }
      }
      if (pc < maxPc) {
        val taggedInst = instructionMemory(pc)
        jumpPc match {
          case Some(target) => // there is no pending jump
            val isBorrowedExecution = taggedInst.tags.exists { t =>
              t == TaggedInstruction.BorrowedExecution
            }
            if (isBorrowedExecution) {
              interpret(taggedInst.instruction)
              handlePhi(taggedInst)
            } else {
              // done with the borrowed execution need to actually jump!
              if (target < pc) {
                ctx.logger.error(
                  s"Can not jump to ${target} when pc is ${pc}. Only forward jumping is semantically correct."
                )
              }
              pc = target
              val jumpTargetInst = instructionMemory(pc)
              val isJumpTarget = taggedInst.tags.exists { t =>
                t.isInstanceOf[
                  TaggedInstruction.JumpTarget
                ] || t == TaggedInstruction.BreakTarget
              }
              assert(isJumpTarget, "Expected a tagged jumped target!")
              jumpPc = None // don't do it after interpret because that may
              // change jumpPc to a new value (i.e., if we have a jump after another jump)
              interpret(jumpTargetInst.instruction)
              handlePhi(jumpTargetInst)
            }
          case None =>
            interpret(taggedInst.instruction)
            handlePhi(taggedInst)
        }
        pc += 1
      }
    }

    override def roll(): Unit = {
      pc = 0
      jumpPc = None
      while (inbox.nonEmpty) {
        val msg = inbox.dequeue()
        val entry = (msg.target_register, msg.source_id)
        if (missing_messages.contains(entry)) {
          // great, we were expecting this message
          write(msg.target_register, msg.value)
          missing_messages -= entry
        } else if (pedanticRecv) {
          // something is up
          ctx.logger.error(
            s"Did not expect message, writing to " +
              s"${msg.target_id}:${msg.target_register} value of ${msg.value} from process ${msg.source_id} (perhaps missing RECV)"
          )
          trap(InternalTrap)
        } else {
          write(msg.target_register, msg.value)
        }
      }
      if (missing_messages.nonEmpty) {
        while (missing_messages.nonEmpty) {
          val expected = missing_messages.dequeue()
          ctx.logger.error(
            s"expected to receive value of ${expected._1} from ${expected._2} but never received it!"
          )
          trap(InternalTrap)
        }
      }

    }
  }

  final class AtomicProgramInterpreter(
      program: DefProgram,
      vcd: Option[PlacedValueChangeWriter],
      monitor: Option[PlacedIRInterpreterMonitor],
      expected_cycles: Option[Int],
      serial: Option[String => Unit]
  )(implicit
      ctx: AssemblyContext
  ) extends ProgramInterpreter {

    override implicit val transformId: TransformationID = TransformationID(
      "atomic_interpreter"
    )

    val recvPedantic =
      program.processes.exists(_.body.exists(_.isInstanceOf[Recv]))
    val cores = program.processes.map { p =>
      p.id -> new AtomicProcessInterpreter(
        p,
        vcd,
        monitor,
        serial,
        recvPedantic
      )(ctx)
    }.toMap
    if (!recvPedantic && program.processes.length != 1) {
      ctx.logger.warn(
        "Interpreter will mask RECV failures because no process contains a RECV message. Please consider interpreting after scheduling."
      )
    }
    val vcycle_length = cores.map { _._2.instructionMemory.length }.max

    override def interpretVirtualCycle(): Seq[InterpretationTrap] = {

      val traps = scala.collection.mutable.Queue.empty[InterpretationTrap]
      var break = false
      var cycle: Int = 0
      while (!break && cycle < vcycle_length) {
        // step through each core
        cores.foreach { case (_, core) => core.step() }
        cores.foreach { case (cid, c) =>
          traps ++= c.dequeueTraps()
          if (traps.nonEmpty) {
            break = true
          }
          val outbound = c.dequeueOutbox()
          // deliver messages
          outbound.foreach { msg =>
            cores(msg.target_id).enqueueInbox(msg)
          }
        }

        cycle += 1
      }
      // virtual cycle is done, or some error has occurred
      // roll every core back to their first instruction and consume any outstanding
      // messages
      if (traps.isEmpty) {
        cores.foreach { _._2.roll() }
      }
      traps.toSeq
    }

    override def interpretCompletion(): Boolean = {

      var vcycle = 0

      val traps = scala.collection.mutable.Queue.empty[InterpretationTrap]
      var break = false
      while (vcycle < ctx.max_cycles && traps.isEmpty) {
        traps ++= interpretVirtualCycle()
        vcd.foreach(_.tick())
        ctx.logger.debug(s"Finished vcycle ${vcycle}")
        vcycle += 1
      }

      val cycles_match = expected_cycles match {
        case None    => true
        case Some(v) => v == vcycle - 1
      }

      if (!cycles_match) {
        ctx.logger.error(
          s"Interpretation finished after ${vcycle - 1} virtual cycles, " +
            s"but expected it to finish in ${expected_cycles.get} virtual cycles!"
        )
        false
      } else if (vcycle >= ctx.max_cycles) {
        ctx.logger.error(
          s"Interpretation timed out after ${vcycle - 1} virtual cycles!"
        )
        false
      } else { // if (traps.nonEmpty) {
        val no_error = traps.forall {
          case FailureTrap | InternalTrap => false
          case FinishTrap                 => true
        }
        ctx.logger.info(
          s"Interpretation finished after ${vcycle - 1} virtual cycles " +
            s"${if (!no_error) "with error(s)" else "without errors"}"
        )
        no_error
      }
    }

  }

  def instance(
      program: DefProgram,
      vcd: Option[PlacedValueChangeWriter] = None,
      monitor: Option[PlacedIRInterpreterMonitor] = None,
      expectedCycles: Option[Int] = None,
      serial: Option[String => Unit] = None
  )(implicit ctx: AssemblyContext): AtomicProgramInterpreter =
    new AtomicProgramInterpreter(program, vcd, monitor, expectedCycles, serial)

  override def check(
      source: DefProgram
  )(implicit context: AssemblyContext): Unit = {

    val vcd = context.dump_dir.map(_ =>
      PlacedValueChangeWriter(source, "atomic_trace.vcd")(context)
    )
    val interp = instance(source, vcd, None, context.expected_cycles)(context)

    interp.interpretCompletion()

    vcd.foreach(_.flush())
    vcd.foreach(_.close())
  }
}
