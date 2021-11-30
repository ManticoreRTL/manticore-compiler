package manticore.assembly

/** DeadCodeElimination.scala
  *
  * @author
  *   Sahand Kashani <sahand.kashani@epfl.ch>
  */

import manticore.assembly.levels.AssemblyTransformer
import manticore.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.AssemblyContext

/** This transform identifies dead code and removes it from the design. Dead
  * code consists of names that are never referenced once written. Note that we
  * replace dead code with `Nop` instructions here. A scheduling pass needs to
  * be run later to try and eliminate the `Nop`s while ensuring that program
  * execution is correct.
  */
object DeadCodeElimination
    extends AssemblyTransformer(UnconstrainedIR, UnconstrainedIR) {

  import UnconstrainedIR._

  def deadCodeElimination(
      asm: DefProcess
  ): DefProcess = {

    def countReferences(
        body: Seq[Instruction],
        refCount: Map[Name, Int] = Map.empty.withDefaultValue(0)
    ): Map[Name, Int] = {
      body match {
        case Nil =>
          refCount

        case head :: tail =>
          val localRefCounts =
            collection.mutable.Map[Name, Int]().withDefaultValue(0)
          def incr(name: Name): Unit = localRefCounts(name) += 1

          head match {
            case BinaryArithmetic(operator, rd, rs1, rs2, annons) =>
              incr(rs1)
              incr(rs2)

            case CustomInstruction(func, rd, rs1, rs2, rs3, rs4, annons) =>
              incr(rs1)
              incr(rs2)
              incr(rs3)
              incr(rs4)

            case LocalLoad(rd, base, offset, annons) =>
              incr(base)

            case LocalStore(rs, base, offset, predicate, annons) =>
              incr(base)
              predicate.foreach(incr)

            case GlobalLoad(rd, base, annons) =>
              incr(base._1)
              incr(base._2)
              incr(base._3)

            case GlobalStore(rs, base, predicate, annons) =>
              incr(base._1)
              incr(base._2)
              incr(base._3)
              predicate.foreach(incr)

            case SetValue(rd, value, annons) =>

            case Send(rd, rs, dest_id, annons) =>
              // The rd register, although READ to generate the destination address in *another* process, cannot impose
              // any form of dependency.
              incr(rs)

            case Expect(ref, got, error_id, annons) =>
              incr(ref)
              incr(got)

            case Predicate(rs, annons) =>
              incr(rs)

            case Mux(rd, sel, rs1, rs2, annons) =>
              incr(sel)
              incr(rs1)
              incr(rs2)

            case Nop =>
          }

          // Update the reference counts with the locally-computed ones Be sure to increment the existing count if it
          // already existed!
          val newRefCount = refCount ++ localRefCounts.map { case (name, cnt) =>
            name -> (cnt + refCount.getOrElse(name, 0))
          }

          countReferences(tail, newRefCount)
      }
    }

    val trackAnnoName = "TRACK"
    val regAnnoName = "REG"
    val regsAndTrackedNames = asm.registers
      .filter { reg =>
        reg.annons.exists { anno =>
          Seq(trackAnnoName, regAnnoName).contains(anno.name)
        }
      }

    // Count all source operands (what "countReferences(asm.body)" represents), but also ensure the
    // names of some signals that may never be read (but that must be preserved) have a positive reference
    // count to avoid pruning them from the circuit. Names to be preserved are tracked signals and
    // all registers. Note that the "_curr" port of a register is always read, but the "_next" is never
    // read, hence why we must force-preserve it.
    val refCount = countReferences(asm.body) ++ regsAndTrackedNames
      .map(reg => reg.variable.name -> 1)
      .toMap

    val newRegs = asm.registers
      .filter { r =>
        // Only keep registers/constants that are referenced at least once.
        refCount.getOrElse(r.variable.name, 0) > 0
      }

    val newBody = asm.body
      .flatMap { instr =>
        val skip = instr match {
          case BinaryArithmetic(operator, rd, rs1, rs2, annons) =>
            refCount(rd) == 0

          case CustomInstruction(func, rd, rs1, rs2, rs3, rs4, annons) =>
            refCount(rd) == 0

          case LocalLoad(rd, base, offset, annons) =>
            refCount(rd) == 0

          case LocalStore(rs, base, offset, predicate, annons) =>
            // Do not skip stores. The predicate tells if it is enabled or not.
            false

          case GlobalLoad(rd, base, annons) =>
            refCount(rd) == 0

          case GlobalStore(rs, base, predicate, annons) =>
            // Do not skip stores. The predicate tells if it is enabled or not.
            false

          case SetValue(rd, value, annons) =>
            refCount(rd) == 0

          case Send(rd, rs, dest_id, annons) =>
            // Do not skip Sends as rd is exiting the process. We therefore don't know who is reading it,
            // but another process is certainly waiting on its value.
            false

          case Expect(ref, got, error_id, annons) =>
            // Do not skip. Used for verification and must always be executed.
            false

          case Predicate(rs, annons) =>
            // Do not skip predicate instructions. They read a register from the register file and
            // set an on-chip reg close to the datapath.
            false

          case Mux(rd, sel, rs1, rs2, annons) =>
            refCount(rd) == 0

          case Nop =>
            // This is unscheduled code. It is safe to skip `Nop`s.
            true
        }

        if (skip) {
          None
        } else {
          Some(instr)
        }
      }

    asm.copy(
      registers = newRegs,
      body = newBody
    )
  }

  override def transform(
      asm: DefProgram,
      context: AssemblyContext
  ): DefProgram = {
    implicit val ctx = context

    val out = DefProgram(
      processes = asm.processes.map(process => deadCodeElimination(process)),
      annons = asm.annons
    )

    if (logger.countErrors > 0) {
      logger.fail(s"Failed transform due to previous errors!")
    }

    out
  }
}
