package manticore.assembly.levels.placed

import manticore.assembly.levels.AssemblyTransformer
import manticore.assembly.levels.placed.PlacedIR
import manticore.compiler.AssemblyContext
import scala.collection.parallel.CollectionConverters._
import manticore.assembly.DependenceGraphBuilder
import manticore.assembly.annotations.AssemblyAnnotationFields.{X => XField, Y => YField, FieldName}
import manticore.assembly.annotations.{Layout => LayoutAnnotation}
object GlobalPacketSchedulerTransform
    extends DependenceGraphBuilder with AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {
  val flavor = PlacedIR
  import flavor._

  import manticore.assembly.levels.placed.LatencyAnalysis
  override def transform(
      program: DefProgram,
      context: AssemblyContext
  ): DefProgram = {

    def getDim(dim: FieldName): Int =
      program.findAnnotationValue(LayoutAnnotation.name, dim) match {
        case Some(manticore.assembly.annotations.IntValue(v)) => v
        case _ =>
          context.logger.fail("Scheduling requires a valid @LAYOUT annotation")
          0
      }
    val dimx = getDim(XField)
    val dimy = getDim(YField)

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
          MutableQueue(), // scheduled sends
          MutableQueue() ++ sends
        )

      }

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
      context.logger.error("Could not schedule Send instruction in 4096 cycles!")
    }

    // now we have the time at which each send instruction can be scheduled,
    // so we are going to "patch" the partial instruction accordingly
    program.copy(
      processes = program.processes.par.map { p =>
        val body: Seq[Instruction] = to_schedule.find(_.proc == p) match {
          case Some(ProcessWrapper(_, send_schedule, _)) =>
            val nonsend_sched =
              MutableQueue[Instruction]() ++ p.body.filter {_.isInstanceOf[Send] == false }
            val sched_len = p.body.length
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
            p.body

        }
        p.copy(body = body)
      }.seq
    )

  }

}
