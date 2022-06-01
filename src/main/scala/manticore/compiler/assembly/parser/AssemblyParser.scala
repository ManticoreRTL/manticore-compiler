package manticore.compiler.assembly.parser
import manticore.compiler.FunctionalTransformation
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.AssemblyContext
import manticore.compiler.LoggerId
import java.io.File
import java.nio.file.Path
object AssemblyParser
    extends FunctionalTransformation[String, UnconstrainedIR.DefProgram] {

  implicit val loggerId = LoggerId("AssemblyParser")

  def apply(
      source: String
  )(implicit context: AssemblyContext): UnconstrainedIR.DefProgram = {
    context.logger.info("Parsing from string input")
    UnconstrainedAssemblyParser(source)
  }

  @deprecated("reading files is deprecated, use AssemblyFileParser instead")
  def apply(
      source: File
  )(implicit context: AssemblyContext): UnconstrainedIR.DefProgram = {
    context.logger.info("Parsing from file input")
    UnconstrainedAssemblyParser(scala.io.Source.fromFile(source).mkString(""))
  }

}

object AssemblyFileParser
    extends FunctionalTransformation[Path, UnconstrainedIR.DefProgram] {

  implicit val loggerId = LoggerId("AssemblyFileParser")

  def apply(
      path: Path
  )(implicit ctx: AssemblyContext): UnconstrainedIR.DefProgram = {
    ctx.logger.info(s"Parsing from file ${path.toAbsolutePath()}")
    UnconstrainedAssemblyParser(
      scala.io.Source.fromFile(path.toFile()).mkString("")
    )
  }

}
