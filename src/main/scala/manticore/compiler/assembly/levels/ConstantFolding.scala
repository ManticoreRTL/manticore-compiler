package manticore.compiler.assembly.levels

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.BinaryOperator
import manticore.compiler.assembly.annotations.Track
import manticore.compiler.assembly.levels.CanRename
import javax.xml.crypto.Data
import manticore.compiler.assembly.CanComputeNameDependence
import scala.collection.immutable.ListMap

/** Base trait to implement a constant folding pass in
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */
trait ConstantFolding
    extends Flavored
    with CanRename
    with CanCollectProgramStatistics
    with CanComputeNameDependence
    with CanCollectInputOutputPairs {

  import flavor._

  def do_transform(
      prog: DefProgram
  )(implicit ctx: AssemblyContext): DefProgram = {

    val res = prog.copy(
      processes = prog.processes.map(constFoldProcess)
    )
    ctx.stats.record(ProgramStatistics.mkProgramStats(res))
    res
  }

  // a concrete constant should have bit-width information embedded in it
  // that may be required in constant evaluation
  type ConcreteConstant

  /** Constant zero and one, override them with their corresponding values in
    * the given flavor
    */
  def isTrue(v: ConcreteConstant): Boolean
  def isFalse(v: ConcreteConstant): Boolean
  def asConcrete(const: Constant)(width: => Int): ConcreteConstant

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
        Either[Name, ConcreteConstant], // rs1
        Either[Name, ConcreteConstant] // rs2
    ),
    Either[Name, ConcreteConstant] // rd
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
      rs1: ConcreteConstant,
      rs2: ConcreteConstant,
      ci: ConcreteConstant
  )(implicit ctx: AssemblyContext): (ConcreteConstant, ConcreteConstant)

  /**
    * A total function that fully evaluates a slice of a constant.
    */
  def sliceEvaluator(
    const: ConcreteConstant,
    offset: Int,
    length: Int
  ): ConcreteConstant

  /** returns a fresh constant (unique name), the value [[v]] given to the
    * function is unique too but can not be used to create the unique name
    *
    * @param v
    * @param ctx
    * @return
    */
  def freshConst(v: ConcreteConstant)(implicit ctx: AssemblyContext): DefReg

  case class ConstantFoldingBuilder(
      constants: ListMap[ConcreteConstant, DefReg],
      constBindings: Map[Name, ConcreteConstant],
      nameBindings: Map[Name, Name],
      keptInstructions: Seq[Instruction],
      isUnopt: Name => Boolean,
      getReg: Name => DefReg
  ) {

    def widthLookup(n: Name): Int = getReg(n).variable.width
    /** bind the name n to name m (i.e., n will no longer be used)
      *
      * @param n
      * @param m
      */
    def bindName(n: Name, m: Name): ConstantFoldingBuilder = copy(
      nameBindings = nameBindings + (n -> m)
    )
    def bindName(binding: (Name, Name)): ConstantFoldingBuilder =
      bindName(binding._1, binding._2)

    /** bind name n to constant v (i.e., is fully reduced)
      *
      * @param n
      * @param v
      */
    def bindConst(n: Name, v: ConcreteConstant)(implicit
        ctx: AssemblyContext
    ): ConstantFoldingBuilder = {
      // val concrete = asConcrete(v) { widthLookup(n) }
      val newConstBindings = constBindings + (n -> v)
      val newConstants =
        // create a new constant if it not already available
        constants.get(v) match {
          case Some(m) => constants
          case None    => constants + (v -> freshConst(v))
        }
      copy(
        constBindings = newConstBindings,
        constants = newConstants
      )
    }
    def bindConst(binding: (Name, ConcreteConstant))(implicit
        ctx: AssemblyContext
    ): ConstantFoldingBuilder =
      bindConst(binding._1, binding._2)

    /** Keep the given instruction
      *
      * @param inst
      */
    def keep(
        inst: Instruction
    )(implicit ctx: AssemblyContext): ConstantFoldingBuilder = {
      copy(keptInstructions = keptInstructions :+ inst)
    }
    def keepAll(
        instructions: IterableOnce[Instruction]
    )(implicit ctx: AssemblyContext): ConstantFoldingBuilder = {
      copy(keptInstructions = keptInstructions ++ instructions)
    }

    def computedValue(name: Name): Either[Name, ConcreteConstant] =
      constBindings.get(name) match {
        case Some(v) => Right(v)
        case None =>
          nameBindings.get(name) match {
            case Some(m) => Left(m)
            case None    => Left(name)
          }
      }

    // def concreteComputedValue(name: Name): Either[Name, ConcreteConstant] =
    //   computedValue(name).map { asConcrete(_) { widthLookup(name) } }

    def withNewScope(): ConstantFoldingBuilder =
      copy(keptInstructions = Seq.empty[Instruction])

  }

  def constFoldBlock(
      scope: ConstantFoldingBuilder,
      block: Iterable[Instruction]
  )(implicit ctx: AssemblyContext): ConstantFoldingBuilder = {

    block.foldLeft(scope) { case (builder, inst) =>
      inst match {
        case i @ (_: LocalLoad | _: GlobalLoad | _: LocalStore |
            _: GlobalStore | _: PadZero | _: SetCarry | _: ClearCarry |
            _: Predicate | _: PadZero | _: Lookup | _: PutSerial | _: Interrupt) =>
          builder.keep(i)
        case Nop =>
          // don't keep
          builder
        case i @ (_: Send | _: Recv | _: CustomInstruction) =>
          ctx.logger.error("Unexpected instruction!", i)
          builder.keep(i)
        case i @ Expect(ref, got, error_id, _) =>
          (builder.computedValue(ref), builder.computedValue(got)) match {
            case (Right(v1), Right(v2)) if (v1 == v2) =>
              // no need to keep it anymore, it's always true
              ctx.logger.warn("Removing systemcall", i)
              builder
            case _ => builder.keep(i)

          }
        case i @ AddC(rd, co, rs1, rs2, ci, _) =>
          val ci_val = builder.computedValue(ci)
          val rs1_val = builder.computedValue(rs1)
          val rs2_val = builder.computedValue(rs2)
          (rs1_val, rs2_val, ci_val) match {
            case (Right(v1), Right(v2), Right(vci)) =>
              val (rd_val, co_val) = addCarryEvaluator(v1, v2, vci)
              builder.bindConst(rd, rd_val).bindConst(co, co_val)
            case (Right(c0), Right(c1), Left(ci_subst))
                if isFalse(c0) && isTrue(c1) =>
              builder.bindName(co, ci_subst)
            case _ => builder.keep(i)
          }
        case SetValue(rd, value, annons) =>
          builder.bindConst(rd, asConcrete(value) { builder.widthLookup(rd) })

        case i @ Slice(rd, rs, offset, length, annons) =>
          val rs_val = builder.computedValue(rs)
          rs_val match {
            case Left(name) =>
              val rsWidth = builder.widthLookup(name)
              val isFullWidthSlice = (offset == 0) && (rsWidth == length)
              if (isFullWidthSlice) {
                // We are slicing the full width of another name. The slice operation
                // is therefore redundanat and we remove it.
                builder.bindName(rd, name)
              } else {
                // Can not simplify
                builder.keep(i)
              }
            case Right(const) =>
              val res = sliceEvaluator(const, offset, length)
              builder.bindConst(rd, res)
          }

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
              case Right(c) =>
                if (isTrue(c)) {
                  rtrue_value match {
                    case Right(ctrue) => builder.bindConst(rd, ctrue)
                    case Left(atrue)  => builder.bindName(rd, atrue)
                  }
                } else if (isFalse(c)) {

                  rfalse_value match {
                    case Right(cfalse) => builder.bindConst(rd, cfalse)
                    case Left(afalse)  => builder.bindName(rd, afalse)
                  }

                } else {
                  ctx.logger.error(
                    s"Mux condition should be either zero or one but it is ${c}",
                    i
                  )
                  builder.keep(i)
                }

              case Left(s) =>
                (rfalse_value, rtrue_value) match {
                  case (Right(c0), Right(c1))
                      if (isFalse(c0) && isTrue(c1) &&
                        builder.widthLookup(
                          rfalse
                        ) == builder.widthLookup(
                          sel
                        ) && builder
                          .widthLookup(rtrue) == builder.widthLookup(sel)) =>
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
                  case (Right(ct1), Right(ct2)) if (ct1 == ct2) =>
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
        case i @ ParMux(rd, cases, default, _) =>
          if (builder.isUnopt(rd)) {
            builder.keep(i)
          } else {
            val computed_cases = cases
              .map { case ParMuxCase(cond, choice) =>
                (builder.computedValue(cond), builder.computedValue(choice))
              }
            val found_true_case = computed_cases
              .collectFirst {
                case (Right(c1), rs_val) if isTrue(c1) =>
                  rs_val
              }

            found_true_case match {
              case None => // maybe all conditions are zero!
                if (
                  computed_cases.forall {
                    case (Right(c0), _) if isFalse(c0) => true
                    case _                             => false
                  }
                ) {
                  // use the default case
                  val default_case_value = builder.computedValue(default)
                  default_case_value match {
                    case Right(const_val) => builder.bindConst(rd, const_val)
                    case Left(dyn_val)    => builder.bindName(rd, dyn_val)
                  }
                } else {
                  // can not optimize
                  builder.keep(i)
                }
              case Some(Right(const_val)) => builder.bindConst(rd, const_val)
              case Some(Left(dyn_val))    => builder.bindName(rd, dyn_val)
            }
          }
        case brk: BreakCase => builder.keep(brk)
        case jtb @ JumpTable(_, _, blocks, dslot, _) =>
          case class CaseBuilder(
              prev: ConstantFoldingBuilder,
              cases: Seq[JumpCase] = Seq.empty[JumpCase]
          )
          // we get rid of the delay slot instructions by moving them to
          // the original instruction stream. This is to undo the effect of
          // scheduling (e.g., we also remove NOPs).
          val currentBuilder = constFoldBlock(builder, dslot)
          val CaseBuilder(newBuilder, newCases) =
            blocks.foldLeft(CaseBuilder(currentBuilder.withNewScope())) {
              case (CaseBuilder(prev, cases), JumpCase(lbl, blk)) =>
                val fld = constFoldBlock(prev, blk)
                CaseBuilder(
                  fld.withNewScope(),
                  cases :+ JumpCase(
                    lbl,
                    fld.keptInstructions.map(_.asInstanceOf[DataInstruction])
                  )
                )
            }
          val foldedTable = jtb.copy(
            blocks = newCases,
            dslot = Seq.empty[DataInstruction]
          )

          newBuilder.keepAll(currentBuilder.keptInstructions).keep(foldedTable)
      }
    }
  }

  def constFoldProcess(
      process: DefProcess
  )(implicit ctx: AssemblyContext): DefProcess = {

    val regMap = process.registers.map { case r => r.variable.name -> r }.toMap
    def hasTrack(r: DefReg): Boolean = r.annons.exists { _.isInstanceOf[Track] }

    val isUnopt = process.registers.collect {
      case r if hasTrack(r) || r.variable.varType == OutputType =>
        r.variable.name
    }.toSet
    val constants = process.registers.collect {
      case r if r.variable.varType == ConstType => r
    }
    val initScope =
      ConstantFoldingBuilder(
        constants = ListMap.from(constants.map { r =>
          asConcrete(r.value.get) { r.variable.width } -> r
        }),
        constBindings = constants.map { r =>
          r.variable.name -> asConcrete(r.value.get) { r.variable.width }
        }.toMap,
        nameBindings = Map.empty[Name, Name],
        keptInstructions = Seq.empty[Instruction],
        isUnopt = isUnopt,
        getReg = regMap
      )
    val folded = constFoldBlock(initScope, process.body)

    // we now have all the instructions that are supposed to be keeping, plus
    // a substitution map for renaming aliases and a set of new definitions
    // for computed constants

    val lookupConstBinding: PartialFunction[Name, Name] =
      folded.constBindings andThen folded.constants andThen { _.variable.name }
    val lookupNameBindings: PartialFunction[Name, Name] = folded.nameBindings
    val subst: Name => Name =
      // if there is a constant binding, used that
      lookupConstBinding orElse
        // else if there is an aliased name binding use that
        lookupNameBindings orElse
        // otherwise just use the original name
        { n => n }

    val finalInstructions = folded.keptInstructions.map { inst =>
      Rename.asRenamed(inst)(subst)
    }

    // now we need to remove any unused DefRegs

    val inputOutputPairs = InputOutputPairs.createInputOutputPairs(process)
    val referenced = NameDependence.referencedNames(finalInstructions)
    def isIo(r: DefReg): Boolean =
      r.variable.varType == InputType || r.variable.varType == OutputType
    // keep a def if it was referenced or it is an actual register, note that
    // this pass can not remove registers, that is the job of the DCE
    def shouldKeepDef(r: DefReg) = referenced(r.variable.name) || isIo(r)

    val nonConstOriginalNames = process.registers.filter { r =>
      r.variable.varType != ConstType
    }

    val newDefs: Seq[DefReg] =
      folded.constants.collect {
        case (_, r) if shouldKeepDef(r) =>
          r // keep the constants that were used
        // in the finalInstructions
      }.toSeq ++
        process.registers.filter { r =>
          r.variable.varType != ConstType && shouldKeepDef(r)
        // keep the non constant original names that are referenced in
        // finalInstructions
        }
    val newLabels = process.labels.collect {
      case l @ DefLabelGroup(m, _, _, _) if referenced(subst(m)) =>
        l.copy(memory = subst(m))
    }

    process.copy(
      body = finalInstructions,
      registers = newDefs,
      labels = newLabels
    )

  }

}
