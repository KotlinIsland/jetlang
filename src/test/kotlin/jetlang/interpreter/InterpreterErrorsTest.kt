package jetlang.interpreter

import jetlang.parser.ExpressionStatement
import jetlang.parser.Identifier
import jetlang.parser.NumberLiteral
import jetlang.parser.Var
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals

class InterpreterErrorsTest {
    @Test
    fun `test variable not yet defined`() = runTest {
        val output =
            interpret(
                ExpressionStatement(Identifier("a")),
                Var("a", NumberLiteral(BigDecimal.ONE)),
            )
        assertEquals(listOf("Variable \"a\" not defined"), output)
    }
}
