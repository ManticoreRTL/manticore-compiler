package manticore.compiler.assembly.annotations

final class Program private (
    val name: String,
    val fields: Map[AssemblyAnnotationFields.FieldName, AnnotationValue]
) extends AssemblyAnnotation

object Program extends AssemblyAnnotationParser {
  val name: String = "Program".toUpperCase()

  def apply(fields: Map[String, AnnotationValue]) = {
    val parsed_fields = parse(fields)
    requiresField(AssemblyAnnotationFields.Name, parsed_fields)

    new Program(
      name,
      parsed_fields
    )
  }


}
