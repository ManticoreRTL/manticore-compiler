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

```

Run something using
```
sbt "runMain manticore.compiler.Main execute /scratch/emami/jit_sim/manticore-compiler/test_run_dir/MipsAluBench/random_inputs_1_should_match_expected_results/main.masm --dump-dir dumps --dump-all -d --output console.log"
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
    // Every annotation is a sequence of key-value pairs, e.g., x is key the
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

        .mem op2_ptr 16 600
        .wire op2_addr 16

        .wire op2_val 16

        // .const define registers that are immutable and retain their
        // values across loops. The first number indicates the bit-width
        // and the latter is the default value
        .const one 16 1
        .const zero 16 0
        .const test_length 16 600

        // state elements in verilog, i.e., regs are defined using .reg followed
        // by a name and a length. However every reg is also followed by an
        // .input that is the current value of that register (with an optional initial value)
        // and an .output which is the next value of the register. Basically
        // for instance
        // reg myreg [15:0] = 231;
        // always @(posedge clk) if (myreg == 2) myreg <= something;
        // is represented as:
        @REG [id = "counter",type = "\REG_CURR"]
        reg myreg 16 .input counter_curr 231 .output counter_next
        // Note that myreg is simply a unique id and it is not readable/writeable.
        // counter_curr is only readable and counter_next is only writeable.
        // The fact that counter_curr and counter_next represent the same register
        // is implicit i.e., we have the following code in manticore
        // SEQ cond, counter_curr, const_2;
        // MUX counter_next, cond, counter_curr, something;



        // The instructions follow the definitions. Each .wire or .output name
        // can only be assigned once. Therefore, the instruction do not need
        // to follow any particular order since the order can be reconstructed
        //


        ADD op1_addr, op1_ptr, counter_curr;
        ADD op2_addr, op2_ptr, counter_curr;
        ADD res_addr, res_ptr, counter_curr;


```

