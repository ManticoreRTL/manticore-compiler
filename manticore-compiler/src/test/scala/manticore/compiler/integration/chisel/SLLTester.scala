package manticore.compiler.integration.chisel

import org.scalatest.fixture.UnitFixture
import manticore.compiler.UnitFixtureTest
import chiseltest._
import chisel3._

import manticore.compiler.assembly.BinaryOperator
import manticore.compiler.assembly.levels.UInt16



class SLLTester extends SingleInstructionTest {


  val operator = BinaryOperator.SLL

  behavior of "SLL in Manticore Machine"
  it should "correctly handle simple SLL cases" in {f =>
    val op1 = Seq.fill(16) { UInt16(1) }
    val op2 = Seq.tabulate(16) { i => UInt16(i) }
    createTest(op1 = op1, op2 = op2, expected_vcycles = 17, fixture = f)
  }
  it should "correctly handle random SLL cases" in {f =>

    val randgen = new scala.util.Random(0)

    val op1 = Seq.fill(400) { UInt16(randgen.nextInt(0xffff + 1)) }
    val op2 = Seq.fill(400) { UInt16(randgen.nextInt(0xffff + 1)) }
    createTest(op1 = op1, op2 = op2, expected_vcycles = 401, fixture = f)
  }


}
