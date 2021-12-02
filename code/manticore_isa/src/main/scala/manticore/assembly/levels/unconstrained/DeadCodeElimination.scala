package manticore.assembly.levels.unconstrained

/** DeadCodeElimination.scala
  *
  * @author
  *   Sahand Kashani <sahand.kashani@epfl.ch>
  */

import manticore.assembly.DependenceGraphBuilder
import manticore.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import manticore.assembly.levels.OutputType

import scalax.collection.Graph
import scalax.collection.edge.LDiEdge

/** This transform identifies dead code and removes it from the design. Dead code
  * is code that does not contribute to the output registers of a program.
  */
object DeadCodeElimination
    extends AssemblyTransformer(UnconstrainedIR, UnconstrainedIR) {

  import UnconstrainedIR._

  object GraphBuilder extends DependenceGraphBuilder(UnconstrainedIR)

  /** Finds all live instructions in a process. An instruction is considered
    * live if there exists a path from the instruction to an output register of
    * the process.
    *
    * @param asm
    *   Target process.
    * @param ctx
    *   Target context.
    * @return
    *   Set of live instructions in the process.
    */
  def findLiveInstrs(
      asm: DefProcess
  )(
      ctx: AssemblyContext
  ): Set[Instruction] = {
    // Output ports of the process.
    val outputNames = asm.registers
      .filter(reg => reg.variable.varType == OutputType)
      .map(reg => reg.variable.name)
      .toSet

    // Instructions that write to the output ports.
    val outputInstrs = asm.body.filter { instr =>
      GraphBuilder.regDef(instr) match {
        case Some(name) => outputNames.contains(name)
        case None       => false
      }
    }

    // Create dependency graph.
    // The value of the label doesn't matter for topological sorting.
    def labelingFunc(pred: Instruction, succ: Instruction) = None
    val dependenceGraph = GraphBuilder.build(asm, labelingFunc)(ctx)

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
          val predInstr = predNode.toOuter
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
    * `fcnt` function tells which registers are to be counted in an instruction.
    *
    * @param instrs
    *   Input instructions.
    * @param cnts
    *   Map associating registers with the number of times they appear in the
    *   instruction stream.
    * @param fcnt
    *   Function returning the registers in an instruction that must be counted.
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
    * @param asm
    *   Target process.
    * @param ctx
    *   Target context.
    * @return
    *   Process after dead-code elimination.
    */
  def dce(
      asm: DefProcess
  )(
      ctx: AssemblyContext
  ): DefProcess = {

    // Remove dead instructions.
    val liveInstrs = findLiveInstrs(asm)(ctx)
    val newBody = asm.body.filter(instr => liveInstrs.contains(instr))

    // Remove dead registers AFTER filtering dead instructions.
    // Eliminating unused registers cannot be done to the fullest extent if it is
    // performed before dead instructions are filtered out as these dead instructions
    // could reference registers which don't lead to the process' output registers.
    val refCounts = countWithFunction(
      newBody,
      (instr: Instruction) => GraphBuilder.regUses(instr)
    )
    val defCounts = countWithFunction(
      newBody,
      (instr: Instruction) => GraphBuilder.regDef(instr).toSeq
    )
    val newRegs = asm.registers.filter { reg =>
      val isReferenced = refCounts(reg.variable.name) != 0
      val isDefined = defCounts(reg.variable.name) != 0
      isReferenced || isDefined
    }

    asm.copy(
      body = newBody,
      registers = newRegs
    )
  }

  override def transform(
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
