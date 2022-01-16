package manticore.assembly.levels.unconstrained

import manticore.compiler.AssemblyContext
import manticore.assembly.parser.AssemblyParser

class UnconstrainedWideSltsTester extends UnconstrainedWideTest {

  def mkProgram(): String = {

    val width = randgen.nextInt(100) + 16
    val rand_val = mkWideRand(width)
    val unsigned_width = rand_val.bitLength
    val signed_width = unsigned_width + 1
    val pos_val = rand_val
    val neg_val = rand_val + (BigInt(1) << unsigned_width)



    s"""
    .prog:
        .proc p00:
            .const const_wide_0 ${signed_width} 0
            .wire pos_value ${signed_width} 0b0${pos_val.toString(2)}
            .wire neg_value ${signed_width} 0b${neg_val.toString(2)}
            .wire cmp_res 1
            .const const_true 1 1
            .const const_false 1 0

            SLTS cmp_res, pos_value, const_wide_0;
            @TRAP [type = "\\fail"]
            EXPECT const_false, cmp_res, ["pos_val is not negative"];

            SLTS cmp_res, neg_value, const_wide_0;
            @TRAP [type = "\\fail"]
            EXPECT const_true, cmp_res, ["neg_val is negative"];

            @TRAP [type = "\\stop"]
            EXPECT const_true, const_false, ["stopped"];
        """

  }



  val ctx = AssemblyContext(dump_all = true, dump_dir = Some(dump_path.toFile))

  val interpreter = UnconstrainedInterpreter


  behavior of "wide STLS"

  it should "not throw user exceptions for a correct program" taggedAs Tags.WidthConversion in {

    repeat(200) { i =>

        val prog = mkProgram()
        println(prog)
        val parsed = AssemblyParser(prog, ctx)
        backend(parsed, ctx)

    }

  }

}
