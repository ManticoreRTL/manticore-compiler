package manticore.compiler.assembly.levels.placed.lowering.util
import scalax.collection.Graph
import scalax.collection.GraphEdge
import scalax.collection.mutable.{Graph => MutableGraph}
import manticore.compiler.assembly.levels.placed.PlacedIR._

/**
  * Modeling a manticore processor for scheduling. The code is messy, do not
  * try to understand it if you don't need to. Sorry :(
  *
  * @author Mahyar Emami <mahyar.emami@epfl.ch>
  */
private[lowering] object Processor {

  type DependenceGraph = Graph[Instruction, GraphEdge.DiEdge]
  sealed trait ProcessorState
  case object MainBlock extends ProcessorState
  case class DelaySlot(jtb: JumpTable, pos: Int) extends ProcessorState
  case class CaseBlock(origJtb: JumpTable, pos: Int) extends ProcessorState

  class JtbBuilder(orig: JumpTable) {
    val newNames =
      orig.blocks.map { case JumpCase(lbl, _) =>
        lbl -> scala.collection.mutable.Map
          .empty[Name, Name]
          .withDefault(n => n)
      }
    def addMapping(label: Label, mapping: Seq[(Name, Name)]) =
      newNames.find(_._1 == label).foreach { _._2 ++= mapping }
    def getMapping(label: Label) = newNames.find(label == _._1).get._2
    val phis = scala.collection.mutable.Queue.empty[Phi]
    val dslot = scala.collection.mutable.Queue.empty[Instruction]
    val blocks = orig.blocks.map { case JumpCase(lbl, blk) =>
      lbl -> scala.collection.mutable.ArrayBuffer.from(blk)
    }

  }
}

private[lowering] class Processor(
    val process: DefProcess,
    val scheduleContext: ScheduleContext
) {

  import Processor._
  var state: ProcessorState = MainBlock
  var jtbBuilder: JtbBuilder = _
  var currentCycle: Int = 0
  val newDefs = scala.collection.mutable.Queue.empty[DefReg]

  // keep a sorted queue of Recv events in increasing recv time
  // since the priority queue sorts the collection in decreasing priority
  // we need to use .reverse on the ordering
  private val recvQueue = scala.collection.mutable.Queue.empty[RecvEvent]
  private var activePredicate: Option[Name] = None

  def hasPredicate(n: Name): Boolean = activePredicate match {
    case None        => false
    case Some(value) => value == n
  }
  def activatePredicate(n: Name): Unit = {
    activePredicate = Some(n)
  }
  def checkCollision(recvEv: RecvEvent): Option[RecvEvent] =
    recvQueue.find(_.cycle == recvEv.cycle)

  def notifyRecv(recvEv: RecvEvent): Unit = {
    assert(checkCollision(recvEv).isEmpty, "Collision in recv port")
    recvQueue enqueue recvEv
  }

  // get the RecvEvents in increasing time order
  def getReceivesSorted(): Seq[RecvEvent] =
    recvQueue.toSeq.sorted(Ordering.by { recvEv: RecvEvent =>
      recvEv.cycle
    })
}
private[lowering] final class ScheduleContext(
    dependenceGraph: Processor.DependenceGraph,
    priority: Ordering[Processor.DependenceGraph#NodeT]
) {

  val graph = {
    // a copy of the original dependence graph excluding the Send instructions

    val builder = MutableGraph.empty[Instruction, GraphEdge.DiEdge]
    builder ++= dependenceGraph.nodes.filter(!_.isInstanceOf[Send])
    builder ++= dependenceGraph.edges.filter {
      case GraphEdge.DiEdge(_, _: Send) => false
      case _                            => true
    }
    builder
  }

  private var nodesToSchedule = dependenceGraph.nodes.length
  private val schedule = scala.collection.mutable.Queue.empty[Instruction]

  // add an instruction to schedule
  def +=(inst: Instruction) = {
    assert(nodesToSchedule != 0)
    schedule += inst
    if (inst != Nop)
      nodesToSchedule -= 1
  }
  // don't really add the instruction but advance the state
  def +?=(inst: Instruction): Unit = {
    assert(nodesToSchedule != 0)
    if (inst != Nop)
      nodesToSchedule -= 1
  }

  // pre-populate the ready list
  val readyList = scala.collection.mutable.PriorityQueue
    .empty[Processor.DependenceGraph#NodeT](priority) ++ graph.nodes.filter {
    _.inDegree == 0
  }

  val activeList =
    scala.collection.mutable.Map.empty[Processor.DependenceGraph#NodeT, Int]

  def finished(): Boolean = nodesToSchedule == 0

  def getSchedule() = schedule.toSeq

}
