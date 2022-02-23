package manticore.compiler.assembly.levels.unconstrained.width

import manticore.compiler.AssemblyContext

import manticore.compiler.assembly.BinaryOperator
import manticore.compiler.assembly.levels.ConstType
import manticore.compiler.assembly.levels.WireType
import manticore.compiler.assembly.annotations.DebugSymbol
import manticore.compiler.assembly.annotations.Memblock
import manticore.compiler.assembly.annotations.{Reg => RegAnnotation}
import manticore.compiler.assembly.annotations.AssemblyAnnotation
import manticore.compiler.assembly.annotations.AssemblyAnnotationFields
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.assembly.levels.MemoryType

/** Translates arbitrary width operations to 16-bit ones that match the machine
  * data width
  *
  * [[SRA, SRL, SLL]]: The trickiest ones are the shift operations, pseudo-code
  * for the shift translations is given below:
  * {{{
  * object ShiftTranslation extends App {
  *   case class ArbitraryInt(val vs: Seq[UInt16], val w: Int) {
  *   def toBinaryString: String = {
  *     f"${w}'b" + vs.reverseIterator
  *       .map { x =>
  *         String.format("%16s", x.toInt.toBinaryString).replace(" ", "0")
  *       }
  *       .mkString(" ")
  *   }
  *   override def toString = toBinaryString
  *   }
  *
  *   def shiftRightArithmetic(i: ArbitraryInt, sh: Int): ArbitraryInt = {
  *
  *     val rd = scala.collection.mutable.ArrayBuffer[UInt16](i.vs:_*)
  *
  *     var mutable_sh = sh
  *     val msbits = i.w % 16
  *     val sign = if (msbits != 0) (rd.last >> (msbits - 1)) else (rd.last >> 15)
  *     // first sign extend the most significant short word
  *     if (i.w % 16 != 0) {
  *       val ext_mask = if (sign == UInt16(1)) (UInt16((1 << 16) - 1) << msbits) else UInt16(0)
  *       rd(rd.length - 1) = rd(rd.length - 1) | ext_mask
  *     }
  *     for(ix <- (i.vs.length - 1) to 0 by -1) {
  *       val left_shift_amount = if (mutable_sh == 0) 0 else (16 - mutable_sh)
  *
  *       // we only apply SRA to the last element, the rest are handled by
  *       // propagating the carry
  *       var carry_in = UInt16(0)
  *       val sign_replicated = if (sign.toInt == 1) UInt16( (1 << 16) - 1) else UInt16(0)
  *
  *       {
  *         val last_jx = i.vs.length - 1
  *         val carry_out =
  *           if (mutable_sh >= 16)
  *             rd.last
  *           else
  *             rd.last << left_shift_amount
  *         val local_res =
  *           if (mutable_sh >= 16)
  *             sign_replicated
  *           else
  *             rd(last_jx) >>> mutable_sh
  *         rd(last_jx) = if (mutable_sh == 0) rd(last_jx) else local_res
  *         carry_in = carry_out
  *       }
  *
  *       for (jx <- (i.vs.length - 2) to 0 by -1) {
  *         val carry_out =
  *           if (mutable_sh >= 16)
  *             rd(jx)
  *           else
  *             rd(jx) << left_shift_amount
  *         val local_res =
  *           if (mutable_sh >= 16)
  *             UInt16(0)
  *           else
  *             rd(jx) >> mutable_sh
  *         val new_res = local_res | carry_in
  *         rd(jx) = if (mutable_sh == 0) rd(jx) else new_res
  *         carry_in = carry_out
  *       }
  *
  *       mutable_sh = if (mutable_sh >= 16) mutable_sh - 16 else 0
  *     }
  *
  *     i.copy(vs = rd.toSeq)
  *   }
  *   def shiftRightLogical(i: ArbitraryInt, sh: Int): ArbitraryInt = {
  *     require(sh < Short.MaxValue)
  *
  *     val rd = scala.collection.mutable.ArrayBuffer[UInt16](i.vs:_*)
  *
  *     var mutable_sh = sh
  *     for(ix <- (i.vs.length - 1) to 0 by -1) {
  *       val msh_eq_0 = mutable_sh == 0
  *       var carry_in = UInt16(0)
  *       val left_shift_amount = if (mutable_sh == 0) 0 else (16 - mutable_sh)
  *       for (jx <- (i.vs.length - 1) to 0 by -1) {
  *
  *         val carry_out =
  *           if (mutable_sh >= 16)
  *             rd(jx)
  *           else
  *             rd(jx) << left_shift_amount
  *         val local_res =
  *           if (mutable_sh >= 16)
  *             UInt16(0)
  *           else
  *             rd(jx) >> mutable_sh
  *         val new_res = local_res | carry_in
  *         rd(jx) = if (mutable_sh == 0) rd(jx) else new_res
  *         carry_in = carry_out
  *
  *       }
  *       mutable_sh = if (mutable_sh >= 16) mutable_sh - 16 else 0
  *     }
  *
  *     i.copy(vs = rd.toSeq)
  *   }
  *   def shiftLeftLogical(i: ArbitraryInt, sh: Int): ArbitraryInt = {
  *
  *     require(sh < Short.MaxValue)
  *
  *     var msh = sh
  *
  *     val rd = scala.collection.mutable.ArrayBuffer[UInt16](
  *       i.vs: _*
  *     )
  *
  *     for (ix <- 0 until i.vs.length) {
  *       val msh_eq_0 = msh == 0
  *       val sixteen_minus_msh = 16 - msh
  *       val fifteen_minus_msh = sixteen_minus_msh - 1
  *       val msh_gt_eq_16 = fifteen_minus_msh < 0
  *       val right_shift_amount = if (msh_eq_0) 0 else sixteen_minus_msh
  *       var carry_in = UInt16(0)
  *       val msh_minus_16 = msh - 16
  *       for (jx <- 0 until i.vs.length) {
  *
  *         val carry_out =
  *           if (msh_gt_eq_16) // ms > 16
  *             rd(jx)
  *           else
  *             rd(jx) >> right_shift_amount
  *
  *         val new_res =
  *           if (msh_gt_eq_16) // msh > 16
  *             UInt16(0)
  *           else
  *             rd(jx) << msh //
  *         val new_new_res = new_res | carry_in
  *         rd(jx) = if (msh_eq_0) rd(jx) else new_new_res
  *         carry_in = carry_out
  *       }
  *
  *       msh = if (msh_gt_eq_16) msh_minus_16 else 0
  *
  *     }
  *
  *     i.copy(vs = rd.toSeq)
  *   }
  *
  *   val x = ArbitraryInt(
  *     Seq(
  *       UInt16(1),
  *       UInt16(2),
  *       UInt16(4),
  *       UInt16(8)
  *     ),
  *     53
  *   )
  *
  *   // println(x.toBinaryString)
  *   for (i <- 0 until 70)
  *     println(f"${i}:\t${shiftRightArithmetic(x, i).toBinaryString}")
  *
  * }
  * }}}
  *
  * [[ADD]]: Addition is pretty straightforward. We use a cascade of ADDC
  * instructions.
  *
  * [[SUB]]: We replace all the wide subtraction with additions by inverting the
  * second operand and feeding in a carry of 1 in the first additions.
  *
  * [[SEQ]]: We break every SEQ to many SEQ instructions, and then ADD all the
  * results, and set the rd to be [[SEQ rd, res_sum, num_SEQ]], where [[rd]] is
  * the result of the wide instruction and [[res_sum]] is the sum of all 16-bit
  * [[SEQ]] instruction, and [[num_SEQ]] is the number of 16-bit [[SEQ]]s
  *
  * [[SLTS]]: We only have to deal with [[SLTS rd, rs, const_0]]. This can be
  * efficiently translated to a sign bit check operation. All we need to do is
  * to shift right the most significant word and do a SEQ on the sign bit!
  *
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */

