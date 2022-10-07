package manticore.compiler.assembly.levels.placed
import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.assembly.levels.VariableType
import manticore.compiler.assembly.levels.HasVariableType
import manticore.compiler.assembly.HasWidth
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.assembly.DependenceGraphBuilder
import manticore.compiler.assembly.annotations.{Memblock => MemblockAnnotation}
import manticore.compiler.assembly.levels.AssemblyPrinter
import java.math.BigInteger
import manticore.compiler.assembly.levels.CanCollectInputOutputPairs
import manticore.compiler.assembly.BinaryOperator
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.AssemblyChecker
import manticore.compiler.assembly.CanBuildDependenceGraph
import manticore.compiler.assembly.CanComputeNameDependence
import manticore.compiler.assembly.levels.CanOrderInstructions
import manticore.compiler.assembly.levels.CanCollectProgramStatistics
import manticore.compiler.assembly.levels.DeadCodeElimination
import manticore.compiler.assembly.levels.CanRename
import manticore.compiler.assembly.HasInterruptAction
import manticore.compiler.assembly.annotations.Sourceinfo

import manticore.compiler.assembly.FinishInterrupt
import manticore.compiler.assembly.StopInterrupt
import manticore.compiler.assembly.AssertionInterrupt
import manticore.compiler.assembly.SerialInterrupt
import manticore.compiler.assembly.InterruptAction

/** IR level with placed processes and allocated registers.
  *
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */
object PlacedIR extends ManticoreAssemblyIR {

  import manticore.compiler.assembly.HasSerialized

  sealed abstract trait PlacedVariable
      extends Named[PlacedVariable]
      with HasSerialized
      with HasVariableType
      with HasWidth {
    override def serialized: String = s"${varType.typeName} ${name} -> ${id} 16"
    override def width              = 16
    val id: Int

    def withId(new_id: Int): PlacedVariable

  }
  import manticore.compiler.assembly.levels.{WireType, RegType, ConstType, InputType, OutputType, MemoryType}

  case class ValueVariable(name: Name, id: Int, tpe: VariableType) extends PlacedVariable {
    override def varType: VariableType           = tpe
    def withName(new_name: Name): PlacedVariable = this.copy(name = new_name)
    def withId(new_id: Int): PlacedVariable      = this.copy(id = new_id)
  }

  case class MemoryVariable(
      name: Name,
      size: Int,
      id: Int,
      initialContent: Seq[UInt16] = Nil
  ) extends PlacedVariable {
    def withName(n: Name)              = this.copy(name = n)
    override def varType: VariableType = MemoryType
    def withId(new_id: Int)            = this.copy(id = new_id)
  }

  sealed trait InterruptDescription extends HasInterruptAction {
    val eid: Int
    val info: Option[Sourceinfo]
  }
  case class SimpleInterruptDescription(
      action: InterruptAction,
      info: Option[Sourceinfo] = None,
      eid: Int = -1
  ) extends InterruptDescription {
    require(action == FinishInterrupt || action == StopInterrupt || action == AssertionInterrupt)
  }
  case class SerialInterruptDescription(
      action: SerialInterrupt,
      info: Option[Sourceinfo] = None,
      eid: Int = -1,
      pointers: Seq[Int] = Nil
  ) extends InterruptDescription

  case class ProcessIdImpl(id: String, x: Int, y: Int) {

    override def toString(): String = id.toString()
  }

  final class CustomFunctionImpl private (
      val arity: Int,
      val resources: Map[Either[Constant, BinaryOperator.BinaryOperator], Int],
      val expr: CustomFunctionImpl.ExprTree
  ) extends HasSerialized {

    import CustomFunctionImpl._

    // This is a VAL. It is only computed once (typically when generating code as
    // we are able to evaluate a custom function simply using its substituted
    // expression tree, which does not require the equation).
    lazy val equation: Seq[BigInt] = computeEquation(arity, expr)

    override def toString: String = {
      val args = Seq.tabulate(arity) { idx => s"%${idx}" }.mkString(", ")

      val resourcesStr = resources
        .map { case (constOrOp, cnt) =>
          constOrOp match {
            case Left(const) => s"$$${const} -> ${cnt}"
            case Right(op)   => s"${op} -> ${cnt}"
          }
        }
        .toSeq
        .sorted
        .mkString(", ")

      s"(${args}) => ${expr} : resources = ${resourcesStr}"
    }

    override def serialized: String = toString
  }

  object CustomFunctionImpl {

    sealed trait Atom

    case class AtomConst(v: UInt16)  extends Atom
    sealed abstract class AtomArg    extends Atom
    case class PositionalArg(v: Int) extends AtomArg
    case class NamedArg(v: Name)     extends AtomArg

    sealed trait ExprTree

    case class OrExpr(op1: ExprTree, op2: ExprTree) extends ExprTree {
      override def toString(): String = {
        s"(${op1} | ${op2})"
      }
    }

