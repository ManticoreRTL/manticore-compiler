package manticore.compiler.assembly.levels.placed.lowering

import manticore.compiler.UnitFixtureTest
import manticore.compiler.ManticorePasses
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.assembly.levels.placed.interpreter.AtomicInterpreter
import manticore.compiler.AssemblyContext
import org.scalatest.matchers.should.Matchers
class InterruptLoweringTransformTester extends UnitFixtureTest with Matchers {

  behavior of "interrupt lowering"

  "PutSerial" should "be lowered to GSTs" in { fixture =>
    val compiler =
      AssemblyParser andThen
        ManticorePasses.frontend andThen
        ManticorePasses.middleend andThen
        ManticorePasses.backend

    val testProgram = s"""|.prog: .proc p0:
                          |     .const one 1 1
                          |     .const zero 1 0
                          |     .reg cnt 32 .input cnt_curr 0 .output cnt_next
                          |     .const max_cnt 32 16
                          |     .reg r1 1 .input done_curr 0 .output done_next
                          |     .reg e1 1 .input even_curr 1 .output even_next
                          |     .reg o1 1 .input odd_curr 0 .output odd_next
                          |
                          |     SLICE odd_next, cnt_next, 0, 1;
                          |     XOR   even_next, odd_next, one;
                          |     SEQ done_next, cnt_next, max_cnt;
                          |     (0) FINISH done_curr;
                          |     (1) PUT cnt_curr, odd_curr;
                          |     (2) FLUSH "#ODD#  %32d", odd_curr;
                          |     (3) PUT cnt_curr, even_curr;
                          |     (4) FLUSH "#EVEN# %32d", even_curr;
                          |     ADD cnt_next, cnt_curr, one;
                          |
                          |""".stripMargin

    implicit val ctx = AssemblyContext(dump_all = true, dump_dir = Some(fixture.test_dir.toFile))

    val program = compiler(testProgram)
    val serialOut = new StringBuilder
    val interp = AtomicInterpreter.instance(
      program = program,
      serial = Some(ln => serialOut ++= s"${ln}\n")
    )

    interp.interpretCompletion()

    val reference =
        """|#EVEN#          0
           |#ODD#           1
           |#EVEN#          2
           |#ODD#           3
           |#EVEN#          4
           |#ODD#           5
           |#EVEN#          6
           |#ODD#           7
           |#EVEN#          8
           |#ODD#           9
           |#EVEN#         10
           |#ODD#          11
           |#EVEN#         12
           |#ODD#          13
           |#EVEN#         14
           |#ODD#          15
           |""".stripMargin
    val result = serialOut.toString()
    println(result)
    result shouldEqual reference

  }

}
