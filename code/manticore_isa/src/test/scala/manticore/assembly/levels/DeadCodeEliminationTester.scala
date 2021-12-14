package manticore.assembly.levels

import manticore.UnitTest

import manticore.compiler.AssemblyContext
import manticore.assembly.parser.AssemblyParser
import manticore.assembly.levels.unconstrained.UnconstrainedIR
import manticore.assembly.levels.unconstrained.UnconstrainedDeadCodeElimination

class DeadCodeEliminationTester extends UnitTest {

  import manticore.assembly.levels.unconstrained.UnconstrainedIR._

  behavior of "dead code elimination transform"

  it should "correctly remove dead code" in {
    val inputProgram = """
      @PROGRAM [name = "test"]
      .prog:
        .proc p0:
          .const c_0 4 0
          .const c_1 4 0
          .const c_2 4 2
          .const c_3 4 2
          .input a 4
          .input b 4
          .input c 4
          .output d 4
          .wire tmp1 4
          .wire tmp2 4
          .wire tmp3 4
          .wire tmp4 4
          ADD tmp1, a, b;
          ADD tmp2, tmp1, c_0;
          SUB tmp3, tmp1, c_2;
          XOR tmp4, tmp3, c_2;
          ADD d, tmp1, c_0;
      """

    val outputProgram = """
      @PROGRAM [name = "test"]
      .prog:
        .proc p0:
          .const c_0 4 0
          .input a 4
          .input b 4
          .output d 4
          .wire tmp1 4
          ADD tmp1, a, b;
          ADD d, tmp1, c_0;
      """

    val ctx = AssemblyContext()
    val got = UnconstrainedDeadCodeElimination(AssemblyParser(inputProgram, ctx), ctx)._1
    val expected = AssemblyParser(outputProgram, ctx)

    // // Debug
    // println(got.serialized)
    // println(expected.serialized)

    got shouldBe expected
  }

}
