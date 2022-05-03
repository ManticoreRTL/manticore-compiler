package manticore.compiler.assembly.levels.unconstrained

import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.levels.MemoryType
import manticore.compiler.assembly.annotations.Memblock
import manticore.compiler.assembly.annotations.AssemblyAnnotationFields
import manticore.compiler.assembly.annotations.IntValue
import manticore.compiler.assembly.annotations.MemInit
import manticore.compiler.assembly.annotations.StringValue
import manticore.compiler.assembly.levels.ConstType
import manticore.compiler.assembly.DependenceGraphBuilder
import manticore.compiler.assembly.levels.AssemblyChecker
import manticore.compiler.assembly.annotations.DebugSymbol
import manticore.compiler.assembly.annotations.Trap
import manticore.compiler.assembly.annotations.AssemblyAnnotation
import manticore.compiler.assembly.annotations.Echo
import java.io.PrintWriter
import java.io.File
import scala.collection.immutable.NumericRange
import manticore.compiler.assembly.levels.ValueChangeWriterBase
import scalax.collection.edge.LDiEdge
import scalax.collection.Graph
import manticore.compiler.assembly.levels.InterpreterMonitor

/** Simple interpreter for unconstrained flavored programs with a single process
  * @author
  *   Mahyar Emami <mahyar.emami@eplf.ch>
  */
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

  sealed trait InterpretationTrap

  case object InterpretationFailure extends InterpretationTrap
  case object InterpretationStop extends InterpretationTrap

  private final class ProcessState(val proc: DefProcess)(implicit
      val ctx: AssemblyContext
  ) {

    // a mutable register file
    val register_file = scala.collection.mutable.Map[Name, BigInt]() ++
      proc.registers.map { r =>
        val k = r.variable.name
        val v = r.value match {
          case Some(x) =>
            if (r.variable.varType == MemoryType) {
              ctx.logger.warn(s"ignoring memory base register initial value", r)
              // note that we set the initial value of all .mem definitions to 0
              // because we resolve the memory address partly by name, partly
              // by a runtime offset. See how memories are initialized bellow
              BigInt(0)
            } else {
              x
            }
          case None =>
            if (r.variable.varType == ConstType) {
              ctx.logger.error(s"constant register without initial value!", r)
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
      def isLabelMem(mem: Name): Boolean = proc.labels.exists {
        case DefLabelGroup(rs, _, _, _) => rs == mem
      }
      proc.registers
        .collect {
          case m @ DefReg(LogicVariable(nm, _, MemoryType), _, _)
              if !isLabelMem(nm) =>
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
                  ctx.logger.error(s"missing block field in MEMBLOCK", m)
                  ""
              }
            case _ =>
              ctx.logger.error(s"missing MEMBLOCK annotation", m)
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
                  ctx.logger.error(
                    s"memory capacity 0! Please make sure all memories are less than 4 GiBs."
                  )
                }
                v
              case _ =>
                ctx.logger.error(s"missing memory block capacity!", m)
                0
            }
            val memblock_width = m.findAnnotationValue(
              Memblock.name,
              AssemblyAnnotationFields.Width
            )
            val width = memblock_width match {
              case Some(IntValue(v)) => v
              case _ =>
                ctx.logger.error(s"missing memory block width!", m)
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
                  ctx.logger.error("init file overflows memory!", m)
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
                    ctx.logger.error(
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
    var program_index: Int = 0
    val label_bindings = scala.collection.mutable.Map.empty[Name, Label]

  }
  final class ProcessInterpreter private[UnconstrainedInterpreter] (
      val proc: DefProcess,
      val vcd_writer: Option[UnconstrainedValueChangeWriter],
      val monitor: Option[UnconstrainedIRInterpreterMonitor]
  )(implicit
      val ctx: AssemblyContext
  ) extends  InterpreterMonitor.CanUpdateMonitor[ProcessInterpreter]{

    private def handleMemoryAccess(base: Name, instruction: Instruction)(
        handler: (BlockRam, Option[Int]) => Unit
    ): Unit = {

      // get the Memblock annotation
      val memblock = instruction.annons.collectFirst { case x: Memblock =>
        x
      }
      memblock match {
        case Some(annon: Memblock) =>
          val block_name =
            annon.getStringValue(AssemblyAnnotationFields.Block).get
          val index: Option[Int] = annon.getIntValue(
            AssemblyAnnotationFields.Index
          ) // sub-word index, only present if width conversion if performed
          val block_object = state.memory_blocks(block_name)
          handler(block_object, index)
        case _ =>
          ctx.logger.error(
            "Could not resolve memory access block, ensure @MEMBLOCK is present",
            instruction
          )
      }
    }
    // A table for looking up name definitions
    val definitions: Map[Name, DefReg] = proc.registers.map { r =>
      r.variable.name -> r
    }.toMap

    private val state = new ProcessState(proc)

    sealed trait ExecutionEdgeLabel
    case class Conditional(l: Label) extends ExecutionEdgeLabel
    case object Unconditional extends ExecutionEdgeLabel
    class ExecutionVertex {

      val body = scala.collection.mutable.ArrayBuffer.empty[Instruction]
      val edges =
        scala.collection.mutable.Map.empty[ExecutionEdgeLabel, ExecutionVertex]
      var target: Option[Name] = None
      def add(inst: Instruction) = {
        body += inst
        this
      }
      def addAll(insts: IterableOnce[Instruction]) = {
        body ++= insts
        this
      }

      def addEdge(l: ExecutionEdgeLabel, v: ExecutionVertex) = {
        edges += (l -> v)
        this
      }

      def setTarget(rs: Name) = {
        assert(target.isEmpty)
        target = Some(rs)
        this
      }

    }

    private val exec_graph_root: ExecutionVertex = {
      val root = new ExecutionVertex

      val last = proc.body.foldLeft(
        root
      ) {
        case (current, jmp @ JumpTable(rs, phis, blocks, dslot, _)) =>
          if (dslot.nonEmpty) {
            current.addAll(dslot)
          }
          val next = new ExecutionVertex

          blocks.foreach { case JumpCase(lbl, insts) =>
            val vtx = new ExecutionVertex
            vtx.addAll(insts)
            vtx.addEdge(Unconditional, next)
            current.addEdge(Conditional(lbl), vtx)
            // we also need to add a Mov instruction to basically implement the
            // Phi operation at the end of each branch, we do this by essentially
            // breaking SSAness of the instructions by appending Mov instructions
            // to the same rd at different branches.
            phis
              .map { case Phi(rd, rss) =>
                rd -> rss.collectFirst { case (l, rs) if l == lbl => rs }
              }
              .foreach {
                case (rd, Some(rs)) =>
                  vtx.add(Mov(rd, rs))
                case (rd, None) =>
                  ctx.logger.error(
                    s"Could not absorb phi inside the case body because case ${lbl} is not use in none of the results",
                    jmp
                  )
              }
          }
          current.setTarget(rs)
          next
        case (current, inst) => // other types of instructions
          current.add(inst)
      }
      root
    }

    var current_node = exec_graph_root

    private case class ClipWidth(w: Int)
    private def clipped(v: BigInt)(implicit w: ClipWidth): BigInt = {
      // require(w < 32, "unsupported width!")
      v & ((BigInt(1) << w.w) - 1)
    }
    private def getSignedValue(r: Name): BigInt = {
      val unsigned_val = state.register_file(r)
      if (unsigned_val < 0) {
        ctx.logger.error(
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
      import manticore.compiler.assembly.BinaryOperator._
      val BinaryArithmetic(op, rd, rs1, rs2, _) = inst
      val rd_width = definitions(rd).variable.width
      implicit val clip_width = ClipWidth(rd_width)
      val rs1_val = state.register_file(rs1)
      val rs2_val = state.register_file(rs2)
      val rd_val = op match {
        case ADD => clipped(rs1_val + rs2_val)
        case SUB => clipped(rs1_val - rs2_val)
        case OR  => clipped(rs1_val | rs2_val)
        case AND => clipped(rs1_val & rs2_val)
        case XOR => clipped((rs1_val &~ rs2_val) | (rs2_val &~ rs1_val))
        case SEQ =>
          if (
            definitions(rs1).variable.width != definitions(rs2).variable.width
          ) {
            ctx.logger.error("Width mismatch in SEQ", inst)
          }
          val is_eq = rs1_val == rs2_val
          state.select = is_eq
          BigInt(if (is_eq) 1 else 0)
        case SLL =>
          if (rs2_val.isValidInt && rs2_val <= 0xffff) {
            clipped(rs1_val << rs2_val.toInt)
          } else {
            ctx.logger.error("unsupported SLL shift amount", inst)
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
            ctx.logger.error("unsupported SRL shift amount", inst)
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
            if (sign_value == 1) {
              val full_mask = (BigInt(1) << rs1_width) - 1
              val nonsigned_width =
                if (rs2_val > rs1_width) 0 else rs1_width - rs2_val.toInt
              val nonsign_mask = (BigInt(1) << nonsigned_width) - 1
              val sign_mask = full_mask - nonsign_mask
              val shifted = rs1_val >> rs2_val.toInt
              val shifted_signed = sign_mask | shifted
              clipped(shifted_signed)
            } else {
              clipped(rs1_val >> rs2_val.toInt)
            }
          } else {
            ctx.logger.error("unsupported SRA shift amount", inst)
            BigInt(0)
          }
        case SLTS =>
          // again, we should encode the signs manually since all
          // interpreted values are stored as positive numbers
          val rs1_signed_val = getSignedValue(rs1)
          val rs2_signed_val = getSignedValue(rs2)
          val is_less = rs1_signed_val < rs2_signed_val
          state.select = is_less
          BigInt(if (is_less) 1 else 0)
      }

      update(rd, rd_val)
    }

    private def update(rd: Name, v: BigInt): Unit = {
      vcd_writer.foreach { _.update(rd, v) }
      monitor.foreach { _.update(rd, v) }
      state.register_file(rd) = v
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
        ctx.logger.error("Custom instruction can not be interpreted yet!", i)
      case LocalLoad(rd, base, offset, _) =>
        // this is wrong, need to somehow infer the address mode (i.e., short-word or arbitrary-word)
        handleMemoryAccess(base, instruction) {
          (resolved_block: BlockRam, subword_index: Option[Int]) =>
            val addr_val = state.register_file(base) + offset
            val block_cap = resolved_block.capacity
            if (block_cap == 0) {
              ctx.logger.error(
                "Can not handle LD from memory with capacity 0!",
                instruction
              )
            } else {
              val block_index: BigInt =
                addr_val & nextPower2BitMask(block_cap.toInt)
              val rd_val =
                if (block_index.toInt >= resolved_block.content.length) {
                  ctx.logger.error(
                    s"Index out of bound! ${block_index.toInt} >= ${resolved_block.content.length}",
                    instruction
                  )
                  BigInt(0)
                } else {
                  resolved_block.content(block_index.toInt)
                }
              // if there is a debug symbol index, we need to shift the word to
              // the right and only extract the relevant bits

              val rd_val_actual = subword_index match {
                case Some(index) =>
                  // the original word was large word and is now broken into
                  // 16-bit words, the index indicates the sub-word position
                  // for instance, index 1 means the part we are actually concerned
                  // with is the bits [16 : 31]
                  val rd_val_shifted = rd_val >> (16 * index)
                  val rd_val_masked =
                    rd_val_shifted & 0xffff // keep only 16 bits
                  rd_val_masked
                case _ =>
                  rd_val
              }
              update(rd, rd_val_actual)

            }
        }
      case LocalStore(rs, base, offset, predicate, annons) =>
        handleMemoryAccess(base, instruction) {
          (resolved_block: BlockRam, subword_index: Option[Int]) =>
            val addr_val = state.register_file(base) + offset
            val block_cap: Int = resolved_block.capacity
            if (block_cap == 0) {
              ctx.logger.error(
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
                assert(
                  block_index.isValidInt,
                  "Something went wrong handling ST"
                )

                val rs_val_actual = subword_index match {
                  case Some(index) =>
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
        ctx.logger.error("Can handle global memory access", instruction)
        state.exception_occurred = Some(InterpretationFailure)
      case GlobalStore(rs, base, predicate, annons) =>
        ctx.logger.error("Can handle global memory access", instruction)
        state.exception_occurred = Some(InterpretationFailure)
      case SetValue(rd, value, annons) =>
        ctx.logger.error("Can handle SET", instruction)
        state.exception_occurred = Some(InterpretationFailure)
      case Send(rd, rs, dest_id, annons) =>
        ctx.logger.error("Can not handle SEND", instruction)
        state.exception_occurred = Some(InterpretationFailure)
      case Expect(ref, got, error_id, annons) =>
        val ref_val = state.register_file(ref)
        val got_val = state.register_file(got)
        if (ref_val != got_val) {

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
              ctx.logger.error(
                s"User exception caught! ${error_id}",
                instruction
              )
              ctx.logger.error(s"Expected ${ref_val} but got ${got_val}")
              Some(InterpretationFailure)
            case Some(Trap.Stop) =>
              ctx.logger.info("Stop signal interpreted.")
              if (trap_source.nonEmpty)
                ctx.logger.info(
                  s"Stop condition from ${trap_source}",
                  instruction
                )
              Some(InterpretationStop)
            case _ =>
              ctx.logger.error(s"Missing TRAP type!", instruction)
              Some(InterpretationFailure)
          }
        } else {

          if (
            annons.exists {
              case a: Echo => true
              case _       => false
            }
          ) {
            ctx.logger.info(
              s"values ${ref_val} and ${got_val} match.",
              instruction
            )
          }

        }

      case Predicate(rs, annons) =>
        ctx.logger.error(s"Can not handle predicate yet!", instruction)

      case Mux(rd, sel, rfalse, rtrue, annons) =>
        val sel_val = state.register_file(sel)
        val rtrue_val = state.register_file(rtrue)
        val rfalse_val = state.register_file(rfalse)
        val rd_val = if (sel_val == 1) {
          rtrue_val
        } else if (sel_val == 0) {
          rfalse_val
        } else {
          ctx.logger.error(s"Select has illegal value ${sel_val}", instruction)
          BigInt(0)
        }
        update(rd, rd_val)

      case Slice(rd, rs, offset, length, _) =>
        val rs_val = state.register_file(rs)
        val rs_shifted = rs_val >> offset
        val mask = (1 << length) - 1
        val rd_val = rs_shifted & mask
        update(rd, rd_val)

      case Nop =>
        // do nothing
        ctx.logger.warn("Nops are unnecessary for interpretation", instruction)
      case AddC(rd, co, rs1, rs2, ci, annons) =>
        val rd_width = definitions(rd).variable.width
        val rs1_val = state.register_file(rs1)
        val rs2_val = state.register_file(rs2)
        val ci_val = state.register_file(ci)
        if (ci_val > 1) {
          ctx.logger.error(
            "Internal interpreter error, carry computation is incorrect",
            instruction
          )
        }
        val rs1_width = definitions(rs1).variable.width
        val rs2_width = definitions(rs2).variable.width
        if (rs1_width != rs2_width || rs1_width != rd_width) {
          ctx.logger.error(
            "Interpreter can only compute ADDCARRY of numbers with equal bit width",
            instruction
          )
        }
        val rd_carry_val = rs1_val + rs2_val + ci_val
        val rd_val = clipped(rd_carry_val)(ClipWidth(rd_width))
        val co_val = BigInt(if (rd_carry_val.testBit(rd_width)) 1 else 0)
        update(rd, rd_val)
        update(co, co_val)

      case PadZero(rd, rs, width, annons) =>
        val rs_val = state.register_file(rs)
        update(rd, rs_val)

      case Mov(rd, rs, _) =>
        val rs_val = state.register_file(rs)
        update(rd, rs_val)

      case SetCarry(rd, _) =>
        val rd_val = BigInt(1)
        update(rd, rd_val)
      case ClearCarry(rd, _) =>
        val rd_val = BigInt(0)
        update(rd, rd_val)
      case ParMux(rd, choices, default, _) =>
        // ensure that only a single condition is true
        val valid_choice = choices.collect {
          case ParMuxCase(cond, rs) if state.register_file(cond) == BigInt(1) =>
            cond -> state.register_file(rs)
        }
        if (valid_choice.length > 1) {
          ctx.logger.error(
            s"Concurrent valid conditions ${valid_choice.map(_._1).mkString(" and ")}!",
            instruction
          )
        } else if (valid_choice.length == 1) {
          val rd_val = valid_choice.head._2
          update(rd, rd_val)
        } else {
          // choose default
          val rd_val = state.register_file(default)
          update(rd, rd_val)
        }
      case JumpTable(target, results, blocks, dslot, _) =>
        ctx.logger.fail(
          s"Internal error! Can not interpret hyper instruction ${instruction} as is!"
        )
      case Lookup(rd, index, base, annons) =>
        val lgrp = proc.labels.collectFirst {
          case g: DefLabelGroup if g.memory == base =>
            g
        }
        lgrp match {
          case None =>
            ctx.logger.error(
              s"Could not interpret! No label group associated with ${base}"
            )
          case Some(DefLabelGroup(_, indexer, default, _)) =>
            val index_val = state.register_file(index)
            indexer.find(_._1 == index_val) match {
              case Some(_ -> lbl) =>
                state.label_bindings += (rd -> lbl)
              case None =>
                default match {
                  case None =>
                    ctx.logger.error(
                      "Could not lookup jump target and the default case is empty!",
                      instruction
                    )
                    ctx.logger.fail("Can not continue interpretation!")
                  case Some(lbl) =>
                    state.label_bindings += (rd -> lbl)
                }
            }
        }
      case _: Recv =>
        ctx.logger.error("Illegal instruction", instruction)
        state.exception_occurred = Some(InterpretationFailure)
    }

    sealed trait StepStatus
    case object StepVCycleFinish extends StepStatus
    case object StepVCycleContinue extends StepStatus

    def step(): StepStatus = {
      val inst = current_node.body(state.program_index)
      interpret(inst)
      if (state.program_index == current_node.body.length - 1) {
        state.program_index = 0
        val num_edges = current_node.edges.size
        num_edges match {
          case 0 => // finished a virtual cycle!
            current_node = exec_graph_root
            StepVCycleFinish
          case 1 => // should be unconditional
            current_node = current_node.edges(Unconditional)
            StepVCycleContinue
          case n: Int =>
            assert(current_node.target.nonEmpty, "Execution graph is incorrect")
            assert(
              state.label_bindings.contains(current_node.target.get),
              s"Lookup logic is invalid! Could not find the binding for ${current_node.target.get}"
            )
            val lbl = state.label_bindings(current_node.target.get)
            current_node = current_node.edges(Conditional(lbl))
            StepVCycleContinue
        }
      } else {
        // simply increment the program index (counter)
        state.program_index += 1
        StepVCycleContinue
      }
    }

    def runVirtualCycle(): Unit = {
      step() match {
        case StepVCycleContinue => if (getException().isEmpty) runVirtualCycle()
        case StepVCycleFinish   => // clear out the label bindings (not really
          // needed but may help catch some errors!)
          state.label_bindings.clear()
      }
    }
    def runCompletion(): Unit = {
      var vcycle = 0
      while (vcycle < ctx.max_cycles && getException().isEmpty) {
        runVirtualCycle()
        vcycle += 1
        vcd_writer.foreach { _.tick() }
      }

      val cycles_match = ctx.expected_cycles match {
        case None    => true
        case Some(v) => v == vcycle - 1
      }

      if (!cycles_match) {
        ctx.logger.error(
          s"Interpretation finished after ${vcycle - 1} virtual cycles, " +
            s"but expected it to finish in ${ctx.expected_cycles.get} virtual cycles!"
        )
      } else if (vcycle >= ctx.max_cycles) {
        ctx.logger.error(
          s"Interpretation timed out after ${vcycle - 1} virtual cycles"
        )
      } else {
        ctx.logger.info(
          s"Interpretation finished after ${vcycle - 1} virtual cycles"
        )
      }
    }
    def getException(): Option[InterpretationTrap] = state.exception_occurred

  }

  final class UnconstrainedValueChangeWriter(
      val program: DefProgram,
      val file_name: String
  )(implicit val ctx: AssemblyContext)
      extends ValueChangeWriterBase {
    val flavor = UnconstrainedIR

    def toBigInt(v: BigInt): BigInt = v

  }

  /**
    * Create an instance of the interpreter to use outside the check
    *
    * @param program
    * @param vcdDump
    * @param monitor
    * @param ctx
    * @return
    */
  def instance(
      program: DefProgram,
      vcdDump: Option[UnconstrainedValueChangeWriter],
      monitor: Option[UnconstrainedIRInterpreterMonitor]
  )(implicit ctx: AssemblyContext): ProcessInterpreter = {
    require(
      program.processes.length == 1,
      "Could only interpret a single process"
    )
    new ProcessInterpreter(program.processes.head, vcdDump, monitor)(ctx)
  }

  /**
    *
    *
    * @param source
    * @param context
    */
  override def check(
      source: UnconstrainedIR.DefProgram,
      context: AssemblyContext
  ): Unit = {

    if (source.processes.length != 1) {
      context.logger.error("Can not handle more than one process for now")
    } else {

      val vcd_writer =
        if (context.dump_all)
          Some(new UnconstrainedValueChangeWriter(source, "trace.vcd")(context))
        else None
      val interp =
        instance(source, vcd_writer, None)(context)

      interp.runCompletion()

      if (vcd_writer.nonEmpty) {
        vcd_writer.get.flush()
        vcd_writer.get.close()
      }

    }

  }

}
