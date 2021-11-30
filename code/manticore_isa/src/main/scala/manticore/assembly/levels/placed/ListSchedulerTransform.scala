package manticore.assembly.levels.placed

import manticore.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import manticore.assembly.BinaryOperator
import scala.annotation.tailrec
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import manticore.assembly.CompilationFailureException
import manticore.assembly.DependenceGraphBuilder
import scalax.collection.edge.LDiEdge

object ListSchedulerTransform extends AssemblyTransformer(PlacedIR, PlacedIR) {

  import PlacedIR._
  import scalax.collection.Graph
  import scalax.collection.mutable.{Graph => MutableGraph}
  import scalax.collection.edge.WDiEdge

  def instructionLatency(instruction: Instruction): Int = 3

  case class Label(v: Int)
  def labelingFunc(pred: Instruction, succ: Instruction): Label = Label(3)


  case class PartiallyScheduledProcess(
      proc: DefProcess,
      partial_schedule: List[Instruction],
      earliest: Map[Send, Int]
  )

  private def partiallySchedule(
      proc: DefProcess,
      ctx: AssemblyContext
  ): PartiallyScheduledProcess = {

    import manticore.assembly.DependenceGraphBuilder
    object DependenceAnalysis extends DependenceGraphBuilder(PlacedIR)

    val dependence_graph = DependenceAnalysis.build[Label](proc, labelingFunc)(ctx)
    // dump the graph
    logger.dumpArtifact(s"dependence_graph_${getName}_${proc.id.id}.dot") {

      import scalax.collection.io.dot._
      import scalax.collection.io.dot.implicits._

      val dot_root = DotRootGraph(
        directed = true,
        id = Some("List scheduling dependence graph")
      )
      def edgeTransform(
          iedge: Graph[Instruction, LDiEdge]#EdgeT
      ): Option[(DotGraph, DotEdgeStmt)] = iedge.edge match {
        case LDiEdge(source, target, l) =>
          Some(
            (
              dot_root,
              DotEdgeStmt(
                source.toOuter.hashCode().toString,
                target.toOuter.hashCode().toString,
                List(DotAttr("label", l.asInstanceOf[Label].v.toString))
              )
            )
          )
        case t @ _ =>
          logger.error(
            s"An edge in the dependence could not be serialized! ${t}"
          )
          None
      }
      def nodeTransformer(
          inode: Graph[Instruction, LDiEdge]#NodeT
      ): Option[(DotGraph, DotNodeStmt)] =
        Some(
          (
            dot_root,
            DotNodeStmt(
              NodeId(inode.toOuter.hashCode().toString()),
              List(DotAttr("label", inode.toOuter.serialized.trim))
            )
          )
        )

      val dot_export: String = dependence_graph.toDot(
        dotRoot = dot_root,
        edgeTransformer = edgeTransform,
        cNodeTransformer = Some(nodeTransformer)
      )
      dot_export
    }(ctx)


    type Node = dependence_graph.NodeT
    type Edge = dependence_graph.EdgeT
    val distance_to_sink = scala.collection.mutable.Map[Node, Int]()

    require(
      dependence_graph.edges.forall(_.isOut),
      "dependence graph is mal-formed!"
    )

    require(dependence_graph.isAcyclic, "Dependence graph is cyclic!")
    require(dependence_graph.nodes.size == proc.body.size)

    def traverseGraphNodesAndRecordDistanceToSink(node: Node): Int = {
      node.outerEdgeTraverser

      if (node.edges.filter(_.from == node).isEmpty) {
        distance_to_sink(node) = 0
        0
      } else if (distance_to_sink.contains(node)) {
        // answer already known
        distance_to_sink(node)
      } else {
        node.edges.map { edge =>
          if (edge.from == node) {
            val dist: Int =
              traverseGraphNodesAndRecordDistanceToSink(edge.to) + edge.label.asInstanceOf[Label].v
            distance_to_sink(node) = dist
            dist
          } else { 0 }

        }.max
      }
    }

    // find the distance of each node to sinks
    dependence_graph.nodes.foreach { x => traverseGraphNodesAndRecordDistanceToSink(x) }

    val send_instructions = proc.body.collect { case i @ Send(_, _, _, _) => i }

    val unsched_list = scala.collection.mutable.ListBuffer[Instruction]()
    unsched_list ++= proc.body.filter(_.isInstanceOf[Send] == false)

    val schedule = scala.collection.mutable.ListBuffer[Instruction]()

    case class ReadyNode(n: Node) extends Ordered[ReadyNode] {
      def compare(that: ReadyNode) =
        Ordering[Double].reverse
          .compare(distance_to_sink(this.n), distance_to_sink(that.n))

    }

    val ready_list = scala.collection.mutable.PriorityQueue[ReadyNode]()
    // initialize the ready list with instruction that have no predecessor

    logger.debug(
      dependence_graph.nodes
        .map { n =>
          s"${n.toOuter.serialized} \tindegree ${n.inDegree}\toutdegree ${n.outDegree}"
        }
        .mkString("\n")
    )(ctx)
    ready_list ++= dependence_graph.nodes
      .filter { n => n.inDegree == 0 && n.toOuter.isInstanceOf[Send] == false }
      .map { ReadyNode(_) }

    logger.debug(
      s"Initial ready list:\n ${ready_list.map(_.n.toOuter.serialized).mkString("\n")}"
    )(ctx)

    // create a mutable set showing the satisfied dependencies
    val satisfied_dependence = scala.collection.mutable.Set.empty[Edge]

    // dependence_graph.OuterEdgeTraverser

    var active_list = scala.collection.mutable.ListBuffer[(Node, Double)]()

    // LIST scheduling simulation loop
    var cycle = 0
    while (unsched_list.nonEmpty && cycle < 4096) {

      val to_retire = active_list.filter(_._2 == 0)
      val finished_list =
        active_list.filter { _._2 == 0.0 } map { finished =>
          logger.debug(
            s"${cycle}: Committing ${finished._1.toOuter.serialized} "
          )(ctx)

          val node = finished._1

          node.edges
            .filter { e => e.from == node }
            .foreach { outbound_edge =>
              // mark any dependency from this node as satisfied
              satisfied_dependence.add(outbound_edge)
              // and check if any instruction has become ready
              val successor = outbound_edge.to
              if (
                successor.edges.filter { _.to == successor }.forall {
                  satisfied_dependence.contains
                }
              ) {
                logger.debug(
                  s"${cycle}: Readying ${successor.toOuter.serialized} "
                )(ctx)
                ready_list += ReadyNode(successor)
              }
            }
          finished
        }
      active_list --= finished_list
      val new_active_list = active_list.map { case (n, d) => (n, d - 1.0) }
      active_list = new_active_list

      if (ready_list.isEmpty) {
        schedule.append(Nop)
      } else {
        val head = ready_list.head
        logger.debug(s"${cycle}: Scheduling ${head.n.toOuter.serialized}")(ctx)
        active_list.append((head.n, instructionLatency(head.n.toOuter)))
        schedule.append(head.n.toOuter)
        unsched_list -= head.n.toOuter
        ready_list.dequeue()
      }
      cycle += 1
    }

    if (cycle >= 4096) {
      logger.error(
        "Failed to schedule processes, ran out of instruction memory",
        proc
      )
    }

    val register_to_send = send_instructions.map { case i @ Send(_, rs, _, _) =>
      rs -> i
    }.toMap
    val earliest_send_cycles =
      scala.collection.mutable.Map[Send, Option[Int]](
        send_instructions.map { x => (x, None) }: _*
      )

    // find the earliest time each send instruction can be scheduled, i.e.,
    // latency + cycle where cycle is the time the producer instruction is
    // scheduled.
    schedule.zipWithIndex.foreach { case (inst, cycle) =>
      DependenceAnalysis.regDef(inst) match {
        case Some(rd) =>
          register_to_send.get(rd) match {
            case Some(send: Send) =>
              earliest_send_cycles.update(
                send,
                Some(cycle + instructionLatency(inst))
              )
            case None =>
            // nothing
          }
        case None =>
        // nothing
      }
    }

    // a map from Send instructions to the earliest time they can be scheduled
    val send_to_earliest = earliest_send_cycles.map { case (inst, c) =>
      (
        inst,
        c match {
          case Some(n) => n
          case None =>
            logger.warn(s"Process ${proc.id.id} sends a constant value", inst)
            0
        }
      )

    }.toMap
    // a partial schedule that does not contain the Send instructions
    val partial_schedule = schedule.toList

    // return both to be used for a global schedule
    PartiallyScheduledProcess(
      proc = proc,
      partial_schedule = partial_schedule,
      earliest = send_to_earliest
    )

  }

