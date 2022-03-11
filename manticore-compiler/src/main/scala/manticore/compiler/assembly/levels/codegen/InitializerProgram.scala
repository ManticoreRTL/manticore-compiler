package manticore.compiler.assembly.levels.codegen

import manticore.compiler.assembly.levels.placed.PlacedIR._
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.HasTransformationID
import manticore.compiler.assembly.levels.ConstType
import manticore.compiler.assembly.levels.InputType
import manticore.compiler.assembly.levels.MemoryType
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.assembly.levels.WireType
import manticore.compiler.assembly.annotations.Memblock
import manticore.compiler.assembly.levels.placed.LatencyAnalysis
import scala.annotation.tailrec
import java.io.File
import java.nio.file.Files

object InitializerProgram
    extends ((DefProgram, AssemblyContext) => Unit)
    with HasTransformationID {

  override def apply(program: DefProgram, context: AssemblyContext): Unit = {

    context.output_dir match {
      case Some(dir_name: File) =>
        val initializers = makeInitializer(program)(context)
        initializers.zipWithIndex.foreach { case (init, ix) =>
          val path = Files.createDirectories(
            dir_name.toPath().resolve("initializer").resolve(ix.toString())
          )
          // assemble the program
          val assembled = MachineCodeGenerator.assembleProgram(init)(context)
          MachineCodeGenerator.generateCode(assembled, path)(context)
        }
      case None =>
        context.logger.error(
          "output directory not specified. Initializer will not be written"
        )
    }

  }

  /** Create a sequence of programs that can be used to initialize the Manticore
    * machine.
    *
    * @param program
    * @param ctx
    * @return
    */
  def makeInitializer(
      program: DefProgram
  )(implicit ctx: AssemblyContext): Seq[DefProgram] = {

    @tailrec
    def do_create(
        left: Seq[Seq[DefProcess]],
        builder: Seq[DefProgram]
    ): Seq[DefProgram] = {
      if (left.nonEmpty) {
        val nonempty_inits = left.collect { case head :: next =>
          head
        }
        val longest_init = nonempty_inits.map(_.body.length).max

        val master_process = nonempty_inits.head
        val processes =
          if (master_process.id.x == 0 && master_process.id.y == 0) {

            val const_0 = master_process.registers.head.variable.name
            val const_1 = master_process.registers.tail.head.variable.name

            val master_with_stop = master_process.copy(body =
              master_process.body :+ Expect(
                const_0,
                const_1,
                ExceptionIdImpl(UInt16(0), "stop", ExpectStop)
              )
            )

            master_with_stop +: nonempty_inits.tail

          } else {
            val const_0 = DefReg(
              ValueVariable(s"const_0_${ctx.uniqueNumber()}", 0, ConstType),
              Some(UInt16(0))
            )
            val const_1 = DefReg(
              ValueVariable(s"const_1_${ctx.uniqueNumber()}", 1, ConstType),
              Some(UInt16(1))
            )
            val body = Seq(
              SetValue(const_0.variable.name, UInt16(0)),
              SetValue(const_1.variable.name, UInt16(1))
            ) ++ Seq.fill(longest_init - 2)(Nop) :+
              Expect(
                const_0.variable.name,
                const_1.variable.name,
                ExceptionIdImpl(UInt16(0), "stop", ExpectStop)
              )
            val master_proc_with_stop = DefProcess(
              registers = Seq(const_0, const_1),
              id = ProcessIdImpl("placed_0_0", 0, 0),
              body = body,
              functions = Seq()
            )
            master_proc_with_stop +: nonempty_inits
          }
        val next_left = left.collect { case _ :: (tail @ (_ :: _)) => tail }
        do_create(next_left, builder :+ DefProgram(processes))
      } else {
        builder
      }
    }
    do_create(program.processes.map { makeInitializer }, Nil)
  }

  /** Create a sequence of idempotent processes that can be used to initialize
    * the registers and local memories for the given process. The reason that
    * there might be multiple processes used to initialize a processor is that
    * we may run out of instruction memory before we fully initialize all local
    * memories.
    *
    * @param process
    * @param ctx
    * @return
    */
  def makeInitializer(
      process: DefProcess
  )(implicit ctx: AssemblyContext): Seq[DefProcess] = {

    // This function can only be called only on a fully implemented process with
    // all registers allocated and memory pointers set.

    val initializer_regs = scala.collection.mutable.Queue.empty[DefReg]
    val initializer_body = scala.collection.mutable.Queue.empty[Instruction]

    val const_0 = process.registers(0)
    val const_1 = process.registers(1)


    process.registers.foreach {
      case r @ DefReg(vr, Some(vl), _)
          if (vr.varType == ConstType || vr.varType == InputType || vr.varType == MemoryType) =>
        initializer_regs += r
        initializer_body += SetValue(r.variable.name, vl)
      case _ => // do nothing
    }

    // then we enable storing
    if (initializer_body.length < LatencyAnalysis.maxLatency() + 2) {
      initializer_body ++= Seq.fill(
        LatencyAnalysis.maxLatency() + 2 - initializer_body.length
      ) { Nop }
      // place Nops between setting the value of const_1 and setting the predicate
      // if needed
    }
    initializer_body += Predicate(const_1.variable.name)

    // and now we create the program required to initialize the memories for
    // this we need to use a couple of temporary register, but since we can not
    // call the register allocator again, we need to perform a mini register
    // allocation and scheduling here. We assume the register allocator uses the
    // lower indices for immortal registers, so we can get the first free index,
    // i.e., the register index belonging to register without an initial value,
    // by simply counting the number of immortal registers.
    val first_free_index = process.registers.count { r =>
      r.variable.varType match {
        case _ @(ConstType | InputType | MemoryType) => true
        case _                                       => false
      }
    }
    // we basically need LatencyAnalysis.maxLatency() + 1 free registers to be
    // able to create sequences of 4 SetValues followed by 4 LocalStore
    // instructions such that we avoid placing Nops.

    val mem_init_slider_size = LatencyAnalysis.maxLatency() + 1
    val temp_regs = Seq.tabulate(mem_init_slider_size) { ix =>
      DefReg(
        variable = ValueVariable(
          s"temp_mem_init_${ctx.uniqueNumber()}",
          first_free_index + ix,
          WireType
        ),
        value = None
      )
    }
    if (temp_regs.last.variable.id >= ctx.max_registers) {
      ctx.logger.error(
        s"Can not create memory initializing sequence! " +
          s"Initializer requires at least ${temp_regs.last.variable.id + 1} " +
          s"registers but only have ${ctx.max_registers}"
      )
    }

    initializer_regs.enqueueAll(temp_regs)

    process.registers.foreach {
      case r @ DefReg(mvr: MemoryVariable, offset_opt, _) =>
        offset_opt match {
          case Some(init @ UInt16(offset)) =>
            mvr.block.initial_content.zipWithIndex
              .grouped(mem_init_slider_size)
              .foreach { window =>
                val zipped_window = window.zip(temp_regs)

                initializer_body ++= zipped_window.map {
                  case ((value, _), tmp) =>
                    SetValue(tmp.variable.name, value)
                }

                // in case the sliding window is "incomplete" place Nops instead
                // of SetValues
                if (window.length < mem_init_slider_size) {
                  initializer_body ++=
                    Seq.fill(mem_init_slider_size - window.length) { Nop }

                }
                // perform the actual store (predicate is set to 1 before)
                initializer_body ++=
                  zipped_window.map { case ((value, ix), tmp) =>
                    LocalStore(
                      rs = tmp.variable.name,
                      base = mvr.name,
                      offset = UInt16(ix),
                      predicate = None
                    )
                  }

              }
          case _ =>
            ctx.logger.error(s"Memory is not allocated!", r)
        }
      case _ =>
      /// do nothing
    }

    // now that we have all the registers and the instructions required to
    // initialize this process, we split the instruction sequence into smaller
    // ones if we have more instructions than we can fit into the instruction
    // memory

    val proc_regs = initializer_regs.toSeq

    initializer_body
      .grouped(
        ctx.max_instructions - 1
      ) // we leave 1 space for the EXPECT instruction that may be added later
      .map { case body =>
        process.copy(
          body = body.toSeq,
          registers = proc_regs
        )
      }
      .toSeq

  }

}
