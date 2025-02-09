package jetlang.interpreter

import jetlang.parser.*
import jetlang.types.NumberJL
import jetlang.types.Value
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import kotlin.test.*

suspend fun interpret(vararg ast: Statement)  =
    Interpreter().interpret(Program(ast.toList())).toList().map { it.value }

suspend fun interpreter(vararg ast: Statement) =
    Interpreter().apply {
        interpret(Program(ast.toList())).toList()
    }

class InterpreterTest {
    @Test
    fun visitPrint() = runTest {
        val stringExpression = "string expression"
        assertEquals(listOf(stringExpression), interpret(Print(stringExpression)))
    }

    @Test
    fun visitOut() = runTest {
        val expressionValue = 1
        assertEquals(
            listOf("$expressionValue"),
            interpret(Out(NumberLiteral(expressionValue.toBigDecimal())))
        )
    }

    @Test
    fun visitVar() = runTest {
        assertEquals<Map<String, Value>>(
            mapOf("a" to NumberJL(BigDecimal.ONE)),
            interpreter(Var("a", NumberLiteral(BigDecimal.ONE))).names
        )
    }

    @Test
    fun visitIdentifier() = runTest {
        val expressionValue = 1
        assertEquals(
            listOf("$expressionValue"),
            interpret(
                Var("a", NumberLiteral(expressionValue.toBigDecimal())),
                Out(Identifier("a"))
            )
        )
    }

    @Test
    fun visitExpressionStatement() = runTest {
        var success = false
        val someExpression = object : Expression() {
            override suspend fun <T> accept(visitor: ExpressionVisitor<T>): T {
                success = true
                return visitor.visitNumberLiteral(NumberLiteral(BigDecimal.ONE))
            }
        }
        Interpreter().visitExpressionStatement(ExpressionStatement(someExpression))
        assertTrue(success)
    }

    @Test
    fun `output last expression`() = runTest {
        val result = interpret(
            ExpressionStatement(NumberLiteral(1)),
            Out(NumberLiteral(2)),
            ExpressionStatement(NumberLiteral(3)),
            ExpressionStatement(NumberLiteral(4)),
        )
        assertEquals(listOf("2", "4"), result)
    }
}
