package manticore.assembly.annotations

final class Memblock private (
    val name: String,
    val fields: Map[AssemblyAnnotationFields.FieldName, AnnotationValue]
) extends AssemblyAnnotation

object Memblock extends AssemblyAnnotationParser {
  val name: String = "Memblock".toUpperCase()

  def apply(fields: Map[String, AnnotationValue]) = {
    val parsed_fields = parse(fields)
    requiresField(AssemblyAnnotationFields.Block, parsed_fields)
    requiresField(AssemblyAnnotationFields.Capacity, parsed_fields)

    new Memblock(
      name,
      parsed_fields
    )
  }


}
