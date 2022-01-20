package manticore.assembly.levels.unconstrained

import manticore.UnconstrainedTags
import manticore.UnitFixtureTest

import java.nio.file.Path
import java.io.PrintWriter
import manticore.assembly.levels.unconstrained.width.WidthConversionCore
import manticore.UnitFixtureTest
trait UnconstrainedWideTest extends UnitFixtureTest  with UnconstrainedTags {

  lazy val randgen = new scala.util.Random(0)
  def mkWideRand(w: Int): BigInt = {
    val shorts = Seq.fill((w - 1) / 16 + 1) { randgen.nextInt(1 << 16) }

    val combined = shorts.foldLeft(BigInt(0)) { case (c, x) =>
      (c << 16) | BigInt(x)
    }

    val masked = combined & ((BigInt(1) << w) - 1)
    masked
  } ensuring (_.bitLength <= w)

  // def dumpToFile(
  //     file_name: String,
  //     content: Array[BigInt]
  // )(implicit f: FixtureParam): Path = {

  //   val fp = f.test_dir.resolve(file_name)
  //   val printer = new PrintWriter(fp.toFile())
  //   printer.print(content mkString ("\n"))
  //   printer.close()
  //   fp
  // }

  def log2Ceil(x: Int): Int = {
    require(x > 0)
    BigInt(x - 1).bitLength
  }

  def repeat(times: Int)(gen: Int => Unit): Unit = Range(0, times) foreach { i => gen(i) }

  val backend =
    WidthConversionCore followedBy UnconstrainedInterpreter
}

