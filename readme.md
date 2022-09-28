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

### What you see below is my own notes



# Optimizing control flow

Manticore does not have any branch instruction to simplify its pipeline
implementation and provide statically predictable performance. Control flow
in handled through `Mux` instructions that perform conditional assignment.
This means the following piece of code in Verilog gets translate to a single
basic block:

```verilog

always_comb begin

    case (condition)
        v1:
            x = E1
        v2:
            x = E2
        v3:
            x = E3
        ..
        vn:
            x = En
        default:
            x = E0
    endcase

end

```

Suppose that each `Ei` can be computed in at most $m$ steps. Then the MASM code
would have:
```
    m * (n + 1): instructions to compute possible values of x
    n: instructions to create a MUX tree
    n: instructions to compute the conditions (i.e., condition == vi)

    which gives us a total of
    (m + 2) * n + m instructions
```

If we had branch instructions, then we could generate the following code:

```

L1:   bneq condition, v1, L2
      x = compute E1
      jump OUT
L2:   bneq condition, v2, L3
      x = compute E2
      jump OUT
...
Ln:   bneq condition, vn, L0
      x = compute E2
      jump OUT
L0:
      x = compute E0
OUT:
    rest of the code

```

In this model:

```
Best case, condition == v1:
    m instruction assign E1 to x, 1 bneq, 1 jump = m + 2

Worse case, default case:
    n bneq instruction, m instructions to compute E0 = m + n


```

For a typical ALU, a switch statement has 16 branches and m = 2.
So in our current implementation we end up executing about 66 instructions
to compute the final result of an ALU.


In the other model, we execute somewhere between 4 to 18 instructions, clearly
there is a significant improvement here but we can also consider penalty of the
jumps on the pipeline occupancy. Since the pipeline depth is 4 we end up losing
three instructions on every branch at runtime (or compile time if insert NOPS)
```
Best case:
   first branch is not taken and the first jump is taken:
   1 + m + 1 + 2 (2 is the penalty of taking the jump)
Worst case:
    all branches are taken:
    n * (1 + 2) + m
```

So the worst case with m = 2 and n = 16  is 50. This is still an improvement
in general but gain might be less visible depending on the workload. We can still
make a conservative assumption about the virtual cycle latency by taking this
worst case performance into account and pad NOPs at the end of each branch
so that all branches end up with the same execution latency. But I think there
is a better yet unconventional way of doing this.


# Optimizing MUX trees with hardware decoding!

The running switch statement example basically describes an address decoder in
hardware. We can take advantage of existing hardware decoders to accelerate
branch condition resolution in special cases where the condition is an integer
and the values `v1` to `vn` are constants.

To do this, create a small memory consisting with values of each cell being
L0, L1, ..., or Ln.
If the condition is an integer of bit width w, then the should have 2**w cells
with the following mapping
```
if address in {v1, .., vn} then data(vi) = Li
else data(address) = L0

```

In other words, the for any of the values `vi` in the case statements, we
save the label `Li` in the memory at address `vi`, all other elements of the memory
have the value of `L0` which is the default case label.
This way the condition evaluation reduces to a load instruction! And there
would be no need to jump around, to finally settle at a branch. This way we
directly jump into the right branch:

```
    LLD label, ConditionMem[condition];
    jr label;

L1:
    x = compute E1
    jump OUT
L2:
    x = compute E2
    jump OUT
...

Ln:
    x = compute En
    jump OUT
L0:
    x = compute E0
    NOP NOP NOP
OUT:

```
This way the best case latency is when we jump to L0 and the worst case is
when we jump to any other label:

```
best case:
    1 + (1 + 2) + m
worst case:
    1 + (1 + 2) + m + (1 + 2)
```

This means if m = 2 we get a latency of 6 or 9!


# Patterns to optimize

## Boolean patterns


