package manticore.assembly.levels

import manticore.assembly.ManticoreAssemblyIR
import manticore.assembly.Reporter
import manticore.compiler.AssemblyContext
import manticore.assembly.CompilationFailureException

trait OtherT[
    S <: ManticoreAssemblyIR#DefProgram,
    T <: ManticoreAssemblyIR#DefProgram
] extends ((S, AssemblyContext) => (T, AssemblyContext)) {

  def apply(s: S, ctx: AssemblyContext): (T, AssemblyContext)
}

trait MyFunction[S, T] extends ((S, Int) => (T, Int)) {

  def apply(s: S, x: Int): (T, Int)

  def followedBy[R](g: MyFunction[T, R]): MyFunction[S, R] = { case (s, x) =>
    val (t, xx) = apply(s, x)
    g.apply(t, xx)
  }
}

// object MyFunTester extends App {

//   object F1 extends MyFunction[Int, Int] {
//     def apply(s: Int, x: Int) = (s, 1 + x)
//   }

//   object F2 extends MyFunction[Int, BigInt] {
//     def apply(s: Int, x: Int) = (BigInt(s), 1 + x)
//   }

//   object F3 extends MyFunction[BigInt, String] {
//     def apply(s: BigInt, x: Int) = (s.toString() + ".toString", x + 1)
//   }

//   val phases = List(F1, F2, F3)

//   // def comp = F1 followedBy F2 followedBy F3

//   println(comp(1, 1))

// }

/** Base transformation signatures, see [[AssemblyTransformer]] and
  * [[AssemblyChecker]] below
  *
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */

trait Transformation[
    S <: ManticoreAssemblyIR#DefProgram,
    T <: ManticoreAssemblyIR#DefProgram
] extends ((S, AssemblyContext) => (T, AssemblyContext)) {

  // def transform(s: S)(implicit ctx: AssemblyContext): T
  def apply(s: S, ctx: AssemblyContext): (T, AssemblyContext)
  // (transform(s)(ctx), ctx)
  def followedBy[R <: ManticoreAssemblyIR#DefProgram](
      g: Transformation[T, R]
  ): Transformation[S, R] = { case (source_ir, source_ctx) =>
    val (target_ir, target_ctx) = this.apply(source_ir, source_ctx)
    g.apply(target_ir, target_ctx)
  }

}

// trait Haha[
//     S <: ManticoreAssemblyIR#DefProgram,
//     T <: ManticoreAssemblyIR#DefProgram
// ] extends Transformation[S, T] {

//   def transform(s: S)(implicit ctx: AssemblyContext): T
//   def apply(s: S, ctx: AssemblyContext): (T, AssemblyContext) =
//     (transform(s)(ctx), ctx)
// }

/** Signature class for IR transformation, taking the [[S]] IR flavor as input
  * and producing a [[T]] flavored IR as output
  *
  * @param programIr
  */
abstract class AssemblyTransformer[
    S <: ManticoreAssemblyIR,
    T <: ManticoreAssemblyIR
](source: S, target: T)
    extends Transformation[S#DefProgram, T#DefProgram]
    with Reporter {

  /**
    * transform a tree of type S to T
    *
    * @param source
    * @param context
    * @return
    */
    
  def transform(source: S#DefProgram, context: AssemblyContext): T#DefProgram
  override final def apply(
      source: S#DefProgram,
      ctx: AssemblyContext
  ): (T#DefProgram, AssemblyContext) =
    (transform(source, ctx), ctx)
}

/** Signature class for IR checkers, taking [[T]] IR flavor as input and
  * producing the same program as outpu
  *
  * @param programIr
  */
abstract class AssemblyChecker[
    T <: ManticoreAssemblyIR
](programIr: T)
    extends Transformation[T#DefProgram, T#DefProgram]
    with Reporter {

  /**
    * check the tree and possibly throw an exception if the check failed
    *
    * @param source the tree to check
    * @param context compilation context
    */
  @throws(classOf[CompilationFailureException])
  def check(source: T#DefProgram, context: AssemblyContext): Unit 

  override final def apply(
      source: T#DefProgram,
      context: AssemblyContext
  ): (T#DefProgram, AssemblyContext) = {
    check(source, context)
    (source, context)
  }
}
