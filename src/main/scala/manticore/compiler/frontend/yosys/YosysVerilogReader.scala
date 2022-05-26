package manticore.compiler.frontend.yosys

import manticore.compiler.AssemblyContext
import manticore.compiler.LoggerId
import manticore.compiler.FunctionalTransformation

import java.nio.file.Path


object YosysVerilogReader
    extends FunctionalTransformation[Iterable[Path], YosysResultProxy] {

  implicit val loggerId = LoggerId("VerilogReader")
  override def apply(files: Iterable[Path])(implicit
      ctx: AssemblyContext
  ): YosysResultProxy = {

    val read = files.foldLeft(YosysResultProxy()) { case (r, fpath) =>
      r :+ (YosysPassProxy("read_verilog") << "-sv" << "-masm" << fpath
        .toAbsolutePath()
        .toString())
    }
    read
  }


}

