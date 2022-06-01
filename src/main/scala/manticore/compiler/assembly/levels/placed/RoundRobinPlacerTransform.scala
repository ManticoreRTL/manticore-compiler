package manticore.compiler.assembly.levels.placed

import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext

/** Adhoc placer
  * @author
  *   Mahyar Emami <mahyar.emami@eplf.ch>
  */
object RoundRobinPlacerTransform extends PlacedIRTransformer {
  import PlacedIR._
  override def transform(
      program: DefProgram
  )(implicit ctx: AssemblyContext): DefProgram = {
    val places = Range(0, ctx.max_dimx).flatMap { x =>
      Range(0, ctx.max_dimy).map { y => (x, y) }
    }

    val sortedProcess = program.processes
      .sortBy { p =>
        p.body.count {
          case _ @(_: Expect | _: Interrupt | _: GlobalLoad | _: GlobalStore |
              _: PutSerial) =>
            true
          case _ => false
        }
      } {
        Ordering[Int].reverse
      }
    val newProcessIds: Map[ProcessId, ProcessId] = sortedProcess
      .zip(places)
      .map { case (proc, (x, y)) =>
        proc.id -> ProcessIdImpl(s"placed_X${x}_Y${y}", x, y)
      }
      .toMap

    val placed = sortedProcess.map { process =>

      val renamed =process.body.map {
        case send: Send => send.copy(dest_id = newProcessIds(send.dest_id)).setPos(send.pos)
        case other => other
      }
      process.copy(
        id = newProcessIds(process.id),
        body = renamed
      ).setPos(process.pos)
    }
    program
      .copy(
        processes = placed
      )
      .setPos(program.pos)

  }
}
