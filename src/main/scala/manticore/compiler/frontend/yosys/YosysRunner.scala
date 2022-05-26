package manticore.compiler.frontend.yosys

import manticore.compiler.AssemblyContext
import manticore.compiler.ManticoreTransform
import manticore.compiler.assembly.levels.TransformationID

import java.nio.file.Files
import java.nio.file.Path



object YosysRunner extends ManticoreTransform[YosysResultProxy, Path] {

  def apply(program: YosysResultProxy)(implicit ctx: AssemblyContext): Path = {
    val runner = new YosysRunner(Files.createTempDirectory("yosys_runner"))
    runner(program)
  }

  def withDirectory(runDir: Path) = new YosysRunner(runDir)

}

final class YosysRunner private (runDir: Path)
    extends ManticoreTransform[YosysResultProxy, Path] {

  implicit private val loggerId = TransformationID("YosysRunner")
  import scala.sys.process.{ProcessLogger, Process}
  import java.nio.file.Files

  override def apply(
      resultProxy: YosysResultProxy
  )(implicit ctx: AssemblyContext): Path = {

    ctx.logger.debug(s"Yosys run directory is ${runDir.toAbsolutePath()}")

    val assemblyResultPath = runDir
      .resolve("main.masm")
      .toAbsolutePath()

    val manticoreWriter =
      YosysPassProxy("manticore_writer") << assemblyResultPath.toString()

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
    ctx.logger.info(s"Yosys took ${duration}%.3f ms")

    if (retCode != 0) {
      ctx.logger.fail(s"Failed compiling with Yosys! (err ${retCode})")
    }

    assemblyResultPath

  }
}
