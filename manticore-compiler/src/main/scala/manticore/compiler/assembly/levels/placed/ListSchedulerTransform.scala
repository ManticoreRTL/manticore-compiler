package manticore.compiler.assembly.levels.placed

import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.BinaryOperator
import scala.annotation.tailrec

import manticore.compiler.assembly.DependenceGraphBuilder
import scalax.collection.edge.LDiEdge
import scala.collection.parallel.CollectionConverters._
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.assembly.annotations.AssemblyAnnotationFields.{
  X => XField,
  Y => YField,
  FieldName
}
import manticore.compiler.assembly.annotations.{Layout => LayoutAnnotation}
import scalax.collection.GraphTraversal
import manticore.compiler.assembly.levels.CloseSequentialCycles
import manticore.compiler.assembly.ManticoreAssemblyIR

/** List scheduler transformation, the output of this transformation is a
  * program with locally scheduled processes. If the input program has Nops,
  * they will be all ignored.
  *
  * IMPORTANT:
  *
  * All the Stores are scheduled after loads since all memories are assumed to
  * have async read and sync write. Therefore, if a register allocator needs to
  * spill to the array memory it has to store and load registers then it can not
  * use this transformation again.
  *
  * @author
  *   Mahyar emami <mahyar.emami@epfl.ch>
  */
