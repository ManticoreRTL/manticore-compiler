package manticore.compiler.integration.yosys.micro

import manticore.compiler.FileDescriptor
import manticore.compiler.WithResource

import scala.collection.mutable.ArrayBuffer

final class ManticoreBench extends MicroBench {

  type TestConfig = Unit
  override def benchName: String = "Manticore"

  override def verilogSources: Seq[FileDescriptor] = Seq(
    WithResource("integration/yosys/micro/Manticore.v")
  )

  override def testBench(cfg: TestConfig): FileDescriptor =
    WithResource("integration/yosys/micro/Manticore_tb.sv")

  override def outputReference(testSize: TestConfig): ArrayBuffer[String] = ArrayBuffer.empty

  testCase("Init program", ())
}
