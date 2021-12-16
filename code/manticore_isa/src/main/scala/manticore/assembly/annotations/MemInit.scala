package manticore.assembly.annotations

final class MemInit private (
    val name: String,
    val fields: Map[String, AnnotationValue]
) extends AssemblyAnnotation

object MemInit {
    val name: String = "MemInit".toUpperCase()

    def apply(fields: Map[String, String]) = {
        require(fields.contains("file"), s"${name} annotation requires file")
        require(fields.contains("size"), s"${name} annotation requires size")
        require(fields.contains("width"), s"${name} annotation requires width")
        new MemInit(
            name,
            Map(
                "file" -> StringValue(fields("file")),
                "size" -> IntValue(fields("size").toInt),
                "width" -> IntValue(fields("width").toInt)
            )
        )
    }
}