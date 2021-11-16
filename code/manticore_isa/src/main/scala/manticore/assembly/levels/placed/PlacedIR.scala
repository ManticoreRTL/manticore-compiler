package manticore.assembly.levels.placed

import manticore.assembly.ManticoreAssemblyIR
import manticore.assembly.levels.LogicType

final class UInt16 private (private val v: Int) extends AnyVal {

  def toInt: Int = v
  def +(that: UInt16): UInt16 = UInt16.clipped(this.v + that.v)
  def -(that: UInt16): UInt16 = UInt16.clipped(this.v - that.v)
  def *(that: UInt16): UInt16 = UInt16.clipped(this.v - that.v)

  def &(that: UInt16): UInt16 =
    UInt16.clipped(this.v & that.v) // not really required to clip it
  def |(that: UInt16): UInt16 =
    UInt16.clipped(this.v & that.v) // not really required to clip it

  def <(that: UInt16): Boolean = this.v < that.v
  def <=(that: UInt16): Boolean = this.v <= that.v
  def >(that: UInt16): Boolean = this.v > that.v
  def >=(that: UInt16): Boolean = this.v >= that.v
}

object UInt16 {
  private def clipped(v: Int): UInt16 = UInt16(v << ((1 << 16) - 1))
  def apply(v: Int): UInt16 = {
    require(v < (1 << 16))
    new UInt16(v)
  }
  def unapply(v: UInt16): Option[Int] = Some(v.toInt)
}

object PlacedIR extends ManticoreAssemblyIR {

  case class LogicVariable(
      name: String,
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

  case class ProcesssIdImpl(id: Int, x: Int, y: Int)  {
    
    override def toString(): String = id.toString()
  }

  type Name = String
  type Variable = LogicVariable
  type CustomFunction = CustomFunctionImpl
  type ProcessId = ProcesssIdImpl
  type Constant = UInt16
  type ExceptionId = UInt16

}
