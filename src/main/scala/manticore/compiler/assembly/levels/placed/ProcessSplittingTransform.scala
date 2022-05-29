package manticore.compiler.assembly.levels.placed

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.DependenceGraphBuilder
import manticore.compiler.assembly.annotations.Memblock
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.ConstType
import manticore.compiler.assembly.levels.InputType
import manticore.compiler.assembly.levels.OutputType
import manticore.compiler.assembly.levels.VariableType
import manticore.compiler.assembly.levels.placed.Helpers.GraphBuilder
import manticore.compiler.assembly.levels.placed.Helpers.NameDependence
import manticore.compiler.assembly.levels.placed.Helpers.InstructionOrder
import manticore.compiler.assembly.levels.placed.Helpers.InputOutputPairs
import scalax.collection.Graph
import scalax.collection.GraphEdge
import scalax.collection.GraphTraversal
import scalax.collection.edge.LDiEdge
import scalax.collection.mutable.{Graph => MutableGraph}

import java.lang.management.MemoryType
import scala.annotation.tailrec
import scala.collection.immutable
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import scala.collection.BitSet

/** A pass to parallelize processes while respecting resource dependence
  * constraints
  *
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */
object ProcessSplittingTransform extends PlacedIRTransformer {

  val flavor = PlacedIR
  import flavor._

  private case class ProcessorDescriptor(
      inSet: BitSet,
      outSet: BitSet,
      body: BitSet,
      memory: Int
  ) {

    def merged(other: ProcessorDescriptor): ProcessorDescriptor = {
      val newOutSet = other.outSet union outSet
      val newInSet = (inSet union other.inSet) diff newOutSet
      val newBody = body union other.body
      ProcessorDescriptor(newInSet, newOutSet, newBody, memory + other.memory)
    }
  }
  private object ProcessorDescriptor {
    def empty = ProcessorDescriptor(BitSet.empty, BitSet.empty, BitSet.empty, 0)

  }
  trait ParallelizationContext {
    def instructionIndex(inst: Instruction): Int
    def inputIndex(r: Name): Int
    def outputIndex(r: Name): Int
    def getInstruction(idx: Int): Instruction
    def getInput(idx: Int): Name
    def getOutput(idx: Int): Name
    def isInput(r: Name): Boolean
    def isOutput(r: Name): Boolean
    def isMemory(r: Name): Boolean
    def memorySize(idx: Int): Int
    def memoryIndex(r: Name): Int
    def numStateRegs(): Int
  }

