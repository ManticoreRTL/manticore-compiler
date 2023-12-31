package manticore.compiler.assembly.levels.placed

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.assembly.annotations.AssemblyAnnotation
import manticore.compiler.assembly.annotations.AssemblyAnnotationFields
import manticore.compiler.assembly.annotations.AssemblyAnnotationFields.{Block => BlockField}
import manticore.compiler.assembly.annotations.AssemblyAnnotationFields.{Capacity => CapacityField}
import manticore.compiler.assembly.annotations.AssemblyAnnotationFields.{X => XField}
import manticore.compiler.assembly.annotations.AssemblyAnnotationFields.{Y => YField}
import manticore.compiler.assembly.annotations.MemInit
import manticore.compiler.assembly.annotations.Memblock
import manticore.compiler.assembly.annotations.Trap
import manticore.compiler.assembly.annotations.{Layout => LayoutAnnotation}
import manticore.compiler.assembly.annotations.{Loc => LocAnnotation}
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.AssemblyTranslator
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.assembly.levels.placed.{PlacedIR => T}
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.assembly.levels.unconstrained.{UnconstrainedIR => S}
import manticore.compiler.assembly.{FinishInterrupt, StopInterrupt, SerialInterrupt, AssertionInterrupt}
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import manticore.compiler.assembly.annotations.Sourceinfo


