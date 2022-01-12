package manticore.assembly.annotations

final class Echo private (
    val name: String,
    val fields: Map[AssemblyAnnotationFields.FieldName, AnnotationValue]
) extends AssemblyAnnotation

object Echo extends AssemblyAnnotationParser {
  val name: String = "Echo".toUpperCase()

  def apply(fields: Map[String, AnnotationValue]) = {
    if (fields.nonEmpty) {
        throw new AssemblyAnnotationParseError("ECHO does not take key-values")
    }
    new Echo(
      name,
      Map()
    )
  }


}
