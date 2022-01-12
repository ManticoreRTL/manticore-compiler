package manticore.assembly.levels

import manticore.UnitTest

import manticore.compiler.AssemblyContext
import manticore.assembly.parser.AssemblyParser
import manticore.assembly.levels.unconstrained.UnconstrainedIR
import manticore.assembly.levels.unconstrained.UnconstrainedOrderInstructions

class OrderInstructionsTester extends UnitTest {

  import manticore.assembly.levels.unconstrained.UnconstrainedIR._

  behavior of "instruction ordering transform"

  it should "correctly order instructions in topological order" in {
    val inputProgram = """
      @PROGRAM [name = "test"]
      .prog:
        .proc p0:
          .input a 4
          .input c 4
          .input b 4
          .const c_0 4 0
          .wire tmp1 4
          .output d 4
          .wire tmp2 4
          ADD tmp2, tmp1, c;
          ADD d, tmp2, c_0;
          ADD tmp1, a, b;
      """

    val outputProgram = """
      @PROGRAM [name = "test"]
      .prog:
        .proc p0:
          .const c_0 4 0
          .input a 4
          .input b 4
          .input c 4
          .output d 4
          .wire tmp1 4
          .wire tmp2 4
          ADD tmp1, a, b;
          ADD tmp2, tmp1, c;
          ADD d, tmp2, c_0;
      """

    val ctx = AssemblyContext()
    val got = UnconstrainedOrderInstructions(AssemblyParser(inputProgram, ctx), ctx)._1
    val expected = AssemblyParser(outputProgram, ctx)

    // // Debug
    // println(got.serialized)
    // println(expected.serialized)

    got shouldBe expected
  }

}