  private def createParallelizationContext(
      process: DefProcess
  )(implicit ctx: AssemblyContext): ParallelizationContext = {

    val statePairs =
      InputOutputPairs.createInputOutputPairs(process).toArray

    val outputIndices = scala.collection.mutable.Map.empty[Name, Int]
    val inputIndices = scala.collection.mutable.Map.empty[Name, Int]
    val instrIndices = scala.collection.mutable.Map.empty[Instruction, Int]
    var idx = 0
    statePairs.foreach { case (current, next) =>
      inputIndices += (current.variable.name -> idx)
      outputIndices += (next.variable.name -> idx)
      idx += 1
    }
    idx = 0
    process.body.foreach { instr =>
      instrIndices += (instr -> idx)
      idx += 1
    }
    val body = process.body.toArray
    val memories = process.registers.collect {
      case DefReg(MemoryVariable(n, sz, _, _), _, _) => (n, sz)
    }.toArray
    idx = 0
    val memoryIndices = scala.collection.mutable.Map.empty[Name, Int]
    memories.foreach { case (name, sz) =>
      memoryIndices += (name -> idx)
      idx += 1
    }
    new ParallelizationContext {
      def numStateRegs(): Int = statePairs.size

      def instructionIndex(inst: Instruction): Int = instrIndices(inst)
      def inputIndex(r: Name): Int = inputIndices(r)
      def outputIndex(r: Name): Int = outputIndices(r)
      def getInstruction(idx: Int): Instruction = body(idx)
      def getInput(idx: Int): Name = statePairs(idx)._1.variable.name
      def getOutput(idx: Int): Name = statePairs(idx)._2.variable.name
      def isInput(r: Name): Boolean = inputIndices.contains(r)
      def isOutput(r: Name): Boolean = outputIndices.contains(r)

      def isMemory(r: Name): Boolean = memoryIndices.contains(r)
      def memoryIndex(r: Name): Int = memoryIndices(r)
      def memorySize(idx: Int): Int = memories(idx)._2
    }
  }
  private def extractIndependentInstructionSequences(
      proc: DefProcess,
      bitSetIndexer: ParallelizationContext
  )(implicit ctx: AssemblyContext) = {

    val dependence_graph = GraphBuilder.rawGraph(proc.body)

    // find the output registers, these are registers that are written in
    // sink instructions, there could be instructions that are sinks in the
    // dependence graph but are not really relevant because they don't write
    // to outputs

    val sink_nodes = dependence_graph.nodes.filter { node =>
      val writes_to_output =
        NameDependence.regDef(node.toOuter).exists(bitSetIndexer.isOutput)
      val is_store = node.toOuter match {
        case _: LocalStore | _: GlobalStore          => true
        case _: Expect | _: Interrupt | _: PutSerial => true
        case _                                       => false
      }
      writes_to_output | is_store
    }

    // We want to find parallel processes from by performing a backward
    // traversal starting from sink nodes in the instruction dependence graph we
    // have instantiated. But in doing so we must respect two constraints:
    // 1. If process p1 and p2 have EXPECT/GlobalLoad/GlobalStore instructions then p1 = p2
    // 2. If process p1 and p2 both access memory block b, then p1 = p2
    //
    // To do so, we build another graph, call it a constraint_graph to encode
    // the which instructions are supposed to be grouped together. This graph is
    // a 3 level high hierarchy: At the first level, we have a vertex that
    // indicates the kind of constraint (i.e., memory or system call). At the
    // second level there are processes "roots" that essentially wrapped output
    // instructions (writing to output regs or storing in memory or system
    // call). And the last level of hierarchy are all the other instructions
    // (including output ones). We can construct this graph by perform multiple
    // backwards traversals starting from the sink nodes. The complexity would
    // be O(#out * (V + E)) with #out begin number of sink instructions and (V +
    // E) being the worst case complexity of a backward traversal (e.g., BFS).
    //
    //              +----------------+ +-----------------+ +--------+ +-----------+
    //              |                | |                 | |        | |           |
    //              |MemBlockRoot(b0)| | MemBlockRoot(b1)| |FreeRoot| |SysCallRoot|
    //              |                | |                 | |        | |           |
    //      +-------+--+-------+-----+ +-------+---------+ +--------+ +-----------+
    //      |          |       |               |           |        |             |
    //      |          |       | +-------------+           |        |             |
    //      |          |       | |                         |        |             |
    // +----v---+  +---v---+ +-v-v--+  +--------+    +-----v-+  +---v----+    +---v-----+
    // |        |  |       | |      |  |        |    |       |  |        |    |         |
    // |ProcRoot|  |       | |      |  |        |    |       |  |        |    |         |
    // +----+---+  +-------+ +------+  +--------+    +-------+  +--------+    +---------+
    //      |
    //      |
    //    +-v----+
    //    | leaf |
    //    +------+
    //
    // After constructing this graph, we mask the edges in the last level (edge
    // going to leaves) and look for weakly connected components, the set of ProcRoots
    // at in every weakly connected sub graph can help us get the instructions
    // that have to be packed together. We simply create a set of instructions
    // by iterating the set of ProcRoots in a weakly connected subgraph and
    // that will be the instructions we should pack in one process.
    //
    //

    // Helper classes for building the constraint graph

    sealed abstract class SubProcess
    case class MemBlockRoot(mem: Name) extends SubProcess {
      override def toString(): String = mem

      override def equals(x: Any) = x match {
        case mx: MemBlockRoot => (mx eq this) || (mem == mx.mem)
        case _                => false
      }
      override def hashCode() = mem.hashCode()

    }
    case object SysCallRoot extends SubProcess {
      override def toString(): String = "syscall"
    }
    case class ProcRoot(sink: Instruction) extends SubProcess {
      override def toString(): String = s"root of ${sink}"
      override def equals(x: Any) = x match {
        case px: ProcRoot => (px eq this) || (px.sink == sink)
        case _            => false
      }
      override def hashCode() = sink.hashCode()
    }
    case class InstLeaf(inst: Instruction) extends SubProcess {
      override def toString(): String = inst.toString()
      override def equals(x: Any) = x match {
        case xl: InstLeaf => (xl eq this) || (xl.inst == inst)
        case _            => false
      }
      override def hashCode() = inst.hashCode()
    }

    // the constraint graph, the graph does not need to be directed... but it is
    val constraint_graph =
      scalax.collection.mutable.Graph.empty[SubProcess, GraphEdge.DiEdge]

    // backward traversal from sink nodes that partially creates the constraint
    // graph
    def createConstraints(output_node: dependence_graph.NodeT): ProcRoot = {
      val local_root = ProcRoot(output_node)
      constraint_graph += local_root
      output_node.outerNodeTraverser
        .withDirection(GraphTraversal.Predecessors)
        .foreach { onode =>
          val inst = InstLeaf(onode)
          constraint_graph += GraphEdge.DiEdge(local_root, inst)
          onode match {
            case _: Expect | _: GlobalStore | _: GlobalLoad | _: Interrupt |
                _: PutSerial => // create and edge from SysCallRoot to ProcRoot
              constraint_graph += GraphEdge.DiEdge(
                SysCallRoot,
                local_root
              )

            case LocalStore(_, _, _, _, order, _) =>
              constraint_graph += GraphEdge.DiEdge(
                MemBlockRoot(order.memory),
                local_root
              )
            case LocalLoad(_, _, _, order, _) =>
              constraint_graph += GraphEdge.DiEdge(
                MemBlockRoot(order.memory),
                local_root
              )
            case _ =>
            // do nothing for now
          }
        }

      local_root
    }

    // run a function and time it
    def timed[T](header: String)(fn_body: => T): T = {
      ctx.logger.info(header)

      val (res, elapsed) = ctx.stats.timed(fn_body)
      ctx.stats.recordRunTime(header, elapsed)
      ctx.logger.info(
        f"took ${elapsed * 1e-3}%.3f seconds"
      )
      ctx.logger.flush()
      res
    }

    // do the backward traversal from sink nodes to build the constraint graph

    val proct_roots = timed("Extracting parallel processes") {
      val res = sink_nodes.map { createConstraints }
      ctx.logger.dumpArtifact(
        s"constraint_graph${ctx.logger.countProgress()}_${transformId}_${proc.id}.dot"
      ) {

        import scalax.collection.io.dot._
        import scalax.collection.io.dot.implicits._

        val dot_root = DotRootGraph(
          directed = true,
          id = Some("Resource dependence graph")
        )

        def nodeTransformer(
            inode: Graph[SubProcess, GraphEdge.DiEdge]#NodeT
        ): Option[(DotGraph, DotNodeStmt)] =
          Some(
            (
              dot_root,
              DotNodeStmt(
                NodeId(inode.toOuter.hashCode().toString()),
                List(DotAttr("label", inode.toOuter.toString.trim))
              )
            )
          )
        val dot_export: String = constraint_graph.toDot(
          dotRoot = dot_root,
          edgeTransformer = iedge =>
            Some(
              (
                dot_root,
                DotEdgeStmt(
                  iedge.edge.source.toOuter.hashCode().toString(),
                  iedge.edge.target.toOuter.hashCode().toString()
                )
              )
            ),
          iNodeTransformer = Some(nodeTransformer),
          cNodeTransformer = Some(nodeTransformer)
        )
        dot_export
      }

      // val leaves = constraint_graph.nodes.filter { inode =>
      //   inode.toOuter match {
      //     case _: InstLeaf => true
      //     case _           => false
      //   }
      // }

      val leaves = constraint_graph.nodes.collect {
        case n if n.toOuter.isInstanceOf[InstLeaf] =>
          n.toOuter.asInstanceOf[InstLeaf].inst
      }
      if (leaves.size != proc.body.size) {
        val left_out = proc.body.toSet[Instruction].diff(leaves).toSeq

        left_out.foreach { i =>
          ctx.logger.warn("removing unused instruction", i)
        }
      }
      // assert(leaves.size == proc.body.size, "no instruction should be left out")
      ctx.logger.info(
        s"Found ${res.size} parallel processes from ${proc.body.size} instruction "
      )
      res
    }

    // proct_roots.foreach { p =>
    //   ctx.logger.info(s"${p.sink} -> ${constraint_graph.get(p).outDegree}")
    // }
    // find weakly connected subgraphs  (masking last level edges and nodes)

    val compatible_processes = timed("Finding compatible processes") {
      val res = constraint_graph
        .componentTraverser(
          subgraphEdges = iedge =>
            iedge.toOuter match {
              case GraphEdge.DiEdge(u: ProcRoot, v: InstLeaf) => false
              case _                                          => true
            },
          subgraphNodes = inode =>
            inode.toOuter match {
              case (_: ProcRoot | _: MemBlockRoot | SysCallRoot) => true
              case _                                             => false
            }
        )
      ctx.logger.info(s"Found ${res.size} compatible processes")
      res
    }

    // combine the instructions in the weakly connected subgraphs
    val process_bodies = timed("Merging incompatible processes") {
      compatible_processes
        .map { connected_components =>
          val mem_nodes = connected_components.nodes.filter(inode =>
            inode.toOuter match {
              case _: MemBlockRoot => true
              case _               => false
            }
          )
          val has_syscall = connected_components.nodes.exists { inode =>
            inode.toOuter == SysCallRoot
          }
          val proc_root_nodes = connected_components.nodes.filter(inode =>
            inode.toOuter match {
              case _: ProcRoot => true
              case _           => false
            }
          )

          val leaf_instructions =
            scala.collection.mutable.Set.empty[Instruction]
          proc_root_nodes.foreach { inode =>
            // println(s"${inode.diSuccessors.size}")
            leaf_instructions ++= inode.diSuccessors.map { ileaf =>
              ileaf.toOuter.asInstanceOf[InstLeaf].inst
            }
          }
          leaf_instructions
        }
    }

    def createProcessDescriptor(
        block: Iterable[Instruction]
    ): ProcessorDescriptor = {

      val bodyBitSet = scala.collection.mutable.BitSet.empty
      val inBitSet = scala.collection.mutable.BitSet.empty
      val outBitSet = scala.collection.mutable.BitSet.empty
      val memBitSet = scala.collection.mutable.BitSet.empty
      val outSet = block.foreach { instr =>
        bodyBitSet += (bitSetIndexer.instructionIndex(instr))
        for (use <- NameDependence.regUses(instr)) {
          if (bitSetIndexer.isInput(use)) {
            inBitSet += bitSetIndexer.inputIndex(use)
          }
          if (bitSetIndexer.isMemory(use)) {
            val memIx = bitSetIndexer.memoryIndex(use)
            memBitSet += memIx
          }
        }
        for (rd <- NameDependence.regDef(instr)) {
          if (bitSetIndexer.isOutput(rd)) {
            outBitSet += bitSetIndexer.outputIndex(rd)
          }
        }
      }
      inBitSet --= outBitSet
      new ProcessorDescriptor(
        inSet = inBitSet,
        outSet = outBitSet,
        body = bodyBitSet,
        memory = memBitSet.foldLeft(0) { case (sz, idx) =>
          bitSetIndexer.memorySize(idx)
        }
      )

    }
    // create new processes
    val result = timed("constructing merged processes") {
      process_bodies.map(createProcessDescriptor)
    }

    result
  }
  override def transform(
      program: DefProgram
  )(implicit ctx: AssemblyContext): DefProgram = {

    if (program.processes.length != 1) {
      ctx.logger.fail("Did not expect to have multiple processes!")

    }
    if (program.processes.head.functions.nonEmpty) {
      ctx.logger.fail("Can not handle Custom instructions yet!")
    }
    val bitSetIndexer = createParallelizationContext(program.processes.head)
    val splitted =
      extractIndependentInstructionSequences(
        program.processes.head,
        bitSetIndexer
      )

    // splitted contains perhaps many processes. We should try to merge them into
    // at most ctx.max_dimx * ctx.max_dimy processes while ensuring no process
    // uses memory than available and also try to minimize the workspan.
    val mergedProcesses: Array[ProcessorDescriptor] =
      Array.fill(ctx.max_dimx * ctx.max_dimy) {
        ProcessorDescriptor.empty
      }

    def estimateCost(p: ProcessorDescriptor): Int =
      p.body.foldLeft(0) { case (cost, idx) =>
        bitSetIndexer.getInstruction(idx) match {
          case JumpTable(_, _, blocks, dslot, _) =>
            cost + blocks.map(_.block.length).max + dslot.length
          case _ => cost + 1
        }
      }
    def canMerge(p1: ProcessorDescriptor, p2: ProcessorDescriptor): Boolean =
      (p1.memory + p2.memory) <= ctx.max_local_memory

    val toMerge =
      scala.collection.mutable.Queue.empty[ProcessorDescriptor] ++ splitted

    sealed trait MergeResult
    case class MergeChoice(result: ProcessorDescriptor, index: Int, cost: Int)
        extends MergeResult
    case object NoMerge extends MergeResult
    while (toMerge.nonEmpty) {
      val head = toMerge.dequeue()
      val (_, best) = mergedProcesses.foldLeft[(Int, MergeResult)](0, NoMerge) {
        case ((ix, prevBest), currentChoice) =>
          if (canMerge(currentChoice, head)) {
            val possibleMerge = head merged currentChoice
            val possibleCost = estimateCost(possibleMerge)
            val nextBest = prevBest match {
              case NoMerge => MergeChoice(possibleMerge, ix, possibleCost)
              case other: MergeChoice if (other.cost > possibleCost) =>
                MergeChoice(possibleMerge, ix, possibleCost)
              case other => other
            }
            (ix + 1, nextBest)
          } else {
            (ix + 1, prevBest)
          }
      }
      best match {
        case MergeChoice(result, index, _) => mergedProcesses(index) = result
        case NoMerge =>
          ctx.logger.error(
            "Could not merge processes because ran out of memory!"
          )
      }
    }

    // val stateOwner =
    val stateUsers = Array.fill(bitSetIndexer.numStateRegs()) { BitSet.empty }

    mergedProcesses.zipWithIndex.foreach {
      case (ProcessorDescriptor(inSet, _, _, _), ix) =>
        inSet.foreach { stateIndex =>
          stateUsers(stateIndex) = stateUsers(stateIndex) | BitSet(ix)
        }
    }
    def processId(index: Int) = ProcessIdImpl(s"p${index}", -1, -1)
    val finalProcesses = mergedProcesses.zipWithIndex.map {
      case (ProcessorDescriptor(_, outSet, bitSetBody, _), procIndex) =>
        // note that since we use the original program order to index the bits,
        // and that the underlying bitSetBody is an ordered set doing .toSeq.map
        // will give back the instruction indices in the original order so no need
        // to reorder the body
        val body = bitSetBody.toSeq.map { ix =>
          bitSetIndexer.getInstruction(ix)
        }
        val sends = outSet.toSeq.flatMap { nextStateIndex =>
          stateUsers(nextStateIndex).toSeq.collect {
            case recipient if recipient != procIndex =>
              val currentStateInDest = bitSetIndexer.getInput(nextStateIndex)
              val nextStateInHere = bitSetIndexer.getOutput(nextStateIndex)
              Send(currentStateInDest, nextStateInHere, processId(recipient))
          }
        }
        val bodyWithSends = body ++ sends
        val referenced = NameDependence.referencedNames(bodyWithSends)
        val usedRegs = program.processes.head.registers.filter { r =>
          referenced(r.variable.name)
        }
        val usedLabelGrps = program.processes.head.labels.filter { lblgrp =>
          referenced(lblgrp.memory)
        }

        val procId = processId(procIndex)

        program.processes.head
          .copy(
            id = procId,
            registers = usedRegs,
            labels = usedLabelGrps,
            body = bodyWithSends
          )
          .setPos(program.processes.head.pos)
    }

    program.copy(finalProcesses.toSeq.filter(_.body.nonEmpty))

  }
}
