package manticore.compiler.assembly.utils

import manticore.compiler.assembly.levels.UInt16
import Chisel.UInt

class XorShift128 private (prefix: String, seed: Int) {

  def randCurr: String = s"%${prefix}x0curr"
  def randNext: String = s"%${prefix}x0next"

  private val x = Array[Int](
    123456789,
    362436069,
    521288629,
    seed
  )

  def currRef(): Int = x(0)

  def nextRef(): Int = {

    var t = x(3)
    val s = x(0)
    x(3) = x(2)
    x(2) = x(1)
    x(1) = s
    t ^= t << 11
    t ^= t >>> 8
    x(0) = t ^ s ^ (s >>> 19)
    x(0)
  }

  val registers = s"""

    .reg %${prefix}X0 32 .input ${randCurr} ${x(0)} .output ${randNext}
    .reg %${prefix}X1 32 .input %${prefix}x1curr ${x(1)} .output %${prefix}x1next
    .reg %${prefix}X2 32 .input %${prefix}x2curr ${x(2)} .output %${prefix}x2next
    .reg %${prefix}X3 32 .input %${prefix}x3curr ${x(3)} .output %${prefix}x3next

    .const %${prefix}eleven 32 11
    .const %${prefix}eight  32 8
    .const %${prefix}nineteen 32 19


    .wire %${prefix}t 32
    .wire %${prefix}s 32
    .wire %${prefix}l1 32
    .wire %${prefix}l2 32
    .wire %${prefix}l3 32
    .wire %${prefix}l4 32
    .wire %${prefix}l5 32
    .wire %${prefix}l6 32
    .wire %${prefix}l7 32
    .wire %${prefix}l8 32
    .wire %${prefix}l9 32
    .wire %${prefix}l10 32
    .wire %${prefix}l11 32


  """

  val code = s"""

    MOV %${prefix}t, %${prefix}x3curr;
    MOV %${prefix}s, ${randCurr};

    SLL %${prefix}l1, %${prefix}t,  %${prefix}eleven;
    XOR %${prefix}l2, %${prefix}l1, %${prefix}t;
    SRL %${prefix}l3, %${prefix}l2, %${prefix}eight;
    XOR %${prefix}l4, %${prefix}l3, %${prefix}l2;
    SRL %${prefix}l5, %${prefix}s,  %${prefix}nineteen;
    XOR %${prefix}l6, %${prefix}s,  %${prefix}l5;
    XOR %${prefix}l7, %${prefix}l6, %${prefix}l4;
    MOV %${prefix}x3next, %${prefix}x2curr;
    MOV %${prefix}x2next, %${prefix}x1curr;
    MOV %${prefix}x1next, %${prefix}s;
    MOV ${randNext}, %${prefix}l7;

  """

}

object XorShift128 {
  def apply(name: String, seed: Int = 1650957049): XorShift128 =
    new XorShift128(name, seed)
}

class XorShift16 private (prefix: String, seed: UInt16) {

  def randCurr: String = s"%${prefix}x0curr"
  def randNext: String = s"%${prefix}x0next"

  private var x: UInt16 = seed

  def currRef(): UInt16 = x
  def nextRef(): UInt16 = {
    x = x ^ (x << 7)
    x = x ^ (x >> 9)
    x = x ^ (x << 8)
    x
  }

  val registers = s"""

    .reg %${prefix}X0 16 .input ${randCurr} ${x} .output ${randNext}

    .const %${prefix}seven 16 7
    .const %${prefix}eight 16 8
    .const %${prefix}nine  16 9


    .wire %${prefix}l1 16
    .wire %${prefix}l2 16
    .wire %${prefix}l3 16
    .wire %${prefix}l4 16
    .wire %${prefix}l5 16
    .wire %${prefix}l6 16
    .wire %${prefix}l7 16
    .wire %${prefix}l8 16
    .wire %${prefix}l9 16
    .wire %${prefix}l10 16
    .wire %${prefix}l11 16


  """

  val code = s"""

    SLL %${prefix}l1, ${randCurr}, %${prefix}seven;
    XOR %${prefix}l2, %${prefix}l1, ${randCurr};
    SRL %${prefix}l3, %${prefix}l2, %${prefix}nine;
    XOR %${prefix}l4, %${prefix}l3, %${prefix}l2;
    SLL %${prefix}l5, %${prefix}l4, %${prefix}eight;
    XOR ${randNext}, %${prefix}l5, %${prefix}l4;

  """
}

object XorShift16 {

   def apply(name: String, seed: UInt16 = UInt16(1509)): XorShift16 =
    new XorShift16(name, seed)
}
