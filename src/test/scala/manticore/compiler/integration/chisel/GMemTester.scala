package manticore.compiler.integration.chisel

import chisel3._
import chiseltest._
import manticore.compiler.AssemblyContext
import manticore.compiler.DefaultHardwareConfig
import manticore.compiler.HasLoggerId
import manticore.compiler.ManticorePasses
import manticore.compiler.UnitFixtureTest
import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.assembly.annotations.DebugSymbol
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.assembly.levels.codegen.MachineCodeGenerator
import manticore.compiler.assembly.utils.XorShift16
import manticore.compiler.integration.chisel.util.ProcessorTester
import manticore.compiler.integration.chisel.util.ProgramTester
import org.scalatest.fixture.UnitFixture

import scala.annotation.tailrec

class GMemTester extends UnitFixtureTest with ChiselScalatestTester with ProgramTester with ProcessorTester {

  def mkProgram(
      context: AssemblyContext,
      testSize: Int
  ): String = {
    val dir     = context.output_dir.get
    val rnd     = XorShift16("rand1", UInt16(12345))
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
       |       ADD st_val, ${rnd.randCurr}, zero;
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

  def createTest(
      expected_vcycles: Int,
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
    val text              = mkProgram(context, expected_vcycles)
    fixture.dump("main.masm", text)
    val program = compile(text, context)
    // interpret the program to make sure it is correct before trying it
    // out on Verilator
    ManticorePasses.BackendInterpreter(true)(program)(context)

    // now we need to generate machine code and give it to verilator
    // we should also keep a list of all the messages we expect to observe

    val assembled_program =
      MachineCodeGenerator.assembleProgram(program)(context)

    val sleep_cycles   = context.hw_config.maxLatency
    val countdown_time = 2
    val timeout =
      (assembled_program.head.total + sleep_cycles) * (expected_vcycles) + 500
    context.logger.info(s"time out is ${timeout} cycles")
    // generate initialization files
    MachineCodeGenerator.generateCode(assembled_program)(context)

    context.logger.info("Starting hardware test")

    test(
      mkProcessor(context, 0, 0)
    ).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) { dut =>
      dut.clock.setTimeout(timeout)
      context.logger.info("Programing the processor")
      programProcessor(
        dut,
        assembled_program.head,
        sleep_cycles,
        countdown_time
      )

      context.logger.info("Waiting for finish")
      while (!dut.io.periphery.exception.error.peek().litToBoolean) {
        dut.clock.step()
      }
      // ensure no EXPECTs failed
      assert(dut.io.periphery.exception.id.peek().litValue.toInt == 1)
    }
  }

  behavior of "Global Memory"
  it should "correctly handle global stores and loads" in { f =>
    val randgen = new scala.util.Random(0)
    createTest(expected_vcycles = 500, fixture = f)
  }

}
