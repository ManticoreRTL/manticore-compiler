package manticore.compiler.integration.yosys.unit.tiny.custom_functions

import manticore.compiler.UnitFixtureTest
import manticore.compiler.integration.yosys.unit.CodeText
import manticore.compiler.integration.yosys.unit.YosysUnitTest
import org.scalatest.CancelAfterFailure
import scala.collection.mutable.ArrayBuffer
import java.math.BigInteger

import collection.mutable.{Map => MMap}

class CustomFunctionTestGenerator extends UnitFixtureTest with CancelAfterFailure {

  behavior of s"Custom function insertion"

  // Set seed for deterministic execution.
  val randGen = new scala.util.Random(0)

  def width = 16

  trait Expr {
    def toVerilog(): String
  }

  trait Operator {
    val symbol: String
  }

  trait BinaryOperator extends Operator
  trait UnaryOperator  extends Operator

  case class NamedOperand(name: String) extends Expr {
    def toVerilog(): String = name
  }
  case class ConstOperand(v: BigInt) extends Expr {
    def toVerilog(): String = s"${v.bitLength}'b${v.toString(2)}"
  }

  object And  extends BinaryOperator { val symbol: String = "&"  }
  object Nand extends BinaryOperator { val symbol: String = "~&" }
  object Or   extends BinaryOperator { val symbol: String = "|"  }
  object Nor  extends BinaryOperator { val symbol: String = "~|" }
  object Xor  extends BinaryOperator { val symbol: String = "^"  }
  object Xnor extends BinaryOperator { val symbol: String = "~^" }
  object Not  extends UnaryOperator  { val symbol: String = "~"  }

  trait BinaryExpr extends Expr {
    val arg1: Expr
    val arg2: Expr
    val op: Operator

    def toVerilog(): String = {
      val arg1Str = arg1.toVerilog()
      val arg2Str = arg2.toVerilog()
      val opStr   = op.symbol
      s"(${arg1Str} ${opStr} ${arg2Str})"
    }
  }

  trait UnaryExpr extends Expr {
    val arg: Expr
    val op: Operator

    def toVerilog(): String = {
      val argStr = arg.toVerilog()
      val opStr  = op.symbol
      s"(${opStr}${argStr})"
    }
  }

  case class AndExpr(arg1: Expr, arg2: Expr)  extends BinaryExpr { val op: Operator = And  }
  case class NandExpr(arg1: Expr, arg2: Expr) extends BinaryExpr { val op: Operator = Nand }
  case class OrExpr(arg1: Expr, arg2: Expr)   extends BinaryExpr { val op: Operator = Or   }
  case class NorExpr(arg1: Expr, arg2: Expr)  extends BinaryExpr { val op: Operator = Nor  }
  case class XorExpr(arg1: Expr, arg2: Expr)  extends BinaryExpr { val op: Operator = Xor  }
  case class XnorExpr(arg1: Expr, arg2: Expr) extends BinaryExpr { val op: Operator = Xnor }
  case class NotExpr(arg: Expr)               extends UnaryExpr  { val op: Operator = Not  }

  def generateRandom(numTests: Int, depth: Int): String = {

    val inputs      = Seq.tabulate(10) { idx => NamedOperand(s"input_${idx}") }
    val outputs     = ArrayBuffer.empty[NamedOperand]
    val connections = ArrayBuffer.empty[(NamedOperand, Expr)]

    val ops: Seq[Operator] = Seq(And, Nand, Or, Nor, Xor, Xnor, Not)

    def getNextInput(): NamedOperand = {
      inputs(randGen.nextInt(inputs.length))
    }

    def getNextOutput(): NamedOperand = {
      val name = NamedOperand(s"out_${outputs.length}")
      outputs += name
      name
    }

    def getRandomOperator(): Operator = {
      ops(randGen.nextInt(ops.length))
    }

    def getRandomOperand(): Expr = {
      val num = randGen.nextInt(5)
      // 4 times more likely to pull a named argument rather than a constant.
      if (num <= 3) {
        getNextInput()
      } else {
        val binConstStr = Range
          .inclusive(0, width - 1)
          .map { bitIdx =>
            if (randGen.nextBoolean()) "1" else "0"
          }
          .mkString
        val binConst = new BigInteger(binConstStr, 2)
        ConstOperand(BigInt(binConst))
      }
    }

    def generateLogicExpr(targetDepth: Int): Expr = {
      def generateLogicExprIter(
          expr: Expr,
          currentDepth: Int = 0
      ): Expr = {
        if (currentDepth == targetDepth) {
          expr
        } else {
          val newExpr = getRandomOperator() match {
            case binOp: BinaryOperator =>
              val arg1 = expr
              val arg2 = getRandomOperand()
              binOp match {
                case And  => AndExpr(arg1, arg2)
                case Nand => NandExpr(arg1, arg2)
                case Or   => OrExpr(arg1, arg2)
                case Nor  => NorExpr(arg1, arg2)
                case Xor  => XorExpr(arg1, arg2)
                case Xnor => XnorExpr(arg1, arg2)
              }

            case unOp: UnaryOperator =>
              val arg = expr
              unOp match {
                case Not => NotExpr(arg)
              }
          }

          generateLogicExprIter(newExpr, currentDepth + 1)
        }
      }

      generateLogicExprIter(getRandomOperand())
    }

    Range.inclusive(0, numTests - 1).foreach { testIdx =>
      connections += Tuple2(getNextOutput(), generateLogicExpr(depth))
    }

    // Output verilog
    val inputsStr = inputs
      .map { name =>
        s"  input wire [${width} - 1 : 0] ${name.toVerilog()}"
      }
      .mkString(",\n")

    val outputsStr = outputs
      .map { name =>
        s"  output wire [${width} - 1 : 0] ${name.toVerilog()}"
      }
      .mkString(",\n")

    val connectionsStr = connections
      .map { case (name, expr) =>
        s"  assign ${name.toVerilog()} = ${expr.toVerilog()};"
      }
      .mkString("\n")

    s"""|
        |module CustomFunctionTestCase(
        |${inputsStr},
        |${outputsStr}
        |);
        |
        |${connectionsStr}
        |
        |endmodule
        |""".stripMargin
  }

  val verilog = generateRandom(numTests = 1000, depth = 10)

  s"Custom functions" should "match verilator results" in { f =>
    new YosysUnitTest {
      val testIterations   = 10000
      val code             = CodeText(verilog)
      val testDir          = f.test_dir
      override def dumpAll = true
    }.lowerAndRun()
  }

}
