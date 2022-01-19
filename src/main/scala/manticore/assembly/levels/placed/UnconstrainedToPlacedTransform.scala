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
        s"${S.getClass().getSimpleName()} not convertible to ${T.getClass().getSimpleName()}"
      );
    }

    val (dimx: Int, dimy: Int) =
      if (hasLayoutInformation(asm))
        getDimensions(asm) match {
          case Some((xx, yy)) => (xx, yy)
          case None =>
            ctx.logger.fail("invalid dimension")
            (0, 0)
        }
      else {
        ctx.logger.fail("no @LAYOUT annotation")
        (0, 0)
      }

    val converted_ids = asm.processes
      .map { p =>
        try {
          val location: AssemblyAnnotation =
            p.annons.find(_.name == LocAnnotation.name).get

          val x = location.getIntValue(XField).get
          val y = location.getIntValue(YField).get
          if (x >= dimx || y >= dimy) {
            ctx.logger.error(s"location out of bounds for process ${p.id}", p)
            p.id -> None
          } else {
            p.id -> Some(T.ProcessIdImpl(p.id, x, y))
          }
        } catch {
          case _: Throwable =>
            p.id -> None
        }

      }

    converted_ids.find { x => x._2.isEmpty } match {
      case Some(p) =>
        ctx.logger.fail(s"process ${p._1} does not have valid @LOC annotation")
      case _ =>
    }

    implicit val proc_map: Map[S.ProcessId, T.ProcessId] = converted_ids.map {
      case (o, n) => o -> n.get
    }.toMap

    // ensure no duplicate location exists
    if (proc_map.values.toSeq.toSet.size != proc_map.size) {
      ctx.logger.error(s"duplicate locations in defined processes")
    }

    val out = T.DefProgram(
      processes = asm.processes.map(convert),
      annons = asm.annons
    )

    if (ctx.logger.countErrors() > 0) {
      ctx.logger.fail(s"Failed transform due to previous errors!")
    }
    out
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
      proc: S.DefProcess
  )(implicit proc_map: Map[S.ProcessId, T.ProcessId], ctx: AssemblyContext): T.DefProcess = {

    if (proc.registers.length >= 2048) {
      ctx.logger.info(
        s"Process ${proc.id} has ${proc.registers.length} registers."
      )
    }
    import manticore.assembly.levels.{
      VariableType,
      ConstType,
      InputType,
      RegType,
      MemoryType,
      OutputType,
      WireType
    }
    def filterRegs(tpe: VariableType) =
      proc.registers.filter(_.variable.tpe == tpe)
    val const_regs = filterRegs(ConstType).zipWithIndex.map { case (r, i) =>
      r.variable ->
        T.ConstVariable(r.variable.name, i)
    }.toMap

    val input_regs = filterRegs(InputType).zipWithIndex.map { case (r, i) =>
      r.variable ->
        T.InputVariable(r.variable.name, i + const_regs.size)
    }.toMap

    val output_regs = filterRegs(OutputType).zipWithIndex.map { case (r, i) =>
      r.variable ->
        T.OutputVariable(r.variable.name, i + const_regs.size + input_regs.size)
    }.toMap

    val reg_regs = filterRegs(RegType).zipWithIndex.map { case (r, i) =>
      r.variable ->
        T.RegVariable(
          r.variable.name,
          i + const_regs.size + input_regs.size + output_regs.size
        )
    }.toMap

    val wire_regs = filterRegs(WireType).zipWithIndex.map { case (r, i) =>
      r.variable ->
        T.RegVariable(
          r.variable.name,
          i + const_regs.size + input_regs.size + output_regs.size + reg_regs.size
        )
    }.toMap

    val mem_regs = filterRegs(MemoryType).zipWithIndex.map {
      case (r: S.DefReg, i) =>
        val block =
          r.findAnnotation(manticore.assembly.annotations.Memblock.name) match {
            case Some(block_annon) =>
              T.MemoryBlock(
                block_annon.getStringValue(BlockField).get,
                block_annon.getIntValue(CapacityField).get
              )
            case None =>
              ctx.logger.error("Memory block not specified")
              T.MemoryBlock("", 0)
          }
        r.variable ->
          T.MemoryVariable(
            r.variable.name,
            i + const_regs.size + input_regs.size + output_regs.size + reg_regs.size + wire_regs.size,
            block
          )
    }
    implicit val subst =
      (const_regs ++ input_regs ++ output_regs ++ reg_regs ++ wire_regs ++ mem_regs)
    T.DefProcess(
      id = proc_map(proc.id),
      registers = proc.registers.map(convert),
      functions = proc.functions.map(convert),
      body = proc.body.map(convert),
      annons = proc.annons
    ).setPos(proc.pos)
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

  /** Unchecked conversion of DefReg
    *
    * @param reg
    *   original DefReg
    * @return
    *   converted one
    */
  private def convert(
      reg: S.DefReg
  )(implicit subst: Map[S.LogicVariable, T.PlacedVariable]): T.DefReg =
    T.DefReg(
      subst(reg.variable),
      reg.value.map { v => UInt16(v.toInt) },
      reg.annons
    ).setPos(reg.pos)

  /** Unchecked conversion of instructions
    *
    * @param inst
    * @param proc_map
    */
  private def convert(
      inst: S.Instruction
  )(implicit proc_map: Map[String, T.ProcessId]): T.Instruction = (inst match {

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
    case S.Nop => T.Nop
  }).setPos(inst.pos)

  /** Checks if the @LAYOUT annotation exists
    *
    * @param asm
    *   original assmebly
    * @return
    *   true if @LAYOUT exists
    */
  private def hasLayoutInformation(asm: S.DefProgram): Boolean =
    asm.findAnnotation("LAYOUT").nonEmpty

  /** Retrieves the dimension info from @LAYOUT
    *
    * @param asm
    * @return
    */
  private def getDimensions(asm: S.DefProgram): Option[(Int, Int)] =
    try {
      val layout = asm.findAnnotation("LAYOUT").get
      val x = layout.getIntValue(XField).get
      val y = layout.getIntValue(YField).get
      Some((x, y))
    } catch {
      case _: Throwable =>
        None
    }

  /** Check is [[asm: S.DefProgram]] is convertible to [[T.DefProgram]]
    *
    * @param asm
    * @return
    */
  private def isConvertible(asm: S.DefProgram)(implicit ctx: AssemblyContext): Boolean = {

    asm.processes
      .map { p =>
        p.registers
          .map { r =>
            // ensure every register is 16 bits
            if (r.variable.width != 16) {
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
                  ctx.logger.error(s"invalid immediate value in ${i.serialized}")
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
