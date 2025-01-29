package jetlang.parser


interface StatementVisitor {
    suspend fun visitPrint(print: Print)
    suspend fun visitOut(out: Out)
    suspend fun visitExpressionStatement(expression: ExpressionStatement)
}

interface ExpressionVisitor<out T> {
    suspend fun visitNumberLiteral(numberLiteral: NumberLiteral): T
}

interface AstVisitor : ExpressionVisitor<Any?>, StatementVisitor {
    suspend fun visitProgram(program: Program)
}
