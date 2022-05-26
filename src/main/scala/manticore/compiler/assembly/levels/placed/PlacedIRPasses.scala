package manticore.compiler.assembly.levels.placed

import manticore.compiler.assembly.levels.DeadCodeElimination
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.OrderInstructions
import manticore.compiler.assembly.levels.CloseSequentialCycles
import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.assembly.levels.CanRenameToDebugSymbols
import manticore.compiler.assembly.annotations.DebugSymbol
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.assembly.levels.BreakSequentialCycles
import manticore.compiler.assembly.levels.CanCollectProgramStatistics
import manticore.compiler.assembly.levels.CanRename

object PlacedIRDeadCodeElimination
    extends DeadCodeElimination
    with PlacedIRTransformer {

  val flavor = PlacedIR
  import flavor._
  override def transform(
      source: DefProgram
  )(implicit context: AssemblyContext): DefProgram = source
    .copy(processes = source.processes.map { doDce(_)(context) })
    .setPos(source.pos)
}

object PlacedIROrderInstructions
    extends OrderInstructions
    with PlacedIRTransformer {

  val flavor = PlacedIR
  import flavor._
  override def transform(source: DefProgram)(implicit
      context: AssemblyContext
  ): DefProgram = do_transform(source, context)
}

object PlacedIRCloseSequentialCycles
    extends CloseSequentialCycles
    with PlacedIRTransformer {

  val flavor = PlacedIR
  import flavor._
  override def transform(source: DefProgram)(implicit
      context: AssemblyContext
  ): DefProgram = do_transform(source)(context)
}

object PlacedIRBreakSequentialCycles
    extends BreakSequentialCycles
    with PlacedIRTransformer {

  val flavor = PlacedIR
  import flavor._
  override def transform(source: DefProgram)(implicit
      context: AssemblyContext
  ): DefProgram = do_transform(source)(context)
}

object PlacedIRDebugSymbolRenamer extends CanRenameToDebugSymbols {

  val flavor = PlacedIR
  import flavor._

  def debugSymToName(dbg: DebugSymbol): Name =
    dbg.getSymbol() + (dbg.getIndex() match {
      case Some(index) => s"[$index]"
      case None        => ""
    })
  def constantName(v: UInt16, w: Int): Name = s"$$$v"
}

object PlacedIRStatisticCollector extends CanCollectProgramStatistics {
  val flavor = PlacedIR
}

object PlacedIRRenamer extends CanRename {

  val flavor = PlacedIR

}
