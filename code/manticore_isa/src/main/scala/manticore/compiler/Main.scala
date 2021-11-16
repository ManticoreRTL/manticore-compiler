package manticore.compiler

import java.io.File
import manticore.assembly.parser.UnconstrainedAssemblyParser
import manticore.assembly.levels.unconstrained.{UnconstrainedIR, UnconstrainedNameChecker}

import scopt.OParser

object CliParser {

  def apply(args: Array[String]) = {

    def parseSwitched(
        parsed: Map[String, Any],
        not_parsed: List[String]
    ): Map[String, Any] = {
      not_parsed match {
        case Nil => parsed
        case "--output" :: value :: tail =>
          parseSwitched(parsed ++ Map("output" -> new File(value)), tail)
        case "--print-tree" :: tail =>
          parseSwitched(parsed ++ Map("print-tree" -> true), tail)
        case option :: tail =>
          println(s"unkown option $option")
          sys.error("coult not parse arguments")
      }
    }

    val arg_list = args.toList
    val positionals = (arg_list) match {
      case input_file :: tail =>
        println(input_file)
        (Map("input_file" -> new File(input_file)), tail)
      case _ => sys.error(s"Missing positional arguments")
    }
    parseSwitched(positionals._1, positionals._2)
  }

}

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
    }
    val parsed = CliParser(args)

    println(parsed)

    println(
      s"Reading input file ${parsed("input_file").asInstanceOf[File].toPath.toAbsolutePath}"
    )
    val asm = scala.io.Source
      .fromFile(parsed("input_file").asInstanceOf[File])
      .mkString("")
    println(s"Parsing into ${UnconstrainedIR.getClass().getSimpleName()}")

    val ast = UnconstrainedAssemblyParser(asm)

    println(ast.serialized)
    val errors = UnconstrainedNameChecker(ast)
    if (errors > 0) {
        sys.error(s"Failed with ${errors} errors!")
    }
    
  }

}
