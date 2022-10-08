package manticore.compiler.integration.chisel
import chisel3._
import chiseltest._
import manticore.compiler.AssemblyContext
import manticore.compiler.DefaultHardwareConfig
import manticore.compiler.HasLoggerId
import manticore.compiler.ManticorePasses
import manticore.compiler.UnitFixtureTest
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.assembly.levels.codegen.MachineCodeGenerator
import manticore.compiler.integration.chisel.util.KernelTester
import manticore.compiler.integration.chisel.util.ProgramTester
import manticore.machine.xrt.ManticoreFlatSimKernel
import org.scalatest.flatspec.AnyFlatSpec

import java.io.PrintWriter
import java.nio.file.Path

/** A stress for the NoC implementation.
  */

class NoCStressTest extends KernelTester {

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
    ): (String, String, String) = {

      val tokens_meminit =
        mkMemInit(tokens, output_dir.resolve(s"tokens_${x}_${y}.dat"))
      val (psums, psum_insts) = createChecksumCompute(x, y)
      val psum_decls = psums.map { s => s".wire ${s} 16" } mkString ("\n")
      val test_size = tokens.length

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
       |${tokens_meminit}
       |.mem token_ptr_${x}_${y} 16 ${test_size}
       |
       |${checksum_meminit}
       |.mem checksum_ptr_${x}_${y} 16 ${test_size}
       |
       |.wire checksum_expected_${x}_${y} 16
       |
       |
       |${mkReg(s"counter_${x}_${y}", Some(UInt16(0)))}
    """.stripMargin
      val body =
        s"""
       |
       |// load the token
       |(token_ptr_${x}_${y}, 0) LLD token_${x}_${y}_next, token_ptr_${x}_${y} [counter_${x}_${y}_curr];
       |
       |${psum_insts mkString ("\n")}
       |
       |// load the checksum
       |(checksum_ptr_${x}_${y}, 0) LLD checksum_expected_${x}_${y}, checksum_ptr_${x}_${y} [counter_${x}_${y}_curr];
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
    ): (String, String, String) = {

      val (header, base_decls, base_body) = generateSlaveProcess(
        output_dir,
        0,
        0,
        tokens,
        checksum_meminit,
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

      val correct_expects = corrects.zipWithIndex.map { case (n, i) =>
        s"(0) ASSERT ${n}_curr;"
      } mkString ("\n")

      val body = base_body + s"""
    |
    |${correct_expects}
    |
    |SEQ done_next, counter_0_0_curr, max_count;
    |
    |(1) FINISH done_curr;
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
              checksum_meminit
            )
          } else {
            generateSlaveProcess(
              test_dir,
              x,
              y,
              tokens(x)(y),
              checksum_meminit
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

  def createTestAndCompileAndRun(dimx: Int, dimy: Int)(implicit
      fixture: FixtureParam
  ): Unit = {
    val context = AssemblyContext(
      output_dir = Some(fixture.test_dir.resolve("out").toFile()),
      hw_config = DefaultHardwareConfig(dimX = dimx, dimY = dimy),
      dump_all = true,
      dump_dir = Some(fixture.test_dir.resolve("dumps").toFile()),
      expected_cycles = Some(5), // has to be at least 2
      use_loc = true,
      log_file = Some(fixture.test_dir.resolve("run.log").toFile()),
      debug_message = false
    )
    val source = createTest(
      context.output_dir.get.toPath(),
      context.hw_config.dimX,
      context.hw_config.dimY,
      context.expected_cycles.get - 1 // test size is one less than the expected cycles, because the first checksum
      // is zero, so if we want to have 2 tests, we need to simulate 3 virtual cycles
      // With a test_size = 1 we basically cover anything that can happen NoC-wise,
      // doing more makes little sense... but I do for good measure :D
    )

    new PrintWriter(fixture.test_dir.resolve("src.masm").toFile()) {
      write(source);
      close
    }
    compileAndRun(source, context)
  }

  Seq(
    (2, 2)
    // (3, 3),
    // (4, 4),
    // (5, 5),
    // (6, 6),
    // (7, 7) // simulation can take a long time, so don't go crazy with the dimensions
  ).foreach { case (dimx, dimy) =>
    it should s"correctly compute checksums in a ${dimx}x${dimy} topology" in {
      implicit f =>
        createTestAndCompileAndRun(dimx, dimy)

    }

  }

}
