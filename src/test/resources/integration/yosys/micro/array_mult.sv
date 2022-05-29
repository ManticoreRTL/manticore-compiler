

module ArrayMultiplier #(
    WIDTH = 8
) (
    input wire clock,
    input wire [WIDTH - 1 : 0] X,
    input wire [WIDTH - 1 : 0] Y,
    input wire start,
    output wire [2 * WIDTH - 1 : 0] P,
    output wire done
);


  logic [2 * WIDTH - 1 : 0] xreg[0 : 2 * WIDTH - 2];
  logic [2 * WIDTH - 1 : 0] busy_stage;
  logic [2 * WIDTH - 1 : 0] yreg[0 : 2 * WIDTH - 2];
  logic [2 * WIDTH - 1 : 0] preg[0 : 2 * WIDTH - 1];

  integer i;

  always_ff @(posedge clock) begin

    busy_stage <= (start << (2 * WIDTH - 1)) | (busy_stage >> 1);
    xreg[0] <= X;
    yreg[0] <= Y;
    for (i = 1; i < 2 * WIDTH - 1; i = i + 1) begin
      xreg[i] <= xreg[i-1];
      yreg[i] <= yreg[i-1];
    end

    preg[0] <= X[0] ? Y : 0;

    for (i = 1; i < 2 * WIDTH; i = i + 1) begin
      preg[i] <= (xreg[i-1][i] ? (yreg[i-1] << i) : 0) + preg[i-1];
    end
  end
  assign P = preg[2*WIDTH-1];
  assign done = busy_stage[0];
endmodule

