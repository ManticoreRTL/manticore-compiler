package manticore.compiler.assembly.levels

import manticore.compiler.assembly.ManticoreAssemblyIR

import scala.annotation.tailrec

import manticore.compiler.AssemblyContext

/** A generic name checker that makes sure all the used names in the program are
  * defined. This class should be specialized as an object before being used,
  * see [[manticore.assembly.levels.unconstrained.UnconstrainedNameChecker]] for
  * an example.
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  *
  * @param irFlavor
  *   IR flavor object to specialize the checker with
  */
trait AssemblyNameChecker extends Flavored {

  import flavor._

  object NameCheck {

    /** Collect any undefined registers in the process
      *
      * @param process
      * @return
      */
    def collectUndefinedRegRefs(
        process: DefProcess
    ): Seq[(Name, Instruction)] = {

      val isDefinedReg: Name => Boolean = process.registers.map { r =>
        r.variable.name
      }.toSet

      def checkBlock(
          undefs: Seq[(Name, Instruction)],
          block: Seq[Instruction]
      ): Seq[(Name, Instruction)] = {
        block.foldLeft(undefs) { case (acc, inst) =>
          inst match {
            case Expect(ref, got, error_id, annons) =>
              acc ++ Seq(ref, got).collect {
                case n if !isDefinedReg(n) => (n -> inst)
              }
            case GlobalLoad(rd, base, annons) =>
              acc ++ Seq(rd, base._1, base._2, base._3).collect {
                case n if !isDefinedReg(n) => (n -> inst)
              }
            case GlobalStore(rs, base, predicate, annons) =>
              acc ++ (Seq(rs, base._1, base._2, base._3) ++ predicate.toSeq)
                .collect { case n if !isDefinedReg(n) => (n -> inst) }
            case JumpTable(target, results, blocks, dslot, annons) =>
              val defTarget = isDefinedReg(target) match {
                case true  => Seq.empty
                case false => Seq((target -> inst))
              }
              (blocks.map(_.block) :+ dslot).foldLeft(acc ++ defTarget) {
                case (prev, blkc) =>
                  checkBlock(prev, blkc)
              }

            case Lookup(rd, index, base, annons) =>
              acc ++ Seq(rd, index, base).collect {
                case n if !isDefinedReg(n) => (n -> inst)
              }
            case ParMux(rd, choices, default, annons) =>
              acc ++ (
                Seq(rd, default) ++ choices
                  .flatMap { case ParMuxCase(c, rs) => Seq(c, rs) }
              ).collect { case n if !isDefinedReg(n) => (n -> inst) }

            case Nop =>
              acc
            case Recv(rd, rs, source_id, annons) =>
              if (isDefinedReg(rd)) {
                acc
              } else {
                acc :+ ((rd -> inst))
              }
            case Send(rd, rs, dest_id, annons) =>
              if (isDefinedReg(rs)) {
                acc
              } else {
                acc :+ ((rs -> inst))
              }
            case CustomInstruction(func, rd, rs1, rs2, rs3, rs4, annons) =>
              acc ++ Seq(rd, rs1, rs2, rs3, rs4).collect {
                case n if !isDefinedReg(n) => (n -> inst)
              }
            case Mux(rd, sel, rfalse, rtrue, annons) =>
              acc ++ Seq(rd, sel, rfalse, rtrue).collect {
                case n if !isDefinedReg(n) => (n -> inst)
              }
            case SetValue(rd, value, annons) =>
              if (isDefinedReg(rd)) {
                acc
              } else {
                acc :+ ((rd -> inst))
              }
            case LocalLoad(rd, base, offset, annons) =>
              acc ++ Seq(rd, base).collect {
                case n if !isDefinedReg(n) => (n -> inst)
              }
            case AddC(rd, co, rs1, rs2, ci, annons) =>
              acc ++ Seq(rd, co, rs1, rs2, ci).collect {
                case n if !isDefinedReg(n) => (n -> inst)
              }
            case LocalStore(rs, base, offset, predicate, annons) =>
              acc ++ (Seq(rs, base) ++ predicate.toSeq).collect {
                case n if !isDefinedReg(n) => (n -> inst)
              }
            case ClearCarry(carry, annons) =>
              if (isDefinedReg(carry)) {
                acc
              } else {
                acc :+ ((carry -> inst))
              }
            case Mov(rd, rs, annons) =>
              acc ++ Seq(rd, rs).collect {
                case n if !isDefinedReg(n) => (n -> inst)
              }
            case Predicate(rs, annons) =>
              if (isDefinedReg(rs)) {
                acc
              } else {
                acc :+ ((rs -> inst))
              }
            case BinaryArithmetic(operator, rd, rs1, rs2, annons) =>
              acc ++ Seq(rd, rs1, rs2).collect {
                case n if !isDefinedReg(n) => (n -> inst)
              }
            case PadZero(rd, rs, width, annons) =>
              acc ++ Seq(rd, rs).collect {
                case n if !isDefinedReg(n) => (n -> inst)
              }
            case SetCarry(carry, annons) =>
              if (isDefinedReg(carry)) {
                acc
              } else {
                acc :+ ((carry -> inst))
              }
          }

        }

      }

      checkBlock(Seq.empty, process.body)
    }

    /** Collect any undefined labels in the process
      *
      * @param process
      * @return
      */

