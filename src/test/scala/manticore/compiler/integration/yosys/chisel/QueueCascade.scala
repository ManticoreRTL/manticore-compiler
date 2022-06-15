package manticore.compiler.integration.yosys.chisel

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage

class QueueCascadeIO(dataBits: Int) extends Bundle {
  val enq = Flipped(EnqIO(UInt(dataBits.W)))
  val deq = Flipped(DeqIO(UInt(dataBits.W)))
}

class QueueCascade(dataBits: Int, count: Int) extends Module {

  val numEntries = 32
  val io = IO(new QueueCascadeIO(dataBits))

  io.deq <> Range(0, count).foldLeft(io.enq) { case (enq, _) =>
    val q = Queue(enq, numEntries, false, false, true)
    q
  }

}

// class QueueCascadeTestBench(dataBits: Int, count: Int) extends Module {


//   val counter = Counter(1024)

//   val dut_rst = WireDefault(counter.value < 10.U)

//   val dut = withReset(dut_rst) { Module(new QueueCascade(dataBits, count)) }

//   val data = Mem(64, UInt(dataBits.W))
//   val dataIn = Reg(UInt(dataBits.W))
//   val validIn = RegInit(false.B)
//   val pointer = RegInit(UInt(16.W), 0.U)

//   dut.io.enq.valid := validIn
//   dut.io.enq.bits := dataIn
//   dataIn := data(pointer)
//   validIn := true.B
//   when(dut.io.enq.ready) {
//     pointer := pointer + 1.U
//   }


//   dut.io.deq.ready := true.B
//   when(dut.io.deq.valid) {
//     printf("%d", dut.io.deq.bits)
//   }

// }

object QueueCascadeGenerator extends App {

  new ChiselStage().emitVerilog(new QueueCascade(16, 64), Array("-td", "dumps"))

}
