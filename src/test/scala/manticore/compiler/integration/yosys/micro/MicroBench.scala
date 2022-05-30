package manticore.compiler.integration.yosys.micro

import manticore.compiler.UnitFixtureTest
import manticore.compiler.UnitTestMatchers
import scala.collection.mutable.ArrayBuffer
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedCloseSequentialCycles
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedInterpreter
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.assembly.levels.placed.PlacedIRCloseSequentialCycles
import manticore.compiler.assembly.levels.placed.JumpTableNormalizationTransform
import manticore.compiler.assembly.levels.placed.JumpLabelAssignmentTransform
import manticore.compiler.assembly.levels.placed.interpreter.AtomicInterpreter
import manticore.compiler.integration.yosys.unit.YosysUnitTest
import manticore.compiler.assembly.parser.AssemblyFileParser
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedNameChecker
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedMakeDebugSymbols
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedOrderInstructions
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRConstantFolding
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRStateUpdateOptimization
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRCommonSubExpressionElimination
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedDeadCodeElimination
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedJumpTableConstruction
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRParMuxDeconstructionTransform
import manticore.compiler.assembly.levels.unconstrained.width.WidthConversion
import manticore.compiler.assembly.levels.placed.UnconstrainedToPlacedTransform
import manticore.compiler.assembly.levels.placed.PlacedIRConstantFolding
import manticore.compiler.assembly.levels.placed.PlacedIRCommonSubExpressionElimination
import manticore.compiler.assembly.levels.placed.PlacedIRDeadCodeElimination
import manticore.compiler.assembly.levels.placed.ProcessSplittingTransform
import manticore.compiler.frontend.yosys.YosysVerilogReader
import manticore.compiler.frontend.yosys.Yosys
import manticore.compiler.frontend.yosys.YosysRunner

abstract class MicroBench extends UnitFixtureTest with UnitTestMatchers {

  type TestConfig

  behavior of benchName

  def timeOut: Int = 1000
  def benchName: String

  val randGen = new scala.util.Random(7891268)

  def resource: String

  def testBench(cfg: TestConfig): String

  def outputReference(testSize: TestConfig): ArrayBuffer[String]

  def testCase(label: String, cfg: TestConfig): Unit = {

    s"$label" should "match expected results" in { fixture =>
      val verilogCode = scala.io.Source
        .fromResource(resource)
        .getLines()
        .mkString("\n") + "\n" + testBench(cfg)

      val vFilePath = fixture.dump(resource.split("/").last, verilogCode)

      implicit val ctx = AssemblyContext(
        dump_all = true,
        dump_dir = Some(fixture.test_dir.toFile),
        quiet = false,
        log_file = Some(fixture.test_dir.resolve("run.log").toFile()),
        max_cycles = timeOut,
        max_dimx = 10,
        max_dimy = 10
        // log_file = None
      )

      val yosysCompiler = YosysVerilogReader andThen
        Yosys.PreparationPasses andThen
        Yosys.LoweringPasses andThen
        YosysRunner(fixture.test_dir) andThen
        CompilationStage.preparation

      val program1 = yosysCompiler(Seq(vFilePath))
      val reference = outputReference(cfg)
      val dumper = { (n: String, t: String) => fixture.dump(n, t); () }
      checkUnconstrained("yosys + ordering", program1, reference, dumper)
      val program2 = CompilationStage.unconstrainedOptimizations(program1)
      checkUnconstrained("prelim opts", program2, reference, dumper)
      val program3 = CompilationStage.controlLowering(program2)
      checkUnconstrained("jump table", program3, reference, dumper)
      val program4 = CompilationStage.widthLowering(program3)
      checkUnconstrained("width conversion", program4, reference, dumper)
      val program5 = CompilationStage.translation(program4)
      checkPlaced("translation", program5, reference, dumper)
      val program6 = CompilationStage.placedOptimizations(program5)
      checkPlaced("placed opts", program6, reference, dumper)
      val program7 = CompilationStage.parallelization(program6)
      checkPlaced("parallelization", program7, reference, dumper)
    }
  }

