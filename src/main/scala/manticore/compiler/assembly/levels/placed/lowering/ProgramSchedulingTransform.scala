package manticore.compiler.assembly.levels.placed.lowering

import manticore.compiler.assembly.levels.placed.PlacedIRDependencyDependenceGraphBuilder.DependenceAnalysis
import manticore.compiler.assembly.levels.placed.PlacedIRInputOutputCollector.InputOutputPairs
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import scalax.collection.mutable.{Graph => MutableGraph}
import scalax.collection.Graph
import scalax.collection.GraphEdge
import manticore.compiler.assembly.annotations.Memblock
import manticore.compiler.assembly.levels.placed.LatencyAnalysis
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.assembly.levels.placed.TaggedInstruction
import scala.annotation.tailrec

private[lowering] object ProgramSchedulingTransform
    extends AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {
  import PlacedIR._
  private type DependenceGraph = Graph[Instruction, GraphEdge.DiEdge]

  def transform(program: DefProgram, context: AssemblyContext): DefProgram = {

    val prepared = context.stats.recordRunTime("preparing processes") {
      program.copy(
        processes =
          program.processes.map(prepareProcessForScheduling(_)(context))
      )
    }

    createProgramSchedule(prepared)(context)

  }

  private def numCyclesInJumpTable(jtb: JumpTable): Int = {
    jtb.blocks.head.block.length + jtb.dslot.length + 1
  }
  def createProgramSchedule(program: DefProgram)(implicit
      ctx: AssemblyContext
  ): DefProgram = {

    def computePriority(process: DefProcess): Int = {
      process.body.foldLeft(0) {
        case (p, jtb: JumpTable) =>
          p + numCyclesInJumpTable(jtb)
        case (p, _) => p + 1
      }
    }
    val processPriority =
      program.processes.map(p => p -> computePriority(p)).toMap
    val StaticOrdering = Ordering.by(processPriority).reverse
    // assign a static priority in scheduling cores, the cores with more
    // instructions should be scheduled first
    val cores = program.processes.sorted(StaticOrdering).map { createProcessor }

    val network = new NetworkOnChip(ctx.max_dimx, ctx.max_dimy)

    val getCore: ProcessId => Processor = cores.map { core =>
      core.process.id -> core
    }.toMap

    var globalCycle = 0

    def running(): Boolean =
      cores.exists { core => core.scheduleContext.finished() == false }
    // simulate/schedule one virtual cycle

    /** Advance the core state by trying to schedule one instruction. If the
      * instruction is a jump table, the core.currentCycle will jump ahead
      * making this core "disabled" until the globalCycle catches up.
      *
      * @param core
      */
    def advanceCoreState(core: Processor): Unit = {

      /** Try the ready list and see if there is any instruction that can be
        * scheduled. If there is a send that can be routed at the head of the
        * readList, this function tries to reserve a path on the network for it
        * (has side effects) and return the send instruction. If the send
        * instruction at the head is not routable. The function is called again
        * on the tail of the readList until some other (send or nonsend)
        * instruction ready to be scheduled is found. All the instructions that
        * were popped from the queue but were not scheduleable are returned.
        * These returned instruction are bound to be of type [[Send]]. The
        * second returned value is the instruction to be scheduled (could be any
        * type).
        * @param triedButFailed
        * @return
        */
      @tailrec
      def tryUntilFindReadyWithSideEffects(
          triedButFailed: Seq[DependenceGraph#NodeT] = Seq.empty
      ): (Seq[DependenceGraph#NodeT], Option[DependenceGraph#NodeT]) = {
        if (core.scheduleContext.readyList.nonEmpty) {
          val toSchedule = core.scheduleContext.readyList.dequeue()
          toSchedule.toOuter match {
            case send: Send =>
              val response =
                network.request(core.process.id, send, core.currentCycle)
              response match {
                case network.Denied =>
                  tryUntilFindReadyWithSideEffects(triedButFailed :+ toSchedule)
                case network.Granted(arrivalCycle) =>
                  getCore(send.dest_id).notifyRecv(
                    RecvEvent(
                      Recv(
                        rd = send.rd,
                        rs = send.rs,
                        source_id = core.process.id
                      ),
                      arrivalCycle
                    )
                  )
                  (triedButFailed, Some(toSchedule))
              }
            case inst => (triedButFailed, Some(toSchedule))
          }
        } else {
          (triedButFailed, None)
        }
      }

      // see if there is anything that can be committed
      val nodesToCommit = core.scheduleContext.activeList.collect {
        case (n, t) if t == core.currentCycle => n
      }
      // and remove them from the activeList
      core.scheduleContext.activeList --= nodesToCommit

      for (nodeJustCommitted <- nodesToCommit) {
        for (dependentNode <- nodeJustCommitted.diSuccessors) {
          // add any successor that only depend on the node that
          // is being committed
          if (dependentNode.inDegree == 0) {
            core.scheduleContext.readyList += dependentNode
          }
        }
        // remove the node from the graph (note that the graph acts as scoreboard)
        core.scheduleContext.graph -= nodeJustCommitted
      }
      val (triedNoLuck, ready) = tryUntilFindReadyWithSideEffects()

      ready match {
        case Some(newActive) =>
          // make the node active
          val commitCycle =
            LatencyAnalysis.latency(newActive.toOuter) + core.currentCycle
          core.scheduleContext.activeList += (newActive -> commitCycle)
        case None => // nothing to add to the active list
      }
      // schedule the active node and advance time
      ready.map(_.toOuter) match {
        case None => // there was nothing we could do mate
          core.scheduleContext.schedule += Nop
          core.currentCycle += 1
        case Some(jtb: JumpTable) =>
          core.scheduleContext.schedule += jtb
          core.currentCycle += numCyclesInJumpTable(jtb)
        case Some(send: Instruction) =>
          core.scheduleContext.schedule += send
          core.currentCycle += 1
      }
      // finally, send back any "seemingly" ready node (which is certainly of type Send)
      // to the readyList
      assert(
        triedNoLuck.forall { _.toOuter.isInstanceOf[Send] },
        "Oops! Your schedule implementation has a bug. Why are you delaying nonsend instructions?"
      )
      core.scheduleContext.readyList ++= triedNoLuck

    }

    ctx.stats.recordRunTime("Simulation loop") {
      while (running()) {
        for (core <- cores) {
          // can't have core lagging behind, only leading ahead
          assert(core.currentCycle >= globalCycle)
          if (core.currentCycle == globalCycle) {
            advanceCoreState(core)
          }
        }
        globalCycle += 1
      }
    }

    @tailrec
    def createRecvSchedule(
        cycle: Int,
        receives: Seq[RecvEvent],
        schedule: Seq[Instruction]
    ): (Seq[Instruction], Int) = {

      receives match {
        case (RecvEvent(inst, recvCycle)) +: receivesTail =>
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

    // schedule the receives
    val scheduledProcesses =
      ctx.stats.recordRunTime("schedule finalization") {
        cores.map { finalizeScheduleWithReceives }.sortBy { p =>
          (p.id.x, p.id.y)
        }
      }
    program
      .copy(
        processes = scheduledProcesses
      )
      .setPos(program.pos)
  }

  private def finalizeScheduleWithReceives(
      core: Processor
  )(implicit ctx: AssemblyContext): DefProcess = {

    // append recvs to the schedule
    @tailrec
    def createRecvSchedule(
        cycle: Int,
        receives: Seq[RecvEvent],
        schedule: Seq[Instruction]
    ): (Seq[Instruction], Int) = {

      receives match {
        case (RecvEvent(inst, recvCycle)) +: receivesTail =>
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
    val sortedRecvs = core.getReceivesSorted()
    val currentSched = core.scheduleContext.schedule.toSeq
    val currentCycle = core.currentCycle

    val (withRecvs, finalCycle) =
      createRecvSchedule(currentCycle, sortedRecvs, currentSched)

    // the last step is to make sure the the "last" recv is properly handled. If
    // there last recv arrives at cycle n, and we end the instruction
    // stream/virtual cycle at cycle n. This recv will not have time to be
    // translated to a Set and therefore will not carry its effect.

    // With multiple recv instruction that should not be necessary, but I am not
    // sure so I leave it as is (the cost is not much is it?:D)

    val finalSchedule = withRecvs match {
      case ls :+ (r: Recv) => withRecvs :+ Nop
      case _               => withRecvs
    }

    if (finalCycle + 1 + LatencyAnalysis.maxLatency() >= ctx.max_instructions) {
      ctx.logger.error(
        s"Could not schedule process ${core.process.id} that requires ${finalCycle + 1}."
      )
    }

    core.process
      .copy(
        body = finalSchedule
      )
      .setPos(core.process.pos)

  }
  private class NetworkOnChip(dimX: Int, dimY: Int) {

    sealed abstract trait Response
    case object Denied extends Response
    case class Granted(arrival: Int) extends Response

    // the start time given should be the cycle at which a Send gets scheduled

    // plus Latency

    case class Step(loc: Int, t: Int)
    case class Path(from: ProcessId, send: Send, scheduleCycle: Int) {

      val to = send.dest_id
      val xDist =
        LatencyAnalysis.xHops(from, to, (dimX, dimY))
      val yDist =
        LatencyAnalysis.yHops(from, to, (dimX, dimY))

      /** [[LatencyAnalysis.latency]] gives the number of NOPs required between
        * two depending instruction, that is, it gives the latency between
        * executing and writing back the instruction but the [[Send]] latency
        * should also consider the number of cycles required for fetching and
        * decoding. That is why we add "2" to the number given by
        * [[LatencyAnalysis.latency]]
        */
      val enqueueTime = LatencyAnalysis.latency(send) + 2 + scheduleCycle
      val xHops: Seq[Step] =
        Seq.tabulate(xDist) { i =>
          val x_v = (from.x + i + 1) % dimX

          Step(x_v, (enqueueTime + i))
        }
      val yHops: Seq[Step] = {

        val p = Seq.tabulate(yDist) { i =>
          val y_v = (from.y + i + 1) % dimY
          xHops match {
            case _ :+ last => Step(y_v, (xHops.last.t + i + 1))
            case Seq() => // there are no steps in the X direction
              Step(y_v, (enqueueTime + i))
          }
        }
        // the last hop always occupies a Y link which the is Y output of
        // the target switch (Y output is shared with the local output)
        val lastHop = p match {
          case _ :+ last => Step((to.y + 1) % dimY, (last.t + 1))
          case Nil       =>
            // this happens if the packet only goes in the X direction. The
            // packet should have at least one X hop, hence xHops can not be
            // empty (we don't have self messages so at least one the
            // the two paths are non empty)
            assert(
              xHops.nonEmpty,
              s"Can not have self messages send ${send.serialized}"
            )
            Step((to.y + 1) % dimY, (xHops.last.t + 1))
        }
        p :+ lastHop
      }

    }
    private type LinkOccupancy = scala.collection.mutable.Set[Int]
    private val linksX = Array.ofDim[LinkOccupancy](dimX, dimY)
    private val linksY = Array.ofDim[LinkOccupancy](dimX, dimY)

    // initially no link is occupied
    for (x <- 0 until dimX; y <- 0 until dimY) {
      linksX(x)(y) = scala.collection.mutable.Set.empty[Int]
      linksY(x)(y) = scala.collection.mutable.Set.empty[Int]
    }

    /** called by a processor to try to enqueue a Send to the NoC
      *
      * @param path
      *   the path the message should traverse
      * @return
      */
    def request(from: ProcessId, send: Send, scheduleCycle: Int): Response =
      request(Path(from, send, scheduleCycle))
    def request(path: Path): Response = {
      val canRouteHorizontally = path.xHops.forall { case Step(x, t) =>
        linksX(x)(path.from.y).contains(t) == false
      }
      val canRouteVertically = path.yHops.forall { case Step(y, t) =>
        linksY(path.to.x)(y).contains(t) == false
      }
      if (canRouteVertically && canRouteHorizontally) {
        // reserve the links
        for (Step(x, t) <- path.xHops) {
          linksX(x)(path.from.y) += t
        }
        for (Step(y, t) <- path.yHops) {
          linksY(path.to.x)(y) += t
        }
        Granted(path.yHops.last.t) // you lucky bastard
      } else {
        Denied // you've been served sir
      }
    }

  }

  private final class ScheduleContext(
      dependenceGraph: DependenceGraph,
      priority: Ordering[DependenceGraph#NodeT]
  ) {

    val graph = {
      // a copy of the original dependence graph excluding the Send instructions

      val builder = MutableGraph.empty[Instruction, GraphEdge.DiEdge]
      builder ++= dependenceGraph.nodes.filter(!_.isInstanceOf[Send])
      builder ++= dependenceGraph.edges.filter {
        case GraphEdge.DiEdge(_, _: Send) => false
        case _                            => true
      }
      builder
    }

    val schedule = scala.collection.mutable.Queue.empty[Instruction]

    // pre-populate the ready list
    val readyList = scala.collection.mutable.PriorityQueue
      .empty[DependenceGraph#NodeT](priority) ++ graph.nodes.filter {
      _.inDegree == 0
    }

    val activeList =
      scala.collection.mutable.Map.empty[DependenceGraph#NodeT, Int]

    def finished(): Boolean = graph.nonEmpty

  }

  case class RecvEvent(recv: Recv, cycle: Int)
  private class Processor(
      val process: DefProcess,
      val scheduleContext: ScheduleContext
  ) {
    var currentCycle: Int = 0
    // keep a sorted queue of Recv events in increasing recv time
    // since the priority queue sorts the collection in decreasing priority
    // we need to use .reverse on the ordering
    private val recvQueue = scala.collection.mutable.Queue.empty[RecvEvent]

    def checkCollision(recvEv: RecvEvent): Option[RecvEvent] =
      recvQueue.find(_.cycle == recvEv.cycle)

    def notifyRecv(recvEv: RecvEvent): Unit = {
      assert(checkCollision(recvEv).isEmpty, "Collision in recv port")
      recvQueue enqueue recvEv
    }

    // get the RecvEvents in increasing time order
    def getReceivesSorted(): Seq[RecvEvent] =
      recvQueue.toSeq.sorted(Ordering.by { recvEv: RecvEvent =>
        recvEv.cycle
      })
  }
  private def createProcessor(
      process: DefProcess
  )(implicit ctx: AssemblyContext): Processor = {
    val definingInstruction =
      DependenceAnalysis.definingInstructionMap(process)
    val dependenceGraph =
      ctx.stats.recordRunTime("building process dependence graph") {
        createDependenceGraph(process.body, definingInstruction)
      }

    val priorities = ctx.stats.recordRunTime("estimating priorities") {
      estimatePriority(dependenceGraph) { targetId =>
        // penalty of Send is a SetValue instruction and the number of hops
        // in the NoC
        LatencyAnalysis.manhattan(
          process.id,
          targetId,
          (ctx.max_dimx, ctx.max_dimy)
        ) + LatencyAnalysis.latency(SetValue("", UInt16(0)))
      }
    }
    val processor =
      new Processor(process, new ScheduleContext(dependenceGraph, priorities))
    processor
  }

  private def prepareProcessForScheduling(process: DefProcess)(implicit
      ctx: AssemblyContext
  ): DefProcess = {
    val statePairs = InputOutputPairs
      .createInputOutputPairs(process)
      .map { case (curr, next) =>
        curr.variable.name -> next.variable.name
      }
      .toMap

    val newBody = process.body
      .filter { inst =>
        inst match {
          case Mov(rd, rs, _) =>
            if (statePairs.contains(rd)) {
              ctx.logger.warn("removing MOV to input", inst)
              false
            } else {
              true
            }
          case Nop =>
            ctx.logger.warn("removing nops")
            false
          case _ =>
            if (
              DependenceAnalysis.regDef(inst).exists(statePairs.contains(_))
            ) {
              ctx.logger.error("unexpected write to input register", inst)
              false
            } else {
              true
            }
        }

      }
      .flatMap {
        case jtb @ JumpTable(_, _, blocks, delaySlot, _) =>
          val newJtb = jtb.copy(
            blocks = blocks.map { case JumpCase(lbl, blk) =>
              JumpCase(lbl, blk.filter { _.isInstanceOf[BreakCase] == false })
            },
            dslot = Seq.empty[Instruction]
          )
          val schedJtb = createJumpTableSchedule(newJtb)
          delaySlot :+ schedJtb
        case inst => Seq(inst)
      }

    process
      .copy(
        body = newBody
      )
      .setPos(process.pos)

  }

  private def createDependenceGraph(
      instructionBlock: Seq[Instruction],
      definingInstruction: Map[Name, Instruction]
  )(implicit ctx: AssemblyContext): DependenceGraph = {

    def getMemoryBlock(inst: Instruction) = inst.annons.collectFirst {
      case mb: Memblock => mb
    }

    val blockStores: MemoryBlock => Seq[Instruction] = instructionBlock
      .collect { case store @ (_: LocalStore | _: GlobalStore) =>
        getMemoryBlock(store) match {
          case None =>
            ctx.logger.error(
              s"missing a valid @${Memblock.name} annotation",
              store
            )
            Option.empty[MemoryBlock] -> store
          case Some(mblock) =>
            Some(MemoryBlock.fromAnnotation(mblock)) -> store
        }
      }
      .collect { case (Some(mb) -> store) => mb -> store }
      .groupMap { case (mb -> store) => mb } { case (mb -> store) => store }
      .withDefault(_ => Seq.empty[Instruction])

    val dependenceGraph = MutableGraph.empty[Instruction, GraphEdge.DiEdge]

    for (instruction <- instructionBlock) {
      dependenceGraph += instruction

      // create register-to-register RAW dependencies
      for (use <- DependenceAnalysis.regUses(instruction)) {
        for (defInst <- definingInstruction.get(use)) {
          val latency =
            dependenceGraph += GraphEdge.DiEdge(defInst -> instruction)
        }
      }
      // create load-to-store dependencies, i.e., make stores dependent on load
      // that access the same memory block
      instruction match {
        case load @ (_: LocalLoad | _: GlobalLoad) =>
          getMemoryBlock(load) match {
            case None =>
              ctx.logger.error(
                s"Missing a valid @${Memblock.name} annotation",
                load
              )
            case Some(mb) =>
              val mblock = MemoryBlock.fromAnnotation(mb)
              for (store <- blockStores(mblock))
                dependenceGraph += GraphEdge.DiEdge(load -> store)
          }
        case _ => // nothing else to do
      }
    }

    dependenceGraph
  }

  private def estimatePriority(
      dependenceGraph: DependenceGraph
  )(sendPenalty: ProcessId => Int) = {

    val sinks = dependenceGraph.nodes.filter { _.outDegree == 0 }
    val distance =
      scala.collection.mutable.Map.empty[DependenceGraph#NodeT, Int] ++
        sinks.map { inner =>
          // set the sink node distances. Nodes that have no further users
          // are likely to have lower priority since they can scheduled later.
          // There are two exceptions:
          // 1. Expect nodes have the highest priority since we want to schedule
          // them before anything else happens. This is not a "must" but is a good
          // idea because it will make exceptions less imprecise. However, it is
          // crucial to order ExpectStop after ExpectFail since we do not want to
          // mask failure. Note that this is also not a must, since a user Verilog
          // code may perhaps wrongfully place an assert after $stop or $finish
          // but right now we can not have those things.
          // 2. Send nodes may have a very high priority if their target is far
          // the function setPenalty should consider a penalty for Send instructions
          // based on their target process.
          val dist = inner.toOuter match {
            case expect: Expect =>
              // we want to make sure assertions fire before anything else
              // happens in a virtual cycle, so we give them the highest priority
              // possible
              expect.error_id.kind match {
                case ExpectFail =>
                  Int.MaxValue // highest possible priority
                case ExpectStop => (Int.MaxValue - 1) // second highest
              }
            case send: Send =>
              // we penalize the send latency by a user defined function,
              // this penalty will most likely be the manhattan distance plus
              // some added latency for the SET instruction in the destination
              LatencyAnalysis.latency(send) + sendPenalty(send.dest_id)
            case inst => LatencyAnalysis.latency(inst)

          }
          inner -> dist
        }

    val toVisit =
      scala.collection.mutable.Queue
        .empty[DependenceGraph#NodeT] ++ sinks.flatMap { inner =>
        inner.diPredecessors
      }
    // do a bfs
    while (toVisit.nonEmpty) {

      val currentNode = toVisit.dequeue()

      val dist = currentNode.diSuccessors.map { distance }.max

      distance += (currentNode -> dist)

      assert(
        currentNode.diPredecessors.forall(n => !distance.contains(n)),
        "graph is cyclic"
      )

      toVisit ++= currentNode.diPredecessors

    }

    Ordering.by { distance }
  }

  /** Schedule the jump cases.
    *
    * This method does not try to bring in instructions from outside and should
    * be called before the process containing this jump table is scheduled.
    * @param jtb
    * @param ctx
    * @return
    */

  private def createJumpTableSchedule(
      jtb: JumpTable
  )(implicit ctx: AssemblyContext): JumpTable = {

    require(jtb.dslot.isEmpty, "Did not expect a populated delay slot")

    val jumpDelaySlotSize = 2
    val breakDelaySlotSize = 2

    val scheduledBlocks = for (JumpCase(lbl, block) <- jtb.blocks) yield {

      val dependenceGraph = ctx.stats.recordRunTime(s"$lbl dependence graph") {
        val definingInstruction = DependenceAnalysis.definingInstruction(block)
        createDependenceGraph(block, definingInstruction)
      }
      val priorities = ctx.stats.recordRunTime(s"$lbl estimating priorities") {
        estimatePriority(dependenceGraph) { _ =>
          ctx.logger.fail("did not expect Send inside jump table!")
        }
      }

      val scheduleContext = new ScheduleContext(dependenceGraph, priorities)
      var time: Int = 0
      while (!scheduleContext.finished()) {
        // look at the activeList at collect the nodes that are ready to be committed
        val nodesToCommit = scheduleContext.activeList.collect {
          case (n, t) if t == time => n
        }
        // and remove them from the active list
        scheduleContext.activeList --= nodesToCommit

        // for any of the nodes being committed
        for (nodeBeingCommitted <- nodesToCommit) {
          for (dependentNode <- nodeBeingCommitted.diSuccessors) {
            // add any successor that only depend on the node that
            // is being committed
            if (dependentNode.inDegree == 0) {
              scheduleContext.readyList += dependentNode
            }
          }
          // then remove the node from the graph
          scheduleContext.graph -= nodeBeingCommitted
        }

        // now try to schedule an instruction
        if (scheduleContext.readyList.isEmpty) {

          // tough luck, there is nothing to do
          scheduleContext.schedule += Nop

        } else {

          val toSchedule = scheduleContext.readyList.dequeue()

          ctx.logger.debug(
            s"@$time: Scheduling ${toSchedule.toOuter.serialized}"
          )
          val commitTime = time + LatencyAnalysis.latency(toSchedule.toOuter)

          scheduleContext.schedule += toSchedule.toOuter
          scheduleContext.activeList += (toSchedule -> commitTime)

        }
        time += 1
      }

      val inBreakDelaySlot =
        scheduleContext.schedule.takeRight(breakDelaySlotSize)
      val len = scheduleContext.schedule.length
      val beforeBreakDelaySlot =
        scheduleContext.schedule.take(len - breakDelaySlotSize)
      // put a break in the middle
      val schedBlk =
        (beforeBreakDelaySlot.toSeq :+ BreakCase(-1)) ++ inBreakDelaySlot.toSeq
      JumpCase(lbl, schedBlk)

    }

    val caseLength = scheduledBlocks.maxBy(_.block.length).block.length
    val paddedBlocks = scheduledBlocks.map { case JumpCase(lbl, blk) =>
      JumpCase(lbl, Seq.fill(caseLength - blk.length) { Nop } ++ blk)
    }
    jtb
      .copy(
        dslot = Seq.fill(jumpDelaySlotSize) { Nop },
        blocks = paddedBlocks
      )
      .setPos(jtb.pos)

  }

}
