package manticore.compiler.assembly.levels.placed.parallel

import manticore.compiler.assembly.levels.placed.parallel.util.BasicProcessExtraction
import manticore.compiler.assembly.levels.placed.PlacedIRTransformer
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.assembly.levels.placed.Helpers.NameDependence
import manticore.compiler.assembly.levels.placed.Helpers.ProgramStatistics
import manticore.compiler.assembly.annotations.Loc
import manticore.compiler.assembly.levels.placed.parallel.util.HyperMapperConfig
import manticore.compiler.assembly.levels.placed.parallel.util.ProcessorDescriptor
import manticore.compiler.assembly.levels.placed.parallel.util.HyperMapper
import java.io.BufferedReader
import java.util.concurrent.BlockingQueue
import java.io.BufferedWriter
import scala.annotation.tailrec
import scala.util.Success
import scala.util.Failure
import scala.collection.BitSet
import scala.concurrent.duration.Duration
object BlackBoxParallelization extends BasicProcessExtraction {
  import PlacedIR._

  /**
    * A simple function to estimate the cost
    *
    * @param processes
    * @param parContext
    * @param dimX
    * @param dimY
    */
  case class VCycle(v: Int, feasible: Boolean)
  final class CostEstimator(
      processes: Iterable[ProcessorDescriptor],
      parContext: ParallelizationContext,
      dimX: Int,
      dimY: Int,
      maxMemory: Int
  ) extends (Iterable[(Int, Int)] => VCycle) {
    private def isPrivileged(p: ProcessorDescriptor): Boolean =
      p.body.exists(i => parContext.getInstruction(i).isInstanceOf[PrivilegedInstruction])
    val movables   = processes.filterNot(isPrivileged).toIndexedSeq
    val privileged = processes.find(isPrivileged)

    def merged(mapping: Iterable[(Int, Int)]): Array[Array[ProcessorDescriptor]] = {
      val noc = Array.ofDim[ProcessorDescriptor](dimX, dimY)
      // init to empty processes
      for (x <- 0 until dimX; y <- 0 until dimY) { noc(x)(y) = ProcessorDescriptor.empty }
      // initialize the privileged processor
      privileged.foreach { priv => noc(0)(0) = priv }

      def asXY(l: Int) = {
        // l = x * dimY + y
        val x = l / dimY
        val y = l % dimY
        (x, y)
      }

      // merge processes into processors using the mappings
      for ((index, assignment) <- mapping) {
        val (x, y) = asXY(assignment)
        noc(x)(y) = noc(x)(y).merged(movables(index))
      }
      noc
    }
    def cost(mapping: Iterable[(Int, Int)]) = {

      val noc = merged(mapping)

      // now find the straggler
      var max      = 0
      var feasible = true
      var maxX     = -1
      var maxY     = -1

      def incr(xx: Int, yy: Int): (Int, Int) = {
        if (xx == (dimX - 1)) {
          (0, yy + 1)
        } else {
          (xx + 1, yy)
        }
      }

      var sol = VCycle(0, true)
      for (x <- 0 until dimX; y <- 0 until dimY) {
        val processor  = noc(x)(y)
        val execTime   = processor.body.size + processor.inBound.size
        val memoryUsed = processor.memory.foldLeft(0) { case (acc, i) => acc + parContext.memorySize(i) }
        if (execTime >= sol.v && memoryUsed <= maxMemory) {
          sol = VCycle(execTime, sol.feasible)
        } else if (memoryUsed > maxMemory) {
          sol = VCycle(sol.v, false)
        }
      }
      sol
    }

    override def apply(mapping: Iterable[(Int, Int)]): VCycle = cost(mapping)

  }

