package manticore.compiler.assembly.utils

import java.nio.file.Path
import manticore.compiler.UnitFixtureTest
import scala.io.BufferedSource

trait TestCircuit {
  protected def prefixPath: String
  protected def masmFile: String
  protected def memInitFiles: Seq[String]

  def name: String = getClass().getSimpleName().takeWhile(_ != '$')

  def apply(f: UnitFixtureTest#FixtureParam): String = {
    // Copy the mem init files to the test directory as we cannot get the Path
    // of a file in the resource directory (it is a path inside the JVM).
    val memInitPaths = memInitFiles.map { fname =>
      val contents = scala.io.Source.fromResource(s"${prefixPath}/${fname}").mkString
      // Dumps in the test directory.
      f.dump(fname, contents)
    }

    // Replace the relative names of the mem init files hard-coded in the masm
    // file with their absolute paths (computed above).
    val source: String = scala.io.Source
      .fromResource(s"${prefixPath}/${masmFile}")
      .getLines()
      .map { l =>
        // Iterate over the names of the mem init files and look for them in the
        // line. If it exists, then replace it with the absolute path of the
        // mem init file in the test directory.
        memInitFiles.zip(memInitPaths).foldLeft(l) { case (ll, (fname, fpath)) =>
          ll.replace(s"${fname}", fpath.toAbsolutePath().toString())
        }
      }
      .mkString("\n")

    // Copy the modified masm resource file to the test directory (so we can look
    // at it if needed).
    f.dump(masmFile, source)

    source
  }
}

object Mips32Circuit extends TestCircuit {
  override protected def prefixPath: String = "levels/placed"
  override protected def masmFile: String = "mips32.masm"
  override protected def memInitFiles: Seq[String] = Seq(
    "mips32.masm.sum_inst_mem.data"
  )
}

object PicoRv32Circuit extends TestCircuit {
  override protected def prefixPath: String = "levels/placed"
  override protected def masmFile: String = "picorv32.masm"
  override protected def memInitFiles: Seq[String] = Seq(
    "picorv32.masm.dut.cpuregs.data",
    "picorv32.masm.memory.data"
  )
}

object Swizzle extends TestCircuit {
  override protected def prefixPath: String = "levels/placed"
  override protected def masmFile: String = "swizzle.masm"
  override protected def memInitFiles: Seq[String] = Seq(
    "swizzle.masm.in_rom.data",
    "swizzle.masm.out_rom.data"
  )
}

object Xormix32 extends TestCircuit {
  override protected def prefixPath: String = "levels/placed"
  override protected def masmFile: String = "xormix32.masm"
  override protected def memInitFiles: Seq[String] = Seq(
    "xormix32.mem.ref.hex",
    "xormix32.mem.salts.hex"
  )
}

object XorReduce extends TestCircuit {
  override protected def prefixPath: String = "levels/placed"
  override protected def masmFile: String = "xor_reduce.masm"
  override protected def memInitFiles: Seq[String] = Seq(
    "xor_reduce.masm.in_rom.data",
    "xor_reduce.masm.out_rom.data"
  )
}
