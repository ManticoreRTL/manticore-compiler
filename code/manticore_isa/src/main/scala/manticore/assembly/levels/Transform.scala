package manticore.assembly.levels

import manticore.assembly.ManticoreAssemblyIR
import manticore.assembly.Reporter
import manticore.compiler.AssemblyContext
import manticore.assembly.CompilationFailureException
import manticore.assembly.levels.unconstrained.UnconstrainedIR

/** Base transformation signatures, see [[AssemblyTransformer]] and
  * [[AssemblyChecker]] below
  *
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */

trait Transformation[
    -S <: ManticoreAssemblyIR#DefProgram,
    +T <: ManticoreAssemblyIR#DefProgram
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



/** Signature class for IR transformation, taking the [[S]] IR flavor as input
  * and producing a [[T]] flavored IR as output
  *
  * @param programIr
  */
trait AssemblyTransformer[
    -S <: ManticoreAssemblyIR#DefProgram,
    +T <: ManticoreAssemblyIR#DefProgram
]  extends Transformation[S, T]
    with Reporter {

  /** transform a tree of type S to T
    *
    * @param source
    * @param context
    * @return
    */

  def transform(source: S, context: AssemblyContext): T
  override final def apply(
      source: S,
      ctx: AssemblyContext
  ): (T, AssemblyContext) = {

    logger.info(s"[${ctx.transform_index}] Starting transformation ${getName}")
    val res = (transform(source, ctx), ctx)
    logger.dumpArtifact(s"dump_post_${ctx.transform_index}_${getName}.masm") {
      res._1.serialized
    }(ctx)
    if (logger.countErrors > 0)
      logger.fail("Compilation failed due to earlier errors")
    ctx.transform_index += 1
    res
  }

}

/** Signature class for IR checkers, taking [[T]] IR flavor as input and
  * producing the same program as outpu
  *
  * @param programIr
  */
trait AssemblyChecker[T <: ManticoreAssemblyIR#DefProgram]
    extends Transformation[T, T]
    with Reporter {

  /** check the tree and possibly throw an exception if the check failed
    *
    * @param source
    *   the tree to check
    * @param context
    *   compilation context
    */
  @throws(classOf[CompilationFailureException])
  def check(source: T, context: AssemblyContext): Unit

  override final def apply(
      source: T,
      context: AssemblyContext
  ): (T, AssemblyContext) = {
    check(source, context)
    if (logger.countErrors > 0)
      logger.fail(s"Checker failed after encountering ${logger.countErrors} errors!")
    (source, context)
  }
}


trait Flavored extends Reporter {

  val flavor: ManticoreAssemblyIR


}