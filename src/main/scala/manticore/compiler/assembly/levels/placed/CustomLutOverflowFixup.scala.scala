package manticore.compiler.assembly.levels.placed

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.DependenceGraphBuilder
import manticore.compiler.assembly.levels.CanCollectProgramStatistics

import scala.collection.mutable.{Map => MMap}
import manticore.compiler.assembly.BinaryOperator
import scala.collection.mutable.ArrayBuffer

// This pass reverts excessive custom functions in processes back into standard instructions.
// We revert the custom functions that result in the smallest number of additional instructions that get added
// to the program.
object CustomLutOverflowFixup extends DependenceGraphBuilder with PlacedIRTransformer with CanCollectProgramStatistics {

  val flavor = PlacedIR
  import flavor._

  import CustomFunctionImpl._

  def undoCustomInstruction(
      proc: DefProcess,
      ci: CustomInstruction
  )(implicit
      ctx: AssemblyContext
  ): (
      Seq[Instruction], // Instructions that replace the custom function.
      Seq[DefReg]       // Constants used by the custom function.
  ) = {

    // Name is defined by the DefReg.
    val consts = MMap.empty[Name, DefReg]

    // Name is defined by the Instruction.
    val instrs = MMap.empty[Name, Instruction]

    def exprTreeToName(
        expr: ExprTree
    )(implicit
        ctx: AssemblyContext
    ): Name = {

      def freshName(implicit
          ctx: AssemblyContext
      ): Name = {
        s"%w${ctx.uniqueNumber()}"
      }

      // ExprTree is a tree. This means the root node is the last operation to be executed.
      expr match {
        case IdExpr(id) =>
          id match {
            case NamedArg(n) =>
              // This is a named parameter, so it already has a name and we just return it.
              n

            case AtomConst(v) =>
              // Create a new DefReg for the constant.
              val reg = PlacedIRConstantFolding.freshConst(v)
              consts += reg.variable.name -> reg
              reg.variable.name

            case PositionalArg(p) =>
              // Will never come here as custom functions can only reference named args or constants.
              ctx.logger.fail(s"Encountered a positional arg in a custom function!")
          }

        case AndExpr(op1, op2) =>
          val op1Name = exprTreeToName(op1)
          val op2Name = exprTreeToName(op2)
          val resName = freshName
          val instr   = BinaryArithmetic(BinaryOperator.AND, resName, op1Name, op2Name)
          instrs += resName -> instr
          resName

        case OrExpr(op1, op2) =>
          val op1Name = exprTreeToName(op1)
          val op2Name = exprTreeToName(op2)
          val resName = freshName
          val instr   = BinaryArithmetic(BinaryOperator.OR, resName, op1Name, op2Name)
          instrs += resName -> instr
          resName

        case XorExpr(op1, op2) =>
          val op1Name = exprTreeToName(op1)
          val op2Name = exprTreeToName(op2)
          val resName = freshName
          val instr   = BinaryArithmetic(BinaryOperator.XOR, resName, op1Name, op2Name)
          instrs += resName -> instr
          resName
      }
    }

    val funcExpr = proc.functions.find(f => f.name == ci.func).get.value.expr

    // This populates the `instrs` and `consts` maps.
    val rootName = exprTreeToName(funcExpr)

    // We order the instructions before returning so the caller can just replace
    // the call to the custom function with a sequence of instructions such that
    // the final program order is still valid.
    val instrsOrdered = PlacedIROrderInstructions.InstructionOrder.reorder(instrs.values).toSeq

    // A custom function is a tree and has a root name that stores the result of
    // its execution.
    //
    //  Ex: cust rd, rs1, rs2, ...
    //           ^^
    //           original root name
    //
    // We replace the name defined by the last instruction by the root name of the
    // custom function so the caller doesn't need to change references in the rest
    // of the program.
    val newLastInstr = instrsOrdered.last match {
      case BinaryArithmetic(operator, rd, rs1, rs2, annons) =>
        // ci.rd is the old root name used by the custom instruction.
        BinaryArithmetic(operator, ci.rd, rs1, rs2, annons)
      case _ =>
        ctx.logger.fail(s"Found non-BinaryArithmetic instruction when unrolling a custom instruction.")
    }

    val newInstrsOrdered = instrsOrdered.dropRight(1) :+ newLastInstr

    (newInstrsOrdered, consts.values.toSeq)
  }

  def onProcess(
      proc: DefProcess
  )(implicit ctx: AssemblyContext): DefProcess = {
    val numCustomFunctions = proc.functions.length

    if (numCustomFunctions <= ctx.hw_config.nCustomFunctions) {
      // The number of custom functions is valid. We don't need to undo any work.
      proc

    } else {
      // There are more custom functions than there are custom function units in HW.
      // We need to undo some custom functions. We choose the custom functions that
      // result in the least number of instructions that would be added to the process
      // when undone.

      val totalInstrsCoveredByFuncs: Map[Name, Int] = proc.body
        .collect { case CustomInstruction(funcName, rd, rsx, annons) => funcName }
        .groupBy { funcName => funcName }
        .map { case (funcName, group) =>
          val numCalls      = group.size
          val funcResources = proc.functions.find(f => f.name == funcName).get.value.resources
          // We only look for Right(binOp) as Left(const) doesn't add an instruction (just storage space
          // in the register file).
          val instrsPerCall = funcResources.collect { case (Right(binOp), cnt) => cnt }.sum
          funcName -> numCalls * instrsPerCall
        }

      // Select least significant functions to remove.
      val numFuncsToRemove = numCustomFunctions - ctx.hw_config.nCustomFunctions
      val funcsToRemove = totalInstrsCoveredByFuncs.toSeq
        .sortBy { case (funcName, numInstrs) =>
          numInstrs
        }
        .take(numFuncsToRemove)

      ctx.logger.info {
        val overflowFuncsStr = funcsToRemove
          .map { case (funcName, numInstrs) =>
            s"\tOverflow func ${funcName} covers ${numInstrs} total instructions"
          }
          .mkString("\n")

        s"Removing ${numFuncsToRemove} overflow funcs in process ${proc.id}:\n${overflowFuncsStr}"
      }

      val newConsts = ArrayBuffer.empty[DefReg]

      val newBody = proc.body.flatMap { instr =>
        instr match {
          case ci: CustomInstruction =>
            val (undoInstrs, undoConsts) = undoCustomInstruction(proc, ci)
            // Replace the single custom instruction with a sequence of instructions.
            newConsts ++= undoConsts
            undoInstrs

          case other => Seq(other)
        }
      }

      val newProc = proc.copy(
        registers = proc.registers ++ newConsts,
        body = newBody
      )

      newProc
    }
  }

  override def transform(
      program: DefProgram
  )(implicit ctx: AssemblyContext): DefProgram = {
    program
  }
}
