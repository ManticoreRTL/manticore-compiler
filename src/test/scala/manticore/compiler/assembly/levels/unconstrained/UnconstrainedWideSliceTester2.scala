package manticore.compiler.assembly.levels.unconstrained

import java.nio.file.Path
import java.io.PrintWriter
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.UnitFixtureTest

class UnconstrainedWideSliceTester2 extends UnconstrainedWideTest {

  // Creates a program that declares 1 random wide constant, then proceeds to
  // slice it at random intervals `numTests` times. The results are checked with
  // an EXPECT.
  def mkProgram(
    f: FixtureParam,
    numTests: Int,
    maxWidth: Int
  ): String = {
    def const_zero_name = "zero"
    def const_one_name = "one"
    def input_name = "input"
    def expected_name(idx: Int) = s"expected_${idx}"
    def computed_name(idx: Int) = s"computed_${idx}"
    // The space at the beginning is for the declarations to line up correctly
    // in the generated code.
    def declareConst(name: String, width: Int, value: BigInt) = s".const ${name} ${width} 0b${value.toString(2)}"
    def declareWire(name: String, width: Int) = s".wire ${name} ${width}"
    def declareSlice(rd: String, rs: String, offset: Int, length: Int) = s"SLICE ${rd}, ${rs}, ${offset}, ${length};"
    def declareExpect(ref: String, got: String, message: String) = {
      Seq(
        "@TRAP [type = \"\\fail\"]",
        s"EXPECT ${ref}, ${got}, [\"${message}\"];"
      ).mkString("\n")
    }

    val constWidth = randgen.nextInt(maxWidth)
    val constVal = mkWideRand(constWidth)

    // Generate random slice intervals.

    val (sliceOffsets, sliceLengths) = Array.fill(numTests) {
      // We are using a 0-based indexing scheme, so the largest offset is one
      // less than the width.
      val ofst = randgen.nextInt(constWidth - 1)
      val maxLen = constWidth - ofst
      val len = randgen.nextInt(maxLen)
      (ofst, len)
    }.unzip

    // Pre-compute slice results.

    val res = sliceOffsets.zip(sliceLengths).zipWithIndex.map { case ((offset, length), idx) =>
      // Slicing is equivalent to shifting right and masking.
      val mask = (BigInt(1) << length) - 1
      (constVal >> offset) & mask
    }

    // (1) Declare constant for single wide input constant used as the argument of all slices.
    // (2) Declare constant for every EXPECTED result.
    // (3) Declare wire for every COMPUTED result.
    // (4) Declare a SLICE operation for every COMPUTED result.
    // (5) Declare an EXPECT operation for every COMPUTED result to check for correctness.

    val inputDecl = declareConst(input_name, constWidth, constVal)

    val expectedDecls = sliceOffsets.zip(sliceLengths).zip(res).zipWithIndex.map { case (((offset, length), res), idx) =>
      declareConst(expected_name(idx), constWidth, res)
    }.mkString("\n")

    val computedWireDecls = sliceOffsets.zip(sliceLengths).zip(res).zipWithIndex.map { case (((offset, length), res), idx) =>
      declareWire(computed_name(idx), length)
    }.mkString("\n")

    val sliceDecls = sliceOffsets.zip(sliceLengths).zipWithIndex.map { case ((offset, length), idx) =>
      val rd = computed_name(idx)
      val rs = input_name
      declareSlice(rd, rs, offset, length)
    }.mkString("\n")

    val expectDecls = (0 until numTests).map { idx =>
      val ref = expected_name(idx)
      val got = computed_name(idx)
      declareExpect(ref, got, "failed")
    }.mkString("\n")

    s"""
    .prog:
        .proc proc_0_0:

            ${declareConst(const_zero_name, 1, 0)}
            ${declareConst(const_one_name, 1, 1)}
            ${inputDecl}
            ${expectedDecls}
            ${computedWireDecls}
            ${sliceDecls}
            ${expectDecls}

            @TRAP [type = "\\stop"]
            EXPECT ${const_zero_name}, ${const_one_name}, ["stopped"];
    """
  }

  behavior of "unconstrained wide slice"

  it should "correctly handle the slice" taggedAs Tags.WidthConversion in { f =>
    val prog_text = mkProgram(f, 1000, 90)
    val parsed = AssemblyParser(prog_text, f.ctx)
    // println(parsed.serialized)
    // println(UnconstrainedIRDebugSymbolRenamer.makeHumanReadable(parsed)(f.ctx).serialized)
    val lowered = backend(parsed, f.ctx)._1
    // println(lowered.serialized)
    // println(UnconstrainedIRDebugSymbolRenamer.makeHumanReadable(lowered)(f.ctx).serialized)
  }
}
