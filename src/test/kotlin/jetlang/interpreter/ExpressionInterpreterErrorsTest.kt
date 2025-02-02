import jetlang.interpreter.ExpressionInterpreter
import jetlang.interpreter.InterpreterResult
import jetlang.parser.Identifier
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class ExpressionInterpreterErrorsTest {
    @Test
    fun `test undefined variable`() = runBlocking {
        val exception = Identifier("a").accept(ExpressionInterpreter(emptyMap()))
        assertEquals("Variable \"a\" not defined", (exception as InterpreterResult.Error).value.message)
    }
}
