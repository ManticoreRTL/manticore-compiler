package manticore.compiler.integration.chisel

import org.scalatest.flatspec.AnyFlatSpec
import manticore.compiler.integration.chisel.util.ProgramTester
import manticore.compiler.assembly.levels.UInt16
import java.nio.file.Path
import manticore.compiler.UnitFixtureTest
import manticore.compiler.AssemblyContext
import manticore.compiler.ManticorePasses
import manticore.machine.xrt.ManticoreFlatKernel
import manticore.machine.xrt.ManticoreFlatSimKernel
import chiseltest._
import manticore.compiler.assembly.levels.codegen.MachineCodeGenerator
import chisel3._

/** A stress for the NoC implementation.
  */

class NoCStressTest
    extends UnitFixtureTest
    with ChiselScalatestTester
    with ProgramTester {

  def createTest(
      test_dir: Path,
      dimx: Int,
      dimy: Int,
      test_size: Int = 1
  ): String = {

    def generate(fn: (Int, Int) => String): String = Range(0, dimx).flatMap {
      x =>
        Range(0, dimy).map { y =>
          fn(x, y)
        }
    } mkString ("\n")

    val reg_decls = generate { case (x, y) => mkReg(s"token_${x}_${y}", None) }
    // def mkDummies(pos_x: Int, pos_y: Int) = generate { case (x, y) =>
    //   mkReg(s"dummy_in_${pos_x}_${pos_y}_${x}_${y}", None)
    // }

    // def dummyMoves(pos_x: Int, pos_y: Int) = generate { case (x, y) =>
    //   s"MOV dummy_in_${pos_x}_${pos_y}_${x}_${y}_next, token_${x}_${y}_curr;"

    // }
    def createChecksumCompute(
        pos_x: Int,
        pos_y: Int
    ): (Seq[String], Seq[String]) = {

      val wires = scala.collection.mutable.Queue.empty[String]
      val insts = scala.collection.mutable.Queue.empty[String]

      Seq
        .tabulate(dimx) { x =>
          Seq.tabulate(dimy) { y => s"token_${x}_${y}_curr" }
        }
        .flatten
        .foldLeft((0, s"zero_${pos_x}_${pos_y}")) {
          case ((index, psum), token) =>
            insts += s"ADD psum_${pos_x}_${pos_y}_${index}, ${token}, ${psum};"
            wires += s"psum_${pos_x}_${pos_y}_${index}"
            (index + 1, s"psum_${pos_x}_${pos_y}_${index}")
        }

      (wires.toSeq, insts.toSeq)
    }
    def generateSlaveProcess(
        output_dir: Path,
        x: Int,
        y: Int,
        tokens: Seq[UInt16],
        checksum_meminit: String,
        checksum_memblock: String
    ): (String, String, String) = {

      val tokens_meminit =
        mkMemInit(tokens, output_dir.resolve(s"tokens_${x}_${y}.dat"))
      val tokens_memblock = mkMemBlock(s"tokens_${x}_${y}.dat", tokens.length)
      val (psums, psum_insts) = createChecksumCompute(x, y)
      val psum_decls = psums.map { s => s".wire ${s} 16" } mkString ("\n")

      val header = s"""
       |@LOC[x = $x, y = $y]
       |.proc p_${x}_${y}:
    """.stripMargin
      val decls = s"""
       |
       |.const zero_${x}_${y} 16 0
       |.const one_${x}_${y}  16 1
       |
       |${reg_decls}
       |
       |${mkReg(s"correct_${x}_${y}", Some(UInt16(1)))}
       |
       |${psum_decls}
       |
       |${tokens_memblock}
       |${tokens_meminit}
       |
       |.mem token_ptr_${x}_${y} 16
       |.wire token_address_${x}_${y} 16
       |
       |${checksum_memblock}
       |${checksum_meminit}
       |.mem checksum_ptr_${x}_${y} 16
       |.wire checksum_address_${x}_${y} 16
       |.wire checksum_expected_${x}_${y} 16
       |
       |
       |${mkReg(s"counter_${x}_${y}", Some(UInt16(0)))}
    """.stripMargin
      val body =
        s"""
       |
       |// load the token
       |ADD token_address_${x}_${y}, token_ptr_${x}_${y}, counter_${x}_${y}_curr;
       |${tokens_memblock}
       |LLD token_${x}_${y}_next, token_address_${x}_${y}[0];
       |
       |${psum_insts mkString ("\n")}
       |
       |// load the checksum
       |ADD checksum_address_${x}_${y}, checksum_ptr_${x}_${y}, counter_${x}_${y}_curr;
       |${checksum_memblock}
       |LLD checksum_expected_${x}_${y}, checksum_address_${x}_${y} [0];
       |
       |SEQ correct_${x}_${y}_next, checksum_expected_${x}_${y}, ${psums.last};
       |
       |
       |ADD counter_${x}_${y}_next, counter_${x}_${y}_curr, one_${x}_${y};
       |
       |
  """.stripMargin

      (header, decls, body)
    }

    def generateMasterProcess(
        output_dir: Path,
        x: Int,
        y: Int,
        tokens: Seq[UInt16],
        checksum_meminit: String,
        checksum_memblock: String
    ): (String, String, String) = {

      val (header, base_decls, base_body) = generateSlaveProcess(
        output_dir,
        0,
        0,
        tokens,
        checksum_meminit,
        checksum_memblock
      )
      val corrects = Seq
        .tabulate(dimx) { x => Seq.tabulate(dimy) { y => (x, y) } }
        .flatten
        .map { case (x, y) =>
          s"correct_${x}_${y}"
        }
      val corrects_decls = corrects.filter(_ != s"correct_0_0").map { n =>
        mkReg(n, Some(UInt16(1)))
      } mkString ("\n")

      val decls = base_decls + s"""
    |
    |.const max_count 16 ${tokens.length}
    |${corrects_decls}
    |${mkReg("done", Some(UInt16(0)))}
    |
    """.stripMargin

      val correct_expects = corrects.map { n =>
        Seq(
          s"@TRAP [ type = \"\\fail\" ]",
          s"EXPECT ${n}_curr, one_0_0, [\"failed_${n}\"];"
        ) mkString ("\n")
      } mkString ("\n")

      val body = base_body + s"""
    |
    |${correct_expects}
    |
    |SEQ done_next, counter_0_0_curr, max_count;
    |
    |@TRAP [ type = "\\stop"]
    |EXPECT done_curr, zero_0_0, ["stop"];
    |
    """.stripMargin

      (header, decls, body)
    }
    val randgen = new scala.util.Random(0)

    val tokens = Seq.fill(dimx) {
      Seq.fill(dimy) {

        Seq.fill(test_size) {
          UInt16(randgen.nextInt(256))
        }

      }
    }

    val checksum: Seq[UInt16] = UInt16(0) +: Seq.tabulate(test_size) { index =>
      Range(0, dimx)
        .flatMap { x =>
          Range(0, dimy).map { y =>
            tokens(x)(y)(index)
          }
        }
        .reduce(_ + _)
    }

    val checksum_memblock = mkMemBlock("checksum_values", checksum.length)
    val checksum_meminit =
      mkMemInit(checksum, test_dir.resolve("checksum_values.dat"))

    val processes = Seq
      .tabulate(dimx) { x =>
        Seq.tabulate(dimy) { y =>
          val (head, decls, body) = if (x == 0 && y == 0) {
            generateMasterProcess(
              test_dir,
              x,
              y,
              tokens(x)(y),
              checksum_meminit,
              checksum_memblock
            )
          } else {
            generateSlaveProcess(
              test_dir,
              x,
              y,
              tokens(x)(y),
              checksum_meminit,
              checksum_memblock
            )
          }
          head + decls + body
        }
      }
      .flatten

    s"""
    |.prog:
    |${processes.mkString("\n")}
    """.stripMargin

  }

  behavior of "Stressed NoC"

  it should "not drop any packets and compute checksums correctly" in {
    fixture =>
      val context = AssemblyContext(
        output_dir = Some(fixture.test_dir.resolve("out").toFile()),
        max_dimx = 2,
        max_dimy = 2,
        dump_all = true,
        dump_dir = Some(fixture.test_dir.resolve("dumps").toFile()),
        expected_cycles = Some(2 + 1),
        use_loc = true,
        // log_file = Some(fixture.test_dir.resolve("run.log").toFile())
        log_file = None,
        debug_message = true
      )
      val source = createTest(
        context.output_dir.get.toPath(),
        context.max_dimx,
        context.max_dimy,
        context.expected_cycles.get - 2
      )

      val program = compile(source, context)
      ManticorePasses.BackendInterpreter(true)(program, context)

      MachineCodeGenerator(program, context)

      test(
        new ManticoreFlatSimKernel(
          DimX = context.max_dimx,
          DimY = context.max_dimy,
          debug_enable = true,
          reset_latency = 12,
          prefix_path =
            fixture.test_dir.resolve("out").toAbsolutePath().toString()
        )
      ).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) {
        dut =>
          dut.clock.step(20)
          dut.io.kernel_ctrl.start.poke(true.B)
          dut.clock.step()
          dut.io.kernel_ctrl.start.poke(false.B)
          dut.clock.setTimeout(40000)
          while (!dut.io.kernel_ctrl.idle.peek().litToBoolean) {
            dut.clock.step()
          }
          assert(
            dut.io.kernel_registers.device.exception_id_0
              .peek()
              .litValue() < 0x8000,
            "execution resulted in fatal exception!"
          )

      }
  }

  // println(
  //   generateProcess(root_dir, 0, 0, Seq(UInt16(1), UInt16(2)), Seq(UInt16(0), UInt16(1)))
  // )
}
