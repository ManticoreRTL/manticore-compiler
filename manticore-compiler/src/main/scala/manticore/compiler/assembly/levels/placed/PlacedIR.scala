package manticore.compiler.assembly.levels.placed
import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.assembly.levels.VariableType
import manticore.compiler.assembly.levels.HasVariableType
import manticore.compiler.assembly.HasWidth
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.assembly.DependenceGraphBuilder
import manticore.compiler.assembly.annotations.{Memblock => MemblockAnnotation}
import manticore.compiler.assembly.levels.AssemblyPrinter
/** IR level with placed processes and allocated registers.
  *
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */
object PlacedIR extends ManticoreAssemblyIR {

  import manticore.compiler.assembly.HasSerialized

  sealed abstract trait PlacedVariable
      extends Named[PlacedVariable]
      with HasSerialized
      with HasVariableType
      with HasWidth {
    override def serialized: String = s"${varType.typeName} ${name} -> ${id} 16"
    override def width = 16
    val id: Int

    def withId(new_id: Int): PlacedVariable

  }
  import manticore.compiler.assembly.levels.{
    WireType,
    RegType,
    ConstType,
    InputType,
    OutputType,
    MemoryType
  }

  case class ValueVariable(name: Name, id: Int, tpe: VariableType)
      extends PlacedVariable {
    override def varType: VariableType = tpe
    def withName(new_name: Name): PlacedVariable = this.copy(name = new_name)
    def withId(new_id: Int): PlacedVariable = this.copy(id = new_id)
  }

  case class MemoryVariable(name: Name, id: Int, block: MemoryBlock)
      extends PlacedVariable {
    def withName(n: Name) = this.copy(name = n)
    override def varType: VariableType = MemoryType
    def withId(new_id: Int) = this.copy(id = new_id)
  }

  case class MemoryBlock(
      block_id: Name,
      capacity: Int,
      width: Int,
      initial_content: Seq[UInt16] = Seq.empty[UInt16]
  ) {
    def capacityInShorts(): Int = {
      capacity * numShortsPerWord()
    }
    def numShortsPerWord(): Int =
      ((width - 1) / 16 + 1)
  }
  object MemoryBlock {
    def fromAnnotation(a: MemblockAnnotation) =
      MemoryBlock(a.getBlock(), a.getCapacity(), a.getWidth())
  }

  case class CustomFunctionImpl(values: Seq[UInt16]) extends HasSerialized {
    def serialized: String = s"[${values.map(_.toInt).mkString(", ")}]"
  }

  case class ProcessIdImpl(id: String, x: Int, y: Int) {

    override def toString(): String = id.toString()
  }

  sealed trait ExceptionKind
  case object ExpectFail extends ExceptionKind
  case object ExpectStop extends ExceptionKind


  case class ExceptionIdImpl(id: UInt16, msg: String, kind: ExceptionKind) {
    override def toString(): String = s"${msg}_${id}"
  }

  type Name = String
  type Variable = PlacedVariable
  type CustomFunction = CustomFunctionImpl
  type ProcessId = ProcessIdImpl
  type Constant = UInt16
  type ExceptionId = ExceptionIdImpl

}

object LatencyAnalysis {

  import PlacedIR._
  def latency(inst: Instruction): Int = inst match {
    case _: Predicate    => 0
    case _: Expect       => 0
    case Nop             => 0
    case _               => maxLatency()
  }

  def xHops(source: ProcessId, target: ProcessId, dim: (Int, Int)) =
    if (source.x > target.x) dim._2 - source.x + target.x
    else target.x - source.x
  def yHops(source: ProcessId, target: ProcessId, dim: (Int, Int)) =
    if (source.y > target.y) dim._2 - source.y + target.y
    else target.y - source.y

  def xyHops(source: ProcessId, target: ProcessId, dim:(Int, Int)) = {
    (xHops(source, target, dim), yHops(source, target, dim))
  }
  def maxLatency(): Int = 3
  def manhattan(
      source: ProcessId,
      target: ProcessId,
      dim: (Int, Int)
  ) = {
    val x_dist = xHops(source, target, dim)
    val y_dist = yHops(source, target, dim)
    val manhattan = x_dist + y_dist
    manhattan
  }
}


object PlacedIRPrinter extends AssemblyPrinter[PlacedIR.DefProgram] {}