package manticore.compiler

import java.io.File
import manticore.assembly.parser.UnconstrainedAssemblyParser
import manticore.assembly.levels.unconstrained.{
  UnconstrainedIR,
  UnconstrainedNameChecker
}

import scopt.OParser

case class CliConfig(
    input_file: File = new File("."),
    print_tree: Boolean = false,
    output_file: File = new File(".")
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

    println(
      s"Reading input file ${cfg.input_file.toPath.toAbsolutePath}"
    )
    val asm = scala.io.Source
      .fromFile(cfg.input_file)
      .mkString("")
    println(s"Parsing into ${UnconstrainedIR.getClass().getSimpleName()}")

    val ast = UnconstrainedAssemblyParser(asm)

    if (cfg.print_tree)
      println(ast.serialized)

    val errors = UnconstrainedNameChecker(ast)
    if (errors > 0) {
      sys.error(s"Failed with ${errors} errors!")
    }

  }

}
