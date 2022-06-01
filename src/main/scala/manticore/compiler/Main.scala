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
import manticore.compiler.assembly.levels.AssemblyPrinter
import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.assembly.levels.placed.LocalMemoryAllocation
import manticore.compiler.assembly.levels.placed.interpreter.AtomicInterpreter
import manticore.compiler.assembly.levels.codegen.MachineCodeGenerator
import manticore.compiler.assembly.parser.AssemblyLexical
import manticore.compiler.assembly.levels.codegen.InitializerProgram
import java.io.PrintWriter
import manticore.compiler.assembly.parser.AssemblyFileParser
sealed trait Mode
case object CompileMode extends Mode
case object ExecMode extends Mode
case class CliConfig(
    mode: Mode = ExecMode,
    input_file: Option[File] = None,
    print_tree: Boolean = false,
    dump_all: Boolean = false,
    dump_dir: Option[File] = None,
    output_dir: Option[File] = None,
    output_file: Option[File] = None,
    log_file: Option[File] = None,
    debug_en: Boolean = false,
    report: Option[File] = None,
    /** Machine configurations * */
    dimx: Int = 1,
    dimy: Int = 1,
    /** Dev configurations * */
    simulate: Boolean = false,
    interpret: Boolean = false,
    dump_ra: Boolean = false,
    dump_rf: Boolean = false,
    dump_ascii: Boolean = false
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
        cmd("execute")
          .action { case (_, c) => c.copy(mode = ExecMode) }
          .text("execute a manticore assembly program")
          .children(
            opt[File]('o', "output")
              .action { case (x, c) => c.copy(output_file = Some(x)) }
              .text("file for writing program output (i.e., $display)"),
            opt[File]('l', "log")
              .action { case (x, c) => c.copy(log_file = Some(x)) }
              .text("compilation log file"),
            opt[File]("dump-dir")
              .action { case (x, c) => c.copy(dump_dir = Some(x)) }
              .text("directory to write all the dump files"),
            opt[Unit]("dump-all")
              .action { case (_, c) => c.copy(dump_all = true) }
              .text(
                "dump everything in each step in the directory given by --dump-dir"
              ),
            opt[Unit]('d', "debug")
              .action { case (_, c) => c.copy(debug_en = true) }
              .text("print debug information"),
            arg[File]("FILE")
              .action { case (x, c) => c.copy(input_file = Some(x)) }
              .text("input file")
              .required()
          ),
        // opt[File]('i', "input")
        //   .action { case (x, c) => c.copy(input_file = Some(x)) }
        //   .required()
        //   .text("input assembly file"),
        // opt[File]('o', "output")
        //   .action { case (x, c) => c.copy(output_dir = Some(x)) }
        //   .text("output directory"),
        // opt[File]('r', "report")
        //   .action { case (x, c) => c.copy(report = Some(x)) }
        //   .text("emit a compilation report"),
        // opt[Unit]('t', "print-tree")
        //   .action { case (_, c) => c.copy(print_tree = true) }
        //   .text("print the asm program at each step of the assembler"),
        // opt[Unit]("dump-all")
        //   .action { case (_, c) => c.copy(dump_all = true) }
        //   .text(
        //     "dump everything in each step in the directory given by --dump-dir"
        //   ),
        // opt[File]("dump-dir")
        //   .action { case (x, c) => c.copy(dump_dir = Some(x)) }
        //   .text("directory to place all the dump files"),
        // opt[Unit]('d', "debug")
        //   .action { case (_, c) => c.copy(debug_en = true) }
        //   .text("print debug information"),
        // opt[Int]('X', "dimx")
        //   .action { case (x, c) => c.copy(dimx = x) }
        //   .text("number of cores in X"),
        // opt[Int]('Y', "dimy")
        //   .action { case (y, c) => c.copy(dimy = y) }
        //   .text("number of cores in y"),
        // opt[Unit]("interpret")
        //   .action { case (_, c) => c.copy(interpret = true) }
        //   .hidden()
        //   .text("interpret the program"),
        // opt[Unit]("dump-rf")
        //   .action { case (_, c) => c.copy(dump_rf = true) }
        //   .text("dump register file initial values in ascii binary format"),
        // opt[Unit]("dump-ra")
        //   .action { case (_, c) => c.copy(dump_ra = true) }
        //   .text("dump register array initial values in ascii binary format"),
        // opt[Unit]("dump-ascii")
        //   .action { case (_, c) => c.copy(dump_ascii = true) }
        //   .text("dump program in in human readable and binary ascii format"),
        help('h', "help").text("print usage text and exit")
      )
    }

    val cfg = OParser.parse(parser, args, CliConfig()) match {
      case Some(config) =>
        config
      case _ =>
        sys.error("Failed parsing cli args")
    }

    implicit val ctx: AssemblyContext =
      AssemblyContext(
        debug_message = cfg.debug_en,
        print_tree = cfg.print_tree,
        source_file = cfg.input_file,
        output_dir = cfg.output_dir,
        dump_all = cfg.dump_all,
        dump_dir = cfg.dump_dir,
        max_dimx = cfg.dimx,
        max_dimy = cfg.dimy,
        dump_ra = cfg.dump_ra,
        dump_rf = cfg.dump_rf,
        dump_ascii = cfg.dump_ascii,
        log_file = cfg.log_file
      )

    val compiler = AssemblyFileParser andThen
      UnconstrainedNameChecker andThen
      UnconstrainedMakeDebugSymbols andThen
      UnconstrainedOrderInstructions andThen
      UnconstrainedCloseSequentialCycles
    val program = compiler(cfg.input_file.get.toPath())

    trait SerialPrinter {
      def println(ln: String): Unit
    }
    val serialCapture = cfg.output_file match {
      case Some(fname) =>
        new PrintWriter(fname)
      case None =>
        new PrintWriter(System.out)
    }
    val interpreter = UnconstrainedInterpreter.instance(
      program = program,
      serial = Some { ln =>
        serialCapture.println(ln)
        serialCapture.flush
      }
    )

    interpreter.runCompletion()

  }

}
