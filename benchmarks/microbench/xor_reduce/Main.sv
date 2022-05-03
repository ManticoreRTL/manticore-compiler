module Parity (
    input wire [35 : 0] IN,
    output wire         OUT
);

  logic [5 : 0] tmp;

  genvar i;
  generate
    for (i = 0; i < 6; i = i + 1) begin
      assign tmp[i] = IN[6*i] ^ IN[6*i + 1] ^ IN[6*i + 2] ^ IN[6*i + 3] ^ IN[6*i + 4] ^ IN[6*i + 5];
    end
  endgenerate

  assign OUT = tmp[0] ^ tmp[1] ^ tmp[2] ^ tmp[3] ^ tmp[4] ^ tmp[5];

endmodule

module Main (
    input wire clock
);

  localparam W = 36;
  localparam TEST_SIZE = 50;

  logic [    W - 1 : 0] in_rom        [0 : TEST_SIZE - 1];
  logic                 out_rom       [0 : TEST_SIZE - 1];

  wire  [    W - 1 : 0] in_val;
  wire                  out_val;
  wire                  out_val_expected;

  logic [       15 : 0] counter = 0;

  initial begin
    $readmemb("in.bin", in_rom);
    $readmemb("out.bin", out_rom);
  end
  assign in_val = in_rom[counter];
  assign out_val_expected = out_rom[counter];

  Parity dut (
      .IN(in_val),
      .OUT(out_val)
  );

  always_ff @(posedge clock) begin
    counter <= counter + 1;

`ifdef VERILATOR
    if (out_val != out_val_expected) begin
      $display("[%d] Expected parity(%d) = %d but got %d", counter, in_val, out_val_expected, out_val);
    end
    assert (out_val == out_val_expected);
`else
    $masm_expect(out_val == out_val_expected, "invalid result");
`endif

    if (counter == TEST_SIZE - 1) begin
`ifdef VERILATOR
      $finish;
`else
      $masm_stop;
`endif
    end
  end

endmodule
