package manticore.compiler.assembly.annotations

final class Track private (
    val name: String,
    val fields: Map[AssemblyAnnotationFields.FieldName, AnnotationValue]
) extends AssemblyAnnotation

object Track extends AssemblyAnnotationParser {
  val name: String = "Track".toUpperCase()

  def apply(fields: Map[String, AnnotationValue]) = {
    val parsed_fields = parse(fields)
    requiresField(AssemblyAnnotationFields.Name, parsed_fields)
    new Track(
      name,
      parsed_fields
    )
  }


}
