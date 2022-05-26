package manticore.compiler.assembly.levels.unconstrained

import java.nio.file.Path
import java.io.PrintWriter
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.UnitFixtureTest

class UnconstrainedWideAddTester extends UnconstrainedWideTest {

  def mkProgram(f: FixtureParam): String = {
    // don't pick too large a value, the test will fail because
    // large memories are not handled yet
    val count = 200
    val width = randgen.nextInt(90)
    val rs1_vals = Array.fill(count) { mkWideRand(width) }
    val rs2_vals = Array.fill(count) { mkWideRand(width) }

    // not masking the results will make the test fail, obviously.
    val rd_vals = rs1_vals zip rs2_vals map { case (r1, r2) =>
      (r1 + r2) & ((BigInt(1) << width) - 1)
    }

    val rs1_fp = f.dump("rs1_vals.dat", rs1_vals)
    val rs2_fp = f.dump("rs2_vals.dat", rs2_vals)
    val rd_fp = f.dump("rd_vals.dat", rd_vals)

    val addr_width = log2Ceil(count)


    s"""
    .prog:
        .proc proc_0_0:

            @MEMINIT[file = "${rs1_fp}", count = ${count}, width = ${width}]
            .mem rs1_ptr ${width} ${count}

            @MEMINIT[file = "${rs2_fp}", count = ${count}, width = ${width}]
            .mem rs2_ptr ${width} ${count}

            @MEMINIT[file = "${rd_fp}", count = ${count}, width = ${width}]
            .mem rd_ptr ${width} ${count}


            .wire counter ${addr_width} 0
            .const const_0 16 0
            .const const_1 16 1
            .const counter_incr ${addr_width} 1
            .const counter_max ${addr_width} ${count - 1}
            .wire done  1
            .wire matches 1

            .wire rs1_v ${width}
            .wire rs2_v ${width}
            .wire rd_v ${width}
            .wire rd_ref ${width}


            (rs1_ptr, 0) LD rs1_v, rs1_ptr[counter];

            (rs2_ptr, 0) LD rs2_v, rs2_ptr[counter];

            ADD rd_v, rs1_v, rs2_v;

            (rd_ptr, 0) LD rd_ref, rd_ptr[counter];

            SEQ matches, rd_ref, rd_v;

            (0) ASSERT matches;

            SEQ done, counter, counter_max;
            (1) FINISH done;
            ADD counter, counter, counter_incr;


    """
  }

  behavior of "unconstrained wide adder"

  it should "correctly handle the addition carry" taggedAs Tags.WidthConversion in {
    f =>
      Range(0, 100) foreach { i =>
        val prog_text = mkProgram(f)

        backend(prog_text)(f.ctx)
      }

  }
}
