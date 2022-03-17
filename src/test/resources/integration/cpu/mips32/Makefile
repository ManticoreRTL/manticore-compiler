######################################################################
#
# DESCRIPTION: Verilator Example: Small Makefile
#
# This calls the object directory makefile.  That allows the objects to
# be placed in the "current directory" which simplifies the Makefile.
#
# This file ONLY is placed under the Creative Commons Public Domain, for
# any use, without warranty, 2020 by Wilson Snyder.
# SPDX-License-Identifier: CC0-1.0
#
######################################################################
# Check for sanity to avoid later confusion

ifneq ($(words $(CURDIR)),1)
 $(error Unsupported: GNU Make cannot build in directories containing spaces, build elsewhere: '$(CURDIR)')
endif

######################################################################
# Set up variables

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
VERILATOR_FLAGS += --exe --build
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
# VERILATOR_INPUT = -f input.vc top.v sim_main.cpp
VERILATOR_CPP_TB = TestBench.cpp
VERILATOR_TOP_FILE = mips32.sv $(VERILATOR_CPP_TB)
VERILATOR_OTHER_FILES = $(VERILATOR_TOP_FILE) -y .

# Top module, not really needed.
VERILATOR_FLAGS += --top-module TestVerilator

######################################################################


default: verilate


verilate: $(wildcard *.sv)
	@echo "Verilating files ..."
	$(VERILATOR) $(VERILATOR_FLAGS) $(VERILATOR_TOP_FILE)
	obj_dir/VTestVerilator

clean mostlyclean distclean maintainer-clean::
	-rm -rf obj_dir logs *.log *.dmp *.vpd coverage.dat core

thyrio: $(wildcard *.sv)
	@echo "Generating MASM"
	thyrio_frontend -vlog_in mips32.sv -masm_out mips32.masm -top TestVerilator -no_techmap -dump