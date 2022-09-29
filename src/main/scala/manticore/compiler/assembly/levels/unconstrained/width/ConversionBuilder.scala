package manticore.compiler.assembly.levels.unconstrained.width

import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.assembly.levels.ConstType
import manticore.compiler.assembly.levels.WireType
import manticore.compiler.assembly.levels.MemoryType
import manticore.compiler.assembly.annotations.DebugSymbol
import manticore.compiler.assembly.annotations.AssemblyAnnotationFields
import manticore.compiler.assembly.annotations.{Reg => RegAnnotation}

import manticore.compiler.assembly.levels.Flavored
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.VariableType
import manticore.compiler.assembly.annotations.MemInit
import manticore.compiler.assembly.annotations.Memblock

/** A helper trait as the base of WidthConversion. It contains the
  * implementation of a mutable Builder class that lazily converts every
  * register in a process to a sequence of converted wires. The
  * WidthConversion]should instantiate this class and call the
  * getConversion method to get the converted sequence of registers as it
  * moves through the instructions. Note that the instructions are assumed to be
  * ordered properly, i.e., registers should be written and then read if they
  * are not constants.
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */

trait ConversionBuilder extends Flavored {

  val flavor = UnconstrainedIR
  import UnconstrainedIR._

  // The mask is that of the most significant word.
  protected case class ConvertedWire(parts: Seq[Name], mask: Option[Name])

