package manticore.assembly.levels.unconstrained

import manticore.UnconstrainedTest
import java.nio.file.Path
import java.io.PrintWriter

trait UnconstrainedWideTest extends UnconstrainedTest {

  lazy val randgen = new scala.util.Random(0)
  def mkWideRand(w: Int): BigInt = {
    val shorts = Seq.fill((w - 1) / 16 + 1) { randgen.nextInt(1 << 16) }

    val combined = shorts.foldLeft(BigInt(0)) { case (c, x) =>
      (c << 16) | BigInt(x)
    }

    val masked = combined & ((BigInt(1) << w) - 1)
    masked
  } ensuring (_.bitLength <= w)
  val dump_path = createDumpDirectory()
  def dumpToFile(
      file_name: String,
      content: Array[BigInt]
  ): Path = {

    val fp = dump_path.resolve(file_name)
    val printer = new PrintWriter(fp.toFile())
    printer.print(content mkString ("\n"))
    printer.close()
    fp
  }

  def log2Ceil(x: Int): Int = {
    require(x > 0)
    BigInt(x - 1).bitLength
  }

  def repeat(times: Int)(gen: Int => Unit): Unit = Range(0, times) foreach { i => gen(i) }
}
