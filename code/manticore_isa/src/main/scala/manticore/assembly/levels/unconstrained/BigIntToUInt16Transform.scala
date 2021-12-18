package manticore.assembly.levels.unconstrained

import manticore.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import manticore.assembly.BinaryOperator

object BigIntToUInt16Transform
    extends AssemblyTransformer(UnconstrainedIR, UnconstrainedIR) {

  import UnconstrainedIR._

  private class ConversionBuilder(proc: DefProcess) {
    var name_id = 0
    val wires = scala.collection.mutable.Queue.empty[DefReg]
    val old_to_new = scala.collection.mutable.HashMap.empty[DefReg, Seq[DefReg]]

    val old_defs = proc.registers.map { r => r.variable.name -> r}.toMap

    def freshName(suggestion: Name): Name = {
      val n = s"%${name_id}%${suggestion.toString()}"
      name_id += 1
      return n
    }

    def width(r: Name): Int = old_defs(r).variable.width
  }
  def convert(instruction: Instruction)(implicit
      ctx: AssemblyContext,
      builder: ConversionBuilder
  ): Seq[Instruction] = {

    instruction match {
      case BinaryArithmetic(BinaryOperator.SLL, rd, rs, shift_amount, annons) =>
        val shift_amount_width = builder.width(shift_amount)
        if (shift_amount_width > 16) {
          logger.error(
            "Shift amount is too large, can only support shifting up to 16-bit " +
              "number as the shift amount, are you sure your design is correct?",
            instruction
          )
        }

        val inst_q = scala.collection.mutable.Queue.empty[Instruction]

        val rs_width: Int = builder.width(rs)
        val uint16_array_size: Int = (rs_width - 1) / 16 + 1
        // we need a set of fresh names to hold rs

        val shift_amount_mutable = builder.freshName(shift_amount + "_mutable")
        val rs_uint16_array = Seq.tabulate(uint16_array_size) { i =>
          builder.freshName(rs + s"%${i}%")
        }
        val rd_uint16_array = Seq.tabulate(uint16_array_size) { i =>
          builder.freshName(s"sll_rd_%${i}%")
        }
        val const_0 = builder.freshName(s"const_0")
        val const_1 = builder.freshName(s"const_0")
        val const_16 = builder.freshName(s"const_16")

        val shift_amount_mutable_eq_0 =
          builder.freshName(shift_amount_mutable + "_eq_0")
        val sixteen_minus_shift_amount_mutable =
          builder.freshName("sixteen_minus_" + shift_amount_mutable)
        val fifteen_minus_shift_amount_mutable =
          builder.freshName("fifteen_minus_" + shift_amount_mutable)
        val shift_amount_mutable_gt_eq_16 =
          builder.freshName(shift_amount_mutable + "gt_eq_16")
        val shift_amount_mutable_minus_16 =
          builder.freshName(shift_amount_mutable + "_minus_16")
        val shift_right_amount_mutable =
          builder.freshName(shift_amount + "_right_mutable")
        val right_shifted = builder.freshName("right_shifted")
        val carry_in = builder.freshName("carry_in")
        val carry_out = builder.freshName("carry_out")
        val tmp_res = builder.freshName("tmp_res")
        val tmp_tmp_res = builder.freshName("tmp_tmp_res")
        val tmp_tmp_tmp_res = builder.freshName("tmp_tmp_tmp_res")
        // initialize the rd array to the values of the rs
        inst_q ++=
          rd_uint16_array zip rs_uint16_array map { case (rd16, rs16) =>
            BinaryArithmetic(BinaryOperator.ADD, rd16, rs16, const_0)
          }

        for (ix <- 0 until uint16_array_size) {
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
          for (jx <- 0 until uint16_array_size) {

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
        val rd_width = builder.width(rd)
        val maskable_bits = uint16_array_size * 16 - rd_width
        if (maskable_bits > 0) {
          val mask = BigInt((1 << 16) - (1 << maskable_bits))
          val const_mask = builder.freshName("mask")
          inst_q +=
          BinaryArithmetic(
            BinaryOperator.AND,
            rd_uint16_array.last,
            rd_uint16_array.last,
            const_mask
          )

        }
        inst_q.toSeq
      case _ => Seq(instruction)
    }

  }
  def convert(proc: DefProcess)(implicit ctx: AssemblyContext): DefProcess = {

    implicit val builder = new ConversionBuilder(proc)

    val insts: Seq[Instruction] = proc.body.flatMap { convert(_) }

    println(insts map { _.serialized } mkString("\n"))
    ???
  }
  override def transform(
      source: DefProgram,
      context: AssemblyContext
  ): DefProgram = {

    source.copy(processes = source.processes.map { p => convert(p)(context) } )
    ???
  }

}
