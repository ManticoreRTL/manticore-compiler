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

final class JpegCoreBench extends MicroBench with CancelAfterFailure {

  type TestConfig = Int
  override def benchName: String = "jpeg_core"

  override def verilogSources: Seq[FileDescriptor] = Seq(
    WithResource("integration/yosys/micro/jpeg_core/src/jpeg_bitbuffer.v"),
    WithResource("integration/yosys/micro/jpeg_core/src/jpeg_core.v"),
    WithResource("integration/yosys/micro/jpeg_core/src/jpeg_dht.v"),
    WithResource("integration/yosys/micro/jpeg_core/src/jpeg_dht_std_cx_ac.v"),
    WithResource("integration/yosys/micro/jpeg_core/src/jpeg_dht_std_cx_dc.v"),
    WithResource("integration/yosys/micro/jpeg_core/src/jpeg_dht_std_y_ac.v"),
    WithResource("integration/yosys/micro/jpeg_core/src/jpeg_dht_std_y_dc.v"),
    WithResource("integration/yosys/micro/jpeg_core/src/jpeg_dqt.v"),
    WithResource("integration/yosys/micro/jpeg_core/src/jpeg_idct.v"),
    WithResource("integration/yosys/micro/jpeg_core/src/jpeg_idct_fifo.v"),
    WithResource("integration/yosys/micro/jpeg_core/src/jpeg_idct_ram.v"),
    WithResource("integration/yosys/micro/jpeg_core/src/jpeg_idct_ram_dp.v"),
    WithResource("integration/yosys/micro/jpeg_core/src/jpeg_idct_transpose.v"),
    WithResource("integration/yosys/micro/jpeg_core/src/jpeg_idct_transpose_ram.v"),
    WithResource("integration/yosys/micro/jpeg_core/src/jpeg_idct_x.v"),
    WithResource("integration/yosys/micro/jpeg_core/src/jpeg_idct_y.v"),
    WithResource("integration/yosys/micro/jpeg_core/src/jpeg_input.v"),
    WithResource("integration/yosys/micro/jpeg_core/src/jpeg_mcu_id.v"),
    WithResource("integration/yosys/micro/jpeg_core/src/jpeg_mcu_proc.v"),
    WithResource("integration/yosys/micro/jpeg_core/src/jpeg_output.v"),
    WithResource("integration/yosys/micro/jpeg_core/src/jpeg_output_cx_ram.v"),
    WithResource("integration/yosys/micro/jpeg_core/src/jpeg_output_fifo.v"),
    WithResource("integration/yosys/micro/jpeg_core/src/jpeg_output_y_ram.v"),
  )

    override def hexSources: Seq[FileDescriptor] = Seq(
      WithResource("integration/yosys/micro/jpeg_core/hex/red-100x75_data.hex"),
      WithResource("integration/yosys/micro/jpeg_core/hex/red-100x75_strb.hex"),
      WithResource("integration/yosys/micro/jpeg_core/hex/green-100x75_strb.hex"),
      WithResource("integration/yosys/micro/jpeg_core/hex/green-100x75_data.hex"),
      WithResource("integration/yosys/micro/jpeg_core/hex/blue-100x75_data.hex"),
      WithResource("integration/yosys/micro/jpeg_core/hex/blue-100x75_strb.hex"),
    )

  override def testBench(cfg: TestConfig): FileDescriptor = {
    WithResource("integration/yosys/micro/jpeg_core/TestBench.sv")
  }

  override def outputReference(config: TestConfig): ArrayBuffer[String] = {

    val tempDir = Files.createTempDirectory("vref")

    implicit val ctx = AssemblyContext(
      log_file = Some(tempDir.resolve("verilator.log").toFile())
    )

    val vPaths = (verilogSources :+ testBench(config)).map(f => f.p)
    val hPaths = hexSources.map(f => f.p)
    YosysUnitTest.verilate(vPaths, hPaths, timeOut)
  }

  override def timeOut: Int = 1000000

  val config = 0 // The configuration doesn't actually do anything here.
  testCase("Stream images to jpeg core", config)

}
