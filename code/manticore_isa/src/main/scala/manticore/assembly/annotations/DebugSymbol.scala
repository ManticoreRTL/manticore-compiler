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
            Map(
                "symbol" -> StringValue(fields("symbol")),
                "index" -> IntValue(fields.getOrElse("index", "-1").toInt)
            )
        )
    }
}