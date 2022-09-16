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

import java.lang
import java.nio.file.Files
import scala.collection.mutable.ArrayBuffer
class AluChiselTester extends KernelTester with ProcessorTester {

  behavior of "ALU of Mips32 in Chisel"

  override def compiler =
    ManticorePasses.frontend andThen
      ManticorePasses.middleend andThen
      ManticorePasses.backend

  val randGen = new scala.util.Random(231)
  def mkTest(fixture: FixtureParam, dimx: Int, dimy: Int, width: Int, testSize: Int): Unit = {
    require(width <= 64)
    val op1Values = Array.fill(testSize) {
      randGen.nextInt(1 << width)
    }
    val op2Values = Array.fill(testSize) {
      randGen.nextInt(1 << width)
    }
    val ctrlValues = Array.fill(testSize) {
      randGen.between(0, 11)
    }
    val resultValues = Array.tabulate(testSize) { i =>
      ctrlValues(i) match {
        case 0  => op1Values(i) << (31 & op2Values(i))
        case 1  => op1Values(i) >> (31 & op2Values(i))
        case 2  => op1Values(i) >>> (31 & op2Values(i))
        case 3  => op1Values(i) + op2Values(i)
        case 4  => op1Values(i) - op2Values(i)
        case 5  => op1Values(i) & op2Values(i)
        case 6  => op1Values(i) | op2Values(i)
        case 7  => op1Values(i) ^ op2Values(i)
        case 8  => ~(op1Values(i) | op2Values(i))
        case 9  => if (op1Values(i) < op2Values(i)) 1 else 0
        case 10 => op1Values(i) << 16
      }
    }
    val zeroValues = Array.tabulate(testSize) { i =>
      if (resultValues(i) == 0) 1 else 0
    }
    def dumpHex(fname: String, vs: Array[Int]) =
      fixture.dump(fname, vs.map(v => v.toHexString).mkString("\n")).toAbsolutePath()
    val op1_rom    = dumpHex("op1_rom.hex", op1Values)
    val op2_rom    = dumpHex("op2_rom.hex", op2Values)
    val ctrl_rom   = dumpHex("ctrl_rom.hex", ctrlValues)
    val result_rom = dumpHex("result_rom.hex", resultValues)
    val zero_rom   = dumpHex("zero_rom.hex", zeroValues)

    val tbWrapper = WithInlineVerilog(s"""|
                                          |module Main(input wire clock);
                                          |   AluTester #(
                                          |     .TEST_SIZE($testSize),
                                          |     .CTRL_ROM("$ctrl_rom"),
                                          |     .OP1_ROM("$op1_rom"),
                                          |     .OP2_ROM("$op2_rom"),
                                          |     .RESULT_ROM("$result_rom"),
                                          |     .ZERO_ROM("$zero_rom")
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
      dump_all = true,
      dump_dir = Some(fixture.test_dir.resolve("dumps").toFile()),
      expected_cycles = Some(testSize),
      debug_message = true,
      log_file = Some(fixture.test_dir.resolve("run.log").toFile())
      // debug_message = true
    )

    val masmFile = verilogCompiler(
      Seq(tbWrapper.p, WithResource("integration/microbench/alu/mips32/alu.sv").p)
    )
    val source = scala.io.Source.fromFile(masmFile.toFile()).getLines().mkString("\n")

    compileAndRun(source, context)(fixture)
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
    it should s"not fail 32-bit ALU in a ${dimx}x${dimy} topology" in {
      mkTest(_, dimx, dimy, 32, 2)

    }

  }

}
