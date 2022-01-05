package manticore.assembly.levels.unconstrained

import manticore.UnitTest

import manticore.assembly.parser.UnconstrainedAssemblyParser
import manticore.assembly.levels.unconstrained.UnconstrainedIR._
import manticore.assembly.levels.UnconstrainedAssemblyParserTester
import manticore.compiler.AssemblyContext
import manticore.assembly.parser.AssemblyParser
import manticore.UnconstrainedTest
import scala.annotation.tailrec
import manticore.assembly.CompilationFailureException

class UnconstrainedWideExpectTester extends UnconstrainedTest {

  behavior of "Unconstrained wide EXPECT conversion"

  val randgen = new scala.util.Random()
  def mkWideRand(w: Int): BigInt = {
    val shorts = Seq.fill((w - 1) / 16 + 1) { randgen.nextInt(1 << 16) }

    val combined = shorts.foldLeft(BigInt(0)) { case (c, x) =>
      (c << 16) | BigInt(x)
    }

    val masked = combined & ((BigInt(1) << w) - 1)
    masked
  }
  def failingProgram() = {
    val width = randgen.nextInt(1024) + 1
    val ref = mkWideRand(width)
    @tailrec
    def mkAnother(): BigInt = {
      val a = mkWideRand(width)
      if (a == ref)
        mkAnother()
      else
        a
    }
    val different = mkAnother()
    s"""
    .prog:
      .proc proc_0_0:
        .const first ${width} ${ref}
        .const second ${width} ${different}
        @TRAP [type = "\\fail"]
        EXPECT first, second, ["failed"];
    """
  }
  def passingProgram() = {
    val width = randgen.nextInt(1024) + 1
    val ref = mkWideRand(width)
    val got = ref
    s"""
    .prog:
      .proc proc_0_0:
        .const first ${width} ${ref}
        .const second ${width} ${got}
        @TRAP [type = "\\fail"]
        EXPECT first, second, ["failed"];
    """
  }

  val dump_path = createDumpDirectory()
  val failing_ctx = AssemblyContext()
  val passing_ctx = AssemblyContext()
  val interpreter = UnconstrainedInterpreter
  val backend =
    UnconstrainedBigIntTo16BitsTransform followedBy UnconstrainedInterpreter

  // do not move the passing test downwards, otherwise the checker will fail
  // unless the logger errors are cleared
  it should "not fail any expect instructions" taggedAs Tags.WidthConversion in {

    for (ix <- 0 until 20) {
      println("Generating passing program")
      val passing = passingProgram()
      println(passing)
      val parsed = AssemblyParser(passing, passing_ctx)
      backend(parsed, passing_ctx)
    }
  }

  it should "catch expect failures" taggedAs Tags.WidthConversion in {

    for (ix <- 0 until 20) {
      println("Generating failing program")
      val failing = failingProgram()
      println(failing)
      val parsed = AssemblyParser(failing, failing_ctx)
      assertThrows[CompilationFailureException] {
        backend(parsed, failing_ctx)

      }
    }
  }

}
