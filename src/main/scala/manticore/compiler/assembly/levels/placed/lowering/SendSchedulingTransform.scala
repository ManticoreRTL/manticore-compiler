package manticore.compiler.assembly.levels.placed.lowering

import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.placed.PlacedIRDependencyDependenceGraphBuilder.DependenceAnalysis
import manticore.compiler.assembly.levels.placed.LatencyAnalysis
import manticore.compiler.CompilationFailureException
import scala.collection.mutable.{Queue => MutableQueue}
import scala.collection.mutable.PriorityQueue
import scala.annotation.tailrec

/** A pass to globally schedule/route messages across processes.
  * @author
  *   Mahyar Emami <mahyar.emami@eplf.ch>
  */

private[lowering] object SendSchedulingTransform
    extends AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {
  val flavor = PlacedIR
  import flavor._

  private case class ScheduleEvent[T](inst: T, cycle: Int)
  private type SendEvent = ScheduleEvent[Send]
  private type RecvEvent = ScheduleEvent[Recv]

  private case class SendWrapper(
      inst: Send,
      source: ProcessId,
      target: ProcessId,
      earliest: Int,
      dimx: Int,
      dimy: Int
  ) {
    val xDist =
      LatencyAnalysis.xHops(source, target, (dimx, dimy))
    val yDist =
      LatencyAnalysis.yHops(source, target, (dimx, dimy))
    val manhattan =
      xDist + yDist

    val pathX: Seq[(Int, Int)] = // a tuple of x location and occupancy time
      Seq.tabulate(xDist) { i =>
        val x_v = (source.x + i + 1) % dimx

        /** [[LatencyAnalysis.latency]] gives the number of NOPs required
          * between two depending instruction, that is, it gives the latency
          * between executing and writing back the instruction but the [[Send]]
          * latency should also consider the number of cycles required for
          * fetching and decoding. That is why we add "2" to the number given by
          * [[LatencyAnalysis.latency]]
          */
        x_v -> (LatencyAnalysis.latency(inst) + i + 2)
      }
    val pathY: Seq[(Int, Int)] = {
      // a tuple of y location and cycle offset from the time the
      // instruction gets scheduled
      // that is, we record the tim
      val p = Seq.tabulate(yDist) { i =>
        val y_v = (source.y + i + 1) % dimy
        pathX match {
          case _ :+ last => y_v -> (pathX.last._2 + i + 1)
          case Seq() =>
            y_v -> (LatencyAnalysis.latency(inst) + i + 2)
        }
      }
      // the last hop always occupies a Y link which the is Y output of
      // the target switch (Y output is shared with the local output)
      val lastHop = p match {
        case _ :+ last => Seq(((target.y + 1) % dimy) -> (last._2 + 1))
        case Seq() if pathX.nonEmpty =>
          // this happens if the packet only goes in the X direction and the
          // packet should have at least one X hop, hence path_x can not be
          // empty (we don't have self messages so at least one the
          // the two paths are non empty)
          assert(pathX.nonEmpty)
          Seq(((target.y + 1) % dimy) -> (pathX.last._2 + 1))
        case _ =>
          throw new CompilationFailureException(
            s"Can not have self messages send ${inst.serialized}"
          )
        //   Seq.empty[(Int, Int)]
      }
      p ++ lastHop
    }
  }

  // a wrapper class for processes that contains a list of scheduled
  // non-Send instruction and non-schedule Send instructions.
  private case class ProcessWrapper(
      proc: DefProcess,
      scheduled: MutableQueue[SendEvent],
      unscheduled: MutableQueue[SendWrapper]
  ) extends Ordered[ProcessWrapper] {

    // Among processes, always prioritize Sends that have a longer
    // manhattan distance. This is much like the LIST scheduling algorithm
    // where the distance to sink is the priority.
    def compare(that: ProcessWrapper): Int =
      (this.unscheduled, that.unscheduled) match {
        case (MutableQueue(), MutableQueue()) => 0
        case (x +: _, y +: _) =>
          Ordering[Int].compare(x.manhattan, y.manhattan)
        case (MutableQueue(), y +: _) => -1
        case (x +: _, MutableQueue()) => 1
      }
    def isFinished() = unscheduled.isEmpty
  }

  private def cyclesConsumedByJumpTable(jtb: JumpTable): Int = {
    val numInstExecutedByJumpTable = 1 + // 1 + for the switch
      jtb.dslot.length +
      jtb.blocks.head.block.length
    numInstExecutedByJumpTable
  }
  private def wrapProcess(
      process: DefProcess
  )(implicit ctx: AssemblyContext): ProcessWrapper = {

    val sendsOf: Map[Name, Seq[Send]] =
      process.body
        .collect { case s: Send => s }
        .groupBy(_.rs)
        .map { case (rs, sends: Seq[Send]) =>
          // in case there are multiple Send instructions for the same value
          // prioritize the ones that have to traverse a longer distance
          val TieBreakerOrder = Ordering
            .by[Send, Int] { send =>
              LatencyAnalysis
                .manhattan(
                  process.id,
                  send.dest_id,
                  (ctx.max_dimx, ctx.max_dimy)
                )
            }
            .reverse
          rs -> sends.sorted(TieBreakerOrder)
        }
        .withDefault(_ => Seq.empty)

    // IMPORTANT: Go through the instructions IN ORDER and find the instructions
    // that define the values used in Send instruction. The cycle at which the
    // other instruction is scheduled plus the pipeline latency indicates the
    // earliest time that Send can be scheduled alone. However, if we schedule
    // one of these Sends, the later ones' earliest times will be naturally
    // incremented by one we can do this by appending the wrapped instruction to
    // a list one by one and offsetting the earliest cycle by the size of the
    // list + 1.
    case class SendQueueBuilder(
        cycle: Int = 0,
        wrapped: Seq[SendWrapper] = Seq.empty
    ) {
      // With s = wrapped.length, the first Send can be schedule
      // at cycle + 1 + n + s where n is latency of the instruction that
      // define the value to be sent the next one would be at cycle + 2
      // + n + s and so on...
      // Since we are enlarging the wrapped sequence, this increment happens
      // naturally on wrapped.length
      def appended(send: Send, latency: Int) = {
        val newSend = SendWrapper(
          inst = send,
          source = process.id,
          target = send.dest_id,
          earliest = cycle + 1 + latency + wrapped.length,
          dimx = ctx.max_dimx,
          dimy = ctx.max_dimy
        )
        copy(wrapped = wrapped :+ newSend)
      }
      def advanced(n: Int) = copy(cycle = cycle + n)
    }

    val wrappedSends = process.body
      .foldLeft(SendQueueBuilder()) {

        case (builder, jtb @ JumpTable(_, results, blocks, dslot, _)) =>
          val numInstExecutedByJumpTable = cyclesConsumedByJumpTable(jtb)
          // the current jump defines a value that is supposed to be sent
          // (maybe to multiple destinations)
          // We can only schedule the send after the JumpTable is fully
          // executed and if we send one, the next send can be scheduled
          // a cycle later at earliest

          // to make sure that the last instruction in the JumpTable
          // has written its value, you can look at it as the
          // latency of the "phi". Note that this is a conservative
          // estimation of the earliest time the first send can
          // be scheduled.
          val sendsOfJtb = DependenceAnalysis.regDef(jtb).flatMap { sendsOf }

          sendsOfJtb
            .foldLeft(
              builder advanced numInstExecutedByJumpTable
            ) { case (bldr, snd) =>
              bldr appended (snd, LatencyAnalysis.maxLatency())
            }

        case (builder, inst) =>
          val sendsOfInst = DependenceAnalysis.regDef(inst).flatMap { sendsOf }
          sendsOfInst.foldLeft(
            builder advanced 1
          ) { case (bldr, snd) =>
            bldr appended (snd, LatencyAnalysis.latency(inst))
          }

      }
      .wrapped

    ProcessWrapper(
      process,
      MutableQueue.empty[SendEvent], // no send is scheduled yet
      MutableQueue.from(wrappedSends)
    )
  }

  private def createCompleteSchedule(
      process: DefProcess,
      sendEvents: Seq[SendEvent],
      recvEvents: Seq[RecvEvent]
  )(implicit ctx: AssemblyContext): DefProcess = {

    val nonSendsToSchedule = process.body.filter {
      _.isInstanceOf[Send] == false
    }

    @tailrec
    def createSchedule(
        cycle: Int,
        sends: Seq[SendEvent],
        nonSends: Seq[Instruction],
        schedule: Seq[Instruction]
    ): (Seq[Instruction], Int) = {

      if (nonSends.isEmpty && sends.isEmpty) {
        (schedule, cycle)
      } else {
        (nonSends, sends) match {
          case (_, sendInst +: sendTail) if sendInst.cycle == cycle =>
            // there is send scheduled at the current cycle
            createSchedule(
              cycle + 1,
              sendTail,
              nonSends,
              schedule :+ sendInst.inst
            )
          case (inst +: instTail, _) =>
            // no send, but some other instruction
            val increment = inst match {
              case jtb: JumpTable =>
                cyclesConsumedByJumpTable(jtb)
              case _ => 1
            }
            createSchedule(
              cycle + increment,
              sends,
              instTail,
              schedule :+ inst
            )
          case _ =>
            // nothing to schedule, put a Nop. Note that we can not have anything
            // in the nonSend list if the local schedule is correct. Here we should
            // only insert Nops if Sends get delayed, i.e., when there is nothing
            // left to schedule but sends.
            assert(
              nonSends.isEmpty,
              "Something is wrong with the local schedule!"
            )
            createSchedule(
              cycle + 1,
              sends,
              nonSends,
              schedule :+ Nop
            )
        }
      }

    }

    val (scheduleWithoutRecvs, cycleWithoutRecv: Int) = createSchedule(
      cycle = 0,
      sends = sendEvents,
      nonSends = nonSendsToSchedule,
      schedule = Seq.empty
    )

    // append the Recv instruction if any

    ctx.logger.debug {
      s"RECVs in ${process.id}:\n${recvEvents.map { i =>
        s"${i.inst}: ${i.cycle}"
      } mkString "\n"}"
    }

    @tailrec
    def createRecvSchedule(
        cycle: Int,
        receives: Seq[RecvEvent],
        schedule: Seq[Instruction]
    ): (Seq[Instruction], Int) = {

      receives match {
        case (ScheduleEvent(inst, recvCycle)) +: receivesTail =>
          if (recvCycle > cycle) {
            // insert Nops up to recvCycle, because we need to wait for
            // messages to arrive
            createRecvSchedule(
              recvCycle + 1,
              receivesTail,
              schedule ++ Seq.fill(recvCycle - cycle) { Nop } :+ inst
            )
          } else {
            // message has already arrived, no need for Nops
            createRecvSchedule(
              cycle + 1,
              receivesTail,
              schedule :+ inst
            )
          }
        case Nil => (schedule, cycle)
      }
    }

    val (scheduleWithRecvs, cycleRecvs) = createRecvSchedule(
      cycle = cycleWithoutRecv,
      receives = recvEvents,
      schedule = scheduleWithoutRecvs
    )

    // the last step is to make sure the the "last" recv is properly handled. If
    // there last recv arrives at cycle n, and we end the instruction
    // stream/virtual cycle at cycle n. This recv will not have time to be
    // translated to a Set and therefore will not carry its effect.

    // With multiple recv instruction that should not be necessary, but I am not
    // sure so I leave it as is (the cost is not much is it?:D)

    val finalSchedule = scheduleWithRecvs match {
      case ls :+ (r: Recv) => scheduleWithRecvs :+ Nop
      case _               => scheduleWithRecvs
    }

    if (cycleRecvs + 1 + LatencyAnalysis.maxLatency() >= ctx.max_instructions) {
      ctx.logger.error(
        s"Could not schedule process ${process.id} that requires ${cycleRecvs + 1}."
      )
    }
    process
      .copy(
        body = finalSchedule
      )
      .setPos(process.pos)
  }
  override def transform(
      program: DefProgram,
      context: AssemblyContext
  ): DefProgram = {

    val dimx = context.max_dimx
    val dimy = context.max_dimy

    // for every process, we collect the earliest time sends in each process
    // can be scheduled. This "earliest" time is rather different from the
    // liveness interval of registers that we wish to send.
    // This is because we assume a fixed order on how sends are scheduled within
    // each process. Sends that their value is produced earlier, are also scheduled
    // earlier and the manhattan distance is used between sends that become alive
    // at the same time to break ties (i.e., a single value may be sent to
    // multiple destinations, the ones that travel further are prioritized).

    val unfinishedProcessQueue =
      PriorityQueue.from(program.processes.map(wrapProcess(_)(context)))

    // keep a sorted queue of Recv events in increasing recv time
    // since the priority queue sorts the collection in decreasing priority
    // we need to use .reverse on the ordering

    val RecvOrdering =
      Ordering.by[RecvEvent, Int] { case ScheduleEvent(rcv, i) => i }.reverse
    val recvQueue = program.processes.map { process =>
      process.id -> PriorityQueue.empty(RecvOrdering)
    }.toMap

    def createLinks() = {
      type LinkOccupancy = scala.collection.mutable.Set[Int]
      val link = Array.ofDim[LinkOccupancy](dimx, dimy)
      for (x <- 0 until dimx; y <- 0 until dimy) {
        link(x)(y) = scala.collection.mutable.Set.empty[Int]
      }
      link
    }
    val linkX = createLinks()
    val linkY = createLinks()

    var cycle = 0
    val partiallyScheduledProcessWrappers = MutableQueue.empty[ProcessWrapper]

    while (unfinishedProcessQueue.exists(!_.isFinished())) {

      val processesToTry = unfinishedProcessQueue.dequeueAll

      for (wProc <- processesToTry) {
        assert(wProc.unscheduled.nonEmpty)

        val sendToTry = wProc.unscheduled.head
        val earliest = sendToTry.earliest
        if (earliest <= cycle) {

          val canRouteHorizontally = sendToTry.pathX.forall { case (x, t) =>
            linkX(x)(sendToTry.source.y).contains(cycle + t) == false
          }
          val canRouteVertically = sendToTry.pathY.forall { case (y, t) =>
            linkY(sendToTry.target.x)(y).contains(cycle + t) == false
          }
          if (canRouteHorizontally && canRouteVertically) {
            context.logger.debug(
              s"@${cycle}: Scheduling ${sendToTry.inst.serialized} in process ${wProc.proc.id}" +
                s"\nPath_x: ${sendToTry.pathX}\nPath_y: ${sendToTry.pathY}"
            )

            // dequeue the head (note sendToSchedule == sendToTry)
            val sendToSchedule = wProc.unscheduled.dequeue()

            // mark the links as occupied in future cycles
            for ((x, t) <- sendToSchedule.pathX) {
              linkX(x)(sendToSchedule.source.y) += (cycle + t)
            }
            for ((y, t) <- sendToSchedule.pathY) {
              linkY(sendToSchedule.target.x)(y) += (cycle + t)
            }

            val recvTime = sendToSchedule.pathY.last._2 + cycle
            val recvEvent = ScheduleEvent(
              Recv(
                rd = sendToSchedule.inst.rd,
                rs = sendToSchedule.inst.rs,
                source_id = sendToSchedule.source
              ),
              recvTime
            )
            context.logger.debug(s"Recv time ${recvTime}", recvEvent.inst)
            recvQueue(sendToSchedule.target) += recvEvent

          }

        }

        if (wProc.isFinished()) {
          partiallyScheduledProcessWrappers += wProc
        } else {
          unfinishedProcessQueue += wProc
        }

      }

      cycle += 1

    }

    // for debug
    if (context.debug_message) {
      val layout = new StringBuilder
      Range(0, dimx).foreach { x =>
        Range(0, dimy).foreach { y =>
          layout ++= s"\tlinkX(${x})(${y}) = [${linkX(x)(y).mkString(",")}]\n"
          layout ++= s"\tlinkY(${x})(${y}) = [${linkY(x)(y).mkString(",")}]\n"
        }
      }

      context.logger.debug(s"\n${layout}")

    }


    if (cycle + LatencyAnalysis.maxLatency() >= context.max_instructions) {
      context.logger.error(
        s"" +
          s"could not schedule Send instructions in ${context.max_instructions}," +
          s" scheduling requires ${cycle} cycles "
      )
    }

    val finalRecvs = recvQueue.map { case (pid, evenQueue) => evenQueue.last }
    for (lastResv <- finalRecvs) {
      if (
        lastResv.cycle + 2 * LatencyAnalysis
          .maxLatency() >= context.max_instructions
      ) {
        context.logger.error(
          s"Recv arrives too late at cycle ${lastResv.cycle}",
          lastResv.inst
        )
      }
    }

    // sort processes base on the ProcessId (mostly for readability in dumps...)

    val sortedByPid = partiallyScheduledProcessWrappers
      .sortBy(p => p.proc.id) {
        Ordering.by { pid => (pid.x, pid.y) }
      }
      .toSeq

    val fullyScheduled = for (wProc <- sortedByPid) yield {
      createCompleteSchedule(
        wProc.proc,
        wProc.scheduled.toSeq,
        recvQueue(wProc.proc.id).toSeq
      )(context)
    }

    program.copy(
      processes = fullyScheduled
    )

  }

}
