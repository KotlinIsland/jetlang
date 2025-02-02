package jetlang.interpreter

import jetlang.parser.*
import jetlang.types.NumberJL
import jetlang.types.Type
import kotlinx.coroutines.flow.channelFlow


sealed class InterpreterResult {
    class Success(val value: Type) : InterpreterResult()
    class Error(val value: InterpreterException) : InterpreterResult()

    fun get() = when (this) {
        is Success -> value
        is Error -> throw RuntimeException("Interpreter error")
    }

    fun map(block: (Type) -> InterpreterResult) = when (this) {
        is Success -> block(value)
        is Error -> this
    }

    fun toOutput(): Output = when (this) {
        is Success -> Output.Standard(value.textContent())
        is Error -> Output.Error(this.value.toString())
    }

    inline fun isError(function: (InterpreterResult) -> Nothing) = when (this) {
        is Success -> this.value
        is Error -> function(this)
    }
}


sealed class Output {
    abstract val value: String
    class Standard(override val value: String) : Output()
    class Error(override val value: String) : Output()
}

class InterpreterException(message: String) : Exception(message)

// TODO: utilize `DeepRecursiveFunction`
class Interpreter : StatementVisitor<Output?> {
    val names = mutableMapOf<String, Type>()

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
        names[`var`.name] = `var`.expression.accept(expressionInterpreter).isError { return it.toOutput() }
        return null
    }

    override fun visitExpressionStatement(expression: ExpressionStatement): Output? {
        expression.expression.accept(expressionInterpreter).isError { return it.toOutput() }
        return null
    }
}

class ExpressionInterpreter(private val names: Map<String, Type>) : ExpressionVisitor<InterpreterResult> {
    override fun visitNumberLiteral(numberLiteral: NumberLiteral) =
        InterpreterResult.Success(NumberJL(numberLiteral.value))

    override fun visitIdentifier(identifier: Identifier): InterpreterResult {
        val result = names[identifier.name]
        return if (result == null)
            InterpreterResult.Error((InterpreterException("Variable \"${identifier.name}\" not defined")))
        else InterpreterResult.Success(result)
    }
}