/** Changes UnconstrainedIR flavor to PlacedIR
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */
object UnconstrainedToPlacedTransform
    extends AssemblyTranslator[
      UnconstrainedIR.DefProgram,
      PlacedIR.DefProgram
    ] {

  override def transform(
      asm: S.DefProgram
  )(implicit ctx: AssemblyContext): T.DefProgram = {

    if (isConvertible(asm) == false) {
      ctx.logger.fail(
        s"Can not convert program"
      );
    }

    val converted_processes = if (ctx.use_loc) {
      // probably in debug or test mode, check for @LOC annotations and
      // place the processes accordingly
      val converted_ids: Map[S.ProcessId, T.ProcessId] = asm.processes.map { p: S.DefProcess =>
        val location: Option[LocAnnotation] = p.annons.collectFirst { case l: LocAnnotation =>
          l
        }
        if (location.isEmpty) {
          ctx.logger.fail(
            s"process ${p} is missing a ${LocAnnotation.name} annotation!"
          )
        }
        p.id -> T.ProcessIdImpl(
          p.id,
          location.get.getX(),
          location.get.getY()
        )
      }.toMap

      asm.processes.map(convert(converted_ids, _))

    } else {
      // normal flow, all processes are mapped to (0, 0)
      val converted_ids = asm.processes.map { p =>
        p.id -> T.ProcessIdImpl(p.id, 0, 0)
      }.toMap

      asm.processes.map(convert(converted_ids, _))
    }

    // potentially merge processes that have the same location
    val merged = converted_processes
      .groupBy(p => (p.id.x, p.id.y))
      .map { case ((x, y), ps: Seq[T.DefProcess]) =>
        T.DefProcess(
          registers = ps.flatMap(_.registers),
          body = ps.flatMap(_.body),
          labels = ps.flatMap(_.labels),
          globalMemories = ps.flatMap(_.globalMemories),
          functions = ps.flatMap(_.functions),
          id = T.ProcessIdImpl(s"p_${x}_${y}", x, y),
          annons = ps.flatMap(_.annons)
        ).setPos(ps.head.pos)
      }
      .toSeq

    T.DefProgram(
      processes = merged,
      annons = asm.annons
    ).setPos(asm.pos)

  }

  /** Unchecked conversion of DefProcess
    *
    * @param proc
    *   original process
    * @param proc_map
    *   mapping from old process ids to new ones
    * @return
    */
  private def convert(
      proc_map: Map[S.ProcessId, T.ProcessId],
      proc: S.DefProcess
  )(implicit ctx: AssemblyContext): T.DefProcess = {

    import manticore.compiler.assembly.levels.{ConstType, InputType, OutputType, RegType, WireType, MemoryType}

    if (proc.registers.length >= 2048) {
      ctx.logger.info(
        s"Process ${proc.id} has ${proc.registers.length} registers."
      )
    }

    val res = T
      .DefProcess(
        id = proc_map(proc.id),
        registers = proc.registers.map(convert),
        // UnconstrainedIR does not have any custom functions, so there is nothing to lower.
        functions = Seq.empty,
        body = proc.body.map(convert(_, proc_map)),
        labels = proc.labels.map { lblgrp =>
          T.DefLabelGroup(
            lblgrp.memory,
            lblgrp.indexer.map { case (v, l) => (UInt16(v.toInt), l) },
            lblgrp.default
          )
        },
        globalMemories = proc.globalMemories.map { gmem =>
          T.DefGlobalMemory(
            memory = gmem.memory,
            base = gmem.base,
            size = gmem.size,
            content = gmem.content.map { bigWord =>
              assert(bigWord < 0xffff)
              UInt16(bigWord.toInt)
            },
            annons = gmem.annons
          )

        },
        annons = proc.annons
      )
      .setPos(proc.pos)
    res
  }

  /** Unchecked conversion of DefReg
    *
    * @param reg
    *   original DefReg
    * @return
    *   converted one
    */

  private def convert(r: S.DefReg)(implicit ctx: AssemblyContext): T.DefReg = {
    import manticore.compiler.assembly.levels.{
      VariableType,
      ConstType,
      InputType,
      RegType,
      MemoryType,
      OutputType,
      WireType
    }

    val v = r.variable.varType match {
      case MemoryType =>
        val memVar = r.variable.asInstanceOf[S.MemoryVariable]

        def initValue(bigVal: BigInt): UInt16 =
          if (bigVal > 0xffff) {
            ctx.logger.error(s"invalid initial memory value ${bigVal}")
            UInt16((bigVal & 0xffff).toInt)
          } else {
            UInt16(bigVal.toInt)
          }
        val initialContent = if (memVar.content.nonEmpty) {
          memVar.content.map { initValue }

        } else {
          r.annons
            .collectFirst { case memInit: MemInit =>
              memInit.readFile().map { initValue }.toSeq
            }
            .getOrElse(Nil)
        }

        T.MemoryVariable(
          name = r.variable.name,
          id = -1, // to indicate an unallocated register
          size = memVar.size,
          initialContent = initialContent
        )

      case t @ _ =>
        T.ValueVariable(
          name = r.variable.name,
          id = -1, // to indicate unallocated registers
          tpe = r.variable.varType
        )
    }
    val value: Option[UInt16] = r.value match {
      case Some(b: BigInt) =>
        Some(if (b > 0xffff) {
          ctx.logger.error(s"initial value is too large!", r)
          UInt16(0)
        } else {
          UInt16(b.toInt)
        })
      case None =>
        None
    }
    T.DefReg(
      variable = v,
      value = value,
      annons = r.annons
    ).setPos(r.pos)
  }

  /** Unchecked conversion of instructions
    *
    * @param inst
    * @param proc_map
    */
  private def convert(
      inst: S.Instruction,
      proc_map: Map[S.ProcessId, T.ProcessId]
  )(implicit ctx: AssemblyContext): T.Instruction = (inst match {

    case S.BinaryArithmetic(operator, rd, rs1, rs2, annons) =>
      T.BinaryArithmetic(operator, rd, rs1, rs2, annons)
    case S.CustomInstruction(func, rd, rsx, annons) =>
      T.CustomInstruction(func, rd, rsx, annons)
    case S.LocalLoad(rd, base, addr, order, annons) =>
      // val new_offset: Int = computeOffset(inst, offset)
      T.LocalLoad(
        rd,
        base,
        addr,
        T.MemoryAccessOrder(order.memory, order.value),
        annons
      )
    case S.LocalStore(rs, base, addr, p, order, annons) =>
      // val new_offset: Int = computeOffset(inst, addr)
      T.LocalStore(
        rs,
        base,
        addr,
        p,
        T.MemoryAccessOrder(order.memory, order.value),
        annons
      )
    case S.GlobalLoad(rd, base, order, annons) =>
      val tOrder = order match {
        case S.SystemCallOrder(value)           => T.SystemCallOrder(value)
        case S.MemoryAccessOrder(memory, value) => T.MemoryAccessOrder(memory, value)
      }
      T.GlobalLoad(rd, base, tOrder, annons)
    case S.GlobalStore(rs, base, p, order, annons) =>
      val tOrder = order match {
        case S.SystemCallOrder(value)           => T.SystemCallOrder(value)
        case S.MemoryAccessOrder(memory, value) => T.MemoryAccessOrder(memory, value)
      }
      T.GlobalStore(rs, base, p, tOrder, annons)
    case S.Send(rd, rs, dest_id, annons) =>
      T.Send(rd, rs, proc_map(dest_id), annons)
    case S.SetValue(rd, value, annons) =>
      T.SetValue(rd, UInt16(value.toInt), annons)
    case S.Predicate(rs, annons) =>
      T.Predicate(rs, annons)
    case S.Mux(rd, sel, rs1, rs2, annons) =>
      T.Mux(rd, sel, rs1, rs2, annons)
    case S.Mov(rd, rs, annons) =>
      T.Mov(rd, rs, annons)
    case S.Slice(rd, rs, offset, length, annons) =>
      T.Slice(rd, rs, offset, length, annons)
    case S.PadZero(rd, rs, width, annons) =>
      ctx.logger.error("Unsupported instruction", inst)
      T.PadZero(rd, rs, UInt16(16), annons)
    case S.Recv(rd, id, rs, a) =>
      ctx.logger.error("Illegal instruction", inst)
      T.Recv(rd, rs, T.ProcessIdImpl(id, -1, -1), a)
    case S.AddCarry(rd, rs1, rs2, ci, annons) =>
      T.AddCarry(rd, rs1, rs2, ci, annons)
    case S.SetCarry(rd, annons)   => T.SetCarry(rd, annons)
    case S.ClearCarry(rd, annons) => T.ClearCarry(rd, annons)
    case S.ParMux(rd, choices, default, annons) =>
      ctx.logger.error(
        "Can not have pseudo-instruction in PlacedIR, make sure you run ParMuxDeconstruction!",
        inst
      )
      T.ParMux(
        rd,
        choices.map { case S.ParMuxCase(c, rs) => T.ParMuxCase(c, rs) },
        default,
        annons
      )
    case S.Nop                             => T.Nop
    case S.BreakCase(target, annons)       => T.BreakCase(target, annons)
    case S.Lookup(rd, index, base, annons) => T.Lookup(rd, index, base, annons)
    case S.JumpTable(target, results, blocks, dslot, annons) =>
      val tPhis = results.map { case S.Phi(rd, rss) => T.Phi(rd, rss) }
      val tBlocks = blocks.map { case S.JumpCase(lbl, blk) =>
        T.JumpCase(lbl, blk.map(convert(_, proc_map)))
      }
      val tDslot = dslot.map(convert(_, proc_map))
      T.JumpTable(target, tPhis, tBlocks, tDslot, annons)
    case S.PutSerial(rs, pred, order, annons) =>
      T.PutSerial(rs, pred, T.SystemCallOrder(order.value), annons)
    case S.Interrupt(description, condition, order, annons) =>
      val info = annons.collectFirst { case src: Sourceinfo => src }

      val tDesc = description.action match {
        case FinishInterrupt | StopInterrupt | AssertionInterrupt =>
          T.SimpleInterruptDescription(action = description.action, info = info, eid = -1)
        case fmt: SerialInterrupt =>
          T.SerialInterruptDescription(action = fmt, info = info, eid = -1, pointers = Nil)
      }
      T.Interrupt(tDesc, condition, T.SystemCallOrder(order.value), annons)
    case S.ConfigCfu(funcIdx, bitIdx, equation, annons) =>
      ctx.logger.error(
        "Can not have CONFIG_CFU instruction in PlacedIR. Can only appear in program initializer!",
        inst
      )
      T.ConfigCfu(funcIdx, bitIdx, equation, annons)

  }).setPos(inst.pos)

  private def computeOffset(
      inst: S.Instruction,
      old_offset: BigInt
  )(implicit ctx: AssemblyContext): Int = {
    val new_offset: Int = inst.annons.collectFirst { case a: Memblock =>
      a
    } match {
      case Some(mblock_annon: Memblock) =>
        mblock_annon.getIndex() match {
          case Some(ix) =>
            if (old_offset != 0) {
              ctx.logger.error(
                "Can not handle offset with subword index!",
                inst
              )
              old_offset.toInt
            } else {
              val cap = mblock_annon.getCapacity()
              ix * cap
            }
          case None =>
            old_offset.toInt
        }
      case None =>
        ctx.logger.error(s"Expected @${Memblock.name}", inst)
        0
    }
    new_offset
  }

  /** Check is [[asm: S.DefProgram]] is convertible to [[T.DefProgram]]
    *
    * @param asm
    * @return
    */
  private def isConvertible(
      asm: S.DefProgram
  )(implicit ctx: AssemblyContext): Boolean = {

    def processIsConvertible(p: S.DefProcess): Boolean = {
      val regsConvertible = p.registers
        .map { r =>
          // ensure every register is 16 bits
          if (!(r.variable.width == 16 || r.variable.width == 1)) {
            ctx.logger.error(
              s"expected  16-bit register in process ${p.id}",
              r
            )
            false
          } else {
            r.value match {
              case Some(x) => // ensure initial values are 16 bits
                s"register ${r.serialized} has an illegal initial value"
                x < (1 << 16)
              case _ => true
            }
          }
        }
        .forall(_ == true)

      val bodyConvertible = p.body
        .map { i =>
          i match {
            case S.SetValue(_, value, _) =>
              if (value >= (1 << 16)) {
                ctx.logger.error(
                  s"invalid immediate value in ${i.serialized}"
                )
                false
              } else true
            case _ => true
          }
        }
        .forall(_ == true)

      regsConvertible && bodyConvertible
    }

    val programConvertible =
      asm.processes.map(p => processIsConvertible(p)).forall(_ == true)
    programConvertible
  }

}
