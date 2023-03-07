package manticore.compiler.assembly.levels

/** DeadCodeElimination.scala
  *
  * @author
  *   Sahand Kashani <sahand.kashani@epfl.ch> Mahyar Emami
  *   <mahyar.emami@epfl.ch>
  */

import manticore.compiler.assembly.CanComputeNameDependence
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.OutputType
import manticore.compiler.assembly.ManticoreAssemblyIR

import scalax.collection.Graph
import scalax.collection.edge.LDiEdge
import manticore.compiler.assembly.annotations.Track
import scalax.collection.GraphEdge
import scalax.collection.GraphTraversal
import scala.annotation.tailrec

/** A functional interface for performing DCE on processes. Do not use DCE on
  * split processes that do not have their Send instructions because otherwise
  * useful code gets removed.
  */
trait DeadCodeElimination extends CanComputeNameDependence with CanCollectInputOutputPairs {

  import flavor._

  /** Collect instructions that are considered "output" or "sink", i.e.,
    * instructions that should not be eliminated. These instructions serve as
    * anchor points for performing DCE, i.e., we perform a backward reachability
    * analysis in a data dependence graph to collect all the instructions that
    * contribute to executing the sink ones, those instructions are considered
    * alive an anything else is dead.
    *
    * @param body
    * @param tracked
    *   a function determining whether a value is tracked or not tracked values
    *   should either have the [[Track]] annotations or are output values in a
    *   block of instruction (i.e., values feeding Phi instructions in a
    *   JumpTable)
    * @return
    */
  def collectSinkInstructions(
      body: Iterable[Instruction]
  )(tracked: Name => Boolean)(implicit ctx: AssemblyContext): Set[Instruction] =
    body.collect {
      case i @ (_: GlobalStore | _: LocalStore | _: Send | _: Interrupt | _: PutSerial) =>
        i
      case inst if NameDependence.regDef(inst).exists(tracked) => inst
    }.toSet

  /** Create a map from [[Name]]s to [[Instructions]]s that define them
    *
    * @param body
    *   sequence of instruction to consider
    * @return
    */
  def createDefMap(
      block: Iterable[Instruction]
  )(implicit ctx: AssemblyContext): Map[Name, Instruction] =
    block.flatMap {
      case jtb @ JumpTable(_, phis, blocks, delaySlot, _) =>
        phis.map { case Phi(rd, _) => rd -> jtb } ++
          delaySlot.flatMap { inst =>
            NameDependence.regDef(inst).map { _ -> inst }
          } ++
          blocks.flatMap { case JumpCase(_, blocks) =>
            blocks.flatMap { inst =>
              NameDependence.regDef(inst).map { _ -> inst }
            }
          }
      case inst =>
        NameDependence.regDef(inst).map { _ -> inst }
    }.toMap

  /** Create a data dependence graph between the instructions in the given block
    * of instructions
    *
    * @param block
    *   a block of instruction, e.g., process body or the body of each case in a
    *   JumpTable
    * @return
    */
  def createDependenceGraph(
      block: Iterable[Instruction],
      defInst: Map[Name, Instruction],
      inputOutputPairs: Map[Name, Name] // a map from input regs to output regs
  )(implicit ctx: AssemblyContext): Graph[Instruction, GraphEdge.DiEdge] = {
    val graph =
      scalax.collection.mutable.Graph.empty[Instruction, GraphEdge.DiEdge]
    val defInst = createDefMap(block)

    block.foreach { inst =>
      graph += inst
      NameDependence.regUses(inst).foreach { use =>
        defInst.get(use) match {
          case Some(producer) =>
            graph += GraphEdge.DiEdge(producer, inst)
          case None => // do nothing, no instruction defines the used Name
          // so the Name is either a constant, or is produced by an instruction
          // not included the given block of instruction
        }
      }
    }

    graph
  }

