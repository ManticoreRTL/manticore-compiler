package manticore.assembly.levels.unconstrained

import manticore.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import manticore.assembly.BinaryOperator
import manticore.assembly.levels.ConstType
import manticore.assembly.levels.WireType
import manticore.assembly.annotations.DebugSymbol
import manticore.assembly.annotations.AssemblyAnnotation
import manticore.assembly.annotations.AssemblyAnnotationFields
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
  *     val sign = rd.last >> (msbits - 1)
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
  *         // val right_shifted = rd(jx) >> hsm.toInt
  *         val carry_out =
  *           if (msh_gt_eq_16) // ms > 16
  *             rd(jx)
  *           else
  *             rd(jx) >> right_shift_amount // val right_shifted
  *
  *         // val res = rd(jx) << msh
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
object UnconstrainedBigIntTo16BitsTransform
    extends AssemblyTransformer[
      UnconstrainedIR.DefProgram,
      UnconstrainedIR.DefProgram
    ] {

  import UnconstrainedIR._

  private case class ConvertedWire(parts: Seq[Name], mask: Option[Name])
  private class ConversionBuilder(private val proc: DefProcess) {

    private var m_name_id = 0
    private val m_wires = scala.collection.mutable.Queue.empty[DefReg]
    private val m_constants =
      scala.collection.mutable.HashMap.empty[Int, DefReg]
    private val m_subst =
      scala.collection.mutable.HashMap.empty[Name, ConvertedWire]
    private val m_old_defs = proc.registers.map { r =>
      r.variable.name -> r
    }.toMap

    /** Build the converted process from the given sequence of instructions
      *
      * @param instructions
      * @return
      */
    def buildFrom(instructions: Seq[Instruction]): DefProcess =
      proc
        .copy(
          registers = m_wires.toSeq ++ m_constants.values,
          body = instructions
        )
        .setPos(proc.pos)

    /** Create a unique name
      *
      * @param suggestion
      * @return
      */
    private def freshName(suggestion: Name): Name = {
      val n = s"${suggestion.toString()}_$$${m_name_id}$$"
      m_name_id += 1
      return n
    }

    /** Create a new 16-bit constant
      *
      * @param value
      * @return
      */
    def mkConstant(value: Int): Name = {
      require(value < (1 << 16), "constants should fit in 16 bits")
      m_constants
        .getOrElseUpdate(
          value,
          DefReg(
            LogicVariable(
              freshName(s"const_${value}"),
              16,
              ConstType
            ),
            Some(value)
          )
        )
        .variable
        .name
    }

    /** Helper function to create temp wires, do not use this function if you
      * are converting a wire
      *
      * @param suggestion
      * @param width
      * @return
      */
    def mkWire(suggestion: Name, width: Int): Name = {
      require(width <= 16, "cannot create wide wire")
      require(width >= 1, "cannot create empty wire")
      require(
        m_old_defs.contains(suggestion) == false,
        "can not create a new wire for something that is defined in the original program"
      )
      val new_wire =
        DefReg(LogicVariable(freshName(suggestion), width, WireType), None)
      m_wires += new_wire
      new_wire.variable.name
    }

    /** Converts a wire in the original program to a sequence of wires that are
      * 16-bit wide at max
      *
      * @param original
      *   the name of the original wire, should be defined in the original
      *   program
      * @return
      *   a converted wire
      */
    private def convertWire(original: Name): ConvertedWire = {
      require(m_old_defs.contains(original), "can not convert undefined wire")
      require(m_subst.contains(original) == false, "conversion already exists!")
      val orig_def = m_old_defs(original)
      val width = orig_def.variable.width
      val array_size = (width - 1) / 16 + 1
      assert(array_size >= 1)
      val mask_bits = width - (array_size - 1) * 16
      assert(mask_bits <= 16)
      val msw_mask = if (mask_bits < 16) {
        val mask_const = mkConstant((1 << mask_bits) - 1)
        Some(mask_const)
      } else None

      val uint16_vars = Seq.tabulate(array_size) { i =>
        LogicVariable(
          freshName(original + s"_$i"),
          if (i == array_size - 1) mask_bits else 16,
          orig_def.variable.varType
        )
      }

      val values: Seq[Option[BigInt]] = orig_def.value match {
        case None => Seq.fill(array_size)(None)
        case Some(big_val) =>
          Seq.tabulate(array_size) { i =>
            val small_val_mask_bits = if (i == array_size - 1) mask_bits else 16
            val small_val =
              (big_val >> (i * 16)) & ((1 << small_val_mask_bits) - 1)
            assert(small_val < (1 << 16))
            Some(small_val)
          }
      }

      val conv_def = (uint16_vars zip values).zipWithIndex.map {
        case ((cvar, cval), ix) =>
          val annons = orig_def.annons.collect {
            case x: DebugSymbol =>
              if (x.fields.contains(AssemblyAnnotationFields.Index)) {
                logger.error("did not expect debug symbol index", orig_def)
              }
              val with_index = x.withIndex(ix)
              with_index.getIntValue(AssemblyAnnotationFields.Width) match {
                case Some(w) =>
                  if (w != width) {
                    logger.error("Width mismatch in the debug symbol", orig_def)
                  }
                  with_index
                case None =>
                  with_index.withWidth(width)
              }
            case y => y
          }

          orig_def
            .copy(variable = cvar, value = cval, annons = annons)
            .setPos(orig_def.pos)
      }

      m_wires ++= conv_def
      val converted =
        ConvertedWire(uint16_vars.map(_.name), msw_mask)

      m_subst += original -> converted
      // sanity check
      assert(converted.mask.nonEmpty || orig_def.variable.width % 16 == 0)
      converted

    } ensuring (r => r.parts.length > 0)

    /** get the conversion of a given name of a wire/reg
      *
      * @param old_name
      * @return
      */
    def getConversion(old_name: Name): ConvertedWire =
      m_subst.getOrElseUpdate(old_name, convertWire(old_name))

    def originalWidth(original_name: Name): Int = m_old_defs(
      original_name
    ).variable.width

    def originalDef(original_name: Name): DefReg = m_old_defs(original_name)

  }

  def convertBinaryArithmetic(
      instruction: BinaryArithmetic
  )(implicit
      ctx: AssemblyContext,
      builder: ConversionBuilder
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
    def maskRd(rd_mutable: Name, rd_mask: Option[Name]): Seq[Instruction] = {

      rd_mask match {
        case Some(const_mask) =>
          Seq(
            instruction.copy(
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

    def moveRegs(dest: Seq[Name], source: Seq[Name]): Seq[Instruction] = {

      require(
        dest.length == source.length,
        "Can not move unaligned register arrays, this is an internal error"
      )

      dest zip source map { case (d, s) =>
        instruction.copy(
          operator = BinaryOperator.ADD,
          rd = d,
          rs1 = s,
          rs2 = builder.mkConstant(0)
        )
      }
    }

    def assertAligned(
        rd_array: Seq[Name],
        rs1_array: Seq[Name],
        rs2_array: Seq[Name]
    ): Unit =
      if (
        rd_array.length != rs1_array.length || rd_array.length != rs2_array.length || rs2_array.length != rs1_array.length
      ) {
        logger.error("Unaligned operands!", instruction)
      }
    def assert16Bit(
        uint16_array: Seq[Name]
    )(msg: => String): Unit = if (uint16_array.length != 1) {
      logger.error(msg, instruction)
    }

    instruction.operator match {
      case BinaryOperator.ADD =>
        val ConvertedWire(rd_uint16_array, rd_mask) =
          builder.getConversion(instruction.rd)
        val rs1_uint16_array: Seq[Name] =
          builder.getConversion(instruction.rs1).parts
        val rs2_uint16_array: Seq[Name] =
          builder.getConversion(instruction.rs2).parts

        assertAligned(rd_uint16_array, rs1_uint16_array, rs2_uint16_array)

        val rd_uint16_array_mutable = rd_uint16_array.map { x =>
          builder.mkWire(x, 16)
        }
        if (rs1_uint16_array.size != 1) {
          val carry: Name = builder.mkWire("add_carry", 16)
          // set the carry-in to be zero for the first partial sum
          inst_q += BinaryArithmetic(
            operator = BinaryOperator.ADD,
            rd = carry,
            rs1 = builder.mkConstant(0),
            rs2 = builder.mkConstant(0)
          )
          val num_loops = rs1_uint16_array.length max rs2_uint16_array.length
          rs1_uint16_array zip
            rs1_uint16_array zip
            rd_uint16_array_mutable foreach { case ((rs1_16, rs2_16), rd_16) =>
              inst_q += AddC(
                rd = rd_16,
                co = carry,
                rs1 = rs1_16,
                rs2 = rs2_16,
                ci = carry
              )
            }

        } else {
          inst_q += instruction
            .copy(
              rd = rd_uint16_array_mutable.head,
              rs1 = rs1_uint16_array.head,
              rs2 = rs2_uint16_array.head
            )

        }

        inst_q ++= maskRd(rd_uint16_array_mutable.last, rd_mask)
        inst_q ++= moveRegs(rd_uint16_array, rd_uint16_array_mutable)

      case BinaryOperator.ADDC =>
        logger.error("Unexpected instruction!", instruction)
      case BinaryOperator.SUB =>
        val ConvertedWire(rd_uint16_array, rd_mask) =
          builder.getConversion(instruction.rd)
        val rs1_uint16_array = builder.getConversion(instruction.rs1).parts
        val rs2_uint16_array = builder.getConversion(instruction.rs2).parts
        assertAligned(rd_uint16_array, rs1_uint16_array, rs2_uint16_array)
        val rd_uint16_array_mutable = rd_uint16_array map {
          builder.mkWire(_, 16)
        }
        if (rs1_uint16_array.length != 1) {

          // we handle SUB with carry by NOTing the second operand and AddCing
          // them. However, unlike the ADD conversion, we initialize the first
          // carry to 1
          val carry = builder.mkWire("carry", 16)
          val rs2_16_neg = builder.mkWire(instruction.rs2 + "_neg", 16)
          val const_0xFFFF = builder.mkConstant(0xffff)

          // set the initial carry to 1
          inst_q += BinaryArithmetic(
            BinaryOperator.ADD,
            carry,
            builder.mkConstant(0),
            builder.mkConstant(1)
          )
          rs1_uint16_array zip rs2_uint16_array zip rd_uint16_array_mutable foreach {
            case ((rs1_16, rs2_16), rd_16) =>
              inst_q += BinaryArithmetic(
                BinaryOperator.XOR,
                rs2_16_neg,
                rs2_16,
                const_0xFFFF
              )
              inst_q += AddC(
                co = carry,
                rd = rd_16,
                rs1 = rs1_16,
                rs2 = rs2_16_neg,
                ci = carry
              )
          }

        } else {
          inst_q += instruction.copy(
            rd = rd_uint16_array_mutable.head,
            rs1 = rs1_uint16_array.head,
            rs2 = rs2_uint16_array.head
          )
        }

        inst_q ++= maskRd(rd_uint16_array_mutable.last, rd_mask)
        inst_q ++ moveRegs(rd_uint16_array, rd_uint16_array_mutable)

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
        inst_q ++= maskRd(rd_uint16_array_mutable.last, mask)
        inst_q ++= moveRegs(rd_uint16_array, rd_uint16_array_mutable)

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
          logger.error("Expected boolean wire in SEQ")
        }
        val seq_sub_res = builder.mkWire("seq_sub_res", 16)
        val seq_add_res = builder.mkWire("seq_add_res", 16)
        if (rs1_uint16_array.size >= (1 << 16)) {
          logger.error("SEQ is too wide!", instruction)
        }
        rs1_uint16_array zip rs2_uint16_array foreach { case (rs1_16, rs2_16) =>
          inst_q += BinaryArithmetic(
            BinaryOperator.SUB,
            seq_sub_res,
            rs1_16,
            rs2_16
          )
          inst_q += BinaryArithmetic(
            BinaryOperator.ADD,
            seq_add_res,
            seq_add_res,
            seq_sub_res
          )
        }
        val ConvertedWire(rd_uint16, _) =
          builder.getConversion(instruction.rd)
        assert(rd_uint16.size == 1)

        inst_q += BinaryArithmetic(
          BinaryOperator.SEQ,
          rd_uint16.head,
          seq_add_res,
          builder.mkConstant(0)
        )
      // no need to mask the result since the hardware implementation
      // can only produce a single bit anyways.
      case BinaryOperator.SLL =>
        val shift_amount = instruction.rs2
        val rs = instruction.rs1
        val rd = instruction.rd
        val ConvertedWire(shift_uint16, shift_amount_mask) =
          builder.getConversion(shift_amount)

        assert16Bit(shift_uint16) {
          "Shift amount is too large, can only support shifting up to 16-bit " +
            "number as the shift amount, are you sure your design is correct?"
        }

        // we don't keep the mask for rs, because the producer should have taken
        // care of ANDing the most significant short word.
        val ConvertedWire(rs_uint16_array, _) = builder.getConversion(rs)

        val ConvertedWire(rd_uint16_array, rd_mask) = builder.getConversion(rd)

        val rd_uint16_array_mutable = rd_uint16_array.map { x =>
          builder.mkWire(x, 16)
        }

        if (rd_uint16_array_mutable.length == 1) {

          if (builder.originalWidth(shift_amount) > 4) {
            logger.error("Unsafe SLL", instruction)
          }
          // easy case
          inst_q += instruction.copy(
            rd = rd_uint16_array_mutable.head,
            rs1 = rs_uint16_array.head,
            rs2 = shift_uint16.head
          )

        } else {
          val shift_amount_mutable = builder.mkWire(
            shift_amount + "_mutable",
            16
          )

          val const_0 = builder.mkConstant(0)
          val const_1 = builder.mkConstant(1)
          val const_16 = builder.mkConstant(16)

          val shift_amount_mutable_eq_0 =
            builder.mkWire("shift_amount_mutable_eq_0", 1)
          val sixteen_minus_shift_amount_mutable =
            builder.mkWire("sixteen_minus_" + shift_amount_mutable, 16)
          val fifteen_minus_shift_amount_mutable =
            builder.mkWire("fifteen_minus_" + shift_amount_mutable, 16)
          val shift_amount_mutable_gt_eq_16 =
            builder.mkWire(shift_amount_mutable + "gt_eq_16", 1)
          val shift_amount_mutable_minus_16 =
            builder.mkWire(shift_amount_mutable + "_minus_16", 16)
          val shift_right_amount_mutable =
            builder.mkWire(shift_amount + "_right_mutable", 16)
          val right_shifted = builder.mkWire("right_shifted", 16)
          val carry_in = builder.mkWire("carry_in", 16)
          val carry_out = builder.mkWire("carry_out", 16)
          val tmp_res = builder.mkWire("tmp_res", 16)
          val tmp_tmp_res = builder.mkWire("tmp_tmp_res", 16)
          val tmp_tmp_tmp_res = builder.mkWire("tmp_tmp_tmp_res", 16)
          // initialize the rd mutable array to the values of the rs

          assert(rs_uint16_array.size == rd_uint16_array.size)
          inst_q ++= moveRegs(rd_uint16_array_mutable, rs_uint16_array)

          // initialize the mutable shift amount to the original value
          inst_q +=
            BinaryArithmetic(
              BinaryOperator.ADD,
              shift_amount_mutable,
              shift_uint16.head,
              const_0
            )
          for (ix <- 0 until rs_uint16_array.length) {
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
                shift_right_amount_mutable,
                const_16
              )
            )
            for (jx <- 0 until rs_uint16_array.length) {

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
                  tmp_res,
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
        inst_q ++= maskRd(rd_uint16_array_mutable.last, rd_mask)
        inst_q ++= moveRegs(rd_uint16_array, rd_uint16_array_mutable)
      case BinaryOperator.SRA =>
        val shift_amount = instruction.rs2
        val rd = instruction.rd
        val rs1 = instruction.rs1

        val ConvertedWire(shift_uint16, _) = builder.getConversion(shift_amount)
        assert16Bit(shift_uint16) {
          "Shift amount is too large, can only support shifting up to 16-bit " +
            "number as the shift amount, are you sure your design is correct?"
        }

        val ConvertedWire(rd_uint16_array, rd_mask) = builder.getConversion(rd)
        val rd_uint16_array_mutable = rd_uint16_array map {
          builder.mkWire(_, 16)
        }

        val ConvertedWire(rs1_uint16_array, _) = builder.getConversion(rs1)
        val rs_width = builder.originalWidth(rs1)

        if (rd_uint16_array_mutable.length == 1) {

          if (builder.originalWidth(shift_amount) > 4) {
            logger.error("unsafe SRA", instruction)
          }
          // easy case
          inst_q += instruction.copy(
            rd = rd_uint16_array_mutable.head,
            rs1 = rs1_uint16_array.head,
            rs2 = shift_uint16.head
          )

        } else {

          moveRegs(rd_uint16_array_mutable, rs1_uint16_array)

          val mutable_sh =
            builder.mkWire("mutable_sh", builder.originalWidth(shift_amount))

          val msbits =
            rs_width % 16 // number of valid bits in the most significant short word
          // we need to sign extend the most significant word if necessary
          val sign_bit = builder.mkWire("sign", 1)
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
              builder.mkConstant((1 << 16) - 1)
            )
            inst_q += BinaryArithmetic(
              BinaryOperator.OR,
              rd_uint16_array_mutable.last,
              rd_uint16_array_mutable.last,
              sext_mask
            )
          }

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
            val sign_replicated = builder.mkWire("sign_replicated", 16)
            inst_q += Mux(
              sign_replicated,
              sign_bit,
              builder.mkConstant(0),
              builder.mkConstant((1 << 16) - 1)
            )
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
              mutable_sh
            )
          }
        }
        inst_q ++= maskRd(rd_uint16_array_mutable.last, rd_mask)
        inst_q ++= moveRegs(rd_uint16_array, rd_uint16_array_mutable)

      case BinaryOperator.SRL =>
        val ConvertedWire(shift_uint16, _) =
          builder.getConversion(instruction.rs2)
        val ConvertedWire(rd_uint16_array, rd_mask) =
          builder.getConversion(instruction.rd)
        val rd_uint16_array_mutable = rd_uint16_array map {
          builder.mkWire(_, 16)
        }

        val ConvertedWire(rs1_uint16_array, _) =
          builder.getConversion(instruction.rs1)

        if (rd_uint16_array_mutable.length == 1) {

          if (builder.originalWidth(instruction.rs2) > 4) {
            logger.error("Unsafe SRL", instruction)
          }
          inst_q += instruction.copy(
            rd = rd_uint16_array_mutable.head,
            rs1 = rs1_uint16_array.head,
            rs2 = shift_uint16.head
          )
        } else {

          // initialize the rd mutable array
          moveRegs(rd_uint16_array_mutable, rs1_uint16_array)

          assert16Bit(shift_uint16) {
            "Shift amount is too large, can only support shifting up to 16-bit " +
              "number as the shift amount, are you sure your design is correct?"
          }

          val mutable_sh =
            builder.mkWire("mutable_sh", builder.originalWidth(instruction.rs2))

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
            inst_q ++= Seq(
              BinaryArithmetic(
                BinaryOperator.SEQ,
                mutable_sh_eq_0,
                mutable_sh,
                builder.mkConstant(0)
              ),
              Mux(
                left_shift_amount,
                mutable_sh_eq_0,
                sixteen_minus_shift_amount,
                builder.mkConstant(0)
              )
            )
            val new_res = builder.mkWire("new_res", 16)
            val rd_left_shifted = builder.mkWire("rd_left_shifted", 16)
            val carry_out = builder.mkWire("carry_out", 16)
            val rd_right_shifted = builder.mkWire("rd_right_shifted", 16)
            val local_res = builder.mkWire("local_wire", 16)
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
              mutable_sh
            )

          }
        }
        inst_q ++= maskRd(rd_uint16_array_mutable.last, rd_mask)
        inst_q ++= moveRegs(rd_uint16_array, rd_uint16_array_mutable)

      case BinaryOperator.SLTS =>
        // we should only handle SLTS rd, rs1, const_0, because this is the
        // only thing the Thyrio frontend gives us. This is rather easy and
        // cheap, we only need to convert rs1 into an array and then check
        // the most significant half-word of rs1 to be less than zero!
        def assumptions(): Unit = {
          val rs2_const = builder.originalDef(instruction.rs2)
          if (rs2_const.variable.varType != ConstType) {
            logger.error(
              "Second SLTS operand should be constant 0!",
              instruction
            )
          } else {
            rs2_const.value match {
              case Some(x) if x == 0 => // nothing
              case _ =>
                logger.error(
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
          logger.warn("Expected Boolean result in SLTS", instruction)
        }
        val rd_uint16 = builder.getConversion(instruction.rd).parts.head
        val rd_uint16_mutable = builder.mkWire(rd_uint16, 16)
        // number of used bits in the most significant short word
        val rs1_ms_half_word_bits = builder.originalWidth(instruction.rs1) % 16

        if (rs1_ms_half_word_bits == 0) {
          // the first operand is 16-bit aligned, so we can simply perform
          // SLTS on the most significant short-word
          inst_q += instruction.copy(
            rd = rd_uint16_mutable,
            rs1 = rs1_uint16_array.last,
            rs2 = builder.mkConstant(0)
          )
        } else {
          // the first operand is not 16-bit aligned
          // now we need to shift the most significant bit of the rs1_uint16_array.last
          // to the right and bring it to bit position zero, then we can just use
          // a SEQ to check the sign bit.
          inst_q ++= Seq(
            BinaryArithmetic(
              BinaryOperator.SRL,
              rd_uint16_mutable,
              rs1_uint16_array.last,
              builder.mkConstant(rs1_ms_half_word_bits - 1)
            ),
            BinaryArithmetic(
              BinaryOperator.SEQ,
              rd_uint16,
              rd_uint16_mutable,
              builder.mkConstant(1)
            )
          )
        }

      case BinaryOperator.PMUX =>
        logger.error("Unexpected instruction!", instruction)

    }
    // set the position
    inst_q.foreach(_.setPos(instruction.pos))

    inst_q.toSeq
  }

  def convert(instruction: Instruction)(implicit
      ctx: AssemblyContext,
      builder: ConversionBuilder
  ): Seq[Instruction] = instruction match {
    case i: BinaryArithmetic => convertBinaryArithmetic(i)
    case i @ LocalLoad(rd, base, offset, _) =>
      val ConvertedWire(rd_uint16_array, rd_mask) = builder.getConversion(rd)
      val ConvertedWire(base_uint16_array, _) = builder.getConversion(base)
      rd_uint16_array.zip(base_uint16_array).zipWithIndex.map {
        case ((rd_16: Name, base_16: Name), ix: Int) =>
          i.copy(rd_16, base_16, if (ix == 0) offset else BigInt(0))
            .setPos(i.pos)
      } ++ rd_mask.map { case m => // Not sure if masking is necessary
        BinaryArithmetic(
          BinaryOperator.AND,
          rd_uint16_array.last,
          rd_uint16_array.last,
          m
        ).setPos(i.pos)
      }.toSeq
    case i @ LocalStore(rs, base, offset, predicate, _) =>
      val ConvertedWire(rs_uint16_array, _) = builder.getConversion(rs)
      val ConvertedWire(base_uint16_array, _) = builder.getConversion(base)
      rs_uint16_array.zip(base_uint16_array).zipWithIndex.map {
        case ((rs_16: Name, base_16: Name), ix: Int) =>
          i.copy(rs_16, base_16, if (ix == 0) offset else BigInt(0))
            .setPos(i.pos)
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
        logger.error("sel should be single bit!", i)
      }
      val rfalse_uint16_array = builder.getConversion(rfalse).parts
      val rtrue_uint16_array = builder.getConversion(rtrue).parts
      rd_uint16_array zip (sel_uint16_array zip (rfalse_uint16_array zip rtrue_uint16_array)) map {
        case (rd16, (sel16, (rfalse16, rtrue16))) =>
          i.copy(rd16, sel16, rfalse16, rtrue16).setPos(i.pos)
      } // don't need to mask the results though, rfalse and rtrue are masked
    // where they are produced and MUX cannot overflow them
    case i: AddC =>
      logger.error("AddC can only be inserted by the compiler!", i)
      Seq.empty[Instruction]
    case Nop =>
      logger.error("Nops can only be inserted by the compiler!")
      Seq.empty[Instruction]
    case i @ PadZero(rd, rs, w, annons) =>
      val rd_w = builder.originalWidth(rd)
      if (rd_w != w) {
        logger.error("Invalid padding width!", i)
      }
      val rd_uint16_array = builder.getConversion(rd).parts
      val rs_uint16_array = builder.getConversion(rs).parts

      rd_uint16_array.zip(
        rs_uint16_array ++ Seq.fill(
          rd_uint16_array.length - rs_uint16_array.length
        )(builder.mkConstant(0))
      ) map { case (rd_16, rs_16) =>
        BinaryArithmetic(
          BinaryOperator.ADD,
          rd_16,
          rs_16,
          builder.mkConstant(0),
          annons
        ).setPos(i.pos)
      }

    case _ =>
      logger.error("Can not handle this type of instruction yet", instruction)
      Seq.empty[Instruction]

  }

  def convert(proc: DefProcess)(implicit ctx: AssemblyContext): DefProcess = {

    implicit val builder = new ConversionBuilder(proc)

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


