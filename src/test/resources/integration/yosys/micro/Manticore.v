module BRAMLike #(
    parameter DATA_WIDTH = 16,
    parameter ADDRESS_WIDTH = 11,
    parameter filename = ""
) (
    input clock,

    // read port
    input  [ADDRESS_WIDTH - 1:0] raddr,
    output [   DATA_WIDTH - 1:0] dout,
    // write port
    input                        wen,
    input  [ADDRESS_WIDTH - 1:0] waddr,
    input  [   DATA_WIDTH - 1:0] din
);


  reg [DATA_WIDTH - 1:0] memory[0:(1 << ADDRESS_WIDTH) - 1];
  reg [DATA_WIDTH - 1:0] dout_reg;
  // reg [ADDRESS_WIDTH - 1:0] addr_reg;

  always @(posedge clock) begin
    if (wen) begin
      memory[waddr] <= din;
    end
    //  write-first behavior
    //  dout_reg <= (waddr == raddr && wen) ? din : memory[raddr];
    dout_reg <= memory[raddr];
    // addr_reg <= raddr;

  end
  assign dout = dout_reg;
  //assign dout = memory[addr_reg];


endmodule

module URAMLike #(
  parameter DATA_WIDTH = 64,
  parameter ADDRESS_WIDTH = 12
)(
   input         clock,

   // read port
   input  [ADDRESS_WIDTH - 1:0] raddr,
   output [DATA_WIDTH - 1:0] dout,
   // write port
   input         wen,
   input  [ADDRESS_WIDTH - 1:0] waddr,
   input  [DATA_WIDTH - 1:0] din
 );
   (* ram_style = "ultra" *)
   reg [DATA_WIDTH - 1:0] memory [0: (1 << ADDRESS_WIDTH) - 1];
   reg [DATA_WIDTH - 1:0] dout_reg;
   reg [ADDRESS_WIDTH - 1:0] addr_reg;
   always @(posedge clock) begin
    if (wen) begin
      memory[waddr] <= din;
    end
    dout_reg <= memory[raddr];
   end
   assign dout = dout_reg;
  //  assign dout = memory[addr_reg];

endmodule

module WrappedLut6
#(
  parameter INIT = 64'h0 // defaults to out = 0
)(
  input wire clock,
  input wire we,
  input wire data,
  input wire a0,
  input wire a1,
  input wire a2,
  input wire a3,
  input wire a4,
  input wire a5,
  output wire out
);


  reg [63:0] equ = INIT;
  wire [5:0] index;
  assign index = {a5, a4, a3, a2, a1, a0};
  integer i;
  wire [63:0] clear = ~(64'b1 << index);
  wire [63:0] set = (data << index);
  always @(posedge clock) begin
    if (we) begin
        equ = (equ & clear) | set;
        // equ[index]= data;
    end
  end

  assign out = equ >> index;


endmodule


module MultiplierDsp48 (
  input               clock,
  input  [16 - 1 : 0] in0,
  input  [16 - 1 : 0] in1,
  output [32 - 1 : 0] out,
  // These ports are here to make simulations easier to understand.
  input               valid_in,
  output              valid_out
);

// Pipeline signal for simulations.
reg valid_d1, valid_d2;
assign valid_out = valid_d2;
always @(posedge clock) begin
  valid_d1 <= valid_in;
  valid_d2 <= valid_d1;
end



  reg [32 - 1 : 0] res_d1, res_d2;
  assign out = res_d2;
  always @(posedge clock) begin
    // Extend to 32 bits to ensure full-precision multiplication result.
    res_d1 <= {16'b0, in0} * {16'b0, in1};
    res_d2 <= res_d1;
  end


endmodule

module SimpleDualPortMemory(
  input         clock,
  input  [11:0] io_raddr,
  output [63:0] io_dout,
  input         io_wen,
  input  [11:0] io_waddr,
  input  [63:0] io_din
);
  wire  impl_wen; // @[GenericMemory.scala 197:20]
  wire  impl_clock; // @[GenericMemory.scala 197:20]
  wire [11:0] impl_raddr; // @[GenericMemory.scala 197:20]
  wire [11:0] impl_waddr; // @[GenericMemory.scala 197:20]
  wire [63:0] impl_din; // @[GenericMemory.scala 197:20]
  wire [63:0] impl_dout; // @[GenericMemory.scala 197:20]
  URAMLike #(.ADDRESS_WIDTH(12), .DATA_WIDTH(64)) impl ( // @[GenericMemory.scala 197:20]
    .wen(impl_wen),
    .clock(impl_clock),
    .raddr(impl_raddr),
    .waddr(impl_waddr),
    .din(impl_din),
    .dout(impl_dout)
  );
  assign io_dout = impl_dout; // @[GenericMemory.scala 207:17]
  assign impl_wen = io_wen; // @[GenericMemory.scala 205:17]
  assign impl_clock = clock; // @[GenericMemory.scala 202:17]
  assign impl_raddr = io_raddr; // @[GenericMemory.scala 204:17]
  assign impl_waddr = io_waddr; // @[GenericMemory.scala 206:17]
  assign impl_din = io_din; // @[GenericMemory.scala 203:17]
