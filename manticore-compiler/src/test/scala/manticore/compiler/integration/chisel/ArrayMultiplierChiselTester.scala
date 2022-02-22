package manticore.compiler.integration.chisel

import manticore.compiler.integration.chisel.util.KernelTester
import manticore.compiler.AssemblyContext
import manticore.compiler.ManticorePasses

class ArrayMultiplierChiselTester extends KernelTester {

  behavior of "Array Multiplier in Chisel"

  override def compiler =
    ManticorePasses.frontend followedBy
      ManticorePasses.middleend followedBy
      ManticorePasses.backend

  it should "correctly compute results" in { implicit fixture =>
    def getResource(name: String) = scala.io.Source.fromResource(
      s"integration/microbench/baked_tests/array_multiplier/${name}"
    )
    val p_rom =
      fixture.dump("p_rom.data", getResource("p_rom.data").mkString(""))
    val x_rom =
      fixture.dump("x_rom.data", getResource("x_rom.data").mkString(""))
    val y_rom =
      fixture.dump("y_rom.data", getResource("y_rom.data").mkString(""))

    val source: String = getResource("ArrayMultiplierTester.masm")
      .getLines()
      .map { l =>
        l.replace("*p_rom.data*", p_rom.toAbsolutePath().toString())
          .replace("*x_rom.data*", x_rom.toAbsolutePath().toString())
          .replace("*y_rom.data*", y_rom.toAbsolutePath().toString())
      }
      .mkString("\n")

    val context = AssemblyContext(
      output_dir = Some(fixture.test_dir.resolve("out").toFile()),
      max_dimx = 2,
      max_dimy = 2,
      dump_all = true,
      dump_dir = Some(fixture.test_dir.resolve("dumps").toFile()),
      expected_cycles = Some(333)
    )

    compileAndRun(source, context)

  }

}
