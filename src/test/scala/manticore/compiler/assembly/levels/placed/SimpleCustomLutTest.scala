package manticore.compiler.assembly.levels.placed

import manticore.compiler.UnitTest

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedDeadCodeElimination
import manticore.compiler.ManticorePasses
import java.nio.file.Paths
import java.nio.file.Files
import manticore.compiler.UnitFixtureTest
import manticore.compiler.ManticorePasses
import manticore.compiler.assembly.levels.UInt16
import scala.collection.mutable.{HashMap => MHashMap, ArrayBuffer}
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedNameChecker
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedMakeDebugSymbols
import manticore.compiler.assembly.levels.placed.interpreter.AtomicInterpreter

class SimpleCustomLutTester extends UnitFixtureTest {

  import manticore.compiler.assembly.levels.placed.PlacedIR._

  behavior of "custom lut insertion transform"

  // Creates a program that reverses the order of bits in a 16-bit constant and
  // checks if the result is correct.
  def mkProgram(f: FixtureParam): (String, AssemblyContext) = {
    val regs = ArrayBuffer.empty[String]
    val wires = ArrayBuffer.empty[String]
    val consts = ArrayBuffer.empty[String]
    val instrs = ArrayBuffer.empty[String]

    def mkReg(name: String, init: Option[UInt16]): String = {
      val res = Seq(
        s"@REG [id = \"${name}\", type = \"\\REG_CURR\" ]",
        s".input ${name}_curr 16 ${init.map(_.toString).getOrElse("")}",
        s"@REG [id = \"${name}\", type = \"\\REG_NEXT\" ]",
        s".output ${name}_next 16 "
      ).mkString("\n")

      regs += res
      res
    }

    def mkWire(name: String): String = {
      val track = s"@DEBUGSYMBOL [symbol = \"${name}\"]"
      val res = s".wire ${name} 16"

      wires += s"${track}\n${res}"
      res
    }

    def mkConst(name: String, init: UInt16): String = {
      val track = s"@DEBUGSYMBOL [symbol = \"${name}\"]"
      val res = s".const ${name} 16 ${init}"

      consts += s"${track}\n${res}"
      res
    }

    val ctx = AssemblyContext(
      dump_all = true,
      dump_dir = Some(f.test_dir.toFile()),
      debug_message = true,
      max_custom_instructions = 32,
      max_custom_instruction_inputs = 6,
      max_dimx = 1,
      max_dimy = 1,
      log_file = Some(f.test_dir.resolve("output.log").toFile())
    )

    def getFreshName = s"gen_${ctx.uniqueNumber()}"

    // Given an input name, this function extracts its 16 bits and places them in 16 named wires.
    // The constants and temporary wires needed to generate the result are automatically created.
    //
    // Ex: inputName = test
    //
    // SRL srl_0, test, 0
    // AND and_0, srl_0, 1 // bit 0 extracted into and_0
    // SRL srl_1, test, 1
    // AND and_1, srl_1, 1 // bit 1 extracted into and_1
    // ...
    // SRL srl_15, test, 15
    // AND and_15, srl_15, 1 // bit 15 extracted into and_15
    def extractAllInputBits(
      inputName: String
    ):
      Map[Int, String] // Maps the bit indices of the input to a named wire (that can be used by other instructions).
    = {
      val inputBitIdx = MHashMap.empty[Int, String]

      def extractBit(
        inputName: String,
        bitIdx: Int
      ): Unit = {
        assert(bitIdx < 16, s"Error: ${bitIdx} is too large to shift.")

        val shiftAmountName = getFreshName
        mkConst(shiftAmountName, UInt16(bitIdx))

        val maskName = getFreshName
        mkConst(maskName, UInt16(1))

        val srlName = getFreshName
        mkWire(srlName)

        val andName = getFreshName
        mkWire(andName)
        inputBitIdx += bitIdx -> andName

        instrs += s"SRL ${srlName}, ${inputName}, ${shiftAmountName};"
        instrs += s"AND ${andName}, ${srlName}, ${maskName};"
      }

      Range(0, 16).foreach { idx =>
        extractBit(inputName, idx)
      }

      inputBitIdx.toMap
    }

    // Given a list of input names, this function concatenates the LSb of each together into a
    // single 16-bit wire.
    // The constants and temporary wires needed to generate the result are automatically created.
    //
    // Ex: inputName = Seq(test_0, test_1, ..., test_15)
    //
    // SLL sll_0, test_0, 0
    // AND and_0, sll_0, 1 // LSb of test_0 shifted to correct position in and_0
    // SLL sll_1, test_1, 1
    // AND and_1, sll_1, 2 // LSb of test_1 shifted to correct position in and_1
    // ...
    // SLL sll_15, test_15, 15
    // AND and_15, sll_15, 32768 // LSb of test_15 shifted to correct position in and_15
    //
    // OR or_0, and_0, and_1
    // OR or_1, or_0, and_2
    // OR or_2, or_1, and_3
    // ...
    // OR or_14, or_13, and_15 // Concatenate bits together.
    def concatenateLsb(
      inputNames: Seq[String]
    ):
      String // Name of the concatenated result.
    = {
      def placeBit(
        inputName: String,
        bitIdx: Int
      ):
        String // Name of shifted result.
      = {
        assert(bitIdx < 16, s"Error: ${bitIdx} is too large to shift.")

        val shiftAmountName = getFreshName
        mkConst(shiftAmountName, UInt16(bitIdx))

        val maskName = getFreshName
        mkConst(maskName, UInt16(1 << bitIdx))

        val sllName = getFreshName
        mkWire(sllName)

        val andName = getFreshName
        mkWire(andName)

        instrs += s"SLL ${sllName}, ${inputName}, ${shiftAmountName};"
        instrs += s"AND ${andName}, ${sllName}, ${maskName};"

        andName
      }

      val lsbShiftedNames = inputNames.zipWithIndex.map { case (inputName, idx) =>
        placeBit(inputName, idx)
      }

      // Concatenation is performed by OR-ing the names that contain the shifted results.
      val resName = lsbShiftedNames.tail.foldLeft(lsbShiftedNames.head) { case (prevName, lsbShiftedName) =>
        val orName = getFreshName
        mkWire(orName)

        instrs += s"OR ${orName}, ${lsbShiftedName}, ${prevName};"
        orName
      }

      resName
    }

    val inputPatternName = "pattern_in"
    mkConst(inputPatternName, UInt16("0100011010001000", 2))
    val outputPatternName = "pattern_out"
    mkConst(outputPatternName, UInt16("0001000101100010", 2))

    val inputBitIdxToNameMap = extractAllInputBits(inputPatternName)
    val inputBitIdxNames = inputBitIdxToNameMap
      .toSeq
      .sortBy { case (bitIdx, bitIdxName) =>
        bitIdx
      }.map { case (bitIdx, bitIdxName) =>
        bitIdxName
      }

    // Reverse the bit order and concatenate the results together.
    val inputBitIdxNamesReversed = inputBitIdxNames.reverse
    val resName = concatenateLsb(inputBitIdxNamesReversed)

    // Check that the final result matches the expected result.
    instrs += s"@TRAP [ type = \"\\fail\" ]"
    instrs += s"EXPECT ${resName}, ${outputPatternName}, [\"Wrong result!\"];"

    // Stop the program by creating an assertion that MUST fail (but that does not throw an exception).
    instrs += s"@TRAP [ type = \"\\stop\" ]"
    instrs += s"EXPECT ${inputPatternName}, ${outputPatternName}, [\"stopped\"];"

    val program = s""".prog:
                     |   .proc Main:
                     |      ${regs.mkString("\n")}
                     |      ${wires.mkString("\n")}
                     |      ${consts.mkString("\n")}
                     |      ${instrs.mkString("\n")}
                     |""".stripMargin

    (program, ctx)
  }

