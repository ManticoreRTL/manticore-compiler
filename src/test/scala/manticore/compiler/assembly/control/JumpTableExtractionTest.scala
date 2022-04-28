package manticore.compiler.assembly.control

import manticore.compiler.UnitFixtureTest
import manticore.compiler.ManticorePasses
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedJumpTableConstruction
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedInterpreter
import scala.io.Source
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedCloseSequentialCycles
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.UnitTestMatchers
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedNameChecker
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedMakeDebugSymbols
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedOrderInstructions
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedRemoveAliases
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedDeadCodeElimination
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedRenameVariables
import manticore.compiler.assembly.levels.unconstrained.width.WidthConversion
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRConstantFolding
import manticore.compiler.HasLoggerId
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRCommonSubExpressionElimination
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRParMuxDeconstructionTransform
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.assembly.levels.Transformation

class JumpTableExtractionTest extends UnitFixtureTest with UnitTestMatchers {

  behavior of "JumpTable extraction"

  val Optimizations = UnconstrainedIRConstantFolding followedBy
    UnconstrainedIRCommonSubExpressionElimination followedBy
    UnconstrainedDeadCodeElimination

  object AluTestCommons {
    def getAluSource(fixture: FixtureParam) = {
      val fnames = Seq(
        "op1_rom.data",
        "op2_rom.data",
        "result_rom.data",
        "zero_rom.data",
        "ctrl_rom.data"
      )
      val fpaths = fnames.map { case fname =>
        fixture.dump(
          fname,
          Source.fromResource(s"control/alu/${fname}").mkString
        )
      }
      val source: String = Source
        .fromResource("control/alu/main.masm")
        .getLines()
        .map { l =>
          fnames.zip(fpaths).foldLeft(l) { case (ll, (fname, fpath)) =>
            ll.replace(s"*${fname}*", fpath.toAbsolutePath().toString())
          }
        }
        .mkString("\n")
      source
    }
    def context(fixture: FixtureParam) = AssemblyContext(
      output_dir = Some(fixture.test_dir.resolve("out").toFile()),
      dump_all = true,
      dump_dir = Some(fixture.test_dir.resolve("dumps").toFile()),
      expected_cycles = Some(100),
      debug_message = true
    )

    def doTest(optimize: Boolean, useJump: Boolean, fixture: FixtureParam) = {
      val ctx = AluTestCommons.context(fixture)
      val program = AssemblyParser(AluTestCommons.getAluSource(fixture), ctx)

      def compiler =
        UnconstrainedNameChecker followedBy
          UnconstrainedMakeDebugSymbols followedBy
          UnconstrainedRenameVariables followedBy
          UnconstrainedOrderInstructions followedBy
          Transformation.predicated(optimize)(Optimizations) followedBy
          Transformation.predicated(useJump)(
            UnconstrainedJumpTableConstruction
          ) followedBy
          UnconstrainedIRParMuxDeconstructionTransform followedBy
          WidthConversion.transformation followedBy
          Transformation.predicated(optimize)(Optimizations) followedBy
          UnconstrainedCloseSequentialCycles followedBy
          UnconstrainedInterpreter

      val (transformed, _) = compiler(program, ctx)
      val hasJumpTable = transformed.processes.head.body.exists {
        _.isInstanceOf[UnconstrainedIR.JumpTable]
      }
      val hasParMux = transformed.processes.head.body.exists {
        _.isInstanceOf[UnconstrainedIR.ParMux]
      }

      ctx.logger.info(ctx.stats.asYaml)(new HasLoggerId { val id = "Test" })
      // hasJumpTable shouldEqual useJump
      // hasParMux shouldEqual false
    }
  }

  "ALU" should "work correctly without optimizations and without a JumpTable" in {
    fixture =>
      AluTestCommons.doTest(false, false, fixture)
  }
  "ALU" should "work correctly with optimizations and without a JumpTable" in {
    fixture =>
      AluTestCommons.doTest(true, false, fixture)
  }
  "ALU" should "work correctly with a sub-optimal JumpTable" in { fixture =>
    AluTestCommons.doTest(false, true, fixture)
  }
  "ALU" should "work correctly with an optimal JumpTable" in { fixture =>
    AluTestCommons.doTest(true, true, fixture)
  }

}
