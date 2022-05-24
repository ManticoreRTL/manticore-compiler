
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
`define MUL 4'b1011

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

