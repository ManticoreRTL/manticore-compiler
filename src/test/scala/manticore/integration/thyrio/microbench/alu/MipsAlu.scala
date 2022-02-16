package integration.thyrio.microbench.alu

import manticore.integration.ThyrioUnitTest
import manticore.integration.thyrio.ExternalTool
import manticore.integration.thyrio.ThyrioFrontend
import manticore.integration.thyrio.Verilator
import manticore.integration.thyrio.Make
import java.io.PrintWriter
import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import manticore.compiler.AssemblyContext
import manticore.assembly.parser.AssemblyParser
import manticore.assembly.levels.Transformation
import manticore.assembly.ManticoreAssemblyIR
import manticore.assembly.levels.unconstrained.UnconstrainedIR
import manticore.compiler.ManticorePasses

class MipsAlu extends ThyrioUnitTest {

  val requiredTools: Seq[ExternalTool] = Seq(
    Make,
    Verilator,
    ThyrioFrontend
  )

  behavior of "MIPS32 ALU test"

  checkInstalled()

  val test_root_dir = root_dir
  val resource_dir =
    getClass().getResource("/integration/microbench/alu/mips32").toURI()

  sealed abstract class AluControl(val v: Int)
  case object SLL extends AluControl(Integer.parseInt("0000", 2))
  case object SRL extends AluControl(Integer.parseInt("0001", 2))
  case object SRA extends AluControl(Integer.parseInt("0010", 2))
  case object ADD extends AluControl(Integer.parseInt("0011", 2))
  case object SUB extends AluControl(Integer.parseInt("0100", 2))
  case object AND extends AluControl(Integer.parseInt("0101", 2))
  case object OR extends AluControl(Integer.parseInt("0110", 2))
  case object XOR extends AluControl(Integer.parseInt("0111", 2))
  case object NOR extends AluControl(Integer.parseInt("1000", 2))
  case object SLT extends AluControl(Integer.parseInt("1001", 2))
  case object LUI extends AluControl(Integer.parseInt("1010", 2))

  val controls = Seq(SLL, SRL, SRA, ADD, SUB, AND, OR, XOR, NOR, SLT, LUI)

  val randGen = new scala.util.Random(0)

  def compute(ctrl: AluControl, op1: Long, op2: Long): (Long, Boolean) = {
    val shift_amount = op2 & 31
    val res = ctrl match {
      case SLL => op1 << shift_amount
      case SRL =>
        op1 >>> shift_amount //unsigned shift i.e., >> in SystemVerilog
      case SRA =>
        (op1.toInt) >> shift_amount.toInt // signed shift i.e., >>> in SystemVerilog
      case ADD => op1 + op2
      case SUB => op1 - op2
      case AND => op1 & op2
      case OR  => op1 | op2
      case XOR => op1 ^ op2
      case NOR => ~(op1 | op2)
      case SLT => if (op1 < op2) 1 else 0
      case LUI => op1 << 16
    }
    val mask: Long = 0x00000000ffffffffL
    val actual_res = res & mask
    (actual_res, actual_res == 0)
  }

