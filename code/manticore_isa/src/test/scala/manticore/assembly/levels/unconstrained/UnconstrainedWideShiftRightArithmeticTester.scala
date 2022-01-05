package manticore.assembly.levels.unconstrained

import manticore.UnitTest

import manticore.assembly.parser.UnconstrainedAssemblyParser
import manticore.assembly.levels.unconstrained.UnconstrainedIR._
import manticore.assembly.levels.UnconstrainedAssemblyParserTester
import manticore.compiler.AssemblyContext
import manticore.assembly.parser.AssemblyParser
import manticore.UnconstrainedTest
import scala.annotation.tailrec

class UnconstrainedWideShiftRightArithmeticTester
    extends UnconstrainedWideTest {

  behavior of "Unconstrained wide SRA conversion"

  // val randgen = new scala.util.Random()
  // a simple program that shifts '1' to the left 0 to 31 times
  def mkProgram(width: Int) = {
    // val width = 32
    // require(width <= 32)
    val initial_value = (BigInt(1) << (width - 1))
    val rs_init_val: Long = 1.toLong
    def computeExpected(init_val: BigInt): Seq[BigInt] = {
      val sign = init_val >> (width - 1)
      val sign_mask = sign << (width - 1)
      @tailrec
      def generate(xs: Seq[BigInt]): Seq[BigInt] = {
        if (xs.length == width + 1) {
          xs
        } else {
          generate(xs :+ (sign_mask | (xs.last >> 1)))
        }
      }
      generate(Seq(init_val))
    }
    val expected_vals = computeExpected(initial_value).toArray

    val expected_fp = dumpToFile(s"expected_${width}.dat", expected_vals)

    val memblock =
      s"@MEMBLOCK [block = \"expected\", width = ${width}, capacity = ${expected_vals.length}]"

    val addr_width = log2Ceil(expected_vals.length)

    s"""
    .prog:
    .proc proc_0_0:

    ${memblock}
    @MEMINIT [file = "${expected_fp}", count = ${expected_vals.length}, width = ${width}]
    .mem res_ref_ptr ${addr_width}
    .const const_ptr_inc ${addr_width} 1
    .reg sh_amount 16 0
    .reg done 1 0

    .const const_0 16 0
    .const const_1 16 1
    .const sh_amount_max 16 ${expected_vals.length}

    .const const_1_wide ${width} 1
    .wire shifted ${width}

    .const init_val ${width} ${initial_value}
    .wire shifted_ref ${width}


    SRA shifted, init_val, sh_amount;
    ${memblock}
    LLD shifted_ref, res_ref_ptr[0];
    @TRAP [type = "\\fail"]
    // @ECHO
    EXPECT shifted_ref, shifted, ["results mismatch"];

    ADD res_ref_ptr, res_ref_ptr, const_ptr_inc;

    ADD sh_amount, sh_amount, const_1;
    SEQ done, sh_amount, sh_amount_max;

    @TRAP [type = "\\stop"]
    EXPECT done, const_0, ["stopped"];
    """
  }

  // val dump_path = createDumpDirectory()
  val ctx = AssemblyContext(
    dump_all = true,
    dump_dir = Some(dump_path.toFile),
    max_cycles = Int.MaxValue
  )

  val interpreter = UnconstrainedInterpreter
  val backend =
    UnconstrainedBigIntTo16BitsTransform followedBy UnconstrainedInterpreter
  it should "correctly translate wide SRA operations to 16-bit SLLs" taggedAs Tags.WidthConversion in {

    repeat(100) { i =>
      val prog_txt = mkProgram(16 + i)
      val program = AssemblyParser(prog_txt, ctx)
      backend.apply(program, ctx)
    }

  }
}
