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
  val print_tree: Boolean // deprecated, use dump_all
  val dump_all: Boolean // dump all intermediate steps
  val dump_dir: Option[File] // location to dump intermediate steps
  val debug_message: Boolean // print debug messages
  val max_registers: Int // maximum number of registers usable in a process
  val max_carries: Int // maximum number of carry bit registers
  val max_local_memory: Int // maximum local memory size in bytes
  val max_instructions: Int // maximum number of instruction a processor can host
  val max_instructions_threshold: Int // the threshold number of instructions for merging processes
  val max_cycles: Int // maximum number of cycles to interpret before erroring out
  val quiet: Boolean // do not print info messages
  val max_dimx: Int // maximum dimension in X
  val max_dimy: Int // maximum dimension in Y
  val use_loc: Boolean // use @LOC annotation instead of automatic placement
  val logger: Logger // logger object

  def uniqueNumber(): Int
}

object AssemblyContext {

  private class ContextImpl(
      val source_file: Option[File],
      val output_file: Option[File],
      val print_tree: Boolean,
      val dump_all: Boolean,
      val dump_dir: Option[File],
      val debug_message: Boolean,
      val max_registers: Int,
      val max_carries: Int,
      val max_local_memory: Int,
      val max_instructions: Int,
      val max_instructions_threshold: Int,
      val max_cycles: Int,
      val quiet: Boolean,
      val max_dimx: Int,
      val max_dimy: Int,
      val use_loc: Boolean,
      val logger: Logger
  ) extends AssemblyContext {


    var unique_int: Int = 0

    def uniqueNumber(): Int = {
      val res = unique_int
      unique_int += 1
      res
    }



  }

  def apply(
      source_file: Option[File] = None,
      output_file: Option[File] = None,
      print_tree: Boolean = false,
      dump_all: Boolean = false,
      dump_dir: Option[File] = None,
      debug_message: Boolean = false,
      max_registers: Int = 512,
      max_carries: Int = 4,
      max_local_memory: Int = 4096,
      max_instructions: Int = 4096,
      max_instructions_threshold: Int = 4096 - 512,
      max_cycles: Int = 1000,
      quiet: Boolean = false,
      max_dimx: Int = 2,
      max_dimy: Int = 2,
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
      max_carries = max_carries,
      max_local_memory = max_local_memory,
      max_instructions = max_instructions,
      max_instructions_threshold = max_instructions_threshold,
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
