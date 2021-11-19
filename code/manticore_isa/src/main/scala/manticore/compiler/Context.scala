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
    val debug_message: Boolean = false
) {

  def dumpArtifact(file_name: String)(gen: => String): Unit = {

    dump_dir match {
      case Some(dir) if dump_all =>
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
