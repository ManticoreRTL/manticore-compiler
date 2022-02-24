package manticore.compiler.integration.chisel

import org.scalatest.fixture.UnitFixture
import manticore.compiler.integration.chisel.util.ProgramTester
import manticore.compiler.UnitFixtureTest
import chiseltest.ChiselScalatestTester
import manticore.compiler.integration.chisel.util.ProcessorTester
import manticore.compiler.AssemblyContext
import manticore.compiler.ManticorePasses
import manticore.compiler.assembly.levels.codegen.MachineCodeGenerator
import manticore.compiler.assembly.levels.UInt16
import chisel3._
import chiseltest._
import manticore.compiler.assembly.BinaryOperator
import manticore.compiler.assembly.annotations.DebugSymbol
import scala.annotation.tailrec
import manticore.compiler.assembly.levels.TransformationID
import manticore.compiler.HasLoggerId



/**
  * Base test trait to verify correctness of the compiler and the
  * manticore machine in interpreting binary arithmetic instruction
  *
  * @author Mahyar Emami <mahyar.emami@epfl.ch>
  */
trait SingleInstructionTest
    extends UnitFixtureTest
    with ChiselScalatestTester
    with ProgramTester
    with ProcessorTester {
  import BinaryOperator._

  val operator: BinaryOperator

  final def compute(op1: UInt16, op2: UInt16): UInt16 = {
    def shiftAmount(v: UInt16): Int = {
      if (v.toInt >= 16) {
        v.toInt & 0x000f
      } else {
        v.toInt
      }
    }
    operator match {

      case ADD => op1 + op2
      case SUB => op1 - op2
      case OR  => op1 | op2
      case AND => op1 & op2
      case XOR => op1 ^ op2
      case SEQ =>
        val is_eq = op1 == op2
        UInt16(if (is_eq) 1 else 0)
      case SLL =>
        val shift_amount = shiftAmount(op2)
        op1 << shift_amount
      case SRL =>
        val shift_amount = shiftAmount(op2)
        op1 >> shift_amount
      case SRA =>
        val shift_amount = shiftAmount(op2)
        op1 >>> shift_amount
      case SLTS =>
        val rs1_sign = (op1 >> 15) == UInt16(1)
        val rs2_sign = (op2 >> 15) == UInt16(1)

        if (rs1_sign && !rs2_sign) {
          // rs1 is negative and rs2 is positive
          UInt16(1)
        } else if (!rs1_sign && rs2_sign) {
          // rs1 is positive and rs2 is negative
          UInt16(0)
        } else if (!rs1_sign && !rs2_sign) {
          // both are positive
          if (op1 < op2) {
            UInt16(1)
          } else {
            UInt16(0)
          }
        } else {
          // both are negative
          val op1_pos =
            (~op1) + UInt16(1) // 2's complement positive number
          val op2_pos =
            (~op2) + UInt16(1) // 2's complement positive number
          if (op1_pos > op2_pos) {
            UInt16(1)
          } else {
            UInt16(0)
          }
        }

    }
  }

  def mkProgram(
      context: AssemblyContext,
      op1: Seq[UInt16],
      op2: Seq[UInt16],
      expected_results: Seq[UInt16]
  ): String = {
    require(op1.length == op2.length)
    require(context.output_dir.nonEmpty)
    val dir = context.output_dir.get

    val res_meminit = mkMemInit(
      expected_results,
      dir.toPath().resolve("expected_results.dat")
    )
    val res_memblock = mkMemBlock("expected_results", expected_results.length)

    val op1_meminit = mkMemInit(op1, dir.toPath().resolve("op1.dat"))
    val op1_memblock = mkMemBlock("op1_values", op1.length)
    val op2_meminit = mkMemInit(op2, dir.toPath().resolve("op2.dat"))
    val op2_memblock = mkMemBlock("op2_values", op1.length)

    s"""
        |.prog:
        |   @LOC [x = 0, y = 0]
        |   .proc proc_0_0:
        |       ${res_meminit}
        |       ${res_memblock}
        |       .mem res_ptr 16
        |       .wire res_addr 16
        |       .wire expected_res 16
        |
        |
        |       ${op1_meminit}
        |       ${op1_memblock}
        |       .mem op1_ptr 16
        |       .wire op1_addr 16
        |       .wire op1_val  16
        |
        |       ${op2_meminit}
        |       ${op2_memblock}
        |       .mem op2_ptr 16
        |       .wire op2_addr 16
        |       .wire op2_val  16
        |
        |       .const one 16 1
        |       .const zero 16 0
        |       .const test_length 16 ${op1.length}
        |
        |       ${mkReg("counter", Some(UInt16(0)))}
        |       ${mkReg("done", Some(UInt16(0)))}
        |       ${mkReg("result", None)}
        |       ${mkReg("correct", Some(UInt16(1)))}
        |
        |
        |       ADD op1_addr, op1_ptr, counter_curr;
        |       ADD op2_addr, op2_ptr, counter_curr;
        |       ADD res_addr, res_ptr, counter_curr;
        |
        |       ${op1_memblock}
        |       LLD op1_val, op1_addr[0];
        |       ${op2_memblock}
        |       LLD op2_val, op2_addr[0];
        |
        |       ${operator} result_next, op1_val, op2_val;
        |
        |       ${res_memblock}
        |       LLD expected_res, res_addr[0];
        |
        |       ADD counter_next, counter_curr, one;
        |       SEQ correct_next, result_next, expected_res;
        |
        |       SEQ done_next, counter_next, test_length;
        |
        |       @TRAP [ type = "\\fail" ]
        |       EXPECT correct_curr, one, ["failed"];
        |       @TRAP [ type = "\\stop" ]
        |       EXPECT done_curr, zero, ["stopped"];
        |
        |
        |   @LOC [ x = 1, y = 0]
        |   .proc p_1_0:
        |       ${mkReg("dummy", None)}
        |       ${mkReg("result", None)}
        |       MOV dummy_next, result_curr;
        """.stripMargin
  }
  def createTest(
      op1: Seq[UInt16],
      op2: Seq[UInt16],
      expected_vcycles: Int,
      fixture: FixtureParam
  ): Unit = {
    val context = AssemblyContext(
      output_dir = Some(fixture.test_dir.resolve("out").toFile()),
      max_dimx = 2,
      max_dimy = 2,
      dump_all = true,
      dump_dir = Some(fixture.test_dir.resolve("dumps").toFile()),
      expected_cycles = Some(expected_vcycles),
      use_loc = true,
      log_file = Some(fixture.test_dir.resolve("run.log").toFile())
      // log_file = None
    )
    implicit val TestName = new HasLoggerId { val id = getTestName }
    val expected_results = (op1 zip op2) map { case (a, b) => compute(a, b) }
    val program =
      compile(mkProgram(context, op1, op2, expected_results), context)
    // interpret the program to make sure it is correct before trying it
    // out on Verilator
    ManticorePasses.BackendInterpreter(true)(program, context)

    // now we need to generate machine code and give it to verilator
    // we should also keep a list of all the messages we expect to observe

    val assembled_program =
      MachineCodeGenerator.assembleProgram(program)(context)
    val sleep_cycles = 5
    val countdown_time = 2
    val timeout =
      (assembled_program.head.total + sleep_cycles) * (expected_vcycles) + 500
    context.logger.info(s"time out is ${timeout} cycles")
    // generate initialization files
    MachineCodeGenerator.generateCode(assembled_program)(context)

    val expected_address: Int = assembled_program
      .find(_.orig.id.x == 1)
      .get
      .orig
      .registers
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
    ).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) {
      dut =>
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
          to_check match {
            case x +: tail =>
              if (dut.io.packet_out.valid.peek().litToBoolean) {
                dut.io.packet_out.address.expect(expected_address.U)
                dut.io.packet_out.data.expect(x.toInt.U)
                check(tail)
              } else {
                check(to_check)
              }
            case Nil =>
          }
        }

        context.logger.info("Snooping results")
        check(expected_results)
        context.logger.info("Waiting for stop")
        while (!dut.io.periphery.exception.error.peek().litToBoolean) {
          dut.clock.step()
        }
        // ensure no EXPECTs failed
        assert(dut.io.periphery.exception.id.peek().litValue.toInt < 0x8000)


    }

  }



}
