package jetlang.types

import java.math.BigDecimal


sealed class Value {
    abstract fun textContent(): String
}

data class NumberJL(val value: BigDecimal) : Value() {
    constructor(value: Int) : this(value.toBigDecimal())

    override fun textContent() = value.toString()
}

sealed class SequenceJL : Value() {
    abstract val values: List<Value>
}

fun SequenceJL(values: List<Value>) = SequenceJLImpl(values)

data class SequenceJLImpl(override val values: List<Value>) : SequenceJL() {
    override fun textContent() =
        values.joinToString(" ", prefix = "{", postfix = "}") { it.textContent() }
}

data class RangeSequenceJL(val range: IntRange) : SequenceJL() {
    override val values: List<Value>
        get() = range.map(::NumberJL)
    override fun textContent() = "{${range.first}, ${range.last}}"
}
