package manticore.compiler.integration.yosys.micro

import manticore.compiler.FileDescriptor
import manticore.compiler.WithInlineVerilog
import manticore.compiler.WithResource

import scala.collection.mutable.ArrayBuffer

final class RiscVMiniBench extends MicroBench {

  case class TestConfig(program: String, timeout: Int) {
      override def toString: String = program.split("/").last
  }
  override def benchName: String = "RISCV MINI"

  override def verilogSources(cfg: TestConfig): Seq[FileDescriptor] = Seq(
    WithResource("integration/yosys/micro/riscv-mini.v")
  )

  override def hexSources(cfg: TestConfig): Seq[FileDescriptor] = Seq.empty

  override def testBench(cfg: TestConfig): FileDescriptor = {

    WithInlineVerilog(
      s"""|
          |module Main(input wire clk);
          |
          |
          |    wire clock = clk;
          |    reg reset = 0;
          |    wire [31:0] tohost;
          |
          |
          |    localparam TIMEOUT = ${cfg.timeout};
          |    TileWithMemory dut(
          |        .clock(clock),
          |        .reset(reset),
          |        .io_fromhost_valid(0),
          |        .io_fromhost_bits(0),
          |        .io_tohost(tohost)
          |    );
          |
          |
          |    reg [31:0] cycle_counter = 0;
          |
          |    always @(posedge clock) begin
          |        cycle_counter <= cycle_counter + 1;
          |        if (cycle_counter < 5) begin
          |            reset = 1;
          |        end else begin
          |            reset = 0;
          |            if (tohost > 1) begin
          |                $$stop;
          |            end else if (tohost == 1) begin
          |                $$finish;
          |            end
          |            if (cycle_counter >= TIMEOUT) begin
          |                $$finish;
          |            end
          |        end
          |    end
          |
          |endmodule
          |
          |""".stripMargin
    )
  }

  override def outputReference(testSize: TestConfig): ArrayBuffer[String] = ArrayBuffer.empty


  testCase("no program", TestConfig("<NONE>", 100))




}