module IndexedPartSelect(input wire [5:0] cnt, output wire [7:0] result);

// Constants defined by the SHA-2 standard.
	localparam Ks = 32'b10101010101010101010101010101010;


    assign result = Ks[cnt -: 8];

endmodule