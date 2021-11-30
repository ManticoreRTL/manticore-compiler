package manticore.assembly.annotations

final class Program private (
    val name: String,
    val fields: Map[String, AnnotationValue]
) extends AssemblyAnnotation

object Program {
  val name: String = "Program".toUpperCase()

  def apply(fields: Map[String, String]) = {
    require(fields.contains("name"))

    new Program(
      name,
      Map(
        "name" -> StringValue(fields("name"))
      )
    )
  }

  def unapply(anno: Program): Option[Map[String, AnnotationValue]] = Some(
    anno.fields
  )
}
