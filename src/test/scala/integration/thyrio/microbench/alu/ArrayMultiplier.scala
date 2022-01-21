package integration.thyrio.microbench.alu

import integration.ThyrioUnitTest
import integration.thyrio.integration.thyrio.ExternalTool
import integration.thyrio.integration.thyrio.Verilator
import integration.thyrio.integration.thyrio.ThyrioFrontend
import java.nio.file.Path
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import integration.thyrio.integration.thyrio.Make
import manticore.compiler.AssemblyContext
import manticore.assembly.parser.AssemblyParser

class ArrayMultiplier extends ThyrioUnitTest {

  val requiredTools: Seq[ExternalTool] = Seq(Make, Verilator, ThyrioFrontend)

  behavior of "ArrayMultiplier"

  checkInstalled()

  val resource_dir =
    getClass().getResource("/integration/microbench/alu/multiplier").toURI()

  val test_size = 300
  val width = 32
  val randGen = new scala.util.Random(0)
  def generateTest(work_dir: Path): Unit = {

    println("Generating random test")
    assert(width <= 32)
    val mask: Long = 0xffffffffL
    val res_mask: Long = 0xffffffffffffffffL
    val op1 = Seq.fill(test_size) { randGen.nextLong() & mask }
    val op2 = Seq.fill(test_size) { randGen.nextLong() & mask }
    val res = op1 zip op2 map { case (r1, r2) => (r1 * r2) & res_mask }

    def dump(file_name: String, content: String): Path = {
      val fp = work_dir.resolve(file_name)
      val printer = new PrintWriter(fp.toFile())
      printer.print(content)
      printer.close()
      fp
    }

    def toHex(x: Seq[Long]): String = x map { xx => f"${xx}%x" } mkString "\n"

    val fop1 = dump("op1.hex", toHex(op1))
    val fop2 = dump("op2.hex", toHex(op2))
    val fres = dump("res.hex", toHex(res))

    val verilog_src = scala.io.Source
      .fromResource("integration/microbench/alu/multiplier/mult.sv.in")
      .getLines()
      .map { l =>
        l.replace("@W@", width.toString())
          .replace("@TS@", test_size.toString())
          .replace("@XROM@", fop1.toAbsolutePath().toString())
          .replace("@YROM@", fop2.toAbsolutePath().toString())
          .replace("@PROM@", fres.toAbsolutePath().toString())
      } mkString "\n"

    dump("array_mult.sv", verilog_src)
    def copy(fname: String): Unit = Files.copy(
      Path.of(resource_dir).resolve(fname),
      work_dir.resolve(fname),
      StandardCopyOption.REPLACE_EXISTING
    )
    copy("VTester.cpp")
    copy("Makefile")

  }

  def runTest(work_dir: Path)(phase: => ManticoreFrontend.Phase): Unit = {
    val ctx =
      AssemblyContext(
        dump_all = true,
        dump_dir = Some(work_dir.resolve("ArrayMultiplier").toFile()),
        max_cycles = test_size + 200
      )
    val parsed =
      AssemblyParser(
        work_dir.resolve(s"ArrayMultiplierTester.masm").toFile(),
        ctx
      )
    phase(parsed, ctx)
  }

  def testIteration(i: Int)(phases: ManticoreFrontend.Phase): Unit = {
    println(s"Test iteration ${i}")
    val work_dir = root_dir.resolve(s"t${i}")
    Files.createDirectories(work_dir)
    generateTest(work_dir)
    Make.invoke(Seq("verilate_run"), work_dir.toFile()) { println(_) }
    Make.invoke(Seq("thyrio"), work_dir.toFile()) { println(_) }
    runTest(work_dir)(phases)
  }

//   it should "successfully interpret the results before width conversion" in {
//     f =>
//       Range(0, 1) foreach { i =>
//         testIteration(i)(ManticoreFrontend.initialPhases())
//       }

//   }

  it should "successfully interpret the results after width conversion" in {
    f =>
      Range(0, 1) foreach { i =>
        testIteration(i)(
          ManticoreFrontend.initialPhases() followedBy ManticoreFrontend
            .finalPhases()
        )
      }

  }

}
