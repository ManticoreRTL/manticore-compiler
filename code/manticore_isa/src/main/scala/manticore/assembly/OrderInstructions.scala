package manticore.assembly

/** OrderInstructions.scala
  *
  * @author
  *   Sahand Kashani <sahand.kashani@epfl.ch>
  */

import manticore.assembly.levels.AssemblyTransformer
import manticore.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.AssemblyContext
import scalax.collection.mutable.Graph
import scalax.collection.GraphEdge.DiEdge

/** This transform identifies dead code and removes it from the design. Dead
  * code consists of names that are never referenced once written. Note that we
  * replace dead code with `Nop` instructions here. A scheduling pass needs to
  * be run later to try and eliminate the `Nop`s while ensuring that program
  * execution is correct.
  */
object OrderInstructions
    extends AssemblyTransformer(UnconstrainedIR, UnconstrainedIR) {

  import UnconstrainedIR._

  def orderInstructions(
      asm: DefProcess
  ): DefProcess = {
    val dstRegToInstrMap = asm.body.foldLeft(Map.empty[Name, Instruction]) {
      case (acc, instr) =>
        instr match {
          case BinaryArithmetic(operator, rd, rs1, rs2, annons) =>
            acc + (rd -> instr)
          case CustomInstruction(func, rd, rs1, rs2, rs3, rs4, annons) =>
            acc + (rd -> instr)
          case LocalLoad(rd, base, offset, annons) =>
            acc + (rd -> instr)
          case LocalStore(rs, base, offset, predicate, annons) =>
            // The instruction does not write to a register.
            acc
          case GlobalLoad(rd, base, annons) =>
            acc + (rd -> instr)
          case GlobalStore(rs, base, predicate, annons) =>
            // The instruction does not write to a register.
            acc
          case SetValue(rd, value, annons) =>
            acc + (rd -> instr)
          case Send(rd, rs, dest_id, annons) =>
            // The instruction does not write to a register.
            acc
          case Expect(ref, got, error_id, annons) =>
            // The instruction does not write to a register.
            acc
          case Predicate(rs, annons) =>
            // The instruction does not write to a register.
            acc
          case Mux(rd, sel, rs1, rs2, annons) =>
            acc + (rd -> instr)
          case Nop =>
            // The instruction does not write to a register.
            acc
        }
    }
    val regs = asm.registers.map(reg => reg.variable.name -> reg).toMap

    type Node = Either[Instruction, DefReg]
    def NodeInstr(instr: Instruction): Node = Left(instr)
    def NodeReg(regName: Name): Node = Right(regs(regName))
    def getNode(regName: Name): Node = {
      // The name will always exist in the list of registers. We want to know if an instruction is writing to the name,
      // or if the name is a port or a constant that is only read. If it is only read, we return the register. If it is
      // written, we return the instruction that writes the register.
      dstRegToInstrMap.get(regName) match {
        case Some(instr) => NodeInstr(instr)
        case None        => NodeReg(regName)
      }
    }

    val g = Graph[Node, DiEdge]()

    // Add all vertices to the graph.
    val nodes = regs.keys.map(name => getNode(name))
    nodes.foreach(node => g.add(node))

    // Add all edges based on program dependencies.
    asm.body.foreach { instr =>
      val edges = instr match {
        case BinaryArithmetic(operator, rd, rs1, rs2, annons) =>
          Seq(
            DiEdge(getNode(rs1), getNode(rd)),
            DiEdge(getNode(rs2), getNode(rd))
          )
        case CustomInstruction(func, rd, rs1, rs2, rs3, rs4, annons) =>
          Seq(
            DiEdge(getNode(rs1), getNode(rd)),
            DiEdge(getNode(rs2), getNode(rd)),
            DiEdge(getNode(rs3), getNode(rd)),
            DiEdge(getNode(rs4), getNode(rd))
          )
        case LocalLoad(rd, base, offset, annons) =>
          Seq(
            DiEdge(getNode(base), getNode(rd))
          )
        case LocalStore(rs, base, offset, predicate, annons) =>
          // Store does not write to a register, so we manually make the edge point to this instruction.
          Seq(
            DiEdge(getNode(rs), NodeInstr(instr)),
            DiEdge(getNode(base), NodeInstr(instr))
          )
        case GlobalLoad(rd, base, annons) =>
          Seq(
            DiEdge(getNode(base._1), getNode(rd)),
            DiEdge(getNode(base._2), getNode(rd)),
            DiEdge(getNode(base._3), getNode(rd))
          )
        case GlobalStore(rs, base, predicate, annons) =>
          // Store does not write to a register, so we manually make the edge point to this instruction.
          Seq(
            DiEdge(getNode(rs), NodeInstr(instr)),
            DiEdge(getNode(base._1), NodeInstr(instr)),
            DiEdge(getNode(base._2), NodeInstr(instr)),
            DiEdge(getNode(base._3), NodeInstr(instr))
          )
        case SetValue(rd, value, annons) =>
          // SetValue defines a source vertex. It doesn't have dependencies as the constant is encoded in the instruction.
          Seq.empty
        case Send(rd, rs, dest_id, annons) =>
          // Send does not write a register, so we manually make the edge point to this instruction.
          Seq(
            DiEdge(getNode(rd), NodeInstr(instr)),
            DiEdge(getNode(rs), NodeInstr(instr))
          )
        case Expect(ref, got, error_id, annons) =>
          // Expect does not write a register, so we manually make the edge point to this instruction.
          Seq(
            DiEdge(getNode(ref), NodeInstr(instr)),
            DiEdge(getNode(got), NodeInstr(instr))
          )
        case Predicate(rs, annons) =>
          // Predicate writes to a non-user-visible register, so we manually make the edge point to this instruction.
          Seq(
            DiEdge(getNode(rs), NodeInstr(instr))
          )
        case Mux(rd, sel, rs1, rs2, annons) =>
          Seq(
            DiEdge(getNode(sel), getNode(rd)),
            DiEdge(getNode(rs1), getNode(rd)),
            DiEdge(getNode(rs2), getNode(rd))
          )
        case Nop =>
          Seq.empty
      }

      edges.foreach(e => g.add(e))
    }

    def writeDot(g: Graph[Node, DiEdge]): String = {
      val out = new StringBuilder()
      val indent = "  "

      // Assign a number to every node.
      val nodes = g.nodes.zipWithIndex.toMap

      // a [label="Foo"];

      out.append("digraph graph {")

      // Declare all nodes
      nodes.foreach { case (node, idx) =>
        val nodeStr = node.toOuter match {
          case Left(instr)    => instr.serialized
          case Right(regName) => regName.serialized
        }
        out.append(s"${indent}${idx} [label = ${nodeStr}]\n")
      }

      out.append("}") // digraph

      out.toString()
    }

    import java.nio.file.Files
    import java.io.PrintWriter

    val writer = new PrintWriter("dumps/dot.dot")
    writer.print(writeDot(g))
    writer.close()

    asm
  }

  override def transform(
      asm: DefProgram,
      context: AssemblyContext
  ): DefProgram = {
    implicit val ctx = context

    val out = DefProgram(
      processes = asm.processes.map(process => orderInstructions(process)),
      annons = asm.annons
    )

    if (logger.countErrors > 0) {
      logger.fail(s"Failed transform due to previous errors!")
    }

    out
  }
}
