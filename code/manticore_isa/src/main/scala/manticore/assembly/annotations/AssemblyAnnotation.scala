package manticore.assembly.annotations

import scala.util.parsing.input.Positional
import manticore.assembly.HasSerialized

sealed abstract class AnnotationValue

case class StringValue(value: String) extends AnnotationValue {
  override def toString() = value
}

case class IntValue(value: Int) extends AnnotationValue {
  override def toString() = value.toString()
}

case class BooleanValue(value: Boolean) extends AnnotationValue {
  override def toString() = value.toString()
}

/** Defines what an annotation looks like. Each annotation has a name and a set
  * of fields.
  */
trait AssemblyAnnotation extends Positional with HasSerialized {
  val name: String
  val fields: Map[String, AnnotationValue]

  def get(field: String): Option[AnnotationValue] = fields.get(field)
  def getIntValue(field: String): Option[Int] = fields.get(field) match {
    case Some(IntValue(v)) => Some(v)
    case _                     => None
  }
  def getStringValue(field: String): Option[String] = fields.get(field) match {
    case Some(StringValue(v)) => Some(v)
    case _ => None
  }
  def getBoolValue(field: String): Option[Boolean] = fields.get(field) match {
    case Some(BooleanValue(v)) => Some(v)
    case _ => None
  }
  def serialized: String = {
    if (fields.nonEmpty) {
      s"@${name} [" + {
        fields.map { case (k, v) => k + "=\"" + v + "\"" } mkString ","
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

/** Generates a typed annotation from a generic string-based annotation.
  */
object AssemblyAnnotationBuilder {
  def apply(name: String, fields: Map[String, String]): AssemblyAnnotation = {
    name.toUpperCase() match {
      case Loc.name      => Loc(fields)
      case Layout.name   => Layout(fields)
      case Memblock.name => Memblock(fields)
      case Program.name  => Program(fields)
      case Reg.name      => Reg(fields)
      case Track.name    => Track(fields)
    }
  }
}
