package manticore.compiler.integration.yosys.micro

import manticore.compiler.UnitFixtureTest
import manticore.compiler.frontend.yosys.YosysVerilogReader
import manticore.compiler.frontend.yosys.Yosys
import manticore.compiler.frontend.yosys.YosysRunner
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.parser.AssemblyFileParser
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedMakeDebugSymbols
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedNameChecker
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedOrderInstructions
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedInterpreter
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedCloseSequentialCycles
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.integration.yosys.unit.YosysUnitTest
import manticore.compiler.UnitTestMatchers
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRConstantFolding
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedDeadCodeElimination
import manticore.compiler.assembly.levels.unconstrained.width.WidthConversion
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRCommonSubExpressionElimination
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedJumpTableConstruction
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRParMuxDeconstructionTransform
import manticore.compiler.assembly.levels.placed.UnconstrainedToPlacedTransform
import scala.collection.mutable.ArrayBuffer

object AluReference {
  sealed abstract class AluControl(val v: Int)
  case object SLL extends AluControl(Integer.parseInt("0000", 2))
  case object SRL extends AluControl(Integer.parseInt("0001", 2))
  case object SRA extends AluControl(Integer.parseInt("0010", 2))
  case object ADD extends AluControl(Integer.parseInt("0011", 2))
  case object SUB extends AluControl(Integer.parseInt("0100", 2))
  case object AND extends AluControl(Integer.parseInt("0101", 2))
  case object OR extends AluControl(Integer.parseInt("0110", 2))
  case object XOR extends AluControl(Integer.parseInt("0111", 2))
  case object NOR extends AluControl(Integer.parseInt("1000", 2))
  case object SLT extends AluControl(Integer.parseInt("1001", 2))
  case object LUI extends AluControl(Integer.parseInt("1010", 2))

  val controls = Seq(SLL, SRL, SRA, ADD, SUB, AND, OR, XOR, NOR, SLT, LUI)

  def compute(ctrl: AluControl, op1: Long, op2: Long): (Long, Boolean) = {
    val shift_amount = op2 & 31
    val res = ctrl match {
      case SLL => op1 << shift_amount
      case SRL =>
        op1 >>> shift_amount //unsigned shift i.e., >> in SystemVerilog
      case SRA =>
        (op1.toInt) >> shift_amount.toInt // signed shift i.e., >>> in SystemVerilog
      case ADD => op1 + op2
      case SUB => op1 - op2
      case AND => op1 & op2
      case OR  => op1 | op2
      case XOR => op1 ^ op2
      case NOR => ~(op1 | op2)
      case SLT => if (op1 < op2) 1 else 0
      case LUI => op1 << 16
    }
    val mask: Long = 0x00000000ffffffffL
    val actual_res = res & mask
    (actual_res, actual_res == 0)
  }

}
class MipsAluTester extends UnitFixtureTest with UnitTestMatchers {

  behavior of "Mips ALU"

  val randGen = new scala.util.Random(10)
  def generateTestBench(testSize: Int): String = {

    val mask: Long = 0x00000000ffffffffL
    val ctrl = Seq.fill(testSize) {
      val ix = randGen.nextInt(AluReference.controls.length)
      AluReference.controls(ix)
    }
    val op1 = Seq.fill(testSize) { randGen.nextLong() & mask }
    val op2 = Seq.tabulate(testSize) { ix =>
      if (
        ctrl(ix) == AluReference.SLL || ctrl(ix) == AluReference.SRL || ctrl(
          ix
        ) == AluReference.SRA
      )
        randGen.nextInt(32)
      else
        randGen.nextLong() & mask
    }

    val multiResults: Seq[(Long, Boolean)] = ctrl zip (op1 zip op2) map {
      case (c, (r1, r2)) =>
        AluReference.compute(c, r1, r2)
    }

    val results = multiResults.map { case (r, _) => r }
    val zeros = multiResults.map { case (_, b) => if (b) 1L else 0L }

    def mkInit(name: String, values: Seq[Long]): String = {
      values.zipWithIndex
        .map { case (v, ix) => s"\t${name}[${ix}] = $v; " }
        .mkString("\n")
    }
    s"""|
        |module Main #(
        |    TEST_SIZE = $testSize
        |) (
        |    input wire clk
        |);
        |
        |  logic [3:0]  ctrl_rom     [TEST_SIZE - 1 : 0];
        |  logic [31:0] op1_rom     [TEST_SIZE - 1 : 0];
        |  logic [31:0] op2_rom     [TEST_SIZE - 1 : 0];
        |  logic [31:0] result_rom  [TEST_SIZE - 1 : 0];
        |  logic [0: 0] zero_rom    [TEST_SIZE - 1 : 0];
        |  wire  [31:0] result;
        |  wire         zero;
        |  logic [15:0] counter = 0;
        |
        |  initial begin
        |${mkInit("op1_rom", op1)}
        |${mkInit("op2_rom", op2)}
        |${mkInit("ctrl_rom", ctrl.map(_.v.toLong))}
        |${mkInit("result_rom", results)}
        |${mkInit("zero_rom", zeros)}
        |  end
        |
        |  Alu dut (
        |      .ctrl(ctrl_rom[counter]),
        |      .op1(op1_rom[counter]),
        |      .op2(op2_rom[counter]),
        |      .result(result),
        |      .zero(zero)
        |  );
        |
        |  always_ff @(posedge clk) begin
        |
        |    if (result_rom[counter] != result) begin
        |      $$display("Expected result[%d] = 0x%h but got 0x%h", counter,  result_rom[counter], result);
        |      $$stop;
        |    end
        |    if (zero_rom[counter] != zero) begin
        |      $$display("Expected zero[%d] = %b but got %b", counter,  zero_rom[counter], zero);
        |      $$stop;
        |    end
        |    if (counter < TEST_SIZE - 1) begin
        |      counter <= counter + 1;
        |    end else begin
        |      $$display("Finished without errors at %d", counter);
        |      $$finish;
        |    end
        |
        |  end
        |
        |endmodule
        |""".stripMargin

  }

