package manticore.compiler.assembly.levels

import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.annotations.{Reg => RegAnnotation}
import manticore.compiler.assembly.annotations.AssemblyAnnotation
import manticore.compiler.assembly.annotations.StringValue
import manticore.compiler.assembly.annotations.AssemblyAnnotationFields
import java.util.function.BinaryOperator


/**
  * Close sequential cycles to persist register values across virtual cycles
  * @author
  *   Mahyar Emami   <mahyar.emami@eplf.ch>
  */
trait CloseSequentialCycles extends CanCollectInputOutputPairs {

  import flavor._

  private def do_transform(
      proc: DefProcess
  )(implicit ctx: AssemblyContext): DefProcess = {

    val move_instructions = InputOutputPairs.createInputOutputPairs(proc).map {
      case (curr, next) =>
        Mov(curr.variable.name, next.variable.name)
    }
    proc.copy(body = proc.body ++ move_instructions)

  }
  def do_transform(source: DefProgram)(implicit
      ctx: AssemblyContext
  ): DefProgram =
    source.copy(
      processes = source.processes.map(do_transform)
    )

}
