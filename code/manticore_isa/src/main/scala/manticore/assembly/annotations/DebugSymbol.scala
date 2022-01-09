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

  def getSymbol() = getStringValue(AssemblyAnnotationFields.Symbol).get
  def getIndex() = getIntValue(AssemblyAnnotationFields.Index).getOrElse(0)
  def getWidth() = getIntValue(AssemblyAnnotationFields.Width).getOrElse(1)

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
  def apply(symbol: String) = {
    new DebugSymbol(
      name, Map(AssemblyAnnotationFields.Symbol -> StringValue(symbol))
    )
  }
}
