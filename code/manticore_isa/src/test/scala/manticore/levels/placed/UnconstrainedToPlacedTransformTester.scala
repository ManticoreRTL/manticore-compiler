package manticore.levels.placed

import manticore.UnitTest
import manticore.assembly.parser.UnconstrainedAssemblyParser
import manticore.assembly.levels.placed.UnconstrainedToPlacedTransform
import scala.language.postfixOps
import manticore.assembly.levels.placed.PlacedIR

import manticore.assembly.levels.ConstLogic
import manticore.assembly.BinaryOperator
import manticore.assembly.AssemblyAnnotation

import manticore.assembly.levels.RegLogic
import manticore.assembly.levels.UInt16
import manticore.compiler.AssemblyContext
import java.io.File
import manticore.assembly.parser.AssemblyParser
import manticore.assembly.CompilationFailureException

class UnconstrainedToPlacedTransformTester extends UnitTest {

  behavior of "unconstrained IR ro placed IR tranform"

  val valid_program =
    """
@LAYOUT [x = "10", y = "32"]
.prog :
    @LOC [x = "0", y = "1"]
    .proc p0:
        .const $zero 16 0x0
        .const $one  16 0x1
        .reg   %x    16;
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
  implicit val context = AssemblyContext()

  
  val backend = UnconstrainedToPlacedTransform
  

  it should "correctly transform a valid program" in {
    import PlacedIR._
    backend(AssemblyParser(valid_program)) shouldBe
      DefProgram(
        Seq(
          DefProcess(
            id = ProcesssIdImpl("p0", 0, 1),
            registers = Seq(
              DefReg(LogicVariable("$zero", 0, ConstLogic), Some(UInt16(0))),
              DefReg(LogicVariable("$one", 1, ConstLogic), Some(UInt16(1))),
              DefReg(LogicVariable("%x", 2, RegLogic), None)
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
              AssemblyAnnotation(
                "LOC",
                Map("x" -> 0.toString, "y" -> 1.toString)
              )
            )
          )
        ),
        Seq(
            AssemblyAnnotation(
                "LAYOUT",
                Map("x" -> "10", "y" -> "32")
            )
        )
      )

  }
  it should "fail transforming an invalid program" in {
    assertThrows[CompilationFailureException] {
      backend(AssemblyParser(invalid_program))
    }
  }

}
