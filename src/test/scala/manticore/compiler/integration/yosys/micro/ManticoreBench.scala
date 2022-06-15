package manticore.compiler.integration.yosys.micro

import scala.collection.mutable.ArrayBuffer

final class ManticoreBench extends MicroBench {

  type TestConfig = Unit
  override def benchName: String = "Manticore"

  override def resource: String = "integration/yosys/micro/Manticore.v"

  override def testBench(cfg: TestConfig): String =

    scala.io.Source.fromResource("integration/yosys/micro/Manticore_tb.sv").mkString("")

  override def outputReference(testSize: TestConfig): ArrayBuffer[String] = ArrayBuffer.empty


  testCase("Init program", ())
}