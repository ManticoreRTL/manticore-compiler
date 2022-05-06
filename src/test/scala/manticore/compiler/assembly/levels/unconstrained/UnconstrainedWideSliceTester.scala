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
            PADZERO rand_padded_1, ${randGen1.randNext}, 96;
            PADZERO rand_padded_2, ${randGen2.randNext}, 96;
            PADZERO rand_padded_3, ${randGen3.randNext}, 96;
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
      UnconstrainedCloseSequentialCycles

    val parsed = AssemblyParser(text, ctx)
    val compiled = frontend(parsed, ctx)._1

    fixture.dump(
      s"human.masm",
      UnconstrainedIRDebugSymbolRenamer.makeHumanReadable(compiled).serialized
    )

    withClue("Only the narrow slice in the original program should translate to a lowered slice:") {
      compiled.processes.head.body.count {
        case _: UnconstrainedIR.Slice => true
        case _ => false
      } shouldBe 1
    }

    val monitor = UnconstrainedIRInterpreterMonitor(compiled)
    val interp = UnconstrainedInterpreter.instance(
      program = compiled,
      monitor = Some(monitor),
      vcdDump = None
    )
    for (i <- 0 until 10000) {
      withClue("No exception should occur during execution:") {
        interp.runVirtualCycle() shouldBe None
      }

      // Generate the next random number. Future calls to currRef() will yield
      // the generated number.
      randGen1.nextRef()
      randGen2.nextRef()
      randGen3.nextRef()

      // println(s"randGen1 = ${randGen1.currRef()}")
      // println(s"randGen2 = ${randGen2.currRef()}")
      // println(s"randGen3 = ${randGen3.currRef()}")

      // MUST mask the generated random number as it could be negative. Our
      // machine only supports positive numbers and SLL in the test program above.
      // However, Scala's BigInt library represents large numbers as a negative
      // sign followed by a large number. Shifting such a number left preserves
      // the negative sign. We want a two's complement number and masking the
      // number achieves this.
      val randGen1Value = BigInt(randGen1.currRef()) & ((BigInt(1) << 32) - 1)
      val randGen2Value = BigInt(randGen2.currRef()) & ((BigInt(1) << 32) - 1)
      val randGen3Value = BigInt(randGen3.currRef()) & ((BigInt(1) << 32) - 1)
      val rand = (randGen3Value << 64) | (randGen2Value << 32) | randGen1Value

      withClue("Correct results are expected for the narrow slice contained within a single word:") {
        val narrowExpected = (rand >> 1) & ((BigInt(1) << 5) - 1)
        val narrowReceived = monitor.read("narrowCurr")
        narrowReceived shouldBe narrowExpected
      }

      withClue("Correct results are expected for the wide slice that spans multiple words:") {
        val wideExpected = (rand >> 9) & ((BigInt(1) << 49) - 1)
        val wideReceived = monitor.read("wideCurr")
        wideReceived shouldBe wideExpected
      }
    }

  }
}
