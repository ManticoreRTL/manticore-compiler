package manticore.assembly.parser

import scala.util.parsing.combinator.Parsers
import scala.collection.mutable
import scala.language.implicitConversions

trait AssemblyTokenParser extends Parsers {

  type Tokens <: AssemblyTokens

  val lexical: AssemblyLexical

  type Elem = lexical.Token

  import lexical._

  protected val keywordCache = mutable.HashMap[String, Parser[Token]]()

  /** A parser which matches a single keyword token.
    *
    * @param chars
    *   The character string making up the matched keyword.
    * @return
    *   a `Parser` that matches the given string
    */
  implicit def keyword(chars: String): Parser[Token] =
    keywordCache.getOrElseUpdate(chars, accept(Keyword(chars)))

  /** A parser which matches a numeric literal */
  def decLit: Parser[Token] =
    elem("number", _.isInstanceOf[DeclLit])

  def hexLit: Parser[Token] =
    elem("hex", _.isInstanceOf[HexLiteral])

  def binLit: Parser[Token] =
    elem("binary", _.isInstanceOf[BinLiteral])

  /** A parser which matches a string literal */
  def stringLit: Parser[Token] =
    elem("string literal", _.isInstanceOf[StringLit])

  /** A parser which matches an identifier */
  def ident: Parser[Token] =
    elem("identifier", _.isInstanceOf[Identifier])

  /** An annotation id/literal */
  def annotLiteral: Parser[Token] =
    elem("annontation", _.isInstanceOf[AnnotationLiteral])

}