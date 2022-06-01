package manticore.compiler.assembly.levels.placed

import manticore.compiler.assembly.levels.AssemblyTransformer

import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.AssemblyContext
import scala.collection.parallel.CollectionConverters._

/** Inserts Predicate instruction before stores and sets the store instruction
  * predicate field to [[None]].
  * @author
  *   Mahyar Emami <mahyar.emami@eplf.ch>
  */
@deprecated("This transformation will be removed")
object PredicateInsertionTransform extends PlacedIRTransformer {

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
        case store @ LocalStore(rs, b, offset, Some(p), _, a) =>
          (schedulePredicate(p), store.copy(predicate = None))
        case store @ GlobalStore(_, _, Some(p), _) =>
          (schedulePredicate(p), store.copy(predicate = None))
        case other @ _ => (active_predicate, other)
      }
      new_schedule.enqueue(to_sched.setPos(inst.pos))
      p
    }

    process.copy(
      body = new_schedule.toSeq
    )

  }

  override def transform(
      program: DefProgram
  )(implicit context: AssemblyContext): DefProgram = {

    program.copy(
      processes = program.processes.par.map { p =>
        insertPredicates(p, context)
      }.seq
    )

  }

}
