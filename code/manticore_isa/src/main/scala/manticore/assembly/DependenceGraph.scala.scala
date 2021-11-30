package manticore.assembly

/** DependenceGraph.scala
  *
  * @author
  *   Sahand Kashani <sahand.kashani@epfl.ch>
  */

import manticore.assembly.levels.AssemblyTransformer
import manticore.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.AssemblyContext
import scalax.collection.GraphEdge.DiEdge

/** This transform identifies dead code and removes it from the design. Dead
  * code consists of names that are never referenced once written. Note that we
  * replace dead code with `Nop` instructions here. A scheduling pass needs to
  * be run later to try and eliminate the `Nop`s while ensuring that program
  * execution is correct.
  */
object DependenceGraph {

  import UnconstrainedIR._

  /** Extracts the registers read by the instruction.
    *
    * @param inst
    *   Target instruction.
    * @return
    *   Registers read by the instruction.
    */
  def regUses(inst: Instruction): Seq[Name] = {
    inst match {
      case BinaryArithmetic(operator, rd, rs1, rs2, annons) =>
        Seq(rs1, rs2)
      case CustomInstruction(func, rd, rs1, rs2, rs3, rs4, annons) =>
        Seq(rs1, rs2, rs3, rs4)
      case LocalLoad(rd, base, offset, annons) =>
        Seq(base)
      case LocalStore(rs, base, offset, predicate, annons) =>
        val pred = predicate match {
          case None      => Seq.empty
          case Some(reg) => Seq(reg)
        }
        Seq(rs, base) ++ pred
      case GlobalLoad(rd, base, annons) =>
        Seq(base._1, base._2, base._3)
      case GlobalStore(rs, base, predicate, annons) =>
        val pred = predicate match {
          case None      => Seq.empty
          case Some(reg) => Seq(reg)
        }
        Seq(rs, base._1, base._2, base._3) ++ pred
      case Send(rd, rs, dest_id, annons) =>
        Seq(rs)
      case SetValue(rd, value, annons) =>
        Seq()
      case Mux(rd, sel, rs1, rs2, annons) =>
        Seq(sel, rs1, rs2)
      case Expect(ref, got, error_id, annons) =>
        Seq(ref, got)
      case Predicate(rs, annons) =>
        Seq(rs)
      case Nop =>
        Seq.empty
    }
  }

  /** Extracts the register defined by the instruction (if it defines one).
    *
    * @param inst
    *   Target instruction.
    * @return
    *   Register written by the instruction (if any).
    */
  def regDef(inst: Instruction): Option[Name] = {
    inst match {
      case BinaryArithmetic(operator, rd, rs1, rs2, annons)        => Some(rd)
      case CustomInstruction(func, rd, rs1, rs2, rs3, rs4, annons) => Some(rd)
      case LocalLoad(rd, base, offset, annons)                     => Some(rd)
      case LocalStore(rs, base, offset, p, annons)                 => None
      case GlobalLoad(rd, base, annons)                            => Some(rd)
      case GlobalStore(rs, base, pred, annons)                     => None
      case Send(rd, rs, dest_id, annons)                           => None
      case SetValue(rd, value, annons)                             => Some(rd)
      case Mux(rd, sel, rs1, rs2, annons)                          => Some(rd)
      case Expect(ref, got, error_id, annons)                      => None
      case Predicate(rs, annons)                                   => None
      case Nop                                                     => None
    }
  }

  def apply(
      instrs: Seq[Instruction]
  ): (
    scalax.collection.Graph[Int, DiEdge],
    Map[Int, Instruction]
  ) = {
    // Start by assigning an identifier to every instruction. One would think the destination register
    // of an instruction is the identifier, but some instructions do not write registers. This is why
    // we explicitly create an index to refer to every instruction.
    val instrToIdMap = collection.mutable.Map[Instruction, Int]()
    val idToInstrMap = collection.mutable.Map[Int, Instruction]()
    val regDefToInstr = collection.mutable.Map[Name, Instruction]()

    // Populate all data structures.
    instrs.zipWithIndex.foreach { case (instr, idx) =>
      instrToIdMap += (instr -> idx)
      idToInstrMap += (idx -> instr)
      regDef(instr) match {
        case Some(reg) => regDefToInstr += (reg -> instr)
        case None      =>
      }
    }

    // Create dependence graph.
    val dependenceGraph = scalax.collection.mutable.Graph[Int, DiEdge]()
    // Add all vertices to the graph. Each vertex represents an instruction.
    instrs.foreach { instr =>
      dependenceGraph.add(instrToIdMap(instr))
    }
    // Add all edges based on program dependencies.
    instrs.foreach { instr =>
      val readRegs = regUses(instr)
      val predInstrs = readRegs.flatMap { reg =>
        // If an instruction writes to the register, that instruction is a predecessor to this instruction.
        // If no instruction writes to the register, then it must be an input of the program (in which
        // case we do not create a dependency since inputs are implicit dependencies).
        regDefToInstr.get(reg)
      }
      val instrIdx = instrToIdMap(instr)
      val edges = predInstrs.map { predInstr =>
        val predInstrIdx = instrToIdMap(predInstr)
        DiEdge(predInstrIdx, instrIdx)
      }
      edges.foreach(edge => dependenceGraph.add(edge))
    }

    // if (dependenceGraph.isCyclic) {
    //   logger.error("Could not create acyclic dependence graph!")
    // }

    (dependenceGraph, idToInstrMap.toMap)
  }
}
