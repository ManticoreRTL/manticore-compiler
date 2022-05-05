package manticore.compiler.assembly.levels.unconstrained

import java.nio.file.Path
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.utils.XorShift128
import manticore.compiler.UnitFixtureTest
import manticore.compiler.UnitTestMatchers
import manticore.compiler.ManticorePasses
import manticore.compiler.assembly.levels.placed.UnconstrainedToPlacedTransform
import manticore.compiler.assembly.levels.placed.interpreter.PlacedIRInterpreterMonitor
import manticore.compiler.assembly.levels.placed.PlacedIRDebugSymbolRenamer
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.assembly.levels.placed.interpreter.AtomicInterpreter
import manticore.compiler.assembly.levels.placed.PlacedIRCloseSequentialCycles

class UnconstrainedWideSliceTester extends UnitFixtureTest with UnitTestMatchers {

  behavior of "wide SLICE"

  "SLICE" should "select bits in a multi-word slice correctly" in { fixture =>
    val randGen1 = XorShift128("rand1", 1)
    val randGen2 = XorShift128("rand2", 2)
    val randGen3 = XorShift128("rand3", 3)

    val text = s"""
        .prog: .proc p0:
            @TRACK [name = "SHOULD_BE_RAND1"]
            .reg res1 5 .input narrowCurr .output narrowNext
            @TRACK [name = "SHOULD_BE_RAND2"]
            .reg res2 49 .input wideCurr .output wideNext

            .const c_thirty_two 16 32
            .const c_sixty_four 16 64

            .wire rand_padded_1 96
            .wire rand_padded_2 96
            .wire rand_padded_3 96
            .wire rand_padded_2_shifted 96
            .wire rand_padded_3_shifted 96
            .wire rand_padded_23 96
            .wire rand 96

            .wire narrow 5
            .wire wide 49

            ${randGen1.registers}
            ${randGen2.registers}
            ${randGen3.registers}

            ${randGen1.code}
            ${randGen2.code}
            ${randGen3.code}

            // Concatenate the 3 32-bit random numbers together to form a wide number.
            PADZERO rand_padded_1, ${randGen1.randCurr}, 96;
            PADZERO rand_padded_2, ${randGen2.randCurr}, 96;
            PADZERO rand_padded_3, ${randGen3.randCurr}, 96;
            SLL rand_padded_2_shifted, rand_padded_2, c_thirty_two;
            SLL rand_padded_3_shifted, rand_padded_3, c_sixty_four;
            OR rand_padded_23, rand_padded_2_shifted, rand_padded_3_shifted;
            OR rand, rand_padded_23, rand_padded_1;

            SLICE narrow, rand, 1, 5;
            SLICE wide, rand, 9, 49;

            MOV narrowNext, narrow;
            MOV wideNext, wide;
        """

    implicit val ctx = AssemblyContext(
      dump_all = true,
      debug_message = true,
      dump_dir = Some(fixture.test_dir.toFile()),
      log_file = Some(fixture.test_dir.resolve("output.log").toFile()),
    )
    def frontend =
      ManticorePasses.frontend followedBy
      ManticorePasses.middleend followedBy
      UnconstrainedToPlacedTransform followedBy
      PlacedIRCloseSequentialCycles

    val parsed = AssemblyParser(text, ctx)
    val compiled = frontend(parsed, ctx)._1

    fixture.dump(
      s"human.masm",
      PlacedIRDebugSymbolRenamer.makeHumanReadable(compiled).serialized
      )

    withClue("Only the narrow slice in the original program should translate to a lowered slice:") {
      compiled.processes.head.body.count {
        case _: PlacedIR.Slice => true
        case _ => false
      } shouldBe 1
    }

    val monitor = PlacedIRInterpreterMonitor(compiled)
    val interp = AtomicInterpreter.instance(
      program = compiled,
      monitor = Some(monitor),
    )
    for (i <- 0 until 10000) {
      withClue("No exception should occur during execution:") {
        interp.interpretVirtualCycle() shouldBe Seq.empty
      }
      withClue("Correct results are expected:") {

        val randGen1Value = BigInt(randGen1.currRef())
        val randGen2Value = BigInt(randGen2.currRef())
        val randGen3Value = BigInt(randGen3.currRef())
        val rand = (randGen3Value << 64) | (randGen2Value << 32) | randGen1Value

        val narrowExpected = (rand >> 1) & BigInt("1f", 16)
        val wideExpected = (rand >> 9) & BigInt("1ffffffffffff", 16)

        val narrowReceived = monitor.read("narrowCurr")
        val wideReceived = monitor.read("wideCurr")

        narrowReceived shouldBe narrowExpected
        wideReceived shouldBe wideExpected

        randGen1.nextRef()
        randGen2.nextRef()
        randGen3.nextRef()
      }
    }

  }
}
