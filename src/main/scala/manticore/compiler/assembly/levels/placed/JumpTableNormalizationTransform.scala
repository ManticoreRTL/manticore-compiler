package manticore.compiler.assembly.levels.placed

import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.AssemblyContext
import manticore.compiler.assembly.DependenceGraphBuilder
import manticore.compiler.assembly.levels.AssemblyNameChecker
import manticore.compiler.assembly.levels.WireType

/** Transforms every [[JumpTable]] in the program to a "normal form". See
  * [[normalize]] for details.
  *
  * @author
  *   Mahyar Emami <mahyar.emami@epfl.ch>
  */
object JumpTableNormalizationTransform
    extends AssemblyTransformer[PlacedIR.DefProgram, PlacedIR.DefProgram]
    with DependenceGraphBuilder
    with AssemblyNameChecker {
  val flavor = PlacedIR
  import flavor._

  object JumpTableProperties {

    def jumpTableContainsAllDefinitionsUsedInPhis(
        jtb: JumpTable
    ): Boolean =
      jtb.results.forall { case Phi(rd, rsx) =>
        rsx.forall { case (lbl, rs) =>
          jtb.blocks.forall { case JumpCase(l, blk) =>
            if (l == lbl)
              blk.exists { inst =>
                DependenceAnalysis.regDef(inst).contains(rs)
              }
            else true
          }

        }
      }
    def jumpTableHasBreakCase(jtb: JumpTable): Boolean =
      jtb.blocks.forall { case JumpCase(_, blk) =>
        blk.count { _.isInstanceOf[BreakCase] == true } == 1
      }
  }

  /** Turns every jump table in "the normal form". A jump table jtb in normal
    * form has the following properties:
    *   - Process in SSA, i.e., [[NameCheck.checkSSA]]
    *   - [[jumpTableContainsAllDefinitionsUsedInPhis(jtb)]]
    *   - Every case has a single [[BreakCase]] This means that normal form
    *     [[JumpTable]] can not have empty case blocks. It is quiet possible
    *     that the normal form is not optimal for performance, and hence
    *     optimization passes will likely destroy the normal form. This
    *     transformation should only be done at the very end
    *
    * Note that we don't check for SSAness, that is something another pass
    * should do.
    * @param process
    * @param ctx
    * @return
    */
  private def normalize(jtb: JumpTable, aliasOf: (Label, Name) => Name)(implicit
      ctx: AssemblyContext
  ): JumpTable = {

    def patchBreakCases(jump: JumpTable): JumpTable = {
      if (!JumpTableProperties.jumpTableHasBreakCase(jump)) {
        val blocksWithBreak = jump.blocks.map { case JumpCase(lbl, blk) =>
          JumpCase(
            lbl,
            blk.filter { _.isInstanceOf[BreakCase] == false } :+ BreakCase()
          )
        }
        jump
          .copy(
            blocks = blocksWithBreak
          )
          .setPos(jump.pos)
      } else {
        jump
      }
    }

    def patchDefinitions(jump: JumpTable): JumpTable = {
      if (!JumpTableProperties.jumpTableContainsAllDefinitionsUsedInPhis(jtb)) {
        // a map from labels to the sequence of names in that label that
        // are used in the Phis
        val phiContributors = jump.results
          .flatMap { case Phi(rd, rsx) =>
            rsx.map { case (lbl, rs) => (lbl, rs) }
          }
          .groupBy(_._1)
          .view
          .mapValues(_.map(_._2))

        def getAliases(jCase: JumpCase) = {

          val isDefineInCaseBody = jCase.block.foldLeft(Set.empty[Name]) {
            _ ++ DependenceAnalysis.regDef(_)
          }
          // now for any rs in contributesToPhi(jCase.label)
          val definedOutSide = phiContributors(jCase.label).filter {
            !isDefineInCaseBody(_)
          }
          val aliases = definedOutSide.distinct.map { origName =>
            origName -> aliasOf(jCase.label, origName)
          }
          aliases

        }
        val aliases = jump.blocks.map { case jCase @ JumpCase(lbl, _) =>
          lbl -> getAliases(jCase)
        }.toMap
        val blocksWithAliasing =
          jump.blocks.map { jCase =>
            jCase.copy(block = jCase.block ++ aliases(jCase.label).map {
              case (origN, newN) => Mov(newN, origN)
            })
          }

        def phiRename(label: Label, rs: Name): Name =
          aliases(label).collectFirst { case (orig, als) if orig == rs => als}.getOrElse(rs)

        val newPhis = jump.results.map { case Phi(rd, rsx) =>
          Phi(
            rd,
            rsx.map { case (lbl, rs) => (lbl, phiRename(lbl, rs)) }
          )
        }
        jump.copy(
          blocks = blocksWithAliasing,
          results = newPhis
        )
      } else {
        jump
      }

    }
    patchDefinitions(patchBreakCases(jtb))
  }

  /** Turns every jump table to "the normal form". A jump table jtb in normal
    * form has the following properties:
    *   - Process in SSA: see [[NameCheck.checkSSA]]
    *   - [[jumpTableContainsAllDefinitionsUsedInPhis(jtb)]]
    *   - Every case has a single [[BreakCase]] This means that normal form
    *     [[JumpTable]] can not have empty case blocks. It is quiet possible
    *     that the normal form is not optimal for performance, and hence
    *     optimization passes will likely destroy the normal form. This
    *     transformation should only be done at the very end
    *
    * Note that we don't check for SSAness, that is something another pass
    * should do.
    * @param process
    * @param ctx
    * @return
    */
  def normalize(
      process: DefProcess
  )(implicit ctx: AssemblyContext): DefProcess = {

    val nonSSAs = NameCheck.collectNonSSA(process)

    if (nonSSAs.nonEmpty) {
      ctx.logger.error(
        s"Process ${process.id} is not in SSA form, please make sure your passes do no break SSAness"
      )
      process
    } else {

      val scopedSubst = scala.collection.mutable.Map.empty[(Label, Name), Name]
      def mkAlias(label: Label, original: Name): Name =
        scopedSubst.get((label, original)) match {
          case None =>
            val newName = s"%w${ctx.uniqueNumber()}"
            scopedSubst += (label, original) -> newName
            newName
          case Some(alias) => alias
        }
      val newBody = process.body.map {
        case jtb: JumpTable => normalize(jtb, mkAlias)
        case i              => i
      }
      val flatSubst = scopedSubst.map { case ((lbl, orig) -> alias) =>
        orig -> alias
      }
      def hasAlias(orig: Name): Boolean = flatSubst.exists { orig == _._1 }
      val newRegs = process.registers.flatMap { r =>
        flatSubst.collect {
          case (orig, alias) if orig == r.variable.name =>
            DefReg(
              ValueVariable(alias, -1, WireType),
              None
            ).setPos(r.pos)
        }.toSeq :+ r
      }
      process.copy(
        body = newBody,
        registers = newRegs
      )
    }

  }

  override def transform(
      program: DefProgram,
      context: AssemblyContext
  ): DefProgram =
    program.copy(processes = program.processes.map(p => normalize(p)(context)))

}

// trait Transformer[T, S]  {

//   def andThen[R](g: Transformer[T, R]): Transformer[T, R] =
// }
