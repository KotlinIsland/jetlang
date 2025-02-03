import jetlang.interpreter.ExpressionInterpreter
import jetlang.interpreter.InterpreterResult
import jetlang.parser.Expression
import jetlang.parser.Identifier
import jetlang.parser.NumberLiteral
import jetlang.parser.SequenceLiteral
import jetlang.types.Value
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

fun interpretExpression(expression: Expression): InterpreterResult<Value> =
    expression.accept(ExpressionInterpreter(emptyMap()))

val sequenceLiteral
    get() = SequenceLiteral(NumberLiteral(BigDecimal.ONE), NumberLiteral(BigDecimal.TWO))

class ExpressionInterpreterErrorsTest {
    @Test
    fun `test undefined variable`() = runBlocking {
        val exception = interpretExpression(Identifier("a"))
        assertEquals(
            "Variable \"a\" not defined",
            (exception as InterpreterResult.Error).value.message
        )
    }

    @Test
    fun `test sequence start not number`() {
        val output = interpretExpression(
            SequenceLiteral(sequenceLiteral, NumberLiteral(BigDecimal.TWO))
        ) as InterpreterResult.Error
        assertEquals(
            "Sequence start value is not a number: {1, 2}",
            output.value.message
        )
    }

    @Test
    fun `test sequence start not int`() {
        val output = interpretExpression(
            SequenceLiteral(NumberLiteral(BigDecimal("1.5")), NumberLiteral(BigDecimal.TWO))
        ) as InterpreterResult.Error
        assertEquals(
            "Sequence start value is not an integer: 1.5",
            output.value.message
        )
    }

    @Test
    fun `test sequence end not number`() {
        val output = interpretExpression(
            SequenceLiteral(NumberLiteral(BigDecimal("1")), sequenceLiteral)
        ) as InterpreterResult.Error
        assertEquals(
            "Sequence end value is not a number: {1, 2}",
            output.value.message
        )
    }

    @Test
    fun `test sequence end not int`() {
        val output = interpretExpression(
            SequenceLiteral(NumberLiteral(BigDecimal.ONE), NumberLiteral(BigDecimal("1.5")))
        ) as InterpreterResult.Error
        assertEquals(
            "Sequence end value is not an integer: 1.5",
            output.value.message
        )
    }

    @Test
    fun `test sequence start greater`() {
        val output = interpretExpression(
            SequenceLiteral(NumberLiteral(BigDecimal.TWO), NumberLiteral(BigDecimal.ONE))
        ) as InterpreterResult.Error
        assertEquals(
            "Sequence start value is greater than end value: {2, 1}",
            output.value.message
        )
    }
}
