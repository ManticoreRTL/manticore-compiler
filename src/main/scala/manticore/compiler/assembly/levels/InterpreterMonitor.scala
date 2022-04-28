package manticore.compiler.assembly.levels

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.annotations.DebugSymbol
import java.io.File
import java.io.PrintWriter
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.assembly.ManticoreAssemblyIR
import scala.collection.immutable.ListMap
import manticore.compiler.CompilationFailureException
import scala.collection.immutable.Seq
import scala.annotation.implicitNotFound

/** @author
  *   Mahyar Emami <mahyar.emami@eplf.ch>
  */


/** The base of any interpreter monitor
  */
trait InterpreterMonitor extends Flavored {

  import flavor._
  protected val watchList: Map[String, InterpreterMonitor.ConditionalWatch]
  protected val records: Map[String, InterpreterMonitor.MonitorRecord]
  protected val debInfo: Map[Name, InterpreterMonitor.DebugInfo]

  def toBigInt(v: Constant): BigInt

  /** Update a the [[Name]] (not the debug symbol) with the given value. This
    * method should solely used by interpreter implementations that extend
    * [[CanUpdateMonitor]] If you try
    * @param name
    * @param value
    * @param updater
    */
  def update(name: Name, value: Constant)(implicit
      @implicitNotFound(
        "Only interpreters mixing with CanUpdateMonitor[${A}] can call update, avoid using update outside an interpreter implementation!"
      ) updater: InterpreterMonitor.MonitorUpdater
  ): Unit = {
    debInfo.get(name) match {
      case Some(InterpreterMonitor.DebugInfo(sym, offset, len)) =>
        val symbol: String = sym.getSymbol()
        val newVal = records(symbol).update(toBigInt(value), offset, len)
        watchList.get(symbol) match {
          case Some(
                InterpreterMonitor.ConditionalWatch(_, condition, callback)
              ) =>
            if (condition(newVal)) {
              callback(newVal, offset, len)
            }
          case None => // do nothing
        }
      case None =>
      // name not being does not have a debug symbol
    }
  }

  /** Read the current value of a debug symbol
    *
    * @param debSym
    * @return
    */
  def read(debSym: String): BigInt = records(debSym).value()

  /** Get the keys, i.e., all the debugs symbols being monitored
    *
    * @return
    */
  def monitoredSymbols = records.keys
  def keys = monitoredSymbols
}

object InterpreterMonitor {

  /** Base functional trait for a callback A callback is provided with the
    * [[finalValue]] of the debug symbol after the update is done in the monitor
    * along with the subword [[offset]] and [[length]]. To create call you can
    * simply use a lambda, this trait is here mostly for documentation to tell
    * you what the callback lambda should be and what are its arguments
    */
  trait MonitorCallBack extends ((BigInt, Int, Int) => Unit) {
    def apply(finalValue: BigInt, offset: Int, length: Int): Unit
  }

  // the super type all watchers
  sealed trait InterpreterWatch

  /** A conditional watch that calls back [[callback]]
    *
    * @param debugSym
    * @param condition
    * @param callback
    */
  case class ConditionalWatch(
      debugSym: String,
      condition: BigInt => Boolean,
      callback: MonitorCallBack
  ) extends InterpreterWatch
  case class WatchSymbol(debugSym: String) extends InterpreterWatch
  case object WatchAll extends InterpreterWatch

  // A helper class for [[InterpreterMonitor]] builder
  case class DebugInfo(sym: DebugSymbol, offset: Int, len: Int)

  /** A single monitor record, used by the [[InterpreterMonitor]] and potential
    * builder of an [[InterpreterMonitor]]
    *
    * @param totalWidth
    *   the width of a debug symbol
    */
  final class MonitorRecord(
      val totalWidth: Int
  ) {

    private var bigValue: BigInt = BigInt(0)
    private val mask: BigInt = (BigInt(1) << totalWidth) - BigInt(1)

    def update(value: BigInt, offset: Int, len: Int): BigInt = {
      val writeMask = (BigInt(1) << len) - BigInt(1)
      val zeroingMask = mask - (writeMask << offset)
      val zeroed = bigValue & zeroingMask
      val updateVal = (value & writeMask) << offset
      bigValue = zeroed | updateVal
      bigValue
    }

    def value(): BigInt = bigValue

  }

