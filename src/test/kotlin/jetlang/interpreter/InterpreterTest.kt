package jetlang.interpreter

import jetlang.parser.Expression
import jetlang.parser.Out
import jetlang.parser.Print
import jetlang.parser.AstVisitor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StubExpression : Expression() {
    override fun accept(visitor: AstVisitor) = TODO("Not yet implemented")

    override fun stringContent() = CONTENT
    companion object {
        const val CONTENT = "stub expression"
    }
}

class InterpreterTest {
    @Test
    fun visitPrint() {
        val interpreter = Interpreter()
        val stringExpression = "string expression"
        interpreter.visit(Print(stringExpression))
        assertEquals(listOf(stringExpression), interpreter.output.toList())
    }

    @Test
    fun visitOut() {
        val interpreter = Interpreter()
        interpreter.visit(Out(StubExpression()))
        assertEquals(listOf("${StubExpression.CONTENT}\n"), interpreter.output.toList())
    }
}
