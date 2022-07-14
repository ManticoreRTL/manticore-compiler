module Main(input wire clock);

  // Testbench parameters.
  localparam NUM_WORDS_TO_HOST = 100;
  localparam NUM_WORDS_FROM_HOST = 100;

  // For AXI4 slave memory.
  localparam G_ADDR_WIDTH = 12;
  localparam G_DATA_WIDTH = 64;
  localparam G_ID_WIDTH = 8;

  // These are all fixed by the AXI standard.
  localparam C_LENBITS = 8;
  localparam C_SIZEBITS = 3;
  localparam C_BURSTBITS = 2;
  localparam C_LOCKBITS = 2;
  localparam C_CACHEBITS = 4;
  localparam C_PROTBITS = 3;
  localparam C_QOSBITS = 4;
  localparam C_RESPBITS = 2;

  localparam C_NUM_BYTES_PER_WORD = G_DATA_WIDTH / 8;
  localparam [C_RESPBITS - 1 : 0] C_RESP_OKAY = 0;
  localparam C_AXSIZE = C_SIZEBITS'($clog2(C_NUM_BYTES_PER_WORD));
  localparam C_AXBURST = C_BURSTBITS'(1); // INCR
  localparam C_WSTRB = {(C_NUM_BYTES_PER_WORD) {1'b1}};
  localparam C_MAX_BURST_LEN = 2**C_LENBITS;

  // Signals driven by testbench.
  logic reset;
  // logic         vta_io_host_aw_valid;
  // logic  [15:0] vta_io_host_aw_bits_addr;
  // logic  [12:0] vta_io_host_aw_bits_id;
  // logic         vta_io_host_aw_bits_user;
  // logic  [3:0]  vta_io_host_aw_bits_len;
  // logic  [2:0]  vta_io_host_aw_bits_size;
  // logic  [1:0]  vta_io_host_aw_bits_burst;
  // logic  [1:0]  vta_io_host_aw_bits_lock;
  // logic  [3:0]  vta_io_host_aw_bits_cache;
  // logic  [2:0]  vta_io_host_aw_bits_prot;
  // logic  [3:0]  vta_io_host_aw_bits_qos;
  // logic  [3:0]  vta_io_host_aw_bits_region;
  // logic         vta_io_host_w_valid;
  // logic  [31:0] vta_io_host_w_bits_data;
  // logic  [3:0]  vta_io_host_w_bits_strb;
  // logic         vta_io_host_w_bits_last;
  // logic  [12:0] vta_io_host_w_bits_id;
  // logic         vta_io_host_w_bits_user;
  // logic         vta_io_host_b_ready;
  // logic         vta_io_host_ar_valid;
  // logic  [15:0] vta_io_host_ar_bits_addr;
  // logic  [12:0] vta_io_host_ar_bits_id;
  // logic         vta_io_host_ar_bits_user;
  // logic  [3:0]  vta_io_host_ar_bits_len;
  // logic  [2:0]  vta_io_host_ar_bits_size;
  // logic  [1:0]  vta_io_host_ar_bits_burst;
  // logic  [1:0]  vta_io_host_ar_bits_lock;
  // logic  [3:0]  vta_io_host_ar_bits_cache;
  // logic  [2:0]  vta_io_host_ar_bits_prot;
  // logic  [3:0]  vta_io_host_ar_bits_qos;
  // logic  [3:0]  vta_io_host_ar_bits_region;
  // logic         vta_io_host_r_ready;
  // logic         xormix_enable

  logic         vta_clock;
  logic         vta_reset;
  wire          vta_io_host_aw_ready;
  logic         vta_io_host_aw_valid;
  logic  [15:0] vta_io_host_aw_bits_addr;
  logic  [12:0] vta_io_host_aw_bits_id;
  logic         vta_io_host_aw_bits_user;
  logic  [3:0]  vta_io_host_aw_bits_len;
  logic  [2:0]  vta_io_host_aw_bits_size;
  logic  [1:0]  vta_io_host_aw_bits_burst;
  logic  [1:0]  vta_io_host_aw_bits_lock;
  logic  [3:0]  vta_io_host_aw_bits_cache;
  logic  [2:0]  vta_io_host_aw_bits_prot;
  logic  [3:0]  vta_io_host_aw_bits_qos;
  logic  [3:0]  vta_io_host_aw_bits_region;
  wire          vta_io_host_w_ready;
  logic         vta_io_host_w_valid;
  logic  [31:0] vta_io_host_w_bits_data;
  logic  [3:0]  vta_io_host_w_bits_strb;
  logic         vta_io_host_w_bits_last;
  logic  [12:0] vta_io_host_w_bits_id;
  logic         vta_io_host_w_bits_user;
  logic         vta_io_host_b_ready;
  wire          vta_io_host_b_valid;
  wire   [1:0]  vta_io_host_b_bits_resp;
  wire   [12:0] vta_io_host_b_bits_id;
  wire          vta_io_host_b_bits_user;
  wire          vta_io_host_ar_ready;
  logic         vta_io_host_ar_valid;
  logic  [15:0] vta_io_host_ar_bits_addr;
  logic  [12:0] vta_io_host_ar_bits_id;
  logic         vta_io_host_ar_bits_user;
  logic  [3:0]  vta_io_host_ar_bits_len;
  logic  [2:0]  vta_io_host_ar_bits_size;
  logic  [1:0]  vta_io_host_ar_bits_burst;
  logic  [1:0]  vta_io_host_ar_bits_lock;
  logic  [3:0]  vta_io_host_ar_bits_cache;
  logic  [2:0]  vta_io_host_ar_bits_prot;
  logic  [3:0]  vta_io_host_ar_bits_qos;
  logic  [3:0]  vta_io_host_ar_bits_region;
  logic         vta_io_host_r_ready;
  wire          vta_io_host_r_valid;
  wire   [31:0] vta_io_host_r_bits_data;
  wire   [1:0]  vta_io_host_r_bits_resp;
  wire          vta_io_host_r_bits_last;
  wire   [12:0] vta_io_host_r_bits_id;
  wire          vta_io_host_r_bits_user;
  logic         vta_io_mem_aw_ready;
  wire          vta_io_mem_aw_valid;
  wire   [31:0] vta_io_mem_aw_bits_addr;
  wire   [7:0]  vta_io_mem_aw_bits_id;
  wire   [4:0]  vta_io_mem_aw_bits_user;
  wire   [3:0]  vta_io_mem_aw_bits_len;
  wire   [2:0]  vta_io_mem_aw_bits_size;
  wire   [1:0]  vta_io_mem_aw_bits_burst;
  wire   [1:0]  vta_io_mem_aw_bits_lock;
  wire   [3:0]  vta_io_mem_aw_bits_cache;
  wire   [2:0]  vta_io_mem_aw_bits_prot;
  wire   [3:0]  vta_io_mem_aw_bits_qos;
  wire   [3:0]  vta_io_mem_aw_bits_region;
  logic         vta_io_mem_w_ready;
  wire          vta_io_mem_w_valid;
  wire   [63:0] vta_io_mem_w_bits_data;
  wire   [7:0]  vta_io_mem_w_bits_strb;
  wire          vta_io_mem_w_bits_last;
  wire   [7:0]  vta_io_mem_w_bits_id;
  wire   [4:0]  vta_io_mem_w_bits_user;
  wire          vta_io_mem_b_ready;
  logic         vta_io_mem_b_valid;
  logic  [1:0]  vta_io_mem_b_bits_resp;
  logic  [7:0]  vta_io_mem_b_bits_id;
  logic  [4:0]  vta_io_mem_b_bits_user;
  logic         vta_io_mem_ar_ready;
  wire          vta_io_mem_ar_valid;
  wire   [31:0] vta_io_mem_ar_bits_addr;
  wire   [7:0]  vta_io_mem_ar_bits_id;
  wire   [4:0]  vta_io_mem_ar_bits_user;
  wire   [3:0]  vta_io_mem_ar_bits_len;
  wire   [2:0]  vta_io_mem_ar_bits_size;
  wire   [1:0]  vta_io_mem_ar_bits_burst;
  wire   [1:0]  vta_io_mem_ar_bits_lock;
  wire   [3:0]  vta_io_mem_ar_bits_cache;
  wire   [2:0]  vta_io_mem_ar_bits_prot;
  wire   [3:0]  vta_io_mem_ar_bits_qos;
  wire   [3:0]  vta_io_mem_ar_bits_region;
  wire          vta_io_mem_r_ready;
  logic         vta_io_mem_r_valid;
  logic  [63:0] vta_io_mem_r_bits_data;
  logic  [1:0]  vta_io_mem_r_bits_resp;
  logic         vta_io_mem_r_bits_last;
  logic  [7:0]  vta_io_mem_r_bits_id;
  logic  [4:0]  vta_io_mem_r_bits_user;

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

  localparam streams = 1;
  logic                         xormix_clk;
  logic                         xormix_rst;
  logic [31 : 0]                xormix_seed_x;
  logic [32 * streams - 1 : 0]  xormix_seed_y;
  logic                         xormix_enable;
  wire   [32 * streams - 1 : 0] xormix_result;

  IntelShell vta (
    .clock(vta_clock),
    .reset(vta_reset),
    .io_host_aw_ready(vta_io_host_aw_ready),
    .io_host_aw_valid(vta_io_host_aw_valid),
    .io_host_aw_bits_addr(vta_io_host_aw_bits_addr),
    .io_host_aw_bits_id(vta_io_host_aw_bits_id),
    .io_host_aw_bits_user(vta_io_host_aw_bits_user),
    .io_host_aw_bits_len(vta_io_host_aw_bits_len),
    .io_host_aw_bits_size(vta_io_host_aw_bits_size),
    .io_host_aw_bits_burst(vta_io_host_aw_bits_burst),
    .io_host_aw_bits_lock(vta_io_host_aw_bits_lock),
    .io_host_aw_bits_cache(vta_io_host_aw_bits_cache),
    .io_host_aw_bits_prot(vta_io_host_aw_bits_prot),
    .io_host_aw_bits_qos(vta_io_host_aw_bits_qos),
    .io_host_aw_bits_region(vta_io_host_aw_bits_region),
    .io_host_w_ready(vta_io_host_w_ready),
    .io_host_w_valid(vta_io_host_w_valid),
    .io_host_w_bits_data(vta_io_host_w_bits_data),
    .io_host_w_bits_strb(vta_io_host_w_bits_strb),
    .io_host_w_bits_last(vta_io_host_w_bits_last),
    .io_host_w_bits_id(vta_io_host_w_bits_id),
    .io_host_w_bits_user(vta_io_host_w_bits_user),
    .io_host_b_ready(vta_io_host_b_ready),
    .io_host_b_valid(vta_io_host_b_valid),
    .io_host_b_bits_resp(vta_io_host_b_bits_resp),
    .io_host_b_bits_id(vta_io_host_b_bits_id),
    .io_host_b_bits_user(vta_io_host_b_bits_user),
    .io_host_ar_ready(vta_io_host_ar_ready),
    .io_host_ar_valid(vta_io_host_ar_valid),
    .io_host_ar_bits_addr(vta_io_host_ar_bits_addr),
    .io_host_ar_bits_id(vta_io_host_ar_bits_id),
    .io_host_ar_bits_user(vta_io_host_ar_bits_user),
    .io_host_ar_bits_len(vta_io_host_ar_bits_len),
    .io_host_ar_bits_size(vta_io_host_ar_bits_size),
    .io_host_ar_bits_burst(vta_io_host_ar_bits_burst),
    .io_host_ar_bits_lock(vta_io_host_ar_bits_lock),
    .io_host_ar_bits_cache(vta_io_host_ar_bits_cache),
    .io_host_ar_bits_prot(vta_io_host_ar_bits_prot),
    .io_host_ar_bits_qos(vta_io_host_ar_bits_qos),
    .io_host_ar_bits_region(vta_io_host_ar_bits_region),
    .io_host_r_ready(vta_io_host_r_ready),
    .io_host_r_valid(vta_io_host_r_valid),
    .io_host_r_bits_data(vta_io_host_r_bits_data),
    .io_host_r_bits_resp(vta_io_host_r_bits_resp),
    .io_host_r_bits_last(vta_io_host_r_bits_last),
    .io_host_r_bits_id(vta_io_host_r_bits_id),
    .io_host_r_bits_user(vta_io_host_r_bits_user),
    .io_mem_aw_ready(vta_io_mem_aw_ready),
    .io_mem_aw_valid(vta_io_mem_aw_valid),
    .io_mem_aw_bits_addr(vta_io_mem_aw_bits_addr),
    .io_mem_aw_bits_id(vta_io_mem_aw_bits_id),
    .io_mem_aw_bits_user(vta_io_mem_aw_bits_user),
    .io_mem_aw_bits_len(vta_io_mem_aw_bits_len),
    .io_mem_aw_bits_size(vta_io_mem_aw_bits_size),
    .io_mem_aw_bits_burst(vta_io_mem_aw_bits_burst),
    .io_mem_aw_bits_lock(vta_io_mem_aw_bits_lock),
    .io_mem_aw_bits_cache(vta_io_mem_aw_bits_cache),
    .io_mem_aw_bits_prot(vta_io_mem_aw_bits_prot),
    .io_mem_aw_bits_qos(vta_io_mem_aw_bits_qos),
    .io_mem_aw_bits_region(vta_io_mem_aw_bits_region),
    .io_mem_w_ready(vta_io_mem_w_ready),
    .io_mem_w_valid(vta_io_mem_w_valid),
    .io_mem_w_bits_data(vta_io_mem_w_bits_data),
    .io_mem_w_bits_strb(vta_io_mem_w_bits_strb),
    .io_mem_w_bits_last(vta_io_mem_w_bits_last),
    .io_mem_w_bits_id(vta_io_mem_w_bits_id),
    .io_mem_w_bits_user(vta_io_mem_w_bits_user),
    .io_mem_b_ready(vta_io_mem_b_ready),
    .io_mem_b_valid(vta_io_mem_b_valid),
    .io_mem_b_bits_resp(vta_io_mem_b_bits_resp),
    .io_mem_b_bits_id(vta_io_mem_b_bits_id),
    .io_mem_b_bits_user(vta_io_mem_b_bits_user),
    .io_mem_ar_ready(vta_io_mem_ar_ready),
    .io_mem_ar_valid(vta_io_mem_ar_valid),
    .io_mem_ar_bits_addr(vta_io_mem_ar_bits_addr),
    .io_mem_ar_bits_id(vta_io_mem_ar_bits_id),
    .io_mem_ar_bits_user(vta_io_mem_ar_bits_user),
    .io_mem_ar_bits_len(vta_io_mem_ar_bits_len),
    .io_mem_ar_bits_size(vta_io_mem_ar_bits_size),
    .io_mem_ar_bits_burst(vta_io_mem_ar_bits_burst),
    .io_mem_ar_bits_lock(vta_io_mem_ar_bits_lock),
    .io_mem_ar_bits_cache(vta_io_mem_ar_bits_cache),
    .io_mem_ar_bits_prot(vta_io_mem_ar_bits_prot),
    .io_mem_ar_bits_qos(vta_io_mem_ar_bits_qos),
    .io_mem_ar_bits_region(vta_io_mem_ar_bits_region),
    .io_mem_r_ready(vta_io_mem_r_ready),
    .io_mem_r_valid(vta_io_mem_r_valid),
    .io_mem_r_bits_data(vta_io_mem_r_bits_data),
    .io_mem_r_bits_resp(vta_io_mem_r_bits_resp),
    .io_mem_r_bits_last(vta_io_mem_r_bits_last),
    .io_mem_r_bits_id(vta_io_mem_r_bits_id),
    .io_mem_r_bits_user(vta_io_mem_r_bits_user)
  );
  assign vta_clock = clock;
  assign vta_reset = reset;
  assign vta_io_mem_aw_ready = mem_s_awready;
  assign vta_io_mem_w_ready = mem_s_wready;
  assign vta_io_mem_b_valid = mem_s_bvalid;
  assign vta_io_mem_b_bits_resp = mem_s_bresp;
  assign vta_io_mem_b_bits_id = mem_s_bid;
  assign vta_io_mem_b_bits_user = 0;
  assign vta_io_mem_ar_ready = mem_s_arready;
  assign vta_io_mem_r_valid = mem_s_rvalid;
  assign vta_io_mem_r_bits_data = mem_s_rdata;
  assign vta_io_mem_r_bits_resp = mem_s_rresp;
  assign vta_io_mem_r_bits_last = mem_s_rlast;
  assign vta_io_mem_r_bits_id = mem_s_rid;
  assign vta_io_mem_r_bits_user = 0;

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
  assign mem_s_awvalid = vta_io_mem_aw_valid;
  assign mem_s_awid = vta_io_mem_aw_bits_id;
  assign mem_s_awaddr = vta_io_mem_aw_bits_addr;
  assign mem_s_awlen = vta_io_mem_aw_bits_len;
  assign mem_s_awsize = vta_io_mem_aw_bits_size;
  assign mem_s_awburst = vta_io_mem_aw_bits_burst;
  assign mem_s_awlock = vta_io_mem_aw_bits_lock;
  assign mem_s_awcache = vta_io_mem_aw_bits_cache;
  assign mem_s_awprot = vta_io_mem_aw_bits_prot;
  assign mem_s_awqos = vta_io_mem_aw_bits_qos;
  assign mem_s_wvalid = vta_io_mem_w_valid;
  assign mem_s_wid = vta_io_mem_w_bits_id;
  assign mem_s_wdata = vta_io_mem_w_bits_data;
  assign mem_s_wstrb = vta_io_mem_w_bits_strb;
  assign mem_s_wlast = vta_io_mem_w_bits_last;
  assign mem_s_bready = vta_io_mem_b_ready;
  assign mem_s_arvalid = vta_io_mem_ar_valid;
  assign mem_s_arid = vta_io_mem_ar_bits_id;
  assign mem_s_araddr = vta_io_mem_ar_bits_addr;
  assign mem_s_arlen = vta_io_mem_ar_bits_len;
  assign mem_s_arsize = vta_io_mem_ar_bits_size;
  assign mem_s_arburst = vta_io_mem_ar_bits_burst;
  assign mem_s_arlock = vta_io_mem_ar_bits_lock;
  assign mem_s_arcache = vta_io_mem_ar_bits_cache;
  assign mem_s_arprot = vta_io_mem_ar_bits_prot;
  assign mem_s_arqos = vta_io_mem_ar_bits_qos;
  assign mem_s_rready = vta_io_mem_r_ready;

  xormix32 #(
    .streams(1)
  ) xormix (
    .clk(xormix_clk),
    .rst(xormix_rst),
    .seed_x(xormix_seed_x),
    .seed_y(xormix_seed_y),
    .enable(xormix_enable),
    .result(xormix_result)
  );
  assign xormix_clk = clock;
  assign xormix_rst = reset;
  assign xormix_seed_x = 1; // Any value is ok.
  assign xormix_seed_y = 2; // Any value is ok.

  typedef enum {
    STATE_IDLE = 0,
    STATE_START = 1,

    // Single AXI4 writes (to host).
    STATE_SINGLE_WRITE_START = 2,
    STATE_SINGLE_WRITE_AWVALID = 3,
    STATE_SINGLE_WRITE_WVALID = 4,
    STATE_SINGLE_WRITE_BREADY = 5,
    STATE_SINGLE_WRITE_CHECK = 6,
    STATE_SINGLE_WRITE_INCR = 7,
    STATE_SINGLE_WRITE_CHECK_END = 8,
    STATE_SINGLE_WRITE_END = 9,

    // Single reads.
    STATE_SINGLE_READ_START = 10,
    STATE_SINGLE_READ_ARVALID = 11,
    STATE_SINGLE_READ_RECV = 12,
    STATE_SINGLE_READ_INCR = 13,
    STATE_SINGLE_READ_CHECK_END = 14,
    STATE_SINGLE_READ_END = 15,

    STATE_END = 100
  } state_t;

  state_t reg_state = STATE_IDLE, next_state;

  int reg_host_wdata_cnt, next_host_wdata_cnt;

  // Count the number of messages received on the host interface.
  // The data received is XOR-ed together to get its bit-level parity.
  int reg_host_rdata_cnt, next_host_rdata_cnt;
  logic [31 : 0] reg_host_rdata_parity, next_host_rdata_parity;

  // State registers.
  always_ff @(posedge clock) begin
    reg_state <= next_state;
    reg_host_wdata_cnt = next_host_wdata_cnt;
    reg_host_rdata_cnt = next_host_rdata_cnt;
    reg_host_rdata_parity = next_host_rdata_parity;

    if (reg_state == STATE_END) begin
      $display("finished");
      $finish;
    end
  end

  // Next state logic.
  always_comb begin
    // Default values.
    next_state = reg_state;
    next_host_wdata_cnt = reg_host_wdata_cnt;
    next_host_rdata_cnt = reg_host_rdata_cnt;
    next_host_rdata_parity = reg_host_rdata_parity;

    reset = 0;                              // logic
    vta_io_host_aw_valid = 0;               // logic
    vta_io_host_aw_bits_addr = 0;           // logic  [15:0]
    vta_io_host_aw_bits_id = 0;             // logic  [12:0]
    vta_io_host_aw_bits_user = 0;           // logic
    vta_io_host_aw_bits_len = 0;            // logic  [3:0] , 1 word
    vta_io_host_aw_bits_size = C_AXSIZE;    // logic  [2:0]
    vta_io_host_aw_bits_burst = C_AXBURST;  // logic  [1:0] , INCR
    vta_io_host_aw_bits_lock = 0;           // logic  [1:0]
    vta_io_host_aw_bits_cache = 0;          // logic  [3:0]
    vta_io_host_aw_bits_prot = 0;           // logic  [2:0]
    vta_io_host_aw_bits_qos = 0;            // logic  [3:0]
    vta_io_host_aw_bits_region = 0;         // logic  [3:0]
    vta_io_host_w_valid = 0;                // logic
    vta_io_host_w_bits_data = 0;            // logic  [31:0]
    vta_io_host_w_bits_strb = 4'hf;         // logic  [3:0] , 0b1111
    vta_io_host_w_bits_last = 0;            // logic
    vta_io_host_w_bits_id = 0;              // logic  [12:0]
    vta_io_host_w_bits_user = 0;            // logic
    vta_io_host_b_ready = 0;                // logic
    vta_io_host_ar_valid = 0;               // logic
    vta_io_host_ar_bits_addr = 0;           // logic  [15:0]
    vta_io_host_ar_bits_id = 0;             // logic  [12:0]
    vta_io_host_ar_bits_user = 0;           // logic
    vta_io_host_ar_bits_len = 0;            // logic  [3:0] , 1 word
    vta_io_host_ar_bits_size = C_AXSIZE;    // logic  [2:0]
    vta_io_host_ar_bits_burst = C_AXBURST;  // logic  [1:0] , INCR
    vta_io_host_ar_bits_lock = 0;           // logic  [1:0]
    vta_io_host_ar_bits_cache = 0;          // logic  [3:0]
    vta_io_host_ar_bits_prot = 0;           // logic  [2:0]
    vta_io_host_ar_bits_qos = 0;            // logic  [3:0]
    vta_io_host_ar_bits_region = 0;         // logic  [3:0]
    vta_io_host_r_ready = 0;                // logic
    xormix_enable = 0;                      // logic

    case (reg_state)
      STATE_IDLE:
      begin
        reset = 1;
        next_host_wdata_cnt = 0;
        next_host_rdata_cnt = 0;
        next_host_rdata_parity = 0;
        next_state = STATE_START;
      end

      STATE_START:
      begin
        next_state = STATE_SINGLE_WRITE_START;
      end

      STATE_SINGLE_WRITE_START:
      begin
        next_state = STATE_SINGLE_WRITE_AWVALID;
        xormix_enable = 1; // Data is available on the next cycle.
      end

      STATE_SINGLE_WRITE_AWVALID:
      begin
        vta_io_host_aw_bits_addr = xormix_result; // We choose a random address.
        vta_io_host_aw_valid = 1;
        vta_io_host_aw_bits_burst = C_AXBURST;
        vta_io_host_aw_bits_len = 0; // 1 word.
        if (vta_io_host_aw_ready) begin
          next_state = STATE_SINGLE_WRITE_WVALID;
        end
      end

      STATE_SINGLE_WRITE_WVALID:
      begin
        vta_io_host_w_valid = 1;
        vta_io_host_w_bits_data = xormix_result;
        vta_io_host_w_bits_last = 1; // Single-word transfer.
        if (vta_io_host_w_ready) begin
          next_state = STATE_SINGLE_WRITE_BREADY;
        end
      end

      STATE_SINGLE_WRITE_BREADY:
      begin
        vta_io_host_b_ready = 1;
        if (vta_io_host_b_valid) begin
          next_state = STATE_SINGLE_WRITE_INCR;
        end
      end

      STATE_SINGLE_WRITE_INCR:
      begin
        next_host_wdata_cnt = reg_host_wdata_cnt + 1;
        next_state = STATE_SINGLE_WRITE_CHECK_END;
      end

      STATE_SINGLE_WRITE_CHECK_END:
      begin
        if (reg_host_wdata_cnt == NUM_WORDS_TO_HOST) begin
          next_state = STATE_SINGLE_WRITE_END;
        end else begin
          next_state = STATE_SINGLE_WRITE_START;
        end
      end

      STATE_SINGLE_WRITE_END:
      begin
        next_state = STATE_SINGLE_READ_START;
      end

      STATE_SINGLE_READ_START:
      begin
        next_state = STATE_SINGLE_READ_ARVALID;
        xormix_enable = 1;
      end

      STATE_SINGLE_READ_ARVALID:
      begin
        vta_io_host_ar_bits_addr = xormix_result; // We choose a random address.
        vta_io_host_ar_valid = 1;
        vta_io_host_ar_bits_burst = C_AXBURST;
        vta_io_host_ar_bits_len = 0; // 1 word.
        if (vta_io_host_ar_ready) begin
          next_state = STATE_SINGLE_READ_RECV;
        end
      end

      STATE_SINGLE_READ_RECV:
      begin
        vta_io_host_r_ready = 1;
        if (vta_io_host_r_valid) begin
          next_host_rdata_parity = reg_host_rdata_parity ^ vta_io_host_r_bits_data;
          next_state = STATE_SINGLE_READ_INCR;
        end
      end

      STATE_SINGLE_READ_INCR:
      begin
        next_host_rdata_cnt = reg_host_rdata_cnt + 1;
        next_state = STATE_SINGLE_READ_CHECK_END;
      end

      STATE_SINGLE_READ_CHECK_END:
      begin
        if (reg_host_rdata_cnt == NUM_WORDS_FROM_HOST) begin
          next_state = STATE_SINGLE_READ_END;
        end else begin
          next_state = STATE_SINGLE_READ_START;
        end
      end

      STATE_SINGLE_READ_END:
      begin
        next_state = STATE_END;
      end

      STATE_END:
      begin

      end
    endcase
  end

endmodule
