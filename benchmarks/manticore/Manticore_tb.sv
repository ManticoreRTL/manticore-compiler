module Main#(
    parameter GMEM_SIZE = 2048,
    GMEM_FILE = "gmem.hex",
    PROGRAM_SIZE = 7,
    RESET_CYCLES = 10

)(input wire clock);


    reg [15:0] cycle_counter = 0;
    wire reset = cycle_counter < RESET_CYCLES;
    reg [15:0] packet_in_data;
    reg [10:0] packet_in_address;
    reg packet_in_valid;
    wire [15:0] packet_out_data;
    wire [10:0] packet_out_address;
    wire packet_out_valid;

    wire periphery_active;

    wire periphery_gmem_access_failure_error;
    wire periphery_exception_error;
    wire [15:0] periphery_exception_id;
    wire periphery_debug_time;
    wire periphery_dynamic_cycle;

    Processor dut(
        .clock(clock),
        .reset(reset),
        .io_packet_in_data(packet_in_data),
        .io_packet_in_address(packet_in_address),
        .io_packet_in_valid(packet_in_valid),
        .io_packet_out_data(packet_out_data),
        .io_packet_out_address(packet_out_address),
        .io_packet_out_valid(packet_out_valid),
        .io_packet_out_xHops(),
        .io_packet_out_yHops(),
        .io_periphery_active(periphery_active),
        .io_periphery_cache_addr(),
        .io_periphery_cache_wdata(),
        .io_periphery_cache_start(),
        .io_periphery_cache_cmd(),
        .io_periphery_cache_rdata(16'b0),
        .io_periphery_cache_done(0),
        .io_periphery_cache_idle(0),
        .io_periphery_gmem_access_failure_error(periphery_gmem_access_failure_error),
        .io_periphery_exception_error(periphery_exception_error),
        .io_periphery_exception_id(periphery_exception_id),
        .io_periphery_debug_time(periphery_debug_time),
        .io_periphery_dynamic_cycle(periphery_dynamic_cycle)
    );

    // BODY_LENGTH, PROGRAM BODY, EPILOGUE_LENGTH, SLEEP_LENGTH, COUNT_DOWN
    localparam BOOT_LOAD_SIZE = 1 + (PROGRAM_SIZE * 4) + 1 + 1 + 1;
    localparam EMIT_BODY_LENGTH = RESET_CYCLES;
    localparam EMIT_EPILOGUE = RESET_CYCLES + (PROGRAM_SIZE * 4) + 1;
    localparam EMIT_SLEEP = RESET_CYCLES + (PROGRAM_SIZE * 4) + 2;
    localparam EMIT_COUNTDOWN = RESET_CYCLES + (PROGRAM_SIZE * 4) + 3;
    reg [15:0] gmem_ptr = 0;
    reg [15:0] gmem [0: GMEM_SIZE - 1];




    enum logic[5:0] {
        sReset, sBoot, sExecute, sFinished
    } pstate, nstate;

    initial begin
        pstate = sReset;
        // $readmemb(GMEM_FILE, gmem);
        gmem[0] = 16'b0000000000000000;
        gmem[1] = 16'b0000000000000111;
        gmem[2] = 16'b0000000000000001;
        gmem[3] = 16'b0000000000000000;
        gmem[4] = 16'b0000000000000000;
        gmem[5] = 16'b0000000000000000;
        gmem[6] = 16'b0000000000010001;
        gmem[7] = 16'b0000000000000000;
        gmem[8] = 16'b0000000000000000;
        gmem[9] = 16'b0000000000000001;
        gmem[10] = 16'b0000000000100001;
        gmem[11] = 16'b0000000000000000;
        gmem[12] = 16'b0000000000000000;
        gmem[13] = 16'b0000000001100100;
        gmem[14] = 16'b0000000000110001;
        gmem[15] = 16'b0000000000000000;
        gmem[16] = 16'b0000000000000000;
        gmem[17] = 16'b0000000000000000;
        gmem[18] = 16'b0000000000000000;
        gmem[19] = 16'b0000000000000000;
        gmem[20] = 16'b0000000000000000;
        gmem[21] = 16'b0000000000000000;
        gmem[22] = 16'b0000000000001010;
        gmem[23] = 16'b0000000000010000;
        gmem[24] = 16'b0000000000000000;
        gmem[25] = 16'b0000000000000000;
        gmem[26] = 16'b1000000000000110;
        gmem[27] = 16'b1000000000000101;
        gmem[28] = 16'b0000000000000000;
        gmem[29] = 16'b0000000000000000;
        gmem[30] = 16'b0000000000000000;
        gmem[31] = 16'b0000000000000011;
    end


    always_ff @(posedge clock) begin
        cycle_counter <= cycle_counter + 16'b1;
        pstate <= nstate;
        if (cycle_counter < RESET_CYCLES) begin
            packet_in_valid <= 1'b0;
        end else if (cycle_counter == EMIT_BODY_LENGTH) begin
            packet_in_data <= PROGRAM_SIZE;
            packet_in_address <= 0;
            packet_in_valid <= 1'b1;
        end else if (cycle_counter < EMIT_EPILOGUE) begin
            packet_in_data <= gmem[cycle_counter - EMIT_BODY_LENGTH + 1]; // first two shorts are meta data
            packet_in_address <= 1;
            packet_in_valid <= 1'b1;
        end else if (cycle_counter <= EMIT_COUNTDOWN) begin
            // epilogue, sleep, and countdon
            packet_in_data <= 16'd4;
            packet_in_address <= 0;
            packet_in_valid <= 1'b1;
        end else begin
            packet_in_valid <= 1'b0;
            if (periphery_gmem_access_failure_error) begin
                $stop;
            end
            if (periphery_exception_error) begin
                if (periphery_exception_id >= 16'h8000) begin
                    $stop; // failed
                end else begin
                    $finish;
                end
            end
        end
    end



endmodule