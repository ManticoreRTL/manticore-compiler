package manticore.compiler.assembly.levels.placed

import manticore.compiler.UnitTest

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedDeadCodeElimination
import manticore.compiler.ManticorePasses
import java.nio.file.{Paths, Files, Path}
import manticore.compiler.UnitFixtureTest
import manticore.compiler.ManticorePasses
import manticore.compiler.assembly.utils.Mips32Circuit
import manticore.compiler.assembly.utils.PicoRv32Circuit
import manticore.compiler.assembly.utils.Swizzle
import manticore.compiler.assembly.utils.Xormix32
import manticore.compiler.UnitTestMatchers
import manticore.compiler.assembly.levels.placed.interpreter.AtomicInterpreter
import manticore.compiler.assembly.levels.placed.interpreter.PlacedIRInterpreterMonitor
import manticore.compiler.assembly.utils.XorReduce
import manticore.compiler.HasLoggerId
import java.io.PrintWriter
import scala.collection.mutable.{HashMap => MHashMap}
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedNameChecker
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedMakeDebugSymbols
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedOrderInstructions
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRConstantFolding
import manticore.compiler.assembly.levels.ParMuxDeconstruction
import manticore.compiler.assembly.levels.unconstrained.width.WidthConversion
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRCommonSubExpressionElimination
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRParMuxDeconstructionTransform
import manticore.compiler.DefaultHardwareConfig

class CustomLutInsertionTester extends UnitFixtureTest with UnitTestMatchers {

  import manticore.compiler.assembly.levels.placed.PlacedIR._

  behavior of "custom lut insertion transform"

  val sources = Seq(
    Mips32Circuit,
    PicoRv32Circuit,
    Swizzle,
    XorReduce,
    Xormix32,
  )

  val dims = Seq(
    (1, 1),
    (4, 4)
  )

  val numLutInputs = Seq(
    // 2,
    // 3,
    4,
    // 5,
    // 6
  )

  val shareCustomFuncs = Seq(
    false,
    true
  )

  val prelimOpts = UnconstrainedIRConstantFolding andThen
    UnconstrainedIRCommonSubExpressionElimination andThen
    UnconstrainedDeadCodeElimination
  val frontend = AssemblyParser andThen
    UnconstrainedNameChecker andThen
    UnconstrainedMakeDebugSymbols andThen
    UnconstrainedOrderInstructions andThen
    prelimOpts andThen
    UnconstrainedIRParMuxDeconstructionTransform andThen
    WidthConversion.transformation andThen
    prelimOpts

  val lowerCompiler = frontend andThen
    UnconstrainedToPlacedTransform andThen
    PlacedIRConstantFolding andThen
    PlacedIRCommonSubExpressionElimination andThen
    ProcessSplittingTransform

  def interpret(program: PlacedIR.DefProgram)(implicit ctx: AssemblyContext) = {

    val prepare = PlacedIRCloseSequentialCycles andThen
      JumpTableNormalizationTransform andThen
      JumpLabelAssignmentTransform
    val prepared = prepare(program)
    AtomicInterpreter(prepared)
  }

  sources.foreach { source =>
    dims.foreach { case (dimx, dimy) =>
      numLutInputs.foreach { numCustomInstrInputs =>
        shareCustomFuncs.foreach { shareLuts =>
          it should s"reduce virtual cycle lengths for (${source.name}, ${dimx} x ${dimy}, ${numCustomInstrInputs}-LUT, shareLuts = ${shareLuts})" in {
            f =>
              implicit val ctx = AssemblyContext(
                dump_all = true,
                dump_dir = Some(f.test_dir.toFile()),
                debug_message = false,
                hw_config = DefaultHardwareConfig(dimX = dimx, dimY = dimy, nCustomFunctions = 32, nCfuInputs = numCustomInstrInputs),
                optimize_common_custom_functions = shareLuts,
                max_cycles = 200,
                log_file = Some(f.test_dir.resolve("output.log").toFile())
              )

              val progLowered = lowerCompiler(source(f))

              // Sanity check that the program interprets correctly without LUTs
              interpret(progLowered)

              val progWithLutsDce = CustomLutInsertion.post(progLowered)

              // Sanity check that the program interprets correctly without LUTs
              interpret(progWithLutsDce)

              def computeVirtualCycle(prog: DefProgram): Int = {
                prog.processes.map(proc => proc.body.length).max
              }

              def computePercentDelta(before: Int, after: Int): String = {
                val delta           = (after - before).toDouble
                val absPercentDelta = (math.abs(delta) / before) * 100
                val sign            = if (delta > 0) "+" else "-"
                s"${sign} %.2f %%".format(absPercentDelta)
              }

              val vCyclesBefore = computeVirtualCycle(progLowered)
              val vCyclesAfter  = computeVirtualCycle(progWithLutsDce)
              val percentDeltaStr =
                computePercentDelta(vCyclesBefore, vCyclesAfter)

              val numCustFuncsPerCore = progWithLutsDce.processes.map { proc =>
                proc.functions.size
              }.sorted
              val numCustFuncsTotal = numCustFuncsPerCore.sum
              println(
                s"${source.name}, ${dimx} x ${dimy}, ${numCustomInstrInputs}-LUT, share LUTs = ${shareLuts}, vCycle ${vCyclesBefore} -> ${vCyclesAfter} (${percentDeltaStr}) using ${numCustFuncsPerCore
                  .mkString("+")} = ${numCustFuncsTotal} custom functions"
              )
              // assert(vCyclesBefore >= vCyclesAfter)

              ctx.logger.dumpArtifact("stats.yaml") {
                ctx.stats.asYaml
              }(new HasLoggerId { val id = "stats_file" })
          }
        }
      }
    }
  }
}
