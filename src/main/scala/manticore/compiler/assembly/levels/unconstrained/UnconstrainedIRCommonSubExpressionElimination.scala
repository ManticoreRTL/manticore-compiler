package manticore.compiler.assembly.levels.unconstrained

import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.CommonSubExpressionElimination
import manticore.compiler.AssemblyContext

object UnconstrainedIRCommonSubExpressionElimination
    extends AssemblyTransformer[UnconstrainedIR.DefProgram, UnconstrainedIR.DefProgram]
    with CommonSubExpressionElimination {

  override val flavor = UnconstrainedIR
  import flavor._

  override def transform(
      source: DefProgram,
      context: AssemblyContext
  ): DefProgram =
    do_transform(source)(context)

}
