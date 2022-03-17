package manticore.compiler.assembly.levels

import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.annotations.{Reg => RegAnnotation}
import manticore.compiler.assembly.annotations.AssemblyAnnotationFields



/**
  * Helper functional trait for building passes in which input and output
  * pairs need to be "closed". See [[manticore.assembly.levels.CloseSequentialCycles]]
  * for an example.
  * @author
  *   Mahyar Emami   <mahyar.emami@eplf.ch>
  */
trait InputOutputPairs extends Flavored {

  val flavor: ManticoreAssemblyIR
  import flavor._
  def createInputOutputPairs(
      proc: DefProcess
  )(implicit ctx: AssemblyContext): Seq[(DefReg, DefReg)] = {
    val user_regs = proc.registers.collect {
      case r: DefReg if r.findAnnotation(RegAnnotation.name).nonEmpty => r
    }

    val ids_to_current_regs = user_regs
      .filter { r =>
        val reg_annon = r.findAnnotation(RegAnnotation.name)
        reg_annon match {
          case Some(reg) =>
            reg.get(AssemblyAnnotationFields.Type).get == RegAnnotation.Current
          case _ =>
            false
        }

      }
      .map { r =>
        val reg_annon = r.findAnnotation(RegAnnotation.name).get
        reg_annon.getStringValue(AssemblyAnnotationFields.Id) match {
          case Some(id) =>
            (
              id,
              reg_annon.getIntValue(AssemblyAnnotationFields.Index).getOrElse(0)
            ) -> r
          case _ =>
            // this should never happen because of how we parse annotation, but
            // left here for good measure
            ctx.logger.error("Invalid @REG annotation, missing id!", r)
            ctx.logger.fail("Failed creating user register map")
        }
      }
      .toMap
    val currs_to_next_pairs =
      scala.collection.mutable.Queue.empty[(DefReg, DefReg)]
    val next_regs = user_regs.foreach { r =>
      val reg_annon = r.findAnnotation(RegAnnotation.name)
      reg_annon match {
        case Some(annon) =>
          annon.get(AssemblyAnnotationFields.Type) match {
            case Some(RegAnnotation.Next) =>
              annon.getStringValue(AssemblyAnnotationFields.Id) match {
                case Some(id) =>
                  val index = annon
                    .getIntValue(AssemblyAnnotationFields.Index)
                    .getOrElse(0)
                  ids_to_current_regs.get((id, index)) match {
                    case Some(curr_v) =>
                      currs_to_next_pairs += (curr_v -> r)
                    case _ =>
                      ctx.logger.warn("Register has no current value!", r)
                  }
                case _ =>
                  ctx.logger.error("@REG annotation is missing id", r)
              }
            case _ =>
            // do nothing, the reg type is RegAnnotation.Current
          }
        case None => // do nothing
      }
    }
    currs_to_next_pairs.toSeq
  }
}
