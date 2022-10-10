package manticore.compiler.assembly.levels

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.assembly.BinaryOperator
import scala.collection.immutable.ListMap
import manticore.compiler.assembly
import manticore.compiler.assembly.StopInterrupt
import manticore.compiler.assembly.FinishInterrupt
import manticore.compiler.assembly.AssertionInterrupt
import manticore.compiler.assembly.SerialInterrupt
import manticore.compiler.TransformationID

case class ProcessStatistic(
    name: String,
    instructions: Seq[(String, Int)],
    length: Int,
    registers: Seq[(String, Int)],
    functs: Int,
    vcycle: Int = 0
)

trait CanCollectProgramStatistics extends Flavored {

  object ProgramStatistics {
    import flavor._

    case class InstructionStat(
        count: Seq[(String, Int)],
        vcycle: Int
    )
    import flavor._
    def mkInstStats(
        proc: DefProcess
    )(implicit ctx: AssemblyContext): InstructionStat = {

      var inst_count: ListMap[String, Int] = ListMap[String, Int](
        "CUST"       -> 0,
        "LLD"        -> 0,
        "LST"        -> 0,
        "GLD"        -> 0,
        "GST"        -> 0,
        "SET"        -> 0,
        "SEND"       -> 0,
        "RECV"       -> 0,
        "MUX"        -> 0,
        "NOP"        -> 0,
        "ADDCARRY"   -> 0,
        "CLEARCARRY" -> 0,
        "SETCARRY"   -> 0,
        "PADZERO"    -> 0,
        "MOV"        -> 0,
        "PREDICATE"  -> 0,
        "PARMUX"     -> 0,
        "LOOKUP"     -> 0,
        "SWITCH"     -> 0,
        "SLICE"      -> 0,
        "BREAK"      -> 0,
        "PUT"        -> 0,
        "FINISH"     -> 0,
        "FLUSH"      -> 0,
        "STOP"       -> 0,
        "ASSERT"     -> 0
      ) ++ Seq
        .tabulate(BinaryOperator.maxId) { i => BinaryOperator(i) }
        .filter { k =>
          k != BinaryOperator.MUX && k != BinaryOperator.ADDC // do not double
        // count these two operators
        }
        .map { k => (k.toString() -> 0) }

      def incr(name: String): Int = {
        inst_count = inst_count.updated(name, inst_count(name) + 1)
        1
      }
      var vcycle = 0

      import BinaryOperator._
      def count(inst: Instruction): Int = inst match {
        case _: CustomInstruction => incr("CUST")
        case _: ConfigCfu => incr("CONFIG_CFU")
        case BinaryArithmetic(op, _, _, _, _) =>
          op match {
            case ADD  => incr("ADD")
            case SUB  => incr("SUB")
            case MUL  => incr("MUL")
            case AND  => incr("AND")
            case OR   => incr("OR")
            case XOR  => incr("XOR")
            case SLL  => incr("SLL")
            case SRL  => incr("SRL")
            case SRA  => incr("SRA")
            case SEQ  => incr("SEQ")
            case SLTS => incr("SLTS")
            case SLT  => incr("SLT")
            case MULH => incr("MULH")
            case MULS => incr("MULHS")
            case _ =>
              ctx.logger.error(s"invalid operator ${op}!")
              0
          }
        case _: GlobalLoad  => incr("GLD")
        case _: GlobalStore => incr("GST")
        case _: LocalLoad   => incr("LLD")
        case _: LocalStore  => incr("LST")
        case _: Predicate   => incr("PREDICATE")
        case _: AddCarry    => incr("ADDCARRY")
        case _: ClearCarry  => incr("CLEARCARRY")
        case _: SetCarry    => incr("SETCARRY")
        case _: Mov         => incr("MOV")
        case _: PadZero     => incr("PADZERO")
        case _: Mux         => incr("MUX")
        case _: Send        => incr("SEND")
        case _: Recv        => incr("RECV")
        case _: SetValue    => incr("SET")
        case _: ParMux      => incr("PARMUX")
        case _: Lookup      => incr("LOOKUP")
        case _: Slice       => incr("SLICE")
        case Nop            => incr("NOP")
        case _: BreakCase   => incr("BREAK")
        case JumpTable(_, _, blocks, dslot, _) =>
          var numInsts = 0
          // numInsts += incr("SWITCH")
          val blockInsts = blocks.map { case JumpCase(_, blk) =>
            blk.map { count }.sum
          }
          numInsts += blockInsts.sum
          vcycle += 1 + blockInsts.max
          val dslotInsts = dslot.foldLeft(0) { case (acc, inst) =>
            acc + count(inst)
          }
          vcycle += dslotInsts

          numInsts + dslotInsts + incr("SWITCH")
        case Interrupt(action, _, _, _) =>
          action.action match {
            case AssertionInterrupt   => incr("ASSERT")
            case FinishInterrupt      => incr("FINISH")
            case SerialInterrupt(fmt) => incr("FLUSH")
            case StopInterrupt        => incr("STOP")
          }
        case _: PutSerial => incr("PUT")
      }

      proc.body.foreach { count }
      vcycle += proc.body.length + inst_count("RECV")

      InstructionStat(inst_count.toSeq, vcycle)
    }

