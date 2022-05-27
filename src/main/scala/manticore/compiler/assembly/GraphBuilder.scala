package manticore.compiler.assembly

import manticore.compiler.assembly.levels.Flavored
import scalax.collection.GraphEdge.DiEdge
import scalax.collection.edge.LDiEdge
import scalax.collection.mutable.{Graph => MutableGraph}
import scalax.collection.{Graph => ImmutableGraph}
import manticore.compiler.AssemblyContext
import scala.reflect.ClassTag
import javax.sound.midi.Instrument

trait CanBuildDependenceGraph extends CanComputeNameDependence {

  import flavor._

  object GraphBuilder {

    def build[Node, Edge[+N] <: DiEdge[N]](
        instructionBlock: Iterable[Instruction]
    )(
        graphNode: Instruction => Node,
        readAfterWriteEdge: (Instruction, Instruction) => Edge[Node],
        antiDependenceEdge: Option[(Instruction, Instruction) => Edge[Node]]
    )(implicit
        ctx: AssemblyContext,
        edgeT: ClassTag[Edge[Node]]
    ): MutableGraph[Node, Edge] = {

      val withAntiDependence = antiDependenceEdge.nonEmpty
      val defInst = NameDependence.definingInstruction(instructionBlock)
      val graph = MutableGraph.empty[Node, Edge]
      val nodeLookup = scala.collection.mutable.Map.empty[Instruction, Node]
      instructionBlock.foreach { instruction =>
        val node = graphNode(instruction)
        if (withAntiDependence) { nodeLookup += (instruction -> node) }
        graph add node
        NameDependence.regUses(instruction).foreach { use =>
          defInst.get(use).foreach { pred =>
            graph add readAfterWriteEdge(pred, instruction)
          }
        }
      }

      if (withAntiDependence) {
        // now add explicit orderings as dependencies
        val hasOrder = instructionBlock.collect {
          case inst: ExplicitlyOrderedInstruction => inst
        }

        val groups = hasOrder.groupBy { inst =>
          inst.order match {
            case scall: SystemCallOrder => "$syscall"
            case morder: MemoryAccessOrder =>
              s"$$memory:${morder.memory.toString()}"
          }
        }

        // groups.foreach { case (_, block) =>
        //   block.sortBy(_.order).sliding(2).foreach { case Seq(prev, next) =>
        //     raw_dependence_graph += LDiEdge(prev, next)(label(prev, next))
        //   }
        // }

        // One thing we need to do is to create dependencies between memory operations
        // and systemcalls. To do so, we start from every load operation and traverse
        // the dependence graph in a DFS manner, keeping a set of visited nodes and
        // a set of nodes that contribute to a system call
        val memoryGroups = groups.filter(_._1 != "$syscall")

        if (groups.contains("$syscall")) {
          val syscallGroup = groups("$syscall").toSeq.sortBy(_.order)
          val hasDependencyToSyscall =
            scala.collection.mutable.Map.empty[Node, Boolean]

          def visitGraph(n: MutableGraph[Node, Edge]#NodeT): Unit = {
            if (!hasDependencyToSyscall.contains(n.toOuter)) {
              n.toOuter match {
                case syscall: SystemCallOrder =>
                  hasDependencyToSyscall += (n.toOuter -> true)
                case inst =>
                  n.diSuccessors.foreach(visitGraph)
                  val isConnectedToSyscall =
                    n.diSuccessors.exists(hasDependencyToSyscall(_) == true)
                  hasDependencyToSyscall += (n.toOuter -> isConnectedToSyscall)
              }
            }
          }

          // Let's visit the graph and see which memory ops should come before
          // systemcalls. Note that this never going to be the case for actual programs
          // since by construction syscalls should appear at the beginning of a cycle
          // not in the middle. We do this for hand-written program that may have
          // memory loads before systemcalls that actually need to be scheduled
          // before systemcalls
          memoryGroups.foreach { case (_, blk) =>
            blk.foreach { memop =>
              if (syscallGroup.nonEmpty) {
                val opNode = nodeLookup(memop)
                visitGraph(graph.get(opNode))
                if (!hasDependencyToSyscall(opNode)) {
                  // create the dependency artificially
                  antiDependenceEdge.foreach { edgeBuilder =>
                    graph add edgeBuilder(memop, syscallGroup.last)
                  }
                }
              }
            }
          }
          // Finally go though all the groups and create dependencies within each
          syscallGroup.sliding(2).foreach { case Seq(prev, next) =>
            antiDependenceEdge.foreach { edgeBuilder =>
              graph add edgeBuilder(prev, next)
            }
          }
        }

        memoryGroups.foreach { case (_, blk) =>
          blk.sliding(2).foreach { case Seq(prev, next) =>
            antiDependenceEdge.foreach { edgeBuilder =>
              graph add edgeBuilder(prev, next)
            }
          }
        }
      }
      graph
    }

