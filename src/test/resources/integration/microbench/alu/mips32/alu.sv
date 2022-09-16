
`define SLL 4'b0000
`define SRL 4'b0001
`define SRA 4'b0010
`define ADD 4'b0011
`define SUB 4'b0100
`define AND 4'b0101
`define OR 4'b0110
`define XOR 4'b0111
`define NOR 4'b1000
`define SLT 4'b1001
`define LUI 4'b1010


module Alu (
    input wire [3:0] ctrl,
    input wire [31:0] op1,
    input wire [31:0] op2,
    output wire zero,
    output wire [31:0] result
);


  wire  [ 4:0] shift_amount;
  logic [31:0] res;
  assign result = res;
  assign shift_amount = op2[4:0];
  assign zero = (res == 32'd0);

  always_comb begin
    case (ctrl)
      `SLL: res = op1 << shift_amount;
      `SRL: res = op1 >> shift_amount;
      // IMPORTANT! op1 >>> shift_amount would not implement an arithmetic shift
      // because op1 is unsigned by default
      `SRA: res = $signed(op1) >>> shift_amount;
      `ADD: res = op1 + op2;
      `SUB: res = op1 - op2;
      `AND: res = op1 & op2;
      `OR: res  = op1 | op2;
      `XOR: res = op1 ^ op2;
      `NOR: res = ~(op1 | op2);
      `SLT: res = (op1 < op2) ? 1 : 0;
      `LUI: res = op1 << 16;
      default: res = 0;
    endcase
  end


endmodule


module AluTester #(
    parameter TEST_SIZE = 100,
    parameter CTRL_ROM = "ctrl_rom.hex",
    parameter OP1_ROM = "op1_rom.hex",
    parameter OP2_ROM = "op2_rom.hex",
    parameter RESULT_ROM = "result_rom.hex",
    parameter ZERO_ROM = "zero_rom.hex",

) (
    input wire clock
);

  logic [3:0] ctrl_rom    [TEST_SIZE - 1 : 0];
  logic [31:0] op1_rom     [TEST_SIZE - 1 : 0];
  logic [31:0] op2_rom     [TEST_SIZE - 1 : 0];
  logic [31:0] result_rom  [TEST_SIZE - 1 : 0];
  logic [0: 0] zero_rom    [TEST_SIZE - 1 : 0];
  wire  [31:0] result;
  wire         zero;
  logic [15:0] counter = 0;

  initial begin
    $readmemh(CTRL_ROM, ctrl_rom);
    $readmemh(OP1_ROM, op1_rom);
    $readmemh(OP2_ROM, op2_rom);
    $readmemh(RESULT_ROM, result_rom);
    $readmemh(ZERO_ROM, zero_rom);
  end

  Alu dut (
      .ctrl(ctrl_rom[counter]),
      .op1(op1_rom[counter]),
      .op2(op2_rom[counter]),
      .result(result),
      .zero(zero)
  );

  always_ff @(posedge clock) begin

    assert (result_rom[counter] == result);
    assert (zero_rom[counter][0] == zero);
    if (counter < TEST_SIZE - 1) begin
      counter <= counter + 1;
    end else begin
      $finish;
    end
  end


endmodule
