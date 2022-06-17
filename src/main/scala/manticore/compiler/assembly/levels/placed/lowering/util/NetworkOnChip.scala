package manticore.compiler.assembly.levels.placed.lowering.util
import manticore.compiler.assembly.levels.placed.PlacedIR._
import manticore.compiler.assembly.levels.placed.LatencyAnalysis
import java.io.PrintWriter

private[lowering] case class RecvEvent(recv: Recv, cycle: Int)

/**
  * Modeling manticore network on chip
  *
  *
  * @param dimX
  * @param dimY
  *
  * @author Mahyar emami <mahyar.emami@epfl.ch>
  */
private[lowering] class NetworkOnChip(val dimX: Int, val dimY: Int) {

  sealed abstract trait Response
  case object Denied               extends Response
  case class Granted(arrival: Int) extends Response

  // the start time given should be the cycle at which a Send gets scheduled

  // plus Latency

  case class Step(loc: Int, t: Int)
  class Path private[NetworkOnChip] (
      val from: ProcessId,
      val send: Send,
      val scheduleCycle: Int
  ) {

    val to = send.dest_id
    val xDist =
      LatencyAnalysis.xHops(from, to, (dimX, dimY))
    val yDist =
      LatencyAnalysis.yHops(from, to, (dimX, dimY))

    /** [[LatencyAnalysis.latency]] gives the number of NOPs required between
      * two depending instruction, that is, it gives the latency between
      * executing and writing back the instruction but the [[Send]] latency
      * should also consider the number of cycles required for fetching and
      * decoding. That is why we add "2" to the number given by
      * [[LatencyAnalysis.latency]]
      */
    val enqueueTime = LatencyAnalysis.latency(send) + 2 + scheduleCycle
    val xHops: Seq[Step] =
      Seq.tabulate(xDist) { i =>
        val x_v = (from.x + i + 1) % dimX

        Step(x_v, (enqueueTime + i))
      }
    val yHops: Seq[Step] = {

      val p = Seq.tabulate(yDist) { i =>
        val y_v = (from.y + i + 1) % dimY
        xHops match {
          case _ :+ last => Step(y_v, (xHops.last.t + i + 1))
          case Seq() => // there are no steps in the X direction
            Step(y_v, (enqueueTime + i))
        }
      }
      // the last hop always occupies a Y link which the is Y output of
      // the target switch (Y output is shared with the local output)
      val lastHop = p match {
        case _ :+ last => Step((to.y + 1) % dimY, (last.t + 1))
        case Nil       =>
          // this happens if the packet only goes in the X direction. The
          // packet should have at least one X hop, hence xHops can not be
          // empty (we don't have self messages so at least one the
          // the two paths are non empty)
          assert(
            xHops.nonEmpty,
            s"Can not have self messages send ${send.serialized} from ${from} with xdist = $xDist and ydist = $yDist"
          )
          Step((to.y + 1) % dimY, (xHops.last.t + 1))
      }
      p :+ lastHop
    }

  }
  private type LinkOccupancy = scala.collection.mutable.Set[Int]
  private val linksX    = Array.ofDim[LinkOccupancy](dimX, dimY)
  private val linksY    = Array.ofDim[LinkOccupancy](dimX, dimY)
  private val usedPaths = scala.collection.mutable.ArrayBuffer.empty[Path]
  def draw(): String = {

    def renderLine(y: Int): String = {
      val topY = new StringBuilder
      val xln  = new StringBuilder
      val botY = new StringBuilder

      val ln = new StringBuilder
      for (x <- 0 until dimX) {
        ln ++= f"${"|"}%12s"
      }
      ln ++= "\n"
      for (x <- 0 until dimX) {
        ln ++= f"    ${linksY(x)(y).size}%8d"
      }
      ln ++= "\n"
      for (x <- 0 until dimX) {
        ln ++= f"${"v"}%12s"
      }
      ln ++= "\n"
      for (x <- 0 until dimX) {
        ln ++= f"${linksX(x)(y).size}%7d->[ ]".replace(" ", "-")
      }
      ln ++= "\n"
      ln.toString()
    }
    val str = new StringBuilder
    str ++= "\n"
    for (y <- 0 until dimY) { str ++= renderLine(y) }
    str.toString()
  }

  def getPaths(): Iterable[Path] = usedPaths

  // initially no link is occupied
  for (x <- 0 until dimX; y <- 0 until dimY) {
    linksX(x)(y) = scala.collection.mutable.Set.empty[Int]
    linksY(x)(y) = scala.collection.mutable.Set.empty[Int]
  }

  def tryReserve(
      from: ProcessId,
      send: Send,
      scheduleCycle: Int
  ): Option[Path] = {
    val path = new Path(from, send, scheduleCycle)
    val canRouteHorizontally = path.xHops.forall { case Step(x, t) =>
      linksX(x)(path.from.y).contains(t) == false
    }
    val canRouteVertically = path.yHops.forall { case Step(y, t) =>
      linksY(path.to.x)(y).contains(t) == false
    }
    if (canRouteVertically && canRouteHorizontally) {
      Some(path)
    } else {
      None
    }
  }

  /** called by a processor to try to enqueue a Send to the NoC
    *
    * @param path
    *   the path the message should traverse
    * @return
    */
  def request(path: Path): RecvEvent = {
    // reserve the links
    assert(tryReserve(path.from, path.send, path.scheduleCycle).nonEmpty)
    usedPaths += path
    for (Step(x, t) <- path.xHops) {
      linksX(x)(path.from.y) += t
    }
    for (Step(y, t) <- path.yHops) {
      linksY(path.to.x)(y) += t
    }
    RecvEvent(
      Recv(
        path.send.rd,
        path.send.rs,
        path.from
      ),
      path.yHops.last.t
    )
  }

}

object NetworkOnChip {

  def jsonDump(network: NetworkOnChip): String = {

    val printer = new StringBuilder
    printer ++= (s"{\n\"topology\": [${network.dimX}, ${network.dimY}], \n\"paths\": [")

    network.getPaths().foreach { p =>
      val ln = s"\"source\": [${p.from.x}, ${p.from.y}],\n" +
        s"\"cycle\": ${p.scheduleCycle},\n" +
        s"\"xHops\": [${p.xHops.map(_.loc).mkString(", ")}],\n" +
        s"\"yHops\": [${p.yHops.map(_.loc).mkString(", ")}],\n" +
        s"\"target\": [${p.to.x}, ${p.to.y}]\n"
      printer ++= ("{")
      printer ++= (ln)
      if (p != network.getPaths().last)
        printer ++= ("},")
      else
        printer ++= ("}")
    }
    printer ++= ("]}")
    printer.toString()
  }
}
