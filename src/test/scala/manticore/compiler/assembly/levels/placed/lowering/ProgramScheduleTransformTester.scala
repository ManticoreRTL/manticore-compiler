package manticore.compiler.assembly.levels.placed.lowering

import manticore.compiler.UnitFixtureTest
import manticore.compiler.UnitTestMatchers
import manticore.compiler.assembly.utils.XorShift16
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedNameChecker
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedRenameVariables
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedOrderInstructions
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedMakeDebugSymbols
import manticore.compiler.assembly.levels.placed.UnconstrainedToPlacedTransform
import manticore.compiler.assembly.levels.placed.PlacedIRCloseSequentialCycles
import manticore.compiler.assembly.levels.placed.interpreter.PlacedIRInterpreterMonitor
import manticore.compiler.assembly.levels.placed.PlacedIRDebugSymbolRenamer
import manticore.compiler.assembly.levels.placed.interpreter.AtomicInterpreter
import manticore.compiler.AssemblyContext

class ProgramScheduleTransformTester
    extends UnitFixtureTest
    with UnitTestMatchers {

  behavior of "ProgramSchedulerTransformTester"

  "Scheduler" should "insert Nops" in { fixture =>
    val randGen = XorShift16("rand")

    val text = s"""
        .prog:
            .proc p0:
                ${randGen.registers}
                @TRACK [name = "somewire"]
                .wire mywire 16

                ${randGen.code}

                MOV mywire, ${randGen.randNext};

        """
    implicit val ctx = AssemblyContext(
      output_dir = Some(fixture.test_dir.resolve("out").toFile()),
      dump_all = true,
      dump_dir = Some(fixture.test_dir.resolve("dumps").toFile()),
      expected_cycles = Some(100),
      debug_message = true
      // log_file = Some(fixture.test_dir.resolve("run.log").toFile())
    )
    val parsed = AssemblyParser(text, ctx)
    val compiler = UnconstrainedNameChecker followedBy
      UnconstrainedMakeDebugSymbols followedBy
      UnconstrainedRenameVariables followedBy
      UnconstrainedOrderInstructions followedBy
      UnconstrainedToPlacedTransform followedBy
      PlacedIRCloseSequentialCycles followedBy
      ProgramSchedulingTransform followedBy
      PlacedIRCloseSequentialCycles

    val compiled = compiler(parsed, ctx)._1

    val monitor = PlacedIRInterpreterMonitor(
      compiled
    )(ctx)
    // println(s"Watching ${monitor.keys}")
    fixture.dump(
      "human_readable.masm",
      PlacedIRDebugSymbolRenamer.makeHumanReadable(compiled)(ctx).serialized
    )
    val interp = AtomicInterpreter.instance(
      program = compiled,
      monitor = Some(monitor)
    )(ctx)

    for (cycle <- 0 until 3000) {

      interp.interpretVirtualCycle() shouldBe Nil
      withClue(s"@${cycle} result mismatch:") {
        monitor.read(randGen.randNext).toInt shouldBe randGen.nextRef().toInt
      }

    }

  }

}
