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
import manticore.compiler.FormatString
import manticore.compiler.FormatString.FmtBin
import manticore.compiler.FormatString.FmtConcat
import manticore.compiler.FormatString.FmtDec
import manticore.compiler.FormatString.FmtHex

/** Simple interpreter for unconstrained flavored programs with a single process
  * @author
  *   Mahyar Emami <mahyar.emami@eplf.ch>
  */
object UnconstrainedInterpreter extends UnconstrainedIRChecker {
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
  case object InterpretationFinish  extends InterpretationTrap

  private final class ProcessState(val proc: DefProcess)(implicit
      val ctx: AssemblyContext
  ) {

    val serial_queue = scala.collection.mutable.Queue.empty[BigInt]
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
    val ovf_register_file = scala.collection.mutable.Map.empty[Name, Boolean] ++
      proc.registers.map { _.variable.name -> false }

    // a container to map block names to their allocated interpreter
    // memory blocks (one to one)
    private def isLabelMem(mem: Name): Boolean = proc.labels.exists { case DefLabelGroup(rs, _, _, _) =>
      rs == mem
    }

    private def loadContentFromFile(
        memInit: MemInit,
        capacity: Int
    ): Array[BigInt] = {

      val fileContent = memInit.readFile()
      fileContent.toArray ++ Array.fill(capacity - fileContent.length)(
        BigInt(0)
      )

    }
    val memory_blocks = proc.registers.collect {
      case DefReg(MemoryVariable(name, width, size, initialValues), _, annons) if !isLabelMem(name) =>
        val content = if (initialValues.nonEmpty) {
          initialValues.toArray
        } else {
          annons
            .collectFirst { case x: MemInit => loadContentFromFile(x, size) }
            .getOrElse(Array.fill(size) { BigInt(0) })

        }
        name -> BlockRam(content, width, size, name)
    }.toMap

    val globalMemory = {
      val requiredSize =
        proc.globalMemories.map { gmem => gmem.base + gmem.size }.maxOption.getOrElse(0L)
      if (!requiredSize.isValidInt) {
        ctx.logger.fail(
          "GlobalMemory is too big for interpretation. Make sure JVM has enough heap space!"
        )

      }
      val impl = Array.ofDim[BigInt](requiredSize.toInt)
      for (gmem <- proc.globalMemories) {
        var index = gmem.base
        for (value <- gmem.content) {
          impl(index.toInt) = value
          index += 1
        }
      }
      impl
    }
    // a container to map .mem names to their allocated block ram (possibly many to one)
    var predicate: Boolean                             = false
    var carry: Boolean                                 = false
    var select: Boolean                                = false
    var exception_occurred: Option[InterpretationTrap] = None
    var program_index: Int                             = 0
    val label_bindings                                 = scala.collection.mutable.Map.empty[Name, Label]

  }
  final class ProcessInterpreter private[UnconstrainedInterpreter] (
      val proc: DefProcess,
      val vcd_writer: Option[UnconstrainedValueChangeWriter],
      val monitor: Option[UnconstrainedIRInterpreterMonitor],
      val serial: Option[String => Unit]
  )(implicit
      val ctx: AssemblyContext
  ) extends InterpreterMonitor.CanUpdateMonitor[ProcessInterpreter] {

    // A table for looking up name definitions
    val definitions: Map[Name, DefReg] = proc.registers.map { r =>
      r.variable.name -> r
    }.toMap

    private val state = new ProcessState(proc)

    sealed trait ExecutionEdgeLabel
    case class Conditional(l: Label) extends ExecutionEdgeLabel
    case object Unconditional        extends ExecutionEdgeLabel
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
      val rd_width                              = definitions(rd).variable.width
      implicit val clip_width                   = ClipWidth(rd_width)
      val rs1_val                               = state.register_file(rs1)
      val rs2_val                               = state.register_file(rs2)
      val rd_val = op match {
        case ADD => clipped(rs1_val + rs2_val)
        case SUB => clipped(rs1_val - rs2_val)
        case OR  => clipped(rs1_val | rs2_val)
        case AND => clipped(rs1_val & rs2_val)
        case XOR => clipped((rs1_val &~ rs2_val) | (rs2_val &~ rs1_val))
        case SEQ =>
          // if (
          //   (definitions(rs1).variable.width != definitions(rs2).variable.width)
          // ) {
          //   ctx.logger.error("Width mismatch in SEQ", inst)
          // }
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

          val rs1_val   = state.register_file(rs1)
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
              val nonsign_mask   = (BigInt(1) << nonsigned_width) - 1
              val sign_mask      = full_mask - nonsign_mask
              val shifted        = rs1_val >> rs2_val.toInt
              val shifted_signed = sign_mask | shifted
              clipped(shifted_signed)
            } else {
              clipped(rs1_val >> rs2_val.toInt)
            }
          } else {
            ctx.logger.error("unsupported SRA shift amount", inst)
            BigInt(0)
          }
        case SLT =>
          val rs1_val = state.register_file(rs1)
          val rs2_val = state.register_file(rs2)
          assert(rs1_val >= 0 && rs2_val >= 0)
          if (rs1_val < rs2_val) {
            BigInt(1)
          } else {
            BigInt(0)
          }
        case SLTS =>
          // again, we should encode the signs manually since all
          // interpreted values are stored as positive numbers
          val rs1_width = definitions(rs1).variable.width
          val rs2_width = definitions(rs2).variable.width
          assert(
            rs1_width == rs2_width,
            s"both operands of SLTS should have the same width in: \n${inst}"
          )

