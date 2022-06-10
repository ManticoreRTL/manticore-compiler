package manticore.compiler.integration.yosys.unit.tiny.operators

/**
  * A bunch of unit tests to make sure code generated by Yosys behaves correctly
  * before doing any compiler transformation. Note that although tests are minimal
  * verilog codes, they take a bit of time to complete since we have to compile
  * them using yosys, execute verilator simulation and then interpret them inside
  * [[UnconstrainedInterpreter]]. So don't expect them to finish fast.
  *
  */



// group 1
class LessThanTester extends BinaryOperatorTestGenerator {
    override def operator: String = "<"
}


class LessThanEqualTester extends BinaryOperatorTestGenerator {
    override def operator: String = "<="
}


class EqualTester extends BinaryOperatorTestGenerator {
    override def operator: String = "=="
}


class NotEqualTester extends BinaryOperatorTestGenerator {
    override def operator: String = "!="
}



class GreaterThanTester extends BinaryOperatorTestGenerator {
    override def operator: String = ">"
}

class GreaterThanEqualTester extends BinaryOperatorTestGenerator {
    override def operator: String = ">="
}



// group 2


class AddTester extends BinaryOperatorTestGenerator {
    override def operator: String = "+"
}

class SubTester extends BinaryOperatorTestGenerator {
    override def operator: String = "-"
}

class MulTester extends BinaryOperatorTestGenerator {

    override def operator: String = "*"
}


class AndTester extends BinaryOperatorTestGenerator {
    override def operator: String = "&"
}


class OrTester extends BinaryOperatorTestGenerator {
    override def operator: String = "|"
}


class XorTester extends BinaryOperatorTestGenerator {
    override def operator: String = "^"
}

class XnorTester extends BinaryOperatorTestGenerator {
    override def operator: String = "~^"
}


// group 3

class ShiftLeftTester extends BinaryOperatorTestGenerator {
    override def operator: String = "<<"
    override def maxShiftBits: Option[Int] = Some(16)
}


class ShiftLeftSignedTester extends BinaryOperatorTestGenerator {
    override def operator: String = "<<<"
    override def maxShiftBits: Option[Int] = Some(16)
}



class ShiftRightTester extends BinaryOperatorTestGenerator {
    override def operator: String = ">>"
    override def maxShiftBits: Option[Int] = Some(16)
}



class ShiftRightSignedTester extends BinaryOperatorTestGenerator {
    override def operator: String = ">>>"
    override def maxShiftBits: Option[Int] = Some(16)
}


class LogicAndTester extends BinaryOperatorTestGenerator {
    override def operator: String = "&&"
}



class LogicOrTester extends BinaryOperatorTestGenerator {
    override def operator: String = "||"
}

// enable them later
// class EqualXTester extends BinaryOperatorTestGenerator {
//     override def operator: String = "==="
// }


// class NotEqualXTester  extends BinaryOperatorTestGenerator {
//     override def operator: String = "!=="
// }

