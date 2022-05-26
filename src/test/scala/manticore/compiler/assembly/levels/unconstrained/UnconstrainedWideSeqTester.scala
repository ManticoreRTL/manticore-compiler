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
    val rs1_fp = f.dump("rs1.dat", rs1_vals)
    val rs2_fp = f.dump("rs2.dat", rs2_vals)
    val rd_fp = f.dump("rd.dat", rd_refs)
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
            .mem rs1_ptr ${width} ${count}
            ${mkMemInit(rs2_fp)}
            .mem rs2_ptr ${width} ${count}
            ${mkMemInit(rd_fp)}
            .mem rd_ptr 1 ${count}

            .wire counter ${addr_width} 0
            .const const_1 16 1
            .const const_0 16 0
            .const const_1_wide ${width} 1
            .const const_ptr_inc ${addr_width} 1
            .const const_max ${addr_width} ${count - 1}
            .wire done 1 0
            .const false 1 0
            .const true 1 1

            .wire rs1_v ${width}
            .wire rs1_u ${width}
            .wire rs2_v ${width}
            .wire cmp_res 1
            .wire rd_ref 1

            ${rs1_mb}
            (rs1_ptr, 0) LLD rs1_v, rs1_ptr[counter];
            ${rs1_mb}
            (rs1_ptr, 1) LLD rs1_u, rs1_ptr[counter];
            ${rs2_mb}
            (rs2_ptr, 0) LLD rs2_v, rs2_ptr[counter];
            ${rd_mb}
            (rd_ptr, 0) LLD rd_ref, rd_ptr[counter];

            // check that reading the same value always yields true

            SEQ cmp_res, rs1_v, rs1_u;
            (0) ASSERT cmp_res;



            // check that comparing different values always yields false
            ADD rs1_u, rs1_u, const_1_wide;
            SEQ cmp_res, rs1_v, rs1_u;
            XOR cmp_res, cmp_res, true;
            (1) ASSERT cmp_res;

            SEQ cmp_res, rs1_v, rs2_v;
            SEQ cmp_res, rd_ref, cmp_res;
            (2) ASSERT cmp_res;

            SEQ done, counter, const_max;
            (3) FINISH done;

            ADD counter, counter, const_ptr_inc;

    """

  }


  it should "correctly compute wide SEQ" taggedAs Tags.WidthConversion in { f =>
    backend(mkProgram(f))(f.ctx)
  }
}
