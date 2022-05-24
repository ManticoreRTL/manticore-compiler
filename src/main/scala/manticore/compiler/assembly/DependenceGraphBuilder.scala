package manticore.compiler.assembly

/** DependenceGraph.scala
  *
  * @author
  *   Mahyar Emami <mahyar.emami@eplf.ch> Sahand Kashani
  *   <sahand.kashani@epfl.ch>
  */

import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.AssemblyContext
import scalax.collection.edge.LDiEdge
import manticore.compiler.assembly.levels.MemoryType
import scalax.collection.Graph
import scala.util.Try
import manticore.compiler.assembly.levels.Flavored
import manticore.compiler.assembly.annotations.Memblock
import scala.util.Success
import scala.util.Failure
import manticore.compiler.assembly.levels.CloseSequentialCycles
import manticore.compiler.assembly.levels.CanCollectInputOutputPairs

/** Generic dependence graph builder, to use it, mix this trait with your
  * transformation
  * @param flavor
  */
trait DependenceGraphBuilder extends CanCollectInputOutputPairs {

  val flavor: ManticoreAssemblyIR
  object DependenceAnalysis {
    import flavor._

    /** Extracts the registers read by the instruction.
      *
      * @param inst
      *   Target instruction.
      * @return
      *   Registers read by the instruction.
      */
    def regUses(
        inst: Instruction
    ): Seq[Name] = {
      inst match {
        case BinaryArithmetic(operator, rd, rs1, rs2, annons) =>
          Seq(rs1, rs2)
        case CustomInstruction(func, rd, rsx, annons) =>
          rsx
        case LocalLoad(rd, base, offset, order, annons) =>
          Seq(base, offset) :+ order.memory
        case LocalStore(rs, base, offset, predicate, order, annons) =>
          val pred = predicate match {
            case None      => Seq(order.memory)
            case Some(reg) => Seq(reg, order.memory)
          }
          Seq(rs, base, offset) ++ pred
        case GlobalLoad(rd, base, annons) =>
          Seq(base._1, base._2, base._3)
        case GlobalStore(rs, base, predicate, annons) =>
          val pred = predicate match {
            case None      => Seq.empty
            case Some(reg) => Seq(reg)
          }
          Seq(rs, base._1, base._2, base._3) ++ pred
        case Send(rd, rs, dest_id, annons) =>
          Seq(rs)
        case _: SetValue | _: ClearCarry | _: SetCarry =>
          Seq.empty
        case Mux(rd, sel, rs1, rs2, annons) =>
          Seq(sel, rs1, rs2)
        case Expect(ref, got, error_id, annons) =>
          Seq(ref, got)
        case Predicate(rs, annons) =>
          Seq(rs)
        case Nop =>
          Seq.empty
        case PadZero(rd, rs, width, annons) =>
          Seq(rs)
        case AddC(rd, co, rs1, rs2, ci, annons) =>
          Seq(rs1, rs2, ci)
        case Mov(rd, rs, _)                  => Seq(rs)
        case Slice(rd, rs, _, _, _)          => Seq(rs)
        case Recv(rd, rs, source_id, annons) =>
          // purely synthetic instruction, should be regarded as NOP
          Seq.empty
        case ParMux(rd, choices, default, annons) =>
          choices.flatMap { case ParMuxCase(cond, ch) =>
            Seq(cond, ch)
          } :+ default
        case Lookup(rd, index, base, annons) =>
          Seq(base, index)
        case JumpTable(target, phis, blocks, dslot, _) =>
          // assert(dslot.isEmpty, "dslot should only be used after scheduling!")
          val defs = blocks.flatMap { case JumpCase(_, blk) =>
            blk.flatMap(regDef)
          } ++ dslot.flatMap { regDef }
          val allUses =
            target +:
              (blocks.flatMap { case JumpCase(_, body) =>
                body.flatMap(regUses)
              } ++
                phis.flatMap { case Phi(_, rss) => rss.map(_._2) })
          // a use in a JumpTable is considered a value that is defined outside
          // of the JumpTable blocks but used inside, so we need to find all
          // possible uses, including ones defined internally and then subtract
          // all the definitions in the internal blocks.
          // We include the values used in the Phis nodes in the allUses because
          // a Phi node can directly use an externally defined value (e.g., if
          // a case block is empty).
          (allUses.toSet -- defs.toSet).toSeq
        case i: BreakCase =>
          Seq.empty
        case PutSerial(rs, pred, _, _) => Seq(rs, pred)
        case Interrupt(action, cond, _, _) =>
          action match {
            case StopInterrupt | FinishInterrupt | AssertionInterrupt =>
              Seq(cond)
            case SerialInterrupt(_) => Seq(cond)
          }

      }
    }

