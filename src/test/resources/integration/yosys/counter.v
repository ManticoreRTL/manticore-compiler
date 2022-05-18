module Counter(input wire clk,
               input wire ld,
               input wire [15:0] init,
               output wire [15:0] count
            );
    reg [15:0] cnt = 0;
    always @(posedge clk) begin
        if (ld)
            cnt <= init;
        else
            cnt <= cnt + 1;
    end
    assign count = cnt;

endmodule

module Main(input wire clk);

    reg [15:0] iter = 0;
    reg ld = 0;
    reg [15:0] init = 16'd71;
    wire [15:0] count;
    Counter dut(clk, ld, init, count);

    always @(posedge clk) begin
        $display("@%d: Count = %d", iter, count);
        if (iter == 200) begin
            $finish;
        end
        if (iter[4:0] == 0) begin
            ld = 1;
            init = (init << 3) ^ (iter >> 2);
        end else begin
            ld = 0;
            init = 16'd871;
        end
        iter <= iter + 1;
    end
endmodule
