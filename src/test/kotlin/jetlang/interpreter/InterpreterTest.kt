package jetlang.interpreter

import jetlang.parser.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class StubExpression : Expression() {
    override suspend fun accept(visitor: AstVisitor) = TODO("Not yet implemented")

    override fun stringContent() = CONTENT
    companion object {
        const val CONTENT = "stub expression"
    }
}

fun interpret(ast: AstNodeBase) = runBlocking {
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
        assertEquals(listOf("${StubExpression.CONTENT}\n"), interpret(Out(StubExpression())))
    }
}
