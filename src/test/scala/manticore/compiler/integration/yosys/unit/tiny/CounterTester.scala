package manticore.compiler.integration.yosys.unit.tiny

import manticore.compiler.integration.yosys.unit.YosysUnitTest
import manticore.compiler.integration.yosys.unit.CodeResource
import org.scalatest.fixture.UnitFixture
import manticore.compiler.UnitFixtureTest
import manticore.compiler.CompilationFailureException

class CounterTester extends UnitFixtureTest {

  behavior of "a simple counter"

  it should "manticore should produce the same stdout as verilator" in { f =>
      new YosysUnitTest {
        val testIterations = 1000
        val code = CodeResource("integration/yosys/tiny/counter.v")
        val testDir = f.test_dir
      }.run()

  }
}
