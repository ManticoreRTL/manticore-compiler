package manticore.compiler.assembly.levels

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.BinaryOperator
import manticore.compiler.assembly.annotations.Track
import manticore.compiler.assembly.levels.CanRename

/** Base trait to implement a constant folding pass in
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */
trait ConstantFolding extends Flavored with StatisticReporter with CanRename {

  import flavor._

  def do_transform(
      prog: DefProgram
  )(implicit ctx: AssemblyContext): DefProgram = {

    reportStats(prog)

    val re = prog.copy(
      processes = prog.processes.map(do_transform)
    )
    reportStats(re)
    re
  }

  /** Constant zero and one, override them with their corresponding values in
    * the given flavor
    *
    * @return
    */
  val ConstOne: Constant
  val ConstZero: Constant

  /** A partial function to either fully or partially evaluate binary operators.
    *
    * The partial function should only be defined for input combinations that
    * actually evaluate to something that is simpler that the single
    * instruction.
    * I.e., it should either fully evaluate to a constant value or determine an
    * aliasing relation. In the former case a Right value is returned and in the
    * latter a Left value.
    *
    * @return
    */
  val binOpEvaluator: PartialFunction[
    (
        BinaryOperator.BinaryOperator, // operator
        Either[Name, Constant], // rs1
        Either[Name, Constant] // rs2
    ),
    Either[Name, Constant] // rd
  ]

  /** A total function that fully evaluates and AddCarry instruction
    *
    * @param rs1
    * @param rs2
    * @param ci
    * @param ctx
    * @return
    */
  def addCarryEvaluator(
      rs1: Constant,
      rs2: Constant,
      ci: Constant
  )(implicit ctx: AssemblyContext): (Constant, Constant)

  /** returns a fresh constant (unique name), the value [[v]] given to the
    * function is unique too but can not be used to create the unique name
    *
    * @param v
    * @param ctx
    * @return
    */
  def freshConst(v: Constant)(implicit ctx: AssemblyContext): DefReg

  /** Helper stateful class to create a reduced process
    *
    * @param proc
    * @param ctx
    */
  class ProcessBuilder(proc: DefProcess)(implicit ctx: AssemblyContext) {

    val m_constants = scala.collection.mutable.Map.empty[Constant, DefReg] ++
      proc.registers.collect {
        case r @ DefReg(v, Some(value), _) if v.varType == ConstType =>
          value -> r
      }

    val m_unoptimizable = scala.collection.mutable.Set.empty[Name] ++
      proc.registers.collect {
        case r: DefReg if r.annons.exists {
              case _: Track => true
              case _        => false
            } =>
          r.variable.name
        case r @ DefReg(v, _, _) if v.varType == OutputType => r.variable.name
      }

    /** Returns true if the name n can not be optimized out
      *
      * @param n
      * @return
      */
    def isUnopt(n: Name): Boolean = m_unoptimizable.contains(n)

    val m_evaluations = scala.collection.mutable.Map.empty[Name, Constant] ++
      m_constants.map { case (v, r) => r.variable.name -> v }
    val m_substs = scala.collection.mutable.Map.empty[Name, Name]
    val m_insts = scala.collection.mutable.Queue.empty[Instruction]

    /** Returns computed value or an alias of the given name (or the name
      * itself)
      *
      * @param name
      * @return
      */
    def computedValue(name: Name): Either[Name, Constant] = {

      m_evaluations.get(name) match {
        // if the name is computed to a constant return the constant
        case Some(v) => Right(v)
        case None =>
          m_substs.get(name) match {
            // if the name is computed to an alias return the alias
            case Some(m) => Left(m)
            // if the name has not been computed to anything, return itself
            case None => Left(name)
          }
      }

    }

    val m_regs = proc.registers.map { r => r.variable.name -> r }.toMap

    val m_kept_regs = scala.collection.mutable.Set.empty[DefReg]

    /** Keep the given instruction
      *
      * @param inst
      */
    def keep(inst: Instruction): Unit = {
      def substitution(n: Name): Name = {
        val r = computedValue(n) match {
          case Right(const) => m_constants(const)
          case Left(name)   => m_regs(name)
        }
        m_kept_regs += r

        r.variable.name
      }
      val cinst = asRenamed(inst)(substitution)
      m_insts += cinst
    }

    /** bind the name n to name m (i.e., n will no longer be used)
      *
      * @param n
      * @param m
      */
    def bindName(n: Name, m: Name): Unit = m_substs += n -> m

    /** bind name n to constant v (i.e., is fully reduced)
      *
      * @param n
      * @param v
      */
    def bindConst(n: Name, v: Constant): Unit = {
      m_evaluations += (n -> v)
      m_constants.get(v) match {
        case Some(m) => // do nothing
        case None    => m_constants += v -> freshConst(v)
      }
    }

    /** Build the process
      *
      * @return
      */
    def build(): DefProcess = {
      proc.copy(
        registers = m_kept_regs.toSeq,
        body = m_insts.dequeueAll(_ => true).toSeq
      )
    }

  }

