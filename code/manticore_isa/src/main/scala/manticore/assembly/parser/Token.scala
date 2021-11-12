package manticore.assembly.parser

import scala.util.parsing.combinator.lexical.Scanners
import scala.util.parsing.input.CharArrayReader.EofCh

/** 
 * Valid tokens in the Assembly language with positions, 
 * the trait almost a copy of [[scala.util.parsing.combinator.token]]
 * with the major difference that every Token kind derives from the 
 * [[scala.util.parsing.input.Positional]] trait which is necessary to have
 * 
 * 
 * 
  */

trait AssemblyTokens {

  import scala.util.parsing.input.Positional
  /**
    * Base abstract token class
    */
  sealed abstract class Token extends Positional {
    def chars: String
  }

  /** The class of lexical errors */

  case class ErrorToken(msg: String) extends Token {
    def chars: String = s"*** Error: $msg"
  }

  /** End-of-file token */
  case object EOF extends Token {
    def chars: String = "<eof>"
  }

  /** The class of keyword tokens */
  case class Keyword(chars: String) extends Token {
    override def toString = s"'$chars'"
  }

//   /** The class of numeric literal tokens */
//   case class NumericLit(chars: String) extends Token {
//     override def toString = chars
//   }

  /** The class of string literal tokens */
  case class StringLit(chars: String) extends Token {
    override def toString = s""""$chars""""
  }

  /** The class of identifier tokens */
  case class Identifier(chars: String) extends Token {
    override def toString = s"identifier $chars"
  }

  
  /** The class of non-negative numeric literals which can be decimal, binary or hex */
  sealed abstract class NumericLit(chars: String) extends Token {
      override def toString = chars
  }

  /** The class of decimal literals */
  case class DeclLit(chars: String) extends NumericLit(chars)

  /** The class of hex literal tokens */
  case class HexLiteral(chars: String) extends NumericLit(chars) {
    override def toString = s"0x$chars"
  }

  case class BinLiteral(chars: String) extends NumericLit(chars) {
      override def toString = s"0b$chars"
  }
  /** The class of annotation literal tokens */
  case class AnnotationLiteral(chars: String) extends Token {
    override def toString = s"@${chars.toUpperCase}"
  }

  def errorToken(msg: String): Token = ErrorToken(msg)
}



/**
  * Abstract helper class that contains some standard token definitions
  * Any Assembly lexer should derive from this class
  */
abstract class AssemblyLexical extends Scanners with AssemblyTokens {

  /** A character-parser that matches a letter (and returns it). */
  def letter = elem("letter", _.isLetter)

  /** A character-parser that matches a digit (and returns it). */
  def digit = elem("digit", _.isDigit)

  /** A character-parser that matches any character except the ones given in
    * `cs` (and returns it).
    */
  def chrExcept(cs: Char*) = elem("", ch => !cs.contains(ch))

  /** A character-parser that matches a white-space character (and returns it).
    */
  def whitespaceChar = elem("space char", ch => ch <= ' ' && ch != EofCh)
  
}