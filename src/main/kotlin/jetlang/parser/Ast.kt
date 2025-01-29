package jetlang.parser

import java.math.BigDecimal

sealed class AstNodeBase {
    abstract suspend fun accept(visitor: AstVisitor): Any?
}

data class Program(val nodes: List<Statement>) : AstNodeBase() {
    override suspend fun accept(visitor: AstVisitor) = visitor.visitProgram(this)
}

sealed class Statement : AstNodeBase() {
    // this is needed because an override can't widen the input types
    final override suspend fun accept(visitor: AstVisitor) =
        accept(visitor as StatementVisitor)

    abstract suspend fun accept(visitor: StatementVisitor)
}

data class Print(val value: String) : Statement() {
    override suspend fun accept(visitor: StatementVisitor) = visitor.visitPrint(this)
}

data class Out(val expression: Expression) : Statement() {
    override suspend fun accept(visitor: StatementVisitor) = visitor.visitOut(this)
}

data class ExpressionStatement(val expression: Expression) : Statement() {
    override suspend fun accept(visitor: StatementVisitor) =
        visitor.visitExpressionStatement(this)
}

abstract class Expression : AstNodeBase() {
    // this is needed because an override can't widen the input types
    final override suspend fun accept(visitor: AstVisitor) =
        accept(visitor as ExpressionVisitor<*>)

    abstract suspend fun <T> accept(visitor: ExpressionVisitor<T>): T
}

data class NumberLiteral(val value: BigDecimal) : Expression() {
    override suspend fun <T> accept(visitor: ExpressionVisitor<T>) =
        visitor.visitNumberLiteral(this)
}
