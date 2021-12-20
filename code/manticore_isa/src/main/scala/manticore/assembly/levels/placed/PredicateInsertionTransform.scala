package manticore.assembly.levels.placed

import manticore.assembly.levels.AssemblyTransformer

import manticore.assembly.levels.placed.PlacedIR
import manticore.compiler.AssemblyContext
import scala.collection.parallel.CollectionConverters._

object PredicateInsertionTransform
    extends AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {

  import PlacedIR._
  import scala.collection.mutable.{Queue => MutableQueue}
  def insertPredicates(
      process: DefProcess,
      ctx: AssemblyContext
  ): DefProcess = {

    val new_schedule = MutableQueue.empty[Instruction]

    /** Go through the scheduled instructions, then before each store
      * instruction insert a predicate if the correct predicate value is not set
      */
    process.body.foldLeft(Option.empty[Name]) { case (active_predicate, inst) =>
      def schedulePredicate(p: Name): Option[Name] = {
        if (!active_predicate.contains(p)) {
          new_schedule.enqueue(Predicate(p))
        } else {
          // lucky, some previous store uses the same predicate
        }
        Some(p)
      }
      val (p, to_sched) = inst match { // get the new predicate Name
        case store @ LocalStore(rs, b, offset, Some(p), a) =>
          (schedulePredicate(p), store.copy(predicate = None))
        case store @ GlobalStore(_, _, Some(p), _) =>
          (schedulePredicate(p), store.copy(predicate = None))
        case other @ _ => (None, other)
      }
      new_schedule.enqueue(to_sched.setPos(inst.pos))
      p
    }

    process.copy(
      body = new_schedule.toSeq
    )

  }

  override def transform(
      program: DefProgram,
      context: AssemblyContext
  ): DefProgram = {

    program.copy(
      processes = program.processes.par.map { p =>
        insertPredicates(p, context)
      }.seq
    )

  }

}
