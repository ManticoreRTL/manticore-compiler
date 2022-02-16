package manticore.compiler.assembly.levels.unconstrained

import manticore.compiler.UnitTest

import manticore.compiler.assembly.parser.UnconstrainedAssemblyParser
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR._
import manticore.compiler.assembly.levels.UnconstrainedAssemblyParserTester
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.parser.AssemblyParser

import scala.annotation.tailrec
import manticore.compiler.CompilationFailureException

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
        .const true 1 1
        .const false 1 0
        @TRAP [type = "\\fail"]
        EXPECT first, second, ["failed"];
        @TRAP [type = "\\stop"]
        EXPECT true, false, ["stop"];
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
        .const true 1 1
        .const false 1 0
        @TRAP [type = "\\fail"]
        EXPECT first, second, ["failed"];
        @TRAP [type = "\\stop"]
        EXPECT true, false, ["stop"];
    """
  }





  // do not move the passing test downwards, otherwise the checker will fail
  // unless the logger errors are cleared
  it should "not fail any expect instructions" taggedAs Tags.WidthConversion in { f =>

    for (ix <- 0 until 20) {
      println("Generating passing program")
      val passing = passingProgram()
      println(passing)
      val parsed = AssemblyParser(passing, f.ctx)
      backend(parsed, f.ctx)
    }
  }

  it should "catch expect failures" taggedAs Tags.WidthConversion in { f =>

    for (ix <- 0 until 20) {
      println("Generating failing program")
      val failing = failingProgram()

      val parsed = AssemblyParser(failing, f.ctx)
      assertThrows[CompilationFailureException] {
        backend(parsed, f.ctx)

      }
    }
  }

}
