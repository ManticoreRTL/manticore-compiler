package manticore.compiler.integration.yosys.micro

import manticore.compiler.AssemblyContext
import manticore.compiler.FileDescriptor
import manticore.compiler.WithInlineVerilog
import manticore.compiler.WithResource
import manticore.compiler.integration.yosys.unit.YosysUnitTest
import org.scalatest.CancelAfterFailure

import java.io.PrintWriter
import java.nio.file.Files
import scala.collection.mutable.ArrayBuffer

final class GaussianBlurBench extends MicroBench with CancelAfterFailure {

  case class TestConfig(replications: Int, loops: Int)
  override def benchName: String = "Gaussian Blur Kernel"

  override def verilogSources: Seq[FileDescriptor] = Seq(
    WithResource("integration/yosys/micro/stencil/Gaussian3x3Kernel.sv"),
    WithResource("integration/yosys/micro/stencil/TestBench.sv")
  )

  override def testBench(cfg: TestConfig): FileDescriptor = {

    WithInlineVerilog(
      s"""|
          |module Main(input wire clk);
          |
          |    wire clock = clk;
          |    parameter NUM_LOOPS = ${cfg.loops};
          |
          |
          |    logic [31:0] counter = 0;
          |
          |    always_ff @(posedge clock) begin
          |        counter <= counter + 31'd1;
          |    end
          |
          |    genvar gidx;
          |
          |
          |    generate
          |        for (gidx = 0; gidx < ${cfg.replications}; gidx = gidx + 1) begin
          |            TestBench #(NUM_LOOPS) dut(clock, counter);
          |
          |        end
          |    endgenerate
          |
          |endmodule
          |
          |
          |""".stripMargin
    )
  }

  override def outputReference(config: TestConfig): ArrayBuffer[String] = {
    val tempDir = Files.createTempDirectory("stencil_ref")
    val vfile   = tempDir.resolve("stencil_tb.sv")

    val writer = new PrintWriter(vfile.toFile())

    // Read each source and concatenate them together.
    val tb = (verilogSources :+ testBench(config))
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

  override def timeOut: Int = 10000


  testCase("32 gaussian kernels", TestConfig(32, 1))


}
