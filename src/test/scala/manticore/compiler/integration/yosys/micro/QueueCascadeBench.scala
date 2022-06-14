package manticore.compiler.integration.yosys.micro

import scala.collection.mutable.ArrayBuffer
import manticore.compiler.integration.yosys.unit.YosysUnitTest
import java.nio.file.Files
import java.io.PrintWriter
import manticore.compiler.AssemblyContext

final class QueueCascadeBench extends MicroBench {

  case class TestConfig(levels: Int, cycles: Int) {
    override def toString = s"$levels cascaded queues running for ${cycles}"
  }
  override def benchName: String = "Cascade Queues"

  override def resource: String = "integration/yosys/micro/QueueCascade.v"

  override def testBench(cfg: TestConfig): String = {

    s"""|
        |module Main(input wire clk);
        |
        |  reg [15:0] counter;
        |  wire rst = (counter < 10);
        |  wire enq_ready;
        |  wire deq_valid;
        |  wire [15:0] deq_bits;
        |  QueueCascade #(${cfg.levels}) dut(
        |    .clock(clk),
        |    .reset(rst),
        |    .io_enq_ready(enq_ready),
        |    .io_enq_valid(counter[0] == 1'b1),
        |    .io_enq_bits(counter),
        |    .io_deq_ready(counter[0] == 1'b0),
        |    .io_deq_valid(deq_valid),
        |    .io_deq_bits(deq_bits)
        |  );
        |
        |
        |  always @(posedge clk) begin
        |    counter <= counter + 1;
        |    if (counter == ${cfg.cycles}) begin
        |      $$finish;
        |    end
        |    if (deq_valid && counter[0] == 1'b0) begin
        |      if (deq_bits[0] != 1'b1) begin
        |        $$stop;
        |      end
        |    end
        |
        |  end
        |
        |
        |endmodule
        |""".stripMargin
  }

  override def outputReference(config: TestConfig): ArrayBuffer[String] = {

    val tempDir = Files.createTempDirectory("queue_ref")
    val vfile   = tempDir.resolve("queue_ref.sv")

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


  override def timeOut: Int = 5000

  // println(outputReference(sum1to9).mkString("\n"))
  val configs = Seq(
    TestConfig(5, 300),
    TestConfig(12, 300),
    TestConfig(20, 400),
    TestConfig(40, 500)
  )

  configs.foreach { cfg => testCase(cfg.toString(), cfg) }


}
