package manticore.compiler.integration.yosys

import manticore.compiler.UnitFixtureTest
import scala.sys.process.Process
import scala.sys.process.ProcessLogger
import manticore.compiler.CompilationFailureException
import manticore.compiler.HasLoggerId
import manticore.compiler.AssemblyContext
import java.io.File
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedNameChecker
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedMakeDebugSymbols
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedOrderInstructions
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.ManticorePasses
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedCloseSequentialCycles
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRInterpreterMonitor
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedInterpreter
import manticore.compiler.UnitTestMatchers

class CounterPrintTester extends UnitFixtureTest with UnitTestMatchers {

  behavior of "A simple Verilog counter"

  def minimalCompile(file: File)(implicit ctx: AssemblyContext) = {
    val parsed = AssemblyParser(file, ctx)
    val compiler =
      UnconstrainedNameChecker followedBy
        UnconstrainedMakeDebugSymbols followedBy
        UnconstrainedOrderInstructions followedBy
        UnconstrainedCloseSequentialCycles

    compiler(parsed, ctx)._1
  }

  "counter" should "match external simulation" in { fixture =>
    fixture.dump(
      "counter.v",
      scala.io.Source.fromResource("integration/yosys/counter.v").mkString
    )
    implicit val loggerId = new HasLoggerId { val id = "TEST" }
    implicit val ctx = AssemblyContext(
      dump_all = true,
      dump_dir = Some(fixture.test_dir.toFile),
      quiet = false
    )
    val ysScript = fixture.dump(
      "counter.ys",
      scala.io.Source.fromResource("integration/yosys/counter.ys").mkString
    )

    val harness = fixture.dump(
      "VHarness.cpp",
      scala.io.Source.fromResource("integration/yosys/VHarness.cpp").mkString
    )

    println("Calling Yosys")

    val logger = ProcessLogger(
      fout => ctx.logger.info(fout),
      ferr => ctx.logger.error(ferr)
    )
    val verilator = Process(
      command =
        "verilator -CFLAGS \"-DVL_USER_FINISH\" -cc --exe --build -Os -x-assign 0 -Wno-WIDTH --top-module Main counter.v VHarness.cpp",
      cwd = fixture.test_dir.toFile()
    ) ! logger

    if (verilator != 0) {
      throw new CompilationFailureException(s"iverilog returned ${verilator}")
    }

    val refBuilder = scala.collection.mutable.ArrayBuffer.empty[String]

    val vsim = Process(
      command = "obj_dir/VMain 1000",
      cwd = fixture.test_dir.toFile()
    ) ! ProcessLogger(ln => refBuilder += ln)

    if (vsim != 0) {
      throw new CompilationFailureException(s"verilator sim returned ${vsim}")
    }

    val ret = Process(
      command = s"yosys -s counter.ys",
      cwd = fixture.test_dir.toFile()
    ).!(
      ProcessLogger(
        out => ctx.logger.info(out),
        err => ctx.logger.error(err)
      )
    )

    if (ret != 0) {
      throw new CompilationFailureException(s"Yosys returned ${ret}")
    }

    val compiled =
      minimalCompile(fixture.test_dir.resolve("counter.masm").toFile())

    val resBuilder = scala.collection.mutable.ArrayBuffer.empty[String]

    val interp = UnconstrainedInterpreter.instance(
      program = compiled,
      serial = Some(resBuilder += _)
    )

    while (interp.runVirtualCycle() == None) {}

    val numLines = resBuilder.length max refBuilder.length

    resBuilder ++= Seq.fill(numLines - resBuilder.length) { "" }
    refBuilder ++= Seq.fill(numLines - refBuilder.length) { "" }

    for (i <- 0 until numLines) {

      withClue(s"Result mismatch: ${resBuilder(i)} vs ${refBuilder(i)}") {
        resBuilder(i) shouldBe refBuilder(i)
      }

    }

  }

}
