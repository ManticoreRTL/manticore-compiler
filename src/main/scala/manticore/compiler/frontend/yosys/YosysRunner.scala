package manticore.compiler.frontend.yosys

import manticore.compiler.AssemblyContext
import manticore.compiler.FunctionalTransformation
import manticore.compiler.assembly.levels.TransformationID

import java.nio.file.Files
import java.nio.file.Path

object YosysRunner extends FunctionalTransformation[YosysResultProxy, Path] {

  def apply(program: YosysResultProxy)(implicit ctx: AssemblyContext): Path = {
    val runDir = Files.createTempDirectory("yosys_runner")
    val backend =
      YosysBackendProxy("manticore_writer", runDir.resolve("main.masm"))
    val runner = new YosysRunner(runDir, backend)
    runner(program)
  }

  def apply(runDir: Path, backend: YosysBackendProxy): YosysRunner = {
    val runner = new YosysRunner(runDir, backend)
    runner
  }
  def apply(runDir: Path): YosysRunner = {
    val backend =
      YosysBackendProxy("manticore_writer", runDir.resolve("main.masm"))
    val runner = new YosysRunner(runDir, backend)
    runner
  }

  def apply(backend: YosysBackendProxy): YosysRunner = {
    val runDir = Files.createTempDirectory("yosys_runner")
    val runner = new YosysRunner(runDir, backend)
    runner
  }

}

final class YosysRunner private (runDir: Path, backend: YosysBackendProxy)
    extends FunctionalTransformation[YosysResultProxy, Path] {

  implicit private val loggerId = TransformationID("YosysRunner")
  import scala.sys.process.{ProcessLogger, Process}
  import java.nio.file.Files

  def withDirectory(newRunDir: Path) = new YosysRunner(newRunDir, backend)
  def withBackend(newBackend: YosysBackendProxy) =
    new YosysRunner(runDir, newBackend)

  override def apply(
      resultProxy: YosysResultProxy
  )(implicit ctx: AssemblyContext): Path = {

    ctx.logger.debug(s"Yosys run directory is ${runDir.toAbsolutePath()}")

    val manticoreWriter =
      YosysPassProxy(
        backend.command,
        backend.args
      ) << backend.filename.toAbsolutePath().toString()

    val allPasses = ctx.dump_dir match {
      case Some(dDir) if ctx.dump_all =>
        var i = 0
        val withDumps = resultProxy.passes.flatMap {
          case yp @ YosysPassProxy(ycmd, _, _) =>
            val index = i
            i = i + 1
            Seq(
              yp,
              Yosys.RtlIlWriter << dDir.toPath
                .resolve(s"y${i}_post_${ycmd}.rtl")
                .toAbsolutePath()
                .toString()
            )
        }
        withDumps :+ manticoreWriter
      case _ =>
        resultProxy.passes :+ manticoreWriter

    }

    val allCmds = allPasses.foldLeft("") {
      case (script, YosysPassProxy(command, args, _)) =>
        script + s"${command} ${args.mkString(" ")}; "
    }

    val yosysLog = new StringBuilder
    val processLogger = ProcessLogger(ln => yosysLog ++= s"${ln}\n")
    val processCmd = s"yosys -p \"${allCmds}\" -T -Q"
    ctx.logger.info(s"Running:\n${processCmd}")

    val (retCode, duration) = ctx.stats.scope {
      Process(
        command = processCmd,
        cwd = runDir.toFile()
      ) ! processLogger

    }

    ctx.logger.info(yosysLog.toString())
    ctx.logger.info(f"Yosys took ${duration}%.3f ms")

    if (retCode != 0) {
      ctx.logger.fail(s"Failed compiling with Yosys! (err ${retCode})")
    }

    backend.filename

  }
}
