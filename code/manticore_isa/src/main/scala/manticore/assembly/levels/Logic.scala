package manticore.assembly.levels


sealed abstract class LogicType
object WireLogic extends LogicType
object RegLogic extends LogicType
object InputLogic extends LogicType
object OutputLogic extends LogicType
object MemoryLogic extends LogicType