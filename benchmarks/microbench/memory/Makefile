


default: thyrio

thyrio: %.masm: %.sv
	@echo "Generating MASM $@"
	thyrio_frontend -vlog_in VectorAdd.sv -masm_out VectorAdd.masm -top VectorAddTester -no_techmap -dump


clean: vclean
	-rm -rf *.masm.dumps.d
	-rm -rf *.masm
	-rm -rf *.masm.log
	-rm -rf *.masm.*.data