// module tb_AXIWrapperTCU(input wire clk);
module Main(input wire clk);

  // Memory parameters.
  localparam G_ADDR_WIDTH = 10;
  localparam G_DATA_WIDTH = 64;
  localparam G_ID_WIDTH = 6;

  // Number of iterations to perform in the test. One iteration is sending some NOPS, followed by reading
  // from DRAM1, followed by an invalid instruction. We reset the circuit after each iteration.
  localparam C_NUM_ITERATIONS = 10;
  localparam C_NUM_NOPS = 10;

  logic reset = 0;

  // Instructions.
  localparam C_INSTR_WIDTH = 64;
  localparam [C_INSTR_WIDTH - 1 : 0] INSTR_NOP = 64'h0000000000000000;
  localparam [C_INSTR_WIDTH - 1 : 0] INSTR_MOVE_DATA_DRAM1_TO_MEM = 64'h2200481000000000;
  localparam [C_INSTR_WIDTH - 1 : 0] INSTR_INVALID = 64'h6000000000000000;

  // AXI stream master (that will send instructions to DUT).
  logic [G_DATA_WIDTH - 1 : 0] tdata;
  logic                        tvalid;
  wire                         tready;
  logic                        tlast;

  // Testbench signals to interact with DUT.
  logic status_ready;
  logic sample_ready;

  // DUT signals.
  logic         dut_clock;
  logic         dut_reset;
  wire          dut_instruction_ready;
  logic         dut_instruction_valid;
  logic  [3:0]  dut_instruction_bits_opcode;
  logic  [3:0]  dut_instruction_bits_flags;
  logic  [47:0] dut_instruction_bits_arguments;
  logic         dut_status_ready;
  wire          dut_status_valid;
  wire          dut_status_bits_last;
  wire [3:0]    dut_status_bits_bits_opcode;
  wire [3:0]    dut_status_bits_bits_flags;
  wire [47:0]   dut_status_bits_bits_arguments;
  logic         dut_dram0_writeAddress_ready;
  wire          dut_dram0_writeAddress_valid;
  wire [5:0]    dut_dram0_writeAddress_bits_id;
  wire [31:0]   dut_dram0_writeAddress_bits_addr;
  wire [7:0]    dut_dram0_writeAddress_bits_len;
  wire [2:0]    dut_dram0_writeAddress_bits_size;
  wire [1:0]    dut_dram0_writeAddress_bits_burst;
  wire [1:0]    dut_dram0_writeAddress_bits_lock;
  wire [3:0]    dut_dram0_writeAddress_bits_cache;
  wire [2:0]    dut_dram0_writeAddress_bits_prot;
  wire [3:0]    dut_dram0_writeAddress_bits_qos;
  logic         dut_dram0_writeData_ready;
  wire          dut_dram0_writeData_valid;
  wire [5:0]    dut_dram0_writeData_bits_id;
  wire [63:0]   dut_dram0_writeData_bits_data;
  wire [7:0]    dut_dram0_writeData_bits_strb;
  wire          dut_dram0_writeData_bits_last;
  wire          dut_dram0_writeResponse_ready;
  logic         dut_dram0_writeResponse_valid;
  logic  [5:0]  dut_dram0_writeResponse_bits_id;
  logic  [1:0]  dut_dram0_writeResponse_bits_resp;
  logic         dut_dram0_readAddress_ready;
  wire          dut_dram0_readAddress_valid;
  wire [5:0]    dut_dram0_readAddress_bits_id;
  wire [31:0]   dut_dram0_readAddress_bits_addr;
  wire [7:0]    dut_dram0_readAddress_bits_len;
  wire [2:0]    dut_dram0_readAddress_bits_size;
  wire [1:0]    dut_dram0_readAddress_bits_burst;
  wire [1:0]    dut_dram0_readAddress_bits_lock;
  wire [3:0]    dut_dram0_readAddress_bits_cache;
  wire [2:0]    dut_dram0_readAddress_bits_prot;
  wire [3:0]    dut_dram0_readAddress_bits_qos;
  wire          dut_dram0_readData_ready;
  logic         dut_dram0_readData_valid;
  logic  [5:0]  dut_dram0_readData_bits_id;
  logic  [63:0] dut_dram0_readData_bits_data;
  logic  [1:0]  dut_dram0_readData_bits_resp;
  logic         dut_dram0_readData_bits_last;
  logic         dut_dram1_writeAddress_ready;
  wire          dut_dram1_writeAddress_valid;
  wire [5:0]    dut_dram1_writeAddress_bits_id;
  wire [31:0]   dut_dram1_writeAddress_bits_addr;
  wire [7:0]    dut_dram1_writeAddress_bits_len;
  wire [2:0]    dut_dram1_writeAddress_bits_size;
  wire [1:0]    dut_dram1_writeAddress_bits_burst;
  wire [1:0]    dut_dram1_writeAddress_bits_lock;
  wire [3:0]    dut_dram1_writeAddress_bits_cache;
  wire [2:0]    dut_dram1_writeAddress_bits_prot;
  wire [3:0]    dut_dram1_writeAddress_bits_qos;
  logic         dut_dram1_writeData_ready;
  wire          dut_dram1_writeData_valid;
  wire [5:0]    dut_dram1_writeData_bits_id;
  wire [63:0]   dut_dram1_writeData_bits_data;
  wire [7:0]    dut_dram1_writeData_bits_strb;
  wire          dut_dram1_writeData_bits_last;
  wire          dut_dram1_writeResponse_ready;
  logic         dut_dram1_writeResponse_valid;
  logic  [5:0]  dut_dram1_writeResponse_bits_id;
  logic  [1:0]  dut_dram1_writeResponse_bits_resp;
  logic         dut_dram1_readAddress_ready;
  wire          dut_dram1_readAddress_valid;
  wire [5:0]    dut_dram1_readAddress_bits_id;
  wire [31:0]   dut_dram1_readAddress_bits_addr;
  wire [7:0]    dut_dram1_readAddress_bits_len;
  wire [2:0]    dut_dram1_readAddress_bits_size;
  wire [1:0]    dut_dram1_readAddress_bits_burst;
  wire [1:0]    dut_dram1_readAddress_bits_lock;
  wire [3:0]    dut_dram1_readAddress_bits_cache;
  wire [2:0]    dut_dram1_readAddress_bits_prot;
  wire [3:0]    dut_dram1_readAddress_bits_qos;
  wire          dut_dram1_readData_ready;
  logic         dut_dram1_readData_valid;
  logic  [5:0]  dut_dram1_readData_bits_id;
  logic  [63:0] dut_dram1_readData_bits_data;
  logic  [1:0]  dut_dram1_readData_bits_resp;
  logic         dut_dram1_readData_bits_last;
  wire          dut_error;
  logic         dut_sample_ready;
  wire          dut_sample_valid;
  wire          dut_sample_bits_last;
  wire          dut_sample_bits_bits_flags_instruction_ready;
  wire          dut_sample_bits_bits_flags_instruction_valid;
  wire          dut_sample_bits_bits_flags_memPortA_ready;
  wire          dut_sample_bits_bits_flags_memPortA_valid;
  wire          dut_sample_bits_bits_flags_memPortB_ready;
  wire          dut_sample_bits_bits_flags_memPortB_valid;
  wire          dut_sample_bits_bits_flags_dram0_ready;
  wire          dut_sample_bits_bits_flags_dram0_valid;
  wire          dut_sample_bits_bits_flags_dram1_ready;
  wire          dut_sample_bits_bits_flags_dram1_valid;
  wire          dut_sample_bits_bits_flags_dataflow_ready;
  wire          dut_sample_bits_bits_flags_dataflow_valid;
  wire          dut_sample_bits_bits_flags_acc_ready;
  wire          dut_sample_bits_bits_flags_acc_valid;
  wire          dut_sample_bits_bits_flags_array_ready;
  wire          dut_sample_bits_bits_flags_array_valid;
  wire [31:0]   dut_sample_bits_bits_programCounter;

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

  AXIWrapperTCU dut (
    .clock(dut_clock),
    .reset(dut_reset),
    .instruction_ready(dut_instruction_ready),
    .instruction_valid(dut_instruction_valid),
    .instruction_bits_opcode(dut_instruction_bits_opcode),
    .instruction_bits_flags(dut_instruction_bits_flags),
    .instruction_bits_arguments(dut_instruction_bits_arguments),
    .status_ready(dut_status_ready),
    .status_valid(dut_status_valid),
    .status_bits_last(dut_status_bits_last),
    .status_bits_bits_opcode(dut_status_bits_bits_opcode),
    .status_bits_bits_flags(dut_status_bits_bits_flags),
    .status_bits_bits_arguments(dut_status_bits_bits_arguments),
    .dram0_writeAddress_ready(dut_dram0_writeAddress_ready),
    .dram0_writeAddress_valid(dut_dram0_writeAddress_valid),
    .dram0_writeAddress_bits_id(dut_dram0_writeAddress_bits_id),
    .dram0_writeAddress_bits_addr(dut_dram0_writeAddress_bits_addr),
    .dram0_writeAddress_bits_len(dut_dram0_writeAddress_bits_len),
    .dram0_writeAddress_bits_size(dut_dram0_writeAddress_bits_size),
    .dram0_writeAddress_bits_burst(dut_dram0_writeAddress_bits_burst),
    .dram0_writeAddress_bits_lock(dut_dram0_writeAddress_bits_lock),
    .dram0_writeAddress_bits_cache(dut_dram0_writeAddress_bits_cache),
    .dram0_writeAddress_bits_prot(dut_dram0_writeAddress_bits_prot),
    .dram0_writeAddress_bits_qos(dut_dram0_writeAddress_bits_qos),
    .dram0_writeData_ready(dut_dram0_writeData_ready),
    .dram0_writeData_valid(dut_dram0_writeData_valid),
    .dram0_writeData_bits_id(dut_dram0_writeData_bits_id),
    .dram0_writeData_bits_data(dut_dram0_writeData_bits_data),
    .dram0_writeData_bits_strb(dut_dram0_writeData_bits_strb),
    .dram0_writeData_bits_last(dut_dram0_writeData_bits_last),
    .dram0_writeResponse_ready(dut_dram0_writeResponse_ready),
    .dram0_writeResponse_valid(dut_dram0_writeResponse_valid),
    .dram0_writeResponse_bits_id(dut_dram0_writeResponse_bits_id),
    .dram0_writeResponse_bits_resp(dut_dram0_writeResponse_bits_resp),
    .dram0_readAddress_ready(dut_dram0_readAddress_ready),
    .dram0_readAddress_valid(dut_dram0_readAddress_valid),
    .dram0_readAddress_bits_id(dut_dram0_readAddress_bits_id),
    .dram0_readAddress_bits_addr(dut_dram0_readAddress_bits_addr),
    .dram0_readAddress_bits_len(dut_dram0_readAddress_bits_len),
    .dram0_readAddress_bits_size(dut_dram0_readAddress_bits_size),
    .dram0_readAddress_bits_burst(dut_dram0_readAddress_bits_burst),
    .dram0_readAddress_bits_lock(dut_dram0_readAddress_bits_lock),
    .dram0_readAddress_bits_cache(dut_dram0_readAddress_bits_cache),
    .dram0_readAddress_bits_prot(dut_dram0_readAddress_bits_prot),
    .dram0_readAddress_bits_qos(dut_dram0_readAddress_bits_qos),
    .dram0_readData_ready(dut_dram0_readData_ready),
    .dram0_readData_valid(dut_dram0_readData_valid),
    .dram0_readData_bits_id(dut_dram0_readData_bits_id),
    .dram0_readData_bits_data(dut_dram0_readData_bits_data),
    .dram0_readData_bits_resp(dut_dram0_readData_bits_resp),
    .dram0_readData_bits_last(dut_dram0_readData_bits_last),
    .dram1_writeAddress_ready(dut_dram1_writeAddress_ready),
    .dram1_writeAddress_valid(dut_dram1_writeAddress_valid),
    .dram1_writeAddress_bits_id(dut_dram1_writeAddress_bits_id),
    .dram1_writeAddress_bits_addr(dut_dram1_writeAddress_bits_addr),
    .dram1_writeAddress_bits_len(dut_dram1_writeAddress_bits_len),
    .dram1_writeAddress_bits_size(dut_dram1_writeAddress_bits_size),
    .dram1_writeAddress_bits_burst(dut_dram1_writeAddress_bits_burst),
    .dram1_writeAddress_bits_lock(dut_dram1_writeAddress_bits_lock),
    .dram1_writeAddress_bits_cache(dut_dram1_writeAddress_bits_cache),
    .dram1_writeAddress_bits_prot(dut_dram1_writeAddress_bits_prot),
    .dram1_writeAddress_bits_qos(dut_dram1_writeAddress_bits_qos),
    .dram1_writeData_ready(dut_dram1_writeData_ready),
    .dram1_writeData_valid(dut_dram1_writeData_valid),
    .dram1_writeData_bits_id(dut_dram1_writeData_bits_id),
    .dram1_writeData_bits_data(dut_dram1_writeData_bits_data),
    .dram1_writeData_bits_strb(dut_dram1_writeData_bits_strb),
    .dram1_writeData_bits_last(dut_dram1_writeData_bits_last),
    .dram1_writeResponse_ready(dut_dram1_writeResponse_ready),
    .dram1_writeResponse_valid(dut_dram1_writeResponse_valid),
    .dram1_writeResponse_bits_id(dut_dram1_writeResponse_bits_id),
    .dram1_writeResponse_bits_resp(dut_dram1_writeResponse_bits_resp),
    .dram1_readAddress_ready(dut_dram1_readAddress_ready),
    .dram1_readAddress_valid(dut_dram1_readAddress_valid),
    .dram1_readAddress_bits_id(dut_dram1_readAddress_bits_id),
    .dram1_readAddress_bits_addr(dut_dram1_readAddress_bits_addr),
    .dram1_readAddress_bits_len(dut_dram1_readAddress_bits_len),
    .dram1_readAddress_bits_size(dut_dram1_readAddress_bits_size),
    .dram1_readAddress_bits_burst(dut_dram1_readAddress_bits_burst),
    .dram1_readAddress_bits_lock(dut_dram1_readAddress_bits_lock),
    .dram1_readAddress_bits_cache(dut_dram1_readAddress_bits_cache),
    .dram1_readAddress_bits_prot(dut_dram1_readAddress_bits_prot),
    .dram1_readAddress_bits_qos(dut_dram1_readAddress_bits_qos),
    .dram1_readData_ready(dut_dram1_readData_ready),
    .dram1_readData_valid(dut_dram1_readData_valid),
    .dram1_readData_bits_id(dut_dram1_readData_bits_id),
    .dram1_readData_bits_data(dut_dram1_readData_bits_data),
    .dram1_readData_bits_resp(dut_dram1_readData_bits_resp),
    .dram1_readData_bits_last(dut_dram1_readData_bits_last),
    .error(dut_error),
    .sample_ready(dut_sample_ready),
    .sample_valid(dut_sample_valid),
    .sample_bits_last(dut_sample_bits_last),
    .sample_bits_bits_flags_instruction_ready(dut_sample_bits_bits_flags_instruction_ready),
    .sample_bits_bits_flags_instruction_valid(dut_sample_bits_bits_flags_instruction_valid),
    .sample_bits_bits_flags_memPortA_ready(dut_sample_bits_bits_flags_memPortA_ready),
    .sample_bits_bits_flags_memPortA_valid(dut_sample_bits_bits_flags_memPortA_valid),
    .sample_bits_bits_flags_memPortB_ready(dut_sample_bits_bits_flags_memPortB_ready),
    .sample_bits_bits_flags_memPortB_valid(dut_sample_bits_bits_flags_memPortB_valid),
    .sample_bits_bits_flags_dram0_ready(dut_sample_bits_bits_flags_dram0_ready),
    .sample_bits_bits_flags_dram0_valid(dut_sample_bits_bits_flags_dram0_valid),
    .sample_bits_bits_flags_dram1_ready(dut_sample_bits_bits_flags_dram1_ready),
    .sample_bits_bits_flags_dram1_valid(dut_sample_bits_bits_flags_dram1_valid),
    .sample_bits_bits_flags_dataflow_ready(dut_sample_bits_bits_flags_dataflow_ready),
    .sample_bits_bits_flags_dataflow_valid(dut_sample_bits_bits_flags_dataflow_valid),
    .sample_bits_bits_flags_acc_ready(dut_sample_bits_bits_flags_acc_ready),
    .sample_bits_bits_flags_acc_valid(dut_sample_bits_bits_flags_acc_valid),
    .sample_bits_bits_flags_array_ready(dut_sample_bits_bits_flags_array_ready),
    .sample_bits_bits_flags_array_valid(dut_sample_bits_bits_flags_array_valid),
    .sample_bits_bits_programCounter(dut_sample_bits_bits_programCounter)
  );
  // DUT inputs.
  assign dut_clock = clk;
  assign dut_reset = reset; // The reset is active-low for synthesis, even if the name conveys active-high! This verilog design was taken from a simulation though, so the reset is active-high.
  assign dut_instruction_valid = tvalid;
  assign dut_instruction_bits_opcode = tdata[63:60];
  assign dut_instruction_bits_flags = tdata[59:56];
  assign dut_instruction_bits_arguments = tdata[55:8];
  assign dut_status_ready = status_ready;
  assign dut_dram0_writeAddress_ready = dram0_s_awready;
  assign dut_dram0_writeData_ready = dram0_s_wready;
  assign dut_dram0_writeResponse_valid = dram0_s_bvalid;
  assign dut_dram0_writeResponse_bits_id = dram0_s_bid;
  assign dut_dram0_writeResponse_bits_resp = dram0_s_bresp;
  assign dut_dram0_readAddress_ready = dram0_s_arready;
  assign dut_dram0_readData_valid = dram0_s_rready;
  assign dut_dram0_readData_bits_id = dram0_s_rid;
  assign dut_dram0_readData_bits_data = dram0_s_rdata;
  assign dut_dram0_readData_bits_resp = dram0_s_rresp;
  assign dut_dram0_readData_bits_last = dram0_s_rlast;
  assign dut_dram1_writeAddress_ready = dram1_s_awready;
  assign dut_dram1_writeData_ready = dram1_s_wready;
  assign dut_dram1_writeResponse_valid = dram1_s_bvalid;
  assign dut_dram1_writeResponse_bits_id = dram1_s_bid;
  assign dut_dram1_writeResponse_bits_resp = dram1_s_bresp;
  assign dut_dram1_readAddress_ready = dram1_s_arready;
  assign dut_dram1_readData_valid = dram1_s_rready;
  assign dut_dram1_readData_bits_id = dram1_s_rid;
  assign dut_dram1_readData_bits_data = dram1_s_rdata;
  assign dut_dram1_readData_bits_resp = dram1_s_rresp;
  assign dut_dram1_readData_bits_last = dram1_s_rlast;
  assign dut_sample_ready = sample_ready;

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
  assign dram0_s_awvalid = dut_dram0_writeAddress_valid;
  assign dram0_s_awid =  dut_dram0_writeAddress_bits_id;
  assign dram0_s_awaddr = dut_dram0_writeAddress_bits_addr;
  assign dram0_s_awlen = dut_dram0_writeAddress_bits_len;
  assign dram0_s_awsize = dut_dram0_writeAddress_bits_size;
  assign dram0_s_awburst = dut_dram0_writeAddress_bits_burst;
  assign dram0_s_awlock = dut_dram0_writeAddress_bits_lock;
  assign dram0_s_awcache = dut_dram0_writeAddress_bits_cache;
  assign dram0_s_awprot = dut_dram0_writeAddress_bits_prot;
  assign dram0_s_awqos = dut_dram0_writeAddress_bits_qos;
  assign dram0_s_wvalid = dut_dram0_writeData_valid;
  assign dram0_s_wid = dut_dram0_writeData_bits_id;
  assign dram0_s_wdata = dut_dram0_writeData_bits_data;
  assign dram0_s_wstrb = dut_dram0_writeData_bits_strb;
  assign dram0_s_wlast = dut_dram0_writeData_bits_last;
  assign dram0_s_bready = dut_dram0_writeResponse_ready;
  assign dram0_s_arvalid = dut_dram0_readAddress_valid;
  assign dram0_s_arid = dut_dram0_readAddress_bits_id;
  assign dram0_s_araddr = dut_dram0_readAddress_bits_addr;
  assign dram0_s_arlen = dut_dram0_readAddress_bits_len;
  assign dram0_s_arsize = dut_dram0_readAddress_bits_size;
  assign dram0_s_arburst = dut_dram0_readAddress_bits_burst;
  assign dram0_s_arlock = dut_dram0_readAddress_bits_lock;
  assign dram0_s_arcache = dut_dram0_readAddress_bits_cache;
  assign dram0_s_arprot = dut_dram0_readAddress_bits_prot;
  assign dram0_s_arqos = dut_dram0_readAddress_bits_qos;
  assign dram0_s_rready = dut_dram0_readData_ready;

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
  assign dram1_s_awvalid = dut_dram1_writeAddress_valid;
  assign dram1_s_awid =  dut_dram1_writeAddress_bits_id;
  assign dram1_s_awaddr = dut_dram1_writeAddress_bits_addr;
  assign dram1_s_awlen = dut_dram1_writeAddress_bits_len;
  assign dram1_s_awsize = dut_dram1_writeAddress_bits_size;
  assign dram1_s_awburst = dut_dram1_writeAddress_bits_burst;
  assign dram1_s_awlock = dut_dram1_writeAddress_bits_lock;
  assign dram1_s_awcache = dut_dram1_writeAddress_bits_cache;
  assign dram1_s_awprot = dut_dram1_writeAddress_bits_prot;
  assign dram1_s_awqos = dut_dram1_writeAddress_bits_qos;
  assign dram1_s_wvalid = dut_dram1_writeData_valid;
  assign dram1_s_wid = dut_dram1_writeData_bits_id;
  assign dram1_s_wdata = dut_dram1_writeData_bits_data;
  assign dram1_s_wstrb = dut_dram1_writeData_bits_strb;
  assign dram1_s_wlast = dut_dram1_writeData_bits_last;
  assign dram1_s_bready = dut_dram1_writeResponse_ready;
  assign dram1_s_arvalid = dut_dram1_readAddress_valid;
  assign dram1_s_arid = dut_dram1_readAddress_bits_id;
  assign dram1_s_araddr = dut_dram1_readAddress_bits_addr;
  assign dram1_s_arlen = dut_dram1_readAddress_bits_len;
  assign dram1_s_arsize = dut_dram1_readAddress_bits_size;
  assign dram1_s_arburst = dut_dram1_readAddress_bits_burst;
  assign dram1_s_arlock = dut_dram1_readAddress_bits_lock;
  assign dram1_s_arcache = dut_dram1_readAddress_bits_cache;
  assign dram1_s_arprot = dut_dram1_readAddress_bits_prot;
  assign dram1_s_arqos = dut_dram1_readAddress_bits_qos;
  assign dram1_s_rready = dut_dram1_readData_ready;

  // State machine for sending instructions to the DUT.
  typedef enum {
    STATE_IDLE = 0,
    STATE_RESET = 1,
    STATE_SEND_NOPS = 2,
    STATE_MOVE_DATA = 3,
    STATE_SEND_INVALID_INSTR = 4,
    STATE_WAIT_ERROR = 5,
    STATE_CHECK_END = 6,
    STATE_END = 100
  } state_t;

  state_t reg_state = STATE_IDLE, next_state;
  int reg_nop_cnt, next_nop_cnt;
  int reg_iteration_cnt, next_iteration_cnt;

  assign tready = dut_instruction_ready;
  assign status_ready = 1;
  assign sample_ready = 1;

  always_ff @(posedge clk) begin
    reg_state = next_state;
    reg_nop_cnt = next_nop_cnt;
    reg_iteration_cnt = next_iteration_cnt;

    if (reg_state == STATE_END) begin
      $display("done");
      $finish;
    end
  end

  always_comb begin
    // Default values.
    next_state = reg_state;
    next_nop_cnt = reg_nop_cnt;
    next_iteration_cnt = reg_iteration_cnt;

    reset = 0;
    tdata = 0;
    tvalid = 0;
    tlast = 0;

    case (reg_state)
      STATE_IDLE:
      begin
        next_iteration_cnt = 0;
        next_state = STATE_RESET;
      end

      STATE_RESET:
      begin
        reset = 1;
        next_nop_cnt = 0;
        next_state = STATE_SEND_NOPS;
      end

      STATE_SEND_NOPS:
      begin
        tvalid = 1;
        tdata = INSTR_NOP;
        if (tready == 1) begin
          next_nop_cnt = reg_nop_cnt + 1;
          if (reg_nop_cnt == C_NUM_NOPS - 1) begin
            next_state = STATE_MOVE_DATA;
          end
        end
      end

      STATE_MOVE_DATA:
      begin
        tvalid = 1;
        tdata = INSTR_MOVE_DATA_DRAM1_TO_MEM;
        if (tready == 1) begin
          next_state = STATE_SEND_INVALID_INSTR;
        end
      end

      STATE_SEND_INVALID_INSTR:
      begin
        tvalid = 1;
        tdata = INSTR_INVALID;
        if (tready == 1) begin
          next_state = STATE_WAIT_ERROR;
        end
      end

      STATE_WAIT_ERROR:
      begin
        if (dut_error == 1) begin
          next_state = STATE_CHECK_END;
        end
      end

      STATE_CHECK_END:
      begin
        if (reg_iteration_cnt == C_NUM_ITERATIONS - 1) begin
          next_state = STATE_END;
        end else begin
          next_iteration_cnt = reg_iteration_cnt + 1;
          next_state = STATE_RESET;
        end
      end

      STATE_END:
      begin
      end
    endcase
  end

endmodule