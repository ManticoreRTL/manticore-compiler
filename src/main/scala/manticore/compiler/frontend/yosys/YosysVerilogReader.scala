package manticore.compiler.frontend.yosys

import manticore.compiler.AssemblyContext
import manticore.compiler.LoggerId
import manticore.compiler.FunctionalTransformation

import java.nio.file.Path

trait YosysVerilogReader extends FunctionalTransformation[Iterable[Path], YosysResultProxy]

object YosysVerilogReader extends FunctionalTransformation[Iterable[Path], YosysResultProxy] {

  private class Impl(extraArgs: Iterable[String]) extends YosysVerilogReader {

    private val cmd = YosysPassProxy("read_verilog") << "-sv" << "-masm" << extraArgs.mkString(" ")

    override def apply(
        files: Iterable[Path]
    )(implicit
        ctx: AssemblyContext
    ): YosysResultProxy = {

      val reader = cmd << files.map(_.toAbsolutePath()).mkString(" ")
      YosysResultProxy() :+ reader
    }
  }

  def deferred: YosysVerilogReader = new Impl(List("-defer"))

  implicit val loggerId = LoggerId("VerilogReader")
  override def apply(
      files: Iterable[Path]
  )(implicit
      ctx: AssemblyContext
  ): YosysResultProxy = {
    val impl = new Impl(Nil)
    impl(files)

  }

}
