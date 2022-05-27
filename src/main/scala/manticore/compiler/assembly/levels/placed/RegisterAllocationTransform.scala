package manticore.compiler.assembly.levels.placed

import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.ConstType
import manticore.compiler.assembly.levels.UInt16

import scala.collection.mutable.{Queue => MutableQueue}
import scala.collection.mutable.{ArrayDeque => MutableDeque}
import scala.collection.mutable.PriorityQueue
import manticore.compiler.assembly.CanComputeNameDependence
import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.assembly.levels.{ConstType, MemoryType, InputType}
import manticore.compiler.assembly.levels.CarryType

/** Register allocation transform using a linear scan
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */

object RegisterAllocationTransform
    extends CanComputeNameDependence
    with PlacedIRTransformer {

  val flavor = PlacedIR
  import PlacedIR._

  case class LiveRegister(reg: DefReg, lifetime: (Int, Int))
  object LivenessStartOrdering extends Ordering[LiveRegister] {
    override def compare(x: LiveRegister, y: LiveRegister): Int =
      Ordering[Int].reverse.compare(x.lifetime._1, y.lifetime._1)
  }

  object LivenessEndOrdering extends Ordering[LiveRegister] {
    override def compare(x: LiveRegister, y: LiveRegister): Int =
      Ordering[Int].reverse.compare(x.lifetime._2, y.lifetime._2)
  }

  /** Compute liveness intervals and return them in increasing order of start
    * times.
    *
    * @param process
    * @param ctx
    * @return
    */
  private def computeLiveness(
      process: DefProcess
  )(implicit
      ctx: AssemblyContext
  ): scala.collection.mutable.PriorityQueue[LiveRegister] = {

    // initialize liveness scopes, constants are always live, therefore,
    // their liveness interval is 0 to infinity.
    val mortal_registers = process.registers.collect {
      case r: DefReg if (!isImmortal(r)) => r.variable.name -> r
    }.toMap

    val life_begin = scala.collection.mutable.Map.empty[DefReg, Int]
    val life_end = scala.collection.mutable.Map.empty[DefReg, Int]

    /** Now walk the sequence of instruction. Every time a register value is
      * defined, create mapping from the defined register to an interval while
      * marking the start with the instruction index. Contrarily, when a
      * register is used, find update its liveness bound end with the index of
      * the instruction.
      */

    process.body.zipWithIndex.foreach { case (inst, cycle) =>
      NameDependence.regDef(inst) match {
        case Seq() =>
        case rds @ _ =>
          rds.foreach { rd =>
            mortal_registers.get(rd) match {
              case Some(rdef) =>
                if (life_begin.contains(rdef)) {
                  ctx.logger.error(
                    s"Register ${rd} is defined multiple time, last here",
                    inst
                  )
                }
                life_begin += rdef -> cycle
              case None => // register is forever live
            }
          }
      }
      NameDependence.regUses(inst).foreach { rs =>
        mortal_registers.get(rs) match {
          case Some(rsdef) =>
            if (!life_begin.contains(rsdef)) {
              ctx.logger
                .error(s"Register ${rs} is used before being defined", inst)
            }
            life_end += (rsdef -> cycle)
          case None => //
        }
      }
    }

    val queue = scala.collection.mutable.PriorityQueue
      .empty[LiveRegister](LivenessStartOrdering)
    life_begin.foreach { case (rdef, beg) =>
      life_end.get(rdef) match {
        case Some(end) =>
          if (rdef.value.isDefined) {
            ctx.logger.warn(
              s"only .input, .mem, and .const definitions can have an initial value!",
              rdef
            )
          }
          queue += LiveRegister(rdef, (beg, end))
        case None =>
          ctx.logger.warn(
            s"Register ${rdef.variable.name} in process ${process.id} is never used.",
            rdef
          )
          queue += LiveRegister(rdef, (beg, beg))
      }
    }
    queue
  }

  private def withId(r: DefReg, new_id: Int): DefReg =
    r.copy(variable = r.variable.withId(new_id)).setPos(r.pos)

  def isImmortal(r: DefReg): Boolean = {
    val tpe = r.variable.varType
    tpe == ConstType || tpe == InputType || tpe == MemoryType
  }
  private def allocateImmortals(
      process: DefProcess
  )(implicit ctx: AssemblyContext): Seq[DefReg] = {

    // define an ordering on DefRegs, prioritizing constants
    object DefRegOrdering extends Ordering[DefReg] {
      override def compare(x: DefReg, y: DefReg): Int =
        (x.variable.varType, y.variable.varType) match {
          case (ConstType, ConstType) =>
            val xval = x.value.getOrElse(
              ctx.logger.fail(s"Constant ${x.variable.name} is not defined!")
            )
            val yval = y.value.getOrElse(
              ctx.logger.fail(s"Constant ${y.variable.name} is not defined!")
            )
            Ordering[Int].reverse.compare(xval.toInt, yval.toInt)
          case (ConstType, _) => 1
          case (_, ConstType) => -1
          case (_, _)         => 0
        }
    }

    // collect the eternal registers, i.e., constants, inputs and memory base pointers
    // the constants are placed at the head of the queue with increasing order of
    // their values

    val eternals = scala.collection.mutable.PriorityQueue
      .empty[DefReg](DefRegOrdering) ++ process.registers.collect {
      case r: DefReg if isImmortal(r) => r
    }
    // the constants are ordered in increasing order
    def putConstIfAbsent(value: Int): Unit = {
      if (
        !eternals.exists(r =>
          r.variable.varType == ConstType && r.value == Some(UInt16(value))
        )
      )
        eternals +=
          DefReg(
            ValueVariable(s"c0_${ctx.uniqueNumber()}", -1, ConstType),
            Some(UInt16(value))
          )
    }
    // append 0 and 1 constants if they are not present
    putConstIfAbsent(0)
    putConstIfAbsent(1)

    // now we have constant 0 and 1 as the first two registers, all we need to
    // do now is to
    val max_registers = ctx.max_registers
    val num_eternals = eternals.size
    if (num_eternals > max_registers) {
      ctx.logger.error(
        s"The can not allocate ${num_eternals} constant and input registers into ${max_registers} machine registers in process ${process.id}"
      )
      ctx.logger.fail(s"Aborting ${transformId}")
    }

    eternals
      .dequeueAll[DefReg]
      .zipWithIndex
      .map { case (r: DefReg, index: Int) =>
        withId(r, index)
      }

  }
  private def allocateRegisters(
      process: DefProcess
  )(implicit ctx: AssemblyContext): DefProcess = {

    // isolate the pre-allocated registers: memory bases, inputs and constants
    val immortals = allocateImmortals(process)
    // from the non-preallocated ones
    val num_immortals = immortals.size
    val max_registers = ctx.max_registers
    val max_carries = ctx.max_carries

    val allocated_carries = scala.collection.mutable.Queue.empty[DefReg]
    val allocated_registers = scala.collection.mutable.Queue.empty[DefReg]

    val free_registers = scala.collection.mutable.Queue
      .empty[Int] ++ Seq.tabulate(max_registers - num_immortals) { i =>
      i + num_immortals
    }
    val free_carries =
      scala.collection.mutable.Queue.empty[Int] ++ Seq.tabulate(max_carries) {
        i => i
      }

    // we spill registers prioritizing the ones that die latest, because these
    // registers

    // a sorted queue of register (increasing start time)
    val to_allocate = computeLiveness(process)

    // a sorted queue of active (allocated) registers, sorted by increasing end time
    val active_list = scala.collection.mutable.PriorityQueue
      .empty[LiveRegister](LivenessEndOrdering)

    object DefRegOrdering extends Ordering[DefReg] {
      override def compare(x: DefReg, y: DefReg): Int =
        Ordering[Int].reverse.compare(x.variable.id, y.variable.id)
    }

    val allocated_list = scala.collection.mutable.PriorityQueue
      .empty[DefReg](DefRegOrdering) ++ immortals
    val allocated_carry_list =
      scala.collection.mutable.PriorityQueue.empty[DefReg](DefRegOrdering)
    var failed = false

    // if (ctx.debug_message) {
    //   val to_alloc_copy: Seq[LiveRegister] = to_allocate.clone().dequeueAll

    //   ctx.logger.debug(to_alloc_copy.map { r =>
    //     s"${r.reg.variable.name}: ${r.lifetime}"
    //   } mkString ("\n"))

    // }

    def releaseDead(
        now_time: Int,
        q: scala.collection.mutable.PriorityQueue[LiveRegister]
    ): Unit = {

      var may_release = q.nonEmpty

      // if (q.nonEmpty && ctx.debug_message) {

      //   val copy = q.clone().dequeueAll
      //   val ls = copy.map { r =>
      //     s"${r.reg.variable.name}: ${r.lifetime}"
      //   } mkString ("\n")
      //   ctx.logger.debug(s"active_list:\n${ls}")
      // }
      while (may_release) {
        // try to release
        val head = q.dequeue()
        if (head.lifetime._2 < now_time) {
          ctx.logger.debug(
            s"@${now_time} Releasing ${head.reg.variable.name} : ${head.lifetime}"
          )
          val dead_reg = head.reg
          if (dead_reg.variable.varType == CarryType) {
            free_carries += dead_reg.variable.id
          } else {
            free_registers += dead_reg.variable.id
          }
          may_release = q.nonEmpty
        } else {
          may_release = false
          q += head
        }
      }
    }
    while (to_allocate.nonEmpty && !failed) {

      val LiveRegister(reg, interval @ (start_time, _)) = to_allocate.dequeue()

      ctx.logger.debug(s"time: ${start_time}")

      // check if we can release any registers

      releaseDead(start_time, active_list)

      if (reg.variable.varType == CarryType) {
        // try allocating
        if (free_carries.nonEmpty) {
          val cid = free_carries.dequeue()
          val reg_as_alloc = withId(reg, cid)
          active_list += LiveRegister(reg_as_alloc, interval)
          allocated_carry_list += reg_as_alloc
        } else {
          ctx.logger.error(s"ran out of carry register", reg)
          failed = true
        }
      } else {
        if (free_registers.nonEmpty) {
          val rid = free_registers.dequeue()
          val reg_as_alloc = withId(reg, rid)
          active_list += LiveRegister(reg_as_alloc, interval)
          allocated_list += reg_as_alloc
        } else {
          ctx.logger.error(s"ran out of registers", reg)
          failed = true
        }
      }
    }

    process
      .copy(
        registers = allocated_list.toSeq ++ allocated_carry_list.toSeq
      )
      .setPos(process.pos)

  }
  override def transform(
      source: DefProgram
  )(implicit context: AssemblyContext): DefProgram = {

    source.copy(
      processes = source.processes.map(allocateRegisters)
    )

  }

}
