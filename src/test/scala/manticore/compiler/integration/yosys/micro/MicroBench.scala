package manticore.compiler.integration.yosys.micro

import manticore.compiler.AssemblyContext
import manticore.compiler.FileDescriptor
import manticore.compiler.LoggerId
import manticore.compiler.UnitFixtureTest
import manticore.compiler.UnitTestMatchers
import manticore.compiler.WithInlineVerilog
import manticore.compiler.assembly.levels.TransformationID
import manticore.compiler.assembly.levels.placed.CustomLutInsertion
import manticore.compiler.assembly.levels.placed.JumpLabelAssignmentTransform
import manticore.compiler.assembly.levels.placed.JumpTableNormalizationTransform
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.assembly.levels.placed.PlacedIRCloseSequentialCycles
import manticore.compiler.assembly.levels.placed.PlacedIRCommonSubExpressionElimination
import manticore.compiler.assembly.levels.placed.PlacedIRConstantFolding
import manticore.compiler.assembly.levels.placed.PlacedIRDeadCodeElimination
import manticore.compiler.assembly.levels.placed.PlacedIRTransformer
import manticore.compiler.assembly.levels.placed.PlacedNameChecker
import manticore.compiler.assembly.levels.placed.ProcessSplittingTransform
import manticore.compiler.assembly.levels.placed.UnconstrainedToPlacedTransform
import manticore.compiler.assembly.levels.placed.interpreter.AtomicInterpreter
import manticore.compiler.assembly.levels.placed.lowering.AbstractExecution
import manticore.compiler.assembly.levels.placed.lowering.Lowering
import manticore.compiler.assembly.levels.placed.lowering.UtilizationChecker
import manticore.compiler.assembly.levels.placed.parallel.AnalyticalPlacerTransform
import manticore.compiler.assembly.levels.placed.parallel.BalancedSplitMergerTransform
import manticore.compiler.assembly.levels.placed.parallel.BlackBoxParallelization
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedCloseSequentialCycles
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedDeadCodeElimination
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRCommonSubExpressionElimination
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRConstantFolding
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRParMuxDeconstructionTransform
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIRStateUpdateOptimization
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedInterpreter
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedJumpTableConstruction
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedMakeDebugSymbols
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedNameChecker
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedOrderInstructions
import manticore.compiler.assembly.levels.unconstrained.width.WidthConversion
import manticore.compiler.assembly.parser.AssemblyFileParser
import manticore.compiler.frontend.yosys.Yosys
import manticore.compiler.frontend.yosys.YosysRunner
import manticore.compiler.frontend.yosys.YosysVerilogReader
import manticore.compiler.integration.yosys.unit.YosysUnitTest

import scala.collection.mutable.ArrayBuffer
import java.nio.file.Path
import java.io.PrintWriter

abstract class MicroBench extends UnitFixtureTest with UnitTestMatchers {

  type TestConfig

  behavior of benchName

  def timeOut: Int = 1000
  def benchName: String

  val randGen = new scala.util.Random(7891268)

  def verilogSources(cfg: TestConfig): Seq[FileDescriptor]

  def hexSources(cfg: TestConfig): Seq[FileDescriptor]

  def testBench(cfg: TestConfig): FileDescriptor

  def outputReference(testSize: TestConfig): ArrayBuffer[String]

