import jetlang.interpreter.ExpressionInterpreter
import jetlang.parser.NumberLiteral
import jetlang.types.NumberJL
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import kotlin.test.*

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
}
