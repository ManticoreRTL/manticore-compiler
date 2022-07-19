
BENCHMARK = Vortex_axi_fpu_post.v
# BENCHMARK = Vortex_axi_nofpu_post.v
VERILOG_SOURCES = TestBench.sv $(BENCHMARK) ../axi4_full_slave/axi4_full_slave.sv

VERILATOR_FLAGS += -Wno-PINMISSING
VERILATOR_FLAGS += -Wno-CASEOVERLAP
VERILATOR_FLAGS += -Wno-UNSIGNED
VERILATOR_FLAGS += -Wno-LITENDIAN
# VERILATOR_FLAGS += --trace

include ../rules.mk
