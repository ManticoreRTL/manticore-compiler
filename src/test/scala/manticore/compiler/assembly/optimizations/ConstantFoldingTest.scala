package manticore.compiler.assembly.optimizations

import manticore.compiler.UnitFixtureTest

class ConstantFoldingTest extends UnitFixtureTest {


    behavior of "ConstantFolding"




    "CF" should "remove redundant MUXes" in { fixture =>


        val text = """
        .prog: .proc p0:

            
            .


        """


    }

}