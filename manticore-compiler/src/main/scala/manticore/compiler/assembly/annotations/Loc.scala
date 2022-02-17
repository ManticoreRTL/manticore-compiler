package manticore.compiler.assembly.annotations

final class Loc private (
    val name: String,
    val fields: Map[AssemblyAnnotationFields.FieldName, AnnotationValue]
) extends AssemblyAnnotation {


  def getX(): Int = getIntValue(AssemblyAnnotationFields.X).get
  def getY(): Int = getIntValue(AssemblyAnnotationFields.Y).get
}

object Loc extends AssemblyAnnotationParser {
  val name: String = "Loc".toUpperCase()

  def apply(fields: Map[String, AnnotationValue]) = {
    val parsed_fields = parse(fields)
    requiresField(AssemblyAnnotationFields.X, parsed_fields)
    requiresField(AssemblyAnnotationFields.Y, parsed_fields)

    new Loc(
      name,
      parsed_fields
    )
  }


}
