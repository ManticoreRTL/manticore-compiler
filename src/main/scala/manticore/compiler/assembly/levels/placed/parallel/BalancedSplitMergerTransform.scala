package manticore.compiler.assembly.levels.placed.parallel

import manticore.compiler.assembly.levels.placed.parallel.util.BasicProcessExtraction
import manticore.compiler.assembly.levels.placed.parallel.util.ProcessorDescriptor
import manticore.compiler.assembly.levels.placed.Helpers.NameDependence
import manticore.compiler.assembly.levels.placed.Helpers.ProgramStatistics
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.placed.PlacedIR
import scalax.collection.mutable.{Graph => MutableGraph}
import scalax.collection.Graph
import scalax.collection.edge.WDiEdge
import scala.collection.BitSet
import scala.annotation.tailrec

object BalancedSplitMergerTransform extends BasicProcessExtraction {

  import PlacedIR._

  private type NodeT = Graph[ProcessorDescriptor, WDiEdge]#NodeT

  def vcycle(node: NodeT) = {
    val numSends = node.outgoing.foldLeft(0.0) { case (acc, e) => acc + e.weight }.toInt
    val numRecvs = node.toOuter.inBound.size
    val numInstr = node.toOuter.body.size
    (numInstr + numSends + numRecvs)
  }
  private def mergeConnectedGraphWithBudget(
      coreBudget: Int,
      graph: MutableGraph[ProcessorDescriptor, WDiEdge],
      parContext: ParallelizationContext
  )(implicit
      ctx: AssemblyContext
  ) = {

    val locked = scala.collection.mutable.Set.empty[NodeT]
    var time_point = System.currentTimeMillis()
    case class MergeChoice(candidate: NodeT, neighbor: NodeT, score: Int)

    def replace(choice: MergeChoice): Unit = {
      val candidate     = choice.candidate
      val neighbor      = choice.neighbor
      val collapsed     = candidate.toOuter merged neighbor.toOuter
      val inherentNodes = Seq(candidate, neighbor)
      val originalPredecessors =
        (neighbor.diPredecessors.toSet ++ candidate.diPredecessors.toSet)
      val originalSuccessors =
        (neighbor.diSuccessors ++ candidate.diSuccessors)
      // create new edges
      def appendPred(pred: NodeT) = {
        if (!inherentNodes.contains(pred)) {
          val sendsFromPred = pred.toOuter.outSet intersect collapsed.inBound
          graph += WDiEdge(pred.toOuter -> collapsed)(sendsFromPred.size)
        }
      }
      neighbor.diPredecessors foreach appendPred
      candidate.diPredecessors foreach appendPred
      def appendSucc(succ: NodeT) = {
        if (!inherentNodes.contains(succ)) {
          val sendsFromCollapsed = collapsed.outSet intersect succ.toOuter.inBound
          graph += WDiEdge(collapsed -> succ.toOuter)(sendsFromCollapsed.size)
        }
      }
      neighbor.diSuccessors foreach appendSucc
      candidate.diSuccessors foreach appendSucc
      graph += collapsed
      graph -= candidate
      graph -= neighbor
    }

    def findShortestAndLongestProcessingTime(): (Option[NodeT], Int, Option[NodeT], Int) = {

      var shortestNode   = Option.empty[NodeT]
      var shortestVcycle = Int.MaxValue
      var longestNode    = Option.empty[NodeT]
      var longestVcycle  = Int.MinValue

      for (node <- graph.nodes) {
        if (!locked(node)) {
          val nodeVcycle = vcycle(node)
          if (nodeVcycle < shortestVcycle) {
            shortestNode = Some(node)
            shortestVcycle = nodeVcycle
          }
          if (nodeVcycle > longestVcycle) {
            longestNode = Some(node)
            longestVcycle = nodeVcycle
          }
        }
      }

      (shortestNode, shortestVcycle, longestNode, longestVcycle)

    }

    def findChoice(mergeCandidate: NodeT, candidateVcycle: Int): Option[MergeChoice] = {

      var choice = Option.empty[MergeChoice]
      for (neighbor <- mergeCandidate.neighbors) {

        val memoryRequirement = (neighbor.toOuter.memory union mergeCandidate.toOuter.memory).toSeq.map {
          parContext.memorySize
        }.sum

        // When we merge mergeCandidate and neighbor, the number of sends from the
        // two merged nodes to their recipients won't change unless they send to
        // each other. Because there is only one state owner. But the number of
        // receive in the merged node may be smaller because of sharing.
        val sendsReduction =
          (mergeCandidate.toOuter.outSet intersect neighbor.toOuter.inBound).size +
            (neighbor.toOuter.outSet intersect mergeCandidate.toOuter.inBound).size
        val recvReduction = (mergeCandidate.toOuter.inBound intersect neighbor.toOuter.inBound).size

        // lower is better
        val score = (candidateVcycle + vcycle(neighbor)) - sendsReduction - recvReduction
        if (memoryRequirement <= (ctx.hw_config.nScratchPad)) {
          if (choice.isEmpty) {
            choice = Some(MergeChoice(mergeCandidate, neighbor, score))
          } else if (choice.get.score > score) {
            choice = Some(MergeChoice(mergeCandidate, neighbor, score))

          }
        }
      }
      choice
    }

    @tailrec
    def tryContraction(processCount: Int): Unit = {
      require(processCount >= 1)
      val now_time = System.currentTimeMillis()
      if ((now_time - time_point) > 1000 * 30 ) { // report at about every 30 seconds
        ctx.logger.info(s"process count ${processCount}")
        time_point = now_time
      }
      // assert(graph.nodes.size == processCount)
      val (shortestNodeOpt, shortestVcycle, longestNodeOpt, longestVcycle) = findShortestAndLongestProcessingTime()

      (shortestNodeOpt, longestNodeOpt) match {
        case (Some(shortestNode), Some(longestNode)) =>
          assert(longestVcycle > 0 && shortestVcycle < Int.MaxValue)
          if (shortestNode != longestNode && processCount > coreBudget) {

            findChoice(shortestNode, shortestVcycle) match {
              case None => // we could not find any feasible choice  (probably due to lack of memory)
                // lock this node so that in the next try we won't make it a candidate to make the algorithm
                // terminate (maybe with a failure)
                locked += shortestNode
                ctx.logger.info("potential high memory pressure in merging")
                tryContraction(processCount) // do not decrease process count because we
              // did not merge any two nodes
              case Some(choice) =>
                // There is a viable choice. Let's see if this choice increase the
                // current estimated vcycle.
                if (choice.score > (longestVcycle / 2)) {
                  ctx.logger.info(
                    s"Merge choice exceeds vcycle threshold ${choice.score} > ${longestVcycle / 2}, will try straggler as merge candidate"
                  )
                  // this choice degrades vcycle, try merging the longest node instead
                  findChoice(longestNode, longestVcycle) match {
                    case Some(otherChoice) if (otherChoice.score < choice.score) =>
                      ctx.logger.info("Merging the current straggler with one of its neighbors")
                      if (otherChoice.score > longestVcycle) {
                        ctx.logger.info(s"increased vcycle from ${longestVcycle} to ${otherChoice.score}")
                      } else {
                        ctx.logger.info(s"decreased vcycle from ${longestVcycle} to ${otherChoice.score}")
                      }
                      replace(otherChoice)
                      tryContraction(processCount - 1)
                    case _ =>
                      replace(choice)
                      ctx.logger.info(s"Have to choose a merge that results in ${choice.score}")
                      tryContraction(processCount - 1)
                  }
                } else {
                  replace(choice)
                  tryContraction(processCount - 1)
                }
            }
          } else if (shortestNode != longestNode && processCount <= coreBudget) {
            // only merge if it leads to an improvement
            findChoice(longestNode, longestVcycle) match {
              case Some(choice) if (choice.score < longestVcycle) =>
                // merging the longest running process with one of its neighbors
                // reduces the virtual cycle length!
                replace(choice)
                tryContraction(processCount - 1)
              case _ =>
                findChoice(shortestNode, shortestVcycle) match {
                  case Some(otherChoice) if (otherChoice.score < longestVcycle) =>
                    // merging the process with largest slack does not violate
                    // virtual cycle but will most likely lead to better network
                    // contention. So we should merge it
                    replace(otherChoice)
                    tryContraction(processCount - 1)
                  case _ =>
                  // Nothing to do. End of the algorithm
                }
            }
          } else if (shortestNode == longestNode && processCount > coreBudget) {
            // this is a strange case, longest and shortest nodes are the same
            // this can only happen if either all nodes have the same vcycle
            // or that there is only a single node. When there is a single
            // node that is not locked and that node is both min and max.
            // This constitutes a failure in merging probably because there is
            // not enough local memory

            findChoice(shortestNode, shortestVcycle) match {
              case None => // failed
                ctx.logger.error(
                  s"Could not complete process merge. There are ${processCount} processes that could not be reduced to ${coreBudget}"
                )
              case Some(choice) => // nodes have the same VCycle
                replace(choice)
                tryContraction(processCount - 1)
            }

          }
        case (None, None) if (processCount <= coreBudget) =>
        // success
        case _ => ctx.logger.error(s"Failed merging. Could not merge ${processCount} processes into ${coreBudget}")

      }

    }

    // val numProcesses = graph.nodes.size
    tryContraction(graph.nodes.size)
    graph
  }
  override protected def doSplit(program: DefProgram)(implicit ctx: AssemblyContext): DefProgram = {

    assert(program.processes.length == 1)
    val parContext = createParallelizationContext(program.processes.head)
    val processes  = extractIndependentInstructionSequences(program.processes.head, parContext).toIndexedSeq
    val stateUsers = computeStateUsers(parContext, processes)

    val dimY         = ctx.hw_config.dimY
    val dimX         = ctx.hw_config.dimX
    val numCores     = dimX * dimY      // N
    val numProcesses = processes.length // M

    // we want to contract a graph with M maximally independent processes to
    // a graph of at most N parallel processes where N = dimX * dimY.
    val graph = MutableGraph.empty[ProcessorDescriptor, WDiEdge]
    // create a directed graph with vertices representing ProcessorDescriptors
    // and edges the messages that flow between them (weighted by number of messages)
    // Graph construction is almost O(M^2) in complexity because in the worst case
    // the graph could be complete with O(M^2) edges
    for (txProcess <- processes) {
      graph += txProcess

      val rxProcesses = txProcess.outSet.flatMap(stateUsers)
      // establish the "send" edges between txProcess and all rxProcesses

      for (rxIndex <- rxProcesses) {
        val rxProcess      = processes(rxIndex)
        val sendsInBetween = txProcess.outSet intersect rxProcess.inBound
        graph += WDiEdge(txProcess -> rxProcess)(sendsInBetween.size)

      }

    }

    val clusters = graph.componentTraverser().map { _.to(MutableGraph) }

    assert(clusters.size == 1, "can not handle disconnected graphs yet") // handle core budget allocation later

    ctx.logger.info(
      s"max split gives a vcycle estimate of ${vcycle(clusters.head.nodes.maxBy(vcycle))} in ${clusters.head.nodes.size} processes"
    )
    // mutates clusters.head
    ctx.stats.recordRunTime("merging") {
      mergeConnectedGraphWithBudget(numCores, clusters.head, parContext)
    }

    val cores          = clusters.head.nodes.toSeq.map { x => x.toOuter }
    val finalProcesses = createProcesses(program.processes.head, cores, parContext)
    val result = program.copy(
      processes = finalProcesses
    )
    ctx.stats.record(ProgramStatistics.mkProgramStats(result))
    result

  }

