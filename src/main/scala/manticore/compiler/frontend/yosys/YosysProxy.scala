package manticore.compiler.frontend.yosys

import manticore.compiler.AssemblyContext
import manticore.compiler.HasLoggerId
import manticore.compiler.LoggerId
import manticore.compiler.FunctionalTransformation
import manticore.compiler.TransformationID

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import scala.language.implicitConversions

private[yosys] case class YosysPassProxy(
    command: String,
    args: Seq[String] = Nil,
    dependencies: Seq[String] = Nil
) {

  def runsAfter(p: YosysPassProxy) =
    copy(dependencies = dependencies :+ p.command)
  def runsAfter(p: YosysPassProxy*) =
    copy(dependencies = dependencies ++ p.map(_.command))
  def addSwitch(s: String) = copy(args = args :+ s)
  def <<(s: String) = addSwitch(s)
}

case class YosysBackendProxy(
    command: String,
    filename: Path,
    args: Seq[String] = Nil
) {
  def addSwitch(s: String) = copy(args = args :+ s)
  def <<(s: String) = addSwitch(s)
}
private[yosys] case class YosysResultProxy private (
    passes: Seq[YosysPassProxy] = Nil
) {
  def :+(pass: YosysPassProxy) = new YosysResultProxy(passes :+ pass)
}
