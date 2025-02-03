import jetlang.interpreter.ExpressionInterpreter
import jetlang.interpreter.InterpreterResult
import jetlang.parser.Expression
import jetlang.parser.Identifier
import jetlang.parser.NumberLiteral
import jetlang.parser.Operation
import jetlang.parser.Operator
import jetlang.parser.Reduce
import jetlang.parser.SequenceLiteral
import jetlang.parser.minus
import jetlang.parser.plus
import jetlang.types.NumberJL
import jetlang.types.SequenceJL
import jetlang.types.Value
import java.math.BigDecimal
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

fun interpretExpressionError(expression: Expression, names: Map<String, Value> = emptyMap()) =
    (expression.accept(ExpressionInterpreter(names)) as InterpreterResult.Error).value

val sequenceLiteral
    get() = SequenceLiteral(NumberLiteral(BigDecimal.ONE), NumberLiteral(BigDecimal.TWO))

class ExpressionInterpreterErrorsTest {
    @Test
    fun `test undefined variable`() {
        assertEquals(
            "Variable \"a\" not defined",
            interpretExpressionError(Identifier("a"))
        )
    }

    @Test
    fun `test sequence start not number`() {
        assertEquals(
            "Sequence start value is not a number: {1 2}",
            interpretExpressionError(
                SequenceLiteral(sequenceLiteral, NumberLiteral(BigDecimal.TWO))
            )
        )
    }

    @Test
    fun `test sequence start not int`() {
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
    fun `test sequence end not number`() {
        assertEquals(
            "Sequence end value is not a number: {1 2}",
            interpretExpressionError(
                SequenceLiteral(
                    NumberLiteral(BigDecimal.ONE), sequenceLiteral
                )
            )
        )
    }

    @Test
    fun `test sequence end not int`() {
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
    fun `test sequence start equal`() {
        SequenceLiteral(
            NumberLiteral(BigDecimal.ONE),
            NumberLiteral(BigDecimal.ONE)
        ) assertInterpretsAs SequenceJL(1..1)
    }

    @Test
    fun `test sequence start greater`() {
        assertEquals(
            "Sequence start value is greater than end value: {2, 1}",
            interpretExpressionError(
                SequenceLiteral(NumberLiteral(BigDecimal.TWO), NumberLiteral(BigDecimal.ONE))
            )
        )
    }

    @Test
    fun `test operation not number left`() {
        assertEquals(
            "for left operand expected NumberJL, got {1}",
            interpretExpressionError(
                Operation(
                    SequenceLiteral(NumberLiteral(1), NumberLiteral(1)),
                    Operator.ADD,
                    NumberLiteral(1),
                )
            )
        )
    }

    @Test
    fun `test operation not number right`() {
        assertEquals(
            "for right operand expected NumberJL, got {1}",
            interpretExpressionError(
                Operation(
                    NumberLiteral(1),
                    Operator.ADD,
                    SequenceLiteral(NumberLiteral(1), NumberLiteral(1)),
                )
            )
        )
    }

    @Test
    fun `reduce takes a sequence`() {
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
    fun `reduce is associative`() {
        assertEquals(
            "The lambda expression of `reduce` must be an associative operation",
            interpretExpressionError(
                Reduce(
                    SequenceLiteral(NumberLiteral(1), NumberLiteral(1)), NumberLiteral(0), "c", "d",
                    Identifier("a") - Identifier("b")
                )
            )
        )
    }

    @Test
    fun `reduce lambda can't see globals`() {
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
}
