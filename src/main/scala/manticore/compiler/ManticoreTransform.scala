package manticore.compiler

trait ManticoreTransform[-S, +T] {

  def apply(program: S)(implicit ctx: AssemblyContext): T
  def andThen[R](next: ManticoreTransform[T, R]): ManticoreTransform[S, R] = {
    val current = this
    new ManticoreTransform[S, R] {
      def apply(program: S)(implicit ctx: AssemblyContext): R = next.apply(current.apply(program))
    }
  }

}


