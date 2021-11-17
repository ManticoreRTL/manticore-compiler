package manticore.assembly.levels


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

  override def toString(): String = v.toString()
}

object UInt16 {
  private def clipped(v: Int): UInt16 = UInt16(v & ((1 << 16) - 1))
  def apply(v: Int): UInt16 = {
    require(v < (1 << 16))
    new UInt16(v)
  }
  def unapply(v: UInt16): Option[Int] = Some(v.toInt)
}