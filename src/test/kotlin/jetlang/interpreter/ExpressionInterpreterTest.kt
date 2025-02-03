import jetlang.interpreter.ExpressionInterpreter
import jetlang.interpreter.InterpreterResult
import jetlang.parser.Expression
import jetlang.parser.Identifier
import jetlang.parser.NumberLiteral
import jetlang.parser.Operation
import jetlang.parser.Operator
import jetlang.parser.Reduce
import jetlang.parser.SequenceLiteral
import jetlang.types.NumberJL
import jetlang.types.Value
import jetlang.parser.plus
import jetlang.parser.minus
import jetlang.parser.times
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import kotlin.test.*

infix fun Expression.assertInterpretsAs(expected: Value) =
    assertEquals(
        expected,
        (accept(ExpressionInterpreter(emptyMap())) as InterpreterResult.Success).value
    )

class ExpressionInterpreterTest {
    @Test
    fun visitNumberLiteral() = runBlocking {
        val expressionValue = BigDecimal.ONE
        assertEquals(
            NumberJL(expressionValue),
            NumberLiteral(expressionValue).accept(ExpressionInterpreter(emptyMap())).get()
        )
    }

    @Test
    fun visitIdentifier() = runBlocking {
        val expressionValue = BigDecimal.ONE
        assertEquals(
            NumberJL(expressionValue),
            NumberLiteral(expressionValue).accept(ExpressionInterpreter(emptyMap())).get()
        )
    }

    @Test
    fun visitSequenceLiteralLiteral() = runBlocking {
        val expressionValue = BigDecimal.ONE
        assertEquals(
            NumberJL(expressionValue),
            NumberLiteral(expressionValue).accept(ExpressionInterpreter(emptyMap())).get()
        )
    }

    @Test
    fun `test add operation`() {
        Operation(
            NumberLiteral(1),
            Operator.ADD,
            NumberLiteral(2)
        ) assertInterpretsAs NumberJL(3)
    }

    @Test
    fun `test subtract operation`() {
        Operation(
            NumberLiteral(3),
            Operator.SUBTRACT,
            NumberLiteral(2)
        ) assertInterpretsAs NumberJL(1)
    }

    @Test
    fun `test multiply operation`() {
        Operation(
            NumberLiteral(3),
            Operator.MULTIPLY,
            NumberLiteral(2)
        ) assertInterpretsAs NumberJL(6)
    }

    @Test
    fun `test divide operation`() {
        Operation(
            NumberLiteral(6),
            Operator.DIVIDE,
            NumberLiteral(3)
        ) assertInterpretsAs NumberJL(2)
    }

    @Test
    fun `test exponent operation`() {
        Operation(
            NumberLiteral(3),
            Operator.EXPONENT,
            NumberLiteral(2)
        ) assertInterpretsAs NumberJL(9)
    }

    @Test
    fun `reduce one arg`() = runBlocking {
        Reduce(
            SequenceLiteral(NumberLiteral(2), NumberLiteral(2)),
            NumberLiteral(1),
            "a", "b",
            Operation(Identifier("a"), Operator.MULTIPLY, Identifier("b"))
        ) assertInterpretsAs NumberJL(2)
    }

    @Test
    fun `reduce two args`() = runBlocking {
        Reduce(
            SequenceLiteral(NumberLiteral(2), NumberLiteral(3)),
            NumberLiteral(1),
            "a", "b",
            Operation(Identifier("a"), Operator.MULTIPLY, Identifier("b"))
        ) assertInterpretsAs NumberJL(6)
    }

    @Test
    fun `reduce three args`() = runBlocking {
        Reduce(
            SequenceLiteral(NumberLiteral(2), NumberLiteral(4)),
            NumberLiteral(1),
            "a", "b",
            Operation(Identifier("a"), Operator.MULTIPLY, Identifier("b"))
        ) assertInterpretsAs NumberJL(24)
    }
}