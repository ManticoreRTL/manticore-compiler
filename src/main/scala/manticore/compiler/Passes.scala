package manticore.compiler

import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.assembly.levels.AssemblyPrinter
import manticore.compiler.assembly.levels.DeadCodeElimination
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.assembly.levels.placed.PlacedIRCloseSequentialCycles
import manticore.compiler.assembly.levels.placed.PlacedIRCommonSubExpressionElimination
import manticore.compiler.assembly.levels.placed.PlacedIRConstantFolding
import manticore.compiler.assembly.levels.placed.PlacedIRDeadCodeElimination
import manticore.compiler.assembly.levels.placed.PlacedIROrderInstructions
import manticore.compiler.assembly.levels.placed.PlacedNameChecker
import manticore.compiler.assembly.levels.placed.ProcessSplittingTransform
import manticore.compiler.assembly.levels.placed.UnconstrainedToPlacedTransform
import manticore.compiler.assembly.levels.placed.interpreter.AtomicInterpreter
import manticore.compiler.assembly.levels.placed.lowering.AbstractExecution
import manticore.compiler.assembly.levels.placed.lowering.Lowering
import manticore.compiler.assembly.levels.unconstrained._
import manticore.compiler.assembly.levels.unconstrained.width.WidthConversion
import manticore.compiler.assembly.levels.placed.parallel.AnalyticalPlacerTransform
import manticore.compiler.assembly.levels.placed.parallel.BalancedSplitMergerTransform
import manticore.compiler.assembly.levels.placed.CustomLutInsertion

object ManticorePasses {

  def FrontendInterpreter(cond: Boolean = true) =
    UnconstrainedCloseSequentialCycles andThen
      UnconstrainedInterpreter.withCondition(cond) andThen
      UnconstrainedBreakSequentialCycles

  val frontend =
    UnconstrainedNameChecker andThen
      UnconstrainedMakeDebugSymbols andThen
      UnconstrainedOrderInstructions andThen
      UnconstrainedIRConstantFolding andThen
      UnconstrainedIRStateUpdateOptimization andThen
      UnconstrainedIRCommonSubExpressionElimination andThen
      UnconstrainedDeadCodeElimination andThen
      UnconstrainedNameChecker andThen
      UnconstrainedIRParMuxDeconstructionTransform andThen
      UnconstrainedNameChecker andThen
      UnconstrainedGraphStatistics

  val middleend =
    WidthConversion.transformation andThen
      UnconstrainedIRConstantFolding andThen
      UnconstrainedIRStateUpdateOptimization andThen
      UnconstrainedIRCommonSubExpressionElimination andThen
      UnconstrainedDeadCodeElimination andThen
      UnconstrainedNameChecker

  def BackendInterpreter(cond: Boolean = true) =
    AtomicInterpreter.withCondition(cond)

  val ExtractParallelism =
    BalancedSplitMergerTransform andThen
      PlacedNameChecker andThen
      AnalyticalPlacerTransform

  val BackendLowerEnd = Lowering.Transformation andThen AbstractExecution

  val ToPlaced = UnconstrainedToPlacedTransform andThen
      PlacedIRConstantFolding andThen
      PlacedIRCommonSubExpressionElimination andThen
      PlacedIRDeadCodeElimination

  def backend =
    UnconstrainedToPlacedTransform andThen
      PlacedIRConstantFolding andThen
      PlacedIRCommonSubExpressionElimination andThen
      PlacedIRDeadCodeElimination andThen
      ExtractParallelism andThen
      CustomLutInsertion.post andThen
      BackendLowerEnd


}
