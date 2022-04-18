package manticore.compiler.assembly.levels.placed

import manticore.compiler.assembly.levels.DeadCodeElimination
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.OrderInstructions
import manticore.compiler.assembly.levels.CloseSequentialCycles
import manticore.compiler.assembly.levels.JumpTableConstructionTransform
import manticore.compiler.assembly.ManticoreAssemblyIR

object PlacedIRDeadCodeElimination
    extends DeadCodeElimination
    with AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {

  val flavor = PlacedIR
  import flavor._
  override def transform(
      source: DefProgram,
      context: AssemblyContext
  ): DefProgram = do_transform(source, context)
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
