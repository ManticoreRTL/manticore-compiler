package manticore.assembly.annotations

final class Layout private (
    val name: String,
    val fields: Map[AssemblyAnnotationFields.FieldName, AnnotationValue]
) extends AssemblyAnnotation

object Layout extends AssemblyAnnotationParser {
  val name: String = "Layout".toUpperCase()

  def apply(fields: Map[String, AnnotationValue]) = {
    val parsed_fields = parse(fields)
    requiresField(AssemblyAnnotationFields.X, parsed_fields)
    requiresField(AssemblyAnnotationFields.Y, parsed_fields)
    new Layout(
      name,
      parsed_fields
    )
  }


}