endmodule
module FetchCore(
  input         clock,
  input         reset,
  input         io_core_interface_execution_enable,
  output [63:0] io_core_interface_instruction,
  input         io_core_interface_programmer_enable,
  input  [63:0] io_core_interface_programmer_instruction,
  input  [11:0] io_core_interface_programmer_address,
  output [11:0] io_memory_interface_raddr,
  input  [63:0] io_memory_interface_dout,
  output        io_memory_interface_wen,
  output [11:0] io_memory_interface_waddr,
  output [63:0] io_memory_interface_din
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
`endif // RANDOMIZE_REG_INIT
  reg [11:0] pc; // @[Fetch.scala 99:15]
  reg  stopped; // @[Fetch.scala 110:24]
  wire [11:0] _pc_T_1 = pc + 12'h1; // @[Fetch.scala 137:19]
  wire  _GEN_1 = io_core_interface_execution_enable ? 1'h0 : 1'h1; // @[Fetch.scala 136:44 138:13 141:13]
  assign io_core_interface_instruction = stopped ? 64'h0 : io_memory_interface_dout; // @[Fetch.scala 144:17 145:35 147:35]
  assign io_memory_interface_raddr = pc; // @[Fetch.scala 107:33]
  assign io_memory_interface_wen = io_core_interface_programmer_enable; // @[Fetch.scala 154:29]
  assign io_memory_interface_waddr = io_core_interface_programmer_address; // @[Fetch.scala 152:29]
  assign io_memory_interface_din = io_core_interface_programmer_instruction; // @[Fetch.scala 153:29]
  always @(posedge clock) begin
    if (io_core_interface_execution_enable) begin // @[Fetch.scala 136:44]
      pc <= _pc_T_1; // @[Fetch.scala 137:13]
    end else begin
      pc <= 12'h0; // @[Fetch.scala 140:13]
    end
    stopped <= reset | _GEN_1; // @[Fetch.scala 110:{24,24}]
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{`RANDOM}};
  pc = _RAND_0[11:0];
  _RAND_1 = {1{`RANDOM}};
  stopped = _RAND_1[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Fetch(
  input         clock,
  input         reset,
  input         io_execution_enable,
  output [63:0] io_instruction,
  input         io_programmer_enable,
  input  [63:0] io_programmer_instruction,
  input  [11:0] io_programmer_address
);
  wire  inst_memory_clock; // @[Fetch.scala 70:27]
  wire [11:0] inst_memory_io_raddr; // @[Fetch.scala 70:27]
  wire [63:0] inst_memory_io_dout; // @[Fetch.scala 70:27]
  wire  inst_memory_io_wen; // @[Fetch.scala 70:27]
  wire [11:0] inst_memory_io_waddr; // @[Fetch.scala 70:27]
  wire [63:0] inst_memory_io_din; // @[Fetch.scala 70:27]
  wire  fetch_core_clock; // @[Fetch.scala 78:26]
  wire  fetch_core_reset; // @[Fetch.scala 78:26]
  wire  fetch_core_io_core_interface_execution_enable; // @[Fetch.scala 78:26]
  wire [63:0] fetch_core_io_core_interface_instruction; // @[Fetch.scala 78:26]
  wire  fetch_core_io_core_interface_programmer_enable; // @[Fetch.scala 78:26]
  wire [63:0] fetch_core_io_core_interface_programmer_instruction; // @[Fetch.scala 78:26]
  wire [11:0] fetch_core_io_core_interface_programmer_address; // @[Fetch.scala 78:26]
  wire [11:0] fetch_core_io_memory_interface_raddr; // @[Fetch.scala 78:26]
  wire [63:0] fetch_core_io_memory_interface_dout; // @[Fetch.scala 78:26]
  wire  fetch_core_io_memory_interface_wen; // @[Fetch.scala 78:26]
  wire [11:0] fetch_core_io_memory_interface_waddr; // @[Fetch.scala 78:26]
  wire [63:0] fetch_core_io_memory_interface_din; // @[Fetch.scala 78:26]
  SimpleDualPortMemory inst_memory ( // @[Fetch.scala 70:27]
    .clock(inst_memory_clock),
    .io_raddr(inst_memory_io_raddr),
    .io_dout(inst_memory_io_dout),
    .io_wen(inst_memory_io_wen),
    .io_waddr(inst_memory_io_waddr),
    .io_din(inst_memory_io_din)
  );
  FetchCore fetch_core ( // @[Fetch.scala 78:26]
    .clock(fetch_core_clock),
    .reset(fetch_core_reset),
    .io_core_interface_execution_enable(fetch_core_io_core_interface_execution_enable),
    .io_core_interface_instruction(fetch_core_io_core_interface_instruction),
    .io_core_interface_programmer_enable(fetch_core_io_core_interface_programmer_enable),
    .io_core_interface_programmer_instruction(fetch_core_io_core_interface_programmer_instruction),
    .io_core_interface_programmer_address(fetch_core_io_core_interface_programmer_address),
    .io_memory_interface_raddr(fetch_core_io_memory_interface_raddr),
    .io_memory_interface_dout(fetch_core_io_memory_interface_dout),
    .io_memory_interface_wen(fetch_core_io_memory_interface_wen),
    .io_memory_interface_waddr(fetch_core_io_memory_interface_waddr),
    .io_memory_interface_din(fetch_core_io_memory_interface_din)
  );
  assign io_instruction = fetch_core_io_core_interface_instruction; // @[Fetch.scala 80:6]
  assign inst_memory_clock = clock;
  assign inst_memory_io_raddr = fetch_core_io_memory_interface_raddr; // @[Fetch.scala 81:34]
  assign inst_memory_io_wen = fetch_core_io_memory_interface_wen; // @[Fetch.scala 81:34]
  assign inst_memory_io_waddr = fetch_core_io_memory_interface_waddr; // @[Fetch.scala 81:34]
  assign inst_memory_io_din = fetch_core_io_memory_interface_din; // @[Fetch.scala 81:34]
  assign fetch_core_clock = clock;
  assign fetch_core_reset = reset;
  assign fetch_core_io_core_interface_execution_enable = io_execution_enable; // @[Fetch.scala 80:6]
  assign fetch_core_io_core_interface_programmer_enable = io_programmer_enable; // @[Fetch.scala 80:6]
  assign fetch_core_io_core_interface_programmer_instruction = io_programmer_instruction; // @[Fetch.scala 80:6]
  assign fetch_core_io_core_interface_programmer_address = io_programmer_address; // @[Fetch.scala 80:6]
  assign fetch_core_io_memory_interface_dout = inst_memory_io_dout; // @[Fetch.scala 81:34]
endmodule
module Decode(
  input         clock,
  input  [63:0] io_instruction,
  output [10:0] io_pipe_out_rd,
  output [10:0] io_pipe_out_rs1,
  output [10:0] io_pipe_out_rs2,
  output [10:0] io_pipe_out_rs3,
  output [10:0] io_pipe_out_rs4,
  output        io_pipe_out_opcode_cust,
  output        io_pipe_out_opcode_arith,
  output        io_pipe_out_opcode_lload,
  output        io_pipe_out_opcode_lstore,
  output        io_pipe_out_opcode_send,
  output        io_pipe_out_opcode_set,
  output        io_pipe_out_opcode_expect,
  output        io_pipe_out_opcode_predicate,
  output        io_pipe_out_opcode_set_carry,
  output        io_pipe_out_opcode_set_lut_data,
  output        io_pipe_out_opcode_configure_luts_0,
  output        io_pipe_out_opcode_configure_luts_1,
  output        io_pipe_out_opcode_configure_luts_2,
  output        io_pipe_out_opcode_configure_luts_3,
  output        io_pipe_out_opcode_configure_luts_4,
  output        io_pipe_out_opcode_configure_luts_5,
  output        io_pipe_out_opcode_configure_luts_6,
  output        io_pipe_out_opcode_configure_luts_7,
  output        io_pipe_out_opcode_configure_luts_8,
  output        io_pipe_out_opcode_configure_luts_9,
  output        io_pipe_out_opcode_configure_luts_10,
  output        io_pipe_out_opcode_configure_luts_11,
  output        io_pipe_out_opcode_configure_luts_12,
  output        io_pipe_out_opcode_configure_luts_13,
  output        io_pipe_out_opcode_configure_luts_14,
  output        io_pipe_out_opcode_configure_luts_15,
  output        io_pipe_out_opcode_configure_luts_16,
  output        io_pipe_out_opcode_configure_luts_17,
  output        io_pipe_out_opcode_configure_luts_18,
  output        io_pipe_out_opcode_configure_luts_19,
  output        io_pipe_out_opcode_configure_luts_20,
  output        io_pipe_out_opcode_configure_luts_21,
  output        io_pipe_out_opcode_configure_luts_22,
  output        io_pipe_out_opcode_configure_luts_23,
  output        io_pipe_out_opcode_configure_luts_24,
  output        io_pipe_out_opcode_configure_luts_25,
  output        io_pipe_out_opcode_configure_luts_26,
  output        io_pipe_out_opcode_configure_luts_27,
  output        io_pipe_out_opcode_configure_luts_28,
  output        io_pipe_out_opcode_configure_luts_29,
  output        io_pipe_out_opcode_configure_luts_30,
  output        io_pipe_out_opcode_configure_luts_31,
  output        io_pipe_out_opcode_slice,
  output        io_pipe_out_opcode_mul,
  output        io_pipe_out_opcode_mulh,
  output [4:0]  io_pipe_out_funct,
  output [15:0] io_pipe_out_immediate,
  output [3:0]  io_pipe_out_slice_ofst
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_6;
  reg [31:0] _RAND_7;
  reg [31:0] _RAND_8;
  reg [31:0] _RAND_9;
  reg [31:0] _RAND_10;
  reg [31:0] _RAND_11;
  reg [31:0] _RAND_12;
  reg [31:0] _RAND_13;
  reg [31:0] _RAND_14;
  reg [31:0] _RAND_15;
  reg [31:0] _RAND_16;
  reg [31:0] _RAND_17;
  reg [31:0] _RAND_18;
  reg [31:0] _RAND_19;
  reg [31:0] _RAND_20;
  reg [31:0] _RAND_21;
  reg [31:0] _RAND_22;
  reg [31:0] _RAND_23;
  reg [31:0] _RAND_24;
  reg [31:0] _RAND_25;
  reg [31:0] _RAND_26;
  reg [31:0] _RAND_27;
  reg [31:0] _RAND_28;
  reg [31:0] _RAND_29;
  reg [31:0] _RAND_30;
  reg [31:0] _RAND_31;
  reg [31:0] _RAND_32;
  reg [31:0] _RAND_33;
  reg [31:0] _RAND_34;
  reg [31:0] _RAND_35;
  reg [31:0] _RAND_36;
  reg [31:0] _RAND_37;
  reg [31:0] _RAND_38;
  reg [31:0] _RAND_39;
  reg [31:0] _RAND_40;
  reg [31:0] _RAND_41;
  reg [31:0] _RAND_42;
  reg [31:0] _RAND_43;
  reg [31:0] _RAND_44;
  reg [31:0] _RAND_45;
  reg [31:0] _RAND_46;
  reg [31:0] _RAND_47;
  reg [31:0] _RAND_48;
`endif // RANDOMIZE_REG_INIT
  wire [3:0] opcode = io_instruction[3:0]; // @[Decode.scala 83:63]
  wire [4:0] funct = io_instruction[19:15]; // @[Decode.scala 83:63]
  reg  opcode_regs_cust; // @[Decode.scala 105:27]
  reg  opcode_regs_arith; // @[Decode.scala 105:27]
  reg  opcode_regs_lload; // @[Decode.scala 105:27]
  reg  opcode_regs_lstore; // @[Decode.scala 105:27]
  reg  opcode_regs_send; // @[Decode.scala 105:27]
  reg  opcode_regs_set; // @[Decode.scala 105:27]
  reg  opcode_regs_expect; // @[Decode.scala 105:27]
  reg  opcode_regs_predicate; // @[Decode.scala 105:27]
  reg  opcode_regs_set_carry; // @[Decode.scala 105:27]
  reg  opcode_regs_set_lut_data; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_0; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_1; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_2; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_3; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_4; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_5; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_6; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_7; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_8; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_9; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_10; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_11; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_12; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_13; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_14; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_15; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_16; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_17; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_18; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_19; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_20; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_21; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_22; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_23; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_24; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_25; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_26; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_27; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_28; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_29; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_30; // @[Decode.scala 105:27]
  reg  opcode_regs_configure_luts_31; // @[Decode.scala 105:27]
  reg  opcode_regs_slice; // @[Decode.scala 105:27]
  reg  opcode_regs_mul; // @[Decode.scala 105:27]
  reg  opcode_regs_mulh; // @[Decode.scala 105:27]
  reg [4:0] funct_reg; // @[Decode.scala 106:27]
  reg [15:0] immediate_reg; // @[Decode.scala 107:27]
  reg [3:0] slice_ofst_reg; // @[Decode.scala 108:27]
  reg [10:0] rd_reg; // @[Decode.scala 109:27]
  wire  is_arith = opcode == 4'h3; // @[Decode.scala 112:23]
  assign io_pipe_out_rd = rd_reg; // @[Decode.scala 141:26]
  assign io_pipe_out_rs1 = io_instruction[30:20]; // @[Decode.scala 83:63]
  assign io_pipe_out_rs2 = io_instruction[41:31]; // @[Decode.scala 83:63]
  assign io_pipe_out_rs3 = io_instruction[52:42]; // @[Decode.scala 83:63]
  assign io_pipe_out_rs4 = io_instruction[63:53]; // @[Decode.scala 83:63]
  assign io_pipe_out_opcode_cust = opcode_regs_cust; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_arith = opcode_regs_arith; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_lload = opcode_regs_lload; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_lstore = opcode_regs_lstore; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_send = opcode_regs_send; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_set = opcode_regs_set; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_expect = opcode_regs_expect; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_predicate = opcode_regs_predicate; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_set_carry = opcode_regs_set_carry; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_set_lut_data = opcode_regs_set_lut_data; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_0 = opcode_regs_configure_luts_0; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_1 = opcode_regs_configure_luts_1; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_2 = opcode_regs_configure_luts_2; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_3 = opcode_regs_configure_luts_3; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_4 = opcode_regs_configure_luts_4; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_5 = opcode_regs_configure_luts_5; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_6 = opcode_regs_configure_luts_6; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_7 = opcode_regs_configure_luts_7; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_8 = opcode_regs_configure_luts_8; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_9 = opcode_regs_configure_luts_9; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_10 = opcode_regs_configure_luts_10; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_11 = opcode_regs_configure_luts_11; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_12 = opcode_regs_configure_luts_12; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_13 = opcode_regs_configure_luts_13; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_14 = opcode_regs_configure_luts_14; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_15 = opcode_regs_configure_luts_15; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_16 = opcode_regs_configure_luts_16; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_17 = opcode_regs_configure_luts_17; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_18 = opcode_regs_configure_luts_18; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_19 = opcode_regs_configure_luts_19; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_20 = opcode_regs_configure_luts_20; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_21 = opcode_regs_configure_luts_21; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_22 = opcode_regs_configure_luts_22; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_23 = opcode_regs_configure_luts_23; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_24 = opcode_regs_configure_luts_24; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_25 = opcode_regs_configure_luts_25; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_26 = opcode_regs_configure_luts_26; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_27 = opcode_regs_configure_luts_27; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_28 = opcode_regs_configure_luts_28; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_29 = opcode_regs_configure_luts_29; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_30 = opcode_regs_configure_luts_30; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_configure_luts_31 = opcode_regs_configure_luts_31; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_slice = opcode_regs_slice; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_mul = opcode_regs_mul; // @[Decode.scala 138:26]
  assign io_pipe_out_opcode_mulh = opcode_regs_mulh; // @[Decode.scala 138:26]
  assign io_pipe_out_funct = funct_reg; // @[Decode.scala 139:26]
  assign io_pipe_out_immediate = immediate_reg; // @[Decode.scala 140:26]
  assign io_pipe_out_slice_ofst = slice_ofst_reg; // @[Decode.scala 142:26]
  always @(posedge clock) begin
    opcode_regs_cust <= opcode == 4'h2; // @[Decode.scala 116:41]
    opcode_regs_arith <= opcode == 4'h3; // @[Decode.scala 112:23]
    opcode_regs_lload <= opcode == 4'h4; // @[Decode.scala 120:41]
    opcode_regs_lstore <= opcode == 4'h5; // @[Decode.scala 121:41]
    opcode_regs_send <= opcode == 4'h9; // @[Decode.scala 126:41]
    opcode_regs_set <= opcode == 4'h1; // @[Decode.scala 123:41]
    opcode_regs_expect <= opcode == 4'h6; // @[Decode.scala 122:41]
    opcode_regs_predicate <= opcode == 4'ha; // @[Decode.scala 127:41]
    opcode_regs_set_carry <= opcode == 4'hb; // @[Decode.scala 128:41]
    opcode_regs_set_lut_data <= opcode == 4'hc; // @[Decode.scala 130:41]
    opcode_regs_configure_luts_0 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_1 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_2 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_3 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_4 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_5 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_6 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_7 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_8 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_9 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_10 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_11 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_12 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_13 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_14 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_15 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_16 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_17 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_18 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_19 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_20 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_21 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_22 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_23 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_24 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_25 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_26 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_27 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_28 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_29 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_30 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_configure_luts_31 <= opcode == 4'hd; // @[Decode.scala 131:68]
    opcode_regs_slice <= opcode == 4'he; // @[Decode.scala 129:41]
    opcode_regs_mul <= is_arith & funct == 5'h2; // @[Decode.scala 118:42]
    opcode_regs_mulh <= is_arith & funct == 5'h3; // @[Decode.scala 119:42]
    funct_reg <= io_instruction[19:15]; // @[Decode.scala 83:63]
    immediate_reg <= io_instruction[63:48]; // @[Decode.scala 83:63]
    slice_ofst_reg <= io_instruction[47:44]; // @[Decode.scala 83:63]
    rd_reg <= io_instruction[14:4]; // @[Decode.scala 83:63]
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{`RANDOM}};
  opcode_regs_cust = _RAND_0[0:0];
  _RAND_1 = {1{`RANDOM}};
  opcode_regs_arith = _RAND_1[0:0];
  _RAND_2 = {1{`RANDOM}};
  opcode_regs_lload = _RAND_2[0:0];
  _RAND_3 = {1{`RANDOM}};
  opcode_regs_lstore = _RAND_3[0:0];
  _RAND_4 = {1{`RANDOM}};
  opcode_regs_send = _RAND_4[0:0];
  _RAND_5 = {1{`RANDOM}};
  opcode_regs_set = _RAND_5[0:0];
  _RAND_6 = {1{`RANDOM}};
  opcode_regs_expect = _RAND_6[0:0];
  _RAND_7 = {1{`RANDOM}};
  opcode_regs_predicate = _RAND_7[0:0];
  _RAND_8 = {1{`RANDOM}};
  opcode_regs_set_carry = _RAND_8[0:0];
  _RAND_9 = {1{`RANDOM}};
  opcode_regs_set_lut_data = _RAND_9[0:0];
  _RAND_10 = {1{`RANDOM}};
  opcode_regs_configure_luts_0 = _RAND_10[0:0];
  _RAND_11 = {1{`RANDOM}};
  opcode_regs_configure_luts_1 = _RAND_11[0:0];
  _RAND_12 = {1{`RANDOM}};
  opcode_regs_configure_luts_2 = _RAND_12[0:0];
  _RAND_13 = {1{`RANDOM}};
  opcode_regs_configure_luts_3 = _RAND_13[0:0];
  _RAND_14 = {1{`RANDOM}};
  opcode_regs_configure_luts_4 = _RAND_14[0:0];
  _RAND_15 = {1{`RANDOM}};
  opcode_regs_configure_luts_5 = _RAND_15[0:0];
  _RAND_16 = {1{`RANDOM}};
  opcode_regs_configure_luts_6 = _RAND_16[0:0];
  _RAND_17 = {1{`RANDOM}};
  opcode_regs_configure_luts_7 = _RAND_17[0:0];
  _RAND_18 = {1{`RANDOM}};
  opcode_regs_configure_luts_8 = _RAND_18[0:0];
  _RAND_19 = {1{`RANDOM}};
  opcode_regs_configure_luts_9 = _RAND_19[0:0];
  _RAND_20 = {1{`RANDOM}};
  opcode_regs_configure_luts_10 = _RAND_20[0:0];
  _RAND_21 = {1{`RANDOM}};
  opcode_regs_configure_luts_11 = _RAND_21[0:0];
  _RAND_22 = {1{`RANDOM}};
  opcode_regs_configure_luts_12 = _RAND_22[0:0];
  _RAND_23 = {1{`RANDOM}};
  opcode_regs_configure_luts_13 = _RAND_23[0:0];
  _RAND_24 = {1{`RANDOM}};
  opcode_regs_configure_luts_14 = _RAND_24[0:0];
  _RAND_25 = {1{`RANDOM}};
  opcode_regs_configure_luts_15 = _RAND_25[0:0];
  _RAND_26 = {1{`RANDOM}};
  opcode_regs_configure_luts_16 = _RAND_26[0:0];
  _RAND_27 = {1{`RANDOM}};
  opcode_regs_configure_luts_17 = _RAND_27[0:0];
  _RAND_28 = {1{`RANDOM}};
  opcode_regs_configure_luts_18 = _RAND_28[0:0];
  _RAND_29 = {1{`RANDOM}};
  opcode_regs_configure_luts_19 = _RAND_29[0:0];
  _RAND_30 = {1{`RANDOM}};
  opcode_regs_configure_luts_20 = _RAND_30[0:0];
  _RAND_31 = {1{`RANDOM}};
  opcode_regs_configure_luts_21 = _RAND_31[0:0];
  _RAND_32 = {1{`RANDOM}};
  opcode_regs_configure_luts_22 = _RAND_32[0:0];
  _RAND_33 = {1{`RANDOM}};
  opcode_regs_configure_luts_23 = _RAND_33[0:0];
  _RAND_34 = {1{`RANDOM}};
  opcode_regs_configure_luts_24 = _RAND_34[0:0];
  _RAND_35 = {1{`RANDOM}};
  opcode_regs_configure_luts_25 = _RAND_35[0:0];
  _RAND_36 = {1{`RANDOM}};
  opcode_regs_configure_luts_26 = _RAND_36[0:0];
  _RAND_37 = {1{`RANDOM}};
  opcode_regs_configure_luts_27 = _RAND_37[0:0];
  _RAND_38 = {1{`RANDOM}};
  opcode_regs_configure_luts_28 = _RAND_38[0:0];
  _RAND_39 = {1{`RANDOM}};
  opcode_regs_configure_luts_29 = _RAND_39[0:0];
  _RAND_40 = {1{`RANDOM}};
  opcode_regs_configure_luts_30 = _RAND_40[0:0];
  _RAND_41 = {1{`RANDOM}};
  opcode_regs_configure_luts_31 = _RAND_41[0:0];
  _RAND_42 = {1{`RANDOM}};
  opcode_regs_slice = _RAND_42[0:0];
  _RAND_43 = {1{`RANDOM}};
  opcode_regs_mul = _RAND_43[0:0];
  _RAND_44 = {1{`RANDOM}};
  opcode_regs_mulh = _RAND_44[0:0];
  _RAND_45 = {1{`RANDOM}};
  funct_reg = _RAND_45[4:0];
  _RAND_46 = {1{`RANDOM}};
  immediate_reg = _RAND_46[15:0];
  _RAND_47 = {1{`RANDOM}};
  slice_ofst_reg = _RAND_47[3:0];
  _RAND_48 = {1{`RANDOM}};
  rd_reg = _RAND_48[10:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module CustomFunction(
  input         clock,
  input         io_config_writeEnable,
  input  [15:0] io_config_loadData,
  input  [15:0] io_rsx_0,
  input  [15:0] io_rsx_1,
  input  [15:0] io_rsx_2,
  input  [15:0] io_rsx_3,
  output [15:0] io_out
);
  wire  lut_0_clock; // @[CustomALU.scala 129:21]
  wire  lut_0_we; // @[CustomALU.scala 129:21]
  wire  lut_0_data; // @[CustomALU.scala 129:21]
  wire  lut_0_a0; // @[CustomALU.scala 129:21]
  wire  lut_0_a1; // @[CustomALU.scala 129:21]
  wire  lut_0_a2; // @[CustomALU.scala 129:21]
  wire  lut_0_a3; // @[CustomALU.scala 129:21]
  wire  lut_0_a4; // @[CustomALU.scala 129:21]
  wire  lut_0_a5; // @[CustomALU.scala 129:21]
  wire  lut_0_out; // @[CustomALU.scala 129:21]
  wire  lut_1_clock; // @[CustomALU.scala 129:21]
  wire  lut_1_we; // @[CustomALU.scala 129:21]
  wire  lut_1_data; // @[CustomALU.scala 129:21]
  wire  lut_1_a0; // @[CustomALU.scala 129:21]
  wire  lut_1_a1; // @[CustomALU.scala 129:21]
  wire  lut_1_a2; // @[CustomALU.scala 129:21]
  wire  lut_1_a3; // @[CustomALU.scala 129:21]
  wire  lut_1_a4; // @[CustomALU.scala 129:21]
  wire  lut_1_a5; // @[CustomALU.scala 129:21]
  wire  lut_1_out; // @[CustomALU.scala 129:21]
  wire  lut_2_clock; // @[CustomALU.scala 129:21]
  wire  lut_2_we; // @[CustomALU.scala 129:21]
  wire  lut_2_data; // @[CustomALU.scala 129:21]
  wire  lut_2_a0; // @[CustomALU.scala 129:21]
  wire  lut_2_a1; // @[CustomALU.scala 129:21]
  wire  lut_2_a2; // @[CustomALU.scala 129:21]
  wire  lut_2_a3; // @[CustomALU.scala 129:21]
  wire  lut_2_a4; // @[CustomALU.scala 129:21]
  wire  lut_2_a5; // @[CustomALU.scala 129:21]
  wire  lut_2_out; // @[CustomALU.scala 129:21]
  wire  lut_3_clock; // @[CustomALU.scala 129:21]
  wire  lut_3_we; // @[CustomALU.scala 129:21]
  wire  lut_3_data; // @[CustomALU.scala 129:21]
  wire  lut_3_a0; // @[CustomALU.scala 129:21]
  wire  lut_3_a1; // @[CustomALU.scala 129:21]
  wire  lut_3_a2; // @[CustomALU.scala 129:21]
  wire  lut_3_a3; // @[CustomALU.scala 129:21]
  wire  lut_3_a4; // @[CustomALU.scala 129:21]
  wire  lut_3_a5; // @[CustomALU.scala 129:21]
  wire  lut_3_out; // @[CustomALU.scala 129:21]
  wire  lut_4_clock; // @[CustomALU.scala 129:21]
  wire  lut_4_we; // @[CustomALU.scala 129:21]
  wire  lut_4_data; // @[CustomALU.scala 129:21]
  wire  lut_4_a0; // @[CustomALU.scala 129:21]
  wire  lut_4_a1; // @[CustomALU.scala 129:21]
  wire  lut_4_a2; // @[CustomALU.scala 129:21]
  wire  lut_4_a3; // @[CustomALU.scala 129:21]
  wire  lut_4_a4; // @[CustomALU.scala 129:21]
  wire  lut_4_a5; // @[CustomALU.scala 129:21]
  wire  lut_4_out; // @[CustomALU.scala 129:21]
  wire  lut_5_clock; // @[CustomALU.scala 129:21]
  wire  lut_5_we; // @[CustomALU.scala 129:21]
  wire  lut_5_data; // @[CustomALU.scala 129:21]
  wire  lut_5_a0; // @[CustomALU.scala 129:21]
  wire  lut_5_a1; // @[CustomALU.scala 129:21]
  wire  lut_5_a2; // @[CustomALU.scala 129:21]
  wire  lut_5_a3; // @[CustomALU.scala 129:21]
  wire  lut_5_a4; // @[CustomALU.scala 129:21]
  wire  lut_5_a5; // @[CustomALU.scala 129:21]
  wire  lut_5_out; // @[CustomALU.scala 129:21]
  wire  lut_6_clock; // @[CustomALU.scala 129:21]
  wire  lut_6_we; // @[CustomALU.scala 129:21]
  wire  lut_6_data; // @[CustomALU.scala 129:21]
  wire  lut_6_a0; // @[CustomALU.scala 129:21]
  wire  lut_6_a1; // @[CustomALU.scala 129:21]
  wire  lut_6_a2; // @[CustomALU.scala 129:21]
  wire  lut_6_a3; // @[CustomALU.scala 129:21]
  wire  lut_6_a4; // @[CustomALU.scala 129:21]
  wire  lut_6_a5; // @[CustomALU.scala 129:21]
  wire  lut_6_out; // @[CustomALU.scala 129:21]
  wire  lut_7_clock; // @[CustomALU.scala 129:21]
  wire  lut_7_we; // @[CustomALU.scala 129:21]
  wire  lut_7_data; // @[CustomALU.scala 129:21]
  wire  lut_7_a0; // @[CustomALU.scala 129:21]
  wire  lut_7_a1; // @[CustomALU.scala 129:21]
  wire  lut_7_a2; // @[CustomALU.scala 129:21]
  wire  lut_7_a3; // @[CustomALU.scala 129:21]
  wire  lut_7_a4; // @[CustomALU.scala 129:21]
  wire  lut_7_a5; // @[CustomALU.scala 129:21]
  wire  lut_7_out; // @[CustomALU.scala 129:21]
  wire  lut_8_clock; // @[CustomALU.scala 129:21]
  wire  lut_8_we; // @[CustomALU.scala 129:21]
  wire  lut_8_data; // @[CustomALU.scala 129:21]
  wire  lut_8_a0; // @[CustomALU.scala 129:21]
  wire  lut_8_a1; // @[CustomALU.scala 129:21]
  wire  lut_8_a2; // @[CustomALU.scala 129:21]
  wire  lut_8_a3; // @[CustomALU.scala 129:21]
  wire  lut_8_a4; // @[CustomALU.scala 129:21]
  wire  lut_8_a5; // @[CustomALU.scala 129:21]
  wire  lut_8_out; // @[CustomALU.scala 129:21]
  wire  lut_9_clock; // @[CustomALU.scala 129:21]
  wire  lut_9_we; // @[CustomALU.scala 129:21]
  wire  lut_9_data; // @[CustomALU.scala 129:21]
  wire  lut_9_a0; // @[CustomALU.scala 129:21]
  wire  lut_9_a1; // @[CustomALU.scala 129:21]
  wire  lut_9_a2; // @[CustomALU.scala 129:21]
  wire  lut_9_a3; // @[CustomALU.scala 129:21]
  wire  lut_9_a4; // @[CustomALU.scala 129:21]
  wire  lut_9_a5; // @[CustomALU.scala 129:21]
  wire  lut_9_out; // @[CustomALU.scala 129:21]
  wire  lut_10_clock; // @[CustomALU.scala 129:21]
  wire  lut_10_we; // @[CustomALU.scala 129:21]
  wire  lut_10_data; // @[CustomALU.scala 129:21]
  wire  lut_10_a0; // @[CustomALU.scala 129:21]
  wire  lut_10_a1; // @[CustomALU.scala 129:21]
  wire  lut_10_a2; // @[CustomALU.scala 129:21]
  wire  lut_10_a3; // @[CustomALU.scala 129:21]
  wire  lut_10_a4; // @[CustomALU.scala 129:21]
  wire  lut_10_a5; // @[CustomALU.scala 129:21]
  wire  lut_10_out; // @[CustomALU.scala 129:21]
  wire  lut_11_clock; // @[CustomALU.scala 129:21]
  wire  lut_11_we; // @[CustomALU.scala 129:21]
  wire  lut_11_data; // @[CustomALU.scala 129:21]
  wire  lut_11_a0; // @[CustomALU.scala 129:21]
  wire  lut_11_a1; // @[CustomALU.scala 129:21]
  wire  lut_11_a2; // @[CustomALU.scala 129:21]
  wire  lut_11_a3; // @[CustomALU.scala 129:21]
  wire  lut_11_a4; // @[CustomALU.scala 129:21]
  wire  lut_11_a5; // @[CustomALU.scala 129:21]
  wire  lut_11_out; // @[CustomALU.scala 129:21]
  wire  lut_12_clock; // @[CustomALU.scala 129:21]
  wire  lut_12_we; // @[CustomALU.scala 129:21]
  wire  lut_12_data; // @[CustomALU.scala 129:21]
  wire  lut_12_a0; // @[CustomALU.scala 129:21]
  wire  lut_12_a1; // @[CustomALU.scala 129:21]
  wire  lut_12_a2; // @[CustomALU.scala 129:21]
  wire  lut_12_a3; // @[CustomALU.scala 129:21]
  wire  lut_12_a4; // @[CustomALU.scala 129:21]
  wire  lut_12_a5; // @[CustomALU.scala 129:21]
  wire  lut_12_out; // @[CustomALU.scala 129:21]
  wire  lut_13_clock; // @[CustomALU.scala 129:21]
  wire  lut_13_we; // @[CustomALU.scala 129:21]
  wire  lut_13_data; // @[CustomALU.scala 129:21]
  wire  lut_13_a0; // @[CustomALU.scala 129:21]
  wire  lut_13_a1; // @[CustomALU.scala 129:21]
  wire  lut_13_a2; // @[CustomALU.scala 129:21]
  wire  lut_13_a3; // @[CustomALU.scala 129:21]
  wire  lut_13_a4; // @[CustomALU.scala 129:21]
  wire  lut_13_a5; // @[CustomALU.scala 129:21]
  wire  lut_13_out; // @[CustomALU.scala 129:21]
  wire  lut_14_clock; // @[CustomALU.scala 129:21]
  wire  lut_14_we; // @[CustomALU.scala 129:21]
  wire  lut_14_data; // @[CustomALU.scala 129:21]
  wire  lut_14_a0; // @[CustomALU.scala 129:21]
  wire  lut_14_a1; // @[CustomALU.scala 129:21]
  wire  lut_14_a2; // @[CustomALU.scala 129:21]
  wire  lut_14_a3; // @[CustomALU.scala 129:21]
  wire  lut_14_a4; // @[CustomALU.scala 129:21]
  wire  lut_14_a5; // @[CustomALU.scala 129:21]
  wire  lut_14_out; // @[CustomALU.scala 129:21]
  wire  lut_15_clock; // @[CustomALU.scala 129:21]
  wire  lut_15_we; // @[CustomALU.scala 129:21]
  wire  lut_15_data; // @[CustomALU.scala 129:21]
  wire  lut_15_a0; // @[CustomALU.scala 129:21]
  wire  lut_15_a1; // @[CustomALU.scala 129:21]
  wire  lut_15_a2; // @[CustomALU.scala 129:21]
  wire  lut_15_a3; // @[CustomALU.scala 129:21]
  wire  lut_15_a4; // @[CustomALU.scala 129:21]
  wire  lut_15_a5; // @[CustomALU.scala 129:21]
  wire  lut_15_out; // @[CustomALU.scala 129:21]
  wire  result_1 = lut_1_out; // @[CustomALU.scala 126:20 158:15]
  wire  result_0 = lut_0_out; // @[CustomALU.scala 126:20 158:15]
  wire  result_3 = lut_3_out; // @[CustomALU.scala 126:20 158:15]
  wire  result_2 = lut_2_out; // @[CustomALU.scala 126:20 158:15]
  wire  result_5 = lut_5_out; // @[CustomALU.scala 126:20 158:15]
  wire  result_4 = lut_4_out; // @[CustomALU.scala 126:20 158:15]
  wire  result_7 = lut_7_out; // @[CustomALU.scala 126:20 158:15]
  wire  result_6 = lut_6_out; // @[CustomALU.scala 126:20 158:15]
  wire [7:0] io_out_lo = {result_7,result_6,result_5,result_4,result_3,result_2,result_1,result_0}; // @[CustomALU.scala 161:20]
  wire  result_9 = lut_9_out; // @[CustomALU.scala 126:20 158:15]
  wire  result_8 = lut_8_out; // @[CustomALU.scala 126:20 158:15]
  wire  result_11 = lut_11_out; // @[CustomALU.scala 126:20 158:15]
  wire  result_10 = lut_10_out; // @[CustomALU.scala 126:20 158:15]
  wire  result_13 = lut_13_out; // @[CustomALU.scala 126:20 158:15]
  wire  result_12 = lut_12_out; // @[CustomALU.scala 126:20 158:15]
  wire  result_15 = lut_15_out; // @[CustomALU.scala 126:20 158:15]
  wire  result_14 = lut_14_out; // @[CustomALU.scala 126:20 158:15]
  wire [7:0] io_out_hi = {result_15,result_14,result_13,result_12,result_11,result_10,result_9,result_8}; // @[CustomALU.scala 161:20]
  WrappedLut6 #(.INIT(0)) lut_0 ( // @[CustomALU.scala 129:21]
    .clock(lut_0_clock),
    .we(lut_0_we),
    .data(lut_0_data),
    .a0(lut_0_a0),
    .a1(lut_0_a1),
    .a2(lut_0_a2),
    .a3(lut_0_a3),
    .a4(lut_0_a4),
    .a5(lut_0_a5),
    .out(lut_0_out)
  );
  WrappedLut6 #(.INIT(0)) lut_1 ( // @[CustomALU.scala 129:21]
    .clock(lut_1_clock),
    .we(lut_1_we),
    .data(lut_1_data),
    .a0(lut_1_a0),
    .a1(lut_1_a1),
    .a2(lut_1_a2),
    .a3(lut_1_a3),
    .a4(lut_1_a4),
    .a5(lut_1_a5),
    .out(lut_1_out)
  );
  WrappedLut6 #(.INIT(0)) lut_2 ( // @[CustomALU.scala 129:21]
    .clock(lut_2_clock),
    .we(lut_2_we),
    .data(lut_2_data),
    .a0(lut_2_a0),
    .a1(lut_2_a1),
    .a2(lut_2_a2),
    .a3(lut_2_a3),
    .a4(lut_2_a4),
    .a5(lut_2_a5),
    .out(lut_2_out)
  );
  WrappedLut6 #(.INIT(0)) lut_3 ( // @[CustomALU.scala 129:21]
    .clock(lut_3_clock),
    .we(lut_3_we),
    .data(lut_3_data),
    .a0(lut_3_a0),
    .a1(lut_3_a1),
    .a2(lut_3_a2),
    .a3(lut_3_a3),
    .a4(lut_3_a4),
    .a5(lut_3_a5),
    .out(lut_3_out)
  );
  WrappedLut6 #(.INIT(0)) lut_4 ( // @[CustomALU.scala 129:21]
    .clock(lut_4_clock),
    .we(lut_4_we),
    .data(lut_4_data),
    .a0(lut_4_a0),
    .a1(lut_4_a1),
    .a2(lut_4_a2),
    .a3(lut_4_a3),
    .a4(lut_4_a4),
    .a5(lut_4_a5),
    .out(lut_4_out)
  );
  WrappedLut6 #(.INIT(0)) lut_5 ( // @[CustomALU.scala 129:21]
    .clock(lut_5_clock),
    .we(lut_5_we),
    .data(lut_5_data),
    .a0(lut_5_a0),
    .a1(lut_5_a1),
    .a2(lut_5_a2),
    .a3(lut_5_a3),
    .a4(lut_5_a4),
    .a5(lut_5_a5),
    .out(lut_5_out)
  );
  WrappedLut6 #(.INIT(0)) lut_6 ( // @[CustomALU.scala 129:21]
    .clock(lut_6_clock),
    .we(lut_6_we),
    .data(lut_6_data),
    .a0(lut_6_a0),
    .a1(lut_6_a1),
    .a2(lut_6_a2),
    .a3(lut_6_a3),
    .a4(lut_6_a4),
    .a5(lut_6_a5),
    .out(lut_6_out)
  );
  WrappedLut6 #(.INIT(0)) lut_7 ( // @[CustomALU.scala 129:21]
    .clock(lut_7_clock),
    .we(lut_7_we),
    .data(lut_7_data),
    .a0(lut_7_a0),
    .a1(lut_7_a1),
    .a2(lut_7_a2),
    .a3(lut_7_a3),
    .a4(lut_7_a4),
    .a5(lut_7_a5),
    .out(lut_7_out)
  );
  WrappedLut6 #(.INIT(0)) lut_8 ( // @[CustomALU.scala 129:21]
    .clock(lut_8_clock),
    .we(lut_8_we),
    .data(lut_8_data),
    .a0(lut_8_a0),
    .a1(lut_8_a1),
    .a2(lut_8_a2),
    .a3(lut_8_a3),
    .a4(lut_8_a4),
    .a5(lut_8_a5),
    .out(lut_8_out)
  );
  WrappedLut6 #(.INIT(0)) lut_9 ( // @[CustomALU.scala 129:21]
    .clock(lut_9_clock),
    .we(lut_9_we),
    .data(lut_9_data),
    .a0(lut_9_a0),
    .a1(lut_9_a1),
    .a2(lut_9_a2),
    .a3(lut_9_a3),
    .a4(lut_9_a4),
    .a5(lut_9_a5),
    .out(lut_9_out)
  );
  WrappedLut6 #(.INIT(0)) lut_10 ( // @[CustomALU.scala 129:21]
    .clock(lut_10_clock),
    .we(lut_10_we),
    .data(lut_10_data),
    .a0(lut_10_a0),
    .a1(lut_10_a1),
    .a2(lut_10_a2),
    .a3(lut_10_a3),
    .a4(lut_10_a4),
    .a5(lut_10_a5),
    .out(lut_10_out)
  );
  WrappedLut6 #(.INIT(0)) lut_11 ( // @[CustomALU.scala 129:21]
    .clock(lut_11_clock),
    .we(lut_11_we),
    .data(lut_11_data),
    .a0(lut_11_a0),
    .a1(lut_11_a1),
    .a2(lut_11_a2),
    .a3(lut_11_a3),
    .a4(lut_11_a4),
    .a5(lut_11_a5),
    .out(lut_11_out)
  );
  WrappedLut6 #(.INIT(0)) lut_12 ( // @[CustomALU.scala 129:21]
    .clock(lut_12_clock),
    .we(lut_12_we),
    .data(lut_12_data),
    .a0(lut_12_a0),
    .a1(lut_12_a1),
    .a2(lut_12_a2),
    .a3(lut_12_a3),
    .a4(lut_12_a4),
    .a5(lut_12_a5),
    .out(lut_12_out)
  );
  WrappedLut6 #(.INIT(0)) lut_13 ( // @[CustomALU.scala 129:21]
    .clock(lut_13_clock),
    .we(lut_13_we),
    .data(lut_13_data),
    .a0(lut_13_a0),
    .a1(lut_13_a1),
    .a2(lut_13_a2),
    .a3(lut_13_a3),
    .a4(lut_13_a4),
    .a5(lut_13_a5),
    .out(lut_13_out)
  );
  WrappedLut6 #(.INIT(0)) lut_14 ( // @[CustomALU.scala 129:21]
    .clock(lut_14_clock),
    .we(lut_14_we),
    .data(lut_14_data),
    .a0(lut_14_a0),
    .a1(lut_14_a1),
    .a2(lut_14_a2),
    .a3(lut_14_a3),
    .a4(lut_14_a4),
    .a5(lut_14_a5),
    .out(lut_14_out)
  );
  WrappedLut6 #(.INIT(0)) lut_15 ( // @[CustomALU.scala 129:21]
    .clock(lut_15_clock),
    .we(lut_15_we),
    .data(lut_15_data),
    .a0(lut_15_a0),
    .a1(lut_15_a1),
    .a2(lut_15_a2),
    .a3(lut_15_a3),
    .a4(lut_15_a4),
    .a5(lut_15_a5),
    .out(lut_15_out)
  );
  assign io_out = {io_out_hi,io_out_lo}; // @[CustomALU.scala 161:20]
  assign lut_0_clock = clock; // @[CustomALU.scala 149:18]
  assign lut_0_we = io_config_writeEnable; // @[CustomALU.scala 150:15]
  assign lut_0_data = io_config_loadData[0]; // @[CustomALU.scala 151:38]
  assign lut_0_a0 = io_rsx_0[0]; // @[CustomALU.scala 152:28]
  assign lut_0_a1 = io_rsx_1[0]; // @[CustomALU.scala 153:28]
  assign lut_0_a2 = io_rsx_2[0]; // @[CustomALU.scala 154:28]
  assign lut_0_a3 = io_rsx_3[0]; // @[CustomALU.scala 155:28]
  assign lut_0_a4 = 1'h0; // @[CustomALU.scala 156:15]
  assign lut_0_a5 = 1'h0; // @[CustomALU.scala 157:15]
  assign lut_1_clock = clock; // @[CustomALU.scala 149:18]
  assign lut_1_we = io_config_writeEnable; // @[CustomALU.scala 150:15]
  assign lut_1_data = io_config_loadData[1]; // @[CustomALU.scala 151:38]
  assign lut_1_a0 = io_rsx_0[1]; // @[CustomALU.scala 152:28]
  assign lut_1_a1 = io_rsx_1[1]; // @[CustomALU.scala 153:28]
  assign lut_1_a2 = io_rsx_2[1]; // @[CustomALU.scala 154:28]
  assign lut_1_a3 = io_rsx_3[1]; // @[CustomALU.scala 155:28]
  assign lut_1_a4 = 1'h0; // @[CustomALU.scala 156:15]
  assign lut_1_a5 = 1'h0; // @[CustomALU.scala 157:15]
  assign lut_2_clock = clock; // @[CustomALU.scala 149:18]
  assign lut_2_we = io_config_writeEnable; // @[CustomALU.scala 150:15]
  assign lut_2_data = io_config_loadData[2]; // @[CustomALU.scala 151:38]
  assign lut_2_a0 = io_rsx_0[2]; // @[CustomALU.scala 152:28]
  assign lut_2_a1 = io_rsx_1[2]; // @[CustomALU.scala 153:28]
  assign lut_2_a2 = io_rsx_2[2]; // @[CustomALU.scala 154:28]
  assign lut_2_a3 = io_rsx_3[2]; // @[CustomALU.scala 155:28]
  assign lut_2_a4 = 1'h0; // @[CustomALU.scala 156:15]
  assign lut_2_a5 = 1'h0; // @[CustomALU.scala 157:15]
  assign lut_3_clock = clock; // @[CustomALU.scala 149:18]
  assign lut_3_we = io_config_writeEnable; // @[CustomALU.scala 150:15]
  assign lut_3_data = io_config_loadData[3]; // @[CustomALU.scala 151:38]
  assign lut_3_a0 = io_rsx_0[3]; // @[CustomALU.scala 152:28]
  assign lut_3_a1 = io_rsx_1[3]; // @[CustomALU.scala 153:28]
  assign lut_3_a2 = io_rsx_2[3]; // @[CustomALU.scala 154:28]
  assign lut_3_a3 = io_rsx_3[3]; // @[CustomALU.scala 155:28]
  assign lut_3_a4 = 1'h0; // @[CustomALU.scala 156:15]
  assign lut_3_a5 = 1'h0; // @[CustomALU.scala 157:15]
  assign lut_4_clock = clock; // @[CustomALU.scala 149:18]
  assign lut_4_we = io_config_writeEnable; // @[CustomALU.scala 150:15]
  assign lut_4_data = io_config_loadData[4]; // @[CustomALU.scala 151:38]
  assign lut_4_a0 = io_rsx_0[4]; // @[CustomALU.scala 152:28]
  assign lut_4_a1 = io_rsx_1[4]; // @[CustomALU.scala 153:28]
  assign lut_4_a2 = io_rsx_2[4]; // @[CustomALU.scala 154:28]
  assign lut_4_a3 = io_rsx_3[4]; // @[CustomALU.scala 155:28]
  assign lut_4_a4 = 1'h0; // @[CustomALU.scala 156:15]
  assign lut_4_a5 = 1'h0; // @[CustomALU.scala 157:15]
  assign lut_5_clock = clock; // @[CustomALU.scala 149:18]
  assign lut_5_we = io_config_writeEnable; // @[CustomALU.scala 150:15]
  assign lut_5_data = io_config_loadData[5]; // @[CustomALU.scala 151:38]
  assign lut_5_a0 = io_rsx_0[5]; // @[CustomALU.scala 152:28]
  assign lut_5_a1 = io_rsx_1[5]; // @[CustomALU.scala 153:28]
  assign lut_5_a2 = io_rsx_2[5]; // @[CustomALU.scala 154:28]
  assign lut_5_a3 = io_rsx_3[5]; // @[CustomALU.scala 155:28]
  assign lut_5_a4 = 1'h0; // @[CustomALU.scala 156:15]
  assign lut_5_a5 = 1'h0; // @[CustomALU.scala 157:15]
  assign lut_6_clock = clock; // @[CustomALU.scala 149:18]
  assign lut_6_we = io_config_writeEnable; // @[CustomALU.scala 150:15]
  assign lut_6_data = io_config_loadData[6]; // @[CustomALU.scala 151:38]
  assign lut_6_a0 = io_rsx_0[6]; // @[CustomALU.scala 152:28]
  assign lut_6_a1 = io_rsx_1[6]; // @[CustomALU.scala 153:28]
  assign lut_6_a2 = io_rsx_2[6]; // @[CustomALU.scala 154:28]
  assign lut_6_a3 = io_rsx_3[6]; // @[CustomALU.scala 155:28]
  assign lut_6_a4 = 1'h0; // @[CustomALU.scala 156:15]
  assign lut_6_a5 = 1'h0; // @[CustomALU.scala 157:15]
  assign lut_7_clock = clock; // @[CustomALU.scala 149:18]
  assign lut_7_we = io_config_writeEnable; // @[CustomALU.scala 150:15]
  assign lut_7_data = io_config_loadData[7]; // @[CustomALU.scala 151:38]
  assign lut_7_a0 = io_rsx_0[7]; // @[CustomALU.scala 152:28]
  assign lut_7_a1 = io_rsx_1[7]; // @[CustomALU.scala 153:28]
  assign lut_7_a2 = io_rsx_2[7]; // @[CustomALU.scala 154:28]
  assign lut_7_a3 = io_rsx_3[7]; // @[CustomALU.scala 155:28]
  assign lut_7_a4 = 1'h0; // @[CustomALU.scala 156:15]
  assign lut_7_a5 = 1'h0; // @[CustomALU.scala 157:15]
  assign lut_8_clock = clock; // @[CustomALU.scala 149:18]
  assign lut_8_we = io_config_writeEnable; // @[CustomALU.scala 150:15]
  assign lut_8_data = io_config_loadData[8]; // @[CustomALU.scala 151:38]
  assign lut_8_a0 = io_rsx_0[8]; // @[CustomALU.scala 152:28]
  assign lut_8_a1 = io_rsx_1[8]; // @[CustomALU.scala 153:28]
  assign lut_8_a2 = io_rsx_2[8]; // @[CustomALU.scala 154:28]
  assign lut_8_a3 = io_rsx_3[8]; // @[CustomALU.scala 155:28]
  assign lut_8_a4 = 1'h0; // @[CustomALU.scala 156:15]
  assign lut_8_a5 = 1'h0; // @[CustomALU.scala 157:15]
  assign lut_9_clock = clock; // @[CustomALU.scala 149:18]
  assign lut_9_we = io_config_writeEnable; // @[CustomALU.scala 150:15]
  assign lut_9_data = io_config_loadData[9]; // @[CustomALU.scala 151:38]
  assign lut_9_a0 = io_rsx_0[9]; // @[CustomALU.scala 152:28]
  assign lut_9_a1 = io_rsx_1[9]; // @[CustomALU.scala 153:28]
  assign lut_9_a2 = io_rsx_2[9]; // @[CustomALU.scala 154:28]
  assign lut_9_a3 = io_rsx_3[9]; // @[CustomALU.scala 155:28]
  assign lut_9_a4 = 1'h0; // @[CustomALU.scala 156:15]
  assign lut_9_a5 = 1'h0; // @[CustomALU.scala 157:15]
  assign lut_10_clock = clock; // @[CustomALU.scala 149:18]
  assign lut_10_we = io_config_writeEnable; // @[CustomALU.scala 150:15]
  assign lut_10_data = io_config_loadData[10]; // @[CustomALU.scala 151:38]
  assign lut_10_a0 = io_rsx_0[10]; // @[CustomALU.scala 152:28]
  assign lut_10_a1 = io_rsx_1[10]; // @[CustomALU.scala 153:28]
  assign lut_10_a2 = io_rsx_2[10]; // @[CustomALU.scala 154:28]
  assign lut_10_a3 = io_rsx_3[10]; // @[CustomALU.scala 155:28]
  assign lut_10_a4 = 1'h0; // @[CustomALU.scala 156:15]
  assign lut_10_a5 = 1'h0; // @[CustomALU.scala 157:15]
  assign lut_11_clock = clock; // @[CustomALU.scala 149:18]
  assign lut_11_we = io_config_writeEnable; // @[CustomALU.scala 150:15]
  assign lut_11_data = io_config_loadData[11]; // @[CustomALU.scala 151:38]
  assign lut_11_a0 = io_rsx_0[11]; // @[CustomALU.scala 152:28]
  assign lut_11_a1 = io_rsx_1[11]; // @[CustomALU.scala 153:28]
  assign lut_11_a2 = io_rsx_2[11]; // @[CustomALU.scala 154:28]
  assign lut_11_a3 = io_rsx_3[11]; // @[CustomALU.scala 155:28]
  assign lut_11_a4 = 1'h0; // @[CustomALU.scala 156:15]
  assign lut_11_a5 = 1'h0; // @[CustomALU.scala 157:15]
  assign lut_12_clock = clock; // @[CustomALU.scala 149:18]
  assign lut_12_we = io_config_writeEnable; // @[CustomALU.scala 150:15]
  assign lut_12_data = io_config_loadData[12]; // @[CustomALU.scala 151:38]
  assign lut_12_a0 = io_rsx_0[12]; // @[CustomALU.scala 152:28]
  assign lut_12_a1 = io_rsx_1[12]; // @[CustomALU.scala 153:28]
  assign lut_12_a2 = io_rsx_2[12]; // @[CustomALU.scala 154:28]
  assign lut_12_a3 = io_rsx_3[12]; // @[CustomALU.scala 155:28]
  assign lut_12_a4 = 1'h0; // @[CustomALU.scala 156:15]
  assign lut_12_a5 = 1'h0; // @[CustomALU.scala 157:15]
  assign lut_13_clock = clock; // @[CustomALU.scala 149:18]
  assign lut_13_we = io_config_writeEnable; // @[CustomALU.scala 150:15]
  assign lut_13_data = io_config_loadData[13]; // @[CustomALU.scala 151:38]
  assign lut_13_a0 = io_rsx_0[13]; // @[CustomALU.scala 152:28]
  assign lut_13_a1 = io_rsx_1[13]; // @[CustomALU.scala 153:28]
  assign lut_13_a2 = io_rsx_2[13]; // @[CustomALU.scala 154:28]
  assign lut_13_a3 = io_rsx_3[13]; // @[CustomALU.scala 155:28]
  assign lut_13_a4 = 1'h0; // @[CustomALU.scala 156:15]
  assign lut_13_a5 = 1'h0; // @[CustomALU.scala 157:15]
  assign lut_14_clock = clock; // @[CustomALU.scala 149:18]
  assign lut_14_we = io_config_writeEnable; // @[CustomALU.scala 150:15]
  assign lut_14_data = io_config_loadData[14]; // @[CustomALU.scala 151:38]
  assign lut_14_a0 = io_rsx_0[14]; // @[CustomALU.scala 152:28]
  assign lut_14_a1 = io_rsx_1[14]; // @[CustomALU.scala 153:28]
  assign lut_14_a2 = io_rsx_2[14]; // @[CustomALU.scala 154:28]
  assign lut_14_a3 = io_rsx_3[14]; // @[CustomALU.scala 155:28]
  assign lut_14_a4 = 1'h0; // @[CustomALU.scala 156:15]
  assign lut_14_a5 = 1'h0; // @[CustomALU.scala 157:15]
  assign lut_15_clock = clock; // @[CustomALU.scala 149:18]
  assign lut_15_we = io_config_writeEnable; // @[CustomALU.scala 150:15]
  assign lut_15_data = io_config_loadData[15]; // @[CustomALU.scala 151:38]
  assign lut_15_a0 = io_rsx_0[15]; // @[CustomALU.scala 152:28]
  assign lut_15_a1 = io_rsx_1[15]; // @[CustomALU.scala 153:28]
  assign lut_15_a2 = io_rsx_2[15]; // @[CustomALU.scala 154:28]
  assign lut_15_a3 = io_rsx_3[15]; // @[CustomALU.scala 155:28]
  assign lut_15_a4 = 1'h0; // @[CustomALU.scala 156:15]
  assign lut_15_a5 = 1'h0; // @[CustomALU.scala 157:15]
endmodule
module CustomAlu(
  input         clock,
  input         io_config_0_writeEnable,
  input  [15:0] io_config_0_loadData,
  input         io_config_1_writeEnable,
  input  [15:0] io_config_1_loadData,
  input         io_config_2_writeEnable,
  input  [15:0] io_config_2_loadData,
  input         io_config_3_writeEnable,
  input  [15:0] io_config_3_loadData,
  input         io_config_4_writeEnable,
  input  [15:0] io_config_4_loadData,
  input         io_config_5_writeEnable,
  input  [15:0] io_config_5_loadData,
  input         io_config_6_writeEnable,
  input  [15:0] io_config_6_loadData,
  input         io_config_7_writeEnable,
  input  [15:0] io_config_7_loadData,
  input         io_config_8_writeEnable,
  input  [15:0] io_config_8_loadData,
  input         io_config_9_writeEnable,
  input  [15:0] io_config_9_loadData,
  input         io_config_10_writeEnable,
  input  [15:0] io_config_10_loadData,
  input         io_config_11_writeEnable,
  input  [15:0] io_config_11_loadData,
  input         io_config_12_writeEnable,
  input  [15:0] io_config_12_loadData,
  input         io_config_13_writeEnable,
  input  [15:0] io_config_13_loadData,
  input         io_config_14_writeEnable,
  input  [15:0] io_config_14_loadData,
  input         io_config_15_writeEnable,
  input  [15:0] io_config_15_loadData,
  input         io_config_16_writeEnable,
  input  [15:0] io_config_16_loadData,
  input         io_config_17_writeEnable,
  input  [15:0] io_config_17_loadData,
  input         io_config_18_writeEnable,
  input  [15:0] io_config_18_loadData,
  input         io_config_19_writeEnable,
  input  [15:0] io_config_19_loadData,
  input         io_config_20_writeEnable,
  input  [15:0] io_config_20_loadData,
  input         io_config_21_writeEnable,
  input  [15:0] io_config_21_loadData,
  input         io_config_22_writeEnable,
  input  [15:0] io_config_22_loadData,
  input         io_config_23_writeEnable,
  input  [15:0] io_config_23_loadData,
  input         io_config_24_writeEnable,
  input  [15:0] io_config_24_loadData,
  input         io_config_25_writeEnable,
  input  [15:0] io_config_25_loadData,
  input         io_config_26_writeEnable,
  input  [15:0] io_config_26_loadData,
  input         io_config_27_writeEnable,
  input  [15:0] io_config_27_loadData,
  input         io_config_28_writeEnable,
  input  [15:0] io_config_28_loadData,
  input         io_config_29_writeEnable,
  input  [15:0] io_config_29_loadData,
  input         io_config_30_writeEnable,
  input  [15:0] io_config_30_loadData,
  input         io_config_31_writeEnable,
  input  [15:0] io_config_31_loadData,
  input  [15:0] io_rsx_0,
  input  [15:0] io_rsx_1,
  input  [15:0] io_rsx_2,
  input  [15:0] io_rsx_3,
  input  [4:0]  io_selector,
  output [15:0] io_out
);
  wire  customFunct_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_1_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_1_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_1_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_1_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_1_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_1_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_1_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_1_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_2_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_2_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_2_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_2_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_2_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_2_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_2_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_2_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_3_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_3_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_3_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_3_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_3_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_3_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_3_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_3_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_4_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_4_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_4_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_4_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_4_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_4_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_4_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_4_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_5_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_5_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_5_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_5_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_5_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_5_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_5_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_5_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_6_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_6_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_6_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_6_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_6_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_6_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_6_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_6_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_7_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_7_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_7_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_7_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_7_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_7_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_7_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_7_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_8_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_8_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_8_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_8_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_8_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_8_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_8_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_8_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_9_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_9_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_9_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_9_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_9_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_9_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_9_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_9_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_10_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_10_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_10_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_10_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_10_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_10_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_10_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_10_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_11_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_11_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_11_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_11_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_11_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_11_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_11_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_11_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_12_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_12_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_12_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_12_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_12_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_12_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_12_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_12_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_13_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_13_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_13_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_13_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_13_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_13_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_13_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_13_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_14_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_14_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_14_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_14_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_14_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_14_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_14_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_14_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_15_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_15_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_15_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_15_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_15_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_15_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_15_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_15_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_16_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_16_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_16_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_16_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_16_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_16_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_16_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_16_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_17_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_17_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_17_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_17_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_17_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_17_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_17_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_17_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_18_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_18_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_18_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_18_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_18_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_18_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_18_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_18_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_19_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_19_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_19_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_19_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_19_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_19_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_19_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_19_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_20_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_20_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_20_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_20_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_20_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_20_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_20_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_20_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_21_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_21_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_21_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_21_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_21_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_21_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_21_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_21_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_22_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_22_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_22_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_22_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_22_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_22_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_22_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_22_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_23_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_23_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_23_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_23_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_23_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_23_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_23_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_23_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_24_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_24_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_24_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_24_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_24_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_24_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_24_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_24_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_25_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_25_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_25_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_25_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_25_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_25_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_25_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_25_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_26_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_26_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_26_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_26_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_26_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_26_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_26_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_26_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_27_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_27_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_27_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_27_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_27_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_27_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_27_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_27_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_28_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_28_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_28_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_28_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_28_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_28_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_28_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_28_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_29_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_29_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_29_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_29_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_29_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_29_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_29_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_29_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_30_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_30_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_30_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_30_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_30_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_30_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_30_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_30_io_out; // @[CustomALU.scala 51:31]
  wire  customFunct_31_clock; // @[CustomALU.scala 51:31]
  wire  customFunct_31_io_config_writeEnable; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_31_io_config_loadData; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_31_io_rsx_0; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_31_io_rsx_1; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_31_io_rsx_2; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_31_io_rsx_3; // @[CustomALU.scala 51:31]
  wire [15:0] customFunct_31_io_out; // @[CustomALU.scala 51:31]
  wire [15:0] results_0 = customFunct_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] results_1 = customFunct_1_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_1 = 5'h1 == io_selector ? results_1 : results_0; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_2 = customFunct_2_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_2 = 5'h2 == io_selector ? results_2 : _GEN_1; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_3 = customFunct_3_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_3 = 5'h3 == io_selector ? results_3 : _GEN_2; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_4 = customFunct_4_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_4 = 5'h4 == io_selector ? results_4 : _GEN_3; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_5 = customFunct_5_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_5 = 5'h5 == io_selector ? results_5 : _GEN_4; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_6 = customFunct_6_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_6 = 5'h6 == io_selector ? results_6 : _GEN_5; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_7 = customFunct_7_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_7 = 5'h7 == io_selector ? results_7 : _GEN_6; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_8 = customFunct_8_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_8 = 5'h8 == io_selector ? results_8 : _GEN_7; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_9 = customFunct_9_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_9 = 5'h9 == io_selector ? results_9 : _GEN_8; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_10 = customFunct_10_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_10 = 5'ha == io_selector ? results_10 : _GEN_9; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_11 = customFunct_11_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_11 = 5'hb == io_selector ? results_11 : _GEN_10; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_12 = customFunct_12_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_12 = 5'hc == io_selector ? results_12 : _GEN_11; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_13 = customFunct_13_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_13 = 5'hd == io_selector ? results_13 : _GEN_12; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_14 = customFunct_14_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_14 = 5'he == io_selector ? results_14 : _GEN_13; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_15 = customFunct_15_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_15 = 5'hf == io_selector ? results_15 : _GEN_14; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_16 = customFunct_16_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_16 = 5'h10 == io_selector ? results_16 : _GEN_15; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_17 = customFunct_17_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_17 = 5'h11 == io_selector ? results_17 : _GEN_16; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_18 = customFunct_18_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_18 = 5'h12 == io_selector ? results_18 : _GEN_17; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_19 = customFunct_19_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_19 = 5'h13 == io_selector ? results_19 : _GEN_18; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_20 = customFunct_20_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_20 = 5'h14 == io_selector ? results_20 : _GEN_19; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_21 = customFunct_21_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_21 = 5'h15 == io_selector ? results_21 : _GEN_20; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_22 = customFunct_22_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_22 = 5'h16 == io_selector ? results_22 : _GEN_21; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_23 = customFunct_23_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_23 = 5'h17 == io_selector ? results_23 : _GEN_22; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_24 = customFunct_24_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_24 = 5'h18 == io_selector ? results_24 : _GEN_23; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_25 = customFunct_25_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_25 = 5'h19 == io_selector ? results_25 : _GEN_24; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_26 = customFunct_26_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_26 = 5'h1a == io_selector ? results_26 : _GEN_25; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_27 = customFunct_27_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_27 = 5'h1b == io_selector ? results_27 : _GEN_26; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_28 = customFunct_28_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_28 = 5'h1c == io_selector ? results_28 : _GEN_27; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_29 = customFunct_29_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_29 = 5'h1d == io_selector ? results_29 : _GEN_28; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_30 = customFunct_30_io_out; // @[CustomALU.scala 47:23 54:18]
  wire [15:0] _GEN_30 = 5'h1e == io_selector ? results_30 : _GEN_29; // @[CustomALU.scala 58:{12,12}]
  wire [15:0] results_31 = customFunct_31_io_out; // @[CustomALU.scala 47:23 54:18]
  CustomFunction customFunct ( // @[CustomALU.scala 51:31]
    .clock(customFunct_clock),
    .io_config_writeEnable(customFunct_io_config_writeEnable),
    .io_config_loadData(customFunct_io_config_loadData),
    .io_rsx_0(customFunct_io_rsx_0),
    .io_rsx_1(customFunct_io_rsx_1),
    .io_rsx_2(customFunct_io_rsx_2),
    .io_rsx_3(customFunct_io_rsx_3),
    .io_out(customFunct_io_out)
  );
  CustomFunction customFunct_1 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_1_clock),
    .io_config_writeEnable(customFunct_1_io_config_writeEnable),
    .io_config_loadData(customFunct_1_io_config_loadData),
    .io_rsx_0(customFunct_1_io_rsx_0),
    .io_rsx_1(customFunct_1_io_rsx_1),
    .io_rsx_2(customFunct_1_io_rsx_2),
    .io_rsx_3(customFunct_1_io_rsx_3),
    .io_out(customFunct_1_io_out)
  );
  CustomFunction customFunct_2 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_2_clock),
    .io_config_writeEnable(customFunct_2_io_config_writeEnable),
    .io_config_loadData(customFunct_2_io_config_loadData),
    .io_rsx_0(customFunct_2_io_rsx_0),
    .io_rsx_1(customFunct_2_io_rsx_1),
    .io_rsx_2(customFunct_2_io_rsx_2),
    .io_rsx_3(customFunct_2_io_rsx_3),
    .io_out(customFunct_2_io_out)
  );
  CustomFunction customFunct_3 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_3_clock),
    .io_config_writeEnable(customFunct_3_io_config_writeEnable),
    .io_config_loadData(customFunct_3_io_config_loadData),
    .io_rsx_0(customFunct_3_io_rsx_0),
    .io_rsx_1(customFunct_3_io_rsx_1),
    .io_rsx_2(customFunct_3_io_rsx_2),
    .io_rsx_3(customFunct_3_io_rsx_3),
    .io_out(customFunct_3_io_out)
  );
  CustomFunction customFunct_4 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_4_clock),
    .io_config_writeEnable(customFunct_4_io_config_writeEnable),
    .io_config_loadData(customFunct_4_io_config_loadData),
    .io_rsx_0(customFunct_4_io_rsx_0),
    .io_rsx_1(customFunct_4_io_rsx_1),
    .io_rsx_2(customFunct_4_io_rsx_2),
    .io_rsx_3(customFunct_4_io_rsx_3),
    .io_out(customFunct_4_io_out)
  );
  CustomFunction customFunct_5 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_5_clock),
    .io_config_writeEnable(customFunct_5_io_config_writeEnable),
    .io_config_loadData(customFunct_5_io_config_loadData),
    .io_rsx_0(customFunct_5_io_rsx_0),
    .io_rsx_1(customFunct_5_io_rsx_1),
    .io_rsx_2(customFunct_5_io_rsx_2),
    .io_rsx_3(customFunct_5_io_rsx_3),
    .io_out(customFunct_5_io_out)
  );
  CustomFunction customFunct_6 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_6_clock),
    .io_config_writeEnable(customFunct_6_io_config_writeEnable),
    .io_config_loadData(customFunct_6_io_config_loadData),
    .io_rsx_0(customFunct_6_io_rsx_0),
    .io_rsx_1(customFunct_6_io_rsx_1),
    .io_rsx_2(customFunct_6_io_rsx_2),
    .io_rsx_3(customFunct_6_io_rsx_3),
    .io_out(customFunct_6_io_out)
  );
  CustomFunction customFunct_7 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_7_clock),
    .io_config_writeEnable(customFunct_7_io_config_writeEnable),
    .io_config_loadData(customFunct_7_io_config_loadData),
    .io_rsx_0(customFunct_7_io_rsx_0),
    .io_rsx_1(customFunct_7_io_rsx_1),
    .io_rsx_2(customFunct_7_io_rsx_2),
    .io_rsx_3(customFunct_7_io_rsx_3),
    .io_out(customFunct_7_io_out)
  );
  CustomFunction customFunct_8 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_8_clock),
    .io_config_writeEnable(customFunct_8_io_config_writeEnable),
    .io_config_loadData(customFunct_8_io_config_loadData),
    .io_rsx_0(customFunct_8_io_rsx_0),
    .io_rsx_1(customFunct_8_io_rsx_1),
    .io_rsx_2(customFunct_8_io_rsx_2),
    .io_rsx_3(customFunct_8_io_rsx_3),
    .io_out(customFunct_8_io_out)
  );
  CustomFunction customFunct_9 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_9_clock),
    .io_config_writeEnable(customFunct_9_io_config_writeEnable),
    .io_config_loadData(customFunct_9_io_config_loadData),
    .io_rsx_0(customFunct_9_io_rsx_0),
    .io_rsx_1(customFunct_9_io_rsx_1),
    .io_rsx_2(customFunct_9_io_rsx_2),
    .io_rsx_3(customFunct_9_io_rsx_3),
    .io_out(customFunct_9_io_out)
  );
  CustomFunction customFunct_10 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_10_clock),
    .io_config_writeEnable(customFunct_10_io_config_writeEnable),
    .io_config_loadData(customFunct_10_io_config_loadData),
    .io_rsx_0(customFunct_10_io_rsx_0),
    .io_rsx_1(customFunct_10_io_rsx_1),
    .io_rsx_2(customFunct_10_io_rsx_2),
    .io_rsx_3(customFunct_10_io_rsx_3),
    .io_out(customFunct_10_io_out)
  );
  CustomFunction customFunct_11 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_11_clock),
    .io_config_writeEnable(customFunct_11_io_config_writeEnable),
    .io_config_loadData(customFunct_11_io_config_loadData),
    .io_rsx_0(customFunct_11_io_rsx_0),
    .io_rsx_1(customFunct_11_io_rsx_1),
    .io_rsx_2(customFunct_11_io_rsx_2),
    .io_rsx_3(customFunct_11_io_rsx_3),
    .io_out(customFunct_11_io_out)
  );
  CustomFunction customFunct_12 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_12_clock),
    .io_config_writeEnable(customFunct_12_io_config_writeEnable),
    .io_config_loadData(customFunct_12_io_config_loadData),
    .io_rsx_0(customFunct_12_io_rsx_0),
    .io_rsx_1(customFunct_12_io_rsx_1),
    .io_rsx_2(customFunct_12_io_rsx_2),
    .io_rsx_3(customFunct_12_io_rsx_3),
    .io_out(customFunct_12_io_out)
  );
  CustomFunction customFunct_13 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_13_clock),
    .io_config_writeEnable(customFunct_13_io_config_writeEnable),
    .io_config_loadData(customFunct_13_io_config_loadData),
    .io_rsx_0(customFunct_13_io_rsx_0),
    .io_rsx_1(customFunct_13_io_rsx_1),
    .io_rsx_2(customFunct_13_io_rsx_2),
    .io_rsx_3(customFunct_13_io_rsx_3),
    .io_out(customFunct_13_io_out)
  );
  CustomFunction customFunct_14 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_14_clock),
    .io_config_writeEnable(customFunct_14_io_config_writeEnable),
    .io_config_loadData(customFunct_14_io_config_loadData),
    .io_rsx_0(customFunct_14_io_rsx_0),
    .io_rsx_1(customFunct_14_io_rsx_1),
    .io_rsx_2(customFunct_14_io_rsx_2),
    .io_rsx_3(customFunct_14_io_rsx_3),
    .io_out(customFunct_14_io_out)
  );
  CustomFunction customFunct_15 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_15_clock),
    .io_config_writeEnable(customFunct_15_io_config_writeEnable),
    .io_config_loadData(customFunct_15_io_config_loadData),
    .io_rsx_0(customFunct_15_io_rsx_0),
    .io_rsx_1(customFunct_15_io_rsx_1),
    .io_rsx_2(customFunct_15_io_rsx_2),
    .io_rsx_3(customFunct_15_io_rsx_3),
    .io_out(customFunct_15_io_out)
  );
  CustomFunction customFunct_16 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_16_clock),
    .io_config_writeEnable(customFunct_16_io_config_writeEnable),
    .io_config_loadData(customFunct_16_io_config_loadData),
    .io_rsx_0(customFunct_16_io_rsx_0),
    .io_rsx_1(customFunct_16_io_rsx_1),
    .io_rsx_2(customFunct_16_io_rsx_2),
    .io_rsx_3(customFunct_16_io_rsx_3),
    .io_out(customFunct_16_io_out)
  );
  CustomFunction customFunct_17 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_17_clock),
    .io_config_writeEnable(customFunct_17_io_config_writeEnable),
    .io_config_loadData(customFunct_17_io_config_loadData),
    .io_rsx_0(customFunct_17_io_rsx_0),
    .io_rsx_1(customFunct_17_io_rsx_1),
    .io_rsx_2(customFunct_17_io_rsx_2),
    .io_rsx_3(customFunct_17_io_rsx_3),
    .io_out(customFunct_17_io_out)
  );
  CustomFunction customFunct_18 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_18_clock),
    .io_config_writeEnable(customFunct_18_io_config_writeEnable),
    .io_config_loadData(customFunct_18_io_config_loadData),
    .io_rsx_0(customFunct_18_io_rsx_0),
    .io_rsx_1(customFunct_18_io_rsx_1),
    .io_rsx_2(customFunct_18_io_rsx_2),
    .io_rsx_3(customFunct_18_io_rsx_3),
    .io_out(customFunct_18_io_out)
  );
  CustomFunction customFunct_19 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_19_clock),
    .io_config_writeEnable(customFunct_19_io_config_writeEnable),
    .io_config_loadData(customFunct_19_io_config_loadData),
    .io_rsx_0(customFunct_19_io_rsx_0),
    .io_rsx_1(customFunct_19_io_rsx_1),
    .io_rsx_2(customFunct_19_io_rsx_2),
    .io_rsx_3(customFunct_19_io_rsx_3),
    .io_out(customFunct_19_io_out)
  );
  CustomFunction customFunct_20 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_20_clock),
    .io_config_writeEnable(customFunct_20_io_config_writeEnable),
    .io_config_loadData(customFunct_20_io_config_loadData),
    .io_rsx_0(customFunct_20_io_rsx_0),
    .io_rsx_1(customFunct_20_io_rsx_1),
    .io_rsx_2(customFunct_20_io_rsx_2),
    .io_rsx_3(customFunct_20_io_rsx_3),
    .io_out(customFunct_20_io_out)
  );
  CustomFunction customFunct_21 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_21_clock),
    .io_config_writeEnable(customFunct_21_io_config_writeEnable),
    .io_config_loadData(customFunct_21_io_config_loadData),
    .io_rsx_0(customFunct_21_io_rsx_0),
    .io_rsx_1(customFunct_21_io_rsx_1),
    .io_rsx_2(customFunct_21_io_rsx_2),
    .io_rsx_3(customFunct_21_io_rsx_3),
    .io_out(customFunct_21_io_out)
  );
  CustomFunction customFunct_22 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_22_clock),
    .io_config_writeEnable(customFunct_22_io_config_writeEnable),
    .io_config_loadData(customFunct_22_io_config_loadData),
    .io_rsx_0(customFunct_22_io_rsx_0),
    .io_rsx_1(customFunct_22_io_rsx_1),
    .io_rsx_2(customFunct_22_io_rsx_2),
    .io_rsx_3(customFunct_22_io_rsx_3),
    .io_out(customFunct_22_io_out)
  );
  CustomFunction customFunct_23 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_23_clock),
    .io_config_writeEnable(customFunct_23_io_config_writeEnable),
    .io_config_loadData(customFunct_23_io_config_loadData),
    .io_rsx_0(customFunct_23_io_rsx_0),
    .io_rsx_1(customFunct_23_io_rsx_1),
    .io_rsx_2(customFunct_23_io_rsx_2),
    .io_rsx_3(customFunct_23_io_rsx_3),
    .io_out(customFunct_23_io_out)
  );
  CustomFunction customFunct_24 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_24_clock),
    .io_config_writeEnable(customFunct_24_io_config_writeEnable),
    .io_config_loadData(customFunct_24_io_config_loadData),
    .io_rsx_0(customFunct_24_io_rsx_0),
    .io_rsx_1(customFunct_24_io_rsx_1),
    .io_rsx_2(customFunct_24_io_rsx_2),
    .io_rsx_3(customFunct_24_io_rsx_3),
    .io_out(customFunct_24_io_out)
  );
  CustomFunction customFunct_25 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_25_clock),
    .io_config_writeEnable(customFunct_25_io_config_writeEnable),
    .io_config_loadData(customFunct_25_io_config_loadData),
    .io_rsx_0(customFunct_25_io_rsx_0),
    .io_rsx_1(customFunct_25_io_rsx_1),
    .io_rsx_2(customFunct_25_io_rsx_2),
    .io_rsx_3(customFunct_25_io_rsx_3),
    .io_out(customFunct_25_io_out)
  );
  CustomFunction customFunct_26 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_26_clock),
    .io_config_writeEnable(customFunct_26_io_config_writeEnable),
    .io_config_loadData(customFunct_26_io_config_loadData),
    .io_rsx_0(customFunct_26_io_rsx_0),
    .io_rsx_1(customFunct_26_io_rsx_1),
    .io_rsx_2(customFunct_26_io_rsx_2),
    .io_rsx_3(customFunct_26_io_rsx_3),
    .io_out(customFunct_26_io_out)
  );
  CustomFunction customFunct_27 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_27_clock),
    .io_config_writeEnable(customFunct_27_io_config_writeEnable),
    .io_config_loadData(customFunct_27_io_config_loadData),
    .io_rsx_0(customFunct_27_io_rsx_0),
    .io_rsx_1(customFunct_27_io_rsx_1),
    .io_rsx_2(customFunct_27_io_rsx_2),
    .io_rsx_3(customFunct_27_io_rsx_3),
    .io_out(customFunct_27_io_out)
  );
  CustomFunction customFunct_28 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_28_clock),
    .io_config_writeEnable(customFunct_28_io_config_writeEnable),
    .io_config_loadData(customFunct_28_io_config_loadData),
    .io_rsx_0(customFunct_28_io_rsx_0),
    .io_rsx_1(customFunct_28_io_rsx_1),
    .io_rsx_2(customFunct_28_io_rsx_2),
    .io_rsx_3(customFunct_28_io_rsx_3),
    .io_out(customFunct_28_io_out)
  );
  CustomFunction customFunct_29 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_29_clock),
    .io_config_writeEnable(customFunct_29_io_config_writeEnable),
    .io_config_loadData(customFunct_29_io_config_loadData),
    .io_rsx_0(customFunct_29_io_rsx_0),
    .io_rsx_1(customFunct_29_io_rsx_1),
    .io_rsx_2(customFunct_29_io_rsx_2),
    .io_rsx_3(customFunct_29_io_rsx_3),
    .io_out(customFunct_29_io_out)
  );
  CustomFunction customFunct_30 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_30_clock),
    .io_config_writeEnable(customFunct_30_io_config_writeEnable),
    .io_config_loadData(customFunct_30_io_config_loadData),
    .io_rsx_0(customFunct_30_io_rsx_0),
    .io_rsx_1(customFunct_30_io_rsx_1),
    .io_rsx_2(customFunct_30_io_rsx_2),
    .io_rsx_3(customFunct_30_io_rsx_3),
    .io_out(customFunct_30_io_out)
  );
  CustomFunction customFunct_31 ( // @[CustomALU.scala 51:31]
    .clock(customFunct_31_clock),
    .io_config_writeEnable(customFunct_31_io_config_writeEnable),
    .io_config_loadData(customFunct_31_io_config_loadData),
    .io_rsx_0(customFunct_31_io_rsx_0),
    .io_rsx_1(customFunct_31_io_rsx_1),
    .io_rsx_2(customFunct_31_io_rsx_2),
    .io_rsx_3(customFunct_31_io_rsx_3),
    .io_out(customFunct_31_io_out)
  );
  assign io_out = 5'h1f == io_selector ? results_31 : _GEN_30; // @[CustomALU.scala 58:{12,12}]
  assign customFunct_clock = clock;
  assign customFunct_io_config_writeEnable = io_config_0_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_io_config_loadData = io_config_0_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_1_clock = clock;
  assign customFunct_1_io_config_writeEnable = io_config_1_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_1_io_config_loadData = io_config_1_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_1_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_1_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_1_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_1_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_2_clock = clock;
  assign customFunct_2_io_config_writeEnable = io_config_2_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_2_io_config_loadData = io_config_2_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_2_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_2_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_2_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_2_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_3_clock = clock;
  assign customFunct_3_io_config_writeEnable = io_config_3_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_3_io_config_loadData = io_config_3_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_3_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_3_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_3_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_3_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_4_clock = clock;
  assign customFunct_4_io_config_writeEnable = io_config_4_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_4_io_config_loadData = io_config_4_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_4_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_4_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_4_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_4_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_5_clock = clock;
  assign customFunct_5_io_config_writeEnable = io_config_5_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_5_io_config_loadData = io_config_5_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_5_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_5_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_5_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_5_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_6_clock = clock;
  assign customFunct_6_io_config_writeEnable = io_config_6_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_6_io_config_loadData = io_config_6_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_6_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_6_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_6_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_6_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_7_clock = clock;
  assign customFunct_7_io_config_writeEnable = io_config_7_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_7_io_config_loadData = io_config_7_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_7_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_7_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_7_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_7_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_8_clock = clock;
  assign customFunct_8_io_config_writeEnable = io_config_8_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_8_io_config_loadData = io_config_8_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_8_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_8_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_8_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_8_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_9_clock = clock;
  assign customFunct_9_io_config_writeEnable = io_config_9_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_9_io_config_loadData = io_config_9_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_9_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_9_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_9_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_9_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_10_clock = clock;
  assign customFunct_10_io_config_writeEnable = io_config_10_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_10_io_config_loadData = io_config_10_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_10_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_10_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_10_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_10_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_11_clock = clock;
  assign customFunct_11_io_config_writeEnable = io_config_11_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_11_io_config_loadData = io_config_11_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_11_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_11_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_11_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_11_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_12_clock = clock;
  assign customFunct_12_io_config_writeEnable = io_config_12_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_12_io_config_loadData = io_config_12_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_12_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_12_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_12_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_12_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_13_clock = clock;
  assign customFunct_13_io_config_writeEnable = io_config_13_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_13_io_config_loadData = io_config_13_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_13_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_13_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_13_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_13_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_14_clock = clock;
  assign customFunct_14_io_config_writeEnable = io_config_14_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_14_io_config_loadData = io_config_14_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_14_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_14_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_14_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_14_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_15_clock = clock;
  assign customFunct_15_io_config_writeEnable = io_config_15_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_15_io_config_loadData = io_config_15_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_15_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_15_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_15_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_15_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_16_clock = clock;
  assign customFunct_16_io_config_writeEnable = io_config_16_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_16_io_config_loadData = io_config_16_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_16_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_16_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_16_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_16_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_17_clock = clock;
  assign customFunct_17_io_config_writeEnable = io_config_17_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_17_io_config_loadData = io_config_17_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_17_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_17_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_17_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_17_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_18_clock = clock;
  assign customFunct_18_io_config_writeEnable = io_config_18_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_18_io_config_loadData = io_config_18_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_18_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_18_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_18_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_18_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_19_clock = clock;
  assign customFunct_19_io_config_writeEnable = io_config_19_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_19_io_config_loadData = io_config_19_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_19_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_19_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_19_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_19_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_20_clock = clock;
  assign customFunct_20_io_config_writeEnable = io_config_20_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_20_io_config_loadData = io_config_20_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_20_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_20_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_20_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_20_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_21_clock = clock;
  assign customFunct_21_io_config_writeEnable = io_config_21_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_21_io_config_loadData = io_config_21_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_21_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_21_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_21_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_21_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_22_clock = clock;
  assign customFunct_22_io_config_writeEnable = io_config_22_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_22_io_config_loadData = io_config_22_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_22_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_22_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_22_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_22_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_23_clock = clock;
  assign customFunct_23_io_config_writeEnable = io_config_23_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_23_io_config_loadData = io_config_23_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_23_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_23_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_23_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_23_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_24_clock = clock;
  assign customFunct_24_io_config_writeEnable = io_config_24_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_24_io_config_loadData = io_config_24_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_24_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_24_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_24_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_24_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_25_clock = clock;
  assign customFunct_25_io_config_writeEnable = io_config_25_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_25_io_config_loadData = io_config_25_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_25_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_25_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_25_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_25_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_26_clock = clock;
  assign customFunct_26_io_config_writeEnable = io_config_26_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_26_io_config_loadData = io_config_26_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_26_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_26_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_26_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_26_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_27_clock = clock;
  assign customFunct_27_io_config_writeEnable = io_config_27_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_27_io_config_loadData = io_config_27_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_27_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_27_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_27_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_27_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_28_clock = clock;
  assign customFunct_28_io_config_writeEnable = io_config_28_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_28_io_config_loadData = io_config_28_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_28_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_28_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_28_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_28_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_29_clock = clock;
  assign customFunct_29_io_config_writeEnable = io_config_29_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_29_io_config_loadData = io_config_29_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_29_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_29_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_29_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_29_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_30_clock = clock;
  assign customFunct_30_io_config_writeEnable = io_config_30_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_30_io_config_loadData = io_config_30_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_30_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_30_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_30_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_30_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
  assign customFunct_31_clock = clock;
  assign customFunct_31_io_config_writeEnable = io_config_31_writeEnable; // @[CustomALU.scala 52:29]
  assign customFunct_31_io_config_loadData = io_config_31_loadData; // @[CustomALU.scala 52:29]
  assign customFunct_31_io_rsx_0 = io_rsx_0; // @[CustomALU.scala 53:26]
  assign customFunct_31_io_rsx_1 = io_rsx_1; // @[CustomALU.scala 53:26]
  assign customFunct_31_io_rsx_2 = io_rsx_2; // @[CustomALU.scala 53:26]
  assign customFunct_31_io_rsx_3 = io_rsx_3; // @[CustomALU.scala 53:26]
endmodule
module StandardALUComb(
  input  [15:0] io_in_x,
  input  [15:0] io_in_y,
  input         io_in_carry,
  input         io_in_select,
  input  [15:0] io_in_mask,
  output [15:0] io_out,
  output        io_carry_out,
  input  [3:0]  io_funct
);
  wire [3:0] shamnt = io_in_y[3:0]; // @[StandardALU.scala 38:28]
  wire [16:0] sum_res_as_wider = {{1'd0}, io_in_x}; // @[StandardALU.scala 33:24 34:14]
  wire [16:0] sum_res_as_wider_1 = {{1'd0}, io_in_y}; // @[StandardALU.scala 33:24 34:14]
  wire [16:0] sum_res = sum_res_as_wider + sum_res_as_wider_1; // @[StandardALU.scala 39:38]
  wire [16:0] sum_with_carry_as_wider = {{16'd0}, io_in_carry}; // @[StandardALU.scala 33:24 34:14]
  wire [16:0] sum_with_carry = sum_res + sum_with_carry_as_wider; // @[StandardALU.scala 40:29]
  wire [15:0] _alu_res_T_2 = io_in_x - io_in_y; // @[StandardALU.scala 47:26]
  wire [15:0] _alu_res_T_3 = io_in_x & io_in_y; // @[StandardALU.scala 58:26]
  wire [15:0] _alu_res_T_4 = io_in_x | io_in_y; // @[StandardALU.scala 61:26]
  wire [15:0] _alu_res_T_5 = io_in_x ^ io_in_y; // @[StandardALU.scala 64:26]
  wire [30:0] _GEN_1 = {{15'd0}, io_in_x}; // @[StandardALU.scala 67:26]
  wire [30:0] _alu_res_T_6 = _GEN_1 << shamnt; // @[StandardALU.scala 67:26]
  wire [15:0] _alu_res_T_7 = io_in_x >> shamnt; // @[StandardALU.scala 70:26]
  wire [15:0] _alu_res_T_8 = io_in_x; // @[StandardALU.scala 73:27]
  wire [15:0] _alu_res_T_10 = $signed(io_in_x) >>> shamnt; // @[StandardALU.scala 73:45]
  wire [15:0] _alu_res_T_14 = io_in_y; // @[StandardALU.scala 82:44]
  wire [15:0] _GEN_0 = io_in_select ? io_in_y : io_in_x; // @[StandardALU.scala 85:26 86:17 88:17]
  wire [15:0] _GEN_2 = 4'hd == io_funct ? _GEN_0 : sum_with_carry[15:0]; // @[StandardALU.scala 42:20]
  wire [15:0] _GEN_3 = 4'hc == io_funct ? {{15'd0}, $signed(_alu_res_T_8) < $signed(_alu_res_T_14)} : _GEN_2; // @[StandardALU.scala 42:20 82:15]
  wire [15:0] _GEN_4 = 4'hb == io_funct ? {{15'd0}, io_in_x < io_in_y} : _GEN_3; // @[StandardALU.scala 42:20 79:15]
  wire [15:0] _GEN_5 = 4'ha == io_funct ? {{15'd0}, io_in_x == io_in_y} : _GEN_4; // @[StandardALU.scala 42:20 76:15]
  wire [15:0] _GEN_6 = 4'h9 == io_funct ? _alu_res_T_10 : _GEN_5; // @[StandardALU.scala 42:20 73:15]
  wire [15:0] _GEN_7 = 4'h8 == io_funct ? _alu_res_T_7 : _GEN_6; // @[StandardALU.scala 42:20 70:15]
  wire [30:0] _GEN_8 = 4'h7 == io_funct ? _alu_res_T_6 : {{15'd0}, _GEN_7}; // @[StandardALU.scala 42:20 67:15]
  wire [30:0] _GEN_9 = 4'h6 == io_funct ? {{15'd0}, _alu_res_T_5} : _GEN_8; // @[StandardALU.scala 42:20 64:15]
  wire [30:0] _GEN_10 = 4'h5 == io_funct ? {{15'd0}, _alu_res_T_4} : _GEN_9; // @[StandardALU.scala 42:20 61:15]
  wire [30:0] _GEN_11 = 4'h4 == io_funct ? {{15'd0}, _alu_res_T_3} : _GEN_10; // @[StandardALU.scala 42:20 58:15]
  wire [30:0] _GEN_12 = 4'h1 == io_funct ? {{15'd0}, _alu_res_T_2} : _GEN_11; // @[StandardALU.scala 42:20 47:15]
  wire [30:0] _GEN_13 = 4'h0 == io_funct ? {{15'd0}, sum_res[15:0]} : _GEN_12; // @[StandardALU.scala 42:20 44:15]
  wire [16:0] _io_carry_out_T = {{16'd0}, sum_with_carry[16]}; // @[StandardALU.scala 96:34]
  wire [15:0] alu_res = _GEN_13[15:0]; // @[StandardALU.scala 30:28]
  assign io_out = alu_res & io_in_mask; // @[StandardALU.scala 99:21]
  assign io_carry_out = _io_carry_out_T[0]; // @[StandardALU.scala 96:16]
endmodule
module ExecuteComb(
  input         clock,
  input  [10:0] io_pipe_in_rd,
  input  [10:0] io_pipe_in_rs4,
  input         io_pipe_in_opcode_cust,
  input         io_pipe_in_opcode_arith,
  input         io_pipe_in_opcode_lload,
  input         io_pipe_in_opcode_lstore,
  input         io_pipe_in_opcode_send,
  input         io_pipe_in_opcode_set,
  input         io_pipe_in_opcode_expect,
  input         io_pipe_in_opcode_predicate,
  input         io_pipe_in_opcode_set_carry,
  input         io_pipe_in_opcode_configure_luts_0,
  input         io_pipe_in_opcode_configure_luts_1,
  input         io_pipe_in_opcode_configure_luts_2,
  input         io_pipe_in_opcode_configure_luts_3,
  input         io_pipe_in_opcode_configure_luts_4,
  input         io_pipe_in_opcode_configure_luts_5,
  input         io_pipe_in_opcode_configure_luts_6,
  input         io_pipe_in_opcode_configure_luts_7,
  input         io_pipe_in_opcode_configure_luts_8,
  input         io_pipe_in_opcode_configure_luts_9,
  input         io_pipe_in_opcode_configure_luts_10,
  input         io_pipe_in_opcode_configure_luts_11,
  input         io_pipe_in_opcode_configure_luts_12,
  input         io_pipe_in_opcode_configure_luts_13,
  input         io_pipe_in_opcode_configure_luts_14,
  input         io_pipe_in_opcode_configure_luts_15,
  input         io_pipe_in_opcode_configure_luts_16,
  input         io_pipe_in_opcode_configure_luts_17,
  input         io_pipe_in_opcode_configure_luts_18,
  input         io_pipe_in_opcode_configure_luts_19,
  input         io_pipe_in_opcode_configure_luts_20,
  input         io_pipe_in_opcode_configure_luts_21,
  input         io_pipe_in_opcode_configure_luts_22,
  input         io_pipe_in_opcode_configure_luts_23,
  input         io_pipe_in_opcode_configure_luts_24,
  input         io_pipe_in_opcode_configure_luts_25,
  input         io_pipe_in_opcode_configure_luts_26,
  input         io_pipe_in_opcode_configure_luts_27,
  input         io_pipe_in_opcode_configure_luts_28,
  input         io_pipe_in_opcode_configure_luts_29,
  input         io_pipe_in_opcode_configure_luts_30,
  input         io_pipe_in_opcode_configure_luts_31,
  input         io_pipe_in_opcode_slice,
  input         io_pipe_in_opcode_mulh,
  input  [4:0]  io_pipe_in_funct,
  input  [15:0] io_pipe_in_immediate,
  input  [3:0]  io_pipe_in_slice_ofst,
  input  [15:0] io_regs_in_rs1,
  input  [15:0] io_regs_in_rs2,
  input  [15:0] io_regs_in_rs3,
  input  [15:0] io_regs_in_rs4,
  input         io_carry_in,
  output        io_pipe_out_opcode_cust,
  output        io_pipe_out_opcode_arith,
  output        io_pipe_out_opcode_lload,
  output        io_pipe_out_opcode_lstore,
  output        io_pipe_out_opcode_send,
  output        io_pipe_out_opcode_set,
  output        io_pipe_out_opcode_expect,
  output        io_pipe_out_opcode_slice,
  output        io_pipe_out_opcode_mulh,
  output [15:0] io_pipe_out_data,
  output [15:0] io_pipe_out_result,
  output [10:0] io_pipe_out_rd,
  output [15:0] io_pipe_out_immediate,
  output        io_pipe_out_pred,
  output [5:0]  io_carry_rd,
  output        io_carry_wen,
  output        io_carry_din,
  input  [15:0] io_lutdata_din_0,
  input  [15:0] io_lutdata_din_1,
  input  [15:0] io_lutdata_din_2,
  input  [15:0] io_lutdata_din_3,
  input  [15:0] io_lutdata_din_4,
  input  [15:0] io_lutdata_din_5,
  input  [15:0] io_lutdata_din_6,
  input  [15:0] io_lutdata_din_7,
  input  [15:0] io_lutdata_din_8,
  input  [15:0] io_lutdata_din_9,
  input  [15:0] io_lutdata_din_10,
  input  [15:0] io_lutdata_din_11,
  input  [15:0] io_lutdata_din_12,
  input  [15:0] io_lutdata_din_13,
  input  [15:0] io_lutdata_din_14,
  input  [15:0] io_lutdata_din_15,
  input  [15:0] io_lutdata_din_16,
  input  [15:0] io_lutdata_din_17,
  input  [15:0] io_lutdata_din_18,
  input  [15:0] io_lutdata_din_19,
  input  [15:0] io_lutdata_din_20,
  input  [15:0] io_lutdata_din_21,
  input  [15:0] io_lutdata_din_22,
  input  [15:0] io_lutdata_din_23,
  input  [15:0] io_lutdata_din_24,
  input  [15:0] io_lutdata_din_25,
  input  [15:0] io_lutdata_din_26,
  input  [15:0] io_lutdata_din_27,
  input  [15:0] io_lutdata_din_28,
  input  [15:0] io_lutdata_din_29,
  input  [15:0] io_lutdata_din_30,
  input  [15:0] io_lutdata_din_31
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_6;
  reg [31:0] _RAND_7;
  reg [31:0] _RAND_8;
  reg [31:0] _RAND_9;
  reg [31:0] _RAND_10;
  reg [31:0] _RAND_11;
  reg [31:0] _RAND_12;
  reg [31:0] _RAND_13;
  reg [31:0] _RAND_14;
`endif // RANDOMIZE_REG_INIT
  wire  custom_alu_clock; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_0_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_0_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_1_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_1_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_2_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_2_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_3_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_3_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_4_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_4_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_5_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_5_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_6_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_6_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_7_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_7_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_8_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_8_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_9_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_9_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_10_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_10_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_11_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_11_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_12_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_12_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_13_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_13_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_14_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_14_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_15_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_15_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_16_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_16_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_17_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_17_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_18_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_18_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_19_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_19_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_20_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_20_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_21_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_21_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_22_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_22_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_23_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_23_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_24_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_24_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_25_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_25_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_26_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_26_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_27_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_27_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_28_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_28_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_29_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_29_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_30_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_30_loadData; // @[Execute.scala 119:26]
  wire  custom_alu_io_config_31_writeEnable; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_config_31_loadData; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_rsx_0; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_rsx_1; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_rsx_2; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_rsx_3; // @[Execute.scala 119:26]
  wire [4:0] custom_alu_io_selector; // @[Execute.scala 119:26]
  wire [15:0] custom_alu_io_out; // @[Execute.scala 119:26]
  wire [15:0] standard_alu_io_in_x; // @[Execute.scala 135:28]
  wire [15:0] standard_alu_io_in_y; // @[Execute.scala 135:28]
  wire  standard_alu_io_in_carry; // @[Execute.scala 135:28]
  wire  standard_alu_io_in_select; // @[Execute.scala 135:28]
  wire [15:0] standard_alu_io_in_mask; // @[Execute.scala 135:28]
  wire [15:0] standard_alu_io_out; // @[Execute.scala 135:28]
  wire  standard_alu_io_carry_out; // @[Execute.scala 135:28]
  wire [3:0] standard_alu_io_funct; // @[Execute.scala 135:28]
  reg  pipe_out_reg_opcode_cust; // @[Execute.scala 111:25]
  reg  pipe_out_reg_opcode_arith; // @[Execute.scala 111:25]
  reg  pipe_out_reg_opcode_lload; // @[Execute.scala 111:25]
  reg  pipe_out_reg_opcode_lstore; // @[Execute.scala 111:25]
  reg  pipe_out_reg_opcode_send; // @[Execute.scala 111:25]
  reg  pipe_out_reg_opcode_set; // @[Execute.scala 111:25]
  reg  pipe_out_reg_opcode_expect; // @[Execute.scala 111:25]
  reg  pipe_out_reg_opcode_slice; // @[Execute.scala 111:25]
  reg  pipe_out_reg_opcode_mulh; // @[Execute.scala 111:25]
  reg [15:0] pipe_out_reg_data; // @[Execute.scala 111:25]
  reg [15:0] pipe_out_reg_result; // @[Execute.scala 111:25]
  reg [10:0] pipe_out_reg_rd; // @[Execute.scala 111:25]
  reg [15:0] pipe_out_reg_immediate; // @[Execute.scala 111:25]
  reg  pred_reg; // @[Execute.scala 112:25]
  reg [5:0] rs4_reg; // @[Execute.scala 113:25]
  wire [4:0] _GEN_1 = io_pipe_in_opcode_expect ? 5'ha : io_pipe_in_funct; // @[Execute.scala 148:36 149:29 151:29]
  wire [4:0] _GEN_2 = io_pipe_in_opcode_slice ? io_pipe_in_funct : 5'h0; // @[Execute.scala 154:35 157:29 166:29]
  wire [15:0] _GEN_3 = io_pipe_in_opcode_slice ? {{12'd0}, io_pipe_in_slice_ofst} : io_pipe_in_immediate; // @[Execute.scala 154:35 158:29 167:29]
  wire [4:0] _GEN_5 = io_pipe_in_opcode_arith | io_pipe_in_opcode_expect ? _GEN_1 : _GEN_2; // @[Execute.scala 146:60]
  wire [10:0] _GEN_8 = io_pipe_in_opcode_set_carry ? io_pipe_in_rd : {{5'd0}, rs4_reg}; // @[Execute.scala 197:37 198:17 202:17]
  wire [10:0] _GEN_9 = io_pipe_in_opcode_set_carry ? {{5'd0}, rs4_reg} : io_pipe_in_rs4; // @[Execute.scala 113:25 197:37 201:17]
  CustomAlu custom_alu ( // @[Execute.scala 119:26]
    .clock(custom_alu_clock),
    .io_config_0_writeEnable(custom_alu_io_config_0_writeEnable),
    .io_config_0_loadData(custom_alu_io_config_0_loadData),
    .io_config_1_writeEnable(custom_alu_io_config_1_writeEnable),
    .io_config_1_loadData(custom_alu_io_config_1_loadData),
    .io_config_2_writeEnable(custom_alu_io_config_2_writeEnable),
    .io_config_2_loadData(custom_alu_io_config_2_loadData),
    .io_config_3_writeEnable(custom_alu_io_config_3_writeEnable),
    .io_config_3_loadData(custom_alu_io_config_3_loadData),
    .io_config_4_writeEnable(custom_alu_io_config_4_writeEnable),
    .io_config_4_loadData(custom_alu_io_config_4_loadData),
    .io_config_5_writeEnable(custom_alu_io_config_5_writeEnable),
    .io_config_5_loadData(custom_alu_io_config_5_loadData),
    .io_config_6_writeEnable(custom_alu_io_config_6_writeEnable),
    .io_config_6_loadData(custom_alu_io_config_6_loadData),
    .io_config_7_writeEnable(custom_alu_io_config_7_writeEnable),
    .io_config_7_loadData(custom_alu_io_config_7_loadData),
    .io_config_8_writeEnable(custom_alu_io_config_8_writeEnable),
    .io_config_8_loadData(custom_alu_io_config_8_loadData),
    .io_config_9_writeEnable(custom_alu_io_config_9_writeEnable),
    .io_config_9_loadData(custom_alu_io_config_9_loadData),
    .io_config_10_writeEnable(custom_alu_io_config_10_writeEnable),
    .io_config_10_loadData(custom_alu_io_config_10_loadData),
    .io_config_11_writeEnable(custom_alu_io_config_11_writeEnable),
    .io_config_11_loadData(custom_alu_io_config_11_loadData),
    .io_config_12_writeEnable(custom_alu_io_config_12_writeEnable),
    .io_config_12_loadData(custom_alu_io_config_12_loadData),
    .io_config_13_writeEnable(custom_alu_io_config_13_writeEnable),
    .io_config_13_loadData(custom_alu_io_config_13_loadData),
    .io_config_14_writeEnable(custom_alu_io_config_14_writeEnable),
    .io_config_14_loadData(custom_alu_io_config_14_loadData),
    .io_config_15_writeEnable(custom_alu_io_config_15_writeEnable),
    .io_config_15_loadData(custom_alu_io_config_15_loadData),
    .io_config_16_writeEnable(custom_alu_io_config_16_writeEnable),
    .io_config_16_loadData(custom_alu_io_config_16_loadData),
    .io_config_17_writeEnable(custom_alu_io_config_17_writeEnable),
    .io_config_17_loadData(custom_alu_io_config_17_loadData),
    .io_config_18_writeEnable(custom_alu_io_config_18_writeEnable),
    .io_config_18_loadData(custom_alu_io_config_18_loadData),
    .io_config_19_writeEnable(custom_alu_io_config_19_writeEnable),
    .io_config_19_loadData(custom_alu_io_config_19_loadData),
    .io_config_20_writeEnable(custom_alu_io_config_20_writeEnable),
    .io_config_20_loadData(custom_alu_io_config_20_loadData),
    .io_config_21_writeEnable(custom_alu_io_config_21_writeEnable),
    .io_config_21_loadData(custom_alu_io_config_21_loadData),
    .io_config_22_writeEnable(custom_alu_io_config_22_writeEnable),
    .io_config_22_loadData(custom_alu_io_config_22_loadData),
    .io_config_23_writeEnable(custom_alu_io_config_23_writeEnable),
    .io_config_23_loadData(custom_alu_io_config_23_loadData),
    .io_config_24_writeEnable(custom_alu_io_config_24_writeEnable),
    .io_config_24_loadData(custom_alu_io_config_24_loadData),
    .io_config_25_writeEnable(custom_alu_io_config_25_writeEnable),
    .io_config_25_loadData(custom_alu_io_config_25_loadData),
    .io_config_26_writeEnable(custom_alu_io_config_26_writeEnable),
    .io_config_26_loadData(custom_alu_io_config_26_loadData),
    .io_config_27_writeEnable(custom_alu_io_config_27_writeEnable),
    .io_config_27_loadData(custom_alu_io_config_27_loadData),
    .io_config_28_writeEnable(custom_alu_io_config_28_writeEnable),
    .io_config_28_loadData(custom_alu_io_config_28_loadData),
    .io_config_29_writeEnable(custom_alu_io_config_29_writeEnable),
    .io_config_29_loadData(custom_alu_io_config_29_loadData),
    .io_config_30_writeEnable(custom_alu_io_config_30_writeEnable),
    .io_config_30_loadData(custom_alu_io_config_30_loadData),
    .io_config_31_writeEnable(custom_alu_io_config_31_writeEnable),
    .io_config_31_loadData(custom_alu_io_config_31_loadData),
    .io_rsx_0(custom_alu_io_rsx_0),
    .io_rsx_1(custom_alu_io_rsx_1),
    .io_rsx_2(custom_alu_io_rsx_2),
    .io_rsx_3(custom_alu_io_rsx_3),
    .io_selector(custom_alu_io_selector),
    .io_out(custom_alu_io_out)
  );
  StandardALUComb standard_alu ( // @[Execute.scala 135:28]
    .io_in_x(standard_alu_io_in_x),
    .io_in_y(standard_alu_io_in_y),
    .io_in_carry(standard_alu_io_in_carry),
    .io_in_select(standard_alu_io_in_select),
    .io_in_mask(standard_alu_io_in_mask),
    .io_out(standard_alu_io_out),
    .io_carry_out(standard_alu_io_carry_out),
    .io_funct(standard_alu_io_funct)
  );
  assign io_pipe_out_opcode_cust = pipe_out_reg_opcode_cust; // @[Execute.scala 191:25]
  assign io_pipe_out_opcode_arith = pipe_out_reg_opcode_arith; // @[Execute.scala 191:25]
  assign io_pipe_out_opcode_lload = pipe_out_reg_opcode_lload; // @[Execute.scala 191:25]
  assign io_pipe_out_opcode_lstore = pipe_out_reg_opcode_lstore; // @[Execute.scala 191:25]
  assign io_pipe_out_opcode_send = pipe_out_reg_opcode_send; // @[Execute.scala 191:25]
  assign io_pipe_out_opcode_set = pipe_out_reg_opcode_set; // @[Execute.scala 191:25]
  assign io_pipe_out_opcode_expect = pipe_out_reg_opcode_expect; // @[Execute.scala 191:25]
  assign io_pipe_out_opcode_slice = pipe_out_reg_opcode_slice; // @[Execute.scala 191:25]
  assign io_pipe_out_opcode_mulh = pipe_out_reg_opcode_mulh; // @[Execute.scala 191:25]
  assign io_pipe_out_data = pipe_out_reg_data; // @[Execute.scala 192:25]
  assign io_pipe_out_result = pipe_out_reg_result; // @[Execute.scala 193:25]
  assign io_pipe_out_rd = pipe_out_reg_rd; // @[Execute.scala 194:25]
  assign io_pipe_out_immediate = pipe_out_reg_immediate; // @[Execute.scala 195:25]
  assign io_pipe_out_pred = pred_reg; // @[Execute.scala 217:20]
  assign io_carry_rd = _GEN_8[5:0];
  assign io_carry_wen = io_pipe_in_opcode_arith & io_pipe_in_funct == 5'he | io_pipe_in_opcode_set_carry; // @[Execute.scala 210:77]
  assign io_carry_din = io_pipe_in_opcode_set_carry ? io_pipe_in_immediate[0] : standard_alu_io_carry_out; // @[Execute.scala 204:37 205:18 207:18]
  assign custom_alu_clock = clock;
  assign custom_alu_io_config_0_writeEnable = io_pipe_in_opcode_configure_luts_0; // @[Execute.scala 127:41]
  assign custom_alu_io_config_0_loadData = io_lutdata_din_0; // @[Execute.scala 128:41]
  assign custom_alu_io_config_1_writeEnable = io_pipe_in_opcode_configure_luts_1; // @[Execute.scala 127:41]
  assign custom_alu_io_config_1_loadData = io_lutdata_din_1; // @[Execute.scala 128:41]
  assign custom_alu_io_config_2_writeEnable = io_pipe_in_opcode_configure_luts_2; // @[Execute.scala 127:41]
  assign custom_alu_io_config_2_loadData = io_lutdata_din_2; // @[Execute.scala 128:41]
  assign custom_alu_io_config_3_writeEnable = io_pipe_in_opcode_configure_luts_3; // @[Execute.scala 127:41]
  assign custom_alu_io_config_3_loadData = io_lutdata_din_3; // @[Execute.scala 128:41]
  assign custom_alu_io_config_4_writeEnable = io_pipe_in_opcode_configure_luts_4; // @[Execute.scala 127:41]
  assign custom_alu_io_config_4_loadData = io_lutdata_din_4; // @[Execute.scala 128:41]
  assign custom_alu_io_config_5_writeEnable = io_pipe_in_opcode_configure_luts_5; // @[Execute.scala 127:41]
  assign custom_alu_io_config_5_loadData = io_lutdata_din_5; // @[Execute.scala 128:41]
  assign custom_alu_io_config_6_writeEnable = io_pipe_in_opcode_configure_luts_6; // @[Execute.scala 127:41]
  assign custom_alu_io_config_6_loadData = io_lutdata_din_6; // @[Execute.scala 128:41]
  assign custom_alu_io_config_7_writeEnable = io_pipe_in_opcode_configure_luts_7; // @[Execute.scala 127:41]
  assign custom_alu_io_config_7_loadData = io_lutdata_din_7; // @[Execute.scala 128:41]
  assign custom_alu_io_config_8_writeEnable = io_pipe_in_opcode_configure_luts_8; // @[Execute.scala 127:41]
  assign custom_alu_io_config_8_loadData = io_lutdata_din_8; // @[Execute.scala 128:41]
  assign custom_alu_io_config_9_writeEnable = io_pipe_in_opcode_configure_luts_9; // @[Execute.scala 127:41]
  assign custom_alu_io_config_9_loadData = io_lutdata_din_9; // @[Execute.scala 128:41]
  assign custom_alu_io_config_10_writeEnable = io_pipe_in_opcode_configure_luts_10; // @[Execute.scala 127:41]
  assign custom_alu_io_config_10_loadData = io_lutdata_din_10; // @[Execute.scala 128:41]
  assign custom_alu_io_config_11_writeEnable = io_pipe_in_opcode_configure_luts_11; // @[Execute.scala 127:41]
  assign custom_alu_io_config_11_loadData = io_lutdata_din_11; // @[Execute.scala 128:41]
  assign custom_alu_io_config_12_writeEnable = io_pipe_in_opcode_configure_luts_12; // @[Execute.scala 127:41]
  assign custom_alu_io_config_12_loadData = io_lutdata_din_12; // @[Execute.scala 128:41]
  assign custom_alu_io_config_13_writeEnable = io_pipe_in_opcode_configure_luts_13; // @[Execute.scala 127:41]
  assign custom_alu_io_config_13_loadData = io_lutdata_din_13; // @[Execute.scala 128:41]
  assign custom_alu_io_config_14_writeEnable = io_pipe_in_opcode_configure_luts_14; // @[Execute.scala 127:41]
  assign custom_alu_io_config_14_loadData = io_lutdata_din_14; // @[Execute.scala 128:41]
  assign custom_alu_io_config_15_writeEnable = io_pipe_in_opcode_configure_luts_15; // @[Execute.scala 127:41]
  assign custom_alu_io_config_15_loadData = io_lutdata_din_15; // @[Execute.scala 128:41]
  assign custom_alu_io_config_16_writeEnable = io_pipe_in_opcode_configure_luts_16; // @[Execute.scala 127:41]
  assign custom_alu_io_config_16_loadData = io_lutdata_din_16; // @[Execute.scala 128:41]
  assign custom_alu_io_config_17_writeEnable = io_pipe_in_opcode_configure_luts_17; // @[Execute.scala 127:41]
  assign custom_alu_io_config_17_loadData = io_lutdata_din_17; // @[Execute.scala 128:41]
  assign custom_alu_io_config_18_writeEnable = io_pipe_in_opcode_configure_luts_18; // @[Execute.scala 127:41]
  assign custom_alu_io_config_18_loadData = io_lutdata_din_18; // @[Execute.scala 128:41]
  assign custom_alu_io_config_19_writeEnable = io_pipe_in_opcode_configure_luts_19; // @[Execute.scala 127:41]
  assign custom_alu_io_config_19_loadData = io_lutdata_din_19; // @[Execute.scala 128:41]
  assign custom_alu_io_config_20_writeEnable = io_pipe_in_opcode_configure_luts_20; // @[Execute.scala 127:41]
  assign custom_alu_io_config_20_loadData = io_lutdata_din_20; // @[Execute.scala 128:41]
  assign custom_alu_io_config_21_writeEnable = io_pipe_in_opcode_configure_luts_21; // @[Execute.scala 127:41]
  assign custom_alu_io_config_21_loadData = io_lutdata_din_21; // @[Execute.scala 128:41]
  assign custom_alu_io_config_22_writeEnable = io_pipe_in_opcode_configure_luts_22; // @[Execute.scala 127:41]
  assign custom_alu_io_config_22_loadData = io_lutdata_din_22; // @[Execute.scala 128:41]
  assign custom_alu_io_config_23_writeEnable = io_pipe_in_opcode_configure_luts_23; // @[Execute.scala 127:41]
  assign custom_alu_io_config_23_loadData = io_lutdata_din_23; // @[Execute.scala 128:41]
  assign custom_alu_io_config_24_writeEnable = io_pipe_in_opcode_configure_luts_24; // @[Execute.scala 127:41]
  assign custom_alu_io_config_24_loadData = io_lutdata_din_24; // @[Execute.scala 128:41]
  assign custom_alu_io_config_25_writeEnable = io_pipe_in_opcode_configure_luts_25; // @[Execute.scala 127:41]
  assign custom_alu_io_config_25_loadData = io_lutdata_din_25; // @[Execute.scala 128:41]
  assign custom_alu_io_config_26_writeEnable = io_pipe_in_opcode_configure_luts_26; // @[Execute.scala 127:41]
  assign custom_alu_io_config_26_loadData = io_lutdata_din_26; // @[Execute.scala 128:41]
  assign custom_alu_io_config_27_writeEnable = io_pipe_in_opcode_configure_luts_27; // @[Execute.scala 127:41]
  assign custom_alu_io_config_27_loadData = io_lutdata_din_27; // @[Execute.scala 128:41]
  assign custom_alu_io_config_28_writeEnable = io_pipe_in_opcode_configure_luts_28; // @[Execute.scala 127:41]
  assign custom_alu_io_config_28_loadData = io_lutdata_din_28; // @[Execute.scala 128:41]
  assign custom_alu_io_config_29_writeEnable = io_pipe_in_opcode_configure_luts_29; // @[Execute.scala 127:41]
  assign custom_alu_io_config_29_loadData = io_lutdata_din_29; // @[Execute.scala 128:41]
  assign custom_alu_io_config_30_writeEnable = io_pipe_in_opcode_configure_luts_30; // @[Execute.scala 127:41]
  assign custom_alu_io_config_30_loadData = io_lutdata_din_30; // @[Execute.scala 128:41]
  assign custom_alu_io_config_31_writeEnable = io_pipe_in_opcode_configure_luts_31; // @[Execute.scala 127:41]
  assign custom_alu_io_config_31_loadData = io_lutdata_din_31; // @[Execute.scala 128:41]
  assign custom_alu_io_rsx_0 = io_regs_in_rs1; // @[Execute.scala 122:{27,27}]
  assign custom_alu_io_rsx_1 = io_regs_in_rs2; // @[Execute.scala 122:{27,27}]
  assign custom_alu_io_rsx_2 = io_regs_in_rs3; // @[Execute.scala 122:{27,27}]
  assign custom_alu_io_rsx_3 = io_regs_in_rs4; // @[Execute.scala 122:{27,27}]
  assign custom_alu_io_selector = io_pipe_in_funct; // @[Execute.scala 130:26]
  assign standard_alu_io_in_x = io_pipe_in_opcode_set | io_pipe_in_opcode_send ? 16'h0 : io_regs_in_rs1; // @[Execute.scala 174:57 175:26 177:26]
  assign standard_alu_io_in_y = io_pipe_in_opcode_arith | io_pipe_in_opcode_expect ? io_regs_in_rs2 : _GEN_3; // @[Execute.scala 146:60 147:26]
  assign standard_alu_io_in_carry = io_carry_in; // @[Execute.scala 172:29]
  assign standard_alu_io_in_select = io_regs_in_rs3[0]; // @[Execute.scala 171:29]
  assign standard_alu_io_in_mask = io_pipe_in_opcode_slice ? io_pipe_in_immediate : 16'hffff; // @[Execute.scala 137:33 140:29 143:29]
  assign standard_alu_io_funct = _GEN_5[3:0];
  always @(posedge clock) begin
    pipe_out_reg_opcode_cust <= io_pipe_in_opcode_cust; // @[Execute.scala 186:26]
    pipe_out_reg_opcode_arith <= io_pipe_in_opcode_arith; // @[Execute.scala 186:26]
    pipe_out_reg_opcode_lload <= io_pipe_in_opcode_lload; // @[Execute.scala 186:26]
    pipe_out_reg_opcode_lstore <= io_pipe_in_opcode_lstore; // @[Execute.scala 186:26]
    pipe_out_reg_opcode_send <= io_pipe_in_opcode_send; // @[Execute.scala 186:26]
    pipe_out_reg_opcode_set <= io_pipe_in_opcode_set; // @[Execute.scala 186:26]
    pipe_out_reg_opcode_expect <= io_pipe_in_opcode_expect; // @[Execute.scala 186:26]
    pipe_out_reg_opcode_slice <= io_pipe_in_opcode_slice; // @[Execute.scala 186:26]
    pipe_out_reg_opcode_mulh <= io_pipe_in_opcode_mulh; // @[Execute.scala 186:26]
    pipe_out_reg_data <= io_regs_in_rs2; // @[Execute.scala 187:26]
    if (io_pipe_in_opcode_cust) begin // @[Execute.scala 180:32]
      pipe_out_reg_result <= custom_alu_io_out; // @[Execute.scala 181:25]
    end else begin
      pipe_out_reg_result <= standard_alu_io_out; // @[Execute.scala 183:25]
    end
    pipe_out_reg_rd <= io_pipe_in_rd; // @[Execute.scala 188:26]
    pipe_out_reg_immediate <= io_pipe_in_immediate; // @[Execute.scala 189:26]
    if (io_pipe_in_opcode_predicate) begin // @[Execute.scala 213:37]
      pred_reg <= io_regs_in_rs1 == 16'h1; // @[Execute.scala 214:14]
    end
    rs4_reg <= _GEN_9[5:0];
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{`RANDOM}};
  pipe_out_reg_opcode_cust = _RAND_0[0:0];
  _RAND_1 = {1{`RANDOM}};
  pipe_out_reg_opcode_arith = _RAND_1[0:0];
  _RAND_2 = {1{`RANDOM}};
  pipe_out_reg_opcode_lload = _RAND_2[0:0];
  _RAND_3 = {1{`RANDOM}};
  pipe_out_reg_opcode_lstore = _RAND_3[0:0];
  _RAND_4 = {1{`RANDOM}};
  pipe_out_reg_opcode_send = _RAND_4[0:0];
  _RAND_5 = {1{`RANDOM}};
  pipe_out_reg_opcode_set = _RAND_5[0:0];
  _RAND_6 = {1{`RANDOM}};
  pipe_out_reg_opcode_expect = _RAND_6[0:0];
  _RAND_7 = {1{`RANDOM}};
  pipe_out_reg_opcode_slice = _RAND_7[0:0];
  _RAND_8 = {1{`RANDOM}};
  pipe_out_reg_opcode_mulh = _RAND_8[0:0];
  _RAND_9 = {1{`RANDOM}};
  pipe_out_reg_data = _RAND_9[15:0];
  _RAND_10 = {1{`RANDOM}};
  pipe_out_reg_result = _RAND_10[15:0];
  _RAND_11 = {1{`RANDOM}};
  pipe_out_reg_rd = _RAND_11[10:0];
  _RAND_12 = {1{`RANDOM}};
  pipe_out_reg_immediate = _RAND_12[15:0];
  _RAND_13 = {1{`RANDOM}};
  pred_reg = _RAND_13[0:0];
  _RAND_14 = {1{`RANDOM}};
  rs4_reg = _RAND_14[5:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module MemoryAccess(
  input         clock,
  input         reset,
  input         io_pipe_in_opcode_cust,
  input         io_pipe_in_opcode_arith,
  input         io_pipe_in_opcode_lload,
  input         io_pipe_in_opcode_lstore,
  input         io_pipe_in_opcode_send,
  input         io_pipe_in_opcode_set,
  input         io_pipe_in_opcode_slice,
  input         io_pipe_in_opcode_mulh,
  input  [15:0] io_pipe_in_data,
  input  [15:0] io_pipe_in_result,
  input  [10:0] io_pipe_in_rd,
  input  [15:0] io_pipe_in_immediate,
  input         io_pipe_in_pred,
  output [15:0] io_pipe_out_result,
  output [15:0] io_pipe_out_packet_data,
  output [10:0] io_pipe_out_packet_address,
  output        io_pipe_out_packet_valid,
  output        io_pipe_out_packet_xHops,
  output        io_pipe_out_packet_yHops,
  output        io_pipe_out_write_back,
  output [10:0] io_pipe_out_rd,
  output        io_pipe_out_mulh,
  output [11:0] io_local_memory_interface_raddr,
  input  [15:0] io_local_memory_interface_dout,
  output        io_local_memory_interface_wen,
  output [11:0] io_local_memory_interface_waddr,
  output [15:0] io_local_memory_interface_din
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_6;
  reg [31:0] _RAND_7;
  reg [31:0] _RAND_8;
`endif // RANDOMIZE_REG_INIT
  reg [15:0] packet_reg_data; // @[MemoryAccess.scala 58:27]
  reg [10:0] packet_reg_address; // @[MemoryAccess.scala 58:27]
  reg  packet_reg_valid; // @[MemoryAccess.scala 58:27]
  reg  packet_reg_xHops; // @[MemoryAccess.scala 58:27]
  reg  packet_reg_yHops; // @[MemoryAccess.scala 58:27]
  wire  _T = io_pipe_in_opcode_lload | io_pipe_in_opcode_cust; // @[MemoryAccess.scala 88:29]
  wire  _T_1 = _T | io_pipe_in_opcode_arith; // @[MemoryAccess.scala 89:28]
  wire  _T_3 = _T_1 | io_pipe_in_opcode_set; // @[MemoryAccess.scala 92:7]
  reg  pipereg; // @[MemoryAccess.scala 82:22]
  reg [10:0] pipereg_1; // @[MemoryAccess.scala 82:22]
  reg  pipereg_5; // @[MemoryAccess.scala 82:22]
  reg [15:0] pipereg_6; // @[MemoryAccess.scala 82:22]
  assign io_pipe_out_result = io_pipe_in_opcode_lload ? io_local_memory_interface_dout : pipereg_6; // @[MemoryAccess.scala 121:35 122:26 84:13]
  assign io_pipe_out_packet_data = packet_reg_data; // @[MemoryAccess.scala 79:22]
  assign io_pipe_out_packet_address = packet_reg_address; // @[MemoryAccess.scala 79:22]
  assign io_pipe_out_packet_valid = packet_reg_valid; // @[MemoryAccess.scala 79:22]
  assign io_pipe_out_packet_xHops = packet_reg_xHops; // @[MemoryAccess.scala 79:22]
  assign io_pipe_out_packet_yHops = packet_reg_yHops; // @[MemoryAccess.scala 79:22]
  assign io_pipe_out_write_back = pipereg; // @[MemoryAccess.scala 84:13]
  assign io_pipe_out_rd = pipereg_1; // @[MemoryAccess.scala 84:13]
  assign io_pipe_out_mulh = pipereg_5; // @[MemoryAccess.scala 84:13]
  assign io_local_memory_interface_raddr = io_pipe_in_result[11:0]; // @[MemoryAccess.scala 45:35]
  assign io_local_memory_interface_wen = io_pipe_in_opcode_lstore & io_pipe_in_pred; // @[MemoryAccess.scala 48:63]
  assign io_local_memory_interface_waddr = io_pipe_in_result[11:0]; // @[MemoryAccess.scala 46:35]
  assign io_local_memory_interface_din = io_pipe_in_data; // @[MemoryAccess.scala 47:35]
  always @(posedge clock) begin
    packet_reg_data <= io_pipe_in_data; // @[MemoryAccess.scala 75:22]
    packet_reg_address <= io_pipe_in_rd; // @[MemoryAccess.scala 76:22]
    if (reset) begin // @[MemoryAccess.scala 58:27]
      packet_reg_valid <= 1'h0; // @[MemoryAccess.scala 58:27]
    end else begin
      packet_reg_valid <= io_pipe_in_opcode_send; // @[MemoryAccess.scala 77:22]
    end
    packet_reg_xHops <= io_pipe_in_immediate[0]; // @[MemoryAccess.scala 72:20]
    packet_reg_yHops <= io_pipe_in_immediate[8]; // @[MemoryAccess.scala 73:20]
    pipereg <= _T_3 | io_pipe_in_opcode_slice; // @[MemoryAccess.scala 93:27]
    pipereg_1 <= io_pipe_in_rd; // @[MemoryAccess.scala 83:13]
    pipereg_5 <= io_pipe_in_opcode_mulh; // @[MemoryAccess.scala 83:13]
    pipereg_6 <= io_pipe_in_result; // @[MemoryAccess.scala 83:13]
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{`RANDOM}};
  packet_reg_data = _RAND_0[15:0];
  _RAND_1 = {1{`RANDOM}};
  packet_reg_address = _RAND_1[10:0];
  _RAND_2 = {1{`RANDOM}};
  packet_reg_valid = _RAND_2[0:0];
  _RAND_3 = {1{`RANDOM}};
  packet_reg_xHops = _RAND_3[0:0];
  _RAND_4 = {1{`RANDOM}};
  packet_reg_yHops = _RAND_4[0:0];
  _RAND_5 = {1{`RANDOM}};
  pipereg = _RAND_5[0:0];
  _RAND_6 = {1{`RANDOM}};
  pipereg_1 = _RAND_6[10:0];
  _RAND_7 = {1{`RANDOM}};
  pipereg_5 = _RAND_7[0:0];
  _RAND_8 = {1{`RANDOM}};
  pipereg_6 = _RAND_8[15:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module SimpleDualPortMemory_1(
  input         clock,
  input  [10:0] io_raddr,
  output [15:0] io_dout,
  input         io_wen,
  input  [10:0] io_waddr,
  input  [15:0] io_din
);
  wire  impl_wen; // @[GenericMemory.scala 197:20]
  wire  impl_clock; // @[GenericMemory.scala 197:20]
  wire [10:0] impl_raddr; // @[GenericMemory.scala 197:20]
  wire [10:0] impl_waddr; // @[GenericMemory.scala 197:20]
  wire [15:0] impl_din; // @[GenericMemory.scala 197:20]
  wire [15:0] impl_dout; // @[GenericMemory.scala 197:20]
  BRAMLike #(.ADDRESS_WIDTH(11), .DATA_WIDTH(16), .filename("")) impl ( // @[GenericMemory.scala 197:20]
    .wen(impl_wen),
    .clock(impl_clock),
    .raddr(impl_raddr),
    .waddr(impl_waddr),
    .din(impl_din),
    .dout(impl_dout)
  );
  assign io_dout = impl_dout; // @[GenericMemory.scala 207:17]
  assign impl_wen = io_wen; // @[GenericMemory.scala 205:17]
  assign impl_clock = clock; // @[GenericMemory.scala 202:17]
  assign impl_raddr = io_raddr; // @[GenericMemory.scala 204:17]
  assign impl_waddr = io_waddr; // @[GenericMemory.scala 206:17]
  assign impl_din = io_din; // @[GenericMemory.scala 203:17]
endmodule
module RegisterFile(
  input         clock,
  input  [10:0] io_rs1_addr,
  output [15:0] io_rs1_dout,
  input  [10:0] io_rs2_addr,
  output [15:0] io_rs2_dout,
  input  [10:0] io_rs3_addr,
  output [15:0] io_rs3_dout,
  input  [10:0] io_rs4_addr,
  output [15:0] io_rs4_dout,
  input  [10:0] io_w_addr,
  input  [15:0] io_w_din,
  input         io_w_en
);
  wire  rs1bank_clock; // @[RegisterFile.scala 94:23]
  wire [10:0] rs1bank_io_raddr; // @[RegisterFile.scala 94:23]
  wire [15:0] rs1bank_io_dout; // @[RegisterFile.scala 94:23]
  wire  rs1bank_io_wen; // @[RegisterFile.scala 94:23]
  wire [10:0] rs1bank_io_waddr; // @[RegisterFile.scala 94:23]
  wire [15:0] rs1bank_io_din; // @[RegisterFile.scala 94:23]
  wire  rs2bank_clock; // @[RegisterFile.scala 95:23]
  wire [10:0] rs2bank_io_raddr; // @[RegisterFile.scala 95:23]
  wire [15:0] rs2bank_io_dout; // @[RegisterFile.scala 95:23]
  wire  rs2bank_io_wen; // @[RegisterFile.scala 95:23]
  wire [10:0] rs2bank_io_waddr; // @[RegisterFile.scala 95:23]
  wire [15:0] rs2bank_io_din; // @[RegisterFile.scala 95:23]
  wire  rs3bank_clock; // @[RegisterFile.scala 96:23]
  wire [10:0] rs3bank_io_raddr; // @[RegisterFile.scala 96:23]
  wire [15:0] rs3bank_io_dout; // @[RegisterFile.scala 96:23]
  wire  rs3bank_io_wen; // @[RegisterFile.scala 96:23]
  wire [10:0] rs3bank_io_waddr; // @[RegisterFile.scala 96:23]
  wire [15:0] rs3bank_io_din; // @[RegisterFile.scala 96:23]
  wire  rs4bank_clock; // @[RegisterFile.scala 97:23]
  wire [10:0] rs4bank_io_raddr; // @[RegisterFile.scala 97:23]
  wire [15:0] rs4bank_io_dout; // @[RegisterFile.scala 97:23]
  wire  rs4bank_io_wen; // @[RegisterFile.scala 97:23]
  wire [10:0] rs4bank_io_waddr; // @[RegisterFile.scala 97:23]
  wire [15:0] rs4bank_io_din; // @[RegisterFile.scala 97:23]
  SimpleDualPortMemory_1 rs1bank ( // @[RegisterFile.scala 94:23]
    .clock(rs1bank_clock),
    .io_raddr(rs1bank_io_raddr),
    .io_dout(rs1bank_io_dout),
    .io_wen(rs1bank_io_wen),
    .io_waddr(rs1bank_io_waddr),
    .io_din(rs1bank_io_din)
  );
  SimpleDualPortMemory_1 rs2bank ( // @[RegisterFile.scala 95:23]
    .clock(rs2bank_clock),
    .io_raddr(rs2bank_io_raddr),
    .io_dout(rs2bank_io_dout),
    .io_wen(rs2bank_io_wen),
    .io_waddr(rs2bank_io_waddr),
    .io_din(rs2bank_io_din)
  );
  SimpleDualPortMemory_1 rs3bank ( // @[RegisterFile.scala 96:23]
    .clock(rs3bank_clock),
    .io_raddr(rs3bank_io_raddr),
    .io_dout(rs3bank_io_dout),
    .io_wen(rs3bank_io_wen),
    .io_waddr(rs3bank_io_waddr),
    .io_din(rs3bank_io_din)
  );
  SimpleDualPortMemory_1 rs4bank ( // @[RegisterFile.scala 97:23]
    .clock(rs4bank_clock),
    .io_raddr(rs4bank_io_raddr),
    .io_dout(rs4bank_io_dout),
    .io_wen(rs4bank_io_wen),
    .io_waddr(rs4bank_io_waddr),
    .io_din(rs4bank_io_din)
  );
  assign io_rs1_dout = rs1bank_io_dout; // @[RegisterFile.scala 49:12]
  assign io_rs2_dout = rs2bank_io_dout; // @[RegisterFile.scala 49:12]
  assign io_rs3_dout = rs3bank_io_dout; // @[RegisterFile.scala 49:12]
  assign io_rs4_dout = rs4bank_io_dout; // @[RegisterFile.scala 49:12]
  assign rs1bank_clock = clock;
  assign rs1bank_io_raddr = io_rs1_addr; // @[RegisterFile.scala 48:20]
  assign rs1bank_io_wen = io_w_en; // @[RegisterFile.scala 66:18]
  assign rs1bank_io_waddr = io_w_addr; // @[RegisterFile.scala 64:20]
  assign rs1bank_io_din = io_w_din; // @[RegisterFile.scala 65:18]
  assign rs2bank_clock = clock;
  assign rs2bank_io_raddr = io_rs2_addr; // @[RegisterFile.scala 48:20]
  assign rs2bank_io_wen = io_w_en; // @[RegisterFile.scala 66:18]
  assign rs2bank_io_waddr = io_w_addr; // @[RegisterFile.scala 64:20]
  assign rs2bank_io_din = io_w_din; // @[RegisterFile.scala 65:18]
  assign rs3bank_clock = clock;
  assign rs3bank_io_raddr = io_rs3_addr; // @[RegisterFile.scala 48:20]
  assign rs3bank_io_wen = io_w_en; // @[RegisterFile.scala 66:18]
  assign rs3bank_io_waddr = io_w_addr; // @[RegisterFile.scala 64:20]
  assign rs3bank_io_din = io_w_din; // @[RegisterFile.scala 65:18]
  assign rs4bank_clock = clock;
  assign rs4bank_io_raddr = io_rs4_addr; // @[RegisterFile.scala 48:20]
  assign rs4bank_io_wen = io_w_en; // @[RegisterFile.scala 66:18]
  assign rs4bank_io_waddr = io_w_addr; // @[RegisterFile.scala 64:20]
  assign rs4bank_io_din = io_w_din; // @[RegisterFile.scala 65:18]
endmodule
module CarryRegisterFile(
  input        clock,
  input  [5:0] io_raddr,
  input  [5:0] io_waddr,
  input        io_din,
  output       io_dout,
  input        io_wen
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
`endif // RANDOMIZE_REG_INIT
  reg  storage [0:63]; // @[RegisterFile.scala 124:28]
  wire  storage_io_dout_MPORT_en; // @[RegisterFile.scala 124:28]
  wire [5:0] storage_io_dout_MPORT_addr; // @[RegisterFile.scala 124:28]
  wire  storage_io_dout_MPORT_data; // @[RegisterFile.scala 124:28]
  wire  storage_MPORT_data; // @[RegisterFile.scala 124:28]
  wire [5:0] storage_MPORT_addr; // @[RegisterFile.scala 124:28]
  wire  storage_MPORT_mask; // @[RegisterFile.scala 124:28]
  wire  storage_MPORT_en; // @[RegisterFile.scala 124:28]
  reg  storage_io_dout_MPORT_en_pipe_0;
  reg [5:0] storage_io_dout_MPORT_addr_pipe_0;
  assign storage_io_dout_MPORT_en = storage_io_dout_MPORT_en_pipe_0;
  assign storage_io_dout_MPORT_addr = storage_io_dout_MPORT_addr_pipe_0;
  assign storage_io_dout_MPORT_data = storage[storage_io_dout_MPORT_addr]; // @[RegisterFile.scala 124:28]
  assign storage_MPORT_data = io_din;
  assign storage_MPORT_addr = io_waddr;
  assign storage_MPORT_mask = 1'h1;
  assign storage_MPORT_en = io_wen;
  assign io_dout = storage_io_dout_MPORT_data; // @[RegisterFile.scala 128:11]
  always @(posedge clock) begin
    if (storage_MPORT_en & storage_MPORT_mask) begin
      storage[storage_MPORT_addr] <= storage_MPORT_data; // @[RegisterFile.scala 124:28]
    end
    storage_io_dout_MPORT_en_pipe_0 <= 1'h1;
    if (1'h1) begin
      storage_io_dout_MPORT_addr_pipe_0 <= io_raddr;
    end
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_MEM_INIT
  _RAND_0 = {1{`RANDOM}};
  for (initvar = 0; initvar < 64; initvar = initvar+1)
    storage[initvar] = _RAND_0[0:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_1 = {1{`RANDOM}};
  storage_io_dout_MPORT_en_pipe_0 = _RAND_1[0:0];
  _RAND_2 = {1{`RANDOM}};
  storage_io_dout_MPORT_addr_pipe_0 = _RAND_2[5:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module LutLoadDataRegisterFile(
  input         clock,
  input  [4:0]  io_waddr,
  input  [15:0] io_din,
  output [15:0] io_dout_0,
  output [15:0] io_dout_1,
  output [15:0] io_dout_2,
  output [15:0] io_dout_3,
  output [15:0] io_dout_4,
  output [15:0] io_dout_5,
  output [15:0] io_dout_6,
  output [15:0] io_dout_7,
  output [15:0] io_dout_8,
  output [15:0] io_dout_9,
  output [15:0] io_dout_10,
  output [15:0] io_dout_11,
  output [15:0] io_dout_12,
  output [15:0] io_dout_13,
  output [15:0] io_dout_14,
  output [15:0] io_dout_15,
  output [15:0] io_dout_16,
  output [15:0] io_dout_17,
  output [15:0] io_dout_18,
  output [15:0] io_dout_19,
  output [15:0] io_dout_20,
  output [15:0] io_dout_21,
  output [15:0] io_dout_22,
  output [15:0] io_dout_23,
  output [15:0] io_dout_24,
  output [15:0] io_dout_25,
  output [15:0] io_dout_26,
  output [15:0] io_dout_27,
  output [15:0] io_dout_28,
  output [15:0] io_dout_29,
  output [15:0] io_dout_30,
  output [15:0] io_dout_31,
  input         io_wen
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_6;
  reg [31:0] _RAND_7;
  reg [31:0] _RAND_8;
  reg [31:0] _RAND_9;
  reg [31:0] _RAND_10;
  reg [31:0] _RAND_11;
  reg [31:0] _RAND_12;
  reg [31:0] _RAND_13;
  reg [31:0] _RAND_14;
  reg [31:0] _RAND_15;
  reg [31:0] _RAND_16;
  reg [31:0] _RAND_17;
  reg [31:0] _RAND_18;
  reg [31:0] _RAND_19;
  reg [31:0] _RAND_20;
  reg [31:0] _RAND_21;
  reg [31:0] _RAND_22;
  reg [31:0] _RAND_23;
  reg [31:0] _RAND_24;
  reg [31:0] _RAND_25;
  reg [31:0] _RAND_26;
  reg [31:0] _RAND_27;
  reg [31:0] _RAND_28;
  reg [31:0] _RAND_29;
  reg [31:0] _RAND_30;
  reg [31:0] _RAND_31;
`endif // RANDOMIZE_REG_INIT
  reg [15:0] storage_0; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_1; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_2; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_3; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_4; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_5; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_6; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_7; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_8; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_9; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_10; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_11; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_12; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_13; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_14; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_15; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_16; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_17; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_18; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_19; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_20; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_21; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_22; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_23; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_24; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_25; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_26; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_27; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_28; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_29; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_30; // @[RegisterFile.scala 143:20]
  reg [15:0] storage_31; // @[RegisterFile.scala 143:20]
  assign io_dout_0 = storage_0; // @[RegisterFile.scala 147:11]
  assign io_dout_1 = storage_1; // @[RegisterFile.scala 147:11]
  assign io_dout_2 = storage_2; // @[RegisterFile.scala 147:11]
  assign io_dout_3 = storage_3; // @[RegisterFile.scala 147:11]
  assign io_dout_4 = storage_4; // @[RegisterFile.scala 147:11]
  assign io_dout_5 = storage_5; // @[RegisterFile.scala 147:11]
  assign io_dout_6 = storage_6; // @[RegisterFile.scala 147:11]
  assign io_dout_7 = storage_7; // @[RegisterFile.scala 147:11]
  assign io_dout_8 = storage_8; // @[RegisterFile.scala 147:11]
  assign io_dout_9 = storage_9; // @[RegisterFile.scala 147:11]
  assign io_dout_10 = storage_10; // @[RegisterFile.scala 147:11]
  assign io_dout_11 = storage_11; // @[RegisterFile.scala 147:11]
  assign io_dout_12 = storage_12; // @[RegisterFile.scala 147:11]
  assign io_dout_13 = storage_13; // @[RegisterFile.scala 147:11]
  assign io_dout_14 = storage_14; // @[RegisterFile.scala 147:11]
  assign io_dout_15 = storage_15; // @[RegisterFile.scala 147:11]
  assign io_dout_16 = storage_16; // @[RegisterFile.scala 147:11]
  assign io_dout_17 = storage_17; // @[RegisterFile.scala 147:11]
  assign io_dout_18 = storage_18; // @[RegisterFile.scala 147:11]
  assign io_dout_19 = storage_19; // @[RegisterFile.scala 147:11]
  assign io_dout_20 = storage_20; // @[RegisterFile.scala 147:11]
  assign io_dout_21 = storage_21; // @[RegisterFile.scala 147:11]
  assign io_dout_22 = storage_22; // @[RegisterFile.scala 147:11]
  assign io_dout_23 = storage_23; // @[RegisterFile.scala 147:11]
  assign io_dout_24 = storage_24; // @[RegisterFile.scala 147:11]
  assign io_dout_25 = storage_25; // @[RegisterFile.scala 147:11]
  assign io_dout_26 = storage_26; // @[RegisterFile.scala 147:11]
  assign io_dout_27 = storage_27; // @[RegisterFile.scala 147:11]
  assign io_dout_28 = storage_28; // @[RegisterFile.scala 147:11]
  assign io_dout_29 = storage_29; // @[RegisterFile.scala 147:11]
  assign io_dout_30 = storage_30; // @[RegisterFile.scala 147:11]
  assign io_dout_31 = storage_31; // @[RegisterFile.scala 147:11]
  always @(posedge clock) begin
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h0 == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_0 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h1 == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_1 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h2 == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_2 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h3 == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_3 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h4 == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_4 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h5 == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_5 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h6 == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_6 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h7 == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_7 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h8 == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_8 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h9 == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_9 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'ha == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_10 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'hb == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_11 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'hc == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_12 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'hd == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_13 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'he == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_14 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'hf == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_15 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h10 == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_16 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h11 == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_17 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h12 == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_18 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h13 == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_19 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h14 == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_20 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h15 == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_21 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h16 == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_22 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h17 == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_23 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h18 == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_24 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h19 == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_25 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h1a == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_26 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h1b == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_27 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h1c == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_28 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h1d == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_29 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h1e == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_30 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
    if (io_wen) begin // @[RegisterFile.scala 144:16]
      if (5'h1f == io_waddr) begin // @[RegisterFile.scala 145:23]
        storage_31 <= io_din; // @[RegisterFile.scala 145:23]
      end
    end
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{`RANDOM}};
  storage_0 = _RAND_0[15:0];
  _RAND_1 = {1{`RANDOM}};
  storage_1 = _RAND_1[15:0];
  _RAND_2 = {1{`RANDOM}};
  storage_2 = _RAND_2[15:0];
  _RAND_3 = {1{`RANDOM}};
  storage_3 = _RAND_3[15:0];
  _RAND_4 = {1{`RANDOM}};
  storage_4 = _RAND_4[15:0];
  _RAND_5 = {1{`RANDOM}};
  storage_5 = _RAND_5[15:0];
  _RAND_6 = {1{`RANDOM}};
  storage_6 = _RAND_6[15:0];
  _RAND_7 = {1{`RANDOM}};
  storage_7 = _RAND_7[15:0];
  _RAND_8 = {1{`RANDOM}};
  storage_8 = _RAND_8[15:0];
  _RAND_9 = {1{`RANDOM}};
  storage_9 = _RAND_9[15:0];
  _RAND_10 = {1{`RANDOM}};
  storage_10 = _RAND_10[15:0];
  _RAND_11 = {1{`RANDOM}};
  storage_11 = _RAND_11[15:0];
  _RAND_12 = {1{`RANDOM}};
  storage_12 = _RAND_12[15:0];
  _RAND_13 = {1{`RANDOM}};
  storage_13 = _RAND_13[15:0];
  _RAND_14 = {1{`RANDOM}};
  storage_14 = _RAND_14[15:0];
  _RAND_15 = {1{`RANDOM}};
  storage_15 = _RAND_15[15:0];
  _RAND_16 = {1{`RANDOM}};
  storage_16 = _RAND_16[15:0];
  _RAND_17 = {1{`RANDOM}};
  storage_17 = _RAND_17[15:0];
  _RAND_18 = {1{`RANDOM}};
  storage_18 = _RAND_18[15:0];
  _RAND_19 = {1{`RANDOM}};
  storage_19 = _RAND_19[15:0];
  _RAND_20 = {1{`RANDOM}};
  storage_20 = _RAND_20[15:0];
  _RAND_21 = {1{`RANDOM}};
  storage_21 = _RAND_21[15:0];
  _RAND_22 = {1{`RANDOM}};
  storage_22 = _RAND_22[15:0];
  _RAND_23 = {1{`RANDOM}};
  storage_23 = _RAND_23[15:0];
  _RAND_24 = {1{`RANDOM}};
  storage_24 = _RAND_24[15:0];
  _RAND_25 = {1{`RANDOM}};
  storage_25 = _RAND_25[15:0];
  _RAND_26 = {1{`RANDOM}};
  storage_26 = _RAND_26[15:0];
  _RAND_27 = {1{`RANDOM}};
  storage_27 = _RAND_27[15:0];
  _RAND_28 = {1{`RANDOM}};
  storage_28 = _RAND_28[15:0];
  _RAND_29 = {1{`RANDOM}};
  storage_29 = _RAND_29[15:0];
  _RAND_30 = {1{`RANDOM}};
  storage_30 = _RAND_30[15:0];
  _RAND_31 = {1{`RANDOM}};
  storage_31 = _RAND_31[15:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module SimpleDualPortMemory_5(
  input         clock,
  input  [9:0]  io_raddr,
  output [15:0] io_dout,
  input         io_wen,
  input  [9:0]  io_waddr,
  input  [15:0] io_din
);
  wire  impl_wen; // @[GenericMemory.scala 197:20]
  wire  impl_clock; // @[GenericMemory.scala 197:20]
  wire [9:0] impl_raddr; // @[GenericMemory.scala 197:20]
  wire [9:0] impl_waddr; // @[GenericMemory.scala 197:20]
  wire [15:0] impl_din; // @[GenericMemory.scala 197:20]
  wire [15:0] impl_dout; // @[GenericMemory.scala 197:20]
  BRAMLike #(.ADDRESS_WIDTH(10), .DATA_WIDTH(16), .filename("")) impl ( // @[GenericMemory.scala 197:20]
    .wen(impl_wen),
    .clock(impl_clock),
    .raddr(impl_raddr),
    .waddr(impl_waddr),
    .din(impl_din),
    .dout(impl_dout)
  );
  assign io_dout = impl_dout; // @[GenericMemory.scala 207:17]
  assign impl_wen = io_wen; // @[GenericMemory.scala 205:17]
  assign impl_clock = clock; // @[GenericMemory.scala 202:17]
  assign impl_raddr = io_raddr; // @[GenericMemory.scala 204:17]
  assign impl_waddr = io_waddr; // @[GenericMemory.scala 206:17]
  assign impl_din = io_din; // @[GenericMemory.scala 203:17]
endmodule
module Multiplier(
  input         clock,
  input  [15:0] io_in0,
  input  [15:0] io_in1,
  output [31:0] io_out,
  input         io_valid_in,
  output        io_valid_out
);
  wire  dsp_clock; // @[Multiplier.scala 34:19]
  wire [15:0] dsp_in0; // @[Multiplier.scala 34:19]
  wire [15:0] dsp_in1; // @[Multiplier.scala 34:19]
  wire [31:0] dsp_out; // @[Multiplier.scala 34:19]
  wire  dsp_valid_in; // @[Multiplier.scala 34:19]
  wire  dsp_valid_out; // @[Multiplier.scala 34:19]
  MultiplierDsp48 dsp ( // @[Multiplier.scala 34:19]
    .clock(dsp_clock),
    .in0(dsp_in0),
    .in1(dsp_in1),
    .out(dsp_out),
    .valid_in(dsp_valid_in),
    .valid_out(dsp_valid_out)
  );
  assign io_out = dsp_out; // @[Multiplier.scala 41:16]
  assign io_valid_out = dsp_valid_out; // @[Multiplier.scala 42:16]
  assign dsp_clock = clock; // @[Multiplier.scala 36:19]
  assign dsp_in0 = io_in0; // @[Multiplier.scala 37:19]
  assign dsp_in1 = io_in1; // @[Multiplier.scala 38:19]
  assign dsp_valid_in = io_valid_in; // @[Multiplier.scala 39:19]
endmodule
module Processor(
  input         clock,
  input         reset,
  input  [15:0] io_packet_in_data,
  input  [10:0] io_packet_in_address,
  input         io_packet_in_valid,
  output [15:0] io_packet_out_data,
  output [10:0] io_packet_out_address,
  output        io_packet_out_valid,
  output        io_packet_out_xHops,
  output        io_packet_out_yHops,
  output        io_periphery_active,
  output [39:0] io_periphery_cache_addr,
  output [15:0] io_periphery_cache_wdata,
  output        io_periphery_cache_start,
  output [1:0]  io_periphery_cache_cmd,
  input  [15:0] io_periphery_cache_rdata,
  input         io_periphery_cache_done,
  input         io_periphery_cache_idle,
  output        io_periphery_gmem_access_failure_error,
  output        io_periphery_exception_error,
  output [15:0] io_periphery_exception_id,
  input  [63:0] io_periphery_debug_time,
  output        io_periphery_dynamic_cycle
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_6;
  reg [31:0] _RAND_7;
  reg [31:0] _RAND_8;
  reg [31:0] _RAND_9;
  reg [31:0] _RAND_10;
  reg [31:0] _RAND_11;
`endif // RANDOMIZE_REG_INIT
  wire  fetch_stage_clock; // @[Processor.scala 106:28]
  wire  fetch_stage_reset; // @[Processor.scala 106:28]
  wire  fetch_stage_io_execution_enable; // @[Processor.scala 106:28]
  wire [63:0] fetch_stage_io_instruction; // @[Processor.scala 106:28]
  wire  fetch_stage_io_programmer_enable; // @[Processor.scala 106:28]
  wire [63:0] fetch_stage_io_programmer_instruction; // @[Processor.scala 106:28]
  wire [11:0] fetch_stage_io_programmer_address; // @[Processor.scala 106:28]
  wire  decode_stage_clock; // @[Processor.scala 107:28]
  wire [63:0] decode_stage_io_instruction; // @[Processor.scala 107:28]
  wire [10:0] decode_stage_io_pipe_out_rd; // @[Processor.scala 107:28]
  wire [10:0] decode_stage_io_pipe_out_rs1; // @[Processor.scala 107:28]
  wire [10:0] decode_stage_io_pipe_out_rs2; // @[Processor.scala 107:28]
  wire [10:0] decode_stage_io_pipe_out_rs3; // @[Processor.scala 107:28]
  wire [10:0] decode_stage_io_pipe_out_rs4; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_cust; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_arith; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_lload; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_lstore; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_send; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_set; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_expect; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_predicate; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_set_carry; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_set_lut_data; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_0; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_1; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_2; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_3; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_4; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_5; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_6; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_7; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_8; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_9; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_10; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_11; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_12; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_13; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_14; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_15; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_16; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_17; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_18; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_19; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_20; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_21; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_22; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_23; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_24; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_25; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_26; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_27; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_28; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_29; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_30; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_configure_luts_31; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_slice; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_mul; // @[Processor.scala 107:28]
  wire  decode_stage_io_pipe_out_opcode_mulh; // @[Processor.scala 107:28]
  wire [4:0] decode_stage_io_pipe_out_funct; // @[Processor.scala 107:28]
  wire [15:0] decode_stage_io_pipe_out_immediate; // @[Processor.scala 107:28]
  wire [3:0] decode_stage_io_pipe_out_slice_ofst; // @[Processor.scala 107:28]
  wire  execute_stage_clock; // @[Processor.scala 108:29]
  wire [10:0] execute_stage_io_pipe_in_rd; // @[Processor.scala 108:29]
  wire [10:0] execute_stage_io_pipe_in_rs4; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_cust; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_arith; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_lload; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_lstore; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_send; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_set; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_expect; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_predicate; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_set_carry; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_0; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_1; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_2; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_3; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_4; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_5; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_6; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_7; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_8; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_9; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_10; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_11; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_12; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_13; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_14; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_15; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_16; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_17; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_18; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_19; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_20; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_21; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_22; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_23; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_24; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_25; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_26; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_27; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_28; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_29; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_30; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_configure_luts_31; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_slice; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_in_opcode_mulh; // @[Processor.scala 108:29]
  wire [4:0] execute_stage_io_pipe_in_funct; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_pipe_in_immediate; // @[Processor.scala 108:29]
  wire [3:0] execute_stage_io_pipe_in_slice_ofst; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_regs_in_rs1; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_regs_in_rs2; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_regs_in_rs3; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_regs_in_rs4; // @[Processor.scala 108:29]
  wire  execute_stage_io_carry_in; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_out_opcode_cust; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_out_opcode_arith; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_out_opcode_lload; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_out_opcode_lstore; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_out_opcode_send; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_out_opcode_set; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_out_opcode_expect; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_out_opcode_slice; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_out_opcode_mulh; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_pipe_out_data; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_pipe_out_result; // @[Processor.scala 108:29]
  wire [10:0] execute_stage_io_pipe_out_rd; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_pipe_out_immediate; // @[Processor.scala 108:29]
  wire  execute_stage_io_pipe_out_pred; // @[Processor.scala 108:29]
  wire [5:0] execute_stage_io_carry_rd; // @[Processor.scala 108:29]
  wire  execute_stage_io_carry_wen; // @[Processor.scala 108:29]
  wire  execute_stage_io_carry_din; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_0; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_1; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_2; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_3; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_4; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_5; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_6; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_7; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_8; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_9; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_10; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_11; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_12; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_13; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_14; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_15; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_16; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_17; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_18; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_19; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_20; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_21; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_22; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_23; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_24; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_25; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_26; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_27; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_28; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_29; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_30; // @[Processor.scala 108:29]
  wire [15:0] execute_stage_io_lutdata_din_31; // @[Processor.scala 108:29]
  wire  memory_stage_clock; // @[Processor.scala 116:28]
  wire  memory_stage_reset; // @[Processor.scala 116:28]
  wire  memory_stage_io_pipe_in_opcode_cust; // @[Processor.scala 116:28]
  wire  memory_stage_io_pipe_in_opcode_arith; // @[Processor.scala 116:28]
  wire  memory_stage_io_pipe_in_opcode_lload; // @[Processor.scala 116:28]
  wire  memory_stage_io_pipe_in_opcode_lstore; // @[Processor.scala 116:28]
  wire  memory_stage_io_pipe_in_opcode_send; // @[Processor.scala 116:28]
  wire  memory_stage_io_pipe_in_opcode_set; // @[Processor.scala 116:28]
  wire  memory_stage_io_pipe_in_opcode_slice; // @[Processor.scala 116:28]
  wire  memory_stage_io_pipe_in_opcode_mulh; // @[Processor.scala 116:28]
  wire [15:0] memory_stage_io_pipe_in_data; // @[Processor.scala 116:28]
  wire [15:0] memory_stage_io_pipe_in_result; // @[Processor.scala 116:28]
  wire [10:0] memory_stage_io_pipe_in_rd; // @[Processor.scala 116:28]
  wire [15:0] memory_stage_io_pipe_in_immediate; // @[Processor.scala 116:28]
  wire  memory_stage_io_pipe_in_pred; // @[Processor.scala 116:28]
  wire [15:0] memory_stage_io_pipe_out_result; // @[Processor.scala 116:28]
  wire [15:0] memory_stage_io_pipe_out_packet_data; // @[Processor.scala 116:28]
  wire [10:0] memory_stage_io_pipe_out_packet_address; // @[Processor.scala 116:28]
  wire  memory_stage_io_pipe_out_packet_valid; // @[Processor.scala 116:28]
  wire  memory_stage_io_pipe_out_packet_xHops; // @[Processor.scala 116:28]
  wire  memory_stage_io_pipe_out_packet_yHops; // @[Processor.scala 116:28]
  wire  memory_stage_io_pipe_out_write_back; // @[Processor.scala 116:28]
  wire [10:0] memory_stage_io_pipe_out_rd; // @[Processor.scala 116:28]
  wire  memory_stage_io_pipe_out_mulh; // @[Processor.scala 116:28]
  wire [11:0] memory_stage_io_local_memory_interface_raddr; // @[Processor.scala 116:28]
  wire [15:0] memory_stage_io_local_memory_interface_dout; // @[Processor.scala 116:28]
  wire  memory_stage_io_local_memory_interface_wen; // @[Processor.scala 116:28]
  wire [11:0] memory_stage_io_local_memory_interface_waddr; // @[Processor.scala 116:28]
  wire [15:0] memory_stage_io_local_memory_interface_din; // @[Processor.scala 116:28]
  wire  register_file_clock; // @[Processor.scala 118:35]
  wire [10:0] register_file_io_rs1_addr; // @[Processor.scala 118:35]
  wire [15:0] register_file_io_rs1_dout; // @[Processor.scala 118:35]
  wire [10:0] register_file_io_rs2_addr; // @[Processor.scala 118:35]
  wire [15:0] register_file_io_rs2_dout; // @[Processor.scala 118:35]
  wire [10:0] register_file_io_rs3_addr; // @[Processor.scala 118:35]
  wire [15:0] register_file_io_rs3_dout; // @[Processor.scala 118:35]
  wire [10:0] register_file_io_rs4_addr; // @[Processor.scala 118:35]
  wire [15:0] register_file_io_rs4_dout; // @[Processor.scala 118:35]
  wire [10:0] register_file_io_w_addr; // @[Processor.scala 118:35]
  wire [15:0] register_file_io_w_din; // @[Processor.scala 118:35]
  wire  register_file_io_w_en; // @[Processor.scala 118:35]
  wire  carry_register_file_clock; // @[Processor.scala 119:35]
  wire [5:0] carry_register_file_io_raddr; // @[Processor.scala 119:35]
  wire [5:0] carry_register_file_io_waddr; // @[Processor.scala 119:35]
  wire  carry_register_file_io_din; // @[Processor.scala 119:35]
  wire  carry_register_file_io_dout; // @[Processor.scala 119:35]
  wire  carry_register_file_io_wen; // @[Processor.scala 119:35]
  wire  lut_load_regs_clock; // @[Processor.scala 121:29]
  wire [4:0] lut_load_regs_io_waddr; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_din; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_0; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_1; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_2; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_3; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_4; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_5; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_6; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_7; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_8; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_9; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_10; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_11; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_12; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_13; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_14; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_15; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_16; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_17; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_18; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_19; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_20; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_21; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_22; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_23; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_24; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_25; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_26; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_27; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_28; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_29; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_30; // @[Processor.scala 121:29]
  wire [15:0] lut_load_regs_io_dout_31; // @[Processor.scala 121:29]
  wire  lut_load_regs_io_wen; // @[Processor.scala 121:29]
  wire  array_memory_clock; // @[Processor.scala 123:28]
  wire [9:0] array_memory_io_raddr; // @[Processor.scala 123:28]
  wire [15:0] array_memory_io_dout; // @[Processor.scala 123:28]
  wire  array_memory_io_wen; // @[Processor.scala 123:28]
  wire [9:0] array_memory_io_waddr; // @[Processor.scala 123:28]
  wire [15:0] array_memory_io_din; // @[Processor.scala 123:28]
  wire  multiplier_clock; // @[Processor.scala 133:35]
  wire [15:0] multiplier_io_in0; // @[Processor.scala 133:35]
  wire [15:0] multiplier_io_in1; // @[Processor.scala 133:35]
  wire [31:0] multiplier_io_out; // @[Processor.scala 133:35]
  wire  multiplier_io_valid_in; // @[Processor.scala 133:35]
  wire  multiplier_io_valid_out; // @[Processor.scala 133:35]
  reg [2:0] state; // @[Processor.scala 78:12]
  reg [15:0] countdown_timer; // @[Processor.scala 84:28]
  reg [11:0] program_body_length; // @[Processor.scala 86:36]
  reg [11:0] program_epilogue_length; // @[Processor.scala 87:36]
  reg [11:0] program_sleep_length; // @[Processor.scala 88:36]
  reg [11:0] program_pointer; // @[Processor.scala 90:28]
  reg [3:0] inst_builder_pos; // @[Processor.scala 101:38]
  reg [15:0] inst_builder_reg_0; // @[Processor.scala 102:40]
  reg [15:0] inst_builder_reg_1; // @[Processor.scala 102:40]
  reg [15:0] inst_builder_reg_2; // @[Processor.scala 102:40]
  wire [11:0] total_program_length = program_epilogue_length + program_body_length; // @[Processor.scala 141:52]
  wire  skip_exec = total_program_length == 12'h0; // @[Processor.scala 142:49]
  wire  skip_sleep = program_sleep_length == 12'h0; // @[Processor.scala 143:49]
  wire  soft_reset = 3'h0 == state; // @[Processor.scala 148:17]
  wire [1:0] _GEN_0 = io_packet_in_data == 16'h0 ? 2'h2 : 2'h1; // @[Processor.scala 153:41 158:17 164:28]
  wire [15:0] _GEN_2 = io_packet_in_valid ? io_packet_in_data : {{4'd0}, program_body_length}; // @[Processor.scala 151:32 152:29 86:36]
  wire [63:0] _fetch_stage_io_programmer_instruction_T = {io_packet_in_data,inst_builder_reg_0,inst_builder_reg_1,
    inst_builder_reg_2}; // @[Cat.scala 31:58]
  wire [11:0] _program_pointer_T_1 = program_pointer + 12'h1; // @[Processor.scala 180:64]
  wire [1:0] _GEN_6 = _program_pointer_T_1 == program_body_length ? 2'h2 : 2'h1; // @[Processor.scala 181:63 182:19 184:19]
  wire [2:0] _GEN_11 = inst_builder_pos[3] ? {{1'd0}, _GEN_6} : state; // @[Processor.scala 173:48 78:12]
  wire [3:0] _inst_builder_pos_T_2 = {inst_builder_pos[2:0],inst_builder_pos[3]}; // @[Processor.scala 189:54]
  wire  _GEN_12 = io_packet_in_valid & inst_builder_pos[3]; // @[Processor.scala 172:32 145:36]
  wire [15:0] _GEN_21 = io_packet_in_valid ? io_packet_in_data : {{4'd0}, program_epilogue_length}; // @[Processor.scala 200:32 201:33 87:36]
  wire [1:0] _GEN_22 = io_packet_in_valid ? 2'h3 : 2'h2; // @[Processor.scala 200:32 202:33 205:15]
  wire [15:0] _GEN_23 = io_packet_in_valid ? io_packet_in_data : {{4'd0}, program_sleep_length}; // @[Processor.scala 210:32 211:30 88:36]
  wire [2:0] _GEN_24 = io_packet_in_valid ? 3'h4 : 3'h3; // @[Processor.scala 210:32 212:30 215:15]
  wire [15:0] _GEN_25 = io_packet_in_valid ? io_packet_in_data : countdown_timer; // @[Processor.scala 220:32 221:25 84:28]
  wire [2:0] _GEN_26 = io_packet_in_valid ? 3'h5 : 3'h4; // @[Processor.scala 220:32 222:25 225:15]
  wire [2:0] _GEN_27 = skip_exec ? 3'h7 : 3'h6; // @[Processor.scala 231:25 232:27 238:27]
  wire [11:0] _GEN_28 = skip_exec ? program_sleep_length : total_program_length; // @[Processor.scala 231:25 233:27 239:27]
  wire [15:0] _countdown_timer_T_1 = countdown_timer - 16'h1; // @[Processor.scala 244:44]
  wire [2:0] _GEN_29 = countdown_timer == 16'h0 ? _GEN_27 : 3'h5; // @[Processor.scala 230:37 243:25]
  wire [15:0] _GEN_30 = countdown_timer == 16'h0 ? {{4'd0}, _GEN_28} : _countdown_timer_T_1; // @[Processor.scala 230:37 244:25]
  wire  _T_28 = countdown_timer == 16'h1; // @[Processor.scala 250:28]
  wire [2:0] _GEN_31 = skip_sleep ? 3'h6 : 3'h7; // @[Processor.scala 251:26 252:17 258:27]
  wire [11:0] _GEN_32 = skip_sleep ? total_program_length : program_sleep_length; // @[Processor.scala 251:26 256:27 259:27]
  wire [2:0] _GEN_33 = countdown_timer == 16'h1 ? _GEN_31 : 3'h6; // @[Processor.scala 250:37 263:25]
  wire [15:0] _GEN_34 = countdown_timer == 16'h1 ? {{4'd0}, _GEN_32} : _countdown_timer_T_1; // @[Processor.scala 250:37 264:25]
  wire [63:0] _fetch_stage_io_programmer_instruction_T_1 = {io_packet_in_data,28'h0,5'h0,io_packet_in_address,4'h1}; // @[Cat.scala 31:58]
  wire [11:0] _GEN_35 = io_packet_in_valid ? _program_pointer_T_1 : program_pointer; // @[Processor.scala 267:32 268:42 90:28]
  wire [2:0] _GEN_38 = _T_28 ? _GEN_27 : 3'h7; // @[Processor.scala 286:37 297:25]
  wire [15:0] _GEN_39 = _T_28 ? {{4'd0}, _GEN_28} : _countdown_timer_T_1; // @[Processor.scala 286:37 298:25]
  wire [11:0] _GEN_40 = _T_28 ? program_body_length : program_pointer; // @[Processor.scala 286:37 295:25 90:28]
  wire [2:0] _GEN_41 = 3'h7 == state ? _GEN_38 : state; // @[Processor.scala 148:17 78:12]
  wire [15:0] _GEN_42 = 3'h7 == state ? _GEN_39 : countdown_timer; // @[Processor.scala 148:17 84:28]
  wire [11:0] _GEN_43 = 3'h7 == state ? _GEN_40 : program_pointer; // @[Processor.scala 148:17 90:28]
  wire [2:0] _GEN_44 = 3'h6 == state ? _GEN_33 : _GEN_41; // @[Processor.scala 148:17]
  wire [15:0] _GEN_45 = 3'h6 == state ? _GEN_34 : _GEN_42; // @[Processor.scala 148:17]
  wire [11:0] _GEN_46 = 3'h6 == state ? _GEN_35 : _GEN_43; // @[Processor.scala 148:17]
  wire [2:0] _GEN_49 = 3'h5 == state ? _GEN_29 : _GEN_44; // @[Processor.scala 148:17]
  wire [15:0] _GEN_50 = 3'h5 == state ? _GEN_30 : _GEN_45; // @[Processor.scala 148:17]
  wire [11:0] _GEN_51 = 3'h5 == state ? program_pointer : _GEN_46; // @[Processor.scala 148:17 90:28]
  wire  _GEN_52 = 3'h5 == state ? 1'h0 : 3'h6 == state & io_packet_in_valid; // @[Processor.scala 148:17 145:36]
  wire [15:0] _GEN_54 = 3'h4 == state ? _GEN_25 : _GEN_50; // @[Processor.scala 148:17]
  wire [2:0] _GEN_55 = 3'h4 == state ? _GEN_26 : _GEN_49; // @[Processor.scala 148:17]
  wire [11:0] _GEN_56 = 3'h4 == state ? program_pointer : _GEN_51; // @[Processor.scala 148:17 90:28]
  wire  _GEN_57 = 3'h4 == state ? 1'h0 : _GEN_52; // @[Processor.scala 148:17 145:36]
  wire [15:0] _GEN_59 = 3'h3 == state ? _GEN_23 : {{4'd0}, program_sleep_length}; // @[Processor.scala 148:17 88:36]
  wire [2:0] _GEN_60 = 3'h3 == state ? _GEN_24 : _GEN_55; // @[Processor.scala 148:17]
  wire  _GEN_63 = 3'h3 == state ? 1'h0 : _GEN_57; // @[Processor.scala 148:17 145:36]
  wire [15:0] _GEN_65 = 3'h2 == state ? _GEN_21 : {{4'd0}, program_epilogue_length}; // @[Processor.scala 148:17 87:36]
  wire [15:0] _GEN_67 = 3'h2 == state ? {{4'd0}, program_sleep_length} : _GEN_59; // @[Processor.scala 148:17 88:36]
  wire  _GEN_70 = 3'h2 == state ? 1'h0 : _GEN_63; // @[Processor.scala 148:17 145:36]
  wire  _GEN_72 = 3'h1 == state ? _GEN_12 : _GEN_70; // @[Processor.scala 148:17]
  wire [15:0] _GEN_81 = 3'h1 == state ? {{4'd0}, program_epilogue_length} : _GEN_65; // @[Processor.scala 148:17 87:36]
  wire [15:0] _GEN_82 = 3'h1 == state ? {{4'd0}, program_sleep_length} : _GEN_67; // @[Processor.scala 148:17 88:36]
  wire [15:0] _GEN_85 = soft_reset ? _GEN_2 : {{4'd0}, program_body_length}; // @[Processor.scala 148:17 86:36]
  wire [15:0] _GEN_95 = soft_reset ? {{4'd0}, program_epilogue_length} : _GEN_81; // @[Processor.scala 148:17 87:36]
  wire [15:0] _GEN_96 = soft_reset ? {{4'd0}, program_sleep_length} : _GEN_82; // @[Processor.scala 148:17 88:36]
  wire [15:0] multiplier_res_high = multiplier_io_out[31:16]; // @[Processor.scala 369:43]
  wire [15:0] multiplier_res_low = multiplier_io_out[15:0]; // @[Processor.scala 370:43]
  wire [15:0] _register_file_io_w_din_T = memory_stage_io_pipe_out_mulh ? multiplier_res_high : multiplier_res_low; // @[Processor.scala 374:34]
  reg  exception_occurred; // @[Processor.scala 415:37]
  reg [15:0] exception_id; // @[Processor.scala 416:37]
  wire  exception_cond = execute_stage_io_pipe_out_opcode_expect & execute_stage_io_pipe_out_result == 16'h0; // @[Processor.scala 422:62]
  Fetch fetch_stage ( // @[Processor.scala 106:28]
    .clock(fetch_stage_clock),
    .reset(fetch_stage_reset),
    .io_execution_enable(fetch_stage_io_execution_enable),
    .io_instruction(fetch_stage_io_instruction),
    .io_programmer_enable(fetch_stage_io_programmer_enable),
    .io_programmer_instruction(fetch_stage_io_programmer_instruction),
    .io_programmer_address(fetch_stage_io_programmer_address)
  );
  Decode decode_stage ( // @[Processor.scala 107:28]
    .clock(decode_stage_clock),
    .io_instruction(decode_stage_io_instruction),
    .io_pipe_out_rd(decode_stage_io_pipe_out_rd),
    .io_pipe_out_rs1(decode_stage_io_pipe_out_rs1),
    .io_pipe_out_rs2(decode_stage_io_pipe_out_rs2),
    .io_pipe_out_rs3(decode_stage_io_pipe_out_rs3),
    .io_pipe_out_rs4(decode_stage_io_pipe_out_rs4),
    .io_pipe_out_opcode_cust(decode_stage_io_pipe_out_opcode_cust),
    .io_pipe_out_opcode_arith(decode_stage_io_pipe_out_opcode_arith),
    .io_pipe_out_opcode_lload(decode_stage_io_pipe_out_opcode_lload),
    .io_pipe_out_opcode_lstore(decode_stage_io_pipe_out_opcode_lstore),
    .io_pipe_out_opcode_send(decode_stage_io_pipe_out_opcode_send),
    .io_pipe_out_opcode_set(decode_stage_io_pipe_out_opcode_set),
    .io_pipe_out_opcode_expect(decode_stage_io_pipe_out_opcode_expect),
    .io_pipe_out_opcode_predicate(decode_stage_io_pipe_out_opcode_predicate),
    .io_pipe_out_opcode_set_carry(decode_stage_io_pipe_out_opcode_set_carry),
    .io_pipe_out_opcode_set_lut_data(decode_stage_io_pipe_out_opcode_set_lut_data),
    .io_pipe_out_opcode_configure_luts_0(decode_stage_io_pipe_out_opcode_configure_luts_0),
    .io_pipe_out_opcode_configure_luts_1(decode_stage_io_pipe_out_opcode_configure_luts_1),
    .io_pipe_out_opcode_configure_luts_2(decode_stage_io_pipe_out_opcode_configure_luts_2),
    .io_pipe_out_opcode_configure_luts_3(decode_stage_io_pipe_out_opcode_configure_luts_3),
    .io_pipe_out_opcode_configure_luts_4(decode_stage_io_pipe_out_opcode_configure_luts_4),
    .io_pipe_out_opcode_configure_luts_5(decode_stage_io_pipe_out_opcode_configure_luts_5),
    .io_pipe_out_opcode_configure_luts_6(decode_stage_io_pipe_out_opcode_configure_luts_6),
    .io_pipe_out_opcode_configure_luts_7(decode_stage_io_pipe_out_opcode_configure_luts_7),
    .io_pipe_out_opcode_configure_luts_8(decode_stage_io_pipe_out_opcode_configure_luts_8),
    .io_pipe_out_opcode_configure_luts_9(decode_stage_io_pipe_out_opcode_configure_luts_9),
    .io_pipe_out_opcode_configure_luts_10(decode_stage_io_pipe_out_opcode_configure_luts_10),
    .io_pipe_out_opcode_configure_luts_11(decode_stage_io_pipe_out_opcode_configure_luts_11),
    .io_pipe_out_opcode_configure_luts_12(decode_stage_io_pipe_out_opcode_configure_luts_12),
    .io_pipe_out_opcode_configure_luts_13(decode_stage_io_pipe_out_opcode_configure_luts_13),
    .io_pipe_out_opcode_configure_luts_14(decode_stage_io_pipe_out_opcode_configure_luts_14),
    .io_pipe_out_opcode_configure_luts_15(decode_stage_io_pipe_out_opcode_configure_luts_15),
    .io_pipe_out_opcode_configure_luts_16(decode_stage_io_pipe_out_opcode_configure_luts_16),
    .io_pipe_out_opcode_configure_luts_17(decode_stage_io_pipe_out_opcode_configure_luts_17),
    .io_pipe_out_opcode_configure_luts_18(decode_stage_io_pipe_out_opcode_configure_luts_18),
    .io_pipe_out_opcode_configure_luts_19(decode_stage_io_pipe_out_opcode_configure_luts_19),
    .io_pipe_out_opcode_configure_luts_20(decode_stage_io_pipe_out_opcode_configure_luts_20),
    .io_pipe_out_opcode_configure_luts_21(decode_stage_io_pipe_out_opcode_configure_luts_21),
    .io_pipe_out_opcode_configure_luts_22(decode_stage_io_pipe_out_opcode_configure_luts_22),
    .io_pipe_out_opcode_configure_luts_23(decode_stage_io_pipe_out_opcode_configure_luts_23),
    .io_pipe_out_opcode_configure_luts_24(decode_stage_io_pipe_out_opcode_configure_luts_24),
    .io_pipe_out_opcode_configure_luts_25(decode_stage_io_pipe_out_opcode_configure_luts_25),
    .io_pipe_out_opcode_configure_luts_26(decode_stage_io_pipe_out_opcode_configure_luts_26),
    .io_pipe_out_opcode_configure_luts_27(decode_stage_io_pipe_out_opcode_configure_luts_27),
    .io_pipe_out_opcode_configure_luts_28(decode_stage_io_pipe_out_opcode_configure_luts_28),
    .io_pipe_out_opcode_configure_luts_29(decode_stage_io_pipe_out_opcode_configure_luts_29),
    .io_pipe_out_opcode_configure_luts_30(decode_stage_io_pipe_out_opcode_configure_luts_30),
    .io_pipe_out_opcode_configure_luts_31(decode_stage_io_pipe_out_opcode_configure_luts_31),
    .io_pipe_out_opcode_slice(decode_stage_io_pipe_out_opcode_slice),
    .io_pipe_out_opcode_mul(decode_stage_io_pipe_out_opcode_mul),
    .io_pipe_out_opcode_mulh(decode_stage_io_pipe_out_opcode_mulh),
    .io_pipe_out_funct(decode_stage_io_pipe_out_funct),
    .io_pipe_out_immediate(decode_stage_io_pipe_out_immediate),
    .io_pipe_out_slice_ofst(decode_stage_io_pipe_out_slice_ofst)
  );
  ExecuteComb execute_stage ( // @[Processor.scala 108:29]
    .clock(execute_stage_clock),
    .io_pipe_in_rd(execute_stage_io_pipe_in_rd),
    .io_pipe_in_rs4(execute_stage_io_pipe_in_rs4),
    .io_pipe_in_opcode_cust(execute_stage_io_pipe_in_opcode_cust),
    .io_pipe_in_opcode_arith(execute_stage_io_pipe_in_opcode_arith),
    .io_pipe_in_opcode_lload(execute_stage_io_pipe_in_opcode_lload),
    .io_pipe_in_opcode_lstore(execute_stage_io_pipe_in_opcode_lstore),
    .io_pipe_in_opcode_send(execute_stage_io_pipe_in_opcode_send),
    .io_pipe_in_opcode_set(execute_stage_io_pipe_in_opcode_set),
    .io_pipe_in_opcode_expect(execute_stage_io_pipe_in_opcode_expect),
    .io_pipe_in_opcode_predicate(execute_stage_io_pipe_in_opcode_predicate),
    .io_pipe_in_opcode_set_carry(execute_stage_io_pipe_in_opcode_set_carry),
    .io_pipe_in_opcode_configure_luts_0(execute_stage_io_pipe_in_opcode_configure_luts_0),
    .io_pipe_in_opcode_configure_luts_1(execute_stage_io_pipe_in_opcode_configure_luts_1),
    .io_pipe_in_opcode_configure_luts_2(execute_stage_io_pipe_in_opcode_configure_luts_2),
    .io_pipe_in_opcode_configure_luts_3(execute_stage_io_pipe_in_opcode_configure_luts_3),
    .io_pipe_in_opcode_configure_luts_4(execute_stage_io_pipe_in_opcode_configure_luts_4),
    .io_pipe_in_opcode_configure_luts_5(execute_stage_io_pipe_in_opcode_configure_luts_5),
    .io_pipe_in_opcode_configure_luts_6(execute_stage_io_pipe_in_opcode_configure_luts_6),
    .io_pipe_in_opcode_configure_luts_7(execute_stage_io_pipe_in_opcode_configure_luts_7),
    .io_pipe_in_opcode_configure_luts_8(execute_stage_io_pipe_in_opcode_configure_luts_8),
    .io_pipe_in_opcode_configure_luts_9(execute_stage_io_pipe_in_opcode_configure_luts_9),
    .io_pipe_in_opcode_configure_luts_10(execute_stage_io_pipe_in_opcode_configure_luts_10),
    .io_pipe_in_opcode_configure_luts_11(execute_stage_io_pipe_in_opcode_configure_luts_11),
    .io_pipe_in_opcode_configure_luts_12(execute_stage_io_pipe_in_opcode_configure_luts_12),
    .io_pipe_in_opcode_configure_luts_13(execute_stage_io_pipe_in_opcode_configure_luts_13),
    .io_pipe_in_opcode_configure_luts_14(execute_stage_io_pipe_in_opcode_configure_luts_14),
    .io_pipe_in_opcode_configure_luts_15(execute_stage_io_pipe_in_opcode_configure_luts_15),
    .io_pipe_in_opcode_configure_luts_16(execute_stage_io_pipe_in_opcode_configure_luts_16),
    .io_pipe_in_opcode_configure_luts_17(execute_stage_io_pipe_in_opcode_configure_luts_17),
    .io_pipe_in_opcode_configure_luts_18(execute_stage_io_pipe_in_opcode_configure_luts_18),
    .io_pipe_in_opcode_configure_luts_19(execute_stage_io_pipe_in_opcode_configure_luts_19),
    .io_pipe_in_opcode_configure_luts_20(execute_stage_io_pipe_in_opcode_configure_luts_20),
    .io_pipe_in_opcode_configure_luts_21(execute_stage_io_pipe_in_opcode_configure_luts_21),
    .io_pipe_in_opcode_configure_luts_22(execute_stage_io_pipe_in_opcode_configure_luts_22),
    .io_pipe_in_opcode_configure_luts_23(execute_stage_io_pipe_in_opcode_configure_luts_23),
    .io_pipe_in_opcode_configure_luts_24(execute_stage_io_pipe_in_opcode_configure_luts_24),
    .io_pipe_in_opcode_configure_luts_25(execute_stage_io_pipe_in_opcode_configure_luts_25),
    .io_pipe_in_opcode_configure_luts_26(execute_stage_io_pipe_in_opcode_configure_luts_26),
    .io_pipe_in_opcode_configure_luts_27(execute_stage_io_pipe_in_opcode_configure_luts_27),
    .io_pipe_in_opcode_configure_luts_28(execute_stage_io_pipe_in_opcode_configure_luts_28),
    .io_pipe_in_opcode_configure_luts_29(execute_stage_io_pipe_in_opcode_configure_luts_29),
    .io_pipe_in_opcode_configure_luts_30(execute_stage_io_pipe_in_opcode_configure_luts_30),
    .io_pipe_in_opcode_configure_luts_31(execute_stage_io_pipe_in_opcode_configure_luts_31),
    .io_pipe_in_opcode_slice(execute_stage_io_pipe_in_opcode_slice),
    .io_pipe_in_opcode_mulh(execute_stage_io_pipe_in_opcode_mulh),
    .io_pipe_in_funct(execute_stage_io_pipe_in_funct),
    .io_pipe_in_immediate(execute_stage_io_pipe_in_immediate),
    .io_pipe_in_slice_ofst(execute_stage_io_pipe_in_slice_ofst),
    .io_regs_in_rs1(execute_stage_io_regs_in_rs1),
    .io_regs_in_rs2(execute_stage_io_regs_in_rs2),
    .io_regs_in_rs3(execute_stage_io_regs_in_rs3),
    .io_regs_in_rs4(execute_stage_io_regs_in_rs4),
    .io_carry_in(execute_stage_io_carry_in),
    .io_pipe_out_opcode_cust(execute_stage_io_pipe_out_opcode_cust),
    .io_pipe_out_opcode_arith(execute_stage_io_pipe_out_opcode_arith),
    .io_pipe_out_opcode_lload(execute_stage_io_pipe_out_opcode_lload),
    .io_pipe_out_opcode_lstore(execute_stage_io_pipe_out_opcode_lstore),
    .io_pipe_out_opcode_send(execute_stage_io_pipe_out_opcode_send),
    .io_pipe_out_opcode_set(execute_stage_io_pipe_out_opcode_set),
    .io_pipe_out_opcode_expect(execute_stage_io_pipe_out_opcode_expect),
    .io_pipe_out_opcode_slice(execute_stage_io_pipe_out_opcode_slice),
    .io_pipe_out_opcode_mulh(execute_stage_io_pipe_out_opcode_mulh),
    .io_pipe_out_data(execute_stage_io_pipe_out_data),
    .io_pipe_out_result(execute_stage_io_pipe_out_result),
    .io_pipe_out_rd(execute_stage_io_pipe_out_rd),
    .io_pipe_out_immediate(execute_stage_io_pipe_out_immediate),
    .io_pipe_out_pred(execute_stage_io_pipe_out_pred),
    .io_carry_rd(execute_stage_io_carry_rd),
    .io_carry_wen(execute_stage_io_carry_wen),
    .io_carry_din(execute_stage_io_carry_din),
    .io_lutdata_din_0(execute_stage_io_lutdata_din_0),
    .io_lutdata_din_1(execute_stage_io_lutdata_din_1),
    .io_lutdata_din_2(execute_stage_io_lutdata_din_2),
    .io_lutdata_din_3(execute_stage_io_lutdata_din_3),
    .io_lutdata_din_4(execute_stage_io_lutdata_din_4),
    .io_lutdata_din_5(execute_stage_io_lutdata_din_5),
    .io_lutdata_din_6(execute_stage_io_lutdata_din_6),
    .io_lutdata_din_7(execute_stage_io_lutdata_din_7),
    .io_lutdata_din_8(execute_stage_io_lutdata_din_8),
    .io_lutdata_din_9(execute_stage_io_lutdata_din_9),
    .io_lutdata_din_10(execute_stage_io_lutdata_din_10),
    .io_lutdata_din_11(execute_stage_io_lutdata_din_11),
    .io_lutdata_din_12(execute_stage_io_lutdata_din_12),
    .io_lutdata_din_13(execute_stage_io_lutdata_din_13),
    .io_lutdata_din_14(execute_stage_io_lutdata_din_14),
    .io_lutdata_din_15(execute_stage_io_lutdata_din_15),
    .io_lutdata_din_16(execute_stage_io_lutdata_din_16),
    .io_lutdata_din_17(execute_stage_io_lutdata_din_17),
    .io_lutdata_din_18(execute_stage_io_lutdata_din_18),
    .io_lutdata_din_19(execute_stage_io_lutdata_din_19),
    .io_lutdata_din_20(execute_stage_io_lutdata_din_20),
    .io_lutdata_din_21(execute_stage_io_lutdata_din_21),
    .io_lutdata_din_22(execute_stage_io_lutdata_din_22),
    .io_lutdata_din_23(execute_stage_io_lutdata_din_23),
    .io_lutdata_din_24(execute_stage_io_lutdata_din_24),
    .io_lutdata_din_25(execute_stage_io_lutdata_din_25),
    .io_lutdata_din_26(execute_stage_io_lutdata_din_26),
    .io_lutdata_din_27(execute_stage_io_lutdata_din_27),
    .io_lutdata_din_28(execute_stage_io_lutdata_din_28),
    .io_lutdata_din_29(execute_stage_io_lutdata_din_29),
    .io_lutdata_din_30(execute_stage_io_lutdata_din_30),
    .io_lutdata_din_31(execute_stage_io_lutdata_din_31)
  );
  MemoryAccess memory_stage ( // @[Processor.scala 116:28]
    .clock(memory_stage_clock),
    .reset(memory_stage_reset),
    .io_pipe_in_opcode_cust(memory_stage_io_pipe_in_opcode_cust),
    .io_pipe_in_opcode_arith(memory_stage_io_pipe_in_opcode_arith),
    .io_pipe_in_opcode_lload(memory_stage_io_pipe_in_opcode_lload),
    .io_pipe_in_opcode_lstore(memory_stage_io_pipe_in_opcode_lstore),
    .io_pipe_in_opcode_send(memory_stage_io_pipe_in_opcode_send),
    .io_pipe_in_opcode_set(memory_stage_io_pipe_in_opcode_set),
    .io_pipe_in_opcode_slice(memory_stage_io_pipe_in_opcode_slice),
    .io_pipe_in_opcode_mulh(memory_stage_io_pipe_in_opcode_mulh),
    .io_pipe_in_data(memory_stage_io_pipe_in_data),
    .io_pipe_in_result(memory_stage_io_pipe_in_result),
    .io_pipe_in_rd(memory_stage_io_pipe_in_rd),
    .io_pipe_in_immediate(memory_stage_io_pipe_in_immediate),
    .io_pipe_in_pred(memory_stage_io_pipe_in_pred),
    .io_pipe_out_result(memory_stage_io_pipe_out_result),
    .io_pipe_out_packet_data(memory_stage_io_pipe_out_packet_data),
    .io_pipe_out_packet_address(memory_stage_io_pipe_out_packet_address),
    .io_pipe_out_packet_valid(memory_stage_io_pipe_out_packet_valid),
    .io_pipe_out_packet_xHops(memory_stage_io_pipe_out_packet_xHops),
    .io_pipe_out_packet_yHops(memory_stage_io_pipe_out_packet_yHops),
    .io_pipe_out_write_back(memory_stage_io_pipe_out_write_back),
    .io_pipe_out_rd(memory_stage_io_pipe_out_rd),
    .io_pipe_out_mulh(memory_stage_io_pipe_out_mulh),
    .io_local_memory_interface_raddr(memory_stage_io_local_memory_interface_raddr),
    .io_local_memory_interface_dout(memory_stage_io_local_memory_interface_dout),
    .io_local_memory_interface_wen(memory_stage_io_local_memory_interface_wen),
    .io_local_memory_interface_waddr(memory_stage_io_local_memory_interface_waddr),
    .io_local_memory_interface_din(memory_stage_io_local_memory_interface_din)
  );
  RegisterFile register_file ( // @[Processor.scala 118:35]
    .clock(register_file_clock),
    .io_rs1_addr(register_file_io_rs1_addr),
    .io_rs1_dout(register_file_io_rs1_dout),
    .io_rs2_addr(register_file_io_rs2_addr),
    .io_rs2_dout(register_file_io_rs2_dout),
    .io_rs3_addr(register_file_io_rs3_addr),
    .io_rs3_dout(register_file_io_rs3_dout),
    .io_rs4_addr(register_file_io_rs4_addr),
    .io_rs4_dout(register_file_io_rs4_dout),
    .io_w_addr(register_file_io_w_addr),
    .io_w_din(register_file_io_w_din),
    .io_w_en(register_file_io_w_en)
  );
  CarryRegisterFile carry_register_file ( // @[Processor.scala 119:35]
    .clock(carry_register_file_clock),
    .io_raddr(carry_register_file_io_raddr),
    .io_waddr(carry_register_file_io_waddr),
    .io_din(carry_register_file_io_din),
    .io_dout(carry_register_file_io_dout),
    .io_wen(carry_register_file_io_wen)
  );
  LutLoadDataRegisterFile lut_load_regs ( // @[Processor.scala 121:29]
    .clock(lut_load_regs_clock),
    .io_waddr(lut_load_regs_io_waddr),
    .io_din(lut_load_regs_io_din),
    .io_dout_0(lut_load_regs_io_dout_0),
    .io_dout_1(lut_load_regs_io_dout_1),
    .io_dout_2(lut_load_regs_io_dout_2),
    .io_dout_3(lut_load_regs_io_dout_3),
    .io_dout_4(lut_load_regs_io_dout_4),
    .io_dout_5(lut_load_regs_io_dout_5),
    .io_dout_6(lut_load_regs_io_dout_6),
    .io_dout_7(lut_load_regs_io_dout_7),
    .io_dout_8(lut_load_regs_io_dout_8),
    .io_dout_9(lut_load_regs_io_dout_9),
    .io_dout_10(lut_load_regs_io_dout_10),
    .io_dout_11(lut_load_regs_io_dout_11),
    .io_dout_12(lut_load_regs_io_dout_12),
    .io_dout_13(lut_load_regs_io_dout_13),
    .io_dout_14(lut_load_regs_io_dout_14),
    .io_dout_15(lut_load_regs_io_dout_15),
    .io_dout_16(lut_load_regs_io_dout_16),
    .io_dout_17(lut_load_regs_io_dout_17),
    .io_dout_18(lut_load_regs_io_dout_18),
    .io_dout_19(lut_load_regs_io_dout_19),
    .io_dout_20(lut_load_regs_io_dout_20),
    .io_dout_21(lut_load_regs_io_dout_21),
    .io_dout_22(lut_load_regs_io_dout_22),
    .io_dout_23(lut_load_regs_io_dout_23),
    .io_dout_24(lut_load_regs_io_dout_24),
    .io_dout_25(lut_load_regs_io_dout_25),
    .io_dout_26(lut_load_regs_io_dout_26),
    .io_dout_27(lut_load_regs_io_dout_27),
    .io_dout_28(lut_load_regs_io_dout_28),
    .io_dout_29(lut_load_regs_io_dout_29),
    .io_dout_30(lut_load_regs_io_dout_30),
    .io_dout_31(lut_load_regs_io_dout_31),
    .io_wen(lut_load_regs_io_wen)
  );
  SimpleDualPortMemory_5 array_memory ( // @[Processor.scala 123:28]
    .clock(array_memory_clock),
    .io_raddr(array_memory_io_raddr),
    .io_dout(array_memory_io_dout),
    .io_wen(array_memory_io_wen),
    .io_waddr(array_memory_io_waddr),
    .io_din(array_memory_io_din)
  );
  Multiplier multiplier ( // @[Processor.scala 133:35]
    .clock(multiplier_clock),
    .io_in0(multiplier_io_in0),
    .io_in1(multiplier_io_in1),
    .io_out(multiplier_io_out),
    .io_valid_in(multiplier_io_valid_in),
    .io_valid_out(multiplier_io_valid_out)
  );
  assign io_packet_out_data = memory_stage_io_pipe_out_packet_data; // @[Processor.scala 403:17]
  assign io_packet_out_address = memory_stage_io_pipe_out_packet_address; // @[Processor.scala 403:17]
  assign io_packet_out_valid = memory_stage_io_pipe_out_packet_valid; // @[Processor.scala 403:17]
  assign io_packet_out_xHops = memory_stage_io_pipe_out_packet_xHops; // @[Processor.scala 403:17]
  assign io_packet_out_yHops = memory_stage_io_pipe_out_packet_yHops; // @[Processor.scala 403:17]
  assign io_periphery_active = state == 3'h6; // @[Processor.scala 304:45]
  assign io_periphery_cache_addr = 40'h0;
  assign io_periphery_cache_wdata = 16'h0;
  assign io_periphery_cache_start = 1'h0;
  assign io_periphery_cache_cmd = 2'h0;
  assign io_periphery_gmem_access_failure_error = 1'h0;
  assign io_periphery_exception_error = exception_occurred; // @[Processor.scala 435:32]
  assign io_periphery_exception_id = exception_id; // @[Processor.scala 434:32]
  assign io_periphery_dynamic_cycle = 1'h0; // @[Processor.scala 441:30]
  assign fetch_stage_clock = clock;
  assign fetch_stage_reset = reset;
  assign fetch_stage_io_execution_enable = state == 3'h6; // @[Processor.scala 303:45]
  assign fetch_stage_io_programmer_enable = soft_reset ? 1'h0 : _GEN_72; // @[Processor.scala 148:17 145:36]
  assign fetch_stage_io_programmer_instruction = 3'h1 == state ? _fetch_stage_io_programmer_instruction_T :
    _fetch_stage_io_programmer_instruction_T_1; // @[Processor.scala 148:17]
  assign fetch_stage_io_programmer_address = program_pointer; // @[Processor.scala 173:48 179:45]
  assign decode_stage_clock = clock;
  assign decode_stage_io_instruction = fetch_stage_io_instruction; // @[Processor.scala 339:31]
  assign execute_stage_clock = clock;
  assign execute_stage_io_pipe_in_rd = decode_stage_io_pipe_out_rd; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_rs4 = decode_stage_io_pipe_out_rs4; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_cust = decode_stage_io_pipe_out_opcode_cust; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_arith = decode_stage_io_pipe_out_opcode_arith; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_lload = decode_stage_io_pipe_out_opcode_lload; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_lstore = decode_stage_io_pipe_out_opcode_lstore; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_send = decode_stage_io_pipe_out_opcode_send; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_set = decode_stage_io_pipe_out_opcode_set; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_expect = decode_stage_io_pipe_out_opcode_expect; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_predicate = decode_stage_io_pipe_out_opcode_predicate; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_set_carry = decode_stage_io_pipe_out_opcode_set_carry; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_0 = decode_stage_io_pipe_out_opcode_configure_luts_0; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_1 = decode_stage_io_pipe_out_opcode_configure_luts_1; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_2 = decode_stage_io_pipe_out_opcode_configure_luts_2; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_3 = decode_stage_io_pipe_out_opcode_configure_luts_3; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_4 = decode_stage_io_pipe_out_opcode_configure_luts_4; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_5 = decode_stage_io_pipe_out_opcode_configure_luts_5; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_6 = decode_stage_io_pipe_out_opcode_configure_luts_6; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_7 = decode_stage_io_pipe_out_opcode_configure_luts_7; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_8 = decode_stage_io_pipe_out_opcode_configure_luts_8; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_9 = decode_stage_io_pipe_out_opcode_configure_luts_9; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_10 = decode_stage_io_pipe_out_opcode_configure_luts_10; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_11 = decode_stage_io_pipe_out_opcode_configure_luts_11; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_12 = decode_stage_io_pipe_out_opcode_configure_luts_12; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_13 = decode_stage_io_pipe_out_opcode_configure_luts_13; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_14 = decode_stage_io_pipe_out_opcode_configure_luts_14; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_15 = decode_stage_io_pipe_out_opcode_configure_luts_15; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_16 = decode_stage_io_pipe_out_opcode_configure_luts_16; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_17 = decode_stage_io_pipe_out_opcode_configure_luts_17; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_18 = decode_stage_io_pipe_out_opcode_configure_luts_18; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_19 = decode_stage_io_pipe_out_opcode_configure_luts_19; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_20 = decode_stage_io_pipe_out_opcode_configure_luts_20; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_21 = decode_stage_io_pipe_out_opcode_configure_luts_21; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_22 = decode_stage_io_pipe_out_opcode_configure_luts_22; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_23 = decode_stage_io_pipe_out_opcode_configure_luts_23; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_24 = decode_stage_io_pipe_out_opcode_configure_luts_24; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_25 = decode_stage_io_pipe_out_opcode_configure_luts_25; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_26 = decode_stage_io_pipe_out_opcode_configure_luts_26; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_27 = decode_stage_io_pipe_out_opcode_configure_luts_27; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_28 = decode_stage_io_pipe_out_opcode_configure_luts_28; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_29 = decode_stage_io_pipe_out_opcode_configure_luts_29; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_30 = decode_stage_io_pipe_out_opcode_configure_luts_30; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_configure_luts_31 = decode_stage_io_pipe_out_opcode_configure_luts_31; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_slice = decode_stage_io_pipe_out_opcode_slice; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_opcode_mulh = decode_stage_io_pipe_out_opcode_mulh; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_funct = decode_stage_io_pipe_out_funct; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_immediate = decode_stage_io_pipe_out_immediate; // @[Processor.scala 342:28]
  assign execute_stage_io_pipe_in_slice_ofst = decode_stage_io_pipe_out_slice_ofst; // @[Processor.scala 342:28]
  assign execute_stage_io_regs_in_rs1 = register_file_io_rs1_dout; // @[Processor.scala 344:32]
  assign execute_stage_io_regs_in_rs2 = register_file_io_rs2_dout; // @[Processor.scala 349:32]
  assign execute_stage_io_regs_in_rs3 = register_file_io_rs3_dout; // @[Processor.scala 354:32]
  assign execute_stage_io_regs_in_rs4 = register_file_io_rs4_dout; // @[Processor.scala 359:32]
  assign execute_stage_io_carry_in = carry_register_file_io_dout; // @[Processor.scala 381:32]
  assign execute_stage_io_lutdata_din_0 = lut_load_regs_io_dout_0; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_1 = lut_load_regs_io_dout_1; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_2 = lut_load_regs_io_dout_2; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_3 = lut_load_regs_io_dout_3; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_4 = lut_load_regs_io_dout_4; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_5 = lut_load_regs_io_dout_5; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_6 = lut_load_regs_io_dout_6; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_7 = lut_load_regs_io_dout_7; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_8 = lut_load_regs_io_dout_8; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_9 = lut_load_regs_io_dout_9; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_10 = lut_load_regs_io_dout_10; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_11 = lut_load_regs_io_dout_11; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_12 = lut_load_regs_io_dout_12; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_13 = lut_load_regs_io_dout_13; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_14 = lut_load_regs_io_dout_14; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_15 = lut_load_regs_io_dout_15; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_16 = lut_load_regs_io_dout_16; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_17 = lut_load_regs_io_dout_17; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_18 = lut_load_regs_io_dout_18; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_19 = lut_load_regs_io_dout_19; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_20 = lut_load_regs_io_dout_20; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_21 = lut_load_regs_io_dout_21; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_22 = lut_load_regs_io_dout_22; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_23 = lut_load_regs_io_dout_23; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_24 = lut_load_regs_io_dout_24; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_25 = lut_load_regs_io_dout_25; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_26 = lut_load_regs_io_dout_26; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_27 = lut_load_regs_io_dout_27; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_28 = lut_load_regs_io_dout_28; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_29 = lut_load_regs_io_dout_29; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_30 = lut_load_regs_io_dout_30; // @[Processor.scala 389:32]
  assign execute_stage_io_lutdata_din_31 = lut_load_regs_io_dout_31; // @[Processor.scala 389:32]
  assign memory_stage_clock = clock;
  assign memory_stage_reset = reset;
  assign memory_stage_io_pipe_in_opcode_cust = execute_stage_io_pipe_out_opcode_cust; // @[Processor.scala 399:47]
  assign memory_stage_io_pipe_in_opcode_arith = execute_stage_io_pipe_out_opcode_arith; // @[Processor.scala 399:47]
  assign memory_stage_io_pipe_in_opcode_lload = execute_stage_io_pipe_out_opcode_lload; // @[Processor.scala 399:47]
  assign memory_stage_io_pipe_in_opcode_lstore = execute_stage_io_pipe_out_opcode_lstore; // @[Processor.scala 399:47]
  assign memory_stage_io_pipe_in_opcode_send = execute_stage_io_pipe_out_opcode_send; // @[Processor.scala 399:47]
  assign memory_stage_io_pipe_in_opcode_set = execute_stage_io_pipe_out_opcode_set; // @[Processor.scala 399:47]
  assign memory_stage_io_pipe_in_opcode_slice = execute_stage_io_pipe_out_opcode_slice; // @[Processor.scala 399:47]
  assign memory_stage_io_pipe_in_opcode_mulh = execute_stage_io_pipe_out_opcode_mulh; // @[Processor.scala 399:47]
  assign memory_stage_io_pipe_in_data = execute_stage_io_pipe_out_data; // @[Processor.scala 399:47]
  assign memory_stage_io_pipe_in_result = execute_stage_io_pipe_out_result; // @[Processor.scala 399:47]
  assign memory_stage_io_pipe_in_rd = execute_stage_io_pipe_out_rd; // @[Processor.scala 399:47]
  assign memory_stage_io_pipe_in_immediate = execute_stage_io_pipe_out_immediate; // @[Processor.scala 399:47]
  assign memory_stage_io_pipe_in_pred = execute_stage_io_pipe_out_pred; // @[Processor.scala 399:47]
  assign memory_stage_io_local_memory_interface_dout = array_memory_io_dout; // @[Processor.scala 398:47]
  assign register_file_clock = clock;
  assign register_file_io_rs1_addr = decode_stage_io_pipe_out_rs1; // @[Processor.scala 364:29]
  assign register_file_io_rs2_addr = decode_stage_io_pipe_out_rs2; // @[Processor.scala 365:29]
  assign register_file_io_rs3_addr = decode_stage_io_pipe_out_rs3; // @[Processor.scala 366:29]
  assign register_file_io_rs4_addr = decode_stage_io_pipe_out_rs4; // @[Processor.scala 367:29]
  assign register_file_io_w_addr = memory_stage_io_pipe_out_rd; // @[Processor.scala 401:27]
  assign register_file_io_w_din = multiplier_io_valid_out ? _register_file_io_w_din_T : memory_stage_io_pipe_out_result; // @[Processor.scala 373:33 374:28 376:28]
  assign register_file_io_w_en = memory_stage_io_pipe_out_write_back; // @[Processor.scala 378:25]
  assign carry_register_file_clock = clock;
  assign carry_register_file_io_raddr = decode_stage_io_pipe_out_rs3[5:0]; // @[Processor.scala 380:32]
  assign carry_register_file_io_waddr = execute_stage_io_carry_rd; // @[Processor.scala 383:32]
  assign carry_register_file_io_din = execute_stage_io_carry_din; // @[Processor.scala 384:32]
  assign carry_register_file_io_wen = execute_stage_io_carry_wen; // @[Processor.scala 382:32]
  assign lut_load_regs_clock = clock;
  assign lut_load_regs_io_waddr = decode_stage_io_pipe_out_funct; // @[Processor.scala 387:32]
  assign lut_load_regs_io_din = decode_stage_io_pipe_out_immediate; // @[Processor.scala 386:32]
  assign lut_load_regs_io_wen = decode_stage_io_pipe_out_opcode_set_lut_data; // @[Processor.scala 388:32]
  assign array_memory_clock = clock;
  assign array_memory_io_raddr = memory_stage_io_local_memory_interface_raddr[9:0]; // @[Processor.scala 397:42]
  assign array_memory_io_wen = memory_stage_io_local_memory_interface_wen; // @[Processor.scala 397:42]
  assign array_memory_io_waddr = memory_stage_io_local_memory_interface_waddr[9:0]; // @[Processor.scala 397:42]
  assign array_memory_io_din = memory_stage_io_local_memory_interface_din; // @[Processor.scala 397:42]
  assign multiplier_clock = clock;
  assign multiplier_io_in0 = register_file_io_rs1_dout; // @[Processor.scala 392:26]
  assign multiplier_io_in1 = register_file_io_rs2_dout; // @[Processor.scala 393:26]
  assign multiplier_io_valid_in = decode_stage_io_pipe_out_opcode_mul | decode_stage_io_pipe_out_opcode_mulh; // @[Processor.scala 394:65]
  always @(posedge clock) begin
    if (reset) begin // @[Processor.scala 78:12]
      state <= 3'h0; // @[Processor.scala 78:12]
    end else if (soft_reset) begin // @[Processor.scala 148:17]
      if (io_packet_in_valid) begin // @[Processor.scala 151:32]
        state <= {{1'd0}, _GEN_0};
      end
    end else if (3'h1 == state) begin // @[Processor.scala 148:17]
      if (io_packet_in_valid) begin // @[Processor.scala 172:32]
        state <= _GEN_11;
      end
    end else if (3'h2 == state) begin // @[Processor.scala 148:17]
      state <= {{1'd0}, _GEN_22};
    end else begin
      state <= _GEN_60;
    end
    if (!(soft_reset)) begin // @[Processor.scala 148:17]
      if (!(3'h1 == state)) begin // @[Processor.scala 148:17]
        if (!(3'h2 == state)) begin // @[Processor.scala 148:17]
          if (!(3'h3 == state)) begin // @[Processor.scala 148:17]
            countdown_timer <= _GEN_54;
          end
        end
      end
    end
    program_body_length <= _GEN_85[11:0];
    program_epilogue_length <= _GEN_95[11:0];
    program_sleep_length <= _GEN_96[11:0];
    if (soft_reset) begin // @[Processor.scala 148:17]
      if (io_packet_in_valid) begin // @[Processor.scala 151:32]
        program_pointer <= 12'h0; // @[Processor.scala 167:25]
      end
    end else if (3'h1 == state) begin // @[Processor.scala 148:17]
      if (io_packet_in_valid) begin // @[Processor.scala 172:32]
        if (inst_builder_pos[3]) begin // @[Processor.scala 173:48]
          program_pointer <= _program_pointer_T_1; // @[Processor.scala 180:45]
        end
      end
    end else if (!(3'h2 == state)) begin // @[Processor.scala 148:17]
      if (!(3'h3 == state)) begin // @[Processor.scala 148:17]
        program_pointer <= _GEN_56;
      end
    end
    if (soft_reset) begin // @[Processor.scala 148:17]
      if (io_packet_in_valid) begin // @[Processor.scala 151:32]
        if (!(io_packet_in_data == 16'h0)) begin // @[Processor.scala 153:41]
          inst_builder_pos <= 4'h1; // @[Processor.scala 165:28]
        end
      end
    end else if (3'h1 == state) begin // @[Processor.scala 148:17]
      if (io_packet_in_valid) begin // @[Processor.scala 172:32]
        inst_builder_pos <= _inst_builder_pos_T_2; // @[Processor.scala 189:26]
      end
    end
    if (!(soft_reset)) begin // @[Processor.scala 148:17]
      if (3'h1 == state) begin // @[Processor.scala 148:17]
        if (io_packet_in_valid) begin // @[Processor.scala 172:32]
          inst_builder_reg_0 <= io_packet_in_data; // @[Processor.scala 191:29]
        end
      end
    end
    if (!(soft_reset)) begin // @[Processor.scala 148:17]
      if (3'h1 == state) begin // @[Processor.scala 148:17]
        if (io_packet_in_valid) begin // @[Processor.scala 172:32]
          inst_builder_reg_1 <= inst_builder_reg_0; // @[Processor.scala 193:31]
        end
      end
    end
    if (!(soft_reset)) begin // @[Processor.scala 148:17]
      if (3'h1 == state) begin // @[Processor.scala 148:17]
        if (io_packet_in_valid) begin // @[Processor.scala 172:32]
          inst_builder_reg_2 <= inst_builder_reg_1; // @[Processor.scala 193:31]
        end
      end
    end
    if (soft_reset) begin // @[Processor.scala 436:20]
      exception_occurred <= 1'h0; // @[Processor.scala 438:24]
    end else begin
      exception_occurred <= exception_cond; // @[Processor.scala 428:22]
    end
    if (exception_cond) begin // @[Processor.scala 430:24]
      exception_id <= execute_stage_io_pipe_out_immediate; // @[Processor.scala 431:18]
    end
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{`RANDOM}};
  state = _RAND_0[2:0];
  _RAND_1 = {1{`RANDOM}};
  countdown_timer = _RAND_1[15:0];
  _RAND_2 = {1{`RANDOM}};
  program_body_length = _RAND_2[11:0];
  _RAND_3 = {1{`RANDOM}};
  program_epilogue_length = _RAND_3[11:0];
  _RAND_4 = {1{`RANDOM}};
  program_sleep_length = _RAND_4[11:0];
  _RAND_5 = {1{`RANDOM}};
  program_pointer = _RAND_5[11:0];
  _RAND_6 = {1{`RANDOM}};
  inst_builder_pos = _RAND_6[3:0];
  _RAND_7 = {1{`RANDOM}};
  inst_builder_reg_0 = _RAND_7[15:0];
  _RAND_8 = {1{`RANDOM}};
  inst_builder_reg_1 = _RAND_8[15:0];
  _RAND_9 = {1{`RANDOM}};
  inst_builder_reg_2 = _RAND_9[15:0];
  _RAND_10 = {1{`RANDOM}};
  exception_occurred = _RAND_10[0:0];
  _RAND_11 = {1{`RANDOM}};
  exception_id = _RAND_11[15:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
