package manticore.compiler.integration.yosys.micro

import manticore.compiler.AssemblyContext
import manticore.compiler.FileDescriptor
import manticore.compiler.WithInlineVerilog
import manticore.compiler.WithResource
import manticore.compiler.integration.yosys.unit.YosysUnitTest

import java.io.PrintWriter
import java.nio.file.Files
import scala.collection.mutable.ArrayBuffer

final class PicoRv32ArrayBench extends MicroBench {

  case class TestConfig(numCycles: Int, numCores: Int) {
    override def toString = s"cycles ${numCycles} cores: ${numCores}"
  }
  override def benchName: String = "Replicated Array of PicoRV32 processors"

  override def verilogSources: Seq[FileDescriptor] = Seq(
    WithResource("integration/yosys/micro/picorv32.v")
  )

  override def testBench(cfg: TestConfig): FileDescriptor = {

    WithInlineVerilog(
      s"""|
          |module Main_inner(input wire clk, input wire resetn);
          |
          |
          | wire clock = clk;
          |	reg [31:0] expected = 0;
          |	wire trap;
          |
          |
          |	wire mem_valid;
          |	wire mem_instr;
          |	reg mem_ready;
          |	wire [31:0] mem_addr;
          |	wire [31:0] mem_wdata;
          |	wire [3:0] mem_wstrb;
          |	reg  [31:0] mem_rdata;
          |
          |	picorv32 #(
          |		.TWO_CYCLE_ALU(1),
          |		.TWO_CYCLE_COMPARE(1)
          |	) dut (
          |		.clk         (clock        ),
          |		.resetn      (resetn     ),
          |		.trap        (trap       ),
          |		.mem_valid   (mem_valid  ),
          |		.mem_instr   (mem_instr  ),
          |		.mem_ready   (mem_ready  ),
          |		.mem_addr    (mem_addr   ),
          |		.mem_wdata   (mem_wdata  ),
          |		.mem_wstrb   (mem_wstrb  ),
          |		.mem_rdata   (mem_rdata  )
          |	);
          |
          |	reg [31:0] memory [0:255];
          |
          |	initial begin
          |		memory[0] = 32'h3fc00093; //       li      x1,1020
          |		memory[1] = 32'h0000a023; //       sw      x0,0(x1)
          |		memory[2] = 32'h0000a103; // loop: lw      x2,0(x1)
          |		memory[3] = 32'h00110113; //       addi    x2,x2,1
          |		memory[4] = 32'h0020a023; //       sw      x2,0(x1)
          |		memory[5] = 32'hff5ff06f; //       j       <loop>
          |	end
          |
          |
          |
          |	reg [31 : 0] wword;
          |	wire [31 : 0] mem_word_addr;
          |	assign mem_word_addr = mem_addr >> 2;
          |
          |	always @(posedge clock) begin
          |		mem_ready <= 0;
          |		if (mem_valid && !mem_ready) begin
          |			if (mem_addr < 1024) begin
          |				if (mem_addr == 1020 && mem_wstrb == 8'hf) begin
          |					if (mem_wdata != expected) begin
          |						//$$display("@ %d expected %d but got %d", cycle_counter, expected, mem_wdata);
          |						$$stop;
          |					end
          |					expected <= expected + 1;
          |				end
          |				mem_ready <= 1;
          |				mem_rdata <= memory[mem_word_addr];
          |
          |				wword = memory[mem_word_addr];
          |
          |
          |				// NOTE: we can not directly assign to subwords in memory
          |				// because then Yosys will make partial bit enables for
          |				// memwr cells which will make us generate invalid code!
          |				if (mem_wstrb[0]) wword[ 7: 0] = mem_wdata[ 7: 0];
          |				if (mem_wstrb[1]) wword[15: 8] = mem_wdata[15: 8];
          |				if (mem_wstrb[2]) wword[23:16] = mem_wdata[23:16];
          |				if (mem_wstrb[3]) wword[31:24] = mem_wdata[31:24];
          |				// if (|mem_wstrb)
          |				memory[mem_word_addr] <= wword;
          |
          |			end
          |			/* add memory-mapped IO here */
          |		end
          |	end
          |endmodule
          |
          |module Main(input wire clk);
          |
          |
          |	localparam NUM_CYCLES = ${cfg.numCycles};
          |	localparam RESET_CYCLES = 10;
          |
          |	genvar i;
          | reg [31:0] cycle_counter = 0;
          | wire resetn;
          | assign resetn = !(cycle_counter < RESET_CYCLES);
          |
          | always @(posedge clk) begin
          |		cycle_counter <= cycle_counter + 1;
          |		if (cycle_counter == NUM_CYCLES) begin
          |     $$display("Finished after %d cycles", cycle_counter);
          |			$$finish;
          |		end
          |	end
          |
          |	for (i = 0; i < ${cfg.numCores}; i = i + 1) begin
          |		Main_inner dut(clk, resetn);
          |	end
          |
          |endmodule
          |""".stripMargin
    )
  }

  override def timeOut: Int = 10000

  override def outputReference(testSize: TestConfig): ArrayBuffer[String] = {

    val tempDir = Files.createTempDirectory("picorv32_ref")
    val vfile = tempDir.resolve("picorv32_tb.sv")

    val writer = new PrintWriter(vfile.toFile())

    // Read each source and concatenate them together.
    val tb = (verilogSources :+ testBench(testSize))
      .map { res =>
        scala.io.Source.fromFile(res.p.toFile()).getLines().mkString("\n")
      }
      .mkString("\n")

    writer.write(tb)
    writer.flush()
    writer.close()
    implicit val ctx = AssemblyContext(
      log_file = Some(tempDir.resolve("verilator.log").toFile())
    )
    YosysUnitTest.verilate(Seq(vfile), timeOut)
  }


  val config1 = TestConfig(100, 40)
  testCase(config1.toString(), config1)
  // testCase("simple counter on PicoRV32", 100)

}
