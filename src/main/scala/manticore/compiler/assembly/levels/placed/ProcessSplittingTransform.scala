package manticore.compiler.assembly.levels.placed

import manticore.compiler.assembly.DependenceGraphBuilder
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.ConstType
import manticore.compiler.assembly.levels.InputType
import manticore.compiler.assembly.levels.VariableType
import manticore.compiler.assembly.levels.OutputType
import scala.util.Success
import scala.util.Try
import scalax.collection.mutable.{Graph => MutableGraph}
import scalax.collection.Graph

import scalax.collection.GraphEdge
import java.lang.management.MemoryType
import scalax.collection.edge.LDiEdge
import scala.util.Failure
import manticore.compiler.assembly.annotations.Memblock
import scala.annotation.tailrec
import scalax.collection.GraphTraversal
import scala.collection.immutable

/** A pass to parallelize processes while respecting resource dependence
  * constraints
  *
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */
object ProcessSplittingTransform
    extends DependenceGraphBuilder
    with PlacedIRTransformer {

  val flavor = PlacedIR
  import flavor._

  private def extractIndependentInstructionSequences(
      proc: DefProcess
  )(implicit ctx: AssemblyContext) = {

    val dependence_graph =
      DependenceAnalysis.build(process = proc, label = (i, j) => i.toString())

    import manticore.compiler.assembly.annotations.{Reg => RegAnnotation}
    // find the output registers, these are registers that are written in
    // sink instructions, there could be instructions that are sinks in the
    // dependence graph but are not really relevant because they don't write
    // to outputs

    def getRegId(r: DefReg): Try[(String, Option[Int])] = Try {
      val annon: Option[RegAnnotation] = r.annons.collectFirst {
        case x: RegAnnotation => x
      }
      (annon.get.getId(), annon.get.getIndex())
    }

    val inputs = proc.registers
      .collect {
        case r @ DefReg(v, _, _) if v.varType == InputType =>
          val id = getRegId(r)
          if (id.isFailure) {
            ctx.logger.error(
              s"input register is missing a valid @${RegAnnotation.name}"
            )
          }
          id -> r
      }
      .collect { case (Success(x) -> r) =>
        x -> r
      }
      .toMap

    def keepOutput(r: DefReg): Boolean = getRegId(r) match {
      case Success(id_unwrapped) =>
        inputs.get(id_unwrapped) match {
          case Some(_) =>
            true
          case None =>
            ctx.logger.warn("output register is missing input correspondent", r)
            ctx.logger.flush()
            false
        }
      case _ =>
        ctx.logger.error(
          s"output register is missing a valid @${RegAnnotation.name}",
          r
        )
        false
    }

    val outputs = proc.registers.collect {
      case r @ DefReg(v, _, _) if v.varType == OutputType && keepOutput(r) =>
        r.variable.name
    }.toSet

    val sink_nodes = dependence_graph.nodes.filter { node =>
      val writes_to_output =
        DependenceAnalysis.regDef(node.toOuter).exists(outputs.contains)
      val is_store = node.toOuter match {
        case _: LocalStore | _: GlobalStore => true
        case _: Expect                      => true
        case _                              => false
      }
      writes_to_output | is_store
    }

    // We want to find parallel processes from by performing a backward
    // traversal starting from sink nodes in the instruction dependence graph we
    // have instantiated. But in doing so we must respect two constraints:
    // 1. If process p1 and p2 have EXPECT/GlobalLoad/GlobalStore instructions then p1 = p2
    // 2. If process p1 and p2 both access memory block b, then p1 = p2
    //
    // To do so, we build another graph, call it a constraint_graph to encode
    // the which instructions are supposed to be grouped together. This graph is
    // a 3 level high hierarchy: At the first level, we have a vertex that
    // indicates the kind of constraint (i.e., memory or system call). At the
    // second level there are processes "roots" that essentially wrapped output
    // instructions (writing to output regs or storing in memory or system
    // call). And the last level of hierarchy are all the other instructions
    // (including output ones). We can construct this graph by perform multiple
    // backwards traversals starting from the sink nodes. The complexity would
    // be O(#out * (V + E)) with #out begin number of sink instructions and (V +
    // E) being the worst case complexity of a backward traversal (e.g., BFS).
    //
    //              +----------------+ +-----------------+ +--------+ +-----------+
    //              |                | |                 | |        | |           |
    //              |MemBlockRoot(b0)| | MemBlockRoot(b1)| |FreeRoot| |SysCallRoot|
    //              |                | |                 | |        | |           |
    //      +-------+--+-------+-----+ +-------+---------+ +--------+ +-----------+
    //      |          |       |               |           |        |             |
    //      |          |       | +-------------+           |        |             |
    //      |          |       | |                         |        |             |
    // +----v---+  +---v---+ +-v-v--+  +--------+    +-----v-+  +---v----+    +---v-----+
    // |        |  |       | |      |  |        |    |       |  |        |    |         |
    // |ProcRoot|  |       | |      |  |        |    |       |  |        |    |         |
    // +----+---+  +-------+ +------+  +--------+    +-------+  +--------+    +---------+
    //      |
    //      |
    //    +-v----+
    //    | leaf |
    //    +------+
    //
    // After constructing this graph, we mask the edges in the last level (edge
    // going to leaves) and look for weakly connected components, the set of ProcRoots
    // at in every weakly connected sub graph can help us get the instructions
    // that have to be packed together. We simply create a set of instructions
    // by iterating the set of ProcRoots in a weakly connected subgraph and
    // that will be the instructions we should pack in one process.
    //
    //

    // Helper classes for building the constraint graph

    sealed abstract class SubProcess
    case class MemBlockRoot(memblock: MemoryBlock) extends SubProcess {
      override def toString(): String = memblock.block_id

      override def equals(x: Any) = x match {
        case mx: MemBlockRoot => (mx eq this) || (mx.memblock == memblock)
        case _                => false
      }
      override def hashCode() = memblock.hashCode()

    }
    case object SysCallRoot extends SubProcess {
      override def toString(): String = "syscall"
    }
    case class ProcRoot(sink: Instruction) extends SubProcess {
      override def toString(): String = s"root of ${sink}"
      override def equals(x: Any) = x match {
        case px: ProcRoot => (px eq this) || (px.sink == sink)
        case _            => false
      }
      override def hashCode() = sink.hashCode()
    }
    case class InstLeaf(inst: Instruction) extends SubProcess {
      override def toString(): String = inst.toString()
      override def equals(x: Any) = x match {
        case xl: InstLeaf => (xl eq this) || (xl.inst == inst)
        case _            => false
      }
      override def hashCode() = inst.hashCode()
    }

    // the constraint graph, the graph does not need to be directed... but it is
    val constraint_graph =
      scalax.collection.mutable.Graph.empty[SubProcess, GraphEdge.DiEdge]

    // backward traversal from sink nodes that partially creates the constraint
    // graph
    def createConstraints(output_node: dependence_graph.NodeT): ProcRoot = {
      val local_root = ProcRoot(output_node)
      constraint_graph += local_root
      output_node.outerNodeTraverser
        .withDirection(GraphTraversal.Predecessors)
        .foreach { onode =>
          val inst = InstLeaf(onode)
          constraint_graph += GraphEdge.DiEdge(local_root, inst)
          onode match {
            case _: Expect | _: GlobalStore |
                _: GlobalLoad => // create and edge from SysCallRoot to ProcRoot
              constraint_graph += GraphEdge.DiEdge(
                SysCallRoot,
                local_root
              )
            case i @ (_: LocalLoad | _: LocalStore) =>
              i.annons.collectFirst { case annon: Memblock =>
                annon
              } match {
                case None =>
                  ctx.logger.error(s"Expected ${Memblock.name} annotation", i)
                case Some(a) =>
                  val mblock = MemBlockRoot(MemoryBlock.fromAnnotation(a))
                  constraint_graph += GraphEdge.DiEdge(
                    mblock,
                    local_root
                  )
              }
            case _ =>
            // do nothing for now
          }
        }

      local_root
    }

    // run a function and time it
    def timed[T](header: String)(fn_body: => T): T = {
      ctx.logger.info(header)

      val (res, elapsed) = ctx.stats.timed(fn_body)
      ctx.stats.recordRunTime(header, elapsed)
      ctx.logger.info(
        f"took ${elapsed * 1e-3}%.3f seconds"
      )
      ctx.logger.flush()
      res
    }

    // do the backward traversal from sink nodes to build the constraint graph

    val proct_roots = timed("Extracting parallel processes") {
      val res = sink_nodes.map { createConstraints }
      ctx.logger.dumpArtifact(
        s"constraint_graph${ctx.logger.countProgress()}_${transformId}_${proc.id}.dot"
      ) {

        import scalax.collection.io.dot._
        import scalax.collection.io.dot.implicits._

        val dot_root = DotRootGraph(
          directed = true,
          id = Some("Resource dependence graph")
        )

        def nodeTransformer(
            inode: Graph[SubProcess, GraphEdge.DiEdge]#NodeT
        ): Option[(DotGraph, DotNodeStmt)] =
          Some(
            (
              dot_root,
              DotNodeStmt(
                NodeId(inode.toOuter.hashCode().toString()),
                List(DotAttr("label", inode.toOuter.toString.trim))
              )
            )
          )
        val dot_export: String = constraint_graph.toDot(
          dotRoot = dot_root,
          edgeTransformer = iedge =>
            Some(
              (
                dot_root,
                DotEdgeStmt(
                  iedge.edge.source.toOuter.hashCode().toString(),
                  iedge.edge.target.toOuter.hashCode().toString()
                )
              )
            ),
          iNodeTransformer = Some(nodeTransformer),
          cNodeTransformer = Some(nodeTransformer)
        )
        dot_export
      }

      // val leaves = constraint_graph.nodes.filter { inode =>
      //   inode.toOuter match {
      //     case _: InstLeaf => true
      //     case _           => false
      //   }
      // }

      val leaves = constraint_graph.nodes.collect {
        case n if n.toOuter.isInstanceOf[InstLeaf] =>
          n.toOuter.asInstanceOf[InstLeaf].inst
      }
      if (leaves.size != proc.body.size) {
        val left_out = proc.body.toSet[Instruction].diff(leaves).toSeq

        left_out.foreach { i =>
          ctx.logger.warn("removing unused instruction", i)
        }
      }
      // assert(leaves.size == proc.body.size, "no instruction should be left out")
      ctx.logger.info(
        s"Found ${res.size} parallel processes from ${proc.body.size} instruction "
      )
      res
    }

    // proct_roots.foreach { p =>
    //   ctx.logger.info(s"${p.sink} -> ${constraint_graph.get(p).outDegree}")
    // }
    // find weakly connected subgraphs  (masking last level edges and nodes)

    val compatible_processes = timed("Finding compatible processes") {
      val res = constraint_graph
        .componentTraverser(
          subgraphEdges = iedge =>
            iedge.toOuter match {
              case GraphEdge.DiEdge(u: ProcRoot, v: InstLeaf) => false
              case _                                          => true
            },
          subgraphNodes = inode =>
            inode.toOuter match {
              case (_: ProcRoot | _: MemBlockRoot | SysCallRoot) => true
              case _                                             => false
            }
        )
      ctx.logger.info(s"Found ${res.size} compatible processes")
      res
    }

    // combine the instructions in the weakly connected subgraphs
    val process_bodies = timed("Merging incompatible processes") {
      compatible_processes
        .map { connected_components =>
          val mem_nodes = connected_components.nodes.filter(inode =>
            inode.toOuter match {
              case _: MemBlockRoot => true
              case _               => false
            }
          )
          val has_syscall = connected_components.nodes.exists { inode =>
            inode.toOuter == SysCallRoot
          }
          val proc_root_nodes = connected_components.nodes.filter(inode =>
            inode.toOuter match {
              case _: ProcRoot => true
              case _           => false
            }
          )

          val leaf_instructions =
            scala.collection.mutable.Set.empty[Instruction]
          proc_root_nodes.foreach { inode =>
            // println(s"${inode.diSuccessors.size}")
            leaf_instructions ++= inode.diSuccessors.map { ileaf =>
              ileaf.toOuter.asInstanceOf[InstLeaf].inst
            }
          }
          leaf_instructions
        }
    }

    def createProcess(block: Iterable[Instruction], index: Int): DefProcess = {

      val referenced = DependenceAnalysis.referencedNames(block)
      val defRegs = proc.registers.filter { r =>
        referenced.contains(r.variable.name)
      }
      val defLabels = proc.labels.filter { lgrp =>
        referenced.contains(lgrp.memory)
      }

      proc
        .copy(
          body = block.toSeq,
          registers = defRegs,
          labels = defLabels,
          id =
            ProcessIdImpl(s"${proc.id}_${index}", proc.id.x, proc.id.y + index)
        )
        .setPos(proc.pos)

    }
    // create new processes
    val result = timed("constructing merged processes") {
      process_bodies.zipWithIndex.map { case (b, ix: Int) =>
        createProcess(b, ix)
      }
    }
    val longest_process = result.maxBy { _.body.length }
    ctx.logger.info(
      s"Longest process has is ${longest_process.id} with ${longest_process.body.length} instructions"
    )
    // done
    result
  }
  override def transform(
      source: DefProgram
  )(implicit context: AssemblyContext): DefProgram = {
    val splitted = source.processes.flatMap { p =>
      extractIndependentInstructionSequences(p)(context)
    }

    source
      .copy(processes = splitted)
      .setPos(source.pos)
  }
}
