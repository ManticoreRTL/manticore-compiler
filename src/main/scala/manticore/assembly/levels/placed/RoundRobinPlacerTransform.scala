package manticore.assembly.levels.placed

import manticore.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext

object RoundRobinPlacerTransform extends AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {
    import PlacedIR._
    override def transform(program: DefProgram, ctx: AssemblyContext): DefProgram = {
        val places = Range(0, ctx.max_dimx).flatMap { x => Range(0, ctx.max_dimy).map { y => (x, y)}}
        val placed_processes = program.processes.zip(places).map { case (proc, (x, y)) =>
            proc.copy(
                id = ProcessIdImpl(s"placed_X${x}_Y${y}", x, y)
            ).setPos(proc.pos)
        }
        program.copy(
            processes = placed_processes
        ).setPos(program.pos)

    }
}