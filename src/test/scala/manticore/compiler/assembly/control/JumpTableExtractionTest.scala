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
import manticore.compiler.assembly.levels.placed.UnconstrainedToPlacedTransform
import manticore.compiler.assembly.levels.placed.JumpTableNormalizationTransform
import manticore.compiler.assembly.levels.placed.JumpLabelAssignmentTransform
import manticore.compiler.assembly.levels.placed.TaggedInstruction
import manticore.compiler.assembly.levels.placed.interpreter.AtomicInterpreter
import manticore.compiler.assembly.utils.XorShift128
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRInterpreterMonitor
import manticore.compiler.assembly.levels.placed.PlacedIRCloseSequentialCycles
import manticore.compiler.assembly.levels.placed.interpreter.PlacedIRInterpreterMonitor
import manticore.compiler.assembly.levels.InterpreterMonitor
import manticore.compiler.assembly.levels.placed.PlacedIRDebugSymbolRenamer

class JumpTableExtractionTest extends UnitFixtureTest with UnitTestMatchers {

  behavior of "JumpTable extraction"

  object Commons {
    val Optimizations = UnconstrainedIRConstantFolding followedBy
      UnconstrainedIRCommonSubExpressionElimination followedBy
      UnconstrainedDeadCodeElimination
    def context(fixture: FixtureParam) = AssemblyContext(
      output_dir = Some(fixture.test_dir.resolve("out").toFile()),
      dump_all = true,
      dump_dir = Some(fixture.test_dir.resolve("dumps").toFile()),
      expected_cycles = Some(100),
      debug_message = true,
      log_file = Some(fixture.test_dir.resolve("run.log").toFile())
    )

    def frontend(optimize: Boolean, useJump: Boolean) =
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
        Transformation.predicated(optimize)(Optimizations)
    def backend =
      UnconstrainedToPlacedTransform followedBy
        PlacedIRCloseSequentialCycles followedBy
        JumpTableNormalizationTransform followedBy
        JumpLabelAssignmentTransform
  }
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

    def doTest(optimize: Boolean, useJump: Boolean, fixture: FixtureParam) = {
      val ctx = Commons.context(fixture)
      val program = AssemblyParser(AluTestCommons.getAluSource(fixture), ctx)

      def compiler = Commons.frontend(optimize, useJump) followedBy
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
  "ALU" should "work correctly with an optimal JumpTable in PlacedIR" in {
    fixture =>
      val ctx = Commons.context(fixture)
      val program = AssemblyParser(AluTestCommons.getAluSource(fixture), ctx)

      val compiler =
        Commons.frontend(optimize = true, useJump = true) followedBy
          UnconstrainedToPlacedTransform followedBy JumpTableNormalizationTransform followedBy
          JumpLabelAssignmentTransform followedBy PlacedIRCloseSequentialCycles
      val (transformed, _) = compiler(program, ctx)
      val instMemory =
        TaggedInstruction.indexedTaggedBlock(transformed.processes.head)(ctx)
      AtomicInterpreter(transformed, ctx)

  }

  "JumpTableExtraction" should "should construct a jump table" in { fixture =>
    val randGen = XorShift128("randGen0")

    val source = s"""
        .prog:
            .proc p0:
            ${randGen.registers}
            @TRACK[name = "result"]
            .reg result 32 .input rescurr 0 .output resnext

            .wire l12 32
            .wire l13 32
            .wire l14 32
            .wire l15 32

            .wire sel 3

            .wire c12 1
            .wire c13 1
            .wire c14 1


            .const eleven 32 11
            .const eight  32 8
            .const nineteen 32 19
            .const twentyNine 32 29

            .const const1 3 1
            .const const2 3 2
            .const const3 3 3

            ${randGen.code}


            SRL sel, ${randGen.randNext}, twentyNine;

            SEQ c12, sel, const1;
            SEQ c13, sel, const2;
            SEQ c14, sel, const3;

            ADD l12, ${randGen.randNext}, eleven;
            SRL l13, ${randGen.randNext}, eight;
            SUB l14, ${randGen.randNext}, eleven;
            MOV l15, ${randGen.randNext};

            PARMUX resnext, c12 ? l12, c13 ? l13, c14 ? l14, l15;

      """
    val ctx = Commons.context(fixture)

    val parsed = AssemblyParser(source, ctx)

    val compiler = Commons.frontend(
      true,
      true
    ) followedBy Commons.backend

    val converted = compiler(parsed, ctx)._1


    val monitor = PlacedIRInterpreterMonitor(
      converted
    )(ctx)
    // println(s"Watching ${monitor.keys}")
    fixture.dump(
      "human_readable.masm",
      PlacedIRDebugSymbolRenamer.makeHumanReadable(converted)(ctx).serialized
    )
    val interp = AtomicInterpreter.instance(
      program = converted,
      monitor = Some(monitor)
    )(ctx)

    def nextExpected(): Int = {

      val currRnd = randGen.nextRef()
      val sel = currRnd >>> 29

      val res = sel match {
        case 1 => currRnd + 11
        case 2 => currRnd >>> 8
        case 3 => currRnd - 11
        case _ => currRnd
      }

      res
    }

    for (cycle <- 0 to 10000) {
      interp.interpretVirtualCycle() shouldBe Nil
      val ref = nextExpected()
      val got = monitor.read("resnext").toInt
      withClue("random generator mismatch") {
        monitor.read(randGen.randNext).toInt shouldEqual randGen.currRef()
      }
      withClue(s"Result mismatch:  ") { got shouldEqual ref }
    }

  }

}