  def testCase(label: String, cfg: TestConfig): Unit = {

    def readResource(res: FileDescriptor): String = {
      scala.io.Source.fromFile(res.p.toFile()).getLines().mkString("\n")
    }

    s"$label" should "match expected results" in { fixture =>
      // Read each verilog source and concatenate them together.
      val tbCode     = testBench(cfg)
      val allVerilog = verilogSources(cfg) :+ tbCode
      val finalVerilog = allVerilog.map(res => readResource(res)).mkString("\n")
      val vFilePath = fixture.dump("benchmark.sv", finalVerilog)

      // The hex files must be copied to the same directory as the single concatenated verilog file.
      hexSources(cfg).foreach { res =>
        val name = res.p.toString().split("/").last
        val content = readResource(res)
        fixture.dump(name, content)
      }

      implicit val ctx = AssemblyContext(
        dump_all = false,
        dump_dir = Some(fixture.test_dir.toFile),
        quiet = false,
        log_file = Some(fixture.test_dir.resolve("run.log").toFile()),
        max_cycles = timeOut,
        max_dimx = 16,
        max_dimy = 16,
        debug_message = false,
        max_registers = 2048,
        max_carries = 64,
        optimize_common_custom_functions = true,
        placement_timeout_s = 10
        // log_file = None,
      )

      val yosysCompiler = YosysVerilogReader andThen
        Yosys.PreparationPasses andThen
        Yosys.LoweringPasses andThen
        YosysRunner(fixture.test_dir) andThen
        CompilationStage.preparation

      val program1  = yosysCompiler(Seq(vFilePath))
      val reference = outputReference(cfg)
      val dumper    = { (n: String, t: String) => fixture.dump(n, t); () }
      // checkUnconstrained("yosys + ordering", program1, reference, dumper)
      val program2 = CompilationStage.unconstrainedOptimizations(program1)
      // checkUnconstrained("prelim opts", program2, reference, dumper)
      // ctx.logger.info(s"Stats: \n${ctx.stats.asYaml}")(LoggerId("Stats"))
      val program3 = CompilationStage.controlLowering(program2)
      // checkUnconstrained("jump table", program3, reference, dumper)
      val program4 = CompilationStage.widthLowering(program3)
      // checkUnconstrained("width conversion", program4, reference, dumper)
      val program5 = CompilationStage.translation(program4)
      // ctx.logger.info(s"Stats: \n${ctx.stats.asYaml}")(LoggerId("Stats"))
      // checkPlaced("translation", program5, reference, dumper)
      val program6 = CompilationStage.placedOptimizations(program5)
      // checkPlaced("placed opts", program6, reference, dumper)
      val program7 = CompilationStage.parallelization(program6)
      // checkPlaced("parallelization", program7, reference, dumper)
      // ctx.logger.info(s"Stats: \n${ctx.stats.asYaml}")(LoggerId("Stats"))
      val program8 = CompilationStage.customLuts(program7)
      // checkPlaced("custom luts", program8, reference, dumper)
      ctx.logger.info(s"Stats: \n${ctx.stats.asYaml}")(LoggerId("Stats"))

      // temporary disabled until I fix the bugs with lowering passes :(
      val program9 = CompilationStage.finalLowering(program8)
      fixture.dump("stats.yml", ctx.stats.asYaml)

      // do one final interpretation to make sure register allocation is correct
      // checking whether a schedule is correct (i.e., enough Nops, contention
      // free network) is done with a checker pass because atomic interpreter is
      // not sophisticated enough to do a full cycle-accurate simulation of a
      // manticore network.

      val serialOut = ArrayBuffer.empty[String]
      val interp = AtomicInterpreter.instance(
        program = program9,
        serial = Some(serialOut += _)
      )
      interp.interpretCompletion()

      if (ctx.logger.countErrors() > 0) {
        dumper("reference.txt", reference.mkString("\n"))
        dumper("results.txt", serialOut.mkString("\n"))
        ctx.logger.flush()
        fail(s"Complete schedule: failed due to earlier errors")
      }
      if (!YosysUnitTest.compare(reference, serialOut)) {
        dumper("reference.txt", reference.mkString("\n"))
        dumper("results.txt", serialOut.mkString("\n"))
        ctx.logger.flush()
        fail(s"Complete schedule: results did not match the reference")
      }
      if (ctx.logger.countErrors() > 0) {
        dumper("reference.txt", reference.mkString("\n"))
        dumper("results.txt", serialOut.mkString("\n"))
        ctx.logger.flush()
        fail(s"Complete schedule: Errors occurred")
      }
    }
  }

  def copyResource(res: FileDescriptor, cwd: Path): Path = {
    val name = res.p.toString().split("/").last
    val targetPath = cwd.resolve(name)
    val content = scala.io.Source.fromFile(res.p.toFile()).getLines().mkString("\n")
    val writer = new PrintWriter(targetPath.toFile())
    writer.write(content)
    writer.flush()
    writer.close()
    targetPath
  }

  def interpretUnconstrained(
      program: UnconstrainedIR.DefProgram
  )(implicit
      ctx: AssemblyContext
  ): ArrayBuffer[String] = {
    val closed    = UnconstrainedCloseSequentialCycles(program)
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
    if (ctx.logger.countErrors() > 0) {
      ctx.logger.flush()
      dumper("reference.txt", reference.mkString("\n"))
      dumper("results.txt", got.mkString("\n"))
      fail(s"${clue}: failed due to earlier errors")
    }
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
    if (ctx.logger.countErrors() > 0) {
      dumper("reference.txt", reference.mkString("\n"))
      dumper("results.txt", got.mkString("\n"))
      fail(s"${clue}: failed due to earlier errors")
    }
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
    UnconstrainedJumpTableConstruction.withCondition(false) andThen
      UnconstrainedNameChecker andThen
      UnconstrainedIRParMuxDeconstructionTransform andThen
      UnconstrainedNameChecker

  val widthLowering =
    WidthConversion.transformation andThen
      UnconstrainedIRConstantFolding andThen
      UnconstrainedIRStateUpdateOptimization andThen
      UnconstrainedIRCommonSubExpressionElimination andThen
      UnconstrainedDeadCodeElimination andThen
      UnconstrainedNameChecker

  val translation =
    UnconstrainedToPlacedTransform

  val placedOptimizations =
    PlacedIRConstantFolding andThen
      PlacedIRCommonSubExpressionElimination andThen
      PlacedIRDeadCodeElimination

  val parallelization =
    BalancedSplitMergerTransform andThen
      // ProcessSplittingTransform andThen
      PlacedNameChecker andThen
      AnalyticalPlacerTransform

  val customLuts =
    CustomLutInsertion andThen
      PlacedIRDeadCodeElimination

  val finalLowering = Lowering.Transformation andThen
    AbstractExecution andThen UtilizationChecker

}
