package manticore.compiler.integration.yosys.micro

import manticore.compiler.AssemblyContext
import manticore.compiler.integration.yosys.unit.YosysUnitTest

import java.io.PrintWriter
import java.nio.file.Files
import scala.collection.mutable.ArrayBuffer
import org.scalatest.CancelAfterFailure

final class BitcoinBench extends MicroBench with CancelAfterFailure {

  case class TestConfig(difficulty: Int, loopLog2: Int) {
      require(difficulty < 256 && difficulty >= 1)
      require(loopLog2 >= 0 && loopLog2 <= 5)
      override def toString = s"DIFF = ${difficulty} LOOP_LOG2 = ${loopLog2}"
  }
  override def benchName: String = "bitcoin"

  override def resource: String = "integration/yosys/micro/bitcoin.v"

  override def testBench(cfg: TestConfig): String = {

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


  }

  override def outputReference(config: TestConfig): ArrayBuffer[String] = {
      val tempDir = Files.createTempDirectory("bitcoin_ref")
    val vfile = tempDir.resolve("bitcoin_tb.sv")

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

  override def timeOut: Int = 10000

  val loopFactor = Range(0, 5+1) // 0 means fully unrolled and hugely parallel hardware

  val configs = loopFactor.map { TestConfig(10, _) }

  configs.reverseIterator.foreach { // do in reverse order because the
    // later ones run faster
    cfg => testCase(cfg.toString(), cfg)
  }


}