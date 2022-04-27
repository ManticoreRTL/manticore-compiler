package manticore.compiler.assembly

import scala.collection.mutable.ArrayBuffer
import scala.util.parsing.input.Positional
import scala.collection.immutable.ListMap
import manticore.compiler.assembly.levels.HasVariableType

import manticore.compiler.assembly.annotations.AssemblyAnnotation
import manticore.compiler.assembly.annotations.AnnotationValue
import manticore.compiler.assembly.annotations.AssemblyAnnotationFields.FieldName
import javax.xml.crypto.Data

/** Base classes for the various IR flavors.
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */

/** Supported arithmetic opcodes
  */
object BinaryOperator extends Enumeration {
  type BinaryOperator = Value
  val ADD, SUB, MUL, AND, OR, XOR, SLL, SRL, SRA, SEQ, SLTS, MUX, ADDC = Value
}

trait HasSerialized {
  def serialized: String
}

trait HasWidth {
  def width: Int
}

/** Abstract IR flavor, any deriving IR flavor should be defined as an object
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
  type Variable <: HasVariableType with Named[
    Variable
  ] with HasSerialized with HasWidth // type defining Variables, should include variable type information
  type CustomFunction <: HasSerialized // type defining custom function, e.g., Seq[UInt16]
  type ProcessId // type defining a process identifier, e.g., String
  type ExceptionId // type defining an exception identifier, e.g., String

  type Label

  trait Named[T] {
    val name: Name
    def withName(new_name: Name): T
  }

  trait HasAnnotations {
    val annons: Seq[AssemblyAnnotation]
    protected def serializedAnnons(tabs: String = ""): String =
      if (annons.nonEmpty)
        s"${annons.map(x => tabs + x.serialized).mkString("\n")}\n"
      else
        ""
    def findAnnotationValue(
        name: String,
        key: FieldName
    ): Option[AnnotationValue] = {
      val found = annons.find(_.name == name)
      found match {
        case None       => None
        case Some(anno) => anno.get(key)
      }
    }
    def findAnnotation(name: String): Option[AssemblyAnnotation] =
      annons.find(_.name == name)

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
      s"${serializedAnnons("\t\t")}\t\t${variable.serialized} ${value.getOrElse("")} // @${pos}"

  }

  case class DefLabelGroup(
      memory: Name,
      indexer: Seq[(Constant, Label)],
      default: Option[Label],
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Declaration
      with HasSerialized {
    override def serialized: String =
      s"${serializedAnnons("\t\t")}\t\t.labelgroup ${memory}\n" +
        indexer.map { case (index, label) =>
          s"\t\t\t${index} -> ${label}"
         }.mkString ("\n") + (if (default.nonEmpty)
                               s"\t\t\t _ -> ${default.get}"
                             else "")

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
      labels: Seq[DefLabelGroup] = Seq(),
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends Declaration
      with HasSerialized {
    override def serialized: String = {

      s"${serializedAnnons("\t")}\t.proc ${id}:\n${(registers
        .map(_.serialized) ++ functions.map(_.serialized) ++
        labels.map(_.serialized) ++ body.map(_.serialized)).mkString("\n")}"

    }
  }

  // base instruction class
  sealed trait Instruction
      extends IRNode
      with HasSerialized
      with Positional
      with HasAnnotations {
    override def serialized: String =
      s"${serializedAnnons("\t\t")}\t\t${toString}; //@${pos}"
  }

  sealed trait DataInstruction extends Instruction
  sealed trait ControlInstruction extends Instruction
  sealed trait PrivilegedInstruction extends Instruction
  sealed trait SynchronizationInstruction extends Instruction


  /** A parallel multiplexer
    *
    * @param rd
    * @param choices
    * @param default
    * @param conditions
    * @param annons
    */
  case class ParMuxCase(condition: Name, choice: Name)
  case class ParMux(
      rd: Name,
      choices: Seq[ParMuxCase],
      default: Name,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends ControlInstruction {

    override def toString: String =
      s"PARMUX ${rd}, ${choices map { case ParMuxCase(cond, ch) =>
        s"${cond} ? ${ch}"
      } mkString (", ")}, ${default}"

  }

  case class JumpCase(label: Label, block: Seq[DataInstruction])
  // a Phi node for selecting results and convergence points, does not really
  // belong to the instruction type hierarchy since right now Phi nodes are
  // strictly used within JumpTable nodes so there is no reason to treat them
  // as real instructions
  case class Phi(rd: Name, rss: Seq[(Label, Name)]) {
    override def toString: String =
      s"PHI $rd, ${rss.map { case (l, n) => s"$l: $n" }.mkString(", ")}"
  }

  /** A Hyper instruction that can compute many results and is identified by a
    * unique label
    *
    * @param target
    *   the jump target, should be a label corresponding to one of the blocks
    * @param results
    *   A sequence of "phi" functions that assign a register to multiple labeled
    *   results
    * @param blocks
    *   blocks of each case branch
    * @param dslot
    *   delay slot instruction
    * @param annons
    */
  case class JumpTable(
      target: Name,
      results: Seq[Phi],
      blocks: Seq[JumpCase],
      dslot: Seq[DataInstruction] = Seq(),
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends ControlInstruction {

    override def toString: String = {
      s"SWITCH ${target}:\n" +
        blocks
          .map { case JumpCase(label, body) =>
            s"\t\t\tCASE ${label}:\n" +
              body
                .map { inst => s"\t\t\t\t${inst}; // ${inst.pos}" }
                .mkString("\n")
          }
          .mkString("\n") + "\n" +
        results.map { case phi => s"\t\t\t$phi;" }.mkString("\n")

    }

  }

  // A pseudo LocalLoad instruction solely used by the address lookup computation
  // it can be replaced with a LocalLoad is the base given is a constant value
  // which would be the case since the base would be memory pointer which is
  // a constant after memory allocation
  case class Lookup(
      rd: Name,
      index: Name,
      base: Name,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends ControlInstruction {
    override def toString: String =
      s"LOOKUP ${rd}, ${base}[${index}]"
  }
  // arithmetic operations see BinaryOperators
  case class BinaryArithmetic(
      operator: BinaryOperator.BinaryOperator,
      rd: Name,
      rs1: Name,
      rs2: Name,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends DataInstruction {

    override def toString: String = s"${operator
      .toString()
      .toUpperCase()}\t${rd}, ${rs1}, ${rs2}"

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
  ) extends DataInstruction {
    override def toString: String =
      s"CUST ${rd}, [${func}], ${rs1}, ${rs2}, ${rs3}, ${rs4}"

  }

  // LocalLoad from the local scratchpad
  case class LocalLoad(
      rd: Name,
      base: Name,
      offset: Constant,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends DataInstruction {
    override def toString: String = s"LLD ${rd}, ${base}[${offset}]"

  }

  // Store to the local scratchpad
  case class LocalStore(
      rs: Name,
      base: Name,
      offset: Constant,
      predicate: Option[Name],
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends DataInstruction {
    override def toString: String = s"LST ${rs}, ${base}[${offset}] ${predicate
      .map(", " + _.toString())
      .getOrElse("")}"
  }

  // Load from global memory (DRAM)
  case class GlobalLoad(
      rd: Name,
      base: Tuple3[Name, Name, Name],
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends DataInstruction
      with PrivilegedInstruction {
    override def toString: String =
      s"GLD ${rd}, [${base._1}, ${base._2}, ${base._3}]"

  }

  // Store to global memory (DRAM)
  case class GlobalStore(
      rs: Name,
      base: Tuple3[Name, Name, Name],
      predicate: Option[Name],
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends DataInstruction
      with PrivilegedInstruction {
    override def toString: String =
      s"GST ${rs}, [${base._1}, ${base._2}, ${base._3}]${predicate
        .map { x => ", " + x.toString() }
        .getOrElse("")}"

  }

  // Set a register value
  case class SetValue(
      rd: Name,
      value: Constant,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends DataInstruction {
    override def toString: String =
      s"SET ${rd}, ${value}"
  }

  // send a register to a process
  case class Send(
      rd: Name, // register name in the destination process
      rs: Name, // register name in the source process
      dest_id: ProcessId, // destination process id
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends SynchronizationInstruction {
    override def toString: String = s"SEND ${rd}, [${dest_id}], ${rs}"

  }

  // a place holder for Send in the destination. This instruction is never
  // translated to machine code.
  // (i.e., )
  case class Recv(
      rd: Name,
      rs: Name,
      source_id: ProcessId,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends SynchronizationInstruction {
    override def toString: String = s"RECV ${rd}, [${source_id}], ${rs}"
  }

  // value assertion
  case class Expect(
      ref: Name,
      got: Name,
      error_id: ExceptionId,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends ControlInstruction
      with PrivilegedInstruction {
    override def toString: String = s"EXPECT ${ref}, ${got}, [${error_id}]"

  }

  case class Predicate(
      rs: Name,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends DataInstruction {
    override def toString: String = s"PREDICATE ${rs}"

  }

  case class Mux(
      rd: Name,
      sel: Name,
      rfalse: Name,
      rtrue: Name,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends DataInstruction {

    override def toString: String = s"MUX ${rd}, ${sel}, ${rfalse}, ${rtrue}"

  }

  case object Nop extends Instruction {
    val annons: Seq[AssemblyAnnotation] = Seq()
    override def toString: String = s"\t\tNOP;"
  }

  case class AddC(
      rd: Name,
      co: Name,
      rs1: Name,
      rs2: Name,
      ci: Name,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends DataInstruction {

    override def toString: String =
      s"ADDCARRY ${rd}, ${co}, ${rs1}, ${rs2}, ${ci}"

  }

  case class ClearCarry(
      carry: Name,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends DataInstruction {
    override def toString(): String =
      s"CLEARCARRY ${carry}"
  }

  case class SetCarry(
      carry: Name,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends DataInstruction {
    override def toString(): String =
      s"SETCARRY ${carry}"

  }

  case class PadZero(
      rd: Name,
      rs: Name,
      width: Constant,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends DataInstruction {
    override def toString: String =
      s"PADZERO ${rd}, ${rs}, ${width}"

  }

  case class Mov(
      rd: Name,
      rs: Name,
      annons: Seq[AssemblyAnnotation] = Seq()
  ) extends DataInstruction {
    override def toString: String =
      s"MOV ${rd}, ${rs}"

  }

}
