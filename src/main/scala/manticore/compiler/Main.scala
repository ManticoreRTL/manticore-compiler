package manticore.compiler

/** Manticore Assembler
  *
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */
import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.assembly.levels.AssemblyPrinter
import manticore.compiler.assembly.levels.DeadCodeElimination
import manticore.compiler.assembly.levels.codegen.InitializerProgram
import manticore.compiler.assembly.levels.codegen.MachineCodeGenerator

import manticore.compiler.assembly.levels.placed.interpreter.AtomicInterpreter
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedNameChecker
import manticore.compiler.assembly.levels.unconstrained._
import manticore.compiler.assembly.levels.unconstrained.width.WidthConversion
import manticore.compiler.assembly.parser.AssemblyFileParser
import manticore.compiler.assembly.parser.AssemblyLexical
import manticore.compiler.assembly.parser.AssemblyParser
import scopt.OParser

import java.io.File
import java.io.PrintStream
import java.io.PrintWriter
import scala.language.postfixOps
import java.nio.file.Path
import manticore.compiler.assembly.levels.codegen.CodeDump
import manticore.compiler.frontend.yosys.YosysVerilogReader
import manticore.compiler.frontend.yosys.YosysRunner
import manticore.compiler.frontend.yosys.Yosys
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import java.nio.file.Files
sealed trait Mode
case object CompileMode extends Mode
case class InterpretMode(lowerFirst: Boolean = false, timeout: Int = 100000, serialOut: Option[File] = None)
    extends Mode
