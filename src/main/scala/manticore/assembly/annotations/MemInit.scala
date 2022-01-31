package manticore.assembly.annotations

final class MemInit private (
    val name: String,
    val fields: Map[AssemblyAnnotationFields.FieldName, AnnotationValue]
) extends AssemblyAnnotation {

  def getFileName(): String = getStringValue(AssemblyAnnotationFields.File).get
  def getWidth(): Int = getIntValue(AssemblyAnnotationFields.Width).get
  def getCount(): Int = getIntValue(AssemblyAnnotationFields.Width).get
}

object MemInit extends AssemblyAnnotationParser {
  val name: String = "MemInit".toUpperCase()

  def apply(fields: Map[String, AnnotationValue]) = {
    val parsed_fields = parse(fields)

    requiresField(AssemblyAnnotationFields.File, parsed_fields)
    requiresField(AssemblyAnnotationFields.Count, parsed_fields)
    requiresField(AssemblyAnnotationFields.Width, parsed_fields)

    new MemInit(
      name,
      parsed_fields
    )
  }
}
