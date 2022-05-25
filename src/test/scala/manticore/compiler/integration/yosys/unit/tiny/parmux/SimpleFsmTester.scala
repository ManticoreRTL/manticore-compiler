package manticore.compiler.integration.yosys.unit.tiny.parmux


import manticore.compiler.UnitFixtureTest
import manticore.compiler.integration.yosys.unit.YosysUnitTest
import manticore.compiler.integration.yosys.unit.CodeResource

class SimpleFsmTester extends UnitFixtureTest {

  behavior of "a simple FSMs"


  def testCase(name: String): Unit = {
      name should "verilator's output" in { f =>
          new YosysUnitTest {
            // override def dumpAll = true
            val testIterations = 3000
            val code = CodeResource(s"integration/yosys/asic-world/$name.v")
            val testDir = f.test_dir
          }.run()

      }
  }

  testCase("code_verilog_tutorial_fsm_full")
  testCase("fsm_using_function")
}