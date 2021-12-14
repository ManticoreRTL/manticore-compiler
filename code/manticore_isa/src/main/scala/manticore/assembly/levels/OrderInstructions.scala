package manticore.assembly.levels

/** OrderInstructions.scala
  *
  * @author
  *   Sahand Kashani <sahand.kashani@epfl.ch>
  */

import manticore.assembly.DependenceGraphBuilder
import manticore.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import scala.collection.mutable.ArrayBuffer
import manticore.assembly.ManticoreAssemblyIR

/** This transform sorts instructions based on their depenencies.
  */
abstract class OrderInstructions[T <: ManticoreAssemblyIR](irFlavor: T)
    extends AssemblyTransformer(irFlavor, irFlavor) {

  // Object Impl is declared private so irFlavor does not escape its defining scope
  // (when irFlavor.<something> is returned from a method).
  private object Impl {

    import irFlavor._

    def orderInstructions(
        asm: DefProcess
    )(
        ctx: AssemblyContext
    ): DefProcess = {

      object GraphBuilder extends DependenceGraphBuilder(irFlavor)

      // The value of the label doesn't matter for topological sorting.
      // Must use T#Instruction instead of irFlavor.Instruction as GraphBuilder requires knowledge
      // of the type itself and cannot use the instance variable irFlavor to infer the type.
      def labelingFunc(pred: T#Instruction, succ: T#Instruction) = None
      val dependenceGraph = GraphBuilder.build(asm, labelingFunc)(ctx)

      // Sort body.
      val sortedInstrs = ArrayBuffer[Instruction]()
      dependenceGraph.topologicalSort match {
        case Left(cycleNode) =>
          logger.error("Dependence graph contains a cycle!")
        case Right(order) =>
          order.foreach { instr =>
            // Must cast the result back to irFlavor as this is a result from GraphBuilder
            // and GraphBuilder only knows T.
            sortedInstrs.append(
              instr.toOuter.asInstanceOf[irFlavor.Instruction]
            )
          }
      }

      // Sort registers.
      val sortedRegs = asm.registers.sortBy { reg =>
        (reg.variable.varType.typeName, reg.variable.name.toString())
      }

      asm.copy(
        registers = sortedRegs,
        body = sortedInstrs.toSeq
      )
    }

    def apply(
        asm: DefProgram,
        context: AssemblyContext
    ): DefProgram = {
      implicit val ctx = context

      val out = DefProgram(
        processes =
          asm.processes.map(process => orderInstructions(process)(ctx)),
        annons = asm.annons
      )

      if (logger.countErrors > 0) {
        logger.fail(s"Failed transform due to previous errors!")
      }

      out
    }
  }

  // Note that we use `T#` for the method signature instead of `irFlavor._` as irFlavor is private to
  // this instance and cannot escape it (as the return value for example).
  override def transform(
      asm: T#DefProgram,
      context: AssemblyContext
  ): T#DefProgram = {
    val asmIn = asm.asInstanceOf[irFlavor.DefProgram]
    val asmOut = Impl(asmIn, context)
    asmOut
  }
}
