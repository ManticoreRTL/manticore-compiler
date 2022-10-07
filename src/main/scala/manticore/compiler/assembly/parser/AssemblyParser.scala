package manticore.compiler.assembly.parser
import manticore.compiler.FunctionalTransformation
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.AssemblyContext
import manticore.compiler.LoggerId
import java.io.File
import java.nio.file.Path
import manticore.compiler.HasTransformationID
object AssemblyParser extends FunctionalTransformation[String, UnconstrainedIR.DefProgram] with HasTransformationID {

  def apply(
      source: String
  )(implicit context: AssemblyContext): UnconstrainedIR.DefProgram = {
    val (res, runtime) = context.stats.scope {
      context.logger.info("Parsing from string input")
      UnconstrainedAssemblyParser(source)
    }
    context.logger.info(s"Finished after ${runtime}%.3f ms")
    res
  }

  @deprecated("reading files is deprecated, use AssemblyFileParser instead")
  def apply(
      source: File
  )(implicit context: AssemblyContext): UnconstrainedIR.DefProgram = {
    context.logger.info("Parsing from file input")
    UnconstrainedAssemblyParser(scala.io.Source.fromFile(source).mkString(""))
  }

}

object AssemblyFileParser extends FunctionalTransformation[Path, UnconstrainedIR.DefProgram] with HasTransformationID {

  def apply(
      path: Path
  )(implicit ctx: AssemblyContext): UnconstrainedIR.DefProgram = {
    val (res, runtime) = ctx.stats.scope {
      ctx.logger.info(s"Parsing from file ${path.toAbsolutePath()}")
      UnconstrainedAssemblyParser(
        scala.io.Source.fromFile(path.toFile()).mkString("")
      )
    }
    ctx.logger.info(s"Finished after ${runtime}%.3f ms")
    res
  }

}
