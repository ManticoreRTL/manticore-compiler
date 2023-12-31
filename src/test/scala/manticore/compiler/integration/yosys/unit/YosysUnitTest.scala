package manticore.compiler.integration.yosys.unit

import org.scalatest.flatspec.FixtureAnyFlatSpec
import manticore.compiler.HasRootTestDirectory
import org.scalatest.FixtureSuite
import manticore.compiler.AssemblyContext
import java.nio.file.Path
import manticore.compiler.UnitFixtureTest
import manticore.compiler.assembly.parser.AssemblyParser
import java.io.File
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedNameChecker
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedMakeDebugSymbols
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedOrderInstructions
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedCloseSequentialCycles
import java.io.PrintWriter
import manticore.compiler.CompilationFailureException
import scala.collection.mutable.ArrayBuffer
import java.nio.file.Files
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedInterpreter
import scala.io.BufferedSource
import manticore.compiler.HasLoggerId
import scala.annotation.tailrec
import manticore.compiler.frontend.yosys.Yosys
import manticore.compiler.frontend.yosys.YosysPass
import manticore.compiler.frontend.yosys.YosysRunner

import manticore.compiler.frontend.yosys.YosysVerilogReader
import manticore.compiler.frontend.yosys.YosysBackendProxy
import manticore.compiler.assembly.parser.AssemblyFileParser
import manticore.compiler.LoggerId
import manticore.compiler.ManticorePasses
import manticore.compiler.assembly.levels.placed.CustomLutInsertion
import manticore.compiler.assembly.levels.placed.interpreter.AtomicInterpreter
import manticore.compiler.assembly.levels.placed.PlacedIRCloseSequentialCycles

sealed trait TestCode
case class CodeText(src: String)      extends TestCode
case class CodeResource(path: String) extends TestCode

object YosysUnitTest {

  def verilate(
      vFilenames: Seq[Path],
      hFilenames: Seq[Path],
      timeOut: Int
  )(implicit
      ctx: AssemblyContext
  ): ArrayBuffer[String] = {
    import scala.sys.process.{Process, ProcessLogger}

    implicit val loggerId = new HasLoggerId { val id = "verilator-compile" }
    val flags = Seq(
      "-CFLAGS \"-DVL_USER_FINISH\"",
      "-cc",
      "--exe",
      "--build",
      "-Os",
      "-x-assign 0",
      // "--trace",
      "-Wno-WIDTH",
      "-Wno-UNOPTFLAT",
      "-Wno-MULTIDRIVEN",
      "--top-module Main"
    ).mkString(" ")

    val harnessPath = {
      val tempDir = Files.createTempDirectory("verilator_harness")
      val text =
        scala.io.Source.fromResource("integration/yosys/VHarness.cpp").mkString
      val tempFile = tempDir.resolve("VHarness.cpp")
      val writer   = new PrintWriter(tempFile.toFile())
      writer.print(text)
      writer.flush()
      writer.close()
      tempFile.toAbsolutePath().toString()
    }

    val runDir = Files.createTempDirectory("verilator_rundir")

    // Copy all files to the run directory.
    val runDirFileNames = (vFilenames ++ hFilenames).map { filepath =>
      val filename   = filepath.toString.split("/").last
      val targetPath = runDir.resolve(filename)
      val content    = scala.io.Source.fromFile(filepath.toFile()).getLines().mkString("\n")
      val writer     = new PrintWriter(targetPath.toFile())
      writer.write(content)
      writer.flush()
      writer.close()
      filepath -> targetPath
    }.toMap

    // We only call verilator on the verilog files. The hex files should be picked up automatically as
    // they use relative names in the verilog files.
    val verilteCmd =
      s"verilator $flags ${vFilenames.map(f => runDirFileNames(f).toAbsolutePath()).mkString(" ")} ${harnessPath}"
    ctx.logger.info(s"Running command:\n${verilteCmd}")
    val ret = Process(
      command = verilteCmd,
      cwd = runDir.toFile()
    ) ! ProcessLogger(msg => ctx.logger.info(msg))
    if (ret != 0) {
      ctx.logger.fail("Failed compiling with verilator")
    }

    // if we have been successful with verilator compilation, we should now
    // run the simulation

    val simCmd = s"obj_dir/VMain ${timeOut}"

    val out = ArrayBuffer.empty[String]
    ctx.logger.info(s"Running ${simCmd}")
    val simRet = Process(
      command = simCmd,
      cwd = runDir.toFile()
    ) ! ProcessLogger(out += _)
    if (simRet != 0) {
      println(out.mkString("\n"))
      throw new CompilationFailureException(
        s"Failed running reference simulation $simCmd: $ret"
      )
    }
    out
  }

