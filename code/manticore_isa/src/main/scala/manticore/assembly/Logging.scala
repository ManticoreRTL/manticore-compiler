package manticore.assembly

import com.typesafe.scalalogging.LazyLogging
import scala.util.parsing.input.Positional
import manticore.compiler.AssemblyContext

class CompilationFailureException(msg: String) extends Exception(msg)

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
        s"${msg} \n at \n${node.serialized}:${node.pos} reported by ${BOLD}${getName}${RESET}"
      )

    private def message(msg: String): Unit =
      println(
        s"${msg} \nreported by ${BOLD}${getName}${RESET}"
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

    def fail(msg: String): Unit = throw new CompilationFailureException(msg)

    def debug(msg: String)(implicit ctx: AssemblyContext): Unit =
      if (ctx.getDebugMessage)
        message(s"${CYAN}DEBUG${RESET}: ${msg}")

    def debug[N <: HasSerialized with Positional](
        msg: String,
        node: N
    )(implicit ctx: AssemblyContext): Unit =
      if (ctx.getDebugMessage)
        message(s"${CYAN}DEBUG${RESET}: ${msg}", node)

  }

  lazy val logger = new Logger

}

