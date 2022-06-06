package manticore.compiler.assembly.levels.placed.interpreter
import manticore.compiler.assembly.levels.placed.PlacedIR._
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.ConstType
import manticore.compiler.assembly.levels.CarryType
import manticore.compiler.assembly.levels.InputType
import manticore.compiler.assembly.levels.MemoryType
import manticore.compiler.assembly.levels.InterpreterMonitor

sealed trait RegisterFile {
  def write(name: Name, value: UInt16): Unit
  def read(name: Name): UInt16
}

/// A virtually unbounded register file
final class NamedRegisterFile(
    proc: DefProcess,
    vcd: Option[PlacedValueChangeWriter],
    monitor: Option[PlacedIRInterpreterMonitor]
)(undefinedConstant: DefReg => Unit)
    extends RegisterFile
    with InterpreterMonitor.CanUpdateMonitor[NamedRegisterFile] {

  private val register_file =
    scala.collection.mutable.Map.empty[Name, UInt16] ++ proc.registers.map { r =>
      if (r.variable.varType == ConstType && r.value.isEmpty) {
        undefinedConstant(r)
      }
      r.variable.name -> r.value.getOrElse(UInt16(0))
    }
  override def write(rd: Name, value: UInt16): Unit = {
    register_file(rd) = value
    vcd.foreach(_.update(rd, value))
    monitor.foreach { _.update(rd, value) }
  }
  override def read(rs: Name): UInt16 = register_file(rs)

}

// a bounded size register file
final class PhysicalRegisterFile(
    proc: DefProcess,
    vcd: Option[PlacedValueChangeWriter],
    monitor: Option[PlacedIRInterpreterMonitor],
    maxRegs: Int,
    maxCarries: Int
)(
    undefinedConstant: DefReg => Unit, // callback for undefined constant
    badAlloc: DefReg => Unit,          // callback for invalid register allocation
    badInit: DefReg => Unit            // callback for bad initialization
) extends RegisterFile
    with InterpreterMonitor.CanUpdateMonitor[PhysicalRegisterFile] {

  private val register_file       = Array.fill(maxRegs) { UInt16(0) }
  private val carry_register_file = Array.fill(maxCarries) { UInt16(0) }

  private val name_to_ids = proc.registers.map { r: DefReg =>
    val reg_id = r.variable.id
    val max_id =
      if (r.variable.varType == CarryType) maxCarries
      else maxRegs

    if (reg_id < 0 || reg_id >= max_id) {
      badAlloc(r)
      r.variable.name -> (0, register_file)
    } else {
      if (r.variable.varType == ConstType && r.value.isEmpty) {
        undefinedConstant(r)
      }
      // set the initial value if there is one
      if (
        r.variable.varType == InputType ||
        r.variable.varType == ConstType ||
        r.variable.varType == MemoryType
      ) {
        r.value match {
          case Some(x) => register_file(reg_id) = x
          case None    => // nothing to do
        }
      } else if (r.value.nonEmpty) {
        badInit(r)
      }

      if (r.variable.varType == CarryType) {
        r.variable.name -> (reg_id, carry_register_file)
      } else {
        r.variable.name -> (reg_id, register_file)
      }
    }
  }.toMap

  override def write(rd: Name, value: UInt16): Unit = {
    //look up the index
    val (index, container) = name_to_ids(rd)
    container(index) = value
    vcd.foreach { _.update(rd, value) }
    monitor.foreach { _.update(rd, value) }

  }

  override def read(rs: Name): UInt16 = {
    val (index, container) = name_to_ids(rs)
    container(index)
  }

}
