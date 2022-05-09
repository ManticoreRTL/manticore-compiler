package manticore.compiler.assembly.levels

import manticore.compiler.assembly.ManticoreAssemblyIR

import manticore.compiler.AssemblyContext

import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.CompilationFailureException
import java.nio.file.Files
import java.io.BufferedWriter
import java.io.PrintWriter
import manticore.compiler.HasLoggerId

/** Base transformation signatures, see [[AssemblyTransformer]] and
  * [[AssemblyChecker]] below
  *
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */

case class TransformationID(id: String) extends HasLoggerId

trait HasTransformationID {
  implicit val phase_id = TransformationID(
    getClass().getSimpleName().takeWhile(_ != '$')
  )
}
trait Transformation[
    -S <: ManticoreAssemblyIR#DefProgram,
    +T <: ManticoreAssemblyIR#DefProgram
] extends ((S, AssemblyContext) => (T, AssemblyContext))
    with HasTransformationID {

  def apply(s: S, ctx: AssemblyContext): (T, AssemblyContext)

  def followedBy[R <: ManticoreAssemblyIR#DefProgram](
      g: Transformation[T, R]
  ): Transformation[S, R] = { case (source_ir, source_ctx) =>
    val (target_ir, target_ctx) = this.apply(source_ir, source_ctx)
    g.apply(target_ir, target_ctx)
  }

  def andFinally[R](
      g: (T, AssemblyContext) => R
  ): (S, AssemblyContext) => R = { case (source_ir, source_ctx) =>
    val (target_ir, target_ctx) = this.apply(source_ir, source_ctx)
    g.apply(target_ir, target_ctx)
  }

}

object Transformation {

  def predicated[T <: ManticoreAssemblyIR#DefProgram](
      predicate: => Boolean
  )(original: Transformation[T, T]) = new Transformation[T, T] {

    override def apply(s: T, ctx: AssemblyContext): (T, AssemblyContext) = {
      if (predicate) {
        // ctx.logger.info("Skipping transformation")
        original(s, ctx)
      } else {
        (s, ctx)
      }
    }
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
] extends Transformation[S, T] {

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

    ctx.logger.start(
      s"[${ctx.logger.countProgress()}] Starting transformation ${phase_id}"
    )

    val (res, duration_time) = ctx.stats.scope {
      transform(source, ctx)
    }

    ctx.logger.info(f"Finished after ${duration_time}%.3f ms")
    ctx.logger.dumpArtifact(
      s"dump_post.masm"
    ) {
      res.serialized
    }
    if (ctx.logger.countErrors() > 0) {
      ctx.logger.fail("Compilation failed due to earlier errors")
    }
    ctx.logger.end("")
    (res, ctx)
  }

}

/** Signature class for IR checkers, taking [[T]] IR flavor as input and
  * producing the same program as outpu
  *
  * @param programIr
  */
trait AssemblyChecker[T <: ManticoreAssemblyIR#DefProgram]
    extends Transformation[T, T] {

  /** check the tree and possibly throw an exception if the check failed
    *
    * @param source
    *   the tree to check
    * @param context
    *   compilation context
    */
  @throws(classOf[CompilationFailureException])
  def check(source: T, context: AssemblyContext): Unit

  /** create another checker that only executes if the guard is satisfied
    *
    * @param cond
    */
  def guard(cond: Boolean) =
    Transformation.predicated(cond) { this }


  override final def apply(
      source: T,
      context: AssemblyContext
  ): (T, AssemblyContext) = {
    context.logger.start(
      s"[${context.logger.countProgress()}] Starting program check ${phase_id}"
    )
    check(source, context)

    if (context.logger.countErrors() > 0)
      context.logger.fail(
        s"Checker failed after encountering ${context.logger.countErrors()} errors!"
      )
    context.logger.end("")
    (source, context)
  }
}

trait AssemblyPrinter[T <: ManticoreAssemblyIR#DefProgram]
    extends Transformation[T, T] {

  override def apply(
      source: T,
      ctx: AssemblyContext
  ): (T, AssemblyContext) = {
    ctx.logger.start(
      s"[${ctx.logger.countProgress()}] Printing assembly"
    )

    ctx.output_dir match {
      case Some(dir) =>
        if (!dir.isDirectory())
          Files.createDirectories(dir.toPath())
        val f = dir.toPath().resolve("out.masm").toFile()
        ctx.logger.info(s"Printing assembly to ${f.toPath().toAbsolutePath()}")
        val writer = new PrintWriter(f)
        writer.print(source.serialized)
        writer.close()
      case None =>
        ctx.logger.warn("Output file not specified!")
    }
    ctx.logger.end("")
    (source, ctx)
  }
}

trait Flavored extends HasTransformationID {

  val flavor: ManticoreAssemblyIR

}
