package manticore.compiler.integration.yosys.unit.ripped

import manticore.compiler.UnitFixtureTest
import manticore.compiler.integration.yosys.unit.YosysUnitTest
import manticore.compiler.integration.yosys.unit.CodeResource
import org.scalatest.CancelAfterFailure

class YosysMemoryTestsTester extends UnitFixtureTest with CancelAfterFailure {

    def testCase(name: String): Unit = {
      name should "match verilator's output" in { f =>
          new YosysUnitTest {
            // override def dumpAll = true
            val testIterations = 30000
            val code = CodeResource(s"integration/yosys/memory/$name.v")
            val testDir = f.test_dir

          }.run()

      }
  }
  testCase("full_write")
  testCase("amber23_sram_byte_en")
  testCase("firrtl_938")
  testCase("implicit_en")
  testCase("issue00335")
  testCase("issue00710")
  testCase("no_implicit_en")
  testCase("read_arst")
  testCase("read_two_mux")
  testCase("shared_ports")
  testCase("simple_sram_byte_en")
  testCase("trans_addr_enable")
  testCase("trans_sdp")
  testCase("trans_sp")
  testCase("wide_all") // this one is slightly modified from Yosys
  testCase("wide_read_async")
  testCase("wide_read_mixed")
  testCase("wide_read_sync")
  testCase("wide_read_trans")
  testCase("wide_thru_priority")
  testCase("wide_write")
  testCase("chisel_queue32")
  testCase("chisel_queue13")
  testCase("xilinx_fifo")

}