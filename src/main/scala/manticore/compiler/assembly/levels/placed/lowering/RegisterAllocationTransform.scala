package manticore.compiler.assembly.levels.placed.lowering

import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.ConstType
import manticore.compiler.assembly.levels.InputType
import manticore.compiler.assembly.levels.MemoryType
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.assembly.levels.placed.PlacedIRInputOutputCollector.InputOutputPairs
import manticore.compiler.assembly.levels.placed.PlacedIRDependencyDependenceGraphBuilder.DependenceAnalysis
import manticore.compiler.assembly.levels.OutputType
import manticore.compiler.assembly.levels.placed.lowering.util.IntervalSet
import manticore.compiler.assembly.levels.CarryType
import scala.annotation.tailrec
import java.util.ArrayDeque
import manticore.compiler.assembly.levels.placed.LatencyAnalysis

private[lowering] object RegisterAllocationTransform
    extends AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {
  import PlacedIR._

  override def transform(
      source: DefProgram,
      context: AssemblyContext
  ): DefProgram = {

    val processes = source.processes.map(transform(_)(context))
    source.copy(
      processes = processes
    )
  }

  private def withId(r: DefReg, new_id: Int): DefReg =
    r.copy(variable = r.variable.withId(new_id)).setPos(r.pos)

  private def allocateImmortals(
      process: DefProcess
  )(implicit ctx: AssemblyContext): Seq[DefReg] = {

    def checkValue(r: DefReg): Unit = if (r.value.isEmpty) {
      ctx.logger.error("Expected initial value!", r)
    }
    val processConstants = process.registers.filter {
      _.variable.varType == ConstType
    }
    // ensure all constants are defined
    processConstants.foreach(checkValue)

    def intValue(r: DefReg): Int = r.value.getOrElse(UInt16(0xffff)).toInt
    def defValue(v: Int) = DefReg(
      ValueVariable(s"%c_${ctx.uniqueNumber()}", -1, ConstType),
      Some(UInt16(v))
    )
    val sortedConstants =
      processConstants.sortBy(_.value.getOrElse(UInt16(0xffff)).toInt)
    val withZeroAndOne = sortedConstants match {
      case first +: (second +: tail) =>
        if (intValue(first) == 0) {
          if (intValue(second) == 1) {
            sortedConstants
          } else {
            first +: (defValue(1) +: (second +: tail))
          }
        } else {
          if (intValue(first) == 1) {
            defValue(0) +: (first +: (second +: tail))
          } else {
            defValue(0) +: (defValue(1) +: (second +: tail))
          }
        }
      case first +: Nil =>
        if (intValue(first) == 0) {
          first +: (defValue(1) +: Nil)
        } else {
          if (intValue(first) == 1) {
            defValue(0) +: (first +: Nil)
          } else {
            defValue(0) +: (defValue(1) +: (first +: Nil))
          }
        }
      case Nil =>
        defValue(0) +: (defValue(1) +: Nil)
    }

    val memories = process.registers.filter { _.variable.varType == MemoryType }

    // make sure memories are allocated
    memories.foreach(checkValue)

    val immortals = withZeroAndOne ++ memories

    if (immortals.length > ctx.max_registers) {
      ctx.logger.error(
        s"Can not allocate register in process ${process.id}! There are " +
          s"${immortals.length} immortal registers but only have " +
          s"${ctx.max_registers} machine registers!"
      )
      ctx.logger.fail("Aborting!")
    }

    immortals.zipWithIndex.map { case (r: DefReg, index) =>
      withId(r, index)
    }
  }

  // a map from Phi operands to their results
  private def phiOperandMap(process: DefProcess) = {

    process.body
      .collect { case JumpTable(_, results, _, _, _) =>
        results.flatMap { case Phi(rd, rss) => rss.map(_._2 -> rd) }
      }
      .flatten
      .toMap

  }

  private case class AllocationHint(lastId: Int, lastInterval: IntervalSet)
  private trait HintManager {

    // tell the manager of a new hint
    def tell(name: Name)(newHint: => AllocationHint): Unit
    // lookup a hint
    def seek(name: Name): Option[AllocationHint]
    // get the Name that aliases name.
    // For instance phi(rd, rs1, rs2):
    // alias(rs1) = rd
    // alias(rs2) = rd
    // alias(rd) = rd
    def alias(name: Name): Option[Name]

  }
  private def createHintManager(
      process: DefProcess
  )(implicit ctx: AssemblyContext): HintManager = {

    val statePairs = InputOutputPairs.createInputOutputPairs(process)

    val phiOutputs = process.body
      .collect { case JumpTable(_, results, _, _, _) =>
        results.map(_.rd)
      }
      .flatten
      .toSet
    val phiInputsToOutputs = process.body
      .collect { case JumpTable(_, results, _, _, _) =>
        results.flatMap { case Phi(rd, rss) => rss.map(_._2 -> rd) }
      }
      .flatten
      .toMap

    val getCurrent = statePairs.map { case (curr, next) =>
      next.variable.name -> curr.variable.name
    }.toMap

    val hints =
      scala.collection.mutable.Map.empty[Name, Option[AllocationHint]]
    hints ++= statePairs.map(_._1.variable.name -> None)
    hints ++= phiOutputs.toSeq.map(_ -> None)

    new HintManager {

      def alias(n: Name): Option[Name] =
        if (hints.contains(n)) {
          // n is either input or rd of a a phi
          Some(n)
        } else {
          if (getCurrent.contains(n)) {
            // n is an output
            val itsCurrent = getCurrent(n)
            Some(itsCurrent)
          } else if (phiInputsToOutputs.contains(n)) {
            // n is a phi operand
            val itsOutput = phiInputsToOutputs(n)
            Some(itsOutput)
          } else {
            None
          }
        }

      def seek(n: Name): Option[AllocationHint] =
        alias(n) match {
          case None        => None
          case Some(realName) => hints(realName)
        }

      def tell(name: Name)(newHint: => AllocationHint): Unit =
        alias(name) match {
          case None        => // do nothing, can not accept hint
          case Some(realName) => hints.update(realName, Some(newHint))
        }

    }
  }

  private def transform(
      process: DefProcess
  )(implicit ctx: AssemblyContext): DefProcess = {
    import scala.collection.mutable.PriorityQueue
    import scala.collection.mutable.ArrayDeque
    import scala.collection.mutable.Queue

    val lifetime = ctx.stats.recordRunTime(s"lifetime ${process.id}") {
      util.LifetimeAnalysis(process)
    }

    val immortals = allocateImmortals(process)
    val numImmortals = immortals.length
    val registerCapacity = ctx.max_registers
    val carryCapacity = ctx.max_carries
    val allocatedCarries = scala.collection.mutable.Queue.empty[DefReg]
    val allocatedNames = scala.collection.mutable.Queue.empty[DefReg]

    // create a list of register with
    val mainFreeList =
      ArrayDeque.empty[Int] ++ Seq.tabulate(registerCapacity - numImmortals) {
        i =>
          i + numImmortals
      }
    val freeCarryList = ArrayDeque.empty[Int] ++ Range(0, carryCapacity)

    val allocationHint =
      scala.collection.mutable.Map.empty[Name, AllocationHint]

    // val unallocatedList = PriorityQueue.empty[DefReg](IncreasingStartTimeOrder)

    // the active list is ordered by increasing end time. So that at any position
    // we can "free" the first n elements that their end time is less than the
    // current position

    val IncreasingEndTime =
      Ordering.by[DefReg, Int] { r => lifetime(r.variable.name).max }.reverse
    val activeList = PriorityQueue.empty[DefReg](IncreasingEndTime)

    // list of allocated registers so far
    val allocatedList = Queue.empty[DefReg] ++ immortals

    val moveQueue = Queue.empty[Mov]
    val hints = createHintManager(process)
    @tailrec
    def tryRelease(
        currentInterval: IntervalSet,
        freeListUsed: ArrayDeque[Int]
    ): Unit = {

      if (activeList.nonEmpty) {

        val firstActive = activeList.dequeue()
        // check if the given interval starts after the
        val mayHavePassedInterval = lifetime(firstActive.variable.name)
        if (mayHavePassedInterval.max <= currentInterval.min) { // <= because interval end in exclusive
          // the firstActive is no longer alive

          freeListUsed += firstActive.variable.id
          ctx.logger.debug(s"Freed ${firstActive.variable.name}:${firstActive.variable.id}")
          tryRelease(currentInterval, freeListUsed)

        } else {
          // there is nothing more to release, put the head back into the
          // priority queue
          activeList enqueue firstActive
        }

      }

    }

    def tryFindIdPositionInFreeList(
        lastId: Int,
        deque: ArrayDeque[Int]
    ): Option[Int] = {

      @tailrec
      def reverseIterate(index: Int = deque.length - 1): Option[Int] = {
        if (deque(index) == lastId) {
          Some(index)
        } else {
          if (index == 0) {
            None
          } else {
            reverseIterate(index - 1)
          }
        }
      }

      if (deque.isEmpty) {
        None
      } else {
        reverseIterate()
      }

    }

    @tailrec
    def allocateWhileHaveFree(
        unallocatedList: PriorityQueue[DefReg]
    ): Seq[DefReg] = {
      if (unallocatedList.nonEmpty) {
        val currentRegToAllocate = unallocatedList.dequeue()
        val currentInterval = lifetime(currentRegToAllocate.variable.name)

        val freeListUsed =
          if (currentRegToAllocate.variable.varType == ConstType) {
            freeCarryList
          } else {
            mainFreeList
          }
        // release any registers whose intervals are passed
        tryRelease(currentInterval, freeListUsed)

        if (freeListUsed.nonEmpty) {
          // look for any hint for allocation
          def allocate(removeIndex: Int) = {

            val allocationId = freeListUsed.remove(removeIndex)
            val regWithId = withId(currentRegToAllocate, allocationId)
            allocatedList += regWithId
            activeList += regWithId
            hints.tell(currentRegToAllocate.variable.name) {
              AllocationHint(allocationId, currentInterval)
            }
          }
          hints.seek(currentRegToAllocate.variable.name) match {
            case Some(lastHint) =>
              // try to use lastId
              val idPosition =
                tryFindIdPositionInFreeList(lastHint.lastId, freeListUsed)
              idPosition match {
                case Some(index) =>
                  // there is valid hint that we can use (by construction
                  // we have the guarantee the interval given in the hint
                  // is dead otherwise we would not be able to find in the
                  // freeListUsed)
                  ctx.logger.debug(s"Using hint ${lastHint.lastId} for ${currentRegToAllocate.variable.name}")
                  allocate(index)

                case None =>
                  // There is a valid hint, but we can not use it, this is because
                  // either the hint is used by some other interval or the original
                  // user is still alive. So we take a new Id from the freeListUsed
                  // and allocate the register accordingly. After register allocation
                  // we need to resolve all unrealized hints with MOVs and a bit of
                  // scheduling
                  ctx.logger.debug(s"Could not use hint ${lastHint.lastId} for ${currentRegToAllocate.variable.name}")
                  allocate(0)

              }
            case None =>
              // there was no hint, so we simply just take fresh register and
              // get on with our lives
              allocate(0)

          }

          allocateWhileHaveFree(unallocatedList)

        } else {
          // failed register allocation!
          unallocatedList.dequeueAll
        }
      } else {
        // success!
        Nil
      }
    }

    // Unallocated registers are kept in an order queue by increasing start time
    val IncreasingStartTimeOrder =
      Ordering.by[DefReg, Int] { r => lifetime(r.variable.name).min }.reverse

    val unallocated = PriorityQueue.empty[DefReg](IncreasingStartTimeOrder)
    unallocated ++= process.registers.filter { r =>
      r.variable.varType != ConstType && r.variable.varType != MemoryType
    }
    val failedToAllocate = allocateWhileHaveFree(unallocated)

    if (failedToAllocate.nonEmpty) {
      val msg = failedToAllocate
        .map { r =>
          s"${r.variable.name}: ${lifetime(r.variable.name)}"
        }
        .mkString("\n")
      ctx.logger.error(
        s"Failed to allocate registers in process ${process.id}:\n${msg}"
      )
      process
    } else {

      val withAllocations = process.copy(
        registers = allocatedList.toSeq
      )
      resolveUnrealizedHints(withAllocations, hints, lifetime)
    }

  }

  private def resolveUnrealizedHints(
      process: DefProcess,
      hints: HintManager,
      lifetime: util.LifetimeAnalysis
  )(implicit ctx: AssemblyContext): DefProcess = {
    import scala.collection.mutable.ArrayBuffer

    val registers = process.registers.map { r => r.variable.name -> r }.toMap
    val moveQueue = ArrayBuffer.empty[(Mov, IntervalSet)]

    def doIfMovNotElided(
        inst: Instruction
    )(action: (Name, Name) => Unit): Unit = {
      for (rd <- DependenceAnalysis.regDef(inst)) {
        hints.alias(rd) match {
          case None         => // nothing to do
          case Some(realRd) =>
            // inst is some instruction that write to the output
            // but we could not elide its MOV to the input using register
            // allocation hints
            val thisId = registers(rd).variable.id
            val realRdId = registers(realRd).variable.id
            if (thisId != realRdId) {
              action(rd, realRd)
              ctx.logger.debug(s"Can not elide MOV for ${rd} -> ${realRd}")
              moveQueue += (Mov(realRd, rd) -> lifetime(rd))
            }

        }
      }

    }
    def createMove(rd: Name, realRd: Name): Unit = {
      ctx.logger.debug(s"Can not elide MOV for ${rd} -> ${realRd}")
      moveQueue += (Mov(realRd, rd) -> lifetime(rd))
    }
    for (inst <- process.body) {

      inst match {
        case jtb: JumpTable =>
          for (delayedInst <- jtb.dslot) {
            doIfMovNotElided(inst)(createMove)
          }

          for (JumpCase(_, block) <- jtb.blocks) {

            for (caseInst <- block) {
              doIfMovNotElided(caseInst) { case (rd, realRd) =>
                ctx.logger.error(
                  s"Can not avoid move elision in Jump table for ${rd}!"
                )
              }
            }

          }

        case _ =>
          doIfMovNotElided(inst)(createMove)

      }
    }

    // now add the movs, ideally this requires some form scheduling to place the
    // moves in-between other instruction is there are some Nops, but I am to tired
    // of this pass for now and do a naive version in which I put the movs at the
    // end. Note that we also add a bunch of Nops before these moves to make sure
    // any read-after-write dependency through the pipeline is satisfied

    val withMoves = process.body ++ Seq.fill(LatencyAnalysis.maxLatency()) {
      Nop
    } ++ moveQueue.map(_._1)

    process.copy(
      body = withMoves
    )
  }

}