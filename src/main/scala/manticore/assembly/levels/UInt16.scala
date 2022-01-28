package manticore.assembly.levels

/**
  * UInt16 unboxed class wich represents Manticore-native 16 bit numbers
  * @author Mahyar Emami <mahyar.emami@epfl.ch>
  *
  *
  * This class should not be constructed with [[new]] rather the factory method
  * provided in the `UInt16` object should be used. See below.
  *
  * @param v a value that fits within 16 bits
  */
final class UInt16 private (private val v: Int) extends AnyVal {

  def toInt: Int = v
  def +(that: UInt16): UInt16 = UInt16.clipped(this.v + that.v)
  def -(that: UInt16): UInt16 = UInt16.clipped(this.v - that.v)
  def *(that: UInt16): UInt16 = UInt16.clipped(this.v * that.v)

  def &(that: UInt16): UInt16 =
    UInt16.clipped(this.v & that.v) // not really required to clip it
  def |(that: UInt16): UInt16 =
    UInt16.clipped(this.v | that.v) // not really required to clip it

  def <(that: UInt16): Boolean = this.v < that.v
  def <=(that: UInt16): Boolean = this.v <= that.v
  def >(that: UInt16): Boolean = this.v > that.v
  def >=(that: UInt16): Boolean = this.v >= that.v
  def ==(that: UInt16): Boolean = this.v == that.v

  /**
    * Logical left shift
    *
    * @param shamnt
    * @return
    */
  def <<(shamnt: Int): UInt16 = {
    require(shamnt < 16)
    UInt16.clipped(this.v << shamnt)
  }

  /**
    * Logical right shift
    * @param shamnt
    * @return
    */
  def >>(shamnt: Int): UInt16 = {
    require(shamnt < 16)
    UInt16.clipped(this.v >> shamnt)
  }

  /**
    * Arithmetic right shift
    *
    * @param shamnt
    * @return
    */
  def >>>(shamnt: Int): UInt16 = {
    require(shamnt < 16)
    val sign = this.v >> 15
    val sign_extension = if (sign == 1) (((1 << 16) - 1) << 16) else 0
    val extended = this.v | sign_extension
    UInt16.clipped(extended >>> shamnt)
  }
  override def toString(): String = v.toString()
}


/**
  * Factory object for UInt16
  */
object UInt16 {
  private def clipped(v: Int): UInt16 = UInt16(v & ((1 << 16) - 1))

  /**
    * Create a new [[UInt16]] value given [[v]].
    *
    * @param v a 16 bit number
    * @return Wrapped 16-bit number
    */
  def apply(v: Int): UInt16 = {
    require(v < (1 << 16))
    new UInt16(v)
  }


  def unapply(v: UInt16): Option[Int] = Some(v.toInt)
}