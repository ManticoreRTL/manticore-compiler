package manticore.compiler.assembly.levels.placed.lowering

import manticore.compiler.assembly.levels.placed.PlacedIRTransformer
import manticore.compiler.assembly.levels.placed.PlacedIRChecker
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.placed.PlacedIR
import scala.annotation.tailrec

object UtilizationChecker extends PlacedIRChecker {
  import PlacedIR._
  override def check(program: DefProgram)(implicit context: AssemblyContext): Unit = {

    val utils = program.processes.map(p => p.id -> computeUtil(p))

    val straggler = utils.maxBy { _._2.cycle }

    context.logger.info(s"${straggler._1} is the straggler:\n${straggler._2}")

    val leastScheduleUtil = utils.maxBy { case (pid, util) =>
      util.nops - util.waitCycles
    }

    context.logger.info(s"${leastScheduleUtil._1} has the worst schedule utilization:\n${leastScheduleUtil._2}")

    val maxNocWait = utils.maxBy { case (pid, util) => util.waitCycles }

    context.logger.info(s"${maxNocWait._1} spends most of its time on messages:\n${maxNocWait._2}")

    val maxRecvs = utils.maxBy { _._2.numRecv }

    context.logger.info(s"${maxRecvs._1} receives the most messages:\n${maxRecvs._2}")

    // print heavy sender
    program.processes
      .map { p =>
        p.id -> p.body.collect { case send: Send => send }
      }
      .filter { case (pid, sends) => sends.nonEmpty }
      .map { case (pid, sends) => pid -> sends.groupBy(_.dest_id).maxBy(_._2.length) }
      .sorted {
        Ordering.by[(ProcessId, (ProcessId, Seq[Send])), Int] { case (pid, (tagetId, sends)) => sends.length }.reverse
      }
      .take(5)
      .foreach { case (sourceId, (targetId, sends)) =>
        context.logger.info(s"$sourceId sends $targetId ${sends.length} packets")

      }

    // print high fan-out
    program.processes
      .flatMap { p => p.body.collect { case send: Send => p.id -> send } }
      .groupBy { _._2.rs }
      .toSeq
      .sorted {
        Ordering.by[(Name, Seq[(ProcessId, Send)]), Int] { case (_, sends) => sends.length }.reverse
      }
      .take(10)
      .foreach { case (rs, sends) =>
        val sourceId = sends.head._1

        context.logger.info(s"High fan-out register $rs from $sourceId to:\n${sends.map(_._2.dest_id).mkString("\n")}")

      }

    // x.sorted(Ordering.by { case (pid: ProcessId, (targetPid: ProcessId, sends: Seq[Send])) => sends.length }.reverse)
  }

  case class UtilBuilder(lastRealInstr: Int = 0, cycle: Int = 0, numRecv: Int = 0, nops: Int = 0, waitCycles: Int = 0) {
    def executes(n: Int = 1) = this.copy(lastRealInstr = cycle, cycle = cycle + n)
    def receives()           = this.copy(cycle = cycle + 1, numRecv = numRecv + 1)
    def idles()              = this.copy(cycle = cycle + 1, nops = nops + 1)
    def waits()              = this.copy(waitCycles = waitCycles + 1)

    override def toString = s"""|  lastRealInstr: ${lastRealInstr}
                                |  cycle: ${cycle}
                                |  numRecv: ${numRecv}
                                |  nops: ${nops}
                                |  waitNops: ${waitCycles}
                                |""".stripMargin
  }
  private def computeUtil(process: DefProcess)(implicit ctx: AssemblyContext): UtilBuilder = {

    @tailrec
    def advanceCycle(instructionBlock: Seq[Instruction], builder: UtilBuilder): UtilBuilder = instructionBlock match {
      case (jtb @ JumpTable(_, _, blocks, dslot, _)) +: tail =>
        ctx.logger.fail("Can not have jump tables yet!")
        val newBuilder = builder.executes(dslot.length + blocks.map(_.block.length).max + 1)
        advanceCycle(tail, newBuilder)
      // advanceCycle(tail, )
      case (_: Recv) +: tail =>
        advanceCycle(tail, builder.receives())
      case Nop +: tail =>
        advanceCycle(tail, builder.idles())
      case instr +: tail =>
        advanceCycle(tail, builder.executes())
      case Nil => builder
    }

    @tailrec
    def reverseCycle(reverseBlock: Seq[Instruction], builder: UtilBuilder): UtilBuilder = reverseBlock match {
      case (_: Recv | Nop) +: tail => reverseCycle(tail, builder.waits())
      case instr +: tail           => builder

    }
    val builder = advanceCycle(process.body, UtilBuilder(0, 0, 0))
    reverseCycle(process.body.reverse, builder)

  }

}