    case class AndExpr(op1: ExprTree, op2: ExprTree) extends ExprTree {
      override def toString(): String = {
        s"(${op1} & ${op2})"
      }
    }

    case class XorExpr(op1: ExprTree, op2: ExprTree) extends ExprTree {
      override def toString(): String = {
        s"(${op1} ^ ${op2})"
      }
    }

    case class IdExpr(id: Atom) extends ExprTree {
      override def toString(): String = {
        id match {
          case AtomConst(v) => v.toString()
          case arg: AtomArg =>
            arg match {
              case PositionalArg(v) => s"%${v}"
              case NamedArg(v)      => v
            }
        }
      }
    }

    // Fully evaluate an expression tree to a constant value. The tree given to
    // the function should have all its [[AtomArg]]s replaced by [[AtomConst]]s.
    // This can be used to interpret the expression tree.
    def evaluate(tree: ExprTree): UInt16 = {
      tree match {
        case AndExpr(op1, op2)    => evaluate(op1) & evaluate(op2)
        case OrExpr(op1, op2)     => evaluate(op1) | evaluate(op2)
        case XorExpr(op1, op2)    => evaluate(op1) ^ evaluate(op2)
        case IdExpr(AtomConst(v)) => v
        case IdExpr(_: AtomArg) =>
          throw new IllegalArgumentException(
            "Tree does not evaluate to constant! Cannot determine final value."
          )
      }
    }

    // Substitute (some or all) [[AtomArg]]s in the expression tree with another
    // AtomArg or a AtomConst. Note that the key of the Map must be AtomArg as
    // they are the only things that we can externally control in the expression
    // (we can't change internal constants of the expression).
    def substitute(tree: ExprTree)(subst: Map[AtomArg, Atom]): ExprTree = {
      tree match {
        case AndExpr(op1, op2) =>
          AndExpr(substitute(op1)(subst), substitute(op2)(subst))
        case OrExpr(op1, op2) =>
          OrExpr(substitute(op1)(subst), substitute(op2)(subst))
        case XorExpr(op1, op2) =>
          XorExpr(substitute(op1)(subst), substitute(op2)(subst))
        case IdExpr(arg: AtomArg) =>
          IdExpr(subst.getOrElse(arg, arg))
        case expr @ IdExpr(const: AtomConst) =>
          // We cannot substitute a constant, so we leave it as-is.
          expr
      }
    }

    // Compute the arity of the tree, i.e., the number of arguments required to
    // fully evaluate the tree.
    private def computeArity(tree: ExprTree): Int = {
      def collectArgs(tree: ExprTree): Set[AtomArg] = {
        tree match {
          case AndExpr(op1, op2)    => collectArgs(op1) ++ collectArgs(op2)
          case OrExpr(op1, op2)     => collectArgs(op1) ++ collectArgs(op2)
          case XorExpr(op1, op2)    => collectArgs(op1) ++ collectArgs(op2)
          case IdExpr(a: AtomArg)   => Set(a)
          case IdExpr(_: AtomConst) => Set.empty
        }
      }

      // Make sure ALL the args are either:
      //  1. a consecutive list of integers (starting from 0)
      //  2. a list of names.
      // Mixes of both are not possible.
      val args  = collectArgs(tree)
      val arity = args.size

      val argsArePositional = args.forall {
        case _: PositionalArg => true
        case _                => false
      } && {
        val foundPositions    = args.collect { case PositionalArg(pos) => pos }
        val expectedPositions = Set.tabulate(arity)(idx => idx)
        foundPositions == expectedPositions
      }

      val argsAreNamed = args.forall {
        case _: NamedArg => true
        case _           => false
      }

      assert(
        argsArePositional || argsAreNamed,
        s"Expression ${tree} to contains a mix of positional and named args!"
      )

      arity
    }

    private def computeResources(
        tree: ExprTree
    ): Map[
      Either[Constant, BinaryOperator.BinaryOperator],
      Int
    ] = {

      def accumulate(
          expr: ExprTree,
          acc: Map[Either[Constant, BinaryOperator.BinaryOperator], Int] = Map.empty.withDefaultValue(0)
      ): Map[Either[Constant, BinaryOperator.BinaryOperator], Int] = {
        expr match {
          case OrExpr(op1, op2) =>
            val childrenAcc = accumulate(op1, accumulate(op2, acc))
            val key         = Right(BinaryOperator.OR)
            val cnt         = childrenAcc(key) + 1
            childrenAcc + (key -> cnt)

          case AndExpr(op1, op2) =>
            val childrenAcc = accumulate(op1, accumulate(op2, acc))
            val key         = Right(BinaryOperator.AND)
            val cnt         = childrenAcc(key) + 1
            childrenAcc + (key -> cnt)

          case XorExpr(op1, op2) =>
            val childrenAcc = accumulate(op1, accumulate(op2, acc))
            val key         = Right(BinaryOperator.XOR)
            val cnt         = childrenAcc(key) + 1
            childrenAcc + (key -> cnt)

          case IdExpr(const: AtomConst) =>
            val key = Left(const.v)
            val cnt = acc(key) + 1
            acc + (key -> cnt)

          case _ =>
            acc
        }
      }

      accumulate(tree)
    }

