package manticore.compiler.assembly.levels.unconstrained

import org.scalatest.fixture.UnitFixture
import manticore.compiler.UnitFixtureTest
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.unconstrained.width.WidthConversionCore
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.UnitTestMatchers

class UnconstrainedWideSerialFlushTester
    extends UnitFixtureTest
    with UnitTestMatchers {

  behavior of "wide flush"

  def run(text: String)(implicit ctx: AssemblyContext) = {
    val compiler = AssemblyParser andThen WidthConversionCore
    val prog = compiler(text)
    val result = new StringBuilder
    val interp = UnconstrainedInterpreter.instance(
      program = prog,
      serial = Some(ln => result ++= s"${ln}\n")
    )
    interp.runVirtualCycle()
    result.toString()
  }

  "wide flush" should "be able to reconstruct formatting after width conversion" in {
    fixture =>
      implicit val ctx = fixture.ctx
      val case1 = s"""|
                        |.prog: .proc p0:
                        |   .const narrows 44 17592186044116
                        |   .const widens  35 34359738318
                        |   .const noChange 12 3896
                        |   .const someBinNum 17 67000
                        |   .const anotherOne 80 1208925819614629174705176
                        |
                        |   .const true 1 1
                        |
                        |   (0) PUT narrows, true;
                        |   (1) PUT widens, true;
                        |   (2) PUT noChange, true;
                        |   (4) PUT someBinNum, true;
                        |   (20) FLUSH "c1: %038d c2: %060d c3: %12d c4: %18b", true;
                        |   (23) PUT anotherOne, true;
                        |   (50) FLUSH "%80b and finish", true;
                        |   (100) FINISH true;
                        |""".stripMargin
      val expected = """|c1: 592186044116 c2: 0000000034359738318 c3: 3896 c4: 010000010110111000
                        |11111111111111111111111111111111111111111111111111111111111111111111110000011000 and finish
                        |""".stripMargin
      val res1 = run(case1)
    //   println(res1)
      res1 shouldEqual expected

  }

}
