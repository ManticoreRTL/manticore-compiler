package manticore.compiler

import java.io.File



import manticore.assembly.levels.UnconstrainedIR

case class CompilationConfig(
    sources: Seq[File],
    print_tree: Boolean
)

case class CompilationContext(
    config:  CompilationConfig,
    root: UnconstrainedIR.IRNode
)