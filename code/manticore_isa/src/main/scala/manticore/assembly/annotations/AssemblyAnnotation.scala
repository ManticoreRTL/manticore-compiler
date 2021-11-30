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

  def serialized: String = {
    if (fields.nonEmpty) {
      s"@${name} [" + {
        fields.map { case (k, v) => k + "=\"" + v + "\"" } mkString ","
      } + "]"
    } else {
      ""
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
