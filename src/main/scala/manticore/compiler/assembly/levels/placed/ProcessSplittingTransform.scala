package manticore.compiler.assembly.levels.placed

import manticore.compiler.AssemblyContext

import manticore.compiler.assembly.levels.AssemblyTransformer

import manticore.compiler.assembly.levels.OutputType
import manticore.compiler.assembly.levels.MemoryType
import manticore.compiler.assembly.levels.placed.Helpers.GraphBuilder
import manticore.compiler.assembly.levels.placed.Helpers.NameDependence
import manticore.compiler.assembly.levels.placed.Helpers.InstructionOrder
import manticore.compiler.assembly.levels.placed.Helpers.InputOutputPairs
import manticore.compiler.assembly.levels.placed.Helpers.ProgramStatistics
import scalax.collection.Graph
import scalax.collection.GraphEdge
import scalax.collection.GraphTraversal
import scalax.collection.edge.LDiEdge
import scalax.collection.mutable.{Graph => MutableGraph}

import scala.annotation.tailrec
import scala.collection.immutable
import scala.collection.BitSet
import manticore.compiler.assembly.annotations.Loc
import manticore.compiler.assembly.levels.InputType
import manticore.compiler.assembly.annotations.Reg

object DisjointSets {
  def apply[T](elements: Iterable[T]) = {
    import scala.collection.mutable.{Map => MutableMap, Set => MutableSet}
    val allSets: MutableMap[T, MutableSet[T]] =
      MutableMap.empty[T, MutableSet[T]]
    allSets ++= elements.map { x => x -> MutableSet[T](x) }
    new DisjointSets[T] {
      override def union(a: T, b: T): scala.collection.Set[T] = {
        val setA = allSets(a)
        val setB = allSets(b)
        if (setA(b) || setB(a)) {
          // already joined
          setA
        } else if (setA.size > setB.size) {
          setA ++= setB
          allSets ++= setB.map { _ -> setA }
          setA
        } else {
          setB ++= setA
          allSets ++= setA.map { _ -> setB }
          setB
        }
      }
      override def find(a: T): scala.collection.Set[T] = allSets(a)

      override def sets: Iterable[scala.collection.Set[T]] = allSets.values.toSet

      override def add(a: T): scala.collection.Set[T] = {
        if (!allSets.contains(a)) {
          allSets += (a -> MutableSet[T](a))
        }
        allSets(a)
      }
    }
  }

}
sealed trait DisjointSets[T] {
  def union(first: T, second: T): scala.collection.Set[T]
  def find(a: T): scala.collection.Set[T]
  def sets: Iterable[scala.collection.Set[T]]
  def add(a: T): scala.collection.Set[T]
}

/** A pass to parallelize processes while respecting resource dependence
  * constraints
  *
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */
object ProcessSplittingTransform extends PlacedIRTransformer {

  val flavor = PlacedIR
  import flavor._

  override def transform(program: DefProgram)(implicit ctx: AssemblyContext): DefProgram = {
    if (ctx.use_loc) {
      val allHaveLoc = program.processes.forall { p =>
        p.annons.exists {
          case _: Loc => true
          case _      => false
        }
      }
      if (!allHaveLoc) {
        ctx.logger.error("not all processes have @LOC annotation!")
        program
      } else {
        ctx.logger.info("Skipping process splitting because @LOC annotations are found")
        createSends(program)
      }

    } else {
      doSplit(program)
    }

  }

