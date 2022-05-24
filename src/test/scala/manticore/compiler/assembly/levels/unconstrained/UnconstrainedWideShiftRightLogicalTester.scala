package manticore.compiler.assembly.levels.unconstrained

import manticore.compiler.UnitTest

import manticore.compiler.assembly.parser.UnconstrainedAssemblyParser
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR._

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.parser.AssemblyParser

class UnconstrainedWideShiftRightLogicalTester extends UnconstrainedWideTest {

  behavior of "Unconstrained wide SRL conversion"

  // a simple program that shifts '1' to the left 0 to 31 times
  def mkProgram(width_rd: Int, width_rs: Int)(f: FixtureParam) = {
    // val width = 32
    // require(width <= 32)
    val initial_value = (BigInt(1) << (width_rs - 1))

    val expected_vals = Array.tabulate(width_rs + 1) { i =>
      (initial_value >> i) & ((BigInt(1) << width_rd) - 1)
    }

    val expected_fp =
      f.dump(s"expected_${width_rd}_${width_rs}.dat", expected_vals)

    val memblock =
      s"@MEMBLOCK [block = \"expected\", width = ${width_rd}, capacity = ${expected_vals.length}]"

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


    SRL shifted, init_val, sh_amount;

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

  private def test(width_rd: Int, width_rs: Int)(f: FixtureParam): Unit = {
    val prog_txt = mkProgram(width_rd, width_rs)(f)
    val program = AssemblyParser(prog_txt, f.ctx)
    backend.apply(program, f.ctx)
  }

  it should "handle width(rd) == width(rs) correctly" taggedAs Tags.WidthConversion in {
    f =>
      repeat(100) { i =>
        val width_rs = 16 + i
        val width_rd = width_rs
        test(width_rd, width_rs)(f)
      }

  }

  it should "handle width(rd) > width(rs) correctly" taggedAs Tags.WidthConversion in {
    f =>
      repeat(100) { i =>
        val width_rs = 16 + i
        val width_rd = width_rs + randgen.nextInt(30)
        test(width_rd, width_rs)(f)
      }
  }

  it should "handle width(rd) < width(rs) correctly" taggedAs Tags.WidthConversion in {
    f =>
      repeat(100) { i =>
        val width_rs = 16 + i
        val width_rd = randgen.nextInt(width_rs) + 1
        test(width_rd, width_rs)(f)
      }
  }

  def mkStaticProgram(
      width_rd: Int,
      width_rs: Int,
      initial_value: BigInt,
      sh_amount: Int
  )(f: FixtureParam): String = {

    val expected_result =
      (initial_value >> sh_amount) & ((BigInt(1) << width_rd) - 1)

    val input_fp = f.dump(
      s"input_value_${width_rd}_${width_rs}.dat",
      Array(initial_value)
    )

    s"""
    .prog:
    .proc proc_0_0:

    .const const_sh_amount 16 ${sh_amount}
    .const ref_result ${width_rd} ${expected_result}
    .wire shifted ${width_rd}


     @MEMINIT [file = "${input_fp}", count = 1, width = ${width_rs}]
    .mem input_ptr ${width_rs} 1
    .wire dyn_rs ${width_rs}
    .const const_0 1 0
    .const const_1 1 1
    .wire matches 1



    (input_prt, 0) LLD dyn_rs, input_ptr[const_0];
    SRL shifted, dyn_rs, const_sh_amount;

    SEQ matches, shifted, ref_result;
    (0) ASSERT matches;

    (1) FINISH const_1;
    """
  }

  private def test_static(width_rd: Int, width_rs: Int, shift_amount: Int)(
      f: FixtureParam
  ): Unit = {
    val txt = mkStaticProgram(
      width_rd,
      width_rs,
      BigInt(1) << (width_rs - 1),
      shift_amount
    )(f)
    val prog = AssemblyParser(txt, f.ctx)
    backend.apply(prog, f.ctx)

  }

  val static_test_cases = Seq
    .fill(2000) {
      val rd_width = randgen.nextInt(70) + 1
      val rs_width = randgen.nextInt(70) + 1
      val shift_amount = randgen.nextInt(rs_width + 1)
      (rd_width, rs_width, shift_amount)
    }
    .distinct

  // println(s"Generated ${static_test_cases.length} static test cases")

  static_test_cases.foreach { case (rd_width, rs_width, shift_amount) =>
    it should s"handle static SRL w${rd_width}, w${rs_width}, ${shift_amount}" taggedAs Tags.WidthConversion in {
      test_static(rd_width, rs_width, shift_amount)(_)
    }
  }

}
