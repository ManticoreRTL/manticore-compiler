package manticore.compiler.assembly.levels.placed

import manticore.compiler.assembly.levels.AssemblyChecker
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.DependenceGraphBuilder
import manticore.compiler.assembly.levels.InputType
import manticore.compiler.assembly.levels.MemoryType
import manticore.compiler.assembly.levels.ConstType
import manticore.compiler.HasLoggerId

object ScheduleChecker
    extends DependenceGraphBuilder
    with AssemblyChecker[PlacedIR.DefProgram] {
  val flavor = PlacedIR

  import flavor._

  override def check(
      program: PlacedIR.DefProgram,
      context: AssemblyContext
  ): Unit = {
    program.processes.map(check(_)(context))
  }

  def check(proc: PlacedIR.DefProcess)(implicit ctx: AssemblyContext): Unit = {

    val defined_names = scala.collection.mutable.Map.empty[Name, Int] ++
      proc.registers.collect {
        case r: DefReg
            if r.variable.varType == InputType ||
              r.variable.varType == MemoryType ||
              r.variable.varType == ConstType =>
          r.variable.name -> 0
      }

    // Inputs, constants, and base pointer are already defined at cycle 0
    val logger_id = new HasLoggerId { val id = phase_id.id + s": ${proc.id}" }

    var expect_stop_reached = false
    proc.body.zipWithIndex.foreach { case (inst, cycle) =>
      // ensure EXPECTS are ordered correctly
      inst match {
        case Expect(_, _, ExceptionIdImpl(_, _, ExpectFail), _) =>
          if (expect_stop_reached) {
            ctx.logger.error("invalid EXPECT ordering!")
          }
        case Expect(_, _, ExceptionIdImpl(_, _, ExpectStop), _) =>
          expect_stop_reached = true
        case _ => // nothing to do
      }
      DependenceAnalysis.regUses(inst).foreach { u =>
        defined_names.get(u) match {
          case Some(def_cycle) =>
            if (cycle < def_cycle) {
              ctx.logger.error(
                s"register ${u} is not yet written back at cycle ${cycle}, " +
                  s"the earliest time it can be used is ${def_cycle + 1}",
                inst
              )(logger_id)
            } else {
              // things are ok
            }
          case None =>
            ctx.logger.error(
              s"register ${u} is being used at cycle ${cycle} but has not been defined!",
              inst
            )(logger_id)
        }
      }

      DependenceAnalysis.regDef(inst).foreach { d =>
        defined_names.get(d) match {
          case Some(def_cycle) =>
            if (def_cycle != 0) {
              ctx.logger.error(
                s"register ${d} is redefined at cycle ${cycle}, first define at cycle ${def_cycle}"
              )(logger_id)
            }
          case None =>
          // nothing
        }
        defined_names += d -> (cycle + LatencyAnalysis.latency(inst) + 1)
      }
    }

  }
}