  def verilate(filenames: Seq[Path], timeOut: Int)(implicit
      ctx: AssemblyContext
  ): ArrayBuffer[String] = {
    verilate(filenames, Seq.empty, timeOut)
  }

  def compare(reference: ArrayBuffer[String], results: ArrayBuffer[String])(implicit
      ctx: AssemblyContext
  ): Boolean = {
    val numLines          = reference.length min results.length
    var ok                = true
    implicit val loggerId = new HasLoggerId { val id = "result-check" }

    @tailrec
    def check(index: Int): Unit = {
      if (index == reference.length) {
        if (results.length > reference.length) {
          ctx.logger.error(
            s"Too many results, expected ${reference.length} but got ${results.length} lines!"
          )
          ok = false
        } else {
          ctx.logger.info("Success")
        }
      } else {
        if (index >= results.length) {
          ctx.logger.error(
            s"Not enough results, expected ${reference.length} but got ${results.length} lines!"
          )
          ok = false
        } else {
          if (reference(index) != results(index)) {
            ctx.logger.error(
              s"line ${index + 1} does not match:\ngot:${results(index)}\nref:${reference(index)}"
            )
            ok = false
          } else {
            check(index + 1)
          }
        }

      }
    }
    if (reference.length != 0) {
      check(0)
      ok
    } else if (results.length != 0) {
      ctx.logger.error(
        s"Did not expect any results but have ${results.length} lines"
      )
      false
    } else {
      true
    }

  }

}
trait YosysUnitTest {
  import scala.sys.process.{Process, ProcessLogger}

  def testIterations: Int
  // def topModule: String
  def code: TestCode
  def testDir: Path

  def dumpAll: Boolean = false

  // Does not parallelize, but simply performs all optimizations before parallelization.
  final def lowerAndRun(): Unit = run((masmFile => context => lowInterpreter(masmFile)(context)))

  final def run(
      // masm file -> interpreted output
      interpreter: (String => (AssemblyContext => ArrayBuffer[String])) =
        (masmFile => context => interpret(masmFile)(context))
      // masmFile and context are passed when invoking `interpret`. They are not known yet.
  ): Unit = {

    implicit val ctx = defaultContext(dumpAll)

    try {
      // copy the unit test verilog files to test directory
      val filename = code match {
        case CodeText(src) =>
          dump("test.sv", src).getFileName().toString()
        case CodeResource(path) =>
          copyResource(path).getFileName().toString()
      }
      // generate test bench using yosys
      val tbFilename = yosysTestbench(filename)
      // get the reference results by simulating using Verilator
      val reference = verilate(Seq(filename, tbFilename))

      if (dumpAll) {
        dump("reference.txt", reference.mkString("\n"))
      }
      // compile the circuit and its test bench using yosys

      val manticore = yosysCompile(Seq(filename, tbFilename), ctx.dump_all)

      val results = interpreter(manticore)(ctx)

      if (dumpAll) {
        dump("results.txt", results.mkString("\n"))
      }

      if (!YosysUnitTest.compare(reference, results)) {
        if (!dumpAll) {
          dump("results.txt", results.mkString("\n"))
          dump("reference.txt", reference.mkString("\n"))
        }

        ctx.logger.fail("encountered error(s)!")(LoggerId("check"))
      }

    } catch {
      case e: Exception =>
        println(
          s"Failed the test because: ${e.getMessage()}\nin: ${testDir.toString()}"
        )
        e.printStackTrace()
        ctx.logger.flush()
        throw new CompilationFailureException("test failed")
    }
  }

