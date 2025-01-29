package jetlang.interpreter

import jetlang.parser.*
import jetlang.types.NumberJL
import jetlang.types.Type
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.channelFlow

// TODO: utilize `DeepRecursiveFunction`
class Interpreter : StatementVisitor {
    lateinit var outputChannel: ProducerScope<String>

    val expressionInterpreter = ExpressionInterpreter()

    suspend fun output(message: String) = outputChannel.send(message)

    fun interpret(program: Program) = channelFlow {
        // TODO: Refactor this (and `outputChannel`) to not have state, maybe create a visitor per call to `interpret`
        outputChannel = this
        program.nodes.forEach { it.accept(this@Interpreter) }
    }

    override suspend fun visitPrint(print: Print) {
        // TODO: should `print` put a newline?
        output(print.value)
    }

    override suspend fun visitOut(out: Out) {
        output(out.expression.accept(expressionInterpreter).textContent() + "\n")
    }

    override suspend fun visitExpressionStatement(expression: ExpressionStatement) {
        expression.expression.accept(expressionInterpreter)
    }
}

class ExpressionInterpreter : ExpressionVisitor<Type> {
    override suspend fun visitNumberLiteral(numberLiteral: NumberLiteral) = NumberJL(numberLiteral.value)
}
