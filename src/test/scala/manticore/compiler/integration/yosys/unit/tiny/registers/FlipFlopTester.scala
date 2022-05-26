package manticore.compiler.integration.yosys.unit.tiny.registers
import manticore.compiler.UnitFixtureTest
import manticore.compiler.integration.yosys.unit.TestCode
import manticore.compiler.integration.yosys.unit.CodeText
import manticore.compiler.integration.yosys.unit.YosysUnitTest
import org.scalatest.CancelAfterFailure
import manticore.compiler.frontend.yosys.Yosys

trait FlipFlopTester extends UnitFixtureTest with CancelAfterFailure {

  def name: String
  def generate(width: Int): TestCode
  behavior of s"$name"

  val randGen = new scala.util.Random(0)
  val randomWidth =
    (Seq(1, 2, 3, 15, 16, 17) ++ Seq.fill(4) { randGen.nextInt(70) }).distinct
  randomWidth.foreach { w =>
    s"$name[$w - 1 : 0]" should "match verilator resutls" in { f =>
      import manticore.compiler.frontend.yosys.Implicits._
      new YosysUnitTest {
        val testIterations = 300
        val code = generate(w)
        val testDir = f.test_dir

        override def yosysSelection = Seq(
          Yosys.Select << "-asser-any" << s"t:$name"
        )
      }.run()
    }
  }
}


class DffTester extends FlipFlopTester {

    override def name = "$dff"
    override def generate(width: Int) = CodeText {
    s"""|
        |module DffUnit(
        |   input wire clk,
        |   input wire [$width - 1 : 0] din,
        |   output reg [$width - 1 : 0] dout);
        |   initial begin dout = ${randGen.nextInt()}; end
        |   always @(posedge clk) dout <= din;
        |endmodule
        |""".stripMargin
    }


}

class SynchResetDffTester extends FlipFlopTester {

    override def name = "$sdff"
    override def generate(width: Int) = CodeText {
        s"""|
            |module SyncResetUnit(
            |   input wire clk,
            |   input wire srst,
            |   input wire srst_n,
            |   input      [$width - 1 : 0] din,
            |   output reg [$width - 1 : 0] dout,
            |   input      [$width - 1 : 0] din_n,
            |   output reg [$width - 1 : 0] dout_n
            |);
            |   initial begin dout = ${randGen.nextInt()}; end
            |   initial begin dout_n = ${randGen.nextInt()}; end
            |   always @(posedge clk) begin
            |       if (srst == 1'b1) dout <= ${randGen.nextInt((1 << 31) - 1)};
            |       else dout <= din;
            |   end
            |   always @(posedge clk) begin
            |       if (srst_n == 1'b0) dout_n <= ${randGen.nextInt((1 << 31) - 1)};
            |       else dout_n <= din_n;
            |   end
            |endmodule
            |""".stripMargin
    }
}

class SyncResetWriteEnableDffTester extends FlipFlopTester {

    override def name = "$sdffe"

    override def generate(width: Int) = CodeText {
        s"""|
            |module SyncResetUnit(
            |   input wire clk,
            |   input wire srst,
            |   input wire en,
            |   input      [$width - 1 : 0] din_pp,
            |   output reg [$width - 1 : 0] dout_pp,
            |   input      [$width - 1 : 0] din_pn,
            |   output reg [$width - 1 : 0] dout_pn,
            |   input      [$width - 1 : 0] din_np,
            |   output reg [$width - 1 : 0] dout_np,
            |   input      [$width - 1 : 0] din_nn,
            |   output reg [$width - 1 : 0] dout_nn
            |);
            |   initial begin dout_pp = ${randGen.nextInt()}; end
            |   initial begin dout_pn = ${randGen.nextInt()}; end
            |   initial begin dout_np = ${randGen.nextInt()}; end
            |   initial begin dout_nn = ${randGen.nextInt()}; end
            |   always @(posedge clk) begin
            |       if (srst == 1'b1)
            |           dout_pp <= ${randGen.nextInt((1 << 31) - 1)};
            |       else if (en == 1'b1) dout_pp <= din_pp;
            |   end
            |   always @(posedge clk) begin
            |       if (srst == 1'b0)
            |           dout_np <= ${randGen.nextInt((1 << 31) - 1)};
            |       else if (en == 1'b1) dout_np <= din_np;
            |   end
            |   always @(posedge clk) begin
            |       if (srst == 1'b1)
            |           dout_pn <= ${randGen.nextInt((1 << 31) - 1)};
            |       else if (en == 1'b0) dout_pn <= din_pn;
            |   end
            |   always @(posedge clk) begin
            |       if (srst == 1'b0)
            |           dout_nn <= ${randGen.nextInt((1 << 31) - 1)};
            |       else if (en == 1'b0) dout_nn <= din_nn;
            |   end
            |endmodule
            |""".stripMargin
    }

}


class SyncResetChipEnableDffTester extends FlipFlopTester {

