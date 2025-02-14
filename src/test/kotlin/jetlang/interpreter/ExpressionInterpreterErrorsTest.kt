package jetlang.interpreter

import assertInterpretsAs
import jetlang.parser.*
import jetlang.types.NumberJL
import jetlang.types.RangeSequenceJL
import jetlang.types.SequenceJL
import jetlang.types.Value
import jetlang.utility.minus
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import jetlang.utility.*
import java.math.BigInteger

suspend fun interpretExpressionError(
    expression: Expression,
    names: Map<String, Value> = emptyMap()
) =
    (expression.accept(ExpressionInterpreter(names)) as InterpreterResult.Error).value

val sequenceLiteral
    get() = SequenceLiteral(NumberLiteral(BigDecimal.ONE), NumberLiteral(BigDecimal.TWO))

class ExpressionInterpreterErrorsTest {
    @Test
    fun `test undefined variable`() = runTest {
        assertEquals(
            "Variable \"a\" not defined",
            interpretExpressionError(Identifier("a"))
        )
    }

    @Test
    fun `test sequence start not number`() = runTest {
        assertEquals(
            "Sequence start value is not a number: {1, 2}",
            interpretExpressionError(
                SequenceLiteral(sequenceLiteral, NumberLiteral(BigDecimal.TWO))
            )
        )
    }

    @Test
    fun `test sequence start not int`() = runTest {
        assertEquals(
            "Sequence start value is not an integer: 1.5",
            interpretExpressionError(
                SequenceLiteral(
                    NumberLiteral(1.5.toBigDecimal()), NumberLiteral(BigDecimal.TWO)
                )
            )
        )
    }

    @Test
    fun `test sequence end not number`() = runTest  {
        assertEquals(
            "Sequence end value is not a number: {1, 2}",
            interpretExpressionError(
                SequenceLiteral(
                    NumberLiteral(BigDecimal.ONE), sequenceLiteral
                )
            )
        )
    }

    @Test
    fun `test sequence end not int`() = runTest  {
        assertEquals(
            "Sequence end value is not an integer: 1.5",
            interpretExpressionError(
                SequenceLiteral(
                    NumberLiteral(BigDecimal.ONE), NumberLiteral(1.5.toBigDecimal())
                )
            )
        )
    }

    @Test
    fun `test sequence start equal`() = runTest  {
        SequenceLiteral(
            NumberLiteral(BigDecimal.ONE),
            NumberLiteral(BigDecimal.ONE)
        ) assertInterpretsAs RangeSequenceJL(BigInteger.ONE, BigInteger.ONE)
    }

    @Test
    fun `test sequence start greater`() = runTest  {
        assertEquals(
            "Sequence start value is greater than end value: {2, 1}",
            interpretExpressionError(
                SequenceLiteral(NumberLiteral(BigDecimal.TWO), NumberLiteral(BigDecimal.ONE))
            )
        )
    }

    @Test
    fun `test operation not number left`() = runTest  {
        assertEquals(
            "for left operand expected NumberJL, got {1, 1}",
            interpretExpressionError(
                    SequenceLiteral(NumberLiteral(1), NumberLiteral(1))
+                    NumberLiteral(1),
            )
        )
    }

    @Test
    fun `test operation not number right`() = runTest  {
        assertEquals(
            "for right operand expected NumberJL, got {1, 1}",
            interpretExpressionError(
                    NumberLiteral(1) +
                    SequenceLiteral(NumberLiteral(1), NumberLiteral(1)),
            )
        )
    }

    @Test
    fun `raising to a decimal isn't supported`() = runTest {
        assertEquals(
            "Raising to a decimal is not supported",
            interpretExpressionError(
                    NumberLiteral(1) pow
                    NumberLiteral(BigDecimal(1.1)),
            )
        )
    }

    @Test
    fun `reduce takes a sequence`() = runTest  {
        assertEquals(
            "Input value expected SequenceJL, got 1",
            interpretExpressionError(
                Reduce(
                    NumberLiteral(1), Identifier("b"), "c", "d",
                    Identifier("c")
                )
            )
        )
    }

    @Test
    fun `reduce is associative`() = runTest  {
        assertEquals(
            "The lambda expression of `reduce` must be an associative operation",
            interpretExpressionError(
                Reduce(
                    SequenceLiteral(NumberLiteral(1), NumberLiteral(1)), NumberLiteral(0), "a", "b",
                    Identifier("a") - Identifier("b")
                )
            )
        )
    }

    @Test
    fun `reduce lambda can't see globals`() = runTest  {
        val a = Identifier("a")
        assertEquals(
            "Variable \"a\" not defined",
            interpretExpressionError(
                Reduce(
                    SequenceLiteral(NumberLiteral(1), NumberLiteral(1)), a, "c", "d",
                    a,
                ),
                mapOf("a" to NumberJL(1)),
            )
        )
    }

    @Test
    fun `map input type`() = runTest  {
        assertEquals(
            "Input for `map` expected SequenceJL, got 1",
            interpretExpressionError(
                MapJL(
                    NumberLiteral(1), "a",
                    NumberLiteral(1)
                ),
            )
        )
    }

    @Test
    fun `map raises error`() = runTest  {
        assertEquals(
            "Sequence start value is greater than end value: {2, 1}",
            interpretExpressionError(
                MapJL(
                    SequenceLiteral(NumberLiteral(1), NumberLiteral(2)),
                    "a",
                    SequenceLiteral(
                        NumberLiteral(2),
                        NumberLiteral(1)
                    ),
                )
            )
        )
    }

    @Test
    fun `map lambda can't see globals`() = runTest {
        val a = Identifier("a")
        assertEquals(
            "Variable \"a\" not defined",
            interpretExpressionError(
                MapJL(
                    SequenceLiteral(NumberLiteral(1), NumberLiteral(1)),
                    "it",
                    a,
                ),
                mapOf("a" to NumberJL(1)),
            )
        )
    }

    @Test
    fun `map must return a number`() = runTest {
        assertEquals(
            "Map lambda resul expected NumberJL, got {1, 1}",
            interpretExpressionError(
                MapJL(
                    SequenceLiteral(NumberLiteral(1), NumberLiteral(1)),
                    "it",
                    SequenceLiteral(NumberLiteral(1), NumberLiteral(1)),
                ),
                mapOf("a" to NumberJL(1)),
            )
        )
    }
}
