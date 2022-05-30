package manticore.compiler.integration.yosys.micro

import scala.collection.mutable.ArrayBuffer
import manticore.compiler.integration.yosys.unit.YosysUnitTest
import java.nio.file.Files
import java.io.PrintWriter
import manticore.compiler.AssemblyContext

final class Mips32Bench extends MicroBench {

  case class TestConfig(bin: Seq[Int])
  override def benchName: String = "Single-Cycle Mips32"

  override def resource: String = "integration/yosys/micro/mips32.sv"

  override def testBench(cfg: TestConfig): String = {

    s"""|
        |module Main (
        |    input wire clk
        |);
        |
        |  wire clock = clk;
        |  reg  [31:0] inst_mem[255:0];
        |  reg  [15:0] cycle_counter = 0;
        |
        |  always @ (posedge clk) begin
        |     cycle_counter <= cycle_counter + 1;
        |     if (cycle_counter == ${timeOut - 10}) $$finish;
        |  end
        |
        |  wire [31:0] inst;
        |  wire [31:0] addr;
        |  assign inst = inst_mem[addr];
        |  wire halted;
        |  wire reset;
        |  ResetDriver rdriver (
        |      .clock(clock),
        |      .reset(reset)
        |  );
        |  // genvar i;
        |  // for (i = 0; i < 10; i = i + 1) begin
        |
        |    Mips32 dut (
        |        .instr (inst),
        |        .raddr (addr),
        |        .clock (clock),
        |        .reset (reset),
        |        .halted(halted)
        |    );
        |  // end
        |
        |
        |
        |  initial begin
        |   ${cfg.bin.zipWithIndex
      .map { case (v, ix) => s"inst_mem[${ix}] = ${v};" }
      .mkString("\n")}
        |  end
        |
        |
        |endmodule""".stripMargin
  }

  override def outputReference(config: TestConfig): ArrayBuffer[String] = {

    val tempDir = Files.createTempDirectory("mips32_ref")
    val vfile = tempDir.resolve("mips32_tb.sv")

    val writer = new PrintWriter(vfile.toFile())
    val tb = scala.io.Source
      .fromResource(resource)
      .getLines()
      .mkString("\n") + testBench(config)

    writer.write(tb)
    writer.flush()
    writer.close()
    implicit val ctx = AssemblyContext(
      log_file = Some(tempDir.resolve("verilator.log").toFile())
    )
    YosysUnitTest.verilate(Seq(vfile), timeOut)
  }

  val sum1to9 = TestConfig(
    Seq(
      0x631826, 0x2063000a, 0x210826, 0x421026, 0x10230003, 0x411020,
      0x20210001, 0x8000004, 0x40000d, 0x0, 0x0, 0x0, 0x0
    )
  )
  val bubbleSort32 = TestConfig(
    Seq(
      0x1ef7826, 0x21ef0020, 0x1ce7026, 0x21ce001f, 0x210826, 0x102f0005,
      0x1e11022, 0x11880, 0xac620000, 0x20210001, 0x8000005, 0x1ad6826,
      0x21ad0001, 0x18c6026, 0x11ac000f, 0x1ad6826, 210826, 0x102e000b, 0x11080,
      0x8c430000, 0x20440004, 0x8c850000, 0xa3302a, 0x10cc0003, 0xac450000,
      0xac830000, 0x21ad0001, 0x20210001, 0x8000011, 0x800000e, 0x421026,
      0x8c420000, 0x40000d, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0
    )
  )
  override def timeOut: Int = 2000

  // println(outputReference(sum1to9).mkString("\n"))
  testCase(s"adding 1 to 9 should sum up to 45", sum1to9)
  testCase(s"bubble sort on a 32-element array", bubbleSort32)
}
