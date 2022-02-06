module Counter(
    input wire __clock__
);
    localparam TEST_SIZE = 32;
    logic [31:0] counter;
    logic [31:0] expected_values [TEST_SIZE - 1 : 0];
    initial begin
        counter = 0;
        $readmemh("expected.dat", expected_values);
    end
    wire [31:0] counter_ref;
    assign counter_ref = expected_values[counter];

    always @(posedge __clock__) begin
        if (counter < TEST_SIZE) begin
            $masm_expect(counter == counter_ref, "invalid counter");
            counter <= counter + 1;
        end else begin
            $masm_stop;
        end

    end

endmodule