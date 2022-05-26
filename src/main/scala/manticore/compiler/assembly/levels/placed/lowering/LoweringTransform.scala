package manticore.compiler.assembly.levels.placed.lowering

import manticore.compiler.assembly.levels.placed.PlacedIRTransformer
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.placed.JumpTableNormalizationTransform
import manticore.compiler.assembly.levels.placed.JumpLabelAssignmentTransform
import manticore.compiler.assembly.levels.placed.LocalMemoryAllocation

object LoweringTransform extends PlacedIRTransformer {

  private val Impl =
    JumpTableNormalizationTransform andThen
      ProgramSchedulingTransform andThen
      JumpLabelAssignmentTransform andThen
      LocalMemoryAllocation andThen
      RegisterAllocationTransform
  override def transform(source: PlacedIR.DefProgram)(implicit
      context: AssemblyContext
  ): PlacedIR.DefProgram = {
    Impl(source)
  }

}
