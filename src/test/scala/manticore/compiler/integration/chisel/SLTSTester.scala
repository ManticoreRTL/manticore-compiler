package manticore.compiler.integration.chisel

import manticore.compiler.assembly.BinaryOperator
import manticore.compiler.assembly.levels.UInt16

import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.ManticorePasses
import manticore.compiler.assembly.levels.placed.UnconstrainedToPlacedTransform

class SLTSTester extends SingleInstructionTest {
  val operator = BinaryOperator.SLTS

  // can not do width conversion with this instruction because the
  // WidthConversion assume the second operand is constant 0
  override def compiler =
    ManticorePasses.frontend andThen
      UnconstrainedToPlacedTransform andThen
      ManticorePasses.BackendLowerEnd


  behavior of "SLTS in Manticore Machine"
  it should "correctly handle random SLTS cases" in { f =>
    val randgen = new scala.util.Random(0)
    val op1 = Seq.fill(600) { UInt16(randgen.nextInt(0xffff + 1)) }
    val op2 = Seq.fill(600) { UInt16(0) }
    createTest(op1 = op1, op2 = op2, expected_vcycles = 600, fixture = f)
  }

}
