package manticore.assembly.parser

import scala.util.parsing.combinator._
import manticore.assembly.ManticoreAssemblyIR

import manticore.assembly.levels.{WireLogic, MemoryLogic, RegLogic, InputLogic, OutputLogic}
import manticore.assembly.levels.MemoryLogic

import scala.util.parsing.input.CharArrayReader.EofCh
import scala.util.parsing.input.Positional
import scala.util.parsing.input.Reader


import scala.language.implicitConversions
import scala.collection.mutable


class SaltyAssemblyLexer extends AssemblyLexical {

  def token: Parser[Token] =
    (defChar ~ identChar ~ rep(identChar) ^^ { case r ~ first ~ rest =>
      Keyword((r :: (first :: rest)) mkString "")
    }
      | annotChar ~ identChar ~ rep(identChar) ^^ { case at ~ first ~ rest =>
        AnnotationLiteral(first :: rest mkString "")
      }
      | identChar ~ rep(identChar | digit) ^^ { case first ~ rest =>
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
  def identChar = letter | elem('_')

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



object RawCircuitAssemblyParser extends AssemblyTokenParser {
  type Tokens = AssemblyTokens
  val lexical: SaltyAssemblyLexer = new SaltyAssemblyLexer()
  import lexical._

  import manticore.assembly.levels.UnconstrainedIR._

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
  lexical.reserved += ("CUST", "LLD", "LST", "GLD", "GST", "EXPECT", "SEND", "SET")

  // defs
  val RegTypes = Seq(".reg", ".wire", ".input", ".output", ".mem")

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
  def const_value: Parser[BigInt] = hex_value | dec_value

  def bit_slice: Parser[(Int, Int)] =
    ("[" ~> const_value ~ ":" ~ const_value <~ "]") ^^ { case (hi ~ _ ~ lo) =>
      (hi.toInt, lo.toInt)
    }
  def keyvalue: Parser[(String, String)] =
    (ident ~ "=" ~ stringLit) ^^ { case (k ~ _ ~ v) => (k.chars, v.chars) }
  def single_annon: Parser[AssemblyAnnotation] =
    log(positioned(annotLiteral) ~ opt("[" ~> repsep(keyvalue, ",") <~ "]"))(
      "annotation"
    ) ^^ { case (n ~ values) =>
      AssemblyAnnotation(
        n.chars,
        values.getOrElse(Seq()).toMap
      )
    }

  def annotations: Parser[Seq[AssemblyAnnotation]] = rep(single_annon)

  def def_reg: Parser[DefReg] =
    (annotations ~ (RegTypes
      .map(keyword(_))
      .reduce(_ | _)) ~ ident ~ bit_slice ~
      opt(const_value) <~ ";") ^^ { case (a ~ t ~ name ~ s ~ v) =>
      val tt = t.chars match {
        case (".wire")   => WireLogic
        case (".reg")    => RegLogic
        case (".input")  => InputLogic
        case (".output") => OutputLogic
        case (".mem")    => MemoryLogic
      }
      DefReg(LogicVariable(name.chars, s, tt), v, a)
    }

  def def_func: Parser[DefFunc] =
    (keyword(".func") ~ ident ~ "[" ~ repsep(
      const_value,
      ","
    ) ~ "]" <~ ";") ^^ { case (Keyword(".func") ~ name ~ _ ~ vs ~ _) =>
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
    (annotations ~ keyword(
      "LLD"
    ) ~ ident ~ "," ~ ident ~ "[" ~ const_value ~ "]") ^^ {
      case (a ~ Keyword("LLD") ~ rd ~ _ ~ base ~ _ ~ offset ~ _) =>
        LocalLoad(rd.chars, base.chars, offset, a)
    }

  def lstore_inst: Parser[LocalStore] =
    (annotations ~ keyword(
      "LST"
    ) ~ ident ~ "," ~ ident ~ "[" ~ const_value ~ "]") ^^ {
      case (a ~ Keyword("LST") ~ rs ~ _ ~ base ~ _ ~ offset ~ _) =>
        LocalStore(rs.chars, base.chars, offset, a)
    }

  def gstore_inst: Parser[GlobalStore] =
    (annotations ~ keyword(
      "GST"
    ) ~ ident ~ "," ~ "[" ~ ident ~ "," ~ ident ~ "," ~ ident ~ "," ~ ident ~ "]") ^^ {
      case (a ~ Keyword(
            "GST"
          ) ~ rs ~ _ ~ _ ~ rhh ~ _ ~ rh ~ _ ~ rl ~ _ ~ rll ~ _) =>
        GlobalStore(rs.chars, (rhh.chars, rh.chars, rl.chars, rll.chars), a)
    }

  def gload_inst: Parser[GlobalLoad] =
    (annotations ~ keyword(
      "GLD"
    ) ~ ident ~ "," ~ "[" ~ ident ~ "," ~ ident ~ "," ~ ident ~ "," ~ ident ~ "]") ^^ {
      case (a ~ Keyword(
            "GLD"
          ) ~ rs ~ _ ~ _ ~ rhh ~ _ ~ rh ~ _ ~ rl ~ _ ~ rll ~ _) =>
        GlobalLoad(rs.chars, (rhh.chars, rh.chars, rl.chars, rll.chars), a)
    }

  def instruction: Parser[Instruction] = positioned(
    arith_inst | lvec_inst | lload_inst | lstore_inst | gload_inst | gstore_inst
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
    // println(tokens)
    // println("Tokens:")
    // def printTokens(tokens_left: lexical.Scanner): Unit = {
    //   tokens_left.atEnd match {
    //     case false =>
    //       println(tokens_left.first)
    //       printTokens(tokens_left.rest)
    //     case _ => ()
    //   }
    // }

    // printTokens(tokens)
    // // println(tokens.first)
    println("Parsing:")
    phrase(positioned(program))(tokens) match {
      case Success(result, _) => result
      case failure: NoSuccess =>
        println("Failed")
        println(failure.msg)
        println(failure.next.pos)
        println(failure.next.first.toString())
        scala.sys.error("Parsing failed")
    }
  }

}

// object ParseTest extends App {

//   /** { } denotes zero or more repetitions Annon::== '@'id Prog ::== '.prog' {
//     * Proc } Proc ::== '.proc' id ':' Regs
//     */
//   val ast = RawCircuitAssemblyParser(
//     """
//         @OPT
//         @PRGANNON [id = "MyTop"]
//         .prog :
//           @PROCID [id = "sd"]
//           .proc proc_0: 
//             // comment:
            
//             .wire zero[31: 0] 0x000000000000000000000000000000;
//             @TYPE [t = "reg"]
//             .input ii[1:0];
//             .output oo[2:0];
//             @TRACKED    [my_name="my_tracked_signal"]
//             .reg r1[31 : 0]  0x19;
//             .reg r2[31 : 12] 0x11;
//             .reg r3[31 : 0]  ;
//             .wire r2[0 : 0] 10;
//             .func f0 [0x10, 0x2];
//             @SOURCEINFO [file="/scaratch/mayy/my_vmodule.v", lines="12:13"]
//             ADD oo, zero, ii;
//             XOR rd, ds, sa;
//             CUST rd, [f0], rs1, rs2, rs3, rs4;
                
//           @PROCID [id = "dd"]
//           .proc proc_1: 
//             // comment:
            
//             @WIRE
//             .reg zero[31: 0] 0x000000000000000000000000000000;
//             .reg r1[31 : 0]  0x19;
//             .reg r2[31 : 12] 0x11;
//             .reg r3[31 : 0]  ;
//             .wire r2 [32 : 0];
//             @MEMORY [orig = "id"]
//             .reg  base_mem_ptr[127:0];
//             .reg  mem_word_read_port [31:0];
//             .func f0 [0x10];
            
//             LLD mem_word_read_port, base_mem_ptr[0x010];
//             @SOURCEINFO [file="/scaratch/mayy/my_vmodule.v", lines="12:13"]
//             @TRACKED    [signal="my_tracked_signal"]
//             ADD r1, r2, r3;
//             XOR rd, ds, sa;
//             CUST rd, [f0], rs1, rs2, rs3, rs4;      
//             LLD  rd, base_mem_0[0x100]; // comment
//             LST  rs, base[0x31]; /* comment */
//             GST  rs, [rs1, rs2, rs3, zero]; // global store
//             GLD  rd, [rhh, rhl, rl, rll]; // global load
//             GLD  rd, [zero, zero, zero, addr]; 

//       """
//   )
//   println(
//     ast.serialized
//   )
//   // println(
//   //   RawCircuitAssemblyParser(
//   //     "LVEC r1, [f0], r2, r3, r4, r5"
//   //   )
//   // )

// }
