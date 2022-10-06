module FullWrite(input wire clock, input [31:0] din, input wire [4:0] waddr, input wire [4:0] raddr, output wire dout);
    reg [31:0] storage [0 : 31];
    assign dout = storage[raddr];
    always @(posedge clock) begin
        storage[waddr] = din;
    end
endmodule