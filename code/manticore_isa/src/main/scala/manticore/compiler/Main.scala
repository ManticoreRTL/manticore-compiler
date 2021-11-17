package manticore.compiler

/** Manticore Assembler
  *
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */
import java.io.File
import manticore.assembly.levels.unconstrained.{
  UnconstrainedIR,
  UnconstrainedNameChecker
}
import scopt.OParser
import manticore.assembly.parser.AssemblyParser
import manticore.assembly.levels.placed.UnconstrainedToPlacedTransform
import manticore.assembly.levels.placed.PlacedIR
import manticore.assembly.levels.placed.PlacedNameChecker
import scala.language.postfixOps

case class CliConfig(
    input_file: Option[File] = None,
    print_tree: Boolean = false,
    output_file: Option[File] = None,
    debug_en: Boolean = false,
    jump_to_placed: Boolean =
      false // jump to placed IR, for Mahyar only.. temp option
)
object Main {

  def main(args: Array[String]): Unit = {

    val builder = OParser.builder[CliConfig]
    val parser = {
      import builder._
      // OParser
      OParser.sequence(
        programName("masm"),
        head("Manticore assmbler", "vPROTOTYPE"),
        opt[File]('i', "input")
          .action { case (x, c) => c.copy(input_file = Some(x)) }
          .required()
          .text("input assembly file"),
        opt[File]('o', "output")
          .action { case (x, c) => c.copy(output_file = Some(x)) }
          .text("output binary file"),
        opt[Unit]('t', "print-tree")
          .action { case (_, c) => c.copy(print_tree = true) }
          .text("print the asm program at each step of the assembler"),
        opt[Unit]('d', "debug")
          .action { case (_, c) => c.copy(debug_en = true) }
          .text("print debug information"),
        opt[Unit]("--placed")
          .action { case (_, c) => c.copy(jump_to_placed = true) }
          .text("jump to placed IR")
          .hidden(),
        help('h', "help").text("print usage text and exit")
      )
    }

    val cfg = OParser.parse(parser, args, CliConfig()) match {
      case Some(config) =>
        println(s"Parsed cli ${config}")
        config
      case _ =>
        sys.error("Failed parsing cli args")
    }

    val ctx: AssemblyContext =
      AssemblyContext()
        .withDebugMessage(cfg.debug_en)
        .withPrintTree(cfg.print_tree)
        .withSourceFile(cfg.input_file)
        .withOutputFile(cfg.output_file)

    def runPhases(prg: UnconstrainedIR.DefProgram) = {
      val phases =
        UnconstrainedNameChecker followedBy
          UnconstrainedToPlacedTransform followedBy
          PlacedNameChecker followedBy
          PlacedNameChecker followedBy
          PlacedNameChecker
      phases(prg, ctx)._1
    }
    
    val parsed = AssemblyParser(cfg.input_file.get, ctx)
    runPhases(parsed)

  }

}
