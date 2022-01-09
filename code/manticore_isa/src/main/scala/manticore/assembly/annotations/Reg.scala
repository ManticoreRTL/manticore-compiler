package manticore.assembly.annotations

final class Reg private (
    val name: String,
    val fields: Map[AssemblyAnnotationFields.FieldName, AnnotationValue]
) extends AssemblyAnnotation {

  def this(new_fields: Map[AssemblyAnnotationFields.FieldName, AnnotationValue]) = this(Reg.name, new_fields)

  // create a new annotation with a sub-word index
  def withIndex(index: Int): Reg =
    new Reg(fields ++ Map(AssemblyAnnotationFields.Index -> IntValue(index)))

}

object Reg extends AssemblyAnnotationParser {
  val name: String = "Reg".toUpperCase()
  val Next: StringValue = StringValue("\\REG_NEXT")
  val Current: StringValue = StringValue("\\REG_CURR")
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
