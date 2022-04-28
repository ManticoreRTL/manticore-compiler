package manticore.compiler.assembly.levels.placed

import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.CommonSubExpressionElimination
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.ManticoreAssemblyIR

object PlacedIRCommonSubExpressionElimination
    extends AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram]
    with CommonSubExpressionElimination {

  override val flavor = PlacedIR
  import flavor._

  override def transform(
      source: DefProgram,
      context: AssemblyContext
  ): DefProgram =
    do_transform(source)(context)

}
