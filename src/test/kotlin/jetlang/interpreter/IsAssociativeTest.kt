package jetlang.interpreter

import jetlang.parser.NumberLiteral
import jetlang.parser.Operation
import jetlang.parser.Operator
import jetlang.parser.Reduce
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import kotlin.test.Test

class IsAssociativeTest {
    // true
    @Test
    fun addition() = runTest  {
        assertEquals(
            true,
            Operation(NumberLiteral(1), Operator.ADD, NumberLiteral(1)).accept(IsAssociative)
        )
    }

    @Test
    fun multiplication() = runTest  {
        assertEquals(
            true,
            Operation(NumberLiteral(1), Operator.MULTIPLY, NumberLiteral(1)).accept(IsAssociative)
        )
    }

    // false
    @Test
    fun subtraction() = runTest  {
        assertEquals(
            false,
            Operation(NumberLiteral(1), Operator.SUBTRACT, NumberLiteral(1)).accept(IsAssociative)
        )
    }

    @Test
    fun division() = runTest  {
        assertEquals(
            false,
            Operation(NumberLiteral(1), Operator.SUBTRACT, NumberLiteral(1)).accept(IsAssociative)
        )
    }

    @Test
    fun exponentiation() = runTest  {
        assertEquals(
            false,
            Operation(NumberLiteral(1), Operator.EXPONENT, NumberLiteral(1)).accept(IsAssociative)
        )
    }

    @Test
    fun reduce() = runTest  {
        assertEquals(
            false,
            Reduce(NumberLiteral(1), NumberLiteral(1), "a", "b", NumberLiteral(1)).accept(
                IsAssociative
            )
        )
    }
}