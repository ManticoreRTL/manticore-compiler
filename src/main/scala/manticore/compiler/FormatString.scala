package manticore.compiler

class FormatString private (val parts: Seq[FormatString.Fmt]) {

  lazy val holes = parts.collect { case x: FormatString.FmtArg => x }

  def isLit = holes.length == 0

  override def toString = parts.mkString("")

  // replace the first argument with the given literal
  def consume(replacement: FormatString.FmtLit): FormatString = {

    def replace(ls: Seq[FormatString.Fmt]): Seq[FormatString.Fmt] = ls match {
      case (h: FormatString.FmtArg) +: tail =>
        replacement +: tail
      case h +: tail =>
        h +: replace(tail)
      case Nil =>
        Nil
    }
    new FormatString(replace(parts))
  }

  def consume(values: Seq[BigInt]): FormatString = {
    def mkLiterals(
        holesLeft: Seq[FormatString.FmtArg],
        valuesLeft: Seq[BigInt]
    ): Seq[FormatString.FmtLit] =
      holesLeft match {
        case h +: tail =>
          h match {
            case atm: FormatString.FmtAtomArg =>
              valuesLeft match {
                case vh +: vTail =>
                  atm.toLit(vh) +: mkLiterals(tail, vTail)
                case _ =>
                  Nil
              }
            case FormatString.FmtConcat(atoms, width) =>
              val (usedValues, vTail) = valuesLeft.splitAt(atoms.length)
              if (usedValues.length == atoms.length) {
                val concatValue = FormatString.concatenate(atoms zip usedValues)
                val litValue = atoms.head.withWidth(width).toLit(concatValue)
                litValue +: mkLiterals(tail, vTail)
              } else {
                Nil
              }
          }
        case _ =>
          Nil
      }
    val literals = mkLiterals(holes, values)
    literals.foldLeft(this) { _ consume _ }
  }
  // substitute arguments for others
  def updated(
      subst: Map[FormatString.FmtArg, FormatString.FmtArg]
  ): FormatString = {
    val newParts = parts.map {
      case lit: FormatString.FmtLit => lit
      case arg: FormatString.FmtArg => subst.getOrElse(arg, arg)
    }
    new FormatString(newParts)
  }
  private def :+(p: FormatString.Fmt) = new FormatString(parts :+ p)
}

object FormatString {

  sealed trait Fmt
  sealed trait FmtArg extends Fmt { def width: Int }
  sealed trait FmtAtomArg extends FmtArg {
    def toLit(v: BigInt): FmtLit
    def withWidth(w: Int): FmtAtomArg
  }

  case class FmtLit(chars: String) extends Fmt {
    override def toString = chars
  }
  case class FmtHex(width: Int) extends FmtAtomArg {
    val hexWidth =
      ((BigInt(1) << width) - 1).toString(16).length
    def toLit(v: BigInt) = FmtLit(truncated(v.toString(16), hexWidth, "0"))
    override def toString: String = s"%${width}h"
    def withWidth(w: Int): FmtHex = copy(w)
  }
  case class FmtBin(width: Int) extends FmtAtomArg {
    def toLit(v: BigInt) = FmtLit(truncated(v.toString(2), width, "0"))
    override def toString: String = s"%${width}b"
    def withWidth(w: Int): FmtBin = copy(w)
  }
  case class FmtDec(width: Int, fillZero: Boolean = false) extends FmtAtomArg {
    val decWidth = ((BigInt(1) << width) - 1).toString().length
    def toLit(v: BigInt) = FmtLit(
      truncated(v.toString(), decWidth, if (fillZero) "0" else " ")
    )
    override def toString: String = if (fillZero) {
      s"%0${width}d"
    } else {
      s"%${width}d"
    }
    def withWidth(w: Int): FmtDec = copy(w)
  }

  case class FmtConcat[A <: FmtAtomArg](atoms: Seq[A], width: Int) extends FmtArg {
    override def toString: String = s"{${atoms.mkString(", ")}}(w${width})"
  }

  case class FmtParseError(error: String)

  private def truncated(vString: String, width: Int, filler: String): String = {
    val tr = if (vString.length > width) {
      vString.takeRight(width)
    } else {
      (filler * (width - vString.length)) ++ vString
    }
    tr
  }

  def parse(fmt: String): Either[FmtParseError, FormatString] = {
    // split the string by % but also keep the delimiter see:
    // https://stackoverflow.com/questions/2206378/how-to-split-a-string-but-also-keep-the-delimiters
    val parts = fmt.split("(?=%)")
    val fmtSpec = raw"%(0?)([1-9][0-9]*)([dbhDBH])(.*)".r

    val r = parts.foldLeft[Either[FmtParseError, Seq[Fmt]]](Right(Seq.empty)) {
      case (Right(builder), p) =>
        p match {
          case fmtSpec(z, w, tpe, msgLit) =>
            val bitWidth = w.toInt
            val lit = if (msgLit.nonEmpty) FmtLit(msgLit) +: Nil else Nil
            tpe match {
              case "d" | "D" =>
                Right((builder :+ FmtDec(bitWidth, z.nonEmpty)) ++ lit)
              case "h" | "H" =>
                Right((builder :+ FmtHex(bitWidth)) ++ lit)
              case "b" | "B" =>
                Right((builder :+ FmtBin(bitWidth)) ++ lit)
              case _ =>
                Left(FmtParseError(msgLit))
            }
          case _ =>
            builder match {
              case prevs :+ (last: FmtLit) =>
                Right(prevs :+ FmtLit(last.chars + p))
              case _ => Right(builder :+ FmtLit(p))
            }

        }
      case (Left(error), _) => Left(error)
    }
    r match {
      case Left(error)     => Left(error)
      case Right(fmtParts) => Right(new FormatString(fmtParts))
    }
  }
  def check(fmt: String): Boolean =
    parse(fmt).isRight

  def concatenate(
      substLeft: Seq[(FmtAtomArg, BigInt)],
      offset: Int = 0
  ): BigInt = {
    substLeft match {
      case (atm, vlu) +: tail =>
        val mask = ((BigInt(1) << atm.width)) - 1
        val masked = vlu & mask
        (masked << offset) | concatenate(
          tail,
          offset + atm.width
        )
      case Nil =>
        BigInt(0)
    }
  }

}
