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

class JumpTableExtractionTest extends UnitFixtureTest with UnitTestMatchers {

  behavior of "JumpTable extraction"

  val InitialPasses = UnconstrainedNameChecker followedBy
    UnconstrainedMakeDebugSymbols followedBy
    UnconstrainedOrderInstructions followedBy
    UnconstrainedRemoveAliases followedBy
    UnconstrainedDeadCodeElimination followedBy
    UnconstrainedRenameVariables

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
  }
  // "ALU" should "work correctly with a JumpTable before width conversion " in {
  //   fixture =>
  //     val ctx = AluTestCommons.context(fixture)
  //     val program = AssemblyParser(AluTestCommons.getAluSource(fixture), ctx)
  //     // run the program
  //     def compiler = InitialPasses followedBy
  //       UnconstrainedJumpTableConstruction followedBy
  //       UnconstrainedCloseSequentialCycles followedBy
  //       UnconstrainedInterpreter
  //     val (transformed, _) = compiler(program, ctx)
  //     // then make sure there was at least one jump table
  //     val hasJumpTable = transformed.processes.head.body.exists {
  //       _.isInstanceOf[UnconstrainedIR.JumpTable]
  //     }
  //     hasJumpTable shouldEqual true

  // }

  "ALU" should "work correctly with a JumpTable after width conversion" in {
    fixture =>
      val ctx = AluTestCommons.context(fixture)
      val program = AssemblyParser(AluTestCommons.getAluSource(fixture), ctx)
      def compiler =
        InitialPasses followedBy
          UnconstrainedJumpTableConstruction followedBy
          WidthConversion.transformation followedBy
          UnconstrainedRemoveAliases followedBy
          // UnconstrainedDeadCodeElimination followedBy
          UnconstrainedCloseSequentialCycles followedBy
          UnconstrainedInterpreter
      val (transformed, _) = compiler(program, ctx)
      val hasJumpTable = transformed.processes.head.body.exists {
        _.isInstanceOf[UnconstrainedIR.JumpTable]
      }
      hasJumpTable shouldEqual true
  }

}
