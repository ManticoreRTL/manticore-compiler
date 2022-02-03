package manticore.assembly.levels

import manticore.assembly.ManticoreAssemblyIR
import manticore.compiler.AssemblyContext
import manticore.assembly.annotations.{Reg => RegAnnotation}
import manticore.assembly.annotations.AssemblyAnnotation
import manticore.assembly.annotations.StringValue
import manticore.assembly.annotations.AssemblyAnnotationFields
import java.util.function.BinaryOperator


/**
  * Close sequential cycles to persist register values across virtual cycles
  * @author
  *   Mahyar Emami   <mahyar.emami@eplf.ch>
  */
trait CloseSequentialCycles extends InputOutputPairs {

  import flavor._

  private def do_transform(
      proc: DefProcess
  )(implicit ctx: AssemblyContext): DefProcess = {

    val move_instructions = createInputOutputPairs(proc).map {
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
