package manticore.compiler.assembly.levels.placed.lowering.util

case class Interval(start: Int, end: Int) {
  require(end >= start, s"invalid interval [${start}, ${end})")
  // def isEmpty: Boolean = start == end
}
object Interval {
  def from(start: Int): Interval = Interval(start, Int.MaxValue)
}

// a set of interval (could be disjoint)
final class IntervalSet private (private val orderedSets: Seq[Interval]) {

  def nonEmpty: Boolean = orderedSets.nonEmpty
  def isEmpty: Boolean = orderedSets.isEmpty
  def isSingleton: Boolean = orderedSets.length == 1

  def map[T](fn: Interval => T) = orderedSets.map(fn)

  def min: Int = {
    require(nonEmpty, "Can not take the start interval of an empty set")
    orderedSets.head.start
  }
  def max: Int = {
    require(nonEmpty, "Can not take the end interval of an empty set")
    orderedSets.last.end
  }
  def covers(cycle: Int): Boolean = {
    def check(xs: Seq[Interval]): Boolean = xs match {
      case Interval(start, end) +: tail =>
        if (cycle >= start && cycle < end) true
        else check(tail)
      case Nil => false
    }
    check(orderedSets)
  }

  // union
  def |(other: Interval): IntervalSet = {
    val (lo, hi) = orderedSets.span { case Interval(start, end) =>
      start <= other.start
    }
    new IntervalSet(IntervalSet.merge((lo :+ other) ++ hi))
  }

  // intersection
  def &(interval: Interval): IntervalSet = {
    def intersect(parts: Seq[Interval]): Seq[Interval] = parts match {
      case Interval(start, end) +: tail =>
        if (end < interval.start) {
          intersect(tail)
        } else { // end >= interval.start
          if (interval.end < start) {
            Nil
          } else { // interval.end >= start && interval.start <= end
            Interval(
              start max interval.start,
              end min interval.end
            ) +: intersect(tail)
          }
        }
      case Nil => Nil
    }
    new IntervalSet(intersect(orderedSets))
  }

  def &(otherSet: IntervalSet): IntervalSet = {

    val parts = otherSet.orderedSets.map(this & _).flatMap(_.orderedSets)
    if (parts.isEmpty) {
      IntervalSet.empty
    } else {
      parts.tail.foldLeft(IntervalSet(parts.head)) { _ | _ }
    }
  }

  def trimStart(start: Int): IntervalSet = {
    if (nonEmpty && start <= orderedSets.last.end) {
      this & Interval(start, orderedSets.last.end)
    } else {
      IntervalSet.empty
    }
  }

  override def toString: String =
    orderedSets.map { i => s"[${i.start}, ${i.end})" } mkString (" | ")

}
object IntervalSet {

  protected def merge(sorted: Seq[Interval]): Seq[Interval] = {
    require(sorted.nonEmpty)
    import scala.collection.mutable.Stack
    val intervalStack = Stack(sorted.head)
    def doMerge(intervals: Seq[Interval]): Seq[Interval] = {

      assert(intervalStack.nonEmpty)
      val tos = intervalStack.head
      intervals match {
        case (i @ Interval(start, end)) +: tail =>
          if (tos.end < start) {
            // non-overlapping
            intervalStack push i
          } else {
            // tos.end >= start
            intervalStack.pop()
            val maxEnd = end max tos.end
            if (maxEnd == tos.start) {
              intervalStack
            } else {
              intervalStack push Interval(tos.start, end max tos.end)
            }
          }
          doMerge(tail)
        case Nil =>
          intervalStack.toSeq.reverse
      }
    }
    doMerge(sorted.tail)
  }

  def empty: IntervalSet = new IntervalSet(Seq.empty)
  def apply(start: Int, end: Int): IntervalSet = apply(Interval(start, end))
  def apply(interval: Interval): IntervalSet =
    if (interval.start == interval.end) {
      empty
    } else {
      new IntervalSet(Seq(interval))
    }

}
