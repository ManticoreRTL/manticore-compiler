package manticore.assembly

import scala.collection.mutable.ArrayBuffer
import scala.util.parsing.input.Positional



trait ManticoreAssemblyIR {

  type Name
  type Constant
  type Variable <: Named
  type CustomFunction
  type ProcessId
  type ExceptionId
  // type SwizzleCode

  trait Named {
    val name: Name
  }
  trait HasSerialized {
    def serialized: String
  }

  trait HasAnnotations {
    val annons: Seq[AssemblyAnnotation]
  }

  case class AssemblyAnnotation(name: String, values: Map[String, String])
      extends Positional {
    def getValue: Map[String, String] = values
    def getName: String = name
    def withElement(k: String, v: String) =
      AssemblyAnnotation(name, values ++ Map(k -> v))
  }

  sealed abstract class IRNode

  sealed abstract class Declaration
      extends IRNode
      with HasAnnotations
      with Positional

  case class DefFunc(
      name: Name,
      value: CustomFunction,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Declaration
      with HasSerialized {
    override def serialized: String = s".def_func ${name} ${value}"
  }

  case class DefReg(
      variable: Variable,
      value: Option[Constant] = Option.empty[Constant],
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Declaration
      with HasSerialized {
    override def serialized: String = s".def_reg ${variable} // ${pos}"
  }

  case class DefProgram(
      processes: Seq[DefProcess],
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Declaration
      with HasSerialized {
    override def serialized: String = processes.foldLeft("") { case (str, p) =>
      str + p.serialized + "\n"
    }
  }

  case class DefProcess(
      id: ProcessId,
      registers: Seq[DefReg],
      functions: Seq[DefFunc],
      body: Seq[Instruction],
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Declaration
      with HasSerialized {
    override def serialized: String = {

      s".proc ${id}:\n\t${(registers.map(_.serialized) ++ functions.map(_.serialized) ++ body
        .map(_.serialized)).mkString("\n\t")}\n"
    }
  }

  sealed abstract class Instruction
      extends IRNode
      with HasSerialized
      with Positional
      with HasAnnotations

  object BinaryOperator extends Enumeration {
    type BinaryOperator = Value
    val ADD, ADDC, SUB, OR, AND, XOR, MUL, SEQ, SLL, SRL, SLTU, SLTS, SGTU,
        SGTS, MUX = Value
  }
  case class BinaryArithmetic(
      operator: BinaryOperator.BinaryOperator,
      rd: Name,
      rs1: Name,
      rs2: Name,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Instruction {

    override def serialized: String =
      s"${operator.toString().toUpperCase()}\t${rd}, ${rs1}, ${rs2}"
  }
  case class CustomInstruction(
      func: Name,
      rd: Name,
      rs1: Name,
      rs2: Name,
      rs3: Name,
      rs4: Name,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Instruction {
    override def serialized: String =
      s"CUST ${rd}, [${func}], ${rs1}, ${rs2}, ${rs3}, ${rs4}"
  }

  case class LocalLoad(
      rd: Name,
      base: Name,
      offset: Constant,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Instruction {
    override def serialized: String =
      s"LLD ${rd}, ${offset}(${base})"
  }

  case class LocalStore(
      rs: Name,
      base: Name,
      offset: Constant,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Instruction {
    override def serialized: String = s"LST ${rs}, ${offset}(${base})"
  }

  case class GlobalLoad(
      rd: Name,
      base: Tuple4[Name, Name, Name, Name],
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Instruction {
    override def serialized: String = s"GLD ${rd}, [${base}]"
  }

  case class GlobalStore(
      rs: Name,
      base: Tuple4[Name, Name, Name, Name],
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Instruction {
    override def serialized: String = s"GST ${rs}, [${base}]"
  }

  case class SetValue(
      rd: Name,
      value: Constant,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Instruction {
    override def serialized: String = s"SET ${rd}, ${value}"
  }

  case class Send(
      rd: Name,
      rs: Name,
      dest_id: ProcessId,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Instruction {
    override def serialized: String =
      s"SEND ${rs}, [${dest_id}].${rd}"
  }

  case class Expect(
      ref: Name,
      got: Name,
      error_id: ExceptionId,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Instruction {
    override def serialized: String = s"EXPECT ${ref}, ${got}, [${error_id}]"
  }
}

