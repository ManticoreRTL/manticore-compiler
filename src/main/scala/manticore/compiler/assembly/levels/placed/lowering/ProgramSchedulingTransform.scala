package manticore.compiler.assembly.levels.placed.lowering

import manticore.compiler.assembly.levels.placed.PlacedIRDependencyDependenceGraphBuilder.DependenceAnalysis
import manticore.compiler.assembly.levels.placed.PlacedIRInputOutputCollector.InputOutputPairs
import manticore.compiler.assembly.levels.placed.PlacedIRRenamer.Rename
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.assembly.levels.placed.PlacedIRTransformer
import manticore.compiler.AssemblyContext
import scalax.collection.mutable.{Graph => MutableGraph}
import scalax.collection.Graph
import scalax.collection.GraphEdge
import manticore.compiler.assembly.annotations.Memblock
import manticore.compiler.assembly.levels.placed.LatencyAnalysis
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.assembly.levels.placed.TaggedInstruction
import scala.annotation.tailrec
import manticore.compiler.assembly.levels.WireType
import manticore.compiler.assembly.levels.placed.lowering.util.Processor
import manticore.compiler.assembly.levels.placed.lowering.util.ScheduleContext
import manticore.compiler.assembly.levels.placed.lowering.util.NetworkOnChip
import manticore.compiler.assembly.levels.placed.lowering.util.RecvEvent

