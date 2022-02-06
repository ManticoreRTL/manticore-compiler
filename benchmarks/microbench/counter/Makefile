

VERILOGS=$(wildcard *.sv) $(wildcard *.v)
TOP=Counter

include ../../check_thyrio.mk


${TOP}.masm: ${VERILOGS}
	thyrio_frontend -vlog_in ${VERILOGS} -masm_out ${TOP}.masm -top Counter -no_techmap -dump


clean:
	rm -rf ${TOP}.masm.dumps.d ${TOP}.masm