package manticore.compiler.assembly.levels.unconstrained

import manticore.compiler.UnitTest

import manticore.compiler.assembly.parser.UnconstrainedAssemblyParser
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR._

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.parser.AssemblyParser

import scala.annotation.tailrec

class UnconstrainedWideShiftRightArithmeticTester
    extends UnconstrainedWideTest {

  behavior of "Unconstrained wide SRA conversion"

  // val randgen = new scala.util.Random()
  // a simple program that shifts '1' to the left 0 to 31 times
  def mkProgram(width_rd: Int, width_rs: Int, pos: Boolean)(f: FixtureParam) = {
    // val width = 32
    // require(width <= 32)
    val initial_value =
      if (pos) (BigInt(1) << (width_rs - 2)) else (BigInt(1) << (width_rs - 1))

    def computeExpected(init_val: BigInt): Seq[BigInt] = {
      val sign = init_val >> (width_rs - 1)
      val rd_max = (BigInt(1) << width_rd) - 1
      val sign_bit =
        if (sign == 1) ((rd_max << (width_rs - 1)) & rd_max) else BigInt(0)
      @tailrec
      def generate(xs: Seq[BigInt]): Seq[BigInt] = {
        if (xs.length == width_rs + 1) {
          xs
        } else {
          generate(xs :+ (sign_bit | (xs.last >> 1)))
        }
      }
      generate(Seq(sign_bit | init_val))
    }
    val expected_vals = computeExpected(initial_value).toArray

    val expected_fp =
      f.dump(s"expected_${width_rd}_${width_rs}_${pos}.dat", expected_vals)



    val addr_width = log2Ceil(expected_vals.length)

    s"""
    .prog:
    .proc proc_0_0:


    @MEMINIT [file = "${expected_fp}", count = ${expected_vals.length}, width = ${width_rd}]
    .mem res_ref_ptr ${width_rd} ${expected_vals.length}
    .wire counter ${addr_width} 0
    .const const_ptr_inc ${addr_width} 1
    .wire sh_amount 16 0
    .wire done 1 0
    .wire matches 1

    .const const_0 16 0
    .const const_1 16 1
    .const sh_amount_max 16 ${expected_vals.length}


    .wire shifted ${width_rd}

    .const init_val ${width_rs} ${initial_value}
    .wire shifted_ref ${width_rd}


    SRA shifted, init_val, sh_amount;

    (res_ref_ptr, 0) LLD shifted_ref, res_ref_ptr[counter];


    @TRAP [type = "\\fail"]
    // @ECHO
    EXPECT shifted_ref, shifted, ["results mismatch"];
    SEQ matches, shifted_ref, shifted;

    (0) ASSERT matches;

    ADD sh_amount, sh_amount, const_1;
    SEQ done, sh_amount, sh_amount_max;
    (1) FINISH done;

    ADD counter, counter, const_ptr_inc;
    """
  }

  private def test(width_rd: Int, width_rs: Int, pos: Boolean)(
      f: FixtureParam
  ): Unit = {
    val prog_txt = mkProgram(width_rd, width_rs, pos)(f)
    backend.apply(prog_txt)(f.ctx)
  }

  it should "handle width(rd) = width(rs) < 16 and rs < 0" taggedAs Tags.WidthConversion in {
    f =>
      Range(1, 16) foreach { i =>
        test(i, i, false)(f)
      }
  }
  it should "handle width(rd) = width(rs) < 16 and rs > 0" taggedAs Tags.WidthConversion in {
    f =>
      Range(1, 16) foreach { i =>
        test(i, i, true)(f)
      }
  }

  it should "handle width(rd) = width(rs) = 16 and rs < 0" taggedAs Tags.WidthConversion in {
    f =>
      test(16, 16, true)(f)
  }

  it should "handle width(rd) = width(rs) = 16 and rs > 0" taggedAs Tags.WidthConversion in {
    f =>
      test(16, 16, false)(f)
  }

  it should "handle width(rd) = width(rs) > 16 and rs < 0" taggedAs Tags.WidthConversion in {
    f =>
      Range(17, 100) foreach { i => test(i, i, false)(f) }
  }

  it should "handle width(rd) = width(rs) > 16 and rs > 0" taggedAs Tags.WidthConversion in {
    f =>
      Range(17, 100) foreach { i => test(i, i, true)(f) }
  }

}
