package manticore.compiler.assembly.parser

import scala.util.parsing.combinator._
import manticore.compiler.assembly.ManticoreAssemblyIR
import manticore.compiler.assembly.annotations.AssemblyAnnotation
import manticore.compiler.assembly.annotations.AssemblyAnnotationBuilder
import manticore.compiler.assembly.BinaryOperator

import scala.util.parsing.input.CharArrayReader.EofCh
import scala.util.parsing.input.Positional
import scala.util.parsing.input.Reader

import scala.language.implicitConversions
import scala.collection.mutable

import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR
import java.io.File

import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.AssemblyChecker
import manticore.compiler.AssemblyContext

import manticore.compiler.assembly.annotations.AnnotationValue
import manticore.compiler.assembly.annotations.IntValue
import manticore.compiler.assembly.annotations.StringValue
import manticore.compiler.assembly.annotations.AssemblyAnnotationFields
import manticore.compiler.assembly.levels.HasTransformationID
import manticore.compiler.assembly.annotations.Reg
import manticore.compiler.HasLoggerId
import manticore.compiler.FormatString
import manticore.compiler.LoggerId

class UnconstrainedAssemblyLexer extends AssemblyLexical {

  def token: Parser[Token] =
    (defChar ~ identChar ~ rep(identChar) ^^ { case r ~ first ~ rest =>
      Keyword((r :: (first :: rest)) mkString "")
    }
      | annotChar ~ identChar ~ rep(identChar | digit) ^^ { case at ~ first ~ rest =>
        AnnotationLiteral(first :: rest mkString "")
      }
      | identChar ~ rep(identChar | digit | '.') ^^ { case first ~ rest =>
        processIdent(first :: rest mkString "")
      }
      | '0' ~ 'x' ~ digit ~ rep(digit) ^^ { case _ ~ _ ~ first ~ rest =>
        HexLiteral(first :: rest mkString "")
      }
      | '0' ~ 'b' ~ digit ~ rep(digit) ^^ { case _ ~ _ ~ first ~ rest =>
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

  def defChar   = elem('.')
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

private[parser] object UnconstrainedAssemblyParser extends AssemblyTokenParser {

  implicit val loggerId = LoggerId("AssemblyParser")
  type Tokens = AssemblyTokens
  val lexical: UnconstrainedAssemblyLexer = new UnconstrainedAssemblyLexer()
  import lexical._

  import manticore.compiler.assembly.levels.unconstrained.UnconstrainedIR._
  import manticore.compiler.assembly.levels.{
    RegType,
    WireType,
    MemoryType,
    InputType,
    OutputType,
    ConstType
  }
  private val privateOperators =
    Set(BinaryOperator.MUX, BinaryOperator.ADDC, BinaryOperator.MULH)
  val arithOperator =
    Seq
      .tabulate(BinaryOperator.maxId) { i =>
        BinaryOperator(i).toString() -> BinaryOperator(i)
      }
      .filter { case (_, op) => //
        !privateOperators(op)
      }
      .toMap

  // Arithmetic instruction opcodes
  lexical.reserved ++= Seq.tabulate(BinaryOperator.maxId) { i =>
    BinaryOperator(i).toString()
  }
  // rest of instruction
  lexical.reserved ++= Seq(
    "CUST",
    "LLD",
    "LST",
    "GLD",
    "GST",
    "EXPECT",
    "SEND",
    "SET",
    "MUX",
    "EXPECT",
    "PREDICATE",
    "NOP",
    "PADZERO",
    "MOV",
    "PARMUX",
    "SLICE",
    "PUT",
    "FLUSH",
    "FINISH",
    "STOP",
    "ASSERT"
  )
  lexical.reserved ++= Seq("LD", "ST") //short hand for LLD and LST
  // defs
  val RegTypes = Seq(".reg", ".wire", ".input", ".output", ".mem", ".const")

  lexical.reserved ++= RegTypes

  // register parameters
  // lexical.reserved += ("$INIT", "$TYPE", "$WIDTH", "$SLICE")
  lexical.delimiters ++= Seq(",", "[", "]", ";", ":", "(", ")", "=", "?")

  // def annotLiteral: Parser[String] =
  //   elem("annontation", _.isInstanceOf[AnnotationLiteral]) ^^ {
  //     _.chars
  //   }
  def hex_value: Parser[BigInt]   = hexLit ^^ { x => BigInt(x.chars, 16) }
  def bin_value: Parser[BigInt]   = binLit ^^ { x => BigInt(x.chars, 2) }
  def dec_value: Parser[BigInt]   = decLit ^^ { t => BigInt(t.chars) }
  def const_value: Parser[BigInt] = hex_value | dec_value | bin_value

  def annon_int_value: Parser[AnnotationValue] = const_value ^^ { case x: BigInt =>
    IntValue(x.toInt)
  }
  def annon_string_value: Parser[AnnotationValue] = stringLit ^^ { case x =>
    StringValue(x.chars)
  }

  def annon_value: Parser[AnnotationValue] =
    annon_int_value | annon_string_value
  def keyvalue: Parser[(String, AnnotationValue)] =
    (ident ~ "=" ~ annon_value) ^^ { case (k ~ _ ~ v) => (k.chars, v) }
  def single_annon: Parser[AssemblyAnnotation] =
    (positioned(annotLiteral) ~ opt("[" ~> repsep(keyvalue, ",") <~ "]")) ^^ { case (n ~ values) =>
      AssemblyAnnotationBuilder(
        n.chars,
        if (values.nonEmpty) values.get.toMap
        else Map.empty[String, AnnotationValue]
      )

    }

  def annotations: Parser[Seq[AssemblyAnnotation]] = rep(single_annon)

  def def_const: Parser[DefReg] =
    (annotations ~ keyword(".const") ~ ident ~ const_value ~ const_value <~ opt(
      ";"
    )) ^^ { case a ~ _ ~ name ~ w ~ v =>
      DefReg(LogicVariable(name.chars, w.toInt, ConstType), Some(v), a)
    }
  def def_wire: Parser[DefReg] =
    (annotations ~ keyword(".wire") ~ ident ~ const_value ~ opt(
      const_value
    ) <~ opt(";")) ^^ { case a ~ _ ~ name ~ w ~ v =>
      DefReg(LogicVariable(name.chars, w.toInt, WireType), v, a)
    }
  def def_input: Parser[DefReg] =
    (annotations ~ keyword(".input") ~ ident ~ const_value ~ opt(
      const_value
    ) <~ opt(";")) ^^ { case a ~ _ ~ name ~ w ~ v =>
      DefReg(LogicVariable(name.chars, w.toInt, InputType), v, a)
    }
  def def_output: Parser[DefReg] =
    (annotations ~ keyword(".output") ~ ident ~ const_value <~ opt(";")) ^^ {
      case a ~ _ ~ name ~ w =>
        DefReg(LogicVariable(name.chars, w.toInt, OutputType), None, a)
    }
  def def_mem: Parser[DefReg] =
    (annotations ~ keyword(".mem") ~ ident ~ const_value ~ const_value <~ opt(
      ";"
    )) ^^ { case a ~ _ ~ name ~ w ~ sz =>
      DefReg(MemoryVariable(name.chars, w.toInt, sz.toInt), None, a)
    }

  def def_pair_input: Parser[(Token, Option[Constant])] =
    (keyword(".input") ~ positioned(ident) ~ opt(const_value)) ^^ { case _ ~ name ~ default_value =>
      (name, default_value)
    }
  def def_pair_output: Parser[Token] =
    (keyword(".output") ~ positioned(ident)) ^^ { case _ ~ name =>
      name
    }

  def def_pair: Parser[Seq[DefReg]] =
    (annotations ~ keyword(
      ".reg"
    ) ~ ident ~ const_value ~ def_pair_input ~ def_pair_output <~ opt(";")) ^^ {
      case a ~ _ ~ nameId ~ width ~ inp ~ outp =>
        Seq(
          DefReg(
            LogicVariable(inp._1.chars, width.toInt, InputType),
            inp._2,
            Reg(
              Map(
                AssemblyAnnotationFields.Id.name   -> StringValue(nameId.chars),
                AssemblyAnnotationFields.Type.name -> Reg.Current
              )
            ) +: a
          ).setPos(inp._1.pos),
          DefReg(
            LogicVariable(outp.chars, width.toInt, OutputType),
            None,
            Seq(
              Reg(
                Map(
                  AssemblyAnnotationFields.Id.name   -> StringValue(nameId.chars),
                  AssemblyAnnotationFields.Type.name -> Reg.Next
                )
              )
            )
          ).setPos(outp.pos)
        )
    }
  def def_reg: Parser[Seq[DefReg]] =
    (positioned(def_const | def_wire | def_input | def_output | def_mem) ^^ { case d =>
      Seq(d)
    }) | def_pair

  def func_single_value: Parser[Seq[BigInt]] = const_value ^^ { x => Seq(x) }
  def func_list_value: Parser[Seq[BigInt]] =
    "[" ~> repsep(const_value, ",") <~ "]"
  def func_value: Parser[Seq[BigInt]] = func_list_value | func_single_value

  def arith_op: Parser[BinaryOperator.BinaryOperator] =
    (arithOperator.keySet.map(keyword(_)).reduce(_ | _)) ^^ { op =>
      arithOperator(op.chars)
    }
  def arith_inst: Parser[BinaryArithmetic] =
    (annotations ~ arith_op ~ ident ~ "," ~ ident ~ "," ~ ident) ^^ {
      case (a ~ op ~ rd ~ _ ~ rs1 ~ _ ~ rs2) =>
        BinaryArithmetic(op, rd.chars, rs1.chars, rs2.chars, a)
    }

  def memory_order: Parser[MemoryAccessOrder] =
    ("(" ~> ident ~ "," ~ const_value <~ ")") ^^ { case (memid ~ _ ~ v) =>
      MemoryAccessOrder(memid.chars, v.toInt)
    }
  def lload_inst: Parser[LocalLoad] =
    (annotations ~ memory_order ~ (keyword(
      "LLD"
    ) | keyword("LD")) ~ ident ~ "," ~ ident ~ "[" ~ ident ~ "]") ^^ {
      case (a ~ order ~ _ ~ rd ~ _ ~ base ~ _ ~ addr ~ _) =>
        LocalLoad(rd.chars, base.chars, addr.chars, order, a)
    }

  def lstore_inst: Parser[LocalStore] =
    (annotations ~ memory_order ~ (keyword(
      "LST"
    ) | keyword("ST")) ~ ident ~ "," ~ ident ~ "[" ~ ident ~ "]" ~ opt(
      "," ~> ident
    )) ^^ { case (a ~ order ~ _ ~ rs ~ _ ~ base ~ _ ~ addr ~ _ ~ p) =>
      LocalStore(rs.chars, base.chars, addr.chars, p.map(_.chars), order, a)
    }

  def set_inst: Parser[SetValue] =
    (annotations ~ keyword("SET") ~ ident ~ "," ~ const_value) ^^ { case (a ~ _ ~ rd ~ _ ~ value) =>
      SetValue(rd.chars, value, a)
    }
  def send_inst: Parser[Send] =
    (annotations ~ keyword(
      "SEND"
    ) ~ ident ~ "," ~ ("[" ~> ident <~ "]") ~ "," ~ ident) ^^ {
      case (a ~ _ ~ rd ~ _ ~ dest_id ~ _ ~ rs) =>
        Send(rd.chars, rs.chars, dest_id.chars, a)
    }
  def expect_inst(implicit ctx: AssemblyContext): Parser[Expect] =
    (annotations ~ positioned(
      keyword(
        "EXPECT"
      )
    ) ~ ident ~ "," ~ ident ~ "," ~ ("[" ~> stringLit <~ "]")) ^^ {
      case (a ~ kword ~ ref ~ _ ~ got ~ _ ~ ex_id) =>
        val instr = Expect(ref.chars, got.chars, ex_id.chars, a).setPos(kword.pos)
        ctx.logger.warn("EXPECT will be retired soon. Use ASSERT instead.", instr)
        instr
    }

  def pred_inst: Parser[Predicate] =
    (annotations ~ keyword("PREDICATE") ~ ident) ^^ { case (a ~ _ ~ rs) =>
      Predicate(rs.chars, a)
    }

  def syscall_order: Parser[SystemCallOrder] = ("(" ~> const_value <~ ")") ^^ { case v1 =>
    SystemCallOrder(v1.toInt)
  }

  def control_syscall(n: String, action: InterruptAction): Parser[Interrupt] =
    (annotations ~ syscall_order ~ keyword(n) ~ ident) ^^ { case (a ~ order ~ _ ~ rs) =>
      Interrupt(action, rs.chars, order, a)
    }

  def fmt_string: Parser[FormatString] = (stringLit ^^ { case str =>
    FormatString.parse(str.chars)
  }) >> {
    case Left(error) => err(s"Invalid fmt: $error")
    case Right(fmt)  => success(fmt)
  }
  def flush_inst: Parser[Interrupt] =
    (annotations ~ syscall_order ~ keyword(
      "FLUSH"
    ) ~ fmt_string ~ "," ~ ident) ^^ { case (a ~ order ~ _ ~ fmt ~ _ ~ cond) =>
      Interrupt(SerialInterrupt(fmt), cond.chars, order, a)
    }

  def interrupt_inst: Parser[Interrupt] =
    control_syscall("FINISH", FinishInterrupt) | control_syscall(
      "STOP",
      StopInterrupt
    ) | control_syscall(
      "ASSERT",
      AssertionInterrupt
    ) | flush_inst

  def put_inst: Parser[PutSerial] =
    (annotations ~ syscall_order ~ keyword("PUT") ~ ident ~ "," ~ ident) ^^ {
      case (a ~ order ~ _ ~ rs ~ _ ~ pred) =>
        PutSerial(rs.chars, pred.chars, order, a)
    }

  def mux_inst: Parser[Mux] =
    (annotations ~ keyword(
      "MUX"
    ) ~ ident ~ "," ~ ident ~ "," ~ ident ~ "," ~ ident) ^^ {
      case (a ~ _ ~ rd ~ _ ~ sel ~ _ ~ rs1 ~ _ ~ rs2) =>
        Mux(rd.chars, sel.chars, rs1.chars, rs2.chars, a)
    } // only a pseudo instruction, should be translated to a binary mux later

  def parmux_case: Parser[ParMuxCase] = (ident ~ "?" ~ ident) ^^ { case (cond ~ _ ~ choice) =>
    ParMuxCase(cond.chars, choice.chars)
  }
  def parmux_inst: Parser[ParMux] =
    (annotations ~ keyword("PARMUX") ~ ident ~ "," ~ rep1sep(parmux_case, ",") ~
      "," ~ ident) ^^ { case (a ~ _ ~ rd ~ _ ~ (cases: Seq[ParMuxCase]) ~ _ ~ defcase) =>
      ParMux(
        rd.chars,
        cases,
        defcase.chars,
        a
      )
    }
  def mov_inst: Parser[Mov] =
    (annotations ~ keyword("MOV") ~ ident ~ "," ~ ident) ^^ { case (a ~ _ ~ rd ~ _ ~ rs) =>
      Mov(rd.chars, rs.chars, a)
    }

  def slice_inst: Parser[Slice] =
    (annotations ~ keyword(
      "SLICE"
    ) ~ ident ~ "," ~ ident ~ "," ~ const_value ~ "," ~ const_value) ^^ {
      case (a ~ _ ~ rd ~ _ ~ rs ~ _ ~ offset ~ _ ~ length) =>
        Slice(rd.chars, rs.chars, offset.toInt, length.toInt)
    }

  def nop_inst: Parser[Instruction] = (keyword("NOP")) ^^ { _ => Nop }

  def padzero_inst: Parser[Instruction] = (annotations ~ keyword(
    "PADZERO"
  ) ~ ident ~ "," ~ ident ~ "," ~ const_value) ^^ { case (a ~ _ ~ rd ~ _ ~ rs ~ _ ~ width) =>
    PadZero(rd.chars, rs.chars, width, a)
  }

  def instruction(implicit ctx: AssemblyContext): Parser[Instruction] = positioned(
    arith_inst | lload_inst | lstore_inst | mux_inst | parmux_inst | nop_inst
      | set_inst | send_inst | expect_inst | pred_inst | padzero_inst | mov_inst | slice_inst
      | interrupt_inst | put_inst
  ) <~ ";"
  def body(implicit ctx: AssemblyContext): Parser[Seq[Instruction]] = rep(instruction)
  def regs(implicit ctx: AssemblyContext): Parser[Seq[Seq[DefReg]]] = rep(def_reg)

  def process(implicit ctx: AssemblyContext): Parser[DefProcess] =
    (annotations ~ keyword(".proc") ~ ident ~ ":" ~ regs ~ body) ^^ {
      case (a ~ _ ~ id ~ _ ~ rs ~ insts) =>
        DefProcess(id.chars, rs.flatten, Seq.empty, insts, Nil, Nil, a)
    }

  def program(implicit ctx: AssemblyContext): Parser[DefProgram] =
    (annotations ~ keyword(".prog") ~ ":" ~ rep(process)) ^^ { case (a ~ _ ~ _ ~ p) =>
      DefProgram(
        p,
        a
      )
    }
  def apply(input: String)(implicit ctx: AssemblyContext): DefProgram = {

    val tokens: lexical.Scanner = new lexical.Scanner(input)
    phrase(positioned(program))(tokens) match {
      case Success(result, _) => result
      case failure: NoSuccess =>
        ctx.logger.error(
          s"Failed parsing at ${failure.next.pos}: ${failure.msg}"
        )
        // println(failure.msg)
        ctx.logger.fail("Parsing failed")
    }
  }

}
