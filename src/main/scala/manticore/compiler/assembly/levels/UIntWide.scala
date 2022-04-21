package manticore.compiler.assembly.levels

final class UIntWide private (private val v: BigInt, val width: Int) {

  def toInt: Int = v.toInt
  def toIntChecked: Int = {
    require(
      v.isValidInt || (width <= 32 && v <= BigInt(0xFFFFFFFFL)),
      s"${v} is not a valid int"
    )
    v.toInt
  }

  def toBigInt = v

  override def toString(): String = v.toString()
  def toString(radix: Int): String = v.toString(radix)

  def toShort: Short = v.toShort
  def toShortChecked: Short = {
    require(
      v.isValidShort || (width <= 16 && v <= BigInt(0xFFFF)),
      s"${v} is not a valid short"
    )
    v.toShort
  }

  private def maxW(that: UIntWide): Int = this.width.max(that.width)

  def +(that: UIntWide): UIntWide = {
    UIntWide.clipped(this.v + that.v, maxW(that))
  }
  def -(that: UIntWide): UIntWide = {
    require(this.width == that.width, "Can not perform non-aligned subtraction")
    // note that clipping ensure the result is represented as a positive number
    // even if the [[that]] is larger
    UIntWide.clipped(this.v - that.v, maxW(that))
  }
  def |(that: UIntWide): UIntWide = {
      require(this.width == that.width, "Can not perform non-aligned |")
      UIntWide.clipped(this.v | that.v, maxW(that))
  }
  def &(that: UIntWide): UIntWide = {
      require(this.width == that.width, "Can not perform non-aligned &")
      UIntWide.clipped(this.v & that.v, maxW(that))
  }
  def ^(that: UIntWide): UIntWide = {
      require(this.width == that.width, "Can not perform non-aligned ^")
      UIntWide.clipped(this.v ^ that.v, maxW(that))
  }

  def ==(that: UIntWide): Boolean = this.v == that.v
  def ==(that: Int): Boolean = this.v == that

  def <(that: UIntWide): Boolean = this.v < that.v
  def <(that: Int): Boolean = this.v < that


  def <=(that: UIntWide): Boolean = this.v <= that.v
  def <=(that: Int): Boolean = this.v <= that


  def >(that: UIntWide): Boolean = this.v > that.v
  def >(that: Int): Boolean = this.v > that

  def >=(that: UIntWide): Boolean = this.v >= that.v
  def >=(that: Int): Boolean = this.v >= that

  def unary_~ = UIntWide.clipped(~v, width)

  /**
    * Logical left shit
    *
    * @param shamnt
    * @return
    */
  def <<(shamnt: Int): UIntWide = {
      require(shamnt < width)
      UIntWide.clipped(v << shamnt, width)
  }

  /**
    * Logical right shift
    *
    * @param shamnt
    * @return
    */
  def >>(shamnt: Int): UIntWide = {
      require(shamnt < width)
      UIntWide.clipped(v >> shamnt, width)
  }

  /**
    * Arithmetic right shift
    *
    * @param shamnt
    * @return
    */
  def >>>(shamnt: Int): UIntWide = {
    require(shamnt < width)
    val sign = v >> (width - 1)
    if (sign == 1) {
        val wideMask = UIntWide.clipMask(width)
        val resultMask = UIntWide.clipMask(width - shamnt)
        val resultRaw = v >> shamnt
        val resultMasked = (wideMask - resultMask) | resultRaw
        UIntWide.clipped(resultMasked, width)
    } else {
        this >> shamnt
    }
  }


}

object UIntWide {

  def constructible(v: BigInt, w: Int): Boolean = {
    v <= clipMask(w) && v > 0
  }
  def clipMask(w: Int): BigInt = (BigInt(1) << w) - BigInt(1)
  def clipped(v: BigInt, w: Int): UIntWide = UIntWide(v & (clipMask(w)), w)
  def apply(v: BigInt, w: Int): UIntWide = {
    require(v <= clipMask(w), s"${v} does not fit in ${w} bits")
    require(v >= BigInt(0), s"${v} is negative!")
    new UIntWide(v, w)
  }

  def apply(v: Int, w: Int): UIntWide = apply(BigInt(v), w)


  def apply(v: BigInt): UIntWide = {
    require(v > 0, s"${v} is negative!")
    if (v == 0) {
      apply(v, 1)
    } else {
      apply(v, v.bitLength)
    }
  }
  // do not include the width in unapply to be able to
  // easily pattern match constants without dealing with
  // width differences
  def unapply(v: UIntWide) = Some(v.v)

}
