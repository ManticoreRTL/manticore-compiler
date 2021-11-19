package manticore.assembly.levels

import manticore.assembly.HasSerialized

/**
  * High-level variable type
  * @author Mahyar Emami <mahyar.emami@epfl.ch>
  */

trait VariableType {
  def typeName: String
}
object ConstType extends VariableType {
  override def typeName: String = ".const"
}
object WireType extends VariableType {
  override def typeName: String = ".wire"
}
object RegType extends VariableType {
  override def typeName: String = ".reg"
}
object InputType extends VariableType {
  override def typeName: String = ".input"
}
object OutputType extends VariableType {
  override def typeName: String = ".output"
}
object MemoryType extends VariableType {
  override def typeName: String = ".mem"
}
// object WireLogic extends LogicType
// object RegLogic extends LogicType
// object InputLogic extends LogicType
// object OutputLogic extends LogicType
// object MemoryLogic extends LogicType

