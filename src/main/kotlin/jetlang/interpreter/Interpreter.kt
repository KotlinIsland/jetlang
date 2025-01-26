package jetlang.interpreter

import jetlang.parser.Out
import jetlang.parser.Print
import jetlang.parser.AstVisitor

class Interpreter : AstVisitor() {
    /**
     * the output of this interpreter, not divided into lines
     */
    val output = ArrayDeque<String>()

    override fun visitPrint(print: Print) {
        // TODO: should `print` put a newline?
        output.add(print.value)
    }

    override fun visitOut(out: Out) {
        output.add(out.expression.stringContent() + "\n")
    }
}
