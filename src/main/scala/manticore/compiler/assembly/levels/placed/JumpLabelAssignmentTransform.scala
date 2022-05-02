package manticore.compiler.assembly.levels.placed

import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.DependenceGraphBuilder
import manticore.compiler.assembly.ManticoreAssemblyIR
import com.sourcegraph.semanticdb_javac.Result
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import scala.collection.immutable
import scala.annotation.tailrec
import manticore.compiler.assembly.levels.UInt16

/**
  * Assign resolve named labels to concrete "PC" values and fixup any [[BreakCase]]
  * target
  *
  * @author Mahyar Emami <mahyar.emami@epfl.ch>
  */
object JumpLabelAssignmentTransform
    extends AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram] {
  import PlacedIR._
  import TaggedInstruction._

  override def transform(
      source: DefProgram,
      context: AssemblyContext
  ): DefProgram = source
    .copy(
      processes = source.processes.map { assignLabels(_)(context) }
    )
    .setPos(source.pos)

  def assignLabels(
      process: DefProcess
  )(implicit ctx: AssemblyContext): DefProcess = {

    val taggedBlock = Try(TaggedInstruction.indexedTaggedBlock(process)) match {
      case Success(result) => result
      case Failure(InstructionUntaggablException(msg)) =>
        ctx.logger.error("msg")
        IndexedSeq.empty
      case Failure(exception) => throw exception
    }

    def hasJumpTargetTag(taggedInstr: TaggedInstruction): Boolean =
      taggedInstr.tags.exists { _.isInstanceOf[JumpTarget] }

    def isBreakTarget(taggedInstr: TaggedInstruction): Boolean =
      taggedInstr.tags.exists { _ == BreakTarget }

    val breakTargets = taggedBlock.zipWithIndex.collect {
      case (taggedInstr, pc) if isBreakTarget(taggedInstr) => pc
    }

    def setBreakTargets(jtb: JumpTable, target: Int): JumpTable = {
      jtb
        .copy(blocks = jtb.blocks.map { case JumpCase(lbl, blk) =>
          JumpCase(
            lbl,
            blk.map {
              case brk: BreakCase => brk.copy(target = target).setPos(brk.pos)
              case i              => i
            }
          )
        })
        .setPos(jtb.pos)
    }

    @tailrec
    def consumeBreakTarget(
        body: Seq[Instruction],
        targets: Seq[Int],
        newBody: Seq[Instruction] = Seq.empty
    ): Seq[Instruction] = {
      body match {
        case (head: JumpTable) :: next =>
          assert(targets.nonEmpty, "Not enough break targets!")
          consumeBreakTarget(
            next,
            targets.tail,
            newBody :+ setBreakTargets(head, targets.head)
          )
        case head :: next =>
          consumeBreakTarget(next, targets, newBody :+ head)
        case Nil => newBody
      }
    }

    val labelMap = taggedBlock.zipWithIndex.collect {
      case (taggedInstr, pc) if hasJumpTargetTag(taggedInstr) =>
        val jTarget = taggedInstr.tags.collect { case t: JumpTarget => t }
        assert(
          jTarget.length == 1,
          "A jump target should have a single predecessor!"
        )
        jTarget.head.label -> pc
    }

    def getLabelPc(label: Label): UInt16 = labelMap.find(_._1 == label) match {
      case None =>
        ctx.logger.error(s"Could not resolve label ${label}!")
        UInt16(0xffff)
      case Some((_, value)) =>
        UInt16(value)
    }
    val newBody = consumeBreakTarget(process.body, breakTargets)

    val memories = process.registers.collect {
      case DefReg(m: MemoryVariable, _, _) => m
    }
    // create the memory contents for each label group
    def createMemoryContent(labelGroup: DefLabelGroup): Seq[UInt16] = {

      implicit object LabelOrdering extends Ordering[(UInt16, Label)] {
        override def compare(x: (UInt16, Label), y: (UInt16, Label)): Int =
          Ordering[Int].compare(x._1.toInt, y._1.toInt)
      }
      val sortedLabels = labelGroup.indexer.sorted
      labelGroup.default match {
        case None => // there is no default/catch all case
          // we need to make sure when there is no default case, the label indexer
          // are consecutive range of numbers
          val isLinearRange = sortedLabels.sliding(2).forall { case x =>
            x.tail.head._1 - x.head._1 == UInt16(1)
          }
          if (!isLinearRange || sortedLabels.head._1 != UInt16(0)) {
            ctx.logger.error(
              "Labels are not in a linear range starting from 0!",
              labelGroup
            )
          }

          val content = sortedLabels.map { case (index, lbl) =>
            getLabelPc(lbl)
          }
          content
        case Some(defaultLabel) =>
          // there is default "catch all label", this means we need to fill the
          // whole memory. In the absence of default that is not the case because
          // we are guaranteed by the JumpTableConstructionTransform not to ever
          // lookup anything other than the labels defined in the indexer
          val cap = memories.collectFirst {
            case MemoryVariable(n, _, block) if n == labelGroup.memory => block
          } match {
            case Some(block) => block.capacity
            case None =>
              ctx.logger.error(
                s"Failed resolving labels! Could not look up capacity of ${labelGroup.memory}.",
                labelGroup
              )
              0
          }
          // the indexer has the following form:
          // (x -> l1)
          // (x + n1 -> l2) // n1 >= 1
          // (x + n2 -> l3) // n2 >= n1 + 1
          // ...
          // and we need to turn it into something like:
          // 0 -> ll1
          // 1 -> ll2
          // 2 -> ll3
          // ...
          // where each ll can be either the default label or a real one if
          // in i -> lli we have i in indexer
          val content = Seq.tabulate(cap) { ix =>
            sortedLabels.find { case (ixx, label) => ix == ixx.toInt } match {
              case None => // use the default label
                getLabelPc(defaultLabel)
              case Some((_, label)) =>
                getLabelPc(label)
            }
          }

          content

      }
    }

    val newRegs = process.registers.map {
      case r @ DefReg(m: MemoryVariable, _, _) =>
        process.labels.find { lblgrp => lblgrp.memory == m.name } match {
          case Some(grp) =>
            val content = createMemoryContent(grp)
            r.copy(
              variable = m.copy(
                block = m.block.copy(initial_content = content)
              )
            ).setPos(r.pos)
          case None => // normal memory
            r
        }
      case r => r

    }

    process
      .copy(
        body = newBody,
        registers = newRegs
      )
      .setPos(process.pos)

  }

}
