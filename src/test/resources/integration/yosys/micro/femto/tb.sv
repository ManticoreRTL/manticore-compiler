module Main #(TIMEOUT = 100)(input wire clk);

    wire clock = clk;
    logic         reset;
    wire          prev_ready;
    logic         prev_valid;
    logic  [31:0] prev_bits_address = 0; // unused
    logic         prev_bits_wen = 0; // unused
    logic  [31:0] prev_bits_data;
    logic  [7:0]  prev_bits_id;
    logic         next_ready;
    wire          next_valid;
    wire   [31:0] next_bits_address;
    wire   [31:0] next_bits_data;
    wire          next_bits_wen;
    wire   [7:0]  next_bits_id;
    wire          halted;

    logic [31:0] counter = 0;
    logic [31:0] memory [0:255];
    initial begin
        memory[0] = 32'h631826;
        memory[1] = 32'h2063ffff;
        memory[2] = 32'h3c03ffff;
        memory[3] = 32'h210826;
        memory[4] = 32'h421026;
        memory[5] = 32'h10230002;
        memory[6] = 32'h20210001;
        memory[7] = 32'h8000005;
        memory[8] = 32'h40000d;
        memory[9] = 32'h0;
        memory[10] = 32'h0;
        // $readmemh(FILE_NAME, memory);
    end
    typedef enum logic[1:0] { sAddr, sReadData, sReadResp, sWriteData } state_t;

    state_t pstate = sAddr;
    state_t nstate;
    logic [31:0] address;
    logic [31:0] data;


    always_ff @(posedge clock) begin
        counter <= counter + 32'd1;
        if (reset) begin
            pstate <= sAddr;
        end else begin
            pstate <= nstate;
        end
        if (pstate == sAddr) begin
            address <= next_bits_address >> 2;
            data <= next_bits_data;
        end
        if (pstate == sReadData) begin
            // $display("@ %d MDATA = 0x%x MADDR = 0x%x", counter, memory[address], address);
            prev_bits_data <= memory[address];
            prev_bits_id   <= data;
        end
        if (pstate == sWriteData) begin
            memory[address] <= data;
            $display("did not expect write from cores!");
            $stop;
        end

        if (counter > TIMEOUT) begin
            $display("finished after %d cycles", counter);
            $finish;
        end

    end


    wire next_fire = next_ready & next_valid;
    wire prev_fire = prev_ready & prev_valid;
    always_comb begin
        nstate = sAddr;
        next_ready = 1'b0;
        prev_valid = 1'b0;
        reset = (counter < 10);
        case(pstate)
            sAddr: begin
                next_ready = 1'b1;
                if (next_fire) begin
                    nstate = next_bits_wen ? sWriteData : sReadData;
                end
            end
            sReadData: begin
                nstate = sReadResp;
            end
            sReadResp: begin
                prev_valid = 1'b1;
                nstate = prev_fire ? sAddr : sReadResp;
            end
            sWriteData: begin
                nstate = sAddr;
            end

        endcase

    end


    MulticoreRing dut (
        .clock(clock),
        .reset(reset),
        .io_prev_ready(prev_ready),
        .io_prev_valid(prev_valid),
        .io_prev_bits_address(prev_bits_address),
        .io_prev_bits_wen(prev_bits_wen),
        .io_prev_bits_data(prev_bits_data),
        .io_prev_bits_id(prev_bits_id),
        .io_next_ready(next_ready),
        .io_next_valid(next_valid),
        .io_next_bits_address(next_bits_address),
        .io_next_bits_data(next_bits_data),
        .io_next_bits_wen(next_bits_wen),
        .io_next_bits_id(next_bits_id),
        .io_halted(halted)
    );

endmodule