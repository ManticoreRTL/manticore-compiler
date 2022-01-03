package manticore.compiler

import java.io.File
import java.nio.file.Path
import java.io.PrintWriter
import java.nio.file.Files

/** Compilation context that is accessible by all the transformations Add more
  * fields (with default values) to the constructor arguments with corresponding
  * constructor methods and getters.
  * @param source_file
  * @param output_file
  * @param print_tree
  * @param debug_message
  */

case class AssemblyContext(
    val source_file: Option[File] = None,
    val output_file: Option[File] = None,
    val print_tree: Boolean = false,
    val dump_all: Boolean = false,
    val dump_dir: Option[File] = None,
    val debug_message: Boolean = false,
    val max_registers: Int = 2048,
    val max_cycles: Int = 20
) {

  var transform_index = 0



}
