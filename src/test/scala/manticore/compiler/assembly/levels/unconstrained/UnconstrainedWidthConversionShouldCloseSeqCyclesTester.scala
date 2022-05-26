package manticore.compiler.assembly.levels.unconstrained

import manticore.compiler.UnitFixtureTest
import manticore.compiler.UnitTestMatchers
import manticore.compiler.assembly.levels.unconstrained.width.WidthConversion
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.assembly.annotations.DebugSymbol

class UnconstrainedWidthConversionShouldCloseSeqCyclesTester
    extends UnitFixtureTest
    with UnitTestMatchers {

  behavior of "WidthConversion"

  // This test is more of a documentation

  val text = """
        .prog: .proc p0:
        @TRACK[name = "dut.cycleCounterCopy"]
        .reg cycleCounterCopy 32 .input currC 0 .output nextC
        .reg cycleCounter 32 .input curr 0 .output next
        .const one 32 1

        ADD next, curr, one;
        MOV nextC, next;


    """

  val compiler =
    UnconstrainedNameChecker andThen
      UnconstrainedMakeDebugSymbols andThen
      WidthConversion.transformation andThen
      UnconstrainedCloseSequentialCycles

  "WidthConversion" should "take a program with its sequential cycles " +
    "closed, otherwise it may remove live registers" in { fixture =>
      val parsed = AssemblyParser(text)(fixture.ctx)
      val compiled = compiler(parsed)(fixture.ctx)
      // we have to make sure the signal marked as tracked is still left in the compiler
      def hasDebSym(r: UnconstrainedIR.DefReg): Boolean =
        r.annons.collectFirst { case x: DebugSymbol =>
          x
        }.nonEmpty
      val keptSyms = compiled.processes.head.registers.collect {
        case r if hasDebSym(r) =>
          r.annons.collectFirst { case x: DebugSymbol => x }.get.getSymbol()
      }

      Seq(
        "currC",
        "nextC",
        "curr",
        "next"
      ).forall { keptSyms.contains(_) } shouldBe true
    }

}
