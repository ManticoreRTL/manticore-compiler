package manticore.compiler.integration.chisel.util

import manticore.compiler.AssemblyContext
import manticore.machine.core.Processor
import manticore.machine.ManticoreBaseISA
import manticore.compiler.assembly.levels.codegen.MachineCodeGenerator
import chiseltest._
import chisel3._
import chisel3.experimental.BundleLiterals._
import manticore.machine.core.BareNoCBundle

trait ProcessorTester {

  def mkProcessor(context: AssemblyContext, x: Int = 0, y: Int = 0) = {
    require(context.output_dir.nonEmpty)
    new Processor(
      config = ManticoreBaseISA,
      DimX = context.hw_config.dimX,
      DimY = context.hw_config.dimY,
      equations = Seq.fill(32)(Seq.fill(16)(0)),
      initial_registers = context.output_dir.get
        .toPath()
        .resolve(s"rf_${x}_${y}.dat")
        .toAbsolutePath()
        .toString(),
      name_tag = s"CoreX${x}Y${y}",
      initial_array = context.output_dir.get
        .toPath()
        .resolve(s"ra_${x}_${y}.dat")
        .toAbsolutePath()
        .toString()
    )
  }

  def programProcessor(
      dut: Processor,
      process: MachineCodeGenerator.AssembledProcess,
      sleep: Int = 0,
      countdown: Int = 1,
  ): Unit = {

    dut.clock.step()
    dut.clock.step(10)
        val empty_packet = new BareNoCBundle(ManticoreBaseISA).Lit(
          _.data -> 0.U,
          _.address -> 0.U,
          _.valid -> false.B
        )

        val boot_stream =
          Seq(
            process.body.length.toLong
          ) ++ process.body.flatMap { w64 =>
            Seq(
              w64 & 0xffff,
              (w64 >> 16L) & 0xffff,
              (w64 >> 32L) & 0xffff,
              (w64 >> 48L) & 0xffff
            )
          } ++ Seq(process.epilogue.toLong, 5L, 4L)

        boot_stream.foldLeft(()) { case (_, w: Long) =>
          dut.io.packet_in.poke(
            new BareNoCBundle(ManticoreBaseISA).Lit(
              _.data -> w.U,
              _.address -> 0.U,
              _.valid -> true.B
            )
          )
          dut.clock.step()
        }
        dut.io.packet_in.poke(empty_packet)


  }




}