  def createProcesses(originalProcess: DefProcess, cores: Seq[ProcessorDescriptor], parContext: ParallelizationContext)(
      implicit ctx: AssemblyContext
  ) = {

    val stateUsers            = computeStateUsers(parContext, cores)
    def processId(index: Int) = ProcessIdImpl(s"p${index}", -1, -1)
    var coreIndex             = 0
    val processes = cores.map { core =>
      val body = core.body.toSeq.map { parContext.getInstruction }
      val sends = core.outSet.toSeq.flatMap { nextStateIdx =>
        stateUsers(nextStateIdx).toSeq.collect {
          case recipient if recipient != coreIndex =>
            val currentStateInDest = parContext.getInput(nextStateIdx)
            val nextStateHere      = parContext.getOutput(nextStateIdx)
            Send(currentStateInDest, nextStateHere, processId(recipient))
        }
      }
      val bodyWithSends = body ++ sends
      val referenced    = NameDependence.referencedNames(bodyWithSends)
      val usedRegs = originalProcess.registers.filter { r =>
        referenced(r.variable.name)
      }
      val usedLabelGrps = originalProcess.labels.filter(lblgrp => referenced(lblgrp.memory))
      val usedGlobalMemories =
        if (bodyWithSends.exists { i => i.isInstanceOf[GlobalLoad] || i.isInstanceOf[GlobalStore] }) {
          originalProcess.globalMemories
        } else {
          Nil
        }
      assert(originalProcess.functions == Nil, "can not handle custom functions yet")

      val p = DefProcess(
        id = processId(coreIndex),
        registers = usedRegs,
        functions = Nil,
        labels = usedLabelGrps,
        body = bodyWithSends,
        globalMemories = usedGlobalMemories
      )
      coreIndex += 1
      p
    }

    processes
  }

  private def computeStateUsers(parContext: ParallelizationContext, processes: Iterable[ProcessorDescriptor]) = {

    val stateUsers = Array.fill(parContext.numStateRegs()) { BitSet.empty }

    var processIndex = 0
    for (p <- processes) {
      for (inpIdx <- p.inBound) {
        stateUsers(inpIdx) = stateUsers(inpIdx) union BitSet(processIndex)
      }
      processIndex += 1
    }

    { stateIndex: Int => stateUsers(stateIndex) }
  }

}
