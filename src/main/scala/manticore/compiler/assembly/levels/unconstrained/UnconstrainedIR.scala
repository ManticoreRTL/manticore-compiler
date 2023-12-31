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
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.AssemblyChecker
import manticore.compiler.assembly.CanBuildDependenceGraph
import manticore.compiler.assembly.levels.CanRename
import manticore.compiler.assembly.CanComputeNameDependence
import manticore.compiler.assembly.levels.CanCollectInputOutputPairs
import manticore.compiler.assembly.levels.CanCollectProgramStatistics
import manticore.compiler.assembly.HasInterruptAction
import manticore.compiler.assembly.InterruptAction

/** Raw assembly, with possible bit slices and wide bit vectors (e.g., 128-bit
  * addition)
  */
object UnconstrainedIR extends ManticoreAssemblyIR {

  trait UnconstrainedIRVariable
      extends Named[UnconstrainedIRVariable]
      with HasSerialized
      with HasVariableType
      with HasWidth
  // case class ConstVariable(name: String, )
  case class LogicVariable(
      name: String,
      width: Int,
      tpe: VariableType
  ) extends UnconstrainedIRVariable {
    require(tpe != MemoryType)
    def serialized: String = s"${tpe.typeName} ${name} ${width}"
    def varType            = tpe

    def withName(n: Name): LogicVariable = this.copy(name = n)
  }

  case class MemoryVariable(
      name: String,
      width: Int,
      size: Int,
      content: Seq[BigInt] = Seq()
  ) extends UnconstrainedIRVariable {

    override def withName(new_name: Name): UnconstrainedIRVariable = copy(name = new_name)

    override def serialized: String = s"${varType.typeName} ${name} ${width} ${size}"

    override def varType: VariableType = MemoryType

  }

  type Constant = BigInt // unlimited bits
  type Variable = UnconstrainedIRVariable
  // UnconstrainedIR does not support custom functions. Only lower IRs use such optimizations.
  type CustomFunction = Nothing
  type Name           = String
  type ProcessId      = String

  case class InterruptDescription(action: InterruptAction) extends HasInterruptAction

  type Label = String
}

trait UnconstrainedIRTransformer extends AssemblyTransformer[UnconstrainedIR.DefProgram] {}
trait UnconstrainedIRChecker     extends AssemblyChecker[UnconstrainedIR.DefProgram]     {}

object Helpers
    extends CanBuildDependenceGraph
    with CanRename
    with CanComputeNameDependence
    with CanCollectInputOutputPairs
    with CanCollectProgramStatistics {
  val flavor = UnconstrainedIR
}
