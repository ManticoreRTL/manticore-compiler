package manticore


import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers._


trait UnitTestMatchers extends should.Matchers
abstract class UnitTest extends AnyFlatSpec with UnitTestMatchers