package manticore.compiler.integration.chisel


import manticore.compiler.assembly.BinaryOperator
import manticore.compiler.assembly.levels.UInt16

class SRATester extends SingleInstructionTest {


  val operator = BinaryOperator.SRA

  behavior of "SRA in Manticore Machine"
  it should "correctly handle simple SRA for negative numbers cases" in {f =>
    val op1 = Seq.fill(16) { UInt16(0xffff) }
    val op2 = Seq.tabulate(16) { i => UInt16(i) }
    createTest(op1 = op1, op2 = op2, expected_vcycles = 16, fixture = f)
  }

  it should "correctly handle simple SRA for positive numbers cases" in {f =>
    val op1 = Seq.fill(15) { UInt16(0x7fff) }
    val op2 = Seq.tabulate(15) { i => UInt16(i) }
    createTest(op1 = op1, op2 = op2, expected_vcycles = 16, fixture = f)
  }

  it should "correctly handle random SRA cases" in {f =>

    val randgen = new scala.util.Random(0)

    val op1 = Seq.fill(400) { UInt16(randgen.nextInt(0xffff + 1)) }
    val op2 = Seq.fill(400) { UInt16(randgen.nextInt(0xffff + 1)) }
    createTest(op1 = op1, op2 = op2, expected_vcycles = 400, fixture = f)
  }


}