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
import java.nio.file.Path

final class TvmVtaBench extends MicroBench with CancelAfterFailure {

  case class TestConfig(blockIn: Int, blockOut: Int)

  override def benchName: String = "tvm-vta"

  override def verilogSources(cfg: TestConfig): Seq[FileDescriptor] = Seq(
    WithResource(s"integration/yosys/micro/tvm-vta/mem_div_16/IntelShell_blockIn${cfg.blockIn}_blockOut${cfg.blockOut}.sv"),
    WithResource("integration/yosys/micro/axi4_full_slave/axi4_full_slave.sv"),
    WithResource("integration/yosys/micro/xormix32.sv")
  )

  override def hexSources(cfg: TestConfig): Seq[FileDescriptor] = Seq(
    WithResource("integration/yosys/micro/tvm-vta/mem.hex"),
  )

  override def testBench(cfg: TestConfig): FileDescriptor = {
    WithResource("integration/yosys/micro/tvm-vta/TestBench.sv")
  }

  override def outputReference(cfg: TestConfig): ArrayBuffer[String] = {

    val tempDir = Files.createTempDirectory("vref")

    implicit val ctx = AssemblyContext(
      log_file = Some(tempDir.resolve("verilator.log").toFile())
    )

    val vPaths = (verilogSources(cfg) :+ testBench(cfg)).map(f => f.p)
    val hPaths = hexSources(cfg).map(f => f.p)
    YosysUnitTest.verilate(vPaths, hPaths, timeOut)
  }

  override def timeOut: Int = 1000000

  val blockIns = Seq(16, 32, 64)
  val blockOuts = Seq(16, 32, 64)

  blockIns.foreach { blockIn =>
    blockOuts.foreach { blockOut =>
      val cfg = TestConfig(blockIn, blockOut)
      testCase(s"(blockIn, blockOut) = (${cfg.blockIn}, ${cfg.blockOut})", cfg)
    }
  }
}
