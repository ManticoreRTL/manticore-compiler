package manticore.compiler.frontend.yosys

import manticore.compiler.ManticoreTransform
import manticore.compiler.AssemblyContext
import java.io.File
import java.nio.file.Path
import java.nio.file.Files
import manticore.compiler.LoggerId

// object ImplicitConversions {
//   implicit def stringToYosysPassProxy(cmd: String) = YosysPassProxy(cmd)
// }



private[yosys] case class YosysPassProxy(
    command: String,
    args: Seq[String] = Nil,
    dependencies: Seq[String] = Nil
) {
  def runsAfter(p: String) = copy(dependencies = dependencies :+ p)
  def addSwitch(s: String) = copy(args = args :+ s)
  def >>(s: String) = addSwitch(s)
}

private[yosys] case class YosysResultProxy private (
    passes: Seq[YosysPassProxy] = Nil
) {
  def andThen(pass: YosysPassProxy) = new YosysResultProxy(passes :+ pass)
}

trait YosysPass extends ManticoreTransform[YosysResultProxy, YosysResultProxy] {

  def passProxy(implicit ctx: AssemblyContext): YosysPassProxy
  override def apply(current: YosysResultProxy)(implicit
      ctx: AssemblyContext
  ): YosysResultProxy = current andThen passProxy
}



object YosysCompilationTransform extends ManticoreTransform[YosysResultProxy, YosysResultProxy] {

    import scala.language.implicitConversions
    implicit def stringToYosysPassProxy(cmd: String) = YosysPassProxy(cmd)

    override def apply(result: YosysResultProxy)(implicit ctx: AssemblyContext): YosysResultProxy = {

        result andThen
        ("hierarchy" >> "-auto-top" >> "-check") andThen
        "proc" andThen
        "opt" andThen
        "op_reduce"


    }

}
// object YosysHierarchyPass extends YosysPass {

//   override def passProxy(implicit ctx: AssemblyContext) =
//     ("hierarchy" addSwitch "-auto-top" addSwitch "-check")

// }


// object YosysReadVerilog
//     extends ManticoreTransform[Iterable[Path], YosysResultProxy] {

//   import YosysCompilationTransform.stringToYosysPassProxy

//   implicit val loggerId = LoggerId("YosysVerilogReader")
//   override def apply(files: Iterable[Path])(implicit
//       ctx: AssemblyContext
//   ): YosysResultProxy = {

//     val read = files.foldLeft(YosysResultProxy()) { case (r, fpath) =>
//       if (!Files.exists(fpath)) {
//         ctx.logger.error(s"File ${fpath.toAbsolutePath()}")
//       }
//       r andThen ("read_verilog" addSwitch "-sv" addSwitch "-masm" addSwitch fpath
//         .toAbsolutePath()
//         .toString())
//     }
//     read
//   }
// }
// object YosysOptPass extends YosysPass {
//   def passProxy(implicit ctx: AssemblyContext) = YosysPassProxy(
//     command = "opt"
//   )
// }

// object YosysOptReducePass extends YosysPass {
//   def passProxy(implicit ctx: AssemblyContext) = "opt_reduce"
// }

// object YosysOptCleanPass extends YosysPass {
//   def passProxy(implicit ctx: AssemblyContext) = "opt_clean"
// }

// object YosysCheckPass extends YosysPass {
//   def passProxy(implicit ctx: AssemblyContext) = "check -assert"
// }

// object YosysMemoryCollectPass extends YosysPass {
//   def passProxy(implicit ctx: AssemblyContext) = "memory_collect"
// }

// object YosysMemoryUnpackPass extends YosysPass {
//   def passProxy(implicit ctx: AssemblyContext) = YosysPassProxy("memory_unpack")
// }

// object ManticoreInitYosysPass extends YosysPass {
//   def passProxy(implicit ctx: AssemblyContext) = YosysPassProxy(
//     command = "manticore_init"
//   )
// }

// object FlattenYosysPass extends YosysPass {
//     def passProxy(implicit ctx: AssemblyContext) YosysPassProxy()
// }
