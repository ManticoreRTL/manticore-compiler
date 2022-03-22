package manticore.compiler.assembly.levels

/** OrderInstructions.scala
  *
  * @author
  *   Sahand Kashani <sahand.kashani@epfl.ch>
  *   Mahyar Emami   <mahyar.emami@epfl.ch>
  */

import manticore.compiler.assembly.DependenceGraphBuilder
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import scala.collection.mutable.ArrayBuffer
import manticore.compiler.assembly.ManticoreAssemblyIR

/** This transform sorts instructions based on their depenencies.
  */
trait OrderInstructions extends DependenceGraphBuilder with Flavored {

  // Object Impl is declared private so flavor does not escape its defining scope
  // (when flavor.<something> is returned from a method).

  import flavor._

  def orderInstructions(
      proc: DefProcess
  )(
    implicit ctx: AssemblyContext
  ): DefProcess = {

    // The value of the label doesn't matter for topological sorting.
    // Must use Instruction instead of flavor.Instruction as GraphBuilder requires knowledge
    // of the type itself and cannot use the instance variable flavor to infer the type.

    val dependence_graph = DependenceAnalysis.build(proc, (_, _) => None)(ctx)

    if (dependence_graph.isCyclic) {
      ctx.logger.error(
        "Dependence graph is not acyclic, can not order instruction!"
      )
      proc
    } else {

      // Alternative to performing DFS, we can use the topologicalSort visitor
      // in the dependence_graph but that one is orders of magnitude slower than
      // our simple DFS here, so don't use it :D
      val schedule = scala.collection.mutable.Stack.empty[Instruction]

      val to_schedule = dependence_graph.nodes.size

      val scheduled_count = 0

      val visited = scala.collection.mutable.Set.empty[dependence_graph.NodeT]
      val unvisited = scala.collection.mutable.Queue
        .empty[dependence_graph.NodeT] ++ dependence_graph.nodes

      def visitNode(n: dependence_graph.NodeT): Unit = {
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
      proc.copy(
        body = schedule
          .popAll()
          .toSeq
      )

    }

  }

  def do_transform(
      asm: DefProgram,
      context: AssemblyContext
  ): DefProgram = {
    implicit val ctx = context

    val out = DefProgram(
      processes = asm.processes.map(orderInstructions),
    )

    out
  }

}
