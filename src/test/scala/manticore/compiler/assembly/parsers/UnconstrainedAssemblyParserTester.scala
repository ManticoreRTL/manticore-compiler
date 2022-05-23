package manticore.compiler.assembly.levels

import manticore.compiler.UnitTest
import manticore.compiler.assembly.parser.UnconstrainedAssemblyParser

import manticore.compiler.assembly.annotations.AssemblyAnnotation
import manticore.compiler.assembly.annotations.Track
import manticore.compiler.assembly.BinaryOperator
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.annotations.StringValue

class UnconstrainedAssemblyParserTester extends UnitTest {

  behavior of "Unconstrained assembly parser"
  import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR._
  import manticore.compiler.assembly.levels.{
    WireType,
    InputType,
    MemoryType,
    OutputType,
    ConstType
  }

  val regs = (
    """.const $zero 32 0
      |.wire %x 32
      |.wire %%xi 32 0x321
      |.wire y  8
      |@TRACK [name = "top/inst/yi"]
      |.wire yi 16 0b011001010101010
      |.mem  m  64
      |.mem  mi  64
      |.input i 9
      |@TRACK [name="top/o"]
      |.output o 12
      |""".stripMargin,
    Seq(
      DefReg(LogicVariable("$zero", 32, ConstType), Some(BigInt(0))),
      DefReg(LogicVariable("%x", 32, WireType), None),
      DefReg(LogicVariable("%%xi", 32, WireType), Some(BigInt(0x321))),
      DefReg(LogicVariable("y", 8, WireType), None),
      DefReg(
        LogicVariable("yi", 16, WireType),
        Some(BigInt("011001010101010", 2)),
        Seq(
          Track(Map("name" -> StringValue("top/inst/yi")))
        )
      ),
      DefReg(LogicVariable("m", 64, MemoryType), None),
      DefReg(LogicVariable("mi", 64, MemoryType), None),
      DefReg(LogicVariable("i", 9, InputType), None),
      DefReg(
        LogicVariable("o", 12, OutputType),
        None,
        Seq(Track(Map("name" -> StringValue("top/o"))))
      )
    )
  )

  // UnconstrainedIR does not support custom functions.
  val funcs = (
    "",
    Seq.empty
  )

  val insts = (
    """ADD x, xi, xi;
      |(m, 0) LD  o, m[0x01]; // LD or LLD are the same
      |(m, 1) ST  i, mi[0x02], p; // ST or LST are the same, predicate p is optional
      |SET  mi, 0x12123;
      |MUX  x, sel, xi, xi;
      |""".stripMargin,
    Seq(
      BinaryArithmetic(
        BinaryOperator.ADD,
        "x",
        "xi",
        "xi"
      ),
      LocalLoad("o", "m", BigInt(0x01), MemoryAccessOrder("m", 0)),
      LocalStore("i", "mi", BigInt(0x02), Some("p"), MemoryAccessOrder("m", 1)),
      SetValue("mi", BigInt(0x12123)),
      Mux("x", "sel", "xi", "xi")
    )
  )

  implicit val ctx = AssemblyContext()

  it should "parse register definitions with annotations" in {

    val program = s""".prog:
                     |  .proc pid:
                     |    ${regs._1}
                     |""".stripMargin

    val ast = AssemblyParser(program, ctx)

    println(ast.serialized)
    val expected =
      DefProgram(
        Seq(
          DefProcess(
            id = "pid",
            registers = regs._2,
            functions = Nil,
            body = Nil
          )
        )
      )
    ast shouldBe expected
  }

  it should "parse function definitions with annotations" in {

    val program = s""".prog:
                     |  .proc pid:
                     |    ${regs._1}
                     |    ${funcs._1}
                     |""".stripMargin

    AssemblyParser(program, ctx) shouldBe
      DefProgram(
        Seq(
          DefProcess(
            id = "pid",
            registers = regs._2,
            functions = funcs._2,
            body = Nil
          )
        )
      )
  }

  it should "parse instructions with annotations" in {

    val program = s""".prog:
                     |  .proc pid:
                     |    ${regs._1}
                     |    ${funcs._1}
                     |    ${insts._1}
                     |""".stripMargin

    val expected = DefProgram(
      Seq(
        DefProcess(
          id = "pid",
          registers = regs._2,
          functions = funcs._2,
          body = insts._2
        )
      ),
      Seq(

      )
    )
    AssemblyParser(program, ctx) shouldBe expected
    println(expected.serialized)

  }

}
