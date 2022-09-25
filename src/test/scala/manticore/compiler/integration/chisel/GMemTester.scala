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
      testSize: Int,
      numMems: Int
  ): String = {
    val dir     = context.output_dir.get
    val rnd     = IndexedSeq.tabulate(numMems) { i => XorShift16(s"rand_$i", UInt16(879 + i << 3)) }
    val meminit = mkMemInit(Seq.fill(0) { UInt16(0) }, dir.toPath().resolve("mem.dat"))

    s"""
       |.prog:
       |   @LOC [x = 0, y = 0]
       |   .proc proc_0_0:
       |
       |       ${Seq.tabulate(numMems) { i => s".mem mem_ptr_${i} 16 ${testSize}"}.mkString("\n")}
       |       ${Seq.tabulate(numMems) { i => s".wire ld_val_${i} 16"}.mkString("\n")}
       |       ${Seq.tabulate(numMems) { i => s".reg cr${i} 1 .input correct_${i}_curr 1 .output correct_${i}_next"}.mkString("\n")}
       |       ${rnd.map(_.registers).mkString("\n")}
       |
       |       .reg cnt 32 .input counter_curr 0 .output counter_next
       |       .reg dn  1 .input done_curr 0 .output done_next
       |       .const one 16 1
       |       .const zero 16 0
       |       .const max_counter 16 ${testSize - 1}
       |
       |       ${rnd.map(_.code).mkString("\n")}
       |
       |
       |       ${Range(0, numMems).map { i => s"(mem_ptr_${i}, 0) LST ${rnd(i).randCurr}, mem_ptr_${i}[counter_curr], one;" }.mkString("\n") }
       |       ${Range(0, numMems).map { i => s"(mem_ptr_${i}, 1) LLD ld_val_${i}, mem_ptr_${i}[counter_curr];" }.mkString("\n") }
       |       ${Range(0, numMems).map { i => s"SEQ correct_${i}_next, ld_val_${i}, ${rnd(i).randCurr};" }.mkString("\n") }
       |       ${Range(0, numMems).map { i => s"(0) ASSERT correct_${i}_curr;" }.mkString("\n") }
       |       ADD counter_next, counter_curr, one;
       |
       |
       |       SEQ done_next, counter_curr, max_counter;
       |
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
      hw_config = DefaultHardwareConfig(dimX = 2, dimY = 2, nScratchPad = 0),
      dump_all = true,
      dump_dir = Some(fixture.test_dir.resolve("dumps").toFile()),
      expected_cycles = Some(expected_vcycles),
      use_loc = true
    )

    implicit val TestName = new HasLoggerId { val id = getTestName }
    val source            = mkProgram(context, expected_vcycles, 10)
    fixture.dump("main.masm", source)
    compileAndRun(source, context)
  }

  behavior of "Global Memory"
  it should "correctly handle global stores and loads" in { implicit f =>
    createTestAndCompileAndRun(expected_vcycles = 4)
  }

}
