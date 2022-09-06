package manticore.compiler.assembly.levels.placed.parallel.util

import manticore.compiler.assembly.levels.Flavored
import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.placed.Helpers.InputOutputPairs
import manticore.compiler.assembly.levels.placed.Helpers.NameDependence
import manticore.compiler.assembly.levels.placed.Helpers.GraphBuilder
import manticore.compiler.assembly.levels.OutputType
import manticore.compiler.assembly.levels.MemoryType
import scalax.collection.GraphTraversal
import manticore.compiler.assembly.annotations.Reg
import manticore.compiler.assembly.levels.InputType
import manticore.compiler.assembly.levels.placed.PlacedIRTransformer
import manticore.compiler.assembly.annotations.Loc

import collection.mutable.{Map => MMap}

trait BasicProcessExtraction extends PlacedIRTransformer {

  import PlacedIR._

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

  protected def createParallelizationContext(
      process: DefProcess
  )(implicit ctx: AssemblyContext): ParallelizationContext = {

    val statePairs =
      InputOutputPairs.createInputOutputPairs(process).toArray

    val outputIndices = MMap.empty[Name, Int]
    val inputIndices  = MMap.empty[Name, Int]
    val instrIndices  = MMap.empty[Instruction, Int]
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
    val memoryIndices = MMap.empty[Name, Int]
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
  protected def extractIndependentInstructionSequences(
      proc: DefProcess,
      parContext: ParallelizationContext
  )(implicit ctx: AssemblyContext): Iterable[ProcessDescriptor] = {

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
    // registers, stores or system calls. Anything else is basically dead code.
    val sinkNodes = executionGraph.nodes.filter(_.outDegree == 0)

    // Collect all the memories that are not read-only. We distinguish between
    // read-only and read-write memory when we are trying to split the process.
    // Read-only memories can be easily copied and do not constrain parallelization.
    val nonCopyableMemory: Name => Boolean = proc.body.collect { case store: LocalStore =>
      assert(store.base == store.order.memory)
      store.base
    }.toSet

    val elements: Iterable[SetElement] = proc.registers.collect {
      case r if r.variable.varType == OutputType => State(r.variable.name)
      case r if r.variable.varType == MemoryType && nonCopyableMemory(r.variable.name) =>
        Memory(r.variable.name)
    } ++ sinkNodes.map(n =>
      Instr(n.toOuter)
    ) :+ Syscall // All syscalls are on 1 core, so there is just 1 "syscall" in the disjoint set.

    // Initialize the disjoint sets with elements that are either sink instructions,
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
          .foreach { predInstr =>
            // If this instruction references a state element, we should
            // union of the state element with the sink instruction
            // so that sink instructions that reference the same output variables
            // or memories are grouped together.
            val namesReferenced = NameDependence.regDef(predInstr) ++ NameDependence.regUses(predInstr)
            namesReferenced.foreach { name =>
              // In case in our traversal we reference some output name we should
              // create a union between the sink node and the output names.
              if (parContext.isOutput(name)) {
                disjoint.union(Instr(sinkInst), State(name))
              }
            }
            predInstr match {
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

      new ProcessDescriptor(
        inSet = inBitSet,
        outSet = outBitSet,
        body = bodyBitSet,
        memory = memBitSet
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

  protected def createSends(program: DefProgram)(implicit ctx: AssemblyContext): DefProgram = {

    import scala.collection.mutable.ArrayBuffer
    case class StateId(id: String, index: Int)
    val stateUsers = MMap.empty[StateId, ArrayBuffer[DefProcess]]

    val currentState = MMap.empty[StateId, Name]

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

  final override def transform(program: DefProgram)(implicit ctx: AssemblyContext): DefProgram = {
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

  protected def doSplit(program: DefProgram)(implicit ctx: AssemblyContext): DefProgram

  final protected def connectivityDotGraph(result: DefProgram)(implicit ctx: AssemblyContext) = {
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
}
