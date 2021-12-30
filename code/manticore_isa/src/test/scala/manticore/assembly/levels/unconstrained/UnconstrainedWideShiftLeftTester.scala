package manticore.assembly.levels.unconstrained

import manticore.UnitTest

import manticore.assembly.parser.UnconstrainedAssemblyParser
import manticore.assembly.levels.unconstrained.UnconstrainedIR._
import manticore.assembly.levels.UnconstrainedAssemblyParserTester
import manticore.compiler.AssemblyContext
import manticore.assembly.parser.AssemblyParser
class UnconstrainedWideShiftLeftTester extends UnitTest {

  behavior of "Unconstrained wide SLL conversion"

  val randgen = new scala.util.Random()
  def mkProgram() = {
    // val width = 32
    // require(width <= 32)
    val rs_init_val = 1.toLong

    s"""
    .prog:
    .proc proc_0_0:
    .input rs 32
    ${Range(0, 17).map { i =>
      val shifted_val = rs_init_val << i.toLong
      val binstring = String
        .format("%32s", shifted_val.toInt.toBinaryString)
        .replace(" ", "0")
      s"@DEBUGSYMBOL [symbol = \"rs_${i}\"]\n" +
      s".const rs_${i} 32 ${shifted_val} \t\t\t\t// 0b${binstring}"
    } mkString ("\n")}
    @DEBUGSYMBOL [symbol="sh"]
    .wire sh 5
    .const const_1 5 1
    .const const_0 32 0
    .const const_0_sh 5 0
    @DEBUGSYMBOL [symbol="result"]
    .output result 32
    // set the initial value of rs
    ADD rs, rs_0, const_0;
    // init shift amount to 0
    ADD sh, sh, const_0_sh;
    ${Range(0, 17).map { ix =>
      Seq(
        s"SLL result, rs, sh;",
        s"EXPECT result, rs_${ix}, [\"e_${ix}\"];",
        s"ADD sh, sh, const_1;"
      ) mkString ("\n")
    } mkString ("\n")}
    """
  }

  val dump_path = createDumpDirectory()
  val ctx = AssemblyContext(dump_all = true, dump_dir = Some(dump_path.toFile))

  val interpreter = UnconstrainedInterpreter
  val backend = UnconstrainedBigIntTo16BitsTransform followedBy UnconstrainedInterpreter
  it should "correctly translate wide SLL operations to 16-bit SLLs" in {

    println(mkProgram())
    val program = AssemblyParser(mkProgram(), ctx)

    interpreter.apply(program, ctx)

    backend.apply(program, ctx)


  }
}
