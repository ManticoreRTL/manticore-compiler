package manticore.compiler.integration.chisel

import manticore.compiler.integration.chisel.util.KernelTester
import manticore.compiler.AssemblyContext
import manticore.compiler.ManticorePasses
import manticore.compiler.integration.chisel.util.ProcessorTester

import manticore.compiler.assembly.levels.placed.interpreter.AtomicInterpreter
import manticore.compiler.assembly.levels.codegen.MachineCodeGenerator
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.DefaultHardwareConfig
import manticore.compiler.WithInlineVerilog
import manticore.compiler.frontend.yosys.Yosys.YosysDefaultPassAggregator
import manticore.compiler.frontend.yosys.YosysRunner
import manticore.compiler.WithResource
import java.nio.file.Files
class ArrayMultiplierChiselTester extends KernelTester with ProcessorTester {

  behavior of "Array Multiplier in Chisel"

  override def compiler =
    ManticorePasses.frontend andThen
      ManticorePasses.middleend andThen
      ManticorePasses.backend

  val randGen = new scala.util.Random(231)
  def mkTest(fixture: FixtureParam, dimx: Int, dimy: Int, width: Int, testSize: Int): Unit = {
    require(width <= 64)
    val maxOperandValue = (BigInt(1) << width) - 1
    val xValues = Array.fill(testSize) {
      maxOperandValue & randGen.nextLong()
    }
    val yValues = Array.fill(testSize) {
      maxOperandValue & randGen.nextLong()
    }
    val pValues = xValues zip yValues map { case (x, y) =>
      (x * y) & ((BigInt(1) << (width * 2)) - 1)
    }
    def dumpHex(fname: String, vs: Array[BigInt]) =
      fixture.dump(fname, vs.map(v => v.toString(16)).mkString("\n")).toAbsolutePath()
    val xrom = dumpHex("xrom.hex", xValues)
    val yrom = dumpHex("yrom.hex", yValues)
    val prom = dumpHex("prom.hex", pValues)

    val tbWrapper = WithInlineVerilog(s"""|
                                          |module Main(input wire clock);
                                          |   TestBench #(
                                          |     .XROM_FILE("$xrom"),
                                          |     .YROM_FILE("$yrom"),
                                          |     .PROM_FILE("$prom"),
                                          |     .TEST_SIZE($testSize),
                                          |     .W($width)
                                          |   ) tb (
                                          |     .clock(clock)
                                          |   );
                                          |endmodule
                                          |""".stripMargin)
    val verilogCompiler = YosysDefaultPassAggregator andThen YosysRunner(fixture.test_dir)
    Files.createDirectories(fixture.test_dir.resolve("dumps"))

    implicit val context = AssemblyContext(
      output_dir = Some(fixture.test_dir.resolve("out").toFile()),
      hw_config = DefaultHardwareConfig(
        dimX = dimx,
        dimY = dimy
      ),
      dump_all = false,
      dump_dir = Some(fixture.test_dir.resolve("dumps").toFile()),
      expected_cycles = None,
      debug_message = false,
      log_file = Some(fixture.test_dir.resolve("run.log").toFile())
      // debug_message = true
    )

    val masmFile = verilogCompiler(
      Seq(tbWrapper.p, WithResource("integration/microbench/alu/multiplier/mult.sv").p)
    )
    val source = scala.io.Source.fromFile(masmFile.toFile()).getLines().mkString("\n")

    compileAndRun(source, context)(fixture)
    // val masmSource =
  }
  Seq(
    (1, 1),
    (2, 2),
    (3, 3),
    (4, 4),
    (5, 5),
    (6, 6),
    (7, 7)
  ).foreach { case (dimx, dimy) =>
    it should s"not fail 16-bit multiplier in a ${dimx}x${dimy} topology" in {
      mkTest(_, dimx, dimy, 16, 2)

    }

  }

}
