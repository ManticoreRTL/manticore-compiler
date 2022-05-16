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

class CustomLutInsertionCommonFunctionDetectionTest extends UnitFixtureTest with UnitTestMatchers {

  behavior of "custom LUT insertion transform"

  it should "detect common custom functions" in { fixture =>
    implicit val ctx = AssemblyContext(
      dump_all = true,
      dump_dir = Some(fixture.test_dir.toFile()),
      debug_message = true,
      max_custom_instructions = 32,
      max_custom_instruction_inputs = 6,
      max_dimx = 1,
      max_dimy = 1,
      max_cycles = 200,
      log_file = Some(fixture.test_dir.resolve("output.log").toFile())
    )

    val lowerCompiler =
      ManticorePasses.frontend followedBy
      ManticorePasses.middleend followedBy
      UnconstrainedToPlacedTransform followedBy
      PlacedIRConstantFolding followedBy
      PlacedIRCommonSubExpressionElimination

    val lutCompiler =
      CustomLutInsertion followedBy
      PlacedIRDeadCodeElimination

    val prog = XorReduce(fixture)
    val parsed = AssemblyParser(prog, ctx)
    val lowered = lowerCompiler(parsed, ctx)._1

    fixture.dump(
      s"before_luts_human.masm",
      PlacedIRDebugSymbolRenamer.makeHumanReadable(lowered).serialized
    )

    val loweredWithLuts = lutCompiler(lowered, ctx)._1

    fixture.dump(
      s"after_luts_human.masm",
      PlacedIRDebugSymbolRenamer.makeHumanReadable(loweredWithLuts).serialized
    )

    withClue("There should only be 1 custom function in the final circuit as all functions implement the same body:") {
      val proc = loweredWithLuts.processes.head
      proc.functions.size shouldBe 1
    }

  }
}
