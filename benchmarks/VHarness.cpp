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
#include "VMain.h"

#include "verilated_vcd_c.h"
#include <chrono>
#include <stdio.h>
// Legacy function required only so linking works on Cygwin and MSVC++
double sc_time_stamp() { return 0; }

int main(int argc, char **argv, char **env) {

  auto start = std::chrono::high_resolution_clock::now();

  const auto top = std::make_unique<VMain>();

  if (argc != 2) {
    printf("TIMEOUT not specified!\n");
    std::exit(-1);
  }
  // Verilated::traceEverOn(true);
  // VerilatedVcdC *tfp = new VerilatedVcdC;
  // top->trace(tfp, 99); // Trace 99 levels of hierarchy
  // tfp->open("trace.vcd");

  unsigned int time_out = std::stoi(argv[1]) << 1;
  printf("Timeout cycles = %u\n", time_out >> 1);
  int time = 0;
  top->clock = 0;

  // Simulate until $finish
  while (!Verilated::gotFinish() && time < time_out) {
    time ++;
    top->clock = !top->clock;
    top->eval();
    // tfp->dump(time);


  }
  auto end = std::chrono::high_resolution_clock::now();
  auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end - start);

  printf("Finished after %d cycles in %.3f seconds\n", time >> 1, static_cast<float>(duration.count()) / 1000.);
  // Final model cleanup
  // top->final();
  // tfp->close();

  return 0;
}
