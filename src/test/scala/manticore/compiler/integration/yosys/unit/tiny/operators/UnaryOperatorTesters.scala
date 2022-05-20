package manticore.compiler.integration.yosys.unit.tiny.operators

import manticore.compiler.integration.yosys.unit.CodeText
import manticore.compiler.integration.yosys.unit.YosysUnitTest

class UnaryNotTester extends UnaryOperatorTestGenerator {
  override def operator = "~"
}

class UnaryPositiveTester extends UnaryOperatorTestGenerator {
  override def operator = "+"
}

class UnaryNegativeTester extends UnaryOperatorTestGenerator {
  override def operator = "-"
}

class UnaryReduceAndTester extends UnaryOperatorTestGenerator {
  override def operator = "&"
}

class UnaryReduceOrTester extends UnaryOperatorTestGenerator {
  override def operator = "|"
}

class UnaryReduceXorTester extends UnaryOperatorTestGenerator {
  override def operator = "^"
}

class UnaryReduceXnorTester extends UnaryOperatorTestGenerator {
  override def operator = "~^"
}

class UnaryConditionalOperatorsTester extends UnaryOperatorTestGeneratorBase {

  override def operator: String = "conditional operators"

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
