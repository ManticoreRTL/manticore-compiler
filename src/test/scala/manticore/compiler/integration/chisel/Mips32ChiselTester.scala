package manticore.compiler.integration.chisel

import manticore.compiler.AssemblyContext
import manticore.compiler.DefaultHardwareConfig
import manticore.compiler.ManticorePasses
import manticore.compiler.WithInlineVerilog
import manticore.compiler.WithResource
import manticore.compiler.assembly.levels.codegen.MachineCodeGenerator
import manticore.compiler.assembly.levels.placed.interpreter.AtomicInterpreter
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.frontend.yosys.Yosys.YosysDefaultPassAggregator
import manticore.compiler.frontend.yosys.YosysRunner
import manticore.compiler.integration.chisel.util.KernelTester
import manticore.compiler.integration.chisel.util.ProcessorTester

import java.nio.file.Files

class Mips32ChiselTester extends KernelTester with ProcessorTester {

  behavior of "Mips32 in Chisel"

  override def compiler =
    ManticorePasses.frontend andThen
      ManticorePasses.middleend andThen
      ManticorePasses.backend

  def mkTest(fixture: FixtureParam, dimx: Int, dimy: Int): Unit = {
    def getResource(name: String) = scala.io.Source.fromResource(
      s"integration/cpu/mips32/${name}"
    )
    val verilogCompiler = YosysDefaultPassAggregator andThen YosysRunner (fixture.test_dir)
    Files.createDirectories(fixture.test_dir.resolve("dumps"))
    implicit val context = AssemblyContext(
      output_dir = Some(fixture.test_dir.resolve("out").toFile()),
      hw_config = DefaultHardwareConfig(dimX = dimx, dimY = dimy),
      dump_all = true,
      dump_dir = Some(fixture.test_dir.resolve("dumps").toFile()),
      expected_cycles = Some(54),
      debug_message = true,
      log_file = Some(fixture.test_dir.resolve("run.log").toFile())
    )

    val instMem =
      fixture.dump("inst_mem.data", getResource("sum.hex").mkString(""))
    val tbWrapper = WithInlineVerilog(s"""|
                                          |module Main (input wire clock);
                                          |  TestVerilator #(
                                          |    .INST_FILE("${instMem.toAbsolutePath().toString()}")
                                          |  ) tb (
                                          |    .clock(clock)
                                          |  );
                                          |endmodule
                                          |""".stripMargin)
    val masmFile = verilogCompiler(
      Seq(tbWrapper.p, WithResource("integration/cpu/mips32/mips32.sv").p)
    )

    val source: String = scala.io.Source.fromFile(masmFile.toFile()).getLines().mkString("\n")

    compileAndRun(source, context)(fixture)
  }
  Seq(
    (1, 1),
    // (2, 2),
    // (3, 3),
    // (4, 4)
  ).foreach { case (dimx, dimy) =>

    it should s"not fail mips32 in a ${dimx}x${dimy} topology" in {
      mkTest(_, dimx, dimy)
    }
  }

}
