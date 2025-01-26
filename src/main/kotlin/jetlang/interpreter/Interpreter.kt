package jetlang.interpreter

import jetlang.parser.Out
import jetlang.parser.Print
import jetlang.parser.AstVisitor
import jetlang.parser.Program

// TODO: utilize `DeepRecursiveFunction`
class Interpreter : AstVisitor() {
    /**
     * the output of this interpreter, not divided into lines
     */
    val output = ArrayDeque<String>()

    override fun visitProgram(program: Program) {
        // TODO: consider abstracting non-leaf nodes to a default implementation
        program.nodes.forEach { visit(it) }
    }

    override fun visitPrint(print: Print) {
        // TODO: should `print` put a newline?
        output.add(print.value)
    }

    override fun visitOut(out: Out) {
        // TODO: recursively visit the expression, perhaps introduce an `ExpressionVisitor`
        output.add(out.expression.stringContent() + "\n")
    }
}
