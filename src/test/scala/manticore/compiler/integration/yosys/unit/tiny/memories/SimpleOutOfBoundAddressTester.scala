package manticore.compiler.integration.yosys.unit.tiny.memories

import manticore.compiler.UnitFixtureTest
import manticore.compiler.integration.yosys.unit.CodeText
import manticore.compiler.integration.yosys.unit.YosysUnitTest

class SimpleOutOfBoundAddressTester extends UnitFixtureTest {


  behavior of "a simple sync memory in Verilog with out of bound memory accesses"

  "manticore interpretation" should "match verilator's" in { f =>
    new YosysUnitTest {
    //   override def dumpAll = true
      val testIterations = 30000
      val testDir = f.test_dir
      val code = CodeText {
        s"""|module SimpleAsyncMemory(
                        |   input wire clk,
                        |   input wire [3 :0] waddr,
                        |   input wire [3 :0] raddr,
                        |   input wire [7 :0] dataIn,
                        |   input wire wen,
                        |   output reg [7 : 0] dataOut
                        |);
                        |
                        |   reg [7 : 0] mem [8 : 12];
                        |   initial begin
                        |       mem[8] = 8'd8;
                        |       mem[9] = 9;
                        |       mem[10] = 255;
                        |   end
                        |   always @(posedge clk) begin
                        |       if (wen) mem[waddr] <= dataIn;
                        |       dataOut <= mem[raddr];
                        |   end
                        |
                        |endmodule
                    """.stripMargin
      }
      override def dumpAll = true

    }.run()

  }

}