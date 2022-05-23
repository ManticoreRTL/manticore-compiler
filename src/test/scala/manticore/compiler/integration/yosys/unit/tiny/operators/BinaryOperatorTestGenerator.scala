package manticore.compiler.integration.yosys.unit.tiny.operators

import manticore.compiler.UnitFixtureTest
import manticore.compiler.integration.yosys.unit.CodeText
import manticore.compiler.integration.yosys.unit.YosysUnitTest
import org.scalatest.CancelAfterFailure

trait BinaryOperatorTestGenerator extends UnitFixtureTest with CancelAfterFailure {

  def operator: String

  behavior of s"Yosys' $operator "

  val randGen =
    new scala.util.Random(
      0
    ) // set the seed to get reproducible results/failures

  def maxWidth = 74

  def maxShiftBits: Option[Int] = None

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


  val randomWidth = (Seq(1, 2, 3, 4, 15, 16, 17) ++ Seq.fill(5) {
    randGen.nextInt(maxWidth)
  }).distinct
  def testCases =
    (for (
      w1 <- randomWidth; w2 <- randomWidth; w3 <- randomWidth
    ) yield {
      maxShiftBits match {
        case None => (w1, w2, w3)
        case Some(m) =>
          if (w2 > m) (w1, m, w3)
          else        (w1, w2, w3)
      }
    }).distinct

  testCases.foreach { case (w1, w2, w3) =>

    s"signed or unsigned assign c[$w3 - 1 : 0] = a[$w1 - 1 : 0] $operator b[$w2 - 1 : 0]" should "match verilator results" in { f =>

        new YosysUnitTest {
            val testIterations = 128
            val code = generateRandom(operator, w1, w2, w3)
            val testDir = f.test_dir
        }.run()

    }

  }

}





