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

class CustomLutInsertionTester extends UnitFixtureTest {

  import manticore.compiler.assembly.levels.placed.PlacedIR._

  behavior of "custom lut insertion transform"

  it should "correctly identify LUTs" in { f =>
    val ctx = AssemblyContext(
      dump_all = true,
      dump_dir = Some(f.test_dir.toFile()),
      debug_message = true,
      log_file = Some(f.test_dir.resolve("output.log").toFile())
    )

    val lowerCompiler = ManticorePasses.frontend followedBy
      ManticorePasses.middleend followedBy
      UnconstrainedToPlacedTransform followedBy
      PlacedIRConstantFolding followedBy
      PlacedIRCommonSubexpressionElimination

    // Lower the input file.
    val inputStr = scala.io.Source.fromResource("levels/placed/swizzle.masm").getLines().mkString("\n")
    val inputLoweredProg = lowerCompiler(AssemblyParser(inputStr, ctx), ctx)._1

    // Write the lowered file so we can look at the
    val inputLoweredPath = f.test_dir.resolve("swizzle_lowered.masm")
    Files.writeString(inputLoweredPath, inputLoweredProg.serialized)

    val withLutsProg = CustomLutInsertion(inputLoweredProg, ctx)._1

    // println(withLutsProg)

    // inputLowered shouldBe gotOutput
  }

}
