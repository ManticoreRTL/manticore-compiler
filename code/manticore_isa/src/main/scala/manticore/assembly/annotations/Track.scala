package manticore.assembly.annotations

final class Track private (
    val name: String,
    val fields: Map[String, AnnotationValue]
) extends AssemblyAnnotation

object Track {
  val name: String = "Track".toUpperCase()

  def apply(fields: Map[String, String]) = {
    require(fields.contains("name"))

    new Track(
      name,
      Map(
        "name" -> StringValue(fields("name"))
      )
    )
  }

  def unapply(anno: Track): Option[Map[String, AnnotationValue]] = Some(
    anno.fields
  )
}
