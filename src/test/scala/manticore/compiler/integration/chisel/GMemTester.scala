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
    val rnd     = XorShift16("rand", UInt16(12345))
    val meminit = mkMemInit(Seq.fill(testSize) { UInt16(0) }, dir.toPath().resolve("mem.dat"))

    s"""
       |.prog:
       |   @LOC [x = 0, y = 0]
       |   .proc proc_0_0:
       |       ${meminit}
       |       .mem mem_ptr 16 ${testSize}
       |
       |       .wire st_val 16
       |       .wire ld_val 16
       |
       |
       |       ${rnd.registers}
       |
       |
       |       .const one 16 1
       |       .const zero 16 0
       |       .const test_length 16 ${testSize}
       |
       |
       |       ${mkReg("counter", Some(UInt16(0)))}
       |       ${mkReg("done", Some(UInt16(0)))}
       |       ${mkReg("correct", Some(UInt16(1)))}
       |
       |
       |       ${rnd.code}
       |
       |
       |       ADD st_val, ${rnd.randNext}, zero;
       |       (mem_ptr, 0) LST st_val, mem_ptr[counter_curr], one;
       |       (mem_ptr, 1) LLD ld_val, mem_ptr[counter_curr];
       |
       |       ADD counter_next, counter_curr, one;
       |       SEQ correct_next, ld_val, st_val;
       |
       |       SEQ done_next, counter_next, test_length;
       |
       |       (0) ASSERT correct_curr;
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
