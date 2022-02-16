package manticore.compiler.assembly.levels

/** OrderInstructions.scala
  *
  * @author
  *   Sahand Kashani <sahand.kashani@epfl.ch>
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
      ctx: AssemblyContext
  ): DefProcess = {

    // The value of the label doesn't matter for topological sorting.
    // Must use Instruction instead of flavor.Instruction as GraphBuilder requires knowledge
    // of the type itself and cannot use the instance variable flavor to infer the type.
    def labelingFunc(pred: Instruction, succ: Instruction) = None
    val dependenceGraph = DependenceAnalysis.build(proc, labelingFunc)(ctx)

    // Sort body.
    val sortedInstrs = ArrayBuffer[Instruction]()
    dependenceGraph.topologicalSort match {
      case Left(cycleNode) =>
        ctx.logger.error("Dependence graph contains a cycle!")
      case Right(order) =>
        order.foreach { instr =>
          // Must cast the result back to flavor as this is a result from GraphBuilder
          // and GraphBuilder only knows T.
          sortedInstrs.append(
            instr.toOuter
          )
        }
    }

    // Sort registers.
    val sortedRegs = proc.registers.sortBy { reg =>
      (reg.variable.varType.typeName, reg.variable.name.toString())
    }

    proc.copy(
      registers = sortedRegs,
      body = sortedInstrs.toSeq
    )
  }

  def do_transform(
      asm: DefProgram,
      context: AssemblyContext
  ): DefProgram = {
    implicit val ctx = context

    val out = DefProgram(
      processes = asm.processes.map(process => orderInstructions(process)(ctx)),
      annons = asm.annons
    )

    if (ctx.logger.countErrors() > 0) {
      ctx.logger.fail(s"Failed transform due to previous errors!")
    }

    out
  }


}
