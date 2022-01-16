package manticore.assembly.levels.unconstrained.width

import manticore.assembly.levels.unconstrained.UnconstrainedIR
import manticore.assembly.levels.ConstType
import manticore.assembly.annotations.DebugSymbol
import manticore.assembly.annotations.AssemblyAnnotationFields
import manticore.assembly.annotations.{Reg => RegAnnotation}

import manticore.assembly.levels.Flavored
import manticore.assembly.levels.WireType

trait ConversionBuilder extends Flavored {

  val flavor = UnconstrainedIR
  import flavor._

  protected case class ConvertedWire(parts: Seq[Name], mask: Option[Name])

  protected class Builder(private val proc: DefProcess) {

    private var m_name_id = 0
    // private val m_wires = scala.collection.mutable.Queue.empty[DefReg]
    private val m_wires = scala.collection.mutable.Map.empty[Name, DefReg]
    private val m_constants =
      scala.collection.mutable.Map.empty[Int, DefReg]
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
          registers = (m_wires.values ++ m_constants.values).toSeq.distinct,
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
      val wire_name = freshName(suggestion)
      val new_wire =
        DefReg(LogicVariable(freshName(suggestion), width, WireType), None)
      m_wires += wire_name -> new_wire
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
      require(
        m_old_defs.contains(original),
        s"can not convert undefined wire ${original}"
      )
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
          // if (i == array_size - 1) mask_bits else 16,
          16, // we make every variable 16 bit and mask the computation results
          // explicitly if necessary
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
          // check if a debug symbol annotation exits
          val dbgsym = orig_def.annons.collect { case x: DebugSymbol =>
            // if DebugSymbol annotation exits, append the index and width
            // to it
            x.getIndex() match {
              case Some(i) if i != 0 =>
                // we don't want to have a non-zero index, it means this pass
                // was run before?
                logger.error(
                  "Did not expect non-zero debug symbol index",
                  orig_def
                )
              case _ =>
              // do nothing
            }

            val with_index = x.withIndex(ix).withGenerated(false)
            with_index.getIntValue(AssemblyAnnotationFields.Width) match {
              case Some(w) =>
                with_index
              case None =>
                with_index.withWidth(width)
            }
          } match {
            case Seq() => // if it does not exists, create one from scratch
              DebugSymbol(orig_def.variable.name)
                .withIndex(ix)
                .withWidth(width)
                .withGenerated(true)
            case org +: _ =>
              org // return the original one with appended index and width
          }

          // also append an index the reg annotation so that later passes
          // could creating mapping from current to next registers
          val reg_annon = orig_def.annons.collect { case x: RegAnnotation =>
            x.withIndex(ix)
          }.toSeq

          val other_annons = orig_def.annons.filter {
            case _: DebugSymbol   => false
            case _: RegAnnotation => false
            case _                => true
          }

          orig_def
            .copy(
              variable = cvar,
              value = cval,
              annons = other_annons ++ reg_annon :+ dbgsym
            )
            .setPos(orig_def.pos)

      }

      m_wires ++= conv_def.map { r => r.variable.name -> r }

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

    def isConst(original_name: Name): Boolean =
      m_old_defs(original_name).variable.varType == ConstType
  }
}
