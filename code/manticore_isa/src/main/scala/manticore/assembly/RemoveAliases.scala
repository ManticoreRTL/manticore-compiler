package manticore.assembly

/** RemoveAliases.scala
  *
  * @author
  *   Sahand Kashani <sahand.kashani@epfl.ch>
  */

import manticore.assembly.levels.AssemblyTransformer
import manticore.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.AssemblyContext
import manticore.assembly.levels.ConstType

/** This transform identifies and removes aliases from the design. Processing
  * the following input program:
  *
  * ```
  *   .const c_0 4 0
  *   .const c_1 4 0
  *   .wire c 4
  *   .wire d 4
  *   .wire e 4
  *   .wire f 4
  *   .wire g 4
  *   .wire h 4
  *   add c, d, e
  *   add f, c, c_0
  *   add g, d, c_1
  *   add h, f, g
  * ```
  *
  * Will result in the following output:
  *
  * ```
  *   .const c_0 4 0
  *   .const c_1 4 0
  *   .wire c 4
  *   .wire d 4
  *   .wire e 4
  *   .wire f 4
  *   .wire g 4
  *   .wire h 4
  *   add c, d, e
  *   add f, c, c_0
  *   add g, d, c_0     <<< "c_1" was replaced with "c_0"
  *   add h, c, d       <<< "f" was replaced with "c"; "g" was replaced with "d".
  * ```
  */
