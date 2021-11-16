package manticore.assembly

import scala.collection.mutable.ArrayBuffer
import scala.util.parsing.input.Positional
import scala.collection.immutable.ListMap

trait ManticoreAssemblyIR {

  type Name
  type Constant
  type Variable <: Named with HasSerialized
  type CustomFunction <: HasSerialized
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
    def serializedAnnons(tabs: String = ""): String =
      s"${annons.map(x => tabs + x.serialized).mkString("\n")}\n"
  }

  case class AssemblyAnnotation(name: String, values: Map[String, String])
      extends Positional
      with HasSerialized {
    def getValue: Map[String, String] = values
    def getName: String = name
    def withElement(k: String, v: String) =
      AssemblyAnnotation(name, values ++ Map(k -> v))
    def serialized: String =
      if (values.nonEmpty)
        s"@${name} [" + { values.map { case (k, v) => k + "=\"" + v + "\"" } mkString "," } + "]"
      else
        ""

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
    override def serialized: String = s"${serializedAnnons("\t\t")}\t\t.func ${name} ${value.serialized};"
  }

  case class DefReg(
      variable: Variable,
      value: Option[Constant] = Option.empty[Constant],
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Declaration
      with HasSerialized {
    override def serialized: String =
      s"${serializedAnnons("\t\t")}\t\t${variable.serialized} ${value.getOrElse("")}; // @${pos}"
  }

  case class DefProgram(
      processes: Seq[DefProcess],
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Declaration
      with HasSerialized {
    override def serialized: String =
      s"${serializedAnnons("")}.prog: \n" + processes.foldLeft("") { case (str, p) =>
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

      s"${serializedAnnons("\t")}\t.proc ${id}:${(registers
        .map(_.serialized) ++ functions.map(_.serialized) ++ body
        .map(_.serialized)).mkString("\n")}"

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
      s"${serializedAnnons("\t\t")}\t\t${operator.toString().toUpperCase()}\t${rd}, ${rs1}, ${rs2}; //@${pos}"
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
      s"${serializedAnnons("\t\t")}\t\tCUST ${rd}, [${func}], ${rs1}, ${rs2}, ${rs3}, ${rs4}; //@${pos}"
  }

  case class LocalLoad(
      rd: Name,
      base: Name,
      offset: Constant,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Instruction {
    override def serialized: String =
      s"${serializedAnnons("\t\t")}\t\tLLD ${rd}, ${offset}(${base});"
  }

  case class LocalStore(
      rs: Name,
      base: Name,
      offset: Constant,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Instruction {
    override def serialized: String = s"${serializedAnnons("\t\t")}\t\tLST ${rs}, ${offset}[${base}]; //@${pos}"
  }

  case class GlobalLoad(
      rd: Name,
      base: Tuple4[Name, Name, Name, Name],
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Instruction {
    override def serialized: String =
      s"${serializedAnnons("\t\t")}\t\tGLD ${rd}, [${base._1}, ${base._2}, ${base._3}, ${base._4}]; //@${pos}"
  }

  case class GlobalStore(
      rs: Name,
      base: Tuple4[Name, Name, Name, Name],
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Instruction {
    override def serialized: String =
      s"${serializedAnnons("\t\t")}\t\tGST ${rs}, [${base._1}, ${base._2}, ${base._3}, ${base._4}]; //@${pos}"
  }

  case class SetValue(
      rd: Name,
      value: Constant,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Instruction {
    override def serialized: String = s"${serializedAnnons("\t\t")}\t\tSET ${rd}, ${value}; //@${pos}"
  }

  case class Send(
      rd: Name,
      rs: Name,
      dest_id: ProcessId,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Instruction {
    override def serialized: String =
      s"${serializedAnnons("\t\t")}\t\tSEND ${rs}, [${dest_id}], ${rd}; //@${pos}"
  }

  case class Expect(
      ref: Name,
      got: Name,
      error_id: ExceptionId,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Instruction {
    override def serialized: String = s"${serializedAnnons("\t\t")}\t\tEXPECT ${ref}, ${got}, [${error_id}]; //@${pos}"
  }
}
