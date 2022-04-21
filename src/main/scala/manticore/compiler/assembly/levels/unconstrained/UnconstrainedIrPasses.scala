package manticore.compiler.assembly.levels.unconstrained

import manticore.compiler.assembly.levels.AssemblyNameChecker
import manticore.compiler.assembly.levels.AssemblyPrinter
import manticore.compiler.assembly.levels.OrderInstructions
import manticore.compiler.assembly.levels.RemoveAliases
import manticore.compiler.assembly.levels.DeadCodeElimination
import manticore.compiler.assembly.levels.AssemblyChecker
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.RenameTransformation
import manticore.compiler.assembly.levels.VariableType
import manticore.compiler.assembly.levels.WireType
import manticore.compiler.assembly.levels.RegType
import manticore.compiler.assembly.levels.ConstType
import manticore.compiler.assembly.levels.MemoryType
import manticore.compiler.assembly.levels.InputType
import manticore.compiler.assembly.levels.OutputType
import manticore.compiler.assembly.levels.CloseSequentialCycles
import manticore.compiler.assembly.levels.BreakSequentialCycles
import manticore.compiler.assembly.levels.CarryType
import manticore.compiler.assembly.levels.JumpTableConstructionTransform

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
      case CarryType  => s"%s${id}"
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
  ): DefProgram = {
    source
      .copy(processes = source.processes.map { doDce(_)(context) })
      .setPos(source.pos)
  }
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

object UnconstrainedPrinter
    extends AssemblyPrinter[UnconstrainedIR.DefProgram] {}

object UnconstrainedJumpTableConstruction
    extends JumpTableConstructionTransform
    with AssemblyTransformer[
      UnconstrainedIR.DefProgram,
      UnconstrainedIR.DefProgram
    ] {

  val flavor = UnconstrainedIR

  import flavor._

  override def uniqueLabel(ctx: AssemblyContext): Label =
    s"L${ctx.uniqueNumber()}"

  override def mkMemory(width: Int)(implicit ctx: AssemblyContext) = DefReg(
    LogicVariable(
      s"%m${ctx.uniqueNumber()}",
      width,
      MemoryType
    ),
    None
  )

  override def mkWire(width: Int)(implicit ctx: AssemblyContext) = DefReg(
    LogicVariable(
      s"%w${ctx.uniqueNumber()}",
      width,
      WireType
    )
  )

  override def indexSequence(to: Int) = Seq.tabulate(to) { i => BigInt(i) }

  override val Zero = BigInt(0)

  override def mkConstant(width: Int, value: Constant)(implicit
      ctx: AssemblyContext
  ) = DefReg(
    LogicVariable(s"%c${ctx.uniqueNumber()}", width, ConstType),
    Some(value)
  )
  override def transform(
      source: DefProgram,
      context: AssemblyContext
  ): DefProgram = do_transform(source)(context)

}
