package manticore.compiler.integration.chisel

import chiseltest._

import manticore.machine.core.Processor
import chisel3._
import chisel3.experimental.BundleLiterals._
import manticore.compiler.UnitFixtureTest
import manticore.compiler.AssemblyContext

import manticore.compiler.assembly.levels.placed.PlacedIR._
import manticore.machine.ManticoreBaseISA
import manticore.compiler.assembly.levels.codegen.MachineCodeGenerator
import manticore.machine.core.BareNoCBundle
import manticore.machine.ManticoreBaseISA

class SimpleChiselTester extends UnitFixtureTest with ChiselScalatestTester {

  behavior of "uniprocessor"

  def declReg(name: String, width: Int, init: Option[BigInt]) = {
    Seq(
      s"@REG [id = \"${name}\", type = \"\\REG_CURR\" ]",
      s".input ${name}_curr ${width} ${init.map(_.toString).getOrElse("")}",
      s"@REG [id = \"${name}\", type = \"\\REG_NEXT\" ]",
      s".output ${name}_next ${width} "
    ).mkString("\n")
  }
  def declConst(name: String, width: Int, value: BigInt) =
    s".const ${name} ${width} ${value}"
  def declWire(name: String, width: Int) =
    s".wire ${name} ${width}"

  val program = s"""
    .prog:
    @LOC [x = 0, y = 0]
    .proc p0:
    ${declReg("in_a", 32, Some(BigInt(1234)))}
    ${declReg("in_b", 32, Some(BigInt(4321)))}
    ${declReg("res", 32, None)}
    ${declConst("true", 1, BigInt(1))}
    ${declConst("res_ref", 32, BigInt(1234 + 4321))}
    ${declConst("false", 1, BigInt(0))}
    ${declReg("stop_cond", 1, Some(BigInt(0)))}
    ${declReg("correct", 1, Some(BigInt(1)))}

    @TRAP [type = "\\stop"]
    EXPECT stop_cond_curr, false, ["stop"];
    @TRAP [type = "\\fail"]
    EXPECT correct_curr, true, ["failed"];

    ADD res_next, in_a_curr, in_b_curr;

    SEQ correct_next, res_next, res_ref;
    MOV stop_cond_next, true;


    @LOC [x = 1, y = 0]
    .proc p1:
    ${declReg("res", 32, None)}
    ${declReg("res_copy", 32, None)}
    MOV res_copy_next, res_curr;


    """


  def compiler =
    manticore.compiler.ManticorePasses.frontend followedBy
      manticore.compiler.ManticorePasses.middleend followedBy
      manticore.compiler.assembly.levels.placed.UnconstrainedToPlacedTransform followedBy
      manticore.compiler.ManticorePasses.BackendLowerEnd followedBy
      manticore.compiler.ManticorePasses.BackendInterpreter(true)

  it should "correctly compile and execute a simple program" in { fixture =>
    val output_dir = fixture.test_dir.resolve("compiled")
    implicit val context = AssemblyContext(
      max_dimx = 2,
      max_dimy = 2,
      output_dir = Some(output_dir.toFile()),
      dump_all = true,
      dump_dir = Some(output_dir.toFile()),
      use_loc = true
    )

    val parsed =
      manticore.compiler.assembly.parser.AssemblyParser(program, context)
    val (compiled, _) = compiler(parsed, context)
    val assembled =
      MachineCodeGenerator.assembleProgram(compiled)
    // generate initialization files
    MachineCodeGenerator.generateCode(assembled)

    val proc_under_test = assembled.head

    test(
      new Processor(
        ManticoreBaseISA,
        context.max_dimx,
        context.max_dimy,
        Seq.fill(32)(Seq.fill(16)(0)),
        initial_registers =
          output_dir.resolve("rf_0_0.dat").toAbsolutePath().toString(),
        initial_array =
          output_dir.resolve("ra_0_0.dat").toAbsolutePath().toString()
      )
    ).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) {
      dut =>
        // program the processor with the proc_under_test

        dut.clock.step(10)
        val empty_packet = new BareNoCBundle(ManticoreBaseISA).Lit(
          _.data -> 0.U,
          _.address -> 0.U,
          _.valid -> false.B
        )

        val boot_stream =
          Seq(
            proc_under_test.body.length.toLong
          ) ++ proc_under_test.body.flatMap { w64 =>
            Seq(
              w64 & 0xffff,
              (w64 >> 16L) & 0xffff,
              (w64 >> 32L) & 0xffff,
              (w64 >> 48L) & 0xffff
            )
          } ++ Seq(proc_under_test.epilogue.toLong, 5L, 4L)

        boot_stream.foldLeft(()) { case (_, w: Long) =>
          dut.io.packet_in.poke(
            new BareNoCBundle(ManticoreBaseISA).Lit(
              _.data -> w.U,
              _.address -> 0.U,
              _.valid -> true.B
            )
          )
          dut.clock.step()
        }
        dut.io.packet_in.poke(empty_packet)
        dut.clock.step(100)
        println("Simulation finished")



    }

  }

}
