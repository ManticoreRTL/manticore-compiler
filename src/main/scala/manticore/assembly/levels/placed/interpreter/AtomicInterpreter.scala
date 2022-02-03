package manticore.assembly.levels.placed.interpreter

import manticore.assembly.levels.placed.PlacedIR._
import manticore.assembly.levels.UInt16

import manticore.compiler.AssemblyContext
import manticore.assembly.levels.TransformationID
import manticore.assembly.levels.ConstType
import manticore.assembly.levels.AssemblyChecker
import manticore.assembly.levels.placed.interpreter.PlacedValueChangeWriter

object AtomicInterpreter extends AssemblyChecker[DefProgram] {

  case class AtomicMessage(
      source_id: ProcessId,
      target_id: ProcessId,
      target_register: Name,
      value: UInt16
  ) extends MessageBase

  final class AtomicProcessInterpreter(
      val proc: DefProcess,
      val vcd: Option[PlacedValueChangeWriter]
  )(implicit
      val ctx: AssemblyContext
  ) extends ProcessInterpreter {

    implicit val phase_id: TransformationID = TransformationID(
      s"interp.${proc.id}"
    )

    type Message = AtomicMessage

    private val register_file =
      scala.collection.mutable.Map.empty[Name, UInt16] ++ proc.registers.map {
        r =>
          if (r.variable.varType == ConstType && r.value.isEmpty) {
            ctx.logger.error(s"constant value is not defined", r)
          }
          r.variable.name -> r.value.getOrElse(UInt16(0))
      }
    override def write(rd: Name, value: UInt16): Unit = {
      register_file(rd) = value
      vcd.foreach(_.update(rd, value))
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
            m.variable.asInstanceOf[MemoryVariable].block.capacityInShorts()
          }.sum

          val mem_with_offset = memories.foldLeft(0, Seq.empty[DefReg]) {
            case ((base, with_offset), m) =>
              val memvar = m.variable.asInstanceOf[MemoryVariable]
              val new_base = base + memvar.block.capacityInShorts()
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
              v @ MemoryVariable(_, _, MemoryBlock(_, _, _, initial_content)),
              offset_opt,
              _
            ) => ///
          val offset = offset_opt match {
            case Some(v) => v
            case None    =>
              // should not happen
              ctx.logger.error(s"memory not allocated", m)
              UInt16(0)
          }
          initial_content.zipWithIndex.foreach { case (v, ix) =>
            underlying(ix + offset.toInt) = v
          }
        case _ => // do nothing, does not happen
      }
      underlying
    }

    private val outbox = scala.collection.mutable.Queue.empty[AtomicMessage]

    private val inbox = scala.collection.mutable.Queue.empty[AtomicMessage]

    private val missing_messages =
      scala.collection.mutable.Queue.empty[(Name, ProcessId)]

    private var pred: Boolean = false

    private val caught_traps =
      scala.collection.mutable.Queue.empty[InterpretationTrap]

    private var cycle = 0
    private val instructions =
      proc.body.toArray // convert to array for faster indexing...
    private val vcycle_length = instructions.length
    override def read(rs: Name): UInt16 = register_file(rs)

    override def lload(address: UInt16): UInt16 = local_memory(address.toInt)

    override def lstore(address: UInt16, value: UInt16): Unit = {
      local_memory(address.toInt) = value
    }

    override def send(dest: ProcessId, rd: Name, value: UInt16): Unit = {
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
          ctx.logger.warn(
            s"Could not find a message for value of ${rd} from ${process_id}. Will have to check for message in roll over."
          )
          missing_messages += ((rd, process_id))
          None
      }
      res
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
      if (cycle < vcycle_length) {
        interpret(instructions(cycle))
        cycle += 1
      }
    }

    override def roll(): Unit = {
      cycle = 0
      while (inbox.nonEmpty) {
        val msg = inbox.dequeue()
        val entry = (msg.target_register, msg.source_id)
        if (missing_messages.contains(entry)) {
          // great, we were expecting this message
          write(msg.target_register, msg.value)
          missing_messages -= entry
        } else {
          // something is up
          ctx.logger.warn(
            s"did not expect message, writing to " +
              s"${msg.target_id}:${msg.target_register} value of ${msg.value} from process ${msg.source_id}"
          )
          trap(InternalTrap)

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
      val program: DefProgram,
      val vcd: Option[PlacedValueChangeWriter],
      val expected_cycles: Option[Int]
  )(implicit
      val ctx: AssemblyContext
  ) extends ProgramInterpreter {

    override implicit val phase_id: TransformationID = TransformationID(
      "atomic_interpreter"
    )

    val cores = program.processes.map { p =>
      p.id -> new AtomicProcessInterpreter(p, vcd)(ctx)
    }.toMap

    val vcycle_length = program.processes.map { _.body.length }.max

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
      cores.foreach { _._2.roll() }
      traps.toSeq
    }

    override def interpretCompletion(): Boolean = {

      var vcycle = 0

      val traps = scala.collection.mutable.Queue.empty[InterpretationTrap]
      var break = false
      while (vcycle < ctx.max_cycles && traps.isEmpty) {
        traps ++= interpretVirtualCycle()
        vcd.foreach(_.tick())
        vcycle += 1
      }

      val cycles_match = expected_cycles match {
        case None    => true
        case Some(v) => v == vcycle
      }

      if (!cycles_match) {
        ctx.logger.error(
          s"Interpretation finished after ${vcycle} virtual cycles, " +
            s"but expected it to finish in ${expected_cycles.get} virtual cycles!"
        )
        false
      } else if (vcycle >= ctx.max_cycles) {
        ctx.logger.error(
          s"Interpretation timed out after ${vcycle} virtual cycles!"
        )
        false
      } else { // if (traps.nonEmpty) {
        val no_error = traps.forall {
          case FailureTrap | InternalTrap => false
          case StopTrap                   => true
        }
        ctx.logger.info(
          s"Interpretation finished after ${vcycle} virtual cycles " +
            s"${if (!no_error) "with error(s)" else "without errors"}"
        )
        no_error
      }
    }

  }

  def mkInterpreter(
      prog: DefProgram,
      vcd: Option[PlacedValueChangeWriter] = None,
      expected_cycles: Option[Int] = None
  )(implicit ctx: AssemblyContext): AtomicProgramInterpreter =
    new AtomicProgramInterpreter(prog, vcd, expected_cycles)

  override def check(source: DefProgram, context: AssemblyContext): Unit = {

    val vcd = context.dump_dir.map(_ =>
      PlacedValueChangeWriter(source, "atomic_trace.vcd")(context)
    )
    val interp = mkInterpreter(source, vcd)(context)

    interp.interpretCompletion()

    vcd.foreach(_.flush())
    vcd.foreach(_.close())
  }
}
