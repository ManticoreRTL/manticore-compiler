package manticore.compiler.assembly.levels

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.BinaryOperator

import manticore.compiler.assembly.levels.CanRename

trait CommonSubexpressionElimination
    extends Flavored
    with CanRename
    with ProgramStatCounter {

  import flavor._

  import BinaryOperator._

  sealed abstract class Expression

  case class BinOpExpr(op: BinaryOperator, rs1: Name, rs2: Name)
      extends Expression
  case class MuxExpr(sel: Name, rfalse: Name, rtrue: Name) extends Expression
  case class AddCarryExpr(rs1: Name, rs2: Name, ci: Name) extends Expression

  class Eliminator(proc: DefProcess)(implicit ctx: AssemblyContext) {

    val m_subst = scala.collection.mutable.Map.empty[Name, Name]
    val m_expr_log = scala.collection.mutable.Map.empty[Expression, Name]
    val m_bindings = scala.collection.mutable.Map.empty[Name, Expression]

    val m_defs = proc.registers.map { r => r.variable.name -> r }.toMap

    val m_insts = scala.collection.mutable.Queue.empty[Instruction]
    val m_regs = scala.collection.mutable.Set.empty[DefReg]

    def getName(n: Name): Name = m_subst.getOrElse(n, n)

    def keep(instruction: Instruction): Unit = {
      def renaming(n: Name): Name = {
        m_regs += m_defs(n)
        m_subst.getOrElse(n, n)
      }
      val renamed_inst = asRenamed(instruction)(renaming)
      m_insts += renamed_inst
    }

    def bind(mapping: (Name, Name)): Unit = {
      assert(m_subst.contains(mapping._1) == false)
      m_subst += mapping
    }

    def available(expr: Expression): Option[Name] = m_expr_log.get(expr)

    def record(binding: (Expression, Name)): Unit = {
      assert(m_expr_log.contains(binding._1) == false)
      m_expr_log += binding
    }

    def build(): DefProcess = {
      proc.copy(
        body = m_insts.dequeueAll(_ => true).toSeq,
        registers = m_regs.toSeq
      )
    }

  }

  def do_transform(prog: DefProgram)(implicit
      ctx: AssemblyContext
  ): DefProgram = {

    val results = prog.copy(
      processes = prog.processes.map(do_transform)
    )
    ctx.stats.record {
      mkProgramStats(results)
    }
    results
  }
  private def do_transform(
      proc: DefProcess
  )(implicit ctx: AssemblyContext): DefProcess = {

    proc.body
      .foldLeft(new Eliminator(proc)) { case (cse, inst) =>
        inst match {
          case Mux(rd, sel, rfalse, rtrue, _) =>
            val sel_name = cse getName sel
            val false_name = cse getName rfalse
            val true_name = cse getName rtrue

            val expr = MuxExpr(sel_name, false_name, true_name)
            cse.available(expr) match {
              case Some(bound_name) => cse bind (rd -> bound_name)
              case None => // keep the instruction, it's a fresh expression
                cse keep inst
                cse record (expr -> rd)
            }
          case BinaryArithmetic(op, rd, rs1, rs2, _) =>
            val rs1_name = cse getName rs1
            val rs2_name = cse getName rs2
            val expr1 = BinOpExpr(op, rs1_name, rs2_name)

            cse available expr1 match {
              case Some(bound_name) => cse bind (rd -> bound_name)
              case None =>
                op match {
                  case ADD | AND | XOR | OR | MUL | SEQ =>
                    // commutative operators
                    val expr2 = BinOpExpr(op, rs2_name, rs1_name)
                    cse available expr2 match {
                      case Some(bound_name) => cse bind (rd -> bound_name)
                      case None             =>
                        // keep the instruction and record a new expr to name binding
                        cse keep inst
                        cse record (expr1 -> rd) // does not matter which expr we keep

                    }
                  case _ =>
                    // non-commutative operators
                    cse keep inst
                    cse record (expr1 -> rd)
                }
            }
          case _ =>
            cse keep inst

        }
        cse
      }
      .build()

  }

}
