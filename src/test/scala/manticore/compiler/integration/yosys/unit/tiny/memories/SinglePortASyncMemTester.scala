package manticore.compiler.integration.yosys.unit.tiny.memories

import manticore.compiler.UnitFixtureTest
import manticore.compiler.integration.yosys.unit.YosysUnitTest
import manticore.compiler.integration.yosys.unit.CodeText

class SinglePortASyncMemTester extends UnitFixtureTest {

  behavior of "a simple asynch memory in Verilog"

  "manticore interpretation" should "match verilator's" in { f =>
    new YosysUnitTest {
    //   override def dumpAll = true
      val testIterations = 400
      val testDir = f.test_dir
      val code = CodeText {
        s"""|module SimpleAsyncMemory(
                        |   input wire clk,
                        |   input wire [3:0] waddr,
                        |   input wire [3:0] raddr,
                        |   input wire [31:0] dataIn,
                        |   input wire wen,
                        |   output wire [31:0] dataOut
                        |);
                        |
                        |   reg [31:0] mem [0:15];
                        |   assign dataOut = mem[raddr];
                        |   always @(posedge clk) begin
                        |       if (wen) mem[waddr] <= dataIn;
                        |   end
                        |
                        |endmodule
                    """.stripMargin
      }

    }.run()

  }

}
