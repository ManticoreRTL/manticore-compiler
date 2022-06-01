module Parity (
    input wire [35 : 0] IN,
    output wire         OUT
);

  logic [5 : 0] tmp;

  genvar i;
  generate
    for (i = 0; i < 6; i = i + 1) begin
      assign tmp[i] = IN[6*i] ^ IN[6*i + 1] ^ IN[6*i + 2] ^ IN[6*i + 3] ^ IN[6*i + 4] ^ IN[6*i + 5];
    end
  endgenerate

  assign OUT = tmp[0] ^ tmp[1] ^ tmp[2] ^ tmp[3] ^ tmp[4] ^ tmp[5];

endmodule