object RemoveAliases
    extends AssemblyTransformer(UnconstrainedIR, UnconstrainedIR) {

  import UnconstrainedIR._

  def removeAliases(
      asm: DefProcess
  ): DefProcess = {

    /** Finds all aliases in the instruction sequence.
      *
      * @param body
      *   Target instructions.
      * @param consts
      *   Constants referenced in `body`. Each constant is defined by a width
      *   and a value.
      * @return
      *   Map of aliases in `body` and `consts`.
      */
    def findAliases(
        body: Seq[Instruction],
        consts: Map[Name, (Int, Constant)]
    ): Map[Name, Name] = {

      def findBodyAliases(
          body: Seq[Instruction],
          consts: Map[Name, (Int, Constant)],
          aliases: Map[Name, Name] = Map.empty
      ): Map[Name, Name] = {
        body match {
          case Nil =>
            // End of instruction stream. We return all aliases we have found so far.
            aliases

          case head :: tail =>
            val newAliases = collection.mutable.Map[Name, Name]()
            // Must use capital letter for first char, otherwise the pattern match below will not work!
            val Zero = BigInt(0)

            head match {
              case BinaryArithmetic(operator, rd, rs1, rs2, annons) =>
                operator match {
                  case BinaryOperator.ADD =>
                    // If rs1 is 0, then rd is an alias of rs2.
                    consts.get(rs1) match {
                      // We don't care about the width of the 0.
                      case Some((width, Zero)) => newAliases += rd -> rs2
                      case _                   =>
                    }
                    // if rs2 is 0, then rd is an alias of rs1.
                    consts.get(rs2) match {
                      // We don't care about the width of the 0.
                      case Some((width, Zero)) => newAliases += rd -> rs1
                      case _                   =>
                    }

                  // Handle aliases generated by
                  // ADD, ADDC, SUB, OR, AND, XOR, MUL, SEQ, SLL, SRL, SLTU, SLTS, SGTU, SGTS
                  case _ =>
                }

              // Handle aliases in MUX, LOADs, STOREs, ...
              case _ =>
            }

            findBodyAliases(tail, consts, aliases ++ newAliases)
        }
      }

      def findConstAliases(
          consts: Map[Name, (Int, Constant)]
      ): Map[Name, Name] = {
        val constAliases = consts
          .groupBy { case (name, (width, value)) =>
            (width, value)
          }
          .flatMap { case ((width, value), names) =>
            // All these names are aliases of one another. We use the first element from the set as the representative
            // and mark all others as being aliases of this one.
            val group = names.keys
            val (representative, aliases) = (group.head, group.tail)
            val aliasMap = aliases.map { alias =>
              alias -> representative
            }.toMap
            aliasMap
          }

        constAliases
      }

      val bodyAliases = findBodyAliases(body, consts)
      val constAliases = findConstAliases(consts)
      val allAliases = bodyAliases ++ constAliases

      allAliases
    }

    /** Backtracks along alias chains and finds the root of every alias.
      *
      * If we have the following input aliases:
      *
      * ```
      * a -> b
      * b -> c
      * c -> d
      * ```
      *
      * Then the output aliases will be:
      *
      * ```
      * a -> d
      * b -> d
      * c -> d
      * ```
      *
      * @param aliases
      *   Aliases to resolve.
      * @return
      *   Resolved aliases.
      */
    def resolveAliases(
        aliases: Map[Name, Name]
    ): Map[Name, Name] = {
      // Cache of backtracking results to avoid exponential lookup if we end up
      // going up the same tree multiple times.
      val aliasCache = collection.mutable.Map() ++ aliases

      def resolve(name: Name): Name = {
        aliasCache.get(name) match {
          case Some(alias) =>
            val root = resolve(alias)
            // Keep backtracking result to prevent exponential lookups later.
            aliasCache += name -> root
            root
          case None =>
            name
        }
      }

      aliases.map { case (name, alias) =>
        name -> resolve(alias)
      }
    }

    /** Replaces aliased names in the input instruction using the given alias
      * table.
      *
      * @param instr
      *   Target instruction.
      * @param aliases
      *   Map of all known aliases.
      * @return
      *   Instruction with aliases replaced.
      */
    def replaceAliases(
        instr: Instruction,
        aliases: Map[Name, Name]
    ): Instruction = {
      def replaceName(name: Name): Name = {
        aliases.get(name) match {
          case Some(rootName) =>
            // `name` is an alias of rootName, so we return `rootName`.
            rootName
          case None =>
            // Original name unchanged.
            name
        }
      }

      instr match {
        case BinaryArithmetic(operator, rd, rs1, rs2, annons) =>
          BinaryArithmetic(
            operator,
            rd,
            replaceName(rs1),
            replaceName(rs2),
            annons
          )

        case CustomInstruction(func, rd, rs1, rs2, rs3, rs4, annons) =>
          CustomInstruction(
            func,
            rd,
            replaceName(rs1),
            replaceName(rs2),
            replaceName(rs3),
            replaceName(rs4),
            annons
          )

        case LocalLoad(rd, base, offset, annons) =>
          LocalLoad(rd, replaceName(base), offset, annons)

        case LocalStore(rs, base, offset, predicate, annons) =>
          val newPredicate = predicate.map(replaceName)
          LocalStore(rs, replaceName(base), offset, newPredicate, annons)

        case GlobalLoad(rd, base, annons) =>
          val newBase =
            (replaceName(base._1), replaceName(base._2), replaceName(base._3))
          GlobalLoad(rd, newBase, annons)

        case GlobalStore(rs, base, predicate, annons) =>
          val newBase =
            (replaceName(base._1), replaceName(base._2), replaceName(base._3))
          val newPredicate = predicate.map(replaceName)
          GlobalStore(rs, newBase, newPredicate, annons)

        case SetValue(rd, value, annons) =>
          SetValue(rd, value, annons)

        case Send(rd, rs, dest_id, annons) =>
          Send(rd, replaceName(rs), dest_id, annons)

        case Expect(ref, got, error_id, annons) =>
          Expect(replaceName(ref), replaceName(got), error_id, annons)

        case Predicate(rs, annons) =>
          Predicate(replaceName(rs), annons)

        case Mux(rd, sel, rs1, rs2, annons) =>
          Mux(rd, replaceName(sel), replaceName(rs1), replaceName(rs2), annons)

        case Nop =>
          Nop
      }
    }

    val consts = asm.registers
      .filter { reg =>
        reg.variable.tpe == ConstType
      }
      .map { reg =>
        reg.variable.name -> (reg.variable.width, reg.value.get)
      }
      .toMap

    val aliases = findAliases(asm.body, consts)
    val resolvedAliases = resolveAliases(aliases)
    // Note that we do not map on the registers of the assembly program as "resolvedAliases" already
    // contains constant-to-constant aliases as well and these are automatically replaced in the body.
    val newBody = asm.body.map(instr => replaceAliases(instr, resolvedAliases))

    asm.copy(body = newBody)
  }

  override def transform(
      asm: DefProgram,
      context: AssemblyContext
  ): DefProgram = {
    implicit val ctx = context

    val out = DefProgram(
      processes = asm.processes.map(process => removeAliases(process)),
      annons = asm.annons
    )

    if (logger.countErrors > 0) {
      logger.fail(s"Failed transform due to previous errors!")
    }

    out
  }
}
