package manticore.assembly.levels.placed

import manticore.assembly.levels.AssemblyTransformer

import scala.collection.mutable.{
  PriorityQueue => MPriorityQueue,
  Queue => MQueue,
  Set => MSet
}
import manticore.compiler.AssemblyContext
import manticore.assembly.levels.MemoryType


/**
  * Shrink the number of processes to the available number of cores
  * @author
  *   Mahyar Emami   <mahyar.emami@eplf.ch>
  */
object ProcessMergingTransform
    extends AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {

  import PlacedIR._

  override def transform(
      program: DefProgram,
      context: AssemblyContext
  ): DefProgram = {

    val MaxMemory = 1024 * 4 // 4 KiB is the maximum array memory for every core

    class ProcessWrapper(
        val body: MSet[Instruction] = MSet.empty[Instruction],
        val regs: MSet[DefReg] = MSet.empty[DefReg],
        var free_mem: Int = context.max_local_memory
    ) extends Ordered[ProcessWrapper] {
      // I hope MSet.size is constant time complexity
      override def compare(that: ProcessWrapper): Int =
        Ordering[(Int, Int)].reverse.compare(
          (this.body.size, this.free_mem),
          (that.body.size, that.free_mem)
        )

    }

    val num_processors = context.max_dimx * context.max_dimy

    if (program.processes.length < num_processors) {
      // no need to merge
      context.logger.info("Skipping process merge")
      program
    } else {
      val processor_queue =
        MPriorityQueue.empty[ProcessWrapper] ++
          Seq.fill(num_processors) { new ProcessWrapper() }
      // initialize the queue with empty processors
      // implicit val DefProcessOrdering: Ordering[DefProcess] =
      object DefProcessOrdering extends Ordering[DefProcess] {
        override def compare(x: DefProcess, y: DefProcess): Int =
          Ordering[Int].compare(x.body.length, y.body.length)

      }

      val to_merge =
        MPriorityQueue.empty[DefProcess](
          DefProcessOrdering
        ) ++ program.processes
      context.logger.info(s"Trying to shrink processes count from ${program.processes.length} into ${num_processors} by merging")
      var failed = false

      while (to_merge.nonEmpty && !failed) {
        val dequeued = MQueue.empty[ProcessWrapper]
        val process = to_merge.dequeue()
        if (process.functions.length != 0) {
          context.logger.fail("Can not handle LUT vectors yet")
        }
        var merged = false
        while (!merged) {
          if (processor_queue.nonEmpty) {
            val candidate_processor = processor_queue.dequeue()
            val mem_required = process.registers.collect {
              case DefReg(m: MemoryVariable, _, _) =>
                val capacity = m.block.capacity
                val width = m.block.width
                val num_shorts: Int = (width - 1) / 16 + 1
                val mem_usage = capacity * num_shorts * (16 / 8)
                mem_usage
            }.sum
            if (candidate_processor.free_mem >= mem_required) {
              // merge the two processes
              val max_body_count =
                candidate_processor.body.size + process.body.size
              if (max_body_count <= context.max_instructions_threshold) {
                candidate_processor.body ++= process.body
                candidate_processor.regs ++= process.registers
                candidate_processor.free_mem =
                  candidate_processor.free_mem - mem_required
                if (candidate_processor.body.size > max_body_count) {
                  // please don't happen, we don't handle it for now
                  context.logger.error(
                    "Can not handle high instruction pressure yet!"
                  )
                }
                merged = true
                dequeued += candidate_processor
              } else {
                context.logger.warn("High instruction memory pressure!")
                dequeued += candidate_processor
                merged = false
              }
            } else {
              // take another processor and see if that one has enough memory
              context.logger.warn("High local memory pressure!")
              dequeued += candidate_processor
              merged = false
            }
          } else {
            context.logger.error(s"Failed merging process ${process.id}")
            failed = true
            merged = true
          }
        }
        processor_queue ++= dequeued.dequeueAll(_ => true)
      }

      val merged = processor_queue.zipWithIndex.collect {
        case (nonempty: ProcessWrapper, ix: Int) if nonempty.body.size > 0 =>
          DefProcess(
            id = ProcessIdImpl(s"merged_${ix}", -1, -1),
            body = nonempty.body.toSeq,
            functions = Seq.empty[DefFunc],
            registers = nonempty.regs.toSeq
          )
      }.toSeq

      program
        .copy(
          processes = merged
        )
        .setPos(program.pos)
    }

  }
}
