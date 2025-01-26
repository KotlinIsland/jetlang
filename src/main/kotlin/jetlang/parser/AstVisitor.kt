package jetlang.parser

interface AstVisitor {
    suspend fun visit(ast: AstNodeBase) = ast.accept(this)
    suspend fun visitProgram(program: Program)
    suspend fun visitPrint(print: Print)
    suspend fun visitOut(out: Out)
}
