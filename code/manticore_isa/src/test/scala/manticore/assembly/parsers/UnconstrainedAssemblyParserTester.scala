package manticore.assembly.levels

import manticore.UnitTest
import manticore.assembly.parser.UnconstrainedAssemblyParser

import manticore.assembly.AssemblyAnnotation
import manticore.assembly.BinaryOperator
import manticore.assembly.parser.AssemblyParser
import manticore.compiler.AssemblyContext

class UnconstrainedAssemblyParserTester extends UnitTest {

  behavior of "Unconstrained assembly parser"
  import manticore.assembly.levels.unconstrained.UnconstrainedIR._
  import manticore.assembly.levels.{
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
    @TRACKED [signal = "top/inst/yi"]
    @SOURCE [file="myfile.v:312.21"]
    .wire yi 16 0b011001010101010  ;
    .mem  m  64 ;
    .mem  mi  64 312312312;
    .input i 9;
    @TRACKED [signal="top/o"]
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
          AssemblyAnnotation("TRACKED", Map("signal" -> "top/inst/yi")),
          AssemblyAnnotation("SOURCE", Map("file" -> "myfile.v:312.21"))
        )
      ),
      DefReg(LogicVariable("m", 64, MemoryType), None),
      DefReg(LogicVariable("mi", 64, MemoryType), Some(312312312)),
      DefReg(LogicVariable("i", 9, InputType), None),
      DefReg(
        LogicVariable("o", 12, OutputType),
        None,
        Seq(AssemblyAnnotation("TRACKED", Map("signal" -> "top/o")))
      )
    )
  )

  val funcs = (
    """
    .func f0 [0x012];
    .func f1 [0x01, 0x15];
  """,
    Seq(
      DefFunc("f0", Seq(BigInt(0x012))),
      DefFunc("f1", Seq(BigInt(0x01), BigInt(0x15)))
    )
  )

  val insts = (
    """
    @MARKED
    ADD x, xi, xi;
    CUST xi, [f0], y, y, yi, yi;
    CUST x, [f1], y, y, yi, yi;
    LD  o, m[0x01]; // LD or LLD are the same
    ST  i, mi[0x02], p; // ST or LST are the same, predicate p is optional
    SET  mi, 0x12123;
    MUX  x, sel, xi, xi; // use if the instructions are not scheuled
    PMUX  x, xi, xi; // don't use if the insturctions are not scheduled!
    

  """,
    Seq(
      BinaryArithmetic(
        BinaryOperator.ADD,
        "x",
        "xi",
        "xi",
        Seq(AssemblyAnnotation("MARKED", Map()))
      ),
      CustomInstruction("f0", "xi", "y", "y", "yi", "yi"),
      CustomInstruction("f1", "x", "y", "y", "yi", "yi"),
      LocalLoad("o", "m", BigInt(0x01)),
      LocalStore("i", "mi", BigInt(0x02), Some("p")),
      SetValue("mi", BigInt(0x12123)),
      Mux("x", "sel", "xi", "xi"),
      BinaryArithmetic(BinaryOperator.PMUX, "x", "xi", "xi")
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
     @ORG [id="ch.epfl.vlsc.manticore"]
        .prog:
            @AUTHOR [name="mayy", email="mahyar.emami@epfl.ch"]
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
          body = insts._2,
          Seq(
            AssemblyAnnotation(
              "AUTHOR",
              Map(
                "name" -> "mayy",
                "email" -> "mahyar.emami@epfl.ch"
              )
            )
          )
        )
      ),
      Seq(
        AssemblyAnnotation(
          "ORG",
          Map("id" -> "ch.epfl.vlsc.manticore")
        )
      )
    )
    AssemblyParser(program, ctx) shouldBe expected
    println(expected.serialized)

  }

}