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
import manticore.assembly.levels.placed.ListSchedulerTransform
import manticore.assembly.levels.placed.GlobalPacketSchedulerTransform
import manticore.assembly.levels.placed.PredicateInsertionTransform
import manticore.assembly.levels.unconstrained._

case class CliConfig(
    input_file: Option[File] = None,
    print_tree: Boolean = false,
    dump_all: Boolean = false,
    dump_dir: Option[File] = None,
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
        head("Manticore assembler", "vPROTOTYPE"),
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
        opt[Unit]("dump-all")
          .action { case (_, c) => c.copy(dump_all = true) }
          .text("dump everything in each step in the directory given by --dump-dir"),
        opt[File]("dump-dir")
          .action { case (x, c) => c.copy(dump_dir = Some(x))}
          .text("directory to place all the dump files"),
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
        config
      case _ =>
        sys.error("Failed parsing cli args")
    }

    val ctx: AssemblyContext =
      AssemblyContext(
        debug_message = cfg.debug_en,
        print_tree = cfg.print_tree,
        source_file = cfg.input_file,
        output_file = cfg.output_file,
        dump_all = cfg.dump_all,
        dump_dir =  cfg.dump_dir
      )
    println(ctx.dump_all)


    def runPhases(prg: UnconstrainedIR.DefProgram) = {
      val unconstrained_phases = UnconstrainedNameChecker followedBy
          UnconstrainedRenameVariables followedBy
          UnconstrainedOrderInstructions followedBy
          UnconstrainedRemoveAliases followedBy
          UnconstrainedDeadCodeElimination followedBy
          // BigIntToUInt16Transform followedBy
          UnconstrainedToPlacedTransform
      val placed_phase = PlacedNameChecker followedBy
          ListSchedulerTransform followedBy
          PredicateInsertionTransform followedBy
          GlobalPacketSchedulerTransform

      val phases =
        unconstrained_phases followedBy placed_phase

      phases(prg, ctx)._1
    }


    val parsed = AssemblyParser(cfg.input_file.get, ctx)

    runPhases(parsed)

  }

}
