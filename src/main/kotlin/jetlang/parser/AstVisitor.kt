package jetlang.parser

abstract class AstVisitor {
    fun visit(ast: AstNodeBase) = ast.accept(this)
    abstract fun visitProgram(program: Program)
    abstract fun visitPrint(print: Print)
    abstract fun visitOut(out: Out)
}
