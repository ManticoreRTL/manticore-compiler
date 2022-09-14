package manticore.compiler.assembly.levels.codegen

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.BinaryOperator
import manticore.compiler.assembly.annotations.Memblock
import manticore.compiler.assembly.levels.ConstType
import manticore.compiler.assembly.levels.HasTransformationID
import manticore.compiler.assembly.levels.InputType
import manticore.compiler.assembly.levels.MemoryType
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.assembly.levels.WireType
import manticore.compiler.assembly.levels.placed.PlacedIR._

import java.io.File
import java.nio.file.Files
import scala.annotation.tailrec

object InitializerProgram extends ((DefProgram, AssemblyContext) => Unit) with HasTransformationID {

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

    program.processes.map { makeInitializer }.transpose.map { initProcs =>
      val maxLen = initProcs.maxBy(_.body.length).body.length
      val withFinish = initProcs.map { init =>
        val isMaster = (init.id.x == 0 && init.id.y == 0)
        if (isMaster) {
          init.copy(
            body = init.body ++ (Seq.fill(maxLen - init.body.length) { Nop } :+ Interrupt(
              FinishInterrupt,
              init.registers(1).variable.name,
              SystemCallOrder(0)
            ))
          )
        } else {
          init
        }
      }
      program.copy(processes = withFinish)
    }

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

    import scala.collection.mutable.{Queue => MQueue}
    // This function can only be called only on a fully implemented process with
    // all registers allocated and memory pointers set.

    // we start by filling up the scratch pad, then configuring custom functions
    // and handle initializing registers last.

    val initMonoBody = MQueue.empty[Instruction]
    // note that since we can not call the register allocator, we need to
    // do a mini version of it here
    val freeIndices = MQueue.empty[Int] ++ Range(2, ctx.hw_config.nRegisters)
    // first things first we need to set the predicate bit to enable storing
    val constZero = DefReg(
      variable = ValueVariable("zero", 0, WireType),
      value = None
    )
    val constOne = DefReg(
      variable = ValueVariable("one", 1, WireType),
      value = None
    )
    initMonoBody += SetValue(constOne.variable.name, UInt16(1))
    initMonoBody += SetValue(constZero.variable.name, UInt16(0))
    initMonoBody ++= Seq.fill(ctx.hw_config.maxLatency - 1) { Nop }
    initMonoBody += Predicate(constOne.variable.name) // enable storing to scratchpad
    // we initialize each memory with a sequence of SetValues followed by LocalStores
    // size of this sliding window is set to the hardware max latency + 1to avoid
    // inserting Nops
    val initWindowSize = ctx.hw_config.maxLatency + 1

    // these register will hold the memory values
    val memInitRegs = Seq.tabulate(initWindowSize) { ix =>
      DefReg(
        variable = ValueVariable(s"mw_$ix", ix + 2, WireType),
        value = None
      )
    }

    // and these register values will hold the memory index
    val memIndexRegs = Seq.tabulate(initWindowSize) { ix =>
      DefReg(
        variable = ValueVariable(s"mix_$ix", ix + memInitRegs.last.variable.id + 1, WireType),
        value = None
      )
    }
    val memoriesToInit = process.registers.foreach {
      case r @ DefReg(mvr: MemoryVariable, None, _) =>
        ctx.logger.error(s"Memory not allocated", r)
      case r @ DefReg(mvr: MemoryVariable, Some(offset), _) =>
        for (window <- mvr.initialContent.zipWithIndex.grouped(initWindowSize)) {
          for (((word, index), (reg, regIx)) <- window.zip(memInitRegs zip memIndexRegs)) {
            initMonoBody += SetValue(reg.variable.name, word)
            initMonoBody += SetValue(regIx.variable.name, UInt16(index))
          }
          // we may have to insert some NOPs if there are not enough words to
          // handle data hazards. Essentially
          if (window.length < initWindowSize) {
            // add nops if necessary
            initMonoBody ++= Seq.fill(initWindowSize - window.length) { Nop }
          }
          for ((value, index) <- memInitRegs.zip(memIndexRegs)) {
            initMonoBody += LocalStore(
              rs = value.variable.name,
              base = mvr.name,
              address = index.variable.name,
              order = MemoryAccessOrder(mvr.name, 0),
              predicate = None
            )
          }
        }
      case _ =>
      // nothing to do
    }

    initMonoBody += Predicate(constZero.variable.name) // disable stores
    // Initialize the custom functions.
    // No NOPs are needed as all the information needed to configure the LUTs
    // can be found in the instruction (cust_ram_idx, rd, and immediate fields).
    for (funcIdx <- Range.inclusive(0, process.functions.size - 1)) {
      val func      = process.functions(funcIdx)
      val equations = func.value.equation
      for (bitIdx <- Range.inclusive(0, equations.size - 1)) {
        val equation = equations(bitIdx)
        initMonoBody += ConfigCfu(funcIdx, bitIdx, equation)
      }
    }
    val regsWithInit = MQueue.empty[DefReg]
    regsWithInit += constZero
    regsWithInit += constOne
    regsWithInit ++= memIndexRegs
    regsWithInit ++= memInitRegs

    // initializing registers is easy now
    process.registers.foreach {
      case r @ DefReg(vr, Some(vl), _)
          if (vr.varType == ConstType || vr.varType == InputType || vr.varType == MemoryType) =>
        regsWithInit += r
        if (vr.id == 0) {
          assert(vl == UInt16(0))
        } else if (vr.id == 1) {
          assert(vl == UInt16(1))
        }
        initMonoBody += SetValue(vr.name, vl)
      case _ => // nothing to do
    }

    val regSeq = regsWithInit.toSeq

    // we need to at least have ctx.hw_config.maxLatency spare instructions
    // in the memory avoid weird instruction overflow bugs
    assert(
      ctx.hw_config.nInstructions > ctx.hw_config.maxLatency,
      s"Minimum instruction memory size is ${ctx.hw_config.maxLatency}"
    )

    // the master process
    initMonoBody
      .grouped(
        ctx.hw_config.nInstructions - ctx.hw_config.maxLatency - 1 // -1 because we have to append a Finish later
      )                                                            // -1 to be able to add a Finish instructions
      .map { body =>
        process.copy(
          body = body.toSeq,
          registers = regSeq
        )
      }
      .toSeq

  }

}
