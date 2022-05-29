package manticore.compiler.assembly.levels.unconstrained

import manticore.compiler.assembly.levels.StateUpdateOptimization
import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.AssemblyContext

object UnconstrainedIRStateUpdateOptimization
    extends StateUpdateOptimization
    with UnconstrainedIRTransformer {

  val flavor = UnconstrainedIR

  override def transform(program: UnconstrainedIR.DefProgram)(implicit
      ctx: AssemblyContext
  ): UnconstrainedIR.DefProgram = do_transform(program)
}
