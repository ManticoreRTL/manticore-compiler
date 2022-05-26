package manticore.compiler.assembly.optimizations

import java.nio.file.Path
import java.io.PrintWriter
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.UnitFixtureTest
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRConstantFolding
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedCloseSequentialCycles
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedInterpreter
import manticore.compiler.UnitTestMatchers
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.assembly.levels.ConstType

class ConstantFoldingSliceTest  extends UnitFixtureTest with UnitTestMatchers {

  val large_constant = BigInt("011111101101001011100111110010100111000000111100010001100110000110110001010011110111110101010111111100110010110111100010100000001110101010001011010100011110", 2)
  val slice_offset = 10
  val slice_length = 15
  val sliced_constant = (large_constant >> slice_offset) & ((BigInt(1) << slice_length) - 1)

  val randGen = new scala.util.Random(0)

  def genRandInt(width: Int): BigInt = {
    BigInt(randGen.nextInt()) & ((BigInt(1) << width) - 1)
  }

  def log2Ceil(x: Int): Int = {
    require(x > 0)
    BigInt(x - 1).bitLength
  }

  // Creates a program that adds numbers in an array by a value X. The value X is
  // a slice of a constant defined in the program. We check that the addition
  // results are indeed correct and that a new constant (corresponding to the sliced
  // region of the original constant) have been introduced in the program.
  def mkProgram(f: FixtureParam): String = {
    // don't pick too large a value, the test will fail because
    // large memories are not handled yet
    val count = 200

    val rs1_vals = Array.fill(count) { genRandInt(16) }

    // not masking the results will make the test fail, obviously.
    val rd_vals = rs1_vals.map { r1 =>
      (r1 + sliced_constant) & ((BigInt(1) << 16) - 1)
    }

    val rs1_fp = f.dump("rs1_vals.dat", rs1_vals)
    val rd_fp = f.dump("rd_vals.dat", rd_vals)

    val addr_width = log2Ceil(count)

    val rs1_mb =
      s"@MEMBLOCK[block = \"rs1\", capacity = ${count}, width = 16]"
    val rd_mb =
      s"@MEMBLOCK[block = \"rd\", capacity = ${count}, width = 16]"
    s"""
    .prog:
        .proc proc_0_0:

            .const large_const ${large_constant.bitLength} 0b${large_constant.toString(2)}

            @MEMINIT[file = "${rs1_fp}", count = ${count}, width = 16]
            ${rs1_mb}
            .mem rs1_ptr ${addr_width}

            @MEMINIT[file = "${rd_fp}", count = ${count}, width = 16]
            ${rd_mb}
            .mem rd_ptr ${addr_width}

            .wire counter 16 0
            .wire done 1 0
            .const const_0 16 0
            .const const_1 16 1
            .const const_ptr_inc ${addr_width} 1
            .const const_max 16 ${count}

            .wire rs1_v 16
            .wire rs2_v 16
            .wire rd_v 16
            .wire rd_ref 16

            ${rs1_mb}
            LD rs1_v, rs1_ptr[0];
            SLICE rs2_v, large_const, ${slice_offset}, ${slice_length};
            ADD rd_v, rs1_v, rs2_v;
            SLICE rd_copy, rd_v, 0, 16; // alias of rd_v
            ${rd_mb}
            LD rd_ref, rd_ptr[0];
            @TRAP [type = "\\fail"]
            EXPECT rd_ref, rd_copy, ["failed"];

            ADD rs1_ptr, rs1_ptr, const_ptr_inc;
            ADD rd_ptr, rd_ptr, const_ptr_inc;
            ADD counter, counter, const_1;
            SEQ done, counter, const_max;
            @TRAP [type = "\\stop"]
            EXPECT done, const_0, ["stopped"];
    """
  }

  behavior of "unconstrained wide slice"

  it should "correctly remove the slice" in { f =>
    val prog_text = mkProgram(f)
    val parsed = AssemblyParser(prog_text)(f.ctx)

    println(parsed.serialized)

    val compiler =
      UnconstrainedIRConstantFolding andThen
      UnconstrainedCloseSequentialCycles

    val lowered = compiler(parsed)(f.ctx)
    println(lowered.serialized)

    // There is only 1 proc.
    val proc = lowered.processes.head

    withClue("No slices should exist in the lowered program:") {
      val numSlices = proc.body.count {
        case _: UnconstrainedIR.Slice => true
        case _ => false
      }
      numSlices shouldBe 0
    }

    withClue("The process should contain a definition for the sliced constant:") {
      val numExpectedConsts = proc.registers.count { reg =>
        reg.value match {
          case Some(const) => const == sliced_constant
          case _ => false
        }
      }
      numExpectedConsts > 0
    }

    UnconstrainedInterpreter(lowered)(f.ctx)
  }
}
