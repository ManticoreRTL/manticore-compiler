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

sealed trait TestCode
case class CodeText(src: String) extends TestCode
case class CodeResource(path: String) extends TestCode

trait YosysUnitTest {
  import scala.sys.process.{Process, ProcessLogger}

  def testIterations: Int
  // def topModule: String
  def code: TestCode
  def testDir: Path

  final def run(): Unit = {

    implicit val ctx = defaultContext(false)

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

      // compile the circuit and its test bench using yosys

      val manticore = yosysCompile(Seq(filename, tbFilename), ctx.dump_all)

      val results = interpret(manticore)

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
                s"line ${index} does not match:\ngot:${results(index)}\nref:${reference(index)}"
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
        dump("results.txt", results.mkString("\n"))
        dump("reference.txt", reference.mkString("\n"))
        ctx.logger.fail("encountered error(s)!")
      }
    } catch {
      case e: Exception =>
        println(
          s"Failed the test because: ${e.getMessage()}\nin: ${testDir.toString()}"
        )
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

  def yosysSelectPasses: Seq[String] = Nil
  private def yosysDefaultPasses = Seq(
    s"hierarchy -auto-top -check",
    "proc",
    "opt",
    "opt_reduce",
    "opt_demorgan",
    "opt_clean",
    "memory_collect",
    "memory_unpack",
    "write_rtlil original.rtl"
  ) ++ yosysSelectPasses ++ Seq(
    "manticore_init", // do basic stuff such as setting track attributes
    "flatten", // flatten the design
    "manticore_meminit", // remove $meminit cells
    "opt", // optimize the flattened design, maybe too expensive if the design is large
    "manticore_dff", // turn every state element into a $dff and fail if not possible
    "manticore_opt_replicate", // optimize bit-replication SigSpecs for code generation
    "manticore_subword", // remove any subword assignment on lhs of connections and output of cells
    "write_rtlil main.rtl", // print the final result
    "manticore_check", // check for existence of a unique clock
    "manticore_writer main.masm" // write manticore assembly
  )
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
    val tbname = s"tb_$filename"
    val ysCmd = Seq(
      s"read_verilog -sv -masm $filename",
      s"hierarchy -auto-top -check",
      s"proc",
      s"manticore_tb -n $testIterations tb_$filename"
    ).mkString("; ")

    val genCmd = s"yosys -p \"$ysCmd\" -Q -T"
    ctx.logger.info(s"Running command:\n${genCmd}")
    val ret = Process(
      command = genCmd,
      cwd = testDir.toFile
    ) ! ProcessLogger(ctx.logger.info(_))
    if (ret != 0) {
      ctx.logger.fail("Failed creating testbench")
    }
    tbname
  }
  private final def yosysCompile(
      verilog: Seq[String],
      dump: Boolean = false
  )(implicit ctx: AssemblyContext): String = {

    implicit val loggerId = new HasLoggerId { val id = "Yosys" }
    val reads = verilog.map { f => s"read_verilog -sv -masm  $f" }
    val script = (if (dump) {
                    reads ++ yosysDefaultPasses.zipWithIndex.flatMap {
                      case (p, ix) =>
                        Seq(
                          p,
                          s"write_rtlil after_$ix.rtl"
                        )
                    }
                  } else {
                    reads ++ yosysDefaultPasses
                  }).mkString("; ")
    val cmd = s"yosys -p \"$script\" -Q -T"
    ctx.logger.info(s"Running command: ${cmd}")
    val ret = Process(
      command = cmd,
      cwd = testDir.toFile
    ) ! ProcessLogger(ctx.logger.info(_))

    if (ret != 0) {
      ctx.logger.fail("Failed compiling with Yosys")
    }

    "main.masm"

  }

  private final def interpret(
      filename: String
  )(implicit ctx: AssemblyContext): ArrayBuffer[String] = {

    val parsed =
      AssemblyParser(testDir.resolve(filename).toFile(), ctx)
    val compiler =
      UnconstrainedNameChecker followedBy
        UnconstrainedMakeDebugSymbols followedBy
        UnconstrainedOrderInstructions followedBy
        UnconstrainedCloseSequentialCycles
    val program = compiler(parsed, ctx)._1
    val out = ArrayBuffer.empty[String]
    val interp = UnconstrainedInterpreter.instance(
      program = program,
      serial = Some(out += _)
    )
    interp.runCompletion()
    out
  }
}
