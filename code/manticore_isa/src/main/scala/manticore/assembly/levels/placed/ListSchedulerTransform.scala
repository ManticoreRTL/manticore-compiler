package manticore.assembly.levels.placed

import manticore.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import manticore.assembly.BinaryOperator
import scala.annotation.tailrec
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import manticore.assembly.CompilationFailureException

object ListSchedulerTransform extends AssemblyTransformer(PlacedIR, PlacedIR) {

  import PlacedIR._
  import scalax.collection.Graph
  import scalax.collection.edge.WDiEdge

  def instructionLatency(instruction: Instruction): Int = 3

  /** Extract the registers used in the instruction
    *
    * @param inst
    * @return
    */
  private def regUses(inst: Instruction): List[Name] = inst match {
    case BinaryArithmetic(BinaryOperator.PMUX, rd, rs1, rs2, _) =>
      logger.warn("PMUX instruction may lead to invalid scheduling!", inst)
      List(rs1, rs2)
    case BinaryArithmetic(operator, rd, rs1, rs2, _) =>
      List(rs1, rs2)
    case CustomInstruction(func, rd, rs1, rs2, rs3, rs4, _) =>
      List(rs1, rs2, rs3, rs4)
    case LocalLoad(rd, base, offset, _) =>
      List(base)
    case LocalStore(rs, base, offset, p, _) =>
      List(rs, base) ++ (p match {
        case None =>
          logger.warn(
            "Local store instruction does not have a predicate, the scheduling maybe invalid!",
            inst
          )
          List.empty[Name]
        case Some(n) => List(n)
      })

    case GlobalLoad(rd, base, _) =>
      List(base._1, base._2, base._3)
    case GlobalStore(rs, base, pred, _) =>
      List(rs, base._1, base._2, base._3) ++ (pred match {
        case None =>
          logger.warn(
            "GlobalStore instruction does not have a predicate, the scheduling maybe invalid!",
            inst
          )
          List.empty[Name]
        case Some(n) =>
          List(n)
      })

    case Send(rd, rs, dest_id, _)  => List(rs)
    case SetValue(rd, value, _)    => List()
    case Mux(rd, sel, rs1, rs2, _) => List(sel, rs1, rs2)
    case Expect(ref, got, error_id, _) =>
      List(ref, got)
    case Predicate(rs, _) =>
      logger.warn(
        "PREDICATE should be added after scheduling! The final schedule maybe invalid!",
        inst
      )
      List(rs)
    case Nop => List()

  }

