package manticore.compiler.assembly.levels.unconstrained

import manticore.compiler.UnitFixtureTest
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.UnitTestMatchers

class SerialFlushTester extends UnitFixtureTest with UnitTestMatchers {

    behavior of "SerialFlush"

    def compile(text: String)(implicit ctx: AssemblyContext) = {
        println(text)
        val parsed = AssemblyParser(text, ctx)
        val compiler = UnconstrainedNameChecker followedBy
          UnconstrainedMakeDebugSymbols
        compiler(parsed, ctx)._1
    }
    "Flush" should "send a formatted message over serial" in { fixture =>


        val text = s"""
            .prog: .proc p0:
                .const v1 32 1231241
                .const v2 14 13
                .const true 1 1
                PUT v1, true;
                PUT v2, true;
                FLUSH "v1 is %032d and v2 is %14b", true;
        """

        implicit val ctx = fixture.ctx
        val program = compile(text)
        val interp = UnconstrainedInterpreter.instance(
            program = program,
            serial = Some(ln =>
                ln shouldBe "v1 is 00000000000000000000000001231241 and v2 is 00000000001101"
            )
        )
        interp.runVirtualCycle()

    }
}