package manticore.compiler.integration.chisel

import manticore.compiler.UnitFixtureTest
import chiseltest.ChiselScalatestTester
import manticore.compiler.AssemblyContext
import java.nio.file.Files
import java.io.PrintWriter
import manticore.compiler.ManticorePasses
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.assembly.levels.AssemblyChecker
import manticore.compiler.integration.chisel.util.ProcessorTester
import chiseltest._
import chisel3._
import manticore.compiler.assembly.levels.codegen.MachineCodeGenerator
import manticore.compiler.HasLoggerId
import manticore.compiler.assembly.levels.placed.UnconstrainedToPlacedTransform
import manticore.compiler.assembly.levels.placed.ScheduleChecker


class ADDCARRYTester
    extends UnitFixtureTest
    with ChiselScalatestTester
    with ProcessorTester {

  val randgen = new scala.util.Random(0)
  def mkBigRand(width: Int): BigInt = {
    val num_shorts = (width - 1) / 16 + 1
    def rand16 = randgen.nextInt(0xffff)
    Seq.fill(num_shorts - 1) { rand16 }.foldLeft(BigInt(rand16)) {
      case (r, x) =>
        (r << 16) | BigInt(x)
    }
  }
  def createProgram(
      width: Int,
      context: AssemblyContext,
      fixture: FixtureParam
  ): String = {

    require(context.expected_cycles.nonEmpty)
    require(context.expected_cycles.get > 2)

    val test_size = context.expected_cycles.get

    def mkMemInit(name: String, values: Seq[BigInt]): String = {
      val path = fixture.dump(name, values.mkString("\n"))
      s"@MEMINIT [ file = \"${path.toAbsolutePath}\", width = ${width}, count = ${values.length} ]"
    }
    def mkMemblock(name: String, values: Seq[BigInt]): String = {
      s"@MEMBLOCK [block = \"${name}\", width = ${width}, capacity = ${values.length}]"
    }

    def mkReg(name: String, width: Int, init: Option[BigInt] = None): String = {
      Seq(
        s"@REG [id = \"${name}\", type = \"\\REG_CURR\" ]",
        s".input ${name}_curr ${width} ${init.map(_.toString).getOrElse("")}",
        s"@REG [id = \"${name}\", type = \"\\REG_NEXT\" ]",
        s".output ${name}_next ${width} "
      ).mkString("\n")
    }

    val op1 = Seq.fill(test_size) { mkBigRand(width) }
    val op2 = Seq.fill(test_size) { mkBigRand(width) }

    val result = op1 zip op2 map { case (x, y) =>
      (x + y) & ((BigInt(1) << width) - 1)
    }

    val op1_meminit = mkMemInit("op1.dat", op1)
    val op1_memblock = mkMemblock("op1", op1)

    val op2_meminit = mkMemInit("op2.dat", op2)
    val op2_memblock = mkMemblock("op2", op2)

    val result_meminit = mkMemInit("result.dat", result)
    val result_memblock = mkMemblock("result", op2)
    def log2ceil(x: Int) = BigInt(x - 1).bitLength
    s"""
    |.prog:
    |     @LOC [ x = 0 , y = 0]
    |    .proc p00:
    |       ${op1_memblock}
    |       ${op1_meminit}
    |       .mem op1_ptr ${log2ceil(test_size)}
    |       .wire op1_address ${log2ceil(test_size)}
    |       .wire op1 ${width}
    |
    |
    |       ${op2_memblock}
    |       ${op2_meminit}
    |       .mem op2_ptr ${log2ceil(test_size)}
    |       .mem op2_address ${log2ceil(test_size)}
    |       .wire op2 ${width}
    |
    |
    |       ${result_memblock}
    |       ${result_meminit}
    |       .mem result_ptr ${log2ceil(test_size)}
    |       .wire result_address ${log2ceil(test_size)}
    |       .wire result_ref ${width}
    |       @TRACK [name = "result_got"]
    |       .wire result_got ${width}
    |
    |
    |       ${mkReg("offset", log2ceil(test_size), Some(BigInt(0)))}
    |       .const offset_one ${log2ceil(test_size)} 1
    |
    |       ${mkReg("counter", 16, Some(BigInt(0)))}
    |       .const one 16 1
    |       .const zero 16 0
    |       .const test_size 16 ${test_size - 1}
    |       ${mkReg("done", 16, Some(BigInt(0)))}
    |
    |       ${mkReg("correct", 16, Some(BigInt(1)))}
    |
    |
    |       ADD offset_next, offset_curr,  offset_one;
    |       ADD op1_address, op1_ptr, offset_curr;
    |       ${op1_memblock}
    |       LLD op1, op1_address [0];
    |
    |       ADD op2_address, op2_ptr, offset_curr;
    |       ${op2_memblock}
    |       LLD op2, op2_address [0];
    |
    |
    |       ADD result_address, result_ptr, offset_curr;
    |       ${result_memblock}
    |       LLD result_ref, result_address [0];
    |
    |       ADD result_got, op1, op2;
    |
    |       SEQ correct_next, result_got, result_ref;
    |       SEQ done_next, counter_curr, test_size;
    |       ADD counter_next, counter_curr, one;
    |
    |       @TRAP [ type = "\\fail" ]
    |       EXPECT correct_curr, one, ["invalid result"];
    |       @TRAP [ type = "\\stop" ]
    |       EXPECT done_curr, zero, ["stopped"];
    |
    |
    |
    """.stripMargin

  }

  def compiler =
    AssemblyParser andThen
    ManticorePasses.frontend andThen
      ManticorePasses.middleend andThen
      UnconstrainedToPlacedTransform andThen
      ManticorePasses.BackendLowerEnd andThen
      ScheduleChecker

  def compile(source: String, context: AssemblyContext): PlacedIR.DefProgram = {

    val program = compiler.apply(source)(context)
    program
  }

  behavior of "ADDCARRY instruction test"

  it should "correctly compute wide addition" in { fixture =>
    val expected_vcycles = 50

    val context = AssemblyContext(
      output_dir = Some(fixture.test_dir.resolve("out").toFile()),
      max_dimx = 1,
      max_dimy = 1,
      dump_all = true,
      dump_dir = Some(fixture.test_dir.resolve("dumps").toFile()),
      expected_cycles = Some(expected_vcycles),
      use_loc = true,
      debug_message = false
      // log_file = None
    )

    val source = createProgram(32, context, fixture)
    val program = compile(source, context)

    ManticorePasses.BackendInterpreter(true)(program)(context)

    val assembled = MachineCodeGenerator.assembleProgram(program)(context)

    MachineCodeGenerator.generateCode(assembled)(context)

    test(mkProcessor(context = context, x = 0, y = 0)).withAnnotations(
      Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)
    ) { dut =>
        implicit val logger_id = new HasLoggerId { val id = getTestName }

        dut.clock.step(10)
        dut.clock.setTimeout(10000)
        context.logger.info("programming the processor")

        programProcessor(dut, assembled.head)

        while (!dut.io.periphery.exception.error.peek().litToBoolean) {
          dut.clock.step()
        }
        // ensure no EXPECTs failed
        assert(dut.io.periphery.exception.id.peek().litValue.toInt < 0x8000)



    }

  }

}
