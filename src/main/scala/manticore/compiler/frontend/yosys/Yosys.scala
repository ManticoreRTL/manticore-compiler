package manticore.compiler.frontend.yosys

object Yosys {

  import Implicits.passProxyToTransformation
  val Hierarchy = YosysPassProxy("hierarchy")
  val Proc = YosysPassProxy("proc")
  val Opt = YosysPassProxy("opt")
  val OptReduce = YosysPassProxy("opt_reduce")
  val OptClean = YosysPassProxy("opt_clean")
  val Flatten = YosysPassProxy("flatten")
  val Check = YosysPassProxy("check")
  val MemoryCollect = YosysPassProxy("memory_collect")
  val MemoryUnpack = YosysPassProxy("memory_unpack") runsAfter MemoryCollect
  val RtlIlWriter = YosysPassProxy("write_rtlil")
  val Select = YosysPassProxy("select")

  val ManticoreInit = YosysPassProxy("manticore_init") runsAfter Check
  val ManticoreMemInit = YosysPassProxy("manticore_meminit")

  val ManticoreDff = YosysPassProxy("manticore_dff") runsAfter ManticoreInit
  val ManticoreOptReplicate =
    YosysPassProxy("manticore_opt_replicate") runsAfter ManticoreDff
  val ManticoreSubword =
    YosysPassProxy("manticore_subword") runsAfter ManticoreOptReplicate
  val ManticoreCheck = YosysPassProxy(
    "manticore_check"
  ) runsAfter (Flatten, ManticoreDff, MemoryUnpack)

  val PreparationPasses =
    (Hierarchy << "-auto-top" << "-check") andThen
      Proc andThen
      Opt andThen
      OptReduce andThen
      OptClean andThen
      (Check << "-assert") andThen
      MemoryCollect andThen MemoryUnpack

  val LoweringPasses = ManticoreInit andThen
    Flatten andThen
    ManticoreMemInit andThen
    Opt andThen
    ManticoreDff andThen
    ManticoreOptReplicate andThen
    ManticoreSubword andThen
    ManticoreCheck

  val YosysDefaultPassAggregator =
    YosysVerilogReader andThen PreparationPasses andThen LoweringPasses

}