### P0
```
.const c0 16 0
.const c1 16 1
.const c127 16 127
AND x1, x0, c127;  only keep the first 7 bits
SEQ x2, x1, c0; x2 = x1 == 0
SEQ x3, x2, c0; x3 = x2 == false
XOR x4, x3, c1; x4 = !x3 or x4 = x2
```

can be reduced to

```
.const c0 16 0
.const c1 16 1
.const c127 16 127
AND x1, x0, c127;  only keep the first 7 bits
SEQ x2, x1, c0; x2 = x1 == 0
SEQ x4, x2, c1; x4 = x2 == true => x4 = x1 == 0
```
and further to


```
.const c0 16 0
.const c1 16 1
.const c127 16 127
AND x1, x0, c127;  only keep the first 7 bits
SEQ x4, x1, c0; x2 = x1 == 0
```

### P1

```
.wire w0 1 // single-bit
.const c0 1 0
.const c1 1 1
.wire w1 1 // single-bit
SEQ w0, x0, c0; // is x0 zero?
XOR w1, w0, c1;  // is w0 true? i.e., is x0 zero?
```

can be reduced to

```
.wire w0 1 // single-bit
.const c0 1 0
.const c1 1 1
.wire w1 1 // single-bit
SEQ w1, x0, c0; // is x0 zero?
```


### P2

```
.const c0 1 0
.const c1 1 1
SEQ x1, x0, c0; is x0 == zero?
SEQ x2, x1, c0; is ! x0 == zero ?
XOR x3, x2, c1; is x0 == zero?
```
reduces to
```
SEQ x3, x0, c0;
```
### P3
Implement CSE for AddCarry!

# Memory access bound checking
Yosys has a pass memory_memx to handle out-of-bound memory accesses according
to the Verilog standard, either use that or do our own checks....



## JumpTables, Interpreters, and register allocation

By construction, a `JumpTable` may have the following form:


```
SWITCH %w0
    case L0:
        ADD %w1, _, _;
        BREAK
    case L1:
        BREAK
    case L2:
        BREAK
    PHI(%w4, L0:%w1, L1:%w2, L2:%c2)

```
That is a source register in the PHI may reference a register that is defined
outside of the scope of the SWITCH. Such register is either constant or a
non-constant register whose also referenced outside the SWITCH blocks.


This means that when interpreting, we should either break SSAness by converting
the above code to the Philess code below:

```
SWITCH %w0
    case L0:
        ADD %w1, _, _;
        BREAK
    case L1:
        MOV %w4, %w2;
        BREAK
    case L2:
        MOV %w4, %c2;
        BREAK
```

Indeed this is what `UnconstrainedInterpreter` does internally.  Another alternative,
which keeps SSAness would be:

```
SWITCH %w0
    case L0:
        ADD %w1, _, _;
        BREAK
    case L1:
        MOV %ww2, %w2;
        BREAK
    case L2:
        MOV %wc2, %c2;
        BREAK
    PHI(%w4, L0:%w1, L1:%ww2, L2:%wc2)

```

You may wonder why wouldn't we place those moves inside the cases and make all
case bodies nonempty and make all Phi operands be defined inside the scope of
the switch?

We can do that, but optimization passes can not guarantee to keep this invariant
for a good reason. For instance, if we force `ConstantFolding` to never remove
`MOV` to Phi then we may miss an opportunity to completely remove the jump table
if we realize all operands to all Phis are constants and replace it with simple
lookup (i.e., the lookup for label would be become a lookup for a value). This
would not be immediately visible if we keep the latter invariant. So it's good
to remove all possible aliases and only bring them back in at the very end when
no more optimization is legal (i.e., just before scheduling).




## Tracking wire

Get rid of `@TRACK` annotation and explicitly handle them with `Track rs`
instructions. Take special care when parallelizing code not to duplicate
tracking of values that are redundantly computed in multiple places. This will
also streamline optimizations that currently avoid optimizing anything that is
tracked. (e.g., if a tracked value is constant, we still compute it rather than
hard-coding it)

Also note that currently if output of a jump table is tracked, `DCE` after
jump table extraction fails.