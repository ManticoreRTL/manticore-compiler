package manticore.compiler.assembly.levels.codegen

import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.BinaryOperator
import manticore.compiler.assembly.annotations.Memblock
import manticore.compiler.assembly.levels.ConstType
import manticore.compiler.HasTransformationID
import manticore.compiler.assembly.levels.InputType
import manticore.compiler.assembly.levels.MemoryType
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.assembly.levels.WireType
import manticore.compiler.assembly.levels.placed.PlacedIR._

import java.io.File
import java.nio.file.Files
import scala.annotation.tailrec
import manticore.compiler.assembly
import scala.util.Try
import scala.util.Failure
import scala.util.Success

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

    val inits              = program.processes.map { makeInitializer }
    val maxInitsPerProcess = inits.map(_.length).max
    def emptyBody          = Seq.fill(ctx.hw_config.maxLatency) { Nop }
    val alignedInits = inits.map { thisInits =>
      thisInits ++ Seq.fill(maxInitsPerProcess - thisInits.length) {
        emptyInit(thisInits.head)
      }
    } // need to align them for transpose, but perhaps we could avoid having the extra empty
    // init processes by writing a for loop! I am just too lazy to do that...
    alignedInits.transpose.map { initProcs =>
      val maxLen = initProcs.maxBy(_.body.length).body.length
      val withFinish = initProcs.map { init =>
        val isMaster = (init.id.x == 0 && init.id.y == 0)
        if (isMaster) {

          init.copy(
            body = init.body ++ Seq.fill(maxLen - init.body.length) { Nop } :+ Interrupt(
              SimpleInterruptDescription(action = assembly.FinishInterrupt, eid = 0, info = None),
              init.registers(1).variable.name,
              SystemCallOrder(0)
            )
          )

        } else {
          init
        }
      }
      program.copy(processes = withFinish)
    }

  }

  /**
   * Create an empty initialization process, used to pad initialization sequence.
   * Any initialization sequence requires a zero and one value so does an empty one.
   */
  private def emptyInit(proc: DefProcess)(implicit ctx: AssemblyContext): DefProcess = {

    val constZero = DefReg(
      variable = ValueVariable("zero", 0, WireType),
      value = None
    )
    val constOne = DefReg(
      variable = ValueVariable("one", 1, WireType),
      value = None
    )

    // note that the SetValue instructions are not really necessary, because
    // an empty sequence only appears after full sequences in which the two
    // values are set
    val body = Seq(
      SetValue(constOne.variable.name, UInt16(1)),
      SetValue(constZero.variable.name, UInt16(0))
    ) ++ Seq.fill(ctx.hw_config.maxLatency) { Nop }

    proc.copy(
      body = body,
      registers = Seq(constZero, constOne)
    )

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
    // and handle initializing registers last. In doing so we break the initializaiton
    // into processes and segments withing processes. Each segments is supposed
    // to represent an atomic idempotent piece of code. Having idempotent code
    // is a require because of how Manticore handles exception. When a program
    // is finished, before loading the next one Mantcore performs  soft reset,
    // which puts each core in an initial state that wait for instructions
    // from the NoC. Now this means technically some instruction may execute after
    // kernel is started again after the previous FINISH and between the next soft
    // reset which can corrupt the initialized values in the register files and the
    // scratch pad. Atomic idempotent segments ensure that this does not happen.

    case class IdempotentSegment(
        body: Vector[Instruction],
        defs: Vector[DefReg]
    ) {
      require(body.length < ctx.hw_config.nInstructions, "can not split a segment into multiple programs!")
    }

    val constZero = DefReg(
      variable = ValueVariable("zero", 0, WireType),
      value = None
    )
    val constOne = DefReg(
      variable = ValueVariable("one", 1, WireType),
      value = None
    )

    // we initialize each memory with a sequence of SetValues followed by LocalStores
    // size of this sliding window is set to the hardware max latency + 1to avoid
    // inserting Nops
    val initWindowSize = ctx.hw_config.maxLatency + 1

    val zeroOneSegment = IdempotentSegment(
      Vector(
        SetValue(constOne.variable.name, UInt16(1)),
        SetValue(constZero.variable.name, UInt16(0))
      ) ++ Vector.fill(ctx.hw_config.maxLatency) { Nop } :+ Predicate(constOne.variable.name),
      Vector(
        constOne,
        constZero
      )
    )
    // check for bad memories
    process.registers.foreach {
      case r @ DefReg(mvr: MemoryVariable, None, _) =>
        ctx.logger.error(s"Memory not allocated", r)
      case _ => // ok
    }

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
    // create memory init segments
    val memInitSegments = process.registers.collect { case r @ DefReg(mvr: MemoryVariable, Some(offset), _) =>
      // all memories should be initialized, otherwise we may into weird runtime
      // bugs due uninitialized scratchpads with left over data from a previous
      // run on hardware
      assert(mvr.initialContent.length == mvr.size, s"Detected memory without initial content!")
      // each window should be made into a segment to avoid splitting the initialization
      // of a memory word into two initialization programs which can roll over and
      // corrupt register values
      for (window <- mvr.initialContent.zipWithIndex.grouped(initWindowSize)) yield {
        val segBody = MQueue.empty[Instruction]
        for (((word, index), (reg, regIx)) <- window.zip(memInitRegs zip memIndexRegs)) {
          segBody += SetValue(reg.variable.name, word)
          segBody += SetValue(regIx.variable.name, UInt16(index))
        }
        // we may have to insert some NOPs if there are not enough words to
        // handle data hazards. Essentially
        if (window.length < initWindowSize) {
          // add nops if necessary
          segBody ++= Seq.fill(initWindowSize - window.length) { Nop }
        }
        for ((value, index) <- memInitRegs.zip(memIndexRegs).take(window.length)) {
          segBody += LocalStore(
            rs = value.variable.name,
            base = mvr.name,
            address = index.variable.name,
            order = MemoryAccessOrder(mvr.name, 0),
            predicate = None
          )
        }
        IdempotentSegment(
          segBody.toVector,
          Vector.empty
        )
      }
    }.flatten

    val memDefs = process.registers.collect { case r @ DefReg(_: MemoryVariable, _, _) => r }
    // Initialize the custom functions.
    // No NOPs are needed as all the information needed to configure the LUTs
    // can be found in the instruction (cust_ram_idx, rd, and immediate fields).

    val cfuInitBody = MQueue.empty[Instruction]
    for (funcIdx <- Range.inclusive(0, process.functions.size - 1)) yield {
      val func      = process.functions(funcIdx)
      val equations = func.value.equation
      for (bitIdx <- Range.inclusive(0, equations.size - 1)) {
        val equation = equations(bitIdx)
        cfuInitBody += ConfigCfu(funcIdx, bitIdx, equation)
      }
    }

    val cfuSegment = IdempotentSegment(
      cfuInitBody.toVector,
      Vector(constZero, constOne)
    )

    // initializing registers is easy now
    val regInitSegment = process.registers.collect {
      case r @ DefReg(vr, vl, _) if (vr.varType == ConstType || vr.varType == InputType || vr.varType == MemoryType) =>
        if (vr.id == 0) {
          assert(vl == Some(UInt16(0)))
        } else if (vr.id == 1) {
          assert(vl == Some(UInt16(1)))
        }
        assert(vl.nonEmpty || vr.varType == InputType)
        IdempotentSegment(
          Vector(SetValue(vr.name, vl.getOrElse(UInt16(0)))),
          Vector(r)
        )
    }

    val predDisable = IdempotentSegment(
      Vector(Predicate(constZero.variable.name)),
      Vector(constZero)
    )

    // now we need to combine segments into processes of maximum
    // ctx.hw_config.nInstructions - ctx.hw_config.maxLatency  - 1 size, but
    // never splitting a single segment across two processes
    // the ctx.hw_config.maxLatency slack to is to ensure all instructions take
    // effect by placing Nops at the end. 1 extra space is required to place
    // a FINISH instruction in the master core.

    def combineSegments(toCombine: Iterable[IdempotentSegment], defs: Iterable[DefReg]): Iterable[DefProcess] = {
      val merged       = MQueue.empty[DefProcess]
      val segmentsLeft = MQueue.empty[IdempotentSegment] ++ toCombine

      while (segmentsLeft.nonEmpty) {

        var numInstr = 0

        val maxInstr = ctx.hw_config.nInstructions - ctx.hw_config.maxLatency - 1
        assert(maxInstr > 0)

        val builder = MQueue.empty[IdempotentSegment]

        while (segmentsLeft.nonEmpty && (numInstr + segmentsLeft.head.body.length) < maxInstr) {
          val seg = segmentsLeft.dequeue()
          numInstr += seg.body.length
          builder += seg
        }

        if (builder.nonEmpty) {
          merged += process.copy(
            body = builder.flatMap(_.body).toSeq ++ Seq.fill(ctx.hw_config.maxLatency) { Nop },
            registers = defs.toSeq
          )
        }
      }
      merged.toSeq
    }

    (
      combineSegments(
        Seq(zeroOneSegment) ++ memInitSegments ++ Seq(cfuSegment, predDisable), // fist initialize memory and then
        Seq(
          constZero,
          constOne
        ) ++ memIndexRegs ++ memInitRegs ++ memDefs // register file, DO NOT MIX them since it will
        // result in non-idempotent code!
      ) ++
        combineSegments(regInitSegment, regInitSegment.flatMap(_.defs))
    ).toSeq
  }

}
