package manticore.compiler.assembly.levels.placed.interpreter

import manticore.compiler.assembly.levels.placed.PlacedIR._
import manticore.compiler.assembly.levels.TransformationID
import manticore.compiler.AssemblyContext

trait ProgramInterpreter extends InterpreterBase {


  // interpret a single virtual cycle
  def interpretVirtualCycle(): Seq[InterpretationTrap]

  // interpret until completion
  def interpretCompletion(): Boolean

}

