package manticore.assembly

/** OrderInstructions.scala
  *
  * @author
  *   Sahand Kashani <sahand.kashani@epfl.ch>
  */

import manticore.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import scalax.collection.edge.LDiEdge
import scala.collection.mutable.ArrayBuffer

/** This transform sorts instructions based on their depenencies.
  */
class OrderInstructions[T <: ManticoreAssemblyIR](irFlavor: T)
    extends AssemblyTransformer(irFlavor, irFlavor) {

  import irFlavor._
  private def orderInstructions(
      asm: DefProcess
  )(
      ctx: AssemblyContext
  ): DefProcess = {

    import manticore.assembly.DependenceGraphBuilder
    object GraphBuilder extends DependenceGraphBuilder(irFlavor)

    // The value of the label doesn't matter for topological sorting.
    case class Label(v: Int)
    def labelingFunc(pred: T#Instruction, succ: T#Instruction): Label = Label(0)
    val dependenceGraph = GraphBuilder.build[Label](asm, labelingFunc)(ctx)

    // Sort body.
    val sortedInstrs = ArrayBuffer[T#Instruction]()
    dependenceGraph.topologicalSort match {
      case Left(cycleNode) =>
        logger.error("Dependence graph contains a cycle!")
      case Right(order) =>
        order.foreach { instr =>
          sortedInstrs.append(instr)
        }
    }

    // Sort registers.
    val sortedRegs = asm.registers
      .groupBy { reg =>
        reg.variable.varType
      }.map(_._2).flatten.toSeq


    // // Debug: dump the graph
    // logger.dumpArtifact(s"dependence_graph.dot") {

    //   import scalax.collection.io.dot._
    //   import scalax.collection.io.dot.implicits._
    //   import scalax.collection.Graph

    //   val dot_root = DotRootGraph(
    //     directed = true,
    //     id = Some("List scheduling dependence graph")
    //   )
    //   def edgeTransform(
    //       iedge: Graph[Instruction, LDiEdge]#EdgeT
    //   ): Option[(DotGraph, DotEdgeStmt)] = iedge.edge match {
    //     case LDiEdge(source, target, l) =>
    //       Some(
    //         (
    //           dot_root,
    //           DotEdgeStmt(
    //             source.toOuter.hashCode().toString,
    //             target.toOuter.hashCode().toString,
    //             List(DotAttr("label", l.asInstanceOf[Label].v.toString))
    //           )
    //         )
    //       )
    //     case t @ _ =>
    //       logger.error(
    //         s"An edge in the dependence could not be serialized! ${t}"
    //       )
    //       None
    //   }
    //   def nodeTransformer(
    //       inode: Graph[Instruction, LDiEdge]#NodeT
    //   ): Option[(DotGraph, DotNodeStmt)] =
    //     Some(
    //       (
    //         dot_root,
    //         DotNodeStmt(
    //           NodeId(inode.toOuter.hashCode().toString()),
    //           List(DotAttr("label", inode.toOuter.serialized.trim))
    //         )
    //       )
    //     )

    //   val dot_export: String = dependenceGraph.toDot(
    //     dotRoot = dot_root,
    //     edgeTransformer = edgeTransform,
    //     cNodeTransformer = Some(nodeTransformer)
    //   )
    //   dot_export
    // }(ctx)

    asm.copy(
      registers = sortedRegs,
      body = sortedInstrs.map(_.asInstanceOf[Instruction])
    )
  }

  // override def transform(source: T#DefProgram, context: AssemblyContext): T#DefProgram = ???

  override def transform(
      asm: T#DefProgram,
      context: AssemblyContext
  ): T#DefProgram = {
    implicit val ctx = context
    val prog = asm.asInstanceOf[DefProgram]
    val out = DefProgram(
      processes =
        prog.processes.map(process => orderInstructions(process)(ctx)),
      annons = prog.annons
    )

    if (logger.countErrors > 0) {
      logger.fail(s"Failed transform due to previous errors!")
    }

    out
  }
}
