package manticore.compiler.assembly.levels.placed

import manticore.compiler.assembly.levels.AssemblyChecker
import manticore.compiler.AssemblyContext

object LinkUtilizationChecker extends AssemblyChecker[PlacedIR.DefProgram] {

  import PlacedIR._

  override def check(program: DefProgram, context: AssemblyContext): Unit = {
    if (
      !(context.max_dimx == 1 && context.max_dimy == 1) || program.processes.length == 1
    ) {
      checkAndReport(program, context)
    } else {
      context.logger.info(
        "Skipping link utilization check since there is only a single processor"
      )
    }
  }
  def checkAndReport(program: DefProgram, context: AssemblyContext): Unit = {

    // compute the maximum number of cycles possible having all messages
    // delivered
    val max_body_length = program.processes
      .map(_.body.length)
      .max
    val max_cycles =
      max_body_length + context.max_dimx + context.max_dimy + 2 * (LatencyAnalysis
        .maxLatency() + 2)

    def createLinks() = Array.fill(max_cycles) {
      Array.fill(context.max_dimx) {
        Array.fill(context.max_dimy) {
          Option.empty[Send]
        }
      }
    }

    val linkX = createLinks()
    val linkY = createLinks()

    val last_recv_cycle = program.processes.map { p =>
      val recv_cycles: Seq[Int] = p.body.zipWithIndex
        .collect { case (s: Send, cycle: Int) => (s, cycle) }
        .map { case (send: Send, cycle: Int) =>
          val source_id = p.id
          val target_id = send.dest_id
          // simulate the passage of this message and record every hop
          val (x_dist, y_dist) = LatencyAnalysis.xyHops(
            source_id,
            target_id,
            (context.max_dimx, context.max_dimy)
          )
          val x_path = Seq.tabulate(x_dist) { i =>
            ((source_id.x + 1 + i) % context.max_dimx, source_id.y)
          }
          val y_path = Seq.tabulate(y_dist + 1) { j =>
            (target_id.x, (source_id.y + 1 + j) % context.max_dimy)
          }
          val y_t = x_path.foldLeft(cycle + LatencyAnalysis.latency(send) + 2) {
            case (t, (x, y)) =>
              linkX(t)(x)(y) match {
                case Some(other_send) =>
                  context.logger.error(
                    s"Send collision between:\n${send}\n${other_send}"
                  )
                case None => // nothing
              }
              linkX(t)(x)(y) = Some(send)
              t + 1
          }
          val recv_time = y_path.foldLeft(y_t) { case (t, (x, y)) =>
            linkY(t)(x)(y) match {
              case Some(other_send) =>
                context.logger.error(
                  s"Send collision between:\n${send}\n${other_send}"
                )
              case None => // nothing
            }
            linkY(t)(x)(y) = Some(send)
            t + 1
          }
          recv_time
        }
      recv_cycles match {
        case Seq() => 0
        case _ => recv_cycles.max
      }
    }.max

    if (last_recv_cycle > max_body_length) {
      context.logger.error(
        s"Last receive is at cycle ${last_recv_cycle} but last processes sleeps at ${max_body_length}!"
      )
    }

    // report link occupancy

    def computeBandwidth(
        name: String,
        link: Array[Array[Array[Option[Send]]]]
    ): Unit = {
      val bits = link.flatMap {
        _.flatMap { _.map { s => if (s.nonEmpty) 1 else 0 } }
      }.sum * 16
      context.logger.info(
        s"Moves total ${bits} bits in link ${name} in ${last_recv_cycle} cycles"
      )
      val bw = bits.toDouble / last_recv_cycle
      context.logger.info(
        f"link ${name} utilization is ${bw}%.3f bits per cycle"
      )
    }
    val linkx_traffic = computeBandwidth("linkX", linkX)
    val linky_traffic = computeBandwidth("linkY", linkY)

  }

}
