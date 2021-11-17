package manticore.compiler

import java.io.File

import manticore.assembly.levels.unconstrained.{
  UnconstrainedIR,
  UnconstrainedNameChecker
}

import scopt.OParser
// import manticore.assembly.parser.AssemblyFileParser
import manticore.assembly.parser.AssemblyParser
import manticore.assembly.levels.placed.UnconstrainedToPlacedTransform
import manticore.assembly.levels.placed.PlacedIR

case class CliConfig(
    input_file: File = new File("."),
    print_tree: Boolean = false,
    output_file: File = new File("."),
    debug_en: Boolean = false
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
          .action { case (x, c) => c.copy(input_file = x) }
          .required()
          .text("input assembly file"),
        opt[File]('o', "output")
          .action { case (x, c) => c.copy(output_file = x) }
          .text("output binary file"),
        opt[Unit]('t', "print-tree")
          .action { case (_, c) => c.copy(print_tree = true) }
          .text("print the asm program at each step of the assembler"),
        opt[Unit]('d', "debug")
          .action { case (_, c) => c.copy(debug_en = true) }
          .text("print debug information"),
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

    
    implicit val ctx: AssemblyContext =
      AssemblyContext()
      .withDebugMessage(cfg.debug_en)
      .withPrintTree(cfg.print_tree)
      .withSourceFile(cfg.input_file)
      .withOutputFile(cfg.output_file)
    

    val backend =
      // UnconstrainedNameChecker followedBy
        UnconstrainedToPlacedTransform

    // backend(Parsercfg.input_file)
    backend(AssemblyParser.apply(cfg.input_file))

  }

}