    def toDotGraph[N](
        graph: ImmutableGraph[N, DiEdge]
    )(implicit ctx: AssemblyContext): String = {
      import scalax.collection.io.dot._
      import scalax.collection.io.dot.implicits._
      val dotRoot = DotRootGraph(
        directed = true,
        id = Some("List scheduling dependence graph")
      )
      val nodeIndex = graph.nodes.map(_.toOuter).zipWithIndex.toMap
      def edgeTransform(
          iedge: ImmutableGraph[N, DiEdge]#EdgeT
      ): Option[(DotGraph, DotEdgeStmt)] = iedge.edge match {
        case DiEdge(source, target) =>
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
          inode: ImmutableGraph[N, DiEdge]#NodeT
      ): Option[(DotGraph, DotNodeStmt)] =
        Some(
          (
            dotRoot,
            DotNodeStmt(
              NodeId(nodeIndex(inode.toOuter)),
              List(DotAttr("label", inode.toOuter.toString.trim.take(64)))
            )
          )
        )

      val dotExport: String = graph.toDot(
        dotRoot = dotRoot,
        edgeTransformer = edgeTransform,
        cNodeTransformer = Some(nodeTransformer), // connected nodes
        iNodeTransformer = Some(nodeTransformer) // isolated nodes
      )
      dotExport
    }

  }

}
abstract class GraphBuilder(f: ManticoreAssemblyIR)
    extends CanComputeNameDependence {
  val flavor = f
  import flavor._
  type Node
  type Edge[+N] <: DiEdge[N]

  val withAntiDependence: Boolean = true

  def readAfterWriteEdge(
      g: MutableGraph[Node, Edge],
      source: Instruction,
      target: Instruction
  ): Unit

  def antiDependenceEdge(
      g: MutableGraph[Node, Edge],
      source: Instruction,
      target: Instruction
  ): Unit

  def graphNode(inst: Instruction): Node

  def build(
      instructionBlock: Iterable[Instruction]
  )(implicit
      ctx: AssemblyContext,
      edgeT: ClassTag[Edge[Node]]
  ): MutableGraph[Node, Edge] = {

    val defInst = NameDependence.definingInstruction(instructionBlock)
    val graph = MutableGraph.empty[Node, Edge]
    val nodeLookup = scala.collection.mutable.Map.empty[Instruction, Node]
    instructionBlock.foreach { instruction =>
      val node = graphNode(instruction)
      if (withAntiDependence) { nodeLookup += (instruction -> node) }
      graph += node
      NameDependence.regUses(instruction).foreach { use =>
        defInst.get(use).foreach { pred =>
          readAfterWriteEdge(graph, pred, instruction)
        }
      }
    }

    if (withAntiDependence) {
      // now add explicit orderings as dependencies
      val hasOrder = instructionBlock.collect {
        case inst: ExplicitlyOrderedInstruction => inst
      }

      val groups = hasOrder.groupBy { inst =>
        inst.order match {
          case scall: SystemCallOrder => "$syscall"
          case morder: MemoryAccessOrder =>
            s"$$memory:${morder.memory.toString()}"
        }
      }

      // groups.foreach { case (_, block) =>
      //   block.sortBy(_.order).sliding(2).foreach { case Seq(prev, next) =>
      //     raw_graph += LDiEdge(prev, next)(label(prev, next))
      //   }
      // }

      // One thing we need to do is to create dependencies between memory operations
      // and systemcalls. To do so, we start from every load operation and traverse
      // the dependence graph in a DFS manner, keeping a set of visited nodes and
      // a set of nodes that contribute to a system call
      val memoryGroups = groups.filter(_._1 != "$syscall")

      if (groups.contains("$syscall")) {
        val syscallGroup = groups("$syscall").toSeq.sortBy(_.order)
        val hasDependencyToSyscall =
          scala.collection.mutable.Map.empty[Node, Boolean]

        def visitGraph(n: MutableGraph[Node, Edge]#NodeT): Unit = {
          if (!hasDependencyToSyscall.contains(n.toOuter)) {
            n.toOuter match {
              case syscall: SystemCallOrder =>
                hasDependencyToSyscall += (n.toOuter -> true)
              case inst =>
                n.diSuccessors.foreach(visitGraph)
                val isConnectedToSyscall =
                  n.diSuccessors.exists(hasDependencyToSyscall(_) == true)
                hasDependencyToSyscall += (n.toOuter -> isConnectedToSyscall)
            }
          }
        }

        // Let's visit the graph and see which memory ops should come before
        // systemcalls. Note that this never going to be the case for actual programs
        // since by construction syscalls should appear at the beginning of a cycle
        // not in the middle. We do this for hand-written program that may have
        // memory loads before systemcalls that actually need to be scheduled
        // before systemcalls
        memoryGroups.foreach { case (_, blk) =>
          blk.foreach { memop =>
            if (syscallGroup.nonEmpty) {
              val opNode = nodeLookup(memop)
              visitGraph(graph.get(opNode))
              if (!hasDependencyToSyscall(opNode)) {
                // create the dependency artificially
                antiDependenceEdge(graph, memop, syscallGroup.last)
              }
            }
          }
        }
        // Finally go though all the groups and create dependencies within each
        syscallGroup.sliding(2).foreach { case Seq(prev, next) =>
          antiDependenceEdge(graph, prev, next)
        }
      }

      memoryGroups.foreach { case (_, blk) =>
        blk.sliding(2).foreach { case Seq(prev, next) =>
          antiDependenceEdge(graph, prev, next)
        }
      }
    }
    graph
  }
}

class DefaultGraphBuilder(f: ManticoreAssemblyIR) extends GraphBuilder(f) {

  import flavor._
  type Node = Instruction
  type Edge[+N] = DiEdge[N]

  def readAfterWriteEdge(
      g: MutableGraph[Node, Edge],
      source: Instruction,
      target: Instruction
  ): Unit = { g += DiEdge(source, target) }

  def antiDependenceEdge(
      g: MutableGraph[Node, Edge],
      source: Instruction,
      target: Instruction
  ): Unit = { g += DiEdge(source, target) }

  def graphNode(inst: Instruction): Node = inst

}
