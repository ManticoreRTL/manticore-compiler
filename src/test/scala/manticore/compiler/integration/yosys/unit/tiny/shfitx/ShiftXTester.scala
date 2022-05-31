package manticore.compiler.integration.yosys.unit.tiny.shfitx

import manticore.compiler.UnitFixtureTest
import manticore.compiler.integration.yosys.unit.YosysUnitTest
import manticore.compiler.integration.yosys.unit.CodeResource

class ShiftXTester extends UnitFixtureTest {

  behavior of "ShiftX cell in yosys"

  "shiftx" should "should not work because there is usually a better way :(" in { f =>
    assertThrows(new YosysUnitTest {
      val testIterations = 10000
      val code = CodeResource("integration/yosys/tiny/indexed_part_select.v")
      val testDir = f.test_dir

      override val dumpAll = true
    }.run())
  }

}
