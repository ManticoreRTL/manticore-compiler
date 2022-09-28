package manticore.compiler.assembly.levels.placed.lowering

import manticore.compiler.assembly.levels.placed.Helpers.NameDependence
import manticore.compiler.assembly.levels.placed.Helpers.GraphBuilder
import manticore.compiler.assembly.levels.placed.Helpers.InputOutputPairs
import manticore.compiler.assembly.levels.placed.Helpers.ProgramStatistics
import manticore.compiler.assembly.levels.placed.PlacedIRRenamer.Rename
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.assembly.levels.placed.PlacedIRTransformer
import manticore.compiler.AssemblyContext
import scalax.collection.mutable.{Graph => MutableGraph}
import scalax.collection.Graph
import scalax.collection.GraphEdge
import manticore.compiler.assembly.annotations.Memblock
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.assembly.levels.placed.TaggedInstruction
import scala.annotation.tailrec
import manticore.compiler.assembly.levels.WireType
import manticore.compiler.assembly.levels.placed.lowering.util.Processor
import manticore.compiler.assembly.levels.placed.lowering.util.ScheduleContext
import manticore.compiler.assembly.levels.placed.lowering.util.NetworkOnChip
import manticore.compiler.assembly.levels.placed.lowering.util.RecvEvent
import manticore.compiler.assembly.CanBuildDependenceGraph
import manticore.compiler.assembly.levels.OutputType
import java.io.PrintWriter

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
private[lowering] object ProgramSchedulingTransform extends PlacedIRTransformer {
  import PlacedIR._

  private val jumpDelaySlotSize  = 2
  private val breakDelaySlotSize = 2
  def transform(
      program: DefProgram
  )(implicit context: AssemblyContext): DefProgram = {

    val prepared = context.stats.recordRunTime("preparing processes") {
      program.copy(
        processes = program.processes.map(prepareProcessForScheduling(_)(context))
      )
    }

    val results = createProgramSchedule(prepared)(context)

    context.stats.record(ProgramStatistics.mkProgramStats(results))

    results

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

    val network = NetworkOnChip(ctx.hw_config)

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
          val matched    = matcher(toSchedule.toOuter)
          if (matched.nonEmpty) {
            core.scheduleContext.readyList ++= notMatched
            matched
          } else {
            findCompatible(matcher, notMatched :+ toSchedule)
          }
        } else {
          core.scheduleContext.readyList ++= notMatched
          None
        }
      }

      sealed trait MatchResult {
        val inst: Instruction
      }
      case class SendMatch(inst: Send, path: network.Path) extends MatchResult
      case class AnyMatch(inst: Instruction)               extends MatchResult

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

      def matchAnyButSendAndStore(inst: Instruction): Option[MatchResult] =
        inst match {
          case _ @(_: LocalStore | _: GlobalStore | _: Send) => None
          case inst                                          => Some(AnyMatch(inst))
        }

      def tryCommitting(): Unit = {
        val nodesToCommit = core.scheduleContext.activeList.collect {
          case (n, t) if t <= core.currentCycle => n
        }
        // and remove them from the activeList
        assert(nodesToCommit.forall(core.scheduleContext.activeList.contains))
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
          ctx.hw_config.latency(
            inst
          ) + core.currentCycle + 1

        core.scheduleContext.activeList += (core.scheduleContext.graph
          .get(inst) -> commitCycle)

        ctx.logger.debug(
          s"@${core.process.id}:${core.currentCycle}: ${inst}"
        )
        ctx.logger.debug(s"commits @ ${commitCycle}")
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
              ctx.logger.error(s"JumpTables are a thing of the past!")
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
          val ready = findCompatible(matchAnyButSendAndStore)
          activateReady(ready)
          ready match {
            case Some(readyToActivate) =>
              val newInst = readyToActivate.inst
              ctx.logger.debug((s"Delay slot: ${newInst}"))
              // tell the context we scheduling a proxy for the original
              // instruction, needed for correct "finish" condition
              core.scheduleContext +?= newInst
              core.jtbBuilder.dslot += newInst

            case None => // nothing to do
              core.jtbBuilder.dslot += Nop
          }

          core.currentCycle += 1

          if (pos == jumpDelaySlotSize - 1) {
            core.state = Processor.CaseBlock(jtb, 0)
          } else {
            core.state = Processor.DelaySlot(jtb, pos + 1)
          }
        case Processor.CaseBlock(jtb, pos) =>
          val maxPos = core.jtbBuilder.blocks.head._2.length - 1

          // we can only bring new instruction from outside if all blocks
          // doing a Nop right now
          val canScheduleInstFromOutside = core.jtbBuilder.blocks.forall { case (lbl, blk) =>
            blk(pos) == Nop
          }

          if (canScheduleInstFromOutside) {
            val ready = findCompatible(matchAnyButSendAndStore)
            activateReady(ready)
            ready match {
              case Some(newActive) =>
                val newInst = newActive.inst
                ctx.logger.debug(s"Bringing in instruction to JumpTable ${jtb.target}", newInst)
                assert(newInst != Nop)
                // notify the context that this newInst is being scheduled
                // in an opaque way
                core.scheduleContext +?= newInst
                // need to rename the output of newInst to a fresh name in every
                // case block to keep the SSA-ness. This means we need to create
                // new Phi outputs for each output of the instruction we are bringing
                // in
                val phiOutputs = NameDependence.regDef(newInst)

                val phiInputs =
                  for ((label, caseBlock) <- core.jtbBuilder.blocks) yield {

                    val newOutputNames = phiOutputs.map { origOutput =>
                      origOutput -> s"%w${ctx.uniqueNumber()}"
                    }

                    for ((origOutput, rdNew) <- newOutputNames) {
                      val tpe = WireType

                      // create a new definition
                      core.newDefs += DefReg(
                        ValueVariable(rdNew, -1, tpe),
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

                val newPhis = phiOutputs.zip(phiInputs.transpose).map { case (rd: Name, rsx: Seq[(Label, Name)]) =>
                  Phi(rd, rsx)
                }

                if (newInst != Nop && newPhis.isEmpty) {
                  ctx.logger.debug("something is up", newInst)
                }
                ctx.logger.debug(s"new phis: \n${newPhis.mkString("\n")}")
                val x = core.jtbBuilder.phis.length
                core.jtbBuilder.phis ++= newPhis
                if (core.jtbBuilder.phis.length != x + newPhis.length) {
                  ctx.logger.error("something is up", newInst)
                }

              case None =>
              // there is nothing we can do, no instruction can be brought in
              // from the outside

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
                results = jtb.results ++ core.jtbBuilder.phis.toSeq,
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
        // ctx.logger.debug(s"finished ${globalCycle}")
        globalCycle += 1
      }
    }

    ctx.logger.info(s"NoC:\n ${network.draw()}")

    ctx.logger.dumpArtifact(s"paths.json") { NetworkOnChip.jsonDump(network) }

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

    val usedNames =
      scala.collection.mutable.Set.empty[Name] ++ process.registers.collect {
        case r: DefReg if r.variable.varType == OutputType => r.variable.name
      }
    val namesToRemove = scala.collection.mutable.Set.empty[Name]

    val newBody = process.body.reverseIterator
      .map {
        case jtb: JumpTable =>
          val (toKeep, toRemove) = jtb.results.partition { case Phi(rd, _) =>
            usedNames(rd)
          }
          if (toRemove.nonEmpty)
            ctx.logger.debug(s"removing dead phis:\n${toRemove.mkString("\n")}")
          namesToRemove ++= toRemove.map(_.rd)
          usedNames ++= NameDependence.regUses(jtb)
          jtb.copy(results = toKeep)
        case inst => // nothing to do
          usedNames ++= NameDependence.regUses(inst)
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
    val sortedRecvs  = core.getReceivesSorted()
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

    if (finalCycle + 1 + ctx.hw_config.maxLatency >= ctx.hw_config.nInstructions) {
      ctx.logger.error(
        s"Could not schedule process ${core.process.id} that requires ${finalCycle + 1} instruction."
      )
    }

    core.process
      .copy(
        body = finalSchedule,
        registers = core.process.registers ++ core.newDefs
      )
      .setPos(core.process.pos)

  }

  private def createProcessor(
      process: DefProcess
  )(implicit ctx: AssemblyContext): Processor = {
    val definingInstruction =
      NameDependence.definingInstructionMap(process)
    val dependenceGraph =
      ctx.stats.recordRunTime(
        s"building process ${process.id} dependence graph"
      ) {
        GraphBuilder.defaultGraph(process.body)
      }

    val priorities = ctx.stats.recordRunTime("estimating priorities") {
      val dist = estimatePriority(dependenceGraph) { targetId =>
        // penalty of Send is a SetValue instruction and the number of hops
        // in the NoC
        ctx.hw_config.manhattan(
          process.id,
          targetId
        ) + ctx.hw_config.latency(SetValue("", UInt16(0)))
      }

      ctx.logger.dumpArtifact(s"${process.id}_priorities.txt") {
        val builder = new StringBuilder
        val sortedBody = process.body.sorted {
          Ordering.by { i: Instruction => dist(dependenceGraph.get(i)) }.reverse
        }
        for (inst <- sortedBody) {
          builder ++= s"${inst.toString()} -> ${dist(dependenceGraph.get(inst))}\n"
        }
        builder.toString()
      }
      Ordering.by { dist }
    }
    val processor =
      new Processor(process, new ScheduleContext(dependenceGraph, priorities))
    ctx.logger.dumpArtifact(s"scheduler_${process.id}.dot") {
      GraphBuilder.toDotGraph(processor.scheduleContext.graph)
    }
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
            if (NameDependence.regDef(inst).exists(statePairs.contains(_))) {
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

  private def estimatePriority(
      dependenceGraph: Processor.DependenceGraph
  )(sendPenalty: ProcessId => Int)(implicit ctx: AssemblyContext) = {

    val sources = dependenceGraph.nodes.filter { _.inDegree == 0 }

    val distance =
      scala.collection.mutable.Map.empty[Processor.DependenceGraph#NodeT, Int]

    def compute(node: dependenceGraph.NodeT): Int = {
      val dist = node.toOuter match {
        case send: Send =>
          // we penalize the send latency by a user defined function,
          // this penalty will most likely be the manhattan distance plus
          // some added latency for the SET instruction in the destination
          ctx.hw_config.latency(send) + sendPenalty(send.dest_id)

        case inst => ctx.hw_config.latency(inst)

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
          val dist     = succDist + thisDist
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

    distance
    // Ordering.by { distance }
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
        GraphBuilder.defaultGraph(block)
        // val definingInstruction = NameDependence.definingInstruction(block)
        // createDependenceGraph(block, definingInstruction)
      }
      val priorities = ctx.stats.recordRunTime(s"$lbl estimating priorities") {
        val dist = estimatePriority(dependenceGraph) { _ =>
          ctx.logger.fail("did not expect Send inside jump table!")
        }

        Ordering.by { dist }
      }

      val scheduleContext = new ScheduleContext(dependenceGraph, priorities)
      var time: Int       = 0
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
            time + ctx.hw_config.latency(toSchedule.toOuter) + 1

          scheduleContext += toSchedule.toOuter
          scheduleContext.activeList += (toSchedule -> commitTime)

        }
        time += 1
      }

      val schedule       = scheduleContext.getSchedule()
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
