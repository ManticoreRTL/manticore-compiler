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

  type TestConfig = Int
  override def benchName: String = "tvm-vta"

  override def verilogSources: Seq[FileDescriptor] = Seq(
    WithResource("integration/yosys/micro/tvm-vta/IntelShell.sv"),
    WithResource("integration/yosys/micro/axi4_full_slave/axi4_full_slave.sv"),
    WithResource("integration/yosys/micro/xormix32.sv")
  )

    override def hexSources: Seq[FileDescriptor] = Seq(
      WithResource("integration/yosys/micro/tvm-vta/mem.hex"),
    )

  override def testBench(cfg: TestConfig): FileDescriptor = {
    WithResource("integration/yosys/micro/tvm-vta/TestBench.sv")
  }

  override def outputReference(config: TestConfig): ArrayBuffer[String] = {

    val tempDir = Files.createTempDirectory("vref")

    implicit val ctx = AssemblyContext(
      // log_file = Some(tempDir.resolve("verilator.log").toFile())
    )

    val vPaths = (verilogSources :+ testBench(config)).map(f => f.p)
    val hPaths = hexSources.map(f => f.p)
    YosysUnitTest.verilate(vPaths, hPaths, timeOut)
  }

  override def timeOut: Int = 1000000

  val config = 0 // The configuration doesn't actually do anything here.
  testCase("Communicate with the VTA core", config)

}
