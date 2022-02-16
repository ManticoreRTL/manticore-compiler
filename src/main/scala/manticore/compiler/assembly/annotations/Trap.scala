package manticore.compiler.assembly.annotations

final class Trap private (
    val name: String,
    val fields: Map[AssemblyAnnotationFields.FieldName, AnnotationValue]
) extends AssemblyAnnotation {

  def getType(): String = getStringValue(AssemblyAnnotationFields.Type).get

}

object Trap extends AssemblyAnnotationParser {

  import AssemblyAnnotationFields._
  override val name: String = "Trap".toUpperCase()
  val Fail = StringValue("\\fail")
  val Stop = StringValue("\\stop")
  // type 0 is $stop and type 1 is failure
  def apply(fields: Map[String, AnnotationValue]) = {
    val parsed_fields = parse(fields)
    requiresField(Type, parsed_fields)
    // requiresField(File, parsed_fields)
    checkBindings(parsed_fields)
    new Trap(name, parsed_fields)
  }
}
