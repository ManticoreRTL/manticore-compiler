package manticore.compiler.integration.yosys.micro

import manticore.compiler.FileDescriptor

import scala.collection.mutable.ArrayBuffer
import manticore.compiler.WithResource

final class MatrixMultiplicationBench extends MicroBench {

  type TestConfig = Unit

  override def benchName: String = "matmul 16x16"

  override def verilogSources(cfg: TestConfig): Seq[FileDescriptor] = Seq(
    WithResource("integration/yosys/micro/matmul/MatrixMultiplierStreaming.v")
  )

  override def hexSources(cfg: TestConfig): Seq[FileDescriptor] = Nil

  override def testBench(cfg: TestConfig): FileDescriptor = WithResource(
    "integration/yosys/micro/matmul/Main.sv"
  )

  override def outputReference(testSize: TestConfig): ArrayBuffer[String] = ArrayBuffer()


  testCase("random values in matmul 16x16", ())

}