  /** Extract the register define by the instruction if it defines one
    *
    * @param inst
    * @return
    */
  private def regDef(inst: Instruction): Option[Name] = inst match {
    case BinaryArithmetic(operator, rd, rs1, rs2, _)        => Some(rd)
    case CustomInstruction(func, rd, rs1, rs2, rs3, rs4, _) => Some(rd)
    case LocalLoad(rd, base, offset, _)                     => Some(rd)
    case LocalStore(rs, base, offset, p, _)                 => None
    case GlobalLoad(rd, base, _)                            => Some(rd)
    case GlobalStore(rs, base, pred, _)                     => None
    case Send(rd, rs, dest_id, _)                           => None
    case SetValue(rd, value, _)                             => Some(rd)
    case Mux(rd, sel, rs1, rs2, _)                          => Some(rd)
    case Expect(ref, got, error_id, _)                      => None
    case Predicate(rs, _)                                   => None
    case Nop                                                => None
  }
  private def createDependenceGraph(
      process: DefProcess,
      ctx: AssemblyContext
  ): Graph[Instruction, WDiEdge] = {

    // A map from registers to the instruction defining it (if any), useful for back tracking
    val def_instructions =
      scala.collection.mutable.Map[Name, Instruction]()
    process.body.foreach { inst =>
      regDef(inst) match {
        case Some(rd) =>
          // keep a map from the target regs to the instruction defining it
          def_instructions.update(rd, inst)

        case None =>
        // instruction does not a value, hence no new dependence edges
      }
    }

    type EitherLoad = Either[LocalLoad, GlobalLoad]
    type EitherStore = Either[LocalStore, GlobalStore]
    val loads: Seq[EitherLoad] = process.body.collect {
      case ll @ LocalLoad(_, _, _, _) => Left(ll)
      case gl @ GlobalLoad(_, _, _)   => Right(gl)
    }

    val stores: Seq[EitherStore] = process.body.collect {
      case ls @ LocalStore(_, _, _, _, _) => Left(ls)
      case gs @ GlobalStore(_, _, _, _)   => Right(gs)

    }

    val mem_decls: Seq[MemoryVariable] = process.registers.collect {
      case DefReg(m @ MemoryVariable(_, _, _), _, _) => m
    }

    // a map from register to potential memory (.mem) declaration
    val mem_block =
      scala.collection.mutable.Map[Name, Option[Name]]()

    // a map from memories to stores to that memory
    val mem_block_stores =
      scala.collection.mutable
        .Map[Name, scala.collection.mutable.Set[EitherStore]]()

    /** Now we need to create a dependence between loads and stores (store
      * depends on load) that operate on the same memory block. For this, we
      * need to start from every load instruction, and trace back throw use-def
      * chains to reach a DefReg with memory type MemoryLogic
      */

    def traceMemoryDecl(base: Name): Option[Name] = {
      mem_block.get(base) match {
        case Some(m) => m // good, we already know the the possible mem block
        case None    => // bad, need to recurse :(
          // get the instruction that produces base
          val producer_inst: Option[Instruction] = def_instructions.get(base)
          producer_inst match {
            case Some(inst) =>
              val uses = regUses(inst)
              // recurse on every use in inst and update the mem_block
              val decls: Seq[Option[Name]] = uses.map { u =>
                val parent_decl = traceMemoryDecl(u)
                mem_block.update(u, parent_decl)
                parent_decl
              }
              // ensure no instruction depends on to mem
              if (decls.count(_.nonEmpty) > 1) {
                logger.error(
                  "instruction traces back to multiple memory blocks!",
                  inst
                )
              }
              // return any findings, so the children could
              decls.find(_.nonEmpty).map(_.get)
            case None =>
              // jackpot, no instruction produces base,
              // now all we need to do is to check declarations
              val decl = mem_decls.find(_.name == base)
              val block = decl.map(_.block)
              mem_block.update(base, block)
              block
          }
      }
    }

    stores.foreach { s =>
      val base_ptrs = s match {
        case Left(LocalStore(_, base, _, _, _))        => Seq(base)
        case Right(GlobalStore(_, (bh, bm, bl), _, _)) => Seq(bh, bm, bl)
      }

      base_ptrs.foreach { b =>
        val m = traceMemoryDecl(b)
        m match {
          case Some(mdef) =>
            val current_stores = mem_block_stores.getOrElse(
              mdef,
              scala.collection.mutable.Set.empty[EitherStore]
            )
            current_stores.add(s)
            mem_block_stores.update(mdef, current_stores)
          case None =>
            val unwrapped: Instruction = s match {
              case Left(i)  => i
              case Right(i) => i
            }
            logger.error(s"store instruction has no memory block!", unwrapped)
        }

      }
    }

    // Load-to-store dependency relation
    val load_store_dependency = scala.collection.mutable
      .Map[Instruction, scala.collection.mutable.Set[Instruction]]()

    /** Create a dependence between load and store instructions through the
      * memory block [[mdef]]
      *
      * @param inst
      *   the load instruction
      * @param mdef
      *   the memory block shared by the load and stores
      */
    def createLoadStoreDependence(
        inst: Instruction,
        block: Name
    ): Unit = {
      require(
        inst.isInstanceOf[LocalLoad] || inst.isInstanceOf[GlobalLoad],
        "can only create load-to-store dependence from loads!"
      )

      // find the corresponding store instructions
      val stores_using_mdef = mem_block_stores.get(block) match {
        case Some(store_set) =>
          if (store_set.isEmpty)
            logger.error(
              s"Internal error! Found empty store set for memory ${block}",
              inst
            )
          load_store_dependency.update(
            inst,
            store_set.collect {
              case Right(x) => x
              case Left(x)  => x
            }
          )
        case None =>
          logger.debug(
            s"Inferred ROM memory ${block}",
            inst
          )(ctx)
      }

    }

    loads.foreach { l =>
      // get all the base pointers
      l match {
        case Left(inst @ LocalLoad(_, base, _, _)) =>
          traceMemoryDecl(base) match {
            case Some(mdef) =>
              createLoadStoreDependence(inst, mdef)
            case None =>
              logger.error("Found no memory for load instruction", inst)
          }

        case Right(inst @ GlobalLoad(_, (bh, bm, bl), _)) =>
          val used_mems: Seq[Name] =
            Seq(bh, bm, bl) map { traceMemoryDecl } collect { case Some(mdef) =>
              mdef
            }
          if (used_mems.isEmpty) {
            logger.error("Found no memory for load instruction", inst)
          } else {
            // make sure a single memory is used!
            if (used_mems.forall(_ != used_mems.head))
              logger.error(
                "Mixed memory access, not all base pointers trace back to the same memory block!",
                inst
              )
            createLoadStoreDependence(inst, used_mems.head)
          }
      }
    }

    /** At this point, we a have mapping from load instructions to depending
      * store instructions, and a mapping from registers to instruction defining
      * them, we can now build the dependence graph
      */

    val raw_dependence_graph =
      process.body.foldLeft(Graph.empty[Instruction, WDiEdge]) {
        case (g, inst) =>
          // first add an edge for register to register dependency
          regUses(inst).foldLeft(g + inst) { case (gg, use) =>
            def_instructions.get(use) match {
              case Some(pred) =>
                gg + WDiEdge(pred, inst)(instructionLatency(pred))
              case None =>
                gg
            }
          }
      }
    // now add the load-to-store dependencies
    val dependence_graph = loads
      .map {
        case Right(x) => x
        case Left(x)  => x
      }
      .foldLeft(raw_dependence_graph) { case (g, l) =>
        load_store_dependency
          .getOrElse(l, scala.collection.mutable.Set.empty[Instruction])
          .foldLeft(g) { case (gg, s) =>
            gg + WDiEdge(l, s)(1)
          }

      }

    if (dependence_graph.isCyclic) {
      logger.error("Could not create acyclic dependence graph!")
    }

    // dependence_graph.
    logger.dumpArtifact(s"dependence_graph_${getName}_${process.id.id}.dot") {

      import scalax.collection.io.dot._
      import scalax.collection.io.dot.implicits._
      // println(g.edges.size)
      val dot_root = DotRootGraph(
        directed = true,
        id = Some("List scheduling dependence graph")
      )
      def edgeTransform(
          iedge: Graph[Instruction, WDiEdge]#EdgeT
      ): Option[(DotGraph, DotEdgeStmt)] = iedge.edge match {
        case WDiEdge(source, target, w) =>
          Some(
            (
              dot_root,
              DotEdgeStmt(
                source.toOuter.hashCode().toString,
                target.toOuter.hashCode().toString,
                List(DotAttr("label", w.toString))
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
          inode: Graph[Instruction, WDiEdge]#NodeT
      ): Option[(DotGraph, DotNodeStmt)] =
        Some(
          (
            dot_root,
            DotNodeStmt(
              NodeId(inode.toOuter.hashCode().toString()),
              List(DotAttr("label", inode.toOuter.serialized))
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
    dependence_graph
  }

  case class PartiallyScheduledProcess(
      proc: DefProcess,
      partial_schedule: List[Instruction],
      earliest: Map[Send, Int]
  )
  private def partiallySchedule(
      proc: DefProcess,
      ctx: AssemblyContext
  ): PartiallyScheduledProcess = {
    type Node = Graph[Instruction, WDiEdge]#NodeT
    type Edge = Graph[Instruction, WDiEdge]#EdgeT
    val dependence_graph = createDependenceGraph(proc, ctx)
    val distance_to_sink = scala.collection.mutable.Map[Node, Double]()

    require(
      dependence_graph.edges.forall(_.isOut),
      "dependence graph is mal-formed!"
    )
    require(dependence_graph.isAcyclic, "Dependence graph is cyclic!")
    require(dependence_graph.nodes.size == proc.body.size)

    def traverseGraphNodesAndRecordDistanceToSink(node: Node): Double = {
      node.outerEdgeTraverser

      if (node.edges.filter(_.from == node).isEmpty) {
        distance_to_sink(node) = 0
        0.0
      } else if (distance_to_sink.contains(node)) {
        // answer already known
        distance_to_sink(node)
      } else {
        node.edges.map { edge =>
          if (edge.from == node) {
            val dist =
              traverseGraphNodesAndRecordDistanceToSink(edge.to) + edge.weight
            distance_to_sink(node) = dist
            dist
          } else { 0.0 }

        }.max
      }
    }

    // find the distance of each node to sinks
    dependence_graph.nodes.foreach { traverseGraphNodesAndRecordDistanceToSink }

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
      regDef(inst) match {
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
        case Some(v) => v.toInt
        case None =>
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
    while(to_schedule.nonEmpty && cycle < 4096) {

      

    }


    source.copy(
      processes = partial_schedules.map { p =>
        p.proc.copy(body = p.partial_schedule ++ p.earliest.keySet.toSeq)
      }.seq
    )
  }

}
