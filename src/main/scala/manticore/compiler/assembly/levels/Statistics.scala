package manticore.compiler.assembly.levels

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.assembly.BinaryOperator
import scala.collection.immutable.ListMap
import manticore.compiler.assembly




case class ProcessStatistic(
    name: String,
    instructions: Seq[(String, Int)],
    length: Int,
    registers: Seq[(String, Int)]
)



trait ProgramStatCounter extends Flavored {

  import flavor._
  private def mkInstStats(
      proc: DefProcess
  )(implicit ctx: AssemblyContext): Seq[(String, Int)] = {

    var inst_count: ListMap[String, Int] = ListMap[String, Int](
      "CUST" -> 0,
      "LLD" -> 0,
      "LST" -> 0,
      "GLD" -> 0,
      "GST" -> 0,
      "SET" -> 0,
      "SEND" -> 0,
      "RECV" -> 0,
      "EXPECT" -> 0,
      "MUX" -> 0,
      "NOP" -> 0,
      "ADDCARRY" -> 0,
      "CLEARCARRY" -> 0,
      "SETCARRY" -> 0,
      "PADZERO" -> 0,
      "MOV" -> 0,
      "PREDICATE" -> 0
    ) ++ Seq
      .tabulate(BinaryOperator.maxId) { i => BinaryOperator(i) }
      .filter { k =>
        k != BinaryOperator.MUX && k != BinaryOperator.ADDC // do not double
      // count these two operators
      }
      .map { k => (k.toString() -> 0) }

    def incr(name: String): Unit = {
      inst_count = inst_count.updated(name, inst_count(name) + 1)
    }

    import BinaryOperator._
    proc.body.foreach {
      case _: CustomInstruction => incr("CUST")
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
          case _    => ctx.logger.error(s"invalid operator ${op}!")
        }
      case _: GlobalLoad  => incr("GLD")
      case _: GlobalStore => incr("GST")
      case _: LocalLoad   => incr("LLD")
      case _: LocalStore  => incr("LST")
      case _: Expect      => incr("EXPECT")
      case _: Predicate   => incr("PREDICATE")
      case _: AddC        => incr("ADDCARRY")
      case _: ClearCarry  => incr("CLEARCARRY")
      case _: SetCarry    => incr("SETCARRY")
      case _: Mov         => incr("MOV")
      case _: PadZero     => incr("PADZERO")
      case _: Mux         => incr("MUX")
      case _: Send        => incr("SEND")
      case _: Recv        => incr("RECV")
      case _: SetValue    => incr("SET")
      case Nop            => incr("NOP")
    }
    inst_count.toSeq
  }

  protected def mkRegStats(
      proc: DefProcess
  )(implicit ctx: AssemblyContext): Seq[(String, Int)] = {

    var counts = ListMap[VariableType, Int](
      ConstType -> 0,
      WireType -> 0,
      InputType -> 0,
      OutputType -> 0,
      MemoryType -> 0,
      CarryType -> 0,
      RegType -> 0
    )

    proc.registers.foreach { r =>
      counts =
        counts.updated(r.variable.varType, counts(r.variable.varType) + 1)
    }

    counts.map { case (k, v) => k.typeName.tail -> v }.toSeq
  }

  protected def mkProcessStats(
      proc: DefProcess
  )(implicit ctx: AssemblyContext): ProcessStatistic = {
    ProcessStatistic(
      name = proc.id.toString(),
      length = proc.body.length,
      instructions = mkInstStats(proc),
      registers = mkRegStats(proc)
    )
  }

  protected def mkProgramStats(
      prog: DefProgram
  )(implicit ctx: AssemblyContext): Seq[ProcessStatistic] =
    prog.processes.map(mkProcessStats)

}

trait StatisticCollector {

  /**
    * Helper function used to run a timed sequence of code
    *
    * @param action
    * @return
    */
  def timed[R](action: => R): (R, Double) = {
    val start_time = System.nanoTime()
    val result = action
    val end_time = System.nanoTime()
    val elapsed_ms = (end_time - start_time) * 1e-6
    (result, elapsed_ms)
  }

  /**
    * Create a dynamic scope for stat collection. Used by the transformation
    * class which sets the scope so that passes implemented using AssemblyTransformer
    * can correctly record statistics
    * @param func
    * @param id
    * @return
    */

