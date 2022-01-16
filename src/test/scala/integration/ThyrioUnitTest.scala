package integration

import manticore.UnitTest
import integration.thyrio.integration.thyrio.ExternalTool


trait ThyrioUnitTest extends UnitTest {


    val requiredTools: Seq[ExternalTool]

    def checkInstalled(): Unit =
        requiredTools.foreach { t =>
            val installed = t.installed()
            if (!installed)
                throw new UnsupportedOperationException(s"${t.name} is not installed")

        }



}