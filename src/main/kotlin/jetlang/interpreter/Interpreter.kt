package jetlang.interpreter

import jetlang.parser.*
import jetlang.parser.BooleanExpressionQuery.Companion.Strategy
import jetlang.types.NumberJL
import jetlang.types.RangeSequenceJL
import jetlang.types.SequenceJL
import jetlang.types.Value
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.channelFlow
import java.lang.ArithmeticException
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


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

@OptIn(ExperimentalContracts::class)
fun Output?.shouldSend(): Boolean {
    contract { returns(true) implies (this@shouldSend != null) }

    return when (this) {
        is Output.Standard -> !this.isExpression
        null -> false
        else -> true
    }
}

class Interpreter(numberScale: Int = 30) : StatementVisitor<Output?> {
    val names = mutableMapOf<String, Value>()

    val expressionInterpreter = ExpressionInterpreter(names, numberScale)

    fun interpret(program: Program) = channelFlow {
        var last: Output? = null
        for (it in program.nodes) {
            val result = it.accept(this@Interpreter)
            if (result.shouldSend()) {
                send(result)
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

class ExpressionInterpreter(private val names: Map<String, Value>, private val numberScale: Int = 30) :
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
            RangeSequenceJL(start.value.intValueExact()..end.value.intValueExact())
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
                Operator.DIVIDE -> left.value.divide(
                    right.value,
                    numberScale,
                    RoundingMode.HALF_EVEN
                ).stripTrailingZeros()

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
        if (!isAssociative(reduce.lambda, reduce.arg1, reduce.arg2)) {
            // TODO: we could have a nice error message that refers to the operation that isn't associative
            return InterpreterResult.Error("The lambda expression of `reduce` must be an associative operation")
        }
        val lambda = reduce.lambda

        // special case summation
        if (input is RangeSequenceJL && lambda is Operation && lambda.operator == Operator.ADD) {
            for ((a, b) in listOf(lambda.left to lambda.right, lambda.right to lambda.left)) {
                if (a == Identifier(reduce.arg1) && b == Identifier(reduce.arg2)) {
                    val result1 = input.range.last * (input.range.last + 1) / 2
                    val result2 = (input.range.first - 1) * (input.range.first) / 2
                    return NumberJL((result1 - result2).toBigDecimal() + initial.value).toResult()
                }
            }
        }
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
                        ),
                        numberScale,
                    )
                )
            }

            val mid = input.size / 2
            val leftRange = input.subList(0, mid)
            val rightRange = input.subList(mid, input.size)
            return coroutineScope {
                ensureActive()
                val leftDeferred = async(Dispatchers.IO) { executeReduce(leftRange) }
                val rightDeferred = async(Dispatchers.IO) { executeReduce(rightRange) }

                executeReduce(
                    listOf(
                        // throw here to cancel the sibling coroutines
                        leftDeferred.await().isError { throw CancellationException(it.value) },
                        rightDeferred.await().isError { throw CancellationException(it.value) },
                    )
                )
            }
        }
        return try {
            executeReduce(listOf(initial, *input.values.toTypedArray()))
        } catch (failure: CancellationException) {
            InterpreterResult.Error(failure.message ?: "Reduce raised an unknown error")
        }
    }

    override suspend fun visitMap(map: MapJL): InterpreterResult<SequenceJL> {
        val input =
            map.input.accept(this)
                .map { it.checkIs<SequenceJL>("Input for `map`") }
                .isError { return it }
        // special case identity function
        if (map.lambda is Identifier && map.lambda.name == map.arg) {
            return input.toResult() // it's okay to return the sequence without copying it, it's immutable
        }
        return coroutineScope {
            runCatching {
                SequenceJL(
                    input.values.map {
                        async(Dispatchers.IO) {
                            ensureActive()
                            map.lambda.accept(
                                ExpressionInterpreter(
                                    mapOf(map.arg to it), numberScale
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
 * simplifies an expression to just the elements that are relevant to associativity determination
 */
class AssociativeSimplifier(a: String, b: String) : ExpressionTransformer() {
    private val a = Identifier(a)
    private val b = Identifier(b)
    private val zero = NumberLiteral(0)
    private val one = NumberLiteral(1)

    override suspend fun visitOperation(operation: Operation): Expression {
        val right = operation.right.accept(this)
        val left = operation.left.accept(this)
        when (operation.operator) {
            Operator.ADD -> {}
            Operator.SUBTRACT -> {
                if (right == zero) return left
                if (left == zero) return right
                if (left == right) return zero
            }

            Operator.MULTIPLY -> {
                for ((subject, other) in listOf(left to right, right to left)) {
                    when (subject) {
                        zero -> return zero
                        one -> return other
                        else -> {}
                    }
                }
            }

            Operator.DIVIDE -> operation.right == one && return left
            Operator.EXPONENT -> when (right) {
                zero -> return one
                one -> return left
                else -> {}
            }
        }

        val default = Operation(left, operation.operator, right)

        suspend fun addToCount(amount: Int): Expression? {
            // a + c * a -> (c + 1) * a
            for (operand in listOf(a, b)) {
                for ((subject, other) in listOf(left to right, right to left)) {
                    if (other !is Operation || other.operator != Operator.MULTIPLY) continue
                    for ((otherSide1, otherSide2) in listOf(
                        other.left to other.right,
                        other.right to other.left
                    )) {
                        if (otherSide1 == operand && otherSide2 is NumberLiteral && subject == operand) {
                            return Operation(
                                otherSide1,
                                Operator.MULTIPLY,
                                NumberLiteral(otherSide2.value + amount.toBigDecimal())
                            ).accept(this)
                        }
                    }
                }
            }
            return null
        }
        return when (default.operator) {
            Operator.DIVIDE, Operator.EXPONENT -> return default
            Operator.SUBTRACT -> {
                addToCount(-1) ?: default
            }

            Operator.ADD -> {
                for (subject in listOf(a, b)) {
                    if (left == subject && right == subject) {
                        return Operation(subject, Operator.MULTIPLY, NumberLiteral(2))
                    } else if (left == subject && right is NumberLiteral || left is NumberLiteral && right == subject) {
                        return subject
                    }
                }
                addToCount(1)?.let { return it }
                if (left is NumberLiteral && right is NumberLiteral) {
                    left
                } else default
            }

            Operator.MULTIPLY -> if (left is NumberLiteral && right is NumberLiteral) {
                return left
            } else default
        }
    }
}

class IsAssociative(private val a: String, private val b: String) :
    BooleanExpressionQuery(Strategy.AND) {
    private var foundA = false
    private var foundB = false
    lateinit var foundOperator: Operator
    override suspend fun visitIdentifier(identifier: Identifier): Boolean {
        if (identifier.name == a) {
            if (foundA) {
                return false
            }
            foundA = true
        }
        if (identifier.name == b) {
            if (foundB) {
                return false
            }
            foundB = true
        }
        return true
    }

    override suspend fun visitOperation(operation: Operation): Boolean {
        if (!::foundOperator.isInitialized) {
            foundOperator = operation.operator
        } else if (operation.operator != foundOperator) {
            return false
        }
        return super.visitOperation(operation)
    }

    override suspend fun visitReduce(reduce: Reduce) = false

    fun isIt() =
        (foundA && foundB && (foundOperator == Operator.ADD || foundOperator == Operator.MULTIPLY))
                || !(foundA || foundB)
}

/**
 * should never produce false positives, but can produce false negatives
 *
 * [expression] must be some binary operation that takes the parameters [a] and [b]
 */
suspend fun isAssociative(expression: Expression, a: String, b: String): Boolean {
    val simplified = expression.accept(AssociativeSimplifier(a, b))
    val visitor = IsAssociative(a, b)
    val result = simplified.accept(visitor)
    return result && visitor.isIt()
}
