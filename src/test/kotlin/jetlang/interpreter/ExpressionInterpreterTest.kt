import jetlang.interpreter.ExpressionInterpreter
import jetlang.parser.Identifier
import jetlang.parser.NumberLiteral
import jetlang.types.NumberJL
import jetlang.types.Type
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.assertThrows
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
}
