package manticore.compiler.assembly.levels.codegen

import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.CompilerTransformation
import manticore.compiler.assembly.levels.placed.PlacedIR._
import manticore.compiler.FunctionalTransformation
import manticore.compiler.AssemblyContext
import manticore.compiler.LoggerId
import manticore.compiler.assembly.{SerialInterrupt, FinishInterrupt, StopInterrupt, AssertionInterrupt}
import manticore.compiler.FormatString.FmtBin
import manticore.compiler.FormatString.FmtHex
import manticore.compiler.FormatString.FmtDec
import manticore.compiler.FormatString.FmtConcat
import manticore.compiler.FormatString.FmtLit
import java.io.PrintWriter

object CodeDump extends FunctionalTransformation[DefProgram, Unit] {
  private implicit val loggerId = LoggerId("CodeDump")
  override def apply(program: DefProgram)(implicit ctx: AssemblyContext): Unit = {

    if (ctx.output_dir.isEmpty) {
      ctx.logger.error("No output dir specified!")
      ctx.logger.fail("Could not continue")
    }

    val outDir = ctx.output_dir.get

    // generate the initializer programs
    ctx.logger.info("Dumping initializers")
    val initializers = InitializerProgram.makeInitializer(program).map(MachineCodeGenerator.assembleProgram)
    val initDirs     = Range(0, initializers.length).map { index => outDir.toPath.resolve(s"init_${index}") }
    initializers.zip(initDirs).foreach { case (init, dir) => MachineCodeGenerator.generateCode(init, dir) }

    ctx.logger.info("Dumping program")
    val programDir = outDir.toPath.resolve("main")
    MachineCodeGenerator.generateCode(MachineCodeGenerator.assembleProgram(program), programDir)

    ctx.logger.info("Creating manifest")

    // first find the privileged process
    val privileged = program.processes.filter { proc => proc.body.exists(_.isInstanceOf[PrivilegedInstruction]) }
    if (privileged.isEmpty) {
      ctx.logger.fail("Could not find a privileged process")
    } else if (privileged.length > 1) {
      ctx.logger.fail("Can not handle more than one privileged process")
    }

    val globalMemories = privileged.head.globalMemories
    val interrupts     = privileged.head.body.collect { case x: Interrupt => x }

    def jsonValue[V](v: V): String = {
      if (v.isInstanceOf[String]) {
        "\"" + v + "\""
      } else if (v.isInstanceOf[Iterable[_]]) {
        "[" + v.asInstanceOf[Iterable[_]].map(jsonValue).mkString(", ") + "]"
      } else {
        v.toString()
      }
    }
    def asRecord[V](tabs: Int, fields: (String, V)*): String = {

      val record = fields
        .map { case (k, v) =>
          s"    \"${k}\": ${jsonValue(v)}"
        }
        .map(" " * tabs + _)
        .mkString(",\n")
      record
    }
    def asJson[V](tabs: Int, fields: (String, V)*): String = {
      val record = asRecord(tabs * 2, fields: _*)
      " " * tabs + "{\n" + record + "\n" + " " * tabs + "}"
    }
    val interruptJson = interrupts
      .map {
        case Interrupt(SimpleInterruptDescription(action, info, eid), _, _, _) =>
          val tpe = (action: @unchecked) match {
            case AssertionInterrupt => "ASSERT"
            case FinishInterrupt    => "FINISH"
            case StopInterrupt      => "STOP"
          }
          val str = asJson(
            12,
            "type" -> tpe,
            "eid"  -> eid,
            "info" -> info.map(_.getFile()).getOrElse("")
          )
          str
        case Interrupt(SerialInterruptDescription(SerialInterrupt(fmt), info, eid, pointers), _, _, _) =>
          val ptrQ = scala.collection.mutable.Queue.from(pointers)

          val fmtStr = fmt.parts
            .map {
              case FmtLit(chars) =>
                asJson(20, "type" -> "string", "value" -> chars)
              case FmtHex(w) =>
                asJson(20, "type" -> "hex", "offsets" -> Seq(ptrQ.dequeue()), "bitwidth" -> w)
              case FmtBin(w) =>
                asJson(20, "type" -> "bin", "offsets" -> Seq(ptrQ.dequeue()), "bitwidth" -> w)
              case d @ FmtDec(w, fillZero) =>
                asJson(
                  20,
                  "type"     -> "bin",
                  "offsets"  -> Seq(ptrQ.dequeue()),
                  "bitwidth" -> w,
                  "digits"   -> d.decWidth,
                  "zeros"    -> fillZero
                )
              case FmtConcat(atoms, width) =>
                val (tpe, w, d) = atoms.head match {
                  case t: FmtBin => ("bin", t.width, t.width)
                  case t: FmtDec => ("dec", t.width, t.decWidth)
                  case t: FmtHex => ("hex", t.width, t.width)
                }
                val offsets = atoms.map(_ => ptrQ.dequeue())
                asJson(20, "type" -> tpe, "bitwidth" -> w, "digits" -> d, "offsets" -> offsets)
            }
            .mkString(",\n")
          val fmtField = " " * 16 + "\"fmt\": [\n" + fmtStr + "\n" + " " * 16 + "]"
          " " * 12 + "{\n" +
            Seq(
              s"\"type\": \"FLUSH\"",
              s"\"eid\": ${eid}",
              s"\"info\": \"${info.map(_.getFile()).getOrElse("")}\""
            ).map(" " * 16 + _).mkString(",\n") + ",\n" + fmtField + "\n" + " " * 12 + "}"

      }
      .mkString(",\n")

    val gridJson        = asRecord(4, "dimx" -> ctx.hw_config.dimX, "dimy" -> ctx.hw_config.dimY)
    val initializerJson = asRecord(4, "initializers" -> initDirs.map(_.resolve("exec.bin").toAbsolutePath().toString()))
    val programJson     = asRecord(4, "program" -> programDir.resolve("exec.bin").toAbsolutePath().toString())
    val exceptionsJson  = " " * 8 + "\"exceptions\": [\n" + interruptJson + "\n" + " " * 8 + "]"
    val globalMemoriesJson = " " * 8 + "\"memories\": [\n" + globalMemories
      .map { case DefGlobalMemory(_, size, base, _, _) =>
        asJson(12, "size" -> size, "base" -> base)
      }
      .mkString(",\n") + "\n" + " " * 8 + "]"
    val manifest =
      "{\n" + Seq(gridJson, initializerJson, programJson, exceptionsJson, globalMemoriesJson).mkString(",\n") + "\n}"
    val manifestWriter = new PrintWriter(outDir.toPath.resolve("manifest.json").toFile)
    manifestWriter.write(manifest)
    manifestWriter.close()
  }

}
