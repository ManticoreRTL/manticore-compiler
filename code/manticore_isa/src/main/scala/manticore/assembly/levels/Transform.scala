package manticore.assembly.levels

import manticore.assembly.ManticoreAssemblyIR

/** Signature class for IR transformation, taking the [[S]] IR flavor as input
  * and producing a [[T]] flavored IR as output
  *
  * @param programIr
  */
abstract class AssemblyTransformer[
    S <: ManticoreAssemblyIR,
    T <: ManticoreAssemblyIR
](
    programIr: S
) extends (S#DefProgram => T#DefProgram)

/** Signature class for IR checkers, taking [[T]] IR flavor as input and
  * producing a results of type [[R]] as output.
  *
  * @param programIr
  */
abstract class AssemblyChecker[
    T <: ManticoreAssemblyIR,
    R
](programIr: T)
    extends (T#DefProgram => R) {
  def apply(prog: T#DefProgram): R
}
