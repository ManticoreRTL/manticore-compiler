module Main(input wire clock);


    // wire clock = clk;
    reg reset = 0;
    wire [31:0] tohost;


    localparam TIMEOUT = 1000000;
    TileWithMemory dut(
        .clock(clock),
        .reset(reset),
        .io_fromhost_valid(0),
        .io_fromhost_bits(0),
        .io_tohost(tohost)
    );


    reg [31:0] cycle_counter = 0;

    always @(posedge clock) begin
        cycle_counter <= cycle_counter + 1;
        if (cycle_counter < 5) begin
            reset = 1;
        end else begin
            reset = 0;
            if (tohost > 1) begin
                $stop;
            end else if (tohost == 1) begin
                $finish;
            end
            if (cycle_counter >= TIMEOUT) begin
                $stop;
            end
        end
    end

endmodule