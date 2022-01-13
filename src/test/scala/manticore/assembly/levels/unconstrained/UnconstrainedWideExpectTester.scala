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

class UnconstrainedWideExpectTester extends UnconstrainedWideTest {

  behavior of "Unconstrained wide EXPECT conversion"


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


  val failing_ctx = AssemblyContext(dump_all = true, dump_dir = Some(createDumpDirectory().toFile()))
  val passing_ctx = AssemblyContext(dump_all = true, dump_dir = Some(createDumpDirectory().toFile()))
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
