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

  def run(test_name: String): Unit = {
    val ctx =
      AssemblyContext(dump_all = true, dump_dir = Some(cwd.toFile()))
    val backend = UnconstrainedNameChecker followedBy
      UnconstrainedMakeDebugSymbols followedBy
      UnconstrainedOrderInstructions followedBy
      UnconstrainedRemoveAliases followedBy
      UnconstrainedDeadCodeElimination followedBy
      UnconstrainedCloseSequentialCycles followedBy
      UnconstrainedInterpreter followedBy
      UnconstrainedBreakSequentialCycles

    val parsed = AssemblyParser(cwd.resolve(s"${test_name}.masm").toFile(), ctx)
    backend(parsed, ctx)

  }


  it should "handle RV32_IntegerRR" in {
      run("RV32_IntegerRR")
  }

  it should "handle RV32_Load" in {
      run("RV32_Load")
  }

  it should "handle RV32_Store" in {
      run("RV32_Store")
  }

}