object ListSchedulerTransform
    extends DependenceGraphBuilder
    with AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {

  val flavor = PlacedIR
  import flavor._
  import manticore.compiler.assembly.levels.placed.LatencyAnalysis
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

  def createLocalSchedule(
      proc: DefProcess,
      dim: (Int, Int)
  )(implicit ctx: AssemblyContext): DefProcess = {

    // import manticore.compiler.assembly.levels.placed.
    val dependence_graph =
      DependenceAnalysis.build[Label](proc, labelingFunc)(ctx)

    // instructions require to close sequential cycles
    val output_input_pairs = createInputOutputPairs(proc)(ctx).map {
      case (curr, next) => next.variable.name -> curr.variable.name
    }.toMap

    val move_instructions = output_input_pairs.map { case (next, curr) =>
      curr -> Mov(curr, next)
    }.toMap

    // create WAR between input(current) register uses and update instructions
    // of the form Mov(curr, next) that we are adding now.

    proc.body.foreach { inst =>
      val uses = DependenceAnalysis.regUses(inst)(ctx)
      val defs = DependenceAnalysis.regDef(inst)(ctx)
      defs.foreach { d =>
        // is this instruction defining the next value of a register?
        // If so create a RAW dependency
        output_input_pairs.get(d) match {
          case Some(curr) =>
            val mov = move_instructions(curr)
            dependence_graph += LDiEdge(inst, mov)(labelingFunc(inst, mov))
          case _ => // do nothing
        }
      }
      uses.foreach { u =>
        // is this instruction using an input? If so create a WAR dependency
        move_instructions.get(u) match {
          case Some(mov) => dependence_graph += LDiEdge(inst, mov)(Label(1))
          // notice that if an input register is never used, we don't
          // create an edge (hence a MOV node) here.
          case _ => // nothing to be done
        }
      }
    }

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
              List(DotAttr("label", inode.toOuter.toString().trim))
            )
          )
        )

      val dot_export: String = dependence_graph.toDot(
        dotRoot = dot_root,
        edgeTransformer = edgeTransform,
        cNodeTransformer = Some(nodeTransformer),
        iNodeTransformer = Some(nodeTransformer)
      )
      dot_export
    }

    ctx.logger.flush()
    type Node = dependence_graph.NodeT
    type Edge = dependence_graph.EdgeT
    val distance_to_sink = scala.collection.mutable.Map[Node, Int]()

    require(
      dependence_graph.edges.forall(_.isOut),
      "dependence graph is mal-formed!"
    )

    if (!dependence_graph.isAcyclic) {
      ctx.logger.error(s"Dependence graph for process ${proc.id} is cyclic!")
      ctx.logger.fail("Failed to schedule process")
    }

    def traverseAndSetDistanceToSinkNonRecursive(node: Node): Int = {

      def sinkDistance(n: Node): Int = n.toOuter match {
        // Nodes that do not introduce any RAW dependence should usually
        // have a distance to sink equal to instruction latency, except for
        // Send instruction, because they need to traverse NoC hops. In
        // order to model their distance sink we can use the Manhattan
        // distance which is essentially the number of cycles it takes a
        // message to reach its destination. But since each Send becomes a
        // SetValue at the target we should also include an additional
        // latency into our calculation
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

      distance_to_sink.get(node) match {
        case Some(dist) => dist // distance already cached
        case None => // need to perform a depth first traversal
          node.innerNodeDownUpTraverser
            .withFilter { case (_, node) =>
              distance_to_sink.contains(node) == false
            }
            .foreach { case (downwards, inode) =>
              if (!downwards) {
                if (inode.outDegree == 0) {
                  val dist = sinkDistance(inode)
                  distance_to_sink(inode) = dist
                } else {
                  val dist = inode.outgoing.map { edge =>
                    val Label(v) = edge.label
                    val succ_dist = distance_to_sink(edge.target) + v
                    succ_dist
                  }.max
                  distance_to_sink(inode) = dist
                }
              }
            }
          distance_to_sink(node)
      }

    }

    // somehow faster, both have the same complexity though I think (visit each node twice)
    def traverseAndSetDistanceToSinkRecursive(node: Node): Int = {
      if (node.outDegree == 0) {
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
        node.outgoing.map { edge =>
          val Label(v) = edge.label
          val dist: Int =
            traverseAndSetDistanceToSinkRecursive(edge.to) + v
          distance_to_sink(node) = dist
          dist
        }.max
      }
    }

    // val some_root_node = dependence_graph.
    // find the distance of each node to sinks
    val now = System.nanoTime()
    dependence_graph.nodes filter { _.inDegree == 0 } foreach {
      traverseAndSetDistanceToSinkRecursive
    }
    val end = System.nanoTime()

    // We filter out the instructions and only schedule the relevant ones.
    // Here, [[Nop]]s are removed since they are always ready and also useless
    // to have them in an unscheduled process. [[Send]] are also removed from
    // the schedule since we need to create a global schedule for them. Note
    // that if schedule them now and a later pass move them around, that pass
    // could end up invalidating the local schedule by "bringing some
    // instructions too close to each other"
    //
    // E.g.,
    // ADD x,_,_
    // NOP
    // SEND y, _, _
    // NOP
    // ADD z, x, _
    // may become
    // ADD x,_,_
    // NOP
    // NOP
    // ADD z, x, _
    // SEND y, _, _
    // Which is invalid because between the definition of x and its use there
    // are not enough instructions
    // Albeit, this does not mean the Send instruction do not play any role in
    // the local schedule. In fact they are used to compute the sink distance
    // to prioritize instructions that their result are sent out of the process
    // over those that do not send them out.
    //
    // After we done creating the local schedule, the Send instruction are simply
    // appended at the end of the schedule.


    val sends = proc.body.collect { case x: Send => x }
    dependence_graph --= sends

    // val unsched_list =
    //   scala.collection.mutable.Queue.empty[Instruction] ++
    //     dependence_graph.nodes.collect {
    //       case n if n.toOuter != Nop && !n.toOuter.isInstanceOf[Send] =>
    //         n.toOuter
    //     }

    val schedule = scala.collection.mutable.Queue[Instruction]()

    case class ReadyNode(n: Node, dist: Int) extends Ordered[ReadyNode] {
      def compare(that: ReadyNode) = {
        (this.n.toOuter, that.n.toOuter) match {
          case (
                Expect(_, _, this_id: ExceptionIdImpl, _),
                Expect(_, _, that_id: ExceptionIdImpl, _)
              ) =>
            (this_id.kind, that_id.kind) match {
              case (ExpectFail, ExpectStop) => 1
              // prioritize Fail type so that we don't end up
              // stopping without checking for the error. Note that we don't
              // need to add any edges in the dependence graph because EXPECTs
              // are going to always be "source" nodes and always ready.
              case (ExpectStop, ExpectFail) => -1
              case (_, _) => Ordering[Int].reverse.compare(this.dist, that.dist)
            }
          case (_, _) =>
            Ordering[Int].reverse
              .compare(this.dist, that.dist)
        }
      }

    }

    val ready_list = scala.collection.mutable.PriorityQueue.empty[ReadyNode]
    // initialize the ready list with instruction that have no predecessor

    ctx.logger.debug(
      dependence_graph.nodes
        .map { n =>
          s"${n.toOuter.serialized} \tindegree ${n.inDegree}\toutdegree ${n.outDegree}"
        }
        .mkString("\n")
    )
    ready_list ++= dependence_graph.nodes.collect {
      case n if n.inDegree == 0 => ReadyNode(n, distance_to_sink(n))
    }

    ctx.logger.debug(
      s"Initial ready list:\n ${ready_list.map(_.n.toOuter.serialized).mkString("\n")}"
    )

    class ActiveNode(val node: Node, var time_left: Int)
    var active_list = scala.collection.mutable.ListBuffer[ActiveNode]()
    // val scheduled_preds = scala.collection.mutable.Set.empty[Node]
    // LIST scheduling simulation loop
    var cycle = 0
    ctx.logger.debug(
      s"Distance to sink \n${distance_to_sink
        .map { case (k, v) => s"${k.serialized} : ${v} " }
        .mkString("\n")}"
    )
    while (dependence_graph.nodes.nonEmpty) {
      val finished_list =
        active_list.filter { _.time_left == 0 } map { f =>
          val node = f.node
          ctx.logger.debug(
            s"${cycle}: Committing ${node.toOuter} "
          )
          node.diSuccessors.foreach { succ =>
            if (succ.inDegree == 1) {
              ctx.logger.debug(
                s"${cycle}: Readying ${succ.toOuter.serialized} "
              )
              ready_list += ReadyNode(succ, distance_to_sink(succ))
            }
          }
          dependence_graph -= node
          f
        }
      active_list --= finished_list
      active_list.foreach { an => an.time_left -= 1 }

      if (ready_list.isEmpty) {
        schedule.append(Nop)
      } else {
        val head = ready_list.head
        // check if a predicate instruction is needed before scheduling the head
        ctx.logger
          .debug(s"${cycle}: Scheduling ${head.n.toOuter.serialized}")
        active_list.append(
          new ActiveNode(head.n, instructionLatency(head.n.toOuter))
        )
        schedule.append(head.n.toOuter)
        ready_list.dequeue()
      }
      cycle += 1
    }



    if (cycle + sends.length >= ctx.max_instructions_threshold) {
      ctx.logger.error(
        "Failed to schedule processes, ran out of instruction memory",
        proc
      )
    }

    val new_body = schedule.toSeq ++ sends

    if (ctx.debug_message) {
      // make sure no instruction is left out
      val old_inst_set = proc.body.filter(_ != Nop).toSet
      val new_inst_set = new_body.filter(_ != Nop).toSet
      if (old_inst_set.size > new_inst_set.size) {
        ctx.logger.error(
          s"some instructions are not present in the final schedule of process ${proc.id}"
        )
      }
    }

    proc.copy(body = new_body.toSeq)

  }

  override def transform(
      source: DefProgram,
      context: AssemblyContext
  ): DefProgram = {

    if (context.debug_message) { // run single-threaded if debug enabled
      source.copy(processes = source.processes.map { p =>
        createLocalSchedule(p, (context.max_dimx, context.max_dimy))(context)
      })
    } else {
      source.copy(processes = source.processes.par.map { p =>
        createLocalSchedule(p, (context.max_dimx, context.max_dimy))(context)
      }.seq)
    }
    // createGlobalSchedule(source, context)

  }

}
