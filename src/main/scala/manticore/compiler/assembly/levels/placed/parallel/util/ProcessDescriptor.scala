package manticore.compiler.assembly.levels.placed.parallel.util

import scala.collection.BitSet

/**
  * Companion object for building mutable disjoint sets
  */
private[parallel] object DisjointSets {
  def apply[T](elements: Iterable[T]) = {
    import scala.collection.mutable.{Map => MutableMap, Set => MutableSet}
    val allSets: MutableMap[T, MutableSet[T]] =
      MutableMap.empty[T, MutableSet[T]]
    allSets ++= elements.map { x => x -> MutableSet[T](x) }
    new DisjointSets[T] {
      override def union(a: T, b: T): scala.collection.Set[T] = {
        val setA = allSets(a)
        val setB = allSets(b)
        if (setA(b) || setB(a)) {
          // already joined
          setA
        } else if (setA.size > setB.size) {
          setA ++= setB
          allSets ++= setB.map { _ -> setA }
          setA
        } else {
          setB ++= setA
          allSets ++= setA.map { _ -> setB }
          setB
        }
      }
      override def find(a: T): scala.collection.Set[T] = allSets(a)

      override def sets: Iterable[scala.collection.Set[T]] = allSets.values.toSet

      override def add(a: T): scala.collection.Set[T] = {
        if (!allSets.contains(a)) {
          allSets += (a -> MutableSet[T](a))
        }
        allSets(a)
      }
    }
  }

}

/**
  * A mutable disjoint set
  */
sealed private[parallel] trait DisjointSets[T] {
  def union(first: T, second: T): scala.collection.Set[T]
  def find(a: T): scala.collection.Set[T]
  def sets: Iterable[scala.collection.Set[T]]
  def add(a: T): scala.collection.Set[T]
}

private[parallel] case class ProcessDescriptor(
    inSet: BitSet,  // the set of state elements read
    outSet: BitSet, // the set of state elements owned which may overlap with inSet
    body: BitSet,
    memory: BitSet
) {

  // Not sure if lazy val would help here. It probably trashes performance due
  // the synchronization overhead it incurs.
  // note that outBound is basically useless. Because we can not use it to determine
  // the number of sends a processor should do, because a single variable maybe have
  // to be multi-casted to multiple users. So if in your logic you are relying on
  // getting the "Sends" using outSet diff inSet, you are probably doing something wrong
  // val outBound = outSet diff inSet // avoid this because it only tells you the state elements
  // you own but don't use, surely these values need to be sent out but to how many processors
  // we can not know!
  val inBound = inSet diff outSet // the set of state elements to receive, i.e., state elements
  // owned by another processor
  def merged(other: ProcessDescriptor): ProcessDescriptor = {
    val newOutSet = other.outSet union outSet
    val newInSet  = (inSet union other.inSet)
    val newBody   = body union other.body
    ProcessDescriptor(newInSet, newOutSet, newBody, memory union other.memory)
  }
}

private[parallel] object ProcessDescriptor {
  def empty = ProcessDescriptor(BitSet.empty, BitSet.empty, BitSet.empty, BitSet.empty)

}
