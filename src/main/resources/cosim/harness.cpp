#include "harness.hpp"

ManticoreKernelSimulator::ManticoreKernelSimulator() {

  m_kernel = std::make_unique<VManticoreFlatSimKernel>("kernel");
  m_tfp = std::make_unique<VerilatedVcdC>();
  Verilated::traceEverOn(true);
  m_kernel->trace(m_tfp.get(), 20);
  m_tfp->open("trace.vcd");

  m_time = 0;
  m_kernel->ap_clk = 0;
  m_kernel->ap_rst = 1;
  tick();
  tick();
  tick();
  m_kernel->ap_rst = 0;
}

void ManticoreKernelSimulator::set(SlaveAddress addr, uint64_t value) {

  switch (addr) {
  case SCHEDULE_LENGTH:
    m_kernel->kernel_registers_host_schedule_length = value;
    break;
  case GLOBAL_MEMORY_INSTRUCTION_BASE:
    m_kernel->kernel_registers_host_global_memory_instruction_base = value;
    break;
  case VALUE_CHANGE_SYMBOL_TABLE_BASE:
    m_kernel->kernel_registers_host_value_change_symbol_table_base = value;
    break;
  case VALUE_CHANGE_LOG_BASE:
    m_kernel->kernel_registers_host_value_change_log_base = value;
    break;
  default:
    throw std::invalid_argument("invalid host registers address");
    break;
  }
}

uint64_t ManticoreKernelSimulator::get(SlaveAddress addr) {
  uint64_t value = -1;
  switch (addr) {
  case BOOTLOADER_CYCLES:
    value = m_kernel->kernel_registers_device_bootloader_cycles;
    break;
  case EXCEPTION_ID_0:
    value = m_kernel->kernel_registers_device_exception_id_0;
    break;
  default:
    throw std::invalid_argument("invalid device registers address");
    break;
  }
  return value;
}

void ManticoreKernelSimulator::tick() {
  m_kernel->ap_clk = 0;
  m_time++;
  m_kernel->eval();
  if (m_tfp)
    m_tfp->dump(m_time);
  m_kernel->ap_clk = 1;
  m_time++;
  m_kernel->eval();
  if (m_tfp)
    m_tfp->dump(m_time);
}

void ManticoreKernelSimulator::start() {
  for (int i = 0; i < 20; i++) {
    tick();
  }

  uint32_t cycles = 0;
  m_kernel->kernel_ctrl_start = 1;
  tick();
  cycles++;
  m_kernel->kernel_ctrl_start = 0;
}

void ManticoreKernelSimulator::simulate(uint32_t max_cycles) {
  start();
  uint32_t cycles = 0;
  while (cycles < max_cycles || !m_kernel->kernel_ctrl_idle) {
    tick();
    cycles++;
  }
  printf("Stopped simulation after %d cycles", cycles);
  printf("\n");
  printf("\tBootloader: % lu\n",
         m_kernel->kernel_registers_device_bootloader_cycles);
  printf("\tExceptionId0: % d\n",
         m_kernel->kernel_registers_device_exception_id_0);
  printf("\tVirtualCycles % lu\n",
         m_kernel->kernel_registers_device_virtual_cycles);
  tick();
  tick();
  tick();
  tick();
}

void *ctor() {
  return reinterpret_cast<void *>(new ManticoreKernelSimulator());
}

void set(void *obj, uint32_t addr, uint64_t value) {
  reinterpret_cast<ManticoreKernelSimulator *>(obj)->set(
      static_cast<SlaveAddress>(addr), value);
}

uint64_t get(void *obj, uint32_t addr) {
  return reinterpret_cast<ManticoreKernelSimulator *>(obj)->get(
      static_cast<SlaveAddress>(addr));
}

void tick(void *obj) {
  reinterpret_cast<ManticoreKernelSimulator *>(obj)->tick();
}

void simulate(void *obj, uint32_t max_cycles) {
  reinterpret_cast<ManticoreKernelSimulator *>(obj)->simulate(max_cycles);
}

void start(void *obj) {
  reinterpret_cast<ManticoreKernelSimulator *>(obj)->start();
}

// int main(int argc, char *argv[]) {

//   Verilated::traceEverOn(true);

//   void *sim_obj = ctor();

//   simulate(sim_obj, 1000);
// }