package manticore.assembly.annotations

final class Loc private (
    val name: String,
    val fields: Map[String, AnnotationValue]
) extends AssemblyAnnotation

object Loc {
  val name: String = "Loc".toUpperCase()

  def apply(fields: Map[String, String]) = {
    require(fields.contains("x"))
    require(fields.contains("y"))

    new Loc(
      name,
      Map(
        "x" -> IntValue(fields("x").toInt),
        "y" -> IntValue(fields("y").toInt)
      )
    )
  }

  def unapply(anno: Loc): Option[Map[String, AnnotationValue]] = Some(
    anno.fields
  )
}
