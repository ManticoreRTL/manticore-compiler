package manticore.assembly.levels

import manticore.assembly.ManticoreAssemblyIR
import manticore.compiler.AssemblyContext
import manticore.assembly.annotations.{Reg => RegAnnotation}
import manticore.assembly.annotations.AssemblyAnnotation
import manticore.assembly.annotations.StringValue
import manticore.assembly.annotations.AssemblyAnnotationFields
import java.util.function.BinaryOperator

/**
  * Break sequential cycles (undo [[manticore.assembly.levels.CloseSequentialCycles]])
  * @author
  *   Mahyar Emami   <mahyar.emami@eplf.ch>
  */
trait BreakSequentialCycles extends Flavored {

  import flavor._

  private def do_transform(proc: DefProcess)(implicit ctx: AssemblyContext): DefProcess = {

    val user_regs = proc.registers.collect{
        case r: DefReg if r.findAnnotation(RegAnnotation.name).nonEmpty => r
    }

    case class RegType(t: StringValue)
    object CurrentType extends RegType(RegAnnotation.Current)
    object NextType extends RegType(RegAnnotation.Next)

    def createIdMap(t: RegType): Map[Name, (String, Int)] =
    user_regs.filter {r =>
        val reg_annon = r.findAnnotation(RegAnnotation.name)
        reg_annon match {
            case Some(reg) =>
                reg.get(AssemblyAnnotationFields.Type).get == t.t
            case _ =>
                false
        }

    }.map { r =>
        val reg_annon = r.findAnnotation(RegAnnotation.name).get
        reg_annon.getStringValue(AssemblyAnnotationFields.Id) match {
            case Some(id) =>
                r.variable.name -> (id, reg_annon.getIntValue(AssemblyAnnotationFields.Index).getOrElse(0))
            case _ =>
                // this should never happen because of how we parse annotation, but
                // left here for good measure
                ctx.logger.error("Invalid @REG annotation, missing id!", r)
                ctx.logger.fail("Failed creating user register map")
        }
    }.toMap


    val current_regs_to_ids = createIdMap(CurrentType)
    val next_regs_to_ids = createIdMap(NextType)

    val removed_moves = proc.body.filter {
        case i @ Mov(rd, rs, _) =>
            current_regs_to_ids.get(rd) match {
                case Some((index_d, id_d)) =>
                    next_regs_to_ids.get(rs) match {
                        case Some((index_s, id_s)) =>
                            if ((index_d, id_d) == (index_s, id_s))
                                false // MOV current, next; remove it
                            else
                                true
                        case _ =>
                            true
                    }
                case None =>
                    // the instruction is not a MOV current, next; so keep it.
                    true
            }
        case i @ _ => true
    }

    proc.copy(body = removed_moves)

  }
  def do_transform(source: DefProgram)(implicit ctx: AssemblyContext): DefProgram =
    source.copy(
        processes =  source.processes.map(do_transform)
    )

}
