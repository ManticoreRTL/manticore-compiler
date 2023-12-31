package manticore.compiler.integration.chisel.util

import manticore.compiler.AssemblyContext
import manticore.compiler.ManticorePasses
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.assembly.levels.codegen.MachineCodeGenerator
import manticore.compiler.assembly.levels.placed.PlacedIR
import manticore.compiler.assembly.levels.placed.PlacedIRConstantFolding
import manticore.compiler.assembly.levels.placed.UnconstrainedToPlacedTransform
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.machine.ManticoreBaseISA
import manticore.machine.core.Processor

import java.io.File
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path


trait ProgramTester {

  // def mkProgram(context: AssemblyContext): String

  def mkMemBlock(name: String, capacity: Int): String =
    s"@MEMBLOCK [block = \"${name}\", width = 16, capacity = ${capacity}]"

  def mkMemInit(values: Seq[UInt16], path: Path): String = {
    Files.createDirectories(path.getParent())
    val printer = new PrintWriter(path.toFile())
    printer.print(values.mkString("\n"))
    printer.close()
    s"@MEMINIT [ file = \"${path.toAbsolutePath}\", width = 16, count = ${values.length} ]"

  }

  def mkReg(name: String, init: Option[UInt16]): String = {
    s".reg ${name} 16 .input ${name}_curr ${init.map(_.toString).getOrElse("0")} .output ${name}_next"
    // Seq(
    //   s"@REG [id = \"${name}\", type = \"\\REG_CURR\" ]",
    //   s".input ${name}_curr 16 ${init.map(_.toString).getOrElse("")}",
    //   s"@REG [id = \"${name}\", type = \"\\REG_NEXT\" ]",
    //   s".output ${name}_next 16 "
    // ).mkString("\n")
  }
  def mkReceiver(name: String, init: Option[UInt16]): String = {
    Seq(
      s"@REG [id = \"${name}\", type = \"\\REG_CURR\" ]",
      s".input ${name}_curr 16 ${init.map(_.toString).getOrElse("")}",
    ).mkString("\n")
  }

  def mkInput(name: String, init: Option[UInt16]): String = {
    Seq(
      s"@REG [id = \"${name}\", type = \"\\REG_CURR\" ]",
      s".input ${name}_curr 16 ${init.map(_.toString).getOrElse("")}"
    ).mkString("\n")
  }

  def compiler =
    ManticorePasses.frontend andThen
      ManticorePasses.middleend andThen
      ManticorePasses.backend

  def compile(source: String, context: AssemblyContext): PlacedIR.DefProgram = {
    (AssemblyParser andThen compiler)(source)(context)

  }


}
