package manticore.assembly.levels.unconstrained

import manticore.UnitTest

import manticore.assembly.parser.UnconstrainedAssemblyParser
import manticore.assembly.levels.unconstrained.UnconstrainedIR._
import manticore.assembly.levels.UnconstrainedAssemblyParserTester
import manticore.compiler.AssemblyContext
import manticore.assembly.parser.AssemblyParser
import manticore.UnconstrainedTest

class UnconstrainedWideShiftLeftTester extends UnconstrainedTest {

  behavior of "Unconstrained wide SLL conversion"

  val randgen = new scala.util.Random()
  // a simple program that shifts '1' to the left 0 to 31 times
  def mkProgram() = {
    // val width = 32
    // require(width <= 32)
    val rs_init_val: Long = 1.toLong

    s"""
    .prog:
    .proc proc_0_0:
    .input rs 60
    ${Range(0, 60).map { i =>
      val shifted_val: Long = rs_init_val << (i.toLong)
      val binstring = String
        .format("%60s", shifted_val.toLong.toBinaryString)
        .replace(" ", "0")
      s"@DEBUGSYMBOL [symbol = \"rs_${i}\"]\n" +
        s".const rs_${i} 60 ${shifted_val} \t\t\t\t// 0b${binstring}"
    } mkString ("\n")}
    @DEBUGSYMBOL [symbol="sh"]
    .wire sh 6
    .const const_1 6 1
    .const const_0 60 0
    .const const_32_sh 6 32
    @DEBUGSYMBOL [symbol="result"]
    .output result 60
    // set the initial value of rs
    ADD rs, rs_0, const_0;
    // init shift amount to 0
    ADD sh, sh, const_32_sh;
    ${Range(32, 33).map { ix =>
      Seq(
        s"SLL result, rs, sh;",
        s"@TRAP [type = \"\\fail\"]",
        s"EXPECT result, rs_${ix}, [\"e_${ix}\"];",
        s"ADD sh, sh, const_1;"
      ) mkString ("\n")
    } mkString ("\n")}
    """
  }

  val dump_path = createDumpDirectory()
  val ctx = AssemblyContext(dump_all = true, dump_dir = Some(dump_path.toFile))

  val interpreter = UnconstrainedInterpreter
  val backend =
    UnconstrainedBigIntTo16BitsTransform followedBy UnconstrainedInterpreter
  it should "correctly translate wide SLL operations to 16-bit SLLs" taggedAs Tags.WidthConversion in {
    println(mkProgram())
    val program = AssemblyParser(mkProgram(), ctx)
    backend.apply(program, ctx)

  }
}
