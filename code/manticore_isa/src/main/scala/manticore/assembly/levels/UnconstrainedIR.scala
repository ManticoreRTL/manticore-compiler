package manticore.assembly.levels

import manticore.assembly.ManticoreAssemblyIR


/** Raw assembly, with possible bit slices and wide bit vectors (e.g., 128-bit
  * addition)
  */
object UnconstrainedIR extends ManticoreAssemblyIR {
  case class LogicVariable(
      name: String,
      slice: (Int, Int),
      tpe: LogicType,
  )
  type Constant = BigInt // unlimited bits
  type Variable = LogicVariable
  type CustomFunction = Seq[BigInt] // unlimited bits
  type Name = String
  type ProcessId = String
  type ExceptionId = Int
}

// object UnvectorizedAssembly extends ManticoreAssemblyIR {

//   case class LogicVariable(name: String, width: Int, tpe: LogicType)

//   type Constant = Int // limited to 16 bits
//   type Variable = LogicVariable
//   type CustomFunction = Int // limited to 16 bits
//   type Name = String
//   type ProcessId = Int
//   type ExceptionId = Int

// }

// object VectorizedAssembly {}
