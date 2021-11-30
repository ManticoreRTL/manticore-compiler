package manticore.assembly.levels.placed
import manticore.assembly.ManticoreAssemblyIR
import manticore.assembly.levels.VariableType
import manticore.assembly.levels.HasVariableType
import manticore.assembly.levels.UInt16

/** IR level with placed processes and allocated registers.
  */
object PlacedIR extends ManticoreAssemblyIR {

  import manticore.assembly.HasSerialized


  sealed abstract class PlacedVariable(val tpe: VariableType)
      extends Named
      with HasSerialized with HasVariableType {
    override def serialized: String = s"${tpe.typeName} ${name} 16"
    override def varType = tpe
  }
  import manticore.assembly.levels.{
    WireType,
    RegType,
    ConstType,
    InputType,
    OutputType,
    MemoryType
  }
  case class WireVariable(name: Name, id: Int)
      extends PlacedVariable(WireType)
  case class RegVariable(name: Name, id: Int)
      extends PlacedVariable(RegType)
  case class InputVariable(name: Name, id: Int)
      extends PlacedVariable(InputType)
  case class ConstVariable(name: Name, id: Int)
      extends PlacedVariable(ConstType)
  case class OutputVariable(name: Name, id: Int)
      extends PlacedVariable(OutputType)
  case class MemoryVariable(name: Name, id: Int, block: Name)
      extends PlacedVariable(MemoryType)




  case class CustomFunctionImpl(values: Seq[UInt16]) extends HasSerialized {
    def serialized: String = s"[${values.map(_.toInt).mkString(", ")}]"
  }

  case class ProcessIdImpl(id: String, x: Int, y: Int) {

    override def toString(): String = id.toString()
  }

  type Name = String
  type Variable = PlacedVariable
  type CustomFunction = CustomFunctionImpl
  type ProcessId = ProcessIdImpl
  type Constant = UInt16
  type ExceptionId = UInt16

}
