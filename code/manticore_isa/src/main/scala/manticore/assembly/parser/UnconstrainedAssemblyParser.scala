package manticore.assembly.parser

import scala.util.parsing.combinator._
import manticore.assembly.ManticoreAssemblyIR
import manticore.assembly.annotations.AssemblyAnnotation
import manticore.assembly.annotations.AssemblyAnnotationBuilder
import manticore.assembly.BinaryOperator

import scala.util.parsing.input.CharArrayReader.EofCh
import scala.util.parsing.input.Positional
import scala.util.parsing.input.Reader

import scala.language.implicitConversions
import scala.collection.mutable

import manticore.assembly.levels.unconstrained.UnconstrainedIR
import java.io.File
import manticore.assembly.levels.Transformation
import manticore.assembly.levels.AssemblyTransformer
import manticore.assembly.levels.AssemblyChecker
import manticore.compiler.AssemblyContext
import manticore.assembly.Reporter
import manticore.assembly.annotations.AnnotationValue
import manticore.assembly.annotations.IntValue
import manticore.assembly.annotations.StringValue
import manticore.assembly.annotations.AssemblyAnnotationFields

class UnconstrainedAssemblyLexer extends AssemblyLexical {

  def token: Parser[Token] =
    (defChar ~ identChar ~ rep(identChar) ^^ { case r ~ first ~ rest =>
      Keyword((r :: (first :: rest)) mkString "")
    }
      | annotChar ~ identChar ~ rep(identChar | digit) ^^ {
        case at ~ first ~ rest =>
          AnnotationLiteral(first :: rest mkString "")
      }
      | identChar ~ rep(identChar | digit | '.') ^^ { case first ~ rest =>
        processIdent(first :: rest mkString "")
      }
      | '0' ~ 'x' ~ digit ~ rep(digit) ^^ { case '0' ~ 'x' ~ first ~ rest =>
        HexLiteral(first :: rest mkString "")
      }
      | '0' ~ 'b' ~ digit ~ rep(digit) ^^ { case '0' ~ 'b' ~ first ~ rest =>
        BinLiteral(first :: rest mkString "")
      }
      | digit ~ rep(digit) ^^ { case first ~ rest =>
        DeclLit(first :: rest mkString "")
      }
      | '\'' ~> rep(chrExcept('\'', '\n')) >> { chars =>
        stringEnd('\'', chars)
      }
      | '\"' ~> rep(chrExcept('\"', '\n')) >> { chars =>
        stringEnd('\"', chars)
      }
      | EofCh ^^^ EOF
      | delim
      | failure("illegal character"))

  /** Returns the legal identifier chars, except digits. */
  def identChar =
    letter | elem('_') | elem('$') | elem('%') | elem('\\')

  def defChar = elem('.')
  def annotChar = elem('@')

  /** Parses the final quote of a string literal or fails if it is unterminated.
    */
  private def stringEnd(quoteChar: Char, chars: List[Char]): Parser[Token] = {
    { elem(quoteChar) ^^^ StringLit(chars mkString "") } | err(
      "unclosed string literal"
    )
  }

  // see `whitespace in `Scanners`
  def whitespace: Parser[Any] = rep[Any](
    whitespaceChar
      | '/' ~ '*' ~ comment
      | '/' ~ '/' ~ rep(chrExcept(EofCh, '\n'))
      | '/' ~ '*' ~ rep(elem("", _ => true)) ~> err("unclosed comment")
  )

  protected def comment: Parser[Any] = (
    rep(chrExcept(EofCh, '*')) ~ '*' ~ '/' ^^ { _ => ' ' }
      | rep(chrExcept(EofCh, '*')) ~ '*' ~ comment ^^ { _ => ' ' }
  )

  /** The set of reserved identifiers: these will be returned as `Keyword`s. */
  val reserved = new mutable.HashSet[String]

  /** The set of delimiters (ordering does not matter). */
  val delimiters = new mutable.HashSet[String]

  protected def processIdent(name: String) =
    if (reserved contains name) Keyword(name) else Identifier(name)