  private def createSends(program: DefProgram)(implicit ctx: AssemblyContext): DefProgram = {

    import scala.collection.mutable.ArrayBuffer
    case class StateId(id: String, index: Int)
    val stateUsers =
      scala.collection.mutable.Map
        .empty[StateId, ArrayBuffer[DefProcess]]

    val currentState = scala.collection.mutable.Map.empty[StateId, Name]

    def getStateId(r: DefReg) = r.annons.collectFirst { case x: Reg =>
      StateId(x.getId(), x.getIndex().getOrElse(0))
    }

    program.processes.foreach { p =>
      val currents = p.registers.collect { case r if r.variable.varType == InputType => r }
      currents.foreach { curr =>
        getStateId(curr) match {
          case Some(id) =>
            currentState += (id -> curr.variable.name)

            if (!stateUsers.contains(id)) {
              stateUsers += (id -> ArrayBuffer.empty[DefProcess])
            }
            if (stateUsers(id).contains(p)) {
              ctx.logger.error(s"multiple definitions of the same current state register ${curr}!")
            } else {
              stateUsers(id) += p
            }
          case None =>
            ctx.logger.error("Missing a valid @REG annotation", curr)

        }
      }
    }

    val withSends = program.processes.map { ownerProcess =>
      val nextValues = ownerProcess.registers.collect {
        case r if r.variable.varType == OutputType => r
      }

      val sends = nextValues.flatMap { next =>
        getStateId(next) match {
          case Some(id) =>
            currentState.get(id) match {
              case Some(curr) =>
                if (stateUsers.contains(id)) {
                  ctx.logger.debug(
                    s"${id.id} is used by ${stateUsers(id).map { _.id }.mkString(",")}"
                  )
                  stateUsers(id).collect {
                    case userProcess if (userProcess.id != ownerProcess.id) =>
                      Send(curr, next.variable.name, userProcess.id)
                  }
                } else {
                  Nil
                }
              case None =>
                ctx.logger.warn("missing current value!", next)
                Nil
            }
          case None =>
            ctx.logger.error("Missing a valid @REG annotation", next)
            Nil
        }
      }
      ownerProcess.copy(
        body = ownerProcess.body ++ sends
      )
    }
    program.copy(processes = withSends)
  }
  private case class ProcessorDescriptor(
      inSet: BitSet,
      outSet: BitSet,
      body: BitSet,
      memory: Int
  ) {

    def merged(other: ProcessorDescriptor): ProcessorDescriptor = {
      val newOutSet = other.outSet union outSet
      val newInSet  = (inSet union other.inSet) diff newOutSet
      val newBody   = body union other.body
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
    val inputIndices  = scala.collection.mutable.Map.empty[Name, Int]
    val instrIndices  = scala.collection.mutable.Map.empty[Instruction, Int]
    var idx           = 0
    statePairs.foreach { case (current, next) =>
      inputIndices += (current.variable.name -> idx)
      outputIndices += (next.variable.name   -> idx)
      idx += 1
    }
    idx = 0
    process.body.foreach { instr =>
      instrIndices += (instr -> idx)
      idx += 1
    }
    val body = process.body.toArray
    val memories = process.registers.collect { case DefReg(MemoryVariable(n, sz, _, _), _, _) =>
      (n, sz)
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
      def inputIndex(r: Name): Int                 = inputIndices(r)
      def outputIndex(r: Name): Int                = outputIndices(r)
      def getInstruction(idx: Int): Instruction    = body(idx)
      def getInput(idx: Int): Name                 = statePairs(idx)._1.variable.name
      def getOutput(idx: Int): Name                = statePairs(idx)._2.variable.name
      def isInput(r: Name): Boolean                = inputIndices.contains(r)
      def isOutput(r: Name): Boolean               = outputIndices.contains(r)

      def isMemory(r: Name): Boolean = memoryIndices.contains(r)
      def memoryIndex(r: Name): Int  = memoryIndices(r)
      def memorySize(idx: Int): Int  = memories(idx)._2
    }
  }
  private def extractIndependentInstructionSequences(
      proc: DefProcess,
      parContext: ParallelizationContext
  )(implicit ctx: AssemblyContext) = {

    sealed trait SetElement
    case object Syscall                  extends SetElement
    case class Instr(instr: Instruction) extends SetElement
    case class Memory(name: Name)        extends SetElement
    case class State(name: Name)         extends SetElement

    val executionGraph = ctx.stats.recordRunTime("creating raw graph") {
      GraphBuilder.rawGraph(proc.body)
    }
    ctx.logger.dumpArtifact("original_exeuction_graph.dot") {
      GraphBuilder.toDotGraph(executionGraph)
    }
    // Collect the sink nodes in the graph. These are either writes to OutputType
    // registers, stores or system calls. Anything else is basically dead code
    val sinkNodes = executionGraph.nodes.filter(_.outDegree == 0)

    // Collect all the memories that are not read-only. We distinguish between
    // read-only and read-write memory when we are trying to split the process.
    // read-only memories can be easily copied and do not constrain parallelization.
    val nonCopyableMemory: Name => Boolean = proc.body.collect { case store: LocalStore =>
      assert(store.base == store.order.memory)
      store.base
    }.toSet

    val elements: Iterable[SetElement] = proc.registers.collect {
      case r if r.variable.varType == OutputType => State(r.variable.name)
      case r if r.variable.varType == MemoryType && nonCopyableMemory(r.variable.name) =>
        Memory(r.variable.name)
    } ++ sinkNodes.map(n => Instr(n.toOuter)) :+ Syscall

    // initialize the disjoint sets with elements that are either sink instructions,
    // memories, next state registers, or a unique Syscall object
    val disjoint = DisjointSets(elements)

    // To extract independent processes, we start from the sink node and do a
    // backward traversal in the read-after-write dependence graph. While doing
    // the traversal we check whether each instruction uses any "exclusively-owned"
    // resources. Such resources are the system call (i.e., clock-gating), memories
    // or the next value of state registers (i.e, OutputType). These resources
    // and the way they are used impose a fundamental limit on how we can extract
    // processes. For instance if in computing the next value of some state we
    // require access to the read/write memory. Then all the state registers
    // that access the memory should be co-located on the same process. To keep
    // track of these constraints. We create disjoint sets of instructions (and resources).
    // The initial disjoint sets are the instructions and the 3 kinds of resources.

    ctx.stats.recordRunTime("creating disjoint sets") {
      sinkNodes.foreach { sink =>
        val sinkInst = sink.toOuter
        // Note that a sink may have the form MOV o1, o2 where o1 and o2 are two
        // different output registers. This means we have to put o1 and o2
        // in the same process

        sink.outerNodeTraverser
          .withDirection(GraphTraversal.Predecessors)
          .foreach { instr =>
            // if this instruction references an state element, we should
            // make the union of the state element with the sink instruction
            // so that sink instruction that reference the same output variables
            // or memories are grouped together
            val namesReferenced =
              NameDependence.regDef(instr) ++ NameDependence.regUses(instr)
            namesReferenced.foreach { name =>
              // in case in our traversal we reference some output name we should
              // create a union between the sink node and the output names
              if (parContext.isOutput(name)) {
                disjoint.union(Instr(sinkInst), State(name))
              }
            }
            instr match {
              case _ @(_: Expect | _: Interrupt | _: PutSerial | _: GlobalLoad | _: GlobalStore) =>
                disjoint.union(Instr(sinkInst), Syscall)
              case load @ LocalLoad(_, mem, _, order, _) if nonCopyableMemory(mem) =>
                assert(order.memory == mem)
                disjoint.union(Instr(sinkInst), Memory(mem))
              case store @ LocalStore(_, mem, _, _, order, _) =>
                assert(order.memory == mem)
                disjoint.union(Instr(sinkInst), Memory(mem))
              case _ => // nothing special to do
            }
          }
      }
    }

    // now each set in our disjoint sets represents a set of sink nodes that make
    // up one process.

    def collectProcessBody(sinks: Iterable[Instruction]) = {

      val bodyBitSet = scala.collection.mutable.BitSet.empty
      val inBitSet   = scala.collection.mutable.BitSet.empty
      val outBitSet  = scala.collection.mutable.BitSet.empty
      val memBitSet  = scala.collection.mutable.BitSet.empty

      sinks.foreach { sinkInst =>
        val sinkNode = executionGraph.get(sinkInst)
        sinkNode.outerNodeTraverser
          .withDirection(GraphTraversal.Predecessors)
          .foreach { reachableInstr =>
            bodyBitSet += (parContext.instructionIndex(reachableInstr))
            for (use <- NameDependence.regUses(reachableInstr)) {
              if (parContext.isInput(use)) {
                inBitSet += parContext.inputIndex(use)
              }
              if (parContext.isMemory(use)) {
                val memIx = parContext.memoryIndex(use)
                memBitSet += memIx
              }
            }
            for (rd <- NameDependence.regDef(reachableInstr)) {
              if (parContext.isOutput(rd)) {
                outBitSet += parContext.outputIndex(rd)
              }
            }
          }
      }
      inBitSet --= outBitSet // if an input and its corresponding output
      // are referenced in the same process, then remove it from the inBitSet
      // because basically inBitSet should only contain InputType registers that
      // are not owned by this process and outBitSet should contains all the
      // OutputType registers that are owned by it.
      new ProcessorDescriptor(
        inSet = inBitSet,
        outSet = outBitSet,
        body = bodyBitSet,
        memory = memBitSet.foldLeft(0) { case (sz, idx) =>
          parContext.memorySize(idx)
        }
      )
    }

    ctx.logger.dumpArtifact("disjoint_sets.txt") {
      ctx.logger.info("Dumping disjoint sets")
      val builder = new StringBuilder
      disjoint.sets.foreach { set =>
        builder ++= "{\n"
        set.foreach {
          case Instr(instr) => builder ++= (instr.toString() + "\n")
          case Syscall      => builder ++= ("syscall\n")
          case Memory(m)    => builder ++= (m + "\n")
          case State(s)     => builder ++= (s + "\n")
        }
        builder ++= "}\n"
      }
      builder.toString()
    }
    val results = ctx.stats.recordRunTime("creating independent processes") {
      disjoint.sets
        .map { resourceSet =>
          val sinkInstrs = resourceSet.collect { case Instr(instr) => instr }
          sinkInstrs
        }
        .collect { case block if block.nonEmpty => collectProcessBody(block) }
    }
    ctx.stats.record("Maximum number of independent processes" -> results.size)
    results
  }

  private def doSplit(
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
      // need the div because the context value is in bytes
      (p1.memory + p2.memory) <= (ctx.max_local_memory / (16 / 8))

    val toMerge =
      scala.collection.mutable.Queue.empty[ProcessorDescriptor] ++ splitted

    sealed trait MergeResult
    case class MergeChoice(result: ProcessorDescriptor, index: Int, cost: Int) extends MergeResult
    case object NoMerge                                                        extends MergeResult
    ctx.stats.recordRunTime("merging processes") {
      while (toMerge.nonEmpty) {
        val head = toMerge.dequeue()
        val (_, best) =
          mergedProcesses.foldLeft[(Int, MergeResult)](0, NoMerge) { case ((ix, prevBest), currentChoice) =>
            if (canMerge(currentChoice, head)) {
              val possibleMerge = head merged currentChoice
              val possibleCost  = estimateCost(possibleMerge)
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
    }

    // val stateOwner =
    val stateUsers = Array.fill(bitSetIndexer.numStateRegs()) { BitSet.empty }

    mergedProcesses.zipWithIndex.foreach { case (ProcessorDescriptor(inSet, _, _, _), ix) =>
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
              val nextStateInHere    = bitSetIndexer.getOutput(nextStateIndex)
              Send(currentStateInDest, nextStateInHere, processId(recipient))
          }
        }
        val bodyWithSends = body ++ sends
        val referenced    = NameDependence.referencedNames(bodyWithSends)
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
            body = bodyWithSends,
            globalMemories =
              if (
                bodyWithSends.exists { inst: Instruction =>
                  inst.isInstanceOf[GlobalLoad] || inst.isInstanceOf[GlobalStore]
                }
              ) {
                program.processes.head.globalMemories
              } else Nil
          )
          .setPos(program.processes.head.pos)
    }

    val result = program.copy(finalProcesses.toSeq.filter(_.body.nonEmpty))

    ctx.logger.dumpArtifact("connectivity.dot") {

      import scalax.collection.mutable.Graph
      import scalax.collection.GraphEdge.DiEdge
      val g = Graph.empty[ProcessId, DiEdge]
      g ++= result.processes.map(_.id)

      result.processes.foreach { process =>
        process.body.foreach {
          case Send(_, _, dest, _) =>
            g += DiEdge(process.id, dest)
          case _ => // nothing
        }
      }
      import scalax.collection.io.dot._
      import scalax.collection.io.dot.implicits._
      val dotRoot = DotRootGraph(
        directed = true,
        id = Some("Connectivity graph")
      )
      def edgeTransform(
          iedge: scalax.collection.Graph[ProcessId, DiEdge]#EdgeT
      ): Option[(DotGraph, DotEdgeStmt)] = iedge.edge match {
        case DiEdge(source, target) =>
          Some(
            (
              dotRoot,
              DotEdgeStmt(
                source.toOuter.toString,
                target.toOuter.toString
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
          inode: scalax.collection.Graph[ProcessId, DiEdge]#NodeT
      ): Option[(DotGraph, DotNodeStmt)] =
        Some(
          (
            dotRoot,
            DotNodeStmt(
              NodeId(inode.toOuter.toString()),
              List(DotAttr("label", inode.toOuter.toString.trim.take(64)))
            )
          )
        )

      val dotExport: String = g.toDot(
        dotRoot = dotRoot,
        edgeTransformer = edgeTransform,
        cNodeTransformer = Some(nodeTransformer), // connected nodes
        iNodeTransformer = Some(nodeTransformer)  // isolated nodes
      )
      dotExport
    }
    ctx.stats.record(ProgramStatistics.mkProgramStats(result))
    result

  }
}
