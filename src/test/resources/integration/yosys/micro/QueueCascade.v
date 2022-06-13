// ----------------------------------------------------------------------------
// -- This file is generated automatically by StreamBlocks
// -- FIFO Queue from used by Vivado HLS streaming interface
// -- Modified to include count output, removed clock enables in write & read
// -- Copyright (C) 2015 Xilinx Inc
// ----------------------------------------------------------------------------
module Queue#(parameter
    MEM_STYLE  = "auto",
    DATA_WIDTH = 16,
    ADDR_WIDTH = 6
)(
  input         clock,
  input         reset,
  output        io_enq_ready,
  input         io_enq_valid,
  input  [15:0] io_enq_bits,
  input         io_deq_ready,
  output        io_deq_valid,
  output [15:0] io_deq_bits
);

localparam DEPTH = 1 << ADDR_WIDTH;


//------------------------Local signal-------------------
(* ram_style = MEM_STYLE *)
reg  [DATA_WIDTH-1:0] mem[0:DEPTH-1];
reg  [DATA_WIDTH-1:0] q_buf = 1'b0;
reg  [ADDR_WIDTH-1:0] waddr = 1'b0;
reg  [ADDR_WIDTH-1:0] raddr = 1'b0;
wire [ADDR_WIDTH-1:0] wnext;
wire [ADDR_WIDTH-1:0] rnext;
wire                  push;
wire                  pop;
reg  [ADDR_WIDTH-1:0] usedw = 1'b0;
reg                   full_n = 1'b1;
reg                   empty_n = 1'b0;
reg  [DATA_WIDTH-1:0] q_tmp = 1'b0;
reg                   show_ahead = 1'b0;
reg  [DATA_WIDTH-1:0] dout_buf = 1'b0;
reg                   dout_valid = 1'b0;

//------------------------Body---------------------------
assign io_enq_ready  = full_n;
assign io_deq_valid  = dout_valid;
assign io_deq_bits   = dout_buf;
wire clk = clock;
wire reset_n = ~reset;
wire [DATA_WIDTH - 1 : 0] if_din = io_enq_bits;
wire                      if_write = io_enq_valid;
wire                      if_read = io_deq_ready;
wire                      if_full_n = io_enq_ready;
assign push       = full_n & if_write;
assign pop        = empty_n & (~dout_valid | if_read);
assign wnext      = !push                ? waddr :
                    (waddr == DEPTH - 1) ? 1'b0  :
                    waddr + 1'b1;
assign rnext      = !pop                 ? raddr :
                    (raddr == DEPTH - 1) ? 1'b0  :
                    raddr + 1'b1;


// waddr
always @(posedge clk) begin
    if (reset_n == 1'b0)
        waddr <= 1'b0;
    else
        waddr <= wnext;
end

// raddr
always @(posedge clk) begin
    if (reset_n == 1'b0)
        raddr <= 1'b0;
    else
        raddr <= rnext;
end

// usedw
always @(posedge clk) begin
    if (reset_n == 1'b0)
        usedw <= 1'b0;
    else if (push & ~pop)
        usedw <= usedw + 1'b1;
    else if (~push & pop)
        usedw <= usedw - 1'b1;
end



// full_n
always @(posedge clk) begin
    if (reset_n == 1'b0)
        full_n <= 1'b1;
    else if (push & ~pop)
        full_n <= (usedw != DEPTH - 1);
    else if (~push & pop)
        full_n <= 1'b1;
end

// empty_n
always @(posedge clk) begin
    if (reset_n == 1'b0)
        empty_n <= 1'b0;
    else if (push & ~pop)
        empty_n <= 1'b1;
    else if (~push & pop)
        empty_n <= (usedw != 1'b1);
end

// mem
always @(posedge clk) begin
    if (push)
        mem[waddr] <= if_din;
end

// q_buf
always @(posedge clk) begin
    q_buf <= mem[rnext];
end

// q_tmp
always @(posedge clk) begin
    if (reset_n == 1'b0)
        q_tmp <= 1'b0;
    else if (push)
        q_tmp <= if_din;
end

// show_ahead
always @(posedge clk) begin
    if (reset_n == 1'b0)
        show_ahead <= 1'b0;
    else if (push && usedw == pop)
        show_ahead <= 1'b1;
    else
        show_ahead <= 1'b0;
end

// dout_buf
always @(posedge clk) begin
    if (reset_n == 1'b0)
        dout_buf <= 1'b0;
    else if (pop)
        dout_buf <= show_ahead? q_tmp : q_buf;
end

// dout_valid
always @(posedge clk) begin
    if (reset_n == 1'b0)
        dout_valid <= 1'b0;
    else if (pop)
        dout_valid <= 1'b1;
    else if (if_read)
        dout_valid <= 1'b0;
end

endmodule


module QueueCascade#(parameter LEVELS = 64)(
    input         clock,
    input         reset,
    output        io_enq_ready,
    input         io_enq_valid,
    input  [15:0] io_enq_bits,
    input         io_deq_ready,
    output        io_deq_valid,
    output [15:0] io_deq_bits
);

    genvar i;

    generate

        for (i = 0; i < LEVELS; i = i + 1) begin : QUEUES
            wire [15:0] enq_bits;
            wire enq_valid;
            wire enq_ready;
            wire [15:0] deq_bits;
            wire deq_valid;
            wire deq_ready;

            Queue qut(
                .clock(clock),
                .reset(reset),
                .io_enq_ready(enq_ready),
                .io_enq_valid(enq_valid),
                .io_enq_bits(enq_bits),
                .io_deq_ready(deq_ready),
                .io_deq_valid(deq_valid),
                .io_deq_bits(deq_bits)
            );
            if (i == 0) begin
                assign enq_bits = io_enq_bits;
                assign enq_valid = io_enq_valid;
                assign io_enq_ready = enq_ready;
            end else begin
                assign enq_bits = QUEUES[i - 1].deq_bits;
                assign enq_valid = QUEUES[i - 1].deq_valid;
                assign QUEUES[i - 1].deq_ready = enq_ready;
            end

        end
    endgenerate

    assign QUEUES[LEVELS - 1].deq_ready = io_deq_ready;
    assign io_deq_bits = QUEUES[LEVELS - 1].deq_bits;
    assign io_deq_valid = QUEUES[LEVELS - 1].deq_valid;

endmodule