package manticore.assembly

/** Classes for diagnostics and reporting
  *
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */

import com.typesafe.scalalogging.LazyLogging
import scala.util.parsing.input.Positional
import manticore.compiler.AssemblyContext
import java.nio.file.Files
import java.io.PrintWriter

/** Irrecoverable compilation exception/error
  *
  * @param msg
  *   message to be displayed on exit
  */
class CompilationFailureException(msg: String) extends Exception(msg)

/** Fully self-contained reported class, transformation mix with it.
  */
trait Reporter {

  def getName: String = this.getClass().getSimpleName()
  class Logger {
    import io.AnsiColor._
    private var error_count: Int = 0;
    private var warn_count: Int = 0;
    private def message[N <: HasSerialized with Positional](
        msg: String,
        node: N
    ): Unit =
      println(
        s"${msg} \n at \n${node.serialized}:${node.pos}\n\t\t reported by ${BOLD}${getName}${RESET}"
      )

    private def message(msg: String): Unit =
      println(
        s"${msg} \n\t\treported by ${BOLD}${getName}${RESET}"
      )

    def error[N <: HasSerialized with Positional](
        msg: String,
        node: N
    ): Unit = {
      message(s"${RED}ERROR${RESET}: ${msg}", node)
      error_count += 1
    }
    def error(msg: String): Unit = {
      message(s"${RED}ERROR${RESET}: ${msg}")
      error_count += 1
    }
    def countErrors: Int = error_count

    def warn(msg: String): Unit = {
      message(s"${YELLOW}WARNING${RESET}: ${msg}")
      warn_count += 1
    }

    def warn[N <: HasSerialized with Positional](msg: String, node: N): Unit = {
      message(s"${YELLOW}WARNING${RESET}: ${msg}", node)
      warn_count += 1
    }

    def info[N <: HasSerialized with Positional](msg: String, node: N): Unit = {
      message(s"${BLUE}INFO${RESET}: ${msg}", node)
    }

    def info(msg: String): Unit =
      message(s"${BLUE}INFO${RESET}: ${msg}")

    def fail(msg: String): Nothing = throw new CompilationFailureException(msg)

    def debug(msg: => String)(implicit ctx: AssemblyContext): Unit =
      if (ctx.debug_message)
        message(s"${CYAN}DEBUG${RESET}: ${msg}")

    def debug[N <: HasSerialized with Positional](
        msg: => String,
        node: N
    )(implicit ctx: AssemblyContext): Unit =
      if (ctx.debug_message)
        message(s"${CYAN}DEBUG${RESET}: ${msg}", node)

    def dumpArtifact(
        file_name: String
    )(gen: => String)(implicit ctx: AssemblyContext): Unit = {

      ctx.dump_dir match {
        case Some(dir) if ctx.dump_all =>
          Files.createDirectories(dir.toPath())
          println(s"Dumping ${file_name} to ${dir.toPath.toAbsolutePath}")
          val fpath = dir.toPath().resolve(file_name)
          val writer = new PrintWriter(fpath.toFile)
          writer.print(gen)
          writer.close()

        case _ => // dot nothing
      }

    }
  }

  lazy val logger = new Logger

}
