package manticore.compiler.assembly.levels

import scala.annotation.tailrec
import manticore.compiler.AssemblyContext

/**
  * Deconstruct ParMux instructions into a sequence of Muxes
  *
  * @author Mahyar Emami <mahyar.emami@epfl.ch>
  */
trait ParMuxDeconstruction extends Flavored {

  import flavor._

  // a wrapper class for a sequence of Mux instructions resulted from
  // deconstructing a parallel Mux
  private case class MuxSeq(instructions: Seq[Mux], tempWires: Seq[DefReg])

  def mkWire(orig: DefReg)(implicit ctx: AssemblyContext): DefReg

  /** Deconstruct a ParMux into a linear sequence of Mux instructions
    *
    * @param pmux
    * @param newWire
    *   a function to generate fresh register definitions for temporary values
    *   in the Mux tree
    * @return
    */
  private def deconstruct(pmux: ParMux)(newWire: => DefReg): MuxSeq = {
    require(pmux.choices.length >= 1, s"ill-formed instruction ${pmux}")
    if (pmux.choices.length == 1) {
      MuxSeq(
        Seq(
          Mux(
            pmux.rd,
            pmux.choices.head.condition,
            pmux.default,
            pmux.choices.head.choice
          ).setPos(pmux.pos)
        ),
        Seq.empty
      )
    } else {

      @tailrec
      def mkMuxSeq(choices: Seq[ParMuxCase], tree: MuxSeq): MuxSeq = {

        if (choices.length == 1) {
          val newMux = Mux(
            pmux.rd,
            choices.head.condition,
            tree.instructions.last.rd,
            choices.head.choice
          ).setPos(pmux.pos)
          MuxSeq(
            instructions = tree.instructions :+ newMux,
            tempWires = tree.tempWires
          )
        } else {
          val newDef = newWire
          val newMux = Mux(
            newDef.variable.name,
            choices.head.condition,
            tree.instructions.last.rd,
            choices.head.choice
          ).setPos(pmux.pos)
          val newTree = MuxSeq(
            instructions = tree.instructions :+ newMux,
            tempWires = tree.tempWires :+ newDef
          )
          mkMuxSeq(choices.tail, newTree)
        }

      }

      val firstDef = newWire
      val firstMux = MuxSeq(
        Seq(
          Mux(
            firstDef.variable.name,
            pmux.choices.head.condition,
            pmux.default,
            pmux.choices.head.choice
          ).setPos(pmux.pos)
        ),
        Seq(
          firstDef
        )
      )
      mkMuxSeq(pmux.choices.tail, firstMux)

    }
  }

  def do_transform(
      process: DefProcess
  )(implicit ctx: AssemblyContext): DefProcess = {

    val regLookup = process.registers.map { r => r.variable.name -> r }.toMap
    case class TransformBuilder(
        block: Seq[Instruction] = Seq.empty,
        newDefs: Seq[DefReg] = Seq.empty
    ) {
      def add(inst: Instruction) = copy(
        block = block :+ inst
      )
      def add(muxSeq: MuxSeq) = copy(
        block = block ++ muxSeq.instructions,
        newDefs = newDefs ++ muxSeq.tempWires
      )
    }

    val transformed = process.body.foldLeft(TransformBuilder()) {
      case (builder, inst) =>
        inst match {
          case pmux: ParMux =>
            builder add deconstruct(pmux) {
              mkWire(regLookup(pmux.rd))
            }
          case i => builder add i
        }
    }

    process
      .copy(
        body = transformed.block,
        registers = process.registers ++ transformed.newDefs
      )
      .setPos(process.pos)

  }

}
