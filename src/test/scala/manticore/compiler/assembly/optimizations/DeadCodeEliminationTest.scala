package manticore.compiler.assembly.optimizations

import manticore.compiler.UnitFixtureTest
import manticore.compiler.ManticorePasses
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedNameChecker
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedMakeDebugSymbols
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedDeadCodeElimination
import manticore.compiler.assembly.levels.unconstrained.width.WidthConversion
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedCloseSequentialCycles
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedInterpreter
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedBreakSequentialCycles
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRInterpreterMonitor
import manticore.compiler.assembly.levels.InterpreterMonitor
import manticore.compiler.HasLoggerId
import manticore.compiler.UnitTestMatchers
import manticore.compiler.assembly.annotations.DebugSymbol
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR

class DeadCodeEliminationTest extends UnitFixtureTest with UnitTestMatchers{

  behavior of "DeadCodeElimination"

  class XorShift128 {
    val x = Array[Int](
      123456789,
      362436069,
      521288629,
      1650957049
    )
    def next(): Int = {

      var t = x(3)
      val s = x(0)
      x(3) = x(2)
      x(2) = x(1)
      x(1) = s
      t ^= t << 11
      t ^= t >>> 8
      x(0) = t ^ s ^ (s >>> 19)
      x(0)
    }
  }

  "DCE" should "remove any register that does not contribute to tracked values" in {
    fixture =>
      val source = """


        .prog:
            .proc p0:

                @TRACK[name = "X0"]
                .reg X0 32 .input x0curr 123456789 .output x0next
                .reg X1 32 .input x1curr 362436069 .output x1next
                .reg X2 32 .input x2curr 521288629 .output x2next
                .reg X3 32 .input x3curr 1650957049 .output x3next

                .reg DEAD 32 .input deadC .output deadN

                .const eleven 32 11
                .const eight  32 8
                .const nineteen 32 19

                .wire t 32
                .wire s 32
                .wire l1 32
                .wire l2 32
                .wire l3 32
                .wire l4 32
                .wire l5 32
                .wire l6 32
                .wire l7 32
                .wire l8 32
                .wire l9 32
                .wire l10 32
                .wire l11 32



                MOV t, x3curr;
                MOV s, x0curr;

                SLL l1, t, eleven;
                XOR l2, l1, t;
                SRL l3, l2, eight;
                XOR l4, l3, l2;
                SRL l5, s, nineteen;
                XOR l6, s, l5;
                XOR l7, l6, l4;

                MOV x3next, x2curr;
                MOV x2next, x1curr;
                MOV x1next, s;
                MOV x0next, l7;

                ADD deadN, x0curr, x1curr;


          """

      val compiler =
        UnconstrainedNameChecker followedBy
          UnconstrainedMakeDebugSymbols followedBy
          UnconstrainedDeadCodeElimination followedBy
          WidthConversion.transformation followedBy
          UnconstrainedCloseSequentialCycles

      // implicit val ctx = AssemblyContext(
      //   output_dir = Some(fixture.test_dir.resolve("out").toFile()),
      //   dump_all = true,
      //   dump_dir = Some(fixture.test_dir.resolve("dumps").toFile()),
      //   expected_cycles = Some(100),
      //   debug_message = true
      // )
      implicit val ctx = fixture.ctx
      val parsed = AssemblyParser(source, ctx)

      val compiled = compiler(parsed, ctx)._1
      implicit val testId = new HasLoggerId {
        val id: String = "Monitor Callback"
      }
      val rangGen = new XorShift128()

      val monitor = UnconstrainedIRInterpreterMonitor(
        compiled,
        Seq(
         InterpreterMonitor.WatchSymbol("x0curr")
        )
      )
      def hasDebSym(r: UnconstrainedIR.DefReg): Boolean = r.annons.collectFirst {
        case x: DebugSymbol => x
      }.nonEmpty
      val keptSyms = compiled.processes.head.registers.collect { case r if hasDebSym(r) =>
        r.annons.collectFirst {case x: DebugSymbol => x }.get.getSymbol()
      }

      val assumedLive = Seq(
        "x0curr", "x0next",
        "x1curr", "x1next",
        "x2curr", "x2next",
        "x3curr", "x3next"
      )
      // all names assumed to be live should remain live
      assumedLive.forall { keptSyms.contains(_) } shouldBe true

      // dead names should be dead
      keptSyms.contains("DEAD") shouldBe false

      val interp = UnconstrainedInterpreter.instance(compiled, None, Some(monitor))
      val reference = new XorShift128

      Range(0, 1000).foreach { _ =>
        interp.runVirtualCycle()
        monitor.read("x0curr").toInt shouldBe reference.next()
        interp.getException() shouldBe None
      }

  }



}
