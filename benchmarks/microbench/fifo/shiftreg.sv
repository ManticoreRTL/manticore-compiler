
module ShiftRegisterFIFO #(
    WIDTH = 32,
    DEPTH = 3
) (
    input wire clk,
    input wire wen,
    input wire [WIDTH - 1 : 0] din,
    output wire [WIDTH - 1 : 0] dout

);

  logic [WIDTH - 1 : 0] regs[0 : DEPTH - 1];
  assign dout = regs[DEPTH-1];
  int i;
  always @(posedge clk) begin
    if (wen) begin
      regs[0] <= din;
      for (i = 1; i < DEPTH; i += 1) begin
        regs[i] <= regs[i-1];
      end
    end
  end

endmodule

module ShiftRegisterFIFOTester (
    input wire clk
);


  localparam WIDTH = 16;
  localparam DEPTH = 4;


//   logic [WIDTH - 1 : 0] rom[0 : DEPTH - 1];

  logic [15 : 0] cycle_counter = 0;



  wire reading;
  wire [WIDTH - 1 : 0] din = 16'h3456;
  wire  [WIDTH - 1 : 0] dout;


  ShiftRegisterFIFO #(
      .WIDTH(WIDTH),
      .DEPTH(DEPTH)
  ) fifo (
      .clk (clk),
      .wen (1'b1),
      .din (din),
      .dout(dout)
  );





  always @(posedge clk) begin
    if (cycle_counter == DEPTH) begin
`ifdef VERILATOR
      assert(din == dout);
`else
      $masm_expect(din == dout, "wrong result");
`endif
    end else if (cycle_counter == DEPTH + 1) begin
`ifdef VERILATOR
      $finish;
`else
      $masm_stop;
`endif
    end
    cycle_counter <= cycle_counter + 1;
  end


endmodule
