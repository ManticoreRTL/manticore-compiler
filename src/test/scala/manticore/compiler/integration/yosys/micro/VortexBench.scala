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

final class VortexBench extends MicroBench with CancelAfterFailure {

  type TestConfig = Int

  override def benchName: String = "vortex"

  override def top: Option[String] = Some("Main")

  override def verilogSources(cfg: TestConfig): Seq[FileDescriptor] = Seq(
    WithResource(s"integration/yosys/micro/vortex/Vortex_axi_nofpu.v"),
    WithResource("integration/yosys/micro/axi4_full_slave/axi4_full_slave.sv"),
  )

  override def hexSources(cfg: TestConfig): Seq[FileDescriptor] = Seq(
    WithResource("integration/yosys/micro/vortex/mem.hex"),
  )

  override def testBench(cfg: TestConfig): FileDescriptor = {
    WithResource("integration/yosys/micro/vortex/TestBench.sv")
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

  val cfg: TestConfig = 1
  testCase(s"vortex", cfg)
}
