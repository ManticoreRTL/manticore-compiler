package manticore.compiler.assembly.levels.unconstrained

import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.assembly.levels.VariableType
import manticore.compiler.assembly.levels.AssemblyNameChecker
import manticore.compiler.assembly.levels.RegType
import manticore.compiler.assembly.levels.WireType
import manticore.compiler.assembly.levels.OutputType
import manticore.compiler.assembly.levels.MemoryType
import manticore.compiler.assembly.levels.InputType
import scala.language.implicitConversions
import manticore.compiler.assembly.levels.ConstType
import manticore.compiler.assembly.HasWidth
import manticore.compiler.assembly.HasSerialized
import manticore.compiler.assembly.levels.HasVariableType

/** Raw assembly, with possible bit slices and wide bit vectors (e.g., 128-bit
  * addition)
  */
object UnconstrainedIR extends ManticoreAssemblyIR {

  // case class ConstVariable(name: String, )
  case class LogicVariable(
      name: String,
      width: Int,
      tpe: VariableType
  ) extends Named[LogicVariable]
      with HasSerialized
      with HasVariableType
      with HasWidth {
    def serialized: String = s"${tpe.typeName} ${name} ${width}"
    def varType = tpe

    def withName(n: Name): LogicVariable = this.copy(name = n)
  }

  type Constant = BigInt // unlimited bits
  type Variable = LogicVariable
  // UnconstrainedIR does not support custom functions. Only lower IRs use such optimizations.
  type CustomFunction = Nothing
  type Name = String
  type ProcessId = String
  type ExceptionId = String

  type Label = String
}
