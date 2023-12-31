package manticore.compiler

import java.nio.file.Files
import manticore.compiler.assembly.HasSerialized
import scala.util.parsing.input.Positional
import java.io.File
import java.io.PrintWriter
import java.io.PrintStream


/** Irrecoverable compilation exception/error
  *
  * @param msg
  *   message to be displayed on exit
  */
class CompilationFailureException(msg: String) extends Exception(msg)

trait HasLoggerId {
  val id: String
  override def toString(): String = id
}
object LoggerId {
  def apply(name: String) = new HasLoggerId { val id = name }
}

/** Fully self-contained reported class
  */

trait Logger {
  import io.AnsiColor._

  protected def message[N <: HasSerialized with Positional](
      msg: => String,
      node: N
  )(implicit phase_id: HasLoggerId): Unit

  protected def message(msg: => String)(implicit
      phase_id: HasLoggerId
  ): Unit

  def error[N <: HasSerialized with Positional](
      msg: => String,
      node: N
  )(implicit phase_id: HasLoggerId): Unit

  def error(msg: => String)(implicit phase_id: HasLoggerId): Unit

  def countErrors(): Int
  def countWarnings(): Int
  def countProgress(): Int

  def warn(msg: => String)(implicit phase_id: HasLoggerId): Unit

  def warn[N <: HasSerialized with Positional](msg: => String, node: N)(implicit
      phase_id: HasLoggerId
  ): Unit

  def info[N <: HasSerialized with Positional](msg: => String, node: N)(implicit
      phase_id: HasLoggerId
  ): Unit
  def info(msg: => String)(implicit phase_id: HasLoggerId): Unit
  def fail(msg: => String)(implicit phase_id: HasLoggerId): Nothing

  def debug(msg: => String)(implicit phase_id: HasLoggerId): Unit
  def debug[N <: HasSerialized with Positional](
      msg: => String,
      node: N
  )(implicit phase_id: HasLoggerId): Unit

  def dumpArtifact(
      file_name: String,
      forceDump: Boolean = false
  )(gen: => String)(implicit phase_id: HasLoggerId): Unit

  def openFile(file_name: String)(implicit phase_id: HasLoggerId): File

  def start(msg: => String)(implicit phase_id: HasLoggerId): Unit
  def end(msg: => String)(implicit phase_id: HasLoggerId): Unit

  def flush(): Unit
}

object Logger {

  trait ColorCollection {
    val RED: String
    val BLUE: String
    val YELLOW: String
    val CYAN: String
    val BOLD: String
    val RESET: String
  }

  object Colored extends ColorCollection {
    import io.AnsiColor
    override val RED: String    = AnsiColor.RED
    override val BLUE: String   = AnsiColor.BLUE
    override val YELLOW: String = AnsiColor.YELLOW
    override val CYAN: String   = AnsiColor.CYAN
    override val BOLD: String   = AnsiColor.BOLD
    override val RESET: String  = AnsiColor.RESET
  }
  object NoColor extends ColorCollection {
    override val RED: String    = ""
    override val BLUE: String   = ""
    override val YELLOW: String = ""
    override val CYAN: String   = ""
    override val BOLD: String   = ""
    override val RESET: String  = ""
  }

