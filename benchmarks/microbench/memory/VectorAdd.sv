module VectorAdd #(
    DATA_WIDTH = 16,
    ADDR_WIDTH = 13,
    COUNT = 20
) (
    input wire clk,
    input wire rst,
    input wire [ADDR_WIDTH - 1 : 0] a_base,
    input wire [ADDR_WIDTH - 1 : 0] b_base,
    input wire [ADDR_WIDTH - 1 : 0] c_base,
    input wire [DATA_WIDTH - 1 : 0] count,
    input wire start,
    output wire done,
    output wire idle
);


  logic [DATA_WIDTH - 1 : 0] mem[(ADDR_WIDTH << 1) - 1 : 0];

  logic [ADDR_WIDTH - 1 : 0] a_addr;
  logic [ADDR_WIDTH - 1 : 0] b_addr;
  logic [ADDR_WIDTH - 1 : 0] c_addr;
  logic [DATA_WIDTH - 1 : 0] count_r;
  logic idle_r = 1'b1;

  always @(posedge clk) begin
    if (rst) begin
      idle_r <= 1'b1;

    end else if (idle_r == 1'b0) begin
      idle_r <= (count_r == 1);
    end
  end

  assign done = (count_r == 1);
  assign idle = idle_r;
  always @(posedge clk) begin
    if (rst) begin
      count_r <= 0;
    end else if (start && idle_r) begin
      a_addr  <= a_base;
      b_addr  <= b_base;
      c_addr  <= c_base;
      count_r <= count;
    end else if (!idle_r) begin
      if (count_r != 0) begin
        a_addr <= a_addr + 1;
        b_addr <= b_addr + 1;
        c_addr <= c_addr + 1;
      end
      count_r <= count_r - 1;
    end
  end

  always @(posedge clk) begin
    if (count_r != 0) begin
      mem[c_addr] <= mem[a_addr] + mem[b_addr];
    end
  end



endmodule


module Memory #(
    DATA_WIDTH = 16,
    ADDR_WIDTH = 13
) (
    input wire [ADDR_WIDTH - 1 : 0] raddr1,
    input wire [ADDR_WIDTH - 1 : 0] raddr2,
    input wire [ADDR_WIDTH - 1 : 0] waddr,
    input wire [ADDR_WIDTH - 1 : 0] wen,
    output wire [DATA_WIDTH - 1 : 0] dout1,
    output wire [DATA_WIDTH - 1 : 0] dout2,
    input wire [DATA_WIDTH - 1 : 0] din,
    input wire clk
);


  logic [DATA_WIDTH - 1 : 0] mem[(ADDR_WIDTH << 1) - 1 : 0];
  initial begin
    $readmemh("memory.dat", mem);
  end

  always @(posedge clk) begin
    if (wen) mem[waddr] <= din;
  end

  assign dout1 = mem[raddr1];
  assign dout2 = mem[raddr2];

endmodule
module VectorAddTester (
    input wire clk
);

  localparam NUM = 10, RESET_CYCLES = 3;

  localparam ADDR_WIDTH = 13;
  localparam DATA_WIDTH = 16;
  localparam [DATA_WIDTH - 1 : 0] REF_BASE = 0;
  localparam [DATA_WIDTH - 1 : 0] A_BASE = 2 * NUM;
  localparam [DATA_WIDTH - 1 : 0] B_BASE = 3 * NUM;
  localparam [DATA_WIDTH - 1 : 0] C_BASE = NUM;


  logic [DATA_WIDTH - 1 : 0] sum;
  logic wen;
  logic [DATA_WIDTH - 1 : 0] dout1;
  logic [DATA_WIDTH - 1 : 0] dout2;
  logic [ADDR_WIDTH - 1 : 0] raddr1;
  logic [ADDR_WIDTH - 1 : 0] raddr2;
  logic [DATA_WIDTH - 1 : 0] counter = 0;
  logic computing = 1'b1;


  always_comb begin
    waddr = C_BASE + counter;
    sum = dout1 + dout2;
    if (computing == 1'b1) begin
      raddr1 = A_BASE + counter;
      raddr2 = B_BASE + counter;
      wen = 1'b1;
    end else begin
      raddr1 = REF_BASE + counter;
      raddr2 = C_BASE + counter;
      wen = 1'b0;
    end
  end

  always @(posedge clk) begin
    if (computing) begin
      if (counter < NUM) begin
        counter <= counter + 1;
      end else begin
        counter   = 0;
        computing = 1'b0;
      end
    end else begin
      if (counter < NUM) begin
        counter <= counter + 1;
        $masm_expect(dout1 == dout2, "haha");
      end else begin
        $masm_stop;
      end
    end
  end

  Memory #(
      .ADDR_WIDTH(ADDR_WIDTH),
      .DATA_WIDTH(DATA_WIDTH)
  ) mem_inst (
      .clk(clk),
      .din(sum),
      .dout1(dout1),
      .dout2(dout2),
      .raddr1(raddr1),
      .raddr2(raddr2),
      .waddr(waddr),
      .wen(wen)
  );

endmodule
