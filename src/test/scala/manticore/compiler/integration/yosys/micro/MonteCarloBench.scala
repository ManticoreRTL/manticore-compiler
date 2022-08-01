package manticore.compiler.integration.yosys.micro

import manticore.compiler.FileDescriptor

import scala.collection.mutable.ArrayBuffer
import manticore.compiler.WithResource
import manticore.compiler.WithInlineVerilog

final class MonteCarloBench extends MicroBench {

  type TestConfig = Unit

  override def benchName: String = "monte-carlo 16x"

  override def verilogSources(cfg: TestConfig): Seq[FileDescriptor] = Seq(
    WithResource("integration/yosys/micro/montecarlo/MonteCarloAccelerator.sv")
  )

  override def hexSources(cfg: TestConfig): Seq[FileDescriptor] = Nil

  override def testBench(cfg: TestConfig): FileDescriptor = WithInlineVerilog(
    """|
       |module Main(input wire clock);
       |
       |
       |    logic [31:0] counter = 0;
       |
       |    always_ff @(posedge clock) counter <= counter + 1;
       |
       |    wire resp_valid;
       |    wire [31:0] resp;
       |    MonteCarloAccelerator dut(
       |        .clock(clock),
       |        .reset(counter < 4),
       |        .io_request_ready(),
       |        .io_request_valid(1'b1),
       |        .io_request_bits_time_steps(32'd100000),
       |        .io_request_bits_coefficient1(32'd1049),
       |        .io_request_bits_coefficient2(32'd210),
       |        .io_request_bits_start_value({1'd1, 20'd0}),
       |        .io_response_ready(1'b1),
       |        .io_response_valid(resp_valid),
       |        .io_response_bits(resp)
       |    );
       |
       |    always_ff @(posedge clock) begin
       |        if (resp_valid) begin
       |            $display("Got %d", resp);
       |        end
       |        if (counter >= 32'd400) $finish;
       |    end
       |
       |
       |endmodule
       |""".stripMargin
  )

  override def outputReference(testSize: TestConfig): ArrayBuffer[String] = ArrayBuffer()


  testCase("monte carlo simulation", ())

}
