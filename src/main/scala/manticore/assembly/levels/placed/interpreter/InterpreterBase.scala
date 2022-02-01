package manticore.assembly.levels.placed.interpreter

import manticore.assembly.levels.placed.PlacedIR._
import manticore.compiler.AssemblyContext
import manticore.assembly.levels.UInt16
import manticore.assembly.levels.AssemblyTransformer
import manticore.assembly.levels.AssemblyChecker
import manticore.assembly.levels.TransformationID

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

sealed abstract class InterpretationTrap
case object FailureTrap extends InterpretationTrap
case object StopTrap extends InterpretationTrap
case object InternalTrap extends InterpretationTrap
