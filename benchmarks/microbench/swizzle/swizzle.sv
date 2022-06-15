module Swizzle #(
    WIDTH = 8
) (
    input wire [WIDTH - 1 : 0] IN,
    output wire [WIDTH - 1 : 0] OUT
);

  logic [WIDTH - 1 : 0] tmp;

  genvar i;
  generate
    for (i=0; i<WIDTH; i=i+1) begin : ff_gen
      assign tmp[i] = IN[WIDTH-1-i];
    end
  endgenerate

  assign OUT = tmp;

endmodule


module Main (
    input wire clock
);

  localparam W = 8;
  localparam TEST_SIZE = 50;

  logic [    W - 1 : 0] in_rom        [0 : TEST_SIZE - 1];
  logic [    W - 1 : 0] out_rom       [0 : TEST_SIZE - 1];

  wire  [    W - 1 : 0] in_val;
  wire  [    W - 1 : 0] out_val;
  wire  [    W - 1 : 0] out_val_expected;

  logic [       15 : 0] counter = 0;

  initial begin
    $readmemb("in.bin", in_rom);
    $readmemb("out.bin", out_rom);
  end
  assign in_val = in_rom[counter];
  assign out_val_expected = out_rom[counter];

  Swizzle #(
      .WIDTH(W)
  ) dut (
      .IN(in_val),
      .OUT(out_val)
  );

  always_ff @(posedge clock) begin
    counter <= counter + 1;
    if (out_val != out_val_expected) begin
      $display("[%d] Expected swizzle(%d) = %d but got %d", counter, in_val, out_val_expected, out_val);
      $stop;
    end
    if (counter == TEST_SIZE - 1) begin
      $display("Finished after %d", counter);
      $finish;
    end
  end

endmodule
