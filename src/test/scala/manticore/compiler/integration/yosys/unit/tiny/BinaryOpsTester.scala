package manticore.compiler.integration.yosys.unit.tiny

import manticore.compiler.UnitFixtureTest
import manticore.compiler.integration.yosys.unit.CodeText
import manticore.compiler.integration.yosys.unit.YosysUnitTest

class BinaryOpsTester extends UnitFixtureTest {

  behavior of "Yosys' binary operators "

  val randGen =
    new scala.util.Random(
      0
    ) // set the seed to get reproducible results/failures

  val maxWidth = 74

  def generateRandom(op: String, widthIn1: Int, widthIn2: Int, widthOut: Int) =
    CodeText {

      s"""|
                       |module BinaryOperatorTestCase(
                       |    input wire [$widthIn1 - 1 : 0] a,
                       |    input wire [$widthIn2 - 1 : 0] b,
                       |    output wire [$widthOut - 1 : 0] c_uu, // unsigned unsigned
                       |    output wire [$widthOut - 1 : 0] c_su, //   signed unsigned
                       |    output wire [$widthOut - 1 : 0] c_us, // unsigned   signed
                       |    output wire [$widthOut - 1 : 0] c_ss  //   signed   signed
                       |);
                       |    assign c_uu = a $op b;
                       |    assign c_su = $$signed(a) $op b;
                       |    assign c_us = a $op $$signed(b);
                       |    assign c_ss = $$signed(a) $op $$signed(b);
                       |endmodule
                       |""".stripMargin

    }

  val operators = Seq(
    "<", // $lt
    // "<=", // $le
    // "==", // $eq
    // "!=", // $ne
    // ">=", // $ge
    // ">", // $gt
    // "+", // $add
    // "-", // $sub
    // "&", // $and
    // "|", // $or
    // "^", // $xor
    // "~^", // $xnor
    // "<<", // $shl
    // ">>", // $shr
    // "<<<", // $sshl
    // ">>>", // $sshr
    // "&&", // $logic_and
    // "||", // $logc_or
    // "===", // $eqx
    // "!===" // $nex
  )
  val randomWidth = (Seq(1, 2, 3, 4, 8) ++ Seq.fill(5) {
    randGen.nextInt(maxWidth)
  }).distinct
  val testCases =
    for (
      w1 <- randomWidth; w2 <- randomWidth; w3 <- randomWidth; op <- operators
    ) yield { (w1, w2, w3, op) }

  testCases.foreach { case (w1, w2, w3, op) =>

    s"signed or unsigned assign c[$w3 - 1 : 0] = a[$w1 - 1 : 0] $op b[$w2 - 1 : 0]" should "match verilator results" in { f =>

        new YosysUnitTest {
            val testIterations = 128
            val code = generateRandom(op, w1, w2, w3)
            val testDir = f.test_dir
        }.run()

    }

  }

}
