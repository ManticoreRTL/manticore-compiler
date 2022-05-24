package manticore.compiler.assembly.levels.unconstrained

import manticore.compiler.assembly.levels.AssemblyNameChecker
import manticore.compiler.assembly.levels.AssemblyPrinter
import manticore.compiler.assembly.levels.OrderInstructions
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
import manticore.compiler.assembly.levels.JumpTableConstruction
import manticore.compiler.assembly.levels.ParMuxDeconstruction
import manticore.compiler.assembly.annotations.Memblock
import manticore.compiler.assembly.annotations.AssemblyAnnotation
import manticore.compiler.assembly.annotations.AssemblyAnnotationFields
import manticore.compiler.assembly.annotations.StringValue
import manticore.compiler.assembly.annotations.IntValue
import manticore.compiler.assembly.levels.CanRenameToDebugSymbols
import manticore.compiler.assembly.annotations.DebugSymbol
import manticore.compiler.assembly.levels.CanCollectProgramStatistics

object UnconstrainedNameChecker
    extends AssemblyNameChecker
    with AssemblyChecker[UnconstrainedIR.DefProgram] {
  val flavor = UnconstrainedIR
  import flavor._
  override def check(
      program: DefProgram,
      context: AssemblyContext
  ): Unit = {

    program.processes.foreach { case process =>
      NameCheck.checkUniqueDefReg(process) { case (sec, fst) =>
        context.logger.error(
          s"name ${sec.variable.name} defined multiple times, first in ${fst.serialized}.",
          sec
        )
      }
      NameCheck.checkNames(process) { case (name, inst) =>
        context.logger.error(s"name ${name} is not defined!", inst)
      }
      NameCheck.checkLabels(process) { case (label, inst) =>
        context.logger.error(s"label ${label} is not defined!", inst)
      }
      NameCheck.checkSSA(process) { case NameCheck.NonSSA(rd, assigns) =>
        context.logger.error(
          s"${rd} is assigned ${assigns.length} times:\n${assigns.mkString("\n")}"
        )
      }

    }
    NameCheck.checkSends(program)(
      badDest = inst => context.logger.error("invalid destination", inst),
      selfDest = inst => context.logger.error("self SEND is not allowed", inst),
      badRegister =
        inst => context.logger.error("Bad destination register", inst)
    )

  }

}

object UnconstrainedRenameVariables
    extends RenameTransformation
    with AssemblyTransformer[
      UnconstrainedIR.DefProgram,
      UnconstrainedIR.DefProgram
    ] {

  val flavor = UnconstrainedIR
  import flavor._

  def mkFreshName(
      original: String,
      tpe: VariableType
  )(implicit
      ctx: AssemblyContext
  ) = {
    mkName(ctx.uniqueNumber(), original, tpe)
  }

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
    extends JumpTableConstruction
    with AssemblyTransformer[
      UnconstrainedIR.DefProgram,
      UnconstrainedIR.DefProgram
    ] {

  val flavor = UnconstrainedIR

  import flavor._

  override def uniqueLabel(ctx: AssemblyContext): Label =
    s"L${ctx.uniqueNumber()}"

  override def mkMemory(width: Int, size: Int)(implicit ctx: AssemblyContext) = {
    val name = s"%m${ctx.uniqueNumber()}"
    DefReg(
      MemoryVariable(
        name,
        width,
        size
      ),
      None,
      Seq(
        Memblock(
          Map(
            AssemblyAnnotationFields.Block.name -> StringValue(name),
            AssemblyAnnotationFields.Capacity.name -> IntValue(size),
            AssemblyAnnotationFields.Width.name -> IntValue(width)
          )
        )
      )
    )
  }

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

object UnconstrainedIRParMuxDeconstructionTransform
    extends ParMuxDeconstruction
    with AssemblyTransformer[
      UnconstrainedIR.DefProgram,
      UnconstrainedIR.DefProgram
    ] {
  val flavor = UnconstrainedIR
  import flavor._

  def mkWire(orig: DefReg)(implicit ctx: AssemblyContext): DefReg =
    DefReg(
      variable = LogicVariable(
        s"%w${ctx.uniqueNumber()}",
        orig.variable.width,
        WireType
      ),
      None
    ).setPos(orig.pos)

  override def transform(
      program: DefProgram,
      context: AssemblyContext
  ): DefProgram = {

    program.copy(processes = program.processes.map { proc =>
      do_transform(proc)(context)
    })

  }

}

object UnconstrainedIRDebugSymbolRenamer extends CanRenameToDebugSymbols {

  val flavor = UnconstrainedIR
  import flavor._

  def debugSymToName(dbg: DebugSymbol): Name =
    dbg.getSymbol() + (dbg.getIndex() match {
      case Some(index) => s"[$index]"
      case None        => ""
    })
  def constantName(v: BigInt, w: Int): Name = s"$$${w}d$v"
}

object UnconstrainedIRStatisticsCollector extends CanCollectProgramStatistics {
  val flavor = UnconstrainedIR
}
