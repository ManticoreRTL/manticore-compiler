package manticore.assembly.annotations

final class Memblock private (
    val name: String,
    val fields: Map[String, AnnotationValue]
) extends AssemblyAnnotation

object Memblock {
  val name: String = "Memblock"

  def apply(fields: Map[String, String]) = {
    require(fields.contains("block"))

    new Memblock(
      name,
      Map(
        "block" -> StringValue(fields("block"))
      )
    )
  }

  def unapply(mb: Memblock): Option[Map[String, AnnotationValue]] = Some(
    mb.fields
  )
}
