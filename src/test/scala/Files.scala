package manticore.compiler


import java.io.File
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path

sealed trait FileDescriptor {
  val p: Path
}

case class WithPath(p: Path) extends FileDescriptor

case class WithFile(f: File) extends FileDescriptor {
  override val p: Path = f.toPath()
}
case class WithResource(s: String) extends FileDescriptor {
  override lazy val p: Path = {
    val content = scala.io.Source.fromResource(s).mkString
    val tmpFile = Files.createTempDirectory("resource").resolve(s.split("/").last)
    val writer  = new PrintWriter(tmpFile.toFile())
    writer.print(content)
    writer.flush()
    writer.close()
    tmpFile
  }
}

sealed trait InlineFile extends FileDescriptor {
  val extension: String
  val content: String

  override lazy val p: Path = {
    val tmpFile = Files.createTempDirectory("resource").resolve(s"content.$extension")
    val writer  = new PrintWriter(tmpFile.toFile())
    writer.print(content)
    writer.flush()
    writer.close()
    tmpFile
  }
}

case class WithInlineVerilog(content: String) extends InlineFile {
  val extension: String = "v"
}
