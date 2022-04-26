package manticore.compiler.assembly.levels.placed

import manticore.compiler.assembly.levels.ConstantFolding
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.assembly.BinaryOperator
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.ConstType

/**
  * Constant folding pass for PlacedIR
  *
  * @author Mahyar Emami <mahyar.emami@epfl.ch>
  */
object PlacedIRConstantFolding
    extends ConstantFolding
    with AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {

  val flavor = PlacedIR
  import BinaryOperator._
  import flavor._

  override val ConstOne: UInt16 = UInt16(1)

  override val ConstZero: UInt16 = UInt16(0)

  private def truncate(v: UInt16): Int = {
    v.toInt & 0xf
  }
  override val binOpEvaluator: PartialFunction[
    (
        BinaryOperator.BinaryOperator,
        Either[Name, UInt16],
        Either[Name, UInt16]
    ),
    Either[Name, UInt16]
  ] = {
    // fully evaluate to const when both operands are constant
    case (ADD, Right(c1), Right(c2)) => Right(c1 + c2)
    case (SUB, Right(c1), Right(c2)) => Right(c1 - c2)
    case (OR, Right(c1), Right(c2))  => Right(c1 | c2)
    case (AND, Right(c1), Right(c2)) => Right(c1 & c2)
    case (XOR, Right(c1), Right(c2)) => Right(c1 ^ c2)
    case (SEQ, Right(c1), Right(c2)) =>
      Right(if (c1 == c2) UInt16(1) else UInt16(0))
    case (SLL, Right(c1), Right(c2)) => Right(c1 << truncate(c2))
    case (SRL, Right(c1), Right(c2)) => Right(c1 >> truncate(c2))
    case (SRA, Right(c1), Right(c2)) => Right(c1 >>> truncate(c2))
    case (SLTS, Right(c1), Right(c2)) =>
      val rs1_sign = (c1 >> 15) == UInt16(1)
      val rs2_sign = (c2 >> 15) == UInt16(1)

      if (rs1_sign && !rs2_sign) {
        // rs1 is negative and rs2 is positive
        Right(UInt16(1))
      } else if (!rs1_sign && rs2_sign) {
        // rs1 is positive and rs2 is negative
        Right(UInt16(0))
      } else if (!rs1_sign && !rs2_sign) {
        // both are positive
        if (c1 < c2) {
          Right(UInt16(1))
        } else {
          Right(UInt16(0))
        }
      } else {
        // both are negative
        val c1_pos =
          (~c1) + UInt16(1) // 2's complement positive number
        val c2_pos =
          (~c2) + UInt16(1) // 2's complement positive number
        if (c1_pos > c2_pos) {
          Right(UInt16(1))
        } else {
          Right(UInt16(0))
        }
      }
    // evaluate if only one of the operands is constant
    case (ADD, Right(ConstZero), Left(n2))      => Left(n2)
    case (ADD, Left(n1), Right(ConstZero))      => Left(n1)
    case (SUB, Left(n1), Right(ConstZero))      => Left(n1)
    case (AND, Left(n1), Right(UInt16(0xffff))) => Left(n1)
    case (AND, Right(UInt16(0xffff)), Left(n2)) => Left(n2)
    case (OR | XOR, Left(n1), Right(ConstZero)) => Left(n1)
    case (OR | XOR, Right(ConstZero), Left(n2)) => Left(n2)

  }

  override def addCarryEvaluator(rs1: UInt16, rs2: UInt16, ci: UInt16)(implicit
      ctx: AssemblyContext
  ): (UInt16, UInt16) = {

    val sum = rs1.toInt + rs2.toInt + ci.toInt
    val rd = UInt16.clipped(sum)
    val co = sum >> 16
    assert(
      co <= 1,
      "something is up in the carry computation in constant folding!"
    )
    (rd, UInt16(co))
  }

  override def freshConst(v: UInt16)(implicit
      ctx: AssemblyContext
  ): DefReg = {
    val name = s"c%${ctx.uniqueNumber()}"
    DefReg(
      ValueVariable(
        name,
        -1,
        ConstType
      ),
      Some(v)
    )
  }

  override def transform(
      source: DefProgram,
      context: AssemblyContext
  ): DefProgram =
    do_transform(source)(context)

}