package manticore.compiler.assembly.levels

/** OrderInstructions.scala
  *
  * @author
  *   Sahand Kashani <sahand.kashani@epfl.ch> Mahyar Emami
  *   <mahyar.emami@epfl.ch>
  */

import manticore.compiler.assembly.DependenceGraphBuilder
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import scala.collection.mutable.ArrayBuffer
import manticore.compiler.assembly.ManticoreAssemblyIR
import scalax.collection.edge.LDiEdge
import scalax.collection.Graph
import scalax.collection.mutable.{Graph => MutableGraph}
import scalax.collection.GraphEdge.DiEdge
import manticore.compiler.assembly.CanBuildDependenceGraph

// base trait for any transform that wants to be able to reorder instruction

trait CanOrderInstructions extends CanBuildDependenceGraph {

  object InstructionOrder {
    import flavor._

    private def dependenceGraph(
        instructionBlock: Iterable[Instruction]
    )(implicit ctx: AssemblyContext) = {
      def readAfterWriteEdge(source: Instruction, target: Instruction) =
        DiEdge(source, target)
      def antiDependenceEdge(source: Instruction, target: Instruction) =
        DiEdge(source, target)
      def graphNode(inst: Instruction) = inst

      val dependence_graph = GraphBuilder(instructionBlock)(
        graphNode,
        readAfterWriteEdge,
        Some(antiDependenceEdge(_, _))
      )
      dependence_graph
    }

    def findCycle(g: MutableGraph[Instruction, DiEdge]) = {

      // a work around to make finding cycles faster: using g.findCycle
      // takes too long if there is no cycle, so we perform our own dfs
      // search to only call that function when there really is a cycle
      sealed trait VisitState
      case object InStack   extends VisitState
      case object Visited   extends VisitState
      case object Unvisited extends VisitState

      val state = scala.collection.mutable.Map.empty[g.NodeT, VisitState] ++ g.nodes.map { _ -> Unvisited }

      val stack = scala.collection.mutable.Stack.empty[g.NodeT]

      def getCycle(n: g.NodeT): Iterable[g.NodeT] = {
        stack.popWhile(_ != n)
      }
      def dfsCheck(n: g.NodeT): Iterable[g.NodeT] = {
        def successorCycles(xs: Seq[g.NodeT]): Iterable[g.NodeT] = xs match {
          case succ +: tail =>
            if (state(succ) == InStack) {

              val c = getCycle(succ)
              c ++ Seq(succ)
            } else {
              stack push succ
              state(succ) = InStack
              dfsCheck(succ)
            }
          case Nil => Nil
        }

        val cycle = successorCycles(n.diSuccessors.toSeq)
        state(n) = Visited
        if (stack.nonEmpty) {
          stack.pop()
        }
        cycle
      }

      def find(nodes: Seq[g.NodeT]): Iterable[g.NodeT] = nodes match {
        case n +: tail if (state(n) == Unvisited) =>
          stack push n
          state(n) = InStack
          val cycle = dfsCheck(n)
          if (cycle.isEmpty) {
            find(tail)
          } else {
            cycle
          }
        case n +: tail =>
          find(tail)
        case Nil =>
          Nil
      }
      find(g.nodes.toSeq)
    }
    def reorder(
        instructionBlock: Iterable[Instruction]
    )(implicit ctx: AssemblyContext): Iterable[Instruction] = {

      val depGraph = dependenceGraph(instructionBlock)

      val cycle = ctx.stats.recordRunTime("finding a cycle") { findCycle(depGraph) }
      if (cycle.nonEmpty) {

        ctx.logger.error("Found combinational loop!")
        ctx.logger.dumpArtifact("cycle.txt") {
          cycle.map { _.toOuter.toString().take(50) }.mkString("->\n")
        }

        ctx.logger.error(
          "Dependence graph is not acyclic, can not order instruction!"
        )
        ctx.logger.dumpArtifact(s"cyclic_dependence_graph_${transformId}.dot") {
          GraphBuilder.toDotGraph(depGraph)
        }
        instructionBlock

      } else {

        val schedule = scala.collection.mutable.Stack.empty[Instruction]

        val to_schedule = depGraph.nodes.size

        val scheduled_count = 0

        val visited = scala.collection.mutable.Set.empty[depGraph.NodeT]
        val unvisited = scala.collection.mutable.Queue
          .empty[depGraph.NodeT] ++ depGraph.nodes

        def visitNode(n: depGraph.NodeT): Unit = {
          if (!visited.contains(n)) {

            n.diSuccessors.foreach { succ =>
              visitNode(succ)
            }
            visited += n
            schedule push n.toOuter
          }
        }
        while (unvisited.nonEmpty) {
          val n = unvisited.dequeue()
          visitNode(n)
        }

        // IMPORTANT, popAll in scala 2.13.7 is broken! if you downgrade
        // the scala version this will break!

        assert(
          util.Properties.versionNumberString == "2.13.8",
          "popAll requires care in scala earlier than 2.13.8"
        )

        val orderedBlock = schedule.popAll()
        orderedBlock
      }
      // }
    }
    def reorder(process: DefProcess)(implicit ctx: AssemblyContext): DefProcess = {
      val orderedBody = reorder(process.body)
      process
        .copy(
          body = orderedBody.toSeq
        )
        .setPos(process.pos)
    }

  }
}

/** This transform sorts instructions based on their dependencies.
  */
trait OrderInstructions extends CanOrderInstructions {

  // Object Impl is declared private so flavor does not escape its defining scope
  // (when flavor.<something> is returned from a method).

  import flavor._

  def do_transform(
      asm: DefProgram,
      context: AssemblyContext
  ): DefProgram = {
    implicit val ctx = context

    val out = DefProgram(
      processes = asm.processes.map(InstructionOrder.reorder)
    )

    out
  }

}
