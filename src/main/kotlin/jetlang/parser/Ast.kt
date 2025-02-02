package jetlang.parser

import java.math.BigDecimal

sealed class AstNodeBase {
    abstract fun <T> accept(visitor: AstVisitor<T>): Any?
}

data class Program(val nodes: List<Statement>) : AstNodeBase() {
    override fun <T> accept(visitor: AstVisitor<T>) = visitor.visitProgram(this)
}

sealed class Statement : AstNodeBase() {
    // this is needed because an override can't widen the input types
    final override fun <T> accept(visitor: AstVisitor<T>) =
        accept(visitor as StatementVisitor<T>)

    abstract fun <T> accept(visitor: StatementVisitor<T>): T
}

data class Print(val value: String) : Statement() {
    override fun <T> accept(visitor: StatementVisitor<T>) = visitor.visitPrint(this)
}

data class Out(val expression: Expression) : Statement() {
    override fun <T> accept(visitor: StatementVisitor<T>) = visitor.visitOut(this)
}

data class Var(val name: String, val expression: Expression) : Statement() {
    override fun <T> accept(visitor: StatementVisitor<T>) = visitor.visitVar(this)
}

data class ExpressionStatement(val expression: Expression) : Statement() {
    override fun <T> accept(visitor: StatementVisitor<T>) =
        visitor.visitExpressionStatement(this)
}

abstract class Expression : AstNodeBase() {
    // this is needed because an override can't widen the input types
    final override fun <T> accept(visitor: AstVisitor<T>) =
        accept(visitor as ExpressionVisitor<T>)

    abstract fun <T> accept(visitor: ExpressionVisitor<T>): T
}

data class NumberLiteral(val value: BigDecimal) : Expression() {
    override fun <T> accept(visitor: ExpressionVisitor<T>) =
        visitor.visitNumberLiteral(this)
}

data class Identifier(val name: String) : Expression() {
    override fun <T> accept(visitor: ExpressionVisitor<T>) =
        visitor.visitIdentifier(this)
}

