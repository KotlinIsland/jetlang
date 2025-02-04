package jetlang.parser


interface StatementVisitor<out T> {
    fun visitPrint(print: Print): T
    fun visitOut(out: Out): T
    fun visitExpressionStatement(expression: ExpressionStatement): T
    fun visitVar(`var`: Var): T
}

interface ExpressionVisitor<out T> {
    fun visitNumberLiteral(numberLiteral: NumberLiteral): T
    fun visitIdentifier(identifier: Identifier): T
    fun visitSequenceLiteral(sequenceLiteral: SequenceLiteral): T
    fun visitOperation(operation: Operation): T
}

interface AstVisitor<out T> : ExpressionVisitor<T>, StatementVisitor<T> {
    fun visitProgram(program: Program): T
}