    /** Extracts the register defined by the instruction (if it defines one).
      *
      * @param inst
      *   Target instruction.
      * @return
      *   Register written by the instruction (if any).
      */
    def regDef(
        inst: Instruction
    ): Seq[Name] = {
      inst match {
        case BinaryArithmetic(operator, rd, rs1, rs2, annons) => Seq(rd)
        case CustomInstruction(func, rd, rsx, annons)         => Seq(rd)
        case LocalLoad(rd, base, offset, _, annons)           => Seq(rd)
        case LocalStore(rs, base, offset, p, _, annons)       => Nil
        case GlobalLoad(rd, base, annons)                     => Seq(rd)
        case GlobalStore(rs, base, pred, annons)              => Nil
        case Send(rd, rs, dest_id, annons)                    => Nil
        case SetValue(rd, value, annons)                      => Seq(rd)
        case Mux(rd, sel, rs1, rs2, annons)                   => Seq(rd)
        case Expect(ref, got, error_id, annons)               => Nil
        case Predicate(rs, annons)                            => Nil
        case Nop                                              => Nil
        case PadZero(rd, rs, width, annons)                   => Seq(rd)
        case AddC(rd, co, rs1, rs2, ci, annons)               => Seq(rd, co)
        case Mov(rd, _, _)                                    => Seq(rd)
        case Slice(rd, _, _, _, _)                            => Seq(rd)
        case ClearCarry(rd, _)                                => Seq(rd)
        case SetCarry(rd, _)                                  => Seq(rd)
        case _: Recv                                          => Nil
        case ParMux(rd, _, _, _)                              => Seq(rd)
        case JumpTable(_, results, _, dslot, _)               =>
          // assert(
          //   dslot.isEmpty || dslot.forall(_ == Nop),
          //   "dslot should only be used after scheduling!"
          // )
          results.map(_.rd)
        case Lookup(rd, _, _, _) => Seq(rd)
        case _: BreakCase        => Seq.empty
        case _: PutSerial        => Seq.empty
        case _: Interrupt        => Seq.empty
      }
    }

    /** Build a dependence graph
      *
      * @param process
      *   the process which contains the instructions
      * @param label
      *   a labeling function for edges
      * @param ctx
      *   compilation context
      * @return
      *   An mutable dependence graph
      */
    import scalax.collection.mutable.{Graph => MutableGraph}
    def build[L](
        process: DefProcess,
        label: (Instruction, Instruction) => L
    )(implicit
        ctx: AssemblyContext
    ): MutableGraph[Instruction, LDiEdge] = {

      // A map from registers to the instruction defining it (if any), useful for back tracking
      val def_instructions = definingInstructionMap(process)

      val raw_dependence_graph = MutableGraph.empty[Instruction, LDiEdge]

      process.body.foreach { inst =>
        // create register to register dependencies
        raw_dependence_graph += inst
        regUses(inst).foreach { use =>
          def_instructions.get(use) match {
            case Some(pred) =>
              raw_dependence_graph += LDiEdge[Instruction, L](pred, inst)(
                label(pred, inst)
              )
            case None =>
            // nothing
          }
        }
      }
      // now add explicit orderings as dependencies
      val hasOrder = process.body.collect {
        case inst: ExplicitlyOrderedInstruction => inst
      }

      val groups = hasOrder.groupBy { inst =>
        inst.order match {
          case scall: SystemCallOrder => "$syscall"
          case morder: MemoryAccessOrder =>
            s"$$memory:${morder.memory.toString()}"
        }
      }

      // groups.foreach { case (_, block) =>
      //   block.sortBy(_.order).sliding(2).foreach { case Seq(prev, next) =>
      //     raw_dependence_graph += LDiEdge(prev, next)(label(prev, next))
      //   }
      // }

      // One thing we need to do is to create dependencies between memory operations
      // and systemcalls. To do so, we start from every load operation and traverse
      // the dependence graph in DFS manner, keeping a set of visited nodes and
      // a set of nodes that contribute to a system call
      val memoryGroups = groups.filter(_._1 != "$syscall")

      if (groups.contains("$syscall")) {
        val syscallGroup = groups("$syscall").sortBy(_.order)
        val hasDependencyToSyscall =
          scala.collection.mutable.Map.empty[Instruction, Boolean]

        def visitGraph(n: MutableGraph[Instruction, LDiEdge]#NodeT): Unit = {
          if (!hasDependencyToSyscall.contains(n.toOuter)) {
            n.toOuter match {
              case syscall: SystemCallOrder =>
                hasDependencyToSyscall += (n.toOuter -> true)
              case inst =>
                n.diSuccessors.foreach(visitGraph)
                val isConnectedToSyscall =
                  n.diSuccessors.exists(hasDependencyToSyscall(_) == true)
                hasDependencyToSyscall += (n.toOuter -> isConnectedToSyscall)
            }
          }
        }

        // Let's visit the graph and see which memory ops should come before
        // systemcalls. Note that this never going to be the case for actual programs
        // since by construction syscalls should appear at the beginning of a cycle
        // not in the middle. We do this for hand-written program that may have
        // memory loads before systemcalls that actually need to be scheduled
        // before systemcalls
        memoryGroups.foreach { case (_, blk) =>
          blk.foreach { memop =>
            if (syscallGroup.nonEmpty) {
              visitGraph(raw_dependence_graph.get(memop))
              if (!hasDependencyToSyscall(memop)) {
                // create the dependency artificially
                raw_dependence_graph += LDiEdge(syscallGroup.last, memop)(
                  label(syscallGroup.last, memop)
                )
              }
            }
          }
        }
        // Finally go though all the groups and create dependencies within each
        syscallGroup.sliding(2).foreach { case Seq(prev, next) =>
          raw_dependence_graph += LDiEdge(prev, next)(
            label(prev, next)
          )
        }
      }

      memoryGroups.foreach { case (_, blk) =>
        blk.sliding(2).foreach { case Seq(prev, next) =>
          raw_dependence_graph += LDiEdge(prev, next)(
            label(prev, next)
          )
        }
      }

      // and we are done!
      raw_dependence_graph
    }.ensuring { g =>
      g.nodes.length == process.body.length
    }

