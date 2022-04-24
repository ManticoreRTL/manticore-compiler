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
    RegType,
    WireType,
    InputType,
    MemoryType,
    OutputType,
    ConstType
  }
  val regs = (
    """
    .const $zero 32 0;
    .reg %x 32;
    .reg %%xi 32 0x321;
    .wire y  8 ;
    @TRACK [name = "top/inst/yi"]
    .wire yi 16 0b011001010101010  ;
    .mem  m  64 ;
    .mem  mi  64 312312312;
    .input i 9;
    @TRACK [name="top/o"]
    .output o 12;
  """,
    Seq(
      DefReg(LogicVariable("$zero", 32, ConstType), Some(BigInt(0))),
      DefReg(LogicVariable("%x", 32, RegType), None),
      DefReg(LogicVariable("%%xi", 32, RegType), Some(BigInt(0x321))),
      DefReg(LogicVariable("y", 8, WireType), None),
      DefReg(
        LogicVariable("yi", 16, WireType),
        Some(BigInt("011001010101010", 2)),
        Seq(
          Track(Map("name" -> StringValue("top/inst/yi")))
        )
      ),
      DefReg(LogicVariable("m", 64, MemoryType), None),
      DefReg(LogicVariable("mi", 64, MemoryType), Some(312312312)),
      DefReg(LogicVariable("i", 9, InputType), None),
      DefReg(
        LogicVariable("o", 12, OutputType),
        None,
        Seq(Track(Map("name" -> StringValue("top/o"))))
      )
    )
  )

  // TODO (skashani): Remove custom functions from this UnconstrainedIR test as
  // custom functions are not supported at this level of the IR.
  val funcs = ("", Seq.empty)

  val insts = (
    """
    ADD x, xi, xi;
    CUST xi, [f0], y, y, yi, yi;
    CUST x, [f1], y, y, yi, yi;
    LD  o, m[0x01]; // LD or LLD are the same
    ST  i, mi[0x02], p; // ST or LST are the same, predicate p is optional
    SET  mi, 0x12123;
    MUX  x, sel, xi, xi;


  """,
    Seq(
      BinaryArithmetic(
        BinaryOperator.ADD,
        "x",
        "xi",
        "xi"
      ),
      CustomInstruction("f0", "xi", Seq("y", "y", "yi", "yi")),
      CustomInstruction("f1", "x", Seq("y", "y", "yi", "yi")),
      LocalLoad("o", "m", BigInt(0x01)),
      LocalStore("i", "mi", BigInt(0x02), Some("p")),
      SetValue("mi", BigInt(0x12123)),
      Mux("x", "sel", "xi", "xi")
    )
  )

  implicit val ctx = AssemblyContext()
  it should "parse a register definitions with annotations" in {

    val program = s"""
        .prog:
            .proc pid:
                ${regs._1}
        """
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
    val program = s"""

        .prog:
            .proc pid:
                ${regs._1}
                ${funcs._1}
      """
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
    val program = s"""

        .prog:

            .proc pid:
                ${regs._1}
                ${funcs._1}
                ${insts._1}
    """

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
