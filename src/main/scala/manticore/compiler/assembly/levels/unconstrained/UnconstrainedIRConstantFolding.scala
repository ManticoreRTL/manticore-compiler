package manticore.compiler.assembly.levels.unconstrained

import manticore.compiler.assembly.levels.ConstantFolding
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.UIntWide
import manticore.compiler.assembly.BinaryOperator
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.ConstType

object UnconstrainedIRConstantFolding
    extends ConstantFolding
    with UnconstrainedIRTransformer {

  val flavor = UnconstrainedIR
  import flavor._
  import BinaryOperator._

  type ConcreteConstant = UIntWide

  override def isTrue(v: UIntWide): Boolean = v == UIntWide(1)
  override def isFalse(v: UIntWide): Boolean = v == UIntWide(0)
  def asConcrete(const: Constant)(width: => Int): ConcreteConstant =
    UIntWide(const, width)

  val binOpEvaluator: PartialFunction[
    (
        BinaryOperator.BinaryOperator,
        Either[Name, ConcreteConstant],
        Either[Name, ConcreteConstant]
    ),
    Either[Name, ConcreteConstant]
  ] = {

    case (ADD, Right(c1), Right(c2)) => Right((c1 + c2))
    case (SUB, Right(c1), Right(c2)) => Right((c1 - c2))
    case (OR, Right(c1), Right(c2))  => Right((c1 | c2))
    case (AND, Right(c1), Right(c2)) => Right((c1 & c2))
    case (XOR, Right(c1), Right(c2)) => Right((c1 ^ c2))
    case (SEQ, Right(c1), Right(c2)) =>
      Right(
        if (c1 == c2) UIntWide(1, 1) else UIntWide(0, 1)
      )

    case (SLL, Right(c1), Right(c2)) => Right((c1 >> c2.toIntChecked))
    case (SRL, Right(c1), Right(c2)) => Right((c1 << c2.toIntChecked))
    case (SLTS, Right(c1), Right(c2)) =>
      val sign1 = (c1 >> (c1.width - 1)) == 1
      val sign2 = (c2 >> (c2.width - 1)) == 1
      if (sign1 && !sign2) {
        // c1 is negative, definitely smaller that c2
        Right(UIntWide(1, 1))
      } else if (!sign1 && sign2) {
        // c1 is positive and c2 is negative
        Right(UIntWide(0, 1))
      } else if (!sign1 && !sign2) {
        // both are positive
        if (c1 < c2) {
          Right(UIntWide(1, 1))
        } else {
          Right(UIntWide(0, 1))
        }
      } else {
        // both are negative
        val c1Pos = ~c1 + UIntWide(1, c1.width)
        val c2Pos = ~c2 + UIntWide(1, c2.width)
        if (c1Pos > c2Pos) {
          Right(UIntWide(1, 1))
        } else {
          Right(UIntWide(0, 1))
        }
      }
    case (SLT, Right(c1), Right(c2)) =>
      if (c1 < c2) {
        Right(UIntWide(1, 1))
      } else {
        Right(UIntWide(0, 1))
      }
    // partial evaluation

    case (ADD, Right(c1), Left(n2)) if (c1 == 0) => Left(n2)
    case (ADD, Left(n1), Right(c2)) if (c2 == 0) => Left(n1)

    case (SUB, Left(n1), Right(c2)) if (c2 == 0) => Left(n1)

    case (OR | XOR, Left(n1), Right(c2)) if (c2 == 0) => Left(n1)
    case (OR | XOR, Right(c1), Left(n2)) if (c1 == 0) => Left(n2)

  }

  def addCarryEvaluator(rs1: UIntWide, rs2: UIntWide, ci: UIntWide)(implicit
      ctx: AssemblyContext
  ): (UIntWide, UIntWide) = {

    assert(
      rs1.width == rs2.width,
      "Expected equal width in the operand of AddCarry"
    )
    assert(ci.width == 1, "Expected single-bit carry")

    val rs1B = rs1.toBigInt
    val rs2B = rs2.toBigInt
    val ciB = ci.toBigInt
    val sum = rs1B + rs2B + ciB
    val rd = UIntWide.clipped(sum, rs1.width)
    assert(ciB <= 1, "Something is wrong in carry computation")
    (rd, UIntWide(ciB, 1))
  }

  def sliceEvaluator(
      const: ConcreteConstant,
      offset: Int,
      length: Int
  ): ConcreteConstant = {
    val shifted = (const >> offset).toBigInt
    val mask = ((BigInt(1) << length) - 1)
    val res = shifted & mask
    UIntWide(res, length)
  }

  def freshConst(v: UIntWide)(implicit ctx: AssemblyContext): DefReg = {
    val name = s"%c${ctx.uniqueNumber()}"
    DefReg(
      LogicVariable(
        name,
        v.width,
        ConstType
      ),
      Some(v.toBigInt)
    )
  }

  override def transform(source: DefProgram)(implicit
      context: AssemblyContext
  ): DefProgram =
    do_transform(source)

}