  def interpret(
      program: UnconstrainedIR.DefProgram
  )(implicit
      ctx: AssemblyContext
  ): ArrayBuffer[String] = {
    val closed = UnconstrainedCloseSequentialCycles(program)
    val serialOut = ArrayBuffer.empty[String]
    val interp = UnconstrainedInterpreter.instance(
      program = closed,
      serial = Some(ln => serialOut += ln)
    )
    interp.runCompletion()
    serialOut
  }

  def check(
      clue: String,
      program: UnconstrainedIR.DefProgram,
      reference: ArrayBuffer[String]
  )(implicit ctx: AssemblyContext) = {
    val got = interpret(program)
    if (!YosysUnitTest.compare(reference, got)) {
      ctx.logger.flush()
      fail(s"${clue}: results did not match the reference")
    }
    if (ctx.logger.countErrors() > 0) {
      ctx.logger.flush()
      fail(s"${clue}: Errors occurred")
    }
  }
  object CompilationStage {

    // the very first stage. Only reorder instructions according to
    // topological order
    val preparation = AssemblyFileParser andThen
      UnconstrainedNameChecker andThen
      UnconstrainedMakeDebugSymbols andThen
      UnconstrainedOrderInstructions

    val unconstrainedOptimizations =
      UnconstrainedIRConstantFolding andThen
        UnconstrainedIRCommonSubExpressionElimination andThen
        UnconstrainedDeadCodeElimination andThen
        UnconstrainedNameChecker

    val controlOptimization =
      UnconstrainedJumpTableConstruction andThen
        UnconstrainedNameChecker andThen
        UnconstrainedIRParMuxDeconstructionTransform andThen
        UnconstrainedNameChecker

    val widthLowering =
      WidthConversion.transformation andThen
        UnconstrainedIRConstantFolding andThen
        UnconstrainedDeadCodeElimination

    val translation = UnconstrainedToPlacedTransform

  }

  "ALU" should "work correctly after parsing" in { fixture =>
    val testBench = generateTestBench(300)
    val verilogCode = scala.io.Source
      .fromResource("integration/yosys/micro/alu.sv")
      .getLines()
      .mkString("\n") + "\n" + testBench
    val vFilePath = fixture.dump("alu.sv", verilogCode)

    implicit val ctx = AssemblyContext(
      dump_all = false,
      dump_dir = Some(fixture.test_dir.toFile),
      quiet = false,
      log_file = Some(fixture.test_dir.resolve("run.log").toFile())
      // log_file = None
    )

    val yosysCompiler = YosysVerilogReader andThen
      Yosys.PreparationPasses andThen
      Yosys.LoweringPasses andThen
      YosysRunner(fixture.test_dir) andThen
      CompilationStage.preparation

    val program1 = yosysCompiler(Seq(vFilePath))
    val reference = ArrayBuffer[String]("Finished without errors at   299")
    check("yosys + ordering", program1, reference)

    val program2 = CompilationStage.unconstrainedOptimizations(program1)
    check("prelim opts", program2, reference)
    val program3 = CompilationStage.unconstrainedOptimizations(program2)
    check("jump table", program3, reference)
    val program4 = CompilationStage.widthLowering(program3)
    check("width conversion", program4, reference)


  }
}
