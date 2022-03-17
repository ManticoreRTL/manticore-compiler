package manticore.compiler.integration.util


import java.nio.file.Path
import manticore.compiler.Logger
import manticore.compiler.assembly.levels.TransformationID
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import scala.sys.process.ProcessLogger
import scala.sys.process.Process
import manticore.machine.Generator.generateSimulationKernel
import java.io.PrintWriter
import manticore.compiler.assembly.levels.HasTransformationID
import com.sun.jna.Library
import com.sun.jna.NativeLibrary
import scala.jdk.CollectionConverters.MapHasAsJava
import manticore.compiler.integration.ThyrioUnitTest
import manticore.compiler.integration.thyrio.ExternalTool
import manticore.compiler.integration.thyrio.ThyrioFrontend
import manticore.compiler.integration.thyrio.Verilator
import manticore.compiler.integration.thyrio.Make

case class CachePath(path: Path)

trait ExternalObjectCache {

  val cache_root: Path

  val logger: Logger

  implicit val phase_id: TransformationID = TransformationID(
    getClass().getSimpleName()
  )

}

case class SimConfig(dimx: Int, dimy: Int, debug_enable: Boolean)
class SimLibCache(
    val cache_root: Path = Path.of(".").resolve(".manticore").resolve("sim"),
    db_en: Boolean = false
) extends ExternalObjectCache {

  val logger = Logger(
    db_en = false,
    info_en = true,
    dump_dir = None,
    dump_all = false,
    log_file = None
  )

  private def timed(msg: String)(fn: => Unit): Double = {
    val now = System.nanoTime()
    fn
    val duration = (System.nanoTime() - now) * 1e-9
    logger.info(f"${msg} took ${duration}%.3f s")
    logger.flush()
    return duration
  }

  private def generateSimLib(
      config: SimConfig
  ): Option[Path] = {

    val object_dir = cache_root
      .resolve("build")
      .resolve(s"${config.dimy}_${config.dimy}_${config.debug_enable}")

    Files.createDirectories(object_dir)
    // generates the Verilog files

    timed("Verilog generation") {
      generateSimulationKernel(
        object_dir.toAbsolutePath().toString(),
        config.dimx,
        config.dimy,
        config.debug_enable
      )
    }

    // generate CPP sources by calling Verilator
    def copyResource(resource: String, dest: Path): Unit = {
      val src =
        scala.io.Source
          .fromResource(s"integration/cosim/$resource")
          .getLines()
          .mkString("\n")
      val printer = new PrintWriter(dest.toFile())
      printer.print(src)
      printer.flush()
      printer.close()
    }
    Seq(
      "harness.cpp",
      "harness.hpp",
      "Makefile",
      "input.vc"
    ).foreach { x => copyResource(x, object_dir.resolve(x)) }

    def executeMake(cwd: Path): Unit = {
      val out = new StringBuilder
      val ret = Process(
        command = s"make -j${Runtime.getRuntime.availableProcessors() / 2}",
        cwd = cwd.toFile()
      ).!(ProcessLogger(ln => out ++= s"${ln}\n"))
      if (ret != 0) {
        logger.error(s"Make failed with (${ret})\n ${out}")
      }
    }
    timed("Generate C++ sources") {
      executeMake(object_dir)
    }

    copyResource(
      "libsim.mk",
      object_dir.resolve("lib").resolve("Makefile")
    )

    timed("Creating sim shared library") {
      executeMake(object_dir.resolve("lib"))
    }

    if (logger.countErrors() == 0) {
      Some(object_dir.resolve("lib").resolve("libsim.so"))
    } else {
      None
    }

  }

  def populateCache(configs: Seq[SimConfig]): Unit = {

    // check that make exists
    if (Process("make -v").!(ProcessLogger(_ => ())) != 0) {
      logger.error("Make is not installed! Can not create cache")
    } else {

      configs.foreach { conf =>
        if (logger.countErrors() == 0) {
          val libname =
            s"libsim${conf.dimx}x${conf.dimy}${if (conf.debug_enable) "_dbg"
            else ""}.so"
          val lib_inst_path = cache_root.resolve(libname)
          if (lib_inst_path.toFile().isFile() == false) {
            logger.info(s"Creating ${libname}")
            generateSimLib(conf) match {
              case Some(lib_path) =>
                Files.copy(
                  lib_path,
                  lib_inst_path,
                  StandardCopyOption.REPLACE_EXISTING
                )
              case None =>
                logger.warn(s"Failed to create ${libname}")
            }
          } else {
            logger.info(
              s"Found ${libname} in ${lib_inst_path.toAbsolutePath.toString}"
            )
          }
        }
      }
    }

  }

  def cache(conf: SimConfig): Option[Path] = {
    if (Process("make -v").!(ProcessLogger(_ => ())) != 0) {
      logger.error("Make is not installed! Can not create cache")
      None
    } else {
      val libname =
        s"libsim${conf.dimx}x${conf.dimy}${if (conf.debug_enable) "_dbg"
        else ""}.so"
      val lib_inst_path = cache_root.resolve(libname)
      if (lib_inst_path.toFile().isFile() == false) {
        logger.info(s"Creating ${libname}")
        generateSimLib(conf) match {
          case Some(lib_path) =>
            Files.copy(
              lib_path,
              lib_inst_path,
              StandardCopyOption.REPLACE_EXISTING
            )
            Some(lib_inst_path)
          case None =>
            logger.warn(s"Failed to create ${libname}")
            None
        }
      } else {
        logger.info(
          s"Found ${libname} in ${lib_inst_path.toAbsolutePath.toString}"
        )
        Some(lib_inst_path)
      }

    }
  }

}

object DefaultSimLibCache extends SimLibCache


class SimulationHarness(dimx: Int, dimy: Int) extends HasTransformationID {

  val logger = Logger(
    db_en = false,
    info_en = true,
    dump_dir = None,
    dump_all = false,
    log_file = None
  )

  object LoadFlags { // from glibc dlfcn.h
    val RTLD_LAZY = 1
    val RTLD_NOW = 2
  }
  val lib_path = DefaultSimLibCache.cache(SimConfig(dimx, dimy, true)) match {
    case Some(p) => p
    case None    => throw new RuntimeException("Could not create libsim")
  }

  val shared_lib = NativeLibrary.getInstance(
    lib_path.toAbsolutePath().toString(),
    Map(Library.OPTION_OPEN_FLAGS -> 2).asJava
  )



  val native_sim_object = shared_lib.getFunction("ctor").invokePointer(Array())

  def simulate(max_cycles: Int) =
    shared_lib.getFunction("simulate").invoke(Array(native_sim_object))
  def run_state() = shared_lib.getFunction("run_state").invokeInt(Array())


}

class TempTester extends ThyrioUnitTest {

  override val requiredTools: Seq[ExternalTool] =
    Seq(Make, Verilator, ThyrioFrontend)

  behavior of "TempTester"

  it should "contain crashes" in { _ =>
    val sim = new SimulationHarness(2, 2)
    println(sim.run_state())
    sim.simulate(1)

  }

  // sim.simulate(2)

}