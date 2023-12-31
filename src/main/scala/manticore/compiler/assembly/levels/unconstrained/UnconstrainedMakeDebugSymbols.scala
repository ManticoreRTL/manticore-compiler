package manticore.compiler.assembly.levels.unconstrained

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.annotations.DebugSymbol
import manticore.compiler.assembly.annotations.AssemblyAnnotation
import manticore.compiler.assembly.annotations.AssemblyAnnotationFields
import manticore.compiler.assembly.levels.ConstType


/**
  * A pass to run immediately after parsing to create debug symbols
  * @author
  *   Mahyar Emami   <mahyar.emami@eplf.ch>
  */
object UnconstrainedMakeDebugSymbols
    extends UnconstrainedIRTransformer {

  import UnconstrainedIR._

  private def transform(proc: DefProcess)(implicit ctx: AssemblyContext): DefProcess = {

    val regs = proc.registers.map { r =>
      r.variable.varType match {
        case ConstType => r
        case _ =>
          val oldsym: Option[DebugSymbol] = r.annons.collectFirst {
            case x: DebugSymbol => x
          }

          val new_sym: DebugSymbol = oldsym match {
            case Some(annon) =>
              // check that the DebugSymbol is not malformed
              annon.getIntValue(AssemblyAnnotationFields.Index) match {
                case Some(i) if (i != 0) =>
                  ctx.logger.error(
                    "invalid debug symbol, expected index to be 0",
                    r
                  )
                case _ => // OK
              }

              val with_index = annon.withIndex(0)
              val with_width =
                with_index.get(AssemblyAnnotationFields.Width) match {
                  case Some(w) => with_index
                  case None    => annon.withWidth(r.variable.width)
                }
              with_width.withGenerated(false)
            case None =>
              DebugSymbol(r.variable.name)
                .withIndex(0)
                .withWidth(r.variable.width)
                .withGenerated(true)
          }
          val other_annons = r.annons.filter {
            case _: DebugSymbol => false
            case _              => true
          }

          r.copy(annons = other_annons :+ new_sym).setPos(r.pos)
      }

    }

    proc.copy(registers = regs).setPos(proc.pos)
  }
  override def transform(
      source: DefProgram,
  )(implicit ctx: AssemblyContext): DefProgram = {

    source
      .copy(processes = source.processes.map { transform })
      .setPos(source.pos)
  }
}
