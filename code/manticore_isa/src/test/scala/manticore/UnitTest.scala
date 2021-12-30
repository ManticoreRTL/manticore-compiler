package manticore

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers._
import java.nio.file.Path
import java.nio.file.Files

trait UnitTestMatchers extends should.Matchers
trait UnitTest extends AnyFlatSpec with UnitTestMatchers {

  private val dump_dir: Path = Path
    .of(".")
    .resolve("test_dump_dir")
    .resolve(this.getClass().getSimpleName())

  def createDumpDirectory(): Path =
    if (!dump_dir.toFile().isDirectory()) Files.createDirectories(dump_dir)
    else dump_dir
}
