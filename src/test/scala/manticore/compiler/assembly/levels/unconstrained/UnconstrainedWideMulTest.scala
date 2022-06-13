package manticore.compiler.assembly.levels.unconstrained

import manticore.compiler.assembly.BinaryOperator
import manticore.compiler.AssemblyContext
import org.scalatest.CancelAfterFailure

class UnconstrainedWideMulTest extends UnconstrainedWideTest with CancelAfterFailure {

  behavior of "wide MUL/MULS"

  def mkTest(width: Int, operator: BinaryOperator.BinaryOperator): Unit = {
    assert(operator == BinaryOperator.MUL || operator == BinaryOperator.MULS)

    s"$operator $width" should "correctly compute multiplication of arbitrary size" in { fixture =>
      val count    = 10000
      val rs1_vals = Array.fill(count) { mkWideRand(width) }
      val rs2_vals = Array.fill(count) { mkWideRand(width) }
      def sext(v: BigInt): BigInt = {
        require(v >= 0)
        v.testBit(width - 1) match {

          case true =>
            val signExtension = ((BigInt(1) << width) - 1) << width
            v | signExtension
          case false =>
            v
        }
      }.ensuring(_ >= 0)
      // not masking the results will make the test fail, obviously.
      val rd_vals: Array[BigInt] = (rs1_vals zip rs2_vals).map { case (r1, r2) =>
        assert(r1 >= 0)
        assert(r2 >= 0)
        operator match {
          case BinaryOperator.MUL =>
            (r1 * r2) & ((BigInt(1) << width) - 1)
          case BinaryOperator.MULS =>
            (sext(r1) * sext(r2)) & ((BigInt(1) << width) - 1)
        }
      }

      val rs1_fp     = fixture.dump("rs1_vals.dat", rs1_vals)
      val rs2_fp     = fixture.dump("rs2_vals.dat", rs2_vals)
      val rd_fp      = fixture.dump("rd_vals.dat", rd_vals)
      val addr_width = log2Ceil(count)
      val text = s"""
    .prog:
        .proc proc_0_0:

            @MEMINIT[file = "${rs1_fp}", count = ${count}, width = ${width}]

            .mem rs1_ptr ${width} ${count}

            @MEMINIT[file = "${rs2_fp}", count = ${count}, width = ${width}]

            .mem rs2_ptr ${width} ${count}

            @MEMINIT[file = "${rd_fp}", count = ${count}, width = ${width}]
            .mem rd_ptr ${width} ${count}

            .wire counter ${addr_width} 0
            .const const_0 16 0
            .const const_1 16 1
            .const const_ptr_inc ${addr_width} 1
            .const const_max ${addr_width} ${count - 1}
            .wire matches 1
            .wire  done  1 0

            .wire rs1_v ${width}
            .wire rs2_v ${width}
            .wire rd_v ${width}
            .wire rd_ref ${width}


            (rs1_ptr, 0) LD rs1_v, rs1_ptr[counter];
            (rs2_ptr, 0) LD rs2_v, rs2_ptr[counter];
            ${operator} rd_v, rs1_v, rs2_v;
            (rd_ptr, 0) LD rd_ref, rd_ptr[counter];

            (0) PUT rd_v, const_1;
            (1) PUT rs1_v, const_1;
            (2) PUT rs2_v, const_1;
            (3) PUT rd_ref, const_1;
            (4) FLUSH "got %${width}b = %${width}b x %${width}b and expected %${width}b.", const_1;
            (5) FLUSH "results match!", matches;
            SEQ matches, rd_ref, rd_v;
            (6) ASSERT matches;

            SEQ done, counter, const_max;
            (7) FINISH done;

            ADD counter, counter, const_ptr_inc;


    """
      implicit val ctx = AssemblyContext(
        dump_all = true,
        dump_dir = Some(fixture.test_dir.toFile),
        quiet = false,
        log_file = Some(fixture.test_dir.resolve("run.log").toFile()),
        max_local_memory = (1 << 20),
        max_cycles = 10500
      )
      backend(text)

    }

  }

  val widthCases = (Seq(1, 2, 3, 4, 7, 8, 17, 16, 32, 33, 29) ++ Seq.fill(80) {
    randgen.nextInt(80) + 1
  }).distinct

  for (w <- widthCases) {

    mkTest(w, BinaryOperator.MUL)
    mkTest(w, BinaryOperator.MULS)

  }
    // mkTest(17, BinaryOperator.MULS)
}