    def mkRegStats(
        proc: DefProcess
    )(implicit ctx: AssemblyContext): Seq[(String, Int)] = {

      var counts = ListMap[VariableType, Int](
        ConstType  -> 0,
        WireType   -> 0,
        InputType  -> 0,
        OutputType -> 0,
        MemoryType -> 0,
        RegType    -> 0
      )

      proc.registers.foreach { r =>
        counts = counts.updated(r.variable.varType, counts(r.variable.varType) + 1)
      }

      counts.map { case (k, v) => k.typeName.tail -> v }.toSeq
    }

    def mkProcessStats(
        proc: DefProcess
    )(implicit ctx: AssemblyContext): ProcessStatistic = {
      val instStat = mkInstStats(proc)
      ProcessStatistic(
        name = proc.id.toString(),
        length = proc.body.length,
        instructions = instStat.count,
        registers = mkRegStats(proc),
        functs = proc.functions.size,
        vcycle = instStat.vcycle
      )
    }

    def mkProgramStats(
        prog: DefProgram
    )(implicit ctx: AssemblyContext): Seq[ProcessStatistic] =
      prog.processes.map(mkProcessStats)
  }
}

trait StatisticCollector {

  /** Helper function used to run a timed sequence of code
    *
    * @param action
    * @return
    */
  def timed[R](action: => R): (R, Double) = {
    val start_time = System.nanoTime()
    val result     = action
    val end_time   = System.nanoTime()
    val elapsed_ms = (end_time - start_time) * 1e-6
    (result, elapsed_ms)
  }

  /** Create a dynamic scope for stat collection. Used by the transformation
    * class which sets the scope so that passes implemented using
    * AssemblyTransformer can correctly record statistics
    * @param func
    * @param id
    * @return
    */

  def scope[R](transformationClosure: => R)(implicit
      id: TransformationID
  ): (R, Double)

  /** Record the runtime of a a labeled event. This is useful if you wish to
    * include some custom runtime information in the statistics
    *
    * @param label
    *   name of the action
    * @param milliseconds
    *   runtime in milliseconds, you can use the [[timed]] method to get the
    *   time
    */
  def recordRunTime(label: String, milliseconds: Double): Unit

  /** Run an action and record its runtime
    *
    * @param label
    *   name of the action
    * @param action
    * @return
    */
  def recordRunTime[R](label: String)(action: => R): R = {
    val (r, t) = timed(action)
    recordRunTime(label, t)
    r
  }

  /** Record program statistics
    *
    * @param prog
    */
  def record(prog: Seq[ProcessStatistic]): Unit

  /** Record a key-value pair. The accepted type of value depends on the
    * implementation of this trait
    * @param name
    * @param value
    */
  def record(name: String, value: Any): Unit
  def record(nv: (String, Any)): Unit = record(nv._1, nv._2)

  /** Record a key value pair conditionally
    *
    * @param cond
    * @param name
    * @param value
    */
  def recordWhen(cond: Boolean)(name: String, value: => Any): Unit =
    if (cond) record(name, value)

  /** Serialize the statistics as a YAML
    *
    * @return
    */

  def asYaml: String

}

object StatisticCollector {

  def apply(): StatisticCollector = new StatisticCollectorImpl()

  /** Basic implementation for a statistic collector
    */
  class StatisticCollectorImpl extends StatisticCollector {

    def prettyPrintRuntime(runtimeMs: Double): String = {
      val numMsInSec = 1e3
      val numMsInMin = 60 * numMsInSec
      val (runtime, units) = runtimeMs match {
        case t if t > numMsInMin => (t / numMsInMin, "m")
        case t if t > numMsInSec => (t / numMsInSec, "s")
        case t                   => (t, "ms")
      }
      f"${runtime}%.3f ${units}"
    }

