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

object PlacedIRDeadCodeElimination
    extends DeadCodeElimination
    with AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {

  val flavor = PlacedIR
  import flavor._
  override def transform(
      source: DefProgram,
      context: AssemblyContext
  ): DefProgram = source
    .copy(processes = source.processes.map { doDce(_)(context) })
    .setPos(source.pos)
}

object PlacedIROrderInstructions
    extends OrderInstructions
    with AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {

  val flavor = PlacedIR
  import flavor._
  override def transform(
      source: DefProgram,
      context: AssemblyContext
  ): DefProgram = do_transform(source, context)
}

object PlacedIRCloseSequentialCycles
    extends CloseSequentialCycles
    with AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {

  val flavor = PlacedIR
  import flavor._
  override def transform(
      source: DefProgram,
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
