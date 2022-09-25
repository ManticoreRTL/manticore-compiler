package manticore.compiler.integration.chisel

import manticore.compiler.AssemblyContext
import manticore.compiler.integration.chisel.util.KernelTester
import manticore.compiler.assembly.utils.XorShift16
import manticore.compiler.assembly.levels.UInt16
import manticore.compiler.DefaultHardwareConfig

class LocalMemoryTester extends KernelTester {

  def mkProgram(
      testSize: Int,
      numMems: Int,
      stride: Int
  )(implicit fixture: FixtureParam): String = {

    val rnd = IndexedSeq.tabulate(numMems) { i =>
      IndexedSeq.tabulate(stride) { j => XorShift16(s"rand_${i}_${j}", UInt16(123 + (i * stride + j))) }
    }
    val meminit = mkMemInit(Seq.fill(testSize) { UInt16(0) }, fixture.test_dir.resolve("mem.dat"))
    def generate[T](gn: (Int, Int) => T) =
      Seq.tabulate(numMems) { i => Seq.tabulate(stride) { j => gn(i, j) } }.flatten
    s"""|
        |.prog:
        |   .proc p0:
        |   ${Seq.tabulate(numMems) { i => s"${meminit}\n.mem mem_ptr_${i} 16 ${testSize}" }.mkString("\n")}
        |   ${generate { case (i, j) => s".wire ld_${i}_${j} 16 " }.mkString("\n")}
        |   ${generate { case (i, j) => s".reg r${i}_${j} 1 .input correct_${i}_${j}_curr 1 .output correct_${i}_${j}_next"}.mkString("\n")}
        |   ${generate { case (i, j) => rnd(i)(j).registers }.mkString("\n")}
        |   .reg cnt 32 .input counter_curr 0 .output counter_next
        |   .reg dn  1 .input done_curr 0 .output done_next
        |   .const one 16 1
        |   .const zero 16 0
        |   .const max_counter 16 ${testSize - 1}
        |   ${generate { case (i, j) => rnd(i)(j).code }.mkString("\n")}
        |   ${generate { case (i, j) => s"(mem_ptr_${i}, ${2 * j}) LST ${rnd(i)(j).randCurr}, mem_ptr_${i}[counter_curr], one;"}.mkString("\n")}
        |   ${generate { case (i, j) => s"(mem_ptr_${i}, ${2 * j + 1}) LLD ld_${i}_${j}, mem_ptr_${i}[counter_curr];" }.mkString("\n")}
        |   ${generate { case (i, j) => s"SEQ correct_${i}_${j}_next, ld_${i}_${j}, ${rnd(i)(j).randCurr};" } .mkString("\n")}
        |   ${generate { case (i, j) => s"(0) ASSERT correct_${i}_${j}_curr;" }.mkString("\n")}
        |   ADD counter_next, counter_curr, one;
        |   SEQ done_next, counter_curr, max_counter;
        |   (1) FINISH done_curr;
        |
        |""".stripMargin
  }

  def doTest(testSize: Int, numMems: Int, stride: Int): Unit =
    s"${numMems} local memories" should s"handle ${stride} LSTs before LLDs" in { implicit fixture =>
      val context = AssemblyContext(
        output_dir = Some(fixture.test_dir.resolve("out").toFile()),
        // make the scratchpad size small so that the global memory will be used
        hw_config = DefaultHardwareConfig(dimX = 2, dimY = 2),
        dump_all = true,
        dump_dir = Some(fixture.test_dir.resolve("dumps").toFile()),
        expected_cycles = Some(testSize),
        log_file = Some(fixture.test_dir.resolve("run.log").toFile)
      )
      val source = mkProgram(testSize, numMems, stride)
      fixture.dump(s"main_${testSize}_${numMems}_${stride}.masm", source)
      compileAndRun(source, context)
    }

    doTest(250, 4, 4)
    doTest(250, 16, 16)
    doTest(250, 20, 16)


}
