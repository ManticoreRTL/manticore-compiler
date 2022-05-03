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
import manticore.compiler.assembly.levels.placed.PlacedIR

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
      debug_message = true
      // log_file = Some(fixture.test_dir.resolve("run.log").toFile())
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

  // "ALU" should "work correctly without optimizations and without a JumpTable" in {
  //   fixture =>
  //     AluTestCommons.doTest(false, false, fixture)
  // }
  // "ALU" should "work correctly with optimizations and without a JumpTable" in {
  //   fixture =>
  //     AluTestCommons.doTest(true, false, fixture)
  // }
  // "ALU" should "work correctly with a sub-optimal JumpTable" in { fixture =>
  //   AluTestCommons.doTest(false, true, fixture)
  // }
  // "ALU" should "work correctly with an optimal JumpTable" in { fixture =>
  //   AluTestCommons.doTest(true, true, fixture)
  // }
  // "ALU" should "work correctly with an optimal JumpTable in PlacedIR" in {
  //   fixture =>
  //     val ctx = Commons.context(fixture)
  //     val program = AssemblyParser(AluTestCommons.getAluSource(fixture), ctx)

  //     val compiler =
  //       Commons.frontend(optimize = true, useJump = true) followedBy
  //         UnconstrainedToPlacedTransform followedBy JumpTableNormalizationTransform followedBy
  //         JumpLabelAssignmentTransform followedBy PlacedIRCloseSequentialCycles
  //     val (transformed, _) = compiler(program, ctx)
  //     val instMemory =
  //       TaggedInstruction.indexedTaggedBlock(transformed.processes.head)(ctx)
  //     AtomicInterpreter(transformed, ctx)

  // }

  // "JumpTable" should "should work with AtomicInterpreter and PlacedIRInterpreterMonitor" in {
  //   fixture =>
  //     val randGen = XorShift128("randGen0")

  //     val source = s"""
  //       .prog:
  //           .proc p0:
  //           ${randGen.registers}
  //           @TRACK[name = "result"]
  //           .reg result 32 .input rescurr 0 .output resnext

  //           .wire l12 32
  //           .wire l13 32
  //           .wire l14 32
  //           .wire l15 32

  //           .wire sel 3

  //           .wire c12 1
  //           .wire c13 1
  //           .wire c14 1

  //           .const eleven 32 11
  //           .const eight  32 8
  //           .const nineteen 32 19
  //           .const twentyNine 32 29

  //           .const const1 3 1
  //           .const const2 3 2
  //           .const const3 3 3

  //           ${randGen.code}

  //           SRL sel, ${randGen.randNext}, twentyNine;

  //           SEQ c12, sel, const1;
  //           SEQ c13, sel, const2;
  //           SEQ c14, sel, const3;

  //           ADD l12, ${randGen.randNext}, eleven;
  //           SRL l13, ${randGen.randNext}, eight;
  //           SUB l14, ${randGen.randNext}, eleven;
  //           MOV l15, ${randGen.randNext};

  //           PARMUX resnext, c12 ? l12, c13 ? l13, c14 ? l14, l15;

  //     """
  //     val ctx = Commons.context(fixture)

  //     val parsed = AssemblyParser(source, ctx)

  //     val compiler = Commons.frontend(
  //       true,
  //       true
  //     ) followedBy Commons.backend

  //     val converted = compiler(parsed, ctx)._1

  //     val monitor = PlacedIRInterpreterMonitor(
  //       converted
  //     )(ctx)
  //     // println(s"Watching ${monitor.keys}")
  //     fixture.dump(
  //       "human_readable.masm",
  //       PlacedIRDebugSymbolRenamer.makeHumanReadable(converted)(ctx).serialized
  //     )
  //     val interp = AtomicInterpreter.instance(
  //       program = converted,
  //       monitor = Some(monitor)
  //     )(ctx)

  //     def nextExpected(): Int = {

  //       val currRnd = randGen.nextRef()
  //       val sel = currRnd >>> 29

  //       val res = sel match {
  //         case 1 => currRnd + 11
  //         case 2 => currRnd >>> 8
  //         case 3 => currRnd - 11
  //         case _ => currRnd
  //       }

  //       res
  //     }

  //     for (cycle <- 0 to 10000) {
  //       interp.interpretVirtualCycle() shouldBe Nil
  //       val ref = nextExpected()
  //       val got = monitor.read("resnext").toInt
  //       withClue("random generator mismatch") {
  //         monitor.read(randGen.randNext).toInt shouldEqual randGen.currRef()
  //       }
  //       withClue(s"Result mismatch:  ") { got shouldEqual ref }
  //     }

  // }

  // "JumpTableConstruction" should "not construct a JumpTable when ParMux inputs have other users" in {
  //   fixture =>
  //     val rand0 = XorShift128(name = "rand0", seed = 88708797)
  //     val rand1 = XorShift128(name = "rand1", seed = 12776512)
  //     val rand2 = XorShift128(name = "rand2", seed = 57384123)
  //     val rand3 = XorShift128(name = "rand3", seed = 98016642)
  //     val rand4 = XorShift128(name = "rand4", seed = 7886834)

  //     val source = s"""
  //   .prog:
  //     .proc p0:

  //       ${rand0.registers}
  //       ${rand1.registers}
  //       ${rand2.registers}
  //       ${rand3.registers}
  //       ${rand4.registers}

  //       .wire const0 5 0

  //       .wire false 1 1

  //       .wire slice8 8
  //       .wire isEven 1
  //       .wire isOdd 1

  //       .const one8 8 1
  //       .const two8 8 2
  //       .const three8 8 3
  //       .const four8 8 4

  //       .wire c1 1
  //       .wire cc1 1
  //       .wire c2 1
  //       .wire cc2 1
  //       .wire c3 1
  //       .wire cc3 1
  //       .wire c4 1
  //       .wire cc4 1

  //       @TRACK[name = "RES"]
  //       .reg RES 32 .input rcurr .output rnext

  //       ${rand0.code}
  //       ${rand1.code}
  //       ${rand2.code}
  //       ${rand3.code}
  //       ${rand4.code}

  //       SRL slice8, ${rand0.randNext}, const0; // slice (7, 0) of rand0.randNext
  //       SRL isOdd, ${rand1.randNext}, const0; // slice(0, 0) of rand0.randNext
  //       XOR isEven, isOdd, false;

  //       SEQ cc1, slice8, one8;
  //       AND c1, isEven, cc1;

  //       SEQ cc2, slice8, two8;
  //       AND c2, isOdd, cc2;

  //       SEQ cc3, slice8, three8;
  //       AND c3, isOdd, cc3;

  //       // This parmux can not be converted to a jump table because its inputs
  //       // are directly used to generate the values of rand#.randCurr (after closing seq cycles)
  //       PARMUX rnext, c1 ? ${rand2.randNext}, c2 ? ${rand3.randNext}, c3 ? ${rand4.randNext}, ${rand0.randNext};

  //   """

  //     def nextRef(): Int = {

  //       val rand0Ref = rand0.nextRef()
  //       val rand1Ref = rand1.nextRef()
  //       val rand2Ref = rand2.nextRef()
  //       val rand3Ref = rand3.nextRef()
  //       val rand4Ref = rand4.nextRef()
  //       val slice8 = 0xff & rand0Ref
  //       val isEven = (0x01 & rand1Ref) == 0
  //       val isOdd = !isEven
  //       if (slice8 == 1 && isEven) {
  //         rand2Ref
  //       } else if (slice8 == 2 && isOdd) {
  //         rand3Ref
  //       } else if (slice8 == 3 && isOdd) {
  //         rand4Ref
  //       } else {
  //         rand0Ref
  //       }
  //     }

  //     val compiler = Commons.frontend(true, true) followedBy Commons.backend

  //     val ctx = Commons.context(fixture)

  //     val parsed = AssemblyParser(source, ctx)
  //     val compiled = compiler(parsed, ctx)._1

  //     val hasNoJumpTable = compiled.processes.head.body.forall(inst =>
  //       !inst.isInstanceOf[PlacedIR.JumpTable]
  //     )

  //     withClue(
  //       "Expected to have no jump tables when a parmux is not wide and its operands are shared"
  //     ) { hasNoJumpTable shouldEqual true }

  //     val monitor = PlacedIRInterpreterMonitor(
  //       compiled
  //     )(ctx)
  //     // println(s"Watching ${monitor.keys}")
  //     fixture.dump(
  //       "human_readable.masm",
  //       PlacedIRDebugSymbolRenamer.makeHumanReadable(compiled)(ctx).serialized
  //     )
  //     val interp = AtomicInterpreter.instance(
  //       program = compiled,
  //       monitor = Some(monitor)
  //     )(ctx)

  //     for (cycle <- 0 to 10000) {
  //       interp.interpretVirtualCycle() shouldBe Nil

  //       val ref = nextRef()
  //       val got = monitor.read("rnext").toInt
  //       withClue("random generator mismatch") {
  //         monitor.read(rand0.randNext).toInt shouldEqual rand0.currRef()
  //         monitor.read(rand1.randNext).toInt shouldEqual rand1.currRef()
  //         monitor.read(rand2.randNext).toInt shouldEqual rand2.currRef()
  //         monitor.read(rand3.randNext).toInt shouldEqual rand3.currRef()
  //         monitor.read(rand4.randNext).toInt shouldEqual rand4.currRef()
  //       }
  //       withClue(s"Result mismatch:  ") { got shouldEqual ref }
  //     }

  // }

  "JumpTableConstruction" should "remove ParMuxes if there are two many cases" in {

    fixture =>
      val numRands = 20
      val randGen = Seq.tabulate(numRands) { i =>
        XorShift128(s"rnd$i", i)
      }

      // The program implements the following pseudo-code:
      // result =
      // if (rand0 is odd) {
      //    rand0 + 6 (case 0)
      // } else if (rand1 is odd) {
      //    rand1
      // } else if (rand2 is odd) {
      //    rand2
      // } ...
      // } else {
      //    (rand0 >> 4) + 6 (case default)
      // }
      // note that this code, although is a prime example of parallel cases
      // can not be converted to an optimal jump table, because the comparison
      // values are different.
      // This small test is supposed to make sure that we can still get a
      // slow jump table out of it. Note that if case 0 and case default were
      // also single expressions "rand0" or any other name used outside of the
      // if-else block, then it would not actually make sense to really implement
      // jump table since we would end up with more instructions.

      val source = s"""

    .prog:
      .proc p0:

        ${randGen.map(_.registers).mkString("\n")}

        @TRACK[name = "res1"]
        .reg RES1 32 .input rcurr1  .output rnext1
        @TRACK[name = "res2"]
        .reg RES2 32 .input rcurr2  .output rnext2

        .wire defaultWire0 32
        .wire defaultWire1 32
        .wire defaultWire2 32
        .wire firstWire0 32
        .wire firstWire1 32

        .const one32 32 1
        .const zero32 32 0

        .const four32 32 4
        .const two32 32 2

        .const true  1 1
        ${Seq.tabulate(numRands) { i => s".wire isOdd$i 1" } mkString ("\n")}
        ${Seq.tabulate(numRands) { i => s".wire notOdd$i 1" } mkString ("\n")}
        ${Seq.tabulate(numRands) { i => s".wire notFound$i 1" } mkString ("\n")}
        ${Seq.tabulate(numRands) { i =>
        s"@TRACK[name = \"cond$i\"] .wire cond$i 1"
      } mkString ("\n")}
        ${randGen.map(_.code).mkString("\n")}

        ${Seq
        .tabulate(numRands) { i =>
          s"SRL isOdd$i, ${randGen(i).randNext}, zero32;"
        }
        .mkString("\n")}
        ${Seq
        .tabulate(numRands) { i =>
          s"XOR notOdd$i, isOdd$i, true;"
        }
        .mkString("\n")}

        MOV cond0, isOdd0;

        MOV notFound0, notOdd0;
        ${Range(1, numRands).map { i =>
        s"AND notFound$i, notOdd$i, notFound${i - 1};"
      } mkString ("\n")}

        ${Range(1, numRands)
        .map { i =>
          s"AND cond$i, isOdd$i, notFound${i - 1};"
        }
        .mkString("\n")}

        SRL defaultWire0, ${randGen.head.randNext}, four32;
        ADD defaultWire1, defaultWire0, two32;
        ADD defaultWire2, defaultWire1, four32;

        ADD firstWire0, ${randGen.head.randNext}, four32;
        ADD firstWire1, firstWire0, two32;
        PARMUX rnext1, cond0 ? firstWire1, ${Range(1, numRands)
        .map { i =>
          s"cond$i ? ${randGen(i).randNext}"
        }
        .mkString(", ")}, defaultWire2;


    """

      fixture.dump(
        "original.masm",
        source
      )

      val compiler = Commons.frontend(true, true) followedBy Commons.backend

      val ctx = Commons.context(fixture)

      val parsed = AssemblyParser(source, ctx)
      val compiled = compiler(parsed, ctx)._1

      val hasNoJumpTable = compiled.processes.head.body.forall(inst =>
        !inst.isInstanceOf[PlacedIR.JumpTable]
      )

      val monitor = PlacedIRInterpreterMonitor(
        compiled
      )(ctx)
      // println(s"Watching ${monitor.keys}")
      fixture.dump(
        "human_readable.masm",
        PlacedIRDebugSymbolRenamer.makeHumanReadable(compiled)(ctx).serialized
      )

      val interp = AtomicInterpreter.instance(
        program = compiled,
        monitor = Some(monitor)
      )(ctx)

      for (cycle <- 0 until 10000) {
        interp.interpretVirtualCycle() shouldBe Nil

        for (i <- 0 until numRands) {
          withClue(s"@$cycle random generator $i mismatch") {
            randGen(i).nextRef()
            monitor.read(randGen(i).randNext).toInt shouldEqual randGen(i)
              .currRef()
          }

        }

        def isOdd(x: Int): Boolean = (0x01 & x) == 1
        withClue(s"@$cycle  result mismatch: ") {
          if (isOdd(randGen(0).currRef())) {
            monitor.read("rnext1").toInt shouldEqual (randGen(0).currRef() + 6)
          } else {
            val res = randGen.tail.foldLeft(Option.empty[Int]) {
              case (None, gen) if isOdd(gen.currRef()) =>
                Some(gen.currRef().toInt)
              case (found, _) => found
            }
            res match {
              case None =>
                val defaultValue = (randGen.head.currRef() >>> 4) + 6
                monitor.read("rnext1").toInt shouldEqual defaultValue
              case Some(value) =>
                monitor.read("rnext1").toInt shouldEqual value
            }
          }
        }

      }

  }
}