  /** Exceptions thrown by the monitor
    *
    * @param msg
    */
  case class MonitorError(msg: String) extends Exception(msg)

  // any class that takes a monitor and uses the update method needs to mix
  // with CanUpdateMonitor. This is required to provide access to the update
  // method of the monitor which is not accessible by the readers

  private[InterpreterMonitor] trait MonitorUpdater
  trait CanUpdateMonitor[T] { this: T =>
    protected implicit object ThisMonitorUpdater extends MonitorUpdater
  }
}

/** This trait solely exists to remove duplicate code required to write
  * companion objects for [[InterpreterMonitor]] implementation
  */
trait InterpreterMonitorCompanion extends Flavored {

  import flavor._

  def toBigInt(v: Constant): BigInt
  protected case class BuildIngredients(
      watchList: Map[String, InterpreterMonitor.ConditionalWatch],
      records: Map[String, InterpreterMonitor.MonitorRecord],
      debInfo: Map[Name, InterpreterMonitor.DebugInfo]
  )

  /** Create an interpreter monitor that only watches the what is defined in
    * [[toWatch]], can be used to register callbacks.
    *
    * @param program
    * @param toWatch
    * @param ctx
    * @return
    */
  protected def collectIngredients(
      program: DefProgram,
      toWatch: Seq[InterpreterMonitor.InterpreterWatch]
  )(implicit
      ctx: AssemblyContext
  ): BuildIngredients = {

    val symGroups = program.processes
      .flatMap { _.registers }
      .map { r =>
        r.annons.collectFirst { case x: DebugSymbol => x } -> r
      }
      .collect { case (Some(x: DebugSymbol), r) => x -> r }
      .groupBy(_._1.getSymbol())

    val debInfo = scala.collection.mutable.Map.empty[Name, InterpreterMonitor.DebugInfo]
    val records = scala.collection.mutable.Map
      .empty[String, InterpreterMonitor.MonitorRecord]

    def append(debsym: String, regs: Seq[(DebugSymbol, DefReg)]): Unit = {
      val indexSortedRegs = regs
        .sortBy { case (dbg, r) => dbg.getIndex().getOrElse(0) }
      val (firstDbg, firstReg) = indexSortedRegs.head
      val totalWidth = indexSortedRegs.map(_._2.variable.width).sum
      assert(
        totalWidth == firstDbg.getWidth().getOrElse(totalWidth),
        "Invalid debug symbol?"
      )
      val rec = new InterpreterMonitor.MonitorRecord(totalWidth)
      records += (debsym -> rec)
      indexSortedRegs.foldLeft(0) { case (offset, (dbg, r)) =>
        debInfo += (r.variable.name -> InterpreterMonitor.DebugInfo(
          dbg,
          offset,
          r.variable.width
        ))
        // set the initial value
        rec.update(r.value.map(toBigInt).getOrElse(0), offset, r.variable.width)
        offset + r.variable.width
      }

    }
    if (toWatch.contains(InterpreterMonitor.WatchAll)) {
      symGroups.foreach { case (debsym, regs) => append(debsym, regs) }
    } else {
      // add the symbols to the monitor debInfo and records
      toWatch
        .collect { case InterpreterMonitor.WatchSymbol(sym) => sym }
        .foreach { sym =>
          symGroups.get(sym) match {
            case Some(regs) =>
              append(sym, regs)
            case None =>
              throw new InterpreterMonitor.MonitorError(
                s"Could not look up symbol ${sym}"
              )
          }
        }
    }
    val watchList = toWatch.collect {
      case w: InterpreterMonitor.ConditionalWatch =>
        w.debugSym -> w
    }.toMap
    watchList.foreach { case (sym, _) =>
      symGroups.get(sym) match {
        case Some(regs) =>
          append(sym, regs)
        case None =>
          throw new InterpreterMonitor.MonitorError(
            s"Could not look up symbol ${sym}"
          )
      }
    }

    BuildIngredients(
      watchList,
      records.toMap,
      debInfo.toMap
    )
  }

}
