package manticore.compiler.assembly.levels.placed.lowering

import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.assembly.levels.placed.PlacedIRTransformer
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.AssertionInterrupt
import manticore.compiler.assembly.FinishInterrupt
import manticore.compiler.assembly.SerialInterrupt
import manticore.compiler.assembly.StopInterrupt
import manticore.compiler.assembly.levels.ConstType
import manticore.compiler.assembly.levels.UInt16
import scala.util.Try
import scala.util.Failure
import scala.util.Success

/**
  * A simple pass to lower interrupts into basic instructions and assign eids
  * used by the runtime to distinguish between the different kinds of interrupts
  * @author Mahyar Emami <mahyar.emami@epfl.ch>
  *
  */

object InterruptLoweringTransform extends PlacedIRTransformer {
  import PlacedIR._

  override def transform(program: DefProgram)(implicit ctx: AssemblyContext): DefProgram = {
    program.copy(processes = program.processes.map(transform))
  }

  private def transform(process: DefProcess)(implicit ctx: AssemblyContext): DefProcess = {

    val constants = scala.collection.mutable.Map.empty[UInt16, DefReg] ++
      process.registers.collect { case c @ DefReg(ValueVariable(_, _, ConstType), Some(v), _) => v -> c }
    def getConstant(value: UInt16): Name = {
      if (!constants.contains(value)) {
        val newConst = DefReg(
          ValueVariable(
            s"%c${ctx.uniqueNumber()}",
            -1,
            ConstType
          ),
          Some(value)
        )
        constants += (value -> newConst)
      }
      constants(value).variable.name
    }

    // go through the instructions one by one and replace every PutSerial
    // with a GlobalStore instruction, keeping track of every stored word
    // until we reach an Interrupt instruction with a SimpleInterruptDescription.
    // We then append the stored addresses to the description and assign an
    // eid to it. For other interrupts we only assign an eid, while making sure
    // failing ones get assigned an eid larger than or equal to 0x8000.
    class UpCounter(init: Int = 0) {
      private var cnt = init
      def next(): Int = {
        val r = cnt
        cnt += 1
        r
      }
    }
    class SerialQueue {
      private val queue        = scala.collection.mutable.Queue.empty[UInt16]
      private var maxQueueSize = 0
      private var order        = 0
      private var memoryName   = s"%system${ctx.uniqueNumber()}"
      def flush(): Seq[UInt16] = {
        queue.dequeueAll(_ => true)
      }
      def put(pt: PutSerial): GlobalStore = {
        val index = queue.length
        val instr = GlobalStore(
          pt.rs,
          Seq(getConstant(UInt16(index)), getConstant(UInt16(0)), getConstant(UInt16(0))),
          Some(pt.pred),
          pt.order
        )
        order += 1
        queue += UInt16(index)
        maxQueueSize = maxQueueSize max queue.length
        instr
      }
      def mkGlobalMemory =  {
        assert(queue.isEmpty, "something is up with lowering PutSerial/SerialInterrupt")
        DefGlobalMemory(memoryName, maxQueueSize, 0)
      }
    }
    var successEid  = new UpCounter(0)
    var failureEid  = new UpCounter(0x8000)
    var serialQueue = new SerialQueue

    val newBody = process.body.map {
      case intr @ Interrupt(description, condition, _, _) =>
        description match {
          case d: SerialInterruptDescription =>
            Try { serialQueue.flush() } match {
              case Failure(exception) =>
                ctx.logger.error("Failed flushing serial queue", intr)
                intr.copy(
                  description = d.copy(eid = successEid.next(), pointers = Nil)
                )
              case Success(indices) =>
                intr.copy(
                  description = d.copy(eid = successEid.next(), pointers = indices.map(_.toInt))
                )
            }

          case d: SimpleInterruptDescription =>
            assert(!d.action.isInstanceOf[SerialInterrupt], s"did not expect action ${intr}")
            val eid =
              if (d.action == AssertionInterrupt || d.action == StopInterrupt) failureEid.next() else successEid.next()
            intr.copy(
              description = d.copy(eid = eid)
            )
        }
      case put @ PutSerial(rs, pred, order, annons) =>
        serialQueue.put(put)
      case jtb : JumpTable =>
        ctx.logger.error("Can not handle instruction", jtb)
        jtb
      case instr => instr
    }

    val newRegisters = constants.map(_._2).toSeq.sortBy(_.value.get.toInt) ++ process.registers.filter(_.variable.varType != ConstType)
    val newProcess = process.copy(
        body = newBody,
        registers = newRegisters,
        globalMemories = serialQueue.mkGlobalMemory +: process.globalMemories
    )
    newProcess
  }

}
