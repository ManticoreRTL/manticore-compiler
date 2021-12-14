package manticore.assembly.levels.unconstrained

import manticore.assembly.ManticoreAssemblyIR
import manticore.assembly.levels.VariableType
import manticore.assembly.levels.AssemblyNameChecker
import manticore.assembly.levels.RegType
import manticore.assembly.levels.WireType
import manticore.assembly.levels.OutputType
import manticore.assembly.levels.MemoryType
import manticore.assembly.levels.InputType
import scala.language.implicitConversions
import manticore.assembly.levels.ConstType
import manticore.assembly.HasWidth
import manticore.assembly.HasSerialized
import manticore.assembly.levels.HasVariableType

/** Raw assembly, with possible bit slices and wide bit vectors (e.g., 128-bit
  * addition)
  */
object UnconstrainedIR extends ManticoreAssemblyIR {

  // case class ConstVariable(name: String, )
  case class LogicVariable(
      name: String,
      width: Int,
      tpe: VariableType
  ) extends Named
      with HasSerialized with HasVariableType with HasWidth {
    def serialized: String = s"${tpe.typeName} ${name} ${width}"
    def varType = tpe
  }

  case class CustomFunctionImpl(values: Seq[BigInt]) extends HasSerialized {
    def serialized: String = "[" + values.mkString(", ") + "]"
  }

  implicit def seqBigIntToCustomFuncImpl(
      values: Seq[BigInt]
  ): CustomFunctionImpl = CustomFunctionImpl(values)

  type Constant = BigInt // unlimited bits
  type Variable = LogicVariable
  type CustomFunction = CustomFunctionImpl // unlimited bits
  type Name = String
  type ProcessId = String
  type ExceptionId = Int
}
