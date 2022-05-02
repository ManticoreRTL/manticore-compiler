// Copyright (c) 2020-2021 Maarten Baert <info@maartenbaert.be>
// Available under the MIT License - see LICENSE.txt for details.

// This file was generated by `generate_verilog.py`.
// Revision: 1

module xormix32 #(
    parameter streams = 1
) (

    // clock and synchronous reset
    input wire clk,
    input wire rst,

    // configuration
    input wire [31 : 0] seed_x,
    input wire [32 * streams - 1 : 0] seed_y,

    // random number generator
    input wire enable,
    output wire [32 * streams - 1 : 0] result

);

  localparam [32 * 32 - 1 : 0] salts = {
    32'h00af1456,
    32'h73674eb7,
    32'he90488a4,
    32'h965c4507,
    32'hab0e51e1,
    32'h618cbc79,
    32'ha71d489e,
    32'h51c0c69b,
    32'h234a07b4,
    32'h854f0980,
    32'h59752549,
    32'hd8449f2a,
    32'h95f13c50,
    32'h3afaca18,
    32'hee211518,
    32'hfeed780c,
    32'h352df180,
    32'h9ceeb1dd,
    32'hf3fb7189,
    32'hd6c99ef7,
    32'hd113a2d8,
    32'h0fc77197,
    32'h232b8463,
    32'h9baafefa,
    32'h0cef1f8b,
    32'h990053fe,
    32'hb9969e83,
    32'h5fda94c2,
    32'hcb246290,
    32'h57f90206,
    32'h46d9b8ac,
    32'h198f8d32
  };

  reg [31 : 0] r_state_x;
  reg [32 * streams - 1 : 0] r_state_y;

  reg [32 * streams - 1 : 0] v_state_y1;
  reg [32 * streams - 1 : 0] v_state_y2;

  reg [31 : 0] v_mixin;
  reg [31 : 0] v_mixup;
  reg [31 : 0] v_res;

  integer i;

  assign result = r_state_y;

  always @(*) begin

    for (i = 0; i < streams; i = i + 1) begin
      v_mixin = r_state_x ^ salts[32*i+:32];
      v_mixup = r_state_y[32*((i+1)%streams)+:32];
      v_res[ 0] = v_mixup[ 0] ^ (v_mixup[ 6] & ~v_mixup[16]) ^ v_mixup[ 9] ^ v_mixup[15] ^ v_mixin[(i + 15) % 32];
      v_res[ 1] = v_mixup[ 1] ^ (v_mixup[ 7] & ~v_mixup[17]) ^ v_mixup[10] ^ v_mixup[16] ^ v_mixin[(i + 29) % 32];
      v_res[ 2] = v_mixup[ 2] ^ (v_mixup[ 8] & ~v_mixup[18]) ^ v_mixup[11] ^ v_mixup[17] ^ v_mixin[(i +  5) % 32];
      v_res[ 3] = v_mixup[ 3] ^ (v_mixup[ 9] & ~v_mixup[19]) ^ v_mixup[12] ^ v_mixup[18] ^ v_mixin[(i +  0) % 32];
      v_res[ 4] = v_mixup[ 4] ^ (v_mixup[10] & ~v_mixup[20]) ^ v_mixup[13] ^ v_mixup[19] ^ v_mixin[(i + 16) % 32];
      v_res[ 5] = v_mixup[ 5] ^ (v_mixup[11] & ~v_mixup[21]) ^ v_mixup[14] ^ v_mixup[20] ^ v_mixin[(i +  9) % 32];
      v_res[ 6] = v_mixup[ 6] ^ (v_mixup[12] & ~v_mixup[22]) ^ v_mixup[15] ^ v_mixup[21] ^ v_mixin[(i + 26) % 32];
      v_res[ 7] = v_mixup[ 7] ^ (v_mixup[13] & ~v_mixup[23]) ^ v_mixup[16] ^ v_mixup[22] ^ v_mixin[(i + 14) % 32];
      v_res[ 8] = v_mixup[ 8] ^ (v_mixup[14] & ~v_mixup[24]) ^ v_mixup[17] ^ v_mixup[23] ^ v_mixin[(i + 13) % 32];
      v_res[ 9] = v_mixup[ 9] ^ (v_mixup[15] & ~v_mixup[25]) ^ v_mixup[18] ^ v_mixup[24] ^ v_mixin[(i + 10) % 32];
      v_res[10] = v_mixup[10] ^ (v_mixup[16] & ~v_mixup[26]) ^ v_mixup[19] ^ v_mixup[25] ^ v_mixin[(i + 19) % 32];
      v_res[11] = v_mixup[11] ^ (v_mixup[17] & ~v_mixup[27]) ^ v_mixup[20] ^ v_mixup[26] ^ v_mixin[(i + 11) % 32];
      v_res[12] = v_mixup[12] ^ (v_mixup[18] & ~v_mixup[28]) ^ v_mixup[21] ^ v_mixup[27] ^ v_mixin[(i +  2) % 32];
      v_res[13] = v_mixup[13] ^ (v_mixup[19] & ~v_mixup[29]) ^ v_mixup[22] ^ v_mixup[28] ^ v_mixin[(i +  6) % 32];
      v_res[14] = v_mixup[14] ^ (v_mixup[20] & ~v_mixup[30]) ^ v_mixup[23] ^ v_mixup[29] ^ v_mixin[(i +  8) % 32];
      v_res[15] = v_mixup[15] ^ (v_mixup[21] & ~v_mixup[31]) ^ v_mixup[24] ^ v_mixup[30] ^ v_mixin[(i + 17) % 32];
      v_state_y1[32*i+:32] = {v_res, r_state_y[32*i+16+:16]};
    end

    for (i = 0; i < streams; i = i + 1) begin
      v_mixin = r_state_x ^ salts[32*i+:32];
      v_mixup = v_state_y1[32*((i+1)%streams)+:32];
      v_res[ 0] = v_mixup[ 0] ^ (v_mixup[ 6] & ~v_mixup[16]) ^ v_mixup[ 9] ^ v_mixup[15] ^ v_mixin[(i + 20) % 32];
      v_res[ 1] = v_mixup[ 1] ^ (v_mixup[ 7] & ~v_mixup[17]) ^ v_mixup[10] ^ v_mixup[16] ^ v_mixin[(i +  4) % 32];
      v_res[ 2] = v_mixup[ 2] ^ (v_mixup[ 8] & ~v_mixup[18]) ^ v_mixup[11] ^ v_mixup[17] ^ v_mixin[(i + 22) % 32];
      v_res[ 3] = v_mixup[ 3] ^ (v_mixup[ 9] & ~v_mixup[19]) ^ v_mixup[12] ^ v_mixup[18] ^ v_mixin[(i + 30) % 32];
      v_res[ 4] = v_mixup[ 4] ^ (v_mixup[10] & ~v_mixup[20]) ^ v_mixup[13] ^ v_mixup[19] ^ v_mixin[(i + 31) % 32];
      v_res[ 5] = v_mixup[ 5] ^ (v_mixup[11] & ~v_mixup[21]) ^ v_mixup[14] ^ v_mixup[20] ^ v_mixin[(i + 21) % 32];
      v_res[ 6] = v_mixup[ 6] ^ (v_mixup[12] & ~v_mixup[22]) ^ v_mixup[15] ^ v_mixup[21] ^ v_mixin[(i + 24) % 32];
      v_res[ 7] = v_mixup[ 7] ^ (v_mixup[13] & ~v_mixup[23]) ^ v_mixup[16] ^ v_mixup[22] ^ v_mixin[(i + 25) % 32];
      v_res[ 8] = v_mixup[ 8] ^ (v_mixup[14] & ~v_mixup[24]) ^ v_mixup[17] ^ v_mixup[23] ^ v_mixin[(i + 18) % 32];
      v_res[ 9] = v_mixup[ 9] ^ (v_mixup[15] & ~v_mixup[25]) ^ v_mixup[18] ^ v_mixup[24] ^ v_mixin[(i + 27) % 32];
      v_res[10] = v_mixup[10] ^ (v_mixup[16] & ~v_mixup[26]) ^ v_mixup[19] ^ v_mixup[25] ^ v_mixin[(i + 28) % 32];
      v_res[11] = v_mixup[11] ^ (v_mixup[17] & ~v_mixup[27]) ^ v_mixup[20] ^ v_mixup[26] ^ v_mixin[(i + 23) % 32];
      v_res[12] = v_mixup[12] ^ (v_mixup[18] & ~v_mixup[28]) ^ v_mixup[21] ^ v_mixup[27] ^ v_mixin[(i + 12) % 32];
      v_res[13] = v_mixup[13] ^ (v_mixup[19] & ~v_mixup[29]) ^ v_mixup[22] ^ v_mixup[28] ^ v_mixin[(i +  7) % 32];
      v_res[14] = v_mixup[14] ^ (v_mixup[20] & ~v_mixup[30]) ^ v_mixup[23] ^ v_mixup[29] ^ v_mixin[(i +  1) % 32];
      v_res[15] = v_mixup[15] ^ (v_mixup[21] & ~v_mixup[31]) ^ v_mixup[24] ^ v_mixup[30] ^ v_mixin[(i +  3) % 32];
      v_state_y2[32*i+:32] = {v_res, v_state_y1[32*i+16+:16]};
    end

  end

  always @(posedge clk) begin
    if (rst == 1'b1) begin

      r_state_x <= seed_x;
      r_state_y <= seed_y;

    end else if (enable == 1'b1) begin

      r_state_x[0] <= r_state_x[11] ^ r_state_x[24] ^ r_state_x[22] ^ r_state_x[3] ^ r_state_x[19];
      r_state_x[ 1] <= r_state_x[25] ^ r_state_x[ 7] ^ r_state_x[20] ^ r_state_x[ 2] ^ r_state_x[26] ^ r_state_x[28];
      r_state_x[2] <= r_state_x[8] ^ r_state_x[5] ^ r_state_x[18] ^ r_state_x[24] ^ r_state_x[4];
      r_state_x[ 3] <= r_state_x[ 8] ^ r_state_x[22] ^ r_state_x[26] ^ r_state_x[ 7] ^ r_state_x[21] ^ r_state_x[14];
      r_state_x[4] <= r_state_x[30] ^ r_state_x[26] ^ r_state_x[25] ^ r_state_x[14] ^ r_state_x[24];
      r_state_x[ 5] <= r_state_x[21] ^ r_state_x[10] ^ r_state_x[16] ^ r_state_x[13] ^ r_state_x[ 5] ^ r_state_x[17];
      r_state_x[6] <= r_state_x[14] ^ r_state_x[29] ^ r_state_x[24] ^ r_state_x[11] ^ r_state_x[25];
      r_state_x[ 7] <= r_state_x[ 5] ^ r_state_x[26] ^ r_state_x[31] ^ r_state_x[22] ^ r_state_x[27] ^ r_state_x[ 7];
      r_state_x[8] <= r_state_x[0] ^ r_state_x[17] ^ r_state_x[1] ^ r_state_x[18] ^ r_state_x[8];
      r_state_x[ 9] <= r_state_x[29] ^ r_state_x[ 0] ^ r_state_x[21] ^ r_state_x[26] ^ r_state_x[ 3] ^ r_state_x[13];
      r_state_x[10] <= r_state_x[23] ^ r_state_x[29] ^ r_state_x[19] ^ r_state_x[21] ^ r_state_x[10];
      r_state_x[11] <= r_state_x[19] ^ r_state_x[20] ^ r_state_x[ 4] ^ r_state_x[18] ^ r_state_x[15] ^ r_state_x[10];
      r_state_x[12] <= r_state_x[28] ^ r_state_x[29] ^ r_state_x[24] ^ r_state_x[19] ^ r_state_x[4];
      r_state_x[13] <= r_state_x[19] ^ r_state_x[ 6] ^ r_state_x[27] ^ r_state_x[12] ^ r_state_x[11] ^ r_state_x[ 7];
      r_state_x[14] <= r_state_x[1] ^ r_state_x[5] ^ r_state_x[3] ^ r_state_x[30] ^ r_state_x[25];
      r_state_x[15] <= r_state_x[22] ^ r_state_x[12] ^ r_state_x[11] ^ r_state_x[ 7] ^ r_state_x[28] ^ r_state_x[ 1];
      r_state_x[16] <= r_state_x[16] ^ r_state_x[5] ^ r_state_x[29] ^ r_state_x[2] ^ r_state_x[14];
      r_state_x[17] <= r_state_x[ 8] ^ r_state_x[24] ^ r_state_x[ 0] ^ r_state_x[23] ^ r_state_x[31] ^ r_state_x[26];
      r_state_x[18] <= r_state_x[15] ^ r_state_x[17] ^ r_state_x[4] ^ r_state_x[9] ^ r_state_x[6];
      r_state_x[19] <= r_state_x[30] ^ r_state_x[ 9] ^ r_state_x[18] ^ r_state_x[ 2] ^ r_state_x[11] ^ r_state_x[ 6];
      r_state_x[20] <= r_state_x[2] ^ r_state_x[27] ^ r_state_x[15] ^ r_state_x[12] ^ r_state_x[20];
      r_state_x[21] <= r_state_x[21] ^ r_state_x[20] ^ r_state_x[10] ^ r_state_x[ 6] ^ r_state_x[31] ^ r_state_x[ 1];
      r_state_x[22] <= r_state_x[9] ^ r_state_x[29] ^ r_state_x[15] ^ r_state_x[27] ^ r_state_x[16];
      r_state_x[23] <= r_state_x[29] ^ r_state_x[10] ^ r_state_x[31] ^ r_state_x[30] ^ r_state_x[13] ^ r_state_x[ 3];
      r_state_x[24] <= r_state_x[31] ^ r_state_x[23] ^ r_state_x[6] ^ r_state_x[24] ^ r_state_x[17];
      r_state_x[25] <= r_state_x[ 4] ^ r_state_x[ 8] ^ r_state_x[ 6] ^ r_state_x[19] ^ r_state_x[16] ^ r_state_x[ 9];
      r_state_x[26] <= r_state_x[23] ^ r_state_x[22] ^ r_state_x[15] ^ r_state_x[28] ^ r_state_x[6];
      r_state_x[27] <= r_state_x[30] ^ r_state_x[ 9] ^ r_state_x[10] ^ r_state_x[28] ^ r_state_x[18] ^ r_state_x[15];
      r_state_x[28] <= r_state_x[25] ^ r_state_x[20] ^ r_state_x[19] ^ r_state_x[12] ^ r_state_x[28];
      r_state_x[29] <= r_state_x[13] ^ r_state_x[10] ^ r_state_x[ 9] ^ r_state_x[ 8] ^ r_state_x[ 0] ^ r_state_x[14];
      r_state_x[30] <= r_state_x[22] ^ r_state_x[27] ^ r_state_x[3] ^ r_state_x[13] ^ r_state_x[23];
      r_state_x[31] <= r_state_x[12] ^ r_state_x[ 2] ^ r_state_x[16] ^ r_state_x[ 1] ^ r_state_x[17] ^ r_state_x[23];

      r_state_y <= v_state_y2;

    end
  end

endmodule



module xormix32_tb (
    input wire clock
);

  // configuration
  localparam STREAMS = 4;
  localparam NUM = 100;
  localparam [31 : 0] seed_x = 32'hdf2c403b;
  localparam [32 * STREAMS - 1 : 0] seed_y = 128'ha9140006e47066dd25e5a545abac0809;

  // reference result
  reg [127:0] ref_result [0 : NUM - 1];

  initial begin
      $readmemh("ref.hex", ref_result);
  end


  // DUT signals
  wire rst;
  wire enable;
  wire [32 * STREAMS - 1 : 0] result;
  wire [32 * STREAMS - 1 : 0] expected;

  // error counter
  //   reg [15 : 0] errors = 0;

  // DUT
  xormix32 #(
      .streams(STREAMS)
  ) inst_xormix (
      .clk(clock),
      .rst(rst),
      .seed_x(seed_x),
      .seed_y(seed_y),
      .enable(enable),
      .result(result)
  );

  reg [ 2 : 0] rst_counter = 0;
  reg [15 : 0] ictr;

  assign expected = ref_result[ictr];
  assign rst = (rst_counter < 2);
  assign enable = (rst_counter == 3);

  always @(posedge clock) begin
    if (rst_counter < 3) begin
      rst_counter <= rst_counter + 1;
      ictr <= 0;
    end else if (rst_counter == 3) begin
      ictr <= ictr + 1;
      if (ictr < NUM) begin
        if (result != expected) begin
`ifdef VERILATOR
          $error("Invalid %dth result %d != %d", ictr, result, expected);
`else
          $masm_expect(0, "Invalid result!");
`endif
        end
      end else begin
`ifdef VERILATOR
        $finish;
`else
        $masm_stop;
`endif

      end
    end
  end


endmodule