    private def computeEquation(arity: Int, expr: ExprTree): Seq[BigInt] = {

      def trimConsts(expr: ExprTree, bitIdx: Int): ExprTree = {
        expr match {
          case OrExpr(op1, op2) =>
            OrExpr(trimConsts(op1, bitIdx), trimConsts(op2, bitIdx))
          case AndExpr(op1, op2) =>
            AndExpr(trimConsts(op1, bitIdx), trimConsts(op2, bitIdx))
          case XorExpr(op1, op2) =>
            XorExpr(trimConsts(op1, bitIdx), trimConsts(op2, bitIdx))
          case IdExpr(id) =>
            val newId = id match {
              // Extract the given bit from each constant.
              case AtomConst(v) => AtomConst((v >> bitIdx) & UInt16(1))
              case other        => other
            }
            IdExpr(newId)
        }
      }

      def computeBitEquation(bitIdx: Int): BigInt = {
        val exprTrimmed = trimConsts(expr, bitIdx)

        val bitIdxRes = Range(0, 1 << arity).map { number =>
          // Decompose "number" into "arity" bits.
          // These correspond to a LUT's (f, e, d, c, b, a) inputs. The number of inputs is equal to "arity".
          val substMap = Range(0, arity).map { argIdx =>
            val arg = (number >> argIdx) & 1
            PositionalArg(argIdx) -> AtomConst(UInt16(arg))
          }.toMap[AtomArg, Atom]

          val exprTrimmedSubst = substitute(exprTrimmed)(substMap)
          val exprTrimmedEval = evaluate(exprTrimmedSubst).toInt

          number -> exprTrimmedEval
        }.toMap

        val equation = bitIdxRes.foldLeft(BigInt(0)) { case (acc, (argIdx, res)) =>
          acc + (res << argIdx)
        }

        equation
      }

      val eqs = Range(0, 16).map { bitIdx =>
        computeBitEquation(bitIdx)
      }

      eqs

    }

    def apply(expr: ExprTree): CustomFunctionImpl = {
      // We compute the arity here so we can reject incorrectly-constructed expression trees
      // given by the user.
      val arity     = computeArity(expr)
      val resources = computeResources(expr)
      new CustomFunctionImpl(arity, resources, expr)
    }

    def unapply(cf: CustomFunctionImpl) = {
      Some((cf.arity, cf.expr, cf.equation))
    }
  }

  type Name           = String
  type Variable       = PlacedVariable
  type CustomFunction = CustomFunctionImpl
  type ProcessId      = ProcessIdImpl
  type Constant       = UInt16

  type Label = String
}

trait PlacedIRTransformer extends AssemblyTransformer[PlacedIR.DefProgram] {}
trait PlacedIRChecker     extends AssemblyChecker[PlacedIR.DefProgram]     {}

object PlacedIRPrinter extends AssemblyPrinter[PlacedIR.DefProgram] {}

@deprecated("Use Helpers instead")
object PlacedIRDependencyDependenceGraphBuilder extends DependenceGraphBuilder {
  val flavor = PlacedIR
}

@deprecated("Use Helpers instead")
object PlacedIRInputOutputCollector extends CanCollectInputOutputPairs {
  val flavor = PlacedIR

}

object Helpers
    extends CanBuildDependenceGraph
    with CanCollectInputOutputPairs
    with CanComputeNameDependence
    with CanOrderInstructions
    with CanCollectProgramStatistics
    with CanRename {
  val flavor = PlacedIR

  object DeadCode extends DeadCodeElimination { val flavor = PlacedIR }
}

// object EquationTest extends App {
//   // (%0, %1) => (21119 ^ (%0 & (%1 ^ 21119))) : resources = $21119 -> 2, AND -> 1, XOR -> 2
//   import PlacedIR._
//   import PlacedIR.CustomFunctionImpl._

//   val expr = XorExpr(
//     IdExpr(AtomConst(UInt16(21119))),
//     AndExpr(
//       IdExpr(PositionalArg(0)),
//       XorExpr(
//         IdExpr(PositionalArg(1)),
//         IdExpr(AtomConst(UInt16(21119)))
//       )
//     )
//   )

//   val func = CustomFunctionImpl(expr)

//   val equationsStr = func.equation.zipWithIndex.map { case (eq, idx) =>
//     s"${idx} -> 0x${eq.toString(16)}"
//   }.mkString("\n")

//   println(func.toString())
//   println(equationsStr)
// }