package manticore.compiler.assembly.levels.placed.interpreter

import manticore.compiler.assembly.levels.placed.PlacedIR._
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.AssemblyChecker
import manticore.compiler.assembly.levels.TransformationID
import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.assembly.levels.ValueChangeWriterBase
import manticore.compiler.assembly.levels.InterpreterMonitor
import manticore.compiler.assembly.levels.InterpreterMonitorCompanion

trait InterpreterBase {
  implicit val phase_id: TransformationID
  implicit val ctx: AssemblyContext
}

/// base trait for a message between the machine interpreter and a process interpreter
trait MessageBase {
  val source_id: ProcessId
  val target_id: ProcessId
  val value: UInt16
  val target_register: Name
}

sealed abstract class InterpretationTrap
case object FailureTrap extends InterpretationTrap
case object StopTrap extends InterpretationTrap
case object InternalTrap extends InterpretationTrap

trait PlacedValueChangeWriter extends ValueChangeWriterBase {
  val flavor = PlacedIR
  def toBigInt(v: UInt16): BigInt = BigInt(v.toInt)
}

object PlacedValueChangeWriter {

  final class VcdDumpImpl(val program: DefProgram, val file_name: String)(
      implicit val ctx: AssemblyContext
  ) extends PlacedValueChangeWriter {}

  def apply(prog: DefProgram, file_name: String)(implicit
      ctx: AssemblyContext
  ): PlacedValueChangeWriter = new VcdDumpImpl(prog, file_name)

}

final class PlacedIRInterpreterMonitor private (
    protected final val watchList: Map[
      String,
      InterpreterMonitor.ConditionalWatch
    ],
    protected final val records: Map[String, InterpreterMonitor.MonitorRecord],
    protected final val debInfo: Map[
      PlacedIR.Name,
      InterpreterMonitor.DebugInfo
    ]
) extends InterpreterMonitor {
  final val flavor = PlacedIR
  final def toBigInt(v: PlacedIR.Constant): BigInt = BigInt(v.toInt)
}

object PlacedIRInterpreterMonitor extends InterpreterMonitorCompanion {
  val flavor = PlacedIR
  import flavor._

  def toBigInt(v: PlacedIR.Constant): BigInt = BigInt(v.toInt)

  def apply(program: DefProgram)(implicit
      ctx: AssemblyContext
  ): PlacedIRInterpreterMonitor =
    apply(program, Seq(InterpreterMonitor.WatchAll))

  def apply(
      program: DefProgram,
      toWatch: Seq[InterpreterMonitor.InterpreterWatch]
  )(implicit
      ctx: AssemblyContext
  ): PlacedIRInterpreterMonitor = {
    val BuildIngredients(watchList, records, debInfo) =
      collectIngredients(program, toWatch)
    new PlacedIRInterpreterMonitor(
      watchList,
      records,
      debInfo
    )
  }

}

