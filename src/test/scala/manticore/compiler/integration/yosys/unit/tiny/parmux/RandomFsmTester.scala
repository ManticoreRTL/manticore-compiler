package manticore.compiler.integration.yosys.unit.tiny.parmux

import manticore.compiler.UnitFixtureTest
import manticore.compiler.integration.yosys.unit.CodeText
import scala.collection.IndexedSeqOps
import scala.collection.mutable.ArrayBuffer
import manticore.compiler.integration.yosys.unit.YosysUnitTest

object RandomUtils {

  def choice[T](ls: Seq[T])(implicit randGen: scala.util.Random): T = {
    val index = randGen.nextInt(ls.size)
    ls(index)
  }
  def shuffle[T](ls: Seq[T])(implicit randGen: scala.util.Random): Seq[T] = {
    val res = ArrayBuffer.from(ls)
    for (i <- ls.length to 2 by -1) {
      val k = randGen.nextInt(i)
      val tmp = res(i - 1)
      res(i - 1) = res(k)
      res(k) = tmp
    }
    res.toSeq
  }
  def some[T](ls: Seq[T])(implicit randGen: scala.util.Random): Seq[T] = {

    val sh = shuffle(ls)
    val n = randGen.nextInt(ls.length - 1) + 1
    sh.slice(0, n)
  }

  def genExpr(vars: Seq[String], depth: Int = 5)(implicit randGen: scala.util.Random): String = {
    sealed trait ExprKind
    case object BinOpExpr extends ExprKind
    case object UnaryOpExpr extends ExprKind
    case object IdOpExpr extends ExprKind
    case object ConstExpr extends ExprKind

    val kind = choice(Seq(BinOpExpr, UnaryOpExpr, IdOpExpr, ConstExpr))
    kind match {
      case BinOpExpr =>
        val op = choice(
          Seq(
            "+",
            "-",
            "<",
            "<=",
            "==",
            "!=",
            ">=",
            ">",
            // "<<", // can not have shifts because we only support shift up to 16-bit operands
            // ">>",
            // "<<<",
            // ">>>",
            "|",
            "&",
            "^",
            "~^",
            "||",
            "&&"
          )
        )
        // in case of shifts, we need to have an operand that is at most 16
        //
        if (depth >= 1) {
          val operand1 = genExpr(vars, depth - 1)
          val operand2 = genExpr(vars, depth - 1)
          s"(${operand1} ${op} ${operand2})"
        } else {
          s"(${choice(vars)} ${op} ${choice(vars)})"
        }
      case UnaryOpExpr =>
        val op = choice(
          Seq("+", "-", "~", "|", "&", "^", "~^", "!", "$signed", "$unsigned")
        )
        if (depth <= 1) {
          s"${op}(${choice(vars)})"
        } else {
          s"${op}(${genExpr(vars, depth - 1)})"
        }
      case IdOpExpr =>
        choice(vars)
      case ConstExpr =>
        s"32'd${randGen.nextInt(Int.MaxValue)}"
    }

  }
  def genCond(vars: Seq[String])(implicit randGen: scala.util.Random): String = {
    val op = choice(Seq("<", "<=", ">=", ">", "=="))
    s"(${genExpr(vars)}) $op (${genExpr(vars)})"
  }

}
final class RandomFsmGenerator(seed: Int) {

  val maxWidth = 32
  val maxStateBits = 8;
  final implicit val randGen = new scala.util.Random(seed)

  private def rSigned() = Seq("", "signed")(randGen.nextInt(2))
  private def rWidth() = randGen.nextInt(maxWidth - 1) + 1

  private def rState(bits: Int) = randGen.nextInt((1 << bits) - 1)

  // inspired by Yosys
  def generate(id: Int) = {
    import RandomUtils._
    val stateBits = randGen.nextInt(maxStateBits - 2) + 2
    val stateList = Range(0, 1 << stateBits)
    val vars = Seq("x", "y", "z", "a", "b")

    def fsmCase(nextStates: Seq[Int]): String = {

      val next = nextStates
        .map { ns =>
          "/* verilator lint_off UNSIGNED */ /* verilator lint_off CMPCONST */\n" +
          s"if(${genCond(vars)}) state <= ${ns};"
        }
        .mkString("\n")

      s"""|
            |x <= ${genExpr(vars)};
            |y <= ${genExpr(vars)};
            |z <= ${genExpr(vars)};
            |${next}
            |""".stripMargin
    }

    val setNext = {

      val cases = stateList.map { ps =>
        val ns = ps +: some(stateList.filter(_ != ps))
        s"""|
            |${ps} : begin
            |   ${fsmCase(ns)}
            |end
            |""".stripMargin
      }

      s"""|
          |case (state)
          |${cases.mkString("\n")}
          |endcase
          |""".stripMargin
    }

    CodeText {

      s"""|
            |module RandomFsm$id(clk, rst, a, b, c, x, y, z);
            |
            |   input clk, rst;
            |
            |   input ${rSigned()} [${rWidth()} - 1 : 0] a;
            |   input ${rSigned()} [${rWidth()} - 1 : 0] b;
            |   input ${rSigned()} [${rWidth()} - 1 : 0] c;
            |
            |   output reg ${rSigned()} [${rWidth()} - 1 : 0] x;
            |   output reg ${rSigned()} [${rWidth()} - 1 : 0] y;
            |   output reg ${rSigned()} [${rWidth()} - 1 : 0] z;
            |
            |   reg [${stateBits} - 1 : 0] state;
            |
            |   always @(posedge clk) begin
            |       if (rst) begin
            |              x <= ${randGen.nextInt()};
            |              y <= ${randGen.nextInt()};
            |              z <= ${randGen.nextInt()};
            |              state <= ${rState(stateBits)};
            |       end else begin
            |       ${setNext}
            |       end
            |
            |   end
            |endmodule
            |""".stripMargin

    }
  }

}


class RandomFsmTester extends UnitFixtureTest {


    behavior of "random FSMs"

    val generator = new RandomFsmGenerator(1234)

    for (i <- 0 to 10) {

      s"random FSM $i" should "match verilator's output" in {f =>

          new YosysUnitTest {
              val testIterations = 400
              val code = generator.generate(0)
              val testDir = f.test_dir
              override def dumpAll: Boolean = false


          }.run()
      }


    }

}
