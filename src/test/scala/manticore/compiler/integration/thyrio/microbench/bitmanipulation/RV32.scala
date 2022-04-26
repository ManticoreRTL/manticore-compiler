package manticore.compiler.integration.thyrio.microbench.bitmanipulation

import manticore.compiler.integration.ThyrioUnitTest
import manticore.compiler.integration.thyrio.ExternalTool
import manticore.compiler.integration.thyrio.ThyrioFrontend
import manticore.compiler.integration.thyrio.Verilator
import manticore.compiler.integration.thyrio.Make
import manticore.compiler.integration.thyrio.Python3

import java.nio.file.StandardCopyOption
import java.nio.file.Path
import java.nio.file.Files
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedNameChecker
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedMakeDebugSymbols
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedOrderInstructions
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedRemoveAliases
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedDeadCodeElimination
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedCloseSequentialCycles
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedInterpreter
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedBreakSequentialCycles
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.parser.AssemblyParser

import manticore.compiler.assembly.levels.unconstrained.width.WidthConversionCore
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.assembly.levels.Transformation
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedRenameVariables
import manticore.compiler.ManticorePasses
import manticore.compiler.assembly.ManticoreAssemblyIR




class RV32 extends ThyrioUnitTest {

  val requiredTools: Seq[ExternalTool] = Seq(
    Make,
    ThyrioFrontend,
    Python3,
    Verilator
  )

  behavior of "RV32 bit-manipulation test"

  checkInstalled()
  val cwd = root_dir
  Files.createDirectories(root_dir)

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

  type Phase[T <: ManticoreAssemblyIR#DefProgram] =
    Transformation[UnconstrainedIR.DefProgram, T]

  def run[T <: ManticoreAssemblyIR#DefProgram](
      test_name: String,
      f: FixtureParam
  )(phases: => Transformation[UnconstrainedIR.DefProgram, T]): Unit = {

    val parsed =
      AssemblyParser(cwd.resolve(s"${test_name}.masm").toFile(), f.ctx)
    phases(parsed, f.ctx)

  }

  // def run(test_name: String): Unit = {}

  it should "handle RV32_IntegerRR initial unconstrained phases" in { f =>
    run("RV32_IntegerRR", f) {
      ManticorePasses.frontend followedBy
        ManticorePasses.FrontendInterpreter(true)
    }
  }

  it should "handle RV32_IntegerRR final unconstrained phases" in { f =>
    run("RV32_IntegerRR", f) {
      ManticorePasses.frontend followedBy
        ManticorePasses.middleend followedBy
        ManticorePasses.FrontendInterpreter(true)
    }
  }

  it should "handle RV32_IntegerRR final placed phases" in { f =>
    run("RV32_IntegerRR", f) {
      ManticorePasses.frontend followedBy
        ManticorePasses.middleend followedBy
        ManticorePasses.backend followedBy
        ManticorePasses.BackendInterpreter(true)
    }
  }

  it should "handle RV32_Load initial unconstrained phases" in { f =>
    run("RV32_Load", f) {
      ManticorePasses.frontend followedBy
        ManticorePasses.FrontendInterpreter(true)
    }
  }

  it should "handle RV32_Load final unconstrained phases" in { f =>
    run("RV32_Load", f) {
      ManticorePasses.frontend followedBy
        ManticorePasses.middleend followedBy
        ManticorePasses.FrontendInterpreter(true)
    }
  }

  it should "handle RV32_Load final placed phases" in { f =>
    run("RV32_Load", f) {
      ManticorePasses.frontend followedBy
        ManticorePasses.middleend followedBy
        ManticorePasses.backend followedBy
        ManticorePasses.BackendInterpreter(true)
    }
  }

  it should "handle RV32_Store initial unconstrained phases" in { f =>
    run("RV32_Store", f) {
      ManticorePasses.frontend followedBy
        ManticorePasses.FrontendInterpreter(true)
    }
  }

  it should "handle RV32_Store final unconstrained phases" in { f =>
    run("RV32_Store", f) {
      ManticorePasses.frontend followedBy
        ManticorePasses.middleend followedBy
        ManticorePasses.FrontendInterpreter(true)
    }
  }
  it should "handle RV32_Store final placed phases" in { f =>
    run("RV32_Store", f) {
      ManticorePasses.frontend followedBy
        ManticorePasses.middleend followedBy
        ManticorePasses.backend followedBy
        ManticorePasses.BackendInterpreter(true)
    }
  }

  it should "handle RV32_Store on a single processor" in { f =>




  }

}