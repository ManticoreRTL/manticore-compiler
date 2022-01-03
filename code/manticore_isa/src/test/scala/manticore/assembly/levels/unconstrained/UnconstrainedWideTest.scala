package manticore.assembly.levels.unconstrained

import manticore.UnconstrainedTest

trait UnconstrainedWideTest extends UnconstrainedTest {


  lazy val randgen = new scala.util.Random(0)
  def mkWideRand(w: Int): BigInt = {
    val shorts = Seq.fill((w - 1) / 16 + 1) { randgen.nextInt(1 << 16) }

    val combined = shorts.foldLeft(BigInt(0)) { case (c, x) =>
      (c << 16) | BigInt(x)
    }

    val masked = combined & ((BigInt(1) << w) - 1)
    masked
  }

}