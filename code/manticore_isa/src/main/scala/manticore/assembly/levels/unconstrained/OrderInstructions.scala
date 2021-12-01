package manticore.assembly.levels.unconstrained

/** OrderInstructions.scala
  *
  * @author
  *   Sahand Kashani <sahand.kashani@epfl.ch>
  */

import manticore.assembly.DependenceGraphBuilder
import manticore.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import scala.collection.mutable.ArrayBuffer

/** This transform sorts instructions based on their depenencies.
  */
object OrderInstructions
    extends AssemblyTransformer(UnconstrainedIR, UnconstrainedIR) {

  import UnconstrainedIR._

  def orderInstructions(
      asm: DefProcess
  )(
      ctx: AssemblyContext
  ): DefProcess = {

    object GraphBuilder extends DependenceGraphBuilder(UnconstrainedIR)

    // The value of the label doesn't matter for topological sorting.
    def labelingFunc(pred: Instruction, succ: Instruction) = None
    val dependenceGraph = GraphBuilder.build(asm, labelingFunc)(ctx)

    // Sort body.
    val sortedInstrs = ArrayBuffer[Instruction]()
    dependenceGraph.topologicalSort match {
      case Left(cycleNode) =>
        logger.error("Dependence graph contains a cycle!")
      case Right(order) =>
        order.foreach { instr =>
          sortedInstrs.append(instr)
        }
    }

    // Sort registers.
    val sortedRegs = asm.registers.sortBy { reg =>
      (reg.variable.tpe.typeName, reg.variable.name)
    }

    asm.copy(
      registers = sortedRegs,
      body = sortedInstrs
    )
  }

  override def transform(
      asm: DefProgram,
      context: AssemblyContext
  ): DefProgram = {
    implicit val ctx = context

    val out = DefProgram(
      processes = asm.processes.map(process => orderInstructions(process)(ctx)),
      annons = asm.annons
    )

    if (logger.countErrors > 0) {
      logger.fail(s"Failed transform due to previous errors!")
    }

    out
  }
}
