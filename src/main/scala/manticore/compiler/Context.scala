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
  val print_tree: Boolean    // deprecated, use dump_all
  val dump_all: Boolean      // dump all intermediate steps
  val dump_dir: Option[File] // location to dump intermediate steps
  val debug_message: Boolean // print debug messages
  val max_registers: Int // maximum number of registers usable in a process
  val max_cycles: Int  // maximum number of cycles to interpret before erroring out
  val quiet: Boolean   // do not print info messages
  val max_dimx: Int    // maximum dimension in X
  val max_dimy: Int    // maximum dimension in Y
  val use_loc: Boolean // use @LOC annotation instead of automatic placement
  val logger: Logger   // logger object

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

      val max_dimx: Int = 10,
      val max_dimy: Int = 32,

      val use_loc: Boolean = false,
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
      max_dimx: Int = 10,
      max_dimy: Int = 32,
      use_loc: Boolean = false,
      logger: Option[Logger] = None,
      log_file: Option[File] = None
  ): AssemblyContext = {
    new ContextImpl(
      source_file = source_file,
      output_file = output_file,
      print_tree = print_tree,
      dump_all = dump_all,
      dump_dir = dump_dir,
      debug_message = debug_message,
      max_registers = max_registers,
      max_cycles = max_cycles,
      quiet = quiet,
      max_dimx = max_dimx,
      max_dimy = max_dimy,
      use_loc = use_loc,
      logger = logger.getOrElse(
        Logger(debug_message, !quiet, dump_dir, dump_all, log_file)
      )
    )
  }

}
