// package manticore.assembly.levels.placed

// import manticore.assembly.levels.Transformation
// import manticore.assembly.levels.AssemblyTransformer
// import manticore.compiler.AssemblyContext
// import manticore.assembly.levels.ConstType
// import manticore.assembly.levels.UInt16

// import scala.collection.mutable.{Queue => MutableQueue}
// import scala.collection.mutable.{ArrayDeque => MutableDeque}
// import scala.collection.mutable.PriorityQueue
// import manticore.assembly.DependenceGraphBuilder
// import manticore.assembly.ManticoreAssemblyIR

// /** Register allocation transform using a linear scan
//   * @author
//   *   Mahyar Emami <mahyar.emami@epfl.ch>
//   */

// object RegisterAllocationTransform
//     extends DependenceGraphBuilder
//     with AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {

//   val flavor = PlacedIR
//   import PlacedIR._

//   // import manticore.assembly.levels.placed.  type LivenessScope = Tuple2[Name, Tuple2[Int, Int]]
//   private def computeLiveness(
//       process: DefProcess
//   )(implicit ctx: AssemblyContext): Map[Name, (Int, Int)] = {

//     // initialize liveness scopes, constants are always live, therefore,
//     // their liveness interval is 0 to infinity.
//     val life_begin = scala.collection.mutable.Map(
//       process.registers.collect {
//         case DefReg(v, Some(_), _) if v.varType == ConstType =>
//           v.name -> 0
//       }: _*
//     )
//     val life_end = scala.collection.mutable.Map.empty[Name, Int]

//     /** Now walk the sequence of instruction. Every time a register value is
//       * defined, create mapping from the defined register to an interval while
//       * marking the start with the instruction index. Contrarily, when a
//       * register is used, find update its liveness bound end with the index of
//       * the instruction.
//       */

//     process.body.zipWithIndex.foreach { case (inst, cycle) =>
//       DependenceAnalysis.regDef(inst) match {
//         case Seq() =>
//         case rds @ _ =>
//           rds.foreach { rd =>
//             if (life_begin.contains(rd)) {
//               ctx.logger.error(s"Register ${rd} is defined multiple times!", inst)
//             } else {
//               life_begin += (rd -> cycle)
//             }
//           }
//       }
//       DependenceAnalysis.regUses(inst).foreach { rs =>
//         if (!life_begin.contains(rs)) {
//           ctx.logger.error(s"Register ${rs} is used before being defined", inst)

//         }
//         life_end += (rs -> cycle)
//       }
//     }

//     life_begin.map { case (name, beg) =>
//       life_end.get(name) match {
//         case Some(end) => (name -> (beg, end))
//         case None =>
//           ctx.logger.warn(
//             s"Register ${name} in process ${process.id} is never used."
//           )
//           (name -> (beg, beg))
//       }
//     }.toMap

//   }

//   /** Sets the pointer values of all [[DefReg(x: MemoryVariable, _, _)]]
//     * register definitions and returns memory definitions with values
//     * corresponding to correct array memory offsets and the total required
//     * capacity
//     * @param process
//     * @param ctx
//     * @return
//     */
//   private def allocateLocalMemory(
//       process: DefProcess
//   )(implicit ctx: AssemblyContext): (Seq[DefReg], Int) = {

//     /** First allocate local memories, this will tell us whether there is any
//       * "spill" capacity left in the array memory storage. We do this by going
//       * through all .mem definitions and checking their capacity.
//       */
//     val memories = process.registers.collect {
//       case r @ DefReg(v: MemoryVariable, _, _) => r
//     }

//     memories.foldLeft((Seq.empty[DefReg], 0)) { case ((allocs, base), m) =>
//       (
//         allocs :+ m.copy(value = Some(UInt16(base))),
//         base + m.variable.asInstanceOf[MemoryVariable].block.capacity
//       )
//     }
//   }

