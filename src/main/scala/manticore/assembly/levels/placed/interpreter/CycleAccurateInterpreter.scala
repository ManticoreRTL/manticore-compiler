package manticore.assembly.levels.placed.interpreter

import manticore.assembly.levels.AssemblyChecker

import manticore.assembly.levels.placed.PlacedIR._
import manticore.assembly.levels.UInt16
import manticore.compiler.AssemblyContext
import manticore.assembly.levels.TransformationID
import com.sourcegraph.semanticdb_javac.Semanticdb.ConstantType
import manticore.assembly.levels.ConstType
import manticore.assembly.levels.placed.LatencyAnalysis

/** A fully cycle accurate machine interpreter (almost a simulator) that only
  * works on fully placed, routed, and register allocated programs. This
  * interpreter can be used to simulate programs before running them on the
  * actual Manticore machine.
  *
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */
object CycleAccurateInterpreter extends AssemblyChecker[DefProgram] {

  override def check(source: DefProgram, context: AssemblyContext): Unit = ???

  case class NoCMessage(
      source_id: ProcessId,
      target_id: ProcessId,
      target_register: Name,
      value: UInt16,
      departure: Int
  ) extends MessageBase

  object NoCMessageOrdering extends Ordering[NoCMessage] {
    override def compare(x: NoCMessage, y: NoCMessage): Int =
      Ordering[Int].reverse.compare(x.departure, y.departure)
  }

  final class ProcessInterpreterImpl(
      val proc: DefProcess,
      val vcd: Option[PlacedValueChangeWriter]
  )(implicit val ctx: AssemblyContext)
      extends ProcessInterpreter {

    type Message = NoCMessage

    private val register_file = Array.fill(ctx.max_registers) { UInt16(0) }

    private val name_to_ids = proc.registers.map { r: DefReg =>
      val reg_id = r.variable.id
      if (reg_id < 0 || reg_id >= ctx.max_registers) {
        ctx.logger.error(s"register not properly allocated!", r)
        r.variable.name -> 0
      } else {
        if (r.variable.varType == ConstType && r.value.isEmpty) {
          ctx.logger.warn(s"constant is not defined!", r)
        }
        r.value match {
          case Some(x) => register_file(reg_id) = x
          case None    => // nothing to do
        }
        r.variable.name -> reg_id
      }
    }.toMap

    private val local_memory = {
      // we assume the memory is already correctly allocated and we only
      // initialize it.
      val underlying = Array.fill(ctx.max_local_memory) { UInt16(0) }
      proc.registers.foreach {
        case m @ DefReg(v: MemoryVariable, opt_offset, _) =>
          opt_offset match {
            case Some(UInt16(offset)) =>
              v.block.initial_content.zipWithIndex.foreach {
                case (value, index) =>
                  underlying(index + offset) = value
              }
            case _ => // oops
              ctx.logger.error(s"memory not allocated!", m)
              trap(InternalTrap)
          }
        case _ => // do nothing
      }
      underlying
    }

    var outbox = Option.empty[Message]
    var inbox = scala.collection.mutable.Queue.empty[Message]
    var global_time: Int = 0

    override def write(rd: Name, value: UInt16): Unit = {
      //look up the index
      val index = name_to_ids(rd)
      register_file(index) = value
      vcd match {
        case Some(handle) => handle.update(rd, value)
        case None         => // do nothing
      }

    }

    override def read(rs: Name): UInt16 = {
      val index = name_to_ids(rs)
      register_file(index)
    }

    override def lload(address: UInt16): UInt16 = local_memory(address.toInt)

    override def lstore(address: UInt16, value: UInt16): Unit = {
      local_memory(address.toInt) = value
    }

    override def send(dest: ProcessId, rd: Name, value: UInt16): Unit = {
      if (outbox.isDefined) {
        ctx.logger.error(
          s"message was not picked up!"
        ) // kind of an internal error
        trap(InternalTrap)
      } else {
        outbox = Some(
          NoCMessage(
            proc.id,
            dest,
            rd,
            value,
            global_time + LatencyAnalysis.maxLatency() + 1
          )
        )
      }
    }

    override def recv(rs: Name, process_id: ProcessId): Option[UInt16] = {
      if (inbox.nonEmpty) {
        val NoCMessage(source_id, _, reg_name, value, _) = inbox.head
        if (source_id != process_id) {
          ctx.logger.error(
            s"expected message from ${process_id} in ${proc.id} but " +
              s"the message was from ${source_id}"
          )
        }
        if (reg_name != rs) {
          ctx.logger.error(
            s"expected value for register ${rs} but the received value" +
              s" is for ${reg_name} in process ${proc.id}"
          )
        }
        Some(value)
      } else {
        ctx.logger.error(
          s"could not find received value of register ${rs} from " +
            s"${process_id} in ${proc.id}"
        )
        trap(InternalTrap)
        None
      }
    }

    var predicate: Boolean = false
    override def getPred(): Boolean = predicate

    override def setPred(v: Boolean): Unit = { predicate = v }

    val trap_queue = scala.collection.mutable.Queue.empty[InterpretationTrap]
    override def trap(kind: InterpretationTrap): Unit = { trap_queue += kind }

    override def dequeueOutbox(): Seq[Message] = {
      val out = outbox.toSeq
      outbox = None
      out
    }

    override def enqueueInbox(msg: Message): Unit = inbox.enqueue(msg)

    override def dequeueTraps(): Seq[InterpretationTrap] =
      trap_queue.dequeueAll(_ => true)

    override def step(): Unit = ???

    override def roll(): Unit = ???

    implicit val phase_id: TransformationID = TransformationID(
      s"interpreter.${proc.id}"
    )

  }

  final class NoCModel(implicit val ctx: AssemblyContext) {

    val x_links = Array.ofDim[Option[NoCMessage]](ctx.max_dimx, ctx.max_dimy)
    val y_links = Array.ofDim[Option[NoCMessage]](ctx.max_dimx, ctx.max_dimy)

    Range(0, ctx.max_dimx).foreach { x =>
      Range(0, ctx.max_dimy).foreach { y =>
        x_links(x)(y) = None
        y_links(x)(y) = None
      }
    }


  }
  final class ProgramInterpreterImpl(
      val program: DefProgram,
      val vcd: Option[PlacedValueChangeWriter],
      val expected_cycles: Option[Int]
  )(implicit val ctx: AssemblyContext)
      extends ProgramInterpreter {

    implicit val phase_id: TransformationID = TransformationID(
      "cycle_accurate_interpreter"
    )

    val cores = program.processes.map { p =>
      (p.id.x, p.id.y) -> new ProcessInterpreterImpl(p, vcd)
    }.toMap
    val vcycle_length = program.processes.map { _.body.length }.max

    override def interpretVirtualCycle(): Seq[InterpretationTrap] = ???

    override def interpretCompletion(): Boolean = ???
  }

}
