package manticore.assembly.levels.unconstrained

import manticore.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import manticore.assembly.levels.MemoryType
import manticore.assembly.annotations.Memblock
import manticore.assembly.annotations.AssemblyAnnotationFields
import manticore.assembly.annotations.IntValue
import manticore.assembly.annotations.MemInit
import manticore.assembly.annotations.StringValue
import manticore.assembly.levels.ConstType
import manticore.assembly.DependenceGraphBuilder
import manticore.assembly.levels.AssemblyChecker
import manticore.assembly.annotations.DebugSymbol

object UnconstrainedInterpreter
    extends DependenceGraphBuilder
    with AssemblyChecker[UnconstrainedIR.DefProgram] {
  val flavor = UnconstrainedIR
  import flavor._

  private final class ProcessState(val proc: DefProcess) {

    // a mutable register file
    val register_file = scala.collection.mutable.Map[Name, BigInt]() ++
      proc.registers.map { r =>
        val k = r.variable.name
        val v = r.value match {
          case Some(x) => x
          case None =>
            if (r.variable.varType == ConstType) {
              logger.error(s"constant register without initial value!", r)
            }
            BigInt(0)
        }
        k -> v
      }
    // wrapper class for memory state
    case class BlockRam(content: Array[Constant], width: Int, capacity: Int)
    // a mutable memory
    val memory: Map[Name, BlockRam] =
      proc.registers
        .collect { case m @ DefReg(LogicVariable(_, _, MemoryType), _, _) =>
          m
        }
        .map { m =>
          val memblock_cap = m.findAnnotationValue(
            Memblock.name,
            AssemblyAnnotationFields.Capacity
          )
          val cap = memblock_cap match {
            case Some(IntValue(v)) =>
              v
            case _ =>
              logger.error(s"missing memory block capacity!", m)
              0
          }
          val memblock_width = m.findAnnotationValue(
            Memblock.name,
            AssemblyAnnotationFields.Width
          )
          val width = memblock_width match {
            case Some(IntValue(v)) => v
            case _ =>
              logger.error(s"missing memory block width!", m)
              0
          }
          val meminit: Array[BigInt] = m.findAnnotation(MemInit.name) match {
            case Some(meminit_annon) =>
              // this is not safe from Scala compilers perspective, but it is safe
              // from our perspective because of the way we construct the
              // annotation fields, i.e., the result is never [[None]]
              val Some(file_name: String) =
                meminit_annon.getStringValue(AssemblyAnnotationFields.File)
              val Some(count: Int) =
                meminit_annon.getIntValue(AssemblyAnnotationFields.Count)
              if (count > cap) {
                logger.error("init file overflows memory!", m)
              }
              import java.nio.file.{Files, Path}
              val file_path = Path.of(file_name)

              try {
                scala.io.Source
                  .fromFile(file_name)
                  .getLines()
                  .slice(0, count)
                  .map {
                    BigInt(_)
                  }
                  .toArray[BigInt] ++ Array.fill(cap - count)(BigInt(0))
              } catch {
                case e: Exception =>
                  logger.error(
                    s"Could not read file ${file_path.toAbsolutePath()}"
                  )
                  Array.fill(cap)(BigInt(0))
              }
            case _ =>
              Array.fill(cap)(BigInt(0))

          }
          m.variable.name -> BlockRam(meminit, width, cap)
        }
        .toMap

    var predicate: Boolean = false
    var carry: Boolean = false
    var select: Boolean = false
    var exception_occurred: Boolean = false

  }
  private final class ProcessInterpreter(val proc: DefProcess)(implicit
      val ctx: AssemblyContext
  ) {

    // A table for looking up instructions that modify the value of a name
    val def_instructions: Map[Name, Instruction] =
      DependenceAnalysis.definingInstructionMap(proc)
    // A table to look up the set of memories a name traces back to
    val memory_blocks: Map[Name, Set[DefReg]] =
      DependenceAnalysis.memoryBlocks(proc, def_instructions)
    // A table for looking up name definitions
    val definitions: Map[Name, DefReg] = proc.registers.map { r =>
      r.variable.name -> r
    }.toMap

    val state = new ProcessState(proc)
    private case class ClipWidth(w: Int)
    private def clipped(v: BigInt)(implicit w: ClipWidth): BigInt = {
      // require(w < 32, "unsupported width!")
      v & ((BigInt(1) << w.w) - 1)
    }
    private def getSignedValue(r: Name): BigInt = {
      val unsigned_val = state.register_file(r)
      if (unsigned_val < 0) {
        logger.error(
          "Interpreter implementation has a bug, negative value detected!"
        )
        BigInt(0)
      } else {
        val w = definitions(r).variable.width
        if (unsigned_val.testBit(w - 1)) {
          -unsigned_val
        } else {
          unsigned_val
        }
      }
    }
    def interpret(inst: BinaryArithmetic): Unit = {
      import manticore.assembly.BinaryOperator._
      val BinaryArithmetic(op, rd, rs1, rs2, _) = inst
      implicit val clip_width = ClipWidth(definitions(rd).variable.width)
      val rs1_val = state.register_file(rs1)
      val rs2_val = state.register_file(rs2)
      val rd_val = op match {
        case ADD => clipped(rs1_val + rs2_val)
        case ADDC =>
          val sum = rs1_val + rs2_val + BigInt(if (state.carry) 1 else 0)
          state.carry = sum.testBit(clip_width.w)
          clipped(sum)
        case SUB => clipped(rs1_val - rs2_val)
        case OR  => clipped(rs1_val | rs2_val)
        case AND => clipped(rs1_val & rs2_val)
        case XOR => clipped((rs1_val &~ rs2_val) | (rs2_val &~ rs1_val))
        case SEQ =>
          if (
            definitions(rs1).variable.width != definitions(rs2).variable.width
          ) {
            logger.error("Width mismatch in SEQ", inst)
          }
          val is_eq = rs1_val == rs2_val
          state.select = is_eq
          BigInt(if (is_eq) 1 else 0)
        case SLL =>
          if (rs2_val.isValidInt && rs2_val <= 0xffff) {
            clipped(rs1_val << rs2_val.toInt)
          } else {
            logger.error("unsupported SLL shift amount", inst)
            BigInt(0)
          }
        case SRL =>
          // because of clipping, the integers we store are always positive
          // so we can always use the BigInt.<< signed shift right as the logical
          // one
          require(
            rs2_val >= 0,
            "something went wrong! all interpreted values should be positive!"
          )

          if (rs2_val.isValidInt && rs2_val <= 0xffff) {
            clipped(rs1_val >> rs2_val.toInt)
          } else {
            logger.error("unsupported SRL shift amount", inst)
            BigInt(0)
          }

        case SRA =>
          // since all values are positive, we should explicitly consider the
          // sign bit

          val rs1_signed_val = getSignedValue(rs1)
          if (rs2_val.isValidInt && rs2_val < 0xffff) {
            clipped(rs1_signed_val >> rs2_val.toInt)
          } else {
            logger.error("unsupported SRA shift amount", inst)
            BigInt(0)
          }
        case SLTS =>
          // again, we should consider encode the signs manually since all
          // interpreted values are stored as positive numbers
          val rs1_signed_val = getSignedValue(rs1)
          val rs2_signed_val = getSignedValue(rs2)
          val is_less = rs1_signed_val < rs2_signed_val
          state.select = is_less
          BigInt(if (is_less) 1 else 0)
        case PMUX =>
          if (state.select)
            rs2_val
          else
            rs1_val
      }
      state.register_file(rd) = rd_val
    }

    // def checkMemoryReference(base: Name, offset: BigInt)
    def interpret(instruction: Instruction): Unit = instruction match {
      case i: BinaryArithmetic => interpret(i)
      case i: CustomInstruction =>
        logger.error("Custom instruction can not be interpreted yet!", i)
      case LocalLoad(rd, base, offset, _) =>
        val used_mems = memory_blocks(base)
        if (used_mems.isEmpty) {
          logger.error("Could not resolve memory block", instruction)
        } else {

          val with_block_annon = used_mems.collect {
            case mdef if mdef.findAnnotation(Memblock.name).nonEmpty => mdef
          }

        }
        ???
      case LocalStore(rs, base, offset, predicate, annons) => ???
      case GlobalLoad(rd, base, annons)                    => ???
      case GlobalStore(rs, base, predicate, annons)        => ???
      case SetValue(rd, value, annons)                     => ???
      case Send(rd, rs, dest_id, annons)                   => ???
      case Expect(ref, got, error_id, annons) =>
        val ref_val = state.register_file(ref)
        val got_val = state.register_file(got)
        if (ref_val != got_val) {

          // dump the state of registers
          logger.dumpArtifact("interpreter_state.txt") {

            case class RegDump(index: Int, value: BigInt)
                extends Ordered[RegDump] {
              override def compare(that: RegDump): Int =
                Ordering[Int].compare(this.index, that.index)
            }

            val width_map = scala.collection.mutable.Map.empty[String, Int]
            val index_map = scala.collection.mutable.Map
              .empty[String, scala.collection.mutable.PriorityQueue[RegDump]]

            state.register_file.foreach { case (n, v) =>
              val name_def = definitions(n)
              val dbginfo = name_def.findAnnotation(DebugSymbol.name)

              // get the debug symbol, if one exits, otherwise get the info
              // from the DefReg node
              val (dbg_name: String, dbg_index: Int, dbg_w: Int) =
                dbginfo match {
                  case Some(dbgsym: DebugSymbol) =>
                    (dbgsym.getSymbol(), dbgsym.getIndex(), dbgsym.getWidth())
                  case _ => (name_def.variable.name, 0, name_def.variable.width)
                }
              if (index_map contains dbg_name) {
                index_map(dbg_name) += RegDump(dbg_index, v)
                assert(width_map(dbg_name) == dbg_w)
              } else {
                index_map += (dbg_name -> scala.collection.mutable
                  .PriorityQueue[RegDump](RegDump(dbg_index, v)))
                width_map += (dbg_name -> dbg_w)
              }
            }

            index_map.map { case (dbg_name, indexed_values) =>
              val ordered_vals = indexed_values.dequeueAll.toSeq
              if (dbg_name == "rs_2")
                println(ordered_vals)
              val value: BigInt = ordered_vals.foldLeft(BigInt(0)) {
                case (carry, x) =>
                  (carry << 16) | x.value
              }
              s"${dbg_name}[${width_map(dbg_name) - 1} : 0] = ${value}"
            }.toSeq.sorted.mkString("\n")
          }
          logger.error(
            s"Interpreter encountered a Manticore exception, expected ${ref_val} but got ${got_val}",
            instruction
          )
          state.exception_occurred = true
        }
      case Predicate(rs, annons) => ???
      case Mux(rd, sel, rfalse, rtrue, annons) =>
        val sel_val = state.register_file(sel)
        val rd_val = if (sel_val == 1) {
          val rtrue_val = state.register_file(rtrue)
          rtrue_val
        } else if (sel_val == 0) {
          val rfalse_val = state.register_file(rfalse)
          rfalse_val
        } else {
          logger.error(s"Select has illegal value ${sel_val}", instruction)
          BigInt(0)
        }
        state.register_file(rd) = rd_val
      case Nop =>
        // do nothing
        logger.warn("Nops are unnecessary for interpretation", instruction)
      case AddC(rd, co, rs1, rs2, ci, annons) =>
        val rd_width = definitions(rd).variable.width
        val rs1_val = state.register_file(rs1)
        val rs2_val = state.register_file(rs2)
        val ci_val = state.register_file(ci)
        if (ci_val > 1) {
          logger.error(
            "Internal interpreter error, carry computation is incorrect",
            instruction
          )
        }
        val rs1_width = definitions(rs1).variable.width
        val rs2_width = definitions(rs2).variable.width
        if (rs1_width != rs2_width || rs1_width != rd_width) {
          logger.error(
            "Interpreter can only compute ADDCARRY of numbers with equal bit width",
            instruction
          )
        }
        val rd_carry_val = rs1_val + rs2_val + ci_val
        val rd_val = clipped(rd_carry_val)(ClipWidth(rd_width))
        val co_val = BigInt(if (rd_carry_val.testBit(rd_width)) 1 else 0)
        state.register_file(rd) = rd_val
        state.register_file(co) = co_val

      case PadZero(rd, rs, width, annons) => ???
    }

    def run(): Unit = proc.body.foreach { interpret }
    def exceptionOccurred(): Boolean = state.exception_occurred

  }
  override def check(
      source: UnconstrainedIR.DefProgram,
      context: AssemblyContext
  ): Unit = {

    if (source.processes.length != 1) {
      logger.error("Can not handle more than one process for now")
    } else {
      val interp = new ProcessInterpreter(source.processes.head)(context)
      // var cycles = 0
      interp.run()
    }

  }

}
