#ifndef __COSIM_HARNESS_HPP__
#define __COSIM_HARNESS_HPP__
#include "VManticoreFlatSimKernel.h"
#include <memory>
#include <verilated_vcd_c.h>
enum SlaveAddress {
  SCHEDULE_LENGTH = 0,
  GLOBAL_MEMORY_INSTRUCTION_BASE,
  VALUE_CHANGE_SYMBOL_TABLE_BASE,
  VALUE_CHANGE_LOG_BASE,
  BOOTLOADER_CYCLES,
  EXCEPTION_ID_0,
  SLAVE_ADDRESS_END
};
class ManticoreKernelSimulator {
public:
  ManticoreKernelSimulator();
  void set(SlaveAddress addr, uint64_t value);
  uint64_t get(SlaveAddress addr);
  void tick();
  void start();
  void simulate(uint32_t max_cycles);

private:
  std::unique_ptr<VManticoreFlatSimKernel> m_kernel;
  std::unique_ptr<VerilatedVcdC> m_tfp;
  uint64_t m_time = 0;
};

extern "C" {

void *ctor();

void set(void *obj, uint32_t addr, uint64_t value);

uint64_t get(void *obj, uint32_t addr);

void tick(void *obj);

void simulate(void *obj, uint32_t max_cycles);

void start(void *obj);

uint32_t run_state();
}

#endif
