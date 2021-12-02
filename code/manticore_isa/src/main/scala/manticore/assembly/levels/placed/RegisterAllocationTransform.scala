package manticore.assembly.levels.placed

import manticore.assembly.levels.Transformation
import manticore.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import manticore.assembly.levels.ConstType
import manticore.assembly.levels.UInt16

import scala.collection.mutable.{Queue => MutableQueue}

/** Register allocation transform using a linear scan
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */

object RegisterAllocationTransform
    extends AssemblyTransformer(PlacedIR, PlacedIR) {

  import PlacedIR._

  import manticore.assembly.levels.placed.DependenceAnalysis
  type LivenessScope = Tuple2[Name, Tuple2[Int, Int]]
  private def computeLiveness(
      process: DefProcess
  )(implicit ctx: AssemblyContext): Seq[LivenessScope] = {

    // initialize liveness scopes, constants are always live, therefore,
    // their liveness internal is 0 to infinity.
    val life_begin = scala.collection.mutable.Map(
      process.registers.collect {
        case DefReg(v, Some(_), _) if v.varType == ConstType =>
          v.name -> 0
      }: _*
    )
    val life_end = scala.collection.mutable.Map.empty[Name, Int]

    /** Now walk the sequence of instruction. Every time a register value is
      * defined, create mapping from the defined register to an interval while
      * marking the start with the instruction index. Contrarily, when a
      * register is used, find update its liveness bound end with the index of
      * the instruction.
      */

    process.body.zipWithIndex.foreach { case (inst, ix) =>
      DependenceAnalysis.regDef(inst) match {
        case Some(rd) =>
          if (life_begin.contains(rd)) {
            logger.error(s"Register ${rd} is defined multiple times!", inst)
          } else {
            life_begin += (rd -> ix)
          }
        case None =>
      }
      DependenceAnalysis.regUses(inst).foreach { rs =>
        if (!life_begin.contains(rs)) {
          logger.error(s"Register ${rs} is used before being defined", inst)

        }
        life_end += (rs -> ix)
      }
    }

    life_begin
      .map { case (name, beg) =>
        life_end.get(name) match {
          case Some(end) => (name -> (beg, end))
          case None =>
            logger.warn(
              s"Register ${name} in process ${process.id} is never used."
            )
            (name -> (beg, beg))
        }
      }
      .toSeq
      .sortBy { case (n, (b, e)) => b }

  }

  /** Sets the pointer values of all [[DefReg(x: MemoryVariable, _, _)]]
    * register definitions and returns memory definitions with values
    * corresponding to correct array memory offsets and the total required
    * capacity
    * @param process
    * @param ctx
    * @return
    */
  private def allocateLocalMemory(
      process: DefProcess
  )(implicit ctx: AssemblyContext): (Seq[DefReg], Int) = {

    /** First allocate local memories, this will tell us whether there is any
      * "spill" capacity left in the array memory storage. We do this by going
      * through all .mem definitions and checking their capacity.
      */
    val memories = process.registers.collect {
      case r @ DefReg(v: MemoryVariable, _, _) => r
    }

    memories.foldLeft((Seq.empty[DefReg], 0)) { case ((allocs, base), m) =>
      (
        allocs :+ m.copy(value = Some(UInt16(base))),
        base + m.variable.asInstanceOf[MemoryVariable].block.capacity
      )
    }
  }
  private def allocateRegisters(
      process: DefProcess
  )(implicit ctx: AssemblyContext): DefProcess = {

    val MaxRegisters = ctx.max_registers
    val ArrayMemoryCapacity = 2048
    val MaxConstRegisters = 32

    val (memories, used_capacity) = allocateLocalMemory(process)
    val free_capacity = ArrayMemoryCapacity - used_capacity
    logger.info(
      s"Process ${process.id} requires ${used_capacity} memory words (max usable ${ArrayMemoryCapacity})"
    )

    val liveness = computeLiveness(process)

    val free_const_regs = MutableQueue[Int](
      Seq.tabulate(MaxConstRegisters) { i => i }: _*
    )

    // val free_user_regs = MutableQueue[Int](
    //   Seq.tabulate(MaxRegisters - MaxConstRegisters)
    // )
    var const_reg_index = 0

    val user_reg_index = MaxRegisters - 1

    logger.fail("Could not allocate registers")
  }
  override def transform(
      source: DefProgram,
      context: AssemblyContext
  ): DefProgram = {

    logger.fail("Could not allocate registers")
  }

}
