package manticore.compiler.assembly.levels.placed
import manticore.compiler.assembly.levels.AssemblyChecker
import manticore.compiler.assembly.levels.AssemblyNameChecker
import manticore.compiler.AssemblyContext

object PlacedNameChecker
    extends AssemblyNameChecker
    with AssemblyChecker[PlacedIR.DefProgram] {
  val flavor = PlacedIR
  override def check(
      source: flavor.DefProgram,
      context: AssemblyContext
  ): Unit = do_check(source, context)
}
