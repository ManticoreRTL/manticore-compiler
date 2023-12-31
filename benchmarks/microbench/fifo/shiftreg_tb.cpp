// DESCRIPTION: Verilator: Verilog example module
//
// This file ONLY is placed under the Creative Commons Public Domain, for
// any use, without warranty, 2017 by Wilson Snyder.
// SPDX-License-Identifier: CC0-1.0
//======================================================================

// For std::unique_ptr
#include <memory>

// Include common routines
#include <verilated.h>

// Include model header, generated from Verilating the modules
#include "VShiftRegisterFIFOTester.h"

#include "verilated_vcd_c.h"

// Legacy function required only so linking works on Cygwin and MSVC++
double sc_time_stamp() { return 0; }

int main(int argc, char **argv, char **env) {


  const auto top = std::make_unique<VShiftRegisterFIFOTester>();

  Verilated::traceEverOn(true);
  VerilatedVcdC *tfp = new VerilatedVcdC;
  top->trace(tfp, 99); // Trace 99 levels of hierarchy
  tfp->open("vtrace.vcd");

  int time_out = 1000;
  int time = 0;
  top->clk = 0;

  // Simulate until $finish
  while (!Verilated::gotFinish() && time < time_out) {
    time ++;
    top->clk = !top->clk;
    top->eval();
    tfp->dump(time);

  }

  // Final model cleanup
  top->final();
  tfp->close();

  return 0;
}
