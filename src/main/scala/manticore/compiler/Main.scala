package manticore.compiler

/** Manticore Assembler
  *
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */
import java.io.File
import manticore.compiler.assembly.levels.unconstrained.{
  UnconstrainedIR,
  UnconstrainedNameChecker
}
import scopt.OParser
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.assembly.levels.placed.UnconstrainedToPlacedTransform
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.assembly.levels.placed.PlacedNameChecker
import scala.language.postfixOps
import manticore.compiler.assembly.levels.placed.ListSchedulerTransform
import manticore.compiler.assembly.levels.placed.GlobalPacketSchedulerTransform
import manticore.compiler.assembly.levels.placed.PredicateInsertionTransform
import manticore.compiler.assembly.levels.unconstrained._
import manticore.compiler.assembly.levels.DeadCodeElimination
import manticore.compiler.assembly.levels.unconstrained.width.WidthConversion
import java.io.PrintStream
import manticore.compiler.assembly.levels.placed.ProcessSplittingTransform
import manticore.compiler.assembly.levels.placed.PlacedIRDeadCodeElimination
import manticore.compiler.assembly.levels.placed.PlacedIROrderInstructions
import manticore.compiler.assembly.levels.placed.ProcessMergingTransform
import manticore.compiler.assembly.levels.placed.RoundRobinPlacerTransform
import manticore.compiler.assembly.levels.placed.SendInsertionTransform
import manticore.compiler.assembly.levels.placed.PlacedIRCloseSequentialCycles
import manticore.compiler.assembly.levels.placed.RegisterAllocationTransform
import manticore.compiler.assembly.levels.placed.PlacedIRPrinter
import manticore.compiler.assembly.levels.AssemblyPrinter
import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.assembly.levels.placed.LocalMemoryAllocation
import manticore.compiler.assembly.levels.placed.interpreter.AtomicInterpreter
import manticore.compiler.assembly.levels.codegen.MachineCodeGenerator
import manticore.compiler.assembly.parser.AssemblyLexical

case class CliConfig(
    input_file: Option[File] = None,
    print_tree: Boolean = false,
    dump_all: Boolean = false,
    dump_dir: Option[File] = None,
    output_dir: Option[File] = None,
    debug_en: Boolean = false,

    /** Machine configurations **/
    dimx: Int = 1,
    dimy: Int = 1,



    /** Dev configurations **/
    simulate: Boolean = false,
    interpret: Boolean = false,
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
          .action { case (x, c) => c.copy(output_dir = Some(x)) }
          .text("output directory"),
        opt[Unit]('t', "print-tree")
          .action { case (_, c) => c.copy(print_tree = true) }
          .text("print the asm program at each step of the assembler"),
        opt[Unit]("dump-all")
          .action { case (_, c) => c.copy(dump_all = true) }
          .text(
            "dump everything in each step in the directory given by --dump-dir"
          ),
        opt[File]("dump-dir")
          .action { case (x, c) => c.copy(dump_dir = Some(x)) }
          .text("directory to place all the dump files"),
        opt[Unit]('d', "debug")
          .action { case (_, c) => c.copy(debug_en = true) }
          .text("print debug information"),
        opt[Unit]("placed")
          .action { case (_, c) => c.copy(jump_to_placed = true) }
          .text("jump to placed IR")
          .hidden(),
        opt[Int]('X', "dimx")
          .action { case (x, c) => c.copy(dimx = x) }
          .text("number of cores in X"),
        opt[Int]('Y', "dimy")
          .action { case (y, c) => c.copy(dimy = y) }
          .text("number of cores in y"),
        opt[Unit]("simulate")
          .action { case (_, c) => c.copy(simulate = true) }
          .hidden()
          .text("simulate the program using Verilator"),
        opt[Unit]("interpret")
          .action { case (_, c) => c.copy(interpret = true) }
          .hidden()
          .text("interpret the program in software"),
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
        output_dir = cfg.output_dir,
        dump_all = cfg.dump_all,
        dump_dir = cfg.dump_dir,
        max_dimx = cfg.dimx,
        max_dimy = cfg.dimy
      )

    def runPhases(prg: UnconstrainedIR.DefProgram) = {

      import ManticorePasses._

      val phases =
        frontend followedBy
          middleend followedBy
          FrontendInterpreter(cfg.interpret) followedBy
          backend followedBy
          BackendInterpreter(cfg.interpret) andFinally
          MachineCodeGenerator
      phases(prg, ctx)
    }

    val parsed = AssemblyParser(cfg.input_file.get, ctx)
    runPhases(parsed)

  }

}