  private final def defaultContext(dump_all: Boolean = false): AssemblyContext =
    AssemblyContext(
      dump_all = dump_all,
      dump_dir = Some(testDir.toFile),
      log_file = Some(testDir.resolve("run.log").toFile),
      max_cycles = testIterations + 1000
    )

  def yosysSelection: Seq[YosysPass] = Nil
  private def yosysRunnables = {
    val prelim   = Yosys.PreparationPasses()
    val lowering = Yosys.LoweringPasses
    if (yosysSelection.nonEmpty) {
      yosysSelection.foldLeft(prelim) { case (agg, p) =>
        agg andThen p
      } andThen lowering
    } else {
      prelim andThen lowering
    }
  }

  private final def dump(filename: String, text: String): Path = {
    val fp      = testDir.resolve(filename)
    val printer = new PrintWriter(fp.toFile)
    printer.print(text)
    printer.close()
    fp
  }
  private final def copyResource(resourcePath: String): Path = {
    val text     = scala.io.Source.fromResource(resourcePath).mkString
    val filename = Path.of(resourcePath).getFileName().toString()
    dump(filename, text)
  }

  final def verilate(filenames: Seq[String])(implicit
      ctx: AssemblyContext
  ): ArrayBuffer[String] = {

    YosysUnitTest.verilate(
      filenames.map(testDir.resolve(_)),
      testIterations + 1000
    )

  }

  private final def yosysTestbench(
      filename: String
  )(implicit ctx: AssemblyContext): String = {

    implicit val loggerId = new HasLoggerId { val id = "Yosys-Auto-TB" }
    import manticore.compiler.frontend.yosys.Implicits.passProxyToTransformation
    val tbname = s"tb_$filename"
    val tbGen = YosysBackendProxy(
      "manticore_tb",
      testDir.resolve(tbname)
    ) << "-n" << s"$testIterations"

    val yosysCompiler = YosysVerilogReader andThen
      Yosys.Hierarchy << "-auto-top" << "-check" andThen
      Yosys.Proc andThen
      YosysRunner(testDir, tbGen)

    yosysCompiler(Seq(testDir.resolve(filename)))

    tbname
  }
  private final def yosysCompile(
      verilog: Seq[String],
      dump: Boolean = false
  )(implicit ctx: AssemblyContext): String = {

    val yosysCompiler =
      YosysVerilogReader andThen yosysRunnables andThen YosysRunner(testDir)

    yosysCompiler(verilog.map(testDir.resolve(_))).toAbsolutePath().toString()

  }

  def interpret(
      filename: String
  )(implicit ctx: AssemblyContext): ArrayBuffer[String] = {

    val parsed = AssemblyFileParser(testDir.resolve(filename))
    val compiler =
      UnconstrainedNameChecker andThen
        UnconstrainedMakeDebugSymbols andThen
        UnconstrainedOrderInstructions andThen
        UnconstrainedCloseSequentialCycles
    val program = compiler(parsed)
    val out     = ArrayBuffer.empty[String]
    val interp = UnconstrainedInterpreter.instance(
      program = program,
      serial = Some(out += _)
    )
    interp.runCompletion()
    out
  }

  def lowInterpreter(
      filename: String
  )(implicit ctx: AssemblyContext): ArrayBuffer[String] = {
    val parsed = AssemblyFileParser(testDir.resolve(filename))
    val compiler =
      ManticorePasses.frontend andThen
        ManticorePasses.middleend andThen
        ManticorePasses.ToPlaced andThen
        CustomLutInsertion.pre andThen
        PlacedIRCloseSequentialCycles
    val program = compiler(parsed)
    val out     = ArrayBuffer.empty[String]
    val interp = AtomicInterpreter.instance(
      program = program,
      serial = Some(out += _)
    )
    interp.interpretCompletion()
    out
  }
}
