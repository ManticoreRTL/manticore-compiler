package manticore.compiler.integration.chisel

import manticore.compiler.integration.chisel.util.ProcessorTester
import manticore.compiler.integration.chisel.util.KernelTester
import manticore.compiler.frontend.yosys.Yosys.YosysDefaultPassAggregator
import manticore.compiler.frontend.yosys.YosysRunner
import manticore.compiler.ManticorePasses
import manticore.compiler.AssemblyContext
import manticore.compiler.DefaultHardwareConfig
import java.nio.file.Files

class FifoSequentialWriteTester extends KernelTester with ProcessorTester {

  behavior of "repeated writes to a FIFO"

  override def compiler =
    ManticorePasses.frontend andThen
      ManticorePasses.middleend andThen
      ManticorePasses.backend

  def mkTest(dimx: Int, dimy: Int): Unit =
    "fifo" should s"work in a $dimx x $dimy grid" in { fixture =>
      val verilogCompiler = YosysDefaultPassAggregator andThen YosysRunner(fixture.test_dir)
      val vfile = fixture.dump(
        "main.masm",
        scala.io.Source.fromResource("integration/microbench/fifo/fifo_seq.v").getLines().mkString("\n")
      )
      Files.createDirectories(fixture.test_dir.resolve("dumps"))
      implicit val context = AssemblyContext(
        output_dir = Some(fixture.test_dir.resolve("out").toFile()),
        hw_config = DefaultHardwareConfig(dimX = dimx, dimY = dimy),
        dump_all = true,
        dump_dir = Some(fixture.test_dir.resolve("dumps").toFile()),
        expected_cycles = Some(23),
        debug_message = true,
        log_file = Some(fixture.test_dir.resolve("run.log").toFile())
      )

      val masmFile = verilogCompiler(Seq(vfile))
      val source = scala.io.Source.fromFile(masmFile.toFile).getLines().mkString("\n")

      compileAndRun(source, context)(fixture)


    }

    mkTest(1, 1)

}
