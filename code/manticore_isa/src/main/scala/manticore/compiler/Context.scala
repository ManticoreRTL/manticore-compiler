package manticore.compiler

import java.io.File

class AssemblyContext private (
    private val source_file: Option[File] = None,
    private val output_file: Option[File] = None,
    private val print_tree: Boolean = false,
    private val debug_message: Boolean = false
) {

  def getSourceFile: Option[File] = source_file
  def withSourceFile(f: File): AssemblyContext =
    new AssemblyContext(Some(f), output_file, print_tree, debug_message)
  def getOutputFile: Option[File] = output_file
  def withOutputFile(f: File): AssemblyContext = 
      new AssemblyContext(source_file, Some(f), print_tree, debug_message)
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


