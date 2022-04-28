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
    override def width = 16
    val id: Int

    def withId(new_id: Int): PlacedVariable

  }
  import manticore.compiler.assembly.levels.{
    WireType,
    RegType,
    ConstType,
    InputType,
    OutputType,
    MemoryType
  }

  case class ValueVariable(name: Name, id: Int, tpe: VariableType)
      extends PlacedVariable {
    override def varType: VariableType = tpe
    def withName(new_name: Name): PlacedVariable = this.copy(name = new_name)
    def withId(new_id: Int): PlacedVariable = this.copy(id = new_id)
  }

  case class MemoryVariable(name: Name, id: Int, block: MemoryBlock)
      extends PlacedVariable {
    def withName(n: Name) = this.copy(name = n)
    override def varType: VariableType = MemoryType
    def withId(new_id: Int) = this.copy(id = new_id)
  }

  case class MemoryBlock(
      block_id: Name,
      capacity: Int,
      width: Int,
      initial_content: Seq[UInt16] = Seq.empty[UInt16]
  ) {
    def capacityInShorts(): Int = {
      capacity * numShortsPerWord()
    }
    def numShortsPerWord(): Int =
      ((width - 1) / 16 + 1)
  }
  object MemoryBlock {
    def fromAnnotation(a: MemblockAnnotation) =
      MemoryBlock(a.getBlock(), a.getCapacity(), a.getWidth())
  }

  case class ProcessIdImpl(id: String, x: Int, y: Int) {

    override def toString(): String = id.toString()
  }

  sealed trait ExceptionKind
  case object ExpectFail extends ExceptionKind
  case object ExpectStop extends ExceptionKind

  case class ExceptionIdImpl(id: UInt16, msg: String, kind: ExceptionKind) {
    override def toString(): String = s"${msg}_${id}"
  }

  final class CustomFunctionImpl private (
    val arity: Int,
    val expr: CustomFunctionImpl.ExprTree,
  ) extends HasSerialized {

    import CustomFunctionImpl._

    // This is a VAL. It is only computed once (typically when generating code as
    // we are able to evaluate a custom function simply using its substituted
    // expression tree, which does not require the equation).
    lazy val equation: Seq[BigInt] = computeEquation(arity, expr)

    override def toString: String = {
      val args = Seq.tabulate(arity) { idx => s"%${idx}" }.mkString(", ")
      s"(${args}) => ${expr}"
    }

    override def serialized: String = toString
  }

  object CustomFunctionImpl {

    sealed trait Atom

    case class AtomConst(v: UInt16) extends Atom
    case class AtomArg(v: Int) extends Atom

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
          case AtomArg(v)   => s"%${v}"
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
          throw new IllegalArgumentException("Tree does not evaluate to constant! Cannot determine final value.")
      }
    }

    // Substitute (some or all) [[AtomArg]]s in the expression tree with constants.
    def substitute(tree: ExprTree)(subst: Map[AtomArg, AtomConst]): ExprTree = {
      tree match {
        case AndExpr(op1, op2)        => AndExpr(substitute(op1)(subst), substitute(op2)(subst))
        case OrExpr(op1, op2)         => OrExpr(substitute(op1)(subst), substitute(op2)(subst))
        case XorExpr(op1, op2)        => XorExpr(substitute(op1)(subst), substitute(op2)(subst))
        case IdExpr(a: AtomArg)       => IdExpr(subst(a))
        case e @ IdExpr(_: AtomConst) => e
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

      val args = collectArgs(tree).toSeq.sortBy(_.v)
      // Make sure the args are consecutive set of integers.
      assert(args == Seq.tabulate(args.length) { idx => AtomArg(idx) })
      args.length
    }

    private def computeEquation(arity: Int, expr: ExprTree): Seq[BigInt] = {

      // Generates a combination of masks that are used to evaluate the expression tree at
      // a given bit offset. Here's an example of the output for increasing values of bitpos
      // for a arity-2 LUT (i.e. a 2-LUT).
      //
      // bitpos = 0
      //   lutVectorInputCombinations(2) = List(
      //     List(0, 0),
      //     List(0, 1),
      //     List(1, 0),
      //     List(1, 1)
      //   )
      //
      // bitpos = 1
      //   lutVectorInputCombinations(2) = List(
      //     List(0, 0),
      //     List(0, 2),
      //     List(2, 0),
      //     List(2, 2)
      //   )
      //
      // bitpos = 2
      //   lutVectorInputCombinations(2) = List(
      //     List(0, 0),
      //     List(0, 4),
      //     List(4, 0),
      //     List(4, 4)
      //   )
      //
      // ...
      def lutVectorInputCombinations(
        count: Int,
        bitpos: Int
      ): Seq[Seq[UInt16]] = {
        def binStr(x: Int, width: Int): String = {
          // Scala's toBinaryString does not emit leading 0s. We therefore interpret
          // the binary string as an int and use a 0-padded format string to get the
          // width of interest.
          //
          // Note that the following intuitive alternative will fail as the string
          // formatter "%s" does not support padding with 0.
          //
          //    s"%0${width}s".format(x.toBinaryString)
          //       ^
          //
          s"%0${width}d".format(x.toBinaryString.toInt)
        }

        val minVal = 0
        val maxVal = (1 << count)

        val binaryRepr = Range(minVal, maxVal).map { num =>
          val bin = binStr(num, count)
          bin.map { char =>
            // Converting a char directly to an integer will return the ordinal of the character.
            // We therefore first convert the char to a string, then to an integer.
            val orig = char.toString.toInt

            // We want to evaluate equations at the word level. We therefore create a mask
            // that is aligned with the bit position we are currently computing the LUT
            // equation of.
            UInt16(orig << bitpos)
          }
        }

        binaryRepr
      }

      // The custom instruction has a 16-bit datapath. We return a Seq[BigInt] as every
      // bit of the result could technically be computed with a different function (a given
      // expression tree may have constants embedded within it and the constant may not be
      // a homogenous sequence of bits).
      val equations = Range(0, 16).map { bitpos =>
        // There are 2^arity lutVectorInputCombinations of inputs for a LUT.
        val inputCombinations: Seq[Seq[UInt16]] = lutVectorInputCombinations(arity, bitpos)

        // For every combination of inputs to the LUT, substitute the input in the
        // expression tree and evaluate it. This yields the truth table value for
        // the LUT vector's bitpos-th LUT.
        val lutOutputs = inputCombinations.map { inputCombination =>
          // The expression tree has arguments that are mapped to indices starting from 0.
          // We replace these indices with constants (defined by the LUT's current input combination).
          val subst = inputCombination.zipWithIndex.map { case (const, idx) =>
            AtomArg(idx) -> AtomConst(const)
          }.toMap

          val resFullDatapath = evaluate(substitute(expr)(subst))

          // The SHIFT-AND is required to isolate the result of the LUT vector's evaluation for
          // the datapath bit of interest. This result is the truth table.
          val resBitDatapath = (resFullDatapath >> bitpos) & UInt16(1)

          // The result is guaranteed to be 0 or 1 since we AND by UInt16(1) above.
          resBitDatapath.toInt
        }

        // Concatenate the k-LUT's output bits to form its truth table.
        // Note that we reverse the lut outputs as we want the MSB on the left and
        // the LSB on the right (common LUT equation representation in truth tables).
        val truthTableStr = lutOutputs.reverse.mkString
        BigInt(truthTableStr, 2)
      }

      equations
    }

    def apply(expr: ExprTree): CustomFunctionImpl = {
      // We compute the arity here so we can reject incorrectly-constructed expression trees
      // given by the user.
      val arity = computeArity(expr)
      new CustomFunctionImpl(arity, expr)
    }

    def unapply(cf: CustomFunctionImpl) = {
      Some((cf.arity, cf.expr, cf.equation))
    }
  }

  type Name = String
  type Variable = PlacedVariable
  type CustomFunction = CustomFunctionImpl
  type ProcessId = ProcessIdImpl
  type Constant = UInt16
  type ExceptionId = ExceptionIdImpl

  type Label = Symbol
}

object LatencyAnalysis {

  import PlacedIR._
  def latency(inst: Instruction): Int = inst match {
    case _: Predicate    => 0
    case _: Expect       => 0
    case Nop             => 0
    case _               => maxLatency()
  }

  def xHops(source: ProcessId, target: ProcessId, dim: (Int, Int)) =
    if (source.x > target.x) dim._2 - source.x + target.x
    else target.x - source.x
  def yHops(source: ProcessId, target: ProcessId, dim: (Int, Int)) =
    if (source.y > target.y) dim._2 - source.y + target.y
    else target.y - source.y

  def xyHops(source: ProcessId, target: ProcessId, dim:(Int, Int)) = {
    (xHops(source, target, dim), yHops(source, target, dim))
  }
  def maxLatency(): Int = 3
  def manhattan(
      source: ProcessId,
      target: ProcessId,
      dim: (Int, Int)
  ) = {
    val x_dist = xHops(source, target, dim)
    val y_dist = yHops(source, target, dim)
    val manhattan = x_dist + y_dist
    manhattan
  }
}


object PlacedIRPrinter extends AssemblyPrinter[PlacedIR.DefProgram] {}
