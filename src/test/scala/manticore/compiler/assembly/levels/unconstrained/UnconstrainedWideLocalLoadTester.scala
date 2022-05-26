package manticore.compiler.assembly.levels.unconstrained


import java.io.File
import java.io.PrintWriter
import java.nio.file.Path
import manticore.compiler.assembly.parser.AssemblyParser
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

    @MEMINIT  [file="${fp
      .toAbsolutePath()}", count = ${init_count}, width =${width}]
    .mem mem_base ${width} ${capacity}
    ${init_vals.zipWithIndex map { case (x, ix) =>
      s".const mvalue_${ix} ${width} ${x}"
    } mkString "\n"}
    .wire index ${addr_bits} 0x0
    .wire mdata ${width}
    .const const_1 ${addr_bits} 1
    .const const_0 ${addr_bits} 0
    .wire matches 1
    ${init_vals.zipWithIndex map { case (x, ix) =>
      Seq(
        s"${memblock}",
        s"(mem_base, ${ix}) LLD mdata, mem_base[index];",
        s"SEQ matches, mdata, mvalue_${ix};",
        s"(${ix}) ASSERT matches;",
        s"ADD index, index, const_1;"
      ) mkString ("\n")

    } mkString ("\n")}
    (${init_vals.length}) FINISH const_1;
    """

  }


  it should "correctly read from memory" taggedAs Tags.WidthConversion in { f =>
    // the test may print warnings about Thyrio that do not matter
    Range(0, 10).foreach { i =>
      val prog_txt = mkProgram(s"ld_test_${i}")(f)
      backend(prog_txt)(f.ctx)
    }


  }

}