  /** Create dependence graph dot serialization (for debug dumps)
    *
    * @param graph
    * @param ctx
    * @return
    */
  def createDotDependenceGraph(
      graph: Graph[Instruction, GraphEdge.DiEdge]
  )(implicit ctx: AssemblyContext): String = {
    import scalax.collection.io.dot._
    import scalax.collection.io.dot.implicits._

    def escape(s: String): String = s.replaceAll("\"", "\\\\\"")
    def quote(s: String): String  = s"\"${s}\""

    val dotRoot = DotRootGraph(
      directed = true,
      id = Some("List scheduling dependence graph")
    )
    val nodeIndex = graph.nodes.map(_.toOuter).zipWithIndex.toMap
    def edgeTransform(
        iedge: Graph[Instruction, GraphEdge.DiEdge]#EdgeT
    ): Option[(DotGraph, DotEdgeStmt)] = iedge.edge match {
      case GraphEdge.DiEdge(source, target) =>
        Some(
          (
            dotRoot,
            DotEdgeStmt(
              nodeIndex(source.toOuter).toString,
              nodeIndex(target.toOuter).toString
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
        inode: Graph[Instruction, GraphEdge.DiEdge]#NodeT
    ): Option[(DotGraph, DotNodeStmt)] =
      Some(
        (
          dotRoot,
          DotNodeStmt(
            NodeId(nodeIndex(inode.toOuter)),
            List(DotAttr("label", quote(escape(inode.toOuter.toString.trim.take(64)))))
          )
        )
      )

    val dotExport: String = graph.toDot(
      dotRoot = dotRoot,
      edgeTransformer = edgeTransform,
      cNodeTransformer = Some(nodeTransformer), // connected nodes
      iNodeTransformer = Some(nodeTransformer)  // isolated nodes
    )
    dotExport
  }

  /** Collect the live instructions given some other live "sink" instructions
    * This function performs a backward BFS (could be any graph search) to
    * determine a set of instructions that are backward reachable from the set
    * of known live instructions.
    *
    * @param sinks
    *   set of already known live instructions
    * @param graph
    *   data dependence graph (maybe cyclic)
    * @param ctx
    *   assembly context
    * @return
    */
  def collectAlive(
      sinks: Set[Instruction],
      graph: Graph[Instruction, GraphEdge.DiEdge]
  )(implicit ctx: AssemblyContext): scala.collection.Set[Instruction] = {

    ctx.logger.info("Collecting live nodes")
    // ctx.logger.info(s"graph size: ${graph.nodes.size}")
    val alive = scala.collection.mutable.Set.empty[Instruction]
    val toVisit =
      scala.collection.mutable.Queue.empty[graph.NodeT] ++ sinks.map {
        graph.get(_)
      }

    ctx.logger.info(s"starting search")
    // perform a backward bfs
    var cnt = 0
    while (toVisit.nonEmpty) {
      val current = toVisit.dequeue()
      if (!alive(current)) {
        cnt += 1
        alive += current
        val newAlive = current.diPredecessors
        val newVisit = newAlive.filter(pred => !alive(pred.toOuter))
        toVisit ++= newVisit // any alive node has been pushed to toVisit, so if some predecessors
        // have already been visited, skip them (in a cyclic graph not skipping
        // them leads to a forever loop!)
      }
    }
    // ctx.stats.record("number of iterations to collect live nodes" -> cnt)
    // ctx.logger.info(s"number of iterations to search the graph: ${cnt}")
    alive
  }

  /** Count the instructions in a way that can be used to determine whether DCE
    * did anything. I.e., if in one iteration of the DCE to the next, the number
    * of instructions computed by this function is reduced, then DCE did
    * something useful
    *
    * @param block
    * @param ctx
    * @return
    */
  def countInstructions(
      block: Iterable[Instruction]
  )(implicit ctx: AssemblyContext): Int = {
    block.foldLeft(0) { case (cnt, inst) =>
      inst match {
        case JumpTable(_, phis, cblocks, dslot, _) =>
          // although Phis are not real instructions, we count them in DCE because
          // if they get removed, other DCE opportunities may be revealed
          cnt + dslot.length + cblocks.map { case JumpCase(_, blk) =>
            blk.length
          }.sum + phis.length + 1
        case ParMux(_, choices, default, _) =>
          cnt + choices.length // we can simply do cnt + 1 too, that won't change anything for the DCE
        case _ => cnt + 1
      }
    }
  }
  def doDce(process: DefProcess)(implicit ctx: AssemblyContext): DefProcess = {

    // We create a set of instructions that produce something useful.
    // Any assertion, store to memory, send to other processes, or an instruction
    // that computes a tracked value is deemed useful.
    def hasTrackAnnotation(r: DefReg): Boolean = r.annons.exists {
      case _: Track => true
      case _        => false
    }

    /** a set of registers with [[Track]] annotation */
    val globallyTrackedSet = process.registers.collect {
      case r if hasTrackAnnotation(r) => r.variable.name
    }.toSet

    case class IterationIndex(index: Int) {
      def next() = IterationIndex(index + 1)
    }

    val inputOutputPairs = InputOutputPairs
      .createInputOutputPairs(process)
      .map { case (curr, next) =>
        curr.variable.name -> next.variable.name
      }
      .toMap

    /** It may be counterintuitive, but DCE needs a closed loop, i.e., a program
      * where instructions like MOV curr, next are present. Without them, the
      * rest of the DCE algorithm breaks. The reason is that to consider some
      * code to be "alive" it has to lead to either a syscall like EXPECT or
      * lead to a SEND, or memory stores. If non of that is true then it should
      * at least lead to the definition of a value that is marked as [[Track]]
      * (see [[collectSinkInstructions]] for a more accurate description).
      *
      * When we use an [[EXPECT]] instruction to decide whether some other
      * instruction is live, we need to see whether that instruction leads to
      * the computation of [[EXPECT]] operands. This can only be seen if the
      * sequential cycles are closed. However, since this would make our
      * dependence graph cyclic, we should pay attention in tracing back live
      * instructions and avoid looping/recurring forever.
      */
    val closedBlock = process.body ++ inputOutputPairs.map { case (curr, next) =>
      Mov(curr, next)
    }
    val defInst = createDefMap(closedBlock)

    // With the inclusion of JumpTables, a single pass on the data dependence
    // graph would not rid us of all the dead code. If we first remove the dead
    // from the program body, without peeking inside JumpTables, then we may
    // keep some instruction that provide values for dead instructions inside
    // the JumpTables. If we do it the opposite way, i.e., perform DCE inside
    // the JumpTables and then do DCE outside. We may not removing some instructions
    // inside the JumpTable compute the values for a Phi instruction that is
    // removed after we perform DCE outside. This means we basically have to
    // perform DCE until we reach a fixed point.
    case class DceResult(block: Iterable[Instruction], score: Int)
    def dceOnce(
        current: Iterable[Instruction],
        index: IterationIndex
    ): DceResult = {

      /** perform DCE on a block of instructions while treating some names as
        * non-optimizable given by the [[tracked]] argument
        *
        * @param block
        * @param tracked
        * @return
        */
      def dceBlock(
          block: Iterable[Instruction]
      )(tracked: Name => Boolean): Iterable[Instruction] = {
        // find the sink instructions
        val sinks    = collectSinkInstructions(block)(tracked)
        val depGraph = createDependenceGraph(block, defInst, inputOutputPairs)
        // we need to handle EXPECTs rather carefully, normally, and EXPECT is
        // singular node in the dependence graph because it takes
        // ctx.logger.dumpArtifact(
        //   s"dce_${ctx.logger.countProgress()}_iter_${index.index}_pre.dot"
        // ) {
        //   createDotDependenceGraph(depGraph)
        // }
        val alive = collectAlive(sinks, depGraph)

        // we now have the block with dead codes eliminated, but since we handle
        // a JumpTable as a single instruction, the Phis nodes are consolidated into
        // one from the perspective of the dependence graph, therefore, although
        // a JumpTable may be alive, some of its Phi nodes may not be. We can
        // modify these JumpTables by looking at their Phi nodes, and checking
        // where there exists at least one instruction in the [[alive]] set that
        // uses the value produced by that phi and remove it otherwise
        val aliveBlock = block.filter { alive.contains(_) }
        // ctx.logger.dumpArtifact(
        //   s"dce_${ctx.logger.countProgress()}_iter_${index.index}_post.dot"
        // ) {
        //   createDotDependenceGraph(
        //     createDependenceGraph(aliveBlock, defInst, inputOutputPairs)
        //   )
        // }
        aliveBlock.map {
          case jtb @ JumpTable(_, results, _, _, _) =>
            val filteredPhis = results.filter { case Phi(rd, _) =>
              globallyTrackedSet(rd) || depGraph.get(jtb).diSuccessors.exists { succ =>
                NameDependence.regUses(succ).contains(rd)
              }
            }
            // note that by construction we should have at least one Phi that
            // its result is used, otherwise, jtb should not have been in the
            // alive block
            assert(filteredPhis.length > 0)
            jtb.copy(results = filteredPhis).setPos(jtb.pos)
          case inst: Instruction => inst // other instruction do not need
          // extra care
        }
      }
      ctx.logger.info(s"DCE on outer block ${index}")
      val outerDceResult =
        dceBlock(current) {
          globallyTrackedSet
          // only keep Names with Track annons
        }

      ctx.logger.info(s"DCE on inner block ${index} (if any)")
      val finalDceResult = outerDceResult.map {
        // perform DCE for the body of the jump
        case jtb @ JumpTable(target, results, caseBlocks, dslot, annons) =>
          // we need to ensure DCE won't remove instruction that lead to Phi
          // nodes, so we augment the tracking set with the operands of Phi nodes

          def scopedTrack(name: Name): Boolean = {
            // probably no need to convert to Set since the Seq should be
            // small enough such that _.contains does not explode on us
            val trackSet = results.flatMap { case Phi(_, rss) =>
              rss.map(_._2)
            }
            trackSet.contains(name) || globallyTrackedSet.contains(name)
          }
          // the cast is a bit of cheat, since the current Instruction AST
          // requires DataInstruction in the body of jump cases (no nested
          // jump tables), we know for sure the casting will succeed because
          // dceBlock will not transform any instruction to another kind
          // so since blk is all DataInstruction, then the result would
          // dynamically be the same
          def dceDataBlock(blk: Iterable[Instruction]): Iterable[Instruction] =
            dceBlock(blk) { scopedTrack }
          val reducedBlocks = caseBlocks.map { case JumpCase(lbl, blk) =>
            JumpCase(
              lbl,
              dceDataBlock(blk).toSeq
            )
          }
          val reducedDelaySlot = dceDataBlock(dslot)
          jtb
            .copy(
              blocks = reducedBlocks,
              dslot = reducedDelaySlot.toSeq
            )
            .setPos(jtb.pos)
        case inst => inst
      }

      DceResult(finalDceResult, countInstructions(finalDceResult))
    }

    val maxIter = 20

    @tailrec
    def fixedPointDoWhile(
        iter: IterationIndex,
        last: DceResult
    ): Iterable[Instruction] = {
      if (iter.index >= maxIter) {
        last.block
      } else {
        ctx.logger.dumpArtifact(
          s"dce_${ctx.logger.countProgress()}_iter_${iter.index}_pre.dot"
        ) {
          createDotDependenceGraph(
            createDependenceGraph(last.block, defInst, inputOutputPairs)
          )
        }
        val newRes = dceOnce(last.block, iter)
        ctx.logger.dumpArtifact(
          s"dce_${ctx.logger.countProgress()}_iter_${iter.index}_post.dot"
        ) {
          createDotDependenceGraph(
            createDependenceGraph(newRes.block, defInst, inputOutputPairs)
          )
        }
        if (newRes.score < last.score) {
          fixedPointDoWhile(iter.next(), newRes)
        } else {
          newRes.block
        }
      }
    }

    // we first close sequential cycles

    val optBlock = fixedPointDoWhile(
      IterationIndex(0),
      DceResult(closedBlock, countInstructions(closedBlock))
    )

    // now we need to go through the optimized block and create a set of names
    // we are ought to keep, basically removing unused DefRegs

    val namesToKeep = NameDependence.referencedNames(optBlock)

    ctx.logger.dumpArtifact(
      s"dce_${ctx.logger.countProgress()}_kept_names.txt"
    ) {
      namesToKeep.mkString("\n")
    }
    val optRegs = process.registers.filter { r =>
      namesToKeep.contains(r.variable.name)
    }

    val optLabels = process.labels.filter { lgrp =>
      namesToKeep.contains(lgrp.memory)
    }

    def isOutputMov(mov: Mov) = {
      inputOutputPairs.get(mov.rd) match {
        case Some(rs) => rs == mov.rs
        case None     => false
      }
    }
    val withOutIOMovs = optBlock.filter {
      case mov: Mov => !isOutputMov(mov)
      case _        => true
    }

    val dceProc = process
      .copy(
        body = withOutIOMovs.toSeq,
        registers = optRegs,
        labels = optLabels
      )
      .setPos(process.pos)

    dceProc
  }

}
