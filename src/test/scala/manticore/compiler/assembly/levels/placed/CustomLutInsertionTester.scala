package manticore.compiler.assembly.levels.placed

import manticore.compiler.UnitTest

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedDeadCodeElimination
import manticore.compiler.ManticorePasses
import java.nio.file.Paths
import java.nio.file.Files
import manticore.compiler.UnitFixtureTest
import manticore.compiler.ManticorePasses

class CustomLutInsertionTester extends UnitFixtureTest {

  import manticore.compiler.assembly.levels.placed.PlacedIR._

  behavior of "custom lut insertion transform"

  it should "correctly identify LUTs" in { f =>
    val ctx = AssemblyContext(
      dump_all = true,
      dump_dir = Some(f.test_dir.toFile()),
      debug_message = false,
      max_custom_instructions = 32,
      max_custom_instruction_inputs = 2,
      max_dimx = 4,
      max_dimy = 4,
      log_file = Some(f.test_dir.resolve("output.log").toFile())
    )

    val lowerCompiler = ManticorePasses.frontend followedBy
      ManticorePasses.middleend followedBy
      UnconstrainedToPlacedTransform followedBy
      PlacedIRConstantFolding followedBy
      PlacedIRCommonSubexpressionElimination followedBy
      ManticorePasses.ExtractParallelism

    // val benchmarkName = "swizzle"
    val benchmarkName = "xormix32"

    // Lower the input file.
    val inputStr = scala.io.Source.fromResource(s"levels/placed/${benchmarkName}.masm").getLines().mkString("\n")
    val inputLoweredProg = lowerCompiler(AssemblyParser(inputStr, ctx), ctx)._1

    // Write the lowered file so we can look at the
    val inputLoweredPath = f.test_dir.resolve(s"${benchmarkName}_lowered.masm")
    Files.writeString(inputLoweredPath, inputLoweredProg.serialized)

    val withLutsProg = CustomLutInsertion(inputLoweredProg, ctx)._1

    // println(withLutsProg)

    // inputLowered shouldBe gotOutput
  }

}
