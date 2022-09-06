package manticore.compiler

import java.io.File
import java.nio.file.Path
import java.io.PrintWriter
import java.nio.file.Files
import manticore.compiler.assembly.HasSerialized
import scala.util.parsing.input.Positional
import manticore.compiler.assembly.levels.TransformationID
import java.awt.Color
import java.util.concurrent.atomic.AtomicInteger
import manticore.compiler.assembly.levels.StatisticCollector

/** Classes for diagnostics and reporting
  *
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */

trait AssemblyContext {
  val source_file: Option[File]
  val output_dir: Option[File]
  val print_tree: Boolean // deprecated, use dump_all
  val dump_all: Boolean // dump all intermediate steps
  val dump_dir: Option[File] // location to dump intermediate steps
  val debug_message: Boolean // print debug messages
  val optimize_common_custom_functions: Boolean // whether common custom functions should be identified and merged
  val max_cycles: Int // maximum number of cycles to interpret before erroring out
  val quiet: Boolean // do not print info messages
  val placement_timeout_s: Int // timeout for analytic placement
  val expected_cycles: Option[
    Int
  ] // number of expected cycles before STOP is reached (used internally for tests)
  val use_loc: Boolean // use @LOC annotation for placement
  val dump_rf: Boolean // dump register file initial values
  val dump_ra: Boolean // dump register array initial values
  val dump_ascii: Boolean // dump the
  val logger: Logger // logger object
  val stats: StatisticCollector
  val hw_config: HardwareConfig

  def uniqueNumber(): Int


}

object AssemblyContext {

  private class ContextImpl(
      val source_file: Option[File],
      val output_dir: Option[File],
      val print_tree: Boolean,
      val dump_all: Boolean,
      val dump_dir: Option[File],
      val debug_message: Boolean,
      val optimize_common_custom_functions: Boolean,
      val max_cycles: Int,
      val quiet: Boolean,
      val placement_timeout_s: Int,
      val use_loc: Boolean,
      val dump_rf: Boolean,
      val dump_ra: Boolean,
      val dump_ascii: Boolean,
      val expected_cycles: Option[Int],
      val logger: Logger,
      val stats: StatisticCollector,
      val hw_config: HardwareConfig

  ) extends AssemblyContext {

    var unique_int: AtomicInteger = new AtomicInteger(0)

    def uniqueNumber(): Int = {
      unique_int.getAndAdd(1)
    }

  }

  def apply(
      source_file: Option[File] = None,
      output_dir: Option[File] = None,
      print_tree: Boolean = false,
      dump_all: Boolean = false,
      dump_dir: Option[File] = None,
      debug_message: Boolean = false,
      optimize_common_custom_functions: Boolean = false,
      max_cycles: Int = 1000,
      quiet: Boolean = false,
      placement_timeout_s: Int = 10,
      use_loc: Boolean = false,
      dump_ra: Boolean = true,
      dump_rf: Boolean = true,
      dump_ascii: Boolean = true,
      expected_cycles: Option[Int] = None,
      logger: Option[Logger] = None,
      log_file: Option[File] = None,
      hw_config: HardwareConfig = DefaultHardwareConfig(2, 2)
  ): AssemblyContext = {
    new ContextImpl(
      source_file = source_file,
      output_dir = output_dir,
      print_tree = print_tree,
      dump_all = dump_all,
      dump_dir = dump_dir,
      debug_message = debug_message,
      optimize_common_custom_functions = optimize_common_custom_functions,
      max_cycles = max_cycles,
      quiet = quiet,
      placement_timeout_s = placement_timeout_s,
      use_loc = use_loc,
      dump_ra = dump_ra,
      dump_rf = dump_rf,
      dump_ascii = dump_ascii,
      expected_cycles = expected_cycles,
      logger = logger.getOrElse(
        Logger(debug_message, !quiet, dump_dir, dump_all, log_file)
      ),
      stats = StatisticCollector(),
      hw_config = hw_config
    )
  }

}
