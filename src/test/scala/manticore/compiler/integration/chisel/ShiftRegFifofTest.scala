package manticore.compiler.integration.chisel

import manticore.compiler.UnitFixtureTest

import manticore.compiler.integration.chisel.util.ProgramTester
import manticore.compiler.ManticorePasses
import manticore.compiler.AssemblyContext
import manticore.compiler.HasLoggerId
import manticore.compiler.assembly.levels.codegen.MachineCodeGenerator
import manticore.compiler.integration.chisel.util.ProcessorTester
import chiseltest._
import chisel3._
import manticore.machine.xrt.ManticoreFlatSimKernel
import chisel3.internal.prefix
import manticore.compiler.integration.chisel.util.KernelTester
import manticore.compiler.WithInlineVerilog
import manticore.compiler.frontend.yosys.Yosys
import manticore.compiler.frontend.yosys.YosysRunner

class ShiftRegFifofTest extends KernelTester {
  behavior of "ShiftRegFifo"

  override def compiler =

    ManticorePasses.frontend andThen
      ManticorePasses.middleend andThen
      ManticorePasses.backend
  it should "shift values in a shift register using a single core" in {
    fixture =>

      val verilogSource = WithInlineVerilog(
        s"""|
            |
            |module ShiftRegisterFIFO #(
            |    WIDTH = 32,
            |    DEPTH = 3
            |) (
            |    input wire clk,
            |    input wire wen,
            |    input wire [WIDTH - 1 : 0] din,
            |    output wire [WIDTH - 1 : 0] dout
            |
            |);
            |
            |  logic [WIDTH - 1 : 0] regs[0 : DEPTH - 1];
            |  assign dout = regs[DEPTH-1];
            |  int i;
            |  always @(posedge clk) begin
            |    if (wen) begin
            |      regs[0] <= din;
            |      for (i = 1; i < DEPTH; i += 1) begin
            |        regs[i] <= regs[i-1];
            |      end
            |    end
            |  end
            |
            |endmodule
            |
            |module ShiftRegisterFIFOTester (
            |    input wire clk
            |);
            |
            |
            |  localparam WIDTH = 16;
            |  localparam DEPTH = 4;
            |
            |
            |//   logic [WIDTH - 1 : 0] rom[0 : DEPTH - 1];
            |
            |  logic [15 : 0] cycle_counter = 0;
            |
            |  wire reading;
            |  wire [WIDTH - 1 : 0] din = 16'h3456;
            |  wire  [WIDTH - 1 : 0] dout;
            |
            |  ShiftRegisterFIFO #(
            |      .WIDTH(WIDTH),
            |      .DEPTH(DEPTH)
            |  ) fifo (
            |      .clk (clk),
            |      .wen (1'b1),
            |      .din (din),
            |      .dout(dout)
            |  );
            |
            |
            |  always @(posedge clk) begin
            |    if (cycle_counter == DEPTH) begin
            |      if (din != dout) begin
            |          $$stop;
            |      end
            |    end else if (cycle_counter == DEPTH + 1) begin
            |      $$finish;
            |    end
            |    cycle_counter <= cycle_counter + 1;
            |  end
            |
            |
            |endmodule
            |
            |
            |""".stripMargin
      )

      val yosys = Yosys.YosysDefaultPassAggregator andThen YosysRunner

      implicit val context = AssemblyContext(
        output_dir = Some(fixture.test_dir.resolve("out").toFile()),
        max_dimx = 1,
        max_dimy = 1,
        max_registers = 32,
        max_carries = 2,
        dump_all = false,
        debug_message = false,
        dump_dir = Some(fixture.test_dir.resolve("dumps").toFile()),
        expected_cycles = Some(6),
        log_file = Some(fixture.test_dir.resolve("run.log").toFile())
      )

      val source = yosys(Seq(verilogSource.p))
      val text = scala.io.Source.fromFile(source.toFile()).mkString("")

      compileAndRun(text, context)(fixture)

  }

  // it should "shift values in a shift register using 4 cores" in {
  //   fixture =>
  //     val source: String = scala.io.Source
  //       .fromResource(
  //         "integration/microbench/baked_tests/shift_reg_fifo/shift.masm"
  //       )
  //       .mkString("")

  //     val context = AssemblyContext(
  //       output_dir = Some(fixture.test_dir.resolve("out").toFile()),
  //       max_dimx = 2,
  //       max_dimy = 2,
  //       dump_all = true,
  //       dump_dir = Some(fixture.test_dir.resolve("dumps").toFile()),
  //       expected_cycles = Some(6),
  //       log_file = Some(fixture.test_dir.resolve("run.log").toFile())
  //     )

  //     compileAndRun(source, context)(fixture)

  // }

}
