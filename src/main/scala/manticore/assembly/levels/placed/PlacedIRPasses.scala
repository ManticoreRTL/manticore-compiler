package manticore.assembly.levels.placed

import manticore.assembly.levels.DeadCodeElimination
import manticore.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import manticore.assembly.levels.OrderInstructions

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
