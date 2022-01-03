package manticore.assembly.levels.unconstrained

import manticore.UnconstrainedTest
import java.io.File
import java.io.PrintWriter
import java.nio.file.Path
import manticore.assembly.parser.AssemblyParser
import manticore.compiler.AssemblyContext

class UnconstrainedWideLocalLoadTester extends UnconstrainedWideTest {

  behavior of "unconstrained wide load instructions"

  val dump_path = createDumpDirectory()
  def mkInitFile(
      file_name: String,
      count: Int,
      content: Array[BigInt]
  ): Path = {
    require(count <= content.length)
    val fp = dump_path.resolve(file_name)
    val print_writer = new PrintWriter(fp.toFile())
    for (ix <- 0 until count) {
      print_writer.println(content(ix))
    }
    print_writer.close()
    fp
  }

  def mkProgram(program_name: String): String = {

    val capacity = 1 << 10
    val width = 60
    val init_count = 1
    // val min_value = mkWideRand(width - 1)
    val init_vals = Array.fill(init_count) {
      mkWideRand(width)
    }
    val fp = mkInitFile(program_name + ".dat", init_count, init_vals)
    val addr_bits = BigInt(capacity - 1).bitLength + 1
    s"""
    .prog:
    .proc proc_0_0:
    @MEMBLOCK [block="block0", capacity = ${capacity}, width = ${width}]
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
        s"LLD mdata, block_base_ptr[${ix}];",
        "@TRAP [type = \"\\fail\"]",
        s"EXPECT mdata, mvalue_${ix}, [\"e1_${ix}\"];",
        "LLD mdata, index[0];",
        "@TRAP [type = \"\\fail\"]",
        s"EXPECT mdata, mvalue_${ix}, [\"e2_${ix}\"];",
        s"ADD index, index, const_1;"
      ) mkString ("\n")

    } mkString ("\n")}

    """

  }

  val ctx =
    AssemblyContext(dump_all = true, dump_dir = Some(dump_path.toFile()))
  val backend =
    UnconstrainedBigIntTo16BitsTransform followedBy
      UnconstrainedRenameVariables followedBy // to be able to build a dependence graph
      UnconstrainedOrderInstructions followedBy // which is required for resolving memory accesses for LLD and LST
      UnconstrainedInterpreter
  it should "correctly read from memory" taggedAs Tags.WidthConversion in {

    val prog_txt = mkProgram("ld_test")
    println(prog_txt)
    val prog = AssemblyParser(mkProgram("ld_test"), ctx)
    println(prog.serialized)
    backend(prog, ctx)

  }

}
