package manticore.assembly.levels.unconstrained

import manticore.assembly.ManticoreAssemblyIR
import manticore.assembly.levels.LogicType
import manticore.assembly.levels.AssemblyNameChecker
import manticore.assembly.levels.RegLogic
import manticore.assembly.levels.WireLogic
import manticore.assembly.levels.OutputLogic
import manticore.assembly.levels.MemoryLogic
import manticore.assembly.levels.InputLogic
import scala.language.implicitConversions
import manticore.assembly.levels.ConstLogic
import manticore.assembly.HasSerialized
/** Raw assembly, with possible bit slices and wide bit vectors (e.g., 128-bit
  * addition)
  */
object UnconstrainedIR extends ManticoreAssemblyIR {
  case class LogicVariable(
      name: String,
      width: Int,
      tpe: LogicType
  ) extends Named with HasSerialized {
    def serialized: String = (tpe match {
      case RegLogic    => ".reg"
      case WireLogic   => ".wire"
      case OutputLogic => ".output"
      case MemoryLogic => ".mem"
      case InputLogic  => ".input"
      case ConstLogic  => ".const"
    }) + s" ${name} ${width}"
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

object UnconstrainedNameChecker extends AssemblyNameChecker(UnconstrainedIR)
