package manticore.compiler.integration.yosys.micro

import manticore.compiler.FileDescriptor
import manticore.compiler.WithInlineVerilog
import manticore.compiler.WithResource

import scala.collection.mutable.ArrayBuffer

final class SwizzleBench extends MicroBench {

  type TestConfig = Unit
  override def benchName: String = "swizzle"

  override def verilogSources: Seq[FileDescriptor] = Seq(
    WithResource("integration/yosys/micro/swizzle.sv")
  )

  override def testBench(cfg: TestConfig): FileDescriptor = {

    val inputs = Seq(
      "10000100",
      "01010000",
      "10110001",
      "01100100",
      "10011111",
      "00011001",
      "10010101",
      "10010010",
      "11001001",
      "10110000",
      "11001110",
      "11110110",
      "00000101",
      "00101001",
      "11111011",
      "00010110",
      "01011010",
      "11001101",
      "00111110",
      "11011101",
      "00000110",
      "10101000",
      "11100100",
      "00011011",
      "01011000",
      "00101010",
      "00010111",
      "10000000",
      "11000100",
      "11110000",
      "01010010",
      "01100100",
      "00101101",
      "10001000",
      "11101011",
      "00101111",
      "00111100",
      "10000001",
      "01001001",
      "10101100",
      "00111001",
      "00001110",
      "00110000",
      "11100010",
      "10101100",
      "11110011",
      "00101101",
      "11000111",
      "11100001",
      "11100011"
    )
    val outputs = Seq(
      "00100001",
      "00001010",
      "10001101",
      "00100110",
      "11111001",
      "10011000",
      "10101001",
      "01001001",
      "10010011",
      "00001101",
      "01110011",
      "01101111",
      "10100000",
      "10010100",
      "11011111",
      "01101000",
      "01011010",
      "10110011",
      "01111100",
      "10111011",
      "01100000",
      "00010101",
      "00100111",
      "11011000",
      "00011010",
      "01010100",
      "11101000",
      "00000001",
      "00100011",
      "00001111",
      "01001010",
      "00100110",
      "10110100",
      "00010001",
      "11010111",
      "11110100",
      "00111100",
      "10000001",
      "10010010",
      "00110101",
      "10011100",
      "01110000",
      "00001100",
      "01000111",
      "00110101",
      "11001111",
      "10110100",
      "11100011",
      "10000111",
      "11000111"
    )

    def mkInit(name: String, values: Seq[String]): String = {
      values.zipWithIndex
        .map { case (ln, ix) => s"${name}[${ix}] = 8'b${ln}; " }
        .mkString("\n")
    }

    WithInlineVerilog(
      s"""|
          |module Main (
          |    input wire clock
          |);
          |
          |  localparam W = 8;
          |  localparam TEST_SIZE = 50;
          |
          |  logic [    W - 1 : 0] in_rom        [0 : TEST_SIZE - 1];
          |  logic [    W - 1 : 0] out_rom       [0 : TEST_SIZE - 1];
          |
          |  wire  [    W - 1 : 0] in_val;
          |  wire  [    W - 1 : 0] out_val;
          |  wire  [    W - 1 : 0] out_val_expected;
          |
          |  logic [       15 : 0] counter = 0;
          |
          |  initial begin
          |   ${mkInit("in_rom", inputs)}
          |   ${mkInit("out_rom", outputs)}
          |  end
          |  assign in_val = in_rom[counter];
          |  assign out_val_expected = out_rom[counter];
          |
          |  Swizzle #(
          |      .WIDTH(W)
          |  ) dut (
          |      .IN(in_val),
          |      .OUT(out_val)
          |  );
          |
          |  always_ff @(posedge clock) begin
          |    counter <= counter + 1;
          |    if (out_val != out_val_expected) begin
          |      $$display("[%d] Expected swizzle(%d) = %d but got %d", counter, in_val, out_val_expected, out_val);
          |      $$stop;
          |    end
          |    if (counter == TEST_SIZE - 1) begin
          |      $$display("Finished after %d cycles", counter);
          |      $$finish;
          |    end
          |  end
          |
          |endmodule
          |""".stripMargin
    )
  }

  override def outputReference(testSize: TestConfig): ArrayBuffer[String] =
    ArrayBuffer("Finished after    49 cycles")

  testCase("bit patterns", ())

}
