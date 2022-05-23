package manticore.compiler.assembly.levels.placed.lowering

import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.placed.JumpTableNormalizationTransform
import manticore.compiler.assembly.levels.placed.JumpLabelAssignmentTransform
import manticore.compiler.assembly.levels.placed.LocalMemoryAllocation

object LoweringTransform
    extends AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {

  private val Impl =
    JumpTableNormalizationTransform followedBy
    ProgramSchedulingTransform followedBy
    JumpLabelAssignmentTransform followedBy
    LocalMemoryAllocation followedBy
    RegisterAllocationTransform
  override def transform(
      source: PlacedIR.DefProgram,
      context: AssemblyContext
  ): PlacedIR.DefProgram = {
    Impl(source, context)._1
  }

}
