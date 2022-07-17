
BLOCKIN = 64
BLOCKOUT = 64
BENCHMARK = mem_div_16/IntelShell_blockIn$(BLOCKIN)_blockOut$(BLOCKOUT).sv
VERILOG_SOURCES = TestBench.sv $(BENCHMARK) ../axi4_full_slave/axi4_full_slave.sv ../xormix32.sv
VERILATOR_FLAGS += -Wno-PINMISSING # --trace

include ../rules.mk