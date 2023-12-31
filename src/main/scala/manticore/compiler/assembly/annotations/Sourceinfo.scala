package manticore.compiler.assembly.annotations

final class Sourceinfo private (
    val name: String,
    val fields: Map[AssemblyAnnotationFields.FieldName, AnnotationValue]
) extends AssemblyAnnotation {

  def getFile(): String = getStringValue(AssemblyAnnotationFields.File).get
}

object Sourceinfo extends AssemblyAnnotationParser{
  val name: String = "Sourceinfo".toUpperCase()

  def apply(fields: Map[String, AnnotationValue]) = {
    val parsed_fields = parse(fields)
    requiresField(AssemblyAnnotationFields.File, parsed_fields)

    new Sourceinfo(
      name,
      parsed_fields
    )
  }


}
