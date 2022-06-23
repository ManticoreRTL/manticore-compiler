package manticore.compiler.integration.yosys.micro

import manticore.compiler.AssemblyContext
import manticore.compiler.FileDescriptor
import manticore.compiler.WithInlineVerilog
import manticore.compiler.WithResource
import manticore.compiler.integration.yosys.unit.YosysUnitTest

import java.io.PrintWriter
import java.nio.file.Files
import scala.collection.mutable.ArrayBuffer

final class FemtoMips32Bench extends MicroBench {

  type TestConfig = Unit
  override def benchName: String = "Multi-Cycle Mips32"

  override def verilogSources: Seq[FileDescriptor] = Seq(
    WithResource("integration/yosys/micro/femto/single/ring.sv")
  )

  override def testBench(cfg: TestConfig): FileDescriptor = WithResource("integration/yosys/micro/femto/tb.sv")

  override def outputReference(config: TestConfig): ArrayBuffer[String] = {

    val tempDir = Files.createTempDirectory("vref")
    val vfile   = tempDir.resolve("tb.sv")

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


  override def timeOut: Int = 2000

  // println(outputReference(sum1to9).mkString("\n"))
  testCase(s"counter on MIPS", ())

}
