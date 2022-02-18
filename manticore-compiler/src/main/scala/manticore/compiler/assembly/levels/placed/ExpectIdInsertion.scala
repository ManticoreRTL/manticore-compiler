package manticore.compiler.assembly.levels.placed

import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.UInt16

object ExpectIdInsertion
    extends AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {

  import PlacedIR._

  override def transform(
      program: DefProgram,
      context: AssemblyContext
  ): DefProgram = {

    var failure_id_base = 0x8000
    var stop_id_base = 0x0000

    val transformed = program.processes.map { proc =>
      val transformed_body = scala.collection.mutable.Queue.empty[Instruction]
      proc.body.foreach {
        case i @ Expect(ref, got, error_id, annons) =>
          error_id match {
            case e @ ExceptionIdImpl(_, _, ExpectFail) =>
              transformed_body += i
                .copy(error_id = e.copy(id = UInt16(failure_id_base)))
                .setPos(i.pos)
            case e @ ExceptionIdImpl(_, _, ExpectStop) =>
              transformed_body += i
                .copy(error_id = e.copy(id = UInt16(stop_id_base)))
                .setPos(i.pos)
          }
        case i @ _ => transformed_body += i.setPos(i.pos)
      }
      proc.copy(body = transformed_body.toSeq).setPos(proc.pos)
    }

    program.copy(processes = transformed).setPos(program.pos)

  }

}
