package manticore.compiler.assembly.levels.placed.lowering

import manticore.compiler.assembly.levels.placed.PlacedIRTransformer
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.placed.JumpTableNormalizationTransform
import manticore.compiler.assembly.levels.placed.JumpLabelAssignmentTransform

object Lowering {

  val Transformation =
    JumpTableNormalizationTransform andThen
      ProgramSchedulingTransform andThen
      SetJumpTargetsTransform andThen
      LocalMemoryAllocation andThen
      RegisterAllocationTransform

}
