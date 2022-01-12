package manticore.assembly.levels.unconstrained

import manticore.UnconstrainedTest
import java.io.File
import java.io.PrintWriter
import java.nio.file.Path
import manticore.assembly.parser.AssemblyParser
import manticore.compiler.AssemblyContext

class UnconstrainedWideLocalStoreTester extends UnconstrainedWideTest {

  behavior of "unconstrained wide local store instructions"



  def mkProgram(program_name: String): String = {

    val capacity = randgen.nextInt(100)
    val width = randgen.nextInt(82)
    val init_count = randgen.nextInt(20)
    // val min_value = mkWideRand(width - 1)
    val init_vals = Array.fill(init_count) {
      mkWideRand(width)
    }
    val memblock = s"@MEMBLOCK [block=\"block0\", capacity = ${capacity}, width = ${width}]"
    val addr_bits = log2Ceil(capacity)
    s"""
    .prog:
    .proc proc_0_0:
    ${memblock}
    .mem block_base_ptr ${addr_bits}
    ${init_vals.zipWithIndex map { case (x, ix) =>
      s".const mvalue_${ix} ${width} ${x}"
    } mkString "\n"}
    .reg index ${addr_bits} 0x0
    .reg mwdata ${width}
    .reg mrdata ${width}
    .const const_1 ${addr_bits} 1
    .const const_0 ${addr_bits} 0
    ADD index, block_base_ptr, const_0;
    ${init_vals.zipWithIndex.map { case (x, ix) =>
      Seq(
        s"${memblock}",
        s"LST mvalue_${ix}, index[0], const_1;",
        s"${memblock}",
        "LLD mrdata, index[0];",
        "@TRAP [type = \"\\fail\"]",
        s"EXPECT mvalue_${ix}, mrdata, [\"e2_${ix}\"];",
        s"ADD index, index, const_1;"
      ) mkString ("\n")

    } mkString ("\n")}
    @TRAP [type = "\\stop"]
    EXPECT const_1, const_0, ["end"];
    """

  }

  val ctx =
    AssemblyContext(dump_all = true, dump_dir = Some(dump_path.toFile()))
  val backend =
    UnconstrainedBigIntTo16BitsTransform followedBy
      UnconstrainedRenameVariables followedBy // to be able to build a dependence graph
      // UnconstrainedOrderInstructions followedBy // which is required for resolving memory accesses for LLD and LST
      UnconstrainedInterpreter
  it should "correctly read from memory" taggedAs Tags.WidthConversion in {
    // the test may print warnings about Thyrio that do not matter
    Range(0, 10).foreach { i =>
      val prog_txt = mkProgram(s"ld_test_${i}")
      val prog = AssemblyParser(prog_txt, ctx)
      println(prog.serialized)
      backend(prog, ctx)
    }


  }

}
