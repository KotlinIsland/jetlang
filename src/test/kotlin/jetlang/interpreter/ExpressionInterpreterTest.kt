import jetlang.interpreter.ExpressionInterpreter
import jetlang.parser.NumberLiteral
import jetlang.types.NumberJL
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class ExpressionInterpreterTest {
    @Test
    fun visitNumberLiteral() = runBlocking {
        val expressionValue = BigDecimal(1)
        assertEquals(
            NumberJL(expressionValue), NumberLiteral(expressionValue).accept(ExpressionInterpreter())
        )
    }
}