          val rs1_val  = state.register_file(rs1)
          val rs2_val  = state.register_file(rs2)
          val rs1_sign = rs1_val.testBit(rs1_width - 1)
          val rs2_sign = rs2_val.testBit(rs2_width - 1)

          if (rs1_sign && rs2_sign) {
            // both are "negative" so we should compare their positive value
            // representation
            val rs1_pos_val =
              clipped((~rs1_val) + BigInt(1))(ClipWidth(rs1_width))
            val rs2_pos_val =
              clipped((~rs2_val) + BigInt(1))(ClipWidth(rs2_width))
            if (rs1_pos_val > rs2_pos_val) {
              BigInt(1)
            } else {
              BigInt(0)
            }
          } else if (rs1_sign && !rs2_sign) {
            BigInt(
              1
            ) // rs1 is negative and rs2 is not, therefore it is certainly
            // less than rs2
          } else if (!rs1_sign && rs2_sign) {
            BigInt(
              0
            )      // rs1 is positive and rs2 negative. Therefore rs1 < rs2 is false
          } else { // both are positive
            if (rs1_val < rs2_val) {
              BigInt(1)
            } else {
              BigInt(0)
            }
          }
        case mulOp @ (MUL | MULH) =>
          assert(rs1_val >= 0 && rs2_val >= 0)
          val wideResult = rs1_val * rs2_val
          if (mulOp == MUL) {
            clipped(wideResult)
          } else {
            clipped(wideResult >> rd_width)
          }
        case MULS =>
          val rsWidth = definitions(rs1).variable.width
          assert(rd_width == rsWidth)
          assert(rsWidth == definitions(rs2).variable.width)
          def sext(v: BigInt): BigInt = if (v.testBit(rsWidth - 1)) {
            v | (((BigInt(1) << rsWidth) - 1) << rsWidth)
          } else {
            v
          }
          val rs1Sext = sext(rs1_val)
          val rs2Sext = sext(rs2_val)
          assert(rs1_val >= 0 && rs2_val >= 0)
          assert(rs1Sext >= 0 && rs2Sext >= 0)
          val wideResult = rs1Sext * rs2Sext
          assert(wideResult >= 0)
          clipped(wideResult)

      }

      update(rd, rd_val)
    }

    private def updateOvf(rd: Name, v: Boolean): Unit = {
      state.ovf_register_file(rd) = v
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
      case i: ConfigCfu =>
        ctx.logger.error("ConfigCfu instruction can not be interpreted yet!", i)
      case LocalLoad(rd, base, addr, _, _) =>
        val addrValue = state.register_file(addr);
        val memory    = state.memory_blocks(base)
        if (addrValue >= memory.capacity) {
          ctx.logger.warn("Index out of bound")
          update(rd, BigInt(0))
        } else {
          val loaded = memory.content(addrValue.toInt)
          update(rd, loaded)
        }

      case LocalStore(rs, base, addr, predicate, _, annons) =>
        val addrValue = state.register_file(addr);
        val memory    = state.memory_blocks(base)
        if (addrValue >= memory.capacity) {
          ctx.logger.warn(
            s"Index out of bound, ignoring write to ${base} at address ${addrValue} >= ${memory.capacity}"
          )
        } else {
          val wen = predicate
            .map(state.register_file(_) == 1)
            .getOrElse(state.predicate)
          if (wen) {
            val rsv = state.register_file(rs)
            memory.content(addrValue.toInt) = rsv
          }
        }

      case SetValue(rd, value, annons) =>
        ctx.logger.error("Can handle SET", instruction)
        state.exception_occurred = Some(InterpretationFailure)
      case Send(rd, rs, dest_id, annons) =>
        ctx.logger.error("Can not handle SEND", instruction)
        state.exception_occurred = Some(InterpretationFailure)
      case PutSerial(rs, cond, _, _) =>
        val cond_val = state.register_file(cond)
        if (cond_val == 1) {
          val rs_val = state.register_file(rs)
          state.serial_queue += rs_val
        }
      case intr @ Interrupt(action, condition, _, _) =>
        val cond_val = state.register_file(condition)
        val fires    = cond_val == 1
        action match {
          case AssertionInterrupt if !fires =>
            ctx.logger.error(s"Assertion failed!", intr)
            state.exception_occurred = Some(InterpretationFailure)
          case FinishInterrupt if fires =>
            ctx.logger.info(s"Finished.", intr)
            state.exception_occurred = Some(InterpretationFinish)
          case StopInterrupt if fires =>
            ctx.logger.error(s"Stopped!", intr)
            state.exception_occurred = Some(InterpretationFailure)
          case SerialInterrupt(fmt) if fires =>
            val values   = state.serial_queue.dequeueAll(_ => true)
            val resolved = fmt.consume(values)
            if (resolved.isLit) {
              serial match {
                case None =>
                  ctx.logger.info(s"[SERIAL]\n ${resolved.toString()}")
                case Some(printer) => printer(resolved.toString())
              }
            } else {
              ctx.logger.error(
                s"Could not fill up the serial line: ${resolved.toString()}"
              )
            }
          case _ => // nothing to do
        }

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
              Some(InterpretationFinish)
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
        val sel_val    = state.register_file(sel)
        val rtrue_val  = state.register_file(rtrue)
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
        // The manticore only supports positive numbers.
        assert(rs_val >= 0)
        val rs_shifted = rs_val >> offset
        // Must use a BigInt for the mask otherwise it may
        // not hold in an Int.
        val mask   = (BigInt(1) << length) - 1
        val rd_val = rs_shifted & mask
        update(rd, rd_val)

      case Nop =>
        // do nothing
        ctx.logger.warn("Nops are unnecessary for interpretation", instruction)
      case AddCarry(rd, rs1, rs2, cin, _) =>
        val rd_width = definitions(rd).variable.width
        val rs1_val  = state.register_file(rs1)
        val rs2_val  = state.register_file(rs2)
        val ci_val   = state.ovf_register_file(cin)
        val rs1_width = definitions(rs1).variable.width
        val rs2_width = definitions(rs2).variable.width
        if (rs1_width != rs2_width || rs1_width != rd_width) {
          ctx.logger.error(
            "Interpreter can only compute ADDCARRY of numbers with equal bit width",
            instruction
          )
        }
        val rd_carry_val = rs1_val + rs2_val + (if (ci_val) 1 else 0)
        val rd_val       = clipped(rd_carry_val)(ClipWidth(rd_width))
        val co_val       = rd_carry_val.testBit(rd_width)
        update(rd, rd_val)
        updateOvf(rd, co_val)

      case PadZero(rd, rs, width, annons) =>
        val rs_val = state.register_file(rs)
        update(rd, rs_val)

      case Mov(rd, rs, _) =>
        val rs_val = state.register_file(rs)
        update(rd, rs_val)

      case SetCarry(rd, _) =>
        updateOvf(rd, true)
      case ClearCarry(rd, _) =>
        updateOvf(rd, false)
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
      case GlobalLoad(rd, base, _, _) =>
        val addressParts = base.map(state.register_file(_))
        assert(
          addressParts.length == 3,
          s"expected 3 registers in global memory address ${instruction}"
        )
        val concreteAddr =
          addressParts(0) | (addressParts(1) << 16) | (addressParts(2) << 32)
        if (!concreteAddr.isValidInt) {
          ctx.logger.error(s"Invalid global memory address ${concreteAddr}!", instruction)
          update(rd, BigInt(0))
        } else {
          val rdVal = state.globalMemory(concreteAddr.toInt)
          update(rd, rdVal)
        }
      case GlobalStore(rs, base, pred, _, _) =>
        val addressParts = base.map(state.register_file(_))
        assert(
          addressParts.length == 3,
          s"expected 3 registers in global memory address ${instruction}"
        )
        val concreteAddr =
          addressParts(0) | (addressParts(1) << 16) | (addressParts(2) << 32)
        if (!concreteAddr.isValidInt) {
          ctx.logger.error(s"Invalid global memory address ${concreteAddr}", instruction)
        } else {
          val enabled = pred
            .map(state.register_file(_) == 1)
            .getOrElse(state.predicate)
          if (enabled) {
            val rsVal = state.register_file(rs)
            state.globalMemory(concreteAddr.toInt) = rsVal
          }
        }
      case i @ (_: Recv | _: BreakCase) =>
        ctx.logger.error("Illegal instruction", instruction)
        state.exception_occurred = Some(InterpretationFailure)

    }

    sealed trait StepStatus
    case object StepVCycleFinish   extends StepStatus
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

    def runVirtualCycle(): Option[InterpretationTrap] = {
      step() match {
        case StepVCycleContinue =>
          if (getException().isEmpty) runVirtualCycle() else getException()
        case StepVCycleFinish => // clear out the label bindings (not really
          // needed but may help catch some errors!)
          state.label_bindings.clear()
          getException()
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

  /** Create an instance of the interpreter to use outside the check
    *
    * @param program
    * @param vcdDump
    * @param monitor
    * @param ctx
    * @return
    */
  def instance(
      program: DefProgram,
      vcdDump: Option[UnconstrainedValueChangeWriter] = None,
      monitor: Option[UnconstrainedIRInterpreterMonitor] = None,
      serial: Option[String => Unit] = None
  )(implicit ctx: AssemblyContext): ProcessInterpreter = {
    require(
      program.processes.length == 1,
      "Could only interpret a single process"
    )
    new ProcessInterpreter(program.processes.head, vcdDump, monitor, serial)(
      ctx
    )
  }

  /** @param source
    * @param context
    */
  override def check(source: UnconstrainedIR.DefProgram)(implicit
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
