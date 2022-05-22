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
    Xormix32,
  )

  val dims = Seq(
    (1, 1),
    (4, 4),
  )

  val numLutInputs = Seq(
    2,
    3,
    4,
    5,
    6,
  )

  val shareCustomFuncs = Seq(
    false,
    true
  )

  val lowerCompiler = ManticorePasses.frontend followedBy
    ManticorePasses.middleend followedBy
    UnconstrainedToPlacedTransform followedBy
    PlacedIRConstantFolding followedBy
    PlacedIRCommonSubExpressionElimination followedBy
    ManticorePasses.ExtractParallelism followedBy
    SendInsertionTransform

  sources.foreach { source =>
    dims.foreach { case (dimx, dimy) =>
      numLutInputs.foreach { numCustomInstrInputs =>
        shareCustomFuncs.foreach { shareLuts =>

          it should s"reduce virtual cycle lengths for (${source.name}, ${dimx} x ${dimy}, ${numCustomInstrInputs}-LUT, shareLuts = ${shareLuts})" in { f =>

            implicit val ctx = AssemblyContext(
              dump_all = true,
              dump_dir = Some(f.test_dir.toFile()),
              debug_message = true,
              max_custom_instructions = 32,
              max_custom_instruction_inputs = numCustomInstrInputs,
              optimize_common_custom_functions = shareLuts,
              max_dimx = dimx,
              max_dimy = dimy,
              max_cycles = 200,
              log_file = Some(f.test_dir.resolve("output.log").toFile())
            )

            val progOrig = AssemblyParser(source(f), ctx)
            val progLowered = lowerCompiler(progOrig, ctx)._1

            // Sanity check that the program interprets correctly without LUTs
            (PlacedIRCloseSequentialCycles followedBy AtomicInterpreter)(progLowered, ctx)

            // val progWithLuts = (CustomLutInsertion followedBy PlacedIRCloseSequentialCycles)(progLowered, ctx)._1
            // val monitor = PlacedIRInterpreterMonitor(progWithLuts)
            // val interp = AtomicInterpreter.instance(
            //   program = progWithLuts,
            //   monitor = Some(monitor)
            // )
            // val keys = monitor.keys.toSeq.sortBy(key => key)
            // val values = MHashMap.empty[(Int, String), BigInt]

            // for { cycle <- 0 until 55 } {
            //   println(s"vCycle = ${cycle}")
            //   val traps = interp.interpretVirtualCycle()
            //   keys.foreach { key =>
            //     val value = monitor.read(key)
            //     values += (cycle, key) -> value
            //   }

            //   if (cycle > 0) {
            //     // Check that something has changed between this and the previous cycle.
            //     val prevCycleValues = keys.map(k => k -> values((cycle - 1, k)))
            //     val currCycleValues = keys.map(k => k -> values((cycle, k)))
            //     if (prevCycleValues == currCycleValues) {
            //       println(s"State has not changed between cycle ${cycle-1} and ${cycle}!")
            //     }
            //   }
            // }

            val progWithLuts = CustomLutInsertion(progLowered, ctx)._1

            // Sanity check that the program interprets correctly with LUTs.
            (PlacedIRCloseSequentialCycles followedBy AtomicInterpreter)(progWithLuts, ctx)

            // Since processes might be split, we need to insert Send instructions for correctness.
            val progWithLutsDce = PlacedIRDeadCodeElimination(progWithLuts, ctx)._1

            // Sanity check that the program interprets correctly without LUTs
            (PlacedIRCloseSequentialCycles followedBy AtomicInterpreter)(progWithLutsDce, ctx)

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
            val percentDeltaStr = computePercentDelta(vCyclesBefore, vCyclesAfter)

            println(s"${source.name}, ${dimx} x ${dimy}, ${numCustomInstrInputs}-LUT, share LUTs = ${shareLuts}, vCycle ${vCyclesBefore} -> ${vCyclesAfter} (${percentDeltaStr})")
            // assert(vCyclesBefore >= vCyclesAfter)

            ctx.logger.dumpArtifact("stats.yaml") {
              ctx.stats.asYaml
            }(new HasLoggerId {val id = "stats_file"})
          }
        }
      }
    }
  }
}
