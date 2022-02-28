![Assembly Tests](https://github.com/epfl-vlsc/manticore-compiler/actions/workflows/assembly_tests.yml/badge.svg)
![Chisel Tests](https://github.com/epfl-vlsc/manticore-compiler/actions/workflows/chisel_tests.yml/badge.svg)
# Manticore Assembly Compiler

**This repository is under heavy development.**

Manticore is a massively parallel architecture for cycle-accurate RTL simulation
on FPGAs. The compiler takes an "unconstrained" assembly program written in
a single block of code and generates parallelized code for hardware execution.

# Installation and usage

## Dependencies
The project is written in `scala 2.13.8` and you has been developed with
`sbt 1.6.2`. Please have them installed.

To install the compiler you need to have the Manticore chisel project installed
on your machine:

```bash
> git@github.com:epfl-vlsc/manticore.git
> cd manticore
> sbt "publishLocal"
> sudo apt install verilator
```



# Overview
## Manticore Assembly

A Manticore processor can execute a sequence of instruction from start to
finish. It does not have any branch or call instructions, but is has conditional
assignments which can be used to implement "if-then-else" like statements.
Additionally, the processor implements an _implicit_ loop, in which whenever all
of the instructions in the instruction memory are executed, it goes back to the
beginning and starts anew. You can find an example program in which values of
`op1` and `op2` are read from a file, added to each other and compared against
`expected_results`. The program consists of two parallel processes that share a
single register, see the comments in the code for further explanations.


```
// THIS IS A COMMENT
// beginning of the program
.prog:
    // @LOC is an annotation
    // Every annotation is sequence of key-value pairs, e.g., x is key the
    // value is 0. Values could be Int, Boolean or Strings. A key can only
    // accept a specific type, for instance we can not give the string "0"
    // to x, or the boolean false.
	@LOC [x = 0,y = 0]
	.proc proc_0_0:
        // Memories are defined as ".mem" and they should be preceded by
        // these two annotations
		@MEMINIT [file = "expected_results.dat",width = 16,count = 600]
		@MEMBLOCK [block = "expected_results",width = 16,capacity = 600]
		.mem res_ptr 16

        // wires are temporary values both are 16-bit wide, you can supply
        // an arbitrary different width if your application demands it
        .wire res_addr 16
		.wire expected_res 16


		@MEMINIT [file = "op1.dat",width = 16,count = 600]
		@MEMBLOCK [block = "op1_values",width = 16,capacity = 600]
		.mem op1_ptr 16

		.wire op1_addr 16
		.wire op1_val 16

		@MEMINIT [file = "op2.dat",width = 16,count = 600]
		@MEMBLOCK [block = "op2_values",width = 16,capacity = 600]

		.mem op2_ptr 16
		.wire op2_addr 16

		.wire op2_val 16

        // .const define registers that are immutable and retain their
        // values across loops. The first number indicates the bit-width
        // and the latter is the default value
		.const one 16 1
		.const zero 16 0
		.const test_length 16 600

        // .input defines an "immutable" within the loop body but its value
        // will be changed at the end of the loop. Every .input is annotated by
        // @REG which should define a globally unique id with type set to \REG_CURR.
        // Each .input should have a corresponding .output definition that
        // shares the same unique id in its @REG annotation with the type
        // set to \REG_NEXT. The instruction can assign a value to the
        // .output register and that value is automatically used as the new
        // value of the corresponding .input definition in the next loop.

		@REG [id = "counter",type = "\REG_CURR"]
		.input counter_curr 16 0
		@REG [id = "counter",type = "\REG_NEXT"]
		.output counter_next 16
		@REG [id = "done",type = "\REG_CURR"]

		.input done_curr 16 0
		@REG [id = "done",type = "\REG_NEXT"]
		.output done_next 16
		@REG [id = "result",type = "\REG_CURR"]

		.input result_curr 16
		@REG [id = "result",type = "\REG_NEXT"]
		.output result_next 16

		@REG [id = "correct",type = "\REG_CURR"]
		.input correct_curr 16 1
		@REG [id = "correct",type = "\REG_NEXT"]
		.output correct_next 16


        // The instructions follow the definitions. Each .wire or .output name
        // can only be assigned once. Therefore, the instruction do not need
        // to follow any particular order since the order can be reconstructed


        ADD	op1_addr, op1_ptr, counter_curr;
		ADD	op2_addr, op2_ptr, counter_curr;
		ADD	res_addr, res_ptr, counter_curr;

        // LD is the "load" instruction and the offset  (constant value) is
        // given in the brackets (always give zero unless if you know what
        // you are doing)
		@MEMBLOCK [block = "op1_values",width = 16,capacity = 600]
		LD op1_val, op1_addr[0];
		@MEMBLOCK [block = "op2_values",width = 16,capacity = 600]
		LD op2_val, op2_addr[0];

		ADD	result_next, op1_val, op2_val;
		@MEMBLOCK [block = "expected_results",width = 16,capacity = 600]
		LD expected_res, res_addr[0];

        ADD	counter_next, counter_curr, one;

        // SEQ is seq equal
		SEQ	correct_next, result_next, expected_res;
		SEQ	done_next, counter_next, test_length;

        // EXPECT is like assert(correct_curr == one)
        @TRAP [type = "\fail"]
		EXPECT correct_curr, one, ["failed"];
		@TRAP [type = "\stop"]
		EXPECT done_curr, zero, ["stopped"];

	@LOC [x = 1,y = 0]
	.proc p_1_0:
		@REG [id = "dummy",type = "\REG_CURR"]

		.input dummy_curr 16
		@REG [id = "dummy",type = "\REG_NEXT"]

		.output dummy_next 16
		@REG [id = "result",type = "\REG_CURR"]

        // notice how the this definition is shared by the
        // two processes. This makes the compiler generate additional
        // code to send out the value of the corresponding .output definition
        // from process p_0_0 at the end of the loop to keep the values of
        // result_curr in sync in both of the process. Only one process
        // can assign result_next though.

		.input result_curr 16

        // this .output is never assigned
		@REG [id = "result",type = "\REG_NEXT"]
		.output result_next 16

		MOV dummy_next, result_curr; //@81.8


```



Although the code snippet above shows two parallel processes, the standard input
to the compiler should only define a single process, and no `@LOC` annotation
should be used. The compiler takes care of parallelizing the code and assigning
each process to a processor. Furthermore, the `LD` instruction should always get
a zero offset.

You generally don't need to write assembly yourself, you can use `thyrio_frontend`
to convert `Verilog` into acceptable assembly. If you are on iccluster030, add the
following to your `.bashrc` or `.zshrc`:

```
export PATH=${PATH}:/scratch/emami/jit_sim/thyrio-frontend/src/thyrio
```


The enables to call `thyrio_frontend`, for instance:
```
> thyrio_frontend -vlog_in mips32.sv -masm_out mips32.masm -top TestVerilator -no_techmap -dump
```

Translate the mips32.sv file with its top module set to `TestVerilator` into `mips32.masm`.
For the time being, always supply the `-no_techmap` argument.



## Compiler architecture
The compiler consists of two main parts, the
[`ManticoreAssemblyIR`](manticore-compiler/main/scala/compiler/assembly/Instruction.scala)
type class which is used as a template for different levels or flavors of the
assembly. Currently, there are two flavors of the assembly language,
`UnconstrainedIR` and `PlacedIR`. The first is used in the preliminary passes of
the compiler in which a "register" can have arbitrary bit-width. For instance
you can define a register that is 1000 bits wide as:

```
.wire my_wide_wire 1000
@REG [....]
.input my_wide_input 1400 1979857298374029374982734908273497239047290834
```


Every pass in the compiler is simply a function that takes one a program in
flavor `T` and produces another in flavor `S` (we could have `S =:= T`, as
is the case with most transformations.

```scala

object AppendConstantZeroTransform extends AssemblyTransformer[UnconstrainedIR.DefProgram,UnconstrainedIR.DefProgram] {
    import UnconstrainedIR._

    override def transform(prog: DefProgram, context: AssemblyContext): DefProgram = {

        context.logger.info("This is a very simple transformation"
            "that add a constant zero to every process")

        prog.copy(
            processes = prog.processes.map { p => transform(p, context) }
        ).setPos(prog.pos)

    }

    def transform(proc: DefProcess, context: AssemblyContext): DefProcess = {
        proc.register
            .find(r => r.variable.varType == ConstType &&
                r.variable.value.get == BigInt(0)) match {
            case None => // no constant zero was found
                proc.copy(
                    registers = proc.registers +:
                        DefReg(
                            variable = LogicVariable(
                                name = s"constant_zero_${context.uniqueNumber()}",
                                width = 32,
                                tpe = ConstType
                            )
                            value = Some(BigInt(0))
                        )
                ).setPos(proc.pos)
            case Some(const0) => proc
        }

    }
}
```

`AssemblyChecker[T]` are specialized transformation that return the same program
and can be defined as follows:

```scala

object ExistsConstantZeroTransform extends AssemblyChecker[UnconstrainedIR.DefProgram] {
    import UnconstrainedIR._

    override def check(prog: DefProgram, context: AssemblyContext): DefProgram = {

        context.logger.info("This is a very simple check to see if "
            "constant zero is defined in all processes")

        prog.copy(
            processes = prog.processes.map { p => transform(p, context) }
        ).setPos(prog.pos)

    }

    def check(proc: DefProcess, context: AssemblyContext): DefProcess = {
        proc.register
            .find(r => r.variable.varType == ConstType &&
                r.variable.value.get == BigInt(0)) match {
            case None => // no constant zero was found
                context.logger.warn(s"Could not find constant zero in process ${p.id}")
            case Some(const0) => proc
                context.logger.info(s"Found constant zero in process ${p.id}", const0)
        }

    }
}
```