  private def do_transform(
      proc: DefProcess
  )(implicit ctx: AssemblyContext): DefProcess = {

    proc.body
      .foldLeft(new ProcessBuilder(proc)) { case (builder, inst) =>
        inst match {
          case i @ (_: LocalLoad | _: GlobalLoad | _: LocalStore |
              _: GlobalStore | _: PadZero | _: SetCarry | _: ClearCarry |
              _: Predicate | _: PadZero) =>
            builder.keep(i)
          case Nop =>
          // don't keep
          case i @ (_: Send | _: Recv | _: CustomInstruction) =>
            ctx.logger.error("Unexpected instruction!", i)

          case i @ Expect(ref, got, error_id, _) =>
            (builder.computedValue(ref), builder.computedValue(got)) match {
              case (Right(v1), Right(v2)) if (v1 == v2) =>
              // no need to keep it anymore, it's always true
              case _ => builder.keep(i)

            }
          case i @ AddC(rd, co, rs1, rs2, ci, _) =>
            val ci_val = builder.computedValue(ci)
            val rs1_val = builder.computedValue(rs1)
            val rs2_val = builder.computedValue(rs2)
            (rs1_val, rs2_val, ci_val) match {
              case (Right(v1), Right(v2), Right(vci)) =>
                val (rd_val, co_val) = addCarryEvaluator(v1, v2, vci)
                builder.bindConst(rd, rd_val)
                builder.bindConst(co, co_val)
              case (Right(ConstZero), Right(ConstZero), Left(ci_subst)) =>
                builder.bindName(co, ci_subst)
              case _ => builder.keep(i)
            }
          case SetValue(rd, value, annons) =>
            builder.bindConst(rd, value)
          case i @ Mov(rd, rs, annons) =>
            if (builder.isUnopt(rd)) {
              // can not get rid of the rd alias
              builder.keep(i)
            } else {
              builder.computedValue(rs) match {
                case Left(root_rs) =>
                  // do not keep the instruction because we have something like:
                  // MOV x1, x0
                  // MOV x2, x1
                  // MOV rs, x2
                  // MOV rd, rs
                  // and we would like to simply bind rd to x0 with out the intermediate steps
                  builder.bindName(rd, root_rs)
                case Right(rs_value) =>
                  // if the root of this chain is constant, bind it to a const
                  builder.bindConst(rd, rs_value)
              }
            }

          case i @ Mux(rd, sel, rfalse, rtrue, annons) =>
            if (builder.isUnopt(rd)) {
              // need to keep rd as is
              builder.keep(i)
            } else {
              val sel_value = builder.computedValue(sel)
              val rtrue_value = builder.computedValue(rtrue)
              val rfalse_value = builder.computedValue(rfalse)
              sel_value match {
                case Right(ConstOne) =>
                  rtrue_value match {
                    case Right(ctrue) => builder.bindConst(rd, ctrue)
                    case Left(atrue)  => builder.bindName(rd, atrue)
                  }
                case Right(ConstZero) =>
                  rfalse_value match {
                    case Right(cfalse) => builder.bindConst(rd, cfalse)
                    case Left(afalse)  => builder.bindName(rd, afalse)
                  }
                case Right(invalid_const) =>
                  ctx.logger.error(
                    s"Mux condition should be either zero or one but it is ${invalid_const}",
                    i
                  )
                  builder.keep(i)
                case Left(s) =>
                  (rfalse_value, rtrue_value) match {
                    case (Right(ConstZero), Right(ConstOne)) =>
                       /*
                       a weird pattern that I've seen in the code is

                       MUX y, s, Const0, Const1 (y and s are not constant)
                       Other instructions using y
                       We can safely remove the mux and substitute y with s
                       since s is by definition single-bit, and y, which is
                       either zero or 1 is also somehow single-bit. In the PlacedIR
                       this is completely safe but in the UnconstrainedIR this may
                       be a problem */
                      assert(
                        sel_value.isLeft,
                        "something went wrong, expected name not constant"
                      )
                      builder.bindName(rd, s)
                    case (Right(ct1), Right(ct2)) if (ct1 == ct2)=>
                      // another weird pattern is having the same constant
                      // in both branches
                      builder.bindConst(rd, ct1)
                    case _ =>
                      // unopt
                      builder.keep(i)
                  }
              }
            }
          case i @ BinaryArithmetic(operator, rd, rs1, rs2, _) =>
            if (builder.isUnopt(rd)) {
              // keep this instruction
              builder.keep(i)
            } else {

              // try to compute it
              val rs1_val = builder.computedValue(rs1)
              val rs2_val = builder.computedValue(rs2)
              if (binOpEvaluator.isDefinedAt(operator, rs1_val, rs2_val)) {
                val rd_val =
                  binOpEvaluator(operator, rs1_val, rs2_val) match {
                    case Right(ct) =>
                      builder.bindConst(rd, ct)
                    case Left(n) =>
                      builder.bindName(rd, n)
                  }
              } else {
                // can not simplify
                builder.keep(i)
              }
            }
        }

        builder
      }
      .build()
  }

}
