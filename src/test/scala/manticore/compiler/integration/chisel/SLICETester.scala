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
import manticore.compiler.integration.chisel.util.ProcessorTester
import manticore.compiler.integration.chisel.util.ProgramTester
import org.scalatest.fixture.UnitFixture

import scala.annotation.tailrec

class SLICETester extends UnitFixtureTest with ChiselScalatestTester with ProgramTester with ProcessorTester {

  def mkProgram(
      context: AssemblyContext,
      op: Seq[UInt16],
      offset: Int,
      length: Int,
      expected_results: Seq[UInt16]
  ): String = {
    require(offset + length <= 16)
    val dir = context.output_dir.get

    val res_meminit = mkMemInit(
      expected_results,
      dir.toPath().resolve("expected_results.dat")
    )

    val op_meminit = mkMemInit(op, dir.toPath().resolve("op.dat"))

    s"""
       |.prog:
       |   @LOC [x = 0, y = 0]
       |   .proc proc_0_0:
       |       ${res_meminit}
       |       .mem res_ptr 16 ${expected_results.length}
       |       .wire expected_res 16
       |
       |
       |       ${op_meminit}
       |       .mem op_ptr 16 ${op.length}
       |       .wire op_val  16
       |
       |       .const one 16 1
       |       .const zero 16 0
       |       .const test_length 16 ${op.length}
       |
       |
       |       ${mkReg("counter", Some(UInt16(0)))}
       |       ${mkReg("done", Some(UInt16(0)))}
       |       ${mkReg("result", None)}
       |       ${mkReg("correct", Some(UInt16(1)))}
       |
       |
       |       (op_ptr, 0) LLD op_val, op_ptr[counter_curr];
       |
       |       SLICE result_next, op_val, ${offset}, ${length};
       |
       |
       |       (res_ptr, 0) LLD expected_res, res_ptr[counter_curr];
       |
       |       ADD counter_next, counter_curr, one;
       |       SEQ correct_next, result_next, expected_res;
       |
       |       SEQ done_next, counter_next, test_length;
       |
       |       (0) ASSERT correct_curr;
       |       (1) FINISH done_curr;
       |
       |
       |   @LOC [ x = 1, y = 0]
       |   .proc p_1_0:
       |       @REG [id = \"result\", type = \"\\REG_CURR\" ]
       |
       |       .input result_curr 16
       |       @TRACK [ name = "dummy" ]
       |       .wire dummy 16
       |       MOV dummy, result_curr;
        """.stripMargin
  }

  def createTest(
      op: Seq[UInt16],
      offset: Int,
      length: Int,
      expected_vcycles: Int,
      fixture: FixtureParam
  ): Unit = {
    val context = AssemblyContext(
      output_dir = Some(fixture.test_dir.resolve("out").toFile()),
      hw_config = DefaultHardwareConfig(dimX = 2, dimY = 2, maxLatency = 10),
      dump_all = true,
      dump_dir = Some(fixture.test_dir.resolve("dumps").toFile()),
      expected_cycles = Some(expected_vcycles),
      use_loc = true
    )

    implicit val TestName = new HasLoggerId { val id = getTestName }
    val expected_results  = op map { a => (a >> offset) & UInt16((1 << length) - 1) }
    val text              = mkProgram(context, op, offset, length, expected_results)
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

    val des_proc = assembled_program
      .find(p => p.orig.id.x == 1 && p.orig.id.y == 0)
      .get
      .orig
    val expected_address = des_proc.registers
      .find(r =>
        r.annons
          .collectFirst { case x: DebugSymbol =>
            x.getSymbol() == "result_curr"
          }
          .getOrElse(false)
      )
      .get
      .variable
      .id

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

      @tailrec
      def check(to_check: Seq[UInt16]): Unit = {
        dut.clock.step()
        if (dut.io.periphery.exception.error.peek().litToBoolean && to_check.nonEmpty) {
          context.logger.error(
            s"Got early exception with id ${dut.io.periphery.exception.id.peek().litValue.toInt} while there were ${to_check.length} packets left to check!"
          )

        } else {
          to_check match {
            case x +: tail =>
              if (dut.io.packet_out.valid.peek().litToBoolean) {
                context.logger.info(s"Checking for value ${x}")
                dut.io.packet_out.address.expect(expected_address.U)
                dut.io.packet_out.data.expect(x.toInt.U)
                check(tail)
              } else {
                check(to_check)
              }
            case Nil =>
          }
        }
      }

      context.logger.info("Snooping results")
      check(expected_results)
      context.logger.info("Waiting for finish")
      while (!dut.io.periphery.exception.error.peek().litToBoolean) {
        dut.clock.step()
      }
      // ensure no EXPECTs failed
      assert(dut.io.periphery.exception.id.peek().litValue.toInt == 1)
    }
  }

  behavior of "SLICE in Manticore Machine"
  it should "correctly handle random SLL cases" in { f =>
    val randgen = new scala.util.Random(0)

    Range(0, 10).foreach { i =>
      val op     = Seq.fill(400) { UInt16(randgen.nextInt(0xffff + 1)) }
      val offset = randgen.nextInt(16)
      val length = randgen.nextInt(16 - offset)
      println(s"offset: %d, length: %d", offset, length)
      createTest(op = op, expected_vcycles = 400, fixture = f, offset = offset, length = length)
    }
  }

}
