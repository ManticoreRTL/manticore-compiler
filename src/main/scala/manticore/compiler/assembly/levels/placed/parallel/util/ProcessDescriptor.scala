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

private[parallel] case class ProcessorDescriptor(
    inSet: BitSet,
    outSet: BitSet,
    body: BitSet,
    memory: BitSet
) {

  def merged(other: ProcessorDescriptor): ProcessorDescriptor = {
    val newOutSet = other.outSet union outSet
    val newInSet  = (inSet union other.inSet) diff newOutSet
    val newBody   = body union other.body
    ProcessorDescriptor(newInSet, newOutSet, newBody, memory union other.memory)
  }
}

private[parallel] object ProcessorDescriptor {
  def empty = ProcessorDescriptor(BitSet.empty, BitSet.empty, BitSet.empty, BitSet.empty)

}
