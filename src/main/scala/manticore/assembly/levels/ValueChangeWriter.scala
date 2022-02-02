package manticore.assembly.levels

import manticore.compiler.AssemblyContext
import manticore.assembly.annotations.DebugSymbol
import java.io.File
import java.io.PrintWriter

trait ValueChangeWriter extends Flavored {
  import flavor._
  def update(name: Name, value: Constant): Unit
  def tick(): Unit
  def flush(): Unit
  def close(): Unit
}

abstract class ValueChangeWriterBase extends ValueChangeWriter {

  import flavor._
  val program: DefProgram
  val file_name: String
  implicit val ctx: AssemblyContext

  def toBigInt(v: Constant): BigInt

  import scala.collection.mutable.{HashMap => MutableMap}
  private def getDebugSymbol(r: DefReg): Option[DebugSymbol] =
    r.annons.collectFirst { case x: DebugSymbol =>
      x
    }
  // TODO: Check that every name is unique?

  // is the debug symbol for a user (i.e., verilog source) or compiler generated?
  private def hasUserDebugSymbol(r: DefReg): Boolean = {
    val dbg = getDebugSymbol(r)
    dbg match {
      case Some(sym) =>
        !sym.isGenerated().getOrElse(true)
      case _ => false
    }
  }
  private val sym_groups = {
    val groups = program.processes.flatMap { _.registers }.collect {
      case r: DefReg if hasUserDebugSymbol(r) => r
    } groupBy { r => getDebugSymbol(r).get.getSymbol() } map {
      case (sym, regs) =>
        sym -> regs.sortBy { r =>
          getDebugSymbol(r).get.getIndex().getOrElse(0)
        }
    }
  }
  trait ChangeRecord {

    def write(value: BigInt, index: Int): Boolean
    def toString(): String
    def getChange(): Option[String]
    def tick(): Unit

  }
  object ChangeRecord {

    private class Impl(
        private val content: Array[BigInt],
        private var updated: Boolean,
        private val part_width: Array[Int],
        private val width: Int
    ) extends ChangeRecord {

      def write(value: BigInt, index: Int): Boolean = {
        if (content(index) == value) {
          false
        } else {
          content(index) = value
          updated = true
          true
        }
      }
      override def toString(): String = {
        def makeBinString(x: BigInt, w: Int): String =
          String
            .format("%" + w + "s", x.toString(2))
            .takeRight(w)
            .replace(' ', '0')
        val parts = content
          .zip(part_width)
          .map { case (x, w) => makeBinString(x, w) }
        val whole = parts.foldRight("") { case (x, builder) =>
          builder + x
        }
        "b" + whole.takeRight(width)
      }
      def getChange(): Option[String] = {
        if (updated) {
          Some(toString())
        } else {
          None
        }
      }

      def tick(): Unit = {
        updated = false
      }

    }

    /** Create VCD record for the sequence of registers that share the same
      * debug symbol
      *
      * @param regs
      *   Sequence of registers sorted by increasing order of their DebugSymbol
      *   index
      * @param width
      * @return
      */
    def apply(regs: Seq[DefReg], width: Int): ChangeRecord = {
      // get the indices (should be sorted)
      val indices = regs.map { r =>
        getDebugSymbol(r) match {
          case None      => 0 // should not happen
          case Some(dbg) => dbg.getIndex().getOrElse(0)
        }
      }
      val max_index = indices.last

      // their might be some missing indices due to optimizations, we need
      // fill in the whole for those (not a very efficient approach but we
      // don't care about performance here)
      val values = regs.map { _.value.map(toBigInt).getOrElse(BigInt(0)) }
      val defined_values = indices.zip(values).toMap

      val content = Array.tabulate(max_index + 1) { i =>
        defined_values.getOrElse(i, BigInt(0))
      }

      new Impl(
        content = content,
        width = width,
        part_width = regs.map { _.variable.width }.toArray,
        updated = false
      )
    }
  }

  def fail(msg: String): Nothing = throw new RuntimeException(msg)

