package manticore.compiler

import manticore.assembly.levels.placed.UnconstrainedToPlacedTransform
import manticore.assembly.levels.placed.PlacedIR
import manticore.assembly.levels.placed.PlacedNameChecker
import scala.language.postfixOps
import manticore.assembly.levels.placed.ListSchedulerTransform
import manticore.assembly.levels.placed.GlobalPacketSchedulerTransform
import manticore.assembly.levels.placed.PredicateInsertionTransform
import manticore.assembly.levels.unconstrained._
import manticore.assembly.levels.DeadCodeElimination
import manticore.assembly.levels.unconstrained.width.WidthConversion

import manticore.assembly.levels.placed.ProcessSplittingTransform
import manticore.assembly.levels.placed.PlacedIRDeadCodeElimination
import manticore.assembly.levels.placed.PlacedIROrderInstructions
import manticore.assembly.levels.placed.ProcessMergingTransform
import manticore.assembly.levels.placed.RoundRobinPlacerTransform
import manticore.assembly.levels.placed.SendInsertionTransform
import manticore.assembly.levels.placed.PlacedIRCloseSequentialCycles
import manticore.assembly.levels.placed.RegisterAllocationTransform
import manticore.assembly.levels.placed.PlacedIRPrinter
import manticore.assembly.levels.AssemblyPrinter
import manticore.assembly.ManticoreAssemblyIR
import manticore.assembly.levels.placed.LocalMemoryAllocation
import manticore.assembly.levels.placed.interpreter.AtomicInterpreter
object ManticorePasses {

  val frontend_interpreter =
    UnconstrainedCloseSequentialCycles followedBy
    UnconstrainedInterpreter followedBy
    UnconstrainedBreakSequentialCycles

  val frontend = UnconstrainedNameChecker followedBy
    UnconstrainedMakeDebugSymbols followedBy
    UnconstrainedOrderInstructions followedBy
    UnconstrainedRemoveAliases followedBy
    UnconstrainedDeadCodeElimination

  val middleend =
    WidthConversion.transformation followedBy
    UnconstrainedRemoveAliases followedBy
    UnconstrainedDeadCodeElimination



  val backend =
      UnconstrainedToPlacedTransform followedBy
      ProcessSplittingTransform followedBy
      PlacedIROrderInstructions followedBy
      PlacedIRDeadCodeElimination followedBy
      ProcessMergingTransform followedBy
      LocalMemoryAllocation followedBy
      PlacedIROrderInstructions followedBy
      PlacedIRDeadCodeElimination followedBy
      RoundRobinPlacerTransform followedBy
      SendInsertionTransform followedBy
      ListSchedulerTransform followedBy
      PredicateInsertionTransform followedBy
      GlobalPacketSchedulerTransform followedBy
      RegisterAllocationTransform
   val backend_atomic_interpreter =
      AtomicInterpreter


}
