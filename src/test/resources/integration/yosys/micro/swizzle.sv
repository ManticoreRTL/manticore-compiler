module Swizzle #(
    WIDTH = 8
) (
    input wire [WIDTH - 1 : 0] IN,
    output wire [WIDTH - 1 : 0] OUT
);

  logic [WIDTH - 1 : 0] tmp;

  genvar i;
  generate
    for (i=0; i<WIDTH; i=i+1) begin : ff_gen
      assign tmp[i] = IN[WIDTH-1-i];
    end
  endgenerate

  assign OUT = tmp;

endmodule
