module Main(input wire clock);

  localparam C_NUM_DUTS = 12;
  localparam C_NUM_ITERATIONS = 3;

  localparam C_IMAGE_WIDTH = 100;
  localparam C_IMAGE_HEIGHT = 75;

  localparam C_RED_NUM_WORDS = 215;
  localparam C_RED_DATA_HEX_FILE = "hex/red-100x75_data.hex";
  localparam C_RED_STRB_HEX_FILE = "hex/red-100x75_strb.hex";

  localparam C_GREEN_NUM_WORDS = 220;
  localparam C_GREEN_DATA_HEX_FILE = "hex/green-100x75_data.hex";
  localparam C_GREEN_STRB_HEX_FILE = "hex/green-100x75_strb.hex";

  localparam C_BLUE_NUM_WORDS = 220;
  localparam C_BLUE_DATA_HEX_FILE = "hex/blue-100x75_data.hex";
  localparam C_BLUE_STRB_HEX_FILE = "hex/blue-100x75_strb.hex";

  logic [C_NUM_DUTS - 1 : 0] start;
  logic [C_NUM_DUTS - 1 : 0] done_ack;
  wire  [C_NUM_DUTS - 1 : 0] done;
  wire  [31 : 0]             sum_r [0 : C_NUM_DUTS - 1];
  wire  [31 : 0]             sum_g [0 : C_NUM_DUTS - 1];
  wire  [31 : 0]             sum_b [0 : C_NUM_DUTS - 1];

  genvar i;
  generate
    for (i = 0; i < C_NUM_DUTS; i = i + 1) begin
      if (i % 3 == 0) begin
        ColorChecker #(
          .IMAGE_WIDTH(C_IMAGE_WIDTH),
          .IMAGE_HEIGHT(C_IMAGE_HEIGHT),
          .DATA_HEX_FILE(C_RED_DATA_HEX_FILE),
          .STRB_HEX_FILE(C_RED_STRB_HEX_FILE),
          .NUM_WORDS(C_RED_NUM_WORDS)
        ) cc (
          .clock(clock),
          .start(start[i]),
          .done_ack(done_ack[i]),
          .done(done[i]),
          .sum_r(sum_r[i]),
          .sum_g(sum_g[i]),
          .sum_b(sum_b[i])
        );
      end else if (i % 3 == 1) begin
        ColorChecker #(
          .IMAGE_WIDTH(C_IMAGE_WIDTH),
          .IMAGE_HEIGHT(C_IMAGE_HEIGHT),
          .DATA_HEX_FILE(C_GREEN_DATA_HEX_FILE),
          .STRB_HEX_FILE(C_GREEN_STRB_HEX_FILE),
          .NUM_WORDS(C_GREEN_NUM_WORDS)
        ) cc (
          .clock(clock),
          .start(start[i]),
          .done_ack(done_ack[i]),
          .done(done[i]),
          .sum_r(sum_r[i]),
          .sum_g(sum_g[i]),
          .sum_b(sum_b[i])
        );
      end else if (i % 3 == 2) begin
        ColorChecker #(
          .IMAGE_WIDTH(C_IMAGE_WIDTH),
          .IMAGE_HEIGHT(C_IMAGE_HEIGHT),
          .DATA_HEX_FILE(C_BLUE_DATA_HEX_FILE),
          .STRB_HEX_FILE(C_BLUE_STRB_HEX_FILE),
          .NUM_WORDS(C_BLUE_NUM_WORDS)
        ) cc (
          .clock(clock),
          .start(start[i]),
          .done_ack(done_ack[i]),
          .done(done[i]),
          .sum_r(sum_r[i]),
          .sum_g(sum_g[i]),
          .sum_b(sum_b[i])
        );
      end
    end
  endgenerate

  typedef enum {
    STATE_IDLE = 0,
    STATE_START = 1,
    STATE_WAIT_ALL_DONE = 2,
    STATE_DONE_ACK = 3,
    STATE_CHECK_END = 4,
    STATE_END = 5
  } state_t;

  state_t reg_state = STATE_IDLE, next_state;
  int reg_cnt, next_cnt;
  int reg_sum_r, next_sum_r;
  int reg_sum_g, next_sum_g;
  int reg_sum_b, next_sum_b;

  // State registers.
  always_ff @(posedge clock) begin
    reg_state <= next_state;
    reg_cnt <= next_cnt;
    reg_sum_r <= next_sum_r;
    reg_sum_g <= next_sum_g;
    reg_sum_b <= next_sum_b;

    if (reg_state == STATE_CHECK_END) begin
      $display("[%d] sum_r = %d", reg_cnt, reg_sum_r);
      $display("[%d] sum_g = %d", reg_cnt, reg_sum_g);
      $display("[%d] sum_b = %d", reg_cnt, reg_sum_b);
    end else if (reg_state == STATE_END) begin
      $display("finished");
      $finish;
    end
  end

  // Next state logic.
  always_comb begin
    // Default values.
    next_state = reg_state;
    next_cnt = reg_cnt;
    next_sum_r = reg_sum_r;
    next_sum_g = reg_sum_g;
    next_sum_b = reg_sum_b;
    start = 0;
    done_ack = 0;

    case (reg_state)
      STATE_IDLE:
      begin
        next_state = STATE_START;
        next_cnt = 0;
      end

      STATE_START:
      begin
        start = {C_NUM_DUTS{1'b1}};
        next_state = STATE_WAIT_ALL_DONE;
      end

      STATE_WAIT_ALL_DONE:
      begin
        if (&done) begin
          next_sum_r = reg_sum_r + sum_r[reg_cnt];
          next_sum_g = reg_sum_g + sum_g[reg_cnt];
          next_sum_b = reg_sum_b + sum_b[reg_cnt];
          next_state = STATE_DONE_ACK;
        end
      end

      STATE_DONE_ACK:
      begin
        done_ack = {C_NUM_DUTS{1'b1}};
        next_state = STATE_CHECK_END;
      end

      STATE_CHECK_END:
      begin
        next_cnt = reg_cnt + 1;
        if (reg_cnt == C_NUM_ITERATIONS - 1) begin
          next_state = STATE_END;
        end else begin
          next_state = STATE_START;
        end
      end

      STATE_END:
      begin
      end
    endcase
  end

endmodule

module ColorChecker #(
  parameter IMAGE_WIDTH,
  parameter IMAGE_HEIGHT,
  parameter DATA_HEX_FILE,
  parameter STRB_HEX_FILE,
  parameter NUM_WORDS
) (
  input               clock,
  input               start,
  input               done_ack,
  output reg          done,
  output reg [31 : 0] sum_r,
  output reg [31 : 0] sum_g,
  output reg [31 : 0] sum_b
);

  logic [31 : 0] DATA [0 : NUM_WORDS - 1];
  logic [ 3 : 0] STRB [0 : NUM_WORDS - 1];

  initial begin
    $readmemh(DATA_HEX_FILE, DATA);
    $readmemh(STRB_HEX_FILE, STRB);
  end

  logic         rst_i = 0;
  logic         inport_valid_i;
  logic [ 31:0] inport_data_i;
  logic [  3:0] inport_strb_i;
  logic         inport_last_i;
  logic         outport_accept_i;
  wire          inport_accept_o;
  wire          outport_valid_o;
  wire [ 15:0]  outport_width_o;
  wire [ 15:0]  outport_height_o;
  wire [ 15:0]  outport_pixel_x_o;
  wire [ 15:0]  outport_pixel_y_o;
  wire [  7:0]  outport_pixel_r_o;
  wire [  7:0]  outport_pixel_g_o;
  wire [  7:0]  outport_pixel_b_o;
  wire          idle_o;

  jpeg_core dut(
    .clk_i(clock),
    .rst_i(rst_i),
    .inport_valid_i(inport_valid_i),
    .inport_data_i(inport_data_i),
    .inport_strb_i(inport_strb_i),
    .inport_last_i(inport_last_i),
    .outport_accept_i(outport_accept_i),
    .inport_accept_o(inport_accept_o),
    .outport_valid_o(outport_valid_o),
    .outport_width_o(outport_width_o),
    .outport_height_o(outport_height_o),
    .outport_pixel_x_o(outport_pixel_x_o),
    .outport_pixel_y_o(outport_pixel_y_o),
    .outport_pixel_r_o(outport_pixel_r_o),
    .outport_pixel_g_o(outport_pixel_g_o),
    .outport_pixel_b_o(outport_pixel_b_o),
    .idle_o(idle_o)
  );

  // State machine for sending instructions to the DUT.
  typedef enum {
    STATE_IDLE = 0,
    STATE_RESET = 1,
    STATE_WAIT_IDLE = 2,
    STATE_SEND_DATA = 3,
    STATE_END = 4
  } state_t;

  state_t reg_state = STATE_IDLE, next_state;
  int reg_cnt, next_cnt;
  int reg_sum_r, next_sum_r;
  int reg_sum_g, next_sum_g;
  int reg_sum_b, next_sum_b;

  assign sum_r = reg_sum_r;
  assign sum_g = reg_sum_g;
  assign sum_b = reg_sum_b;

  // State registers.
  always_ff @(posedge clock) begin
    reg_state <= next_state;
    reg_cnt <= next_cnt;
    reg_sum_r <= next_sum_r;
    reg_sum_g <= next_sum_g;
    reg_sum_b <= next_sum_b;
  end

  // Next state logic.
  always_comb begin
    // Default values.
    next_state = reg_state;
    rst_i = 0;
    inport_valid_i = 0;
    inport_data_i = 0;
    inport_strb_i = 0;
    inport_last_i = 0;
    outport_accept_i = 0;
    done = 0;
    next_cnt = reg_cnt;

    case (reg_state)
      STATE_IDLE:
      begin
        if (start) begin
          next_state = STATE_RESET;
        end
      end

      STATE_RESET:
      begin
        next_state = STATE_WAIT_IDLE;
        rst_i = 1;
      end

      STATE_WAIT_IDLE:
      begin
        next_cnt = 0;
        if (idle_o) begin
          next_state = STATE_SEND_DATA;
        end
      end

      STATE_SEND_DATA:
      begin
        outport_accept_i = 1;
        inport_valid_i = 1;
        inport_data_i = DATA[reg_cnt];
        inport_strb_i = STRB[reg_cnt];
        // Input data accepted by DUT.
        if (inport_accept_o) begin
          next_cnt = reg_cnt + 1;
        end
        if (reg_cnt == NUM_WORDS - 1) begin
          inport_last_i = 1;
          next_state = STATE_END;
        end
      end

      STATE_END:
      begin
        done = 1;
        if (done_ack) begin
          next_state = STATE_IDLE;
        end
      end
    endcase
  end

  always_comb begin
    next_sum_r = reg_sum_r;
    next_sum_g = reg_sum_g;
    next_sum_b = reg_sum_b;

    if (reg_state == STATE_RESET) begin
      next_sum_r = 0;
      next_sum_g = 0;
      next_sum_b = 0;
    end else if (outport_valid_o) begin
      next_sum_r = reg_sum_r + 1;
      next_sum_g = reg_sum_g + 1;
      next_sum_b = reg_sum_b + 1;
    end
  end

endmodule