package manticore.compiler.integration

import manticore.compiler.UnitTest
import manticore.compiler.integration.thyrio.ExternalTool

import manticore.compiler.UnitFixtureTest

trait ThyrioUnitTest extends UnitFixtureTest {

  val requiredTools: Seq[ExternalTool]

  def checkInstalled(): Unit =
    requiredTools.foreach { t =>
      val installed = t.installed()
      if (!installed)
        throw new UnsupportedOperationException(s"${t.name} is not installed")

    }




}
