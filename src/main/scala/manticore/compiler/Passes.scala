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
// import manticore.compiler.assembly.levels.placed.PlacedIRPrinter
import manticore.compiler.assembly.levels.AssemblyPrinter
import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.assembly.levels.placed.LocalMemoryAllocation
import manticore.compiler.assembly.levels.placed.interpreter.AtomicInterpreter
import manticore.compiler.assembly.levels.placed.ExpectIdInsertion
import manticore.compiler.assembly.levels.placed.PlacedIRConstantFolding
import manticore.compiler.assembly.levels.placed.PlacedIRCommonSubexpressionElimination
// import manticore.compiler.assembly.levels.placed.PlacedIrJumpTableConstructionTransform

object ManticorePasses {

  def FrontendInterpreter(cond: Boolean = true) =
    UnconstrainedCloseSequentialCycles followedBy
      UnconstrainedInterpreter.guard(cond) followedBy
      UnconstrainedBreakSequentialCycles

  val frontend = UnconstrainedNameChecker followedBy
    UnconstrainedMakeDebugSymbols followedBy
    UnconstrainedOrderInstructions followedBy
    UnconstrainedRemoveAliases followedBy
    UnconstrainedDeadCodeElimination followedBy
    UnconstrainedRenameVariables

  val middleend =
    WidthConversion.transformation followedBy
      UnconstrainedRemoveAliases followedBy
      UnconstrainedDeadCodeElimination

  def BackendInterpreter(cond: Boolean = true) =
    AtomicInterpreter.guard(cond)

  val ExtractParallelism = // do not call DCE in the middle
    ProcessSplittingTransform followedBy
      PlacedIROrderInstructions followedBy
      ProcessMergingTransform followedBy
      PlacedIROrderInstructions followedBy
      RoundRobinPlacerTransform
  val BackendLowerEnd =
      LocalMemoryAllocation followedBy
      SendInsertionTransform followedBy
      PlacedIRDeadCodeElimination followedBy
      ListSchedulerTransform followedBy
      PredicateInsertionTransform followedBy
      GlobalPacketSchedulerTransform followedBy
      RegisterAllocationTransform followedBy
      ExpectIdInsertion

  def backend =
    UnconstrainedToPlacedTransform followedBy
    PlacedIRConstantFolding followedBy
    PlacedIRCommonSubexpressionElimination followedBy
    ExtractParallelism followedBy
    BackendLowerEnd

}
