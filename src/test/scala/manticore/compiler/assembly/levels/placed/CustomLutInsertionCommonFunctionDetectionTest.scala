package manticore.compiler.assembly.levels.placed

import java.nio.file.Path
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.utils.XorShift128
import manticore.compiler.UnitFixtureTest
import manticore.compiler.UnitTestMatchers
import manticore.compiler.ManticorePasses
import manticore.compiler.assembly.levels.placed.UnconstrainedToPlacedTransform
import manticore.compiler.assembly.levels.placed.interpreter.PlacedIRInterpreterMonitor
import manticore.compiler.assembly.levels.placed.PlacedIRDebugSymbolRenamer
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.assembly.levels.placed.interpreter.AtomicInterpreter
import manticore.compiler.assembly.levels.placed.PlacedIRCloseSequentialCycles
import manticore.compiler.assembly.utils.XorReduce
import manticore.compiler.DefaultHardwareConfig

class CustomLutInsertionCommonFunctionDetectionTest extends UnitFixtureTest with UnitTestMatchers {

  behavior of "custom LUT insertion transform"

  it should "detect common custom functions" in { fixture =>
    implicit val ctx = AssemblyContext(
      dump_all = true,
      dump_dir = Some(fixture.test_dir.toFile()),
      debug_message = true,
      hw_config = DefaultHardwareConfig(dimX = 1, dimY = 1, nCustomFunctions = 32, nCfuInputs = 6),
      optimize_common_custom_functions = true,
      max_cycles = 200,
      log_file = Some(fixture.test_dir.resolve("output.log").toFile())
    )

    val lowerCompiler =
      AssemblyParser andThen
      ManticorePasses.frontend andThen
      ManticorePasses.middleend andThen
      UnconstrainedToPlacedTransform andThen
      PlacedIRConstantFolding andThen
      PlacedIRCommonSubExpressionElimination

    val lutCompiler =
      CustomLutInsertion

    val prog = XorReduce(fixture)

    val lowered = lowerCompiler(prog)

    fixture.dump(
      s"before_luts_human.masm",
      PlacedIRDebugSymbolRenamer.makeHumanReadable(lowered).serialized
    )

    val loweredWithLuts = lutCompiler(lowered)

    fixture.dump(
      s"after_luts_human.masm",
      PlacedIRDebugSymbolRenamer.makeHumanReadable(loweredWithLuts).serialized
    )

    withClue("There should only be 1 custom function in the final circuit as all functions implement the same body:") {
      val proc = loweredWithLuts.processes.head
      proc.functions.size shouldBe 1
    }

    // Interpret the optimized program to ensure it does not fail.
    // If it crashes, then the program is incorrect (as the previous lowered program
    // is correct if we reached this point)

    // Note that we must close sequential cycles before running the interpreter as we have not
    // scheduled the code and sequential cycles are open (InputType registers never get updated
    // between cycles).
    (PlacedIRCloseSequentialCycles andThen AtomicInterpreter)(loweredWithLuts)
  }
}
