

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


module TestBench #(
	parameter XROM_FILE="xrom.hex",
	parameter YROM_FILE="yrom.hex",
	parameter PROM_FILE="prom.hex",
	parameter W = 8,
	parameter TEST_SIZE = 200,

)
(
    input wire clock
);





  logic [    W - 1 : 0] x_rom        [0 : TEST_SIZE - 1];
  logic [    W - 1 : 0] y_rom        [0 : TEST_SIZE - 1];
  logic [2 * W - 1 : 0] p_rom        [0 : TEST_SIZE - 1];

  wire  [    W - 1 : 0] x_val;
  wire  [    W - 1 : 0] y_val;
  wire  [2 * W - 1 : 0] p_val;

  wire                  done;
  logic [       15 : 0] icounter = 0;
  logic [       15 : 0] ocounter = 0;
  ArrayMultiplier #(
      .WIDTH(W)
  ) dut (
      .clock(clock),
      .X(x_val),
      .Y(y_val),
      .P(p_val),
      .start(1'b1),
      .done(done)
  );

  always_ff @(posedge clock) begin
    if (icounter < TEST_SIZE - 1) begin
      icounter <= icounter + 1;
    end
    if (done) begin
      ocounter <= ocounter + 1;
      if (p_val != p_rom[ocounter]) begin
        // $display("@ %d Expected %d * %d = %d but got %d", ocounter, x_rom[ocounter],
        //          y_rom[ocounter], p_rom[ocounter], p_val);
        $stop;
      end
    end
    if (ocounter == TEST_SIZE - 1) begin
    //   $display("@ %d Finished!", ocounter);
      $finish;
    end
  end

  assign x_val = x_rom[icounter];
  assign y_val = y_rom[icounter];
  initial begin
		$readmemh(XROM_FILE, x_rom);
		$readmemh(YROM_FILE, y_rom);
		$readmemh(PROM_FILE, p_rom);
  end

endmodule