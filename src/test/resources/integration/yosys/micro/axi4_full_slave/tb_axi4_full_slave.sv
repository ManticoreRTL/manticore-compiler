module Main(input wire clk);

  // These are all fixed by the standard.
  localparam C_LENBITS = 8;
  localparam C_SIZEBITS = 3;
  localparam C_BURSTBITS = 2;
  localparam C_LOCKBITS = 2;
  localparam C_CACHEBITS = 4;
  localparam C_PROTBITS = 3;
  localparam C_QOSBITS = 4;
  localparam C_RESPBITS = 2;

  localparam [C_RESPBITS - 1 : 0] C_RESP_OKAY = 0;

  // DUT generics.
  localparam G_ADDR_WIDTH = 7;
  localparam G_DATA_WIDTH = 32;
  localparam G_ID_WIDTH = 1;
  localparam MEM_INIT_FILE = "mem.hex";

  // Addresses issued to the DUT must be G_DATA_WIDTH-aligned.
  localparam C_ADDR_MASK = ~((1 << $clog2(G_DATA_WIDTH / 8)) - 1);
  localparam C_MEM_DEPTH = 2 ** (G_ADDR_WIDTH - $clog2(G_DATA_WIDTH / 8));

  // Number of tests to perform.
  localparam C_NUM_SINGLE_READS = 3;
  localparam C_NUM_SINGLE_WRITES = 4;

  // Generate addresses used for single-read and single-write transactions.
  logic [G_ADDR_WIDTH - 1 : 0] RD_ADDRS [0 : C_NUM_SINGLE_READS - 1];
  logic [G_ADDR_WIDTH - 1 : 0] WR_ADDRS [0 : C_NUM_SINGLE_WRITES - 1];
  initial begin
    // Generated addresses must be a multiple of the data width, so we mask the addresses.
    for (int i = 0; i < C_NUM_SINGLE_READS; i = i + 1) begin
      RD_ADDRS[i] = G_ADDR_WIDTH'($urandom_range(0, 2**G_ADDR_WIDTH - 1) & C_ADDR_MASK);
    end
    for (int i = 0; i < C_NUM_SINGLE_WRITES; i = i + 1) begin
      WR_ADDRS[i] = G_ADDR_WIDTH'($urandom_range(0, 2**G_ADDR_WIDTH - 1) & C_ADDR_MASK);
    end
  end

  logic [G_DATA_WIDTH - 1 : 0] mem [0 : C_MEM_DEPTH - 1];
  // View of the memory by the testbench. The DUT itself also has an internal
  // memory that is initialized with these contents as we give the same memory
  // initialization file to the DUT.
  initial begin
    $readmemh(MEM_INIT_FILE, mem);
  end

  function logic [G_DATA_WIDTH - 1 : 0] read_mem(
    int addr
  );
    logic [G_ADDR_WIDTH - $clog2(G_DATA_WIDTH / 8) - 1 : 0] mem_addr = addr[G_ADDR_WIDTH - 1 : $clog2(G_DATA_WIDTH / 8)];
    $display("mem[%d] = %h", addr, mem[mem_addr]);
    return mem[mem_addr];
  endfunction

  function void write_mem(
    int addr,
    logic [G_DATA_WIDTH - 1 : 0] data
  );
    logic [G_ADDR_WIDTH - $clog2(G_DATA_WIDTH / 8) - 1 : 0] mem_addr = addr[G_ADDR_WIDTH - 1 : $clog2(G_DATA_WIDTH / 8)];
    mem[mem_addr] = data;
  endfunction

  logic                              resetn;
  wire                               s_awready;
  logic                              s_awvalid;
  logic [G_ID_WIDTH - 1 : 0]         s_awid;
  logic [G_ADDR_WIDTH - 1 : 0]       s_awaddr;
  logic [C_LENBITS - 1 : 0]          s_awlen;
  logic [C_SIZEBITS - 1 : 0]         s_awsize;
  logic [C_BURSTBITS - 1 : 0]        s_awburst;
  logic [C_LOCKBITS - 1 : 0]         s_awlock;
  logic [C_CACHEBITS - 1 : 0]        s_awcache;
  logic [C_PROTBITS - 1 : 0]         s_awprot;
  logic [C_QOSBITS - 1 : 0]          s_awqos;
  wire                               s_wready;
  logic                              s_wvalid;
  logic [G_ID_WIDTH - 1 : 0]         s_wid;
  logic [G_DATA_WIDTH - 1 : 0]       s_wdata;
  logic [(G_DATA_WIDTH / 8) - 1 : 0] s_wstrb;
  logic                              s_wlast;
  logic                              s_bready;
  wire                               s_bvalid;
  wire [G_ID_WIDTH - 1 : 0]          s_bid;
  wire [C_RESPBITS - 1 : 0]          s_bresp;
  wire                               s_arready;
  logic                              s_arvalid;
  logic [G_ID_WIDTH - 1 : 0]         s_arid;
  logic [G_ADDR_WIDTH - 1 : 0]       s_araddr;
  logic [C_LENBITS - 1 : 0]          s_arlen;
  logic [C_SIZEBITS - 1 : 0]         s_arsize;
  logic [C_BURSTBITS - 1 : 0]        s_arburst;
  logic [C_LOCKBITS - 1 : 0]         s_arlock;
  logic [C_CACHEBITS - 1 : 0]        s_arcache;
  logic [C_PROTBITS - 1 : 0]         s_arprot;
  logic [C_QOSBITS - 1 : 0]          s_arqos;
  logic                              s_rready;
  wire                               s_rvalid;
  wire [G_ID_WIDTH - 1 : 0]          s_rid;
  wire [G_DATA_WIDTH - 1 : 0]        s_rdata;
  wire [C_RESPBITS - 1 : 0]          s_rresp;
  wire                               s_rlast;

  axi4_full_slave #(
    .G_ADDR_WIDTH(G_ADDR_WIDTH),
    .G_DATA_WIDTH(G_DATA_WIDTH),
    .G_ID_WIDTH(G_ID_WIDTH),
    .MEM_INIT_FILE(MEM_INIT_FILE)
  ) dut (
    .clock(clk),
    .resetn(resetn),
    .s_awready(s_awready),
    .s_awvalid(s_awvalid),
    .s_awid(s_awid),
    .s_awaddr(s_awaddr),
    .s_awlen(s_awlen),
    .s_awsize(s_awsize),
    .s_awburst(s_awburst),
    .s_awlock(s_awlock),
    .s_awcache(s_awcache),
    .s_awprot(s_awprot),
    .s_awqos(s_awqos),
    .s_wready(s_wready),
    .s_wvalid(s_wvalid),
    .s_wid(s_wid),
    .s_wdata(s_wdata),
    .s_wstrb(s_wstrb),
    .s_wlast(s_wlast),
    .s_bready(s_bready),
    .s_bvalid(s_bvalid),
    .s_bid(s_bid),
    .s_bresp(s_bresp),
    .s_arready(s_arready),
    .s_arvalid(s_arvalid),
    .s_arid(s_arid),
    .s_araddr(s_araddr),
    .s_arlen(s_arlen),
    .s_arsize(s_arsize),
    .s_arburst(s_arburst),
    .s_arlock(s_arlock),
    .s_arcache(s_arcache),
    .s_arprot(s_arprot),
    .s_arqos(s_arqos),
    .s_rready(s_rready),
    .s_rvalid(s_rvalid),
    .s_rid(s_rid),
    .s_rdata(s_rdata),
    .s_rresp(s_rresp),
    .s_rlast(s_rlast)
  );

  // Use a state machine to check the DUT for correctness.
  // 1) Read some random addresses (no burst) and check that the result is what we expect the pre-populated
  //    memory contents to be.
  // 2) Write some random addresses (no burst).
  // 3) Read back random addresses that we wrote to and check that they are what we expect.

  typedef enum {
    STATE_IDLE = 0,
    // Single reads.
    STATE_SINGLE_READ_START = 1,
    STATE_SINGLE_READ_ARVALID = 2,
    STATE_SINGLE_READ_RECV = 3,
    STATE_SINGLE_READ_CHECK = 4,
    STATE_SINGLE_READ_INCR = 5,
    STATE_SINGLE_READ_FAIL = 6,
    STATE_SINGLE_READ_END = 7,
    // Single writes.
    STATE_SINGLE_WRITE_START = 8,
    STATE_SINGLE_WRITE_ARVALID = 9,
    STATE_SINGLE_WRITE_WVALID = 10,
    STATE_SINGLE_WRITE_BREADY = 11,
    STATE_SINGLE_WRITE_CHECK = 12,
    STATE_SINGLE_WRITE_INCR = 13,
    STATE_SINGLE_WRITE_FAIL = 14,
    STATE_SINGLE_WRITE_END = 15,
    // Check single writes.
    STATE_SINGLE_WRITE_CHECK_START = 16,
    STATE_SINGLE_WRITE_CHECK_ARVALID = 17,
    STATE_SINGLE_WRITE_CHECK_RECV = 18,
    STATE_SINGLE_WRITE_CHECK_VERIFY = 19,
    STATE_SINGLE_WRITE_CHECK_INCR = 20,
    STATE_SINGLE_WRITE_CHECK_FAIL = 21,
    STATE_SINGLE_WRITE_CHECK_END = 22,
    // Burst writes.

    // End.
    STATE_SUCCESS = 100
  } state_t;

  state_t reg_state = STATE_IDLE, next_state;
  int reg_single_rd_cnt, next_single_rd_cnt;
  int reg_single_wr_cnt, next_single_wr_cnt;
  logic [G_DATA_WIDTH - 1 : 0] reg_rdata, next_rdata;
  logic [1 : 0] reg_rresp, next_rresp;

  // State update.
  always_ff @(posedge clk) begin
    reg_state <= next_state;
    reg_single_rd_cnt <= next_single_rd_cnt;
    reg_single_wr_cnt <= next_single_wr_cnt;
    reg_rdata <= next_rdata;
    reg_rresp <= next_rresp;
  end

  // Next state logic.
  always_comb begin
    // DUT inputs
    resetn = 1;
    s_awvalid = 0;
    s_awid = 0;
    s_awaddr = 0;
    s_awlen = 0;
    s_awsize = 0;
    s_awburst = 0;
    s_awlock = 0;
    s_awcache = 0;
    s_awprot = 0;
    s_awqos = 0;
    s_wvalid = 0;
    s_wid = 0;
    s_wdata = 0;
    s_wstrb = 0;
    s_wlast = 0;
    s_bready = 0;
    s_arvalid = 0;
    s_arid = 0;
    s_araddr = 0;
    s_arlen = 0;
    s_arsize = C_SIZEBITS'($clog2(G_DATA_WIDTH / 8));
    s_arburst = 0;
    s_arlock = 0;
    s_arcache = 0;
    s_arprot = 0;
    s_arqos = 0;
    s_rready = 0;

    next_state = reg_state;
    next_single_rd_cnt = reg_single_rd_cnt;
    next_single_wr_cnt = reg_single_wr_cnt;
    next_rdata = reg_rdata;
    next_rresp = reg_rresp;

    case (reg_state)
      STATE_IDLE:
      begin
        resetn = 0;
        next_state = STATE_SINGLE_READ_START;
      end

      STATE_SINGLE_READ_START:
      begin
        next_single_rd_cnt = 0;
        next_state = STATE_SINGLE_READ_ARVALID;
      end

      STATE_SINGLE_READ_ARVALID:
      begin
        s_araddr = RD_ADDRS[reg_single_rd_cnt];
        s_arvalid = 1;
        s_arlen = 0; // no burst
        if (s_arready == 1) begin
          next_state = STATE_SINGLE_READ_RECV;
        end
      end

      STATE_SINGLE_READ_RECV:
      begin
        s_rready = 1;
        next_rdata = s_rdata;
        next_rresp = s_rresp;
        if (s_rvalid == 1) begin
          next_state = STATE_SINGLE_READ_CHECK;
        end
      end

      STATE_SINGLE_READ_CHECK:
      begin
        if ((reg_rdata == read_mem(RD_ADDRS[reg_single_rd_cnt])) && (reg_rresp == 0)) begin
          next_state = STATE_SINGLE_READ_INCR;
        end else begin
          next_state = STATE_SINGLE_READ_FAIL;
        end
      end

      STATE_SINGLE_READ_INCR:
      begin
        next_single_rd_cnt = reg_single_rd_cnt + 1;
        next_state = STATE_SINGLE_READ_END;
      end

      STATE_SINGLE_READ_FAIL:
      begin
        $display("single-read failed");
        $finish;
      end

      STATE_SINGLE_READ_END:
      begin
        if (reg_single_rd_cnt < C_NUM_SINGLE_READS) begin
          next_state = STATE_SINGLE_READ_ARVALID;
        end else begin
          next_state = STATE_SUCCESS;
        end
      end

      STATE_SINGLE_WRITE_START:
      begin

      end

      STATE_SINGLE_WRITE_ARVALID:
      begin

      end

      STATE_SINGLE_WRITE_WVALID:
      begin

      end

      STATE_SINGLE_WRITE_BREADY:
      begin

      end

      STATE_SINGLE_WRITE_CHECK:
      begin

      end

      STATE_SINGLE_WRITE_INCR:
      begin

      end

      STATE_SINGLE_WRITE_FAIL:
      begin

      end

      STATE_SINGLE_WRITE_END:
      begin

      end

      STATE_SINGLE_WRITE_CHECK_START:
      begin

      end

      STATE_SINGLE_WRITE_CHECK_ARVALID:
      begin

      end

      STATE_SINGLE_WRITE_CHECK_RECV:
      begin

      end

      STATE_SINGLE_WRITE_CHECK_VERIFY:
      begin

      end

      STATE_SINGLE_WRITE_CHECK_INCR:
      begin

      end

      STATE_SINGLE_WRITE_CHECK_FAIL:
      begin

      end

      STATE_SINGLE_WRITE_CHECK_END:
      begin

      end

      STATE_SUCCESS:
      begin
        $finish;
      end

    endcase
  end

endmodule