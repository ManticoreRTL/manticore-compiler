## Basic rules for testing

ROOT_DIR:=$(dir $(abspath $(lastword $(MAKEFILE_LIST))))

THREADS = 0

VERILATOR = verilator
VERILATOR_FLAGS += -cc --exe --build -Os -x-assign 0
VERILATOR_FLAGS += -Wno-WIDTH --trace
VERILATOR_FLAGS += --assert

VERILATOR_FLAGS += --top-module Main
VERILATOR_FLAGS += --threads $(THREADS)
VERILATOR_FLAGS += $(ROOT_DIR)/VHarness.cpp
VERILATOR_TIMEOUT += 10000

default: verilate thyrio

check: verilator_exists thyrio_exists

verilator_exists:
	@echo "Checking verilator"
	@which verilator

thyrio_exists:
	@echo "Checking thyrio"
	@which thyrio_frontend


default: verilate

verilate: $(VERILOG_SOURCES) verilator_exists
	@echo "Verilating files $@"
	$(VERILATOR) $(VERILATOR_FLAGS) $(VERILOG_SOURCES)
	@echo "Running simulation"
	obj_dir/VMain $(VERILATOR_TIMEOUT)

thyrio: $(VERILOG_SOURCES) thyrio_exists
	thyrio_frontend -vlog_in $(VERILOG_SOURCES) -masm_out main.masm -top Main -no_techmap -dump -track $(TRACK_YML)
clean:
	-rm -rf obj_dir logs *.log *.dmp *.vpd coverage.dat core trace.vcd main.masm*