package manticore.assembly.levels

import manticore.assembly.ManticoreAssemblyIR
import manticore.assembly.Reporter
import manticore.compiler.AssemblyContext


trait Transformation[
    S <: ManticoreAssemblyIR#DefProgram,
    T <: ManticoreAssemblyIR#DefProgram
] {

  def apply(s: S)(implicit context: AssemblyContext): T
  def followedBy[R <: ManticoreAssemblyIR#DefProgram](
      g: Transformation[T, R]
  )(implicit context: AssemblyContext): S => R = { x =>
    g(apply(x))
  }
}

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
    with Reporter

/** Signature class for IR checkers, taking [[T]] IR flavor as input and
  * producing the same program as outpu
  *
  * @param programIr
  */
abstract class AssemblyChecker[
    T <: ManticoreAssemblyIR
](programIr: T)
    extends Transformation[T#DefProgram, T#DefProgram]
    with Reporter
