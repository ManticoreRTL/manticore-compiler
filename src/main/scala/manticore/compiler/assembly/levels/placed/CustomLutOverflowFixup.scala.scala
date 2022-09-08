package manticore.compiler.assembly.levels.placed

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.DependenceGraphBuilder
import manticore.compiler.assembly.levels.CanCollectProgramStatistics

import scala.collection.mutable.{Map => MMap, Set => MSet}
import manticore.compiler.assembly.BinaryOperator
import scala.collection.mutable.ArrayBuffer
import manticore.compiler.assembly.levels.ConstType
import manticore.compiler.assembly.levels.WireType

// This pass reverts excessive custom functions in processes back into standard instructions.
// We revert the custom functions that result in the smallest number of additional instructions that get added
// to the program.
object CustomLutOverflowFixup extends DependenceGraphBuilder with PlacedIRTransformer with CanCollectProgramStatistics {

  val flavor = PlacedIR
  import flavor._

  import CustomFunctionImpl._

  def freshConstName(implicit
      ctx: AssemblyContext
  ): Name = {
    s"%c${ctx.uniqueNumber()}"
  }

  def freshWireName(implicit
      ctx: AssemblyContext
  ): Name = {
    s"%w${ctx.uniqueNumber()}"
  }

  def undoCustomInstruction(
      // Custom instruction to undo.
      ci: CustomInstruction,
      // Constants that already exist.
      // We use collection.Map instead of `Map` as we want to pass either a mutable or immutable Map here.
      consts: collection.Map[Constant, Name],
      // Functions available in the enclosing process.
      funcs: Map[Name, CustomFunction]
  )(implicit
      ctx: AssemblyContext
  ): (
      // Instructions that replace the custom function.
      Seq[Instruction],
      // New constants used by the custom function. These constants did not already exist in the list of input constants provided to this function.
      Map[Constant, Name],
      // New intermediate wires defined by the custom function.
      Set[Name]
  ) = {

    // Name is defined by the Instruction.
    val instrs = MMap.empty[Name, Instruction]

    val newConsts = MMap.empty[Constant, Name]

    // Need to declare wires for the intermediate results that were hidden in the custom function.
    val newWires = MSet.empty[Name]

    def exprTreeToName(
        expr: ExprTree
    )(implicit
        ctx: AssemblyContext
    ): Name = {

      // ExprTree is a tree. This means the root node is the last operation to be executed.
      expr match {
        case IdExpr(id) =>
          id match {
            case PositionalArg(p) =>
              ci.rsx(p)

            case AtomConst(v) =>
              consts.get(v) match {
                case None =>
                  // The constant does not exist under a known name, so we need a new name for it.
                  // Before creating a name, first check in the list of new constants to see if it already
                  // exists as another branch in the exprTree might have already created the constant.
                  newConsts.get(v) match {
                    case None =>
                      // The constant really doesn't exist. We create one and record it.
                      val resName = freshConstName
                      newConsts += v -> resName
                      resName

                    case Some(name) =>
                      // Some other branch in the exprTree created the constant already. We reuse its name.
                      name
                  }

                case Some(name) =>
                  // The constant was already defined, so we reuse its existing name.
                  name
              }

            case NamedArg(name) =>
              // Functions do not have named args directly (only positional args and constants). The positional args
              // need to be replaced by names when the expression is used in a custom instruction.
              ctx.logger.fail(s"Encountered a positional arg in a custom function!")
          }

        case AndExpr(op1, op2) =>
          val op1Name = exprTreeToName(op1)
          val op2Name = exprTreeToName(op2)
          val resName = freshWireName
          val instr   = BinaryArithmetic(BinaryOperator.AND, resName, op1Name, op2Name)
          newWires += resName
          instrs += resName -> instr
          resName

        case OrExpr(op1, op2) =>
          val op1Name = exprTreeToName(op1)
          val op2Name = exprTreeToName(op2)
          val resName = freshWireName
          val instr   = BinaryArithmetic(BinaryOperator.OR, resName, op1Name, op2Name)
          newWires += resName
          instrs += resName -> instr
          resName

        case XorExpr(op1, op2) =>
          val op1Name = exprTreeToName(op1)
          val op2Name = exprTreeToName(op2)
          val resName = freshWireName
          val instr   = BinaryArithmetic(BinaryOperator.XOR, resName, op1Name, op2Name)
          newWires += resName
          instrs += resName -> instr
          resName
      }
    }

    // This populates the `instrs` and `newConsts` maps.
    val rootName = exprTreeToName(funcs(ci.func).expr)

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
    // We replace the name that the last instruction defines by the root name of the
    // custom function such that the caller doesn't need to change references in the rest
    // of the program.
    val newLastInstr = instrsOrdered.last match {
      case BinaryArithmetic(operator, rd, rs1, rs2, annons) =>
        // ci.rd is the old root name used by the custom instruction.
        // We also remove the temporary wire name that was construction for the instruction as we are
        // no longer using it.
        newWires -= rd
        BinaryArithmetic(operator, ci.rd, rs1, rs2, annons)
      case _ =>
        ctx.logger.fail(s"Found non-BinaryArithmetic instruction when unrolling a custom instruction.")
    }

    val newInstrsOrdered = instrsOrdered.dropRight(1) :+ newLastInstr

    (newInstrsOrdered, newConsts.toMap, newWires.toSet)
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

      val procConsts = proc.registers.collect { case DefReg(ValueVariable(name, _, ConstType), value, _) =>
        name -> value.get
      }.toMap

      val procFuncs = proc.functions.map { defFunc =>
        defFunc.name -> defFunc.value
      }.toMap

      val totalInstrsCoveredByFuncs: Map[Name, Int] = proc.body
        .collect { case CustomInstruction(funcName, rd, rsx, annons) => funcName }
        .groupBy { funcName => funcName }
        .map { case (funcName, group) =>
          val numCalls      = group.size
          val funcResources = procFuncs(funcName).resources
          // We only look for Right(binOp) as Left(const) doesn't add an instruction (just storage space
          // in the register file).
          val instrsPerCall = funcResources.collect { case (Right(binOp), cnt) => cnt }.sum
          funcName -> numCalls * instrsPerCall
        }

      // Select least significant functions to remove.
      val numFuncsToRemove = numCustomFunctions - ctx.hw_config.nCustomFunctions

      val funcNamesToRemove = totalInstrsCoveredByFuncs.toSeq
        .sortBy { case (funcName, numInstrs) =>
          numInstrs
        }
        .map { case (funcName, numInstrs) =>
          funcName
        }
        .take(numFuncsToRemove)

      ctx.logger.info {
        val overflowFuncsStr = funcNamesToRemove
          .map { funcName =>
            s"\tOverflow func ${funcName} covers ${totalInstrsCoveredByFuncs(funcName)} total instructions"
          }
          .mkString("\n")

        s"Removing ${numFuncsToRemove} overflow funcs in process ${proc.id}:\n${overflowFuncsStr}"
      }

      // We need to replace a subset of the custom instructions in the process body.
      // Removing a custom instruction may introduce new constants (constants that were unseen until now).
      //
      // To replace instructions in the body you may think of using flatMap and simply replacing one
      // custom instruction with a sequence of instructions, but this will not allow subsequent calls to
      // `undoCustomInstruction` to see that new constants have been introduced.
      //
      // So we instead use `foreach` and keep a mutable data structure for the constants so future calls of
      // `undoCustomInstruction` see the updated constants.

      val finalBody   = ArrayBuffer.empty[Instruction]
      val finalRegs   = ArrayBuffer.from(proc.registers)
      val constToName = MMap.from(procConsts.map(_.swap))

      proc.body.foreach {
        case ci @ CustomInstruction(funcName, _, _, _) if funcNamesToRemove.contains(funcName) =>
          val (undoInstrs, undoConsts, undoWires) = undoCustomInstruction(ci, constToName, procFuncs)
          // Replace the single custom instruction with a sequence of instructions.
          finalBody ++= undoInstrs
          // Add any new constants generated to the map of constants.
          constToName ++= undoConsts
          finalRegs ++= undoConsts.map { case (value, name) => DefReg(ValueVariable(name, -1, ConstType), Some(value)) }
          // Add any new wires generated to the set of names in the process.
          finalRegs ++= undoWires.map { name => DefReg(ValueVariable(name, -1, WireType)) }

        case other =>
          // The instruction is not marked to be modified, so we just add it as-is to the new body.
          // No constant has been added here.
          finalBody += other
      }

      val finalFuncs = proc.functions.filter(f => !funcNamesToRemove.contains(f.name))

      val newProc = proc.copy(
        body = finalBody.toSeq,
        registers = finalRegs.toSeq,
        functions = finalFuncs.toSeq
      )

      newProc
    }
  }

  override def transform(
      program: DefProgram
  )(implicit ctx: AssemblyContext): DefProgram = {
    program.copy(
      processes = program.processes.map(proc => onProcess(proc))
    )
  }
}
