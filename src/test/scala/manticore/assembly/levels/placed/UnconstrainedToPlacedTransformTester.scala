package manticore.assembly.levels.placed

import manticore.UnitTest
import manticore.assembly.parser.UnconstrainedAssemblyParser
import scala.language.postfixOps

import manticore.assembly.BinaryOperator
import manticore.assembly.annotations.{
  AssemblyAnnotation,
  Memblock,
  Layout,
  Loc
}

import manticore.assembly.levels.UInt16
import manticore.compiler.AssemblyContext
import java.io.File
import manticore.assembly.parser.AssemblyParser

import manticore.assembly.annotations.StringValue
import manticore.assembly.annotations.IntValue
import manticore.compiler.CompilationFailureException

class UnconstrainedToPlacedTransformTester extends UnitTest {

  behavior of "unconstrained IR to placed IR transform"

  val valid_program =
    """
@LAYOUT [x = "10", y = "32"]
.prog :
    @LOC [x = "0", y = "1"]
    .proc p0:
        .const $zero 16 0x0
        .const $one  16 0x1
        .reg   %x    16;
        @MEMBLOCK [block="mem_0"]
        .mem   $$bram 16
        .func f0 [0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01]
        ADD %x, $zero, $one;
        """
  val invalid_program =
    """
 // @LAYOUT [x = "10", y = "32"]
.prog :
    // @LOC [x = "0", y = "1"]
    .proc p0:
        .const $zero 16 0b111111111111111111111111111111111111111111111111111 // wrong
        .const $one  11 0x1 // wrong
        .reg   %x    16;
        .func f0 [0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
                    0x01, 0x01, 0x01, 0x01, 0x01, 0x01] // wrong
        ADD %x, $zero, $one;

        """
  val ctx = AssemblyContext()

  val backend = UnconstrainedToPlacedTransform

  it should "correctly transform a valid program" in {
    import PlacedIR._
    val got = backend(AssemblyParser(valid_program, ctx), ctx)._1
    got shouldBe
      DefProgram(
        Seq(
          DefProcess(
            id = ProcessIdImpl("p0", 0, 1),
            registers = Seq(
              DefReg(ConstVariable("$zero", 0), Some(UInt16(0))),
              DefReg(ConstVariable("$one", 1), Some(UInt16(1))),
              DefReg(RegVariable("%x", 2), None),
              DefReg(
                MemoryVariable("$$bram", 3, MemoryBlock("mem_0", 512)),
                None,
                Seq(
                  Memblock(
                    Map("block" -> StringValue("mem_0"), "capacity" -> IntValue(512))
                  )
                )
              )
            ),
            functions = Seq(
              DefFunc(
                "f0",
                CustomFunctionImpl(
                  Seq.fill(16)(UInt16(1))
                )
              )
            ),
            body = Seq(
              BinaryArithmetic(BinaryOperator.ADD, "%x", "$zero", "$one")
            ),
            Seq(
              Loc(
                Map("x" -> IntValue(0), "y" -> IntValue(1))
              )
            )
          )
        ),
        Seq(
          Layout(
            Map("x" -> IntValue(10), "y" -> IntValue(32))
          )
        )
      )

  }
  it should "fail transforming an invalid program" in {
    assertThrows[CompilationFailureException] {
      backend(AssemblyParser(invalid_program, ctx), ctx)
    }
  }

}
