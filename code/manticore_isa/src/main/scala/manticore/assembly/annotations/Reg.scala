package manticore.assembly.annotations

final class Reg private (
    val name: String,
    val fields: Map[String, AnnotationValue]
) extends AssemblyAnnotation

object Reg {
  val name: String = "Reg".toUpperCase()

  def apply(fields: Map[String, String]) = {
    require(fields.contains("id"))
    require(fields.contains("type"))

    new Reg(
      name,
      Map(
        "id" -> StringValue(fields("id")),
        "type" -> StringValue(fields("type"))
      )
    )
  }

  def unapply(anno: Reg): Option[Map[String, AnnotationValue]] = Some(
    anno.fields
  )
}
