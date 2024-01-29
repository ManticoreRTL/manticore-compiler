# Manticore Assembly Compiler

This repository contains all the needed tools for running code on the Manticore hardware.
This repository is not standalone and requires two other repositories to work (frontend and hardware) and another one to run RTL simulation on an FPGA (the runtime).



## Software Requirements

You need SBT 1.6.2 to build the compiler; we recommend using SDKMAN to install it.
```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install sbt 1.6.2
sdk install scala 2.13.8

```
The frontend needs the following packages:
```
sudo apt-get install build-essential clang bison flex \
    libreadline-dev gawk tcl-dev libffi-dev git \
    graphviz xdot pkg-config python3 libboost-system-dev \
    libboost-python-dev libboost-filesystem-dev zlib1g-dev
```

For some tests, we also need Verilator:
```
sudo apt install verilator
```

## Hardware Requirements
Manticore is only tested on an Alveo U200 FPGA board.
You can build it on another Alveo board, but you may not get the best performance.
See the [hardware submodule](https://github.com/ManticoreRTL/manticore-hw) for more information.

To build the hardware, you need Vivado 2022.1 (another version may also work).
To run code on hardware, you need the XRT runtime; please consult AMD/Xilinx guides to install XRT.

## Build Instructions

First, initialize the submodules
```
git submodule update --init --recursive
```

Next step is to build the frontend:
```
make -C frontend -j 12
```

Some tests also need the hardware artifacts, so:
```
pushd hardware && sbt publishLocal && popd
```
This does not build the hardware; it just makes the project available to the compiler so that we can run some integration tests.
If you want to build the FPGA bitstream, use the [`build.sh`](https://github.com/ManticoreRTL/manticore-hw/blob/master/build.sh) stript in the  [hardware submodule](https://github.com/ManticoreRTL/manticore-hw)

If you want to deploy your programs, you need to build the runtime:

```
mkdir -p runtime/build
cmake -S runtime -B runtime/build
cmake --build runtime/build
```


The last step is to build the compiler:

```
sbt assembly
```

With all of that taken care of, you can now call the compiler (target is a 2x2 Manticore):
```
./masm -x 2 -y 2 benchmarks/MIPS32/main.sv benchmarks/MIPS32/mips32.sv
```

This will compile the verilog files down to Manticore's machine code, run using:

```
./runtime/build/manticore --xclbin PATH_TO_FPGA_XCLBIN obj_dir/manifest.json

```
where PATH_TO_FPGA_XCLBIN points to the FPGA bitstream.


If you do not have an FPGA and just want to play around with the compiler, you can consider simulating the hardware with Verilator; see the integration tests for example.
For even something quicker, you ask the compiler to interpret the code for you directly:

```
./masm interpret -x 2 -y 2 benchmarks/MIPS32/main.sv benchmarks/MIPS32/mips32.sv
```
This will interpret the high-level assembly code, in which operands may have arbitrary width.
You can alternatively lower the code almost to what is given to the hardware (post scheduling and register allocation):

```
./masm interpret --lower -x 2 -y 2 benchmarks/MIPS32/main.sv benchmarks/MIPS32/mips32.sv
```


