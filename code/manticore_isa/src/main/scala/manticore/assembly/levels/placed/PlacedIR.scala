package manticore.assembly.levels.placed

import manticore.assembly.ManticoreAssemblyIR
import manticore.assembly.levels.LogicType

import manticore.assembly.levels.UInt16

object PlacedIR extends ManticoreAssemblyIR {

  import manticore.assembly.HasSerialized
  case class LogicVariable(
      name: String,
      id: Int,
      tpe: LogicType
  ) extends Named
      with HasSerialized {
    import manticore.assembly.levels.{
      RegLogic,
      WireLogic,
      OutputLogic,
      InputLogic,
      MemoryLogic,
      ConstLogic
    }
    def serialized: String = (tpe match {
      case RegLogic    => ".reg"
      case WireLogic   => ".wire"
      case OutputLogic => ".output"
      case MemoryLogic => ".mem"
      case InputLogic  => ".input"
      case ConstLogic  => ".const"
    }) + s" ${name} 16"
  }

  case class CustomFunctionImpl(values: Seq[UInt16]) extends HasSerialized {
    def serialized: String = s"[${values.map(_.toInt).mkString(", ")}]"
  }

  case class ProcesssIdImpl(id: String, x: Int, y: Int)  {
    
    override def toString(): String = id.toString()
  }

  type Name = String
  type Variable = LogicVariable
  type CustomFunction = CustomFunctionImpl
  type ProcessId = ProcesssIdImpl
  type Constant = UInt16
  type ExceptionId = UInt16

}