    def collectUndefinedLabels(process: DefProcess): Seq[(Label, JumpTable)] = {
      val isDefinedLabel: Label => Boolean = process.labels.flatMap { lgrp =>
        (lgrp.default.toSeq ++ lgrp.indexer.map(_._2))
      }.toSet
      def checkJumpTables(
          undefs: Seq[(Label, JumpTable)],
          block: Seq[Instruction]
      ): Seq[(Label, JumpTable)] = block.foldLeft(undefs) {
        case (acc, i @ JumpTable(_, _, blocks, _, _)) =>
          blocks.foldLeft(acc) { case (prev, JumpCase(lbl, blk)) =>
            if (!isDefinedLabel(lbl)) {
              checkJumpTables(prev :+ (lbl -> i), blk)
            } else {
              checkJumpTables(prev, blk)
            }
          }
        case (acc, _) => acc

      }
      checkJumpTables(Seq.empty, process.body)
    }

    case class NonSSA(rd: Name, assigns: Seq[Instruction]) {
      def :+(inst: Instruction) = copy(assigns = assigns :+ inst)
    }

    /** Collect non SSA assignments, i.e., collect any name that has been
      * assigned multiple times
      *
      * @param process
      * @return
      */
    def collectNonSSA(process: DefProcess): Iterable[NonSSA] = {
      def checkSSA(
          dirty: Map[Name, Seq[Instruction]],
          block: Seq[Instruction]
      ): Map[Name, Seq[Instruction]] =
        block.foldLeft(dirty) { case (assigns, inst) =>
          inst match {
            case GlobalLoad(rd, _, _) =>
              assigns + (rd -> (assigns(rd) :+ inst))
            case JumpTable(_, results, blocks, dslot, _) =>
              checkSSA(
                assigns ++ results.map { case Phi(rd, _) =>
                  (rd -> (assigns(rd) :+ inst))
                },
                blocks.flatMap(_.block) ++ dslot
              )
            case Lookup(rd, _, _, _) =>
              assigns + (rd -> (assigns(rd) :+ inst))
            case ParMux(rd, choices, default, _) =>
              assigns + (rd -> (assigns(rd) :+ inst))

            case CustomInstruction(func, rd, rs1, rs2, rs3, rs4, _) =>
              assigns + (rd -> (assigns(rd) :+ inst))
            case AddC(rd, co, _, _, _, _) =>
              assigns + (rd -> (assigns(rd) :+ inst)) + (co -> (assigns(
                co
              ) :+ inst))
            case Mov(rd, _, _) =>
              assigns + (rd -> (assigns(rd) :+ inst))
            case BinaryArithmetic(_, rd, _, _, _) =>
              assigns + (rd -> (assigns(rd) :+ inst))
            case Mux(rd, sel, _, _, _) =>
              assigns + (rd -> (assigns(rd) :+ inst))
            case ClearCarry(rd, _) =>
              assigns + (rd -> (assigns(rd) :+ inst))
            case SetValue(rd, _, _) =>
              assigns + (rd -> (assigns(rd) :+ inst))
            case PadZero(rd, _, _, _) =>
              assigns + (rd -> (assigns(rd) :+ inst))
            case SetCarry(rd, _) =>
              assigns + (rd -> (assigns(rd) :+ inst))
            case LocalLoad(rd, _, _, _) =>
              assigns + (rd -> (assigns(rd) :+ inst))
            case Recv(rd, _, _, _) =>
              assigns + (rd -> (assigns(rd) :+ inst))
            case i @ (_: LocalStore | _: Predicate | Nop | _: Send | _: Expect |
                _: GlobalStore) =>
              assigns
          }
        }
      checkSSA(
        Map.empty[Name, Seq[Instruction]].withDefaultValue(Seq.empty),
        process.body
      ).collect {
        case (name, assigns) if (assigns.length > 1) =>
          NonSSA(name, assigns)
      }
    }

    /** Check all register names have a DefReg
      *
      * @param process
      * @param notifier
      */
    def checkNames(
        process: DefProcess
    )(notifier: (Name, Instruction) => Unit): Unit = {
      val undefinedNames = collectUndefinedRegRefs(process)
      undefinedNames.foreach { case (n, inst) => notifier(n, inst) }
    }

    /** Check all labels in the process are defined
      *
      * @param process
      * @param notifier
      */
    def checkLabels(
        process: DefProcess
    )(notifier: (Label, Instruction) => Unit): Unit = {
      val undefinedLabels = collectUndefinedLabels(process)
      undefinedLabels.foreach { case (l, inst) => notifier(l, inst) }
    }

    /** Check that the process is in SSA form
      *
      * @param process
      * @param notifier
      */
    def checkSSA(process: DefProcess)(notifier: NonSSA => Unit): Unit = {
      val nonSSA = collectNonSSA(process)
      nonSSA.foreach { notifier }
    }

    /** Check cross process names, make sure all send messages have valid
      * destinations and target registers
      * @param prog
      * @param selfDest
      * @param badDest
      * @param badRegister
      */
    def checkSends(prog: DefProgram)(
        selfDest: Send => Unit,
        badDest: Send => Unit,
        badRegister: Send => Unit
    ): Unit = {

      /** Check cross process names, make sure all send messages have valid
        * destinations
        */
      val procIds = prog.processes.map { _.id }

      val procMap = prog.processes.map { p => p.id -> p }.toMap
      prog.processes.foreach { p =>
        p.body
          .filter { _.isInstanceOf[Send] }
          .map(_.asInstanceOf[Send])
          .foreach { case inst @ Send(rd, _, dest, _) =>
            if (dest == p.id) { selfDest(inst) }
            if (!procIds.contains(dest)) {
              badDest(inst)
            } else {
              // check if dest reg is defined
              if (
                !procMap(dest).registers
                  .map { r => r.variable.name }
                  .contains(rd)
              ) {
                badRegister(inst)
              }
            }
          }
      }
    }


  }
}
