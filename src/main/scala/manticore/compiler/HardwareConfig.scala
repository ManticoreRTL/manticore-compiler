package manticore.compiler

import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.assembly.levels.placed.PlacedIR

sealed trait HardwareConfig {
  import PlacedIR._

  // number of machine registers
  val nRegisters: Int
  // instruction memory capacity in instructions
  val nInstructions: Int
  // scratch pad capacity in 16-bit words
  val nScratchPad: Int

  // number of custom functions
  val nCustomFunctions: Int
  // number of inputs to the custom function unit
  val nCfuInputs: Int

  // number of carry register
  val nCarries: Int

  val dimX: Int
  val dimY: Int

  val maxLatency: Int

  protected val nHops: Int = 1

  def latency(inst: Instruction): Int = inst match {
    case _: Predicate => 0
    // sink instructions can only have anti-dependencies which are independent of
    // the time it takes to commit them. For an anti dependence LST -> LLD only
    // mandates placing LST first, but no Nop is needed in between them.
    case _ @(_: Interrupt | _: PutSerial | _: GlobalStore | _: LocalStore) => 0
    case Nop                                   => 0
    case JumpTable(_, _, blocks, delaySlot, _) =>
      // this is a conservative estimation
      val delaySlotLatency = 1 + delaySlot.length
      delaySlotLatency + blocks.map { case JumpCase(_, blk) =>
        blk.length + maxLatency + 1
      }.max

    case _ => maxLatency
  }

  def yHops(source: ProcessId, target: ProcessId): Int =
    (if (source.y > target.y) { dimY - source.y + target.y }
     else { target.y - source.y }) * nHops

  def xHops(source: ProcessId, target: ProcessId): Int =
    (if (source.x > target.x) dimX - source.x + target.x
     else target.x - source.x) * nHops

  def xyHops(source: ProcessId, target: ProcessId): (Int, Int) = {
    (xHops(source, target), yHops(source, target))
  }

  def manhattan(source: ProcessId, target: ProcessId): Int = {
    val (xDist, yDist) = xyHops(source, target)
    xDist + yDist
  }

}

case class DefaultHardwareConfig(
    dimX: Int,
    dimY: Int,
    maxLatency: Int = 10,
    nRegisters: Int = 2048,
    nCarries: Int = 64,
    nScratchPad: Int = (1 << 14),
    nInstructions: Int = 4096,
    nCustomFunctions: Int = 32,
    nCfuInputs: Int = 4
) extends HardwareConfig
