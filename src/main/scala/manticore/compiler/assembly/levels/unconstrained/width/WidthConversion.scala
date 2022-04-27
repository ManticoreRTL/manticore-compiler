package manticore.compiler.assembly.levels.unconstrained.width

import manticore.compiler.assembly.levels.AssemblyTransformer
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedRenameVariables
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedCloseSequentialCycles
import manticore.compiler.assembly.levels.unconstrained.UnconstrainedBreakSequentialCycles
/**
  * WidthConversion transform
  *
  * @author Mahyar Emami <mahyar.emami@epfl.ch>
  */
object WidthConversion {


  // The WidthConversion pass consists of multiple steps.
  // The core pass goes through instructions one by one and converts them to
  // multiple instructions and turn every name into multiple names. Sadly enough
  // the pass outputs code that is not SSA. Hence we need to run UnconstrainedRenameVariables
  // to make the code SSA. Furthermore, only lazily creates new registers when
  // it sees a register either at use or def positions. This means that if there
  // is an InputType registers that is never read (i.e., consider a counter register
  // which the user only expects to read it in the VCD dumps!) then this register
  // does not get translated and hence gets removed. To avoid this, we need to
  // run UnconstrainedCloseSequentialCycles before and UnconstrainedBreakSequentialCycles
  // to make sure such registers are at least written to once and hence conversion
  // applies to them.

  val core = WidthConversionCore
  val transformation =
    UnconstrainedCloseSequentialCycles followedBy
      core followedBy
      UnconstrainedBreakSequentialCycles followedBy
      UnconstrainedRenameVariables

}
