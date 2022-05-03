package manticore.compiler.assembly.annotations

import scala.util.parsing.input.Positional
import manticore.compiler.assembly.HasSerialized

sealed abstract class AnnotationValue

case class StringValue(value: String) extends AnnotationValue {
  override def toString() = s"\"${value}\""
}

case class IntValue(value: Int) extends AnnotationValue {
  override def toString() = value.toString()
}

case class BooleanValue(value: Boolean) extends AnnotationValue {
  override def toString() = value.toString()
}

/** All annotation field names
  */
object AssemblyAnnotationFields {
  sealed abstract class FieldName(val name: String) {
    override def toString(): String = name
  }
  trait BindsToString
  trait BindsToInt
  trait BindsToBoolean

  object Symbol extends FieldName("symbol") with BindsToString
  object Width extends FieldName("width") with BindsToInt
  object Index extends FieldName("index") with BindsToInt
  object X extends FieldName("x") with BindsToInt
  object Y extends FieldName("y") with BindsToInt
  object Block extends FieldName("block") with BindsToString
  object Capacity extends FieldName("capacity") with BindsToInt
  object File extends FieldName("file") with BindsToString
  object Count extends FieldName("count") with BindsToInt
  object Name extends FieldName("name") with BindsToString
  object Id extends FieldName("id") with BindsToString
  object Type extends FieldName("type") with BindsToString
  object Generated extends FieldName("generated") with BindsToBoolean


  def parse(name: String): Option[FieldName] = name match {
    case Symbol.name   => Some(Symbol)
    case Width.name    => Some(Width)
    case Index.name    => Some(Index)
    case X.name        => Some(X)
    case Y.name        => Some(Y)
    case Block.name    => Some(Block)
    case Capacity.name => Some(Capacity)
    case File.name     => Some(File)
    case Count.name    => Some(Count)
    case Name.name     => Some(Name)
    case Id.name       => Some(Id)
    case Type.name     => Some(Type)
    case _             => None
  }

}

/** Defines what an annotation looks like. Each annotation has a name and a set
  * of fields.
  */
trait AssemblyAnnotation extends Positional with HasSerialized {
  val name: String
  import AssemblyAnnotationFields.FieldName

  val fields: Map[FieldName, AnnotationValue]
  def get(field: FieldName): Option[AnnotationValue] =
    fields.get(field)
  def getIntValue(field: FieldName): Option[Int] =
    fields.get(field) match {
      case Some(IntValue(v)) => Some(v)
      case _                 => None
    }
  def getStringValue(field: FieldName): Option[String] =
    fields.get(field) match {
      case Some(StringValue(v)) => Some(v)
      case _                    => None
    }
  def getBoolValue(field: FieldName): Option[Boolean] =
    fields.get(field) match {
      case Some(BooleanValue(v)) => Some(v)
      case _                     => None
    }
  def serialized: String = {
    if (fields.nonEmpty) {
      s"@${name} [" + {
        fields.map { case (k, v) => s"${k} = ${v}" } mkString ","
      } + "]"
    } else {
      ""
    }
  }

  override def equals(x: Any): Boolean = {
    if (x == null) {
      false
    } else if (x.isInstanceOf[AssemblyAnnotation]) {
      val a = x.asInstanceOf[AssemblyAnnotation]
      a.name == this.name && a.fields == this.fields
    } else {
      false
    }
  }

}

trait AssemblyAnnotationParser {
  val name: String
  import AssemblyAnnotationFields.FieldName
  class AssemblyAnnotationParseError(val msg: String) extends Exception(msg)
  def parse(fields: Map[String, AnnotationValue]) = {
    val parsed_fields = fields.map { case (k, v) =>
      AssemblyAnnotationFields.parse(k) match {
        case Some(f) =>
          f -> v
        case None =>
          throw new AssemblyAnnotationParseError(
            s"Invalid ${name} annotation fields ${k}"
          )
      }
    }
    checkBindings(parsed_fields)
    parsed_fields
  }

  def requiresField(
      should_have: FieldName,
      fields: Map[FieldName, AnnotationValue]
  ): Unit =
    if (!fields.contains(should_have)) {
      throw new AssemblyAnnotationParseError(
        s"${name} annotation is missing field ${should_have}"
      )
    }

  def checkBindings(bindings: Map[FieldName, AnnotationValue]): Unit = {
    bindings.foreach {
      case (_: AssemblyAnnotationFields.BindsToInt, _: IntValue)       =>
      case (_: AssemblyAnnotationFields.BindsToString, _: StringValue) =>
      case (_: AssemblyAnnotationFields.BindsToBoolean, _: BooleanValue) =>
      case (x: FieldName, _) =>
        throw new AssemblyAnnotationParseError(
          s"Invalid ${name} annotation fields or values"
        )
    }
  }

}

/** Generates a typed annotation from a generic string-based annotation.
  */
object AssemblyAnnotationBuilder {
  def apply(
      name: String,
      fields: Map[String, AnnotationValue]
  ): AssemblyAnnotation = {

    name.toUpperCase() match {
      case Loc.name         => Loc(fields)
      case Layout.name      => Layout(fields)
      case Memblock.name    => Memblock(fields)
      case Program.name     => Program(fields)
      case Reg.name         => Reg(fields)
      case Sourceinfo.name  => Sourceinfo(fields)
      case Track.name       => Track(fields)
      case MemInit.name     => MemInit(fields)
      case DebugSymbol.name => DebugSymbol(fields)
      case Trap.name        => Trap(fields)
      case Echo.name        => Echo(fields)
    }
  }
}
