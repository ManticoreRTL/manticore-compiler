package manticore.assembly.levels.unconstrained


import java.io.File
import java.io.PrintWriter
import java.nio.file.Path
import manticore.assembly.parser.AssemblyParser
import manticore.compiler.AssemblyContext

class UnconstrainedWideLocalLoadTester extends UnconstrainedWideTest {

  behavior of "unconstrained wide local load instructions"


  def mkProgram(program_name: String)(f: FixtureParam): String = {

    val capacity = randgen.nextInt(100)
    val width = randgen.nextInt(82)
    val init_count = randgen.nextInt(capacity )
    // val min_value = mkWideRand(width - 1)
    val init_vals = Array.fill(init_count) {
      mkWideRand(width)
    }
    val fp = f.dump(program_name + ".dat", init_vals)
    val addr_bits = log2Ceil(capacity)
    val memblock = s"@MEMBLOCK [block=\"block0\", capacity = ${capacity}, width = ${width}]"
    s"""
    .prog:
    .proc proc_0_0:
    ${memblock}
    @MEMINIT  [file="${fp
      .toAbsolutePath()}", count = ${init_count}, width =${width}]
    .mem block_base_ptr ${addr_bits}
    ${init_vals.zipWithIndex map { case (x, ix) =>
      s".const mvalue_${ix} ${width} ${x}"
    } mkString "\n"}
    .reg index ${addr_bits} 0x0
    .reg mdata ${width}
    .const const_1 ${addr_bits} 1
    .const const_0 ${addr_bits} 0
    ADD index, block_base_ptr, const_0;
    ${init_vals.zipWithIndex map { case (x, ix) =>
      Seq(
        s"${memblock}",
        s"LLD mdata, block_base_ptr[${ix}];",
        "@TRAP [type = \"\\fail\"]",
        s"EXPECT mdata, mvalue_${ix}, [\"e1_${ix}\"];",
        s"${memblock}",
        "LLD mdata, index[0];",
        "@TRAP [type = \"\\fail\"]",
        s"EXPECT mdata, mvalue_${ix}, [\"e2_${ix}\"];",
        s"ADD index, index, const_1;"
      ) mkString ("\n")

    } mkString ("\n")}
    @TRAP [type = "\\stop"]
    EXPECT const_1, const_0, ["end"];
    """

  }


  it should "correctly read from memory" taggedAs Tags.WidthConversion in { f =>
    // the test may print warnings about Thyrio that do not matter
    Range(0, 10).foreach { i =>
      val prog_txt = mkProgram(s"ld_test_${i}")(f)
      val prog = AssemblyParser(prog_txt, f.ctx)

      backend(prog, f.ctx)
    }


  }

}