  private lazy val _delim: Parser[Token] = {
    // construct parser for delimiters by |'ing together the parsers for the individual delimiters,
    // starting with the longest one -- otherwise a delimiter D will never be matched if there is
    // another delimiter that is a prefix of D
    def parseDelim(s: String): Parser[Token] = accept(s.toList) ^^ { x =>
      Keyword(s)
    }

    val d = new Array[String](delimiters.size)
    delimiters.copyToArray(d, 0)
    scala.util.Sorting.quickSort(d)
    (d.toList map parseDelim).foldRight(
      failure("no matching delimiter"): Parser[Token]
    )((x, y) => y | x)
  }
  protected def delim: Parser[Token] = _delim
}

private[this] object UnconstrainedAssemblyParser extends AssemblyTokenParser {
  type Tokens = AssemblyTokens
  val lexical: UnconstrainedAssemblyLexer = new UnconstrainedAssemblyLexer()
  import lexical._

  import manticore.assembly.levels.unconstrained.UnconstrainedIR._
  import manticore.assembly.levels.{
    RegType,
    WireType,
    MemoryType,
    InputType,
    OutputType,
    ConstType
  }

  val arithOperator =
    Seq
      .tabulate(BinaryOperator.maxId) { i =>
        BinaryOperator(i).toString() -> BinaryOperator(i)
      }
      .toMap

  // Arithmetic instruction opcodes
  lexical.reserved ++= Seq.tabulate(BinaryOperator.maxId) { i =>
    BinaryOperator(i).toString()
  }
  // rest of instruction
  lexical.reserved += ("CUST", "LLD", "LST", "GLD", "GST", "EXPECT",
  "SEND", "SET", "MUX", "EXPECT", "PREDICATE", "NOP", "PADZERO")
  lexical.reserved += ("LD", "ST") //short hand for LLD and LST
  // defs
  val RegTypes = Seq(".reg", ".wire", ".input", ".output", ".mem", ".const")

  lexical.reserved ++= RegTypes

  // register parameters
  // lexical.reserved += ("$INIT", "$TYPE", "$WIDTH", "$SLICE")
  lexical.delimiters += (",", "[", "]", ";", ":", "(", ")", "=")

  // def annotLiteral: Parser[String] =
  //   elem("annontation", _.isInstanceOf[AnnotationLiteral]) ^^ {
  //     _.chars
  //   }
  def hex_value: Parser[BigInt] = hexLit ^^ { x => BigInt(x.chars, 16) }
  def bin_value: Parser[BigInt] = binLit ^^ { x => BigInt(x.chars, 2) }
  def dec_value: Parser[BigInt] = decLit ^^ { t => BigInt(t.chars) }
  def const_value: Parser[BigInt] = hex_value | dec_value | bin_value

  def annon_int_value: Parser[AnnotationValue] = const_value ^^ {
    case x: BigInt => IntValue(x.toInt)
  }
  def annon_string_value: Parser[AnnotationValue] = stringLit ^^ { case x =>
    StringValue(x.chars)
  }

  def annon_value: Parser[AnnotationValue] =
    annon_int_value | annon_string_value
  def keyvalue: Parser[(String, AnnotationValue)] =
    (ident ~ "=" ~ annon_value) ^^ { case (k ~ _ ~ v) => (k.chars, v) }
  def single_annon: Parser[AssemblyAnnotation] =
    (positioned(annotLiteral) ~ opt("[" ~> repsep(keyvalue, ",") <~ "]")) ^^ {
      case (n ~ values) =>
        AssemblyAnnotationBuilder(
          n.chars,
          if (values.nonEmpty) values.get.toMap
          else Map.empty[String, AnnotationValue]
        )

    }

  def annotations: Parser[Seq[AssemblyAnnotation]] = rep(single_annon)

  def def_reg: Parser[DefReg] =
    (annotations ~ (RegTypes
      .map(keyword(_))
      .reduce(_ | _)) ~ ident ~ const_value ~
      opt(const_value) <~ opt(";")) ^^ { case (a ~ t ~ name ~ s ~ v) =>
      val tt = t.chars match {
        case (".wire")   => WireType
        case (".reg")    => RegType
        case (".input")  => InputType
        case (".output") => OutputType
        case (".mem")    => MemoryType
        case (".const")  => ConstType
      }
      DefReg(LogicVariable(name.chars, s.toInt, tt), v, a)
    }

  def func_single_value: Parser[Seq[BigInt]] = const_value ^^ { x => Seq(x) }
  def func_list_value: Parser[Seq[BigInt]] =
    "[" ~> repsep(const_value, ",") <~ "]"
  def func_value: Parser[Seq[BigInt]] = func_list_value | func_single_value

  def def_func: Parser[DefFunc] =
    (keyword(".func") ~ ident ~ func_value <~ opt(";")) ^^ {
      case (Keyword(".func") ~ name ~ vs) =>
        DefFunc(name.chars, vs)
    }

  def arith_op: Parser[BinaryOperator.BinaryOperator] =
    (arithOperator.keySet.map(keyword(_)).reduce(_ | _)) ^^ { op =>
      arithOperator(op.chars)
    }
  def arith_inst: Parser[BinaryArithmetic] =
    (annotations ~ arith_op ~ ident ~ "," ~ ident ~ "," ~ ident) ^^ {
      case (a ~ op ~ rd ~ _ ~ rs1 ~ _ ~ rs2) =>
        BinaryArithmetic(op, rd.chars, rs1.chars, rs2.chars, a)
    }

  def lvec_inst: Parser[CustomInstruction] =
    (annotations ~ keyword(
      "CUST"
    ) ~ ident ~ "," ~ "[" ~ ident ~ "]" ~ "," ~ ident ~ "," ~ ident ~ "," ~ ident ~ "," ~ ident) ^^ {
      case (a ~ Keyword("CUST")
          ~ rd ~ _ ~ _ ~ fn ~ _ ~ _ ~ rs1 ~ _ ~ rs2 ~ _ ~ rs3 ~ _ ~ rs4) =>
        CustomInstruction(
          fn.chars,
          rd.chars,
          rs1.chars,
          rs2.chars,
          rs3.chars,
          rs4.chars,
          a
        )
    }

  def lload_inst: Parser[LocalLoad] =
    (annotations ~ (keyword(
      "LLD"
    ) | keyword("LD")) ~ ident ~ "," ~ ident ~ "[" ~ const_value ~ "]") ^^ {
      case (a ~ _ ~ rd ~ _ ~ base ~ _ ~ offset ~ _) =>
        LocalLoad(rd.chars, base.chars, offset, a)
    }

  def lstore_inst: Parser[LocalStore] =
    (annotations ~ (keyword(
      "LST"
    ) | keyword("ST")) ~ ident ~ "," ~ ident ~ "[" ~ const_value ~ "]" ~ opt(
      "," ~> ident
    )) ^^ { case (a ~ _ ~ rs ~ _ ~ base ~ _ ~ offset ~ _ ~ p) =>
      LocalStore(rs.chars, base.chars, offset, p.map(_.chars), a)
    }

  def gstore_inst: Parser[GlobalStore] =
    (annotations ~ keyword(
      "GST"
    ) ~ ident ~ "," ~ "[" ~ ident ~ "," ~ ident ~ "," ~ ident ~ "]" ~ opt(
      "," ~> ident
    )) ^^ {
      case (a ~ Keyword(
            "GST"
          ) ~ rs ~ _ ~ _ ~ rh ~ _ ~ rm ~ _ ~ rl ~ _ ~ p) =>
        GlobalStore(
          rs.chars,
          (rh.chars, rm.chars, rl.chars),
          p.map(_.chars),
          a
        )
    }

  def gload_inst: Parser[GlobalLoad] =
    (annotations ~ keyword(
      "GLD"
    ) ~ ident ~ "," ~ "[" ~ ident ~ "," ~ ident ~ "," ~ ident ~ "]") ^^ {
      case (a ~ Keyword(
            "GLD"
          ) ~ rs ~ _ ~ _ ~ rh ~ _ ~ rm ~ _ ~ rl ~ _) =>
        GlobalLoad(rs.chars, (rh.chars, rm.chars, rl.chars), a)
    }
  def set_inst: Parser[SetValue] =
    (annotations ~ keyword("SET") ~ ident ~ "," ~ const_value) ^^ {
      case (a ~ Keyword("SET") ~ rd ~ _ ~ value) =>
        SetValue(rd.chars, value, a)
    }
  def send_inst: Parser[Send] =
    (annotations ~ keyword(
      "SEND"
    ) ~ ident ~ "," ~ ("[" ~> ident <~ "]") ~ "," ~ ident) ^^ {
      case (a ~ Keyword("SEND") ~ rd ~ _ ~ dest_id ~ _ ~ rs) =>
        Send(rd.chars, rs.chars, dest_id.chars, a)
    }
  def expect_inst: Parser[Expect] =
    (annotations ~ keyword(
      "EXPECT"
    ) ~ ident ~ "," ~ ident ~ "," ~ ("[" ~> stringLit <~ "]")) ^^ {
      case (a ~ Keyword("EXPECT") ~ ref ~ _ ~ got ~ _ ~ ex_id) =>
        Expect(ref.chars, got.chars, ex_id.chars, a)
    }

  def pred_inst: Parser[Predicate] =
    (annotations ~ keyword("PREDICATE") ~ ident) ^^ {
      case (a ~ Keyword("PREDICATE") ~ rs) =>
        Predicate(rs.chars, a)
    }

  def mux_inst: Parser[Mux] =
    (annotations ~ keyword(
      "MUX"
    ) ~ ident ~ "," ~ ident ~ "," ~ ident ~ "," ~ ident) ^^ {
      case (a ~ _ ~ rd ~ _ ~ sel ~ _ ~ rs1 ~ _ ~ rs2) =>
        Mux(rd.chars, sel.chars, rs1.chars, rs2.chars, a)
    } // only a pseudo instruction, should be translated to a binary mux later

  def nop_inst: Parser[Instruction] = (keyword("NOP")) ^^ { _ => Nop }

  def padzero_inst: Parser[Instruction] = (annotations ~ keyword(
    "PADZERO"
  ) ~ ident ~ "," ~ ident ~ "," ~ const_value) ^^ {
    case (a ~ _ ~ rd ~ _ ~ rs ~ _ ~ width) =>
      PadZero(rd.chars, rs.chars, width, a)
  }

  def instruction: Parser[Instruction] = positioned(
    arith_inst | lvec_inst | lload_inst | lstore_inst | mux_inst | nop_inst
      | gload_inst | gstore_inst | set_inst | send_inst | expect_inst | pred_inst | padzero_inst
  ) <~ ";"
  def body: Parser[Seq[Instruction]] = rep(instruction)
  def regs: Parser[Seq[DefReg]] = rep(positioned(def_reg))
  def funcs: Parser[Seq[DefFunc]] = rep(positioned(def_func))
  def process: Parser[DefProcess] =
    (annotations ~ keyword(".proc") ~ ident ~ ":" ~ regs ~ funcs ~ body) ^^ {
      case (a ~ Keyword(".proc") ~ id ~ _ ~ rs ~ fs ~ insts) =>
        DefProcess(id.chars, rs, fs, insts, a)
    }

  def program: Parser[DefProgram] =
    (annotations ~ keyword(".prog") ~ ":" ~ rep(process)) ^^ {
      case (a ~ _ ~ _ ~ p) =>
        DefProgram(
          p,
          a
        )
    }
  def apply(input: String): DefProgram = {

    val tokens: lexical.Scanner = new lexical.Scanner(input)
    phrase(positioned(program))(tokens) match {
      case Success(result, _) => result
      case failure: NoSuccess =>
        println(s"Failed parsing at ${failure.next.pos}: ${failure.msg}")
        // println(failure.msg)
        scala.sys.error("Parsing failed")
    }
  }

}

object AssemblyParser extends Reporter {

  def apply(
      source: String,
      context: AssemblyContext
  ): UnconstrainedIR.DefProgram = {
    logger.info("Parsing from string input")
    UnconstrainedAssemblyParser(source)
  }

  def apply(
      source: File,
      context: AssemblyContext
  ): UnconstrainedIR.DefProgram = {
    logger.info("Parsing from file input")
    UnconstrainedAssemblyParser(scala.io.Source.fromFile(source).mkString(""))
  }

}
