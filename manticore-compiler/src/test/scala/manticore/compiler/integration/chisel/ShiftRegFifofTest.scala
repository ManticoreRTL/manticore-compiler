package manticore.compiler.integration.chisel

import manticore.compiler.UnitFixtureTest

import manticore.compiler.integration.chisel.util.ProgramTester
import manticore.compiler.ManticorePasses
import manticore.compiler.AssemblyContext
import manticore.compiler.HasLoggerId
import manticore.compiler.assembly.levels.codegen.MachineCodeGenerator
import manticore.compiler.integration.chisel.util.ProcessorTester
import chiseltest._
import chisel3._
import manticore.machine.xrt.ManticoreFlatSimKernel
import chisel3.internal.prefix
import manticore.compiler.integration.chisel.util.KernelTester

class ShiftRegFifofTest extends KernelTester {
  behavior of "ShiftRegFifo"

  override def compiler =
    ManticorePasses.frontend followedBy
      ManticorePasses.middleend followedBy
      ManticorePasses.backend
  it should "shift values in a shift register using a single core" in {
    fixture =>
      val source: String = scala.io.Source
        .fromResource(
          "integration/microbench/baked_tests/shift_reg_fifo/shift.masm"
        )
        .mkString("")

      val context = AssemblyContext(
        output_dir = Some(fixture.test_dir.resolve("out").toFile()),
        max_dimx = 1,
        max_dimy = 1,
        dump_all = true,
        dump_dir = Some(fixture.test_dir.resolve("dumps").toFile()),
        expected_cycles = Some(6)
      )

      compileAndRun(source, context)(fixture)

  }

  it should "shift values in a shift register using 4 cores" in {
    fixture =>
      val source: String = scala.io.Source
        .fromResource(
          "integration/microbench/baked_tests/shift_reg_fifo/shift.masm"
        )
        .mkString("")

      val context = AssemblyContext(
        output_dir = Some(fixture.test_dir.resolve("out").toFile()),
        max_dimx = 2,
        max_dimy = 2,
        dump_all = true,
        dump_dir = Some(fixture.test_dir.resolve("dumps").toFile()),
        expected_cycles = Some(6)
      )

      compileAndRun(source, context)(fixture)

  }

}
