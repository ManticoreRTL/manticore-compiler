package manticore.compiler.integration

import manticore.compiler.UnitTest
import manticore.compiler.integration.thyrio.ExternalTool
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedNameChecker
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedMakeDebugSymbols
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedOrderInstructions
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedRemoveAliases
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedDeadCodeElimination
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedCloseSequentialCycles
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedInterpreter
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedBreakSequentialCycles
import manticore.compiler.assembly.levels.unconstrained.width.WidthConversionCore
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedRenameVariables
import manticore.compiler.assembly.levels.Transformation
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.UnitFixtureTest

trait ThyrioUnitTest extends UnitFixtureTest {

  val requiredTools: Seq[ExternalTool]

  def checkInstalled(): Unit =
    requiredTools.foreach { t =>
      val installed = t.installed()
      if (!installed)
        throw new UnsupportedOperationException(s"${t.name} is not installed")

    }




}