  private class VerbosePrintLogger(
      val db_en: Boolean,
      val info_en: Boolean,
      val dump_dir: Option[File],
      val dump_all: Boolean,
      val no_colors: Boolean,
      val printer: PrintWriter
  ) extends Logger {

    val color_pallette = if (no_colors) NoColor else Colored
    import color_pallette._

    private var transform_index: Int = 0
    private var error_count: Int     = 0
    private var warn_count: Int      = 0

    override protected def message[N <: HasSerialized with Positional](
        msg: => String,
        node: N
    )(implicit phase_id: HasLoggerId): Unit = {
      printer.println(
        s"${msg} \n at \n${node.serialized}:${node.pos}\n\t\t reported by ${BOLD}${phase_id}${RESET}"
      )
      flush()
    }

    override protected def message(
        msg: => String
    )(implicit phase_id: HasLoggerId): Unit = {
      printer.println(
        s"${msg} \n\t\treported by ${BOLD}${phase_id}${RESET}"
      )
      flush()
    }

    def error[N <: HasSerialized with Positional](
        msg: => String,
        node: N
    )(implicit phase_id: HasLoggerId): Unit = {
      message(s"[${RED}error${RESET}] ${msg}", node)
      error_count += 1
      flush()
    }
    def error(msg: => String)(implicit phase_id: HasLoggerId): Unit = {
      message(s"[${RED}error${RESET}] ${msg}")
      flush()
      error_count += 1
    }

    def countErrors(): Int   = error_count
    def countWarnings(): Int = warn_count
    def countProgress(): Int = transform_index

    def warn(msg: => String)(implicit phase_id: HasLoggerId): Unit = {
      message(s"[${YELLOW}warn${RESET}] ${msg}")
      warn_count += 1
      flush()
    }

    def warn[N <: HasSerialized with Positional](msg: => String, node: N)(implicit
        phase_id: HasLoggerId
    ): Unit = {
      message(s"[${YELLOW}warn${RESET}] ${msg}", node)
      warn_count += 1
      flush()
    }

    def info[N <: HasSerialized with Positional](msg: => String, node: N)(implicit
        phase_id: HasLoggerId
    ): Unit = if (info_en) {
      message(s"[${BLUE}info${RESET}] ${msg}", node)
      flush()
    }

    def info(msg: => String)(implicit phase_id: HasLoggerId): Unit = if (info_en) {
      message(s"[${BLUE}info${RESET}] ${msg}")
      flush()
    }

    def fail(msg: => String)(implicit phase_id: HasLoggerId): Nothing = {
      flush()
      throw new CompilationFailureException(msg)
    }

    def debug(msg: => String)(implicit phase_id: HasLoggerId): Unit = {
      if (db_en) {
        message(s"[${CYAN}debug${RESET}] ${msg}")
        // We want debug messages to be printed even if the program crashes
        // shortly after the call to the logger, so we flush.
        flush()
      }
    }

    def debug[N <: HasSerialized with Positional](
        msg: => String,
        node: N
    )(implicit phase_id: HasLoggerId): Unit = {
      if (db_en) {
        message(s"[${CYAN}debug${RESET}] ${msg}", node)
        // We want debug messages to be printed even if the program crashes
        // shortly after the call to the logger, so we flush.
        flush()
      }
    }

    def dumpArtifact(
        file_name: String,
        forceDump: Boolean = false
    )(gen: => String)(implicit phase_id: HasLoggerId): Unit = {

      dump_dir match {
        case Some(dir) if dump_all || forceDump =>
          Files.createDirectories(dir.toPath())
          val actualFileName = s"${countProgress()}_${phase_id}_${file_name}"
          info(s"Dumping ${dir.toPath.toAbsolutePath}/${actualFileName}")
          val xpath  = dir.toPath().resolve(actualFileName)
          val writer = new PrintWriter(xpath.toFile)
          writer.print(gen)
          writer.close()

        case _ => // do nothing
      }
    }

    def openFile(
        file_name: String
    )(implicit phase_id: HasLoggerId): File = {
      dump_dir match {
        case Some(dir) =>
          Files.createDirectories(dir.toPath())
          val actualFileName = s"${countProgress()}_${phase_id}_${file_name}"
          val fpath          = dir.toPath().resolve(actualFileName)
          fpath.toFile()
        case None =>
          fail("Could not open file, make sure dump directory is defined!")
      }
    }

    def start(msg: => String)(implicit phase_id: HasLoggerId): Unit = {
      info(s"${msg} ")
    }

    def end(msg: => String)(implicit phase_id: HasLoggerId): Unit = {
      printer.flush()
      if (msg.nonEmpty)
        info(msg)
      transform_index += 1
    }

    def flush(): Unit = printer.flush()
  }

  def apply(
      db_en: Boolean,
      info_en: Boolean,
      dump_dir: Option[File],
      dump_all: Boolean,
      log_file: Option[File]
  ): Logger = {

    val printer = log_file match {
      case Some(f: File) =>
        Files.createDirectories(f.toPath().toAbsolutePath.getParent())
        new PrintWriter(f)
      case None => new PrintWriter(System.out, true)
    }
    new VerbosePrintLogger(
      db_en,
      info_en,
      dump_dir,
      dump_all,
      log_file.nonEmpty,
      printer
    )
  }

}
