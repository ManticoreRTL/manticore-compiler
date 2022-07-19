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

#if VM_TRACE
#include "verilated_vcd_c.h"
#endif
#include <chrono>
#include <stdio.h>

#ifdef VL_USER_FINISH
// #define VL_USER_FINISH ///< Define this to override the vl_finish function
void vl_finish(const char *filename, int linenum,
               const char *hier) VL_MT_UNSAFE {
  Verilated::gotFinish(true);
}
#endif

// Legacy function required only so linking works on Cygwin and MSVC++
double sc_time_stamp() { return 0; }

int main(int argc, char **argv, char **env) {

  auto start = std::chrono::high_resolution_clock::now();

  const auto top = std::make_unique<VMain>();

  if (argc != 2) {
    printf("TIMEOUT not specified!\n");
    std::exit(-1);
  }
#if VM_TRACE
  Verilated::traceEverOn(true);
  VerilatedVcdC *tfp = new VerilatedVcdC;
  top->trace(tfp, 99); // Trace 99 levels of hierarchy
  tfp->open("trace.vcd");
#endif

  unsigned int time_out = std::stoi(argv[1]) << 1;
  // printf("Timeout cycles = %u\n", time_out >> 1);
  int time = 0;
  top->clock = 0;

  // Simulate until $finish
  while (!Verilated::gotFinish() && time < time_out) {
    time++;
    top->clock = !top->clock;
    top->eval();
#if VM_TRACE
    tfp->dump(time);
#endif
  }
  auto end = std::chrono::high_resolution_clock::now();
  auto duration =
      std::chrono::duration_cast<std::chrono::milliseconds>(end - start);

  // printf("Finished after %d cycles in %.3f seconds\n", time >> 1,
  //        static_cast<float>(duration.count()) / 1000.);
  // Final model cleanup
  top->final();
#if VM_TRACE
  tfp->close();
#endif

  return 0;
}