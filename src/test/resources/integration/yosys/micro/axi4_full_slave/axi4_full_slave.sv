module axi4_full_slave_mem #(
  parameter G_DATA_WIDTH,
  parameter G_ADDR_WIDTH,
  parameter MEM_INIT_FILE = ""
)(
  input                         clk,
  input      [G_DATA_WIDTH-1:0] data,
  input      [G_ADDR_WIDTH-1:0] read_addr,
  input      [G_ADDR_WIDTH-1:0] write_addr,
  input                         we,
  output reg [G_DATA_WIDTH-1:0] q
);

  reg [G_DATA_WIDTH-1:0] ram [2**G_ADDR_WIDTH-1:0];

  always @(posedge clk) begin //WRITE
    if (we) begin
      ram[write_addr] <= data;
    end
  end

  always @(posedge clk) begin //READ
    q <= ram[read_addr];
  end

  initial begin
    if (MEM_INIT_FILE != "") begin
      $readmemh(MEM_INIT_FILE, ram);
    end
  end

endmodule

module axi4_full_slave
#(
  parameter G_ADDR_WIDTH = 6,
  parameter G_DATA_WIDTH = 32,
  parameter G_ID_WIDTH = 2,
  parameter MEM_INIT_FILE = ""
)(
  clock,
  resetn,
  s_awready,
  s_awvalid,
  s_awid,
  s_awaddr,
  s_awlen,
  s_awsize,
  s_awburst,
  s_awlock,
  s_awcache,
  s_awprot,
  s_awqos,
  s_wready,
  s_wvalid,
  s_wid,
  s_wdata,
  s_wstrb,
  s_wlast,
  s_bready,
  s_bvalid,
  s_bid,
  s_bresp,
  s_arready,
  s_arvalid,
  s_arid,
  s_araddr,
  s_arlen,
  s_arsize,
  s_arburst,
  s_arlock,
  s_arcache,
  s_arprot,
  s_arqos,
  s_rready,
  s_rvalid,
  s_rid,
  s_rdata,
  s_rresp,
  s_rlast
);

  input                               clock;
  input                               resetn;

  output logic                        s_awready;
  input                               s_awvalid;
  input [G_ID_WIDTH - 1 : 0]          s_awid;
  input [G_ADDR_WIDTH - 1 : 0]        s_awaddr;
  input [7:0]                         s_awlen;
  input [2:0]                         s_awsize;
  input [1:0]                         s_awburst;
  input [1:0]                         s_awlock;
  input [3:0]                         s_awcache;
  input [2:0]                         s_awprot;
  input [3:0]                         s_awqos;

  output logic                        s_wready;
  input                               s_wvalid;
  input [G_ID_WIDTH - 1 : 0]          s_wid;
  input [G_DATA_WIDTH - 1 : 0]        s_wdata;
  input [(G_DATA_WIDTH / 8) - 1 : 0]  s_wstrb;
  input                               s_wlast;

  input                               s_bready;
  output logic                        s_bvalid;
  output logic [G_ID_WIDTH - 1 : 0]   s_bid;
  output logic [1:0]                  s_bresp;

  output logic                        s_arready;
  input                               s_arvalid;
  input [G_ID_WIDTH - 1 : 0]          s_arid;
  input [G_ADDR_WIDTH - 1 : 0]        s_araddr;
  input [7:0]                         s_arlen;
  input [2:0]                         s_arsize;
  input [1:0]                         s_arburst;
  input [1:0]                         s_arlock;
  input [3:0]                         s_arcache;
  input [2:0]                         s_arprot;
  input [3:0]                         s_arqos;

  input                               s_rready;
  output logic                        s_rvalid;
  output logic [G_ID_WIDTH - 1 : 0]   s_rid;
  output logic [G_DATA_WIDTH - 1 : 0] s_rdata;
  output logic [1:0]                  s_rresp;
  output logic                        s_rlast;

  localparam NUM_BYTES_IN_WORD = G_DATA_WIDTH / 8;
  localparam OFST = $clog2(NUM_BYTES_IN_WORD);
  localparam MEM_ADDR_WIDTH = G_ADDR_WIDTH - OFST;
  localparam MEM_NUM_WORDS = 2 ** MEM_ADDR_WIDTH;

  enum logic [2:0] {
    STATE_WRITE_IDLE = 0,
    STATE_WRITE_ACK_AWREADY = 1,
    STATE_WRITE_WAIT_WVALID = 2,
    STATE_WRITE_BURST = 3,
    STATE_WRITE_SEND_BVALID = 4
  } reg_write_state, next_write_state;

  enum logic [1:0] {
    STATE_READ_IDLE = 0,
    STATE_READ_ACK_ARREADY = 1,
    STATE_READ_BURST = 2
  } reg_read_state, next_read_state;

  logic [MEM_ADDR_WIDTH - 1 : 0] reg_write_addr, next_write_addr;
  logic [G_ID_WIDTH - 1 : 0] reg_write_id, next_write_id;
  logic [7 : 0] reg_write_len, next_write_len;
  logic [7 : 0] reg_write_len_cnt, next_write_len_cnt;

  logic [MEM_ADDR_WIDTH - 1 : 0] reg_read_addr, next_read_addr;
  logic [G_ID_WIDTH - 1 : 0] reg_read_id, next_read_id;
  logic [7 : 0] reg_read_len, next_read_len;
  logic [7 : 0] reg_read_len_cnt, next_read_len_cnt;

  logic mem_we;

  axi4_full_slave_mem #(
    .G_ADDR_WIDTH(MEM_ADDR_WIDTH),
    .G_DATA_WIDTH(G_DATA_WIDTH),
    .MEM_INIT_FILE(MEM_INIT_FILE)
  ) mem (
    .clk(clock),
    .data(s_wdata),
    .read_addr(reg_read_addr),
    .write_addr(reg_write_addr),
    .we(mem_we),
    .q(s_rdata)
  );

  // State registers.
  always_ff @(posedge clock) begin
    if (resetn == 0) begin
      reg_write_state <= STATE_WRITE_IDLE;
      reg_read_state <= STATE_READ_IDLE;
    end else begin
      reg_write_state <= next_write_state;
      reg_read_state <= next_read_state;

      reg_write_addr <= next_write_addr;
      reg_write_id <= next_write_id;
      reg_write_len <= next_write_len;
      reg_write_len_cnt <= next_write_len_cnt;

      reg_read_addr <= next_read_addr;
      reg_read_id <= next_read_id;
      reg_read_len <= next_read_len;
      reg_read_len_cnt <= next_read_len_cnt;
    end
  end

  // Write FSM
  always_comb begin
    s_awready = 0;
    s_wready = 0;
    s_bvalid = 0;
    s_bid = 0;
    s_bresp = 0;

    mem_we = 0;

    next_write_state = reg_write_state;
    next_write_addr = reg_write_addr;
    next_write_id = reg_write_id;
    next_write_len = reg_write_len;
    next_write_len_cnt = reg_write_len_cnt;

    case (reg_write_state)
      STATE_WRITE_IDLE :
      begin
        next_write_addr = s_awaddr[G_ADDR_WIDTH - 1 : OFST];
        next_write_id = s_awid;
        next_write_len = s_awlen;
        next_write_len_cnt = 0;

        // We can only start a write transaction if no read is ongoing (for simplicity).
        if (s_awvalid == 1) begin
          next_write_state = STATE_WRITE_ACK_AWREADY;
        end
      end

      STATE_WRITE_ACK_AWREADY :
      begin
        s_awready = 1;
        next_write_state = STATE_WRITE_WAIT_WVALID;
      end

      STATE_WRITE_WAIT_WVALID :
      begin
        if (s_wvalid == 1) begin
          next_write_state = STATE_WRITE_BURST;
        end
      end

      STATE_WRITE_BURST :
      begin
        s_wready = 1;

        if (s_wvalid == 1) begin
          next_write_len_cnt = reg_write_len_cnt + 1'b1;
          next_write_addr = reg_write_addr + 1'b1;
          mem_we = 1;

          if (reg_write_len_cnt == reg_write_len) begin
            next_write_state = STATE_WRITE_SEND_BVALID;
          end
        end
      end

      STATE_WRITE_SEND_BVALID :
      begin
        s_bvalid = 1;
        s_bid = reg_write_id;

        if (s_bready == 1) begin
          next_write_state = STATE_WRITE_IDLE;
        end
      end

      default :
      begin
      end
    endcase
  end

  // Read FSM
  always_comb begin
    s_arready = 0;
    s_rvalid = 0;
    s_rid = 0;
    s_rresp = 0;
    s_rlast = 0;

    next_read_state = reg_read_state;
    next_read_addr = reg_read_addr;
    next_read_id = reg_read_id;
    next_read_len = reg_read_len;
    next_read_len_cnt = reg_read_len_cnt;

    case (reg_read_state)
      STATE_READ_IDLE:
      begin
        next_read_addr = s_araddr[G_ADDR_WIDTH - 1 : OFST];
        next_read_id = s_arid;
        next_read_len = s_arlen;
        next_read_len_cnt = 0;

        // We can only start a read transaction if no write is ongoing (for simplicity).\
        if (s_arvalid == 1) begin
          next_read_state = STATE_READ_ACK_ARREADY;
        end
      end

      STATE_READ_ACK_ARREADY:
      begin
        s_arready = 1;
        next_read_state = STATE_READ_BURST;
      end

      STATE_READ_BURST:
      begin
        s_rvalid = 1;
        s_rid = reg_read_id;

        if (s_rready == 1) begin
          next_read_len_cnt = reg_read_len_cnt + 1'b1;
          next_read_addr = reg_read_addr + 1'b1;

          if (reg_read_len_cnt >= reg_read_len) begin
            s_rlast = 1;
            next_read_state = STATE_READ_IDLE;
          end
        end
      end

      default :
      begin
      end

    endcase

  end

endmodule
