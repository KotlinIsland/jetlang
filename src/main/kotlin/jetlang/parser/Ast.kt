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
    constructor(value: Int) : this(value.toBigDecimal())

    override fun <T> accept(visitor: ExpressionVisitor<T>) =
        visitor.visitNumberLiteral(this)
}

data class SequenceLiteral(val start: Expression, val end: Expression) : Expression() {
    override fun <T> accept(visitor: ExpressionVisitor<T>) =
        visitor.visitSequenceLiteral(this)
}

data class Identifier(val name: String) : Expression() {
    override fun <T> accept(visitor: ExpressionVisitor<T>) =
        visitor.visitIdentifier(this)
}

enum class Operator {
    ADD, SUBTRACT, MULTIPLY, DIVIDE, EXPONENT
}

data class Operation(val left: Expression, val operator: Operator, val right: Expression) :
    Expression() {
    override fun <T> accept(visitor: ExpressionVisitor<T>) =
        visitor.visitOperation(this)
}

data class Reduce(
    val input: Expression,
    val initial: Expression,
    val arg1: String,
    val arg2: String,
    val lambda: Expression,
) : Expression() {
    override fun <T> accept(visitor: ExpressionVisitor<T>) =
        visitor.visitReduce(this)
}
