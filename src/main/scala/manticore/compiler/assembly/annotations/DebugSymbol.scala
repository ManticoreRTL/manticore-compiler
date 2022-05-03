package manticore.compiler.assembly.annotations

final class DebugSymbol private (
    val name: String,
    val fields: Map[AssemblyAnnotationFields.FieldName, AnnotationValue]
) extends AssemblyAnnotation {

  private def this(
      new_fields: Map[AssemblyAnnotationFields.FieldName, AnnotationValue]
  ) =
    this(DebugSymbol.name, new_fields)
  def withIndex(ix: Int) = {
    require(ix >= 0)
    new DebugSymbol(
      fields.updated(AssemblyAnnotationFields.Index, IntValue(ix))
    )
  }

  def withWidth(w: Int) = new DebugSymbol(
    fields.updated(AssemblyAnnotationFields.Width, IntValue(w))
  )
  def withGenerated(b: Boolean) = new DebugSymbol(
    fields.updated(AssemblyAnnotationFields.Generated, BooleanValue(b))
  )
  def withCount(l: Int) = new DebugSymbol(
    fields.updated(AssemblyAnnotationFields.Count, IntValue(l))
  )

  def getSymbol() = getStringValue(AssemblyAnnotationFields.Symbol).get
  def getIndex() = getIntValue(AssemblyAnnotationFields.Index)
  def getWidth() = getIntValue(AssemblyAnnotationFields.Width)
  def isGenerated() = getBoolValue(AssemblyAnnotationFields.Generated)
  def getCount() = getIntValue(AssemblyAnnotationFields.Count)

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
      name,
      Map(AssemblyAnnotationFields.Symbol -> StringValue(symbol))
    )
  }
}
