package manticore.assembly

import scala.collection.mutable.ArrayBuffer
import scala.util.parsing.input.Positional
import scala.collection.immutable.ListMap

/** Base classes for the various IR flavors.
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */

/** Supported arithmetic opcodes
  */
object BinaryOperator extends Enumeration {
  type BinaryOperator = Value
  val ADD, ADDC, SUB, OR, AND, XOR, MUL, SEQ, SLL, SRL, SLTU, SLTS, SGTU, SGTS,
      MUX = Value
}

/** An assembly annotation
  *
  * @param name
  *   name of the annotation (given by @NAME in the code)
  * @param values
  *   key-value pairs for the annotation
  */
case class AssemblyAnnotation(name: String, values: Map[String, String])
    extends Positional
    with HasSerialized {
  def getValue: Map[String, String] = values
  def getName: String = name
  def withElement(k: String, v: String) =
    AssemblyAnnotation(name, values ++ Map(k -> v))
  def serialized: String =
    if (values.nonEmpty)
      s"@${name} [" + {
        values.map { case (k, v) => k + "=\"" + v + "\"" } mkString ","
      } + "]"
    else
      ""

}

trait HasSerialized {
  def serialized: String
}

/** Abastract IR flavor, any deriving IR flavor should be defined as an object
  * that defines the unbound types [[Name]], [[Constant]], [[Variable]],
  * [[CustomFunction]], [[ProcessId]], [[ExceptionId]].
  *
  * Each IR node has the [[HasSerialized]] trait, meaning that by calling
  * [[serialized]] a textual form of the program is returned. Note that to
  * ensure a serialized IR can be parsed again, [[Name]], [[Constant]],
  * [[ProcessId]], [[ExceptionId]] should properly define [[toString]] while
  * [[Variable]] and [[CustomFunction]] should define [[serialized]] should
  * define [[serialized]].
  */
trait ManticoreAssemblyIR {

  type Name // type defining names, e.g., String
  type Constant // type defining constants, e.g., UInt16 or BigInt
  type Variable <: Named with HasSerialized // type defining Variables, should include variable type information
  type CustomFunction <: HasSerialized // type defining custom function, e.g., Seq[UInt16]
  type ProcessId // type defining a process identifier, e.g., String
  type ExceptionId // type defining an exception identifier, e.g., String
  // type SwizzleCode

  trait Named {
    val name: Name
  }

  trait HasAnnotations {
    val annons: Seq[AssemblyAnnotation]
    def serializedAnnons(tabs: String = ""): String =
      if (annons.nonEmpty)
        s"${annons.map(x => tabs + x.serialized).mkString("\n")}\n"
      else
        ""
  }

  // base IRNode
  sealed abstract class IRNode

  // IRNode for declarations
  sealed abstract class Declaration
      extends IRNode
      with HasAnnotations
      with Positional

  // Function def/decl
  case class DefFunc(
      name: Name,
      value: CustomFunction,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Declaration
      with HasSerialized {
    override def serialized: String =
      s"${serializedAnnons("\t\t")}\t\t.func ${name} ${value.serialized};"
  }

  // Register/wire/const/input/output/.. decl and def
  case class DefReg(
      variable: Variable,
      value: Option[Constant] = Option.empty[Constant],
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Declaration
      with HasSerialized {
    override def serialized: String =
      s"${serializedAnnons("\t\t")}\t\t${variable.serialized} ${value.getOrElse("")}; // @${pos}"
  }

  // program definition, single one
  case class DefProgram(
      processes: Seq[DefProcess],
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Declaration
      with HasSerialized {
    override def serialized: String =
      s"${serializedAnnons("")}.prog: \n" + processes.foldLeft("") {
        case (str, p) =>
          str + p.serialized + "\n"
      }
  }

  // process definition, can be multiple
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

  // base instruction class
  sealed abstract class Instruction
      extends IRNode
      with HasSerialized
      with Positional
      with HasAnnotations

  // arithmetic operations see BinaryOperators
  case class BinaryArithmetic(
      operator: BinaryOperator.BinaryOperator,
      rd: Name,
      rs1: Name,
      rs2: Name,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Instruction {

    override def serialized: String =
      s"${serializedAnnons("\t\t")}\t\t${operator
        .toString()
        .toUpperCase()}\t${rd}, ${rs1}, ${rs2}; //@${pos}"
  }

  // Custom instruction
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

  // LocalLoad from the local scratchpad
  case class LocalLoad(
      rd: Name,
      base: Name,
      offset: Constant,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Instruction {
    override def serialized: String =
      s"${serializedAnnons("\t\t")}\t\tLLD ${rd}, ${offset}(${base});"
  }

  // Store to the local scrathpad
  case class LocalStore(
      rs: Name,
      base: Name,
      offset: Constant,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Instruction {
    override def serialized: String =
      s"${serializedAnnons("\t\t")}\t\tLST ${rs}, ${offset}[${base}]; //@${pos}"
  }

  // Load from global memory (DRAM)
  case class GlobalLoad(
      rd: Name,
      base: Tuple4[Name, Name, Name, Name],
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Instruction {
    override def serialized: String =
      s"${serializedAnnons("\t\t")}\t\tGLD ${rd}, [${base._1}, ${base._2}, ${base._3}, ${base._4}]; //@${pos}"
  }

  // Store to global memory (DRAM)
  case class GlobalStore(
      rs: Name,
      base: Tuple4[Name, Name, Name, Name],
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Instruction {
    override def serialized: String =
      s"${serializedAnnons("\t\t")}\t\tGST ${rs}, [${base._1}, ${base._2}, ${base._3}, ${base._4}]; //@${pos}"
  }

  // Set a register value
  case class SetValue(
      rd: Name,
      value: Constant,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Instruction {
    override def serialized: String =
      s"${serializedAnnons("\t\t")}\t\tSET ${rd}, ${value}; //@${pos}"
  }

  // send a register to a process
  case class Send(
      rd: Name,
      rs: Name,
      dest_id: ProcessId,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Instruction {
    override def serialized: String =
      s"${serializedAnnons("\t\t")}\t\tSEND ${rs}, [${dest_id}], ${rd}; //@${pos}"
  }

  // value assertion
  case class Expect(
      ref: Name,
      got: Name,
      error_id: ExceptionId,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Instruction {
    override def serialized: String =
      s"${serializedAnnons("\t\t")}\t\tEXPECT ${ref}, ${got}, [${error_id}]; //@${pos}"
  }
}
