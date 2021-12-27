package manticore.assembly.annotations

final class DebugSymbol private (
    val name: String,
    val fields: Map[String, AnnotationValue]
) extends AssemblyAnnotation

object DebugSymbol {
  val name: String = "DebugSymbol".toUpperCase()

  def apply(fields: Map[String, String]) = {
    require(fields.contains("symbol"), s"${name} annotation requires file")
    new DebugSymbol(
      name,
      Map("symbol" -> StringValue(fields("symbol")))
        ++ fields.get("index").map { x => "index" -> IntValue(x.toInt) }.toMap
        ++ fields.get("width").map { x => "width" -> IntValue(x.toInt) }.toMap
    )
  }
}
