package manticore.assembly.levels.placed.interpreter

import manticore.assembly.levels.placed.PlacedIR._
import manticore.assembly.levels.TransformationID
import manticore.compiler.AssemblyContext

trait ProgramInterpreter extends InterpreterBase {

  val program: DefProgram

  // interpret a single virtual cycle
  def interpretVirtualCycle(): Seq[InterpretationTrap]

  // interpret until completion
  def interpretCompletion(): Boolean

}