  override def transform(
      source: DefProgram,
      context: AssemblyContext
  ): DefProgram = {

    type Node = Graph[Instruction, WDiEdge]#NodeT

    // first find partial schedules that do not have the send instructions (in
    // parallel)
    val partial_schedules = source.processes.par.map { p =>
      partiallySchedule(p, context)
    }.seq

    def getDim(dim: String): Int =
      source.findAnnotationValue("LAYOUT", dim) match {
        case Some(manticore.assembly.annotations.IntValue(v)) => v
        case _ =>
          logger.fail("Scheduling requires a valid @LAYOUT annotation")
          0
      }

    val dimx = getDim("x")
    val dimy = getDim("y")

    case class SendWrapper(
        inst: Send,
        source: ProcessId,
        target: ProcessId,
        earliest: Int
    ) extends Ordered[SendWrapper] {
      val manhattan = {
        val x_dist =
          if (source.x > target.x) dimx - source.x + target.x
          else target.x - source.x
        val y_dist =
          if (source.y > target.y) dimy - source.y + target.y
          else target.y - source.y
        x_dist + y_dist
      }
      val priority = manhattan + earliest
      def compare(that: SendWrapper): Int =
        Ordering[Int].reverse.compare(this.priority, that.priority)
    }
    case class ProcessWrapper(
        proc: DefProcess,
        scheduled: List[Instruction],
        unscheduled: List[SendWrapper]
    ) extends Ordered[ProcessWrapper] {

      def compare(that: ProcessWrapper): Int =
        (this.unscheduled, that.unscheduled) match {
          case (Nil, Nil) => 0
          case (x :: _, y :: _) =>
            Ordering[Int].reverse.compare(x.priority, y.priority)
          case (Nil, y :: _) => -1
          case (x :: _, Nil) => 1
        }
    }
    // now patch the local schedule by globally scheduling the send instructions
    import scala.collection.mutable.PriorityQueue

    // keep a sorted queue of Processes, sorting is done based on the
    // priority of each processes' send instruction. I.e., the process with
    // the most critical send (largest manhattan distance and latest possible schedule time)
    // should be considered first when trying to schedule sends in a cycle
    val to_schedule = PriorityQueue.empty[ProcessWrapper] ++
      partial_schedules.map { psched =>
        val sends = psched.earliest
          .map { case (send, early) =>
            SendWrapper(
              inst = send,
              source = psched.proc.id,
              target = send.dest_id,
              earliest = early
            )
          }
          .toList
          .sorted
        ProcessWrapper(psched.proc, psched.partial_schedule, sends)
      }.toSeq

    type LinkOccupancy = scala.collection.mutable.Set[Int]
    val linkX = Array.ofDim[LinkOccupancy](dimx, dimy)
    val linkY = Array.ofDim[LinkOccupancy](dimx, dimy)

    var cycle = 0
    // while(to_schedule.nonEmpty && cycle < 4096) {

    // }

    source.copy(
      processes = partial_schedules.map { p =>
        p.proc.copy(body = p.partial_schedule ++ p.earliest.keySet.toSeq)
      }.seq
    )
  }

}
