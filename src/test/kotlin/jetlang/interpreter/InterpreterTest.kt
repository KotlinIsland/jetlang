package jetlang.interpreter

import jetlang.parser.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import kotlin.test.*

fun interpret(ast: Statement) = runBlocking {
    Interpreter().interpret(Program(listOf(ast))).toList()
}

class InterpreterTest {
    @Test
    fun visitPrint() = runBlocking {
        val stringExpression = "string expression"
        assertEquals(listOf(stringExpression), interpret(Print(stringExpression)))
    }

    @Test
    fun visitOut() = runBlocking {
        val expressionValue = 1
        assertEquals(
            listOf("$expressionValue\n"), interpret(Out(NumberLiteral(BigDecimal(expressionValue))))
        )
    }

    @Test
    fun visitExpressionStatement() = runBlocking {
        var success = false
        val someExpression = object : Expression() {
            override suspend fun <T> accept(visitor: ExpressionVisitor<T>): T {
                success = true
                return visitor.visitNumberLiteral(NumberLiteral(BigDecimal(1)))
            }
        }
        Interpreter().visitExpressionStatement(ExpressionStatement(someExpression))
        assertTrue(success)
    }
}

