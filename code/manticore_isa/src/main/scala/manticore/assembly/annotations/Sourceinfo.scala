package manticore.assembly.annotations

final class Sourceinfo private (
    val name: String,
    val fields: Map[String, AnnotationValue]
) extends AssemblyAnnotation

object Sourceinfo {
  val name: String = "Sourceinfo".toUpperCase()

  def apply(fields: Map[String, String]) = {
    require(fields.contains("file"))

    new Sourceinfo(
      name,
      Map(
        "file" -> StringValue(fields("file"))
      )
    )
  }

  def unapply(anno: Sourceinfo): Option[Map[String, AnnotationValue]] = Some(
    anno.fields
  )
}
