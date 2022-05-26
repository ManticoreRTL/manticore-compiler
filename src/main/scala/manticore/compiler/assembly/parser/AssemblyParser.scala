package manticore.compiler.assembly.parser
import manticore.compiler.FunctionalTransformation
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.AssemblyContext
import manticore.compiler.LoggerId
import java.io.File
object AssemblyParser
    extends FunctionalTransformation[String, UnconstrainedIR.DefProgram] {

  implicit val loggerId = LoggerId("AssemblyParser")

  def apply(
      source: String
  )(implicit context: AssemblyContext): UnconstrainedIR.DefProgram = {
    context.logger.info("Parsing from string input")
    UnconstrainedAssemblyParser(source, context)
  }

  def apply(
      source: File
  )(implicit context: AssemblyContext): UnconstrainedIR.DefProgram = {
    context.logger.info("Parsing from file input")
    UnconstrainedAssemblyParser(
      scala.io.Source.fromFile(source).mkString(""),
      context
    )
  }

}
