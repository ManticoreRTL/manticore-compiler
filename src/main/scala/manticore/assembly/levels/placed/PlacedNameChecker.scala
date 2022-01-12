package manticore.assembly.levels.placed
import manticore.assembly.levels.AssemblyChecker
import manticore.assembly.levels.AssemblyNameChecker
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