  private class HyperMapperClient(
      hmOut: BufferedReader,
      hmIn: BufferedWriter,
      err: BufferedReader,
      cfg: HyperMapperConfig,
      costFunction: CostEstimator
  )(implicit
      ctx: AssemblyContext
  ) {
    private var running                            = true
    private var bestSolution: Iterable[(Int, Int)] = _
    private var bestCost                           = VCycle(Int.MaxValue, false)
    private var timeComputingCost                  = 0.0
    object Matchers {
      val Request        = raw"Request ([0-9]*)".r
      val FRequest       = raw"FRequest ([0-9]*) (.*)".r
      val End            = "End"
      val EndHyperMapper = "End of HyperMapper"
      val Pareto         = "Pareto"
    }

    private def makeResponse(count: Int): Unit = {
      val keys    = hmOut.readLine().trim.split(",")
      val indices = keys.map(cfg.indexOf(_))
      val keysOut = keys ++ Seq(cfg.objective, HyperMapperConfig.validName)
      hmIn.write(keysOut.mkString(",") + "\n")
      for (rIdx <- 0 until count) {
        val values    = hmOut.readLine().trim.split(",").map(_.toInt)
        val (cost, t) = ctx.stats.timed(costFunction(indices zip values))
        timeComputingCost += t
        if (cost.feasible && cost.v <= bestCost.v) {
          bestCost = VCycle(cost.v, true)
          bestSolution = indices zip values
        }
        val resp = values.map(_.toString) ++ (cost match {
          case VCycle(v, true)  => Seq(v.toString(), HyperMapperConfig.trueValue)
          case VCycle(v, false) => Seq(v.toString(), HyperMapperConfig.falseValue)
        })

        hmIn.write(resp.mkString(",") + "\n")
      }
      hmIn.flush()

    }
    def run(): Iterable[(Int, Int)] = {
      try {
        ctx.logger.debug("Started HyperMapper client")
        ctx.stats.recordRunTime("blackbox optmization") {
          while (running) {
            val header = hmOut.readLine() // read the header, should be
            import Matchers._
            header match {
              case Request(n) =>
                ctx.logger.debug(s"HyperMapper requested ${n} points")
                makeResponse(n.toInt)
              case FRequest(n) => throw new UnsupportedOperationException(s"FRequest '$header' not supported!")
              case End | EndHyperMapper | Pareto =>
                running = false
                ctx.logger.info(s"HyperMapper finished: ${header}")
              case msg => throw new UnsupportedOperationException(s"Unknown request type '$msg'")
            }
          }
        }
        ctx.stats.recordRunTime("evaluating cost function", timeComputingCost)
        hmOut.close()
        hmIn.close()
        err.close()
        ctx.logger.info(s"Best solution is vcycle = ${bestCost.v}")
        bestSolution
      } catch {
        case e: Exception =>
          ctx.logger.error(s"Caught exception in HyperMapper client: ${e.getMessage()}")
          ctx.logger.fail("Irrecoverable error!")
      }

    }

  }
  override protected def doSplit(program: DefProgram)(implicit ctx: AssemblyContext): DefProgram = {

    assert(program.processes.length == 1)
    val parContext = createParallelizationContext(program.processes.head)
    val processes  = extractIndependentInstructionSequences(program.processes.head, parContext)

    val dimY = ctx.max_dimy
    val dimX = ctx.max_dimx

    val costEval = new CostEstimator(processes, parContext, dimX, dimY, ctx.max_local_memory / (16 / 8))
    val hmConfig = HyperMapperConfig(
      numCores = dimX * dimY,
      numProcesses = costEval.movables.size,
      numWarmUpSamples = processes.size + 1,
      optIterations = 200
    )

    val hyperMapperServer           = HyperMapper(hmConfig)
    val (hmReader, hmWriter, hmErr) = hyperMapperServer.start()

    val hyperMapperClient = new HyperMapperClient(hmReader, hmWriter, hmErr, hmConfig, costEval)

    import scala.concurrent._
    import ExecutionContext.Implicits.global

    val bestSolutionComputer = Future { hyperMapperClient.run() }

    val r = bestSolutionComputer map { sol =>
      hyperMapperServer.finish()
      // merge the processes by the solution mapping
      val processors = costEval.merged(sol)
      val stateUsers = Array.fill(parContext.numStateRegs()) { BitSet.empty }
      // initialize the state user
      for (x <- 0 until dimX; y <- 0 until dimY) {
        val processor = processors(x)(y)
        val coreIdx   = y + x * dimY
        for (inpIdx <- processor.inBound) {
          stateUsers(inpIdx) = stateUsers(inpIdx) | BitSet(coreIdx)
        }
      }
      def mkPid(x: Int, y: Int): ProcessId = ProcessIdImpl(s"p_${x}_${y}", x, y)
      def getPid(index: Int): ProcessId = {
        val x = index / dimY
        val y = index % dimY
        mkPid(x, y)
      }
      // for every core, create a DefProcess with Sends
      val finalProcesses = ctx.stats.recordRunTime("building final the final result") {
        for (x <- 0 until dimX; y <- 0 until dimY) yield {
          val coreIndex = y + x * dimY

          val core = processors(x)(y)
          val pid  = mkPid(x, y)
          val body = core.body.toSeq.map { parContext.getInstruction(_) }
          val sends = core.outSet.toSeq.flatMap { nextStateIdx =>
            stateUsers(nextStateIdx).toSeq.collect {
              case recipient if recipient != coreIndex =>
                val currentStateInDest = parContext.getInput(nextStateIdx)
                val nextStateHere      = parContext.getOutput(nextStateIdx)
                Send(currentStateInDest, nextStateHere, getPid(recipient))
            }
          }
          val bodyWithSends = body ++ sends
          val referenced    = NameDependence.referencedNames(bodyWithSends)
          val usedRegs = program.processes.head.registers.filter { r =>
            referenced(r.variable.name)
          }
          val usedLabelGrps = program.processes.head.labels.filter(lblgrp => referenced(lblgrp.memory))
          val usedGlobalMemories =
            if (bodyWithSends.exists { i => i.isInstanceOf[GlobalLoad] || i.isInstanceOf[GlobalStore] }) {
              program.processes.head.globalMemories
            } else {
              Nil
            }
          assert(program.processes.head.functions == Nil, "can not handle custom functions yet")

          DefProcess(
            id = pid,
            registers = usedRegs,
            functions = Nil,
            labels = usedLabelGrps,
            body = bodyWithSends,
            globalMemories = usedGlobalMemories
          )
        }
      }

      val result = program.copy(processes = finalProcesses.filter(_.body.nonEmpty))
      ctx.logger.dumpArtifact("connectivity.dot") { connectivityDotGraph(result) }
      ctx.stats.record(ProgramStatistics.mkProgramStats(result))
      result

    }

    Await.result(r, Duration.Inf)

  }

}
