package manticore.compiler.assembly.levels

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.BinaryOperator

import manticore.compiler.assembly.levels.CanRename
import manticore.compiler.assembly.CanComputeNameDependence
import manticore.compiler.assembly.annotations.Track

/** @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */
trait CommonSubExpressionElimination
    extends Flavored
    with CanRename
    with CanCollectProgramStatistics
    with CanComputeNameDependence {

  import flavor._

  import BinaryOperator._

  sealed abstract class Expression

  case class BinOpExpr(op: BinaryOperator, rs1: Name, rs2: Name)
      extends Expression
  case class MuxExpr(sel: Name, rfalse: Name, rtrue: Name) extends Expression
  case class AddCarryExpr(rs1: Name, rs2: Name, ci: Name) extends Expression

  case class EliminationContext(
      noOpt: Name => Boolean,
      expressions: Map[Expression, Name] = Map.empty[Expression, Name],
      substitutions: Map[Name, Name] = Map.empty[Name, Name],
      keptInstructions: Seq[Instruction] = Seq.empty[Instruction]
  ) {

    def getName(n: Name): Name = substitutions.getOrElse(n, n)

    def bind(mapping: (Name, Name)): EliminationContext = {
      assert(
        substitutions.contains(mapping._1) == false,
        "can not double bind!"
      )
      copy(substitutions = substitutions + mapping)
    }
    def available(expr: Expression): Option[Name] = expressions get expr

    def record(
        binding: (Instruction, (Expression, Name))
    ): EliminationContext = {
      assert(expressions.contains(binding._2._1) == false)
      copy(
        expressions = expressions + binding._2,
        keptInstructions = keptInstructions :+ binding._1
      )
    }
    def keep(inst: Instruction): EliminationContext = copy(
      keptInstructions = keptInstructions :+ inst
    )

    def withNewScope(
        availExprs: Map[Expression, Name] = expressions,
        instructions: Seq[Instruction] = Seq.empty[Instruction]
    ): EliminationContext =
      copy(keptInstructions = instructions, expressions = availExprs)

  }

  def cseBlock(cseCtx: EliminationContext, block: Seq[Instruction])(implicit
      ctx: AssemblyContext
  ): EliminationContext = {
    block.foldLeft(cseCtx) { case (cse, inst) =>
      inst match {
        case Mux(rd, sel, rfalse, rtrue, _) if !cse.noOpt(rd) =>
          val selName = cse getName sel
          val falseName = cse getName rfalse
          val trueName = cse getName rtrue

          val expr = MuxExpr(selName, falseName, trueName)
          cse available expr match {
            case Some(boundName) => cse bind (rd -> boundName)
            case None => // keep the instruction, it's a fresh expression
              cse record (inst -> (expr -> rd))
          }

        case BinaryArithmetic(op, rd, rs1, rs2, _) if !cse.noOpt(rd) =>
          val rs1Name = cse getName rs1
          val rs2Name = cse getName rs2
          val expr1 = BinOpExpr(op, rs1Name, rs2Name)

          cse available expr1 match {
            case Some(bound_name) => cse bind (rd -> bound_name)
            case None =>
              op match {
                case ADD | AND | XOR | OR | MUL | SEQ =>
                  // commutative operators
                  val expr2 = BinOpExpr(op, rs2Name, rs1Name)
                  cse available expr2 match {
                    case Some(bound_name) => cse bind (rd -> bound_name)
                    case None             =>
                      // keep the instruction and record a new expr to name binding
                      cse record (inst -> (expr1 -> rd))
                    // does not matter which expr we keep, i.e., expr1 or expr

                  }
                case _ =>
                  // non-commutative operators
                  cse record (inst -> (expr1 -> rd))
              }
          }

        case jtb @ JumpTable(_, _, blocks, dslot, _) =>
          // we treat delay slot as if it was part of the original program body
          val afterDelaySlot = cseBlock(cse, dslot)
          val availExpressions = afterDelaySlot.expressions
          // removes the instructions seen so far but keeps
          // the available expressions up to now
          case class CaseCseBuilder(
              prev: EliminationContext,
              cases: Seq[JumpCase] = Seq.empty[JumpCase]
          )

          val CaseCseBuilder(newCtx, newCases) = blocks.foldLeft(
            CaseCseBuilder(afterDelaySlot.withNewScope(availExpressions))
          ) { case (CaseCseBuilder(prev, cases), JumpCase(lbl, blk)) =>
            val eliminated = cseBlock(prev, blk)
            CaseCseBuilder(
              eliminated.withNewScope(
                availExpressions
              ), // reset the available expression to the ones seen
              // before the previous case and remove the instruction kept in the log
              cases :+ JumpCase(
                lbl,
                eliminated.keptInstructions
              )
            )
          }

          val optJtb = jtb.copy(
            blocks = newCases,
            dslot =
              Seq.empty[DataInstruction] // dslot should only be used after scheduling where optimizations are not
            // called anymore, so it makes sense to put them back in the original program body
          )

          newCtx withNewScope (availExpressions, afterDelaySlot.keptInstructions :+ optJtb)

        case _ =>
          cse keep inst

      }
    }
  }

  // def emptyCseContext = EliminationContext(
  //   expressions = Map.empty[Expression, Name],
  //   substitutions = Map.empty[Name, Name],
  //   keptInstructions = Seq.empty[Instruction]
  // )
  def cseProcess(
      process: DefProcess
  )(implicit ctx: AssemblyContext): DefProcess = {
    def hasTrack(r: DefReg): Boolean = r.annons.exists { _.isInstanceOf[Track] }
    val noOpt = process.registers.collect {
      case r if r.variable.varType == OutputType || hasTrack(r) =>
        r.variable.name
    }.toSet
    val cseCtx = cseBlock(
      EliminationContext(noOpt),
      process.body
    )
    val subst: Name => Name = cseCtx.substitutions orElse { n => n }
    // rename the instructions by the given substitution function
    val finalInstructions = cseCtx.keptInstructions.map {
      Rename.asRenamed(_)(subst)
    }

    val referenced = NameDependence.referencedNames(finalInstructions)

    def isIo(r: DefReg): Boolean =
      r.variable.varType == InputType || r.variable.varType == OutputType

    val newDefs: Seq[DefReg] = process.registers.filter { r =>
      referenced(r.variable.name) || isIo(r)
    }

    // val newLabesl = process.labels // should not change, cse does not touch memories

    process
      .copy(
        body = finalInstructions,
        registers = newDefs
      )
      .setPos(process.pos)

  }

  def do_transform(
      prog: DefProgram
  )(implicit ctx: AssemblyContext): DefProgram = {

    val res = prog.copy(
      processes = prog.processes.map(cseProcess)
    )
    ctx.stats.record(ProgramStatistics.mkProgramStats(res))
    res
  }

}