  it should "correctly identify LUTs" in { f =>
    val (programOrig, ctx) = mkProgram(f)
    val programOrigUnconstrained = AssemblyParser(programOrig, ctx)

    val lowerCompiler =
      UnconstrainedNameChecker followedBy
      UnconstrainedMakeDebugSymbols followedBy
      UnconstrainedToPlacedTransform

    val programPlaced = lowerCompiler(programOrigUnconstrained, ctx)._1
    f.dump(
      s"placed_human.masm",
      PlacedIRDebugSymbolRenamer.makeHumanReadable(programPlaced)(ctx).serialized
    )

    val lutCompiler =
      CustomLutInsertion followedBy
      PlacedIRDeadCodeElimination

    withClue("The program without LUTs should successfully run:") {
      // Interpret the placed program to ensure it does not fail.
      // If it crashes, then the program is ill-formed.
      AtomicInterpreter(programPlaced, ctx)
    }

    withClue("The program with LUTs should successfully run:") {
      val placedProgramWithLuts = lutCompiler(programPlaced, ctx)._1
      f.dump(
        s"lut_human.masm",
        PlacedIRDebugSymbolRenamer.makeHumanReadable(placedProgramWithLuts)(ctx).serialized
      )

      // Interpret the optimized program to ensure it does not fail.
      // If it crashes, then the program is incorrect (as the previous lowered program
      // is correct if we reached this point)
      AtomicInterpreter(placedProgramWithLuts, ctx)
    }

  }

}
