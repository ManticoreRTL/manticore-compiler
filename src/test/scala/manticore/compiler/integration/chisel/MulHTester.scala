package manticore.compiler.integration.chisel

import chisel3._
import chiseltest._
import manticore.compiler.AssemblyContext
import manticore.compiler.DefaultHardwareConfig
import manticore.compiler.HasLoggerId
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.assembly.levels.codegen.MachineCodeGenerator
import manticore.compiler.assembly.utils.XorShift16
import manticore.compiler.integration.chisel.util.KernelTester

import scala.annotation.tailrec

class MulHTester extends KernelTester {

  def mkProgram(
      context: AssemblyContext,
      testSize: Int,
      width: Int
  )(implicit f: FixtureParam): String = {
    require(width < 63)

    val randGen = new scala.util.Random(1231)

    val dir = context.output_dir.get

    val op1 = Seq.fill(testSize) { randGen.nextLong(1L << width) }
    val op2 = Seq.fill(testSize) { randGen.nextLong(1L << width) }

    val res = op1 zip op2 map { case (x, y) => (BigInt(x) * BigInt(y)) & ((1L << width) - 1L) }

    val op1Meminit = f.dump("op1.dat", op1.mkString("\n")).toAbsolutePath.toString
    val op2Meminit = f.dump("op2.dat", op2.mkString("\n")).toAbsolutePath.toString
    val resMeminit = f.dump("res.dat", res.mkString("\n")).toAbsolutePath.toString

    s"""
       |.prog:
       |   @LOC [x = 0, y = 0]
       |   .proc proc_0_0:
       |       @MEMINIT[ file="${op1Meminit}", count=$testSize, width=${width} ]
       |       .mem op1_ptr ${width} ${testSize}
       |       @MEMINIT[ file="${op2Meminit}", count=$testSize, width=${width} ]
       |       .mem op2_ptr ${width} ${testSize}
       |       @MEMINIT[ file="${resMeminit}", count=$testSize, width=${width} ]
       |       .mem res_ptr ${width} ${testSize}
       |
       |       .reg cnt 32 .input counter_curr 0 .output counter_next
       |       .reg dn  1 .input done_curr 0 .output done_next
       |       .reg cr  1 .input correct_curr 1 .output correct_next
       |       // .reg r1  ${width} .input res_ref_curr .output res_ref_next
       |       // .reg r2  ${width} .input res_curr .output res_next
       |       .wire res_ref_next ${width}
       |       .wire res_next ${width}
       |       .wire op1 ${width}
       |       .wire op2 ${width}
       |       .wire res ${width}
       |
       |       .const one 16 1
       |       .const zero 16 0
       |       .const max_counter 16 ${testSize - 1}
       |
       |       (op1_ptr, 0) LLD op1, op1_ptr[counter_curr];
       |       (op2_ptr, 0) LLD op2, op2_ptr[counter_curr];
       |       (res_ptr, 0) LLD res_ref_next, res_ptr[counter_curr];
       |
       |       MUL res_next, op1, op2;
       |       SEQ correct_next, res_next, res_ref_next;
       |
       |       ADD counter_next, counter_curr, one;
       |       SEQ done_next, counter_curr, max_counter;
       |       // (0) PUT counter_curr, one;
       |       // (1) PUT res_curr, one;
       |       // (2) PUT res_ref_curr, one;
       |       // (3) FLUSH "@%32d got %${width}d expected %${width}d", one;
       |       (4) ASSERT correct_curr;
       |       (5) FINISH done_curr;
       |
        """.stripMargin


  }

  def testCase(width: Int, testSize: Int): Unit = {
    s"${width}-bit MUL" should s"correctly compute ${testSize} results" in { implicit f =>
      val context = AssemblyContext(
        output_dir = Some(f.test_dir.resolve("out").toFile()),
        hw_config = DefaultHardwareConfig(dimX = 2, dimY = 2),
        dump_all = true,
        dump_dir = Some(f.test_dir.resolve("dumps").toFile()),
        log_file = Some(f.test_dir.resolve("run.log").toFile),
        expected_cycles = Some(testSize),
        use_loc = true
      )

      implicit val TestName = new HasLoggerId { val id = getTestName }
      val source            = mkProgram(context, testSize, width)
      f.dump("main.masm", source)
      compileAndRun(source, context)

    }
  }
  behavior of "Wide multiplication"

  testCase(16, 150)
  testCase(17, 300)
  testCase(32, 300)
  testCase(34, 300)


}
