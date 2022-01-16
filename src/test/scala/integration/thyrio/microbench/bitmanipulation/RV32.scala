package integration.thyrio.microbench.bitmanipulation

import integration.ThyrioUnitTest
import integration.thyrio.integration.thyrio.ExternalTool
import integration.thyrio.integration.thyrio.ThyrioFrontend
import integration.thyrio.integration.thyrio.Verilator
import integration.thyrio.integration.thyrio.Make
import integration.thyrio.integration.thyrio.Python3

import java.nio.file.StandardCopyOption
import java.nio.file.Path
import java.nio.file.Files
import manticore.assembly.levels.unconstrained.UnconstrainedNameChecker
import manticore.assembly.levels.unconstrained.UnconstrainedMakeDebugSymbols
import manticore.assembly.levels.unconstrained.UnconstrainedOrderInstructions
import manticore.assembly.levels.unconstrained.UnconstrainedRemoveAliases
import manticore.assembly.levels.unconstrained.UnconstrainedDeadCodeElimination
import manticore.assembly.levels.unconstrained.UnconstrainedCloseSequentialCycles
import manticore.assembly.levels.unconstrained.UnconstrainedInterpreter
import manticore.assembly.levels.unconstrained.UnconstrainedBreakSequentialCycles
import manticore.compiler.AssemblyContext
import manticore.assembly.parser.AssemblyParser
import manticore.UnconstrainedTest
import manticore.assembly.levels.unconstrained.width.WidthConversionCore
import manticore.assembly.levels.AssemblyTransformer
import manticore.assembly.levels.unconstrained.UnconstrainedIR
import manticore.assembly.levels.Transformation
import manticore.assembly.levels.unconstrained.UnconstrainedRenameVariables

class RV32 extends ThyrioUnitTest {

  val requiredTools: Seq[ExternalTool] = Seq(
    Make,
    ThyrioFrontend,
    Python3,
    Verilator
  )

  behavior of "RV32 bit-manipulation test"

  checkInstalled()
  val cwd = createDumpDirectory()

  // copy the resources
  val resource_dir =
    getClass.getResource("/integration/microbench/bitmanipulation").toURI()
  def copyResource(name: String): Unit = Files.copy(
    Path.of(resource_dir).resolve(name),
    cwd.resolve(name),
    StandardCopyOption.REPLACE_EXISTING
  )

  Seq("Makefile", "RV32.py", "VMake.mk", "VTester.cpp") foreach {
    copyResource(_)
  }

  Make.invoke(Seq(), cwd.toFile()) { println(_) }

  def initialPhases() = UnconstrainedNameChecker followedBy
    UnconstrainedMakeDebugSymbols followedBy
    UnconstrainedOrderInstructions followedBy
    UnconstrainedRemoveAliases followedBy
    UnconstrainedDeadCodeElimination followedBy
    UnconstrainedCloseSequentialCycles followedBy
    UnconstrainedInterpreter followedBy
    UnconstrainedBreakSequentialCycles
  def finalPhases() = WidthConversionCore followedBy
    UnconstrainedRenameVariables followedBy
    UnconstrainedDeadCodeElimination followedBy
    UnconstrainedCloseSequentialCycles followedBy
    UnconstrainedInterpreter followedBy
    UnconstrainedBreakSequentialCycles

  type Phase =
    Transformation[UnconstrainedIR.DefProgram, UnconstrainedIR.DefProgram]

  def run(test_name: String)(phases: => Phase): Unit = {
    val ctx =
      AssemblyContext(
        dump_all = true,
        dump_dir = Some(cwd.resolve(test_name).toFile())
      )

    val parsed = AssemblyParser(cwd.resolve(s"${test_name}.masm").toFile(), ctx)
    phases(parsed, ctx)

  }

  // def run(test_name: String): Unit = {}

  it should "handle RV32_IntegerRR initial unconstrained phases" in {
    run("RV32_IntegerRR") { initialPhases() }
  }

  it should "handle RV32_IntegerRR final unconstrained phases" in {
    run("RV32_IntegerRR") { initialPhases() followedBy finalPhases() }
  }

  it should "handle RV32_Load initial unconstrained phases" in {
    run("RV32_Load") { initialPhases() }
  }

  it should "handle RV32_Load final unconstrained phases" in {
    run("RV32_Load") { initialPhases() followedBy finalPhases() }
  }

  it should "handle RV32_Store initial unconstrained phases" in {
    run("RV32_Store") { initialPhases() }
  }

  it should "handle RV32_Store final unconstrained phases" in {
    run("RV32_Store") { initialPhases() followedBy finalPhases() }
  }

}
