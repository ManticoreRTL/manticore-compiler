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

sealed trait TestCode
case class CodeText(src: String) extends TestCode
case class CodeResource(path: String) extends TestCode

trait YosysUnitTest {
  import scala.sys.process.{Process, ProcessLogger}

  def testIterations: Int
  // def topModule: String
  def code: TestCode
  def testDir: Path

  def dumpAll: Boolean = false

  final def run(): Unit = {

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

      val results = interpret(manticore)

      if (dumpAll) {
        dump("results.txt", results.mkString("\n"))
      }

      val numLines = reference.length min results.length
      implicit val loggerId = new HasLoggerId { val id = "result-check" }

      @tailrec
      def check(index: Int): Unit = {
        if (index == reference.length) {
          if (results.length > reference.length) {
            ctx.logger.error(
              s"Too many results, expected ${reference.length} but got ${results.length} lines!"
            )
          } else {
            ctx.logger.info("Success")
          }
        } else {
          if (index > results.length) {
            ctx.logger.error(
              s"Not enough results, expected ${reference.length} but got ${results.length} lines!"
            )
          } else {
            if (reference(index) != results(index)) {
              ctx.logger.error(
                s"line ${index + 1} does not match:\ngot:${results(index)}\nref:${reference(index)}"
              )
            } else {
              check(index + 1)
            }
          }

        }
      }
      assert(reference.length > 0)
      check(0)
      if (ctx.logger.countErrors() > 0) {
        if (!dumpAll) {
          dump("results.txt", results.mkString("\n"))
          dump("reference.txt", reference.mkString("\n"))
        }
        ctx.logger.fail("encountered error(s)!")
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
    val prelim = Yosys.PreparationPasses
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
    val fp = testDir.resolve(filename)
    val printer = new PrintWriter(fp.toFile)
    printer.print(text)
    printer.close()
    fp
  }
  private final def copyResource(resourcePath: String): Path = {
    val text = scala.io.Source.fromResource(resourcePath).mkString
    val filename = Path.of(resourcePath).getFileName().toString()
    dump(filename, text)
  }

  private final def verilate(filenames: Seq[String])(implicit
      ctx: AssemblyContext
  ): ArrayBuffer[String] = {

    implicit val loggerId = new HasLoggerId { val id = "verilator-compile" }
    val flags = Seq(
      "-CFLAGS \"-DVL_USER_FINISH\"",
      "-cc",
      "--exe",
      "--build",
      "-Os",
      "-x-assign 0",
      "-Wno-WIDTH",
      "--top-module Main"
    ).mkString(" ")

    copyResource("integration/yosys/VHarness.cpp")

    val verilteCmd = s"verilator $flags ${filenames.mkString(" ")} VHarness.cpp"
    ctx.logger.info(s"Running command:\n${verilteCmd}")
    val ret = Process(
      command = verilteCmd,
      cwd = testDir.toFile
    ) ! ProcessLogger(msg => ctx.logger.info(msg))
    if (ret != 0) {
      if (ret != 0) {
        ctx.logger.fail("Failed compiling with verilator")
      }
    }

    // if we have been successful with verilator compilation, we should now
    // run the simulation

    val simCmd = s"obj_dir/VMain ${testIterations + 1000}"
    val out = ArrayBuffer.empty[String]
    val simRet = Process(
      command = simCmd,
      cwd = testDir.toFile
    ) ! ProcessLogger(out += _)
    if (simRet != 0) {
      println(out.mkString("\n"))
      throw new CompilationFailureException(
        s"Failed running reference simulation $simCmd: $ret"
      )
    }
    out
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

  private final def interpret(
      filename: String
  )(implicit ctx: AssemblyContext): ArrayBuffer[String] = {

    val parsed =
      AssemblyParser(testDir.resolve(filename).toFile())
    val compiler =
      UnconstrainedNameChecker andThen
        UnconstrainedMakeDebugSymbols andThen
        UnconstrainedOrderInstructions andThen
        UnconstrainedCloseSequentialCycles
    val program = compiler(parsed)
    val out = ArrayBuffer.empty[String]
    val interp = UnconstrainedInterpreter.instance(
      program = program,
      serial = Some(out += _)
    )
    interp.runCompletion()
    out
  }
}
