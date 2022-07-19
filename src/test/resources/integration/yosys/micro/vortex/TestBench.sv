module Main(input wire clock);

  // Testbench parameters.

  // Signals driven by testbench.
  logic reset;

	localparam AXI_DATA_WIDTH = (0 || 0 ? 16 : 64) * 8;
	localparam AXI_ADDR_WIDTH = 32;
	localparam AXI_TID_WIDTH = (1 > (4 > ((1 + $clog2((0 || 0 ? 16 : 64) / 4)) + 48) ? 4 : (1 + $clog2((0 || 0 ? 16 : 64) / 4)) + 48) ? 1 : (4 > ((1 + $clog2((0 || 0 ? 16 : 64) / 4)) + 48) ? 4 : (1 + $clog2((0 || 0 ? 16 : 64) / 4)) + 48)) + 1;
	localparam AXI_STROBE_WIDTH = ((0 || 0 ? 16 : 64) * 8) / 8;
	wire                           vortex_clk;           // input
	wire                           vortex_reset;         // input
	wire [AXI_TID_WIDTH - 1:0]    vortex_m_axi_awid;     // output
	wire [AXI_ADDR_WIDTH - 1:0]   vortex_m_axi_awaddr;   // output
	wire [7:0]                    vortex_m_axi_awlen;    // output
	wire [2:0]                    vortex_m_axi_awsize;   // output
	wire [1:0]                    vortex_m_axi_awburst;  // output
	wire                          vortex_m_axi_awlock;   // output
	wire [3:0]                    vortex_m_axi_awcache;  // output
	wire [2:0]                    vortex_m_axi_awprot;   // output
	wire [3:0]                    vortex_m_axi_awqos;    // output
	wire                          vortex_m_axi_awvalid;  // output
	wire                           vortex_m_axi_awready; // input
	wire [AXI_DATA_WIDTH - 1:0]   vortex_m_axi_wdata;    // output
	wire [AXI_STROBE_WIDTH - 1:0] vortex_m_axi_wstrb;    // output
	wire                          vortex_m_axi_wlast;    // output
	wire                          vortex_m_axi_wvalid;   // output
	wire                           vortex_m_axi_wready;  // input
	wire [AXI_TID_WIDTH - 1:0]     vortex_m_axi_bid;     // input
	wire [1:0]                     vortex_m_axi_bresp;   // input
	wire                           vortex_m_axi_bvalid;  // input
	wire                          vortex_m_axi_bready;   // output
	wire [AXI_TID_WIDTH - 1:0]    vortex_m_axi_arid;     // output
	wire [AXI_ADDR_WIDTH - 1:0]   vortex_m_axi_araddr;   // output
	wire [7:0]                    vortex_m_axi_arlen;    // output
	wire [2:0]                    vortex_m_axi_arsize;   // output
	wire [1:0]                    vortex_m_axi_arburst;  // output
	wire                          vortex_m_axi_arlock;   // output
	wire [3:0]                    vortex_m_axi_arcache;  // output
	wire [2:0]                    vortex_m_axi_arprot;   // output
	wire [3:0]                    vortex_m_axi_arqos;    // output
	wire                          vortex_m_axi_arvalid;  // output
	wire                           vortex_m_axi_arready; // input
	wire [AXI_TID_WIDTH - 1:0]     vortex_m_axi_rid;     // input
	wire [AXI_DATA_WIDTH - 1:0]    vortex_m_axi_rdata;   // input
	wire [1:0]                     vortex_m_axi_rresp;   // input
	wire                           vortex_m_axi_rlast;   // input
	wire                           vortex_m_axi_rvalid;  // input
	wire                          vortex_m_axi_rready;   // output
	wire                          vortex_busy;           // output

  // For AXI4 slave memory.
  localparam G_ADDR_WIDTH = 10; // Intentionally smaller than the address of the Vortex GPU.
  localparam G_DATA_WIDTH = AXI_DATA_WIDTH;
  localparam G_ID_WIDTH = AXI_TID_WIDTH;
  // These are all fixed by the AXI standard.
  localparam C_LENBITS = 8;
  localparam C_SIZEBITS = 3;
  localparam C_BURSTBITS = 2;
  localparam C_LOCKBITS = 2;
  localparam C_CACHEBITS = 4;
  localparam C_PROTBITS = 3;
  localparam C_QOSBITS = 4;
  localparam C_RESPBITS = 2;
  logic                               mem_clock;
  logic                               mem_resetn;
  wire                                mem_s_awready;
  logic                               mem_s_awvalid;
  logic [G_ID_WIDTH - 1 : 0]          mem_s_awid;
  logic [G_ADDR_WIDTH - 1 : 0]        mem_s_awaddr;
  logic [C_LENBITS - 1 : 0]           mem_s_awlen;
  logic [C_SIZEBITS - 1 : 0]          mem_s_awsize;
  logic [C_BURSTBITS - 1 : 0]         mem_s_awburst;
  logic [C_LOCKBITS - 1 : 0]          mem_s_awlock;
  logic [C_CACHEBITS - 1 : 0]         mem_s_awcache;
  logic [C_PROTBITS - 1 : 0]          mem_s_awprot;
  logic [C_QOSBITS - 1 : 0]           mem_s_awqos;
  wire                                mem_s_wready;
  logic                               mem_s_wvalid;
  logic [G_ID_WIDTH - 1 : 0]          mem_s_wid;
  logic [G_DATA_WIDTH - 1 : 0]        mem_s_wdata;
  logic  [(G_DATA_WIDTH / 8) - 1 : 0] mem_s_wstrb;
  logic                               mem_s_wlast;
  logic                               mem_s_bready;
  wire                                mem_s_bvalid;
  wire [G_ID_WIDTH - 1 : 0]           mem_s_bid;
  wire [C_RESPBITS - 1 : 0]           mem_s_bresp;
  wire                                mem_s_arready;
  logic                               mem_s_arvalid;
  logic [G_ID_WIDTH - 1 : 0]          mem_s_arid;
  logic [G_ADDR_WIDTH - 1 : 0]        mem_s_araddr;
  logic [C_LENBITS - 1 : 0]           mem_s_arlen;
  logic [C_SIZEBITS - 1 : 0]          mem_s_arsize;
  logic [C_BURSTBITS - 1 : 0]         mem_s_arburst;
  logic [C_LOCKBITS - 1 : 0]          mem_s_arlock;
  logic [C_CACHEBITS - 1 : 0]         mem_s_arcache;
  logic [C_PROTBITS - 1 : 0]          mem_s_arprot;
  logic [C_QOSBITS - 1 : 0]           mem_s_arqos;
  logic                               mem_s_rready;
  wire                                mem_s_rvalid;
  wire [G_ID_WIDTH - 1 : 0]           mem_s_rid;
  wire [G_DATA_WIDTH - 1 : 0]         mem_s_rdata;
  wire [C_RESPBITS - 1 : 0]           mem_s_rresp;
  wire                                mem_s_rlast;

  Vortex_axi vortex (
    .clk(vortex_clk),
    .reset(vortex_reset),
    .m_axi_awid(vortex_m_axi_awid),
    .m_axi_awaddr(vortex_m_axi_awaddr),
    .m_axi_awlen(vortex_m_axi_awlen),
    .m_axi_awsize(vortex_m_axi_awsize),
    .m_axi_awburst(vortex_m_axi_awburst),
    .m_axi_awlock(vortex_m_axi_awlock),
    .m_axi_awcache(vortex_m_axi_awcache),
    .m_axi_awprot(vortex_m_axi_awprot),
    .m_axi_awqos(vortex_m_axi_awqos),
    .m_axi_awvalid(vortex_m_axi_awvalid),
    .m_axi_awready(vortex_m_axi_awready),
    .m_axi_wdata(vortex_m_axi_wdata),
    .m_axi_wstrb(vortex_m_axi_wstrb),
    .m_axi_wlast(vortex_m_axi_wlast),
    .m_axi_wvalid(vortex_m_axi_wvalid),
    .m_axi_wready(vortex_m_axi_wready),
    .m_axi_bid(vortex_m_axi_bid),
    .m_axi_bresp(vortex_m_axi_bresp),
    .m_axi_bvalid(vortex_m_axi_bvalid),
    .m_axi_bready(vortex_m_axi_bready),
    .m_axi_arid(vortex_m_axi_arid),
    .m_axi_araddr(vortex_m_axi_araddr),
    .m_axi_arlen(vortex_m_axi_arlen),
    .m_axi_arsize(vortex_m_axi_arsize),
    .m_axi_arburst(vortex_m_axi_arburst),
    .m_axi_arlock(vortex_m_axi_arlock),
    .m_axi_arcache(vortex_m_axi_arcache),
    .m_axi_arprot(vortex_m_axi_arprot),
    .m_axi_arqos(vortex_m_axi_arqos),
    .m_axi_arvalid(vortex_m_axi_arvalid),
    .m_axi_arready(vortex_m_axi_arready),
    .m_axi_rid(vortex_m_axi_rid),
    .m_axi_rdata(vortex_m_axi_rdata),
    .m_axi_rresp(vortex_m_axi_rresp),
    .m_axi_rlast(vortex_m_axi_rlast),
    .m_axi_rvalid(vortex_m_axi_rvalid),
    .m_axi_rready(vortex_m_axi_rready),
    .busy(vortex_busy)
  );
  assign vortex_clk = clock;
  assign vortex_reset = reset;
  assign vortex_m_axi_awready = mem_s_awready;
  assign vortex_m_axi_wready = mem_s_wready;
  assign vortex_m_axi_bid = mem_s_bid;
  assign vortex_m_axi_bresp = mem_s_bresp;
  assign vortex_m_axi_bvalid = mem_s_bvalid;
  assign vortex_m_axi_arready = mem_s_arready;
  assign vortex_m_axi_rid = mem_s_rid;
  assign vortex_m_axi_rdata = mem_s_rdata;
  assign vortex_m_axi_rresp = mem_s_rresp;
  assign vortex_m_axi_rlast = mem_s_rlast;
  assign vortex_m_axi_rvalid = mem_s_rvalid;

  axi4_full_slave #(
    .G_ADDR_WIDTH(G_ADDR_WIDTH),
    .G_DATA_WIDTH(G_DATA_WIDTH),
    .G_ID_WIDTH(G_ID_WIDTH),
    .MEM_INIT_FILE("mem.hex")
  ) mem (
    .clock(mem_clock),
    .resetn(mem_resetn),
    .s_awready(mem_s_awready),
    .s_awvalid(mem_s_awvalid),
    .s_awid(mem_s_awid),
    .s_awaddr(mem_s_awaddr),
    .s_awlen(mem_s_awlen),
    .s_awsize(mem_s_awsize),
    .s_awburst(mem_s_awburst),
    .s_awlock(mem_s_awlock),
    .s_awcache(mem_s_awcache),
    .s_awprot(mem_s_awprot),
    .s_awqos(mem_s_awqos),
    .s_wready(mem_s_wready),
    .s_wvalid(mem_s_wvalid),
    .s_wid(mem_s_wid),
    .s_wdata(mem_s_wdata),
    .s_wstrb(mem_s_wstrb),
    .s_wlast(mem_s_wlast),
    .s_bready(mem_s_bready),
    .s_bvalid(mem_s_bvalid),
    .s_bid(mem_s_bid),
    .s_bresp(mem_s_bresp),
    .s_arready(mem_s_arready),
    .s_arvalid(mem_s_arvalid),
    .s_arid(mem_s_arid),
    .s_araddr(mem_s_araddr),
    .s_arlen(mem_s_arlen),
    .s_arsize(mem_s_arsize),
    .s_arburst(mem_s_arburst),
    .s_arlock(mem_s_arlock),
    .s_arcache(mem_s_arcache),
    .s_arprot(mem_s_arprot),
    .s_arqos(mem_s_arqos),
    .s_rready(mem_s_rready),
    .s_rvalid(mem_s_rvalid),
    .s_rid(mem_s_rid),
    .s_rdata(mem_s_rdata),
    .s_rresp(mem_s_rresp),
    .s_rlast(mem_s_rlast)
  );
  assign mem_clock = clock;
  assign mem_resetn = ~reset;
  assign mem_s_awvalid = vortex_m_axi_awvalid;
  assign mem_s_awid = vortex_m_axi_awid;
  assign mem_s_awaddr = vortex_m_axi_awaddr;
  assign mem_s_awlen = vortex_m_axi_awlen;
  assign mem_s_awsize = vortex_m_axi_awsize;
  assign mem_s_awburst = vortex_m_axi_awburst;
  assign mem_s_awlock = vortex_m_axi_awlock;
  assign mem_s_awcache = vortex_m_axi_awcache;
  assign mem_s_awprot = vortex_m_axi_awprot;
  assign mem_s_awqos = vortex_m_axi_awqos;
  assign mem_s_wvalid = vortex_m_axi_wvalid;
  assign mem_s_wid = 0;
  assign mem_s_wdata = vortex_m_axi_wdata;
  assign mem_s_wstrb = vortex_m_axi_wstrb;
  assign mem_s_wlast = vortex_m_axi_wlast;
  assign mem_s_bready = vortex_m_axi_bready;
  assign mem_s_arvalid = vortex_m_axi_arvalid;
  assign mem_s_arid = vortex_m_axi_arid;
  assign mem_s_araddr = vortex_m_axi_araddr;
  assign mem_s_arlen = vortex_m_axi_arlen;
  assign mem_s_arsize = vortex_m_axi_arsize;
  assign mem_s_arburst = vortex_m_axi_arburst;
  assign mem_s_arlock = vortex_m_axi_arlock;
  assign mem_s_arcache = vortex_m_axi_arcache;
  assign mem_s_arprot = vortex_m_axi_arprot;
  assign mem_s_arqos = vortex_m_axi_arqos;
  assign mem_s_rready = vortex_m_axi_rready;

  typedef enum {
    STATE_IDLE = 0,
    STATE_RESET = 1,
    STATE_WAIT_UNTIL_BUSY = 2,
    STATE_WAIT_UNTIL_IDLE = 3,
    STATE_END = 100
  } state_t;

  state_t reg_state = STATE_IDLE, next_state;
  int reg_after_rst_cnt, next_after_rst_cnt;

  // State registers.
  always_ff @(posedge clock) begin
    reg_state <= next_state;
    reg_after_rst_cnt <= next_after_rst_cnt;

    if (reg_state == STATE_END) begin
      $display("finished");
      $finish;
    end
  end

  // Next state logic.
  always_comb begin
    // Default values.
    next_state = reg_state;
    next_after_rst_cnt = reg_after_rst_cnt;
    reset = 0;

    case (reg_state)
      STATE_IDLE:
      begin
        next_state = STATE_RESET;
      end

      STATE_RESET:
      begin
        reset = 1;
        next_after_rst_cnt = 0;
        next_state = STATE_WAIT_UNTIL_BUSY;
      end

      STATE_WAIT_UNTIL_BUSY:
      begin
        if (vortex_busy) begin
          next_state = STATE_WAIT_UNTIL_IDLE;
        end
      end

      STATE_WAIT_UNTIL_IDLE:
      begin
        if (~vortex_busy) begin
          next_state = STATE_END;
        end
      end

      STATE_END:
      begin

      end
    endcase
  end

endmodule
