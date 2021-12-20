package manticore.assembly

/** DependenceGraph.scala
  *
  * @author
  *   Sahand Kashani <sahand.kashani@epfl.ch> Mahyar Emami
  *   <mahyar.emami@eplf.ch>
  */

import manticore.assembly.levels.AssemblyTransformer
import manticore.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.AssemblyContext
import scalax.collection.edge.LDiEdge
import manticore.assembly.levels.MemoryType
import scalax.collection.Graph
import scala.util.Try
import manticore.assembly.levels.Flavored

/** Generic dependence graph builder, to use it, mix this trait with your
  * transformation
  * @param flavor
  */
trait DependenceGraphBuilder extends Flavored {

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
      (inst: @unchecked) match {
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
        case SetValue(rd, value, annons) =>
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
    ): Option[Name] = {
      (inst: @unchecked) match {
        case BinaryArithmetic(operator, rd, rs1, rs2, annons)        => Some(rd)
        case CustomInstruction(func, rd, rs1, rs2, rs3, rs4, annons) => Some(rd)
        case LocalLoad(rd, base, offset, annons)                     => Some(rd)
        case LocalStore(rs, base, offset, p, annons)                 => None
        case GlobalLoad(rd, base, annons)                            => Some(rd)
        case GlobalStore(rs, base, pred, annons)                     => None
        case Send(rd, rs, dest_id, annons)                           => None
        case SetValue(rd, value, annons)                             => Some(rd)
        case Mux(rd, sel, rs1, rs2, annons)                          => Some(rd)
        case Expect(ref, got, error_id, annons)                      => None
        case Predicate(rs, annons)                                   => None
        case Nop                                                     => None
        case PadZero(rd, rs, width, annons)                          => Some(rd)
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
      *   An immutable dependence graph
      */
    def build[L](
        process: DefProcess,
        label: (Instruction, Instruction) => L
    )(implicit
        ctx: AssemblyContext
    ): Graph[Instruction, LDiEdge] = {

      import scalax.collection.mutable.{Graph => MutableGraph}

      // A map from registers to the instruction defining it (if any), useful for back tracking
      val def_instructions =
        scala.collection.mutable.Map[Name, Instruction]()
      process.body.foreach { inst =>
        regDef(inst) match {
          case Some(rd) =>
            // keep a map from the target regs to the instruction defining it
            def_instructions.update(rd, inst)

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
        case d @ DefReg(m, _, _) if m.varType == MemoryType => d
      }

      // a map from register to potential memory (.mem) declaration
      val mem_block =
        scala.collection.mutable.Map[Name, Set[DefReg]]()

      // a map from memories to stores to that memory
      val mem_block_stores =
        scala.collection.mutable
          .Map[DefReg, scala.collection.mutable.Set[EitherStore]]()

      /** Now we need to create a dependence between loads and stores (store
        * depends on load) that operate on the same memory block. For this, we
        * need to start from every load instruction, and trace back throw
        * use-def chains to reach a DefReg with memory type MemoryLogic
        */

      def traceMemoryDecl(base: Name): Set[DefReg] = {
        mem_block.get(base) match {
          case Some(m) => m // good, we already know the the possible mem block
          case None    => // bad, need to recurse :(
            // get the instruction that produces base
            val producer_inst: Option[Instruction] = def_instructions.get(base)
            producer_inst match {
              case Some(inst) =>
                val uses = regUses(inst)
                // recurse on every use in inst and update the mem_block
                val decls: Seq[Set[DefReg]] = uses.map { u: Name =>
                  val parent_decl = traceMemoryDecl(u)
                  mem_block.update(u, parent_decl)
                  parent_decl
                }
                // ensure no instruction depends on to mem
                if (decls.count(_.nonEmpty) > 1) {
                  logger.warn(
                    "instruction traces back to multiple memory blocks!",
                    inst
                  )
                }
                // return any findings
                decls.foldLeft(Set.empty[DefReg]) { case (c, x) => c ++ x }
              case None =>
                // jackpot, no instruction produces base,
                // now all we need to do is to check declarations
                val decl = mem_decls.find(_.variable.name == base).toSet
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

        base_ptrs.foreach { b =>
          val m = traceMemoryDecl(b)
          if (m.isEmpty) {
            val unwrapped: Instruction = s match {
              case Left(i)  => i
              case Right(i) => i
            }
            logger.error(s"store instruction has no memory block!", unwrapped)
          }
          m.foreach { mdef =>
            val current_stores = mem_block_stores.getOrElse(
              mdef,
              scala.collection.mutable.Set.empty[EitherStore]
            )
            current_stores.add(s)
            mem_block_stores.update(mdef, current_stores)
          }
        }
      }

      // Load-to-store dependency relation
      val load_store_dependency = scala.collection.mutable
        .Map[Instruction, scala.collection.mutable.Set[Instruction]]()

      /** Create a dependence between load and store instructions through the
        * memory block [[mdef]]
        *
        * @param inst
        *   the load instruction
        * @param mdef
        *   the memory block shared by the load and stores
        */
      def createLoadStoreDependence(
          inst: Instruction,
          block: DefReg
      ): Unit = {
        require(
          inst.isInstanceOf[LocalLoad] || inst.isInstanceOf[GlobalLoad],
          "can only create load-to-store dependence from loads!"
        )

        // find the corresponding store instructions
        val stores_using_mdef = mem_block_stores.get(block) match {
          case Some(store_set) =>
            if (store_set.isEmpty)
              logger.error(
                s"Internal error! Found empty store set for memory ${block}",
                inst
              )
            load_store_dependency.update(
              inst,
              store_set.collect {
                case Right(x) => x
                case Left(x)  => x
              }
            )
          case None =>
            logger.debug(
              s"Inferred ROM memory ${block}",
              inst
            )(ctx)
        }

      }

      loads.foreach { l =>
        // get all the base pointers
        l match {
          case Left(inst @ LocalLoad(_, base, _, _)) =>
            val decls = traceMemoryDecl(base)

            if (decls.isEmpty) {
              logger.error("Found no memory for load instruction", inst)
            }

            decls.foreach { mdef => createLoadStoreDependence(inst, mdef) }

          case Right(inst @ GlobalLoad(_, (bh, bm, bl), _)) =>
            val used_mems: Set[DefReg] =
              Seq(bh, bm, bl).foldLeft(Set.empty[DefReg]) { case (c, x) =>
                c ++ traceMemoryDecl(x)
              }

            if (used_mems.isEmpty) {
              logger.error("Found no memory for load instruction", inst)
            } else {
              // make sure a single memory is used!
              used_mems.foreach { u => createLoadStoreDependence(inst, u) }

            }
        }
      }

      /** At this point, we a have mapping from load instructions to depending
        * store instructions, and a mapping from registers to instruction
        * defining them, we can now build the dependence graph
        */

      val raw_dependence_graph =
        process.body.foldLeft(
          MutableGraph[Instruction, LDiEdge](process.body: _*)
        ) { case (g, inst) =>
          // first add an edge for register to register dependency
          regUses(inst).foldLeft(g + inst) { case (gg, use) =>
            def_instructions.get(use) match {
              case Some(pred) =>
                gg += LDiEdge[Instruction, L](pred, inst)(label(pred, inst))
              case None =>
                gg
            }
          }
        }
      // now add the load-to-store dependencies
      val dependence_graph = loads
        .map {
          case Right(x) => x
          case Left(x)  => x
        }
        .foldLeft(raw_dependence_graph) { case (g, l) =>
          load_store_dependency
            .getOrElse(l, scala.collection.mutable.Set.empty[Instruction])
            .foldLeft(g) { case (gg, s) =>
              gg += LDiEdge(l, s)(label(l, s))
            }
        }

      // dependence_graph.nodes.foreach { n =>
      //   println(s"Node: ${n.toOuter.serialized}")
      // }
      process.body.foreach { inst => }
      if (dependence_graph.isCyclic) {
        logger.error("Could not create acyclic dependence graph!")
      }

      if (logger.countErrors > 0) {
        logger.fail("Failed to create a dependence graph due to earlier errors")
      }
      dependence_graph
    }
  }

}
