package jetlang.interpreter

import jetlang.parser.*
import jetlang.types.NumberJL
import jetlang.types.Type
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import kotlin.test.*

fun interpret(vararg ast: Statement) = runBlocking {
    Interpreter().interpret(Program(ast.toList())).toList()
}.map { it.value }

fun interpreter(vararg ast: Statement) = runBlocking {
    Interpreter().apply {
        interpret(Program(ast.toList())).toList()
    }
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
    fun visitVar() = runBlocking {
        assertEquals<Map<String, Type>>(
            mapOf("a" to NumberJL(BigDecimal.ONE)),
            interpreter(Var("a", NumberLiteral(BigDecimal.ONE))).names
        )
    }

    @Test
    fun visitIdentifier() = runBlocking {
        val expressionValue = 1
        assertEquals(
            listOf("$expressionValue\n"),
            interpret(
                Var("a", NumberLiteral(BigDecimal(expressionValue))),
                Out(Identifier("a"))
            )
        )
    }

    @Test
    fun visitExpressionStatement() = runBlocking {
        var success = false
        val someExpression = object : Expression() {
            override fun <T> accept(visitor: ExpressionVisitor<T>): T {
                success = true
                return visitor.visitNumberLiteral(NumberLiteral(BigDecimal.ONE))
            }
        }
        Interpreter().visitExpressionStatement(ExpressionStatement(someExpression))
        assertTrue(success)
    }
}
