package manticore.compiler.integration.yosys.unit.tiny

import manticore.compiler.UnitFixtureTest
import manticore.compiler.integration.yosys.unit.CodeText
import manticore.compiler.integration.yosys.unit.YosysUnitTest

class UnaryOpsTester extends UnitFixtureTest {

  behavior of "Yosys' unary operators"

  val randGen = new scala.util.Random(0) // hard code seed to make the tests
  // reproducible

  val maxWidth = 72
  def generateRandomCode(
      operation: String,
      widthIn: Int,
      widthOut: Int
  ): CodeText = {
    val text = s"""|
                      |module UnaryOpTestCase(
                      |    input wire [$widthIn - 1 : 0] a,
                      |    output wire [$widthOut - 1 : 0] u_b,
                      |    output wire [$widthOut - 1 : 0] s_b);
                      |    assign u_b = $operation a;
                      |    assign s_b = $operation $$signed(a);
                      |endmodule
                      |""".stripMargin

    CodeText(text)
  }
  def generateRandomConditionalCode(widthIn: Int, widthOut: Int): CodeText = {

    val text = s"""|
                       |module UnaryOpTestCase(
                       |    input wire [$widthIn - 1   : 0] a,
                       |    output wire [0             : 0] logic_not_bit,
                       |    output wire [$widthOut - 1 : 0] logic_not_word,
                       |    output wire [0             : 0] reduce_bool_bit,
                       |    output wire [$widthOut - 1 : 0] reduce_bool_word);
                       |    assign logic_not_bit = !a ? a : ~a;
                       |    assign logic_not_word = !a ? a : ~a;
                       |    assign reduce_bool_bit = a ? a : ~a;
                       |    assign reduce_bool_word = a ? a : ~a;
                       |endmodule
                       |""".stripMargin
    CodeText(text)
  }

  val operations = Seq(
    "~", // $not
    "+", // $pos
    "-", // $neg
    "&", // $reduce_and
    "|", // $reduce_or
    "^", // $reduce_xor
    "~^" // $reduce_xnor
  )

  val randomWidth = (Seq(1, 2, 3, 4, 8, 16, 32, 64) ++ Seq.fill(5) {
    randGen.nextInt(maxWidth)
  }).distinct

  val widthCases = for (w1 <- randomWidth; w2 <- randomWidth) yield { (w1, w2) }
  val testCases1 = for ((w1, w2) <- widthCases; op <- operations) yield {
    (w1, w2, op)
  }
  for (w1 <- widthCases; w2 <- randomWidth; op <- operations) yield {
    (w1, w2, op)
  }

  testCases1.foreach { case (w1, w2, op) =>
    s"signed or unsigned assign b[$w2 - 1 : 0 ] = $op a[$w1 - 1 : 0]" should "match verilator results" in {
      f =>
        new YosysUnitTest {
          val testIterations = 64
          val code = generateRandomCode(op, w1, w2)
          val testDir = f.test_dir
        }.run()

    }

  }

  widthCases.foreach { case (w1, w2) =>
    s"signed or unsigned inline switch _[$w1 - 1 : 0] = !_[$w2 - 1 : 0] ? _ : _" should "match verilator results" in {

      f =>
        new YosysUnitTest {
          val testIterations = 64
          val code = generateRandomConditionalCode(w1, w2)
          val testDir = f.test_dir

        }.run()
    }

  }

}
