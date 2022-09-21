package manticore.compiler.integration.chisel

import chisel3._
import chiseltest._
import manticore.compiler.AssemblyContext
import manticore.compiler.DefaultHardwareConfig
import manticore.compiler.HasLoggerId
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.assembly.levels.codegen.MachineCodeGenerator
import manticore.compiler.assembly.utils.XorShift16
import manticore.compiler.integration.chisel.util.KernelTester

import scala.annotation.tailrec

class GMemTester extends KernelTester {

  def mkProgram(
      context: AssemblyContext,
      testSize: Int
  ): String = {
    val dir     = context.output_dir.get
    val rnd1    = XorShift16("rand1", UInt16(123))
    val rnd2    = XorShift16("rand2", UInt16(456))
    val rnd3    = XorShift16("rand3", UInt16(789))
    val meminit = mkMemInit(Seq.fill(testSize) { UInt16(0) }, dir.toPath().resolve("mem.dat"))

    s"""
       |.prog:
       |   @LOC [x = 0, y = 0]
       |   .proc proc_0_0:
       |       ${meminit}
       |       .mem mem_ptr1 16 ${testSize}
       |       ${meminit}
       |       .mem mem_ptr2 16 ${testSize}
       |       ${meminit}
       |       .mem mem_ptr3 16 ${testSize}
       |
       |       .wire st_val1 16
       |       .wire st_val2 16
       |       .wire st_val3 16
       |       .wire ld_val1 16
       |       .wire ld_val2 16
       |       .wire ld_val3 16
       |
       |
       |       ${rnd1.registers}
       |       ${rnd2.registers}
       |       ${rnd3.registers}
       |
       |
       |       .const one 16 1
       |       .const zero 16 0
       |       .const test_length 16 ${testSize}
       |
       |
       |       ${mkReg("counter", Some(UInt16(0)))}
       |       ${mkReg("done", Some(UInt16(0)))}
       |       ${mkReg("correct1", Some(UInt16(1)))}
       |       ${mkReg("correct2", Some(UInt16(1)))}
       |       ${mkReg("correct3", Some(UInt16(1)))}
       |
       |
       |       ${rnd1.code}
       |       ${rnd2.code}
       |       ${rnd3.code}
       |
       |
       |       ADD st_val1, ${rnd1.randNext}, zero;
       |       ADD st_val2, ${rnd2.randNext}, zero;
       |       ADD st_val3, ${rnd3.randNext}, zero;
       |       (mem_ptr1, 0) LST st_val1, mem_ptr1[counter_curr], one;
       |       (mem_ptr2, 0) LST st_val2, mem_ptr2[counter_curr], one;
       |       (mem_ptr3, 0) LST st_val3, mem_ptr3[counter_curr], one;
       |       (mem_ptr1, 1) LLD ld_val1, mem_ptr1[counter_curr];
       |       (mem_ptr2, 1) LLD ld_val2, mem_ptr2[counter_curr];
       |       (mem_ptr3, 1) LLD ld_val3, mem_ptr3[counter_curr];
       |
       |       ADD counter_next, counter_curr, one;
       |       SEQ correct1_next, ld_val1, st_val1;
       |       SEQ correct2_next, ld_val2, st_val2;
       |       SEQ correct3_next, ld_val3, st_val3;
       |
       |       SEQ done_next, counter_next, test_length;
       |
       |       (0) ASSERT correct1_curr;
       |       (0) ASSERT correct2_curr;
       |       (0) ASSERT correct3_curr;
       |       (1) FINISH done_curr;
       |
        """.stripMargin
  }

  def createTestAndCompileAndRun(expected_vcycles: Int)(implicit
      fixture: FixtureParam
  ): Unit = {
    val context = AssemblyContext(
      output_dir = Some(fixture.test_dir.resolve("out").toFile()),
      // make the scratchpad size small so that the global memory will be used
      hw_config = DefaultHardwareConfig(dimX = 2, dimY = 2, nScratchPad = 128),
      dump_all = true,
      dump_dir = Some(fixture.test_dir.resolve("dumps").toFile()),
      expected_cycles = Some(expected_vcycles),
      use_loc = true
    )

    implicit val TestName = new HasLoggerId { val id = getTestName }
    val source            = mkProgram(context, expected_vcycles)
    fixture.dump("main.masm", source)
    compileAndRun(source, context)
  }

  behavior of "Global Memory"
  it should "correctly handle global stores and loads" in { implicit f =>
    createTestAndCompileAndRun(expected_vcycles = 500)
  }

}
