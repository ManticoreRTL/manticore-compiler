package manticore.assembly.levels

import manticore.assembly.ManticoreAssemblyIR
import manticore.compiler.AssemblyContext
import manticore.assembly.annotations.{Reg => RegAnnotation}
import manticore.assembly.annotations.AssemblyAnnotation
import manticore.assembly.annotations.StringValue
import manticore.assembly.annotations.AssemblyAnnotationFields
import java.util.function.BinaryOperator

trait CloseSequentialCycles extends Flavored {

  import flavor._

  private def do_transform(proc: DefProcess)(implicit ctx: AssemblyContext): DefProcess = {

    val user_regs = proc.registers.collect{
        case r: DefReg if r.findAnnotation(RegAnnotation.name).nonEmpty => r
    }


    val ids_to_current_regs = user_regs.filter {r =>
        val reg_annon = r.findAnnotation(RegAnnotation.name)
        reg_annon match {
            case Some(reg) =>
                reg.get(AssemblyAnnotationFields.Type).get == RegAnnotation.Current
            case _ =>
                false
        }

    }.map { r =>
        val reg_annon = r.findAnnotation(RegAnnotation.name).get
        reg_annon.getStringValue(AssemblyAnnotationFields.Id) match {
            case Some(id) =>
                (id, reg_annon.getIntValue(AssemblyAnnotationFields.Index).getOrElse(0)) -> r
            case _ =>
                // this should never happen because of how we parse annotation, but
                // left here for good measure
                ctx.logger.error("Invalid @REG annotation, missing id!", r)
                ctx.logger.fail("Failed creating user register map")
        }
    }.toMap

    val move_instructions = scala.collection.mutable.Queue.empty[Instruction]
    val next_regs = user_regs.foreach { r =>
        val reg_annon = r.findAnnotation(RegAnnotation.name)
        reg_annon match {
            case Some(annon) =>
                annon.get(AssemblyAnnotationFields.Type) match {
                    case Some(RegAnnotation.Next) =>
                        annon.getStringValue(AssemblyAnnotationFields.Id) match {
                            case Some(id) =>
                                val index = annon.getIntValue(AssemblyAnnotationFields.Index).getOrElse(0)
                                ids_to_current_regs.get((id, index)) match {
                                    case Some(curr_v) =>
                                        move_instructions += Mov(
                                            curr_v.variable.name,
                                            r.variable.name,
                                        )
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

    proc.copy(body = proc.body ++ move_instructions.toSeq)

  }
  def do_transform(source: DefProgram)(implicit ctx: AssemblyContext): DefProgram =
    source.copy(
        processes =  source.processes.map(do_transform)
    )

}
