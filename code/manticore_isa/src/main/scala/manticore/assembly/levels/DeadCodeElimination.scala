package manticore.assembly.levels

/** DeadCodeElimination.scala
  *
  * @author
  *   Sahand Kashani <sahand.kashani@epfl.ch>
  */

import manticore.assembly.DependenceGraphBuilder
import manticore.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import manticore.assembly.levels.OutputType
import manticore.assembly.ManticoreAssemblyIR

import scalax.collection.Graph
import scalax.collection.edge.LDiEdge

/** This transform identifies dead code and removes it from the design. Dead
  * code is code that does not contribute to the output registers of a program.
  */
abstract class DeadCodeElimination[T <: ManticoreAssemblyIR](irFlavor: T)
    extends AssemblyTransformer(irFlavor, irFlavor) {

  private object Impl {

    import irFlavor._

    // The value of the label doesn't matter for topological sorting.
    // Must use T#Instruction instead of irFlavor.Instruction as GraphBuilder requires knowledge
    // of the type itself and cannot use the instance variable irFlavor to infer the type.
    private def labelingFunc(pred: T#Instruction, succ: T#Instruction) = None
    object GraphBuilder extends DependenceGraphBuilder(irFlavor)

    /** Finds all live instructions in a process. An instruction is considered
      * live if there exists a path from the instruction to an output register
      * of the process.
      *
      * @param proc
      *   Target process.
      * @param ctx
      *   Target context.
      * @return
      *   Set of live instructions in the process.
      */
    def findLiveInstrs(
        proc: DefProcess
    )(
        ctx: AssemblyContext
    ): Set[Instruction] = {
      // Output ports of the process.
      val outputNames = proc.registers
        .filter(reg => reg.variable.varType == OutputType)
        .map(reg => reg.variable.name)
        .toSet

      // Instructions that write to the output ports.
      val outputInstrs = proc.body.filter { instr =>
        // The cast is needed to extract the T#-like type returned from GraphBuilder into irFlavor.
        GraphBuilder.regDef(instr).asInstanceOf[Option[Name]] match {
          case Some(name) => outputNames.contains(name)
          case None       => false
        }
      }

      // Create dependency graph.
      val dependenceGraph = GraphBuilder.build(proc, labelingFunc)(ctx)

      // Cache of backtracking results to avoid exponential lookup if we end up
      // going up the same tree multiple times.
      val visitedInstrs = collection.mutable.Set.empty[Instruction]

      def findLiveNodesIter(
          dstInstr: Instruction
      ): Unit = {
        // No need to backtrack if we've already encountered this vertex.
        if (!visitedInstrs.contains(dstInstr)) {
          visitedInstrs.add(dstInstr)

          val dstNode = dependenceGraph.get(dstInstr)
          val predNodes = dstNode.diPredecessors
          predNodes.foreach { predNode =>
            // The cast is needed to extract the T#-like type returned from GraphBuilder into irFlavor.
            val predInstr = predNode.toOuter.asInstanceOf[Instruction]
            findLiveNodesIter(predInstr)
          }
        }
      }

      // Backtrack from the output instructions to find all live nodes in the graph.
      outputInstrs.foreach { outputInstr =>
        findLiveNodesIter(outputInstr)
      }

      visitedInstrs.toSet
    }

    /** Counts the number of times registers are present in an instruction. The
      * `fcnt` function tells which registers are to be counted in an
      * instruction.
      *
      * @param instrs
      *   Input instructions.
      * @param cnts
      *   Map associating registers with the number of times they appear in the
      *   instruction stream.
      * @param fcnt
      *   Function returning the registers in an instruction that must be
      *   counted.
      * @return
      *   Number of times the register was counted in the instruction stream.
      */
    def countWithFunction(
        instrs: Seq[Instruction],
        fcnt: Instruction => Seq[Name],
        cnts: Map[Name, Int] = Map.empty.withDefaultValue(0)
    ): Map[Name, Int] = {
      instrs match {
        case Nil =>
          cnts

        case head +: tail =>
          val regs = fcnt(head)

          val newCnts = regs.foldLeft(cnts) { case (oldCnts, reg) =>
            val newCnt = oldCnts.get(reg) match {
              case Some(oldCnt) => oldCnt + 1
              case None         => 1
            }
            oldCnts + (reg -> newCnt)
          }

          countWithFunction(tail, fcnt, newCnts)
      }
    }

    /** Performs dead-code elimination.
      *
      * @param proc
      *   Target process.
      * @param ctx
      *   Target context.
      * @return
      *   Process after dead-code elimination.
      */
    def dce(
        proc: DefProcess
    )(
        ctx: AssemblyContext
    ): DefProcess = {

      // Remove dead instructions.
      val liveInstrs = findLiveInstrs(proc)(ctx)
      val newBody = proc.body.filter(instr => liveInstrs.contains(instr))

      // Remove dead registers AFTER filtering dead instructions.
      // Eliminating unused registers cannot be done to the fullest extent if it is
      // performed before dead instructions are filtered out as these dead instructions
      // could reference registers which don't lead to the process' output registers.
      val refCounts = countWithFunction(
        newBody,
        // The cast is needed to extract the T#-like type returned from GraphBuilder into irFlavor.
        (instr: Instruction) => GraphBuilder.regUses(instr).asInstanceOf[Seq[Name]]
      )
      val defCounts = countWithFunction(
        newBody,
        // The cast is needed to extract the T#-like type returned from GraphBuilder into irFlavor.
        (instr: Instruction) => GraphBuilder.regDef(instr).asInstanceOf[Option[Name]].toSeq
      )
      val newRegs = proc.registers.filter { reg =>
        val isReferenced = refCounts(reg.variable.name) != 0
        val isDefined = defCounts(reg.variable.name) != 0
        isReferenced || isDefined
      }

      val newProc = proc.copy(
        body = newBody,
        registers = newRegs
      )

      // Debug dump graph.
      logger.dumpArtifact(
        s"dependence_graph_${getName}_${newProc.id}_${ctx.transform_index}.dot"
        ) {
        val dp = GraphBuilder.build(newProc, labelingFunc)(ctx)

        import scalax.collection.io.dot._
        import scalax.collection.io.dot.implicits._

        val dot_root = DotRootGraph(
          directed = true,
          id = Some("List scheduling dependence graph")
        )
        def edgeTransform(
            iedge: Graph[T#Instruction, LDiEdge]#EdgeT
        ): Option[(DotGraph, DotEdgeStmt)] = iedge.edge match {
          case LDiEdge(source, target, l) =>
            Some(
              (
                dot_root,
                DotEdgeStmt(
                  source.toOuter.hashCode().toString,
                  target.toOuter.hashCode().toString,
                  List(DotAttr("label", 0))
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
            inode: Graph[T#Instruction, LDiEdge]#NodeT
        ): Option[(DotGraph, DotNodeStmt)] =
          Some(
            (
              dot_root,
              DotNodeStmt(
                NodeId(inode.toOuter.hashCode().toString()),
                List(DotAttr("label", inode.toOuter.serialized.trim))
              )
            )
          )

        val dot_export: String = dp.toDot(
          dotRoot = dot_root,
          edgeTransformer = edgeTransform,
          cNodeTransformer = Some(nodeTransformer)
        )
        dot_export
      }(ctx)

      newProc
    }

    def apply(
        asm: DefProgram,
        context: AssemblyContext
    ): DefProgram = {
      implicit val ctx = context

      val out = DefProgram(
        processes = asm.processes.map(process => dce(process)(ctx)),
        annons = asm.annons
      )

      if (logger.countErrors > 0) {
        logger.fail(s"Failed transform due to previous errors!")
      }

      out
    }
  }

  // Note that we use `T#` for the method signature instead of `irFlavor._` as irFlavor is private to
  // this instance and cannot escape it (as the return value for example).
  override def transform(
      asm: T#DefProgram,
      context: AssemblyContext
  ): T#DefProgram = {
    val asmIn = asm.asInstanceOf[irFlavor.DefProgram]
    val asmOut = Impl(asmIn, context)
    asmOut
  }
}
