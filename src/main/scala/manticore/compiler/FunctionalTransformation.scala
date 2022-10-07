package manticore.compiler

trait FunctionalTransformation[-S, +T] {

  def apply(program: S)(implicit ctx: AssemblyContext): T
  def andThen[R](next: FunctionalTransformation[T, R]): FunctionalTransformation[S, R] = {
    val current = this
    new FunctionalTransformation[S, R] {
      def apply(source: S)(implicit ctx: AssemblyContext): R = next.apply(current.apply(source))
    }
  }

}

case class TransformationID(id: String) extends HasLoggerId
trait HasTransformationID {
  implicit val transformId = TransformationID(
    getClass().getSimpleName().takeWhile(_ != '$')
  )
}

