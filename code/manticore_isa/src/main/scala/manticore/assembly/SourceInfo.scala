package manticore.assembly

import scala.util.parsing.input.Positional

/** Simple case class to represent a point in a file
  * @param line
  *   line number
  * @param col
  *   column number
  */
case class LineInfo(line: Int, col: Int) {
  def serialized: String = s"${line}:${col}"
}

/** Source file information, used by each assembly instruction if it comes from
  * a source file
  *
  * @param file_name
  *   file name
  * @param from_line
  *   row/col number of the starting position
  * @param to_line
  *   row/col number of the ending position
  * @param file_type
  *   type of the file, e.g., verilog or firrtl
  */
sealed abstract class SourceInfo extends Positional {
  def serialized: String
}

case class SourceFileInfo(
    file_name: String,
    file_type: SourceType.SourceType
) extends SourceInfo {
  override def serialized: String =
    s"@[${file_name}][${file_type}]"
}

object NoInfo extends SourceInfo {
  override def serialized: String = ""
}

object SourceType extends Enumeration {
  type SourceType = Value
  val Virtual, Firrtl, Chisel, Verilog, Vhdl, Scala = Value
}
