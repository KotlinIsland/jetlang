package jetlang.interpreter

import jetlang.parser.NumberLiteral
import jetlang.parser.Operation
import jetlang.parser.Operator
import jetlang.parser.Reduce
import org.junit.jupiter.api.Assertions.*
import kotlin.test.Test

class IsAssociativeTest {
    // true
    @Test
    fun addition() {
        assertEquals(
            true,
            Operation(NumberLiteral(1), Operator.ADD, NumberLiteral(1)).accept(IsAssociative)
        )
    }

    @Test
    fun multiplication() {
        assertEquals(
            true,
            Operation(NumberLiteral(1), Operator.MULTIPLY, NumberLiteral(1)).accept(IsAssociative)
        )
    }

    // false
    @Test
    fun subtraction() {
        assertEquals(
            false,
            Operation(NumberLiteral(1), Operator.SUBTRACT, NumberLiteral(1)).accept(IsAssociative)
        )
    }

    @Test
    fun division() {
        assertEquals(
            false,
            Operation(NumberLiteral(1), Operator.SUBTRACT, NumberLiteral(1)).accept(IsAssociative)
        )
    }

    @Test
    fun exponentiation() {
        assertEquals(
            false,
            Operation(NumberLiteral(1), Operator.EXPONENT, NumberLiteral(1)).accept(IsAssociative)
        )
    }

    @Test
    fun reduce() {
        assertEquals(
            false,
            Reduce(NumberLiteral(1), NumberLiteral(1), "a", "b", NumberLiteral(1)).accept(
                IsAssociative
            )
        )
    }
}