//   private def defineConstantZeroAndOne(
//       process: DefProcess
//   )(implicit ctx: AssemblyContext): DefProcess = {

//     def findConstant(value: Int): Option[DefReg] =
//       process.registers.find {
//         case DefReg(v, Some(UInt16(value)), _) if v.varType == ConstType => true
//         case _                                                => false
//       }

//     val consts: Map[UInt16, DefReg] = process.registers.collect {
//       case c @ DefReg(v, Some(x), _) if v.varType == ConstType => x -> c
//     }.toMap
//     val with_0 = consts.get(UInt16(0)) match {
//       case None => consts ++ UInt16(0)
//     }
//     val const_1 = consts.get(UInt16(1))

//     process.copy(
//       registers = consts ++ non_consts
//     )
//   }

//   private def allocateRegisters(
//       process: DefProcess
//   )(implicit ctx: AssemblyContext): DefProcess = {

//     val ReservedRegisters = 2 + 1 // first two registers are reserved,
//     // the first two are tied to zero and one and the third is used as a base
//     // for spilling space
//     val MaxRegisters = ctx.max_registers - ReservedRegisters
//     val ArrayMemoryCapacity = 2048

//     val (memories, used_capacity) = allocateLocalMemory(process)

//     val free_capacity = ArrayMemoryCapacity - used_capacity
//     ctx.logger.info(
//       s"Process ${process.id} requires ${used_capacity} " +
//         s"memory words (max usable ${ArrayMemoryCapacity})"
//     )

//     val liveness = computeLiveness(process)
//     val const_0 = process.registers.head.variable.name

//     // get all the non-zero constants
//     val const_decls = process.registers.collect {
//       case r @ DefReg(v: ConstVariable, Some(UInt16(value)), _) if value != 0 =>
//         r
//     }

//     // we spill registers prioritizing the ones that die latest, because these
//     // registers
//     case class LiveRegister(name: Name, lifetime: (Int, Int))
//         extends Ordered[LiveRegister] {
//       override def compare(that: LiveRegister): Int = {
//         Ordering[Int].reverse.compare(this.lifetime._2, that.lifetime._2)
//       }
//     }

//     // a map from unique names to possibly colliding hardware registers IDs
//     val rename_map = scala.collection.mutable.Map.empty[Name, Int]

//     // initialize the free register deque, constants use the
//     val free_ids = MutableDeque[Int](
//       // valid register IDs are 1 to 2046 (i.e., ctx.max_registers - 2)
//       // because register 0 is not writeable (tied to zero)
//       Seq.tabulate(MaxRegisters) { i => i + ReservedRegisters }: _*
//     )
//     val push_cycles = MutableQueue.empty[(Name, Int)]
//     val pop_cycles = MutableQueue.empty[(Name, Int)]
//     // create a queue of unborn names, ordered by increasing birth/start time
//     // we allocate registers by dequeueing an unborn name and pulling an id from
//     // the free_ids and putting in an active list while also keeping a map from
//     // names to the id assigned. If there are no free registers, we look at
//     // a priority queue holding all active registers ordered by decreasing
//     // end time. We "spill" the head of the priority queue
//     // At the same time, we keep another queue, ordered by increasing death/end
//     // time. Dequeueing from this queue means that a register has become free,
//     // we then put the register back in the free_id deque.
//     val unborn_names =
//       MutableQueue.empty[LiveRegister] ++ liveness
//         .map { case (name, range) => LiveRegister(name, range) }
//         .toSeq
//         .sortBy { case LiveRegister(_, life) => life._1 }

//     // .sortBy { case (_, (start, _)) => start }
//     // val live_names = scala.collection.mutable.PriorityQueue[
//     // val active_regs = PriorityQueue.empty[]

//     ctx.logger.fail("Could not allocate registers")
//   }
//   override def transform(
//       source: DefProgram,
//       context: AssemblyContext
//   ): DefProgram = {

//     context.logger.fail("Could not allocate registers")
//   }

// }
