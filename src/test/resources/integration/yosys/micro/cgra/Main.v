module Main(input wire clk);

    wire clock = clk;

    reg [4:0] counter = 0;
    wire error;
    always @(posedge clock) begin
        counter <= counter + 1;
        if (error)
            $stop;
        if (counter > 20)
            $finish;

    end

    DataflowTester dut (.clock(clock), .reset(counter < 3), .io_error(error));

endmodule