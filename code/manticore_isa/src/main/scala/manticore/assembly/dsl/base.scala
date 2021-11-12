package manticore.assembly.dsl

import manticore.assembly.ManticoreAssemblyIR
import scala.collection.mutable.ArrayBuffer

object DslBase {

  /** Base class for Assembly DSL
    */
  abstract class AbstractAssemblyProgram extends ManticoreAssemblyIR {

    class AssemblyProcess(val id: ProcessId) {
      val reg_defs: ArrayBuffer[DefReg] = ArrayBuffer()
      val func_defs: ArrayBuffer[DefFunc] = ArrayBuffer()
      val insts: ArrayBuffer[Instruction] = ArrayBuffer()

      object DEF {
        def REG(rd: Variable, value: Constant): Variable = {
          val r = DefReg(rd, Some(value))
          reg_defs += r
          r.variable
        }
        def REG(rd: Variable): Variable = {
          val r = DefReg(rd, None)
          reg_defs += r
          r.variable
        }
        def FUNC(name: Name, func: CustomFunction): Name = {
          val f = DefFunc(name, func)
          func_defs += DefFunc(name, func)
          f.name
        }
      }

      private def mkArithmetic(
          op: BinaryOperator.BinaryOperator,
          rd: Name,
          rs1: Name,
          rs2: Name
      ): Instruction = {
        val i = BinaryArithmetic(op, rd, rs1, rs2)
        insts += i
        i
      }
      def ADD(rd: Name, rs1: Name, rs2: Name) =
        mkArithmetic(BinaryOperator.ADD, rd, rs1, rs2)
      def ADDC(rd: Name, rs1: Name, rs2: Name) =
        mkArithmetic(BinaryOperator.ADD, rd, rs1, rs2)
      def OR(rd: Name, rs1: Name, rs2: Name) =
        mkArithmetic(BinaryOperator.OR, rd, rs1, rs2)
      def AND(rd: Name, rs1: Name, rs2: Name) =
        mkArithmetic(BinaryOperator.AND, rd, rs1, rs2)
      def XOR(rd: Name, rs1: Name, rs2: Name) =
        mkArithmetic(BinaryOperator.XOR, rd, rs1, rs2)
      def MUL(rd: Name, rs1: Name, rs2: Name) =
        mkArithmetic(BinaryOperator.MUL, rd, rs1, rs2)
      def SEQ(rd: Name, rs1: Name, rs2: Name) =
        mkArithmetic(BinaryOperator.SEQ, rd, rs1, rs2)
      def SLTU(rd: Name, rs1: Name, rs2: Name) =
        mkArithmetic(BinaryOperator.SLTU, rd, rs1, rs2)
      def SLTS(rd: Name, rs1: Name, rs2: Name) =
        mkArithmetic(BinaryOperator.SLTS, rd, rs1, rs2)
      def SGTU(rd: Name, rs1: Name, rs2: Name) =
        mkArithmetic(BinaryOperator.SGTU, rd, rs1, rs2)
      def SGTS(rd: Name, rs1: Name, rs2: Name) =
        mkArithmetic(BinaryOperator.SGTS, rd, rs1, rs2)
      def MUX(rd: Name, rs1: Name, rs2: Name) =
        mkArithmetic(BinaryOperator.MUX, rd, rs1, rs2)
      def SRL(rd: Name, rs1: Name, rs2: Name) =
        mkArithmetic(BinaryOperator.SRL, rd, rs1, rs2)
      def SLL(rd: Name, rs1: Name, rs2: Name) =
        mkArithmetic(BinaryOperator.SLL, rd, rs1, rs2)
      def LVEC(
          rd: Name,
          func: Name,
          rs1: Name,
          rs2: Name,
          rs3: Name,
          rs4: Name
      ) = {
        val i = CustomInstruction(func, rd, rs1, rs2, rs3, rs4)
        insts += i
        i
      }

      def LLD(rd: Name, base: Name, offset: Constant) = {
        val i = LocalLoad(rd, base, offset)
        insts += i
        i
      }

      def LST(rd: Name, base: Name, offset: Constant) = {
        val i = LocalStore(rd, base, offset)
        insts += i
        i
      }

      def GLD(rd: Name, base: (Name, Name, Name, Name)) = {
        val i = GlobalLoad(rd, base)
        insts += i
        i
      }
      def GST(rd: Name, base: (Name, Name, Name, Name)) = {
        val i = GlobalStore(rd, base)
        insts += i
        i
      }

      def SEND(rd: Name, rs: Name, dest_id: ProcessId) = {
        val i = Send(rd, rs, dest_id)
        insts += i
        i
      }

      def EXPECT(ref: Name, got: Name, error_id: ExceptionId) = {
        val i = Expect(ref, got, error_id)
        insts += i
        i
      }
    }
    private val processes: ArrayBuffer[DefProcess] = ArrayBuffer()

    def freshId(): ProcessId
    object PROCESS {
      def apply(p: AssemblyProcess): DefProcess = {
        val dp = DefProcess(
          id = p.id,
          registers = p.reg_defs.toSeq,
          functions = p.func_defs.toSeq,
          body = p.insts.toSeq
        )
        processes += dp
        dp
      }

    }

    def end: DefProgram = DefProgram(processes.toSeq)

  }

}

/** Example DSL usage
  * {{{
  * class AssemblyProgram extends AbstractAssemblyProgram {
  *
  *   type Constant = Int
  *   type Name = String
  *   type ProcessId = Int
  *   type Variable = String
  *   type CustomFunction = Int
  *   var next_id: Int = 0
  *   override protected def freshId(): ProcessId = {
  *     val id = next_id
  *     next_id = next_id + 1
  *   id
  *   }
  * }
  * }}}
  *
  * {{{
  * val my_prog = new AssemblyProgram {
  *
  *   PROCESS {
  *     new AssemblyProcess(freshId) {
  *       val r1 = DEF.REG("r1", 2)
  *       val r2 = DEF.REG("r1")
  *       val f0 = DEF.FUNC("f0", 0x10)
  *       ADD(r1, r1, r1)
  *       LVEC(r1, f0, r1, r1, r1, r1)
  *     }
  *   }
  * }.end
  *
  * }}}
  */
