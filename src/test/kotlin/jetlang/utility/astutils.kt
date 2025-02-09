package jetlang.utility

import jetlang.parser.Expression
import jetlang.parser.Operation
import jetlang.parser.Operator

operator fun Expression.plus(other: Expression) =
    Operation(this, Operator.ADD, other)

operator fun Expression.minus(other: Expression) =
    Operation(this, Operator.SUBTRACT, other)

operator fun Expression.times(other: Expression) =
    Operation(this, Operator.MULTIPLY, other)

operator fun Expression.div(other: Expression) =
    Operation(this, Operator.DIVIDE, other)

infix fun Expression.pow(other: Expression) =
    Operation(this, Operator.EXPONENT, other)
