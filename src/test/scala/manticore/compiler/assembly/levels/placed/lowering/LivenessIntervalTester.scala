package manticore.compiler.assembly.levels.placed.lowering

import manticore.compiler.UnitFixtureTest
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedNameChecker
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedMakeDebugSymbols
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedRenameVariables
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedOrderInstructions
import manticore.compiler.assembly.levels.placed.UnconstrainedToPlacedTransform
import manticore.compiler.assembly.levels.placed.PlacedIRCloseSequentialCycles
import manticore.compiler.assembly.utils.XorShift16
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedJumpTableConstruction
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.assembly.levels.placed.PlacedIRDebugSymbolRenamer
import manticore.compiler.assembly.levels.unconstrained.width.WidthConversion
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRConstantFolding
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRCommonSubExpressionElimination
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedDeadCodeElimination
import manticore.compiler.assembly.utils.XorShift128
import manticore.compiler.assembly.levels.placed.JumpTableNormalizationTransform
import manticore.compiler.assembly.levels.placed.JumpLabelAssignmentTransform
import manticore.compiler.assembly.levels.placed.LocalMemoryAllocation

class LivenessIntervalTester extends UnitFixtureTest {

  behavior of "Lifetime interval"

  val compiler = AssemblyParser andThen
    UnconstrainedNameChecker andThen
    UnconstrainedMakeDebugSymbols andThen
    UnconstrainedRenameVariables andThen
    UnconstrainedJumpTableConstruction andThen
    WidthConversion.transformation andThen
    UnconstrainedIRConstantFolding andThen
    UnconstrainedIRCommonSubExpressionElimination andThen
    UnconstrainedDeadCodeElimination andThen
    UnconstrainedToPlacedTransform andThen
    JumpTableNormalizationTransform andThen
    ProgramSchedulingTransform andThen
    JumpLabelAssignmentTransform andThen
    LocalMemoryAllocation andThen
    RegisterAllocationTransform

  it should "do something" in { fixture =>
    val numState = 5

    assert(numState < 32)
    // val depth = 10
    val randGen = Seq.tabulate(numState) { i => XorShift16(s"r$i", UInt16(i)) }
    val dummyCode = XorShift128("rwide")
    def generate(fn: Int => String) =
      Seq.tabulate(numState) { i => fn(i) }.mkString("\n")

    def createTree(index: Int): String = {

      val builder = new StringBuilder
      val last = Range(1, index).foldLeft(randGen(0).randNext) {
        case (prev, j) =>
          builder ++= s"ADD x_${index}_${j}, ${randGen(j).randNext}, ${prev};\n"
          s"x_${index}_${j}"
      }
      builder ++= s"MOV rs_${index}, ${last};\n"
      builder.toString()
    }

    def createWires(index: Int): String = {
      Range(1, index).map { j =>
        s".wire x_${index}_${j} 16"
      } :+ s".wire rs_${index} 16"
    }.mkString("\n")
    val text = s"""

            .prog:
            .proc p0:


                ${generate(createWires)}
                ${generate { i => s".wire cond$i 1" }}
                ${generate { i => s".const const$i  5 $i" }}

                .const zero  16 0
                .const one   16 1
                .const two   16 2
                .const three 16 3

                @TRACK[name = "result"]
                .reg R 16 .input rC 0 .output rN
                .wire cast5 5
                .wire out 16

                @TRACK[name = "dummy"]
                .wire dummy 32

                ${dummyCode.registers}



                ${randGen.map { _.registers }.mkString("\n")}

                ${randGen.map { _.code }.mkString("\n")}

                SRL cast5, rC, zero;


                ${generate(createTree)}
                ${generate { i => s"SEQ cond$i, cast5, const$i;" }}
                PARMUX out, ${Seq
      .tabulate(numState) { i => s"cond$i ? rs_$i" }
      .mkString(", ")}, ${randGen(0).randNext};

                MOV rN, out;

                ${dummyCode.code}

                MOV dummy, ${dummyCode.randNext};


        """

    fixture.dump("test.masm", text)
    // println(text)

    implicit val ctx = AssemblyContext(
      output_dir = Some(fixture.test_dir.resolve("out").toFile()),
      dump_all = true,
      dump_dir = Some(fixture.test_dir.resolve("dumps").toFile()),
      expected_cycles = Some(100),
      debug_message = true
      // log_file = Some(fixture.test_dir.resolve("run.log").toFile())
    )

    val compiled = compiler(text)
    fixture.dump(
      "human_readable.masm",
      PlacedIRDebugSymbolRenamer.makeHumanReadable(compiled)(ctx).serialized
    )




  }

}
