package manticore.compiler

import manticore.compiler.assembly.levels.placed.UnconstrainedToPlacedTransform
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.assembly.levels.placed.PlacedNameChecker

import manticore.compiler.assembly.levels.placed.ListSchedulerTransform
import manticore.compiler.assembly.levels.placed.GlobalPacketSchedulerTransform
import manticore.compiler.assembly.levels.placed.PredicateInsertionTransform
import manticore.compiler.assembly.levels.unconstrained._
import manticore.compiler.assembly.levels.DeadCodeElimination
import manticore.compiler.assembly.levels.unconstrained.width.WidthConversion

import manticore.compiler.assembly.levels.placed.ProcessSplittingTransform
import manticore.compiler.assembly.levels.placed.PlacedIRDeadCodeElimination
import manticore.compiler.assembly.levels.placed.PlacedIROrderInstructions
import manticore.compiler.assembly.levels.placed.ProcessMergingTransform
import manticore.compiler.assembly.levels.placed.RoundRobinPlacerTransform
import manticore.compiler.assembly.levels.placed.SendInsertionTransform
import manticore.compiler.assembly.levels.placed.PlacedIRCloseSequentialCycles
import manticore.compiler.assembly.levels.placed.RegisterAllocationTransform
import manticore.compiler.assembly.levels.AssemblyPrinter
import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.assembly.levels.placed.LocalMemoryAllocation
import manticore.compiler.assembly.levels.placed.interpreter.AtomicInterpreter
import manticore.compiler.assembly.levels.placed.ExpectIdInsertion
import manticore.compiler.assembly.levels.placed.PlacedIRConstantFolding
import manticore.compiler.assembly.levels.placed.PlacedIRCommonSubExpressionElimination

object ManticorePasses {

  def FrontendInterpreter(cond: Boolean = true) =
    UnconstrainedCloseSequentialCycles andThen
      UnconstrainedInterpreter.withCondition(cond) andThen
      UnconstrainedBreakSequentialCycles

  val frontend = UnconstrainedNameChecker andThen
    UnconstrainedMakeDebugSymbols andThen
    UnconstrainedOrderInstructions andThen
    UnconstrainedIRConstantFolding andThen
    UnconstrainedDeadCodeElimination andThen
    UnconstrainedRenameVariables

  val middleend =
    WidthConversion.transformation andThen
      UnconstrainedIRConstantFolding andThen
      UnconstrainedDeadCodeElimination

  def BackendInterpreter(cond: Boolean = true) =
    AtomicInterpreter.withCondition(cond)

  val ExtractParallelism = // do not call DCE in the middle
    ProcessSplittingTransform andThen
      PlacedIROrderInstructions andThen
      ProcessMergingTransform andThen
      PlacedIROrderInstructions andThen
      RoundRobinPlacerTransform
  val BackendLowerEnd =
      LocalMemoryAllocation andThen
      SendInsertionTransform andThen
      PlacedIRDeadCodeElimination andThen
      ListSchedulerTransform andThen
      PredicateInsertionTransform andThen
      GlobalPacketSchedulerTransform andThen
      RegisterAllocationTransform andThen
      ExpectIdInsertion

  def backend =
    UnconstrainedToPlacedTransform andThen
    PlacedIRConstantFolding andThen
    PlacedIRCommonSubExpressionElimination andThen
    ExtractParallelism andThen
    BackendLowerEnd

}
