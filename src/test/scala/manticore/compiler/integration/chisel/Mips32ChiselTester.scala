package manticore.compiler.integration.chisel

import manticore.compiler.integration.chisel.util.KernelTester
import manticore.compiler.AssemblyContext
import manticore.compiler.ManticorePasses
import manticore.compiler.integration.chisel.util.ProcessorTester

import manticore.compiler.assembly.levels.placed.interpreter.AtomicInterpreter
import manticore.compiler.assembly.levels.codegen.MachineCodeGenerator
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.DefaultHardwareConfig

class Mips32ChiselTester extends KernelTester with ProcessorTester {

  behavior of "Mips32 in Chisel"

  override def compiler =
    ManticorePasses.frontend andThen
      ManticorePasses.middleend andThen
      ManticorePasses.backend


  Seq(
    // (1, 1),
    (2, 2),
    // (3, 3),
    // (4, 4)
  ).foreach { case (dimx, dimy) =>

    it should s"not fail in a ${dimx}x${dimy} topology" in { implicit fixture =>
      def getResource(name: String) = scala.io.Source.fromResource(
        s"integration/cpu/baked_tests/mips32/${name}"
      )
      val inst_mem =
        fixture.dump("inst_mem.data", getResource("sum_inst_mem.data").mkString(""))

      val source: String = getResource("mips32.masm")
        .getLines()
        .map { l =>
          l.replace("*inst_mem.data*", inst_mem.toAbsolutePath().toString())
        }
        .mkString("\n")

      val context = AssemblyContext(
        output_dir = Some(fixture.test_dir.resolve("out").toFile()),
        hw_config = DefaultHardwareConfig(dimX = dimx, dimY = dimy),
        dump_all = true,
        dump_dir = Some(fixture.test_dir.resolve("dumps").toFile()),
        expected_cycles = Some(54),
        debug_message = false
        // log_file = Some(fixture.test_dir.resolve("run.log").toFile())
        // log_file = None
        // debug_message = true
      )

      compileAndRun(source, context)

    }
  }

}
