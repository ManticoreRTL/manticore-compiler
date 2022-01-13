from dataclasses import dataclass
import typing
import random

if __name__ == "__main__":

    RV32 = {
        "FUNCT7": 7,
        "RS2": 5,
        "RS1": 5,
        "FUNCT3": 3,
        "RD": 5,
        "OPCODE": 7,
        "IMM12": 12,
        "IMM7": 7,
        "IMM5": 5
    }

    def generateVerilog(name: str, signature: typing.List[str]) -> str:

        outputs = "\t" + \
            ',\n\t'.join(
                [f"output wire [{RV32[field]} - 1 : 0] {field.lower()} " for field in signature])
        assigns = []
        index = 0
        for field in signature:
            width = RV32[field]
            assigns.append(
                f"assign {field.lower()} = instruction [{index} +: {width}];")
            index += width
        body = "\t" + '\n\t'.join(assigns)
        module = f"""
module {name}(
    input wire [32 - 1 : 0] instruction,
{outputs}
);
{body}
endmodule
"""
        # set the seed to a fixed value for reproducibility
        random.seed(0)
        num_tests = 10

        # generate random fields and assemble the instruction
        insts = []
        results = {f: [] for f in signature}
        inits = []
        for i in range(0, num_tests):
            index = 0
            inst_builder = 0
            for f in signature:
                width = RV32[f]
                v = random.randint(0, (1 << width) - 1)
                results[f].append(v)
                inst_builder |= v << index
                index += width
                inits.append(f"{f.lower()}_rom[{i}] = {width}'d{v};")
            insts.append(inst_builder)
            inits.append(f"inst_rom[{i}] = 32'd{inst_builder};\n")

        res_decls = "\t" + \
            '\n\t'.join(
                [f"logic [{RV32[field]} - 1 : 0] {field.lower()}_rom [{num_tests} - 1 : 0];" for field in signature])
        sig_decls = "\t" + \
            '\n\t'.join(
                [f"wire [{RV32[field]} - 1 : 0] {field.lower()};" for field in signature])
        bindings = "\t\t" + \
            ",\n\t\t".join(
                [f".{field.lower()}({field.lower()})" for field in signature])

        inits_str = '\t\t\t' + '\n\t\t\t'.join(inits)

        assertions = '\t\t' + \
            '\n\t\t'.join(
                [f"assert ({field.lower()} == {field.lower()}_rom[pc]);" for field in signature])
        masm_assertions = '\t\t' + \
            '\n\t\t'.join(
                [f"$masm_expect ({field.lower()} == {field.lower()}_rom[pc], \"failed\");" for field in signature])
        tester = f"""
module Tester_{name}(input wire clock);
        logic [32 - 1 : 0] inst_rom [{num_tests} - 1 : 0];
        logic [32 - 1 : 0] pc = 0;
        wire  [32 - 1 : 0] inst;
{res_decls}
{sig_decls}
        assign inst = inst_rom[pc];
        {name} dut(
                .instruction(inst),
{bindings}
        );
        always_ff @(posedge clock) begin
            if (pc < {num_tests}) begin
                pc <= pc + 1;
`ifdef VERILATOR
{assertions}
`else
{masm_assertions}
`endif
            end else begin
`ifdef VERILATOR
                $finish;
`else
                $masm_stop;
`endif
            end
        end
        initial begin
{inits_str}
        end

endmodule
        """

        return module + "\n" + tester

    def generateVerilatorMakeFile(name: str) -> str:
        with open('VMake.mk', 'r') as fp:
            lines = fp.readlines()
            subst = [l.replace("@name@", name) for l in lines]
            return ''.join(subst)

    def generateVerilatorTb(name: str) -> str:
        with open('VTester.cpp', 'r') as fp:
            lines = fp.readlines()
            subst = [l.replace("@name@", name) for l in lines]
            return ''.join(subst)

    RV32_IntegerRR = ["OPCODE", "RD", "FUNCT3", "RS1", "RS2", "FUNCT7"]
    RV32_Load = ["OPCODE", "IMM5", "FUNCT3", "RS1", "RS2", "IMM7"]
    RV32_Store = ["OPCODE", "IMM5", "FUNCT3", "RS1", "RS2", "IMM7"]
    import inspect
    import re
    def varname(p):
        for line in inspect.getframeinfo(inspect.currentframe().f_back)[3]:
            m = re.search(
                r'\bvarname\s*\(\s*([A-Za-z_][A-Za-z0-9_]*)\s*\)', line)
            if m:
                return m.group(1)


    def generate(name: str, signature: typing.List[str]) -> None:

        def writeToFile(fname: str, text: str):
            with open(fname, 'w') as fp:
                fp.write(text)

        writeToFile(name + ".sv", generateVerilog(name, signature))
        writeToFile(name + "_tb.cpp", generateVerilatorTb(name))
        writeToFile(name + ".mk", generateVerilatorMakeFile(name))


    generate(varname(RV32_IntegerRR), RV32_IntegerRR)
    generate(varname(RV32_Load), RV32_Load)
    generate(varname(RV32_Store), RV32_Store)



