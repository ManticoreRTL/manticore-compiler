package manticore.compiler.integration.yosys.micro

import manticore.compiler.AssemblyContext
import manticore.compiler.FileDescriptor
import manticore.compiler.WithInlineVerilog
import manticore.compiler.WithResource
import manticore.compiler.integration.yosys.unit.YosysUnitTest
import org.scalatest.CancelAfterFailure

import java.io.PrintWriter
import java.nio.file.Files
import scala.collection.mutable.ArrayBuffer
import java.nio.file.Path

final class RayTracerBench extends MicroBench with CancelAfterFailure {

  type TestConfig = Int

  override def benchName: String = "vortex"

  override def top: Option[String] = Some("Main")

  override def verilogSources(cfg: TestConfig): Seq[FileDescriptor] = Seq(
    WithResource(s"integration/yosys/micro/ray_tracer/Pano.v"),
  )

  override def hexSources(cfg: TestConfig): Seq[FileDescriptor] = Seq(
    WithResource("integration/yosys/micro/ray_tracer/Pano.v_toplevel_core_u_pano_core_rt_u_normalize_ray_u_rsqrt_rsqrt_table.bin"),
    WithResource("integration/yosys/micro/ray_tracer/Pano.v_toplevel_core_u_pano_core_rt_u_plane_intersect_u_div_p0r0_dot_norm_denom_div_table.bin"),
    WithResource("integration/yosys/micro/ray_tracer/Pano.v_toplevel_core_u_pano_core_rt_u_shadow_sphere_intersect_u_normalize_u_rsqrt_rsqrt_table.bin"),
    WithResource("integration/yosys/micro/ray_tracer/Pano.v_toplevel_core_u_pano_core_rt_u_shadow_sphere_intersect_u_thc_sqrt_table.bin"),
    WithResource("integration/yosys/micro/ray_tracer/Pano.v_toplevel_core_u_pano_core_rt_u_sphere_intersect_u_normalize_u_rsqrt_rsqrt_table.bin"),
    WithResource("integration/yosys/micro/ray_tracer/Pano.v_toplevel_core_u_pano_core_rt_u_sphere_intersect_u_thc_sqrt_table.bin"),
    WithResource("integration/yosys/micro/ray_tracer/Pano.v_toplevel_core_u_pano_core_u_mr1_top_ram_cpu_ram_symbol0.bin"),
    WithResource("integration/yosys/micro/ray_tracer/Pano.v_toplevel_core_u_pano_core_u_mr1_top_ram_cpu_ram_symbol1.bin"),
    WithResource("integration/yosys/micro/ray_tracer/Pano.v_toplevel_core_u_pano_core_u_mr1_top_ram_cpu_ram_symbol2.bin"),
    WithResource("integration/yosys/micro/ray_tracer/Pano.v_toplevel_core_u_pano_core_u_mr1_top_ram_cpu_ram_symbol3.bin"),
    WithResource("integration/yosys/micro/ray_tracer/Pano.v_toplevel_core_u_pano_core_u_txt_gen_u_font_bitmap_ram.bin"),
  )

  override def testBench(cfg: TestConfig): FileDescriptor = {
    WithResource("integration/yosys/micro/ray_tracer/TestBench.sv")
  }

  override def outputReference(cfg: TestConfig): ArrayBuffer[String] = {

    val tempDir = Files.createTempDirectory("vref")

    implicit val ctx = AssemblyContext(
      log_file = Some(tempDir.resolve("verilator.log").toFile())
    )

    val vPaths = (verilogSources(cfg) :+ testBench(cfg)).map(f => f.p)
    val hPaths = hexSources(cfg).map(f => f.p)
    YosysUnitTest.verilate(vPaths, hPaths, timeOut)
  }

  override def timeOut: Int = 1000000

  val cfg: TestConfig = 1
  testCase(s"vortex", cfg)
}
