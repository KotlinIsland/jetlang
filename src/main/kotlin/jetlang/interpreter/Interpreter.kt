package jetlang.interpreter

import jetlang.parser.*
import jetlang.parser.BooleanExpressionQuery.Companion.Strategy
import jetlang.types.NumberJL
import jetlang.types.SequenceJL
import jetlang.types.Value
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.channelFlow
import java.lang.ArithmeticException
import java.math.BigDecimal


sealed class InterpreterResult<out T : Value> {
    data class Success<T : Value>(val value: T) : InterpreterResult<T>()
    data class Error(val value: String) : InterpreterResult<Nothing>()

    fun <R : Value> map(block: (Value) -> InterpreterResult<R>) = when (this) {
        is Success -> block(value)
        is Error -> this
    }

    fun toOutput(): Output = when (this) {
        is Success -> Output.Standard(value.textContent())
        is Error -> Output.Error(this.value)
    }

    inline fun isError(function: (Error) -> Nothing) = when (this) {
        is Success -> this.value
        is Error -> function(this)
    }
}

fun <TValue : Value> TValue.toResult() = InterpreterResult.Success(this)

sealed class Output {
    abstract val value: String

    data class Standard(override val value: String, val isExpression: Boolean = false) : Output()
    data class Error(override val value: String) : Output()
}

fun Output?.shouldSend() = when (this) {
    is Output.Standard -> !this.isExpression
    else -> true
}

class Interpreter : StatementVisitor<Output?> {
    val names = mutableMapOf<String, Value>()

    val expressionInterpreter = ExpressionInterpreter(names)

    fun interpret(program: Program) = channelFlow {
        var last: Output? = null
        for (it in program.nodes) {
            val result = it.accept(this@Interpreter)
            if (result.shouldSend()) {
                send(result!!) // TODO: contract on shouldSend
            }
            if (result is Output.Error) {
                return@channelFlow
            } else {
                last = result as Output.Standard?
            }
        }
        if (last is Output.Standard && last.isExpression) {
            send(last)
        }
    }

    override suspend fun visitPrint(print: Print): Output {
        return Output.Standard(print.value)
    }

    override suspend fun visitOut(out: Out): Output {
        val result = out.expression.accept(expressionInterpreter).isError { return it.toOutput() }
        return Output.Standard(result.textContent())
    }

    override suspend fun visitVar(`var`: Var): Output? {
        names[`var`.name] =
            `var`.expression.accept(expressionInterpreter).isError { return it.toOutput() }
        return null
    }

    override suspend fun visitExpressionStatement(expression: ExpressionStatement): Output {
        val result =
            expression.expression.accept(expressionInterpreter).isError { return it.toOutput() }
        return Output.Standard(result.textContent(), isExpression = true)
    }
}

val BigDecimal.isInt
    get() = stripTrailingZeros().scale() <= 0