  protected class Builder(private val proc: DefProcess)(implicit
      val ctx: AssemblyContext
  ) {

    private var m_name_id       = 0
    private var m_syscall_order = 0
    private var m_serial_queue  = scala.collection.mutable.Queue.empty[Seq[Name]]
    // private val m_wires = scala.collection.mutable.Queue.empty[DefReg]
    private val m_wires      = scala.collection.mutable.Map.empty[Name, DefReg]

    private var m_carry_zero = Option.empty[DefReg]
    private var m_carry_one  = Option.empty[DefReg]

    private val m_constants =
      scala.collection.mutable.Map.empty[Int, DefReg]
    private val m_subst =
      scala.collection.mutable.HashMap.empty[Name, ConvertedWire]
    private val m_const_subst =
      scala.collection.mutable.Map.empty[Name, ConvertedWire]
    private val m_old_defs = proc.registers.map { r =>
      r.variable.name -> r
    }.toMap

    private var m_gmem_user_base = ctx.hw_config.userGlobalMemoryBase

    private val m_gmem_allocations = scala.collection.mutable.Map.empty[Name, DefGlobalMemory]

    // unlike the local memories, we do allocate the global memory bases right
    // here in the width conversion pass. They are all going to be located
    // on the same core, so there is no point delaying the allocation
    def mkGlobalMemory(mem: Name): DefGlobalMemory =
      m_gmem_allocations.get(mem) match {
        case None =>
          // do the allocation
          val origMemDef    = originalDef(mem)
          val origMemVar    = origMemDef.variable.asInstanceOf[MemoryVariable]
          val shortsPerWord = (origMemVar.width - 1) / 16 + 1
          val sizeInShorts  = origMemVar.size * shortsPerWord

          val nextUserBase = m_gmem_user_base + sizeInShorts
          assert(nextUserBase >= (1 << 40), "GlobalMemory too large!")

          val newGlobalMem = DefGlobalMemory(
            memory = mem,
            base = nextUserBase,
            size = sizeInShorts,
            content = origMemDef.annons
              .collectFirst { case x: MemInit =>
                x.readFile()
                  .flatMap { bigWord =>
                    Seq.tabulate(shortsPerWord) { index =>
                      (bigWord >> (index * 16)) & 0xffff
                    }
                  }
                  .toIndexedSeq
              // note the difference between handling of initial values in
              // global and local memory. Here we do not need to transpose
              // the initial values because we don't split the memory into
              // multiple smaller ones, rather we just reorganize into
              // 16-bit shorts

              }
              .getOrElse(IndexedSeq.empty[Constant])
          ).setPos(origMemDef.pos)

          // update the state
          m_gmem_user_base = nextUserBase
          m_gmem_allocations += (mem -> newGlobalMem)
          newGlobalMem
        case Some(gmemDef) => gmemDef
      }

    def putSerial(ns: Seq[Name]): Seq[Int] = {
      val res = m_syscall_order
      m_serial_queue += ns
      m_syscall_order += ns.length
      ns.indices.map(_ + res)
    }

    def flushSerial(): Seq[Seq[Name]] = {
      m_serial_queue.dequeueAll(_ => true)
    }

    def nextOrder(): Int = {
      val res = m_syscall_order
      m_syscall_order += 1
      res
    }

    /** Build the converted process from the given sequence of instructions
      *
      * @param instructions
      * @return
      */
    def buildFrom(instructions: Seq[Instruction]): DefProcess = {

      val preamble =
        m_carry_zero.map { r => ClearCarry(r.variable.name) }.toSeq ++
          m_carry_one.map { r => SetCarry(r.variable.name) }.toSeq
      val const_carries =
        m_carry_zero.toSeq ++ m_carry_one.toSeq
      proc
        .copy(
          registers =
            (m_wires.values ++ m_constants.values ++ const_carries).toSeq.distinct,
          body = preamble ++ instructions,
          labels = proc.labels.map { lgrp =>
            lgrp.copy(memory = getConversion(lgrp.memory).parts.head)
          },
          globalMemories = m_gmem_allocations.map(_._2).toSeq
        )
        .setPos(proc.pos)
    }

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
      if (value < 0) {

        require(value > 0, "Can not make negative constant!")
      }
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

    def mkCarry0(): Name = {
      if (m_carry_zero.isEmpty) {
        val carry = DefReg(
          LogicVariable(freshName(s"carry0"), 1, WireType)
        )
        m_carry_zero = Some(carry)
      }
      m_carry_zero.get.variable.name
    }

    def mkCarry1(): Name = {
      if (m_carry_one.isEmpty) {
        val carry = DefReg(
          LogicVariable(freshName(s"carry0"), 1, WireType)
        )
        m_carry_one = Some(carry)
      }
      m_carry_one.get.variable.name
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

      val width      = orig_def.variable.width
      val array_size = (width - 1) / 16 + 1

      def convertConstToArray(big_val: BigInt): Seq[BigInt] = {
        val max_val = (BigInt(1) << width) - 1
        if (big_val > max_val) {
          ctx.logger.error(
            "constant value does not fit in the specified number of bits!",
            orig_def
          )
        }
        Seq.tabulate(array_size) { i =>
          val small_val = (big_val >> (i * 16)) & (0xffff)
          assert(small_val <= 0xffff)
          small_val
        }
      }
      if (isConstant(original)) {
        val values: Seq[Int] = orig_def.value match {
          case None =>
            ctx.logger.error("constant value is not defined!", orig_def)
            Seq.fill(array_size)(0)
          case Some(big_val) =>
            convertConstToArray(big_val).map(_.toInt)
        }
        val constants = values.map { mkConstant }
        // no need to mask the constants, they should be already masked
        val converted = ConvertedWire(constants, None)
        m_const_subst += original -> converted
        converted
      } else {

        assert(array_size >= 1)
        val mask_bits = width - (array_size - 1) * 16
        assert(mask_bits <= 16)
        val msw_mask = if (mask_bits < 16) {
          val mask_const = mkConstant((1 << mask_bits) - 1)
          Some(mask_const)
        } else None

        val uint16_vars =
          if (orig_def.variable.isInstanceOf[MemoryVariable]) {
            createMemoryVariables(
              original,
              array_size,
              orig_def.variable.asInstanceOf[MemoryVariable].size
            )
          } else {
            createLogicVariables(
              original,
              orig_def.variable.varType,
              array_size
            )
          }

        val values: Seq[Option[BigInt]] = orig_def.value match {
          case None => Seq.fill(array_size)(None)
          case Some(big_val) =>
            convertConstToArray(big_val).map { Some(_) }
        }

        val conv_def = (uint16_vars zip values).zipWithIndex.map { case ((cvar, cval), ix) =>
          // check if a debug symbol annotation exits
          val dbgsym = orig_def.annons.collect { case x: DebugSymbol =>
            // if DebugSymbol annotation exits, append the index and width
            // to it
            x.getIndex() match {
              case Some(i) if i != 0 =>
                // we don't want to have a non-zero index, it means this pass
                // was run before?
                ctx.logger.error(
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

          val memblock_annon = orig_def.annons.collect { case x: Memblock =>
            x.withIndex(ix)
          }

          val other_annons = orig_def.annons.filter {
            case _: DebugSymbol   => false
            case _: RegAnnotation => false
            case _: MemInit =>
              false // remove memory init since we store them internally from now on
            case _: Memblock => false
            case _           => true
          }

          orig_def
            .copy(
              variable = cvar,
              value = cval,
              annons = other_annons ++ memblock_annon ++ reg_annon :+ dbgsym
                .withCount(
                  uint16_vars.length
                )
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
      }
    } ensuring (r => r.parts.length > 0)

    def createLogicVariables(oldName: Name, tpe: VariableType, count: Int) = {
      require(tpe != MemoryType)

      Seq.tabulate(count) { i =>
        LogicVariable(
          freshName(s"${oldName}_${i}"),
          16,
          tpe
        )
      }
    }

    private def createMemoryVariables(
        oldName: Name,
        count: Int,
        memorySize: Int
    ) = {

      // get any initial values (if any)
      val initialValues = originalDef(oldName).annons
        .collectFirst { case mi: MemInit => mi }
        .map { mi =>
          mi.readFile()
            .map { bigWord =>
              Seq.tabulate(count) { index =>
                val shifted = bigWord >> (index * 16)
                val masked  = shifted & 0xffff
                masked
              }
            }
            .toSeq
            .transpose
        }
        .getOrElse(Seq.empty)
      Seq.tabulate(count) { i =>
        MemoryVariable(
          name = freshName(s"${oldName}_${i}"),
          width = 16,
          size = memorySize,
          content = if (initialValues.nonEmpty) initialValues(i) else Seq.empty
        )
      }
    }

    /** get the conversion of a given name of a wire/reg
      *
      * @param old_name
      * @return
      */
    def getConversion(old_name: Name): ConvertedWire =
      if (isConstant(old_name)) {
        m_const_subst.getOrElse(old_name, convertWire(old_name))
      } else {
        m_subst.getOrElseUpdate(old_name, convertWire(old_name))
      }

    def originalWidth(original_name: Name): Int = m_old_defs(
      original_name
    ).variable.width

    def originalDef(original_name: Name): DefReg = m_old_defs(original_name)

    def isConstant(original_name: Name): Boolean =
      m_old_defs(original_name).variable.varType == ConstType
    def isMemory(original: Name): Boolean =
      m_old_defs(original).variable.varType == MemoryType
  }
}
