package jetlang.interpreter

import jetlang.parser.ExpressionStatement
import jetlang.parser.Identifier
import jetlang.parser.NumberLiteral
import jetlang.parser.Var
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.assertEquals

class InterpreterErrorsTest {
    @Test
    fun `test variable not yet defined`() = runBlocking {
        val output =
            interpret(
                ExpressionStatement(Identifier("a")),
                Var("a", NumberLiteral(BigDecimal.ONE)),
            )
        assertEquals(listOf("${InterpreterException::class.qualifiedName}: Variable \"a\" not defined"), output)
    }
}