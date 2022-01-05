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
import manticore.assembly.annotations.Trap
import manticore.assembly.annotations.AssemblyAnnotation
import manticore.assembly.annotations.Echo

object UnconstrainedInterpreter
    extends AssemblyChecker[UnconstrainedIR.DefProgram] {
  val flavor = UnconstrainedIR
  import flavor._
  // wrapper class for memory state
  case class BlockRam(
      content: Array[Constant],
      width: Int,
      capacity: Int,
      block: String
  )

  abstract class InterpretationTrap

  case object InterpretationFailure extends InterpretationTrap
  case object InterpretationStop extends InterpretationTrap

  private final class ProcessState(val proc: DefProcess) {

    // a mutable register file
    val register_file = scala.collection.mutable.Map[Name, BigInt]() ++
      proc.registers.map { r =>
        val k = r.variable.name
        val v = r.value match {
          case Some(x) =>
            if (r.variable.varType == MemoryType) {
              logger.warn(s"ignoring memory base register initial value", r)
              // note that we set the initial value of all .mem definitions to 0
              // because we resolve the memory address partly by name, partly
              // by a runtime offset. See how memories are initialized bellow
              BigInt(0)
            } else {
              x
            }
          case None =>
            if (r.variable.varType == ConstType) {
              logger.error(s"constant register without initial value!", r)
            }
            BigInt(0)
        }
        k -> v
      }

    // a container to map block names to their allocated interpreter
    // memory blocks (one to one)
    val memory_blocks =
      scala.collection.mutable.Map.empty[String, BlockRam]

    // a container to map .mem names to their allocated block ram (possibly many to one)
    val memory: Map[Name, BlockRam] = {

      // go through the list of registers, filter out the memory ones, and
      // see if the memory block is already initialized in the or not. If not
      // initialized, read the init files or zero out the memory and establish
      // a mapping from register name to the BlockRam case class

      proc.registers
        .collect { case m @ DefReg(LogicVariable(_, _, MemoryType), _, _) =>
          m
        }
        .map { m =>
          val block_name: String = m.findAnnotation(Memblock.name) match {
            case Some(block_annon) =>
              block_annon.getStringValue(
                AssemblyAnnotationFields.Block
              ) match {
                case Some(n: String) => n
                case _ =>
                  logger.error(s"missing block field in MEMBLOCK", m)
                  ""
              }
            case _ =>
              logger.error(s"missing MEMBLOCK annotation", m)
              ""
          }
          if (memory_blocks contains block_name) {
            m.variable.name -> memory_blocks(block_name)
          } else {
            val memblock_cap = m.findAnnotationValue(
              Memblock.name,
              AssemblyAnnotationFields.Capacity
            )
            val cap = memblock_cap match {
              case Some(IntValue(v)) =>
                if (v == 0) {
                  logger.error(
                    s"memory capacity 0! Please make sure all memories are less than 4 GiBs."
                  )
                }
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
            val new_block = BlockRam(meminit, width, cap, block_name)
            memory_blocks += (block_name -> new_block)
            m.variable.name -> new_block
          }
        }
        .toMap
    }
    var predicate: Boolean = false
    var carry: Boolean = false
    var select: Boolean = false
    var exception_occurred: Option[InterpretationTrap] = None

  }
  private final class ProcessInterpreter(val proc: DefProcess)(implicit
      val ctx: AssemblyContext
  ) {

    private def handleMemoryAccess(base: Name, instruction: Instruction)(
        handler: BlockRam => Unit
    ): Unit = {

      val resolved_block = instruction.findAnnotationValue(
        Memblock.name,
        AssemblyAnnotationFields.Block
      ) match {
        case Some(StringValue(block_name))
            if state.memory_blocks.contains(block_name) =>
          handler(state.memory_blocks(block_name))
        case _ =>
          logger.error("Could not resolve memory access block", instruction)
      }
    }
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
          // since all values are positive, we should manually handle
          // propagation of the sign bit

          val rs1_val = state.register_file(rs1)
          val rs1_width = definitions(rs1).variable.width
          assert(
            rs2_val >= 0,
            "all values are supposed to be stored as non-negative BigInts!"
          )
          val sign_value = rs1_val >> (rs1_width - 1)


          if (rs2_val.isValidInt && rs2_val < 0xffff) {
            val full_mask = (BigInt(1) << rs1_width) - 1
            val nonsigned_width = if (rs2_val > rs1_width) 0 else rs1_width - rs2_val.toInt
            val nonsign_mask = (1 << nonsigned_width) - 1
            val sign_mask = full_mask - nonsign_mask
            val shifted = rs1_val >> rs2_val.toInt
            val shifted_signed = sign_mask | shifted
            clipped(shifted_signed)
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

    private def nextPower2BitMask(x: Int): Int = {
      var v = x
      v -= 1
      v |= v >> 1
      v |= v >> 2
      v |= v >> 4
      v |= v >> 8
      v |= v >> 16
      v
    }
    // def checkMemoryReference(base: Name, offset: BigInt)
    def interpret(instruction: Instruction): Unit = instruction match {
      case i: BinaryArithmetic => interpret(i)
      case i: CustomInstruction =>
        logger.error("Custom instruction can not be interpreted yet!", i)
      case LocalLoad(rd, base, offset, _) =>
        // this is wrong, need to somehow infer the address mode (i.e., short-word or arbitrary-word)
        handleMemoryAccess(base, instruction) { resolved_block =>
          val addr_val = state.register_file(base) + offset
          val block_cap = resolved_block.capacity
          if (block_cap == 0) {
            logger.error(
              "Can not handle LD from memory with capacity 0!",
              instruction
            )
          } else {
            val block_index: BigInt =
              addr_val & nextPower2BitMask(block_cap.toInt)
            val rd_val =
              if (block_index.toInt >= resolved_block.content.length) {
                logger.error(
                  s"Index out of bound! ${block_index.toInt} >= ${resolved_block.content.length}",
                  instruction
                )
                BigInt(0)
              } else {
                resolved_block.content(block_index.toInt)
              }
            // if there is a debug symbol index, we need to shift the word to
            // the right and only extract the relevant bits
            val rd_def = definitions(rd)
            val rd_val_actual = rd_def.findAnnotationValue(
              DebugSymbol.name,
              AssemblyAnnotationFields.Index
            ) match {
              case Some(IntValue(index)) =>
                // the original word was large word and is now broken into
                // 16-bit words, the index indicates the sub-word position
                // for instance, index 1 means the part we are actually concerned
                // with is the bits [16 : 31]
                val rd_val_shifted = rd_val >> (16 * index)
                val rd_val_masked = rd_val_shifted & 0xffff // keep only 16 bits
                rd_val_masked
              case _ =>
                rd_val
            }
            state.register_file(rd) = rd_val_actual
          }
        }
      case LocalStore(rs, base, offset, predicate, annons) =>
        handleMemoryAccess(base, instruction) { resolved_block =>
          val addr_val = state.register_file(base) + offset
          val block_cap: Int = resolved_block.capacity
          if (block_cap == 0) {
            logger.error(
              "Can not ST from  memory with capacity 0!",
              instruction
            )
          } else {
            val block_index: BigInt =
              addr_val & nextPower2BitMask(block_cap.toInt)
            val rs_val = state.register_file(rs)
            val pred_val = predicate
              .map { x => state.register_file(x) == 1 }
              .getOrElse(state.predicate)

            if (pred_val) {
              assert(block_index.isValidInt, "Something went wrong handling ST")
              val rs_def = definitions(rs)
              val rs_val_actual = rs_def.findAnnotationValue(
                DebugSymbol.name,
                AssemblyAnnotationFields.Index
              ) match {
                case Some(IntValue(index)) =>
                  // since the debug symbol is augmented with index, then
                  // the original memory has gone through width conversion
                  // and we need to only store a sub-word (it could be a full
                  // word iff the original memory width was 16 bits)
                  // therefore we first need to read from the memory and then
                  // write a modified wide word to it.
                  val old_val = resolved_block.content(block_index.toInt)
                  val rs_val_shifted = rs_val << (16 * index)
                  val mask: BigInt = ((BigInt(
                    1
                  ) << resolved_block.width) - 1) - (BigInt(
                    0xffff
                  ) << (16 * index))
                  val old_val_masked = old_val & mask
                  val new_val = old_val_masked | rs_val_shifted
                  new_val
                case _ =>
                  // no valid index, so no need to handle sub-word stores
                  rs_val

              }
              resolved_block.content(block_index.toInt) = rs_val_actual
            }

          }
        }

      case GlobalLoad(rd, base, annons) =>
        logger.error("Can handle global memory access", instruction)
      case GlobalStore(rs, base, predicate, annons) =>
        logger.error("Can handle global memory access", instruction)
      case SetValue(rd, value, annons) =>
        logger.error("Can handle SET", instruction)
      case Send(rd, rs, dest_id, annons) =>
        logger.error("Can not handle SEND", instruction)
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

            index_map
              .map { case (dbg_name, indexed_values) =>
                val ordered_vals = indexed_values.dequeueAll.toSeq
                if (dbg_name == "rs_2")
                  println(ordered_vals)
                val value: BigInt = ordered_vals.foldLeft(BigInt(0)) {
                  case (carry, x) =>
                    (carry << 16) | x.value
                }
                s"${dbg_name}[${width_map(dbg_name) - 1} : 0] = ${value}"
              }
              .toSeq
              .sorted
              .mkString("\n")
          }

          val trap_source = instruction
            .findAnnotationValue(
              Trap.name,
              AssemblyAnnotationFields.File
            ) match {
            case Some(StringValue(x)) => x
            case _                    => ""
          }
          state.exception_occurred = instruction.findAnnotationValue(
            Trap.name,
            AssemblyAnnotationFields.Type
          ) match {
            case Some(Trap.Fail) =>
              logger.error(s"User exception caught! ${error_id}", instruction)
              logger.error(s"Expected ${ref_val} but got ${got_val}")
              Some(InterpretationFailure)
            case Some(Trap.Stop) =>
              logger.info("Stop signal interpreted.")
              if (trap_source.nonEmpty)
                logger.error(s"Stop condition from ${trap_source}", instruction)
              Some(InterpretationStop)
            case _ =>
              logger.error(s"Missing TRAP type!", instruction)
              Some(InterpretationFailure)
          }
        } else {

          if (
            annons.exists {
              case a: Echo => true
              case _       => false
            }
          ) {
            logger.info(s"values ${ref_val} and ${got_val} match.", instruction)
          }

        }

      case Predicate(rs, annons) => ???
      case Mux(rd, sel, rfalse, rtrue, annons) =>
        val sel_val = state.register_file(sel)
        val rtrue_val = state.register_file(rtrue)
        val rfalse_val = state.register_file(rfalse)
        val rd_val = if (sel_val == 1) {
          rtrue_val
        } else if (sel_val == 0) {
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
    def getException(): Option[InterpretationTrap] = state.exception_occurred

  }
  override def check(
      source: UnconstrainedIR.DefProgram,
      context: AssemblyContext
  ): Unit = {

    if (source.processes.length != 1) {
      logger.error("Can not handle more than one process for now")
    } else {
      var cycles = 0
      val interp = new ProcessInterpreter(source.processes.head)(context)

      while (cycles < context.max_cycles && interp.getException().isEmpty) {
        logger.info(s"Starting cycle ${cycles}")
        if (cycles == 16)
          logger.info("TRAP")
        interp.run()
        cycles += 1
      }
      logger.info(s"Finished interpretation after ${cycles} cycles")

    }

  }

}
