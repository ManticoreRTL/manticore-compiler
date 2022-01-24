package manticore.assembly.annotations

final class Memblock private (
    val name: String,
    val fields: Map[AssemblyAnnotationFields.FieldName, AnnotationValue]
) extends AssemblyAnnotation {

  private def this(
      new_fields: Map[AssemblyAnnotationFields.FieldName, AnnotationValue]
  ) =
    this(Memblock.name, new_fields)

  def getIndex(): Option[Int] = getIntValue(AssemblyAnnotationFields.Index)
  def getBlock(): String =
    getStringValue(AssemblyAnnotationFields.Block).getOrElse {
      throw new NoSuchElementException(
        "bad Memblock annotation, missing block field"
      )
    }
  def getWidth(): Int = getIntValue(AssemblyAnnotationFields.Width).get
  def getCapacity(): Int = getIntValue(AssemblyAnnotationFields.Capacity).get
  def withIndex(ix: Int) = {
    require(ix >= 0)
    new Memblock(
      fields.updated(AssemblyAnnotationFields.Index, IntValue(ix))
    )
  }
}

object Memblock extends AssemblyAnnotationParser {
  val name: String = "Memblock".toUpperCase()

  def apply(fields: Map[String, AnnotationValue]) = {
    val parsed_fields = parse(fields)
    requiresField(AssemblyAnnotationFields.Block, parsed_fields)
    requiresField(AssemblyAnnotationFields.Capacity, parsed_fields)
    requiresField(AssemblyAnnotationFields.Width, parsed_fields)
    new Memblock(
      name,
      parsed_fields
    )
  }

}
