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
import scalax.collection.mutable.GraphBuilder
import scalax.collection.config.CoreConfig
import manticore.compiler.assembly.levels.placed.TaggedInstruction

private[lowering] object InstructionScheduling
    extends AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {
  import PlacedIR._

  def transform(program: DefProgram, context: AssemblyContext): DefProgram = {

    program.copy(
      processes = program.processes.map(transform(_)(context))
    )

  }

  def transform(
      process: DefProcess
  )(implicit ctx: AssemblyContext): DefProcess = {

    // remove Nops, potential state updates, breaks inside jumps, and empty out
    // delay slots
    val preprocessed = prepareProcessForScheduling(process)

    val withScheduledJumpTables = preprocessed.body.map {
      case jtb: JumpTable =>
        createJumpTableSchedule(jtb)
      case inst => inst
    }

    val finalSchedule = createProcessSchedule(
      process.copy(body = withScheduledJumpTables).setPos(process.pos)
    )

    finalSchedule
  }
  type DependenceGraph = Graph[Instruction, GraphEdge.DiEdge]
  private def prepareProcessForScheduling(process: DefProcess)(implicit
      ctx: AssemblyContext
  ): DefProcess = {
    val statePairs = InputOutputPairs
      .createInputOutputPairs(process)
      .map { case (curr, next) =>
        curr.variable.name -> next.variable.name
      }
      .toMap

    // val
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
          delaySlot :+ newJtb
        case inst => Seq(inst)
      }

    process
      .copy(
        body = newBody
      )
      .setPos(process.pos)

  }
  private case class LatencyLabel(l: Int)
  private def createDependenceGraph(
      instructionBlock: Seq[Instruction],
      definingInstruction: Map[Name, Instruction]
  )(implicit ctx: AssemblyContext): DependenceGraph = {

    // val definingInstruction = DependenceAnalysis.definingInstructionMap(process)

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

  private final class ScheduleContext(
      dependenceGraph: DependenceGraph,
      priority: Ordering[DependenceGraph#NodeT],
      initTime: Int = 0
  )(implicit ctx: AssemblyContext) {

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

    // val activeList = new ActiveList()

    val activeList =
      scala.collection.mutable.Map.empty[DependenceGraph#NodeT, Int]

    def finished(): Boolean = graph.nonEmpty

  }

  private def trySchedule(time: Int, scheduleContext: ScheduleContext)(implicit
      ctx: AssemblyContext
  ): Instruction = {

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

      Nop
    } else {

      val toSchedule = scheduleContext.readyList.dequeue()

      ctx.logger.debug(
        s"@$time: Scheduling ${toSchedule.toOuter.serialized}"
      )
      val commitTime = time + LatencyAnalysis.latency(toSchedule.toOuter)

      scheduleContext.schedule += toSchedule.toOuter
      scheduleContext.activeList += (toSchedule -> commitTime)

      toSchedule.toOuter
    }

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
        trySchedule(time, scheduleContext)
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

  private def createProcessSchedule(
      process: DefProcess
  )(implicit ctx: AssemblyContext): DefProcess = {

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

    // we have already scheduled the jump tables
    val scheduleContext = new ScheduleContext(dependenceGraph, priorities)
    // scheduling the process is slightly more tricky when we schedule a jump
    // table.

    var time: Int = 0

    while (!scheduleContext.finished()) {
      trySchedule(time, scheduleContext) match {
        case jtb: JumpTable => // do something
        case _              => // do nothing
      }
      time += 1
    }

    val scheduled = scheduleContext.schedule.toSeq ++ process.body.filter {
      _.isInstanceOf[Send]
    }

    val indexed = TaggedInstruction.indexedTaggedBlock(scheduled)

    if (indexed.length > ctx.max_instructions_threshold) {
      ctx.logger.error(
        s"Failed to schedule process ${process.id} which requires ${indexed.length} instructions"
      )
    }

    process
      .copy(
        body = scheduled
      )
      .setPos(process.pos)

  }
}
