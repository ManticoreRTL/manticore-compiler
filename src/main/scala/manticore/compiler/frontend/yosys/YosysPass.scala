package manticore.compiler.frontend.yosys

import manticore.compiler.FunctionalTransformation
import manticore.compiler.AssemblyContext
import scala.language.implicitConversions


trait YosysPass extends FunctionalTransformation[YosysResultProxy, YosysResultProxy] {

  protected def passProxy(implicit ctx: AssemblyContext): YosysPassProxy
  override def apply(current: YosysResultProxy)(implicit
      ctx: AssemblyContext
  ): YosysResultProxy = current :+ passProxy
}

object Implicits {
  implicit def stringToYosysPassProxy(cmd: String) = YosysPassProxy(cmd)
  implicit def passProxyToTransformation(pass: YosysPassProxy) = new YosysPass {
    def passProxy(implicit ctx: AssemblyContext) = pass
  }
}