package manticore.compiler.integration.yosys.unit.tiny.parmux

import manticore.compiler.UnitFixtureTest
import manticore.compiler.integration.yosys.unit.CodeText
import manticore.compiler.integration.yosys.unit.YosysUnitTest
import manticore.compiler.integration.yosys.unit.CodeResource

class SimpleAluTester extends UnitFixtureTest {

  behavior of "a simple ALU in Verilig"

  "manticore interpretation" should "match verilator's" in { f =>
    new YosysUnitTest {
      override def dumpAll = true
      val testIterations = 40000
      val testDir = f.test_dir
      val code = CodeResource("integration/yosys/tiny/alu.sv")
    }.run()
  }

}
