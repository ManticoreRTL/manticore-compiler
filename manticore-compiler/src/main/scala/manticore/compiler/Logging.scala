package manticore.compiler

import java.nio.file.Files
import manticore.compiler.assembly.levels.TransformationID
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

/** Fully self-contained reported class
  */

trait Logger {
  import io.AnsiColor._

  protected def message[N <: HasSerialized with Positional](
      msg: => String,
      node: N
  )(implicit phase_id: TransformationID): Unit

  protected def message(msg: => String)(implicit
      phase_id: TransformationID
  ): Unit

  def error[N <: HasSerialized with Positional](
      msg: => String,
      node: N
  )(implicit phase_id: TransformationID): Unit

  def error(msg: => String)(implicit phase_id: TransformationID): Unit

  def countErrors(): Int
  def countWarnings(): Int
  def countProgress(): Int

  def warn(msg: => String)(implicit phase_id: TransformationID): Unit

  def warn[N <: HasSerialized with Positional](msg: => String, node: N)(implicit
      phase_id: TransformationID
  ): Unit

  def info[N <: HasSerialized with Positional](msg: => String, node: N)(implicit
      phase_id: TransformationID
  ): Unit
  def info(msg: => String)(implicit phase_id: TransformationID): Unit
  def fail(msg: => String)(implicit phase_id: TransformationID): Nothing

  def debug(msg: => String)(implicit phase_id: TransformationID): Unit
  def debug[N <: HasSerialized with Positional](
      msg: => String,
      node: N
  )(implicit phase_id: TransformationID): Unit

  def dumpArtifact(
      file_name: String
  )(gen: => String)(implicit phase_id: TransformationID): Unit

  def openFile(file_name: String)(implicit phase_id: TransformationID): File

  def start(msg: => String)(implicit phase_id: TransformationID): Unit
  def end(msg: => String)(implicit phase_id: TransformationID): Unit

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
    override val RED: String = AnsiColor.RED
    override val BLUE: String = AnsiColor.BLUE
    override val YELLOW: String = AnsiColor.YELLOW
    override val CYAN: String = AnsiColor.CYAN
    override val BOLD: String = AnsiColor.BOLD
    override val RESET: String = AnsiColor.RESET
  }
  object NoColor extends ColorCollection {
    override val RED: String = ""
    override val BLUE: String = ""
    override val YELLOW: String = ""
    override val CYAN: String = ""
    override val BOLD: String = ""
    override val RESET: String = ""
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
    private var error_count: Int = 0
    private var warn_count: Int = 0

    override protected def message[N <: HasSerialized with Positional](
        msg: => String,
        node: N
    )(implicit phase_id: TransformationID): Unit =
      printer.println(
        s"${msg} \n at \n${node.serialized}:${node.pos}\n\t\t reported by ${BOLD}${phase_id}${RESET}"
      )

    override protected def message(
        msg: => String
    )(implicit phase_id: TransformationID): Unit =
      printer.println(
        s"${msg} \n\t\treported by ${BOLD}${phase_id}${RESET}"
      )

    def error[N <: HasSerialized with Positional](
        msg: => String,
        node: N
    )(implicit phase_id: TransformationID): Unit = {
      message(s"[${RED}error${RESET}]${msg}", node)
      error_count += 1
    }
    def error(msg: => String)(implicit phase_id: TransformationID): Unit = {
      message(s"[${RED}error${RESET}] ${msg}")
      error_count += 1
    }
    def countErrors(): Int = error_count
    def countWarnings(): Int = warn_count
    def countProgress(): Int = transform_index

    def warn(msg: => String)(implicit phase_id: TransformationID): Unit = {
      message(s"[${YELLOW}warn${RESET}] ${msg}")
      warn_count += 1
    }

    def warn[N <: HasSerialized with Positional](msg: => String, node: N)(
        implicit phase_id: TransformationID
    ): Unit = {
      message(s"[${YELLOW}warn${RESET}] ${msg}", node)
      warn_count += 1
    }

    def info[N <: HasSerialized with Positional](msg: => String, node: N)(
        implicit phase_id: TransformationID
    ): Unit = if (info_en) {
      message(s"[${BLUE}info${RESET}] ${msg}", node)
    }

    def info(msg: => String)(implicit phase_id: TransformationID): Unit = if (
      info_en
    ) {
      message(s"[${BLUE}info${RESET}] ${msg}")
    }

    def fail(msg: => String)(implicit phase_id: TransformationID): Nothing = {

      printer.flush()
      throw new CompilationFailureException(msg)
    }

    def debug(msg: => String)(implicit phase_id: TransformationID): Unit =
      if (db_en)
        message(s"[${CYAN}debug${RESET}] ${msg}")

    def debug[N <: HasSerialized with Positional](
        msg: => String,
        node: N
    )(implicit phase_id: TransformationID): Unit =
      if (db_en)
        message(s"[${CYAN}debug${RESET}] ${msg}", node)

    def dumpArtifact(
        file_name: String
    )(gen: => String)(implicit phase_id: TransformationID): Unit = {

      dump_dir match {
        case Some(dir) if dump_all =>
          Files.createDirectories(dir.toPath())

          info(s"Dumping ${file_name} to ${dir.toPath.toAbsolutePath}")
          val xpath = dir.toPath().resolve(file_name)
          val writer = new PrintWriter(xpath.toFile)
          writer.print(gen)
          writer.close()

        case _ => // dot nothing
      }
    }

    def openFile(
        file_name: String
    )(implicit phase_id: TransformationID): File = {
      dump_dir match {
        case Some(dir) =>
          Files.createDirectories(dir.toPath())
          val fpath = dir.toPath().resolve(file_name)
          fpath.toFile()
        case None =>
          fail("Could not open file, make sure dump directory is defined!")
      }
    }

    def start(msg: => String)(implicit phase_id: TransformationID): Unit = {
      info(s"${msg} ")
    }

    def end(msg: => String)(implicit phase_id: TransformationID): Unit = {
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
        Files.createDirectories(f.toPath().getParent())
        new PrintWriter(f)
      case None          => new PrintWriter(System.out, true)
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
