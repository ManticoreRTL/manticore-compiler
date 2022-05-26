package manticore.compiler.assembly.levels.placed

import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.AssemblyContext

import manticore.compiler.assembly.annotations.{Reg => RegAnnotation}
import manticore.compiler.assembly.levels.InputType
import manticore.compiler.assembly.levels.OutputType

/** Insert send instructions between processes, should be executed once after
  * the processes are merged.
  * @author
  *   Mahyar Emami <mahyar.emami@eplf.ch>
  */
object SendInsertionTransform extends PlacedIRTransformer {

  import PlacedIR._

  override def transform(
      program: DefProgram
  )(implicit context: AssemblyContext): DefProgram = {

    // to insert send instructions, we first need to create a mapping from
    // output registers---that are produced only in a single process---to
    // input registers that are possibly in other processes.

    case class RegUID(id: String, index: Option[Int])
    case class SendDest(r: DefReg, p: DefProcess)
    object RegUID {
      def of(reg: DefReg): Option[RegUID] =
        reg.annons.collectFirst { case ra: RegAnnotation =>
          RegUID(ra.getId(), ra.getIndex())
        }
    }
    val current_registers = scala.collection.mutable.Map
      .empty[RegUID, scala.collection.mutable.Queue[SendDest]]

    def appendCurrents(proc: DefProcess): Unit = {

      proc.registers.foreach { r =>
        if (r.variable.varType == InputType) {
          RegUID.of(r) match {
            case Some(uid) =>
              current_registers.get(uid) match {
                case Some(q) => q += SendDest(r, proc)
                case None =>
                  current_registers += uid -> scala.collection.mutable
                    .Queue[SendDest](SendDest(r, proc))
              }
            case None =>
              context.logger.error(
                s"Input register is missing a valid @${RegAnnotation.name}",
                r
              )
          }
        } else {
          // not a input register
        }
      }
    }
    program.processes.foreach(appendCurrents)

    def withSend(proc: DefProcess): DefProcess = {

      val sends: Seq[Send] = proc.registers
        .filter { r => r.variable.varType == OutputType }
        .flatMap { r =>
          val uid_opt = RegUID.of(r)
          uid_opt match {
            case Some(uid) =>
              // get the input registers that need this output register
              current_registers.get(uid) match {
                case Some(dest_queue) =>
                  dest_queue
                    .filter { dest => dest.p != proc }
                    .map { case SendDest(dest_reg, dest_proc) =>
                      Send(
                        dest_reg.variable.name,
                        r.variable.name,
                        dest_proc.id
                      )
                    }
                    .toSeq
                case None =>
                  context.logger.warn(s"Output register is never used", r)
                  Seq.empty[Send]
              }
            case None =>
              context.logger.error(
                s"Output register is missing a valid @${RegAnnotation.name}",
                r
              )
              Seq.empty[Send]
          }
        }
      proc
        .copy(
          body = proc.body ++ sends
        )
        .setPos(proc.pos)
    }

    program
      .copy(
        processes = program.processes.map(withSend)
      )
      .setPos(program.pos)
  }
}