object WidthConversionCore
    extends ConversionBuilder
    with AssemblyTransformer[
      UnconstrainedIR.DefProgram,
      UnconstrainedIR.DefProgram
    ] {

  import flavor._

  val MaxLocalAddressBits = 11
  private def assert16Bit(
      uint16_array: Seq[Name],
      inst: Instruction
  )(msg: => String)(implicit ctx: AssemblyContext): Unit = if (
    uint16_array.length != 1
  ) {
    ctx.logger.error(msg, inst)
  }

  /** Convert binary arithmetic operations
    *
    * TODO: handle constant cases (remove unnecessary masking) [x] SLL [ ] SRA [
    * ] SRL [ ] ADD [ ] SUB [ ] AND OR XOR [ ] PMUX [ ] SLTS [ ] SEQ
    * @param instruction
    * @param ctx
    * @param builder
    * @return
    */
  def convertBinaryArithmetic(
      instruction: BinaryArithmetic
  )(implicit
      ctx: AssemblyContext,
      builder: Builder
  ): Seq[Instruction] = {

    val inst_q = scala.collection.mutable.Queue.empty[Instruction]

    def convertBitwise(bitwise_inst: BinaryArithmetic): Unit = {

      val ConvertedWire(rd_uint16_array, mask) =
        builder.getConversion(bitwise_inst.rd)
      val rs1_uint16_array = builder.getConversion(bitwise_inst.rs1).parts
      val rs2_uint16_array = builder.getConversion(bitwise_inst.rs2).parts

      rd_uint16_array zip (rs1_uint16_array zip rs2_uint16_array) foreach {
        case (rd_16, (rs1_16, rs2_16)) =>
          inst_q += BinaryArithmetic(
            bitwise_inst.operator,
            rd_16,
            rs1_16,
            rs2_16,
            bitwise_inst.annons
          )
      }

    }
    def maskRd(
        rd_mutable: Name,
        rd_mask: Option[Name],
        orig: BinaryArithmetic
    ): Seq[Instruction] = {

      rd_mask match {
        case Some(const_mask) =>
          Seq(
            orig.copy(
              operator = BinaryOperator.AND,
              rd = rd_mutable,
              rs1 = rd_mutable,
              rs2 = const_mask
            )
          )
        case _ =>
          Seq()
        // do nothing
      }
    }

    def moveRegs(
        dest: Seq[Name],
        source: Seq[Name],
        orig: BinaryArithmetic
    ): Seq[Instruction] = {

      require(
        dest.length == source.length,
        "Can not move unaligned register arrays, this is an internal error"
      )

      dest zip source map { case (d, s) =>
        Mov(
          rd = d,
          rs = s,
          annons = orig.annons
        )
      }
    }

    def assertAligned(inst: BinaryArithmetic): Unit = {

      val rd_w = builder.originalWidth(inst.rd)
      val rs1_w = builder.originalWidth(inst.rs1)
      val rs2_w = builder.originalWidth(inst.rs2)
      if (rs1_w != rs2_w) {
        ctx.logger.error(
          s"Operands width ${rs1_w} and ${rs2_w} are not aligned!",
          inst
        )
      }
      if (rs1_w != rd_w || rs2_w != rd_w) {
        ctx.logger.error(s"Result register is not aligned with operands", inst)
      }
    }

    instruction.operator match {
      case BinaryOperator.ADD =>
        // TODO: Handle cases when either of the operands are constant
        val ConvertedWire(rd_uint16_array, rd_mask) =
          builder.getConversion(instruction.rd)
        val rs1_uint16_array: Seq[Name] =
          builder.getConversion(instruction.rs1).parts
        val rs2_uint16_array: Seq[Name] =
          builder.getConversion(instruction.rs2).parts

        val rd_width = builder.originalWidth(instruction.rd)
        val rs1_width = builder.originalWidth(instruction.rs1)
        val rs2_width = builder.originalWidth(instruction.rs2)

        val result_size = rs1_width max rs2_width

        if (result_size != rd_width) {
          ctx.logger.error(
            "Unaligned ADD, ensure that the width of the result aligns with the wider operand",
            instruction
          )
        }

        val rs_array_length =
          rs1_uint16_array.length max rs2_uint16_array.length
        val rs1_uint16_array_aligned = rs1_uint16_array ++ Seq.fill(
          rs_array_length - rs1_uint16_array.length
        ) { builder.mkConstant(0) }
        val rs2_uint16_array_aligned = rs2_uint16_array ++ Seq.fill(
          rs_array_length - rs2_uint16_array.length
        ) { builder.mkConstant(0) }

        // val rs1_uint16_array_aligned = rs_1
        val rd_uint16_array_mutable = rd_uint16_array.map { x =>
          builder.mkWire(x, 16)
        }

        assert(
          rd_uint16_array.length == rs2_uint16_array_aligned.length &&
            rd_uint16_array.length == rs2_uint16_array_aligned.length
        )
        if (rs1_uint16_array_aligned.length != 1) {

          // set the carry-in to be zero for the first partial sum
          (rs1_uint16_array_aligned zip
            rs2_uint16_array_aligned zip
            rd_uint16_array_mutable).foldLeft(builder.mkCarry0()) {
            case (cin, ((rs1_16, rs2_16), rd_16)) =>
              val cout = builder.mkCarry()
              inst_q += AddC(
                rd = rd_16,
                co = cout,
                rs1 = rs1_16,
                rs2 = rs2_16,
                ci = cin
              )
              cout
          }

        } else {
          inst_q += instruction
            .copy(
              rd = rd_uint16_array_mutable.head,
              rs1 = rs1_uint16_array_aligned.head,
              rs2 = rs2_uint16_array_aligned.head
            )

        }

        // if the ADD has one MemoryType operand, it should not be masked
        // otherwise, when the memory is allocated (i.e., initial value is
        // set) we may end up truncating addresses.
        if (
          builder.originalDef(instruction.rs1).variable.varType != MemoryType
        ) {

          inst_q ++= maskRd(rd_uint16_array_mutable.last, rd_mask, instruction)
        } else {
          // the first operand is a memory base pointer!
          if (rs1_uint16_array.length > 1) {
            // ONLY TEMPORARY, NEED TO IMPLEMENT!
            ctx.logger.error(s"Global memory not implemented yet!", instruction)
          }
        }
        if (
          builder.originalDef(instruction.rs2).variable.varType == MemoryType
        ) {
          ctx.logger.error(
            s"Can not have memory variable as the second operand!",
            instruction
          )
        }

        // val moves =
        inst_q ++= moveRegs(
          rd_uint16_array,
          rd_uint16_array_mutable,
          instruction
        )

      case BinaryOperator.SUB =>
        val ConvertedWire(rd_uint16_array, rd_mask) =
          builder.getConversion(instruction.rd)
        val rs1_uint16_array = builder.getConversion(instruction.rs1).parts
        val rs2_uint16_array = builder.getConversion(instruction.rs2).parts
        // ensure that both operands and the results are has the same width

        val rd_uint16_array_mutable = rd_uint16_array map { n =>
          builder.mkWire(n + "_mutable", 16)
        }
        val rd_width = builder.originalWidth(instruction.rd)
        val rs1_width = builder.originalWidth(instruction.rs1)
        val rs2_width = builder.originalWidth(instruction.rs2)

        val result_size = rs1_width max rs2_width

        if (result_size != rd_width) {
          ctx.logger.error(
            "Unaligned SUB, ensure that the width of the result aligns with the wider operand",
            instruction
          )
        }

        val rs_array_length =
          rs1_uint16_array.length max rs2_uint16_array.length
        val rs1_uint16_array_aligned = rs1_uint16_array ++ Seq.fill(
          rs_array_length - rs1_uint16_array.length
        ) { builder.mkConstant(0) }
        val rs2_uint16_array_aligned = rs2_uint16_array ++ Seq.fill(
          rs_array_length - rs2_uint16_array.length
        ) { builder.mkConstant(0) }

        assert(
          rd_uint16_array.length == rs2_uint16_array_aligned.length && rd_uint16_array.length == rs2_uint16_array_aligned.length
        )
        if (rs1_uint16_array_aligned.length != 1) {

          // we handle SUB with carry by NOTing the second operand and AddCing
          // them. However, unlike the ADD conversion, we initialize the first
          // carry to 1
          val carry = builder.mkCarry()
          val rs2_16_neg = builder.mkWire(instruction.rs2 + "_neg", 16)
          val const_0xFFFF = builder.mkConstant(0xffff)

          // set the initial carry to 1
          inst_q += SetCarry(carry)

          (rs1_uint16_array_aligned zip rs2_uint16_array_aligned zip rd_uint16_array_mutable)
            .foldLeft(builder.mkCarry1()) {
              case (cin, ((rs1_16, rs2_16), rd_16)) =>
                inst_q += BinaryArithmetic(
                  BinaryOperator.XOR,
                  rs2_16_neg,
                  rs2_16,
                  const_0xFFFF
                )
                val cout = builder.mkCarry()
                inst_q += AddC(
                  co = cout,
                  rd = rd_16,
                  rs1 = rs1_16,
                  rs2 = rs2_16_neg,
                  ci = cin
                )
                cout

            }

        } else {
          // trivial case, use the dedicated SUB instruction
          inst_q += instruction.copy(
            rd = rd_uint16_array_mutable.head,
            rs1 = rs1_uint16_array_aligned.head,
            rs2 = rs2_uint16_array_aligned.head
          )
        }

        inst_q ++= maskRd(rd_uint16_array_mutable.last, rd_mask, instruction)
        // val moves =
        inst_q ++= moveRegs(
          rd_uint16_array,
          rd_uint16_array_mutable,
          instruction
        )

      case op @ (BinaryOperator.OR | BinaryOperator.AND | BinaryOperator.XOR) =>
        val ConvertedWire(rd_uint16_array, mask) =
          builder.getConversion(instruction.rd)
        val rd_uint16_array_mutable = rd_uint16_array map {
          builder.mkWire(_, 16)
        }
        val rs1_uint16_array = builder.getConversion(instruction.rs1).parts
        val rs2_uint16_array = builder.getConversion(instruction.rs2).parts
        assert(
          rs1_uint16_array.size == rs2_uint16_array.size && rs1_uint16_array.size == rd_uint16_array.size,
          "size miss match"
        )
        rd_uint16_array_mutable zip (rs1_uint16_array zip rs2_uint16_array) foreach {
          case (rd_16, (rs1_16, rs2_16)) =>
            inst_q += instruction
              .copy(
                rd = rd_16,
                rs1 = rs1_16,
                rs2 = rs2_16
              )
              .setPos(instruction.pos)
        }
        inst_q ++= maskRd(rd_uint16_array_mutable.last, mask, instruction)
        inst_q ++= moveRegs(
          rd_uint16_array,
          rd_uint16_array_mutable,
          instruction
        )

      case BinaryOperator.SEQ =>
        val rs1_uint16_array = builder.getConversion(instruction.rs1).parts
        val rs2_uint16_array = builder.getConversion(instruction.rs2).parts
        assert(
          builder.originalWidth(instruction.rs1) == builder.originalWidth(
            (instruction.rs2)
          ),
          "width mismatch in SEQ operands"
        )
        assert(
          rs1_uint16_array.size == rs2_uint16_array.size,
          "width mismatch in SEQ operands"
        )
        val orig_rd_width = builder.originalWidth(instruction.rd)
        if (orig_rd_width != 1) {
          ctx.logger.warn("Expected boolean wire in SEQ", instruction)
        }
        val ConvertedWire(rd_uint16, _) =
          builder.getConversion(instruction.rd)

        if (rs1_uint16_array.length == 1) {
          inst_q += instruction.copy(
            rd = rd_uint16.head,
            rs1 = rs1_uint16_array.head,
            rs2 = rs2_uint16_array.head
          )
        } else {
          val seq_partial_res = builder.mkWire("seq_partial_res", 16)
          val seq_add_res = builder.mkWire("seq_add_res", 16)
          if (rs1_uint16_array.size >= (1 << 16)) {
            ctx.logger.error("SEQ is too wide!", instruction)
          }
          // init sum of results to zero
          inst_q += Mov(
            seq_add_res,
            builder.mkConstant(0)
          )

          // compute partial equalities by computing the equality of
          // the partial operands and then summing up the results and
          // checking whether sum is equal to the number of partial results
          rs1_uint16_array zip rs2_uint16_array foreach {
            case (rs1_16, rs2_16) =>
              inst_q += instruction.copy(
                operator = BinaryOperator.SEQ,
                rd = seq_partial_res,
                rs1 = rs1_16,
                rs2 = rs2_16
              )
              inst_q += BinaryArithmetic(
                BinaryOperator.ADD,
                seq_add_res,
                seq_add_res,
                seq_partial_res
              )
          }

          // zero out the upper bits
          inst_q ++= moveRegs(
            rd_uint16.tail,
            Seq.fill(rd_uint16.tail.length) { builder.mkConstant(0) },
            instruction
          )
          // set the least significant bit
          inst_q += BinaryArithmetic(
            BinaryOperator.SEQ,
            rd_uint16.head,
            seq_add_res,
            builder.mkConstant(rs1_uint16_array.length)
          )
        }

      case BinaryOperator.SLL if builder.isConstant(instruction.rs2) =>
        /** It is important to minimize the number of instruction here and not
          * rely on further optimization passes to coalesce MOVs or propagate
          * constants. A SLL/SRL with constant shift amount is likely
          * responsible for bit manipulation in circuits and we should make sure
          * to have the optimal implementation
          */
        val shift_amount =
          builder.originalDef(instruction.rs2).value.getOrElse {
            ctx.logger.fail("I though shift amount was constant!")
          }

        val rs = instruction.rs1
        val rd = instruction.rd
        val rs_width = builder.originalWidth(rs)
        val rd_width = builder.originalWidth(rd)
        if (shift_amount > 0xffff) {
          ctx.logger.error(
            s"Shift left amount ${shift_amount} is too large! " +
              s"Are you sure the program is correct?",
            instruction
          )
        }

        if (shift_amount >= rd_width) { // shift overflow, hardwire to zer0
          ctx.logger.warn(
            s"SLL by constant ${shift_amount} discards all bits!",
            instruction
          )
          // The result is zero, doesn't matter what the input is
          val rd_uint16_array = builder.getConversion(rd).parts
          inst_q ++= moveRegs(
            rd_uint16_array,
            rd_uint16_array.map { _ => builder.mkConstant(0) },
            instruction
          )
        } else { // reasonable shift amount

          val ConvertedWire(rd_uint16_array, rd_mask) =
            builder.getConversion(rd)
          val rs_uint16_array = builder.getConversion(rs).parts
          if (rd_width <= 16) {

            rd_mask match {
              case Some(mask) => // rd_width < 16
                val tmp = builder.mkWire("sll_non_masked", 16)
                assert(
                  shift_amount.toInt < 16,
                  "internal error, invalid transformation scope!"
                )
                inst_q += instruction.copy(
                  rd = tmp,
                  rs1 = rs_uint16_array.head,
                  rs2 = builder.mkConstant(shift_amount.toInt)
                )
                inst_q += instruction.copy(
                  operator = BinaryOperator.AND,
                  rd = rd_uint16_array.head,
                  rs1 = tmp,
                  rs2 = mask
                )

              case None => // rd_width == 16
                inst_q += instruction.copy(
                  rd = rd_uint16_array.head,
                  rs1 = rs_uint16_array.head,
                  rs2 = builder.mkConstant(shift_amount.toInt)
                )

            }

          } else {

            // we create an array the same size as the output to hold the temporary
            // computation results. Note that we have to use the output size
            // because the input operand might be narrower, we won't lose bits.
            val computable_result_length = rd_uint16_array.length

            // create a bunch temp wires to keep the intermediate result of shifting
            val result_builder_array = Seq.tabulate(computable_result_length) {
              i =>
                builder.mkWire(s"csll_builder_${i}", 16)
            }

            // Conceptually, we should subdivide the shift_amount > 16 into a
            // Seq(16, 16, ..., n) then we go through each element x in the
            // sequence and shift the short-words in the results_builder_array by
            // that amount. Note that if we have x = 16, then we are essentially
            // shifting out whole of a short-word into the next one. but if the
            // shift amount is x < 16, then we have to compute a carry by right
            // shifting the word by 16 - x and then OR the previous carry with it.

            val num_full_shifts = shift_amount.toInt / 16

            val last_shift_amount = shift_amount.toInt % 16
            // we have to do num_full_shifts SLL by 16. Each amounts to moving one
            // short-word from result_builder_array to the next one. Therefore, we
            // end up placing result_builder_array(0) in
            // result_builder_array(num_full_shifts) and result_builder_array(i)
            // in result_builder_array(i + num_full_shifts) if in range
            // any result_builder_array(j < num_full_shifts) is also initialized
            // to zero.
            val pre_shifted_operand =
              Seq.fill(num_full_shifts) { builder.mkConstant(0) } ++
                rs_uint16_array
            // append zeros to the pre_shifted_operand in case the output results
            // is wider than the pre-shifted operand
            val initial_result_builder =
              if (pre_shifted_operand.length >= result_builder_array.length)
                pre_shifted_operand.take(result_builder_array.length)
              else
                pre_shifted_operand ++
                  Seq.fill(
                    result_builder_array.length - pre_shifted_operand.length
                  ) { builder.mkConstant(0) }

            if (last_shift_amount == 0) {
              // shift_amount is a multiple of 16
              rd_mask match {
                case Some(mask) =>
                  // the most significant one needs masking
                  inst_q += instruction.copy(
                    operator = BinaryOperator.AND,
                    rd = rd_uint16_array.last,
                    rs1 = initial_result_builder.last,
                    rs2 = mask
                  )
                  // move the rest of the input operands to the output and we are
                  // done
                  inst_q ++= moveRegs(
                    rd_uint16_array.take(rd_uint16_array.length - 1),
                    initial_result_builder.take(rd_uint16_array.length - 1),
                    instruction
                  )
                case None =>
                  // no need to mask the result, therefore we just need to move
                  // the pre-shifted operands to the final output
                  inst_q ++= moveRegs(
                    rd_uint16_array,
                    initial_result_builder.take(rd_uint16_array.length),
                    instruction
                  )
              }
            } else {
              // the shift amount is not a multiple of 16, therefore we need to
              // handle the last shift by propagating carries across the whole
              // wide word.

              inst_q ++= moveRegs(
                result_builder_array,
                initial_result_builder.take(result_builder_array.length),
                instruction
              )

              // now start from the first non-zero result_builder_array element,
              // overwrite it with it self left_shifted by last_shift_amount and
              // ORed with a carry (initially zero). We also compute the carry
              // to the next element by right shifting the initial element value
              // by 16 - last_shift_amount

              val non_zero_results = result_builder_array.takeRight(
                result_builder_array.length - num_full_shifts
              )

              non_zero_results.foldLeft(builder.mkConstant(0)) {
                case (carry_in, rd_16) =>
                  // compute the carry
                  val carry_out = builder.mkWire("carry_out", 16)
                  inst_q += instruction.copy(
                    operator = BinaryOperator.SRL,
                    rd = carry_out,
                    rs1 = rd_16,
                    rs2 = builder.mkConstant(16 - last_shift_amount)
                  )
                  inst_q += instruction.copy(
                    operator = BinaryOperator.SLL,
                    rd = rd_16,
                    rs1 = rd_16,
                    rs2 = builder.mkConstant(last_shift_amount)
                  )
                  inst_q += instruction.copy(
                    operator = BinaryOperator.OR,
                    rd = rd_16,
                    rs1 = rd_16,
                    rs2 = carry_in
                  )
                  carry_out
              }

              inst_q ++= maskRd(
                result_builder_array.last,
                rd_mask,
                instruction
              )

              inst_q ++= moveRegs(
                rd_uint16_array,
                result_builder_array,
                instruction
              )
            }
          }
        }

      // no need to mask the result since the hardware implementation
      // can only produce a single bit anyways.
      case BinaryOperator.SLL if !builder.isConstant(instruction.rs2) =>
        val shift_amount = instruction.rs2
        val rs = instruction.rs1
        val rd = instruction.rd
        val ConvertedWire(shift_uint16, shift_amount_mask) =
          builder.getConversion(shift_amount)

        assert16Bit(shift_uint16, instruction) {
          "Shift amount is too large, can only support shifting up to 16-bit " +
            "number as the shift amount, are you sure your design is correct?"
        }

        val shift_orig_def = builder.originalDef(shift_amount)

        // we don't keep the mask for rs, because the producer should have taken
        // care of ANDing the most significant short word.
        val ConvertedWire(rs_uint16_array, _) = builder.getConversion(rs)

        val ConvertedWire(rd_uint16_array, rd_mask) = builder.getConversion(rd)

        /** We create a copy of the input array (rs) and modify the copy
          * in-place to compute the shifted value (rd). Note that although I
          * call it "rd_uint16_array_mutable" this array is in fact as big as
          * the larger of rd_uint16_array and rs_uint16_array. We want to
          * correctly compute a SLL in which width(rd) > width(rs). For
          * instance, if width(rs) = 2 and width(rd) = 4, SLL rd, rs = 1, 3
          * should result in rd = 4 even though we are effectively overflowing
          * the rs operand.
          */
        val lossless_result_size =
          rs_uint16_array.length max rd_uint16_array.length
        val rd_uint16_array_mutable = Seq.tabulate(lossless_result_size) { i =>
          builder.mkWire(s"sll_builder_${i}", 16)
        }

        if (rd_uint16_array_mutable.length == 1) {

          if (builder.originalWidth(shift_amount) > 16) {
            ctx.logger.error("Unsafe SLL", instruction)
          }
          // easy case
          inst_q += instruction.copy(
            rd = rd_uint16_array_mutable.head,
            rs1 = rs_uint16_array.head,
            rs2 = shift_uint16.head
          )

        } else {
          val shift_amount_mutable = builder.mkWire(
            "sh_mutable",
            16
          )

          val const_0 = builder.mkConstant(0)
          val const_1 = builder.mkConstant(1)
          val const_16 = builder.mkConstant(16)

          val shift_amount_mutable_eq_0 =
            builder.mkWire("shift_amount_mutable_eq_0", 1)
          val sixteen_minus_shift_amount_mutable =
            builder.mkWire("sixteen_minus_mutable_sh", 16)
          val fifteen_minus_shift_amount_mutable =
            builder.mkWire("fifteen_minus_sh_mutable", 16)
          val shift_amount_mutable_gt_eq_16 =
            builder.mkWire("sh_mutable_gt_eq_16", 1)
          val shift_amount_mutable_minus_16 =
            builder.mkWire("sh_mutable_minus_16", 16)
          val shift_right_amount_mutable =
            builder.mkWire("sh_right_mutable", 16)
          val right_shifted = builder.mkWire("right_shifted", 16)
          val carry_in = builder.mkWire("carry_in", 16)
          val carry_out = builder.mkWire("carry_out", 16)
          val tmp_res = builder.mkWire("tmp_res", 16)
          val tmp_tmp_res = builder.mkWire("tmp_tmp_res", 16)
          val tmp_tmp_tmp_res = builder.mkWire("tmp_tmp_tmp_res", 16)
          // initialize the rd mutable array to the values of the rs
          // if rd_uint16_array is larger, we zero extend rs
          inst_q ++= moveRegs(
            rd_uint16_array_mutable,
            rs_uint16_array
              ++ Seq.fill(lossless_result_size - rs_uint16_array.length) {
                builder.mkConstant(0)
              },
            instruction
          )

          // initialize the mutable shift amount to the original value
          inst_q +=
            BinaryArithmetic(
              BinaryOperator.ADD,
              shift_amount_mutable,
              shift_uint16.head,
              const_0
            )
          for (ix <- 0 until rd_uint16_array_mutable.length) {
            inst_q ++= Seq(
              BinaryArithmetic(
                BinaryOperator.SEQ,
                shift_amount_mutable_eq_0,
                shift_amount_mutable,
                const_0
              ),
              BinaryArithmetic(
                BinaryOperator.SUB,
                sixteen_minus_shift_amount_mutable,
                const_16,
                shift_amount_mutable
              ),
              BinaryArithmetic(
                BinaryOperator.SUB,
                fifteen_minus_shift_amount_mutable,
                sixteen_minus_shift_amount_mutable,
                const_1
              ),
              BinaryArithmetic(
                BinaryOperator.SLTS,
                shift_amount_mutable_gt_eq_16,
                fifteen_minus_shift_amount_mutable,
                const_0
              ),
              Mux(
                shift_right_amount_mutable,
                shift_amount_mutable_eq_0,
                sixteen_minus_shift_amount_mutable,
                const_0
              ),
              BinaryArithmetic(
                BinaryOperator.ADD,
                carry_in,
                const_0,
                const_0
              ),
              BinaryArithmetic(
                BinaryOperator.SUB,
                shift_amount_mutable_minus_16,
                shift_amount_mutable,
                const_16
              )
            )
            for (jx <- 0 until rd_uint16_array_mutable.length) {

              inst_q ++= Seq(
                BinaryArithmetic(
                  BinaryOperator.SRL,
                  right_shifted,
                  rd_uint16_array_mutable(jx),
                  shift_right_amount_mutable
                ),
                BinaryArithmetic(
                  BinaryOperator.SLL,
                  tmp_res,
                  rd_uint16_array_mutable(jx),
                  shift_amount_mutable
                ),
                Mux(
                  carry_out,
                  shift_amount_mutable_gt_eq_16,
                  right_shifted,
                  rd_uint16_array_mutable(jx)
                ),
                Mux(
                  tmp_tmp_res,
                  shift_amount_mutable_gt_eq_16,
                  tmp_res,
                  const_0
                ),
                BinaryArithmetic(
                  BinaryOperator.OR,
                  tmp_tmp_tmp_res,
                  tmp_tmp_res,
                  carry_in
                ),
                Mux(
                  rd_uint16_array_mutable(jx),
                  shift_amount_mutable_eq_0,
                  tmp_tmp_tmp_res,
                  rd_uint16_array_mutable(jx)
                ),
                BinaryArithmetic(
                  BinaryOperator.ADD,
                  carry_in,
                  carry_out,
                  const_0
                )
              )
            }
            inst_q +=
              Mux(
                shift_amount_mutable,
                shift_amount_mutable_gt_eq_16,
                const_0,
                shift_amount_mutable_minus_16
              )

          }
        }

        inst_q ++= maskRd(
          rd_uint16_array_mutable.take(rd_uint16_array.length).last,
          rd_mask,
          instruction
        )
        inst_q ++= moveRegs(
          rd_uint16_array,
          rd_uint16_array_mutable.take(rd_uint16_array.length),
          instruction
        )

      case BinaryOperator.SRA =>
        /** Unlike SLL and SRL, we can make the assumption that the input
          * operand and the result are ALWAYS aligned. Without this assumption,
          * there are certain complications, especially if the output is
          * narrower than the input. For instance, suppose input is 3 bits and
          * output is 2 bits, then the results of shifting input = 4 by zero is
          * zero but by 1 is 2! This is very counter-intuitive and in fact the
          * Thyrio front-end will not emit such nonsense anyways. The case that
          * the output is wider also is possible and that requires
          * sign-extending the input to compute the output. Yosys internally
          * takes care of that so we don't need to handle it here either.
          */
        val shift_amount = instruction.rs2
        val rd = instruction.rd
        val rs1 = instruction.rs1

        val ConvertedWire(shift_uint16, _) = builder.getConversion(shift_amount)
        assert16Bit(shift_uint16, instruction) {
          "Shift amount is too large, can only support shifting up to 16-bit " +
            "number as the shift amount, are you sure your design is correct?"
        }

        val ConvertedWire(rd_uint16_array, rd_mask) = builder.getConversion(rd)

        val ConvertedWire(rs1_uint16_array, _) = builder.getConversion(rs1)

        val rs_width = builder.originalWidth(rs1)
        val rd_width = builder.originalWidth(rd)
        if (rd_width != rs_width) {
          ctx.logger.error(
            "SRA operand and result should be aligned!",
            instruction
          )
        }
        // sign extension wire, maybe needed if the output is wider than the input
        val sign_replicated = builder.mkWire("sign_replicated", 16)
        val sign_bit = builder.mkWire("sign", 1)

        val msbits = rs_width % 16
        if (rd_width < 16) {

          val sign_extended_rs = builder.mkWire("sra_sign_extended_rs", 16)
          // not aligned with our machine instructions, we need to manually
          // extract the sign bit
          inst_q += BinaryArithmetic(
            BinaryOperator.SRL,
            sign_bit,
            rs1_uint16_array.last,
            builder.mkConstant(rd_width - 1)
          )
          val sext_mask = builder.mkWire("sext_mask", 16)
          inst_q += Mux(
            sext_mask,
            sign_bit,
            builder.mkConstant(0),
            builder.mkConstant((0xffff << msbits) & 0xffff)
          )
          inst_q += BinaryArithmetic(
            BinaryOperator.OR,
            sign_extended_rs,
            rs1_uint16_array.head,
            sext_mask
          )
          inst_q += instruction.copy(
            rd = sign_extended_rs,
            rs1 = sign_extended_rs,
            rs2 = shift_uint16.head
          )
          inst_q ++= maskRd(sign_extended_rs, rd_mask, instruction)
          inst_q ++= moveRegs(
            rd_uint16_array,
            Seq(sign_extended_rs),
            instruction
          )

        } else if (rd_width == 16) {
          // easy case, no translation needed
          inst_q += instruction.copy(
            rd = rd_uint16_array.head,
            rs1 = rs1_uint16_array.head,
            rs2 = shift_uint16.head
          )

        } else { // rd_width > 16

          /** We create a copy of rs and modify/shift in-place. This copy is
            * later sign extended and moved back to the result registers if
            * needed
            */
          val rd_uint16_array_mutable = Seq.tabulate(rs1_uint16_array.length) {
            i =>
              builder.mkWire(s"sra_builder_${i}", 16)
          }

          inst_q ++= moveRegs(
            rd_uint16_array_mutable,
            rs1_uint16_array,
            instruction
          )

          val mutable_sh =
            builder.mkWire("mutable_sh", 16)
          inst_q += BinaryArithmetic(
            BinaryOperator.ADD,
            mutable_sh,
            shift_uint16.head,
            builder.mkConstant(0)
          )
          val msbits =
            rs_width % 16 // number of valid bits in the most significant short word

          val msbits_shift = builder.mkConstant(msbits - 1)

          if (msbits != 0) {
            // not 16-bit aligned, so need to extend the sign bit
            inst_q += BinaryArithmetic(
              BinaryOperator.SRL,
              sign_bit,
              rd_uint16_array_mutable.last,
              builder.mkConstant(msbits - 1)
            )
            val sext_mask = builder.mkWire("sext_mask", 16)
            inst_q += Mux(
              sext_mask,
              sign_bit,
              builder.mkConstant(0),
              builder.mkConstant((0xffff << msbits) & 0xffff)
            )
            inst_q += BinaryArithmetic(
              BinaryOperator.OR,
              rd_uint16_array_mutable.last,
              rd_uint16_array_mutable.last,
              sext_mask
            )
          } else {
            inst_q += BinaryArithmetic(
              BinaryOperator.SRL,
              sign_bit,
              rd_uint16_array_mutable.last,
              builder.mkConstant(15)
            )
          }

          inst_q += Mux(
            sign_replicated,
            sign_bit,
            builder.mkConstant(0),
            builder.mkConstant(0xffff)
          )

          for (ix <- (rd_uint16_array_mutable.length - 1) to 0 by -1) {

            val left_shift_amount = builder.mkWire("left_shift_amount", 16)
            val sixteen_minus_shift_amount =
              builder.mkWire("sixteen_minus_shift_amount", 16)
            val mutable_sh_eq_0 = builder.mkWire("mutable_sh_eq_0", 1)

            inst_q ++= Seq(
              BinaryArithmetic(
                BinaryOperator.SEQ,
                mutable_sh_eq_0,
                mutable_sh,
                builder.mkConstant(0)
              ),
              BinaryArithmetic(
                BinaryOperator.SUB,
                sixteen_minus_shift_amount,
                builder.mkConstant(16),
                mutable_sh
              ),
              Mux(
                left_shift_amount,
                mutable_sh_eq_0,
                sixteen_minus_shift_amount,
                builder.mkConstant(0)
              )
            )
            val carry_in = builder.mkWire("carry_in", 16)

            // handle the most significant short word, in this case we will actually
            // use the SRA instruction, but for the other short words we use SRL
            val carry_out = builder.mkWire("carry_out", 16)
            val rd_left_shifted = builder.mkWire("rd_left_shifted", 16)
            inst_q += BinaryArithmetic(
              BinaryOperator.SLL,
              rd_left_shifted,
              rd_uint16_array_mutable.last,
              left_shift_amount
            )
            val mutable_sh_gt_eq_16 = builder.mkWire("mutable_sh_gt_eq_16", 1)
            val fifteen_minus_mutable_sh =
              builder.mkWire("fifteen_minus_mutable_sh", 16)
            inst_q += BinaryArithmetic(
              BinaryOperator.SUB,
              fifteen_minus_mutable_sh,
              sixteen_minus_shift_amount,
              builder.mkConstant(1)
            )
            inst_q += BinaryArithmetic(
              BinaryOperator.SLTS,
              mutable_sh_gt_eq_16,
              fifteen_minus_mutable_sh,
              builder.mkConstant(0)
            )
            inst_q += Mux(
              carry_out,
              mutable_sh_gt_eq_16,
              rd_left_shifted,
              rd_uint16_array_mutable.last
            )
            val rd_right_shifted = builder.mkWire("rd_right_shifted", 16)
            inst_q += BinaryArithmetic(
              BinaryOperator.SRA,
              rd_right_shifted,
              rd_uint16_array_mutable.last,
              mutable_sh
            )
            val local_res = builder.mkWire("local_res", 16)
            inst_q += Mux(
              local_res,
              mutable_sh_gt_eq_16,
              rd_right_shifted,
              sign_replicated
            )
            inst_q += Mux(
              rd_uint16_array_mutable.last,
              mutable_sh_eq_0,
              local_res,
              rd_uint16_array_mutable.last
            )
            inst_q += BinaryArithmetic(
              BinaryOperator.ADD,
              carry_in,
              carry_out,
              builder.mkConstant(0)
            )
            val new_res = builder.mkWire("new_res", 16)
            // handle the rest (if any)
            for (jx <- (rd_uint16_array_mutable.length - 2) to 0 by -1) {

              inst_q += BinaryArithmetic(
                BinaryOperator.SLL,
                rd_left_shifted,
                rd_uint16_array_mutable(jx),
                left_shift_amount
              )
              inst_q += Mux(
                carry_out,
                mutable_sh_gt_eq_16,
                rd_left_shifted,
                rd_uint16_array_mutable(jx)
              )
              // we use logical right shift because the "sign" carry is
              // handled manually
              inst_q += BinaryArithmetic(
                BinaryOperator.SRL,
                rd_right_shifted,
                rd_uint16_array_mutable(jx),
                mutable_sh
              )
              inst_q += Mux(
                local_res,
                mutable_sh_gt_eq_16,
                rd_right_shifted,
                builder.mkConstant(0)
              )
              inst_q += BinaryArithmetic(
                BinaryOperator.OR,
                new_res,
                local_res,
                carry_in
              )
              inst_q += Mux(
                rd_uint16_array_mutable(jx),
                mutable_sh_eq_0,
                new_res,
                rd_uint16_array_mutable(jx)
              )
              inst_q += BinaryArithmetic(
                BinaryOperator.ADD,
                carry_in,
                carry_out,
                builder.mkConstant(0)
              )
            }

            val mutable_sh_minus_sixteen =
              builder.mkWire("mutable_sh_minus_sixteen", 16)
            inst_q += BinaryArithmetic(
              BinaryOperator.SUB,
              mutable_sh_minus_sixteen,
              mutable_sh,
              builder.mkConstant(16)
            )
            inst_q += Mux(
              mutable_sh,
              mutable_sh_gt_eq_16,
              builder.mkConstant(0),
              mutable_sh_minus_sixteen
            )
          }

          // we have the computed output, mask and move it
          inst_q ++= maskRd(rd_uint16_array_mutable.last, rd_mask, instruction)
          inst_q ++= moveRegs(
            rd_uint16_array,
            rd_uint16_array_mutable,
            instruction
          )
        }

      case BinaryOperator.SRL if builder.isConstant(instruction.rs2) =>
        val shift_amount = builder.originalDef(instruction.rs2).value.get

        if (shift_amount > 0xffff) {
          ctx.logger.error(
            s"SRL shift amount ${shift_amount} is too large!",
            instruction
          )
        }

        val rd = instruction.rd
        val rs = instruction.rs1
        val rd_width = builder.originalWidth(rd)
        val rs_width = builder.originalWidth(rs)

        // Constant SRL is very useful in bit extraction, so we have to do our
        // best to translate to a minimal representation. Constant SRL is
        // controlled by the rs_width, that is based on shift_amount and
        //
        // We handle multiple cases separately:
        //   1. shift_amount >= rs_width => probably wrong code because the
        //      results is zero
        //   2. shift_amount < rs_width =>
        //      2.1 rs_width < 16 => trivial transformation, apply masking if needed
        //      2.2 rs_width >= 16 =>
        //         requires multiple instructions
        //         2.2.1 shift_amount % 16 == 0 =>
        //            shift implemented by a series of MOVs
        //         2.2.2 shift_amount % 16 != 0 =>
        //             shift implemented by a series of MOVs and then a chain of carries
        //

        val ConvertedWire(rd_uint16_array, rd_mask) = builder.getConversion(rd)

        if (shift_amount.toInt >= rs_width) {

          ctx.logger.warn("SRA discards all bits!", instruction)
          inst_q ++= moveRegs(
            rd_uint16_array,
            Seq.fill(rd_uint16_array.length) { builder.mkConstant(0) },
            instruction
          )
        } else {

          val rs_uint16_array = builder.getConversion(rs).parts
          if (rs_width <= 16) {
            assert(shift_amount < 16, "invalid SRL transformation scope")
            if (rd_width <= 16) {
              rd_mask match {
                case Some(mask) =>
                  val rd_builder = builder.mkWire("srl_rd_builder", 16)
                  inst_q += instruction.copy(
                    rd = rd_builder,
                    rs1 = rs_uint16_array.head,
                    rs2 = builder.mkConstant(shift_amount.toInt)
                  )
                  inst_q += instruction.copy(
                    operator = BinaryOperator.AND,
                    rd = rd_uint16_array.head,
                    rs1 = rd_builder,
                    rs2 = mask
                  )
                case None =>
                  inst_q += instruction.copy(
                    rd = rd_uint16_array.head,
                    rs1 = rs_uint16_array.head,
                    rs2 = builder.mkConstant(shift_amount.toInt)
                  )
              }
            } else {
              // rd is wider, zero out the higher bits
              inst_q += instruction.copy(
                rd = rd_uint16_array.head,
                rs1 = rs_uint16_array.head,
                rs2 = builder.mkConstant(shift_amount.toInt)
              )
              inst_q ++= moveRegs(
                rd_uint16_array.tail,
                rd_uint16_array.tail.map { _ => builder.mkConstant(0) },
                instruction
              )
            }
          } else { // rs_width > 16

            val num_full_shifts = shift_amount.toInt / 16
            val last_shift_amount = shift_amount.toInt % 16
            // num_full_shifts parts of the input are discarded so take the ones that remain
            val pre_shifted = rs_uint16_array.takeRight(
              rs_uint16_array.length - num_full_shifts
            )
            val pre_shifted_rd_aligned =
              if (rd_uint16_array.length >= pre_shifted.length)
                pre_shifted ++ Seq.fill(
                  rd_uint16_array.length - pre_shifted.length
                ) { builder.mkConstant(0) }
              else
                pre_shifted.take(rd_uint16_array.length)

            if (last_shift_amount == 0) {
              // shifting is fully aligned, only use MOVs
              val movable = rd_mask match {
                case Some(mask) =>
                  if (pre_shifted_rd_aligned.last == builder.mkConstant(0)) {
                    // no need to mask, the last part is already masked
                    pre_shifted_rd_aligned
                  } else {
                    // need to mask the last element in the pre_shifted_rd_aligned
                    // array
                    val masked_last = builder.mkWire("masked_last", 16)
                    inst_q += instruction.copy(
                      operator = BinaryOperator.AND,
                      rd = masked_last,
                      rs1 = pre_shifted_rd_aligned.last,
                      rs2 = mask
                    )
                    pre_shifted_rd_aligned.take(
                      pre_shifted_rd_aligned.length - 1
                    ) :+ masked_last
                  }

                case None =>
                  // no mask required for the result
                  pre_shifted_rd_aligned
              }
              inst_q ++= moveRegs(
                rd_uint16_array,
                movable,
                instruction
              )

            } else { // non-aligned shifting :(

              // now we need to start from the last element of srl_builder_array
              // and move backwards, first computing the carry out which is the
              // srl_builder_array element left shifted by 16 - last_shift_amount
              // However, if rd is wider than the pre-shifted rs we shouldn't
              // start from the last element of srl_builder because we end
              // shifting zeros wastefully.

              def constructCarryChain(
                  rd_array: Seq[Name],
                  rs_array: Seq[Name],
                  initial_carry_in: Name
              ): Unit =
                (rd_array zip rs_array).foldRight(initial_carry_in) {
                  case ((rd_16_t, rs_16), carry_in) =>
                    // compute the carry out but not for the first one (carry out is not used anywhere)
                    val carry_out = builder.mkWire("srl_co", 16)
                    if (rd_16_t != rd_array.head) {
                      inst_q += instruction.copy(
                        operator = BinaryOperator.SLL,
                        rd = carry_out,
                        rs1 = rs_16,
                        rs2 = builder.mkConstant(16 - last_shift_amount)
                      )
                    }
                    // do the shifting
                    if (carry_in != builder.mkConstant(0)) {
                      val srl_builder = builder.mkWire("srl_builder", 16)
                      inst_q += instruction.copy(
                        rd = srl_builder,
                        rs1 = rs_16,
                        rs2 = builder.mkConstant(last_shift_amount)
                      )
                      inst_q += instruction.copy(
                        operator = BinaryOperator.OR,
                        rd = rd_16_t,
                        rs1 = srl_builder,
                        rs2 = carry_in
                      )
                    } else {
                      inst_q += instruction.copy(
                        rd = rd_16_t,
                        rs1 = rs_16,
                        rs2 = builder.mkConstant(last_shift_amount)
                      )
                    }

                    carry_out
                }
              if (rd_uint16_array.length > pre_shifted.length) {
                //

                val non_zero_rd = rd_uint16_array.take(pre_shifted.length)
                constructCarryChain(
                  non_zero_rd,
                  pre_shifted,
                  builder.mkConstant(0)
                )

                // zero out the upper bits of rd
                val zero_len = rd_uint16_array.length - non_zero_rd.length
                inst_q ++= moveRegs(
                  rd_uint16_array.takeRight(zero_len),
                  Seq.fill(zero_len) { builder.mkConstant(0) },
                  instruction
                )
              } else if (rd_uint16_array.length <= pre_shifted.length) {
                // handle the most significant part of rd first
                val carry_in =
                  if (rd_uint16_array.length == pre_shifted.length) {
                    builder.mkConstant(0)
                  } else {
                    val ci = builder.mkWire("srl_ci", 16)
                    inst_q += instruction.copy(
                      operator = BinaryOperator.SLL,
                      rd = ci,
                      rs1 = pre_shifted(rd_uint16_array.length),
                      rs2 = builder.mkConstant(16 - last_shift_amount)
                    )
                    ci
                  }
                val rd_last = rd_mask match {
                  case Some(_) =>
                    builder.mkWire("srl_builder_last", 16)
                  case None =>
                    rd_uint16_array.last
                }
                // now build the carry chain (excluding the last element)
                val rd_in_chain =
                  rd_uint16_array.take(rd_uint16_array.length - 1) :+ rd_last
                val rs_in_chain = pre_shifted_rd_aligned
                constructCarryChain(rd_in_chain, rs_in_chain, carry_in)

                rd_mask match {
                  case Some(mask) =>
                    inst_q += instruction.copy(
                      operator = BinaryOperator.AND,
                      rd = rd_uint16_array.last,
                      rs1 = rd_last,
                      rs2 = mask
                    )
                  case None =>
                }
              }

            }

          }

        }

      case BinaryOperator.SRL if !builder.isConstant(instruction.rs2) =>
        val ConvertedWire(shift_uint16, _) =
          builder.getConversion(instruction.rs2)
        val ConvertedWire(rd_uint16_array, rd_mask) =
          builder.getConversion(instruction.rd)

        val ConvertedWire(rs1_uint16_array, _) =
          builder.getConversion(instruction.rs1)

        // we create a copy of RS1 and shift it in a mutable manner
        // and then move this array to the RD. In case width(rd) < width(rs1)
        // we don't have any bits lost, so the shift operation can be use
        // to extract ranges from a larger word (i.e., RS1) into a smaller one
        // (i.e., RD)
        val mutable_array_size = rs1_uint16_array.length

        val rd_uint16_array_mutable = Seq.tabulate(mutable_array_size) { i =>
          builder.mkWire(s"srl_builder_${i}", 16)
        }

        if (rd_uint16_array_mutable.length == 1) {

          if (builder.originalWidth(instruction.rs2) > 16) {
            ctx.logger.error("Unsafe SRL", instruction)
          }

          inst_q += instruction.copy(
            rd = rd_uint16_array_mutable.head,
            rs1 = rs1_uint16_array.head,
            rs2 = shift_uint16.head
          )
        } else {

          // initialize the rd mutable array
          inst_q ++= moveRegs(
            rd_uint16_array_mutable,
            rs1_uint16_array,
            instruction
          )

          assert16Bit(shift_uint16, instruction) {
            "Shift amount is too large, can only support shifting up to 16-bit " +
              "number as the shift amount, are you sure your design is correct?"
          }

          val mutable_sh =
            builder.mkWire("mutable_sh", 16)
          inst_q += BinaryArithmetic(
            BinaryOperator.ADD,
            mutable_sh,
            shift_uint16.head,
            builder.mkConstant(0)
          )
          for (ix <- (rd_uint16_array_mutable.length - 1) to 0 by -1) {

            val mutable_sh_eq_0 = builder.mkWire("mutable_sh_eq_0", 1)
            inst_q += BinaryArithmetic(
              BinaryOperator.SEQ,
              mutable_sh_eq_0,
              mutable_sh,
              builder.mkConstant(0)
            )
            val carry_in = builder.mkWire("carry_in", 16)
            val mutable_sh_gt_eq_16 = builder.mkWire("mutable_sh_gt_eq_16", 1)
            val sixteen_minus_shift_amount =
              builder.mkWire("sixteen_minus_shift_amount", 16)
            inst_q +=
              BinaryArithmetic(
                BinaryOperator.SUB,
                sixteen_minus_shift_amount,
                builder.mkConstant(16),
                mutable_sh
              )
            val fifteen_minus_mutable_sh =
              builder.mkWire("fifteen_minus_mutable_sh", 16)
            inst_q += BinaryArithmetic(
              BinaryOperator.SUB,
              fifteen_minus_mutable_sh,
              sixteen_minus_shift_amount,
              builder.mkConstant(1)
            )
            inst_q += BinaryArithmetic(
              BinaryOperator.SLTS,
              mutable_sh_gt_eq_16,
              fifteen_minus_mutable_sh,
              builder.mkConstant(0)
            )
            inst_q += BinaryArithmetic(
              BinaryOperator.ADD,
              carry_in,
              builder.mkConstant(0),
              builder.mkConstant(0)
            )
            val left_shift_amount = builder.mkWire("left_shift_amount", 16)
            inst_q +=
              Mux(
                left_shift_amount,
                mutable_sh_eq_0,
                sixteen_minus_shift_amount,
                builder.mkConstant(0)
              )

            val new_res = builder.mkWire("new_res", 16)
            val rd_left_shifted = builder.mkWire("rd_left_shifted", 16)
            val carry_out = builder.mkWire("carry_out", 16)
            val rd_right_shifted = builder.mkWire("rd_right_shifted", 16)
            val local_res = builder.mkWire("local_res", 16)
            // handle the rest (if any)
            for (jx <- (rd_uint16_array_mutable.length - 1) to 0 by -1) {

              inst_q += BinaryArithmetic(
                BinaryOperator.SLL,
                rd_left_shifted,
                rd_uint16_array_mutable(jx),
                left_shift_amount
              )
              inst_q += Mux(
                carry_out,
                mutable_sh_gt_eq_16,
                rd_left_shifted,
                rd_uint16_array_mutable(jx)
              )
              // we use logical right shift because the carry is handled explicitly
              inst_q += BinaryArithmetic(
                BinaryOperator.SRL,
                rd_right_shifted,
                rd_uint16_array_mutable(jx),
                mutable_sh
              )
              inst_q += Mux(
                local_res,
                mutable_sh_gt_eq_16,
                rd_right_shifted,
                builder.mkConstant(0)
              )
              inst_q += BinaryArithmetic(
                BinaryOperator.OR,
                new_res,
                local_res,
                carry_in
              )
              inst_q += Mux(
                rd_uint16_array_mutable(jx),
                mutable_sh_eq_0,
                new_res,
                rd_uint16_array_mutable(jx)
              )
              inst_q += BinaryArithmetic(
                BinaryOperator.ADD,
                carry_in,
                carry_out,
                builder.mkConstant(0)
              )
            }

            val mutable_sh_minus_sixteen =
              builder.mkWire("mutable_sh_minus_sixteen", 16)
            inst_q += BinaryArithmetic(
              BinaryOperator.SUB,
              mutable_sh_minus_sixteen,
              mutable_sh,
              builder.mkConstant(16)
            )
            inst_q += Mux(
              mutable_sh,
              mutable_sh_gt_eq_16,
              builder.mkConstant(0),
              mutable_sh_minus_sixteen
            )

          }
        }

        if (rd_uint16_array.length > rd_uint16_array_mutable.length) {
          inst_q ++= moveRegs(
            rd_uint16_array,
            rd_uint16_array_mutable ++
              Seq.fill(
                rd_uint16_array.length - rd_uint16_array_mutable.length
              ) {
                builder.mkConstant(0)
              },
            instruction
          )
        } else {
          inst_q ++= maskRd(
            rd_uint16_array_mutable.take(rd_uint16_array.length).last,
            rd_mask,
            instruction
          )
          inst_q ++= moveRegs(
            rd_uint16_array,
            rd_uint16_array_mutable.take(rd_uint16_array.length),
            instruction
          )
        }

      case BinaryOperator.SLTS =>
        // we should only handle SLTS rd, rs1, const_0, because this is the
        // only thing the Thyrio frontend gives us. This is rather easy and
        // cheap, we only need to convert rs1 into an array and then check
        // the most significant half-word of rs1 to be less than zero!
        def assumptions(): Unit = {
          val rs2_const = builder.originalDef(instruction.rs2)
          if (rs2_const.variable.varType != ConstType) {
            ctx.logger.error(
              "Second SLTS operand should be constant 0!",
              instruction
            )
          } else {
            rs2_const.value match {
              case Some(x) if x == 0 => // nothing
              case _ =>
                ctx.logger.error(
                  "Second SLTS operand should be constant 0!",
                  instruction
                )
            }
          }
        }
        assumptions()

        val ConvertedWire(rs1_uint16_array, _) =
          builder.getConversion(instruction.rs1)
        val orig_rd_width = builder.originalWidth(instruction.rd)
        if (orig_rd_width == 1) {
          ctx.logger.warn("Expected Boolean result in SLTS", instruction)
        }
        val rd_uint16 = builder.getConversion(instruction.rd).parts.head

        // number of used bits in the most significant short word
        val rs1_ms_half_word_bits = builder.originalWidth(instruction.rs1) % 16

        if (rs1_ms_half_word_bits == 0) {
          // the first operand is 16-bit aligned, so we can simply perform
          // SLTS on the most significant short-word
          inst_q += instruction.copy(
            rd = rd_uint16,
            rs1 = rs1_uint16_array.last,
            rs2 = builder.mkConstant(0)
          )
        } else {
          // the first operand is not 16-bit aligned
          // now we need to shift the most significant bit of the rs1_uint16_array.last
          // to the right and bring it to bit position zero, then we can just use
          // a SEQ to check the sign bit.
          if (rs1_ms_half_word_bits == 1) {
            // trivial case, no need to right shift
            inst_q += instruction.copy(
              operator = BinaryOperator.SEQ,
              rd = rd_uint16,
              rs1 = rs1_uint16_array.last,
              rs2 = builder.mkConstant(1)
            )
          } else {
            // need to right shift the most significant bit
            val sign_bit = builder.mkWire("sign_bit", 16)
            inst_q += instruction.copy(
              operator = BinaryOperator.SRL,
              rd = sign_bit,
              rs1 = rs1_uint16_array.last,
              rs2 = builder.mkConstant(rs1_ms_half_word_bits - 1)
            )
            inst_q += instruction.copy(
              operator = BinaryOperator.SEQ,
              rd = rd_uint16,
              rs1 = sign_bit,
              rs2 = builder.mkConstant(1)
            )
          }
        }
      // no need to mask

    }
    // set the position
    inst_q.foreach(_.setPos(instruction.pos))

    inst_q.toSeq
  }

  def log2ceil(x: Int) = BigInt(x - 1).bitLength

  private def appendIndexToMemblock(
      instruction: Instruction,
      orig_mblock: Option[Memblock],
      index: Int
  )(implicit ctx: AssemblyContext) = {
    // append an index to the Memblock annotation to be able to resolve the memory access later
    // the index comes from the index associated with the destination register
    // this is necessary since we are essentially creating multiple narrow
    // memories from a wide ones. The final simulation binary or
    // an intermediate interpreter can should be able to distinguish between
    // the newly create memory blocks especially in the case of memories with
    // initial values.

    val other_annons = instruction.annons.filter {
      case _: Memblock => false
      case _           => true
    }
    val indexed_mblock = orig_mblock match {
      case Some(mblock) => mblock.withIndex(index)
      case None =>
        ctx.logger.fail("Expected a memory block!")
    }

    other_annons :+ indexed_mblock
  }
  def convert(instruction: Instruction)(implicit
      ctx: AssemblyContext,
      builder: Builder
  ): Seq[Instruction] = instruction match {
    case i: BinaryArithmetic                => convertBinaryArithmetic(i)
    case i @ LocalLoad(rd, base, offset, _) =>
      /** Memories with wide words are translated into multiple parallel
        * memories with short-words where the for instance a 10 deep 33 wide
        * memory is translate into 3 memories that are 10 deep and each 16-bit
        * wide. This streamlines the translation process quite significantly as
        * we do not need to do any sort of wide-address to short-address
        * conversion.
        */
      val ConvertedWire(rd_uint16_array, rd_mask) = builder.getConversion(rd)
      val ConvertedWire(base_uint16_array, _) = builder.getConversion(base)
      // ensure the memory can fit in a physical BRAM
      val orig_mblock = i.annons.collectFirst { case m: Memblock =>
        m
      }

      val num_shorts = orig_mblock match {
        case Some(mblock) =>
          val width = mblock.getIntValue(AssemblyAnnotationFields.Width).get
          val capacity = mblock.getIntValue(AssemblyAnnotationFields.Width).get
          val shorts = (width - 1) / 16 + 1
          (shorts * capacity)
        case _ =>
          ctx.logger.error("Could not find Memblock annotation", instruction)
          0
      }

      if (num_shorts > (1 << MaxLocalAddressBits)) {
        ctx.logger.error(
          s"Can not handle large memories yet with ${num_shorts} short words!"
        )
        // TODO: promote to global access
        Seq(i)
      } else {
        val base_uint16_head = base_uint16_array.head
        // here we discard the highest bits of base_uint16 because a valid
        // program should not access those. In fact we have a wide base registers
        // because of PADZERO instructions and the fact that we don't shrink
        // on ADD, but only expand.
        if (offset != 0) {
          ctx.logger.warn(
            "Thyrio is supposed to use offset zero at all times, are you not using Thyrio?",
            i
          )
        }

        rd_uint16_array.zipWithIndex.map { case (rd_16, index) =>
          i.copy(
            rd = rd_16,
            base = base_uint16_head,
            offset = offset,
            annons = appendIndexToMemblock(i, orig_mblock, index)
          ).setPos(i.pos)
        }
      }

    case i @ LocalStore(rs, base, offset, predicate, _) =>
      val ConvertedWire(rs_uint16_array, _) = builder.getConversion(rs)
      val ConvertedWire(base_uint16_array, _) = builder.getConversion(base)
      val pred_uint16 = predicate.map { p =>
        val conv = builder.getConversion(p).parts
        if (conv.length != 1) {
          ctx.logger.error("LST predicate should be single-bit!")
        }
        conv.head
      }
      val orig_mblock = instruction.annons.collectFirst { case x: Memblock =>
        x
      }

      val num_shorts = orig_mblock match {
        case Some(mblock) =>
          val width = mblock.getIntValue(AssemblyAnnotationFields.Width).get
          val capacity = mblock.getIntValue(AssemblyAnnotationFields.Width).get
          val shorts = (width - 1) / 16 + 1
          (shorts * capacity)
        case _ =>
          ctx.logger.error("Could not find Memblock annotation", instruction)
          0
      }

      if (num_shorts > (1 << MaxLocalAddressBits)) {
        ctx.logger.error(
          s"Can not handle large memories yet with ${num_shorts} short words!"
        )
        // TODO: promote to global access
        Seq(i)
      } else {
        val base_uint16_head = base_uint16_array.head

        if (offset != 0) {
          ctx.logger.error(
            "Thyrio is supposed to use offset zero at all times, are you not using Thyrio?",
            i
          )
        }
        rs_uint16_array.zipWithIndex.map { case (rs_16, index) =>
          i.copy(
            rs = rs_16,
            base = base_uint16_head,
            offset = offset,
            predicate = pred_uint16,
            annons = appendIndexToMemblock(i, orig_mblock, index)
          ).setPos(i.pos)
        }
      }

    case i @ Expect(ref, got, _, _) =>
      val ConvertedWire(ref_uint16_array, _) = builder.getConversion(ref)
      val ConvertedWire(got_uint16_array, _) = builder.getConversion(got)
      ref_uint16_array zip got_uint16_array map { case (r16, g16) =>
        i.copy(r16, g16).setPos(i.pos)
      }
    case i @ Mux(rd, sel, rfalse, rtrue, _) =>
      val ConvertedWire(rd_uint16_array, _) = builder.getConversion(rd)
      val sel_uint16_array = builder.getConversion(sel).parts
      if (sel_uint16_array.size != 1) {
        ctx.logger.error("sel should be single bit!", i)
      }
      val rfalse_uint16_array = builder.getConversion(rfalse).parts
      val rtrue_uint16_array = builder.getConversion(rtrue).parts
      rd_uint16_array zip (rfalse_uint16_array zip rtrue_uint16_array) map {
        case (rd16, (rfalse16, rtrue16)) =>
          i.copy(
            rd = rd16,
            sel = sel_uint16_array.head,
            rfalse = rfalse16,
            rtrue = rtrue16
          ).setPos(i.pos)
      } // don't need to mask the results though, rfalse and rtrue are masked
    // where they are produced and MUX cannot overflow them
    case i: AddC =>
      ctx.logger.error("AddC can only be inserted by the compiler!", i)
      Seq.empty[Instruction]
    case Nop =>
      ctx.logger.error("Nops can only be inserted by the compiler!")
      Seq.empty[Instruction]
    case i @ PadZero(rd, rs, w, annons) =>
      val rd_w = builder.originalWidth(rd)
      if (rd_w != w) {
        ctx.logger.error("Invalid padding width!", i)
      }
      val rd_uint16_array = builder.getConversion(rd).parts
      val rs_uint16_array = builder.getConversion(rs).parts

      rd_uint16_array.zip(
        rs_uint16_array ++ Seq.fill(
          rd_uint16_array.length - rs_uint16_array.length
        )(builder.mkConstant(0))
      ) map { case (rd_16, rs_16) =>
        Mov(
          rd = rd_16,
          rs = rs_16,
          annons
        ).setPos(i.pos)
      }
    case i @ Mov(rd, rs, annons) =>
      val rd_uint16_array = builder.getConversion(rd).parts
      val rs_uint16_array = builder.getConversion(rs).parts
      if (builder.originalWidth(rd) != builder.originalWidth(rs)) {
        ctx.logger.error("Mov instruction can not change width!", i)
      }

      rd_uint16_array zip rs_uint16_array map { case (rd16, rs16) =>
        i.copy(rd = rd16, rs = rs16).setPos(i.pos)
      }

    case _ =>
      ctx.logger.error(
        "Can not handle this type of instruction yet",
        instruction
      )
      Seq.empty[Instruction]

  }

  def convert(proc: DefProcess)(implicit ctx: AssemblyContext): DefProcess = {

    implicit val builder = new Builder(proc)

    val insts: Seq[Instruction] = proc.body.flatMap { convert(_) }

    builder.buildFrom(insts)

  }
  override def transform(
      source: DefProgram,
      context: AssemblyContext
  ): DefProgram = source.copy(processes = source.processes.map { p =>
    convert(p)(context)
  })

}
