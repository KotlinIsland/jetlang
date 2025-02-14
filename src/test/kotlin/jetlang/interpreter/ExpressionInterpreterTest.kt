import jetlang.fraction.Fraction
import jetlang.interpreter.ExpressionInterpreter
import jetlang.interpreter.InterpreterResult
import jetlang.interpreter.InterpreterResult.Success
import jetlang.parser.Expression
import jetlang.parser.Identifier
import jetlang.parser.MapJL
import jetlang.parser.NumberLiteral
import jetlang.parser.Operation
import jetlang.parser.Operator
import jetlang.parser.Reduce
import jetlang.parser.SequenceLiteral
import jetlang.types.NumberJL
import jetlang.types.RangeSequenceJL
import jetlang.types.SequenceJL
import jetlang.types.Value
import jetlang.utility.*
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import kotlin.test.*

fun <TValue : Value> InterpreterResult<TValue>.get() = when (this) {
    is Success -> value
    is InterpreterResult.Error -> throw RuntimeException("Interpreter error: $value")
}

suspend infix fun Expression.assertInterpretsAs(expected: Value) = assertEquals(
    expected, (accept(ExpressionInterpreter(emptyMap())) as Success).value
)

class ExpressionInterpreterTest {
    @Test
    fun visitNumberLiteral() = runTest {
        val expressionValue = BigDecimal.ONE
        assertEquals(
            NumberJL(expressionValue),
            NumberLiteral(expressionValue).accept(ExpressionInterpreter(emptyMap())).get()
        )
    }

    @Test
    fun visitIdentifier() = runTest {
        val expressionValue = BigDecimal.ONE
        assertEquals(
            NumberJL(expressionValue),
            NumberLiteral(expressionValue).accept(ExpressionInterpreter(emptyMap())).get()
        )
    }

    @Test
    fun visitSequenceLiteral() = runTest {
        val expressionValue = BigDecimal.ONE
        assertEquals(
            NumberJL(expressionValue),
            NumberLiteral(expressionValue).accept(ExpressionInterpreter(emptyMap())).get()
        )
    }

    @Test
    fun `test add operation`() = runTest {
        NumberLiteral(1) + NumberLiteral(2) assertInterpretsAs NumberJL(3)
    }

    @Test
    fun `test subtract operation`() = runTest {
        NumberLiteral(3) - NumberLiteral(2) assertInterpretsAs NumberJL(1)
    }

    @Test
    fun `test multiply operation`() = runTest {
        NumberLiteral(3) * NumberLiteral(2) assertInterpretsAs NumberJL(6)
    }

    @Test
    fun `divide operation`() = runTest {
        NumberLiteral(6) / NumberLiteral(3) assertInterpretsAs NumberJL(2)
    }

    @Test
    fun `divide operation with decimal`() = runTest {
        NumberLiteral(4) / NumberLiteral(3) assertInterpretsAs NumberJL(Fraction(4, 3))
    }

    @Test
    fun `test exponent operation`() = runTest {
        NumberLiteral(3) pow NumberLiteral(2) assertInterpretsAs NumberJL(9)
    }

    @Test
    fun `reduce one arg`() = runTest {
        Reduce(
            SequenceLiteral(NumberLiteral(2), NumberLiteral(2)),
            NumberLiteral(1),
            "a",
            "b",
            Identifier("a") * Identifier("b"),
        ) assertInterpretsAs NumberJL(2)
    }

    @Test
    fun `reduce two args`() = runTest {
        Reduce(
            SequenceLiteral(NumberLiteral(2), NumberLiteral(3)),
            NumberLiteral(1),
            "a",
            "b",
            Identifier("a") * Identifier("b"),
        ) assertInterpretsAs NumberJL(6)
    }

    @Test
    fun `reduce three args`() = runTest {
        Reduce(
            SequenceLiteral(NumberLiteral(2), NumberLiteral(4)),
            NumberLiteral(1),
            "a",
            "b",
            Identifier("a") * Identifier("b"),
        ) assertInterpretsAs NumberJL(24)
    }

    @Test
    fun `reduce range sum optimization`() = runTest {
        Reduce(
            SequenceLiteral(NumberLiteral(5), NumberLiteral(10)),
            NumberLiteral(7),
            "a",
            "b",
            Identifier("a") + Identifier("b"),
        ) assertInterpretsAs NumberJL(52)
    }

    @Test
    fun `reduce range sum one`() = runTest {
        Reduce(
            SequenceLiteral(NumberLiteral(5), NumberLiteral(5)),
            NumberLiteral(7),
            "a",
            "b",
            Identifier("a") + Identifier("b"),
        ) assertInterpretsAs NumberJL(12)
    }

    @Test
    fun `reduce range sum optimization large`() = runTest {
        Reduce(
            SequenceLiteral(NumberLiteral(0), NumberLiteral(50000)),
            NumberLiteral(0),
            "a",
            "b",
            Identifier("a") + Identifier("b"),
        ) assertInterpretsAs NumberJL(1250025000)
    }

    @Test
    fun map() = runTest {
        MapJL(
            SequenceLiteral(NumberLiteral(1), NumberLiteral(3)),
            "a",
            Identifier("a") * NumberLiteral(2),
        ) assertInterpretsAs SequenceJL(listOf(NumberJL(2), NumberJL(4), NumberJL(6)))
    }

    @Test
    fun `map with identity optimisation`() = runTest {
        val result = SequenceJL(listOf(NumberJL(2), NumberJL(4), NumberJL(6)))
        assertTrue(
            result === (MapJL(
                Identifier("input"),
                "a",
                Identifier("a"),
            ).accept(ExpressionInterpreter(mapOf("input" to result))) as Success).value
        )
    }
}
