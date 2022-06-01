package manticore.compiler.assembly.levels

import manticore.compiler.assembly.ManticoreAssemblyIR

import manticore.compiler.AssemblyContext

import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.CompilationFailureException
import java.nio.file.Files
import java.io.BufferedWriter
import java.io.PrintWriter
import manticore.compiler.HasLoggerId
import manticore.compiler.FunctionalTransformation

case class TransformationID(id: String) extends HasLoggerId
trait HasTransformationID {
  implicit val transformId = TransformationID(
    getClass().getSimpleName().takeWhile(_ != '$')
  )
}

/** Base transformation signatures, see [[AssemblyTransformer]] and
  * [[AssemblyChecker]] below
  *
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */

trait CompilerTransformation[
    S <: ManticoreAssemblyIR#DefProgram,
    T <: ManticoreAssemblyIR#DefProgram
] extends FunctionalTransformation[S, T]
    with HasTransformationID {

  def transform(program: S)(implicit ctx: AssemblyContext): T

  protected def dumpResults(res: T)(implicit ctx: AssemblyContext): Unit = {
    ctx.logger.dumpArtifact(
      s"dump_post.masm"
    ) {
      res.serialized
    }
  }

  protected final def do_apply(program: S)(implicit ctx: AssemblyContext): T = {
    ctx.logger.start(
      s"[${ctx.logger.countProgress()}] Starting transformation ${transformId}"
    )

    val (res, runDuration) = ctx.stats.scope {
      transform(program)
    }

    ctx.logger.info(f"Finished after ${runDuration}%.3f ms")

    dumpResults(res)

    if (ctx.logger.countErrors() > 0) {
      ctx.logger.fail("Compilation failed due to earlier errors")
    }

    ctx.logger.end("")
    res
  }
  override def apply(program: S)(implicit ctx: AssemblyContext): T = do_apply(
    program
  )

}

/** Signature class for IR transformation that does not change the flavor
  * @param programIr
  */
trait AssemblyTransformer[T <: ManticoreAssemblyIR#DefProgram]
    extends CompilerTransformation[T, T] {

  // we specialize andThen for this trait so that sub-types of AssemblyTransformer
  // of some type T remain a sub-type of AssemblyTransformer[T]. If we do not
  // do this then the generic andThen[R] of FunctionalTransformation kicks in
  // and loses the extra typing information. This means then we would not be
  // able to call withCondition on chained transformations.
  def andThen(other: AssemblyTransformer[T]) = {
    val thisTransform = this
    new AssemblyTransformer[T] {
      override implicit val transformId: TransformationID = other.transformId
      override def transform(program: T)(implicit ctx: AssemblyContext): T = {
        throw new UnsupportedOperationException(
          "Can not call this method on a combined transform"
        )
      }
      override def apply(t: T)(implicit ctx: AssemblyContext): T = {
        type ParentType = CompilerTransformation[T, T]
        val r = thisTransform.apply(t)
        other.apply(r)
      }
    }
  }

  final def withCondition(cond: => Boolean) = {
    val thisTransform = this
    new AssemblyTransformer[T] {
      override implicit val transformId: TransformationID =
        thisTransform.transformId
      override def apply(program: T)(implicit ctx: AssemblyContext) = {
        if (cond) {
          thisTransform.apply(program)
        } else {
          program
        }
      }
      override def transform(p: T)(implicit ctx: AssemblyContext) =
        throw new UnsupportedOperationException(
          "Can not call this method on a combined transform"
        )

    }
  }
  /**
    * create a new transformation that only executes if the given condition
    * holds
    *
    * @param cond
    * @return
    */
  final def ?(cond: => Boolean) = withCondition(cond)
}

// base trait for changing flavor
trait AssemblyTranslator[
    S <: ManticoreAssemblyIR#DefProgram,
    T <: ManticoreAssemblyIR#DefProgram
] extends CompilerTransformation[S, T] {}

/** Signature class for IR checkers, taking [[T]] IR flavor as input and
  * producing the same program as outpu
  *
  * @param programIr
  */
trait AssemblyChecker[T <: ManticoreAssemblyIR#DefProgram]
    extends AssemblyTransformer[T] {

  /** check the tree and possibly throw an exception if the check failed
    *
    * @param source
    *   the tree to check
    * @param context
    *   compilation context
    */
  @throws(classOf[CompilationFailureException])
  def check(source: T)(implicit context: AssemblyContext): Unit

  final def transform(program: T)(implicit ctx: AssemblyContext): T = {
    check(program)
    program
  }

  // don't dump
  override protected def dumpResults(res: T)(implicit
      ctx: AssemblyContext
  ): Unit = {}

  def andThen(other: AssemblyChecker[T]) = {
    val thisTransform = this
    new AssemblyChecker[T] {
      override implicit val transformId: TransformationID = other.transformId
      override def check(source: T)(implicit context: AssemblyContext): Unit =
        throw new UnsupportedOperationException(
          "Can not call this method on a combined transform"
        )
      override def apply(t: T)(implicit ctx: AssemblyContext): T = {
        val r = thisTransform.apply(t)
        other.apply(r)
      }
    }
  }
}

trait AssemblyPrinter[T <: ManticoreAssemblyIR#DefProgram]
    extends AssemblyChecker[T] {

  override def check(
      source: T
  )(implicit ctx: AssemblyContext): Unit = {
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