case class CliConfig(
    mode: Mode = CompileMode,
    inputFiles: Seq[File] = Nil,
    outputDir: File = Path.of(".").toFile,
    dumpDir: Option[File] = None,
    dumpAll: Boolean = false,
    dumpScratchPad: Boolean = false,
    dumpRegisterFile: Boolean = false,
    dumpAscii: Boolean = false,
    logFile: Option[File] = None,
    debugMessage: Boolean = false,
    debug_en: Boolean = false,
    report: Option[File] = None,
    noOptCommonCfs: Boolean = false,
    /** Machine configurations * */
    dimX: Int = 12,
    dimY: Int = 12
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
        arg[File]("<file>...")
          .unbounded()
          .minOccurs(1)
          .action { case (f, c) => c.copy(inputFiles = c.inputFiles :+ f) }
          .text("files (.masm, .v, .sv)"),
        opt[Unit]("no-opt-shared-cfs")
          .action { case (_, c) => c.copy(noOptCommonCfs = true) }
          .text("Do not optimize shared custom functions"),
        opt[File]('o', "output")
          .action { case (f, c) => c.copy(outputDir = f) }
          .text("output directory"),
        opt[File]('r', "report")
          .action { case (x, c) => c.copy(report = Some(x)) }
          .text("emit a compilation report"),
        opt[Unit]("dump-all")
          .action { case (_, c) => c.copy(dumpAll = true) }
          .text("dump intermediate results"),
        opt[File]("dump-dir")
          .action { case (x, c) => c.copy(dumpDir = Some(x)) }
          .text("directory to place all the dump files"),
        opt[File]('l', "log")
          .action { case (x, c) => c.copy(logFile = Some(x)) }
          .text("log file"),
        opt[Unit]('d', "debug")
          .action { case (_, c) => c.copy(debug_en = true) }
          .text("print debug information"),
        opt[Unit]("dump-register-file")
          .action { case (_, c) => c.copy(dumpRegisterFile = true) }
          .text("dump register file initial values in ascii binary format"),
        opt[Unit]("dump-scratch-pad")
          .action { case (_, c) => c.copy(dumpScratchPad = true) }
          .text("dump scratch pad initial values in ascii binary format"),
        opt[Unit]("dump-ascii")
          .action { case (_, c) => c.copy(dumpAscii = true) }
          .text("dump program in in human readable and binary ascii format"),
        opt[Int]('x', "dim-x")
          .action { case (v, c) => c.copy(dimX = v) }
          .text("horizontal grid dimension")
          .required(),
        opt[Int]('y', "dim-y")
          .action { case (v, c) => c.copy(dimY = v) }
          .text("vertical grid dimension")
          .required(),
        cmd("interpret")
          .action { case (_, c) => c.copy(mode = InterpretMode()) }
          .text("interpret")
          .children(
            opt[Unit]('L', "lower")
              .action { case (_, c) =>
                c.copy(mode = c.mode.asInstanceOf[InterpretMode].copy(lowerFirst = true))
              }
              .text("interpret in lower assembly the program before interpretation"),
            opt[File]('s', "serial")
              .action { case (f, c) => c.copy(mode = c.mode.asInstanceOf[InterpretMode].copy(serialOut = Some(f))) }
              .text("redirect interpretation serial output to a file"),
            opt[Int]('t', "timeout")
              .action { case (v, c) =>
                c.copy(mode = c.mode.asInstanceOf[InterpretMode].copy(timeout = v))
              }
              .text("timeout after the given number of cycles")
          ),
        help('h', "help").text("print usage text and exit"),
        checkConfig { cfg =>
          if (cfg.dimX == 0 || cfg.dimY == 0) {
            failure(s"invalid grid ${cfg.dimX}x${cfg.dimY}!")
          } else {
            success
          }
        }
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
        output_dir = Some(cfg.outputDir),
        dump_all = cfg.dumpAll,
        dump_dir = cfg.dumpDir,
        dump_ra = cfg.dumpScratchPad,
        dump_rf = cfg.dumpRegisterFile,
        dump_ascii = cfg.dumpAscii,
        log_file = cfg.logFile,
        max_cycles = Try { cfg.mode.asInstanceOf[InterpretMode].timeout }.getOrElse(0),
        hw_config = DefaultHardwareConfig(dimX = cfg.dimX, dimY = cfg.dimY)
      )
    cfg.dumpDir.foreach { f => Files.createDirectories(f.toPath) }

    val VerilogCompiler = Yosys.YosysDefaultPassAggregator andThen
      YosysRunner

    val AssemblyCompiler = AssemblyFileParser andThen
      ManticorePasses.frontend andThen
      ManticorePasses.middleend andThen
      ManticorePasses.backend

    val assemblyFile = if (cfg.inputFiles.length == 1 && cfg.inputFiles.head.toPath.endsWith(".masm")) {
      cfg.inputFiles.head.toPath
    } else {
      val VerilogCompiler = Yosys.YosysDefaultPassAggregator andThen
        YosysRunner(cfg.outputDir.toPath.resolve("yosys"))
      VerilogCompiler(cfg.inputFiles.map(_.toPath))
    }

    cfg.mode match {
      case CompileMode =>
        val compiler = AssemblyCompiler andThen CodeDump
        compiler(assemblyFile)
      case InterpretMode(lowerFirst, timeout, serialOut) =>
        val serialWriter = serialOut.map { file =>
          new PrintWriter(file)
        }
        val serialCapture = serialWriter.map { writer => ln: String => writer.println(ln) }
        val result = Try {
          if (lowerFirst) {
            // lower the program but do not dump code, interpret instead
            val program = AssemblyCompiler(assemblyFile)

            val interpreter = AtomicInterpreter.instance(
              program = program,
              vcd = None,
              monitor = None,
              expectedCycles = None,
              serial = serialCapture.orElse { Some(println(_)) }
            )

            interpreter.interpretCompletion()
            if (ctx.logger.countErrors() > 0) {
              throw new CompilationFailureException("Interpretation encountered errors! See the log.")
            }
          } else {
            // immediately interpret the program
            val program =
              (AssemblyFileParser andThen ManticorePasses.frontend andThen UnconstrainedCloseSequentialCycles)(
                assemblyFile
              )
            val interpreter = UnconstrainedInterpreter.instance(
              program = program,
              vcdDump = None,
              monitor = None,
              serial = serialCapture.orElse { Some(println(_)) }
            )

            interpreter.runCompletion()
            if (ctx.logger.countErrors() > 0) {
              throw new CompilationFailureException("Interpretation encountered errors! See the log.")
            }
          }

        }
        serialWriter.foreach { _.close() }
        result match {
          case Failure(exception) =>
            System.err.println("Failed interpretation:\n" + exception.getMessage())
            System.exit(-1)
          case Success(_) =>
            System.exit(0)
        }

    }

  }

}
