package manticore.assembly.levels.placed

import manticore.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import manticore.assembly.BinaryOperator
import scala.annotation.tailrec
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import manticore.assembly.CompilationFailureException

object ListSchedulerTransform extends AssemblyTransformer(PlacedIR, PlacedIR) {

  import PlacedIR._
  import scalax.collection.Graph
  import scalax.collection.edge.WDiEdge

  def instructionLatency(instruction: Instruction): Int = ???

  def createDependenceGraph(
      process: DefProcess,
      ctx: AssemblyContext
  ): Graph[Instruction, WDiEdge] = {

    /** Extract the registers used in the instruction
      *
      * @param inst
      * @return
      */
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

    /** Extract the register define by the instruction if it defines one
      *
      * @param inst
      * @return
      */
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

    val raw_inst_graph = Graph[Instruction, WDiEdge]() ++ process.body

    // The register-to-register RAW dependencies
    val raw_dependency =
      scala.collection.mutable.Map[Name, scala.collection.mutable.Set[Name]]()

    // A map from registers to the instruction defining it (if any), useful for back tracking
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

    val mem_decls: Seq[MemoryVariable] = process.registers.collect {
      case DefReg(m @ MemoryVariable(_, _, _), _, _) => m
    }

    // a map from register to potential memory (.mem) declaration
    val mem_block =
      scala.collection.mutable.Map[Name, Option[Name]]()

    // a map from memories to stores to that memory
    val mem_block_stores =
      scala.collection.mutable
        .Map[Name, scala.collection.mutable.Set[EitherStore]]()

    /** Now we need to create a dependence between loads and stores (store
      * depends on load) that operate on the same memory block. For this, we
      * need to start from every load instruction, and trace back throw use-def
      * chains to reach a DefReg with memory type MemoryLogic
      */

    def traceMemoryDecl(base: Name): Option[Name] = {
      mem_block.get(base) match {
        case Some(m) => m // good, we already know the the possible mem block
        case None    => // bad, need to recurse :(
          // get the instruction that produces base
          val producer_inst: Option[Instruction] = def_instructions.get(base)
          producer_inst match {
            case Some(inst) =>
              val uses = regUses(inst)
              // recurse on every use in inst and update the mem_block
              val decls: Seq[Option[Name]] = uses.map { u =>
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
              val decl = mem_decls.find(_.name == base)
              val block = decl.map(_.block)
              mem_block.update(base, block)
              block
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
            logger.error(s"store instruction has no memory block!", unwrapped)
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
        block: Name
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
          traceMemoryDecl(base) match {
            case Some(mdef) =>
              createLoadStoreDependence(inst, mdef)
            case None =>
              logger.error("Found no memory for load instruction", inst)
          }

        case Right(inst @ GlobalLoad(_, (bh, bm, bl), _)) =>
          val used_mems: Seq[Name] =
            Seq(bh, bm, bl) map { traceMemoryDecl } collect { case Some(mdef) =>
              mdef
            }
          if (used_mems.isEmpty) {
            logger.error("Found no memory for load instruction", inst)
          } else {
            // make sure a single memory is used!
            if (used_mems.forall(_ != used_mems.head))
              logger.error(
                "Mixed memory access, not all base pointers trace back to the same memory block!",
                inst
              )
            createLoadStoreDependence(inst, used_mems.head)
          }
      }
    }

    /** At this point, we a have mapping from load instructions to depending
      * store instructions, and a mapping from registers to register based on
      * RAW dependencies (i.e., raw(rs)=rd, iff rs is used to produce rd) and a
      * mapping from registers to their defining instruction. Combining the tree
      * gives a us an instruction-to-instruction dependence relation
      */

    val raw_dependence_graph =
      process.body.foldLeft(Graph.empty[Instruction, WDiEdge]) {
        case (g, inst) =>
          // first add an edge for register to register dependency
          regDef(inst) match {
            case Some(rd) =>
              // find the instructions that use the value defined by inst
              val using_instructions = raw_dependency.get(rd) match {
                case Some(uses) =>
                  val insts_of_uses: scala.collection.mutable.Set[Instruction] =
                    uses.collect { rs =>
                      def_instructions.get(rs) match {
                        case Some(inst) => inst
                      }
                    }
                  insts_of_uses
                case None =>
                  scala.collection.mutable.Set.empty[Instruction]
              }
              // these instructions depend on inst, so create an edge for each
              using_instructions.foldLeft(g + inst) { case (gg, ii) =>
                gg + WDiEdge(inst, ii)(4)
              } // latency is hardcoded to 4
            case None =>
              g
            // do nothing, the instruction does not define any value, e.g., could
            // be a store instruction
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
            gg + WDiEdge(l, s)(1)
          }

      }

    if (dependence_graph.isCyclic) {
      logger.error("Could not create acyclic dependence graph!")
    }

    dependence_graph.nodes.foreach { n =>
      println(n.toOuter.serialized)
    }
    // dependence_graph.
    ctx.dumpArtifact(s"dependence_graph_${getName}.dot") {

      import scalax.collection.io.dot._
      import scalax.collection.io.dot.implicits._
      // println(g.edges.size)
      val dot_root = DotRootGraph(
        directed = true,
        id = Some("List scheduling dependence graph")
      )
      def edgeTransform(
          iedge: Graph[Instruction, WDiEdge]#EdgeT
      ): Option[(DotGraph, DotEdgeStmt)] = iedge.edge match {
        case WDiEdge(source, target, w) =>
          Some(
            (
              dot_root,
              DotEdgeStmt(
                source.toOuter.hashCode().toString,
                target.toOuter.hashCode().toString,
                List(DotAttr("label", w.toString))
              )
            )
          )
        case t @ _ =>
          logger.error(
            s"An edge in the dependence could not be serialized! ${t}"
          )
          None
      }
      def nodeTransformer(
          inode: Graph[Instruction, WDiEdge]#NodeT
      ): Option[(DotGraph, DotNodeStmt)] =
        Some(
          (
            dot_root,
            DotNodeStmt(
              NodeId(inode.toOuter.hashCode().toString()),
              List(DotAttr("label", inode.toOuter.serialized))
            )
          )
        )

      val dot_export: String = dependence_graph.toDot(
        dotRoot = dot_root, 
        edgeTransformer = edgeTransform,
        cNodeTransformer = Some(nodeTransformer))
      dot_export
    }
    dependence_graph
  }

  override def transform(
      source: DefProgram,
      context: AssemblyContext
  ): DefProgram = {

    val g = createDependenceGraph(source.processes.head, context)

    source
  }

}
