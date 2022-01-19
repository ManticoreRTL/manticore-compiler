package manticore.assembly.levels.placed

import manticore.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import manticore.assembly.BinaryOperator
import scala.annotation.tailrec

import manticore.assembly.DependenceGraphBuilder
import scalax.collection.edge.LDiEdge
import scala.collection.parallel.CollectionConverters._
import manticore.assembly.levels.UInt16
import manticore.assembly.annotations.AssemblyAnnotationFields.{X => XField, Y => YField, FieldName}
import manticore.assembly.annotations.{Layout => LayoutAnnotation}
/** List scheduler transformation, the output of this transformation is a
  * program with locally scheduled processes. If the input program has Nops,
  * they will be all ignored.
  *
  * IMPORTANT:
  *
  * All the Stores are scheduled after loads since all memories are assumed to
  * have async read and sync write. Therefore, so if a register allocator needs
  * to spill to the array memory it has to store and load registers then it can
  * not use this transformation again.
  *
  * @author
  *   Mahyar emami <mahyar.emami@epfl.ch>
  */
object ListSchedulerTransform
    extends DependenceGraphBuilder
    with AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {

  val flavor = PlacedIR
  import flavor._
  import manticore.assembly.levels.placed.LatencyAnalysis
  import scalax.collection.Graph
  import scalax.collection.mutable.{Graph => MutableGraph}
  import scalax.collection.edge.WDiEdge

  private case class Label(v: Int)

  private def manhattanDistance(
      source: ProcessId,
      target: ProcessId,
      dim: (Int, Int)
  ) =
    LatencyAnalysis.manhattan(source, target, dim)

  private def instructionLatency(inst: Instruction): Int =
    LatencyAnalysis.latency(inst)

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
      dim: (Int, Int),
      ctx: AssemblyContext
  ): DefProcess = {


    // import manticore.assembly.levels.placed.
    val dependence_graph =
      DependenceAnalysis.build[Label](proc, labelingFunc)(ctx)
    // dump the graph
    ctx.logger.dumpArtifact(
      s"dependence_graph_${phase_id}_${proc.id.id}_${ctx.logger.countProgress()}.dot"
    ) {

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
          ctx.logger.error(
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
    }

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

      if (node.edges.filter(_.from == node).isEmpty) {

        // Nodes that do not introduce any RAW dependence should usually
        // have a distance to sink equal to instruction latency, except for
        // Send instruction, because they need to traverse NoC hops. In
        // order to model their distance sink we can use the Manhattan
        // distance which is essentially the number of cycles it takes a
        // message to reach its destination. But since each Send becomes a
        // SetValue at the target we should also include an additional
        // latency into our calculation
        val dist = node.toOuter match {
          case inst @ Send(rd, _, target, _) =>
            instructionLatency(inst) + // local instruction latency
              manhattanDistance(
                proc.id,
                target,
                dim
              ) + // time to traverse the NoC
              instructionLatency(
                SetValue(rd, UInt16(0))
              ) // remote instruction latency
          case inst @ _ =>
            instructionLatency(inst)
        }
        distance_to_sink(node) = dist
        dist
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
          } else {
            0 // irrelevant, the edge is inwards
          }
        }.max
      }
    }

    // find the distance of each node to sinks
    dependence_graph.nodes.foreach { x =>
      traverseGraphNodesAndRecordDistanceToSink(x)
    }

    val send_instructions = proc.body.collect { case i @ Send(_, _, _, _) => i }

    val unsched_list = scala.collection.mutable.ListBuffer[Instruction](
      proc.body.filter(_ != Nop): _* // remove nops, they can not be
      // scheduled because they never become ready (no operands) and they
      // don't matter anyways.
    )

    val schedule = scala.collection.mutable.ListBuffer[Instruction]()

    case class ReadyNode(n: Node) extends Ordered[ReadyNode] {
      def compare(that: ReadyNode) =
        Ordering[Double].reverse
          .compare(distance_to_sink(this.n), distance_to_sink(that.n))

    }

    val ready_list = scala.collection.mutable.PriorityQueue[ReadyNode]()
    // initialize the ready list with instruction that have no predecessor

    ctx.logger.debug(
      dependence_graph.nodes
        .map { n =>
          s"${n.toOuter.serialized} \tindegree ${n.inDegree}\toutdegree ${n.outDegree}"
        }
        .mkString("\n")
    )
    ready_list ++= dependence_graph.nodes
      .filter { n => n.inDegree == 0 }
      .map { ReadyNode(_) }

    ctx.logger.debug(
      s"Initial ready list:\n ${ready_list.map(_.n.toOuter.serialized).mkString("\n")}"
    )

    // create a mutable set showing the satisfied dependencies
    val satisfied_dependence = scala.collection.mutable.Set.empty[Edge]

    // dependence_graph.OuterEdgeTraverser

    var active_list = scala.collection.mutable.ListBuffer[(Node, Double)]()
    // val scheduled_preds = scala.collection.mutable.Set.empty[Node]
    // LIST scheduling simulation loop
    var cycle = 0
    ctx.logger.debug(
      s"Distance to sink \n${distance_to_sink
        .map { case (k, v) => s"${k.serialized} : ${v} " }
        .mkString("\n")}"
    )
    while (unsched_list.nonEmpty && cycle < 4096) {

      val to_retire = active_list.filter(_._2 == 0)
      val finished_list =
        active_list.filter { _._2 == 0 } map { finished =>
          ctx.logger.debug(
            s"${cycle}: Committing ${finished._1.toOuter.serialized} "
          )

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
                ctx.logger.debug(
                  s"${cycle}: Readying ${successor.toOuter.serialized} "
                )
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
        ctx.logger
          .debug(s"${cycle}: Scheduling ${head.n.toOuter.serialized}")
        active_list.append((head.n, instructionLatency(head.n.toOuter)))
        schedule.append(head.n.toOuter)
        unsched_list -= head.n.toOuter
        ready_list.dequeue()
      }
      cycle += 1
    }

    if (cycle >= 4096) {
      ctx.logger.error(
        "Failed to schedule processes, ran out of instruction memory",
        proc
      )
    }

    proc.copy(body = schedule.toSeq)

  }

  override def transform(
      source: DefProgram,
      context: AssemblyContext
  ): DefProgram = {

    def getDim(dim: FieldName): Int =
      source.findAnnotationValue(LayoutAnnotation.name, dim) match {
        case Some(manticore.assembly.annotations.IntValue(v)) => v
        case _ =>
          context.logger.fail("Scheduling requires a valid @LAYOUT annotation")
          0
      }

    val dimx = getDim(XField)
    val dimy = getDim(YField)

    if (context.debug_message) { // run single-threaded if debug enabled
      source.copy(processes = source.processes.map { p =>
        createLocalSchedule(p, (dimx, dimy), context)
      })
    } else {
      source.copy(processes = source.processes.par.map { p =>
        createLocalSchedule(p, (dimx, dimy), context)
      }.seq)
    }
    // createGlobalSchedule(source, context)

  }

}
