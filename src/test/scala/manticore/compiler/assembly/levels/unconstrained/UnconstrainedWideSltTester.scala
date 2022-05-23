package manticore.compiler.assembly.levels.unconstrained

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.parser.AssemblyParser

class UnconstrainedWideSltTester extends UnconstrainedWideTest {

  def mkProgram(f: FixtureParam, width: Int): String = {
    // don't pick too large a value, the test will fail because
    // large memories are not handled yet
    val count = 200
    val rs1_vals = Array.fill(count) { mkWideRand(width) }
    val rs2_vals = Array.fill(count) { mkWideRand(width) }

    // not masking the results will make the test fail, obviously.
    val rd_vals = rs1_vals zip rs2_vals map { case (r1, r2) =>
      if (r1 < r2) BigInt(1) else BigInt(0)
    }

    val rs1_fp = f.dump("rs1_vals.dat", rs1_vals)
    val rs2_fp = f.dump("rs2_vals.dat", rs2_vals)
    val rd_fp = f.dump("rd_vals.dat", rd_vals)

    val addr_width = log2Ceil(count)

    val rs1_mb =
      s"@MEMBLOCK[block = \"rs1\", capacity = ${count}, width = ${width}]"
    val rs2_mb =
      s"@MEMBLOCK[block = \"rs2\", capacity = ${count}, width = ${width}]"
    val rd_mb =
      s"@MEMBLOCK[block = \"rd\", capacity = ${count}, width = 1]"
    s"""
    .prog:
        .proc proc_0_0:

            @MEMINIT[file = "${rs1_fp}", count = ${count}, width = ${width}]
            ${rs1_mb}
            .mem rs1_ptr ${addr_width}

            @MEMINIT[file = "${rs2_fp}", count = ${count}, width = ${width}]
            ${rs2_mb}
            .mem rs2_ptr ${addr_width}

            @MEMINIT[file = "${rd_fp}", count = ${count}, width = 1]
            ${rd_mb}
            .mem rd_ptr ${addr_width}

            .wire counter 16 0
            .const const_0 16 0
            .const const_1 16 1
            .const const_ptr_inc ${addr_width} 1
            .const const_max 16 ${count}
            .wire   done  1 0

            .wire rs1_v ${width}
            .wire rs2_v ${width}
            .wire rd_v 1
            .wire rd_ref 1

            ${rs1_mb}
            LD rs1_v, rs1_ptr[0];
            ${rs2_mb}
            LD rs2_v, rs2_ptr[0];
            SLT rd_v, rs1_v, rs2_v;
            ${rd_mb}
            LD rd_ref, rd_ptr[0];
            @TRAP [type = "\\fail"]
            EXPECT rd_ref, rd_v, ["failed"];

            ADD rs1_ptr, rs1_ptr, const_ptr_inc;
            ADD rs2_ptr, rs2_ptr, const_ptr_inc;
            ADD rd_ptr, rd_ptr, const_ptr_inc;
            ADD counter, counter, const_1;
            SEQ done, counter, const_max;
            @TRAP [type = "\\stop"]
            EXPECT done, const_0, ["stopped"];

    """
  }

  behavior of "wide SLT"

  val widthCases = (Seq(1, 2, 3, 4, 8, 17, 18, 32, 33) ++ Seq.fill(70) {
    randgen.nextInt(70) + 1 // + 1 not to have width 0
  }).distinct
  for (w <- widthCases) {

    it should s"should hanle SLT $w" taggedAs Tags.WidthConversion in { f =>
      val prog = mkProgram(f, w)
      val parsed = AssemblyParser(prog, f.ctx)
      backend(parsed, f.ctx)
    }
  }


}
