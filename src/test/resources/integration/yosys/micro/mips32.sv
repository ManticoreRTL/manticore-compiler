
/**
Cascade

Copyright (c) 2017 VMware, Inc.  All rights reserved

The BSD-2 license (the "License") set forth below applies to all parts of the
Cascade project.  You may not use this file except in compliance with the
License.

BSD-2 License

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
module AluControl (
    alu_op,
    op,
    ffield,
    ctrl
);
  input wire alu_op;
  input wire [5:0] op;
  input wire [5:0] ffield;
  output reg [3:0] ctrl;

  always @(*) begin
    // Switch on opcode
    if (alu_op == 0) begin
      case (op)
        6'd4: ctrl = 4'b0100;  // beq:  sub
        6'd8: ctrl = 4'b0011;  // addi: add
        6'd10: ctrl = 4'b1001;  // slti: slt
        6'd12: ctrl = 4'b0101;  // andi: and
        6'd13: ctrl = 4'b0110;  // ori:  or
        6'd14: ctrl = 4'b0111;  // xori: xor
        6'd15: ctrl = 4'b1010;  // lui:  lui
        6'd35: ctrl = 4'b0011;  // lw:   add
        6'd43: ctrl = 4'b0011;  // sw:   add
        default: ctrl = 0;
      endcase
    end
    // Switch on function field
    else
    begin
      case (ffield)
        6'd0: ctrl = 4'b0000;  // sll:  sll
        6'd2: ctrl = 4'b0001;  // srl:  srl
        6'd3: ctrl = 4'b0010;  // sra:  sra
        6'd6: ctrl = 4'b0001;  // srlv: srl
        6'd7: ctrl = 4'b0010;  // srav: sra
        6'd32: ctrl = 4'b0011;  // add:  add
        6'd34: ctrl = 4'b0100;  // sub:  sub
        6'd36: ctrl = 4'b0101;  // and:  and
        6'd37: ctrl = 4'b0110;  // or:   or
        6'd38: ctrl = 4'b0111;  // xor:  xor
        6'd39: ctrl = 4'b1000;  // nor:  nor
        6'd42: ctrl = 4'b1001;  // slt:  slt
        default: ctrl = 0;
      endcase
    end
  end
endmodule

module Alu (
    ctrl,
    op1,
    op2,
    zero,
    result
);
  input wire [3:0] ctrl;
  input wire [31:0] op1;
  input wire [31:0] op2;
  output wire zero;
  output reg [31:0] result;

  always @(*) begin
    case (ctrl)
      4'b0000: result = op1 << op2[4:0];  // sll
      4'b0001: result = op1 >> op2[4:0];  // srl
      4'b0010: result = op1 >>> op2[4:0];  // sra
      4'b0011: result = op1 + op2;  // add
      4'b0100: result = op1 - op2;  // sub
      4'b0101: result = op1 & op2;  // and
      4'b0110: result = op1 | op2;  // or
      4'b0111: result = op1 ^ op2;  // xor
      4'b1000: result = !(op1 | op2);  // nor
      4'b1001: result = (op1 < op2) ? 1 : 0;  // slt
      4'b1010: result = op2 << 16;  // lui
      default: result = 0;
    endcase
  end
  assign zero = (result == 32'b0);
endmodule


module RegisterFile #(
    parameter ADDR_SIZE = 4,
    parameter BYTE_SIZE = 8
) (
    input wire clock,
    input wire wen,
    input wire [ADDR_SIZE-1:0] raddr1,
    output wire [BYTE_SIZE-1:0] rdata1,
    input wire [ADDR_SIZE-1:0] raddr2,
    output wire [BYTE_SIZE-1:0] rdata2,
    input wire [ADDR_SIZE-1:0] waddr,
    input wire [BYTE_SIZE-1:0] wdata
);
  reg [BYTE_SIZE-1:0] mem[2**ADDR_SIZE-1:0];
  assign rdata1 = mem[raddr1];
  assign rdata2 = mem[raddr2];
  always @(posedge clock) if (wen) mem[waddr] <= wdata;
endmodule

module Memory #(
    parameter ADDR_SIZE = 4,
    parameter BYTE_SIZE = 8
) (
    input wire clock,
    input wire wen,
    input wire [ADDR_SIZE-1:0] raddr1,
    output wire [BYTE_SIZE-1:0] rdata1,
    input wire [ADDR_SIZE-1:0] waddr,
    input wire [BYTE_SIZE-1:0] wdata
);
  reg [BYTE_SIZE-1:0] mem[2**ADDR_SIZE-1:0];
  assign rdata1 = mem[raddr1];
  always @(posedge clock) if (wen) mem[waddr] <= wdata;
endmodule

module Control (
    instruction,
    reg_dst,
    jump,
    branch,
    mem_to_reg,
    alu_op,
    mem_write,
    alu_src,
    reg_write
);
  input wire [31:0] instruction;
  output reg reg_dst;
  output reg jump;
  output reg branch;
  output reg mem_to_reg;
  output reg alu_op;
  output reg mem_write;
  output reg [1:0] alu_src;
  output reg reg_write;

  always @(*) begin
    case (instruction[31:26])
      // r-type
      6'd0: begin
        // break
        if (instruction[5:0] == 6'd13) begin
          reg_dst = 0;
          jump = 0;
          branch = 0;
          mem_to_reg = 0;
          alu_op = 0;
          mem_write = 0;
          alu_src = 2'b00;
          reg_write = 0;
        end
        // sll, srl, sra
        else
        if (instruction[5:0] < 6'd4) begin
          reg_dst = 1;
          jump = 0;
          branch = 0;
          mem_to_reg = 0;
          alu_op = 1;
          mem_write = 0;
          alu_src = 2'b10;
          reg_write = 1;
        end
        // srlv, srav
        else
        if (instruction[5:0] < 6'd8) begin
          reg_dst = 1;
          jump = 0;
          branch = 0;
          mem_to_reg = 0;
          alu_op = 1;
          mem_write = 0;
          alu_src = 2'b11;
          reg_write = 1;
        end
        // add, sub, and, or, xor, nor, slt
        else
        begin
          reg_dst = 1;
          jump = 0;
          branch = 0;
          mem_to_reg = 0;
          alu_op = 1;
          mem_write = 0;
          alu_src = 2'b00;
          reg_write = 1;
        end
      end
      // j
      6'd2: begin
        reg_dst = 0;
        jump = 1;
        branch = 0;
        mem_to_reg = 0;
        alu_op = 0;
        mem_write = 0;
        alu_src = 2'b00;
        reg_write = 0;
      end
      // beq
      6'd4: begin
        reg_dst = 0;
        jump = 0;
        branch = 1;
        mem_to_reg = 0;
        alu_op = 0;
        mem_write = 0;
        alu_src = 2'b00;
        reg_write = 0;
      end
      // addi
      6'd8: begin
        reg_dst = 0;
        jump = 0;
        branch = 0;
        mem_to_reg = 0;
        alu_op = 0;
        mem_write = 0;
        alu_src = 2'b01;
        reg_write = 1;
      end
      // slti
      6'd10: begin
        reg_dst = 0;
        jump = 0;
        branch = 0;
        mem_to_reg = 0;
        alu_op = 0;
        mem_write = 0;
        alu_src = 2'b01;
        reg_write = 1;
      end
      // andi
      6'd12: begin
        reg_dst = 0;
        jump = 0;
        branch = 0;
        mem_to_reg = 0;
        alu_op = 0;
        mem_write = 0;
        alu_src = 2'b01;
        reg_write = 1;
      end
      // ori
      6'd13: begin
        reg_dst = 0;
        jump = 0;
        branch = 0;
        mem_to_reg = 0;
        alu_op = 0;
        mem_write = 0;
        alu_src = 2'b01;
        reg_write = 1;
      end
      // xori
      6'd14: begin
        reg_dst = 0;
        jump = 0;
        branch = 0;
        mem_to_reg = 0;
        alu_op = 0;
        mem_write = 0;
        alu_src = 2'b01;
        reg_write = 1;
      end
      // lui
      6'd15: begin
        reg_dst = 0;
        jump = 0;
        branch = 0;
        mem_to_reg = 0;
        alu_op = 0;
        mem_write = 0;
        alu_src = 2'b01;
        reg_write = 1;
      end
      // lw
      6'd35: begin
        reg_dst = 0;
        jump = 0;
        branch = 0;
        mem_to_reg = 1;
        alu_op = 0;
        mem_write = 0;
        alu_src = 2'b01;
        reg_write = 1;
      end
      // sw
      6'd43: begin
        reg_dst = 0;
        jump = 0;
        branch = 0;
        mem_to_reg = 0;
        alu_op = 0;
        mem_write = 1;
        alu_src = 2'b01;
        reg_write = 0;
      end

      // Careful! Don't create a latch here!
      default: begin
        reg_dst = 0;
        jump = 0;
        branch = 0;
        mem_to_reg = 0;
        alu_op = 0;
        mem_write = 0;
        alu_src = 2'b00;
        reg_write = 0;
      end
    endcase
  end
endmodule

module Mips32 (
    input wire [31:0] instr,
    output wire [31:0] raddr,
    output halted,
    input wire clock,
    input wire reset
);

  // Program Counter
  reg [31:0] pc = 0;
  wire [31:0] pc_4 = pc + 4;

  // Connection to Instruction Memory (64 aligned 32-bit words)
  // (Aligned word addressable, so addrs are shifted right 2)
  assign raddr = pc >> 2;

  // Control Unit
  wire reg_dst;
  wire jump;
  wire branch;
  wire mem_to_reg;
  wire alu_op;
  wire mem_write;
  wire [1:0] alu_src;
  wire c_reg_write;
  Control control (
      .instruction(instr),
      .reg_dst(reg_dst),
      .jump(jump),
      .branch(branch),
      .mem_to_reg(mem_to_reg),
      .alu_op(alu_op),
      .mem_write(mem_write),
      .alu_src(alu_src),
      .reg_write(c_reg_write)
  );

  // Immediate Logic
  wire [4:0] shamt = instr[10:6];
  wire [31:0] imm = {instr[15] ? 16'b1 : 16'b0, instr[15:0]};

  // Jump Logic
  wire [31:0] jump_addr = {pc_4[31:28], instr[25:0], 2'b0};
  wire [31:0] branch_addr = pc_4 + (imm << 2);

  // Register File
  wire [31:0] reg_read1;
  wire [31:0] reg_read2;
  wire [31:0] reg_write;
  RegisterFile #(5, 32) regs (
      .clock(clock),
      .wen(c_reg_write),
      .raddr1(halted_r ? 5'd2 : instr[25:21]),
      .rdata1(reg_read1),
      .raddr2(instr[20:16]),
      .rdata2(reg_read2),
      .waddr(reg_dst ? instr[15:11] : instr[20:16]),
      .wdata(reg_write)
  );
  always @(posedge clock) begin
    if (c_reg_write) begin
      $display("%h %h: RF[%d] <= %d", pc, instr, reg_dst ? instr[15:11] : instr[20:16], reg_write);
    end
  end
  // ALU Control
  wire [3:0] ctrl;
  AluControl alu_control (
      .alu_op(alu_op),
      .op(instr[31:26]),
      .ffield(instr[5:0]),
      .ctrl(ctrl)
  );

  // ALU
  wire zero;
  wire [31:0] result;
  Alu alu (
      .ctrl(ctrl),
      .op1(alu_src[1] ? reg_read2 : reg_read1),
      .op2(alu_src[1] ? (alu_src[0] ? reg_read1 : shamt) : (alu_src[0] ? imm : reg_read2)),
      .zero(zero),
      .result(result)
  );

  // Data Memory (2^16 aligned 32-bit words)
  // (Aligned word addressable, so addres are shifted right 2)
  wire [31:0] mem_read;
  Memory #(7, 32) dmem (
      .clock(clock),
      .wen(mem_write),
      .raddr1(result >> 2),
      .rdata1(mem_read),
      .waddr(result >> 2),
      .wdata(reg_read2)
  );

  reg halted_r = 0;
  assign halted = halted_r;
  assign reg_write = mem_to_reg ? mem_read : result;


  // Main Loop
  always @(posedge clock) begin

    if (reset) begin
      pc <= 0;
      halted_r <= 1'b0;
    end else begin
      if (instr[5:0] == 6'd13) begin
        halted_r <= 1'b1;
        $display("Got halt!");
        $finish;
      end
      // if (halted_r == 1'b0) begin
      if (jump) begin
        pc <= jump_addr;
      end else if (branch & zero) begin
        pc <= branch_addr;
      end else begin
        pc <= pc_4;
      end
      // end

    end




  end
endmodule





module ResetDriver (
    input  wire clock,
    output wire reset
);



  reg [2:0] reset_counter = 7;

  always_ff @(posedge clock) begin
    if (reset_counter > 0) reset_counter <= reset_counter - 1;
  end

  assign reset = reset_counter != 0;

endmodule

