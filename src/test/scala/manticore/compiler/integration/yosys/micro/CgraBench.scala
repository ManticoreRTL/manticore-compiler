package manticore.compiler.integration.yosys.micro

import manticore.compiler.AssemblyContext
import manticore.compiler.FileDescriptor
import manticore.compiler.WithInlineVerilog
import manticore.compiler.WithResource
import manticore.compiler.integration.yosys.unit.YosysUnitTest

import java.io.PrintWriter
import java.nio.file.Files
import scala.collection.mutable.ArrayBuffer

final class CgraBench extends MicroBench {

  type TestConfig = Unit
  override def benchName: String = "A latency-insensitive CGRA"

  override def verilogSources: Seq[FileDescriptor] = Seq(
    WithResource("integration/yosys/micro/cgra/DataflowTester.v"),
    WithResource("integration/yosys/micro/cgra/fp_add.v"),
    WithResource("integration/yosys/micro/cgra/fp_mult.v")
  )

  override def testBench(cfg: TestConfig): FileDescriptor = WithResource("integration/yosys/micro/cgra/Main.v")

  override def outputReference(config: TestConfig): ArrayBuffer[String] = ArrayBuffer()

  override def timeOut: Int = 2000

  // println(outputReference(sum1to9).mkString("\n"))
  testCase(s"Pass through kernel", ())

}
