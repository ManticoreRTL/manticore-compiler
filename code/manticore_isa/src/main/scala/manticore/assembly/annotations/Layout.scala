package manticore.assembly.annotations

final class Layout private (
    val name: String,
    val fields: Map[String, AnnotationValue]
) extends AssemblyAnnotation

object Layout {
  val name: String = "Layout".toUpperCase()

  def apply(fields: Map[String, String]) = {
    require(fields.contains("x"))
    require(fields.contains("y"))

    new Layout(
      name,
      Map(
        "x" -> IntValue(fields("x").toInt),
        "y" -> IntValue(fields("y").toInt)
      )
    )
  }

  def unapply(anno: Layout): Option[Map[String, AnnotationValue]] = Some(
    anno.fields
  )
}
