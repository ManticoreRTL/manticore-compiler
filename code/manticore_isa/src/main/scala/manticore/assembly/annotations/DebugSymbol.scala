package manticore.assembly.annotations

final class DebugSymbol private (
    val name: String,
    val fields: Map[AssemblyAnnotationFields.FieldName, AnnotationValue]
) extends AssemblyAnnotation {

  private def this(
      new_fields: Map[AssemblyAnnotationFields.FieldName, AnnotationValue]
  ) =
    this(DebugSymbol.name, new_fields)
  def withIndex(ix: Int) = new DebugSymbol(
    fields.updated(AssemblyAnnotationFields.Index, IntValue(ix))
  )
  def withWidth(w: Int) = new DebugSymbol(
    fields.updated(AssemblyAnnotationFields.Width, IntValue(w))
  )
}

object DebugSymbol extends AssemblyAnnotationParser {
  import AssemblyAnnotationFields._
  override val name: String = "DebugSymbol".toUpperCase()

  def apply(fields: Map[String, AnnotationValue]) = {
    val parsed_fields = parse(fields)
    requiresField(Symbol, parsed_fields)
    checkBindings(parsed_fields)
    new DebugSymbol(
      name,
      parsed_fields
    )
  }
}
