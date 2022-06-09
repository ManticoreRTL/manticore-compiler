package manticore.compiler.assembly.levels.unconstrained

import manticore.compiler.UnitFixtureTest
import manticore.compiler.assembly.utils.XorShift128
import manticore.compiler.assembly.levels.unconstrained.width.WidthConversion
import manticore.compiler.assembly.parser.AssemblyParser
import manticore.compiler.AssemblyContext

class UnconstrainedWideGlobalMemoryTester extends UnitFixtureTest {

  behavior of "wide large memories"

  def testCase(width: Int, size: Int) =
    s"${width}x${size} memory" should "correctly store and then load" in { fixture =>
      assert(width <= 96)

      val rnd1     = XorShift128("rand1", 13723)
      val rnd2     = XorShift128("rand2", 78979812)
      val rnd3     = XorShift128("rand3", 9876123)
      val addrBits = BigInt(size).bitLength
      val text =
        s"""|
                |.prog: .proc p0:
                |   .mem m ${width} ${size}
                |
                |   .reg waddr ${addrBits} .input wptr 0 .output wptr_n
                |   .reg raddr ${addrBits} .input rptr 0 .output rptr_n
                |   .reg correct 1 .input correct_c 1 .output correct_n
                |   .reg storeValue ${width} .input stc 0 .output stn
                |   .reg loadValue ${width} .input ldc 0 .output ldn
                |   .reg done       1     .input donec 0 .output donen
                |   .const wen 1 1
                |    ${rnd1.registers}
                |    ${rnd2.registers}
                |    ${rnd3.registers}
                |   .const c0 96 0
                |   .const c32 96 32
                |   .const c64 96 64
                |   .const cw 32 ${width}
                |   .wire t0 96
                |   .wire t1 96
                |   .wire t2 96
                |   .wire t3 96
                |   .wire t4 96
                |   .wire t5 ${width}
                |   .wire correct 1
                |   .wire rnd64 64
                |   .wire rnd96 96
                |   .wire ldval ${width}
                |   .const incr ${addrBits} 1
                |   .const csize ${addrBits} ${size - 1}
                |   .wire finished 1
                |    ${rnd1.code}
                |    ${rnd2.code}
                |    ${rnd3.code}
                |
                |   // concatenate random values
                |
                |   (0) PUT stc, wen;
                |   (1) PUT ldc, wen;
                |   (2) PUT correct_c, wen;
                |   (3) FLUSH "(%${width}h == %${width}h) = %1d", wen;
                |
                |   (4) ASSERT correct_c;
                |   (5) FINISH donec;
                |
                |   SLL t0, ${rnd1.randCurr}, c0;
                |   SLL t1, ${rnd2.randNext}, c32;
                |   SLL t2, ${rnd3.randNext}, c64;
                |   OR t3, t1, t0;
                |   OR t4, t3, t2;
                |
                |   SRL stn, t4, cw;
                |
                |   (m, 0) LST stn, m[wptr], wen;
                |
                |   (m, 1) LLD ldn, m[rptr];
                |
                |   SEQ correct_n, ldn, stn;
                |
                |   ADD wptr_n, wptr, incr;
                |   MOV rptr_n, wptr_n;
                |   SEQ donen, wptr, csize;
                |
                |
                |
                |""".stripMargin

      val compiler = AssemblyParser andThen
        UnconstrainedNameChecker andThen
        UnconstrainedMakeDebugSymbols andThen
        UnconstrainedOrderInstructions andThen
        UnconstrainedIRConstantFolding andThen
        UnconstrainedIRStateUpdateOptimization andThen
        UnconstrainedIRCommonSubExpressionElimination andThen
        UnconstrainedDeadCodeElimination andThen
        WidthConversion.transformation andThen
        UnconstrainedNameChecker andThen UnconstrainedCloseSequentialCycles andThen UnconstrainedInterpreter
      implicit val ctx = AssemblyContext(
        dump_all = false,
        dump_dir = Some(fixture.test_dir.toFile()),
        log_file = Some(fixture.test_dir.resolve("run.log").toFile()),
        debug_message = true,
        max_cycles = size + 2,
        max_local_memory = 256 // artificially limit the local memory cap
      )
      fixture.dump("main.masm", text)
      compiler(text)

    }

  testCase(10, 10) // should be local
  testCase(10, 800) // should be global
  testCase(40, 600) //...
  testCase(40, 256) //...
  testCase(64, 128) //...
  testCase(32, 780) //...
  testCase(15, 297) //...
  testCase(2, 1279) //...
  testCase(1, 2000) //...
  testCase(7, 423) //...
  testCase(48, 690) //...
  testCase(33, 300) //...
  testCase(16, 320) //...
  testCase(17, 180) //...


//   testCase(10, 800)
//   testCase(50, 1200)

}
