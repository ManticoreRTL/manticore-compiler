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
import scala.collection.parallel.CollectionConverters._
/**
  * List scheduler transformation
  *
  * @author Mahyar emami <mahyar.emami@epfl.ch>
  *
  */
object ListSchedulerTransform extends AssemblyTransformer(PlacedIR, PlacedIR) {

  import PlacedIR._
  import scalax.collection.Graph
  import scalax.collection.mutable.{Graph => MutableGraph}
  import scalax.collection.edge.WDiEdge

  private case class Label(v: Int)

  private def instructionLatency(inst: Instruction): Int = inst match {
    case Predicate(_, _) => 0
    case _               => 3
  }
  private def labelingFunc(pred: Instruction, succ: Instruction): Label =
    (pred, succ) match {
      // store instructions take a cycle longer, because there is going to be a
      // predicate instruction just before them
      case (p, q: GlobalStore) => Label(instructionLatency(q) + 1)
      case (p, q: LocalStore)  => Label(instructionLatency(q) + 1)
      case (p, q)              => Label(instructionLatency(p))
    }

  private case class PartiallyScheduledProcess(
      proc: DefProcess,
      partial_schedule: List[Instruction],
      earliest: Map[Send, Int]
  )

  private def createLocalSchedule(
      proc: DefProcess,
      ctx: AssemblyContext
  ): PartiallyScheduledProcess = {

    import manticore.assembly.DependenceGraphBuilder
    import manticore.assembly.levels.placed.DependenceAnalysis

    val dependence_graph =
      DependenceAnalysis.build[Label](proc, labelingFunc)(ctx)
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
    // require(dependence_graph.nodes.size == proc.body.size)

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
              traverseGraphNodesAndRecordDistanceToSink(edge.to) + edge.label
                .asInstanceOf[Label]
                .v
            distance_to_sink(node) = dist
            dist
          } else { 0 }

        }.max
      }
    }

    // find the distance of each node to sinks
    dependence_graph.nodes.foreach { x =>
      traverseGraphNodesAndRecordDistanceToSink(x)
    }

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
    val scheduled_preds = scala.collection.mutable.Set.empty[Node]
    // LIST scheduling simulation loop
    var cycle = 0
    while (unsched_list.nonEmpty && cycle < 4096) {

      val to_retire = active_list.filter(_._2 == 0)
      val finished_list =
        active_list.filter { _._2 == 0 } map { finished =>
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
      val new_active_list = active_list.map { case (n, d) => (n, d - 1) }
      active_list = new_active_list

      if (ready_list.isEmpty) {
        schedule.append(Nop)
      } else {
        val head = ready_list.head
        // check if a predicate instruction is needed before scheduling the head
        val predicate: Option[Predicate] =
          if (scheduled_preds.contains(head.n)) {
            None // predicate of the store is already scheduled
          } else {
            head.n.toOuter match {
              case GlobalStore(_, (b0, b1, b2), p, _) =>
                p.map(Predicate(_))
              case LocalStore(_, b, _, p, _) =>
                p.map(Predicate(_))
              case _ => None
            }

          }
        predicate match {
          case Some(x: Predicate) =>
            logger.debug(s"${cycle}: Scheduling ${x}")(ctx)
            schedule.append(x)
            scheduled_preds += head.n
          case None =>
            logger
              .debug(s"${cycle}: Scheduling ${head.n.toOuter.serialized}")(ctx)
            active_list.append((head.n, instructionLatency(head.n.toOuter)))
            schedule.append(head.n.toOuter)
            unsched_list -= head.n.toOuter
            ready_list.dequeue()
        }
      }
      cycle += 1
    }

    if (cycle >= 4096) {
      logger.error(
        "Failed to schedule processes, ran out of instruction memory",
        proc
      )
    }

    /** All of the Non-send instructions are now scheduled. We need to "patch"
      * the schedule with [[Predicate]] instructions so that [[LocalStore]] and
      * [[GlobalStore]] would no longer need to have explicit predicates
      */

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

    // now go through all instruction, see if their produced value is sent
    // and then compute the earliest time a Send can be scheduled
    schedule.zipWithIndex.foreach { case (inst, cycle) =>
      DependenceAnalysis.regDef(inst) match {
        case Some(rd) =>
          register_to_send.get(rd) match {
            case Some(send: Send) =>
              earliest_send_cycles +=
                send ->
                  Some(
                    cycle + 1 + instructionLatency(inst)
                  )

            case None =>
            // nothing
          }
        case None =>
        // nothing
      }
    }

    val earliest_send_sorted = earliest_send_cycles
      .map { case (inst, c) =>
        inst -> (c match {
          case Some(n) => n
          case None    => // in case the Send has no producer, warn the user and
            // set the earliest time to 0
            logger.warn(s"Process ${proc.id.id} sends a constant value", inst)
            0
        })
      }
      .toSeq
      .sortBy(_._2)

    // a map from Send instructions to the earliest time they can be scheduled
    val send_to_earliest = earliest_send_sorted.zipWithIndex.map {
      case ((inst, c), offset) =>
        /**
          * We prioritize order the Sends within each process by their earliest
          * time, notice that this means we have to add a cycle offset to
          * every send corresponding to the number of Sends that are ordered
          * before them. This ensures that the ordering of Sends is taken
          * into account later when we perform a global scheduling of Sends
          */
        inst -> (c + offset)
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

  private def createGlobalSchedule(
      source: DefProgram,
      context: AssemblyContext
  ): DefProgram = {

    type Node = Graph[Instruction, WDiEdge]#NodeT

    // first find partial schedules that do not have the send instructions (in
    // parallel)
    val partial_schedules = source.processes.par.map { p =>
      // remove the Nops, the local scheduler will put them back in
      val pruned_process = p.copy(
        body = p.body.filter( _!= Nop )
      )
      createLocalSchedule(pruned_process, context)
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

    // a wrapper class for Send instruction
    case class SendWrapper(
        inst: Send,
        source: ProcessId,
        target: ProcessId,
        earliest: Int
    ) extends Ordered[SendWrapper] {
      val x_dist =
        if (source.x > target.x) dimx - source.x + target.x
        else target.x - source.x
      val y_dist =
        if (source.y > target.y) dimy - source.y + target.y
        else target.y - source.y
      val manhattan =
        x_dist + y_dist

      val path_x: Seq[(Int, Int)] = // a tuple of x location and occupancy time
        Seq.tabulate(x_dist) { i =>
          val x_v = (source.x + i) % dimx
          x_v -> (earliest + instructionLatency(inst) + i + 1)
        }
      val path_y: Seq[(Int, Int)] = // a tuple of y location and occupancy time
        Seq.tabulate(y_dist) { i =>
          val y_v = (source.y + i) % dimy
          y_v -> (path_x.last._2 + i + 1)
        }

      // A send instruction that becomes available earlier, has higher priority
      // locally, this is somehow enforced, i.e., the List scheduler assumes
      // that Sends are scheduled in the order in which their data is produced.
      // Without this assumption it is not possible to compute a valid early time
      // since scheduling one Send will change the earliest time unscheduled ones
      def compare(that: SendWrapper): Int =
        Ordering[Int]
          .compare(this.earliest, that.earliest)
    }

    import scala.collection.mutable.{Queue => MutableQueue}

    // a wrapper class for processes that contains a list of scheduled
    // non-Send instruction and non-schedule Send instructions.
    case class ProcessWrapper(
        proc: DefProcess,
        scheduled: MutableQueue[(Send, Int)],
        unscheduled: MutableQueue[SendWrapper]
    ) extends Ordered[ProcessWrapper] {

      // Among processes, always prioritize Sends that have a longer
      // manhattan distance. This is much like the LIST scheduling algorithm
      // where the distance to sink is the priority.
      def compare(that: ProcessWrapper): Int =
        (this.unscheduled, that.unscheduled) match {
          case (MutableQueue(), MutableQueue()) => 0
          case (x +: _, y +: _) =>
            Ordering[Int].reverse.compare(x.manhattan, y.manhattan)
          case (MutableQueue(), y +: _) => -1
          case (x +: _, MutableQueue()) => 1
        }
    }
    // now patch the local schedule by globally scheduling the send instructions
    import scala.collection.mutable.PriorityQueue

    // keep a sorted queue of Processes, sorting is done based on the priority
    // of each processes' send instruction. I.e., the process with the most
    // critical send (largest manhattan distance and earliest possible schedule
    // time) should be considered first when trying to schedule sends in a cycle
    val to_schedule = PriorityQueue.empty[ProcessWrapper] ++
      partial_schedules
        .filter {
          _.earliest.nonEmpty
        }
        .map { psched =>
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
          ProcessWrapper(
            psched.proc,
            MutableQueue(),
            MutableQueue() ++ sends
          )
        }
        .toSeq

    def createLinks = {
      type LinkOccupancy = scala.collection.mutable.Set[Int]
      val link = Array.ofDim[LinkOccupancy](dimx, dimy)
      for (x <- 0 until dimx; y <- 0 until dimy) {
        link(x)(y) = scala.collection.mutable.Set.empty[Int]
      }
      link
    }
    val linkX = createLinks
    val linkY = createLinks

    var cycle = 0
    var schedule_unfinished = true
    while (cycle < 4096 && to_schedule.exists(_.unscheduled.nonEmpty)) {

      // go through all processes and try to schedule the highest priority
      // send instruction in each
      val checked = to_schedule.dequeueAll.map { h: ProcessWrapper =>
        if (h.unscheduled.nonEmpty) {
          val inst_wrapper = h.unscheduled.head
          if (inst_wrapper.earliest <= cycle) {
            val can_route_horiz = inst_wrapper.path_x.forall { case (x, t) =>
              linkX(x)(inst_wrapper.source.y).contains(t) == false
            }
            val can_route_vert = inst_wrapper.path_y.forall { case (y, t) =>
              linkY(inst_wrapper.target.x)(y).contains(t) == false
            }
            if (can_route_vert && can_route_horiz) {
              logger.debug(
                s"@${cycle}: Scheduling ${inst_wrapper.inst.serialized} in process ${h.proc.id}"
              )(context)
              h.scheduled += (h.unscheduled.dequeue().inst -> cycle)
            }
          }

        }
        h
      }
      cycle += 1
      to_schedule ++= checked
    }

    if (to_schedule.exists(_.unscheduled.nonEmpty)) {
      logger.error("Could not schedule Send instruction in 4096 cycles!")
    }

    // now we have the time at which each send instruction can be scheduled,
    // so we are going to "patch" the partial instruction accordingly
    source.copy(
      processes = partial_schedules.map { p =>
        val body: Seq[Instruction] = to_schedule.find(_.proc == p.proc) match {
          case Some(ProcessWrapper(_, send_schedule, _)) =>
            val nonsend_sched =
              MutableQueue[Instruction]() ++ p.partial_schedule
            val sched_len = nonsend_sched.length + send_schedule.length
            val full_sched = MutableQueue[Instruction]()
            var cycle = 0
            while (nonsend_sched.nonEmpty || send_schedule.nonEmpty) {
              if (send_schedule.nonEmpty && cycle == send_schedule.head._2) {
                full_sched.enqueue(send_schedule.dequeue()._1)
              } else if (nonsend_sched.nonEmpty) {
                full_sched.enqueue(nonsend_sched.dequeue())
              } else {
                full_sched enqueue Nop
              }
              cycle += 1
            }

            full_sched.toSeq
          case None =>
            p.partial_schedule

        }
        p.proc.copy(body = body)
      }
    )
  }


  override def transform(
      source: DefProgram,
      context: AssemblyContext
  ): DefProgram = {

    createGlobalSchedule(source, context)

  }

}