    case class UserDict(
        runtime: Seq[(String, Double)] = Nil,
        pairs: Seq[(String, Any)] = Nil
    ) {
      def nonEmpty: Boolean = runtime.nonEmpty || pairs.nonEmpty
      def toYaml(indent: Int): String = {
        val str          = new StringBuilder
        val tab: String  = "    "
        val tabs: String = tab * indent
        str ++= s"${tabs}user:\n"
        if (runtime.nonEmpty) {
          str ++= s"${tabs}${tab}runtime:\n"
          runtime.foreach { case (k, v) =>
            str ++= s"${tabs}${tab}${tab}- ${k}: ${prettyPrintRuntime(v)}\n"
          }
        }
        if (pairs.nonEmpty) {
          str ++= s"${tabs}${tab}data: \n"
          pairs.foreach { case (k, v) =>
            str ++= s"${tabs}${tab}${tab}- ${k}: ${v}\n"
          }
        }
        str.toString()
      }
    }
    case class TransformationStatBuilder(
        id: String,
        runtimeMs: Double,
        user: UserDict = UserDict(),
        nProcesses: Option[Int] = None,
        vcycle: Option[Int] = None,
        program: Seq[ProcessStatistic] = Nil,
        slowest: Option[String] = None
    ) {
      def nonEmpty: Boolean =
        user.nonEmpty ||
          nProcesses.nonEmpty || vcycle.nonEmpty ||
          program.nonEmpty || slowest.nonEmpty
    }

    private val transStatBuilder =
      scala.collection.mutable.ArrayBuffer.empty[TransformationStatBuilder]
    private var currentTrans =
      TransformationStatBuilder(id = "<INV>", runtimeMs = 0.0)
    private var open = false
    override def recordRunTime(
        label: String,
        milliseconds: Double
    ): Unit = {
      currentTrans = currentTrans.copy(
        user = currentTrans.user.copy(runtime = currentTrans.user.runtime :+ (label -> milliseconds))
      )
    }

    override def record(prog: Seq[ProcessStatistic]): Unit = {
      val slowest = prog.maxBy { p => p.vcycle }
      currentTrans = currentTrans.copy(
        nProcesses = Some(prog.length),
        program = prog,
        vcycle = Some(slowest.vcycle),
        slowest = Some(slowest.name)
      )
    }

    override def record(name: String, value: Any): Unit = value match {
      case (_: Double | _: Int | _: String) =>
        currentTrans = currentTrans.copy(
          user = currentTrans.user.copy(pairs = currentTrans.user.pairs :+ (name -> value))
        )
      case _ => throw new RuntimeException("Can not handle value type!")
    }

    override def scope[R](
        func: => R
    )(implicit id: TransformationID): (R, Double) = {
      require(!open, "Can not have nested scopes yet!")
      currentTrans = TransformationStatBuilder(id = id.toString(), runtimeMs = 0)
      open = true
      val (res, runtimeMs) = timed(func)
      transStatBuilder += currentTrans.copy(runtimeMs = runtimeMs)
      open = false
      (res, runtimeMs)
    }

    override def asYaml: String = {
      val str = new StringBuilder

      def tabs(n: Int)(s: => String): Unit = str ++= (("    " * n) + s + "\n")
      tabs(0) { s"transformations: " }

      transStatBuilder.foreach { stat =>
        tabs(1) {
          s"- ${stat.id}:"
        }
        tabs(2) {
          s"runtime: ${prettyPrintRuntime(stat.runtimeMs)}"
        }
        if (stat.user.nonEmpty) {
          str ++= stat.user.toYaml(2)
        }
        if (stat.nProcesses.nonEmpty) {
          tabs(2) { s"num-processes: ${stat.nProcesses.get}" }
        }
        if (stat.vcycle.nonEmpty) {
          tabs(2) { s"vcycle: ${stat.vcycle.get}" }
        }
        if (stat.slowest.nonEmpty) {
          tabs(2) { s"slowest: ${stat.slowest.get}" }
        }
        if (stat.program.nonEmpty) {
          tabs(2) { s"program:" }
          stat.program.foreach { p =>
            tabs(3) { s"- ${p.name}: " }
            tabs(4) { s"length: ${p.length}" }
            tabs(4) { s"vcycle: ${p.vcycle}" }
            tabs(4) { s"functs: ${p.functs}" }
            tabs(4) { s"instructions:" }
            p.instructions.foreach { case (k, v) => tabs(5) { s"${k}: ${v} " } }
            tabs(4) { s"registers:" }
            p.registers.foreach { case (k, v) => tabs(5) { s"${k}: ${v}" } }
          }
        }

      }
      str.toString()
    }
  }

}
