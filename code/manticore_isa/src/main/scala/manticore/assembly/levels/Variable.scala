package manticore.assembly.levels

/**
  * High-level variable type
  * @author Mahyar Emami <mahyar.emami@epfl.ch>
  */

sealed abstract class LogicType
object ConstLogic extends LogicType
object WireLogic extends LogicType
object RegLogic extends LogicType
object InputLogic extends LogicType
object OutputLogic extends LogicType
object MemoryLogic extends LogicType

