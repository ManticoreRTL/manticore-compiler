module Main (
    input wire clock
);

  wire clk = clock;
  reg  [31:0] inst_mem[255:0];
  reg  [15:0] cycle_counter = 0;

  always @ (posedge clk) begin
     cycle_counter <= cycle_counter + 1;
     if (cycle_counter == 1990) $finish;
  end

  wire [31:0] inst;
  wire [31:0] addr;
  assign inst = inst_mem[addr];
  wire halted;
  wire reset;
  ResetDriver rdriver (
      .clock(clock),
      .reset(reset)
  );
  // genvar i;
  // for (i = 0; i < 10; i = i + 1) begin

    Mips32 dut (
        .instr (inst),
        .raddr (addr),
        .clock (clock),
        .reset (reset),
        .halted(halted)
    );
  // end



  initial begin
   inst_mem[0] = 6494246;
    inst_mem[1] = 543358986;
    inst_mem[2] = 2164774;
    inst_mem[3] = 4329510;
    inst_mem[4] = 270729219;
    inst_mem[5] = 4263968;
    inst_mem[6] = 539033601;
    inst_mem[7] = 134217732;
    inst_mem[8] = 4194317;
    inst_mem[9] = 0;
    inst_mem[10] = 0;
    inst_mem[11] = 0;
    inst_mem[12] = 0;
  end


endmodule