    /** Create mapping from names to the instruction defining them (i.e.,
      * instruction that have that name as the destination)
      *
      * @param proc
      * @return
      */
    def definingInstructionMap(
        proc: DefProcess
    )(implicit ctx: AssemblyContext): Map[Name, Instruction] = {
      val name_def_map = scala.collection.mutable.Map.empty[Name, Instruction]
      proc.body.foreach { inst =>
        name_def_map ++= regDef(inst) map { rd => rd -> inst }
      }
      name_def_map.toMap
    }

    def definingInstruction(
        block: Seq[Instruction]
    )(implicit ctx: AssemblyContext): Map[Name, Instruction] = {
      block.flatMap { inst =>
        regDef(inst) map { _ -> inst }
      }.toMap
    }

    def extractBlock(
        n: Instruction
    )(implicit ctx: AssemblyContext): Option[UMemBlock] = {
      val annon = n.annons.collectFirst { case a: Memblock =>
        UMemBlock(a.getBlock(), a.getIndex())
      }
      annon
    }

    def memoryBlockStores(proc: DefProcess)(implicit
        ctx: AssemblyContext
    ): Map[UMemBlock, Instruction] = {

      proc.body
        .collect { case store @ (_: LocalStore | _: GlobalStore) =>
          val opt_b = extractBlock(store)
          if (opt_b.isEmpty) {
            ctx.logger.error(s"missing valid @${Memblock.name}", store)
          }
          opt_b -> store
        }
        .collect { case (Some(b), i) =>
          b -> i
        }
        .toMap
    }
    // a unique memory block
    case class UMemBlock(block: String, index: Option[Int])

    // /** Create a mapping from load/store base registers to the set of registers
    //   * that constitute the base of the memory
    //   *
    //   * @param proc
    //   * @param def_instructions
    //   * @return
    //   */
    // def memoryBlocks(
    //     proc: DefProcess,
    //     def_instructions: Map[Name, Instruction]
    // )(implicit
    //     ctx: AssemblyContext
    // ): Map[Name, Set[DefReg]] = {

    //   Map.empty[Name, Set[DefReg]]

    // }

    /** Collect all the referenced name in the given block of instructions (use
      * or def)
      *
      * @param block
      * @param ctx
      * @return
      */
    def referencedNames(
        block: Iterable[Instruction]
    )(implicit ctx: AssemblyContext): Set[Name] = {

      val namesToKeep = scala.collection.mutable.Set.empty[Name]

      block.foreach { inst =>
        namesToKeep ++= regDef(inst)
        namesToKeep ++= regUses(inst)
        inst match {
          // handle jump tables differently, note that redDef on JumpTable
          // only returns the value defined by Phi and no the ones inside since
          // any value defined inside, unless used in the Phi operands should not
          // reach outside
          case JumpTable(target, results, blocks, dslot, annons) =>
            blocks.foreach { case JumpCase(_, blk) =>
              blk.foreach { i => namesToKeep ++= regDef(i) }
            }
            dslot.foreach { i => namesToKeep ++= regDef(i) }
          case _ =>
        }
      }

      namesToKeep.toSet
    }
  }

}
