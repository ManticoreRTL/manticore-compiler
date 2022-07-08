

module TestBench #(parameter NUM_LOOPS = 1000) (input wire clock, input wire [31:0] counter);

    wire         reset = counter < 5;
    wire        io_in_pixel_ready;

    logic  [7:0]  io_in_pixel_bits;
    wire        io_in_imgw_ready;

    logic  [15:0] io_in_imgw_bits;
    wire        io_in_imgh_ready;

    logic  [15:0] io_in_imgh_bits;
    logic         io_out_ready;
    wire        io_out_valid;
    wire [7:0]  io_out_bits;



    logic [7:0] input_image [0:15];
    logic [7:0] output_image [0:3];
    logic [31:0] iteration;

    initial begin
        input_image[0] = 0;
        input_image[1] = 0;
        input_image[2] = 0;
        input_image[3] = 0;

        input_image[4] = 0;
        input_image[5] = 16;
        input_image[6] = 64;
        input_image[7] = 0;

        input_image[8] = 0;
        input_image[9] = 32;
        input_image[10] = 128;
        input_image[11] = 0;

        input_image[12] = 0;
        input_image[13] = 0;
        input_image[14] = 0;
        input_image[15] = 0;


        output_image[0] = 24;
        output_image[1] = 36;
        output_image[2] = 30;
        output_image[3] = 45;

    end

    logic [7:0] input_pointer;
    logic [7:0] output_pointer;

    always_ff @(posedge clock) begin

        if (reset) begin
            iteration <= 0;
            input_pointer <= 0;
            output_pointer <= 0;
        end else begin
            if (io_in_pixel_ready) begin
                input_pointer <= (input_pointer == 8'd15 ? 8'd0 : input_pointer  + 8'd1);
            end

            if (io_out_valid) begin
                if (output_image[output_pointer] != io_out_bits) begin
                    $display("Invalid result!");
                    $stop;
                end
                output_pointer <= (output_pointer == 8'd3 ? 8'd0 : output_pointer + 8'd1);
                iteration <= (output_pointer == 8'd3 ? iteration + 32'd1 : iteration);
            end

            if (iteration == NUM_LOOPS) begin
                $display("Finished after %d cycles", counter);
                $finish;
            end
        end
    end




    Gaussian3x3Kernel dut(
        .clock(clock),
        .reset(reset),
        .io_in_pixel_ready(io_in_pixel_ready),
        .io_in_pixel_valid(~reset),
        .io_in_pixel_bits(input_image[input_pointer]),
        .io_in_imgw_ready(io_in_imgw_ready),
        .io_in_imgw_valid(~reset),
        .io_in_imgw_bits(16'd4),
        .io_in_imgh_ready(io_in_imgh_ready),
        .io_in_imgh_valid(~reset),
        .io_in_imgh_bits(16'd4),
        .io_out_ready(1'b1),
        .io_out_valid(io_out_valid),
        .io_out_bits(io_out_bits)
    );

endmodule