package manticore.compiler.assembly.annotations

import manticore.compiler.CompilationFailureException

final class MemInit private (
    val name: String,
    val fields: Map[AssemblyAnnotationFields.FieldName, AnnotationValue]
) extends AssemblyAnnotation {

  def getFileName(): String = getStringValue(AssemblyAnnotationFields.File).get
  def getWidth(): Int = getIntValue(AssemblyAnnotationFields.Width).get
  def getCount(): Int = getIntValue(AssemblyAnnotationFields.Count).get

  def readFile() = {

    import java.nio.file.Path

    val filePath = Path.of(getFileName())
    val fileExt =
      filePath.getFileName().getFileName().toString().split("\\.").last

    val radix = fileExt match {
      case "bin"                   => 2
      case "hex"                   => 16
      case "dat" | "txt" | "data" => 10
      case _ =>
        throw new CompilationFailureException(
          s"Failed reading file ${filePath.toAbsolutePath()}. " +
            s"Only can accept binary (.bin), hexadecimal (.hex) or decimal (.dat or .txt or .data) memory initialization files!"
        )
    }

    try {
      scala.io.Source
        .fromFile(filePath.toString())
        .getLines()
        .slice(0, getCount())
        .map { line =>
          BigInt(line, radix)
        }
    } catch {
      case e: Exception =>
        throw new CompilationFailureException(
          s"Could not read file ${filePath.toString()}:\n ${e.getMessage()}"
        )
    }

  }
}

object MemInit extends AssemblyAnnotationParser {
  val name: String = "MemInit".toUpperCase()

  def apply(fields: Map[String, AnnotationValue]) = {
    val parsed_fields = parse(fields)

    requiresField(AssemblyAnnotationFields.File, parsed_fields)
    requiresField(AssemblyAnnotationFields.Count, parsed_fields)
    requiresField(AssemblyAnnotationFields.Width, parsed_fields)

    new MemInit(
      name,
      parsed_fields
    )
  }

}
