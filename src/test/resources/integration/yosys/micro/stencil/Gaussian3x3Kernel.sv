module DynamicFilter(
  input         clock,
  input         reset,
  output        io_pixel_in_pixel_ready,
  input         io_pixel_in_pixel_valid,
  input  [7:0]  io_pixel_in_pixel_bits,
  output        io_pixel_in_imgw_ready,
  input         io_pixel_in_imgw_valid,
  input  [15:0] io_pixel_in_imgw_bits,
  output        io_pixel_in_imgh_ready,
  input         io_pixel_in_imgh_valid,
  input  [15:0] io_pixel_in_imgh_bits,
  input         io_pixel_out_ready,
  output        io_pixel_out_valid,
  output [7:0]  io_pixel_out_bits
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
  reg [15:0] x_index; // @[BlurFilter.scala 112:23]
  reg [15:0] y_index; // @[BlurFilter.scala 112:23]
  reg [15:0] x_dim; // @[BlurFilter.scala 116:18]
  reg [15:0] y_dim; // @[BlurFilter.scala 117:18]
  reg [15:0] x_ub; // @[BlurFilter.scala 120:17]
  reg [15:0] y_ub; // @[BlurFilter.scala 122:17]
  reg  idle; // @[BlurFilter.scala 124:21]
  wire  _io_pixel_in_imgh_ready_T = io_pixel_in_imgh_valid & io_pixel_in_imgw_valid; // @[BlurFilter.scala 148:55]
  wire [15:0] _x_ub_T_2 = $signed(io_pixel_in_imgw_bits) - 16'sh2; // @[BlurFilter.scala 158:24]
  wire [15:0] _y_ub_T_2 = $signed(io_pixel_in_imgh_bits) - 16'sh2; // @[BlurFilter.scala 161:24]
  wire  _GEN_0 = $signed(io_pixel_in_imgw_bits) != 16'sh0 & $signed(io_pixel_in_imgh_bits) != 16'sh0 ? 1'h0 : 1'h1; // @[BlurFilter.scala 166:62 167:14 169:14]
  wire  _GEN_9 = _io_pixel_in_imgh_ready_T ? _GEN_0 : idle; // @[BlurFilter.scala 124:21 152:59]
  wire [15:0] _can_pass_T_6 = 16'sh1 + $signed(x_ub); // @[BlurFilter.scala 177:74]
  wire  _can_pass_T_12 = $signed(y_index) >= 16'sh2; // @[BlurFilter.scala 178:18]
  wire  _can_pass_T_13 = $signed(x_index) >= 16'sh2 & $signed(x_index) <= $signed(_can_pass_T_6) & _can_pass_T_12; // @[BlurFilter.scala 177:83]
  wire [15:0] _can_pass_T_16 = 16'sh1 + $signed(y_ub); // @[BlurFilter.scala 178:76]
  wire  can_pass = _can_pass_T_13 & $signed(y_index) <= $signed(_can_pass_T_16); // @[BlurFilter.scala 178:46]
  wire  _T_4 = io_pixel_out_ready & io_pixel_out_valid; // @[Decoupled.scala 50:35]
  wire [15:0] _T_7 = $signed(x_dim) - 16'sh1; // @[BlurFilter.scala 127:29]
  wire [15:0] _T_11 = $signed(y_dim) - 16'sh1; // @[BlurFilter.scala 129:31]
  wire [15:0] _y_index_T_2 = $signed(y_index) + 16'sh1; // @[BlurFilter.scala 133:28]
  wire  _GEN_10 = $signed(y_index) == $signed(_T_11) | idle; // @[BlurFilter.scala 129:39 130:17 124:21]
  wire [15:0] _GEN_11 = $signed(y_index) == $signed(_T_11) ? $signed(16'sh0) : $signed(_y_index_T_2); // @[BlurFilter.scala 129:39 131:17 133:17]
  wire [15:0] _x_index_T_2 = $signed(x_index) + 16'sh1; // @[BlurFilter.scala 136:26]
  wire [15:0] _GEN_12 = $signed(x_index) == $signed(_T_7) ? $signed(16'sh0) : $signed(_x_index_T_2); // @[BlurFilter.scala 127:37 128:15 136:15]
  wire  _GEN_13 = $signed(x_index) == $signed(_T_7) ? _GEN_10 : idle; // @[BlurFilter.scala 124:21 127:37]
  wire [15:0] _GEN_14 = $signed(x_index) == $signed(_T_7) ? $signed(_GEN_11) : $signed(y_index); // @[BlurFilter.scala 112:23 127:37]
  wire  _GEN_16 = _T_4 ? _GEN_13 : idle; // @[BlurFilter.scala 124:21 183:31]
  wire  _T_13 = io_pixel_in_pixel_ready & io_pixel_in_pixel_valid; // @[Decoupled.scala 50:35]
  wire  _GEN_24 = _T_13 ? _GEN_13 : idle; // @[BlurFilter.scala 124:21 189:36]
  wire  _GEN_26 = can_pass & io_pixel_in_pixel_valid; // @[BlurFilter.scala 180:20 181:31 187:31]
  wire  _GEN_27 = can_pass ? io_pixel_out_ready : 1'h1; // @[BlurFilter.scala 180:20 182:31 188:31]
  wire  _GEN_29 = can_pass ? _GEN_16 : _GEN_24; // @[BlurFilter.scala 180:20]
  wire  _GEN_41 = idle ? _GEN_9 : _GEN_29; // @[BlurFilter.scala 147:14]
  assign io_pixel_in_pixel_ready = idle ? 1'h0 : _GEN_27; // @[BlurFilter.scala 147:14 143:27]
  assign io_pixel_in_imgw_ready = idle & _io_pixel_in_imgh_ready_T; // @[BlurFilter.scala 147:14 141:27 149:28]
  assign io_pixel_in_imgh_ready = idle & (io_pixel_in_imgh_valid & io_pixel_in_imgw_valid); // @[BlurFilter.scala 147:14 140:27 148:28]
  assign io_pixel_out_valid = idle ? 1'h0 : _GEN_26; // @[BlurFilter.scala 147:14 142:27]
  assign io_pixel_out_bits = io_pixel_in_pixel_bits; // @[BlurFilter.scala 145:21]
  always @(posedge clock) begin
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_index <= 16'sh0; // @[BlurFilter.scala 163:15]
      end
    end else if (can_pass) begin // @[BlurFilter.scala 180:20]
      if (_T_4) begin // @[BlurFilter.scala 183:31]
        x_index <= _GEN_12;
      end
    end else if (_T_13) begin // @[BlurFilter.scala 189:36]
      x_index <= _GEN_12;
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_index <= 16'sh0; // @[BlurFilter.scala 164:15]
      end
    end else if (can_pass) begin // @[BlurFilter.scala 180:20]
      if (_T_4) begin // @[BlurFilter.scala 183:31]
        y_index <= _GEN_14;
      end
    end else if (_T_13) begin // @[BlurFilter.scala 189:36]
      y_index <= _GEN_14;
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_dim <= io_pixel_in_imgw_bits; // @[BlurFilter.scala 155:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_dim <= io_pixel_in_imgh_bits; // @[BlurFilter.scala 156:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_ub <= _x_ub_T_2; // @[BlurFilter.scala 158:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_ub <= _y_ub_T_2; // @[BlurFilter.scala 161:13]
      end
    end
    idle <= reset | _GEN_41; // @[BlurFilter.scala 124:{21,21}]
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
  x_index = _RAND_0[15:0];
  _RAND_1 = {1{`RANDOM}};
  y_index = _RAND_1[15:0];
  _RAND_2 = {1{`RANDOM}};
  x_dim = _RAND_2[15:0];
  _RAND_3 = {1{`RANDOM}};
  y_dim = _RAND_3[15:0];
  _RAND_4 = {1{`RANDOM}};
  x_ub = _RAND_4[15:0];
  _RAND_5 = {1{`RANDOM}};
  y_ub = _RAND_5[15:0];
  _RAND_6 = {1{`RANDOM}};
  idle = _RAND_6[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module DynamicFilter_1(
  input         clock,
  input         reset,
  output        io_pixel_in_pixel_ready,
  input         io_pixel_in_pixel_valid,
  input  [7:0]  io_pixel_in_pixel_bits,
  output        io_pixel_in_imgw_ready,
  input         io_pixel_in_imgw_valid,
  input  [15:0] io_pixel_in_imgw_bits,
  output        io_pixel_in_imgh_ready,
  input         io_pixel_in_imgh_valid,
  input  [15:0] io_pixel_in_imgh_bits,
  input         io_pixel_out_ready,
  output        io_pixel_out_valid,
  output [7:0]  io_pixel_out_bits
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
  reg [15:0] x_index; // @[BlurFilter.scala 112:23]
  reg [15:0] y_index; // @[BlurFilter.scala 112:23]
  reg [15:0] x_dim; // @[BlurFilter.scala 116:18]
  reg [15:0] y_dim; // @[BlurFilter.scala 117:18]
  reg [15:0] x_ub; // @[BlurFilter.scala 120:17]
  reg [15:0] y_ub; // @[BlurFilter.scala 122:17]
  reg  idle; // @[BlurFilter.scala 124:21]
  wire  _io_pixel_in_imgh_ready_T = io_pixel_in_imgh_valid & io_pixel_in_imgw_valid; // @[BlurFilter.scala 148:55]
  wire [15:0] _x_ub_T_2 = $signed(io_pixel_in_imgw_bits) - 16'sh2; // @[BlurFilter.scala 158:24]
  wire [15:0] _y_ub_T_2 = $signed(io_pixel_in_imgh_bits) - 16'sh2; // @[BlurFilter.scala 161:24]
  wire  _GEN_0 = $signed(io_pixel_in_imgw_bits) != 16'sh0 & $signed(io_pixel_in_imgh_bits) != 16'sh0 ? 1'h0 : 1'h1; // @[BlurFilter.scala 166:62 167:14 169:14]
  wire  _GEN_9 = _io_pixel_in_imgh_ready_T ? _GEN_0 : idle; // @[BlurFilter.scala 124:21 152:59]
  wire [16:0] _can_pass_T_4 = {{1{x_ub[15]}},x_ub}; // @[BlurFilter.scala 177:74]
  wire [15:0] _can_pass_T_6 = _can_pass_T_4[15:0]; // @[BlurFilter.scala 177:74]
  wire  _can_pass_T_12 = $signed(y_index) >= 16'sh2; // @[BlurFilter.scala 178:18]
  wire  _can_pass_T_13 = $signed(x_index) >= 16'sh1 & $signed(x_index) <= $signed(_can_pass_T_6) & _can_pass_T_12; // @[BlurFilter.scala 177:83]
  wire [15:0] _can_pass_T_16 = 16'sh1 + $signed(y_ub); // @[BlurFilter.scala 178:76]
  wire  can_pass = _can_pass_T_13 & $signed(y_index) <= $signed(_can_pass_T_16); // @[BlurFilter.scala 178:46]
  wire  _T_4 = io_pixel_out_ready & io_pixel_out_valid; // @[Decoupled.scala 50:35]
  wire [15:0] _T_7 = $signed(x_dim) - 16'sh1; // @[BlurFilter.scala 127:29]
  wire [15:0] _T_11 = $signed(y_dim) - 16'sh1; // @[BlurFilter.scala 129:31]
  wire [15:0] _y_index_T_2 = $signed(y_index) + 16'sh1; // @[BlurFilter.scala 133:28]
  wire  _GEN_10 = $signed(y_index) == $signed(_T_11) | idle; // @[BlurFilter.scala 129:39 130:17 124:21]
  wire [15:0] _GEN_11 = $signed(y_index) == $signed(_T_11) ? $signed(16'sh0) : $signed(_y_index_T_2); // @[BlurFilter.scala 129:39 131:17 133:17]
  wire [15:0] _x_index_T_2 = $signed(x_index) + 16'sh1; // @[BlurFilter.scala 136:26]
  wire [15:0] _GEN_12 = $signed(x_index) == $signed(_T_7) ? $signed(16'sh0) : $signed(_x_index_T_2); // @[BlurFilter.scala 127:37 128:15 136:15]
  wire  _GEN_13 = $signed(x_index) == $signed(_T_7) ? _GEN_10 : idle; // @[BlurFilter.scala 124:21 127:37]
  wire [15:0] _GEN_14 = $signed(x_index) == $signed(_T_7) ? $signed(_GEN_11) : $signed(y_index); // @[BlurFilter.scala 112:23 127:37]
  wire  _GEN_16 = _T_4 ? _GEN_13 : idle; // @[BlurFilter.scala 124:21 183:31]
  wire  _T_13 = io_pixel_in_pixel_ready & io_pixel_in_pixel_valid; // @[Decoupled.scala 50:35]
  wire  _GEN_24 = _T_13 ? _GEN_13 : idle; // @[BlurFilter.scala 124:21 189:36]
  wire  _GEN_26 = can_pass & io_pixel_in_pixel_valid; // @[BlurFilter.scala 180:20 181:31 187:31]
  wire  _GEN_27 = can_pass ? io_pixel_out_ready : 1'h1; // @[BlurFilter.scala 180:20 182:31 188:31]
  wire  _GEN_29 = can_pass ? _GEN_16 : _GEN_24; // @[BlurFilter.scala 180:20]
  wire  _GEN_41 = idle ? _GEN_9 : _GEN_29; // @[BlurFilter.scala 147:14]
  assign io_pixel_in_pixel_ready = idle ? 1'h0 : _GEN_27; // @[BlurFilter.scala 147:14 143:27]
  assign io_pixel_in_imgw_ready = idle & _io_pixel_in_imgh_ready_T; // @[BlurFilter.scala 147:14 141:27 149:28]
  assign io_pixel_in_imgh_ready = idle & (io_pixel_in_imgh_valid & io_pixel_in_imgw_valid); // @[BlurFilter.scala 147:14 140:27 148:28]
  assign io_pixel_out_valid = idle ? 1'h0 : _GEN_26; // @[BlurFilter.scala 147:14 142:27]
  assign io_pixel_out_bits = io_pixel_in_pixel_bits; // @[BlurFilter.scala 145:21]
  always @(posedge clock) begin
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_index <= 16'sh0; // @[BlurFilter.scala 163:15]
      end
    end else if (can_pass) begin // @[BlurFilter.scala 180:20]
      if (_T_4) begin // @[BlurFilter.scala 183:31]
        x_index <= _GEN_12;
      end
    end else if (_T_13) begin // @[BlurFilter.scala 189:36]
      x_index <= _GEN_12;
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_index <= 16'sh0; // @[BlurFilter.scala 164:15]
      end
    end else if (can_pass) begin // @[BlurFilter.scala 180:20]
      if (_T_4) begin // @[BlurFilter.scala 183:31]
        y_index <= _GEN_14;
      end
    end else if (_T_13) begin // @[BlurFilter.scala 189:36]
      y_index <= _GEN_14;
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_dim <= io_pixel_in_imgw_bits; // @[BlurFilter.scala 155:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_dim <= io_pixel_in_imgh_bits; // @[BlurFilter.scala 156:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_ub <= _x_ub_T_2; // @[BlurFilter.scala 158:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_ub <= _y_ub_T_2; // @[BlurFilter.scala 161:13]
      end
    end
    idle <= reset | _GEN_41; // @[BlurFilter.scala 124:{21,21}]
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
  x_index = _RAND_0[15:0];
  _RAND_1 = {1{`RANDOM}};
  y_index = _RAND_1[15:0];
  _RAND_2 = {1{`RANDOM}};
  x_dim = _RAND_2[15:0];
  _RAND_3 = {1{`RANDOM}};
  y_dim = _RAND_3[15:0];
  _RAND_4 = {1{`RANDOM}};
  x_ub = _RAND_4[15:0];
  _RAND_5 = {1{`RANDOM}};
  y_ub = _RAND_5[15:0];
  _RAND_6 = {1{`RANDOM}};
  idle = _RAND_6[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module DynamicFilter_2(
  input         clock,
  input         reset,
  output        io_pixel_in_pixel_ready,
  input         io_pixel_in_pixel_valid,
  input  [7:0]  io_pixel_in_pixel_bits,
  output        io_pixel_in_imgw_ready,
  input         io_pixel_in_imgw_valid,
  input  [15:0] io_pixel_in_imgw_bits,
  output        io_pixel_in_imgh_ready,
  input         io_pixel_in_imgh_valid,
  input  [15:0] io_pixel_in_imgh_bits,
  input         io_pixel_out_ready,
  output        io_pixel_out_valid,
  output [7:0]  io_pixel_out_bits
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
  reg [15:0] x_index; // @[BlurFilter.scala 112:23]
  reg [15:0] y_index; // @[BlurFilter.scala 112:23]
  reg [15:0] x_dim; // @[BlurFilter.scala 116:18]
  reg [15:0] y_dim; // @[BlurFilter.scala 117:18]
  reg [15:0] x_ub; // @[BlurFilter.scala 120:17]
  reg [15:0] y_ub; // @[BlurFilter.scala 122:17]
  reg  idle; // @[BlurFilter.scala 124:21]
  wire  _io_pixel_in_imgh_ready_T = io_pixel_in_imgh_valid & io_pixel_in_imgw_valid; // @[BlurFilter.scala 148:55]
  wire [15:0] _x_ub_T_2 = $signed(io_pixel_in_imgw_bits) - 16'sh2; // @[BlurFilter.scala 158:24]
  wire [15:0] _y_ub_T_2 = $signed(io_pixel_in_imgh_bits) - 16'sh2; // @[BlurFilter.scala 161:24]
  wire  _GEN_0 = $signed(io_pixel_in_imgw_bits) != 16'sh0 & $signed(io_pixel_in_imgh_bits) != 16'sh0 ? 1'h0 : 1'h1; // @[BlurFilter.scala 166:62 167:14 169:14]
  wire  _GEN_9 = _io_pixel_in_imgh_ready_T ? _GEN_0 : idle; // @[BlurFilter.scala 124:21 152:59]
  wire [15:0] _can_pass_T_6 = $signed(x_ub) - 16'sh1; // @[BlurFilter.scala 177:74]
  wire  _can_pass_T_12 = $signed(y_index) >= 16'sh2; // @[BlurFilter.scala 178:18]
  wire  _can_pass_T_13 = $signed(x_index) >= 16'sh0 & $signed(x_index) <= $signed(_can_pass_T_6) & _can_pass_T_12; // @[BlurFilter.scala 177:83]
  wire [15:0] _can_pass_T_16 = 16'sh1 + $signed(y_ub); // @[BlurFilter.scala 178:76]
  wire  can_pass = _can_pass_T_13 & $signed(y_index) <= $signed(_can_pass_T_16); // @[BlurFilter.scala 178:46]
  wire  _T_4 = io_pixel_out_ready & io_pixel_out_valid; // @[Decoupled.scala 50:35]
  wire [15:0] _T_7 = $signed(x_dim) - 16'sh1; // @[BlurFilter.scala 127:29]
  wire [15:0] _T_11 = $signed(y_dim) - 16'sh1; // @[BlurFilter.scala 129:31]
  wire [15:0] _y_index_T_2 = $signed(y_index) + 16'sh1; // @[BlurFilter.scala 133:28]
  wire  _GEN_10 = $signed(y_index) == $signed(_T_11) | idle; // @[BlurFilter.scala 129:39 130:17 124:21]
  wire [15:0] _GEN_11 = $signed(y_index) == $signed(_T_11) ? $signed(16'sh0) : $signed(_y_index_T_2); // @[BlurFilter.scala 129:39 131:17 133:17]
  wire [15:0] _x_index_T_2 = $signed(x_index) + 16'sh1; // @[BlurFilter.scala 136:26]
  wire [15:0] _GEN_12 = $signed(x_index) == $signed(_T_7) ? $signed(16'sh0) : $signed(_x_index_T_2); // @[BlurFilter.scala 127:37 128:15 136:15]
  wire  _GEN_13 = $signed(x_index) == $signed(_T_7) ? _GEN_10 : idle; // @[BlurFilter.scala 124:21 127:37]
  wire [15:0] _GEN_14 = $signed(x_index) == $signed(_T_7) ? $signed(_GEN_11) : $signed(y_index); // @[BlurFilter.scala 112:23 127:37]
  wire  _GEN_16 = _T_4 ? _GEN_13 : idle; // @[BlurFilter.scala 124:21 183:31]
  wire  _T_13 = io_pixel_in_pixel_ready & io_pixel_in_pixel_valid; // @[Decoupled.scala 50:35]
  wire  _GEN_24 = _T_13 ? _GEN_13 : idle; // @[BlurFilter.scala 124:21 189:36]
  wire  _GEN_26 = can_pass & io_pixel_in_pixel_valid; // @[BlurFilter.scala 180:20 181:31 187:31]
  wire  _GEN_27 = can_pass ? io_pixel_out_ready : 1'h1; // @[BlurFilter.scala 180:20 182:31 188:31]
  wire  _GEN_29 = can_pass ? _GEN_16 : _GEN_24; // @[BlurFilter.scala 180:20]
  wire  _GEN_41 = idle ? _GEN_9 : _GEN_29; // @[BlurFilter.scala 147:14]
  assign io_pixel_in_pixel_ready = idle ? 1'h0 : _GEN_27; // @[BlurFilter.scala 147:14 143:27]
  assign io_pixel_in_imgw_ready = idle & _io_pixel_in_imgh_ready_T; // @[BlurFilter.scala 147:14 141:27 149:28]
  assign io_pixel_in_imgh_ready = idle & (io_pixel_in_imgh_valid & io_pixel_in_imgw_valid); // @[BlurFilter.scala 147:14 140:27 148:28]
  assign io_pixel_out_valid = idle ? 1'h0 : _GEN_26; // @[BlurFilter.scala 147:14 142:27]
  assign io_pixel_out_bits = io_pixel_in_pixel_bits; // @[BlurFilter.scala 145:21]
  always @(posedge clock) begin
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_index <= 16'sh0; // @[BlurFilter.scala 163:15]
      end
    end else if (can_pass) begin // @[BlurFilter.scala 180:20]
      if (_T_4) begin // @[BlurFilter.scala 183:31]
        x_index <= _GEN_12;
      end
    end else if (_T_13) begin // @[BlurFilter.scala 189:36]
      x_index <= _GEN_12;
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_index <= 16'sh0; // @[BlurFilter.scala 164:15]
      end
    end else if (can_pass) begin // @[BlurFilter.scala 180:20]
      if (_T_4) begin // @[BlurFilter.scala 183:31]
        y_index <= _GEN_14;
      end
    end else if (_T_13) begin // @[BlurFilter.scala 189:36]
      y_index <= _GEN_14;
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_dim <= io_pixel_in_imgw_bits; // @[BlurFilter.scala 155:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_dim <= io_pixel_in_imgh_bits; // @[BlurFilter.scala 156:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_ub <= _x_ub_T_2; // @[BlurFilter.scala 158:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_ub <= _y_ub_T_2; // @[BlurFilter.scala 161:13]
      end
    end
    idle <= reset | _GEN_41; // @[BlurFilter.scala 124:{21,21}]
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
  x_index = _RAND_0[15:0];
  _RAND_1 = {1{`RANDOM}};
  y_index = _RAND_1[15:0];
  _RAND_2 = {1{`RANDOM}};
  x_dim = _RAND_2[15:0];
  _RAND_3 = {1{`RANDOM}};
  y_dim = _RAND_3[15:0];
  _RAND_4 = {1{`RANDOM}};
  x_ub = _RAND_4[15:0];
  _RAND_5 = {1{`RANDOM}};
  y_ub = _RAND_5[15:0];
  _RAND_6 = {1{`RANDOM}};
  idle = _RAND_6[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module DynamicFilter_3(
  input         clock,
  input         reset,
  output        io_pixel_in_pixel_ready,
  input         io_pixel_in_pixel_valid,
  input  [7:0]  io_pixel_in_pixel_bits,
  output        io_pixel_in_imgw_ready,
  input         io_pixel_in_imgw_valid,
  input  [15:0] io_pixel_in_imgw_bits,
  output        io_pixel_in_imgh_ready,
  input         io_pixel_in_imgh_valid,
  input  [15:0] io_pixel_in_imgh_bits,
  input         io_pixel_out_ready,
  output        io_pixel_out_valid,
  output [7:0]  io_pixel_out_bits
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
  reg [15:0] x_index; // @[BlurFilter.scala 112:23]
  reg [15:0] y_index; // @[BlurFilter.scala 112:23]
  reg [15:0] x_dim; // @[BlurFilter.scala 116:18]
  reg [15:0] y_dim; // @[BlurFilter.scala 117:18]
  reg [15:0] x_ub; // @[BlurFilter.scala 120:17]
  reg [15:0] y_ub; // @[BlurFilter.scala 122:17]
  reg  idle; // @[BlurFilter.scala 124:21]
  wire  _io_pixel_in_imgh_ready_T = io_pixel_in_imgh_valid & io_pixel_in_imgw_valid; // @[BlurFilter.scala 148:55]
  wire [15:0] _x_ub_T_2 = $signed(io_pixel_in_imgw_bits) - 16'sh2; // @[BlurFilter.scala 158:24]
  wire [15:0] _y_ub_T_2 = $signed(io_pixel_in_imgh_bits) - 16'sh2; // @[BlurFilter.scala 161:24]
  wire  _GEN_0 = $signed(io_pixel_in_imgw_bits) != 16'sh0 & $signed(io_pixel_in_imgh_bits) != 16'sh0 ? 1'h0 : 1'h1; // @[BlurFilter.scala 166:62 167:14 169:14]
  wire  _GEN_9 = _io_pixel_in_imgh_ready_T ? _GEN_0 : idle; // @[BlurFilter.scala 124:21 152:59]
  wire [15:0] _can_pass_T_6 = 16'sh1 + $signed(x_ub); // @[BlurFilter.scala 177:74]
  wire  _can_pass_T_12 = $signed(y_index) >= 16'sh1; // @[BlurFilter.scala 178:18]
  wire  _can_pass_T_13 = $signed(x_index) >= 16'sh2 & $signed(x_index) <= $signed(_can_pass_T_6) & _can_pass_T_12; // @[BlurFilter.scala 177:83]
  wire [16:0] _can_pass_T_14 = {{1{y_ub[15]}},y_ub}; // @[BlurFilter.scala 178:76]
  wire [15:0] _can_pass_T_16 = _can_pass_T_14[15:0]; // @[BlurFilter.scala 178:76]
  wire  can_pass = _can_pass_T_13 & $signed(y_index) <= $signed(_can_pass_T_16); // @[BlurFilter.scala 178:46]
  wire  _T_4 = io_pixel_out_ready & io_pixel_out_valid; // @[Decoupled.scala 50:35]
  wire [15:0] _T_7 = $signed(x_dim) - 16'sh1; // @[BlurFilter.scala 127:29]
  wire [15:0] _T_11 = $signed(y_dim) - 16'sh1; // @[BlurFilter.scala 129:31]
  wire [15:0] _y_index_T_2 = $signed(y_index) + 16'sh1; // @[BlurFilter.scala 133:28]
  wire  _GEN_10 = $signed(y_index) == $signed(_T_11) | idle; // @[BlurFilter.scala 129:39 130:17 124:21]
  wire [15:0] _GEN_11 = $signed(y_index) == $signed(_T_11) ? $signed(16'sh0) : $signed(_y_index_T_2); // @[BlurFilter.scala 129:39 131:17 133:17]
  wire [15:0] _x_index_T_2 = $signed(x_index) + 16'sh1; // @[BlurFilter.scala 136:26]
  wire [15:0] _GEN_12 = $signed(x_index) == $signed(_T_7) ? $signed(16'sh0) : $signed(_x_index_T_2); // @[BlurFilter.scala 127:37 128:15 136:15]
  wire  _GEN_13 = $signed(x_index) == $signed(_T_7) ? _GEN_10 : idle; // @[BlurFilter.scala 124:21 127:37]
  wire [15:0] _GEN_14 = $signed(x_index) == $signed(_T_7) ? $signed(_GEN_11) : $signed(y_index); // @[BlurFilter.scala 112:23 127:37]
  wire  _GEN_16 = _T_4 ? _GEN_13 : idle; // @[BlurFilter.scala 124:21 183:31]
  wire  _T_13 = io_pixel_in_pixel_ready & io_pixel_in_pixel_valid; // @[Decoupled.scala 50:35]
  wire  _GEN_24 = _T_13 ? _GEN_13 : idle; // @[BlurFilter.scala 124:21 189:36]
  wire  _GEN_26 = can_pass & io_pixel_in_pixel_valid; // @[BlurFilter.scala 180:20 181:31 187:31]
  wire  _GEN_27 = can_pass ? io_pixel_out_ready : 1'h1; // @[BlurFilter.scala 180:20 182:31 188:31]
  wire  _GEN_29 = can_pass ? _GEN_16 : _GEN_24; // @[BlurFilter.scala 180:20]
  wire  _GEN_41 = idle ? _GEN_9 : _GEN_29; // @[BlurFilter.scala 147:14]
  assign io_pixel_in_pixel_ready = idle ? 1'h0 : _GEN_27; // @[BlurFilter.scala 147:14 143:27]
  assign io_pixel_in_imgw_ready = idle & _io_pixel_in_imgh_ready_T; // @[BlurFilter.scala 147:14 141:27 149:28]
  assign io_pixel_in_imgh_ready = idle & (io_pixel_in_imgh_valid & io_pixel_in_imgw_valid); // @[BlurFilter.scala 147:14 140:27 148:28]
  assign io_pixel_out_valid = idle ? 1'h0 : _GEN_26; // @[BlurFilter.scala 147:14 142:27]
  assign io_pixel_out_bits = io_pixel_in_pixel_bits; // @[BlurFilter.scala 145:21]
  always @(posedge clock) begin
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_index <= 16'sh0; // @[BlurFilter.scala 163:15]
      end
    end else if (can_pass) begin // @[BlurFilter.scala 180:20]
      if (_T_4) begin // @[BlurFilter.scala 183:31]
        x_index <= _GEN_12;
      end
    end else if (_T_13) begin // @[BlurFilter.scala 189:36]
      x_index <= _GEN_12;
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_index <= 16'sh0; // @[BlurFilter.scala 164:15]
      end
    end else if (can_pass) begin // @[BlurFilter.scala 180:20]
      if (_T_4) begin // @[BlurFilter.scala 183:31]
        y_index <= _GEN_14;
      end
    end else if (_T_13) begin // @[BlurFilter.scala 189:36]
      y_index <= _GEN_14;
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_dim <= io_pixel_in_imgw_bits; // @[BlurFilter.scala 155:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_dim <= io_pixel_in_imgh_bits; // @[BlurFilter.scala 156:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_ub <= _x_ub_T_2; // @[BlurFilter.scala 158:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_ub <= _y_ub_T_2; // @[BlurFilter.scala 161:13]
      end
    end
    idle <= reset | _GEN_41; // @[BlurFilter.scala 124:{21,21}]
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
  x_index = _RAND_0[15:0];
  _RAND_1 = {1{`RANDOM}};
  y_index = _RAND_1[15:0];
  _RAND_2 = {1{`RANDOM}};
  x_dim = _RAND_2[15:0];
  _RAND_3 = {1{`RANDOM}};
  y_dim = _RAND_3[15:0];
  _RAND_4 = {1{`RANDOM}};
  x_ub = _RAND_4[15:0];
  _RAND_5 = {1{`RANDOM}};
  y_ub = _RAND_5[15:0];
  _RAND_6 = {1{`RANDOM}};
  idle = _RAND_6[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module DynamicFilter_4(
  input         clock,
  input         reset,
  output        io_pixel_in_pixel_ready,
  input         io_pixel_in_pixel_valid,
  input  [7:0]  io_pixel_in_pixel_bits,
  output        io_pixel_in_imgw_ready,
  input         io_pixel_in_imgw_valid,
  input  [15:0] io_pixel_in_imgw_bits,
  output        io_pixel_in_imgh_ready,
  input         io_pixel_in_imgh_valid,
  input  [15:0] io_pixel_in_imgh_bits,
  input         io_pixel_out_ready,
  output        io_pixel_out_valid,
  output [7:0]  io_pixel_out_bits
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
  reg [15:0] x_index; // @[BlurFilter.scala 112:23]
  reg [15:0] y_index; // @[BlurFilter.scala 112:23]
  reg [15:0] x_dim; // @[BlurFilter.scala 116:18]
  reg [15:0] y_dim; // @[BlurFilter.scala 117:18]
  reg [15:0] x_ub; // @[BlurFilter.scala 120:17]
  reg [15:0] y_ub; // @[BlurFilter.scala 122:17]
  reg  idle; // @[BlurFilter.scala 124:21]
  wire  _io_pixel_in_imgh_ready_T = io_pixel_in_imgh_valid & io_pixel_in_imgw_valid; // @[BlurFilter.scala 148:55]
  wire [15:0] _x_ub_T_2 = $signed(io_pixel_in_imgw_bits) - 16'sh2; // @[BlurFilter.scala 158:24]
  wire [15:0] _y_ub_T_2 = $signed(io_pixel_in_imgh_bits) - 16'sh2; // @[BlurFilter.scala 161:24]
  wire  _GEN_0 = $signed(io_pixel_in_imgw_bits) != 16'sh0 & $signed(io_pixel_in_imgh_bits) != 16'sh0 ? 1'h0 : 1'h1; // @[BlurFilter.scala 166:62 167:14 169:14]
  wire  _GEN_9 = _io_pixel_in_imgh_ready_T ? _GEN_0 : idle; // @[BlurFilter.scala 124:21 152:59]
  wire [16:0] _can_pass_T_4 = {{1{x_ub[15]}},x_ub}; // @[BlurFilter.scala 177:74]
  wire [15:0] _can_pass_T_6 = _can_pass_T_4[15:0]; // @[BlurFilter.scala 177:74]
  wire  _can_pass_T_12 = $signed(y_index) >= 16'sh1; // @[BlurFilter.scala 178:18]
  wire  _can_pass_T_13 = $signed(x_index) >= 16'sh1 & $signed(x_index) <= $signed(_can_pass_T_6) & _can_pass_T_12; // @[BlurFilter.scala 177:83]
  wire [16:0] _can_pass_T_14 = {{1{y_ub[15]}},y_ub}; // @[BlurFilter.scala 178:76]
  wire [15:0] _can_pass_T_16 = _can_pass_T_14[15:0]; // @[BlurFilter.scala 178:76]
  wire  can_pass = _can_pass_T_13 & $signed(y_index) <= $signed(_can_pass_T_16); // @[BlurFilter.scala 178:46]
  wire  _T_4 = io_pixel_out_ready & io_pixel_out_valid; // @[Decoupled.scala 50:35]
  wire [15:0] _T_7 = $signed(x_dim) - 16'sh1; // @[BlurFilter.scala 127:29]
  wire [15:0] _T_11 = $signed(y_dim) - 16'sh1; // @[BlurFilter.scala 129:31]
  wire [15:0] _y_index_T_2 = $signed(y_index) + 16'sh1; // @[BlurFilter.scala 133:28]
  wire  _GEN_10 = $signed(y_index) == $signed(_T_11) | idle; // @[BlurFilter.scala 129:39 130:17 124:21]
  wire [15:0] _GEN_11 = $signed(y_index) == $signed(_T_11) ? $signed(16'sh0) : $signed(_y_index_T_2); // @[BlurFilter.scala 129:39 131:17 133:17]
  wire [15:0] _x_index_T_2 = $signed(x_index) + 16'sh1; // @[BlurFilter.scala 136:26]
  wire [15:0] _GEN_12 = $signed(x_index) == $signed(_T_7) ? $signed(16'sh0) : $signed(_x_index_T_2); // @[BlurFilter.scala 127:37 128:15 136:15]
  wire  _GEN_13 = $signed(x_index) == $signed(_T_7) ? _GEN_10 : idle; // @[BlurFilter.scala 124:21 127:37]
  wire [15:0] _GEN_14 = $signed(x_index) == $signed(_T_7) ? $signed(_GEN_11) : $signed(y_index); // @[BlurFilter.scala 112:23 127:37]
  wire  _GEN_16 = _T_4 ? _GEN_13 : idle; // @[BlurFilter.scala 124:21 183:31]
  wire  _T_13 = io_pixel_in_pixel_ready & io_pixel_in_pixel_valid; // @[Decoupled.scala 50:35]
  wire  _GEN_24 = _T_13 ? _GEN_13 : idle; // @[BlurFilter.scala 124:21 189:36]
  wire  _GEN_26 = can_pass & io_pixel_in_pixel_valid; // @[BlurFilter.scala 180:20 181:31 187:31]
  wire  _GEN_27 = can_pass ? io_pixel_out_ready : 1'h1; // @[BlurFilter.scala 180:20 182:31 188:31]
  wire  _GEN_29 = can_pass ? _GEN_16 : _GEN_24; // @[BlurFilter.scala 180:20]
  wire  _GEN_41 = idle ? _GEN_9 : _GEN_29; // @[BlurFilter.scala 147:14]
  assign io_pixel_in_pixel_ready = idle ? 1'h0 : _GEN_27; // @[BlurFilter.scala 147:14 143:27]
  assign io_pixel_in_imgw_ready = idle & _io_pixel_in_imgh_ready_T; // @[BlurFilter.scala 147:14 141:27 149:28]
  assign io_pixel_in_imgh_ready = idle & (io_pixel_in_imgh_valid & io_pixel_in_imgw_valid); // @[BlurFilter.scala 147:14 140:27 148:28]
  assign io_pixel_out_valid = idle ? 1'h0 : _GEN_26; // @[BlurFilter.scala 147:14 142:27]
  assign io_pixel_out_bits = io_pixel_in_pixel_bits; // @[BlurFilter.scala 145:21]
  always @(posedge clock) begin
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_index <= 16'sh0; // @[BlurFilter.scala 163:15]
      end
    end else if (can_pass) begin // @[BlurFilter.scala 180:20]
      if (_T_4) begin // @[BlurFilter.scala 183:31]
        x_index <= _GEN_12;
      end
    end else if (_T_13) begin // @[BlurFilter.scala 189:36]
      x_index <= _GEN_12;
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_index <= 16'sh0; // @[BlurFilter.scala 164:15]
      end
    end else if (can_pass) begin // @[BlurFilter.scala 180:20]
      if (_T_4) begin // @[BlurFilter.scala 183:31]
        y_index <= _GEN_14;
      end
    end else if (_T_13) begin // @[BlurFilter.scala 189:36]
      y_index <= _GEN_14;
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_dim <= io_pixel_in_imgw_bits; // @[BlurFilter.scala 155:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_dim <= io_pixel_in_imgh_bits; // @[BlurFilter.scala 156:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_ub <= _x_ub_T_2; // @[BlurFilter.scala 158:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_ub <= _y_ub_T_2; // @[BlurFilter.scala 161:13]
      end
    end
    idle <= reset | _GEN_41; // @[BlurFilter.scala 124:{21,21}]
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
  x_index = _RAND_0[15:0];
  _RAND_1 = {1{`RANDOM}};
  y_index = _RAND_1[15:0];
  _RAND_2 = {1{`RANDOM}};
  x_dim = _RAND_2[15:0];
  _RAND_3 = {1{`RANDOM}};
  y_dim = _RAND_3[15:0];
  _RAND_4 = {1{`RANDOM}};
  x_ub = _RAND_4[15:0];
  _RAND_5 = {1{`RANDOM}};
  y_ub = _RAND_5[15:0];
  _RAND_6 = {1{`RANDOM}};
  idle = _RAND_6[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module DynamicFilter_5(
  input         clock,
  input         reset,
  output        io_pixel_in_pixel_ready,
  input         io_pixel_in_pixel_valid,
  input  [7:0]  io_pixel_in_pixel_bits,
  output        io_pixel_in_imgw_ready,
  input         io_pixel_in_imgw_valid,
  input  [15:0] io_pixel_in_imgw_bits,
  output        io_pixel_in_imgh_ready,
  input         io_pixel_in_imgh_valid,
  input  [15:0] io_pixel_in_imgh_bits,
  input         io_pixel_out_ready,
  output        io_pixel_out_valid,
  output [7:0]  io_pixel_out_bits
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
  reg [15:0] x_index; // @[BlurFilter.scala 112:23]
  reg [15:0] y_index; // @[BlurFilter.scala 112:23]
  reg [15:0] x_dim; // @[BlurFilter.scala 116:18]
  reg [15:0] y_dim; // @[BlurFilter.scala 117:18]
  reg [15:0] x_ub; // @[BlurFilter.scala 120:17]
  reg [15:0] y_ub; // @[BlurFilter.scala 122:17]
  reg  idle; // @[BlurFilter.scala 124:21]
  wire  _io_pixel_in_imgh_ready_T = io_pixel_in_imgh_valid & io_pixel_in_imgw_valid; // @[BlurFilter.scala 148:55]
  wire [15:0] _x_ub_T_2 = $signed(io_pixel_in_imgw_bits) - 16'sh2; // @[BlurFilter.scala 158:24]
  wire [15:0] _y_ub_T_2 = $signed(io_pixel_in_imgh_bits) - 16'sh2; // @[BlurFilter.scala 161:24]
  wire  _GEN_0 = $signed(io_pixel_in_imgw_bits) != 16'sh0 & $signed(io_pixel_in_imgh_bits) != 16'sh0 ? 1'h0 : 1'h1; // @[BlurFilter.scala 166:62 167:14 169:14]
  wire  _GEN_9 = _io_pixel_in_imgh_ready_T ? _GEN_0 : idle; // @[BlurFilter.scala 124:21 152:59]
  wire [15:0] _can_pass_T_6 = $signed(x_ub) - 16'sh1; // @[BlurFilter.scala 177:74]
  wire  _can_pass_T_12 = $signed(y_index) >= 16'sh1; // @[BlurFilter.scala 178:18]
  wire  _can_pass_T_13 = $signed(x_index) >= 16'sh0 & $signed(x_index) <= $signed(_can_pass_T_6) & _can_pass_T_12; // @[BlurFilter.scala 177:83]
  wire [16:0] _can_pass_T_14 = {{1{y_ub[15]}},y_ub}; // @[BlurFilter.scala 178:76]
  wire [15:0] _can_pass_T_16 = _can_pass_T_14[15:0]; // @[BlurFilter.scala 178:76]
  wire  can_pass = _can_pass_T_13 & $signed(y_index) <= $signed(_can_pass_T_16); // @[BlurFilter.scala 178:46]
  wire  _T_4 = io_pixel_out_ready & io_pixel_out_valid; // @[Decoupled.scala 50:35]
  wire [15:0] _T_7 = $signed(x_dim) - 16'sh1; // @[BlurFilter.scala 127:29]
  wire [15:0] _T_11 = $signed(y_dim) - 16'sh1; // @[BlurFilter.scala 129:31]
  wire [15:0] _y_index_T_2 = $signed(y_index) + 16'sh1; // @[BlurFilter.scala 133:28]
  wire  _GEN_10 = $signed(y_index) == $signed(_T_11) | idle; // @[BlurFilter.scala 129:39 130:17 124:21]
  wire [15:0] _GEN_11 = $signed(y_index) == $signed(_T_11) ? $signed(16'sh0) : $signed(_y_index_T_2); // @[BlurFilter.scala 129:39 131:17 133:17]
  wire [15:0] _x_index_T_2 = $signed(x_index) + 16'sh1; // @[BlurFilter.scala 136:26]
  wire [15:0] _GEN_12 = $signed(x_index) == $signed(_T_7) ? $signed(16'sh0) : $signed(_x_index_T_2); // @[BlurFilter.scala 127:37 128:15 136:15]
  wire  _GEN_13 = $signed(x_index) == $signed(_T_7) ? _GEN_10 : idle; // @[BlurFilter.scala 124:21 127:37]
  wire [15:0] _GEN_14 = $signed(x_index) == $signed(_T_7) ? $signed(_GEN_11) : $signed(y_index); // @[BlurFilter.scala 112:23 127:37]
  wire  _GEN_16 = _T_4 ? _GEN_13 : idle; // @[BlurFilter.scala 124:21 183:31]
  wire  _T_13 = io_pixel_in_pixel_ready & io_pixel_in_pixel_valid; // @[Decoupled.scala 50:35]
  wire  _GEN_24 = _T_13 ? _GEN_13 : idle; // @[BlurFilter.scala 124:21 189:36]
  wire  _GEN_26 = can_pass & io_pixel_in_pixel_valid; // @[BlurFilter.scala 180:20 181:31 187:31]
  wire  _GEN_27 = can_pass ? io_pixel_out_ready : 1'h1; // @[BlurFilter.scala 180:20 182:31 188:31]
  wire  _GEN_29 = can_pass ? _GEN_16 : _GEN_24; // @[BlurFilter.scala 180:20]
  wire  _GEN_41 = idle ? _GEN_9 : _GEN_29; // @[BlurFilter.scala 147:14]
  assign io_pixel_in_pixel_ready = idle ? 1'h0 : _GEN_27; // @[BlurFilter.scala 147:14 143:27]
  assign io_pixel_in_imgw_ready = idle & _io_pixel_in_imgh_ready_T; // @[BlurFilter.scala 147:14 141:27 149:28]
  assign io_pixel_in_imgh_ready = idle & (io_pixel_in_imgh_valid & io_pixel_in_imgw_valid); // @[BlurFilter.scala 147:14 140:27 148:28]
  assign io_pixel_out_valid = idle ? 1'h0 : _GEN_26; // @[BlurFilter.scala 147:14 142:27]
  assign io_pixel_out_bits = io_pixel_in_pixel_bits; // @[BlurFilter.scala 145:21]
  always @(posedge clock) begin
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_index <= 16'sh0; // @[BlurFilter.scala 163:15]
      end
    end else if (can_pass) begin // @[BlurFilter.scala 180:20]
      if (_T_4) begin // @[BlurFilter.scala 183:31]
        x_index <= _GEN_12;
      end
    end else if (_T_13) begin // @[BlurFilter.scala 189:36]
      x_index <= _GEN_12;
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_index <= 16'sh0; // @[BlurFilter.scala 164:15]
      end
    end else if (can_pass) begin // @[BlurFilter.scala 180:20]
      if (_T_4) begin // @[BlurFilter.scala 183:31]
        y_index <= _GEN_14;
      end
    end else if (_T_13) begin // @[BlurFilter.scala 189:36]
      y_index <= _GEN_14;
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_dim <= io_pixel_in_imgw_bits; // @[BlurFilter.scala 155:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_dim <= io_pixel_in_imgh_bits; // @[BlurFilter.scala 156:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_ub <= _x_ub_T_2; // @[BlurFilter.scala 158:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_ub <= _y_ub_T_2; // @[BlurFilter.scala 161:13]
      end
    end
    idle <= reset | _GEN_41; // @[BlurFilter.scala 124:{21,21}]
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
  x_index = _RAND_0[15:0];
  _RAND_1 = {1{`RANDOM}};
  y_index = _RAND_1[15:0];
  _RAND_2 = {1{`RANDOM}};
  x_dim = _RAND_2[15:0];
  _RAND_3 = {1{`RANDOM}};
  y_dim = _RAND_3[15:0];
  _RAND_4 = {1{`RANDOM}};
  x_ub = _RAND_4[15:0];
  _RAND_5 = {1{`RANDOM}};
  y_ub = _RAND_5[15:0];
  _RAND_6 = {1{`RANDOM}};
  idle = _RAND_6[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module DynamicFilter_6(
  input         clock,
  input         reset,
  output        io_pixel_in_pixel_ready,
  input         io_pixel_in_pixel_valid,
  input  [7:0]  io_pixel_in_pixel_bits,
  output        io_pixel_in_imgw_ready,
  input         io_pixel_in_imgw_valid,
  input  [15:0] io_pixel_in_imgw_bits,
  output        io_pixel_in_imgh_ready,
  input         io_pixel_in_imgh_valid,
  input  [15:0] io_pixel_in_imgh_bits,
  input         io_pixel_out_ready,
  output        io_pixel_out_valid,
  output [7:0]  io_pixel_out_bits
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
  reg [15:0] x_index; // @[BlurFilter.scala 112:23]
  reg [15:0] y_index; // @[BlurFilter.scala 112:23]
  reg [15:0] x_dim; // @[BlurFilter.scala 116:18]
  reg [15:0] y_dim; // @[BlurFilter.scala 117:18]
  reg [15:0] x_ub; // @[BlurFilter.scala 120:17]
  reg [15:0] y_ub; // @[BlurFilter.scala 122:17]
  reg  idle; // @[BlurFilter.scala 124:21]
  wire  _io_pixel_in_imgh_ready_T = io_pixel_in_imgh_valid & io_pixel_in_imgw_valid; // @[BlurFilter.scala 148:55]
  wire [15:0] _x_ub_T_2 = $signed(io_pixel_in_imgw_bits) - 16'sh2; // @[BlurFilter.scala 158:24]
  wire [15:0] _y_ub_T_2 = $signed(io_pixel_in_imgh_bits) - 16'sh2; // @[BlurFilter.scala 161:24]
  wire  _GEN_0 = $signed(io_pixel_in_imgw_bits) != 16'sh0 & $signed(io_pixel_in_imgh_bits) != 16'sh0 ? 1'h0 : 1'h1; // @[BlurFilter.scala 166:62 167:14 169:14]
  wire  _GEN_9 = _io_pixel_in_imgh_ready_T ? _GEN_0 : idle; // @[BlurFilter.scala 124:21 152:59]
  wire [15:0] _can_pass_T_6 = 16'sh1 + $signed(x_ub); // @[BlurFilter.scala 177:74]
  wire  _can_pass_T_12 = $signed(y_index) >= 16'sh0; // @[BlurFilter.scala 178:18]
  wire  _can_pass_T_13 = $signed(x_index) >= 16'sh2 & $signed(x_index) <= $signed(_can_pass_T_6) & _can_pass_T_12; // @[BlurFilter.scala 177:83]
  wire [15:0] _can_pass_T_16 = $signed(y_ub) - 16'sh1; // @[BlurFilter.scala 178:76]
  wire  can_pass = _can_pass_T_13 & $signed(y_index) <= $signed(_can_pass_T_16); // @[BlurFilter.scala 178:46]
  wire  _T_4 = io_pixel_out_ready & io_pixel_out_valid; // @[Decoupled.scala 50:35]
  wire [15:0] _T_7 = $signed(x_dim) - 16'sh1; // @[BlurFilter.scala 127:29]
  wire [15:0] _T_11 = $signed(y_dim) - 16'sh1; // @[BlurFilter.scala 129:31]
  wire [15:0] _y_index_T_2 = $signed(y_index) + 16'sh1; // @[BlurFilter.scala 133:28]
  wire  _GEN_10 = $signed(y_index) == $signed(_T_11) | idle; // @[BlurFilter.scala 129:39 130:17 124:21]
  wire [15:0] _GEN_11 = $signed(y_index) == $signed(_T_11) ? $signed(16'sh0) : $signed(_y_index_T_2); // @[BlurFilter.scala 129:39 131:17 133:17]
  wire [15:0] _x_index_T_2 = $signed(x_index) + 16'sh1; // @[BlurFilter.scala 136:26]
  wire [15:0] _GEN_12 = $signed(x_index) == $signed(_T_7) ? $signed(16'sh0) : $signed(_x_index_T_2); // @[BlurFilter.scala 127:37 128:15 136:15]
  wire  _GEN_13 = $signed(x_index) == $signed(_T_7) ? _GEN_10 : idle; // @[BlurFilter.scala 124:21 127:37]
  wire [15:0] _GEN_14 = $signed(x_index) == $signed(_T_7) ? $signed(_GEN_11) : $signed(y_index); // @[BlurFilter.scala 112:23 127:37]
  wire  _GEN_16 = _T_4 ? _GEN_13 : idle; // @[BlurFilter.scala 124:21 183:31]
  wire  _T_13 = io_pixel_in_pixel_ready & io_pixel_in_pixel_valid; // @[Decoupled.scala 50:35]
  wire  _GEN_24 = _T_13 ? _GEN_13 : idle; // @[BlurFilter.scala 124:21 189:36]
  wire  _GEN_26 = can_pass & io_pixel_in_pixel_valid; // @[BlurFilter.scala 180:20 181:31 187:31]
  wire  _GEN_27 = can_pass ? io_pixel_out_ready : 1'h1; // @[BlurFilter.scala 180:20 182:31 188:31]
  wire  _GEN_29 = can_pass ? _GEN_16 : _GEN_24; // @[BlurFilter.scala 180:20]
  wire  _GEN_41 = idle ? _GEN_9 : _GEN_29; // @[BlurFilter.scala 147:14]
  assign io_pixel_in_pixel_ready = idle ? 1'h0 : _GEN_27; // @[BlurFilter.scala 147:14 143:27]
  assign io_pixel_in_imgw_ready = idle & _io_pixel_in_imgh_ready_T; // @[BlurFilter.scala 147:14 141:27 149:28]
  assign io_pixel_in_imgh_ready = idle & (io_pixel_in_imgh_valid & io_pixel_in_imgw_valid); // @[BlurFilter.scala 147:14 140:27 148:28]
  assign io_pixel_out_valid = idle ? 1'h0 : _GEN_26; // @[BlurFilter.scala 147:14 142:27]
  assign io_pixel_out_bits = io_pixel_in_pixel_bits; // @[BlurFilter.scala 145:21]
  always @(posedge clock) begin
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_index <= 16'sh0; // @[BlurFilter.scala 163:15]
      end
    end else if (can_pass) begin // @[BlurFilter.scala 180:20]
      if (_T_4) begin // @[BlurFilter.scala 183:31]
        x_index <= _GEN_12;
      end
    end else if (_T_13) begin // @[BlurFilter.scala 189:36]
      x_index <= _GEN_12;
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_index <= 16'sh0; // @[BlurFilter.scala 164:15]
      end
    end else if (can_pass) begin // @[BlurFilter.scala 180:20]
      if (_T_4) begin // @[BlurFilter.scala 183:31]
        y_index <= _GEN_14;
      end
    end else if (_T_13) begin // @[BlurFilter.scala 189:36]
      y_index <= _GEN_14;
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_dim <= io_pixel_in_imgw_bits; // @[BlurFilter.scala 155:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_dim <= io_pixel_in_imgh_bits; // @[BlurFilter.scala 156:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_ub <= _x_ub_T_2; // @[BlurFilter.scala 158:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_ub <= _y_ub_T_2; // @[BlurFilter.scala 161:13]
      end
    end
    idle <= reset | _GEN_41; // @[BlurFilter.scala 124:{21,21}]
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
  x_index = _RAND_0[15:0];
  _RAND_1 = {1{`RANDOM}};
  y_index = _RAND_1[15:0];
  _RAND_2 = {1{`RANDOM}};
  x_dim = _RAND_2[15:0];
  _RAND_3 = {1{`RANDOM}};
  y_dim = _RAND_3[15:0];
  _RAND_4 = {1{`RANDOM}};
  x_ub = _RAND_4[15:0];
  _RAND_5 = {1{`RANDOM}};
  y_ub = _RAND_5[15:0];
  _RAND_6 = {1{`RANDOM}};
  idle = _RAND_6[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module DynamicFilter_7(
  input         clock,
  input         reset,
  output        io_pixel_in_pixel_ready,
  input         io_pixel_in_pixel_valid,
  input  [7:0]  io_pixel_in_pixel_bits,
  output        io_pixel_in_imgw_ready,
  input         io_pixel_in_imgw_valid,
  input  [15:0] io_pixel_in_imgw_bits,
  output        io_pixel_in_imgh_ready,
  input         io_pixel_in_imgh_valid,
  input  [15:0] io_pixel_in_imgh_bits,
  input         io_pixel_out_ready,
  output        io_pixel_out_valid,
  output [7:0]  io_pixel_out_bits
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
  reg [15:0] x_index; // @[BlurFilter.scala 112:23]
  reg [15:0] y_index; // @[BlurFilter.scala 112:23]
  reg [15:0] x_dim; // @[BlurFilter.scala 116:18]
  reg [15:0] y_dim; // @[BlurFilter.scala 117:18]
  reg [15:0] x_ub; // @[BlurFilter.scala 120:17]
  reg [15:0] y_ub; // @[BlurFilter.scala 122:17]
  reg  idle; // @[BlurFilter.scala 124:21]
  wire  _io_pixel_in_imgh_ready_T = io_pixel_in_imgh_valid & io_pixel_in_imgw_valid; // @[BlurFilter.scala 148:55]
  wire [15:0] _x_ub_T_2 = $signed(io_pixel_in_imgw_bits) - 16'sh2; // @[BlurFilter.scala 158:24]
  wire [15:0] _y_ub_T_2 = $signed(io_pixel_in_imgh_bits) - 16'sh2; // @[BlurFilter.scala 161:24]
  wire  _GEN_0 = $signed(io_pixel_in_imgw_bits) != 16'sh0 & $signed(io_pixel_in_imgh_bits) != 16'sh0 ? 1'h0 : 1'h1; // @[BlurFilter.scala 166:62 167:14 169:14]
  wire  _GEN_9 = _io_pixel_in_imgh_ready_T ? _GEN_0 : idle; // @[BlurFilter.scala 124:21 152:59]
  wire [16:0] _can_pass_T_4 = {{1{x_ub[15]}},x_ub}; // @[BlurFilter.scala 177:74]
  wire [15:0] _can_pass_T_6 = _can_pass_T_4[15:0]; // @[BlurFilter.scala 177:74]
  wire  _can_pass_T_12 = $signed(y_index) >= 16'sh0; // @[BlurFilter.scala 178:18]
  wire  _can_pass_T_13 = $signed(x_index) >= 16'sh1 & $signed(x_index) <= $signed(_can_pass_T_6) & _can_pass_T_12; // @[BlurFilter.scala 177:83]
  wire [15:0] _can_pass_T_16 = $signed(y_ub) - 16'sh1; // @[BlurFilter.scala 178:76]
  wire  can_pass = _can_pass_T_13 & $signed(y_index) <= $signed(_can_pass_T_16); // @[BlurFilter.scala 178:46]
  wire  _T_4 = io_pixel_out_ready & io_pixel_out_valid; // @[Decoupled.scala 50:35]
  wire [15:0] _T_7 = $signed(x_dim) - 16'sh1; // @[BlurFilter.scala 127:29]
  wire [15:0] _T_11 = $signed(y_dim) - 16'sh1; // @[BlurFilter.scala 129:31]
  wire [15:0] _y_index_T_2 = $signed(y_index) + 16'sh1; // @[BlurFilter.scala 133:28]
  wire  _GEN_10 = $signed(y_index) == $signed(_T_11) | idle; // @[BlurFilter.scala 129:39 130:17 124:21]
  wire [15:0] _GEN_11 = $signed(y_index) == $signed(_T_11) ? $signed(16'sh0) : $signed(_y_index_T_2); // @[BlurFilter.scala 129:39 131:17 133:17]
  wire [15:0] _x_index_T_2 = $signed(x_index) + 16'sh1; // @[BlurFilter.scala 136:26]
  wire [15:0] _GEN_12 = $signed(x_index) == $signed(_T_7) ? $signed(16'sh0) : $signed(_x_index_T_2); // @[BlurFilter.scala 127:37 128:15 136:15]
  wire  _GEN_13 = $signed(x_index) == $signed(_T_7) ? _GEN_10 : idle; // @[BlurFilter.scala 124:21 127:37]
  wire [15:0] _GEN_14 = $signed(x_index) == $signed(_T_7) ? $signed(_GEN_11) : $signed(y_index); // @[BlurFilter.scala 112:23 127:37]
  wire  _GEN_16 = _T_4 ? _GEN_13 : idle; // @[BlurFilter.scala 124:21 183:31]
  wire  _T_13 = io_pixel_in_pixel_ready & io_pixel_in_pixel_valid; // @[Decoupled.scala 50:35]
  wire  _GEN_24 = _T_13 ? _GEN_13 : idle; // @[BlurFilter.scala 124:21 189:36]
  wire  _GEN_26 = can_pass & io_pixel_in_pixel_valid; // @[BlurFilter.scala 180:20 181:31 187:31]
  wire  _GEN_27 = can_pass ? io_pixel_out_ready : 1'h1; // @[BlurFilter.scala 180:20 182:31 188:31]
  wire  _GEN_29 = can_pass ? _GEN_16 : _GEN_24; // @[BlurFilter.scala 180:20]
  wire  _GEN_41 = idle ? _GEN_9 : _GEN_29; // @[BlurFilter.scala 147:14]
  assign io_pixel_in_pixel_ready = idle ? 1'h0 : _GEN_27; // @[BlurFilter.scala 147:14 143:27]
  assign io_pixel_in_imgw_ready = idle & _io_pixel_in_imgh_ready_T; // @[BlurFilter.scala 147:14 141:27 149:28]
  assign io_pixel_in_imgh_ready = idle & (io_pixel_in_imgh_valid & io_pixel_in_imgw_valid); // @[BlurFilter.scala 147:14 140:27 148:28]
  assign io_pixel_out_valid = idle ? 1'h0 : _GEN_26; // @[BlurFilter.scala 147:14 142:27]
  assign io_pixel_out_bits = io_pixel_in_pixel_bits; // @[BlurFilter.scala 145:21]
  always @(posedge clock) begin
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_index <= 16'sh0; // @[BlurFilter.scala 163:15]
      end
    end else if (can_pass) begin // @[BlurFilter.scala 180:20]
      if (_T_4) begin // @[BlurFilter.scala 183:31]
        x_index <= _GEN_12;
      end
    end else if (_T_13) begin // @[BlurFilter.scala 189:36]
      x_index <= _GEN_12;
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_index <= 16'sh0; // @[BlurFilter.scala 164:15]
      end
    end else if (can_pass) begin // @[BlurFilter.scala 180:20]
      if (_T_4) begin // @[BlurFilter.scala 183:31]
        y_index <= _GEN_14;
      end
    end else if (_T_13) begin // @[BlurFilter.scala 189:36]
      y_index <= _GEN_14;
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_dim <= io_pixel_in_imgw_bits; // @[BlurFilter.scala 155:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_dim <= io_pixel_in_imgh_bits; // @[BlurFilter.scala 156:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_ub <= _x_ub_T_2; // @[BlurFilter.scala 158:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_ub <= _y_ub_T_2; // @[BlurFilter.scala 161:13]
      end
    end
    idle <= reset | _GEN_41; // @[BlurFilter.scala 124:{21,21}]
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
  x_index = _RAND_0[15:0];
  _RAND_1 = {1{`RANDOM}};
  y_index = _RAND_1[15:0];
  _RAND_2 = {1{`RANDOM}};
  x_dim = _RAND_2[15:0];
  _RAND_3 = {1{`RANDOM}};
  y_dim = _RAND_3[15:0];
  _RAND_4 = {1{`RANDOM}};
  x_ub = _RAND_4[15:0];
  _RAND_5 = {1{`RANDOM}};
  y_ub = _RAND_5[15:0];
  _RAND_6 = {1{`RANDOM}};
  idle = _RAND_6[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module DynamicFilter_8(
  input         clock,
  input         reset,
  output        io_pixel_in_pixel_ready,
  input         io_pixel_in_pixel_valid,
  input  [7:0]  io_pixel_in_pixel_bits,
  output        io_pixel_in_imgw_ready,
  input         io_pixel_in_imgw_valid,
  input  [15:0] io_pixel_in_imgw_bits,
  output        io_pixel_in_imgh_ready,
  input         io_pixel_in_imgh_valid,
  input  [15:0] io_pixel_in_imgh_bits,
  input         io_pixel_out_ready,
  output        io_pixel_out_valid,
  output [7:0]  io_pixel_out_bits
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
  reg [15:0] x_index; // @[BlurFilter.scala 112:23]
  reg [15:0] y_index; // @[BlurFilter.scala 112:23]
  reg [15:0] x_dim; // @[BlurFilter.scala 116:18]
  reg [15:0] y_dim; // @[BlurFilter.scala 117:18]
  reg [15:0] x_ub; // @[BlurFilter.scala 120:17]
  reg [15:0] y_ub; // @[BlurFilter.scala 122:17]
  reg  idle; // @[BlurFilter.scala 124:21]
  wire  _io_pixel_in_imgh_ready_T = io_pixel_in_imgh_valid & io_pixel_in_imgw_valid; // @[BlurFilter.scala 148:55]
  wire [15:0] _x_ub_T_2 = $signed(io_pixel_in_imgw_bits) - 16'sh2; // @[BlurFilter.scala 158:24]
  wire [15:0] _y_ub_T_2 = $signed(io_pixel_in_imgh_bits) - 16'sh2; // @[BlurFilter.scala 161:24]
  wire  _GEN_0 = $signed(io_pixel_in_imgw_bits) != 16'sh0 & $signed(io_pixel_in_imgh_bits) != 16'sh0 ? 1'h0 : 1'h1; // @[BlurFilter.scala 166:62 167:14 169:14]
  wire  _GEN_9 = _io_pixel_in_imgh_ready_T ? _GEN_0 : idle; // @[BlurFilter.scala 124:21 152:59]
  wire [15:0] _can_pass_T_6 = $signed(x_ub) - 16'sh1; // @[BlurFilter.scala 177:74]
  wire  _can_pass_T_12 = $signed(y_index) >= 16'sh0; // @[BlurFilter.scala 178:18]
  wire  _can_pass_T_13 = $signed(x_index) >= 16'sh0 & $signed(x_index) <= $signed(_can_pass_T_6) & _can_pass_T_12; // @[BlurFilter.scala 177:83]
  wire [15:0] _can_pass_T_16 = $signed(y_ub) - 16'sh1; // @[BlurFilter.scala 178:76]
  wire  can_pass = _can_pass_T_13 & $signed(y_index) <= $signed(_can_pass_T_16); // @[BlurFilter.scala 178:46]
  wire  _T_4 = io_pixel_out_ready & io_pixel_out_valid; // @[Decoupled.scala 50:35]
  wire [15:0] _T_7 = $signed(x_dim) - 16'sh1; // @[BlurFilter.scala 127:29]
  wire [15:0] _T_11 = $signed(y_dim) - 16'sh1; // @[BlurFilter.scala 129:31]
  wire [15:0] _y_index_T_2 = $signed(y_index) + 16'sh1; // @[BlurFilter.scala 133:28]
  wire  _GEN_10 = $signed(y_index) == $signed(_T_11) | idle; // @[BlurFilter.scala 129:39 130:17 124:21]
  wire [15:0] _GEN_11 = $signed(y_index) == $signed(_T_11) ? $signed(16'sh0) : $signed(_y_index_T_2); // @[BlurFilter.scala 129:39 131:17 133:17]
  wire [15:0] _x_index_T_2 = $signed(x_index) + 16'sh1; // @[BlurFilter.scala 136:26]
  wire [15:0] _GEN_12 = $signed(x_index) == $signed(_T_7) ? $signed(16'sh0) : $signed(_x_index_T_2); // @[BlurFilter.scala 127:37 128:15 136:15]
  wire  _GEN_13 = $signed(x_index) == $signed(_T_7) ? _GEN_10 : idle; // @[BlurFilter.scala 124:21 127:37]
  wire [15:0] _GEN_14 = $signed(x_index) == $signed(_T_7) ? $signed(_GEN_11) : $signed(y_index); // @[BlurFilter.scala 112:23 127:37]
  wire  _GEN_16 = _T_4 ? _GEN_13 : idle; // @[BlurFilter.scala 124:21 183:31]
  wire  _T_13 = io_pixel_in_pixel_ready & io_pixel_in_pixel_valid; // @[Decoupled.scala 50:35]
  wire  _GEN_24 = _T_13 ? _GEN_13 : idle; // @[BlurFilter.scala 124:21 189:36]
  wire  _GEN_26 = can_pass & io_pixel_in_pixel_valid; // @[BlurFilter.scala 180:20 181:31 187:31]
  wire  _GEN_27 = can_pass ? io_pixel_out_ready : 1'h1; // @[BlurFilter.scala 180:20 182:31 188:31]
  wire  _GEN_29 = can_pass ? _GEN_16 : _GEN_24; // @[BlurFilter.scala 180:20]
  wire  _GEN_41 = idle ? _GEN_9 : _GEN_29; // @[BlurFilter.scala 147:14]
  assign io_pixel_in_pixel_ready = idle ? 1'h0 : _GEN_27; // @[BlurFilter.scala 147:14 143:27]
  assign io_pixel_in_imgw_ready = idle & _io_pixel_in_imgh_ready_T; // @[BlurFilter.scala 147:14 141:27 149:28]
  assign io_pixel_in_imgh_ready = idle & (io_pixel_in_imgh_valid & io_pixel_in_imgw_valid); // @[BlurFilter.scala 147:14 140:27 148:28]
  assign io_pixel_out_valid = idle ? 1'h0 : _GEN_26; // @[BlurFilter.scala 147:14 142:27]
  assign io_pixel_out_bits = io_pixel_in_pixel_bits; // @[BlurFilter.scala 145:21]
  always @(posedge clock) begin
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_index <= 16'sh0; // @[BlurFilter.scala 163:15]
      end
    end else if (can_pass) begin // @[BlurFilter.scala 180:20]
      if (_T_4) begin // @[BlurFilter.scala 183:31]
        x_index <= _GEN_12;
      end
    end else if (_T_13) begin // @[BlurFilter.scala 189:36]
      x_index <= _GEN_12;
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_index <= 16'sh0; // @[BlurFilter.scala 164:15]
      end
    end else if (can_pass) begin // @[BlurFilter.scala 180:20]
      if (_T_4) begin // @[BlurFilter.scala 183:31]
        y_index <= _GEN_14;
      end
    end else if (_T_13) begin // @[BlurFilter.scala 189:36]
      y_index <= _GEN_14;
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_dim <= io_pixel_in_imgw_bits; // @[BlurFilter.scala 155:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_dim <= io_pixel_in_imgh_bits; // @[BlurFilter.scala 156:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        x_ub <= _x_ub_T_2; // @[BlurFilter.scala 158:13]
      end
    end
    if (idle) begin // @[BlurFilter.scala 147:14]
      if (_io_pixel_in_imgh_ready_T) begin // @[BlurFilter.scala 152:59]
        y_ub <= _y_ub_T_2; // @[BlurFilter.scala 161:13]
      end
    end
    idle <= reset | _GEN_41; // @[BlurFilter.scala 124:{21,21}]
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
  x_index = _RAND_0[15:0];
  _RAND_1 = {1{`RANDOM}};
  y_index = _RAND_1[15:0];
  _RAND_2 = {1{`RANDOM}};
  x_dim = _RAND_2[15:0];
  _RAND_3 = {1{`RANDOM}};
  y_dim = _RAND_3[15:0];
  _RAND_4 = {1{`RANDOM}};
  x_ub = _RAND_4[15:0];
  _RAND_5 = {1{`RANDOM}};
  y_ub = _RAND_5[15:0];
  _RAND_6 = {1{`RANDOM}};
  idle = _RAND_6[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Splitter(
  output       io_pixel_in_ready,
  input        io_pixel_in_valid,
  input  [7:0] io_pixel_in_bits,
  input        io_pixel_left_ready,
  output       io_pixel_left_valid,
  output [7:0] io_pixel_left_bits,
  input        io_pixel_right_ready,
  output       io_pixel_right_valid,
  output [7:0] io_pixel_right_bits
);
  wire  can_consume = io_pixel_left_ready & io_pixel_right_ready; // @[BlurFilter.scala 84:53]
  assign io_pixel_in_ready = io_pixel_left_ready & io_pixel_right_ready; // @[BlurFilter.scala 84:53]
  assign io_pixel_left_valid = io_pixel_in_valid & can_consume; // @[BlurFilter.scala 86:45]
  assign io_pixel_left_bits = io_pixel_in_bits; // @[BlurFilter.scala 88:24]
  assign io_pixel_right_valid = io_pixel_in_valid & can_consume; // @[BlurFilter.scala 87:45]
  assign io_pixel_right_bits = io_pixel_in_bits; // @[BlurFilter.scala 89:24]
endmodule
module Gaussian3x3(
  input        clock,
  input        reset,
  output       io_pixels_in_0_ready,
  input        io_pixels_in_0_valid,
  input  [7:0] io_pixels_in_0_bits,
  output       io_pixels_in_1_ready,
  input        io_pixels_in_1_valid,
  input  [7:0] io_pixels_in_1_bits,
  output       io_pixels_in_2_ready,
  input        io_pixels_in_2_valid,
  input  [7:0] io_pixels_in_2_bits,
  output       io_pixels_in_3_ready,
  input        io_pixels_in_3_valid,
  input  [7:0] io_pixels_in_3_bits,
  output       io_pixels_in_4_ready,
  input        io_pixels_in_4_valid,
  input  [7:0] io_pixels_in_4_bits,
  output       io_pixels_in_5_ready,
  input        io_pixels_in_5_valid,
  input  [7:0] io_pixels_in_5_bits,
  output       io_pixels_in_6_ready,
  input        io_pixels_in_6_valid,
  input  [7:0] io_pixels_in_6_bits,
  output       io_pixels_in_7_ready,
  input        io_pixels_in_7_valid,
  input  [7:0] io_pixels_in_7_bits,
  output       io_pixels_in_8_ready,
  input        io_pixels_in_8_valid,
  input  [7:0] io_pixels_in_8_bits,
  input        io_pixel_out_ready,
  output       io_pixel_out_valid,
  output [7:0] io_pixel_out_bits
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
`endif // RANDOMIZE_REG_INIT
  reg [7:0] result; // @[BlurFilter.scala 30:25]
  reg  result_valid; // @[BlurFilter.scala 31:29]
  wire  all_inputs_valid = io_pixels_in_0_valid & io_pixels_in_1_valid & io_pixels_in_2_valid & io_pixels_in_3_valid &
    io_pixels_in_4_valid & io_pixels_in_5_valid & io_pixels_in_6_valid & io_pixels_in_7_valid & io_pixels_in_8_valid; // @[BlurFilter.scala 33:46]
  wire  _T = io_pixels_in_0_ready & io_pixels_in_0_valid; // @[Decoupled.scala 50:35]
  wire  _T_1 = io_pixels_in_1_ready & io_pixels_in_1_valid; // @[Decoupled.scala 50:35]
  wire  _T_2 = io_pixels_in_2_ready & io_pixels_in_2_valid; // @[Decoupled.scala 50:35]
  wire  _T_3 = io_pixels_in_3_ready & io_pixels_in_3_valid; // @[Decoupled.scala 50:35]
  wire  _T_4 = io_pixels_in_4_ready & io_pixels_in_4_valid; // @[Decoupled.scala 50:35]
  wire  _T_5 = io_pixels_in_5_ready & io_pixels_in_5_valid; // @[Decoupled.scala 50:35]
  wire  _T_6 = io_pixels_in_6_ready & io_pixels_in_6_valid; // @[Decoupled.scala 50:35]
  wire  _T_7 = io_pixels_in_7_ready & io_pixels_in_7_valid; // @[Decoupled.scala 50:35]
  wire  _T_8 = io_pixels_in_8_ready & io_pixels_in_8_valid; // @[Decoupled.scala 50:35]
  wire [15:0] t1 = {{8'd0}, io_pixels_in_1_bits}; // @[BlurFilter.scala 38:16 47:6]
  wire [17:0] _result_T = t1 * 2'h2; // @[BlurFilter.scala 61:16]
  wire [15:0] t0 = {{8'd0}, io_pixels_in_0_bits}; // @[BlurFilter.scala 37:16 46:6]
  wire [17:0] _GEN_3 = {{2'd0}, t0}; // @[BlurFilter.scala 61:11]
  wire [17:0] _result_T_2 = _GEN_3 + _result_T; // @[BlurFilter.scala 61:11]
  wire [15:0] t2 = {{8'd0}, io_pixels_in_2_bits}; // @[BlurFilter.scala 39:16 48:6]
  wire [17:0] _GEN_4 = {{2'd0}, t2}; // @[BlurFilter.scala 61:22]
  wire [17:0] _result_T_4 = _result_T_2 + _GEN_4; // @[BlurFilter.scala 61:22]
  wire [15:0] t4 = {{8'd0}, io_pixels_in_4_bits}; // @[BlurFilter.scala 41:16 50:6]
  wire [17:0] _result_T_5 = t4 * 2'h2; // @[BlurFilter.scala 62:18]
  wire [15:0] t3 = {{8'd0}, io_pixels_in_3_bits}; // @[BlurFilter.scala 40:16 49:6]
  wire [17:0] _GEN_5 = {{2'd0}, t3}; // @[BlurFilter.scala 62:13]
  wire [17:0] _result_T_7 = _GEN_5 + _result_T_5; // @[BlurFilter.scala 62:13]
  wire [15:0] t5 = {{8'd0}, io_pixels_in_5_bits}; // @[BlurFilter.scala 42:16 51:6]
  wire [17:0] _GEN_6 = {{2'd0}, t5}; // @[BlurFilter.scala 62:24]
  wire [17:0] _result_T_9 = _result_T_7 + _GEN_6; // @[BlurFilter.scala 62:24]
  wire [19:0] _result_T_10 = _result_T_9 * 2'h2; // @[BlurFilter.scala 62:30]
  wire [19:0] _GEN_7 = {{2'd0}, _result_T_4}; // @[BlurFilter.scala 61:28]
  wire [19:0] _result_T_12 = _GEN_7 + _result_T_10; // @[BlurFilter.scala 61:28]
  wire [15:0] t7 = {{8'd0}, io_pixels_in_7_bits}; // @[BlurFilter.scala 44:16 53:6]
  wire [17:0] _result_T_13 = t7 * 2'h2; // @[BlurFilter.scala 63:18]
  wire [15:0] t6 = {{8'd0}, io_pixels_in_6_bits}; // @[BlurFilter.scala 43:16 52:6]
  wire [17:0] _GEN_8 = {{2'd0}, t6}; // @[BlurFilter.scala 63:13]
  wire [17:0] _result_T_15 = _GEN_8 + _result_T_13; // @[BlurFilter.scala 63:13]
  wire [15:0] t8 = {{8'd0}, io_pixels_in_8_bits}; // @[BlurFilter.scala 45:16 54:6]
  wire [17:0] _GEN_9 = {{2'd0}, t8}; // @[BlurFilter.scala 63:24]
  wire [17:0] _result_T_17 = _result_T_15 + _GEN_9; // @[BlurFilter.scala 63:24]
  wire [19:0] _GEN_10 = {{2'd0}, _result_T_17}; // @[BlurFilter.scala 62:36]
  wire [19:0] _result_T_19 = _result_T_12 + _GEN_10; // @[BlurFilter.scala 62:36]
  wire [19:0] _result_T_20 = _result_T_19 / 5'h10; // @[BlurFilter.scala 64:7]
  wire [19:0] _GEN_0 = _T & _T_1 & _T_2 & _T_3 & _T_4 & _T_5 & _T_6 & _T_7 & _T_8 ? _result_T_20 : {{12'd0}, result}; // @[BlurFilter.scala 59:37 60:12 30:25]
  wire  _GEN_1 = _T & _T_1 & _T_2 & _T_3 & _T_4 & _T_5 & _T_6 & _T_7 & _T_8 | result_valid; // @[BlurFilter.scala 59:37 65:18 31:29]
  wire  _T_18 = io_pixel_out_ready & io_pixel_out_valid; // @[Decoupled.scala 50:35]
  assign io_pixels_in_0_ready = ~result_valid & all_inputs_valid; // @[BlurFilter.scala 34:58]
  assign io_pixels_in_1_ready = ~result_valid & all_inputs_valid; // @[BlurFilter.scala 34:58]
  assign io_pixels_in_2_ready = ~result_valid & all_inputs_valid; // @[BlurFilter.scala 34:58]
  assign io_pixels_in_3_ready = ~result_valid & all_inputs_valid; // @[BlurFilter.scala 34:58]
  assign io_pixels_in_4_ready = ~result_valid & all_inputs_valid; // @[BlurFilter.scala 34:58]
  assign io_pixels_in_5_ready = ~result_valid & all_inputs_valid; // @[BlurFilter.scala 34:58]
  assign io_pixels_in_6_ready = ~result_valid & all_inputs_valid; // @[BlurFilter.scala 34:58]
  assign io_pixels_in_7_ready = ~result_valid & all_inputs_valid; // @[BlurFilter.scala 34:58]
  assign io_pixels_in_8_ready = ~result_valid & all_inputs_valid; // @[BlurFilter.scala 34:58]
  assign io_pixel_out_valid = result_valid; // @[BlurFilter.scala 35:22]
  assign io_pixel_out_bits = result; // @[BlurFilter.scala 72:21]
  always @(posedge clock) begin
    result <= _GEN_0[7:0];
    if (reset) begin // @[BlurFilter.scala 31:29]
      result_valid <= 1'h0; // @[BlurFilter.scala 31:29]
    end else if (_T_18) begin // @[BlurFilter.scala 68:27]
      result_valid <= 1'h0; // @[BlurFilter.scala 69:18]
    end else begin
      result_valid <= _GEN_1;
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
  result = _RAND_0[7:0];
  _RAND_1 = {1{`RANDOM}};
  result_valid = _RAND_1[0:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
module Queue(
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
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
`endif // RANDOMIZE_REG_INIT
  reg [7:0] ram [0:511]; // @[Decoupled.scala 259:44]
  wire  ram_io_deq_bits_MPORT_en; // @[Decoupled.scala 259:44]
  wire [8:0] ram_io_deq_bits_MPORT_addr; // @[Decoupled.scala 259:44]
  wire [7:0] ram_io_deq_bits_MPORT_data; // @[Decoupled.scala 259:44]
  wire [7:0] ram_MPORT_data; // @[Decoupled.scala 259:44]
  wire [8:0] ram_MPORT_addr; // @[Decoupled.scala 259:44]
  wire  ram_MPORT_mask; // @[Decoupled.scala 259:44]
  wire  ram_MPORT_en; // @[Decoupled.scala 259:44]
  reg  ram_io_deq_bits_MPORT_en_pipe_0;
  reg [8:0] ram_io_deq_bits_MPORT_addr_pipe_0;
  reg [8:0] value; // @[Counter.scala 62:40]
  reg [8:0] value_1; // @[Counter.scala 62:40]
  reg  maybe_full; // @[Decoupled.scala 262:27]
  wire  ptr_match = value == value_1; // @[Decoupled.scala 263:33]
  wire  empty = ptr_match & ~maybe_full; // @[Decoupled.scala 264:25]
  wire  full = ptr_match & maybe_full; // @[Decoupled.scala 265:24]
  wire  do_enq = io_enq_ready & io_enq_valid; // @[Decoupled.scala 50:35]
  wire  do_deq = io_deq_ready & io_deq_valid; // @[Decoupled.scala 50:35]
  wire [8:0] _value_T_1 = value + 9'h1; // @[Counter.scala 78:24]
  wire [8:0] _value_T_3 = value_1 + 9'h1; // @[Counter.scala 78:24]
  wire [9:0] _deq_ptr_next_T_1 = 10'h200 - 10'h1; // @[Decoupled.scala 292:57]
  wire [9:0] _GEN_15 = {{1'd0}, value_1}; // @[Decoupled.scala 292:42]
  assign ram_io_deq_bits_MPORT_en = ram_io_deq_bits_MPORT_en_pipe_0;
  assign ram_io_deq_bits_MPORT_addr = ram_io_deq_bits_MPORT_addr_pipe_0;
  assign ram_io_deq_bits_MPORT_data = ram[ram_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 259:44]
  assign ram_MPORT_data = io_enq_bits;
  assign ram_MPORT_addr = value;
  assign ram_MPORT_mask = 1'h1;
  assign ram_MPORT_en = io_enq_ready & io_enq_valid;
  assign io_enq_ready = ~full; // @[Decoupled.scala 289:19]
  assign io_deq_valid = ~empty; // @[Decoupled.scala 288:19]
  assign io_deq_bits = ram_io_deq_bits_MPORT_data; // @[Decoupled.scala 294:17]
  always @(posedge clock) begin
    if (ram_MPORT_en & ram_MPORT_mask) begin
      ram[ram_MPORT_addr] <= ram_MPORT_data; // @[Decoupled.scala 259:44]
    end
    ram_io_deq_bits_MPORT_en_pipe_0 <= 1'h1;
    if (1'h1) begin
      if (do_deq) begin
        if (_GEN_15 == _deq_ptr_next_T_1) begin // @[Decoupled.scala 292:27]
          ram_io_deq_bits_MPORT_addr_pipe_0 <= 9'h0;
        end else begin
          ram_io_deq_bits_MPORT_addr_pipe_0 <= _value_T_3;
        end
      end else begin
        ram_io_deq_bits_MPORT_addr_pipe_0 <= value_1;
      end
    end
    if (reset) begin // @[Counter.scala 62:40]
      value <= 9'h0; // @[Counter.scala 62:40]
    end else if (do_enq) begin // @[Decoupled.scala 272:16]
      value <= _value_T_1; // @[Counter.scala 78:15]
    end
    if (reset) begin // @[Counter.scala 62:40]
      value_1 <= 9'h0; // @[Counter.scala 62:40]
    end else if (do_deq) begin // @[Decoupled.scala 276:16]
      value_1 <= _value_T_3; // @[Counter.scala 78:15]
    end
    if (reset) begin // @[Decoupled.scala 262:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 262:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 279:27]
      maybe_full <= do_enq; // @[Decoupled.scala 280:16]
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
  for (initvar = 0; initvar < 512; initvar = initvar+1)
    ram[initvar] = _RAND_0[7:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_1 = {1{`RANDOM}};
  ram_io_deq_bits_MPORT_en_pipe_0 = _RAND_1[0:0];
  _RAND_2 = {1{`RANDOM}};
  ram_io_deq_bits_MPORT_addr_pipe_0 = _RAND_2[8:0];
  _RAND_3 = {1{`RANDOM}};
  value = _RAND_3[8:0];
  _RAND_4 = {1{`RANDOM}};
  value_1 = _RAND_4[8:0];
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
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
`endif // RANDOMIZE_REG_INIT
  reg [7:0] ram [0:3]; // @[Decoupled.scala 259:44]
  wire  ram_io_deq_bits_MPORT_en; // @[Decoupled.scala 259:44]
  wire [1:0] ram_io_deq_bits_MPORT_addr; // @[Decoupled.scala 259:44]
  wire [7:0] ram_io_deq_bits_MPORT_data; // @[Decoupled.scala 259:44]
  wire [7:0] ram_MPORT_data; // @[Decoupled.scala 259:44]
  wire [1:0] ram_MPORT_addr; // @[Decoupled.scala 259:44]
  wire  ram_MPORT_mask; // @[Decoupled.scala 259:44]
  wire  ram_MPORT_en; // @[Decoupled.scala 259:44]
  reg  ram_io_deq_bits_MPORT_en_pipe_0;
  reg [1:0] ram_io_deq_bits_MPORT_addr_pipe_0;
  reg [1:0] value; // @[Counter.scala 62:40]
  reg [1:0] value_1; // @[Counter.scala 62:40]
  reg  maybe_full; // @[Decoupled.scala 262:27]
  wire  ptr_match = value == value_1; // @[Decoupled.scala 263:33]
  wire  empty = ptr_match & ~maybe_full; // @[Decoupled.scala 264:25]
  wire  full = ptr_match & maybe_full; // @[Decoupled.scala 265:24]
  wire  do_enq = io_enq_ready & io_enq_valid; // @[Decoupled.scala 50:35]
  wire  do_deq = io_deq_ready & io_deq_valid; // @[Decoupled.scala 50:35]
  wire [1:0] _value_T_1 = value + 2'h1; // @[Counter.scala 78:24]
  wire [1:0] _value_T_3 = value_1 + 2'h1; // @[Counter.scala 78:24]
  wire [2:0] _deq_ptr_next_T_1 = 3'h4 - 3'h1; // @[Decoupled.scala 292:57]
  wire [2:0] _GEN_15 = {{1'd0}, value_1}; // @[Decoupled.scala 292:42]
  assign ram_io_deq_bits_MPORT_en = ram_io_deq_bits_MPORT_en_pipe_0;
  assign ram_io_deq_bits_MPORT_addr = ram_io_deq_bits_MPORT_addr_pipe_0;
  assign ram_io_deq_bits_MPORT_data = ram[ram_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 259:44]
  assign ram_MPORT_data = io_enq_bits;
  assign ram_MPORT_addr = value;
  assign ram_MPORT_mask = 1'h1;
  assign ram_MPORT_en = io_enq_ready & io_enq_valid;
  assign io_enq_ready = ~full; // @[Decoupled.scala 289:19]
  assign io_deq_valid = ~empty; // @[Decoupled.scala 288:19]
  assign io_deq_bits = ram_io_deq_bits_MPORT_data; // @[Decoupled.scala 294:17]
  always @(posedge clock) begin
    if (ram_MPORT_en & ram_MPORT_mask) begin
      ram[ram_MPORT_addr] <= ram_MPORT_data; // @[Decoupled.scala 259:44]
    end
    ram_io_deq_bits_MPORT_en_pipe_0 <= 1'h1;
    if (1'h1) begin
      if (do_deq) begin
        if (_GEN_15 == _deq_ptr_next_T_1) begin // @[Decoupled.scala 292:27]
          ram_io_deq_bits_MPORT_addr_pipe_0 <= 2'h0;
        end else begin
          ram_io_deq_bits_MPORT_addr_pipe_0 <= _value_T_3;
        end
      end else begin
        ram_io_deq_bits_MPORT_addr_pipe_0 <= value_1;
      end
    end
    if (reset) begin // @[Counter.scala 62:40]
      value <= 2'h0; // @[Counter.scala 62:40]
    end else if (do_enq) begin // @[Decoupled.scala 272:16]
      value <= _value_T_1; // @[Counter.scala 78:15]
    end
    if (reset) begin // @[Counter.scala 62:40]
      value_1 <= 2'h0; // @[Counter.scala 62:40]
    end else if (do_deq) begin // @[Decoupled.scala 276:16]
      value_1 <= _value_T_3; // @[Counter.scala 78:15]
    end
    if (reset) begin // @[Decoupled.scala 262:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 262:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 279:27]
      maybe_full <= do_enq; // @[Decoupled.scala 280:16]
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
  for (initvar = 0; initvar < 4; initvar = initvar+1)
    ram[initvar] = _RAND_0[7:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_1 = {1{`RANDOM}};
  ram_io_deq_bits_MPORT_en_pipe_0 = _RAND_1[0:0];
  _RAND_2 = {1{`RANDOM}};
  ram_io_deq_bits_MPORT_addr_pipe_0 = _RAND_2[1:0];
  _RAND_3 = {1{`RANDOM}};
  value = _RAND_3[1:0];
  _RAND_4 = {1{`RANDOM}};
  value_1 = _RAND_4[1:0];
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
module Queue_26(
  input         clock,
  input         reset,
  output        io_enq_ready,
  input         io_enq_valid,
  input  [15:0] io_enq_bits,
  input         io_deq_ready,
  output        io_deq_valid,
  output [15:0] io_deq_bits
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
`endif // RANDOMIZE_REG_INIT
  reg [15:0] ram [0:3]; // @[Decoupled.scala 259:44]
  wire  ram_io_deq_bits_MPORT_en; // @[Decoupled.scala 259:44]
  wire [1:0] ram_io_deq_bits_MPORT_addr; // @[Decoupled.scala 259:44]
  wire [15:0] ram_io_deq_bits_MPORT_data; // @[Decoupled.scala 259:44]
  wire [15:0] ram_MPORT_data; // @[Decoupled.scala 259:44]
  wire [1:0] ram_MPORT_addr; // @[Decoupled.scala 259:44]
  wire  ram_MPORT_mask; // @[Decoupled.scala 259:44]
  wire  ram_MPORT_en; // @[Decoupled.scala 259:44]
  reg  ram_io_deq_bits_MPORT_en_pipe_0;
  reg [1:0] ram_io_deq_bits_MPORT_addr_pipe_0;
  reg [1:0] enq_ptr_value; // @[Counter.scala 62:40]
  reg [1:0] deq_ptr_value; // @[Counter.scala 62:40]
  reg  maybe_full; // @[Decoupled.scala 262:27]
  wire  ptr_match = enq_ptr_value == deq_ptr_value; // @[Decoupled.scala 263:33]
  wire  empty = ptr_match & ~maybe_full; // @[Decoupled.scala 264:25]
  wire  full = ptr_match & maybe_full; // @[Decoupled.scala 265:24]
  wire  do_enq = io_enq_ready & io_enq_valid; // @[Decoupled.scala 50:35]
  wire  do_deq = io_deq_ready & io_deq_valid; // @[Decoupled.scala 50:35]
  wire [1:0] _value_T_1 = enq_ptr_value + 2'h1; // @[Counter.scala 78:24]
  wire [1:0] _value_T_3 = deq_ptr_value + 2'h1; // @[Counter.scala 78:24]
  wire [2:0] _deq_ptr_next_T_1 = 3'h4 - 3'h1; // @[Decoupled.scala 292:57]
  wire [2:0] _GEN_15 = {{1'd0}, deq_ptr_value}; // @[Decoupled.scala 292:42]
  assign ram_io_deq_bits_MPORT_en = ram_io_deq_bits_MPORT_en_pipe_0;
  assign ram_io_deq_bits_MPORT_addr = ram_io_deq_bits_MPORT_addr_pipe_0;
  assign ram_io_deq_bits_MPORT_data = ram[ram_io_deq_bits_MPORT_addr]; // @[Decoupled.scala 259:44]
  assign ram_MPORT_data = io_enq_bits;
  assign ram_MPORT_addr = enq_ptr_value;
  assign ram_MPORT_mask = 1'h1;
  assign ram_MPORT_en = io_enq_ready & io_enq_valid;
  assign io_enq_ready = ~full; // @[Decoupled.scala 289:19]
  assign io_deq_valid = ~empty; // @[Decoupled.scala 288:19]
  assign io_deq_bits = ram_io_deq_bits_MPORT_data; // @[Decoupled.scala 294:17]
  always @(posedge clock) begin
    if (ram_MPORT_en & ram_MPORT_mask) begin
      ram[ram_MPORT_addr] <= ram_MPORT_data; // @[Decoupled.scala 259:44]
    end
    ram_io_deq_bits_MPORT_en_pipe_0 <= 1'h1;
    if (1'h1) begin
      if (do_deq) begin
        if (_GEN_15 == _deq_ptr_next_T_1) begin // @[Decoupled.scala 292:27]
          ram_io_deq_bits_MPORT_addr_pipe_0 <= 2'h0;
        end else begin
          ram_io_deq_bits_MPORT_addr_pipe_0 <= _value_T_3;
        end
      end else begin
        ram_io_deq_bits_MPORT_addr_pipe_0 <= deq_ptr_value;
      end
    end
    if (reset) begin // @[Counter.scala 62:40]
      enq_ptr_value <= 2'h0; // @[Counter.scala 62:40]
    end else if (do_enq) begin // @[Decoupled.scala 272:16]
      enq_ptr_value <= _value_T_1; // @[Counter.scala 78:15]
    end
    if (reset) begin // @[Counter.scala 62:40]
      deq_ptr_value <= 2'h0; // @[Counter.scala 62:40]
    end else if (do_deq) begin // @[Decoupled.scala 276:16]
      deq_ptr_value <= _value_T_3; // @[Counter.scala 78:15]
    end
    if (reset) begin // @[Decoupled.scala 262:27]
      maybe_full <= 1'h0; // @[Decoupled.scala 262:27]
    end else if (do_enq != do_deq) begin // @[Decoupled.scala 279:27]
      maybe_full <= do_enq; // @[Decoupled.scala 280:16]
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
  for (initvar = 0; initvar < 4; initvar = initvar+1)
    ram[initvar] = _RAND_0[15:0];
`endif // RANDOMIZE_MEM_INIT
`ifdef RANDOMIZE_REG_INIT
  _RAND_1 = {1{`RANDOM}};
  ram_io_deq_bits_MPORT_en_pipe_0 = _RAND_1[0:0];
  _RAND_2 = {1{`RANDOM}};
  ram_io_deq_bits_MPORT_addr_pipe_0 = _RAND_2[1:0];
  _RAND_3 = {1{`RANDOM}};
  enq_ptr_value = _RAND_3[1:0];
  _RAND_4 = {1{`RANDOM}};
  deq_ptr_value = _RAND_4[1:0];
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
module Gaussian3x3Kernel(
  input         clock,
  input         reset,
  output        io_in_pixel_ready,
  input         io_in_pixel_valid,
  input  [7:0]  io_in_pixel_bits,
  output        io_in_imgw_ready,
  input         io_in_imgw_valid,
  input  [15:0] io_in_imgw_bits,
  output        io_in_imgh_ready,
  input         io_in_imgh_valid,
  input  [15:0] io_in_imgh_bits,
  input         io_out_ready,
  output        io_out_valid,
  output [7:0]  io_out_bits
);
  wire  f0_clock; // @[BlurFilter.scala 356:38]
  wire  f0_reset; // @[BlurFilter.scala 356:38]
  wire  f0_io_pixel_in_pixel_ready; // @[BlurFilter.scala 356:38]
  wire  f0_io_pixel_in_pixel_valid; // @[BlurFilter.scala 356:38]
  wire [7:0] f0_io_pixel_in_pixel_bits; // @[BlurFilter.scala 356:38]
  wire  f0_io_pixel_in_imgw_ready; // @[BlurFilter.scala 356:38]
  wire  f0_io_pixel_in_imgw_valid; // @[BlurFilter.scala 356:38]
  wire [15:0] f0_io_pixel_in_imgw_bits; // @[BlurFilter.scala 356:38]
  wire  f0_io_pixel_in_imgh_ready; // @[BlurFilter.scala 356:38]
  wire  f0_io_pixel_in_imgh_valid; // @[BlurFilter.scala 356:38]
  wire [15:0] f0_io_pixel_in_imgh_bits; // @[BlurFilter.scala 356:38]
  wire  f0_io_pixel_out_ready; // @[BlurFilter.scala 356:38]
  wire  f0_io_pixel_out_valid; // @[BlurFilter.scala 356:38]
  wire [7:0] f0_io_pixel_out_bits; // @[BlurFilter.scala 356:38]
  wire  f1_clock; // @[BlurFilter.scala 356:38]
  wire  f1_reset; // @[BlurFilter.scala 356:38]
  wire  f1_io_pixel_in_pixel_ready; // @[BlurFilter.scala 356:38]
  wire  f1_io_pixel_in_pixel_valid; // @[BlurFilter.scala 356:38]
  wire [7:0] f1_io_pixel_in_pixel_bits; // @[BlurFilter.scala 356:38]
  wire  f1_io_pixel_in_imgw_ready; // @[BlurFilter.scala 356:38]
  wire  f1_io_pixel_in_imgw_valid; // @[BlurFilter.scala 356:38]
  wire [15:0] f1_io_pixel_in_imgw_bits; // @[BlurFilter.scala 356:38]
  wire  f1_io_pixel_in_imgh_ready; // @[BlurFilter.scala 356:38]
  wire  f1_io_pixel_in_imgh_valid; // @[BlurFilter.scala 356:38]
  wire [15:0] f1_io_pixel_in_imgh_bits; // @[BlurFilter.scala 356:38]
  wire  f1_io_pixel_out_ready; // @[BlurFilter.scala 356:38]
  wire  f1_io_pixel_out_valid; // @[BlurFilter.scala 356:38]
  wire [7:0] f1_io_pixel_out_bits; // @[BlurFilter.scala 356:38]
  wire  f2_clock; // @[BlurFilter.scala 356:38]
  wire  f2_reset; // @[BlurFilter.scala 356:38]
  wire  f2_io_pixel_in_pixel_ready; // @[BlurFilter.scala 356:38]
  wire  f2_io_pixel_in_pixel_valid; // @[BlurFilter.scala 356:38]
  wire [7:0] f2_io_pixel_in_pixel_bits; // @[BlurFilter.scala 356:38]
  wire  f2_io_pixel_in_imgw_ready; // @[BlurFilter.scala 356:38]
  wire  f2_io_pixel_in_imgw_valid; // @[BlurFilter.scala 356:38]
  wire [15:0] f2_io_pixel_in_imgw_bits; // @[BlurFilter.scala 356:38]
  wire  f2_io_pixel_in_imgh_ready; // @[BlurFilter.scala 356:38]
  wire  f2_io_pixel_in_imgh_valid; // @[BlurFilter.scala 356:38]
  wire [15:0] f2_io_pixel_in_imgh_bits; // @[BlurFilter.scala 356:38]
  wire  f2_io_pixel_out_ready; // @[BlurFilter.scala 356:38]
  wire  f2_io_pixel_out_valid; // @[BlurFilter.scala 356:38]
  wire [7:0] f2_io_pixel_out_bits; // @[BlurFilter.scala 356:38]
  wire  f3_clock; // @[BlurFilter.scala 356:38]
  wire  f3_reset; // @[BlurFilter.scala 356:38]
  wire  f3_io_pixel_in_pixel_ready; // @[BlurFilter.scala 356:38]
  wire  f3_io_pixel_in_pixel_valid; // @[BlurFilter.scala 356:38]
  wire [7:0] f3_io_pixel_in_pixel_bits; // @[BlurFilter.scala 356:38]
  wire  f3_io_pixel_in_imgw_ready; // @[BlurFilter.scala 356:38]
  wire  f3_io_pixel_in_imgw_valid; // @[BlurFilter.scala 356:38]
  wire [15:0] f3_io_pixel_in_imgw_bits; // @[BlurFilter.scala 356:38]
  wire  f3_io_pixel_in_imgh_ready; // @[BlurFilter.scala 356:38]
  wire  f3_io_pixel_in_imgh_valid; // @[BlurFilter.scala 356:38]
  wire [15:0] f3_io_pixel_in_imgh_bits; // @[BlurFilter.scala 356:38]
  wire  f3_io_pixel_out_ready; // @[BlurFilter.scala 356:38]
  wire  f3_io_pixel_out_valid; // @[BlurFilter.scala 356:38]
  wire [7:0] f3_io_pixel_out_bits; // @[BlurFilter.scala 356:38]
  wire  f4_clock; // @[BlurFilter.scala 356:38]
  wire  f4_reset; // @[BlurFilter.scala 356:38]
  wire  f4_io_pixel_in_pixel_ready; // @[BlurFilter.scala 356:38]
  wire  f4_io_pixel_in_pixel_valid; // @[BlurFilter.scala 356:38]
  wire [7:0] f4_io_pixel_in_pixel_bits; // @[BlurFilter.scala 356:38]
  wire  f4_io_pixel_in_imgw_ready; // @[BlurFilter.scala 356:38]
  wire  f4_io_pixel_in_imgw_valid; // @[BlurFilter.scala 356:38]
  wire [15:0] f4_io_pixel_in_imgw_bits; // @[BlurFilter.scala 356:38]
  wire  f4_io_pixel_in_imgh_ready; // @[BlurFilter.scala 356:38]
  wire  f4_io_pixel_in_imgh_valid; // @[BlurFilter.scala 356:38]
  wire [15:0] f4_io_pixel_in_imgh_bits; // @[BlurFilter.scala 356:38]
  wire  f4_io_pixel_out_ready; // @[BlurFilter.scala 356:38]
  wire  f4_io_pixel_out_valid; // @[BlurFilter.scala 356:38]
  wire [7:0] f4_io_pixel_out_bits; // @[BlurFilter.scala 356:38]
  wire  f5_clock; // @[BlurFilter.scala 356:38]
  wire  f5_reset; // @[BlurFilter.scala 356:38]
  wire  f5_io_pixel_in_pixel_ready; // @[BlurFilter.scala 356:38]
  wire  f5_io_pixel_in_pixel_valid; // @[BlurFilter.scala 356:38]
  wire [7:0] f5_io_pixel_in_pixel_bits; // @[BlurFilter.scala 356:38]
  wire  f5_io_pixel_in_imgw_ready; // @[BlurFilter.scala 356:38]
  wire  f5_io_pixel_in_imgw_valid; // @[BlurFilter.scala 356:38]
  wire [15:0] f5_io_pixel_in_imgw_bits; // @[BlurFilter.scala 356:38]
  wire  f5_io_pixel_in_imgh_ready; // @[BlurFilter.scala 356:38]
  wire  f5_io_pixel_in_imgh_valid; // @[BlurFilter.scala 356:38]
  wire [15:0] f5_io_pixel_in_imgh_bits; // @[BlurFilter.scala 356:38]
  wire  f5_io_pixel_out_ready; // @[BlurFilter.scala 356:38]
  wire  f5_io_pixel_out_valid; // @[BlurFilter.scala 356:38]
  wire [7:0] f5_io_pixel_out_bits; // @[BlurFilter.scala 356:38]
  wire  f6_clock; // @[BlurFilter.scala 356:38]
  wire  f6_reset; // @[BlurFilter.scala 356:38]
  wire  f6_io_pixel_in_pixel_ready; // @[BlurFilter.scala 356:38]
  wire  f6_io_pixel_in_pixel_valid; // @[BlurFilter.scala 356:38]
  wire [7:0] f6_io_pixel_in_pixel_bits; // @[BlurFilter.scala 356:38]
  wire  f6_io_pixel_in_imgw_ready; // @[BlurFilter.scala 356:38]
  wire  f6_io_pixel_in_imgw_valid; // @[BlurFilter.scala 356:38]
  wire [15:0] f6_io_pixel_in_imgw_bits; // @[BlurFilter.scala 356:38]
  wire  f6_io_pixel_in_imgh_ready; // @[BlurFilter.scala 356:38]
  wire  f6_io_pixel_in_imgh_valid; // @[BlurFilter.scala 356:38]
  wire [15:0] f6_io_pixel_in_imgh_bits; // @[BlurFilter.scala 356:38]
  wire  f6_io_pixel_out_ready; // @[BlurFilter.scala 356:38]
  wire  f6_io_pixel_out_valid; // @[BlurFilter.scala 356:38]
  wire [7:0] f6_io_pixel_out_bits; // @[BlurFilter.scala 356:38]
  wire  f7_clock; // @[BlurFilter.scala 356:38]
  wire  f7_reset; // @[BlurFilter.scala 356:38]
  wire  f7_io_pixel_in_pixel_ready; // @[BlurFilter.scala 356:38]
  wire  f7_io_pixel_in_pixel_valid; // @[BlurFilter.scala 356:38]
  wire [7:0] f7_io_pixel_in_pixel_bits; // @[BlurFilter.scala 356:38]
  wire  f7_io_pixel_in_imgw_ready; // @[BlurFilter.scala 356:38]
  wire  f7_io_pixel_in_imgw_valid; // @[BlurFilter.scala 356:38]
  wire [15:0] f7_io_pixel_in_imgw_bits; // @[BlurFilter.scala 356:38]
  wire  f7_io_pixel_in_imgh_ready; // @[BlurFilter.scala 356:38]
  wire  f7_io_pixel_in_imgh_valid; // @[BlurFilter.scala 356:38]
  wire [15:0] f7_io_pixel_in_imgh_bits; // @[BlurFilter.scala 356:38]
  wire  f7_io_pixel_out_ready; // @[BlurFilter.scala 356:38]
  wire  f7_io_pixel_out_valid; // @[BlurFilter.scala 356:38]
  wire [7:0] f7_io_pixel_out_bits; // @[BlurFilter.scala 356:38]
  wire  f8_clock; // @[BlurFilter.scala 356:38]
  wire  f8_reset; // @[BlurFilter.scala 356:38]
  wire  f8_io_pixel_in_pixel_ready; // @[BlurFilter.scala 356:38]
  wire  f8_io_pixel_in_pixel_valid; // @[BlurFilter.scala 356:38]
  wire [7:0] f8_io_pixel_in_pixel_bits; // @[BlurFilter.scala 356:38]
  wire  f8_io_pixel_in_imgw_ready; // @[BlurFilter.scala 356:38]
  wire  f8_io_pixel_in_imgw_valid; // @[BlurFilter.scala 356:38]
  wire [15:0] f8_io_pixel_in_imgw_bits; // @[BlurFilter.scala 356:38]
  wire  f8_io_pixel_in_imgh_ready; // @[BlurFilter.scala 356:38]
  wire  f8_io_pixel_in_imgh_valid; // @[BlurFilter.scala 356:38]
  wire [15:0] f8_io_pixel_in_imgh_bits; // @[BlurFilter.scala 356:38]
  wire  f8_io_pixel_out_ready; // @[BlurFilter.scala 356:38]
  wire  f8_io_pixel_out_valid; // @[BlurFilter.scala 356:38]
  wire [7:0] f8_io_pixel_out_bits; // @[BlurFilter.scala 356:38]
  wire  s0_io_pixel_in_ready; // @[BlurFilter.scala 368:18]
  wire  s0_io_pixel_in_valid; // @[BlurFilter.scala 368:18]
  wire [7:0] s0_io_pixel_in_bits; // @[BlurFilter.scala 368:18]
  wire  s0_io_pixel_left_ready; // @[BlurFilter.scala 368:18]
  wire  s0_io_pixel_left_valid; // @[BlurFilter.scala 368:18]
  wire [7:0] s0_io_pixel_left_bits; // @[BlurFilter.scala 368:18]
  wire  s0_io_pixel_right_ready; // @[BlurFilter.scala 368:18]
  wire  s0_io_pixel_right_valid; // @[BlurFilter.scala 368:18]
  wire [7:0] s0_io_pixel_right_bits; // @[BlurFilter.scala 368:18]
  wire  s1_io_pixel_in_ready; // @[BlurFilter.scala 369:18]
  wire  s1_io_pixel_in_valid; // @[BlurFilter.scala 369:18]
  wire [7:0] s1_io_pixel_in_bits; // @[BlurFilter.scala 369:18]
  wire  s1_io_pixel_left_ready; // @[BlurFilter.scala 369:18]
  wire  s1_io_pixel_left_valid; // @[BlurFilter.scala 369:18]
  wire [7:0] s1_io_pixel_left_bits; // @[BlurFilter.scala 369:18]
  wire  s1_io_pixel_right_ready; // @[BlurFilter.scala 369:18]
  wire  s1_io_pixel_right_valid; // @[BlurFilter.scala 369:18]
  wire [7:0] s1_io_pixel_right_bits; // @[BlurFilter.scala 369:18]
  wire  s2_io_pixel_in_ready; // @[BlurFilter.scala 370:18]
  wire  s2_io_pixel_in_valid; // @[BlurFilter.scala 370:18]
  wire [7:0] s2_io_pixel_in_bits; // @[BlurFilter.scala 370:18]
  wire  s2_io_pixel_left_ready; // @[BlurFilter.scala 370:18]
  wire  s2_io_pixel_left_valid; // @[BlurFilter.scala 370:18]
  wire [7:0] s2_io_pixel_left_bits; // @[BlurFilter.scala 370:18]
  wire  s2_io_pixel_right_ready; // @[BlurFilter.scala 370:18]
  wire  s2_io_pixel_right_valid; // @[BlurFilter.scala 370:18]
  wire [7:0] s2_io_pixel_right_bits; // @[BlurFilter.scala 370:18]
  wire  s3_io_pixel_in_ready; // @[BlurFilter.scala 371:18]
  wire  s3_io_pixel_in_valid; // @[BlurFilter.scala 371:18]
  wire [7:0] s3_io_pixel_in_bits; // @[BlurFilter.scala 371:18]
  wire  s3_io_pixel_left_ready; // @[BlurFilter.scala 371:18]
  wire  s3_io_pixel_left_valid; // @[BlurFilter.scala 371:18]
  wire [7:0] s3_io_pixel_left_bits; // @[BlurFilter.scala 371:18]
  wire  s3_io_pixel_right_ready; // @[BlurFilter.scala 371:18]
  wire  s3_io_pixel_right_valid; // @[BlurFilter.scala 371:18]
  wire [7:0] s3_io_pixel_right_bits; // @[BlurFilter.scala 371:18]
  wire  s4_io_pixel_in_ready; // @[BlurFilter.scala 372:18]
  wire  s4_io_pixel_in_valid; // @[BlurFilter.scala 372:18]
  wire [7:0] s4_io_pixel_in_bits; // @[BlurFilter.scala 372:18]
  wire  s4_io_pixel_left_ready; // @[BlurFilter.scala 372:18]
  wire  s4_io_pixel_left_valid; // @[BlurFilter.scala 372:18]
  wire [7:0] s4_io_pixel_left_bits; // @[BlurFilter.scala 372:18]
  wire  s4_io_pixel_right_ready; // @[BlurFilter.scala 372:18]
  wire  s4_io_pixel_right_valid; // @[BlurFilter.scala 372:18]
  wire [7:0] s4_io_pixel_right_bits; // @[BlurFilter.scala 372:18]
  wire  s5_io_pixel_in_ready; // @[BlurFilter.scala 373:18]
  wire  s5_io_pixel_in_valid; // @[BlurFilter.scala 373:18]
  wire [7:0] s5_io_pixel_in_bits; // @[BlurFilter.scala 373:18]
  wire  s5_io_pixel_left_ready; // @[BlurFilter.scala 373:18]
  wire  s5_io_pixel_left_valid; // @[BlurFilter.scala 373:18]
  wire [7:0] s5_io_pixel_left_bits; // @[BlurFilter.scala 373:18]
  wire  s5_io_pixel_right_ready; // @[BlurFilter.scala 373:18]
  wire  s5_io_pixel_right_valid; // @[BlurFilter.scala 373:18]
  wire [7:0] s5_io_pixel_right_bits; // @[BlurFilter.scala 373:18]
  wire  s6_io_pixel_in_ready; // @[BlurFilter.scala 374:18]
  wire  s6_io_pixel_in_valid; // @[BlurFilter.scala 374:18]
  wire [7:0] s6_io_pixel_in_bits; // @[BlurFilter.scala 374:18]
  wire  s6_io_pixel_left_ready; // @[BlurFilter.scala 374:18]
  wire  s6_io_pixel_left_valid; // @[BlurFilter.scala 374:18]
  wire [7:0] s6_io_pixel_left_bits; // @[BlurFilter.scala 374:18]
  wire  s6_io_pixel_right_ready; // @[BlurFilter.scala 374:18]
  wire  s6_io_pixel_right_valid; // @[BlurFilter.scala 374:18]
  wire [7:0] s6_io_pixel_right_bits; // @[BlurFilter.scala 374:18]
  wire  s7_io_pixel_in_ready; // @[BlurFilter.scala 375:18]
  wire  s7_io_pixel_in_valid; // @[BlurFilter.scala 375:18]
  wire [7:0] s7_io_pixel_in_bits; // @[BlurFilter.scala 375:18]
  wire  s7_io_pixel_left_ready; // @[BlurFilter.scala 375:18]
  wire  s7_io_pixel_left_valid; // @[BlurFilter.scala 375:18]
  wire [7:0] s7_io_pixel_left_bits; // @[BlurFilter.scala 375:18]
  wire  s7_io_pixel_right_ready; // @[BlurFilter.scala 375:18]
  wire  s7_io_pixel_right_valid; // @[BlurFilter.scala 375:18]
  wire [7:0] s7_io_pixel_right_bits; // @[BlurFilter.scala 375:18]
  wire  compute_clock; // @[BlurFilter.scala 377:23]
  wire  compute_reset; // @[BlurFilter.scala 377:23]
  wire  compute_io_pixels_in_0_ready; // @[BlurFilter.scala 377:23]
  wire  compute_io_pixels_in_0_valid; // @[BlurFilter.scala 377:23]
  wire [7:0] compute_io_pixels_in_0_bits; // @[BlurFilter.scala 377:23]
  wire  compute_io_pixels_in_1_ready; // @[BlurFilter.scala 377:23]
  wire  compute_io_pixels_in_1_valid; // @[BlurFilter.scala 377:23]
  wire [7:0] compute_io_pixels_in_1_bits; // @[BlurFilter.scala 377:23]
  wire  compute_io_pixels_in_2_ready; // @[BlurFilter.scala 377:23]
  wire  compute_io_pixels_in_2_valid; // @[BlurFilter.scala 377:23]
  wire [7:0] compute_io_pixels_in_2_bits; // @[BlurFilter.scala 377:23]
  wire  compute_io_pixels_in_3_ready; // @[BlurFilter.scala 377:23]
  wire  compute_io_pixels_in_3_valid; // @[BlurFilter.scala 377:23]
  wire [7:0] compute_io_pixels_in_3_bits; // @[BlurFilter.scala 377:23]
  wire  compute_io_pixels_in_4_ready; // @[BlurFilter.scala 377:23]
  wire  compute_io_pixels_in_4_valid; // @[BlurFilter.scala 377:23]
  wire [7:0] compute_io_pixels_in_4_bits; // @[BlurFilter.scala 377:23]
  wire  compute_io_pixels_in_5_ready; // @[BlurFilter.scala 377:23]
  wire  compute_io_pixels_in_5_valid; // @[BlurFilter.scala 377:23]
  wire [7:0] compute_io_pixels_in_5_bits; // @[BlurFilter.scala 377:23]
  wire  compute_io_pixels_in_6_ready; // @[BlurFilter.scala 377:23]
  wire  compute_io_pixels_in_6_valid; // @[BlurFilter.scala 377:23]
  wire [7:0] compute_io_pixels_in_6_bits; // @[BlurFilter.scala 377:23]
  wire  compute_io_pixels_in_7_ready; // @[BlurFilter.scala 377:23]
  wire  compute_io_pixels_in_7_valid; // @[BlurFilter.scala 377:23]
  wire [7:0] compute_io_pixels_in_7_bits; // @[BlurFilter.scala 377:23]
  wire  compute_io_pixels_in_8_ready; // @[BlurFilter.scala 377:23]
  wire  compute_io_pixels_in_8_valid; // @[BlurFilter.scala 377:23]
  wire [7:0] compute_io_pixels_in_8_bits; // @[BlurFilter.scala 377:23]
  wire  compute_io_pixel_out_ready; // @[BlurFilter.scala 377:23]
  wire  compute_io_pixel_out_valid; // @[BlurFilter.scala 377:23]
  wire [7:0] compute_io_pixel_out_bits; // @[BlurFilter.scala 377:23]
  wire  q_clock; // @[Decoupled.scala 361:21]
  wire  q_reset; // @[Decoupled.scala 361:21]
  wire  q_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_1_clock; // @[Decoupled.scala 361:21]
  wire  q_1_reset; // @[Decoupled.scala 361:21]
  wire  q_1_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_1_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_1_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_1_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_1_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_1_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_2_clock; // @[Decoupled.scala 361:21]
  wire  q_2_reset; // @[Decoupled.scala 361:21]
  wire  q_2_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_2_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_2_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_2_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_2_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_2_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_3_clock; // @[Decoupled.scala 361:21]
  wire  q_3_reset; // @[Decoupled.scala 361:21]
  wire  q_3_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_3_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_3_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_3_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_3_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_3_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_4_clock; // @[Decoupled.scala 361:21]
  wire  q_4_reset; // @[Decoupled.scala 361:21]
  wire  q_4_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_4_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_4_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_4_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_4_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_4_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_5_clock; // @[Decoupled.scala 361:21]
  wire  q_5_reset; // @[Decoupled.scala 361:21]
  wire  q_5_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_5_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_5_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_5_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_5_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_5_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_6_clock; // @[Decoupled.scala 361:21]
  wire  q_6_reset; // @[Decoupled.scala 361:21]
  wire  q_6_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_6_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_6_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_6_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_6_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_6_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_7_clock; // @[Decoupled.scala 361:21]
  wire  q_7_reset; // @[Decoupled.scala 361:21]
  wire  q_7_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_7_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_7_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_7_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_7_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_7_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_8_clock; // @[Decoupled.scala 361:21]
  wire  q_8_reset; // @[Decoupled.scala 361:21]
  wire  q_8_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_8_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_8_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_8_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_8_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_8_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_9_clock; // @[Decoupled.scala 361:21]
  wire  q_9_reset; // @[Decoupled.scala 361:21]
  wire  q_9_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_9_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_9_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_9_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_9_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_9_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_10_clock; // @[Decoupled.scala 361:21]
  wire  q_10_reset; // @[Decoupled.scala 361:21]
  wire  q_10_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_10_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_10_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_10_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_10_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_10_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_11_clock; // @[Decoupled.scala 361:21]
  wire  q_11_reset; // @[Decoupled.scala 361:21]
  wire  q_11_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_11_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_11_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_11_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_11_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_11_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_12_clock; // @[Decoupled.scala 361:21]
  wire  q_12_reset; // @[Decoupled.scala 361:21]
  wire  q_12_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_12_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_12_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_12_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_12_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_12_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_13_clock; // @[Decoupled.scala 361:21]
  wire  q_13_reset; // @[Decoupled.scala 361:21]
  wire  q_13_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_13_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_13_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_13_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_13_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_13_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_14_clock; // @[Decoupled.scala 361:21]
  wire  q_14_reset; // @[Decoupled.scala 361:21]
  wire  q_14_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_14_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_14_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_14_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_14_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_14_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_15_clock; // @[Decoupled.scala 361:21]
  wire  q_15_reset; // @[Decoupled.scala 361:21]
  wire  q_15_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_15_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_15_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_15_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_15_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_15_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_16_clock; // @[Decoupled.scala 361:21]
  wire  q_16_reset; // @[Decoupled.scala 361:21]
  wire  q_16_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_16_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_16_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_16_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_16_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_16_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_17_clock; // @[Decoupled.scala 361:21]
  wire  q_17_reset; // @[Decoupled.scala 361:21]
  wire  q_17_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_17_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_17_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_17_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_17_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_17_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_18_clock; // @[Decoupled.scala 361:21]
  wire  q_18_reset; // @[Decoupled.scala 361:21]
  wire  q_18_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_18_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_18_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_18_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_18_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_18_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_19_clock; // @[Decoupled.scala 361:21]
  wire  q_19_reset; // @[Decoupled.scala 361:21]
  wire  q_19_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_19_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_19_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_19_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_19_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_19_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_20_clock; // @[Decoupled.scala 361:21]
  wire  q_20_reset; // @[Decoupled.scala 361:21]
  wire  q_20_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_20_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_20_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_20_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_20_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_20_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_21_clock; // @[Decoupled.scala 361:21]
  wire  q_21_reset; // @[Decoupled.scala 361:21]
  wire  q_21_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_21_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_21_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_21_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_21_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_21_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_22_clock; // @[Decoupled.scala 361:21]
  wire  q_22_reset; // @[Decoupled.scala 361:21]
  wire  q_22_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_22_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_22_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_22_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_22_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_22_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_23_clock; // @[Decoupled.scala 361:21]
  wire  q_23_reset; // @[Decoupled.scala 361:21]
  wire  q_23_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_23_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_23_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_23_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_23_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_23_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_24_clock; // @[Decoupled.scala 361:21]
  wire  q_24_reset; // @[Decoupled.scala 361:21]
  wire  q_24_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_24_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_24_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_24_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_24_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_24_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  q_25_clock; // @[Decoupled.scala 361:21]
  wire  q_25_reset; // @[Decoupled.scala 361:21]
  wire  q_25_io_enq_ready; // @[Decoupled.scala 361:21]
  wire  q_25_io_enq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_25_io_enq_bits; // @[Decoupled.scala 361:21]
  wire  q_25_io_deq_ready; // @[Decoupled.scala 361:21]
  wire  q_25_io_deq_valid; // @[Decoupled.scala 361:21]
  wire [7:0] q_25_io_deq_bits; // @[Decoupled.scala 361:21]
  wire  fanout_clock; // @[BlurFilter.scala 320:36]
  wire  fanout_reset; // @[BlurFilter.scala 320:36]
  wire  fanout_io_enq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_io_enq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_io_enq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_io_deq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_io_deq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_io_deq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_1_clock; // @[BlurFilter.scala 320:36]
  wire  fanout_1_reset; // @[BlurFilter.scala 320:36]
  wire  fanout_1_io_enq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_1_io_enq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_1_io_enq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_1_io_deq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_1_io_deq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_1_io_deq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_2_clock; // @[BlurFilter.scala 320:36]
  wire  fanout_2_reset; // @[BlurFilter.scala 320:36]
  wire  fanout_2_io_enq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_2_io_enq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_2_io_enq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_2_io_deq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_2_io_deq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_2_io_deq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_3_clock; // @[BlurFilter.scala 320:36]
  wire  fanout_3_reset; // @[BlurFilter.scala 320:36]
  wire  fanout_3_io_enq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_3_io_enq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_3_io_enq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_3_io_deq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_3_io_deq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_3_io_deq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_4_clock; // @[BlurFilter.scala 320:36]
  wire  fanout_4_reset; // @[BlurFilter.scala 320:36]
  wire  fanout_4_io_enq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_4_io_enq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_4_io_enq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_4_io_deq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_4_io_deq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_4_io_deq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_5_clock; // @[BlurFilter.scala 320:36]
  wire  fanout_5_reset; // @[BlurFilter.scala 320:36]
  wire  fanout_5_io_enq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_5_io_enq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_5_io_enq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_5_io_deq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_5_io_deq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_5_io_deq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_6_clock; // @[BlurFilter.scala 320:36]
  wire  fanout_6_reset; // @[BlurFilter.scala 320:36]
  wire  fanout_6_io_enq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_6_io_enq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_6_io_enq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_6_io_deq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_6_io_deq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_6_io_deq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_7_clock; // @[BlurFilter.scala 320:36]
  wire  fanout_7_reset; // @[BlurFilter.scala 320:36]
  wire  fanout_7_io_enq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_7_io_enq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_7_io_enq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_7_io_deq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_7_io_deq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_7_io_deq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_8_clock; // @[BlurFilter.scala 320:36]
  wire  fanout_8_reset; // @[BlurFilter.scala 320:36]
  wire  fanout_8_io_enq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_8_io_enq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_8_io_enq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_8_io_deq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_8_io_deq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_8_io_deq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_9_clock; // @[BlurFilter.scala 320:36]
  wire  fanout_9_reset; // @[BlurFilter.scala 320:36]
  wire  fanout_9_io_enq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_9_io_enq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_9_io_enq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_9_io_deq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_9_io_deq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_9_io_deq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_10_clock; // @[BlurFilter.scala 320:36]
  wire  fanout_10_reset; // @[BlurFilter.scala 320:36]
  wire  fanout_10_io_enq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_10_io_enq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_10_io_enq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_10_io_deq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_10_io_deq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_10_io_deq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_11_clock; // @[BlurFilter.scala 320:36]
  wire  fanout_11_reset; // @[BlurFilter.scala 320:36]
  wire  fanout_11_io_enq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_11_io_enq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_11_io_enq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_11_io_deq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_11_io_deq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_11_io_deq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_12_clock; // @[BlurFilter.scala 320:36]
  wire  fanout_12_reset; // @[BlurFilter.scala 320:36]
  wire  fanout_12_io_enq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_12_io_enq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_12_io_enq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_12_io_deq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_12_io_deq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_12_io_deq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_13_clock; // @[BlurFilter.scala 320:36]
  wire  fanout_13_reset; // @[BlurFilter.scala 320:36]
  wire  fanout_13_io_enq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_13_io_enq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_13_io_enq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_13_io_deq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_13_io_deq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_13_io_deq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_14_clock; // @[BlurFilter.scala 320:36]
  wire  fanout_14_reset; // @[BlurFilter.scala 320:36]
  wire  fanout_14_io_enq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_14_io_enq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_14_io_enq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_14_io_deq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_14_io_deq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_14_io_deq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_15_clock; // @[BlurFilter.scala 320:36]
  wire  fanout_15_reset; // @[BlurFilter.scala 320:36]
  wire  fanout_15_io_enq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_15_io_enq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_15_io_enq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_15_io_deq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_15_io_deq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_15_io_deq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_16_clock; // @[BlurFilter.scala 320:36]
  wire  fanout_16_reset; // @[BlurFilter.scala 320:36]
  wire  fanout_16_io_enq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_16_io_enq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_16_io_enq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_16_io_deq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_16_io_deq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_16_io_deq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_17_clock; // @[BlurFilter.scala 320:36]
  wire  fanout_17_reset; // @[BlurFilter.scala 320:36]
  wire  fanout_17_io_enq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_17_io_enq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_17_io_enq_bits; // @[BlurFilter.scala 320:36]
  wire  fanout_17_io_deq_ready; // @[BlurFilter.scala 320:36]
  wire  fanout_17_io_deq_valid; // @[BlurFilter.scala 320:36]
  wire [15:0] fanout_17_io_deq_bits; // @[BlurFilter.scala 320:36]
  wire  ready__0 = fanout_io_enq_ready; // @[BlurFilter.scala 321:21 324:9]
  wire  ready__1 = fanout_1_io_enq_ready; // @[BlurFilter.scala 321:21 324:9]
  wire  ready__2 = fanout_2_io_enq_ready; // @[BlurFilter.scala 321:21 324:9]
  wire  ready__3 = fanout_3_io_enq_ready; // @[BlurFilter.scala 321:21 324:9]
  wire  ready__4 = fanout_4_io_enq_ready; // @[BlurFilter.scala 321:21 324:9]
  wire  ready__5 = fanout_5_io_enq_ready; // @[BlurFilter.scala 321:21 324:9]
  wire  ready__6 = fanout_6_io_enq_ready; // @[BlurFilter.scala 321:21 324:9]
  wire  ready__7 = fanout_7_io_enq_ready; // @[BlurFilter.scala 321:21 324:9]
  wire  ready__8 = fanout_8_io_enq_ready; // @[BlurFilter.scala 321:21 324:9]
  wire  ready_1_0 = fanout_9_io_enq_ready; // @[BlurFilter.scala 321:21 324:9]
  wire  ready_1_1 = fanout_10_io_enq_ready; // @[BlurFilter.scala 321:21 324:9]
  wire  ready_1_2 = fanout_11_io_enq_ready; // @[BlurFilter.scala 321:21 324:9]
  wire  ready_1_3 = fanout_12_io_enq_ready; // @[BlurFilter.scala 321:21 324:9]
  wire  ready_1_4 = fanout_13_io_enq_ready; // @[BlurFilter.scala 321:21 324:9]
  wire  ready_1_5 = fanout_14_io_enq_ready; // @[BlurFilter.scala 321:21 324:9]
  wire  ready_1_6 = fanout_15_io_enq_ready; // @[BlurFilter.scala 321:21 324:9]
  wire  ready_1_7 = fanout_16_io_enq_ready; // @[BlurFilter.scala 321:21 324:9]
  wire  ready_1_8 = fanout_17_io_enq_ready; // @[BlurFilter.scala 321:21 324:9]
  DynamicFilter f0 ( // @[BlurFilter.scala 356:38]
    .clock(f0_clock),
    .reset(f0_reset),
    .io_pixel_in_pixel_ready(f0_io_pixel_in_pixel_ready),
    .io_pixel_in_pixel_valid(f0_io_pixel_in_pixel_valid),
    .io_pixel_in_pixel_bits(f0_io_pixel_in_pixel_bits),
    .io_pixel_in_imgw_ready(f0_io_pixel_in_imgw_ready),
    .io_pixel_in_imgw_valid(f0_io_pixel_in_imgw_valid),
    .io_pixel_in_imgw_bits(f0_io_pixel_in_imgw_bits),
    .io_pixel_in_imgh_ready(f0_io_pixel_in_imgh_ready),
    .io_pixel_in_imgh_valid(f0_io_pixel_in_imgh_valid),
    .io_pixel_in_imgh_bits(f0_io_pixel_in_imgh_bits),
    .io_pixel_out_ready(f0_io_pixel_out_ready),
    .io_pixel_out_valid(f0_io_pixel_out_valid),
    .io_pixel_out_bits(f0_io_pixel_out_bits)
  );
  DynamicFilter_1 f1 ( // @[BlurFilter.scala 356:38]
    .clock(f1_clock),
    .reset(f1_reset),
    .io_pixel_in_pixel_ready(f1_io_pixel_in_pixel_ready),
    .io_pixel_in_pixel_valid(f1_io_pixel_in_pixel_valid),
    .io_pixel_in_pixel_bits(f1_io_pixel_in_pixel_bits),
    .io_pixel_in_imgw_ready(f1_io_pixel_in_imgw_ready),
    .io_pixel_in_imgw_valid(f1_io_pixel_in_imgw_valid),
    .io_pixel_in_imgw_bits(f1_io_pixel_in_imgw_bits),
    .io_pixel_in_imgh_ready(f1_io_pixel_in_imgh_ready),
    .io_pixel_in_imgh_valid(f1_io_pixel_in_imgh_valid),
    .io_pixel_in_imgh_bits(f1_io_pixel_in_imgh_bits),
    .io_pixel_out_ready(f1_io_pixel_out_ready),
    .io_pixel_out_valid(f1_io_pixel_out_valid),
    .io_pixel_out_bits(f1_io_pixel_out_bits)
  );
  DynamicFilter_2 f2 ( // @[BlurFilter.scala 356:38]
    .clock(f2_clock),
    .reset(f2_reset),
    .io_pixel_in_pixel_ready(f2_io_pixel_in_pixel_ready),
    .io_pixel_in_pixel_valid(f2_io_pixel_in_pixel_valid),
    .io_pixel_in_pixel_bits(f2_io_pixel_in_pixel_bits),
    .io_pixel_in_imgw_ready(f2_io_pixel_in_imgw_ready),
    .io_pixel_in_imgw_valid(f2_io_pixel_in_imgw_valid),
    .io_pixel_in_imgw_bits(f2_io_pixel_in_imgw_bits),
    .io_pixel_in_imgh_ready(f2_io_pixel_in_imgh_ready),
    .io_pixel_in_imgh_valid(f2_io_pixel_in_imgh_valid),
    .io_pixel_in_imgh_bits(f2_io_pixel_in_imgh_bits),
    .io_pixel_out_ready(f2_io_pixel_out_ready),
    .io_pixel_out_valid(f2_io_pixel_out_valid),
    .io_pixel_out_bits(f2_io_pixel_out_bits)
  );
  DynamicFilter_3 f3 ( // @[BlurFilter.scala 356:38]
    .clock(f3_clock),
    .reset(f3_reset),
    .io_pixel_in_pixel_ready(f3_io_pixel_in_pixel_ready),
    .io_pixel_in_pixel_valid(f3_io_pixel_in_pixel_valid),
    .io_pixel_in_pixel_bits(f3_io_pixel_in_pixel_bits),
    .io_pixel_in_imgw_ready(f3_io_pixel_in_imgw_ready),
    .io_pixel_in_imgw_valid(f3_io_pixel_in_imgw_valid),
    .io_pixel_in_imgw_bits(f3_io_pixel_in_imgw_bits),
    .io_pixel_in_imgh_ready(f3_io_pixel_in_imgh_ready),
    .io_pixel_in_imgh_valid(f3_io_pixel_in_imgh_valid),
    .io_pixel_in_imgh_bits(f3_io_pixel_in_imgh_bits),
    .io_pixel_out_ready(f3_io_pixel_out_ready),
    .io_pixel_out_valid(f3_io_pixel_out_valid),
    .io_pixel_out_bits(f3_io_pixel_out_bits)
  );
  DynamicFilter_4 f4 ( // @[BlurFilter.scala 356:38]
    .clock(f4_clock),
    .reset(f4_reset),
    .io_pixel_in_pixel_ready(f4_io_pixel_in_pixel_ready),
    .io_pixel_in_pixel_valid(f4_io_pixel_in_pixel_valid),
    .io_pixel_in_pixel_bits(f4_io_pixel_in_pixel_bits),
    .io_pixel_in_imgw_ready(f4_io_pixel_in_imgw_ready),
    .io_pixel_in_imgw_valid(f4_io_pixel_in_imgw_valid),
    .io_pixel_in_imgw_bits(f4_io_pixel_in_imgw_bits),
    .io_pixel_in_imgh_ready(f4_io_pixel_in_imgh_ready),
    .io_pixel_in_imgh_valid(f4_io_pixel_in_imgh_valid),
    .io_pixel_in_imgh_bits(f4_io_pixel_in_imgh_bits),
    .io_pixel_out_ready(f4_io_pixel_out_ready),
    .io_pixel_out_valid(f4_io_pixel_out_valid),
    .io_pixel_out_bits(f4_io_pixel_out_bits)
  );
  DynamicFilter_5 f5 ( // @[BlurFilter.scala 356:38]
    .clock(f5_clock),
    .reset(f5_reset),
    .io_pixel_in_pixel_ready(f5_io_pixel_in_pixel_ready),
    .io_pixel_in_pixel_valid(f5_io_pixel_in_pixel_valid),
    .io_pixel_in_pixel_bits(f5_io_pixel_in_pixel_bits),
    .io_pixel_in_imgw_ready(f5_io_pixel_in_imgw_ready),
    .io_pixel_in_imgw_valid(f5_io_pixel_in_imgw_valid),
    .io_pixel_in_imgw_bits(f5_io_pixel_in_imgw_bits),
    .io_pixel_in_imgh_ready(f5_io_pixel_in_imgh_ready),
    .io_pixel_in_imgh_valid(f5_io_pixel_in_imgh_valid),
    .io_pixel_in_imgh_bits(f5_io_pixel_in_imgh_bits),
    .io_pixel_out_ready(f5_io_pixel_out_ready),
    .io_pixel_out_valid(f5_io_pixel_out_valid),
    .io_pixel_out_bits(f5_io_pixel_out_bits)
  );
  DynamicFilter_6 f6 ( // @[BlurFilter.scala 356:38]
    .clock(f6_clock),
    .reset(f6_reset),
    .io_pixel_in_pixel_ready(f6_io_pixel_in_pixel_ready),
    .io_pixel_in_pixel_valid(f6_io_pixel_in_pixel_valid),
    .io_pixel_in_pixel_bits(f6_io_pixel_in_pixel_bits),
    .io_pixel_in_imgw_ready(f6_io_pixel_in_imgw_ready),
    .io_pixel_in_imgw_valid(f6_io_pixel_in_imgw_valid),
    .io_pixel_in_imgw_bits(f6_io_pixel_in_imgw_bits),
    .io_pixel_in_imgh_ready(f6_io_pixel_in_imgh_ready),
    .io_pixel_in_imgh_valid(f6_io_pixel_in_imgh_valid),
    .io_pixel_in_imgh_bits(f6_io_pixel_in_imgh_bits),
    .io_pixel_out_ready(f6_io_pixel_out_ready),
    .io_pixel_out_valid(f6_io_pixel_out_valid),
    .io_pixel_out_bits(f6_io_pixel_out_bits)
  );
  DynamicFilter_7 f7 ( // @[BlurFilter.scala 356:38]
    .clock(f7_clock),
    .reset(f7_reset),
    .io_pixel_in_pixel_ready(f7_io_pixel_in_pixel_ready),
    .io_pixel_in_pixel_valid(f7_io_pixel_in_pixel_valid),
    .io_pixel_in_pixel_bits(f7_io_pixel_in_pixel_bits),
    .io_pixel_in_imgw_ready(f7_io_pixel_in_imgw_ready),
    .io_pixel_in_imgw_valid(f7_io_pixel_in_imgw_valid),
    .io_pixel_in_imgw_bits(f7_io_pixel_in_imgw_bits),
    .io_pixel_in_imgh_ready(f7_io_pixel_in_imgh_ready),
    .io_pixel_in_imgh_valid(f7_io_pixel_in_imgh_valid),
    .io_pixel_in_imgh_bits(f7_io_pixel_in_imgh_bits),
    .io_pixel_out_ready(f7_io_pixel_out_ready),
    .io_pixel_out_valid(f7_io_pixel_out_valid),
    .io_pixel_out_bits(f7_io_pixel_out_bits)
  );
  DynamicFilter_8 f8 ( // @[BlurFilter.scala 356:38]
    .clock(f8_clock),
    .reset(f8_reset),
    .io_pixel_in_pixel_ready(f8_io_pixel_in_pixel_ready),
    .io_pixel_in_pixel_valid(f8_io_pixel_in_pixel_valid),
    .io_pixel_in_pixel_bits(f8_io_pixel_in_pixel_bits),
    .io_pixel_in_imgw_ready(f8_io_pixel_in_imgw_ready),
    .io_pixel_in_imgw_valid(f8_io_pixel_in_imgw_valid),
    .io_pixel_in_imgw_bits(f8_io_pixel_in_imgw_bits),
    .io_pixel_in_imgh_ready(f8_io_pixel_in_imgh_ready),
    .io_pixel_in_imgh_valid(f8_io_pixel_in_imgh_valid),
    .io_pixel_in_imgh_bits(f8_io_pixel_in_imgh_bits),
    .io_pixel_out_ready(f8_io_pixel_out_ready),
    .io_pixel_out_valid(f8_io_pixel_out_valid),
    .io_pixel_out_bits(f8_io_pixel_out_bits)
  );
  Splitter s0 ( // @[BlurFilter.scala 368:18]
    .io_pixel_in_ready(s0_io_pixel_in_ready),
    .io_pixel_in_valid(s0_io_pixel_in_valid),
    .io_pixel_in_bits(s0_io_pixel_in_bits),
    .io_pixel_left_ready(s0_io_pixel_left_ready),
    .io_pixel_left_valid(s0_io_pixel_left_valid),
    .io_pixel_left_bits(s0_io_pixel_left_bits),
    .io_pixel_right_ready(s0_io_pixel_right_ready),
    .io_pixel_right_valid(s0_io_pixel_right_valid),
    .io_pixel_right_bits(s0_io_pixel_right_bits)
  );
  Splitter s1 ( // @[BlurFilter.scala 369:18]
    .io_pixel_in_ready(s1_io_pixel_in_ready),
    .io_pixel_in_valid(s1_io_pixel_in_valid),
    .io_pixel_in_bits(s1_io_pixel_in_bits),
    .io_pixel_left_ready(s1_io_pixel_left_ready),
    .io_pixel_left_valid(s1_io_pixel_left_valid),
    .io_pixel_left_bits(s1_io_pixel_left_bits),
    .io_pixel_right_ready(s1_io_pixel_right_ready),
    .io_pixel_right_valid(s1_io_pixel_right_valid),
    .io_pixel_right_bits(s1_io_pixel_right_bits)
  );
  Splitter s2 ( // @[BlurFilter.scala 370:18]
    .io_pixel_in_ready(s2_io_pixel_in_ready),
    .io_pixel_in_valid(s2_io_pixel_in_valid),
    .io_pixel_in_bits(s2_io_pixel_in_bits),
    .io_pixel_left_ready(s2_io_pixel_left_ready),
    .io_pixel_left_valid(s2_io_pixel_left_valid),
    .io_pixel_left_bits(s2_io_pixel_left_bits),
    .io_pixel_right_ready(s2_io_pixel_right_ready),
    .io_pixel_right_valid(s2_io_pixel_right_valid),
    .io_pixel_right_bits(s2_io_pixel_right_bits)
  );
  Splitter s3 ( // @[BlurFilter.scala 371:18]
    .io_pixel_in_ready(s3_io_pixel_in_ready),
    .io_pixel_in_valid(s3_io_pixel_in_valid),
    .io_pixel_in_bits(s3_io_pixel_in_bits),
    .io_pixel_left_ready(s3_io_pixel_left_ready),
    .io_pixel_left_valid(s3_io_pixel_left_valid),
    .io_pixel_left_bits(s3_io_pixel_left_bits),
    .io_pixel_right_ready(s3_io_pixel_right_ready),
    .io_pixel_right_valid(s3_io_pixel_right_valid),
    .io_pixel_right_bits(s3_io_pixel_right_bits)
  );
  Splitter s4 ( // @[BlurFilter.scala 372:18]
    .io_pixel_in_ready(s4_io_pixel_in_ready),
    .io_pixel_in_valid(s4_io_pixel_in_valid),
    .io_pixel_in_bits(s4_io_pixel_in_bits),
    .io_pixel_left_ready(s4_io_pixel_left_ready),
    .io_pixel_left_valid(s4_io_pixel_left_valid),
    .io_pixel_left_bits(s4_io_pixel_left_bits),
    .io_pixel_right_ready(s4_io_pixel_right_ready),
    .io_pixel_right_valid(s4_io_pixel_right_valid),
    .io_pixel_right_bits(s4_io_pixel_right_bits)
  );
  Splitter s5 ( // @[BlurFilter.scala 373:18]
    .io_pixel_in_ready(s5_io_pixel_in_ready),
    .io_pixel_in_valid(s5_io_pixel_in_valid),
    .io_pixel_in_bits(s5_io_pixel_in_bits),
    .io_pixel_left_ready(s5_io_pixel_left_ready),
    .io_pixel_left_valid(s5_io_pixel_left_valid),
    .io_pixel_left_bits(s5_io_pixel_left_bits),
    .io_pixel_right_ready(s5_io_pixel_right_ready),
    .io_pixel_right_valid(s5_io_pixel_right_valid),
    .io_pixel_right_bits(s5_io_pixel_right_bits)
  );
  Splitter s6 ( // @[BlurFilter.scala 374:18]
    .io_pixel_in_ready(s6_io_pixel_in_ready),
    .io_pixel_in_valid(s6_io_pixel_in_valid),
    .io_pixel_in_bits(s6_io_pixel_in_bits),
    .io_pixel_left_ready(s6_io_pixel_left_ready),
    .io_pixel_left_valid(s6_io_pixel_left_valid),
    .io_pixel_left_bits(s6_io_pixel_left_bits),
    .io_pixel_right_ready(s6_io_pixel_right_ready),
    .io_pixel_right_valid(s6_io_pixel_right_valid),
    .io_pixel_right_bits(s6_io_pixel_right_bits)
  );
  Splitter s7 ( // @[BlurFilter.scala 375:18]
    .io_pixel_in_ready(s7_io_pixel_in_ready),
    .io_pixel_in_valid(s7_io_pixel_in_valid),
    .io_pixel_in_bits(s7_io_pixel_in_bits),
    .io_pixel_left_ready(s7_io_pixel_left_ready),
    .io_pixel_left_valid(s7_io_pixel_left_valid),
    .io_pixel_left_bits(s7_io_pixel_left_bits),
    .io_pixel_right_ready(s7_io_pixel_right_ready),
    .io_pixel_right_valid(s7_io_pixel_right_valid),
    .io_pixel_right_bits(s7_io_pixel_right_bits)
  );
  Gaussian3x3 compute ( // @[BlurFilter.scala 377:23]
    .clock(compute_clock),
    .reset(compute_reset),
    .io_pixels_in_0_ready(compute_io_pixels_in_0_ready),
    .io_pixels_in_0_valid(compute_io_pixels_in_0_valid),
    .io_pixels_in_0_bits(compute_io_pixels_in_0_bits),
    .io_pixels_in_1_ready(compute_io_pixels_in_1_ready),
    .io_pixels_in_1_valid(compute_io_pixels_in_1_valid),
    .io_pixels_in_1_bits(compute_io_pixels_in_1_bits),
    .io_pixels_in_2_ready(compute_io_pixels_in_2_ready),
    .io_pixels_in_2_valid(compute_io_pixels_in_2_valid),
    .io_pixels_in_2_bits(compute_io_pixels_in_2_bits),
    .io_pixels_in_3_ready(compute_io_pixels_in_3_ready),
    .io_pixels_in_3_valid(compute_io_pixels_in_3_valid),
    .io_pixels_in_3_bits(compute_io_pixels_in_3_bits),
    .io_pixels_in_4_ready(compute_io_pixels_in_4_ready),
    .io_pixels_in_4_valid(compute_io_pixels_in_4_valid),
    .io_pixels_in_4_bits(compute_io_pixels_in_4_bits),
    .io_pixels_in_5_ready(compute_io_pixels_in_5_ready),
    .io_pixels_in_5_valid(compute_io_pixels_in_5_valid),
    .io_pixels_in_5_bits(compute_io_pixels_in_5_bits),
    .io_pixels_in_6_ready(compute_io_pixels_in_6_ready),
    .io_pixels_in_6_valid(compute_io_pixels_in_6_valid),
    .io_pixels_in_6_bits(compute_io_pixels_in_6_bits),
    .io_pixels_in_7_ready(compute_io_pixels_in_7_ready),
    .io_pixels_in_7_valid(compute_io_pixels_in_7_valid),
    .io_pixels_in_7_bits(compute_io_pixels_in_7_bits),
    .io_pixels_in_8_ready(compute_io_pixels_in_8_ready),
    .io_pixels_in_8_valid(compute_io_pixels_in_8_valid),
    .io_pixels_in_8_bits(compute_io_pixels_in_8_bits),
    .io_pixel_out_ready(compute_io_pixel_out_ready),
    .io_pixel_out_valid(compute_io_pixel_out_valid),
    .io_pixel_out_bits(compute_io_pixel_out_bits)
  );
  Queue q ( // @[Decoupled.scala 361:21]
    .clock(q_clock),
    .reset(q_reset),
    .io_enq_ready(q_io_enq_ready),
    .io_enq_valid(q_io_enq_valid),
    .io_enq_bits(q_io_enq_bits),
    .io_deq_ready(q_io_deq_ready),
    .io_deq_valid(q_io_deq_valid),
    .io_deq_bits(q_io_deq_bits)
  );
  Queue_1 q_1 ( // @[Decoupled.scala 361:21]
    .clock(q_1_clock),
    .reset(q_1_reset),
    .io_enq_ready(q_1_io_enq_ready),
    .io_enq_valid(q_1_io_enq_valid),
    .io_enq_bits(q_1_io_enq_bits),
    .io_deq_ready(q_1_io_deq_ready),
    .io_deq_valid(q_1_io_deq_valid),
    .io_deq_bits(q_1_io_deq_bits)
  );
  Queue q_2 ( // @[Decoupled.scala 361:21]
    .clock(q_2_clock),
    .reset(q_2_reset),
    .io_enq_ready(q_2_io_enq_ready),
    .io_enq_valid(q_2_io_enq_valid),
    .io_enq_bits(q_2_io_enq_bits),
    .io_deq_ready(q_2_io_deq_ready),
    .io_deq_valid(q_2_io_deq_valid),
    .io_deq_bits(q_2_io_deq_bits)
  );
  Queue_1 q_3 ( // @[Decoupled.scala 361:21]
    .clock(q_3_clock),
    .reset(q_3_reset),
    .io_enq_ready(q_3_io_enq_ready),
    .io_enq_valid(q_3_io_enq_valid),
    .io_enq_bits(q_3_io_enq_bits),
    .io_deq_ready(q_3_io_deq_ready),
    .io_deq_valid(q_3_io_deq_valid),
    .io_deq_bits(q_3_io_deq_bits)
  );
  Queue q_4 ( // @[Decoupled.scala 361:21]
    .clock(q_4_clock),
    .reset(q_4_reset),
    .io_enq_ready(q_4_io_enq_ready),
    .io_enq_valid(q_4_io_enq_valid),
    .io_enq_bits(q_4_io_enq_bits),
    .io_deq_ready(q_4_io_deq_ready),
    .io_deq_valid(q_4_io_deq_valid),
    .io_deq_bits(q_4_io_deq_bits)
  );
  Queue_1 q_5 ( // @[Decoupled.scala 361:21]
    .clock(q_5_clock),
    .reset(q_5_reset),
    .io_enq_ready(q_5_io_enq_ready),
    .io_enq_valid(q_5_io_enq_valid),
    .io_enq_bits(q_5_io_enq_bits),
    .io_deq_ready(q_5_io_deq_ready),
    .io_deq_valid(q_5_io_deq_valid),
    .io_deq_bits(q_5_io_deq_bits)
  );
  Queue q_6 ( // @[Decoupled.scala 361:21]
    .clock(q_6_clock),
    .reset(q_6_reset),
    .io_enq_ready(q_6_io_enq_ready),
    .io_enq_valid(q_6_io_enq_valid),
    .io_enq_bits(q_6_io_enq_bits),
    .io_deq_ready(q_6_io_deq_ready),
    .io_deq_valid(q_6_io_deq_valid),
    .io_deq_bits(q_6_io_deq_bits)
  );
  Queue_1 q_7 ( // @[Decoupled.scala 361:21]
    .clock(q_7_clock),
    .reset(q_7_reset),
    .io_enq_ready(q_7_io_enq_ready),
    .io_enq_valid(q_7_io_enq_valid),
    .io_enq_bits(q_7_io_enq_bits),
    .io_deq_ready(q_7_io_deq_ready),
    .io_deq_valid(q_7_io_deq_valid),
    .io_deq_bits(q_7_io_deq_bits)
  );
  Queue q_8 ( // @[Decoupled.scala 361:21]
    .clock(q_8_clock),
    .reset(q_8_reset),
    .io_enq_ready(q_8_io_enq_ready),
    .io_enq_valid(q_8_io_enq_valid),
    .io_enq_bits(q_8_io_enq_bits),
    .io_deq_ready(q_8_io_deq_ready),
    .io_deq_valid(q_8_io_deq_valid),
    .io_deq_bits(q_8_io_deq_bits)
  );
  Queue_1 q_9 ( // @[Decoupled.scala 361:21]
    .clock(q_9_clock),
    .reset(q_9_reset),
    .io_enq_ready(q_9_io_enq_ready),
    .io_enq_valid(q_9_io_enq_valid),
    .io_enq_bits(q_9_io_enq_bits),
    .io_deq_ready(q_9_io_deq_ready),
    .io_deq_valid(q_9_io_deq_valid),
    .io_deq_bits(q_9_io_deq_bits)
  );
  Queue q_10 ( // @[Decoupled.scala 361:21]
    .clock(q_10_clock),
    .reset(q_10_reset),
    .io_enq_ready(q_10_io_enq_ready),
    .io_enq_valid(q_10_io_enq_valid),
    .io_enq_bits(q_10_io_enq_bits),
    .io_deq_ready(q_10_io_deq_ready),
    .io_deq_valid(q_10_io_deq_valid),
    .io_deq_bits(q_10_io_deq_bits)
  );
  Queue_1 q_11 ( // @[Decoupled.scala 361:21]
    .clock(q_11_clock),
    .reset(q_11_reset),
    .io_enq_ready(q_11_io_enq_ready),
    .io_enq_valid(q_11_io_enq_valid),
    .io_enq_bits(q_11_io_enq_bits),
    .io_deq_ready(q_11_io_deq_ready),
    .io_deq_valid(q_11_io_deq_valid),
    .io_deq_bits(q_11_io_deq_bits)
  );
  Queue q_12 ( // @[Decoupled.scala 361:21]
    .clock(q_12_clock),
    .reset(q_12_reset),
    .io_enq_ready(q_12_io_enq_ready),
    .io_enq_valid(q_12_io_enq_valid),
    .io_enq_bits(q_12_io_enq_bits),
    .io_deq_ready(q_12_io_deq_ready),
    .io_deq_valid(q_12_io_deq_valid),
    .io_deq_bits(q_12_io_deq_bits)
  );
  Queue_1 q_13 ( // @[Decoupled.scala 361:21]
    .clock(q_13_clock),
    .reset(q_13_reset),
    .io_enq_ready(q_13_io_enq_ready),
    .io_enq_valid(q_13_io_enq_valid),
    .io_enq_bits(q_13_io_enq_bits),
    .io_deq_ready(q_13_io_deq_ready),
    .io_deq_valid(q_13_io_deq_valid),
    .io_deq_bits(q_13_io_deq_bits)
  );
  Queue q_14 ( // @[Decoupled.scala 361:21]
    .clock(q_14_clock),
    .reset(q_14_reset),
    .io_enq_ready(q_14_io_enq_ready),
    .io_enq_valid(q_14_io_enq_valid),
    .io_enq_bits(q_14_io_enq_bits),
    .io_deq_ready(q_14_io_deq_ready),
    .io_deq_valid(q_14_io_deq_valid),
    .io_deq_bits(q_14_io_deq_bits)
  );
  Queue_1 q_15 ( // @[Decoupled.scala 361:21]
    .clock(q_15_clock),
    .reset(q_15_reset),
    .io_enq_ready(q_15_io_enq_ready),
    .io_enq_valid(q_15_io_enq_valid),
    .io_enq_bits(q_15_io_enq_bits),
    .io_deq_ready(q_15_io_deq_ready),
    .io_deq_valid(q_15_io_deq_valid),
    .io_deq_bits(q_15_io_deq_bits)
  );
  Queue_1 q_16 ( // @[Decoupled.scala 361:21]
    .clock(q_16_clock),
    .reset(q_16_reset),
    .io_enq_ready(q_16_io_enq_ready),
    .io_enq_valid(q_16_io_enq_valid),
    .io_enq_bits(q_16_io_enq_bits),
    .io_deq_ready(q_16_io_deq_ready),
    .io_deq_valid(q_16_io_deq_valid),
    .io_deq_bits(q_16_io_deq_bits)
  );
  Queue_1 q_17 ( // @[Decoupled.scala 361:21]
    .clock(q_17_clock),
    .reset(q_17_reset),
    .io_enq_ready(q_17_io_enq_ready),
    .io_enq_valid(q_17_io_enq_valid),
    .io_enq_bits(q_17_io_enq_bits),
    .io_deq_ready(q_17_io_deq_ready),
    .io_deq_valid(q_17_io_deq_valid),
    .io_deq_bits(q_17_io_deq_bits)
  );
  Queue_1 q_18 ( // @[Decoupled.scala 361:21]
    .clock(q_18_clock),
    .reset(q_18_reset),
    .io_enq_ready(q_18_io_enq_ready),
    .io_enq_valid(q_18_io_enq_valid),
    .io_enq_bits(q_18_io_enq_bits),
    .io_deq_ready(q_18_io_deq_ready),
    .io_deq_valid(q_18_io_deq_valid),
    .io_deq_bits(q_18_io_deq_bits)
  );
  Queue_1 q_19 ( // @[Decoupled.scala 361:21]
    .clock(q_19_clock),
    .reset(q_19_reset),
    .io_enq_ready(q_19_io_enq_ready),
    .io_enq_valid(q_19_io_enq_valid),
    .io_enq_bits(q_19_io_enq_bits),
    .io_deq_ready(q_19_io_deq_ready),
    .io_deq_valid(q_19_io_deq_valid),
    .io_deq_bits(q_19_io_deq_bits)
  );
  Queue_1 q_20 ( // @[Decoupled.scala 361:21]
    .clock(q_20_clock),
    .reset(q_20_reset),
    .io_enq_ready(q_20_io_enq_ready),
    .io_enq_valid(q_20_io_enq_valid),
    .io_enq_bits(q_20_io_enq_bits),
    .io_deq_ready(q_20_io_deq_ready),
    .io_deq_valid(q_20_io_deq_valid),
    .io_deq_bits(q_20_io_deq_bits)
  );
  Queue_1 q_21 ( // @[Decoupled.scala 361:21]
    .clock(q_21_clock),
    .reset(q_21_reset),
    .io_enq_ready(q_21_io_enq_ready),
    .io_enq_valid(q_21_io_enq_valid),
    .io_enq_bits(q_21_io_enq_bits),
    .io_deq_ready(q_21_io_deq_ready),
    .io_deq_valid(q_21_io_deq_valid),
    .io_deq_bits(q_21_io_deq_bits)
  );
  Queue_1 q_22 ( // @[Decoupled.scala 361:21]
    .clock(q_22_clock),
    .reset(q_22_reset),
    .io_enq_ready(q_22_io_enq_ready),
    .io_enq_valid(q_22_io_enq_valid),
    .io_enq_bits(q_22_io_enq_bits),
    .io_deq_ready(q_22_io_deq_ready),
    .io_deq_valid(q_22_io_deq_valid),
    .io_deq_bits(q_22_io_deq_bits)
  );
  Queue_1 q_23 ( // @[Decoupled.scala 361:21]
    .clock(q_23_clock),
    .reset(q_23_reset),
    .io_enq_ready(q_23_io_enq_ready),
    .io_enq_valid(q_23_io_enq_valid),
    .io_enq_bits(q_23_io_enq_bits),
    .io_deq_ready(q_23_io_deq_ready),
    .io_deq_valid(q_23_io_deq_valid),
    .io_deq_bits(q_23_io_deq_bits)
  );
  Queue_1 q_24 ( // @[Decoupled.scala 361:21]
    .clock(q_24_clock),
    .reset(q_24_reset),
    .io_enq_ready(q_24_io_enq_ready),
    .io_enq_valid(q_24_io_enq_valid),
    .io_enq_bits(q_24_io_enq_bits),
    .io_deq_ready(q_24_io_deq_ready),
    .io_deq_valid(q_24_io_deq_valid),
    .io_deq_bits(q_24_io_deq_bits)
  );
  Queue_1 q_25 ( // @[Decoupled.scala 361:21]
    .clock(q_25_clock),
    .reset(q_25_reset),
    .io_enq_ready(q_25_io_enq_ready),
    .io_enq_valid(q_25_io_enq_valid),
    .io_enq_bits(q_25_io_enq_bits),
    .io_deq_ready(q_25_io_deq_ready),
    .io_deq_valid(q_25_io_deq_valid),
    .io_deq_bits(q_25_io_deq_bits)
  );
  Queue_26 fanout ( // @[BlurFilter.scala 320:36]
    .clock(fanout_clock),
    .reset(fanout_reset),
    .io_enq_ready(fanout_io_enq_ready),
    .io_enq_valid(fanout_io_enq_valid),
    .io_enq_bits(fanout_io_enq_bits),
    .io_deq_ready(fanout_io_deq_ready),
    .io_deq_valid(fanout_io_deq_valid),
    .io_deq_bits(fanout_io_deq_bits)
  );
  Queue_26 fanout_1 ( // @[BlurFilter.scala 320:36]
    .clock(fanout_1_clock),
    .reset(fanout_1_reset),
    .io_enq_ready(fanout_1_io_enq_ready),
    .io_enq_valid(fanout_1_io_enq_valid),
    .io_enq_bits(fanout_1_io_enq_bits),
    .io_deq_ready(fanout_1_io_deq_ready),
    .io_deq_valid(fanout_1_io_deq_valid),
    .io_deq_bits(fanout_1_io_deq_bits)
  );
  Queue_26 fanout_2 ( // @[BlurFilter.scala 320:36]
    .clock(fanout_2_clock),
    .reset(fanout_2_reset),
    .io_enq_ready(fanout_2_io_enq_ready),
    .io_enq_valid(fanout_2_io_enq_valid),
    .io_enq_bits(fanout_2_io_enq_bits),
    .io_deq_ready(fanout_2_io_deq_ready),
    .io_deq_valid(fanout_2_io_deq_valid),
    .io_deq_bits(fanout_2_io_deq_bits)
  );
  Queue_26 fanout_3 ( // @[BlurFilter.scala 320:36]
    .clock(fanout_3_clock),
    .reset(fanout_3_reset),
    .io_enq_ready(fanout_3_io_enq_ready),
    .io_enq_valid(fanout_3_io_enq_valid),
    .io_enq_bits(fanout_3_io_enq_bits),
    .io_deq_ready(fanout_3_io_deq_ready),
    .io_deq_valid(fanout_3_io_deq_valid),
    .io_deq_bits(fanout_3_io_deq_bits)
  );
  Queue_26 fanout_4 ( // @[BlurFilter.scala 320:36]
    .clock(fanout_4_clock),
    .reset(fanout_4_reset),
    .io_enq_ready(fanout_4_io_enq_ready),
    .io_enq_valid(fanout_4_io_enq_valid),
    .io_enq_bits(fanout_4_io_enq_bits),
    .io_deq_ready(fanout_4_io_deq_ready),
    .io_deq_valid(fanout_4_io_deq_valid),
    .io_deq_bits(fanout_4_io_deq_bits)
  );
  Queue_26 fanout_5 ( // @[BlurFilter.scala 320:36]
    .clock(fanout_5_clock),
    .reset(fanout_5_reset),
    .io_enq_ready(fanout_5_io_enq_ready),
    .io_enq_valid(fanout_5_io_enq_valid),
    .io_enq_bits(fanout_5_io_enq_bits),
    .io_deq_ready(fanout_5_io_deq_ready),
    .io_deq_valid(fanout_5_io_deq_valid),
    .io_deq_bits(fanout_5_io_deq_bits)
  );
  Queue_26 fanout_6 ( // @[BlurFilter.scala 320:36]
    .clock(fanout_6_clock),
    .reset(fanout_6_reset),
    .io_enq_ready(fanout_6_io_enq_ready),
    .io_enq_valid(fanout_6_io_enq_valid),
    .io_enq_bits(fanout_6_io_enq_bits),
    .io_deq_ready(fanout_6_io_deq_ready),
    .io_deq_valid(fanout_6_io_deq_valid),
    .io_deq_bits(fanout_6_io_deq_bits)
  );
  Queue_26 fanout_7 ( // @[BlurFilter.scala 320:36]
    .clock(fanout_7_clock),
    .reset(fanout_7_reset),
    .io_enq_ready(fanout_7_io_enq_ready),
    .io_enq_valid(fanout_7_io_enq_valid),
    .io_enq_bits(fanout_7_io_enq_bits),
    .io_deq_ready(fanout_7_io_deq_ready),
    .io_deq_valid(fanout_7_io_deq_valid),
    .io_deq_bits(fanout_7_io_deq_bits)
  );
  Queue_26 fanout_8 ( // @[BlurFilter.scala 320:36]
    .clock(fanout_8_clock),
    .reset(fanout_8_reset),
    .io_enq_ready(fanout_8_io_enq_ready),
    .io_enq_valid(fanout_8_io_enq_valid),
    .io_enq_bits(fanout_8_io_enq_bits),
    .io_deq_ready(fanout_8_io_deq_ready),
    .io_deq_valid(fanout_8_io_deq_valid),
    .io_deq_bits(fanout_8_io_deq_bits)
  );
  Queue_26 fanout_9 ( // @[BlurFilter.scala 320:36]
    .clock(fanout_9_clock),
    .reset(fanout_9_reset),
    .io_enq_ready(fanout_9_io_enq_ready),
    .io_enq_valid(fanout_9_io_enq_valid),
    .io_enq_bits(fanout_9_io_enq_bits),
    .io_deq_ready(fanout_9_io_deq_ready),
    .io_deq_valid(fanout_9_io_deq_valid),
    .io_deq_bits(fanout_9_io_deq_bits)
  );
  Queue_26 fanout_10 ( // @[BlurFilter.scala 320:36]
    .clock(fanout_10_clock),
    .reset(fanout_10_reset),
    .io_enq_ready(fanout_10_io_enq_ready),
    .io_enq_valid(fanout_10_io_enq_valid),
    .io_enq_bits(fanout_10_io_enq_bits),
    .io_deq_ready(fanout_10_io_deq_ready),
    .io_deq_valid(fanout_10_io_deq_valid),
    .io_deq_bits(fanout_10_io_deq_bits)
  );
  Queue_26 fanout_11 ( // @[BlurFilter.scala 320:36]
    .clock(fanout_11_clock),
    .reset(fanout_11_reset),
    .io_enq_ready(fanout_11_io_enq_ready),
    .io_enq_valid(fanout_11_io_enq_valid),
    .io_enq_bits(fanout_11_io_enq_bits),
    .io_deq_ready(fanout_11_io_deq_ready),
    .io_deq_valid(fanout_11_io_deq_valid),
    .io_deq_bits(fanout_11_io_deq_bits)
  );
  Queue_26 fanout_12 ( // @[BlurFilter.scala 320:36]
    .clock(fanout_12_clock),
    .reset(fanout_12_reset),
    .io_enq_ready(fanout_12_io_enq_ready),
    .io_enq_valid(fanout_12_io_enq_valid),
    .io_enq_bits(fanout_12_io_enq_bits),
    .io_deq_ready(fanout_12_io_deq_ready),
    .io_deq_valid(fanout_12_io_deq_valid),
    .io_deq_bits(fanout_12_io_deq_bits)
  );
  Queue_26 fanout_13 ( // @[BlurFilter.scala 320:36]
    .clock(fanout_13_clock),
    .reset(fanout_13_reset),
    .io_enq_ready(fanout_13_io_enq_ready),
    .io_enq_valid(fanout_13_io_enq_valid),
    .io_enq_bits(fanout_13_io_enq_bits),
    .io_deq_ready(fanout_13_io_deq_ready),
    .io_deq_valid(fanout_13_io_deq_valid),
    .io_deq_bits(fanout_13_io_deq_bits)
  );
  Queue_26 fanout_14 ( // @[BlurFilter.scala 320:36]
    .clock(fanout_14_clock),
    .reset(fanout_14_reset),
    .io_enq_ready(fanout_14_io_enq_ready),
    .io_enq_valid(fanout_14_io_enq_valid),
    .io_enq_bits(fanout_14_io_enq_bits),
    .io_deq_ready(fanout_14_io_deq_ready),
    .io_deq_valid(fanout_14_io_deq_valid),
    .io_deq_bits(fanout_14_io_deq_bits)
  );
  Queue_26 fanout_15 ( // @[BlurFilter.scala 320:36]
    .clock(fanout_15_clock),
    .reset(fanout_15_reset),
    .io_enq_ready(fanout_15_io_enq_ready),
    .io_enq_valid(fanout_15_io_enq_valid),
    .io_enq_bits(fanout_15_io_enq_bits),
    .io_deq_ready(fanout_15_io_deq_ready),
    .io_deq_valid(fanout_15_io_deq_valid),
    .io_deq_bits(fanout_15_io_deq_bits)
  );
  Queue_26 fanout_16 ( // @[BlurFilter.scala 320:36]
    .clock(fanout_16_clock),
    .reset(fanout_16_reset),
    .io_enq_ready(fanout_16_io_enq_ready),
    .io_enq_valid(fanout_16_io_enq_valid),
    .io_enq_bits(fanout_16_io_enq_bits),
    .io_deq_ready(fanout_16_io_deq_ready),
    .io_deq_valid(fanout_16_io_deq_valid),
    .io_deq_bits(fanout_16_io_deq_bits)
  );
  Queue_26 fanout_17 ( // @[BlurFilter.scala 320:36]
    .clock(fanout_17_clock),
    .reset(fanout_17_reset),
    .io_enq_ready(fanout_17_io_enq_ready),
    .io_enq_valid(fanout_17_io_enq_valid),
    .io_enq_bits(fanout_17_io_enq_bits),
    .io_deq_ready(fanout_17_io_deq_ready),
    .io_deq_valid(fanout_17_io_deq_valid),
    .io_deq_bits(fanout_17_io_deq_bits)
  );
  assign io_in_pixel_ready = q_io_enq_ready; // @[Decoupled.scala 365:17]
  assign io_in_imgw_ready = ready_1_0 & ready_1_1 & ready_1_2 & ready_1_3 & ready_1_4 & ready_1_5 & ready_1_6 &
    ready_1_7 & ready_1_8; // @[BlurFilter.scala 327:33]
  assign io_in_imgh_ready = ready__0 & ready__1 & ready__2 & ready__3 & ready__4 & ready__5 & ready__6 & ready__7 &
    ready__8; // @[BlurFilter.scala 327:33]
  assign io_out_valid = compute_io_pixel_out_valid; // @[BlurFilter.scala 445:10]
  assign io_out_bits = compute_io_pixel_out_bits; // @[BlurFilter.scala 445:10]
  assign f0_clock = clock;
  assign f0_reset = reset;
  assign f0_io_pixel_in_pixel_valid = q_1_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign f0_io_pixel_in_pixel_bits = q_1_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign f0_io_pixel_in_imgw_valid = fanout_9_io_deq_valid; // @[BlurFilter.scala 336:16]
  assign f0_io_pixel_in_imgw_bits = fanout_9_io_deq_bits; // @[BlurFilter.scala 336:16]
  assign f0_io_pixel_in_imgh_valid = fanout_io_deq_valid; // @[BlurFilter.scala 336:16]
  assign f0_io_pixel_in_imgh_bits = fanout_io_deq_bits; // @[BlurFilter.scala 336:16]
  assign f0_io_pixel_out_ready = q_17_io_enq_ready; // @[Decoupled.scala 365:17]
  assign f1_clock = clock;
  assign f1_reset = reset;
  assign f1_io_pixel_in_pixel_valid = q_3_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign f1_io_pixel_in_pixel_bits = q_3_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign f1_io_pixel_in_imgw_valid = fanout_10_io_deq_valid; // @[BlurFilter.scala 336:16]
  assign f1_io_pixel_in_imgw_bits = fanout_10_io_deq_bits; // @[BlurFilter.scala 336:16]
  assign f1_io_pixel_in_imgh_valid = fanout_1_io_deq_valid; // @[BlurFilter.scala 336:16]
  assign f1_io_pixel_in_imgh_bits = fanout_1_io_deq_bits; // @[BlurFilter.scala 336:16]
  assign f1_io_pixel_out_ready = q_18_io_enq_ready; // @[Decoupled.scala 365:17]
  assign f2_clock = clock;
  assign f2_reset = reset;
  assign f2_io_pixel_in_pixel_valid = q_5_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign f2_io_pixel_in_pixel_bits = q_5_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign f2_io_pixel_in_imgw_valid = fanout_11_io_deq_valid; // @[BlurFilter.scala 336:16]
  assign f2_io_pixel_in_imgw_bits = fanout_11_io_deq_bits; // @[BlurFilter.scala 336:16]
  assign f2_io_pixel_in_imgh_valid = fanout_2_io_deq_valid; // @[BlurFilter.scala 336:16]
  assign f2_io_pixel_in_imgh_bits = fanout_2_io_deq_bits; // @[BlurFilter.scala 336:16]
  assign f2_io_pixel_out_ready = q_19_io_enq_ready; // @[Decoupled.scala 365:17]
  assign f3_clock = clock;
  assign f3_reset = reset;
  assign f3_io_pixel_in_pixel_valid = q_7_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign f3_io_pixel_in_pixel_bits = q_7_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign f3_io_pixel_in_imgw_valid = fanout_12_io_deq_valid; // @[BlurFilter.scala 336:16]
  assign f3_io_pixel_in_imgw_bits = fanout_12_io_deq_bits; // @[BlurFilter.scala 336:16]
  assign f3_io_pixel_in_imgh_valid = fanout_3_io_deq_valid; // @[BlurFilter.scala 336:16]
  assign f3_io_pixel_in_imgh_bits = fanout_3_io_deq_bits; // @[BlurFilter.scala 336:16]
  assign f3_io_pixel_out_ready = q_20_io_enq_ready; // @[Decoupled.scala 365:17]
  assign f4_clock = clock;
  assign f4_reset = reset;
  assign f4_io_pixel_in_pixel_valid = q_9_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign f4_io_pixel_in_pixel_bits = q_9_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign f4_io_pixel_in_imgw_valid = fanout_13_io_deq_valid; // @[BlurFilter.scala 336:16]
  assign f4_io_pixel_in_imgw_bits = fanout_13_io_deq_bits; // @[BlurFilter.scala 336:16]
  assign f4_io_pixel_in_imgh_valid = fanout_4_io_deq_valid; // @[BlurFilter.scala 336:16]
  assign f4_io_pixel_in_imgh_bits = fanout_4_io_deq_bits; // @[BlurFilter.scala 336:16]
  assign f4_io_pixel_out_ready = q_21_io_enq_ready; // @[Decoupled.scala 365:17]
  assign f5_clock = clock;
  assign f5_reset = reset;
  assign f5_io_pixel_in_pixel_valid = q_11_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign f5_io_pixel_in_pixel_bits = q_11_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign f5_io_pixel_in_imgw_valid = fanout_14_io_deq_valid; // @[BlurFilter.scala 336:16]
  assign f5_io_pixel_in_imgw_bits = fanout_14_io_deq_bits; // @[BlurFilter.scala 336:16]
  assign f5_io_pixel_in_imgh_valid = fanout_5_io_deq_valid; // @[BlurFilter.scala 336:16]
  assign f5_io_pixel_in_imgh_bits = fanout_5_io_deq_bits; // @[BlurFilter.scala 336:16]
  assign f5_io_pixel_out_ready = q_22_io_enq_ready; // @[Decoupled.scala 365:17]
  assign f6_clock = clock;
  assign f6_reset = reset;
  assign f6_io_pixel_in_pixel_valid = q_13_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign f6_io_pixel_in_pixel_bits = q_13_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign f6_io_pixel_in_imgw_valid = fanout_15_io_deq_valid; // @[BlurFilter.scala 336:16]
  assign f6_io_pixel_in_imgw_bits = fanout_15_io_deq_bits; // @[BlurFilter.scala 336:16]
  assign f6_io_pixel_in_imgh_valid = fanout_6_io_deq_valid; // @[BlurFilter.scala 336:16]
  assign f6_io_pixel_in_imgh_bits = fanout_6_io_deq_bits; // @[BlurFilter.scala 336:16]
  assign f6_io_pixel_out_ready = q_23_io_enq_ready; // @[Decoupled.scala 365:17]
  assign f7_clock = clock;
  assign f7_reset = reset;
  assign f7_io_pixel_in_pixel_valid = q_15_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign f7_io_pixel_in_pixel_bits = q_15_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign f7_io_pixel_in_imgw_valid = fanout_16_io_deq_valid; // @[BlurFilter.scala 336:16]
  assign f7_io_pixel_in_imgw_bits = fanout_16_io_deq_bits; // @[BlurFilter.scala 336:16]
  assign f7_io_pixel_in_imgh_valid = fanout_7_io_deq_valid; // @[BlurFilter.scala 336:16]
  assign f7_io_pixel_in_imgh_bits = fanout_7_io_deq_bits; // @[BlurFilter.scala 336:16]
  assign f7_io_pixel_out_ready = q_24_io_enq_ready; // @[Decoupled.scala 365:17]
  assign f8_clock = clock;
  assign f8_reset = reset;
  assign f8_io_pixel_in_pixel_valid = q_16_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign f8_io_pixel_in_pixel_bits = q_16_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign f8_io_pixel_in_imgw_valid = fanout_17_io_deq_valid; // @[BlurFilter.scala 336:16]
  assign f8_io_pixel_in_imgw_bits = fanout_17_io_deq_bits; // @[BlurFilter.scala 336:16]
  assign f8_io_pixel_in_imgh_valid = fanout_8_io_deq_valid; // @[BlurFilter.scala 336:16]
  assign f8_io_pixel_in_imgh_bits = fanout_8_io_deq_bits; // @[BlurFilter.scala 336:16]
  assign f8_io_pixel_out_ready = q_25_io_enq_ready; // @[Decoupled.scala 365:17]
  assign s0_io_pixel_in_valid = q_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign s0_io_pixel_in_bits = q_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign s0_io_pixel_left_ready = q_1_io_enq_ready; // @[Decoupled.scala 365:17]
  assign s0_io_pixel_right_ready = q_2_io_enq_ready; // @[Decoupled.scala 365:17]
  assign s1_io_pixel_in_valid = q_2_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign s1_io_pixel_in_bits = q_2_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign s1_io_pixel_left_ready = q_3_io_enq_ready; // @[Decoupled.scala 365:17]
  assign s1_io_pixel_right_ready = q_4_io_enq_ready; // @[Decoupled.scala 365:17]
  assign s2_io_pixel_in_valid = q_4_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign s2_io_pixel_in_bits = q_4_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign s2_io_pixel_left_ready = q_5_io_enq_ready; // @[Decoupled.scala 365:17]
  assign s2_io_pixel_right_ready = q_6_io_enq_ready; // @[Decoupled.scala 365:17]
  assign s3_io_pixel_in_valid = q_6_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign s3_io_pixel_in_bits = q_6_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign s3_io_pixel_left_ready = q_7_io_enq_ready; // @[Decoupled.scala 365:17]
  assign s3_io_pixel_right_ready = q_8_io_enq_ready; // @[Decoupled.scala 365:17]
  assign s4_io_pixel_in_valid = q_8_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign s4_io_pixel_in_bits = q_8_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign s4_io_pixel_left_ready = q_9_io_enq_ready; // @[Decoupled.scala 365:17]
  assign s4_io_pixel_right_ready = q_10_io_enq_ready; // @[Decoupled.scala 365:17]
  assign s5_io_pixel_in_valid = q_10_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign s5_io_pixel_in_bits = q_10_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign s5_io_pixel_left_ready = q_11_io_enq_ready; // @[Decoupled.scala 365:17]
  assign s5_io_pixel_right_ready = q_12_io_enq_ready; // @[Decoupled.scala 365:17]
  assign s6_io_pixel_in_valid = q_12_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign s6_io_pixel_in_bits = q_12_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign s6_io_pixel_left_ready = q_13_io_enq_ready; // @[Decoupled.scala 365:17]
  assign s6_io_pixel_right_ready = q_14_io_enq_ready; // @[Decoupled.scala 365:17]
  assign s7_io_pixel_in_valid = q_14_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign s7_io_pixel_in_bits = q_14_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign s7_io_pixel_left_ready = q_15_io_enq_ready; // @[Decoupled.scala 365:17]
  assign s7_io_pixel_right_ready = q_16_io_enq_ready; // @[Decoupled.scala 365:17]
  assign compute_clock = clock;
  assign compute_reset = reset;
  assign compute_io_pixels_in_0_valid = q_17_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign compute_io_pixels_in_0_bits = q_17_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign compute_io_pixels_in_1_valid = q_18_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign compute_io_pixels_in_1_bits = q_18_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign compute_io_pixels_in_2_valid = q_19_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign compute_io_pixels_in_2_bits = q_19_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign compute_io_pixels_in_3_valid = q_20_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign compute_io_pixels_in_3_bits = q_20_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign compute_io_pixels_in_4_valid = q_21_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign compute_io_pixels_in_4_bits = q_21_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign compute_io_pixels_in_5_valid = q_22_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign compute_io_pixels_in_5_bits = q_22_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign compute_io_pixels_in_6_valid = q_23_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign compute_io_pixels_in_6_bits = q_23_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign compute_io_pixels_in_7_valid = q_24_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign compute_io_pixels_in_7_bits = q_24_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign compute_io_pixels_in_8_valid = q_25_io_deq_valid; // @[BlurFilter.scala 314:12]
  assign compute_io_pixels_in_8_bits = q_25_io_deq_bits; // @[BlurFilter.scala 314:12]
  assign compute_io_pixel_out_ready = io_out_ready; // @[BlurFilter.scala 445:10]
  assign q_clock = clock;
  assign q_reset = reset;
  assign q_io_enq_valid = io_in_pixel_valid; // @[Decoupled.scala 363:22]
  assign q_io_enq_bits = io_in_pixel_bits; // @[Decoupled.scala 364:21]
  assign q_io_deq_ready = s0_io_pixel_in_ready; // @[BlurFilter.scala 314:12]
  assign q_1_clock = clock;
  assign q_1_reset = reset;
  assign q_1_io_enq_valid = s0_io_pixel_left_valid; // @[Decoupled.scala 363:22]
  assign q_1_io_enq_bits = s0_io_pixel_left_bits; // @[Decoupled.scala 364:21]
  assign q_1_io_deq_ready = f0_io_pixel_in_pixel_ready; // @[BlurFilter.scala 314:12]
  assign q_2_clock = clock;
  assign q_2_reset = reset;
  assign q_2_io_enq_valid = s0_io_pixel_right_valid; // @[Decoupled.scala 363:22]
  assign q_2_io_enq_bits = s0_io_pixel_right_bits; // @[Decoupled.scala 364:21]
  assign q_2_io_deq_ready = s1_io_pixel_in_ready; // @[BlurFilter.scala 314:12]
  assign q_3_clock = clock;
  assign q_3_reset = reset;
  assign q_3_io_enq_valid = s1_io_pixel_left_valid; // @[Decoupled.scala 363:22]
  assign q_3_io_enq_bits = s1_io_pixel_left_bits; // @[Decoupled.scala 364:21]
  assign q_3_io_deq_ready = f1_io_pixel_in_pixel_ready; // @[BlurFilter.scala 314:12]
  assign q_4_clock = clock;
  assign q_4_reset = reset;
  assign q_4_io_enq_valid = s1_io_pixel_right_valid; // @[Decoupled.scala 363:22]
  assign q_4_io_enq_bits = s1_io_pixel_right_bits; // @[Decoupled.scala 364:21]
  assign q_4_io_deq_ready = s2_io_pixel_in_ready; // @[BlurFilter.scala 314:12]
  assign q_5_clock = clock;
  assign q_5_reset = reset;
  assign q_5_io_enq_valid = s2_io_pixel_left_valid; // @[Decoupled.scala 363:22]
  assign q_5_io_enq_bits = s2_io_pixel_left_bits; // @[Decoupled.scala 364:21]
  assign q_5_io_deq_ready = f2_io_pixel_in_pixel_ready; // @[BlurFilter.scala 314:12]
  assign q_6_clock = clock;
  assign q_6_reset = reset;
  assign q_6_io_enq_valid = s2_io_pixel_right_valid; // @[Decoupled.scala 363:22]
  assign q_6_io_enq_bits = s2_io_pixel_right_bits; // @[Decoupled.scala 364:21]
  assign q_6_io_deq_ready = s3_io_pixel_in_ready; // @[BlurFilter.scala 314:12]
  assign q_7_clock = clock;
  assign q_7_reset = reset;
  assign q_7_io_enq_valid = s3_io_pixel_left_valid; // @[Decoupled.scala 363:22]
  assign q_7_io_enq_bits = s3_io_pixel_left_bits; // @[Decoupled.scala 364:21]
  assign q_7_io_deq_ready = f3_io_pixel_in_pixel_ready; // @[BlurFilter.scala 314:12]
  assign q_8_clock = clock;
  assign q_8_reset = reset;
  assign q_8_io_enq_valid = s3_io_pixel_right_valid; // @[Decoupled.scala 363:22]
  assign q_8_io_enq_bits = s3_io_pixel_right_bits; // @[Decoupled.scala 364:21]
  assign q_8_io_deq_ready = s4_io_pixel_in_ready; // @[BlurFilter.scala 314:12]
  assign q_9_clock = clock;
  assign q_9_reset = reset;
  assign q_9_io_enq_valid = s4_io_pixel_left_valid; // @[Decoupled.scala 363:22]
  assign q_9_io_enq_bits = s4_io_pixel_left_bits; // @[Decoupled.scala 364:21]
  assign q_9_io_deq_ready = f4_io_pixel_in_pixel_ready; // @[BlurFilter.scala 314:12]
  assign q_10_clock = clock;
  assign q_10_reset = reset;
  assign q_10_io_enq_valid = s4_io_pixel_right_valid; // @[Decoupled.scala 363:22]
  assign q_10_io_enq_bits = s4_io_pixel_right_bits; // @[Decoupled.scala 364:21]
  assign q_10_io_deq_ready = s5_io_pixel_in_ready; // @[BlurFilter.scala 314:12]
  assign q_11_clock = clock;
  assign q_11_reset = reset;
  assign q_11_io_enq_valid = s5_io_pixel_left_valid; // @[Decoupled.scala 363:22]
  assign q_11_io_enq_bits = s5_io_pixel_left_bits; // @[Decoupled.scala 364:21]
  assign q_11_io_deq_ready = f5_io_pixel_in_pixel_ready; // @[BlurFilter.scala 314:12]
  assign q_12_clock = clock;
  assign q_12_reset = reset;
  assign q_12_io_enq_valid = s5_io_pixel_right_valid; // @[Decoupled.scala 363:22]
  assign q_12_io_enq_bits = s5_io_pixel_right_bits; // @[Decoupled.scala 364:21]
  assign q_12_io_deq_ready = s6_io_pixel_in_ready; // @[BlurFilter.scala 314:12]
  assign q_13_clock = clock;
  assign q_13_reset = reset;
  assign q_13_io_enq_valid = s6_io_pixel_left_valid; // @[Decoupled.scala 363:22]
  assign q_13_io_enq_bits = s6_io_pixel_left_bits; // @[Decoupled.scala 364:21]
  assign q_13_io_deq_ready = f6_io_pixel_in_pixel_ready; // @[BlurFilter.scala 314:12]
  assign q_14_clock = clock;
  assign q_14_reset = reset;
  assign q_14_io_enq_valid = s6_io_pixel_right_valid; // @[Decoupled.scala 363:22]
  assign q_14_io_enq_bits = s6_io_pixel_right_bits; // @[Decoupled.scala 364:21]
  assign q_14_io_deq_ready = s7_io_pixel_in_ready; // @[BlurFilter.scala 314:12]
  assign q_15_clock = clock;
  assign q_15_reset = reset;
  assign q_15_io_enq_valid = s7_io_pixel_left_valid; // @[Decoupled.scala 363:22]
  assign q_15_io_enq_bits = s7_io_pixel_left_bits; // @[Decoupled.scala 364:21]
  assign q_15_io_deq_ready = f7_io_pixel_in_pixel_ready; // @[BlurFilter.scala 314:12]
  assign q_16_clock = clock;
  assign q_16_reset = reset;
  assign q_16_io_enq_valid = s7_io_pixel_right_valid; // @[Decoupled.scala 363:22]
  assign q_16_io_enq_bits = s7_io_pixel_right_bits; // @[Decoupled.scala 364:21]
  assign q_16_io_deq_ready = f8_io_pixel_in_pixel_ready; // @[BlurFilter.scala 314:12]
  assign q_17_clock = clock;
  assign q_17_reset = reset;
  assign q_17_io_enq_valid = f0_io_pixel_out_valid; // @[Decoupled.scala 363:22]
  assign q_17_io_enq_bits = f0_io_pixel_out_bits; // @[Decoupled.scala 364:21]
  assign q_17_io_deq_ready = compute_io_pixels_in_0_ready; // @[BlurFilter.scala 314:12]
  assign q_18_clock = clock;
  assign q_18_reset = reset;
  assign q_18_io_enq_valid = f1_io_pixel_out_valid; // @[Decoupled.scala 363:22]
  assign q_18_io_enq_bits = f1_io_pixel_out_bits; // @[Decoupled.scala 364:21]
  assign q_18_io_deq_ready = compute_io_pixels_in_1_ready; // @[BlurFilter.scala 314:12]
  assign q_19_clock = clock;
  assign q_19_reset = reset;
  assign q_19_io_enq_valid = f2_io_pixel_out_valid; // @[Decoupled.scala 363:22]
  assign q_19_io_enq_bits = f2_io_pixel_out_bits; // @[Decoupled.scala 364:21]
  assign q_19_io_deq_ready = compute_io_pixels_in_2_ready; // @[BlurFilter.scala 314:12]
  assign q_20_clock = clock;
  assign q_20_reset = reset;
  assign q_20_io_enq_valid = f3_io_pixel_out_valid; // @[Decoupled.scala 363:22]
  assign q_20_io_enq_bits = f3_io_pixel_out_bits; // @[Decoupled.scala 364:21]
  assign q_20_io_deq_ready = compute_io_pixels_in_3_ready; // @[BlurFilter.scala 314:12]
  assign q_21_clock = clock;
  assign q_21_reset = reset;
  assign q_21_io_enq_valid = f4_io_pixel_out_valid; // @[Decoupled.scala 363:22]
  assign q_21_io_enq_bits = f4_io_pixel_out_bits; // @[Decoupled.scala 364:21]
  assign q_21_io_deq_ready = compute_io_pixels_in_4_ready; // @[BlurFilter.scala 314:12]
  assign q_22_clock = clock;
  assign q_22_reset = reset;
  assign q_22_io_enq_valid = f5_io_pixel_out_valid; // @[Decoupled.scala 363:22]
  assign q_22_io_enq_bits = f5_io_pixel_out_bits; // @[Decoupled.scala 364:21]
  assign q_22_io_deq_ready = compute_io_pixels_in_5_ready; // @[BlurFilter.scala 314:12]
  assign q_23_clock = clock;
  assign q_23_reset = reset;
  assign q_23_io_enq_valid = f6_io_pixel_out_valid; // @[Decoupled.scala 363:22]
  assign q_23_io_enq_bits = f6_io_pixel_out_bits; // @[Decoupled.scala 364:21]
  assign q_23_io_deq_ready = compute_io_pixels_in_6_ready; // @[BlurFilter.scala 314:12]
  assign q_24_clock = clock;
  assign q_24_reset = reset;
  assign q_24_io_enq_valid = f7_io_pixel_out_valid; // @[Decoupled.scala 363:22]
  assign q_24_io_enq_bits = f7_io_pixel_out_bits; // @[Decoupled.scala 364:21]
  assign q_24_io_deq_ready = compute_io_pixels_in_7_ready; // @[BlurFilter.scala 314:12]
  assign q_25_clock = clock;
  assign q_25_reset = reset;
  assign q_25_io_enq_valid = f8_io_pixel_out_valid; // @[Decoupled.scala 363:22]
  assign q_25_io_enq_bits = f8_io_pixel_out_bits; // @[Decoupled.scala 364:21]
  assign q_25_io_deq_ready = compute_io_pixels_in_8_ready; // @[BlurFilter.scala 314:12]
  assign fanout_clock = clock;
  assign fanout_reset = reset;
  assign fanout_io_enq_valid = io_in_imgh_valid; // @[BlurFilter.scala 332:22]
  assign fanout_io_enq_bits = io_in_imgh_bits; // @[BlurFilter.scala 331:22]
  assign fanout_io_deq_ready = f0_io_pixel_in_imgh_ready; // @[BlurFilter.scala 336:16]
  assign fanout_1_clock = clock;
  assign fanout_1_reset = reset;
  assign fanout_1_io_enq_valid = io_in_imgh_valid; // @[BlurFilter.scala 332:22]
  assign fanout_1_io_enq_bits = io_in_imgh_bits; // @[BlurFilter.scala 331:22]
  assign fanout_1_io_deq_ready = f1_io_pixel_in_imgh_ready; // @[BlurFilter.scala 336:16]
  assign fanout_2_clock = clock;
  assign fanout_2_reset = reset;
  assign fanout_2_io_enq_valid = io_in_imgh_valid; // @[BlurFilter.scala 332:22]
  assign fanout_2_io_enq_bits = io_in_imgh_bits; // @[BlurFilter.scala 331:22]
  assign fanout_2_io_deq_ready = f2_io_pixel_in_imgh_ready; // @[BlurFilter.scala 336:16]
  assign fanout_3_clock = clock;
  assign fanout_3_reset = reset;
  assign fanout_3_io_enq_valid = io_in_imgh_valid; // @[BlurFilter.scala 332:22]
  assign fanout_3_io_enq_bits = io_in_imgh_bits; // @[BlurFilter.scala 331:22]
  assign fanout_3_io_deq_ready = f3_io_pixel_in_imgh_ready; // @[BlurFilter.scala 336:16]
  assign fanout_4_clock = clock;
  assign fanout_4_reset = reset;
  assign fanout_4_io_enq_valid = io_in_imgh_valid; // @[BlurFilter.scala 332:22]
  assign fanout_4_io_enq_bits = io_in_imgh_bits; // @[BlurFilter.scala 331:22]
  assign fanout_4_io_deq_ready = f4_io_pixel_in_imgh_ready; // @[BlurFilter.scala 336:16]
  assign fanout_5_clock = clock;
  assign fanout_5_reset = reset;
  assign fanout_5_io_enq_valid = io_in_imgh_valid; // @[BlurFilter.scala 332:22]
  assign fanout_5_io_enq_bits = io_in_imgh_bits; // @[BlurFilter.scala 331:22]
  assign fanout_5_io_deq_ready = f5_io_pixel_in_imgh_ready; // @[BlurFilter.scala 336:16]
  assign fanout_6_clock = clock;
  assign fanout_6_reset = reset;
  assign fanout_6_io_enq_valid = io_in_imgh_valid; // @[BlurFilter.scala 332:22]
  assign fanout_6_io_enq_bits = io_in_imgh_bits; // @[BlurFilter.scala 331:22]
  assign fanout_6_io_deq_ready = f6_io_pixel_in_imgh_ready; // @[BlurFilter.scala 336:16]
  assign fanout_7_clock = clock;
  assign fanout_7_reset = reset;
  assign fanout_7_io_enq_valid = io_in_imgh_valid; // @[BlurFilter.scala 332:22]
  assign fanout_7_io_enq_bits = io_in_imgh_bits; // @[BlurFilter.scala 331:22]
  assign fanout_7_io_deq_ready = f7_io_pixel_in_imgh_ready; // @[BlurFilter.scala 336:16]
  assign fanout_8_clock = clock;
  assign fanout_8_reset = reset;
  assign fanout_8_io_enq_valid = io_in_imgh_valid; // @[BlurFilter.scala 332:22]
  assign fanout_8_io_enq_bits = io_in_imgh_bits; // @[BlurFilter.scala 331:22]
  assign fanout_8_io_deq_ready = f8_io_pixel_in_imgh_ready; // @[BlurFilter.scala 336:16]
  assign fanout_9_clock = clock;
  assign fanout_9_reset = reset;
  assign fanout_9_io_enq_valid = io_in_imgw_valid; // @[BlurFilter.scala 332:22]
  assign fanout_9_io_enq_bits = io_in_imgw_bits; // @[BlurFilter.scala 331:22]
  assign fanout_9_io_deq_ready = f0_io_pixel_in_imgw_ready; // @[BlurFilter.scala 336:16]
  assign fanout_10_clock = clock;
  assign fanout_10_reset = reset;
  assign fanout_10_io_enq_valid = io_in_imgw_valid; // @[BlurFilter.scala 332:22]
  assign fanout_10_io_enq_bits = io_in_imgw_bits; // @[BlurFilter.scala 331:22]
  assign fanout_10_io_deq_ready = f1_io_pixel_in_imgw_ready; // @[BlurFilter.scala 336:16]
  assign fanout_11_clock = clock;
  assign fanout_11_reset = reset;
  assign fanout_11_io_enq_valid = io_in_imgw_valid; // @[BlurFilter.scala 332:22]
  assign fanout_11_io_enq_bits = io_in_imgw_bits; // @[BlurFilter.scala 331:22]
  assign fanout_11_io_deq_ready = f2_io_pixel_in_imgw_ready; // @[BlurFilter.scala 336:16]
  assign fanout_12_clock = clock;
  assign fanout_12_reset = reset;
  assign fanout_12_io_enq_valid = io_in_imgw_valid; // @[BlurFilter.scala 332:22]
  assign fanout_12_io_enq_bits = io_in_imgw_bits; // @[BlurFilter.scala 331:22]
  assign fanout_12_io_deq_ready = f3_io_pixel_in_imgw_ready; // @[BlurFilter.scala 336:16]
  assign fanout_13_clock = clock;
  assign fanout_13_reset = reset;
  assign fanout_13_io_enq_valid = io_in_imgw_valid; // @[BlurFilter.scala 332:22]
  assign fanout_13_io_enq_bits = io_in_imgw_bits; // @[BlurFilter.scala 331:22]
  assign fanout_13_io_deq_ready = f4_io_pixel_in_imgw_ready; // @[BlurFilter.scala 336:16]
  assign fanout_14_clock = clock;
  assign fanout_14_reset = reset;
  assign fanout_14_io_enq_valid = io_in_imgw_valid; // @[BlurFilter.scala 332:22]
  assign fanout_14_io_enq_bits = io_in_imgw_bits; // @[BlurFilter.scala 331:22]
  assign fanout_14_io_deq_ready = f5_io_pixel_in_imgw_ready; // @[BlurFilter.scala 336:16]
  assign fanout_15_clock = clock;
  assign fanout_15_reset = reset;
  assign fanout_15_io_enq_valid = io_in_imgw_valid; // @[BlurFilter.scala 332:22]
  assign fanout_15_io_enq_bits = io_in_imgw_bits; // @[BlurFilter.scala 331:22]
  assign fanout_15_io_deq_ready = f6_io_pixel_in_imgw_ready; // @[BlurFilter.scala 336:16]
  assign fanout_16_clock = clock;
  assign fanout_16_reset = reset;
  assign fanout_16_io_enq_valid = io_in_imgw_valid; // @[BlurFilter.scala 332:22]
  assign fanout_16_io_enq_bits = io_in_imgw_bits; // @[BlurFilter.scala 331:22]
  assign fanout_16_io_deq_ready = f7_io_pixel_in_imgw_ready; // @[BlurFilter.scala 336:16]
  assign fanout_17_clock = clock;
  assign fanout_17_reset = reset;
  assign fanout_17_io_enq_valid = io_in_imgw_valid; // @[BlurFilter.scala 332:22]
  assign fanout_17_io_enq_bits = io_in_imgw_bits; // @[BlurFilter.scala 331:22]
  assign fanout_17_io_deq_ready = f8_io_pixel_in_imgw_ready; // @[BlurFilter.scala 336:16]
endmodule
