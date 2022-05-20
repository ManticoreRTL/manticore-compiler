package manticore.compiler.integration.yosys.unit.tiny.operators




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
}


class ShiftLeftSignedTester extends BinaryOperatorTestGenerator {
    override def operator: String = "<<<"
}



class ShiftRightTester extends BinaryOperatorTestGenerator {
    override def operator: String = ">>"
}



class ShiftRightSignedTester extends BinaryOperatorTestGenerator {
    override def operator: String = ">>>"
}


class LogicAndTester extends BinaryOperatorTestGenerator {
    override def operator: String = "&&"
}



class LogicOrTester extends BinaryOperatorTestGenerator {
    override def operator: String = "||"
}

class EqualXTester extends BinaryOperatorTestGenerator {
    override def operator: String = "==="
}


class NotEqualXTester  extends BinaryOperatorTestGenerator {
    override def operator: String = "!=="
}

