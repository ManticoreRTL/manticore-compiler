package manticore.assembly.levels.unconstrained

import manticore.assembly.levels.AssemblyNameChecker
import manticore.assembly.levels.OrderInstructions
import manticore.assembly.levels.RemoveAliases
import manticore.assembly.levels.DeadCodeElimination
import manticore.assembly.levels.AssemblyChecker
import manticore.compiler.AssemblyContext
import manticore.assembly.levels.AssemblyTransformer
import manticore.assembly.levels.RenameTransformation
import manticore.assembly.levels.VariableType
import manticore.assembly.levels.WireType
import manticore.assembly.levels.RegType
import manticore.assembly.levels.ConstType
import manticore.assembly.levels.MemoryType
import manticore.assembly.levels.InputType
import manticore.assembly.levels.OutputType
import manticore.assembly.levels.CloseSequentialCycles
import manticore.assembly.levels.BreakSequentialCycles


object UnconstrainedNameChecker
    extends AssemblyNameChecker
    with AssemblyChecker[UnconstrainedIR.DefProgram] {
  val flavor = UnconstrainedIR
  override def check(
      source: UnconstrainedIR.DefProgram,
      context: AssemblyContext
  ): Unit = do_check(source, context)

}

object UnconstrainedRenameVariables
    extends RenameTransformation
    with AssemblyTransformer[
      UnconstrainedIR.DefProgram,
      UnconstrainedIR.DefProgram
    ] {

  val flavor = UnconstrainedIR
  import flavor._

  override def mkName(id: Long, original: String, tpe: VariableType): String =
    tpe match {
      case WireType   => s"%w${id}"
      case RegType    => s"%r${id}"
      case MemoryType => s"%m${id}"
      case ConstType  => s"%c${id}"
      case InputType  => s"%i${id}"
      case OutputType => s"%o${id}"
    }
  override def transform(p: DefProgram, ctx: AssemblyContext) =
    do_transform(p, ctx)
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
  val Zero = BigInt(0)
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


object UnconstrainedCloseSequentialCycles
      extends CloseSequentialCycles
      with AssemblyTransformer[
        UnconstrainedIR.DefProgram,
        UnconstrainedIR.DefProgram
      ] {

    val flavor = UnconstrainedIR

    import flavor._

    override def transform(
        source: DefProgram,
        context: AssemblyContext
    ): DefProgram = do_transform(source)(context)

  }

   object UnconstrainedBreakSequentialCycles
      extends BreakSequentialCycles
      with AssemblyTransformer[
        UnconstrainedIR.DefProgram,
        UnconstrainedIR.DefProgram
      ] {

    val flavor = UnconstrainedIR

    import flavor._

    override def transform(
        source: DefProgram,
        context: AssemblyContext
    ): DefProgram = do_transform(source)(context)

  }