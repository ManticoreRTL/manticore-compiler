package manticore.assembly.levels.placed.interpreter

import manticore.assembly.levels.placed.PlacedIR._
import manticore.assembly.levels.UInt16

import manticore.compiler.AssemblyContext
import manticore.assembly.levels.TransformationID
import manticore.assembly.levels.ConstType

object AtomicInterpreter {

  case class AtomicMessage(
      source_id: ProcessId,
      target_id: ProcessId,
      target_register: Name,
      value: UInt16
  ) extends MessageBase

  final class AtomicProcessInterpreter(val proc: DefProcess)(implicit
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
      register_file += (rd -> value)
    }

    private val local_memory = {
      // initialize the local memory if needed
      val underlying = Array.fill(ctx.max_local_memory / (16 / 8)) { UInt16(0) }
      proc.registers.foreach {
        case m @ DefReg(
              v @ MemoryVariable(_, _, MemoryBlock(_, _, _, initial_content)),
              offset_opt,
              _
            ) => ///
          val offset = offset_opt match {
            case Some(v) => v
            case None =>
              ctx.logger.error(s"memory not allocated", m)
              UInt16(0)
          }
          initial_content.zipWithIndex.foreach { case (v, ix) =>
            underlying(ix + offset.toInt) = v
          }
        case _ => // do nothing
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
            s"Could not find a message for value of ${rd} from ${process_id}"
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
          ctx.logger.error(
            s"did not expect message, writing to ${msg.target_id} from process ${msg.source_id}"
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

  final class AtomicProgramInterpreter(val program: DefProgram)(implicit
      val ctx: AssemblyContext
  ) extends ProgramInterpreter {

    override implicit val phase_id: TransformationID = TransformationID(
      "atomic_interpreter"
    )

    val cores = program.processes.map { p =>
      p.id -> new AtomicProcessInterpreter(p)(ctx)
    }.toMap

    val vcycle_length = program.processes.map { _.body.length }.max
    var cycle: Int = 0

    override def interpretVirtualCycle(): Seq[InterpretationTrap] = {

      val break = scala.collection.mutable.Queue.empty[InterpretationTrap]

      while (break.isEmpty && cycle < vcycle_length) {
        // step through each core
        cores.foreach { case (_, core) => core.step() }
        cores.foreach { case (cid, c) =>
          val traps = c.dequeueTraps()
          break ++= traps
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
      break.toSeq
    }

    override def interpretCompletion(): Boolean = {

      var vcycle = 0

      val traps = scala.collection.mutable.Queue.empty[InterpretationTrap]

      while (vcycle < ctx.max_cycles && traps.isEmpty) {
        interpretVirtualCycle()
        vcycle += 1
      }

      if (vcycle >= ctx.max_cycles) {
        ctx.logger.error(
          s"Interpretation timed out after ${vcycle} virtual cycles!"
        )
        false
      } else { // if (traps.nonEmpty) {
        traps.forall {
          case FailureTrap | InternalTrap => false
          case StopTrap                   => true
        }
      }
    }

  }
}
