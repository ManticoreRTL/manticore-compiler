package manticore.assembly.levels.placed.interpreter

import manticore.assembly.levels.AssemblyChecker

import manticore.assembly.levels.placed.PlacedIR._
import manticore.assembly.levels.UInt16
import manticore.compiler.AssemblyContext
import manticore.assembly.levels.TransformationID

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
      departure: Int,
      arrival: Int
  ) extends MessageBase

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
        r.variable.name -> r.variable.id
      }
    }.toMap


    override def write(rd: Name, value: UInt16): Unit = ???

    override def read(rs: Name): UInt16 = ???

    override def lload(address: UInt16): UInt16 = ???

    override def lstore(address: UInt16, value: UInt16): Unit = ???

    override def send(dest: ProcessId, rd: Name, value: UInt16): Unit = ???

    override def recv(rs: Name, process_id: ProcessId): Option[UInt16] = ???

    override def getPred(): Boolean = ???

    override def setPred(v: Boolean): Unit = ???

    override def trap(kind: InterpretationTrap): Unit = ???

    override def dequeueOutbox(): Seq[Message] = ???

    override def enqueueInbox(msg: Message): Unit = ???

    override def dequeueTraps(): Seq[InterpretationTrap] = ???

    override def step(): Unit = ???

    override def roll(): Unit = ???

    implicit val phase_id: TransformationID = TransformationID(
      s"interpreter.${proc.id}"
    )

  }

}
