package manticore.compiler.integration.yosys.micro

import manticore.compiler.FileDescriptor
import manticore.compiler.WithInlineVerilog
import manticore.compiler.WithResource

import scala.collection.mutable.ArrayBuffer

final class XorMix32Bench extends MicroBench {

  type TestConfig = Unit
  override def benchName: String = "XorMix32"

  override def verilogSources(cfg: TestConfig): Seq[FileDescriptor] = Seq(
    WithResource("integration/yosys/micro/xormix32.sv")
  )

  override def hexSources(cfg: TestConfig): Seq[FileDescriptor] = Seq.empty

  override def testBench(cfg: TestConfig): FileDescriptor = {

    val reference = scala.io.Source.fromResource("integration/yosys/micro/xormix32_ref.hex").getLines().zipWithIndex.map {
      case (ln, ix) => s"ref_result[$ix] = 128'h${ln};"
    }.mkString("\n")

    WithInlineVerilog(
      s"""|module Main (
          |    input wire clk
          |);
          |  wire clock = clk;
          |  // configuration
          |  localparam STREAMS = 4;
          |  localparam NUM = 100;
          |  localparam [31 : 0] seed_x = 32'hdf2c403b;
          |  localparam [32 * STREAMS - 1 : 0] seed_y = 128'ha9140006e47066dd25e5a545abac0809;
          |
          |  // reference result
          |  reg [127:0] ref_result [0 : NUM - 1];
          |
          |  initial begin
          |      $reference
          |  end
          |
          |
          |  // DUT signals
          |  wire rst;
          |  wire enable;
          |  wire [32 * STREAMS - 1 : 0] result;
          |  wire [32 * STREAMS - 1 : 0] expected;
          |
          |  // error counter
          |  //   reg [15 : 0] errors = 0;
          |
          |  // DUT
          |  xormix32 #(
          |      .streams(STREAMS)
          |  ) inst_xormix (
          |      .clk(clock),
          |      .rst(rst),
          |      .seed_x(seed_x),
          |      .seed_y(seed_y),
          |      .enable(enable),
          |      .result(result)
          |  );
          |
          |  reg [ 2 : 0] rst_counter = 0;
          |  reg [15 : 0] ictr;
          |
          |  assign expected = ref_result[ictr];
          |  assign rst = (rst_counter < 2);
          |  assign enable = (rst_counter == 3);
          |
          |  always @(posedge clock) begin
          |    if (rst_counter < 3) begin
          |      rst_counter <= rst_counter + 1;
          |      ictr <= 0;
          |    end else if (rst_counter == 3) begin
          |      ictr <= ictr + 1;
          |      if (ictr < NUM) begin
          |        if (result != expected) begin
          |          $$display("Invalid %dth result %d != %d", ictr, result, expected);
          |          $$stop;
          |        end
          |      end else begin
          |        $$display("Finished at %d", ictr);
          |        $$finish;
          |      end
          |    end
          |  end
          |
          |
          |endmodule
          |""".stripMargin
    )
  }

  override def outputReference(testSize: TestConfig): ArrayBuffer[String] =
    ArrayBuffer("Finished at   100")

  testCase("bit patterns", ())

}
