package manticore

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers._
import java.nio.file.Path
import java.nio.file.Files
import org.scalatest.Tag
import org.scalatest.fixture.TestDataFixture
import org.scalatest.flatspec.FixtureAnyFlatSpec
import org.scalatest.TestData
import org.scalatest.FixtureTestSuite
import org.scalatest.Outcome
import manticore.compiler.AssemblyContext
import java.io.PrintWriter

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

trait UnconstrainedTags {

  object Tags {
    // in sbt run the test using "testOnly -- -n WidthConversion"
    object WidthConversion extends Tag("WidthConversion")
  }

}

trait UnitFixtureTest extends FixtureAnyFlatSpec {


  val root_dir = Path
      .of(".")
      .resolve("test_run_dir")
      .resolve(this.getClass().getSimpleName())
  case class FixtureParam(test_dir: Path, ctx: AssemblyContext) {
    def dump(file_name: String, text: String): Path = {
      val fp = test_dir.resolve(file_name)
      val printer = new PrintWriter(fp.toFile())
      printer.print(text)
      printer.close()
      fp
    }
    def dump(file_name: String, content: Array[BigInt]): Path =
      dump(file_name, content mkString "\n" )

  }

  protected def withFixture(test: OneArgTest) = {
    val test_dir = root_dir.resolve(test.name.replace(" ", "_").replace("/", "_/_"))
    val ctx = AssemblyContext(dump_all = true, dump_dir = Some(test_dir.toFile), quiet = false)
    if (!test_dir.toFile().isDirectory()) Files.createDirectories(test_dir)
    else test_dir
    withFixture(test.toNoArgTest(FixtureParam(test_dir, ctx)))

  }

}


class TestTest extends UnitFixtureTest {

  behavior of "Test of Test"

  it should "have the fixture" in { () =>

    println("")
  }
}
