package manticore.compiler.assembly.levels.placed

import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.AssemblyContext
import scala.collection.parallel.CollectionConverters._
import manticore.compiler.assembly.DependenceGraphBuilder
import manticore.compiler.assembly.annotations.AssemblyAnnotationFields.{
  X => XField,
  Y => YField,
  FieldName
}

/** A pass to globally schedule/route messages across processes.
  * @author
  *   Mahyar Emami <mahyar.emami@eplf.ch>
  */

import manticore.compiler.assembly.annotations.{Layout => LayoutAnnotation}
object GlobalPacketSchedulerTransform
    extends DependenceGraphBuilder
    with PlacedIRTransformer {
  val flavor = PlacedIR
  import flavor._

  import manticore.compiler.assembly.levels.placed.LatencyAnalysis
  override def transform(
      program: DefProgram
  )(implicit context: AssemblyContext): DefProgram = {

    val dimx = context.max_dimx
    val dimy = context.max_dimy

    // a wrapper class for Send instruction
    case class SendWrapper(
        inst: Send,
        source: ProcessId,
        target: ProcessId,
        earliest: Int
    ) {
      val x_dist =
        LatencyAnalysis.xHops(source, target, (dimx, dimy))
      val y_dist =
        LatencyAnalysis.yHops(source, target, (dimx, dimy))
      val manhattan =
        x_dist + y_dist

      val path_x: Seq[(Int, Int)] = // a tuple of x location and occupancy time
        Seq.tabulate(x_dist) { i =>
          val x_v = (source.x + i + 1) % dimx

          /** [[LatencyAnalysis.latency]] gives the number of NOPs required
            * between two depending instruction, that is, it gives the latency
            * between executing and writing back the instruction but the
            * [[Send]] latency should also consider the number of cycles
            * required for fetching and decoding. That is why we add "2" to the
            * number given by [[LatencyAnalysis.latency]]
            */
          x_v -> (LatencyAnalysis.latency(inst) + i + 2)
        }
      val path_y: Seq[(Int, Int)] = {
        // a tuple of y location and cycle offset from the time the
        // instruction gets scheduled
        // that is, we record the tim
        val p = Seq.tabulate(y_dist) { i =>
          val y_v = (source.y + i + 1) % dimy
          path_x match {
            case _ :+ last => y_v -> (path_x.last._2 + i + 1)
            case Seq() =>
              y_v -> (LatencyAnalysis.latency(inst) + i + 2)
          }
        }
        // the last hop always occupies a Y link which the is Y output of
        // the target switch (Y output is shared with the local output)
        val last_hop = p match {
          case _ :+ last => Seq(((target.y + 1) % dimy) -> (last._2 + 1))
          case Seq() if path_x.nonEmpty =>
            // this happens if the packet only goes in the X direction and the
            // packet should have at least one X hop, hence path_x can not be
            // empty (we don't have self messages so at least one the
            // the two paths are non empty)
            assert(path_x.nonEmpty)
            Seq(((target.y + 1) % dimy) -> (path_x.last._2 + 1))
          case _ =>
            context.logger.error(s"Can not have self messages!", inst)
            Seq.empty[(Int, Int)]
        }
        p ++ last_hop
      }
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
            Ordering[Int].compare(x.manhattan, y.manhattan)
          case (MutableQueue(), y +: _) => -1
          case (x +: _, MutableQueue()) => 1
        }
    }
    // now patch the local schedule by globally scheduling the send instructions
    import scala.collection.mutable.PriorityQueue

    // keep a sorted queue of Processes, sorting is done based on the priority
    // of each processes' send instruction. I.e., the process with the most
    // critical send (largest manhattan distance should be considered first when
    // trying to schedule sends in a cycle
    val to_schedule = PriorityQueue.empty[ProcessWrapper] ++
      program.processes.map { proc =>
        def isSend(x: Instruction) = x.isInstanceOf[Send]
        // create a map from register names to the Sends corresponding to them
        // a single Name may be sent out multiple times (to different targets)

        val sends: Map[Name, Seq[Send]] = proc.body
          .collect { case x: Send =>
            x
          }
          .groupBy(_.rs)
          .map { case (rs, sends: Seq[Send]) =>
            // in case there are multiple Send instructions for the same value
            // prioritize the ones that have to traverse a longer distance
            object SendTieBreakerOrder extends Ordering[Send] {
              override def compare(s1: Send, s2: Send): Int = {
                val m1 =
                  LatencyAnalysis.manhattan(proc.id, s1.dest_id, (dimx, dimy))
                val m2 =
                  LatencyAnalysis.manhattan(proc.id, s2.dest_id, (dimx, dimy))
                Ordering[Int].reverse.compare(m1, m2)
              }
            }
            rs -> sends.sorted(SendTieBreakerOrder)
          }

        // wrap the sends in a helper class that has the manhattan distance
        // precomputed
        val sends_wrapped = scala.collection.mutable.Queue.empty[SendWrapper]
        // IMPORTANT: Go through the instructions IN ORDER and find the
        // instructions that define the values used in Send instruction.
        // The cycle at which the other instruction is scheduled plus the
        // pipeline latency indicates the earliest time that Send can be
        // scheduled alone. However, if we schedule on of these Sends, the
        // later ones' earliest times will be naturally incremented by one
        // we can do this by having appending the wrapped instruction
        // to a list one by one and offsetting the earliest cycle by the
        // size of the list + 1.

        proc.body.zipWithIndex.foreach { case (other_inst, cycle) =>
          DependenceAnalysis
            .regDef(other_inst)
            .find(sends.contains) match {
            case Some(reg_to_send: Name) =>
              // Note that since we are traversing proc.body in order, we are
              // implicitly prioritizing the Sends that have their operands
              // ready earlier. Additionally, the sends(reg_to_send) is ordered
              // by decreasing manhattan distance. So we schedule the ones that
              // travel further first.

              // With s = sends_wrapped.length, the first Send can be schedule
              // at cycle + 1 + n + s where n is latency of the instruction that
              // define the value to be sent the next one would be at cycle + 2
              // + n + s and so on...

              sends(reg_to_send).foldLeft(
                cycle + 1 + sends_wrapped.length + LatencyAnalysis.latency(
                  other_inst
                )
              ) { case (earliest, send_inst) =>
                sends_wrapped +=
                  SendWrapper(
                    inst = send_inst,
                    source = proc.id,
                    target = send_inst.dest_id,
                    earliest = earliest
                  )
                earliest + 1
              }

            case None =>
          }
        }
        context.logger.debug(
          s"In process: ${proc.id}:\n${sends_wrapped mkString ("\n")}"
        )

        if (context.debug_message) {
          if (proc.body.count(isSend) != sends_wrapped.size) {
            context.logger.error(
              s"Not all Send instructions are accounted for in ${proc.id}, " +
                s"are you sending constants? "
            )
          }
        }

        ProcessWrapper(
          proc,
          MutableQueue.empty[(Send, Int)], // scheduled sends
          sends_wrapped
        )

      }

    // keep a sorted queue of RECV in increasing recv order
    // since the priority queue sorts the collection in decreasing priority
    // we need to use the reverse ordering type class
    object RecvOrdering extends Ordering[(Recv, Int)] {
      override def compare(x: (Recv, Int), y: (Recv, Int)): Int =
        Ordering[Int].reverse.compare(x._2, y._2)
    }
    // a table that maps processes to a queue of Recv instructions
    val recv_queue = program.processes.map { proc =>
      proc.id -> PriorityQueue.empty[(Recv, Int)](RecvOrdering)
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
    var schedule_unfinished = true
    while (
      cycle < context.max_instructions_threshold && to_schedule.exists(
        _.unscheduled.nonEmpty
      )
    ) {

      // go through all processes and try to schedule the highest priority
      // send instruction in each
      val checked = to_schedule.dequeueAll.map { h: ProcessWrapper =>
        if (h.unscheduled.nonEmpty) {
          val inst_wrapper = h.unscheduled.head
          val earliest = inst_wrapper.earliest
          if (earliest <= cycle) {

            val can_route_horiz = inst_wrapper.path_x.forall { case (x, t) =>
              linkX(x)(inst_wrapper.source.y).contains(cycle + t) == false
            }
            val can_route_vert = inst_wrapper.path_y.forall { case (y, t) =>
              linkY(inst_wrapper.target.x)(y).contains(cycle + t) == false
            }
            if (can_route_vert && can_route_horiz) {
              context.logger.debug(
                s"@${cycle}: Scheduling ${inst_wrapper.inst.serialized} in process ${h.proc.id}" +
                  s"\nPath_x: ${inst_wrapper.path_x}\nPath_y: ${inst_wrapper.path_y}"
              )

              val send_inst = h.unscheduled.dequeue()

              inst_wrapper.path_x.foreach { case (x, t) =>
                linkX(x)(inst_wrapper.source.y) += (cycle + t)
              }
              inst_wrapper.path_y.foreach { case (y, t) =>
                linkY(inst_wrapper.target.x)(y) += (cycle + t)
              }

              h.scheduled += (send_inst.inst -> cycle)

              // create a receive instruction
              val recv_inst_time = inst_wrapper.path_y.last._2 + cycle
              val recv = Recv(
                rd = send_inst.inst.rd,
                rs = send_inst.inst.rs,
                source_id = send_inst.source
              )
              context.logger.debug(s"recv time ${recv_inst_time}", recv)
              recv_queue(send_inst.target) += (recv -> recv_inst_time)
            }
          }

        }
        h
      }
      cycle += 1
      to_schedule ++= checked
    }

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

    if (to_schedule.exists(_.unscheduled.nonEmpty)) {
      context.logger.error(
        s"Could not schedule Send instruction in ${context.max_instructions_threshold} cycles!"
      )
      val left = to_schedule
        .filter(_.unscheduled.nonEmpty)
        .flatMap { _.unscheduled }
        .toSeq
      context.logger.debug(
        s"remaining instructions:\n${left.mkString("\n")}"
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

        context.logger.debug {
          val cp = recv_to_sched.clone().dequeueAll
          s"RECVs in ${p.id}:\n${cp.map { i =>
            s"${i._1}: ${i._2}"
          } mkString "\n"}"
        }

        while (recv_to_sched.nonEmpty) {
          val first_recv = recv_to_sched.dequeue()
          val recv_time = first_recv._2
          // insert NOPs if messages are arriving later
          if (recv_time > cycle) {
            full_sched enqueueAll Seq.fill(recv_time - cycle) { Nop }
            cycle = recv_time + 1
          } else {
            cycle = cycle + 1
          }
          full_sched enqueue first_recv._1

        }

        if (full_sched.last.isInstanceOf[Recv]) {
          // in case the last instruction is a RECV, put  a NOP to ensure the
          // RECV gets translated to an instruction. Note that if there are
          // multiple RECV, this is not necessary. So this can be optimized
          full_sched enqueue Nop
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
