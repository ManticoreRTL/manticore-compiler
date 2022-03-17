package manticore.compiler.assembly.levels.unconstrained.width

import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedRenameVariables

object WidthConversion {

    val core = WidthConversionCore
    val transformation = core followedBy UnconstrainedRenameVariables

}