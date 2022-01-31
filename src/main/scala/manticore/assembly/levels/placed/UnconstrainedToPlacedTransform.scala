package manticore.assembly.levels.placed

import manticore.assembly.levels.unconstrained.UnconstrainedIR
import manticore.assembly.levels.AssemblyTransformer

import manticore.assembly.levels.unconstrained.{UnconstrainedIR => S}
import manticore.assembly.levels.placed.{PlacedIR => T}

import manticore.assembly.levels.UInt16

import manticore.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import manticore.assembly.annotations.AssemblyAnnotation
import manticore.assembly.annotations.{
  Loc => LocAnnotation,
  Layout => LayoutAnnotation
}
import manticore.assembly.annotations.AssemblyAnnotationFields.{
  X => XField,
  Y => YField,
  Block => BlockField,
  Capacity => CapacityField
}
import manticore.assembly.annotations.Memblock
import manticore.assembly.ManticoreAssemblyIR
import manticore.assembly.levels.CarryType
import manticore.assembly.annotations.MemInit
import scala.util.Try
import scala.util.Failure
import scala.util.Success

/** Transform an Unconstrained assembly to a placed one, looking for [[@LAYOUT]]
  * and [[@LOC]] annotations for placement information
  *
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */
object UnconstrainedToPlacedTransform
    extends AssemblyTransformer[
      UnconstrainedIR.DefProgram,
      PlacedIR.DefProgram
    ] {

  override def transform(
      asm: S.DefProgram,
      context: AssemblyContext
  ): T.DefProgram = {
    implicit val ctx = context

    if (isConvertible(asm) == false) {
      ctx.logger.fail(
        s"Can not convert program"
      );
    }

    val converted_processes = if (ctx.use_loc) {
      // probably in debug or test mode, check for @LOC annotations and
      // place the processes accordingly
      val converted_ids: Map[S.ProcessId, T.ProcessId] = asm.processes.map {
        p: S.DefProcess =>
          val location: Option[LocAnnotation] = p.annons.collectFirst {
            case l: LocAnnotation => l
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

    import manticore.assembly.levels.{
      ConstType,
      InputType,
      OutputType,
      RegType,
      WireType,
      MemoryType
    }

    if (proc.registers.length >= 2048) {
      ctx.logger.info(
        s"Process ${proc.id} has ${proc.registers.length} registers."
      )
    }

    T.DefProcess(
      id = proc_map(proc.id),
      registers = proc.registers.map(convert),
      functions = proc.functions.map(convert),
      body = proc.body.map(convert(_, proc_map)),
      annons = proc.annons
    ).setPos(proc.pos)
  }

  /** Unchecked conversion of DefReg
    *
    * @param reg
    *   original DefReg
    * @return
    *   converted one
    */

  private def convert(r: S.DefReg)(implicit ctx: AssemblyContext): T.DefReg = {
    import manticore.assembly.levels.{
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
        val mblock_annon_opt = r.annons.collectFirst { case m: Memblock => m }
        if (mblock_annon_opt.isEmpty) {
          ctx.logger.error(s"Expected @${Memblock.name} annotation", r)
          ctx.logger.fail(s"failed transformation")
        }

        val initial_content: Seq[UInt16] = r.annons.collectFirst {
          case i: MemInit =>
            i
        } match {
          case Some(init) =>
            Try {
              val count = init.getCount()
              val width = init.getWidth()
              if (width != mblock_annon_opt.get.getWidth()) {
                ctx.logger.error(
                  s"memory init width is different from the block width!"
                )
              }
              val lines = scala.io.Source
                .fromFile(init.getFileName())
                .getLines()
                .slice(0, count)

              if (width <= 16) {
                lines.map { l: String => UInt16(l.toInt) }.toSeq
              } else {
                // create an initial memory with
                // least significant shorts first followed by
                // most significant shorts
                // [0xE 0xFFFF, 0x2 0x0001] becomes
                // [0xFFFF, 0x0001, 0x000E, 0x0002]
                lines
                  .map { l: String =>
                    val big_word = BigInt(l)

                    val num_shorts = (width - 1) / 16 + 1

                    Seq.tabulate(num_shorts) { sub_word_index =>
                      val shifted = big_word >> (sub_word_index * 16)
                      val masked = shifted & 0xffff
                      UInt16(masked.toInt)
                    }
                  }
                  .toSeq
                  .transpose
                  .flatten
              }
            } match {
              case Failure(exception) =>
                ctx.logger.error("could not initialize memory properly", r)
                ctx.logger.error(
                  s"Exception occurred while initializing memory: ${exception.getMessage()}"
                )
                Seq.empty[UInt16]
              case Success(value) =>
                value
            }
          case None =>
            Seq.empty[UInt16]
        }
        T.MemoryVariable(
          name = r.variable.name,
          id = -1, // to indicate an unallocated register
          block = T
            .MemoryBlock(
              block_id = mblock_annon_opt.get.getBlock(),
              capacity = mblock_annon_opt.get.getCapacity(),
              width = mblock_annon_opt.get.getWidth(),
              initial_content = initial_content
            )
        )
      case t @ _ =>
        T.ValueVariable(
          name = r.variable.name,
          id = -1, // to indicate unallocated registers
          tpe = r.variable.tpe
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

  /** Unchecked conversion of DefFunc
    *
    * @param func
    *   orignal function
    * @return
    *   converted one
    */
  private def convert(func: S.DefFunc): T.DefFunc =
    T.DefFunc(
      func.name,
      T.CustomFunctionImpl(func.value.values.map(x => UInt16(x.toInt))),
      func.annons
    ).setPos(func.pos)

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
    case S.CustomInstruction(func, rd, rs1, rs2, rs3, rs4, annons) =>
      T.CustomInstruction(func, rd, rs1, rs2, rs3, rs4, annons)
    case S.Expect(ref, got, error_id, annons) =>
      T.Expect(ref, got, UInt16(0), annons)
    case S.LocalLoad(rd, base, offset, annons) =>
      T.LocalLoad(rd, base, UInt16(offset.toInt), annons)
    case S.LocalStore(rs, base, offset, p, annons) =>
      T.LocalStore(rs, base, UInt16(offset.toInt), p, annons)
    case S.GlobalLoad(rd, base, annons) =>
      T.GlobalLoad(rd, base, annons)
    case S.GlobalStore(rs, base, p, annons) =>
      T.GlobalStore(rs, base, p, annons)
    case S.Send(rd, rs, dest_id, annons) =>
      T.Send(rd, rs, proc_map(dest_id), annons)
    case S.SetValue(rd, value, annons) =>
      T.SetValue(rd, UInt16(value.toInt), annons)
    case S.Predicate(rs, annons) =>
      T.Predicate(rs, annons)
    case S.Mux(rd, sel, rs1, rs2, annons) =>
      T.Mux(rd, sel, rs1, rs2, annons)
    case S.Mov(rd, rs, annons) => T.Mov(rd, rs, annons)
    case S.PadZero(rd, rs, width, annons) =>
      ctx.logger.error("Unsupported instruction", inst)
      T.PadZero(rd, rs, UInt16(16), annons)
    case S.Recv(rd, id, rs, a) =>
      ctx.logger.error("Illegal instruction", inst)
      T.Recv(rd, rs, T.ProcessIdImpl(id, -1, -1), a)
    case S.AddC(rd, co, rs1, rs2, ci, annons) =>
      T.AddC(rd, co, rs1, rs2, ci, annons)
    case S.SetCarry(rd, annons)   => T.SetCarry(rd, annons)
    case S.ClearCarry(rd, annons) => T.ClearCarry(rd, annons)
    case S.Nop                    => T.Nop

  }).setPos(inst.pos)

  /** Check is [[asm: S.DefProgram]] is convertible to [[T.DefProgram]]
    *
    * @param asm
    * @return
    */
  private def isConvertible(
      asm: S.DefProgram
  )(implicit ctx: AssemblyContext): Boolean = {

    asm.processes
      .map { p =>
        p.registers
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
          .forall(_ == true) &&
        p.functions
          .map { f =>
            // ensure every function has 16 elements
            if (f.value.values.length != 16) {
              ctx.logger.error(
                s"function ${f.serialized} in process ${p.id} is not 16-bit"
              )
              false
            } else {
              // ensure every equation fits in 16 bits
              if (f.value.values.forall { x => x < (1 << 16) } == false) {
                ctx.logger.error(
                  "" +
                    s"function ${f.serialized} has illegal values"
                )
                false
              } else {
                true
              }

            }
          }
          .forall(_ == true) &&
        p.body
          .map { i =>
            i match {
              case S.LocalLoad(_, _, offset, _) =>
                if (offset >= (1 << 16)) {
                  ctx.logger.error(s"invalid offset in ${i.serialized}")
                  false
                } else true
              case S.LocalStore(_, _, offset, _, _) =>
                if (offset >= (1 << 16)) {
                  ctx.logger.error(s"invalid offset in ${i.serialized}")
                  false
                } else true
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
      }
      .forall(_ == true)

  }

}
