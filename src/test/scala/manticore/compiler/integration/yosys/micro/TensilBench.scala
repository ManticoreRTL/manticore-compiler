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

final class TensilBench extends MicroBench with CancelAfterFailure {

  type TestConfig = Int
  override def benchName: String = "tensil"

  override def verilogSources(cfg: TestConfig): Seq[FileDescriptor] = Seq(
    WithResource("integration/yosys/micro/tensil/AXIWrapperTCU.v"),
    WithResource("integration/yosys/micro/tensil/bram_dp_128x64.v"),
    WithResource("integration/yosys/micro/axi4_full_slave/axi4_full_slave.sv")
  )

  override def hexSources(cfg: TestConfig): Seq[FileDescriptor] = Seq.empty

  override def testBench(cfg: TestConfig): FileDescriptor = {
    WithResource("integration/yosys/micro/tensil/tb_AXIWrapperTCU.sv")
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

  val config = 0 // The configuration doesn't actually do anything here.
  testCase("stream instructions on Tensil accelerator", config)

}
