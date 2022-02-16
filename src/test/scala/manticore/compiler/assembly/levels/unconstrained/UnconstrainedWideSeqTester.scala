package manticore.compiler.assembly.levels.unconstrained

import java.nio.file.Path
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.AssemblyContext

class UnconstrainedWideSeqTester extends UnconstrainedWideTest {

  behavior of "wide SEQ"

  def mkProgram(f: FixtureParam): String = {
    val count = 200
    val width = randgen.nextInt(90)
    val rs1_vals = Array.fill(count) { mkWideRand(width) }
    val rs2_vals = Array.fill(count) { mkWideRand(width) }
    val rd_refs = rs1_vals.zip(rs2_vals).map { case (x, y) =>
      if (x == y) BigInt(1) else BigInt(0)
    }
    val rs1_fp = f.dump("rs1", rs1_vals)
    val rs2_fp = f.dump("rs2", rs2_vals)
    val rd_fp = f.dump("rd", rd_refs)
    def mkMb(n: String, w: Int): String =
      s"@MEMBLOCK [ block = \"${n}\", capacity = ${count}, width = ${w} ]"
    def mkMemInit(p: Path): String =
      s"@MEMINIT [ file = \"${p}\", count = ${count}, width = ${width} ]"
    val rs1_mb = mkMb("rs1", width)
    val rs2_mb = mkMb("rs2", width)
    val rd_mb = mkMb("rd", 1)
    val addr_width = log2Ceil(count)
    s"""
    .prog:
        .proc p00:
            ${mkMemInit(rs1_fp)}
            ${rs1_mb}
            .mem rs1_ptr ${addr_width}
            ${mkMemInit(rs2_fp)}
            ${rs2_mb}
            .mem rs2_ptr ${addr_width}
            ${mkMemInit(rd_fp)}
            ${rd_mb}
            .mem rd_ptr ${addr_width}

            .reg counter 16 0
            .const const_1 16 1
            .const const_0 16 0
            .const const_1_wide ${width} 1
            .const const_ptr_inc ${addr_width} 1
            .const const_max 16 ${count}
            .wire done 1 0

            .wire rs1_v ${width}
            .wire rs1_u ${width}
            .wire rs2_v ${width}
            .wire cmp_res 1
            .wire rd_ref 1

            ${rs1_mb}
            LLD rs1_v, rs1_ptr[0];
            ${rs1_mb}
            LLD rs1_u, rs1_ptr[0];
            ${rs2_mb}
            LLD rs2_v, rs2_ptr[0];
            ${rd_mb}
            LLD rd_ref, rd_ptr[0];

            // check that reading the same value always yields true
            SEQ cmp_res, rs1_v, rs1_u;
            @TRAP [type = "\\fail"]
            EXPECT const_1, cmp_res, ["expected equals!"];



            // check that comparing different values always yields false
            ADD rs1_u, rs1_u, const_1_wide;
            SEQ cmp_res, rs1_v, rs1_u;
            @TRAP [type = "\\fail"]
            EXPECT const_0, cmp_res, ["expected not equals!"];

            SEQ cmp_res, rs1_v, rs2_v;
            @TRAP [type = "\\fail"]
            EXPECT cmp_res, rd_ref, ["reference not matched!"];

            ADD rs1_ptr, rs1_ptr, const_ptr_inc;
            ADD rs2_ptr, rs2_ptr, const_ptr_inc;
            ADD rd_ptr, rd_ptr, const_ptr_inc;
            ADD counter, counter, const_1;
            SEQ done, counter, const_max;
            @TRAP [type = "\\stop"]
            EXPECT done, const_0, ["stopped"];

    """

  }


  it should "correctly compute wide SEQ" taggedAs Tags.WidthConversion in { f =>
    val prog = AssemblyParser(mkProgram(f), f.ctx)
    backend(prog, f.ctx)
  }
}
