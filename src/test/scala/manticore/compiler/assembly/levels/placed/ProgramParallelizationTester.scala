package manticore.compiler.assembly.levels.placed

import manticore.compiler.UnitFixtureTest
import manticore.compiler.assembly.utils.XorShift128
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedNameChecker
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedMakeDebugSymbols
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedOrderInstructions
import manticore.compiler.assembly.levels.unconstrained.width.WidthConversion
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRConstantFolding
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedDeadCodeElimination
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.placed.interpreter.AtomicInterpreter
import manticore.compiler.assembly.levels.placed.interpreter.PlacedIRInterpreterMonitor
import manticore.compiler.UnitTestMatchers
import manticore.compiler.DefaultHardwareConfig

class ProgramParallelizationTester extends UnitFixtureTest with UnitTestMatchers {

  behavior of "Program parallelization"

  "unused input registers" should "remain in the process that produces the next value" in { fixture =>
    val numStates = 250
    val maxCycles = numStates * 5
    val stateDef = Range(0, numStates)
      .map { index =>
        s".reg r$index 32 .input i$index 0 .output o$index"
      }
      .mkString("\n")
    val randGen = XorShift128("rand")

    val body = new StringBuilder
    Range(0, numStates).foldLeft(randGen.randNext) { case (prev, ix) =>
      body ++= s"MOV o${ix}, ${prev};\n"
      s"i${ix}"
    }

    val text = s"""|.prog: .proc p0:
                          |
                          | $stateDef
                          | ${randGen.registers}
                          | .const true 1 1
                          | .const false 1 0
                          | .const one16 16 1
                          | .const maxCounter 16 $maxCycles
                          | .wire done 1
                          | .reg cnt 16 .input counterI 0 .output counterO;
                          |
                          | ${randGen.code}
                          | ${body}
                          | ADD counterO, counterI, one16;
                          | SEQ done, counterI, maxCounter;
                          | (0) PUT counterI, true;
                          | (1) PUT i${numStates - 1}, true;
                          | (2) FLUSH "(@ %16d) %32d", true;
                          | (3) FINISH done;
                          |
                          |
                          |""".stripMargin
    val compiler = AssemblyParser andThen
      UnconstrainedNameChecker andThen
      UnconstrainedMakeDebugSymbols andThen
      UnconstrainedOrderInstructions andThen
      WidthConversion.transformation andThen
      UnconstrainedIRConstantFolding andThen
      UnconstrainedDeadCodeElimination andThen
      UnconstrainedToPlacedTransform andThen
      ProcessSplittingTransform
    implicit val ctx = AssemblyContext(
      dump_all = true,
      dump_dir = Some(fixture.test_dir.toFile),
      quiet = false,
      hw_config = DefaultHardwareConfig(dimX = 16, dimY = 16),
      log_file = Some(fixture.test_dir.resolve("run.log").toFile())
      // log_file = None
    )

    val compiled = compiler(text)
    val closed   = PlacedIRCloseSequentialCycles(compiled)
    val monitor  = PlacedIRInterpreterMonitor(closed)
    val interp =
      AtomicInterpreter.instance(program = closed, monitor = Some(monitor))
    var finished = false
    val expected = scala.collection.mutable.ArrayBuffer.empty[Int]
    var cycle    = 0
    // println(monitor.keys)
    while (!finished) {
      cycle += 1
      interp.interpretVirtualCycle() match {
        case manticore.compiler.assembly.levels.placed.interpreter.FinishTrap +: Nil =>
          finished = true
        case _ +: _ =>
          ctx.logger.flush()
          finished = true
          fail("failed interpretation!")
        case Nil => // nothing
      }
      expected += randGen.nextRef()
      if (cycle >= numStates) {
        monitor.read(s"o${numStates - 1}").toInt shouldBe expected(
          cycle - numStates
        )
        // println(monitor.read(s"counterI"))
        // println(monitor.read(s"o${numStates - 1}"))
      }

      if (cycle > maxCycles + 200) {
        fail("Timedout!")
      }
    }

  }

}