    override def name = "$sdffce"

    override def generate(width: Int) = CodeText {
        s"""|
            |module SyncResetUnit(
            |   input wire clk,
            |   input wire srst,
            |   input wire en,
            |   input      [$width - 1 : 0] din_pp,
            |   output reg [$width - 1 : 0] dout_pp,
            |   input      [$width - 1 : 0] din_pn,
            |   output reg [$width - 1 : 0] dout_pn,
            |   input      [$width - 1 : 0] din_np,
            |   output reg [$width - 1 : 0] dout_np,
            |   input      [$width - 1 : 0] din_nn,
            |   output reg [$width - 1 : 0] dout_nn
            |);
            |   initial begin dout_pp = ${randGen.nextInt()}; end
            |   initial begin dout_pn = ${randGen.nextInt()}; end
            |   initial begin dout_np = ${randGen.nextInt()}; end
            |   initial begin dout_nn = ${randGen.nextInt()}; end
            |   always @(posedge clk) begin
            |       if  (en == 1'b1) begin
            |           if (srst == 1'b1)
            |               dout_pp <= ${randGen.nextInt((1 << 31) - 1)};
            |           else  dout_pp <= din_pp;
            |       end
            |   end
            |   always @(posedge clk) begin
            |       if  (en == 1'b0) begin
            |           if (srst == 1'b1)
            |               dout_pn <= ${randGen.nextInt((1 << 31) - 1)};
            |           else  dout_pn <= din_pn;
            |       end
            |   end
            |   always @(posedge clk) begin
            |       if  (en == 1'b1) begin
            |           if (srst == 1'b0)
            |               dout_np <= ${randGen.nextInt((1 << 31) - 1)};
            |           else  dout_np <= din_np;
            |       end
            |   end
            |   always @(posedge clk) begin
            |       if  (en == 1'b0) begin
            |           if (srst == 1'b0)
            |               dout_nn <= ${randGen.nextInt((1 << 31) - 1)};
            |           else  dout_nn <= din_nn;
            |       end
            |   end
            |
            |endmodule
            |""".stripMargin
    }

}


class AsyncResetDffTester extends FlipFlopTester {
    override def name: String = "$adff"

    override def generate(width: Int) = CodeText {
        s"""|
            |module SyncResetUnit(
            |   input wire clk,
            |   input wire arst,
            |   input      [$width - 1 : 0] din,
            |   output reg [$width - 1 : 0] dout,
            |   input      [$width - 1 : 0] din_n,
            |   output reg [$width - 1 : 0] dout_n
            |);
            |   initial begin dout = ${randGen.nextInt()}; end
            |   initial begin dout_n = ${randGen.nextInt()}; end
            |   always @(posedge clk or posedge arst) begin
            |       if (arst == 1'b1) dout <= ${randGen.nextInt((1 << 31) - 1)};
            |       else dout <= din;
            |   end
            |   always @(posedge clk or negedge arst) begin
            |       if (arst == 1'b0) dout_n <= ${randGen.nextInt((1 << 31) - 1)};
            |       else dout_n <= din_n;
            |   end
            |endmodule
            |""".stripMargin
    }


}
class AsyncResetWriteEnableDffTester extends FlipFlopTester {

    override def name = "$adffe"

    override def generate(width: Int) = CodeText {
        s"""|
            |module SyncResetUnit(
            |   input wire clk,
            |   input wire arst,
            |   input wire en,
            |   input      [$width - 1 : 0] din_pp,
            |   output reg [$width - 1 : 0] dout_pp,
            |   input      [$width - 1 : 0] din_pn,
            |   output reg [$width - 1 : 0] dout_pn,
            |   input      [$width - 1 : 0] din_np,
            |   output reg [$width - 1 : 0] dout_np,
            |   input      [$width - 1 : 0] din_nn,
            |   output reg [$width - 1 : 0] dout_nn
            |);
            |   always @(posedge clk or posedge arst) begin
            |       if (arst == 1'b1)
            |           dout_pp <= ${randGen.nextInt((1 << 31) - 1)};
            |       else if (en == 1'b1) dout_pp <= din_pp;
            |   end
            |   always @(posedge clk or negedge arst) begin
            |       if (arst == 1'b0)
            |           dout_np <= ${randGen.nextInt((1 << 31) - 1)};
            |       else if (en == 1'b1) dout_np <= din_np;
            |   end
            |   always @(posedge clk or posedge arst) begin
            |       if (arst == 1'b1)
            |           dout_pn <= ${randGen.nextInt((1 << 31) - 1)};
            |       else if (en == 1'b0) dout_pn <= din_pn;
            |   end
            |   always @(posedge clk or negedge arst) begin
            |       if (arst == 1'b0)
            |           dout_nn <= ${randGen.nextInt((1 << 31) - 1)};
            |       else if (en == 1'b0) dout_nn <= din_nn;
            |   end
            |endmodule
            |""".stripMargin
    }


}
