package manticore.assembly.annotations

final class DebugSymbol private (
    val name: String,
    val fields: Map[String, AnnotationValue]
) extends AssemblyAnnotation {

  private def this(new_fields: Map[String, AnnotationValue]) =
    this(DebugSymbol.name, new_fields)
  def withIndex(ix: Int) = new DebugSymbol(
    fields.updated(DebugSymbol.Index, IntValue(ix))
  )
}

object DebugSymbol {
  val name: String = "DebugSymbol".toUpperCase()
  val Symbol: String = "symbol"
  val Index: String = "index"
  val Width: String = "width"
  def apply(fields: Map[String, String]) = {
    require(fields.contains("symbol"), s"${name} annotation requires file")
    new DebugSymbol(
      name,
      Map(Symbol -> StringValue(fields(Symbol)))
        ++ fields.get(Index).map { x => Index -> IntValue(x.toInt) }.toMap
        ++ fields.get(Width).map { x => Width -> IntValue(x.toInt) }.toMap
    )
  }
}
