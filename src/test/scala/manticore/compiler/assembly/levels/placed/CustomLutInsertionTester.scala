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

class CustomLutInsertionTester extends UnitFixtureTest with UnitTestMatchers {

  import manticore.compiler.assembly.levels.placed.PlacedIR._

  behavior of "custom lut insertion transform"

  val sources = Seq(
    Mips32Circuit,
    PicoRv32Circuit,
    Swizzle,
    XorReduce,
    Xormix32
  )

  val dims = Seq(
    (1, 1),
    (4, 4)
  )

  val numLutInputs = Seq(
    2,
    3,
    4,
    5,
    6
  )

  val shareCustomFuncs = Seq(
    false,
    true
  )

  val lowerCompiler = AssemblyParser andThen ManticorePasses.frontend andThen
    ManticorePasses.middleend andThen
    UnconstrainedToPlacedTransform andThen
    PlacedIRConstantFolding andThen
    PlacedIRCommonSubExpressionElimination andThen
    ManticorePasses.ExtractParallelism andThen
    SendInsertionTransform

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
                max_custom_instructions = 32,
                max_custom_instruction_inputs = numCustomInstrInputs,
                optimize_common_custom_functions = shareLuts,
                max_dimx = dimx,
                max_dimy = dimy,
                max_cycles = 200,
                log_file = Some(f.test_dir.resolve("output.log").toFile())
              )

              val progLowered = lowerCompiler(source(f))

              // Sanity check that the program interprets correctly without LUTs
              (PlacedIRCloseSequentialCycles andThen AtomicInterpreter)(
                progLowered
              )

              val progWithLuts = CustomLutInsertion(progLowered)

              // Sanity check that the program interprets correctly with LUTs.
              (PlacedIRCloseSequentialCycles andThen AtomicInterpreter)(
                progWithLuts
              )

              // Since processes might be split, we need to insert Send instructions for correctness.
              val progWithLutsDce =
                PlacedIRDeadCodeElimination(progWithLuts)

              // Sanity check that the program interprets correctly without LUTs
              (PlacedIRCloseSequentialCycles andThen AtomicInterpreter)(
                progWithLutsDce
              )

              def computeVirtualCycle(prog: DefProgram): Int = {
                prog.processes.map(proc => proc.body.length).max
              }

              def computePercentDelta(before: Int, after: Int): String = {
                val delta = (after - before).toDouble
                val absPercentDelta = (math.abs(delta) / before) * 100
                val sign = if (delta > 0) "+" else "-"
                s"${sign} %.2f %%".format(absPercentDelta)
              }

              val vCyclesBefore = computeVirtualCycle(progLowered)
              val vCyclesAfter = computeVirtualCycle(progWithLutsDce)
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
