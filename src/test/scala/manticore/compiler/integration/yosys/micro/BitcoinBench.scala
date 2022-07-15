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

final class BitcoinBench extends MicroBench with CancelAfterFailure {

  case class TestConfig(difficulty: Int, loopLog2: Int) {
    require(difficulty < 256 && difficulty >= 1)
    require(loopLog2 >= 0 && loopLog2 <= 5)
    override def toString = s"DIFF = ${difficulty} LOOP_LOG2 = ${loopLog2}"
  }
  override def benchName: String = "bitcoin"

  override def verilogSources(cfg: TestConfig): Seq[FileDescriptor] = Seq(
    WithResource("integration/yosys/micro/bitcoin.v")
  )

  override def hexSources(cfg: TestConfig): Seq[FileDescriptor] = Seq.empty

  override def testBench(cfg: TestConfig): FileDescriptor = {

    WithInlineVerilog(
      s"""|
          |module Main(input wire clk);
          |
          |   wire [31:0] golden_nonce;
          |   wire [31:0] nonce;
          |   reg [31:0] counter = 0;
          |   fpgaminer_top
          |        #(.LOOP_LOG2(${cfg.loopLog2}), .DIFFICULTY(${cfg.difficulty}))
          |        miner(.clk(clk), .golden_nonce(golden_nonce), .nonce_out(nonce));
          |
          |   always @ (posedge clk) begin
          |       counter <= counter + 1;
          |
          |       if (golden_nonce) begin
          |           $$display("@ %d %h %h", counter, golden_nonce, nonce);
          |           $$finish;
          |       end else if (counter == ${timeOut - 10}) begin
          |           $$display("@ %d timed out!", counter);
          |           $$finish;
          |       end
          |
          |   end
          |
          |endmodule
          |
          |
          |""".stripMargin
    )
  }

  override def outputReference(cfg: TestConfig): ArrayBuffer[String] = {
    val tempDir = Files.createTempDirectory("vref")
    val vfile   = tempDir.resolve("tb.sv")

    val writer = new PrintWriter(vfile.toFile())

    // Read each source and concatenate them together.
    val tb = (verilogSources(cfg) :+ testBench(cfg))
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

  val loopFactor = Range(0, 5 + 1) // 5 means fully unrolled and hugely parallel hardware

  val configs = loopFactor.map { TestConfig(10, _) }

  configs.reverseIterator.foreach { // do in reverse order because the
    // later ones run faster
    cfg => testCase(cfg.toString(), cfg)
  }

}
