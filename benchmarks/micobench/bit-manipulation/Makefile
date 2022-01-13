

TEST_CASES = RV32_IntegerRR RV32_Load RV32_Store
VMAKE = VMake.mk
VTESTER = VTester.cpp
VERILOG = $(TEST_CASES:=.sv)
VCPP    = $(TEST_CASES:=_tb.cc)
VMK     = $(TEST_CASES:=.mk)
VVCD    = $(addprefix Tester_, $(TEST_CASES:=_trace.vcd))

MASM    = $(TEST_CASES:=.masm)
MASM_DUMPS = $(TEST_CASES:=.masm.dumps.d)

include ../../check_thyrio.mk

all: verify generate


$(TEST_CASES) $(VERILOG) $(VCPP) $(VMK): Makefile RV32.py $(VMAKE) $(VTESTER)
	@echo "Generating verilog files"
	-python3 RV32.py

VTARGETS = $(addprefix verify-, $(TEST_CASES))
VTARGETS_CLEAN = $(addprefix verify-clean-, $(TEST_CASES))

THYRIO_TARGETS = $(TEST_CASES:=.masm)


$(VTARGETS):  verify-%: %.mk
	@echo "verifying $<"
	$(MAKE) -f $<

$(VTARGETS_CLEAN): verify-clean-%: %.mk
	$(MAKE) clean -f $<

$(THYRIO_TARGETS): %.masm: %.sv
	@echo "Generating MASM $@"
	thyrio_frontend -vlog_in $< -masm_out $@ -top Tester_$* -no_techmap -dump 1> $@.log

# Verify that all verilator simulation pass
verify: $(VTARGETS)
vclean: $(VTARGETS_CLEAN)

# Generate MASM code
generate: $(THYRIO_TARGETS)


${TOP}.masm: ${VERILOGS}
	thyrio_frontend -vlog_in ${VERILOGS} -masm_out ${TOP}.masm -top Counter -no_techmap -dump


.PHONY: clean
clean: vclean
	-rm -rf *.masm.dumps.d
	-rm -rf *.masm
	-rm -rf *.masm.log
	-rm -rf *.masm.*.data
	-rm -rf *.vcd
	-rm $(VERILOG) $(VCPP) $(VMK)