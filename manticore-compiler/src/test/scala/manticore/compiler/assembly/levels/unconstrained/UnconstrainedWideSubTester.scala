package manticore.compiler.assembly.levels.unconstrained


import java.nio.file.Path
import java.io.PrintWriter
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.parser.AssemblyParser

class UnconstrainedWideSubTester extends UnconstrainedWideTest {

  def mkProgram(f: FixtureParam): String = {
    // don't pick too large a value, the test will fail because
    // large memories are not handled yet
    val count = 200
    val width = randgen.nextInt(90) + 8
    val rs1_vals = Array.fill(count) { mkWideRand(width) }
    val rs2_vals = Array.fill(count) { mkWideRand(width) }

    // not masking the results will make the test fail, obviously.
    val rd_vals = rs1_vals zip rs2_vals map { case (r1, r2) =>
      (r1 - r2) & ((BigInt(1) << width) - 1)
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
      s"@MEMBLOCK[block = \"rd\", capacity = ${count}, width = ${width}]"
    s"""
    .prog:
        .proc proc_0_0:

            @MEMINIT[file = "${rs1_fp}", count = ${count}, width = ${width}]
            ${rs1_mb}
            .mem rs1_ptr ${addr_width}

            @MEMINIT[file = "${rs2_fp}", count = ${count}, width = ${width}]
            ${rs2_mb}
            .mem rs2_ptr ${addr_width}

            @MEMINIT[file = "${rd_fp}", count = ${count}, width = ${width}]
            ${rd_mb}
            .mem rd_ptr ${addr_width}

            .reg counter 16 0
            .const const_0 16 0
            .const const_1 16 1
            .const const_ptr_inc ${addr_width} 1
            .const const_max 16 ${count}
            .reg   done  1 0

            .wire rs1_v ${width}
            .wire rs2_v ${width}
            .wire rd_v ${width}
            .wire rd_ref ${width}

            ${rs1_mb}
            LD rs1_v, rs1_ptr[0];
            ${rs2_mb}
            LD rs2_v, rs2_ptr[0];
            SUB rd_v, rs1_v, rs2_v;
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

  behavior of "unconstrained wide sub"

  it should "correctly handle the wide subtraction" taggedAs Tags.WidthConversion in {
    f =>
      repeat(100) { i =>

        val prog_text = mkProgram(f)
        val parsed = AssemblyParser(prog_text, f.ctx)
        backend(parsed, f.ctx)
      }

  }
}
