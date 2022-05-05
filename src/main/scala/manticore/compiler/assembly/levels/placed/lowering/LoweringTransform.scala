package manticore.compiler.assembly.levels.placed.lowering

import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.AssemblyContext

object LoweringTransform
    extends AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {

  private val Impl =
    InstructionScheduling followedBy
    PredicateInsertionTransform followedBy
    RegisterAllocationTransform
  override def transform(
      source: PlacedIR.DefProgram,
      context: AssemblyContext
  ): PlacedIR.DefProgram = {
    Impl(source, context)._1
  }

}
