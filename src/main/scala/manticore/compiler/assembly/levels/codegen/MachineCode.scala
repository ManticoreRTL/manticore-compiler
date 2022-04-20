package manticore.compiler.assembly.levels.codegen

import manticore.compiler.AssemblyContext

import manticore.compiler.assembly.levels.placed.PlacedIR._
import manticore.compiler.assembly.BinaryOperator
import manticore.compiler.assembly.levels.AssemblyChecker
import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.HasTransformationID
import manticore.compiler.assembly.levels.TransformationID
import manticore.compiler.assembly.levels.placed.LatencyAnalysis

import java.io.File
import java.io.PrintWriter
import java.nio.file.Files
import manticore.compiler.assembly.levels.ConstType
import manticore.compiler.assembly.levels.InputType
import manticore.compiler.assembly.levels.MemoryType

import manticore.compiler.assembly.levels.UInt16
import java.nio.file.Path
import java.io.FileOutputStream

object MachineCodeGenerator
    extends ((DefProgram, AssemblyContext) => Unit)
    with HasTransformationID {

  override def apply(prog: DefProgram, ctx: AssemblyContext): Unit = {

    val assembled = assembleProgram(prog)(ctx)
    generateCode(assembled)(ctx)
    if (ctx.logger.countErrors() > 0) {
      ctx.logger.fail("Aborted due to earlier errors!")
    }
  }

  def generateCode(
      assembled: Seq[AssembledProcess]
  )(implicit ctx: AssemblyContext): Unit =
    ctx.output_dir match {
      case Some(dir_name) =>
        generateCode(
          assembled,
          dir_name.toPath()
        )
      case _ =>
        ctx.logger.error(
          "Output directory not specified! Skipping code generation!"
        )
    }

  def makeBinaryStream(
      assembled: Seq[AssembledProcess]
  )(implicit ctx: AssemblyContext): Seq[Int] = {

    // +++++++++++++++++++++ MANTICORE BINARY STREAM FORMAT+++++++++++++++++++++++++++
    // the binary format of a manticore program consists of uint16s: each process
    // get its own contiguous block starting with a header and a ending with a
    // footer illustrate below:
    // ==================BINARY FORMAT FOR A PROCESS============================
    // HEADER : 2 shorts, location and program length (excluding instructions spawned dynamically from the NoC)
    // BODY   : main payload, assembled instructions, little-endian (LSB first)
    // FOOTER : Meta data for processor wake up and sleep, 2 shorts
    // =========================================================================
    // HEADER is:
    // 0x0000: LOC X << 8 | LOC Y << 0
    // 0x0001: PROGRAM_BODY_LENGTH
    // The first short encodes the location for which this binary is
    // destined for which is use by the boot loader.
    // PROGRAM_BODY_LENGTH is the length of program excluding the SetValue
    // instructions spawned at runtime by the NoC message from other processes
    // in number of 64-bit words (not shorts).
    // BODY:
    // Each instruction is 64 bits and therefore is broken down to 4 shorts in
    // little-endian order (least significant short first).
    // FOOTER:
    // 0x0000 + PROGRAM_BODY_LENGTH * 4 + 1: EPILOGUE_LENGTH
    // 0x0001 + PROGRAM_BODY_LENGTH * 4 + 1: SLEEP_LENGTH
    // The footer contains necessary control information for virtual cycle roll
    // over.
    // EPILOGUE_LENGTH is the number of message a process is expected to HAVE
    // RECEIVED during the first PROGRAM_BODY_LENGTH instruction executions.
    // Therefore, a processor will end up executing
    // PROGRAM_BODY_LENGTH + EPILOGUE_LENGTH instruction at every virtual cycle
    // and the roll over to the first instruction.
    // SLEEP_LENGTH indicates the number of cycles a processor should go to sleep
    // after it has executed all of teh PROGRAM_BODY_LENGTH + EPILOGUE_LENGTH
    // instructions. The SLEEP_LENGTH cycles is used to synchronize all processors
    // at the end of a virtual cycle.
    /// IMPORTANT: It is a good idea to have SLEEP_CYCLE >= MIN_LATENCY
    // to ensure that all instructions are finished write back when a new
    // virtual cycles begins.
    //++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

    val vcycle_length =
      assembled.map(_.total).max + LatencyAnalysis.maxLatency()
    ctx.logger.info(s"Virtual cycle length: ${vcycle_length}")
    // for each process p , we need to compute the SLEEP_LENGTH as
    // vcycle_length - p.total (total is the total execution time including epilogue)

    val binary_stream: Seq[Int] = assembled.sortBy(p => p.place).flatMap {
      case AssembledProcess(_, body, loc, epilogue_length, total_length) =>
        Seq(
          (loc._2 << 8 | loc._1).toInt,
          (total_length - epilogue_length).toInt
        ) ++ body.flatMap { w64: Long =>
          Seq(
            w64 & 0xffff,
            (w64 >> 16L) & 0xffff,
            (w64 >> 32L) & 0xffff,
            (w64 >> 48L) & 0xffff
          ).map(_.toInt)
        } ++ Seq(
          epilogue_length.toInt,
          (vcycle_length - total_length).toInt
        )
    }
    binary_stream
  }

  def generateCode(
      assembled: Seq[AssembledProcess],
      dir_name: Path
  )(implicit ctx: AssemblyContext): Unit = {

    val binary_stream: Seq[Int] = makeBinaryStream(assembled)

    // print the instructions in ASCII format for debugging
    if (ctx.dump_ascii) {
      Files.createDirectories(dir_name)
      assembled.foreach { case AssembledProcess(proc, aproc, _, _, _) =>
        if (aproc.length > 0) {
          val f = dir_name.resolve(s"${proc.id}.masm")
          val printer = new PrintWriter(f.toFile())
          printer.println(s".proc ${proc.id}:")
          printer.println(
            "//--------------- REGISTERS ----------------------//"
          )
          proc.registers
            .groupBy { r => r.variable.id }
            .toSeq
            .sortBy(_._1)
            .foreach { case (id, regs) =>
              printer.println(s"\tr${id}:")
              regs.foreach { rr =>
                printer.println(
                  s"\t\t${rr.variable.varType.typeName} ${rr.variable.name} ${if (rr.value.nonEmpty)
                    rr.value.get.toInt.toString
                  else ""}"
                )
              }
            }
          printer.println("// ---------------- BODY ---------------------- //")
          proc.body.zipWithIndex.zip(aproc).foreach {
            case ((inst, ix), bincode) =>
              val bin_str = f"${bincode.toBinaryString}%64s".replace(' ', '0')
              printer.println(
                f"${ix}%4d |${inst.toString().strip()}%-50s \t | ${bin_str}"
              )
          }
          printer.flush()
          printer.close()
        }
      }
    }

    def writeToFile(file_name: File, data: Iterable[Int]): Unit = {
      val file_writer = new PrintWriter(file_name)
      // write in ASCII binary format, not very efficient, but only used for
      // prototyping
      data.foreach { x =>
        require(x <= 0xffff)
        file_writer.println(f"${x.toBinaryString}%16s".replace(' ', '0'))
      }
      file_writer.close()
      ctx.logger.info(s"Finished writing ${file_name.toPath.toAbsolutePath}")
    }

    def writeToBinaryFile(file_name: File, data: Iterable[Int]): Unit = {

      val file_writer = new FileOutputStream(file_name)
      data.foreach { x =>
        // little endian
        file_writer.write(x.toByte)
        file_writer.write((x >> 8).toByte)

      }
      file_writer.close()
      ctx.logger.info(s"Finished writing to ${file_name.toPath.toAbsolutePath}")

    }
    Files.createDirectories(dir_name)
    val exe_file = dir_name.resolve("exec.dat").toFile()
    if (ctx.dump_ascii) {
      writeToFile(exe_file, binary_stream)
    }
    writeToBinaryFile(dir_name.resolve("exec.bin").toFile(), binary_stream)

    // write initial register values
    // note that at this point registers are allocated and the ones with
    // initial values (constants and inputs) are placed first

    assembled.foreach { case AssembledProcess(p, _, _, _, _) =>
      // write initial register values
      if (ctx.dump_rf) {
        val initial_reg_vals = p.registers
          .takeWhile { r =>
            r.variable.varType == ConstType || r.variable.varType == InputType || r.variable.varType == MemoryType
          }
          .map { v => v.value.getOrElse(UInt16(0)).toInt }
        val rf_file =
          dir_name.resolve(s"rf_${p.id.x}_${p.id.y}.dat").toFile()
        writeToFile(rf_file, initial_reg_vals)
      }
      if (ctx.dump_ra) {

        // write initial memory values

        val initial_mem_values = Array.fill(ctx.max_local_memory) {
          0
        }
        p.registers.foreach {
          case _ @DefReg(v: MemoryVariable, Some(UInt16(offset)), _) =>
            v.block.initial_content.zipWithIndex.foreach { case (value, ix) =>
              initial_mem_values(offset + ix) = value.toShort
            }
          case _ => // do nothing
        }
        val ra_file =
          dir_name.resolve(s"ra_${p.id.x}_${p.id.y}.dat").toFile()
        writeToFile(ra_file, initial_mem_values)
      }
    }
    ctx.logger.flush()
  }
  def assembleProgram(
      prog: DefProgram
  )(implicit ctx: AssemblyContext): Seq[AssembledProcess] = {
    val ids: Map[ProcessId, Map[Name, Int]] = prog.processes.map { p =>
      p.id -> p.registers.map { r => r.variable.name -> r.variable.id }.toMap
    }.toMap
    // create a grid for processes initially empty
    // and then replace them if a processes actually exists. We do this because
    // the current version of the boot loader programs all processors, even
    // if a processor does not really have a process to run so we have to
    // place them in the binary
    val grid = Seq
      .tabulate(ctx.max_dimx) { x =>
        Seq
          .tabulate(ctx.max_dimy) { y =>
            AssembledProcess(
              orig = DefProcess(
                ProcessIdImpl(s"empty_${x}_${y}", x, y),
                Seq(),
                Seq(),
                Seq()
              ),
              body = Seq.empty[Long],
              place = (x, y),
              epilogue = 0,
              total = 0
            )
          }
          .toArray[AssembledProcess]
      }
      .toArray[Array[AssembledProcess]]

    prog.processes.foreach { p =>
      // if the process exists, put it on the grid
      grid(p.id.x)(p.id.y) = assembleProcess(p)(ctx, ids)

    }

    grid.flatten.toSeq
  }
  case class AssembledProcess(
      orig: DefProcess,
      body: Seq[Long],
      place: (Int, Int),
      epilogue: Int,
      total: Int
  )
  def assembleProcess(
      proc: DefProcess
  )(implicit
      ctx: AssemblyContext,
      ids: ProcessId => Name => Int
  ): AssembledProcess = {

    val recv_count = proc.body.filter {
      case _: Recv => true
      case _       => false
    }.length
    implicit val asm = Assembler()
    // the schedule length on this process is the proc.body.length + recv_count
    implicit val local: Name => Int = ids(proc.id)
    implicit val remote: (ProcessId, Name) => Int = (i, n) => ids(i)(n)

    val assembled = proc.body.map(assemble(proc, _))
    AssembledProcess(
      proc,
      assembled,
      (proc.id.x, proc.id.y),
      recv_count,
      recv_count + proc.body.length
    )
  }
  sealed abstract class Field(val startIndex: Int, val bitLength: Int) {
    val endIndex = startIndex + bitLength - 1
  }
  case object OpcodeField extends Field(0, 4)
  case object RdField extends Field(OpcodeField.endIndex + 1, 11)
  case object FunctField extends Field(RdField.endIndex + 1, 5)
  case object Rs1Field extends Field(FunctField.endIndex + 1, 11)
  case object Rs2Field extends Field(Rs1Field.endIndex + 1, 11)
  case object Rs3Field extends Field(Rs2Field.endIndex + 1, 11)
  case object Rs4Field extends Field(Rs3Field.endIndex + 1, 11)
  case object ImmField extends Field(Rs1Field.endIndex + 1, 33)

  object Opcodes extends Enumeration {
    type Type = Value
    val NOP = Value(0x0)
    val SET = Value(0x1)
    val CUST0 = Value(0x2)
    val ARITH = Value(0x3)
    val LLOAD = Value(0x4)
    val LSTORE = Value(0x5)
    val EXPECT = Value(0x6)
    val GLOAD = Value(0x7)
    val GSTORE = Value(0x8)
    val SEND = Value(0x9)
    val PREDICATE = Value(0xa)
    val SETCARRY = Value(0xb)
  }

  final class Assembler() {
    var inst: Long = 0L
    var pos: Long = 0L
    def Rd(value: Int): Assembler = <<(value, RdField)
    def Funct(value: BinaryOperator.BinaryOperator): Assembler =
      <<(value.id, FunctField)
    def Rs1(value: Int): Assembler = <<(value, Rs1Field)
    def Rs2(value: Int): Assembler = <<(value, Rs2Field)
    def Rs3(value: Int): Assembler = <<(value, Rs3Field)
    def Rs4(value: Int): Assembler = <<(value, Rs4Field)
    def Zero(length: Int): Assembler = <<(0, length)
    def Immediate(value: Int): Assembler = <<(value, 16)
    def DestX(value: Int): Assembler = <<(value, 8)
    def DestY(value: Int): Assembler = <<(value, 8)
    private def <<(value: Int, field: Field): Assembler =
      <<(value, field.bitLength)
    private def <<(value: Int, bit_length: Int): Assembler = {
      require(value >= 0)
      inst = inst | (value.toLong << pos)
      pos += bit_length
      this
    }
    @inline final def Opcode(opcode: Opcodes.Type): Assembler = {
      inst = opcode.id
      pos = OpcodeField.bitLength
      this
    }
    @inline final def toLong: Long = inst

  }
  object Assembler {
    def apply() = new Assembler()
  }

  import scala.language.postfixOps

  def assemble(
      inst: BinaryArithmetic
  )(implicit
      ctx: AssemblyContext,
      asm: Assembler,
      ids: Name => Int
  ): Long = {

    val rd = ids(inst.rd)
    val rs1 = ids(inst.rs1)
    val rs2 = ids(inst.rs2)
    asm
      .Opcode(Opcodes.ARITH)
      .Rd(rd)
      .Funct(inst.operator)
      .Rs1(rs1)
      .Rs2(rs2)
      .Zero(Rs3Field.bitLength + Rs4Field.bitLength)
      .toLong
  }
  def assemble(
      proc: DefProcess,
      inst: Instruction
  )(implicit
      ctx: AssemblyContext,
      asm: Assembler,
      local: Name => Int,
      remote: (ProcessId, Name) => Int
  ): Long = {

    val res = inst match {
      case i: BinaryArithmetic => assemble(i)
      case i: CustomInstruction =>
        ctx.logger.error(s"Can not generate code", i)
        0L
      case LocalLoad(rd, base, offset, annons) =>
        asm
          .Opcode(Opcodes.LLOAD)
          .Rd(local(rd))
          .Funct(BinaryOperator.ADD)
          .Rs1(local(base))
          .Zero(
            Rs2Field.bitLength + Rs3Field.bitLength + Rs4Field.bitLength - 16
          )
          .Immediate(offset.toInt)
          .toLong
      case LocalStore(rs, base, offset, predicate, annons) =>
        if (predicate.nonEmpty) {
          ctx.logger.error("can not handle explicit predicate", inst)
        }
        asm
          .Opcode(Opcodes.LSTORE)
          .Zero(RdField.bitLength)
          .Funct(BinaryOperator.ADD)
          .Rs1(local(base))
          .Rs2(local(rs))
          .Zero(Rs3Field.bitLength + Rs4Field.bitLength - 16)
          .Immediate(offset.toInt)
          .toLong
      case Send(rd, rs, dest_id, annons) =>
        // in the machine code, we don't really specify the
        // destination, but rather the path to the destination
        // in terms of x and y hops
        val x_hops =
          LatencyAnalysis.xHops(proc.id, dest_id, (ctx.max_dimx, ctx.max_dimy))
        val y_hops =
          LatencyAnalysis.yHops(proc.id, dest_id, (ctx.max_dimx, ctx.max_dimy))
        asm
          .Opcode(Opcodes.SEND)
          .Rd(remote(dest_id, rd))
          .Funct(BinaryOperator.ADD)
          .Zero(Rs1Field.bitLength)
          .Rs2(local(rs))
          .Zero(Rs3Field.bitLength + Rs4Field.bitLength - 16)
          .DestX(x_hops)
          .DestY(y_hops)
          .toLong
      case _: Recv => assemble(proc, Nop)
      case Expect(ref, got, error_id, annons) =>
        asm
          .Opcode(Opcodes.EXPECT)
          .Zero(RdField.bitLength)
          .Funct(BinaryOperator.SEQ)
          .Rs1(local(ref))
          .Rs2(local(got))
          .Zero(Rs3Field.bitLength + Rs4Field.bitLength - 16)
          .Immediate(error_id.id.toInt)
          .toLong
      case Predicate(rs, annons) =>
        asm
          .Opcode(Opcodes.PREDICATE)
          .Zero(RdField.bitLength)
          .Funct(BinaryOperator.ADD)
          .Rs1(local(rs))
          .Zero(Rs2Field.bitLength + Rs3Field.bitLength + Rs4Field.bitLength)
          .toLong
      case Mux(rd, sel, rfalse, rtrue, annons) =>
        asm
          .Opcode(Opcodes.ARITH) // MUX is not a real Opcode
          .Rd(local(rd))
          .Funct(BinaryOperator.MUX)
          .Rs1(local(rfalse))
          .Rs2(local(rtrue))
          .Rs3(local(sel))
          .Zero(Rs4Field.bitLength)
          .toLong
      case Nop => asm.Opcode(Opcodes.NOP).toLong
      case ClearCarry(carry, annons) =>
        asm
          .Opcode(Opcodes.SETCARRY)
          .Rd(local(carry))
          .Funct(BinaryOperator.ADD)
          .Zero(
            Rs1Field.bitLength + Rs2Field.bitLength + Rs3Field.bitLength + Rs4Field.bitLength - 16
          )
          .Immediate(0)
          .toLong
      case SetCarry(carry, annons) =>
        asm
          .Opcode(Opcodes.SETCARRY)
          .Rd(local(carry))
          .Funct(BinaryOperator.ADD)
          .Zero(
            Rs1Field.bitLength + Rs2Field.bitLength +
              Rs3Field.bitLength + Rs4Field.bitLength - 16
          )
          .Immediate(1)
          .toLong
      case Mov(rd, rs, _) =>
        asm
          .Opcode(Opcodes.ARITH)
          .Rd(local(rd))
          .Funct(BinaryOperator.ADD)
          .Rs1(local(rs))
          .Rs2(0) // reg 0 is tied to zero
          .Zero(Rs3Field.bitLength + Rs4Field.bitLength)
          .toLong
      case AddC(rd, co, rs1, rs2, ci, _) =>
        asm
          .Opcode(Opcodes.ARITH) // Note that ADDCARRY is not a real opcode
          .Rd(local(rd))
          .Funct(BinaryOperator.ADDC)
          .Rs1(local(rs1))
          .Rs2(local(rs2))
          .Rs3(local(ci))
          .Rs4(local(co))
          .toLong
      case SetValue(rd, value, _) =>
        asm
          .Opcode(Opcodes.SET)
          .Rd(local(rd))
          .Funct(BinaryOperator.ADD)
          .Zero(
            Rs1Field.bitLength + Rs2Field.bitLength +
              Rs3Field.bitLength + Rs4Field.bitLength - 16
          )
          .Immediate(value.toInt)
          .toLong
      case _ =>
        ctx.logger.error("can not handle instruction", inst)
        0L

    }
    res
  }

}
