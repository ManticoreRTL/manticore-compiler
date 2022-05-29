package manticore.compiler.integration.yosys.micro

import scala.collection.mutable.ArrayBuffer

final class SimpleArrayMultiplierBench extends MicroBench {

  case class TestConfig(width: Int, testSize: Int)

  override def benchName: String = "Simple array multiplier"

  override def resource: String = "integration/yosys/micro/array_mult.sv"

  override def testBench(config: TestConfig): String = {

    assert(config.width <= 64)
    val maxOperandValue = (BigInt(1) << config.width) - 1
    val xValues = Seq.fill(config.testSize) {
      maxOperandValue & randGen.nextLong()
    }
    val yValues = Seq.fill(config.testSize) {
      maxOperandValue & randGen.nextLong()
    }
    val pValues = xValues zip yValues map { case (x, y) =>
      (x * y) & ((BigInt(1) << (config.width * 2)) - 1)
    }
    def mkInit(name: String, values: Seq[BigInt]): String = values.zipWithIndex
      .map { case (v, ix) =>
        s"\t\t${name}[$ix] = $v;"
      }
      .mkString("\n")
    val tb = s"""|
                 |module Main (
                 |    input wire clock
                 |);
                 |
                 |
                 |  localparam W = ${config.width};
                 |  localparam TEST_SIZE = ${config.testSize};
                 |
                 |  logic [    W - 1 : 0] x_rom        [0 : TEST_SIZE - 1];
                 |  logic [    W - 1 : 0] y_rom        [0 : TEST_SIZE - 1];
                 |  logic [2 * W - 1 : 0] p_rom        [0 : TEST_SIZE - 1];
                 |
                 |  wire  [    W - 1 : 0] x_val;
                 |  wire  [    W - 1 : 0] y_val;
                 |  wire  [2 * W - 1 : 0] p_val;
                 |
                 |  wire                  done;
                 |  logic [       15 : 0] icounter = 0;
                 |  logic [       15 : 0] ocounter = 0;
                 |  ArrayMultiplier #(
                 |      .WIDTH(W)
                 |  ) dut (
                 |      .clock(clock),
                 |      .X(x_val),
                 |      .Y(y_val),
                 |      .P(p_val),
                 |      .start(1'b1),
                 |      .done(done)
                 |  );
                 |
                 |  always_ff @(posedge clock) begin
                 |    if (icounter < TEST_SIZE - 1) begin
                 |      icounter <= icounter + 1;
                 |    end
                 |    if (done) begin
                 |      ocounter <= ocounter + 1;
                 |      if (p_val != p_rom[ocounter]) begin
                 |        $$display("@ %d Expected %d * %d = %d but got %d", ocounter, x_rom[ocounter],
                 |                 y_rom[ocounter], p_rom[ocounter], p_val);
                 |        $$stop;
                 |      end
                 |    end
                 |    if (ocounter == TEST_SIZE - 1) begin
                 |      $$display("@ %d Finished!", ocounter);
                 |      $$finish;
                 |    end
                 |  end
                 |
                 |  assign x_val = x_rom[icounter];
                 |  assign y_val = y_rom[icounter];
                 |  initial begin
                 |      ${mkInit("x_rom", xValues)}
                 |      ${mkInit("y_rom", yValues)}
                 |      ${mkInit("p_rom", pValues)}
                 |  end
                 |
                 |endmodule""".stripMargin
    tb
  }

  override def outputReference(config: TestConfig): ArrayBuffer[String] = ArrayBuffer(
      f"@ ${config.testSize - 1}%5d Finished!"
  )


  testCase("8-bit multiplier with random inputs", TestConfig(8, 200))
  testCase("16-bit multiplier with random inputs", TestConfig(16, 200))
  testCase("32-bit multiplier with random inputs", TestConfig(32, 200))

}
