package manticore.compiler.assembly.levels.placed.lowering

import manticore.compiler.assembly.levels.placed.PlacedIRTransformer

import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.AssemblyContext
import scala.collection.parallel.CollectionConverters._

/** Inserts Predicate instruction before stores and sets the store instruction
  * predicate field to [[None]].
  * @author
  *   Mahyar Emami <mahyar.emami@eplf.ch>
  */
private[lowering] object PredicateInsertionTransform
    extends PlacedIRTransformer {

  import PlacedIR._
  import scala.collection.mutable.{Queue => MutableQueue}
  def insertPredicates(
      process: DefProcess,
      ctx: AssemblyContext
  ): DefProcess = {

    val newSchedule = MutableQueue.empty[Instruction]

    /** Go through the scheduled instructions, then before each store
      * instruction insert a predicate if the correct predicate value is not set
      */
    process.body.foldLeft(Option.empty[Name]) { case (activePredicate, inst) =>
      def schedulePredicate(p: Name): Option[Name] = {
        if (!activePredicate.contains(p)) {
          newSchedule.enqueue(Predicate(p))
        } else {
          // lucky, some previous store uses the same predicate
        }
        Some(p)
      }
      val (p, toSched) = inst match { // get the new predicate Name
        case store @ LocalStore(rs, b, offset, Some(p), _, _) =>
          (schedulePredicate(p), store.copy(predicate = None))
        case store @ GlobalStore(_, _, Some(p), _) =>
          (schedulePredicate(p), store.copy(predicate = None))
        case other @ _ => (activePredicate, other)
      }
      newSchedule.enqueue(toSched.setPos(inst.pos))
      p
    }

    process.copy(
      body = newSchedule.toSeq
    )

  }

  override def transform(program: DefProgram)(implicit
      context: AssemblyContext
  ): DefProgram = {

    program.copy(
      processes = program.processes.par.map { p =>
        insertPredicates(p, context)
      }.seq
    )

  }

}
