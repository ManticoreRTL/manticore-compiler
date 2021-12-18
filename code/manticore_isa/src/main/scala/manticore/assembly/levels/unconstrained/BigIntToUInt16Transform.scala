package manticore.assembly.levels.unconstrained

import manticore.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import manticore.assembly.BinaryOperator
import manticore.assembly.levels.ConstType
import manticore.assembly.levels.WireType

object BigIntToUInt16Transform
    extends AssemblyTransformer(UnconstrainedIR, UnconstrainedIR) {

  import UnconstrainedIR._

  private case class ConvertedWire(parts: Seq[Name], mask: Option[Name])
  private class ConversionBuilder(proc: DefProcess) {

    private var m_name_id = 0
    private val m_wires = scala.collection.mutable.Queue.empty[DefReg]
    private val m_constants =
      scala.collection.mutable.HashMap.empty[Int, DefReg]
    private val m_subst =
      scala.collection.mutable.HashMap.empty[Name, ConvertedWire]
    private val m_old_defs = proc.registers.map { r =>
      r.variable.name -> r
    }.toMap

    /** Create a unique name
      *
      * @param suggestion
      * @return
      */
    def freshName(suggestion: Name): Name = {
      val n = s"%${m_name_id}%${suggestion.toString()}"
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

      val conv_def = uint16_vars zip values map { case (cvar, cval) =>
        orig_def.copy(variable = cvar, value = cval).setPos(orig_def.pos)
      }

      m_wires ++= conv_def
      val converted =
        ConvertedWire(uint16_vars.map(_.name), msw_mask)

      m_subst += original -> converted
      converted

    }

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

  def convert(
      instruction: BinaryArithmetic
  )(implicit ctx: AssemblyContext, builder: ConversionBuilder) {

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

    instruction.operator match {
      case BinaryOperator.ADD =>

      case BinaryOperator.ADDC =>
        logger.error("Unexpected instruction!")
      case BinaryOperator.SUB =>
      case op @ (BinaryOperator.OR | BinaryOperator.AND | BinaryOperator.XOR) =>
        val ConvertedWire(rd_uint16_array, mask) =
          builder.getConversion(instruction.rd)
        val rs1_uint16_array = builder.getConversion(instruction.rs1).parts
        val rs2_uint16_array = builder.getConversion(instruction.rs2).parts

        assert(
          rs1_uint16_array.size == rs2_uint16_array.size && rs1_uint16_array.size == rd_uint16_array.size,
          "size miss match"
        )
        rd_uint16_array zip (rs1_uint16_array zip rs2_uint16_array) foreach {
          case (rd_16, (rs1_16, rs2_16)) =>
            inst_q += instruction
              .copy(
                rd = rd_16,
                rs1 = rs1_16,
                rs2 = rs2_16
              )
              .setPos(instruction.pos)
        }
        mask match {
          case Some(const_mask) =>
            inst_q += BinaryArithmetic(
              BinaryOperator.AND,
              rd_uint16_array.last,
              rd_uint16_array.last,
              const_mask
            )
          case None => // no masking needed
        }
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
        if (shift_uint16.length != 1) {
          logger.error(
            "Shift amount is too large, can only support shifting up to 16-bit " +
              "number as the shift amount, are you sure your design is correct?",
            instruction
          )
        }

        // we don't keep the mask for rs, because the producer should have taken
        // care of ANDing the most significant short word.
        val ConvertedWire(rs_uint16_array, _) = builder.getConversion(rs)

        val shift_amount_mutable = builder.freshName(shift_amount + "_mutable")

        val ConvertedWire(rd_uint16_array, rd_mask) = builder.getConversion(rd)

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
        // initialize the rd array to the values of the rs

        assert(rs_uint16_array.size == rd_uint16_array.size)
        inst_q ++=
          rd_uint16_array zip rs_uint16_array map { case (rd16, rs16) =>
            BinaryArithmetic(BinaryOperator.ADD, rd16, rs16, const_0)
          }
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
                rd_uint16_array(jx),
                shift_right_amount_mutable
              ),
              BinaryArithmetic(
                BinaryOperator.SLL,
                tmp_res,
                rd_uint16_array(jx),
                shift_amount_mutable
              ),
              Mux(
                carry_out,
                shift_amount_mutable_gt_eq_16,
                right_shifted,
                rd_uint16_array(jx)
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
                rd_uint16_array(jx),
                shift_amount_mutable_eq_0,
                tmp_tmp_tmp_res,
                rd_uint16_array(jx)
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

        rd_mask match {
          case Some(mask_const) =>
            inst_q +=
              BinaryArithmetic(
                BinaryOperator.AND,
                rd_uint16_array.last,
                rd_uint16_array.last,
                mask_const
              )
          case _ => // do nothing
        }
      case BinaryOperator.SRL  =>
      case BinaryOperator.SRA  =>
      case BinaryOperator.SLTS =>
        // we should only handle SLTS rd, rs1, const_0, because this is the
        // only thing the Thyrio frontend gives us. This is rather easy and
        // cheap, we only need to convert rs1 into an array and then check
        // the most significant half-word of rs1 to be less than zero!
        def assumptions(): Unit = {
          val rs2_const = builder.originalDef(instruction.rs1)
          if (rs2_const.variable.varType != ConstType) {
            logger.error("Second SLTS operand should be constant 0!", instruction)
          } else {
            rs2_const.value match {
              case Some(x) if x == 0 => // nothing
              case _ =>
                logger.error("Second SLTS operand should be constant 0!", instruction)
            }
          }
        }
        assumptions()

        val ConvertedWire(rs1_uint16_array, rs1_mask) = builder.getConversion(instruction.rs1)
        val orig_rd_width = builder.originalWidth(instruction.rd)
        if (orig_rd_width == 1) {
          logger.error("Expected Boolean result in SLTS", instruction)
        }
        val rd_uint16 = builder.getConversion(instruction.rd).parts.head

        // number of used bits in the most significant short word
        val rs1_ms_half_word_bits = rs1_mask match {
          case Some(value) => BigInt(value).bitLength
          case None => 16
        }

         // now we need to shift the most significant bit of the rs1_uint16_array.last
        // to the right and bring it to bit position zero, then we can just use
        // a SEQ to check the sign bit, in fact, we no longer need


        // ATTENTION: We can not use SLTS on the most significant half-word simply
        // because the most significant half-word might be partial, i.e., have
        // less than actual 16 bits. So a negative number appears as positive
        // to the SLTS instruction because it only understands 16-bit 2's compl.
        inst_q ++= Seq(
          BinaryArithmetic(
            BinaryOperator.SRL,
            rd_uint16,
            rs1_uint16_array.last,
            builder.mkConstant(rs1_ms_half_word_bits - 1)
          ),
          BinaryArithmetic(
            BinaryOperator.SEQ,
            rd_uint16,
            rd_uint16,
            builder.mkConstant(1)
          )
        )

      case BinaryOperator.PMUX =>
        logger.error("Unexpected instruction!")

    }

    inst_q.toSeq
  }
  def convert(instruction: Instruction)(implicit
      ctx: AssemblyContext,
      builder: ConversionBuilder
  ): Seq[Instruction] = {

    logger.error("Not implemented!")
    Seq()

  }
  def convert(proc: DefProcess)(implicit ctx: AssemblyContext): DefProcess = {

    implicit val builder = new ConversionBuilder(proc)

    val insts: Seq[Instruction] = proc.body.flatMap { convert(_) }

    println(insts map { _.serialized } mkString ("\n"))
    ???
  }
  override def transform(
      source: DefProgram,
      context: AssemblyContext
  ): DefProgram = {

    source.copy(processes = source.processes.map { p => convert(p)(context) })
    ???
  }

}
