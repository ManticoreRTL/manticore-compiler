module Main(input wire clk);

  // Memory parameters.
  localparam G_ADDR_WIDTH = 10;
  localparam G_DATA_WIDTH = 64;
  localparam G_ID_WIDTH = 6;

  logic reset = 0;

  // Instructions.
  localparam C_INSTR_WIDTH = 64;
  localparam C_INSTRS_DEPTH = 2894;
  logic [C_INSTR_WIDTH - 1 : 0] instrs [0 : C_INSTRS_DEPTH - 1];
  initial begin
    $readmemh("instrs.hex", instrs);
  end

  // AXI stream master (that will send instructions to DUT).
  logic [G_DATA_WIDTH - 1 : 0] tdata;
  logic                        tvalid;
  wire                         tready;
  logic                        tlast;

  // DUT signals.
  logic         dut_clock;
  logic         dut_reset;
  logic  [63:0] dut_instruction_tdata;
  logic         dut_instruction_tvalid;
  wire          dut_instruction_tready;
  logic         dut_instruction_tlast;
  logic         dut_m_axi_dram0_awready;
  wire          dut_m_axi_dram0_awvalid;
  wire [5:0]    dut_m_axi_dram0_awid;
  wire [31:0]   dut_m_axi_dram0_awaddr;
  wire [7:0]    dut_m_axi_dram0_awlen;
  wire [2:0]    dut_m_axi_dram0_awsize;
  wire [1:0]    dut_m_axi_dram0_awburst;
  wire [1:0]    dut_m_axi_dram0_awlock;
  wire [3:0]    dut_m_axi_dram0_awcache;
  wire [2:0]    dut_m_axi_dram0_awprot;
  wire [3:0]    dut_m_axi_dram0_awqos;
  logic         dut_m_axi_dram0_wready;
  wire          dut_m_axi_dram0_wvalid;
  wire [5:0]    dut_m_axi_dram0_wid;
  wire [63:0]   dut_m_axi_dram0_wdata;
  wire [7:0]    dut_m_axi_dram0_wstrb;
  wire          dut_m_axi_dram0_wlast;
  wire          dut_m_axi_dram0_bready;
  logic         dut_m_axi_dram0_bvalid;
  logic  [5:0]  dut_m_axi_dram0_bid;
  logic  [1:0]  dut_m_axi_dram0_bresp;
  logic         dut_m_axi_dram0_arready;
  wire          dut_m_axi_dram0_arvalid;
  wire [5:0]    dut_m_axi_dram0_arid;
  wire [31:0]   dut_m_axi_dram0_araddr;
  wire [7:0]    dut_m_axi_dram0_arlen;
  wire [2:0]    dut_m_axi_dram0_arsize;
  wire [1:0]    dut_m_axi_dram0_arburst;
  wire [1:0]    dut_m_axi_dram0_arlock;
  wire [3:0]    dut_m_axi_dram0_arcache;
  wire [2:0]    dut_m_axi_dram0_arprot;
  wire [3:0]    dut_m_axi_dram0_arqos;
  wire          dut_m_axi_dram0_rready;
  logic         dut_m_axi_dram0_rvalid;
  logic  [5:0]  dut_m_axi_dram0_rid;
  logic  [63:0] dut_m_axi_dram0_rdata;
  logic  [1:0]  dut_m_axi_dram0_rresp;
  logic         dut_m_axi_dram0_rlast;
  logic         dut_m_axi_dram1_awready;
  wire          dut_m_axi_dram1_awvalid;
  wire [5:0]    dut_m_axi_dram1_awid;
  wire [31:0]   dut_m_axi_dram1_awaddr;
  wire [7:0]    dut_m_axi_dram1_awlen;
  wire [2:0]    dut_m_axi_dram1_awsize;
  wire [1:0]    dut_m_axi_dram1_awburst;
  wire [1:0]    dut_m_axi_dram1_awlock;
  wire [3:0]    dut_m_axi_dram1_awcache;
  wire [2:0]    dut_m_axi_dram1_awprot;
  wire [3:0]    dut_m_axi_dram1_awqos;
  logic         dut_m_axi_dram1_wready;
  wire          dut_m_axi_dram1_wvalid;
  wire [5:0]    dut_m_axi_dram1_wid;
  wire [63:0]   dut_m_axi_dram1_wdata;
  wire [7:0]    dut_m_axi_dram1_wstrb;
  wire          dut_m_axi_dram1_wlast;
  wire          dut_m_axi_dram1_bready;
  logic         dut_m_axi_dram1_bvalid;
  logic  [5:0]  dut_m_axi_dram1_bid;
  logic  [1:0]  dut_m_axi_dram1_bresp;
  logic         dut_m_axi_dram1_arready;
  wire          dut_m_axi_dram1_arvalid;
  wire [5:0]    dut_m_axi_dram1_arid;
  wire [31:0]   dut_m_axi_dram1_araddr;
  wire [7:0]    dut_m_axi_dram1_arlen;
  wire [2:0]    dut_m_axi_dram1_arsize;
  wire [1:0]    dut_m_axi_dram1_arburst;
  wire [1:0]    dut_m_axi_dram1_arlock;
  wire [3:0]    dut_m_axi_dram1_arcache;
  wire [2:0]    dut_m_axi_dram1_arprot;
  wire [3:0]    dut_m_axi_dram1_arqos;
  wire          dut_m_axi_dram1_rready;
  logic         dut_m_axi_dram1_rvalid;
  logic  [5:0]  dut_m_axi_dram1_rid;
  logic  [63:0] dut_m_axi_dram1_rdata;
  logic  [1:0]  dut_m_axi_dram1_rresp;
  logic         dut_m_axi_dram1_rlast;

  // dram0 slave
  logic                               dram0_clock;
  logic                               dram0_resetn;
  wire                                dram0_s_awready;
  logic                               dram0_s_awvalid;
  logic [G_ID_WIDTH - 1 : 0]          dram0_s_awid;
  logic [G_ADDR_WIDTH - 1 : 0]        dram0_s_awaddr;
  logic [7:0]                         dram0_s_awlen;
  logic [2:0]                         dram0_s_awsize;
  logic [1:0]                         dram0_s_awburst;
  logic [1:0]                         dram0_s_awlock;
  logic [3:0]                         dram0_s_awcache;
  logic [2:0]                         dram0_s_awprot;
  logic [3:0]                         dram0_s_awqos;
  wire                                dram0_s_wready;
  logic                               dram0_s_wvalid;
  logic [G_ID_WIDTH - 1 : 0]          dram0_s_wid;
  logic [G_DATA_WIDTH - 1 : 0]        dram0_s_wdata;
  logic  [(G_DATA_WIDTH / 8) - 1 : 0] dram0_s_wstrb;
  logic                               dram0_s_wlast;
  logic                               dram0_s_bready;
  wire                                dram0_s_bvalid;
  wire [G_ID_WIDTH - 1 : 0]           dram0_s_bid;
  wire [1:0]                          dram0_s_bresp;
  wire                                dram0_s_arready;
  logic                               dram0_s_arvalid;
  logic [G_ID_WIDTH - 1 : 0]          dram0_s_arid;
  logic [G_ADDR_WIDTH - 1 : 0]        dram0_s_araddr;
  logic [7:0]                         dram0_s_arlen;
  logic [2:0]                         dram0_s_arsize;
  logic [1:0]                         dram0_s_arburst;
  logic [1:0]                         dram0_s_arlock;
  logic [3:0]                         dram0_s_arcache;
  logic [2:0]                         dram0_s_arprot;
  logic [3:0]                         dram0_s_arqos;
  logic                               dram0_s_rready;
  wire                                dram0_s_rvalid;
  wire [G_ID_WIDTH - 1 : 0]           dram0_s_rid;
  wire [G_DATA_WIDTH - 1 : 0]         dram0_s_rdata;
  wire [1:0]                          dram0_s_rresp;
  wire                                dram0_s_rlast;

  // dram1 slave
  logic                               dram1_clock;
  logic                               dram1_resetn;
  wire                                dram1_s_awready;
  logic                               dram1_s_awvalid;
  logic [G_ID_WIDTH - 1 : 0]          dram1_s_awid;
  logic [G_ADDR_WIDTH - 1 : 0]        dram1_s_awaddr;
  logic [7:0]                         dram1_s_awlen;
  logic [2:0]                         dram1_s_awsize;
  logic [1:0]                         dram1_s_awburst;
  logic [1:0]                         dram1_s_awlock;
  logic [3:0]                         dram1_s_awcache;
  logic [2:0]                         dram1_s_awprot;
  logic [3:0]                         dram1_s_awqos;
  wire                                dram1_s_wready;
  logic                               dram1_s_wvalid;
  logic [G_ID_WIDTH - 1 : 0]          dram1_s_wid;
  logic [G_DATA_WIDTH - 1 : 0]        dram1_s_wdata;
  logic  [(G_DATA_WIDTH / 8) - 1 : 0] dram1_s_wstrb;
  logic                               dram1_s_wlast;
  logic                               dram1_s_bready;
  wire                                dram1_s_bvalid;
  wire [G_ID_WIDTH - 1 : 0]           dram1_s_bid;
  wire [1:0]                          dram1_s_bresp;
  wire                                dram1_s_arready;
  logic                               dram1_s_arvalid;
  logic [G_ID_WIDTH - 1 : 0]          dram1_s_arid;
  logic [G_ADDR_WIDTH - 1 : 0]        dram1_s_araddr;
  logic [7:0]                         dram1_s_arlen;
  logic [2:0]                         dram1_s_arsize;
  logic [1:0]                         dram1_s_arburst;
  logic [1:0]                         dram1_s_arlock;
  logic [3:0]                         dram1_s_arcache;
  logic [2:0]                         dram1_s_arprot;
  logic [3:0]                         dram1_s_arqos;
  logic                               dram1_s_rready;
  wire                                dram1_s_rvalid;
  wire [G_ID_WIDTH - 1 : 0]           dram1_s_rid;
  wire [G_DATA_WIDTH - 1 : 0]         dram1_s_rdata;
  wire [1:0]                          dram1_s_rresp;
  wire                                dram1_s_rlast;

  top_fp16bp8_local_256_acc_64 dut (
    .clock(dut_clock),
    .reset(dut_reset),
    .instruction_tdata(dut_instruction_tdata),
    .instruction_tvalid(dut_instruction_tvalid),
    .instruction_tready(dut_instruction_tready),
    .instruction_tlast(dut_instruction_tlast),
    .m_axi_dram0_awready(dut_m_axi_dram0_awready),
    .m_axi_dram0_awvalid(dut_m_axi_dram0_awvalid),
    .m_axi_dram0_awid(dut_m_axi_dram0_awid),
    .m_axi_dram0_awaddr(dut_m_axi_dram0_awaddr),
    .m_axi_dram0_awlen(dut_m_axi_dram0_awlen),
    .m_axi_dram0_awsize(dut_m_axi_dram0_awsize),
    .m_axi_dram0_awburst(dut_m_axi_dram0_awburst),
    .m_axi_dram0_awlock(dut_m_axi_dram0_awlock),
    .m_axi_dram0_awcache(dut_m_axi_dram0_awcache),
    .m_axi_dram0_awprot(dut_m_axi_dram0_awprot),
    .m_axi_dram0_awqos(dut_m_axi_dram0_awqos),
    .m_axi_dram0_wready(dut_m_axi_dram0_wready),
    .m_axi_dram0_wvalid(dut_m_axi_dram0_wvalid),
    .m_axi_dram0_wid(dut_m_axi_dram0_wid),
    .m_axi_dram0_wdata(dut_m_axi_dram0_wdata),
    .m_axi_dram0_wstrb(dut_m_axi_dram0_wstrb),
    .m_axi_dram0_wlast(dut_m_axi_dram0_wlast),
    .m_axi_dram0_bready(dut_m_axi_dram0_bready),
    .m_axi_dram0_bvalid(dut_m_axi_dram0_bvalid),
    .m_axi_dram0_bid(dut_m_axi_dram0_bid),
    .m_axi_dram0_bresp(dut_m_axi_dram0_bresp),
    .m_axi_dram0_arready(dut_m_axi_dram0_arready),
    .m_axi_dram0_arvalid(dut_m_axi_dram0_arvalid),
    .m_axi_dram0_arid(dut_m_axi_dram0_arid),
    .m_axi_dram0_araddr(dut_m_axi_dram0_araddr),
    .m_axi_dram0_arlen(dut_m_axi_dram0_arlen),
    .m_axi_dram0_arsize(dut_m_axi_dram0_arsize),
    .m_axi_dram0_arburst(dut_m_axi_dram0_arburst),
    .m_axi_dram0_arlock(dut_m_axi_dram0_arlock),
    .m_axi_dram0_arcache(dut_m_axi_dram0_arcache),
    .m_axi_dram0_arprot(dut_m_axi_dram0_arprot),
    .m_axi_dram0_arqos(dut_m_axi_dram0_arqos),
    .m_axi_dram0_rready(dut_m_axi_dram0_rready),
    .m_axi_dram0_rvalid(dut_m_axi_dram0_rvalid),
    .m_axi_dram0_rid(dut_m_axi_dram0_rid),
    .m_axi_dram0_rdata(dut_m_axi_dram0_rdata),
    .m_axi_dram0_rresp(dut_m_axi_dram0_rresp),
    .m_axi_dram0_rlast(dut_m_axi_dram0_rlast),
    .m_axi_dram1_awready(dut_m_axi_dram1_awready),
    .m_axi_dram1_awvalid(dut_m_axi_dram1_awvalid),
    .m_axi_dram1_awid(dut_m_axi_dram1_awid),
    .m_axi_dram1_awaddr(dut_m_axi_dram1_awaddr),
    .m_axi_dram1_awlen(dut_m_axi_dram1_awlen),
    .m_axi_dram1_awsize(dut_m_axi_dram1_awsize),
    .m_axi_dram1_awburst(dut_m_axi_dram1_awburst),
    .m_axi_dram1_awlock(dut_m_axi_dram1_awlock),
    .m_axi_dram1_awcache(dut_m_axi_dram1_awcache),
    .m_axi_dram1_awprot(dut_m_axi_dram1_awprot),
    .m_axi_dram1_awqos(dut_m_axi_dram1_awqos),
    .m_axi_dram1_wready(dut_m_axi_dram1_wready),
    .m_axi_dram1_wvalid(dut_m_axi_dram1_wvalid),
    .m_axi_dram1_wid(dut_m_axi_dram1_wid),
    .m_axi_dram1_wdata(dut_m_axi_dram1_wdata),
    .m_axi_dram1_wstrb(dut_m_axi_dram1_wstrb),
    .m_axi_dram1_wlast(dut_m_axi_dram1_wlast),
    .m_axi_dram1_bready(dut_m_axi_dram1_bready),
    .m_axi_dram1_bvalid(dut_m_axi_dram1_bvalid),
    .m_axi_dram1_bid(dut_m_axi_dram1_bid),
    .m_axi_dram1_bresp(dut_m_axi_dram1_bresp),
    .m_axi_dram1_arready(dut_m_axi_dram1_arready),
    .m_axi_dram1_arvalid(dut_m_axi_dram1_arvalid),
    .m_axi_dram1_arid(dut_m_axi_dram1_arid),
    .m_axi_dram1_araddr(dut_m_axi_dram1_araddr),
    .m_axi_dram1_arlen(dut_m_axi_dram1_arlen),
    .m_axi_dram1_arsize(dut_m_axi_dram1_arsize),
    .m_axi_dram1_arburst(dut_m_axi_dram1_arburst),
    .m_axi_dram1_arlock(dut_m_axi_dram1_arlock),
    .m_axi_dram1_arcache(dut_m_axi_dram1_arcache),
    .m_axi_dram1_arprot(dut_m_axi_dram1_arprot),
    .m_axi_dram1_arqos(dut_m_axi_dram1_arqos),
    .m_axi_dram1_rready(dut_m_axi_dram1_rready),
    .m_axi_dram1_rvalid(dut_m_axi_dram1_rvalid),
    .m_axi_dram1_rid(dut_m_axi_dram1_rid),
    .m_axi_dram1_rdata(dut_m_axi_dram1_rdata),
    .m_axi_dram1_rresp(dut_m_axi_dram1_rresp),
    .m_axi_dram1_rlast(dut_m_axi_dram1_rlast)
  );
  // DUT inputs.
  assign dut_clock = clk;
  assign dut_reset = ~reset; // The reset is active-low for synthesis, even if the name conveys active-high! Check Top.scala in the Tensil sources to see this.
  assign dut_instruction_tdata = tdata;
  assign dut_instruction_tvalid = tvalid;
  assign dut_instruction_tlast = tlast;
  assign dut_m_axi_dram0_awready = dram0_s_awready;
  assign dut_m_axi_dram0_wready = dram0_s_wready;
  assign dut_m_axi_dram0_bvalid = dram0_s_bvalid;
  assign dut_m_axi_dram0_bid = dram0_s_bid;
  assign dut_m_axi_dram0_bresp = dram0_s_bresp;
  assign dut_m_axi_dram0_arready = dram0_s_arready;
  assign dut_m_axi_dram0_rvalid = dram0_s_rvalid;
  assign dut_m_axi_dram0_rid = dram0_s_rid;
  assign dut_m_axi_dram0_rdata = dram0_s_rdata;
  assign dut_m_axi_dram0_rresp = dram0_s_rresp;
  assign dut_m_axi_dram0_rlast = dram0_s_rlast;
  assign dut_m_axi_dram1_awready = dram1_s_awready;
  assign dut_m_axi_dram1_wready = dram1_s_wready;
  assign dut_m_axi_dram1_bvalid = dram1_s_bvalid;
  assign dut_m_axi_dram1_bid = dram1_s_bid;
  assign dut_m_axi_dram1_bresp = dram1_s_bresp;
  assign dut_m_axi_dram1_arready = dram1_s_arready;
  assign dut_m_axi_dram1_rvalid = dram1_s_rvalid;
  assign dut_m_axi_dram1_rid = dram1_s_rid;
  assign dut_m_axi_dram1_rdata = dram1_s_rdata;
  assign dut_m_axi_dram1_rresp = dram1_s_rresp;
  assign dut_m_axi_dram1_rlast = dram1_s_rlast;

  axi4_full_slave #(
    .G_ADDR_WIDTH(G_ADDR_WIDTH),
    .G_DATA_WIDTH(G_DATA_WIDTH),
    .G_ID_WIDTH(G_ID_WIDTH)
  ) dram0 (
    .clock(dram0_clock),
    .resetn(dram0_resetn),
    .s_awready(dram0_s_awready),
    .s_awvalid(dram0_s_awvalid),
    .s_awid(dram0_s_awid),
    .s_awaddr(dram0_s_awaddr),
    .s_awlen(dram0_s_awlen),
    .s_awsize(dram0_s_awsize),
    .s_awburst(dram0_s_awburst),
    .s_awlock(dram0_s_awlock),
    .s_awcache(dram0_s_awcache),
    .s_awprot(dram0_s_awprot),
    .s_awqos(dram0_s_awqos),
    .s_wready(dram0_s_wready),
    .s_wvalid(dram0_s_wvalid),
    .s_wid(dram0_s_wid),
    .s_wdata(dram0_s_wdata),
    .s_wstrb(dram0_s_wstrb),
    .s_wlast(dram0_s_wlast),
    .s_bready(dram0_s_bready),
    .s_bvalid(dram0_s_bvalid),
    .s_bid(dram0_s_bid),
    .s_bresp(dram0_s_bresp),
    .s_arready(dram0_s_arready),
    .s_arvalid(dram0_s_arvalid),
    .s_arid(dram0_s_arid),
    .s_araddr(dram0_s_araddr),
    .s_arlen(dram0_s_arlen),
    .s_arsize(dram0_s_arsize),
    .s_arburst(dram0_s_arburst),
    .s_arlock(dram0_s_arlock),
    .s_arcache(dram0_s_arcache),
    .s_arprot(dram0_s_arprot),
    .s_arqos(dram0_s_arqos),
    .s_rready(dram0_s_rready),
    .s_rvalid(dram0_s_rvalid),
    .s_rid(dram0_s_rid),
    .s_rdata(dram0_s_rdata),
    .s_rresp(dram0_s_rresp),
    .s_rlast(dram0_s_rlast)
  );
  // dram0 inputs.
  assign dram0_clock = clk;
  assign dram0_resetn = ~reset;
  assign dram0_s_awvalid = dut_m_axi_dram0_awvalid;
  assign dram0_s_awid = dut_m_axi_dram0_awid;
  assign dram0_s_awaddr = dut_m_axi_dram0_awaddr;
  assign dram0_s_awlen = dut_m_axi_dram0_awlen;
  assign dram0_s_awsize = dut_m_axi_dram0_awsize;
  assign dram0_s_awburst = dut_m_axi_dram0_awburst;
  assign dram0_s_awlock = dut_m_axi_dram0_awlock;
  assign dram0_s_awcache = dut_m_axi_dram0_awcache;
  assign dram0_s_awprot = dut_m_axi_dram0_awprot;
  assign dram0_s_awqos = dut_m_axi_dram0_awqos;
  assign dram0_s_wvalid = dut_m_axi_dram0_wvalid;
  assign dram0_s_wid = dut_m_axi_dram0_wid;
  assign dram0_s_wdata = dut_m_axi_dram0_wdata;
  assign dram0_s_wstrb = dut_m_axi_dram0_wstrb;
  assign dram0_s_wlast = dut_m_axi_dram0_wlast;
  assign dram0_s_bready = dut_m_axi_dram0_bready;
  assign dram0_s_arvalid = dut_m_axi_dram0_arvalid;
  assign dram0_s_arid = dut_m_axi_dram0_arid;
  assign dram0_s_araddr = dut_m_axi_dram0_araddr;
  assign dram0_s_arlen = dut_m_axi_dram0_arlen;
  assign dram0_s_arsize = dut_m_axi_dram0_arsize;
  assign dram0_s_arburst = dut_m_axi_dram0_arburst;
  assign dram0_s_arlock = dut_m_axi_dram0_arlock;
  assign dram0_s_arcache = dut_m_axi_dram0_arcache;
  assign dram0_s_arprot = dut_m_axi_dram0_arprot;
  assign dram0_s_arqos = dut_m_axi_dram0_arqos;
  assign dram0_s_rready = dut_m_axi_dram0_rready;

  axi4_full_slave #(
    .G_ADDR_WIDTH(G_ADDR_WIDTH),
    .G_DATA_WIDTH(G_DATA_WIDTH),
    .G_ID_WIDTH(G_ID_WIDTH)
  ) dram1 (
    .clock(dram1_clock),
    .resetn(dram1_resetn),
    .s_awready(dram1_s_awready),
    .s_awvalid(dram1_s_awvalid),
    .s_awid(dram1_s_awid),
    .s_awaddr(dram1_s_awaddr),
    .s_awlen(dram1_s_awlen),
    .s_awsize(dram1_s_awsize),
    .s_awburst(dram1_s_awburst),
    .s_awlock(dram1_s_awlock),
    .s_awcache(dram1_s_awcache),
    .s_awprot(dram1_s_awprot),
    .s_awqos(dram1_s_awqos),
    .s_wready(dram1_s_wready),
    .s_wvalid(dram1_s_wvalid),
    .s_wid(dram1_s_wid),
    .s_wdata(dram1_s_wdata),
    .s_wstrb(dram1_s_wstrb),
    .s_wlast(dram1_s_wlast),
    .s_bready(dram1_s_bready),
    .s_bvalid(dram1_s_bvalid),
    .s_bid(dram1_s_bid),
    .s_bresp(dram1_s_bresp),
    .s_arready(dram1_s_arready),
    .s_arvalid(dram1_s_arvalid),
    .s_arid(dram1_s_arid),
    .s_araddr(dram1_s_araddr),
    .s_arlen(dram1_s_arlen),
    .s_arsize(dram1_s_arsize),
    .s_arburst(dram1_s_arburst),
    .s_arlock(dram1_s_arlock),
    .s_arcache(dram1_s_arcache),
    .s_arprot(dram1_s_arprot),
    .s_arqos(dram1_s_arqos),
    .s_rready(dram1_s_rready),
    .s_rvalid(dram1_s_rvalid),
    .s_rid(dram1_s_rid),
    .s_rdata(dram1_s_rdata),
    .s_rresp(dram1_s_rresp),
    .s_rlast(dram1_s_rlast)
  );
  // dram1 inputs.
  assign dram1_clock = clk;
  assign dram1_resetn = ~reset;
  assign dram1_s_awvalid = dut_m_axi_dram1_awvalid;
  assign dram1_s_awid = dut_m_axi_dram1_awid;
  assign dram1_s_awaddr = dut_m_axi_dram1_awaddr;
  assign dram1_s_awlen = dut_m_axi_dram1_awlen;
  assign dram1_s_awsize = dut_m_axi_dram1_awsize;
  assign dram1_s_awburst = dut_m_axi_dram1_awburst;
  assign dram1_s_awlock = dut_m_axi_dram1_awlock;
  assign dram1_s_awcache = dut_m_axi_dram1_awcache;
  assign dram1_s_awprot = dut_m_axi_dram1_awprot;
  assign dram1_s_awqos = dut_m_axi_dram1_awqos;
  assign dram1_s_wvalid = dut_m_axi_dram1_wvalid;
  assign dram1_s_wid = dut_m_axi_dram1_wid;
  assign dram1_s_wdata = dut_m_axi_dram1_wdata;
  assign dram1_s_wstrb = dut_m_axi_dram1_wstrb;
  assign dram1_s_wlast = dut_m_axi_dram1_wlast;
  assign dram1_s_bready = dut_m_axi_dram1_bready;
  assign dram1_s_arvalid = dut_m_axi_dram1_arvalid;
  assign dram1_s_arid = dut_m_axi_dram1_arid;
  assign dram1_s_araddr = dut_m_axi_dram1_araddr;
  assign dram1_s_arlen = dut_m_axi_dram1_arlen;
  assign dram1_s_arsize = dut_m_axi_dram1_arsize;
  assign dram1_s_arburst = dut_m_axi_dram1_arburst;
  assign dram1_s_arlock = dut_m_axi_dram1_arlock;
  assign dram1_s_arcache = dut_m_axi_dram1_arcache;
  assign dram1_s_arprot = dut_m_axi_dram1_arprot;
  assign dram1_s_arqos = dut_m_axi_dram1_arqos;
  assign dram1_s_rready = dut_m_axi_dram1_rready;

  // State machine for sending instructions to the DUT.
  typedef enum {
    STATE_IDLE = 0,
    STATE_STREAM = 1,
    STATE_SUCCESS = 100
  } state_t;

  state_t reg_state = STATE_IDLE, next_state;
  int reg_instr_cnt, next_instr_cnt;

  assign tready = dut_instruction_tready;

  always_ff @(posedge clk) begin
    reg_state = next_state;
    reg_instr_cnt = next_instr_cnt;
  end

  always_comb begin
    // Default values.
    next_state = reg_state;

    reset = 0;
    tdata = 0;
    tvalid = 0;
    tlast = 0;
    next_instr_cnt = reg_instr_cnt;

    case (reg_state)
      STATE_IDLE:
      begin
        reset = 1;
        next_instr_cnt = 0;
        next_state = STATE_STREAM;
      end

      STATE_STREAM:
      begin
        tvalid = 1;
        tdata = instrs[reg_instr_cnt];
        tlast = reg_instr_cnt == (C_INSTRS_DEPTH - 1);
        if (tready == 1) begin
          next_instr_cnt = reg_instr_cnt + 1;
          if (reg_instr_cnt == C_INSTRS_DEPTH - 1) begin
            next_state = STATE_SUCCESS;
          end
        end
      end

      STATE_SUCCESS:
      begin
      end
    endcase
  end

endmodule