  val test_size = 100
  def generateTest(work_dir: Path): Unit = {

    println("Generating random tests")

    val mask: Long = 0x00000000ffffffffL
    val ctrl = Seq.fill(test_size) {
      val ix = randGen.nextInt(controls.length)
      // val ix = randGen.nextInt(4)
      controls(ix)
      // SLL
      // SUB
      // SLT
    }
    val op1 = Seq.fill(test_size) { randGen.nextLong() & mask }
    val op2 = Seq.tabulate(test_size) { ix =>
      if (ctrl(ix) == SLL || ctrl(ix) == SRL || ctrl(ix) == SRA)
        randGen.nextInt(32)
      else
        randGen.nextLong() & mask
    }

    val results: Seq[(Long, Boolean)] = ctrl zip (op1 zip op2) map {
      case (c, (r1, r2)) =>
        compute(c, r1, r2)
    }

    def dumpString(file_name: String, content: String): Path = {
      val fp = work_dir.resolve(file_name)
      val printer = new PrintWriter(fp.toFile())
      printer.print(content)
      printer.close()
      fp
    }
    def dumpSeq(file_name: String, content: Seq[Long]): Path =
      // weirdly if we do not append a an extra element to the end of the list
      // Verilator will not read the actual last element correctly!
      dumpString(
        file_name,
        (content :+ 0L) map { i => f"${i}%x" } mkString "\n"
      )

    val op1_fp = dumpSeq("op1_rom.hex", op1)
    val op2_fp = dumpSeq("op2_rom.hex", op2)
    val ctrl_fp = dumpSeq("ctrl_rom.hex", ctrl.map { _.v })
    val result_fp = dumpSeq("results_rom.hex", results map (_._1))
    val zero_fp = dumpSeq(
      "zero_rom.hex",
      results map { case (_, b) => if (b) 1.toLong else 0.toLong }
    )

    val verilog_src = scala.io.Source
      .fromResource("integration/microbench/alu/mips32/alu.sv.in")
      .getLines()
      .map { l =>
        l.replace("@SIZE@", test_size.toString())
          .replace(
            "@CTRL@",
            ctrl_fp.toAbsolutePath.toString()
          )
          .replace(
            "@OP1@",
            op1_fp.toAbsolutePath.toString()
          )
          .replace(
            "@OP2@",
            op2_fp.toAbsolutePath.toString()
          )
          .replace(
            "@RESULT@",
            result_fp.toAbsolutePath.toString()
          )
          .replace(
            "@ZERO@",
            zero_fp.toAbsolutePath.toString()
          )
      } mkString "\n"

    dumpString("alu.sv", verilog_src)
    def copy(fname: String): Unit = Files.copy(
      Path.of(resource_dir).resolve(fname),
      work_dir.resolve(fname),
      StandardCopyOption.REPLACE_EXISTING
    )
    copy("VTester.cpp")
    copy("Makefile")
    copy("track.yml")

  }

  // generateTest()

  def runTest[T <: ManticoreAssemblyIR#DefProgram](
      work_dir: Path
  )(phase: => Transformation[UnconstrainedIR.DefProgram, T]): Unit = {

    val ctx =
      AssemblyContext(
        dump_all = true,
        dump_dir = Some(work_dir.resolve("AluTester").toFile()),
        max_cycles = test_size + 2,
        debug_message =  false,
        expected_cycles = Some(test_size + 1)
      )
    val parsed =
      AssemblyParser(work_dir.resolve(s"AluTester.masm").toFile(), ctx)
    phase(parsed, ctx)
  }

  def testIteration[T <: ManticoreAssemblyIR#DefProgram](
      i: Int, fixture: FixtureParam
  )(phases: Transformation[UnconstrainedIR.DefProgram, T]): Unit = {
    println(s"Test iteration ${i}")
    val work_dir = fixture.test_dir.resolve(s"t${i}")
    Files.createDirectories(work_dir)
    generateTest(work_dir)
    Make.invoke(Seq("verilate_run"), work_dir.toFile()) { println(_) }
    Make.invoke(Seq("thyrio"), work_dir.toFile()) { println(_) }
    runTest(work_dir)(phases)
  }
  it should "successfully interpret the results before width conversion" in {
    f =>
      Range(0, 10) foreach { i =>
        testIteration(i, f)(
          ManticorePasses.frontend followedBy
          ManticorePasses.FrontendInterpreter(true)
        )
      }

  }
  it should "successfully interpret the results before and after width conversion" in {
    f =>
      Range(0, 10) foreach { i =>
        testIteration(i, f)(
          ManticorePasses.frontend followedBy
          ManticorePasses.middleend followedBy
          ManticorePasses.FrontendInterpreter(true)
        )
      }
  }

  it should "successfully pass atomic interpretation test" in {
    f =>
      Range(0, 10) foreach { i =>
        testIteration(i, f)(
          ManticorePasses.frontend followedBy
          ManticorePasses.middleend followedBy
          ManticorePasses.FrontendInterpreter(true) followedBy
          ManticorePasses.backend followedBy
          ManticorePasses.BackendInterpreter(true)
        )
      }
  }

}
