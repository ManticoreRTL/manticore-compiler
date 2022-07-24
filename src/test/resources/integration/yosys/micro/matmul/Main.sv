module Main(input wire clock);


    localparam DIM = 16;

    localparam TIME_OUT = 100;

    logic [31:0] counter = 0;

    always @(posedge clock) begin
        counter <= counter + 32'b1;
        if (counter > TIME_OUT) begin
            $finish;
        end
    end

    logic [3:0] value_counter = 1;
    logic value_en;
    logic [3:0] weight_counter = 1;
    logic weight_en;
    logic out_valid;
    logic [31:0] value_out;
    MatrixMultiplierStreaming dut(
          .clock(clock),
          .reset(counter < 3),
          .io_value_in_ready(value_en),
          .io_value_in_valid(1'b1),
          .io_value_in_bits(value_counter),
          .io_weight_in_ready(weight_en),
          .io_weight_in_valid(1'b1),
          .io_weight_in_bits(weight_counter),
          .io_value_out_ready(1'b1),
          .io_value_out_valid(out_valid),
          .io_value_out_bits(value_out)
    );

    always @(posedge clock) begin
        if (value_en) value_counter <= value_counter + 1;
        if (weight_en) weight_counter <= weight_counter + 1;
        if (out_valid) begin
            assert(value_out[0] == 0);
        end
    end



endmodule