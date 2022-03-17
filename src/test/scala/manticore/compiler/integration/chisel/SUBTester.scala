package manticore.compiler.integration.chisel


import manticore.compiler.assembly.BinaryOperator
import manticore.compiler.assembly.levels.UInt16

class SUBTester extends SingleInstructionTest {
  val operator = BinaryOperator.SUB
  behavior of "SUB in Manticore Machine"
  it should "correctly handle random SUB cases" in {f =>
    val randgen = new scala.util.Random(0)
    val op1 = Seq.fill(600) { UInt16(randgen.nextInt(0xffff + 1)) }
    val op2 = Seq.fill(600) { UInt16(randgen.nextInt(0xffff + 1)) }
    createTest(op1 = op1, op2 = op2, expected_vcycles = 600, fixture = f)
  }

}
