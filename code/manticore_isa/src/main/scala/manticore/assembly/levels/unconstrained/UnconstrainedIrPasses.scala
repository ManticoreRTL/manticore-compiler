package manticore.assembly.levels.unconstrained

import manticore.assembly.levels.AssemblyNameChecker
import manticore.assembly.levels.OrderInstructions
import manticore.assembly.levels.RemoveAliases
import manticore.assembly.levels.DeadCodeElimination
import manticore.assembly.levels.AssemblyChecker
import manticore.compiler.AssemblyContext
import manticore.assembly.levels.AssemblyTransformer

object UnconstrainedNameChecker
    extends AssemblyNameChecker
    with AssemblyChecker[UnconstrainedIR.DefProgram] {
  val flavor = UnconstrainedIR
  override def check(
      source: UnconstrainedIR.DefProgram,
      context: AssemblyContext
  ): Unit = do_check(source, context)

}
object UnconstrainedOrderInstructions
    extends OrderInstructions
    with AssemblyTransformer[
      UnconstrainedIR.DefProgram,
      UnconstrainedIR.DefProgram
    ] {
  val flavor = UnconstrainedIR
  import flavor._
  override def transform(
      source: DefProgram,
      context: AssemblyContext
  ): DefProgram = do_transform(source, context)
}
object UnconstrainedRemoveAliases
    extends RemoveAliases
    with AssemblyTransformer[
      UnconstrainedIR.DefProgram,
      UnconstrainedIR.DefProgram
    ] {
  val flavor = UnconstrainedIR
  import flavor._
  override def transform(
      source: DefProgram,
      context: AssemblyContext
  ): DefProgram = do_transform(source, context)
}
object UnconstrainedDeadCodeElimination
    extends DeadCodeElimination
    with AssemblyTransformer[
      UnconstrainedIR.DefProgram,
      UnconstrainedIR.DefProgram
    ] {
  val flavor = UnconstrainedIR
  import flavor._
  override def transform(
      source: DefProgram,
      context: AssemblyContext
  ): DefProgram = do_transform(source, context)
}
