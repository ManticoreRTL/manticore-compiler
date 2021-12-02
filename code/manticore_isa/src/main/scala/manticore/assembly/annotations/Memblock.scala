package manticore.assembly.annotations

final class Memblock private (
    val name: String,
    val fields: Map[String, AnnotationValue]
) extends AssemblyAnnotation

object Memblock {
  val name: String = "Memblock".toUpperCase()

  def apply(fields: Map[String, String]) = {
    require(fields.contains("block"), s"${name} annotation requires block")
    require(fields.contains("capacity"), s"${name} annotation requires capacity")

    new Memblock(
      name,
      Map(
        "block" -> StringValue(fields("block")),
        "capacity" -> IntValue(fields("capacity").toInt)
      )
    )
  }

  def unapply(anno: Memblock): Option[Map[String, AnnotationValue]] = Some(
    anno.fields
  )
}
