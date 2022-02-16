package manticore.integration

import manticore.machine.Generator.generateSimulationKernel
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import manticore.integration.thyrio.Make
import manticore.integration.thyrio.ExternalTool
import manticore.integration.thyrio.Verilator
import manticore.integration.thyrio.ThyrioFrontend
import manticore.assembly.levels.HasTransformationID
import manticore.compiler.Logger
import java.io.PrintWriter
import com.sun.jna.NativeLibrary
import com.sun.jna.Library
import com.sun.jna.Native
import scala.jdk.CollectionConverters.MapHasAsJava

import manticore.compiler.util.caching.DefaultSimLibCache
import manticore.compiler.util.caching.SimConfig
import java.rmi.UnexpectedException

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
    case None    => throw new UnexpectedException("Could not create libsim")
  }

  val shared_lib = NativeLibrary.getInstance(
    lib_path.toAbsolutePath().toString(),
    Map(Library.OPTION_OPEN_FLAGS -> 2).asJava
  )

  Native.setProtected(true)

  val native_sim_object = shared_lib.getFunction("ctor").invokePointer(Array())

  def simulate(max_cycles: Int) =
    shared_lib.getFunction("simulate").invoke(Array(native_sim_object))

}

class TempTester extends ThyrioUnitTest {

  override val requiredTools: Seq[ExternalTool] =
    Seq(Make, Verilator, ThyrioFrontend)

  behavior of "TempTester"

  it should "contain crashes" in { _ =>
    val thread = new Thread {
      override def run() {
        val sim = new SimulationHarness(2, 2)
      }
    }
    thread.start()

  }

  // sim.simulate(2)

}
