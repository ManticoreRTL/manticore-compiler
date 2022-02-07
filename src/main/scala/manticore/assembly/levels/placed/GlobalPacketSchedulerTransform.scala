package manticore.assembly.levels.placed

import manticore.assembly.levels.AssemblyTransformer
import manticore.assembly.levels.placed.PlacedIR
import manticore.compiler.AssemblyContext
import scala.collection.parallel.CollectionConverters._
import manticore.assembly.DependenceGraphBuilder
import manticore.assembly.annotations.AssemblyAnnotationFields.{
  X => XField,
  Y => YField,
  FieldName
}

/** A pass to globally schedule/route messages across processes.
  * @author
  *   Mahyar Emami <mahyar.emami@eplf.ch>
  */

import manticore.assembly.annotations.{Layout => LayoutAnnotation}
object GlobalPacketSchedulerTransform
    extends DependenceGraphBuilder
    with AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {
  val flavor = PlacedIR
  import flavor._

  import manticore.assembly.levels.placed.LatencyAnalysis
  override def transform(
      program: DefProgram,
      context: AssemblyContext
  ): DefProgram = {

    val dimx = context.max_dimx
    val dimy = context.max_dimy

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
          x_v -> (earliest + LatencyAnalysis.latency(inst) + i + 1)
        }
      val path_y: Seq[(Int, Int)] = // a tuple of y location and occupancy time
        Seq.tabulate(y_dist) { i =>
          val y_v = (source.y + i) % dimy
          path_x match {
            case _ :+ last => y_v -> (path_x.last._2 + i + 1)
            case Seq() =>
              y_v -> (earliest + LatencyAnalysis.latency(inst) + i + 1)
          }

        }

      // A send instruction that becomes available earlier, has higher priority
      // locally, this is somehow enforced, i.e., the List scheduler assumes
      // that Sends are scheduled in the order in which their data is produced.
      // Without this assumption it is not possible to compute a valid early time
      // since scheduling one Send will change the earliest time of unscheduled ones
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
      program.processes.map { proc =>
        val sends = proc.body.zipWithIndex.collect { case (inst: Send, cycle) =>
          SendWrapper(
            inst = inst,
            source = proc.id,
            target = inst.dest_id,
            earliest = cycle
          )
        }
        ProcessWrapper(
          proc,
          MutableQueue.empty[(Send, Int)], // scheduled sends
          MutableQueue() ++ sends
        )

      }

    object RecvOrdering extends Ordering[(Recv, Int)] {
      override def compare(x: (Recv, Int), y: (Recv, Int)): Int =
        Ordering[Int].reverse.compare(x._2, y._2)
    }
    // a table that maps processes to a queue of Recv instructions
    val recv_queue = program.processes.map { proc =>
      proc.id -> PriorityQueue.empty[(Recv, Int)](RecvOrdering)
    }.toMap

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
              context.logger.debug(
                s"@${cycle}: Scheduling ${inst_wrapper.inst.serialized} in process ${h.proc.id}"
              )

              val send_inst = h.unscheduled.dequeue()

              h.scheduled += (send_inst.inst -> cycle)

              // create a receive instruction
              val recv_inst_time = cycle + send_inst.manhattan
              val recv = Recv(
                rd = send_inst.inst.rd,
                rs = send_inst.inst.rs,
                source_id = send_inst.source
              )
              recv_queue(send_inst.target) += (recv -> recv_inst_time)
            }
          }

        }
        h
      }
      cycle += 1
      to_schedule ++= checked
    }

    if (to_schedule.exists(_.unscheduled.nonEmpty)) {
      context.logger.error(
        "Could not schedule Send instruction in 4096 cycles!"
      )
    }

    // now we have the time at which each send instruction can be scheduled,
    // so we are going to "patch" the partial instruction accordingly
    val scheduled =
      to_schedule.map { case ProcessWrapper(p: DefProcess, send_schedule, _) =>
        val nonsend_sched =
          MutableQueue[Instruction]() ++ p.body.filter {
            _.isInstanceOf[Send] == false
          }

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
        // append the RECV instructions if any
        val recv_to_sched = recv_queue(p.id)
        while (recv_to_sched.nonEmpty) {
          val first_recv = recv_to_sched.head
          val recv_time = first_recv._2
          // insert NOPs if messages are arriving later
          full_sched enqueueAll Seq.fill(recv_time - cycle) { Nop }
          full_sched enqueue first_recv._1
          cycle = recv_time
          recv_to_sched.dequeue()
        }

        p.copy(body = full_sched.toSeq).setPos(p.pos)
      }.toSeq

    program
      .copy(
        processes = scheduled.sortBy(p => (p.id.x, p.id.y))
      )
      .setPos(program.pos)
  }

}
