package manticore.compiler

import java.io.File
import java.nio.file.Path
import java.io.PrintWriter
import java.nio.file.Files
import manticore.assembly.HasSerialized
import scala.util.parsing.input.Positional
import scala.reflect.internal.Phase
import manticore.assembly.levels.TransformationID
import java.awt.Color

/** Classes for diagnostics and reporting
  *
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */

trait AssemblyContext {
  val source_file: Option[File]
  val output_file: Option[File]
  val print_tree: Boolean
  val dump_all: Boolean
  val dump_dir: Option[File]
  val debug_message: Boolean
  val max_registers: Int
  val max_cycles: Int
  val quiet: Boolean

  val logger: Logger

}

object AssemblyContext {

  private class ContextImpl(
      val source_file: Option[File] = None,
      val output_file: Option[File] = None,
      val print_tree: Boolean = false,
      val dump_all: Boolean = false,
      val dump_dir: Option[File] = None,
      val debug_message: Boolean = false,
      val max_registers: Int = 2048,
      val max_cycles: Int = 1000,
      val quiet: Boolean = false,
      val logger: Logger
  ) extends AssemblyContext {}

  def apply(
      source_file: Option[File] = None,
      output_file: Option[File] = None,
      print_tree: Boolean = false,
      dump_all: Boolean = false,
      dump_dir: Option[File] = None,
      debug_message: Boolean = false,
      max_registers: Int = 2048,
      max_cycles: Int = 1000,
      quiet: Boolean = false,
      logger: Option[Logger] = None,
      log_file: Option[File] = None
  ): AssemblyContext = {
    new ContextImpl(
      source_file,
      output_file,
      print_tree,
      dump_all,
      dump_dir,
      debug_message,
      max_registers,
      max_cycles,
      quiet,
      logger.getOrElse(
        Logger(debug_message, !quiet, dump_dir, dump_all, log_file)
      )
    )
  }

}
