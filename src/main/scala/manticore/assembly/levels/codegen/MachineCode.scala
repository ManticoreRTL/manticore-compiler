package manticore.assembly.levels.codegen

import scala.annotation.meta.field
import manticore.compiler.AssemblyContext

import manticore.assembly.levels.placed.PlacedIR._
import manticore.assembly.BinaryOperator
import manticore.assembly.levels.AssemblyChecker
import manticore.assembly.levels.AssemblyTransformer
import manticore.assembly.levels.HasTransformationID
import manticore.assembly.levels.TransformationID
import manticore.assembly.levels.placed.LatencyAnalysis
import java.io.FileOutputStream
import java.io.File
import java.io.BufferedWriter
import java.io.PrintWriter
import java.nio.file.Files

object MachineCodeGenerator
    extends ((DefProgram, AssemblyContext) => Unit)
    with HasTransformationID {

  override def apply(prog: DefProgram, ctx: AssemblyContext): Unit = {

    val ids = prog.processes.flatMap { p =>
      p.registers.map { r => r.variable.name -> r.variable.id }
    }.toMap
    // compute the virtual cycle length
    val assembled = prog.processes.map { assembleProcess(_)(ctx, ids) }

    // +++++++++++++++++++++++ BINARY STREAM FORMAT+++++++++++++++++++++++++++++
    // the binary format of a manticore program consists of uint16: each process
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
    // for each process p , we need to compute the SLEEP_LENGTH as
    // vcycle_length - p.total (total is the total execution time including epilogue)

    val binary_stream: Seq[Short] = assembled.sortBy(p => p.place).flatMap {
      case AssembledProcess(body, loc, epilogue_length, total_length) =>
        Seq(
          (loc._2 << 8 | loc._1).toShort,
          (total_length - epilogue_length).toShort
        ) ++ body.flatMap { w64: Long =>
          Seq(
            w64 & 0xffff,
            (w64 >> 16L) & 0xffff,
            (w64 >> 32L) & 0xffff,
            (w64 >> 64L) & 0xffff
          ).map(_.toShort)
        } ++ Seq(
          epilogue_length.toShort,
          (vcycle_length - total_length).toShort
        )
    }
    ctx.output_file match {
      case Some(file_name: File) =>
        Files.createDirectories(file_name.toPath().getParent())
        val name_parts = file_name.toPath().getFileName().toString().split('.')
        if (name_parts.length > 1) {
          name_parts.last match {
            case "bin" => // emit binary
              val file_writer = new FileOutputStream(file_name)
              file_writer.write(
                binary_stream
                  .flatMap { shrt => Seq(shrt & 0xff, (shrt >> 8) & 0xff) }
                  .map(_.toByte)
                  .toArray
              )
              file_writer.close()
            case "hex" => // emit ASCII hex
              val file_writer = new PrintWriter(file_name)
              binary_stream.foreach { x => file_writer.println(f"${x}%x") }
              file_writer.close()
              ctx.logger.info(
                s"Finished writing ${file_name.toPath.toAbsolutePath}"
              )
            case ext @ _ =>
              ctx.logger.error(s"unsupported file type ${ext}")
          }
        } else {

          ctx.logger.error("output file name requires valid extension")

        }

      case None => // oops,
        ctx.logger.warn(
          "Output file path not specified! Machine code will not be written."
        )
    }
    ctx.logger.flush()
  }

  case class AssembledProcess(
      body: Seq[Long],
      place: (Int, Int),
      epilogue: Int,
      total: Int
  )
  def assembleProcess(
      proc: DefProcess
  )(implicit ctx: AssemblyContext, ids: Map[Name, Int]): AssembledProcess = {

    val recv_count = proc.body.filter {
      case _: Recv => true
      case _       => false
    }.length
    implicit val asm = Assembler()
    // the schedule length on this process is the proc.body.length + recv_count
    val assembled = proc.body.map(assemble)
    AssembledProcess(
      assembled,
      (proc.id.x, proc.id.y),
      recv_count,
      recv_count + proc.body.length
    )
  }
  sealed abstract class Field(val startIndex: Int, val bitLength: Int) {
    val endIndex = startIndex + bitLength - 1
  }
  case object OpcodeField extends Field(0, 5)
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
    val ADDCARRY = Value(0xb)
    val MUX = Value(0xc)
    val SETCARRY = Value(0xd)
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
      inst |= value.toLong << pos
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
      ids: Map[Name, Int]
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
      inst: Instruction
  )(implicit ctx: AssemblyContext, asm: Assembler, ids: Map[Name, Int]): Long =
    inst match {
      case i: BinaryArithmetic => assemble(i)
      case i: CustomInstruction =>
        ctx.logger.error(s"Can not generate code", i)
        0L
      case LocalLoad(rd, base, offset, annons) =>
        asm
          .Opcode(Opcodes.LLOAD)
          .Rd(ids(rd))
          .Funct(BinaryOperator.ADD)
          .Rs1(ids(base))
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
          .Rs1(ids(base))
          .Rs2(ids(rs))
          .Zero(Rs3Field.bitLength + Rs4Field.bitLength - 16)
          .Immediate(offset.toInt)
          .toLong
      case Send(rd, rs, dest_id, annons) =>
        asm
          .Opcode(Opcodes.SEND)
          .Rd(ids(rd))
          .Funct(BinaryOperator.ADD)
          .Zero(Rs1Field.bitLength)
          .Rs2(ids(rs))
          .Zero(Rs3Field.bitLength + Rs4Field.bitLength)
          .DestX(dest_id.x)
          .DestY(dest_id.y)
          .toLong
      case Recv(rd, rs, source_id, annons) => assemble(Nop)
      case Expect(ref, got, error_id, annons) =>
        asm
          .Opcode(Opcodes.EXPECT)
          .Zero(RdField.bitLength)
          .Funct(BinaryOperator.SEQ)
          .Rs1(ids(ref))
          .Rs2(ids(got))
          .Zero(Rs3Field.bitLength + Rs4Field.bitLength)
          .Immediate(error_id.id.toInt)
          .toLong
      case Predicate(rs, annons) =>
        asm
          .Opcode(Opcodes.PREDICATE)
          .Zero(RdField.bitLength)
          .Funct(BinaryOperator.ADD)
          .Rs1(ids(rs))
          .Zero(Rs2Field.bitLength + Rs3Field.bitLength + Rs4Field.bitLength)
          .toLong
      case Mux(rd, sel, rfalse, rtrue, annons) =>
        asm
          .Opcode(Opcodes.ARITH)
          .Rd(ids(rd))
          .Funct(BinaryOperator.MUX)
          .Rs1(ids(rfalse))
          .Rs2(ids(rtrue))
          .Rs3(ids(sel))
          .Zero(Rs4Field.bitLength)
          .toLong
      case Nop => asm.Opcode(Opcodes.NOP).toLong
      case ClearCarry(carry, annons) =>
        asm
          .Opcode(Opcodes.SETCARRY)
          .Rd(ids(carry))
          .Funct(BinaryOperator.ADD)
          .Zero(
            Rs1Field.bitLength + Rs2Field.bitLength + Rs3Field.bitLength + Rs4Field.bitLength - 16
          )
          .Immediate(0)
          .toLong
      case SetCarry(carry, annons) =>
        asm
          .Opcode(Opcodes.SETCARRY)
          .Rd(ids(carry))
          .Funct(BinaryOperator.ADD)
          .Zero(
            Rs1Field.bitLength + Rs2Field.bitLength + Rs3Field.bitLength + Rs4Field.bitLength - 16
          )
          .Immediate(1)
          .toLong
      case Mov(rd, rs, _) =>
        asm
          .Opcode(Opcodes.ARITH)
          .Rd(ids(rd))
          .Funct(BinaryOperator.ADD)
          .Rs1(ids(rs))
          .Rs2(0) // reg 0 is tied to zero
          .Zero(Rs3Field.bitLength + Rs4Field.bitLength)
          .toLong
      case AddC(rd, co, rs1, rs2, ci, _) =>
        asm
          .Opcode(Opcodes.ADDCARRY)
          .Rd(ids(rd))
          .Funct(BinaryOperator.ADD)
          .Rs1(ids(rs1))
          .Rs2(ids(rs2))
          .Rs3(ids(ci))
          .Rs4(ids(co))
          .toLong
      case _ =>
        ctx.logger.error("can not handle instruction", inst)
        0L

    }

}
