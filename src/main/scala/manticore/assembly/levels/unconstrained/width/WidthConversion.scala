package manticore.assembly.levels.unconstrained.width

import manticore.assembly.levels.AssemblyTransformer
import manticore.assembly.levels.unconstrained.UnconstrainedRenameVariables

object WidthConversion {

    val core = WidthConversionCore
    val transformation = core followedBy UnconstrainedRenameVariables

}