package jetlang.interpreter

import jetlang.parser.*
import jetlang.parser.BooleanExpressionQuery.Companion.Strategy
import jetlang.types.NumberJL
import jetlang.types.SequenceJL
import jetlang.types.Value
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal


sealed class InterpreterResult<out T : Value> {
    data class Success<T : Value>(val value: T) : InterpreterResult<T>()
    data class Error(val value: String) : InterpreterResult<Nothing>()

    fun get() = when (this) {
        is Success -> value
        is Error -> throw RuntimeException("Interpreter error: $value")
    }

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

fun Value.toResult() = InterpreterResult.Success(this)

sealed class Output {
    abstract val value: String

    data class Standard(override val value: String) : Output()
    data class Error(override val value: String) : Output()
}

// TODO: utilize `DeepRecursiveFunction`
class Interpreter : StatementVisitor<Output?> {
    val names = mutableMapOf<String, Value>()

    val expressionInterpreter = ExpressionInterpreter(names)

    // TODO: hmmm, not sure how this is going to work
//    suspend fun handleResult(block: suspend () -> InterpreterResult?): Output? {
//        val result = block()
//        return when (result) {
//            is InterpreterResult.Success -> null
//            is InterpreterResult.Error -> result.toOutput()
//        }
//    }

    fun interpret(program: Program) = channelFlow {
        for (it in program.nodes) {
            it.accept(this@Interpreter)?.apply {
                send(this)
            } is Output.Error && break
        }
    }

    override fun visitPrint(print: Print): Output {
        // TODO: should `print` put a newline?
        return Output.Standard(print.value)
    }

    override fun visitOut(out: Out): Output {
        val result = out.expression.accept(expressionInterpreter).isError { return it.toOutput() }
        return Output.Standard(result.textContent() + "\n")
    }

    override fun visitVar(`var`: Var): Output? {
        names[`var`.name] =
            `var`.expression.accept(expressionInterpreter).isError { return it.toOutput() }
        return null
    }

    override fun visitExpressionStatement(expression: ExpressionStatement): Output? {
        expression.expression.accept(expressionInterpreter).isError { return it.toOutput() }
        return null
    }
}

val BigDecimal.isInt
    get() = stripTrailingZeros().scale() <= 0

class ExpressionInterpreter(private val names: Map<String, Value>) :
    ExpressionVisitor<InterpreterResult<*>> {
    override fun visitNumberLiteral(numberLiteral: NumberLiteral) =
        InterpreterResult.Success(NumberJL(numberLiteral.value))

    override fun visitIdentifier(identifier: Identifier): InterpreterResult<*> {
        val result = names[identifier.name]
        return if (result == null)
            InterpreterResult.Error(("Variable \"${identifier.name}\" not defined"))
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

    override fun visitSequenceLiteral(sequenceLiteral: SequenceLiteral): InterpreterResult<SequenceJL> {
        val start =
            sequenceLiteral.start.accept(this).map { it.checkSequenceValue("start") }
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

    private inline fun <reified TValue : Value> Value.checkIs(message: String) =
        when (this) {
            is TValue -> InterpreterResult.Success(this)
            else -> InterpreterResult.Error("$message expected ${TValue::class.simpleName}, got ${textContent()}")
        }

    override fun visitOperation(operation: Operation): InterpreterResult<*> {
        val left = operation.left.accept(this).map { it.checkIs<NumberJL>("for left operand") }
            .isError { return it }
        val right = operation.right.accept(this).map { it.checkIs<NumberJL>("for right operand") }
            .isError { return it }
        return when (operation.operator) {
            Operator.ADD -> NumberJL(left.value + right.value)
            Operator.SUBTRACT -> NumberJL(left.value - right.value)
            Operator.MULTIPLY -> NumberJL(left.value * right.value)
            Operator.DIVIDE -> NumberJL(left.value / right.value)
            // TODO: handle non-int gracefully
            Operator.EXPONENT -> NumberJL(left.value.pow(right.value.intValueExact()))
        }.toResult()
    }

    override fun visitReduce(reduce: Reduce): InterpreterResult<*> {
        val input = reduce.input.accept(this)
            .map { it.checkIs<SequenceJL>("Input value") }
            .isError { return it }
        val initial = reduce.initial.accept(this)
            .map { it.checkIs<NumberJL>("Initial value") }
            .isError { return it }
        if (!reduce.lambda.accept(IsAssociative)) {
            // TODO: we could have a nice error message that refers to the operation that isn't associative
            return InterpreterResult.Error("The lambda expression of `reduce` must be an associative operation")
        }
        val lambda = reduce.lambda
        suspend fun executeReduce(input: List<Value>): InterpreterResult<Value> =
            coroutineScope {
                if (input.size == 1) {
                    return@coroutineScope InterpreterResult.Success(input[0])
                }
                if (input.size == 2) {
                    return@coroutineScope lambda.accept(
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

                val leftDeferred = async { executeReduce(leftRange) }
                val rightDeferred = async { executeReduce(rightRange) }

                executeReduce(
                    listOf(
                        leftDeferred.await().isError { return@coroutineScope it },
                        rightDeferred.await().isError { return@coroutineScope it },
                    )
                )
            }
        return runBlocking { executeReduce(listOf(initial, *input.values.toTypedArray())) }
    }
}

/**
 * will never produce a false positive, but can produce false negatives
 */
object IsAssociative : BooleanExpressionQuery(Strategy.AND) {
    override fun visitOperation(operation: Operation) = when (operation.operator) {
        Operator.SUBTRACT, Operator.DIVIDE, Operator.EXPONENT -> false
        else -> super.visitOperation(operation)
    }

    override fun visitReduce(reduce: Reduce) = false
}