class ExpressionInterpreter(private val names: Map<String, Value>) :
    ExpressionVisitor<InterpreterResult<*>> {
    override suspend fun visitNumberLiteral(numberLiteral: NumberLiteral) =
        InterpreterResult.Success(NumberJL(numberLiteral.value))

    override suspend fun visitIdentifier(identifier: Identifier): InterpreterResult<*> {
        val result = names[identifier.name]
        return if (result == null) InterpreterResult.Error(("Variable \"${identifier.name}\" not defined"))
        else InterpreterResult.Success(result)
    }

    fun Value.checkSequenceValue(label: String): InterpreterResult<NumberJL> {
        if (this !is NumberJL) {
            return InterpreterResult.Error("Sequence $label value is not a number: ${textContent()}")
        }
        if (!value.isInt) {
            return InterpreterResult.Error("Sequence $label value is not an integer: ${textContent()}")
        }
        return InterpreterResult.Success(this)
    }

    override suspend fun visitSequenceLiteral(sequenceLiteral: SequenceLiteral): InterpreterResult<SequenceJL> {
        val start = sequenceLiteral.start.accept(this).map { it.checkSequenceValue("start") }
            .isError { return it }
        val end = sequenceLiteral.end.accept(this).map { it.checkSequenceValue("end") }
            .isError { return it }
        if (start.value > end.value) {
            return InterpreterResult.Error("Sequence start value is greater than end value: {${start.textContent()}, ${end.textContent()}}")
        }
        return InterpreterResult.Success(
            SequenceJL(start.value.intValueExact()..end.value.intValueExact())
        )
    }

    private inline fun <reified TValue : Value> Value.checkIs(message: String) = when (this) {
        is TValue -> InterpreterResult.Success(this)
        else -> InterpreterResult.Error("$message expected ${TValue::class.simpleName}, got ${textContent()}")
    }

    override suspend fun visitOperation(operation: Operation): InterpreterResult<*> {
        val left = operation.left
            .accept(this)
            .map { it.checkIs<NumberJL>("for left operand") }
            .isError { return it }
        val right = operation.right
            .accept(this)
            .map { it.checkIs<NumberJL>("for right operand") }
            .isError { return it }
        return NumberJL(
            when (operation.operator) {
                Operator.ADD -> left.value + right.value
                Operator.SUBTRACT -> left.value - right.value
                Operator.MULTIPLY -> left.value * right.value
                Operator.DIVIDE -> left.value / right.value
                Operator.EXPONENT -> {
                    left.value.pow(
                        try {
                            right.value.intValueExact()
                        } catch (_: ArithmeticException) {
                            return InterpreterResult.Error("Raising to a decimal is not supported")
                        }
                    )
                }
            }
        ).toResult()
    }

    override suspend fun visitReduce(reduce: Reduce): InterpreterResult<*> {
        val input = reduce.input
            .accept(this)
            .map { it.checkIs<SequenceJL>("Input value") }
            .isError { return it }
        val initial = reduce.initial
            .accept(this)
            .map { it.checkIs<NumberJL>("Initial value") }
            .isError { return it }
        if (!reduce.lambda.accept(IsAssociative)) {
            // TODO: we could have a nice error message that refers to the operation that isn't associative
            return InterpreterResult.Error("The lambda expression of `reduce` must be an associative operation")
        }
        val lambda = reduce.lambda

        //TODO: just value?
        suspend fun executeReduce(input: List<Value>): InterpreterResult<Value> {
            if (input.size == 1) {
                return InterpreterResult.Success(input[0])
            }
            if (input.size == 2) {
                return lambda.accept(
                    ExpressionInterpreter(
                        mapOf(
                            reduce.arg1 to input[0],
                            reduce.arg2 to input[1],
                        )
                    )
                )
            }

            val mid = input.size / 2
            val leftRange = input.subList(0, mid)
            val rightRange = input.subList(mid, input.size)
            return coroutineScope {
                // TODO: infinity threads????
                val leftDeferred = async { executeReduce(leftRange) }
                val rightDeferred = async { executeReduce(rightRange) }

                executeReduce(
                    listOf(
                        // throw here to cancel the sibling contexts
                        leftDeferred.await().isError { throw CancellationException(it.value) },
                        rightDeferred.await().isError { throw CancellationException(it.value) },
                    )
                )
            }
        }
        return try {
            executeReduce(listOf(initial, *input.values.toTypedArray()))
        } catch (_: CancellationException) {
            // TODO
            InterpreterResult.Error("fail...")
        }
    }

    override suspend fun visitMap(map: MapJL): InterpreterResult<SequenceJL> {
        val input =
            map.input.accept(this)
                .map { it.checkIs<SequenceJL>("Input for `map`") }
                .isError { return it }
        return coroutineScope {
            runCatching {
                SequenceJL(
                    input.values.map {
                        async {
                            map.lambda.accept(
                                ExpressionInterpreter(
                                    mapOf(map.arg to it)
                                )
                            ).isError { error ->
                                // throw instead of returning to cancel the sibling coroutines
                                throw CancellationException(error.value)
                            }
                        }
                    }.awaitAll()
                ).toResult()
            }.getOrElse { InterpreterResult.Error(it.message ?: "Unknown error") }
        }
    }
}

/**
 * should never produce a false positive, but can produce false negatives
 */
object IsAssociative : BooleanExpressionQuery(Strategy.AND) {
    override suspend fun visitOperation(operation: Operation) = when (operation.operator) {
        Operator.SUBTRACT, Operator.DIVIDE, Operator.EXPONENT -> false
        else -> super.visitOperation(operation)
    }

    override suspend fun visitReduce(reduce: Reduce) = false
}
