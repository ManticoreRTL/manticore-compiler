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
    )(implicit ctx: AssemblyContext): Seq[Name] = {
      inst match {
        case BinaryArithmetic(operator, rd, rs1, rs2, annons) =>
          Seq(rs1, rs2)
        case CustomInstruction(func, rd, rs1, rs2, rs3, rs4, annons) =>
          Seq(rs1, rs2, rs3, rs4)
        case LocalLoad(rd, base, offset, annons) =>
          Seq(base)
        case LocalStore(rs, base, offset, predicate, annons) =>
          val pred = predicate match {
            case None      => Seq.empty
            case Some(reg) => Seq(reg)
          }
          Seq(rs, base) ++ pred
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
          assert(dslot.isEmpty, "dslot should only be used after scheduling!")
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
    )(implicit ctx: AssemblyContext): Seq[Name] = {

      inst match {
        case BinaryArithmetic(operator, rd, rs1, rs2, annons)        => Seq(rd)
        case CustomInstruction(func, rd, rs1, rs2, rs3, rs4, annons) => Seq(rd)
        case LocalLoad(rd, base, offset, annons)                     => Seq(rd)
        case LocalStore(rs, base, offset, p, annons)                 => Nil
        case GlobalLoad(rd, base, annons)                            => Seq(rd)
        case GlobalStore(rs, base, pred, annons)                     => Nil
        case Send(rd, rs, dest_id, annons)                           => Nil
        case SetValue(rd, value, annons)                             => Seq(rd)
        case Mux(rd, sel, rs1, rs2, annons)                          => Seq(rd)
        case Expect(ref, got, error_id, annons)                      => Nil
        case Predicate(rs, annons)                                   => Nil
        case Nop                                                     => Nil
        case PadZero(rd, rs, width, annons)                          => Seq(rd)
        case AddC(rd, co, rs1, rs2, ci, annons) => Seq(rd, co)
        case Mov(rd, _, _)                      => Seq(rd)
        case ClearCarry(rd, _)                  => Seq(rd)
        case SetCarry(rd, _)                    => Seq(rd)
        case _: Recv                            => Nil
        case ParMux(rd, _, _, _)                => Seq(rd)
        case JumpTable(_, results, _, dslot, _)     =>
          assert(dslot.isEmpty, "dslot should only be used after scheduling!")
          results.map(_.rd)
        case Lookup(rd, _, _, _)                => Seq(rd)
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

      // create a mapping from unique memory blocks to store instructions
      val blocks_to_stores = memoryBlockStores(process)
      val loads = process.body.collect {
        case x @ (_: LocalLoad | _: GlobalLoad) => x -> extractBlock(x).get
      }
      val load_to_store = loads.map { case (l, b) =>
        l -> blocks_to_stores.get(b)
      }.toMap

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

        // now add a load to store dependency if the instruction is a load
        inst match {
          case load @ (_: LocalLoad | _: GlobalLoad) =>
            // find the memory block associated with this load
            extractBlock(load) match {
              case Some(block) =>
                blocks_to_stores.get(block) match {
                  case Some(store) =>
                    raw_dependence_graph += LDiEdge[Instruction, L](
                      load,
                      store
                    )(
                      label(load, store)
                    )
                  case None =>
                    ctx.logger.debug("Inferring read-only memory", load)
                  // read only memory
                }
              case _ =>
                ctx.logger.error(s"Missing valid @${Memblock.name}", load)

            }
          case _ =>
          // do nothing
        }
      }
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

    /** Create a mapping from load/store base registers to the set of registers
      * that constitute the base of the memory
      *
      * @param proc
      * @param def_instructions
      * @return
      */
    def memoryBlocks(
        proc: DefProcess,
        def_instructions: Map[Name, Instruction]
    )(implicit
        ctx: AssemblyContext
    ): Map[Name, Set[DefReg]] = {

      Map.empty[Name, Set[DefReg]]

    }

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
