package manticore.compiler.assembly.levels.unconstrained

import manticore.compiler.assembly.levels.InterpreterMonitor
import manticore.compiler.assembly.levels.InterpreterMonitor.ConditionalWatch
import manticore.compiler.assembly.levels.InterpreterMonitor.InterpreterWatch
import manticore.compiler.assembly.levels.InterpreterMonitor.WatchAll
import manticore.compiler.assembly.levels.InterpreterMonitor.WatchSymbol
import manticore.compiler.assembly.levels.InterpreterMonitor.MonitorRecord
import manticore.compiler.assembly.levels.InterpreterMonitor.DebugInfo
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.annotations.DebugSymbol
import manticore.compiler.assembly.levels.InterpreterMonitorCompanion
import manticore.compiler.assembly.ManticoreAssemblyIR

/** An UnconstrainedIR interpreter Monitor capable of monitoring the values of
  * registers under the the umbrella of a debug symbol, see the companion object
  * for instantiation
  *
  * @param watchList
  * @param records
  * @param debInfo
  */
final class UnconstrainedIRInterpreterMonitor private (
    protected final val watchList: Map[String, ConditionalWatch],
    protected final val records: Map[String, MonitorRecord],
    protected final val debInfo: Map[UnconstrainedIR.Name, DebugInfo]
) extends InterpreterMonitor {
  final val flavor = UnconstrainedIR
  final def toBigInt(v: BigInt): BigInt = v
}

/** UnconstrainedIRInterpreterMonitor companion object
  */
object UnconstrainedIRInterpreterMonitor extends InterpreterMonitorCompanion {
  val flavor = UnconstrainedIR
  import flavor._

  def toBigInt(v: BigInt): BigInt = v

  /** Create an interpreter monitor that watches all the debug symbols
    *
    * @param program
    * @param ctx
    * @return
    */


  def apply(program: DefProgram)(implicit
      ctx: AssemblyContext
  ): UnconstrainedIRInterpreterMonitor = apply(program, Seq(WatchAll))

  /** Create an interpreter monitor that only watches the what is defined in
    * toWatch, can be used to register callbacks.
    *
    * @param program
    * @param toWatch
    * @param ctx
    * @return
    */
  def apply(program: DefProgram, toWatch: Seq[InterpreterWatch])(implicit
      ctx: AssemblyContext
  ): UnconstrainedIRInterpreterMonitor = {
    val BuildIngredients(watchList, records, debInfo) = collectIngredients(program, toWatch)
    new UnconstrainedIRInterpreterMonitor(
      watchList,
      records,
      debInfo
    )
  }

}
