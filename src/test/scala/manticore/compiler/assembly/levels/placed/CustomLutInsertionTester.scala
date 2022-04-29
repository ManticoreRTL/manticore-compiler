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

class CustomLutInsertionTester extends UnitFixtureTest with UnitTestMatchers {

  import manticore.compiler.assembly.levels.placed.PlacedIR._

  behavior of "custom lut insertion transform"

  val sources = Seq(
    Mips32Circuit,
    PicoRv32Circuit,
    // Swizzle,
    Xormix32,
  )

  val dims = Seq(
    (1, 1),
    (4, 4),
  )

  val numLutInputs = Seq(
    2,
    4,
    6,
  )

  val lowerCompiler = ManticorePasses.frontend followedBy
    ManticorePasses.middleend followedBy
    UnconstrainedToPlacedTransform followedBy
    PlacedIRConstantFolding followedBy
    PlacedIRCommonSubExpressionElimination followedBy
    ManticorePasses.ExtractParallelism

  sources.foreach { source =>
    dims.foreach { case (dimx, dimy) =>
      numLutInputs.foreach { numCustomInstrInputs =>

        it should s"reduce virtual cycle lengths for (${source.name}, ${dimx} x ${dimy}, ${numCustomInstrInputs}-LUT)" in { f =>


          val ctx = AssemblyContext(
            dump_all = false,
            dump_dir = Some(f.test_dir.toFile()),
            debug_message = true,
            max_custom_instructions = 32,
            max_custom_instruction_inputs = numCustomInstrInputs,
            max_dimx = dimx,
            max_dimy = dimy,
            max_cycles = 200,
            log_file = Some(f.test_dir.resolve("output.log").toFile())
          )

          val progOrig = AssemblyParser(source(f), ctx)
          val progLowered = lowerCompiler(progOrig, ctx)._1
          val progWithLuts = CustomLutInsertion(progLowered, ctx)._1
          // Since processes might be split, we need to insert Send instructions
          // for correctness.
          val progWithLutsDce = (SendInsertionTransform followedBy PlacedIRDeadCodeElimination)(progWithLuts, ctx)._1

          def computeVirtualCycle(prog: DefProgram): Int = {
            prog.processes.map(proc => proc.body.length).max
          }

          val vCyclesBefore = computeVirtualCycle(progLowered)
          val vCyclesAfter = computeVirtualCycle(progWithLutsDce)

          println(s"${source.name}, ${dimx} x ${dimy}, ${numCustomInstrInputs}-LUT, vCycle ${vCyclesBefore} -> ${vCyclesAfter}")

          assert(vCyclesBefore > vCyclesAfter)
          // This is supposed to change, but since we are not scheduling the code
          // sequential cycles are open and InputType registers never get updated
          // unless we close them.
          (PlacedIRCloseSequentialCycles followedBy AtomicInterpreter)(progWithLutsDce, ctx)

        }

      }
    }
  }
}
