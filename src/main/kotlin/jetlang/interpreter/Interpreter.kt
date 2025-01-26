package jetlang.interpreter

import jetlang.parser.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.channelFlow

// TODO: utilize `DeepRecursiveFunction`
class Interpreter : AstVisitor {
    lateinit var outputChannel: ProducerScope<String>

    suspend fun output(message: String) = outputChannel.send(message)

    fun interpret(program: AstNodeBase) = channelFlow {
        // TODO: Refactor this (and `outputChannel`) to not have state, maybe create a visitor per call to `interpret`
        outputChannel = this
        visit(program)
    }

    override suspend fun visitProgram(program: Program) {
        // TODO: consider abstracting non-leaf nodes to a default implementation
        program.nodes.forEach { visit(it) }
    }

    override suspend fun visitPrint(print: Print) {
        // TODO: should `print` put a newline?
        output(print.value)
    }

    override suspend fun visitOut(out: Out) {
        // TODO: recursively visit the expression, perhaps introduce an `ExpressionVisitor`
        output(out.expression.stringContent() + "\n")
    }
}
