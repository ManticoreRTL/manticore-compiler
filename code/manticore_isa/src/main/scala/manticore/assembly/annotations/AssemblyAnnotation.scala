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

object AssemblyAnnotationBuilder {
  def apply(name: String, fields: Map[String, String]) = {
    name match {
      case Memblock.name => Memblock(fields)
    }
  }
}
