package manticore.compiler.assembly.levels

import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.assembly.BinaryOperator._
import scala.collection.immutable.ListMap

trait StatisticReporter extends Flavored {
  import flavor._
  def reportStats(
      prog: DefProgram
  )(implicit ctx: AssemblyContext): Unit = {

    class ReportBuilder {
      val text = new StringBuilder
      private var m_indent = 0

      def indent() = {
        m_indent += 1
      }
      def unindent() = {
        m_indent -= 1
      }
      def emit(s: String) = text ++= "\t" * m_indent + s + "\n"
      def build(): String = text.toString()
    }
    val report_builder = new ReportBuilder
    prog.processes.map { proc =>

      report_builder emit s"Process ${proc.id}:"

      var inst_count = ListMap[String, Int](
        "CUST" -> 0,
        "LLD" -> 0,
        "LST" -> 0,
        "GLD" -> 0,
        "GST" -> 0,
        "SET" -> 0,
        "SEND" -> 0,
        "RECV" -> 0,
        "EXPECT" -> 0,
        "MUX" -> 0,
        "NOP" -> 0,
        "ADDCARRY" -> 0,
        "CLEARCARRY" -> 0,
        "SETCARRY" -> 0,
        "PADZERO" -> 0,
        "MOV" -> 0,
        "PREDICATE" -> 0,
        "ADD" -> 0,
        "SUB" -> 0,
        "MUL" -> 0,
        "AND" -> 0,
        "OR" -> 0,
        "XOR" -> 0,
        "SLL" -> 0,
        "SRL" -> 0,
        "SRL" -> 0,
        "SRA" -> 0,
        "SEQ" -> 0,
        "SLTS" -> 0
      )

      def incr(name: String): Unit = {
        inst_count = inst_count.updated(name, inst_count(name) + 1)
      }
      proc.body.foreach {
        case _: CustomInstruction => incr("CUST")
        case BinaryArithmetic(op, _, _, _, _) =>
          op match {
            case ADD  => incr("ADD")
            case SUB  => incr("SUB")
            case MUL  => incr("MUL")
            case AND  => incr("AND")
            case OR   => incr("OR")
            case XOR  => incr("XOR")
            case SLL  => incr("SLL")
            case SRL  => incr("SRL")
            case SRA  => incr("SRA")
            case SEQ  => incr("SEQ")
            case SLTS => incr("SLTS")
            case _    => ctx.logger.error(s"invalid operator ${op}!")
          }
        case _: GlobalLoad  => incr("GLD")
        case _: GlobalStore => incr("GST")
        case _: LocalLoad   => incr("LLD")
        case _: LocalStore  => incr("LST")
        case _: Expect      => incr("EXPECT")
        case _: Predicate   => incr("PREDICATE")
        case _: AddC        => incr("ADDCARRY")
        case _: ClearCarry  => incr("CLEARCARRY")
        case _: SetCarry    => incr("SETCARRY")
        case _: Mov         => incr("MOV")
        case _: PadZero     => incr("PADZERO")
        case _: Mux         => incr("MUX")
        case _: Send        => incr("SEND")
        case _: Recv        => incr("RECV")
        case _: SetValue    => incr("SET")
        case Nop            => incr("NOP")
      }

      val num_instructions = proc.body.length

      report_builder.indent()
      report_builder emit "Instructions:"
      report_builder.indent()
      report_builder.emit(f"${"Total"}%-15s:\t\t\t${num_instructions}")
      inst_count.foreach { case (k, v) =>
        report_builder emit f"${k}%-15s:\t\t\t${v}%4d\t\t\t${v.toDouble / num_instructions.toDouble * 100.0}%4.1f"
      }
      report_builder.unindent()
      report_builder emit "Registers:"
      report_builder.indent()
      report_builder emit s"Total\t\t:\t\t\t${proc.registers.length}"
      report_builder.unindent()


      proc

    }

    ctx.logger.info(report_builder.build())

  }

  // def report[T <: UnconstrainedIR]

}