  // sequence of register that have the DebugSymbol and are not generated
  // by the compiler phases
  private val user_registers: Seq[DefReg] =
    program.processes.flatMap { _.registers }.collect {
      case r: DefReg if hasUserDebugSymbol(r) => r
    }
  // a map from debug symbol to the sequence of registers sharing it with
  // different indices. Note that we do not check for duplicate indices
  // in debug symbols as we assume the debug symbols are well-formed.
  private val groups: Map[String, Seq[DefReg]] =
    user_registers groupBy { r => getDebugSymbol(r).get.getSymbol() } map {
      case (sym, regs) =>
        sym -> regs.sortBy { r =>
          getDebugSymbol(r).get.getIndex().getOrElse(0)
        }

    }
  // a map from register names to their debug symbol, note that
  // we can have name_sym_lookup(x) == name_sym_lookup(y) for x != y
  private val name_sym_lookup: Map[Name, DebugSymbol] = user_registers.map {
    r =>
      r.variable.name -> getDebugSymbol(r).get
  }.toMap

  // a map from symbols to records
  private val record_table: Map[String, ChangeRecord] =
    groups.map { case (sym, regs) =>
      sym -> ChangeRecord(regs, getDebugSymbol(regs.head).get.getWidth().get)
    }

  private var tick_num: Long = 0
  private val dump_file: File = ctx.logger.openFile(file_name)
  private val printer: PrintWriter = new PrintWriter(dump_file)
  // initialize the file
  private def emit(header: String)(body: => String): Unit = {
    printer.print(f"$$${header}  ")
    printer.print(body)
    printer.println("  $end")
  }
  private val vcd_names: Map[String, String] = {
    import java.util.Calendar
    import java.text.SimpleDateFormat
    val date_fmt = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm z")
    val now = Calendar.getInstance().getTime()

    emit("date") { date_fmt.format(now) }
    emit("version") { "VCD generated by Manticore Assembly Compiler" }
    emit("timescale") { "1ns" }
    emit("scope") { "module TOP" }
    // we use the ascii ! as the clock symbol
    emit("var") { "wire 1 ! clock" }

    val vcd_ids = Range(0, groups.size) map { case i => s"s${i}" }

    val id_map = scala.collection.mutable.Map[String, String]()
    groups zip vcd_ids foreach { case ((sym, regs), id) =>
      val width = getDebugSymbol(regs.head).get.getWidth().get
      def legalize(n: String): String =
        if (
          n.head == '$' || (('0' to '9') contains n.head)
        ) // these are compiler generated names
          "\\" + n
        else if (n.head == '\\') // yosys names
          n.tail
        else
          n
      def emitVar(n: Seq[String]): Unit = n match {

        case name +: Seq() =>
          emit("var") { s"wire ${width} ${id} ${legalize(name)}" }
        case hier +: rest =>
          emit("scope") { s"module ${legalize(hier)}" }
          emitVar(rest)
          emit("upscope") { "" }
      }
      val name_parts = sym.split('.').toSeq
      emitVar(name_parts)
      id_map += (sym -> id)

    }
    emit("upscope") { "" }
    emit("enddefinitions") { "" }
    emit("dumpvars") {
      record_table.map { case (sym, record) =>
        val id = id_map(sym)
        record.toString + " " + id
      } mkString "\n"
    }
    id_map.toMap
  }

  def update(name: Name, value: Constant): Unit =
    name_sym_lookup.get(name) match {
      case Some(sym) =>
        val symbol: String = sym.getSymbol()
        val index = sym.getIndex().getOrElse(0)
        val records = record_table(symbol)
        try {
          record_table(symbol).write(toBigInt(value), index)
        } catch {
          case e: Exception =>
            ctx.logger.error(
              s"error writing VCD record for ${name} (symbol = ${symbol}, index = ${index}): ${e.getMessage()}"
            )
        }
      case None =>
      // name is not tracked
    }

  def tick(): Unit = {
    printer.println(s"#${tick_num}")
    printer.println("1!")
    record_table foreach { case (sym, record) =>
      val id = vcd_names(sym)
      record.getChange() match {
        case Some(v) =>
          printer.println(s"${v} ${id}")
        case None =>
        // value not change, no need to dump it
      }
      record.tick()
    }
    printer.println(s"#${tick_num + 1}")
    printer.println("0!")
    tick_num += 2
  }

  def flush(): Unit = printer.flush()
  def close(): Unit = printer.close()

}