  def interpretUnconstrained(
      program: UnconstrainedIR.DefProgram
  )(implicit
      ctx: AssemblyContext
  ): ArrayBuffer[String] = {
    val closed = UnconstrainedCloseSequentialCycles(program)
    val serialOut = ArrayBuffer.empty[String]
    val interp = UnconstrainedInterpreter.instance(
      program = closed,
      serial = Some(ln => serialOut += ln)
    )
    interp.runCompletion()
    serialOut
  }
  def interpretPlaced(program: PlacedIR.DefProgram)(implicit
      ctx: AssemblyContext
  ): ArrayBuffer[String] = {
    val serialOut = ArrayBuffer.empty[String]
    val closed =
      (PlacedIRCloseSequentialCycles andThen
        JumpTableNormalizationTransform andThen
        JumpLabelAssignmentTransform)(program)
    val interp = AtomicInterpreter.instance(
      program = closed,
      serial = Some(serialOut += _)
    )
    interp.interpretCompletion()
    serialOut
  }

  def checkUnconstrained(
      clue: String,
      program: UnconstrainedIR.DefProgram,
      reference: ArrayBuffer[String],
      dumper: (String, String) => Unit
  )(implicit ctx: AssemblyContext) = {
    val got = interpretUnconstrained(program)
    if (!YosysUnitTest.compare(reference, got)) {
      ctx.logger.flush()
      dumper("reference.txt", reference.mkString("\n"))
      dumper("results.txt", got.mkString("\n"))
      fail(s"${clue}: results did not match the reference")
    }
    if (ctx.logger.countErrors() > 0) {
      ctx.logger.flush()
      dumper("reference.txt", reference.mkString("\n"))
      dumper("results.txt", got.mkString("\n"))
      fail(s"${clue}: Errors occurred")
    }
  }

  def checkPlaced(
      clue: String,
      program: PlacedIR.DefProgram,
      reference: ArrayBuffer[String],
      dumper: (String, String) => Unit
  )(implicit ctx: AssemblyContext) = {

    val got = interpretPlaced(program)
    if (!YosysUnitTest.compare(reference, got)) {
      ctx.logger.flush()
      dumper("reference.txt", reference.mkString("\n"))
      dumper("results.txt", got.mkString("\n"))
      fail(s"${clue}: results did not match the reference")
    }
    if (ctx.logger.countErrors() > 0) {
      ctx.logger.flush()
      dumper("reference.txt", reference.mkString("\n"))
      dumper("results.txt", got.mkString("\n"))
      fail(s"${clue}: Errors occurred")
    }
  }
  object CompilationStage {

    // the very first stage. Only reorder instructions according to
    // topological order
    val preparation = AssemblyFileParser andThen
      UnconstrainedNameChecker andThen
      UnconstrainedMakeDebugSymbols andThen
      UnconstrainedOrderInstructions

    val unconstrainedOptimizations =
      UnconstrainedIRConstantFolding andThen
        UnconstrainedIRStateUpdateOptimization andThen
        UnconstrainedIRCommonSubExpressionElimination andThen
        UnconstrainedDeadCodeElimination andThen
        UnconstrainedNameChecker

    val controlLowering =
      UnconstrainedJumpTableConstruction andThen
        UnconstrainedNameChecker andThen
        UnconstrainedIRParMuxDeconstructionTransform andThen
        UnconstrainedNameChecker

    val widthLowering =
      WidthConversion.transformation andThen
        UnconstrainedIRConstantFolding andThen
        UnconstrainedDeadCodeElimination andThen
        UnconstrainedIRConstantFolding

    val translation =
      UnconstrainedToPlacedTransform

    val placedOptimizations =
      PlacedIRConstantFolding
    PlacedIRCommonSubExpressionElimination andThen
      PlacedIRDeadCodeElimination

    val parallelization = ProcessSplittingTransform
  }

}
