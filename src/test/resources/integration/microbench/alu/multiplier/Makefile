
# If $VERILATOR_ROOT isn't in the environment, we assume it is part of a
# package install, and verilator is in your path. Otherwise find the
# binary relative to $VERILATOR_ROOT (such as when inside the git sources).
ifeq ($(VERILATOR_ROOT),)
VERILATOR = verilator
else
export VERILATOR_ROOT
VERILATOR = $(VERILATOR_ROOT)/bin/verilator
endif

# Generate C++ from Verilog/SystemVerilog, testbench not included
VERILATOR_FLAGS += -cc
# if you had a testbench in C++
VERILATOR_FLAGS += --exe --build --Mdir obj_dir_mult
# Generate makefile dependencies (not shown as complicates the Makefile)
#VERILATOR_FLAGS += -MMD
# Optimize
VERILATOR_FLAGS += -Os -x-assign 0 # assign Xs to 0
# Do not warn/fail when the assignment width do not match
VERILATOR_FLAGS += -Wno-WIDTH
# Make waveforms
VERILATOR_FLAGS += --trace
# Check SystemVerilog assertions
VERILATOR_FLAGS += --assert
# Generate coverage analysis
# VERILATOR_FLAGS += --coverage
# Run Verilator in debug mode, dumps tree files
# VERILATOR_FLAGS += --debug
# Add this trace to get a backtrace in gdb
#VERILATOR_FLAGS += --gdbbt

# Input files for Verilator
VERILATOR_CPP_TB = VTester.cpp
VERILATOR_TOP_FILE = array_mult.sv $(VERILATOR_CPP_TB)
VERILATOR_OTHER_FILES = $(VERILATOR_TOP_FILE) -y .

# Top module, not really needed.
VERILATOR_FLAGS += --top-module ArrayMultiplierTester

######################################################################


default: verilate_run

verilate_compile: $(VERILATOR_TOP_FILE)
	@echo "Verilating files ..."
	$(VERILATOR) $(VERILATOR_FLAGS) $(VERILATOR_TOP_FILE)

verilate_run: verilate_compile
	@echo "Running Verialtor sim ..."
	./obj_dir_mult/VArrayMultiplierTester

thyrio: verilate_run
	@echo "Calling thyrio"
	thyrio_frontend -vlog_in array_mult.sv -masm_out ArrayMultiplierTester.masm -top ArrayMultiplierTester -no_techmap -dump


clean mostlyclean distclean maintainer-clean::
	-rm -rf obj_dir_alu logs *.log *.dmp *.vpd coverage.dat core

