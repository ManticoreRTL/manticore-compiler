module Queue(
  input         clock,
  input         reset,
  output        io_enq_ready,
  input         io_enq_valid,
  input  [3:0]  io_enq_bits_opcode,
  input  [3:0]  io_enq_bits_flags,
  input  [47:0] io_enq_bits_arguments,
  input         io_deq_ready,
  output        io_deq_valid,
  output [3:0]  io_deq_bits_opcode,
  output [3:0]  io_deq_bits_flags,
  output [47:0] io_deq_bits_arguments
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [63:0] _RAND_2;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
`endif // RANDOMIZE_REG_INIT
  reg [3:0] ram_opcode [0:1]; // @[Decoupled.scala 218:16]
  wire [3:0] ram_opcode_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_opcode_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [3:0] ram_opcode_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_opcode_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_opcode_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_opcode_MPORT_en; // @[Decoupled.scala 218:16]
  reg [3:0] ram_flags [0:1]; // @[Decoupled.scala 218:16]
  wire [3:0] ram_flags_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_flags_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [3:0] ram_flags_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_flags_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_flags_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_flags_MPORT_en; // @[Decoupled.scala 218:16]
  reg [47:0] ram_arguments [0:1]; // @[Decoupled.scala 218:16]
  wire [47:0] ram_arguments_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_arguments_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [47:0] ram_arguments_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_arguments_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_arguments_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_arguments_MPORT_en; // @[Decoupled.scala 218:16]
  reg  value; // @[Counter.scala 60:40]
  reg  value_1; // @[Counter.scala 60:40]
  reg  maybe_full; // @[Decoupled.scala 221:27]
  wire  ptr_match = value == value_1; // @[Decoupled.scala 223:33]
  wire  empty = ptr_match & ~maybe_full; // @[Decoupled.scala 224:25]
  wire  full = ptr_match & maybe_full; // @[Decoupled.scala 225:24]
  wire  do_enq = io_enq_ready & io_enq_valid; // @[Decoupled.scala 40:37]
  wire  do_deq = io_deq_ready & io_deq_valid; // @[Decoupled.scala 40:37]
  assign ram_opcode_io_deq_bits_MPORT_addr = value_1;
  assign ram_opcode_io_deq_bits_MPORT_data = ram_opcode[ram_opcode_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_opcode_MPORT_data = io_enq_bits_opcode;
  assign ram_opcode_MPORT_addr = value;
  assign ram_opcode_MPORT_mask = 1'h1;
  assign ram_opcode_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_flags_io_deq_bits_MPORT_addr = value_1;
  assign ram_flags_io_deq_bits_MPORT_data = ram_flags[ram_flags_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_flags_MPORT_data = io_enq_bits_flags;
  assign ram_flags_MPORT_addr = value;
  assign ram_flags_MPORT_mask = 1'h1;
  assign ram_flags_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_arguments_io_deq_bits_MPORT_addr = value_1;
  assign ram_arguments_io_deq_bits_MPORT_data = ram_arguments[ram_arguments_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_arguments_MPORT_data = io_enq_bits_arguments;
  assign ram_arguments_MPORT_addr = value;
  assign ram_arguments_MPORT_mask = 1'h1;
  assign ram_arguments_MPORT_en = io_enq_ready & io_enq_valid;
  assign io_enq_ready = ~full; // @[Decoupled.scala 241:19]
  assign io_deq_valid = ~empty; // @[Decoupled.scala 240:19]
  assign io_deq_bits_opcode = ram_opcode_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_flags = ram_flags_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_arguments = ram_arguments_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  always @(posedge clock) begin
    if(ram_opcode_MPORT_en & ram_opcode_MPORT_mask) begin
      ram_opcode[ram_opcode_MPORT_addr] <= ram_opcode_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_flags_MPORT_en & ram_flags_MPORT_mask) begin
      ram_flags[ram_flags_MPORT_addr] <= ram_flags_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_arguments_MPORT_en & ram_arguments_MPORT_mask) begin
      ram_arguments[ram_arguments_MPORT_addr] <= ram_arguments_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if (reset) begin // @[Counter.scala 60:40]
      value <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_enq) begin // @[Decoupled.scala 229:17]
      value <= value + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Counter.scala 60:40]
      value_1 <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_deq) begin // @[Decoupled.scala 233:17]
      value_1 <= value_1 + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Decoupled.scala 221:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 221:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 236:28]
      maybe_full <= do_enq; // @[Decoupled.scala 237:16]
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
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_opcode[initvar] = _RAND_0[3:0];
  _RAND_1 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_flags[initvar] = _RAND_1[3:0];
  _RAND_2 = {2{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_arguments[initvar] = _RAND_2[47:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_3 = {1{`RANDOM}};
  value = _RAND_3[0:0];
  _RAND_4 = {1{`RANDOM}};
  value_1 = _RAND_4[0:0];
  _RAND_5 = {1{`RANDOM}};
  maybe_full = _RAND_5[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Queue_1(
  input         clock,
  input         reset,
  output        io_enq_ready,
  input         io_enq_valid,
  input  [3:0]  io_enq_bits_bits_opcode,
  input  [3:0]  io_enq_bits_bits_flags,
  input  [47:0] io_enq_bits_bits_arguments,
  input         io_deq_ready,
  output        io_deq_valid,
  output        io_deq_bits_last,
  output [3:0]  io_deq_bits_bits_opcode,
  output [3:0]  io_deq_bits_bits_flags,
  output [47:0] io_deq_bits_bits_arguments
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [63:0] _RAND_3;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_4;
`endif // RANDOMIZE_REG_INIT
  reg  ram_last [0:0]; // @[Decoupled.scala 218:16]
  wire  ram_last_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_last_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_last_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_last_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_last_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_last_MPORT_en; // @[Decoupled.scala 218:16]
  reg [3:0] ram_bits_opcode [0:0]; // @[Decoupled.scala 218:16]
  wire [3:0] ram_bits_opcode_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_bits_opcode_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [3:0] ram_bits_opcode_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_bits_opcode_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_bits_opcode_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_bits_opcode_MPORT_en; // @[Decoupled.scala 218:16]
  reg [3:0] ram_bits_flags [0:0]; // @[Decoupled.scala 218:16]
  wire [3:0] ram_bits_flags_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_bits_flags_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [3:0] ram_bits_flags_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_bits_flags_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_bits_flags_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_bits_flags_MPORT_en; // @[Decoupled.scala 218:16]
  reg [47:0] ram_bits_arguments [0:0]; // @[Decoupled.scala 218:16]
  wire [47:0] ram_bits_arguments_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_bits_arguments_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [47:0] ram_bits_arguments_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_bits_arguments_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_bits_arguments_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_bits_arguments_MPORT_en; // @[Decoupled.scala 218:16]
  reg  maybe_full; // @[Decoupled.scala 221:27]
  wire  empty = ~maybe_full; // @[Decoupled.scala 224:28]
  wire  _do_enq_T = io_enq_ready & io_enq_valid; // @[Decoupled.scala 40:37]
  wire  _do_deq_T = io_deq_ready & io_deq_valid; // @[Decoupled.scala 40:37]
  wire  _GEN_10 = io_deq_ready ? 1'h0 : _do_enq_T; // @[Decoupled.scala 249:27 Decoupled.scala 249:36]
  wire  do_enq = empty ? _GEN_10 : _do_enq_T; // @[Decoupled.scala 246:18]
  wire  do_deq = empty ? 1'h0 : _do_deq_T; // @[Decoupled.scala 246:18 Decoupled.scala 248:14]
  assign ram_last_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_last_io_deq_bits_MPORT_data = ram_last[ram_last_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_last_MPORT_data = 1'h1;
  assign ram_last_MPORT_addr = 1'h0;
  assign ram_last_MPORT_mask = 1'h1;
  assign ram_last_MPORT_en = empty ? _GEN_10 : _do_enq_T;
  assign ram_bits_opcode_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_bits_opcode_io_deq_bits_MPORT_data = ram_bits_opcode[ram_bits_opcode_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_bits_opcode_MPORT_data = io_enq_bits_bits_opcode;
  assign ram_bits_opcode_MPORT_addr = 1'h0;
  assign ram_bits_opcode_MPORT_mask = 1'h1;
  assign ram_bits_opcode_MPORT_en = empty ? _GEN_10 : _do_enq_T;
  assign ram_bits_flags_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_bits_flags_io_deq_bits_MPORT_data = ram_bits_flags[ram_bits_flags_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_bits_flags_MPORT_data = io_enq_bits_bits_flags;
  assign ram_bits_flags_MPORT_addr = 1'h0;
  assign ram_bits_flags_MPORT_mask = 1'h1;
  assign ram_bits_flags_MPORT_en = empty ? _GEN_10 : _do_enq_T;
  assign ram_bits_arguments_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_bits_arguments_io_deq_bits_MPORT_data = ram_bits_arguments[ram_bits_arguments_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_bits_arguments_MPORT_data = io_enq_bits_bits_arguments;
  assign ram_bits_arguments_MPORT_addr = 1'h0;
  assign ram_bits_arguments_MPORT_mask = 1'h1;
  assign ram_bits_arguments_MPORT_en = empty ? _GEN_10 : _do_enq_T;
  assign io_enq_ready = ~maybe_full; // @[Decoupled.scala 241:19]
  assign io_deq_valid = io_enq_valid | ~empty; // @[Decoupled.scala 245:25 Decoupled.scala 245:40 Decoupled.scala 240:16]
  assign io_deq_bits_last = empty | ram_last_io_deq_bits_MPORT_data; // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_bits_opcode = empty ? io_enq_bits_bits_opcode : ram_bits_opcode_io_deq_bits_MPORT_data; // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_bits_flags = empty ? io_enq_bits_bits_flags : ram_bits_flags_io_deq_bits_MPORT_data; // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_bits_arguments = empty ? io_enq_bits_bits_arguments : ram_bits_arguments_io_deq_bits_MPORT_data; // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  always @(posedge clock) begin
    if(ram_last_MPORT_en & ram_last_MPORT_mask) begin
      ram_last[ram_last_MPORT_addr] <= ram_last_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_bits_opcode_MPORT_en & ram_bits_opcode_MPORT_mask) begin
      ram_bits_opcode[ram_bits_opcode_MPORT_addr] <= ram_bits_opcode_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_bits_flags_MPORT_en & ram_bits_flags_MPORT_mask) begin
      ram_bits_flags[ram_bits_flags_MPORT_addr] <= ram_bits_flags_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_bits_arguments_MPORT_en & ram_bits_arguments_MPORT_mask) begin
      ram_bits_arguments[ram_bits_arguments_MPORT_addr] <= ram_bits_arguments_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if (reset) begin // @[Decoupled.scala 221:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 221:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 236:28]
      if (empty) begin // @[Decoupled.scala 246:18]
        if (io_deq_ready) begin // @[Decoupled.scala 249:27]
          maybe_full <= 1'h0; // @[Decoupled.scala 249:36]
        end else begin
          maybe_full <= _do_enq_T;
        end
      end else begin
        maybe_full <= _do_enq_T;
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
`ifdef RANDOMIZE_MEM_INIT
  _RAND_0 = {1{`RANDOM}};
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram_last[initvar] = _RAND_0[0:0];
  _RAND_1 = {1{`RANDOM}};
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram_bits_opcode[initvar] = _RAND_1[3:0];
  _RAND_2 = {1{`RANDOM}};
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram_bits_flags[initvar] = _RAND_2[3:0];
  _RAND_3 = {2{`RANDOM}};
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram_bits_arguments[initvar] = _RAND_3[47:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_4 = {1{`RANDOM}};
  maybe_full = _RAND_4[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Counter(
  input         clock,
  input         reset,
  input         io_value_ready,
  output [19:0] io_value_bits,
  input         io_resetValue
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
`endif // RANDOMIZE_REG_INIT
  reg [19:0] value; // @[Counter.scala 16:22]
  wire [19:0] _value_T_1 = value + 20'h1; // @[Counter.scala 24:22]
  assign io_value_bits = value; // @[Counter.scala 18:17]
  always @(posedge clock) begin
    if (reset) begin // @[Counter.scala 16:22]
      value <= 20'h0; // @[Counter.scala 16:22]
    end else if (io_resetValue) begin // @[Counter.scala 27:23]
      value <= 20'h0; // @[Counter.scala 28:11]
    end else if (io_value_ready) begin // @[Counter.scala 20:24]
      if (value == 20'hfffff) begin // @[Counter.scala 21:31]
        value <= 20'h0; // @[Counter.scala 22:13]
      end else begin
        value <= _value_T_1; // @[Counter.scala 24:13]
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
  value = _RAND_0[19:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module CountBy(
  input         clock,
  input         reset,
  input         io_value_ready,
  output [19:0] io_value_bits,
  input  [19:0] io_step,
  input         io_resetValue
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
`endif // RANDOMIZE_REG_INIT
  reg [19:0] value; // @[CountBy.scala 17:22]
  wire [20:0] _GEN_3 = {{1'd0}, io_step}; // @[CountBy.scala 22:24]
  wire [20:0] _T_1 = 21'h100000 - _GEN_3; // @[CountBy.scala 22:24]
  wire [20:0] _GEN_4 = {{1'd0}, value}; // @[CountBy.scala 22:16]
  wire [19:0] _value_T_1 = value + io_step; // @[CountBy.scala 25:22]
  assign io_value_bits = value; // @[CountBy.scala 19:17]
  always @(posedge clock) begin
    if (reset) begin // @[CountBy.scala 17:22]
      value <= 20'h0; // @[CountBy.scala 17:22]
    end else if (io_resetValue) begin // @[CountBy.scala 28:23]
      value <= 20'h0; // @[CountBy.scala 29:11]
    end else if (io_value_ready) begin // @[CountBy.scala 21:24]
      if (_GEN_4 >= _T_1) begin // @[CountBy.scala 22:36]
        value <= 20'h0; // @[CountBy.scala 23:13]
      end else begin
        value <= _value_T_1; // @[CountBy.scala 25:13]
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
  value = _RAND_0[19:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module SizeAndStrideHandler(
  input         clock,
  input         reset,
  output        io_in_ready,
  input         io_in_valid,
  input         io_in_bits_write,
  input  [19:0] io_in_bits_address,
  input  [19:0] io_in_bits_size,
  input  [2:0]  io_in_bits_stride,
  input         io_out_ready,
  output        io_out_valid,
  output        io_out_bits_write,
  output [19:0] io_out_bits_address
);
  wire  sizeCounter_clock; // @[Counter.scala 34:19]
  wire  sizeCounter_reset; // @[Counter.scala 34:19]
  wire  sizeCounter_io_value_ready; // @[Counter.scala 34:19]
  wire [19:0] sizeCounter_io_value_bits; // @[Counter.scala 34:19]
  wire  sizeCounter_io_resetValue; // @[Counter.scala 34:19]
  wire  addressCounter_clock; // @[CountBy.scala 35:19]
  wire  addressCounter_reset; // @[CountBy.scala 35:19]
  wire  addressCounter_io_value_ready; // @[CountBy.scala 35:19]
  wire [19:0] addressCounter_io_value_bits; // @[CountBy.scala 35:19]
  wire [19:0] addressCounter_io_step; // @[CountBy.scala 35:19]
  wire  addressCounter_io_resetValue; // @[CountBy.scala 35:19]
  wire [7:0] stride = 8'h1 << io_in_bits_stride; // @[SizeAndStrideHandler.scala 30:20]
  wire  fire = io_in_valid & io_out_ready; // @[SizeAndStrideHandler.scala 51:23]
  Counter sizeCounter ( // @[Counter.scala 34:19]
    .clock(sizeCounter_clock),
    .reset(sizeCounter_reset),
    .io_value_ready(sizeCounter_io_value_ready),
    .io_value_bits(sizeCounter_io_value_bits),
    .io_resetValue(sizeCounter_io_resetValue)
  );
  CountBy addressCounter ( // @[CountBy.scala 35:19]
    .clock(addressCounter_clock),
    .reset(addressCounter_reset),
    .io_value_ready(addressCounter_io_value_ready),
    .io_value_bits(addressCounter_io_value_bits),
    .io_step(addressCounter_io_step),
    .io_resetValue(addressCounter_io_resetValue)
  );
  assign io_in_ready = sizeCounter_io_value_bits == io_in_bits_size & io_out_ready; // @[SizeAndStrideHandler.scala 53:52 SizeAndStrideHandler.scala 54:14 SizeAndStrideHandler.scala 58:14]
  assign io_out_valid = io_in_valid; // @[SizeAndStrideHandler.scala 35:16]
  assign io_out_bits_write = io_in_bits_write; // @[SizeAndStrideHandler.scala 38:34]
  assign io_out_bits_address = io_in_bits_address + addressCounter_io_value_bits; // @[SizeAndStrideHandler.scala 48:44]
  assign sizeCounter_clock = clock;
  assign sizeCounter_reset = reset;
  assign sizeCounter_io_value_ready = sizeCounter_io_value_bits == io_in_bits_size ? 1'h0 : fire; // @[SizeAndStrideHandler.scala 53:52 Counter.scala 36:22 SizeAndStrideHandler.scala 59:32]
  assign sizeCounter_io_resetValue = sizeCounter_io_value_bits == io_in_bits_size & fire; // @[SizeAndStrideHandler.scala 53:52 SizeAndStrideHandler.scala 55:31 Counter.scala 35:21]
  assign addressCounter_clock = clock;
  assign addressCounter_reset = reset;
  assign addressCounter_io_value_ready = sizeCounter_io_value_bits == io_in_bits_size ? 1'h0 : fire; // @[SizeAndStrideHandler.scala 53:52 Counter.scala 36:22 SizeAndStrideHandler.scala 59:32]
  assign addressCounter_io_step = {{12'd0}, stride}; // @[SizeAndStrideHandler.scala 30:20]
  assign addressCounter_io_resetValue = sizeCounter_io_value_bits == io_in_bits_size & fire; // @[SizeAndStrideHandler.scala 53:52 SizeAndStrideHandler.scala 55:31 Counter.scala 35:21]
endmodule
module StrideHandler(
  input         clock,
  input         reset,
  output        io_in_ready,
  input         io_in_valid,
  input         io_in_bits_write,
  input  [19:0] io_in_bits_address,
  input  [19:0] io_in_bits_size,
  input  [2:0]  io_in_bits_stride,
  input         io_out_ready,
  output        io_out_valid,
  output        io_out_bits_write,
  output [19:0] io_out_bits_address,
  output [19:0] io_out_bits_size
);
  wire  handler_clock; // @[StrideHandler.scala 27:23]
  wire  handler_reset; // @[StrideHandler.scala 27:23]
  wire  handler_io_in_ready; // @[StrideHandler.scala 27:23]
  wire  handler_io_in_valid; // @[StrideHandler.scala 27:23]
  wire  handler_io_in_bits_write; // @[StrideHandler.scala 27:23]
  wire [19:0] handler_io_in_bits_address; // @[StrideHandler.scala 27:23]
  wire [19:0] handler_io_in_bits_size; // @[StrideHandler.scala 27:23]
  wire [2:0] handler_io_in_bits_stride; // @[StrideHandler.scala 27:23]
  wire  handler_io_out_ready; // @[StrideHandler.scala 27:23]
  wire  handler_io_out_valid; // @[StrideHandler.scala 27:23]
  wire  handler_io_out_bits_write; // @[StrideHandler.scala 27:23]
  wire [19:0] handler_io_out_bits_address; // @[StrideHandler.scala 27:23]
  SizeAndStrideHandler handler ( // @[StrideHandler.scala 27:23]
    .clock(handler_clock),
    .reset(handler_reset),
    .io_in_ready(handler_io_in_ready),
    .io_in_valid(handler_io_in_valid),
    .io_in_bits_write(handler_io_in_bits_write),
    .io_in_bits_address(handler_io_in_bits_address),
    .io_in_bits_size(handler_io_in_bits_size),
    .io_in_bits_stride(handler_io_in_bits_stride),
    .io_out_ready(handler_io_out_ready),
    .io_out_valid(handler_io_out_valid),
    .io_out_bits_write(handler_io_out_bits_write),
    .io_out_bits_address(handler_io_out_bits_address)
  );
  assign io_in_ready = io_in_bits_stride == 3'h0 ? io_out_ready : handler_io_in_ready; // @[StrideHandler.scala 41:32 StrideHandler.scala 49:14 StrideHandler.scala 52:19]
  assign io_out_valid = io_in_bits_stride == 3'h0 ? io_in_valid : handler_io_out_valid; // @[StrideHandler.scala 41:32 StrideHandler.scala 50:18 StrideHandler.scala 61:18]
  assign io_out_bits_write = io_in_bits_stride == 3'h0 ? io_in_bits_write : handler_io_out_bits_write; // @[StrideHandler.scala 41:32 StrideHandler.scala 44:36 StrideHandler.scala 55:36]
  assign io_out_bits_address = io_in_bits_stride == 3'h0 ? io_in_bits_address : handler_io_out_bits_address; // @[StrideHandler.scala 41:32 StrideHandler.scala 47:25 StrideHandler.scala 58:25]
  assign io_out_bits_size = io_in_bits_stride == 3'h0 ? io_in_bits_size : 20'h0; // @[StrideHandler.scala 41:32 StrideHandler.scala 48:22 StrideHandler.scala 59:22]
  assign handler_clock = clock;
  assign handler_reset = reset;
  assign handler_io_in_valid = io_in_bits_stride == 3'h0 ? 1'h0 : io_in_valid; // @[StrideHandler.scala 41:32 StrideHandler.scala 37:23 StrideHandler.scala 52:19]
  assign handler_io_in_bits_write = io_in_bits_stride == 3'h0 ? 1'h0 : io_in_bits_write; // @[StrideHandler.scala 41:32 StrideHandler.scala 38:22 StrideHandler.scala 52:19]
  assign handler_io_in_bits_address = io_in_bits_stride == 3'h0 ? 20'h0 : io_in_bits_address; // @[StrideHandler.scala 41:32 StrideHandler.scala 38:22 StrideHandler.scala 52:19]
  assign handler_io_in_bits_size = io_in_bits_stride == 3'h0 ? 20'h0 : io_in_bits_size; // @[StrideHandler.scala 41:32 StrideHandler.scala 38:22 StrideHandler.scala 52:19]
  assign handler_io_in_bits_stride = io_in_bits_stride == 3'h0 ? 3'h0 : io_in_bits_stride; // @[StrideHandler.scala 41:32 StrideHandler.scala 38:22 StrideHandler.scala 52:19]
  assign handler_io_out_ready = io_in_bits_stride == 3'h0 ? 1'h0 : io_out_ready; // @[StrideHandler.scala 41:32 StrideHandler.scala 39:24 StrideHandler.scala 60:26]
endmodule
module Counter_2(
  input        clock,
  input        reset,
  input        io_value_ready,
  output [5:0] io_value_bits,
  input        io_resetValue
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
`endif // RANDOMIZE_REG_INIT
  reg [5:0] value; // @[Counter.scala 16:22]
  wire [5:0] _value_T_1 = value + 6'h1; // @[Counter.scala 24:22]
  assign io_value_bits = value; // @[Counter.scala 18:17]
  always @(posedge clock) begin
    if (reset) begin // @[Counter.scala 16:22]
      value <= 6'h0; // @[Counter.scala 16:22]
    end else if (io_resetValue) begin // @[Counter.scala 27:23]
      value <= 6'h0; // @[Counter.scala 28:11]
    end else if (io_value_ready) begin // @[Counter.scala 20:24]
      if (value == 6'h3f) begin // @[Counter.scala 21:31]
        value <= 6'h0; // @[Counter.scala 22:13]
      end else begin
        value <= _value_T_1; // @[Counter.scala 24:13]
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
  value = _RAND_0[5:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module CountBy_2(
  input        clock,
  input        reset,
  input        io_value_ready,
  output [5:0] io_value_bits,
  input  [5:0] io_step,
  input        io_resetValue
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
`endif // RANDOMIZE_REG_INIT
  reg [5:0] value; // @[CountBy.scala 17:22]
  wire [6:0] _GEN_3 = {{1'd0}, io_step}; // @[CountBy.scala 22:24]
  wire [6:0] _T_1 = 7'h40 - _GEN_3; // @[CountBy.scala 22:24]
  wire [6:0] _GEN_4 = {{1'd0}, value}; // @[CountBy.scala 22:16]
  wire [5:0] _value_T_1 = value + io_step; // @[CountBy.scala 25:22]
  assign io_value_bits = value; // @[CountBy.scala 19:17]
  always @(posedge clock) begin
    if (reset) begin // @[CountBy.scala 17:22]
      value <= 6'h0; // @[CountBy.scala 17:22]
    end else if (io_resetValue) begin // @[CountBy.scala 28:23]
      value <= 6'h0; // @[CountBy.scala 29:11]
    end else if (io_value_ready) begin // @[CountBy.scala 21:24]
      if (_GEN_4 >= _T_1) begin // @[CountBy.scala 22:36]
        value <= 6'h0; // @[CountBy.scala 23:13]
      end else begin
        value <= _value_T_1; // @[CountBy.scala 25:13]
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
  value = _RAND_0[5:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module SizeAndStrideHandler_2(
  input        clock,
  input        reset,
  output       io_in_ready,
  input        io_in_valid,
  input        io_in_bits_write,
  input  [5:0] io_in_bits_address,
  input  [5:0] io_in_bits_size,
  input  [2:0] io_in_bits_stride,
  input        io_in_bits_reverse,
  input        io_out_ready,
  output       io_out_valid,
  output       io_out_bits_write,
  output [5:0] io_out_bits_address
);
  wire  sizeCounter_clock; // @[Counter.scala 34:19]
  wire  sizeCounter_reset; // @[Counter.scala 34:19]
  wire  sizeCounter_io_value_ready; // @[Counter.scala 34:19]
  wire [5:0] sizeCounter_io_value_bits; // @[Counter.scala 34:19]
  wire  sizeCounter_io_resetValue; // @[Counter.scala 34:19]
  wire  addressCounter_clock; // @[CountBy.scala 35:19]
  wire  addressCounter_reset; // @[CountBy.scala 35:19]
  wire  addressCounter_io_value_ready; // @[CountBy.scala 35:19]
  wire [5:0] addressCounter_io_value_bits; // @[CountBy.scala 35:19]
  wire [5:0] addressCounter_io_step; // @[CountBy.scala 35:19]
  wire  addressCounter_io_resetValue; // @[CountBy.scala 35:19]
  wire [7:0] stride = 8'h1 << io_in_bits_stride; // @[SizeAndStrideHandler.scala 30:20]
  wire [5:0] _io_out_bits_address_T_1 = io_in_bits_address - addressCounter_io_value_bits; // @[SizeAndStrideHandler.scala 46:44]
  wire [5:0] _io_out_bits_address_T_3 = io_in_bits_address + addressCounter_io_value_bits; // @[SizeAndStrideHandler.scala 48:44]
  wire  fire = io_in_valid & io_out_ready; // @[SizeAndStrideHandler.scala 51:23]
  Counter_2 sizeCounter ( // @[Counter.scala 34:19]
    .clock(sizeCounter_clock),
    .reset(sizeCounter_reset),
    .io_value_ready(sizeCounter_io_value_ready),
    .io_value_bits(sizeCounter_io_value_bits),
    .io_resetValue(sizeCounter_io_resetValue)
  );
  CountBy_2 addressCounter ( // @[CountBy.scala 35:19]
    .clock(addressCounter_clock),
    .reset(addressCounter_reset),
    .io_value_ready(addressCounter_io_value_ready),
    .io_value_bits(addressCounter_io_value_bits),
    .io_step(addressCounter_io_step),
    .io_resetValue(addressCounter_io_resetValue)
  );
  assign io_in_ready = sizeCounter_io_value_bits == io_in_bits_size & io_out_ready; // @[SizeAndStrideHandler.scala 53:52 SizeAndStrideHandler.scala 54:14 SizeAndStrideHandler.scala 58:14]
  assign io_out_valid = io_in_valid; // @[SizeAndStrideHandler.scala 35:16]
  assign io_out_bits_write = io_in_bits_write; // @[SizeAndStrideHandler.scala 38:34]
  assign io_out_bits_address = io_in_bits_reverse ? _io_out_bits_address_T_1 : _io_out_bits_address_T_3; // @[SizeAndStrideHandler.scala 45:25 SizeAndStrideHandler.scala 46:25 SizeAndStrideHandler.scala 48:25]
  assign sizeCounter_clock = clock;
  assign sizeCounter_reset = reset;
  assign sizeCounter_io_value_ready = sizeCounter_io_value_bits == io_in_bits_size ? 1'h0 : fire; // @[SizeAndStrideHandler.scala 53:52 Counter.scala 36:22 SizeAndStrideHandler.scala 59:32]
  assign sizeCounter_io_resetValue = sizeCounter_io_value_bits == io_in_bits_size & fire; // @[SizeAndStrideHandler.scala 53:52 SizeAndStrideHandler.scala 55:31 Counter.scala 35:21]
  assign addressCounter_clock = clock;
  assign addressCounter_reset = reset;
  assign addressCounter_io_value_ready = sizeCounter_io_value_bits == io_in_bits_size ? 1'h0 : fire; // @[SizeAndStrideHandler.scala 53:52 Counter.scala 36:22 SizeAndStrideHandler.scala 59:32]
  assign addressCounter_io_step = stride[5:0]; // @[CountBy.scala 36:15]
  assign addressCounter_io_resetValue = sizeCounter_io_value_bits == io_in_bits_size & fire; // @[SizeAndStrideHandler.scala 53:52 SizeAndStrideHandler.scala 55:31 Counter.scala 35:21]
endmodule
module Queue_2(
  input        clock,
  input        reset,
  output       io_enq_ready,
  input        io_enq_valid,
  input        io_enq_bits_write,
  input  [5:0] io_enq_bits_address,
  input  [5:0] io_enq_bits_size,
  input  [2:0] io_enq_bits_stride,
  input        io_enq_bits_reverse,
  input        io_deq_ready,
  output       io_deq_valid,
  output       io_deq_bits_write,
  output [5:0] io_deq_bits_address,
  output [5:0] io_deq_bits_size,
  output [2:0] io_deq_bits_stride,
  output       io_deq_bits_reverse
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_6;
  reg [31:0] _RAND_7;
`endif // RANDOMIZE_REG_INIT
  reg  ram_write [0:1]; // @[Decoupled.scala 218:16]
  wire  ram_write_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_write_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_write_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_write_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_write_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_write_MPORT_en; // @[Decoupled.scala 218:16]
  reg [5:0] ram_address [0:1]; // @[Decoupled.scala 218:16]
  wire [5:0] ram_address_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_address_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [5:0] ram_address_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_address_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_address_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_address_MPORT_en; // @[Decoupled.scala 218:16]
  reg [5:0] ram_size [0:1]; // @[Decoupled.scala 218:16]
  wire [5:0] ram_size_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_size_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [5:0] ram_size_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_size_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_size_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_size_MPORT_en; // @[Decoupled.scala 218:16]
  reg [2:0] ram_stride [0:1]; // @[Decoupled.scala 218:16]
  wire [2:0] ram_stride_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_stride_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [2:0] ram_stride_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_stride_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_stride_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_stride_MPORT_en; // @[Decoupled.scala 218:16]
  reg  ram_reverse [0:1]; // @[Decoupled.scala 218:16]
  wire  ram_reverse_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_reverse_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_reverse_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_reverse_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_reverse_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_reverse_MPORT_en; // @[Decoupled.scala 218:16]
  reg  enq_ptr_value; // @[Counter.scala 60:40]
  reg  deq_ptr_value; // @[Counter.scala 60:40]
  reg  maybe_full; // @[Decoupled.scala 221:27]
  wire  ptr_match = enq_ptr_value == deq_ptr_value; // @[Decoupled.scala 223:33]
  wire  empty = ptr_match & ~maybe_full; // @[Decoupled.scala 224:25]
  wire  full = ptr_match & maybe_full; // @[Decoupled.scala 225:24]
  wire  do_enq = io_enq_ready & io_enq_valid; // @[Decoupled.scala 40:37]
  wire  do_deq = io_deq_ready & io_deq_valid; // @[Decoupled.scala 40:37]
  assign ram_write_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_write_io_deq_bits_MPORT_data = ram_write[ram_write_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_write_MPORT_data = io_enq_bits_write;
  assign ram_write_MPORT_addr = enq_ptr_value;
  assign ram_write_MPORT_mask = 1'h1;
  assign ram_write_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_address_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_address_io_deq_bits_MPORT_data = ram_address[ram_address_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_address_MPORT_data = io_enq_bits_address;
  assign ram_address_MPORT_addr = enq_ptr_value;
  assign ram_address_MPORT_mask = 1'h1;
  assign ram_address_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_size_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_size_io_deq_bits_MPORT_data = ram_size[ram_size_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_size_MPORT_data = io_enq_bits_size;
  assign ram_size_MPORT_addr = enq_ptr_value;
  assign ram_size_MPORT_mask = 1'h1;
  assign ram_size_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_stride_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_stride_io_deq_bits_MPORT_data = ram_stride[ram_stride_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_stride_MPORT_data = io_enq_bits_stride;
  assign ram_stride_MPORT_addr = enq_ptr_value;
  assign ram_stride_MPORT_mask = 1'h1;
  assign ram_stride_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_reverse_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_reverse_io_deq_bits_MPORT_data = ram_reverse[ram_reverse_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_reverse_MPORT_data = io_enq_bits_reverse;
  assign ram_reverse_MPORT_addr = enq_ptr_value;
  assign ram_reverse_MPORT_mask = 1'h1;
  assign ram_reverse_MPORT_en = io_enq_ready & io_enq_valid;
  assign io_enq_ready = ~full; // @[Decoupled.scala 241:19]
  assign io_deq_valid = ~empty; // @[Decoupled.scala 240:19]
  assign io_deq_bits_write = ram_write_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_address = ram_address_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_size = ram_size_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_stride = ram_stride_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_reverse = ram_reverse_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  always @(posedge clock) begin
    if(ram_write_MPORT_en & ram_write_MPORT_mask) begin
      ram_write[ram_write_MPORT_addr] <= ram_write_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_address_MPORT_en & ram_address_MPORT_mask) begin
      ram_address[ram_address_MPORT_addr] <= ram_address_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_size_MPORT_en & ram_size_MPORT_mask) begin
      ram_size[ram_size_MPORT_addr] <= ram_size_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_stride_MPORT_en & ram_stride_MPORT_mask) begin
      ram_stride[ram_stride_MPORT_addr] <= ram_stride_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_reverse_MPORT_en & ram_reverse_MPORT_mask) begin
      ram_reverse[ram_reverse_MPORT_addr] <= ram_reverse_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if (reset) begin // @[Counter.scala 60:40]
      enq_ptr_value <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_enq) begin // @[Decoupled.scala 229:17]
      enq_ptr_value <= enq_ptr_value + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Counter.scala 60:40]
      deq_ptr_value <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_deq) begin // @[Decoupled.scala 233:17]
      deq_ptr_value <= deq_ptr_value + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Decoupled.scala 221:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 221:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 236:28]
      maybe_full <= do_enq; // @[Decoupled.scala 237:16]
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
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_write[initvar] = _RAND_0[0:0];
  _RAND_1 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_address[initvar] = _RAND_1[5:0];
  _RAND_2 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_size[initvar] = _RAND_2[5:0];
  _RAND_3 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_stride[initvar] = _RAND_3[2:0];
  _RAND_4 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_reverse[initvar] = _RAND_4[0:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_5 = {1{`RANDOM}};
  enq_ptr_value = _RAND_5[0:0];
  _RAND_6 = {1{`RANDOM}};
  deq_ptr_value = _RAND_6[0:0];
  _RAND_7 = {1{`RANDOM}};
  maybe_full = _RAND_7[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module SizeAndStrideHandler_4(
  input        clock,
  input        reset,
  output       io_in_ready,
  input        io_in_valid,
  input  [3:0] io_in_bits_instruction_op,
  input        io_in_bits_instruction_sourceLeft,
  input        io_in_bits_instruction_sourceRight,
  input        io_in_bits_instruction_dest,
  input  [5:0] io_in_bits_address,
  input  [5:0] io_in_bits_altAddress,
  input        io_in_bits_read,
  input        io_in_bits_write,
  input        io_in_bits_accumulate,
  input  [5:0] io_in_bits_size,
  input  [2:0] io_in_bits_stride,
  input        io_out_ready,
  output       io_out_valid,
  output [3:0] io_out_bits_instruction_op,
  output       io_out_bits_instruction_sourceLeft,
  output       io_out_bits_instruction_sourceRight,
  output       io_out_bits_instruction_dest,
  output [5:0] io_out_bits_address,
  output [5:0] io_out_bits_altAddress,
  output       io_out_bits_read,
  output       io_out_bits_write,
  output       io_out_bits_accumulate
);
  wire  sizeCounter_clock; // @[Counter.scala 34:19]
  wire  sizeCounter_reset; // @[Counter.scala 34:19]
  wire  sizeCounter_io_value_ready; // @[Counter.scala 34:19]
  wire [5:0] sizeCounter_io_value_bits; // @[Counter.scala 34:19]
  wire  sizeCounter_io_resetValue; // @[Counter.scala 34:19]
  wire  addressCounter_clock; // @[CountBy.scala 35:19]
  wire  addressCounter_reset; // @[CountBy.scala 35:19]
  wire  addressCounter_io_value_ready; // @[CountBy.scala 35:19]
  wire [5:0] addressCounter_io_value_bits; // @[CountBy.scala 35:19]
  wire [5:0] addressCounter_io_step; // @[CountBy.scala 35:19]
  wire  addressCounter_io_resetValue; // @[CountBy.scala 35:19]
  wire [7:0] stride = 8'h1 << io_in_bits_stride; // @[SizeAndStrideHandler.scala 30:20]
  wire  fire = io_in_valid & io_out_ready; // @[SizeAndStrideHandler.scala 51:23]
  Counter_2 sizeCounter ( // @[Counter.scala 34:19]
    .clock(sizeCounter_clock),
    .reset(sizeCounter_reset),
    .io_value_ready(sizeCounter_io_value_ready),
    .io_value_bits(sizeCounter_io_value_bits),
    .io_resetValue(sizeCounter_io_resetValue)
  );
  CountBy_2 addressCounter ( // @[CountBy.scala 35:19]
    .clock(addressCounter_clock),
    .reset(addressCounter_reset),
    .io_value_ready(addressCounter_io_value_ready),
    .io_value_bits(addressCounter_io_value_bits),
    .io_step(addressCounter_io_step),
    .io_resetValue(addressCounter_io_resetValue)
  );
  assign io_in_ready = sizeCounter_io_value_bits == io_in_bits_size & io_out_ready; // @[SizeAndStrideHandler.scala 53:52 SizeAndStrideHandler.scala 54:14 SizeAndStrideHandler.scala 58:14]
  assign io_out_valid = io_in_valid; // @[SizeAndStrideHandler.scala 35:16]
  assign io_out_bits_instruction_op = io_in_bits_instruction_op; // @[SizeAndStrideHandler.scala 38:34]
  assign io_out_bits_instruction_sourceLeft = io_in_bits_instruction_sourceLeft; // @[SizeAndStrideHandler.scala 38:34]
  assign io_out_bits_instruction_sourceRight = io_in_bits_instruction_sourceRight; // @[SizeAndStrideHandler.scala 38:34]
  assign io_out_bits_instruction_dest = io_in_bits_instruction_dest; // @[SizeAndStrideHandler.scala 38:34]
  assign io_out_bits_address = io_in_bits_address + addressCounter_io_value_bits; // @[SizeAndStrideHandler.scala 48:44]
  assign io_out_bits_altAddress = io_in_bits_altAddress; // @[SizeAndStrideHandler.scala 38:34]
  assign io_out_bits_read = io_in_bits_read; // @[SizeAndStrideHandler.scala 38:34]
  assign io_out_bits_write = io_in_bits_write; // @[SizeAndStrideHandler.scala 38:34]
  assign io_out_bits_accumulate = io_in_bits_accumulate; // @[SizeAndStrideHandler.scala 38:34]
  assign sizeCounter_clock = clock;
  assign sizeCounter_reset = reset;
  assign sizeCounter_io_value_ready = sizeCounter_io_value_bits == io_in_bits_size ? 1'h0 : fire; // @[SizeAndStrideHandler.scala 53:52 Counter.scala 36:22 SizeAndStrideHandler.scala 59:32]
  assign sizeCounter_io_resetValue = sizeCounter_io_value_bits == io_in_bits_size & fire; // @[SizeAndStrideHandler.scala 53:52 SizeAndStrideHandler.scala 55:31 Counter.scala 35:21]
  assign addressCounter_clock = clock;
  assign addressCounter_reset = reset;
  assign addressCounter_io_value_ready = sizeCounter_io_value_bits == io_in_bits_size ? 1'h0 : fire; // @[SizeAndStrideHandler.scala 53:52 Counter.scala 36:22 SizeAndStrideHandler.scala 59:32]
  assign addressCounter_io_step = stride[5:0]; // @[CountBy.scala 36:15]
  assign addressCounter_io_resetValue = sizeCounter_io_value_bits == io_in_bits_size & fire; // @[SizeAndStrideHandler.scala 53:52 SizeAndStrideHandler.scala 55:31 Counter.scala 35:21]
endmodule
module SizeHandler(
  input        clock,
  input        reset,
  output       io_in_ready,
  input        io_in_valid,
  input        io_in_bits_load,
  input        io_in_bits_zeroes,
  input  [5:0] io_in_bits_size,
  input        io_out_ready,
  output       io_out_valid,
  output       io_out_bits_load,
  output       io_out_bits_zeroes
);
  wire  sizeCounter_clock; // @[Counter.scala 34:19]
  wire  sizeCounter_reset; // @[Counter.scala 34:19]
  wire  sizeCounter_io_value_ready; // @[Counter.scala 34:19]
  wire [5:0] sizeCounter_io_value_bits; // @[Counter.scala 34:19]
  wire  sizeCounter_io_resetValue; // @[Counter.scala 34:19]
  wire  fire = io_in_valid & io_out_ready; // @[SizeHandler.scala 32:23]
  Counter_2 sizeCounter ( // @[Counter.scala 34:19]
    .clock(sizeCounter_clock),
    .reset(sizeCounter_reset),
    .io_value_ready(sizeCounter_io_value_ready),
    .io_value_bits(sizeCounter_io_value_bits),
    .io_resetValue(sizeCounter_io_resetValue)
  );
  assign io_in_ready = sizeCounter_io_value_bits == io_in_bits_size & io_out_ready; // @[SizeHandler.scala 34:52 SizeHandler.scala 35:14 SizeHandler.scala 38:14]
  assign io_out_valid = io_in_valid; // @[SizeHandler.scala 25:16]
  assign io_out_bits_load = io_in_bits_load; // @[SizeHandler.scala 28:34]
  assign io_out_bits_zeroes = io_in_bits_zeroes; // @[SizeHandler.scala 28:34]
  assign sizeCounter_clock = clock;
  assign sizeCounter_reset = reset;
  assign sizeCounter_io_value_ready = sizeCounter_io_value_bits == io_in_bits_size ? 1'h0 : fire; // @[SizeHandler.scala 34:52 Counter.scala 36:22 SizeHandler.scala 39:32]
  assign sizeCounter_io_resetValue = sizeCounter_io_value_bits == io_in_bits_size & fire; // @[SizeHandler.scala 34:52 SizeHandler.scala 36:31 Counter.scala 35:21]
endmodule
module SizeHandler_1(
  input        clock,
  input        reset,
  output       io_in_ready,
  input        io_in_valid,
  input  [1:0] io_in_bits_kind,
  input  [5:0] io_in_bits_size,
  input        io_out_ready,
  output       io_out_valid,
  output [1:0] io_out_bits_kind
);
  wire  sizeCounter_clock; // @[Counter.scala 34:19]
  wire  sizeCounter_reset; // @[Counter.scala 34:19]
  wire  sizeCounter_io_value_ready; // @[Counter.scala 34:19]
  wire [5:0] sizeCounter_io_value_bits; // @[Counter.scala 34:19]
  wire  sizeCounter_io_resetValue; // @[Counter.scala 34:19]
  wire  fire = io_in_valid & io_out_ready; // @[SizeHandler.scala 32:23]
  Counter_2 sizeCounter ( // @[Counter.scala 34:19]
    .clock(sizeCounter_clock),
    .reset(sizeCounter_reset),
    .io_value_ready(sizeCounter_io_value_ready),
    .io_value_bits(sizeCounter_io_value_bits),
    .io_resetValue(sizeCounter_io_resetValue)
  );
  assign io_in_ready = sizeCounter_io_value_bits == io_in_bits_size & io_out_ready; // @[SizeHandler.scala 34:52 SizeHandler.scala 35:14 SizeHandler.scala 38:14]
  assign io_out_valid = io_in_valid; // @[SizeHandler.scala 25:16]
  assign io_out_bits_kind = io_in_bits_kind; // @[SizeHandler.scala 28:34]
  assign sizeCounter_clock = clock;
  assign sizeCounter_reset = reset;
  assign sizeCounter_io_value_ready = sizeCounter_io_value_bits == io_in_bits_size ? 1'h0 : fire; // @[SizeHandler.scala 34:52 Counter.scala 36:22 SizeHandler.scala 39:32]
  assign sizeCounter_io_resetValue = sizeCounter_io_value_bits == io_in_bits_size & fire; // @[SizeHandler.scala 34:52 SizeHandler.scala 36:31 Counter.scala 35:21]
endmodule
module MultiEnqueue(
  input   clock,
  input   reset,
  output  io_in_ready,
  input   io_in_valid,
  input   io_out_0_ready,
  output  io_out_0_valid
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
`endif // RANDOMIZE_REG_INIT
  reg  enq_0; // @[MultiEnqueue.scala 16:47]
  wire  allEnqueued = io_out_0_ready | enq_0; // @[MultiEnqueue.scala 22:34]
  wire  _io_out_0_valid_T = ~enq_0; // @[MultiEnqueue.scala 27:39]
  assign io_in_ready = io_out_0_ready | enq_0; // @[MultiEnqueue.scala 22:34]
  assign io_out_0_valid = io_in_valid & ~enq_0; // @[MultiEnqueue.scala 27:36]
  always @(posedge clock) begin
    if (reset) begin // @[MultiEnqueue.scala 16:47]
      enq_0 <= 1'h0; // @[MultiEnqueue.scala 16:47]
    end else if (allEnqueued) begin // @[MultiEnqueue.scala 28:23]
      enq_0 <= 1'h0; // @[MultiEnqueue.scala 29:14]
    end else if (_io_out_0_valid_T) begin // @[MultiEnqueue.scala 31:21]
      enq_0 <= io_out_0_valid & io_out_0_ready; // @[MultiEnqueue.scala 32:16]
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
  enq_0 = _RAND_0[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module MultiEnqueue_1(
  input   clock,
  input   reset,
  output  io_in_ready,
  input   io_in_valid,
  input   io_out_0_ready,
  output  io_out_0_valid,
  input   io_out_1_ready,
  output  io_out_1_valid
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
`endif // RANDOMIZE_REG_INIT
  reg  enq_0; // @[MultiEnqueue.scala 16:47]
  reg  enq_1; // @[MultiEnqueue.scala 16:47]
  wire  _allEnqueued_T = io_out_0_ready | enq_0; // @[MultiEnqueue.scala 22:34]
  wire  _allEnqueued_T_1 = io_out_1_ready | enq_1; // @[MultiEnqueue.scala 22:34]
  wire  allEnqueued = _allEnqueued_T & _allEnqueued_T_1; // @[MultiEnqueue.scala 24:15]
  wire  _io_out_0_valid_T = ~enq_0; // @[MultiEnqueue.scala 27:39]
  wire  _io_out_1_valid_T = ~enq_1; // @[MultiEnqueue.scala 27:39]
  assign io_in_ready = _allEnqueued_T & _allEnqueued_T_1; // @[MultiEnqueue.scala 24:15]
  assign io_out_0_valid = io_in_valid & ~enq_0; // @[MultiEnqueue.scala 27:36]
  assign io_out_1_valid = io_in_valid & ~enq_1; // @[MultiEnqueue.scala 27:36]
  always @(posedge clock) begin
    if (reset) begin // @[MultiEnqueue.scala 16:47]
      enq_0 <= 1'h0; // @[MultiEnqueue.scala 16:47]
    end else if (allEnqueued) begin // @[MultiEnqueue.scala 28:23]
      enq_0 <= 1'h0; // @[MultiEnqueue.scala 29:14]
    end else if (_io_out_0_valid_T) begin // @[MultiEnqueue.scala 31:21]
      enq_0 <= io_out_0_valid & io_out_0_ready; // @[MultiEnqueue.scala 32:16]
    end
    if (reset) begin // @[MultiEnqueue.scala 16:47]
      enq_1 <= 1'h0; // @[MultiEnqueue.scala 16:47]
    end else if (allEnqueued) begin // @[MultiEnqueue.scala 28:23]
      enq_1 <= 1'h0; // @[MultiEnqueue.scala 29:14]
    end else if (_io_out_1_valid_T) begin // @[MultiEnqueue.scala 31:21]
      enq_1 <= io_out_1_valid & io_out_1_ready; // @[MultiEnqueue.scala 32:16]
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
  enq_0 = _RAND_0[0:0];
  _RAND_1 = {1{`RANDOM}};
  enq_1 = _RAND_1[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module MultiEnqueue_2(
  input   clock,
  input   reset,
  output  io_in_ready,
  input   io_in_valid,
  input   io_out_0_ready,
  output  io_out_0_valid,
  input   io_out_1_ready,
  output  io_out_1_valid,
  input   io_out_2_ready,
  output  io_out_2_valid
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
`endif // RANDOMIZE_REG_INIT
  reg  enq_0; // @[MultiEnqueue.scala 16:47]
  reg  enq_1; // @[MultiEnqueue.scala 16:47]
  reg  enq_2; // @[MultiEnqueue.scala 16:47]
  wire  _allEnqueued_T = io_out_0_ready | enq_0; // @[MultiEnqueue.scala 22:34]
  wire  _allEnqueued_T_1 = io_out_1_ready | enq_1; // @[MultiEnqueue.scala 22:34]
  wire  _allEnqueued_T_2 = io_out_2_ready | enq_2; // @[MultiEnqueue.scala 22:34]
  wire  allEnqueued = _allEnqueued_T & _allEnqueued_T_1 & _allEnqueued_T_2; // @[MultiEnqueue.scala 24:15]
  wire  _io_out_0_valid_T = ~enq_0; // @[MultiEnqueue.scala 27:39]
  wire  _io_out_1_valid_T = ~enq_1; // @[MultiEnqueue.scala 27:39]
  wire  _io_out_2_valid_T = ~enq_2; // @[MultiEnqueue.scala 27:39]
  assign io_in_ready = _allEnqueued_T & _allEnqueued_T_1 & _allEnqueued_T_2; // @[MultiEnqueue.scala 24:15]
  assign io_out_0_valid = io_in_valid & ~enq_0; // @[MultiEnqueue.scala 27:36]
  assign io_out_1_valid = io_in_valid & ~enq_1; // @[MultiEnqueue.scala 27:36]
  assign io_out_2_valid = io_in_valid & ~enq_2; // @[MultiEnqueue.scala 27:36]
  always @(posedge clock) begin
    if (reset) begin // @[MultiEnqueue.scala 16:47]
      enq_0 <= 1'h0; // @[MultiEnqueue.scala 16:47]
    end else if (allEnqueued) begin // @[MultiEnqueue.scala 28:23]
      enq_0 <= 1'h0; // @[MultiEnqueue.scala 29:14]
    end else if (_io_out_0_valid_T) begin // @[MultiEnqueue.scala 31:21]
      enq_0 <= io_out_0_valid & io_out_0_ready; // @[MultiEnqueue.scala 32:16]
    end
    if (reset) begin // @[MultiEnqueue.scala 16:47]
      enq_1 <= 1'h0; // @[MultiEnqueue.scala 16:47]
    end else if (allEnqueued) begin // @[MultiEnqueue.scala 28:23]
      enq_1 <= 1'h0; // @[MultiEnqueue.scala 29:14]
    end else if (_io_out_1_valid_T) begin // @[MultiEnqueue.scala 31:21]
      enq_1 <= io_out_1_valid & io_out_1_ready; // @[MultiEnqueue.scala 32:16]
    end
    if (reset) begin // @[MultiEnqueue.scala 16:47]
      enq_2 <= 1'h0; // @[MultiEnqueue.scala 16:47]
    end else if (allEnqueued) begin // @[MultiEnqueue.scala 28:23]
      enq_2 <= 1'h0; // @[MultiEnqueue.scala 29:14]
    end else if (_io_out_2_valid_T) begin // @[MultiEnqueue.scala 31:21]
      enq_2 <= io_out_2_valid & io_out_2_ready; // @[MultiEnqueue.scala 32:16]
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
  enq_0 = _RAND_0[0:0];
  _RAND_1 = {1{`RANDOM}};
  enq_1 = _RAND_1[0:0];
  _RAND_2 = {1{`RANDOM}};
  enq_2 = _RAND_2[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module MultiEnqueue_3(
  input   clock,
  input   reset,
  output  io_in_ready,
  input   io_in_valid,
  input   io_out_0_ready,
  output  io_out_0_valid,
  input   io_out_1_ready,
  output  io_out_1_valid,
  input   io_out_2_ready,
  output  io_out_2_valid,
  input   io_out_3_ready,
  output  io_out_3_valid
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
`endif // RANDOMIZE_REG_INIT
  reg  enq_0; // @[MultiEnqueue.scala 16:47]
  reg  enq_1; // @[MultiEnqueue.scala 16:47]
  reg  enq_2; // @[MultiEnqueue.scala 16:47]
  reg  enq_3; // @[MultiEnqueue.scala 16:47]
  wire  _allEnqueued_T = io_out_0_ready | enq_0; // @[MultiEnqueue.scala 22:34]
  wire  _allEnqueued_T_1 = io_out_1_ready | enq_1; // @[MultiEnqueue.scala 22:34]
  wire  _allEnqueued_T_2 = io_out_2_ready | enq_2; // @[MultiEnqueue.scala 22:34]
  wire  _allEnqueued_T_3 = io_out_3_ready | enq_3; // @[MultiEnqueue.scala 22:34]
  wire  allEnqueued = _allEnqueued_T & _allEnqueued_T_1 & _allEnqueued_T_2 & _allEnqueued_T_3; // @[MultiEnqueue.scala 24:15]
  wire  _io_out_0_valid_T = ~enq_0; // @[MultiEnqueue.scala 27:39]
  wire  _io_out_1_valid_T = ~enq_1; // @[MultiEnqueue.scala 27:39]
  wire  _io_out_2_valid_T = ~enq_2; // @[MultiEnqueue.scala 27:39]
  wire  _io_out_3_valid_T = ~enq_3; // @[MultiEnqueue.scala 27:39]
  assign io_in_ready = _allEnqueued_T & _allEnqueued_T_1 & _allEnqueued_T_2 & _allEnqueued_T_3; // @[MultiEnqueue.scala 24:15]
  assign io_out_0_valid = io_in_valid & ~enq_0; // @[MultiEnqueue.scala 27:36]
  assign io_out_1_valid = io_in_valid & ~enq_1; // @[MultiEnqueue.scala 27:36]
  assign io_out_2_valid = io_in_valid & ~enq_2; // @[MultiEnqueue.scala 27:36]
  assign io_out_3_valid = io_in_valid & ~enq_3; // @[MultiEnqueue.scala 27:36]
  always @(posedge clock) begin
    if (reset) begin // @[MultiEnqueue.scala 16:47]
      enq_0 <= 1'h0; // @[MultiEnqueue.scala 16:47]
    end else if (allEnqueued) begin // @[MultiEnqueue.scala 28:23]
      enq_0 <= 1'h0; // @[MultiEnqueue.scala 29:14]
    end else if (_io_out_0_valid_T) begin // @[MultiEnqueue.scala 31:21]
      enq_0 <= io_out_0_valid & io_out_0_ready; // @[MultiEnqueue.scala 32:16]
    end
    if (reset) begin // @[MultiEnqueue.scala 16:47]
      enq_1 <= 1'h0; // @[MultiEnqueue.scala 16:47]
    end else if (allEnqueued) begin // @[MultiEnqueue.scala 28:23]
      enq_1 <= 1'h0; // @[MultiEnqueue.scala 29:14]
    end else if (_io_out_1_valid_T) begin // @[MultiEnqueue.scala 31:21]
      enq_1 <= io_out_1_valid & io_out_1_ready; // @[MultiEnqueue.scala 32:16]
    end
    if (reset) begin // @[MultiEnqueue.scala 16:47]
      enq_2 <= 1'h0; // @[MultiEnqueue.scala 16:47]
    end else if (allEnqueued) begin // @[MultiEnqueue.scala 28:23]
      enq_2 <= 1'h0; // @[MultiEnqueue.scala 29:14]
    end else if (_io_out_2_valid_T) begin // @[MultiEnqueue.scala 31:21]
      enq_2 <= io_out_2_valid & io_out_2_ready; // @[MultiEnqueue.scala 32:16]
    end
    if (reset) begin // @[MultiEnqueue.scala 16:47]
      enq_3 <= 1'h0; // @[MultiEnqueue.scala 16:47]
    end else if (allEnqueued) begin // @[MultiEnqueue.scala 28:23]
      enq_3 <= 1'h0; // @[MultiEnqueue.scala 29:14]
    end else if (_io_out_3_valid_T) begin // @[MultiEnqueue.scala 31:21]
      enq_3 <= io_out_3_valid & io_out_3_ready; // @[MultiEnqueue.scala 32:16]
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
  enq_0 = _RAND_0[0:0];
  _RAND_1 = {1{`RANDOM}};
  enq_1 = _RAND_1[0:0];
  _RAND_2 = {1{`RANDOM}};
  enq_2 = _RAND_2[0:0];
  _RAND_3 = {1{`RANDOM}};
  enq_3 = _RAND_3[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Validator(
  input         io_instruction_valid,
  input  [3:0]  io_instruction_bits_opcode,
  input  [3:0]  io_instruction_bits_flags,
  input  [47:0] io_instruction_bits_arguments,
  output        io_error
);
  wire  flags_accumulate = io_instruction_bits_flags[0]; // @[Validator.scala 38:47]
  wire [1:0] flags__unused = io_instruction_bits_flags[3:2]; // @[Validator.scala 38:47]
  wire [5:0] args_memAddress = io_instruction_bits_arguments[5:0]; // @[Validator.scala 39:50]
  wire [19:0] args_accAddress = io_instruction_bits_arguments[35:16]; // @[Validator.scala 39:50]
  wire [7:0] args_size = io_instruction_bits_arguments[47:40]; // @[Validator.scala 39:50]
  wire  _T_1 = args_size >= 8'h40; // @[Validator.scala 41:22]
  wire  _T_2 = args_accAddress >= 20'h40; // @[Validator.scala 43:34]
  wire [19:0] _GEN_36 = {{12'd0}, args_size}; // @[Validator.scala 48:25]
  wire [19:0] _T_5 = args_accAddress + _GEN_36; // @[Validator.scala 48:25]
  wire  _T_6 = _T_5 >= 20'h40; // @[Validator.scala 48:37]
  wire [7:0] _GEN_37 = {{2'd0}, args_memAddress}; // @[Validator.scala 51:34]
  wire [7:0] _T_8 = _GEN_37 + args_size; // @[Validator.scala 51:34]
  wire  _T_9 = _T_8 >= 8'h40; // @[Validator.scala 51:46]
  wire  _T_10 = flags__unused != 2'h0; // @[Validator.scala 53:32]
  wire  _GEN_1 = _T_8 >= 8'h40 | _T_10; // @[Validator.scala 51:75 Validator.scala 52:18]
  wire  _GEN_2 = _T_6 | _GEN_1; // @[Validator.scala 49:9 Validator.scala 50:18]
  wire  _GEN_4 = args_accAddress >= 20'h40 | _GEN_2; // @[Validator.scala 43:69 Validator.scala 44:18]
  wire  _GEN_5 = args_size >= 8'h40 | _GEN_4; // @[Validator.scala 41:57 Validator.scala 42:18]
  wire [2:0] flags_1__unused = io_instruction_bits_flags[3:1]; // @[Validator.scala 62:47]
  wire [5:0] args_1_address = io_instruction_bits_arguments[5:0]; // @[Validator.scala 63:50]
  wire [23:0] args_1_size = io_instruction_bits_arguments[39:16]; // @[Validator.scala 63:50]
  wire [23:0] _GEN_38 = {{18'd0}, args_1_address}; // @[Validator.scala 69:31]
  wire [23:0] _T_15 = _GEN_38 + args_1_size; // @[Validator.scala 69:31]
  wire  _T_17 = flags_1__unused != 3'h0; // @[Validator.scala 71:32]
  wire  _GEN_7 = _T_15 >= 24'h40 | _T_17; // @[Validator.scala 69:72 Validator.scala 70:18]
  wire  _GEN_9 = args_1_size > 24'h9 | _GEN_7; // @[Validator.scala 65:55 Validator.scala 66:18]
  wire  _T_20 = io_instruction_bits_flags == 4'h1; // @[Validator.scala 85:22]
  wire  _T_21 = io_instruction_bits_flags == 4'h0 | _T_20; // @[Validator.scala 84:51]
  wire  _T_22 = io_instruction_bits_flags == 4'h2; // @[Validator.scala 86:22]
  wire  _T_23 = _T_21 | _T_22; // @[Validator.scala 85:53]
  wire  _T_24 = io_instruction_bits_flags == 4'h3; // @[Validator.scala 87:22]
  wire  _T_25 = _T_23 | _T_24; // @[Validator.scala 86:53]
  wire  _GEN_12 = _T_1 | _T_9; // @[Validator.scala 89:53 Validator.scala 90:20]
  wire  _T_32 = io_instruction_bits_flags == 4'hd; // @[Validator.scala 100:22]
  wire  _T_33 = io_instruction_bits_flags == 4'hc | _T_32; // @[Validator.scala 99:57]
  wire  _T_34 = io_instruction_bits_flags == 4'hf; // @[Validator.scala 101:22]
  wire  _T_35 = _T_33 | _T_34; // @[Validator.scala 100:59]
  wire  _GEN_14 = _T_6 | _T_9; // @[Validator.scala 111:11 Validator.scala 112:20]
  wire  _GEN_16 = _T_2 | _GEN_14; // @[Validator.scala 105:71 Validator.scala 106:20]
  wire  _GEN_17 = _T_1 | _GEN_16; // @[Validator.scala 103:59 Validator.scala 104:20]
  wire  _GEN_18 = _T_35 ? _GEN_17 : 1'h1; // @[Validator.scala 102:9 Validator.scala 119:18]
  wire  _GEN_19 = _T_25 ? _GEN_12 : _GEN_18; // @[Validator.scala 88:9]
  wire  flags_3__unused = io_instruction_bits_flags[3]; // @[Validator.scala 126:47]
  wire [15:0] args_3_accWriteAddress = io_instruction_bits_arguments[15:0]; // @[Validator.scala 127:50]
  wire [23:0] args_3_accReadAddress = io_instruction_bits_arguments[39:16]; // @[Validator.scala 127:50]
  wire  args_3_instruction_sourceRight = io_instruction_bits_arguments[41]; // @[Validator.scala 127:50]
  wire  args_3_instruction_sourceLeft = io_instruction_bits_arguments[42]; // @[Validator.scala 127:50]
  wire  _T_55 = ~args_3_instruction_sourceLeft | ~args_3_instruction_sourceRight; // @[Validator.scala 148:61]
  wire  _T_57 = flags_accumulate & ~(~args_3_instruction_sourceLeft | ~args_3_instruction_sourceRight); // @[Validator.scala 148:20]
  wire  _T_62 = ~flags_accumulate & _T_55; // @[Validator.scala 154:21]
  wire  _GEN_21 = _T_57 | _T_62; // @[Validator.scala 149:9 Validator.scala 152:18]
  wire  _GEN_22 = flags_3__unused | _GEN_21; // @[Validator.scala 145:41 Validator.scala 146:18]
  wire  _GEN_27 = args_3_accWriteAddress >= 16'h40 | _GEN_22; // @[Validator.scala 131:74 Validator.scala 132:18]
  wire  _GEN_28 = args_3_accReadAddress >= 24'h40 | _GEN_27; // @[Validator.scala 129:67 Validator.scala 130:18]
  wire  _GEN_29 = io_instruction_bits_opcode == 4'h0 ? 1'h0 : 1'h1; // @[Validator.scala 165:57 Validator.scala 166:16 Validator.scala 168:16]
  wire  _GEN_30 = io_instruction_bits_opcode == 4'hf ? 1'h0 : _GEN_29; // @[Validator.scala 163:62 Validator.scala 164:16]
  wire  _GEN_31 = io_instruction_bits_opcode == 4'h4 ? _GEN_28 : _GEN_30; // @[Validator.scala 122:57]
  wire  _GEN_32 = io_instruction_bits_opcode == 4'h2 ? _GEN_19 : _GEN_31; // @[Validator.scala 76:61]
  wire  _GEN_33 = io_instruction_bits_opcode == 4'h3 ? _GEN_9 : _GEN_32; // @[Validator.scala 58:64]
  wire  _GEN_34 = io_instruction_bits_opcode == 4'h1 ? _GEN_5 : _GEN_33; // @[Validator.scala 34:53]
  assign io_error = io_instruction_valid & _GEN_34; // @[Validator.scala 33:27 Validator.scala 171:14]
endmodule
module Decoder(
  input         clock,
  input         reset,
  output        io_instruction_ready,
  input         io_instruction_valid,
  input  [3:0]  io_instruction_bits_opcode,
  input  [3:0]  io_instruction_bits_flags,
  input  [47:0] io_instruction_bits_arguments,
  input         io_memPortA_ready,
  output        io_memPortA_valid,
  output        io_memPortA_bits_write,
  output [5:0]  io_memPortA_bits_address,
  input         io_memPortB_ready,
  output        io_memPortB_valid,
  output        io_memPortB_bits_write,
  output [5:0]  io_memPortB_bits_address,
  input         io_dram0_ready,
  output        io_dram0_valid,
  output        io_dram0_bits_write,
  output [19:0] io_dram0_bits_address,
  output [19:0] io_dram0_bits_size,
  input         io_dram1_ready,
  output        io_dram1_valid,
  output        io_dram1_bits_write,
  output [19:0] io_dram1_bits_address,
  output [19:0] io_dram1_bits_size,
  input         io_dataflow_ready,
  output        io_dataflow_valid,
  output [3:0]  io_dataflow_bits_kind,
  output [5:0]  io_dataflow_bits_size,
  input         io_hostDataflow_ready,
  output        io_hostDataflow_valid,
  output [1:0]  io_hostDataflow_bits_kind,
  input         io_acc_ready,
  output        io_acc_valid,
  output [3:0]  io_acc_bits_instruction_op,
  output        io_acc_bits_instruction_sourceLeft,
  output        io_acc_bits_instruction_sourceRight,
  output        io_acc_bits_instruction_dest,
  output [5:0]  io_acc_bits_readAddress,
  output [5:0]  io_acc_bits_writeAddress,
  output        io_acc_bits_accumulate,
  output        io_acc_bits_write,
  output        io_acc_bits_read,
  input         io_array_ready,
  output        io_array_valid,
  output        io_array_bits_load,
  output        io_array_bits_zeroes,
  output [31:0] io_config_dram0AddressOffset,
  output [3:0]  io_config_dram0CacheBehaviour,
  output [31:0] io_config_dram1AddressOffset,
  output [3:0]  io_config_dram1CacheBehaviour,
  input         io_status_ready,
  output        io_status_valid,
  output        io_status_bits_last,
  output [3:0]  io_status_bits_bits_opcode,
  output [3:0]  io_status_bits_bits_flags,
  output [47:0] io_status_bits_bits_arguments,
  output        io_timeout,
  output        io_error,
  output        io_tracepoint,
  output [31:0] io_programCounter
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
`endif // RANDOMIZE_REG_INIT
  wire  instruction_clock; // @[Decoupled.scala 296:21]
  wire  instruction_reset; // @[Decoupled.scala 296:21]
  wire  instruction_io_enq_ready; // @[Decoupled.scala 296:21]
  wire  instruction_io_enq_valid; // @[Decoupled.scala 296:21]
  wire [3:0] instruction_io_enq_bits_opcode; // @[Decoupled.scala 296:21]
  wire [3:0] instruction_io_enq_bits_flags; // @[Decoupled.scala 296:21]
  wire [47:0] instruction_io_enq_bits_arguments; // @[Decoupled.scala 296:21]
  wire  instruction_io_deq_ready; // @[Decoupled.scala 296:21]
  wire  instruction_io_deq_valid; // @[Decoupled.scala 296:21]
  wire [3:0] instruction_io_deq_bits_opcode; // @[Decoupled.scala 296:21]
  wire [3:0] instruction_io_deq_bits_flags; // @[Decoupled.scala 296:21]
  wire [47:0] instruction_io_deq_bits_arguments; // @[Decoupled.scala 296:21]
  wire  status_clock; // @[Decoder.scala 78:23]
  wire  status_reset; // @[Decoder.scala 78:23]
  wire  status_io_enq_ready; // @[Decoder.scala 78:23]
  wire  status_io_enq_valid; // @[Decoder.scala 78:23]
  wire [3:0] status_io_enq_bits_bits_opcode; // @[Decoder.scala 78:23]
  wire [3:0] status_io_enq_bits_bits_flags; // @[Decoder.scala 78:23]
  wire [47:0] status_io_enq_bits_bits_arguments; // @[Decoder.scala 78:23]
  wire  status_io_deq_ready; // @[Decoder.scala 78:23]
  wire  status_io_deq_valid; // @[Decoder.scala 78:23]
  wire  status_io_deq_bits_last; // @[Decoder.scala 78:23]
  wire [3:0] status_io_deq_bits_bits_opcode; // @[Decoder.scala 78:23]
  wire [3:0] status_io_deq_bits_bits_flags; // @[Decoder.scala 78:23]
  wire [47:0] status_io_deq_bits_bits_arguments; // @[Decoder.scala 78:23]
  wire  dram0Handler_clock; // @[Decoder.scala 135:28]
  wire  dram0Handler_reset; // @[Decoder.scala 135:28]
  wire  dram0Handler_io_in_ready; // @[Decoder.scala 135:28]
  wire  dram0Handler_io_in_valid; // @[Decoder.scala 135:28]
  wire  dram0Handler_io_in_bits_write; // @[Decoder.scala 135:28]
  wire [19:0] dram0Handler_io_in_bits_address; // @[Decoder.scala 135:28]
  wire [19:0] dram0Handler_io_in_bits_size; // @[Decoder.scala 135:28]
  wire [2:0] dram0Handler_io_in_bits_stride; // @[Decoder.scala 135:28]
  wire  dram0Handler_io_out_ready; // @[Decoder.scala 135:28]
  wire  dram0Handler_io_out_valid; // @[Decoder.scala 135:28]
  wire  dram0Handler_io_out_bits_write; // @[Decoder.scala 135:28]
  wire [19:0] dram0Handler_io_out_bits_address; // @[Decoder.scala 135:28]
  wire [19:0] dram0Handler_io_out_bits_size; // @[Decoder.scala 135:28]
  wire  dram1Handler_clock; // @[Decoder.scala 144:28]
  wire  dram1Handler_reset; // @[Decoder.scala 144:28]
  wire  dram1Handler_io_in_ready; // @[Decoder.scala 144:28]
  wire  dram1Handler_io_in_valid; // @[Decoder.scala 144:28]
  wire  dram1Handler_io_in_bits_write; // @[Decoder.scala 144:28]
  wire [19:0] dram1Handler_io_in_bits_address; // @[Decoder.scala 144:28]
  wire [19:0] dram1Handler_io_in_bits_size; // @[Decoder.scala 144:28]
  wire [2:0] dram1Handler_io_in_bits_stride; // @[Decoder.scala 144:28]
  wire  dram1Handler_io_out_ready; // @[Decoder.scala 144:28]
  wire  dram1Handler_io_out_valid; // @[Decoder.scala 144:28]
  wire  dram1Handler_io_out_bits_write; // @[Decoder.scala 144:28]
  wire [19:0] dram1Handler_io_out_bits_address; // @[Decoder.scala 144:28]
  wire [19:0] dram1Handler_io_out_bits_size; // @[Decoder.scala 144:28]
  wire  memPortAHandler_clock; // @[Decoder.scala 160:31]
  wire  memPortAHandler_reset; // @[Decoder.scala 160:31]
  wire  memPortAHandler_io_in_ready; // @[Decoder.scala 160:31]
  wire  memPortAHandler_io_in_valid; // @[Decoder.scala 160:31]
  wire  memPortAHandler_io_in_bits_write; // @[Decoder.scala 160:31]
  wire [5:0] memPortAHandler_io_in_bits_address; // @[Decoder.scala 160:31]
  wire [5:0] memPortAHandler_io_in_bits_size; // @[Decoder.scala 160:31]
  wire [2:0] memPortAHandler_io_in_bits_stride; // @[Decoder.scala 160:31]
  wire  memPortAHandler_io_in_bits_reverse; // @[Decoder.scala 160:31]
  wire  memPortAHandler_io_out_ready; // @[Decoder.scala 160:31]
  wire  memPortAHandler_io_out_valid; // @[Decoder.scala 160:31]
  wire  memPortAHandler_io_out_bits_write; // @[Decoder.scala 160:31]
  wire [5:0] memPortAHandler_io_out_bits_address; // @[Decoder.scala 160:31]
  wire  memPortBHandler_clock; // @[Decoder.scala 169:31]
  wire  memPortBHandler_reset; // @[Decoder.scala 169:31]
  wire  memPortBHandler_io_in_ready; // @[Decoder.scala 169:31]
  wire  memPortBHandler_io_in_valid; // @[Decoder.scala 169:31]
  wire  memPortBHandler_io_in_bits_write; // @[Decoder.scala 169:31]
  wire [5:0] memPortBHandler_io_in_bits_address; // @[Decoder.scala 169:31]
  wire [5:0] memPortBHandler_io_in_bits_size; // @[Decoder.scala 169:31]
  wire [2:0] memPortBHandler_io_in_bits_stride; // @[Decoder.scala 169:31]
  wire  memPortBHandler_io_in_bits_reverse; // @[Decoder.scala 169:31]
  wire  memPortBHandler_io_out_ready; // @[Decoder.scala 169:31]
  wire  memPortBHandler_io_out_valid; // @[Decoder.scala 169:31]
  wire  memPortBHandler_io_out_bits_write; // @[Decoder.scala 169:31]
  wire [5:0] memPortBHandler_io_out_bits_address; // @[Decoder.scala 169:31]
  wire  memPortA_clock; // @[Mem.scala 23:19]
  wire  memPortA_reset; // @[Mem.scala 23:19]
  wire  memPortA_io_enq_ready; // @[Mem.scala 23:19]
  wire  memPortA_io_enq_valid; // @[Mem.scala 23:19]
  wire  memPortA_io_enq_bits_write; // @[Mem.scala 23:19]
  wire [5:0] memPortA_io_enq_bits_address; // @[Mem.scala 23:19]
  wire [5:0] memPortA_io_enq_bits_size; // @[Mem.scala 23:19]
  wire [2:0] memPortA_io_enq_bits_stride; // @[Mem.scala 23:19]
  wire  memPortA_io_enq_bits_reverse; // @[Mem.scala 23:19]
  wire  memPortA_io_deq_ready; // @[Mem.scala 23:19]
  wire  memPortA_io_deq_valid; // @[Mem.scala 23:19]
  wire  memPortA_io_deq_bits_write; // @[Mem.scala 23:19]
  wire [5:0] memPortA_io_deq_bits_address; // @[Mem.scala 23:19]
  wire [5:0] memPortA_io_deq_bits_size; // @[Mem.scala 23:19]
  wire [2:0] memPortA_io_deq_bits_stride; // @[Mem.scala 23:19]
  wire  memPortA_io_deq_bits_reverse; // @[Mem.scala 23:19]
  wire  memPortB_clock; // @[Mem.scala 23:19]
  wire  memPortB_reset; // @[Mem.scala 23:19]
  wire  memPortB_io_enq_ready; // @[Mem.scala 23:19]
  wire  memPortB_io_enq_valid; // @[Mem.scala 23:19]
  wire  memPortB_io_enq_bits_write; // @[Mem.scala 23:19]
  wire [5:0] memPortB_io_enq_bits_address; // @[Mem.scala 23:19]
  wire [5:0] memPortB_io_enq_bits_size; // @[Mem.scala 23:19]
  wire [2:0] memPortB_io_enq_bits_stride; // @[Mem.scala 23:19]
  wire  memPortB_io_enq_bits_reverse; // @[Mem.scala 23:19]
  wire  memPortB_io_deq_ready; // @[Mem.scala 23:19]
  wire  memPortB_io_deq_valid; // @[Mem.scala 23:19]
  wire  memPortB_io_deq_bits_write; // @[Mem.scala 23:19]
  wire [5:0] memPortB_io_deq_bits_address; // @[Mem.scala 23:19]
  wire [5:0] memPortB_io_deq_bits_size; // @[Mem.scala 23:19]
  wire [2:0] memPortB_io_deq_bits_stride; // @[Mem.scala 23:19]
  wire  memPortB_io_deq_bits_reverse; // @[Mem.scala 23:19]
  wire  accHandler_clock; // @[Decoder.scala 185:26]
  wire  accHandler_reset; // @[Decoder.scala 185:26]
  wire  accHandler_io_in_ready; // @[Decoder.scala 185:26]
  wire  accHandler_io_in_valid; // @[Decoder.scala 185:26]
  wire [3:0] accHandler_io_in_bits_instruction_op; // @[Decoder.scala 185:26]
  wire  accHandler_io_in_bits_instruction_sourceLeft; // @[Decoder.scala 185:26]
  wire  accHandler_io_in_bits_instruction_sourceRight; // @[Decoder.scala 185:26]
  wire  accHandler_io_in_bits_instruction_dest; // @[Decoder.scala 185:26]
  wire [5:0] accHandler_io_in_bits_address; // @[Decoder.scala 185:26]
  wire [5:0] accHandler_io_in_bits_altAddress; // @[Decoder.scala 185:26]
  wire  accHandler_io_in_bits_read; // @[Decoder.scala 185:26]
  wire  accHandler_io_in_bits_write; // @[Decoder.scala 185:26]
  wire  accHandler_io_in_bits_accumulate; // @[Decoder.scala 185:26]
  wire [5:0] accHandler_io_in_bits_size; // @[Decoder.scala 185:26]
  wire [2:0] accHandler_io_in_bits_stride; // @[Decoder.scala 185:26]
  wire  accHandler_io_out_ready; // @[Decoder.scala 185:26]
  wire  accHandler_io_out_valid; // @[Decoder.scala 185:26]
  wire [3:0] accHandler_io_out_bits_instruction_op; // @[Decoder.scala 185:26]
  wire  accHandler_io_out_bits_instruction_sourceLeft; // @[Decoder.scala 185:26]
  wire  accHandler_io_out_bits_instruction_sourceRight; // @[Decoder.scala 185:26]
  wire  accHandler_io_out_bits_instruction_dest; // @[Decoder.scala 185:26]
  wire [5:0] accHandler_io_out_bits_address; // @[Decoder.scala 185:26]
  wire [5:0] accHandler_io_out_bits_altAddress; // @[Decoder.scala 185:26]
  wire  accHandler_io_out_bits_read; // @[Decoder.scala 185:26]
  wire  accHandler_io_out_bits_write; // @[Decoder.scala 185:26]
  wire  accHandler_io_out_bits_accumulate; // @[Decoder.scala 185:26]
  wire  arrayHandler_clock; // @[Decoder.scala 202:28]
  wire  arrayHandler_reset; // @[Decoder.scala 202:28]
  wire  arrayHandler_io_in_ready; // @[Decoder.scala 202:28]
  wire  arrayHandler_io_in_valid; // @[Decoder.scala 202:28]
  wire  arrayHandler_io_in_bits_load; // @[Decoder.scala 202:28]
  wire  arrayHandler_io_in_bits_zeroes; // @[Decoder.scala 202:28]
  wire [5:0] arrayHandler_io_in_bits_size; // @[Decoder.scala 202:28]
  wire  arrayHandler_io_out_ready; // @[Decoder.scala 202:28]
  wire  arrayHandler_io_out_valid; // @[Decoder.scala 202:28]
  wire  arrayHandler_io_out_bits_load; // @[Decoder.scala 202:28]
  wire  arrayHandler_io_out_bits_zeroes; // @[Decoder.scala 202:28]
  wire  hostDataflowHandler_clock; // @[Decoder.scala 216:35]
  wire  hostDataflowHandler_reset; // @[Decoder.scala 216:35]
  wire  hostDataflowHandler_io_in_ready; // @[Decoder.scala 216:35]
  wire  hostDataflowHandler_io_in_valid; // @[Decoder.scala 216:35]
  wire [1:0] hostDataflowHandler_io_in_bits_kind; // @[Decoder.scala 216:35]
  wire [5:0] hostDataflowHandler_io_in_bits_size; // @[Decoder.scala 216:35]
  wire  hostDataflowHandler_io_out_ready; // @[Decoder.scala 216:35]
  wire  hostDataflowHandler_io_out_valid; // @[Decoder.scala 216:35]
  wire [1:0] hostDataflowHandler_io_out_bits_kind; // @[Decoder.scala 216:35]
  wire  enqueuer1_clock; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer1_reset; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer1_io_in_ready; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer1_io_in_valid; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer1_io_out_0_ready; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer1_io_out_0_valid; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer2_clock; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer2_reset; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer2_io_in_ready; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer2_io_in_valid; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer2_io_out_0_ready; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer2_io_out_0_valid; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer2_io_out_1_ready; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer2_io_out_1_valid; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer3_clock; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer3_reset; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer3_io_in_ready; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer3_io_in_valid; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer3_io_out_0_ready; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer3_io_out_0_valid; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer3_io_out_1_ready; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer3_io_out_1_valid; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer3_io_out_2_ready; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer3_io_out_2_valid; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer4_clock; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer4_reset; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer4_io_in_ready; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer4_io_in_valid; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer4_io_out_0_ready; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer4_io_out_0_valid; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer4_io_out_1_ready; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer4_io_out_1_valid; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer4_io_out_2_ready; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer4_io_out_2_valid; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer4_io_out_3_ready; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer4_io_out_3_valid; // @[MultiEnqueue.scala 160:43]
  wire  validator_io_instruction_valid; // @[Decoder.scala 571:27]
  wire [3:0] validator_io_instruction_bits_opcode; // @[Decoder.scala 571:27]
  wire [3:0] validator_io_instruction_bits_flags; // @[Decoder.scala 571:27]
  wire [47:0] validator_io_instruction_bits_arguments; // @[Decoder.scala 571:27]
  wire  validator_io_error; // @[Decoder.scala 571:27]
  reg [15:0] timeout; // @[Decoder.scala 91:24]
  reg [15:0] timer; // @[Decoder.scala 92:24]
  wire [15:0] _timer_T_1 = timer + 16'h1; // @[Decoder.scala 97:22]
  reg [31:0] tracepoint; // @[Decoder.scala 103:31]
  reg [31:0] programCounter; // @[Decoder.scala 104:31]
  wire [31:0] _programCounter_T_1 = programCounter + 32'h1; // @[Decoder.scala 106:38]
  wire [31:0] _GEN_2 = instruction_io_deq_ready & instruction_io_deq_valid ? _programCounter_T_1 : programCounter; // @[Decoder.scala 105:48 Decoder.scala 106:20 Decoder.scala 104:31]
  reg [31:0] dram0AddressOffset; // @[Decoder.scala 121:36]
  reg [3:0] dram0CacheBehaviour; // @[Decoder.scala 122:36]
  reg [31:0] dram1AddressOffset; // @[Decoder.scala 123:36]
  reg [3:0] dram1CacheBehaviour; // @[Decoder.scala 124:36]
  wire  io_acc_bits_isMemControl = accHandler_io_out_bits_instruction_op == 4'h0; // @[AccumulatorWithALUArrayControl.scala 101:39]
  wire [5:0] _GEN_3 = accHandler_io_out_bits_write ? accHandler_io_out_bits_altAddress : accHandler_io_out_bits_address; // @[AccumulatorWithALUArrayControl.scala 111:21 AccumulatorWithALUArrayControl.scala 112:25 AccumulatorWithALUArrayControl.scala 115:25]
  wire [5:0] _GEN_4 = accHandler_io_out_bits_write ? accHandler_io_out_bits_address : accHandler_io_out_bits_altAddress; // @[AccumulatorWithALUArrayControl.scala 111:21 AccumulatorWithALUArrayControl.scala 113:26 AccumulatorWithALUArrayControl.scala 116:26]
  wire [5:0] _GEN_5 = accHandler_io_out_bits_read ? accHandler_io_out_bits_address : _GEN_3; // @[AccumulatorWithALUArrayControl.scala 107:18 AccumulatorWithALUArrayControl.scala 108:23]
  wire [5:0] _GEN_6 = accHandler_io_out_bits_read ? accHandler_io_out_bits_altAddress : _GEN_4; // @[AccumulatorWithALUArrayControl.scala 107:18 AccumulatorWithALUArrayControl.scala 109:24]
  wire [3:0] _flags_WIRE_1 = instruction_io_deq_bits_flags;
  wire  flags_accumulate = _flags_WIRE_1[0]; // @[Decoder.scala 250:45]
  wire  flags_zeroes = _flags_WIRE_1[1]; // @[Decoder.scala 250:45]
  wire [47:0] _args_WIRE_1 = instruction_io_deq_bits_arguments;
  wire [5:0] args_memAddress = _args_WIRE_1[5:0]; // @[Decoder.scala 251:48]
  wire [2:0] args_memStride = _args_WIRE_1[8:6]; // @[Decoder.scala 251:48]
  wire [19:0] args_accAddress = _args_WIRE_1[35:16]; // @[Decoder.scala 251:48]
  wire [2:0] args_accStride = _args_WIRE_1[38:36]; // @[Decoder.scala 251:48]
  wire [7:0] args_size = _args_WIRE_1[47:40]; // @[Decoder.scala 251:48]
  wire  _GEN_9 = flags_zeroes & instruction_io_deq_valid; // @[Decoder.scala 253:24 MultiEnqueue.scala 114:17 MultiEnqueue.scala 40:17]
  wire  _GEN_10 = flags_zeroes & io_dataflow_ready; // @[Decoder.scala 253:24 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire [5:0] instruction_io_deq_ready_w_size = args_size[5:0]; // @[Decoder.scala 673:17 Decoder.scala 675:12]
  wire [5:0] _GEN_11 = flags_zeroes ? instruction_io_deq_ready_w_size : instruction_io_deq_ready_w_size; // @[Decoder.scala 253:24 MultiEnqueue.scala 115:10 MultiEnqueue.scala 151:10]
  wire [3:0] _GEN_12 = flags_zeroes ? 4'h3 : 4'h2; // @[Decoder.scala 253:24 MultiEnqueue.scala 115:10 MultiEnqueue.scala 151:10]
  wire  instruction_io_deq_ready_io_dataflow_w_valid = enqueuer3_io_out_0_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  instruction_io_deq_ready_io_dataflow_w_1_valid = enqueuer4_io_out_0_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_13 = flags_zeroes ? instruction_io_deq_ready_io_dataflow_w_valid :
    instruction_io_deq_ready_io_dataflow_w_1_valid; // @[Decoder.scala 253:24 MultiEnqueue.scala 115:10 MultiEnqueue.scala 151:10]
  wire  instruction_io_deq_ready_arrayHandler_io_in_w_ready = arrayHandler_io_in_ready; // @[ReadyValid.scala 16:17 MultiEnqueue.scala 116:10]
  wire  _GEN_14 = flags_zeroes & instruction_io_deq_ready_arrayHandler_io_in_w_ready; // @[Decoder.scala 253:24 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire  instruction_io_deq_ready_arrayHandler_io_in_w_valid = enqueuer3_io_out_1_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  instruction_io_deq_ready_arrayHandler_io_in_w_1_valid = enqueuer4_io_out_2_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_18 = flags_zeroes ? instruction_io_deq_ready_arrayHandler_io_in_w_valid :
    instruction_io_deq_ready_arrayHandler_io_in_w_1_valid; // @[Decoder.scala 253:24 MultiEnqueue.scala 116:10 MultiEnqueue.scala 153:10]
  wire  instruction_io_deq_ready_accHandler_io_in_w_ready = accHandler_io_in_ready; // @[ReadyValid.scala 16:17 MultiEnqueue.scala 117:10]
  wire  _GEN_19 = flags_zeroes & instruction_io_deq_ready_accHandler_io_in_w_ready; // @[Decoder.scala 253:24 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire [5:0] instruction_io_deq_ready_w_2_address = args_accAddress[5:0]; // @[Decoder.scala 656:17 Decoder.scala 658:15]
  wire [5:0] _GEN_27 = flags_zeroes ? instruction_io_deq_ready_w_2_address : instruction_io_deq_ready_w_2_address; // @[Decoder.scala 253:24 MultiEnqueue.scala 117:10 MultiEnqueue.scala 154:10]
  wire  instruction_io_deq_ready_accHandler_io_in_w_valid = enqueuer3_io_out_2_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  instruction_io_deq_ready_accHandler_io_in_w_1_valid = enqueuer4_io_out_3_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_32 = flags_zeroes ? instruction_io_deq_ready_accHandler_io_in_w_valid :
    instruction_io_deq_ready_accHandler_io_in_w_1_valid; // @[Decoder.scala 253:24 MultiEnqueue.scala 117:10 MultiEnqueue.scala 154:10]
  wire  _GEN_33 = flags_zeroes ? enqueuer3_io_in_ready : enqueuer4_io_in_ready; // @[Decoder.scala 253:24 Decoder.scala 254:25 Decoder.scala 269:25]
  wire  _GEN_34 = flags_zeroes ? 1'h0 : instruction_io_deq_valid; // @[Decoder.scala 253:24 MultiEnqueue.scala 40:17 MultiEnqueue.scala 150:17]
  wire  _GEN_35 = flags_zeroes ? 1'h0 : io_dataflow_ready; // @[Decoder.scala 253:24 MultiEnqueue.scala 42:18 ReadyValid.scala 19:11]
  wire  instruction_io_deq_ready_memPortA_io_enq_w_ready = memPortA_io_enq_ready; // @[ReadyValid.scala 16:17 MultiEnqueue.scala 152:10]
  wire  _GEN_36 = flags_zeroes ? 1'h0 : instruction_io_deq_ready_memPortA_io_enq_w_ready; // @[Decoder.scala 253:24 MultiEnqueue.scala 42:18 ReadyValid.scala 19:11]
  wire [2:0] _GEN_38 = flags_zeroes ? 3'h0 : args_memStride; // @[Decoder.scala 253:24 Decoder.scala 609:15 MultiEnqueue.scala 152:10]
  wire [5:0] _GEN_39 = flags_zeroes ? 6'h0 : instruction_io_deq_ready_w_size; // @[Decoder.scala 253:24 Decoder.scala 609:15 MultiEnqueue.scala 152:10]
  wire [5:0] _GEN_40 = flags_zeroes ? 6'h0 : args_memAddress; // @[Decoder.scala 253:24 Decoder.scala 609:15 MultiEnqueue.scala 152:10]
  wire  instruction_io_deq_ready_memPortA_io_enq_w_valid = enqueuer4_io_out_1_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_42 = flags_zeroes ? 1'h0 : instruction_io_deq_ready_memPortA_io_enq_w_valid; // @[Decoder.scala 253:24 Decoder.scala 610:16 MultiEnqueue.scala 152:10]
  wire  _GEN_43 = flags_zeroes ? 1'h0 : instruction_io_deq_ready_arrayHandler_io_in_w_ready; // @[Decoder.scala 253:24 MultiEnqueue.scala 42:18 ReadyValid.scala 19:11]
  wire  _GEN_44 = flags_zeroes ? 1'h0 : instruction_io_deq_ready_accHandler_io_in_w_ready; // @[Decoder.scala 253:24 MultiEnqueue.scala 42:18 ReadyValid.scala 19:11]
  wire  _T_3 = instruction_io_deq_bits_opcode == 4'h3; // @[Decoder.scala 292:38]
  wire [5:0] args_1_address = instruction_io_deq_bits_arguments[5:0]; // @[Decoder.scala 300:48]
  wire [2:0] args_1_stride = instruction_io_deq_bits_arguments[8:6]; // @[Decoder.scala 300:48]
  wire [23:0] args_1_size = instruction_io_deq_bits_arguments[39:16]; // @[Decoder.scala 300:48]
  wire [7:0] stride = 8'h1 << args_1_stride; // @[Decoder.scala 309:24]
  wire [31:0] _instruction_io_deq_ready_T = args_1_size * stride; // @[Decoder.scala 321:37]
  wire [31:0] _GEN_603 = {{26'd0}, args_1_address}; // @[Decoder.scala 321:24]
  wire [31:0] _instruction_io_deq_ready_T_2 = _GEN_603 + _instruction_io_deq_ready_T; // @[Decoder.scala 321:24]
  wire  _GEN_45 = flags_accumulate & instruction_io_deq_valid; // @[Decoder.scala 302:24 MultiEnqueue.scala 60:17 MultiEnqueue.scala 40:17]
  wire  _GEN_46 = flags_accumulate & instruction_io_deq_ready_arrayHandler_io_in_w_ready; // @[Decoder.scala 302:24 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire [5:0] instruction_io_deq_ready_w_7_size = args_1_size[5:0]; // @[Decoder.scala 684:17 Decoder.scala 687:12]
  wire [5:0] _GEN_47 = flags_accumulate ? instruction_io_deq_ready_w_7_size : instruction_io_deq_ready_w_7_size; // @[Decoder.scala 302:24 MultiEnqueue.scala 61:10 MultiEnqueue.scala 116:10]
  wire  instruction_io_deq_ready_arrayHandler_io_in_w_2_valid = enqueuer1_io_out_0_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_50 = flags_accumulate ? instruction_io_deq_ready_arrayHandler_io_in_w_2_valid :
    instruction_io_deq_ready_arrayHandler_io_in_w_valid; // @[Decoder.scala 302:24 MultiEnqueue.scala 61:10 MultiEnqueue.scala 116:10]
  wire  _GEN_51 = flags_accumulate ? enqueuer1_io_in_ready : enqueuer3_io_in_ready; // @[Decoder.scala 302:24 Decoder.scala 303:25 Decoder.scala 310:25]
  wire  _GEN_52 = flags_accumulate ? 1'h0 : instruction_io_deq_valid; // @[Decoder.scala 302:24 MultiEnqueue.scala 40:17 MultiEnqueue.scala 114:17]
  wire  _GEN_53 = flags_accumulate ? 1'h0 : io_dataflow_ready; // @[Decoder.scala 302:24 MultiEnqueue.scala 42:18 ReadyValid.scala 19:11]
  wire [5:0] _GEN_54 = flags_accumulate ? 6'h0 : instruction_io_deq_ready_w_7_size; // @[Decoder.scala 302:24 Decoder.scala 609:15 MultiEnqueue.scala 115:10]
  wire [3:0] _GEN_55 = flags_accumulate ? 4'h0 : 4'h1; // @[Decoder.scala 302:24 Decoder.scala 609:15 MultiEnqueue.scala 115:10]
  wire  _GEN_56 = flags_accumulate ? 1'h0 : instruction_io_deq_ready_io_dataflow_w_valid; // @[Decoder.scala 302:24 Decoder.scala 610:16 MultiEnqueue.scala 115:10]
  wire  _GEN_57 = flags_accumulate ? 1'h0 : instruction_io_deq_ready_arrayHandler_io_in_w_ready; // @[Decoder.scala 302:24 MultiEnqueue.scala 42:18 ReadyValid.scala 19:11]
  wire  _GEN_58 = flags_accumulate ? 1'h0 : instruction_io_deq_ready_memPortA_io_enq_w_ready; // @[Decoder.scala 302:24 MultiEnqueue.scala 42:18 ReadyValid.scala 19:11]
  wire  _GEN_59 = flags_accumulate ? 1'h0 : 1'h1; // @[Decoder.scala 302:24 Decoder.scala 609:15 MultiEnqueue.scala 117:10]
  wire [2:0] _GEN_60 = flags_accumulate ? 3'h0 : args_1_stride; // @[Decoder.scala 302:24 Decoder.scala 609:15 MultiEnqueue.scala 117:10]
  wire [5:0] instruction_io_deq_ready_w_10_address = _instruction_io_deq_ready_T_2[5:0]; // @[MemControl.scala 39:19 MemControl.scala 40:17]
  wire [5:0] _GEN_62 = flags_accumulate ? 6'h0 : instruction_io_deq_ready_w_10_address; // @[Decoder.scala 302:24 Decoder.scala 609:15 MultiEnqueue.scala 117:10]
  wire  _GEN_64 = flags_accumulate ? 1'h0 : instruction_io_deq_ready_accHandler_io_in_w_valid; // @[Decoder.scala 302:24 Decoder.scala 610:16 MultiEnqueue.scala 117:10]
  wire  _T_6 = _flags_WIRE_1 == 4'h1; // @[Decoder.scala 365:27]
  wire  _T_7 = _flags_WIRE_1 == 4'h2; // @[Decoder.scala 391:27]
  wire  _T_8 = _flags_WIRE_1 == 4'h3; // @[Decoder.scala 417:27]
  wire  _T_9 = _flags_WIRE_1 == 4'hc; // @[Decoder.scala 443:18]
  wire  _T_10 = _flags_WIRE_1 == 4'hd; // @[Decoder.scala 465:18]
  wire  _T_11 = _flags_WIRE_1 == 4'hf; // @[Decoder.scala 487:18]
  wire  _GEN_65 = _T_11 & instruction_io_deq_valid; // @[Decoder.scala 488:7 MultiEnqueue.scala 114:17 MultiEnqueue.scala 40:17]
  wire  _GEN_66 = _T_11 & io_dataflow_ready; // @[Decoder.scala 488:7 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire [5:0] _GEN_67 = _T_11 ? instruction_io_deq_ready_w_size : 6'h0; // @[Decoder.scala 488:7 MultiEnqueue.scala 115:10 Decoder.scala 609:15]
  wire [3:0] _GEN_68 = _T_11 ? 4'h5 : 4'h0; // @[Decoder.scala 488:7 MultiEnqueue.scala 115:10 Decoder.scala 609:15]
  wire  _GEN_69 = _T_11 & instruction_io_deq_ready_io_dataflow_w_valid; // @[Decoder.scala 488:7 MultiEnqueue.scala 115:10 Decoder.scala 610:16]
  wire  _GEN_70 = _T_11 & instruction_io_deq_ready_memPortA_io_enq_w_ready; // @[Decoder.scala 488:7 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire [2:0] _GEN_72 = _T_11 ? args_memStride : 3'h0; // @[Decoder.scala 488:7 MultiEnqueue.scala 116:10 Decoder.scala 609:15]
  wire [5:0] _GEN_74 = _T_11 ? args_memAddress : 6'h0; // @[Decoder.scala 488:7 MultiEnqueue.scala 116:10 Decoder.scala 609:15]
  wire  _GEN_76 = _T_11 & instruction_io_deq_ready_arrayHandler_io_in_w_valid; // @[Decoder.scala 488:7 MultiEnqueue.scala 116:10 Decoder.scala 610:16]
  wire  _GEN_77 = _T_11 & instruction_io_deq_ready_accHandler_io_in_w_ready; // @[Decoder.scala 488:7 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire [2:0] _GEN_79 = _T_11 ? args_accStride : 3'h0; // @[Decoder.scala 488:7 MultiEnqueue.scala 117:10 Decoder.scala 609:15]
  wire [5:0] _GEN_85 = _T_11 ? instruction_io_deq_ready_w_2_address : 6'h0; // @[Decoder.scala 488:7 MultiEnqueue.scala 117:10 Decoder.scala 609:15]
  wire  _GEN_90 = _T_11 & instruction_io_deq_ready_accHandler_io_in_w_valid; // @[Decoder.scala 488:7 MultiEnqueue.scala 117:10 Decoder.scala 610:16]
  wire  _GEN_91 = _T_11 ? enqueuer3_io_in_ready : 1'h1; // @[Decoder.scala 488:7 Decoder.scala 490:25 Decoder.scala 510:25]
  wire  _GEN_92 = _T_10 ? instruction_io_deq_valid : _GEN_65; // @[Decoder.scala 466:7 MultiEnqueue.scala 114:17]
  wire  _GEN_93 = _T_10 ? io_dataflow_ready : _GEN_66; // @[Decoder.scala 466:7 ReadyValid.scala 19:11]
  wire [5:0] _GEN_94 = _T_10 ? instruction_io_deq_ready_w_size : _GEN_67; // @[Decoder.scala 466:7 MultiEnqueue.scala 115:10]
  wire [3:0] _GEN_95 = _T_10 ? 4'h5 : _GEN_68; // @[Decoder.scala 466:7 MultiEnqueue.scala 115:10]
  wire  _GEN_96 = _T_10 ? instruction_io_deq_ready_io_dataflow_w_valid : _GEN_69; // @[Decoder.scala 466:7 MultiEnqueue.scala 115:10]
  wire  _GEN_97 = _T_10 ? instruction_io_deq_ready_memPortA_io_enq_w_ready : _GEN_70; // @[Decoder.scala 466:7 ReadyValid.scala 19:11]
  wire [2:0] _GEN_99 = _T_10 ? args_memStride : _GEN_72; // @[Decoder.scala 466:7 MultiEnqueue.scala 116:10]
  wire [5:0] _GEN_101 = _T_10 ? args_memAddress : _GEN_74; // @[Decoder.scala 466:7 MultiEnqueue.scala 116:10]
  wire  _GEN_103 = _T_10 ? instruction_io_deq_ready_arrayHandler_io_in_w_valid : _GEN_76; // @[Decoder.scala 466:7 MultiEnqueue.scala 116:10]
  wire  _GEN_104 = _T_10 ? instruction_io_deq_ready_accHandler_io_in_w_ready : _GEN_77; // @[Decoder.scala 466:7 ReadyValid.scala 19:11]
  wire [2:0] _GEN_106 = _T_10 ? args_accStride : _GEN_79; // @[Decoder.scala 466:7 MultiEnqueue.scala 117:10]
  wire  _GEN_108 = _T_10 ? 1'h0 : _T_11; // @[Decoder.scala 466:7 MultiEnqueue.scala 117:10]
  wire  _GEN_109 = _T_10 | _T_11; // @[Decoder.scala 466:7 MultiEnqueue.scala 117:10]
  wire [5:0] _GEN_112 = _T_10 ? instruction_io_deq_ready_w_2_address : _GEN_85; // @[Decoder.scala 466:7 MultiEnqueue.scala 117:10]
  wire  _GEN_117 = _T_10 ? instruction_io_deq_ready_accHandler_io_in_w_valid : _GEN_90; // @[Decoder.scala 466:7 MultiEnqueue.scala 117:10]
  wire  _GEN_118 = _T_10 ? enqueuer3_io_in_ready : _GEN_91; // @[Decoder.scala 466:7 Decoder.scala 468:25]
  wire  _GEN_119 = _T_9 ? instruction_io_deq_valid : _GEN_92; // @[Decoder.scala 444:7 MultiEnqueue.scala 114:17]
  wire  _GEN_120 = _T_9 ? io_dataflow_ready : _GEN_93; // @[Decoder.scala 444:7 ReadyValid.scala 19:11]
  wire [5:0] _GEN_121 = _T_9 ? instruction_io_deq_ready_w_size : _GEN_94; // @[Decoder.scala 444:7 MultiEnqueue.scala 115:10]
  wire [3:0] _GEN_122 = _T_9 ? 4'h4 : _GEN_95; // @[Decoder.scala 444:7 MultiEnqueue.scala 115:10]
  wire  _GEN_123 = _T_9 ? instruction_io_deq_ready_io_dataflow_w_valid : _GEN_96; // @[Decoder.scala 444:7 MultiEnqueue.scala 115:10]
  wire  _GEN_124 = _T_9 ? instruction_io_deq_ready_memPortA_io_enq_w_ready : _GEN_97; // @[Decoder.scala 444:7 ReadyValid.scala 19:11]
  wire [2:0] _GEN_126 = _T_9 ? args_memStride : _GEN_99; // @[Decoder.scala 444:7 MultiEnqueue.scala 116:10]
  wire [5:0] _GEN_128 = _T_9 ? args_memAddress : _GEN_101; // @[Decoder.scala 444:7 MultiEnqueue.scala 116:10]
  wire  _GEN_130 = _T_9 ? instruction_io_deq_ready_arrayHandler_io_in_w_valid : _GEN_103; // @[Decoder.scala 444:7 MultiEnqueue.scala 116:10]
  wire  _GEN_131 = _T_9 ? instruction_io_deq_ready_accHandler_io_in_w_ready : _GEN_104; // @[Decoder.scala 444:7 ReadyValid.scala 19:11]
  wire [2:0] _GEN_133 = _T_9 ? args_accStride : _GEN_106; // @[Decoder.scala 444:7 MultiEnqueue.scala 117:10]
  wire  _GEN_135 = _T_9 ? 1'h0 : _GEN_108; // @[Decoder.scala 444:7 MultiEnqueue.scala 117:10]
  wire  _GEN_136 = _T_9 ? 1'h0 : _GEN_109; // @[Decoder.scala 444:7 MultiEnqueue.scala 117:10]
  wire [5:0] _GEN_139 = _T_9 ? instruction_io_deq_ready_w_2_address : _GEN_112; // @[Decoder.scala 444:7 MultiEnqueue.scala 117:10]
  wire  _GEN_144 = _T_9 ? instruction_io_deq_ready_accHandler_io_in_w_valid : _GEN_117; // @[Decoder.scala 444:7 MultiEnqueue.scala 117:10]
  wire  _GEN_145 = _T_9 ? enqueuer3_io_in_ready : _GEN_118; // @[Decoder.scala 444:7 Decoder.scala 446:25]
  wire  _GEN_146 = _flags_WIRE_1 == 4'h3 ? instruction_io_deq_valid : _GEN_119; // @[Decoder.scala 417:59 MultiEnqueue.scala 114:17]
  wire  instruction_io_deq_ready_hostDataflowHandler_io_in_w_3_ready = hostDataflowHandler_io_in_ready; // @[ReadyValid.scala 16:17 MultiEnqueue.scala 115:10]
  wire  _GEN_147 = _flags_WIRE_1 == 4'h3 ? instruction_io_deq_ready_hostDataflowHandler_io_in_w_3_ready : _GEN_120; // @[Decoder.scala 417:59 ReadyValid.scala 19:11]
  wire [5:0] _GEN_148 = _flags_WIRE_1 == 4'h3 ? instruction_io_deq_ready_w_size : 6'h0; // @[Decoder.scala 417:59 MultiEnqueue.scala 115:10 Decoder.scala 609:15]
  wire [1:0] _GEN_149 = _flags_WIRE_1 == 4'h3 ? 2'h3 : 2'h0; // @[Decoder.scala 417:59 MultiEnqueue.scala 115:10 Decoder.scala 609:15]
  wire  _GEN_150 = _flags_WIRE_1 == 4'h3 & instruction_io_deq_ready_io_dataflow_w_valid; // @[Decoder.scala 417:59 MultiEnqueue.scala 115:10 Decoder.scala 610:16]
  wire  instruction_io_deq_ready_memPortB_io_enq_w_3_ready = memPortB_io_enq_ready; // @[ReadyValid.scala 16:17 MultiEnqueue.scala 116:10]
  wire  _GEN_151 = _flags_WIRE_1 == 4'h3 ? instruction_io_deq_ready_memPortB_io_enq_w_3_ready : _GEN_124; // @[Decoder.scala 417:59 ReadyValid.scala 19:11]
  wire [2:0] _GEN_153 = _flags_WIRE_1 == 4'h3 ? args_memStride : 3'h0; // @[Decoder.scala 417:59 MultiEnqueue.scala 116:10 Decoder.scala 609:15]
  wire [5:0] _GEN_155 = _flags_WIRE_1 == 4'h3 ? args_memAddress : 6'h0; // @[Decoder.scala 417:59 MultiEnqueue.scala 116:10 Decoder.scala 609:15]
  wire  _GEN_157 = _flags_WIRE_1 == 4'h3 & instruction_io_deq_ready_arrayHandler_io_in_w_valid; // @[Decoder.scala 417:59 MultiEnqueue.scala 116:10 Decoder.scala 610:16]
  wire  instruction_io_deq_ready_dram1Handler_io_in_w_1_ready = dram1Handler_io_in_ready; // @[ReadyValid.scala 16:17 MultiEnqueue.scala 117:10]
  wire  _GEN_158 = _flags_WIRE_1 == 4'h3 ? instruction_io_deq_ready_dram1Handler_io_in_w_1_ready : _GEN_131; // @[Decoder.scala 417:59 ReadyValid.scala 19:11]
  wire [2:0] _GEN_160 = _flags_WIRE_1 == 4'h3 ? args_accStride : 3'h0; // @[Decoder.scala 417:59 MultiEnqueue.scala 117:10 Decoder.scala 609:15]
  wire [19:0] instruction_io_deq_ready_w_22_size = {{12'd0}, args_size}; // @[MemControl.scala 39:19 MemControl.scala 41:14]
  wire [19:0] _GEN_161 = _flags_WIRE_1 == 4'h3 ? instruction_io_deq_ready_w_22_size : 20'h0; // @[Decoder.scala 417:59 MultiEnqueue.scala 117:10 Decoder.scala 609:15]
  wire [19:0] _GEN_162 = _flags_WIRE_1 == 4'h3 ? args_accAddress : 20'h0; // @[Decoder.scala 417:59 MultiEnqueue.scala 117:10 Decoder.scala 609:15]
  wire  _GEN_164 = _flags_WIRE_1 == 4'h3 & instruction_io_deq_ready_accHandler_io_in_w_valid; // @[Decoder.scala 417:59 MultiEnqueue.scala 117:10 Decoder.scala 610:16]
  wire  _GEN_165 = _flags_WIRE_1 == 4'h3 ? enqueuer3_io_in_ready : _GEN_145; // @[Decoder.scala 417:59 Decoder.scala 418:25]
  wire [5:0] _GEN_166 = _flags_WIRE_1 == 4'h3 ? 6'h0 : _GEN_121; // @[Decoder.scala 417:59 Decoder.scala 609:15]
  wire [3:0] _GEN_167 = _flags_WIRE_1 == 4'h3 ? 4'h0 : _GEN_122; // @[Decoder.scala 417:59 Decoder.scala 609:15]
  wire  _GEN_168 = _flags_WIRE_1 == 4'h3 ? 1'h0 : _GEN_123; // @[Decoder.scala 417:59 Decoder.scala 610:16]
  wire [2:0] _GEN_170 = _flags_WIRE_1 == 4'h3 ? 3'h0 : _GEN_126; // @[Decoder.scala 417:59 Decoder.scala 609:15]
  wire [5:0] _GEN_172 = _flags_WIRE_1 == 4'h3 ? 6'h0 : _GEN_128; // @[Decoder.scala 417:59 Decoder.scala 609:15]
  wire  _GEN_173 = _flags_WIRE_1 == 4'h3 ? 1'h0 : _T_9; // @[Decoder.scala 417:59 Decoder.scala 609:15]
  wire  _GEN_174 = _flags_WIRE_1 == 4'h3 ? 1'h0 : _GEN_130; // @[Decoder.scala 417:59 Decoder.scala 610:16]
  wire [2:0] _GEN_176 = _flags_WIRE_1 == 4'h3 ? 3'h0 : _GEN_133; // @[Decoder.scala 417:59 Decoder.scala 609:15]
  wire  _GEN_178 = _flags_WIRE_1 == 4'h3 ? 1'h0 : _GEN_135; // @[Decoder.scala 417:59 Decoder.scala 609:15]
  wire  _GEN_179 = _flags_WIRE_1 == 4'h3 ? 1'h0 : _GEN_136; // @[Decoder.scala 417:59 Decoder.scala 609:15]
  wire [5:0] _GEN_182 = _flags_WIRE_1 == 4'h3 ? 6'h0 : _GEN_139; // @[Decoder.scala 417:59 Decoder.scala 609:15]
  wire  _GEN_187 = _flags_WIRE_1 == 4'h3 ? 1'h0 : _GEN_144; // @[Decoder.scala 417:59 Decoder.scala 610:16]
  wire  _GEN_188 = _flags_WIRE_1 == 4'h2 ? instruction_io_deq_valid : _GEN_146; // @[Decoder.scala 391:59 MultiEnqueue.scala 114:17]
  wire  _GEN_189 = _flags_WIRE_1 == 4'h2 ? instruction_io_deq_ready_hostDataflowHandler_io_in_w_3_ready : _GEN_147; // @[Decoder.scala 391:59 ReadyValid.scala 19:11]
  wire [5:0] _GEN_190 = _flags_WIRE_1 == 4'h2 ? instruction_io_deq_ready_w_size : _GEN_148; // @[Decoder.scala 391:59 MultiEnqueue.scala 115:10]
  wire [1:0] _GEN_191 = _flags_WIRE_1 == 4'h2 ? 2'h2 : _GEN_149; // @[Decoder.scala 391:59 MultiEnqueue.scala 115:10]
  wire  _GEN_192 = _flags_WIRE_1 == 4'h2 ? instruction_io_deq_ready_io_dataflow_w_valid : _GEN_150; // @[Decoder.scala 391:59 MultiEnqueue.scala 115:10]
  wire  _GEN_193 = _flags_WIRE_1 == 4'h2 ? instruction_io_deq_ready_memPortB_io_enq_w_3_ready : _GEN_151; // @[Decoder.scala 391:59 ReadyValid.scala 19:11]
  wire [2:0] _GEN_195 = _flags_WIRE_1 == 4'h2 ? args_memStride : _GEN_153; // @[Decoder.scala 391:59 MultiEnqueue.scala 116:10]
  wire [5:0] _GEN_197 = _flags_WIRE_1 == 4'h2 ? args_memAddress : _GEN_155; // @[Decoder.scala 391:59 MultiEnqueue.scala 116:10]
  wire  _GEN_199 = _flags_WIRE_1 == 4'h2 ? instruction_io_deq_ready_arrayHandler_io_in_w_valid : _GEN_157; // @[Decoder.scala 391:59 MultiEnqueue.scala 116:10]
  wire  _GEN_200 = _flags_WIRE_1 == 4'h2 ? instruction_io_deq_ready_dram1Handler_io_in_w_1_ready : _GEN_158; // @[Decoder.scala 391:59 ReadyValid.scala 19:11]
  wire [2:0] _GEN_202 = _flags_WIRE_1 == 4'h2 ? args_accStride : _GEN_160; // @[Decoder.scala 391:59 MultiEnqueue.scala 117:10]
  wire [19:0] _GEN_203 = _flags_WIRE_1 == 4'h2 ? instruction_io_deq_ready_w_22_size : _GEN_161; // @[Decoder.scala 391:59 MultiEnqueue.scala 117:10]
  wire [19:0] _GEN_204 = _flags_WIRE_1 == 4'h2 ? args_accAddress : _GEN_162; // @[Decoder.scala 391:59 MultiEnqueue.scala 117:10]
  wire  _GEN_205 = _flags_WIRE_1 == 4'h2 ? 1'h0 : _T_8; // @[Decoder.scala 391:59 MultiEnqueue.scala 117:10]
  wire  _GEN_206 = _flags_WIRE_1 == 4'h2 ? instruction_io_deq_ready_accHandler_io_in_w_valid : _GEN_164; // @[Decoder.scala 391:59 MultiEnqueue.scala 117:10]
  wire  _GEN_207 = _flags_WIRE_1 == 4'h2 ? enqueuer3_io_in_ready : _GEN_165; // @[Decoder.scala 391:59 Decoder.scala 393:25]
  wire [5:0] _GEN_208 = _flags_WIRE_1 == 4'h2 ? 6'h0 : _GEN_166; // @[Decoder.scala 391:59 Decoder.scala 609:15]
  wire [3:0] _GEN_209 = _flags_WIRE_1 == 4'h2 ? 4'h0 : _GEN_167; // @[Decoder.scala 391:59 Decoder.scala 609:15]
  wire  _GEN_210 = _flags_WIRE_1 == 4'h2 ? 1'h0 : _GEN_168; // @[Decoder.scala 391:59 Decoder.scala 610:16]
  wire [2:0] _GEN_212 = _flags_WIRE_1 == 4'h2 ? 3'h0 : _GEN_170; // @[Decoder.scala 391:59 Decoder.scala 609:15]
  wire [5:0] _GEN_214 = _flags_WIRE_1 == 4'h2 ? 6'h0 : _GEN_172; // @[Decoder.scala 391:59 Decoder.scala 609:15]
  wire  _GEN_215 = _flags_WIRE_1 == 4'h2 ? 1'h0 : _GEN_173; // @[Decoder.scala 391:59 Decoder.scala 609:15]
  wire  _GEN_216 = _flags_WIRE_1 == 4'h2 ? 1'h0 : _GEN_174; // @[Decoder.scala 391:59 Decoder.scala 610:16]
  wire [2:0] _GEN_218 = _flags_WIRE_1 == 4'h2 ? 3'h0 : _GEN_176; // @[Decoder.scala 391:59 Decoder.scala 609:15]
  wire  _GEN_220 = _flags_WIRE_1 == 4'h2 ? 1'h0 : _GEN_178; // @[Decoder.scala 391:59 Decoder.scala 609:15]
  wire  _GEN_221 = _flags_WIRE_1 == 4'h2 ? 1'h0 : _GEN_179; // @[Decoder.scala 391:59 Decoder.scala 609:15]
  wire [5:0] _GEN_224 = _flags_WIRE_1 == 4'h2 ? 6'h0 : _GEN_182; // @[Decoder.scala 391:59 Decoder.scala 609:15]
  wire  _GEN_229 = _flags_WIRE_1 == 4'h2 ? 1'h0 : _GEN_187; // @[Decoder.scala 391:59 Decoder.scala 610:16]
  wire  _GEN_230 = _flags_WIRE_1 == 4'h1 ? instruction_io_deq_valid : _GEN_188; // @[Decoder.scala 365:59 MultiEnqueue.scala 114:17]
  wire  _GEN_231 = _flags_WIRE_1 == 4'h1 ? instruction_io_deq_ready_hostDataflowHandler_io_in_w_3_ready : _GEN_189; // @[Decoder.scala 365:59 ReadyValid.scala 19:11]
  wire [5:0] _GEN_232 = _flags_WIRE_1 == 4'h1 ? instruction_io_deq_ready_w_size : _GEN_190; // @[Decoder.scala 365:59 MultiEnqueue.scala 115:10]
  wire [1:0] _GEN_233 = _flags_WIRE_1 == 4'h1 ? 2'h1 : _GEN_191; // @[Decoder.scala 365:59 MultiEnqueue.scala 115:10]
  wire  _GEN_234 = _flags_WIRE_1 == 4'h1 ? instruction_io_deq_ready_io_dataflow_w_valid : _GEN_192; // @[Decoder.scala 365:59 MultiEnqueue.scala 115:10]
  wire  _GEN_235 = _flags_WIRE_1 == 4'h1 ? instruction_io_deq_ready_memPortB_io_enq_w_3_ready : _GEN_193; // @[Decoder.scala 365:59 ReadyValid.scala 19:11]
  wire [2:0] _GEN_237 = _flags_WIRE_1 == 4'h1 ? args_memStride : _GEN_195; // @[Decoder.scala 365:59 MultiEnqueue.scala 116:10]
  wire [5:0] _GEN_239 = _flags_WIRE_1 == 4'h1 ? args_memAddress : _GEN_197; // @[Decoder.scala 365:59 MultiEnqueue.scala 116:10]
  wire  _GEN_240 = _flags_WIRE_1 == 4'h1 ? 1'h0 : _T_7; // @[Decoder.scala 365:59 MultiEnqueue.scala 116:10]
  wire  _GEN_241 = _flags_WIRE_1 == 4'h1 ? instruction_io_deq_ready_arrayHandler_io_in_w_valid : _GEN_199; // @[Decoder.scala 365:59 MultiEnqueue.scala 116:10]
  wire  instruction_io_deq_ready_dram0Handler_io_in_w_1_ready = dram0Handler_io_in_ready; // @[ReadyValid.scala 16:17 MultiEnqueue.scala 117:10]
  wire  _GEN_242 = _flags_WIRE_1 == 4'h1 ? instruction_io_deq_ready_dram0Handler_io_in_w_1_ready : _GEN_200; // @[Decoder.scala 365:59 ReadyValid.scala 19:11]
  wire [2:0] _GEN_244 = _flags_WIRE_1 == 4'h1 ? args_accStride : 3'h0; // @[Decoder.scala 365:59 MultiEnqueue.scala 117:10 Decoder.scala 609:15]
  wire [19:0] _GEN_245 = _flags_WIRE_1 == 4'h1 ? instruction_io_deq_ready_w_22_size : 20'h0; // @[Decoder.scala 365:59 MultiEnqueue.scala 117:10 Decoder.scala 609:15]
  wire [19:0] _GEN_246 = _flags_WIRE_1 == 4'h1 ? args_accAddress : 20'h0; // @[Decoder.scala 365:59 MultiEnqueue.scala 117:10 Decoder.scala 609:15]
  wire  _GEN_248 = _flags_WIRE_1 == 4'h1 & instruction_io_deq_ready_accHandler_io_in_w_valid; // @[Decoder.scala 365:59 MultiEnqueue.scala 117:10 Decoder.scala 610:16]
  wire  _GEN_249 = _flags_WIRE_1 == 4'h1 ? enqueuer3_io_in_ready : _GEN_207; // @[Decoder.scala 365:59 Decoder.scala 367:25]
  wire [2:0] _GEN_251 = _flags_WIRE_1 == 4'h1 ? 3'h0 : _GEN_202; // @[Decoder.scala 365:59 Decoder.scala 609:15]
  wire [19:0] _GEN_252 = _flags_WIRE_1 == 4'h1 ? 20'h0 : _GEN_203; // @[Decoder.scala 365:59 Decoder.scala 609:15]
  wire [19:0] _GEN_253 = _flags_WIRE_1 == 4'h1 ? 20'h0 : _GEN_204; // @[Decoder.scala 365:59 Decoder.scala 609:15]
  wire  _GEN_254 = _flags_WIRE_1 == 4'h1 ? 1'h0 : _GEN_205; // @[Decoder.scala 365:59 Decoder.scala 609:15]
  wire  _GEN_255 = _flags_WIRE_1 == 4'h1 ? 1'h0 : _GEN_206; // @[Decoder.scala 365:59 Decoder.scala 610:16]
  wire [5:0] _GEN_256 = _flags_WIRE_1 == 4'h1 ? 6'h0 : _GEN_208; // @[Decoder.scala 365:59 Decoder.scala 609:15]
  wire [3:0] _GEN_257 = _flags_WIRE_1 == 4'h1 ? 4'h0 : _GEN_209; // @[Decoder.scala 365:59 Decoder.scala 609:15]
  wire  _GEN_258 = _flags_WIRE_1 == 4'h1 ? 1'h0 : _GEN_210; // @[Decoder.scala 365:59 Decoder.scala 610:16]
  wire [2:0] _GEN_260 = _flags_WIRE_1 == 4'h1 ? 3'h0 : _GEN_212; // @[Decoder.scala 365:59 Decoder.scala 609:15]
  wire [5:0] _GEN_262 = _flags_WIRE_1 == 4'h1 ? 6'h0 : _GEN_214; // @[Decoder.scala 365:59 Decoder.scala 609:15]
  wire  _GEN_263 = _flags_WIRE_1 == 4'h1 ? 1'h0 : _GEN_215; // @[Decoder.scala 365:59 Decoder.scala 609:15]
  wire  _GEN_264 = _flags_WIRE_1 == 4'h1 ? 1'h0 : _GEN_216; // @[Decoder.scala 365:59 Decoder.scala 610:16]
  wire [2:0] _GEN_266 = _flags_WIRE_1 == 4'h1 ? 3'h0 : _GEN_218; // @[Decoder.scala 365:59 Decoder.scala 609:15]
  wire  _GEN_268 = _flags_WIRE_1 == 4'h1 ? 1'h0 : _GEN_220; // @[Decoder.scala 365:59 Decoder.scala 609:15]
  wire  _GEN_269 = _flags_WIRE_1 == 4'h1 ? 1'h0 : _GEN_221; // @[Decoder.scala 365:59 Decoder.scala 609:15]
  wire [5:0] _GEN_272 = _flags_WIRE_1 == 4'h1 ? 6'h0 : _GEN_224; // @[Decoder.scala 365:59 Decoder.scala 609:15]
  wire  _GEN_277 = _flags_WIRE_1 == 4'h1 ? 1'h0 : _GEN_229; // @[Decoder.scala 365:59 Decoder.scala 610:16]
  wire  _GEN_278 = _flags_WIRE_1 == 4'h0 ? instruction_io_deq_valid : _GEN_230; // @[Decoder.scala 339:53 MultiEnqueue.scala 114:17]
  wire  _GEN_279 = _flags_WIRE_1 == 4'h0 ? instruction_io_deq_ready_hostDataflowHandler_io_in_w_3_ready : _GEN_231; // @[Decoder.scala 339:53 ReadyValid.scala 19:11]
  wire [5:0] _GEN_280 = _flags_WIRE_1 == 4'h0 ? instruction_io_deq_ready_w_size : _GEN_232; // @[Decoder.scala 339:53 MultiEnqueue.scala 115:10]
  wire [1:0] _GEN_281 = _flags_WIRE_1 == 4'h0 ? 2'h0 : _GEN_233; // @[Decoder.scala 339:53 MultiEnqueue.scala 115:10]
  wire  _GEN_282 = _flags_WIRE_1 == 4'h0 ? instruction_io_deq_ready_io_dataflow_w_valid : _GEN_234; // @[Decoder.scala 339:53 MultiEnqueue.scala 115:10]
  wire  _GEN_283 = _flags_WIRE_1 == 4'h0 ? instruction_io_deq_ready_memPortB_io_enq_w_3_ready : _GEN_235; // @[Decoder.scala 339:53 ReadyValid.scala 19:11]
  wire [2:0] _GEN_285 = _flags_WIRE_1 == 4'h0 ? args_memStride : _GEN_237; // @[Decoder.scala 339:53 MultiEnqueue.scala 116:10]
  wire [5:0] _GEN_287 = _flags_WIRE_1 == 4'h0 ? args_memAddress : _GEN_239; // @[Decoder.scala 339:53 MultiEnqueue.scala 116:10]
  wire  _GEN_288 = _flags_WIRE_1 == 4'h0 | _GEN_240; // @[Decoder.scala 339:53 MultiEnqueue.scala 116:10]
  wire  _GEN_289 = _flags_WIRE_1 == 4'h0 ? instruction_io_deq_ready_arrayHandler_io_in_w_valid : _GEN_241; // @[Decoder.scala 339:53 MultiEnqueue.scala 116:10]
  wire  _GEN_290 = _flags_WIRE_1 == 4'h0 ? instruction_io_deq_ready_dram0Handler_io_in_w_1_ready : _GEN_242; // @[Decoder.scala 339:53 ReadyValid.scala 19:11]
  wire [2:0] _GEN_292 = _flags_WIRE_1 == 4'h0 ? args_accStride : _GEN_244; // @[Decoder.scala 339:53 MultiEnqueue.scala 117:10]
  wire [19:0] _GEN_293 = _flags_WIRE_1 == 4'h0 ? instruction_io_deq_ready_w_22_size : _GEN_245; // @[Decoder.scala 339:53 MultiEnqueue.scala 117:10]
  wire [19:0] _GEN_294 = _flags_WIRE_1 == 4'h0 ? args_accAddress : _GEN_246; // @[Decoder.scala 339:53 MultiEnqueue.scala 117:10]
  wire  _GEN_295 = _flags_WIRE_1 == 4'h0 ? 1'h0 : _T_6; // @[Decoder.scala 339:53 MultiEnqueue.scala 117:10]
  wire  _GEN_296 = _flags_WIRE_1 == 4'h0 ? instruction_io_deq_ready_accHandler_io_in_w_valid : _GEN_248; // @[Decoder.scala 339:53 MultiEnqueue.scala 117:10]
  wire  _GEN_297 = _flags_WIRE_1 == 4'h0 ? enqueuer3_io_in_ready : _GEN_249; // @[Decoder.scala 339:53 Decoder.scala 341:25]
  wire [2:0] _GEN_299 = _flags_WIRE_1 == 4'h0 ? 3'h0 : _GEN_251; // @[Decoder.scala 339:53 Decoder.scala 609:15]
  wire [19:0] _GEN_300 = _flags_WIRE_1 == 4'h0 ? 20'h0 : _GEN_252; // @[Decoder.scala 339:53 Decoder.scala 609:15]
  wire [19:0] _GEN_301 = _flags_WIRE_1 == 4'h0 ? 20'h0 : _GEN_253; // @[Decoder.scala 339:53 Decoder.scala 609:15]
  wire  _GEN_302 = _flags_WIRE_1 == 4'h0 ? 1'h0 : _GEN_254; // @[Decoder.scala 339:53 Decoder.scala 609:15]
  wire  _GEN_303 = _flags_WIRE_1 == 4'h0 ? 1'h0 : _GEN_255; // @[Decoder.scala 339:53 Decoder.scala 610:16]
  wire [5:0] _GEN_304 = _flags_WIRE_1 == 4'h0 ? 6'h0 : _GEN_256; // @[Decoder.scala 339:53 Decoder.scala 609:15]
  wire [3:0] _GEN_305 = _flags_WIRE_1 == 4'h0 ? 4'h0 : _GEN_257; // @[Decoder.scala 339:53 Decoder.scala 609:15]
  wire  _GEN_306 = _flags_WIRE_1 == 4'h0 ? 1'h0 : _GEN_258; // @[Decoder.scala 339:53 Decoder.scala 610:16]
  wire [2:0] _GEN_308 = _flags_WIRE_1 == 4'h0 ? 3'h0 : _GEN_260; // @[Decoder.scala 339:53 Decoder.scala 609:15]
  wire [5:0] _GEN_310 = _flags_WIRE_1 == 4'h0 ? 6'h0 : _GEN_262; // @[Decoder.scala 339:53 Decoder.scala 609:15]
  wire  _GEN_311 = _flags_WIRE_1 == 4'h0 ? 1'h0 : _GEN_263; // @[Decoder.scala 339:53 Decoder.scala 609:15]
  wire  _GEN_312 = _flags_WIRE_1 == 4'h0 ? 1'h0 : _GEN_264; // @[Decoder.scala 339:53 Decoder.scala 610:16]
  wire [2:0] _GEN_314 = _flags_WIRE_1 == 4'h0 ? 3'h0 : _GEN_266; // @[Decoder.scala 339:53 Decoder.scala 609:15]
  wire  _GEN_316 = _flags_WIRE_1 == 4'h0 ? 1'h0 : _GEN_268; // @[Decoder.scala 339:53 Decoder.scala 609:15]
  wire  _GEN_317 = _flags_WIRE_1 == 4'h0 ? 1'h0 : _GEN_269; // @[Decoder.scala 339:53 Decoder.scala 609:15]
  wire [5:0] _GEN_320 = _flags_WIRE_1 == 4'h0 ? 6'h0 : _GEN_272; // @[Decoder.scala 339:53 Decoder.scala 609:15]
  wire  _GEN_325 = _flags_WIRE_1 == 4'h0 ? 1'h0 : _GEN_277; // @[Decoder.scala 339:53 Decoder.scala 610:16]
  wire  flags_3_accumulate = _flags_WIRE_1[2]; // @[Decoder.scala 518:45]
  wire [15:0] args_3_accWriteAddress = _args_WIRE_1[15:0]; // @[Decoder.scala 519:48]
  wire [23:0] args_3_accReadAddress = _args_WIRE_1[39:16]; // @[Decoder.scala 519:48]
  wire  args_3_instruction_dest = _args_WIRE_1[40]; // @[Decoder.scala 519:48]
  wire  args_3_instruction_sourceRight = _args_WIRE_1[41]; // @[Decoder.scala 519:48]
  wire  args_3_instruction_sourceLeft = _args_WIRE_1[42]; // @[Decoder.scala 519:48]
  wire [3:0] args_3_instruction_op = _args_WIRE_1[46:43]; // @[Decoder.scala 519:48]
  wire [3:0] args_4_register = instruction_io_deq_bits_arguments[3:0]; // @[Decoder.scala 540:50]
  wire [27:0] args_4_value = instruction_io_deq_bits_arguments[31:4]; // @[Decoder.scala 540:50]
  wire [43:0] _dram0AddressOffset_T = {args_4_value, 16'h0}; // @[Decoder.scala 543:43]
  wire [31:0] _GEN_327 = args_4_register == 4'ha ? {{4'd0}, args_4_value} : _GEN_2; // @[Decoder.scala 554:62 Decoder.scala 555:24]
  wire [31:0] _GEN_329 = args_4_register == 4'h9 ? {{4'd0}, args_4_value} : tracepoint; // @[Decoder.scala 552:58 Decoder.scala 553:20 Decoder.scala 103:31]
  wire [31:0] _GEN_330 = args_4_register == 4'h9 ? _GEN_2 : _GEN_327; // @[Decoder.scala 552:58]
  wire [27:0] _GEN_332 = args_4_register == 4'h8 ? args_4_value : {{12'd0}, timeout}; // @[Decoder.scala 550:55 Decoder.scala 551:17 Decoder.scala 91:24]
  wire [31:0] _GEN_333 = args_4_register == 4'h8 ? tracepoint : _GEN_329; // @[Decoder.scala 550:55 Decoder.scala 103:31]
  wire [31:0] _GEN_334 = args_4_register == 4'h8 ? _GEN_2 : _GEN_330; // @[Decoder.scala 550:55]
  wire [27:0] _GEN_336 = args_4_register == 4'h5 ? args_4_value : {{24'd0}, dram1CacheBehaviour}; // @[Decoder.scala 548:67 Decoder.scala 549:29 Decoder.scala 124:36]
  wire [27:0] _GEN_337 = args_4_register == 4'h5 ? {{12'd0}, timeout} : _GEN_332; // @[Decoder.scala 548:67 Decoder.scala 91:24]
  wire [31:0] _GEN_338 = args_4_register == 4'h5 ? tracepoint : _GEN_333; // @[Decoder.scala 548:67 Decoder.scala 103:31]
  wire [31:0] _GEN_339 = args_4_register == 4'h5 ? _GEN_2 : _GEN_334; // @[Decoder.scala 548:67]
  wire [43:0] _GEN_341 = args_4_register == 4'h4 ? _dram0AddressOffset_T : {{12'd0}, dram1AddressOffset}; // @[Decoder.scala 546:66 Decoder.scala 547:28 Decoder.scala 123:36]
  wire [27:0] _GEN_342 = args_4_register == 4'h4 ? {{24'd0}, dram1CacheBehaviour} : _GEN_336; // @[Decoder.scala 546:66 Decoder.scala 124:36]
  wire [27:0] _GEN_343 = args_4_register == 4'h4 ? {{12'd0}, timeout} : _GEN_337; // @[Decoder.scala 546:66 Decoder.scala 91:24]
  wire [31:0] _GEN_344 = args_4_register == 4'h4 ? tracepoint : _GEN_338; // @[Decoder.scala 546:66 Decoder.scala 103:31]
  wire [31:0] _GEN_345 = args_4_register == 4'h4 ? _GEN_2 : _GEN_339; // @[Decoder.scala 546:66]
  wire [27:0] _GEN_347 = args_4_register == 4'h1 ? args_4_value : {{24'd0}, dram0CacheBehaviour}; // @[Decoder.scala 544:67 Decoder.scala 545:29 Decoder.scala 122:36]
  wire [43:0] _GEN_348 = args_4_register == 4'h1 ? {{12'd0}, dram1AddressOffset} : _GEN_341; // @[Decoder.scala 544:67 Decoder.scala 123:36]
  wire [27:0] _GEN_349 = args_4_register == 4'h1 ? {{24'd0}, dram1CacheBehaviour} : _GEN_342; // @[Decoder.scala 544:67 Decoder.scala 124:36]
  wire [27:0] _GEN_350 = args_4_register == 4'h1 ? {{12'd0}, timeout} : _GEN_343; // @[Decoder.scala 544:67 Decoder.scala 91:24]
  wire [31:0] _GEN_351 = args_4_register == 4'h1 ? tracepoint : _GEN_344; // @[Decoder.scala 544:67 Decoder.scala 103:31]
  wire [31:0] _GEN_352 = args_4_register == 4'h1 ? _GEN_2 : _GEN_345; // @[Decoder.scala 544:67]
  wire [43:0] _GEN_354 = args_4_register == 4'h0 ? _dram0AddressOffset_T : {{12'd0}, dram0AddressOffset}; // @[Decoder.scala 542:60 Decoder.scala 543:28 Decoder.scala 121:36]
  wire [27:0] _GEN_355 = args_4_register == 4'h0 ? {{24'd0}, dram0CacheBehaviour} : _GEN_347; // @[Decoder.scala 542:60 Decoder.scala 122:36]
  wire [43:0] _GEN_356 = args_4_register == 4'h0 ? {{12'd0}, dram1AddressOffset} : _GEN_348; // @[Decoder.scala 542:60 Decoder.scala 123:36]
  wire [27:0] _GEN_357 = args_4_register == 4'h0 ? {{24'd0}, dram1CacheBehaviour} : _GEN_349; // @[Decoder.scala 542:60 Decoder.scala 124:36]
  wire [27:0] _GEN_358 = args_4_register == 4'h0 ? {{12'd0}, timeout} : _GEN_350; // @[Decoder.scala 542:60 Decoder.scala 91:24]
  wire [31:0] _GEN_359 = args_4_register == 4'h0 ? tracepoint : _GEN_351; // @[Decoder.scala 542:60 Decoder.scala 103:31]
  wire [31:0] _GEN_360 = args_4_register == 4'h0 ? _GEN_2 : _GEN_352; // @[Decoder.scala 542:60]
  wire [43:0] _GEN_362 = instruction_io_deq_valid ? _GEN_354 : {{12'd0}, dram0AddressOffset}; // @[Decoder.scala 536:29 Decoder.scala 121:36]
  wire [27:0] _GEN_363 = instruction_io_deq_valid ? _GEN_355 : {{24'd0}, dram0CacheBehaviour}; // @[Decoder.scala 536:29 Decoder.scala 122:36]
  wire [43:0] _GEN_364 = instruction_io_deq_valid ? _GEN_356 : {{12'd0}, dram1AddressOffset}; // @[Decoder.scala 536:29 Decoder.scala 123:36]
  wire [27:0] _GEN_365 = instruction_io_deq_valid ? _GEN_357 : {{24'd0}, dram1CacheBehaviour}; // @[Decoder.scala 536:29 Decoder.scala 124:36]
  wire [27:0] _GEN_366 = instruction_io_deq_valid ? _GEN_358 : {{12'd0}, timeout}; // @[Decoder.scala 536:29 Decoder.scala 91:24]
  wire [31:0] _GEN_367 = instruction_io_deq_valid ? _GEN_359 : tracepoint; // @[Decoder.scala 536:29 Decoder.scala 103:31]
  wire [31:0] _GEN_368 = instruction_io_deq_valid ? _GEN_360 : _GEN_2; // @[Decoder.scala 536:29]
  wire [43:0] _GEN_373 = instruction_io_deq_bits_opcode == 4'hf ? _GEN_362 : {{12'd0}, dram0AddressOffset}; // @[Decoder.scala 535:60 Decoder.scala 121:36]
  wire [27:0] _GEN_374 = instruction_io_deq_bits_opcode == 4'hf ? _GEN_363 : {{24'd0}, dram0CacheBehaviour}; // @[Decoder.scala 535:60 Decoder.scala 122:36]
  wire [43:0] _GEN_375 = instruction_io_deq_bits_opcode == 4'hf ? _GEN_364 : {{12'd0}, dram1AddressOffset}; // @[Decoder.scala 535:60 Decoder.scala 123:36]
  wire [27:0] _GEN_376 = instruction_io_deq_bits_opcode == 4'hf ? _GEN_365 : {{24'd0}, dram1CacheBehaviour}; // @[Decoder.scala 535:60 Decoder.scala 124:36]
  wire [27:0] _GEN_377 = instruction_io_deq_bits_opcode == 4'hf ? _GEN_366 : {{12'd0}, timeout}; // @[Decoder.scala 535:60 Decoder.scala 91:24]
  wire [31:0] _GEN_378 = instruction_io_deq_bits_opcode == 4'hf ? _GEN_367 : tracepoint; // @[Decoder.scala 535:60 Decoder.scala 103:31]
  wire [31:0] _GEN_379 = instruction_io_deq_bits_opcode == 4'hf ? _GEN_368 : _GEN_2; // @[Decoder.scala 535:60]
  wire  _GEN_384 = instruction_io_deq_bits_opcode == 4'h4 & instruction_io_deq_valid; // @[Decoder.scala 512:55 MultiEnqueue.scala 60:17 MultiEnqueue.scala 40:17]
  wire  _GEN_385 = instruction_io_deq_bits_opcode == 4'h4 & instruction_io_deq_ready_accHandler_io_in_w_ready; // @[Decoder.scala 512:55 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire  _GEN_389 = instruction_io_deq_bits_opcode == 4'h4 & flags_3_accumulate; // @[Decoder.scala 512:55 MultiEnqueue.scala 61:10 Decoder.scala 609:15]
  wire  _GEN_390 = instruction_io_deq_bits_opcode == 4'h4 & flags_zeroes; // @[Decoder.scala 512:55 MultiEnqueue.scala 61:10 Decoder.scala 609:15]
  wire  _GEN_391 = instruction_io_deq_bits_opcode == 4'h4 & flags_accumulate; // @[Decoder.scala 512:55 MultiEnqueue.scala 61:10 Decoder.scala 609:15]
  wire [5:0] instruction_io_deq_ready_w_32_altAddress = args_3_accWriteAddress[5:0]; // @[Decoder.scala 656:17 Decoder.scala 659:18]
  wire [5:0] _GEN_392 = instruction_io_deq_bits_opcode == 4'h4 ? instruction_io_deq_ready_w_32_altAddress : 6'h0; // @[Decoder.scala 512:55 MultiEnqueue.scala 61:10 Decoder.scala 609:15]
  wire [5:0] instruction_io_deq_ready_w_32_address = args_3_accReadAddress[5:0]; // @[Decoder.scala 656:17 Decoder.scala 658:15]
  wire [5:0] _GEN_393 = instruction_io_deq_bits_opcode == 4'h4 ? instruction_io_deq_ready_w_32_address : 6'h0; // @[Decoder.scala 512:55 MultiEnqueue.scala 61:10 Decoder.scala 609:15]
  wire  _GEN_394 = instruction_io_deq_bits_opcode == 4'h4 & args_3_instruction_dest; // @[Decoder.scala 512:55 MultiEnqueue.scala 61:10 Decoder.scala 609:15]
  wire  _GEN_395 = instruction_io_deq_bits_opcode == 4'h4 & args_3_instruction_sourceRight; // @[Decoder.scala 512:55 MultiEnqueue.scala 61:10 Decoder.scala 609:15]
  wire  _GEN_396 = instruction_io_deq_bits_opcode == 4'h4 & args_3_instruction_sourceLeft; // @[Decoder.scala 512:55 MultiEnqueue.scala 61:10 Decoder.scala 609:15]
  wire [3:0] _GEN_397 = instruction_io_deq_bits_opcode == 4'h4 ? args_3_instruction_op : 4'h0; // @[Decoder.scala 512:55 MultiEnqueue.scala 61:10 Decoder.scala 609:15]
  wire  _GEN_398 = instruction_io_deq_bits_opcode == 4'h4 & instruction_io_deq_ready_arrayHandler_io_in_w_2_valid; // @[Decoder.scala 512:55 MultiEnqueue.scala 61:10 Decoder.scala 610:16]
  wire  _GEN_399 = instruction_io_deq_bits_opcode == 4'h4 ? enqueuer1_io_in_ready : 1'h1; // @[Decoder.scala 512:55 Decoder.scala 521:23]
  wire [43:0] _GEN_400 = instruction_io_deq_bits_opcode == 4'h4 ? {{12'd0}, dram0AddressOffset} : _GEN_373; // @[Decoder.scala 512:55 Decoder.scala 121:36]
  wire [27:0] _GEN_401 = instruction_io_deq_bits_opcode == 4'h4 ? {{24'd0}, dram0CacheBehaviour} : _GEN_374; // @[Decoder.scala 512:55 Decoder.scala 122:36]
  wire [43:0] _GEN_402 = instruction_io_deq_bits_opcode == 4'h4 ? {{12'd0}, dram1AddressOffset} : _GEN_375; // @[Decoder.scala 512:55 Decoder.scala 123:36]
  wire [27:0] _GEN_403 = instruction_io_deq_bits_opcode == 4'h4 ? {{24'd0}, dram1CacheBehaviour} : _GEN_376; // @[Decoder.scala 512:55 Decoder.scala 124:36]
  wire [27:0] _GEN_404 = instruction_io_deq_bits_opcode == 4'h4 ? {{12'd0}, timeout} : _GEN_377; // @[Decoder.scala 512:55 Decoder.scala 91:24]
  wire [31:0] _GEN_405 = instruction_io_deq_bits_opcode == 4'h4 ? tracepoint : _GEN_378; // @[Decoder.scala 512:55 Decoder.scala 103:31]
  wire [31:0] _GEN_406 = instruction_io_deq_bits_opcode == 4'h4 ? _GEN_2 : _GEN_379; // @[Decoder.scala 512:55]
  wire  _GEN_410 = instruction_io_deq_bits_opcode == 4'h2 & _GEN_278; // @[Decoder.scala 329:59 MultiEnqueue.scala 40:17]
  wire  _GEN_411 = instruction_io_deq_bits_opcode == 4'h2 & _GEN_279; // @[Decoder.scala 329:59 MultiEnqueue.scala 42:18]
  wire [5:0] _GEN_412 = instruction_io_deq_bits_opcode == 4'h2 ? _GEN_280 : 6'h0; // @[Decoder.scala 329:59 Decoder.scala 609:15]
  wire [1:0] _GEN_413 = instruction_io_deq_bits_opcode == 4'h2 ? _GEN_281 : 2'h0; // @[Decoder.scala 329:59 Decoder.scala 609:15]
  wire  _GEN_414 = instruction_io_deq_bits_opcode == 4'h2 & _GEN_282; // @[Decoder.scala 329:59 Decoder.scala 610:16]
  wire  _GEN_415 = instruction_io_deq_bits_opcode == 4'h2 & _GEN_283; // @[Decoder.scala 329:59 MultiEnqueue.scala 42:18]
  wire [2:0] _GEN_417 = instruction_io_deq_bits_opcode == 4'h2 ? _GEN_285 : 3'h0; // @[Decoder.scala 329:59 Decoder.scala 609:15]
  wire [5:0] _GEN_419 = instruction_io_deq_bits_opcode == 4'h2 ? _GEN_287 : 6'h0; // @[Decoder.scala 329:59 Decoder.scala 609:15]
  wire  _GEN_420 = instruction_io_deq_bits_opcode == 4'h2 & _GEN_288; // @[Decoder.scala 329:59 Decoder.scala 609:15]
  wire  _GEN_421 = instruction_io_deq_bits_opcode == 4'h2 & _GEN_289; // @[Decoder.scala 329:59 Decoder.scala 610:16]
  wire  _GEN_422 = instruction_io_deq_bits_opcode == 4'h2 & _GEN_290; // @[Decoder.scala 329:59 MultiEnqueue.scala 42:18]
  wire [2:0] _GEN_424 = instruction_io_deq_bits_opcode == 4'h2 ? _GEN_292 : 3'h0; // @[Decoder.scala 329:59 Decoder.scala 609:15]
  wire [19:0] _GEN_425 = instruction_io_deq_bits_opcode == 4'h2 ? _GEN_293 : 20'h0; // @[Decoder.scala 329:59 Decoder.scala 609:15]
  wire [19:0] _GEN_426 = instruction_io_deq_bits_opcode == 4'h2 ? _GEN_294 : 20'h0; // @[Decoder.scala 329:59 Decoder.scala 609:15]
  wire  _GEN_427 = instruction_io_deq_bits_opcode == 4'h2 & _GEN_295; // @[Decoder.scala 329:59 Decoder.scala 609:15]
  wire  _GEN_428 = instruction_io_deq_bits_opcode == 4'h2 & _GEN_296; // @[Decoder.scala 329:59 Decoder.scala 610:16]
  wire  _GEN_429 = instruction_io_deq_bits_opcode == 4'h2 ? _GEN_297 : _GEN_399; // @[Decoder.scala 329:59]
  wire [2:0] _GEN_431 = instruction_io_deq_bits_opcode == 4'h2 ? _GEN_299 : 3'h0; // @[Decoder.scala 329:59 Decoder.scala 609:15]
  wire [19:0] _GEN_432 = instruction_io_deq_bits_opcode == 4'h2 ? _GEN_300 : 20'h0; // @[Decoder.scala 329:59 Decoder.scala 609:15]
  wire [19:0] _GEN_433 = instruction_io_deq_bits_opcode == 4'h2 ? _GEN_301 : 20'h0; // @[Decoder.scala 329:59 Decoder.scala 609:15]
  wire  _GEN_434 = instruction_io_deq_bits_opcode == 4'h2 & _GEN_302; // @[Decoder.scala 329:59 Decoder.scala 609:15]
  wire  _GEN_435 = instruction_io_deq_bits_opcode == 4'h2 & _GEN_303; // @[Decoder.scala 329:59 Decoder.scala 610:16]
  wire [5:0] _GEN_436 = instruction_io_deq_bits_opcode == 4'h2 ? _GEN_304 : 6'h0; // @[Decoder.scala 329:59 Decoder.scala 609:15]
  wire [3:0] _GEN_437 = instruction_io_deq_bits_opcode == 4'h2 ? _GEN_305 : 4'h0; // @[Decoder.scala 329:59 Decoder.scala 609:15]
  wire  _GEN_438 = instruction_io_deq_bits_opcode == 4'h2 & _GEN_306; // @[Decoder.scala 329:59 Decoder.scala 610:16]
  wire [2:0] _GEN_440 = instruction_io_deq_bits_opcode == 4'h2 ? _GEN_308 : 3'h0; // @[Decoder.scala 329:59 Decoder.scala 609:15]
  wire [5:0] _GEN_442 = instruction_io_deq_bits_opcode == 4'h2 ? _GEN_310 : 6'h0; // @[Decoder.scala 329:59 Decoder.scala 609:15]
  wire  _GEN_443 = instruction_io_deq_bits_opcode == 4'h2 & _GEN_311; // @[Decoder.scala 329:59 Decoder.scala 609:15]
  wire  _GEN_444 = instruction_io_deq_bits_opcode == 4'h2 & _GEN_312; // @[Decoder.scala 329:59 Decoder.scala 610:16]
  wire [2:0] _GEN_446 = instruction_io_deq_bits_opcode == 4'h2 ? _GEN_314 : 3'h0; // @[Decoder.scala 329:59]
  wire  _GEN_448 = instruction_io_deq_bits_opcode == 4'h2 ? _GEN_316 : _GEN_389; // @[Decoder.scala 329:59]
  wire  _GEN_449 = instruction_io_deq_bits_opcode == 4'h2 ? _GEN_317 : _GEN_390; // @[Decoder.scala 329:59]
  wire  _GEN_450 = instruction_io_deq_bits_opcode == 4'h2 ? _GEN_311 : _GEN_391; // @[Decoder.scala 329:59]
  wire [5:0] _GEN_451 = instruction_io_deq_bits_opcode == 4'h2 ? 6'h0 : _GEN_392; // @[Decoder.scala 329:59]
  wire [5:0] _GEN_452 = instruction_io_deq_bits_opcode == 4'h2 ? _GEN_320 : _GEN_393; // @[Decoder.scala 329:59]
  wire  _GEN_453 = instruction_io_deq_bits_opcode == 4'h2 ? 1'h0 : _GEN_394; // @[Decoder.scala 329:59]
  wire  _GEN_454 = instruction_io_deq_bits_opcode == 4'h2 ? 1'h0 : _GEN_395; // @[Decoder.scala 329:59]
  wire  _GEN_455 = instruction_io_deq_bits_opcode == 4'h2 ? 1'h0 : _GEN_396; // @[Decoder.scala 329:59]
  wire [3:0] _GEN_456 = instruction_io_deq_bits_opcode == 4'h2 ? 4'h0 : _GEN_397; // @[Decoder.scala 329:59]
  wire  _GEN_457 = instruction_io_deq_bits_opcode == 4'h2 ? _GEN_325 : _GEN_398; // @[Decoder.scala 329:59]
  wire  _GEN_458 = instruction_io_deq_bits_opcode == 4'h2 ? 1'h0 : _GEN_384; // @[Decoder.scala 329:59 MultiEnqueue.scala 40:17]
  wire  _GEN_459 = instruction_io_deq_bits_opcode == 4'h2 ? 1'h0 : _GEN_385; // @[Decoder.scala 329:59 MultiEnqueue.scala 42:18]
  wire [43:0] _GEN_460 = instruction_io_deq_bits_opcode == 4'h2 ? {{12'd0}, dram0AddressOffset} : _GEN_400; // @[Decoder.scala 329:59 Decoder.scala 121:36]
  wire [27:0] _GEN_461 = instruction_io_deq_bits_opcode == 4'h2 ? {{24'd0}, dram0CacheBehaviour} : _GEN_401; // @[Decoder.scala 329:59 Decoder.scala 122:36]
  wire [43:0] _GEN_462 = instruction_io_deq_bits_opcode == 4'h2 ? {{12'd0}, dram1AddressOffset} : _GEN_402; // @[Decoder.scala 329:59 Decoder.scala 123:36]
  wire [27:0] _GEN_463 = instruction_io_deq_bits_opcode == 4'h2 ? {{24'd0}, dram1CacheBehaviour} : _GEN_403; // @[Decoder.scala 329:59 Decoder.scala 124:36]
  wire [27:0] _GEN_464 = instruction_io_deq_bits_opcode == 4'h2 ? {{12'd0}, timeout} : _GEN_404; // @[Decoder.scala 329:59 Decoder.scala 91:24]
  wire  _GEN_470 = instruction_io_deq_bits_opcode == 4'h3 ? _GEN_45 : _GEN_458; // @[Decoder.scala 292:62]
  wire  _GEN_471 = instruction_io_deq_bits_opcode == 4'h3 ? _GEN_46 : _GEN_459; // @[Decoder.scala 292:62]
  wire [5:0] _GEN_472 = instruction_io_deq_bits_opcode == 4'h3 ? _GEN_47 : 6'h0; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire  _GEN_473 = instruction_io_deq_bits_opcode == 4'h3 & flags_accumulate; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire  _GEN_475 = instruction_io_deq_bits_opcode == 4'h3 & _GEN_50; // @[Decoder.scala 292:62 Decoder.scala 610:16]
  wire  _GEN_476 = instruction_io_deq_bits_opcode == 4'h3 ? _GEN_51 : _GEN_429; // @[Decoder.scala 292:62]
  wire  _GEN_477 = instruction_io_deq_bits_opcode == 4'h3 ? _GEN_52 : _GEN_410; // @[Decoder.scala 292:62]
  wire  _GEN_478 = instruction_io_deq_bits_opcode == 4'h3 ? _GEN_53 : _GEN_411; // @[Decoder.scala 292:62]
  wire [5:0] _GEN_479 = instruction_io_deq_bits_opcode == 4'h3 ? _GEN_54 : _GEN_436; // @[Decoder.scala 292:62]
  wire [3:0] _GEN_480 = instruction_io_deq_bits_opcode == 4'h3 ? _GEN_55 : _GEN_437; // @[Decoder.scala 292:62]
  wire  _GEN_481 = instruction_io_deq_bits_opcode == 4'h3 ? _GEN_56 : _GEN_438; // @[Decoder.scala 292:62]
  wire  _GEN_482 = instruction_io_deq_bits_opcode == 4'h3 ? _GEN_57 : _GEN_415; // @[Decoder.scala 292:62]
  wire  _GEN_483 = instruction_io_deq_bits_opcode == 4'h3 ? _GEN_58 : _GEN_422; // @[Decoder.scala 292:62]
  wire  _GEN_484 = instruction_io_deq_bits_opcode == 4'h3 & _GEN_59; // @[Decoder.scala 292:62]
  wire [2:0] _GEN_485 = instruction_io_deq_bits_opcode == 4'h3 ? _GEN_60 : _GEN_440; // @[Decoder.scala 292:62]
  wire [5:0] _GEN_487 = instruction_io_deq_bits_opcode == 4'h3 ? _GEN_62 : _GEN_442; // @[Decoder.scala 292:62]
  wire  _GEN_488 = instruction_io_deq_bits_opcode == 4'h3 ? 1'h0 : _GEN_443; // @[Decoder.scala 292:62]
  wire  _GEN_489 = instruction_io_deq_bits_opcode == 4'h3 ? _GEN_64 : _GEN_444; // @[Decoder.scala 292:62]
  wire [5:0] _GEN_490 = instruction_io_deq_bits_opcode == 4'h3 ? 6'h0 : _GEN_412; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire [1:0] _GEN_491 = instruction_io_deq_bits_opcode == 4'h3 ? 2'h0 : _GEN_413; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire  _GEN_492 = instruction_io_deq_bits_opcode == 4'h3 ? 1'h0 : _GEN_414; // @[Decoder.scala 292:62 Decoder.scala 610:16]
  wire [2:0] _GEN_494 = instruction_io_deq_bits_opcode == 4'h3 ? 3'h0 : _GEN_417; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire [5:0] _GEN_496 = instruction_io_deq_bits_opcode == 4'h3 ? 6'h0 : _GEN_419; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire  _GEN_497 = instruction_io_deq_bits_opcode == 4'h3 ? 1'h0 : _GEN_420; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire  _GEN_498 = instruction_io_deq_bits_opcode == 4'h3 ? 1'h0 : _GEN_421; // @[Decoder.scala 292:62 Decoder.scala 610:16]
  wire [2:0] _GEN_500 = instruction_io_deq_bits_opcode == 4'h3 ? 3'h0 : _GEN_424; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire [19:0] _GEN_501 = instruction_io_deq_bits_opcode == 4'h3 ? 20'h0 : _GEN_425; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire [19:0] _GEN_502 = instruction_io_deq_bits_opcode == 4'h3 ? 20'h0 : _GEN_426; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire  _GEN_503 = instruction_io_deq_bits_opcode == 4'h3 ? 1'h0 : _GEN_427; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire  _GEN_504 = instruction_io_deq_bits_opcode == 4'h3 ? 1'h0 : _GEN_428; // @[Decoder.scala 292:62 Decoder.scala 610:16]
  wire [2:0] _GEN_506 = instruction_io_deq_bits_opcode == 4'h3 ? 3'h0 : _GEN_431; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire [19:0] _GEN_507 = instruction_io_deq_bits_opcode == 4'h3 ? 20'h0 : _GEN_432; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire [19:0] _GEN_508 = instruction_io_deq_bits_opcode == 4'h3 ? 20'h0 : _GEN_433; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire  _GEN_509 = instruction_io_deq_bits_opcode == 4'h3 ? 1'h0 : _GEN_434; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire  _GEN_510 = instruction_io_deq_bits_opcode == 4'h3 ? 1'h0 : _GEN_435; // @[Decoder.scala 292:62 Decoder.scala 610:16]
  wire [2:0] _GEN_512 = instruction_io_deq_bits_opcode == 4'h3 ? 3'h0 : _GEN_446; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire [5:0] _GEN_513 = instruction_io_deq_bits_opcode == 4'h3 ? 6'h0 : _GEN_436; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire  _GEN_514 = instruction_io_deq_bits_opcode == 4'h3 ? 1'h0 : _GEN_448; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire  _GEN_515 = instruction_io_deq_bits_opcode == 4'h3 ? 1'h0 : _GEN_449; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire  _GEN_516 = instruction_io_deq_bits_opcode == 4'h3 ? 1'h0 : _GEN_450; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire [5:0] _GEN_517 = instruction_io_deq_bits_opcode == 4'h3 ? 6'h0 : _GEN_451; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire [5:0] _GEN_518 = instruction_io_deq_bits_opcode == 4'h3 ? 6'h0 : _GEN_452; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire  _GEN_519 = instruction_io_deq_bits_opcode == 4'h3 ? 1'h0 : _GEN_453; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire  _GEN_520 = instruction_io_deq_bits_opcode == 4'h3 ? 1'h0 : _GEN_454; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire  _GEN_521 = instruction_io_deq_bits_opcode == 4'h3 ? 1'h0 : _GEN_455; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire [3:0] _GEN_522 = instruction_io_deq_bits_opcode == 4'h3 ? 4'h0 : _GEN_456; // @[Decoder.scala 292:62 Decoder.scala 609:15]
  wire  _GEN_523 = instruction_io_deq_bits_opcode == 4'h3 ? 1'h0 : _GEN_457; // @[Decoder.scala 292:62 Decoder.scala 610:16]
  wire [43:0] _GEN_524 = instruction_io_deq_bits_opcode == 4'h3 ? {{12'd0}, dram0AddressOffset} : _GEN_460; // @[Decoder.scala 292:62 Decoder.scala 121:36]
  wire [27:0] _GEN_525 = instruction_io_deq_bits_opcode == 4'h3 ? {{24'd0}, dram0CacheBehaviour} : _GEN_461; // @[Decoder.scala 292:62 Decoder.scala 122:36]
  wire [43:0] _GEN_526 = instruction_io_deq_bits_opcode == 4'h3 ? {{12'd0}, dram1AddressOffset} : _GEN_462; // @[Decoder.scala 292:62 Decoder.scala 123:36]
  wire [27:0] _GEN_527 = instruction_io_deq_bits_opcode == 4'h3 ? {{24'd0}, dram1CacheBehaviour} : _GEN_463; // @[Decoder.scala 292:62 Decoder.scala 124:36]
  wire [27:0] _GEN_528 = instruction_io_deq_bits_opcode == 4'h3 ? {{12'd0}, timeout} : _GEN_464; // @[Decoder.scala 292:62 Decoder.scala 91:24]
  wire [43:0] _GEN_593 = instruction_io_deq_bits_opcode == 4'h1 ? {{12'd0}, dram0AddressOffset} : _GEN_524; // @[Decoder.scala 244:51 Decoder.scala 121:36]
  wire [27:0] _GEN_594 = instruction_io_deq_bits_opcode == 4'h1 ? {{24'd0}, dram0CacheBehaviour} : _GEN_525; // @[Decoder.scala 244:51 Decoder.scala 122:36]
  wire [43:0] _GEN_595 = instruction_io_deq_bits_opcode == 4'h1 ? {{12'd0}, dram1AddressOffset} : _GEN_526; // @[Decoder.scala 244:51 Decoder.scala 123:36]
  wire [27:0] _GEN_596 = instruction_io_deq_bits_opcode == 4'h1 ? {{24'd0}, dram1CacheBehaviour} : _GEN_527; // @[Decoder.scala 244:51 Decoder.scala 124:36]
  wire [27:0] _GEN_597 = instruction_io_deq_bits_opcode == 4'h1 ? {{12'd0}, timeout} : _GEN_528; // @[Decoder.scala 244:51 Decoder.scala 91:24]
  Queue instruction ( // @[Decoupled.scala 296:21]
    .clock(instruction_clock),
    .reset(instruction_reset),
    .io_enq_ready(instruction_io_enq_ready),
    .io_enq_valid(instruction_io_enq_valid),
    .io_enq_bits_opcode(instruction_io_enq_bits_opcode),
    .io_enq_bits_flags(instruction_io_enq_bits_flags),
    .io_enq_bits_arguments(instruction_io_enq_bits_arguments),
    .io_deq_ready(instruction_io_deq_ready),
    .io_deq_valid(instruction_io_deq_valid),
    .io_deq_bits_opcode(instruction_io_deq_bits_opcode),
    .io_deq_bits_flags(instruction_io_deq_bits_flags),
    .io_deq_bits_arguments(instruction_io_deq_bits_arguments)
  );
  Queue_1 status ( // @[Decoder.scala 78:23]
    .clock(status_clock),
    .reset(status_reset),
    .io_enq_ready(status_io_enq_ready),
    .io_enq_valid(status_io_enq_valid),
    .io_enq_bits_bits_opcode(status_io_enq_bits_bits_opcode),
    .io_enq_bits_bits_flags(status_io_enq_bits_bits_flags),
    .io_enq_bits_bits_arguments(status_io_enq_bits_bits_arguments),
    .io_deq_ready(status_io_deq_ready),
    .io_deq_valid(status_io_deq_valid),
    .io_deq_bits_last(status_io_deq_bits_last),
    .io_deq_bits_bits_opcode(status_io_deq_bits_bits_opcode),
    .io_deq_bits_bits_flags(status_io_deq_bits_bits_flags),
    .io_deq_bits_bits_arguments(status_io_deq_bits_bits_arguments)
  );
  StrideHandler dram0Handler ( // @[Decoder.scala 135:28]
    .clock(dram0Handler_clock),
    .reset(dram0Handler_reset),
    .io_in_ready(dram0Handler_io_in_ready),
    .io_in_valid(dram0Handler_io_in_valid),
    .io_in_bits_write(dram0Handler_io_in_bits_write),
    .io_in_bits_address(dram0Handler_io_in_bits_address),
    .io_in_bits_size(dram0Handler_io_in_bits_size),
    .io_in_bits_stride(dram0Handler_io_in_bits_stride),
    .io_out_ready(dram0Handler_io_out_ready),
    .io_out_valid(dram0Handler_io_out_valid),
    .io_out_bits_write(dram0Handler_io_out_bits_write),
    .io_out_bits_address(dram0Handler_io_out_bits_address),
    .io_out_bits_size(dram0Handler_io_out_bits_size)
  );
  StrideHandler dram1Handler ( // @[Decoder.scala 144:28]
    .clock(dram1Handler_clock),
    .reset(dram1Handler_reset),
    .io_in_ready(dram1Handler_io_in_ready),
    .io_in_valid(dram1Handler_io_in_valid),
    .io_in_bits_write(dram1Handler_io_in_bits_write),
    .io_in_bits_address(dram1Handler_io_in_bits_address),
    .io_in_bits_size(dram1Handler_io_in_bits_size),
    .io_in_bits_stride(dram1Handler_io_in_bits_stride),
    .io_out_ready(dram1Handler_io_out_ready),
    .io_out_valid(dram1Handler_io_out_valid),
    .io_out_bits_write(dram1Handler_io_out_bits_write),
    .io_out_bits_address(dram1Handler_io_out_bits_address),
    .io_out_bits_size(dram1Handler_io_out_bits_size)
  );
  SizeAndStrideHandler_2 memPortAHandler ( // @[Decoder.scala 160:31]
    .clock(memPortAHandler_clock),
    .reset(memPortAHandler_reset),
    .io_in_ready(memPortAHandler_io_in_ready),
    .io_in_valid(memPortAHandler_io_in_valid),
    .io_in_bits_write(memPortAHandler_io_in_bits_write),
    .io_in_bits_address(memPortAHandler_io_in_bits_address),
    .io_in_bits_size(memPortAHandler_io_in_bits_size),
    .io_in_bits_stride(memPortAHandler_io_in_bits_stride),
    .io_in_bits_reverse(memPortAHandler_io_in_bits_reverse),
    .io_out_ready(memPortAHandler_io_out_ready),
    .io_out_valid(memPortAHandler_io_out_valid),
    .io_out_bits_write(memPortAHandler_io_out_bits_write),
    .io_out_bits_address(memPortAHandler_io_out_bits_address)
  );
  SizeAndStrideHandler_2 memPortBHandler ( // @[Decoder.scala 169:31]
    .clock(memPortBHandler_clock),
    .reset(memPortBHandler_reset),
    .io_in_ready(memPortBHandler_io_in_ready),
    .io_in_valid(memPortBHandler_io_in_valid),
    .io_in_bits_write(memPortBHandler_io_in_bits_write),
    .io_in_bits_address(memPortBHandler_io_in_bits_address),
    .io_in_bits_size(memPortBHandler_io_in_bits_size),
    .io_in_bits_stride(memPortBHandler_io_in_bits_stride),
    .io_in_bits_reverse(memPortBHandler_io_in_bits_reverse),
    .io_out_ready(memPortBHandler_io_out_ready),
    .io_out_valid(memPortBHandler_io_out_valid),
    .io_out_bits_write(memPortBHandler_io_out_bits_write),
    .io_out_bits_address(memPortBHandler_io_out_bits_address)
  );
  Queue_2 memPortA ( // @[Mem.scala 23:19]
    .clock(memPortA_clock),
    .reset(memPortA_reset),
    .io_enq_ready(memPortA_io_enq_ready),
    .io_enq_valid(memPortA_io_enq_valid),
    .io_enq_bits_write(memPortA_io_enq_bits_write),
    .io_enq_bits_address(memPortA_io_enq_bits_address),
    .io_enq_bits_size(memPortA_io_enq_bits_size),
    .io_enq_bits_stride(memPortA_io_enq_bits_stride),
    .io_enq_bits_reverse(memPortA_io_enq_bits_reverse),
    .io_deq_ready(memPortA_io_deq_ready),
    .io_deq_valid(memPortA_io_deq_valid),
    .io_deq_bits_write(memPortA_io_deq_bits_write),
    .io_deq_bits_address(memPortA_io_deq_bits_address),
    .io_deq_bits_size(memPortA_io_deq_bits_size),
    .io_deq_bits_stride(memPortA_io_deq_bits_stride),
    .io_deq_bits_reverse(memPortA_io_deq_bits_reverse)
  );
  Queue_2 memPortB ( // @[Mem.scala 23:19]
    .clock(memPortB_clock),
    .reset(memPortB_reset),
    .io_enq_ready(memPortB_io_enq_ready),
    .io_enq_valid(memPortB_io_enq_valid),
    .io_enq_bits_write(memPortB_io_enq_bits_write),
    .io_enq_bits_address(memPortB_io_enq_bits_address),
    .io_enq_bits_size(memPortB_io_enq_bits_size),
    .io_enq_bits_stride(memPortB_io_enq_bits_stride),
    .io_enq_bits_reverse(memPortB_io_enq_bits_reverse),
    .io_deq_ready(memPortB_io_deq_ready),
    .io_deq_valid(memPortB_io_deq_valid),
    .io_deq_bits_write(memPortB_io_deq_bits_write),
    .io_deq_bits_address(memPortB_io_deq_bits_address),
    .io_deq_bits_size(memPortB_io_deq_bits_size),
    .io_deq_bits_stride(memPortB_io_deq_bits_stride),
    .io_deq_bits_reverse(memPortB_io_deq_bits_reverse)
  );
  SizeAndStrideHandler_4 accHandler ( // @[Decoder.scala 185:26]
    .clock(accHandler_clock),
    .reset(accHandler_reset),
    .io_in_ready(accHandler_io_in_ready),
    .io_in_valid(accHandler_io_in_valid),
    .io_in_bits_instruction_op(accHandler_io_in_bits_instruction_op),
    .io_in_bits_instruction_sourceLeft(accHandler_io_in_bits_instruction_sourceLeft),
    .io_in_bits_instruction_sourceRight(accHandler_io_in_bits_instruction_sourceRight),
    .io_in_bits_instruction_dest(accHandler_io_in_bits_instruction_dest),
    .io_in_bits_address(accHandler_io_in_bits_address),
    .io_in_bits_altAddress(accHandler_io_in_bits_altAddress),
    .io_in_bits_read(accHandler_io_in_bits_read),
    .io_in_bits_write(accHandler_io_in_bits_write),
    .io_in_bits_accumulate(accHandler_io_in_bits_accumulate),
    .io_in_bits_size(accHandler_io_in_bits_size),
    .io_in_bits_stride(accHandler_io_in_bits_stride),
    .io_out_ready(accHandler_io_out_ready),
    .io_out_valid(accHandler_io_out_valid),
    .io_out_bits_instruction_op(accHandler_io_out_bits_instruction_op),
    .io_out_bits_instruction_sourceLeft(accHandler_io_out_bits_instruction_sourceLeft),
    .io_out_bits_instruction_sourceRight(accHandler_io_out_bits_instruction_sourceRight),
    .io_out_bits_instruction_dest(accHandler_io_out_bits_instruction_dest),
    .io_out_bits_address(accHandler_io_out_bits_address),
    .io_out_bits_altAddress(accHandler_io_out_bits_altAddress),
    .io_out_bits_read(accHandler_io_out_bits_read),
    .io_out_bits_write(accHandler_io_out_bits_write),
    .io_out_bits_accumulate(accHandler_io_out_bits_accumulate)
  );
  SizeHandler arrayHandler ( // @[Decoder.scala 202:28]
    .clock(arrayHandler_clock),
    .reset(arrayHandler_reset),
    .io_in_ready(arrayHandler_io_in_ready),
    .io_in_valid(arrayHandler_io_in_valid),
    .io_in_bits_load(arrayHandler_io_in_bits_load),
    .io_in_bits_zeroes(arrayHandler_io_in_bits_zeroes),
    .io_in_bits_size(arrayHandler_io_in_bits_size),
    .io_out_ready(arrayHandler_io_out_ready),
    .io_out_valid(arrayHandler_io_out_valid),
    .io_out_bits_load(arrayHandler_io_out_bits_load),
    .io_out_bits_zeroes(arrayHandler_io_out_bits_zeroes)
  );
  SizeHandler_1 hostDataflowHandler ( // @[Decoder.scala 216:35]
    .clock(hostDataflowHandler_clock),
    .reset(hostDataflowHandler_reset),
    .io_in_ready(hostDataflowHandler_io_in_ready),
    .io_in_valid(hostDataflowHandler_io_in_valid),
    .io_in_bits_kind(hostDataflowHandler_io_in_bits_kind),
    .io_in_bits_size(hostDataflowHandler_io_in_bits_size),
    .io_out_ready(hostDataflowHandler_io_out_ready),
    .io_out_valid(hostDataflowHandler_io_out_valid),
    .io_out_bits_kind(hostDataflowHandler_io_out_bits_kind)
  );
  MultiEnqueue enqueuer1 ( // @[MultiEnqueue.scala 160:43]
    .clock(enqueuer1_clock),
    .reset(enqueuer1_reset),
    .io_in_ready(enqueuer1_io_in_ready),
    .io_in_valid(enqueuer1_io_in_valid),
    .io_out_0_ready(enqueuer1_io_out_0_ready),
    .io_out_0_valid(enqueuer1_io_out_0_valid)
  );
  MultiEnqueue_1 enqueuer2 ( // @[MultiEnqueue.scala 160:43]
    .clock(enqueuer2_clock),
    .reset(enqueuer2_reset),
    .io_in_ready(enqueuer2_io_in_ready),
    .io_in_valid(enqueuer2_io_in_valid),
    .io_out_0_ready(enqueuer2_io_out_0_ready),
    .io_out_0_valid(enqueuer2_io_out_0_valid),
    .io_out_1_ready(enqueuer2_io_out_1_ready),
    .io_out_1_valid(enqueuer2_io_out_1_valid)
  );
  MultiEnqueue_2 enqueuer3 ( // @[MultiEnqueue.scala 160:43]
    .clock(enqueuer3_clock),
    .reset(enqueuer3_reset),
    .io_in_ready(enqueuer3_io_in_ready),
    .io_in_valid(enqueuer3_io_in_valid),
    .io_out_0_ready(enqueuer3_io_out_0_ready),
    .io_out_0_valid(enqueuer3_io_out_0_valid),
    .io_out_1_ready(enqueuer3_io_out_1_ready),
    .io_out_1_valid(enqueuer3_io_out_1_valid),
    .io_out_2_ready(enqueuer3_io_out_2_ready),
    .io_out_2_valid(enqueuer3_io_out_2_valid)
  );
  MultiEnqueue_3 enqueuer4 ( // @[MultiEnqueue.scala 160:43]
    .clock(enqueuer4_clock),
    .reset(enqueuer4_reset),
    .io_in_ready(enqueuer4_io_in_ready),
    .io_in_valid(enqueuer4_io_in_valid),
    .io_out_0_ready(enqueuer4_io_out_0_ready),
    .io_out_0_valid(enqueuer4_io_out_0_valid),
    .io_out_1_ready(enqueuer4_io_out_1_ready),
    .io_out_1_valid(enqueuer4_io_out_1_valid),
    .io_out_2_ready(enqueuer4_io_out_2_ready),
    .io_out_2_valid(enqueuer4_io_out_2_valid),
    .io_out_3_ready(enqueuer4_io_out_3_ready),
    .io_out_3_valid(enqueuer4_io_out_3_valid)
  );
  Validator validator ( // @[Decoder.scala 571:27]
    .io_instruction_valid(validator_io_instruction_valid),
    .io_instruction_bits_opcode(validator_io_instruction_bits_opcode),
    .io_instruction_bits_flags(validator_io_instruction_bits_flags),
    .io_instruction_bits_arguments(validator_io_instruction_bits_arguments),
    .io_error(validator_io_error)
  );
  assign io_instruction_ready = instruction_io_enq_ready; // @[Decoupled.scala 299:17]
  assign io_memPortA_valid = memPortAHandler_io_out_valid; // @[Decoder.scala 178:15]
  assign io_memPortA_bits_write = memPortAHandler_io_out_bits_write; // @[Decoder.scala 178:15]
  assign io_memPortA_bits_address = memPortAHandler_io_out_bits_address; // @[Decoder.scala 178:15]
  assign io_memPortB_valid = memPortBHandler_io_out_valid; // @[Decoder.scala 179:15]
  assign io_memPortB_bits_write = memPortBHandler_io_out_bits_write; // @[Decoder.scala 179:15]
  assign io_memPortB_bits_address = memPortBHandler_io_out_bits_address; // @[Decoder.scala 179:15]
  assign io_dram0_valid = dram0Handler_io_out_valid; // @[Decoder.scala 153:12]
  assign io_dram0_bits_write = dram0Handler_io_out_bits_write; // @[Decoder.scala 153:12]
  assign io_dram0_bits_address = dram0Handler_io_out_bits_address; // @[Decoder.scala 153:12]
  assign io_dram0_bits_size = dram0Handler_io_out_bits_size; // @[Decoder.scala 153:12]
  assign io_dram1_valid = dram1Handler_io_out_valid; // @[Decoder.scala 154:12]
  assign io_dram1_bits_write = dram1Handler_io_out_bits_write; // @[Decoder.scala 154:12]
  assign io_dram1_bits_address = dram1Handler_io_out_bits_address; // @[Decoder.scala 154:12]
  assign io_dram1_bits_size = dram1Handler_io_out_bits_size; // @[Decoder.scala 154:12]
  assign io_dataflow_valid = instruction_io_deq_bits_opcode == 4'h1 ? _GEN_13 : _GEN_481; // @[Decoder.scala 244:51]
  assign io_dataflow_bits_kind = instruction_io_deq_bits_opcode == 4'h1 ? _GEN_12 : _GEN_480; // @[Decoder.scala 244:51]
  assign io_dataflow_bits_size = instruction_io_deq_bits_opcode == 4'h1 ? _GEN_11 : _GEN_479; // @[Decoder.scala 244:51]
  assign io_hostDataflow_valid = hostDataflowHandler_io_out_valid; // @[Decoder.scala 223:19]
  assign io_hostDataflow_bits_kind = hostDataflowHandler_io_out_bits_kind; // @[Decoder.scala 223:19]
  assign io_acc_valid = accHandler_io_out_valid; // @[Decoder.scala 196:16]
  assign io_acc_bits_instruction_op = accHandler_io_out_bits_instruction_op; // @[AccumulatorWithALUArrayControl.scala 96:17 AccumulatorWithALUArrayControl.scala 102:19]
  assign io_acc_bits_instruction_sourceLeft = accHandler_io_out_bits_instruction_sourceLeft; // @[AccumulatorWithALUArrayControl.scala 96:17 AccumulatorWithALUArrayControl.scala 102:19]
  assign io_acc_bits_instruction_sourceRight = accHandler_io_out_bits_instruction_sourceRight; // @[AccumulatorWithALUArrayControl.scala 96:17 AccumulatorWithALUArrayControl.scala 102:19]
  assign io_acc_bits_instruction_dest = accHandler_io_out_bits_instruction_dest; // @[AccumulatorWithALUArrayControl.scala 96:17 AccumulatorWithALUArrayControl.scala 102:19]
  assign io_acc_bits_readAddress = io_acc_bits_isMemControl ? _GEN_5 : accHandler_io_out_bits_address; // @[AccumulatorWithALUArrayControl.scala 106:24 AccumulatorWithALUArrayControl.scala 120:21]
  assign io_acc_bits_writeAddress = io_acc_bits_isMemControl ? _GEN_6 : accHandler_io_out_bits_altAddress; // @[AccumulatorWithALUArrayControl.scala 106:24 AccumulatorWithALUArrayControl.scala 121:22]
  assign io_acc_bits_accumulate = accHandler_io_out_bits_accumulate; // @[AccumulatorWithALUArrayControl.scala 96:17 AccumulatorWithALUArrayControl.scala 105:18]
  assign io_acc_bits_write = accHandler_io_out_bits_write; // @[AccumulatorWithALUArrayControl.scala 96:17 AccumulatorWithALUArrayControl.scala 104:13]
  assign io_acc_bits_read = accHandler_io_out_bits_read; // @[AccumulatorWithALUArrayControl.scala 96:17 AccumulatorWithALUArrayControl.scala 103:12]
  assign io_array_valid = arrayHandler_io_out_valid; // @[Decoder.scala 211:12]
  assign io_array_bits_load = arrayHandler_io_out_bits_load; // @[Decoder.scala 211:12]
  assign io_array_bits_zeroes = arrayHandler_io_out_bits_zeroes; // @[Decoder.scala 211:12]
  assign io_config_dram0AddressOffset = dram0AddressOffset; // @[Decoder.scala 126:32]
  assign io_config_dram0CacheBehaviour = dram0CacheBehaviour; // @[Decoder.scala 127:33]
  assign io_config_dram1AddressOffset = dram1AddressOffset; // @[Decoder.scala 128:32]
  assign io_config_dram1CacheBehaviour = dram1CacheBehaviour; // @[Decoder.scala 129:33]
  assign io_status_valid = status_io_deq_valid; // @[Decoder.scala 88:13]
  assign io_status_bits_last = status_io_deq_bits_last; // @[Decoder.scala 88:13]
  assign io_status_bits_bits_opcode = status_io_deq_bits_bits_opcode; // @[Decoder.scala 88:13]
  assign io_status_bits_bits_flags = status_io_deq_bits_bits_flags; // @[Decoder.scala 88:13]
  assign io_status_bits_bits_arguments = status_io_deq_bits_bits_arguments; // @[Decoder.scala 88:13]
  assign io_timeout = timer == timeout; // @[Decoder.scala 100:23]
  assign io_error = validator_io_error; // @[Decoder.scala 574:14]
  assign io_tracepoint = programCounter == tracepoint; // @[Decoder.scala 108:35]
  assign io_programCounter = programCounter; // @[Decoder.scala 109:21]
  assign instruction_clock = clock;
  assign instruction_reset = reset;
  assign instruction_io_enq_valid = io_instruction_valid; // @[Decoupled.scala 297:22]
  assign instruction_io_enq_bits_opcode = io_instruction_bits_opcode; // @[Decoupled.scala 298:21]
  assign instruction_io_enq_bits_flags = io_instruction_bits_flags; // @[Decoupled.scala 298:21]
  assign instruction_io_enq_bits_arguments = io_instruction_bits_arguments; // @[Decoupled.scala 298:21]
  assign instruction_io_deq_ready = instruction_io_deq_bits_opcode == 4'h1 ? _GEN_33 : _GEN_476; // @[Decoder.scala 244:51]
  assign status_clock = clock;
  assign status_reset = reset;
  assign status_io_enq_valid = instruction_io_deq_valid & instruction_io_deq_ready; // @[Decoder.scala 85:44]
  assign status_io_enq_bits_bits_opcode = instruction_io_deq_bits_opcode; // @[Decoder.scala 86:27]
  assign status_io_enq_bits_bits_flags = instruction_io_deq_bits_flags; // @[Decoder.scala 86:27]
  assign status_io_enq_bits_bits_arguments = instruction_io_deq_bits_arguments; // @[Decoder.scala 86:27]
  assign status_io_deq_ready = io_status_ready; // @[Decoder.scala 88:13]
  assign dram0Handler_clock = clock;
  assign dram0Handler_reset = reset;
  assign dram0Handler_io_in_valid = instruction_io_deq_bits_opcode == 4'h1 ? 1'h0 : _GEN_504; // @[Decoder.scala 244:51 Decoder.scala 610:16]
  assign dram0Handler_io_in_bits_write = instruction_io_deq_bits_opcode == 4'h1 ? 1'h0 : _GEN_503; // @[Decoder.scala 244:51 Decoder.scala 609:15]
  assign dram0Handler_io_in_bits_address = instruction_io_deq_bits_opcode == 4'h1 ? 20'h0 : _GEN_502; // @[Decoder.scala 244:51 Decoder.scala 609:15]
  assign dram0Handler_io_in_bits_size = instruction_io_deq_bits_opcode == 4'h1 ? 20'h0 : _GEN_501; // @[Decoder.scala 244:51 Decoder.scala 609:15]
  assign dram0Handler_io_in_bits_stride = instruction_io_deq_bits_opcode == 4'h1 ? 3'h0 : _GEN_500; // @[Decoder.scala 244:51 Decoder.scala 609:15]
  assign dram0Handler_io_out_ready = io_dram0_ready; // @[Decoder.scala 153:12]
  assign dram1Handler_clock = clock;
  assign dram1Handler_reset = reset;
  assign dram1Handler_io_in_valid = instruction_io_deq_bits_opcode == 4'h1 ? 1'h0 : _GEN_510; // @[Decoder.scala 244:51 Decoder.scala 610:16]
  assign dram1Handler_io_in_bits_write = instruction_io_deq_bits_opcode == 4'h1 ? 1'h0 : _GEN_509; // @[Decoder.scala 244:51 Decoder.scala 609:15]
  assign dram1Handler_io_in_bits_address = instruction_io_deq_bits_opcode == 4'h1 ? 20'h0 : _GEN_508; // @[Decoder.scala 244:51 Decoder.scala 609:15]
  assign dram1Handler_io_in_bits_size = instruction_io_deq_bits_opcode == 4'h1 ? 20'h0 : _GEN_507; // @[Decoder.scala 244:51 Decoder.scala 609:15]
  assign dram1Handler_io_in_bits_stride = instruction_io_deq_bits_opcode == 4'h1 ? 3'h0 : _GEN_506; // @[Decoder.scala 244:51 Decoder.scala 609:15]
  assign dram1Handler_io_out_ready = io_dram1_ready; // @[Decoder.scala 154:12]
  assign memPortAHandler_clock = clock;
  assign memPortAHandler_reset = reset;
  assign memPortAHandler_io_in_valid = memPortA_io_deq_valid; // @[Mem.scala 24:7]
  assign memPortAHandler_io_in_bits_write = memPortA_io_deq_bits_write; // @[Mem.scala 24:7]
  assign memPortAHandler_io_in_bits_address = memPortA_io_deq_bits_address; // @[Mem.scala 24:7]
  assign memPortAHandler_io_in_bits_size = memPortA_io_deq_bits_size; // @[Mem.scala 24:7]
  assign memPortAHandler_io_in_bits_stride = memPortA_io_deq_bits_stride; // @[Mem.scala 24:7]
  assign memPortAHandler_io_in_bits_reverse = memPortA_io_deq_bits_reverse; // @[Mem.scala 24:7]
  assign memPortAHandler_io_out_ready = io_memPortA_ready; // @[Decoder.scala 178:15]
  assign memPortBHandler_clock = clock;
  assign memPortBHandler_reset = reset;
  assign memPortBHandler_io_in_valid = memPortB_io_deq_valid; // @[Mem.scala 24:7]
  assign memPortBHandler_io_in_bits_write = memPortB_io_deq_bits_write; // @[Mem.scala 24:7]
  assign memPortBHandler_io_in_bits_address = memPortB_io_deq_bits_address; // @[Mem.scala 24:7]
  assign memPortBHandler_io_in_bits_size = memPortB_io_deq_bits_size; // @[Mem.scala 24:7]
  assign memPortBHandler_io_in_bits_stride = memPortB_io_deq_bits_stride; // @[Mem.scala 24:7]
  assign memPortBHandler_io_in_bits_reverse = memPortB_io_deq_bits_reverse; // @[Mem.scala 24:7]
  assign memPortBHandler_io_out_ready = io_memPortB_ready; // @[Decoder.scala 179:15]
  assign memPortA_clock = clock;
  assign memPortA_reset = reset;
  assign memPortA_io_enq_valid = instruction_io_deq_bits_opcode == 4'h1 ? _GEN_42 : _GEN_489; // @[Decoder.scala 244:51]
  assign memPortA_io_enq_bits_write = instruction_io_deq_bits_opcode == 4'h1 ? 1'h0 : _GEN_488; // @[Decoder.scala 244:51]
  assign memPortA_io_enq_bits_address = instruction_io_deq_bits_opcode == 4'h1 ? _GEN_40 : _GEN_487; // @[Decoder.scala 244:51]
  assign memPortA_io_enq_bits_size = instruction_io_deq_bits_opcode == 4'h1 ? _GEN_39 : _GEN_479; // @[Decoder.scala 244:51]
  assign memPortA_io_enq_bits_stride = instruction_io_deq_bits_opcode == 4'h1 ? _GEN_38 : _GEN_485; // @[Decoder.scala 244:51]
  assign memPortA_io_enq_bits_reverse = instruction_io_deq_bits_opcode == 4'h1 ? 1'h0 : _GEN_484; // @[Decoder.scala 244:51]
  assign memPortA_io_deq_ready = memPortAHandler_io_in_ready; // @[Mem.scala 24:7]
  assign memPortB_clock = clock;
  assign memPortB_reset = reset;
  assign memPortB_io_enq_valid = instruction_io_deq_bits_opcode == 4'h1 ? 1'h0 : _GEN_498; // @[Decoder.scala 244:51 Decoder.scala 610:16]
  assign memPortB_io_enq_bits_write = instruction_io_deq_bits_opcode == 4'h1 ? 1'h0 : _GEN_497; // @[Decoder.scala 244:51 Decoder.scala 609:15]
  assign memPortB_io_enq_bits_address = instruction_io_deq_bits_opcode == 4'h1 ? 6'h0 : _GEN_496; // @[Decoder.scala 244:51 Decoder.scala 609:15]
  assign memPortB_io_enq_bits_size = instruction_io_deq_bits_opcode == 4'h1 ? 6'h0 : _GEN_490; // @[Decoder.scala 244:51 Decoder.scala 609:15]
  assign memPortB_io_enq_bits_stride = instruction_io_deq_bits_opcode == 4'h1 ? 3'h0 : _GEN_494; // @[Decoder.scala 244:51 Decoder.scala 609:15]
  assign memPortB_io_enq_bits_reverse = 1'h0; // @[Decoder.scala 244:51 Decoder.scala 609:15]
  assign memPortB_io_deq_ready = memPortBHandler_io_in_ready; // @[Mem.scala 24:7]
  assign accHandler_clock = clock;
  assign accHandler_reset = reset;
  assign accHandler_io_in_valid = instruction_io_deq_bits_opcode == 4'h1 ? _GEN_32 : _GEN_523; // @[Decoder.scala 244:51]
  assign accHandler_io_in_bits_instruction_op = instruction_io_deq_bits_opcode == 4'h1 ? 4'h0 : _GEN_522; // @[Decoder.scala 244:51]
  assign accHandler_io_in_bits_instruction_sourceLeft = instruction_io_deq_bits_opcode == 4'h1 ? 1'h0 : _GEN_521; // @[Decoder.scala 244:51]
  assign accHandler_io_in_bits_instruction_sourceRight = instruction_io_deq_bits_opcode == 4'h1 ? 1'h0 : _GEN_520; // @[Decoder.scala 244:51]
  assign accHandler_io_in_bits_instruction_dest = instruction_io_deq_bits_opcode == 4'h1 ? 1'h0 : _GEN_519; // @[Decoder.scala 244:51]
  assign accHandler_io_in_bits_address = instruction_io_deq_bits_opcode == 4'h1 ? _GEN_27 : _GEN_518; // @[Decoder.scala 244:51]
  assign accHandler_io_in_bits_altAddress = instruction_io_deq_bits_opcode == 4'h1 ? 6'h0 : _GEN_517; // @[Decoder.scala 244:51]
  assign accHandler_io_in_bits_read = instruction_io_deq_bits_opcode == 4'h1 ? 1'h0 : _GEN_516; // @[Decoder.scala 244:51]
  assign accHandler_io_in_bits_write = instruction_io_deq_bits_opcode == 4'h1 | _GEN_515; // @[Decoder.scala 244:51]
  assign accHandler_io_in_bits_accumulate = instruction_io_deq_bits_opcode == 4'h1 ? flags_accumulate : _GEN_514; // @[Decoder.scala 244:51]
  assign accHandler_io_in_bits_size = instruction_io_deq_bits_opcode == 4'h1 ? _GEN_11 : _GEN_513; // @[Decoder.scala 244:51]
  assign accHandler_io_in_bits_stride = instruction_io_deq_bits_opcode == 4'h1 ? args_accStride : _GEN_512; // @[Decoder.scala 244:51]
  assign accHandler_io_out_ready = io_acc_ready; // @[Decoder.scala 197:27]
  assign arrayHandler_clock = clock;
  assign arrayHandler_reset = reset;
  assign arrayHandler_io_in_valid = instruction_io_deq_bits_opcode == 4'h1 ? _GEN_18 : _GEN_475; // @[Decoder.scala 244:51]
  assign arrayHandler_io_in_bits_load = instruction_io_deq_bits_opcode == 4'h1 ? 1'h0 : _T_3; // @[Decoder.scala 244:51]
  assign arrayHandler_io_in_bits_zeroes = instruction_io_deq_bits_opcode == 4'h1 ? flags_zeroes : _GEN_473; // @[Decoder.scala 244:51]
  assign arrayHandler_io_in_bits_size = instruction_io_deq_bits_opcode == 4'h1 ? _GEN_11 : _GEN_472; // @[Decoder.scala 244:51]
  assign arrayHandler_io_out_ready = io_array_ready; // @[Decoder.scala 211:12]
  assign hostDataflowHandler_clock = clock;
  assign hostDataflowHandler_reset = reset;
  assign hostDataflowHandler_io_in_valid = instruction_io_deq_bits_opcode == 4'h1 ? 1'h0 : _GEN_492; // @[Decoder.scala 244:51 Decoder.scala 610:16]
  assign hostDataflowHandler_io_in_bits_kind = instruction_io_deq_bits_opcode == 4'h1 ? 2'h0 : _GEN_491; // @[Decoder.scala 244:51 Decoder.scala 609:15]
  assign hostDataflowHandler_io_in_bits_size = instruction_io_deq_bits_opcode == 4'h1 ? 6'h0 : _GEN_490; // @[Decoder.scala 244:51 Decoder.scala 609:15]
  assign hostDataflowHandler_io_out_ready = io_hostDataflow_ready; // @[Decoder.scala 223:19]
  assign enqueuer1_clock = clock;
  assign enqueuer1_reset = reset;
  assign enqueuer1_io_in_valid = instruction_io_deq_bits_opcode == 4'h1 ? 1'h0 : _GEN_470; // @[Decoder.scala 244:51 MultiEnqueue.scala 40:17]
  assign enqueuer1_io_out_0_ready = instruction_io_deq_bits_opcode == 4'h1 ? 1'h0 : _GEN_471; // @[Decoder.scala 244:51 MultiEnqueue.scala 42:18]
  assign enqueuer2_clock = clock;
  assign enqueuer2_reset = reset;
  assign enqueuer2_io_in_valid = 1'h0; // @[MultiEnqueue.scala 40:17]
  assign enqueuer2_io_out_0_ready = 1'h0; // @[MultiEnqueue.scala 42:18]
  assign enqueuer2_io_out_1_ready = 1'h0; // @[MultiEnqueue.scala 42:18]
  assign enqueuer3_clock = clock;
  assign enqueuer3_reset = reset;
  assign enqueuer3_io_in_valid = instruction_io_deq_bits_opcode == 4'h1 ? _GEN_9 : _GEN_477; // @[Decoder.scala 244:51]
  assign enqueuer3_io_out_0_ready = instruction_io_deq_bits_opcode == 4'h1 ? _GEN_10 : _GEN_478; // @[Decoder.scala 244:51]
  assign enqueuer3_io_out_1_ready = instruction_io_deq_bits_opcode == 4'h1 ? _GEN_14 : _GEN_482; // @[Decoder.scala 244:51]
  assign enqueuer3_io_out_2_ready = instruction_io_deq_bits_opcode == 4'h1 ? _GEN_19 : _GEN_483; // @[Decoder.scala 244:51]
  assign enqueuer4_clock = clock;
  assign enqueuer4_reset = reset;
  assign enqueuer4_io_in_valid = instruction_io_deq_bits_opcode == 4'h1 & _GEN_34; // @[Decoder.scala 244:51 MultiEnqueue.scala 40:17]
  assign enqueuer4_io_out_0_ready = instruction_io_deq_bits_opcode == 4'h1 & _GEN_35; // @[Decoder.scala 244:51 MultiEnqueue.scala 42:18]
  assign enqueuer4_io_out_1_ready = instruction_io_deq_bits_opcode == 4'h1 & _GEN_36; // @[Decoder.scala 244:51 MultiEnqueue.scala 42:18]
  assign enqueuer4_io_out_2_ready = instruction_io_deq_bits_opcode == 4'h1 & _GEN_43; // @[Decoder.scala 244:51 MultiEnqueue.scala 42:18]
  assign enqueuer4_io_out_3_ready = instruction_io_deq_bits_opcode == 4'h1 & _GEN_44; // @[Decoder.scala 244:51 MultiEnqueue.scala 42:18]
  assign validator_io_instruction_valid = instruction_io_deq_valid; // @[Decoder.scala 573:36]
  assign validator_io_instruction_bits_opcode = instruction_io_deq_bits_opcode; // @[Decoder.scala 572:35]
  assign validator_io_instruction_bits_flags = instruction_io_deq_bits_flags; // @[Decoder.scala 572:35]
  assign validator_io_instruction_bits_arguments = instruction_io_deq_bits_arguments; // @[Decoder.scala 572:35]
  always @(posedge clock) begin
    if (reset) begin // @[Decoder.scala 91:24]
      timeout <= 16'h64; // @[Decoder.scala 91:24]
    end else begin
      timeout <= _GEN_597[15:0];
    end
    if (reset) begin // @[Decoder.scala 92:24]
      timer <= 16'h0; // @[Decoder.scala 92:24]
    end else if (instruction_io_deq_ready) begin // @[Decoder.scala 93:27]
      timer <= 16'h0; // @[Decoder.scala 94:11]
    end else if (timer < timeout) begin // @[Decoder.scala 96:27]
      timer <= _timer_T_1; // @[Decoder.scala 97:13]
    end
    if (reset) begin // @[Decoder.scala 103:31]
      tracepoint <= 32'hffffffff; // @[Decoder.scala 103:31]
    end else if (!(instruction_io_deq_bits_opcode == 4'h1)) begin // @[Decoder.scala 244:51]
      if (!(instruction_io_deq_bits_opcode == 4'h3)) begin // @[Decoder.scala 292:62]
        if (!(instruction_io_deq_bits_opcode == 4'h2)) begin // @[Decoder.scala 329:59]
          tracepoint <= _GEN_405;
        end
      end
    end
    if (reset) begin // @[Decoder.scala 104:31]
      programCounter <= 32'h0; // @[Decoder.scala 104:31]
    end else if (instruction_io_deq_bits_opcode == 4'h1) begin // @[Decoder.scala 244:51]
      programCounter <= _GEN_2;
    end else if (instruction_io_deq_bits_opcode == 4'h3) begin // @[Decoder.scala 292:62]
      programCounter <= _GEN_2;
    end else if (instruction_io_deq_bits_opcode == 4'h2) begin // @[Decoder.scala 329:59]
      programCounter <= _GEN_2;
    end else begin
      programCounter <= _GEN_406;
    end
    if (reset) begin // @[Decoder.scala 121:36]
      dram0AddressOffset <= 32'h0; // @[Decoder.scala 121:36]
    end else begin
      dram0AddressOffset <= _GEN_593[31:0];
    end
    if (reset) begin // @[Decoder.scala 122:36]
      dram0CacheBehaviour <= 4'h0; // @[Decoder.scala 122:36]
    end else begin
      dram0CacheBehaviour <= _GEN_594[3:0];
    end
    if (reset) begin // @[Decoder.scala 123:36]
      dram1AddressOffset <= 32'h0; // @[Decoder.scala 123:36]
    end else begin
      dram1AddressOffset <= _GEN_595[31:0];
    end
    if (reset) begin // @[Decoder.scala 124:36]
      dram1CacheBehaviour <= 4'h0; // @[Decoder.scala 124:36]
    end else begin
      dram1CacheBehaviour <= _GEN_596[3:0];
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
  timeout = _RAND_0[15:0];
  _RAND_1 = {1{`RANDOM}};
  timer = _RAND_1[15:0];
  _RAND_2 = {1{`RANDOM}};
  tracepoint = _RAND_2[31:0];
  _RAND_3 = {1{`RANDOM}};
  programCounter = _RAND_3[31:0];
  _RAND_4 = {1{`RANDOM}};
  dram0AddressOffset = _RAND_4[31:0];
  _RAND_5 = {1{`RANDOM}};
  dram0CacheBehaviour = _RAND_5[3:0];
  _RAND_6 = {1{`RANDOM}};
  dram1AddressOffset = _RAND_6[31:0];
  _RAND_7 = {1{`RANDOM}};
  dram1CacheBehaviour = _RAND_7[3:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module MAC(
  input         clock,
  input         reset,
  input         io_load,
  input  [15:0] io_mulInput,
  input  [15:0] io_addInput,
  output [15:0] io_output,
  output [15:0] io_passthrough
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
`endif // RANDOMIZE_REG_INIT
  reg [15:0] weight; // @[MAC.scala 18:28]
  reg [15:0] passthrough; // @[MAC.scala 19:28]
  reg [24:0] output_; // @[MAC.scala 20:28]
  wire [31:0] _output_mac_T = $signed(io_mulInput) * $signed(weight); // @[package.scala 122:18]
  wire [23:0] _output_mac_T_1 = {$signed(io_addInput), 8'h0}; // @[package.scala 122:29]
  wire [31:0] _GEN_3 = {{8{_output_mac_T_1[23]}},_output_mac_T_1}; // @[package.scala 122:23]
  wire [32:0] output_mac = $signed(_output_mac_T) + $signed(_GEN_3); // @[package.scala 122:23]
  wire [8:0] output_mask1 = 9'sh80 - 9'sh1; // @[package.scala 125:44]
  wire [32:0] _output_adjustment_T_1 = $signed(output_mac) & 33'sh80; // @[package.scala 130:16]
  wire [32:0] _GEN_4 = {{24{output_mask1[8]}},output_mask1}; // @[package.scala 130:44]
  wire [32:0] _output_adjustment_T_4 = $signed(output_mac) & $signed(_GEN_4); // @[package.scala 130:44]
  wire [32:0] _output_adjustment_T_7 = $signed(output_mac) & 33'sh100; // @[package.scala 130:71]
  wire  _output_adjustment_T_10 = $signed(_output_adjustment_T_1) != 33'sh0 & ($signed(_output_adjustment_T_4) != 33'sh0
     | $signed(_output_adjustment_T_7) != 33'sh0); // @[package.scala 130:34]
  wire [1:0] output_adjustment = _output_adjustment_T_10 ? $signed(2'sh1) : $signed(2'sh0); // @[package.scala 129:10]
  wire [24:0] _output_adjusted_T = output_mac[32:8]; // @[package.scala 135:26]
  wire [24:0] _GEN_5 = {{23{output_adjustment[1]}},output_adjustment}; // @[package.scala 135:42]
  wire [24:0] output_adjusted = $signed(_output_adjusted_T) + $signed(_GEN_5); // @[package.scala 135:42]
  wire [24:0] _GEN_1 = io_load ? $signed({{9{weight[15]}},weight}) : $signed(output_); // @[MAC.scala 25:17 MAC.scala 27:15 MAC.scala 30:15]
  assign io_output = _GEN_1[15:0];
  assign io_passthrough = passthrough; // @[MAC.scala 22:18]
  always @(posedge clock) begin
    if (reset) begin // @[MAC.scala 18:28]
      weight <= 16'sh0; // @[MAC.scala 18:28]
    end else if (io_load) begin // @[MAC.scala 25:17]
      weight <= io_addInput; // @[MAC.scala 26:12]
    end
    if (reset) begin // @[MAC.scala 19:28]
      passthrough <= 16'sh0; // @[MAC.scala 19:28]
    end else begin
      passthrough <= io_mulInput; // @[MAC.scala 23:15]
    end
    if (reset) begin // @[MAC.scala 20:28]
      output_ <= 25'sh0; // @[MAC.scala 20:28]
    end else if (!(io_load)) begin // @[MAC.scala 25:17]
      if ($signed(output_adjusted) > 25'sh7fff) begin // @[package.scala 103:8]
        output_ <= 25'sh7fff;
      end else if ($signed(output_adjusted) < -25'sh8000) begin // @[package.scala 103:26]
        output_ <= -25'sh8000;
      end else begin
        output_ <= output_adjusted;
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
  weight = _RAND_0[15:0];
  _RAND_1 = {1{`RANDOM}};
  passthrough = _RAND_1[15:0];
  _RAND_2 = {1{`RANDOM}};
  output_ = _RAND_2[24:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module InnerSystolicArray(
  input         clock,
  input         reset,
  input         io_load,
  input  [15:0] io_input_0,
  input  [15:0] io_input_1,
  input  [15:0] io_input_2,
  input  [15:0] io_input_3,
  input  [15:0] io_input_4,
  input  [15:0] io_input_5,
  input  [15:0] io_input_6,
  input  [15:0] io_input_7,
  input  [15:0] io_weight_0,
  input  [15:0] io_weight_1,
  input  [15:0] io_weight_2,
  input  [15:0] io_weight_3,
  input  [15:0] io_weight_4,
  input  [15:0] io_weight_5,
  input  [15:0] io_weight_6,
  input  [15:0] io_weight_7,
  output [15:0] io_output_0,
  output [15:0] io_output_1,
  output [15:0] io_output_2,
  output [15:0] io_output_3,
  output [15:0] io_output_4,
  output [15:0] io_output_5,
  output [15:0] io_output_6,
  output [15:0] io_output_7
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
  reg [31:0] _RAND_49;
  reg [31:0] _RAND_50;
  reg [31:0] _RAND_51;
  reg [31:0] _RAND_52;
  reg [31:0] _RAND_53;
  reg [31:0] _RAND_54;
  reg [31:0] _RAND_55;
  reg [31:0] _RAND_56;
  reg [31:0] _RAND_57;
  reg [31:0] _RAND_58;
  reg [31:0] _RAND_59;
  reg [31:0] _RAND_60;
  reg [31:0] _RAND_61;
  reg [31:0] _RAND_62;
  reg [31:0] _RAND_63;
`endif // RANDOMIZE_REG_INIT
  wire  mac_0_0_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_0_0_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_0_0_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_0_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_0_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_0_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_0_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_0_1_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_0_1_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_0_1_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_1_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_1_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_1_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_1_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_0_2_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_0_2_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_0_2_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_2_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_2_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_2_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_2_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_0_3_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_0_3_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_0_3_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_3_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_3_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_3_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_3_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_0_4_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_0_4_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_0_4_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_4_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_4_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_4_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_4_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_0_5_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_0_5_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_0_5_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_5_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_5_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_5_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_5_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_0_6_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_0_6_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_0_6_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_6_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_6_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_6_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_6_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_0_7_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_0_7_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_0_7_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_7_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_7_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_7_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_0_7_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_1_0_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_1_0_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_1_0_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_0_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_0_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_0_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_0_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_1_1_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_1_1_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_1_1_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_1_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_1_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_1_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_1_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_1_2_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_1_2_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_1_2_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_2_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_2_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_2_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_2_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_1_3_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_1_3_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_1_3_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_3_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_3_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_3_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_3_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_1_4_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_1_4_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_1_4_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_4_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_4_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_4_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_4_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_1_5_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_1_5_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_1_5_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_5_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_5_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_5_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_5_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_1_6_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_1_6_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_1_6_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_6_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_6_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_6_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_6_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_1_7_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_1_7_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_1_7_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_7_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_7_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_7_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_1_7_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_2_0_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_2_0_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_2_0_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_0_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_0_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_0_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_0_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_2_1_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_2_1_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_2_1_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_1_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_1_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_1_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_1_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_2_2_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_2_2_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_2_2_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_2_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_2_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_2_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_2_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_2_3_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_2_3_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_2_3_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_3_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_3_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_3_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_3_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_2_4_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_2_4_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_2_4_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_4_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_4_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_4_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_4_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_2_5_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_2_5_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_2_5_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_5_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_5_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_5_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_5_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_2_6_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_2_6_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_2_6_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_6_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_6_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_6_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_6_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_2_7_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_2_7_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_2_7_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_7_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_7_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_7_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_2_7_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_3_0_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_3_0_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_3_0_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_0_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_0_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_0_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_0_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_3_1_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_3_1_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_3_1_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_1_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_1_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_1_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_1_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_3_2_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_3_2_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_3_2_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_2_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_2_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_2_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_2_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_3_3_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_3_3_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_3_3_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_3_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_3_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_3_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_3_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_3_4_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_3_4_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_3_4_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_4_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_4_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_4_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_4_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_3_5_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_3_5_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_3_5_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_5_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_5_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_5_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_5_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_3_6_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_3_6_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_3_6_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_6_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_6_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_6_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_6_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_3_7_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_3_7_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_3_7_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_7_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_7_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_7_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_3_7_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_4_0_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_4_0_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_4_0_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_0_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_0_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_0_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_0_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_4_1_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_4_1_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_4_1_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_1_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_1_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_1_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_1_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_4_2_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_4_2_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_4_2_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_2_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_2_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_2_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_2_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_4_3_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_4_3_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_4_3_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_3_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_3_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_3_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_3_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_4_4_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_4_4_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_4_4_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_4_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_4_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_4_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_4_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_4_5_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_4_5_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_4_5_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_5_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_5_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_5_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_5_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_4_6_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_4_6_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_4_6_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_6_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_6_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_6_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_6_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_4_7_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_4_7_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_4_7_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_7_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_7_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_7_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_4_7_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_5_0_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_5_0_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_5_0_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_0_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_0_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_0_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_0_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_5_1_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_5_1_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_5_1_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_1_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_1_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_1_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_1_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_5_2_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_5_2_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_5_2_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_2_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_2_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_2_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_2_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_5_3_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_5_3_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_5_3_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_3_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_3_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_3_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_3_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_5_4_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_5_4_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_5_4_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_4_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_4_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_4_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_4_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_5_5_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_5_5_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_5_5_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_5_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_5_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_5_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_5_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_5_6_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_5_6_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_5_6_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_6_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_6_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_6_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_6_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_5_7_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_5_7_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_5_7_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_7_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_7_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_7_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_5_7_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_6_0_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_6_0_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_6_0_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_0_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_0_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_0_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_0_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_6_1_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_6_1_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_6_1_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_1_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_1_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_1_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_1_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_6_2_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_6_2_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_6_2_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_2_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_2_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_2_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_2_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_6_3_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_6_3_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_6_3_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_3_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_3_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_3_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_3_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_6_4_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_6_4_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_6_4_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_4_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_4_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_4_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_4_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_6_5_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_6_5_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_6_5_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_5_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_5_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_5_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_5_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_6_6_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_6_6_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_6_6_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_6_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_6_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_6_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_6_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_6_7_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_6_7_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_6_7_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_7_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_7_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_7_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_6_7_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_7_0_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_7_0_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_7_0_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_0_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_0_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_0_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_0_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_7_1_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_7_1_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_7_1_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_1_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_1_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_1_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_1_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_7_2_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_7_2_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_7_2_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_2_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_2_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_2_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_2_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_7_3_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_7_3_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_7_3_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_3_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_3_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_3_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_3_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_7_4_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_7_4_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_7_4_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_4_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_4_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_4_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_4_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_7_5_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_7_5_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_7_5_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_5_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_5_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_5_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_5_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_7_6_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_7_6_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_7_6_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_6_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_6_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_6_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_6_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  wire  mac_7_7_clock; // @[InnerSystolicArray.scala 34:50]
  wire  mac_7_7_reset; // @[InnerSystolicArray.scala 34:50]
  wire  mac_7_7_io_load; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_7_io_mulInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_7_io_addInput; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_7_io_output; // @[InnerSystolicArray.scala 34:50]
  wire [15:0] mac_7_7_io_passthrough; // @[InnerSystolicArray.scala 34:50]
  reg [15:0] bias_0; // @[InnerSystolicArray.scala 37:20]
  reg [15:0] bias_1; // @[InnerSystolicArray.scala 37:20]
  reg [15:0] bias_2; // @[InnerSystolicArray.scala 37:20]
  reg [15:0] bias_3; // @[InnerSystolicArray.scala 37:20]
  reg [15:0] bias_4; // @[InnerSystolicArray.scala 37:20]
  reg [15:0] bias_5; // @[InnerSystolicArray.scala 37:20]
  reg [15:0] bias_6; // @[InnerSystolicArray.scala 37:20]
  reg [15:0] bias_7; // @[InnerSystolicArray.scala 37:20]
  reg [15:0] mac_0_1_io_mulInput_sr_0; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_2_io_mulInput_sr_0; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_2_io_mulInput_sr_1; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_3_io_mulInput_sr_0; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_3_io_mulInput_sr_1; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_3_io_mulInput_sr_2; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_4_io_mulInput_sr_0; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_4_io_mulInput_sr_1; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_4_io_mulInput_sr_2; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_4_io_mulInput_sr_3; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_5_io_mulInput_sr_0; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_5_io_mulInput_sr_1; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_5_io_mulInput_sr_2; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_5_io_mulInput_sr_3; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_5_io_mulInput_sr_4; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_6_io_mulInput_sr_0; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_6_io_mulInput_sr_1; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_6_io_mulInput_sr_2; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_6_io_mulInput_sr_3; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_6_io_mulInput_sr_4; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_6_io_mulInput_sr_5; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_7_io_mulInput_sr_0; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_7_io_mulInput_sr_1; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_7_io_mulInput_sr_2; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_7_io_mulInput_sr_3; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_7_io_mulInput_sr_4; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_7_io_mulInput_sr_5; // @[ShiftRegister.scala 10:22]
  reg [15:0] mac_0_7_io_mulInput_sr_6; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_0_sr_0; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_0_sr_1; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_0_sr_2; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_0_sr_3; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_0_sr_4; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_0_sr_5; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_0_sr_6; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_1_sr_0; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_1_sr_1; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_1_sr_2; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_1_sr_3; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_1_sr_4; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_1_sr_5; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_2_sr_0; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_2_sr_1; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_2_sr_2; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_2_sr_3; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_2_sr_4; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_3_sr_0; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_3_sr_1; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_3_sr_2; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_3_sr_3; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_4_sr_0; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_4_sr_1; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_4_sr_2; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_5_sr_0; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_5_sr_1; // @[ShiftRegister.scala 10:22]
  reg [15:0] io_output_6_sr_0; // @[ShiftRegister.scala 10:22]
  MAC mac_0_0 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_0_0_clock),
    .reset(mac_0_0_reset),
    .io_load(mac_0_0_io_load),
    .io_mulInput(mac_0_0_io_mulInput),
    .io_addInput(mac_0_0_io_addInput),
    .io_output(mac_0_0_io_output),
    .io_passthrough(mac_0_0_io_passthrough)
  );
  MAC mac_0_1 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_0_1_clock),
    .reset(mac_0_1_reset),
    .io_load(mac_0_1_io_load),
    .io_mulInput(mac_0_1_io_mulInput),
    .io_addInput(mac_0_1_io_addInput),
    .io_output(mac_0_1_io_output),
    .io_passthrough(mac_0_1_io_passthrough)
  );
  MAC mac_0_2 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_0_2_clock),
    .reset(mac_0_2_reset),
    .io_load(mac_0_2_io_load),
    .io_mulInput(mac_0_2_io_mulInput),
    .io_addInput(mac_0_2_io_addInput),
    .io_output(mac_0_2_io_output),
    .io_passthrough(mac_0_2_io_passthrough)
  );
  MAC mac_0_3 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_0_3_clock),
    .reset(mac_0_3_reset),
    .io_load(mac_0_3_io_load),
    .io_mulInput(mac_0_3_io_mulInput),
    .io_addInput(mac_0_3_io_addInput),
    .io_output(mac_0_3_io_output),
    .io_passthrough(mac_0_3_io_passthrough)
  );
  MAC mac_0_4 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_0_4_clock),
    .reset(mac_0_4_reset),
    .io_load(mac_0_4_io_load),
    .io_mulInput(mac_0_4_io_mulInput),
    .io_addInput(mac_0_4_io_addInput),
    .io_output(mac_0_4_io_output),
    .io_passthrough(mac_0_4_io_passthrough)
  );
  MAC mac_0_5 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_0_5_clock),
    .reset(mac_0_5_reset),
    .io_load(mac_0_5_io_load),
    .io_mulInput(mac_0_5_io_mulInput),
    .io_addInput(mac_0_5_io_addInput),
    .io_output(mac_0_5_io_output),
    .io_passthrough(mac_0_5_io_passthrough)
  );
  MAC mac_0_6 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_0_6_clock),
    .reset(mac_0_6_reset),
    .io_load(mac_0_6_io_load),
    .io_mulInput(mac_0_6_io_mulInput),
    .io_addInput(mac_0_6_io_addInput),
    .io_output(mac_0_6_io_output),
    .io_passthrough(mac_0_6_io_passthrough)
  );
  MAC mac_0_7 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_0_7_clock),
    .reset(mac_0_7_reset),
    .io_load(mac_0_7_io_load),
    .io_mulInput(mac_0_7_io_mulInput),
    .io_addInput(mac_0_7_io_addInput),
    .io_output(mac_0_7_io_output),
    .io_passthrough(mac_0_7_io_passthrough)
  );
  MAC mac_1_0 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_1_0_clock),
    .reset(mac_1_0_reset),
    .io_load(mac_1_0_io_load),
    .io_mulInput(mac_1_0_io_mulInput),
    .io_addInput(mac_1_0_io_addInput),
    .io_output(mac_1_0_io_output),
    .io_passthrough(mac_1_0_io_passthrough)
  );
  MAC mac_1_1 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_1_1_clock),
    .reset(mac_1_1_reset),
    .io_load(mac_1_1_io_load),
    .io_mulInput(mac_1_1_io_mulInput),
    .io_addInput(mac_1_1_io_addInput),
    .io_output(mac_1_1_io_output),
    .io_passthrough(mac_1_1_io_passthrough)
  );
  MAC mac_1_2 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_1_2_clock),
    .reset(mac_1_2_reset),
    .io_load(mac_1_2_io_load),
    .io_mulInput(mac_1_2_io_mulInput),
    .io_addInput(mac_1_2_io_addInput),
    .io_output(mac_1_2_io_output),
    .io_passthrough(mac_1_2_io_passthrough)
  );
  MAC mac_1_3 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_1_3_clock),
    .reset(mac_1_3_reset),
    .io_load(mac_1_3_io_load),
    .io_mulInput(mac_1_3_io_mulInput),
    .io_addInput(mac_1_3_io_addInput),
    .io_output(mac_1_3_io_output),
    .io_passthrough(mac_1_3_io_passthrough)
  );
  MAC mac_1_4 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_1_4_clock),
    .reset(mac_1_4_reset),
    .io_load(mac_1_4_io_load),
    .io_mulInput(mac_1_4_io_mulInput),
    .io_addInput(mac_1_4_io_addInput),
    .io_output(mac_1_4_io_output),
    .io_passthrough(mac_1_4_io_passthrough)
  );
  MAC mac_1_5 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_1_5_clock),
    .reset(mac_1_5_reset),
    .io_load(mac_1_5_io_load),
    .io_mulInput(mac_1_5_io_mulInput),
    .io_addInput(mac_1_5_io_addInput),
    .io_output(mac_1_5_io_output),
    .io_passthrough(mac_1_5_io_passthrough)
  );
  MAC mac_1_6 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_1_6_clock),
    .reset(mac_1_6_reset),
    .io_load(mac_1_6_io_load),
    .io_mulInput(mac_1_6_io_mulInput),
    .io_addInput(mac_1_6_io_addInput),
    .io_output(mac_1_6_io_output),
    .io_passthrough(mac_1_6_io_passthrough)
  );
  MAC mac_1_7 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_1_7_clock),
    .reset(mac_1_7_reset),
    .io_load(mac_1_7_io_load),
    .io_mulInput(mac_1_7_io_mulInput),
    .io_addInput(mac_1_7_io_addInput),
    .io_output(mac_1_7_io_output),
    .io_passthrough(mac_1_7_io_passthrough)
  );
  MAC mac_2_0 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_2_0_clock),
    .reset(mac_2_0_reset),
    .io_load(mac_2_0_io_load),
    .io_mulInput(mac_2_0_io_mulInput),
    .io_addInput(mac_2_0_io_addInput),
    .io_output(mac_2_0_io_output),
    .io_passthrough(mac_2_0_io_passthrough)
  );
  MAC mac_2_1 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_2_1_clock),
    .reset(mac_2_1_reset),
    .io_load(mac_2_1_io_load),
    .io_mulInput(mac_2_1_io_mulInput),
    .io_addInput(mac_2_1_io_addInput),
    .io_output(mac_2_1_io_output),
    .io_passthrough(mac_2_1_io_passthrough)
  );
  MAC mac_2_2 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_2_2_clock),
    .reset(mac_2_2_reset),
    .io_load(mac_2_2_io_load),
    .io_mulInput(mac_2_2_io_mulInput),
    .io_addInput(mac_2_2_io_addInput),
    .io_output(mac_2_2_io_output),
    .io_passthrough(mac_2_2_io_passthrough)
  );
  MAC mac_2_3 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_2_3_clock),
    .reset(mac_2_3_reset),
    .io_load(mac_2_3_io_load),
    .io_mulInput(mac_2_3_io_mulInput),
    .io_addInput(mac_2_3_io_addInput),
    .io_output(mac_2_3_io_output),
    .io_passthrough(mac_2_3_io_passthrough)
  );
  MAC mac_2_4 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_2_4_clock),
    .reset(mac_2_4_reset),
    .io_load(mac_2_4_io_load),
    .io_mulInput(mac_2_4_io_mulInput),
    .io_addInput(mac_2_4_io_addInput),
    .io_output(mac_2_4_io_output),
    .io_passthrough(mac_2_4_io_passthrough)
  );
  MAC mac_2_5 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_2_5_clock),
    .reset(mac_2_5_reset),
    .io_load(mac_2_5_io_load),
    .io_mulInput(mac_2_5_io_mulInput),
    .io_addInput(mac_2_5_io_addInput),
    .io_output(mac_2_5_io_output),
    .io_passthrough(mac_2_5_io_passthrough)
  );
  MAC mac_2_6 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_2_6_clock),
    .reset(mac_2_6_reset),
    .io_load(mac_2_6_io_load),
    .io_mulInput(mac_2_6_io_mulInput),
    .io_addInput(mac_2_6_io_addInput),
    .io_output(mac_2_6_io_output),
    .io_passthrough(mac_2_6_io_passthrough)
  );
  MAC mac_2_7 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_2_7_clock),
    .reset(mac_2_7_reset),
    .io_load(mac_2_7_io_load),
    .io_mulInput(mac_2_7_io_mulInput),
    .io_addInput(mac_2_7_io_addInput),
    .io_output(mac_2_7_io_output),
    .io_passthrough(mac_2_7_io_passthrough)
  );
  MAC mac_3_0 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_3_0_clock),
    .reset(mac_3_0_reset),
    .io_load(mac_3_0_io_load),
    .io_mulInput(mac_3_0_io_mulInput),
    .io_addInput(mac_3_0_io_addInput),
    .io_output(mac_3_0_io_output),
    .io_passthrough(mac_3_0_io_passthrough)
  );
  MAC mac_3_1 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_3_1_clock),
    .reset(mac_3_1_reset),
    .io_load(mac_3_1_io_load),
    .io_mulInput(mac_3_1_io_mulInput),
    .io_addInput(mac_3_1_io_addInput),
    .io_output(mac_3_1_io_output),
    .io_passthrough(mac_3_1_io_passthrough)
  );
  MAC mac_3_2 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_3_2_clock),
    .reset(mac_3_2_reset),
    .io_load(mac_3_2_io_load),
    .io_mulInput(mac_3_2_io_mulInput),
    .io_addInput(mac_3_2_io_addInput),
    .io_output(mac_3_2_io_output),
    .io_passthrough(mac_3_2_io_passthrough)
  );
  MAC mac_3_3 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_3_3_clock),
    .reset(mac_3_3_reset),
    .io_load(mac_3_3_io_load),
    .io_mulInput(mac_3_3_io_mulInput),
    .io_addInput(mac_3_3_io_addInput),
    .io_output(mac_3_3_io_output),
    .io_passthrough(mac_3_3_io_passthrough)
  );
  MAC mac_3_4 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_3_4_clock),
    .reset(mac_3_4_reset),
    .io_load(mac_3_4_io_load),
    .io_mulInput(mac_3_4_io_mulInput),
    .io_addInput(mac_3_4_io_addInput),
    .io_output(mac_3_4_io_output),
    .io_passthrough(mac_3_4_io_passthrough)
  );
  MAC mac_3_5 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_3_5_clock),
    .reset(mac_3_5_reset),
    .io_load(mac_3_5_io_load),
    .io_mulInput(mac_3_5_io_mulInput),
    .io_addInput(mac_3_5_io_addInput),
    .io_output(mac_3_5_io_output),
    .io_passthrough(mac_3_5_io_passthrough)
  );
  MAC mac_3_6 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_3_6_clock),
    .reset(mac_3_6_reset),
    .io_load(mac_3_6_io_load),
    .io_mulInput(mac_3_6_io_mulInput),
    .io_addInput(mac_3_6_io_addInput),
    .io_output(mac_3_6_io_output),
    .io_passthrough(mac_3_6_io_passthrough)
  );
  MAC mac_3_7 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_3_7_clock),
    .reset(mac_3_7_reset),
    .io_load(mac_3_7_io_load),
    .io_mulInput(mac_3_7_io_mulInput),
    .io_addInput(mac_3_7_io_addInput),
    .io_output(mac_3_7_io_output),
    .io_passthrough(mac_3_7_io_passthrough)
  );
  MAC mac_4_0 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_4_0_clock),
    .reset(mac_4_0_reset),
    .io_load(mac_4_0_io_load),
    .io_mulInput(mac_4_0_io_mulInput),
    .io_addInput(mac_4_0_io_addInput),
    .io_output(mac_4_0_io_output),
    .io_passthrough(mac_4_0_io_passthrough)
  );
  MAC mac_4_1 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_4_1_clock),
    .reset(mac_4_1_reset),
    .io_load(mac_4_1_io_load),
    .io_mulInput(mac_4_1_io_mulInput),
    .io_addInput(mac_4_1_io_addInput),
    .io_output(mac_4_1_io_output),
    .io_passthrough(mac_4_1_io_passthrough)
  );
  MAC mac_4_2 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_4_2_clock),
    .reset(mac_4_2_reset),
    .io_load(mac_4_2_io_load),
    .io_mulInput(mac_4_2_io_mulInput),
    .io_addInput(mac_4_2_io_addInput),
    .io_output(mac_4_2_io_output),
    .io_passthrough(mac_4_2_io_passthrough)
  );
  MAC mac_4_3 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_4_3_clock),
    .reset(mac_4_3_reset),
    .io_load(mac_4_3_io_load),
    .io_mulInput(mac_4_3_io_mulInput),
    .io_addInput(mac_4_3_io_addInput),
    .io_output(mac_4_3_io_output),
    .io_passthrough(mac_4_3_io_passthrough)
  );
  MAC mac_4_4 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_4_4_clock),
    .reset(mac_4_4_reset),
    .io_load(mac_4_4_io_load),
    .io_mulInput(mac_4_4_io_mulInput),
    .io_addInput(mac_4_4_io_addInput),
    .io_output(mac_4_4_io_output),
    .io_passthrough(mac_4_4_io_passthrough)
  );
  MAC mac_4_5 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_4_5_clock),
    .reset(mac_4_5_reset),
    .io_load(mac_4_5_io_load),
    .io_mulInput(mac_4_5_io_mulInput),
    .io_addInput(mac_4_5_io_addInput),
    .io_output(mac_4_5_io_output),
    .io_passthrough(mac_4_5_io_passthrough)
  );
  MAC mac_4_6 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_4_6_clock),
    .reset(mac_4_6_reset),
    .io_load(mac_4_6_io_load),
    .io_mulInput(mac_4_6_io_mulInput),
    .io_addInput(mac_4_6_io_addInput),
    .io_output(mac_4_6_io_output),
    .io_passthrough(mac_4_6_io_passthrough)
  );
  MAC mac_4_7 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_4_7_clock),
    .reset(mac_4_7_reset),
    .io_load(mac_4_7_io_load),
    .io_mulInput(mac_4_7_io_mulInput),
    .io_addInput(mac_4_7_io_addInput),
    .io_output(mac_4_7_io_output),
    .io_passthrough(mac_4_7_io_passthrough)
  );
  MAC mac_5_0 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_5_0_clock),
    .reset(mac_5_0_reset),
    .io_load(mac_5_0_io_load),
    .io_mulInput(mac_5_0_io_mulInput),
    .io_addInput(mac_5_0_io_addInput),
    .io_output(mac_5_0_io_output),
    .io_passthrough(mac_5_0_io_passthrough)
  );
  MAC mac_5_1 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_5_1_clock),
    .reset(mac_5_1_reset),
    .io_load(mac_5_1_io_load),
    .io_mulInput(mac_5_1_io_mulInput),
    .io_addInput(mac_5_1_io_addInput),
    .io_output(mac_5_1_io_output),
    .io_passthrough(mac_5_1_io_passthrough)
  );
  MAC mac_5_2 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_5_2_clock),
    .reset(mac_5_2_reset),
    .io_load(mac_5_2_io_load),
    .io_mulInput(mac_5_2_io_mulInput),
    .io_addInput(mac_5_2_io_addInput),
    .io_output(mac_5_2_io_output),
    .io_passthrough(mac_5_2_io_passthrough)
  );
  MAC mac_5_3 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_5_3_clock),
    .reset(mac_5_3_reset),
    .io_load(mac_5_3_io_load),
    .io_mulInput(mac_5_3_io_mulInput),
    .io_addInput(mac_5_3_io_addInput),
    .io_output(mac_5_3_io_output),
    .io_passthrough(mac_5_3_io_passthrough)
  );
  MAC mac_5_4 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_5_4_clock),
    .reset(mac_5_4_reset),
    .io_load(mac_5_4_io_load),
    .io_mulInput(mac_5_4_io_mulInput),
    .io_addInput(mac_5_4_io_addInput),
    .io_output(mac_5_4_io_output),
    .io_passthrough(mac_5_4_io_passthrough)
  );
  MAC mac_5_5 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_5_5_clock),
    .reset(mac_5_5_reset),
    .io_load(mac_5_5_io_load),
    .io_mulInput(mac_5_5_io_mulInput),
    .io_addInput(mac_5_5_io_addInput),
    .io_output(mac_5_5_io_output),
    .io_passthrough(mac_5_5_io_passthrough)
  );
  MAC mac_5_6 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_5_6_clock),
    .reset(mac_5_6_reset),
    .io_load(mac_5_6_io_load),
    .io_mulInput(mac_5_6_io_mulInput),
    .io_addInput(mac_5_6_io_addInput),
    .io_output(mac_5_6_io_output),
    .io_passthrough(mac_5_6_io_passthrough)
  );
  MAC mac_5_7 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_5_7_clock),
    .reset(mac_5_7_reset),
    .io_load(mac_5_7_io_load),
    .io_mulInput(mac_5_7_io_mulInput),
    .io_addInput(mac_5_7_io_addInput),
    .io_output(mac_5_7_io_output),
    .io_passthrough(mac_5_7_io_passthrough)
  );
  MAC mac_6_0 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_6_0_clock),
    .reset(mac_6_0_reset),
    .io_load(mac_6_0_io_load),
    .io_mulInput(mac_6_0_io_mulInput),
    .io_addInput(mac_6_0_io_addInput),
    .io_output(mac_6_0_io_output),
    .io_passthrough(mac_6_0_io_passthrough)
  );
  MAC mac_6_1 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_6_1_clock),
    .reset(mac_6_1_reset),
    .io_load(mac_6_1_io_load),
    .io_mulInput(mac_6_1_io_mulInput),
    .io_addInput(mac_6_1_io_addInput),
    .io_output(mac_6_1_io_output),
    .io_passthrough(mac_6_1_io_passthrough)
  );
  MAC mac_6_2 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_6_2_clock),
    .reset(mac_6_2_reset),
    .io_load(mac_6_2_io_load),
    .io_mulInput(mac_6_2_io_mulInput),
    .io_addInput(mac_6_2_io_addInput),
    .io_output(mac_6_2_io_output),
    .io_passthrough(mac_6_2_io_passthrough)
  );
  MAC mac_6_3 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_6_3_clock),
    .reset(mac_6_3_reset),
    .io_load(mac_6_3_io_load),
    .io_mulInput(mac_6_3_io_mulInput),
    .io_addInput(mac_6_3_io_addInput),
    .io_output(mac_6_3_io_output),
    .io_passthrough(mac_6_3_io_passthrough)
  );
  MAC mac_6_4 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_6_4_clock),
    .reset(mac_6_4_reset),
    .io_load(mac_6_4_io_load),
    .io_mulInput(mac_6_4_io_mulInput),
    .io_addInput(mac_6_4_io_addInput),
    .io_output(mac_6_4_io_output),
    .io_passthrough(mac_6_4_io_passthrough)
  );
  MAC mac_6_5 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_6_5_clock),
    .reset(mac_6_5_reset),
    .io_load(mac_6_5_io_load),
    .io_mulInput(mac_6_5_io_mulInput),
    .io_addInput(mac_6_5_io_addInput),
    .io_output(mac_6_5_io_output),
    .io_passthrough(mac_6_5_io_passthrough)
  );
  MAC mac_6_6 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_6_6_clock),
    .reset(mac_6_6_reset),
    .io_load(mac_6_6_io_load),
    .io_mulInput(mac_6_6_io_mulInput),
    .io_addInput(mac_6_6_io_addInput),
    .io_output(mac_6_6_io_output),
    .io_passthrough(mac_6_6_io_passthrough)
  );
  MAC mac_6_7 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_6_7_clock),
    .reset(mac_6_7_reset),
    .io_load(mac_6_7_io_load),
    .io_mulInput(mac_6_7_io_mulInput),
    .io_addInput(mac_6_7_io_addInput),
    .io_output(mac_6_7_io_output),
    .io_passthrough(mac_6_7_io_passthrough)
  );
  MAC mac_7_0 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_7_0_clock),
    .reset(mac_7_0_reset),
    .io_load(mac_7_0_io_load),
    .io_mulInput(mac_7_0_io_mulInput),
    .io_addInput(mac_7_0_io_addInput),
    .io_output(mac_7_0_io_output),
    .io_passthrough(mac_7_0_io_passthrough)
  );
  MAC mac_7_1 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_7_1_clock),
    .reset(mac_7_1_reset),
    .io_load(mac_7_1_io_load),
    .io_mulInput(mac_7_1_io_mulInput),
    .io_addInput(mac_7_1_io_addInput),
    .io_output(mac_7_1_io_output),
    .io_passthrough(mac_7_1_io_passthrough)
  );
  MAC mac_7_2 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_7_2_clock),
    .reset(mac_7_2_reset),
    .io_load(mac_7_2_io_load),
    .io_mulInput(mac_7_2_io_mulInput),
    .io_addInput(mac_7_2_io_addInput),
    .io_output(mac_7_2_io_output),
    .io_passthrough(mac_7_2_io_passthrough)
  );
  MAC mac_7_3 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_7_3_clock),
    .reset(mac_7_3_reset),
    .io_load(mac_7_3_io_load),
    .io_mulInput(mac_7_3_io_mulInput),
    .io_addInput(mac_7_3_io_addInput),
    .io_output(mac_7_3_io_output),
    .io_passthrough(mac_7_3_io_passthrough)
  );
  MAC mac_7_4 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_7_4_clock),
    .reset(mac_7_4_reset),
    .io_load(mac_7_4_io_load),
    .io_mulInput(mac_7_4_io_mulInput),
    .io_addInput(mac_7_4_io_addInput),
    .io_output(mac_7_4_io_output),
    .io_passthrough(mac_7_4_io_passthrough)
  );
  MAC mac_7_5 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_7_5_clock),
    .reset(mac_7_5_reset),
    .io_load(mac_7_5_io_load),
    .io_mulInput(mac_7_5_io_mulInput),
    .io_addInput(mac_7_5_io_addInput),
    .io_output(mac_7_5_io_output),
    .io_passthrough(mac_7_5_io_passthrough)
  );
  MAC mac_7_6 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_7_6_clock),
    .reset(mac_7_6_reset),
    .io_load(mac_7_6_io_load),
    .io_mulInput(mac_7_6_io_mulInput),
    .io_addInput(mac_7_6_io_addInput),
    .io_output(mac_7_6_io_output),
    .io_passthrough(mac_7_6_io_passthrough)
  );
  MAC mac_7_7 ( // @[InnerSystolicArray.scala 34:50]
    .clock(mac_7_7_clock),
    .reset(mac_7_7_reset),
    .io_load(mac_7_7_io_load),
    .io_mulInput(mac_7_7_io_mulInput),
    .io_addInput(mac_7_7_io_addInput),
    .io_output(mac_7_7_io_output),
    .io_passthrough(mac_7_7_io_passthrough)
  );
  assign io_output_0 = io_output_0_sr_6; // @[InnerSystolicArray.scala 75:18]
  assign io_output_1 = io_output_1_sr_5; // @[InnerSystolicArray.scala 75:18]
  assign io_output_2 = io_output_2_sr_4; // @[InnerSystolicArray.scala 75:18]
  assign io_output_3 = io_output_3_sr_3; // @[InnerSystolicArray.scala 75:18]
  assign io_output_4 = io_output_4_sr_2; // @[InnerSystolicArray.scala 75:18]
  assign io_output_5 = io_output_5_sr_1; // @[InnerSystolicArray.scala 75:18]
  assign io_output_6 = io_output_6_sr_0; // @[InnerSystolicArray.scala 75:18]
  assign io_output_7 = mac_7_7_io_output; // @[InnerSystolicArray.scala 75:18]
  assign mac_0_0_clock = clock;
  assign mac_0_0_reset = reset;
  assign mac_0_0_io_load = io_load; // @[InnerSystolicArray.scala 52:23]
  assign mac_0_0_io_mulInput = io_input_0; // @[InnerSystolicArray.scala 48:27]
  assign mac_0_0_io_addInput = bias_0; // @[InnerSystolicArray.scala 57:27]
  assign mac_0_1_clock = clock;
  assign mac_0_1_reset = reset;
  assign mac_0_1_io_load = io_load; // @[InnerSystolicArray.scala 52:23]
  assign mac_0_1_io_mulInput = mac_0_1_io_mulInput_sr_0; // @[InnerSystolicArray.scala 48:27]
  assign mac_0_1_io_addInput = mac_0_0_io_output; // @[InnerSystolicArray.scala 50:29]
  assign mac_0_2_clock = clock;
  assign mac_0_2_reset = reset;
  assign mac_0_2_io_load = io_load; // @[InnerSystolicArray.scala 52:23]
  assign mac_0_2_io_mulInput = mac_0_2_io_mulInput_sr_1; // @[InnerSystolicArray.scala 48:27]
  assign mac_0_2_io_addInput = mac_0_1_io_output; // @[InnerSystolicArray.scala 50:29]
  assign mac_0_3_clock = clock;
  assign mac_0_3_reset = reset;
  assign mac_0_3_io_load = io_load; // @[InnerSystolicArray.scala 52:23]
  assign mac_0_3_io_mulInput = mac_0_3_io_mulInput_sr_2; // @[InnerSystolicArray.scala 48:27]
  assign mac_0_3_io_addInput = mac_0_2_io_output; // @[InnerSystolicArray.scala 50:29]
  assign mac_0_4_clock = clock;
  assign mac_0_4_reset = reset;
  assign mac_0_4_io_load = io_load; // @[InnerSystolicArray.scala 52:23]
  assign mac_0_4_io_mulInput = mac_0_4_io_mulInput_sr_3; // @[InnerSystolicArray.scala 48:27]
  assign mac_0_4_io_addInput = mac_0_3_io_output; // @[InnerSystolicArray.scala 50:29]
  assign mac_0_5_clock = clock;
  assign mac_0_5_reset = reset;
  assign mac_0_5_io_load = io_load; // @[InnerSystolicArray.scala 52:23]
  assign mac_0_5_io_mulInput = mac_0_5_io_mulInput_sr_4; // @[InnerSystolicArray.scala 48:27]
  assign mac_0_5_io_addInput = mac_0_4_io_output; // @[InnerSystolicArray.scala 50:29]
  assign mac_0_6_clock = clock;
  assign mac_0_6_reset = reset;
  assign mac_0_6_io_load = io_load; // @[InnerSystolicArray.scala 52:23]
  assign mac_0_6_io_mulInput = mac_0_6_io_mulInput_sr_5; // @[InnerSystolicArray.scala 48:27]
  assign mac_0_6_io_addInput = mac_0_5_io_output; // @[InnerSystolicArray.scala 50:29]
  assign mac_0_7_clock = clock;
  assign mac_0_7_reset = reset;
  assign mac_0_7_io_load = io_load; // @[InnerSystolicArray.scala 52:23]
  assign mac_0_7_io_mulInput = mac_0_7_io_mulInput_sr_6; // @[InnerSystolicArray.scala 48:27]
  assign mac_0_7_io_addInput = mac_0_6_io_output; // @[InnerSystolicArray.scala 50:29]
  assign mac_1_0_clock = clock;
  assign mac_1_0_reset = reset;
  assign mac_1_0_io_load = io_load; // @[InnerSystolicArray.scala 60:25]
  assign mac_1_0_io_mulInput = mac_0_0_io_passthrough; // @[InnerSystolicArray.scala 59:29]
  assign mac_1_0_io_addInput = bias_1; // @[InnerSystolicArray.scala 57:27]
  assign mac_1_1_clock = clock;
  assign mac_1_1_reset = reset;
  assign mac_1_1_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_1_1_io_mulInput = mac_0_1_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_1_1_io_addInput = mac_1_0_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_1_2_clock = clock;
  assign mac_1_2_reset = reset;
  assign mac_1_2_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_1_2_io_mulInput = mac_0_2_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_1_2_io_addInput = mac_1_1_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_1_3_clock = clock;
  assign mac_1_3_reset = reset;
  assign mac_1_3_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_1_3_io_mulInput = mac_0_3_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_1_3_io_addInput = mac_1_2_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_1_4_clock = clock;
  assign mac_1_4_reset = reset;
  assign mac_1_4_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_1_4_io_mulInput = mac_0_4_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_1_4_io_addInput = mac_1_3_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_1_5_clock = clock;
  assign mac_1_5_reset = reset;
  assign mac_1_5_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_1_5_io_mulInput = mac_0_5_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_1_5_io_addInput = mac_1_4_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_1_6_clock = clock;
  assign mac_1_6_reset = reset;
  assign mac_1_6_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_1_6_io_mulInput = mac_0_6_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_1_6_io_addInput = mac_1_5_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_1_7_clock = clock;
  assign mac_1_7_reset = reset;
  assign mac_1_7_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_1_7_io_mulInput = mac_0_7_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_1_7_io_addInput = mac_1_6_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_2_0_clock = clock;
  assign mac_2_0_reset = reset;
  assign mac_2_0_io_load = io_load; // @[InnerSystolicArray.scala 60:25]
  assign mac_2_0_io_mulInput = mac_1_0_io_passthrough; // @[InnerSystolicArray.scala 59:29]
  assign mac_2_0_io_addInput = bias_2; // @[InnerSystolicArray.scala 57:27]
  assign mac_2_1_clock = clock;
  assign mac_2_1_reset = reset;
  assign mac_2_1_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_2_1_io_mulInput = mac_1_1_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_2_1_io_addInput = mac_2_0_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_2_2_clock = clock;
  assign mac_2_2_reset = reset;
  assign mac_2_2_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_2_2_io_mulInput = mac_1_2_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_2_2_io_addInput = mac_2_1_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_2_3_clock = clock;
  assign mac_2_3_reset = reset;
  assign mac_2_3_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_2_3_io_mulInput = mac_1_3_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_2_3_io_addInput = mac_2_2_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_2_4_clock = clock;
  assign mac_2_4_reset = reset;
  assign mac_2_4_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_2_4_io_mulInput = mac_1_4_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_2_4_io_addInput = mac_2_3_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_2_5_clock = clock;
  assign mac_2_5_reset = reset;
  assign mac_2_5_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_2_5_io_mulInput = mac_1_5_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_2_5_io_addInput = mac_2_4_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_2_6_clock = clock;
  assign mac_2_6_reset = reset;
  assign mac_2_6_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_2_6_io_mulInput = mac_1_6_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_2_6_io_addInput = mac_2_5_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_2_7_clock = clock;
  assign mac_2_7_reset = reset;
  assign mac_2_7_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_2_7_io_mulInput = mac_1_7_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_2_7_io_addInput = mac_2_6_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_3_0_clock = clock;
  assign mac_3_0_reset = reset;
  assign mac_3_0_io_load = io_load; // @[InnerSystolicArray.scala 60:25]
  assign mac_3_0_io_mulInput = mac_2_0_io_passthrough; // @[InnerSystolicArray.scala 59:29]
  assign mac_3_0_io_addInput = bias_3; // @[InnerSystolicArray.scala 57:27]
  assign mac_3_1_clock = clock;
  assign mac_3_1_reset = reset;
  assign mac_3_1_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_3_1_io_mulInput = mac_2_1_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_3_1_io_addInput = mac_3_0_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_3_2_clock = clock;
  assign mac_3_2_reset = reset;
  assign mac_3_2_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_3_2_io_mulInput = mac_2_2_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_3_2_io_addInput = mac_3_1_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_3_3_clock = clock;
  assign mac_3_3_reset = reset;
  assign mac_3_3_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_3_3_io_mulInput = mac_2_3_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_3_3_io_addInput = mac_3_2_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_3_4_clock = clock;
  assign mac_3_4_reset = reset;
  assign mac_3_4_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_3_4_io_mulInput = mac_2_4_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_3_4_io_addInput = mac_3_3_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_3_5_clock = clock;
  assign mac_3_5_reset = reset;
  assign mac_3_5_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_3_5_io_mulInput = mac_2_5_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_3_5_io_addInput = mac_3_4_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_3_6_clock = clock;
  assign mac_3_6_reset = reset;
  assign mac_3_6_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_3_6_io_mulInput = mac_2_6_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_3_6_io_addInput = mac_3_5_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_3_7_clock = clock;
  assign mac_3_7_reset = reset;
  assign mac_3_7_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_3_7_io_mulInput = mac_2_7_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_3_7_io_addInput = mac_3_6_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_4_0_clock = clock;
  assign mac_4_0_reset = reset;
  assign mac_4_0_io_load = io_load; // @[InnerSystolicArray.scala 60:25]
  assign mac_4_0_io_mulInput = mac_3_0_io_passthrough; // @[InnerSystolicArray.scala 59:29]
  assign mac_4_0_io_addInput = bias_4; // @[InnerSystolicArray.scala 57:27]
  assign mac_4_1_clock = clock;
  assign mac_4_1_reset = reset;
  assign mac_4_1_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_4_1_io_mulInput = mac_3_1_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_4_1_io_addInput = mac_4_0_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_4_2_clock = clock;
  assign mac_4_2_reset = reset;
  assign mac_4_2_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_4_2_io_mulInput = mac_3_2_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_4_2_io_addInput = mac_4_1_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_4_3_clock = clock;
  assign mac_4_3_reset = reset;
  assign mac_4_3_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_4_3_io_mulInput = mac_3_3_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_4_3_io_addInput = mac_4_2_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_4_4_clock = clock;
  assign mac_4_4_reset = reset;
  assign mac_4_4_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_4_4_io_mulInput = mac_3_4_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_4_4_io_addInput = mac_4_3_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_4_5_clock = clock;
  assign mac_4_5_reset = reset;
  assign mac_4_5_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_4_5_io_mulInput = mac_3_5_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_4_5_io_addInput = mac_4_4_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_4_6_clock = clock;
  assign mac_4_6_reset = reset;
  assign mac_4_6_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_4_6_io_mulInput = mac_3_6_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_4_6_io_addInput = mac_4_5_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_4_7_clock = clock;
  assign mac_4_7_reset = reset;
  assign mac_4_7_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_4_7_io_mulInput = mac_3_7_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_4_7_io_addInput = mac_4_6_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_5_0_clock = clock;
  assign mac_5_0_reset = reset;
  assign mac_5_0_io_load = io_load; // @[InnerSystolicArray.scala 60:25]
  assign mac_5_0_io_mulInput = mac_4_0_io_passthrough; // @[InnerSystolicArray.scala 59:29]
  assign mac_5_0_io_addInput = bias_5; // @[InnerSystolicArray.scala 57:27]
  assign mac_5_1_clock = clock;
  assign mac_5_1_reset = reset;
  assign mac_5_1_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_5_1_io_mulInput = mac_4_1_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_5_1_io_addInput = mac_5_0_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_5_2_clock = clock;
  assign mac_5_2_reset = reset;
  assign mac_5_2_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_5_2_io_mulInput = mac_4_2_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_5_2_io_addInput = mac_5_1_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_5_3_clock = clock;
  assign mac_5_3_reset = reset;
  assign mac_5_3_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_5_3_io_mulInput = mac_4_3_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_5_3_io_addInput = mac_5_2_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_5_4_clock = clock;
  assign mac_5_4_reset = reset;
  assign mac_5_4_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_5_4_io_mulInput = mac_4_4_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_5_4_io_addInput = mac_5_3_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_5_5_clock = clock;
  assign mac_5_5_reset = reset;
  assign mac_5_5_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_5_5_io_mulInput = mac_4_5_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_5_5_io_addInput = mac_5_4_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_5_6_clock = clock;
  assign mac_5_6_reset = reset;
  assign mac_5_6_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_5_6_io_mulInput = mac_4_6_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_5_6_io_addInput = mac_5_5_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_5_7_clock = clock;
  assign mac_5_7_reset = reset;
  assign mac_5_7_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_5_7_io_mulInput = mac_4_7_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_5_7_io_addInput = mac_5_6_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_6_0_clock = clock;
  assign mac_6_0_reset = reset;
  assign mac_6_0_io_load = io_load; // @[InnerSystolicArray.scala 60:25]
  assign mac_6_0_io_mulInput = mac_5_0_io_passthrough; // @[InnerSystolicArray.scala 59:29]
  assign mac_6_0_io_addInput = bias_6; // @[InnerSystolicArray.scala 57:27]
  assign mac_6_1_clock = clock;
  assign mac_6_1_reset = reset;
  assign mac_6_1_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_6_1_io_mulInput = mac_5_1_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_6_1_io_addInput = mac_6_0_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_6_2_clock = clock;
  assign mac_6_2_reset = reset;
  assign mac_6_2_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_6_2_io_mulInput = mac_5_2_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_6_2_io_addInput = mac_6_1_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_6_3_clock = clock;
  assign mac_6_3_reset = reset;
  assign mac_6_3_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_6_3_io_mulInput = mac_5_3_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_6_3_io_addInput = mac_6_2_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_6_4_clock = clock;
  assign mac_6_4_reset = reset;
  assign mac_6_4_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_6_4_io_mulInput = mac_5_4_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_6_4_io_addInput = mac_6_3_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_6_5_clock = clock;
  assign mac_6_5_reset = reset;
  assign mac_6_5_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_6_5_io_mulInput = mac_5_5_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_6_5_io_addInput = mac_6_4_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_6_6_clock = clock;
  assign mac_6_6_reset = reset;
  assign mac_6_6_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_6_6_io_mulInput = mac_5_6_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_6_6_io_addInput = mac_6_5_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_6_7_clock = clock;
  assign mac_6_7_reset = reset;
  assign mac_6_7_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_6_7_io_mulInput = mac_5_7_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_6_7_io_addInput = mac_6_6_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_7_0_clock = clock;
  assign mac_7_0_reset = reset;
  assign mac_7_0_io_load = io_load; // @[InnerSystolicArray.scala 60:25]
  assign mac_7_0_io_mulInput = mac_6_0_io_passthrough; // @[InnerSystolicArray.scala 59:29]
  assign mac_7_0_io_addInput = bias_7; // @[InnerSystolicArray.scala 57:27]
  assign mac_7_1_clock = clock;
  assign mac_7_1_reset = reset;
  assign mac_7_1_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_7_1_io_mulInput = mac_6_1_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_7_1_io_addInput = mac_7_0_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_7_2_clock = clock;
  assign mac_7_2_reset = reset;
  assign mac_7_2_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_7_2_io_mulInput = mac_6_2_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_7_2_io_addInput = mac_7_1_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_7_3_clock = clock;
  assign mac_7_3_reset = reset;
  assign mac_7_3_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_7_3_io_mulInput = mac_6_3_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_7_3_io_addInput = mac_7_2_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_7_4_clock = clock;
  assign mac_7_4_reset = reset;
  assign mac_7_4_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_7_4_io_mulInput = mac_6_4_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_7_4_io_addInput = mac_7_3_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_7_5_clock = clock;
  assign mac_7_5_reset = reset;
  assign mac_7_5_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_7_5_io_mulInput = mac_6_5_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_7_5_io_addInput = mac_7_4_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_7_6_clock = clock;
  assign mac_7_6_reset = reset;
  assign mac_7_6_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_7_6_io_mulInput = mac_6_6_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_7_6_io_addInput = mac_7_5_io_output; // @[InnerSystolicArray.scala 68:29]
  assign mac_7_7_clock = clock;
  assign mac_7_7_reset = reset;
  assign mac_7_7_io_load = io_load; // @[InnerSystolicArray.scala 69:25]
  assign mac_7_7_io_mulInput = mac_6_7_io_passthrough; // @[InnerSystolicArray.scala 67:29]
  assign mac_7_7_io_addInput = mac_7_6_io_output; // @[InnerSystolicArray.scala 68:29]
  always @(posedge clock) begin
    if (reset) begin // @[InnerSystolicArray.scala 37:20]
      bias_0 <= 16'sh0; // @[InnerSystolicArray.scala 37:20]
    end else if (io_load) begin // @[InnerSystolicArray.scala 38:19]
      bias_0 <= io_weight_0; // @[InnerSystolicArray.scala 39:9]
    end
    if (reset) begin // @[InnerSystolicArray.scala 37:20]
      bias_1 <= 16'sh0; // @[InnerSystolicArray.scala 37:20]
    end else if (io_load) begin // @[InnerSystolicArray.scala 38:19]
      bias_1 <= io_weight_1; // @[InnerSystolicArray.scala 39:9]
    end
    if (reset) begin // @[InnerSystolicArray.scala 37:20]
      bias_2 <= 16'sh0; // @[InnerSystolicArray.scala 37:20]
    end else if (io_load) begin // @[InnerSystolicArray.scala 38:19]
      bias_2 <= io_weight_2; // @[InnerSystolicArray.scala 39:9]
    end
    if (reset) begin // @[InnerSystolicArray.scala 37:20]
      bias_3 <= 16'sh0; // @[InnerSystolicArray.scala 37:20]
    end else if (io_load) begin // @[InnerSystolicArray.scala 38:19]
      bias_3 <= io_weight_3; // @[InnerSystolicArray.scala 39:9]
    end
    if (reset) begin // @[InnerSystolicArray.scala 37:20]
      bias_4 <= 16'sh0; // @[InnerSystolicArray.scala 37:20]
    end else if (io_load) begin // @[InnerSystolicArray.scala 38:19]
      bias_4 <= io_weight_4; // @[InnerSystolicArray.scala 39:9]
    end
    if (reset) begin // @[InnerSystolicArray.scala 37:20]
      bias_5 <= 16'sh0; // @[InnerSystolicArray.scala 37:20]
    end else if (io_load) begin // @[InnerSystolicArray.scala 38:19]
      bias_5 <= io_weight_5; // @[InnerSystolicArray.scala 39:9]
    end
    if (reset) begin // @[InnerSystolicArray.scala 37:20]
      bias_6 <= 16'sh0; // @[InnerSystolicArray.scala 37:20]
    end else if (io_load) begin // @[InnerSystolicArray.scala 38:19]
      bias_6 <= io_weight_6; // @[InnerSystolicArray.scala 39:9]
    end
    if (reset) begin // @[InnerSystolicArray.scala 37:20]
      bias_7 <= 16'sh0; // @[InnerSystolicArray.scala 37:20]
    end else if (io_load) begin // @[InnerSystolicArray.scala 38:19]
      bias_7 <= io_weight_7; // @[InnerSystolicArray.scala 39:9]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_1_io_mulInput_sr_0 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_1_io_mulInput_sr_0 <= io_input_1; // @[ShiftRegister.scala 25:12]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_2_io_mulInput_sr_0 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_2_io_mulInput_sr_0 <= io_input_2; // @[ShiftRegister.scala 25:12]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_2_io_mulInput_sr_1 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_2_io_mulInput_sr_1 <= mac_0_2_io_mulInput_sr_0; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_3_io_mulInput_sr_0 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_3_io_mulInput_sr_0 <= io_input_3; // @[ShiftRegister.scala 25:12]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_3_io_mulInput_sr_1 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_3_io_mulInput_sr_1 <= mac_0_3_io_mulInput_sr_0; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_3_io_mulInput_sr_2 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_3_io_mulInput_sr_2 <= mac_0_3_io_mulInput_sr_1; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_4_io_mulInput_sr_0 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_4_io_mulInput_sr_0 <= io_input_4; // @[ShiftRegister.scala 25:12]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_4_io_mulInput_sr_1 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_4_io_mulInput_sr_1 <= mac_0_4_io_mulInput_sr_0; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_4_io_mulInput_sr_2 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_4_io_mulInput_sr_2 <= mac_0_4_io_mulInput_sr_1; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_4_io_mulInput_sr_3 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_4_io_mulInput_sr_3 <= mac_0_4_io_mulInput_sr_2; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_5_io_mulInput_sr_0 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_5_io_mulInput_sr_0 <= io_input_5; // @[ShiftRegister.scala 25:12]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_5_io_mulInput_sr_1 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_5_io_mulInput_sr_1 <= mac_0_5_io_mulInput_sr_0; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_5_io_mulInput_sr_2 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_5_io_mulInput_sr_2 <= mac_0_5_io_mulInput_sr_1; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_5_io_mulInput_sr_3 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_5_io_mulInput_sr_3 <= mac_0_5_io_mulInput_sr_2; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_5_io_mulInput_sr_4 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_5_io_mulInput_sr_4 <= mac_0_5_io_mulInput_sr_3; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_6_io_mulInput_sr_0 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_6_io_mulInput_sr_0 <= io_input_6; // @[ShiftRegister.scala 25:12]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_6_io_mulInput_sr_1 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_6_io_mulInput_sr_1 <= mac_0_6_io_mulInput_sr_0; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_6_io_mulInput_sr_2 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_6_io_mulInput_sr_2 <= mac_0_6_io_mulInput_sr_1; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_6_io_mulInput_sr_3 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_6_io_mulInput_sr_3 <= mac_0_6_io_mulInput_sr_2; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_6_io_mulInput_sr_4 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_6_io_mulInput_sr_4 <= mac_0_6_io_mulInput_sr_3; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_6_io_mulInput_sr_5 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_6_io_mulInput_sr_5 <= mac_0_6_io_mulInput_sr_4; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_7_io_mulInput_sr_0 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_7_io_mulInput_sr_0 <= io_input_7; // @[ShiftRegister.scala 25:12]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_7_io_mulInput_sr_1 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_7_io_mulInput_sr_1 <= mac_0_7_io_mulInput_sr_0; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_7_io_mulInput_sr_2 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_7_io_mulInput_sr_2 <= mac_0_7_io_mulInput_sr_1; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_7_io_mulInput_sr_3 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_7_io_mulInput_sr_3 <= mac_0_7_io_mulInput_sr_2; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_7_io_mulInput_sr_4 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_7_io_mulInput_sr_4 <= mac_0_7_io_mulInput_sr_3; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_7_io_mulInput_sr_5 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_7_io_mulInput_sr_5 <= mac_0_7_io_mulInput_sr_4; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      mac_0_7_io_mulInput_sr_6 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      mac_0_7_io_mulInput_sr_6 <= mac_0_7_io_mulInput_sr_5; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_0_sr_0 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_0_sr_0 <= mac_0_7_io_output; // @[ShiftRegister.scala 25:12]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_0_sr_1 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_0_sr_1 <= io_output_0_sr_0; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_0_sr_2 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_0_sr_2 <= io_output_0_sr_1; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_0_sr_3 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_0_sr_3 <= io_output_0_sr_2; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_0_sr_4 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_0_sr_4 <= io_output_0_sr_3; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_0_sr_5 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_0_sr_5 <= io_output_0_sr_4; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_0_sr_6 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_0_sr_6 <= io_output_0_sr_5; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_1_sr_0 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_1_sr_0 <= mac_1_7_io_output; // @[ShiftRegister.scala 25:12]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_1_sr_1 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_1_sr_1 <= io_output_1_sr_0; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_1_sr_2 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_1_sr_2 <= io_output_1_sr_1; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_1_sr_3 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_1_sr_3 <= io_output_1_sr_2; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_1_sr_4 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_1_sr_4 <= io_output_1_sr_3; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_1_sr_5 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_1_sr_5 <= io_output_1_sr_4; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_2_sr_0 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_2_sr_0 <= mac_2_7_io_output; // @[ShiftRegister.scala 25:12]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_2_sr_1 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_2_sr_1 <= io_output_2_sr_0; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_2_sr_2 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_2_sr_2 <= io_output_2_sr_1; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_2_sr_3 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_2_sr_3 <= io_output_2_sr_2; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_2_sr_4 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_2_sr_4 <= io_output_2_sr_3; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_3_sr_0 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_3_sr_0 <= mac_3_7_io_output; // @[ShiftRegister.scala 25:12]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_3_sr_1 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_3_sr_1 <= io_output_3_sr_0; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_3_sr_2 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_3_sr_2 <= io_output_3_sr_1; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_3_sr_3 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_3_sr_3 <= io_output_3_sr_2; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_4_sr_0 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_4_sr_0 <= mac_4_7_io_output; // @[ShiftRegister.scala 25:12]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_4_sr_1 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_4_sr_1 <= io_output_4_sr_0; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_4_sr_2 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_4_sr_2 <= io_output_4_sr_1; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_5_sr_0 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_5_sr_0 <= mac_5_7_io_output; // @[ShiftRegister.scala 25:12]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_5_sr_1 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_5_sr_1 <= io_output_5_sr_0; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      io_output_6_sr_0 <= 16'sh0; // @[ShiftRegister.scala 10:22]
    end else begin
      io_output_6_sr_0 <= mac_6_7_io_output; // @[ShiftRegister.scala 25:12]
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
  bias_0 = _RAND_0[15:0];
  _RAND_1 = {1{`RANDOM}};
  bias_1 = _RAND_1[15:0];
  _RAND_2 = {1{`RANDOM}};
  bias_2 = _RAND_2[15:0];
  _RAND_3 = {1{`RANDOM}};
  bias_3 = _RAND_3[15:0];
  _RAND_4 = {1{`RANDOM}};
  bias_4 = _RAND_4[15:0];
  _RAND_5 = {1{`RANDOM}};
  bias_5 = _RAND_5[15:0];
  _RAND_6 = {1{`RANDOM}};
  bias_6 = _RAND_6[15:0];
  _RAND_7 = {1{`RANDOM}};
  bias_7 = _RAND_7[15:0];
  _RAND_8 = {1{`RANDOM}};
  mac_0_1_io_mulInput_sr_0 = _RAND_8[15:0];
  _RAND_9 = {1{`RANDOM}};
  mac_0_2_io_mulInput_sr_0 = _RAND_9[15:0];
  _RAND_10 = {1{`RANDOM}};
  mac_0_2_io_mulInput_sr_1 = _RAND_10[15:0];
  _RAND_11 = {1{`RANDOM}};
  mac_0_3_io_mulInput_sr_0 = _RAND_11[15:0];
  _RAND_12 = {1{`RANDOM}};
  mac_0_3_io_mulInput_sr_1 = _RAND_12[15:0];
  _RAND_13 = {1{`RANDOM}};
  mac_0_3_io_mulInput_sr_2 = _RAND_13[15:0];
  _RAND_14 = {1{`RANDOM}};
  mac_0_4_io_mulInput_sr_0 = _RAND_14[15:0];
  _RAND_15 = {1{`RANDOM}};
  mac_0_4_io_mulInput_sr_1 = _RAND_15[15:0];
  _RAND_16 = {1{`RANDOM}};
  mac_0_4_io_mulInput_sr_2 = _RAND_16[15:0];
  _RAND_17 = {1{`RANDOM}};
  mac_0_4_io_mulInput_sr_3 = _RAND_17[15:0];
  _RAND_18 = {1{`RANDOM}};
  mac_0_5_io_mulInput_sr_0 = _RAND_18[15:0];
  _RAND_19 = {1{`RANDOM}};
  mac_0_5_io_mulInput_sr_1 = _RAND_19[15:0];
  _RAND_20 = {1{`RANDOM}};
  mac_0_5_io_mulInput_sr_2 = _RAND_20[15:0];
  _RAND_21 = {1{`RANDOM}};
  mac_0_5_io_mulInput_sr_3 = _RAND_21[15:0];
  _RAND_22 = {1{`RANDOM}};
  mac_0_5_io_mulInput_sr_4 = _RAND_22[15:0];
  _RAND_23 = {1{`RANDOM}};
  mac_0_6_io_mulInput_sr_0 = _RAND_23[15:0];
  _RAND_24 = {1{`RANDOM}};
  mac_0_6_io_mulInput_sr_1 = _RAND_24[15:0];
  _RAND_25 = {1{`RANDOM}};
  mac_0_6_io_mulInput_sr_2 = _RAND_25[15:0];
  _RAND_26 = {1{`RANDOM}};
  mac_0_6_io_mulInput_sr_3 = _RAND_26[15:0];
  _RAND_27 = {1{`RANDOM}};
  mac_0_6_io_mulInput_sr_4 = _RAND_27[15:0];
  _RAND_28 = {1{`RANDOM}};
  mac_0_6_io_mulInput_sr_5 = _RAND_28[15:0];
  _RAND_29 = {1{`RANDOM}};
  mac_0_7_io_mulInput_sr_0 = _RAND_29[15:0];
  _RAND_30 = {1{`RANDOM}};
  mac_0_7_io_mulInput_sr_1 = _RAND_30[15:0];
  _RAND_31 = {1{`RANDOM}};
  mac_0_7_io_mulInput_sr_2 = _RAND_31[15:0];
  _RAND_32 = {1{`RANDOM}};
  mac_0_7_io_mulInput_sr_3 = _RAND_32[15:0];
  _RAND_33 = {1{`RANDOM}};
  mac_0_7_io_mulInput_sr_4 = _RAND_33[15:0];
  _RAND_34 = {1{`RANDOM}};
  mac_0_7_io_mulInput_sr_5 = _RAND_34[15:0];
  _RAND_35 = {1{`RANDOM}};
  mac_0_7_io_mulInput_sr_6 = _RAND_35[15:0];
  _RAND_36 = {1{`RANDOM}};
  io_output_0_sr_0 = _RAND_36[15:0];
  _RAND_37 = {1{`RANDOM}};
  io_output_0_sr_1 = _RAND_37[15:0];
  _RAND_38 = {1{`RANDOM}};
  io_output_0_sr_2 = _RAND_38[15:0];
  _RAND_39 = {1{`RANDOM}};
  io_output_0_sr_3 = _RAND_39[15:0];
  _RAND_40 = {1{`RANDOM}};
  io_output_0_sr_4 = _RAND_40[15:0];
  _RAND_41 = {1{`RANDOM}};
  io_output_0_sr_5 = _RAND_41[15:0];
  _RAND_42 = {1{`RANDOM}};
  io_output_0_sr_6 = _RAND_42[15:0];
  _RAND_43 = {1{`RANDOM}};
  io_output_1_sr_0 = _RAND_43[15:0];
  _RAND_44 = {1{`RANDOM}};
  io_output_1_sr_1 = _RAND_44[15:0];
  _RAND_45 = {1{`RANDOM}};
  io_output_1_sr_2 = _RAND_45[15:0];
  _RAND_46 = {1{`RANDOM}};
  io_output_1_sr_3 = _RAND_46[15:0];
  _RAND_47 = {1{`RANDOM}};
  io_output_1_sr_4 = _RAND_47[15:0];
  _RAND_48 = {1{`RANDOM}};
  io_output_1_sr_5 = _RAND_48[15:0];
  _RAND_49 = {1{`RANDOM}};
  io_output_2_sr_0 = _RAND_49[15:0];
  _RAND_50 = {1{`RANDOM}};
  io_output_2_sr_1 = _RAND_50[15:0];
  _RAND_51 = {1{`RANDOM}};
  io_output_2_sr_2 = _RAND_51[15:0];
  _RAND_52 = {1{`RANDOM}};
  io_output_2_sr_3 = _RAND_52[15:0];
  _RAND_53 = {1{`RANDOM}};
  io_output_2_sr_4 = _RAND_53[15:0];
  _RAND_54 = {1{`RANDOM}};
  io_output_3_sr_0 = _RAND_54[15:0];
  _RAND_55 = {1{`RANDOM}};
  io_output_3_sr_1 = _RAND_55[15:0];
  _RAND_56 = {1{`RANDOM}};
  io_output_3_sr_2 = _RAND_56[15:0];
  _RAND_57 = {1{`RANDOM}};
  io_output_3_sr_3 = _RAND_57[15:0];
  _RAND_58 = {1{`RANDOM}};
  io_output_4_sr_0 = _RAND_58[15:0];
  _RAND_59 = {1{`RANDOM}};
  io_output_4_sr_1 = _RAND_59[15:0];
  _RAND_60 = {1{`RANDOM}};
  io_output_4_sr_2 = _RAND_60[15:0];
  _RAND_61 = {1{`RANDOM}};
  io_output_5_sr_0 = _RAND_61[15:0];
  _RAND_62 = {1{`RANDOM}};
  io_output_5_sr_1 = _RAND_62[15:0];
  _RAND_63 = {1{`RANDOM}};
  io_output_6_sr_0 = _RAND_63[15:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Queue_4(
  input         clock,
  input         reset,
  output        io_enq_ready,
  input         io_enq_valid,
  input  [15:0] io_enq_bits_0,
  input  [15:0] io_enq_bits_1,
  input  [15:0] io_enq_bits_2,
  input  [15:0] io_enq_bits_3,
  input  [15:0] io_enq_bits_4,
  input  [15:0] io_enq_bits_5,
  input  [15:0] io_enq_bits_6,
  input  [15:0] io_enq_bits_7,
  input         io_deq_ready,
  output        io_deq_valid,
  output [15:0] io_deq_bits_0,
  output [15:0] io_deq_bits_1,
  output [15:0] io_deq_bits_2,
  output [15:0] io_deq_bits_3,
  output [15:0] io_deq_bits_4,
  output [15:0] io_deq_bits_5,
  output [15:0] io_deq_bits_6,
  output [15:0] io_deq_bits_7
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_6;
  reg [31:0] _RAND_7;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_8;
  reg [31:0] _RAND_9;
  reg [31:0] _RAND_10;
`endif // RANDOMIZE_REG_INIT
  reg [15:0] ram_0 [0:1]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_0_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_0_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_0_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_0_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_0_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_0_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_1 [0:1]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_1_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_1_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_1_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_1_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_1_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_1_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_2 [0:1]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_2_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_2_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_2_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_2_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_2_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_2_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_3 [0:1]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_3_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_3_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_3_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_3_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_3_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_3_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_4 [0:1]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_4_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_4_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_4_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_4_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_4_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_4_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_5 [0:1]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_5_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_5_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_5_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_5_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_5_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_5_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_6 [0:1]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_6_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_6_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_6_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_6_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_6_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_6_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_7 [0:1]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_7_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_7_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_7_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_7_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_7_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_7_MPORT_en; // @[Decoupled.scala 218:16]
  reg  value; // @[Counter.scala 60:40]
  reg  value_1; // @[Counter.scala 60:40]
  reg  maybe_full; // @[Decoupled.scala 221:27]
  wire  ptr_match = value == value_1; // @[Decoupled.scala 223:33]
  wire  empty = ptr_match & ~maybe_full; // @[Decoupled.scala 224:25]
  wire  full = ptr_match & maybe_full; // @[Decoupled.scala 225:24]
  wire  do_enq = io_enq_ready & io_enq_valid; // @[Decoupled.scala 40:37]
  wire  do_deq = io_deq_ready & io_deq_valid; // @[Decoupled.scala 40:37]
  assign ram_0_io_deq_bits_MPORT_addr = value_1;
  assign ram_0_io_deq_bits_MPORT_data = ram_0[ram_0_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_0_MPORT_data = io_enq_bits_0;
  assign ram_0_MPORT_addr = value;
  assign ram_0_MPORT_mask = 1'h1;
  assign ram_0_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_1_io_deq_bits_MPORT_addr = value_1;
  assign ram_1_io_deq_bits_MPORT_data = ram_1[ram_1_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_1_MPORT_data = io_enq_bits_1;
  assign ram_1_MPORT_addr = value;
  assign ram_1_MPORT_mask = 1'h1;
  assign ram_1_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_2_io_deq_bits_MPORT_addr = value_1;
  assign ram_2_io_deq_bits_MPORT_data = ram_2[ram_2_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_2_MPORT_data = io_enq_bits_2;
  assign ram_2_MPORT_addr = value;
  assign ram_2_MPORT_mask = 1'h1;
  assign ram_2_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_3_io_deq_bits_MPORT_addr = value_1;
  assign ram_3_io_deq_bits_MPORT_data = ram_3[ram_3_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_3_MPORT_data = io_enq_bits_3;
  assign ram_3_MPORT_addr = value;
  assign ram_3_MPORT_mask = 1'h1;
  assign ram_3_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_4_io_deq_bits_MPORT_addr = value_1;
  assign ram_4_io_deq_bits_MPORT_data = ram_4[ram_4_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_4_MPORT_data = io_enq_bits_4;
  assign ram_4_MPORT_addr = value;
  assign ram_4_MPORT_mask = 1'h1;
  assign ram_4_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_5_io_deq_bits_MPORT_addr = value_1;
  assign ram_5_io_deq_bits_MPORT_data = ram_5[ram_5_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_5_MPORT_data = io_enq_bits_5;
  assign ram_5_MPORT_addr = value;
  assign ram_5_MPORT_mask = 1'h1;
  assign ram_5_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_6_io_deq_bits_MPORT_addr = value_1;
  assign ram_6_io_deq_bits_MPORT_data = ram_6[ram_6_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_6_MPORT_data = io_enq_bits_6;
  assign ram_6_MPORT_addr = value;
  assign ram_6_MPORT_mask = 1'h1;
  assign ram_6_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_7_io_deq_bits_MPORT_addr = value_1;
  assign ram_7_io_deq_bits_MPORT_data = ram_7[ram_7_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_7_MPORT_data = io_enq_bits_7;
  assign ram_7_MPORT_addr = value;
  assign ram_7_MPORT_mask = 1'h1;
  assign ram_7_MPORT_en = io_enq_ready & io_enq_valid;
  assign io_enq_ready = ~full; // @[Decoupled.scala 241:19]
  assign io_deq_valid = ~empty; // @[Decoupled.scala 240:19]
  assign io_deq_bits_0 = ram_0_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_1 = ram_1_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_2 = ram_2_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_3 = ram_3_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_4 = ram_4_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_5 = ram_5_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_6 = ram_6_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_7 = ram_7_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  always @(posedge clock) begin
    if(ram_0_MPORT_en & ram_0_MPORT_mask) begin
      ram_0[ram_0_MPORT_addr] <= ram_0_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_1_MPORT_en & ram_1_MPORT_mask) begin
      ram_1[ram_1_MPORT_addr] <= ram_1_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_2_MPORT_en & ram_2_MPORT_mask) begin
      ram_2[ram_2_MPORT_addr] <= ram_2_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_3_MPORT_en & ram_3_MPORT_mask) begin
      ram_3[ram_3_MPORT_addr] <= ram_3_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_4_MPORT_en & ram_4_MPORT_mask) begin
      ram_4[ram_4_MPORT_addr] <= ram_4_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_5_MPORT_en & ram_5_MPORT_mask) begin
      ram_5[ram_5_MPORT_addr] <= ram_5_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_6_MPORT_en & ram_6_MPORT_mask) begin
      ram_6[ram_6_MPORT_addr] <= ram_6_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_7_MPORT_en & ram_7_MPORT_mask) begin
      ram_7[ram_7_MPORT_addr] <= ram_7_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if (reset) begin // @[Counter.scala 60:40]
      value <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_enq) begin // @[Decoupled.scala 229:17]
      value <= value + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Counter.scala 60:40]
      value_1 <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_deq) begin // @[Decoupled.scala 233:17]
      value_1 <= value_1 + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Decoupled.scala 221:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 221:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 236:28]
      maybe_full <= do_enq; // @[Decoupled.scala 237:16]
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
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_0[initvar] = _RAND_0[15:0];
  _RAND_1 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_1[initvar] = _RAND_1[15:0];
  _RAND_2 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_2[initvar] = _RAND_2[15:0];
  _RAND_3 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_3[initvar] = _RAND_3[15:0];
  _RAND_4 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_4[initvar] = _RAND_4[15:0];
  _RAND_5 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_5[initvar] = _RAND_5[15:0];
  _RAND_6 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_6[initvar] = _RAND_6[15:0];
  _RAND_7 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_7[initvar] = _RAND_7[15:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_8 = {1{`RANDOM}};
  value = _RAND_8[0:0];
  _RAND_9 = {1{`RANDOM}};
  value_1 = _RAND_9[0:0];
  _RAND_10 = {1{`RANDOM}};
  maybe_full = _RAND_10[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Queue_5(
  input         clock,
  input         reset,
  output        io_enq_ready,
  input         io_enq_valid,
  input  [15:0] io_enq_bits_0,
  input  [15:0] io_enq_bits_1,
  input  [15:0] io_enq_bits_2,
  input  [15:0] io_enq_bits_3,
  input  [15:0] io_enq_bits_4,
  input  [15:0] io_enq_bits_5,
  input  [15:0] io_enq_bits_6,
  input  [15:0] io_enq_bits_7,
  input         io_deq_ready,
  output        io_deq_valid,
  output [15:0] io_deq_bits_0,
  output [15:0] io_deq_bits_1,
  output [15:0] io_deq_bits_2,
  output [15:0] io_deq_bits_3,
  output [15:0] io_deq_bits_4,
  output [15:0] io_deq_bits_5,
  output [15:0] io_deq_bits_6,
  output [15:0] io_deq_bits_7
);
`ifdef RANDOMIZE_GARBAGE_ASSIGN
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_7;
  reg [31:0] _RAND_9;
  reg [31:0] _RAND_11;
  reg [31:0] _RAND_13;
  reg [31:0] _RAND_15;
`endif // RANDOMIZE_GARBAGE_ASSIGN
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_6;
  reg [31:0] _RAND_8;
  reg [31:0] _RAND_10;
  reg [31:0] _RAND_12;
  reg [31:0] _RAND_14;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_16;
  reg [31:0] _RAND_17;
  reg [31:0] _RAND_18;
`endif // RANDOMIZE_REG_INIT
  reg [15:0] ram_0 [0:14]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_0_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire [3:0] ram_0_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_0_MPORT_data; // @[Decoupled.scala 218:16]
  wire [3:0] ram_0_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_0_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_0_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_1 [0:14]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_1_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire [3:0] ram_1_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_1_MPORT_data; // @[Decoupled.scala 218:16]
  wire [3:0] ram_1_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_1_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_1_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_2 [0:14]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_2_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire [3:0] ram_2_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_2_MPORT_data; // @[Decoupled.scala 218:16]
  wire [3:0] ram_2_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_2_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_2_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_3 [0:14]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_3_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire [3:0] ram_3_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_3_MPORT_data; // @[Decoupled.scala 218:16]
  wire [3:0] ram_3_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_3_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_3_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_4 [0:14]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_4_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire [3:0] ram_4_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_4_MPORT_data; // @[Decoupled.scala 218:16]
  wire [3:0] ram_4_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_4_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_4_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_5 [0:14]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_5_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire [3:0] ram_5_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_5_MPORT_data; // @[Decoupled.scala 218:16]
  wire [3:0] ram_5_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_5_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_5_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_6 [0:14]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_6_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire [3:0] ram_6_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_6_MPORT_data; // @[Decoupled.scala 218:16]
  wire [3:0] ram_6_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_6_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_6_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_7 [0:14]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_7_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire [3:0] ram_7_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_7_MPORT_data; // @[Decoupled.scala 218:16]
  wire [3:0] ram_7_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_7_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_7_MPORT_en; // @[Decoupled.scala 218:16]
  reg [3:0] enq_ptr_value; // @[Counter.scala 60:40]
  reg [3:0] deq_ptr_value; // @[Counter.scala 60:40]
  reg  maybe_full; // @[Decoupled.scala 221:27]
  wire  ptr_match = enq_ptr_value == deq_ptr_value; // @[Decoupled.scala 223:33]
  wire  empty = ptr_match & ~maybe_full; // @[Decoupled.scala 224:25]
  wire  full = ptr_match & maybe_full; // @[Decoupled.scala 225:24]
  wire  _do_enq_T = io_enq_ready & io_enq_valid; // @[Decoupled.scala 40:37]
  wire  _do_deq_T = io_deq_ready & io_deq_valid; // @[Decoupled.scala 40:37]
  wire  wrap = enq_ptr_value == 4'he; // @[Counter.scala 72:24]
  wire [3:0] _value_T_1 = enq_ptr_value + 4'h1; // @[Counter.scala 76:24]
  wire  _GEN_18 = io_deq_ready ? 1'h0 : _do_enq_T; // @[Decoupled.scala 249:27 Decoupled.scala 249:36]
  wire  do_enq = empty ? _GEN_18 : _do_enq_T; // @[Decoupled.scala 246:18]
  wire  wrap_1 = deq_ptr_value == 4'he; // @[Counter.scala 72:24]
  wire [3:0] _value_T_3 = deq_ptr_value + 4'h1; // @[Counter.scala 76:24]
  wire  do_deq = empty ? 1'h0 : _do_deq_T; // @[Decoupled.scala 246:18 Decoupled.scala 248:14]
  assign ram_0_io_deq_bits_MPORT_addr = deq_ptr_value;
  `ifndef RANDOMIZE_GARBAGE_ASSIGN
  assign ram_0_io_deq_bits_MPORT_data = ram_0[ram_0_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `else
  assign ram_0_io_deq_bits_MPORT_data = ram_0_io_deq_bits_MPORT_addr >= 4'hf ? _RAND_1[15:0] :
    ram_0[ram_0_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `endif // RANDOMIZE_GARBAGE_ASSIGN
  assign ram_0_MPORT_data = io_enq_bits_0;
  assign ram_0_MPORT_addr = enq_ptr_value;
  assign ram_0_MPORT_mask = 1'h1;
  assign ram_0_MPORT_en = empty ? _GEN_18 : _do_enq_T;
  assign ram_1_io_deq_bits_MPORT_addr = deq_ptr_value;
  `ifndef RANDOMIZE_GARBAGE_ASSIGN
  assign ram_1_io_deq_bits_MPORT_data = ram_1[ram_1_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `else
  assign ram_1_io_deq_bits_MPORT_data = ram_1_io_deq_bits_MPORT_addr >= 4'hf ? _RAND_3[15:0] :
    ram_1[ram_1_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `endif // RANDOMIZE_GARBAGE_ASSIGN
  assign ram_1_MPORT_data = io_enq_bits_1;
  assign ram_1_MPORT_addr = enq_ptr_value;
  assign ram_1_MPORT_mask = 1'h1;
  assign ram_1_MPORT_en = empty ? _GEN_18 : _do_enq_T;
  assign ram_2_io_deq_bits_MPORT_addr = deq_ptr_value;
  `ifndef RANDOMIZE_GARBAGE_ASSIGN
  assign ram_2_io_deq_bits_MPORT_data = ram_2[ram_2_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `else
  assign ram_2_io_deq_bits_MPORT_data = ram_2_io_deq_bits_MPORT_addr >= 4'hf ? _RAND_5[15:0] :
    ram_2[ram_2_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `endif // RANDOMIZE_GARBAGE_ASSIGN
  assign ram_2_MPORT_data = io_enq_bits_2;
  assign ram_2_MPORT_addr = enq_ptr_value;
  assign ram_2_MPORT_mask = 1'h1;
  assign ram_2_MPORT_en = empty ? _GEN_18 : _do_enq_T;
  assign ram_3_io_deq_bits_MPORT_addr = deq_ptr_value;
  `ifndef RANDOMIZE_GARBAGE_ASSIGN
  assign ram_3_io_deq_bits_MPORT_data = ram_3[ram_3_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `else
  assign ram_3_io_deq_bits_MPORT_data = ram_3_io_deq_bits_MPORT_addr >= 4'hf ? _RAND_7[15:0] :
    ram_3[ram_3_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `endif // RANDOMIZE_GARBAGE_ASSIGN
  assign ram_3_MPORT_data = io_enq_bits_3;
  assign ram_3_MPORT_addr = enq_ptr_value;
  assign ram_3_MPORT_mask = 1'h1;
  assign ram_3_MPORT_en = empty ? _GEN_18 : _do_enq_T;
  assign ram_4_io_deq_bits_MPORT_addr = deq_ptr_value;
  `ifndef RANDOMIZE_GARBAGE_ASSIGN
  assign ram_4_io_deq_bits_MPORT_data = ram_4[ram_4_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `else
  assign ram_4_io_deq_bits_MPORT_data = ram_4_io_deq_bits_MPORT_addr >= 4'hf ? _RAND_9[15:0] :
    ram_4[ram_4_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `endif // RANDOMIZE_GARBAGE_ASSIGN
  assign ram_4_MPORT_data = io_enq_bits_4;
  assign ram_4_MPORT_addr = enq_ptr_value;
  assign ram_4_MPORT_mask = 1'h1;
  assign ram_4_MPORT_en = empty ? _GEN_18 : _do_enq_T;
  assign ram_5_io_deq_bits_MPORT_addr = deq_ptr_value;
  `ifndef RANDOMIZE_GARBAGE_ASSIGN
  assign ram_5_io_deq_bits_MPORT_data = ram_5[ram_5_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `else
  assign ram_5_io_deq_bits_MPORT_data = ram_5_io_deq_bits_MPORT_addr >= 4'hf ? _RAND_11[15:0] :
    ram_5[ram_5_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `endif // RANDOMIZE_GARBAGE_ASSIGN
  assign ram_5_MPORT_data = io_enq_bits_5;
  assign ram_5_MPORT_addr = enq_ptr_value;
  assign ram_5_MPORT_mask = 1'h1;
  assign ram_5_MPORT_en = empty ? _GEN_18 : _do_enq_T;
  assign ram_6_io_deq_bits_MPORT_addr = deq_ptr_value;
  `ifndef RANDOMIZE_GARBAGE_ASSIGN
  assign ram_6_io_deq_bits_MPORT_data = ram_6[ram_6_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `else
  assign ram_6_io_deq_bits_MPORT_data = ram_6_io_deq_bits_MPORT_addr >= 4'hf ? _RAND_13[15:0] :
    ram_6[ram_6_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `endif // RANDOMIZE_GARBAGE_ASSIGN
  assign ram_6_MPORT_data = io_enq_bits_6;
  assign ram_6_MPORT_addr = enq_ptr_value;
  assign ram_6_MPORT_mask = 1'h1;
  assign ram_6_MPORT_en = empty ? _GEN_18 : _do_enq_T;
  assign ram_7_io_deq_bits_MPORT_addr = deq_ptr_value;
  `ifndef RANDOMIZE_GARBAGE_ASSIGN
  assign ram_7_io_deq_bits_MPORT_data = ram_7[ram_7_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `else
  assign ram_7_io_deq_bits_MPORT_data = ram_7_io_deq_bits_MPORT_addr >= 4'hf ? _RAND_15[15:0] :
    ram_7[ram_7_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `endif // RANDOMIZE_GARBAGE_ASSIGN
  assign ram_7_MPORT_data = io_enq_bits_7;
  assign ram_7_MPORT_addr = enq_ptr_value;
  assign ram_7_MPORT_mask = 1'h1;
  assign ram_7_MPORT_en = empty ? _GEN_18 : _do_enq_T;
  assign io_enq_ready = ~full; // @[Decoupled.scala 241:19]
  assign io_deq_valid = io_enq_valid | ~empty; // @[Decoupled.scala 245:25 Decoupled.scala 245:40 Decoupled.scala 240:16]
  assign io_deq_bits_0 = empty ? $signed(io_enq_bits_0) : $signed(ram_0_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_1 = empty ? $signed(io_enq_bits_1) : $signed(ram_1_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_2 = empty ? $signed(io_enq_bits_2) : $signed(ram_2_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_3 = empty ? $signed(io_enq_bits_3) : $signed(ram_3_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_4 = empty ? $signed(io_enq_bits_4) : $signed(ram_4_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_5 = empty ? $signed(io_enq_bits_5) : $signed(ram_5_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_6 = empty ? $signed(io_enq_bits_6) : $signed(ram_6_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_7 = empty ? $signed(io_enq_bits_7) : $signed(ram_7_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  always @(posedge clock) begin
    if(ram_0_MPORT_en & ram_0_MPORT_mask) begin
      ram_0[ram_0_MPORT_addr] <= ram_0_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_1_MPORT_en & ram_1_MPORT_mask) begin
      ram_1[ram_1_MPORT_addr] <= ram_1_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_2_MPORT_en & ram_2_MPORT_mask) begin
      ram_2[ram_2_MPORT_addr] <= ram_2_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_3_MPORT_en & ram_3_MPORT_mask) begin
      ram_3[ram_3_MPORT_addr] <= ram_3_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_4_MPORT_en & ram_4_MPORT_mask) begin
      ram_4[ram_4_MPORT_addr] <= ram_4_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_5_MPORT_en & ram_5_MPORT_mask) begin
      ram_5[ram_5_MPORT_addr] <= ram_5_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_6_MPORT_en & ram_6_MPORT_mask) begin
      ram_6[ram_6_MPORT_addr] <= ram_6_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_7_MPORT_en & ram_7_MPORT_mask) begin
      ram_7[ram_7_MPORT_addr] <= ram_7_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if (reset) begin // @[Counter.scala 60:40]
      enq_ptr_value <= 4'h0; // @[Counter.scala 60:40]
    end else if (do_enq) begin // @[Decoupled.scala 229:17]
      if (wrap) begin // @[Counter.scala 86:20]
        enq_ptr_value <= 4'h0; // @[Counter.scala 86:28]
      end else begin
        enq_ptr_value <= _value_T_1; // @[Counter.scala 76:15]
      end
    end
    if (reset) begin // @[Counter.scala 60:40]
      deq_ptr_value <= 4'h0; // @[Counter.scala 60:40]
    end else if (do_deq) begin // @[Decoupled.scala 233:17]
      if (wrap_1) begin // @[Counter.scala 86:20]
        deq_ptr_value <= 4'h0; // @[Counter.scala 86:28]
      end else begin
        deq_ptr_value <= _value_T_3; // @[Counter.scala 76:15]
      end
    end
    if (reset) begin // @[Decoupled.scala 221:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 221:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 236:28]
      if (empty) begin // @[Decoupled.scala 246:18]
        if (io_deq_ready) begin // @[Decoupled.scala 249:27]
          maybe_full <= 1'h0; // @[Decoupled.scala 249:36]
        end else begin
          maybe_full <= _do_enq_T;
        end
      end else begin
        maybe_full <= _do_enq_T;
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
`ifdef RANDOMIZE_GARBAGE_ASSIGN
  _RAND_1 = {1{`RANDOM}};
  _RAND_3 = {1{`RANDOM}};
  _RAND_5 = {1{`RANDOM}};
  _RAND_7 = {1{`RANDOM}};
  _RAND_9 = {1{`RANDOM}};
  _RAND_11 = {1{`RANDOM}};
  _RAND_13 = {1{`RANDOM}};
  _RAND_15 = {1{`RANDOM}};
`endif // RANDOMIZE_GARBAGE_ASSIGN
`ifdef RANDOMIZE_MEM_INIT
  _RAND_0 = {1{`RANDOM}};
  for (initvar = 0; initvar < 15; initvar = initvar+1)
    ram_0[initvar] = _RAND_0[15:0];
  _RAND_2 = {1{`RANDOM}};
  for (initvar = 0; initvar < 15; initvar = initvar+1)
    ram_1[initvar] = _RAND_2[15:0];
  _RAND_4 = {1{`RANDOM}};
  for (initvar = 0; initvar < 15; initvar = initvar+1)
    ram_2[initvar] = _RAND_4[15:0];
  _RAND_6 = {1{`RANDOM}};
  for (initvar = 0; initvar < 15; initvar = initvar+1)
    ram_3[initvar] = _RAND_6[15:0];
  _RAND_8 = {1{`RANDOM}};
  for (initvar = 0; initvar < 15; initvar = initvar+1)
    ram_4[initvar] = _RAND_8[15:0];
  _RAND_10 = {1{`RANDOM}};
  for (initvar = 0; initvar < 15; initvar = initvar+1)
    ram_5[initvar] = _RAND_10[15:0];
  _RAND_12 = {1{`RANDOM}};
  for (initvar = 0; initvar < 15; initvar = initvar+1)
    ram_6[initvar] = _RAND_12[15:0];
  _RAND_14 = {1{`RANDOM}};
  for (initvar = 0; initvar < 15; initvar = initvar+1)
    ram_7[initvar] = _RAND_14[15:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_16 = {1{`RANDOM}};
  enq_ptr_value = _RAND_16[3:0];
  _RAND_17 = {1{`RANDOM}};
  deq_ptr_value = _RAND_17[3:0];
  _RAND_18 = {1{`RANDOM}};
  maybe_full = _RAND_18[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module SystolicArray(
  input         clock,
  input         reset,
  output        io_control_ready,
  input         io_control_valid,
  input         io_control_bits_load,
  input         io_control_bits_zeroes,
  output        io_input_ready,
  input         io_input_valid,
  input  [15:0] io_input_bits_0,
  input  [15:0] io_input_bits_1,
  input  [15:0] io_input_bits_2,
  input  [15:0] io_input_bits_3,
  input  [15:0] io_input_bits_4,
  input  [15:0] io_input_bits_5,
  input  [15:0] io_input_bits_6,
  input  [15:0] io_input_bits_7,
  output        io_weight_ready,
  input         io_weight_valid,
  input  [15:0] io_weight_bits_0,
  input  [15:0] io_weight_bits_1,
  input  [15:0] io_weight_bits_2,
  input  [15:0] io_weight_bits_3,
  input  [15:0] io_weight_bits_4,
  input  [15:0] io_weight_bits_5,
  input  [15:0] io_weight_bits_6,
  input  [15:0] io_weight_bits_7,
  input         io_output_ready,
  output        io_output_valid,
  output [15:0] io_output_bits_0,
  output [15:0] io_output_bits_1,
  output [15:0] io_output_bits_2,
  output [15:0] io_output_bits_3,
  output [15:0] io_output_bits_4,
  output [15:0] io_output_bits_5,
  output [15:0] io_output_bits_6,
  output [15:0] io_output_bits_7
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
`endif // RANDOMIZE_REG_INIT
  wire  array_clock; // @[SystolicArray.scala 40:37]
  wire  array_reset; // @[SystolicArray.scala 40:37]
  wire  array_io_load; // @[SystolicArray.scala 40:37]
  wire [15:0] array_io_input_0; // @[SystolicArray.scala 40:37]
  wire [15:0] array_io_input_1; // @[SystolicArray.scala 40:37]
  wire [15:0] array_io_input_2; // @[SystolicArray.scala 40:37]
  wire [15:0] array_io_input_3; // @[SystolicArray.scala 40:37]
  wire [15:0] array_io_input_4; // @[SystolicArray.scala 40:37]
  wire [15:0] array_io_input_5; // @[SystolicArray.scala 40:37]
  wire [15:0] array_io_input_6; // @[SystolicArray.scala 40:37]
  wire [15:0] array_io_input_7; // @[SystolicArray.scala 40:37]
  wire [15:0] array_io_weight_0; // @[SystolicArray.scala 40:37]
  wire [15:0] array_io_weight_1; // @[SystolicArray.scala 40:37]
  wire [15:0] array_io_weight_2; // @[SystolicArray.scala 40:37]
  wire [15:0] array_io_weight_3; // @[SystolicArray.scala 40:37]
  wire [15:0] array_io_weight_4; // @[SystolicArray.scala 40:37]
  wire [15:0] array_io_weight_5; // @[SystolicArray.scala 40:37]
  wire [15:0] array_io_weight_6; // @[SystolicArray.scala 40:37]
  wire [15:0] array_io_weight_7; // @[SystolicArray.scala 40:37]
  wire [15:0] array_io_output_0; // @[SystolicArray.scala 40:37]
  wire [15:0] array_io_output_1; // @[SystolicArray.scala 40:37]
  wire [15:0] array_io_output_2; // @[SystolicArray.scala 40:37]
  wire [15:0] array_io_output_3; // @[SystolicArray.scala 40:37]
  wire [15:0] array_io_output_4; // @[SystolicArray.scala 40:37]
  wire [15:0] array_io_output_5; // @[SystolicArray.scala 40:37]
  wire [15:0] array_io_output_6; // @[SystolicArray.scala 40:37]
  wire [15:0] array_io_output_7; // @[SystolicArray.scala 40:37]
  wire  input__clock; // @[Decoupled.scala 296:21]
  wire  input__reset; // @[Decoupled.scala 296:21]
  wire  input__io_enq_ready; // @[Decoupled.scala 296:21]
  wire  input__io_enq_valid; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_enq_bits_0; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_enq_bits_1; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_enq_bits_2; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_enq_bits_3; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_enq_bits_4; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_enq_bits_5; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_enq_bits_6; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_enq_bits_7; // @[Decoupled.scala 296:21]
  wire  input__io_deq_ready; // @[Decoupled.scala 296:21]
  wire  input__io_deq_valid; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_deq_bits_0; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_deq_bits_1; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_deq_bits_2; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_deq_bits_3; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_deq_bits_4; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_deq_bits_5; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_deq_bits_6; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_deq_bits_7; // @[Decoupled.scala 296:21]
  wire  output__clock; // @[SystolicArray.scala 45:22]
  wire  output__reset; // @[SystolicArray.scala 45:22]
  wire  output__io_enq_ready; // @[SystolicArray.scala 45:22]
  wire  output__io_enq_valid; // @[SystolicArray.scala 45:22]
  wire [15:0] output__io_enq_bits_0; // @[SystolicArray.scala 45:22]
  wire [15:0] output__io_enq_bits_1; // @[SystolicArray.scala 45:22]
  wire [15:0] output__io_enq_bits_2; // @[SystolicArray.scala 45:22]
  wire [15:0] output__io_enq_bits_3; // @[SystolicArray.scala 45:22]
  wire [15:0] output__io_enq_bits_4; // @[SystolicArray.scala 45:22]
  wire [15:0] output__io_enq_bits_5; // @[SystolicArray.scala 45:22]
  wire [15:0] output__io_enq_bits_6; // @[SystolicArray.scala 45:22]
  wire [15:0] output__io_enq_bits_7; // @[SystolicArray.scala 45:22]
  wire  output__io_deq_ready; // @[SystolicArray.scala 45:22]
  wire  output__io_deq_valid; // @[SystolicArray.scala 45:22]
  wire [15:0] output__io_deq_bits_0; // @[SystolicArray.scala 45:22]
  wire [15:0] output__io_deq_bits_1; // @[SystolicArray.scala 45:22]
  wire [15:0] output__io_deq_bits_2; // @[SystolicArray.scala 45:22]
  wire [15:0] output__io_deq_bits_3; // @[SystolicArray.scala 45:22]
  wire [15:0] output__io_deq_bits_4; // @[SystolicArray.scala 45:22]
  wire [15:0] output__io_deq_bits_5; // @[SystolicArray.scala 45:22]
  wire [15:0] output__io_deq_bits_6; // @[SystolicArray.scala 45:22]
  wire [15:0] output__io_deq_bits_7; // @[SystolicArray.scala 45:22]
  wire  _runInput_T = ~io_control_bits_load; // @[SystolicArray.scala 62:36]
  wire  _runInput_T_1 = io_control_valid & ~io_control_bits_load; // @[SystolicArray.scala 62:33]
  wire  _runInput_T_2 = ~io_control_bits_zeroes; // @[SystolicArray.scala 62:58]
  wire  runInput = io_control_valid & ~io_control_bits_load & ~io_control_bits_zeroes; // @[SystolicArray.scala 62:55]
  wire  runZeroes = _runInput_T_1 & io_control_bits_zeroes; // @[SystolicArray.scala 63:55]
  wire  _loadWeight_T = io_control_valid & io_control_bits_load; // @[SystolicArray.scala 65:19]
  wire  loadWeight = io_control_valid & io_control_bits_load & _runInput_T_2; // @[SystolicArray.scala 65:40]
  wire  loadZeroes = _loadWeight_T & io_control_bits_zeroes; // @[SystolicArray.scala 66:55]
  wire  running = (runInput & input__io_deq_valid & input__io_deq_ready | runZeroes) & output__io_deq_ready; // @[SystolicArray.scala 69:61]
  wire  loading = loadWeight & io_weight_valid & io_weight_ready | loadZeroes; // @[SystolicArray.scala 70:62]
  reg [3:0] arrayPropagationCountdown; // @[SystolicArray.scala 74:42]
  wire [3:0] _arrayPropagationCountdown_T_1 = arrayPropagationCountdown - 4'h1; // @[SystolicArray.scala 81:62]
  wire  inputDone = arrayPropagationCountdown == 4'h0; // @[SystolicArray.scala 87:45]
  wire  _io_control_ready_T_6 = io_control_bits_load & (io_control_bits_zeroes | io_weight_valid) & inputDone; // @[SystolicArray.scala 102:65]
  reg  output_io_enq_valid_sr_0; // @[ShiftRegister.scala 10:22]
  reg  output_io_enq_valid_sr_1; // @[ShiftRegister.scala 10:22]
  reg  output_io_enq_valid_sr_2; // @[ShiftRegister.scala 10:22]
  reg  output_io_enq_valid_sr_3; // @[ShiftRegister.scala 10:22]
  reg  output_io_enq_valid_sr_4; // @[ShiftRegister.scala 10:22]
  reg  output_io_enq_valid_sr_5; // @[ShiftRegister.scala 10:22]
  reg  output_io_enq_valid_sr_6; // @[ShiftRegister.scala 10:22]
  reg  output_io_enq_valid_sr_7; // @[ShiftRegister.scala 10:22]
  reg  output_io_enq_valid_sr_8; // @[ShiftRegister.scala 10:22]
  reg  output_io_enq_valid_sr_9; // @[ShiftRegister.scala 10:22]
  reg  output_io_enq_valid_sr_10; // @[ShiftRegister.scala 10:22]
  reg  output_io_enq_valid_sr_11; // @[ShiftRegister.scala 10:22]
  reg  output_io_enq_valid_sr_12; // @[ShiftRegister.scala 10:22]
  reg  output_io_enq_valid_sr_13; // @[ShiftRegister.scala 10:22]
  reg  output_io_enq_valid_sr_14; // @[ShiftRegister.scala 10:22]
  InnerSystolicArray array ( // @[SystolicArray.scala 40:37]
    .clock(array_clock),
    .reset(array_reset),
    .io_load(array_io_load),
    .io_input_0(array_io_input_0),
    .io_input_1(array_io_input_1),
    .io_input_2(array_io_input_2),
    .io_input_3(array_io_input_3),
    .io_input_4(array_io_input_4),
    .io_input_5(array_io_input_5),
    .io_input_6(array_io_input_6),
    .io_input_7(array_io_input_7),
    .io_weight_0(array_io_weight_0),
    .io_weight_1(array_io_weight_1),
    .io_weight_2(array_io_weight_2),
    .io_weight_3(array_io_weight_3),
    .io_weight_4(array_io_weight_4),
    .io_weight_5(array_io_weight_5),
    .io_weight_6(array_io_weight_6),
    .io_weight_7(array_io_weight_7),
    .io_output_0(array_io_output_0),
    .io_output_1(array_io_output_1),
    .io_output_2(array_io_output_2),
    .io_output_3(array_io_output_3),
    .io_output_4(array_io_output_4),
    .io_output_5(array_io_output_5),
    .io_output_6(array_io_output_6),
    .io_output_7(array_io_output_7)
  );
  Queue_4 input_ ( // @[Decoupled.scala 296:21]
    .clock(input__clock),
    .reset(input__reset),
    .io_enq_ready(input__io_enq_ready),
    .io_enq_valid(input__io_enq_valid),
    .io_enq_bits_0(input__io_enq_bits_0),
    .io_enq_bits_1(input__io_enq_bits_1),
    .io_enq_bits_2(input__io_enq_bits_2),
    .io_enq_bits_3(input__io_enq_bits_3),
    .io_enq_bits_4(input__io_enq_bits_4),
    .io_enq_bits_5(input__io_enq_bits_5),
    .io_enq_bits_6(input__io_enq_bits_6),
    .io_enq_bits_7(input__io_enq_bits_7),
    .io_deq_ready(input__io_deq_ready),
    .io_deq_valid(input__io_deq_valid),
    .io_deq_bits_0(input__io_deq_bits_0),
    .io_deq_bits_1(input__io_deq_bits_1),
    .io_deq_bits_2(input__io_deq_bits_2),
    .io_deq_bits_3(input__io_deq_bits_3),
    .io_deq_bits_4(input__io_deq_bits_4),
    .io_deq_bits_5(input__io_deq_bits_5),
    .io_deq_bits_6(input__io_deq_bits_6),
    .io_deq_bits_7(input__io_deq_bits_7)
  );
  Queue_5 output_ ( // @[SystolicArray.scala 45:22]
    .clock(output__clock),
    .reset(output__reset),
    .io_enq_ready(output__io_enq_ready),
    .io_enq_valid(output__io_enq_valid),
    .io_enq_bits_0(output__io_enq_bits_0),
    .io_enq_bits_1(output__io_enq_bits_1),
    .io_enq_bits_2(output__io_enq_bits_2),
    .io_enq_bits_3(output__io_enq_bits_3),
    .io_enq_bits_4(output__io_enq_bits_4),
    .io_enq_bits_5(output__io_enq_bits_5),
    .io_enq_bits_6(output__io_enq_bits_6),
    .io_enq_bits_7(output__io_enq_bits_7),
    .io_deq_ready(output__io_deq_ready),
    .io_deq_valid(output__io_deq_valid),
    .io_deq_bits_0(output__io_deq_bits_0),
    .io_deq_bits_1(output__io_deq_bits_1),
    .io_deq_bits_2(output__io_deq_bits_2),
    .io_deq_bits_3(output__io_deq_bits_3),
    .io_deq_bits_4(output__io_deq_bits_4),
    .io_deq_bits_5(output__io_deq_bits_5),
    .io_deq_bits_6(output__io_deq_bits_6),
    .io_deq_bits_7(output__io_deq_bits_7)
  );
  assign io_control_ready = _runInput_T & (io_control_bits_zeroes | input__io_deq_valid) & output__io_deq_ready |
    _io_control_ready_T_6; // @[SystolicArray.scala 101:104]
  assign io_input_ready = input__io_enq_ready; // @[Decoupled.scala 299:17]
  assign io_weight_ready = loadWeight & inputDone; // @[SystolicArray.scala 100:30]
  assign io_output_valid = output__io_deq_valid; // @[SystolicArray.scala 97:13]
  assign io_output_bits_0 = output__io_deq_bits_0; // @[SystolicArray.scala 97:13]
  assign io_output_bits_1 = output__io_deq_bits_1; // @[SystolicArray.scala 97:13]
  assign io_output_bits_2 = output__io_deq_bits_2; // @[SystolicArray.scala 97:13]
  assign io_output_bits_3 = output__io_deq_bits_3; // @[SystolicArray.scala 97:13]
  assign io_output_bits_4 = output__io_deq_bits_4; // @[SystolicArray.scala 97:13]
  assign io_output_bits_5 = output__io_deq_bits_5; // @[SystolicArray.scala 97:13]
  assign io_output_bits_6 = output__io_deq_bits_6; // @[SystolicArray.scala 97:13]
  assign io_output_bits_7 = output__io_deq_bits_7; // @[SystolicArray.scala 97:13]
  assign array_clock = clock;
  assign array_reset = reset;
  assign array_io_load = inputDone & loading; // @[SystolicArray.scala 89:30]
  assign array_io_input_0 = io_control_bits_zeroes ? $signed(16'sh0) : $signed(input__io_deq_bits_0); // @[SystolicArray.scala 90:24]
  assign array_io_input_1 = io_control_bits_zeroes ? $signed(16'sh0) : $signed(input__io_deq_bits_1); // @[SystolicArray.scala 90:24]
  assign array_io_input_2 = io_control_bits_zeroes ? $signed(16'sh0) : $signed(input__io_deq_bits_2); // @[SystolicArray.scala 90:24]
  assign array_io_input_3 = io_control_bits_zeroes ? $signed(16'sh0) : $signed(input__io_deq_bits_3); // @[SystolicArray.scala 90:24]
  assign array_io_input_4 = io_control_bits_zeroes ? $signed(16'sh0) : $signed(input__io_deq_bits_4); // @[SystolicArray.scala 90:24]
  assign array_io_input_5 = io_control_bits_zeroes ? $signed(16'sh0) : $signed(input__io_deq_bits_5); // @[SystolicArray.scala 90:24]
  assign array_io_input_6 = io_control_bits_zeroes ? $signed(16'sh0) : $signed(input__io_deq_bits_6); // @[SystolicArray.scala 90:24]
  assign array_io_input_7 = io_control_bits_zeroes ? $signed(16'sh0) : $signed(input__io_deq_bits_7); // @[SystolicArray.scala 90:24]
  assign array_io_weight_0 = io_control_bits_zeroes ? $signed(16'sh0) : $signed(io_weight_bits_0); // @[SystolicArray.scala 91:25]
  assign array_io_weight_1 = io_control_bits_zeroes ? $signed(16'sh0) : $signed(io_weight_bits_1); // @[SystolicArray.scala 91:25]
  assign array_io_weight_2 = io_control_bits_zeroes ? $signed(16'sh0) : $signed(io_weight_bits_2); // @[SystolicArray.scala 91:25]
  assign array_io_weight_3 = io_control_bits_zeroes ? $signed(16'sh0) : $signed(io_weight_bits_3); // @[SystolicArray.scala 91:25]
  assign array_io_weight_4 = io_control_bits_zeroes ? $signed(16'sh0) : $signed(io_weight_bits_4); // @[SystolicArray.scala 91:25]
  assign array_io_weight_5 = io_control_bits_zeroes ? $signed(16'sh0) : $signed(io_weight_bits_5); // @[SystolicArray.scala 91:25]
  assign array_io_weight_6 = io_control_bits_zeroes ? $signed(16'sh0) : $signed(io_weight_bits_6); // @[SystolicArray.scala 91:25]
  assign array_io_weight_7 = io_control_bits_zeroes ? $signed(16'sh0) : $signed(io_weight_bits_7); // @[SystolicArray.scala 91:25]
  assign input__clock = clock;
  assign input__reset = reset;
  assign input__io_enq_valid = io_input_valid; // @[Decoupled.scala 297:22]
  assign input__io_enq_bits_0 = io_input_bits_0; // @[Decoupled.scala 298:21]
  assign input__io_enq_bits_1 = io_input_bits_1; // @[Decoupled.scala 298:21]
  assign input__io_enq_bits_2 = io_input_bits_2; // @[Decoupled.scala 298:21]
  assign input__io_enq_bits_3 = io_input_bits_3; // @[Decoupled.scala 298:21]
  assign input__io_enq_bits_4 = io_input_bits_4; // @[Decoupled.scala 298:21]
  assign input__io_enq_bits_5 = io_input_bits_5; // @[Decoupled.scala 298:21]
  assign input__io_enq_bits_6 = io_input_bits_6; // @[Decoupled.scala 298:21]
  assign input__io_enq_bits_7 = io_input_bits_7; // @[Decoupled.scala 298:21]
  assign input__io_deq_ready = runInput & output__io_deq_ready; // @[SystolicArray.scala 99:27]
  assign output__clock = clock;
  assign output__reset = reset;
  assign output__io_enq_valid = output_io_enq_valid_sr_14; // @[SystolicArray.scala 105:23]
  assign output__io_enq_bits_0 = array_io_output_0; // @[SystolicArray.scala 96:22]
  assign output__io_enq_bits_1 = array_io_output_1; // @[SystolicArray.scala 96:22]
  assign output__io_enq_bits_2 = array_io_output_2; // @[SystolicArray.scala 96:22]
  assign output__io_enq_bits_3 = array_io_output_3; // @[SystolicArray.scala 96:22]
  assign output__io_enq_bits_4 = array_io_output_4; // @[SystolicArray.scala 96:22]
  assign output__io_enq_bits_5 = array_io_output_5; // @[SystolicArray.scala 96:22]
  assign output__io_enq_bits_6 = array_io_output_6; // @[SystolicArray.scala 96:22]
  assign output__io_enq_bits_7 = array_io_output_7; // @[SystolicArray.scala 96:22]
  assign output__io_deq_ready = io_output_ready; // @[SystolicArray.scala 97:13]
  always @(posedge clock) begin
    if (reset) begin // @[SystolicArray.scala 74:42]
      arrayPropagationCountdown <= 4'h0; // @[SystolicArray.scala 74:42]
    end else if (running) begin // @[SystolicArray.scala 77:17]
      arrayPropagationCountdown <= 4'hf; // @[SystolicArray.scala 78:31]
    end else if (arrayPropagationCountdown > 4'h0) begin // @[SystolicArray.scala 80:43]
      arrayPropagationCountdown <= _arrayPropagationCountdown_T_1; // @[SystolicArray.scala 81:33]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      output_io_enq_valid_sr_0 <= 1'h0; // @[ShiftRegister.scala 10:22]
    end else begin
      output_io_enq_valid_sr_0 <= running; // @[ShiftRegister.scala 25:12]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      output_io_enq_valid_sr_1 <= 1'h0; // @[ShiftRegister.scala 10:22]
    end else begin
      output_io_enq_valid_sr_1 <= output_io_enq_valid_sr_0; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      output_io_enq_valid_sr_2 <= 1'h0; // @[ShiftRegister.scala 10:22]
    end else begin
      output_io_enq_valid_sr_2 <= output_io_enq_valid_sr_1; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      output_io_enq_valid_sr_3 <= 1'h0; // @[ShiftRegister.scala 10:22]
    end else begin
      output_io_enq_valid_sr_3 <= output_io_enq_valid_sr_2; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      output_io_enq_valid_sr_4 <= 1'h0; // @[ShiftRegister.scala 10:22]
    end else begin
      output_io_enq_valid_sr_4 <= output_io_enq_valid_sr_3; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      output_io_enq_valid_sr_5 <= 1'h0; // @[ShiftRegister.scala 10:22]
    end else begin
      output_io_enq_valid_sr_5 <= output_io_enq_valid_sr_4; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      output_io_enq_valid_sr_6 <= 1'h0; // @[ShiftRegister.scala 10:22]
    end else begin
      output_io_enq_valid_sr_6 <= output_io_enq_valid_sr_5; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      output_io_enq_valid_sr_7 <= 1'h0; // @[ShiftRegister.scala 10:22]
    end else begin
      output_io_enq_valid_sr_7 <= output_io_enq_valid_sr_6; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      output_io_enq_valid_sr_8 <= 1'h0; // @[ShiftRegister.scala 10:22]
    end else begin
      output_io_enq_valid_sr_8 <= output_io_enq_valid_sr_7; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      output_io_enq_valid_sr_9 <= 1'h0; // @[ShiftRegister.scala 10:22]
    end else begin
      output_io_enq_valid_sr_9 <= output_io_enq_valid_sr_8; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      output_io_enq_valid_sr_10 <= 1'h0; // @[ShiftRegister.scala 10:22]
    end else begin
      output_io_enq_valid_sr_10 <= output_io_enq_valid_sr_9; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      output_io_enq_valid_sr_11 <= 1'h0; // @[ShiftRegister.scala 10:22]
    end else begin
      output_io_enq_valid_sr_11 <= output_io_enq_valid_sr_10; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      output_io_enq_valid_sr_12 <= 1'h0; // @[ShiftRegister.scala 10:22]
    end else begin
      output_io_enq_valid_sr_12 <= output_io_enq_valid_sr_11; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      output_io_enq_valid_sr_13 <= 1'h0; // @[ShiftRegister.scala 10:22]
    end else begin
      output_io_enq_valid_sr_13 <= output_io_enq_valid_sr_12; // @[ShiftRegister.scala 13:11]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      output_io_enq_valid_sr_14 <= 1'h0; // @[ShiftRegister.scala 10:22]
    end else begin
      output_io_enq_valid_sr_14 <= output_io_enq_valid_sr_13; // @[ShiftRegister.scala 13:11]
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
  arrayPropagationCountdown = _RAND_0[3:0];
  _RAND_1 = {1{`RANDOM}};
  output_io_enq_valid_sr_0 = _RAND_1[0:0];
  _RAND_2 = {1{`RANDOM}};
  output_io_enq_valid_sr_1 = _RAND_2[0:0];
  _RAND_3 = {1{`RANDOM}};
  output_io_enq_valid_sr_2 = _RAND_3[0:0];
  _RAND_4 = {1{`RANDOM}};
  output_io_enq_valid_sr_3 = _RAND_4[0:0];
  _RAND_5 = {1{`RANDOM}};
  output_io_enq_valid_sr_4 = _RAND_5[0:0];
  _RAND_6 = {1{`RANDOM}};
  output_io_enq_valid_sr_5 = _RAND_6[0:0];
  _RAND_7 = {1{`RANDOM}};
  output_io_enq_valid_sr_6 = _RAND_7[0:0];
  _RAND_8 = {1{`RANDOM}};
  output_io_enq_valid_sr_7 = _RAND_8[0:0];
  _RAND_9 = {1{`RANDOM}};
  output_io_enq_valid_sr_8 = _RAND_9[0:0];
  _RAND_10 = {1{`RANDOM}};
  output_io_enq_valid_sr_9 = _RAND_10[0:0];
  _RAND_11 = {1{`RANDOM}};
  output_io_enq_valid_sr_10 = _RAND_11[0:0];
  _RAND_12 = {1{`RANDOM}};
  output_io_enq_valid_sr_11 = _RAND_12[0:0];
  _RAND_13 = {1{`RANDOM}};
  output_io_enq_valid_sr_12 = _RAND_13[0:0];
  _RAND_14 = {1{`RANDOM}};
  output_io_enq_valid_sr_13 = _RAND_14[0:0];
  _RAND_15 = {1{`RANDOM}};
  output_io_enq_valid_sr_14 = _RAND_15[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module InnerDualPortMem(
  input         clock,
  input         reset,
  input  [5:0]  io_portA_address,
  input         io_portA_read_enable,
  output [15:0] io_portA_read_data_0,
  output [15:0] io_portA_read_data_1,
  output [15:0] io_portA_read_data_2,
  output [15:0] io_portA_read_data_3,
  output [15:0] io_portA_read_data_4,
  output [15:0] io_portA_read_data_5,
  output [15:0] io_portA_read_data_6,
  output [15:0] io_portA_read_data_7,
  input         io_portA_write_enable,
  input  [15:0] io_portA_write_data_0,
  input  [15:0] io_portA_write_data_1,
  input  [15:0] io_portA_write_data_2,
  input  [15:0] io_portA_write_data_3,
  input  [15:0] io_portA_write_data_4,
  input  [15:0] io_portA_write_data_5,
  input  [15:0] io_portA_write_data_6,
  input  [15:0] io_portA_write_data_7,
  input  [5:0]  io_portB_address,
  input         io_portB_read_enable,
  output [15:0] io_portB_read_data_0,
  output [15:0] io_portB_read_data_1,
  output [15:0] io_portB_read_data_2,
  output [15:0] io_portB_read_data_3,
  output [15:0] io_portB_read_data_4,
  output [15:0] io_portB_read_data_5,
  output [15:0] io_portB_read_data_6,
  output [15:0] io_portB_read_data_7,
  input         io_portB_write_enable,
  input  [15:0] io_portB_write_data_0,
  input  [15:0] io_portB_write_data_1,
  input  [15:0] io_portB_write_data_2,
  input  [15:0] io_portB_write_data_3,
  input  [15:0] io_portB_write_data_4,
  input  [15:0] io_portB_write_data_5,
  input  [15:0] io_portB_write_data_6,
  input  [15:0] io_portB_write_data_7
);
  wire  mem_clka; // @[DualPortMem.scala 164:25]
  wire  mem_wea; // @[DualPortMem.scala 164:25]
  wire  mem_ena; // @[DualPortMem.scala 164:25]
  wire [5:0] mem_addra; // @[DualPortMem.scala 164:25]
  wire [127:0] mem_dia; // @[DualPortMem.scala 164:25]
  wire [127:0] mem_doa; // @[DualPortMem.scala 164:25]
  wire  mem_clkb; // @[DualPortMem.scala 164:25]
  wire  mem_web; // @[DualPortMem.scala 164:25]
  wire  mem_enb; // @[DualPortMem.scala 164:25]
  wire [5:0] mem_addrb; // @[DualPortMem.scala 164:25]
  wire [127:0] mem_dib; // @[DualPortMem.scala 164:25]
  wire [127:0] mem_dob; // @[DualPortMem.scala 164:25]
  wire [127:0] _io_portA_read_data_WIRE_1 = mem_doa;
  wire [63:0] mem_io_dia_lo = {io_portA_write_data_3,io_portA_write_data_2,io_portA_write_data_1,io_portA_write_data_0}; // @[DualPortMem.scala 171:51]
  wire [63:0] mem_io_dia_hi = {io_portA_write_data_7,io_portA_write_data_6,io_portA_write_data_5,io_portA_write_data_4}; // @[DualPortMem.scala 171:51]
  wire [127:0] _io_portB_read_data_WIRE_1 = mem_dob;
  wire [63:0] mem_io_dib_lo = {io_portB_write_data_3,io_portB_write_data_2,io_portB_write_data_1,io_portB_write_data_0}; // @[DualPortMem.scala 178:51]
  wire [63:0] mem_io_dib_hi = {io_portB_write_data_7,io_portB_write_data_6,io_portB_write_data_5,io_portB_write_data_4}; // @[DualPortMem.scala 178:51]
  bram_dp_128x64 mem ( // @[DualPortMem.scala 164:25]
    .clka(mem_clka),
    .wea(mem_wea),
    .ena(mem_ena),
    .addra(mem_addra),
    .dia(mem_dia),
    .doa(mem_doa),
    .clkb(mem_clkb),
    .web(mem_web),
    .enb(mem_enb),
    .addrb(mem_addrb),
    .dib(mem_dib),
    .dob(mem_dob)
  );
  assign io_portA_read_data_0 = _io_portA_read_data_WIRE_1[15:0]; // @[DualPortMem.scala 169:50]
  assign io_portA_read_data_1 = _io_portA_read_data_WIRE_1[31:16]; // @[DualPortMem.scala 169:50]
  assign io_portA_read_data_2 = _io_portA_read_data_WIRE_1[47:32]; // @[DualPortMem.scala 169:50]
  assign io_portA_read_data_3 = _io_portA_read_data_WIRE_1[63:48]; // @[DualPortMem.scala 169:50]
  assign io_portA_read_data_4 = _io_portA_read_data_WIRE_1[79:64]; // @[DualPortMem.scala 169:50]
  assign io_portA_read_data_5 = _io_portA_read_data_WIRE_1[95:80]; // @[DualPortMem.scala 169:50]
  assign io_portA_read_data_6 = _io_portA_read_data_WIRE_1[111:96]; // @[DualPortMem.scala 169:50]
  assign io_portA_read_data_7 = _io_portA_read_data_WIRE_1[127:112]; // @[DualPortMem.scala 169:50]
  assign io_portB_read_data_0 = _io_portB_read_data_WIRE_1[15:0]; // @[DualPortMem.scala 176:50]
  assign io_portB_read_data_1 = _io_portB_read_data_WIRE_1[31:16]; // @[DualPortMem.scala 176:50]
  assign io_portB_read_data_2 = _io_portB_read_data_WIRE_1[47:32]; // @[DualPortMem.scala 176:50]
  assign io_portB_read_data_3 = _io_portB_read_data_WIRE_1[63:48]; // @[DualPortMem.scala 176:50]
  assign io_portB_read_data_4 = _io_portB_read_data_WIRE_1[79:64]; // @[DualPortMem.scala 176:50]
  assign io_portB_read_data_5 = _io_portB_read_data_WIRE_1[95:80]; // @[DualPortMem.scala 176:50]
  assign io_portB_read_data_6 = _io_portB_read_data_WIRE_1[111:96]; // @[DualPortMem.scala 176:50]
  assign io_portB_read_data_7 = _io_portB_read_data_WIRE_1[127:112]; // @[DualPortMem.scala 176:50]
  assign mem_clka = clock; // @[DualPortMem.scala 166:30]
  assign mem_wea = io_portA_write_enable; // @[DualPortMem.scala 170:20]
  assign mem_ena = ~reset; // @[DualPortMem.scala 167:23]
  assign mem_addra = io_portA_address; // @[DualPortMem.scala 168:22]
  assign mem_dia = {mem_io_dia_hi,mem_io_dia_lo}; // @[DualPortMem.scala 171:51]
  assign mem_clkb = clock; // @[DualPortMem.scala 173:30]
  assign mem_web = io_portB_write_enable; // @[DualPortMem.scala 177:20]
  assign mem_enb = ~reset; // @[DualPortMem.scala 174:23]
  assign mem_addrb = io_portB_address; // @[DualPortMem.scala 175:22]
  assign mem_dib = {mem_io_dib_hi,mem_io_dib_lo}; // @[DualPortMem.scala 178:51]
endmodule
module Queue_7(
  input         clock,
  input         reset,
  output        io_enq_ready,
  input         io_enq_valid,
  input  [15:0] io_enq_bits_0,
  input  [15:0] io_enq_bits_1,
  input  [15:0] io_enq_bits_2,
  input  [15:0] io_enq_bits_3,
  input  [15:0] io_enq_bits_4,
  input  [15:0] io_enq_bits_5,
  input  [15:0] io_enq_bits_6,
  input  [15:0] io_enq_bits_7,
  input         io_deq_ready,
  output        io_deq_valid,
  output [15:0] io_deq_bits_0,
  output [15:0] io_deq_bits_1,
  output [15:0] io_deq_bits_2,
  output [15:0] io_deq_bits_3,
  output [15:0] io_deq_bits_4,
  output [15:0] io_deq_bits_5,
  output [15:0] io_deq_bits_6,
  output [15:0] io_deq_bits_7,
  output [1:0]  io_count
);
`ifdef RANDOMIZE_GARBAGE_ASSIGN
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_7;
  reg [31:0] _RAND_9;
  reg [31:0] _RAND_11;
  reg [31:0] _RAND_13;
  reg [31:0] _RAND_15;
`endif // RANDOMIZE_GARBAGE_ASSIGN
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_6;
  reg [31:0] _RAND_8;
  reg [31:0] _RAND_10;
  reg [31:0] _RAND_12;
  reg [31:0] _RAND_14;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_16;
  reg [31:0] _RAND_17;
  reg [31:0] _RAND_18;
`endif // RANDOMIZE_REG_INIT
  reg [15:0] ram_0 [0:2]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_0_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire [1:0] ram_0_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_0_MPORT_data; // @[Decoupled.scala 218:16]
  wire [1:0] ram_0_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_0_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_0_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_1 [0:2]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_1_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire [1:0] ram_1_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_1_MPORT_data; // @[Decoupled.scala 218:16]
  wire [1:0] ram_1_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_1_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_1_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_2 [0:2]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_2_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire [1:0] ram_2_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_2_MPORT_data; // @[Decoupled.scala 218:16]
  wire [1:0] ram_2_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_2_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_2_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_3 [0:2]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_3_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire [1:0] ram_3_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_3_MPORT_data; // @[Decoupled.scala 218:16]
  wire [1:0] ram_3_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_3_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_3_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_4 [0:2]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_4_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire [1:0] ram_4_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_4_MPORT_data; // @[Decoupled.scala 218:16]
  wire [1:0] ram_4_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_4_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_4_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_5 [0:2]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_5_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire [1:0] ram_5_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_5_MPORT_data; // @[Decoupled.scala 218:16]
  wire [1:0] ram_5_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_5_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_5_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_6 [0:2]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_6_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire [1:0] ram_6_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_6_MPORT_data; // @[Decoupled.scala 218:16]
  wire [1:0] ram_6_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_6_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_6_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_7 [0:2]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_7_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire [1:0] ram_7_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_7_MPORT_data; // @[Decoupled.scala 218:16]
  wire [1:0] ram_7_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_7_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_7_MPORT_en; // @[Decoupled.scala 218:16]
  reg [1:0] enq_ptr_value; // @[Counter.scala 60:40]
  reg [1:0] deq_ptr_value; // @[Counter.scala 60:40]
  reg  maybe_full; // @[Decoupled.scala 221:27]
  wire  ptr_match = enq_ptr_value == deq_ptr_value; // @[Decoupled.scala 223:33]
  wire  empty = ptr_match & ~maybe_full; // @[Decoupled.scala 224:25]
  wire  full = ptr_match & maybe_full; // @[Decoupled.scala 225:24]
  wire  _do_enq_T = io_enq_ready & io_enq_valid; // @[Decoupled.scala 40:37]
  wire  _do_deq_T = io_deq_ready & io_deq_valid; // @[Decoupled.scala 40:37]
  wire  wrap = enq_ptr_value == 2'h2; // @[Counter.scala 72:24]
  wire [1:0] _value_T_1 = enq_ptr_value + 2'h1; // @[Counter.scala 76:24]
  wire  _GEN_18 = io_deq_ready ? 1'h0 : _do_enq_T; // @[Decoupled.scala 249:27 Decoupled.scala 249:36]
  wire  do_enq = empty ? _GEN_18 : _do_enq_T; // @[Decoupled.scala 246:18]
  wire  wrap_1 = deq_ptr_value == 2'h2; // @[Counter.scala 72:24]
  wire [1:0] _value_T_3 = deq_ptr_value + 2'h1; // @[Counter.scala 76:24]
  wire  do_deq = empty ? 1'h0 : _do_deq_T; // @[Decoupled.scala 246:18 Decoupled.scala 248:14]
  wire [1:0] ptr_diff = enq_ptr_value - deq_ptr_value; // @[Decoupled.scala 257:32]
  wire [1:0] _io_count_T = maybe_full ? 2'h3 : 2'h0; // @[Decoupled.scala 262:24]
  wire [1:0] _io_count_T_3 = 2'h3 + ptr_diff; // @[Decoupled.scala 265:38]
  wire [1:0] _io_count_T_4 = deq_ptr_value > enq_ptr_value ? _io_count_T_3 : ptr_diff; // @[Decoupled.scala 264:24]
  assign ram_0_io_deq_bits_MPORT_addr = deq_ptr_value;
  `ifndef RANDOMIZE_GARBAGE_ASSIGN
  assign ram_0_io_deq_bits_MPORT_data = ram_0[ram_0_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `else
  assign ram_0_io_deq_bits_MPORT_data = ram_0_io_deq_bits_MPORT_addr >= 2'h3 ? _RAND_1[15:0] :
    ram_0[ram_0_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `endif // RANDOMIZE_GARBAGE_ASSIGN
  assign ram_0_MPORT_data = io_enq_bits_0;
  assign ram_0_MPORT_addr = enq_ptr_value;
  assign ram_0_MPORT_mask = 1'h1;
  assign ram_0_MPORT_en = empty ? _GEN_18 : _do_enq_T;
  assign ram_1_io_deq_bits_MPORT_addr = deq_ptr_value;
  `ifndef RANDOMIZE_GARBAGE_ASSIGN
  assign ram_1_io_deq_bits_MPORT_data = ram_1[ram_1_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `else
  assign ram_1_io_deq_bits_MPORT_data = ram_1_io_deq_bits_MPORT_addr >= 2'h3 ? _RAND_3[15:0] :
    ram_1[ram_1_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `endif // RANDOMIZE_GARBAGE_ASSIGN
  assign ram_1_MPORT_data = io_enq_bits_1;
  assign ram_1_MPORT_addr = enq_ptr_value;
  assign ram_1_MPORT_mask = 1'h1;
  assign ram_1_MPORT_en = empty ? _GEN_18 : _do_enq_T;
  assign ram_2_io_deq_bits_MPORT_addr = deq_ptr_value;
  `ifndef RANDOMIZE_GARBAGE_ASSIGN
  assign ram_2_io_deq_bits_MPORT_data = ram_2[ram_2_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `else
  assign ram_2_io_deq_bits_MPORT_data = ram_2_io_deq_bits_MPORT_addr >= 2'h3 ? _RAND_5[15:0] :
    ram_2[ram_2_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `endif // RANDOMIZE_GARBAGE_ASSIGN
  assign ram_2_MPORT_data = io_enq_bits_2;
  assign ram_2_MPORT_addr = enq_ptr_value;
  assign ram_2_MPORT_mask = 1'h1;
  assign ram_2_MPORT_en = empty ? _GEN_18 : _do_enq_T;
  assign ram_3_io_deq_bits_MPORT_addr = deq_ptr_value;
  `ifndef RANDOMIZE_GARBAGE_ASSIGN
  assign ram_3_io_deq_bits_MPORT_data = ram_3[ram_3_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `else
  assign ram_3_io_deq_bits_MPORT_data = ram_3_io_deq_bits_MPORT_addr >= 2'h3 ? _RAND_7[15:0] :
    ram_3[ram_3_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `endif // RANDOMIZE_GARBAGE_ASSIGN
  assign ram_3_MPORT_data = io_enq_bits_3;
  assign ram_3_MPORT_addr = enq_ptr_value;
  assign ram_3_MPORT_mask = 1'h1;
  assign ram_3_MPORT_en = empty ? _GEN_18 : _do_enq_T;
  assign ram_4_io_deq_bits_MPORT_addr = deq_ptr_value;
  `ifndef RANDOMIZE_GARBAGE_ASSIGN
  assign ram_4_io_deq_bits_MPORT_data = ram_4[ram_4_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `else
  assign ram_4_io_deq_bits_MPORT_data = ram_4_io_deq_bits_MPORT_addr >= 2'h3 ? _RAND_9[15:0] :
    ram_4[ram_4_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `endif // RANDOMIZE_GARBAGE_ASSIGN
  assign ram_4_MPORT_data = io_enq_bits_4;
  assign ram_4_MPORT_addr = enq_ptr_value;
  assign ram_4_MPORT_mask = 1'h1;
  assign ram_4_MPORT_en = empty ? _GEN_18 : _do_enq_T;
  assign ram_5_io_deq_bits_MPORT_addr = deq_ptr_value;
  `ifndef RANDOMIZE_GARBAGE_ASSIGN
  assign ram_5_io_deq_bits_MPORT_data = ram_5[ram_5_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `else
  assign ram_5_io_deq_bits_MPORT_data = ram_5_io_deq_bits_MPORT_addr >= 2'h3 ? _RAND_11[15:0] :
    ram_5[ram_5_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `endif // RANDOMIZE_GARBAGE_ASSIGN
  assign ram_5_MPORT_data = io_enq_bits_5;
  assign ram_5_MPORT_addr = enq_ptr_value;
  assign ram_5_MPORT_mask = 1'h1;
  assign ram_5_MPORT_en = empty ? _GEN_18 : _do_enq_T;
  assign ram_6_io_deq_bits_MPORT_addr = deq_ptr_value;
  `ifndef RANDOMIZE_GARBAGE_ASSIGN
  assign ram_6_io_deq_bits_MPORT_data = ram_6[ram_6_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `else
  assign ram_6_io_deq_bits_MPORT_data = ram_6_io_deq_bits_MPORT_addr >= 2'h3 ? _RAND_13[15:0] :
    ram_6[ram_6_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `endif // RANDOMIZE_GARBAGE_ASSIGN
  assign ram_6_MPORT_data = io_enq_bits_6;
  assign ram_6_MPORT_addr = enq_ptr_value;
  assign ram_6_MPORT_mask = 1'h1;
  assign ram_6_MPORT_en = empty ? _GEN_18 : _do_enq_T;
  assign ram_7_io_deq_bits_MPORT_addr = deq_ptr_value;
  `ifndef RANDOMIZE_GARBAGE_ASSIGN
  assign ram_7_io_deq_bits_MPORT_data = ram_7[ram_7_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `else
  assign ram_7_io_deq_bits_MPORT_data = ram_7_io_deq_bits_MPORT_addr >= 2'h3 ? _RAND_15[15:0] :
    ram_7[ram_7_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  `endif // RANDOMIZE_GARBAGE_ASSIGN
  assign ram_7_MPORT_data = io_enq_bits_7;
  assign ram_7_MPORT_addr = enq_ptr_value;
  assign ram_7_MPORT_mask = 1'h1;
  assign ram_7_MPORT_en = empty ? _GEN_18 : _do_enq_T;
  assign io_enq_ready = ~full; // @[Decoupled.scala 241:19]
  assign io_deq_valid = io_enq_valid | ~empty; // @[Decoupled.scala 245:25 Decoupled.scala 245:40 Decoupled.scala 240:16]
  assign io_deq_bits_0 = empty ? $signed(io_enq_bits_0) : $signed(ram_0_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_1 = empty ? $signed(io_enq_bits_1) : $signed(ram_1_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_2 = empty ? $signed(io_enq_bits_2) : $signed(ram_2_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_3 = empty ? $signed(io_enq_bits_3) : $signed(ram_3_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_4 = empty ? $signed(io_enq_bits_4) : $signed(ram_4_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_5 = empty ? $signed(io_enq_bits_5) : $signed(ram_5_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_6 = empty ? $signed(io_enq_bits_6) : $signed(ram_6_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_7 = empty ? $signed(io_enq_bits_7) : $signed(ram_7_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_count = ptr_match ? _io_count_T : _io_count_T_4; // @[Decoupled.scala 261:20]
  always @(posedge clock) begin
    if(ram_0_MPORT_en & ram_0_MPORT_mask) begin
      ram_0[ram_0_MPORT_addr] <= ram_0_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_1_MPORT_en & ram_1_MPORT_mask) begin
      ram_1[ram_1_MPORT_addr] <= ram_1_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_2_MPORT_en & ram_2_MPORT_mask) begin
      ram_2[ram_2_MPORT_addr] <= ram_2_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_3_MPORT_en & ram_3_MPORT_mask) begin
      ram_3[ram_3_MPORT_addr] <= ram_3_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_4_MPORT_en & ram_4_MPORT_mask) begin
      ram_4[ram_4_MPORT_addr] <= ram_4_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_5_MPORT_en & ram_5_MPORT_mask) begin
      ram_5[ram_5_MPORT_addr] <= ram_5_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_6_MPORT_en & ram_6_MPORT_mask) begin
      ram_6[ram_6_MPORT_addr] <= ram_6_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_7_MPORT_en & ram_7_MPORT_mask) begin
      ram_7[ram_7_MPORT_addr] <= ram_7_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if (reset) begin // @[Counter.scala 60:40]
      enq_ptr_value <= 2'h0; // @[Counter.scala 60:40]
    end else if (do_enq) begin // @[Decoupled.scala 229:17]
      if (wrap) begin // @[Counter.scala 86:20]
        enq_ptr_value <= 2'h0; // @[Counter.scala 86:28]
      end else begin
        enq_ptr_value <= _value_T_1; // @[Counter.scala 76:15]
      end
    end
    if (reset) begin // @[Counter.scala 60:40]
      deq_ptr_value <= 2'h0; // @[Counter.scala 60:40]
    end else if (do_deq) begin // @[Decoupled.scala 233:17]
      if (wrap_1) begin // @[Counter.scala 86:20]
        deq_ptr_value <= 2'h0; // @[Counter.scala 86:28]
      end else begin
        deq_ptr_value <= _value_T_3; // @[Counter.scala 76:15]
      end
    end
    if (reset) begin // @[Decoupled.scala 221:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 221:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 236:28]
      if (empty) begin // @[Decoupled.scala 246:18]
        if (io_deq_ready) begin // @[Decoupled.scala 249:27]
          maybe_full <= 1'h0; // @[Decoupled.scala 249:36]
        end else begin
          maybe_full <= _do_enq_T;
        end
      end else begin
        maybe_full <= _do_enq_T;
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
`ifdef RANDOMIZE_GARBAGE_ASSIGN
  _RAND_1 = {1{`RANDOM}};
  _RAND_3 = {1{`RANDOM}};
  _RAND_5 = {1{`RANDOM}};
  _RAND_7 = {1{`RANDOM}};
  _RAND_9 = {1{`RANDOM}};
  _RAND_11 = {1{`RANDOM}};
  _RAND_13 = {1{`RANDOM}};
  _RAND_15 = {1{`RANDOM}};
`endif // RANDOMIZE_GARBAGE_ASSIGN
`ifdef RANDOMIZE_MEM_INIT
  _RAND_0 = {1{`RANDOM}};
  for (initvar = 0; initvar < 3; initvar = initvar+1)
    ram_0[initvar] = _RAND_0[15:0];
  _RAND_2 = {1{`RANDOM}};
  for (initvar = 0; initvar < 3; initvar = initvar+1)
    ram_1[initvar] = _RAND_2[15:0];
  _RAND_4 = {1{`RANDOM}};
  for (initvar = 0; initvar < 3; initvar = initvar+1)
    ram_2[initvar] = _RAND_4[15:0];
  _RAND_6 = {1{`RANDOM}};
  for (initvar = 0; initvar < 3; initvar = initvar+1)
    ram_3[initvar] = _RAND_6[15:0];
  _RAND_8 = {1{`RANDOM}};
  for (initvar = 0; initvar < 3; initvar = initvar+1)
    ram_4[initvar] = _RAND_8[15:0];
  _RAND_10 = {1{`RANDOM}};
  for (initvar = 0; initvar < 3; initvar = initvar+1)
    ram_5[initvar] = _RAND_10[15:0];
  _RAND_12 = {1{`RANDOM}};
  for (initvar = 0; initvar < 3; initvar = initvar+1)
    ram_6[initvar] = _RAND_12[15:0];
  _RAND_14 = {1{`RANDOM}};
  for (initvar = 0; initvar < 3; initvar = initvar+1)
    ram_7[initvar] = _RAND_14[15:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_16 = {1{`RANDOM}};
  enq_ptr_value = _RAND_16[1:0];
  _RAND_17 = {1{`RANDOM}};
  deq_ptr_value = _RAND_17[1:0];
  _RAND_18 = {1{`RANDOM}};
  maybe_full = _RAND_18[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module DualPortMem(
  input         clock,
  input         reset,
  output        io_portA_control_ready,
  input         io_portA_control_valid,
  input         io_portA_control_bits_write,
  input  [5:0]  io_portA_control_bits_address,
  output        io_portA_input_ready,
  input         io_portA_input_valid,
  input  [15:0] io_portA_input_bits_0,
  input  [15:0] io_portA_input_bits_1,
  input  [15:0] io_portA_input_bits_2,
  input  [15:0] io_portA_input_bits_3,
  input  [15:0] io_portA_input_bits_4,
  input  [15:0] io_portA_input_bits_5,
  input  [15:0] io_portA_input_bits_6,
  input  [15:0] io_portA_input_bits_7,
  input         io_portA_output_ready,
  output        io_portA_output_valid,
  output [15:0] io_portA_output_bits_0,
  output [15:0] io_portA_output_bits_1,
  output [15:0] io_portA_output_bits_2,
  output [15:0] io_portA_output_bits_3,
  output [15:0] io_portA_output_bits_4,
  output [15:0] io_portA_output_bits_5,
  output [15:0] io_portA_output_bits_6,
  output [15:0] io_portA_output_bits_7,
  output        io_portB_control_ready,
  input         io_portB_control_valid,
  input         io_portB_control_bits_write,
  input  [5:0]  io_portB_control_bits_address,
  output        io_portB_input_ready,
  input         io_portB_input_valid,
  input  [15:0] io_portB_input_bits_0,
  input  [15:0] io_portB_input_bits_1,
  input  [15:0] io_portB_input_bits_2,
  input  [15:0] io_portB_input_bits_3,
  input  [15:0] io_portB_input_bits_4,
  input  [15:0] io_portB_input_bits_5,
  input  [15:0] io_portB_input_bits_6,
  input  [15:0] io_portB_input_bits_7,
  input         io_portB_output_ready,
  output        io_portB_output_valid,
  output [15:0] io_portB_output_bits_0,
  output [15:0] io_portB_output_bits_1,
  output [15:0] io_portB_output_bits_2,
  output [15:0] io_portB_output_bits_3,
  output [15:0] io_portB_output_bits_4,
  output [15:0] io_portB_output_bits_5,
  output [15:0] io_portB_output_bits_6,
  output [15:0] io_portB_output_bits_7,
  input         io_tracepoint,
  input  [31:0] io_programCounter
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
`endif // RANDOMIZE_REG_INIT
  wire  mem_clock; // @[DualPortMem.scala 34:19]
  wire  mem_reset; // @[DualPortMem.scala 34:19]
  wire [5:0] mem_io_portA_address; // @[DualPortMem.scala 34:19]
  wire  mem_io_portA_read_enable; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portA_read_data_0; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portA_read_data_1; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portA_read_data_2; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portA_read_data_3; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portA_read_data_4; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portA_read_data_5; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portA_read_data_6; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portA_read_data_7; // @[DualPortMem.scala 34:19]
  wire  mem_io_portA_write_enable; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portA_write_data_0; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portA_write_data_1; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portA_write_data_2; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portA_write_data_3; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portA_write_data_4; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portA_write_data_5; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portA_write_data_6; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portA_write_data_7; // @[DualPortMem.scala 34:19]
  wire [5:0] mem_io_portB_address; // @[DualPortMem.scala 34:19]
  wire  mem_io_portB_read_enable; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portB_read_data_0; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portB_read_data_1; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portB_read_data_2; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portB_read_data_3; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portB_read_data_4; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portB_read_data_5; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portB_read_data_6; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portB_read_data_7; // @[DualPortMem.scala 34:19]
  wire  mem_io_portB_write_enable; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portB_write_data_0; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portB_write_data_1; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portB_write_data_2; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portB_write_data_3; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portB_write_data_4; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portB_write_data_5; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portB_write_data_6; // @[DualPortMem.scala 34:19]
  wire [15:0] mem_io_portB_write_data_7; // @[DualPortMem.scala 34:19]
  wire  output__clock; // @[DualPortMem.scala 49:24]
  wire  output__reset; // @[DualPortMem.scala 49:24]
  wire  output__io_enq_ready; // @[DualPortMem.scala 49:24]
  wire  output__io_enq_valid; // @[DualPortMem.scala 49:24]
  wire [15:0] output__io_enq_bits_0; // @[DualPortMem.scala 49:24]
  wire [15:0] output__io_enq_bits_1; // @[DualPortMem.scala 49:24]
  wire [15:0] output__io_enq_bits_2; // @[DualPortMem.scala 49:24]
  wire [15:0] output__io_enq_bits_3; // @[DualPortMem.scala 49:24]
  wire [15:0] output__io_enq_bits_4; // @[DualPortMem.scala 49:24]
  wire [15:0] output__io_enq_bits_5; // @[DualPortMem.scala 49:24]
  wire [15:0] output__io_enq_bits_6; // @[DualPortMem.scala 49:24]
  wire [15:0] output__io_enq_bits_7; // @[DualPortMem.scala 49:24]
  wire  output__io_deq_ready; // @[DualPortMem.scala 49:24]
  wire  output__io_deq_valid; // @[DualPortMem.scala 49:24]
  wire [15:0] output__io_deq_bits_0; // @[DualPortMem.scala 49:24]
  wire [15:0] output__io_deq_bits_1; // @[DualPortMem.scala 49:24]
  wire [15:0] output__io_deq_bits_2; // @[DualPortMem.scala 49:24]
  wire [15:0] output__io_deq_bits_3; // @[DualPortMem.scala 49:24]
  wire [15:0] output__io_deq_bits_4; // @[DualPortMem.scala 49:24]
  wire [15:0] output__io_deq_bits_5; // @[DualPortMem.scala 49:24]
  wire [15:0] output__io_deq_bits_6; // @[DualPortMem.scala 49:24]
  wire [15:0] output__io_deq_bits_7; // @[DualPortMem.scala 49:24]
  wire [1:0] output__io_count; // @[DualPortMem.scala 49:24]
  wire  output_1_clock; // @[DualPortMem.scala 49:24]
  wire  output_1_reset; // @[DualPortMem.scala 49:24]
  wire  output_1_io_enq_ready; // @[DualPortMem.scala 49:24]
  wire  output_1_io_enq_valid; // @[DualPortMem.scala 49:24]
  wire [15:0] output_1_io_enq_bits_0; // @[DualPortMem.scala 49:24]
  wire [15:0] output_1_io_enq_bits_1; // @[DualPortMem.scala 49:24]
  wire [15:0] output_1_io_enq_bits_2; // @[DualPortMem.scala 49:24]
  wire [15:0] output_1_io_enq_bits_3; // @[DualPortMem.scala 49:24]
  wire [15:0] output_1_io_enq_bits_4; // @[DualPortMem.scala 49:24]
  wire [15:0] output_1_io_enq_bits_5; // @[DualPortMem.scala 49:24]
  wire [15:0] output_1_io_enq_bits_6; // @[DualPortMem.scala 49:24]
  wire [15:0] output_1_io_enq_bits_7; // @[DualPortMem.scala 49:24]
  wire  output_1_io_deq_ready; // @[DualPortMem.scala 49:24]
  wire  output_1_io_deq_valid; // @[DualPortMem.scala 49:24]
  wire [15:0] output_1_io_deq_bits_0; // @[DualPortMem.scala 49:24]
  wire [15:0] output_1_io_deq_bits_1; // @[DualPortMem.scala 49:24]
  wire [15:0] output_1_io_deq_bits_2; // @[DualPortMem.scala 49:24]
  wire [15:0] output_1_io_deq_bits_3; // @[DualPortMem.scala 49:24]
  wire [15:0] output_1_io_deq_bits_4; // @[DualPortMem.scala 49:24]
  wire [15:0] output_1_io_deq_bits_5; // @[DualPortMem.scala 49:24]
  wire [15:0] output_1_io_deq_bits_6; // @[DualPortMem.scala 49:24]
  wire [15:0] output_1_io_deq_bits_7; // @[DualPortMem.scala 49:24]
  wire [1:0] output_1_io_count; // @[DualPortMem.scala 49:24]
  wire  outputReady = output__io_count < 2'h2; // @[DualPortMem.scala 56:39]
  reg  output_io_enq_valid_sr_0; // @[ShiftRegister.scala 10:22]
  wire  outputReady_1 = output_1_io_count < 2'h2; // @[DualPortMem.scala 56:39]
  reg  output_io_enq_valid_sr_1_0; // @[ShiftRegister.scala 10:22]
  InnerDualPortMem mem ( // @[DualPortMem.scala 34:19]
    .clock(mem_clock),
    .reset(mem_reset),
    .io_portA_address(mem_io_portA_address),
    .io_portA_read_enable(mem_io_portA_read_enable),
    .io_portA_read_data_0(mem_io_portA_read_data_0),
    .io_portA_read_data_1(mem_io_portA_read_data_1),
    .io_portA_read_data_2(mem_io_portA_read_data_2),
    .io_portA_read_data_3(mem_io_portA_read_data_3),
    .io_portA_read_data_4(mem_io_portA_read_data_4),
    .io_portA_read_data_5(mem_io_portA_read_data_5),
    .io_portA_read_data_6(mem_io_portA_read_data_6),
    .io_portA_read_data_7(mem_io_portA_read_data_7),
    .io_portA_write_enable(mem_io_portA_write_enable),
    .io_portA_write_data_0(mem_io_portA_write_data_0),
    .io_portA_write_data_1(mem_io_portA_write_data_1),
    .io_portA_write_data_2(mem_io_portA_write_data_2),
    .io_portA_write_data_3(mem_io_portA_write_data_3),
    .io_portA_write_data_4(mem_io_portA_write_data_4),
    .io_portA_write_data_5(mem_io_portA_write_data_5),
    .io_portA_write_data_6(mem_io_portA_write_data_6),
    .io_portA_write_data_7(mem_io_portA_write_data_7),
    .io_portB_address(mem_io_portB_address),
    .io_portB_read_enable(mem_io_portB_read_enable),
    .io_portB_read_data_0(mem_io_portB_read_data_0),
    .io_portB_read_data_1(mem_io_portB_read_data_1),
    .io_portB_read_data_2(mem_io_portB_read_data_2),
    .io_portB_read_data_3(mem_io_portB_read_data_3),
    .io_portB_read_data_4(mem_io_portB_read_data_4),
    .io_portB_read_data_5(mem_io_portB_read_data_5),
    .io_portB_read_data_6(mem_io_portB_read_data_6),
    .io_portB_read_data_7(mem_io_portB_read_data_7),
    .io_portB_write_enable(mem_io_portB_write_enable),
    .io_portB_write_data_0(mem_io_portB_write_data_0),
    .io_portB_write_data_1(mem_io_portB_write_data_1),
    .io_portB_write_data_2(mem_io_portB_write_data_2),
    .io_portB_write_data_3(mem_io_portB_write_data_3),
    .io_portB_write_data_4(mem_io_portB_write_data_4),
    .io_portB_write_data_5(mem_io_portB_write_data_5),
    .io_portB_write_data_6(mem_io_portB_write_data_6),
    .io_portB_write_data_7(mem_io_portB_write_data_7)
  );
  Queue_7 output_ ( // @[DualPortMem.scala 49:24]
    .clock(output__clock),
    .reset(output__reset),
    .io_enq_ready(output__io_enq_ready),
    .io_enq_valid(output__io_enq_valid),
    .io_enq_bits_0(output__io_enq_bits_0),
    .io_enq_bits_1(output__io_enq_bits_1),
    .io_enq_bits_2(output__io_enq_bits_2),
    .io_enq_bits_3(output__io_enq_bits_3),
    .io_enq_bits_4(output__io_enq_bits_4),
    .io_enq_bits_5(output__io_enq_bits_5),
    .io_enq_bits_6(output__io_enq_bits_6),
    .io_enq_bits_7(output__io_enq_bits_7),
    .io_deq_ready(output__io_deq_ready),
    .io_deq_valid(output__io_deq_valid),
    .io_deq_bits_0(output__io_deq_bits_0),
    .io_deq_bits_1(output__io_deq_bits_1),
    .io_deq_bits_2(output__io_deq_bits_2),
    .io_deq_bits_3(output__io_deq_bits_3),
    .io_deq_bits_4(output__io_deq_bits_4),
    .io_deq_bits_5(output__io_deq_bits_5),
    .io_deq_bits_6(output__io_deq_bits_6),
    .io_deq_bits_7(output__io_deq_bits_7),
    .io_count(output__io_count)
  );
  Queue_7 output_1 ( // @[DualPortMem.scala 49:24]
    .clock(output_1_clock),
    .reset(output_1_reset),
    .io_enq_ready(output_1_io_enq_ready),
    .io_enq_valid(output_1_io_enq_valid),
    .io_enq_bits_0(output_1_io_enq_bits_0),
    .io_enq_bits_1(output_1_io_enq_bits_1),
    .io_enq_bits_2(output_1_io_enq_bits_2),
    .io_enq_bits_3(output_1_io_enq_bits_3),
    .io_enq_bits_4(output_1_io_enq_bits_4),
    .io_enq_bits_5(output_1_io_enq_bits_5),
    .io_enq_bits_6(output_1_io_enq_bits_6),
    .io_enq_bits_7(output_1_io_enq_bits_7),
    .io_deq_ready(output_1_io_deq_ready),
    .io_deq_valid(output_1_io_deq_valid),
    .io_deq_bits_0(output_1_io_deq_bits_0),
    .io_deq_bits_1(output_1_io_deq_bits_1),
    .io_deq_bits_2(output_1_io_deq_bits_2),
    .io_deq_bits_3(output_1_io_deq_bits_3),
    .io_deq_bits_4(output_1_io_deq_bits_4),
    .io_deq_bits_5(output_1_io_deq_bits_5),
    .io_deq_bits_6(output_1_io_deq_bits_6),
    .io_deq_bits_7(output_1_io_deq_bits_7),
    .io_count(output_1_io_count)
  );
  assign io_portA_control_ready = io_portA_control_bits_write ? io_portA_input_valid : outputReady; // @[DualPortMem.scala 60:30 DualPortMem.scala 61:21 DualPortMem.scala 65:21]
  assign io_portA_input_ready = io_portA_control_valid & io_portA_control_bits_write; // @[DualPortMem.scala 76:34]
  assign io_portA_output_valid = output__io_deq_valid; // @[DualPortMem.scala 73:17]
  assign io_portA_output_bits_0 = output__io_deq_bits_0; // @[DualPortMem.scala 73:17]
  assign io_portA_output_bits_1 = output__io_deq_bits_1; // @[DualPortMem.scala 73:17]
  assign io_portA_output_bits_2 = output__io_deq_bits_2; // @[DualPortMem.scala 73:17]
  assign io_portA_output_bits_3 = output__io_deq_bits_3; // @[DualPortMem.scala 73:17]
  assign io_portA_output_bits_4 = output__io_deq_bits_4; // @[DualPortMem.scala 73:17]
  assign io_portA_output_bits_5 = output__io_deq_bits_5; // @[DualPortMem.scala 73:17]
  assign io_portA_output_bits_6 = output__io_deq_bits_6; // @[DualPortMem.scala 73:17]
  assign io_portA_output_bits_7 = output__io_deq_bits_7; // @[DualPortMem.scala 73:17]
  assign io_portB_control_ready = io_portB_control_bits_write ? io_portB_input_valid : outputReady_1; // @[DualPortMem.scala 60:30 DualPortMem.scala 61:21 DualPortMem.scala 65:21]
  assign io_portB_input_ready = io_portB_control_valid & io_portB_control_bits_write; // @[DualPortMem.scala 76:34]
  assign io_portB_output_valid = output_1_io_deq_valid; // @[DualPortMem.scala 73:17]
  assign io_portB_output_bits_0 = output_1_io_deq_bits_0; // @[DualPortMem.scala 73:17]
  assign io_portB_output_bits_1 = output_1_io_deq_bits_1; // @[DualPortMem.scala 73:17]
  assign io_portB_output_bits_2 = output_1_io_deq_bits_2; // @[DualPortMem.scala 73:17]
  assign io_portB_output_bits_3 = output_1_io_deq_bits_3; // @[DualPortMem.scala 73:17]
  assign io_portB_output_bits_4 = output_1_io_deq_bits_4; // @[DualPortMem.scala 73:17]
  assign io_portB_output_bits_5 = output_1_io_deq_bits_5; // @[DualPortMem.scala 73:17]
  assign io_portB_output_bits_6 = output_1_io_deq_bits_6; // @[DualPortMem.scala 73:17]
  assign io_portB_output_bits_7 = output_1_io_deq_bits_7; // @[DualPortMem.scala 73:17]
  assign mem_clock = clock;
  assign mem_reset = reset;
  assign mem_io_portA_address = io_portA_control_bits_address; // @[DualPortMem.scala 58:19]
  assign mem_io_portA_read_enable = io_portA_control_bits_write ? 1'h0 : io_portA_control_valid & outputReady; // @[DualPortMem.scala 60:30 DualPortMem.scala 63:25 DualPortMem.scala 67:25]
  assign mem_io_portA_write_enable = io_portA_control_bits_write & (io_portA_control_valid & io_portA_input_valid); // @[DualPortMem.scala 60:30 DualPortMem.scala 62:26 DualPortMem.scala 66:26]
  assign mem_io_portA_write_data_0 = io_portA_input_bits_0; // @[DualPortMem.scala 75:22]
  assign mem_io_portA_write_data_1 = io_portA_input_bits_1; // @[DualPortMem.scala 75:22]
  assign mem_io_portA_write_data_2 = io_portA_input_bits_2; // @[DualPortMem.scala 75:22]
  assign mem_io_portA_write_data_3 = io_portA_input_bits_3; // @[DualPortMem.scala 75:22]
  assign mem_io_portA_write_data_4 = io_portA_input_bits_4; // @[DualPortMem.scala 75:22]
  assign mem_io_portA_write_data_5 = io_portA_input_bits_5; // @[DualPortMem.scala 75:22]
  assign mem_io_portA_write_data_6 = io_portA_input_bits_6; // @[DualPortMem.scala 75:22]
  assign mem_io_portA_write_data_7 = io_portA_input_bits_7; // @[DualPortMem.scala 75:22]
  assign mem_io_portB_address = io_portB_control_bits_address; // @[DualPortMem.scala 58:19]
  assign mem_io_portB_read_enable = io_portB_control_bits_write ? 1'h0 : io_portB_control_valid & outputReady_1; // @[DualPortMem.scala 60:30 DualPortMem.scala 63:25 DualPortMem.scala 67:25]
  assign mem_io_portB_write_enable = io_portB_control_bits_write & (io_portB_control_valid & io_portB_input_valid); // @[DualPortMem.scala 60:30 DualPortMem.scala 62:26 DualPortMem.scala 66:26]
  assign mem_io_portB_write_data_0 = io_portB_input_bits_0; // @[DualPortMem.scala 75:22]
  assign mem_io_portB_write_data_1 = io_portB_input_bits_1; // @[DualPortMem.scala 75:22]
  assign mem_io_portB_write_data_2 = io_portB_input_bits_2; // @[DualPortMem.scala 75:22]
  assign mem_io_portB_write_data_3 = io_portB_input_bits_3; // @[DualPortMem.scala 75:22]
  assign mem_io_portB_write_data_4 = io_portB_input_bits_4; // @[DualPortMem.scala 75:22]
  assign mem_io_portB_write_data_5 = io_portB_input_bits_5; // @[DualPortMem.scala 75:22]
  assign mem_io_portB_write_data_6 = io_portB_input_bits_6; // @[DualPortMem.scala 75:22]
  assign mem_io_portB_write_data_7 = io_portB_input_bits_7; // @[DualPortMem.scala 75:22]
  assign output__clock = clock;
  assign output__reset = reset;
  assign output__io_enq_valid = output_io_enq_valid_sr_0; // @[DualPortMem.scala 71:25]
  assign output__io_enq_bits_0 = mem_io_portA_read_data_0; // @[DualPortMem.scala 70:24]
  assign output__io_enq_bits_1 = mem_io_portA_read_data_1; // @[DualPortMem.scala 70:24]
  assign output__io_enq_bits_2 = mem_io_portA_read_data_2; // @[DualPortMem.scala 70:24]
  assign output__io_enq_bits_3 = mem_io_portA_read_data_3; // @[DualPortMem.scala 70:24]
  assign output__io_enq_bits_4 = mem_io_portA_read_data_4; // @[DualPortMem.scala 70:24]
  assign output__io_enq_bits_5 = mem_io_portA_read_data_5; // @[DualPortMem.scala 70:24]
  assign output__io_enq_bits_6 = mem_io_portA_read_data_6; // @[DualPortMem.scala 70:24]
  assign output__io_enq_bits_7 = mem_io_portA_read_data_7; // @[DualPortMem.scala 70:24]
  assign output__io_deq_ready = io_portA_output_ready; // @[DualPortMem.scala 73:17]
  assign output_1_clock = clock;
  assign output_1_reset = reset;
  assign output_1_io_enq_valid = output_io_enq_valid_sr_1_0; // @[DualPortMem.scala 71:25]
  assign output_1_io_enq_bits_0 = mem_io_portB_read_data_0; // @[DualPortMem.scala 70:24]
  assign output_1_io_enq_bits_1 = mem_io_portB_read_data_1; // @[DualPortMem.scala 70:24]
  assign output_1_io_enq_bits_2 = mem_io_portB_read_data_2; // @[DualPortMem.scala 70:24]
  assign output_1_io_enq_bits_3 = mem_io_portB_read_data_3; // @[DualPortMem.scala 70:24]
  assign output_1_io_enq_bits_4 = mem_io_portB_read_data_4; // @[DualPortMem.scala 70:24]
  assign output_1_io_enq_bits_5 = mem_io_portB_read_data_5; // @[DualPortMem.scala 70:24]
  assign output_1_io_enq_bits_6 = mem_io_portB_read_data_6; // @[DualPortMem.scala 70:24]
  assign output_1_io_enq_bits_7 = mem_io_portB_read_data_7; // @[DualPortMem.scala 70:24]
  assign output_1_io_deq_ready = io_portB_output_ready; // @[DualPortMem.scala 73:17]
  always @(posedge clock) begin
    if (reset) begin // @[ShiftRegister.scala 10:22]
      output_io_enq_valid_sr_0 <= 1'h0; // @[ShiftRegister.scala 10:22]
    end else begin
      output_io_enq_valid_sr_0 <= mem_io_portA_read_enable; // @[ShiftRegister.scala 25:12]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      output_io_enq_valid_sr_1_0 <= 1'h0; // @[ShiftRegister.scala 10:22]
    end else begin
      output_io_enq_valid_sr_1_0 <= mem_io_portB_read_enable; // @[ShiftRegister.scala 25:12]
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
  output_io_enq_valid_sr_0 = _RAND_0[0:0];
  _RAND_1 = {1{`RANDOM}};
  output_io_enq_valid_sr_1_0 = _RAND_1[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Queue_11(
  input         clock,
  input         reset,
  output        io_enq_ready,
  input         io_enq_valid,
  input  [15:0] io_enq_bits_0,
  input  [15:0] io_enq_bits_1,
  input  [15:0] io_enq_bits_2,
  input  [15:0] io_enq_bits_3,
  input  [15:0] io_enq_bits_4,
  input  [15:0] io_enq_bits_5,
  input  [15:0] io_enq_bits_6,
  input  [15:0] io_enq_bits_7,
  input         io_deq_ready,
  output        io_deq_valid,
  output [15:0] io_deq_bits_0,
  output [15:0] io_deq_bits_1,
  output [15:0] io_deq_bits_2,
  output [15:0] io_deq_bits_3,
  output [15:0] io_deq_bits_4,
  output [15:0] io_deq_bits_5,
  output [15:0] io_deq_bits_6,
  output [15:0] io_deq_bits_7
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_6;
  reg [31:0] _RAND_7;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_8;
`endif // RANDOMIZE_REG_INIT
  reg [15:0] ram_0 [0:0]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_0_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_0_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_0_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_0_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_0_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_0_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_1 [0:0]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_1_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_1_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_1_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_1_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_1_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_1_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_2 [0:0]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_2_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_2_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_2_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_2_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_2_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_2_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_3 [0:0]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_3_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_3_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_3_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_3_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_3_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_3_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_4 [0:0]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_4_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_4_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_4_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_4_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_4_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_4_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_5 [0:0]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_5_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_5_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_5_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_5_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_5_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_5_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_6 [0:0]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_6_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_6_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_6_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_6_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_6_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_6_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_7 [0:0]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_7_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_7_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_7_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_7_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_7_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_7_MPORT_en; // @[Decoupled.scala 218:16]
  reg  maybe_full; // @[Decoupled.scala 221:27]
  wire  empty = ~maybe_full; // @[Decoupled.scala 224:28]
  wire  _do_enq_T = io_enq_ready & io_enq_valid; // @[Decoupled.scala 40:37]
  wire  _do_deq_T = io_deq_ready & io_deq_valid; // @[Decoupled.scala 40:37]
  wire  _GEN_14 = io_deq_ready ? 1'h0 : _do_enq_T; // @[Decoupled.scala 249:27 Decoupled.scala 249:36]
  wire  do_enq = empty ? _GEN_14 : _do_enq_T; // @[Decoupled.scala 246:18]
  wire  do_deq = empty ? 1'h0 : _do_deq_T; // @[Decoupled.scala 246:18 Decoupled.scala 248:14]
  assign ram_0_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_0_io_deq_bits_MPORT_data = ram_0[ram_0_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_0_MPORT_data = io_enq_bits_0;
  assign ram_0_MPORT_addr = 1'h0;
  assign ram_0_MPORT_mask = 1'h1;
  assign ram_0_MPORT_en = empty ? _GEN_14 : _do_enq_T;
  assign ram_1_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_1_io_deq_bits_MPORT_data = ram_1[ram_1_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_1_MPORT_data = io_enq_bits_1;
  assign ram_1_MPORT_addr = 1'h0;
  assign ram_1_MPORT_mask = 1'h1;
  assign ram_1_MPORT_en = empty ? _GEN_14 : _do_enq_T;
  assign ram_2_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_2_io_deq_bits_MPORT_data = ram_2[ram_2_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_2_MPORT_data = io_enq_bits_2;
  assign ram_2_MPORT_addr = 1'h0;
  assign ram_2_MPORT_mask = 1'h1;
  assign ram_2_MPORT_en = empty ? _GEN_14 : _do_enq_T;
  assign ram_3_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_3_io_deq_bits_MPORT_data = ram_3[ram_3_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_3_MPORT_data = io_enq_bits_3;
  assign ram_3_MPORT_addr = 1'h0;
  assign ram_3_MPORT_mask = 1'h1;
  assign ram_3_MPORT_en = empty ? _GEN_14 : _do_enq_T;
  assign ram_4_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_4_io_deq_bits_MPORT_data = ram_4[ram_4_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_4_MPORT_data = io_enq_bits_4;
  assign ram_4_MPORT_addr = 1'h0;
  assign ram_4_MPORT_mask = 1'h1;
  assign ram_4_MPORT_en = empty ? _GEN_14 : _do_enq_T;
  assign ram_5_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_5_io_deq_bits_MPORT_data = ram_5[ram_5_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_5_MPORT_data = io_enq_bits_5;
  assign ram_5_MPORT_addr = 1'h0;
  assign ram_5_MPORT_mask = 1'h1;
  assign ram_5_MPORT_en = empty ? _GEN_14 : _do_enq_T;
  assign ram_6_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_6_io_deq_bits_MPORT_data = ram_6[ram_6_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_6_MPORT_data = io_enq_bits_6;
  assign ram_6_MPORT_addr = 1'h0;
  assign ram_6_MPORT_mask = 1'h1;
  assign ram_6_MPORT_en = empty ? _GEN_14 : _do_enq_T;
  assign ram_7_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_7_io_deq_bits_MPORT_data = ram_7[ram_7_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_7_MPORT_data = io_enq_bits_7;
  assign ram_7_MPORT_addr = 1'h0;
  assign ram_7_MPORT_mask = 1'h1;
  assign ram_7_MPORT_en = empty ? _GEN_14 : _do_enq_T;
  assign io_enq_ready = io_deq_ready | empty; // @[Decoupled.scala 254:25 Decoupled.scala 254:40 Decoupled.scala 241:16]
  assign io_deq_valid = io_enq_valid | ~empty; // @[Decoupled.scala 245:25 Decoupled.scala 245:40 Decoupled.scala 240:16]
  assign io_deq_bits_0 = empty ? $signed(io_enq_bits_0) : $signed(ram_0_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_1 = empty ? $signed(io_enq_bits_1) : $signed(ram_1_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_2 = empty ? $signed(io_enq_bits_2) : $signed(ram_2_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_3 = empty ? $signed(io_enq_bits_3) : $signed(ram_3_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_4 = empty ? $signed(io_enq_bits_4) : $signed(ram_4_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_5 = empty ? $signed(io_enq_bits_5) : $signed(ram_5_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_6 = empty ? $signed(io_enq_bits_6) : $signed(ram_6_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_7 = empty ? $signed(io_enq_bits_7) : $signed(ram_7_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  always @(posedge clock) begin
    if(ram_0_MPORT_en & ram_0_MPORT_mask) begin
      ram_0[ram_0_MPORT_addr] <= ram_0_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_1_MPORT_en & ram_1_MPORT_mask) begin
      ram_1[ram_1_MPORT_addr] <= ram_1_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_2_MPORT_en & ram_2_MPORT_mask) begin
      ram_2[ram_2_MPORT_addr] <= ram_2_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_3_MPORT_en & ram_3_MPORT_mask) begin
      ram_3[ram_3_MPORT_addr] <= ram_3_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_4_MPORT_en & ram_4_MPORT_mask) begin
      ram_4[ram_4_MPORT_addr] <= ram_4_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_5_MPORT_en & ram_5_MPORT_mask) begin
      ram_5[ram_5_MPORT_addr] <= ram_5_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_6_MPORT_en & ram_6_MPORT_mask) begin
      ram_6[ram_6_MPORT_addr] <= ram_6_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_7_MPORT_en & ram_7_MPORT_mask) begin
      ram_7[ram_7_MPORT_addr] <= ram_7_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if (reset) begin // @[Decoupled.scala 221:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 221:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 236:28]
      if (empty) begin // @[Decoupled.scala 246:18]
        if (io_deq_ready) begin // @[Decoupled.scala 249:27]
          maybe_full <= 1'h0; // @[Decoupled.scala 249:36]
        end else begin
          maybe_full <= _do_enq_T;
        end
      end else begin
        maybe_full <= _do_enq_T;
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
`ifdef RANDOMIZE_MEM_INIT
  _RAND_0 = {1{`RANDOM}};
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram_0[initvar] = _RAND_0[15:0];
  _RAND_1 = {1{`RANDOM}};
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram_1[initvar] = _RAND_1[15:0];
  _RAND_2 = {1{`RANDOM}};
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram_2[initvar] = _RAND_2[15:0];
  _RAND_3 = {1{`RANDOM}};
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram_3[initvar] = _RAND_3[15:0];
  _RAND_4 = {1{`RANDOM}};
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram_4[initvar] = _RAND_4[15:0];
  _RAND_5 = {1{`RANDOM}};
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram_5[initvar] = _RAND_5[15:0];
  _RAND_6 = {1{`RANDOM}};
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram_6[initvar] = _RAND_6[15:0];
  _RAND_7 = {1{`RANDOM}};
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram_7[initvar] = _RAND_7[15:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_8 = {1{`RANDOM}};
  maybe_full = _RAND_8[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module VecAdder(
  input         clock,
  input         reset,
  output        io_left_ready,
  input         io_left_valid,
  input  [15:0] io_left_bits_0,
  input  [15:0] io_left_bits_1,
  input  [15:0] io_left_bits_2,
  input  [15:0] io_left_bits_3,
  input  [15:0] io_left_bits_4,
  input  [15:0] io_left_bits_5,
  input  [15:0] io_left_bits_6,
  input  [15:0] io_left_bits_7,
  output        io_right_ready,
  input         io_right_valid,
  input  [15:0] io_right_bits_0,
  input  [15:0] io_right_bits_1,
  input  [15:0] io_right_bits_2,
  input  [15:0] io_right_bits_3,
  input  [15:0] io_right_bits_4,
  input  [15:0] io_right_bits_5,
  input  [15:0] io_right_bits_6,
  input  [15:0] io_right_bits_7,
  input         io_output_ready,
  output        io_output_valid,
  output [15:0] io_output_bits_0,
  output [15:0] io_output_bits_1,
  output [15:0] io_output_bits_2,
  output [15:0] io_output_bits_3,
  output [15:0] io_output_bits_4,
  output [15:0] io_output_bits_5,
  output [15:0] io_output_bits_6,
  output [15:0] io_output_bits_7
);
  wire  left_clock; // @[Decoupled.scala 296:21]
  wire  left_reset; // @[Decoupled.scala 296:21]
  wire  left_io_enq_ready; // @[Decoupled.scala 296:21]
  wire  left_io_enq_valid; // @[Decoupled.scala 296:21]
  wire [15:0] left_io_enq_bits_0; // @[Decoupled.scala 296:21]
  wire [15:0] left_io_enq_bits_1; // @[Decoupled.scala 296:21]
  wire [15:0] left_io_enq_bits_2; // @[Decoupled.scala 296:21]
  wire [15:0] left_io_enq_bits_3; // @[Decoupled.scala 296:21]
  wire [15:0] left_io_enq_bits_4; // @[Decoupled.scala 296:21]
  wire [15:0] left_io_enq_bits_5; // @[Decoupled.scala 296:21]
  wire [15:0] left_io_enq_bits_6; // @[Decoupled.scala 296:21]
  wire [15:0] left_io_enq_bits_7; // @[Decoupled.scala 296:21]
  wire  left_io_deq_ready; // @[Decoupled.scala 296:21]
  wire  left_io_deq_valid; // @[Decoupled.scala 296:21]
  wire [15:0] left_io_deq_bits_0; // @[Decoupled.scala 296:21]
  wire [15:0] left_io_deq_bits_1; // @[Decoupled.scala 296:21]
  wire [15:0] left_io_deq_bits_2; // @[Decoupled.scala 296:21]
  wire [15:0] left_io_deq_bits_3; // @[Decoupled.scala 296:21]
  wire [15:0] left_io_deq_bits_4; // @[Decoupled.scala 296:21]
  wire [15:0] left_io_deq_bits_5; // @[Decoupled.scala 296:21]
  wire [15:0] left_io_deq_bits_6; // @[Decoupled.scala 296:21]
  wire [15:0] left_io_deq_bits_7; // @[Decoupled.scala 296:21]
  wire  right_clock; // @[Decoupled.scala 296:21]
  wire  right_reset; // @[Decoupled.scala 296:21]
  wire  right_io_enq_ready; // @[Decoupled.scala 296:21]
  wire  right_io_enq_valid; // @[Decoupled.scala 296:21]
  wire [15:0] right_io_enq_bits_0; // @[Decoupled.scala 296:21]
  wire [15:0] right_io_enq_bits_1; // @[Decoupled.scala 296:21]
  wire [15:0] right_io_enq_bits_2; // @[Decoupled.scala 296:21]
  wire [15:0] right_io_enq_bits_3; // @[Decoupled.scala 296:21]
  wire [15:0] right_io_enq_bits_4; // @[Decoupled.scala 296:21]
  wire [15:0] right_io_enq_bits_5; // @[Decoupled.scala 296:21]
  wire [15:0] right_io_enq_bits_6; // @[Decoupled.scala 296:21]
  wire [15:0] right_io_enq_bits_7; // @[Decoupled.scala 296:21]
  wire  right_io_deq_ready; // @[Decoupled.scala 296:21]
  wire  right_io_deq_valid; // @[Decoupled.scala 296:21]
  wire [15:0] right_io_deq_bits_0; // @[Decoupled.scala 296:21]
  wire [15:0] right_io_deq_bits_1; // @[Decoupled.scala 296:21]
  wire [15:0] right_io_deq_bits_2; // @[Decoupled.scala 296:21]
  wire [15:0] right_io_deq_bits_3; // @[Decoupled.scala 296:21]
  wire [15:0] right_io_deq_bits_4; // @[Decoupled.scala 296:21]
  wire [15:0] right_io_deq_bits_5; // @[Decoupled.scala 296:21]
  wire [15:0] right_io_deq_bits_6; // @[Decoupled.scala 296:21]
  wire [15:0] right_io_deq_bits_7; // @[Decoupled.scala 296:21]
  Queue_11 left ( // @[Decoupled.scala 296:21]
    .clock(left_clock),
    .reset(left_reset),
    .io_enq_ready(left_io_enq_ready),
    .io_enq_valid(left_io_enq_valid),
    .io_enq_bits_0(left_io_enq_bits_0),
    .io_enq_bits_1(left_io_enq_bits_1),
    .io_enq_bits_2(left_io_enq_bits_2),
    .io_enq_bits_3(left_io_enq_bits_3),
    .io_enq_bits_4(left_io_enq_bits_4),
    .io_enq_bits_5(left_io_enq_bits_5),
    .io_enq_bits_6(left_io_enq_bits_6),
    .io_enq_bits_7(left_io_enq_bits_7),
    .io_deq_ready(left_io_deq_ready),
    .io_deq_valid(left_io_deq_valid),
    .io_deq_bits_0(left_io_deq_bits_0),
    .io_deq_bits_1(left_io_deq_bits_1),
    .io_deq_bits_2(left_io_deq_bits_2),
    .io_deq_bits_3(left_io_deq_bits_3),
    .io_deq_bits_4(left_io_deq_bits_4),
    .io_deq_bits_5(left_io_deq_bits_5),
    .io_deq_bits_6(left_io_deq_bits_6),
    .io_deq_bits_7(left_io_deq_bits_7)
  );
  Queue_11 right ( // @[Decoupled.scala 296:21]
    .clock(right_clock),
    .reset(right_reset),
    .io_enq_ready(right_io_enq_ready),
    .io_enq_valid(right_io_enq_valid),
    .io_enq_bits_0(right_io_enq_bits_0),
    .io_enq_bits_1(right_io_enq_bits_1),
    .io_enq_bits_2(right_io_enq_bits_2),
    .io_enq_bits_3(right_io_enq_bits_3),
    .io_enq_bits_4(right_io_enq_bits_4),
    .io_enq_bits_5(right_io_enq_bits_5),
    .io_enq_bits_6(right_io_enq_bits_6),
    .io_enq_bits_7(right_io_enq_bits_7),
    .io_deq_ready(right_io_deq_ready),
    .io_deq_valid(right_io_deq_valid),
    .io_deq_bits_0(right_io_deq_bits_0),
    .io_deq_bits_1(right_io_deq_bits_1),
    .io_deq_bits_2(right_io_deq_bits_2),
    .io_deq_bits_3(right_io_deq_bits_3),
    .io_deq_bits_4(right_io_deq_bits_4),
    .io_deq_bits_5(right_io_deq_bits_5),
    .io_deq_bits_6(right_io_deq_bits_6),
    .io_deq_bits_7(right_io_deq_bits_7)
  );
  assign io_left_ready = left_io_enq_ready; // @[Decoupled.scala 299:17]
  assign io_right_ready = right_io_enq_ready; // @[Decoupled.scala 299:17]
  assign io_output_valid = left_io_deq_valid & right_io_deq_valid; // @[VecAdder.scala 23:33]
  assign io_output_bits_0 = $signed(left_io_deq_bits_0) + $signed(right_io_deq_bits_0); // @[VecAdder.scala 21:39]
  assign io_output_bits_1 = $signed(left_io_deq_bits_1) + $signed(right_io_deq_bits_1); // @[VecAdder.scala 21:39]
  assign io_output_bits_2 = $signed(left_io_deq_bits_2) + $signed(right_io_deq_bits_2); // @[VecAdder.scala 21:39]
  assign io_output_bits_3 = $signed(left_io_deq_bits_3) + $signed(right_io_deq_bits_3); // @[VecAdder.scala 21:39]
  assign io_output_bits_4 = $signed(left_io_deq_bits_4) + $signed(right_io_deq_bits_4); // @[VecAdder.scala 21:39]
  assign io_output_bits_5 = $signed(left_io_deq_bits_5) + $signed(right_io_deq_bits_5); // @[VecAdder.scala 21:39]
  assign io_output_bits_6 = $signed(left_io_deq_bits_6) + $signed(right_io_deq_bits_6); // @[VecAdder.scala 21:39]
  assign io_output_bits_7 = $signed(left_io_deq_bits_7) + $signed(right_io_deq_bits_7); // @[VecAdder.scala 21:39]
  assign left_clock = clock;
  assign left_reset = reset;
  assign left_io_enq_valid = io_left_valid; // @[Decoupled.scala 297:22]
  assign left_io_enq_bits_0 = io_left_bits_0; // @[Decoupled.scala 298:21]
  assign left_io_enq_bits_1 = io_left_bits_1; // @[Decoupled.scala 298:21]
  assign left_io_enq_bits_2 = io_left_bits_2; // @[Decoupled.scala 298:21]
  assign left_io_enq_bits_3 = io_left_bits_3; // @[Decoupled.scala 298:21]
  assign left_io_enq_bits_4 = io_left_bits_4; // @[Decoupled.scala 298:21]
  assign left_io_enq_bits_5 = io_left_bits_5; // @[Decoupled.scala 298:21]
  assign left_io_enq_bits_6 = io_left_bits_6; // @[Decoupled.scala 298:21]
  assign left_io_enq_bits_7 = io_left_bits_7; // @[Decoupled.scala 298:21]
  assign left_io_deq_ready = io_output_ready & right_io_deq_valid; // @[VecAdder.scala 24:33]
  assign right_clock = clock;
  assign right_reset = reset;
  assign right_io_enq_valid = io_right_valid; // @[Decoupled.scala 297:22]
  assign right_io_enq_bits_0 = io_right_bits_0; // @[Decoupled.scala 298:21]
  assign right_io_enq_bits_1 = io_right_bits_1; // @[Decoupled.scala 298:21]
  assign right_io_enq_bits_2 = io_right_bits_2; // @[Decoupled.scala 298:21]
  assign right_io_enq_bits_3 = io_right_bits_3; // @[Decoupled.scala 298:21]
  assign right_io_enq_bits_4 = io_right_bits_4; // @[Decoupled.scala 298:21]
  assign right_io_enq_bits_5 = io_right_bits_5; // @[Decoupled.scala 298:21]
  assign right_io_enq_bits_6 = io_right_bits_6; // @[Decoupled.scala 298:21]
  assign right_io_enq_bits_7 = io_right_bits_7; // @[Decoupled.scala 298:21]
  assign right_io_deq_ready = io_output_ready & left_io_deq_valid; // @[VecAdder.scala 25:34]
endmodule
module Queue_13(
  input        clock,
  input        reset,
  output       io_enq_ready,
  input        io_enq_valid,
  input  [5:0] io_enq_bits_address,
  input        io_enq_bits_accumulate,
  input        io_enq_bits_write,
  input        io_deq_ready,
  output       io_deq_valid,
  output [5:0] io_deq_bits_address,
  output       io_deq_bits_accumulate,
  output       io_deq_bits_write
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_3;
`endif // RANDOMIZE_REG_INIT
  reg [5:0] ram_address [0:0]; // @[Decoupled.scala 218:16]
  wire [5:0] ram_address_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_address_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [5:0] ram_address_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_address_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_address_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_address_MPORT_en; // @[Decoupled.scala 218:16]
  reg  ram_accumulate [0:0]; // @[Decoupled.scala 218:16]
  wire  ram_accumulate_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_accumulate_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_accumulate_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_accumulate_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_accumulate_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_accumulate_MPORT_en; // @[Decoupled.scala 218:16]
  reg  ram_write [0:0]; // @[Decoupled.scala 218:16]
  wire  ram_write_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_write_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_write_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_write_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_write_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_write_MPORT_en; // @[Decoupled.scala 218:16]
  reg  maybe_full; // @[Decoupled.scala 221:27]
  wire  empty = ~maybe_full; // @[Decoupled.scala 224:28]
  wire  _do_enq_T = io_enq_ready & io_enq_valid; // @[Decoupled.scala 40:37]
  wire  _do_deq_T = io_deq_ready & io_deq_valid; // @[Decoupled.scala 40:37]
  wire  _GEN_9 = io_deq_ready ? 1'h0 : _do_enq_T; // @[Decoupled.scala 249:27 Decoupled.scala 249:36]
  wire  do_enq = empty ? _GEN_9 : _do_enq_T; // @[Decoupled.scala 246:18]
  wire  do_deq = empty ? 1'h0 : _do_deq_T; // @[Decoupled.scala 246:18 Decoupled.scala 248:14]
  assign ram_address_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_address_io_deq_bits_MPORT_data = ram_address[ram_address_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_address_MPORT_data = io_enq_bits_address;
  assign ram_address_MPORT_addr = 1'h0;
  assign ram_address_MPORT_mask = 1'h1;
  assign ram_address_MPORT_en = empty ? _GEN_9 : _do_enq_T;
  assign ram_accumulate_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_accumulate_io_deq_bits_MPORT_data = ram_accumulate[ram_accumulate_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_accumulate_MPORT_data = io_enq_bits_accumulate;
  assign ram_accumulate_MPORT_addr = 1'h0;
  assign ram_accumulate_MPORT_mask = 1'h1;
  assign ram_accumulate_MPORT_en = empty ? _GEN_9 : _do_enq_T;
  assign ram_write_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_write_io_deq_bits_MPORT_data = ram_write[ram_write_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_write_MPORT_data = io_enq_bits_write;
  assign ram_write_MPORT_addr = 1'h0;
  assign ram_write_MPORT_mask = 1'h1;
  assign ram_write_MPORT_en = empty ? _GEN_9 : _do_enq_T;
  assign io_enq_ready = io_deq_ready | empty; // @[Decoupled.scala 254:25 Decoupled.scala 254:40 Decoupled.scala 241:16]
  assign io_deq_valid = io_enq_valid | ~empty; // @[Decoupled.scala 245:25 Decoupled.scala 245:40 Decoupled.scala 240:16]
  assign io_deq_bits_address = empty ? io_enq_bits_address : ram_address_io_deq_bits_MPORT_data; // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_accumulate = empty ? io_enq_bits_accumulate : ram_accumulate_io_deq_bits_MPORT_data; // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_write = empty ? io_enq_bits_write : ram_write_io_deq_bits_MPORT_data; // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  always @(posedge clock) begin
    if(ram_address_MPORT_en & ram_address_MPORT_mask) begin
      ram_address[ram_address_MPORT_addr] <= ram_address_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_accumulate_MPORT_en & ram_accumulate_MPORT_mask) begin
      ram_accumulate[ram_accumulate_MPORT_addr] <= ram_accumulate_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_write_MPORT_en & ram_write_MPORT_mask) begin
      ram_write[ram_write_MPORT_addr] <= ram_write_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if (reset) begin // @[Decoupled.scala 221:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 221:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 236:28]
      if (empty) begin // @[Decoupled.scala 246:18]
        if (io_deq_ready) begin // @[Decoupled.scala 249:27]
          maybe_full <= 1'h0; // @[Decoupled.scala 249:36]
        end else begin
          maybe_full <= _do_enq_T;
        end
      end else begin
        maybe_full <= _do_enq_T;
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
`ifdef RANDOMIZE_MEM_INIT
  _RAND_0 = {1{`RANDOM}};
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram_address[initvar] = _RAND_0[5:0];
  _RAND_1 = {1{`RANDOM}};
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram_accumulate[initvar] = _RAND_1[0:0];
  _RAND_2 = {1{`RANDOM}};
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram_write[initvar] = _RAND_2[0:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_3 = {1{`RANDOM}};
  maybe_full = _RAND_3[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Queue_15(
  input        clock,
  input        reset,
  output       io_enq_ready,
  input        io_enq_valid,
  input        io_enq_bits_write,
  input  [5:0] io_enq_bits_address,
  input        io_deq_ready,
  output       io_deq_valid,
  output       io_deq_bits_write,
  output [5:0] io_deq_bits_address
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_2;
`endif // RANDOMIZE_REG_INIT
  reg  ram_write [0:0]; // @[Decoupled.scala 218:16]
  wire  ram_write_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_write_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_write_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_write_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_write_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_write_MPORT_en; // @[Decoupled.scala 218:16]
  reg [5:0] ram_address [0:0]; // @[Decoupled.scala 218:16]
  wire [5:0] ram_address_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_address_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [5:0] ram_address_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_address_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_address_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_address_MPORT_en; // @[Decoupled.scala 218:16]
  reg  maybe_full; // @[Decoupled.scala 221:27]
  wire  empty = ~maybe_full; // @[Decoupled.scala 224:28]
  wire  _do_enq_T = io_enq_ready & io_enq_valid; // @[Decoupled.scala 40:37]
  wire  _do_deq_T = io_deq_ready & io_deq_valid; // @[Decoupled.scala 40:37]
  wire  _GEN_9 = io_deq_ready ? 1'h0 : _do_enq_T; // @[Decoupled.scala 249:27 Decoupled.scala 249:36]
  wire  do_enq = empty ? _GEN_9 : _do_enq_T; // @[Decoupled.scala 246:18]
  wire  do_deq = empty ? 1'h0 : _do_deq_T; // @[Decoupled.scala 246:18 Decoupled.scala 248:14]
  assign ram_write_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_write_io_deq_bits_MPORT_data = ram_write[ram_write_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_write_MPORT_data = io_enq_bits_write;
  assign ram_write_MPORT_addr = 1'h0;
  assign ram_write_MPORT_mask = 1'h1;
  assign ram_write_MPORT_en = empty ? _GEN_9 : _do_enq_T;
  assign ram_address_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_address_io_deq_bits_MPORT_data = ram_address[ram_address_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_address_MPORT_data = io_enq_bits_address;
  assign ram_address_MPORT_addr = 1'h0;
  assign ram_address_MPORT_mask = 1'h1;
  assign ram_address_MPORT_en = empty ? _GEN_9 : _do_enq_T;
  assign io_enq_ready = io_deq_ready | empty; // @[Decoupled.scala 254:25 Decoupled.scala 254:40 Decoupled.scala 241:16]
  assign io_deq_valid = io_enq_valid | ~empty; // @[Decoupled.scala 245:25 Decoupled.scala 245:40 Decoupled.scala 240:16]
  assign io_deq_bits_write = empty ? io_enq_bits_write : ram_write_io_deq_bits_MPORT_data; // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_address = empty ? io_enq_bits_address : ram_address_io_deq_bits_MPORT_data; // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  always @(posedge clock) begin
    if(ram_write_MPORT_en & ram_write_MPORT_mask) begin
      ram_write[ram_write_MPORT_addr] <= ram_write_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_address_MPORT_en & ram_address_MPORT_mask) begin
      ram_address[ram_address_MPORT_addr] <= ram_address_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if (reset) begin // @[Decoupled.scala 221:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 221:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 236:28]
      if (empty) begin // @[Decoupled.scala 246:18]
        if (io_deq_ready) begin // @[Decoupled.scala 249:27]
          maybe_full <= 1'h0; // @[Decoupled.scala 249:36]
        end else begin
          maybe_full <= _do_enq_T;
        end
      end else begin
        maybe_full <= _do_enq_T;
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
`ifdef RANDOMIZE_MEM_INIT
  _RAND_0 = {1{`RANDOM}};
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram_write[initvar] = _RAND_0[0:0];
  _RAND_1 = {1{`RANDOM}};
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram_address[initvar] = _RAND_1[5:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_2 = {1{`RANDOM}};
  maybe_full = _RAND_2[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Demux(
  output        io_in_ready,
  input         io_in_valid,
  input  [15:0] io_in_bits_0,
  input  [15:0] io_in_bits_1,
  input  [15:0] io_in_bits_2,
  input  [15:0] io_in_bits_3,
  input  [15:0] io_in_bits_4,
  input  [15:0] io_in_bits_5,
  input  [15:0] io_in_bits_6,
  input  [15:0] io_in_bits_7,
  output        io_sel_ready,
  input         io_sel_valid,
  input         io_sel_bits,
  input         io_out_0_ready,
  output        io_out_0_valid,
  output [15:0] io_out_0_bits_0,
  output [15:0] io_out_0_bits_1,
  output [15:0] io_out_0_bits_2,
  output [15:0] io_out_0_bits_3,
  output [15:0] io_out_0_bits_4,
  output [15:0] io_out_0_bits_5,
  output [15:0] io_out_0_bits_6,
  output [15:0] io_out_0_bits_7,
  input         io_out_1_ready,
  output        io_out_1_valid,
  output [15:0] io_out_1_bits_0,
  output [15:0] io_out_1_bits_1,
  output [15:0] io_out_1_bits_2,
  output [15:0] io_out_1_bits_3,
  output [15:0] io_out_1_bits_4,
  output [15:0] io_out_1_bits_5,
  output [15:0] io_out_1_bits_6,
  output [15:0] io_out_1_bits_7
);
  wire  _GEN_19 = io_sel_bits ? io_out_1_ready : io_out_0_ready; // @[Demux.scala 34:25 Demux.scala 34:25]
  assign io_in_ready = io_sel_valid & _GEN_19; // @[Demux.scala 35:25]
  assign io_sel_ready = io_in_valid & _GEN_19; // @[Demux.scala 34:25]
  assign io_out_0_valid = ~io_sel_bits & (io_sel_valid & io_in_valid); // @[Demux.scala 33:13 Demux.scala 33:13 Demux.scala 28:15]
  assign io_out_0_bits_0 = ~io_sel_bits ? $signed(io_in_bits_0) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_0_bits_1 = ~io_sel_bits ? $signed(io_in_bits_1) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_0_bits_2 = ~io_sel_bits ? $signed(io_in_bits_2) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_0_bits_3 = ~io_sel_bits ? $signed(io_in_bits_3) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_0_bits_4 = ~io_sel_bits ? $signed(io_in_bits_4) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_0_bits_5 = ~io_sel_bits ? $signed(io_in_bits_5) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_0_bits_6 = ~io_sel_bits ? $signed(io_in_bits_6) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_0_bits_7 = ~io_sel_bits ? $signed(io_in_bits_7) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_1_valid = io_sel_bits & (io_sel_valid & io_in_valid); // @[Demux.scala 33:13 Demux.scala 33:13 Demux.scala 28:15]
  assign io_out_1_bits_0 = io_sel_bits ? $signed(io_in_bits_0) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_1_bits_1 = io_sel_bits ? $signed(io_in_bits_1) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_1_bits_2 = io_sel_bits ? $signed(io_in_bits_2) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_1_bits_3 = io_sel_bits ? $signed(io_in_bits_3) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_1_bits_4 = io_sel_bits ? $signed(io_in_bits_4) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_1_bits_5 = io_sel_bits ? $signed(io_in_bits_5) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_1_bits_6 = io_sel_bits ? $signed(io_in_bits_6) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_1_bits_7 = io_sel_bits ? $signed(io_in_bits_7) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
endmodule
module Queue_16(
  input   clock,
  input   reset,
  output  io_enq_ready,
  input   io_enq_valid,
  input   io_enq_bits,
  input   io_deq_ready,
  output  io_deq_valid,
  output  io_deq_bits
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_1;
`endif // RANDOMIZE_REG_INIT
  reg  ram [0:0]; // @[Decoupled.scala 218:16]
  wire  ram_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_MPORT_en; // @[Decoupled.scala 218:16]
  reg  maybe_full; // @[Decoupled.scala 221:27]
  wire  empty = ~maybe_full; // @[Decoupled.scala 224:28]
  wire  _do_enq_T = io_enq_ready & io_enq_valid; // @[Decoupled.scala 40:37]
  wire  _do_deq_T = io_deq_ready & io_deq_valid; // @[Decoupled.scala 40:37]
  wire  _GEN_7 = io_deq_ready ? 1'h0 : _do_enq_T; // @[Decoupled.scala 249:27 Decoupled.scala 249:36]
  wire  do_enq = empty ? _GEN_7 : _do_enq_T; // @[Decoupled.scala 246:18]
  wire  do_deq = empty ? 1'h0 : _do_deq_T; // @[Decoupled.scala 246:18 Decoupled.scala 248:14]
  assign ram_io_deq_bits_MPORT_addr = 1'h0;
  assign ram_io_deq_bits_MPORT_data = ram[ram_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_MPORT_data = io_enq_bits;
  assign ram_MPORT_addr = 1'h0;
  assign ram_MPORT_mask = 1'h1;
  assign ram_MPORT_en = empty ? _GEN_7 : _do_enq_T;
  assign io_enq_ready = io_deq_ready | empty; // @[Decoupled.scala 254:25 Decoupled.scala 254:40 Decoupled.scala 241:16]
  assign io_deq_valid = io_enq_valid | ~empty; // @[Decoupled.scala 245:25 Decoupled.scala 245:40 Decoupled.scala 240:16]
  assign io_deq_bits = empty ? io_enq_bits : ram_io_deq_bits_MPORT_data; // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  always @(posedge clock) begin
    if(ram_MPORT_en & ram_MPORT_mask) begin
      ram[ram_MPORT_addr] <= ram_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if (reset) begin // @[Decoupled.scala 221:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 221:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 236:28]
      if (empty) begin // @[Decoupled.scala 246:18]
        if (io_deq_ready) begin // @[Decoupled.scala 249:27]
          maybe_full <= 1'h0; // @[Decoupled.scala 249:36]
        end else begin
          maybe_full <= _do_enq_T;
        end
      end else begin
        maybe_full <= _do_enq_T;
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
`ifdef RANDOMIZE_MEM_INIT
  _RAND_0 = {1{`RANDOM}};
  for (initvar = 0; initvar < 1; initvar = initvar+1)
    ram[initvar] = _RAND_0[0:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_1 = {1{`RANDOM}};
  maybe_full = _RAND_1[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Accumulator(
  input         clock,
  input         reset,
  output        io_input_ready,
  input         io_input_valid,
  input  [15:0] io_input_bits_0,
  input  [15:0] io_input_bits_1,
  input  [15:0] io_input_bits_2,
  input  [15:0] io_input_bits_3,
  input  [15:0] io_input_bits_4,
  input  [15:0] io_input_bits_5,
  input  [15:0] io_input_bits_6,
  input  [15:0] io_input_bits_7,
  input         io_output_ready,
  output        io_output_valid,
  output [15:0] io_output_bits_0,
  output [15:0] io_output_bits_1,
  output [15:0] io_output_bits_2,
  output [15:0] io_output_bits_3,
  output [15:0] io_output_bits_4,
  output [15:0] io_output_bits_5,
  output [15:0] io_output_bits_6,
  output [15:0] io_output_bits_7,
  output        io_control_ready,
  input         io_control_valid,
  input  [5:0]  io_control_bits_address,
  input         io_control_bits_accumulate,
  input         io_control_bits_write,
  input         io_tracepoint,
  input  [31:0] io_programCounter
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
`endif // RANDOMIZE_REG_INIT
  wire  mem_clock; // @[Accumulator.scala 38:19]
  wire  mem_reset; // @[Accumulator.scala 38:19]
  wire  mem_io_portA_control_ready; // @[Accumulator.scala 38:19]
  wire  mem_io_portA_control_valid; // @[Accumulator.scala 38:19]
  wire  mem_io_portA_control_bits_write; // @[Accumulator.scala 38:19]
  wire [5:0] mem_io_portA_control_bits_address; // @[Accumulator.scala 38:19]
  wire  mem_io_portA_input_ready; // @[Accumulator.scala 38:19]
  wire  mem_io_portA_input_valid; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portA_input_bits_0; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portA_input_bits_1; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portA_input_bits_2; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portA_input_bits_3; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portA_input_bits_4; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portA_input_bits_5; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portA_input_bits_6; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portA_input_bits_7; // @[Accumulator.scala 38:19]
  wire  mem_io_portA_output_ready; // @[Accumulator.scala 38:19]
  wire  mem_io_portA_output_valid; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portA_output_bits_0; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portA_output_bits_1; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portA_output_bits_2; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portA_output_bits_3; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portA_output_bits_4; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portA_output_bits_5; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portA_output_bits_6; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portA_output_bits_7; // @[Accumulator.scala 38:19]
  wire  mem_io_portB_control_ready; // @[Accumulator.scala 38:19]
  wire  mem_io_portB_control_valid; // @[Accumulator.scala 38:19]
  wire  mem_io_portB_control_bits_write; // @[Accumulator.scala 38:19]
  wire [5:0] mem_io_portB_control_bits_address; // @[Accumulator.scala 38:19]
  wire  mem_io_portB_input_ready; // @[Accumulator.scala 38:19]
  wire  mem_io_portB_input_valid; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portB_input_bits_0; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portB_input_bits_1; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portB_input_bits_2; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portB_input_bits_3; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portB_input_bits_4; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portB_input_bits_5; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portB_input_bits_6; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portB_input_bits_7; // @[Accumulator.scala 38:19]
  wire  mem_io_portB_output_ready; // @[Accumulator.scala 38:19]
  wire  mem_io_portB_output_valid; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portB_output_bits_0; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portB_output_bits_1; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portB_output_bits_2; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portB_output_bits_3; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portB_output_bits_4; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portB_output_bits_5; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portB_output_bits_6; // @[Accumulator.scala 38:19]
  wire [15:0] mem_io_portB_output_bits_7; // @[Accumulator.scala 38:19]
  wire  mem_io_tracepoint; // @[Accumulator.scala 38:19]
  wire [31:0] mem_io_programCounter; // @[Accumulator.scala 38:19]
  wire  adder_clock; // @[Accumulator.scala 46:23]
  wire  adder_reset; // @[Accumulator.scala 46:23]
  wire  adder_io_left_ready; // @[Accumulator.scala 46:23]
  wire  adder_io_left_valid; // @[Accumulator.scala 46:23]
  wire [15:0] adder_io_left_bits_0; // @[Accumulator.scala 46:23]
  wire [15:0] adder_io_left_bits_1; // @[Accumulator.scala 46:23]
  wire [15:0] adder_io_left_bits_2; // @[Accumulator.scala 46:23]
  wire [15:0] adder_io_left_bits_3; // @[Accumulator.scala 46:23]
  wire [15:0] adder_io_left_bits_4; // @[Accumulator.scala 46:23]
  wire [15:0] adder_io_left_bits_5; // @[Accumulator.scala 46:23]
  wire [15:0] adder_io_left_bits_6; // @[Accumulator.scala 46:23]
  wire [15:0] adder_io_left_bits_7; // @[Accumulator.scala 46:23]
  wire  adder_io_right_ready; // @[Accumulator.scala 46:23]
  wire  adder_io_right_valid; // @[Accumulator.scala 46:23]
  wire [15:0] adder_io_right_bits_0; // @[Accumulator.scala 46:23]
  wire [15:0] adder_io_right_bits_1; // @[Accumulator.scala 46:23]
  wire [15:0] adder_io_right_bits_2; // @[Accumulator.scala 46:23]
  wire [15:0] adder_io_right_bits_3; // @[Accumulator.scala 46:23]
  wire [15:0] adder_io_right_bits_4; // @[Accumulator.scala 46:23]
  wire [15:0] adder_io_right_bits_5; // @[Accumulator.scala 46:23]
  wire [15:0] adder_io_right_bits_6; // @[Accumulator.scala 46:23]
  wire [15:0] adder_io_right_bits_7; // @[Accumulator.scala 46:23]
  wire  adder_io_output_ready; // @[Accumulator.scala 46:23]
  wire  adder_io_output_valid; // @[Accumulator.scala 46:23]
  wire [15:0] adder_io_output_bits_0; // @[Accumulator.scala 46:23]
  wire [15:0] adder_io_output_bits_1; // @[Accumulator.scala 46:23]
  wire [15:0] adder_io_output_bits_2; // @[Accumulator.scala 46:23]
  wire [15:0] adder_io_output_bits_3; // @[Accumulator.scala 46:23]
  wire [15:0] adder_io_output_bits_4; // @[Accumulator.scala 46:23]
  wire [15:0] adder_io_output_bits_5; // @[Accumulator.scala 46:23]
  wire [15:0] adder_io_output_bits_6; // @[Accumulator.scala 46:23]
  wire [15:0] adder_io_output_bits_7; // @[Accumulator.scala 46:23]
  wire  control_clock; // @[Decoupled.scala 296:21]
  wire  control_reset; // @[Decoupled.scala 296:21]
  wire  control_io_enq_ready; // @[Decoupled.scala 296:21]
  wire  control_io_enq_valid; // @[Decoupled.scala 296:21]
  wire [5:0] control_io_enq_bits_address; // @[Decoupled.scala 296:21]
  wire  control_io_enq_bits_accumulate; // @[Decoupled.scala 296:21]
  wire  control_io_enq_bits_write; // @[Decoupled.scala 296:21]
  wire  control_io_deq_ready; // @[Decoupled.scala 296:21]
  wire  control_io_deq_valid; // @[Decoupled.scala 296:21]
  wire [5:0] control_io_deq_bits_address; // @[Decoupled.scala 296:21]
  wire  control_io_deq_bits_accumulate; // @[Decoupled.scala 296:21]
  wire  control_io_deq_bits_write; // @[Decoupled.scala 296:21]
  wire  input__clock; // @[Decoupled.scala 296:21]
  wire  input__reset; // @[Decoupled.scala 296:21]
  wire  input__io_enq_ready; // @[Decoupled.scala 296:21]
  wire  input__io_enq_valid; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_enq_bits_0; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_enq_bits_1; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_enq_bits_2; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_enq_bits_3; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_enq_bits_4; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_enq_bits_5; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_enq_bits_6; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_enq_bits_7; // @[Decoupled.scala 296:21]
  wire  input__io_deq_ready; // @[Decoupled.scala 296:21]
  wire  input__io_deq_valid; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_deq_bits_0; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_deq_bits_1; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_deq_bits_2; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_deq_bits_3; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_deq_bits_4; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_deq_bits_5; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_deq_bits_6; // @[Decoupled.scala 296:21]
  wire [15:0] input__io_deq_bits_7; // @[Decoupled.scala 296:21]
  wire  portBControl_clock; // @[Mem.scala 23:19]
  wire  portBControl_reset; // @[Mem.scala 23:19]
  wire  portBControl_io_enq_ready; // @[Mem.scala 23:19]
  wire  portBControl_io_enq_valid; // @[Mem.scala 23:19]
  wire  portBControl_io_enq_bits_write; // @[Mem.scala 23:19]
  wire [5:0] portBControl_io_enq_bits_address; // @[Mem.scala 23:19]
  wire  portBControl_io_deq_ready; // @[Mem.scala 23:19]
  wire  portBControl_io_deq_valid; // @[Mem.scala 23:19]
  wire  portBControl_io_deq_bits_write; // @[Mem.scala 23:19]
  wire [5:0] portBControl_io_deq_bits_address; // @[Mem.scala 23:19]
  wire  inputDemux_x14_demux_io_in_ready; // @[Demux.scala 46:23]
  wire  inputDemux_x14_demux_io_in_valid; // @[Demux.scala 46:23]
  wire [15:0] inputDemux_x14_demux_io_in_bits_0; // @[Demux.scala 46:23]
  wire [15:0] inputDemux_x14_demux_io_in_bits_1; // @[Demux.scala 46:23]
  wire [15:0] inputDemux_x14_demux_io_in_bits_2; // @[Demux.scala 46:23]
  wire [15:0] inputDemux_x14_demux_io_in_bits_3; // @[Demux.scala 46:23]
  wire [15:0] inputDemux_x14_demux_io_in_bits_4; // @[Demux.scala 46:23]
  wire [15:0] inputDemux_x14_demux_io_in_bits_5; // @[Demux.scala 46:23]
  wire [15:0] inputDemux_x14_demux_io_in_bits_6; // @[Demux.scala 46:23]
  wire [15:0] inputDemux_x14_demux_io_in_bits_7; // @[Demux.scala 46:23]
  wire  inputDemux_x14_demux_io_sel_ready; // @[Demux.scala 46:23]
  wire  inputDemux_x14_demux_io_sel_valid; // @[Demux.scala 46:23]
  wire  inputDemux_x14_demux_io_sel_bits; // @[Demux.scala 46:23]
  wire  inputDemux_x14_demux_io_out_0_ready; // @[Demux.scala 46:23]
  wire  inputDemux_x14_demux_io_out_0_valid; // @[Demux.scala 46:23]
  wire [15:0] inputDemux_x14_demux_io_out_0_bits_0; // @[Demux.scala 46:23]
  wire [15:0] inputDemux_x14_demux_io_out_0_bits_1; // @[Demux.scala 46:23]
  wire [15:0] inputDemux_x14_demux_io_out_0_bits_2; // @[Demux.scala 46:23]
  wire [15:0] inputDemux_x14_demux_io_out_0_bits_3; // @[Demux.scala 46:23]
  wire [15:0] inputDemux_x14_demux_io_out_0_bits_4; // @[Demux.scala 46:23]
  wire [15:0] inputDemux_x14_demux_io_out_0_bits_5; // @[Demux.scala 46:23]
  wire [15:0] inputDemux_x14_demux_io_out_0_bits_6; // @[Demux.scala 46:23]
  wire [15:0] inputDemux_x14_demux_io_out_0_bits_7; // @[Demux.scala 46:23]
  wire  inputDemux_x14_demux_io_out_1_ready; // @[Demux.scala 46:23]
  wire  inputDemux_x14_demux_io_out_1_valid; // @[Demux.scala 46:23]
  wire [15:0] inputDemux_x14_demux_io_out_1_bits_0; // @[Demux.scala 46:23]
  wire [15:0] inputDemux_x14_demux_io_out_1_bits_1; // @[Demux.scala 46:23]
  wire [15:0] inputDemux_x14_demux_io_out_1_bits_2; // @[Demux.scala 46:23]
  wire [15:0] inputDemux_x14_demux_io_out_1_bits_3; // @[Demux.scala 46:23]
  wire [15:0] inputDemux_x14_demux_io_out_1_bits_4; // @[Demux.scala 46:23]
  wire [15:0] inputDemux_x14_demux_io_out_1_bits_5; // @[Demux.scala 46:23]
  wire [15:0] inputDemux_x14_demux_io_out_1_bits_6; // @[Demux.scala 46:23]
  wire [15:0] inputDemux_x14_demux_io_out_1_bits_7; // @[Demux.scala 46:23]
  wire  inputDemux_clock; // @[Mem.scala 23:19]
  wire  inputDemux_reset; // @[Mem.scala 23:19]
  wire  inputDemux_io_enq_ready; // @[Mem.scala 23:19]
  wire  inputDemux_io_enq_valid; // @[Mem.scala 23:19]
  wire  inputDemux_io_enq_bits; // @[Mem.scala 23:19]
  wire  inputDemux_io_deq_ready; // @[Mem.scala 23:19]
  wire  inputDemux_io_deq_valid; // @[Mem.scala 23:19]
  wire  inputDemux_io_deq_bits; // @[Mem.scala 23:19]
  wire  memOutputDemux_x23_demux_io_in_ready; // @[Demux.scala 46:23]
  wire  memOutputDemux_x23_demux_io_in_valid; // @[Demux.scala 46:23]
  wire [15:0] memOutputDemux_x23_demux_io_in_bits_0; // @[Demux.scala 46:23]
  wire [15:0] memOutputDemux_x23_demux_io_in_bits_1; // @[Demux.scala 46:23]
  wire [15:0] memOutputDemux_x23_demux_io_in_bits_2; // @[Demux.scala 46:23]
  wire [15:0] memOutputDemux_x23_demux_io_in_bits_3; // @[Demux.scala 46:23]
  wire [15:0] memOutputDemux_x23_demux_io_in_bits_4; // @[Demux.scala 46:23]
  wire [15:0] memOutputDemux_x23_demux_io_in_bits_5; // @[Demux.scala 46:23]
  wire [15:0] memOutputDemux_x23_demux_io_in_bits_6; // @[Demux.scala 46:23]
  wire [15:0] memOutputDemux_x23_demux_io_in_bits_7; // @[Demux.scala 46:23]
  wire  memOutputDemux_x23_demux_io_sel_ready; // @[Demux.scala 46:23]
  wire  memOutputDemux_x23_demux_io_sel_valid; // @[Demux.scala 46:23]
  wire  memOutputDemux_x23_demux_io_sel_bits; // @[Demux.scala 46:23]
  wire  memOutputDemux_x23_demux_io_out_0_ready; // @[Demux.scala 46:23]
  wire  memOutputDemux_x23_demux_io_out_0_valid; // @[Demux.scala 46:23]
  wire [15:0] memOutputDemux_x23_demux_io_out_0_bits_0; // @[Demux.scala 46:23]
  wire [15:0] memOutputDemux_x23_demux_io_out_0_bits_1; // @[Demux.scala 46:23]
  wire [15:0] memOutputDemux_x23_demux_io_out_0_bits_2; // @[Demux.scala 46:23]
  wire [15:0] memOutputDemux_x23_demux_io_out_0_bits_3; // @[Demux.scala 46:23]
  wire [15:0] memOutputDemux_x23_demux_io_out_0_bits_4; // @[Demux.scala 46:23]
  wire [15:0] memOutputDemux_x23_demux_io_out_0_bits_5; // @[Demux.scala 46:23]
  wire [15:0] memOutputDemux_x23_demux_io_out_0_bits_6; // @[Demux.scala 46:23]
  wire [15:0] memOutputDemux_x23_demux_io_out_0_bits_7; // @[Demux.scala 46:23]
  wire  memOutputDemux_x23_demux_io_out_1_ready; // @[Demux.scala 46:23]
  wire  memOutputDemux_x23_demux_io_out_1_valid; // @[Demux.scala 46:23]
  wire [15:0] memOutputDemux_x23_demux_io_out_1_bits_0; // @[Demux.scala 46:23]
  wire [15:0] memOutputDemux_x23_demux_io_out_1_bits_1; // @[Demux.scala 46:23]
  wire [15:0] memOutputDemux_x23_demux_io_out_1_bits_2; // @[Demux.scala 46:23]
  wire [15:0] memOutputDemux_x23_demux_io_out_1_bits_3; // @[Demux.scala 46:23]
  wire [15:0] memOutputDemux_x23_demux_io_out_1_bits_4; // @[Demux.scala 46:23]
  wire [15:0] memOutputDemux_x23_demux_io_out_1_bits_5; // @[Demux.scala 46:23]
  wire [15:0] memOutputDemux_x23_demux_io_out_1_bits_6; // @[Demux.scala 46:23]
  wire [15:0] memOutputDemux_x23_demux_io_out_1_bits_7; // @[Demux.scala 46:23]
  wire  memOutputDemux_clock; // @[Mem.scala 23:19]
  wire  memOutputDemux_reset; // @[Mem.scala 23:19]
  wire  memOutputDemux_io_enq_ready; // @[Mem.scala 23:19]
  wire  memOutputDemux_io_enq_valid; // @[Mem.scala 23:19]
  wire  memOutputDemux_io_enq_bits; // @[Mem.scala 23:19]
  wire  memOutputDemux_io_deq_ready; // @[Mem.scala 23:19]
  wire  memOutputDemux_io_deq_valid; // @[Mem.scala 23:19]
  wire  memOutputDemux_io_deq_bits; // @[Mem.scala 23:19]
  wire  writeEnqueuer_clock; // @[MultiEnqueue.scala 160:43]
  wire  writeEnqueuer_reset; // @[MultiEnqueue.scala 160:43]
  wire  writeEnqueuer_io_in_ready; // @[MultiEnqueue.scala 160:43]
  wire  writeEnqueuer_io_in_valid; // @[MultiEnqueue.scala 160:43]
  wire  writeEnqueuer_io_out_0_ready; // @[MultiEnqueue.scala 160:43]
  wire  writeEnqueuer_io_out_0_valid; // @[MultiEnqueue.scala 160:43]
  wire  writeEnqueuer_io_out_1_ready; // @[MultiEnqueue.scala 160:43]
  wire  writeEnqueuer_io_out_1_valid; // @[MultiEnqueue.scala 160:43]
  wire  readEnqueuer_clock; // @[MultiEnqueue.scala 160:43]
  wire  readEnqueuer_reset; // @[MultiEnqueue.scala 160:43]
  wire  readEnqueuer_io_in_ready; // @[MultiEnqueue.scala 160:43]
  wire  readEnqueuer_io_in_valid; // @[MultiEnqueue.scala 160:43]
  wire  readEnqueuer_io_out_0_ready; // @[MultiEnqueue.scala 160:43]
  wire  readEnqueuer_io_out_0_valid; // @[MultiEnqueue.scala 160:43]
  wire  readEnqueuer_io_out_1_ready; // @[MultiEnqueue.scala 160:43]
  wire  readEnqueuer_io_out_1_valid; // @[MultiEnqueue.scala 160:43]
  wire  accEnqueuer_clock; // @[MultiEnqueue.scala 160:43]
  wire  accEnqueuer_reset; // @[MultiEnqueue.scala 160:43]
  wire  accEnqueuer_io_in_ready; // @[MultiEnqueue.scala 160:43]
  wire  accEnqueuer_io_in_valid; // @[MultiEnqueue.scala 160:43]
  wire  accEnqueuer_io_out_0_ready; // @[MultiEnqueue.scala 160:43]
  wire  accEnqueuer_io_out_0_valid; // @[MultiEnqueue.scala 160:43]
  wire  accEnqueuer_io_out_1_ready; // @[MultiEnqueue.scala 160:43]
  wire  accEnqueuer_io_out_1_valid; // @[MultiEnqueue.scala 160:43]
  wire  accEnqueuer_io_out_2_ready; // @[MultiEnqueue.scala 160:43]
  wire  accEnqueuer_io_out_2_valid; // @[MultiEnqueue.scala 160:43]
  wire  accEnqueuer_io_out_3_ready; // @[MultiEnqueue.scala 160:43]
  wire  accEnqueuer_io_out_3_valid; // @[MultiEnqueue.scala 160:43]
  reg  writeAccumulating; // @[Accumulator.scala 105:34]
  wire  _GEN_0 = control_io_deq_bits_accumulate; // @[Accumulator.scala 109:35 Accumulator.scala 110:25 Accumulator.scala 106:21]
  wire  _GEN_1 = control_io_deq_bits_accumulate & control_io_deq_valid; // @[Accumulator.scala 109:35 MultiEnqueue.scala 150:17 MultiEnqueue.scala 40:17]
  wire  control_io_deq_ready_mem_io_portA_control_w_ready = mem_io_portA_control_ready; // @[ReadyValid.scala 16:17 MultiEnqueue.scala 151:10]
  wire  _GEN_2 = control_io_deq_bits_accumulate & control_io_deq_ready_mem_io_portA_control_w_ready; // @[Accumulator.scala 109:35 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire [5:0] control_io_deq_ready_w_address = control_io_deq_bits_address; // @[MemControl.scala 76:19 MemControl.scala 77:17]
  wire [5:0] _GEN_4 = control_io_deq_bits_accumulate ? control_io_deq_ready_w_address : control_io_deq_ready_w_address; // @[Accumulator.scala 109:35 MultiEnqueue.scala 151:10 MultiEnqueue.scala 85:10]
  wire  _GEN_5 = control_io_deq_bits_accumulate ? 1'h0 : 1'h1; // @[Accumulator.scala 109:35 MultiEnqueue.scala 151:10 MultiEnqueue.scala 85:10]
  wire  control_io_deq_ready_mem_io_portA_control_w_valid = accEnqueuer_io_out_0_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  control_io_deq_ready_mem_io_portA_control_w_1_valid = writeEnqueuer_io_out_0_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_6 = control_io_deq_bits_accumulate ? control_io_deq_ready_mem_io_portA_control_w_valid :
    control_io_deq_ready_mem_io_portA_control_w_1_valid; // @[Accumulator.scala 109:35 MultiEnqueue.scala 151:10 MultiEnqueue.scala 85:10]
  wire  control_io_deq_ready_memOutputDemux_io_enq_w_ready = memOutputDemux_io_enq_ready; // @[ReadyValid.scala 16:17 MultiEnqueue.scala 152:10]
  wire  _GEN_7 = control_io_deq_bits_accumulate & control_io_deq_ready_memOutputDemux_io_enq_w_ready; // @[Accumulator.scala 109:35 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire  control_io_deq_ready_memOutputDemux_io_enq_w_valid = accEnqueuer_io_out_1_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_9 = control_io_deq_bits_accumulate & control_io_deq_ready_memOutputDemux_io_enq_w_valid; // @[Accumulator.scala 109:35 MultiEnqueue.scala 152:10 package.scala 410:15]
  wire  control_io_deq_ready_portBControl_io_enq_w_ready = portBControl_io_enq_ready; // @[ReadyValid.scala 16:17 MultiEnqueue.scala 153:10]
  wire  _GEN_10 = control_io_deq_bits_accumulate & control_io_deq_ready_portBControl_io_enq_w_ready; // @[Accumulator.scala 109:35 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire [5:0] _GEN_12 = control_io_deq_bits_accumulate ? control_io_deq_ready_w_address : 6'h0; // @[Accumulator.scala 109:35 MultiEnqueue.scala 153:10 package.scala 409:14]
  wire  control_io_deq_ready_portBControl_io_enq_w_valid = accEnqueuer_io_out_2_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_14 = control_io_deq_bits_accumulate & control_io_deq_ready_portBControl_io_enq_w_valid; // @[Accumulator.scala 109:35 MultiEnqueue.scala 153:10 package.scala 410:15]
  wire  control_io_deq_ready_inputDemux_io_enq_w_ready = inputDemux_io_enq_ready; // @[ReadyValid.scala 16:17 MultiEnqueue.scala 154:10]
  wire  _GEN_15 = control_io_deq_bits_accumulate & control_io_deq_ready_inputDemux_io_enq_w_ready; // @[Accumulator.scala 109:35 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire  control_io_deq_ready_inputDemux_io_enq_w_valid = accEnqueuer_io_out_3_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  control_io_deq_ready_inputDemux_io_enq_w_1_valid = writeEnqueuer_io_out_1_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_17 = control_io_deq_bits_accumulate ? control_io_deq_ready_inputDemux_io_enq_w_valid :
    control_io_deq_ready_inputDemux_io_enq_w_1_valid; // @[Accumulator.scala 109:35 MultiEnqueue.scala 154:10 MultiEnqueue.scala 86:10]
  wire  _GEN_18 = control_io_deq_bits_accumulate ? accEnqueuer_io_in_ready : writeEnqueuer_io_in_ready; // @[Accumulator.scala 109:35 Accumulator.scala 111:21 Accumulator.scala 126:21]
  wire  _GEN_19 = control_io_deq_bits_accumulate ? 1'h0 : control_io_deq_valid; // @[Accumulator.scala 109:35 MultiEnqueue.scala 40:17 MultiEnqueue.scala 84:17]
  wire  _GEN_20 = control_io_deq_bits_accumulate ? 1'h0 : control_io_deq_ready_mem_io_portA_control_w_ready; // @[Accumulator.scala 109:35 MultiEnqueue.scala 42:18 ReadyValid.scala 19:11]
  wire  _GEN_21 = control_io_deq_bits_accumulate ? 1'h0 : control_io_deq_ready_inputDemux_io_enq_w_ready; // @[Accumulator.scala 109:35 MultiEnqueue.scala 42:18 ReadyValid.scala 19:11]
  wire  _GEN_22 = ~writeAccumulating & control_io_deq_valid; // @[Accumulator.scala 136:30 MultiEnqueue.scala 84:17 MultiEnqueue.scala 40:17]
  wire  _GEN_23 = ~writeAccumulating & control_io_deq_ready_mem_io_portA_control_w_ready; // @[Accumulator.scala 136:30 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire [5:0] _GEN_25 = ~writeAccumulating ? control_io_deq_ready_w_address : 6'h0; // @[Accumulator.scala 136:30 MultiEnqueue.scala 85:10 package.scala 409:14]
  wire  control_io_deq_ready_mem_io_portA_control_w_2_valid = readEnqueuer_io_out_0_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_27 = ~writeAccumulating & control_io_deq_ready_mem_io_portA_control_w_2_valid; // @[Accumulator.scala 136:30 MultiEnqueue.scala 85:10 package.scala 410:15]
  wire  _GEN_28 = ~writeAccumulating & control_io_deq_ready_memOutputDemux_io_enq_w_ready; // @[Accumulator.scala 136:30 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire  control_io_deq_ready_memOutputDemux_io_enq_w_1_valid = readEnqueuer_io_out_1_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_30 = ~writeAccumulating & control_io_deq_ready_memOutputDemux_io_enq_w_1_valid; // @[Accumulator.scala 136:30 MultiEnqueue.scala 86:10 package.scala 410:15]
  wire  _GEN_31 = ~writeAccumulating & readEnqueuer_io_in_ready; // @[Accumulator.scala 136:30 Accumulator.scala 137:21 Accumulator.scala 146:21]
  wire  _GEN_32 = control_io_deq_bits_write & _GEN_0; // @[Accumulator.scala 108:28 Accumulator.scala 106:21]
  DualPortMem mem ( // @[Accumulator.scala 38:19]
    .clock(mem_clock),
    .reset(mem_reset),
    .io_portA_control_ready(mem_io_portA_control_ready),
    .io_portA_control_valid(mem_io_portA_control_valid),
    .io_portA_control_bits_write(mem_io_portA_control_bits_write),
    .io_portA_control_bits_address(mem_io_portA_control_bits_address),
    .io_portA_input_ready(mem_io_portA_input_ready),
    .io_portA_input_valid(mem_io_portA_input_valid),
    .io_portA_input_bits_0(mem_io_portA_input_bits_0),
    .io_portA_input_bits_1(mem_io_portA_input_bits_1),
    .io_portA_input_bits_2(mem_io_portA_input_bits_2),
    .io_portA_input_bits_3(mem_io_portA_input_bits_3),
    .io_portA_input_bits_4(mem_io_portA_input_bits_4),
    .io_portA_input_bits_5(mem_io_portA_input_bits_5),
    .io_portA_input_bits_6(mem_io_portA_input_bits_6),
    .io_portA_input_bits_7(mem_io_portA_input_bits_7),
    .io_portA_output_ready(mem_io_portA_output_ready),
    .io_portA_output_valid(mem_io_portA_output_valid),
    .io_portA_output_bits_0(mem_io_portA_output_bits_0),
    .io_portA_output_bits_1(mem_io_portA_output_bits_1),
    .io_portA_output_bits_2(mem_io_portA_output_bits_2),
    .io_portA_output_bits_3(mem_io_portA_output_bits_3),
    .io_portA_output_bits_4(mem_io_portA_output_bits_4),
    .io_portA_output_bits_5(mem_io_portA_output_bits_5),
    .io_portA_output_bits_6(mem_io_portA_output_bits_6),
    .io_portA_output_bits_7(mem_io_portA_output_bits_7),
    .io_portB_control_ready(mem_io_portB_control_ready),
    .io_portB_control_valid(mem_io_portB_control_valid),
    .io_portB_control_bits_write(mem_io_portB_control_bits_write),
    .io_portB_control_bits_address(mem_io_portB_control_bits_address),
    .io_portB_input_ready(mem_io_portB_input_ready),
    .io_portB_input_valid(mem_io_portB_input_valid),
    .io_portB_input_bits_0(mem_io_portB_input_bits_0),
    .io_portB_input_bits_1(mem_io_portB_input_bits_1),
    .io_portB_input_bits_2(mem_io_portB_input_bits_2),
    .io_portB_input_bits_3(mem_io_portB_input_bits_3),
    .io_portB_input_bits_4(mem_io_portB_input_bits_4),
    .io_portB_input_bits_5(mem_io_portB_input_bits_5),
    .io_portB_input_bits_6(mem_io_portB_input_bits_6),
    .io_portB_input_bits_7(mem_io_portB_input_bits_7),
    .io_portB_output_ready(mem_io_portB_output_ready),
    .io_portB_output_valid(mem_io_portB_output_valid),
    .io_portB_output_bits_0(mem_io_portB_output_bits_0),
    .io_portB_output_bits_1(mem_io_portB_output_bits_1),
    .io_portB_output_bits_2(mem_io_portB_output_bits_2),
    .io_portB_output_bits_3(mem_io_portB_output_bits_3),
    .io_portB_output_bits_4(mem_io_portB_output_bits_4),
    .io_portB_output_bits_5(mem_io_portB_output_bits_5),
    .io_portB_output_bits_6(mem_io_portB_output_bits_6),
    .io_portB_output_bits_7(mem_io_portB_output_bits_7),
    .io_tracepoint(mem_io_tracepoint),
    .io_programCounter(mem_io_programCounter)
  );
  VecAdder adder ( // @[Accumulator.scala 46:23]
    .clock(adder_clock),
    .reset(adder_reset),
    .io_left_ready(adder_io_left_ready),
    .io_left_valid(adder_io_left_valid),
    .io_left_bits_0(adder_io_left_bits_0),
    .io_left_bits_1(adder_io_left_bits_1),
    .io_left_bits_2(adder_io_left_bits_2),
    .io_left_bits_3(adder_io_left_bits_3),
    .io_left_bits_4(adder_io_left_bits_4),
    .io_left_bits_5(adder_io_left_bits_5),
    .io_left_bits_6(adder_io_left_bits_6),
    .io_left_bits_7(adder_io_left_bits_7),
    .io_right_ready(adder_io_right_ready),
    .io_right_valid(adder_io_right_valid),
    .io_right_bits_0(adder_io_right_bits_0),
    .io_right_bits_1(adder_io_right_bits_1),
    .io_right_bits_2(adder_io_right_bits_2),
    .io_right_bits_3(adder_io_right_bits_3),
    .io_right_bits_4(adder_io_right_bits_4),
    .io_right_bits_5(adder_io_right_bits_5),
    .io_right_bits_6(adder_io_right_bits_6),
    .io_right_bits_7(adder_io_right_bits_7),
    .io_output_ready(adder_io_output_ready),
    .io_output_valid(adder_io_output_valid),
    .io_output_bits_0(adder_io_output_bits_0),
    .io_output_bits_1(adder_io_output_bits_1),
    .io_output_bits_2(adder_io_output_bits_2),
    .io_output_bits_3(adder_io_output_bits_3),
    .io_output_bits_4(adder_io_output_bits_4),
    .io_output_bits_5(adder_io_output_bits_5),
    .io_output_bits_6(adder_io_output_bits_6),
    .io_output_bits_7(adder_io_output_bits_7)
  );
  Queue_13 control ( // @[Decoupled.scala 296:21]
    .clock(control_clock),
    .reset(control_reset),
    .io_enq_ready(control_io_enq_ready),
    .io_enq_valid(control_io_enq_valid),
    .io_enq_bits_address(control_io_enq_bits_address),
    .io_enq_bits_accumulate(control_io_enq_bits_accumulate),
    .io_enq_bits_write(control_io_enq_bits_write),
    .io_deq_ready(control_io_deq_ready),
    .io_deq_valid(control_io_deq_valid),
    .io_deq_bits_address(control_io_deq_bits_address),
    .io_deq_bits_accumulate(control_io_deq_bits_accumulate),
    .io_deq_bits_write(control_io_deq_bits_write)
  );
  Queue_4 input_ ( // @[Decoupled.scala 296:21]
    .clock(input__clock),
    .reset(input__reset),
    .io_enq_ready(input__io_enq_ready),
    .io_enq_valid(input__io_enq_valid),
    .io_enq_bits_0(input__io_enq_bits_0),
    .io_enq_bits_1(input__io_enq_bits_1),
    .io_enq_bits_2(input__io_enq_bits_2),
    .io_enq_bits_3(input__io_enq_bits_3),
    .io_enq_bits_4(input__io_enq_bits_4),
    .io_enq_bits_5(input__io_enq_bits_5),
    .io_enq_bits_6(input__io_enq_bits_6),
    .io_enq_bits_7(input__io_enq_bits_7),
    .io_deq_ready(input__io_deq_ready),
    .io_deq_valid(input__io_deq_valid),
    .io_deq_bits_0(input__io_deq_bits_0),
    .io_deq_bits_1(input__io_deq_bits_1),
    .io_deq_bits_2(input__io_deq_bits_2),
    .io_deq_bits_3(input__io_deq_bits_3),
    .io_deq_bits_4(input__io_deq_bits_4),
    .io_deq_bits_5(input__io_deq_bits_5),
    .io_deq_bits_6(input__io_deq_bits_6),
    .io_deq_bits_7(input__io_deq_bits_7)
  );
  Queue_15 portBControl ( // @[Mem.scala 23:19]
    .clock(portBControl_clock),
    .reset(portBControl_reset),
    .io_enq_ready(portBControl_io_enq_ready),
    .io_enq_valid(portBControl_io_enq_valid),
    .io_enq_bits_write(portBControl_io_enq_bits_write),
    .io_enq_bits_address(portBControl_io_enq_bits_address),
    .io_deq_ready(portBControl_io_deq_ready),
    .io_deq_valid(portBControl_io_deq_valid),
    .io_deq_bits_write(portBControl_io_deq_bits_write),
    .io_deq_bits_address(portBControl_io_deq_bits_address)
  );
  Demux inputDemux_x14_demux ( // @[Demux.scala 46:23]
    .io_in_ready(inputDemux_x14_demux_io_in_ready),
    .io_in_valid(inputDemux_x14_demux_io_in_valid),
    .io_in_bits_0(inputDemux_x14_demux_io_in_bits_0),
    .io_in_bits_1(inputDemux_x14_demux_io_in_bits_1),
    .io_in_bits_2(inputDemux_x14_demux_io_in_bits_2),
    .io_in_bits_3(inputDemux_x14_demux_io_in_bits_3),
    .io_in_bits_4(inputDemux_x14_demux_io_in_bits_4),
    .io_in_bits_5(inputDemux_x14_demux_io_in_bits_5),
    .io_in_bits_6(inputDemux_x14_demux_io_in_bits_6),
    .io_in_bits_7(inputDemux_x14_demux_io_in_bits_7),
    .io_sel_ready(inputDemux_x14_demux_io_sel_ready),
    .io_sel_valid(inputDemux_x14_demux_io_sel_valid),
    .io_sel_bits(inputDemux_x14_demux_io_sel_bits),
    .io_out_0_ready(inputDemux_x14_demux_io_out_0_ready),
    .io_out_0_valid(inputDemux_x14_demux_io_out_0_valid),
    .io_out_0_bits_0(inputDemux_x14_demux_io_out_0_bits_0),
    .io_out_0_bits_1(inputDemux_x14_demux_io_out_0_bits_1),
    .io_out_0_bits_2(inputDemux_x14_demux_io_out_0_bits_2),
    .io_out_0_bits_3(inputDemux_x14_demux_io_out_0_bits_3),
    .io_out_0_bits_4(inputDemux_x14_demux_io_out_0_bits_4),
    .io_out_0_bits_5(inputDemux_x14_demux_io_out_0_bits_5),
    .io_out_0_bits_6(inputDemux_x14_demux_io_out_0_bits_6),
    .io_out_0_bits_7(inputDemux_x14_demux_io_out_0_bits_7),
    .io_out_1_ready(inputDemux_x14_demux_io_out_1_ready),
    .io_out_1_valid(inputDemux_x14_demux_io_out_1_valid),
    .io_out_1_bits_0(inputDemux_x14_demux_io_out_1_bits_0),
    .io_out_1_bits_1(inputDemux_x14_demux_io_out_1_bits_1),
    .io_out_1_bits_2(inputDemux_x14_demux_io_out_1_bits_2),
    .io_out_1_bits_3(inputDemux_x14_demux_io_out_1_bits_3),
    .io_out_1_bits_4(inputDemux_x14_demux_io_out_1_bits_4),
    .io_out_1_bits_5(inputDemux_x14_demux_io_out_1_bits_5),
    .io_out_1_bits_6(inputDemux_x14_demux_io_out_1_bits_6),
    .io_out_1_bits_7(inputDemux_x14_demux_io_out_1_bits_7)
  );
  Queue_16 inputDemux ( // @[Mem.scala 23:19]
    .clock(inputDemux_clock),
    .reset(inputDemux_reset),
    .io_enq_ready(inputDemux_io_enq_ready),
    .io_enq_valid(inputDemux_io_enq_valid),
    .io_enq_bits(inputDemux_io_enq_bits),
    .io_deq_ready(inputDemux_io_deq_ready),
    .io_deq_valid(inputDemux_io_deq_valid),
    .io_deq_bits(inputDemux_io_deq_bits)
  );
  Demux memOutputDemux_x23_demux ( // @[Demux.scala 46:23]
    .io_in_ready(memOutputDemux_x23_demux_io_in_ready),
    .io_in_valid(memOutputDemux_x23_demux_io_in_valid),
    .io_in_bits_0(memOutputDemux_x23_demux_io_in_bits_0),
    .io_in_bits_1(memOutputDemux_x23_demux_io_in_bits_1),
    .io_in_bits_2(memOutputDemux_x23_demux_io_in_bits_2),
    .io_in_bits_3(memOutputDemux_x23_demux_io_in_bits_3),
    .io_in_bits_4(memOutputDemux_x23_demux_io_in_bits_4),
    .io_in_bits_5(memOutputDemux_x23_demux_io_in_bits_5),
    .io_in_bits_6(memOutputDemux_x23_demux_io_in_bits_6),
    .io_in_bits_7(memOutputDemux_x23_demux_io_in_bits_7),
    .io_sel_ready(memOutputDemux_x23_demux_io_sel_ready),
    .io_sel_valid(memOutputDemux_x23_demux_io_sel_valid),
    .io_sel_bits(memOutputDemux_x23_demux_io_sel_bits),
    .io_out_0_ready(memOutputDemux_x23_demux_io_out_0_ready),
    .io_out_0_valid(memOutputDemux_x23_demux_io_out_0_valid),
    .io_out_0_bits_0(memOutputDemux_x23_demux_io_out_0_bits_0),
    .io_out_0_bits_1(memOutputDemux_x23_demux_io_out_0_bits_1),
    .io_out_0_bits_2(memOutputDemux_x23_demux_io_out_0_bits_2),
    .io_out_0_bits_3(memOutputDemux_x23_demux_io_out_0_bits_3),
    .io_out_0_bits_4(memOutputDemux_x23_demux_io_out_0_bits_4),
    .io_out_0_bits_5(memOutputDemux_x23_demux_io_out_0_bits_5),
    .io_out_0_bits_6(memOutputDemux_x23_demux_io_out_0_bits_6),
    .io_out_0_bits_7(memOutputDemux_x23_demux_io_out_0_bits_7),
    .io_out_1_ready(memOutputDemux_x23_demux_io_out_1_ready),
    .io_out_1_valid(memOutputDemux_x23_demux_io_out_1_valid),
    .io_out_1_bits_0(memOutputDemux_x23_demux_io_out_1_bits_0),
    .io_out_1_bits_1(memOutputDemux_x23_demux_io_out_1_bits_1),
    .io_out_1_bits_2(memOutputDemux_x23_demux_io_out_1_bits_2),
    .io_out_1_bits_3(memOutputDemux_x23_demux_io_out_1_bits_3),
    .io_out_1_bits_4(memOutputDemux_x23_demux_io_out_1_bits_4),
    .io_out_1_bits_5(memOutputDemux_x23_demux_io_out_1_bits_5),
    .io_out_1_bits_6(memOutputDemux_x23_demux_io_out_1_bits_6),
    .io_out_1_bits_7(memOutputDemux_x23_demux_io_out_1_bits_7)
  );
  Queue_16 memOutputDemux ( // @[Mem.scala 23:19]
    .clock(memOutputDemux_clock),
    .reset(memOutputDemux_reset),
    .io_enq_ready(memOutputDemux_io_enq_ready),
    .io_enq_valid(memOutputDemux_io_enq_valid),
    .io_enq_bits(memOutputDemux_io_enq_bits),
    .io_deq_ready(memOutputDemux_io_deq_ready),
    .io_deq_valid(memOutputDemux_io_deq_valid),
    .io_deq_bits(memOutputDemux_io_deq_bits)
  );
  MultiEnqueue_1 writeEnqueuer ( // @[MultiEnqueue.scala 160:43]
    .clock(writeEnqueuer_clock),
    .reset(writeEnqueuer_reset),
    .io_in_ready(writeEnqueuer_io_in_ready),
    .io_in_valid(writeEnqueuer_io_in_valid),
    .io_out_0_ready(writeEnqueuer_io_out_0_ready),
    .io_out_0_valid(writeEnqueuer_io_out_0_valid),
    .io_out_1_ready(writeEnqueuer_io_out_1_ready),
    .io_out_1_valid(writeEnqueuer_io_out_1_valid)
  );
  MultiEnqueue_1 readEnqueuer ( // @[MultiEnqueue.scala 160:43]
    .clock(readEnqueuer_clock),
    .reset(readEnqueuer_reset),
    .io_in_ready(readEnqueuer_io_in_ready),
    .io_in_valid(readEnqueuer_io_in_valid),
    .io_out_0_ready(readEnqueuer_io_out_0_ready),
    .io_out_0_valid(readEnqueuer_io_out_0_valid),
    .io_out_1_ready(readEnqueuer_io_out_1_ready),
    .io_out_1_valid(readEnqueuer_io_out_1_valid)
  );
  MultiEnqueue_3 accEnqueuer ( // @[MultiEnqueue.scala 160:43]
    .clock(accEnqueuer_clock),
    .reset(accEnqueuer_reset),
    .io_in_ready(accEnqueuer_io_in_ready),
    .io_in_valid(accEnqueuer_io_in_valid),
    .io_out_0_ready(accEnqueuer_io_out_0_ready),
    .io_out_0_valid(accEnqueuer_io_out_0_valid),
    .io_out_1_ready(accEnqueuer_io_out_1_ready),
    .io_out_1_valid(accEnqueuer_io_out_1_valid),
    .io_out_2_ready(accEnqueuer_io_out_2_ready),
    .io_out_2_valid(accEnqueuer_io_out_2_valid),
    .io_out_3_ready(accEnqueuer_io_out_3_ready),
    .io_out_3_valid(accEnqueuer_io_out_3_valid)
  );
  assign io_input_ready = input__io_enq_ready; // @[Decoupled.scala 299:17]
  assign io_output_valid = memOutputDemux_x23_demux_io_out_0_valid; // @[Demux.scala 55:10]
  assign io_output_bits_0 = memOutputDemux_x23_demux_io_out_0_bits_0; // @[Demux.scala 55:10]
  assign io_output_bits_1 = memOutputDemux_x23_demux_io_out_0_bits_1; // @[Demux.scala 55:10]
  assign io_output_bits_2 = memOutputDemux_x23_demux_io_out_0_bits_2; // @[Demux.scala 55:10]
  assign io_output_bits_3 = memOutputDemux_x23_demux_io_out_0_bits_3; // @[Demux.scala 55:10]
  assign io_output_bits_4 = memOutputDemux_x23_demux_io_out_0_bits_4; // @[Demux.scala 55:10]
  assign io_output_bits_5 = memOutputDemux_x23_demux_io_out_0_bits_5; // @[Demux.scala 55:10]
  assign io_output_bits_6 = memOutputDemux_x23_demux_io_out_0_bits_6; // @[Demux.scala 55:10]
  assign io_output_bits_7 = memOutputDemux_x23_demux_io_out_0_bits_7; // @[Demux.scala 55:10]
  assign io_control_ready = control_io_enq_ready; // @[Decoupled.scala 299:17]
  assign mem_clock = clock;
  assign mem_reset = reset;
  assign mem_io_portA_control_valid = control_io_deq_bits_write ? _GEN_6 : _GEN_27; // @[Accumulator.scala 108:28]
  assign mem_io_portA_control_bits_write = control_io_deq_bits_write & _GEN_5; // @[Accumulator.scala 108:28]
  assign mem_io_portA_control_bits_address = control_io_deq_bits_write ? _GEN_4 : _GEN_25; // @[Accumulator.scala 108:28]
  assign mem_io_portA_input_valid = inputDemux_x14_demux_io_out_0_valid; // @[Demux.scala 55:10]
  assign mem_io_portA_input_bits_0 = inputDemux_x14_demux_io_out_0_bits_0; // @[Demux.scala 55:10]
  assign mem_io_portA_input_bits_1 = inputDemux_x14_demux_io_out_0_bits_1; // @[Demux.scala 55:10]
  assign mem_io_portA_input_bits_2 = inputDemux_x14_demux_io_out_0_bits_2; // @[Demux.scala 55:10]
  assign mem_io_portA_input_bits_3 = inputDemux_x14_demux_io_out_0_bits_3; // @[Demux.scala 55:10]
  assign mem_io_portA_input_bits_4 = inputDemux_x14_demux_io_out_0_bits_4; // @[Demux.scala 55:10]
  assign mem_io_portA_input_bits_5 = inputDemux_x14_demux_io_out_0_bits_5; // @[Demux.scala 55:10]
  assign mem_io_portA_input_bits_6 = inputDemux_x14_demux_io_out_0_bits_6; // @[Demux.scala 55:10]
  assign mem_io_portA_input_bits_7 = inputDemux_x14_demux_io_out_0_bits_7; // @[Demux.scala 55:10]
  assign mem_io_portA_output_ready = memOutputDemux_x23_demux_io_in_ready; // @[Demux.scala 54:17]
  assign mem_io_portB_control_valid = portBControl_io_deq_valid; // @[Mem.scala 24:7]
  assign mem_io_portB_control_bits_write = portBControl_io_deq_bits_write; // @[Mem.scala 24:7]
  assign mem_io_portB_control_bits_address = portBControl_io_deq_bits_address; // @[Mem.scala 24:7]
  assign mem_io_portB_input_valid = adder_io_output_valid; // @[Accumulator.scala 66:15]
  assign mem_io_portB_input_bits_0 = adder_io_output_bits_0; // @[Accumulator.scala 66:15]
  assign mem_io_portB_input_bits_1 = adder_io_output_bits_1; // @[Accumulator.scala 66:15]
  assign mem_io_portB_input_bits_2 = adder_io_output_bits_2; // @[Accumulator.scala 66:15]
  assign mem_io_portB_input_bits_3 = adder_io_output_bits_3; // @[Accumulator.scala 66:15]
  assign mem_io_portB_input_bits_4 = adder_io_output_bits_4; // @[Accumulator.scala 66:15]
  assign mem_io_portB_input_bits_5 = adder_io_output_bits_5; // @[Accumulator.scala 66:15]
  assign mem_io_portB_input_bits_6 = adder_io_output_bits_6; // @[Accumulator.scala 66:15]
  assign mem_io_portB_input_bits_7 = adder_io_output_bits_7; // @[Accumulator.scala 66:15]
  assign mem_io_portB_output_ready = 1'h0; // @[Accumulator.scala 67:22]
  assign mem_io_tracepoint = io_tracepoint; // @[Accumulator.scala 56:21]
  assign mem_io_programCounter = io_programCounter; // @[Accumulator.scala 55:25]
  assign adder_clock = clock;
  assign adder_reset = reset;
  assign adder_io_left_valid = inputDemux_x14_demux_io_out_1_valid; // @[Demux.scala 56:10]
  assign adder_io_left_bits_0 = inputDemux_x14_demux_io_out_1_bits_0; // @[Demux.scala 56:10]
  assign adder_io_left_bits_1 = inputDemux_x14_demux_io_out_1_bits_1; // @[Demux.scala 56:10]
  assign adder_io_left_bits_2 = inputDemux_x14_demux_io_out_1_bits_2; // @[Demux.scala 56:10]
  assign adder_io_left_bits_3 = inputDemux_x14_demux_io_out_1_bits_3; // @[Demux.scala 56:10]
  assign adder_io_left_bits_4 = inputDemux_x14_demux_io_out_1_bits_4; // @[Demux.scala 56:10]
  assign adder_io_left_bits_5 = inputDemux_x14_demux_io_out_1_bits_5; // @[Demux.scala 56:10]
  assign adder_io_left_bits_6 = inputDemux_x14_demux_io_out_1_bits_6; // @[Demux.scala 56:10]
  assign adder_io_left_bits_7 = inputDemux_x14_demux_io_out_1_bits_7; // @[Demux.scala 56:10]
  assign adder_io_right_valid = memOutputDemux_x23_demux_io_out_1_valid; // @[Demux.scala 56:10]
  assign adder_io_right_bits_0 = memOutputDemux_x23_demux_io_out_1_bits_0; // @[Demux.scala 56:10]
  assign adder_io_right_bits_1 = memOutputDemux_x23_demux_io_out_1_bits_1; // @[Demux.scala 56:10]
  assign adder_io_right_bits_2 = memOutputDemux_x23_demux_io_out_1_bits_2; // @[Demux.scala 56:10]
  assign adder_io_right_bits_3 = memOutputDemux_x23_demux_io_out_1_bits_3; // @[Demux.scala 56:10]
  assign adder_io_right_bits_4 = memOutputDemux_x23_demux_io_out_1_bits_4; // @[Demux.scala 56:10]
  assign adder_io_right_bits_5 = memOutputDemux_x23_demux_io_out_1_bits_5; // @[Demux.scala 56:10]
  assign adder_io_right_bits_6 = memOutputDemux_x23_demux_io_out_1_bits_6; // @[Demux.scala 56:10]
  assign adder_io_right_bits_7 = memOutputDemux_x23_demux_io_out_1_bits_7; // @[Demux.scala 56:10]
  assign adder_io_output_ready = mem_io_portB_input_ready; // @[Accumulator.scala 66:15]
  assign control_clock = clock;
  assign control_reset = reset;
  assign control_io_enq_valid = io_control_valid; // @[Decoupled.scala 297:22]
  assign control_io_enq_bits_address = io_control_bits_address; // @[Decoupled.scala 298:21]
  assign control_io_enq_bits_accumulate = io_control_bits_accumulate; // @[Decoupled.scala 298:21]
  assign control_io_enq_bits_write = io_control_bits_write; // @[Decoupled.scala 298:21]
  assign control_io_deq_ready = control_io_deq_bits_write ? _GEN_18 : _GEN_31; // @[Accumulator.scala 108:28]
  assign input__clock = clock;
  assign input__reset = reset;
  assign input__io_enq_valid = io_input_valid; // @[Decoupled.scala 297:22]
  assign input__io_enq_bits_0 = io_input_bits_0; // @[Decoupled.scala 298:21]
  assign input__io_enq_bits_1 = io_input_bits_1; // @[Decoupled.scala 298:21]
  assign input__io_enq_bits_2 = io_input_bits_2; // @[Decoupled.scala 298:21]
  assign input__io_enq_bits_3 = io_input_bits_3; // @[Decoupled.scala 298:21]
  assign input__io_enq_bits_4 = io_input_bits_4; // @[Decoupled.scala 298:21]
  assign input__io_enq_bits_5 = io_input_bits_5; // @[Decoupled.scala 298:21]
  assign input__io_enq_bits_6 = io_input_bits_6; // @[Decoupled.scala 298:21]
  assign input__io_enq_bits_7 = io_input_bits_7; // @[Decoupled.scala 298:21]
  assign input__io_deq_ready = inputDemux_x14_demux_io_in_ready; // @[Demux.scala 54:17]
  assign portBControl_clock = clock;
  assign portBControl_reset = reset;
  assign portBControl_io_enq_valid = control_io_deq_bits_write & _GEN_14; // @[Accumulator.scala 108:28 package.scala 410:15]
  assign portBControl_io_enq_bits_write = control_io_deq_bits_write & _GEN_0; // @[Accumulator.scala 108:28 package.scala 409:14]
  assign portBControl_io_enq_bits_address = control_io_deq_bits_write ? _GEN_12 : 6'h0; // @[Accumulator.scala 108:28 package.scala 409:14]
  assign portBControl_io_deq_ready = mem_io_portB_control_ready; // @[Mem.scala 24:7]
  assign inputDemux_x14_demux_io_in_valid = input__io_deq_valid; // @[Demux.scala 54:17]
  assign inputDemux_x14_demux_io_in_bits_0 = input__io_deq_bits_0; // @[Demux.scala 54:17]
  assign inputDemux_x14_demux_io_in_bits_1 = input__io_deq_bits_1; // @[Demux.scala 54:17]
  assign inputDemux_x14_demux_io_in_bits_2 = input__io_deq_bits_2; // @[Demux.scala 54:17]
  assign inputDemux_x14_demux_io_in_bits_3 = input__io_deq_bits_3; // @[Demux.scala 54:17]
  assign inputDemux_x14_demux_io_in_bits_4 = input__io_deq_bits_4; // @[Demux.scala 54:17]
  assign inputDemux_x14_demux_io_in_bits_5 = input__io_deq_bits_5; // @[Demux.scala 54:17]
  assign inputDemux_x14_demux_io_in_bits_6 = input__io_deq_bits_6; // @[Demux.scala 54:17]
  assign inputDemux_x14_demux_io_in_bits_7 = input__io_deq_bits_7; // @[Demux.scala 54:17]
  assign inputDemux_x14_demux_io_sel_valid = inputDemux_io_deq_valid; // @[Mem.scala 24:7]
  assign inputDemux_x14_demux_io_sel_bits = inputDemux_io_deq_bits; // @[Mem.scala 24:7]
  assign inputDemux_x14_demux_io_out_0_ready = mem_io_portA_input_ready; // @[Demux.scala 55:10]
  assign inputDemux_x14_demux_io_out_1_ready = adder_io_left_ready; // @[Demux.scala 56:10]
  assign inputDemux_clock = clock;
  assign inputDemux_reset = reset;
  assign inputDemux_io_enq_valid = control_io_deq_bits_write & _GEN_17; // @[Accumulator.scala 108:28 package.scala 410:15]
  assign inputDemux_io_enq_bits = control_io_deq_bits_write & _GEN_0; // @[Accumulator.scala 108:28 package.scala 409:14]
  assign inputDemux_io_deq_ready = inputDemux_x14_demux_io_sel_ready; // @[Mem.scala 24:7]
  assign memOutputDemux_x23_demux_io_in_valid = mem_io_portA_output_valid; // @[Demux.scala 54:17]
  assign memOutputDemux_x23_demux_io_in_bits_0 = mem_io_portA_output_bits_0; // @[Demux.scala 54:17]
  assign memOutputDemux_x23_demux_io_in_bits_1 = mem_io_portA_output_bits_1; // @[Demux.scala 54:17]
  assign memOutputDemux_x23_demux_io_in_bits_2 = mem_io_portA_output_bits_2; // @[Demux.scala 54:17]
  assign memOutputDemux_x23_demux_io_in_bits_3 = mem_io_portA_output_bits_3; // @[Demux.scala 54:17]
  assign memOutputDemux_x23_demux_io_in_bits_4 = mem_io_portA_output_bits_4; // @[Demux.scala 54:17]
  assign memOutputDemux_x23_demux_io_in_bits_5 = mem_io_portA_output_bits_5; // @[Demux.scala 54:17]
  assign memOutputDemux_x23_demux_io_in_bits_6 = mem_io_portA_output_bits_6; // @[Demux.scala 54:17]
  assign memOutputDemux_x23_demux_io_in_bits_7 = mem_io_portA_output_bits_7; // @[Demux.scala 54:17]
  assign memOutputDemux_x23_demux_io_sel_valid = memOutputDemux_io_deq_valid; // @[Mem.scala 24:7]
  assign memOutputDemux_x23_demux_io_sel_bits = memOutputDemux_io_deq_bits; // @[Mem.scala 24:7]
  assign memOutputDemux_x23_demux_io_out_0_ready = io_output_ready; // @[Demux.scala 55:10]
  assign memOutputDemux_x23_demux_io_out_1_ready = adder_io_right_ready; // @[Demux.scala 56:10]
  assign memOutputDemux_clock = clock;
  assign memOutputDemux_reset = reset;
  assign memOutputDemux_io_enq_valid = control_io_deq_bits_write ? _GEN_9 : _GEN_30; // @[Accumulator.scala 108:28]
  assign memOutputDemux_io_enq_bits = control_io_deq_bits_write & _GEN_0; // @[Accumulator.scala 108:28]
  assign memOutputDemux_io_deq_ready = memOutputDemux_x23_demux_io_sel_ready; // @[Mem.scala 24:7]
  assign writeEnqueuer_clock = clock;
  assign writeEnqueuer_reset = reset;
  assign writeEnqueuer_io_in_valid = control_io_deq_bits_write & _GEN_19; // @[Accumulator.scala 108:28 MultiEnqueue.scala 40:17]
  assign writeEnqueuer_io_out_0_ready = control_io_deq_bits_write & _GEN_20; // @[Accumulator.scala 108:28 MultiEnqueue.scala 42:18]
  assign writeEnqueuer_io_out_1_ready = control_io_deq_bits_write & _GEN_21; // @[Accumulator.scala 108:28 MultiEnqueue.scala 42:18]
  assign readEnqueuer_clock = clock;
  assign readEnqueuer_reset = reset;
  assign readEnqueuer_io_in_valid = control_io_deq_bits_write ? 1'h0 : _GEN_22; // @[Accumulator.scala 108:28 MultiEnqueue.scala 40:17]
  assign readEnqueuer_io_out_0_ready = control_io_deq_bits_write ? 1'h0 : _GEN_23; // @[Accumulator.scala 108:28 MultiEnqueue.scala 42:18]
  assign readEnqueuer_io_out_1_ready = control_io_deq_bits_write ? 1'h0 : _GEN_28; // @[Accumulator.scala 108:28 MultiEnqueue.scala 42:18]
  assign accEnqueuer_clock = clock;
  assign accEnqueuer_reset = reset;
  assign accEnqueuer_io_in_valid = control_io_deq_bits_write & _GEN_1; // @[Accumulator.scala 108:28 MultiEnqueue.scala 40:17]
  assign accEnqueuer_io_out_0_ready = control_io_deq_bits_write & _GEN_2; // @[Accumulator.scala 108:28 MultiEnqueue.scala 42:18]
  assign accEnqueuer_io_out_1_ready = control_io_deq_bits_write & _GEN_7; // @[Accumulator.scala 108:28 MultiEnqueue.scala 42:18]
  assign accEnqueuer_io_out_2_ready = control_io_deq_bits_write & _GEN_10; // @[Accumulator.scala 108:28 MultiEnqueue.scala 42:18]
  assign accEnqueuer_io_out_3_ready = control_io_deq_bits_write & _GEN_15; // @[Accumulator.scala 108:28 MultiEnqueue.scala 42:18]
  always @(posedge clock) begin
    if (reset) begin // @[Accumulator.scala 105:34]
      writeAccumulating <= 1'h0; // @[Accumulator.scala 105:34]
    end else begin
      writeAccumulating <= _GEN_32;
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
  writeAccumulating = _RAND_0[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Queue_18(
  input         clock,
  input         reset,
  output        io_enq_ready,
  input         io_enq_valid,
  input  [15:0] io_enq_bits_0,
  input  [15:0] io_enq_bits_1,
  input  [15:0] io_enq_bits_2,
  input  [15:0] io_enq_bits_3,
  input  [15:0] io_enq_bits_4,
  input  [15:0] io_enq_bits_5,
  input  [15:0] io_enq_bits_6,
  input  [15:0] io_enq_bits_7,
  input         io_deq_ready,
  output        io_deq_valid,
  output [15:0] io_deq_bits_0,
  output [15:0] io_deq_bits_1,
  output [15:0] io_deq_bits_2,
  output [15:0] io_deq_bits_3,
  output [15:0] io_deq_bits_4,
  output [15:0] io_deq_bits_5,
  output [15:0] io_deq_bits_6,
  output [15:0] io_deq_bits_7
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_6;
  reg [31:0] _RAND_7;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_8;
  reg [31:0] _RAND_9;
  reg [31:0] _RAND_10;
`endif // RANDOMIZE_REG_INIT
  reg [15:0] ram_0 [0:1]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_0_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_0_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_0_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_0_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_0_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_0_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_1 [0:1]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_1_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_1_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_1_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_1_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_1_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_1_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_2 [0:1]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_2_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_2_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_2_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_2_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_2_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_2_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_3 [0:1]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_3_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_3_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_3_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_3_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_3_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_3_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_4 [0:1]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_4_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_4_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_4_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_4_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_4_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_4_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_5 [0:1]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_5_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_5_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_5_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_5_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_5_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_5_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_6 [0:1]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_6_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_6_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_6_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_6_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_6_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_6_MPORT_en; // @[Decoupled.scala 218:16]
  reg [15:0] ram_7 [0:1]; // @[Decoupled.scala 218:16]
  wire [15:0] ram_7_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_7_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [15:0] ram_7_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_7_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_7_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_7_MPORT_en; // @[Decoupled.scala 218:16]
  reg  enq_ptr_value; // @[Counter.scala 60:40]
  reg  deq_ptr_value; // @[Counter.scala 60:40]
  reg  maybe_full; // @[Decoupled.scala 221:27]
  wire  ptr_match = enq_ptr_value == deq_ptr_value; // @[Decoupled.scala 223:33]
  wire  empty = ptr_match & ~maybe_full; // @[Decoupled.scala 224:25]
  wire  full = ptr_match & maybe_full; // @[Decoupled.scala 225:24]
  wire  _do_enq_T = io_enq_ready & io_enq_valid; // @[Decoupled.scala 40:37]
  wire  _do_deq_T = io_deq_ready & io_deq_valid; // @[Decoupled.scala 40:37]
  wire  _GEN_16 = io_deq_ready ? 1'h0 : _do_enq_T; // @[Decoupled.scala 249:27 Decoupled.scala 249:36]
  wire  do_enq = empty ? _GEN_16 : _do_enq_T; // @[Decoupled.scala 246:18]
  wire  do_deq = empty ? 1'h0 : _do_deq_T; // @[Decoupled.scala 246:18 Decoupled.scala 248:14]
  assign ram_0_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_0_io_deq_bits_MPORT_data = ram_0[ram_0_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_0_MPORT_data = io_enq_bits_0;
  assign ram_0_MPORT_addr = enq_ptr_value;
  assign ram_0_MPORT_mask = 1'h1;
  assign ram_0_MPORT_en = empty ? _GEN_16 : _do_enq_T;
  assign ram_1_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_1_io_deq_bits_MPORT_data = ram_1[ram_1_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_1_MPORT_data = io_enq_bits_1;
  assign ram_1_MPORT_addr = enq_ptr_value;
  assign ram_1_MPORT_mask = 1'h1;
  assign ram_1_MPORT_en = empty ? _GEN_16 : _do_enq_T;
  assign ram_2_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_2_io_deq_bits_MPORT_data = ram_2[ram_2_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_2_MPORT_data = io_enq_bits_2;
  assign ram_2_MPORT_addr = enq_ptr_value;
  assign ram_2_MPORT_mask = 1'h1;
  assign ram_2_MPORT_en = empty ? _GEN_16 : _do_enq_T;
  assign ram_3_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_3_io_deq_bits_MPORT_data = ram_3[ram_3_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_3_MPORT_data = io_enq_bits_3;
  assign ram_3_MPORT_addr = enq_ptr_value;
  assign ram_3_MPORT_mask = 1'h1;
  assign ram_3_MPORT_en = empty ? _GEN_16 : _do_enq_T;
  assign ram_4_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_4_io_deq_bits_MPORT_data = ram_4[ram_4_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_4_MPORT_data = io_enq_bits_4;
  assign ram_4_MPORT_addr = enq_ptr_value;
  assign ram_4_MPORT_mask = 1'h1;
  assign ram_4_MPORT_en = empty ? _GEN_16 : _do_enq_T;
  assign ram_5_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_5_io_deq_bits_MPORT_data = ram_5[ram_5_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_5_MPORT_data = io_enq_bits_5;
  assign ram_5_MPORT_addr = enq_ptr_value;
  assign ram_5_MPORT_mask = 1'h1;
  assign ram_5_MPORT_en = empty ? _GEN_16 : _do_enq_T;
  assign ram_6_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_6_io_deq_bits_MPORT_data = ram_6[ram_6_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_6_MPORT_data = io_enq_bits_6;
  assign ram_6_MPORT_addr = enq_ptr_value;
  assign ram_6_MPORT_mask = 1'h1;
  assign ram_6_MPORT_en = empty ? _GEN_16 : _do_enq_T;
  assign ram_7_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_7_io_deq_bits_MPORT_data = ram_7[ram_7_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_7_MPORT_data = io_enq_bits_7;
  assign ram_7_MPORT_addr = enq_ptr_value;
  assign ram_7_MPORT_mask = 1'h1;
  assign ram_7_MPORT_en = empty ? _GEN_16 : _do_enq_T;
  assign io_enq_ready = ~full; // @[Decoupled.scala 241:19]
  assign io_deq_valid = io_enq_valid | ~empty; // @[Decoupled.scala 245:25 Decoupled.scala 245:40 Decoupled.scala 240:16]
  assign io_deq_bits_0 = empty ? $signed(io_enq_bits_0) : $signed(ram_0_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_1 = empty ? $signed(io_enq_bits_1) : $signed(ram_1_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_2 = empty ? $signed(io_enq_bits_2) : $signed(ram_2_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_3 = empty ? $signed(io_enq_bits_3) : $signed(ram_3_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_4 = empty ? $signed(io_enq_bits_4) : $signed(ram_4_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_5 = empty ? $signed(io_enq_bits_5) : $signed(ram_5_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_6 = empty ? $signed(io_enq_bits_6) : $signed(ram_6_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  assign io_deq_bits_7 = empty ? $signed(io_enq_bits_7) : $signed(ram_7_io_deq_bits_MPORT_data); // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  always @(posedge clock) begin
    if(ram_0_MPORT_en & ram_0_MPORT_mask) begin
      ram_0[ram_0_MPORT_addr] <= ram_0_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_1_MPORT_en & ram_1_MPORT_mask) begin
      ram_1[ram_1_MPORT_addr] <= ram_1_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_2_MPORT_en & ram_2_MPORT_mask) begin
      ram_2[ram_2_MPORT_addr] <= ram_2_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_3_MPORT_en & ram_3_MPORT_mask) begin
      ram_3[ram_3_MPORT_addr] <= ram_3_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_4_MPORT_en & ram_4_MPORT_mask) begin
      ram_4[ram_4_MPORT_addr] <= ram_4_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_5_MPORT_en & ram_5_MPORT_mask) begin
      ram_5[ram_5_MPORT_addr] <= ram_5_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_6_MPORT_en & ram_6_MPORT_mask) begin
      ram_6[ram_6_MPORT_addr] <= ram_6_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_7_MPORT_en & ram_7_MPORT_mask) begin
      ram_7[ram_7_MPORT_addr] <= ram_7_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if (reset) begin // @[Counter.scala 60:40]
      enq_ptr_value <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_enq) begin // @[Decoupled.scala 229:17]
      enq_ptr_value <= enq_ptr_value + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Counter.scala 60:40]
      deq_ptr_value <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_deq) begin // @[Decoupled.scala 233:17]
      deq_ptr_value <= deq_ptr_value + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Decoupled.scala 221:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 221:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 236:28]
      if (empty) begin // @[Decoupled.scala 246:18]
        if (io_deq_ready) begin // @[Decoupled.scala 249:27]
          maybe_full <= 1'h0; // @[Decoupled.scala 249:36]
        end else begin
          maybe_full <= _do_enq_T;
        end
      end else begin
        maybe_full <= _do_enq_T;
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
`ifdef RANDOMIZE_MEM_INIT
  _RAND_0 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_0[initvar] = _RAND_0[15:0];
  _RAND_1 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_1[initvar] = _RAND_1[15:0];
  _RAND_2 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_2[initvar] = _RAND_2[15:0];
  _RAND_3 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_3[initvar] = _RAND_3[15:0];
  _RAND_4 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_4[initvar] = _RAND_4[15:0];
  _RAND_5 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_5[initvar] = _RAND_5[15:0];
  _RAND_6 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_6[initvar] = _RAND_6[15:0];
  _RAND_7 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_7[initvar] = _RAND_7[15:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_8 = {1{`RANDOM}};
  enq_ptr_value = _RAND_8[0:0];
  _RAND_9 = {1{`RANDOM}};
  deq_ptr_value = _RAND_9[0:0];
  _RAND_10 = {1{`RANDOM}};
  maybe_full = _RAND_10[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module ALU(
  input         clock,
  input         reset,
  input  [3:0]  io_op,
  input  [15:0] io_input,
  input         io_sourceLeft,
  input         io_sourceRight,
  input         io_dest,
  output [15:0] io_output
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_6;
`endif // RANDOMIZE_REG_INIT
  reg [3:0] op; // @[ALU.scala 35:42]
  reg [15:0] input_; // @[ALU.scala 36:42]
  reg  sourceLeftInput; // @[ALU.scala 38:32]
  reg  sourceRightInput; // @[ALU.scala 40:32]
  reg  destInput; // @[ALU.scala 41:46]
  reg [15:0] reg_0; // @[ALU.scala 43:20]
  wire [15:0] sourceLeft = ~sourceLeftInput ? $signed(input_) : $signed(reg_0); // @[ALU.scala 45:8]
  wire [15:0] sourceRight = ~sourceRightInput ? $signed(input_) : $signed(reg_0); // @[ALU.scala 47:8]
  wire  _dest_T_2 = ~destInput | op == 4'h0; // @[ALU.scala 51:25]
  wire  _result_T_54 = $signed(sourceLeft) < $signed(sourceRight); // @[ALU.scala 128:29]
  wire [15:0] _result_T_55 = $signed(sourceLeft) < $signed(sourceRight) ? $signed(sourceRight) : $signed(sourceLeft); // @[ALU.scala 128:29]
  wire [15:0] _result_T_53 = _result_T_54 ? $signed(sourceLeft) : $signed(sourceRight); // @[ALU.scala 125:29]
  wire [15:0] _GEN_17 = $signed(sourceLeft) >= $signed(sourceRight) ? $signed(16'sh100) : $signed(16'sh0); // @[ALU.scala 120:37 ALU.scala 121:14 ALU.scala 119:12]
  wire [15:0] _GEN_15 = $signed(sourceLeft) > $signed(sourceRight) ? $signed(16'sh100) : $signed(16'sh0); // @[ALU.scala 114:36 ALU.scala 115:14 ALU.scala 113:12]
  wire [15:0] _result_T_42 = 16'sh0 - $signed(sourceLeft); // @[ALU.scala 108:26]
  wire [15:0] _result_T_43 = $signed(sourceLeft) < 16'sh0 ? $signed(_result_T_42) : $signed(sourceLeft); // @[ALU.scala 108:26]
  wire [31:0] _result_mac_T_8 = $signed(sourceLeft) * $signed(sourceRight); // @[package.scala 122:18]
  wire [32:0] result_mac_4 = {{1{_result_mac_T_8[31]}},_result_mac_T_8}; // @[package.scala 122:23]
  wire [24:0] _result_adjusted_T_12 = result_mac_4[32:8]; // @[package.scala 135:26]
  wire [32:0] _result_adjustment_T_45 = $signed(result_mac_4) & 33'sh80; // @[package.scala 130:16]
  wire [8:0] result_mask1_4 = 9'sh80 - 9'sh1; // @[package.scala 125:44]
  wire [32:0] _GEN_21 = {{24{result_mask1_4[8]}},result_mask1_4}; // @[package.scala 130:44]
  wire [32:0] _result_adjustment_T_48 = $signed(result_mac_4) & $signed(_GEN_21); // @[package.scala 130:44]
  wire [32:0] _result_adjustment_T_51 = $signed(result_mac_4) & 33'sh100; // @[package.scala 130:71]
  wire  _result_adjustment_T_54 = $signed(_result_adjustment_T_45) != 33'sh0 & ($signed(_result_adjustment_T_48) != 33'sh0
     | $signed(_result_adjustment_T_51) != 33'sh0); // @[package.scala 130:34]
  wire [1:0] result_adjustment_4 = _result_adjustment_T_54 ? $signed(2'sh1) : $signed(2'sh0); // @[package.scala 129:10]
  wire [24:0] _GEN_22 = {{23{result_adjustment_4[1]}},result_adjustment_4}; // @[package.scala 135:42]
  wire [24:0] result_adjusted_4 = $signed(_result_adjusted_T_12) + $signed(_GEN_22); // @[package.scala 135:42]
  wire [24:0] _result_saturated_T_14 = $signed(result_adjusted_4) < -25'sh8000 ? $signed(-25'sh8000) : $signed(
    result_adjusted_4); // @[package.scala 103:26]
  wire [24:0] result_saturated_4 = $signed(result_adjusted_4) > 25'sh7fff ? $signed(25'sh7fff) : $signed(
    _result_saturated_T_14); // @[package.scala 103:8]
  wire [25:0] _result_mac_T_6 = $signed(sourceLeft) * 10'sh100; // @[package.scala 122:18]
  wire [16:0] _result_T_33 = 16'sh0 - $signed(sourceRight); // @[package.scala 176:45]
  wire [24:0] _result_mac_T_7 = {$signed(_result_T_33), 8'h0}; // @[package.scala 122:29]
  wire [25:0] _GEN_23 = {{1{_result_mac_T_7[24]}},_result_mac_T_7}; // @[package.scala 122:23]
  wire [26:0] result_mac_3 = $signed(_result_mac_T_6) + $signed(_GEN_23); // @[package.scala 122:23]
  wire [18:0] _result_adjusted_T_9 = result_mac_3[26:8]; // @[package.scala 135:26]
  wire [26:0] _result_adjustment_T_34 = $signed(result_mac_3) & 27'sh80; // @[package.scala 130:16]
  wire [26:0] _GEN_24 = {{18{result_mask1_4[8]}},result_mask1_4}; // @[package.scala 130:44]
  wire [26:0] _result_adjustment_T_37 = $signed(result_mac_3) & $signed(_GEN_24); // @[package.scala 130:44]
  wire [26:0] _result_adjustment_T_40 = $signed(result_mac_3) & 27'sh100; // @[package.scala 130:71]
  wire  _result_adjustment_T_43 = $signed(_result_adjustment_T_34) != 27'sh0 & ($signed(_result_adjustment_T_37) != 27'sh0
     | $signed(_result_adjustment_T_40) != 27'sh0); // @[package.scala 130:34]
  wire [1:0] result_adjustment_3 = _result_adjustment_T_43 ? $signed(2'sh1) : $signed(2'sh0); // @[package.scala 129:10]
  wire [18:0] _GEN_25 = {{17{result_adjustment_3[1]}},result_adjustment_3}; // @[package.scala 135:42]
  wire [18:0] result_adjusted_3 = $signed(_result_adjusted_T_9) + $signed(_GEN_25); // @[package.scala 135:42]
  wire [18:0] _result_saturated_T_11 = $signed(result_adjusted_3) < -19'sh8000 ? $signed(-19'sh8000) : $signed(
    result_adjusted_3); // @[package.scala 103:26]
  wire [18:0] result_saturated_3 = $signed(result_adjusted_3) > 19'sh7fff ? $signed(19'sh7fff) : $signed(
    _result_saturated_T_11); // @[package.scala 103:8]
  wire [23:0] _result_mac_T_5 = {$signed(sourceRight), 8'h0}; // @[package.scala 122:29]
  wire [25:0] _GEN_26 = {{2{_result_mac_T_5[23]}},_result_mac_T_5}; // @[package.scala 122:23]
  wire [26:0] result_mac_2 = $signed(_result_mac_T_6) + $signed(_GEN_26); // @[package.scala 122:23]
  wire [18:0] _result_adjusted_T_6 = result_mac_2[26:8]; // @[package.scala 135:26]
  wire [26:0] _result_adjustment_T_23 = $signed(result_mac_2) & 27'sh80; // @[package.scala 130:16]
  wire [26:0] _result_adjustment_T_26 = $signed(result_mac_2) & $signed(_GEN_24); // @[package.scala 130:44]
  wire [26:0] _result_adjustment_T_29 = $signed(result_mac_2) & 27'sh100; // @[package.scala 130:71]
  wire  _result_adjustment_T_32 = $signed(_result_adjustment_T_23) != 27'sh0 & ($signed(_result_adjustment_T_26) != 27'sh0
     | $signed(_result_adjustment_T_29) != 27'sh0); // @[package.scala 130:34]
  wire [1:0] result_adjustment_2 = _result_adjustment_T_32 ? $signed(2'sh1) : $signed(2'sh0); // @[package.scala 129:10]
  wire [18:0] _GEN_28 = {{17{result_adjustment_2[1]}},result_adjustment_2}; // @[package.scala 135:42]
  wire [18:0] result_adjusted_2 = $signed(_result_adjusted_T_6) + $signed(_GEN_28); // @[package.scala 135:42]
  wire [18:0] _result_saturated_T_8 = $signed(result_adjusted_2) < -19'sh8000 ? $signed(-19'sh8000) : $signed(
    result_adjusted_2); // @[package.scala 103:26]
  wire [18:0] result_saturated_2 = $signed(result_adjusted_2) > 19'sh7fff ? $signed(19'sh7fff) : $signed(
    _result_saturated_T_8); // @[package.scala 103:8]
  wire [16:0] _result_T_24 = 16'sh0 - 16'sh100; // @[package.scala 176:45]
  wire [24:0] _result_mac_T_3 = {$signed(_result_T_24), 8'h0}; // @[package.scala 122:29]
  wire [25:0] _GEN_29 = {{1{_result_mac_T_3[24]}},_result_mac_T_3}; // @[package.scala 122:23]
  wire [26:0] result_mac_1 = $signed(_result_mac_T_6) + $signed(_GEN_29); // @[package.scala 122:23]
  wire [18:0] _result_adjusted_T_3 = result_mac_1[26:8]; // @[package.scala 135:26]
  wire [26:0] _result_adjustment_T_12 = $signed(result_mac_1) & 27'sh80; // @[package.scala 130:16]
  wire [26:0] _result_adjustment_T_15 = $signed(result_mac_1) & $signed(_GEN_24); // @[package.scala 130:44]
  wire [26:0] _result_adjustment_T_18 = $signed(result_mac_1) & 27'sh100; // @[package.scala 130:71]
  wire  _result_adjustment_T_21 = $signed(_result_adjustment_T_12) != 27'sh0 & ($signed(_result_adjustment_T_15) != 27'sh0
     | $signed(_result_adjustment_T_18) != 27'sh0); // @[package.scala 130:34]
  wire [1:0] result_adjustment_1 = _result_adjustment_T_21 ? $signed(2'sh1) : $signed(2'sh0); // @[package.scala 129:10]
  wire [18:0] _GEN_31 = {{17{result_adjustment_1[1]}},result_adjustment_1}; // @[package.scala 135:42]
  wire [18:0] result_adjusted_1 = $signed(_result_adjusted_T_3) + $signed(_GEN_31); // @[package.scala 135:42]
  wire [18:0] _result_saturated_T_5 = $signed(result_adjusted_1) < -19'sh8000 ? $signed(-19'sh8000) : $signed(
    result_adjusted_1); // @[package.scala 103:26]
  wire [18:0] result_saturated_1 = $signed(result_adjusted_1) > 19'sh7fff ? $signed(19'sh7fff) : $signed(
    _result_saturated_T_5); // @[package.scala 103:8]
  wire [26:0] result_mac = $signed(_result_mac_T_6) + 26'sh10000; // @[package.scala 122:23]
  wire [18:0] _result_adjusted_T = result_mac[26:8]; // @[package.scala 135:26]
  wire [26:0] _result_adjustment_T_1 = $signed(result_mac) & 27'sh80; // @[package.scala 130:16]
  wire [26:0] _result_adjustment_T_4 = $signed(result_mac) & $signed(_GEN_24); // @[package.scala 130:44]
  wire [26:0] _result_adjustment_T_7 = $signed(result_mac) & 27'sh100; // @[package.scala 130:71]
  wire  _result_adjustment_T_10 = $signed(_result_adjustment_T_1) != 27'sh0 & ($signed(_result_adjustment_T_4) != 27'sh0
     | $signed(_result_adjustment_T_7) != 27'sh0); // @[package.scala 130:34]
  wire [1:0] result_adjustment = _result_adjustment_T_10 ? $signed(2'sh1) : $signed(2'sh0); // @[package.scala 129:10]
  wire [18:0] _GEN_33 = {{17{result_adjustment[1]}},result_adjustment}; // @[package.scala 135:42]
  wire [18:0] result_adjusted = $signed(_result_adjusted_T) + $signed(_GEN_33); // @[package.scala 135:42]
  wire [18:0] _result_saturated_T_2 = $signed(result_adjusted) < -19'sh8000 ? $signed(-19'sh8000) : $signed(
    result_adjusted); // @[package.scala 103:26]
  wire [18:0] result_saturated = $signed(result_adjusted) > 19'sh7fff ? $signed(19'sh7fff) : $signed(
    _result_saturated_T_2); // @[package.scala 103:8]
  wire  _T_28 = $signed(sourceLeft) >= 16'sh0 & 16'sh0 >= $signed(sourceLeft); // @[ALU.scala 132:54]
  wire  _T_29 = ~_T_28; // @[ALU.scala 131:40]
  wire  _T_34 = $signed(sourceRight) >= 16'sh0 & 16'sh0 >= $signed(sourceRight); // @[ALU.scala 132:54]
  wire  _T_35 = ~_T_34; // @[ALU.scala 131:40]
  wire [15:0] _GEN_7 = _T_29 | _T_35 ? $signed(16'sh100) : $signed(16'sh0); // @[ALU.scala 86:53 ALU.scala 87:14 ALU.scala 85:12]
  wire [15:0] _GEN_5 = _T_29 & _T_35 ? $signed(16'sh100) : $signed(16'sh0); // @[ALU.scala 80:53 ALU.scala 81:14 ALU.scala 79:12]
  wire [15:0] _GEN_3 = _T_29 ? $signed(16'sh0) : $signed(16'sh100); // @[ALU.scala 74:30 ALU.scala 75:14 ALU.scala 73:12]
  wire [15:0] _GEN_1 = op == 4'h1 ? $signed(16'sh0) : $signed(input_); // @[ALU.scala 64:26 ALU.scala 65:12 ALU.scala 62:10]
  wire [15:0] _GEN_2 = op == 4'h2 ? $signed(sourceLeft) : $signed(_GEN_1); // @[ALU.scala 67:26 ALU.scala 68:12]
  wire [15:0] _GEN_4 = op == 4'h3 ? $signed(_GEN_3) : $signed(_GEN_2); // @[ALU.scala 72:25]
  wire [15:0] _GEN_6 = op == 4'h4 ? $signed(_GEN_5) : $signed(_GEN_4); // @[ALU.scala 78:25]
  wire [15:0] _GEN_8 = op == 4'h5 ? $signed(_GEN_7) : $signed(_GEN_6); // @[ALU.scala 84:24]
  wire [18:0] _GEN_9 = op == 4'h6 ? $signed(result_saturated) : $signed({{3{_GEN_8[15]}},_GEN_8}); // @[ALU.scala 92:31 ALU.scala 93:12]
  wire [18:0] _GEN_10 = op == 4'h7 ? $signed(result_saturated_1) : $signed(_GEN_9); // @[ALU.scala 95:31 ALU.scala 96:12]
  wire [18:0] _GEN_11 = op == 4'h8 ? $signed(result_saturated_2) : $signed(_GEN_10); // @[ALU.scala 98:25 ALU.scala 99:12]
  wire [18:0] _GEN_12 = op == 4'h9 ? $signed(result_saturated_3) : $signed(_GEN_11); // @[ALU.scala 101:30 ALU.scala 102:12]
  wire [24:0] _GEN_13 = op == 4'ha ? $signed(result_saturated_4) : $signed({{6{_GEN_12[18]}},_GEN_12}); // @[ALU.scala 104:30 ALU.scala 105:12]
  wire [24:0] _GEN_14 = op == 4'hb ? $signed({{9{_result_T_43[15]}},_result_T_43}) : $signed(_GEN_13); // @[ALU.scala 107:25 ALU.scala 108:12]
  wire [24:0] _GEN_16 = op == 4'hc ? $signed({{9{_GEN_15[15]}},_GEN_15}) : $signed(_GEN_14); // @[ALU.scala 112:33]
  wire [24:0] _GEN_18 = op == 4'hd ? $signed({{9{_GEN_17[15]}},_GEN_17}) : $signed(_GEN_16); // @[ALU.scala 118:38]
  wire [24:0] _GEN_19 = op == 4'he ? $signed({{9{_result_T_53[15]}},_result_T_53}) : $signed(_GEN_18); // @[ALU.scala 124:25 ALU.scala 125:12]
  wire [24:0] _GEN_20 = op == 4'hf ? $signed({{9{_result_T_55[15]}},_result_T_55}) : $signed(_GEN_19); // @[ALU.scala 127:25 ALU.scala 128:12]
  wire [15:0] result = _GEN_20[15:0]; // @[ALU.scala 56:20]
  reg [15:0] output_; // @[ALU.scala 57:43]
  assign io_output = output_; // @[ALU.scala 58:13]
  always @(posedge clock) begin
    op <= io_op; // @[ALU.scala 35:42]
    input_ <= io_input; // @[ALU.scala 36:42]
    sourceLeftInput <= io_sourceLeft; // @[ALU.scala 38:32]
    sourceRightInput <= io_sourceRight; // @[ALU.scala 40:32]
    destInput <= io_dest; // @[ALU.scala 41:46]
    if (reset) begin // @[ALU.scala 43:20]
      reg_0 <= 16'sh0; // @[ALU.scala 43:20]
    end else if (!(_dest_T_2)) begin // @[Demux.scala 12:16]
      reg_0 <= result; // @[Demux.scala 15:11]
    end
    output_ <= _GEN_20[15:0]; // @[ALU.scala 56:20]
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
  op = _RAND_0[3:0];
  _RAND_1 = {1{`RANDOM}};
  input_ = _RAND_1[15:0];
  _RAND_2 = {1{`RANDOM}};
  sourceLeftInput = _RAND_2[0:0];
  _RAND_3 = {1{`RANDOM}};
  sourceRightInput = _RAND_3[0:0];
  _RAND_4 = {1{`RANDOM}};
  destInput = _RAND_4[0:0];
  _RAND_5 = {1{`RANDOM}};
  reg_0 = _RAND_5[15:0];
  _RAND_6 = {1{`RANDOM}};
  output_ = _RAND_6[15:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module ALUArray(
  input         clock,
  input         reset,
  output        io_input_ready,
  input         io_input_valid,
  input  [15:0] io_input_bits_0,
  input  [15:0] io_input_bits_1,
  input  [15:0] io_input_bits_2,
  input  [15:0] io_input_bits_3,
  input  [15:0] io_input_bits_4,
  input  [15:0] io_input_bits_5,
  input  [15:0] io_input_bits_6,
  input  [15:0] io_input_bits_7,
  input         io_output_ready,
  output        io_output_valid,
  output [15:0] io_output_bits_0,
  output [15:0] io_output_bits_1,
  output [15:0] io_output_bits_2,
  output [15:0] io_output_bits_3,
  output [15:0] io_output_bits_4,
  output [15:0] io_output_bits_5,
  output [15:0] io_output_bits_6,
  output [15:0] io_output_bits_7,
  output        io_instruction_ready,
  input         io_instruction_valid,
  input  [3:0]  io_instruction_bits_op,
  input         io_instruction_bits_sourceLeft,
  input         io_instruction_bits_sourceRight,
  input         io_instruction_bits_dest
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
`endif // RANDOMIZE_REG_INIT
  wire  output__clock; // @[ALUArray.scala 34:22]
  wire  output__reset; // @[ALUArray.scala 34:22]
  wire  output__io_enq_ready; // @[ALUArray.scala 34:22]
  wire  output__io_enq_valid; // @[ALUArray.scala 34:22]
  wire [15:0] output__io_enq_bits_0; // @[ALUArray.scala 34:22]
  wire [15:0] output__io_enq_bits_1; // @[ALUArray.scala 34:22]
  wire [15:0] output__io_enq_bits_2; // @[ALUArray.scala 34:22]
  wire [15:0] output__io_enq_bits_3; // @[ALUArray.scala 34:22]
  wire [15:0] output__io_enq_bits_4; // @[ALUArray.scala 34:22]
  wire [15:0] output__io_enq_bits_5; // @[ALUArray.scala 34:22]
  wire [15:0] output__io_enq_bits_6; // @[ALUArray.scala 34:22]
  wire [15:0] output__io_enq_bits_7; // @[ALUArray.scala 34:22]
  wire  output__io_deq_ready; // @[ALUArray.scala 34:22]
  wire  output__io_deq_valid; // @[ALUArray.scala 34:22]
  wire [15:0] output__io_deq_bits_0; // @[ALUArray.scala 34:22]
  wire [15:0] output__io_deq_bits_1; // @[ALUArray.scala 34:22]
  wire [15:0] output__io_deq_bits_2; // @[ALUArray.scala 34:22]
  wire [15:0] output__io_deq_bits_3; // @[ALUArray.scala 34:22]
  wire [15:0] output__io_deq_bits_4; // @[ALUArray.scala 34:22]
  wire [15:0] output__io_deq_bits_5; // @[ALUArray.scala 34:22]
  wire [15:0] output__io_deq_bits_6; // @[ALUArray.scala 34:22]
  wire [15:0] output__io_deq_bits_7; // @[ALUArray.scala 34:22]
  wire  m_clock; // @[ALUArray.scala 53:19]
  wire  m_reset; // @[ALUArray.scala 53:19]
  wire [3:0] m_io_op; // @[ALUArray.scala 53:19]
  wire [15:0] m_io_input; // @[ALUArray.scala 53:19]
  wire  m_io_sourceLeft; // @[ALUArray.scala 53:19]
  wire  m_io_sourceRight; // @[ALUArray.scala 53:19]
  wire  m_io_dest; // @[ALUArray.scala 53:19]
  wire [15:0] m_io_output; // @[ALUArray.scala 53:19]
  wire  m_1_clock; // @[ALUArray.scala 53:19]
  wire  m_1_reset; // @[ALUArray.scala 53:19]
  wire [3:0] m_1_io_op; // @[ALUArray.scala 53:19]
  wire [15:0] m_1_io_input; // @[ALUArray.scala 53:19]
  wire  m_1_io_sourceLeft; // @[ALUArray.scala 53:19]
  wire  m_1_io_sourceRight; // @[ALUArray.scala 53:19]
  wire  m_1_io_dest; // @[ALUArray.scala 53:19]
  wire [15:0] m_1_io_output; // @[ALUArray.scala 53:19]
  wire  m_2_clock; // @[ALUArray.scala 53:19]
  wire  m_2_reset; // @[ALUArray.scala 53:19]
  wire [3:0] m_2_io_op; // @[ALUArray.scala 53:19]
  wire [15:0] m_2_io_input; // @[ALUArray.scala 53:19]
  wire  m_2_io_sourceLeft; // @[ALUArray.scala 53:19]
  wire  m_2_io_sourceRight; // @[ALUArray.scala 53:19]
  wire  m_2_io_dest; // @[ALUArray.scala 53:19]
  wire [15:0] m_2_io_output; // @[ALUArray.scala 53:19]
  wire  m_3_clock; // @[ALUArray.scala 53:19]
  wire  m_3_reset; // @[ALUArray.scala 53:19]
  wire [3:0] m_3_io_op; // @[ALUArray.scala 53:19]
  wire [15:0] m_3_io_input; // @[ALUArray.scala 53:19]
  wire  m_3_io_sourceLeft; // @[ALUArray.scala 53:19]
  wire  m_3_io_sourceRight; // @[ALUArray.scala 53:19]
  wire  m_3_io_dest; // @[ALUArray.scala 53:19]
  wire [15:0] m_3_io_output; // @[ALUArray.scala 53:19]
  wire  m_4_clock; // @[ALUArray.scala 53:19]
  wire  m_4_reset; // @[ALUArray.scala 53:19]
  wire [3:0] m_4_io_op; // @[ALUArray.scala 53:19]
  wire [15:0] m_4_io_input; // @[ALUArray.scala 53:19]
  wire  m_4_io_sourceLeft; // @[ALUArray.scala 53:19]
  wire  m_4_io_sourceRight; // @[ALUArray.scala 53:19]
  wire  m_4_io_dest; // @[ALUArray.scala 53:19]
  wire [15:0] m_4_io_output; // @[ALUArray.scala 53:19]
  wire  m_5_clock; // @[ALUArray.scala 53:19]
  wire  m_5_reset; // @[ALUArray.scala 53:19]
  wire [3:0] m_5_io_op; // @[ALUArray.scala 53:19]
  wire [15:0] m_5_io_input; // @[ALUArray.scala 53:19]
  wire  m_5_io_sourceLeft; // @[ALUArray.scala 53:19]
  wire  m_5_io_sourceRight; // @[ALUArray.scala 53:19]
  wire  m_5_io_dest; // @[ALUArray.scala 53:19]
  wire [15:0] m_5_io_output; // @[ALUArray.scala 53:19]
  wire  m_6_clock; // @[ALUArray.scala 53:19]
  wire  m_6_reset; // @[ALUArray.scala 53:19]
  wire [3:0] m_6_io_op; // @[ALUArray.scala 53:19]
  wire [15:0] m_6_io_input; // @[ALUArray.scala 53:19]
  wire  m_6_io_sourceLeft; // @[ALUArray.scala 53:19]
  wire  m_6_io_sourceRight; // @[ALUArray.scala 53:19]
  wire  m_6_io_dest; // @[ALUArray.scala 53:19]
  wire [15:0] m_6_io_output; // @[ALUArray.scala 53:19]
  wire  m_7_clock; // @[ALUArray.scala 53:19]
  wire  m_7_reset; // @[ALUArray.scala 53:19]
  wire [3:0] m_7_io_op; // @[ALUArray.scala 53:19]
  wire [15:0] m_7_io_input; // @[ALUArray.scala 53:19]
  wire  m_7_io_sourceLeft; // @[ALUArray.scala 53:19]
  wire  m_7_io_sourceRight; // @[ALUArray.scala 53:19]
  wire  m_7_io_dest; // @[ALUArray.scala 53:19]
  wire [15:0] m_7_io_output; // @[ALUArray.scala 53:19]
  wire  _inputNotNeeded_T_11 = io_instruction_bits_op == 4'h2 & io_instruction_bits_op == 4'h3 & io_instruction_bits_op
     == 4'h6 & io_instruction_bits_op == 4'h7 & io_instruction_bits_op == 4'hb; // @[Op.scala 40:39]
  wire  _inputNotNeeded_T_13 = _inputNotNeeded_T_11 & io_instruction_bits_sourceLeft; // @[ALUArray.scala 41:9]
  wire  _inputNotNeeded_T_14 = io_instruction_bits_op == 4'h0 | io_instruction_bits_op == 4'h1 | _inputNotNeeded_T_13; // @[ALUArray.scala 38:76]
  wire  _inputNotNeeded_T_17 = io_instruction_bits_sourceLeft & io_instruction_bits_sourceRight; // @[ALUArray.scala 42:44]
  wire  inputNotNeeded = _inputNotNeeded_T_14 | _inputNotNeeded_T_17; // @[ALUArray.scala 41:49]
  wire  inputNeeded = ~inputNotNeeded; // @[ALUArray.scala 43:21]
  wire  _io_instruction_ready_T_1 = io_input_valid | ~inputNeeded; // @[ALUArray.scala 46:38]
  wire  _output_io_enq_valid_T_2 = _io_instruction_ready_T_1 & io_instruction_valid; // @[ALUArray.scala 48:35]
  reg  output_io_enq_valid_sr_0; // @[ShiftRegister.scala 10:22]
  reg  output_io_enq_valid_sr_1; // @[ShiftRegister.scala 10:22]
  wire  _m_io_op_T = io_instruction_ready & io_instruction_valid; // @[ALUArray.scala 63:25]
  Queue_18 output_ ( // @[ALUArray.scala 34:22]
    .clock(output__clock),
    .reset(output__reset),
    .io_enq_ready(output__io_enq_ready),
    .io_enq_valid(output__io_enq_valid),
    .io_enq_bits_0(output__io_enq_bits_0),
    .io_enq_bits_1(output__io_enq_bits_1),
    .io_enq_bits_2(output__io_enq_bits_2),
    .io_enq_bits_3(output__io_enq_bits_3),
    .io_enq_bits_4(output__io_enq_bits_4),
    .io_enq_bits_5(output__io_enq_bits_5),
    .io_enq_bits_6(output__io_enq_bits_6),
    .io_enq_bits_7(output__io_enq_bits_7),
    .io_deq_ready(output__io_deq_ready),
    .io_deq_valid(output__io_deq_valid),
    .io_deq_bits_0(output__io_deq_bits_0),
    .io_deq_bits_1(output__io_deq_bits_1),
    .io_deq_bits_2(output__io_deq_bits_2),
    .io_deq_bits_3(output__io_deq_bits_3),
    .io_deq_bits_4(output__io_deq_bits_4),
    .io_deq_bits_5(output__io_deq_bits_5),
    .io_deq_bits_6(output__io_deq_bits_6),
    .io_deq_bits_7(output__io_deq_bits_7)
  );
  ALU m ( // @[ALUArray.scala 53:19]
    .clock(m_clock),
    .reset(m_reset),
    .io_op(m_io_op),
    .io_input(m_io_input),
    .io_sourceLeft(m_io_sourceLeft),
    .io_sourceRight(m_io_sourceRight),
    .io_dest(m_io_dest),
    .io_output(m_io_output)
  );
  ALU m_1 ( // @[ALUArray.scala 53:19]
    .clock(m_1_clock),
    .reset(m_1_reset),
    .io_op(m_1_io_op),
    .io_input(m_1_io_input),
    .io_sourceLeft(m_1_io_sourceLeft),
    .io_sourceRight(m_1_io_sourceRight),
    .io_dest(m_1_io_dest),
    .io_output(m_1_io_output)
  );
  ALU m_2 ( // @[ALUArray.scala 53:19]
    .clock(m_2_clock),
    .reset(m_2_reset),
    .io_op(m_2_io_op),
    .io_input(m_2_io_input),
    .io_sourceLeft(m_2_io_sourceLeft),
    .io_sourceRight(m_2_io_sourceRight),
    .io_dest(m_2_io_dest),
    .io_output(m_2_io_output)
  );
  ALU m_3 ( // @[ALUArray.scala 53:19]
    .clock(m_3_clock),
    .reset(m_3_reset),
    .io_op(m_3_io_op),
    .io_input(m_3_io_input),
    .io_sourceLeft(m_3_io_sourceLeft),
    .io_sourceRight(m_3_io_sourceRight),
    .io_dest(m_3_io_dest),
    .io_output(m_3_io_output)
  );
  ALU m_4 ( // @[ALUArray.scala 53:19]
    .clock(m_4_clock),
    .reset(m_4_reset),
    .io_op(m_4_io_op),
    .io_input(m_4_io_input),
    .io_sourceLeft(m_4_io_sourceLeft),
    .io_sourceRight(m_4_io_sourceRight),
    .io_dest(m_4_io_dest),
    .io_output(m_4_io_output)
  );
  ALU m_5 ( // @[ALUArray.scala 53:19]
    .clock(m_5_clock),
    .reset(m_5_reset),
    .io_op(m_5_io_op),
    .io_input(m_5_io_input),
    .io_sourceLeft(m_5_io_sourceLeft),
    .io_sourceRight(m_5_io_sourceRight),
    .io_dest(m_5_io_dest),
    .io_output(m_5_io_output)
  );
  ALU m_6 ( // @[ALUArray.scala 53:19]
    .clock(m_6_clock),
    .reset(m_6_reset),
    .io_op(m_6_io_op),
    .io_input(m_6_io_input),
    .io_sourceLeft(m_6_io_sourceLeft),
    .io_sourceRight(m_6_io_sourceRight),
    .io_dest(m_6_io_dest),
    .io_output(m_6_io_output)
  );
  ALU m_7 ( // @[ALUArray.scala 53:19]
    .clock(m_7_clock),
    .reset(m_7_reset),
    .io_op(m_7_io_op),
    .io_input(m_7_io_input),
    .io_sourceLeft(m_7_io_sourceLeft),
    .io_sourceRight(m_7_io_sourceRight),
    .io_dest(m_7_io_dest),
    .io_output(m_7_io_output)
  );
  assign io_input_ready = output__io_enq_ready & io_instruction_valid & inputNeeded; // @[ALUArray.scala 45:59]
  assign io_output_valid = output__io_deq_valid; // @[ALUArray.scala 35:13]
  assign io_output_bits_0 = output__io_deq_bits_0; // @[ALUArray.scala 35:13]
  assign io_output_bits_1 = output__io_deq_bits_1; // @[ALUArray.scala 35:13]
  assign io_output_bits_2 = output__io_deq_bits_2; // @[ALUArray.scala 35:13]
  assign io_output_bits_3 = output__io_deq_bits_3; // @[ALUArray.scala 35:13]
  assign io_output_bits_4 = output__io_deq_bits_4; // @[ALUArray.scala 35:13]
  assign io_output_bits_5 = output__io_deq_bits_5; // @[ALUArray.scala 35:13]
  assign io_output_bits_6 = output__io_deq_bits_6; // @[ALUArray.scala 35:13]
  assign io_output_bits_7 = output__io_deq_bits_7; // @[ALUArray.scala 35:13]
  assign io_instruction_ready = (io_input_valid | ~inputNeeded) & output__io_enq_ready; // @[ALUArray.scala 46:55]
  assign output__clock = clock;
  assign output__reset = reset;
  assign output__io_enq_valid = output_io_enq_valid_sr_1; // @[ALUArray.scala 47:23]
  assign output__io_enq_bits_0 = m_io_output; // @[ALUArray.scala 71:27]
  assign output__io_enq_bits_1 = m_1_io_output; // @[ALUArray.scala 71:27]
  assign output__io_enq_bits_2 = m_2_io_output; // @[ALUArray.scala 71:27]
  assign output__io_enq_bits_3 = m_3_io_output; // @[ALUArray.scala 71:27]
  assign output__io_enq_bits_4 = m_4_io_output; // @[ALUArray.scala 71:27]
  assign output__io_enq_bits_5 = m_5_io_output; // @[ALUArray.scala 71:27]
  assign output__io_enq_bits_6 = m_6_io_output; // @[ALUArray.scala 71:27]
  assign output__io_enq_bits_7 = m_7_io_output; // @[ALUArray.scala 71:27]
  assign output__io_deq_ready = io_output_ready; // @[ALUArray.scala 35:13]
  assign m_clock = clock;
  assign m_reset = reset;
  assign m_io_op = _m_io_op_T ? io_instruction_bits_op : 4'h0; // @[ALUArray.scala 62:19]
  assign m_io_input = io_input_bits_0; // @[ALUArray.scala 70:16]
  assign m_io_sourceLeft = io_instruction_bits_sourceLeft; // @[ALUArray.scala 67:21]
  assign m_io_sourceRight = io_instruction_bits_sourceRight; // @[ALUArray.scala 68:22]
  assign m_io_dest = io_instruction_bits_dest; // @[ALUArray.scala 69:15]
  assign m_1_clock = clock;
  assign m_1_reset = reset;
  assign m_1_io_op = _m_io_op_T ? io_instruction_bits_op : 4'h0; // @[ALUArray.scala 62:19]
  assign m_1_io_input = io_input_bits_1; // @[ALUArray.scala 70:16]
  assign m_1_io_sourceLeft = io_instruction_bits_sourceLeft; // @[ALUArray.scala 67:21]
  assign m_1_io_sourceRight = io_instruction_bits_sourceRight; // @[ALUArray.scala 68:22]
  assign m_1_io_dest = io_instruction_bits_dest; // @[ALUArray.scala 69:15]
  assign m_2_clock = clock;
  assign m_2_reset = reset;
  assign m_2_io_op = _m_io_op_T ? io_instruction_bits_op : 4'h0; // @[ALUArray.scala 62:19]
  assign m_2_io_input = io_input_bits_2; // @[ALUArray.scala 70:16]
  assign m_2_io_sourceLeft = io_instruction_bits_sourceLeft; // @[ALUArray.scala 67:21]
  assign m_2_io_sourceRight = io_instruction_bits_sourceRight; // @[ALUArray.scala 68:22]
  assign m_2_io_dest = io_instruction_bits_dest; // @[ALUArray.scala 69:15]
  assign m_3_clock = clock;
  assign m_3_reset = reset;
  assign m_3_io_op = _m_io_op_T ? io_instruction_bits_op : 4'h0; // @[ALUArray.scala 62:19]
  assign m_3_io_input = io_input_bits_3; // @[ALUArray.scala 70:16]
  assign m_3_io_sourceLeft = io_instruction_bits_sourceLeft; // @[ALUArray.scala 67:21]
  assign m_3_io_sourceRight = io_instruction_bits_sourceRight; // @[ALUArray.scala 68:22]
  assign m_3_io_dest = io_instruction_bits_dest; // @[ALUArray.scala 69:15]
  assign m_4_clock = clock;
  assign m_4_reset = reset;
  assign m_4_io_op = _m_io_op_T ? io_instruction_bits_op : 4'h0; // @[ALUArray.scala 62:19]
  assign m_4_io_input = io_input_bits_4; // @[ALUArray.scala 70:16]
  assign m_4_io_sourceLeft = io_instruction_bits_sourceLeft; // @[ALUArray.scala 67:21]
  assign m_4_io_sourceRight = io_instruction_bits_sourceRight; // @[ALUArray.scala 68:22]
  assign m_4_io_dest = io_instruction_bits_dest; // @[ALUArray.scala 69:15]
  assign m_5_clock = clock;
  assign m_5_reset = reset;
  assign m_5_io_op = _m_io_op_T ? io_instruction_bits_op : 4'h0; // @[ALUArray.scala 62:19]
  assign m_5_io_input = io_input_bits_5; // @[ALUArray.scala 70:16]
  assign m_5_io_sourceLeft = io_instruction_bits_sourceLeft; // @[ALUArray.scala 67:21]
  assign m_5_io_sourceRight = io_instruction_bits_sourceRight; // @[ALUArray.scala 68:22]
  assign m_5_io_dest = io_instruction_bits_dest; // @[ALUArray.scala 69:15]
  assign m_6_clock = clock;
  assign m_6_reset = reset;
  assign m_6_io_op = _m_io_op_T ? io_instruction_bits_op : 4'h0; // @[ALUArray.scala 62:19]
  assign m_6_io_input = io_input_bits_6; // @[ALUArray.scala 70:16]
  assign m_6_io_sourceLeft = io_instruction_bits_sourceLeft; // @[ALUArray.scala 67:21]
  assign m_6_io_sourceRight = io_instruction_bits_sourceRight; // @[ALUArray.scala 68:22]
  assign m_6_io_dest = io_instruction_bits_dest; // @[ALUArray.scala 69:15]
  assign m_7_clock = clock;
  assign m_7_reset = reset;
  assign m_7_io_op = _m_io_op_T ? io_instruction_bits_op : 4'h0; // @[ALUArray.scala 62:19]
  assign m_7_io_input = io_input_bits_7; // @[ALUArray.scala 70:16]
  assign m_7_io_sourceLeft = io_instruction_bits_sourceLeft; // @[ALUArray.scala 67:21]
  assign m_7_io_sourceRight = io_instruction_bits_sourceRight; // @[ALUArray.scala 68:22]
  assign m_7_io_dest = io_instruction_bits_dest; // @[ALUArray.scala 69:15]
  always @(posedge clock) begin
    if (reset) begin // @[ShiftRegister.scala 10:22]
      output_io_enq_valid_sr_0 <= 1'h0; // @[ShiftRegister.scala 10:22]
    end else begin
      output_io_enq_valid_sr_0 <= _output_io_enq_valid_T_2; // @[ShiftRegister.scala 25:12]
    end
    if (reset) begin // @[ShiftRegister.scala 10:22]
      output_io_enq_valid_sr_1 <= 1'h0; // @[ShiftRegister.scala 10:22]
    end else begin
      output_io_enq_valid_sr_1 <= output_io_enq_valid_sr_0; // @[ShiftRegister.scala 13:11]
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
  output_io_enq_valid_sr_0 = _RAND_0[0:0];
  _RAND_1 = {1{`RANDOM}};
  output_io_enq_valid_sr_1 = _RAND_1[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Mux(
  output        io_in_0_ready,
  input         io_in_0_valid,
  input  [15:0] io_in_0_bits_0,
  input  [15:0] io_in_0_bits_1,
  input  [15:0] io_in_0_bits_2,
  input  [15:0] io_in_0_bits_3,
  input  [15:0] io_in_0_bits_4,
  input  [15:0] io_in_0_bits_5,
  input  [15:0] io_in_0_bits_6,
  input  [15:0] io_in_0_bits_7,
  output        io_in_1_ready,
  input         io_in_1_valid,
  input  [15:0] io_in_1_bits_0,
  input  [15:0] io_in_1_bits_1,
  input  [15:0] io_in_1_bits_2,
  input  [15:0] io_in_1_bits_3,
  input  [15:0] io_in_1_bits_4,
  input  [15:0] io_in_1_bits_5,
  input  [15:0] io_in_1_bits_6,
  input  [15:0] io_in_1_bits_7,
  output        io_sel_ready,
  input         io_sel_valid,
  input         io_sel_bits,
  input         io_out_ready,
  output        io_out_valid,
  output [15:0] io_out_bits_0,
  output [15:0] io_out_bits_1,
  output [15:0] io_out_bits_2,
  output [15:0] io_out_bits_3,
  output [15:0] io_out_bits_4,
  output [15:0] io_out_bits_5,
  output [15:0] io_out_bits_6,
  output [15:0] io_out_bits_7
);
  wire  _GEN_17 = io_sel_bits ? io_in_1_valid : io_in_0_valid; // @[Mux.scala 58:29 Mux.scala 58:29]
  assign io_in_0_ready = ~io_sel_bits & (io_sel_valid & io_out_ready); // @[Mux.scala 60:13 Mux.scala 60:13 Mux.scala 52:19]
  assign io_in_1_ready = io_sel_bits & (io_sel_valid & io_out_ready); // @[Mux.scala 60:13 Mux.scala 60:13 Mux.scala 52:19]
  assign io_sel_ready = _GEN_17 & io_out_ready; // @[Mux.scala 59:26]
  assign io_out_valid = io_sel_valid & _GEN_17; // @[Mux.scala 58:29]
  assign io_out_bits_0 = io_sel_bits ? $signed(io_in_1_bits_0) : $signed(io_in_0_bits_0); // @[Mux.scala 57:15 Mux.scala 57:15]
  assign io_out_bits_1 = io_sel_bits ? $signed(io_in_1_bits_1) : $signed(io_in_0_bits_1); // @[Mux.scala 57:15 Mux.scala 57:15]
  assign io_out_bits_2 = io_sel_bits ? $signed(io_in_1_bits_2) : $signed(io_in_0_bits_2); // @[Mux.scala 57:15 Mux.scala 57:15]
  assign io_out_bits_3 = io_sel_bits ? $signed(io_in_1_bits_3) : $signed(io_in_0_bits_3); // @[Mux.scala 57:15 Mux.scala 57:15]
  assign io_out_bits_4 = io_sel_bits ? $signed(io_in_1_bits_4) : $signed(io_in_0_bits_4); // @[Mux.scala 57:15 Mux.scala 57:15]
  assign io_out_bits_5 = io_sel_bits ? $signed(io_in_1_bits_5) : $signed(io_in_0_bits_5); // @[Mux.scala 57:15 Mux.scala 57:15]
  assign io_out_bits_6 = io_sel_bits ? $signed(io_in_1_bits_6) : $signed(io_in_0_bits_6); // @[Mux.scala 57:15 Mux.scala 57:15]
  assign io_out_bits_7 = io_sel_bits ? $signed(io_in_1_bits_7) : $signed(io_in_0_bits_7); // @[Mux.scala 57:15 Mux.scala 57:15]
endmodule
module AccumulatorWithALUArray(
  input         clock,
  input         reset,
  output        io_input_ready,
  input         io_input_valid,
  input  [15:0] io_input_bits_0,
  input  [15:0] io_input_bits_1,
  input  [15:0] io_input_bits_2,
  input  [15:0] io_input_bits_3,
  input  [15:0] io_input_bits_4,
  input  [15:0] io_input_bits_5,
  input  [15:0] io_input_bits_6,
  input  [15:0] io_input_bits_7,
  input         io_output_ready,
  output        io_output_valid,
  output [15:0] io_output_bits_0,
  output [15:0] io_output_bits_1,
  output [15:0] io_output_bits_2,
  output [15:0] io_output_bits_3,
  output [15:0] io_output_bits_4,
  output [15:0] io_output_bits_5,
  output [15:0] io_output_bits_6,
  output [15:0] io_output_bits_7,
  output        io_control_ready,
  input         io_control_valid,
  input  [3:0]  io_control_bits_instruction_op,
  input         io_control_bits_instruction_sourceLeft,
  input         io_control_bits_instruction_sourceRight,
  input         io_control_bits_instruction_dest,
  input  [5:0]  io_control_bits_readAddress,
  input  [5:0]  io_control_bits_writeAddress,
  input         io_control_bits_accumulate,
  input         io_control_bits_write,
  input         io_control_bits_read,
  input         io_tracepoint,
  input  [31:0] io_programCounter
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
`endif // RANDOMIZE_REG_INIT
  wire  acc_clock; // @[AccumulatorWithALUArray.scala 44:19]
  wire  acc_reset; // @[AccumulatorWithALUArray.scala 44:19]
  wire  acc_io_input_ready; // @[AccumulatorWithALUArray.scala 44:19]
  wire  acc_io_input_valid; // @[AccumulatorWithALUArray.scala 44:19]
  wire [15:0] acc_io_input_bits_0; // @[AccumulatorWithALUArray.scala 44:19]
  wire [15:0] acc_io_input_bits_1; // @[AccumulatorWithALUArray.scala 44:19]
  wire [15:0] acc_io_input_bits_2; // @[AccumulatorWithALUArray.scala 44:19]
  wire [15:0] acc_io_input_bits_3; // @[AccumulatorWithALUArray.scala 44:19]
  wire [15:0] acc_io_input_bits_4; // @[AccumulatorWithALUArray.scala 44:19]
  wire [15:0] acc_io_input_bits_5; // @[AccumulatorWithALUArray.scala 44:19]
  wire [15:0] acc_io_input_bits_6; // @[AccumulatorWithALUArray.scala 44:19]
  wire [15:0] acc_io_input_bits_7; // @[AccumulatorWithALUArray.scala 44:19]
  wire  acc_io_output_ready; // @[AccumulatorWithALUArray.scala 44:19]
  wire  acc_io_output_valid; // @[AccumulatorWithALUArray.scala 44:19]
  wire [15:0] acc_io_output_bits_0; // @[AccumulatorWithALUArray.scala 44:19]
  wire [15:0] acc_io_output_bits_1; // @[AccumulatorWithALUArray.scala 44:19]
  wire [15:0] acc_io_output_bits_2; // @[AccumulatorWithALUArray.scala 44:19]
  wire [15:0] acc_io_output_bits_3; // @[AccumulatorWithALUArray.scala 44:19]
  wire [15:0] acc_io_output_bits_4; // @[AccumulatorWithALUArray.scala 44:19]
  wire [15:0] acc_io_output_bits_5; // @[AccumulatorWithALUArray.scala 44:19]
  wire [15:0] acc_io_output_bits_6; // @[AccumulatorWithALUArray.scala 44:19]
  wire [15:0] acc_io_output_bits_7; // @[AccumulatorWithALUArray.scala 44:19]
  wire  acc_io_control_ready; // @[AccumulatorWithALUArray.scala 44:19]
  wire  acc_io_control_valid; // @[AccumulatorWithALUArray.scala 44:19]
  wire [5:0] acc_io_control_bits_address; // @[AccumulatorWithALUArray.scala 44:19]
  wire  acc_io_control_bits_accumulate; // @[AccumulatorWithALUArray.scala 44:19]
  wire  acc_io_control_bits_write; // @[AccumulatorWithALUArray.scala 44:19]
  wire  acc_io_tracepoint; // @[AccumulatorWithALUArray.scala 44:19]
  wire [31:0] acc_io_programCounter; // @[AccumulatorWithALUArray.scala 44:19]
  wire  alu_clock; // @[AccumulatorWithALUArray.scala 45:19]
  wire  alu_reset; // @[AccumulatorWithALUArray.scala 45:19]
  wire  alu_io_input_ready; // @[AccumulatorWithALUArray.scala 45:19]
  wire  alu_io_input_valid; // @[AccumulatorWithALUArray.scala 45:19]
  wire [15:0] alu_io_input_bits_0; // @[AccumulatorWithALUArray.scala 45:19]
  wire [15:0] alu_io_input_bits_1; // @[AccumulatorWithALUArray.scala 45:19]
  wire [15:0] alu_io_input_bits_2; // @[AccumulatorWithALUArray.scala 45:19]
  wire [15:0] alu_io_input_bits_3; // @[AccumulatorWithALUArray.scala 45:19]
  wire [15:0] alu_io_input_bits_4; // @[AccumulatorWithALUArray.scala 45:19]
  wire [15:0] alu_io_input_bits_5; // @[AccumulatorWithALUArray.scala 45:19]
  wire [15:0] alu_io_input_bits_6; // @[AccumulatorWithALUArray.scala 45:19]
  wire [15:0] alu_io_input_bits_7; // @[AccumulatorWithALUArray.scala 45:19]
  wire  alu_io_output_ready; // @[AccumulatorWithALUArray.scala 45:19]
  wire  alu_io_output_valid; // @[AccumulatorWithALUArray.scala 45:19]
  wire [15:0] alu_io_output_bits_0; // @[AccumulatorWithALUArray.scala 45:19]
  wire [15:0] alu_io_output_bits_1; // @[AccumulatorWithALUArray.scala 45:19]
  wire [15:0] alu_io_output_bits_2; // @[AccumulatorWithALUArray.scala 45:19]
  wire [15:0] alu_io_output_bits_3; // @[AccumulatorWithALUArray.scala 45:19]
  wire [15:0] alu_io_output_bits_4; // @[AccumulatorWithALUArray.scala 45:19]
  wire [15:0] alu_io_output_bits_5; // @[AccumulatorWithALUArray.scala 45:19]
  wire [15:0] alu_io_output_bits_6; // @[AccumulatorWithALUArray.scala 45:19]
  wire [15:0] alu_io_output_bits_7; // @[AccumulatorWithALUArray.scala 45:19]
  wire  alu_io_instruction_ready; // @[AccumulatorWithALUArray.scala 45:19]
  wire  alu_io_instruction_valid; // @[AccumulatorWithALUArray.scala 45:19]
  wire [3:0] alu_io_instruction_bits_op; // @[AccumulatorWithALUArray.scala 45:19]
  wire  alu_io_instruction_bits_sourceLeft; // @[AccumulatorWithALUArray.scala 45:19]
  wire  alu_io_instruction_bits_sourceRight; // @[AccumulatorWithALUArray.scala 45:19]
  wire  alu_io_instruction_bits_dest; // @[AccumulatorWithALUArray.scala 45:19]
  wire  aluOutputDemux_x6_demux_io_in_ready; // @[Demux.scala 46:23]
  wire  aluOutputDemux_x6_demux_io_in_valid; // @[Demux.scala 46:23]
  wire [15:0] aluOutputDemux_x6_demux_io_in_bits_0; // @[Demux.scala 46:23]
  wire [15:0] aluOutputDemux_x6_demux_io_in_bits_1; // @[Demux.scala 46:23]
  wire [15:0] aluOutputDemux_x6_demux_io_in_bits_2; // @[Demux.scala 46:23]
  wire [15:0] aluOutputDemux_x6_demux_io_in_bits_3; // @[Demux.scala 46:23]
  wire [15:0] aluOutputDemux_x6_demux_io_in_bits_4; // @[Demux.scala 46:23]
  wire [15:0] aluOutputDemux_x6_demux_io_in_bits_5; // @[Demux.scala 46:23]
  wire [15:0] aluOutputDemux_x6_demux_io_in_bits_6; // @[Demux.scala 46:23]
  wire [15:0] aluOutputDemux_x6_demux_io_in_bits_7; // @[Demux.scala 46:23]
  wire  aluOutputDemux_x6_demux_io_sel_ready; // @[Demux.scala 46:23]
  wire  aluOutputDemux_x6_demux_io_sel_valid; // @[Demux.scala 46:23]
  wire  aluOutputDemux_x6_demux_io_sel_bits; // @[Demux.scala 46:23]
  wire  aluOutputDemux_x6_demux_io_out_0_ready; // @[Demux.scala 46:23]
  wire  aluOutputDemux_x6_demux_io_out_0_valid; // @[Demux.scala 46:23]
  wire [15:0] aluOutputDemux_x6_demux_io_out_0_bits_0; // @[Demux.scala 46:23]
  wire [15:0] aluOutputDemux_x6_demux_io_out_0_bits_1; // @[Demux.scala 46:23]
  wire [15:0] aluOutputDemux_x6_demux_io_out_0_bits_2; // @[Demux.scala 46:23]
  wire [15:0] aluOutputDemux_x6_demux_io_out_0_bits_3; // @[Demux.scala 46:23]
  wire [15:0] aluOutputDemux_x6_demux_io_out_0_bits_4; // @[Demux.scala 46:23]
  wire [15:0] aluOutputDemux_x6_demux_io_out_0_bits_5; // @[Demux.scala 46:23]
  wire [15:0] aluOutputDemux_x6_demux_io_out_0_bits_6; // @[Demux.scala 46:23]
  wire [15:0] aluOutputDemux_x6_demux_io_out_0_bits_7; // @[Demux.scala 46:23]
  wire  aluOutputDemux_x6_demux_io_out_1_ready; // @[Demux.scala 46:23]
  wire  aluOutputDemux_x6_demux_io_out_1_valid; // @[Demux.scala 46:23]
  wire [15:0] aluOutputDemux_x6_demux_io_out_1_bits_0; // @[Demux.scala 46:23]
  wire [15:0] aluOutputDemux_x6_demux_io_out_1_bits_1; // @[Demux.scala 46:23]
  wire [15:0] aluOutputDemux_x6_demux_io_out_1_bits_2; // @[Demux.scala 46:23]
  wire [15:0] aluOutputDemux_x6_demux_io_out_1_bits_3; // @[Demux.scala 46:23]
  wire [15:0] aluOutputDemux_x6_demux_io_out_1_bits_4; // @[Demux.scala 46:23]
  wire [15:0] aluOutputDemux_x6_demux_io_out_1_bits_5; // @[Demux.scala 46:23]
  wire [15:0] aluOutputDemux_x6_demux_io_out_1_bits_6; // @[Demux.scala 46:23]
  wire [15:0] aluOutputDemux_x6_demux_io_out_1_bits_7; // @[Demux.scala 46:23]
  wire  aluOutputDemux_clock; // @[Mem.scala 23:19]
  wire  aluOutputDemux_reset; // @[Mem.scala 23:19]
  wire  aluOutputDemux_io_enq_ready; // @[Mem.scala 23:19]
  wire  aluOutputDemux_io_enq_valid; // @[Mem.scala 23:19]
  wire  aluOutputDemux_io_enq_bits; // @[Mem.scala 23:19]
  wire  aluOutputDemux_io_deq_ready; // @[Mem.scala 23:19]
  wire  aluOutputDemux_io_deq_valid; // @[Mem.scala 23:19]
  wire  aluOutputDemux_io_deq_bits; // @[Mem.scala 23:19]
  wire  accInputMux_x15_mux_io_in_0_ready; // @[Mux.scala 71:21]
  wire  accInputMux_x15_mux_io_in_0_valid; // @[Mux.scala 71:21]
  wire [15:0] accInputMux_x15_mux_io_in_0_bits_0; // @[Mux.scala 71:21]
  wire [15:0] accInputMux_x15_mux_io_in_0_bits_1; // @[Mux.scala 71:21]
  wire [15:0] accInputMux_x15_mux_io_in_0_bits_2; // @[Mux.scala 71:21]
  wire [15:0] accInputMux_x15_mux_io_in_0_bits_3; // @[Mux.scala 71:21]
  wire [15:0] accInputMux_x15_mux_io_in_0_bits_4; // @[Mux.scala 71:21]
  wire [15:0] accInputMux_x15_mux_io_in_0_bits_5; // @[Mux.scala 71:21]
  wire [15:0] accInputMux_x15_mux_io_in_0_bits_6; // @[Mux.scala 71:21]
  wire [15:0] accInputMux_x15_mux_io_in_0_bits_7; // @[Mux.scala 71:21]
  wire  accInputMux_x15_mux_io_in_1_ready; // @[Mux.scala 71:21]
  wire  accInputMux_x15_mux_io_in_1_valid; // @[Mux.scala 71:21]
  wire [15:0] accInputMux_x15_mux_io_in_1_bits_0; // @[Mux.scala 71:21]
  wire [15:0] accInputMux_x15_mux_io_in_1_bits_1; // @[Mux.scala 71:21]
  wire [15:0] accInputMux_x15_mux_io_in_1_bits_2; // @[Mux.scala 71:21]
  wire [15:0] accInputMux_x15_mux_io_in_1_bits_3; // @[Mux.scala 71:21]
  wire [15:0] accInputMux_x15_mux_io_in_1_bits_4; // @[Mux.scala 71:21]
  wire [15:0] accInputMux_x15_mux_io_in_1_bits_5; // @[Mux.scala 71:21]
  wire [15:0] accInputMux_x15_mux_io_in_1_bits_6; // @[Mux.scala 71:21]
  wire [15:0] accInputMux_x15_mux_io_in_1_bits_7; // @[Mux.scala 71:21]
  wire  accInputMux_x15_mux_io_sel_ready; // @[Mux.scala 71:21]
  wire  accInputMux_x15_mux_io_sel_valid; // @[Mux.scala 71:21]
  wire  accInputMux_x15_mux_io_sel_bits; // @[Mux.scala 71:21]
  wire  accInputMux_x15_mux_io_out_ready; // @[Mux.scala 71:21]
  wire  accInputMux_x15_mux_io_out_valid; // @[Mux.scala 71:21]
  wire [15:0] accInputMux_x15_mux_io_out_bits_0; // @[Mux.scala 71:21]
  wire [15:0] accInputMux_x15_mux_io_out_bits_1; // @[Mux.scala 71:21]
  wire [15:0] accInputMux_x15_mux_io_out_bits_2; // @[Mux.scala 71:21]
  wire [15:0] accInputMux_x15_mux_io_out_bits_3; // @[Mux.scala 71:21]
  wire [15:0] accInputMux_x15_mux_io_out_bits_4; // @[Mux.scala 71:21]
  wire [15:0] accInputMux_x15_mux_io_out_bits_5; // @[Mux.scala 71:21]
  wire [15:0] accInputMux_x15_mux_io_out_bits_6; // @[Mux.scala 71:21]
  wire [15:0] accInputMux_x15_mux_io_out_bits_7; // @[Mux.scala 71:21]
  wire  accInputMux_clock; // @[Mem.scala 23:19]
  wire  accInputMux_reset; // @[Mem.scala 23:19]
  wire  accInputMux_io_enq_ready; // @[Mem.scala 23:19]
  wire  accInputMux_io_enq_valid; // @[Mem.scala 23:19]
  wire  accInputMux_io_enq_bits; // @[Mem.scala 23:19]
  wire  accInputMux_io_deq_ready; // @[Mem.scala 23:19]
  wire  accInputMux_io_deq_valid; // @[Mem.scala 23:19]
  wire  accInputMux_io_deq_bits; // @[Mem.scala 23:19]
  wire  accOutputDemux_x24_demux_io_in_ready; // @[Demux.scala 46:23]
  wire  accOutputDemux_x24_demux_io_in_valid; // @[Demux.scala 46:23]
  wire [15:0] accOutputDemux_x24_demux_io_in_bits_0; // @[Demux.scala 46:23]
  wire [15:0] accOutputDemux_x24_demux_io_in_bits_1; // @[Demux.scala 46:23]
  wire [15:0] accOutputDemux_x24_demux_io_in_bits_2; // @[Demux.scala 46:23]
  wire [15:0] accOutputDemux_x24_demux_io_in_bits_3; // @[Demux.scala 46:23]
  wire [15:0] accOutputDemux_x24_demux_io_in_bits_4; // @[Demux.scala 46:23]
  wire [15:0] accOutputDemux_x24_demux_io_in_bits_5; // @[Demux.scala 46:23]
  wire [15:0] accOutputDemux_x24_demux_io_in_bits_6; // @[Demux.scala 46:23]
  wire [15:0] accOutputDemux_x24_demux_io_in_bits_7; // @[Demux.scala 46:23]
  wire  accOutputDemux_x24_demux_io_sel_ready; // @[Demux.scala 46:23]
  wire  accOutputDemux_x24_demux_io_sel_valid; // @[Demux.scala 46:23]
  wire  accOutputDemux_x24_demux_io_sel_bits; // @[Demux.scala 46:23]
  wire  accOutputDemux_x24_demux_io_out_0_ready; // @[Demux.scala 46:23]
  wire  accOutputDemux_x24_demux_io_out_0_valid; // @[Demux.scala 46:23]
  wire [15:0] accOutputDemux_x24_demux_io_out_0_bits_0; // @[Demux.scala 46:23]
  wire [15:0] accOutputDemux_x24_demux_io_out_0_bits_1; // @[Demux.scala 46:23]
  wire [15:0] accOutputDemux_x24_demux_io_out_0_bits_2; // @[Demux.scala 46:23]
  wire [15:0] accOutputDemux_x24_demux_io_out_0_bits_3; // @[Demux.scala 46:23]
  wire [15:0] accOutputDemux_x24_demux_io_out_0_bits_4; // @[Demux.scala 46:23]
  wire [15:0] accOutputDemux_x24_demux_io_out_0_bits_5; // @[Demux.scala 46:23]
  wire [15:0] accOutputDemux_x24_demux_io_out_0_bits_6; // @[Demux.scala 46:23]
  wire [15:0] accOutputDemux_x24_demux_io_out_0_bits_7; // @[Demux.scala 46:23]
  wire  accOutputDemux_x24_demux_io_out_1_ready; // @[Demux.scala 46:23]
  wire  accOutputDemux_x24_demux_io_out_1_valid; // @[Demux.scala 46:23]
  wire [15:0] accOutputDemux_x24_demux_io_out_1_bits_0; // @[Demux.scala 46:23]
  wire [15:0] accOutputDemux_x24_demux_io_out_1_bits_1; // @[Demux.scala 46:23]
  wire [15:0] accOutputDemux_x24_demux_io_out_1_bits_2; // @[Demux.scala 46:23]
  wire [15:0] accOutputDemux_x24_demux_io_out_1_bits_3; // @[Demux.scala 46:23]
  wire [15:0] accOutputDemux_x24_demux_io_out_1_bits_4; // @[Demux.scala 46:23]
  wire [15:0] accOutputDemux_x24_demux_io_out_1_bits_5; // @[Demux.scala 46:23]
  wire [15:0] accOutputDemux_x24_demux_io_out_1_bits_6; // @[Demux.scala 46:23]
  wire [15:0] accOutputDemux_x24_demux_io_out_1_bits_7; // @[Demux.scala 46:23]
  wire  accOutputDemux_clock; // @[Mem.scala 23:19]
  wire  accOutputDemux_reset; // @[Mem.scala 23:19]
  wire  accOutputDemux_io_enq_ready; // @[Mem.scala 23:19]
  wire  accOutputDemux_io_enq_valid; // @[Mem.scala 23:19]
  wire  accOutputDemux_io_enq_bits; // @[Mem.scala 23:19]
  wire  accOutputDemux_io_deq_ready; // @[Mem.scala 23:19]
  wire  accOutputDemux_io_deq_valid; // @[Mem.scala 23:19]
  wire  accOutputDemux_io_deq_bits; // @[Mem.scala 23:19]
  wire  accWriteEnqueuer_clock; // @[MultiEnqueue.scala 160:43]
  wire  accWriteEnqueuer_reset; // @[MultiEnqueue.scala 160:43]
  wire  accWriteEnqueuer_io_in_ready; // @[MultiEnqueue.scala 160:43]
  wire  accWriteEnqueuer_io_in_valid; // @[MultiEnqueue.scala 160:43]
  wire  accWriteEnqueuer_io_out_0_ready; // @[MultiEnqueue.scala 160:43]
  wire  accWriteEnqueuer_io_out_0_valid; // @[MultiEnqueue.scala 160:43]
  wire  accWriteEnqueuer_io_out_1_ready; // @[MultiEnqueue.scala 160:43]
  wire  accWriteEnqueuer_io_out_1_valid; // @[MultiEnqueue.scala 160:43]
  wire  accReadEnqueuer_clock; // @[MultiEnqueue.scala 160:43]
  wire  accReadEnqueuer_reset; // @[MultiEnqueue.scala 160:43]
  wire  accReadEnqueuer_io_in_ready; // @[MultiEnqueue.scala 160:43]
  wire  accReadEnqueuer_io_in_valid; // @[MultiEnqueue.scala 160:43]
  wire  accReadEnqueuer_io_out_0_ready; // @[MultiEnqueue.scala 160:43]
  wire  accReadEnqueuer_io_out_0_valid; // @[MultiEnqueue.scala 160:43]
  wire  accReadEnqueuer_io_out_1_ready; // @[MultiEnqueue.scala 160:43]
  wire  accReadEnqueuer_io_out_1_valid; // @[MultiEnqueue.scala 160:43]
  wire  simdRWWriteEnqueuer_clock; // @[MultiEnqueue.scala 160:43]
  wire  simdRWWriteEnqueuer_reset; // @[MultiEnqueue.scala 160:43]
  wire  simdRWWriteEnqueuer_io_in_ready; // @[MultiEnqueue.scala 160:43]
  wire  simdRWWriteEnqueuer_io_in_valid; // @[MultiEnqueue.scala 160:43]
  wire  simdRWWriteEnqueuer_io_out_0_ready; // @[MultiEnqueue.scala 160:43]
  wire  simdRWWriteEnqueuer_io_out_0_valid; // @[MultiEnqueue.scala 160:43]
  wire  simdRWWriteEnqueuer_io_out_1_ready; // @[MultiEnqueue.scala 160:43]
  wire  simdRWWriteEnqueuer_io_out_1_valid; // @[MultiEnqueue.scala 160:43]
  wire  simdRWWriteEnqueuer_io_out_2_ready; // @[MultiEnqueue.scala 160:43]
  wire  simdRWWriteEnqueuer_io_out_2_valid; // @[MultiEnqueue.scala 160:43]
  wire  simdRWReadEnqueuer_clock; // @[MultiEnqueue.scala 160:43]
  wire  simdRWReadEnqueuer_reset; // @[MultiEnqueue.scala 160:43]
  wire  simdRWReadEnqueuer_io_in_ready; // @[MultiEnqueue.scala 160:43]
  wire  simdRWReadEnqueuer_io_in_valid; // @[MultiEnqueue.scala 160:43]
  wire  simdRWReadEnqueuer_io_out_0_ready; // @[MultiEnqueue.scala 160:43]
  wire  simdRWReadEnqueuer_io_out_0_valid; // @[MultiEnqueue.scala 160:43]
  wire  simdRWReadEnqueuer_io_out_1_ready; // @[MultiEnqueue.scala 160:43]
  wire  simdRWReadEnqueuer_io_out_1_valid; // @[MultiEnqueue.scala 160:43]
  wire  simdRWReadEnqueuer_io_out_2_ready; // @[MultiEnqueue.scala 160:43]
  wire  simdRWReadEnqueuer_io_out_2_valid; // @[MultiEnqueue.scala 160:43]
  wire  simdWriteEnqueuer_clock; // @[MultiEnqueue.scala 160:43]
  wire  simdWriteEnqueuer_reset; // @[MultiEnqueue.scala 160:43]
  wire  simdWriteEnqueuer_io_in_ready; // @[MultiEnqueue.scala 160:43]
  wire  simdWriteEnqueuer_io_in_valid; // @[MultiEnqueue.scala 160:43]
  wire  simdWriteEnqueuer_io_out_0_ready; // @[MultiEnqueue.scala 160:43]
  wire  simdWriteEnqueuer_io_out_0_valid; // @[MultiEnqueue.scala 160:43]
  wire  simdWriteEnqueuer_io_out_1_ready; // @[MultiEnqueue.scala 160:43]
  wire  simdWriteEnqueuer_io_out_1_valid; // @[MultiEnqueue.scala 160:43]
  wire  simdWriteEnqueuer_io_out_2_ready; // @[MultiEnqueue.scala 160:43]
  wire  simdWriteEnqueuer_io_out_2_valid; // @[MultiEnqueue.scala 160:43]
  wire  simdWriteEnqueuer_io_out_3_ready; // @[MultiEnqueue.scala 160:43]
  wire  simdWriteEnqueuer_io_out_3_valid; // @[MultiEnqueue.scala 160:43]
  wire  simdReadEnqueuer_clock; // @[MultiEnqueue.scala 160:43]
  wire  simdReadEnqueuer_reset; // @[MultiEnqueue.scala 160:43]
  wire  simdReadEnqueuer_io_in_ready; // @[MultiEnqueue.scala 160:43]
  wire  simdReadEnqueuer_io_in_valid; // @[MultiEnqueue.scala 160:43]
  wire  simdReadEnqueuer_io_out_0_ready; // @[MultiEnqueue.scala 160:43]
  wire  simdReadEnqueuer_io_out_0_valid; // @[MultiEnqueue.scala 160:43]
  wire  simdReadEnqueuer_io_out_1_ready; // @[MultiEnqueue.scala 160:43]
  wire  simdReadEnqueuer_io_out_1_valid; // @[MultiEnqueue.scala 160:43]
  wire  simdReadEnqueuer_io_out_2_ready; // @[MultiEnqueue.scala 160:43]
  wire  simdReadEnqueuer_io_out_2_valid; // @[MultiEnqueue.scala 160:43]
  wire  simdReadEnqueuer_io_out_3_ready; // @[MultiEnqueue.scala 160:43]
  wire  simdReadEnqueuer_io_out_3_valid; // @[MultiEnqueue.scala 160:43]
  wire  simdEnqueuer_clock; // @[MultiEnqueue.scala 160:43]
  wire  simdEnqueuer_reset; // @[MultiEnqueue.scala 160:43]
  wire  simdEnqueuer_io_in_ready; // @[MultiEnqueue.scala 160:43]
  wire  simdEnqueuer_io_in_valid; // @[MultiEnqueue.scala 160:43]
  wire  simdEnqueuer_io_out_0_ready; // @[MultiEnqueue.scala 160:43]
  wire  simdEnqueuer_io_out_0_valid; // @[MultiEnqueue.scala 160:43]
  wire  simdEnqueuer_io_out_1_ready; // @[MultiEnqueue.scala 160:43]
  wire  simdEnqueuer_io_out_1_valid; // @[MultiEnqueue.scala 160:43]
  wire  _io_wrote_valid_T = io_control_bits_instruction_op == 4'h0; // @[AccumulatorWithALUArray.scala 58:33]
  reg  readEnqueued; // @[AccumulatorWithALUArray.scala 110:29]
  wire  _GEN_10 = readEnqueued & accWriteEnqueuer_io_in_ready; // @[AccumulatorWithALUArray.scala 137:28 AccumulatorWithALUArray.scala 138:25 AccumulatorWithALUArray.scala 151:25]
  wire  _GEN_26 = io_control_bits_write ? _GEN_10 : accReadEnqueuer_io_in_ready; // @[AccumulatorWithALUArray.scala 136:32 AccumulatorWithALUArray.scala 161:23]
  wire  _GEN_42 = io_control_bits_write ? accWriteEnqueuer_io_in_ready : 1'h1; // @[AccumulatorWithALUArray.scala 170:32 AccumulatorWithALUArray.scala 171:23 AccumulatorWithALUArray.scala 179:23]
  wire  dataPathReady = io_control_bits_read ? _GEN_26 : _GEN_42; // @[AccumulatorWithALUArray.scala 135:29]
  wire  _GEN_0 = dataPathReady ? 1'h0 : readEnqueued; // @[AccumulatorWithALUArray.scala 145:31 AccumulatorWithALUArray.scala 146:26 AccumulatorWithALUArray.scala 148:26]
  wire  _GEN_1 = readEnqueued & io_control_valid; // @[AccumulatorWithALUArray.scala 137:28 MultiEnqueue.scala 84:17 MultiEnqueue.scala 40:17]
  wire  dataPathReady_acc_io_control_w_ready = acc_io_control_ready; // @[ReadyValid.scala 16:17 MultiEnqueue.scala 85:10]
  wire  _GEN_2 = readEnqueued & dataPathReady_acc_io_control_w_ready; // @[AccumulatorWithALUArray.scala 137:28 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire  _GEN_4 = readEnqueued & io_control_bits_accumulate; // @[AccumulatorWithALUArray.scala 137:28 MultiEnqueue.scala 85:10 MultiEnqueue.scala 85:10]
  wire [5:0] _GEN_5 = readEnqueued ? io_control_bits_writeAddress : io_control_bits_readAddress; // @[AccumulatorWithALUArray.scala 137:28 MultiEnqueue.scala 85:10 MultiEnqueue.scala 85:10]
  wire  dataPathReady_acc_io_control_w_valid = accWriteEnqueuer_io_out_0_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  readEnqueued_acc_io_control_w_valid = accReadEnqueuer_io_out_0_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_6 = readEnqueued ? dataPathReady_acc_io_control_w_valid : readEnqueued_acc_io_control_w_valid; // @[AccumulatorWithALUArray.scala 137:28 MultiEnqueue.scala 85:10 MultiEnqueue.scala 85:10]
  wire  dataPathReady_accInputMux_io_enq_w_ready = accInputMux_io_enq_ready; // @[ReadyValid.scala 16:17 MultiEnqueue.scala 86:10]
  wire  _GEN_7 = readEnqueued & dataPathReady_accInputMux_io_enq_w_ready; // @[AccumulatorWithALUArray.scala 137:28 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire  dataPathReady_accInputMux_io_enq_w_valid = accWriteEnqueuer_io_out_1_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_9 = readEnqueued & dataPathReady_accInputMux_io_enq_w_valid; // @[AccumulatorWithALUArray.scala 137:28 MultiEnqueue.scala 86:10 package.scala 410:15]
  wire  _GEN_11 = readEnqueued ? _GEN_0 : accReadEnqueuer_io_in_ready; // @[AccumulatorWithALUArray.scala 137:28 AccumulatorWithALUArray.scala 152:24]
  wire  _GEN_12 = readEnqueued ? 1'h0 : io_control_valid; // @[AccumulatorWithALUArray.scala 137:28 MultiEnqueue.scala 40:17 MultiEnqueue.scala 84:17]
  wire  _GEN_13 = readEnqueued ? 1'h0 : dataPathReady_acc_io_control_w_ready; // @[AccumulatorWithALUArray.scala 137:28 MultiEnqueue.scala 42:18 ReadyValid.scala 19:11]
  wire  readEnqueued_accOutputDemux_io_enq_w_ready = accOutputDemux_io_enq_ready; // @[ReadyValid.scala 16:17 MultiEnqueue.scala 86:10]
  wire  _GEN_14 = readEnqueued ? 1'h0 : readEnqueued_accOutputDemux_io_enq_w_ready; // @[AccumulatorWithALUArray.scala 137:28 MultiEnqueue.scala 42:18 ReadyValid.scala 19:11]
  wire  readEnqueued_accOutputDemux_io_enq_w_valid = accReadEnqueuer_io_out_1_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_16 = readEnqueued ? 1'h0 : readEnqueued_accOutputDemux_io_enq_w_valid; // @[AccumulatorWithALUArray.scala 137:28 package.scala 410:15 MultiEnqueue.scala 86:10]
  wire  _GEN_17 = io_control_bits_write & _GEN_1; // @[AccumulatorWithALUArray.scala 136:32 MultiEnqueue.scala 40:17]
  wire  _GEN_18 = io_control_bits_write & _GEN_2; // @[AccumulatorWithALUArray.scala 136:32 MultiEnqueue.scala 42:18]
  wire  _GEN_19 = io_control_bits_write & readEnqueued; // @[AccumulatorWithALUArray.scala 136:32 MultiEnqueue.scala 85:10]
  wire  _GEN_20 = io_control_bits_write & _GEN_4; // @[AccumulatorWithALUArray.scala 136:32 MultiEnqueue.scala 85:10]
  wire [5:0] _GEN_21 = io_control_bits_write ? _GEN_5 : io_control_bits_readAddress; // @[AccumulatorWithALUArray.scala 136:32 MultiEnqueue.scala 85:10]
  wire  _GEN_22 = io_control_bits_write ? _GEN_6 : readEnqueued_acc_io_control_w_valid; // @[AccumulatorWithALUArray.scala 136:32 MultiEnqueue.scala 85:10]
  wire  _GEN_23 = io_control_bits_write & _GEN_7; // @[AccumulatorWithALUArray.scala 136:32 MultiEnqueue.scala 42:18]
  wire  _GEN_25 = io_control_bits_write & _GEN_9; // @[AccumulatorWithALUArray.scala 136:32 package.scala 410:15]
  wire  _GEN_27 = io_control_bits_write & _GEN_11; // @[AccumulatorWithALUArray.scala 136:32 AccumulatorWithALUArray.scala 111:16]
  wire  _GEN_28 = io_control_bits_write ? _GEN_12 : io_control_valid; // @[AccumulatorWithALUArray.scala 136:32 MultiEnqueue.scala 84:17]
  wire  _GEN_29 = io_control_bits_write ? _GEN_13 : dataPathReady_acc_io_control_w_ready; // @[AccumulatorWithALUArray.scala 136:32 ReadyValid.scala 19:11]
  wire  _GEN_30 = io_control_bits_write ? _GEN_14 : readEnqueued_accOutputDemux_io_enq_w_ready; // @[AccumulatorWithALUArray.scala 136:32 ReadyValid.scala 19:11]
  wire  _GEN_32 = io_control_bits_write ? _GEN_16 : readEnqueued_accOutputDemux_io_enq_w_valid; // @[AccumulatorWithALUArray.scala 136:32 MultiEnqueue.scala 86:10]
  wire  _GEN_33 = io_control_bits_write & io_control_valid; // @[AccumulatorWithALUArray.scala 170:32 MultiEnqueue.scala 84:17 MultiEnqueue.scala 40:17]
  wire  _GEN_34 = io_control_bits_write & dataPathReady_acc_io_control_w_ready; // @[AccumulatorWithALUArray.scala 170:32 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire  _GEN_36 = io_control_bits_write & io_control_bits_accumulate; // @[AccumulatorWithALUArray.scala 170:32 MultiEnqueue.scala 85:10 package.scala 409:14]
  wire [5:0] _GEN_37 = io_control_bits_write ? io_control_bits_writeAddress : 6'h0; // @[AccumulatorWithALUArray.scala 170:32 MultiEnqueue.scala 85:10 package.scala 409:14]
  wire  _GEN_38 = io_control_bits_write & dataPathReady_acc_io_control_w_valid; // @[AccumulatorWithALUArray.scala 170:32 MultiEnqueue.scala 85:10 package.scala 410:15]
  wire  _GEN_39 = io_control_bits_write & dataPathReady_accInputMux_io_enq_w_ready; // @[AccumulatorWithALUArray.scala 170:32 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire  _GEN_41 = io_control_bits_write & dataPathReady_accInputMux_io_enq_w_valid; // @[AccumulatorWithALUArray.scala 170:32 MultiEnqueue.scala 86:10 package.scala 410:15]
  wire  _GEN_43 = io_control_bits_read ? _GEN_17 : _GEN_33; // @[AccumulatorWithALUArray.scala 135:29]
  wire  _GEN_44 = io_control_bits_read ? _GEN_18 : _GEN_34; // @[AccumulatorWithALUArray.scala 135:29]
  wire  _GEN_45 = io_control_bits_read ? _GEN_19 : io_control_bits_write; // @[AccumulatorWithALUArray.scala 135:29]
  wire  _GEN_46 = io_control_bits_read ? _GEN_20 : _GEN_36; // @[AccumulatorWithALUArray.scala 135:29]
  wire [5:0] _GEN_47 = io_control_bits_read ? _GEN_21 : _GEN_37; // @[AccumulatorWithALUArray.scala 135:29]
  wire  _GEN_48 = io_control_bits_read ? _GEN_22 : _GEN_38; // @[AccumulatorWithALUArray.scala 135:29]
  wire  _GEN_49 = io_control_bits_read ? _GEN_23 : _GEN_39; // @[AccumulatorWithALUArray.scala 135:29]
  wire  _GEN_51 = io_control_bits_read ? _GEN_25 : _GEN_41; // @[AccumulatorWithALUArray.scala 135:29]
  wire  _GEN_53 = io_control_bits_read & _GEN_27; // @[AccumulatorWithALUArray.scala 135:29 AccumulatorWithALUArray.scala 111:16]
  wire  _GEN_54 = io_control_bits_read & _GEN_28; // @[AccumulatorWithALUArray.scala 135:29 MultiEnqueue.scala 40:17]
  wire  _GEN_55 = io_control_bits_read & _GEN_29; // @[AccumulatorWithALUArray.scala 135:29 MultiEnqueue.scala 42:18]
  wire  _GEN_56 = io_control_bits_read & _GEN_30; // @[AccumulatorWithALUArray.scala 135:29 MultiEnqueue.scala 42:18]
  wire  _GEN_58 = io_control_bits_read & _GEN_32; // @[AccumulatorWithALUArray.scala 135:29 package.scala 410:15]
  wire  _GEN_87 = readEnqueued & simdRWWriteEnqueuer_io_in_ready; // @[AccumulatorWithALUArray.scala 188:28 AccumulatorWithALUArray.scala 189:25 AccumulatorWithALUArray.scala 204:25]
  wire  _GEN_111 = io_control_bits_write ? _GEN_87 : simdReadEnqueuer_io_in_ready; // @[AccumulatorWithALUArray.scala 186:32 AccumulatorWithALUArray.scala 222:23]
  wire  _GEN_146 = io_control_bits_write ? simdWriteEnqueuer_io_in_ready : simdEnqueuer_io_in_ready; // @[AccumulatorWithALUArray.scala 235:32 AccumulatorWithALUArray.scala 236:23 AccumulatorWithALUArray.scala 248:23]
  wire  dataPathReady_1 = io_control_bits_read ? _GEN_111 : _GEN_146; // @[AccumulatorWithALUArray.scala 185:29]
  wire  _GEN_59 = dataPathReady_1 ? 1'h0 : readEnqueued; // @[AccumulatorWithALUArray.scala 198:31 AccumulatorWithALUArray.scala 199:26 AccumulatorWithALUArray.scala 201:26]
  wire  _T_2 = ~io_control_bits_instruction_sourceLeft | ~io_control_bits_instruction_sourceRight; // @[AccumulatorWithALUArray.scala 206:57]
  wire  _GEN_60 = _T_2 & io_control_valid; // @[AccumulatorWithALUArray.scala 207:13 MultiEnqueue.scala 114:17 MultiEnqueue.scala 40:17]
  wire  _GEN_61 = _T_2 & dataPathReady_acc_io_control_w_ready; // @[AccumulatorWithALUArray.scala 207:13 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire [5:0] _GEN_64 = _T_2 ? io_control_bits_readAddress : 6'h0; // @[AccumulatorWithALUArray.scala 207:13 MultiEnqueue.scala 115:10 package.scala 409:14]
  wire  readEnqueued_acc_io_control_w_1_valid = simdRWReadEnqueuer_io_out_0_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_65 = _T_2 & readEnqueued_acc_io_control_w_1_valid; // @[AccumulatorWithALUArray.scala 207:13 MultiEnqueue.scala 115:10 package.scala 410:15]
  wire  _GEN_66 = _T_2 & readEnqueued_accOutputDemux_io_enq_w_ready; // @[AccumulatorWithALUArray.scala 207:13 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire  readEnqueued_accOutputDemux_io_enq_w_1_valid = simdRWReadEnqueuer_io_out_1_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_68 = _T_2 & readEnqueued_accOutputDemux_io_enq_w_1_valid; // @[AccumulatorWithALUArray.scala 207:13 MultiEnqueue.scala 116:10 package.scala 410:15]
  wire  readEnqueued_alu_io_instruction_w_ready = alu_io_instruction_ready; // @[ReadyValid.scala 16:17 MultiEnqueue.scala 117:10]
  wire  _GEN_69 = _T_2 & readEnqueued_alu_io_instruction_w_ready; // @[AccumulatorWithALUArray.scala 207:13 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire  _GEN_70 = _T_2 & io_control_bits_instruction_dest; // @[AccumulatorWithALUArray.scala 207:13 MultiEnqueue.scala 117:10 package.scala 409:14]
  wire  _GEN_71 = _T_2 & io_control_bits_instruction_sourceRight; // @[AccumulatorWithALUArray.scala 207:13 MultiEnqueue.scala 117:10 package.scala 409:14]
  wire  _GEN_72 = _T_2 & io_control_bits_instruction_sourceLeft; // @[AccumulatorWithALUArray.scala 207:13 MultiEnqueue.scala 117:10 package.scala 409:14]
  wire [3:0] _GEN_73 = _T_2 ? io_control_bits_instruction_op : 4'h0; // @[AccumulatorWithALUArray.scala 207:13 MultiEnqueue.scala 117:10 package.scala 409:14]
  wire  readEnqueued_alu_io_instruction_w_valid = simdRWReadEnqueuer_io_out_2_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_74 = _T_2 & readEnqueued_alu_io_instruction_w_valid; // @[AccumulatorWithALUArray.scala 207:13 MultiEnqueue.scala 117:10 package.scala 410:15]
  wire  _GEN_75 = _T_2 ? simdRWReadEnqueuer_io_in_ready : 1'h1; // @[AccumulatorWithALUArray.scala 207:13 AccumulatorWithALUArray.scala 208:26 AccumulatorWithALUArray.scala 218:26]
  wire [5:0] _GEN_79 = readEnqueued ? io_control_bits_writeAddress : _GEN_64; // @[AccumulatorWithALUArray.scala 188:28 MultiEnqueue.scala 115:10]
  wire  dataPathReady_acc_io_control_w_3_valid = simdRWWriteEnqueuer_io_out_0_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_80 = readEnqueued ? dataPathReady_acc_io_control_w_3_valid : _GEN_65; // @[AccumulatorWithALUArray.scala 188:28 MultiEnqueue.scala 115:10]
  wire  dataPathReady_aluOutputDemux_io_enq_w_ready = aluOutputDemux_io_enq_ready; // @[ReadyValid.scala 16:17 MultiEnqueue.scala 116:10]
  wire  _GEN_81 = readEnqueued & dataPathReady_aluOutputDemux_io_enq_w_ready; // @[AccumulatorWithALUArray.scala 188:28 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire  dataPathReady_aluOutputDemux_io_enq_w_valid = simdRWWriteEnqueuer_io_out_1_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_83 = readEnqueued & dataPathReady_aluOutputDemux_io_enq_w_valid; // @[AccumulatorWithALUArray.scala 188:28 MultiEnqueue.scala 116:10 package.scala 410:15]
  wire  dataPathReady_accInputMux_io_enq_w_2_valid = simdRWWriteEnqueuer_io_out_2_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_86 = readEnqueued & dataPathReady_accInputMux_io_enq_w_2_valid; // @[AccumulatorWithALUArray.scala 188:28 MultiEnqueue.scala 117:10 package.scala 410:15]
  wire  _GEN_88 = readEnqueued ? _GEN_59 : _GEN_75; // @[AccumulatorWithALUArray.scala 188:28]
  wire  _GEN_89 = readEnqueued ? 1'h0 : _GEN_60; // @[AccumulatorWithALUArray.scala 188:28 MultiEnqueue.scala 40:17]
  wire  _GEN_90 = readEnqueued ? 1'h0 : _GEN_61; // @[AccumulatorWithALUArray.scala 188:28 MultiEnqueue.scala 42:18]
  wire  _GEN_91 = readEnqueued ? 1'h0 : _GEN_66; // @[AccumulatorWithALUArray.scala 188:28 MultiEnqueue.scala 42:18]
  wire  _GEN_92 = readEnqueued ? 1'h0 : _T_2; // @[AccumulatorWithALUArray.scala 188:28 package.scala 409:14]
  wire  _GEN_93 = readEnqueued ? 1'h0 : _GEN_68; // @[AccumulatorWithALUArray.scala 188:28 package.scala 410:15]
  wire  _GEN_94 = readEnqueued ? 1'h0 : _GEN_69; // @[AccumulatorWithALUArray.scala 188:28 MultiEnqueue.scala 42:18]
  wire  _GEN_95 = readEnqueued ? 1'h0 : _GEN_70; // @[AccumulatorWithALUArray.scala 188:28 package.scala 409:14]
  wire  _GEN_96 = readEnqueued ? 1'h0 : _GEN_71; // @[AccumulatorWithALUArray.scala 188:28 package.scala 409:14]
  wire  _GEN_97 = readEnqueued ? 1'h0 : _GEN_72; // @[AccumulatorWithALUArray.scala 188:28 package.scala 409:14]
  wire [3:0] _GEN_98 = readEnqueued ? 4'h0 : _GEN_73; // @[AccumulatorWithALUArray.scala 188:28 package.scala 409:14]
  wire  _GEN_99 = readEnqueued ? 1'h0 : _GEN_74; // @[AccumulatorWithALUArray.scala 188:28 package.scala 410:15]
  wire [5:0] _GEN_103 = io_control_bits_write ? _GEN_79 : io_control_bits_readAddress; // @[AccumulatorWithALUArray.scala 186:32 MultiEnqueue.scala 151:10]
  wire  dataPathReady_acc_io_control_w_4_valid = simdReadEnqueuer_io_out_0_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_104 = io_control_bits_write ? _GEN_80 : dataPathReady_acc_io_control_w_4_valid; // @[AccumulatorWithALUArray.scala 186:32 MultiEnqueue.scala 151:10]
  wire  _GEN_105 = io_control_bits_write & _GEN_81; // @[AccumulatorWithALUArray.scala 186:32 MultiEnqueue.scala 42:18]
  wire  dataPathReady_aluOutputDemux_io_enq_w_1_valid = simdReadEnqueuer_io_out_2_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_107 = io_control_bits_write ? _GEN_83 : dataPathReady_aluOutputDemux_io_enq_w_1_valid; // @[AccumulatorWithALUArray.scala 186:32 MultiEnqueue.scala 153:10]
  wire  _GEN_110 = io_control_bits_write & _GEN_86; // @[AccumulatorWithALUArray.scala 186:32 package.scala 410:15]
  wire  _GEN_112 = io_control_bits_write & _GEN_88; // @[AccumulatorWithALUArray.scala 186:32 AccumulatorWithALUArray.scala 111:16]
  wire  _GEN_113 = io_control_bits_write & _GEN_89; // @[AccumulatorWithALUArray.scala 186:32 MultiEnqueue.scala 40:17]
  wire  _GEN_114 = io_control_bits_write & _GEN_90; // @[AccumulatorWithALUArray.scala 186:32 MultiEnqueue.scala 42:18]
  wire  _GEN_115 = io_control_bits_write & _GEN_91; // @[AccumulatorWithALUArray.scala 186:32 MultiEnqueue.scala 42:18]
  wire  _GEN_116 = io_control_bits_write ? _GEN_92 : 1'h1; // @[AccumulatorWithALUArray.scala 186:32 MultiEnqueue.scala 152:10]
  wire  dataPathReady_accOutputDemux_io_enq_w_1_valid = simdReadEnqueuer_io_out_1_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_117 = io_control_bits_write ? _GEN_93 : dataPathReady_accOutputDemux_io_enq_w_1_valid; // @[AccumulatorWithALUArray.scala 186:32 MultiEnqueue.scala 152:10]
  wire  _GEN_118 = io_control_bits_write & _GEN_94; // @[AccumulatorWithALUArray.scala 186:32 MultiEnqueue.scala 42:18]
  wire  _GEN_119 = io_control_bits_write ? _GEN_95 : io_control_bits_instruction_dest; // @[AccumulatorWithALUArray.scala 186:32 MultiEnqueue.scala 154:10]
  wire  _GEN_120 = io_control_bits_write ? _GEN_96 : io_control_bits_instruction_sourceRight; // @[AccumulatorWithALUArray.scala 186:32 MultiEnqueue.scala 154:10]
  wire  _GEN_121 = io_control_bits_write ? _GEN_97 : io_control_bits_instruction_sourceLeft; // @[AccumulatorWithALUArray.scala 186:32 MultiEnqueue.scala 154:10]
  wire [3:0] _GEN_122 = io_control_bits_write ? _GEN_98 : io_control_bits_instruction_op; // @[AccumulatorWithALUArray.scala 186:32 MultiEnqueue.scala 154:10]
  wire  dataPathReady_alu_io_instruction_w_valid = simdReadEnqueuer_io_out_3_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_123 = io_control_bits_write ? _GEN_99 : dataPathReady_alu_io_instruction_w_valid; // @[AccumulatorWithALUArray.scala 186:32 MultiEnqueue.scala 154:10]
  wire  _GEN_124 = io_control_bits_write ? 1'h0 : io_control_valid; // @[AccumulatorWithALUArray.scala 186:32 MultiEnqueue.scala 40:17 MultiEnqueue.scala 150:17]
  wire  _GEN_125 = io_control_bits_write ? 1'h0 : dataPathReady_acc_io_control_w_ready; // @[AccumulatorWithALUArray.scala 186:32 MultiEnqueue.scala 42:18 ReadyValid.scala 19:11]
  wire  _GEN_126 = io_control_bits_write ? 1'h0 : readEnqueued_accOutputDemux_io_enq_w_ready; // @[AccumulatorWithALUArray.scala 186:32 MultiEnqueue.scala 42:18 ReadyValid.scala 19:11]
  wire  _GEN_127 = io_control_bits_write ? 1'h0 : dataPathReady_aluOutputDemux_io_enq_w_ready; // @[AccumulatorWithALUArray.scala 186:32 MultiEnqueue.scala 42:18 ReadyValid.scala 19:11]
  wire  _GEN_128 = io_control_bits_write ? 1'h0 : readEnqueued_alu_io_instruction_w_ready; // @[AccumulatorWithALUArray.scala 186:32 MultiEnqueue.scala 42:18 ReadyValid.scala 19:11]
  wire  dataPathReady_acc_io_control_w_5_valid = simdWriteEnqueuer_io_out_0_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_133 = io_control_bits_write & dataPathReady_acc_io_control_w_5_valid; // @[AccumulatorWithALUArray.scala 235:32 MultiEnqueue.scala 151:10 package.scala 410:15]
  wire  _GEN_134 = io_control_bits_write & dataPathReady_aluOutputDemux_io_enq_w_ready; // @[AccumulatorWithALUArray.scala 235:32 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire  dataPathReady_aluOutputDemux_io_enq_w_2_valid = simdWriteEnqueuer_io_out_1_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  dataPathReady_aluOutputDemux_io_enq_w_3_valid = simdEnqueuer_io_out_0_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_136 = io_control_bits_write ? dataPathReady_aluOutputDemux_io_enq_w_2_valid :
    dataPathReady_aluOutputDemux_io_enq_w_3_valid; // @[AccumulatorWithALUArray.scala 235:32 MultiEnqueue.scala 152:10 MultiEnqueue.scala 85:10]
  wire  dataPathReady_accInputMux_io_enq_w_3_valid = simdWriteEnqueuer_io_out_2_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_139 = io_control_bits_write & dataPathReady_accInputMux_io_enq_w_3_valid; // @[AccumulatorWithALUArray.scala 235:32 MultiEnqueue.scala 153:10 package.scala 410:15]
  wire  _GEN_140 = io_control_bits_write & readEnqueued_alu_io_instruction_w_ready; // @[AccumulatorWithALUArray.scala 235:32 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire  dataPathReady_alu_io_instruction_w_1_valid = simdWriteEnqueuer_io_out_3_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  dataPathReady_alu_io_instruction_w_2_valid = simdEnqueuer_io_out_1_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_145 = io_control_bits_write ? dataPathReady_alu_io_instruction_w_1_valid :
    dataPathReady_alu_io_instruction_w_2_valid; // @[AccumulatorWithALUArray.scala 235:32 MultiEnqueue.scala 154:10 MultiEnqueue.scala 86:10]
  wire  _GEN_149 = io_control_bits_read & _GEN_17; // @[AccumulatorWithALUArray.scala 185:29 MultiEnqueue.scala 40:17]
  wire  _GEN_150 = io_control_bits_read & _GEN_18; // @[AccumulatorWithALUArray.scala 185:29 MultiEnqueue.scala 42:18]
  wire [5:0] _GEN_153 = io_control_bits_read ? _GEN_103 : _GEN_37; // @[AccumulatorWithALUArray.scala 185:29]
  wire  _GEN_154 = io_control_bits_read ? _GEN_104 : _GEN_133; // @[AccumulatorWithALUArray.scala 185:29]
  wire  _GEN_155 = io_control_bits_read & _GEN_105; // @[AccumulatorWithALUArray.scala 185:29 MultiEnqueue.scala 42:18]
  wire  _GEN_157 = io_control_bits_read ? _GEN_107 : _GEN_136; // @[AccumulatorWithALUArray.scala 185:29]
  wire  _GEN_158 = io_control_bits_read & _GEN_23; // @[AccumulatorWithALUArray.scala 185:29 MultiEnqueue.scala 42:18]
  wire  _GEN_160 = io_control_bits_read ? _GEN_110 : _GEN_139; // @[AccumulatorWithALUArray.scala 185:29]
  wire  _GEN_162 = io_control_bits_read & _GEN_112; // @[AccumulatorWithALUArray.scala 185:29 AccumulatorWithALUArray.scala 111:16]
  wire  _GEN_163 = io_control_bits_read & _GEN_113; // @[AccumulatorWithALUArray.scala 185:29 MultiEnqueue.scala 40:17]
  wire  _GEN_164 = io_control_bits_read & _GEN_114; // @[AccumulatorWithALUArray.scala 185:29 MultiEnqueue.scala 42:18]
  wire  _GEN_165 = io_control_bits_read & _GEN_115; // @[AccumulatorWithALUArray.scala 185:29 MultiEnqueue.scala 42:18]
  wire  _GEN_166 = io_control_bits_read & _GEN_116; // @[AccumulatorWithALUArray.scala 185:29 package.scala 409:14]
  wire  _GEN_167 = io_control_bits_read & _GEN_117; // @[AccumulatorWithALUArray.scala 185:29 package.scala 410:15]
  wire  _GEN_168 = io_control_bits_read & _GEN_118; // @[AccumulatorWithALUArray.scala 185:29 MultiEnqueue.scala 42:18]
  wire  _GEN_169 = io_control_bits_read ? _GEN_119 : io_control_bits_instruction_dest; // @[AccumulatorWithALUArray.scala 185:29]
  wire  _GEN_170 = io_control_bits_read ? _GEN_120 : io_control_bits_instruction_sourceRight; // @[AccumulatorWithALUArray.scala 185:29]
  wire  _GEN_171 = io_control_bits_read ? _GEN_121 : io_control_bits_instruction_sourceLeft; // @[AccumulatorWithALUArray.scala 185:29]
  wire [3:0] _GEN_172 = io_control_bits_read ? _GEN_122 : io_control_bits_instruction_op; // @[AccumulatorWithALUArray.scala 185:29]
  wire  _GEN_173 = io_control_bits_read ? _GEN_123 : _GEN_145; // @[AccumulatorWithALUArray.scala 185:29]
  wire  _GEN_174 = io_control_bits_read & _GEN_124; // @[AccumulatorWithALUArray.scala 185:29 MultiEnqueue.scala 40:17]
  wire  _GEN_175 = io_control_bits_read & _GEN_125; // @[AccumulatorWithALUArray.scala 185:29 MultiEnqueue.scala 42:18]
  wire  _GEN_176 = io_control_bits_read & _GEN_126; // @[AccumulatorWithALUArray.scala 185:29 MultiEnqueue.scala 42:18]
  wire  _GEN_177 = io_control_bits_read & _GEN_127; // @[AccumulatorWithALUArray.scala 185:29 MultiEnqueue.scala 42:18]
  wire  _GEN_178 = io_control_bits_read & _GEN_128; // @[AccumulatorWithALUArray.scala 185:29 MultiEnqueue.scala 42:18]
  wire  _GEN_179 = io_control_bits_read ? 1'h0 : _GEN_33; // @[AccumulatorWithALUArray.scala 185:29 MultiEnqueue.scala 40:17]
  wire  _GEN_180 = io_control_bits_read ? 1'h0 : _GEN_34; // @[AccumulatorWithALUArray.scala 185:29 MultiEnqueue.scala 42:18]
  wire  _GEN_181 = io_control_bits_read ? 1'h0 : _GEN_134; // @[AccumulatorWithALUArray.scala 185:29 MultiEnqueue.scala 42:18]
  wire  _GEN_182 = io_control_bits_read ? 1'h0 : _GEN_39; // @[AccumulatorWithALUArray.scala 185:29 MultiEnqueue.scala 42:18]
  wire  _GEN_183 = io_control_bits_read ? 1'h0 : _GEN_140; // @[AccumulatorWithALUArray.scala 185:29 MultiEnqueue.scala 42:18]
  wire  _GEN_184 = io_control_bits_read ? 1'h0 : _GEN_124; // @[AccumulatorWithALUArray.scala 185:29 MultiEnqueue.scala 40:17]
  wire  _GEN_185 = io_control_bits_read ? 1'h0 : _GEN_127; // @[AccumulatorWithALUArray.scala 185:29 MultiEnqueue.scala 42:18]
  wire  _GEN_186 = io_control_bits_read ? 1'h0 : _GEN_128; // @[AccumulatorWithALUArray.scala 185:29 MultiEnqueue.scala 42:18]
  Accumulator acc ( // @[AccumulatorWithALUArray.scala 44:19]
    .clock(acc_clock),
    .reset(acc_reset),
    .io_input_ready(acc_io_input_ready),
    .io_input_valid(acc_io_input_valid),
    .io_input_bits_0(acc_io_input_bits_0),
    .io_input_bits_1(acc_io_input_bits_1),
    .io_input_bits_2(acc_io_input_bits_2),
    .io_input_bits_3(acc_io_input_bits_3),
    .io_input_bits_4(acc_io_input_bits_4),
    .io_input_bits_5(acc_io_input_bits_5),
    .io_input_bits_6(acc_io_input_bits_6),
    .io_input_bits_7(acc_io_input_bits_7),
    .io_output_ready(acc_io_output_ready),
    .io_output_valid(acc_io_output_valid),
    .io_output_bits_0(acc_io_output_bits_0),
    .io_output_bits_1(acc_io_output_bits_1),
    .io_output_bits_2(acc_io_output_bits_2),
    .io_output_bits_3(acc_io_output_bits_3),
    .io_output_bits_4(acc_io_output_bits_4),
    .io_output_bits_5(acc_io_output_bits_5),
    .io_output_bits_6(acc_io_output_bits_6),
    .io_output_bits_7(acc_io_output_bits_7),
    .io_control_ready(acc_io_control_ready),
    .io_control_valid(acc_io_control_valid),
    .io_control_bits_address(acc_io_control_bits_address),
    .io_control_bits_accumulate(acc_io_control_bits_accumulate),
    .io_control_bits_write(acc_io_control_bits_write),
    .io_tracepoint(acc_io_tracepoint),
    .io_programCounter(acc_io_programCounter)
  );
  ALUArray alu ( // @[AccumulatorWithALUArray.scala 45:19]
    .clock(alu_clock),
    .reset(alu_reset),
    .io_input_ready(alu_io_input_ready),
    .io_input_valid(alu_io_input_valid),
    .io_input_bits_0(alu_io_input_bits_0),
    .io_input_bits_1(alu_io_input_bits_1),
    .io_input_bits_2(alu_io_input_bits_2),
    .io_input_bits_3(alu_io_input_bits_3),
    .io_input_bits_4(alu_io_input_bits_4),
    .io_input_bits_5(alu_io_input_bits_5),
    .io_input_bits_6(alu_io_input_bits_6),
    .io_input_bits_7(alu_io_input_bits_7),
    .io_output_ready(alu_io_output_ready),
    .io_output_valid(alu_io_output_valid),
    .io_output_bits_0(alu_io_output_bits_0),
    .io_output_bits_1(alu_io_output_bits_1),
    .io_output_bits_2(alu_io_output_bits_2),
    .io_output_bits_3(alu_io_output_bits_3),
    .io_output_bits_4(alu_io_output_bits_4),
    .io_output_bits_5(alu_io_output_bits_5),
    .io_output_bits_6(alu_io_output_bits_6),
    .io_output_bits_7(alu_io_output_bits_7),
    .io_instruction_ready(alu_io_instruction_ready),
    .io_instruction_valid(alu_io_instruction_valid),
    .io_instruction_bits_op(alu_io_instruction_bits_op),
    .io_instruction_bits_sourceLeft(alu_io_instruction_bits_sourceLeft),
    .io_instruction_bits_sourceRight(alu_io_instruction_bits_sourceRight),
    .io_instruction_bits_dest(alu_io_instruction_bits_dest)
  );
  Demux aluOutputDemux_x6_demux ( // @[Demux.scala 46:23]
    .io_in_ready(aluOutputDemux_x6_demux_io_in_ready),
    .io_in_valid(aluOutputDemux_x6_demux_io_in_valid),
    .io_in_bits_0(aluOutputDemux_x6_demux_io_in_bits_0),
    .io_in_bits_1(aluOutputDemux_x6_demux_io_in_bits_1),
    .io_in_bits_2(aluOutputDemux_x6_demux_io_in_bits_2),
    .io_in_bits_3(aluOutputDemux_x6_demux_io_in_bits_3),
    .io_in_bits_4(aluOutputDemux_x6_demux_io_in_bits_4),
    .io_in_bits_5(aluOutputDemux_x6_demux_io_in_bits_5),
    .io_in_bits_6(aluOutputDemux_x6_demux_io_in_bits_6),
    .io_in_bits_7(aluOutputDemux_x6_demux_io_in_bits_7),
    .io_sel_ready(aluOutputDemux_x6_demux_io_sel_ready),
    .io_sel_valid(aluOutputDemux_x6_demux_io_sel_valid),
    .io_sel_bits(aluOutputDemux_x6_demux_io_sel_bits),
    .io_out_0_ready(aluOutputDemux_x6_demux_io_out_0_ready),
    .io_out_0_valid(aluOutputDemux_x6_demux_io_out_0_valid),
    .io_out_0_bits_0(aluOutputDemux_x6_demux_io_out_0_bits_0),
    .io_out_0_bits_1(aluOutputDemux_x6_demux_io_out_0_bits_1),
    .io_out_0_bits_2(aluOutputDemux_x6_demux_io_out_0_bits_2),
    .io_out_0_bits_3(aluOutputDemux_x6_demux_io_out_0_bits_3),
    .io_out_0_bits_4(aluOutputDemux_x6_demux_io_out_0_bits_4),
    .io_out_0_bits_5(aluOutputDemux_x6_demux_io_out_0_bits_5),
    .io_out_0_bits_6(aluOutputDemux_x6_demux_io_out_0_bits_6),
    .io_out_0_bits_7(aluOutputDemux_x6_demux_io_out_0_bits_7),
    .io_out_1_ready(aluOutputDemux_x6_demux_io_out_1_ready),
    .io_out_1_valid(aluOutputDemux_x6_demux_io_out_1_valid),
    .io_out_1_bits_0(aluOutputDemux_x6_demux_io_out_1_bits_0),
    .io_out_1_bits_1(aluOutputDemux_x6_demux_io_out_1_bits_1),
    .io_out_1_bits_2(aluOutputDemux_x6_demux_io_out_1_bits_2),
    .io_out_1_bits_3(aluOutputDemux_x6_demux_io_out_1_bits_3),
    .io_out_1_bits_4(aluOutputDemux_x6_demux_io_out_1_bits_4),
    .io_out_1_bits_5(aluOutputDemux_x6_demux_io_out_1_bits_5),
    .io_out_1_bits_6(aluOutputDemux_x6_demux_io_out_1_bits_6),
    .io_out_1_bits_7(aluOutputDemux_x6_demux_io_out_1_bits_7)
  );
  Queue_16 aluOutputDemux ( // @[Mem.scala 23:19]
    .clock(aluOutputDemux_clock),
    .reset(aluOutputDemux_reset),
    .io_enq_ready(aluOutputDemux_io_enq_ready),
    .io_enq_valid(aluOutputDemux_io_enq_valid),
    .io_enq_bits(aluOutputDemux_io_enq_bits),
    .io_deq_ready(aluOutputDemux_io_deq_ready),
    .io_deq_valid(aluOutputDemux_io_deq_valid),
    .io_deq_bits(aluOutputDemux_io_deq_bits)
  );
  Mux accInputMux_x15_mux ( // @[Mux.scala 71:21]
    .io_in_0_ready(accInputMux_x15_mux_io_in_0_ready),
    .io_in_0_valid(accInputMux_x15_mux_io_in_0_valid),
    .io_in_0_bits_0(accInputMux_x15_mux_io_in_0_bits_0),
    .io_in_0_bits_1(accInputMux_x15_mux_io_in_0_bits_1),
    .io_in_0_bits_2(accInputMux_x15_mux_io_in_0_bits_2),
    .io_in_0_bits_3(accInputMux_x15_mux_io_in_0_bits_3),
    .io_in_0_bits_4(accInputMux_x15_mux_io_in_0_bits_4),
    .io_in_0_bits_5(accInputMux_x15_mux_io_in_0_bits_5),
    .io_in_0_bits_6(accInputMux_x15_mux_io_in_0_bits_6),
    .io_in_0_bits_7(accInputMux_x15_mux_io_in_0_bits_7),
    .io_in_1_ready(accInputMux_x15_mux_io_in_1_ready),
    .io_in_1_valid(accInputMux_x15_mux_io_in_1_valid),
    .io_in_1_bits_0(accInputMux_x15_mux_io_in_1_bits_0),
    .io_in_1_bits_1(accInputMux_x15_mux_io_in_1_bits_1),
    .io_in_1_bits_2(accInputMux_x15_mux_io_in_1_bits_2),
    .io_in_1_bits_3(accInputMux_x15_mux_io_in_1_bits_3),
    .io_in_1_bits_4(accInputMux_x15_mux_io_in_1_bits_4),
    .io_in_1_bits_5(accInputMux_x15_mux_io_in_1_bits_5),
    .io_in_1_bits_6(accInputMux_x15_mux_io_in_1_bits_6),
    .io_in_1_bits_7(accInputMux_x15_mux_io_in_1_bits_7),
    .io_sel_ready(accInputMux_x15_mux_io_sel_ready),
    .io_sel_valid(accInputMux_x15_mux_io_sel_valid),
    .io_sel_bits(accInputMux_x15_mux_io_sel_bits),
    .io_out_ready(accInputMux_x15_mux_io_out_ready),
    .io_out_valid(accInputMux_x15_mux_io_out_valid),
    .io_out_bits_0(accInputMux_x15_mux_io_out_bits_0),
    .io_out_bits_1(accInputMux_x15_mux_io_out_bits_1),
    .io_out_bits_2(accInputMux_x15_mux_io_out_bits_2),
    .io_out_bits_3(accInputMux_x15_mux_io_out_bits_3),
    .io_out_bits_4(accInputMux_x15_mux_io_out_bits_4),
    .io_out_bits_5(accInputMux_x15_mux_io_out_bits_5),
    .io_out_bits_6(accInputMux_x15_mux_io_out_bits_6),
    .io_out_bits_7(accInputMux_x15_mux_io_out_bits_7)
  );
  Queue_16 accInputMux ( // @[Mem.scala 23:19]
    .clock(accInputMux_clock),
    .reset(accInputMux_reset),
    .io_enq_ready(accInputMux_io_enq_ready),
    .io_enq_valid(accInputMux_io_enq_valid),
    .io_enq_bits(accInputMux_io_enq_bits),
    .io_deq_ready(accInputMux_io_deq_ready),
    .io_deq_valid(accInputMux_io_deq_valid),
    .io_deq_bits(accInputMux_io_deq_bits)
  );
  Demux accOutputDemux_x24_demux ( // @[Demux.scala 46:23]
    .io_in_ready(accOutputDemux_x24_demux_io_in_ready),
    .io_in_valid(accOutputDemux_x24_demux_io_in_valid),
    .io_in_bits_0(accOutputDemux_x24_demux_io_in_bits_0),
    .io_in_bits_1(accOutputDemux_x24_demux_io_in_bits_1),
    .io_in_bits_2(accOutputDemux_x24_demux_io_in_bits_2),
    .io_in_bits_3(accOutputDemux_x24_demux_io_in_bits_3),
    .io_in_bits_4(accOutputDemux_x24_demux_io_in_bits_4),
    .io_in_bits_5(accOutputDemux_x24_demux_io_in_bits_5),
    .io_in_bits_6(accOutputDemux_x24_demux_io_in_bits_6),
    .io_in_bits_7(accOutputDemux_x24_demux_io_in_bits_7),
    .io_sel_ready(accOutputDemux_x24_demux_io_sel_ready),
    .io_sel_valid(accOutputDemux_x24_demux_io_sel_valid),
    .io_sel_bits(accOutputDemux_x24_demux_io_sel_bits),
    .io_out_0_ready(accOutputDemux_x24_demux_io_out_0_ready),
    .io_out_0_valid(accOutputDemux_x24_demux_io_out_0_valid),
    .io_out_0_bits_0(accOutputDemux_x24_demux_io_out_0_bits_0),
    .io_out_0_bits_1(accOutputDemux_x24_demux_io_out_0_bits_1),
    .io_out_0_bits_2(accOutputDemux_x24_demux_io_out_0_bits_2),
    .io_out_0_bits_3(accOutputDemux_x24_demux_io_out_0_bits_3),
    .io_out_0_bits_4(accOutputDemux_x24_demux_io_out_0_bits_4),
    .io_out_0_bits_5(accOutputDemux_x24_demux_io_out_0_bits_5),
    .io_out_0_bits_6(accOutputDemux_x24_demux_io_out_0_bits_6),
    .io_out_0_bits_7(accOutputDemux_x24_demux_io_out_0_bits_7),
    .io_out_1_ready(accOutputDemux_x24_demux_io_out_1_ready),
    .io_out_1_valid(accOutputDemux_x24_demux_io_out_1_valid),
    .io_out_1_bits_0(accOutputDemux_x24_demux_io_out_1_bits_0),
    .io_out_1_bits_1(accOutputDemux_x24_demux_io_out_1_bits_1),
    .io_out_1_bits_2(accOutputDemux_x24_demux_io_out_1_bits_2),
    .io_out_1_bits_3(accOutputDemux_x24_demux_io_out_1_bits_3),
    .io_out_1_bits_4(accOutputDemux_x24_demux_io_out_1_bits_4),
    .io_out_1_bits_5(accOutputDemux_x24_demux_io_out_1_bits_5),
    .io_out_1_bits_6(accOutputDemux_x24_demux_io_out_1_bits_6),
    .io_out_1_bits_7(accOutputDemux_x24_demux_io_out_1_bits_7)
  );
  Queue_16 accOutputDemux ( // @[Mem.scala 23:19]
    .clock(accOutputDemux_clock),
    .reset(accOutputDemux_reset),
    .io_enq_ready(accOutputDemux_io_enq_ready),
    .io_enq_valid(accOutputDemux_io_enq_valid),
    .io_enq_bits(accOutputDemux_io_enq_bits),
    .io_deq_ready(accOutputDemux_io_deq_ready),
    .io_deq_valid(accOutputDemux_io_deq_valid),
    .io_deq_bits(accOutputDemux_io_deq_bits)
  );
  MultiEnqueue_1 accWriteEnqueuer ( // @[MultiEnqueue.scala 160:43]
    .clock(accWriteEnqueuer_clock),
    .reset(accWriteEnqueuer_reset),
    .io_in_ready(accWriteEnqueuer_io_in_ready),
    .io_in_valid(accWriteEnqueuer_io_in_valid),
    .io_out_0_ready(accWriteEnqueuer_io_out_0_ready),
    .io_out_0_valid(accWriteEnqueuer_io_out_0_valid),
    .io_out_1_ready(accWriteEnqueuer_io_out_1_ready),
    .io_out_1_valid(accWriteEnqueuer_io_out_1_valid)
  );
  MultiEnqueue_1 accReadEnqueuer ( // @[MultiEnqueue.scala 160:43]
    .clock(accReadEnqueuer_clock),
    .reset(accReadEnqueuer_reset),
    .io_in_ready(accReadEnqueuer_io_in_ready),
    .io_in_valid(accReadEnqueuer_io_in_valid),
    .io_out_0_ready(accReadEnqueuer_io_out_0_ready),
    .io_out_0_valid(accReadEnqueuer_io_out_0_valid),
    .io_out_1_ready(accReadEnqueuer_io_out_1_ready),
    .io_out_1_valid(accReadEnqueuer_io_out_1_valid)
  );
  MultiEnqueue_2 simdRWWriteEnqueuer ( // @[MultiEnqueue.scala 160:43]
    .clock(simdRWWriteEnqueuer_clock),
    .reset(simdRWWriteEnqueuer_reset),
    .io_in_ready(simdRWWriteEnqueuer_io_in_ready),
    .io_in_valid(simdRWWriteEnqueuer_io_in_valid),
    .io_out_0_ready(simdRWWriteEnqueuer_io_out_0_ready),
    .io_out_0_valid(simdRWWriteEnqueuer_io_out_0_valid),
    .io_out_1_ready(simdRWWriteEnqueuer_io_out_1_ready),
    .io_out_1_valid(simdRWWriteEnqueuer_io_out_1_valid),
    .io_out_2_ready(simdRWWriteEnqueuer_io_out_2_ready),
    .io_out_2_valid(simdRWWriteEnqueuer_io_out_2_valid)
  );
  MultiEnqueue_2 simdRWReadEnqueuer ( // @[MultiEnqueue.scala 160:43]
    .clock(simdRWReadEnqueuer_clock),
    .reset(simdRWReadEnqueuer_reset),
    .io_in_ready(simdRWReadEnqueuer_io_in_ready),
    .io_in_valid(simdRWReadEnqueuer_io_in_valid),
    .io_out_0_ready(simdRWReadEnqueuer_io_out_0_ready),
    .io_out_0_valid(simdRWReadEnqueuer_io_out_0_valid),
    .io_out_1_ready(simdRWReadEnqueuer_io_out_1_ready),
    .io_out_1_valid(simdRWReadEnqueuer_io_out_1_valid),
    .io_out_2_ready(simdRWReadEnqueuer_io_out_2_ready),
    .io_out_2_valid(simdRWReadEnqueuer_io_out_2_valid)
  );
  MultiEnqueue_3 simdWriteEnqueuer ( // @[MultiEnqueue.scala 160:43]
    .clock(simdWriteEnqueuer_clock),
    .reset(simdWriteEnqueuer_reset),
    .io_in_ready(simdWriteEnqueuer_io_in_ready),
    .io_in_valid(simdWriteEnqueuer_io_in_valid),
    .io_out_0_ready(simdWriteEnqueuer_io_out_0_ready),
    .io_out_0_valid(simdWriteEnqueuer_io_out_0_valid),
    .io_out_1_ready(simdWriteEnqueuer_io_out_1_ready),
    .io_out_1_valid(simdWriteEnqueuer_io_out_1_valid),
    .io_out_2_ready(simdWriteEnqueuer_io_out_2_ready),
    .io_out_2_valid(simdWriteEnqueuer_io_out_2_valid),
    .io_out_3_ready(simdWriteEnqueuer_io_out_3_ready),
    .io_out_3_valid(simdWriteEnqueuer_io_out_3_valid)
  );
  MultiEnqueue_3 simdReadEnqueuer ( // @[MultiEnqueue.scala 160:43]
    .clock(simdReadEnqueuer_clock),
    .reset(simdReadEnqueuer_reset),
    .io_in_ready(simdReadEnqueuer_io_in_ready),
    .io_in_valid(simdReadEnqueuer_io_in_valid),
    .io_out_0_ready(simdReadEnqueuer_io_out_0_ready),
    .io_out_0_valid(simdReadEnqueuer_io_out_0_valid),
    .io_out_1_ready(simdReadEnqueuer_io_out_1_ready),
    .io_out_1_valid(simdReadEnqueuer_io_out_1_valid),
    .io_out_2_ready(simdReadEnqueuer_io_out_2_ready),
    .io_out_2_valid(simdReadEnqueuer_io_out_2_valid),
    .io_out_3_ready(simdReadEnqueuer_io_out_3_ready),
    .io_out_3_valid(simdReadEnqueuer_io_out_3_valid)
  );
  MultiEnqueue_1 simdEnqueuer ( // @[MultiEnqueue.scala 160:43]
    .clock(simdEnqueuer_clock),
    .reset(simdEnqueuer_reset),
    .io_in_ready(simdEnqueuer_io_in_ready),
    .io_in_valid(simdEnqueuer_io_in_valid),
    .io_out_0_ready(simdEnqueuer_io_out_0_ready),
    .io_out_0_valid(simdEnqueuer_io_out_0_valid),
    .io_out_1_ready(simdEnqueuer_io_out_1_ready),
    .io_out_1_valid(simdEnqueuer_io_out_1_valid)
  );
  assign io_input_ready = accInputMux_x15_mux_io_in_0_ready; // @[Mux.scala 79:18]
  assign io_output_valid = accOutputDemux_x24_demux_io_out_0_valid; // @[Demux.scala 55:10]
  assign io_output_bits_0 = accOutputDemux_x24_demux_io_out_0_bits_0; // @[Demux.scala 55:10]
  assign io_output_bits_1 = accOutputDemux_x24_demux_io_out_0_bits_1; // @[Demux.scala 55:10]
  assign io_output_bits_2 = accOutputDemux_x24_demux_io_out_0_bits_2; // @[Demux.scala 55:10]
  assign io_output_bits_3 = accOutputDemux_x24_demux_io_out_0_bits_3; // @[Demux.scala 55:10]
  assign io_output_bits_4 = accOutputDemux_x24_demux_io_out_0_bits_4; // @[Demux.scala 55:10]
  assign io_output_bits_5 = accOutputDemux_x24_demux_io_out_0_bits_5; // @[Demux.scala 55:10]
  assign io_output_bits_6 = accOutputDemux_x24_demux_io_out_0_bits_6; // @[Demux.scala 55:10]
  assign io_output_bits_7 = accOutputDemux_x24_demux_io_out_0_bits_7; // @[Demux.scala 55:10]
  assign io_control_ready = _io_wrote_valid_T ? dataPathReady : dataPathReady_1; // @[AccumulatorWithALUArray.scala 133:16 AccumulatorWithALUArray.scala 182:19 AccumulatorWithALUArray.scala 257:19]
  assign acc_clock = clock;
  assign acc_reset = reset;
  assign acc_io_input_valid = accInputMux_x15_mux_io_out_valid; // @[Mux.scala 81:9]
  assign acc_io_input_bits_0 = accInputMux_x15_mux_io_out_bits_0; // @[Mux.scala 81:9]
  assign acc_io_input_bits_1 = accInputMux_x15_mux_io_out_bits_1; // @[Mux.scala 81:9]
  assign acc_io_input_bits_2 = accInputMux_x15_mux_io_out_bits_2; // @[Mux.scala 81:9]
  assign acc_io_input_bits_3 = accInputMux_x15_mux_io_out_bits_3; // @[Mux.scala 81:9]
  assign acc_io_input_bits_4 = accInputMux_x15_mux_io_out_bits_4; // @[Mux.scala 81:9]
  assign acc_io_input_bits_5 = accInputMux_x15_mux_io_out_bits_5; // @[Mux.scala 81:9]
  assign acc_io_input_bits_6 = accInputMux_x15_mux_io_out_bits_6; // @[Mux.scala 81:9]
  assign acc_io_input_bits_7 = accInputMux_x15_mux_io_out_bits_7; // @[Mux.scala 81:9]
  assign acc_io_output_ready = accOutputDemux_x24_demux_io_in_ready; // @[Demux.scala 54:17]
  assign acc_io_control_valid = _io_wrote_valid_T ? _GEN_48 : _GEN_154; // @[AccumulatorWithALUArray.scala 133:16]
  assign acc_io_control_bits_address = _io_wrote_valid_T ? _GEN_47 : _GEN_153; // @[AccumulatorWithALUArray.scala 133:16]
  assign acc_io_control_bits_accumulate = _io_wrote_valid_T ? _GEN_46 : _GEN_46; // @[AccumulatorWithALUArray.scala 133:16]
  assign acc_io_control_bits_write = _io_wrote_valid_T ? _GEN_45 : _GEN_45; // @[AccumulatorWithALUArray.scala 133:16]
  assign acc_io_tracepoint = io_tracepoint; // @[AccumulatorWithALUArray.scala 68:21]
  assign acc_io_programCounter = io_programCounter; // @[AccumulatorWithALUArray.scala 69:25]
  assign alu_clock = clock;
  assign alu_reset = reset;
  assign alu_io_input_valid = accOutputDemux_x24_demux_io_out_1_valid; // @[Demux.scala 56:10]
  assign alu_io_input_bits_0 = accOutputDemux_x24_demux_io_out_1_bits_0; // @[Demux.scala 56:10]
  assign alu_io_input_bits_1 = accOutputDemux_x24_demux_io_out_1_bits_1; // @[Demux.scala 56:10]
  assign alu_io_input_bits_2 = accOutputDemux_x24_demux_io_out_1_bits_2; // @[Demux.scala 56:10]
  assign alu_io_input_bits_3 = accOutputDemux_x24_demux_io_out_1_bits_3; // @[Demux.scala 56:10]
  assign alu_io_input_bits_4 = accOutputDemux_x24_demux_io_out_1_bits_4; // @[Demux.scala 56:10]
  assign alu_io_input_bits_5 = accOutputDemux_x24_demux_io_out_1_bits_5; // @[Demux.scala 56:10]
  assign alu_io_input_bits_6 = accOutputDemux_x24_demux_io_out_1_bits_6; // @[Demux.scala 56:10]
  assign alu_io_input_bits_7 = accOutputDemux_x24_demux_io_out_1_bits_7; // @[Demux.scala 56:10]
  assign alu_io_output_ready = aluOutputDemux_x6_demux_io_in_ready; // @[Demux.scala 54:17]
  assign alu_io_instruction_valid = _io_wrote_valid_T ? 1'h0 : _GEN_173; // @[AccumulatorWithALUArray.scala 133:16 package.scala 410:15]
  assign alu_io_instruction_bits_op = _io_wrote_valid_T ? 4'h0 : _GEN_172; // @[AccumulatorWithALUArray.scala 133:16 package.scala 409:14]
  assign alu_io_instruction_bits_sourceLeft = _io_wrote_valid_T ? 1'h0 : _GEN_171; // @[AccumulatorWithALUArray.scala 133:16 package.scala 409:14]
  assign alu_io_instruction_bits_sourceRight = _io_wrote_valid_T ? 1'h0 : _GEN_170; // @[AccumulatorWithALUArray.scala 133:16 package.scala 409:14]
  assign alu_io_instruction_bits_dest = _io_wrote_valid_T ? 1'h0 : _GEN_169; // @[AccumulatorWithALUArray.scala 133:16 package.scala 409:14]
  assign aluOutputDemux_x6_demux_io_in_valid = alu_io_output_valid; // @[Demux.scala 54:17]
  assign aluOutputDemux_x6_demux_io_in_bits_0 = alu_io_output_bits_0; // @[Demux.scala 54:17]
  assign aluOutputDemux_x6_demux_io_in_bits_1 = alu_io_output_bits_1; // @[Demux.scala 54:17]
  assign aluOutputDemux_x6_demux_io_in_bits_2 = alu_io_output_bits_2; // @[Demux.scala 54:17]
  assign aluOutputDemux_x6_demux_io_in_bits_3 = alu_io_output_bits_3; // @[Demux.scala 54:17]
  assign aluOutputDemux_x6_demux_io_in_bits_4 = alu_io_output_bits_4; // @[Demux.scala 54:17]
  assign aluOutputDemux_x6_demux_io_in_bits_5 = alu_io_output_bits_5; // @[Demux.scala 54:17]
  assign aluOutputDemux_x6_demux_io_in_bits_6 = alu_io_output_bits_6; // @[Demux.scala 54:17]
  assign aluOutputDemux_x6_demux_io_in_bits_7 = alu_io_output_bits_7; // @[Demux.scala 54:17]
  assign aluOutputDemux_x6_demux_io_sel_valid = aluOutputDemux_io_deq_valid; // @[Mem.scala 24:7]
  assign aluOutputDemux_x6_demux_io_sel_bits = aluOutputDemux_io_deq_bits; // @[Mem.scala 24:7]
  assign aluOutputDemux_x6_demux_io_out_0_ready = 1'h1; // @[Demux.scala 55:10]
  assign aluOutputDemux_x6_demux_io_out_1_ready = accInputMux_x15_mux_io_in_1_ready; // @[AccumulatorWithALUArray.scala 50:34 Mux.scala 80:18]
  assign aluOutputDemux_clock = clock;
  assign aluOutputDemux_reset = reset;
  assign aluOutputDemux_io_enq_valid = _io_wrote_valid_T ? 1'h0 : _GEN_157; // @[AccumulatorWithALUArray.scala 133:16 package.scala 410:15]
  assign aluOutputDemux_io_enq_bits = _io_wrote_valid_T ? 1'h0 : _GEN_45; // @[AccumulatorWithALUArray.scala 133:16 package.scala 409:14]
  assign aluOutputDemux_io_deq_ready = aluOutputDemux_x6_demux_io_sel_ready; // @[Mem.scala 24:7]
  assign accInputMux_x15_mux_io_in_0_valid = io_input_valid; // @[Mux.scala 79:18]
  assign accInputMux_x15_mux_io_in_0_bits_0 = io_input_bits_0; // @[Mux.scala 79:18]
  assign accInputMux_x15_mux_io_in_0_bits_1 = io_input_bits_1; // @[Mux.scala 79:18]
  assign accInputMux_x15_mux_io_in_0_bits_2 = io_input_bits_2; // @[Mux.scala 79:18]
  assign accInputMux_x15_mux_io_in_0_bits_3 = io_input_bits_3; // @[Mux.scala 79:18]
  assign accInputMux_x15_mux_io_in_0_bits_4 = io_input_bits_4; // @[Mux.scala 79:18]
  assign accInputMux_x15_mux_io_in_0_bits_5 = io_input_bits_5; // @[Mux.scala 79:18]
  assign accInputMux_x15_mux_io_in_0_bits_6 = io_input_bits_6; // @[Mux.scala 79:18]
  assign accInputMux_x15_mux_io_in_0_bits_7 = io_input_bits_7; // @[Mux.scala 79:18]
  assign accInputMux_x15_mux_io_in_1_valid = aluOutputDemux_x6_demux_io_out_1_valid; // @[AccumulatorWithALUArray.scala 50:34 Demux.scala 56:10]
  assign accInputMux_x15_mux_io_in_1_bits_0 = aluOutputDemux_x6_demux_io_out_1_bits_0; // @[AccumulatorWithALUArray.scala 50:34 Demux.scala 56:10]
  assign accInputMux_x15_mux_io_in_1_bits_1 = aluOutputDemux_x6_demux_io_out_1_bits_1; // @[AccumulatorWithALUArray.scala 50:34 Demux.scala 56:10]
  assign accInputMux_x15_mux_io_in_1_bits_2 = aluOutputDemux_x6_demux_io_out_1_bits_2; // @[AccumulatorWithALUArray.scala 50:34 Demux.scala 56:10]
  assign accInputMux_x15_mux_io_in_1_bits_3 = aluOutputDemux_x6_demux_io_out_1_bits_3; // @[AccumulatorWithALUArray.scala 50:34 Demux.scala 56:10]
  assign accInputMux_x15_mux_io_in_1_bits_4 = aluOutputDemux_x6_demux_io_out_1_bits_4; // @[AccumulatorWithALUArray.scala 50:34 Demux.scala 56:10]
  assign accInputMux_x15_mux_io_in_1_bits_5 = aluOutputDemux_x6_demux_io_out_1_bits_5; // @[AccumulatorWithALUArray.scala 50:34 Demux.scala 56:10]
  assign accInputMux_x15_mux_io_in_1_bits_6 = aluOutputDemux_x6_demux_io_out_1_bits_6; // @[AccumulatorWithALUArray.scala 50:34 Demux.scala 56:10]
  assign accInputMux_x15_mux_io_in_1_bits_7 = aluOutputDemux_x6_demux_io_out_1_bits_7; // @[AccumulatorWithALUArray.scala 50:34 Demux.scala 56:10]
  assign accInputMux_x15_mux_io_sel_valid = accInputMux_io_deq_valid; // @[Mem.scala 24:7]
  assign accInputMux_x15_mux_io_sel_bits = accInputMux_io_deq_bits; // @[Mem.scala 24:7]
  assign accInputMux_x15_mux_io_out_ready = acc_io_input_ready; // @[Mux.scala 81:9]
  assign accInputMux_clock = clock;
  assign accInputMux_reset = reset;
  assign accInputMux_io_enq_valid = _io_wrote_valid_T ? _GEN_51 : _GEN_160; // @[AccumulatorWithALUArray.scala 133:16]
  assign accInputMux_io_enq_bits = _io_wrote_valid_T ? 1'h0 : _GEN_45; // @[AccumulatorWithALUArray.scala 133:16]
  assign accInputMux_io_deq_ready = accInputMux_x15_mux_io_sel_ready; // @[Mem.scala 24:7]
  assign accOutputDemux_x24_demux_io_in_valid = acc_io_output_valid; // @[Demux.scala 54:17]
  assign accOutputDemux_x24_demux_io_in_bits_0 = acc_io_output_bits_0; // @[Demux.scala 54:17]
  assign accOutputDemux_x24_demux_io_in_bits_1 = acc_io_output_bits_1; // @[Demux.scala 54:17]
  assign accOutputDemux_x24_demux_io_in_bits_2 = acc_io_output_bits_2; // @[Demux.scala 54:17]
  assign accOutputDemux_x24_demux_io_in_bits_3 = acc_io_output_bits_3; // @[Demux.scala 54:17]
  assign accOutputDemux_x24_demux_io_in_bits_4 = acc_io_output_bits_4; // @[Demux.scala 54:17]
  assign accOutputDemux_x24_demux_io_in_bits_5 = acc_io_output_bits_5; // @[Demux.scala 54:17]
  assign accOutputDemux_x24_demux_io_in_bits_6 = acc_io_output_bits_6; // @[Demux.scala 54:17]
  assign accOutputDemux_x24_demux_io_in_bits_7 = acc_io_output_bits_7; // @[Demux.scala 54:17]
  assign accOutputDemux_x24_demux_io_sel_valid = accOutputDemux_io_deq_valid; // @[Mem.scala 24:7]
  assign accOutputDemux_x24_demux_io_sel_bits = accOutputDemux_io_deq_bits; // @[Mem.scala 24:7]
  assign accOutputDemux_x24_demux_io_out_0_ready = io_output_ready; // @[Demux.scala 55:10]
  assign accOutputDemux_x24_demux_io_out_1_ready = alu_io_input_ready; // @[Demux.scala 56:10]
  assign accOutputDemux_clock = clock;
  assign accOutputDemux_reset = reset;
  assign accOutputDemux_io_enq_valid = _io_wrote_valid_T ? _GEN_58 : _GEN_167; // @[AccumulatorWithALUArray.scala 133:16]
  assign accOutputDemux_io_enq_bits = _io_wrote_valid_T ? 1'h0 : _GEN_166; // @[AccumulatorWithALUArray.scala 133:16]
  assign accOutputDemux_io_deq_ready = accOutputDemux_x24_demux_io_sel_ready; // @[Mem.scala 24:7]
  assign accWriteEnqueuer_clock = clock;
  assign accWriteEnqueuer_reset = reset;
  assign accWriteEnqueuer_io_in_valid = _io_wrote_valid_T & _GEN_43; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 40:17]
  assign accWriteEnqueuer_io_out_0_ready = _io_wrote_valid_T & _GEN_44; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 42:18]
  assign accWriteEnqueuer_io_out_1_ready = _io_wrote_valid_T & _GEN_49; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 42:18]
  assign accReadEnqueuer_clock = clock;
  assign accReadEnqueuer_reset = reset;
  assign accReadEnqueuer_io_in_valid = _io_wrote_valid_T & _GEN_54; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 40:17]
  assign accReadEnqueuer_io_out_0_ready = _io_wrote_valid_T & _GEN_55; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 42:18]
  assign accReadEnqueuer_io_out_1_ready = _io_wrote_valid_T & _GEN_56; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 42:18]
  assign simdRWWriteEnqueuer_clock = clock;
  assign simdRWWriteEnqueuer_reset = reset;
  assign simdRWWriteEnqueuer_io_in_valid = _io_wrote_valid_T ? 1'h0 : _GEN_149; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 40:17]
  assign simdRWWriteEnqueuer_io_out_0_ready = _io_wrote_valid_T ? 1'h0 : _GEN_150; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 42:18]
  assign simdRWWriteEnqueuer_io_out_1_ready = _io_wrote_valid_T ? 1'h0 : _GEN_155; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 42:18]
  assign simdRWWriteEnqueuer_io_out_2_ready = _io_wrote_valid_T ? 1'h0 : _GEN_158; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 42:18]
  assign simdRWReadEnqueuer_clock = clock;
  assign simdRWReadEnqueuer_reset = reset;
  assign simdRWReadEnqueuer_io_in_valid = _io_wrote_valid_T ? 1'h0 : _GEN_163; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 40:17]
  assign simdRWReadEnqueuer_io_out_0_ready = _io_wrote_valid_T ? 1'h0 : _GEN_164; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 42:18]
  assign simdRWReadEnqueuer_io_out_1_ready = _io_wrote_valid_T ? 1'h0 : _GEN_165; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 42:18]
  assign simdRWReadEnqueuer_io_out_2_ready = _io_wrote_valid_T ? 1'h0 : _GEN_168; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 42:18]
  assign simdWriteEnqueuer_clock = clock;
  assign simdWriteEnqueuer_reset = reset;
  assign simdWriteEnqueuer_io_in_valid = _io_wrote_valid_T ? 1'h0 : _GEN_179; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 40:17]
  assign simdWriteEnqueuer_io_out_0_ready = _io_wrote_valid_T ? 1'h0 : _GEN_180; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 42:18]
  assign simdWriteEnqueuer_io_out_1_ready = _io_wrote_valid_T ? 1'h0 : _GEN_181; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 42:18]
  assign simdWriteEnqueuer_io_out_2_ready = _io_wrote_valid_T ? 1'h0 : _GEN_182; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 42:18]
  assign simdWriteEnqueuer_io_out_3_ready = _io_wrote_valid_T ? 1'h0 : _GEN_183; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 42:18]
  assign simdReadEnqueuer_clock = clock;
  assign simdReadEnqueuer_reset = reset;
  assign simdReadEnqueuer_io_in_valid = _io_wrote_valid_T ? 1'h0 : _GEN_174; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 40:17]
  assign simdReadEnqueuer_io_out_0_ready = _io_wrote_valid_T ? 1'h0 : _GEN_175; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 42:18]
  assign simdReadEnqueuer_io_out_1_ready = _io_wrote_valid_T ? 1'h0 : _GEN_176; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 42:18]
  assign simdReadEnqueuer_io_out_2_ready = _io_wrote_valid_T ? 1'h0 : _GEN_177; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 42:18]
  assign simdReadEnqueuer_io_out_3_ready = _io_wrote_valid_T ? 1'h0 : _GEN_178; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 42:18]
  assign simdEnqueuer_clock = clock;
  assign simdEnqueuer_reset = reset;
  assign simdEnqueuer_io_in_valid = _io_wrote_valid_T ? 1'h0 : _GEN_184; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 40:17]
  assign simdEnqueuer_io_out_0_ready = _io_wrote_valid_T ? 1'h0 : _GEN_185; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 42:18]
  assign simdEnqueuer_io_out_1_ready = _io_wrote_valid_T ? 1'h0 : _GEN_186; // @[AccumulatorWithALUArray.scala 133:16 MultiEnqueue.scala 42:18]
  always @(posedge clock) begin
    if (reset) begin // @[AccumulatorWithALUArray.scala 110:29]
      readEnqueued <= 1'h0; // @[AccumulatorWithALUArray.scala 110:29]
    end else if (_io_wrote_valid_T) begin // @[AccumulatorWithALUArray.scala 133:16]
      readEnqueued <= _GEN_53;
    end else begin
      readEnqueued <= _GEN_162;
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
  readEnqueued = _RAND_0[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Demux_4(
  output        io_in_ready,
  input         io_in_valid,
  input  [15:0] io_in_bits_0,
  input  [15:0] io_in_bits_1,
  input  [15:0] io_in_bits_2,
  input  [15:0] io_in_bits_3,
  input  [15:0] io_in_bits_4,
  input  [15:0] io_in_bits_5,
  input  [15:0] io_in_bits_6,
  input  [15:0] io_in_bits_7,
  output        io_sel_ready,
  input         io_sel_valid,
  input  [1:0]  io_sel_bits,
  input         io_out_0_ready,
  output        io_out_0_valid,
  output [15:0] io_out_0_bits_0,
  output [15:0] io_out_0_bits_1,
  output [15:0] io_out_0_bits_2,
  output [15:0] io_out_0_bits_3,
  output [15:0] io_out_0_bits_4,
  output [15:0] io_out_0_bits_5,
  output [15:0] io_out_0_bits_6,
  output [15:0] io_out_0_bits_7,
  input         io_out_1_ready,
  output        io_out_1_valid,
  output [15:0] io_out_1_bits_0,
  output [15:0] io_out_1_bits_1,
  output [15:0] io_out_1_bits_2,
  output [15:0] io_out_1_bits_3,
  output [15:0] io_out_1_bits_4,
  output [15:0] io_out_1_bits_5,
  output [15:0] io_out_1_bits_6,
  output [15:0] io_out_1_bits_7,
  input         io_out_2_ready,
  output        io_out_2_valid,
  output [15:0] io_out_2_bits_0,
  output [15:0] io_out_2_bits_1,
  output [15:0] io_out_2_bits_2,
  output [15:0] io_out_2_bits_3,
  output [15:0] io_out_2_bits_4,
  output [15:0] io_out_2_bits_5,
  output [15:0] io_out_2_bits_6,
  output [15:0] io_out_2_bits_7
);
  wire  _GEN_28 = 2'h1 == io_sel_bits ? io_out_1_ready : io_out_0_ready; // @[Demux.scala 34:25 Demux.scala 34:25]
  wire  _GEN_29 = 2'h2 == io_sel_bits ? io_out_2_ready : _GEN_28; // @[Demux.scala 34:25 Demux.scala 34:25]
  assign io_in_ready = io_sel_valid & _GEN_29; // @[Demux.scala 35:25]
  assign io_sel_ready = io_in_valid & _GEN_29; // @[Demux.scala 34:25]
  assign io_out_0_valid = 2'h0 == io_sel_bits & (io_sel_valid & io_in_valid); // @[Demux.scala 33:13 Demux.scala 33:13 Demux.scala 28:15]
  assign io_out_0_bits_0 = 2'h0 == io_sel_bits ? $signed(io_in_bits_0) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_0_bits_1 = 2'h0 == io_sel_bits ? $signed(io_in_bits_1) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_0_bits_2 = 2'h0 == io_sel_bits ? $signed(io_in_bits_2) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_0_bits_3 = 2'h0 == io_sel_bits ? $signed(io_in_bits_3) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_0_bits_4 = 2'h0 == io_sel_bits ? $signed(io_in_bits_4) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_0_bits_5 = 2'h0 == io_sel_bits ? $signed(io_in_bits_5) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_0_bits_6 = 2'h0 == io_sel_bits ? $signed(io_in_bits_6) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_0_bits_7 = 2'h0 == io_sel_bits ? $signed(io_in_bits_7) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_1_valid = 2'h1 == io_sel_bits & (io_sel_valid & io_in_valid); // @[Demux.scala 33:13 Demux.scala 33:13 Demux.scala 28:15]
  assign io_out_1_bits_0 = 2'h1 == io_sel_bits ? $signed(io_in_bits_0) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_1_bits_1 = 2'h1 == io_sel_bits ? $signed(io_in_bits_1) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_1_bits_2 = 2'h1 == io_sel_bits ? $signed(io_in_bits_2) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_1_bits_3 = 2'h1 == io_sel_bits ? $signed(io_in_bits_3) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_1_bits_4 = 2'h1 == io_sel_bits ? $signed(io_in_bits_4) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_1_bits_5 = 2'h1 == io_sel_bits ? $signed(io_in_bits_5) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_1_bits_6 = 2'h1 == io_sel_bits ? $signed(io_in_bits_6) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_1_bits_7 = 2'h1 == io_sel_bits ? $signed(io_in_bits_7) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_2_valid = 2'h2 == io_sel_bits & (io_sel_valid & io_in_valid); // @[Demux.scala 33:13 Demux.scala 33:13 Demux.scala 28:15]
  assign io_out_2_bits_0 = 2'h2 == io_sel_bits ? $signed(io_in_bits_0) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_2_bits_1 = 2'h2 == io_sel_bits ? $signed(io_in_bits_1) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_2_bits_2 = 2'h2 == io_sel_bits ? $signed(io_in_bits_2) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_2_bits_3 = 2'h2 == io_sel_bits ? $signed(io_in_bits_3) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_2_bits_4 = 2'h2 == io_sel_bits ? $signed(io_in_bits_4) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_2_bits_5 = 2'h2 == io_sel_bits ? $signed(io_in_bits_5) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_2_bits_6 = 2'h2 == io_sel_bits ? $signed(io_in_bits_6) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
  assign io_out_2_bits_7 = 2'h2 == io_sel_bits ? $signed(io_in_bits_7) : $signed(16'sh0); // @[Demux.scala 32:12 Demux.scala 32:12 Demux.scala 27:14]
endmodule
module SizeHandler_3(
  input        clock,
  input        reset,
  output       io_in_ready,
  input        io_in_valid,
  input        io_in_bits_sel,
  input  [5:0] io_in_bits_size,
  input        io_out_ready,
  output       io_out_valid,
  output       io_out_bits_sel
);
  wire  sizeCounter_clock; // @[Counter.scala 34:19]
  wire  sizeCounter_reset; // @[Counter.scala 34:19]
  wire  sizeCounter_io_value_ready; // @[Counter.scala 34:19]
  wire [5:0] sizeCounter_io_value_bits; // @[Counter.scala 34:19]
  wire  sizeCounter_io_resetValue; // @[Counter.scala 34:19]
  wire  fire = io_in_valid & io_out_ready; // @[SizeHandler.scala 32:23]
  Counter_2 sizeCounter ( // @[Counter.scala 34:19]
    .clock(sizeCounter_clock),
    .reset(sizeCounter_reset),
    .io_value_ready(sizeCounter_io_value_ready),
    .io_value_bits(sizeCounter_io_value_bits),
    .io_resetValue(sizeCounter_io_resetValue)
  );
  assign io_in_ready = sizeCounter_io_value_bits == io_in_bits_size & io_out_ready; // @[SizeHandler.scala 34:52 SizeHandler.scala 35:14 SizeHandler.scala 38:14]
  assign io_out_valid = io_in_valid; // @[SizeHandler.scala 25:16]
  assign io_out_bits_sel = io_in_bits_sel; // @[SizeHandler.scala 28:34]
  assign sizeCounter_clock = clock;
  assign sizeCounter_reset = reset;
  assign sizeCounter_io_value_ready = sizeCounter_io_value_bits == io_in_bits_size ? 1'h0 : fire; // @[SizeHandler.scala 34:52 Counter.scala 36:22 SizeHandler.scala 39:32]
  assign sizeCounter_io_resetValue = sizeCounter_io_value_bits == io_in_bits_size & fire; // @[SizeHandler.scala 34:52 SizeHandler.scala 36:31 Counter.scala 35:21]
endmodule
module LocalRouter(
  input         clock,
  input         reset,
  output        io_control_ready,
  input         io_control_valid,
  input  [3:0]  io_control_bits_kind,
  input  [5:0]  io_control_bits_size,
  output        io_mem_output_ready,
  input         io_mem_output_valid,
  input  [15:0] io_mem_output_bits_0,
  input  [15:0] io_mem_output_bits_1,
  input  [15:0] io_mem_output_bits_2,
  input  [15:0] io_mem_output_bits_3,
  input  [15:0] io_mem_output_bits_4,
  input  [15:0] io_mem_output_bits_5,
  input  [15:0] io_mem_output_bits_6,
  input  [15:0] io_mem_output_bits_7,
  input         io_mem_input_ready,
  output        io_mem_input_valid,
  output [15:0] io_mem_input_bits_0,
  output [15:0] io_mem_input_bits_1,
  output [15:0] io_mem_input_bits_2,
  output [15:0] io_mem_input_bits_3,
  output [15:0] io_mem_input_bits_4,
  output [15:0] io_mem_input_bits_5,
  output [15:0] io_mem_input_bits_6,
  output [15:0] io_mem_input_bits_7,
  input         io_array_input_ready,
  output        io_array_input_valid,
  output [15:0] io_array_input_bits_0,
  output [15:0] io_array_input_bits_1,
  output [15:0] io_array_input_bits_2,
  output [15:0] io_array_input_bits_3,
  output [15:0] io_array_input_bits_4,
  output [15:0] io_array_input_bits_5,
  output [15:0] io_array_input_bits_6,
  output [15:0] io_array_input_bits_7,
  output        io_array_output_ready,
  input         io_array_output_valid,
  input  [15:0] io_array_output_bits_0,
  input  [15:0] io_array_output_bits_1,
  input  [15:0] io_array_output_bits_2,
  input  [15:0] io_array_output_bits_3,
  input  [15:0] io_array_output_bits_4,
  input  [15:0] io_array_output_bits_5,
  input  [15:0] io_array_output_bits_6,
  input  [15:0] io_array_output_bits_7,
  input         io_array_weightInput_ready,
  output        io_array_weightInput_valid,
  output [15:0] io_array_weightInput_bits_0,
  output [15:0] io_array_weightInput_bits_1,
  output [15:0] io_array_weightInput_bits_2,
  output [15:0] io_array_weightInput_bits_3,
  output [15:0] io_array_weightInput_bits_4,
  output [15:0] io_array_weightInput_bits_5,
  output [15:0] io_array_weightInput_bits_6,
  output [15:0] io_array_weightInput_bits_7,
  output        io_acc_output_ready,
  input         io_acc_output_valid,
  input  [15:0] io_acc_output_bits_0,
  input  [15:0] io_acc_output_bits_1,
  input  [15:0] io_acc_output_bits_2,
  input  [15:0] io_acc_output_bits_3,
  input  [15:0] io_acc_output_bits_4,
  input  [15:0] io_acc_output_bits_5,
  input  [15:0] io_acc_output_bits_6,
  input  [15:0] io_acc_output_bits_7,
  input         io_acc_input_ready,
  output        io_acc_input_valid,
  output [15:0] io_acc_input_bits_0,
  output [15:0] io_acc_input_bits_1,
  output [15:0] io_acc_input_bits_2,
  output [15:0] io_acc_input_bits_3,
  output [15:0] io_acc_input_bits_4,
  output [15:0] io_acc_input_bits_5,
  output [15:0] io_acc_input_bits_6,
  output [15:0] io_acc_input_bits_7,
  input         io_timeout,
  input         io_tracepoint,
  input  [31:0] io_programCounter
);
  wire  memReadDataDemuxModule_io_in_ready; // @[LocalRouter.scala 55:38]
  wire  memReadDataDemuxModule_io_in_valid; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_in_bits_0; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_in_bits_1; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_in_bits_2; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_in_bits_3; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_in_bits_4; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_in_bits_5; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_in_bits_6; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_in_bits_7; // @[LocalRouter.scala 55:38]
  wire  memReadDataDemuxModule_io_sel_ready; // @[LocalRouter.scala 55:38]
  wire  memReadDataDemuxModule_io_sel_valid; // @[LocalRouter.scala 55:38]
  wire [1:0] memReadDataDemuxModule_io_sel_bits; // @[LocalRouter.scala 55:38]
  wire  memReadDataDemuxModule_io_out_0_ready; // @[LocalRouter.scala 55:38]
  wire  memReadDataDemuxModule_io_out_0_valid; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_out_0_bits_0; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_out_0_bits_1; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_out_0_bits_2; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_out_0_bits_3; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_out_0_bits_4; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_out_0_bits_5; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_out_0_bits_6; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_out_0_bits_7; // @[LocalRouter.scala 55:38]
  wire  memReadDataDemuxModule_io_out_1_ready; // @[LocalRouter.scala 55:38]
  wire  memReadDataDemuxModule_io_out_1_valid; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_out_1_bits_0; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_out_1_bits_1; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_out_1_bits_2; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_out_1_bits_3; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_out_1_bits_4; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_out_1_bits_5; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_out_1_bits_6; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_out_1_bits_7; // @[LocalRouter.scala 55:38]
  wire  memReadDataDemuxModule_io_out_2_ready; // @[LocalRouter.scala 55:38]
  wire  memReadDataDemuxModule_io_out_2_valid; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_out_2_bits_0; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_out_2_bits_1; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_out_2_bits_2; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_out_2_bits_3; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_out_2_bits_4; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_out_2_bits_5; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_out_2_bits_6; // @[LocalRouter.scala 55:38]
  wire [15:0] memReadDataDemuxModule_io_out_2_bits_7; // @[LocalRouter.scala 55:38]
  wire  memWriteDataMuxModule_io_in_0_ready; // @[LocalRouter.scala 66:37]
  wire  memWriteDataMuxModule_io_in_0_valid; // @[LocalRouter.scala 66:37]
  wire [15:0] memWriteDataMuxModule_io_in_0_bits_0; // @[LocalRouter.scala 66:37]
  wire [15:0] memWriteDataMuxModule_io_in_0_bits_1; // @[LocalRouter.scala 66:37]
  wire [15:0] memWriteDataMuxModule_io_in_0_bits_2; // @[LocalRouter.scala 66:37]
  wire [15:0] memWriteDataMuxModule_io_in_0_bits_3; // @[LocalRouter.scala 66:37]
  wire [15:0] memWriteDataMuxModule_io_in_0_bits_4; // @[LocalRouter.scala 66:37]
  wire [15:0] memWriteDataMuxModule_io_in_0_bits_5; // @[LocalRouter.scala 66:37]
  wire [15:0] memWriteDataMuxModule_io_in_0_bits_6; // @[LocalRouter.scala 66:37]
  wire [15:0] memWriteDataMuxModule_io_in_0_bits_7; // @[LocalRouter.scala 66:37]
  wire  memWriteDataMuxModule_io_in_1_ready; // @[LocalRouter.scala 66:37]
  wire  memWriteDataMuxModule_io_in_1_valid; // @[LocalRouter.scala 66:37]
  wire [15:0] memWriteDataMuxModule_io_in_1_bits_0; // @[LocalRouter.scala 66:37]
  wire [15:0] memWriteDataMuxModule_io_in_1_bits_1; // @[LocalRouter.scala 66:37]
  wire [15:0] memWriteDataMuxModule_io_in_1_bits_2; // @[LocalRouter.scala 66:37]
  wire [15:0] memWriteDataMuxModule_io_in_1_bits_3; // @[LocalRouter.scala 66:37]
  wire [15:0] memWriteDataMuxModule_io_in_1_bits_4; // @[LocalRouter.scala 66:37]
  wire [15:0] memWriteDataMuxModule_io_in_1_bits_5; // @[LocalRouter.scala 66:37]
  wire [15:0] memWriteDataMuxModule_io_in_1_bits_6; // @[LocalRouter.scala 66:37]
  wire [15:0] memWriteDataMuxModule_io_in_1_bits_7; // @[LocalRouter.scala 66:37]
  wire  memWriteDataMuxModule_io_sel_ready; // @[LocalRouter.scala 66:37]
  wire  memWriteDataMuxModule_io_sel_valid; // @[LocalRouter.scala 66:37]
  wire  memWriteDataMuxModule_io_sel_bits; // @[LocalRouter.scala 66:37]
  wire  memWriteDataMuxModule_io_out_ready; // @[LocalRouter.scala 66:37]
  wire  memWriteDataMuxModule_io_out_valid; // @[LocalRouter.scala 66:37]
  wire [15:0] memWriteDataMuxModule_io_out_bits_0; // @[LocalRouter.scala 66:37]
  wire [15:0] memWriteDataMuxModule_io_out_bits_1; // @[LocalRouter.scala 66:37]
  wire [15:0] memWriteDataMuxModule_io_out_bits_2; // @[LocalRouter.scala 66:37]
  wire [15:0] memWriteDataMuxModule_io_out_bits_3; // @[LocalRouter.scala 66:37]
  wire [15:0] memWriteDataMuxModule_io_out_bits_4; // @[LocalRouter.scala 66:37]
  wire [15:0] memWriteDataMuxModule_io_out_bits_5; // @[LocalRouter.scala 66:37]
  wire [15:0] memWriteDataMuxModule_io_out_bits_6; // @[LocalRouter.scala 66:37]
  wire [15:0] memWriteDataMuxModule_io_out_bits_7; // @[LocalRouter.scala 66:37]
  wire  accWriteDataMuxModule_io_in_0_ready; // @[LocalRouter.scala 77:37]
  wire  accWriteDataMuxModule_io_in_0_valid; // @[LocalRouter.scala 77:37]
  wire [15:0] accWriteDataMuxModule_io_in_0_bits_0; // @[LocalRouter.scala 77:37]
  wire [15:0] accWriteDataMuxModule_io_in_0_bits_1; // @[LocalRouter.scala 77:37]
  wire [15:0] accWriteDataMuxModule_io_in_0_bits_2; // @[LocalRouter.scala 77:37]
  wire [15:0] accWriteDataMuxModule_io_in_0_bits_3; // @[LocalRouter.scala 77:37]
  wire [15:0] accWriteDataMuxModule_io_in_0_bits_4; // @[LocalRouter.scala 77:37]
  wire [15:0] accWriteDataMuxModule_io_in_0_bits_5; // @[LocalRouter.scala 77:37]
  wire [15:0] accWriteDataMuxModule_io_in_0_bits_6; // @[LocalRouter.scala 77:37]
  wire [15:0] accWriteDataMuxModule_io_in_0_bits_7; // @[LocalRouter.scala 77:37]
  wire  accWriteDataMuxModule_io_in_1_ready; // @[LocalRouter.scala 77:37]
  wire  accWriteDataMuxModule_io_in_1_valid; // @[LocalRouter.scala 77:37]
  wire [15:0] accWriteDataMuxModule_io_in_1_bits_0; // @[LocalRouter.scala 77:37]
  wire [15:0] accWriteDataMuxModule_io_in_1_bits_1; // @[LocalRouter.scala 77:37]
  wire [15:0] accWriteDataMuxModule_io_in_1_bits_2; // @[LocalRouter.scala 77:37]
  wire [15:0] accWriteDataMuxModule_io_in_1_bits_3; // @[LocalRouter.scala 77:37]
  wire [15:0] accWriteDataMuxModule_io_in_1_bits_4; // @[LocalRouter.scala 77:37]
  wire [15:0] accWriteDataMuxModule_io_in_1_bits_5; // @[LocalRouter.scala 77:37]
  wire [15:0] accWriteDataMuxModule_io_in_1_bits_6; // @[LocalRouter.scala 77:37]
  wire [15:0] accWriteDataMuxModule_io_in_1_bits_7; // @[LocalRouter.scala 77:37]
  wire  accWriteDataMuxModule_io_sel_ready; // @[LocalRouter.scala 77:37]
  wire  accWriteDataMuxModule_io_sel_valid; // @[LocalRouter.scala 77:37]
  wire  accWriteDataMuxModule_io_sel_bits; // @[LocalRouter.scala 77:37]
  wire  accWriteDataMuxModule_io_out_ready; // @[LocalRouter.scala 77:37]
  wire  accWriteDataMuxModule_io_out_valid; // @[LocalRouter.scala 77:37]
  wire [15:0] accWriteDataMuxModule_io_out_bits_0; // @[LocalRouter.scala 77:37]
  wire [15:0] accWriteDataMuxModule_io_out_bits_1; // @[LocalRouter.scala 77:37]
  wire [15:0] accWriteDataMuxModule_io_out_bits_2; // @[LocalRouter.scala 77:37]
  wire [15:0] accWriteDataMuxModule_io_out_bits_3; // @[LocalRouter.scala 77:37]
  wire [15:0] accWriteDataMuxModule_io_out_bits_4; // @[LocalRouter.scala 77:37]
  wire [15:0] accWriteDataMuxModule_io_out_bits_5; // @[LocalRouter.scala 77:37]
  wire [15:0] accWriteDataMuxModule_io_out_bits_6; // @[LocalRouter.scala 77:37]
  wire [15:0] accWriteDataMuxModule_io_out_bits_7; // @[LocalRouter.scala 77:37]
  wire  sizeHandler_clock; // @[package.scala 31:29]
  wire  sizeHandler_reset; // @[package.scala 31:29]
  wire  sizeHandler_io_in_ready; // @[package.scala 31:29]
  wire  sizeHandler_io_in_valid; // @[package.scala 31:29]
  wire [1:0] sizeHandler_io_in_bits_kind; // @[package.scala 31:29]
  wire [5:0] sizeHandler_io_in_bits_size; // @[package.scala 31:29]
  wire  sizeHandler_io_out_ready; // @[package.scala 31:29]
  wire  sizeHandler_io_out_valid; // @[package.scala 31:29]
  wire [1:0] sizeHandler_io_out_bits_kind; // @[package.scala 31:29]
  wire  sizeHandler_1_clock; // @[package.scala 31:29]
  wire  sizeHandler_1_reset; // @[package.scala 31:29]
  wire  sizeHandler_1_io_in_ready; // @[package.scala 31:29]
  wire  sizeHandler_1_io_in_valid; // @[package.scala 31:29]
  wire  sizeHandler_1_io_in_bits_sel; // @[package.scala 31:29]
  wire [5:0] sizeHandler_1_io_in_bits_size; // @[package.scala 31:29]
  wire  sizeHandler_1_io_out_ready; // @[package.scala 31:29]
  wire  sizeHandler_1_io_out_valid; // @[package.scala 31:29]
  wire  sizeHandler_1_io_out_bits_sel; // @[package.scala 31:29]
  wire  sizeHandler_2_clock; // @[package.scala 31:29]
  wire  sizeHandler_2_reset; // @[package.scala 31:29]
  wire  sizeHandler_2_io_in_ready; // @[package.scala 31:29]
  wire  sizeHandler_2_io_in_valid; // @[package.scala 31:29]
  wire  sizeHandler_2_io_in_bits_sel; // @[package.scala 31:29]
  wire [5:0] sizeHandler_2_io_in_bits_size; // @[package.scala 31:29]
  wire  sizeHandler_2_io_out_ready; // @[package.scala 31:29]
  wire  sizeHandler_2_io_out_valid; // @[package.scala 31:29]
  wire  sizeHandler_2_io_out_bits_sel; // @[package.scala 31:29]
  wire  enqueuer1_clock; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer1_reset; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer1_io_in_ready; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer1_io_in_valid; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer1_io_out_0_ready; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer1_io_out_0_valid; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer2_clock; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer2_reset; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer2_io_in_ready; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer2_io_in_valid; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer2_io_out_0_ready; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer2_io_out_0_valid; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer2_io_out_1_ready; // @[MultiEnqueue.scala 160:43]
  wire  enqueuer2_io_out_1_valid; // @[MultiEnqueue.scala 160:43]
  wire  _T_3 = io_control_bits_kind == 4'h4; // @[LocalRouter.scala 142:32]
  wire  _T_4 = io_control_bits_kind == 4'h5; // @[LocalRouter.scala 149:23]
  wire  _GEN_0 = _T_4 & io_control_valid; // @[LocalRouter.scala 150:5 MultiEnqueue.scala 84:17 MultiEnqueue.scala 40:17]
  wire  io_control_ready_sizeHandler_io_in_w_5_ready = sizeHandler_io_in_ready; // @[ReadyValid.scala 16:17 MultiEnqueue.scala 85:10]
  wire  _GEN_1 = _T_4 & io_control_ready_sizeHandler_io_in_w_5_ready; // @[LocalRouter.scala 150:5 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire [5:0] _GEN_2 = _T_4 ? io_control_bits_size : 6'h0; // @[LocalRouter.scala 150:5 MultiEnqueue.scala 85:10 package.scala 409:14]
  wire [1:0] _GEN_3 = _T_4 ? 2'h2 : 2'h0; // @[LocalRouter.scala 150:5 MultiEnqueue.scala 85:10 package.scala 409:14]
  wire  io_control_ready_sizeHandler_io_in_w_5_valid = enqueuer2_io_out_0_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_4 = _T_4 & io_control_ready_sizeHandler_io_in_w_5_valid; // @[LocalRouter.scala 150:5 MultiEnqueue.scala 85:10 package.scala 410:15]
  wire  io_control_ready_sizeHandler_io_in_w_6_ready = sizeHandler_2_io_in_ready; // @[ReadyValid.scala 16:17 MultiEnqueue.scala 86:10]
  wire  _GEN_5 = _T_4 & io_control_ready_sizeHandler_io_in_w_6_ready; // @[LocalRouter.scala 150:5 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire  io_control_ready_sizeHandler_io_in_w_6_valid = enqueuer2_io_out_1_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_8 = _T_4 & io_control_ready_sizeHandler_io_in_w_6_valid; // @[LocalRouter.scala 150:5 MultiEnqueue.scala 86:10 package.scala 410:15]
  wire  _GEN_9 = _T_4 ? enqueuer2_io_in_ready : 1'h1; // @[LocalRouter.scala 150:5 LocalRouter.scala 151:19 LocalRouter.scala 159:19]
  wire  _GEN_10 = io_control_bits_kind == 4'h4 & io_control_valid; // @[LocalRouter.scala 142:78 MultiEnqueue.scala 60:17 MultiEnqueue.scala 40:17]
  wire  io_control_ready_sizeHandler_io_in_w_4_ready = sizeHandler_1_io_in_ready; // @[ReadyValid.scala 16:17 MultiEnqueue.scala 61:10]
  wire  _GEN_11 = io_control_bits_kind == 4'h4 & io_control_ready_sizeHandler_io_in_w_4_ready; // @[LocalRouter.scala 142:78 ReadyValid.scala 19:11 MultiEnqueue.scala 42:18]
  wire [5:0] _GEN_12 = io_control_bits_kind == 4'h4 ? io_control_bits_size : 6'h0; // @[LocalRouter.scala 142:78 MultiEnqueue.scala 61:10 package.scala 409:14]
  wire  io_control_ready_sizeHandler_io_in_w_4_valid = enqueuer1_io_out_0_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_14 = io_control_bits_kind == 4'h4 & io_control_ready_sizeHandler_io_in_w_4_valid; // @[LocalRouter.scala 142:78 MultiEnqueue.scala 61:10 package.scala 410:15]
  wire  _GEN_15 = io_control_bits_kind == 4'h4 ? enqueuer1_io_in_ready : _GEN_9; // @[LocalRouter.scala 142:78 LocalRouter.scala 143:19]
  wire  _GEN_16 = io_control_bits_kind == 4'h4 ? 1'h0 : _GEN_0; // @[LocalRouter.scala 142:78 MultiEnqueue.scala 40:17]
  wire  _GEN_17 = io_control_bits_kind == 4'h4 ? 1'h0 : _GEN_1; // @[LocalRouter.scala 142:78 MultiEnqueue.scala 42:18]
  wire [5:0] _GEN_18 = io_control_bits_kind == 4'h4 ? 6'h0 : _GEN_2; // @[LocalRouter.scala 142:78 package.scala 409:14]
  wire [1:0] _GEN_19 = io_control_bits_kind == 4'h4 ? 2'h0 : _GEN_3; // @[LocalRouter.scala 142:78 package.scala 409:14]
  wire  _GEN_20 = io_control_bits_kind == 4'h4 ? 1'h0 : _GEN_4; // @[LocalRouter.scala 142:78 package.scala 410:15]
  wire  _GEN_21 = io_control_bits_kind == 4'h4 ? 1'h0 : _GEN_5; // @[LocalRouter.scala 142:78 MultiEnqueue.scala 42:18]
  wire  _GEN_23 = io_control_bits_kind == 4'h4 ? 1'h0 : _T_4; // @[LocalRouter.scala 142:78 package.scala 409:14]
  wire  _GEN_24 = io_control_bits_kind == 4'h4 ? 1'h0 : _GEN_8; // @[LocalRouter.scala 142:78 package.scala 410:15]
  wire  _GEN_25 = io_control_bits_kind == 4'h3 ? io_control_valid : _GEN_10; // @[LocalRouter.scala 136:70 MultiEnqueue.scala 60:17]
  wire  _GEN_26 = io_control_bits_kind == 4'h3 ? io_control_ready_sizeHandler_io_in_w_6_ready : _GEN_11; // @[LocalRouter.scala 136:70 ReadyValid.scala 19:11]
  wire [5:0] _GEN_27 = io_control_bits_kind == 4'h3 ? io_control_bits_size : _GEN_18; // @[LocalRouter.scala 136:70 MultiEnqueue.scala 61:10]
  wire  _GEN_28 = io_control_bits_kind == 4'h3 ? 1'h0 : _GEN_23; // @[LocalRouter.scala 136:70 MultiEnqueue.scala 61:10]
  wire  _GEN_29 = io_control_bits_kind == 4'h3 ? io_control_ready_sizeHandler_io_in_w_4_valid : _GEN_24; // @[LocalRouter.scala 136:70 MultiEnqueue.scala 61:10]
  wire  _GEN_30 = io_control_bits_kind == 4'h3 ? enqueuer1_io_in_ready : _GEN_15; // @[LocalRouter.scala 136:70 LocalRouter.scala 137:19]
  wire [5:0] _GEN_31 = io_control_bits_kind == 4'h3 ? 6'h0 : _GEN_12; // @[LocalRouter.scala 136:70 package.scala 409:14]
  wire  _GEN_32 = io_control_bits_kind == 4'h3 ? 1'h0 : _T_3; // @[LocalRouter.scala 136:70 package.scala 409:14]
  wire  _GEN_33 = io_control_bits_kind == 4'h3 ? 1'h0 : _GEN_14; // @[LocalRouter.scala 136:70 package.scala 410:15]
  wire  _GEN_34 = io_control_bits_kind == 4'h3 ? 1'h0 : _GEN_16; // @[LocalRouter.scala 136:70 MultiEnqueue.scala 40:17]
  wire  _GEN_35 = io_control_bits_kind == 4'h3 ? 1'h0 : _GEN_17; // @[LocalRouter.scala 136:70 MultiEnqueue.scala 42:18]
  wire [5:0] _GEN_36 = io_control_bits_kind == 4'h3 ? 6'h0 : _GEN_18; // @[LocalRouter.scala 136:70 package.scala 409:14]
  wire [1:0] _GEN_37 = io_control_bits_kind == 4'h3 ? 2'h0 : _GEN_19; // @[LocalRouter.scala 136:70 package.scala 409:14]
  wire  _GEN_38 = io_control_bits_kind == 4'h3 ? 1'h0 : _GEN_20; // @[LocalRouter.scala 136:70 package.scala 410:15]
  wire  _GEN_39 = io_control_bits_kind == 4'h3 ? 1'h0 : _GEN_21; // @[LocalRouter.scala 136:70 MultiEnqueue.scala 42:18]
  wire  _GEN_40 = io_control_bits_kind == 4'h2 ? io_control_valid : _GEN_34; // @[LocalRouter.scala 128:78 MultiEnqueue.scala 84:17]
  wire  _GEN_41 = io_control_bits_kind == 4'h2 ? io_control_ready_sizeHandler_io_in_w_5_ready : _GEN_35; // @[LocalRouter.scala 128:78 ReadyValid.scala 19:11]
  wire [5:0] _GEN_42 = io_control_bits_kind == 4'h2 ? io_control_bits_size : _GEN_36; // @[LocalRouter.scala 128:78 MultiEnqueue.scala 85:10]
  wire [1:0] _GEN_43 = io_control_bits_kind == 4'h2 ? 2'h1 : _GEN_37; // @[LocalRouter.scala 128:78 MultiEnqueue.scala 85:10]
  wire  _GEN_44 = io_control_bits_kind == 4'h2 ? io_control_ready_sizeHandler_io_in_w_5_valid : _GEN_38; // @[LocalRouter.scala 128:78 MultiEnqueue.scala 85:10]
  wire  _GEN_45 = io_control_bits_kind == 4'h2 ? io_control_ready_sizeHandler_io_in_w_6_ready : _GEN_39; // @[LocalRouter.scala 128:78 ReadyValid.scala 19:11]
  wire [5:0] _GEN_46 = io_control_bits_kind == 4'h2 ? io_control_bits_size : _GEN_27; // @[LocalRouter.scala 128:78 MultiEnqueue.scala 86:10]
  wire  _GEN_47 = io_control_bits_kind == 4'h2 ? 1'h0 : _GEN_28; // @[LocalRouter.scala 128:78 MultiEnqueue.scala 86:10]
  wire  _GEN_48 = io_control_bits_kind == 4'h2 ? io_control_ready_sizeHandler_io_in_w_6_valid : _GEN_29; // @[LocalRouter.scala 128:78 MultiEnqueue.scala 86:10]
  wire  _GEN_49 = io_control_bits_kind == 4'h2 ? enqueuer2_io_in_ready : _GEN_30; // @[LocalRouter.scala 128:78 LocalRouter.scala 129:19]
  wire  _GEN_50 = io_control_bits_kind == 4'h2 ? 1'h0 : _GEN_25; // @[LocalRouter.scala 128:78 MultiEnqueue.scala 40:17]
  wire  _GEN_51 = io_control_bits_kind == 4'h2 ? 1'h0 : _GEN_26; // @[LocalRouter.scala 128:78 MultiEnqueue.scala 42:18]
  wire [5:0] _GEN_52 = io_control_bits_kind == 4'h2 ? 6'h0 : _GEN_31; // @[LocalRouter.scala 128:78 package.scala 409:14]
  wire  _GEN_53 = io_control_bits_kind == 4'h2 ? 1'h0 : _GEN_32; // @[LocalRouter.scala 128:78 package.scala 409:14]
  wire  _GEN_54 = io_control_bits_kind == 4'h2 ? 1'h0 : _GEN_33; // @[LocalRouter.scala 128:78 package.scala 410:15]
  Demux_4 memReadDataDemuxModule ( // @[LocalRouter.scala 55:38]
    .io_in_ready(memReadDataDemuxModule_io_in_ready),
    .io_in_valid(memReadDataDemuxModule_io_in_valid),
    .io_in_bits_0(memReadDataDemuxModule_io_in_bits_0),
    .io_in_bits_1(memReadDataDemuxModule_io_in_bits_1),
    .io_in_bits_2(memReadDataDemuxModule_io_in_bits_2),
    .io_in_bits_3(memReadDataDemuxModule_io_in_bits_3),
    .io_in_bits_4(memReadDataDemuxModule_io_in_bits_4),
    .io_in_bits_5(memReadDataDemuxModule_io_in_bits_5),
    .io_in_bits_6(memReadDataDemuxModule_io_in_bits_6),
    .io_in_bits_7(memReadDataDemuxModule_io_in_bits_7),
    .io_sel_ready(memReadDataDemuxModule_io_sel_ready),
    .io_sel_valid(memReadDataDemuxModule_io_sel_valid),
    .io_sel_bits(memReadDataDemuxModule_io_sel_bits),
    .io_out_0_ready(memReadDataDemuxModule_io_out_0_ready),
    .io_out_0_valid(memReadDataDemuxModule_io_out_0_valid),
    .io_out_0_bits_0(memReadDataDemuxModule_io_out_0_bits_0),
    .io_out_0_bits_1(memReadDataDemuxModule_io_out_0_bits_1),
    .io_out_0_bits_2(memReadDataDemuxModule_io_out_0_bits_2),
    .io_out_0_bits_3(memReadDataDemuxModule_io_out_0_bits_3),
    .io_out_0_bits_4(memReadDataDemuxModule_io_out_0_bits_4),
    .io_out_0_bits_5(memReadDataDemuxModule_io_out_0_bits_5),
    .io_out_0_bits_6(memReadDataDemuxModule_io_out_0_bits_6),
    .io_out_0_bits_7(memReadDataDemuxModule_io_out_0_bits_7),
    .io_out_1_ready(memReadDataDemuxModule_io_out_1_ready),
    .io_out_1_valid(memReadDataDemuxModule_io_out_1_valid),
    .io_out_1_bits_0(memReadDataDemuxModule_io_out_1_bits_0),
    .io_out_1_bits_1(memReadDataDemuxModule_io_out_1_bits_1),
    .io_out_1_bits_2(memReadDataDemuxModule_io_out_1_bits_2),
    .io_out_1_bits_3(memReadDataDemuxModule_io_out_1_bits_3),
    .io_out_1_bits_4(memReadDataDemuxModule_io_out_1_bits_4),
    .io_out_1_bits_5(memReadDataDemuxModule_io_out_1_bits_5),
    .io_out_1_bits_6(memReadDataDemuxModule_io_out_1_bits_6),
    .io_out_1_bits_7(memReadDataDemuxModule_io_out_1_bits_7),
    .io_out_2_ready(memReadDataDemuxModule_io_out_2_ready),
    .io_out_2_valid(memReadDataDemuxModule_io_out_2_valid),
    .io_out_2_bits_0(memReadDataDemuxModule_io_out_2_bits_0),
    .io_out_2_bits_1(memReadDataDemuxModule_io_out_2_bits_1),
    .io_out_2_bits_2(memReadDataDemuxModule_io_out_2_bits_2),
    .io_out_2_bits_3(memReadDataDemuxModule_io_out_2_bits_3),
    .io_out_2_bits_4(memReadDataDemuxModule_io_out_2_bits_4),
    .io_out_2_bits_5(memReadDataDemuxModule_io_out_2_bits_5),
    .io_out_2_bits_6(memReadDataDemuxModule_io_out_2_bits_6),
    .io_out_2_bits_7(memReadDataDemuxModule_io_out_2_bits_7)
  );
  Mux memWriteDataMuxModule ( // @[LocalRouter.scala 66:37]
    .io_in_0_ready(memWriteDataMuxModule_io_in_0_ready),
    .io_in_0_valid(memWriteDataMuxModule_io_in_0_valid),
    .io_in_0_bits_0(memWriteDataMuxModule_io_in_0_bits_0),
    .io_in_0_bits_1(memWriteDataMuxModule_io_in_0_bits_1),
    .io_in_0_bits_2(memWriteDataMuxModule_io_in_0_bits_2),
    .io_in_0_bits_3(memWriteDataMuxModule_io_in_0_bits_3),
    .io_in_0_bits_4(memWriteDataMuxModule_io_in_0_bits_4),
    .io_in_0_bits_5(memWriteDataMuxModule_io_in_0_bits_5),
    .io_in_0_bits_6(memWriteDataMuxModule_io_in_0_bits_6),
    .io_in_0_bits_7(memWriteDataMuxModule_io_in_0_bits_7),
    .io_in_1_ready(memWriteDataMuxModule_io_in_1_ready),
    .io_in_1_valid(memWriteDataMuxModule_io_in_1_valid),
    .io_in_1_bits_0(memWriteDataMuxModule_io_in_1_bits_0),
    .io_in_1_bits_1(memWriteDataMuxModule_io_in_1_bits_1),
    .io_in_1_bits_2(memWriteDataMuxModule_io_in_1_bits_2),
    .io_in_1_bits_3(memWriteDataMuxModule_io_in_1_bits_3),
    .io_in_1_bits_4(memWriteDataMuxModule_io_in_1_bits_4),
    .io_in_1_bits_5(memWriteDataMuxModule_io_in_1_bits_5),
    .io_in_1_bits_6(memWriteDataMuxModule_io_in_1_bits_6),
    .io_in_1_bits_7(memWriteDataMuxModule_io_in_1_bits_7),
    .io_sel_ready(memWriteDataMuxModule_io_sel_ready),
    .io_sel_valid(memWriteDataMuxModule_io_sel_valid),
    .io_sel_bits(memWriteDataMuxModule_io_sel_bits),
    .io_out_ready(memWriteDataMuxModule_io_out_ready),
    .io_out_valid(memWriteDataMuxModule_io_out_valid),
    .io_out_bits_0(memWriteDataMuxModule_io_out_bits_0),
    .io_out_bits_1(memWriteDataMuxModule_io_out_bits_1),
    .io_out_bits_2(memWriteDataMuxModule_io_out_bits_2),
    .io_out_bits_3(memWriteDataMuxModule_io_out_bits_3),
    .io_out_bits_4(memWriteDataMuxModule_io_out_bits_4),
    .io_out_bits_5(memWriteDataMuxModule_io_out_bits_5),
    .io_out_bits_6(memWriteDataMuxModule_io_out_bits_6),
    .io_out_bits_7(memWriteDataMuxModule_io_out_bits_7)
  );
  Mux accWriteDataMuxModule ( // @[LocalRouter.scala 77:37]
    .io_in_0_ready(accWriteDataMuxModule_io_in_0_ready),
    .io_in_0_valid(accWriteDataMuxModule_io_in_0_valid),
    .io_in_0_bits_0(accWriteDataMuxModule_io_in_0_bits_0),
    .io_in_0_bits_1(accWriteDataMuxModule_io_in_0_bits_1),
    .io_in_0_bits_2(accWriteDataMuxModule_io_in_0_bits_2),
    .io_in_0_bits_3(accWriteDataMuxModule_io_in_0_bits_3),
    .io_in_0_bits_4(accWriteDataMuxModule_io_in_0_bits_4),
    .io_in_0_bits_5(accWriteDataMuxModule_io_in_0_bits_5),
    .io_in_0_bits_6(accWriteDataMuxModule_io_in_0_bits_6),
    .io_in_0_bits_7(accWriteDataMuxModule_io_in_0_bits_7),
    .io_in_1_ready(accWriteDataMuxModule_io_in_1_ready),
    .io_in_1_valid(accWriteDataMuxModule_io_in_1_valid),
    .io_in_1_bits_0(accWriteDataMuxModule_io_in_1_bits_0),
    .io_in_1_bits_1(accWriteDataMuxModule_io_in_1_bits_1),
    .io_in_1_bits_2(accWriteDataMuxModule_io_in_1_bits_2),
    .io_in_1_bits_3(accWriteDataMuxModule_io_in_1_bits_3),
    .io_in_1_bits_4(accWriteDataMuxModule_io_in_1_bits_4),
    .io_in_1_bits_5(accWriteDataMuxModule_io_in_1_bits_5),
    .io_in_1_bits_6(accWriteDataMuxModule_io_in_1_bits_6),
    .io_in_1_bits_7(accWriteDataMuxModule_io_in_1_bits_7),
    .io_sel_ready(accWriteDataMuxModule_io_sel_ready),
    .io_sel_valid(accWriteDataMuxModule_io_sel_valid),
    .io_sel_bits(accWriteDataMuxModule_io_sel_bits),
    .io_out_ready(accWriteDataMuxModule_io_out_ready),
    .io_out_valid(accWriteDataMuxModule_io_out_valid),
    .io_out_bits_0(accWriteDataMuxModule_io_out_bits_0),
    .io_out_bits_1(accWriteDataMuxModule_io_out_bits_1),
    .io_out_bits_2(accWriteDataMuxModule_io_out_bits_2),
    .io_out_bits_3(accWriteDataMuxModule_io_out_bits_3),
    .io_out_bits_4(accWriteDataMuxModule_io_out_bits_4),
    .io_out_bits_5(accWriteDataMuxModule_io_out_bits_5),
    .io_out_bits_6(accWriteDataMuxModule_io_out_bits_6),
    .io_out_bits_7(accWriteDataMuxModule_io_out_bits_7)
  );
  SizeHandler_1 sizeHandler ( // @[package.scala 31:29]
    .clock(sizeHandler_clock),
    .reset(sizeHandler_reset),
    .io_in_ready(sizeHandler_io_in_ready),
    .io_in_valid(sizeHandler_io_in_valid),
    .io_in_bits_kind(sizeHandler_io_in_bits_kind),
    .io_in_bits_size(sizeHandler_io_in_bits_size),
    .io_out_ready(sizeHandler_io_out_ready),
    .io_out_valid(sizeHandler_io_out_valid),
    .io_out_bits_kind(sizeHandler_io_out_bits_kind)
  );
  SizeHandler_3 sizeHandler_1 ( // @[package.scala 31:29]
    .clock(sizeHandler_1_clock),
    .reset(sizeHandler_1_reset),
    .io_in_ready(sizeHandler_1_io_in_ready),
    .io_in_valid(sizeHandler_1_io_in_valid),
    .io_in_bits_sel(sizeHandler_1_io_in_bits_sel),
    .io_in_bits_size(sizeHandler_1_io_in_bits_size),
    .io_out_ready(sizeHandler_1_io_out_ready),
    .io_out_valid(sizeHandler_1_io_out_valid),
    .io_out_bits_sel(sizeHandler_1_io_out_bits_sel)
  );
  SizeHandler_3 sizeHandler_2 ( // @[package.scala 31:29]
    .clock(sizeHandler_2_clock),
    .reset(sizeHandler_2_reset),
    .io_in_ready(sizeHandler_2_io_in_ready),
    .io_in_valid(sizeHandler_2_io_in_valid),
    .io_in_bits_sel(sizeHandler_2_io_in_bits_sel),
    .io_in_bits_size(sizeHandler_2_io_in_bits_size),
    .io_out_ready(sizeHandler_2_io_out_ready),
    .io_out_valid(sizeHandler_2_io_out_valid),
    .io_out_bits_sel(sizeHandler_2_io_out_bits_sel)
  );
  MultiEnqueue enqueuer1 ( // @[MultiEnqueue.scala 160:43]
    .clock(enqueuer1_clock),
    .reset(enqueuer1_reset),
    .io_in_ready(enqueuer1_io_in_ready),
    .io_in_valid(enqueuer1_io_in_valid),
    .io_out_0_ready(enqueuer1_io_out_0_ready),
    .io_out_0_valid(enqueuer1_io_out_0_valid)
  );
  MultiEnqueue_1 enqueuer2 ( // @[MultiEnqueue.scala 160:43]
    .clock(enqueuer2_clock),
    .reset(enqueuer2_reset),
    .io_in_ready(enqueuer2_io_in_ready),
    .io_in_valid(enqueuer2_io_in_valid),
    .io_out_0_ready(enqueuer2_io_out_0_ready),
    .io_out_0_valid(enqueuer2_io_out_0_valid),
    .io_out_1_ready(enqueuer2_io_out_1_ready),
    .io_out_1_valid(enqueuer2_io_out_1_valid)
  );
  assign io_control_ready = io_control_bits_kind == 4'h1 ? enqueuer1_io_in_ready : _GEN_49; // @[LocalRouter.scala 122:72 LocalRouter.scala 123:19]
  assign io_mem_output_ready = memReadDataDemuxModule_io_in_ready; // @[LocalRouter.scala 62:32]
  assign io_mem_input_valid = memWriteDataMuxModule_io_out_valid; // @[LocalRouter.scala 75:16]
  assign io_mem_input_bits_0 = memWriteDataMuxModule_io_out_bits_0; // @[LocalRouter.scala 75:16]
  assign io_mem_input_bits_1 = memWriteDataMuxModule_io_out_bits_1; // @[LocalRouter.scala 75:16]
  assign io_mem_input_bits_2 = memWriteDataMuxModule_io_out_bits_2; // @[LocalRouter.scala 75:16]
  assign io_mem_input_bits_3 = memWriteDataMuxModule_io_out_bits_3; // @[LocalRouter.scala 75:16]
  assign io_mem_input_bits_4 = memWriteDataMuxModule_io_out_bits_4; // @[LocalRouter.scala 75:16]
  assign io_mem_input_bits_5 = memWriteDataMuxModule_io_out_bits_5; // @[LocalRouter.scala 75:16]
  assign io_mem_input_bits_6 = memWriteDataMuxModule_io_out_bits_6; // @[LocalRouter.scala 75:16]
  assign io_mem_input_bits_7 = memWriteDataMuxModule_io_out_bits_7; // @[LocalRouter.scala 75:16]
  assign io_array_input_valid = memReadDataDemuxModule_io_out_1_valid; // @[LocalRouter.scala 64:18]
  assign io_array_input_bits_0 = memReadDataDemuxModule_io_out_1_bits_0; // @[LocalRouter.scala 64:18]
  assign io_array_input_bits_1 = memReadDataDemuxModule_io_out_1_bits_1; // @[LocalRouter.scala 64:18]
  assign io_array_input_bits_2 = memReadDataDemuxModule_io_out_1_bits_2; // @[LocalRouter.scala 64:18]
  assign io_array_input_bits_3 = memReadDataDemuxModule_io_out_1_bits_3; // @[LocalRouter.scala 64:18]
  assign io_array_input_bits_4 = memReadDataDemuxModule_io_out_1_bits_4; // @[LocalRouter.scala 64:18]
  assign io_array_input_bits_5 = memReadDataDemuxModule_io_out_1_bits_5; // @[LocalRouter.scala 64:18]
  assign io_array_input_bits_6 = memReadDataDemuxModule_io_out_1_bits_6; // @[LocalRouter.scala 64:18]
  assign io_array_input_bits_7 = memReadDataDemuxModule_io_out_1_bits_7; // @[LocalRouter.scala 64:18]
  assign io_array_output_ready = accWriteDataMuxModule_io_in_0_ready; // @[LocalRouter.scala 84:34]
  assign io_array_weightInput_valid = memReadDataDemuxModule_io_out_0_valid; // @[LocalRouter.scala 63:24]
  assign io_array_weightInput_bits_0 = memReadDataDemuxModule_io_out_0_bits_0; // @[LocalRouter.scala 63:24]
  assign io_array_weightInput_bits_1 = memReadDataDemuxModule_io_out_0_bits_1; // @[LocalRouter.scala 63:24]
  assign io_array_weightInput_bits_2 = memReadDataDemuxModule_io_out_0_bits_2; // @[LocalRouter.scala 63:24]
  assign io_array_weightInput_bits_3 = memReadDataDemuxModule_io_out_0_bits_3; // @[LocalRouter.scala 63:24]
  assign io_array_weightInput_bits_4 = memReadDataDemuxModule_io_out_0_bits_4; // @[LocalRouter.scala 63:24]
  assign io_array_weightInput_bits_5 = memReadDataDemuxModule_io_out_0_bits_5; // @[LocalRouter.scala 63:24]
  assign io_array_weightInput_bits_6 = memReadDataDemuxModule_io_out_0_bits_6; // @[LocalRouter.scala 63:24]
  assign io_array_weightInput_bits_7 = memReadDataDemuxModule_io_out_0_bits_7; // @[LocalRouter.scala 63:24]
  assign io_acc_output_ready = memWriteDataMuxModule_io_in_1_ready; // @[LocalRouter.scala 74:34]
  assign io_acc_input_valid = accWriteDataMuxModule_io_out_valid; // @[LocalRouter.scala 86:16]
  assign io_acc_input_bits_0 = accWriteDataMuxModule_io_out_bits_0; // @[LocalRouter.scala 86:16]
  assign io_acc_input_bits_1 = accWriteDataMuxModule_io_out_bits_1; // @[LocalRouter.scala 86:16]
  assign io_acc_input_bits_2 = accWriteDataMuxModule_io_out_bits_2; // @[LocalRouter.scala 86:16]
  assign io_acc_input_bits_3 = accWriteDataMuxModule_io_out_bits_3; // @[LocalRouter.scala 86:16]
  assign io_acc_input_bits_4 = accWriteDataMuxModule_io_out_bits_4; // @[LocalRouter.scala 86:16]
  assign io_acc_input_bits_5 = accWriteDataMuxModule_io_out_bits_5; // @[LocalRouter.scala 86:16]
  assign io_acc_input_bits_6 = accWriteDataMuxModule_io_out_bits_6; // @[LocalRouter.scala 86:16]
  assign io_acc_input_bits_7 = accWriteDataMuxModule_io_out_bits_7; // @[LocalRouter.scala 86:16]
  assign memReadDataDemuxModule_io_in_valid = io_mem_output_valid; // @[LocalRouter.scala 62:32]
  assign memReadDataDemuxModule_io_in_bits_0 = io_mem_output_bits_0; // @[LocalRouter.scala 62:32]
  assign memReadDataDemuxModule_io_in_bits_1 = io_mem_output_bits_1; // @[LocalRouter.scala 62:32]
  assign memReadDataDemuxModule_io_in_bits_2 = io_mem_output_bits_2; // @[LocalRouter.scala 62:32]
  assign memReadDataDemuxModule_io_in_bits_3 = io_mem_output_bits_3; // @[LocalRouter.scala 62:32]
  assign memReadDataDemuxModule_io_in_bits_4 = io_mem_output_bits_4; // @[LocalRouter.scala 62:32]
  assign memReadDataDemuxModule_io_in_bits_5 = io_mem_output_bits_5; // @[LocalRouter.scala 62:32]
  assign memReadDataDemuxModule_io_in_bits_6 = io_mem_output_bits_6; // @[LocalRouter.scala 62:32]
  assign memReadDataDemuxModule_io_in_bits_7 = io_mem_output_bits_7; // @[LocalRouter.scala 62:32]
  assign memReadDataDemuxModule_io_sel_valid = sizeHandler_io_out_valid; // @[package.scala 36:18]
  assign memReadDataDemuxModule_io_sel_bits = sizeHandler_io_out_bits_kind; // @[package.scala 35:17]
  assign memReadDataDemuxModule_io_out_0_ready = io_array_weightInput_ready; // @[LocalRouter.scala 63:24]
  assign memReadDataDemuxModule_io_out_1_ready = io_array_input_ready; // @[LocalRouter.scala 64:18]
  assign memReadDataDemuxModule_io_out_2_ready = accWriteDataMuxModule_io_in_1_ready; // @[LocalRouter.scala 85:34]
  assign memWriteDataMuxModule_io_in_0_valid = 1'h0; // @[package.scala 410:15]
  assign memWriteDataMuxModule_io_in_0_bits_0 = 16'sh0; // @[package.scala 80:57]
  assign memWriteDataMuxModule_io_in_0_bits_1 = 16'sh0; // @[package.scala 80:57]
  assign memWriteDataMuxModule_io_in_0_bits_2 = 16'sh0; // @[package.scala 80:57]
  assign memWriteDataMuxModule_io_in_0_bits_3 = 16'sh0; // @[package.scala 80:57]
  assign memWriteDataMuxModule_io_in_0_bits_4 = 16'sh0; // @[package.scala 80:57]
  assign memWriteDataMuxModule_io_in_0_bits_5 = 16'sh0; // @[package.scala 80:57]
  assign memWriteDataMuxModule_io_in_0_bits_6 = 16'sh0; // @[package.scala 80:57]
  assign memWriteDataMuxModule_io_in_0_bits_7 = 16'sh0; // @[package.scala 80:57]
  assign memWriteDataMuxModule_io_in_1_valid = io_acc_output_valid; // @[LocalRouter.scala 74:34]
  assign memWriteDataMuxModule_io_in_1_bits_0 = io_acc_output_bits_0; // @[LocalRouter.scala 74:34]
  assign memWriteDataMuxModule_io_in_1_bits_1 = io_acc_output_bits_1; // @[LocalRouter.scala 74:34]
  assign memWriteDataMuxModule_io_in_1_bits_2 = io_acc_output_bits_2; // @[LocalRouter.scala 74:34]
  assign memWriteDataMuxModule_io_in_1_bits_3 = io_acc_output_bits_3; // @[LocalRouter.scala 74:34]
  assign memWriteDataMuxModule_io_in_1_bits_4 = io_acc_output_bits_4; // @[LocalRouter.scala 74:34]
  assign memWriteDataMuxModule_io_in_1_bits_5 = io_acc_output_bits_5; // @[LocalRouter.scala 74:34]
  assign memWriteDataMuxModule_io_in_1_bits_6 = io_acc_output_bits_6; // @[LocalRouter.scala 74:34]
  assign memWriteDataMuxModule_io_in_1_bits_7 = io_acc_output_bits_7; // @[LocalRouter.scala 74:34]
  assign memWriteDataMuxModule_io_sel_valid = sizeHandler_1_io_out_valid; // @[package.scala 36:18]
  assign memWriteDataMuxModule_io_sel_bits = sizeHandler_1_io_out_bits_sel; // @[package.scala 35:17]
  assign memWriteDataMuxModule_io_out_ready = io_mem_input_ready; // @[LocalRouter.scala 75:16]
  assign accWriteDataMuxModule_io_in_0_valid = io_array_output_valid; // @[LocalRouter.scala 84:34]
  assign accWriteDataMuxModule_io_in_0_bits_0 = io_array_output_bits_0; // @[LocalRouter.scala 84:34]
  assign accWriteDataMuxModule_io_in_0_bits_1 = io_array_output_bits_1; // @[LocalRouter.scala 84:34]
  assign accWriteDataMuxModule_io_in_0_bits_2 = io_array_output_bits_2; // @[LocalRouter.scala 84:34]
  assign accWriteDataMuxModule_io_in_0_bits_3 = io_array_output_bits_3; // @[LocalRouter.scala 84:34]
  assign accWriteDataMuxModule_io_in_0_bits_4 = io_array_output_bits_4; // @[LocalRouter.scala 84:34]
  assign accWriteDataMuxModule_io_in_0_bits_5 = io_array_output_bits_5; // @[LocalRouter.scala 84:34]
  assign accWriteDataMuxModule_io_in_0_bits_6 = io_array_output_bits_6; // @[LocalRouter.scala 84:34]
  assign accWriteDataMuxModule_io_in_0_bits_7 = io_array_output_bits_7; // @[LocalRouter.scala 84:34]
  assign accWriteDataMuxModule_io_in_1_valid = memReadDataDemuxModule_io_out_2_valid; // @[LocalRouter.scala 85:34]
  assign accWriteDataMuxModule_io_in_1_bits_0 = memReadDataDemuxModule_io_out_2_bits_0; // @[LocalRouter.scala 85:34]
  assign accWriteDataMuxModule_io_in_1_bits_1 = memReadDataDemuxModule_io_out_2_bits_1; // @[LocalRouter.scala 85:34]
  assign accWriteDataMuxModule_io_in_1_bits_2 = memReadDataDemuxModule_io_out_2_bits_2; // @[LocalRouter.scala 85:34]
  assign accWriteDataMuxModule_io_in_1_bits_3 = memReadDataDemuxModule_io_out_2_bits_3; // @[LocalRouter.scala 85:34]
  assign accWriteDataMuxModule_io_in_1_bits_4 = memReadDataDemuxModule_io_out_2_bits_4; // @[LocalRouter.scala 85:34]
  assign accWriteDataMuxModule_io_in_1_bits_5 = memReadDataDemuxModule_io_out_2_bits_5; // @[LocalRouter.scala 85:34]
  assign accWriteDataMuxModule_io_in_1_bits_6 = memReadDataDemuxModule_io_out_2_bits_6; // @[LocalRouter.scala 85:34]
  assign accWriteDataMuxModule_io_in_1_bits_7 = memReadDataDemuxModule_io_out_2_bits_7; // @[LocalRouter.scala 85:34]
  assign accWriteDataMuxModule_io_sel_valid = sizeHandler_2_io_out_valid; // @[package.scala 36:18]
  assign accWriteDataMuxModule_io_sel_bits = sizeHandler_2_io_out_bits_sel; // @[package.scala 35:17]
  assign accWriteDataMuxModule_io_out_ready = io_acc_input_ready; // @[LocalRouter.scala 86:16]
  assign sizeHandler_clock = clock;
  assign sizeHandler_reset = reset;
  assign sizeHandler_io_in_valid = io_control_bits_kind == 4'h1 ? io_control_ready_sizeHandler_io_in_w_4_valid : _GEN_44
    ; // @[LocalRouter.scala 122:72 MultiEnqueue.scala 61:10]
  assign sizeHandler_io_in_bits_kind = io_control_bits_kind == 4'h1 ? 2'h0 : _GEN_43; // @[LocalRouter.scala 122:72 MultiEnqueue.scala 61:10]
  assign sizeHandler_io_in_bits_size = io_control_bits_kind == 4'h1 ? io_control_bits_size : _GEN_42; // @[LocalRouter.scala 122:72 MultiEnqueue.scala 61:10]
  assign sizeHandler_io_out_ready = memReadDataDemuxModule_io_sel_ready; // @[package.scala 37:30]
  assign sizeHandler_1_clock = clock;
  assign sizeHandler_1_reset = reset;
  assign sizeHandler_1_io_in_valid = io_control_bits_kind == 4'h1 ? 1'h0 : _GEN_54; // @[LocalRouter.scala 122:72 package.scala 410:15]
  assign sizeHandler_1_io_in_bits_sel = io_control_bits_kind == 4'h1 ? 1'h0 : _GEN_53; // @[LocalRouter.scala 122:72 package.scala 409:14]
  assign sizeHandler_1_io_in_bits_size = io_control_bits_kind == 4'h1 ? 6'h0 : _GEN_52; // @[LocalRouter.scala 122:72 package.scala 409:14]
  assign sizeHandler_1_io_out_ready = memWriteDataMuxModule_io_sel_ready; // @[package.scala 37:30]
  assign sizeHandler_2_clock = clock;
  assign sizeHandler_2_reset = reset;
  assign sizeHandler_2_io_in_valid = io_control_bits_kind == 4'h1 ? 1'h0 : _GEN_48; // @[LocalRouter.scala 122:72 package.scala 410:15]
  assign sizeHandler_2_io_in_bits_sel = io_control_bits_kind == 4'h1 ? 1'h0 : _GEN_47; // @[LocalRouter.scala 122:72 package.scala 409:14]
  assign sizeHandler_2_io_in_bits_size = io_control_bits_kind == 4'h1 ? 6'h0 : _GEN_46; // @[LocalRouter.scala 122:72 package.scala 409:14]
  assign sizeHandler_2_io_out_ready = accWriteDataMuxModule_io_sel_ready; // @[package.scala 37:30]
  assign enqueuer1_clock = clock;
  assign enqueuer1_reset = reset;
  assign enqueuer1_io_in_valid = io_control_bits_kind == 4'h1 ? io_control_valid : _GEN_50; // @[LocalRouter.scala 122:72 MultiEnqueue.scala 60:17]
  assign enqueuer1_io_out_0_ready = io_control_bits_kind == 4'h1 ? io_control_ready_sizeHandler_io_in_w_5_ready :
    _GEN_51; // @[LocalRouter.scala 122:72 ReadyValid.scala 19:11]
  assign enqueuer2_clock = clock;
  assign enqueuer2_reset = reset;
  assign enqueuer2_io_in_valid = io_control_bits_kind == 4'h1 ? 1'h0 : _GEN_40; // @[LocalRouter.scala 122:72 MultiEnqueue.scala 40:17]
  assign enqueuer2_io_out_0_ready = io_control_bits_kind == 4'h1 ? 1'h0 : _GEN_41; // @[LocalRouter.scala 122:72 MultiEnqueue.scala 42:18]
  assign enqueuer2_io_out_1_ready = io_control_bits_kind == 4'h1 ? 1'h0 : _GEN_45; // @[LocalRouter.scala 122:72 MultiEnqueue.scala 42:18]
endmodule
module HostRouter(
  output        io_control_ready,
  input         io_control_valid,
  input  [1:0]  io_control_bits_kind,
  output        io_dram0_dataIn_ready,
  input         io_dram0_dataIn_valid,
  input  [15:0] io_dram0_dataIn_bits_0,
  input  [15:0] io_dram0_dataIn_bits_1,
  input  [15:0] io_dram0_dataIn_bits_2,
  input  [15:0] io_dram0_dataIn_bits_3,
  input  [15:0] io_dram0_dataIn_bits_4,
  input  [15:0] io_dram0_dataIn_bits_5,
  input  [15:0] io_dram0_dataIn_bits_6,
  input  [15:0] io_dram0_dataIn_bits_7,
  input         io_dram0_dataOut_ready,
  output        io_dram0_dataOut_valid,
  output [15:0] io_dram0_dataOut_bits_0,
  output [15:0] io_dram0_dataOut_bits_1,
  output [15:0] io_dram0_dataOut_bits_2,
  output [15:0] io_dram0_dataOut_bits_3,
  output [15:0] io_dram0_dataOut_bits_4,
  output [15:0] io_dram0_dataOut_bits_5,
  output [15:0] io_dram0_dataOut_bits_6,
  output [15:0] io_dram0_dataOut_bits_7,
  output        io_dram1_dataIn_ready,
  input         io_dram1_dataIn_valid,
  input  [15:0] io_dram1_dataIn_bits_0,
  input  [15:0] io_dram1_dataIn_bits_1,
  input  [15:0] io_dram1_dataIn_bits_2,
  input  [15:0] io_dram1_dataIn_bits_3,
  input  [15:0] io_dram1_dataIn_bits_4,
  input  [15:0] io_dram1_dataIn_bits_5,
  input  [15:0] io_dram1_dataIn_bits_6,
  input  [15:0] io_dram1_dataIn_bits_7,
  input         io_dram1_dataOut_ready,
  output        io_dram1_dataOut_valid,
  output [15:0] io_dram1_dataOut_bits_0,
  output [15:0] io_dram1_dataOut_bits_1,
  output [15:0] io_dram1_dataOut_bits_2,
  output [15:0] io_dram1_dataOut_bits_3,
  output [15:0] io_dram1_dataOut_bits_4,
  output [15:0] io_dram1_dataOut_bits_5,
  output [15:0] io_dram1_dataOut_bits_6,
  output [15:0] io_dram1_dataOut_bits_7,
  output        io_mem_output_ready,
  input         io_mem_output_valid,
  input  [15:0] io_mem_output_bits_0,
  input  [15:0] io_mem_output_bits_1,
  input  [15:0] io_mem_output_bits_2,
  input  [15:0] io_mem_output_bits_3,
  input  [15:0] io_mem_output_bits_4,
  input  [15:0] io_mem_output_bits_5,
  input  [15:0] io_mem_output_bits_6,
  input  [15:0] io_mem_output_bits_7,
  input         io_mem_input_ready,
  output        io_mem_input_valid,
  output [15:0] io_mem_input_bits_0,
  output [15:0] io_mem_input_bits_1,
  output [15:0] io_mem_input_bits_2,
  output [15:0] io_mem_input_bits_3,
  output [15:0] io_mem_input_bits_4,
  output [15:0] io_mem_input_bits_5,
  output [15:0] io_mem_input_bits_6,
  output [15:0] io_mem_input_bits_7
);
  wire  dataIn_mux_io_in_0_ready; // @[Mux.scala 71:21]
  wire  dataIn_mux_io_in_0_valid; // @[Mux.scala 71:21]
  wire [15:0] dataIn_mux_io_in_0_bits_0; // @[Mux.scala 71:21]
  wire [15:0] dataIn_mux_io_in_0_bits_1; // @[Mux.scala 71:21]
  wire [15:0] dataIn_mux_io_in_0_bits_2; // @[Mux.scala 71:21]
  wire [15:0] dataIn_mux_io_in_0_bits_3; // @[Mux.scala 71:21]
  wire [15:0] dataIn_mux_io_in_0_bits_4; // @[Mux.scala 71:21]
  wire [15:0] dataIn_mux_io_in_0_bits_5; // @[Mux.scala 71:21]
  wire [15:0] dataIn_mux_io_in_0_bits_6; // @[Mux.scala 71:21]
  wire [15:0] dataIn_mux_io_in_0_bits_7; // @[Mux.scala 71:21]
  wire  dataIn_mux_io_in_1_ready; // @[Mux.scala 71:21]
  wire  dataIn_mux_io_in_1_valid; // @[Mux.scala 71:21]
  wire [15:0] dataIn_mux_io_in_1_bits_0; // @[Mux.scala 71:21]
  wire [15:0] dataIn_mux_io_in_1_bits_1; // @[Mux.scala 71:21]
  wire [15:0] dataIn_mux_io_in_1_bits_2; // @[Mux.scala 71:21]
  wire [15:0] dataIn_mux_io_in_1_bits_3; // @[Mux.scala 71:21]
  wire [15:0] dataIn_mux_io_in_1_bits_4; // @[Mux.scala 71:21]
  wire [15:0] dataIn_mux_io_in_1_bits_5; // @[Mux.scala 71:21]
  wire [15:0] dataIn_mux_io_in_1_bits_6; // @[Mux.scala 71:21]
  wire [15:0] dataIn_mux_io_in_1_bits_7; // @[Mux.scala 71:21]
  wire  dataIn_mux_io_sel_ready; // @[Mux.scala 71:21]
  wire  dataIn_mux_io_sel_valid; // @[Mux.scala 71:21]
  wire  dataIn_mux_io_sel_bits; // @[Mux.scala 71:21]
  wire  dataIn_mux_io_out_ready; // @[Mux.scala 71:21]
  wire  dataIn_mux_io_out_valid; // @[Mux.scala 71:21]
  wire [15:0] dataIn_mux_io_out_bits_0; // @[Mux.scala 71:21]
  wire [15:0] dataIn_mux_io_out_bits_1; // @[Mux.scala 71:21]
  wire [15:0] dataIn_mux_io_out_bits_2; // @[Mux.scala 71:21]
  wire [15:0] dataIn_mux_io_out_bits_3; // @[Mux.scala 71:21]
  wire [15:0] dataIn_mux_io_out_bits_4; // @[Mux.scala 71:21]
  wire [15:0] dataIn_mux_io_out_bits_5; // @[Mux.scala 71:21]
  wire [15:0] dataIn_mux_io_out_bits_6; // @[Mux.scala 71:21]
  wire [15:0] dataIn_mux_io_out_bits_7; // @[Mux.scala 71:21]
  wire  dataOut_demux_io_in_ready; // @[Demux.scala 46:23]
  wire  dataOut_demux_io_in_valid; // @[Demux.scala 46:23]
  wire [15:0] dataOut_demux_io_in_bits_0; // @[Demux.scala 46:23]
  wire [15:0] dataOut_demux_io_in_bits_1; // @[Demux.scala 46:23]
  wire [15:0] dataOut_demux_io_in_bits_2; // @[Demux.scala 46:23]
  wire [15:0] dataOut_demux_io_in_bits_3; // @[Demux.scala 46:23]
  wire [15:0] dataOut_demux_io_in_bits_4; // @[Demux.scala 46:23]
  wire [15:0] dataOut_demux_io_in_bits_5; // @[Demux.scala 46:23]
  wire [15:0] dataOut_demux_io_in_bits_6; // @[Demux.scala 46:23]
  wire [15:0] dataOut_demux_io_in_bits_7; // @[Demux.scala 46:23]
  wire  dataOut_demux_io_sel_ready; // @[Demux.scala 46:23]
  wire  dataOut_demux_io_sel_valid; // @[Demux.scala 46:23]
  wire  dataOut_demux_io_sel_bits; // @[Demux.scala 46:23]
  wire  dataOut_demux_io_out_0_ready; // @[Demux.scala 46:23]
  wire  dataOut_demux_io_out_0_valid; // @[Demux.scala 46:23]
  wire [15:0] dataOut_demux_io_out_0_bits_0; // @[Demux.scala 46:23]
  wire [15:0] dataOut_demux_io_out_0_bits_1; // @[Demux.scala 46:23]
  wire [15:0] dataOut_demux_io_out_0_bits_2; // @[Demux.scala 46:23]
  wire [15:0] dataOut_demux_io_out_0_bits_3; // @[Demux.scala 46:23]
  wire [15:0] dataOut_demux_io_out_0_bits_4; // @[Demux.scala 46:23]
  wire [15:0] dataOut_demux_io_out_0_bits_5; // @[Demux.scala 46:23]
  wire [15:0] dataOut_demux_io_out_0_bits_6; // @[Demux.scala 46:23]
  wire [15:0] dataOut_demux_io_out_0_bits_7; // @[Demux.scala 46:23]
  wire  dataOut_demux_io_out_1_ready; // @[Demux.scala 46:23]
  wire  dataOut_demux_io_out_1_valid; // @[Demux.scala 46:23]
  wire [15:0] dataOut_demux_io_out_1_bits_0; // @[Demux.scala 46:23]
  wire [15:0] dataOut_demux_io_out_1_bits_1; // @[Demux.scala 46:23]
  wire [15:0] dataOut_demux_io_out_1_bits_2; // @[Demux.scala 46:23]
  wire [15:0] dataOut_demux_io_out_1_bits_3; // @[Demux.scala 46:23]
  wire [15:0] dataOut_demux_io_out_1_bits_4; // @[Demux.scala 46:23]
  wire [15:0] dataOut_demux_io_out_1_bits_5; // @[Demux.scala 46:23]
  wire [15:0] dataOut_demux_io_out_1_bits_6; // @[Demux.scala 46:23]
  wire [15:0] dataOut_demux_io_out_1_bits_7; // @[Demux.scala 46:23]
  wire  isDataIn = io_control_bits_kind == 2'h0 | io_control_bits_kind == 2'h2; // @[HostRouter.scala 57:18]
  wire  isDataOut = io_control_bits_kind == 2'h1 | io_control_bits_kind == 2'h3; // @[HostRouter.scala 61:19]
  Mux dataIn_mux ( // @[Mux.scala 71:21]
    .io_in_0_ready(dataIn_mux_io_in_0_ready),
    .io_in_0_valid(dataIn_mux_io_in_0_valid),
    .io_in_0_bits_0(dataIn_mux_io_in_0_bits_0),
    .io_in_0_bits_1(dataIn_mux_io_in_0_bits_1),
    .io_in_0_bits_2(dataIn_mux_io_in_0_bits_2),
    .io_in_0_bits_3(dataIn_mux_io_in_0_bits_3),
    .io_in_0_bits_4(dataIn_mux_io_in_0_bits_4),
    .io_in_0_bits_5(dataIn_mux_io_in_0_bits_5),
    .io_in_0_bits_6(dataIn_mux_io_in_0_bits_6),
    .io_in_0_bits_7(dataIn_mux_io_in_0_bits_7),
    .io_in_1_ready(dataIn_mux_io_in_1_ready),
    .io_in_1_valid(dataIn_mux_io_in_1_valid),
    .io_in_1_bits_0(dataIn_mux_io_in_1_bits_0),
    .io_in_1_bits_1(dataIn_mux_io_in_1_bits_1),
    .io_in_1_bits_2(dataIn_mux_io_in_1_bits_2),
    .io_in_1_bits_3(dataIn_mux_io_in_1_bits_3),
    .io_in_1_bits_4(dataIn_mux_io_in_1_bits_4),
    .io_in_1_bits_5(dataIn_mux_io_in_1_bits_5),
    .io_in_1_bits_6(dataIn_mux_io_in_1_bits_6),
    .io_in_1_bits_7(dataIn_mux_io_in_1_bits_7),
    .io_sel_ready(dataIn_mux_io_sel_ready),
    .io_sel_valid(dataIn_mux_io_sel_valid),
    .io_sel_bits(dataIn_mux_io_sel_bits),
    .io_out_ready(dataIn_mux_io_out_ready),
    .io_out_valid(dataIn_mux_io_out_valid),
    .io_out_bits_0(dataIn_mux_io_out_bits_0),
    .io_out_bits_1(dataIn_mux_io_out_bits_1),
    .io_out_bits_2(dataIn_mux_io_out_bits_2),
    .io_out_bits_3(dataIn_mux_io_out_bits_3),
    .io_out_bits_4(dataIn_mux_io_out_bits_4),
    .io_out_bits_5(dataIn_mux_io_out_bits_5),
    .io_out_bits_6(dataIn_mux_io_out_bits_6),
    .io_out_bits_7(dataIn_mux_io_out_bits_7)
  );
  Demux dataOut_demux ( // @[Demux.scala 46:23]
    .io_in_ready(dataOut_demux_io_in_ready),
    .io_in_valid(dataOut_demux_io_in_valid),
    .io_in_bits_0(dataOut_demux_io_in_bits_0),
    .io_in_bits_1(dataOut_demux_io_in_bits_1),
    .io_in_bits_2(dataOut_demux_io_in_bits_2),
    .io_in_bits_3(dataOut_demux_io_in_bits_3),
    .io_in_bits_4(dataOut_demux_io_in_bits_4),
    .io_in_bits_5(dataOut_demux_io_in_bits_5),
    .io_in_bits_6(dataOut_demux_io_in_bits_6),
    .io_in_bits_7(dataOut_demux_io_in_bits_7),
    .io_sel_ready(dataOut_demux_io_sel_ready),
    .io_sel_valid(dataOut_demux_io_sel_valid),
    .io_sel_bits(dataOut_demux_io_sel_bits),
    .io_out_0_ready(dataOut_demux_io_out_0_ready),
    .io_out_0_valid(dataOut_demux_io_out_0_valid),
    .io_out_0_bits_0(dataOut_demux_io_out_0_bits_0),
    .io_out_0_bits_1(dataOut_demux_io_out_0_bits_1),
    .io_out_0_bits_2(dataOut_demux_io_out_0_bits_2),
    .io_out_0_bits_3(dataOut_demux_io_out_0_bits_3),
    .io_out_0_bits_4(dataOut_demux_io_out_0_bits_4),
    .io_out_0_bits_5(dataOut_demux_io_out_0_bits_5),
    .io_out_0_bits_6(dataOut_demux_io_out_0_bits_6),
    .io_out_0_bits_7(dataOut_demux_io_out_0_bits_7),
    .io_out_1_ready(dataOut_demux_io_out_1_ready),
    .io_out_1_valid(dataOut_demux_io_out_1_valid),
    .io_out_1_bits_0(dataOut_demux_io_out_1_bits_0),
    .io_out_1_bits_1(dataOut_demux_io_out_1_bits_1),
    .io_out_1_bits_2(dataOut_demux_io_out_1_bits_2),
    .io_out_1_bits_3(dataOut_demux_io_out_1_bits_3),
    .io_out_1_bits_4(dataOut_demux_io_out_1_bits_4),
    .io_out_1_bits_5(dataOut_demux_io_out_1_bits_5),
    .io_out_1_bits_6(dataOut_demux_io_out_1_bits_6),
    .io_out_1_bits_7(dataOut_demux_io_out_1_bits_7)
  );
  assign io_control_ready = isDataIn & dataIn_mux_io_sel_ready | isDataOut & dataOut_demux_io_sel_ready; // @[HostRouter.scala 41:47]
  assign io_dram0_dataIn_ready = dataIn_mux_io_in_0_ready; // @[Mux.scala 79:18]
  assign io_dram0_dataOut_valid = dataOut_demux_io_out_0_valid; // @[Demux.scala 55:10]
  assign io_dram0_dataOut_bits_0 = dataOut_demux_io_out_0_bits_0; // @[Demux.scala 55:10]
  assign io_dram0_dataOut_bits_1 = dataOut_demux_io_out_0_bits_1; // @[Demux.scala 55:10]
  assign io_dram0_dataOut_bits_2 = dataOut_demux_io_out_0_bits_2; // @[Demux.scala 55:10]
  assign io_dram0_dataOut_bits_3 = dataOut_demux_io_out_0_bits_3; // @[Demux.scala 55:10]
  assign io_dram0_dataOut_bits_4 = dataOut_demux_io_out_0_bits_4; // @[Demux.scala 55:10]
  assign io_dram0_dataOut_bits_5 = dataOut_demux_io_out_0_bits_5; // @[Demux.scala 55:10]
  assign io_dram0_dataOut_bits_6 = dataOut_demux_io_out_0_bits_6; // @[Demux.scala 55:10]
  assign io_dram0_dataOut_bits_7 = dataOut_demux_io_out_0_bits_7; // @[Demux.scala 55:10]
  assign io_dram1_dataIn_ready = dataIn_mux_io_in_1_ready; // @[Mux.scala 80:18]
  assign io_dram1_dataOut_valid = dataOut_demux_io_out_1_valid; // @[Demux.scala 56:10]
  assign io_dram1_dataOut_bits_0 = dataOut_demux_io_out_1_bits_0; // @[Demux.scala 56:10]
  assign io_dram1_dataOut_bits_1 = dataOut_demux_io_out_1_bits_1; // @[Demux.scala 56:10]
  assign io_dram1_dataOut_bits_2 = dataOut_demux_io_out_1_bits_2; // @[Demux.scala 56:10]
  assign io_dram1_dataOut_bits_3 = dataOut_demux_io_out_1_bits_3; // @[Demux.scala 56:10]
  assign io_dram1_dataOut_bits_4 = dataOut_demux_io_out_1_bits_4; // @[Demux.scala 56:10]
  assign io_dram1_dataOut_bits_5 = dataOut_demux_io_out_1_bits_5; // @[Demux.scala 56:10]
  assign io_dram1_dataOut_bits_6 = dataOut_demux_io_out_1_bits_6; // @[Demux.scala 56:10]
  assign io_dram1_dataOut_bits_7 = dataOut_demux_io_out_1_bits_7; // @[Demux.scala 56:10]
  assign io_mem_output_ready = dataOut_demux_io_in_ready; // @[Demux.scala 54:17]
  assign io_mem_input_valid = dataIn_mux_io_out_valid; // @[Mux.scala 81:9]
  assign io_mem_input_bits_0 = dataIn_mux_io_out_bits_0; // @[Mux.scala 81:9]
  assign io_mem_input_bits_1 = dataIn_mux_io_out_bits_1; // @[Mux.scala 81:9]
  assign io_mem_input_bits_2 = dataIn_mux_io_out_bits_2; // @[Mux.scala 81:9]
  assign io_mem_input_bits_3 = dataIn_mux_io_out_bits_3; // @[Mux.scala 81:9]
  assign io_mem_input_bits_4 = dataIn_mux_io_out_bits_4; // @[Mux.scala 81:9]
  assign io_mem_input_bits_5 = dataIn_mux_io_out_bits_5; // @[Mux.scala 81:9]
  assign io_mem_input_bits_6 = dataIn_mux_io_out_bits_6; // @[Mux.scala 81:9]
  assign io_mem_input_bits_7 = dataIn_mux_io_out_bits_7; // @[Mux.scala 81:9]
  assign dataIn_mux_io_in_0_valid = io_dram0_dataIn_valid; // @[Mux.scala 79:18]
  assign dataIn_mux_io_in_0_bits_0 = io_dram0_dataIn_bits_0; // @[Mux.scala 79:18]
  assign dataIn_mux_io_in_0_bits_1 = io_dram0_dataIn_bits_1; // @[Mux.scala 79:18]
  assign dataIn_mux_io_in_0_bits_2 = io_dram0_dataIn_bits_2; // @[Mux.scala 79:18]
  assign dataIn_mux_io_in_0_bits_3 = io_dram0_dataIn_bits_3; // @[Mux.scala 79:18]
  assign dataIn_mux_io_in_0_bits_4 = io_dram0_dataIn_bits_4; // @[Mux.scala 79:18]
  assign dataIn_mux_io_in_0_bits_5 = io_dram0_dataIn_bits_5; // @[Mux.scala 79:18]
  assign dataIn_mux_io_in_0_bits_6 = io_dram0_dataIn_bits_6; // @[Mux.scala 79:18]
  assign dataIn_mux_io_in_0_bits_7 = io_dram0_dataIn_bits_7; // @[Mux.scala 79:18]
  assign dataIn_mux_io_in_1_valid = io_dram1_dataIn_valid; // @[Mux.scala 80:18]
  assign dataIn_mux_io_in_1_bits_0 = io_dram1_dataIn_bits_0; // @[Mux.scala 80:18]
  assign dataIn_mux_io_in_1_bits_1 = io_dram1_dataIn_bits_1; // @[Mux.scala 80:18]
  assign dataIn_mux_io_in_1_bits_2 = io_dram1_dataIn_bits_2; // @[Mux.scala 80:18]
  assign dataIn_mux_io_in_1_bits_3 = io_dram1_dataIn_bits_3; // @[Mux.scala 80:18]
  assign dataIn_mux_io_in_1_bits_4 = io_dram1_dataIn_bits_4; // @[Mux.scala 80:18]
  assign dataIn_mux_io_in_1_bits_5 = io_dram1_dataIn_bits_5; // @[Mux.scala 80:18]
  assign dataIn_mux_io_in_1_bits_6 = io_dram1_dataIn_bits_6; // @[Mux.scala 80:18]
  assign dataIn_mux_io_in_1_bits_7 = io_dram1_dataIn_bits_7; // @[Mux.scala 80:18]
  assign dataIn_mux_io_sel_valid = io_control_valid & isDataIn; // @[HostRouter.scala 43:33]
  assign dataIn_mux_io_sel_bits = io_control_bits_kind[1]; // @[HostRouter.scala 44:35]
  assign dataIn_mux_io_out_ready = io_mem_input_ready; // @[Mux.scala 81:9]
  assign dataOut_demux_io_in_valid = io_mem_output_valid; // @[Demux.scala 54:17]
  assign dataOut_demux_io_in_bits_0 = io_mem_output_bits_0; // @[Demux.scala 54:17]
  assign dataOut_demux_io_in_bits_1 = io_mem_output_bits_1; // @[Demux.scala 54:17]
  assign dataOut_demux_io_in_bits_2 = io_mem_output_bits_2; // @[Demux.scala 54:17]
  assign dataOut_demux_io_in_bits_3 = io_mem_output_bits_3; // @[Demux.scala 54:17]
  assign dataOut_demux_io_in_bits_4 = io_mem_output_bits_4; // @[Demux.scala 54:17]
  assign dataOut_demux_io_in_bits_5 = io_mem_output_bits_5; // @[Demux.scala 54:17]
  assign dataOut_demux_io_in_bits_6 = io_mem_output_bits_6; // @[Demux.scala 54:17]
  assign dataOut_demux_io_in_bits_7 = io_mem_output_bits_7; // @[Demux.scala 54:17]
  assign dataOut_demux_io_sel_valid = io_control_valid & isDataOut; // @[HostRouter.scala 46:34]
  assign dataOut_demux_io_sel_bits = io_control_bits_kind[1]; // @[HostRouter.scala 47:36]
  assign dataOut_demux_io_out_0_ready = io_dram0_dataOut_ready; // @[Demux.scala 55:10]
  assign dataOut_demux_io_out_1_ready = io_dram1_dataOut_ready; // @[Demux.scala 56:10]
endmodule
module Queue_26(
  input        clock,
  input        reset,
  output       io_enq_ready,
  input        io_enq_valid,
  input  [3:0] io_enq_bits_instruction_op,
  input        io_enq_bits_instruction_sourceLeft,
  input        io_enq_bits_instruction_sourceRight,
  input        io_enq_bits_instruction_dest,
  input  [5:0] io_enq_bits_readAddress,
  input  [5:0] io_enq_bits_writeAddress,
  input        io_enq_bits_accumulate,
  input        io_enq_bits_write,
  input        io_enq_bits_read,
  input        io_deq_ready,
  output       io_deq_valid,
  output [3:0] io_deq_bits_instruction_op,
  output       io_deq_bits_instruction_sourceLeft,
  output       io_deq_bits_instruction_sourceRight,
  output       io_deq_bits_instruction_dest,
  output [5:0] io_deq_bits_readAddress,
  output [5:0] io_deq_bits_writeAddress,
  output       io_deq_bits_accumulate,
  output       io_deq_bits_write,
  output       io_deq_bits_read
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_6;
  reg [31:0] _RAND_7;
  reg [31:0] _RAND_8;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_9;
  reg [31:0] _RAND_10;
  reg [31:0] _RAND_11;
`endif // RANDOMIZE_REG_INIT
  reg [3:0] ram_instruction_op [0:1]; // @[Decoupled.scala 218:16]
  wire [3:0] ram_instruction_op_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_instruction_op_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [3:0] ram_instruction_op_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_instruction_op_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_instruction_op_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_instruction_op_MPORT_en; // @[Decoupled.scala 218:16]
  reg  ram_instruction_sourceLeft [0:1]; // @[Decoupled.scala 218:16]
  wire  ram_instruction_sourceLeft_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_instruction_sourceLeft_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_instruction_sourceLeft_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_instruction_sourceLeft_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_instruction_sourceLeft_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_instruction_sourceLeft_MPORT_en; // @[Decoupled.scala 218:16]
  reg  ram_instruction_sourceRight [0:1]; // @[Decoupled.scala 218:16]
  wire  ram_instruction_sourceRight_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_instruction_sourceRight_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_instruction_sourceRight_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_instruction_sourceRight_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_instruction_sourceRight_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_instruction_sourceRight_MPORT_en; // @[Decoupled.scala 218:16]
  reg  ram_instruction_dest [0:1]; // @[Decoupled.scala 218:16]
  wire  ram_instruction_dest_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_instruction_dest_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_instruction_dest_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_instruction_dest_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_instruction_dest_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_instruction_dest_MPORT_en; // @[Decoupled.scala 218:16]
  reg [5:0] ram_readAddress [0:1]; // @[Decoupled.scala 218:16]
  wire [5:0] ram_readAddress_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_readAddress_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [5:0] ram_readAddress_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_readAddress_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_readAddress_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_readAddress_MPORT_en; // @[Decoupled.scala 218:16]
  reg [5:0] ram_writeAddress [0:1]; // @[Decoupled.scala 218:16]
  wire [5:0] ram_writeAddress_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_writeAddress_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [5:0] ram_writeAddress_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_writeAddress_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_writeAddress_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_writeAddress_MPORT_en; // @[Decoupled.scala 218:16]
  reg  ram_accumulate [0:1]; // @[Decoupled.scala 218:16]
  wire  ram_accumulate_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_accumulate_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_accumulate_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_accumulate_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_accumulate_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_accumulate_MPORT_en; // @[Decoupled.scala 218:16]
  reg  ram_write [0:1]; // @[Decoupled.scala 218:16]
  wire  ram_write_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_write_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_write_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_write_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_write_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_write_MPORT_en; // @[Decoupled.scala 218:16]
  reg  ram_read [0:1]; // @[Decoupled.scala 218:16]
  wire  ram_read_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_read_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_read_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_read_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_read_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_read_MPORT_en; // @[Decoupled.scala 218:16]
  reg  enq_ptr_value; // @[Counter.scala 60:40]
  reg  deq_ptr_value; // @[Counter.scala 60:40]
  reg  maybe_full; // @[Decoupled.scala 221:27]
  wire  ptr_match = enq_ptr_value == deq_ptr_value; // @[Decoupled.scala 223:33]
  wire  empty = ptr_match & ~maybe_full; // @[Decoupled.scala 224:25]
  wire  full = ptr_match & maybe_full; // @[Decoupled.scala 225:24]
  wire  do_enq = io_enq_ready & io_enq_valid; // @[Decoupled.scala 40:37]
  wire  do_deq = io_deq_ready & io_deq_valid; // @[Decoupled.scala 40:37]
  assign ram_instruction_op_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_instruction_op_io_deq_bits_MPORT_data = ram_instruction_op[ram_instruction_op_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_instruction_op_MPORT_data = io_enq_bits_instruction_op;
  assign ram_instruction_op_MPORT_addr = enq_ptr_value;
  assign ram_instruction_op_MPORT_mask = 1'h1;
  assign ram_instruction_op_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_instruction_sourceLeft_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_instruction_sourceLeft_io_deq_bits_MPORT_data =
    ram_instruction_sourceLeft[ram_instruction_sourceLeft_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_instruction_sourceLeft_MPORT_data = io_enq_bits_instruction_sourceLeft;
  assign ram_instruction_sourceLeft_MPORT_addr = enq_ptr_value;
  assign ram_instruction_sourceLeft_MPORT_mask = 1'h1;
  assign ram_instruction_sourceLeft_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_instruction_sourceRight_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_instruction_sourceRight_io_deq_bits_MPORT_data =
    ram_instruction_sourceRight[ram_instruction_sourceRight_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_instruction_sourceRight_MPORT_data = io_enq_bits_instruction_sourceRight;
  assign ram_instruction_sourceRight_MPORT_addr = enq_ptr_value;
  assign ram_instruction_sourceRight_MPORT_mask = 1'h1;
  assign ram_instruction_sourceRight_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_instruction_dest_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_instruction_dest_io_deq_bits_MPORT_data = ram_instruction_dest[ram_instruction_dest_io_deq_bits_MPORT_addr]
    ; // @[Decoupled.scala 218:16]
  assign ram_instruction_dest_MPORT_data = io_enq_bits_instruction_dest;
  assign ram_instruction_dest_MPORT_addr = enq_ptr_value;
  assign ram_instruction_dest_MPORT_mask = 1'h1;
  assign ram_instruction_dest_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_readAddress_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_readAddress_io_deq_bits_MPORT_data = ram_readAddress[ram_readAddress_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_readAddress_MPORT_data = io_enq_bits_readAddress;
  assign ram_readAddress_MPORT_addr = enq_ptr_value;
  assign ram_readAddress_MPORT_mask = 1'h1;
  assign ram_readAddress_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_writeAddress_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_writeAddress_io_deq_bits_MPORT_data = ram_writeAddress[ram_writeAddress_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_writeAddress_MPORT_data = io_enq_bits_writeAddress;
  assign ram_writeAddress_MPORT_addr = enq_ptr_value;
  assign ram_writeAddress_MPORT_mask = 1'h1;
  assign ram_writeAddress_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_accumulate_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_accumulate_io_deq_bits_MPORT_data = ram_accumulate[ram_accumulate_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_accumulate_MPORT_data = io_enq_bits_accumulate;
  assign ram_accumulate_MPORT_addr = enq_ptr_value;
  assign ram_accumulate_MPORT_mask = 1'h1;
  assign ram_accumulate_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_write_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_write_io_deq_bits_MPORT_data = ram_write[ram_write_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_write_MPORT_data = io_enq_bits_write;
  assign ram_write_MPORT_addr = enq_ptr_value;
  assign ram_write_MPORT_mask = 1'h1;
  assign ram_write_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_read_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_read_io_deq_bits_MPORT_data = ram_read[ram_read_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_read_MPORT_data = io_enq_bits_read;
  assign ram_read_MPORT_addr = enq_ptr_value;
  assign ram_read_MPORT_mask = 1'h1;
  assign ram_read_MPORT_en = io_enq_ready & io_enq_valid;
  assign io_enq_ready = ~full; // @[Decoupled.scala 241:19]
  assign io_deq_valid = ~empty; // @[Decoupled.scala 240:19]
  assign io_deq_bits_instruction_op = ram_instruction_op_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_instruction_sourceLeft = ram_instruction_sourceLeft_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_instruction_sourceRight = ram_instruction_sourceRight_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_instruction_dest = ram_instruction_dest_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_readAddress = ram_readAddress_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_writeAddress = ram_writeAddress_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_accumulate = ram_accumulate_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_write = ram_write_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_read = ram_read_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  always @(posedge clock) begin
    if(ram_instruction_op_MPORT_en & ram_instruction_op_MPORT_mask) begin
      ram_instruction_op[ram_instruction_op_MPORT_addr] <= ram_instruction_op_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_instruction_sourceLeft_MPORT_en & ram_instruction_sourceLeft_MPORT_mask) begin
      ram_instruction_sourceLeft[ram_instruction_sourceLeft_MPORT_addr] <= ram_instruction_sourceLeft_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_instruction_sourceRight_MPORT_en & ram_instruction_sourceRight_MPORT_mask) begin
      ram_instruction_sourceRight[ram_instruction_sourceRight_MPORT_addr] <= ram_instruction_sourceRight_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_instruction_dest_MPORT_en & ram_instruction_dest_MPORT_mask) begin
      ram_instruction_dest[ram_instruction_dest_MPORT_addr] <= ram_instruction_dest_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_readAddress_MPORT_en & ram_readAddress_MPORT_mask) begin
      ram_readAddress[ram_readAddress_MPORT_addr] <= ram_readAddress_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_writeAddress_MPORT_en & ram_writeAddress_MPORT_mask) begin
      ram_writeAddress[ram_writeAddress_MPORT_addr] <= ram_writeAddress_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_accumulate_MPORT_en & ram_accumulate_MPORT_mask) begin
      ram_accumulate[ram_accumulate_MPORT_addr] <= ram_accumulate_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_write_MPORT_en & ram_write_MPORT_mask) begin
      ram_write[ram_write_MPORT_addr] <= ram_write_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_read_MPORT_en & ram_read_MPORT_mask) begin
      ram_read[ram_read_MPORT_addr] <= ram_read_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if (reset) begin // @[Counter.scala 60:40]
      enq_ptr_value <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_enq) begin // @[Decoupled.scala 229:17]
      enq_ptr_value <= enq_ptr_value + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Counter.scala 60:40]
      deq_ptr_value <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_deq) begin // @[Decoupled.scala 233:17]
      deq_ptr_value <= deq_ptr_value + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Decoupled.scala 221:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 221:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 236:28]
      maybe_full <= do_enq; // @[Decoupled.scala 237:16]
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
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_instruction_op[initvar] = _RAND_0[3:0];
  _RAND_1 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_instruction_sourceLeft[initvar] = _RAND_1[0:0];
  _RAND_2 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_instruction_sourceRight[initvar] = _RAND_2[0:0];
  _RAND_3 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_instruction_dest[initvar] = _RAND_3[0:0];
  _RAND_4 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_readAddress[initvar] = _RAND_4[5:0];
  _RAND_5 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_writeAddress[initvar] = _RAND_5[5:0];
  _RAND_6 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_accumulate[initvar] = _RAND_6[0:0];
  _RAND_7 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_write[initvar] = _RAND_7[0:0];
  _RAND_8 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_read[initvar] = _RAND_8[0:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_9 = {1{`RANDOM}};
  enq_ptr_value = _RAND_9[0:0];
  _RAND_10 = {1{`RANDOM}};
  deq_ptr_value = _RAND_10[0:0];
  _RAND_11 = {1{`RANDOM}};
  maybe_full = _RAND_11[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Queue_27(
  input   clock,
  input   reset,
  output  io_enq_ready,
  input   io_enq_valid,
  input   io_enq_bits_load,
  input   io_enq_bits_zeroes,
  input   io_deq_ready,
  output  io_deq_valid,
  output  io_deq_bits_load,
  output  io_deq_bits_zeroes
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
`endif // RANDOMIZE_REG_INIT
  reg  ram_load [0:1]; // @[Decoupled.scala 218:16]
  wire  ram_load_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_load_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_load_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_load_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_load_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_load_MPORT_en; // @[Decoupled.scala 218:16]
  reg  ram_zeroes [0:1]; // @[Decoupled.scala 218:16]
  wire  ram_zeroes_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_zeroes_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_zeroes_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_zeroes_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_zeroes_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_zeroes_MPORT_en; // @[Decoupled.scala 218:16]
  reg  enq_ptr_value; // @[Counter.scala 60:40]
  reg  deq_ptr_value; // @[Counter.scala 60:40]
  reg  maybe_full; // @[Decoupled.scala 221:27]
  wire  ptr_match = enq_ptr_value == deq_ptr_value; // @[Decoupled.scala 223:33]
  wire  empty = ptr_match & ~maybe_full; // @[Decoupled.scala 224:25]
  wire  full = ptr_match & maybe_full; // @[Decoupled.scala 225:24]
  wire  do_enq = io_enq_ready & io_enq_valid; // @[Decoupled.scala 40:37]
  wire  do_deq = io_deq_ready & io_deq_valid; // @[Decoupled.scala 40:37]
  assign ram_load_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_load_io_deq_bits_MPORT_data = ram_load[ram_load_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_load_MPORT_data = io_enq_bits_load;
  assign ram_load_MPORT_addr = enq_ptr_value;
  assign ram_load_MPORT_mask = 1'h1;
  assign ram_load_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_zeroes_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_zeroes_io_deq_bits_MPORT_data = ram_zeroes[ram_zeroes_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_zeroes_MPORT_data = io_enq_bits_zeroes;
  assign ram_zeroes_MPORT_addr = enq_ptr_value;
  assign ram_zeroes_MPORT_mask = 1'h1;
  assign ram_zeroes_MPORT_en = io_enq_ready & io_enq_valid;
  assign io_enq_ready = ~full; // @[Decoupled.scala 241:19]
  assign io_deq_valid = ~empty; // @[Decoupled.scala 240:19]
  assign io_deq_bits_load = ram_load_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_zeroes = ram_zeroes_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  always @(posedge clock) begin
    if(ram_load_MPORT_en & ram_load_MPORT_mask) begin
      ram_load[ram_load_MPORT_addr] <= ram_load_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_zeroes_MPORT_en & ram_zeroes_MPORT_mask) begin
      ram_zeroes[ram_zeroes_MPORT_addr] <= ram_zeroes_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if (reset) begin // @[Counter.scala 60:40]
      enq_ptr_value <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_enq) begin // @[Decoupled.scala 229:17]
      enq_ptr_value <= enq_ptr_value + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Counter.scala 60:40]
      deq_ptr_value <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_deq) begin // @[Decoupled.scala 233:17]
      deq_ptr_value <= deq_ptr_value + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Decoupled.scala 221:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 221:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 236:28]
      maybe_full <= do_enq; // @[Decoupled.scala 237:16]
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
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_load[initvar] = _RAND_0[0:0];
  _RAND_1 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_zeroes[initvar] = _RAND_1[0:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_2 = {1{`RANDOM}};
  enq_ptr_value = _RAND_2[0:0];
  _RAND_3 = {1{`RANDOM}};
  deq_ptr_value = _RAND_3[0:0];
  _RAND_4 = {1{`RANDOM}};
  maybe_full = _RAND_4[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module TCU(
  input         clock,
  input         reset,
  output        io_instruction_ready,
  input         io_instruction_valid,
  input  [3:0]  io_instruction_bits_opcode,
  input  [3:0]  io_instruction_bits_flags,
  input  [47:0] io_instruction_bits_arguments,
  input         io_status_ready,
  output        io_status_valid,
  output        io_status_bits_last,
  output [3:0]  io_status_bits_bits_opcode,
  output [3:0]  io_status_bits_bits_flags,
  output [47:0] io_status_bits_bits_arguments,
  input         io_dram0_control_ready,
  output        io_dram0_control_valid,
  output        io_dram0_control_bits_write,
  output [19:0] io_dram0_control_bits_address,
  output [19:0] io_dram0_control_bits_size,
  output        io_dram0_dataIn_ready,
  input         io_dram0_dataIn_valid,
  input  [15:0] io_dram0_dataIn_bits_0,
  input  [15:0] io_dram0_dataIn_bits_1,
  input  [15:0] io_dram0_dataIn_bits_2,
  input  [15:0] io_dram0_dataIn_bits_3,
  input  [15:0] io_dram0_dataIn_bits_4,
  input  [15:0] io_dram0_dataIn_bits_5,
  input  [15:0] io_dram0_dataIn_bits_6,
  input  [15:0] io_dram0_dataIn_bits_7,
  input         io_dram0_dataOut_ready,
  output        io_dram0_dataOut_valid,
  output [15:0] io_dram0_dataOut_bits_0,
  output [15:0] io_dram0_dataOut_bits_1,
  output [15:0] io_dram0_dataOut_bits_2,
  output [15:0] io_dram0_dataOut_bits_3,
  output [15:0] io_dram0_dataOut_bits_4,
  output [15:0] io_dram0_dataOut_bits_5,
  output [15:0] io_dram0_dataOut_bits_6,
  output [15:0] io_dram0_dataOut_bits_7,
  input         io_dram1_control_ready,
  output        io_dram1_control_valid,
  output        io_dram1_control_bits_write,
  output [19:0] io_dram1_control_bits_address,
  output [19:0] io_dram1_control_bits_size,
  output        io_dram1_dataIn_ready,
  input         io_dram1_dataIn_valid,
  input  [15:0] io_dram1_dataIn_bits_0,
  input  [15:0] io_dram1_dataIn_bits_1,
  input  [15:0] io_dram1_dataIn_bits_2,
  input  [15:0] io_dram1_dataIn_bits_3,
  input  [15:0] io_dram1_dataIn_bits_4,
  input  [15:0] io_dram1_dataIn_bits_5,
  input  [15:0] io_dram1_dataIn_bits_6,
  input  [15:0] io_dram1_dataIn_bits_7,
  input         io_dram1_dataOut_ready,
  output        io_dram1_dataOut_valid,
  output [15:0] io_dram1_dataOut_bits_0,
  output [15:0] io_dram1_dataOut_bits_1,
  output [15:0] io_dram1_dataOut_bits_2,
  output [15:0] io_dram1_dataOut_bits_3,
  output [15:0] io_dram1_dataOut_bits_4,
  output [15:0] io_dram1_dataOut_bits_5,
  output [15:0] io_dram1_dataOut_bits_6,
  output [15:0] io_dram1_dataOut_bits_7,
  output [31:0] io_config_dram0AddressOffset,
  output [3:0]  io_config_dram0CacheBehaviour,
  output [31:0] io_config_dram1AddressOffset,
  output [3:0]  io_config_dram1CacheBehaviour,
  output        io_timeout,
  output        io_error,
  output        io_tracepoint,
  output [31:0] io_programCounter
);
  wire  decoder_clock; // @[TCU.scala 62:23]
  wire  decoder_reset; // @[TCU.scala 62:23]
  wire  decoder_io_instruction_ready; // @[TCU.scala 62:23]
  wire  decoder_io_instruction_valid; // @[TCU.scala 62:23]
  wire [3:0] decoder_io_instruction_bits_opcode; // @[TCU.scala 62:23]
  wire [3:0] decoder_io_instruction_bits_flags; // @[TCU.scala 62:23]
  wire [47:0] decoder_io_instruction_bits_arguments; // @[TCU.scala 62:23]
  wire  decoder_io_memPortA_ready; // @[TCU.scala 62:23]
  wire  decoder_io_memPortA_valid; // @[TCU.scala 62:23]
  wire  decoder_io_memPortA_bits_write; // @[TCU.scala 62:23]
  wire [5:0] decoder_io_memPortA_bits_address; // @[TCU.scala 62:23]
  wire  decoder_io_memPortB_ready; // @[TCU.scala 62:23]
  wire  decoder_io_memPortB_valid; // @[TCU.scala 62:23]
  wire  decoder_io_memPortB_bits_write; // @[TCU.scala 62:23]
  wire [5:0] decoder_io_memPortB_bits_address; // @[TCU.scala 62:23]
  wire  decoder_io_dram0_ready; // @[TCU.scala 62:23]
  wire  decoder_io_dram0_valid; // @[TCU.scala 62:23]
  wire  decoder_io_dram0_bits_write; // @[TCU.scala 62:23]
  wire [19:0] decoder_io_dram0_bits_address; // @[TCU.scala 62:23]
  wire [19:0] decoder_io_dram0_bits_size; // @[TCU.scala 62:23]
  wire  decoder_io_dram1_ready; // @[TCU.scala 62:23]
  wire  decoder_io_dram1_valid; // @[TCU.scala 62:23]
  wire  decoder_io_dram1_bits_write; // @[TCU.scala 62:23]
  wire [19:0] decoder_io_dram1_bits_address; // @[TCU.scala 62:23]
  wire [19:0] decoder_io_dram1_bits_size; // @[TCU.scala 62:23]
  wire  decoder_io_dataflow_ready; // @[TCU.scala 62:23]
  wire  decoder_io_dataflow_valid; // @[TCU.scala 62:23]
  wire [3:0] decoder_io_dataflow_bits_kind; // @[TCU.scala 62:23]
  wire [5:0] decoder_io_dataflow_bits_size; // @[TCU.scala 62:23]
  wire  decoder_io_hostDataflow_ready; // @[TCU.scala 62:23]
  wire  decoder_io_hostDataflow_valid; // @[TCU.scala 62:23]
  wire [1:0] decoder_io_hostDataflow_bits_kind; // @[TCU.scala 62:23]
  wire  decoder_io_acc_ready; // @[TCU.scala 62:23]
  wire  decoder_io_acc_valid; // @[TCU.scala 62:23]
  wire [3:0] decoder_io_acc_bits_instruction_op; // @[TCU.scala 62:23]
  wire  decoder_io_acc_bits_instruction_sourceLeft; // @[TCU.scala 62:23]
  wire  decoder_io_acc_bits_instruction_sourceRight; // @[TCU.scala 62:23]
  wire  decoder_io_acc_bits_instruction_dest; // @[TCU.scala 62:23]
  wire [5:0] decoder_io_acc_bits_readAddress; // @[TCU.scala 62:23]
  wire [5:0] decoder_io_acc_bits_writeAddress; // @[TCU.scala 62:23]
  wire  decoder_io_acc_bits_accumulate; // @[TCU.scala 62:23]
  wire  decoder_io_acc_bits_write; // @[TCU.scala 62:23]
  wire  decoder_io_acc_bits_read; // @[TCU.scala 62:23]
  wire  decoder_io_array_ready; // @[TCU.scala 62:23]
  wire  decoder_io_array_valid; // @[TCU.scala 62:23]
  wire  decoder_io_array_bits_load; // @[TCU.scala 62:23]
  wire  decoder_io_array_bits_zeroes; // @[TCU.scala 62:23]
  wire [31:0] decoder_io_config_dram0AddressOffset; // @[TCU.scala 62:23]
  wire [3:0] decoder_io_config_dram0CacheBehaviour; // @[TCU.scala 62:23]
  wire [31:0] decoder_io_config_dram1AddressOffset; // @[TCU.scala 62:23]
  wire [3:0] decoder_io_config_dram1CacheBehaviour; // @[TCU.scala 62:23]
  wire  decoder_io_status_ready; // @[TCU.scala 62:23]
  wire  decoder_io_status_valid; // @[TCU.scala 62:23]
  wire  decoder_io_status_bits_last; // @[TCU.scala 62:23]
  wire [3:0] decoder_io_status_bits_bits_opcode; // @[TCU.scala 62:23]
  wire [3:0] decoder_io_status_bits_bits_flags; // @[TCU.scala 62:23]
  wire [47:0] decoder_io_status_bits_bits_arguments; // @[TCU.scala 62:23]
  wire  decoder_io_timeout; // @[TCU.scala 62:23]
  wire  decoder_io_error; // @[TCU.scala 62:23]
  wire  decoder_io_tracepoint; // @[TCU.scala 62:23]
  wire [31:0] decoder_io_programCounter; // @[TCU.scala 62:23]
  wire  array_clock; // @[TCU.scala 63:21]
  wire  array_reset; // @[TCU.scala 63:21]
  wire  array_io_control_ready; // @[TCU.scala 63:21]
  wire  array_io_control_valid; // @[TCU.scala 63:21]
  wire  array_io_control_bits_load; // @[TCU.scala 63:21]
  wire  array_io_control_bits_zeroes; // @[TCU.scala 63:21]
  wire  array_io_input_ready; // @[TCU.scala 63:21]
  wire  array_io_input_valid; // @[TCU.scala 63:21]
  wire [15:0] array_io_input_bits_0; // @[TCU.scala 63:21]
  wire [15:0] array_io_input_bits_1; // @[TCU.scala 63:21]
  wire [15:0] array_io_input_bits_2; // @[TCU.scala 63:21]
  wire [15:0] array_io_input_bits_3; // @[TCU.scala 63:21]
  wire [15:0] array_io_input_bits_4; // @[TCU.scala 63:21]
  wire [15:0] array_io_input_bits_5; // @[TCU.scala 63:21]
  wire [15:0] array_io_input_bits_6; // @[TCU.scala 63:21]
  wire [15:0] array_io_input_bits_7; // @[TCU.scala 63:21]
  wire  array_io_weight_ready; // @[TCU.scala 63:21]
  wire  array_io_weight_valid; // @[TCU.scala 63:21]
  wire [15:0] array_io_weight_bits_0; // @[TCU.scala 63:21]
  wire [15:0] array_io_weight_bits_1; // @[TCU.scala 63:21]
  wire [15:0] array_io_weight_bits_2; // @[TCU.scala 63:21]
  wire [15:0] array_io_weight_bits_3; // @[TCU.scala 63:21]
  wire [15:0] array_io_weight_bits_4; // @[TCU.scala 63:21]
  wire [15:0] array_io_weight_bits_5; // @[TCU.scala 63:21]
  wire [15:0] array_io_weight_bits_6; // @[TCU.scala 63:21]
  wire [15:0] array_io_weight_bits_7; // @[TCU.scala 63:21]
  wire  array_io_output_ready; // @[TCU.scala 63:21]
  wire  array_io_output_valid; // @[TCU.scala 63:21]
  wire [15:0] array_io_output_bits_0; // @[TCU.scala 63:21]
  wire [15:0] array_io_output_bits_1; // @[TCU.scala 63:21]
  wire [15:0] array_io_output_bits_2; // @[TCU.scala 63:21]
  wire [15:0] array_io_output_bits_3; // @[TCU.scala 63:21]
  wire [15:0] array_io_output_bits_4; // @[TCU.scala 63:21]
  wire [15:0] array_io_output_bits_5; // @[TCU.scala 63:21]
  wire [15:0] array_io_output_bits_6; // @[TCU.scala 63:21]
  wire [15:0] array_io_output_bits_7; // @[TCU.scala 63:21]
  wire  acc_clock; // @[TCU.scala 66:19]
  wire  acc_reset; // @[TCU.scala 66:19]
  wire  acc_io_input_ready; // @[TCU.scala 66:19]
  wire  acc_io_input_valid; // @[TCU.scala 66:19]
  wire [15:0] acc_io_input_bits_0; // @[TCU.scala 66:19]
  wire [15:0] acc_io_input_bits_1; // @[TCU.scala 66:19]
  wire [15:0] acc_io_input_bits_2; // @[TCU.scala 66:19]
  wire [15:0] acc_io_input_bits_3; // @[TCU.scala 66:19]
  wire [15:0] acc_io_input_bits_4; // @[TCU.scala 66:19]
  wire [15:0] acc_io_input_bits_5; // @[TCU.scala 66:19]
  wire [15:0] acc_io_input_bits_6; // @[TCU.scala 66:19]
  wire [15:0] acc_io_input_bits_7; // @[TCU.scala 66:19]
  wire  acc_io_output_ready; // @[TCU.scala 66:19]
  wire  acc_io_output_valid; // @[TCU.scala 66:19]
  wire [15:0] acc_io_output_bits_0; // @[TCU.scala 66:19]
  wire [15:0] acc_io_output_bits_1; // @[TCU.scala 66:19]
  wire [15:0] acc_io_output_bits_2; // @[TCU.scala 66:19]
  wire [15:0] acc_io_output_bits_3; // @[TCU.scala 66:19]
  wire [15:0] acc_io_output_bits_4; // @[TCU.scala 66:19]
  wire [15:0] acc_io_output_bits_5; // @[TCU.scala 66:19]
  wire [15:0] acc_io_output_bits_6; // @[TCU.scala 66:19]
  wire [15:0] acc_io_output_bits_7; // @[TCU.scala 66:19]
  wire  acc_io_control_ready; // @[TCU.scala 66:19]
  wire  acc_io_control_valid; // @[TCU.scala 66:19]
  wire [3:0] acc_io_control_bits_instruction_op; // @[TCU.scala 66:19]
  wire  acc_io_control_bits_instruction_sourceLeft; // @[TCU.scala 66:19]
  wire  acc_io_control_bits_instruction_sourceRight; // @[TCU.scala 66:19]
  wire  acc_io_control_bits_instruction_dest; // @[TCU.scala 66:19]
  wire [5:0] acc_io_control_bits_readAddress; // @[TCU.scala 66:19]
  wire [5:0] acc_io_control_bits_writeAddress; // @[TCU.scala 66:19]
  wire  acc_io_control_bits_accumulate; // @[TCU.scala 66:19]
  wire  acc_io_control_bits_write; // @[TCU.scala 66:19]
  wire  acc_io_control_bits_read; // @[TCU.scala 66:19]
  wire  acc_io_tracepoint; // @[TCU.scala 66:19]
  wire [31:0] acc_io_programCounter; // @[TCU.scala 66:19]
  wire  mem_clock; // @[TCU.scala 69:19]
  wire  mem_reset; // @[TCU.scala 69:19]
  wire  mem_io_portA_control_ready; // @[TCU.scala 69:19]
  wire  mem_io_portA_control_valid; // @[TCU.scala 69:19]
  wire  mem_io_portA_control_bits_write; // @[TCU.scala 69:19]
  wire [5:0] mem_io_portA_control_bits_address; // @[TCU.scala 69:19]
  wire  mem_io_portA_input_ready; // @[TCU.scala 69:19]
  wire  mem_io_portA_input_valid; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portA_input_bits_0; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portA_input_bits_1; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portA_input_bits_2; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portA_input_bits_3; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portA_input_bits_4; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portA_input_bits_5; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portA_input_bits_6; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portA_input_bits_7; // @[TCU.scala 69:19]
  wire  mem_io_portA_output_ready; // @[TCU.scala 69:19]
  wire  mem_io_portA_output_valid; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portA_output_bits_0; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portA_output_bits_1; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portA_output_bits_2; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portA_output_bits_3; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portA_output_bits_4; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portA_output_bits_5; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portA_output_bits_6; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portA_output_bits_7; // @[TCU.scala 69:19]
  wire  mem_io_portB_control_ready; // @[TCU.scala 69:19]
  wire  mem_io_portB_control_valid; // @[TCU.scala 69:19]
  wire  mem_io_portB_control_bits_write; // @[TCU.scala 69:19]
  wire [5:0] mem_io_portB_control_bits_address; // @[TCU.scala 69:19]
  wire  mem_io_portB_input_ready; // @[TCU.scala 69:19]
  wire  mem_io_portB_input_valid; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portB_input_bits_0; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portB_input_bits_1; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portB_input_bits_2; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portB_input_bits_3; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portB_input_bits_4; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portB_input_bits_5; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portB_input_bits_6; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portB_input_bits_7; // @[TCU.scala 69:19]
  wire  mem_io_portB_output_ready; // @[TCU.scala 69:19]
  wire  mem_io_portB_output_valid; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portB_output_bits_0; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portB_output_bits_1; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portB_output_bits_2; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portB_output_bits_3; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portB_output_bits_4; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portB_output_bits_5; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portB_output_bits_6; // @[TCU.scala 69:19]
  wire [15:0] mem_io_portB_output_bits_7; // @[TCU.scala 69:19]
  wire  mem_io_tracepoint; // @[TCU.scala 69:19]
  wire [31:0] mem_io_programCounter; // @[TCU.scala 69:19]
  wire  router_clock; // @[TCU.scala 77:22]
  wire  router_reset; // @[TCU.scala 77:22]
  wire  router_io_control_ready; // @[TCU.scala 77:22]
  wire  router_io_control_valid; // @[TCU.scala 77:22]
  wire [3:0] router_io_control_bits_kind; // @[TCU.scala 77:22]
  wire [5:0] router_io_control_bits_size; // @[TCU.scala 77:22]
  wire  router_io_mem_output_ready; // @[TCU.scala 77:22]
  wire  router_io_mem_output_valid; // @[TCU.scala 77:22]
  wire [15:0] router_io_mem_output_bits_0; // @[TCU.scala 77:22]
  wire [15:0] router_io_mem_output_bits_1; // @[TCU.scala 77:22]
  wire [15:0] router_io_mem_output_bits_2; // @[TCU.scala 77:22]
  wire [15:0] router_io_mem_output_bits_3; // @[TCU.scala 77:22]
  wire [15:0] router_io_mem_output_bits_4; // @[TCU.scala 77:22]
  wire [15:0] router_io_mem_output_bits_5; // @[TCU.scala 77:22]
  wire [15:0] router_io_mem_output_bits_6; // @[TCU.scala 77:22]
  wire [15:0] router_io_mem_output_bits_7; // @[TCU.scala 77:22]
  wire  router_io_mem_input_ready; // @[TCU.scala 77:22]
  wire  router_io_mem_input_valid; // @[TCU.scala 77:22]
  wire [15:0] router_io_mem_input_bits_0; // @[TCU.scala 77:22]
  wire [15:0] router_io_mem_input_bits_1; // @[TCU.scala 77:22]
  wire [15:0] router_io_mem_input_bits_2; // @[TCU.scala 77:22]
  wire [15:0] router_io_mem_input_bits_3; // @[TCU.scala 77:22]
  wire [15:0] router_io_mem_input_bits_4; // @[TCU.scala 77:22]
  wire [15:0] router_io_mem_input_bits_5; // @[TCU.scala 77:22]
  wire [15:0] router_io_mem_input_bits_6; // @[TCU.scala 77:22]
  wire [15:0] router_io_mem_input_bits_7; // @[TCU.scala 77:22]
  wire  router_io_array_input_ready; // @[TCU.scala 77:22]
  wire  router_io_array_input_valid; // @[TCU.scala 77:22]
  wire [15:0] router_io_array_input_bits_0; // @[TCU.scala 77:22]
  wire [15:0] router_io_array_input_bits_1; // @[TCU.scala 77:22]
  wire [15:0] router_io_array_input_bits_2; // @[TCU.scala 77:22]
  wire [15:0] router_io_array_input_bits_3; // @[TCU.scala 77:22]
  wire [15:0] router_io_array_input_bits_4; // @[TCU.scala 77:22]
  wire [15:0] router_io_array_input_bits_5; // @[TCU.scala 77:22]
  wire [15:0] router_io_array_input_bits_6; // @[TCU.scala 77:22]
  wire [15:0] router_io_array_input_bits_7; // @[TCU.scala 77:22]
  wire  router_io_array_output_ready; // @[TCU.scala 77:22]
  wire  router_io_array_output_valid; // @[TCU.scala 77:22]
  wire [15:0] router_io_array_output_bits_0; // @[TCU.scala 77:22]
  wire [15:0] router_io_array_output_bits_1; // @[TCU.scala 77:22]
  wire [15:0] router_io_array_output_bits_2; // @[TCU.scala 77:22]
  wire [15:0] router_io_array_output_bits_3; // @[TCU.scala 77:22]
  wire [15:0] router_io_array_output_bits_4; // @[TCU.scala 77:22]
  wire [15:0] router_io_array_output_bits_5; // @[TCU.scala 77:22]
  wire [15:0] router_io_array_output_bits_6; // @[TCU.scala 77:22]
  wire [15:0] router_io_array_output_bits_7; // @[TCU.scala 77:22]
  wire  router_io_array_weightInput_ready; // @[TCU.scala 77:22]
  wire  router_io_array_weightInput_valid; // @[TCU.scala 77:22]
  wire [15:0] router_io_array_weightInput_bits_0; // @[TCU.scala 77:22]
  wire [15:0] router_io_array_weightInput_bits_1; // @[TCU.scala 77:22]
  wire [15:0] router_io_array_weightInput_bits_2; // @[TCU.scala 77:22]
  wire [15:0] router_io_array_weightInput_bits_3; // @[TCU.scala 77:22]
  wire [15:0] router_io_array_weightInput_bits_4; // @[TCU.scala 77:22]
  wire [15:0] router_io_array_weightInput_bits_5; // @[TCU.scala 77:22]
  wire [15:0] router_io_array_weightInput_bits_6; // @[TCU.scala 77:22]
  wire [15:0] router_io_array_weightInput_bits_7; // @[TCU.scala 77:22]
  wire  router_io_acc_output_ready; // @[TCU.scala 77:22]
  wire  router_io_acc_output_valid; // @[TCU.scala 77:22]
  wire [15:0] router_io_acc_output_bits_0; // @[TCU.scala 77:22]
  wire [15:0] router_io_acc_output_bits_1; // @[TCU.scala 77:22]
  wire [15:0] router_io_acc_output_bits_2; // @[TCU.scala 77:22]
  wire [15:0] router_io_acc_output_bits_3; // @[TCU.scala 77:22]
  wire [15:0] router_io_acc_output_bits_4; // @[TCU.scala 77:22]
  wire [15:0] router_io_acc_output_bits_5; // @[TCU.scala 77:22]
  wire [15:0] router_io_acc_output_bits_6; // @[TCU.scala 77:22]
  wire [15:0] router_io_acc_output_bits_7; // @[TCU.scala 77:22]
  wire  router_io_acc_input_ready; // @[TCU.scala 77:22]
  wire  router_io_acc_input_valid; // @[TCU.scala 77:22]
  wire [15:0] router_io_acc_input_bits_0; // @[TCU.scala 77:22]
  wire [15:0] router_io_acc_input_bits_1; // @[TCU.scala 77:22]
  wire [15:0] router_io_acc_input_bits_2; // @[TCU.scala 77:22]
  wire [15:0] router_io_acc_input_bits_3; // @[TCU.scala 77:22]
  wire [15:0] router_io_acc_input_bits_4; // @[TCU.scala 77:22]
  wire [15:0] router_io_acc_input_bits_5; // @[TCU.scala 77:22]
  wire [15:0] router_io_acc_input_bits_6; // @[TCU.scala 77:22]
  wire [15:0] router_io_acc_input_bits_7; // @[TCU.scala 77:22]
  wire  router_io_timeout; // @[TCU.scala 77:22]
  wire  router_io_tracepoint; // @[TCU.scala 77:22]
  wire [31:0] router_io_programCounter; // @[TCU.scala 77:22]
  wire  hostRouter_io_control_ready; // @[TCU.scala 84:26]
  wire  hostRouter_io_control_valid; // @[TCU.scala 84:26]
  wire [1:0] hostRouter_io_control_bits_kind; // @[TCU.scala 84:26]
  wire  hostRouter_io_dram0_dataIn_ready; // @[TCU.scala 84:26]
  wire  hostRouter_io_dram0_dataIn_valid; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram0_dataIn_bits_0; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram0_dataIn_bits_1; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram0_dataIn_bits_2; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram0_dataIn_bits_3; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram0_dataIn_bits_4; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram0_dataIn_bits_5; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram0_dataIn_bits_6; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram0_dataIn_bits_7; // @[TCU.scala 84:26]
  wire  hostRouter_io_dram0_dataOut_ready; // @[TCU.scala 84:26]
  wire  hostRouter_io_dram0_dataOut_valid; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram0_dataOut_bits_0; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram0_dataOut_bits_1; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram0_dataOut_bits_2; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram0_dataOut_bits_3; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram0_dataOut_bits_4; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram0_dataOut_bits_5; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram0_dataOut_bits_6; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram0_dataOut_bits_7; // @[TCU.scala 84:26]
  wire  hostRouter_io_dram1_dataIn_ready; // @[TCU.scala 84:26]
  wire  hostRouter_io_dram1_dataIn_valid; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram1_dataIn_bits_0; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram1_dataIn_bits_1; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram1_dataIn_bits_2; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram1_dataIn_bits_3; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram1_dataIn_bits_4; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram1_dataIn_bits_5; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram1_dataIn_bits_6; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram1_dataIn_bits_7; // @[TCU.scala 84:26]
  wire  hostRouter_io_dram1_dataOut_ready; // @[TCU.scala 84:26]
  wire  hostRouter_io_dram1_dataOut_valid; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram1_dataOut_bits_0; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram1_dataOut_bits_1; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram1_dataOut_bits_2; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram1_dataOut_bits_3; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram1_dataOut_bits_4; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram1_dataOut_bits_5; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram1_dataOut_bits_6; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_dram1_dataOut_bits_7; // @[TCU.scala 84:26]
  wire  hostRouter_io_mem_output_ready; // @[TCU.scala 84:26]
  wire  hostRouter_io_mem_output_valid; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_mem_output_bits_0; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_mem_output_bits_1; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_mem_output_bits_2; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_mem_output_bits_3; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_mem_output_bits_4; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_mem_output_bits_5; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_mem_output_bits_6; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_mem_output_bits_7; // @[TCU.scala 84:26]
  wire  hostRouter_io_mem_input_ready; // @[TCU.scala 84:26]
  wire  hostRouter_io_mem_input_valid; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_mem_input_bits_0; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_mem_input_bits_1; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_mem_input_bits_2; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_mem_input_bits_3; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_mem_input_bits_4; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_mem_input_bits_5; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_mem_input_bits_6; // @[TCU.scala 84:26]
  wire [15:0] hostRouter_io_mem_input_bits_7; // @[TCU.scala 84:26]
  wire  acc_io_control_q_clock; // @[TCU.scala 107:39]
  wire  acc_io_control_q_reset; // @[TCU.scala 107:39]
  wire  acc_io_control_q_io_enq_ready; // @[TCU.scala 107:39]
  wire  acc_io_control_q_io_enq_valid; // @[TCU.scala 107:39]
  wire [3:0] acc_io_control_q_io_enq_bits_instruction_op; // @[TCU.scala 107:39]
  wire  acc_io_control_q_io_enq_bits_instruction_sourceLeft; // @[TCU.scala 107:39]
  wire  acc_io_control_q_io_enq_bits_instruction_sourceRight; // @[TCU.scala 107:39]
  wire  acc_io_control_q_io_enq_bits_instruction_dest; // @[TCU.scala 107:39]
  wire [5:0] acc_io_control_q_io_enq_bits_readAddress; // @[TCU.scala 107:39]
  wire [5:0] acc_io_control_q_io_enq_bits_writeAddress; // @[TCU.scala 107:39]
  wire  acc_io_control_q_io_enq_bits_accumulate; // @[TCU.scala 107:39]
  wire  acc_io_control_q_io_enq_bits_write; // @[TCU.scala 107:39]
  wire  acc_io_control_q_io_enq_bits_read; // @[TCU.scala 107:39]
  wire  acc_io_control_q_io_deq_ready; // @[TCU.scala 107:39]
  wire  acc_io_control_q_io_deq_valid; // @[TCU.scala 107:39]
  wire [3:0] acc_io_control_q_io_deq_bits_instruction_op; // @[TCU.scala 107:39]
  wire  acc_io_control_q_io_deq_bits_instruction_sourceLeft; // @[TCU.scala 107:39]
  wire  acc_io_control_q_io_deq_bits_instruction_sourceRight; // @[TCU.scala 107:39]
  wire  acc_io_control_q_io_deq_bits_instruction_dest; // @[TCU.scala 107:39]
  wire [5:0] acc_io_control_q_io_deq_bits_readAddress; // @[TCU.scala 107:39]
  wire [5:0] acc_io_control_q_io_deq_bits_writeAddress; // @[TCU.scala 107:39]
  wire  acc_io_control_q_io_deq_bits_accumulate; // @[TCU.scala 107:39]
  wire  acc_io_control_q_io_deq_bits_write; // @[TCU.scala 107:39]
  wire  acc_io_control_q_io_deq_bits_read; // @[TCU.scala 107:39]
  wire  array_io_control_q_clock; // @[TCU.scala 114:41]
  wire  array_io_control_q_reset; // @[TCU.scala 114:41]
  wire  array_io_control_q_io_enq_ready; // @[TCU.scala 114:41]
  wire  array_io_control_q_io_enq_valid; // @[TCU.scala 114:41]
  wire  array_io_control_q_io_enq_bits_load; // @[TCU.scala 114:41]
  wire  array_io_control_q_io_enq_bits_zeroes; // @[TCU.scala 114:41]
  wire  array_io_control_q_io_deq_ready; // @[TCU.scala 114:41]
  wire  array_io_control_q_io_deq_valid; // @[TCU.scala 114:41]
  wire  array_io_control_q_io_deq_bits_load; // @[TCU.scala 114:41]
  wire  array_io_control_q_io_deq_bits_zeroes; // @[TCU.scala 114:41]
  Decoder decoder ( // @[TCU.scala 62:23]
    .clock(decoder_clock),
    .reset(decoder_reset),
    .io_instruction_ready(decoder_io_instruction_ready),
    .io_instruction_valid(decoder_io_instruction_valid),
    .io_instruction_bits_opcode(decoder_io_instruction_bits_opcode),
    .io_instruction_bits_flags(decoder_io_instruction_bits_flags),
    .io_instruction_bits_arguments(decoder_io_instruction_bits_arguments),
    .io_memPortA_ready(decoder_io_memPortA_ready),
    .io_memPortA_valid(decoder_io_memPortA_valid),
    .io_memPortA_bits_write(decoder_io_memPortA_bits_write),
    .io_memPortA_bits_address(decoder_io_memPortA_bits_address),
    .io_memPortB_ready(decoder_io_memPortB_ready),
    .io_memPortB_valid(decoder_io_memPortB_valid),
    .io_memPortB_bits_write(decoder_io_memPortB_bits_write),
    .io_memPortB_bits_address(decoder_io_memPortB_bits_address),
    .io_dram0_ready(decoder_io_dram0_ready),
    .io_dram0_valid(decoder_io_dram0_valid),
    .io_dram0_bits_write(decoder_io_dram0_bits_write),
    .io_dram0_bits_address(decoder_io_dram0_bits_address),
    .io_dram0_bits_size(decoder_io_dram0_bits_size),
    .io_dram1_ready(decoder_io_dram1_ready),
    .io_dram1_valid(decoder_io_dram1_valid),
    .io_dram1_bits_write(decoder_io_dram1_bits_write),
    .io_dram1_bits_address(decoder_io_dram1_bits_address),
    .io_dram1_bits_size(decoder_io_dram1_bits_size),
    .io_dataflow_ready(decoder_io_dataflow_ready),
    .io_dataflow_valid(decoder_io_dataflow_valid),
    .io_dataflow_bits_kind(decoder_io_dataflow_bits_kind),
    .io_dataflow_bits_size(decoder_io_dataflow_bits_size),
    .io_hostDataflow_ready(decoder_io_hostDataflow_ready),
    .io_hostDataflow_valid(decoder_io_hostDataflow_valid),
    .io_hostDataflow_bits_kind(decoder_io_hostDataflow_bits_kind),
    .io_acc_ready(decoder_io_acc_ready),
    .io_acc_valid(decoder_io_acc_valid),
    .io_acc_bits_instruction_op(decoder_io_acc_bits_instruction_op),
    .io_acc_bits_instruction_sourceLeft(decoder_io_acc_bits_instruction_sourceLeft),
    .io_acc_bits_instruction_sourceRight(decoder_io_acc_bits_instruction_sourceRight),
    .io_acc_bits_instruction_dest(decoder_io_acc_bits_instruction_dest),
    .io_acc_bits_readAddress(decoder_io_acc_bits_readAddress),
    .io_acc_bits_writeAddress(decoder_io_acc_bits_writeAddress),
    .io_acc_bits_accumulate(decoder_io_acc_bits_accumulate),
    .io_acc_bits_write(decoder_io_acc_bits_write),
    .io_acc_bits_read(decoder_io_acc_bits_read),
    .io_array_ready(decoder_io_array_ready),
    .io_array_valid(decoder_io_array_valid),
    .io_array_bits_load(decoder_io_array_bits_load),
    .io_array_bits_zeroes(decoder_io_array_bits_zeroes),
    .io_config_dram0AddressOffset(decoder_io_config_dram0AddressOffset),
    .io_config_dram0CacheBehaviour(decoder_io_config_dram0CacheBehaviour),
    .io_config_dram1AddressOffset(decoder_io_config_dram1AddressOffset),
    .io_config_dram1CacheBehaviour(decoder_io_config_dram1CacheBehaviour),
    .io_status_ready(decoder_io_status_ready),
    .io_status_valid(decoder_io_status_valid),
    .io_status_bits_last(decoder_io_status_bits_last),
    .io_status_bits_bits_opcode(decoder_io_status_bits_bits_opcode),
    .io_status_bits_bits_flags(decoder_io_status_bits_bits_flags),
    .io_status_bits_bits_arguments(decoder_io_status_bits_bits_arguments),
    .io_timeout(decoder_io_timeout),
    .io_error(decoder_io_error),
    .io_tracepoint(decoder_io_tracepoint),
    .io_programCounter(decoder_io_programCounter)
  );
  SystolicArray array ( // @[TCU.scala 63:21]
    .clock(array_clock),
    .reset(array_reset),
    .io_control_ready(array_io_control_ready),
    .io_control_valid(array_io_control_valid),
    .io_control_bits_load(array_io_control_bits_load),
    .io_control_bits_zeroes(array_io_control_bits_zeroes),
    .io_input_ready(array_io_input_ready),
    .io_input_valid(array_io_input_valid),
    .io_input_bits_0(array_io_input_bits_0),
    .io_input_bits_1(array_io_input_bits_1),
    .io_input_bits_2(array_io_input_bits_2),
    .io_input_bits_3(array_io_input_bits_3),
    .io_input_bits_4(array_io_input_bits_4),
    .io_input_bits_5(array_io_input_bits_5),
    .io_input_bits_6(array_io_input_bits_6),
    .io_input_bits_7(array_io_input_bits_7),
    .io_weight_ready(array_io_weight_ready),
    .io_weight_valid(array_io_weight_valid),
    .io_weight_bits_0(array_io_weight_bits_0),
    .io_weight_bits_1(array_io_weight_bits_1),
    .io_weight_bits_2(array_io_weight_bits_2),
    .io_weight_bits_3(array_io_weight_bits_3),
    .io_weight_bits_4(array_io_weight_bits_4),
    .io_weight_bits_5(array_io_weight_bits_5),
    .io_weight_bits_6(array_io_weight_bits_6),
    .io_weight_bits_7(array_io_weight_bits_7),
    .io_output_ready(array_io_output_ready),
    .io_output_valid(array_io_output_valid),
    .io_output_bits_0(array_io_output_bits_0),
    .io_output_bits_1(array_io_output_bits_1),
    .io_output_bits_2(array_io_output_bits_2),
    .io_output_bits_3(array_io_output_bits_3),
    .io_output_bits_4(array_io_output_bits_4),
    .io_output_bits_5(array_io_output_bits_5),
    .io_output_bits_6(array_io_output_bits_6),
    .io_output_bits_7(array_io_output_bits_7)
  );
  AccumulatorWithALUArray acc ( // @[TCU.scala 66:19]
    .clock(acc_clock),
    .reset(acc_reset),
    .io_input_ready(acc_io_input_ready),
    .io_input_valid(acc_io_input_valid),
    .io_input_bits_0(acc_io_input_bits_0),
    .io_input_bits_1(acc_io_input_bits_1),
    .io_input_bits_2(acc_io_input_bits_2),
    .io_input_bits_3(acc_io_input_bits_3),
    .io_input_bits_4(acc_io_input_bits_4),
    .io_input_bits_5(acc_io_input_bits_5),
    .io_input_bits_6(acc_io_input_bits_6),
    .io_input_bits_7(acc_io_input_bits_7),
    .io_output_ready(acc_io_output_ready),
    .io_output_valid(acc_io_output_valid),
    .io_output_bits_0(acc_io_output_bits_0),
    .io_output_bits_1(acc_io_output_bits_1),
    .io_output_bits_2(acc_io_output_bits_2),
    .io_output_bits_3(acc_io_output_bits_3),
    .io_output_bits_4(acc_io_output_bits_4),
    .io_output_bits_5(acc_io_output_bits_5),
    .io_output_bits_6(acc_io_output_bits_6),
    .io_output_bits_7(acc_io_output_bits_7),
    .io_control_ready(acc_io_control_ready),
    .io_control_valid(acc_io_control_valid),
    .io_control_bits_instruction_op(acc_io_control_bits_instruction_op),
    .io_control_bits_instruction_sourceLeft(acc_io_control_bits_instruction_sourceLeft),
    .io_control_bits_instruction_sourceRight(acc_io_control_bits_instruction_sourceRight),
    .io_control_bits_instruction_dest(acc_io_control_bits_instruction_dest),
    .io_control_bits_readAddress(acc_io_control_bits_readAddress),
    .io_control_bits_writeAddress(acc_io_control_bits_writeAddress),
    .io_control_bits_accumulate(acc_io_control_bits_accumulate),
    .io_control_bits_write(acc_io_control_bits_write),
    .io_control_bits_read(acc_io_control_bits_read),
    .io_tracepoint(acc_io_tracepoint),
    .io_programCounter(acc_io_programCounter)
  );
  DualPortMem mem ( // @[TCU.scala 69:19]
    .clock(mem_clock),
    .reset(mem_reset),
    .io_portA_control_ready(mem_io_portA_control_ready),
    .io_portA_control_valid(mem_io_portA_control_valid),
    .io_portA_control_bits_write(mem_io_portA_control_bits_write),
    .io_portA_control_bits_address(mem_io_portA_control_bits_address),
    .io_portA_input_ready(mem_io_portA_input_ready),
    .io_portA_input_valid(mem_io_portA_input_valid),
    .io_portA_input_bits_0(mem_io_portA_input_bits_0),
    .io_portA_input_bits_1(mem_io_portA_input_bits_1),
    .io_portA_input_bits_2(mem_io_portA_input_bits_2),
    .io_portA_input_bits_3(mem_io_portA_input_bits_3),
    .io_portA_input_bits_4(mem_io_portA_input_bits_4),
    .io_portA_input_bits_5(mem_io_portA_input_bits_5),
    .io_portA_input_bits_6(mem_io_portA_input_bits_6),
    .io_portA_input_bits_7(mem_io_portA_input_bits_7),
    .io_portA_output_ready(mem_io_portA_output_ready),
    .io_portA_output_valid(mem_io_portA_output_valid),
    .io_portA_output_bits_0(mem_io_portA_output_bits_0),
    .io_portA_output_bits_1(mem_io_portA_output_bits_1),
    .io_portA_output_bits_2(mem_io_portA_output_bits_2),
    .io_portA_output_bits_3(mem_io_portA_output_bits_3),
    .io_portA_output_bits_4(mem_io_portA_output_bits_4),
    .io_portA_output_bits_5(mem_io_portA_output_bits_5),
    .io_portA_output_bits_6(mem_io_portA_output_bits_6),
    .io_portA_output_bits_7(mem_io_portA_output_bits_7),
    .io_portB_control_ready(mem_io_portB_control_ready),
    .io_portB_control_valid(mem_io_portB_control_valid),
    .io_portB_control_bits_write(mem_io_portB_control_bits_write),
    .io_portB_control_bits_address(mem_io_portB_control_bits_address),
    .io_portB_input_ready(mem_io_portB_input_ready),
    .io_portB_input_valid(mem_io_portB_input_valid),
    .io_portB_input_bits_0(mem_io_portB_input_bits_0),
    .io_portB_input_bits_1(mem_io_portB_input_bits_1),
    .io_portB_input_bits_2(mem_io_portB_input_bits_2),
    .io_portB_input_bits_3(mem_io_portB_input_bits_3),
    .io_portB_input_bits_4(mem_io_portB_input_bits_4),
    .io_portB_input_bits_5(mem_io_portB_input_bits_5),
    .io_portB_input_bits_6(mem_io_portB_input_bits_6),
    .io_portB_input_bits_7(mem_io_portB_input_bits_7),
    .io_portB_output_ready(mem_io_portB_output_ready),
    .io_portB_output_valid(mem_io_portB_output_valid),
    .io_portB_output_bits_0(mem_io_portB_output_bits_0),
    .io_portB_output_bits_1(mem_io_portB_output_bits_1),
    .io_portB_output_bits_2(mem_io_portB_output_bits_2),
    .io_portB_output_bits_3(mem_io_portB_output_bits_3),
    .io_portB_output_bits_4(mem_io_portB_output_bits_4),
    .io_portB_output_bits_5(mem_io_portB_output_bits_5),
    .io_portB_output_bits_6(mem_io_portB_output_bits_6),
    .io_portB_output_bits_7(mem_io_portB_output_bits_7),
    .io_tracepoint(mem_io_tracepoint),
    .io_programCounter(mem_io_programCounter)
  );
  LocalRouter router ( // @[TCU.scala 77:22]
    .clock(router_clock),
    .reset(router_reset),
    .io_control_ready(router_io_control_ready),
    .io_control_valid(router_io_control_valid),
    .io_control_bits_kind(router_io_control_bits_kind),
    .io_control_bits_size(router_io_control_bits_size),
    .io_mem_output_ready(router_io_mem_output_ready),
    .io_mem_output_valid(router_io_mem_output_valid),
    .io_mem_output_bits_0(router_io_mem_output_bits_0),
    .io_mem_output_bits_1(router_io_mem_output_bits_1),
    .io_mem_output_bits_2(router_io_mem_output_bits_2),
    .io_mem_output_bits_3(router_io_mem_output_bits_3),
    .io_mem_output_bits_4(router_io_mem_output_bits_4),
    .io_mem_output_bits_5(router_io_mem_output_bits_5),
    .io_mem_output_bits_6(router_io_mem_output_bits_6),
    .io_mem_output_bits_7(router_io_mem_output_bits_7),
    .io_mem_input_ready(router_io_mem_input_ready),
    .io_mem_input_valid(router_io_mem_input_valid),
    .io_mem_input_bits_0(router_io_mem_input_bits_0),
    .io_mem_input_bits_1(router_io_mem_input_bits_1),
    .io_mem_input_bits_2(router_io_mem_input_bits_2),
    .io_mem_input_bits_3(router_io_mem_input_bits_3),
    .io_mem_input_bits_4(router_io_mem_input_bits_4),
    .io_mem_input_bits_5(router_io_mem_input_bits_5),
    .io_mem_input_bits_6(router_io_mem_input_bits_6),
    .io_mem_input_bits_7(router_io_mem_input_bits_7),
    .io_array_input_ready(router_io_array_input_ready),
    .io_array_input_valid(router_io_array_input_valid),
    .io_array_input_bits_0(router_io_array_input_bits_0),
    .io_array_input_bits_1(router_io_array_input_bits_1),
    .io_array_input_bits_2(router_io_array_input_bits_2),
    .io_array_input_bits_3(router_io_array_input_bits_3),
    .io_array_input_bits_4(router_io_array_input_bits_4),
    .io_array_input_bits_5(router_io_array_input_bits_5),
    .io_array_input_bits_6(router_io_array_input_bits_6),
    .io_array_input_bits_7(router_io_array_input_bits_7),
    .io_array_output_ready(router_io_array_output_ready),
    .io_array_output_valid(router_io_array_output_valid),
    .io_array_output_bits_0(router_io_array_output_bits_0),
    .io_array_output_bits_1(router_io_array_output_bits_1),
    .io_array_output_bits_2(router_io_array_output_bits_2),
    .io_array_output_bits_3(router_io_array_output_bits_3),
    .io_array_output_bits_4(router_io_array_output_bits_4),
    .io_array_output_bits_5(router_io_array_output_bits_5),
    .io_array_output_bits_6(router_io_array_output_bits_6),
    .io_array_output_bits_7(router_io_array_output_bits_7),
    .io_array_weightInput_ready(router_io_array_weightInput_ready),
    .io_array_weightInput_valid(router_io_array_weightInput_valid),
    .io_array_weightInput_bits_0(router_io_array_weightInput_bits_0),
    .io_array_weightInput_bits_1(router_io_array_weightInput_bits_1),
    .io_array_weightInput_bits_2(router_io_array_weightInput_bits_2),
    .io_array_weightInput_bits_3(router_io_array_weightInput_bits_3),
    .io_array_weightInput_bits_4(router_io_array_weightInput_bits_4),
    .io_array_weightInput_bits_5(router_io_array_weightInput_bits_5),
    .io_array_weightInput_bits_6(router_io_array_weightInput_bits_6),
    .io_array_weightInput_bits_7(router_io_array_weightInput_bits_7),
    .io_acc_output_ready(router_io_acc_output_ready),
    .io_acc_output_valid(router_io_acc_output_valid),
    .io_acc_output_bits_0(router_io_acc_output_bits_0),
    .io_acc_output_bits_1(router_io_acc_output_bits_1),
    .io_acc_output_bits_2(router_io_acc_output_bits_2),
    .io_acc_output_bits_3(router_io_acc_output_bits_3),
    .io_acc_output_bits_4(router_io_acc_output_bits_4),
    .io_acc_output_bits_5(router_io_acc_output_bits_5),
    .io_acc_output_bits_6(router_io_acc_output_bits_6),
    .io_acc_output_bits_7(router_io_acc_output_bits_7),
    .io_acc_input_ready(router_io_acc_input_ready),
    .io_acc_input_valid(router_io_acc_input_valid),
    .io_acc_input_bits_0(router_io_acc_input_bits_0),
    .io_acc_input_bits_1(router_io_acc_input_bits_1),
    .io_acc_input_bits_2(router_io_acc_input_bits_2),
    .io_acc_input_bits_3(router_io_acc_input_bits_3),
    .io_acc_input_bits_4(router_io_acc_input_bits_4),
    .io_acc_input_bits_5(router_io_acc_input_bits_5),
    .io_acc_input_bits_6(router_io_acc_input_bits_6),
    .io_acc_input_bits_7(router_io_acc_input_bits_7),
    .io_timeout(router_io_timeout),
    .io_tracepoint(router_io_tracepoint),
    .io_programCounter(router_io_programCounter)
  );
  HostRouter hostRouter ( // @[TCU.scala 84:26]
    .io_control_ready(hostRouter_io_control_ready),
    .io_control_valid(hostRouter_io_control_valid),
    .io_control_bits_kind(hostRouter_io_control_bits_kind),
    .io_dram0_dataIn_ready(hostRouter_io_dram0_dataIn_ready),
    .io_dram0_dataIn_valid(hostRouter_io_dram0_dataIn_valid),
    .io_dram0_dataIn_bits_0(hostRouter_io_dram0_dataIn_bits_0),
    .io_dram0_dataIn_bits_1(hostRouter_io_dram0_dataIn_bits_1),
    .io_dram0_dataIn_bits_2(hostRouter_io_dram0_dataIn_bits_2),
    .io_dram0_dataIn_bits_3(hostRouter_io_dram0_dataIn_bits_3),
    .io_dram0_dataIn_bits_4(hostRouter_io_dram0_dataIn_bits_4),
    .io_dram0_dataIn_bits_5(hostRouter_io_dram0_dataIn_bits_5),
    .io_dram0_dataIn_bits_6(hostRouter_io_dram0_dataIn_bits_6),
    .io_dram0_dataIn_bits_7(hostRouter_io_dram0_dataIn_bits_7),
    .io_dram0_dataOut_ready(hostRouter_io_dram0_dataOut_ready),
    .io_dram0_dataOut_valid(hostRouter_io_dram0_dataOut_valid),
    .io_dram0_dataOut_bits_0(hostRouter_io_dram0_dataOut_bits_0),
    .io_dram0_dataOut_bits_1(hostRouter_io_dram0_dataOut_bits_1),
    .io_dram0_dataOut_bits_2(hostRouter_io_dram0_dataOut_bits_2),
    .io_dram0_dataOut_bits_3(hostRouter_io_dram0_dataOut_bits_3),
    .io_dram0_dataOut_bits_4(hostRouter_io_dram0_dataOut_bits_4),
    .io_dram0_dataOut_bits_5(hostRouter_io_dram0_dataOut_bits_5),
    .io_dram0_dataOut_bits_6(hostRouter_io_dram0_dataOut_bits_6),
    .io_dram0_dataOut_bits_7(hostRouter_io_dram0_dataOut_bits_7),
    .io_dram1_dataIn_ready(hostRouter_io_dram1_dataIn_ready),
    .io_dram1_dataIn_valid(hostRouter_io_dram1_dataIn_valid),
    .io_dram1_dataIn_bits_0(hostRouter_io_dram1_dataIn_bits_0),
    .io_dram1_dataIn_bits_1(hostRouter_io_dram1_dataIn_bits_1),
    .io_dram1_dataIn_bits_2(hostRouter_io_dram1_dataIn_bits_2),
    .io_dram1_dataIn_bits_3(hostRouter_io_dram1_dataIn_bits_3),
    .io_dram1_dataIn_bits_4(hostRouter_io_dram1_dataIn_bits_4),
    .io_dram1_dataIn_bits_5(hostRouter_io_dram1_dataIn_bits_5),
    .io_dram1_dataIn_bits_6(hostRouter_io_dram1_dataIn_bits_6),
    .io_dram1_dataIn_bits_7(hostRouter_io_dram1_dataIn_bits_7),
    .io_dram1_dataOut_ready(hostRouter_io_dram1_dataOut_ready),
    .io_dram1_dataOut_valid(hostRouter_io_dram1_dataOut_valid),
    .io_dram1_dataOut_bits_0(hostRouter_io_dram1_dataOut_bits_0),
    .io_dram1_dataOut_bits_1(hostRouter_io_dram1_dataOut_bits_1),
    .io_dram1_dataOut_bits_2(hostRouter_io_dram1_dataOut_bits_2),
    .io_dram1_dataOut_bits_3(hostRouter_io_dram1_dataOut_bits_3),
    .io_dram1_dataOut_bits_4(hostRouter_io_dram1_dataOut_bits_4),
    .io_dram1_dataOut_bits_5(hostRouter_io_dram1_dataOut_bits_5),
    .io_dram1_dataOut_bits_6(hostRouter_io_dram1_dataOut_bits_6),
    .io_dram1_dataOut_bits_7(hostRouter_io_dram1_dataOut_bits_7),
    .io_mem_output_ready(hostRouter_io_mem_output_ready),
    .io_mem_output_valid(hostRouter_io_mem_output_valid),
    .io_mem_output_bits_0(hostRouter_io_mem_output_bits_0),
    .io_mem_output_bits_1(hostRouter_io_mem_output_bits_1),
    .io_mem_output_bits_2(hostRouter_io_mem_output_bits_2),
    .io_mem_output_bits_3(hostRouter_io_mem_output_bits_3),
    .io_mem_output_bits_4(hostRouter_io_mem_output_bits_4),
    .io_mem_output_bits_5(hostRouter_io_mem_output_bits_5),
    .io_mem_output_bits_6(hostRouter_io_mem_output_bits_6),
    .io_mem_output_bits_7(hostRouter_io_mem_output_bits_7),
    .io_mem_input_ready(hostRouter_io_mem_input_ready),
    .io_mem_input_valid(hostRouter_io_mem_input_valid),
    .io_mem_input_bits_0(hostRouter_io_mem_input_bits_0),
    .io_mem_input_bits_1(hostRouter_io_mem_input_bits_1),
    .io_mem_input_bits_2(hostRouter_io_mem_input_bits_2),
    .io_mem_input_bits_3(hostRouter_io_mem_input_bits_3),
    .io_mem_input_bits_4(hostRouter_io_mem_input_bits_4),
    .io_mem_input_bits_5(hostRouter_io_mem_input_bits_5),
    .io_mem_input_bits_6(hostRouter_io_mem_input_bits_6),
    .io_mem_input_bits_7(hostRouter_io_mem_input_bits_7)
  );
  Queue_26 acc_io_control_q ( // @[TCU.scala 107:39]
    .clock(acc_io_control_q_clock),
    .reset(acc_io_control_q_reset),
    .io_enq_ready(acc_io_control_q_io_enq_ready),
    .io_enq_valid(acc_io_control_q_io_enq_valid),
    .io_enq_bits_instruction_op(acc_io_control_q_io_enq_bits_instruction_op),
    .io_enq_bits_instruction_sourceLeft(acc_io_control_q_io_enq_bits_instruction_sourceLeft),
    .io_enq_bits_instruction_sourceRight(acc_io_control_q_io_enq_bits_instruction_sourceRight),
    .io_enq_bits_instruction_dest(acc_io_control_q_io_enq_bits_instruction_dest),
    .io_enq_bits_readAddress(acc_io_control_q_io_enq_bits_readAddress),
    .io_enq_bits_writeAddress(acc_io_control_q_io_enq_bits_writeAddress),
    .io_enq_bits_accumulate(acc_io_control_q_io_enq_bits_accumulate),
    .io_enq_bits_write(acc_io_control_q_io_enq_bits_write),
    .io_enq_bits_read(acc_io_control_q_io_enq_bits_read),
    .io_deq_ready(acc_io_control_q_io_deq_ready),
    .io_deq_valid(acc_io_control_q_io_deq_valid),
    .io_deq_bits_instruction_op(acc_io_control_q_io_deq_bits_instruction_op),
    .io_deq_bits_instruction_sourceLeft(acc_io_control_q_io_deq_bits_instruction_sourceLeft),
    .io_deq_bits_instruction_sourceRight(acc_io_control_q_io_deq_bits_instruction_sourceRight),
    .io_deq_bits_instruction_dest(acc_io_control_q_io_deq_bits_instruction_dest),
    .io_deq_bits_readAddress(acc_io_control_q_io_deq_bits_readAddress),
    .io_deq_bits_writeAddress(acc_io_control_q_io_deq_bits_writeAddress),
    .io_deq_bits_accumulate(acc_io_control_q_io_deq_bits_accumulate),
    .io_deq_bits_write(acc_io_control_q_io_deq_bits_write),
    .io_deq_bits_read(acc_io_control_q_io_deq_bits_read)
  );
  Queue_27 array_io_control_q ( // @[TCU.scala 114:41]
    .clock(array_io_control_q_clock),
    .reset(array_io_control_q_reset),
    .io_enq_ready(array_io_control_q_io_enq_ready),
    .io_enq_valid(array_io_control_q_io_enq_valid),
    .io_enq_bits_load(array_io_control_q_io_enq_bits_load),
    .io_enq_bits_zeroes(array_io_control_q_io_enq_bits_zeroes),
    .io_deq_ready(array_io_control_q_io_deq_ready),
    .io_deq_valid(array_io_control_q_io_deq_valid),
    .io_deq_bits_load(array_io_control_q_io_deq_bits_load),
    .io_deq_bits_zeroes(array_io_control_q_io_deq_bits_zeroes)
  );
  assign io_instruction_ready = decoder_io_instruction_ready; // @[TCU.scala 94:26]
  assign io_status_valid = decoder_io_status_valid; // @[TCU.scala 95:13]
  assign io_status_bits_last = decoder_io_status_bits_last; // @[TCU.scala 95:13]
  assign io_status_bits_bits_opcode = decoder_io_status_bits_bits_opcode; // @[TCU.scala 95:13]
  assign io_status_bits_bits_flags = decoder_io_status_bits_bits_flags; // @[TCU.scala 95:13]
  assign io_status_bits_bits_arguments = decoder_io_status_bits_bits_arguments; // @[TCU.scala 95:13]
  assign io_dram0_control_valid = decoder_io_dram0_valid; // @[TCU.scala 96:20]
  assign io_dram0_control_bits_write = decoder_io_dram0_bits_write; // @[TCU.scala 96:20]
  assign io_dram0_control_bits_address = decoder_io_dram0_bits_address; // @[TCU.scala 96:20]
  assign io_dram0_control_bits_size = decoder_io_dram0_bits_size; // @[TCU.scala 96:20]
  assign io_dram0_dataIn_ready = hostRouter_io_dram0_dataIn_ready; // @[TCU.scala 152:30]
  assign io_dram0_dataOut_valid = hostRouter_io_dram0_dataOut_valid; // @[TCU.scala 153:20]
  assign io_dram0_dataOut_bits_0 = hostRouter_io_dram0_dataOut_bits_0; // @[TCU.scala 153:20]
  assign io_dram0_dataOut_bits_1 = hostRouter_io_dram0_dataOut_bits_1; // @[TCU.scala 153:20]
  assign io_dram0_dataOut_bits_2 = hostRouter_io_dram0_dataOut_bits_2; // @[TCU.scala 153:20]
  assign io_dram0_dataOut_bits_3 = hostRouter_io_dram0_dataOut_bits_3; // @[TCU.scala 153:20]
  assign io_dram0_dataOut_bits_4 = hostRouter_io_dram0_dataOut_bits_4; // @[TCU.scala 153:20]
  assign io_dram0_dataOut_bits_5 = hostRouter_io_dram0_dataOut_bits_5; // @[TCU.scala 153:20]
  assign io_dram0_dataOut_bits_6 = hostRouter_io_dram0_dataOut_bits_6; // @[TCU.scala 153:20]
  assign io_dram0_dataOut_bits_7 = hostRouter_io_dram0_dataOut_bits_7; // @[TCU.scala 153:20]
  assign io_dram1_control_valid = decoder_io_dram1_valid; // @[TCU.scala 97:20]
  assign io_dram1_control_bits_write = decoder_io_dram1_bits_write; // @[TCU.scala 97:20]
  assign io_dram1_control_bits_address = decoder_io_dram1_bits_address; // @[TCU.scala 97:20]
  assign io_dram1_control_bits_size = decoder_io_dram1_bits_size; // @[TCU.scala 97:20]
  assign io_dram1_dataIn_ready = hostRouter_io_dram1_dataIn_ready; // @[TCU.scala 155:30]
  assign io_dram1_dataOut_valid = hostRouter_io_dram1_dataOut_valid; // @[TCU.scala 156:20]
  assign io_dram1_dataOut_bits_0 = hostRouter_io_dram1_dataOut_bits_0; // @[TCU.scala 156:20]
  assign io_dram1_dataOut_bits_1 = hostRouter_io_dram1_dataOut_bits_1; // @[TCU.scala 156:20]
  assign io_dram1_dataOut_bits_2 = hostRouter_io_dram1_dataOut_bits_2; // @[TCU.scala 156:20]
  assign io_dram1_dataOut_bits_3 = hostRouter_io_dram1_dataOut_bits_3; // @[TCU.scala 156:20]
  assign io_dram1_dataOut_bits_4 = hostRouter_io_dram1_dataOut_bits_4; // @[TCU.scala 156:20]
  assign io_dram1_dataOut_bits_5 = hostRouter_io_dram1_dataOut_bits_5; // @[TCU.scala 156:20]
  assign io_dram1_dataOut_bits_6 = hostRouter_io_dram1_dataOut_bits_6; // @[TCU.scala 156:20]
  assign io_dram1_dataOut_bits_7 = hostRouter_io_dram1_dataOut_bits_7; // @[TCU.scala 156:20]
  assign io_config_dram0AddressOffset = decoder_io_config_dram0AddressOffset; // @[TCU.scala 98:13]
  assign io_config_dram0CacheBehaviour = decoder_io_config_dram0CacheBehaviour; // @[TCU.scala 98:13]
  assign io_config_dram1AddressOffset = decoder_io_config_dram1AddressOffset; // @[TCU.scala 98:13]
  assign io_config_dram1CacheBehaviour = decoder_io_config_dram1CacheBehaviour; // @[TCU.scala 98:13]
  assign io_timeout = decoder_io_timeout; // @[TCU.scala 99:14]
  assign io_error = decoder_io_error; // @[TCU.scala 100:12]
  assign io_tracepoint = decoder_io_tracepoint; // @[TCU.scala 101:17]
  assign io_programCounter = decoder_io_programCounter; // @[TCU.scala 102:21]
  assign decoder_clock = clock;
  assign decoder_reset = reset;
  assign decoder_io_instruction_valid = io_instruction_valid; // @[TCU.scala 94:26]
  assign decoder_io_instruction_bits_opcode = io_instruction_bits_opcode; // @[TCU.scala 94:26]
  assign decoder_io_instruction_bits_flags = io_instruction_bits_flags; // @[TCU.scala 94:26]
  assign decoder_io_instruction_bits_arguments = io_instruction_bits_arguments; // @[TCU.scala 94:26]
  assign decoder_io_memPortA_ready = mem_io_portA_control_ready; // @[TCU.scala 121:17]
  assign decoder_io_memPortB_ready = mem_io_portB_control_ready; // @[TCU.scala 145:17]
  assign decoder_io_dram0_ready = io_dram0_control_ready; // @[TCU.scala 96:20]
  assign decoder_io_dram1_ready = io_dram1_control_ready; // @[TCU.scala 97:20]
  assign decoder_io_dataflow_ready = router_io_control_ready; // @[TCU.scala 130:21]
  assign decoder_io_hostDataflow_ready = hostRouter_io_control_ready; // @[TCU.scala 143:25]
  assign decoder_io_acc_ready = acc_io_control_q_io_enq_ready; // @[TCU.scala 107:39]
  assign decoder_io_array_ready = array_io_control_q_io_enq_ready; // @[TCU.scala 114:41]
  assign decoder_io_status_ready = io_status_ready; // @[TCU.scala 95:13]
  assign array_clock = clock;
  assign array_reset = reset;
  assign array_io_control_valid = array_io_control_q_io_deq_valid; // @[TCU.scala 114:20]
  assign array_io_control_bits_load = array_io_control_q_io_deq_bits_load; // @[TCU.scala 114:20]
  assign array_io_control_bits_zeroes = array_io_control_q_io_deq_bits_zeroes; // @[TCU.scala 114:20]
  assign array_io_input_valid = router_io_array_input_valid; // @[TCU.scala 135:18]
  assign array_io_input_bits_0 = router_io_array_input_bits_0; // @[TCU.scala 135:18]
  assign array_io_input_bits_1 = router_io_array_input_bits_1; // @[TCU.scala 135:18]
  assign array_io_input_bits_2 = router_io_array_input_bits_2; // @[TCU.scala 135:18]
  assign array_io_input_bits_3 = router_io_array_input_bits_3; // @[TCU.scala 135:18]
  assign array_io_input_bits_4 = router_io_array_input_bits_4; // @[TCU.scala 135:18]
  assign array_io_input_bits_5 = router_io_array_input_bits_5; // @[TCU.scala 135:18]
  assign array_io_input_bits_6 = router_io_array_input_bits_6; // @[TCU.scala 135:18]
  assign array_io_input_bits_7 = router_io_array_input_bits_7; // @[TCU.scala 135:18]
  assign array_io_weight_valid = router_io_array_weightInput_valid; // @[TCU.scala 137:19]
  assign array_io_weight_bits_0 = router_io_array_weightInput_bits_0; // @[TCU.scala 137:19]
  assign array_io_weight_bits_1 = router_io_array_weightInput_bits_1; // @[TCU.scala 137:19]
  assign array_io_weight_bits_2 = router_io_array_weightInput_bits_2; // @[TCU.scala 137:19]
  assign array_io_weight_bits_3 = router_io_array_weightInput_bits_3; // @[TCU.scala 137:19]
  assign array_io_weight_bits_4 = router_io_array_weightInput_bits_4; // @[TCU.scala 137:19]
  assign array_io_weight_bits_5 = router_io_array_weightInput_bits_5; // @[TCU.scala 137:19]
  assign array_io_weight_bits_6 = router_io_array_weightInput_bits_6; // @[TCU.scala 137:19]
  assign array_io_weight_bits_7 = router_io_array_weightInput_bits_7; // @[TCU.scala 137:19]
  assign array_io_output_ready = router_io_array_output_ready; // @[TCU.scala 136:26]
  assign acc_clock = clock;
  assign acc_reset = reset;
  assign acc_io_input_valid = router_io_acc_input_valid; // @[TCU.scala 139:16]
  assign acc_io_input_bits_0 = router_io_acc_input_bits_0; // @[TCU.scala 139:16]
  assign acc_io_input_bits_1 = router_io_acc_input_bits_1; // @[TCU.scala 139:16]
  assign acc_io_input_bits_2 = router_io_acc_input_bits_2; // @[TCU.scala 139:16]
  assign acc_io_input_bits_3 = router_io_acc_input_bits_3; // @[TCU.scala 139:16]
  assign acc_io_input_bits_4 = router_io_acc_input_bits_4; // @[TCU.scala 139:16]
  assign acc_io_input_bits_5 = router_io_acc_input_bits_5; // @[TCU.scala 139:16]
  assign acc_io_input_bits_6 = router_io_acc_input_bits_6; // @[TCU.scala 139:16]
  assign acc_io_input_bits_7 = router_io_acc_input_bits_7; // @[TCU.scala 139:16]
  assign acc_io_output_ready = router_io_acc_output_ready; // @[TCU.scala 140:24]
  assign acc_io_control_valid = acc_io_control_q_io_deq_valid; // @[TCU.scala 107:18]
  assign acc_io_control_bits_instruction_op = acc_io_control_q_io_deq_bits_instruction_op; // @[TCU.scala 107:18]
  assign acc_io_control_bits_instruction_sourceLeft = acc_io_control_q_io_deq_bits_instruction_sourceLeft; // @[TCU.scala 107:18]
  assign acc_io_control_bits_instruction_sourceRight = acc_io_control_q_io_deq_bits_instruction_sourceRight; // @[TCU.scala 107:18]
  assign acc_io_control_bits_instruction_dest = acc_io_control_q_io_deq_bits_instruction_dest; // @[TCU.scala 107:18]
  assign acc_io_control_bits_readAddress = acc_io_control_q_io_deq_bits_readAddress; // @[TCU.scala 107:18]
  assign acc_io_control_bits_writeAddress = acc_io_control_q_io_deq_bits_writeAddress; // @[TCU.scala 107:18]
  assign acc_io_control_bits_accumulate = acc_io_control_q_io_deq_bits_accumulate; // @[TCU.scala 107:18]
  assign acc_io_control_bits_write = acc_io_control_q_io_deq_bits_write; // @[TCU.scala 107:18]
  assign acc_io_control_bits_read = acc_io_control_q_io_deq_bits_read; // @[TCU.scala 107:18]
  assign acc_io_tracepoint = decoder_io_tracepoint; // @[TCU.scala 108:21]
  assign acc_io_programCounter = decoder_io_programCounter; // @[TCU.scala 109:25]
  assign mem_clock = clock;
  assign mem_reset = reset;
  assign mem_io_portA_control_valid = decoder_io_memPortA_valid; // @[TCU.scala 121:17]
  assign mem_io_portA_control_bits_write = decoder_io_memPortA_bits_write; // @[TCU.scala 121:17]
  assign mem_io_portA_control_bits_address = decoder_io_memPortA_bits_address; // @[TCU.scala 121:17]
  assign mem_io_portA_input_valid = router_io_mem_input_valid; // @[TCU.scala 133:15]
  assign mem_io_portA_input_bits_0 = router_io_mem_input_bits_0; // @[TCU.scala 133:15]
  assign mem_io_portA_input_bits_1 = router_io_mem_input_bits_1; // @[TCU.scala 133:15]
  assign mem_io_portA_input_bits_2 = router_io_mem_input_bits_2; // @[TCU.scala 133:15]
  assign mem_io_portA_input_bits_3 = router_io_mem_input_bits_3; // @[TCU.scala 133:15]
  assign mem_io_portA_input_bits_4 = router_io_mem_input_bits_4; // @[TCU.scala 133:15]
  assign mem_io_portA_input_bits_5 = router_io_mem_input_bits_5; // @[TCU.scala 133:15]
  assign mem_io_portA_input_bits_6 = router_io_mem_input_bits_6; // @[TCU.scala 133:15]
  assign mem_io_portA_input_bits_7 = router_io_mem_input_bits_7; // @[TCU.scala 133:15]
  assign mem_io_portA_output_ready = router_io_mem_output_ready; // @[TCU.scala 132:24]
  assign mem_io_portB_control_valid = decoder_io_memPortB_valid; // @[TCU.scala 145:17]
  assign mem_io_portB_control_bits_write = decoder_io_memPortB_bits_write; // @[TCU.scala 145:17]
  assign mem_io_portB_control_bits_address = decoder_io_memPortB_bits_address; // @[TCU.scala 145:17]
  assign mem_io_portB_input_valid = hostRouter_io_mem_input_valid; // @[TCU.scala 146:15]
  assign mem_io_portB_input_bits_0 = hostRouter_io_mem_input_bits_0; // @[TCU.scala 146:15]
  assign mem_io_portB_input_bits_1 = hostRouter_io_mem_input_bits_1; // @[TCU.scala 146:15]
  assign mem_io_portB_input_bits_2 = hostRouter_io_mem_input_bits_2; // @[TCU.scala 146:15]
  assign mem_io_portB_input_bits_3 = hostRouter_io_mem_input_bits_3; // @[TCU.scala 146:15]
  assign mem_io_portB_input_bits_4 = hostRouter_io_mem_input_bits_4; // @[TCU.scala 146:15]
  assign mem_io_portB_input_bits_5 = hostRouter_io_mem_input_bits_5; // @[TCU.scala 146:15]
  assign mem_io_portB_input_bits_6 = hostRouter_io_mem_input_bits_6; // @[TCU.scala 146:15]
  assign mem_io_portB_input_bits_7 = hostRouter_io_mem_input_bits_7; // @[TCU.scala 146:15]
  assign mem_io_portB_output_ready = hostRouter_io_mem_output_ready; // @[TCU.scala 147:28]
  assign mem_io_tracepoint = decoder_io_tracepoint; // @[TCU.scala 119:21]
  assign mem_io_programCounter = decoder_io_programCounter; // @[TCU.scala 120:25]
  assign router_clock = clock;
  assign router_reset = reset;
  assign router_io_control_valid = decoder_io_dataflow_valid; // @[TCU.scala 130:21]
  assign router_io_control_bits_kind = decoder_io_dataflow_bits_kind; // @[TCU.scala 130:21]
  assign router_io_control_bits_size = decoder_io_dataflow_bits_size; // @[TCU.scala 130:21]
  assign router_io_mem_output_valid = mem_io_portA_output_valid; // @[TCU.scala 132:24]
  assign router_io_mem_output_bits_0 = mem_io_portA_output_bits_0; // @[TCU.scala 132:24]
  assign router_io_mem_output_bits_1 = mem_io_portA_output_bits_1; // @[TCU.scala 132:24]
  assign router_io_mem_output_bits_2 = mem_io_portA_output_bits_2; // @[TCU.scala 132:24]
  assign router_io_mem_output_bits_3 = mem_io_portA_output_bits_3; // @[TCU.scala 132:24]
  assign router_io_mem_output_bits_4 = mem_io_portA_output_bits_4; // @[TCU.scala 132:24]
  assign router_io_mem_output_bits_5 = mem_io_portA_output_bits_5; // @[TCU.scala 132:24]
  assign router_io_mem_output_bits_6 = mem_io_portA_output_bits_6; // @[TCU.scala 132:24]
  assign router_io_mem_output_bits_7 = mem_io_portA_output_bits_7; // @[TCU.scala 132:24]
  assign router_io_mem_input_ready = mem_io_portA_input_ready; // @[TCU.scala 133:15]
  assign router_io_array_input_ready = array_io_input_ready; // @[TCU.scala 135:18]
  assign router_io_array_output_valid = array_io_output_valid; // @[TCU.scala 136:26]
  assign router_io_array_output_bits_0 = array_io_output_bits_0; // @[TCU.scala 136:26]
  assign router_io_array_output_bits_1 = array_io_output_bits_1; // @[TCU.scala 136:26]
  assign router_io_array_output_bits_2 = array_io_output_bits_2; // @[TCU.scala 136:26]
  assign router_io_array_output_bits_3 = array_io_output_bits_3; // @[TCU.scala 136:26]
  assign router_io_array_output_bits_4 = array_io_output_bits_4; // @[TCU.scala 136:26]
  assign router_io_array_output_bits_5 = array_io_output_bits_5; // @[TCU.scala 136:26]
  assign router_io_array_output_bits_6 = array_io_output_bits_6; // @[TCU.scala 136:26]
  assign router_io_array_output_bits_7 = array_io_output_bits_7; // @[TCU.scala 136:26]
  assign router_io_array_weightInput_ready = array_io_weight_ready; // @[TCU.scala 137:19]
  assign router_io_acc_output_valid = acc_io_output_valid; // @[TCU.scala 140:24]
  assign router_io_acc_output_bits_0 = acc_io_output_bits_0; // @[TCU.scala 140:24]
  assign router_io_acc_output_bits_1 = acc_io_output_bits_1; // @[TCU.scala 140:24]
  assign router_io_acc_output_bits_2 = acc_io_output_bits_2; // @[TCU.scala 140:24]
  assign router_io_acc_output_bits_3 = acc_io_output_bits_3; // @[TCU.scala 140:24]
  assign router_io_acc_output_bits_4 = acc_io_output_bits_4; // @[TCU.scala 140:24]
  assign router_io_acc_output_bits_5 = acc_io_output_bits_5; // @[TCU.scala 140:24]
  assign router_io_acc_output_bits_6 = acc_io_output_bits_6; // @[TCU.scala 140:24]
  assign router_io_acc_output_bits_7 = acc_io_output_bits_7; // @[TCU.scala 140:24]
  assign router_io_acc_input_ready = acc_io_input_ready; // @[TCU.scala 139:16]
  assign router_io_timeout = decoder_io_timeout; // @[TCU.scala 126:21]
  assign router_io_tracepoint = decoder_io_tracepoint; // @[TCU.scala 127:24]
  assign router_io_programCounter = decoder_io_programCounter; // @[TCU.scala 128:28]
  assign hostRouter_io_control_valid = decoder_io_hostDataflow_valid; // @[TCU.scala 143:25]
  assign hostRouter_io_control_bits_kind = decoder_io_hostDataflow_bits_kind; // @[TCU.scala 143:25]
  assign hostRouter_io_dram0_dataIn_valid = io_dram0_dataIn_valid; // @[TCU.scala 152:30]
  assign hostRouter_io_dram0_dataIn_bits_0 = io_dram0_dataIn_bits_0; // @[TCU.scala 152:30]
  assign hostRouter_io_dram0_dataIn_bits_1 = io_dram0_dataIn_bits_1; // @[TCU.scala 152:30]
  assign hostRouter_io_dram0_dataIn_bits_2 = io_dram0_dataIn_bits_2; // @[TCU.scala 152:30]
  assign hostRouter_io_dram0_dataIn_bits_3 = io_dram0_dataIn_bits_3; // @[TCU.scala 152:30]
  assign hostRouter_io_dram0_dataIn_bits_4 = io_dram0_dataIn_bits_4; // @[TCU.scala 152:30]
  assign hostRouter_io_dram0_dataIn_bits_5 = io_dram0_dataIn_bits_5; // @[TCU.scala 152:30]
  assign hostRouter_io_dram0_dataIn_bits_6 = io_dram0_dataIn_bits_6; // @[TCU.scala 152:30]
  assign hostRouter_io_dram0_dataIn_bits_7 = io_dram0_dataIn_bits_7; // @[TCU.scala 152:30]
  assign hostRouter_io_dram0_dataOut_ready = io_dram0_dataOut_ready; // @[TCU.scala 153:20]
  assign hostRouter_io_dram1_dataIn_valid = io_dram1_dataIn_valid; // @[TCU.scala 155:30]
  assign hostRouter_io_dram1_dataIn_bits_0 = io_dram1_dataIn_bits_0; // @[TCU.scala 155:30]
  assign hostRouter_io_dram1_dataIn_bits_1 = io_dram1_dataIn_bits_1; // @[TCU.scala 155:30]
  assign hostRouter_io_dram1_dataIn_bits_2 = io_dram1_dataIn_bits_2; // @[TCU.scala 155:30]
  assign hostRouter_io_dram1_dataIn_bits_3 = io_dram1_dataIn_bits_3; // @[TCU.scala 155:30]
  assign hostRouter_io_dram1_dataIn_bits_4 = io_dram1_dataIn_bits_4; // @[TCU.scala 155:30]
  assign hostRouter_io_dram1_dataIn_bits_5 = io_dram1_dataIn_bits_5; // @[TCU.scala 155:30]
  assign hostRouter_io_dram1_dataIn_bits_6 = io_dram1_dataIn_bits_6; // @[TCU.scala 155:30]
  assign hostRouter_io_dram1_dataIn_bits_7 = io_dram1_dataIn_bits_7; // @[TCU.scala 155:30]
  assign hostRouter_io_dram1_dataOut_ready = io_dram1_dataOut_ready; // @[TCU.scala 156:20]
  assign hostRouter_io_mem_output_valid = mem_io_portB_output_valid; // @[TCU.scala 147:28]
  assign hostRouter_io_mem_output_bits_0 = mem_io_portB_output_bits_0; // @[TCU.scala 147:28]
  assign hostRouter_io_mem_output_bits_1 = mem_io_portB_output_bits_1; // @[TCU.scala 147:28]
  assign hostRouter_io_mem_output_bits_2 = mem_io_portB_output_bits_2; // @[TCU.scala 147:28]
  assign hostRouter_io_mem_output_bits_3 = mem_io_portB_output_bits_3; // @[TCU.scala 147:28]
  assign hostRouter_io_mem_output_bits_4 = mem_io_portB_output_bits_4; // @[TCU.scala 147:28]
  assign hostRouter_io_mem_output_bits_5 = mem_io_portB_output_bits_5; // @[TCU.scala 147:28]
  assign hostRouter_io_mem_output_bits_6 = mem_io_portB_output_bits_6; // @[TCU.scala 147:28]
  assign hostRouter_io_mem_output_bits_7 = mem_io_portB_output_bits_7; // @[TCU.scala 147:28]
  assign hostRouter_io_mem_input_ready = mem_io_portB_input_ready; // @[TCU.scala 146:15]
  assign acc_io_control_q_clock = clock;
  assign acc_io_control_q_reset = reset;
  assign acc_io_control_q_io_enq_valid = decoder_io_acc_valid; // @[TCU.scala 107:39]
  assign acc_io_control_q_io_enq_bits_instruction_op = decoder_io_acc_bits_instruction_op; // @[TCU.scala 107:39]
  assign acc_io_control_q_io_enq_bits_instruction_sourceLeft = decoder_io_acc_bits_instruction_sourceLeft; // @[TCU.scala 107:39]
  assign acc_io_control_q_io_enq_bits_instruction_sourceRight = decoder_io_acc_bits_instruction_sourceRight; // @[TCU.scala 107:39]
  assign acc_io_control_q_io_enq_bits_instruction_dest = decoder_io_acc_bits_instruction_dest; // @[TCU.scala 107:39]
  assign acc_io_control_q_io_enq_bits_readAddress = decoder_io_acc_bits_readAddress; // @[TCU.scala 107:39]
  assign acc_io_control_q_io_enq_bits_writeAddress = decoder_io_acc_bits_writeAddress; // @[TCU.scala 107:39]
  assign acc_io_control_q_io_enq_bits_accumulate = decoder_io_acc_bits_accumulate; // @[TCU.scala 107:39]
  assign acc_io_control_q_io_enq_bits_write = decoder_io_acc_bits_write; // @[TCU.scala 107:39]
  assign acc_io_control_q_io_enq_bits_read = decoder_io_acc_bits_read; // @[TCU.scala 107:39]
  assign acc_io_control_q_io_deq_ready = acc_io_control_ready; // @[TCU.scala 107:18]
  assign array_io_control_q_clock = clock;
  assign array_io_control_q_reset = reset;
  assign array_io_control_q_io_enq_valid = decoder_io_array_valid; // @[TCU.scala 114:41]
  assign array_io_control_q_io_enq_bits_load = decoder_io_array_bits_load; // @[TCU.scala 114:41]
  assign array_io_control_q_io_enq_bits_zeroes = decoder_io_array_bits_zeroes; // @[TCU.scala 114:41]
  assign array_io_control_q_io_deq_ready = array_io_control_ready; // @[TCU.scala 114:20]
endmodule
module Queue_28(
  input        clock,
  input        reset,
  output       io_enq_ready,
  input        io_enq_valid,
  input  [7:0] io_enq_bits,
  input        io_deq_ready,
  output       io_deq_valid,
  output [7:0] io_deq_bits
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
`endif // RANDOMIZE_REG_INIT
  reg [7:0] ram [0:1]; // @[Decoupled.scala 218:16]
  wire [7:0] ram_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [7:0] ram_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_MPORT_en; // @[Decoupled.scala 218:16]
  reg  enq_ptr_value; // @[Counter.scala 60:40]
  reg  deq_ptr_value; // @[Counter.scala 60:40]
  reg  maybe_full; // @[Decoupled.scala 221:27]
  wire  ptr_match = enq_ptr_value == deq_ptr_value; // @[Decoupled.scala 223:33]
  wire  empty = ptr_match & ~maybe_full; // @[Decoupled.scala 224:25]
  wire  full = ptr_match & maybe_full; // @[Decoupled.scala 225:24]
  wire  _do_enq_T = io_enq_ready & io_enq_valid; // @[Decoupled.scala 40:37]
  wire  _do_deq_T = io_deq_ready & io_deq_valid; // @[Decoupled.scala 40:37]
  wire  _GEN_9 = io_deq_ready ? 1'h0 : _do_enq_T; // @[Decoupled.scala 249:27 Decoupled.scala 249:36]
  wire  do_enq = empty ? _GEN_9 : _do_enq_T; // @[Decoupled.scala 246:18]
  wire  do_deq = empty ? 1'h0 : _do_deq_T; // @[Decoupled.scala 246:18 Decoupled.scala 248:14]
  assign ram_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_io_deq_bits_MPORT_data = ram[ram_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_MPORT_data = io_enq_bits;
  assign ram_MPORT_addr = enq_ptr_value;
  assign ram_MPORT_mask = 1'h1;
  assign ram_MPORT_en = empty ? _GEN_9 : _do_enq_T;
  assign io_enq_ready = io_deq_ready | ~full; // @[Decoupled.scala 254:25 Decoupled.scala 254:40 Decoupled.scala 241:16]
  assign io_deq_valid = io_enq_valid | ~empty; // @[Decoupled.scala 245:25 Decoupled.scala 245:40 Decoupled.scala 240:16]
  assign io_deq_bits = empty ? io_enq_bits : ram_io_deq_bits_MPORT_data; // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  always @(posedge clock) begin
    if(ram_MPORT_en & ram_MPORT_mask) begin
      ram[ram_MPORT_addr] <= ram_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if (reset) begin // @[Counter.scala 60:40]
      enq_ptr_value <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_enq) begin // @[Decoupled.scala 229:17]
      enq_ptr_value <= enq_ptr_value + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Counter.scala 60:40]
      deq_ptr_value <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_deq) begin // @[Decoupled.scala 233:17]
      deq_ptr_value <= deq_ptr_value + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Decoupled.scala 221:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 221:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 236:28]
      if (empty) begin // @[Decoupled.scala 246:18]
        if (io_deq_ready) begin // @[Decoupled.scala 249:27]
          maybe_full <= 1'h0; // @[Decoupled.scala 249:36]
        end else begin
          maybe_full <= _do_enq_T;
        end
      end else begin
        maybe_full <= _do_enq_T;
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
`ifdef RANDOMIZE_MEM_INIT
  _RAND_0 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram[initvar] = _RAND_0[7:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_1 = {1{`RANDOM}};
  enq_ptr_value = _RAND_1[0:0];
  _RAND_2 = {1{`RANDOM}};
  deq_ptr_value = _RAND_2[0:0];
  _RAND_3 = {1{`RANDOM}};
  maybe_full = _RAND_3[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Queue_30(
  input   clock,
  input   reset,
  output  io_enq_ready,
  input   io_enq_valid,
  input   io_enq_bits,
  input   io_deq_ready,
  output  io_deq_valid,
  output  io_deq_bits
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
`endif // RANDOMIZE_REG_INIT
  reg  ram [0:1]; // @[Decoupled.scala 218:16]
  wire  ram_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_MPORT_en; // @[Decoupled.scala 218:16]
  reg  enq_ptr_value; // @[Counter.scala 60:40]
  reg  deq_ptr_value; // @[Counter.scala 60:40]
  reg  maybe_full; // @[Decoupled.scala 221:27]
  wire  ptr_match = enq_ptr_value == deq_ptr_value; // @[Decoupled.scala 223:33]
  wire  empty = ptr_match & ~maybe_full; // @[Decoupled.scala 224:25]
  wire  full = ptr_match & maybe_full; // @[Decoupled.scala 225:24]
  wire  _do_enq_T = io_enq_ready & io_enq_valid; // @[Decoupled.scala 40:37]
  wire  _do_deq_T = io_deq_ready & io_deq_valid; // @[Decoupled.scala 40:37]
  wire  _GEN_9 = io_deq_ready ? 1'h0 : _do_enq_T; // @[Decoupled.scala 249:27 Decoupled.scala 249:36]
  wire  do_enq = empty ? _GEN_9 : _do_enq_T; // @[Decoupled.scala 246:18]
  wire  do_deq = empty ? 1'h0 : _do_deq_T; // @[Decoupled.scala 246:18 Decoupled.scala 248:14]
  assign ram_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_io_deq_bits_MPORT_data = ram[ram_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_MPORT_data = io_enq_bits;
  assign ram_MPORT_addr = enq_ptr_value;
  assign ram_MPORT_mask = 1'h1;
  assign ram_MPORT_en = empty ? _GEN_9 : _do_enq_T;
  assign io_enq_ready = io_deq_ready | ~full; // @[Decoupled.scala 254:25 Decoupled.scala 254:40 Decoupled.scala 241:16]
  assign io_deq_valid = io_enq_valid | ~empty; // @[Decoupled.scala 245:25 Decoupled.scala 245:40 Decoupled.scala 240:16]
  assign io_deq_bits = empty ? io_enq_bits : ram_io_deq_bits_MPORT_data; // @[Decoupled.scala 246:18 Decoupled.scala 247:19 Decoupled.scala 242:15]
  always @(posedge clock) begin
    if(ram_MPORT_en & ram_MPORT_mask) begin
      ram[ram_MPORT_addr] <= ram_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if (reset) begin // @[Counter.scala 60:40]
      enq_ptr_value <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_enq) begin // @[Decoupled.scala 229:17]
      enq_ptr_value <= enq_ptr_value + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Counter.scala 60:40]
      deq_ptr_value <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_deq) begin // @[Decoupled.scala 233:17]
      deq_ptr_value <= deq_ptr_value + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Decoupled.scala 221:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 221:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 236:28]
      if (empty) begin // @[Decoupled.scala 246:18]
        if (io_deq_ready) begin // @[Decoupled.scala 249:27]
          maybe_full <= 1'h0; // @[Decoupled.scala 249:36]
        end else begin
          maybe_full <= _do_enq_T;
        end
      end else begin
        maybe_full <= _do_enq_T;
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
`ifdef RANDOMIZE_MEM_INIT
  _RAND_0 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram[initvar] = _RAND_0[0:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_1 = {1{`RANDOM}};
  enq_ptr_value = _RAND_1[0:0];
  _RAND_2 = {1{`RANDOM}};
  deq_ptr_value = _RAND_2[0:0];
  _RAND_3 = {1{`RANDOM}};
  maybe_full = _RAND_3[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Counter_10(
  input        clock,
  input        reset,
  input        io_value_ready,
  output [7:0] io_value_bits,
  input        io_resetValue
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
`endif // RANDOMIZE_REG_INIT
  reg [7:0] value; // @[Counter.scala 16:22]
  wire [7:0] _value_T_1 = value + 8'h1; // @[Counter.scala 24:22]
  assign io_value_bits = value; // @[Counter.scala 18:17]
  always @(posedge clock) begin
    if (reset) begin // @[Counter.scala 16:22]
      value <= 8'h0; // @[Counter.scala 16:22]
    end else if (io_resetValue) begin // @[Counter.scala 27:23]
      value <= 8'h0; // @[Counter.scala 28:11]
    end else if (io_value_ready) begin // @[Counter.scala 20:24]
      if (value == 8'hff) begin // @[Counter.scala 21:31]
        value <= 8'h0; // @[Counter.scala 22:13]
      end else begin
        value <= _value_T_1; // @[Counter.scala 24:13]
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
  value = _RAND_0[7:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module BurstSplitter(
  input         clock,
  input         reset,
  output        io_control_ready,
  input         io_control_valid,
  input  [7:0]  io_control_bits,
  output        io_in_ready,
  input         io_in_valid,
  input  [63:0] io_in_bits_data,
  input         io_out_ready,
  output        io_out_valid,
  output [63:0] io_out_bits_data,
  output        io_out_bits_last
);
  wire  counter_clock; // @[Counter.scala 34:19]
  wire  counter_reset; // @[Counter.scala 34:19]
  wire  counter_io_value_ready; // @[Counter.scala 34:19]
  wire [7:0] counter_io_value_bits; // @[Counter.scala 34:19]
  wire  counter_io_resetValue; // @[Counter.scala 34:19]
  wire  _counter_io_resetValue_T = io_out_ready & io_out_valid; // @[Decoupled.scala 40:37]
  Counter_10 counter ( // @[Counter.scala 34:19]
    .clock(counter_clock),
    .reset(counter_reset),
    .io_value_ready(counter_io_value_ready),
    .io_value_bits(counter_io_value_bits),
    .io_resetValue(counter_io_resetValue)
  );
  assign io_control_ready = counter_io_value_bits == io_control_bits & _counter_io_resetValue_T; // @[MemBoundarySplitter.scala 45:51 MemBoundarySplitter.scala 48:22 MemBoundarySplitter.scala 52:22]
  assign io_in_ready = io_control_valid & io_out_ready; // @[MemBoundarySplitter.scala 41:35]
  assign io_out_valid = io_control_valid & io_in_valid; // @[MemBoundarySplitter.scala 40:36]
  assign io_out_bits_data = io_in_bits_data; // @[MemBoundarySplitter.scala 34:34]
  assign io_out_bits_last = counter_io_value_bits == io_control_bits; // @[MemBoundarySplitter.scala 45:30]
  assign counter_clock = clock;
  assign counter_reset = reset;
  assign counter_io_value_ready = counter_io_value_bits == io_control_bits ? 1'h0 : _counter_io_resetValue_T; // @[MemBoundarySplitter.scala 45:51 Counter.scala 36:22 MemBoundarySplitter.scala 51:28]
  assign counter_io_resetValue = counter_io_value_bits == io_control_bits & _counter_io_resetValue_T; // @[MemBoundarySplitter.scala 45:51 MemBoundarySplitter.scala 47:27 Counter.scala 35:21]
endmodule
module BurstSplitter_1(
  input         clock,
  input         reset,
  output        io_control_ready,
  input         io_control_valid,
  input  [7:0]  io_control_bits,
  output        io_in_ready,
  input         io_in_valid,
  input  [5:0]  io_in_bits_id,
  input  [63:0] io_in_bits_data,
  input  [7:0]  io_in_bits_strb,
  input         io_out_ready,
  output        io_out_valid,
  output [5:0]  io_out_bits_id,
  output [63:0] io_out_bits_data,
  output [7:0]  io_out_bits_strb,
  output        io_out_bits_last
);
  wire  counter_clock; // @[Counter.scala 34:19]
  wire  counter_reset; // @[Counter.scala 34:19]
  wire  counter_io_value_ready; // @[Counter.scala 34:19]
  wire [7:0] counter_io_value_bits; // @[Counter.scala 34:19]
  wire  counter_io_resetValue; // @[Counter.scala 34:19]
  wire  _counter_io_resetValue_T = io_out_ready & io_out_valid; // @[Decoupled.scala 40:37]
  Counter_10 counter ( // @[Counter.scala 34:19]
    .clock(counter_clock),
    .reset(counter_reset),
    .io_value_ready(counter_io_value_ready),
    .io_value_bits(counter_io_value_bits),
    .io_resetValue(counter_io_resetValue)
  );
  assign io_control_ready = counter_io_value_bits == io_control_bits & _counter_io_resetValue_T; // @[MemBoundarySplitter.scala 45:51 MemBoundarySplitter.scala 48:22 MemBoundarySplitter.scala 52:22]
  assign io_in_ready = io_control_valid & io_out_ready; // @[MemBoundarySplitter.scala 41:35]
  assign io_out_valid = io_control_valid & io_in_valid; // @[MemBoundarySplitter.scala 40:36]
  assign io_out_bits_id = io_in_bits_id; // @[MemBoundarySplitter.scala 34:34]
  assign io_out_bits_data = io_in_bits_data; // @[MemBoundarySplitter.scala 34:34]
  assign io_out_bits_strb = io_in_bits_strb; // @[MemBoundarySplitter.scala 34:34]
  assign io_out_bits_last = counter_io_value_bits == io_control_bits; // @[MemBoundarySplitter.scala 45:30]
  assign counter_clock = clock;
  assign counter_reset = reset;
  assign counter_io_value_ready = counter_io_value_bits == io_control_bits ? 1'h0 : _counter_io_resetValue_T; // @[MemBoundarySplitter.scala 45:51 Counter.scala 36:22 MemBoundarySplitter.scala 51:28]
  assign counter_io_resetValue = counter_io_value_bits == io_control_bits & _counter_io_resetValue_T; // @[MemBoundarySplitter.scala 45:51 MemBoundarySplitter.scala 47:27 Counter.scala 35:21]
endmodule
module Filter(
  output  io_control_ready,
  input   io_control_valid,
  input   io_control_bits,
  output  io_in_ready,
  input   io_in_valid,
  input   io_out_ready,
  output  io_out_valid
);
  assign io_control_ready = io_control_bits ? io_in_valid & io_out_ready : io_in_valid; // @[MemBoundarySplitter.scala 71:25 MemBoundarySplitter.scala 74:22 MemBoundarySplitter.scala 78:22]
  assign io_in_ready = io_control_bits ? io_control_valid & io_out_ready : io_control_valid; // @[MemBoundarySplitter.scala 71:25 MemBoundarySplitter.scala 73:17 MemBoundarySplitter.scala 77:17]
  assign io_out_valid = io_control_bits & (io_control_valid & io_in_valid); // @[MemBoundarySplitter.scala 71:25 MemBoundarySplitter.scala 72:18 MemBoundarySplitter.scala 76:18]
endmodule
module MemBoundarySplitter(
  input         clock,
  input         reset,
  output        io_in_writeAddress_ready,
  input         io_in_writeAddress_valid,
  input  [5:0]  io_in_writeAddress_bits_id,
  input  [31:0] io_in_writeAddress_bits_addr,
  input  [7:0]  io_in_writeAddress_bits_len,
  input  [2:0]  io_in_writeAddress_bits_size,
  input  [1:0]  io_in_writeAddress_bits_burst,
  input  [1:0]  io_in_writeAddress_bits_lock,
  input  [3:0]  io_in_writeAddress_bits_cache,
  input  [2:0]  io_in_writeAddress_bits_prot,
  input  [3:0]  io_in_writeAddress_bits_qos,
  output        io_in_writeData_ready,
  input         io_in_writeData_valid,
  input  [5:0]  io_in_writeData_bits_id,
  input  [63:0] io_in_writeData_bits_data,
  input  [7:0]  io_in_writeData_bits_strb,
  input         io_in_writeResponse_ready,
  output        io_in_writeResponse_valid,
  output        io_in_readAddress_ready,
  input         io_in_readAddress_valid,
  input  [5:0]  io_in_readAddress_bits_id,
  input  [31:0] io_in_readAddress_bits_addr,
  input  [7:0]  io_in_readAddress_bits_len,
  input  [2:0]  io_in_readAddress_bits_size,
  input  [1:0]  io_in_readAddress_bits_burst,
  input  [1:0]  io_in_readAddress_bits_lock,
  input  [3:0]  io_in_readAddress_bits_cache,
  input  [2:0]  io_in_readAddress_bits_prot,
  input  [3:0]  io_in_readAddress_bits_qos,
  input         io_in_readData_ready,
  output        io_in_readData_valid,
  output [63:0] io_in_readData_bits_data,
  output        io_in_readData_bits_last,
  input         io_out_writeAddress_ready,
  output        io_out_writeAddress_valid,
  output [5:0]  io_out_writeAddress_bits_id,
  output [31:0] io_out_writeAddress_bits_addr,
  output [7:0]  io_out_writeAddress_bits_len,
  output [2:0]  io_out_writeAddress_bits_size,
  output [1:0]  io_out_writeAddress_bits_burst,
  output [1:0]  io_out_writeAddress_bits_lock,
  output [3:0]  io_out_writeAddress_bits_cache,
  output [2:0]  io_out_writeAddress_bits_prot,
  output [3:0]  io_out_writeAddress_bits_qos,
  input         io_out_writeData_ready,
  output        io_out_writeData_valid,
  output [5:0]  io_out_writeData_bits_id,
  output [63:0] io_out_writeData_bits_data,
  output [7:0]  io_out_writeData_bits_strb,
  output        io_out_writeData_bits_last,
  output        io_out_writeResponse_ready,
  input         io_out_writeResponse_valid,
  input         io_out_readAddress_ready,
  output        io_out_readAddress_valid,
  output [5:0]  io_out_readAddress_bits_id,
  output [31:0] io_out_readAddress_bits_addr,
  output [7:0]  io_out_readAddress_bits_len,
  output [2:0]  io_out_readAddress_bits_size,
  output [1:0]  io_out_readAddress_bits_burst,
  output [1:0]  io_out_readAddress_bits_lock,
  output [3:0]  io_out_readAddress_bits_cache,
  output [2:0]  io_out_readAddress_bits_prot,
  output [3:0]  io_out_readAddress_bits_qos,
  output        io_out_readData_ready,
  input         io_out_readData_valid,
  input  [63:0] io_out_readData_bits_data
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
`endif // RANDOMIZE_REG_INIT
  wire  readDataQueue_clock; // @[MemBoundarySplitter.scala 111:29]
  wire  readDataQueue_reset; // @[MemBoundarySplitter.scala 111:29]
  wire  readDataQueue_io_enq_ready; // @[MemBoundarySplitter.scala 111:29]
  wire  readDataQueue_io_enq_valid; // @[MemBoundarySplitter.scala 111:29]
  wire [7:0] readDataQueue_io_enq_bits; // @[MemBoundarySplitter.scala 111:29]
  wire  readDataQueue_io_deq_ready; // @[MemBoundarySplitter.scala 111:29]
  wire  readDataQueue_io_deq_valid; // @[MemBoundarySplitter.scala 111:29]
  wire [7:0] readDataQueue_io_deq_bits; // @[MemBoundarySplitter.scala 111:29]
  wire  writeDataQueue_clock; // @[MemBoundarySplitter.scala 114:30]
  wire  writeDataQueue_reset; // @[MemBoundarySplitter.scala 114:30]
  wire  writeDataQueue_io_enq_ready; // @[MemBoundarySplitter.scala 114:30]
  wire  writeDataQueue_io_enq_valid; // @[MemBoundarySplitter.scala 114:30]
  wire [7:0] writeDataQueue_io_enq_bits; // @[MemBoundarySplitter.scala 114:30]
  wire  writeDataQueue_io_deq_ready; // @[MemBoundarySplitter.scala 114:30]
  wire  writeDataQueue_io_deq_valid; // @[MemBoundarySplitter.scala 114:30]
  wire [7:0] writeDataQueue_io_deq_bits; // @[MemBoundarySplitter.scala 114:30]
  wire  writeResponseQueue_clock; // @[MemBoundarySplitter.scala 117:34]
  wire  writeResponseQueue_reset; // @[MemBoundarySplitter.scala 117:34]
  wire  writeResponseQueue_io_enq_ready; // @[MemBoundarySplitter.scala 117:34]
  wire  writeResponseQueue_io_enq_valid; // @[MemBoundarySplitter.scala 117:34]
  wire  writeResponseQueue_io_enq_bits; // @[MemBoundarySplitter.scala 117:34]
  wire  writeResponseQueue_io_deq_ready; // @[MemBoundarySplitter.scala 117:34]
  wire  writeResponseQueue_io_deq_valid; // @[MemBoundarySplitter.scala 117:34]
  wire  writeResponseQueue_io_deq_bits; // @[MemBoundarySplitter.scala 117:34]
  wire  readMerger_clock; // @[MemBoundarySplitter.scala 121:26]
  wire  readMerger_reset; // @[MemBoundarySplitter.scala 121:26]
  wire  readMerger_io_control_ready; // @[MemBoundarySplitter.scala 121:26]
  wire  readMerger_io_control_valid; // @[MemBoundarySplitter.scala 121:26]
  wire [7:0] readMerger_io_control_bits; // @[MemBoundarySplitter.scala 121:26]
  wire  readMerger_io_in_ready; // @[MemBoundarySplitter.scala 121:26]
  wire  readMerger_io_in_valid; // @[MemBoundarySplitter.scala 121:26]
  wire [63:0] readMerger_io_in_bits_data; // @[MemBoundarySplitter.scala 121:26]
  wire  readMerger_io_out_ready; // @[MemBoundarySplitter.scala 121:26]
  wire  readMerger_io_out_valid; // @[MemBoundarySplitter.scala 121:26]
  wire [63:0] readMerger_io_out_bits_data; // @[MemBoundarySplitter.scala 121:26]
  wire  readMerger_io_out_bits_last; // @[MemBoundarySplitter.scala 121:26]
  wire  writeSplitter_clock; // @[MemBoundarySplitter.scala 125:29]
  wire  writeSplitter_reset; // @[MemBoundarySplitter.scala 125:29]
  wire  writeSplitter_io_control_ready; // @[MemBoundarySplitter.scala 125:29]
  wire  writeSplitter_io_control_valid; // @[MemBoundarySplitter.scala 125:29]
  wire [7:0] writeSplitter_io_control_bits; // @[MemBoundarySplitter.scala 125:29]
  wire  writeSplitter_io_in_ready; // @[MemBoundarySplitter.scala 125:29]
  wire  writeSplitter_io_in_valid; // @[MemBoundarySplitter.scala 125:29]
  wire [5:0] writeSplitter_io_in_bits_id; // @[MemBoundarySplitter.scala 125:29]
  wire [63:0] writeSplitter_io_in_bits_data; // @[MemBoundarySplitter.scala 125:29]
  wire [7:0] writeSplitter_io_in_bits_strb; // @[MemBoundarySplitter.scala 125:29]
  wire  writeSplitter_io_out_ready; // @[MemBoundarySplitter.scala 125:29]
  wire  writeSplitter_io_out_valid; // @[MemBoundarySplitter.scala 125:29]
  wire [5:0] writeSplitter_io_out_bits_id; // @[MemBoundarySplitter.scala 125:29]
  wire [63:0] writeSplitter_io_out_bits_data; // @[MemBoundarySplitter.scala 125:29]
  wire [7:0] writeSplitter_io_out_bits_strb; // @[MemBoundarySplitter.scala 125:29]
  wire  writeSplitter_io_out_bits_last; // @[MemBoundarySplitter.scala 125:29]
  wire  writeResponseFilter_io_control_ready; // @[MemBoundarySplitter.scala 129:35]
  wire  writeResponseFilter_io_control_valid; // @[MemBoundarySplitter.scala 129:35]
  wire  writeResponseFilter_io_control_bits; // @[MemBoundarySplitter.scala 129:35]
  wire  writeResponseFilter_io_in_ready; // @[MemBoundarySplitter.scala 129:35]
  wire  writeResponseFilter_io_in_valid; // @[MemBoundarySplitter.scala 129:35]
  wire  writeResponseFilter_io_out_ready; // @[MemBoundarySplitter.scala 129:35]
  wire  writeResponseFilter_io_out_valid; // @[MemBoundarySplitter.scala 129:35]
  wire  readEnqueuer_clock; // @[MultiEnqueue.scala 160:43]
  wire  readEnqueuer_reset; // @[MultiEnqueue.scala 160:43]
  wire  readEnqueuer_io_in_ready; // @[MultiEnqueue.scala 160:43]
  wire  readEnqueuer_io_in_valid; // @[MultiEnqueue.scala 160:43]
  wire  readEnqueuer_io_out_0_ready; // @[MultiEnqueue.scala 160:43]
  wire  readEnqueuer_io_out_0_valid; // @[MultiEnqueue.scala 160:43]
  wire  readEnqueuer_io_out_1_ready; // @[MultiEnqueue.scala 160:43]
  wire  readEnqueuer_io_out_1_valid; // @[MultiEnqueue.scala 160:43]
  wire  writeEnqueuer_clock; // @[MultiEnqueue.scala 160:43]
  wire  writeEnqueuer_reset; // @[MultiEnqueue.scala 160:43]
  wire  writeEnqueuer_io_in_ready; // @[MultiEnqueue.scala 160:43]
  wire  writeEnqueuer_io_in_valid; // @[MultiEnqueue.scala 160:43]
  wire  writeEnqueuer_io_out_0_ready; // @[MultiEnqueue.scala 160:43]
  wire  writeEnqueuer_io_out_0_valid; // @[MultiEnqueue.scala 160:43]
  wire  writeEnqueuer_io_out_1_ready; // @[MultiEnqueue.scala 160:43]
  wire  writeEnqueuer_io_out_1_valid; // @[MultiEnqueue.scala 160:43]
  wire  writeEnqueuer_io_out_2_ready; // @[MultiEnqueue.scala 160:43]
  wire  writeEnqueuer_io_out_2_valid; // @[MultiEnqueue.scala 160:43]
  reg [10:0] readAddressCounter; // @[MemBoundarySplitter.scala 137:35]
  reg [7:0] readLenCounter; // @[MemBoundarySplitter.scala 140:31]
  reg [10:0] writeAddressCounter; // @[MemBoundarySplitter.scala 141:36]
  reg [7:0] writeLenCounter; // @[MemBoundarySplitter.scala 144:32]
  wire [11:0] lengthBytes = io_in_readAddress_bits_len * 4'h8; // @[MemBoundarySplitter.scala 105:30]
  wire [31:0] _GEN_6 = io_in_readAddress_bits_addr % 32'h1000; // @[MemBoundarySplitter.scala 106:45]
  wire [12:0] _T_1 = _GEN_6[12:0]; // @[MemBoundarySplitter.scala 106:45]
  wire [12:0] _GEN_125 = {{1'd0}, lengthBytes}; // @[MemBoundarySplitter.scala 106:79]
  wire [12:0] _T_3 = 13'h1001 - _GEN_125; // @[MemBoundarySplitter.scala 106:79]
  wire  _T_4 = _T_1 > _T_3; // @[MemBoundarySplitter.scala 106:59]
  wire [31:0] _GEN_126 = {{21'd0}, readAddressCounter}; // @[MemBoundarySplitter.scala 151:44]
  wire [31:0] addr = io_in_readAddress_bits_addr + _GEN_126; // @[MemBoundarySplitter.scala 151:44]
  wire [31:0] _GEN_8 = addr % 32'h1000; // @[MemBoundarySplitter.scala 153:27]
  wire [12:0] _availableAddresses_T = _GEN_8[12:0]; // @[MemBoundarySplitter.scala 153:27]
  wire [12:0] availableAddresses = 13'h1000 - _availableAddresses_T; // @[MemBoundarySplitter.scala 153:19]
  wire [12:0] availableBeats = availableAddresses / 4'h8; // @[MemBoundarySplitter.scala 154:45]
  wire  _len_T = readLenCounter == 8'h0; // @[MemBoundarySplitter.scala 156:22]
  wire [12:0] _GEN_127 = {{5'd0}, readLenCounter}; // @[MemBoundarySplitter.scala 109:43]
  wire [12:0] _len_T_2 = availableBeats > _GEN_127 ? {{5'd0}, readLenCounter} : availableBeats; // @[MemBoundarySplitter.scala 109:40]
  wire [12:0] _len_T_3 = _len_T ? availableBeats : _len_T_2; // @[MemBoundarySplitter.scala 155:18]
  wire [12:0] len = _len_T_3 - 13'h1; // @[MemBoundarySplitter.scala 159:7]
  wire [12:0] _GEN_128 = {{5'd0}, io_in_readAddress_bits_len}; // @[MemBoundarySplitter.scala 182:54]
  wire [12:0] _readLenCounter_T_1 = _GEN_128 - len; // @[MemBoundarySplitter.scala 182:54]
  wire [12:0] _GEN_0 = io_in_readAddress_valid & readEnqueuer_io_in_ready ? availableAddresses : {{2'd0},
    readAddressCounter}; // @[MemBoundarySplitter.scala 180:46 MemBoundarySplitter.scala 181:28 MemBoundarySplitter.scala 137:35]
  wire [12:0] _GEN_1 = io_in_readAddress_valid & readEnqueuer_io_in_ready ? _readLenCounter_T_1 : {{5'd0},
    readLenCounter}; // @[MemBoundarySplitter.scala 180:46 MemBoundarySplitter.scala 182:24 MemBoundarySplitter.scala 140:31]
  wire  _T_9 = io_out_readAddress_ready & io_out_readAddress_valid; // @[Decoupled.scala 40:37]
  wire [10:0] _GEN_2 = _T_9 ? 11'h0 : readAddressCounter; // @[MemBoundarySplitter.scala 193:39 MemBoundarySplitter.scala 194:28 MemBoundarySplitter.scala 137:35]
  wire [7:0] _GEN_3 = _T_9 ? 8'h0 : readLenCounter; // @[MemBoundarySplitter.scala 193:39 MemBoundarySplitter.scala 195:24 MemBoundarySplitter.scala 140:31]
  wire [12:0] _GEN_130 = {{2'd0}, readAddressCounter}; // @[MemBoundarySplitter.scala 208:50]
  wire [12:0] _readAddressCounter_T_1 = _GEN_130 + availableAddresses; // @[MemBoundarySplitter.scala 208:50]
  wire [12:0] _readLenCounter_T_3 = len + 13'h1; // @[MemBoundarySplitter.scala 210:49]
  wire [12:0] _readLenCounter_T_5 = _GEN_127 - _readLenCounter_T_3; // @[MemBoundarySplitter.scala 210:42]
  wire [12:0] _GEN_4 = _T_9 ? _readAddressCounter_T_1 : {{2'd0}, readAddressCounter}; // @[MemBoundarySplitter.scala 206:39 MemBoundarySplitter.scala 208:28 MemBoundarySplitter.scala 137:35]
  wire [12:0] _GEN_5 = _T_9 ? _readLenCounter_T_5 : {{5'd0}, readLenCounter}; // @[MemBoundarySplitter.scala 206:39 MemBoundarySplitter.scala 210:24 MemBoundarySplitter.scala 140:31]
  wire  _GEN_7 = _GEN_127 <= availableBeats & io_out_readAddress_ready; // @[MemBoundarySplitter.scala 184:50 MemBoundarySplitter.scala 187:31 Decoupled.scala 72:20]
  wire [7:0] address_len = len[7:0]; // @[MemBoundarySplitter.scala 161:23 MemBoundarySplitter.scala 168:17]
  wire [12:0] _GEN_18 = _GEN_127 <= availableBeats ? {{2'd0}, _GEN_2} : _GEN_4; // @[MemBoundarySplitter.scala 184:50]
  wire [12:0] _GEN_19 = _GEN_127 <= availableBeats ? {{5'd0}, _GEN_3} : _GEN_5; // @[MemBoundarySplitter.scala 184:50]
  wire  _GEN_20 = _len_T & io_in_readAddress_valid; // @[MemBoundarySplitter.scala 170:34 MultiEnqueue.scala 84:17]
  wire  _GEN_21 = _len_T & io_out_readAddress_ready; // @[MemBoundarySplitter.scala 170:34 ReadyValid.scala 19:11]
  wire  ready_io_out_readAddress_w_valid = readEnqueuer_io_out_0_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_31 = _len_T ? ready_io_out_readAddress_w_valid : io_in_readAddress_valid; // @[MemBoundarySplitter.scala 170:34 MultiEnqueue.scala 85:10]
  wire  ready_readDataQueue_io_enq_w_ready = readDataQueue_io_enq_ready; // @[ReadyValid.scala 16:17 MultiEnqueue.scala 86:10]
  wire  _GEN_32 = _len_T & ready_readDataQueue_io_enq_w_ready; // @[MemBoundarySplitter.scala 170:34 ReadyValid.scala 19:11]
  wire  ready_readDataQueue_io_enq_w_valid = readEnqueuer_io_out_1_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_34 = _len_T & ready_readDataQueue_io_enq_w_valid; // @[MemBoundarySplitter.scala 170:34 MultiEnqueue.scala 86:10]
  wire  _GEN_35 = _len_T ? 1'h0 : _GEN_7; // @[MemBoundarySplitter.scala 170:34 Decoupled.scala 72:20]
  wire [12:0] _GEN_36 = _len_T ? _GEN_0 : _GEN_18; // @[MemBoundarySplitter.scala 170:34]
  wire [12:0] _GEN_37 = _len_T ? _GEN_1 : _GEN_19; // @[MemBoundarySplitter.scala 170:34]
  wire [12:0] _GEN_54 = _T_4 ? _GEN_36 : {{2'd0}, readAddressCounter}; // @[MemBoundarySplitter.scala 150:74 MemBoundarySplitter.scala 137:35]
  wire [12:0] _GEN_55 = _T_4 ? _GEN_37 : {{5'd0}, readLenCounter}; // @[MemBoundarySplitter.scala 150:74 MemBoundarySplitter.scala 140:31]
  wire [11:0] lengthBytes_1 = io_in_writeAddress_bits_len * 4'h8; // @[MemBoundarySplitter.scala 105:30]
  wire [31:0] _GEN_9 = io_in_writeAddress_bits_addr % 32'h1000; // @[MemBoundarySplitter.scala 106:45]
  wire [12:0] _T_12 = _GEN_9[12:0]; // @[MemBoundarySplitter.scala 106:45]
  wire [12:0] _GEN_132 = {{1'd0}, lengthBytes_1}; // @[MemBoundarySplitter.scala 106:79]
  wire [12:0] _T_14 = 13'h1001 - _GEN_132; // @[MemBoundarySplitter.scala 106:79]
  wire  _T_15 = _T_12 > _T_14; // @[MemBoundarySplitter.scala 106:59]
  wire [31:0] _GEN_133 = {{21'd0}, writeAddressCounter}; // @[MemBoundarySplitter.scala 225:45]
  wire [31:0] addr_1 = io_in_writeAddress_bits_addr + _GEN_133; // @[MemBoundarySplitter.scala 225:45]
  wire [31:0] _GEN_10 = addr_1 % 32'h1000; // @[MemBoundarySplitter.scala 227:27]
  wire [12:0] _availableAddresses_T_2 = _GEN_10[12:0]; // @[MemBoundarySplitter.scala 227:27]
  wire [12:0] availableAddresses_1 = 13'h1000 - _availableAddresses_T_2; // @[MemBoundarySplitter.scala 227:19]
  wire [12:0] availableBeats_1 = availableAddresses_1 / 4'h8; // @[MemBoundarySplitter.scala 228:45]
  wire  _len_T_5 = writeLenCounter == 8'h0; // @[MemBoundarySplitter.scala 230:23]
  wire [12:0] _GEN_134 = {{5'd0}, writeLenCounter}; // @[MemBoundarySplitter.scala 109:43]
  wire [12:0] _len_T_7 = availableBeats_1 > _GEN_134 ? {{5'd0}, writeLenCounter} : availableBeats_1; // @[MemBoundarySplitter.scala 109:40]
  wire [12:0] _len_T_8 = _len_T_5 ? availableBeats_1 : _len_T_7; // @[MemBoundarySplitter.scala 229:18]
  wire [12:0] len_1 = _len_T_8 - 13'h1; // @[MemBoundarySplitter.scala 233:7]
  wire  _T_18 = io_in_writeAddress_valid & writeEnqueuer_io_in_ready; // @[MemBoundarySplitter.scala 256:37]
  wire [12:0] _GEN_135 = {{5'd0}, io_in_writeAddress_bits_len}; // @[MemBoundarySplitter.scala 258:56]
  wire [12:0] _writeLenCounter_T_1 = _GEN_135 - len_1; // @[MemBoundarySplitter.scala 258:56]
  wire [12:0] _GEN_56 = io_in_writeAddress_valid & writeEnqueuer_io_in_ready ? availableAddresses_1 : {{2'd0},
    writeAddressCounter}; // @[MemBoundarySplitter.scala 256:47 MemBoundarySplitter.scala 257:29 MemBoundarySplitter.scala 141:36]
  wire [12:0] _GEN_57 = io_in_writeAddress_valid & writeEnqueuer_io_in_ready ? _writeLenCounter_T_1 : {{5'd0},
    writeLenCounter}; // @[MemBoundarySplitter.scala 256:47 MemBoundarySplitter.scala 258:25 MemBoundarySplitter.scala 144:32]
  wire  _T_19 = _GEN_134 <= availableBeats_1; // @[MemBoundarySplitter.scala 260:32]
  wire [10:0] _GEN_58 = _T_18 ? 11'h0 : writeAddressCounter; // @[MemBoundarySplitter.scala 272:47 MemBoundarySplitter.scala 273:29 MemBoundarySplitter.scala 141:36]
  wire [7:0] _GEN_59 = _T_18 ? 8'h0 : writeLenCounter; // @[MemBoundarySplitter.scala 272:47 MemBoundarySplitter.scala 274:25 MemBoundarySplitter.scala 144:32]
  wire [12:0] _GEN_137 = {{2'd0}, writeAddressCounter}; // @[MemBoundarySplitter.scala 290:52]
  wire [12:0] _writeAddressCounter_T_1 = _GEN_137 + availableAddresses_1; // @[MemBoundarySplitter.scala 290:52]
  wire [12:0] _writeLenCounter_T_3 = len_1 + 13'h1; // @[MemBoundarySplitter.scala 292:51]
  wire [12:0] _writeLenCounter_T_5 = _GEN_134 - _writeLenCounter_T_3; // @[MemBoundarySplitter.scala 292:44]
  wire [12:0] _GEN_60 = _T_18 ? _writeAddressCounter_T_1 : {{2'd0}, writeAddressCounter}; // @[MemBoundarySplitter.scala 288:47 MemBoundarySplitter.scala 290:29 MemBoundarySplitter.scala 141:36]
  wire [12:0] _GEN_61 = _T_18 ? _writeLenCounter_T_5 : {{5'd0}, writeLenCounter}; // @[MemBoundarySplitter.scala 288:47 MemBoundarySplitter.scala 292:25 MemBoundarySplitter.scala 144:32]
  wire [7:0] address_1_len = len_1[7:0]; // @[MemBoundarySplitter.scala 235:23 MemBoundarySplitter.scala 242:17]
  wire  ready_io_out_writeAddress_w_1_valid = writeEnqueuer_io_out_0_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_73 = _GEN_134 <= availableBeats_1 ? ready_io_out_writeAddress_w_1_valid :
    ready_io_out_writeAddress_w_1_valid; // @[MemBoundarySplitter.scala 260:51 MultiEnqueue.scala 115:10 MultiEnqueue.scala 115:10]
  wire  ready_writeDataQueue_io_enq_w_1_ready = writeDataQueue_io_enq_ready; // @[ReadyValid.scala 16:17 MultiEnqueue.scala 116:10]
  wire  _GEN_74 = _GEN_134 <= availableBeats_1 ? ready_writeDataQueue_io_enq_w_1_ready :
    ready_writeDataQueue_io_enq_w_1_ready; // @[MemBoundarySplitter.scala 260:51 ReadyValid.scala 19:11 ReadyValid.scala 19:11]
  wire  ready_writeDataQueue_io_enq_w_1_valid = writeEnqueuer_io_out_1_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_76 = _GEN_134 <= availableBeats_1 ? ready_writeDataQueue_io_enq_w_1_valid :
    ready_writeDataQueue_io_enq_w_1_valid; // @[MemBoundarySplitter.scala 260:51 MultiEnqueue.scala 116:10 MultiEnqueue.scala 116:10]
  wire  ready_writeResponseQueue_io_enq_w_1_ready = writeResponseQueue_io_enq_ready; // @[ReadyValid.scala 16:17 MultiEnqueue.scala 117:10]
  wire  _GEN_77 = _GEN_134 <= availableBeats_1 ? ready_writeResponseQueue_io_enq_w_1_ready :
    ready_writeResponseQueue_io_enq_w_1_ready; // @[MemBoundarySplitter.scala 260:51 ReadyValid.scala 19:11 ReadyValid.scala 19:11]
  wire  ready_writeResponseQueue_io_enq_w_1_valid = writeEnqueuer_io_out_2_valid; // @[ReadyValid.scala 16:17 ReadyValid.scala 18:13]
  wire  _GEN_79 = _GEN_134 <= availableBeats_1 ? ready_writeResponseQueue_io_enq_w_1_valid :
    ready_writeResponseQueue_io_enq_w_1_valid; // @[MemBoundarySplitter.scala 260:51 MultiEnqueue.scala 117:10 MultiEnqueue.scala 117:10]
  wire  _GEN_80 = _GEN_134 <= availableBeats_1 & writeEnqueuer_io_in_ready; // @[MemBoundarySplitter.scala 260:51 MemBoundarySplitter.scala 271:32 Decoupled.scala 72:20]
  wire [12:0] _GEN_81 = _GEN_134 <= availableBeats_1 ? {{2'd0}, _GEN_58} : _GEN_60; // @[MemBoundarySplitter.scala 260:51]
  wire [12:0] _GEN_82 = _GEN_134 <= availableBeats_1 ? {{5'd0}, _GEN_59} : _GEN_61; // @[MemBoundarySplitter.scala 260:51]
  wire  _GEN_94 = _len_T_5 ? ready_io_out_writeAddress_w_1_valid : _GEN_73; // @[MemBoundarySplitter.scala 244:35 MultiEnqueue.scala 115:10]
  wire  _GEN_95 = _len_T_5 ? ready_writeDataQueue_io_enq_w_1_ready : _GEN_74; // @[MemBoundarySplitter.scala 244:35 ReadyValid.scala 19:11]
  wire  _GEN_97 = _len_T_5 ? ready_writeDataQueue_io_enq_w_1_valid : _GEN_76; // @[MemBoundarySplitter.scala 244:35 MultiEnqueue.scala 116:10]
  wire  _GEN_98 = _len_T_5 ? ready_writeResponseQueue_io_enq_w_1_ready : _GEN_77; // @[MemBoundarySplitter.scala 244:35 ReadyValid.scala 19:11]
  wire  _GEN_99 = _len_T_5 ? 1'h0 : _T_19; // @[MemBoundarySplitter.scala 244:35 MultiEnqueue.scala 117:10]
  wire  _GEN_100 = _len_T_5 ? ready_writeResponseQueue_io_enq_w_1_valid : _GEN_79; // @[MemBoundarySplitter.scala 244:35 MultiEnqueue.scala 117:10]
  wire  _GEN_101 = _len_T_5 ? 1'h0 : _GEN_80; // @[MemBoundarySplitter.scala 244:35 Decoupled.scala 72:20]
  wire [12:0] _GEN_102 = _len_T_5 ? _GEN_56 : _GEN_81; // @[MemBoundarySplitter.scala 244:35]
  wire [12:0] _GEN_103 = _len_T_5 ? _GEN_57 : _GEN_82; // @[MemBoundarySplitter.scala 244:35]
  wire [12:0] _GEN_123 = _T_15 ? _GEN_102 : {{2'd0}, writeAddressCounter}; // @[MemBoundarySplitter.scala 224:76 MemBoundarySplitter.scala 141:36]
  wire [12:0] _GEN_124 = _T_15 ? _GEN_103 : {{5'd0}, writeLenCounter}; // @[MemBoundarySplitter.scala 224:76 MemBoundarySplitter.scala 144:32]
  Queue_28 readDataQueue ( // @[MemBoundarySplitter.scala 111:29]
    .clock(readDataQueue_clock),
    .reset(readDataQueue_reset),
    .io_enq_ready(readDataQueue_io_enq_ready),
    .io_enq_valid(readDataQueue_io_enq_valid),
    .io_enq_bits(readDataQueue_io_enq_bits),
    .io_deq_ready(readDataQueue_io_deq_ready),
    .io_deq_valid(readDataQueue_io_deq_valid),
    .io_deq_bits(readDataQueue_io_deq_bits)
  );
  Queue_28 writeDataQueue ( // @[MemBoundarySplitter.scala 114:30]
    .clock(writeDataQueue_clock),
    .reset(writeDataQueue_reset),
    .io_enq_ready(writeDataQueue_io_enq_ready),
    .io_enq_valid(writeDataQueue_io_enq_valid),
    .io_enq_bits(writeDataQueue_io_enq_bits),
    .io_deq_ready(writeDataQueue_io_deq_ready),
    .io_deq_valid(writeDataQueue_io_deq_valid),
    .io_deq_bits(writeDataQueue_io_deq_bits)
  );
  Queue_30 writeResponseQueue ( // @[MemBoundarySplitter.scala 117:34]
    .clock(writeResponseQueue_clock),
    .reset(writeResponseQueue_reset),
    .io_enq_ready(writeResponseQueue_io_enq_ready),
    .io_enq_valid(writeResponseQueue_io_enq_valid),
    .io_enq_bits(writeResponseQueue_io_enq_bits),
    .io_deq_ready(writeResponseQueue_io_deq_ready),
    .io_deq_valid(writeResponseQueue_io_deq_valid),
    .io_deq_bits(writeResponseQueue_io_deq_bits)
  );
  BurstSplitter readMerger ( // @[MemBoundarySplitter.scala 121:26]
    .clock(readMerger_clock),
    .reset(readMerger_reset),
    .io_control_ready(readMerger_io_control_ready),
    .io_control_valid(readMerger_io_control_valid),
    .io_control_bits(readMerger_io_control_bits),
    .io_in_ready(readMerger_io_in_ready),
    .io_in_valid(readMerger_io_in_valid),
    .io_in_bits_data(readMerger_io_in_bits_data),
    .io_out_ready(readMerger_io_out_ready),
    .io_out_valid(readMerger_io_out_valid),
    .io_out_bits_data(readMerger_io_out_bits_data),
    .io_out_bits_last(readMerger_io_out_bits_last)
  );
  BurstSplitter_1 writeSplitter ( // @[MemBoundarySplitter.scala 125:29]
    .clock(writeSplitter_clock),
    .reset(writeSplitter_reset),
    .io_control_ready(writeSplitter_io_control_ready),
    .io_control_valid(writeSplitter_io_control_valid),
    .io_control_bits(writeSplitter_io_control_bits),
    .io_in_ready(writeSplitter_io_in_ready),
    .io_in_valid(writeSplitter_io_in_valid),
    .io_in_bits_id(writeSplitter_io_in_bits_id),
    .io_in_bits_data(writeSplitter_io_in_bits_data),
    .io_in_bits_strb(writeSplitter_io_in_bits_strb),
    .io_out_ready(writeSplitter_io_out_ready),
    .io_out_valid(writeSplitter_io_out_valid),
    .io_out_bits_id(writeSplitter_io_out_bits_id),
    .io_out_bits_data(writeSplitter_io_out_bits_data),
    .io_out_bits_strb(writeSplitter_io_out_bits_strb),
    .io_out_bits_last(writeSplitter_io_out_bits_last)
  );
  Filter writeResponseFilter ( // @[MemBoundarySplitter.scala 129:35]
    .io_control_ready(writeResponseFilter_io_control_ready),
    .io_control_valid(writeResponseFilter_io_control_valid),
    .io_control_bits(writeResponseFilter_io_control_bits),
    .io_in_ready(writeResponseFilter_io_in_ready),
    .io_in_valid(writeResponseFilter_io_in_valid),
    .io_out_ready(writeResponseFilter_io_out_ready),
    .io_out_valid(writeResponseFilter_io_out_valid)
  );
  MultiEnqueue_1 readEnqueuer ( // @[MultiEnqueue.scala 160:43]
    .clock(readEnqueuer_clock),
    .reset(readEnqueuer_reset),
    .io_in_ready(readEnqueuer_io_in_ready),
    .io_in_valid(readEnqueuer_io_in_valid),
    .io_out_0_ready(readEnqueuer_io_out_0_ready),
    .io_out_0_valid(readEnqueuer_io_out_0_valid),
    .io_out_1_ready(readEnqueuer_io_out_1_ready),
    .io_out_1_valid(readEnqueuer_io_out_1_valid)
  );
  MultiEnqueue_2 writeEnqueuer ( // @[MultiEnqueue.scala 160:43]
    .clock(writeEnqueuer_clock),
    .reset(writeEnqueuer_reset),
    .io_in_ready(writeEnqueuer_io_in_ready),
    .io_in_valid(writeEnqueuer_io_in_valid),
    .io_out_0_ready(writeEnqueuer_io_out_0_ready),
    .io_out_0_valid(writeEnqueuer_io_out_0_valid),
    .io_out_1_ready(writeEnqueuer_io_out_1_ready),
    .io_out_1_valid(writeEnqueuer_io_out_1_valid),
    .io_out_2_ready(writeEnqueuer_io_out_2_ready),
    .io_out_2_valid(writeEnqueuer_io_out_2_valid)
  );
  assign io_in_writeAddress_ready = _T_15 ? _GEN_101 : writeEnqueuer_io_in_ready; // @[MemBoundarySplitter.scala 224:76 MemBoundarySplitter.scala 296:30]
  assign io_in_writeData_ready = writeSplitter_io_in_ready; // @[MemBoundarySplitter.scala 127:23]
  assign io_in_writeResponse_valid = writeResponseFilter_io_out_valid; // @[MemBoundarySplitter.scala 132:23]
  assign io_in_readAddress_ready = _T_4 ? _GEN_35 : readEnqueuer_io_in_ready; // @[MemBoundarySplitter.scala 150:74 MemBoundarySplitter.scala 214:29]
  assign io_in_readData_valid = readMerger_io_out_valid; // @[MemBoundarySplitter.scala 124:18]
  assign io_in_readData_bits_data = readMerger_io_out_bits_data; // @[MemBoundarySplitter.scala 124:18]
  assign io_in_readData_bits_last = readMerger_io_out_bits_last; // @[MemBoundarySplitter.scala 124:18]
  assign io_out_writeAddress_valid = _T_15 ? _GEN_94 : ready_io_out_writeAddress_w_1_valid; // @[MemBoundarySplitter.scala 224:76 MultiEnqueue.scala 115:10]
  assign io_out_writeAddress_bits_id = io_in_writeAddress_bits_id; // @[MemBoundarySplitter.scala 224:76 MultiEnqueue.scala 115:10]
  assign io_out_writeAddress_bits_addr = _T_15 ? addr_1 : io_in_writeAddress_bits_addr; // @[MemBoundarySplitter.scala 224:76 MultiEnqueue.scala 115:10]
  assign io_out_writeAddress_bits_len = _T_15 ? address_1_len : io_in_writeAddress_bits_len; // @[MemBoundarySplitter.scala 224:76 MultiEnqueue.scala 115:10]
  assign io_out_writeAddress_bits_size = io_in_writeAddress_bits_size; // @[MemBoundarySplitter.scala 224:76 MultiEnqueue.scala 115:10]
  assign io_out_writeAddress_bits_burst = io_in_writeAddress_bits_burst; // @[MemBoundarySplitter.scala 224:76 MultiEnqueue.scala 115:10]
  assign io_out_writeAddress_bits_lock = io_in_writeAddress_bits_lock; // @[MemBoundarySplitter.scala 224:76 MultiEnqueue.scala 115:10]
  assign io_out_writeAddress_bits_cache = io_in_writeAddress_bits_cache; // @[MemBoundarySplitter.scala 224:76 MultiEnqueue.scala 115:10]
  assign io_out_writeAddress_bits_prot = io_in_writeAddress_bits_prot; // @[MemBoundarySplitter.scala 224:76 MultiEnqueue.scala 115:10]
  assign io_out_writeAddress_bits_qos = io_in_writeAddress_bits_qos; // @[MemBoundarySplitter.scala 224:76 MultiEnqueue.scala 115:10]
  assign io_out_writeData_valid = writeSplitter_io_out_valid; // @[MemBoundarySplitter.scala 128:20]
  assign io_out_writeData_bits_id = writeSplitter_io_out_bits_id; // @[MemBoundarySplitter.scala 128:20]
  assign io_out_writeData_bits_data = writeSplitter_io_out_bits_data; // @[MemBoundarySplitter.scala 128:20]
  assign io_out_writeData_bits_strb = writeSplitter_io_out_bits_strb; // @[MemBoundarySplitter.scala 128:20]
  assign io_out_writeData_bits_last = writeSplitter_io_out_bits_last; // @[MemBoundarySplitter.scala 128:20]
  assign io_out_writeResponse_ready = writeResponseFilter_io_in_ready; // @[MemBoundarySplitter.scala 131:29]
  assign io_out_readAddress_valid = _T_4 ? _GEN_31 : ready_io_out_readAddress_w_valid; // @[MemBoundarySplitter.scala 150:74 MultiEnqueue.scala 85:10]
  assign io_out_readAddress_bits_id = io_in_readAddress_bits_id; // @[MemBoundarySplitter.scala 150:74 MultiEnqueue.scala 85:10]
  assign io_out_readAddress_bits_addr = _T_4 ? addr : io_in_readAddress_bits_addr; // @[MemBoundarySplitter.scala 150:74 MultiEnqueue.scala 85:10]
  assign io_out_readAddress_bits_len = _T_4 ? address_len : io_in_readAddress_bits_len; // @[MemBoundarySplitter.scala 150:74 MultiEnqueue.scala 85:10]
  assign io_out_readAddress_bits_size = io_in_readAddress_bits_size; // @[MemBoundarySplitter.scala 150:74 MultiEnqueue.scala 85:10]
  assign io_out_readAddress_bits_burst = io_in_readAddress_bits_burst; // @[MemBoundarySplitter.scala 150:74 MultiEnqueue.scala 85:10]
  assign io_out_readAddress_bits_lock = io_in_readAddress_bits_lock; // @[MemBoundarySplitter.scala 150:74 MultiEnqueue.scala 85:10]
  assign io_out_readAddress_bits_cache = io_in_readAddress_bits_cache; // @[MemBoundarySplitter.scala 150:74 MultiEnqueue.scala 85:10]
  assign io_out_readAddress_bits_prot = io_in_readAddress_bits_prot; // @[MemBoundarySplitter.scala 150:74 MultiEnqueue.scala 85:10]
  assign io_out_readAddress_bits_qos = io_in_readAddress_bits_qos; // @[MemBoundarySplitter.scala 150:74 MultiEnqueue.scala 85:10]
  assign io_out_readData_ready = readMerger_io_in_ready; // @[MemBoundarySplitter.scala 123:20]
  assign readDataQueue_clock = clock;
  assign readDataQueue_reset = reset;
  assign readDataQueue_io_enq_valid = _T_4 ? _GEN_34 : ready_readDataQueue_io_enq_w_valid; // @[MemBoundarySplitter.scala 150:74 MultiEnqueue.scala 86:10]
  assign readDataQueue_io_enq_bits = io_in_readAddress_bits_len; // @[MemBoundarySplitter.scala 150:74 MultiEnqueue.scala 86:10]
  assign readDataQueue_io_deq_ready = readMerger_io_control_ready; // @[MemBoundarySplitter.scala 122:25]
  assign writeDataQueue_clock = clock;
  assign writeDataQueue_reset = reset;
  assign writeDataQueue_io_enq_valid = _T_15 ? _GEN_97 : ready_writeDataQueue_io_enq_w_1_valid; // @[MemBoundarySplitter.scala 224:76 MultiEnqueue.scala 116:10]
  assign writeDataQueue_io_enq_bits = _T_15 ? address_1_len : io_in_writeAddress_bits_len; // @[MemBoundarySplitter.scala 224:76 MultiEnqueue.scala 116:10]
  assign writeDataQueue_io_deq_ready = writeSplitter_io_control_ready; // @[MemBoundarySplitter.scala 126:28]
  assign writeResponseQueue_clock = clock;
  assign writeResponseQueue_reset = reset;
  assign writeResponseQueue_io_enq_valid = _T_15 ? _GEN_100 : ready_writeResponseQueue_io_enq_w_1_valid; // @[MemBoundarySplitter.scala 224:76 MultiEnqueue.scala 117:10]
  assign writeResponseQueue_io_enq_bits = _T_15 ? _GEN_99 : 1'h1; // @[MemBoundarySplitter.scala 224:76 MultiEnqueue.scala 117:10]
  assign writeResponseQueue_io_deq_ready = writeResponseFilter_io_control_ready; // @[MemBoundarySplitter.scala 130:34]
  assign readMerger_clock = clock;
  assign readMerger_reset = reset;
  assign readMerger_io_control_valid = readDataQueue_io_deq_valid; // @[MemBoundarySplitter.scala 122:25]
  assign readMerger_io_control_bits = readDataQueue_io_deq_bits; // @[MemBoundarySplitter.scala 122:25]
  assign readMerger_io_in_valid = io_out_readData_valid; // @[MemBoundarySplitter.scala 123:20]
  assign readMerger_io_in_bits_data = io_out_readData_bits_data; // @[MemBoundarySplitter.scala 123:20]
  assign readMerger_io_out_ready = io_in_readData_ready; // @[MemBoundarySplitter.scala 124:18]
  assign writeSplitter_clock = clock;
  assign writeSplitter_reset = reset;
  assign writeSplitter_io_control_valid = writeDataQueue_io_deq_valid; // @[MemBoundarySplitter.scala 126:28]
  assign writeSplitter_io_control_bits = writeDataQueue_io_deq_bits; // @[MemBoundarySplitter.scala 126:28]
  assign writeSplitter_io_in_valid = io_in_writeData_valid; // @[MemBoundarySplitter.scala 127:23]
  assign writeSplitter_io_in_bits_id = io_in_writeData_bits_id; // @[MemBoundarySplitter.scala 127:23]
  assign writeSplitter_io_in_bits_data = io_in_writeData_bits_data; // @[MemBoundarySplitter.scala 127:23]
  assign writeSplitter_io_in_bits_strb = io_in_writeData_bits_strb; // @[MemBoundarySplitter.scala 127:23]
  assign writeSplitter_io_out_ready = io_out_writeData_ready; // @[MemBoundarySplitter.scala 128:20]
  assign writeResponseFilter_io_control_valid = writeResponseQueue_io_deq_valid; // @[MemBoundarySplitter.scala 130:34]
  assign writeResponseFilter_io_control_bits = writeResponseQueue_io_deq_bits; // @[MemBoundarySplitter.scala 130:34]
  assign writeResponseFilter_io_in_valid = io_out_writeResponse_valid; // @[MemBoundarySplitter.scala 131:29]
  assign writeResponseFilter_io_out_ready = io_in_writeResponse_ready; // @[MemBoundarySplitter.scala 132:23]
  assign readEnqueuer_clock = clock;
  assign readEnqueuer_reset = reset;
  assign readEnqueuer_io_in_valid = _T_4 ? _GEN_20 : io_in_readAddress_valid; // @[MemBoundarySplitter.scala 150:74 MultiEnqueue.scala 84:17]
  assign readEnqueuer_io_out_0_ready = _T_4 ? _GEN_21 : io_out_readAddress_ready; // @[MemBoundarySplitter.scala 150:74 ReadyValid.scala 19:11]
  assign readEnqueuer_io_out_1_ready = _T_4 ? _GEN_32 : ready_readDataQueue_io_enq_w_ready; // @[MemBoundarySplitter.scala 150:74 ReadyValid.scala 19:11]
  assign writeEnqueuer_clock = clock;
  assign writeEnqueuer_reset = reset;
  assign writeEnqueuer_io_in_valid = io_in_writeAddress_valid; // @[MemBoundarySplitter.scala 224:76 MultiEnqueue.scala 114:17]
  assign writeEnqueuer_io_out_0_ready = io_out_writeAddress_ready; // @[MemBoundarySplitter.scala 224:76 ReadyValid.scala 19:11]
  assign writeEnqueuer_io_out_1_ready = _T_15 ? _GEN_95 : ready_writeDataQueue_io_enq_w_1_ready; // @[MemBoundarySplitter.scala 224:76 ReadyValid.scala 19:11]
  assign writeEnqueuer_io_out_2_ready = _T_15 ? _GEN_98 : ready_writeResponseQueue_io_enq_w_1_ready; // @[MemBoundarySplitter.scala 224:76 ReadyValid.scala 19:11]
  always @(posedge clock) begin
    if (reset) begin // @[MemBoundarySplitter.scala 137:35]
      readAddressCounter <= 11'h0; // @[MemBoundarySplitter.scala 137:35]
    end else begin
      readAddressCounter <= _GEN_54[10:0];
    end
    if (reset) begin // @[MemBoundarySplitter.scala 140:31]
      readLenCounter <= 8'h0; // @[MemBoundarySplitter.scala 140:31]
    end else begin
      readLenCounter <= _GEN_55[7:0];
    end
    if (reset) begin // @[MemBoundarySplitter.scala 141:36]
      writeAddressCounter <= 11'h0; // @[MemBoundarySplitter.scala 141:36]
    end else begin
      writeAddressCounter <= _GEN_123[10:0];
    end
    if (reset) begin // @[MemBoundarySplitter.scala 144:32]
      writeLenCounter <= 8'h0; // @[MemBoundarySplitter.scala 144:32]
    end else begin
      writeLenCounter <= _GEN_124[7:0];
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
  readAddressCounter = _RAND_0[10:0];
  _RAND_1 = {1{`RANDOM}};
  readLenCounter = _RAND_1[7:0];
  _RAND_2 = {1{`RANDOM}};
  writeAddressCounter = _RAND_2[10:0];
  _RAND_3 = {1{`RANDOM}};
  writeLenCounter = _RAND_3[7:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Queue_34(
  input         clock,
  input         reset,
  output        io_enq_ready,
  input         io_enq_valid,
  input         io_enq_bits_write,
  input  [19:0] io_enq_bits_address,
  input  [19:0] io_enq_bits_size,
  input         io_deq_ready,
  output        io_deq_valid,
  output        io_deq_bits_write,
  output [19:0] io_deq_bits_address,
  output [19:0] io_deq_bits_size
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
`endif // RANDOMIZE_REG_INIT
  reg  ram_write [0:1]; // @[Decoupled.scala 218:16]
  wire  ram_write_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_write_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_write_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_write_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_write_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_write_MPORT_en; // @[Decoupled.scala 218:16]
  reg [19:0] ram_address [0:1]; // @[Decoupled.scala 218:16]
  wire [19:0] ram_address_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_address_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [19:0] ram_address_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_address_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_address_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_address_MPORT_en; // @[Decoupled.scala 218:16]
  reg [19:0] ram_size [0:1]; // @[Decoupled.scala 218:16]
  wire [19:0] ram_size_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_size_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [19:0] ram_size_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_size_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_size_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_size_MPORT_en; // @[Decoupled.scala 218:16]
  reg  enq_ptr_value; // @[Counter.scala 60:40]
  reg  deq_ptr_value; // @[Counter.scala 60:40]
  reg  maybe_full; // @[Decoupled.scala 221:27]
  wire  ptr_match = enq_ptr_value == deq_ptr_value; // @[Decoupled.scala 223:33]
  wire  empty = ptr_match & ~maybe_full; // @[Decoupled.scala 224:25]
  wire  full = ptr_match & maybe_full; // @[Decoupled.scala 225:24]
  wire  do_enq = io_enq_ready & io_enq_valid; // @[Decoupled.scala 40:37]
  wire  do_deq = io_deq_ready & io_deq_valid; // @[Decoupled.scala 40:37]
  assign ram_write_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_write_io_deq_bits_MPORT_data = ram_write[ram_write_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_write_MPORT_data = io_enq_bits_write;
  assign ram_write_MPORT_addr = enq_ptr_value;
  assign ram_write_MPORT_mask = 1'h1;
  assign ram_write_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_address_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_address_io_deq_bits_MPORT_data = ram_address[ram_address_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_address_MPORT_data = io_enq_bits_address;
  assign ram_address_MPORT_addr = enq_ptr_value;
  assign ram_address_MPORT_mask = 1'h1;
  assign ram_address_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_size_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_size_io_deq_bits_MPORT_data = ram_size[ram_size_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_size_MPORT_data = io_enq_bits_size;
  assign ram_size_MPORT_addr = enq_ptr_value;
  assign ram_size_MPORT_mask = 1'h1;
  assign ram_size_MPORT_en = io_enq_ready & io_enq_valid;
  assign io_enq_ready = ~full; // @[Decoupled.scala 241:19]
  assign io_deq_valid = ~empty; // @[Decoupled.scala 240:19]
  assign io_deq_bits_write = ram_write_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_address = ram_address_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_size = ram_size_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  always @(posedge clock) begin
    if(ram_write_MPORT_en & ram_write_MPORT_mask) begin
      ram_write[ram_write_MPORT_addr] <= ram_write_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_address_MPORT_en & ram_address_MPORT_mask) begin
      ram_address[ram_address_MPORT_addr] <= ram_address_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_size_MPORT_en & ram_size_MPORT_mask) begin
      ram_size[ram_size_MPORT_addr] <= ram_size_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if (reset) begin // @[Counter.scala 60:40]
      enq_ptr_value <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_enq) begin // @[Decoupled.scala 229:17]
      enq_ptr_value <= enq_ptr_value + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Counter.scala 60:40]
      deq_ptr_value <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_deq) begin // @[Decoupled.scala 233:17]
      deq_ptr_value <= deq_ptr_value + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Decoupled.scala 221:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 221:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 236:28]
      maybe_full <= do_enq; // @[Decoupled.scala 237:16]
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
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_write[initvar] = _RAND_0[0:0];
  _RAND_1 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_address[initvar] = _RAND_1[19:0];
  _RAND_2 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_size[initvar] = _RAND_2[19:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_3 = {1{`RANDOM}};
  enq_ptr_value = _RAND_3[0:0];
  _RAND_4 = {1{`RANDOM}};
  deq_ptr_value = _RAND_4[0:0];
  _RAND_5 = {1{`RANDOM}};
  maybe_full = _RAND_5[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module RequestSplitter(
  input         clock,
  input         reset,
  output        io_in_ready,
  input         io_in_valid,
  input         io_in_bits_write,
  input  [19:0] io_in_bits_address,
  input  [19:0] io_in_bits_size,
  input         io_out_ready,
  output        io_out_valid,
  output        io_out_bits_write,
  output [19:0] io_out_bits_address,
  output [19:0] io_out_bits_size
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
`endif // RANDOMIZE_REG_INIT
  reg [19:0] sizeCounter; // @[RequestSplitter.scala 21:33]
  reg  sizeCounterValid; // @[RequestSplitter.scala 22:33]
  reg [19:0] addressOffset; // @[RequestSplitter.scala 23:33]
  wire [19:0] address = io_in_bits_address + addressOffset; // @[RequestSplitter.scala 24:42]
  wire  _T_3 = io_in_valid & io_out_ready; // @[RequestSplitter.scala 38:21]
  wire [19:0] _sizeCounter_T_1 = sizeCounter - 20'h80; // @[RequestSplitter.scala 53:38]
  wire [19:0] _addressOffset_T_1 = addressOffset + 20'h80; // @[RequestSplitter.scala 54:42]
  wire [19:0] _sizeCounter_T_3 = io_in_bits_size - 20'h80; // @[RequestSplitter.scala 56:39]
  wire [19:0] _GEN_2 = sizeCounterValid ? _sizeCounter_T_1 : _sizeCounter_T_3; // @[RequestSplitter.scala 52:32 RequestSplitter.scala 53:23 RequestSplitter.scala 56:23]
  wire [19:0] _GEN_3 = sizeCounterValid ? _addressOffset_T_1 : 20'h80; // @[RequestSplitter.scala 52:32 RequestSplitter.scala 54:25 RequestSplitter.scala 57:25]
  wire  _GEN_4 = sizeCounterValid ? sizeCounterValid : 1'h1; // @[RequestSplitter.scala 52:32 RequestSplitter.scala 22:33 RequestSplitter.scala 58:28]
  wire [19:0] _GEN_8 = sizeCounterValid & sizeCounter < 20'h80 ? sizeCounter : 20'h7f; // @[RequestSplitter.scala 29:55 RequestSplitter.scala 30:19 RequestSplitter.scala 43:19]
  wire  _GEN_12 = sizeCounterValid & sizeCounter < 20'h80 & io_out_ready; // @[RequestSplitter.scala 29:55 RequestSplitter.scala 37:16 RequestSplitter.scala 50:16]
  assign io_in_ready = io_in_bits_size < 20'h80 ? io_out_ready : _GEN_12; // @[RequestSplitter.scala 26:34 RequestSplitter.scala 27:12]
  assign io_out_valid = io_in_valid; // @[RequestSplitter.scala 26:34 RequestSplitter.scala 27:12]
  assign io_out_bits_write = io_in_bits_write; // @[RequestSplitter.scala 26:34 RequestSplitter.scala 27:12]
  assign io_out_bits_address = io_in_bits_size < 20'h80 ? io_in_bits_address : address; // @[RequestSplitter.scala 26:34 RequestSplitter.scala 27:12]
  assign io_out_bits_size = io_in_bits_size < 20'h80 ? io_in_bits_size : _GEN_8; // @[RequestSplitter.scala 26:34 RequestSplitter.scala 27:12]
  always @(posedge clock) begin
    if (reset) begin // @[RequestSplitter.scala 21:33]
      sizeCounter <= 20'h0; // @[RequestSplitter.scala 21:33]
    end else if (!(io_in_bits_size < 20'h80)) begin // @[RequestSplitter.scala 26:34]
      if (!(sizeCounterValid & sizeCounter < 20'h80)) begin // @[RequestSplitter.scala 29:55]
        if (_T_3) begin // @[RequestSplitter.scala 51:38]
          sizeCounter <= _GEN_2;
        end
      end
    end
    if (reset) begin // @[RequestSplitter.scala 22:33]
      sizeCounterValid <= 1'h0; // @[RequestSplitter.scala 22:33]
    end else if (!(io_in_bits_size < 20'h80)) begin // @[RequestSplitter.scala 26:34]
      if (sizeCounterValid & sizeCounter < 20'h80) begin // @[RequestSplitter.scala 29:55]
        if (io_in_valid & io_out_ready) begin // @[RequestSplitter.scala 38:38]
          sizeCounterValid <= 1'h0; // @[RequestSplitter.scala 39:26]
        end
      end else if (_T_3) begin // @[RequestSplitter.scala 51:38]
        sizeCounterValid <= _GEN_4;
      end
    end
    if (reset) begin // @[RequestSplitter.scala 23:33]
      addressOffset <= 20'h0; // @[RequestSplitter.scala 23:33]
    end else if (!(io_in_bits_size < 20'h80)) begin // @[RequestSplitter.scala 26:34]
      if (sizeCounterValid & sizeCounter < 20'h80) begin // @[RequestSplitter.scala 29:55]
        if (io_in_valid & io_out_ready) begin // @[RequestSplitter.scala 38:38]
          addressOffset <= 20'h0; // @[RequestSplitter.scala 40:23]
        end
      end else if (_T_3) begin // @[RequestSplitter.scala 51:38]
        addressOffset <= _GEN_3;
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
  sizeCounter = _RAND_0[19:0];
  _RAND_1 = {1{`RANDOM}};
  sizeCounterValid = _RAND_1[0:0];
  _RAND_2 = {1{`RANDOM}};
  addressOffset = _RAND_2[19:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Queue_36(
  input         clock,
  input         reset,
  output        io_enq_ready,
  input         io_enq_valid,
  input  [63:0] io_enq_bits_data,
  input         io_enq_bits_last,
  input         io_deq_ready,
  output        io_deq_valid,
  output [63:0] io_deq_bits_data,
  output        io_deq_bits_last
);
`ifdef RANDOMIZE_MEM_INIT
  reg [63:0] _RAND_0;
  reg [31:0] _RAND_1;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
`endif // RANDOMIZE_REG_INIT
  reg [63:0] ram_data [0:1]; // @[Decoupled.scala 218:16]
  wire [63:0] ram_data_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_data_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [63:0] ram_data_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_data_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_data_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_data_MPORT_en; // @[Decoupled.scala 218:16]
  reg  ram_last [0:1]; // @[Decoupled.scala 218:16]
  wire  ram_last_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_last_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_last_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_last_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_last_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_last_MPORT_en; // @[Decoupled.scala 218:16]
  reg  enq_ptr_value; // @[Counter.scala 60:40]
  reg  deq_ptr_value; // @[Counter.scala 60:40]
  reg  maybe_full; // @[Decoupled.scala 221:27]
  wire  ptr_match = enq_ptr_value == deq_ptr_value; // @[Decoupled.scala 223:33]
  wire  empty = ptr_match & ~maybe_full; // @[Decoupled.scala 224:25]
  wire  full = ptr_match & maybe_full; // @[Decoupled.scala 225:24]
  wire  do_enq = io_enq_ready & io_enq_valid; // @[Decoupled.scala 40:37]
  wire  do_deq = io_deq_ready & io_deq_valid; // @[Decoupled.scala 40:37]
  assign ram_data_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_data_io_deq_bits_MPORT_data = ram_data[ram_data_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_data_MPORT_data = io_enq_bits_data;
  assign ram_data_MPORT_addr = enq_ptr_value;
  assign ram_data_MPORT_mask = 1'h1;
  assign ram_data_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_last_io_deq_bits_MPORT_addr = deq_ptr_value;
  assign ram_last_io_deq_bits_MPORT_data = ram_last[ram_last_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_last_MPORT_data = io_enq_bits_last;
  assign ram_last_MPORT_addr = enq_ptr_value;
  assign ram_last_MPORT_mask = 1'h1;
  assign ram_last_MPORT_en = io_enq_ready & io_enq_valid;
  assign io_enq_ready = ~full; // @[Decoupled.scala 241:19]
  assign io_deq_valid = ~empty; // @[Decoupled.scala 240:19]
  assign io_deq_bits_data = ram_data_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_last = ram_last_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  always @(posedge clock) begin
    if(ram_data_MPORT_en & ram_data_MPORT_mask) begin
      ram_data[ram_data_MPORT_addr] <= ram_data_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_last_MPORT_en & ram_last_MPORT_mask) begin
      ram_last[ram_last_MPORT_addr] <= ram_last_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if (reset) begin // @[Counter.scala 60:40]
      enq_ptr_value <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_enq) begin // @[Decoupled.scala 229:17]
      enq_ptr_value <= enq_ptr_value + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Counter.scala 60:40]
      deq_ptr_value <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_deq) begin // @[Decoupled.scala 233:17]
      deq_ptr_value <= deq_ptr_value + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Decoupled.scala 221:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 221:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 236:28]
      maybe_full <= do_enq; // @[Decoupled.scala 237:16]
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
  _RAND_0 = {2{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_data[initvar] = _RAND_0[63:0];
  _RAND_1 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_last[initvar] = _RAND_1[0:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_2 = {1{`RANDOM}};
  enq_ptr_value = _RAND_2[0:0];
  _RAND_3 = {1{`RANDOM}};
  deq_ptr_value = _RAND_3[0:0];
  _RAND_4 = {1{`RANDOM}};
  maybe_full = _RAND_4[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Queue_37(
  input   clock,
  input   reset,
  output  io_enq_ready,
  input   io_enq_valid,
  input   io_deq_ready,
  output  io_deq_valid
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
`endif // RANDOMIZE_REG_INIT
  reg  enq_ptr_value; // @[Counter.scala 60:40]
  reg  deq_ptr_value; // @[Counter.scala 60:40]
  reg  maybe_full; // @[Decoupled.scala 221:27]
  wire  ptr_match = enq_ptr_value == deq_ptr_value; // @[Decoupled.scala 223:33]
  wire  empty = ptr_match & ~maybe_full; // @[Decoupled.scala 224:25]
  wire  full = ptr_match & maybe_full; // @[Decoupled.scala 225:24]
  wire  do_enq = io_enq_ready & io_enq_valid; // @[Decoupled.scala 40:37]
  wire  do_deq = io_deq_ready & io_deq_valid; // @[Decoupled.scala 40:37]
  assign io_enq_ready = ~full; // @[Decoupled.scala 241:19]
  assign io_deq_valid = ~empty; // @[Decoupled.scala 240:19]
  always @(posedge clock) begin
    if (reset) begin // @[Counter.scala 60:40]
      enq_ptr_value <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_enq) begin // @[Decoupled.scala 229:17]
      enq_ptr_value <= enq_ptr_value + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Counter.scala 60:40]
      deq_ptr_value <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_deq) begin // @[Decoupled.scala 233:17]
      deq_ptr_value <= deq_ptr_value + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Decoupled.scala 221:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 221:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 236:28]
      maybe_full <= do_enq; // @[Decoupled.scala 237:16]
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
  enq_ptr_value = _RAND_0[0:0];
  _RAND_1 = {1{`RANDOM}};
  deq_ptr_value = _RAND_1[0:0];
  _RAND_2 = {1{`RANDOM}};
  maybe_full = _RAND_2[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module VectorSerializer(
  input         clock,
  input         reset,
  output        io_in_ready,
  input         io_in_valid,
  input  [15:0] io_in_bits_0,
  input  [15:0] io_in_bits_1,
  input  [15:0] io_in_bits_2,
  input  [15:0] io_in_bits_3,
  input  [15:0] io_in_bits_4,
  input  [15:0] io_in_bits_5,
  input  [15:0] io_in_bits_6,
  input  [15:0] io_in_bits_7,
  input         io_out_ready,
  output        io_out_valid,
  output [63:0] io_out_bits
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
`endif // RANDOMIZE_REG_INIT
  reg [15:0] bits_0; // @[VectorSerializer.scala 37:22]
  reg [15:0] bits_1; // @[VectorSerializer.scala 37:22]
  reg [15:0] bits_2; // @[VectorSerializer.scala 37:22]
  reg [15:0] bits_3; // @[VectorSerializer.scala 37:22]
  reg [15:0] bits_4; // @[VectorSerializer.scala 37:22]
  reg [15:0] bits_5; // @[VectorSerializer.scala 37:22]
  reg [15:0] bits_6; // @[VectorSerializer.scala 37:22]
  reg [15:0] bits_7; // @[VectorSerializer.scala 37:22]
  reg  valid; // @[VectorSerializer.scala 38:22]
  wire  _T = valid & io_out_ready; // @[VectorSerializer.scala 40:48]
  reg  ctr; // @[Counter.scala 60:40]
  wire  wrap = _T & ctr; // @[Counter.scala 118:17 Counter.scala 118:24]
  wire [3:0] _out_T = ctr * 3'h4; // @[VectorSerializer.scala 46:18]
  wire [4:0] _out_T_1 = {{1'd0}, _out_T}; // @[VectorSerializer.scala 46:40]
  wire [15:0] _GEN_3 = 3'h1 == _out_T_1[2:0] ? $signed(bits_1) : $signed(bits_0); // @[VectorSerializer.scala 46:47 VectorSerializer.scala 46:47]
  wire [15:0] _GEN_4 = 3'h2 == _out_T_1[2:0] ? $signed(bits_2) : $signed(_GEN_3); // @[VectorSerializer.scala 46:47 VectorSerializer.scala 46:47]
  wire [15:0] _GEN_5 = 3'h3 == _out_T_1[2:0] ? $signed(bits_3) : $signed(_GEN_4); // @[VectorSerializer.scala 46:47 VectorSerializer.scala 46:47]
  wire [15:0] _GEN_6 = 3'h4 == _out_T_1[2:0] ? $signed(bits_4) : $signed(_GEN_5); // @[VectorSerializer.scala 46:47 VectorSerializer.scala 46:47]
  wire [15:0] _GEN_7 = 3'h5 == _out_T_1[2:0] ? $signed(bits_5) : $signed(_GEN_6); // @[VectorSerializer.scala 46:47 VectorSerializer.scala 46:47]
  wire [15:0] _GEN_8 = 3'h6 == _out_T_1[2:0] ? $signed(bits_6) : $signed(_GEN_7); // @[VectorSerializer.scala 46:47 VectorSerializer.scala 46:47]
  wire [15:0] out_0 = 3'h7 == _out_T_1[2:0] ? $signed(bits_7) : $signed(_GEN_8); // @[VectorSerializer.scala 46:47]
  wire [3:0] _out_T_7 = _out_T + 4'h1; // @[VectorSerializer.scala 46:40]
  wire [15:0] _GEN_11 = 3'h1 == _out_T_7[2:0] ? $signed(bits_1) : $signed(bits_0); // @[VectorSerializer.scala 46:47 VectorSerializer.scala 46:47]
  wire [15:0] _GEN_12 = 3'h2 == _out_T_7[2:0] ? $signed(bits_2) : $signed(_GEN_11); // @[VectorSerializer.scala 46:47 VectorSerializer.scala 46:47]
  wire [15:0] _GEN_13 = 3'h3 == _out_T_7[2:0] ? $signed(bits_3) : $signed(_GEN_12); // @[VectorSerializer.scala 46:47 VectorSerializer.scala 46:47]
  wire [15:0] _GEN_14 = 3'h4 == _out_T_7[2:0] ? $signed(bits_4) : $signed(_GEN_13); // @[VectorSerializer.scala 46:47 VectorSerializer.scala 46:47]
  wire [15:0] _GEN_15 = 3'h5 == _out_T_7[2:0] ? $signed(bits_5) : $signed(_GEN_14); // @[VectorSerializer.scala 46:47 VectorSerializer.scala 46:47]
  wire [15:0] _GEN_16 = 3'h6 == _out_T_7[2:0] ? $signed(bits_6) : $signed(_GEN_15); // @[VectorSerializer.scala 46:47 VectorSerializer.scala 46:47]
  wire [15:0] out_1 = 3'h7 == _out_T_7[2:0] ? $signed(bits_7) : $signed(_GEN_16); // @[VectorSerializer.scala 46:47]
  wire [3:0] _out_T_12 = _out_T + 4'h2; // @[VectorSerializer.scala 46:40]
  wire [15:0] _GEN_19 = 3'h1 == _out_T_12[2:0] ? $signed(bits_1) : $signed(bits_0); // @[VectorSerializer.scala 46:47 VectorSerializer.scala 46:47]
  wire [15:0] _GEN_20 = 3'h2 == _out_T_12[2:0] ? $signed(bits_2) : $signed(_GEN_19); // @[VectorSerializer.scala 46:47 VectorSerializer.scala 46:47]
  wire [15:0] _GEN_21 = 3'h3 == _out_T_12[2:0] ? $signed(bits_3) : $signed(_GEN_20); // @[VectorSerializer.scala 46:47 VectorSerializer.scala 46:47]
  wire [15:0] _GEN_22 = 3'h4 == _out_T_12[2:0] ? $signed(bits_4) : $signed(_GEN_21); // @[VectorSerializer.scala 46:47 VectorSerializer.scala 46:47]
  wire [15:0] _GEN_23 = 3'h5 == _out_T_12[2:0] ? $signed(bits_5) : $signed(_GEN_22); // @[VectorSerializer.scala 46:47 VectorSerializer.scala 46:47]
  wire [15:0] _GEN_24 = 3'h6 == _out_T_12[2:0] ? $signed(bits_6) : $signed(_GEN_23); // @[VectorSerializer.scala 46:47 VectorSerializer.scala 46:47]
  wire [15:0] out_2 = 3'h7 == _out_T_12[2:0] ? $signed(bits_7) : $signed(_GEN_24); // @[VectorSerializer.scala 46:47]
  wire [3:0] _out_T_17 = _out_T + 4'h3; // @[VectorSerializer.scala 46:40]
  wire [15:0] _GEN_27 = 3'h1 == _out_T_17[2:0] ? $signed(bits_1) : $signed(bits_0); // @[VectorSerializer.scala 46:47 VectorSerializer.scala 46:47]
  wire [15:0] _GEN_28 = 3'h2 == _out_T_17[2:0] ? $signed(bits_2) : $signed(_GEN_27); // @[VectorSerializer.scala 46:47 VectorSerializer.scala 46:47]
  wire [15:0] _GEN_29 = 3'h3 == _out_T_17[2:0] ? $signed(bits_3) : $signed(_GEN_28); // @[VectorSerializer.scala 46:47 VectorSerializer.scala 46:47]
  wire [15:0] _GEN_30 = 3'h4 == _out_T_17[2:0] ? $signed(bits_4) : $signed(_GEN_29); // @[VectorSerializer.scala 46:47 VectorSerializer.scala 46:47]
  wire [15:0] _GEN_31 = 3'h5 == _out_T_17[2:0] ? $signed(bits_5) : $signed(_GEN_30); // @[VectorSerializer.scala 46:47 VectorSerializer.scala 46:47]
  wire [15:0] _GEN_32 = 3'h6 == _out_T_17[2:0] ? $signed(bits_6) : $signed(_GEN_31); // @[VectorSerializer.scala 46:47 VectorSerializer.scala 46:47]
  wire [15:0] out_3 = 3'h7 == _out_T_17[2:0] ? $signed(bits_7) : $signed(_GEN_32); // @[VectorSerializer.scala 46:47]
  wire [31:0] io_out_bits_lo = {out_1,out_0}; // @[Cat.scala 30:58]
  wire [31:0] io_out_bits_hi = {out_3,out_2}; // @[Cat.scala 30:58]
  wire [127:0] _T_1 = {io_in_bits_7,io_in_bits_6,io_in_bits_5,io_in_bits_4,io_in_bits_3,io_in_bits_2,io_in_bits_1,
    io_in_bits_0}; // @[VectorSerializer.scala 56:34]
  wire [15:0] _T_3 = _T_1[15:0]; // @[VectorSerializer.scala 56:34]
  wire [15:0] _T_5 = _T_1[31:16]; // @[VectorSerializer.scala 56:34]
  wire [15:0] _T_7 = _T_1[47:32]; // @[VectorSerializer.scala 56:34]
  wire [15:0] _T_9 = _T_1[63:48]; // @[VectorSerializer.scala 56:34]
  wire [15:0] _T_11 = _T_1[79:64]; // @[VectorSerializer.scala 56:34]
  wire [15:0] _T_13 = _T_1[95:80]; // @[VectorSerializer.scala 56:34]
  wire [15:0] _T_15 = _T_1[111:96]; // @[VectorSerializer.scala 56:34]
  wire [15:0] _T_17 = _T_1[127:112]; // @[VectorSerializer.scala 56:34]
  assign io_in_ready = ~valid | wrap; // @[VectorSerializer.scala 52:25]
  assign io_out_valid = valid; // @[VectorSerializer.scala 50:16]
  assign io_out_bits = {io_out_bits_hi,io_out_bits_lo}; // @[Cat.scala 30:58]
  always @(posedge clock) begin
    if (reset) begin // @[VectorSerializer.scala 37:22]
      bits_0 <= 16'sh0; // @[VectorSerializer.scala 37:22]
    end else if (io_in_ready) begin // @[VectorSerializer.scala 54:21]
      if (io_in_valid) begin // @[VectorSerializer.scala 55:23]
        bits_0 <= _T_3; // @[VectorSerializer.scala 56:12]
      end
    end
    if (reset) begin // @[VectorSerializer.scala 37:22]
      bits_1 <= 16'sh0; // @[VectorSerializer.scala 37:22]
    end else if (io_in_ready) begin // @[VectorSerializer.scala 54:21]
      if (io_in_valid) begin // @[VectorSerializer.scala 55:23]
        bits_1 <= _T_5; // @[VectorSerializer.scala 56:12]
      end
    end
    if (reset) begin // @[VectorSerializer.scala 37:22]
      bits_2 <= 16'sh0; // @[VectorSerializer.scala 37:22]
    end else if (io_in_ready) begin // @[VectorSerializer.scala 54:21]
      if (io_in_valid) begin // @[VectorSerializer.scala 55:23]
        bits_2 <= _T_7; // @[VectorSerializer.scala 56:12]
      end
    end
    if (reset) begin // @[VectorSerializer.scala 37:22]
      bits_3 <= 16'sh0; // @[VectorSerializer.scala 37:22]
    end else if (io_in_ready) begin // @[VectorSerializer.scala 54:21]
      if (io_in_valid) begin // @[VectorSerializer.scala 55:23]
        bits_3 <= _T_9; // @[VectorSerializer.scala 56:12]
      end
    end
    if (reset) begin // @[VectorSerializer.scala 37:22]
      bits_4 <= 16'sh0; // @[VectorSerializer.scala 37:22]
    end else if (io_in_ready) begin // @[VectorSerializer.scala 54:21]
      if (io_in_valid) begin // @[VectorSerializer.scala 55:23]
        bits_4 <= _T_11; // @[VectorSerializer.scala 56:12]
      end
    end
    if (reset) begin // @[VectorSerializer.scala 37:22]
      bits_5 <= 16'sh0; // @[VectorSerializer.scala 37:22]
    end else if (io_in_ready) begin // @[VectorSerializer.scala 54:21]
      if (io_in_valid) begin // @[VectorSerializer.scala 55:23]
        bits_5 <= _T_13; // @[VectorSerializer.scala 56:12]
      end
    end
    if (reset) begin // @[VectorSerializer.scala 37:22]
      bits_6 <= 16'sh0; // @[VectorSerializer.scala 37:22]
    end else if (io_in_ready) begin // @[VectorSerializer.scala 54:21]
      if (io_in_valid) begin // @[VectorSerializer.scala 55:23]
        bits_6 <= _T_15; // @[VectorSerializer.scala 56:12]
      end
    end
    if (reset) begin // @[VectorSerializer.scala 37:22]
      bits_7 <= 16'sh0; // @[VectorSerializer.scala 37:22]
    end else if (io_in_ready) begin // @[VectorSerializer.scala 54:21]
      if (io_in_valid) begin // @[VectorSerializer.scala 55:23]
        bits_7 <= _T_17; // @[VectorSerializer.scala 56:12]
      end
    end
    if (reset) begin // @[VectorSerializer.scala 38:22]
      valid <= 1'h0; // @[VectorSerializer.scala 38:22]
    end else if (io_in_ready) begin // @[VectorSerializer.scala 54:21]
      valid <= io_in_valid;
    end
    if (reset) begin // @[Counter.scala 60:40]
      ctr <= 1'h0; // @[Counter.scala 60:40]
    end else if (_T) begin // @[Counter.scala 118:17]
      ctr <= ctr + 1'h1; // @[Counter.scala 76:15]
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
  bits_0 = _RAND_0[15:0];
  _RAND_1 = {1{`RANDOM}};
  bits_1 = _RAND_1[15:0];
  _RAND_2 = {1{`RANDOM}};
  bits_2 = _RAND_2[15:0];
  _RAND_3 = {1{`RANDOM}};
  bits_3 = _RAND_3[15:0];
  _RAND_4 = {1{`RANDOM}};
  bits_4 = _RAND_4[15:0];
  _RAND_5 = {1{`RANDOM}};
  bits_5 = _RAND_5[15:0];
  _RAND_6 = {1{`RANDOM}};
  bits_6 = _RAND_6[15:0];
  _RAND_7 = {1{`RANDOM}};
  bits_7 = _RAND_7[15:0];
  _RAND_8 = {1{`RANDOM}};
  valid = _RAND_8[0:0];
  _RAND_9 = {1{`RANDOM}};
  ctr = _RAND_9[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module DataCounter(
  input         clock,
  input         reset,
  output        io_in_ready,
  input         io_in_valid,
  input  [63:0] io_in_bits,
  input         io_out_ready,
  output        io_out_valid,
  output [63:0] io_out_bits,
  output        io_len_ready,
  input         io_len_valid,
  input  [7:0]  io_len_bits
);
  wire  counter_clock; // @[Counter.scala 34:19]
  wire  counter_reset; // @[Counter.scala 34:19]
  wire  counter_io_value_ready; // @[Counter.scala 34:19]
  wire [7:0] counter_io_value_bits; // @[Counter.scala 34:19]
  wire  counter_io_resetValue; // @[Counter.scala 34:19]
  wire  _counter_io_resetValue_T = io_out_ready & io_out_valid; // @[Decoupled.scala 40:37]
  Counter_10 counter ( // @[Counter.scala 34:19]
    .clock(counter_clock),
    .reset(counter_reset),
    .io_value_ready(counter_io_value_ready),
    .io_value_bits(counter_io_value_bits),
    .io_resetValue(counter_io_resetValue)
  );
  assign io_in_ready = io_len_valid & io_out_ready; // @[DataCounter.scala 27:25]
  assign io_out_valid = io_in_valid & io_len_valid; // @[DataCounter.scala 26:28]
  assign io_out_bits = io_in_bits; // @[DataCounter.scala 25:15]
  assign io_len_ready = counter_io_value_bits == io_len_bits & (io_in_valid & io_out_ready); // @[DataCounter.scala 29:44 DataCounter.scala 31:15 DataCounter.scala 34:15]
  assign counter_clock = clock;
  assign counter_reset = reset;
  assign counter_io_value_ready = counter_io_value_bits == io_len_bits ? 1'h0 : _counter_io_resetValue_T; // @[DataCounter.scala 29:44 Counter.scala 36:22 DataCounter.scala 36:28]
  assign counter_io_resetValue = counter_io_value_bits == io_len_bits & _counter_io_resetValue_T; // @[DataCounter.scala 29:44 DataCounter.scala 32:27 Counter.scala 35:21]
endmodule
module VectorDeserializer(
  input         clock,
  input         reset,
  output        io_in_ready,
  input         io_in_valid,
  input  [63:0] io_in_bits,
  input         io_out_ready,
  output        io_out_valid,
  output [15:0] io_out_bits_0,
  output [15:0] io_out_bits_1,
  output [15:0] io_out_bits_2,
  output [15:0] io_out_bits_3,
  output [15:0] io_out_bits_4,
  output [15:0] io_out_bits_5,
  output [15:0] io_out_bits_6,
  output [15:0] io_out_bits_7
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
`endif // RANDOMIZE_REG_INIT
  reg [15:0] bits_0; // @[VectorDeserializer.scala 41:24]
  reg [15:0] bits_1; // @[VectorDeserializer.scala 41:24]
  reg [15:0] bits_2; // @[VectorDeserializer.scala 41:24]
  reg [15:0] bits_3; // @[VectorDeserializer.scala 41:24]
  reg [15:0] bits_4; // @[VectorDeserializer.scala 41:24]
  reg [15:0] bits_5; // @[VectorDeserializer.scala 41:24]
  reg [15:0] bits_6; // @[VectorDeserializer.scala 41:24]
  reg [15:0] bits_7; // @[VectorDeserializer.scala 41:24]
  reg  valid; // @[VectorDeserializer.scala 42:24]
  wire  _T = io_in_ready & io_in_valid; // @[Decoupled.scala 40:37]
  reg  ctr; // @[Counter.scala 60:40]
  wire  wrap = _T & ctr; // @[Counter.scala 118:17 Counter.scala 118:24]
  wire [3:0] _T_2 = ctr * 3'h4; // @[VectorDeserializer.scala 54:18]
  wire [4:0] _T_3 = {{1'd0}, _T_2}; // @[VectorDeserializer.scala 54:40]
  wire [15:0] _bits_T_17 = io_in_bits[15:0]; // @[VectorDeserializer.scala 37:57]
  wire [15:0] _GEN_2 = 3'h0 == _T_3[2:0] ? $signed(_bits_T_17) : $signed(bits_0); // @[VectorDeserializer.scala 54:47 VectorDeserializer.scala 54:47 VectorDeserializer.scala 41:24]
  wire [15:0] _GEN_3 = 3'h1 == _T_3[2:0] ? $signed(_bits_T_17) : $signed(bits_1); // @[VectorDeserializer.scala 54:47 VectorDeserializer.scala 54:47 VectorDeserializer.scala 41:24]
  wire [15:0] _GEN_4 = 3'h2 == _T_3[2:0] ? $signed(_bits_T_17) : $signed(bits_2); // @[VectorDeserializer.scala 54:47 VectorDeserializer.scala 54:47 VectorDeserializer.scala 41:24]
  wire [15:0] _GEN_5 = 3'h3 == _T_3[2:0] ? $signed(_bits_T_17) : $signed(bits_3); // @[VectorDeserializer.scala 54:47 VectorDeserializer.scala 54:47 VectorDeserializer.scala 41:24]
  wire [15:0] _GEN_6 = 3'h4 == _T_3[2:0] ? $signed(_bits_T_17) : $signed(bits_4); // @[VectorDeserializer.scala 54:47 VectorDeserializer.scala 54:47 VectorDeserializer.scala 41:24]
  wire [15:0] _GEN_7 = 3'h5 == _T_3[2:0] ? $signed(_bits_T_17) : $signed(bits_5); // @[VectorDeserializer.scala 54:47 VectorDeserializer.scala 54:47 VectorDeserializer.scala 41:24]
  wire [15:0] _GEN_8 = 3'h6 == _T_3[2:0] ? $signed(_bits_T_17) : $signed(bits_6); // @[VectorDeserializer.scala 54:47 VectorDeserializer.scala 54:47 VectorDeserializer.scala 41:24]
  wire [15:0] _GEN_9 = 3'h7 == _T_3[2:0] ? $signed(_bits_T_17) : $signed(bits_7); // @[VectorDeserializer.scala 54:47 VectorDeserializer.scala 54:47 VectorDeserializer.scala 41:24]
  wire [3:0] _T_8 = _T_2 + 4'h1; // @[VectorDeserializer.scala 54:40]
  wire [15:0] _bits_T_19 = io_in_bits[31:16]; // @[VectorDeserializer.scala 37:57]
  wire [15:0] _GEN_10 = 3'h0 == _T_8[2:0] ? $signed(_bits_T_19) : $signed(_GEN_2); // @[VectorDeserializer.scala 54:47 VectorDeserializer.scala 54:47]
  wire [15:0] _GEN_11 = 3'h1 == _T_8[2:0] ? $signed(_bits_T_19) : $signed(_GEN_3); // @[VectorDeserializer.scala 54:47 VectorDeserializer.scala 54:47]
  wire [15:0] _GEN_12 = 3'h2 == _T_8[2:0] ? $signed(_bits_T_19) : $signed(_GEN_4); // @[VectorDeserializer.scala 54:47 VectorDeserializer.scala 54:47]
  wire [15:0] _GEN_13 = 3'h3 == _T_8[2:0] ? $signed(_bits_T_19) : $signed(_GEN_5); // @[VectorDeserializer.scala 54:47 VectorDeserializer.scala 54:47]
  wire [15:0] _GEN_14 = 3'h4 == _T_8[2:0] ? $signed(_bits_T_19) : $signed(_GEN_6); // @[VectorDeserializer.scala 54:47 VectorDeserializer.scala 54:47]
  wire [15:0] _GEN_15 = 3'h5 == _T_8[2:0] ? $signed(_bits_T_19) : $signed(_GEN_7); // @[VectorDeserializer.scala 54:47 VectorDeserializer.scala 54:47]
  wire [15:0] _GEN_16 = 3'h6 == _T_8[2:0] ? $signed(_bits_T_19) : $signed(_GEN_8); // @[VectorDeserializer.scala 54:47 VectorDeserializer.scala 54:47]
  wire [15:0] _GEN_17 = 3'h7 == _T_8[2:0] ? $signed(_bits_T_19) : $signed(_GEN_9); // @[VectorDeserializer.scala 54:47 VectorDeserializer.scala 54:47]
  wire [3:0] _T_12 = _T_2 + 4'h2; // @[VectorDeserializer.scala 54:40]
  wire [15:0] _bits_T_21 = io_in_bits[47:32]; // @[VectorDeserializer.scala 37:57]
  wire [3:0] _T_16 = _T_2 + 4'h3; // @[VectorDeserializer.scala 54:40]
  wire [15:0] _bits_T_23 = io_in_bits[63:48]; // @[VectorDeserializer.scala 37:57]
  wire  _T_18 = io_out_ready & io_out_valid; // @[Decoupled.scala 40:37]
  assign io_in_ready = ~valid | io_out_ready; // @[VectorDeserializer.scala 50:27]
  assign io_out_valid = valid; // @[VectorDeserializer.scala 47:18]
  assign io_out_bits_0 = bits_0; // @[VectorDeserializer.scala 48:17]
  assign io_out_bits_1 = bits_1; // @[VectorDeserializer.scala 48:17]
  assign io_out_bits_2 = bits_2; // @[VectorDeserializer.scala 48:17]
  assign io_out_bits_3 = bits_3; // @[VectorDeserializer.scala 48:17]
  assign io_out_bits_4 = bits_4; // @[VectorDeserializer.scala 48:17]
  assign io_out_bits_5 = bits_5; // @[VectorDeserializer.scala 48:17]
  assign io_out_bits_6 = bits_6; // @[VectorDeserializer.scala 48:17]
  assign io_out_bits_7 = bits_7; // @[VectorDeserializer.scala 48:17]
  always @(posedge clock) begin
    if (reset) begin // @[VectorDeserializer.scala 41:24]
      bits_0 <= 16'sh0; // @[VectorDeserializer.scala 41:24]
    end else if (_T) begin // @[VectorDeserializer.scala 52:24]
      if (3'h0 == _T_16[2:0]) begin // @[VectorDeserializer.scala 54:47]
        bits_0 <= _bits_T_23; // @[VectorDeserializer.scala 54:47]
      end else if (3'h0 == _T_12[2:0]) begin // @[VectorDeserializer.scala 54:47]
        bits_0 <= _bits_T_21; // @[VectorDeserializer.scala 54:47]
      end else begin
        bits_0 <= _GEN_10;
      end
    end
    if (reset) begin // @[VectorDeserializer.scala 41:24]
      bits_1 <= 16'sh0; // @[VectorDeserializer.scala 41:24]
    end else if (_T) begin // @[VectorDeserializer.scala 52:24]
      if (3'h1 == _T_16[2:0]) begin // @[VectorDeserializer.scala 54:47]
        bits_1 <= _bits_T_23; // @[VectorDeserializer.scala 54:47]
      end else if (3'h1 == _T_12[2:0]) begin // @[VectorDeserializer.scala 54:47]
        bits_1 <= _bits_T_21; // @[VectorDeserializer.scala 54:47]
      end else begin
        bits_1 <= _GEN_11;
      end
    end
    if (reset) begin // @[VectorDeserializer.scala 41:24]
      bits_2 <= 16'sh0; // @[VectorDeserializer.scala 41:24]
    end else if (_T) begin // @[VectorDeserializer.scala 52:24]
      if (3'h2 == _T_16[2:0]) begin // @[VectorDeserializer.scala 54:47]
        bits_2 <= _bits_T_23; // @[VectorDeserializer.scala 54:47]
      end else if (3'h2 == _T_12[2:0]) begin // @[VectorDeserializer.scala 54:47]
        bits_2 <= _bits_T_21; // @[VectorDeserializer.scala 54:47]
      end else begin
        bits_2 <= _GEN_12;
      end
    end
    if (reset) begin // @[VectorDeserializer.scala 41:24]
      bits_3 <= 16'sh0; // @[VectorDeserializer.scala 41:24]
    end else if (_T) begin // @[VectorDeserializer.scala 52:24]
      if (3'h3 == _T_16[2:0]) begin // @[VectorDeserializer.scala 54:47]
        bits_3 <= _bits_T_23; // @[VectorDeserializer.scala 54:47]
      end else if (3'h3 == _T_12[2:0]) begin // @[VectorDeserializer.scala 54:47]
        bits_3 <= _bits_T_21; // @[VectorDeserializer.scala 54:47]
      end else begin
        bits_3 <= _GEN_13;
      end
    end
    if (reset) begin // @[VectorDeserializer.scala 41:24]
      bits_4 <= 16'sh0; // @[VectorDeserializer.scala 41:24]
    end else if (_T) begin // @[VectorDeserializer.scala 52:24]
      if (3'h4 == _T_16[2:0]) begin // @[VectorDeserializer.scala 54:47]
        bits_4 <= _bits_T_23; // @[VectorDeserializer.scala 54:47]
      end else if (3'h4 == _T_12[2:0]) begin // @[VectorDeserializer.scala 54:47]
        bits_4 <= _bits_T_21; // @[VectorDeserializer.scala 54:47]
      end else begin
        bits_4 <= _GEN_14;
      end
    end
    if (reset) begin // @[VectorDeserializer.scala 41:24]
      bits_5 <= 16'sh0; // @[VectorDeserializer.scala 41:24]
    end else if (_T) begin // @[VectorDeserializer.scala 52:24]
      if (3'h5 == _T_16[2:0]) begin // @[VectorDeserializer.scala 54:47]
        bits_5 <= _bits_T_23; // @[VectorDeserializer.scala 54:47]
      end else if (3'h5 == _T_12[2:0]) begin // @[VectorDeserializer.scala 54:47]
        bits_5 <= _bits_T_21; // @[VectorDeserializer.scala 54:47]
      end else begin
        bits_5 <= _GEN_15;
      end
    end
    if (reset) begin // @[VectorDeserializer.scala 41:24]
      bits_6 <= 16'sh0; // @[VectorDeserializer.scala 41:24]
    end else if (_T) begin // @[VectorDeserializer.scala 52:24]
      if (3'h6 == _T_16[2:0]) begin // @[VectorDeserializer.scala 54:47]
        bits_6 <= _bits_T_23; // @[VectorDeserializer.scala 54:47]
      end else if (3'h6 == _T_12[2:0]) begin // @[VectorDeserializer.scala 54:47]
        bits_6 <= _bits_T_21; // @[VectorDeserializer.scala 54:47]
      end else begin
        bits_6 <= _GEN_16;
      end
    end
    if (reset) begin // @[VectorDeserializer.scala 41:24]
      bits_7 <= 16'sh0; // @[VectorDeserializer.scala 41:24]
    end else if (_T) begin // @[VectorDeserializer.scala 52:24]
      if (3'h7 == _T_16[2:0]) begin // @[VectorDeserializer.scala 54:47]
        bits_7 <= _bits_T_23; // @[VectorDeserializer.scala 54:47]
      end else if (3'h7 == _T_12[2:0]) begin // @[VectorDeserializer.scala 54:47]
        bits_7 <= _bits_T_21; // @[VectorDeserializer.scala 54:47]
      end else begin
        bits_7 <= _GEN_17;
      end
    end
    if (reset) begin // @[VectorDeserializer.scala 42:24]
      valid <= 1'h0; // @[VectorDeserializer.scala 42:24]
    end else if (_T_18) begin // @[VectorDeserializer.scala 63:25]
      valid <= 1'h0; // @[VectorDeserializer.scala 64:13]
    end else if (_T) begin // @[VectorDeserializer.scala 52:24]
      valid <= wrap; // @[VectorDeserializer.scala 61:13]
    end
    if (reset) begin // @[Counter.scala 60:40]
      ctr <= 1'h0; // @[Counter.scala 60:40]
    end else if (_T) begin // @[Counter.scala 118:17]
      ctr <= ctr + 1'h1; // @[Counter.scala 76:15]
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
  bits_0 = _RAND_0[15:0];
  _RAND_1 = {1{`RANDOM}};
  bits_1 = _RAND_1[15:0];
  _RAND_2 = {1{`RANDOM}};
  bits_2 = _RAND_2[15:0];
  _RAND_3 = {1{`RANDOM}};
  bits_3 = _RAND_3[15:0];
  _RAND_4 = {1{`RANDOM}};
  bits_4 = _RAND_4[15:0];
  _RAND_5 = {1{`RANDOM}};
  bits_5 = _RAND_5[15:0];
  _RAND_6 = {1{`RANDOM}};
  bits_6 = _RAND_6[15:0];
  _RAND_7 = {1{`RANDOM}};
  bits_7 = _RAND_7[15:0];
  _RAND_8 = {1{`RANDOM}};
  valid = _RAND_8[0:0];
  _RAND_9 = {1{`RANDOM}};
  ctr = _RAND_9[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Converter(
  input         clock,
  input         reset,
  output        io_mem_control_ready,
  input         io_mem_control_valid,
  input         io_mem_control_bits_write,
  input  [19:0] io_mem_control_bits_address,
  input  [19:0] io_mem_control_bits_size,
  input         io_mem_dataIn_ready,
  output        io_mem_dataIn_valid,
  output [15:0] io_mem_dataIn_bits_0,
  output [15:0] io_mem_dataIn_bits_1,
  output [15:0] io_mem_dataIn_bits_2,
  output [15:0] io_mem_dataIn_bits_3,
  output [15:0] io_mem_dataIn_bits_4,
  output [15:0] io_mem_dataIn_bits_5,
  output [15:0] io_mem_dataIn_bits_6,
  output [15:0] io_mem_dataIn_bits_7,
  output        io_mem_dataOut_ready,
  input         io_mem_dataOut_valid,
  input  [15:0] io_mem_dataOut_bits_0,
  input  [15:0] io_mem_dataOut_bits_1,
  input  [15:0] io_mem_dataOut_bits_2,
  input  [15:0] io_mem_dataOut_bits_3,
  input  [15:0] io_mem_dataOut_bits_4,
  input  [15:0] io_mem_dataOut_bits_5,
  input  [15:0] io_mem_dataOut_bits_6,
  input  [15:0] io_mem_dataOut_bits_7,
  input         io_axi_writeAddress_ready,
  output        io_axi_writeAddress_valid,
  output [31:0] io_axi_writeAddress_bits_addr,
  output [7:0]  io_axi_writeAddress_bits_len,
  output [3:0]  io_axi_writeAddress_bits_cache,
  input         io_axi_writeData_ready,
  output        io_axi_writeData_valid,
  output [63:0] io_axi_writeData_bits_data,
  output        io_axi_writeResponse_ready,
  input         io_axi_writeResponse_valid,
  input         io_axi_readAddress_ready,
  output        io_axi_readAddress_valid,
  output [31:0] io_axi_readAddress_bits_addr,
  output [7:0]  io_axi_readAddress_bits_len,
  output [3:0]  io_axi_readAddress_bits_cache,
  output        io_axi_readData_ready,
  input         io_axi_readData_valid,
  input  [63:0] io_axi_readData_bits_data,
  input         io_axi_readData_bits_last,
  input  [31:0] io_addressOffset,
  input  [3:0]  io_cacheBehavior,
  input         io_timeout,
  input         io_tracepoint,
  input  [31:0] io_programCounter
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
`endif // RANDOMIZE_REG_INIT
  wire  control_q_clock; // @[Converter.scala 66:25]
  wire  control_q_reset; // @[Converter.scala 66:25]
  wire  control_q_io_enq_ready; // @[Converter.scala 66:25]
  wire  control_q_io_enq_valid; // @[Converter.scala 66:25]
  wire  control_q_io_enq_bits_write; // @[Converter.scala 66:25]
  wire [19:0] control_q_io_enq_bits_address; // @[Converter.scala 66:25]
  wire [19:0] control_q_io_enq_bits_size; // @[Converter.scala 66:25]
  wire  control_q_io_deq_ready; // @[Converter.scala 66:25]
  wire  control_q_io_deq_valid; // @[Converter.scala 66:25]
  wire  control_q_io_deq_bits_write; // @[Converter.scala 66:25]
  wire [19:0] control_q_io_deq_bits_address; // @[Converter.scala 66:25]
  wire [19:0] control_q_io_deq_bits_size; // @[Converter.scala 66:25]
  wire  control_splitter_clock; // @[RequestSplitter.scala 69:26]
  wire  control_splitter_reset; // @[RequestSplitter.scala 69:26]
  wire  control_splitter_io_in_ready; // @[RequestSplitter.scala 69:26]
  wire  control_splitter_io_in_valid; // @[RequestSplitter.scala 69:26]
  wire  control_splitter_io_in_bits_write; // @[RequestSplitter.scala 69:26]
  wire [19:0] control_splitter_io_in_bits_address; // @[RequestSplitter.scala 69:26]
  wire [19:0] control_splitter_io_in_bits_size; // @[RequestSplitter.scala 69:26]
  wire  control_splitter_io_out_ready; // @[RequestSplitter.scala 69:26]
  wire  control_splitter_io_out_valid; // @[RequestSplitter.scala 69:26]
  wire  control_splitter_io_out_bits_write; // @[RequestSplitter.scala 69:26]
  wire [19:0] control_splitter_io_out_bits_address; // @[RequestSplitter.scala 69:26]
  wire [19:0] control_splitter_io_out_bits_size; // @[RequestSplitter.scala 69:26]
  wire  dataOut_clock; // @[Converter.scala 68:41]
  wire  dataOut_reset; // @[Converter.scala 68:41]
  wire  dataOut_io_enq_ready; // @[Converter.scala 68:41]
  wire  dataOut_io_enq_valid; // @[Converter.scala 68:41]
  wire [15:0] dataOut_io_enq_bits_0; // @[Converter.scala 68:41]
  wire [15:0] dataOut_io_enq_bits_1; // @[Converter.scala 68:41]
  wire [15:0] dataOut_io_enq_bits_2; // @[Converter.scala 68:41]
  wire [15:0] dataOut_io_enq_bits_3; // @[Converter.scala 68:41]
  wire [15:0] dataOut_io_enq_bits_4; // @[Converter.scala 68:41]
  wire [15:0] dataOut_io_enq_bits_5; // @[Converter.scala 68:41]
  wire [15:0] dataOut_io_enq_bits_6; // @[Converter.scala 68:41]
  wire [15:0] dataOut_io_enq_bits_7; // @[Converter.scala 68:41]
  wire  dataOut_io_deq_ready; // @[Converter.scala 68:41]
  wire  dataOut_io_deq_valid; // @[Converter.scala 68:41]
  wire [15:0] dataOut_io_deq_bits_0; // @[Converter.scala 68:41]
  wire [15:0] dataOut_io_deq_bits_1; // @[Converter.scala 68:41]
  wire [15:0] dataOut_io_deq_bits_2; // @[Converter.scala 68:41]
  wire [15:0] dataOut_io_deq_bits_3; // @[Converter.scala 68:41]
  wire [15:0] dataOut_io_deq_bits_4; // @[Converter.scala 68:41]
  wire [15:0] dataOut_io_deq_bits_5; // @[Converter.scala 68:41]
  wire [15:0] dataOut_io_deq_bits_6; // @[Converter.scala 68:41]
  wire [15:0] dataOut_io_deq_bits_7; // @[Converter.scala 68:41]
  wire  readData_clock; // @[Converter.scala 69:41]
  wire  readData_reset; // @[Converter.scala 69:41]
  wire  readData_io_enq_ready; // @[Converter.scala 69:41]
  wire  readData_io_enq_valid; // @[Converter.scala 69:41]
  wire [63:0] readData_io_enq_bits_data; // @[Converter.scala 69:41]
  wire  readData_io_enq_bits_last; // @[Converter.scala 69:41]
  wire  readData_io_deq_ready; // @[Converter.scala 69:41]
  wire  readData_io_deq_valid; // @[Converter.scala 69:41]
  wire [63:0] readData_io_deq_bits_data; // @[Converter.scala 69:41]
  wire  readData_io_deq_bits_last; // @[Converter.scala 69:41]
  wire  writeResponse_clock; // @[Converter.scala 70:41]
  wire  writeResponse_reset; // @[Converter.scala 70:41]
  wire  writeResponse_io_enq_ready; // @[Converter.scala 70:41]
  wire  writeResponse_io_enq_valid; // @[Converter.scala 70:41]
  wire  writeResponse_io_deq_ready; // @[Converter.scala 70:41]
  wire  writeResponse_io_deq_valid; // @[Converter.scala 70:41]
  wire  ser_clock; // @[Converter.scala 90:19]
  wire  ser_reset; // @[Converter.scala 90:19]
  wire  ser_io_in_ready; // @[Converter.scala 90:19]
  wire  ser_io_in_valid; // @[Converter.scala 90:19]
  wire [15:0] ser_io_in_bits_0; // @[Converter.scala 90:19]
  wire [15:0] ser_io_in_bits_1; // @[Converter.scala 90:19]
  wire [15:0] ser_io_in_bits_2; // @[Converter.scala 90:19]
  wire [15:0] ser_io_in_bits_3; // @[Converter.scala 90:19]
  wire [15:0] ser_io_in_bits_4; // @[Converter.scala 90:19]
  wire [15:0] ser_io_in_bits_5; // @[Converter.scala 90:19]
  wire [15:0] ser_io_in_bits_6; // @[Converter.scala 90:19]
  wire [15:0] ser_io_in_bits_7; // @[Converter.scala 90:19]
  wire  ser_io_out_ready; // @[Converter.scala 90:19]
  wire  ser_io_out_valid; // @[Converter.scala 90:19]
  wire [63:0] ser_io_out_bits; // @[Converter.scala 90:19]
  wire  serCounter_clock; // @[Converter.scala 100:26]
  wire  serCounter_reset; // @[Converter.scala 100:26]
  wire  serCounter_io_in_ready; // @[Converter.scala 100:26]
  wire  serCounter_io_in_valid; // @[Converter.scala 100:26]
  wire [63:0] serCounter_io_in_bits; // @[Converter.scala 100:26]
  wire  serCounter_io_out_ready; // @[Converter.scala 100:26]
  wire  serCounter_io_out_valid; // @[Converter.scala 100:26]
  wire [63:0] serCounter_io_out_bits; // @[Converter.scala 100:26]
  wire  serCounter_io_len_ready; // @[Converter.scala 100:26]
  wire  serCounter_io_len_valid; // @[Converter.scala 100:26]
  wire [7:0] serCounter_io_len_bits; // @[Converter.scala 100:26]
  wire  des_clock; // @[Converter.scala 109:19]
  wire  des_reset; // @[Converter.scala 109:19]
  wire  des_io_in_ready; // @[Converter.scala 109:19]
  wire  des_io_in_valid; // @[Converter.scala 109:19]
  wire [63:0] des_io_in_bits; // @[Converter.scala 109:19]
  wire  des_io_out_ready; // @[Converter.scala 109:19]
  wire  des_io_out_valid; // @[Converter.scala 109:19]
  wire [15:0] des_io_out_bits_0; // @[Converter.scala 109:19]
  wire [15:0] des_io_out_bits_1; // @[Converter.scala 109:19]
  wire [15:0] des_io_out_bits_2; // @[Converter.scala 109:19]
  wire [15:0] des_io_out_bits_3; // @[Converter.scala 109:19]
  wire [15:0] des_io_out_bits_4; // @[Converter.scala 109:19]
  wire [15:0] des_io_out_bits_5; // @[Converter.scala 109:19]
  wire [15:0] des_io_out_bits_6; // @[Converter.scala 109:19]
  wire [15:0] des_io_out_bits_7; // @[Converter.scala 109:19]
  wire  writeEnqueue_clock; // @[MultiEnqueue.scala 160:43]
  wire  writeEnqueue_reset; // @[MultiEnqueue.scala 160:43]
  wire  writeEnqueue_io_in_ready; // @[MultiEnqueue.scala 160:43]
  wire  writeEnqueue_io_in_valid; // @[MultiEnqueue.scala 160:43]
  wire  writeEnqueue_io_out_0_ready; // @[MultiEnqueue.scala 160:43]
  wire  writeEnqueue_io_out_0_valid; // @[MultiEnqueue.scala 160:43]
  wire  writeEnqueue_io_out_1_ready; // @[MultiEnqueue.scala 160:43]
  wire  writeEnqueue_io_out_1_valid; // @[MultiEnqueue.scala 160:43]
  wire [24:0] address = control_splitter_io_out_bits_address * 5'h10; // @[Converter.scala 79:26]
  wire [19:0] _size_T_1 = control_splitter_io_out_bits_size + 20'h1; // @[Converter.scala 82:24]
  wire [21:0] _size_T_2 = _size_T_1 * 2'h2; // @[Converter.scala 82:31]
  wire [21:0] size = _size_T_2 - 22'h1; // @[Converter.scala 82:68]
  reg [7:0] writeResponseCount; // @[Converter.scala 124:35]
  reg [7:0] readResponseCount; // @[Converter.scala 125:35]
  wire  _canWrite_T_1 = writeResponseCount < 8'hff; // @[Converter.scala 127:56]
  wire  canWrite = readResponseCount == 8'h0 & writeResponseCount < 8'hff; // @[Converter.scala 127:33]
  wire  _canRead_T_1 = readResponseCount < 8'hff; // @[Converter.scala 129:56]
  wire  canRead = writeResponseCount == 8'h0 & readResponseCount < 8'hff; // @[Converter.scala 129:34]
  wire  writeRequested = io_axi_writeAddress_ready & io_axi_writeAddress_valid; // @[Converter.scala 130:50]
  wire  writeResponded = writeResponse_io_deq_ready & writeResponse_io_deq_valid; // @[Converter.scala 131:44]
  wire  readRequested = io_axi_readAddress_ready & io_axi_readAddress_valid; // @[Converter.scala 132:49]
  wire  readResponded = readData_io_deq_ready & readData_io_deq_valid & readData_io_deq_bits_last; // @[Converter.scala 133:57]
  wire [7:0] _writeResponseCount_T_1 = writeResponseCount + 8'h1; // @[Converter.scala 139:50]
  wire [7:0] _writeResponseCount_T_3 = writeResponseCount - 8'h1; // @[Converter.scala 144:48]
  wire [7:0] _readResponseCount_T_1 = readResponseCount + 8'h1; // @[Converter.scala 154:48]
  wire [7:0] _readResponseCount_T_3 = readResponseCount - 8'h1; // @[Converter.scala 159:46]
  wire [31:0] _GEN_15 = {{7'd0}, address}; // @[Converter.scala 169:15]
  Queue_34 control_q ( // @[Converter.scala 66:25]
    .clock(control_q_clock),
    .reset(control_q_reset),
    .io_enq_ready(control_q_io_enq_ready),
    .io_enq_valid(control_q_io_enq_valid),
    .io_enq_bits_write(control_q_io_enq_bits_write),
    .io_enq_bits_address(control_q_io_enq_bits_address),
    .io_enq_bits_size(control_q_io_enq_bits_size),
    .io_deq_ready(control_q_io_deq_ready),
    .io_deq_valid(control_q_io_deq_valid),
    .io_deq_bits_write(control_q_io_deq_bits_write),
    .io_deq_bits_address(control_q_io_deq_bits_address),
    .io_deq_bits_size(control_q_io_deq_bits_size)
  );
  RequestSplitter control_splitter ( // @[RequestSplitter.scala 69:26]
    .clock(control_splitter_clock),
    .reset(control_splitter_reset),
    .io_in_ready(control_splitter_io_in_ready),
    .io_in_valid(control_splitter_io_in_valid),
    .io_in_bits_write(control_splitter_io_in_bits_write),
    .io_in_bits_address(control_splitter_io_in_bits_address),
    .io_in_bits_size(control_splitter_io_in_bits_size),
    .io_out_ready(control_splitter_io_out_ready),
    .io_out_valid(control_splitter_io_out_valid),
    .io_out_bits_write(control_splitter_io_out_bits_write),
    .io_out_bits_address(control_splitter_io_out_bits_address),
    .io_out_bits_size(control_splitter_io_out_bits_size)
  );
  Queue_4 dataOut ( // @[Converter.scala 68:41]
    .clock(dataOut_clock),
    .reset(dataOut_reset),
    .io_enq_ready(dataOut_io_enq_ready),
    .io_enq_valid(dataOut_io_enq_valid),
    .io_enq_bits_0(dataOut_io_enq_bits_0),
    .io_enq_bits_1(dataOut_io_enq_bits_1),
    .io_enq_bits_2(dataOut_io_enq_bits_2),
    .io_enq_bits_3(dataOut_io_enq_bits_3),
    .io_enq_bits_4(dataOut_io_enq_bits_4),
    .io_enq_bits_5(dataOut_io_enq_bits_5),
    .io_enq_bits_6(dataOut_io_enq_bits_6),
    .io_enq_bits_7(dataOut_io_enq_bits_7),
    .io_deq_ready(dataOut_io_deq_ready),
    .io_deq_valid(dataOut_io_deq_valid),
    .io_deq_bits_0(dataOut_io_deq_bits_0),
    .io_deq_bits_1(dataOut_io_deq_bits_1),
    .io_deq_bits_2(dataOut_io_deq_bits_2),
    .io_deq_bits_3(dataOut_io_deq_bits_3),
    .io_deq_bits_4(dataOut_io_deq_bits_4),
    .io_deq_bits_5(dataOut_io_deq_bits_5),
    .io_deq_bits_6(dataOut_io_deq_bits_6),
    .io_deq_bits_7(dataOut_io_deq_bits_7)
  );
  Queue_36 readData ( // @[Converter.scala 69:41]
    .clock(readData_clock),
    .reset(readData_reset),
    .io_enq_ready(readData_io_enq_ready),
    .io_enq_valid(readData_io_enq_valid),
    .io_enq_bits_data(readData_io_enq_bits_data),
    .io_enq_bits_last(readData_io_enq_bits_last),
    .io_deq_ready(readData_io_deq_ready),
    .io_deq_valid(readData_io_deq_valid),
    .io_deq_bits_data(readData_io_deq_bits_data),
    .io_deq_bits_last(readData_io_deq_bits_last)
  );
  Queue_37 writeResponse ( // @[Converter.scala 70:41]
    .clock(writeResponse_clock),
    .reset(writeResponse_reset),
    .io_enq_ready(writeResponse_io_enq_ready),
    .io_enq_valid(writeResponse_io_enq_valid),
    .io_deq_ready(writeResponse_io_deq_ready),
    .io_deq_valid(writeResponse_io_deq_valid)
  );
  VectorSerializer ser ( // @[Converter.scala 90:19]
    .clock(ser_clock),
    .reset(ser_reset),
    .io_in_ready(ser_io_in_ready),
    .io_in_valid(ser_io_in_valid),
    .io_in_bits_0(ser_io_in_bits_0),
    .io_in_bits_1(ser_io_in_bits_1),
    .io_in_bits_2(ser_io_in_bits_2),
    .io_in_bits_3(ser_io_in_bits_3),
    .io_in_bits_4(ser_io_in_bits_4),
    .io_in_bits_5(ser_io_in_bits_5),
    .io_in_bits_6(ser_io_in_bits_6),
    .io_in_bits_7(ser_io_in_bits_7),
    .io_out_ready(ser_io_out_ready),
    .io_out_valid(ser_io_out_valid),
    .io_out_bits(ser_io_out_bits)
  );
  DataCounter serCounter ( // @[Converter.scala 100:26]
    .clock(serCounter_clock),
    .reset(serCounter_reset),
    .io_in_ready(serCounter_io_in_ready),
    .io_in_valid(serCounter_io_in_valid),
    .io_in_bits(serCounter_io_in_bits),
    .io_out_ready(serCounter_io_out_ready),
    .io_out_valid(serCounter_io_out_valid),
    .io_out_bits(serCounter_io_out_bits),
    .io_len_ready(serCounter_io_len_ready),
    .io_len_valid(serCounter_io_len_valid),
    .io_len_bits(serCounter_io_len_bits)
  );
  VectorDeserializer des ( // @[Converter.scala 109:19]
    .clock(des_clock),
    .reset(des_reset),
    .io_in_ready(des_io_in_ready),
    .io_in_valid(des_io_in_valid),
    .io_in_bits(des_io_in_bits),
    .io_out_ready(des_io_out_ready),
    .io_out_valid(des_io_out_valid),
    .io_out_bits_0(des_io_out_bits_0),
    .io_out_bits_1(des_io_out_bits_1),
    .io_out_bits_2(des_io_out_bits_2),
    .io_out_bits_3(des_io_out_bits_3),
    .io_out_bits_4(des_io_out_bits_4),
    .io_out_bits_5(des_io_out_bits_5),
    .io_out_bits_6(des_io_out_bits_6),
    .io_out_bits_7(des_io_out_bits_7)
  );
  MultiEnqueue_1 writeEnqueue ( // @[MultiEnqueue.scala 160:43]
    .clock(writeEnqueue_clock),
    .reset(writeEnqueue_reset),
    .io_in_ready(writeEnqueue_io_in_ready),
    .io_in_valid(writeEnqueue_io_in_valid),
    .io_out_0_ready(writeEnqueue_io_out_0_ready),
    .io_out_0_valid(writeEnqueue_io_out_0_valid),
    .io_out_1_ready(writeEnqueue_io_out_1_ready),
    .io_out_1_valid(writeEnqueue_io_out_1_valid)
  );
  assign io_mem_control_ready = control_q_io_enq_ready; // @[Converter.scala 66:25]
  assign io_mem_dataIn_valid = des_io_out_valid; // @[Converter.scala 117:17]
  assign io_mem_dataIn_bits_0 = des_io_out_bits_0; // @[Converter.scala 117:17]
  assign io_mem_dataIn_bits_1 = des_io_out_bits_1; // @[Converter.scala 117:17]
  assign io_mem_dataIn_bits_2 = des_io_out_bits_2; // @[Converter.scala 117:17]
  assign io_mem_dataIn_bits_3 = des_io_out_bits_3; // @[Converter.scala 117:17]
  assign io_mem_dataIn_bits_4 = des_io_out_bits_4; // @[Converter.scala 117:17]
  assign io_mem_dataIn_bits_5 = des_io_out_bits_5; // @[Converter.scala 117:17]
  assign io_mem_dataIn_bits_6 = des_io_out_bits_6; // @[Converter.scala 117:17]
  assign io_mem_dataIn_bits_7 = des_io_out_bits_7; // @[Converter.scala 117:17]
  assign io_mem_dataOut_ready = dataOut_io_enq_ready; // @[Converter.scala 68:41]
  assign io_axi_writeAddress_valid = control_splitter_io_out_bits_write & writeEnqueue_io_out_0_valid; // @[Converter.scala 208:28 Converter.scala 213:31 Converter.scala 173:29]
  assign io_axi_writeAddress_bits_addr = _GEN_15 + io_addressOffset; // @[Converter.scala 169:15]
  assign io_axi_writeAddress_bits_len = size[7:0]; // @[Address.scala 28:9]
  assign io_axi_writeAddress_bits_cache = io_cacheBehavior; // @[Address.scala 30:34]
  assign io_axi_writeData_valid = serCounter_io_out_valid; // @[Converter.scala 180:26]
  assign io_axi_writeData_bits_data = serCounter_io_out_bits; // @[WriteData.scala 20:15]
  assign io_axi_writeResponse_ready = writeResponse_io_enq_ready; // @[Converter.scala 70:41]
  assign io_axi_readAddress_valid = control_splitter_io_out_bits_write ? 1'h0 : control_splitter_io_out_valid & canRead; // @[Converter.scala 208:28 Converter.scala 195:28 Converter.scala 217:30]
  assign io_axi_readAddress_bits_addr = _GEN_15 + io_addressOffset; // @[Converter.scala 191:15]
  assign io_axi_readAddress_bits_len = size[7:0]; // @[Address.scala 28:9]
  assign io_axi_readAddress_bits_cache = io_cacheBehavior; // @[Address.scala 30:34]
  assign io_axi_readData_ready = readData_io_enq_ready; // @[Converter.scala 69:41]
  assign control_q_clock = clock;
  assign control_q_reset = reset;
  assign control_q_io_enq_valid = io_mem_control_valid; // @[Converter.scala 66:25]
  assign control_q_io_enq_bits_write = io_mem_control_bits_write; // @[Converter.scala 66:25]
  assign control_q_io_enq_bits_address = io_mem_control_bits_address; // @[Converter.scala 66:25]
  assign control_q_io_enq_bits_size = io_mem_control_bits_size; // @[Converter.scala 66:25]
  assign control_q_io_deq_ready = control_splitter_io_in_ready; // @[RequestSplitter.scala 70:20]
  assign control_splitter_clock = clock;
  assign control_splitter_reset = reset;
  assign control_splitter_io_in_valid = control_q_io_deq_valid; // @[RequestSplitter.scala 70:20]
  assign control_splitter_io_in_bits_write = control_q_io_deq_bits_write; // @[RequestSplitter.scala 70:20]
  assign control_splitter_io_in_bits_address = control_q_io_deq_bits_address; // @[RequestSplitter.scala 70:20]
  assign control_splitter_io_in_bits_size = control_q_io_deq_bits_size; // @[RequestSplitter.scala 70:20]
  assign control_splitter_io_out_ready = control_splitter_io_out_bits_write ? writeEnqueue_io_in_ready :
    io_axi_readAddress_ready & canRead; // @[Converter.scala 208:28 Converter.scala 210:19 Converter.scala 218:19]
  assign dataOut_clock = clock;
  assign dataOut_reset = reset;
  assign dataOut_io_enq_valid = io_mem_dataOut_valid; // @[Converter.scala 68:41]
  assign dataOut_io_enq_bits_0 = io_mem_dataOut_bits_0; // @[Converter.scala 68:41]
  assign dataOut_io_enq_bits_1 = io_mem_dataOut_bits_1; // @[Converter.scala 68:41]
  assign dataOut_io_enq_bits_2 = io_mem_dataOut_bits_2; // @[Converter.scala 68:41]
  assign dataOut_io_enq_bits_3 = io_mem_dataOut_bits_3; // @[Converter.scala 68:41]
  assign dataOut_io_enq_bits_4 = io_mem_dataOut_bits_4; // @[Converter.scala 68:41]
  assign dataOut_io_enq_bits_5 = io_mem_dataOut_bits_5; // @[Converter.scala 68:41]
  assign dataOut_io_enq_bits_6 = io_mem_dataOut_bits_6; // @[Converter.scala 68:41]
  assign dataOut_io_enq_bits_7 = io_mem_dataOut_bits_7; // @[Converter.scala 68:41]
  assign dataOut_io_deq_ready = ser_io_in_ready; // @[Converter.scala 98:13]
  assign readData_clock = clock;
  assign readData_reset = reset;
  assign readData_io_enq_valid = io_axi_readData_valid; // @[Converter.scala 69:41]
  assign readData_io_enq_bits_data = io_axi_readData_bits_data; // @[Converter.scala 69:41]
  assign readData_io_enq_bits_last = io_axi_readData_bits_last; // @[Converter.scala 69:41]
  assign readData_io_deq_ready = des_io_in_ready; // @[Converter.scala 202:18]
  assign writeResponse_clock = clock;
  assign writeResponse_reset = reset;
  assign writeResponse_io_enq_valid = io_axi_writeResponse_valid; // @[Converter.scala 70:41]
  assign writeResponse_io_deq_ready = 1'h1; // @[Converter.scala 185:23]
  assign ser_clock = clock;
  assign ser_reset = reset;
  assign ser_io_in_valid = dataOut_io_deq_valid; // @[Converter.scala 98:13]
  assign ser_io_in_bits_0 = dataOut_io_deq_bits_0; // @[Converter.scala 98:13]
  assign ser_io_in_bits_1 = dataOut_io_deq_bits_1; // @[Converter.scala 98:13]
  assign ser_io_in_bits_2 = dataOut_io_deq_bits_2; // @[Converter.scala 98:13]
  assign ser_io_in_bits_3 = dataOut_io_deq_bits_3; // @[Converter.scala 98:13]
  assign ser_io_in_bits_4 = dataOut_io_deq_bits_4; // @[Converter.scala 98:13]
  assign ser_io_in_bits_5 = dataOut_io_deq_bits_5; // @[Converter.scala 98:13]
  assign ser_io_in_bits_6 = dataOut_io_deq_bits_6; // @[Converter.scala 98:13]
  assign ser_io_in_bits_7 = dataOut_io_deq_bits_7; // @[Converter.scala 98:13]
  assign ser_io_out_ready = serCounter_io_in_ready; // @[Converter.scala 103:20]
  assign serCounter_clock = clock;
  assign serCounter_reset = reset;
  assign serCounter_io_in_valid = ser_io_out_valid; // @[Converter.scala 103:20]
  assign serCounter_io_in_bits = ser_io_out_bits; // @[Converter.scala 103:20]
  assign serCounter_io_out_ready = io_axi_writeData_ready; // @[Converter.scala 181:16]
  assign serCounter_io_len_valid = control_splitter_io_out_bits_write & writeEnqueue_io_out_1_valid; // @[Converter.scala 208:28 Converter.scala 215:29 Converter.scala 105:27]
  assign serCounter_io_len_bits = size[7:0]; // @[Converter.scala 104:26]
  assign des_clock = clock;
  assign des_reset = reset;
  assign des_io_in_valid = readData_io_deq_valid; // @[Converter.scala 201:19]
  assign des_io_in_bits = readData_io_deq_bits_data; // @[Converter.scala 200:18]
  assign des_io_out_ready = io_mem_dataIn_ready; // @[Converter.scala 117:17]
  assign writeEnqueue_clock = clock;
  assign writeEnqueue_reset = reset;
  assign writeEnqueue_io_in_valid = control_splitter_io_out_bits_write & (control_splitter_io_out_valid & canWrite); // @[Converter.scala 208:28 Converter.scala 209:30 MultiEnqueue.scala 40:17]
  assign writeEnqueue_io_out_0_ready = control_splitter_io_out_bits_write & io_axi_writeAddress_ready; // @[Converter.scala 208:28 Converter.scala 212:34 MultiEnqueue.scala 42:18]
  assign writeEnqueue_io_out_1_ready = control_splitter_io_out_bits_write & serCounter_io_len_ready; // @[Converter.scala 208:28 Converter.scala 214:34 MultiEnqueue.scala 42:18]
  always @(posedge clock) begin
    if (reset) begin // @[Converter.scala 124:35]
      writeResponseCount <= 8'h0; // @[Converter.scala 124:35]
    end else if (writeRequested) begin // @[Converter.scala 134:24]
      if (!(writeResponded)) begin // @[Converter.scala 135:26]
        if (_canWrite_T_1) begin // @[Converter.scala 138:62]
          writeResponseCount <= _writeResponseCount_T_1; // @[Converter.scala 139:28]
        end
      end
    end else if (writeResponded & writeResponseCount > 8'h0) begin // @[Converter.scala 143:54]
      writeResponseCount <= _writeResponseCount_T_3; // @[Converter.scala 144:26]
    end
    if (reset) begin // @[Converter.scala 125:35]
      readResponseCount <= 8'h0; // @[Converter.scala 125:35]
    end else if (readRequested) begin // @[Converter.scala 149:23]
      if (!(readResponded)) begin // @[Converter.scala 150:25]
        if (_canRead_T_1) begin // @[Converter.scala 153:61]
          readResponseCount <= _readResponseCount_T_1; // @[Converter.scala 154:27]
        end
      end
    end else if (readResponded & readResponseCount > 8'h0) begin // @[Converter.scala 158:52]
      readResponseCount <= _readResponseCount_T_3; // @[Converter.scala 159:25]
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
  writeResponseCount = _RAND_0[7:0];
  _RAND_1 = {1{`RANDOM}};
  readResponseCount = _RAND_1[7:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Queue_38(
  input         clock,
  input         reset,
  output        io_enq_ready,
  input         io_enq_valid,
  input  [31:0] io_enq_bits_addr,
  input  [7:0]  io_enq_bits_len,
  input  [3:0]  io_enq_bits_cache,
  input         io_deq_ready,
  output        io_deq_valid,
  output [5:0]  io_deq_bits_id,
  output [31:0] io_deq_bits_addr,
  output [7:0]  io_deq_bits_len,
  output [2:0]  io_deq_bits_size,
  output [1:0]  io_deq_bits_burst,
  output [1:0]  io_deq_bits_lock,
  output [3:0]  io_deq_bits_cache,
  output [2:0]  io_deq_bits_prot,
  output [3:0]  io_deq_bits_qos
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_6;
  reg [31:0] _RAND_7;
  reg [31:0] _RAND_8;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_9;
  reg [31:0] _RAND_10;
  reg [31:0] _RAND_11;
`endif // RANDOMIZE_REG_INIT
  reg [5:0] ram_id [0:1]; // @[Decoupled.scala 218:16]
  wire [5:0] ram_id_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_id_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [5:0] ram_id_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_id_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_id_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_id_MPORT_en; // @[Decoupled.scala 218:16]
  reg [31:0] ram_addr [0:1]; // @[Decoupled.scala 218:16]
  wire [31:0] ram_addr_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_addr_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [31:0] ram_addr_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_addr_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_addr_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_addr_MPORT_en; // @[Decoupled.scala 218:16]
  reg [7:0] ram_len [0:1]; // @[Decoupled.scala 218:16]
  wire [7:0] ram_len_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_len_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [7:0] ram_len_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_len_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_len_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_len_MPORT_en; // @[Decoupled.scala 218:16]
  reg [2:0] ram_size [0:1]; // @[Decoupled.scala 218:16]
  wire [2:0] ram_size_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_size_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [2:0] ram_size_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_size_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_size_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_size_MPORT_en; // @[Decoupled.scala 218:16]
  reg [1:0] ram_burst [0:1]; // @[Decoupled.scala 218:16]
  wire [1:0] ram_burst_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_burst_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [1:0] ram_burst_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_burst_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_burst_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_burst_MPORT_en; // @[Decoupled.scala 218:16]
  reg [1:0] ram_lock [0:1]; // @[Decoupled.scala 218:16]
  wire [1:0] ram_lock_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_lock_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [1:0] ram_lock_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_lock_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_lock_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_lock_MPORT_en; // @[Decoupled.scala 218:16]
  reg [3:0] ram_cache [0:1]; // @[Decoupled.scala 218:16]
  wire [3:0] ram_cache_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_cache_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [3:0] ram_cache_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_cache_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_cache_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_cache_MPORT_en; // @[Decoupled.scala 218:16]
  reg [2:0] ram_prot [0:1]; // @[Decoupled.scala 218:16]
  wire [2:0] ram_prot_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_prot_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [2:0] ram_prot_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_prot_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_prot_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_prot_MPORT_en; // @[Decoupled.scala 218:16]
  reg [3:0] ram_qos [0:1]; // @[Decoupled.scala 218:16]
  wire [3:0] ram_qos_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_qos_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [3:0] ram_qos_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_qos_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_qos_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_qos_MPORT_en; // @[Decoupled.scala 218:16]
  reg  value; // @[Counter.scala 60:40]
  reg  value_1; // @[Counter.scala 60:40]
  reg  maybe_full; // @[Decoupled.scala 221:27]
  wire  ptr_match = value == value_1; // @[Decoupled.scala 223:33]
  wire  empty = ptr_match & ~maybe_full; // @[Decoupled.scala 224:25]
  wire  full = ptr_match & maybe_full; // @[Decoupled.scala 225:24]
  wire  do_enq = io_enq_ready & io_enq_valid; // @[Decoupled.scala 40:37]
  wire  do_deq = io_deq_ready & io_deq_valid; // @[Decoupled.scala 40:37]
  assign ram_id_io_deq_bits_MPORT_addr = value_1;
  assign ram_id_io_deq_bits_MPORT_data = ram_id[ram_id_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_id_MPORT_data = 6'h0;
  assign ram_id_MPORT_addr = value;
  assign ram_id_MPORT_mask = 1'h1;
  assign ram_id_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_addr_io_deq_bits_MPORT_addr = value_1;
  assign ram_addr_io_deq_bits_MPORT_data = ram_addr[ram_addr_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_addr_MPORT_data = io_enq_bits_addr;
  assign ram_addr_MPORT_addr = value;
  assign ram_addr_MPORT_mask = 1'h1;
  assign ram_addr_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_len_io_deq_bits_MPORT_addr = value_1;
  assign ram_len_io_deq_bits_MPORT_data = ram_len[ram_len_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_len_MPORT_data = io_enq_bits_len;
  assign ram_len_MPORT_addr = value;
  assign ram_len_MPORT_mask = 1'h1;
  assign ram_len_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_size_io_deq_bits_MPORT_addr = value_1;
  assign ram_size_io_deq_bits_MPORT_data = ram_size[ram_size_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_size_MPORT_data = 3'h3;
  assign ram_size_MPORT_addr = value;
  assign ram_size_MPORT_mask = 1'h1;
  assign ram_size_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_burst_io_deq_bits_MPORT_addr = value_1;
  assign ram_burst_io_deq_bits_MPORT_data = ram_burst[ram_burst_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_burst_MPORT_data = 2'h1;
  assign ram_burst_MPORT_addr = value;
  assign ram_burst_MPORT_mask = 1'h1;
  assign ram_burst_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_lock_io_deq_bits_MPORT_addr = value_1;
  assign ram_lock_io_deq_bits_MPORT_data = ram_lock[ram_lock_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_lock_MPORT_data = 2'h0;
  assign ram_lock_MPORT_addr = value;
  assign ram_lock_MPORT_mask = 1'h1;
  assign ram_lock_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_cache_io_deq_bits_MPORT_addr = value_1;
  assign ram_cache_io_deq_bits_MPORT_data = ram_cache[ram_cache_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_cache_MPORT_data = io_enq_bits_cache;
  assign ram_cache_MPORT_addr = value;
  assign ram_cache_MPORT_mask = 1'h1;
  assign ram_cache_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_prot_io_deq_bits_MPORT_addr = value_1;
  assign ram_prot_io_deq_bits_MPORT_data = ram_prot[ram_prot_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_prot_MPORT_data = 3'h0;
  assign ram_prot_MPORT_addr = value;
  assign ram_prot_MPORT_mask = 1'h1;
  assign ram_prot_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_qos_io_deq_bits_MPORT_addr = value_1;
  assign ram_qos_io_deq_bits_MPORT_data = ram_qos[ram_qos_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_qos_MPORT_data = 4'h0;
  assign ram_qos_MPORT_addr = value;
  assign ram_qos_MPORT_mask = 1'h1;
  assign ram_qos_MPORT_en = io_enq_ready & io_enq_valid;
  assign io_enq_ready = ~full; // @[Decoupled.scala 241:19]
  assign io_deq_valid = ~empty; // @[Decoupled.scala 240:19]
  assign io_deq_bits_id = ram_id_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_addr = ram_addr_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_len = ram_len_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_size = ram_size_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_burst = ram_burst_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_lock = ram_lock_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_cache = ram_cache_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_prot = ram_prot_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_qos = ram_qos_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  always @(posedge clock) begin
    if(ram_id_MPORT_en & ram_id_MPORT_mask) begin
      ram_id[ram_id_MPORT_addr] <= ram_id_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_addr_MPORT_en & ram_addr_MPORT_mask) begin
      ram_addr[ram_addr_MPORT_addr] <= ram_addr_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_len_MPORT_en & ram_len_MPORT_mask) begin
      ram_len[ram_len_MPORT_addr] <= ram_len_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_size_MPORT_en & ram_size_MPORT_mask) begin
      ram_size[ram_size_MPORT_addr] <= ram_size_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_burst_MPORT_en & ram_burst_MPORT_mask) begin
      ram_burst[ram_burst_MPORT_addr] <= ram_burst_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_lock_MPORT_en & ram_lock_MPORT_mask) begin
      ram_lock[ram_lock_MPORT_addr] <= ram_lock_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_cache_MPORT_en & ram_cache_MPORT_mask) begin
      ram_cache[ram_cache_MPORT_addr] <= ram_cache_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_prot_MPORT_en & ram_prot_MPORT_mask) begin
      ram_prot[ram_prot_MPORT_addr] <= ram_prot_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_qos_MPORT_en & ram_qos_MPORT_mask) begin
      ram_qos[ram_qos_MPORT_addr] <= ram_qos_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if (reset) begin // @[Counter.scala 60:40]
      value <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_enq) begin // @[Decoupled.scala 229:17]
      value <= value + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Counter.scala 60:40]
      value_1 <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_deq) begin // @[Decoupled.scala 233:17]
      value_1 <= value_1 + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Decoupled.scala 221:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 221:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 236:28]
      maybe_full <= do_enq; // @[Decoupled.scala 237:16]
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
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_id[initvar] = _RAND_0[5:0];
  _RAND_1 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_addr[initvar] = _RAND_1[31:0];
  _RAND_2 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_len[initvar] = _RAND_2[7:0];
  _RAND_3 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_size[initvar] = _RAND_3[2:0];
  _RAND_4 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_burst[initvar] = _RAND_4[1:0];
  _RAND_5 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_lock[initvar] = _RAND_5[1:0];
  _RAND_6 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_cache[initvar] = _RAND_6[3:0];
  _RAND_7 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_prot[initvar] = _RAND_7[2:0];
  _RAND_8 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_qos[initvar] = _RAND_8[3:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_9 = {1{`RANDOM}};
  value = _RAND_9[0:0];
  _RAND_10 = {1{`RANDOM}};
  value_1 = _RAND_10[0:0];
  _RAND_11 = {1{`RANDOM}};
  maybe_full = _RAND_11[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Queue_40(
  input         clock,
  input         reset,
  output        io_enq_ready,
  input         io_enq_valid,
  input  [63:0] io_enq_bits_data,
  input         io_deq_ready,
  output        io_deq_valid,
  output [5:0]  io_deq_bits_id,
  output [63:0] io_deq_bits_data,
  output [7:0]  io_deq_bits_strb
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
  reg [63:0] _RAND_1;
  reg [31:0] _RAND_2;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
`endif // RANDOMIZE_REG_INIT
  reg [5:0] ram_id [0:1]; // @[Decoupled.scala 218:16]
  wire [5:0] ram_id_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_id_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [5:0] ram_id_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_id_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_id_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_id_MPORT_en; // @[Decoupled.scala 218:16]
  reg [63:0] ram_data [0:1]; // @[Decoupled.scala 218:16]
  wire [63:0] ram_data_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_data_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [63:0] ram_data_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_data_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_data_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_data_MPORT_en; // @[Decoupled.scala 218:16]
  reg [7:0] ram_strb [0:1]; // @[Decoupled.scala 218:16]
  wire [7:0] ram_strb_io_deq_bits_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_strb_io_deq_bits_MPORT_addr; // @[Decoupled.scala 218:16]
  wire [7:0] ram_strb_MPORT_data; // @[Decoupled.scala 218:16]
  wire  ram_strb_MPORT_addr; // @[Decoupled.scala 218:16]
  wire  ram_strb_MPORT_mask; // @[Decoupled.scala 218:16]
  wire  ram_strb_MPORT_en; // @[Decoupled.scala 218:16]
  reg  value; // @[Counter.scala 60:40]
  reg  value_1; // @[Counter.scala 60:40]
  reg  maybe_full; // @[Decoupled.scala 221:27]
  wire  ptr_match = value == value_1; // @[Decoupled.scala 223:33]
  wire  empty = ptr_match & ~maybe_full; // @[Decoupled.scala 224:25]
  wire  full = ptr_match & maybe_full; // @[Decoupled.scala 225:24]
  wire  do_enq = io_enq_ready & io_enq_valid; // @[Decoupled.scala 40:37]
  wire  do_deq = io_deq_ready & io_deq_valid; // @[Decoupled.scala 40:37]
  assign ram_id_io_deq_bits_MPORT_addr = value_1;
  assign ram_id_io_deq_bits_MPORT_data = ram_id[ram_id_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_id_MPORT_data = 6'h0;
  assign ram_id_MPORT_addr = value;
  assign ram_id_MPORT_mask = 1'h1;
  assign ram_id_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_data_io_deq_bits_MPORT_addr = value_1;
  assign ram_data_io_deq_bits_MPORT_data = ram_data[ram_data_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_data_MPORT_data = io_enq_bits_data;
  assign ram_data_MPORT_addr = value;
  assign ram_data_MPORT_mask = 1'h1;
  assign ram_data_MPORT_en = io_enq_ready & io_enq_valid;
  assign ram_strb_io_deq_bits_MPORT_addr = value_1;
  assign ram_strb_io_deq_bits_MPORT_data = ram_strb[ram_strb_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 218:16]
  assign ram_strb_MPORT_data = 8'hff;
  assign ram_strb_MPORT_addr = value;
  assign ram_strb_MPORT_mask = 1'h1;
  assign ram_strb_MPORT_en = io_enq_ready & io_enq_valid;
  assign io_enq_ready = ~full; // @[Decoupled.scala 241:19]
  assign io_deq_valid = ~empty; // @[Decoupled.scala 240:19]
  assign io_deq_bits_id = ram_id_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_data = ram_data_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  assign io_deq_bits_strb = ram_strb_io_deq_bits_MPORT_data; // @[Decoupled.scala 242:15]
  always @(posedge clock) begin
    if(ram_id_MPORT_en & ram_id_MPORT_mask) begin
      ram_id[ram_id_MPORT_addr] <= ram_id_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_data_MPORT_en & ram_data_MPORT_mask) begin
      ram_data[ram_data_MPORT_addr] <= ram_data_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if(ram_strb_MPORT_en & ram_strb_MPORT_mask) begin
      ram_strb[ram_strb_MPORT_addr] <= ram_strb_MPORT_data; // @[Decoupled.scala 218:16]
    end
    if (reset) begin // @[Counter.scala 60:40]
      value <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_enq) begin // @[Decoupled.scala 229:17]
      value <= value + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Counter.scala 60:40]
      value_1 <= 1'h0; // @[Counter.scala 60:40]
    end else if (do_deq) begin // @[Decoupled.scala 233:17]
      value_1 <= value_1 + 1'h1; // @[Counter.scala 76:15]
    end
    if (reset) begin // @[Decoupled.scala 221:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 221:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 236:28]
      maybe_full <= do_enq; // @[Decoupled.scala 237:16]
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
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_id[initvar] = _RAND_0[5:0];
  _RAND_1 = {2{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_data[initvar] = _RAND_1[63:0];
  _RAND_2 = {1{`RANDOM}};
  for (initvar = 0; initvar < 2; initvar = initvar+1)
    ram_strb[initvar] = _RAND_2[7:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_3 = {1{`RANDOM}};
  value = _RAND_3[0:0];
  _RAND_4 = {1{`RANDOM}};
  value_1 = _RAND_4[0:0];
  _RAND_5 = {1{`RANDOM}};
  maybe_full = _RAND_5[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Queue_43(
  input         clock,
  input         reset,
  output        io_in_writeAddress_ready,
  input         io_in_writeAddress_valid,
  input  [31:0] io_in_writeAddress_bits_addr,
  input  [7:0]  io_in_writeAddress_bits_len,
  input  [3:0]  io_in_writeAddress_bits_cache,
  output        io_in_writeData_ready,
  input         io_in_writeData_valid,
  input  [63:0] io_in_writeData_bits_data,
  input         io_in_writeResponse_ready,
  output        io_in_writeResponse_valid,
  output        io_in_readAddress_ready,
  input         io_in_readAddress_valid,
  input  [31:0] io_in_readAddress_bits_addr,
  input  [7:0]  io_in_readAddress_bits_len,
  input  [3:0]  io_in_readAddress_bits_cache,
  input         io_in_readData_ready,
  output        io_in_readData_valid,
  output [63:0] io_in_readData_bits_data,
  output        io_in_readData_bits_last,
  input         io_out_writeAddress_ready,
  output        io_out_writeAddress_valid,
  output [5:0]  io_out_writeAddress_bits_id,
  output [31:0] io_out_writeAddress_bits_addr,
  output [7:0]  io_out_writeAddress_bits_len,
  output [2:0]  io_out_writeAddress_bits_size,
  output [1:0]  io_out_writeAddress_bits_burst,
  output [1:0]  io_out_writeAddress_bits_lock,
  output [3:0]  io_out_writeAddress_bits_cache,
  output [2:0]  io_out_writeAddress_bits_prot,
  output [3:0]  io_out_writeAddress_bits_qos,
  input         io_out_writeData_ready,
  output        io_out_writeData_valid,
  output [5:0]  io_out_writeData_bits_id,
  output [63:0] io_out_writeData_bits_data,
  output [7:0]  io_out_writeData_bits_strb,
  output        io_out_writeResponse_ready,
  input         io_out_writeResponse_valid,
  input         io_out_readAddress_ready,
  output        io_out_readAddress_valid,
  output [5:0]  io_out_readAddress_bits_id,
  output [31:0] io_out_readAddress_bits_addr,
  output [7:0]  io_out_readAddress_bits_len,
  output [2:0]  io_out_readAddress_bits_size,
  output [1:0]  io_out_readAddress_bits_burst,
  output [1:0]  io_out_readAddress_bits_lock,
  output [3:0]  io_out_readAddress_bits_cache,
  output [2:0]  io_out_readAddress_bits_prot,
  output [3:0]  io_out_readAddress_bits_qos,
  output        io_out_readData_ready,
  input         io_out_readData_valid,
  input  [63:0] io_out_readData_bits_data,
  input         io_out_readData_bits_last
);
  wire  io_out_readAddress_q_clock; // @[Decoupled.scala 296:21]
  wire  io_out_readAddress_q_reset; // @[Decoupled.scala 296:21]
  wire  io_out_readAddress_q_io_enq_ready; // @[Decoupled.scala 296:21]
  wire  io_out_readAddress_q_io_enq_valid; // @[Decoupled.scala 296:21]
  wire [31:0] io_out_readAddress_q_io_enq_bits_addr; // @[Decoupled.scala 296:21]
  wire [7:0] io_out_readAddress_q_io_enq_bits_len; // @[Decoupled.scala 296:21]
  wire [3:0] io_out_readAddress_q_io_enq_bits_cache; // @[Decoupled.scala 296:21]
  wire  io_out_readAddress_q_io_deq_ready; // @[Decoupled.scala 296:21]
  wire  io_out_readAddress_q_io_deq_valid; // @[Decoupled.scala 296:21]
  wire [5:0] io_out_readAddress_q_io_deq_bits_id; // @[Decoupled.scala 296:21]
  wire [31:0] io_out_readAddress_q_io_deq_bits_addr; // @[Decoupled.scala 296:21]
  wire [7:0] io_out_readAddress_q_io_deq_bits_len; // @[Decoupled.scala 296:21]
  wire [2:0] io_out_readAddress_q_io_deq_bits_size; // @[Decoupled.scala 296:21]
  wire [1:0] io_out_readAddress_q_io_deq_bits_burst; // @[Decoupled.scala 296:21]
  wire [1:0] io_out_readAddress_q_io_deq_bits_lock; // @[Decoupled.scala 296:21]
  wire [3:0] io_out_readAddress_q_io_deq_bits_cache; // @[Decoupled.scala 296:21]
  wire [2:0] io_out_readAddress_q_io_deq_bits_prot; // @[Decoupled.scala 296:21]
  wire [3:0] io_out_readAddress_q_io_deq_bits_qos; // @[Decoupled.scala 296:21]
  wire  io_out_writeAddress_q_clock; // @[Decoupled.scala 296:21]
  wire  io_out_writeAddress_q_reset; // @[Decoupled.scala 296:21]
  wire  io_out_writeAddress_q_io_enq_ready; // @[Decoupled.scala 296:21]
  wire  io_out_writeAddress_q_io_enq_valid; // @[Decoupled.scala 296:21]
  wire [31:0] io_out_writeAddress_q_io_enq_bits_addr; // @[Decoupled.scala 296:21]
  wire [7:0] io_out_writeAddress_q_io_enq_bits_len; // @[Decoupled.scala 296:21]
  wire [3:0] io_out_writeAddress_q_io_enq_bits_cache; // @[Decoupled.scala 296:21]
  wire  io_out_writeAddress_q_io_deq_ready; // @[Decoupled.scala 296:21]
  wire  io_out_writeAddress_q_io_deq_valid; // @[Decoupled.scala 296:21]
  wire [5:0] io_out_writeAddress_q_io_deq_bits_id; // @[Decoupled.scala 296:21]
  wire [31:0] io_out_writeAddress_q_io_deq_bits_addr; // @[Decoupled.scala 296:21]
  wire [7:0] io_out_writeAddress_q_io_deq_bits_len; // @[Decoupled.scala 296:21]
  wire [2:0] io_out_writeAddress_q_io_deq_bits_size; // @[Decoupled.scala 296:21]
  wire [1:0] io_out_writeAddress_q_io_deq_bits_burst; // @[Decoupled.scala 296:21]
  wire [1:0] io_out_writeAddress_q_io_deq_bits_lock; // @[Decoupled.scala 296:21]
  wire [3:0] io_out_writeAddress_q_io_deq_bits_cache; // @[Decoupled.scala 296:21]
  wire [2:0] io_out_writeAddress_q_io_deq_bits_prot; // @[Decoupled.scala 296:21]
  wire [3:0] io_out_writeAddress_q_io_deq_bits_qos; // @[Decoupled.scala 296:21]
  wire  io_out_writeData_q_clock; // @[Decoupled.scala 296:21]
  wire  io_out_writeData_q_reset; // @[Decoupled.scala 296:21]
  wire  io_out_writeData_q_io_enq_ready; // @[Decoupled.scala 296:21]
  wire  io_out_writeData_q_io_enq_valid; // @[Decoupled.scala 296:21]
  wire [63:0] io_out_writeData_q_io_enq_bits_data; // @[Decoupled.scala 296:21]
  wire  io_out_writeData_q_io_deq_ready; // @[Decoupled.scala 296:21]
  wire  io_out_writeData_q_io_deq_valid; // @[Decoupled.scala 296:21]
  wire [5:0] io_out_writeData_q_io_deq_bits_id; // @[Decoupled.scala 296:21]
  wire [63:0] io_out_writeData_q_io_deq_bits_data; // @[Decoupled.scala 296:21]
  wire [7:0] io_out_writeData_q_io_deq_bits_strb; // @[Decoupled.scala 296:21]
  wire  io_in_readData_q_clock; // @[Decoupled.scala 296:21]
  wire  io_in_readData_q_reset; // @[Decoupled.scala 296:21]
  wire  io_in_readData_q_io_enq_ready; // @[Decoupled.scala 296:21]
  wire  io_in_readData_q_io_enq_valid; // @[Decoupled.scala 296:21]
  wire [63:0] io_in_readData_q_io_enq_bits_data; // @[Decoupled.scala 296:21]
  wire  io_in_readData_q_io_enq_bits_last; // @[Decoupled.scala 296:21]
  wire  io_in_readData_q_io_deq_ready; // @[Decoupled.scala 296:21]
  wire  io_in_readData_q_io_deq_valid; // @[Decoupled.scala 296:21]
  wire [63:0] io_in_readData_q_io_deq_bits_data; // @[Decoupled.scala 296:21]
  wire  io_in_readData_q_io_deq_bits_last; // @[Decoupled.scala 296:21]
  wire  io_in_writeResponse_q_clock; // @[Decoupled.scala 296:21]
  wire  io_in_writeResponse_q_reset; // @[Decoupled.scala 296:21]
  wire  io_in_writeResponse_q_io_enq_ready; // @[Decoupled.scala 296:21]
  wire  io_in_writeResponse_q_io_enq_valid; // @[Decoupled.scala 296:21]
  wire  io_in_writeResponse_q_io_deq_ready; // @[Decoupled.scala 296:21]
  wire  io_in_writeResponse_q_io_deq_valid; // @[Decoupled.scala 296:21]
  Queue_38 io_out_readAddress_q ( // @[Decoupled.scala 296:21]
    .clock(io_out_readAddress_q_clock),
    .reset(io_out_readAddress_q_reset),
    .io_enq_ready(io_out_readAddress_q_io_enq_ready),
    .io_enq_valid(io_out_readAddress_q_io_enq_valid),
    .io_enq_bits_addr(io_out_readAddress_q_io_enq_bits_addr),
    .io_enq_bits_len(io_out_readAddress_q_io_enq_bits_len),
    .io_enq_bits_cache(io_out_readAddress_q_io_enq_bits_cache),
    .io_deq_ready(io_out_readAddress_q_io_deq_ready),
    .io_deq_valid(io_out_readAddress_q_io_deq_valid),
    .io_deq_bits_id(io_out_readAddress_q_io_deq_bits_id),
    .io_deq_bits_addr(io_out_readAddress_q_io_deq_bits_addr),
    .io_deq_bits_len(io_out_readAddress_q_io_deq_bits_len),
    .io_deq_bits_size(io_out_readAddress_q_io_deq_bits_size),
    .io_deq_bits_burst(io_out_readAddress_q_io_deq_bits_burst),
    .io_deq_bits_lock(io_out_readAddress_q_io_deq_bits_lock),
    .io_deq_bits_cache(io_out_readAddress_q_io_deq_bits_cache),
    .io_deq_bits_prot(io_out_readAddress_q_io_deq_bits_prot),
    .io_deq_bits_qos(io_out_readAddress_q_io_deq_bits_qos)
  );
  Queue_38 io_out_writeAddress_q ( // @[Decoupled.scala 296:21]
    .clock(io_out_writeAddress_q_clock),
    .reset(io_out_writeAddress_q_reset),
    .io_enq_ready(io_out_writeAddress_q_io_enq_ready),
    .io_enq_valid(io_out_writeAddress_q_io_enq_valid),
    .io_enq_bits_addr(io_out_writeAddress_q_io_enq_bits_addr),
    .io_enq_bits_len(io_out_writeAddress_q_io_enq_bits_len),
    .io_enq_bits_cache(io_out_writeAddress_q_io_enq_bits_cache),
    .io_deq_ready(io_out_writeAddress_q_io_deq_ready),
    .io_deq_valid(io_out_writeAddress_q_io_deq_valid),
    .io_deq_bits_id(io_out_writeAddress_q_io_deq_bits_id),
    .io_deq_bits_addr(io_out_writeAddress_q_io_deq_bits_addr),
    .io_deq_bits_len(io_out_writeAddress_q_io_deq_bits_len),
    .io_deq_bits_size(io_out_writeAddress_q_io_deq_bits_size),
    .io_deq_bits_burst(io_out_writeAddress_q_io_deq_bits_burst),
    .io_deq_bits_lock(io_out_writeAddress_q_io_deq_bits_lock),
    .io_deq_bits_cache(io_out_writeAddress_q_io_deq_bits_cache),
    .io_deq_bits_prot(io_out_writeAddress_q_io_deq_bits_prot),
    .io_deq_bits_qos(io_out_writeAddress_q_io_deq_bits_qos)
  );
  Queue_40 io_out_writeData_q ( // @[Decoupled.scala 296:21]
    .clock(io_out_writeData_q_clock),
    .reset(io_out_writeData_q_reset),
    .io_enq_ready(io_out_writeData_q_io_enq_ready),
    .io_enq_valid(io_out_writeData_q_io_enq_valid),
    .io_enq_bits_data(io_out_writeData_q_io_enq_bits_data),
    .io_deq_ready(io_out_writeData_q_io_deq_ready),
    .io_deq_valid(io_out_writeData_q_io_deq_valid),
    .io_deq_bits_id(io_out_writeData_q_io_deq_bits_id),
    .io_deq_bits_data(io_out_writeData_q_io_deq_bits_data),
    .io_deq_bits_strb(io_out_writeData_q_io_deq_bits_strb)
  );
  Queue_36 io_in_readData_q ( // @[Decoupled.scala 296:21]
    .clock(io_in_readData_q_clock),
    .reset(io_in_readData_q_reset),
    .io_enq_ready(io_in_readData_q_io_enq_ready),
    .io_enq_valid(io_in_readData_q_io_enq_valid),
    .io_enq_bits_data(io_in_readData_q_io_enq_bits_data),
    .io_enq_bits_last(io_in_readData_q_io_enq_bits_last),
    .io_deq_ready(io_in_readData_q_io_deq_ready),
    .io_deq_valid(io_in_readData_q_io_deq_valid),
    .io_deq_bits_data(io_in_readData_q_io_deq_bits_data),
    .io_deq_bits_last(io_in_readData_q_io_deq_bits_last)
  );
  Queue_37 io_in_writeResponse_q ( // @[Decoupled.scala 296:21]
    .clock(io_in_writeResponse_q_clock),
    .reset(io_in_writeResponse_q_reset),
    .io_enq_ready(io_in_writeResponse_q_io_enq_ready),
    .io_enq_valid(io_in_writeResponse_q_io_enq_valid),
    .io_deq_ready(io_in_writeResponse_q_io_deq_ready),
    .io_deq_valid(io_in_writeResponse_q_io_deq_valid)
  );
  assign io_in_writeAddress_ready = io_out_writeAddress_q_io_enq_ready; // @[Decoupled.scala 299:17]
  assign io_in_writeData_ready = io_out_writeData_q_io_enq_ready; // @[Decoupled.scala 299:17]
  assign io_in_writeResponse_valid = io_in_writeResponse_q_io_deq_valid; // @[Queue.scala 18:23]
  assign io_in_readAddress_ready = io_out_readAddress_q_io_enq_ready; // @[Decoupled.scala 299:17]
  assign io_in_readData_valid = io_in_readData_q_io_deq_valid; // @[Queue.scala 17:18]
  assign io_in_readData_bits_data = io_in_readData_q_io_deq_bits_data; // @[Queue.scala 17:18]
  assign io_in_readData_bits_last = io_in_readData_q_io_deq_bits_last; // @[Queue.scala 17:18]
  assign io_out_writeAddress_valid = io_out_writeAddress_q_io_deq_valid; // @[Queue.scala 15:23]
  assign io_out_writeAddress_bits_id = io_out_writeAddress_q_io_deq_bits_id; // @[Queue.scala 15:23]
  assign io_out_writeAddress_bits_addr = io_out_writeAddress_q_io_deq_bits_addr; // @[Queue.scala 15:23]
  assign io_out_writeAddress_bits_len = io_out_writeAddress_q_io_deq_bits_len; // @[Queue.scala 15:23]
  assign io_out_writeAddress_bits_size = io_out_writeAddress_q_io_deq_bits_size; // @[Queue.scala 15:23]
  assign io_out_writeAddress_bits_burst = io_out_writeAddress_q_io_deq_bits_burst; // @[Queue.scala 15:23]
  assign io_out_writeAddress_bits_lock = io_out_writeAddress_q_io_deq_bits_lock; // @[Queue.scala 15:23]
  assign io_out_writeAddress_bits_cache = io_out_writeAddress_q_io_deq_bits_cache; // @[Queue.scala 15:23]
  assign io_out_writeAddress_bits_prot = io_out_writeAddress_q_io_deq_bits_prot; // @[Queue.scala 15:23]
  assign io_out_writeAddress_bits_qos = io_out_writeAddress_q_io_deq_bits_qos; // @[Queue.scala 15:23]
  assign io_out_writeData_valid = io_out_writeData_q_io_deq_valid; // @[Queue.scala 16:20]
  assign io_out_writeData_bits_id = io_out_writeData_q_io_deq_bits_id; // @[Queue.scala 16:20]
  assign io_out_writeData_bits_data = io_out_writeData_q_io_deq_bits_data; // @[Queue.scala 16:20]
  assign io_out_writeData_bits_strb = io_out_writeData_q_io_deq_bits_strb; // @[Queue.scala 16:20]
  assign io_out_writeResponse_ready = io_in_writeResponse_q_io_enq_ready; // @[Decoupled.scala 299:17]
  assign io_out_readAddress_valid = io_out_readAddress_q_io_deq_valid; // @[Queue.scala 14:22]
  assign io_out_readAddress_bits_id = io_out_readAddress_q_io_deq_bits_id; // @[Queue.scala 14:22]
  assign io_out_readAddress_bits_addr = io_out_readAddress_q_io_deq_bits_addr; // @[Queue.scala 14:22]
  assign io_out_readAddress_bits_len = io_out_readAddress_q_io_deq_bits_len; // @[Queue.scala 14:22]
  assign io_out_readAddress_bits_size = io_out_readAddress_q_io_deq_bits_size; // @[Queue.scala 14:22]
  assign io_out_readAddress_bits_burst = io_out_readAddress_q_io_deq_bits_burst; // @[Queue.scala 14:22]
  assign io_out_readAddress_bits_lock = io_out_readAddress_q_io_deq_bits_lock; // @[Queue.scala 14:22]
  assign io_out_readAddress_bits_cache = io_out_readAddress_q_io_deq_bits_cache; // @[Queue.scala 14:22]
  assign io_out_readAddress_bits_prot = io_out_readAddress_q_io_deq_bits_prot; // @[Queue.scala 14:22]
  assign io_out_readAddress_bits_qos = io_out_readAddress_q_io_deq_bits_qos; // @[Queue.scala 14:22]
  assign io_out_readData_ready = io_in_readData_q_io_enq_ready; // @[Decoupled.scala 299:17]
  assign io_out_readAddress_q_clock = clock;
  assign io_out_readAddress_q_reset = reset;
  assign io_out_readAddress_q_io_enq_valid = io_in_readAddress_valid; // @[Decoupled.scala 297:22]
  assign io_out_readAddress_q_io_enq_bits_addr = io_in_readAddress_bits_addr; // @[Decoupled.scala 298:21]
  assign io_out_readAddress_q_io_enq_bits_len = io_in_readAddress_bits_len; // @[Decoupled.scala 298:21]
  assign io_out_readAddress_q_io_enq_bits_cache = io_in_readAddress_bits_cache; // @[Decoupled.scala 298:21]
  assign io_out_readAddress_q_io_deq_ready = io_out_readAddress_ready; // @[Queue.scala 14:22]
  assign io_out_writeAddress_q_clock = clock;
  assign io_out_writeAddress_q_reset = reset;
  assign io_out_writeAddress_q_io_enq_valid = io_in_writeAddress_valid; // @[Decoupled.scala 297:22]
  assign io_out_writeAddress_q_io_enq_bits_addr = io_in_writeAddress_bits_addr; // @[Decoupled.scala 298:21]
  assign io_out_writeAddress_q_io_enq_bits_len = io_in_writeAddress_bits_len; // @[Decoupled.scala 298:21]
  assign io_out_writeAddress_q_io_enq_bits_cache = io_in_writeAddress_bits_cache; // @[Decoupled.scala 298:21]
  assign io_out_writeAddress_q_io_deq_ready = io_out_writeAddress_ready; // @[Queue.scala 15:23]
  assign io_out_writeData_q_clock = clock;
  assign io_out_writeData_q_reset = reset;
  assign io_out_writeData_q_io_enq_valid = io_in_writeData_valid; // @[Decoupled.scala 297:22]
  assign io_out_writeData_q_io_enq_bits_data = io_in_writeData_bits_data; // @[Decoupled.scala 298:21]
  assign io_out_writeData_q_io_deq_ready = io_out_writeData_ready; // @[Queue.scala 16:20]
  assign io_in_readData_q_clock = clock;
  assign io_in_readData_q_reset = reset;
  assign io_in_readData_q_io_enq_valid = io_out_readData_valid; // @[Decoupled.scala 297:22]
  assign io_in_readData_q_io_enq_bits_data = io_out_readData_bits_data; // @[Decoupled.scala 298:21]
  assign io_in_readData_q_io_enq_bits_last = io_out_readData_bits_last; // @[Decoupled.scala 298:21]
  assign io_in_readData_q_io_deq_ready = io_in_readData_ready; // @[Queue.scala 17:18]
  assign io_in_writeResponse_q_clock = clock;
  assign io_in_writeResponse_q_reset = reset;
  assign io_in_writeResponse_q_io_enq_valid = io_out_writeResponse_valid; // @[Decoupled.scala 297:22]
  assign io_in_writeResponse_q_io_deq_ready = io_in_writeResponse_ready; // @[Queue.scala 18:23]
endmodule
module AXIWrapperTCU(
  input         clock,
  input         reset,
  output        instruction_ready,
  input         instruction_valid,
  input  [3:0]  instruction_bits_opcode,
  input  [3:0]  instruction_bits_flags,
  input  [47:0] instruction_bits_arguments,
  input         status_ready,
  output        status_valid,
  output        status_bits_last,
  output [3:0]  status_bits_bits_opcode,
  output [3:0]  status_bits_bits_flags,
  output [47:0] status_bits_bits_arguments,
  input         dram0_writeAddress_ready,
  output        dram0_writeAddress_valid,
  output [5:0]  dram0_writeAddress_bits_id,
  output [31:0] dram0_writeAddress_bits_addr,
  output [7:0]  dram0_writeAddress_bits_len,
  output [2:0]  dram0_writeAddress_bits_size,
  output [1:0]  dram0_writeAddress_bits_burst,
  output [1:0]  dram0_writeAddress_bits_lock,
  output [3:0]  dram0_writeAddress_bits_cache,
  output [2:0]  dram0_writeAddress_bits_prot,
  output [3:0]  dram0_writeAddress_bits_qos,
  input         dram0_writeData_ready,
  output        dram0_writeData_valid,
  output [5:0]  dram0_writeData_bits_id,
  output [63:0] dram0_writeData_bits_data,
  output [7:0]  dram0_writeData_bits_strb,
  output        dram0_writeData_bits_last,
  output        dram0_writeResponse_ready,
  input         dram0_writeResponse_valid,
  input  [5:0]  dram0_writeResponse_bits_id,
  input  [1:0]  dram0_writeResponse_bits_resp,
  input         dram0_readAddress_ready,
  output        dram0_readAddress_valid,
  output [5:0]  dram0_readAddress_bits_id,
  output [31:0] dram0_readAddress_bits_addr,
  output [7:0]  dram0_readAddress_bits_len,
  output [2:0]  dram0_readAddress_bits_size,
  output [1:0]  dram0_readAddress_bits_burst,
  output [1:0]  dram0_readAddress_bits_lock,
  output [3:0]  dram0_readAddress_bits_cache,
  output [2:0]  dram0_readAddress_bits_prot,
  output [3:0]  dram0_readAddress_bits_qos,
  output        dram0_readData_ready,
  input         dram0_readData_valid,
  input  [5:0]  dram0_readData_bits_id,
  input  [63:0] dram0_readData_bits_data,
  input  [1:0]  dram0_readData_bits_resp,
  input         dram0_readData_bits_last,
  input         dram1_writeAddress_ready,
  output        dram1_writeAddress_valid,
  output [5:0]  dram1_writeAddress_bits_id,
  output [31:0] dram1_writeAddress_bits_addr,
  output [7:0]  dram1_writeAddress_bits_len,
  output [2:0]  dram1_writeAddress_bits_size,
  output [1:0]  dram1_writeAddress_bits_burst,
  output [1:0]  dram1_writeAddress_bits_lock,
  output [3:0]  dram1_writeAddress_bits_cache,
  output [2:0]  dram1_writeAddress_bits_prot,
  output [3:0]  dram1_writeAddress_bits_qos,
  input         dram1_writeData_ready,
  output        dram1_writeData_valid,
  output [5:0]  dram1_writeData_bits_id,
  output [63:0] dram1_writeData_bits_data,
  output [7:0]  dram1_writeData_bits_strb,
  output        dram1_writeData_bits_last,
  output        dram1_writeResponse_ready,
  input         dram1_writeResponse_valid,
  input  [5:0]  dram1_writeResponse_bits_id,
  input  [1:0]  dram1_writeResponse_bits_resp,
  input         dram1_readAddress_ready,
  output        dram1_readAddress_valid,
  output [5:0]  dram1_readAddress_bits_id,
  output [31:0] dram1_readAddress_bits_addr,
  output [7:0]  dram1_readAddress_bits_len,
  output [2:0]  dram1_readAddress_bits_size,
  output [1:0]  dram1_readAddress_bits_burst,
  output [1:0]  dram1_readAddress_bits_lock,
  output [3:0]  dram1_readAddress_bits_cache,
  output [2:0]  dram1_readAddress_bits_prot,
  output [3:0]  dram1_readAddress_bits_qos,
  output        dram1_readData_ready,
  input         dram1_readData_valid,
  input  [5:0]  dram1_readData_bits_id,
  input  [63:0] dram1_readData_bits_data,
  input  [1:0]  dram1_readData_bits_resp,
  input         dram1_readData_bits_last,
  output        error,
  input         sample_ready,
  output        sample_valid,
  output        sample_bits_last,
  output        sample_bits_bits_flags_instruction_ready,
  output        sample_bits_bits_flags_instruction_valid,
  output        sample_bits_bits_flags_memPortA_ready,
  output        sample_bits_bits_flags_memPortA_valid,
  output        sample_bits_bits_flags_memPortB_ready,
  output        sample_bits_bits_flags_memPortB_valid,
  output        sample_bits_bits_flags_dram0_ready,
  output        sample_bits_bits_flags_dram0_valid,
  output        sample_bits_bits_flags_dram1_ready,
  output        sample_bits_bits_flags_dram1_valid,
  output        sample_bits_bits_flags_dataflow_ready,
  output        sample_bits_bits_flags_dataflow_valid,
  output        sample_bits_bits_flags_acc_ready,
  output        sample_bits_bits_flags_acc_valid,
  output        sample_bits_bits_flags_array_ready,
  output        sample_bits_bits_flags_array_valid,
  output [31:0] sample_bits_bits_programCounter
);
  wire  tcu_clock; // @[AXIWrapperTCU.scala 32:19]
  wire  tcu_reset; // @[AXIWrapperTCU.scala 32:19]
  wire  tcu_io_instruction_ready; // @[AXIWrapperTCU.scala 32:19]
  wire  tcu_io_instruction_valid; // @[AXIWrapperTCU.scala 32:19]
  wire [3:0] tcu_io_instruction_bits_opcode; // @[AXIWrapperTCU.scala 32:19]
  wire [3:0] tcu_io_instruction_bits_flags; // @[AXIWrapperTCU.scala 32:19]
  wire [47:0] tcu_io_instruction_bits_arguments; // @[AXIWrapperTCU.scala 32:19]
  wire  tcu_io_status_ready; // @[AXIWrapperTCU.scala 32:19]
  wire  tcu_io_status_valid; // @[AXIWrapperTCU.scala 32:19]
  wire  tcu_io_status_bits_last; // @[AXIWrapperTCU.scala 32:19]
  wire [3:0] tcu_io_status_bits_bits_opcode; // @[AXIWrapperTCU.scala 32:19]
  wire [3:0] tcu_io_status_bits_bits_flags; // @[AXIWrapperTCU.scala 32:19]
  wire [47:0] tcu_io_status_bits_bits_arguments; // @[AXIWrapperTCU.scala 32:19]
  wire  tcu_io_dram0_control_ready; // @[AXIWrapperTCU.scala 32:19]
  wire  tcu_io_dram0_control_valid; // @[AXIWrapperTCU.scala 32:19]
  wire  tcu_io_dram0_control_bits_write; // @[AXIWrapperTCU.scala 32:19]
  wire [19:0] tcu_io_dram0_control_bits_address; // @[AXIWrapperTCU.scala 32:19]
  wire [19:0] tcu_io_dram0_control_bits_size; // @[AXIWrapperTCU.scala 32:19]
  wire  tcu_io_dram0_dataIn_ready; // @[AXIWrapperTCU.scala 32:19]
  wire  tcu_io_dram0_dataIn_valid; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram0_dataIn_bits_0; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram0_dataIn_bits_1; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram0_dataIn_bits_2; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram0_dataIn_bits_3; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram0_dataIn_bits_4; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram0_dataIn_bits_5; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram0_dataIn_bits_6; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram0_dataIn_bits_7; // @[AXIWrapperTCU.scala 32:19]
  wire  tcu_io_dram0_dataOut_ready; // @[AXIWrapperTCU.scala 32:19]
  wire  tcu_io_dram0_dataOut_valid; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram0_dataOut_bits_0; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram0_dataOut_bits_1; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram0_dataOut_bits_2; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram0_dataOut_bits_3; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram0_dataOut_bits_4; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram0_dataOut_bits_5; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram0_dataOut_bits_6; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram0_dataOut_bits_7; // @[AXIWrapperTCU.scala 32:19]
  wire  tcu_io_dram1_control_ready; // @[AXIWrapperTCU.scala 32:19]
  wire  tcu_io_dram1_control_valid; // @[AXIWrapperTCU.scala 32:19]
  wire  tcu_io_dram1_control_bits_write; // @[AXIWrapperTCU.scala 32:19]
  wire [19:0] tcu_io_dram1_control_bits_address; // @[AXIWrapperTCU.scala 32:19]
  wire [19:0] tcu_io_dram1_control_bits_size; // @[AXIWrapperTCU.scala 32:19]
  wire  tcu_io_dram1_dataIn_ready; // @[AXIWrapperTCU.scala 32:19]
  wire  tcu_io_dram1_dataIn_valid; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram1_dataIn_bits_0; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram1_dataIn_bits_1; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram1_dataIn_bits_2; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram1_dataIn_bits_3; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram1_dataIn_bits_4; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram1_dataIn_bits_5; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram1_dataIn_bits_6; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram1_dataIn_bits_7; // @[AXIWrapperTCU.scala 32:19]
  wire  tcu_io_dram1_dataOut_ready; // @[AXIWrapperTCU.scala 32:19]
  wire  tcu_io_dram1_dataOut_valid; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram1_dataOut_bits_0; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram1_dataOut_bits_1; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram1_dataOut_bits_2; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram1_dataOut_bits_3; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram1_dataOut_bits_4; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram1_dataOut_bits_5; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram1_dataOut_bits_6; // @[AXIWrapperTCU.scala 32:19]
  wire [15:0] tcu_io_dram1_dataOut_bits_7; // @[AXIWrapperTCU.scala 32:19]
  wire [31:0] tcu_io_config_dram0AddressOffset; // @[AXIWrapperTCU.scala 32:19]
  wire [3:0] tcu_io_config_dram0CacheBehaviour; // @[AXIWrapperTCU.scala 32:19]
  wire [31:0] tcu_io_config_dram1AddressOffset; // @[AXIWrapperTCU.scala 32:19]
  wire [3:0] tcu_io_config_dram1CacheBehaviour; // @[AXIWrapperTCU.scala 32:19]
  wire  tcu_io_timeout; // @[AXIWrapperTCU.scala 32:19]
  wire  tcu_io_error; // @[AXIWrapperTCU.scala 32:19]
  wire  tcu_io_tracepoint; // @[AXIWrapperTCU.scala 32:19]
  wire [31:0] tcu_io_programCounter; // @[AXIWrapperTCU.scala 32:19]
  wire  dram0BoundarySplitter_clock; // @[AXIWrapperTCU.scala 56:37]
  wire  dram0BoundarySplitter_reset; // @[AXIWrapperTCU.scala 56:37]
  wire  dram0BoundarySplitter_io_in_writeAddress_ready; // @[AXIWrapperTCU.scala 56:37]
  wire  dram0BoundarySplitter_io_in_writeAddress_valid; // @[AXIWrapperTCU.scala 56:37]
  wire [5:0] dram0BoundarySplitter_io_in_writeAddress_bits_id; // @[AXIWrapperTCU.scala 56:37]
  wire [31:0] dram0BoundarySplitter_io_in_writeAddress_bits_addr; // @[AXIWrapperTCU.scala 56:37]
  wire [7:0] dram0BoundarySplitter_io_in_writeAddress_bits_len; // @[AXIWrapperTCU.scala 56:37]
  wire [2:0] dram0BoundarySplitter_io_in_writeAddress_bits_size; // @[AXIWrapperTCU.scala 56:37]
  wire [1:0] dram0BoundarySplitter_io_in_writeAddress_bits_burst; // @[AXIWrapperTCU.scala 56:37]
  wire [1:0] dram0BoundarySplitter_io_in_writeAddress_bits_lock; // @[AXIWrapperTCU.scala 56:37]
  wire [3:0] dram0BoundarySplitter_io_in_writeAddress_bits_cache; // @[AXIWrapperTCU.scala 56:37]
  wire [2:0] dram0BoundarySplitter_io_in_writeAddress_bits_prot; // @[AXIWrapperTCU.scala 56:37]
  wire [3:0] dram0BoundarySplitter_io_in_writeAddress_bits_qos; // @[AXIWrapperTCU.scala 56:37]
  wire  dram0BoundarySplitter_io_in_writeData_ready; // @[AXIWrapperTCU.scala 56:37]
  wire  dram0BoundarySplitter_io_in_writeData_valid; // @[AXIWrapperTCU.scala 56:37]
  wire [5:0] dram0BoundarySplitter_io_in_writeData_bits_id; // @[AXIWrapperTCU.scala 56:37]
  wire [63:0] dram0BoundarySplitter_io_in_writeData_bits_data; // @[AXIWrapperTCU.scala 56:37]
  wire [7:0] dram0BoundarySplitter_io_in_writeData_bits_strb; // @[AXIWrapperTCU.scala 56:37]
  wire  dram0BoundarySplitter_io_in_writeResponse_ready; // @[AXIWrapperTCU.scala 56:37]
  wire  dram0BoundarySplitter_io_in_writeResponse_valid; // @[AXIWrapperTCU.scala 56:37]
  wire  dram0BoundarySplitter_io_in_readAddress_ready; // @[AXIWrapperTCU.scala 56:37]
  wire  dram0BoundarySplitter_io_in_readAddress_valid; // @[AXIWrapperTCU.scala 56:37]
  wire [5:0] dram0BoundarySplitter_io_in_readAddress_bits_id; // @[AXIWrapperTCU.scala 56:37]
  wire [31:0] dram0BoundarySplitter_io_in_readAddress_bits_addr; // @[AXIWrapperTCU.scala 56:37]
  wire [7:0] dram0BoundarySplitter_io_in_readAddress_bits_len; // @[AXIWrapperTCU.scala 56:37]
  wire [2:0] dram0BoundarySplitter_io_in_readAddress_bits_size; // @[AXIWrapperTCU.scala 56:37]
  wire [1:0] dram0BoundarySplitter_io_in_readAddress_bits_burst; // @[AXIWrapperTCU.scala 56:37]
  wire [1:0] dram0BoundarySplitter_io_in_readAddress_bits_lock; // @[AXIWrapperTCU.scala 56:37]
  wire [3:0] dram0BoundarySplitter_io_in_readAddress_bits_cache; // @[AXIWrapperTCU.scala 56:37]
  wire [2:0] dram0BoundarySplitter_io_in_readAddress_bits_prot; // @[AXIWrapperTCU.scala 56:37]
  wire [3:0] dram0BoundarySplitter_io_in_readAddress_bits_qos; // @[AXIWrapperTCU.scala 56:37]
  wire  dram0BoundarySplitter_io_in_readData_ready; // @[AXIWrapperTCU.scala 56:37]
  wire  dram0BoundarySplitter_io_in_readData_valid; // @[AXIWrapperTCU.scala 56:37]
  wire [63:0] dram0BoundarySplitter_io_in_readData_bits_data; // @[AXIWrapperTCU.scala 56:37]
  wire  dram0BoundarySplitter_io_in_readData_bits_last; // @[AXIWrapperTCU.scala 56:37]
  wire  dram0BoundarySplitter_io_out_writeAddress_ready; // @[AXIWrapperTCU.scala 56:37]
  wire  dram0BoundarySplitter_io_out_writeAddress_valid; // @[AXIWrapperTCU.scala 56:37]
  wire [5:0] dram0BoundarySplitter_io_out_writeAddress_bits_id; // @[AXIWrapperTCU.scala 56:37]
  wire [31:0] dram0BoundarySplitter_io_out_writeAddress_bits_addr; // @[AXIWrapperTCU.scala 56:37]
  wire [7:0] dram0BoundarySplitter_io_out_writeAddress_bits_len; // @[AXIWrapperTCU.scala 56:37]
  wire [2:0] dram0BoundarySplitter_io_out_writeAddress_bits_size; // @[AXIWrapperTCU.scala 56:37]
  wire [1:0] dram0BoundarySplitter_io_out_writeAddress_bits_burst; // @[AXIWrapperTCU.scala 56:37]
  wire [1:0] dram0BoundarySplitter_io_out_writeAddress_bits_lock; // @[AXIWrapperTCU.scala 56:37]
  wire [3:0] dram0BoundarySplitter_io_out_writeAddress_bits_cache; // @[AXIWrapperTCU.scala 56:37]
  wire [2:0] dram0BoundarySplitter_io_out_writeAddress_bits_prot; // @[AXIWrapperTCU.scala 56:37]
  wire [3:0] dram0BoundarySplitter_io_out_writeAddress_bits_qos; // @[AXIWrapperTCU.scala 56:37]
  wire  dram0BoundarySplitter_io_out_writeData_ready; // @[AXIWrapperTCU.scala 56:37]
  wire  dram0BoundarySplitter_io_out_writeData_valid; // @[AXIWrapperTCU.scala 56:37]
  wire [5:0] dram0BoundarySplitter_io_out_writeData_bits_id; // @[AXIWrapperTCU.scala 56:37]
  wire [63:0] dram0BoundarySplitter_io_out_writeData_bits_data; // @[AXIWrapperTCU.scala 56:37]
  wire [7:0] dram0BoundarySplitter_io_out_writeData_bits_strb; // @[AXIWrapperTCU.scala 56:37]
  wire  dram0BoundarySplitter_io_out_writeData_bits_last; // @[AXIWrapperTCU.scala 56:37]
  wire  dram0BoundarySplitter_io_out_writeResponse_ready; // @[AXIWrapperTCU.scala 56:37]
  wire  dram0BoundarySplitter_io_out_writeResponse_valid; // @[AXIWrapperTCU.scala 56:37]
  wire  dram0BoundarySplitter_io_out_readAddress_ready; // @[AXIWrapperTCU.scala 56:37]
  wire  dram0BoundarySplitter_io_out_readAddress_valid; // @[AXIWrapperTCU.scala 56:37]
  wire [5:0] dram0BoundarySplitter_io_out_readAddress_bits_id; // @[AXIWrapperTCU.scala 56:37]
  wire [31:0] dram0BoundarySplitter_io_out_readAddress_bits_addr; // @[AXIWrapperTCU.scala 56:37]
  wire [7:0] dram0BoundarySplitter_io_out_readAddress_bits_len; // @[AXIWrapperTCU.scala 56:37]
  wire [2:0] dram0BoundarySplitter_io_out_readAddress_bits_size; // @[AXIWrapperTCU.scala 56:37]
  wire [1:0] dram0BoundarySplitter_io_out_readAddress_bits_burst; // @[AXIWrapperTCU.scala 56:37]
  wire [1:0] dram0BoundarySplitter_io_out_readAddress_bits_lock; // @[AXIWrapperTCU.scala 56:37]
  wire [3:0] dram0BoundarySplitter_io_out_readAddress_bits_cache; // @[AXIWrapperTCU.scala 56:37]
  wire [2:0] dram0BoundarySplitter_io_out_readAddress_bits_prot; // @[AXIWrapperTCU.scala 56:37]
  wire [3:0] dram0BoundarySplitter_io_out_readAddress_bits_qos; // @[AXIWrapperTCU.scala 56:37]
  wire  dram0BoundarySplitter_io_out_readData_ready; // @[AXIWrapperTCU.scala 56:37]
  wire  dram0BoundarySplitter_io_out_readData_valid; // @[AXIWrapperTCU.scala 56:37]
  wire [63:0] dram0BoundarySplitter_io_out_readData_bits_data; // @[AXIWrapperTCU.scala 56:37]
  wire  dram1BoundarySplitter_clock; // @[AXIWrapperTCU.scala 60:37]
  wire  dram1BoundarySplitter_reset; // @[AXIWrapperTCU.scala 60:37]
  wire  dram1BoundarySplitter_io_in_writeAddress_ready; // @[AXIWrapperTCU.scala 60:37]
  wire  dram1BoundarySplitter_io_in_writeAddress_valid; // @[AXIWrapperTCU.scala 60:37]
  wire [5:0] dram1BoundarySplitter_io_in_writeAddress_bits_id; // @[AXIWrapperTCU.scala 60:37]
  wire [31:0] dram1BoundarySplitter_io_in_writeAddress_bits_addr; // @[AXIWrapperTCU.scala 60:37]
  wire [7:0] dram1BoundarySplitter_io_in_writeAddress_bits_len; // @[AXIWrapperTCU.scala 60:37]
  wire [2:0] dram1BoundarySplitter_io_in_writeAddress_bits_size; // @[AXIWrapperTCU.scala 60:37]
  wire [1:0] dram1BoundarySplitter_io_in_writeAddress_bits_burst; // @[AXIWrapperTCU.scala 60:37]
  wire [1:0] dram1BoundarySplitter_io_in_writeAddress_bits_lock; // @[AXIWrapperTCU.scala 60:37]
  wire [3:0] dram1BoundarySplitter_io_in_writeAddress_bits_cache; // @[AXIWrapperTCU.scala 60:37]
  wire [2:0] dram1BoundarySplitter_io_in_writeAddress_bits_prot; // @[AXIWrapperTCU.scala 60:37]
  wire [3:0] dram1BoundarySplitter_io_in_writeAddress_bits_qos; // @[AXIWrapperTCU.scala 60:37]
  wire  dram1BoundarySplitter_io_in_writeData_ready; // @[AXIWrapperTCU.scala 60:37]
  wire  dram1BoundarySplitter_io_in_writeData_valid; // @[AXIWrapperTCU.scala 60:37]
  wire [5:0] dram1BoundarySplitter_io_in_writeData_bits_id; // @[AXIWrapperTCU.scala 60:37]
  wire [63:0] dram1BoundarySplitter_io_in_writeData_bits_data; // @[AXIWrapperTCU.scala 60:37]
  wire [7:0] dram1BoundarySplitter_io_in_writeData_bits_strb; // @[AXIWrapperTCU.scala 60:37]
  wire  dram1BoundarySplitter_io_in_writeResponse_ready; // @[AXIWrapperTCU.scala 60:37]
  wire  dram1BoundarySplitter_io_in_writeResponse_valid; // @[AXIWrapperTCU.scala 60:37]
  wire  dram1BoundarySplitter_io_in_readAddress_ready; // @[AXIWrapperTCU.scala 60:37]
  wire  dram1BoundarySplitter_io_in_readAddress_valid; // @[AXIWrapperTCU.scala 60:37]
  wire [5:0] dram1BoundarySplitter_io_in_readAddress_bits_id; // @[AXIWrapperTCU.scala 60:37]
  wire [31:0] dram1BoundarySplitter_io_in_readAddress_bits_addr; // @[AXIWrapperTCU.scala 60:37]
  wire [7:0] dram1BoundarySplitter_io_in_readAddress_bits_len; // @[AXIWrapperTCU.scala 60:37]
  wire [2:0] dram1BoundarySplitter_io_in_readAddress_bits_size; // @[AXIWrapperTCU.scala 60:37]
  wire [1:0] dram1BoundarySplitter_io_in_readAddress_bits_burst; // @[AXIWrapperTCU.scala 60:37]
  wire [1:0] dram1BoundarySplitter_io_in_readAddress_bits_lock; // @[AXIWrapperTCU.scala 60:37]
  wire [3:0] dram1BoundarySplitter_io_in_readAddress_bits_cache; // @[AXIWrapperTCU.scala 60:37]
  wire [2:0] dram1BoundarySplitter_io_in_readAddress_bits_prot; // @[AXIWrapperTCU.scala 60:37]
  wire [3:0] dram1BoundarySplitter_io_in_readAddress_bits_qos; // @[AXIWrapperTCU.scala 60:37]
  wire  dram1BoundarySplitter_io_in_readData_ready; // @[AXIWrapperTCU.scala 60:37]
  wire  dram1BoundarySplitter_io_in_readData_valid; // @[AXIWrapperTCU.scala 60:37]
  wire [63:0] dram1BoundarySplitter_io_in_readData_bits_data; // @[AXIWrapperTCU.scala 60:37]
  wire  dram1BoundarySplitter_io_in_readData_bits_last; // @[AXIWrapperTCU.scala 60:37]
  wire  dram1BoundarySplitter_io_out_writeAddress_ready; // @[AXIWrapperTCU.scala 60:37]
  wire  dram1BoundarySplitter_io_out_writeAddress_valid; // @[AXIWrapperTCU.scala 60:37]
  wire [5:0] dram1BoundarySplitter_io_out_writeAddress_bits_id; // @[AXIWrapperTCU.scala 60:37]
  wire [31:0] dram1BoundarySplitter_io_out_writeAddress_bits_addr; // @[AXIWrapperTCU.scala 60:37]
  wire [7:0] dram1BoundarySplitter_io_out_writeAddress_bits_len; // @[AXIWrapperTCU.scala 60:37]
  wire [2:0] dram1BoundarySplitter_io_out_writeAddress_bits_size; // @[AXIWrapperTCU.scala 60:37]
  wire [1:0] dram1BoundarySplitter_io_out_writeAddress_bits_burst; // @[AXIWrapperTCU.scala 60:37]
  wire [1:0] dram1BoundarySplitter_io_out_writeAddress_bits_lock; // @[AXIWrapperTCU.scala 60:37]
  wire [3:0] dram1BoundarySplitter_io_out_writeAddress_bits_cache; // @[AXIWrapperTCU.scala 60:37]
  wire [2:0] dram1BoundarySplitter_io_out_writeAddress_bits_prot; // @[AXIWrapperTCU.scala 60:37]
  wire [3:0] dram1BoundarySplitter_io_out_writeAddress_bits_qos; // @[AXIWrapperTCU.scala 60:37]
  wire  dram1BoundarySplitter_io_out_writeData_ready; // @[AXIWrapperTCU.scala 60:37]
  wire  dram1BoundarySplitter_io_out_writeData_valid; // @[AXIWrapperTCU.scala 60:37]
  wire [5:0] dram1BoundarySplitter_io_out_writeData_bits_id; // @[AXIWrapperTCU.scala 60:37]
  wire [63:0] dram1BoundarySplitter_io_out_writeData_bits_data; // @[AXIWrapperTCU.scala 60:37]
  wire [7:0] dram1BoundarySplitter_io_out_writeData_bits_strb; // @[AXIWrapperTCU.scala 60:37]
  wire  dram1BoundarySplitter_io_out_writeData_bits_last; // @[AXIWrapperTCU.scala 60:37]
  wire  dram1BoundarySplitter_io_out_writeResponse_ready; // @[AXIWrapperTCU.scala 60:37]
  wire  dram1BoundarySplitter_io_out_writeResponse_valid; // @[AXIWrapperTCU.scala 60:37]
  wire  dram1BoundarySplitter_io_out_readAddress_ready; // @[AXIWrapperTCU.scala 60:37]
  wire  dram1BoundarySplitter_io_out_readAddress_valid; // @[AXIWrapperTCU.scala 60:37]
  wire [5:0] dram1BoundarySplitter_io_out_readAddress_bits_id; // @[AXIWrapperTCU.scala 60:37]
  wire [31:0] dram1BoundarySplitter_io_out_readAddress_bits_addr; // @[AXIWrapperTCU.scala 60:37]
  wire [7:0] dram1BoundarySplitter_io_out_readAddress_bits_len; // @[AXIWrapperTCU.scala 60:37]
  wire [2:0] dram1BoundarySplitter_io_out_readAddress_bits_size; // @[AXIWrapperTCU.scala 60:37]
  wire [1:0] dram1BoundarySplitter_io_out_readAddress_bits_burst; // @[AXIWrapperTCU.scala 60:37]
  wire [1:0] dram1BoundarySplitter_io_out_readAddress_bits_lock; // @[AXIWrapperTCU.scala 60:37]
  wire [3:0] dram1BoundarySplitter_io_out_readAddress_bits_cache; // @[AXIWrapperTCU.scala 60:37]
  wire [2:0] dram1BoundarySplitter_io_out_readAddress_bits_prot; // @[AXIWrapperTCU.scala 60:37]
  wire [3:0] dram1BoundarySplitter_io_out_readAddress_bits_qos; // @[AXIWrapperTCU.scala 60:37]
  wire  dram1BoundarySplitter_io_out_readData_ready; // @[AXIWrapperTCU.scala 60:37]
  wire  dram1BoundarySplitter_io_out_readData_valid; // @[AXIWrapperTCU.scala 60:37]
  wire [63:0] dram1BoundarySplitter_io_out_readData_bits_data; // @[AXIWrapperTCU.scala 60:37]
  wire  dram0Converter_clock; // @[AXIWrapperTCU.scala 65:30]
  wire  dram0Converter_reset; // @[AXIWrapperTCU.scala 65:30]
  wire  dram0Converter_io_mem_control_ready; // @[AXIWrapperTCU.scala 65:30]
  wire  dram0Converter_io_mem_control_valid; // @[AXIWrapperTCU.scala 65:30]
  wire  dram0Converter_io_mem_control_bits_write; // @[AXIWrapperTCU.scala 65:30]
  wire [19:0] dram0Converter_io_mem_control_bits_address; // @[AXIWrapperTCU.scala 65:30]
  wire [19:0] dram0Converter_io_mem_control_bits_size; // @[AXIWrapperTCU.scala 65:30]
  wire  dram0Converter_io_mem_dataIn_ready; // @[AXIWrapperTCU.scala 65:30]
  wire  dram0Converter_io_mem_dataIn_valid; // @[AXIWrapperTCU.scala 65:30]
  wire [15:0] dram0Converter_io_mem_dataIn_bits_0; // @[AXIWrapperTCU.scala 65:30]
  wire [15:0] dram0Converter_io_mem_dataIn_bits_1; // @[AXIWrapperTCU.scala 65:30]
  wire [15:0] dram0Converter_io_mem_dataIn_bits_2; // @[AXIWrapperTCU.scala 65:30]
  wire [15:0] dram0Converter_io_mem_dataIn_bits_3; // @[AXIWrapperTCU.scala 65:30]
  wire [15:0] dram0Converter_io_mem_dataIn_bits_4; // @[AXIWrapperTCU.scala 65:30]
  wire [15:0] dram0Converter_io_mem_dataIn_bits_5; // @[AXIWrapperTCU.scala 65:30]
  wire [15:0] dram0Converter_io_mem_dataIn_bits_6; // @[AXIWrapperTCU.scala 65:30]
  wire [15:0] dram0Converter_io_mem_dataIn_bits_7; // @[AXIWrapperTCU.scala 65:30]
  wire  dram0Converter_io_mem_dataOut_ready; // @[AXIWrapperTCU.scala 65:30]
  wire  dram0Converter_io_mem_dataOut_valid; // @[AXIWrapperTCU.scala 65:30]
  wire [15:0] dram0Converter_io_mem_dataOut_bits_0; // @[AXIWrapperTCU.scala 65:30]
  wire [15:0] dram0Converter_io_mem_dataOut_bits_1; // @[AXIWrapperTCU.scala 65:30]
  wire [15:0] dram0Converter_io_mem_dataOut_bits_2; // @[AXIWrapperTCU.scala 65:30]
  wire [15:0] dram0Converter_io_mem_dataOut_bits_3; // @[AXIWrapperTCU.scala 65:30]
  wire [15:0] dram0Converter_io_mem_dataOut_bits_4; // @[AXIWrapperTCU.scala 65:30]
  wire [15:0] dram0Converter_io_mem_dataOut_bits_5; // @[AXIWrapperTCU.scala 65:30]
  wire [15:0] dram0Converter_io_mem_dataOut_bits_6; // @[AXIWrapperTCU.scala 65:30]
  wire [15:0] dram0Converter_io_mem_dataOut_bits_7; // @[AXIWrapperTCU.scala 65:30]
  wire  dram0Converter_io_axi_writeAddress_ready; // @[AXIWrapperTCU.scala 65:30]
  wire  dram0Converter_io_axi_writeAddress_valid; // @[AXIWrapperTCU.scala 65:30]
  wire [31:0] dram0Converter_io_axi_writeAddress_bits_addr; // @[AXIWrapperTCU.scala 65:30]
  wire [7:0] dram0Converter_io_axi_writeAddress_bits_len; // @[AXIWrapperTCU.scala 65:30]
  wire [3:0] dram0Converter_io_axi_writeAddress_bits_cache; // @[AXIWrapperTCU.scala 65:30]
  wire  dram0Converter_io_axi_writeData_ready; // @[AXIWrapperTCU.scala 65:30]
  wire  dram0Converter_io_axi_writeData_valid; // @[AXIWrapperTCU.scala 65:30]
  wire [63:0] dram0Converter_io_axi_writeData_bits_data; // @[AXIWrapperTCU.scala 65:30]
  wire  dram0Converter_io_axi_writeResponse_ready; // @[AXIWrapperTCU.scala 65:30]
  wire  dram0Converter_io_axi_writeResponse_valid; // @[AXIWrapperTCU.scala 65:30]
  wire  dram0Converter_io_axi_readAddress_ready; // @[AXIWrapperTCU.scala 65:30]
  wire  dram0Converter_io_axi_readAddress_valid; // @[AXIWrapperTCU.scala 65:30]
  wire [31:0] dram0Converter_io_axi_readAddress_bits_addr; // @[AXIWrapperTCU.scala 65:30]
  wire [7:0] dram0Converter_io_axi_readAddress_bits_len; // @[AXIWrapperTCU.scala 65:30]
  wire [3:0] dram0Converter_io_axi_readAddress_bits_cache; // @[AXIWrapperTCU.scala 65:30]
  wire  dram0Converter_io_axi_readData_ready; // @[AXIWrapperTCU.scala 65:30]
  wire  dram0Converter_io_axi_readData_valid; // @[AXIWrapperTCU.scala 65:30]
  wire [63:0] dram0Converter_io_axi_readData_bits_data; // @[AXIWrapperTCU.scala 65:30]
  wire  dram0Converter_io_axi_readData_bits_last; // @[AXIWrapperTCU.scala 65:30]
  wire [31:0] dram0Converter_io_addressOffset; // @[AXIWrapperTCU.scala 65:30]
  wire [3:0] dram0Converter_io_cacheBehavior; // @[AXIWrapperTCU.scala 65:30]
  wire  dram0Converter_io_timeout; // @[AXIWrapperTCU.scala 65:30]
  wire  dram0Converter_io_tracepoint; // @[AXIWrapperTCU.scala 65:30]
  wire [31:0] dram0Converter_io_programCounter; // @[AXIWrapperTCU.scala 65:30]
  wire  dram0BoundarySplitter_io_in_q_clock; // @[Queue.scala 23:19]
  wire  dram0BoundarySplitter_io_in_q_reset; // @[Queue.scala 23:19]
  wire  dram0BoundarySplitter_io_in_q_io_in_writeAddress_ready; // @[Queue.scala 23:19]
  wire  dram0BoundarySplitter_io_in_q_io_in_writeAddress_valid; // @[Queue.scala 23:19]
  wire [31:0] dram0BoundarySplitter_io_in_q_io_in_writeAddress_bits_addr; // @[Queue.scala 23:19]
  wire [7:0] dram0BoundarySplitter_io_in_q_io_in_writeAddress_bits_len; // @[Queue.scala 23:19]
  wire [3:0] dram0BoundarySplitter_io_in_q_io_in_writeAddress_bits_cache; // @[Queue.scala 23:19]
  wire  dram0BoundarySplitter_io_in_q_io_in_writeData_ready; // @[Queue.scala 23:19]
  wire  dram0BoundarySplitter_io_in_q_io_in_writeData_valid; // @[Queue.scala 23:19]
  wire [63:0] dram0BoundarySplitter_io_in_q_io_in_writeData_bits_data; // @[Queue.scala 23:19]
  wire  dram0BoundarySplitter_io_in_q_io_in_writeResponse_ready; // @[Queue.scala 23:19]
  wire  dram0BoundarySplitter_io_in_q_io_in_writeResponse_valid; // @[Queue.scala 23:19]
  wire  dram0BoundarySplitter_io_in_q_io_in_readAddress_ready; // @[Queue.scala 23:19]
  wire  dram0BoundarySplitter_io_in_q_io_in_readAddress_valid; // @[Queue.scala 23:19]
  wire [31:0] dram0BoundarySplitter_io_in_q_io_in_readAddress_bits_addr; // @[Queue.scala 23:19]
  wire [7:0] dram0BoundarySplitter_io_in_q_io_in_readAddress_bits_len; // @[Queue.scala 23:19]
  wire [3:0] dram0BoundarySplitter_io_in_q_io_in_readAddress_bits_cache; // @[Queue.scala 23:19]
  wire  dram0BoundarySplitter_io_in_q_io_in_readData_ready; // @[Queue.scala 23:19]
  wire  dram0BoundarySplitter_io_in_q_io_in_readData_valid; // @[Queue.scala 23:19]
  wire [63:0] dram0BoundarySplitter_io_in_q_io_in_readData_bits_data; // @[Queue.scala 23:19]
  wire  dram0BoundarySplitter_io_in_q_io_in_readData_bits_last; // @[Queue.scala 23:19]
  wire  dram0BoundarySplitter_io_in_q_io_out_writeAddress_ready; // @[Queue.scala 23:19]
  wire  dram0BoundarySplitter_io_in_q_io_out_writeAddress_valid; // @[Queue.scala 23:19]
  wire [5:0] dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_id; // @[Queue.scala 23:19]
  wire [31:0] dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_addr; // @[Queue.scala 23:19]
  wire [7:0] dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_len; // @[Queue.scala 23:19]
  wire [2:0] dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_size; // @[Queue.scala 23:19]
  wire [1:0] dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_burst; // @[Queue.scala 23:19]
  wire [1:0] dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_lock; // @[Queue.scala 23:19]
  wire [3:0] dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_cache; // @[Queue.scala 23:19]
  wire [2:0] dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_prot; // @[Queue.scala 23:19]
  wire [3:0] dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_qos; // @[Queue.scala 23:19]
  wire  dram0BoundarySplitter_io_in_q_io_out_writeData_ready; // @[Queue.scala 23:19]
  wire  dram0BoundarySplitter_io_in_q_io_out_writeData_valid; // @[Queue.scala 23:19]
  wire [5:0] dram0BoundarySplitter_io_in_q_io_out_writeData_bits_id; // @[Queue.scala 23:19]
  wire [63:0] dram0BoundarySplitter_io_in_q_io_out_writeData_bits_data; // @[Queue.scala 23:19]
  wire [7:0] dram0BoundarySplitter_io_in_q_io_out_writeData_bits_strb; // @[Queue.scala 23:19]
  wire  dram0BoundarySplitter_io_in_q_io_out_writeResponse_ready; // @[Queue.scala 23:19]
  wire  dram0BoundarySplitter_io_in_q_io_out_writeResponse_valid; // @[Queue.scala 23:19]
  wire  dram0BoundarySplitter_io_in_q_io_out_readAddress_ready; // @[Queue.scala 23:19]
  wire  dram0BoundarySplitter_io_in_q_io_out_readAddress_valid; // @[Queue.scala 23:19]
  wire [5:0] dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_id; // @[Queue.scala 23:19]
  wire [31:0] dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_addr; // @[Queue.scala 23:19]
  wire [7:0] dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_len; // @[Queue.scala 23:19]
  wire [2:0] dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_size; // @[Queue.scala 23:19]
  wire [1:0] dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_burst; // @[Queue.scala 23:19]
  wire [1:0] dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_lock; // @[Queue.scala 23:19]
  wire [3:0] dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_cache; // @[Queue.scala 23:19]
  wire [2:0] dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_prot; // @[Queue.scala 23:19]
  wire [3:0] dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_qos; // @[Queue.scala 23:19]
  wire  dram0BoundarySplitter_io_in_q_io_out_readData_ready; // @[Queue.scala 23:19]
  wire  dram0BoundarySplitter_io_in_q_io_out_readData_valid; // @[Queue.scala 23:19]
  wire [63:0] dram0BoundarySplitter_io_in_q_io_out_readData_bits_data; // @[Queue.scala 23:19]
  wire  dram0BoundarySplitter_io_in_q_io_out_readData_bits_last; // @[Queue.scala 23:19]
  wire  dram1Converter_clock; // @[AXIWrapperTCU.scala 82:30]
  wire  dram1Converter_reset; // @[AXIWrapperTCU.scala 82:30]
  wire  dram1Converter_io_mem_control_ready; // @[AXIWrapperTCU.scala 82:30]
  wire  dram1Converter_io_mem_control_valid; // @[AXIWrapperTCU.scala 82:30]
  wire  dram1Converter_io_mem_control_bits_write; // @[AXIWrapperTCU.scala 82:30]
  wire [19:0] dram1Converter_io_mem_control_bits_address; // @[AXIWrapperTCU.scala 82:30]
  wire [19:0] dram1Converter_io_mem_control_bits_size; // @[AXIWrapperTCU.scala 82:30]
  wire  dram1Converter_io_mem_dataIn_ready; // @[AXIWrapperTCU.scala 82:30]
  wire  dram1Converter_io_mem_dataIn_valid; // @[AXIWrapperTCU.scala 82:30]
  wire [15:0] dram1Converter_io_mem_dataIn_bits_0; // @[AXIWrapperTCU.scala 82:30]
  wire [15:0] dram1Converter_io_mem_dataIn_bits_1; // @[AXIWrapperTCU.scala 82:30]
  wire [15:0] dram1Converter_io_mem_dataIn_bits_2; // @[AXIWrapperTCU.scala 82:30]
  wire [15:0] dram1Converter_io_mem_dataIn_bits_3; // @[AXIWrapperTCU.scala 82:30]
  wire [15:0] dram1Converter_io_mem_dataIn_bits_4; // @[AXIWrapperTCU.scala 82:30]
  wire [15:0] dram1Converter_io_mem_dataIn_bits_5; // @[AXIWrapperTCU.scala 82:30]
  wire [15:0] dram1Converter_io_mem_dataIn_bits_6; // @[AXIWrapperTCU.scala 82:30]
  wire [15:0] dram1Converter_io_mem_dataIn_bits_7; // @[AXIWrapperTCU.scala 82:30]
  wire  dram1Converter_io_mem_dataOut_ready; // @[AXIWrapperTCU.scala 82:30]
  wire  dram1Converter_io_mem_dataOut_valid; // @[AXIWrapperTCU.scala 82:30]
  wire [15:0] dram1Converter_io_mem_dataOut_bits_0; // @[AXIWrapperTCU.scala 82:30]
  wire [15:0] dram1Converter_io_mem_dataOut_bits_1; // @[AXIWrapperTCU.scala 82:30]
  wire [15:0] dram1Converter_io_mem_dataOut_bits_2; // @[AXIWrapperTCU.scala 82:30]
  wire [15:0] dram1Converter_io_mem_dataOut_bits_3; // @[AXIWrapperTCU.scala 82:30]
  wire [15:0] dram1Converter_io_mem_dataOut_bits_4; // @[AXIWrapperTCU.scala 82:30]
  wire [15:0] dram1Converter_io_mem_dataOut_bits_5; // @[AXIWrapperTCU.scala 82:30]
  wire [15:0] dram1Converter_io_mem_dataOut_bits_6; // @[AXIWrapperTCU.scala 82:30]
  wire [15:0] dram1Converter_io_mem_dataOut_bits_7; // @[AXIWrapperTCU.scala 82:30]
  wire  dram1Converter_io_axi_writeAddress_ready; // @[AXIWrapperTCU.scala 82:30]
  wire  dram1Converter_io_axi_writeAddress_valid; // @[AXIWrapperTCU.scala 82:30]
  wire [31:0] dram1Converter_io_axi_writeAddress_bits_addr; // @[AXIWrapperTCU.scala 82:30]
  wire [7:0] dram1Converter_io_axi_writeAddress_bits_len; // @[AXIWrapperTCU.scala 82:30]
  wire [3:0] dram1Converter_io_axi_writeAddress_bits_cache; // @[AXIWrapperTCU.scala 82:30]
  wire  dram1Converter_io_axi_writeData_ready; // @[AXIWrapperTCU.scala 82:30]
  wire  dram1Converter_io_axi_writeData_valid; // @[AXIWrapperTCU.scala 82:30]
  wire [63:0] dram1Converter_io_axi_writeData_bits_data; // @[AXIWrapperTCU.scala 82:30]
  wire  dram1Converter_io_axi_writeResponse_ready; // @[AXIWrapperTCU.scala 82:30]
  wire  dram1Converter_io_axi_writeResponse_valid; // @[AXIWrapperTCU.scala 82:30]
  wire  dram1Converter_io_axi_readAddress_ready; // @[AXIWrapperTCU.scala 82:30]
  wire  dram1Converter_io_axi_readAddress_valid; // @[AXIWrapperTCU.scala 82:30]
  wire [31:0] dram1Converter_io_axi_readAddress_bits_addr; // @[AXIWrapperTCU.scala 82:30]
  wire [7:0] dram1Converter_io_axi_readAddress_bits_len; // @[AXIWrapperTCU.scala 82:30]
  wire [3:0] dram1Converter_io_axi_readAddress_bits_cache; // @[AXIWrapperTCU.scala 82:30]
  wire  dram1Converter_io_axi_readData_ready; // @[AXIWrapperTCU.scala 82:30]
  wire  dram1Converter_io_axi_readData_valid; // @[AXIWrapperTCU.scala 82:30]
  wire [63:0] dram1Converter_io_axi_readData_bits_data; // @[AXIWrapperTCU.scala 82:30]
  wire  dram1Converter_io_axi_readData_bits_last; // @[AXIWrapperTCU.scala 82:30]
  wire [31:0] dram1Converter_io_addressOffset; // @[AXIWrapperTCU.scala 82:30]
  wire [3:0] dram1Converter_io_cacheBehavior; // @[AXIWrapperTCU.scala 82:30]
  wire  dram1Converter_io_timeout; // @[AXIWrapperTCU.scala 82:30]
  wire  dram1Converter_io_tracepoint; // @[AXIWrapperTCU.scala 82:30]
  wire [31:0] dram1Converter_io_programCounter; // @[AXIWrapperTCU.scala 82:30]
  wire  dram1BoundarySplitter_io_in_q_clock; // @[Queue.scala 23:19]
  wire  dram1BoundarySplitter_io_in_q_reset; // @[Queue.scala 23:19]
  wire  dram1BoundarySplitter_io_in_q_io_in_writeAddress_ready; // @[Queue.scala 23:19]
  wire  dram1BoundarySplitter_io_in_q_io_in_writeAddress_valid; // @[Queue.scala 23:19]
  wire [31:0] dram1BoundarySplitter_io_in_q_io_in_writeAddress_bits_addr; // @[Queue.scala 23:19]
  wire [7:0] dram1BoundarySplitter_io_in_q_io_in_writeAddress_bits_len; // @[Queue.scala 23:19]
  wire [3:0] dram1BoundarySplitter_io_in_q_io_in_writeAddress_bits_cache; // @[Queue.scala 23:19]
  wire  dram1BoundarySplitter_io_in_q_io_in_writeData_ready; // @[Queue.scala 23:19]
  wire  dram1BoundarySplitter_io_in_q_io_in_writeData_valid; // @[Queue.scala 23:19]
  wire [63:0] dram1BoundarySplitter_io_in_q_io_in_writeData_bits_data; // @[Queue.scala 23:19]
  wire  dram1BoundarySplitter_io_in_q_io_in_writeResponse_ready; // @[Queue.scala 23:19]
  wire  dram1BoundarySplitter_io_in_q_io_in_writeResponse_valid; // @[Queue.scala 23:19]
  wire  dram1BoundarySplitter_io_in_q_io_in_readAddress_ready; // @[Queue.scala 23:19]
  wire  dram1BoundarySplitter_io_in_q_io_in_readAddress_valid; // @[Queue.scala 23:19]
  wire [31:0] dram1BoundarySplitter_io_in_q_io_in_readAddress_bits_addr; // @[Queue.scala 23:19]
  wire [7:0] dram1BoundarySplitter_io_in_q_io_in_readAddress_bits_len; // @[Queue.scala 23:19]
  wire [3:0] dram1BoundarySplitter_io_in_q_io_in_readAddress_bits_cache; // @[Queue.scala 23:19]
  wire  dram1BoundarySplitter_io_in_q_io_in_readData_ready; // @[Queue.scala 23:19]
  wire  dram1BoundarySplitter_io_in_q_io_in_readData_valid; // @[Queue.scala 23:19]
  wire [63:0] dram1BoundarySplitter_io_in_q_io_in_readData_bits_data; // @[Queue.scala 23:19]
  wire  dram1BoundarySplitter_io_in_q_io_in_readData_bits_last; // @[Queue.scala 23:19]
  wire  dram1BoundarySplitter_io_in_q_io_out_writeAddress_ready; // @[Queue.scala 23:19]
  wire  dram1BoundarySplitter_io_in_q_io_out_writeAddress_valid; // @[Queue.scala 23:19]
  wire [5:0] dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_id; // @[Queue.scala 23:19]
  wire [31:0] dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_addr; // @[Queue.scala 23:19]
  wire [7:0] dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_len; // @[Queue.scala 23:19]
  wire [2:0] dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_size; // @[Queue.scala 23:19]
  wire [1:0] dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_burst; // @[Queue.scala 23:19]
  wire [1:0] dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_lock; // @[Queue.scala 23:19]
  wire [3:0] dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_cache; // @[Queue.scala 23:19]
  wire [2:0] dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_prot; // @[Queue.scala 23:19]
  wire [3:0] dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_qos; // @[Queue.scala 23:19]
  wire  dram1BoundarySplitter_io_in_q_io_out_writeData_ready; // @[Queue.scala 23:19]
  wire  dram1BoundarySplitter_io_in_q_io_out_writeData_valid; // @[Queue.scala 23:19]
  wire [5:0] dram1BoundarySplitter_io_in_q_io_out_writeData_bits_id; // @[Queue.scala 23:19]
  wire [63:0] dram1BoundarySplitter_io_in_q_io_out_writeData_bits_data; // @[Queue.scala 23:19]
  wire [7:0] dram1BoundarySplitter_io_in_q_io_out_writeData_bits_strb; // @[Queue.scala 23:19]
  wire  dram1BoundarySplitter_io_in_q_io_out_writeResponse_ready; // @[Queue.scala 23:19]
  wire  dram1BoundarySplitter_io_in_q_io_out_writeResponse_valid; // @[Queue.scala 23:19]
  wire  dram1BoundarySplitter_io_in_q_io_out_readAddress_ready; // @[Queue.scala 23:19]
  wire  dram1BoundarySplitter_io_in_q_io_out_readAddress_valid; // @[Queue.scala 23:19]
  wire [5:0] dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_id; // @[Queue.scala 23:19]
  wire [31:0] dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_addr; // @[Queue.scala 23:19]
  wire [7:0] dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_len; // @[Queue.scala 23:19]
  wire [2:0] dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_size; // @[Queue.scala 23:19]
  wire [1:0] dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_burst; // @[Queue.scala 23:19]
  wire [1:0] dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_lock; // @[Queue.scala 23:19]
  wire [3:0] dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_cache; // @[Queue.scala 23:19]
  wire [2:0] dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_prot; // @[Queue.scala 23:19]
  wire [3:0] dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_qos; // @[Queue.scala 23:19]
  wire  dram1BoundarySplitter_io_in_q_io_out_readData_ready; // @[Queue.scala 23:19]
  wire  dram1BoundarySplitter_io_in_q_io_out_readData_valid; // @[Queue.scala 23:19]
  wire [63:0] dram1BoundarySplitter_io_in_q_io_out_readData_bits_data; // @[Queue.scala 23:19]
  wire  dram1BoundarySplitter_io_in_q_io_out_readData_bits_last; // @[Queue.scala 23:19]
  TCU tcu ( // @[AXIWrapperTCU.scala 32:19]
    .clock(tcu_clock),
    .reset(tcu_reset),
    .io_instruction_ready(tcu_io_instruction_ready),
    .io_instruction_valid(tcu_io_instruction_valid),
    .io_instruction_bits_opcode(tcu_io_instruction_bits_opcode),
    .io_instruction_bits_flags(tcu_io_instruction_bits_flags),
    .io_instruction_bits_arguments(tcu_io_instruction_bits_arguments),
    .io_status_ready(tcu_io_status_ready),
    .io_status_valid(tcu_io_status_valid),
    .io_status_bits_last(tcu_io_status_bits_last),
    .io_status_bits_bits_opcode(tcu_io_status_bits_bits_opcode),
    .io_status_bits_bits_flags(tcu_io_status_bits_bits_flags),
    .io_status_bits_bits_arguments(tcu_io_status_bits_bits_arguments),
    .io_dram0_control_ready(tcu_io_dram0_control_ready),
    .io_dram0_control_valid(tcu_io_dram0_control_valid),
    .io_dram0_control_bits_write(tcu_io_dram0_control_bits_write),
    .io_dram0_control_bits_address(tcu_io_dram0_control_bits_address),
    .io_dram0_control_bits_size(tcu_io_dram0_control_bits_size),
    .io_dram0_dataIn_ready(tcu_io_dram0_dataIn_ready),
    .io_dram0_dataIn_valid(tcu_io_dram0_dataIn_valid),
    .io_dram0_dataIn_bits_0(tcu_io_dram0_dataIn_bits_0),
    .io_dram0_dataIn_bits_1(tcu_io_dram0_dataIn_bits_1),
    .io_dram0_dataIn_bits_2(tcu_io_dram0_dataIn_bits_2),
    .io_dram0_dataIn_bits_3(tcu_io_dram0_dataIn_bits_3),
    .io_dram0_dataIn_bits_4(tcu_io_dram0_dataIn_bits_4),
    .io_dram0_dataIn_bits_5(tcu_io_dram0_dataIn_bits_5),
    .io_dram0_dataIn_bits_6(tcu_io_dram0_dataIn_bits_6),
    .io_dram0_dataIn_bits_7(tcu_io_dram0_dataIn_bits_7),
    .io_dram0_dataOut_ready(tcu_io_dram0_dataOut_ready),
    .io_dram0_dataOut_valid(tcu_io_dram0_dataOut_valid),
    .io_dram0_dataOut_bits_0(tcu_io_dram0_dataOut_bits_0),
    .io_dram0_dataOut_bits_1(tcu_io_dram0_dataOut_bits_1),
    .io_dram0_dataOut_bits_2(tcu_io_dram0_dataOut_bits_2),
    .io_dram0_dataOut_bits_3(tcu_io_dram0_dataOut_bits_3),
    .io_dram0_dataOut_bits_4(tcu_io_dram0_dataOut_bits_4),
    .io_dram0_dataOut_bits_5(tcu_io_dram0_dataOut_bits_5),
    .io_dram0_dataOut_bits_6(tcu_io_dram0_dataOut_bits_6),
    .io_dram0_dataOut_bits_7(tcu_io_dram0_dataOut_bits_7),
    .io_dram1_control_ready(tcu_io_dram1_control_ready),
    .io_dram1_control_valid(tcu_io_dram1_control_valid),
    .io_dram1_control_bits_write(tcu_io_dram1_control_bits_write),
    .io_dram1_control_bits_address(tcu_io_dram1_control_bits_address),
    .io_dram1_control_bits_size(tcu_io_dram1_control_bits_size),
    .io_dram1_dataIn_ready(tcu_io_dram1_dataIn_ready),
    .io_dram1_dataIn_valid(tcu_io_dram1_dataIn_valid),
    .io_dram1_dataIn_bits_0(tcu_io_dram1_dataIn_bits_0),
    .io_dram1_dataIn_bits_1(tcu_io_dram1_dataIn_bits_1),
    .io_dram1_dataIn_bits_2(tcu_io_dram1_dataIn_bits_2),
    .io_dram1_dataIn_bits_3(tcu_io_dram1_dataIn_bits_3),
    .io_dram1_dataIn_bits_4(tcu_io_dram1_dataIn_bits_4),
    .io_dram1_dataIn_bits_5(tcu_io_dram1_dataIn_bits_5),
    .io_dram1_dataIn_bits_6(tcu_io_dram1_dataIn_bits_6),
    .io_dram1_dataIn_bits_7(tcu_io_dram1_dataIn_bits_7),
    .io_dram1_dataOut_ready(tcu_io_dram1_dataOut_ready),
    .io_dram1_dataOut_valid(tcu_io_dram1_dataOut_valid),
    .io_dram1_dataOut_bits_0(tcu_io_dram1_dataOut_bits_0),
    .io_dram1_dataOut_bits_1(tcu_io_dram1_dataOut_bits_1),
    .io_dram1_dataOut_bits_2(tcu_io_dram1_dataOut_bits_2),
    .io_dram1_dataOut_bits_3(tcu_io_dram1_dataOut_bits_3),
    .io_dram1_dataOut_bits_4(tcu_io_dram1_dataOut_bits_4),
    .io_dram1_dataOut_bits_5(tcu_io_dram1_dataOut_bits_5),
    .io_dram1_dataOut_bits_6(tcu_io_dram1_dataOut_bits_6),
    .io_dram1_dataOut_bits_7(tcu_io_dram1_dataOut_bits_7),
    .io_config_dram0AddressOffset(tcu_io_config_dram0AddressOffset),
    .io_config_dram0CacheBehaviour(tcu_io_config_dram0CacheBehaviour),
    .io_config_dram1AddressOffset(tcu_io_config_dram1AddressOffset),
    .io_config_dram1CacheBehaviour(tcu_io_config_dram1CacheBehaviour),
    .io_timeout(tcu_io_timeout),
    .io_error(tcu_io_error),
    .io_tracepoint(tcu_io_tracepoint),
    .io_programCounter(tcu_io_programCounter)
  );
  MemBoundarySplitter dram0BoundarySplitter ( // @[AXIWrapperTCU.scala 56:37]
    .clock(dram0BoundarySplitter_clock),
    .reset(dram0BoundarySplitter_reset),
    .io_in_writeAddress_ready(dram0BoundarySplitter_io_in_writeAddress_ready),
    .io_in_writeAddress_valid(dram0BoundarySplitter_io_in_writeAddress_valid),
    .io_in_writeAddress_bits_id(dram0BoundarySplitter_io_in_writeAddress_bits_id),
    .io_in_writeAddress_bits_addr(dram0BoundarySplitter_io_in_writeAddress_bits_addr),
    .io_in_writeAddress_bits_len(dram0BoundarySplitter_io_in_writeAddress_bits_len),
    .io_in_writeAddress_bits_size(dram0BoundarySplitter_io_in_writeAddress_bits_size),
    .io_in_writeAddress_bits_burst(dram0BoundarySplitter_io_in_writeAddress_bits_burst),
    .io_in_writeAddress_bits_lock(dram0BoundarySplitter_io_in_writeAddress_bits_lock),
    .io_in_writeAddress_bits_cache(dram0BoundarySplitter_io_in_writeAddress_bits_cache),
    .io_in_writeAddress_bits_prot(dram0BoundarySplitter_io_in_writeAddress_bits_prot),
    .io_in_writeAddress_bits_qos(dram0BoundarySplitter_io_in_writeAddress_bits_qos),
    .io_in_writeData_ready(dram0BoundarySplitter_io_in_writeData_ready),
    .io_in_writeData_valid(dram0BoundarySplitter_io_in_writeData_valid),
    .io_in_writeData_bits_id(dram0BoundarySplitter_io_in_writeData_bits_id),
    .io_in_writeData_bits_data(dram0BoundarySplitter_io_in_writeData_bits_data),
    .io_in_writeData_bits_strb(dram0BoundarySplitter_io_in_writeData_bits_strb),
    .io_in_writeResponse_ready(dram0BoundarySplitter_io_in_writeResponse_ready),
    .io_in_writeResponse_valid(dram0BoundarySplitter_io_in_writeResponse_valid),
    .io_in_readAddress_ready(dram0BoundarySplitter_io_in_readAddress_ready),
    .io_in_readAddress_valid(dram0BoundarySplitter_io_in_readAddress_valid),
    .io_in_readAddress_bits_id(dram0BoundarySplitter_io_in_readAddress_bits_id),
    .io_in_readAddress_bits_addr(dram0BoundarySplitter_io_in_readAddress_bits_addr),
    .io_in_readAddress_bits_len(dram0BoundarySplitter_io_in_readAddress_bits_len),
    .io_in_readAddress_bits_size(dram0BoundarySplitter_io_in_readAddress_bits_size),
    .io_in_readAddress_bits_burst(dram0BoundarySplitter_io_in_readAddress_bits_burst),
    .io_in_readAddress_bits_lock(dram0BoundarySplitter_io_in_readAddress_bits_lock),
    .io_in_readAddress_bits_cache(dram0BoundarySplitter_io_in_readAddress_bits_cache),
    .io_in_readAddress_bits_prot(dram0BoundarySplitter_io_in_readAddress_bits_prot),
    .io_in_readAddress_bits_qos(dram0BoundarySplitter_io_in_readAddress_bits_qos),
    .io_in_readData_ready(dram0BoundarySplitter_io_in_readData_ready),
    .io_in_readData_valid(dram0BoundarySplitter_io_in_readData_valid),
    .io_in_readData_bits_data(dram0BoundarySplitter_io_in_readData_bits_data),
    .io_in_readData_bits_last(dram0BoundarySplitter_io_in_readData_bits_last),
    .io_out_writeAddress_ready(dram0BoundarySplitter_io_out_writeAddress_ready),
    .io_out_writeAddress_valid(dram0BoundarySplitter_io_out_writeAddress_valid),
    .io_out_writeAddress_bits_id(dram0BoundarySplitter_io_out_writeAddress_bits_id),
    .io_out_writeAddress_bits_addr(dram0BoundarySplitter_io_out_writeAddress_bits_addr),
    .io_out_writeAddress_bits_len(dram0BoundarySplitter_io_out_writeAddress_bits_len),
    .io_out_writeAddress_bits_size(dram0BoundarySplitter_io_out_writeAddress_bits_size),
    .io_out_writeAddress_bits_burst(dram0BoundarySplitter_io_out_writeAddress_bits_burst),
    .io_out_writeAddress_bits_lock(dram0BoundarySplitter_io_out_writeAddress_bits_lock),
    .io_out_writeAddress_bits_cache(dram0BoundarySplitter_io_out_writeAddress_bits_cache),
    .io_out_writeAddress_bits_prot(dram0BoundarySplitter_io_out_writeAddress_bits_prot),
    .io_out_writeAddress_bits_qos(dram0BoundarySplitter_io_out_writeAddress_bits_qos),
    .io_out_writeData_ready(dram0BoundarySplitter_io_out_writeData_ready),
    .io_out_writeData_valid(dram0BoundarySplitter_io_out_writeData_valid),
    .io_out_writeData_bits_id(dram0BoundarySplitter_io_out_writeData_bits_id),
    .io_out_writeData_bits_data(dram0BoundarySplitter_io_out_writeData_bits_data),
    .io_out_writeData_bits_strb(dram0BoundarySplitter_io_out_writeData_bits_strb),
    .io_out_writeData_bits_last(dram0BoundarySplitter_io_out_writeData_bits_last),
    .io_out_writeResponse_ready(dram0BoundarySplitter_io_out_writeResponse_ready),
    .io_out_writeResponse_valid(dram0BoundarySplitter_io_out_writeResponse_valid),
    .io_out_readAddress_ready(dram0BoundarySplitter_io_out_readAddress_ready),
    .io_out_readAddress_valid(dram0BoundarySplitter_io_out_readAddress_valid),
    .io_out_readAddress_bits_id(dram0BoundarySplitter_io_out_readAddress_bits_id),
    .io_out_readAddress_bits_addr(dram0BoundarySplitter_io_out_readAddress_bits_addr),
    .io_out_readAddress_bits_len(dram0BoundarySplitter_io_out_readAddress_bits_len),
    .io_out_readAddress_bits_size(dram0BoundarySplitter_io_out_readAddress_bits_size),
    .io_out_readAddress_bits_burst(dram0BoundarySplitter_io_out_readAddress_bits_burst),
    .io_out_readAddress_bits_lock(dram0BoundarySplitter_io_out_readAddress_bits_lock),
    .io_out_readAddress_bits_cache(dram0BoundarySplitter_io_out_readAddress_bits_cache),
    .io_out_readAddress_bits_prot(dram0BoundarySplitter_io_out_readAddress_bits_prot),
    .io_out_readAddress_bits_qos(dram0BoundarySplitter_io_out_readAddress_bits_qos),
    .io_out_readData_ready(dram0BoundarySplitter_io_out_readData_ready),
    .io_out_readData_valid(dram0BoundarySplitter_io_out_readData_valid),
    .io_out_readData_bits_data(dram0BoundarySplitter_io_out_readData_bits_data)
  );
  MemBoundarySplitter dram1BoundarySplitter ( // @[AXIWrapperTCU.scala 60:37]
    .clock(dram1BoundarySplitter_clock),
    .reset(dram1BoundarySplitter_reset),
    .io_in_writeAddress_ready(dram1BoundarySplitter_io_in_writeAddress_ready),
    .io_in_writeAddress_valid(dram1BoundarySplitter_io_in_writeAddress_valid),
    .io_in_writeAddress_bits_id(dram1BoundarySplitter_io_in_writeAddress_bits_id),
    .io_in_writeAddress_bits_addr(dram1BoundarySplitter_io_in_writeAddress_bits_addr),
    .io_in_writeAddress_bits_len(dram1BoundarySplitter_io_in_writeAddress_bits_len),
    .io_in_writeAddress_bits_size(dram1BoundarySplitter_io_in_writeAddress_bits_size),
    .io_in_writeAddress_bits_burst(dram1BoundarySplitter_io_in_writeAddress_bits_burst),
    .io_in_writeAddress_bits_lock(dram1BoundarySplitter_io_in_writeAddress_bits_lock),
    .io_in_writeAddress_bits_cache(dram1BoundarySplitter_io_in_writeAddress_bits_cache),
    .io_in_writeAddress_bits_prot(dram1BoundarySplitter_io_in_writeAddress_bits_prot),
    .io_in_writeAddress_bits_qos(dram1BoundarySplitter_io_in_writeAddress_bits_qos),
    .io_in_writeData_ready(dram1BoundarySplitter_io_in_writeData_ready),
    .io_in_writeData_valid(dram1BoundarySplitter_io_in_writeData_valid),
    .io_in_writeData_bits_id(dram1BoundarySplitter_io_in_writeData_bits_id),
    .io_in_writeData_bits_data(dram1BoundarySplitter_io_in_writeData_bits_data),
    .io_in_writeData_bits_strb(dram1BoundarySplitter_io_in_writeData_bits_strb),
    .io_in_writeResponse_ready(dram1BoundarySplitter_io_in_writeResponse_ready),
    .io_in_writeResponse_valid(dram1BoundarySplitter_io_in_writeResponse_valid),
    .io_in_readAddress_ready(dram1BoundarySplitter_io_in_readAddress_ready),
    .io_in_readAddress_valid(dram1BoundarySplitter_io_in_readAddress_valid),
    .io_in_readAddress_bits_id(dram1BoundarySplitter_io_in_readAddress_bits_id),
    .io_in_readAddress_bits_addr(dram1BoundarySplitter_io_in_readAddress_bits_addr),
    .io_in_readAddress_bits_len(dram1BoundarySplitter_io_in_readAddress_bits_len),
    .io_in_readAddress_bits_size(dram1BoundarySplitter_io_in_readAddress_bits_size),
    .io_in_readAddress_bits_burst(dram1BoundarySplitter_io_in_readAddress_bits_burst),
    .io_in_readAddress_bits_lock(dram1BoundarySplitter_io_in_readAddress_bits_lock),
    .io_in_readAddress_bits_cache(dram1BoundarySplitter_io_in_readAddress_bits_cache),
    .io_in_readAddress_bits_prot(dram1BoundarySplitter_io_in_readAddress_bits_prot),
    .io_in_readAddress_bits_qos(dram1BoundarySplitter_io_in_readAddress_bits_qos),
    .io_in_readData_ready(dram1BoundarySplitter_io_in_readData_ready),
    .io_in_readData_valid(dram1BoundarySplitter_io_in_readData_valid),
    .io_in_readData_bits_data(dram1BoundarySplitter_io_in_readData_bits_data),
    .io_in_readData_bits_last(dram1BoundarySplitter_io_in_readData_bits_last),
    .io_out_writeAddress_ready(dram1BoundarySplitter_io_out_writeAddress_ready),
    .io_out_writeAddress_valid(dram1BoundarySplitter_io_out_writeAddress_valid),
    .io_out_writeAddress_bits_id(dram1BoundarySplitter_io_out_writeAddress_bits_id),
    .io_out_writeAddress_bits_addr(dram1BoundarySplitter_io_out_writeAddress_bits_addr),
    .io_out_writeAddress_bits_len(dram1BoundarySplitter_io_out_writeAddress_bits_len),
    .io_out_writeAddress_bits_size(dram1BoundarySplitter_io_out_writeAddress_bits_size),
    .io_out_writeAddress_bits_burst(dram1BoundarySplitter_io_out_writeAddress_bits_burst),
    .io_out_writeAddress_bits_lock(dram1BoundarySplitter_io_out_writeAddress_bits_lock),
    .io_out_writeAddress_bits_cache(dram1BoundarySplitter_io_out_writeAddress_bits_cache),
    .io_out_writeAddress_bits_prot(dram1BoundarySplitter_io_out_writeAddress_bits_prot),
    .io_out_writeAddress_bits_qos(dram1BoundarySplitter_io_out_writeAddress_bits_qos),
    .io_out_writeData_ready(dram1BoundarySplitter_io_out_writeData_ready),
    .io_out_writeData_valid(dram1BoundarySplitter_io_out_writeData_valid),
    .io_out_writeData_bits_id(dram1BoundarySplitter_io_out_writeData_bits_id),
    .io_out_writeData_bits_data(dram1BoundarySplitter_io_out_writeData_bits_data),
    .io_out_writeData_bits_strb(dram1BoundarySplitter_io_out_writeData_bits_strb),
    .io_out_writeData_bits_last(dram1BoundarySplitter_io_out_writeData_bits_last),
    .io_out_writeResponse_ready(dram1BoundarySplitter_io_out_writeResponse_ready),
    .io_out_writeResponse_valid(dram1BoundarySplitter_io_out_writeResponse_valid),
    .io_out_readAddress_ready(dram1BoundarySplitter_io_out_readAddress_ready),
    .io_out_readAddress_valid(dram1BoundarySplitter_io_out_readAddress_valid),
    .io_out_readAddress_bits_id(dram1BoundarySplitter_io_out_readAddress_bits_id),
    .io_out_readAddress_bits_addr(dram1BoundarySplitter_io_out_readAddress_bits_addr),
    .io_out_readAddress_bits_len(dram1BoundarySplitter_io_out_readAddress_bits_len),
    .io_out_readAddress_bits_size(dram1BoundarySplitter_io_out_readAddress_bits_size),
    .io_out_readAddress_bits_burst(dram1BoundarySplitter_io_out_readAddress_bits_burst),
    .io_out_readAddress_bits_lock(dram1BoundarySplitter_io_out_readAddress_bits_lock),
    .io_out_readAddress_bits_cache(dram1BoundarySplitter_io_out_readAddress_bits_cache),
    .io_out_readAddress_bits_prot(dram1BoundarySplitter_io_out_readAddress_bits_prot),
    .io_out_readAddress_bits_qos(dram1BoundarySplitter_io_out_readAddress_bits_qos),
    .io_out_readData_ready(dram1BoundarySplitter_io_out_readData_ready),
    .io_out_readData_valid(dram1BoundarySplitter_io_out_readData_valid),
    .io_out_readData_bits_data(dram1BoundarySplitter_io_out_readData_bits_data)
  );
  Converter dram0Converter ( // @[AXIWrapperTCU.scala 65:30]
    .clock(dram0Converter_clock),
    .reset(dram0Converter_reset),
    .io_mem_control_ready(dram0Converter_io_mem_control_ready),
    .io_mem_control_valid(dram0Converter_io_mem_control_valid),
    .io_mem_control_bits_write(dram0Converter_io_mem_control_bits_write),
    .io_mem_control_bits_address(dram0Converter_io_mem_control_bits_address),
    .io_mem_control_bits_size(dram0Converter_io_mem_control_bits_size),
    .io_mem_dataIn_ready(dram0Converter_io_mem_dataIn_ready),
    .io_mem_dataIn_valid(dram0Converter_io_mem_dataIn_valid),
    .io_mem_dataIn_bits_0(dram0Converter_io_mem_dataIn_bits_0),
    .io_mem_dataIn_bits_1(dram0Converter_io_mem_dataIn_bits_1),
    .io_mem_dataIn_bits_2(dram0Converter_io_mem_dataIn_bits_2),
    .io_mem_dataIn_bits_3(dram0Converter_io_mem_dataIn_bits_3),
    .io_mem_dataIn_bits_4(dram0Converter_io_mem_dataIn_bits_4),
    .io_mem_dataIn_bits_5(dram0Converter_io_mem_dataIn_bits_5),
    .io_mem_dataIn_bits_6(dram0Converter_io_mem_dataIn_bits_6),
    .io_mem_dataIn_bits_7(dram0Converter_io_mem_dataIn_bits_7),
    .io_mem_dataOut_ready(dram0Converter_io_mem_dataOut_ready),
    .io_mem_dataOut_valid(dram0Converter_io_mem_dataOut_valid),
    .io_mem_dataOut_bits_0(dram0Converter_io_mem_dataOut_bits_0),
    .io_mem_dataOut_bits_1(dram0Converter_io_mem_dataOut_bits_1),
    .io_mem_dataOut_bits_2(dram0Converter_io_mem_dataOut_bits_2),
    .io_mem_dataOut_bits_3(dram0Converter_io_mem_dataOut_bits_3),
    .io_mem_dataOut_bits_4(dram0Converter_io_mem_dataOut_bits_4),
    .io_mem_dataOut_bits_5(dram0Converter_io_mem_dataOut_bits_5),
    .io_mem_dataOut_bits_6(dram0Converter_io_mem_dataOut_bits_6),
    .io_mem_dataOut_bits_7(dram0Converter_io_mem_dataOut_bits_7),
    .io_axi_writeAddress_ready(dram0Converter_io_axi_writeAddress_ready),
    .io_axi_writeAddress_valid(dram0Converter_io_axi_writeAddress_valid),
    .io_axi_writeAddress_bits_addr(dram0Converter_io_axi_writeAddress_bits_addr),
    .io_axi_writeAddress_bits_len(dram0Converter_io_axi_writeAddress_bits_len),
    .io_axi_writeAddress_bits_cache(dram0Converter_io_axi_writeAddress_bits_cache),
    .io_axi_writeData_ready(dram0Converter_io_axi_writeData_ready),
    .io_axi_writeData_valid(dram0Converter_io_axi_writeData_valid),
    .io_axi_writeData_bits_data(dram0Converter_io_axi_writeData_bits_data),
    .io_axi_writeResponse_ready(dram0Converter_io_axi_writeResponse_ready),
    .io_axi_writeResponse_valid(dram0Converter_io_axi_writeResponse_valid),
    .io_axi_readAddress_ready(dram0Converter_io_axi_readAddress_ready),
    .io_axi_readAddress_valid(dram0Converter_io_axi_readAddress_valid),
    .io_axi_readAddress_bits_addr(dram0Converter_io_axi_readAddress_bits_addr),
    .io_axi_readAddress_bits_len(dram0Converter_io_axi_readAddress_bits_len),
    .io_axi_readAddress_bits_cache(dram0Converter_io_axi_readAddress_bits_cache),
    .io_axi_readData_ready(dram0Converter_io_axi_readData_ready),
    .io_axi_readData_valid(dram0Converter_io_axi_readData_valid),
    .io_axi_readData_bits_data(dram0Converter_io_axi_readData_bits_data),
    .io_axi_readData_bits_last(dram0Converter_io_axi_readData_bits_last),
    .io_addressOffset(dram0Converter_io_addressOffset),
    .io_cacheBehavior(dram0Converter_io_cacheBehavior),
    .io_timeout(dram0Converter_io_timeout),
    .io_tracepoint(dram0Converter_io_tracepoint),
    .io_programCounter(dram0Converter_io_programCounter)
  );
  Queue_43 dram0BoundarySplitter_io_in_q ( // @[Queue.scala 23:19]
    .clock(dram0BoundarySplitter_io_in_q_clock),
    .reset(dram0BoundarySplitter_io_in_q_reset),
    .io_in_writeAddress_ready(dram0BoundarySplitter_io_in_q_io_in_writeAddress_ready),
    .io_in_writeAddress_valid(dram0BoundarySplitter_io_in_q_io_in_writeAddress_valid),
    .io_in_writeAddress_bits_addr(dram0BoundarySplitter_io_in_q_io_in_writeAddress_bits_addr),
    .io_in_writeAddress_bits_len(dram0BoundarySplitter_io_in_q_io_in_writeAddress_bits_len),
    .io_in_writeAddress_bits_cache(dram0BoundarySplitter_io_in_q_io_in_writeAddress_bits_cache),
    .io_in_writeData_ready(dram0BoundarySplitter_io_in_q_io_in_writeData_ready),
    .io_in_writeData_valid(dram0BoundarySplitter_io_in_q_io_in_writeData_valid),
    .io_in_writeData_bits_data(dram0BoundarySplitter_io_in_q_io_in_writeData_bits_data),
    .io_in_writeResponse_ready(dram0BoundarySplitter_io_in_q_io_in_writeResponse_ready),
    .io_in_writeResponse_valid(dram0BoundarySplitter_io_in_q_io_in_writeResponse_valid),
    .io_in_readAddress_ready(dram0BoundarySplitter_io_in_q_io_in_readAddress_ready),
    .io_in_readAddress_valid(dram0BoundarySplitter_io_in_q_io_in_readAddress_valid),
    .io_in_readAddress_bits_addr(dram0BoundarySplitter_io_in_q_io_in_readAddress_bits_addr),
    .io_in_readAddress_bits_len(dram0BoundarySplitter_io_in_q_io_in_readAddress_bits_len),
    .io_in_readAddress_bits_cache(dram0BoundarySplitter_io_in_q_io_in_readAddress_bits_cache),
    .io_in_readData_ready(dram0BoundarySplitter_io_in_q_io_in_readData_ready),
    .io_in_readData_valid(dram0BoundarySplitter_io_in_q_io_in_readData_valid),
    .io_in_readData_bits_data(dram0BoundarySplitter_io_in_q_io_in_readData_bits_data),
    .io_in_readData_bits_last(dram0BoundarySplitter_io_in_q_io_in_readData_bits_last),
    .io_out_writeAddress_ready(dram0BoundarySplitter_io_in_q_io_out_writeAddress_ready),
    .io_out_writeAddress_valid(dram0BoundarySplitter_io_in_q_io_out_writeAddress_valid),
    .io_out_writeAddress_bits_id(dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_id),
    .io_out_writeAddress_bits_addr(dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_addr),
    .io_out_writeAddress_bits_len(dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_len),
    .io_out_writeAddress_bits_size(dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_size),
    .io_out_writeAddress_bits_burst(dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_burst),
    .io_out_writeAddress_bits_lock(dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_lock),
    .io_out_writeAddress_bits_cache(dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_cache),
    .io_out_writeAddress_bits_prot(dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_prot),
    .io_out_writeAddress_bits_qos(dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_qos),
    .io_out_writeData_ready(dram0BoundarySplitter_io_in_q_io_out_writeData_ready),
    .io_out_writeData_valid(dram0BoundarySplitter_io_in_q_io_out_writeData_valid),
    .io_out_writeData_bits_id(dram0BoundarySplitter_io_in_q_io_out_writeData_bits_id),
    .io_out_writeData_bits_data(dram0BoundarySplitter_io_in_q_io_out_writeData_bits_data),
    .io_out_writeData_bits_strb(dram0BoundarySplitter_io_in_q_io_out_writeData_bits_strb),
    .io_out_writeResponse_ready(dram0BoundarySplitter_io_in_q_io_out_writeResponse_ready),
    .io_out_writeResponse_valid(dram0BoundarySplitter_io_in_q_io_out_writeResponse_valid),
    .io_out_readAddress_ready(dram0BoundarySplitter_io_in_q_io_out_readAddress_ready),
    .io_out_readAddress_valid(dram0BoundarySplitter_io_in_q_io_out_readAddress_valid),
    .io_out_readAddress_bits_id(dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_id),
    .io_out_readAddress_bits_addr(dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_addr),
    .io_out_readAddress_bits_len(dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_len),
    .io_out_readAddress_bits_size(dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_size),
    .io_out_readAddress_bits_burst(dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_burst),
    .io_out_readAddress_bits_lock(dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_lock),
    .io_out_readAddress_bits_cache(dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_cache),
    .io_out_readAddress_bits_prot(dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_prot),
    .io_out_readAddress_bits_qos(dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_qos),
    .io_out_readData_ready(dram0BoundarySplitter_io_in_q_io_out_readData_ready),
    .io_out_readData_valid(dram0BoundarySplitter_io_in_q_io_out_readData_valid),
    .io_out_readData_bits_data(dram0BoundarySplitter_io_in_q_io_out_readData_bits_data),
    .io_out_readData_bits_last(dram0BoundarySplitter_io_in_q_io_out_readData_bits_last)
  );
  Converter dram1Converter ( // @[AXIWrapperTCU.scala 82:30]
    .clock(dram1Converter_clock),
    .reset(dram1Converter_reset),
    .io_mem_control_ready(dram1Converter_io_mem_control_ready),
    .io_mem_control_valid(dram1Converter_io_mem_control_valid),
    .io_mem_control_bits_write(dram1Converter_io_mem_control_bits_write),
    .io_mem_control_bits_address(dram1Converter_io_mem_control_bits_address),
    .io_mem_control_bits_size(dram1Converter_io_mem_control_bits_size),
    .io_mem_dataIn_ready(dram1Converter_io_mem_dataIn_ready),
    .io_mem_dataIn_valid(dram1Converter_io_mem_dataIn_valid),
    .io_mem_dataIn_bits_0(dram1Converter_io_mem_dataIn_bits_0),
    .io_mem_dataIn_bits_1(dram1Converter_io_mem_dataIn_bits_1),
    .io_mem_dataIn_bits_2(dram1Converter_io_mem_dataIn_bits_2),
    .io_mem_dataIn_bits_3(dram1Converter_io_mem_dataIn_bits_3),
    .io_mem_dataIn_bits_4(dram1Converter_io_mem_dataIn_bits_4),
    .io_mem_dataIn_bits_5(dram1Converter_io_mem_dataIn_bits_5),
    .io_mem_dataIn_bits_6(dram1Converter_io_mem_dataIn_bits_6),
    .io_mem_dataIn_bits_7(dram1Converter_io_mem_dataIn_bits_7),
    .io_mem_dataOut_ready(dram1Converter_io_mem_dataOut_ready),
    .io_mem_dataOut_valid(dram1Converter_io_mem_dataOut_valid),
    .io_mem_dataOut_bits_0(dram1Converter_io_mem_dataOut_bits_0),
    .io_mem_dataOut_bits_1(dram1Converter_io_mem_dataOut_bits_1),
    .io_mem_dataOut_bits_2(dram1Converter_io_mem_dataOut_bits_2),
    .io_mem_dataOut_bits_3(dram1Converter_io_mem_dataOut_bits_3),
    .io_mem_dataOut_bits_4(dram1Converter_io_mem_dataOut_bits_4),
    .io_mem_dataOut_bits_5(dram1Converter_io_mem_dataOut_bits_5),
    .io_mem_dataOut_bits_6(dram1Converter_io_mem_dataOut_bits_6),
    .io_mem_dataOut_bits_7(dram1Converter_io_mem_dataOut_bits_7),
    .io_axi_writeAddress_ready(dram1Converter_io_axi_writeAddress_ready),
    .io_axi_writeAddress_valid(dram1Converter_io_axi_writeAddress_valid),
    .io_axi_writeAddress_bits_addr(dram1Converter_io_axi_writeAddress_bits_addr),
    .io_axi_writeAddress_bits_len(dram1Converter_io_axi_writeAddress_bits_len),
    .io_axi_writeAddress_bits_cache(dram1Converter_io_axi_writeAddress_bits_cache),
    .io_axi_writeData_ready(dram1Converter_io_axi_writeData_ready),
    .io_axi_writeData_valid(dram1Converter_io_axi_writeData_valid),
    .io_axi_writeData_bits_data(dram1Converter_io_axi_writeData_bits_data),
    .io_axi_writeResponse_ready(dram1Converter_io_axi_writeResponse_ready),
    .io_axi_writeResponse_valid(dram1Converter_io_axi_writeResponse_valid),
    .io_axi_readAddress_ready(dram1Converter_io_axi_readAddress_ready),
    .io_axi_readAddress_valid(dram1Converter_io_axi_readAddress_valid),
    .io_axi_readAddress_bits_addr(dram1Converter_io_axi_readAddress_bits_addr),
    .io_axi_readAddress_bits_len(dram1Converter_io_axi_readAddress_bits_len),
    .io_axi_readAddress_bits_cache(dram1Converter_io_axi_readAddress_bits_cache),
    .io_axi_readData_ready(dram1Converter_io_axi_readData_ready),
    .io_axi_readData_valid(dram1Converter_io_axi_readData_valid),
    .io_axi_readData_bits_data(dram1Converter_io_axi_readData_bits_data),
    .io_axi_readData_bits_last(dram1Converter_io_axi_readData_bits_last),
    .io_addressOffset(dram1Converter_io_addressOffset),
    .io_cacheBehavior(dram1Converter_io_cacheBehavior),
    .io_timeout(dram1Converter_io_timeout),
    .io_tracepoint(dram1Converter_io_tracepoint),
    .io_programCounter(dram1Converter_io_programCounter)
  );
  Queue_43 dram1BoundarySplitter_io_in_q ( // @[Queue.scala 23:19]
    .clock(dram1BoundarySplitter_io_in_q_clock),
    .reset(dram1BoundarySplitter_io_in_q_reset),
    .io_in_writeAddress_ready(dram1BoundarySplitter_io_in_q_io_in_writeAddress_ready),
    .io_in_writeAddress_valid(dram1BoundarySplitter_io_in_q_io_in_writeAddress_valid),
    .io_in_writeAddress_bits_addr(dram1BoundarySplitter_io_in_q_io_in_writeAddress_bits_addr),
    .io_in_writeAddress_bits_len(dram1BoundarySplitter_io_in_q_io_in_writeAddress_bits_len),
    .io_in_writeAddress_bits_cache(dram1BoundarySplitter_io_in_q_io_in_writeAddress_bits_cache),
    .io_in_writeData_ready(dram1BoundarySplitter_io_in_q_io_in_writeData_ready),
    .io_in_writeData_valid(dram1BoundarySplitter_io_in_q_io_in_writeData_valid),
    .io_in_writeData_bits_data(dram1BoundarySplitter_io_in_q_io_in_writeData_bits_data),
    .io_in_writeResponse_ready(dram1BoundarySplitter_io_in_q_io_in_writeResponse_ready),
    .io_in_writeResponse_valid(dram1BoundarySplitter_io_in_q_io_in_writeResponse_valid),
    .io_in_readAddress_ready(dram1BoundarySplitter_io_in_q_io_in_readAddress_ready),
    .io_in_readAddress_valid(dram1BoundarySplitter_io_in_q_io_in_readAddress_valid),
    .io_in_readAddress_bits_addr(dram1BoundarySplitter_io_in_q_io_in_readAddress_bits_addr),
    .io_in_readAddress_bits_len(dram1BoundarySplitter_io_in_q_io_in_readAddress_bits_len),
    .io_in_readAddress_bits_cache(dram1BoundarySplitter_io_in_q_io_in_readAddress_bits_cache),
    .io_in_readData_ready(dram1BoundarySplitter_io_in_q_io_in_readData_ready),
    .io_in_readData_valid(dram1BoundarySplitter_io_in_q_io_in_readData_valid),
    .io_in_readData_bits_data(dram1BoundarySplitter_io_in_q_io_in_readData_bits_data),
    .io_in_readData_bits_last(dram1BoundarySplitter_io_in_q_io_in_readData_bits_last),
    .io_out_writeAddress_ready(dram1BoundarySplitter_io_in_q_io_out_writeAddress_ready),
    .io_out_writeAddress_valid(dram1BoundarySplitter_io_in_q_io_out_writeAddress_valid),
    .io_out_writeAddress_bits_id(dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_id),
    .io_out_writeAddress_bits_addr(dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_addr),
    .io_out_writeAddress_bits_len(dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_len),
    .io_out_writeAddress_bits_size(dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_size),
    .io_out_writeAddress_bits_burst(dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_burst),
    .io_out_writeAddress_bits_lock(dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_lock),
    .io_out_writeAddress_bits_cache(dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_cache),
    .io_out_writeAddress_bits_prot(dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_prot),
    .io_out_writeAddress_bits_qos(dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_qos),
    .io_out_writeData_ready(dram1BoundarySplitter_io_in_q_io_out_writeData_ready),
    .io_out_writeData_valid(dram1BoundarySplitter_io_in_q_io_out_writeData_valid),
    .io_out_writeData_bits_id(dram1BoundarySplitter_io_in_q_io_out_writeData_bits_id),
    .io_out_writeData_bits_data(dram1BoundarySplitter_io_in_q_io_out_writeData_bits_data),
    .io_out_writeData_bits_strb(dram1BoundarySplitter_io_in_q_io_out_writeData_bits_strb),
    .io_out_writeResponse_ready(dram1BoundarySplitter_io_in_q_io_out_writeResponse_ready),
    .io_out_writeResponse_valid(dram1BoundarySplitter_io_in_q_io_out_writeResponse_valid),
    .io_out_readAddress_ready(dram1BoundarySplitter_io_in_q_io_out_readAddress_ready),
    .io_out_readAddress_valid(dram1BoundarySplitter_io_in_q_io_out_readAddress_valid),
    .io_out_readAddress_bits_id(dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_id),
    .io_out_readAddress_bits_addr(dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_addr),
    .io_out_readAddress_bits_len(dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_len),
    .io_out_readAddress_bits_size(dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_size),
    .io_out_readAddress_bits_burst(dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_burst),
    .io_out_readAddress_bits_lock(dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_lock),
    .io_out_readAddress_bits_cache(dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_cache),
    .io_out_readAddress_bits_prot(dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_prot),
    .io_out_readAddress_bits_qos(dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_qos),
    .io_out_readData_ready(dram1BoundarySplitter_io_in_q_io_out_readData_ready),
    .io_out_readData_valid(dram1BoundarySplitter_io_in_q_io_out_readData_valid),
    .io_out_readData_bits_data(dram1BoundarySplitter_io_in_q_io_out_readData_bits_data),
    .io_out_readData_bits_last(dram1BoundarySplitter_io_in_q_io_out_readData_bits_last)
  );
  assign instruction_ready = tcu_io_instruction_ready; // @[AXIWrapperTCU.scala 47:22]
  assign status_valid = tcu_io_status_valid; // @[AXIWrapperTCU.scala 48:10]
  assign status_bits_last = tcu_io_status_bits_last; // @[AXIWrapperTCU.scala 48:10]
  assign status_bits_bits_opcode = tcu_io_status_bits_bits_opcode; // @[AXIWrapperTCU.scala 48:10]
  assign status_bits_bits_flags = tcu_io_status_bits_bits_flags; // @[AXIWrapperTCU.scala 48:10]
  assign status_bits_bits_arguments = tcu_io_status_bits_bits_arguments; // @[AXIWrapperTCU.scala 48:10]
  assign dram0_writeAddress_valid = dram0BoundarySplitter_io_out_writeAddress_valid; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_writeAddress_bits_id = dram0BoundarySplitter_io_out_writeAddress_bits_id; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_writeAddress_bits_addr = dram0BoundarySplitter_io_out_writeAddress_bits_addr; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_writeAddress_bits_len = dram0BoundarySplitter_io_out_writeAddress_bits_len; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_writeAddress_bits_size = dram0BoundarySplitter_io_out_writeAddress_bits_size; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_writeAddress_bits_burst = dram0BoundarySplitter_io_out_writeAddress_bits_burst; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_writeAddress_bits_lock = dram0BoundarySplitter_io_out_writeAddress_bits_lock; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_writeAddress_bits_cache = dram0BoundarySplitter_io_out_writeAddress_bits_cache; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_writeAddress_bits_prot = dram0BoundarySplitter_io_out_writeAddress_bits_prot; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_writeAddress_bits_qos = dram0BoundarySplitter_io_out_writeAddress_bits_qos; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_writeData_valid = dram0BoundarySplitter_io_out_writeData_valid; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_writeData_bits_id = dram0BoundarySplitter_io_out_writeData_bits_id; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_writeData_bits_data = dram0BoundarySplitter_io_out_writeData_bits_data; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_writeData_bits_strb = dram0BoundarySplitter_io_out_writeData_bits_strb; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_writeData_bits_last = dram0BoundarySplitter_io_out_writeData_bits_last; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_writeResponse_ready = dram0BoundarySplitter_io_out_writeResponse_ready; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_readAddress_valid = dram0BoundarySplitter_io_out_readAddress_valid; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_readAddress_bits_id = dram0BoundarySplitter_io_out_readAddress_bits_id; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_readAddress_bits_addr = dram0BoundarySplitter_io_out_readAddress_bits_addr; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_readAddress_bits_len = dram0BoundarySplitter_io_out_readAddress_bits_len; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_readAddress_bits_size = dram0BoundarySplitter_io_out_readAddress_bits_size; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_readAddress_bits_burst = dram0BoundarySplitter_io_out_readAddress_bits_burst; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_readAddress_bits_lock = dram0BoundarySplitter_io_out_readAddress_bits_lock; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_readAddress_bits_cache = dram0BoundarySplitter_io_out_readAddress_bits_cache; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_readAddress_bits_prot = dram0BoundarySplitter_io_out_readAddress_bits_prot; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_readAddress_bits_qos = dram0BoundarySplitter_io_out_readAddress_bits_qos; // @[AXIWrapperTCU.scala 59:9]
  assign dram0_readData_ready = dram0BoundarySplitter_io_out_readData_ready; // @[AXIWrapperTCU.scala 59:9]
  assign dram1_writeAddress_valid = dram1BoundarySplitter_io_out_writeAddress_valid; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_writeAddress_bits_id = dram1BoundarySplitter_io_out_writeAddress_bits_id; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_writeAddress_bits_addr = dram1BoundarySplitter_io_out_writeAddress_bits_addr; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_writeAddress_bits_len = dram1BoundarySplitter_io_out_writeAddress_bits_len; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_writeAddress_bits_size = dram1BoundarySplitter_io_out_writeAddress_bits_size; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_writeAddress_bits_burst = dram1BoundarySplitter_io_out_writeAddress_bits_burst; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_writeAddress_bits_lock = dram1BoundarySplitter_io_out_writeAddress_bits_lock; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_writeAddress_bits_cache = dram1BoundarySplitter_io_out_writeAddress_bits_cache; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_writeAddress_bits_prot = dram1BoundarySplitter_io_out_writeAddress_bits_prot; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_writeAddress_bits_qos = dram1BoundarySplitter_io_out_writeAddress_bits_qos; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_writeData_valid = dram1BoundarySplitter_io_out_writeData_valid; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_writeData_bits_id = dram1BoundarySplitter_io_out_writeData_bits_id; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_writeData_bits_data = dram1BoundarySplitter_io_out_writeData_bits_data; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_writeData_bits_strb = dram1BoundarySplitter_io_out_writeData_bits_strb; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_writeData_bits_last = dram1BoundarySplitter_io_out_writeData_bits_last; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_writeResponse_ready = dram1BoundarySplitter_io_out_writeResponse_ready; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_readAddress_valid = dram1BoundarySplitter_io_out_readAddress_valid; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_readAddress_bits_id = dram1BoundarySplitter_io_out_readAddress_bits_id; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_readAddress_bits_addr = dram1BoundarySplitter_io_out_readAddress_bits_addr; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_readAddress_bits_len = dram1BoundarySplitter_io_out_readAddress_bits_len; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_readAddress_bits_size = dram1BoundarySplitter_io_out_readAddress_bits_size; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_readAddress_bits_burst = dram1BoundarySplitter_io_out_readAddress_bits_burst; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_readAddress_bits_lock = dram1BoundarySplitter_io_out_readAddress_bits_lock; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_readAddress_bits_cache = dram1BoundarySplitter_io_out_readAddress_bits_cache; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_readAddress_bits_prot = dram1BoundarySplitter_io_out_readAddress_bits_prot; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_readAddress_bits_qos = dram1BoundarySplitter_io_out_readAddress_bits_qos; // @[AXIWrapperTCU.scala 63:9]
  assign dram1_readData_ready = dram1BoundarySplitter_io_out_readData_ready; // @[AXIWrapperTCU.scala 63:9]
  assign error = tcu_io_error; // @[AXIWrapperTCU.scala 49:9]
  assign sample_valid = 1'h0; // @[AXIWrapperTCU.scala 50:10]
  assign sample_bits_last = 1'h0; // @[AXIWrapperTCU.scala 50:10]
  assign sample_bits_bits_flags_instruction_ready = 1'h0; // @[AXIWrapperTCU.scala 50:10]
  assign sample_bits_bits_flags_instruction_valid = 1'h0; // @[AXIWrapperTCU.scala 50:10]
  assign sample_bits_bits_flags_memPortA_ready = 1'h0; // @[AXIWrapperTCU.scala 50:10]
  assign sample_bits_bits_flags_memPortA_valid = 1'h0; // @[AXIWrapperTCU.scala 50:10]
  assign sample_bits_bits_flags_memPortB_ready = 1'h0; // @[AXIWrapperTCU.scala 50:10]
  assign sample_bits_bits_flags_memPortB_valid = 1'h0; // @[AXIWrapperTCU.scala 50:10]
  assign sample_bits_bits_flags_dram0_ready = 1'h0; // @[AXIWrapperTCU.scala 50:10]
  assign sample_bits_bits_flags_dram0_valid = 1'h0; // @[AXIWrapperTCU.scala 50:10]
  assign sample_bits_bits_flags_dram1_ready = 1'h0; // @[AXIWrapperTCU.scala 50:10]
  assign sample_bits_bits_flags_dram1_valid = 1'h0; // @[AXIWrapperTCU.scala 50:10]
  assign sample_bits_bits_flags_dataflow_ready = 1'h0; // @[AXIWrapperTCU.scala 50:10]
  assign sample_bits_bits_flags_dataflow_valid = 1'h0; // @[AXIWrapperTCU.scala 50:10]
  assign sample_bits_bits_flags_acc_ready = 1'h0; // @[AXIWrapperTCU.scala 50:10]
  assign sample_bits_bits_flags_acc_valid = 1'h0; // @[AXIWrapperTCU.scala 50:10]
  assign sample_bits_bits_flags_array_ready = 1'h0; // @[AXIWrapperTCU.scala 50:10]
  assign sample_bits_bits_flags_array_valid = 1'h0; // @[AXIWrapperTCU.scala 50:10]
  assign sample_bits_bits_programCounter = 32'h0; // @[AXIWrapperTCU.scala 50:10]
  assign tcu_clock = clock;
  assign tcu_reset = reset;
  assign tcu_io_instruction_valid = instruction_valid; // @[AXIWrapperTCU.scala 47:22]
  assign tcu_io_instruction_bits_opcode = instruction_bits_opcode; // @[AXIWrapperTCU.scala 47:22]
  assign tcu_io_instruction_bits_flags = instruction_bits_flags; // @[AXIWrapperTCU.scala 47:22]
  assign tcu_io_instruction_bits_arguments = instruction_bits_arguments; // @[AXIWrapperTCU.scala 47:22]
  assign tcu_io_status_ready = status_ready; // @[AXIWrapperTCU.scala 48:10]
  assign tcu_io_dram0_control_ready = dram0Converter_io_mem_control_ready; // @[AXIWrapperTCU.scala 75:25]
  assign tcu_io_dram0_dataIn_valid = dram0Converter_io_mem_dataIn_valid; // @[AXIWrapperTCU.scala 75:25]
  assign tcu_io_dram0_dataIn_bits_0 = dram0Converter_io_mem_dataIn_bits_0; // @[AXIWrapperTCU.scala 75:25]
  assign tcu_io_dram0_dataIn_bits_1 = dram0Converter_io_mem_dataIn_bits_1; // @[AXIWrapperTCU.scala 75:25]
  assign tcu_io_dram0_dataIn_bits_2 = dram0Converter_io_mem_dataIn_bits_2; // @[AXIWrapperTCU.scala 75:25]
  assign tcu_io_dram0_dataIn_bits_3 = dram0Converter_io_mem_dataIn_bits_3; // @[AXIWrapperTCU.scala 75:25]
  assign tcu_io_dram0_dataIn_bits_4 = dram0Converter_io_mem_dataIn_bits_4; // @[AXIWrapperTCU.scala 75:25]
  assign tcu_io_dram0_dataIn_bits_5 = dram0Converter_io_mem_dataIn_bits_5; // @[AXIWrapperTCU.scala 75:25]
  assign tcu_io_dram0_dataIn_bits_6 = dram0Converter_io_mem_dataIn_bits_6; // @[AXIWrapperTCU.scala 75:25]
  assign tcu_io_dram0_dataIn_bits_7 = dram0Converter_io_mem_dataIn_bits_7; // @[AXIWrapperTCU.scala 75:25]
  assign tcu_io_dram0_dataOut_ready = dram0Converter_io_mem_dataOut_ready; // @[AXIWrapperTCU.scala 75:25]
  assign tcu_io_dram1_control_ready = dram1Converter_io_mem_control_ready; // @[AXIWrapperTCU.scala 92:25]
  assign tcu_io_dram1_dataIn_valid = dram1Converter_io_mem_dataIn_valid; // @[AXIWrapperTCU.scala 92:25]
  assign tcu_io_dram1_dataIn_bits_0 = dram1Converter_io_mem_dataIn_bits_0; // @[AXIWrapperTCU.scala 92:25]
  assign tcu_io_dram1_dataIn_bits_1 = dram1Converter_io_mem_dataIn_bits_1; // @[AXIWrapperTCU.scala 92:25]
  assign tcu_io_dram1_dataIn_bits_2 = dram1Converter_io_mem_dataIn_bits_2; // @[AXIWrapperTCU.scala 92:25]
  assign tcu_io_dram1_dataIn_bits_3 = dram1Converter_io_mem_dataIn_bits_3; // @[AXIWrapperTCU.scala 92:25]
  assign tcu_io_dram1_dataIn_bits_4 = dram1Converter_io_mem_dataIn_bits_4; // @[AXIWrapperTCU.scala 92:25]
  assign tcu_io_dram1_dataIn_bits_5 = dram1Converter_io_mem_dataIn_bits_5; // @[AXIWrapperTCU.scala 92:25]
  assign tcu_io_dram1_dataIn_bits_6 = dram1Converter_io_mem_dataIn_bits_6; // @[AXIWrapperTCU.scala 92:25]
  assign tcu_io_dram1_dataIn_bits_7 = dram1Converter_io_mem_dataIn_bits_7; // @[AXIWrapperTCU.scala 92:25]
  assign tcu_io_dram1_dataOut_ready = dram1Converter_io_mem_dataOut_ready; // @[AXIWrapperTCU.scala 92:25]
  assign dram0BoundarySplitter_clock = clock;
  assign dram0BoundarySplitter_reset = reset;
  assign dram0BoundarySplitter_io_in_writeAddress_valid = dram0BoundarySplitter_io_in_q_io_out_writeAddress_valid; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_writeAddress_bits_id = dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_id; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_writeAddress_bits_addr =
    dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_addr; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_writeAddress_bits_len = dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_len; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_writeAddress_bits_size =
    dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_size; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_writeAddress_bits_burst =
    dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_burst; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_writeAddress_bits_lock =
    dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_lock; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_writeAddress_bits_cache =
    dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_cache; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_writeAddress_bits_prot =
    dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_prot; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_writeAddress_bits_qos = dram0BoundarySplitter_io_in_q_io_out_writeAddress_bits_qos; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_writeData_valid = dram0BoundarySplitter_io_in_q_io_out_writeData_valid; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_writeData_bits_id = dram0BoundarySplitter_io_in_q_io_out_writeData_bits_id; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_writeData_bits_data = dram0BoundarySplitter_io_in_q_io_out_writeData_bits_data; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_writeData_bits_strb = dram0BoundarySplitter_io_in_q_io_out_writeData_bits_strb; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_writeResponse_ready = dram0BoundarySplitter_io_in_q_io_out_writeResponse_ready; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_readAddress_valid = dram0BoundarySplitter_io_in_q_io_out_readAddress_valid; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_readAddress_bits_id = dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_id; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_readAddress_bits_addr = dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_addr; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_readAddress_bits_len = dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_len; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_readAddress_bits_size = dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_size; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_readAddress_bits_burst =
    dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_burst; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_readAddress_bits_lock = dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_lock; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_readAddress_bits_cache =
    dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_cache; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_readAddress_bits_prot = dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_prot; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_readAddress_bits_qos = dram0BoundarySplitter_io_in_q_io_out_readAddress_bits_qos; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_readData_ready = dram0BoundarySplitter_io_in_q_io_out_readData_ready; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_out_writeAddress_ready = dram0_writeAddress_ready; // @[AXIWrapperTCU.scala 59:9]
  assign dram0BoundarySplitter_io_out_writeData_ready = dram0_writeData_ready; // @[AXIWrapperTCU.scala 59:9]
  assign dram0BoundarySplitter_io_out_writeResponse_valid = dram0_writeResponse_valid; // @[AXIWrapperTCU.scala 59:9]
  assign dram0BoundarySplitter_io_out_readAddress_ready = dram0_readAddress_ready; // @[AXIWrapperTCU.scala 59:9]
  assign dram0BoundarySplitter_io_out_readData_valid = dram0_readData_valid; // @[AXIWrapperTCU.scala 59:9]
  assign dram0BoundarySplitter_io_out_readData_bits_data = dram0_readData_bits_data; // @[AXIWrapperTCU.scala 59:9]
  assign dram1BoundarySplitter_clock = clock;
  assign dram1BoundarySplitter_reset = reset;
  assign dram1BoundarySplitter_io_in_writeAddress_valid = dram1BoundarySplitter_io_in_q_io_out_writeAddress_valid; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_writeAddress_bits_id = dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_id; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_writeAddress_bits_addr =
    dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_addr; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_writeAddress_bits_len = dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_len; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_writeAddress_bits_size =
    dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_size; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_writeAddress_bits_burst =
    dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_burst; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_writeAddress_bits_lock =
    dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_lock; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_writeAddress_bits_cache =
    dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_cache; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_writeAddress_bits_prot =
    dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_prot; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_writeAddress_bits_qos = dram1BoundarySplitter_io_in_q_io_out_writeAddress_bits_qos; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_writeData_valid = dram1BoundarySplitter_io_in_q_io_out_writeData_valid; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_writeData_bits_id = dram1BoundarySplitter_io_in_q_io_out_writeData_bits_id; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_writeData_bits_data = dram1BoundarySplitter_io_in_q_io_out_writeData_bits_data; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_writeData_bits_strb = dram1BoundarySplitter_io_in_q_io_out_writeData_bits_strb; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_writeResponse_ready = dram1BoundarySplitter_io_in_q_io_out_writeResponse_ready; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_readAddress_valid = dram1BoundarySplitter_io_in_q_io_out_readAddress_valid; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_readAddress_bits_id = dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_id; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_readAddress_bits_addr = dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_addr; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_readAddress_bits_len = dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_len; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_readAddress_bits_size = dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_size; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_readAddress_bits_burst =
    dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_burst; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_readAddress_bits_lock = dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_lock; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_readAddress_bits_cache =
    dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_cache; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_readAddress_bits_prot = dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_prot; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_readAddress_bits_qos = dram1BoundarySplitter_io_in_q_io_out_readAddress_bits_qos; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_readData_ready = dram1BoundarySplitter_io_in_q_io_out_readData_ready; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_out_writeAddress_ready = dram1_writeAddress_ready; // @[AXIWrapperTCU.scala 63:9]
  assign dram1BoundarySplitter_io_out_writeData_ready = dram1_writeData_ready; // @[AXIWrapperTCU.scala 63:9]
  assign dram1BoundarySplitter_io_out_writeResponse_valid = dram1_writeResponse_valid; // @[AXIWrapperTCU.scala 63:9]
  assign dram1BoundarySplitter_io_out_readAddress_ready = dram1_readAddress_ready; // @[AXIWrapperTCU.scala 63:9]
  assign dram1BoundarySplitter_io_out_readData_valid = dram1_readData_valid; // @[AXIWrapperTCU.scala 63:9]
  assign dram1BoundarySplitter_io_out_readData_bits_data = dram1_readData_bits_data; // @[AXIWrapperTCU.scala 63:9]
  assign dram0Converter_clock = clock;
  assign dram0Converter_reset = reset;
  assign dram0Converter_io_mem_control_valid = tcu_io_dram0_control_valid; // @[AXIWrapperTCU.scala 75:25]
  assign dram0Converter_io_mem_control_bits_write = tcu_io_dram0_control_bits_write; // @[AXIWrapperTCU.scala 75:25]
  assign dram0Converter_io_mem_control_bits_address = tcu_io_dram0_control_bits_address; // @[AXIWrapperTCU.scala 75:25]
  assign dram0Converter_io_mem_control_bits_size = tcu_io_dram0_control_bits_size; // @[AXIWrapperTCU.scala 75:25]
  assign dram0Converter_io_mem_dataIn_ready = tcu_io_dram0_dataIn_ready; // @[AXIWrapperTCU.scala 75:25]
  assign dram0Converter_io_mem_dataOut_valid = tcu_io_dram0_dataOut_valid; // @[AXIWrapperTCU.scala 75:25]
  assign dram0Converter_io_mem_dataOut_bits_0 = tcu_io_dram0_dataOut_bits_0; // @[AXIWrapperTCU.scala 75:25]
  assign dram0Converter_io_mem_dataOut_bits_1 = tcu_io_dram0_dataOut_bits_1; // @[AXIWrapperTCU.scala 75:25]
  assign dram0Converter_io_mem_dataOut_bits_2 = tcu_io_dram0_dataOut_bits_2; // @[AXIWrapperTCU.scala 75:25]
  assign dram0Converter_io_mem_dataOut_bits_3 = tcu_io_dram0_dataOut_bits_3; // @[AXIWrapperTCU.scala 75:25]
  assign dram0Converter_io_mem_dataOut_bits_4 = tcu_io_dram0_dataOut_bits_4; // @[AXIWrapperTCU.scala 75:25]
  assign dram0Converter_io_mem_dataOut_bits_5 = tcu_io_dram0_dataOut_bits_5; // @[AXIWrapperTCU.scala 75:25]
  assign dram0Converter_io_mem_dataOut_bits_6 = tcu_io_dram0_dataOut_bits_6; // @[AXIWrapperTCU.scala 75:25]
  assign dram0Converter_io_mem_dataOut_bits_7 = tcu_io_dram0_dataOut_bits_7; // @[AXIWrapperTCU.scala 75:25]
  assign dram0Converter_io_axi_writeAddress_ready = dram0BoundarySplitter_io_in_q_io_in_writeAddress_ready; // @[Queue.scala 24:13]
  assign dram0Converter_io_axi_writeData_ready = dram0BoundarySplitter_io_in_q_io_in_writeData_ready; // @[Queue.scala 24:13]
  assign dram0Converter_io_axi_writeResponse_valid = dram0BoundarySplitter_io_in_q_io_in_writeResponse_valid; // @[Queue.scala 24:13]
  assign dram0Converter_io_axi_readAddress_ready = dram0BoundarySplitter_io_in_q_io_in_readAddress_ready; // @[Queue.scala 24:13]
  assign dram0Converter_io_axi_readData_valid = dram0BoundarySplitter_io_in_q_io_in_readData_valid; // @[Queue.scala 24:13]
  assign dram0Converter_io_axi_readData_bits_data = dram0BoundarySplitter_io_in_q_io_in_readData_bits_data; // @[Queue.scala 24:13]
  assign dram0Converter_io_axi_readData_bits_last = dram0BoundarySplitter_io_in_q_io_in_readData_bits_last; // @[Queue.scala 24:13]
  assign dram0Converter_io_addressOffset = tcu_io_config_dram0AddressOffset; // @[AXIWrapperTCU.scala 76:35]
  assign dram0Converter_io_cacheBehavior = tcu_io_config_dram0CacheBehaviour; // @[AXIWrapperTCU.scala 77:35]
  assign dram0Converter_io_timeout = tcu_io_timeout; // @[AXIWrapperTCU.scala 78:29]
  assign dram0Converter_io_tracepoint = tcu_io_tracepoint; // @[AXIWrapperTCU.scala 79:32]
  assign dram0Converter_io_programCounter = tcu_io_programCounter; // @[AXIWrapperTCU.scala 80:36]
  assign dram0BoundarySplitter_io_in_q_clock = clock;
  assign dram0BoundarySplitter_io_in_q_reset = reset;
  assign dram0BoundarySplitter_io_in_q_io_in_writeAddress_valid = dram0Converter_io_axi_writeAddress_valid; // @[Queue.scala 24:13]
  assign dram0BoundarySplitter_io_in_q_io_in_writeAddress_bits_addr = dram0Converter_io_axi_writeAddress_bits_addr; // @[Queue.scala 24:13]
  assign dram0BoundarySplitter_io_in_q_io_in_writeAddress_bits_len = dram0Converter_io_axi_writeAddress_bits_len; // @[Queue.scala 24:13]
  assign dram0BoundarySplitter_io_in_q_io_in_writeAddress_bits_cache = dram0Converter_io_axi_writeAddress_bits_cache; // @[Queue.scala 24:13]
  assign dram0BoundarySplitter_io_in_q_io_in_writeData_valid = dram0Converter_io_axi_writeData_valid; // @[Queue.scala 24:13]
  assign dram0BoundarySplitter_io_in_q_io_in_writeData_bits_data = dram0Converter_io_axi_writeData_bits_data; // @[Queue.scala 24:13]
  assign dram0BoundarySplitter_io_in_q_io_in_writeResponse_ready = dram0Converter_io_axi_writeResponse_ready; // @[Queue.scala 24:13]
  assign dram0BoundarySplitter_io_in_q_io_in_readAddress_valid = dram0Converter_io_axi_readAddress_valid; // @[Queue.scala 24:13]
  assign dram0BoundarySplitter_io_in_q_io_in_readAddress_bits_addr = dram0Converter_io_axi_readAddress_bits_addr; // @[Queue.scala 24:13]
  assign dram0BoundarySplitter_io_in_q_io_in_readAddress_bits_len = dram0Converter_io_axi_readAddress_bits_len; // @[Queue.scala 24:13]
  assign dram0BoundarySplitter_io_in_q_io_in_readAddress_bits_cache = dram0Converter_io_axi_readAddress_bits_cache; // @[Queue.scala 24:13]
  assign dram0BoundarySplitter_io_in_q_io_in_readData_ready = dram0Converter_io_axi_readData_ready; // @[Queue.scala 24:13]
  assign dram0BoundarySplitter_io_in_q_io_out_writeAddress_ready = dram0BoundarySplitter_io_in_writeAddress_ready; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_q_io_out_writeData_ready = dram0BoundarySplitter_io_in_writeData_ready; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_q_io_out_writeResponse_valid = dram0BoundarySplitter_io_in_writeResponse_valid; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_q_io_out_readAddress_ready = dram0BoundarySplitter_io_in_readAddress_ready; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_q_io_out_readData_valid = dram0BoundarySplitter_io_in_readData_valid; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_q_io_out_readData_bits_data = dram0BoundarySplitter_io_in_readData_bits_data; // @[AXIWrapperTCU.scala 74:31]
  assign dram0BoundarySplitter_io_in_q_io_out_readData_bits_last = dram0BoundarySplitter_io_in_readData_bits_last; // @[AXIWrapperTCU.scala 74:31]
  assign dram1Converter_clock = clock;
  assign dram1Converter_reset = reset;
  assign dram1Converter_io_mem_control_valid = tcu_io_dram1_control_valid; // @[AXIWrapperTCU.scala 92:25]
  assign dram1Converter_io_mem_control_bits_write = tcu_io_dram1_control_bits_write; // @[AXIWrapperTCU.scala 92:25]
  assign dram1Converter_io_mem_control_bits_address = tcu_io_dram1_control_bits_address; // @[AXIWrapperTCU.scala 92:25]
  assign dram1Converter_io_mem_control_bits_size = tcu_io_dram1_control_bits_size; // @[AXIWrapperTCU.scala 92:25]
  assign dram1Converter_io_mem_dataIn_ready = tcu_io_dram1_dataIn_ready; // @[AXIWrapperTCU.scala 92:25]
  assign dram1Converter_io_mem_dataOut_valid = tcu_io_dram1_dataOut_valid; // @[AXIWrapperTCU.scala 92:25]
  assign dram1Converter_io_mem_dataOut_bits_0 = tcu_io_dram1_dataOut_bits_0; // @[AXIWrapperTCU.scala 92:25]
  assign dram1Converter_io_mem_dataOut_bits_1 = tcu_io_dram1_dataOut_bits_1; // @[AXIWrapperTCU.scala 92:25]
  assign dram1Converter_io_mem_dataOut_bits_2 = tcu_io_dram1_dataOut_bits_2; // @[AXIWrapperTCU.scala 92:25]
  assign dram1Converter_io_mem_dataOut_bits_3 = tcu_io_dram1_dataOut_bits_3; // @[AXIWrapperTCU.scala 92:25]
  assign dram1Converter_io_mem_dataOut_bits_4 = tcu_io_dram1_dataOut_bits_4; // @[AXIWrapperTCU.scala 92:25]
  assign dram1Converter_io_mem_dataOut_bits_5 = tcu_io_dram1_dataOut_bits_5; // @[AXIWrapperTCU.scala 92:25]
  assign dram1Converter_io_mem_dataOut_bits_6 = tcu_io_dram1_dataOut_bits_6; // @[AXIWrapperTCU.scala 92:25]
  assign dram1Converter_io_mem_dataOut_bits_7 = tcu_io_dram1_dataOut_bits_7; // @[AXIWrapperTCU.scala 92:25]
  assign dram1Converter_io_axi_writeAddress_ready = dram1BoundarySplitter_io_in_q_io_in_writeAddress_ready; // @[Queue.scala 24:13]
  assign dram1Converter_io_axi_writeData_ready = dram1BoundarySplitter_io_in_q_io_in_writeData_ready; // @[Queue.scala 24:13]
  assign dram1Converter_io_axi_writeResponse_valid = dram1BoundarySplitter_io_in_q_io_in_writeResponse_valid; // @[Queue.scala 24:13]
  assign dram1Converter_io_axi_readAddress_ready = dram1BoundarySplitter_io_in_q_io_in_readAddress_ready; // @[Queue.scala 24:13]
  assign dram1Converter_io_axi_readData_valid = dram1BoundarySplitter_io_in_q_io_in_readData_valid; // @[Queue.scala 24:13]
  assign dram1Converter_io_axi_readData_bits_data = dram1BoundarySplitter_io_in_q_io_in_readData_bits_data; // @[Queue.scala 24:13]
  assign dram1Converter_io_axi_readData_bits_last = dram1BoundarySplitter_io_in_q_io_in_readData_bits_last; // @[Queue.scala 24:13]
  assign dram1Converter_io_addressOffset = tcu_io_config_dram1AddressOffset; // @[AXIWrapperTCU.scala 93:35]
  assign dram1Converter_io_cacheBehavior = tcu_io_config_dram1CacheBehaviour; // @[AXIWrapperTCU.scala 94:35]
  assign dram1Converter_io_timeout = tcu_io_timeout; // @[AXIWrapperTCU.scala 95:29]
  assign dram1Converter_io_tracepoint = tcu_io_tracepoint; // @[AXIWrapperTCU.scala 96:32]
  assign dram1Converter_io_programCounter = tcu_io_programCounter; // @[AXIWrapperTCU.scala 97:36]
  assign dram1BoundarySplitter_io_in_q_clock = clock;
  assign dram1BoundarySplitter_io_in_q_reset = reset;
  assign dram1BoundarySplitter_io_in_q_io_in_writeAddress_valid = dram1Converter_io_axi_writeAddress_valid; // @[Queue.scala 24:13]
  assign dram1BoundarySplitter_io_in_q_io_in_writeAddress_bits_addr = dram1Converter_io_axi_writeAddress_bits_addr; // @[Queue.scala 24:13]
  assign dram1BoundarySplitter_io_in_q_io_in_writeAddress_bits_len = dram1Converter_io_axi_writeAddress_bits_len; // @[Queue.scala 24:13]
  assign dram1BoundarySplitter_io_in_q_io_in_writeAddress_bits_cache = dram1Converter_io_axi_writeAddress_bits_cache; // @[Queue.scala 24:13]
  assign dram1BoundarySplitter_io_in_q_io_in_writeData_valid = dram1Converter_io_axi_writeData_valid; // @[Queue.scala 24:13]
  assign dram1BoundarySplitter_io_in_q_io_in_writeData_bits_data = dram1Converter_io_axi_writeData_bits_data; // @[Queue.scala 24:13]
  assign dram1BoundarySplitter_io_in_q_io_in_writeResponse_ready = dram1Converter_io_axi_writeResponse_ready; // @[Queue.scala 24:13]
  assign dram1BoundarySplitter_io_in_q_io_in_readAddress_valid = dram1Converter_io_axi_readAddress_valid; // @[Queue.scala 24:13]
  assign dram1BoundarySplitter_io_in_q_io_in_readAddress_bits_addr = dram1Converter_io_axi_readAddress_bits_addr; // @[Queue.scala 24:13]
  assign dram1BoundarySplitter_io_in_q_io_in_readAddress_bits_len = dram1Converter_io_axi_readAddress_bits_len; // @[Queue.scala 24:13]
  assign dram1BoundarySplitter_io_in_q_io_in_readAddress_bits_cache = dram1Converter_io_axi_readAddress_bits_cache; // @[Queue.scala 24:13]
  assign dram1BoundarySplitter_io_in_q_io_in_readData_ready = dram1Converter_io_axi_readData_ready; // @[Queue.scala 24:13]
  assign dram1BoundarySplitter_io_in_q_io_out_writeAddress_ready = dram1BoundarySplitter_io_in_writeAddress_ready; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_q_io_out_writeData_ready = dram1BoundarySplitter_io_in_writeData_ready; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_q_io_out_writeResponse_valid = dram1BoundarySplitter_io_in_writeResponse_valid; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_q_io_out_readAddress_ready = dram1BoundarySplitter_io_in_readAddress_ready; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_q_io_out_readData_valid = dram1BoundarySplitter_io_in_readData_valid; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_q_io_out_readData_bits_data = dram1BoundarySplitter_io_in_readData_bits_data; // @[AXIWrapperTCU.scala 91:31]
  assign dram1BoundarySplitter_io_in_q_io_out_readData_bits_last = dram1BoundarySplitter_io_in_readData_bits_last; // @[AXIWrapperTCU.scala 91:31]
endmodule
