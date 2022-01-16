package manticore.assembly.levels.unconstrained

import manticore.UnitTest

import manticore.assembly.parser.UnconstrainedAssemblyParser
import manticore.assembly.levels.unconstrained.UnconstrainedIR._
import manticore.assembly.levels.UnconstrainedAssemblyParserTester
import manticore.compiler.AssemblyContext
import manticore.assembly.parser.AssemblyParser
import manticore.UnconstrainedTest

class UnconstrainedWideShiftRightLogicalTester extends UnconstrainedWideTest {

  behavior of "Unconstrained wide SRL conversion"

  // a simple program that shifts '1' to the left 0 to 31 times
  def mkProgram(width_rd: Int, width_rs: Int) = {
    // val width = 32
    // require(width <= 32)
    val initial_value = (BigInt(1) << (width_rs - 1))

    val expected_vals = Array.tabulate(width_rs + 1) { i =>
      (initial_value >> i) & ((BigInt(1) << width_rd) - 1)
    }

    val expected_fp =
      dumpToFile(s"expected_${width_rd}_${width_rs}.dat", expected_vals)

    val memblock =
      s"@MEMBLOCK [block = \"expected\", width = ${width_rd}, capacity = ${expected_vals.length}]"

    val addr_width = log2Ceil(expected_vals.length)

    s"""
    .prog:
    .proc proc_0_0:

    ${memblock}
    @MEMINIT [file = "${expected_fp}", count = ${expected_vals.length}, width = ${width_rd}]
    .mem res_ref_ptr ${addr_width}
    .const const_ptr_inc ${addr_width} 1
    .reg sh_amount 16 0
    .reg done 1 0

    .const const_0 16 0
    .const const_1 16 1
    .const sh_amount_max 16 ${expected_vals.length}

    .wire shifted ${width_rd}

    .const init_val ${width_rs} ${initial_value}
    .wire shifted_ref ${width_rd}


    SRL shifted, init_val, sh_amount;
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



  private def test(width_rd: Int, width_rs: Int): Unit = {
    val prog_txt = mkProgram(width_rd, width_rs)
    val program = AssemblyParser(prog_txt, ctx)
    backend.apply(program, ctx)
  }
  it should "handle width(rd) == width(rs) correctly" taggedAs Tags.WidthConversion in {

    repeat(100) { i =>
      val width_rs = 16 + i
      val width_rd = width_rs
      test(width_rd, width_rs)
    }

  }

  it should "handle width(rd) > width(rs) correctly" taggedAs Tags.WidthConversion in {
    repeat(100) { i =>
      val width_rs = 16 + i
      val width_rd = width_rs + randgen.nextInt(30)
      test(width_rd, width_rs)
    }
  }

  it should "handle width(rd) < width(rs) correctly" taggedAs Tags.WidthConversion in {
    repeat(100) { i =>
      val width_rs = 16 + i
      val width_rd = randgen.nextInt(width_rs) + 1
      test(width_rd, width_rs)
    }
  }
}
