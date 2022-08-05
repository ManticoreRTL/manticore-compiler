module Main(input wire clock);

  logic osc_clk; // input
  wire vo_clk; // output
  wire vo_vsync; // output
  wire vo_hsync; // output
  wire vo_blank_; // output
  wire [7:0] vo_r; // output
  wire [7:0] vo_g; // output
  wire [7:0] vo_b; // output
  wire led_green; // output
  wire led_blue; // output

  Pano pano (
    .osc_clk(osc_clk),
    .vo_clk(vo_clk),
    .vo_vsync(vo_vsync),
    .vo_hsync(vo_hsync),
    .vo_blank_(vo_blank_),
    .vo_r(vo_r),
    .vo_g(vo_g),
    .vo_b(vo_b),
    .led_green(led_green),
    .led_blue(led_blue)
  );
  assign osc_clk = clock;

  typedef enum {
    STATE_IDLE = 0
  } state_t;

  state_t reg_state = STATE_IDLE, next_state;
  logic reg_vsync;
  logic reg_hsync;
  logic rising_vsync;
  logic rising_hsync;

  assign rising_vsync = ~reg_vsync && vo_vsync;
  assign rising_hsync = ~reg_hsync && vo_hsync;

  int reg_clk_cnt = 0;
  int reg_num_vsync = 0;
  int reg_num_hsync = 0;
  int reg_num_blank = 0;
  int reg_r_acc, next_r_acc;
  int reg_g_acc, next_g_acc;
  int reg_b_acc, next_b_acc;

  // always_ff @(posedge clock) begin
  //   reg_state <= next_state;
  //   reg_r_acc <= next_r_acc;
  //   reg_g_acc <= next_g_acc;
  //   reg_b_acc <= next_b_acc;
  // end

  always_ff @(posedge clock) begin
    reg_clk_cnt = reg_clk_cnt + 1;

    reg_vsync = vo_vsync;
    reg_hsync = vo_hsync;

    if (rising_vsync) begin
      reg_num_vsync = reg_num_vsync + 1;
      // $display("[%d] reg_num_vsync = %d", reg_clk_cnt, reg_num_vsync);
    end

    if (rising_hsync) begin
      reg_num_hsync = reg_num_hsync + 1;
      // $display("[%d] reg_num_hsync = %d", reg_clk_cnt, reg_num_hsync);
    end

    // There is a bogus vsync at the very beginning, then somewhere after a few hsyncs.
    // Then the real vsyncs appear, hence why I check > 2.
    if (reg_num_vsync > 2) begin
      $display("[%d] reg_num_vsync = %d", reg_clk_cnt, reg_num_vsync);
      $display("[%d] reg_num_hsync = %d", reg_clk_cnt, reg_num_hsync);
      $finish;
    end
  end

endmodule
