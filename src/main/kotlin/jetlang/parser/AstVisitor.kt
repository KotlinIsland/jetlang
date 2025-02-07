package jetlang.parser


interface StatementVisitor<out T> {
    suspend fun visitPrint(print: Print): T
    suspend fun visitOut(out: Out): T
    suspend fun visitExpressionStatement(expression: ExpressionStatement): T
    suspend fun visitVar(`var`: Var): T
}

interface ExpressionVisitor<out T> {
    suspend fun visitNumberLiteral(numberLiteral: NumberLiteral): T
    suspend fun visitIdentifier(identifier: Identifier): T
    suspend fun visitSequenceLiteral(sequenceLiteral: SequenceLiteral): T
    suspend fun visitOperation(operation: Operation): T
    suspend fun visitReduce(reduce: Reduce): T
    suspend fun visitMap(map: MapJL): T
}

interface AstVisitor<out T> : ExpressionVisitor<T>, StatementVisitor<T> {
    fun visitProgram(program: Program): T
}

open class BooleanExpressionQuery(val strategy: Strategy) : ExpressionVisitor<Boolean> {
    private val default: Boolean = when (strategy) {
        Strategy.AND -> true
        Strategy.OR -> false
    }

    override suspend fun visitNumberLiteral(numberLiteral: NumberLiteral) = default
    override suspend fun visitIdentifier(identifier: Identifier) = default
    override suspend fun visitSequenceLiteral(sequenceLiteral: SequenceLiteral) =
        query(sequenceLiteral.start, sequenceLiteral.end)

    override suspend fun visitOperation(operation: Operation) = query(operation.left, operation.right)
    override suspend fun visitReduce(reduce: Reduce) = query(reduce.input, reduce.initial, reduce.lambda)
    override suspend fun visitMap(map: MapJL): Boolean = query(map.input, map.lambda)

    protected suspend fun query(vararg expressions: Expression) = when (strategy) {
        Strategy.AND -> expressions.all { it.accept(this) }
        Strategy.OR -> expressions.any { it.accept(this) }
    }

    companion object {
        enum class Strategy { AND, OR }
    }
}
