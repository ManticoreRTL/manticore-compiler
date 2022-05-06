package manticore.compiler.assembly.optimizations

import manticore.compiler.UnitFixtureTest
import manticore.compiler.assembly.utils.XorShift128
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedNameChecker
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedMakeDebugSymbols
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedRenameVariables
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedOrderInstructions
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRConstantFolding
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.assembly.levels.unconstrained.width.WidthConversion
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRInterpreterMonitor
import manticore.compiler.UnitTestMatchers
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedCloseSequentialCycles
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedInterpreter
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRDebugSymbolRenamer

class ConstantFoldingTest extends UnitFixtureTest with UnitTestMatchers {

  behavior of "ConstantFolding"

  "CF" should "remove redundant MUXes in UnconstrainedIR" in { fixture =>
    val randGen1 = XorShift128("rand1", 1231239)
    val randGen2 = XorShift128("rand2", 78086551)
    val randGen3 = XorShift128("rand3", 92308681)

    val text = s"""
        .prog: .proc p0:
            .const one32 32 1
            .const zero32 32 0

            .const true 1 1
            .const false 1 0
            @TRACK [name = "SHOULD_BE_RAND1"]
            .reg res1 32 .input rcurr1 .output rnext1
            @TRACK [name = "SHOULD_BE_RAND2"]
            .reg res2 32 .input rcurr2 .output rnext2
            @TRACK [name = "SOMETHING_ELSE"]
            .reg res3 32 .input rcurr3 .output rnext3
            .wire w1 32
            .wire w2 32
            .wire w3 32
            .wire cond1 1
            .wire cond2 1



            ${randGen1.registers}
            ${randGen2.registers}
            ${randGen3.registers}

            ${randGen1.code}
            ${randGen2.code}
            ${randGen3.code}


            MUX w1, false, ${randGen1.randNext}, ${randGen2.randNext};
            MUX w2, true, ${randGen1.randNext}, ${randGen2.randNext};

            SRL cond1, ${randGen1.randNext}, zero32;
            MUX cond2, cond1, false, true;
            MUX w3, cond2, ${randGen2.randNext}, ${randGen3.randNext};

            MOV rnext1, w1;
            MOV rnext2, w2;
            MOV rnext3, w3;
        """

    implicit val ctx = fixture.ctx
    def frontend =
      UnconstrainedNameChecker followedBy
        UnconstrainedMakeDebugSymbols followedBy
        UnconstrainedRenameVariables followedBy
        UnconstrainedOrderInstructions followedBy
        UnconstrainedIRConstantFolding followedBy
        UnconstrainedCloseSequentialCycles

    val parsed = AssemblyParser(text, ctx)
    val compiled = frontend(parsed, ctx)._1

    val monitor = UnconstrainedIRInterpreterMonitor(compiled)

    // println(UnconstrainedIRDebugSymbolRenamer.makeHumanReadable(compiled).serialized)

    withClue("Only a single mux should be present after folding:") {
      compiled.processes.head.body.count(inst =>
        inst.isInstanceOf[UnconstrainedIR.Mux]
      ) shouldBe 1
    }

    val interp = UnconstrainedInterpreter.instance(
      program = compiled,
      monitor = Some(monitor),
      vcdDump = None
    )
    for (i <- 0 until 10000) {
      withClue("No exception should occur during execution:") {
        interp.runVirtualCycle() shouldBe None
      }
      withClue("Correct results are expected:") {

        randGen1.nextRef()
        randGen2.nextRef()
        randGen3.nextRef()

        monitor.read("rnext1").toInt shouldBe randGen1.currRef()
        monitor.read("rnext2").toInt shouldBe randGen2.currRef()
        val isOdd = (0x1 & randGen1.currRef()) == 1
        if (isOdd) {
            monitor.read("rnext3").toInt shouldBe randGen3.currRef()
        } else {
            monitor.read("rnext3").toInt shouldBe randGen2.currRef()
        }
      }
    }

  }



}