  def scope[R](transformationClosure: => R)(implicit id: TransformationID): (R, Double)

  /**
    * Record the runtime of a a labeled event. This is useful if you wish to
    * include some custom runtime information in the statistics
    *
    * @param label name of the action
    * @param milliseconds runtime in milliseconds, you can use the [[timed]]
    * method to get the time
    */
  def recordRunTime(label: String, milliseconds: Double): Unit

  /**
    * Run an action and record its runtime
    *
    * @param label name of the action
    * @param action
    * @return
    */
  def recordRunTime[R](label: String)(action: => R): R = {
    val (r, t) = timed(action)
    recordRunTime(label, t)
    r
  }

  /**
    * Record program statistics
    *
    * @param prog
    */
  def record(prog: Seq[ProcessStatistic]): Unit

  /**
    * Record a key-value pair. The accepted type of value depends on the
    * implementation of this trait
    * @param name
    * @param value
    */
  def record(name: String, value: Any): Unit
  def record(nv: (String, Any)): Unit = record(nv._1, nv._2)

  /**
    * Record a key value pair conditionally
    *
    * @param cond
    * @param name
    * @param value
    */
  def recordWhen(cond: Boolean)(name: String, value: => Any): Unit =
    if (cond) record(name, value)

  /**
    * Serialize the statistics as a YAML
    *
    * @return
    */

  def asYaml: String

}

object StatisticCollector {

  def apply(): StatisticCollector = new StatisticCollectorImpl()

  /**
    * Basic implementation for a statistic collector
    *
    */
  class StatisticCollectorImpl extends StatisticCollector {

    case class UserDict(
        runtime: Seq[(String, Double)] = Nil,
        pairs: Seq[(String, Any)] = Nil
    ) {
      def nonEmpty: Boolean = runtime.nonEmpty || pairs.nonEmpty
      def toYaml(indent: Int): String = {
        val str = new StringBuilder
        val tabs: String = "\t" * indent
        str ++= s"${tabs}user:\n"
        if (runtime.nonEmpty) {
          str ++= s"${tabs}\truntime:\n"
          runtime.foreach { case (k, v) =>
            str ++= f"${tabs}\t\t- ${k}: ${v}%.3f\n"
          }
        }
        if (pairs.nonEmpty) {
          str ++= s"${tabs}\tdata: \n"
          runtime.foreach { case (k, v) =>
            str ++= s"${tabs}\t\t- ${k}: ${v}\n"
          }
        }
        str.toString()
      }
    }
    case class TransformationStatBuilder(
        id: String,
        runtime: Double,
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
    private var currentTrans = TransformationStatBuilder(id = "<INV>", runtime = 0.0)

    override def recordRunTime(
        label: String,
        milliseconds: Double
    ): Unit = {
      currentTrans = currentTrans.copy(
        user = currentTrans.user.copy(runtime =
          currentTrans.user.runtime :+ (label -> milliseconds)
        )
      )
    }

    override def record(prog: Seq[ProcessStatistic]): Unit = {
      val slowest = prog.maxBy { p => p.length }
      currentTrans = currentTrans.copy(
        nProcesses = Some(prog.length),
        program = prog,
        vcycle = Some(slowest.length),
        slowest = Some(slowest.name)
      )
    }

    override def record(name: String, value: Any): Unit = value match {
      case (_: Double | _: Int | _: String) =>
        currentTrans.copy(
          user = currentTrans.user.copy(pairs =
            currentTrans.user.pairs :+ (name -> value)
          )
        )
      case _ => throw new RuntimeException("Can not handle value type!")
    }

    override def scope[R](
        func: => R
    )(implicit id: TransformationID): (R, Double) = {
      currentTrans = TransformationStatBuilder(id = id.toString(), runtime = 0)
      val (res, runtime) = timed(func)
      transStatBuilder += currentTrans.copy(runtime = runtime)
      (res, runtime)
    }

    override def asYaml: String = {
      val str = new StringBuilder

      def tabs(n: Int)(s: => String): Unit = str ++= (("\t" * n) + s + "\n")
      tabs(0) { s"transformations: " }

      transStatBuilder.foreach { stat =>

        tabs(1) {
          s"- ${stat.id}:"
        }
        tabs(2) {
          f"runtime: ${stat.runtime}%.3f"
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
