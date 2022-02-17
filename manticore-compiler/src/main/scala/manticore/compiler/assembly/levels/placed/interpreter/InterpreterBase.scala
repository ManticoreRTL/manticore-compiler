package manticore.compiler.assembly.levels.placed.interpreter

import manticore.compiler.assembly.levels.placed.PlacedIR._
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.AssemblyChecker
import manticore.compiler.assembly.levels.TransformationID
import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.assembly.levels.ValueChangeWriterBase

trait InterpreterBase {
  implicit val phase_id: TransformationID
  implicit val ctx: AssemblyContext
}

/// base trait for a message between the machine interpreter and a process interpreter
trait MessageBase {
  val source_id: ProcessId
  val target_id: ProcessId
  val value: UInt16
  val target_register: Name
}

trait PlacedValueChangeWriter extends ValueChangeWriterBase {
  val flavor = PlacedIR
  def toBigInt(v: UInt16): BigInt = BigInt(v.toInt)
}

object PlacedValueChangeWriter {

  final class VcdDumpImpl(val program: DefProgram, val file_name: String)(
      implicit val ctx: AssemblyContext
  ) extends PlacedValueChangeWriter {}

  def apply(prog: DefProgram, file_name: String)(implicit
      ctx: AssemblyContext
  ): PlacedValueChangeWriter = new VcdDumpImpl(prog, file_name)

}

sealed abstract class InterpretationTrap
case object FailureTrap extends InterpretationTrap
case object StopTrap extends InterpretationTrap
case object InternalTrap extends InterpretationTrap
