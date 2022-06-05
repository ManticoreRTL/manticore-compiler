package manticore.compiler.assembly.levels.placed.lowering

import manticore.compiler.assembly.levels.placed.PlacedIRChecker
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.assembly.levels.InputType
import manticore.compiler.assembly.levels.MemoryType
import manticore.compiler.assembly.levels.ConstType
import manticore.compiler.assembly.levels.placed.Helpers.NameDependence
import manticore.compiler.assembly.levels.placed.LatencyAnalysis
import manticore.compiler.assembly.levels.placed.lowering.util.NetworkOnChip

object AbstractExecution extends PlacedIRChecker {
  import PlacedIR._
  override def check(source: DefProgram)(implicit
      context: AssemblyContext
  ): Unit =  {
    source.processes.foreach(checkReadAfterWrite)
    checkLinkUtilization(source)
  }

  def checkReadAfterWrite(
      process: DefProcess
  )(implicit ctx: AssemblyContext): Unit = {

    val definitionTime = scala.collection.mutable.Map.empty[Name, Int] ++
      process.registers.collect {
        case r
            if r.variable.varType == InputType ||
              r.variable.varType == MemoryType ||
              r.variable.varType == ConstType =>
          r.variable.name -> -1
      }

    def doCycle(instructionBlocK: Seq[Instruction], cycle: Int = 0): Int =
      instructionBlocK match {
        case instr +: tail =>
          val nextCycle = instr match {
            case jtb @ JumpTable(_, results, blocks, dslot, _) =>
              val cycleAfterDelaySlot = doCycle(dslot, cycle)
              val finalCycle = blocks.map { case JumpCase(_, blk) =>
                doCycle(blk, cycleAfterDelaySlot)
              }

              if (!finalCycle.tail.forall(_ == finalCycle.head)) {
                ctx.logger.error("Imbalanced block schedule!", jtb)
              }
              for (Phi(rd, rss) <- results) {
                val rdTime = rss.map { case (_, name) =>
                  definitionTime.get(name) match {
                    case Some(t) => t
                    case None =>
                      ctx.logger.error(s"${name} is not defined!", jtb)
                      0
                  }
                }.max
                definitionTime += (rd -> rdTime)
              }
              finalCycle.max
            case _ =>
              // report an error if the schedule reads a value too early
              for (use <- NameDependence.regUses(instr)) {
                definitionTime.get(use) match {
                  case Some(t) if cycle <= t =>
                    ctx.logger.error(
                      s"${use} is available at ${t + 1} but read at ${cycle}",
                      instr
                    )
                  case _ => // nothing to do, read is valid
                }
              }
              for (newVal <- NameDependence.regDef(instr)) {
                if (definitionTime.contains(newVal) && definitionTime(newVal) >= 0) {
                  ctx.logger.error(
                    s"${newVal} is defined multiple times!",
                    instr
                  )
                }
                definitionTime += (newVal -> (LatencyAnalysis.latency(instr)))
              }
              cycle + 1
          }
          doCycle(tail, nextCycle)
        case Nil =>
          cycle
      }

    doCycle(process.body)
  }

  def checkLinkUtilization(program: DefProgram)(implicit
      ctx: AssemblyContext
  ): Unit = {

    val messages = scala.collection.mutable.Map.empty[Send, (Int, ProcessId)]
    def doCycle(instructionBlock: Seq[Instruction], cycle: Int = 0)(implicit
        pid: ProcessId
    ): Int =
      instructionBlock match {
        case instr +: tail =>
          val nextCycle = instr match {
            case send: Send =>
              messages.get(send) match {
                case Some((v, _)) if v != cycle =>
                  // double send?
                  ctx.logger.error(
                    s"Double send! Already dispatched at ${v} but trying to dispatch again at ${cycle}",
                    send
                  )
                case _ => // ok
              }
              messages += (send -> (cycle, pid))
              cycle + 1
            case jtb @ JumpTable(_, _, blocks, dslot, _) =>
              val c1 = doCycle(dslot, cycle)
              val c2 = blocks.map { case JumpCase(_, blk) =>
                doCycle(blk, c1)
              }.max
              c2
            case _ => cycle + 1
          }
          doCycle(tail, nextCycle)
        case Nil => cycle
      }
    // abstractly execute the program and collect all the messages
    program.processes.foreach { p => doCycle(p.body)(p.id) }

    val noc = new NetworkOnChip(ctx.max_dimx, ctx.max_dimy)
    // Now we know exactly when each message was scheduled. We can try to reserve
    // path in the NoC for them
    for ((send, (t, pid)) <- messages) {
      noc.tryReserve(pid, send, t) match {
        case None =>
          ctx.logger.info(
            s"Could not reserve path in process ${pid} at ${t}",
            send
          )
        case Some(path) => noc.request(path)
      }
    }
  }

}
