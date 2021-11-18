package manticore.assembly.levels.placed

import manticore.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import manticore.assembly.BinaryOperator
import scala.annotation.tailrec
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import manticore.assembly.CompilationFailureException
import manticore.assembly.levels.ConstLogic
import manticore.assembly.levels.MemoryLogic

object ListSchedulerTransform extends AssemblyTransformer(PlacedIR, PlacedIR) {

  import PlacedIR._

  class DependenceGraph {

    case class Vertex(inst: Instruction, weight: Int, egress: List[Edge])
    case class Edge(target: Vertex, weight: Int)

  }

  def instructionLatency(instruction: Instruction): Int = ???

  def createDependenceGraph(
      process: DefProcess,
      ctx: AssemblyContext
  ): DependenceGraph = {

    def regUses(inst: Instruction): List[Name] = inst match {
      case BinaryArithmetic(BinaryOperator.PMUX, rd, rs1, rs2, _) =>
        logger.warn("PMUX instruction may lead to invalid scheduling!", inst)
        List(rs1, rs2)
      case BinaryArithmetic(operator, rd, rs1, rs2, _) =>
        List(rs1, rs2)
      case CustomInstruction(func, rd, rs1, rs2, rs3, rs4, _) =>
        List(rs1, rs2, rs3, rs4)
      case LocalLoad(rd, base, offset, _) =>
        List(base)
      case LocalStore(rs, base, offset, p, _) =>
        List(rs, base) ++ (p match {
          case None =>
            logger.warn(
              "Local store instruction does not have a predicate, the scheduling maybe invalid!",
              inst
            )
            List.empty[Name]
          case Some(n) => List(n)
        })

      case GlobalLoad(rd, base, _) =>
        List(base._1, base._2, base._3)
      case GlobalStore(rs, base, pred, _) =>
        List(rs, base._1, base._2, base._3) ++ (pred match {
          case None =>
            logger.warn(
              "GlobalStore instruction does not have a predicate, the scheduling maybe invalid!",
              inst
            )
            List.empty[Name]
          case Some(n) =>
            List(n)
        })

      case Send(rd, rs, dest_id, _)  => List(rs)
      case SetValue(rd, value, _)    => List(rd)
      case Mux(rd, sel, rs1, rs2, _) => List(sel, rs1, rs2)
      case Expect(ref, got, error_id, _) =>
        List(ref, got)
      case Predicate(rs, _) =>
        logger.warn(
          "PREDICATE should be added after scheduling! The final schedule maybe invalid!",
          inst
        )
        List(rs)

    }

    def regDef(inst: Instruction): Option[Name] = inst match {
      case BinaryArithmetic(operator, rd, rs1, rs2, _)        => Some(rd)
      case CustomInstruction(func, rd, rs1, rs2, rs3, rs4, _) => Some(rd)
      case LocalLoad(rd, base, offset, _)                     => Some(rd)
      case LocalStore(rs, base, offset, p, _)                 => None
      case GlobalLoad(rd, base, _)                            => Some(rd)
      case GlobalStore(rs, base, pred, _)                     => None
      case Send(rd, rs, dest_id, _)                           => None
      case SetValue(rd, value, _)                             => Some(rd)
      case Mux(rd, sel, rs1, rs2, _)                          => Some(rd)
      case Expect(ref, got, error_id, _)                      => None
      case Predicate(rs, _)                                   => None
    }

    // The register-to-register RAW dependencies
    val raw_dependency =
      scala.collection.mutable.Map[Name, scala.collection.mutable.Set[Name]]()
    // A map from registers to the instruction defining it (if any)
    val def_instructions =
      scala.collection.mutable.Map[Name, Instruction]()
    process.body.foreach { inst =>
      regDef(inst) match {
        case Some(rd) =>
          // keep a map from the target regs to the instruction defining it
          // useful for backward traversal of the dependence graph
          def_instructions.update(rd, inst)
          // a list of operands used by the instruction that rd depends on the
          val rss = regUses(inst)
          rss.foreach { rs =>
            val seen_so_far: scala.collection.mutable.Set[Name] =
              raw_dependency.getOrElse(
                rs,
                scala.collection.mutable.Set.empty[Name]
              )
            seen_so_far.add(rd)
            // creates a dependence between rd and rs (rd depends on rs,
            // therefore raw_dependency(rs) contains rd)
            raw_dependency.update(
              rs,
              seen_so_far
            )
          }
        case None =>
        // instruction does not a value, hence no new dependence edges
      }
    }

    type EitherLoad = Either[LocalLoad, GlobalLoad]
    type EitherStore = Either[LocalStore, GlobalStore]
    val loads: Seq[EitherLoad] = process.body.collect {
      case ll @ LocalLoad(_, _, _, _) => Left(ll)
      case gl @ GlobalLoad(_, _, _)   => Right(gl)
    }

    val stores: Seq[EitherStore] = process.body.collect {
      case ls @ LocalStore(_, _, _, _, _) => Left(ls)
      case gs @ GlobalStore(_, _, _, _)   => Right(gs)

    }

    val mem_decls: Seq[DefReg] = process.registers.collect {
      case m @ DefReg(LogicVariable(_, _, MemoryLogic), v, _) => m
    }

    // a map from register to potential memory (.mem) declaration
    val mem_block =
      scala.collection.mutable.Map[Name, Option[DefReg]]()

    // a map from memories to stores to that memory
    val mem_block_stores =
      scala.collection.mutable
        .Map[DefReg, scala.collection.mutable.Set[EitherStore]]()

    /** Now we need to create a dependence between loads and stores (store
      * depends on load) that operate on the same memory block. For this, we
      * need to start from every load instruction, and trace back throw use-def
      * chains to reach a DefReg with memory type MemoryLogic
      */

    def traceMemoryDecl(base: Name): Option[DefReg] = {
      mem_block.get(base) match {
        case Some(m) => m // good, we already know the the possible mem block
        case None    => // bad, need to recurse :(
          // get the instruction that produces base
          val producer_inst: Option[Instruction] = def_instructions.get(base)
          producer_inst match {
            case Some(inst) =>
              val uses = regUses(inst)
              // recurse on every use in inst and update the mem_block
              val decls: Seq[Option[DefReg]] = uses.map { u =>
                val parent_decl = traceMemoryDecl(u)
                mem_block.update(u, parent_decl)
                parent_decl
              }
              // ensure no instruction depends on to mem
              if (decls.count(_.nonEmpty) > 1) {
                logger.error(
                  "instruction traces back to multiple memory blocks!",
                  inst
                )
              }
              // return any findings, so the children could
              decls.find(_.nonEmpty).map(_.get)
            case None =>
              // jackpot, no instruction produces base,
              // now all we need to do is to check declarations
              val decl = mem_decls.find(_.variable.name == base)
              mem_block.update(base, decl)
              decl
          }
      }
    }

    stores.foreach { s =>
      val base_ptrs = s match {
        case Left(LocalStore(_, base, _, _, _))        => Seq(base)
        case Right(GlobalStore(_, (bh, bm, bl), _, _)) => Seq(bh, bm, bl)
      }

      base_ptrs.map { b =>
        val m = traceMemoryDecl(b)
        m match {
          case Some(mdef) =>
            val current_stores = mem_block_stores.getOrElse(
              mdef,
              scala.collection.mutable.Set.empty[EitherStore]
            )
            current_stores.add(s)
            mem_block_stores.update(mdef, current_stores)
          case None =>
            val unwrapped: Instruction = s match {
              case Left(i)  => i
              case Right(i) => i
            }
            logger.error(s"store instruction with no memory block!", unwrapped)
        }
        m
      }
    }

    loads.foreach { l =>
      // get all the base pointers
      val base_ptrs = l match {
        case Left(LocalLoad(rd, base, _, _))        => Seq(base)
        case Right(GlobalLoad(rd, (bh, bm, bl), _)) => Seq(bh, bm, bl)
      }
      // and trace them back to a .mem decl
      base_ptrs.map { b =>
        val m = traceMemoryDecl(b)

      }

    }

    // A map from Names to their instruction definition site, note that const values
    // are defined at their declaration site and impose no dependencies throughout the
    // execution. That is why the map is from Name to Option[Instruction] not Instruction
    val register_definitions =
      scala.collection.mutable.Map[Name, Option[Instruction]]()

    def findDefiningInstruction(use: Name): Option[Instruction] = {
      // recursively traverse the body and find and instruction that
      // defines 'use'
      @tailrec
      def findDefInst(to_check: Seq[Instruction]): Option[Instruction] =
        to_check match {
          case Nil => // const value!
            if (ctx.getDebugMessage) {
              // debug mode enabled, ensure the definition exists and is marked
              // as const
              process.registers.find { r => r.variable.name == use } match {
                case Some(reg) =>
                  if (reg.variable.tpe != ConstLogic)
                    logger.warn(
                      s"Register ${use} is constant but not declared as const",
                      reg
                    )
                  if (reg.value.isEmpty)
                    logger.warn(
                      s"Register ${use} is constant but does not have a value!",
                      reg
                    )
                case None =>
                  logger.error(s"Register ${use} is used but never defined")
              }
            }
            None
          case head :: tail =>
            regDef(head) match {
              case Some(name) if name == use =>
                Some(head)
              case _ =>
                findDefInst(tail)
            }
        }
      // if the we already know who defines 'use', return it, otherwise
      // find the definition, which could be 'None', e.g., for constants
      register_definitions.getOrElseUpdate(use, findDefInst(process.body))

    }

    /** We create a dependence graph as a Map from instructions to
      * Set[Instruction] This can be done by iterating over each instruction,
      * extracting its left-hand-side (target) register (i.e., the one for which
      * it defines a new value) and then finding all of the other instructions
      * that use that register. Those instructions depend on the first one and
      * the map is essentially a graph, with a directed edge from x -> y where y
      * depends on x.
      */

    val raw = process.body.map { inst =>
      val depending_insts =
        regDef(inst) match {
          case None    => Set.empty[Instruction]
          case Some(r) =>
            // find all the uses r
            process.body.filter { dep =>
              regUses(dep) contains r
            }.toSet
        }
      (inst -> depending_insts)
    }.toMap

    /** However, this only takes care of register-to-register RAW dependence but
      * we also need to ensure that every store to memory block m, comes after
      * all of the other loads to the same block. So we need to pattern match on
      * every load, and find the memory that defines it (i.e., .mem) by
      * following a chain of instructions that defines the base register. Then
      * we have to find all the stores that uses the same `.mem` block and
      * include those store instruction in the set of depending instruction of
      * the load
      */
    // val gg

    /** TODO: Can we make it tailrec? */
    // def traceMemoryDecl(base_ptr: Name): Option[DefReg] = {
    //   findDefiningInstruction(base_ptr) match {
    //     case None => // jack-pot, base_ptr is not defined by any instruction, so look in the decls
    //       process.registers.find { reg => reg.variable.name == base_ptr }
    //     case Some(inst) => // need to track instructions back to some .mem
    //       regUses(inst).map { traceMemoryDecl } find { _.nonEmpty } match {
    //         case None    => None
    //         case Some(x) => x
    //       }
    //   }
    // }

    // val mem_users = scala.collection.mutable
    //   .Map[DefReg, scala.collection.mutable.Set[Instruction]]()

    // def findStoreUsers(mem_decl: DefReg): Set[Instruction] = {

    //   def ???

    // }

    // val mem_order = process.body.map { inst =>
    //   val depending = inst match {
    //     case LocalLoad(rd, base, _, _) =>
    //       traceMemoryDecl(base) match {
    //         case None =>
    //           logger.error("Could not find the memory declaration!", inst)
    //         case Some(mem_decl) =>

    //       }
    //     case GlobalLoad(rd, base, _) => Set()
    //     case _                       => Set()
    //   }

    // }
    ???
  }

  override def transform(
      source: DefProgram,
      context: AssemblyContext
  ): DefProgram = ???

}
