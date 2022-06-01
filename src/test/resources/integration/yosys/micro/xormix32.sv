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

  reg [31:0] salts [0:31];
  initial begin
    salts[31] = 32'h00af1456;
    salts[30] = 32'h73674eb7;
    salts[29] = 32'he90488a4;
    salts[28] = 32'h965c4507;
    salts[27] = 32'hab0e51e1;
    salts[26] = 32'h618cbc79;
    salts[25] = 32'ha71d489e;
    salts[24] = 32'h51c0c69b;
    salts[23] = 32'h234a07b4;
    salts[22] = 32'h854f0980;
    salts[21] = 32'h59752549;
    salts[20] = 32'hd8449f2a;
    salts[19] = 32'h95f13c50;
    salts[18] = 32'h3afaca18;
    salts[17] = 32'hee211518;
    salts[16] = 32'hfeed780c;
    salts[15] = 32'h352df180;
    salts[14] = 32'h9ceeb1dd;
    salts[13] = 32'hf3fb7189;
    salts[12] = 32'hd6c99ef7;
    salts[11] = 32'hd113a2d8;
    salts[10] = 32'h0fc77197;
    salts[9] = 32'h232b8463;
    salts[8] = 32'h9baafefa;
    salts[7] = 32'h0cef1f8b;
    salts[6] = 32'h990053fe;
    salts[5] = 32'hb9969e83;
    salts[4] = 32'h5fda94c2;
    salts[3] = 32'hcb246290;
    salts[2] = 32'h57f90206;
    salts[1] = 32'h46d9b8ac;
    salts[0] = 32'h198f8d32;
  end


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
      v_mixin = r_state_x ^ salts[i];
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
      v_mixin = r_state_x ^ salts[i];
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

