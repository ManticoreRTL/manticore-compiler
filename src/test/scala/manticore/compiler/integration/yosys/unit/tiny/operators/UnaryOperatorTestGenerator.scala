package manticore.compiler.integration.yosys.unit.tiny.operators

import manticore.compiler.UnitFixtureTest
import manticore.compiler.integration.yosys.unit.CodeText
import manticore.compiler.integration.yosys.unit.YosysUnitTest
import org.scalatest.CancelAfterFailure

trait UnaryOperatorTestGeneratorBase extends UnitFixtureTest with CancelAfterFailure {
  def operator: String

  behavior of s"Yosys' unary $operator"

  val randGen = new scala.util.Random(0) // hard code seed to make the tests
  // reproducible

  val maxWidth = 72

  val randomWidth = (Seq(1, 2, 3, 4, 8, 16, 32, 64) ++ Seq.fill(5) {
    randGen.nextInt(maxWidth)
  }).distinct

  val widthCases = for (w1 <- randomWidth; w2 <- randomWidth) yield { (w1, w2) }

}

trait UnaryOperatorTestGenerator extends UnaryOperatorTestGeneratorBase {

  def generateRandomCode(
      widthIn: Int,
      widthOut: Int
  ): CodeText = {
    val text = s"""|
                      |module UnaryOpTestCase(
                      |    input wire [$widthIn - 1 : 0] a,
                      |    input wire signed [$widthIn - 1 : 0] sa,
                      |    output wire [$widthOut - 1 : 0] u_b,
                      |    output wire [$widthOut - 1 : 0] s_b,
                      |    output wire [$widthOut - 1 : 0] ub,
                      |    output wire signed [$widthOut - 1 : 0] sb
                      |);
                      |    assign u_b = $operator a;
                      |    assign s_b = $operator $$signed(a);
                      |    assign ub = $operator sa;
                      |    assign sb = $operator sa;
                      |endmodule
                      |""".stripMargin

    CodeText(text)
  }

  widthCases.foreach { case (w1, w2) =>
    s"signed or unsigned assign b[$w2 - 1 : 0 ] = $operator a[$w1 - 1 : 0]" should "match verilator results" in {
      f =>
        new YosysUnitTest {
          val testIterations = 64
          val code = generateRandomCode(w1, w2)
          val testDir = f.test_dir
        }.run()

    }

  }



}

