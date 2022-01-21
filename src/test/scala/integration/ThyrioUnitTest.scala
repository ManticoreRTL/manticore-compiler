package integration

import manticore.UnitTest
import integration.thyrio.integration.thyrio.ExternalTool
import manticore.assembly.levels.unconstrained.UnconstrainedNameChecker
import manticore.assembly.levels.unconstrained.UnconstrainedMakeDebugSymbols
import manticore.assembly.levels.unconstrained.UnconstrainedOrderInstructions
import manticore.assembly.levels.unconstrained.UnconstrainedRemoveAliases
import manticore.assembly.levels.unconstrained.UnconstrainedDeadCodeElimination
import manticore.assembly.levels.unconstrained.UnconstrainedCloseSequentialCycles
import manticore.assembly.levels.unconstrained.UnconstrainedInterpreter
import manticore.assembly.levels.unconstrained.UnconstrainedBreakSequentialCycles
import manticore.assembly.levels.unconstrained.width.WidthConversionCore
import manticore.assembly.levels.unconstrained.UnconstrainedRenameVariables
import manticore.assembly.levels.Transformation
import manticore.assembly.levels.unconstrained.UnconstrainedIR
import manticore.UnitFixtureTest

trait ThyrioUnitTest extends UnitFixtureTest {

  val requiredTools: Seq[ExternalTool]

  def checkInstalled(): Unit =
    requiredTools.foreach { t =>
      val installed = t.installed()
      if (!installed)
        throw new UnsupportedOperationException(s"${t.name} is not installed")

    }

  object ManticoreFrontend {
    type Phase =
      Transformation[UnconstrainedIR.DefProgram, UnconstrainedIR.DefProgram]
    def initialPhases() = UnconstrainedNameChecker followedBy
      UnconstrainedMakeDebugSymbols followedBy
      UnconstrainedOrderInstructions followedBy
      UnconstrainedRemoveAliases followedBy
      UnconstrainedDeadCodeElimination followedBy
      UnconstrainedCloseSequentialCycles followedBy
      UnconstrainedInterpreter followedBy
      UnconstrainedBreakSequentialCycles
    def finalPhases() = WidthConversionCore followedBy
      UnconstrainedRenameVariables followedBy
      UnconstrainedRemoveAliases followedBy
      UnconstrainedDeadCodeElimination followedBy
      UnconstrainedCloseSequentialCycles followedBy
      UnconstrainedInterpreter followedBy
      UnconstrainedBreakSequentialCycles
  }

}
