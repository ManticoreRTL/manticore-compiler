package manticore.compiler.assembly.levels

import manticore.compiler.assembly.HasSerialized



/**
  * High-level variable type
  * @author Mahyar Emami <mahyar.emami@epfl.ch>
  */


trait HasVariableType {
  def varType: VariableType
}
sealed trait VariableType {
  def typeName: String
}
case object ConstType extends VariableType {
  override def typeName: String = ".const"
}
case object WireType extends VariableType {
  override def typeName: String = ".wire"
}
case object RegType extends VariableType {
  override def typeName: String = ".reg"
}
case object InputType extends VariableType {
  override def typeName: String = ".input"
}
case object OutputType extends VariableType {
  override def typeName: String = ".output"
}
case object MemoryType extends VariableType {
  override def typeName: String = ".mem"
}
