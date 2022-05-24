package manticore.compiler.assembly.levels.unconstrained


import java.nio.file.Path
import java.io.PrintWriter
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.parser.AssemblyParser

class UnconstrainedWideMuxTester extends UnconstrainedWideTest {

  def mkProgram(f: FixtureParam): String = {

    val width = randgen.nextInt(90) + 8
    val rs1_val = mkWideRand(width)
    val rs2_val = mkWideRand(width)
    val sel_val = randgen.nextInt(2)

    s"""
    .prog:
        .proc proc_0_0:

            .const v1 ${width} ${rs1_val}
            .wire rs1_u ${width}
            .const v2 ${width} ${rs2_val}
            .wire rs2_u ${width}
            .const v3 1 ${sel_val}
            .wire sel 1
            .const expected ${width} ${if (sel_val == 1) rs2_val else rs1_val}
            .const const_0 16 0
            .const const_1 16 1
            .const const_wide_0 ${width} 0
            .const const_false 1 0
            .const const_true 1 1
            .wire matches 1
            .wire res ${width}

            ADD rs1_u, v1, const_wide_0;
            ADD rs2_u, v2, const_wide_0;
            ADD sel, v3, const_false;
            MUX res, sel, rs1_u, rs2_u;
            SEQ matches, res, expected;
            (0) ASSERT matches;
            (1) FINISH const_true;


    """
  }

  behavior of "unconstrained wide mux"


  it should "correctly handle the wide mux" taggedAs Tags.WidthConversion in { f =>

    repeat(100) { i =>
      val prog_text = mkProgram(f)

      val parsed = AssemblyParser(prog_text, f.ctx)

      backend(parsed, f.ctx)
    }

  }
}
