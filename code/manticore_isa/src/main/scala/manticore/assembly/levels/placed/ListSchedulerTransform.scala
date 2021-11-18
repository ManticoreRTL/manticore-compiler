// package manticore.assembly.levels.placed

// import manticore.assembly.levels.AssemblyTransformer
// import manticore.compiler.AssemblyContext

// object ListSchedulerTransform extends AssemblyTransformer(PlacedIR, PlacedIR) {

//   import PlacedIR._

//   class DependenceGraph {

//     case class Vertex(inst: Instruction, weight: Int, egress: List[Edge])
//     case class Edge(target: Vertex, weight: Int)

//   }

  

//   def instructionLatency(instruction: Instruction): Int = ???

  
//   def createDependenceGraph(instructions: Seq[Instruction]): Graph[Int, Int] = {

//     def regUses(inst: Instruction): List[Name] = inst match {
//       case BinaryArithmetic(operator, rd, rs1, rs2, annons) =>
//         List(rs1, rs2)
//       case CustomInstruction(func, rd, rs1, rs2, rs3, rs4, annons) =>
//         List(rs1, rs2, rs3, rs4)
//       case Expect(ref, got, error_id, annons) =>
//         List(ref, got)
//       case LocalLoad(rd, base, offset, annons) =>
//         List(base)
//       case LocalStore(rs, base, offset, annons) =>
//         List(rs, base)
//       case GlobalLoad(rd, base, annons) =>
//         List(base._1, base._2, base._3, base._4)
//       case GlobalStore(rs, base, annons) =>
//         List(rs, base._1, base._2, base._3, base._4)
//       case Send(rd, rs, dest_id, annons) =>
//         List(rs)
//       case SetValue(rd, value, annons) =>
//         List(rd)
//     }

//     // def regDef(inst: Instruction): Option[Name] = inst match {

//     // }
//     // instructions.map { inst => instructions.filter { uses => } }
//     ???
//   }

//   override def transform(
//       source: DefProgram,
//       context: AssemblyContext
//   ): DefProgram = ???

// }
