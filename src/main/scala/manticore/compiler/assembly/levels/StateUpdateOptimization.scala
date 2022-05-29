package manticore.compiler.assembly.levels

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.assembly.levels.OutputType
import manticore.compiler.assembly.levels.CanRename
import manticore.compiler.assembly.CanComputeNameDependence

/** A simple optimization path to remove redundant MOV instructions to output
  * names. This is basically a hack to work around the fact that CF or CSE do
  * not remove them since they visit instruction in the program order and we do
  * not have real Phi nodes at the beginning of the implicit loop to be able to
  * rename phi operands and therefore correctly close sequential cycles.
  *
  * @author
  *   Mahyar Emami
  */
trait StateUpdateOptimization extends CanRename with CanComputeNameDependence {

  import flavor._

  protected def do_transform(program: DefProgram)(implicit
      ctx: AssemblyContext
  ): DefProgram = program.copy(processes = program.processes.map(do_transform))

  private def do_transform(
      process: DefProcess
  )(implicit ctx: AssemblyContext) = {

    val registers = process.registers.map { r => r.variable.name -> r }.toMap
    val isStateUpdateName = registers andThen (_.variable.varType == OutputType )
    val isWire = registers andThen (_.variable.varType == WireType)

    val toRemove = scala.collection.mutable.Set.empty[Instruction]
    val subst = scala.collection.mutable.Map.empty[Name, Name].withDefault(r => r)
    process.body.reverseIterator.foreach {
      // we can only move write to output back up if the operand of a mov is
      // a temporary value, i.e., a wire. We can not do the same if rs is InpuType
      // MemoryType or ConstType because this pass is not supposed to remove
      // dead code.
      case mov @ Mov(rd, rs, _) if isStateUpdateName(rd) && isWire(rs) =>
        toRemove += mov
        subst += (rs -> rd)
      case _ => // nothing to do
    }

    val renamedBody =
      process.body.filter(!toRemove(_)).map(Rename.asRenamed(_)(subst))
    val referenced = NameDependence.referencedNames(renamedBody)
    val newRegs = process.registers.filter(r =>
      referenced(
        r.variable.name
      ) || r.variable.varType == OutputType || r.variable.varType == InputType
    )

    process.copy(
      body = renamedBody,
      registers = newRegs
    )

  }

}
