package manticore.assembly.annotations

final class Reg private (
    val name: String,
    val fields: Map[AssemblyAnnotationFields.FieldName, AnnotationValue]
) extends AssemblyAnnotation

object Reg extends AssemblyAnnotationParser {
  val name: String = "Reg".toUpperCase()

  def apply(fields: Map[String, AnnotationValue]) = {
    val parsed_fields = parse(fields)
    requiresField(AssemblyAnnotationFields.Id, parsed_fields)
    requiresField(AssemblyAnnotationFields.Type, parsed_fields)

    new Reg(
      name,
      parsed_fields
    )
  }


}
