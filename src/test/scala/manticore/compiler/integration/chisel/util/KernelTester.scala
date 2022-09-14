package manticore.compiler.integration.chisel.util

import chisel3._
import chiseltest.ChiselScalatestTester
import chiseltest._
import manticore.compiler.AssemblyContext
import manticore.compiler.HasLoggerId
import manticore.compiler.ManticorePasses
import manticore.compiler.UnitFixtureTest
import manticore.compiler.assembly.levels.codegen.InitializerProgram
import manticore.compiler.assembly.levels.codegen.MachineCodeGenerator
import manticore.compiler.assembly.levels.placed.interpreter.AtomicInterpreter
import manticore.machine.xrt.ManticoreFlatSimKernel

import java.io.FileOutputStream
import java.io.PrintWriter

trait KernelTester
    extends UnitFixtureTest
    with ChiselScalatestTester
    with ProgramTester {

  def compileAndRun(source: String, context: AssemblyContext)(implicit
      fixture: FixtureParam
  ): Unit = {

    implicit val log_id = new HasLoggerId { val id = getTestName }

    val program = compile(source, context)

    // interpret to ensure our expectations are met before RTL simulation is run
    AtomicInterpreter(program)(context)

    val assembled_program =
      MachineCodeGenerator.assembleProgram(program)(context)



    val initializers = InitializerProgram.makeInitializer(program)(context)

    val initializer_streams = initializers.map { init =>
      val asm = MachineCodeGenerator.assembleProgram(init)(context)
      MachineCodeGenerator.generateCode(asm, fixture.test_dir.resolve("init"))(context)
      val bstream = MachineCodeGenerator.makeBinaryStream(asm)(context)
      bstream
    }
    MachineCodeGenerator.generateCode(assembled_program)(context)
    val vcycles_length =
      assembled_program.map(_.total).max + context.hw_config.maxLatency
    context.logger.info(s"Virtual cycles length is ${vcycles_length}")

    test(
      new ManticoreFlatSimKernel(
        DimX = context.hw_config.dimX,
        DimY = context.hw_config.dimY,
        debug_enable = true
        // prefix_path =
        //   fixture.test_dir.resolve("out").toAbsolutePath().toString()
      )
    ).withAnnotations(Seq(VerilatorBackendAnnotation, WriteVcdAnnotation)) {
      dut =>
        var cycle = 0
        def tick(n: Int = 1): Unit = {
          dut.clock.step(n)
          cycle += n
        }

        def copyToDRAM(bstream: Seq[Int]): Unit = {
          // take control of the DRAM
          dut.io.dmi.locked.poke(true.B)
          // fill in the dram with the binary program
          bstream.zipWithIndex.foreach { case (b, ix) =>
            dut.io.dmi.addr.poke(ix.U)
            dut.io.dmi.wen.poke(true.B)
            dut.io.dmi.wdata.poke(b.U)
            tick()
          }
          // release the DRAM
          dut.io.dmi.wen.poke(false.B)
          dut.io.dmi.locked.poke(false.B)
        }
        // let the reset propagate through the cores
        context.logger.info("Starting RTL simulation")

        // initialize the DRAM with initializer programs
        initializer_streams.foreach { bstream =>
          copyToDRAM(bstream)
          // execute the initializer
          tick()
          dut.io.kernel_ctrl.start.poke(true.B)
          tick()
          dut.io.kernel_ctrl.start.poke(false.B)
          tick()

          dut.clock.setTimeout(
            context.hw_config.nInstructions + bstream.length * 600 * context.hw_config.dimX * context.hw_config.dimY
          )

          while (!dut.io.kernel_ctrl.idle.peek().litToBoolean) {
            tick()
          }
          context.logger.info(s"Finished initializer at ${cycle}")

        }

        tick(20)

        copyToDRAM(MachineCodeGenerator.makeBinaryStream(assembled_program)(context))
        tick()

        dut.io.kernel_ctrl.start.poke(true.B)
        tick()
        dut.io.kernel_ctrl.start.poke(false.B)
        tick()

        dut.clock.setTimeout(
          // set the timeout to be the the time required for execution plus
          // some time required to program the processors
          if (context.expected_cycles.isDefined) {
            vcycles_length * context.expected_cycles.get +
            vcycles_length * 100 * context.hw_config.dimX * context.hw_config.dimY
          } else {
            vcycles_length * 100 * context.hw_config.dimX * context.hw_config.dimY
          }
        )

        while (!dut.io.kernel_ctrl.idle.peek().litToBoolean) {
          tick()
        }

        context.logger.info(
          s"Got IDLE after ${cycle} cycles with " +
            s"${dut.io.kernel_registers.device.bootloader_cycles.peek().litValue} " +
            s"cycles to boot the machine"
        )

        assert(
          dut.io.kernel_registers.device.exception_id
            .peek()
            .litValue < 0x8000,
          "execution resulted in fatal exception!"
        )
        if (context.expected_cycles.isDefined) {
          assert(
            dut.io.kernel_registers.device.virtual_cycles
              .peek()
              .litValue == context.expected_cycles.get,
            "invalid number of virtual cycles!"
          )
        }
    }
  }
}