/** Program scheduler pass
  *
  * A List scheduling inspired scheduling algorithm that performs:
  *   1. Nop insertion, to handle instruction latencies 2. Send scheduling, to
  *      handle flow-control NoC traffic 3. Predicate insertion, to have
  *      explicit store predicates 4. JumpTable optimization, to fill in the Nop
  *      gaps inside jump cases
  *
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */
private[lowering] object ProgramSchedulingTransform
    extends PlacedIRTransformer {
  import PlacedIR._

  private val jumpDelaySlotSize = 2
  private val breakDelaySlotSize = 2
  def transform(
      program: DefProgram
  )(implicit context: AssemblyContext): DefProgram = {

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

      // try popping the readyList until we find an instruction that is accepted
      // by the given matcher
      @tailrec
      def findCompatible[T](
          matcher: Instruction => Option[T],
          notMatched: Seq[Processor.DependenceGraph#NodeT] = Seq.empty
      ): Option[T] = {
        if (core.scheduleContext.readyList.nonEmpty) {
          val toSchedule = core.scheduleContext.readyList.dequeue()
          val matched = matcher(toSchedule.toOuter)
          if (matched.nonEmpty) {
            core.scheduleContext.readyList ++= notMatched
            matched
          } else {
            findCompatible(matcher, notMatched :+ toSchedule)
          }
        } else {
          None
        }
      }

      sealed trait MatchResult {
        val inst: Instruction
      }
      case class SendMatch(inst: Send, path: network.Path) extends MatchResult
      case class AnyMatch(inst: Instruction) extends MatchResult

      def matchSend(send: Send) = {
        val path =
          network.tryReserve(core.process.id, send, core.currentCycle)
        if (path.nonEmpty) {
          Some(SendMatch(send, path.get))
        } else {
          None
        }
      }

      def matchSendOrAnyInst(inst: Instruction): Option[MatchResult] =
        inst match {
          case send: Send =>
            matchSend(send)
          case inst: Instruction => Some(AnyMatch(inst))
        }

      def matchAnyButStore(inst: Instruction): Option[MatchResult] =
        inst match {
          case send: Send                          => matchSend(send)
          case _ @(_: LocalStore | _: GlobalStore) => None
          case inst                                => Some(AnyMatch(inst))
        }

      def tryCommitting(): Unit = {
        val nodesToCommit = core.scheduleContext.activeList.collect {
          case (n, t) if t <= core.currentCycle => n
        }
        // and remove them from the activeList
        core.scheduleContext.activeList --= nodesToCommit

        for (nodeJustCommitted <- nodesToCommit) {
          for (dependentNode <- nodeJustCommitted.diSuccessors) {
            // add any successor that only depend on the node that
            // is being committed
            if (dependentNode.inDegree == 1) {
              core.scheduleContext.readyList += dependentNode
            }
          }
          // remove the node from the graph (note that the graph acts as scoreboard)
          core.scheduleContext.graph -= nodeJustCommitted
        }
      }

      def storePredicate(store: Instruction) = store match {
        case lstore: LocalStore  => lstore.predicate
        case gstore: GlobalStore => gstore.predicate
        case _                   => None
      }
      def removePredicate(store: Instruction) = store match {
        case lstore: LocalStore  => lstore.copy(predicate = None)
        case gstore: GlobalStore => gstore.copy(predicate = None)
        case _                   => store
      }

      def notifyReceiver(ready: Option[MatchResult]): Unit = ready match {
        case Some(SendMatch(send, path)) =>
          val recvEvent = network.request(path)
          getCore(send.dest_id).notifyRecv(recvEvent)
        case _ => //nothing to do
      }

      def activateInstruction(inst: Instruction): Unit = {
        // make the node active
        val commitCycle =
          LatencyAnalysis.latency(
            inst
          ) + core.currentCycle + 1

        core.scheduleContext.activeList += (core.scheduleContext.graph
          .get(inst) -> commitCycle)

        ctx.logger.info(
          s"@${core.process.id}:${core.currentCycle}: ${inst}"
        )
        ctx.logger.info(s"commits @ ${commitCycle}")
      }
      def activateReady(ready: Option[MatchResult]): Unit = ready match {
        case Some(newActive) =>
          // make the node active
          activateInstruction(newActive.inst)
          notifyReceiver(ready)
        case None => //nothing to do
      }
      def scheduleReady(ready: Option[MatchResult]): Unit = ready match {
        case Some(newActive) =>
          newActive.inst match {
            case jtb: JumpTable =>
              // do not push jtb into the schedule yet
              core.state = Processor.DelaySlot(jtb, 0)
              core.jtbBuilder = new Processor.JtbBuilder(jtb)
              core.currentCycle += 1
            case store @ (_: LocalStore | _: GlobalStore) =>
              storePredicate(store) match {
                case Some(p) =>
                  if (!core.hasPredicate(p)) {
                    core.activatePredicate(p)
                    core.scheduleContext += Predicate(p)
                    core.scheduleContext += removePredicate(store)
                    core.currentCycle += 2
                  } else {
                    core.scheduleContext += removePredicate(store)
                    core.currentCycle += 1
                  }
                case None =>
                  ctx.logger.error("Expected predicated store!")
                  core.scheduleContext += store
                  core.currentCycle += 1
              }
            case anyInst: Instruction =>
              core.scheduleContext += anyInst
              core.currentCycle += 1
          }
        case None => // nothing to add to the active list
          core.scheduleContext += Nop
          core.currentCycle += 1
      }
      // see if there is anything that can be committed
      tryCommitting()

      core.state match {
        case Processor.MainBlock =>
          // then try to get an instruction to schedule it
          val ready = findCompatible(matchSendOrAnyInst)
          activateReady(ready)
          scheduleReady(ready)

        case Processor.DelaySlot(jtb, pos) =>
          // put a single and only a single instruction at the current position
          // i.e., this excludes LocalStore and GlobalStore because those ones
          // unpack into two instructions
          val ready = findCompatible(matchAnyButStore)
          val newInst = ready.map(_.inst).getOrElse(Nop)
          activateReady(ready)
          core.scheduleContext +?= newInst // tell the context we scheduling
          // a proxy for the original instruction, needed for correct "finish"
          // condition
          core.currentCycle += 1
          core.jtbBuilder.dslot += newInst

          if (pos == jumpDelaySlotSize - 1) {
            core.state = Processor.CaseBlock(jtb, 0)
          } else {
            core.state = Processor.DelaySlot(jtb, pos + 1)
          }
        case Processor.CaseBlock(jtb, pos) =>
          val maxPos = core.jtbBuilder.blocks.head._2.length - 1

          // we can only bring new instruction from outside if all blocks
          // doing a Nop right now
          val canScheduleInstFromOutside = core.jtbBuilder.blocks.forall {
            case (lbl, blk) =>
              blk(pos) == Nop
          }

          if (canScheduleInstFromOutside) {
            val ready = findCompatible(matchAnyButStore)
            val newInst = ready.map(_.inst).getOrElse(Nop)
            activateReady(ready)
            // notify the context that this newInst is being scheduled
            // in an opaque way
            core.scheduleContext +?= newInst

            if (newInst != Nop) {
              ctx.logger.debug("Bringing in instruction to JumpTable", newInst)

            }
            // need to rename the output of newInst to a fresh name in every
            // case block
            val phiOutputs = DependenceAnalysis.regDef(newInst)
            val phiInputs =
              for ((label, caseBlock) <- core.jtbBuilder.blocks) yield {

                val newOutputNames = phiOutputs.map { origOutput =>
                  origOutput -> s"%w${ctx.uniqueNumber()}"
                }

                for ((_, rdNew) <- newOutputNames) {
                  // create a new definition
                  core.newDefs += DefReg(
                    ValueVariable(rdNew, -1, WireType),
                    None
                  )
                }

                core.jtbBuilder.addMapping(label, newOutputNames)

                val renamedInst = Rename.asRenamed(newInst) {
                  core.jtbBuilder.getMapping(label)
                }

                caseBlock(pos) = renamedInst
                newOutputNames.map { case (rd, rs) => label -> rs }
              }
            // we create Phis for all the new outputs even though they may
            // no longer be used outside. We have to remove the redundant ones
            // later. If we don't, we screw up lifetime intervals for register
            // allocation (i.e., we wrongfully extends lifetimes by keeping
            // dead Phis and thus introduce artificial register pressure)
            core.jtbBuilder.phis ++= phiOutputs.zip(phiInputs.transpose).map {
              case (rd: Name, rsx: Seq[(Label, Name)]) => Phi(rd, rsx)
            }

          }
          core.currentCycle += 1
          if (pos == maxPos) {
            // we are done with the jump table
            core.state = Processor.MainBlock
            // now is the time for actually making the jump table active. The
            // redundant Phis can be removed after the full schedule is done
            // to ensure lifetime intervals remain accurate

            val newJtb = jtb
              .copy(
                results = jtb.results ++ core.jtbBuilder.phis,
                dslot = core.jtbBuilder.dslot.toSeq,
                blocks = core.jtbBuilder.blocks.map { case (lbl, blk) =>
                  JumpCase(lbl, blk.toSeq)
                }
              )
              .setPos(jtb.pos)
            core.scheduleContext += newJtb

          } else {
            core.state = Processor.CaseBlock(jtb, pos + 1)
          }

      }

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
        ctx.logger.debug(s"finished ${globalCycle}")
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
        processes = scheduledProcesses.map(removeDeadPhis)
      )
      .setPos(program.pos)
  }

  private def removeDeadPhis(
      process: DefProcess
  )(implicit ctx: AssemblyContext) = {

    // in the process of bringing instructions inside case body, we might have
    // created dead phi nodes. Now it is the time to go through all the dead
    // phi nodes and remove them.

    val usedNames = scala.collection.mutable.Set.empty[Name]
    val namesToRemove = scala.collection.mutable.Set.empty[Name]

    val newBody = process.body.reverseIterator
      .map {
        case jtb: JumpTable =>
          val (toKeep, toRemove) = jtb.results.partition { case Phi(rd, _) =>
            usedNames(rd)
          }
          namesToRemove ++= toRemove.map(_.rd)
          jtb.copy(results = toKeep)
        case inst => // nothing to do
          usedNames ++= DependenceAnalysis.regUses(inst)
          inst
      }
      .toSeq
      .reverse

    val usedDefs = process.registers.filter { r =>
      !namesToRemove(r.variable.name)
    }

    process.copy(
      body = newBody,
      registers = usedDefs
    )
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
    val currentSched = core.scheduleContext.getSchedule()
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
        body = finalSchedule,
        registers = core.process.registers ++ core.newDefs
      )
      .setPos(core.process.pos)

  }

  def createDotDependenceGraph(
      graph: Graph[Instruction, GraphEdge.DiEdge]
  )(implicit ctx: AssemblyContext): String = {
    import scalax.collection.io.dot._
    import scalax.collection.io.dot.implicits._
    val dotRoot = DotRootGraph(
      directed = true,
      id = Some("List scheduling dependence graph")
    )
    val nodeIndex = graph.nodes.map(_.toOuter).zipWithIndex.toMap
    def edgeTransform(
        iedge: Graph[Instruction, GraphEdge.DiEdge]#EdgeT
    ): Option[(DotGraph, DotEdgeStmt)] = iedge.edge match {
      case GraphEdge.DiEdge(source, target) =>
        Some(
          (
            dotRoot,
            DotEdgeStmt(
              nodeIndex(source.toOuter).toString,
              nodeIndex(target.toOuter).toString
            )
          )
        )
      case t @ _ =>
        ctx.logger.error(
          s"An edge in the dependence could not be serialized! ${t}"
        )
        None
    }
    def nodeTransformer(
        inode: Graph[Instruction, GraphEdge.DiEdge]#NodeT
    ): Option[(DotGraph, DotNodeStmt)] =
      Some(
        (
          dotRoot,
          DotNodeStmt(
            NodeId(nodeIndex(inode.toOuter)),
            List(DotAttr("label", inode.toOuter.toString.trim.take(64)))
          )
        )
      )

    val dotExport: String = graph.toDot(
      dotRoot = dotRoot,
      edgeTransformer = edgeTransform,
      cNodeTransformer = Some(nodeTransformer), // connected nodes
      iNodeTransformer = Some(nodeTransformer) // isolated nodes
    )
    dotExport
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

    ctx.logger.dumpArtifact(s"scheduler_${process.id}.dot") {
      createDotDependenceGraph(dependenceGraph)
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

  /** Prepare the process for scheduling
    *
    * To schedule a process we remove the state updates and pre schedule the
    * jump tables.
    * @param jtb
    * @param ctx
    * @return
    */
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
              ctx.logger.warn(
                "removing MOV to input, state updates should be handled at register allocation time",
                inst
              )
              false
            } else {
              true
            }
          case Nop =>
            ctx.logger.warn(
              "removing nops, scheduler will decide on the number of nops."
            )
            false
          case _ =>
            if (
              DependenceAnalysis.regDef(inst).exists(statePairs.contains(_))
            ) {
              ctx.logger.error(
                "unexpected write to input register, updates to state should be handled at register allocation time",
                inst
              )
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
  )(implicit ctx: AssemblyContext): Processor.DependenceGraph = {

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

    assert(dependenceGraph.isAcyclic)
    dependenceGraph
  }

  private def estimatePriority(
      dependenceGraph: Processor.DependenceGraph
  )(sendPenalty: ProcessId => Int) = {

    val sources = dependenceGraph.nodes.filter { _.inDegree == 0 }

    val distance =
      scala.collection.mutable.Map.empty[Processor.DependenceGraph#NodeT, Int]

    def compute(node: dependenceGraph.NodeT): Int = {
      val dist = node.toOuter match {
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
      dist
    }
    def traverse(node: dependenceGraph.NodeT): Int = {

      if (!distance.contains(node)) {

        if (node.outDegree == 0) {
          val dist = compute(node)
          distance += (node -> dist)
          dist
        } else {
          // recursively traverse the graph and get the distance to sink nodes
          val succDist = node.diSuccessors.toSeq.map { traverse }.max
          val thisDist = compute(node)
          val dist = succDist + thisDist
          distance += (node -> dist)
          dist
        }
      } else {
        distance(node)
      }

    }

    for (sourceNode <- sources) {
      traverse(sourceNode)
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
            if (dependentNode.inDegree == 1) {
              scheduleContext.readyList += dependentNode
            }
          }
          // then remove the node from the graph
          scheduleContext.graph -= nodeBeingCommitted
        }

        // now try to schedule an instruction
        if (scheduleContext.readyList.isEmpty) {

          // tough luck, there is nothing to do
          scheduleContext += Nop

        } else {

          val toSchedule = scheduleContext.readyList.dequeue()

          ctx.logger.debug(
            s"@$time: Scheduling ${toSchedule.toOuter.serialized}"
          )
          val commitTime =
            time + LatencyAnalysis.latency(toSchedule.toOuter) + 1

          scheduleContext += toSchedule.toOuter
          scheduleContext.activeList += (toSchedule -> commitTime)

        }
        time += 1
      }

      val schedule = scheduleContext.getSchedule()
      val scheduleLength = schedule.length

      if (scheduleLength <= breakDelaySlotSize) {

        JumpCase(
          lbl,
          (BreakCase(-1) +: schedule) ++ Seq.fill(
            breakDelaySlotSize - scheduleLength
          ) { Nop }
        )
      } else {

        val inBreakDelaySlot =
          schedule.takeRight(breakDelaySlotSize)

        val beforeBreakDelaySlot =
          schedule.take(scheduleLength - breakDelaySlotSize)
        // put a break in the middle
        val withBreak = beforeBreakDelaySlot match {
          case before :+ Nop =>
            before :+ BreakCase(-1) // replace a Nop with break
          case _ => beforeBreakDelaySlot :+ BreakCase(-1)
        }

        JumpCase(lbl, withBreak ++ inBreakDelaySlot)
      }
    }

    val caseLength = scheduledBlocks.maxBy(_.block.length).block.length
    val paddedBlocks = scheduledBlocks.map { case JumpCase(lbl, blk) =>
      JumpCase(lbl, Seq.fill(caseLength - blk.length) { Nop } ++ blk)
    }
    jtb
      .copy(
        // dslot = Seq.fill(jumpDelaySlotSize) { Nop },
        dslot = Nil,
        blocks = paddedBlocks
      )
      .setPos(jtb.pos)

  }

}
