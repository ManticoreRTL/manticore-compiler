package manticore.compiler

import java.io.File


/**
  * 
  * Compilation context that is accessiable by all the transformations
  * Add more fields (with default values) to the constructor arguments
  * with corresponding constructor methods and getters.
  * @param source_file
  * @param output_file
  * @param print_tree
  * @param debug_message
  */

class AssemblyContext private (
    private val source_file: Option[File] = None,
    private val output_file: Option[File] = None,
    private val print_tree: Boolean = false,
    private val debug_message: Boolean = false
) {

  def getSourceFile: Option[File] = source_file
  def withSourceFile(f: Option[File]): AssemblyContext =
    new AssemblyContext(f, output_file, print_tree, debug_message)
  def getOutputFile: Option[File] = output_file
  def withOutputFile(f: Option[File]): AssemblyContext = 
      new AssemblyContext(source_file, f, print_tree, debug_message)
  def getPrintTree: Boolean = print_tree
  def withPrintTree(b: Boolean) = 
      new AssemblyContext(source_file, output_file, b, debug_message)
  def getDebugMessage: Boolean = debug_message
  def withDebugMessage(b: Boolean) = 
      new AssemblyContext(source_file, output_file, print_tree, b)
  
}

object AssemblyContext {
  def apply(): AssemblyContext = new AssemblyContext()
}


