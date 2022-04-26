module Swizzle #(
    WIDTH = 8
) (
    input wire clock,
    input wire [WIDTH - 1 : 0] IN,
    output wire [WIDTH - 1 : 0] OUT
);

  logic [WIDTH - 1 : 0] swizzled;
  reg   [WIDTH - 1 : 0] swizzled_reg;

  genvar i;
  generate
    for (i=0; i<WIDTH; i=i+1) begin : ff_gen
      assign swizzled[i] = IN[WIDTH-1-i];
    end
  endgenerate

  always_ff @(posedge clock) begin
    swizzled_reg <= swizzled;
  end

  assign OUT = swizzled_reg;

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

  logic [       15 : 0] icounter = 0;
  logic [       15 : 0] ocounter = 0;
  Swizzle #(
      .WIDTH(W)
  ) dut (
      .clock(clock),
      .IN(in_val),
      .OUT(out_val)
  );

  always_ff @(posedge clock) begin
    if (icounter < TEST_SIZE - 1) begin
      icounter <= icounter + 1;
    end

    ocounter <= ocounter + 1;

`ifdef VERILATOR
    if (out_val != out_rom[ocounter]) begin
      $display("[%d] Expected swizzle(%d) = %d but got %d", ocounter, in_rom[ocounter], out_rom[ocounter], out_val);
    end
    assert (out_val == out_rom[ocounter]);
`else
    $masm_expect(out_val == out_rom[ocounter], "invalid result");
`endif

    if (ocounter == TEST_SIZE - 1) begin
`ifdef VERILATOR
      $finish;
`else
      $masm_stop;
`endif
    end
  end

  assign in_val = in_rom[icounter];
  assign out_val = out_rom[icounter];
  initial begin
    $readmemb("in.bin", in_rom);
    $readmemb("out.bin", out_rom);
  end

endmodule
