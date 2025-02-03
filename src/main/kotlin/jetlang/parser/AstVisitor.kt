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
    fun visitReduce(reduce: Reduce): T
}

interface AstVisitor<out T> : ExpressionVisitor<T>, StatementVisitor<T> {
    fun visitProgram(program: Program): T
}

    open class BooleanExpressionQuery(val strategy: Strategy) : ExpressionVisitor<Boolean> {
        private val default: Boolean = when (strategy) {
            Strategy.AND -> true
            Strategy.OR -> false
        }

        override fun visitNumberLiteral(numberLiteral: NumberLiteral) = default
        override fun visitIdentifier(identifier: Identifier) = default
        override fun visitSequenceLiteral(sequenceLiteral: SequenceLiteral) = query(sequenceLiteral.start, sequenceLiteral.end)
        override fun visitOperation(operation: Operation) = query(operation.left, operation.right)
        override fun visitReduce(reduce: Reduce) = query(reduce.input, reduce.initial, reduce.lambda)

        protected fun query(vararg expressions: Expression) = when (strategy) {
            Strategy.AND -> expressions.all { it.accept(this) }
            Strategy.OR -> expressions.any { it.accept(this) }
        }

        companion object {
            enum class Strategy { AND, OR }
        }
    }
